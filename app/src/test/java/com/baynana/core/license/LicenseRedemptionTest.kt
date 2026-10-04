package com.baynana.core.license

import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.license.LicenseKinds
import com.baynana.data.local.license.LicenseRepository
import com.baynana.domain.license.EcdsaSignatureFormat
import com.baynana.domain.license.LicenseRejection
import com.baynana.domain.license.LicenseToken
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة ح١٣ على جهاز حقيقي (قاعدة في الذاكرة + تخزين التفعيل الفعلي).
 *
 * السؤال الذي تجيب عنه هذه البوابة، وهو نصّ شرط القبول في خطة البناء: **«استخدام الرمز مرتين لا
 * يمدّد»**. وكان الجواب قبل الإصلاح: «يمدّد كل مرة» (LIC-01).
 *
 * التوقيع هنا حقيقي: زوج مفاتيح P-256 يُولَّد في الاختبار، والتوقيع يُحوَّل إلى صيغة المستطيل
 * (P1363) كما تنتجه WebCrypto في لوحة الأدمن، ثم يُمرَّر إلى الفاحص كما يمرّ في الإنتاج. فلو
 * انكسر التحويل بين الصيغتين لسقط هذا الاختبار بكامله لا اختبار الصيغة وحده.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicenseRedemptionTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: LicenseRepository
    private lateinit var keys: KeyPair
    private lateinit var deviceCode: String

    private val knownRoles = setOf("MUSRIB", "DALLAL", "BUYER", "FARMER")

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LicenseRepository(db, knownRoles = knownRoles)
        keys = KeyPairGenerator.getInstance("EC")
            .apply { initialize(ECGenParameterSpec("secp256r1")) }
            .generateKeyPair()
        deviceCode = LicenseManager.getDeviceCode(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ------------------------------------------------------------------ أدوات البناء

    private fun sign(grant: LicenseToken.Grant): String {
        val payload = LicenseToken.payloadText(grant).toByteArray(Charsets.UTF_8)
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keys.private)
        signer.update(payload)
        val raw = EcdsaSignatureFormat.derToRaw(signer.sign())
            ?: throw AssertionError("توقيع جافا لم يُفكّ إلى صيغة المستطيل")
        return LicenseToken.encode(LicenseToken.payloadText(grant), raw)
    }

    private fun token(
        days: Int = 30,
        role: String = "MUSRIB",
        plan: String = "MONTHLY",
        forDevice: String = deviceCode,
        licenseId: String = "lic-test-" + System.nanoTime()
    ): String {
        val now = System.currentTimeMillis()
        return sign(
            LicenseToken.Grant(
                licenseId = licenseId,
                deviceCode = forDevice,
                role = role,
                plan = plan,
                durationDays = days,
                issuedAt = now,
                expiresAt = now + days.toLong() * LicenseRepository.DAY_MS
            )
        )
    }

    private fun publicKeyMaterial(): String =
        Base64.encodeToString(keys.public.encoded, Base64.NO_WRAP)

    /**
     * الفاحص هنا مولَّد من زوج مفاتيح الاختبار نفسه: التوقيع حقيقي والتحقق حقيقي (P1363 ← DER)، ولا
     * نعتمد على مفتاح مثبَّت في الأصول. و`missingPublicKeyAssetIsReportedAsSuch` هو الذي يثبّت أن
     * قالب الأصول الفارغ لا يمرّ كمفتاح صالح.
     */
    private fun redeem(token: String) = runBlocking {
        LicenseManager.redeem(
            context = context,
            enteredKey = token,
            repository = repository,
            verifierOverride = EcdsaSignatureVerifier(publicKeyMaterial())
        )
    }

    private fun storedExpiry(role: LicenseManager.LicenseRole = LicenseManager.LicenseRole.MUSRIB): Long =
        LicenseManager.getRoleExpiresAt(context, role)

    // ------------------------------------------------------------------ ١) شرط القبول

    @Test
    fun sameTokenTwiceExtendsOnlyOnce() {
        val token = token(days = 30)

        val first = redeem(token)
        assertTrue("المحاولة الأولى تُقبل", first is LicenseManager.ActivationOutcome.Activated)
        val expiryAfterFirst = storedExpiry()
        assertTrue("الاستحقاق كُتب في الدفتر", expiryAfterFirst > System.currentTimeMillis())
        assertEquals(
            "المدة المعروضة هي الموقّعة نفسها",
            (first as LicenseManager.ActivationOutcome.Activated).result.expiresAt,
            expiryAfterFirst
        )

        val second = redeem(token)
        assertTrue("المحاولة الثانية تُرفض", second is LicenseManager.ActivationOutcome.Rejected)
        assertEquals(
            "سبب الرفض هو الاسترداد السابق",
            LicenseRejection.ALREADY_REDEEMED,
            (second as LicenseManager.ActivationOutcome.Rejected).reason
        )
        assertEquals("لا تمديد: نهاية الاستحقاق لم تتغيّر بايتًا واحدًا", expiryAfterFirst, storedExpiry())

        assertEquals("صفّ استحقاق واحد فقط", 1, runBlocking { db.licenseDao().countLicenses() })
        val events = runBlocking { db.licenseDao().redeemedKeys() }
        assertEquals("المفتاح مسجّل مرة واحدة", 1, events.size)
    }

    @Test
    fun theAuditTrailShowsOneGrantAndOneRefusalWithItsReason() {
        val token = token()
        redeem(token)
        redeem(token)

        val events = runBlocking { db.licenseDao().observeEvents(10).first() }
        assertEquals("محاولتان = حدثان", 2, events.size)
        val granted = events.single { it.outcome == LicenseKinds.GRANTED }
        val rejected = events.single { it.outcome == LicenseKinds.REJECTED }
        assertEquals("رسالة القبول ليست فارغة", true, granted.message.isNotEmpty())
        assertEquals(LicenseRejection.ALREADY_REDEEMED.name, rejected.reason)
        assertEquals(
            "السبب المحفوظ هو نفسه الذي رآه المستخدم",
            LicenseRejection.ALREADY_REDEEMED.messageArabic,
            rejected.message
        )
        assertEquals("الرمز لا يُطبع كاملًا في السجل", true, rejected.tokenPrefix.length <= 13)
        assertEquals("الأثر يربط الحدث بالاستحقاق نفسه", granted.licenseId, rejected.licenseId)
        assertTrue("الرفض لم يمدّد شيئًا في الأثر", rejected.expiresAtAfter == rejected.expiresAtBefore)
    }

    @Test
    fun legacyKeyAlsoRedeemsOnceEvenThoughItIsDeviceDerived() {
        val legacyKey = LicenseManager.generateActivationKey(
            deviceCode,
            LicenseManager.LicenseRole.MUSRIB,
            LicenseManager.SubscriptionPlan.MONTHLY
        )

        val first = redeem(legacyKey)
        assertTrue("المفتاح القديم ما زال يعمل (توافق خلفي)", first is LicenseManager.ActivationOutcome.Activated)
        val kind = (first as LicenseManager.ActivationOutcome.Activated).kind
        assertEquals("ويُوسَم أنه موروث لا أنه تصريح موقّع", LicenseKinds.LEGACY, kind)

        val expiryAfterFirst = storedExpiry()
        val second = redeem(legacyKey)
        assertTrue(second is LicenseManager.ActivationOutcome.Rejected)
        assertEquals(LicenseRejection.ALREADY_REDEEMED, (second as LicenseManager.ActivationOutcome.Rejected).reason)
        assertEquals("إدخال المفتاح القديم ثانية لا يمدّد", expiryAfterFirst, storedExpiry())
    }

    // ------------------------------------------------------------------ ٢) الرفض يشرح نفسه

    @Test
    fun aTokenForAnotherDeviceIsRefusedAndWritesNothing() {
        val token = token(forDevice = "MSRB-1111-2222")
        val outcome = redeem(token)
        assertTrue(outcome is LicenseManager.ActivationOutcome.Rejected)
        assertEquals(LicenseRejection.WRONG_DEVICE, (outcome as LicenseManager.ActivationOutcome.Rejected).reason)
        assertEquals("لا استحقاق من نصّ مرفوض", 0, runBlocking { db.licenseDao().countLicenses() })
        assertEquals("ولا كتابة في التخزين السريع", 0L, storedExpiry())
    }

    @Test
    fun aTamperedTermIsRefusedEvenWithAValidLookingTokenShell() {
        val honestGrant = LicenseToken.Grant(
            licenseId = "lic-tamper-1",
            deviceCode = deviceCode,
            role = "MUSRIB",
            plan = "MONTHLY",
            durationDays = 30,
            issuedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 30L * LicenseRepository.DAY_MS
        )
        // نأخذ توقيع الحمولة الصادقة ونلبسه حمولة أطول مدة: هذا تمامًا ما يفعله المعدّل.
        val honestToken = sign(honestGrant)
        val signature = honestToken.substringAfterLast('.')
        val greedyPayload = LicenseToken.payloadText(honestGrant.copy(durationDays = 3650, expiresAt = honestyExpiry(honestGrant)))
        val forged = LicenseToken.PREFIX + "." +
            com.baynana.domain.license.Base64Codec.encodeUrl(greedyPayload.toByteArray(Charsets.UTF_8)) + "." + signature

        val outcome = redeem(forged)
        assertTrue(outcome is LicenseManager.ActivationOutcome.Rejected)
        assertEquals(LicenseRejection.BAD_SIGNATURE, (outcome as LicenseManager.ActivationOutcome.Rejected).reason)
        assertEquals(0L, storedExpiry())
    }

    private fun honestyExpiry(grant: LicenseToken.Grant): Long =
        grant.issuedAt + 3650L * LicenseRepository.DAY_MS

    @Test
    fun aSignatureFromTheBrowserFormatVerifiesWithTheRealKeyPair() {
        val verifier = EcdsaSignatureVerifier(publicKeyMaterial())
        assertTrue("المفتاح العام من أعلى الشكل صالح", verifier.isUsable)

        val grant = LicenseToken.Grant(
            licenseId = "lic-verify-1",
            deviceCode = deviceCode,
            role = "MUSRIB",
            plan = "MONTHLY",
            durationDays = 30,
            issuedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 30L * LicenseRepository.DAY_MS
        )
        val payload = LicenseToken.payloadText(grant).toByteArray(Charsets.UTF_8)
        val parsed = LicenseToken.parse(sign(grant))
        assertTrue(parsed is LicenseToken.Parsed.Signed)
        val signed = parsed as LicenseToken.Parsed.Signed
        assertEquals("الحمولة لم تتغيّر عبر الترميز", payload.toList(), signed.payloadBytes.toList())
        assertTrue("التوقيع المستطيل يُقبل بعد التحويل", verifier.verify(signed.payloadBytes, signed.signatureBytes))
        assertTrue(
            "وحمولة معدّلة لا تُقبل",
            !verifier.verify(payload + " ".toByteArray(Charsets.UTF_8), signed.signatureBytes)
        )
    }

    @Test
    fun missingPublicKeyAssetIsReportedAsSuch() {
        val material = LicenseSignatures.extractKeyMaterial(
            java.io.File("app/src/main/assets/license_public_key.txt").takeIf { it.exists() }?.readText()
                ?: java.io.File("src/main/assets/license_public_key.txt").readText()
        )
        assertTrue(
            "قالب المفتاح الموجود في المستودع ليس مفتاحًا: وجود قالب لا يجوز أن يمرّ كمفتاح صالح",
            material == null
        )
        assertNotNull("والمفتاح الحقيقي في الاختبار يُقرأ", LicenseSignatures.extractKeyMaterial(publicKeyMaterial()))
    }
}

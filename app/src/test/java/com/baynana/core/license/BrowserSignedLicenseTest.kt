package com.baynana.core.license

import com.baynana.domain.license.LicenseAuthority
import com.baynana.domain.license.LicenseCheck
import com.baynana.domain.license.LicenseRejection
import com.baynana.domain.license.LicenseToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **متجه ذهبي** لتصريح وُقّع في المتصفح.
 *
 * الرمز أدناه لم يُبنَ في Kotlin ولا في هذا الاختبار: وُلد من كتلة `LICENSE SIGNING` في
 * `tools/key_generator.html` (WebCrypto، توقيع P1363) وتحقّقت منه مكتبة مستقلة في
 * `tools/license_signer_test.mjs`. وهنا نمرّره على **فاحص التطبيق نفسه**.
 *
 * قيمة هذا الاختبار أنه يمسك ما لا تمسكه اختبارات كل جهة وحدها: فرق صيغة أو ترميز بين المتصفح
 * والتطبيق. لو انكسر التحويل من P1363 إلى DER، أو تغيّر ترتيب حقول الحمولة، أو اختلف ترميز
 * base64url، لسقط هذا الاختبار وحده وكانت رسالته واضحة.
 *
 * **لإعادة توليد المتجه** (بعد أي تغيير في الصيغة):
 * `node tools/license_signer_test.mjs` ثم انسخ السطور الثلاث المطبوعة إلى هنا.
 */
class BrowserSignedLicenseTest {

    private companion object {
        const val GOLDEN_PUBLIC_KEY_BASE64 =
            "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEieTmSFKjjVisSMkVJYWGi1AKyWwmrOEp/y9rJaQJGheeGt89+I92rvt532m+HTvYSvPj1tbtFCrycaF/8p9SOA=="

        const val GOLDEN_TOKEN =
            "BNNA1.MXxsaWMtZ29sZGVuLTAwMDEtYWJjZHxNU1JCOEY0MjlEMUJ8TVVTUklCfE1PTlRITFl8MzB8MTc3MDAwMDAwMDAwMHwxNzcyNTkyMDAwMDAw." +
                "BZaGr02du9tSeyPPfClcE5mzDXHK00CecxZiWRD46FgHoQyn1wDPyXI__tASymrKNG68jyPA5v29KxmPJG-V-w"

        const val GOLDEN_PAYLOAD =
            "1|lic-golden-0001-abcd|MSRB8F429D1B|MUSRIB|MONTHLY|30|1770000000000|1772592000000"

        const val DEVICE = "MSRB-8F42-9D1B"
        const val ISSUED_AT = 1770000000000L
        const val EXPIRES_AT = ISSUED_AT + 30L * 24L * 3600L * 1000L
        const val NOW = ISSUED_AT + 60_000L
    }

    private val verifier = EcdsaSignatureVerifier(GOLDEN_PUBLIC_KEY_BASE64)

    @Test
    fun thePublicKeyFromTheToolIsUsableByTheApp() {
        assertTrue("المفتاح العام المصدَّر من المتصفح مقبول في Android", verifier.isUsable)
    }

    @Test
    fun theBrowserSignedPayloadIsExactlyWhatTheAppExpects() {
        val parsed = LicenseToken.parse(GOLDEN_TOKEN)
        assertTrue(parsed is LicenseToken.Parsed.Signed)
        val signed = parsed as LicenseToken.Parsed.Signed
        assertEquals("الحمولة كما كتبها المتصفح", GOLDEN_PAYLOAD, signed.payloadText)
        assertEquals("التوقيع المستطيل ٦٤ بايت كما تنتجه WebCrypto", 64, signed.signatureBytes.size)
        assertEquals(
            "والحمولة تُفكّ إلى استحقاق مفهوم",
            LicenseToken.Grant(
                licenseId = "lic-golden-0001-abcd",
                deviceCode = "MSRB8F429D1B",
                role = "MUSRIB",
                plan = "MONTHLY",
                durationDays = 30,
                issuedAt = ISSUED_AT,
                expiresAt = EXPIRES_AT
            ),
            LicenseToken.decodeGrant(signed.payloadText)
        )
    }

    @Test
    fun theAppAcceptsTheBrowserSignature() {
        val parsed = LicenseToken.parse(GOLDEN_TOKEN) as LicenseToken.Parsed.Signed
        assertTrue(
            "توقيع WebCrypto يجب أن يُقبل في التطبيق (تحويل P1363 ← DER)",
            verifier.verify(parsed.payloadBytes, parsed.signatureBytes)
        )
        assertTrue(
            "وأي بايت مضاف إلى الحمولة يُرفض",
            !verifier.verify(parsed.payloadBytes + ' '.code.toByte(), parsed.signatureBytes)
        )
    }

    @Test
    fun theAuthorityGrantsItForTheRightDevice() {
        val result = LicenseAuthority.check(
            entered = GOLDEN_TOKEN,
            deviceCode = DEVICE,
            nowMillis = NOW,
            verifier = verifier,
            redeemedKeys = emptySet(),
            knownRoles = setOf("MUSRIB", "DALLAL", "BUYER", "FARMER")
        )
        assertTrue("التصريح الموقّع في اللوحة يُقبل", result is LicenseCheck.Granted)
        assertEquals("lic-golden-0001-abcd", (result as LicenseCheck.Granted).grant.licenseId)
    }

    @Test
    fun theAuthorityRefusesItForAnotherDeviceAndForAReplay() {
        val wrongDevice = LicenseAuthority.check(
            entered = GOLDEN_TOKEN,
            deviceCode = "MSRB-0000-1111",
            nowMillis = NOW,
            verifier = verifier,
            redeemedKeys = emptySet()
        )
        assertEquals(
            LicenseRejection.WRONG_DEVICE,
            (wrongDevice as LicenseCheck.Rejected).reason
        )

        val replayed = LicenseAuthority.check(
            entered = GOLDEN_TOKEN,
            deviceCode = DEVICE,
            nowMillis = NOW,
            verifier = verifier,
            redeemedKeys = setOf("signed:lic-golden-0001-abcd")
        )
        assertEquals(
            LicenseRejection.ALREADY_REDEEMED,
            (replayed as LicenseCheck.Rejected).reason
        )
    }

    @Test
    fun aSignatureFromAnotherKeyIsRefusedByTheApp() {
        val parsed = LicenseToken.parse(GOLDEN_TOKEN) as LicenseToken.Parsed.Signed
        val generator = java.security.KeyPairGenerator.getInstance("EC")
        generator.initialize(java.security.spec.ECGenParameterSpec("secp256r1"))
        val other = generator.generateKeyPair()
        val signer = java.security.Signature.getInstance("SHA256withECDSA")
        signer.initSign(other.private)
        signer.update(parsed.payloadBytes)
        val forged = com.baynana.domain.license.EcdsaSignatureFormat.derToRaw(signer.sign())!!

        assertTrue(
            "توقيع مفتاح آخر لا يُقبل ولو كانت الحمولة صحيحة",
            !verifier.verify(parsed.payloadBytes, forged)
        )
    }

    @Test
    fun theAssetTemplateIsNotAKeyButARealKeyIsReadable() {
        val template = java.io.File("src/main/assets/license_public_key.txt")
            .takeIf { it.exists() }
            ?: java.io.File("app/src/main/assets/license_public_key.txt")
        if (template.exists()) {
            assertEquals(
                "قالب المستودع ليس مفتاحًا: يُلصق المفتاح الحقيقي قبله",
                null,
                LicenseSignatures.extractKeyMaterial(template.readText())
            )
        }
        assertEquals(
            "ونصّ PEM بالمفتاح الحقيقي يُقرأ بلا رؤوس ولا أسطر",
            GOLDEN_PUBLIC_KEY_BASE64,
            LicenseSignatures.extractKeyMaterial(
                "-----BEGIN PUBLIC KEY-----\n$GOLDEN_PUBLIC_KEY_BASE64\n-----END PUBLIC KEY-----\n"
            )
        )
    }
}

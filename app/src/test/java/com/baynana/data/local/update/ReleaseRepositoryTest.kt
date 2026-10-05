package com.baynana.data.local.update

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.core.license.EcdsaSignatureVerifier
import com.baynana.core.update.ReleaseFetch
import com.baynana.core.update.ReleaseSource
import com.baynana.domain.update.ReleaseManifest
import com.baynana.domain.update.ReleaseSignatureVectors
import com.baynana.domain.update.UpdateDecision
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest

/**
 * بوابة ح٢٠ على الجهاز: ما يُخزَّن، وما يُعرض، وما يُنزَّل.
 *
 * أربع قواعد تحرسها هذه البوابة، وكل واحدة تقابل عطبًا يُشكى منه:
 * 1. **جهاز بلا إنترنت لا يفقد ما يعرفه:** آخر ملفّ **سليم** يُخزَّن، فيبقى التنبيه قائمًا بعد
 *    انقطاع الشبكة — ويُقال إنّ الحالة من ملفّ محفوظ.
 * 2. **ملفّ مرفوض لا يمحو ملفًّا سليمًا:** توقيع مزوّف يصل بعد ملفّ صحيح لا يجوز أن يمسح التنبيه.
 * 3. **الملفّ الذي لا يطابق بصمته يُحذف ولا يُعرض للتثبيت أبدًا:** أخطر لحظة في قناة التحديث.
 * 4. **الإجباري لا يُتخطّى:** من نسخته أقدم من الحد المدعوم لا يملك تأجيل التحديث.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReleaseRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    private val verifier = EcdsaSignatureVerifier(ReleaseSignatureVectors.TEST_PUBLIC_KEY_BASE64)
    private val verify: (ByteArray, ByteArray) -> Boolean = { payload, signature ->
        verifier.verify(payload, signature)
    }

    private val now = 1_767_225_600_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun repository(
        versionCode: Int,
        source: ReleaseSource,
        downloader: ApkDownloader = HttpApkDownloader(),
        online: () -> Boolean = { true }
    ) = ReleaseRepository(
        context = context,
        db = db,
        currentVersionCode = versionCode,
        currentVersionName = "1.0",
        sourceOverride = source,
        downloader = downloader,
        verifierOverride = verify,
        now = { now },
        onlineOverride = online
    )

    private fun body(token: String) = ReleaseSource { ReleaseFetch.Body(token) }

    // ------------------------------------------------------------------ ١) التخزين والانقطاع

    @Test
    fun aVerifiedManifestIsStoredAndSurvivesWithoutInternet() = runBlocking {
        val repo = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN))
        val checked = repo.check()
        assertTrue(checked.state is UpdateDecision.UpdateState.Optional)
        assertFalse(checked.fromCache)
        assertEquals(now, checked.checkedAt)
        // ويُخزَّن الملفّ نفسه كما وصل: الدليل بعد انقطاع الشبكة يُبنى على ما وُقّع فعلًا.
        assertEquals(
            ReleaseSignatureVectors.OPTIONAL_TOKEN,
            db.appSettingDao().getSettingValue(ReleaseRepository.KEY_LAST_TOKEN)
        )

        // الآن بلا إنترنت: الحكم نفسه يبقى، ويُقال إنه من ملفّ محفوظ.
        val offline = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN), online = { false }).check()
        assertTrue(offline.state is UpdateDecision.UpdateState.Optional)
        assertTrue(offline.fromCache)
        assertEquals(now, offline.checkedAt)
    }

    @Test
    fun aRejectedManifestNeverErasesAKnownGoodOne() = runBlocking {
        repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN)).check()
        val tampered = repository(3, body(ReleaseSignatureVectors.TAMPERED_PAYLOAD_TOKEN)).check()
        // الرفض لا يمحو المعرفة السابقة: الحالة تبقى «إصدار أحدث متاح»…
        assertTrue(tampered.state is UpdateDecision.UpdateState.Optional)
        // …وسببُ الرفض يُقال إلى جانبها لا بدلًا منها.
        assertTrue(
            "سبب رفض الملفّ يجب أن يُعرض للمستخدم",
            tampered.failureReasonArabic?.contains("توقيع") == true
        )

        // وجهاز جديد لم يفحص شيئًا: الرفض يُعلن بصراحة لا صمتًا.
        db.appSettingDao().saveSetting(com.baynana.features.settings.AppSetting(ReleaseRepository.KEY_LAST_TOKEN, ""))
        val fresh = repository(3, body(ReleaseSignatureVectors.TAMPERED_PAYLOAD_TOKEN)).check()
        assertTrue(fresh.state is UpdateDecision.UpdateState.Unknown)
        assertTrue(fresh.failureReasonArabic?.contains("توقيع") == true)
    }

    @Test
    fun withoutInternetOrWithoutEndpointTheDeviceSaysSoAndKeepsWorking() = runBlocking {
        val noNetwork = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN), online = { false }).check()
        assertTrue(noNetwork.state is UpdateDecision.UpdateState.Unknown)
        assertEquals("لا إنترنت الآن", (noNetwork.state as UpdateDecision.UpdateState.Unknown).reasonArabic)
        assertEquals("لا إنترنت الآن", noNetwork.failureReasonArabic)

        val failed = repository(3, ReleaseSource { ReleaseFetch.Failed("قناة التحديث ردّت بحالة 404") }).check()
        assertEquals("قناة التحديث ردّت بحالة 404", (failed.state as UpdateDecision.UpdateState.Unknown).reasonArabic)
        assertEquals("قناة التحديث ردّت بحالة 404", failed.failureReasonArabic)
    }

    // ------------------------------------------------------------------ ٢) التخطّي والإجبار

    @Test
    fun skippingIsRecordedAndRequiredUpdatesCannotBeSkipped() = runBlocking {
        val repo = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN))
        repo.check()
        assertTrue(repo.skip(4))
        val after = repo.cachedState()
        assertTrue((after.state as UpdateDecision.UpdateState.Optional).skipped)

        // الإجباري: نسخة ٠ أقدم من الحد المدعوم ٥ في المتجه الإجباري.
        val required = repository(0, body(ReleaseSignatureVectors.REQUIRED_TOKEN))
        required.check()
        assertTrue(required.cachedState().state is UpdateDecision.UpdateState.Required)
        assertFalse("لا يجوز تخطّي تحديث لازم", required.skip(7))
    }

    // ------------------------------------------------------------------ ٣) التنزيل والبصمة

    @Test
    fun aFileThatDoesNotMatchTheSignedFingerprintIsDeletedAndNeverOffered() = runBlocking {
        val release = verifiedRelease(ReleaseSignatureVectors.OPTIONAL_TOKEN)
        val downloader = ApkDownloader { _, destination, _ ->
            destination.writeBytes("محتوى ليس هو الملفّ الموقّع".toByteArray())
            DownloadFetch.Saved(destination.length())
        }
        val outcome = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN), downloader).download(release)
        assertTrue(outcome is ReleaseRepository.DownloadOutcome.Refused)
        assertTrue((outcome as ReleaseRepository.DownloadOutcome.Refused).reasonArabic.contains("البصمة"))
        // ولا يبقى الملفّ في الجهاز: لا يُتاح تثبيت شيء لم نتحقق منه.
        val directory = File(context.cacheDir, "updates")
        assertTrue((directory.listFiles() ?: emptyArray()).isEmpty())
    }

    @Test
    fun aFileMatchingTheFingerprintIsSavedReadyForTheSystemInstaller() = runBlocking {
        // نبني ملفًّا يكون هو نفسه موضوع البصمة: نُوقّع ملفّ إصدار يحمل بصمته الصحيحة.
        val apkBytes = "ملفّ APK تجريبي".toByteArray()
        val sha = MessageDigest.getInstance("SHA-256").digest(apkBytes)
            .joinToString("") { "%02x".format(it) }
        val downloader = ApkDownloader { _, destination, _ ->
            destination.writeBytes(apkBytes)
            DownloadFetch.Saved(apkBytes.size.toLong())
        }
        val release = ReleaseManifest.Release(
            versionCode = 9,
            versionName = "1.4",
            minSupportedVersionCode = 1,
            apkUrl = "https://example.com/baynana-1.4.apk",
            apkSha256 = sha,
            publishedAt = now,
            messageArabic = "تحسينات"
        )
        val outcome = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN), downloader).download(release)
        assertTrue(outcome is ReleaseRepository.DownloadOutcome.Ready)
        val file = (outcome as ReleaseRepository.DownloadOutcome.Ready).file
        assertTrue(file.exists())
        assertEquals(apkBytes.size.toLong(), outcome.sizeBytes)
        assertTrue(file.name.endsWith(".apk"))
    }

    @Test
    fun theLocalInstallerSummarySaysDataIsNotTouched() = runBlocking {
        val release = verifiedRelease(ReleaseSignatureVectors.OPTIONAL_TOKEN)
        val summary = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN)).installSummary(release)
        assertTrue(summary.contains("لا تُرسل ولا تُمحى"))
        assertTrue(summary.contains("1.2"))
    }

    @Test
    fun aNonHttpsDownloadUrlIsRefusedBeforeAnyByteMoves() = runBlocking {
        val release = ReleaseManifest.Release(
            versionCode = 9,
            versionName = "1.4",
            minSupportedVersionCode = 1,
            apkUrl = "http://example.com/baynana.apk",
            apkSha256 = "f".repeat(64),
            publishedAt = now,
            messageArabic = "تحسينات"
        )
        var called = false
        val downloader = ApkDownloader { _, _, _ -> called = true; DownloadFetch.Saved(0) }
        val outcome = repository(3, body(ReleaseSignatureVectors.OPTIONAL_TOKEN), downloader).download(release)
        assertTrue(outcome is ReleaseRepository.DownloadOutcome.Refused)
        assertFalse("لا يُنادى التنزيل أصلًا على رابط غير مشفّر", called)
    }

    private fun verifiedRelease(token: String): ReleaseManifest.Release {
        val readout = ReleaseManifest.read(token, verify)
        assertTrue(readout is ReleaseManifest.Readout.Verified)
        return (readout as ReleaseManifest.Readout.Verified).release
    }
}

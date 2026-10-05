package com.baynana.domain.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٢٠ على **القرار** لا على الصيغة: ما يراه الإنسان حين يفحص التحديث.
 *
 * وكل حالة هنا تقابل عطبًا يُشكى منه:
 * 1. «تعذّر الفحص» يُقطع العمل ويربك صاحب الدفتر ⇒ الفحص لا يوقف شيئًا، والسبب يُقال بالعربية.
 * 2. «أنت غير مدعوم» لمن هو مدعوم ⇒ الحد الأدنى يُقارن بصدق.
 * 3. تحديث يبدأ وحده ⇒ لا قرار تنزيل واحد في هذا الملفّ بلا بصمة مطابقة.
 * 4. «تخطّي» يُسقط تحديثًا لازمًا ⇒ الإجباري لا يُتخطّى.
 */
class UpdateDecisionTest {

    private fun release(
        versionCode: Int,
        minSupported: Int = 1,
        versionName: String = "1.2",
        sha256: String = "d".repeat(64)
    ) = ReleaseManifest.Release(
        versionCode = versionCode,
        versionName = versionName,
        minSupportedVersionCode = minSupported,
        apkUrl = "https://example.com/baynana.apk",
        apkSha256 = sha256,
        publishedAt = 1_767_225_600_000,
        messageArabic = "تحسينات وإصلاحات"
    )

    private fun verified(release: ReleaseManifest.Release) = ReleaseManifest.Readout.Verified(release)

    @Test
    fun afailedCheckKeepsTheAppUsableAndExplainsWhy() {
        val state = UpdateDecision.decide(3, ReleaseManifest.Readout.NotARelease("قناة التحديث ردّت صفحة خطأ"))
        assertTrue(state is UpdateDecision.UpdateState.Unknown)
        assertEquals("قناة التحديث ردّت صفحة خطأ", (state as UpdateDecision.UpdateState.Unknown).reasonArabic)
        assertEquals("تعذّر فحص التحديث", state.titleArabic)
    }

    @Test
    fun aRejectedSignatureIsNeverPresentedAsAnUpdate() {
        val state = UpdateDecision.decide(3, ReleaseManifest.Readout.SignatureRejected("التوقيع مرفوض"))
        assertTrue(state is UpdateDecision.UpdateState.Unknown)
        assertFalse(state is UpdateDecision.UpdateState.Optional)
    }

    @Test
    fun newerReleaseIsOptionalForASupportedDevice() {
        val state = UpdateDecision.decide(3, verified(release(4)))
        assertTrue(state is UpdateDecision.UpdateState.Optional)
        assertEquals("إصدار أحدث متاح", state.titleArabic)
    }

    @Test
    fun sameOrOlderReleaseMeansUpToDate() {
        assertTrue(UpdateDecision.decide(4, verified(release(4))) is UpdateDecision.UpdateState.UpToDate)
        assertTrue(UpdateDecision.decide(5, verified(release(4))) is UpdateDecision.UpdateState.UpToDate)
        assertEquals("أنت على أحدث إصدار", UpdateDecision.decide(4, verified(release(4))).titleArabic)
    }

    @Test
    fun anUnsupportedDeviceGetsARequiredUpdateEvenIfTheVersionIsNotNewer() {
        // نسخة الجهاز ٢، والحد المدعوم ٣: التطبيق القديم معطوب لا «مؤجَّل». وهذه أهمّ حالة في الملفّ.
        val state = UpdateDecision.decide(2, verified(release(3, minSupported = 3)))
        assertTrue(state is UpdateDecision.UpdateState.Required)
        assertEquals("تحديث لازم ليكمل التطبيق عمله", state.titleArabic)
    }

    @Test
    fun skippingHidesTheOptionalReleaseButNeverTheRequiredOne() {
        val optional = UpdateDecision.decide(3, verified(release(4)), skippedVersionCode = 4)
        assertTrue(optional is UpdateDecision.UpdateState.Optional)
        assertTrue((optional as UpdateDecision.UpdateState.Optional).skipped)
        assertEquals("إصدار أحدث متاح (تخطّيته)", optional.titleArabic)

        // ومتى ظهر إصدار أحدث من الذي تخطّاه، عاد التنبيه: التخطّي لإصدار لا لدوام.
        assertFalse((UpdateDecision.decide(3, verified(release(5)), skippedVersionCode = 4)
            as UpdateDecision.UpdateState.Optional).skipped)
    }

    @Test
    fun amatchingFingerprintIsTheOnlyWayToAllowAnInstall() {
        val target = release(4)
        assertTrue(UpdateDecision.allowDownload(target, target.apkSha256) is UpdateDecision.DownloadPermission.Allowed)
        // حالة الأحرف لا تُهمّ، لكن حرفًا واحدًا يهمّ.
        assertTrue(UpdateDecision.allowDownload(target, target.apkSha256.uppercase()) is UpdateDecision.DownloadPermission.Allowed)
        val wrong = UpdateDecision.allowDownload(target, "e".repeat(64))
        assertTrue(wrong is UpdateDecision.DownloadPermission.Refused)
        assertTrue((wrong as UpdateDecision.DownloadPermission.Refused).reasonArabic.contains("البصمة"))
        assertTrue(UpdateDecision.allowDownload(target, null) is UpdateDecision.DownloadPermission.Refused)
        assertTrue(UpdateDecision.allowDownload(target, "   ") is UpdateDecision.DownloadPermission.Refused)
    }

    @Test
    fun versionDisplayKeepsBothTheHumanNameAndTheNumber() {
        assertEquals("1.2 (4)", UpdateDecision.displayVersion("1.2", 4))
    }
}

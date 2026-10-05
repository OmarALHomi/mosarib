package com.baynana.domain.update

import com.baynana.core.license.EcdsaSignatureVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٢٠ على الصيغة: **يُوقَّع الرمز بأداة المالك (Node) ويُتحقق منه في التطبيق (Kotlin)** على
 * البايتات نفسها، لا على نسخة ثانية منها.
 *
 * المتجهات في [ReleaseSignatureVectors] مولَّدة آليًّا (`node tools/release_manifest.mjs --write`)
 * ومفحوصة في CI (`--check`)، فلا تنفصل عن الأداة بعد أول تعديل.
 *
 * وكل حالة هنا تقابل عطبًا حقيقيًا في قناة التحديث:
 * 1. **توقيع صحيح لا يمرّ في التطبيق** ⇒ كل الأجهزة تظنّ القناة مخرومة، والحقيقة فرق صيغة.
 * 2. **تعديل حرف في الحمولة لا يُكتشف** ⇒ يمكن زرع رابط تنزيل آخر في ملفّ يبدو موقّعًا.
 * 3. **توقيع مقلوب يمرّ** ⇒ كل الملفّات مقبولة، والقناة بلا حماية.
 * 4. **ملفّ JSON عارٍ يمرّ** ⇒ يكفي أن يُخترق المستضيف، بلا حاجة إلى مفتاح المالك.
 * 5. **رابط http يمرّ** ⇒ الوسيط يستبدل الملفّ الذي ينزل منه الجهاز.
 */
class ReleaseManifestTest {

    private val verifier = EcdsaSignatureVerifier(ReleaseSignatureVectors.TEST_PUBLIC_KEY_BASE64)

    private fun read(token: String) = ReleaseManifest.read(token) { payload, signature ->
        verifier.verify(payload, signature)
    }

    @Test
    fun aReleaseSignedByTheOwnersToolVerifiesInKotlin() {
        val readout = read(ReleaseSignatureVectors.OPTIONAL_TOKEN)
        assertTrue("التوقيع المولَّد في Node يجب أن يمرّ في Kotlin", readout is ReleaseManifest.Readout.Verified)
        val release = (readout as ReleaseManifest.Readout.Verified).release
        assertEquals(4, release.versionCode)
        assertEquals("1.2", release.versionName)
        assertEquals(1, release.minSupportedVersionCode)
        assertEquals(
            "https://releases.baynana.example/baynana-1.2.apk",
            release.apkUrl
        )
        assertEquals(
            "0f8c1d2e3b4a59687766554433221100aabbccddeeff00112233445566778899",
            release.apkSha256
        )
        assertTrue(release.messageArabic.contains("الباقي"))
    }

    @Test
    fun thePayloadIsExactlyWhatTheToolSigned() {
        assertEquals(
            "الحمولة المفكوكة في Kotlin يجب أن تساوي ما بنته الأداة بالحرف",
            ReleaseSignatureVectors.OPTIONAL_PAYLOAD,
            ReleaseManifest.payloadOf(ReleaseSignatureVectors.OPTIONAL_TOKEN)
        )
    }

    @Test
    fun oneCharacterChangeInThePayloadBreaksTheSignature() {
        val readout = read(ReleaseSignatureVectors.TAMPERED_PAYLOAD_TOKEN)
        assertTrue(readout is ReleaseManifest.Readout.SignatureRejected)
    }

    @Test
    fun aFlippedSignatureByteIsRejected() {
        val readout = read(ReleaseSignatureVectors.CORRUPT_SIGNATURE_TOKEN)
        assertTrue(readout is ReleaseManifest.Readout.SignatureRejected)
    }

    @Test
    fun plainJsonNeverPassesEvenIfItLooksLikeARelease() {
        val readout = read(ReleaseSignatureVectors.PLAIN_JSON)
        assertTrue(readout is ReleaseManifest.Readout.NotARelease)
    }

    @Test
    fun httpUrlIsRefusedBecauseAnIntermediaryCouldSwapTheFile() {
        val release = ReleaseManifest.Release(
            versionCode = 5,
            versionName = "1.3",
            minSupportedVersionCode = 1,
            apkUrl = "http://example.com/baynana.apk",
            apkSha256 = "a".repeat(64),
            publishedAt = 1_767_225_600_000,
            messageArabic = "تجربة"
        )
        val readout = ReleaseManifest.parsePayload(ReleaseManifest.buildPayload(release))
        assertTrue(readout is ReleaseManifest.Readout.Malformed)
        assertTrue((readout as ReleaseManifest.Readout.Malformed).reasonArabic.contains("https"))
    }

    @Test
    fun aShortFingerprintIsRefusedBecauseItCannotCoverTheFile() {
        val release = ReleaseManifest.Release(
            versionCode = 5,
            versionName = "1.3",
            minSupportedVersionCode = 1,
            apkUrl = "https://example.com/baynana.apk",
            apkSha256 = "0f8c1d2e",
            publishedAt = 1_767_225_600_000,
            messageArabic = "تجربة"
        )
        val readout = ReleaseManifest.parsePayload(ReleaseManifest.buildPayload(release))
        assertTrue(readout is ReleaseManifest.Readout.Malformed)
        assertTrue((readout as ReleaseManifest.Readout.Malformed).reasonArabic.contains("sha256"))
    }

    @Test
    fun minSupportedCannotExceedThePublishedVersion() {
        val release = ReleaseManifest.Release(
            versionCode = 3,
            versionName = "1.1",
            minSupportedVersionCode = 4,
            apkUrl = "https://example.com/baynana.apk",
            apkSha256 = "b".repeat(64),
            publishedAt = 1_767_225_600_000,
            messageArabic = "تجربة"
        )
        val readout = ReleaseManifest.parsePayload(ReleaseManifest.buildPayload(release))
        assertTrue(readout is ReleaseManifest.Readout.Malformed)
    }

    @Test
    fun anEmptyMessageIsRefusedBecauseTheUserDeservesToKnowWhatChanged() {
        val release = ReleaseManifest.Release(
            versionCode = 3,
            versionName = "1.1",
            minSupportedVersionCode = 1,
            apkUrl = "https://example.com/baynana.apk",
            apkSha256 = "c".repeat(64),
            publishedAt = 1_767_225_600_000,
            messageArabic = "   "
        )
        val readout = ReleaseManifest.parsePayload(ReleaseManifest.buildPayload(release))
        assertTrue(readout is ReleaseManifest.Readout.Malformed)
    }
}

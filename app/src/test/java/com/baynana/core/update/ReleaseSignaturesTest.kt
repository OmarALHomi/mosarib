package com.baynana.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * حارس قالبَي الأصول في قناة التحديث (ح٢٠) — وهما ملفّان يُخطئ فيهما الناس:
 *
 * 1. **مفتاح التحديث:** المستودع يحمل **قالبًا بلا مفتاح** عن قصد. والقاعدة التي تحمي الأجهزة:
 *    القالب لا يمرّ كمفتاح صالح أبدًا. وإن لصق المالك مفتاحه، فالفحص يتحوّل إلى الشرط الثاني:
 *    أن يكون المفتاح الموجود صالحًا فعلًا (لا نصًّا مقلوبًا يظنّ صاحبه أن القناة محميّة).
 * 2. **عنوان القناة:** يُقرأ من أصل بنائي ولا يُحرَّر من الشاشة، والقالب الفارغ يعني «لم يُضبط
 *    العنوان» لا «افحص من مكان ما». والرابط يُرفض إن لم يكن https، والمسار يُضاف مرة واحدة.
 */
class ReleaseSignaturesTest {

    private fun asset(name: String): String {
        val candidates = listOf(
            java.io.File("app/src/main/assets/$name"),
            java.io.File("src/main/assets/$name")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: error("لم يوجد الأصل $name في مسار الاختبار")
        return file.readText()
    }

    @Test
    fun theReleaseKeyAssetIsEitherEmptyOrAUsableKeyNeverATemplateThatPasses() {
        val material = com.baynana.core.license.LicenseSignatures.extractKeyMaterial(asset(ReleaseSignatures.PUBLIC_KEY_ASSET))
        if (material == null) {
            // الحالة المعتادة اليوم: قالب بلا مفتاح. المهمّ أنه لا يمرّ كمفتاح.
            assertTrue(
                "قالب المفتاح لا يجوز أن يُنتج فاحصًا صالحًا",
                com.baynana.core.license.EcdsaSignatureVerifier(asset(ReleaseSignatures.PUBLIC_KEY_ASSET)).isUsable.not()
            )
        } else {
            assertTrue(
                "المفتاح الموجود في الأصول يجب أن يكون مفتاحًا عامًّا صالحًا (EC/X.509)",
                com.baynana.core.license.EcdsaSignatureVerifier(material).isUsable
            )
        }
    }

    @Test
    fun theReleaseKeyIsNotTheLicenseKeyByMistake() {
        // فصل المفتاحين مقصود: مفتاح التصاريح يقترب من يد من يستعمل اللوحة، ومفتاح التحديث يحمي
        // كل الأجهزة. فإن كانا ملفًّا واحدًا بنفس المحتوى، فالقرار نُقض بصمت.
        val license = com.baynana.core.license.LicenseSignatures.extractKeyMaterial(asset("license_public_key.txt"))
        val release = com.baynana.core.license.LicenseSignatures.extractKeyMaterial(asset(ReleaseSignatures.PUBLIC_KEY_ASSET))
        if (license != null && release != null) {
            assertTrue("مفتاح التحديث يجب ألا يساوي مفتاح التصاريح", license != release)
        }
    }

    @Test
    fun theEndpointTemplateIsNotAnAddress() {
        val endpoint = ReleaseChannel.endpoint(asset(ReleaseChannel.ENDPOINT_ASSET))
        assertNull("القالب الفارغ لا يُنتج عنوانًا يُفحص منه", endpoint)
    }

    @Test
    fun theEndpointGetsTheContractPathExactlyOnce() {
        assertEquals(
            "https://example.com/api/v1/app-release",
            ReleaseChannel.endpoint("https://example.com")
        )
        assertEquals(
            "https://example.com/api/v1/app-release",
            ReleaseChannel.endpoint("https://example.com/")
        )
        assertEquals(
            "https://example.com/api/v1/app-release",
            ReleaseChannel.endpoint("https://example.com/api/v1/app-release")
        )
        // تعليقات الملفّ تُتخطّى، وأول سطر فعّال هو العنوان.
        assertEquals(
            "https://example.com/api/v1/app-release",
            ReleaseChannel.endpoint("# تعليق\n\n  https://example.com  \n# تعليق آخر")
        )
    }

    @Test
    fun anUnencryptedEndpointIsRefused() {
        assertNull(ReleaseChannel.endpoint("http://example.com"))
        assertNull(ReleaseChannel.endpoint("ftp://example.com"))
        assertNull(ReleaseChannel.endpoint("  "))
    }
}

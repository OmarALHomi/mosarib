package com.baynana.domain.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * صيغة التصريح: تُقرأ كما تُكتب، أو تُرفض بصراحة — بلا تخمين ولا ترميم.
 *
 * الفحص هنا على **القراءة** وحدها (بلا توقيع ولا قاعدة): القراءة والتحقق مسؤوليتان، واختبار
 * كل واحدة وحده هو ما يجعل الفشل مفهومًا عند حدوثه.
 */
class LicenseTokenTest {

    private val validGrant = LicenseToken.Grant(
        licenseId = "lic-2026-0001-abcd",
        deviceCode = "MSRB-8F42-9D1B",
        role = "MUSRIB",
        plan = "MONTHLY",
        durationDays = 30,
        issuedAt = 1_760_000_000_000L,
        expiresAt = 1_762_592_000_000L
    )

    @Test
    fun payloadIsCanonicalAndRoundTrips() {
        val text = LicenseToken.payloadText(validGrant)
        val decoded = LicenseToken.decodeGrant(text)
        // كود الجهاز يُقرأ مطبَّعًا (بلا فواصل)، وهذا هو الشكل الذي يُقارن به مع كود الجهاز الحقيقي.
        assertEquals(validGrant.copy(deviceCode = "MSRB8F429D1B"), decoded)
        assertEquals(
            "الحمولة تُكتب بالترتيب نفسه دائمًا، لأن التوقيع عليها بالحرف",
            "1|lic-2026-0001-abcd|MSRB8F429D1B|MUSRIB|MONTHLY|30|1760000000000|1762592000000",
            text
        )
    }

    @Test
    fun deviceCodeIsNormalisedSoPrintedFormMatchesTypedForm() {
        assertEquals("MSRB8F429D1B", LicenseToken.normalizeDeviceCode("msrb-8f42-9d1b"))
        assertEquals("MSRB8F429D1B", LicenseToken.normalizeDeviceCode(" MSRB 8F42 9D1B "))
    }

    @Test
    fun encodedTokenCarriesThreePartsAndParsesBack() {
        val payload = LicenseToken.payloadText(validGrant)
        val signature = ByteArray(64) { it.toByte() }
        val token = LicenseToken.encode(payload, signature)

        assertTrue(token.startsWith("BNNA1."))
        val parsed = LicenseToken.parse(token)
        assertTrue("التصريح الصحيح يُقرأ", parsed is LicenseToken.Parsed.Signed)
        val signed = parsed as LicenseToken.Parsed.Signed
        assertEquals(payload, signed.payloadText)
        assertEquals(64, signed.signatureBytes.size)
        assertEquals(validGrant.copy(deviceCode = "MSRB8F429D1B"), LicenseToken.decodeGrant(signed.payloadText))
    }

    @Test
    fun legacyKeyIsNotMistakenForAToken() {
        assertFalse(LicenseToken.looksLikeToken("ACTV-M-8F42-9D1B"))
        assertTrue(LicenseToken.parse("ACTV-M-8F42-9D1B") is LicenseToken.Parsed.NotAToken)
    }

    @Test
    fun malformedTokensAreRejectedOneByOne() {
        val cases = mapOf(
            "جزءان فقط" to "BNNA1.YWJjZA",
            "أربعة أجزاء" to "BNNA1.a.b.c",
            "حمولة فارغة" to "BNNA1..YWJjZA",
            "توقيع فارغ" to "BNNA1.YWJjZA.",
            "حمولة ليست base64" to "BNNA1.!!!.YWJjZGVmZ2hp",
            "توقيع قصير جدًا" to "BNNA1.YWJjZGVmZ2hp.YWJj",
            "حمولة بلا فواصل" to "BNNA1.${Base64Codec.encodeUrl("لاشيء".toByteArray())}.YWJjZGVmZ2hpamts"
        )
        cases.forEach { (name, token) ->
            val parsed = LicenseToken.parse(token)
            assertTrue(
                "$name يجب أن يُرفض كمشوّه لا أن يُقبل",
                parsed is LicenseToken.Parsed.Malformed
            )
        }
    }

    @Test
    fun grantFieldsAreValidatedNotTrusted() {
        val base = LicenseToken.payloadText(validGrant)
        // تنبيه مقصود: `replaceFirst(String, String)` في Kotlin بحث نصّي لا regex، فـ"^1" لا تُبدّل
        // شيئًا. لذلك نبني نسخة الغد بإزاحة الحرف الأول صراحةً — وإلا مرّ فحص «نسخة مجهولة» بلا فحص.
        assertNull("نسخة حمولة مجهولة تُرفض", LicenseToken.decodeGrant("2" + base.drop(1)))
        assertNull("مدة صفرية تُرفض", LicenseToken.decodeGrant(base.replace("|30|", "|0|")))
        assertNull("مدة أطول من عقد تُرفض", LicenseToken.decodeGrant(base.replace("|30|", "|99999|")))
        assertNull("انتهاء قبل الإصدار يُرفض", LicenseToken.decodeGrant(base.replace("|1762592000000", "|1750000000000")))
        assertNull("معرّف قصير يُرفض", LicenseToken.decodeGrant(base.replace("lic-2026-0001-abcd", "li")))
        assertNull("مهنة بحروف صغيرة تُرفض", LicenseToken.decodeGrant(base.replace("|MUSRIB|", "|musrib|")))
        assertNull("حقل ناقص يُرفض", LicenseToken.decodeGrant(base.substringBeforeLast("|")))
        assertNull("حقل زائد يُرفض", LicenseToken.decodeGrant(base + "|extra"))
    }

    @Test
    fun redemptionKeysAreDistinctPerKindAndStable() {
        val signed = LicenseToken.redemptionKey(validGrant)
        assertEquals("signed:lic-2026-0001-abcd", signed)
        assertEquals("legacy:abc123", LicenseToken.legacyRedemptionKey("abc123"))
        assertTrue("مفتاح التصريح لا يلتبس بمفتاح قديم", signed != LicenseToken.legacyRedemptionKey("lic-2026-0001-abcd"))
    }

    @Test
    fun base64AcceptsBothAlphabetsAndRejectsGarbage() {
        val bytes = byteArrayOf(-1, -2, 0, 1, 2, 3, 120, 121, 122)
        val url = Base64Codec.encodeUrl(bytes)
        assertTrue("الترميز بلا حشو", !url.contains("="))
        assertEquals("فكّ ما رمّزناه يعيد الأصل", bytes.toList(), Base64Codec.decode(url)!!.toList())
        assertNull("رمز غريب يُرفض", Base64Codec.decode("؟؟؟"))
        assertEquals("الفراغات والحشو تُتجاهل", bytes.toList(), Base64Codec.decode("  $url=  ")!!.toList())
    }
}

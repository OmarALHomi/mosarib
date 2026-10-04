package com.baynana.domain.license

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * صيغتا التوقيع: التحويل بينهما يجب أن يكون تامًّا، لأن الخطأ فيه لا يظهر كخطأ برمجة بل كـ«توقيع
 * مزوّر» على كل تصريح صحيح. نتحقق من ثلاثة أوجه:
 * 1. التحويل ذهابًا وإيابًا يعيد القيمة نفسها.
 * 2. توقيع WebCrypto المستطيل (P1363) يُقبل بعد تحويله إلى DER.
 * 3. متجه حقيقي موقّع بـ`SHA256withECDSA` يُفكّ إلى مستطيل ثم يُعاد تركيبه فيُتحقق منه.
 */
class EcdsaSignatureFormatTest {

    private fun newKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }

    @Test
    fun rawAndDerRoundTripForExtremeValues() {
        val cases = listOf(
            ByteArray(64) { 1 },
            ByteArray(64) { -1 },
            ByteArray(64) { 0 }.also { it[0] = 0x7F; it[31] = 0x01; it[32] = 0x80.toByte(); it[63] = 0x02 },
            ByteArray(64) { 0 }.also { it[31] = 0x01; it[63] = 1 }
        )
        cases.forEach { raw ->
            val der = EcdsaSignatureFormat.rawToDer(raw)
            val back = EcdsaSignatureFormat.derToRaw(der)
            assertNotNull("كل توقيع مستطيل يُحوَّل إلى DER ويُفكّ", back)
            assertEquals(
                "التحويل ذهابًا وإيابًا لا يفقد بايتًا",
                raw.toList(),
                back!!.toList()
            )
        }
    }

    @Test
    fun derEncodingUsesMinimalIntegersAsAsn1Requires() {
        val raw = ByteArray(64) { 0 }.also {
            it[31] = 0x01          // r = 1
            it[63] = 0x80.toByte() // s عالي البت ⇒ يسبقه صفر لئلا يُفهم سالبًا
        }
        val der = EcdsaSignatureFormat.rawToDer(raw)
        assertEquals(0x30, der[0].toInt() and 0xFF)
        assertEquals(0x02, der[2].toInt() and 0xFF)
        assertEquals("طول r ببايت واحد", 1, der[3].toInt())
        assertEquals("وقيمة r = 1 بلا أصفار زائدة", 1, der[4].toInt())
        val sIndex = 5
        assertEquals(0x02, der[sIndex].toInt() and 0xFF)
        assertEquals("s يُكتب ببايتين مع صفر الإشارة", 2, der[sIndex + 1].toInt())
        assertEquals(0x00, der[sIndex + 2].toInt())
        assertEquals(0x80, der[sIndex + 3].toInt() and 0xFF)
    }

    @Test
    fun malformedDerIsRejectedInsteadOfGuessed() {
        assertNull("بايت واحد لا يكفي", EcdsaSignatureFormat.derToRaw(byteArrayOf(0x30)))
        assertNull("وسم خاطئ يُرفض", EcdsaSignatureFormat.derToRaw(byteArrayOf(0x31, 0x00)))
        assertNull("طول يتجاوز المحتوى يُرفض", EcdsaSignatureFormat.derToRaw(byteArrayOf(0x30, 0x7F, 0x02, 0x01)))
        assertNull(
            "عدد صحيح فارغ ليس DER صالحًا",
            EcdsaSignatureFormat.derToRaw(byteArrayOf(0x30, 0x04, 0x02, 0x01, 0x01, 0x02, 0x00))
        )
        assertNull(
            "عدد صحيح واحد ليس توقيعًا",
            EcdsaSignatureFormat.derToRaw(byteArrayOf(0x30, 0x03, 0x02, 0x01, 0x01))
        )
    }

    @Test
    fun webCryptoRawSignatureVerifiesAfterConversion() {
        val keys = newKeyPair()
        val payload = "1|lic-1|MSRB8F429D1B|MUSRIB|MONTHLY|30|1|2".toByteArray(Charsets.UTF_8)

        // ما يفعله المتصفح: توقيع مستطيل (r||s).
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keys.private)
        signer.update(payload)
        val der = signer.sign()
        val raw = EcdsaSignatureFormat.derToRaw(der)
        assertNotNull("توقيع جافا DER يُفكّ إلى مستطيل", raw)

        // ما يفعله التطبيق: يستقبل المستطيل فيحوّله ثم يتحقق.
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(keys.public)
        verifier.update(payload)
        assertTrue(
            "توقيع WebCrypto يُقبل بعد التحويل (وهذا ما كان سيفشل لو مرّرناه كما هو)",
            verifier.verify(EcdsaSignatureFormat.rawToDer(raw!!))
        )
    }

    @Test
    fun convertedSignatureStillFailsForTamperedPayload() {
        val keys = newKeyPair()
        val payload = "1|lic-1|MSRB8F429D1B|MUSRIB|MONTHLY|30|1|2".toByteArray(Charsets.UTF_8)
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(keys.private)
        signer.update(payload)
        val raw = EcdsaSignatureFormat.derToRaw(signer.sign())!!

        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(keys.public)
        verifier.update("1|lic-1|MSRB8F429D1B|MUSRIB|LIFETIME|3650|1|2".toByteArray(Charsets.UTF_8))
        assertTrue(
            "تحويل الصيغة لا يُضعف التحقق: أي تغيير في الحمولة يُرفض",
            !verifier.verify(EcdsaSignatureFormat.rawToDer(raw))
        )
    }
}

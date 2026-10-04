package com.baynana.domain.license

/**
 * صيغتا توقيع ECDSA — والفرق بينهما كلّف أنظمة كثيرة أخطاءً صامتة:
 *
 * - **P1363 (المستطيلة):** `r || s` كل واحد ٣٢ بايت بالضبط. هذه صيغة WebCrypto في المتصفح
 *   (وهي ما توقّع به لوحة الأدمن).
 * - **DER (ASN.1):** `SEQUENCE { INTEGER r, INTEGER s }` بحذف الأصفار البادئة، وهو ما يتوقّعه
 *   `Signature.getInstance("SHA256withECDSA")` في جافا/Android افتراضيًا.
 *
 * فنكتب التحويل هنا مرة واحدة (نقيًا، بلا مكتبات) حتى يقبل الفاحص ما توقّعه المتصفح: لو مرّرنا
 * توقيع المستطيل إلى محقق DER لفشل كل تصريح صحيح، ولظهر العطل كأنه «توقيع مزوّر» — وهو أسوأ
 * تشخيص ممكن. والاختبار يقارن التحويلين ذهابًا وإيابًا ويستعمل متجهًا حقيقيًا.
 */
object EcdsaSignatureFormat {

    /** طول توقيع P1363 لمنحنى P-256: ٣٢ بايت لـr و٣٢ بايت لـs. */
    const val RAW_P1363_LENGTH = 64

    private const val TAG_SEQUENCE = 0x30
    private const val TAG_INTEGER = 0x02
    private const val HALF = RAW_P1363_LENGTH / 2

    /** هل التوقيع بصيغة WebCrypto المستطيلة؟ (هي وحدها ما يحتاج تحويلًا) */
    fun isRawP1363(signatureBytes: ByteArray): Boolean = signatureBytes.size == RAW_P1363_LENGTH

    /** `r || s` ← `SEQUENCE { INTEGER r, INTEGER s }`. */
    fun rawToDer(raw: ByteArray): ByteArray {
        require(raw.size == RAW_P1363_LENGTH) {
            "توقيع P1363 يجب أن يكون $RAW_P1363_LENGTH بايت، والموجود ${raw.size}."
        }
        val r = derInteger(raw.copyOfRange(0, HALF))
        val s = derInteger(raw.copyOfRange(HALF, RAW_P1363_LENGTH))
        return derTagged(TAG_SEQUENCE, r + s)
    }

    /** `SEQUENCE { INTEGER r, INTEGER s }` ← `r || s`، أو `null` إن لم يكن DER صالحًا. */
    fun derToRaw(der: ByteArray): ByteArray? {
        val sequence = readTagged(der, 0, TAG_SEQUENCE) ?: return null
        val r = readTagged(sequence.content, 0, TAG_INTEGER) ?: return null
        val s = readTagged(sequence.content, r.next, TAG_INTEGER) ?: return null
        if (s.next != sequence.content.size) return null
        return leftPad32(r.content) + leftPad32(s.content)
    }

    /** مقطع مقروء من DER: محتواه، وموضع ما بعده. */
    private class Element(val content: ByteArray, val next: Int)

    private fun readTagged(bytes: ByteArray, from: Int, expectedTag: Int): Element? {
        if (from >= bytes.size) return null
        if ((bytes[from].toInt() and 0xFF) != expectedTag) return null

        var index = from + 1
        if (index >= bytes.size) return null
        val first = bytes[index].toInt() and 0xFF
        index += 1
        val length = if (first < 0x80) {
            first
        } else {
            val lengthBytes = first and 0x7F
            if (lengthBytes == 0 || lengthBytes > 4) return null
            var value = 0
            repeat(lengthBytes) {
                if (index >= bytes.size) return null
                value = (value shl 8) or (bytes[index].toInt() and 0xFF)
                index += 1
            }
            value
        }
        if (index + length > bytes.size) return null
        // عدد صحيح بلا محتوى ليس DER صالحًا، وقبوله يعني ترميم ما يجب رفضه.
        if (length == 0) return null
        return Element(bytes.copyOfRange(index, index + length), index + length)
    }

    private fun derInteger(value: ByteArray): ByteArray {
        var start = 0
        while (start < value.size - 1 && value[start] == 0.toByte()) start += 1
        var trimmed = value.copyOfRange(start, value.size)
        // الأصفار البادئة تُحذف، وبيت الصفر يُضاف فقط إن كانت الإشارة ستُفهم سالبة.
        if (trimmed.isNotEmpty() && (trimmed[0].toInt() and 0x80) != 0) {
            trimmed = byteArrayOf(0) + trimmed
        }
        return derTagged(TAG_INTEGER, trimmed)
    }

    private fun derTagged(tag: Int, content: ByteArray): ByteArray {
        val header = when {
            content.size < 0x80 -> byteArrayOf(tag.toByte(), content.size.toByte())
            content.size < 0x100 ->
                byteArrayOf(tag.toByte(), 0x81.toByte(), content.size.toByte())
            else ->
                byteArrayOf(
                    tag.toByte(),
                    0x82.toByte(),
                    (content.size shr 8).toByte(),
                    (content.size and 0xFF).toByte()
                )
        }
        return header + content
    }

    private fun leftPad32(value: ByteArray): ByteArray {
        var start = 0
        while (start < value.size - 1 && value[start] == 0.toByte()) start += 1
        val trimmed = value.copyOfRange(start, value.size)
        if (trimmed.size > HALF) return trimmed.copyOfRange(trimmed.size - HALF, trimmed.size)
        val out = ByteArray(HALF)
        trimmed.copyInto(out, HALF - trimmed.size)
        return out
    }
}

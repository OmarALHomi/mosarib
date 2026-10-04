package com.baynana.domain.license

/**
 * ترميز base64/nase64url مكتوب هنا بلا اعتماد على `android.util.Base64` ولا على `java.util.Base64`
 * (الأول لا يوجد في طبقة domain، والثاني يحتاج API 26 والتطبيق يدعم من 24)، وبلا مكتبة خارجية.
 *
 * لماذا نكتبه بأيدينا؟ لأن التصريح الموقّع (ح١٣) يُنقل نصًّا في واتساب ورمز QR، ويجب أن يفهمه
 * كل الأطراف بالترميز نفسه: الفاحص في التطبيق، والموقِّع في لوحة الأدمن (WebCrypto)، وأي أداة
 * تحقق مستقلة. والقاعدة الواحدة أوضح من ثلاثة ترميزات متشابهة تختلف في وضع الحشو.
 *
 * المفكّك يقبل الأبجديتين (`-_` و`+/`) ويتجاهل الحشو والفراغات، لأنه يُستعمل أيضًا لقراءة
 * المفتاح العام بصيغة PEM (وهي base64 قياسي بأسطر).
 */
object Base64Codec {

    private const val URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /** ترميز بلا حشو (`=`)، لأن الرمز يُنسخ بالإصبع ويُقرأ من كاميرا. */
    fun encodeUrl(bytes: ByteArray): String {
        val out = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index + 3 <= bytes.size) {
            val b0 = bytes[index].toInt() and 0xFF
            val b1 = bytes[index + 1].toInt() and 0xFF
            val b2 = bytes[index + 2].toInt() and 0xFF
            out.append(URL_ALPHABET[b0 shr 2])
            out.append(URL_ALPHABET[((b0 and 0x03) shl 4) or (b1 shr 4)])
            out.append(URL_ALPHABET[((b1 and 0x0F) shl 2) or (b2 shr 6)])
            out.append(URL_ALPHABET[b2 and 0x3F])
            index += 3
        }
        when (bytes.size - index) {
            1 -> {
                val b0 = bytes[index].toInt() and 0xFF
                out.append(URL_ALPHABET[b0 shr 2])
                out.append(URL_ALPHABET[(b0 and 0x03) shl 4])
            }
            2 -> {
                val b0 = bytes[index].toInt() and 0xFF
                val b1 = bytes[index + 1].toInt() and 0xFF
                out.append(URL_ALPHABET[b0 shr 2])
                out.append(URL_ALPHABET[((b0 and 0x03) shl 4) or (b1 shr 4)])
                out.append(URL_ALPHABET[(b1 and 0x0F) shl 2])
            }
        }
        return out.toString()
    }

    /** فكّ الترميز، أو `null` إن كان النصّ ليس base64 أصلًا (لا نُخمّن ولا نتسامح). */
    fun decode(text: String): ByteArray? {
        val cleaned = StringBuilder(text.length)
        for (ch in text) {
            if (!ch.isWhitespace() && ch != '=') cleaned.append(ch)
        }
        if (cleaned.isEmpty()) return ByteArray(0)
        val remainder = cleaned.length % 4
        if (remainder == 1) return null
        val size = cleaned.length / 4 * 3 + when (remainder) {
            2 -> 1
            3 -> 2
            else -> 0
        }
        val out = ByteArray(size)
        var outIndex = 0
        var buffer = 0
        var bits = 0
        for (ch in cleaned) {
            val value = valueOf(ch) ?: return null
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out[outIndex] = ((buffer shr bits) and 0xFF).toByte()
                outIndex += 1
            }
        }
        return out.copyOf(outIndex)
    }

    private fun valueOf(ch: Char): Int? = when (ch) {
        in 'A'..'Z' -> ch - 'A'
        in 'a'..'z' -> ch - 'a' + 26
        in '0'..'9' -> ch - '0' + 52
        '-', '+' -> 62
        '_', '/' -> 63
        else -> null
    }
}

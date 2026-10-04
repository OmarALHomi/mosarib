package com.baynana.domain.handover

import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * حزمة التسليم بلا خادم (ح١٩): النقل اليدوي عبر واتساب أو أي تطبيق مشاركة، بلا إنترنت ولا حساب.
 *
 * **لماذا نصّ لا ZIP ولا قاعدة**: الملف يمرّ في محادثة واتساب تُقتطع فيها الملفات الثنائية أحيانًا،
 * ويُقرأ بالعين عند الشك. فالصيغة نصّ سطري: سطر رأس، ثم سطر لكل حركة، ثم بصمة. وكل حقل قد يحمل
 * عربية (وصف القيد، اسم الغرفة) يُرمَّز base64url فلا يكسر السطر ولا يفقد حرفًا.
 *
 * **ما لا تفعله هذه الطبقة**: لا تُقرّر صلاحية أحد، ولا تمس المال. هي تغليف وفكّ وفحص سلامة فقط.
 * والقرار — من يحقّ له الكتابة في أي غرفة، وهل هذا القيد مقبول — يبقى في المستودع ومحرّك المزامنة،
 * حيث قواعد §8 ونموذج الأطراف. ولهذا تُستورد حزمة مجهولة المصدر إلى **مسار التطبيق نفسه**، فتخضع
 * لكل الفحوص التي يخضع لها أي تغيير قادم من الشبكة.
 *
 * **البصمة ليست توقيعًا**: `digest` يكشف قطعًا أو تعديلًا عابرًا (نقل ناقص، تحرير بالخطأ)، ولا يمنع
 * مَن يعيد كتابة الملف كاملًا من إعادة حساب البصمة. الحماية الحقيقية: كل قيد يحمل `operationId`
 * فريدًا وكاتبًا معلومًا وعضوية غرفة، والتكرار يُرفض، والإقرار لا يُنشئ مالًا — فلا يستطيع طرف
 * غريب أن «يخترع» دَينًا على أحد، ولا أن يمدّد استحقاقًا، ولا أن يقرّ عن غيره.
 */
object HandoverBundleCodec {

    const val MAGIC = "BAYNANA-HANDOVER"
    const val VERSION = 1

    /** سقف الرمز النصّي المضغوط: ما فوقه يُشارَك كملف، ولا يُقتطع صامتًا. */
    const val MAX_COMPACT_CODE_LENGTH = 6000

    /**
     * سقف عدد عناصر الحزمة المستقبَلة. السبب ليس الشكل بل الحماية: ملفّ كبير (بقصد أو بخطأ)
     * يُدخل آلاف الصفوف في قاعدة الجهاز المستقبِل. فوق السقف نرفض الملفّ كاملًا بسببه العربي —
     * والطريق الصحيح موجود: يُقسم إلى حزم أصغر، ولكل حزمة بصمتها وسجلّها.
     */
    const val MAX_ITEMS = 2000

    fun encode(bundle: HandoverBundle): String {
        val body = buildString {
            append(MAGIC).append(' ').append(VERSION).append('\n')
            append("bundle ").append(bundle.bundleId).append('\n')
            append("created ").append(bundle.createdAt).append('\n')
            append("device ").append(Base64Url.encode(bundle.deviceCode.toByteArray(Charsets.UTF_8))).append('\n')
            append("db ").append(bundle.dbVersion).append('\n')
            append("rooms ").append(bundle.rooms.joinToString(",") { Base64Url.encode(it.toByteArray(Charsets.UTF_8)) }).append('\n')
            append("items ").append(bundle.items.size).append('\n')
            bundle.items.forEach { item ->
                append("item ")
                append(item.createdAt).append(' ')
                append(Base64Url.encode(item.operationId.toByteArray(Charsets.UTF_8))).append(' ')
                append(Base64Url.encode(item.entityType.toByteArray(Charsets.UTF_8))).append(' ')
                append(Base64Url.encode(item.entityId.toByteArray(Charsets.UTF_8))).append(' ')
                append(Base64Url.encode(item.action.toByteArray(Charsets.UTF_8))).append(' ')
                append(Base64Url.encode(item.payload.toByteArray(Charsets.UTF_8))).append('\n')
            }
        }
        return body + "digest " + digestOf(body) + "\n"
    }

    /** البصمة تُحسب على **كل ما سبق سطر البصمة** حرفًا بحرف، فتكشف أي تحريف. */
    fun digestOf(textBeforeDigest: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(textBeforeDigest.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun parse(text: String): BundleParse {
        val lines = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n').lines()
        if (lines.isEmpty()) return BundleParse.Refused(BundleRefusal.NOT_A_BUNDLE)
        val header = lines.first().trim().split(' ')
        if (header.size != 2 || header[0] != MAGIC) return BundleParse.Refused(BundleRefusal.NOT_A_BUNDLE)
        val version = header[1].toIntOrNull() ?: return BundleParse.Refused(BundleRefusal.NOT_A_BUNDLE)
        if (version < 1) return BundleParse.Refused(BundleRefusal.NOT_A_BUNDLE)
        if (version > VERSION) return BundleParse.Refused(BundleRefusal.UNSUPPORTED_VERSION)

        // سطر البصمة هو **آخر سطر من الحزمة**، وما بعده نصّ لا يخصّها (كلام يُضاف في المحادثة)؛
        // يُهمل ولا يُطبَّق، ويُذكر في التقرير حتى لا يظنّ أحد أن شيئًا منه دخل.
        val digestIndex = lines.indexOfFirst { it.trim().startsWith("digest ") }
        if (digestIndex < 0) return BundleParse.Refused(BundleRefusal.TRUNCATED)
        val trailingText = lines.drop(digestIndex + 1).any { it.isNotBlank() }
        val claimed = lines[digestIndex].trim().removePrefix("digest ").trim()
        val contentLines = lines.take(digestIndex)
        val body = contentLines.joinToString("\n") + "\n"
        if (digestOf(body) != claimed) return BundleParse.Refused(BundleRefusal.TAMPERED)

        var bundleId = ""
        var createdAt = 0L
        var device = ""
        var dbVersion = 0
        var rooms = emptyList<String>()
        var expectedItems = -1
        val items = mutableListOf<HandoverItem>()

        contentLines.drop(1).forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            when {
                line.startsWith("bundle ") -> bundleId = line.removePrefix("bundle ").trim()
                line.startsWith("created ") -> createdAt = line.removePrefix("created ").trim().toLongOrNull()
                    ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                line.startsWith("device ") -> {
                    val raw = Base64Url.decode(line.removePrefix("device ").trim()) ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                    device = String(raw, Charsets.UTF_8)
                }
                line.startsWith("db ") -> dbVersion = line.removePrefix("db ").trim().toIntOrNull()
                    ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                line.startsWith("rooms ") -> {
                    rooms = line.removePrefix("rooms ").trim().split(',')
                        .filter { it.isNotBlank() }
                        .map { encoded ->
                            val raw = Base64Url.decode(encoded) ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                            String(raw, Charsets.UTF_8)
                        }
                }
                line.startsWith("items ") -> expectedItems = line.removePrefix("items ").trim().toIntOrNull()
                    ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                line.startsWith("item ") -> {
                    val parts = line.removePrefix("item ").trim().split(' ')
                    if (parts.size != 6) return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                    val createdAtItem = parts[0].toLongOrNull() ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                    val fields = parts.drop(1).map {
                        val raw = Base64Url.decode(it) ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
                        String(raw, Charsets.UTF_8)
                    }
                    items += HandoverItem(
                        operationId = fields[0],
                        entityType = fields[1],
                        entityId = fields[2],
                        action = fields[3],
                        payload = fields[4],
                        createdAt = createdAtItem
                    )
                }
                else -> return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
            }
        }

        if (items.size > MAX_ITEMS) return BundleParse.Refused(BundleRefusal.TOO_LARGE)
        if (bundleId.isBlank()) return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
        if (expectedItems < 0 || expectedItems != items.size) return BundleParse.Refused(BundleRefusal.TRUNCATED)
        if (items.isEmpty()) return BundleParse.Refused(BundleRefusal.EMPTY)
        return BundleParse.Parsed(
            HandoverBundle(
                bundleId = bundleId,
                createdAt = createdAt,
                deviceCode = device,
                dbVersion = dbVersion,
                rooms = rooms,
                items = items,
                hasTrailingText = trailingText
            )
        )
    }

    /**
     * رمز نصّي مضغوط لنفس الحزمة: يُلصق في واتساب كنصّ فيصل بلا مرفق. لا يُقتطع صامتًا: إن تجاوز
     * الحدّ يُرجع `null` وعلى المنادي أن يقول للمستخدم إن الملف هو الطريق.
     *
     * البادئة `BNNH1.` **مختلفة قصدًا** عن بادئة التصاريح `BNNA1.`: الاثنتان تمرّان في نفس المحادثة،
     * وخلطهما يجعل مستخدمًا يُرسل تصريحه مكان حزمة أو العكس. وحاجز البناء يمنع تكرار أي من
     * البادئتين خارج ملفّها.
     */
    fun compactCode(bundle: HandoverBundle): String? {
        val deflated = deflate(encode(bundle).toByteArray(Charsets.UTF_8)) ?: return null
        val code = "BNNH1." + Base64Url.encode(deflated)
        return if (code.length > MAX_COMPACT_CODE_LENGTH) null else code
    }

    fun parseCompactCode(code: String): BundleParse {
        val trimmed = code.trim()
        if (!trimmed.startsWith("BNNH1.")) return BundleParse.Refused(BundleRefusal.NOT_A_BUNDLE)
        val raw = Base64Url.decode(trimmed.removePrefix("BNNH1.")) ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
        val inflated = inflate(raw) ?: return BundleParse.Refused(BundleRefusal.MALFORMED_ITEM)
        return parse(String(inflated, Charsets.UTF_8))
    }

    /** هل يبدو النصّ حزمةً أو رمزًا مضغوطًا؟ للتمييز بين «لصق حركة» و«لصق كلام». */
    fun looksLikeHandover(text: String): Boolean {
        val trimmed = text.trimStart()
        return trimmed.startsWith(MAGIC) || trimmed.startsWith("BNNH1.")
    }

    /** فكّ أي من الصيغتين بمدخل واحد. */
    fun parseAny(text: String): BundleParse =
        if (text.trimStart().startsWith("BNNH1.")) parseCompactCode(text) else parse(text)

    private fun deflate(bytes: ByteArray): ByteArray? = try {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(bytes)
        deflater.finish()
        val out = ByteArrayOutputStream(bytes.size / 2 + 64)
        val buffer = ByteArray(4096)
        while (!deflater.finished()) {
            val written = deflater.deflate(buffer)
            if (written <= 0) break
            out.write(buffer, 0, written)
        }
        deflater.end()
        out.toByteArray()
    } catch (_: Exception) {
        null
    }

    private fun inflate(bytes: ByteArray): ByteArray? = try {
        val inflater = Inflater()
        inflater.setInput(bytes)
        val out = ByteArrayOutputStream(bytes.size * 3 + 64)
        val buffer = ByteArray(4096)
        while (!inflater.finished()) {
            val written = inflater.inflate(buffer)
            if (written == 0) {
                if (inflater.needsInput() || inflater.needsDictionary()) break
            } else {
                out.write(buffer, 0, written)
            }
        }
        inflater.end()
        out.toByteArray()
    } catch (_: Exception) {
        null
    }
}

/** حزمة تسليم: ما خرج من جهاز وما دخل إلى جهاز آخر. */
data class HandoverBundle(
    val bundleId: String,
    val createdAt: Long,
    val deviceCode: String,
    val dbVersion: Int,
    val rooms: List<String>,
    val items: List<HandoverItem>,
    /**
     * هل وُجد نصّ بعد سطر البصمة؟ لا يدخل منه شيء في التطبيق، لكن يُذكر للمستخدم حتى لا يظن أن
     * كل ما لصقه استُورد.
     */
    val hasTrailingText: Boolean = false
)

/** عنصر حزمة: نسخة حرفية من صفّ صندوق الصادر، بلا إعادة تفسير لأي مبلغ (ADR-04). */
data class HandoverItem(
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payload: String,
    val createdAt: Long
)

sealed interface BundleParse {
    data class Parsed(val bundle: HandoverBundle) : BundleParse

    /** رفض معلن بسببه العربي، ولا يُطبَّق من الحزمة شيء. */
    data class Refused(val reason: BundleRefusal) : BundleParse
}

enum class BundleRefusal(val messageArabic: String) {
    NOT_A_BUNDLE("هذا ليس ملف تسليم من بيننا."),
    UNSUPPORTED_VERSION("الملف من نسخة أحدث من نسختك. حدّث التطبيق ثم أعد المحاولة."),
    TRUNCATED("الملف ناقص: عدد الحركات داخله لا يطابق ترويسته."),
    TAMPERED("الملف تغيّر بعد إنشائه (بصمته لا تطابق)، فلم يُقرأ منه شيء."),
    EMPTY("الملف سليم لكنه لا يحمل أي حركة."),
    MALFORMED_ITEM("في الملف سطر لا يُفهم، فلم يُستورد منه شيء — اطلب إعادة إرساله."),
    TOO_LARGE("الملف أكبر من حدّ الاستيراد الآمن. اقسم الحركات على حزم أصغر ثم أعد الإرسال.")
}

/**
 * base64url بلا حشو، مكتوب هنا صراحةً: `android.util.Base64` غير متاح في الطبقة النقية،
 * و`java.util.Base64` لا يعمل على أندرويد 7 (minSdk 24). والصيغة URL-safe حتى تمرّ في رابط
 * أو نصّ محادثة بلا تهريب.
 */
object Base64Url {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    fun encode(bytes: ByteArray): String {
        val builder = StringBuilder((bytes.size + 2) / 3 * 4)
        var index = 0
        while (index < bytes.size) {
            val b0 = bytes[index].toInt() and 0xFF
            val b1 = if (index + 1 < bytes.size) bytes[index + 1].toInt() and 0xFF else -1
            val b2 = if (index + 2 < bytes.size) bytes[index + 2].toInt() and 0xFF else -1
            builder.append(ALPHABET[b0 shr 2])
            builder.append(ALPHABET[((b0 and 0x03) shl 4) or (if (b1 >= 0) b1 shr 4 else 0)])
            if (b1 >= 0) builder.append(ALPHABET[((b1 and 0x0F) shl 2) or (if (b2 >= 0) b2 shr 6 else 0)])
            if (b2 >= 0) builder.append(ALPHABET[b2 and 0x3F])
            index += 3
        }
        return builder.toString()
    }

    fun decode(text: String): ByteArray? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ByteArray(0)
        val out = ByteArrayOutputStream(trimmed.length * 3 / 4 + 3)
        var buffer = 0
        var bits = 0
        trimmed.forEach { char ->
            val value = ALPHABET.indexOf(char)
            if (value < 0) return null
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}

package com.baynana.domain.handover

import com.baynana.data.local.handover.HandoverPayloads
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.data.local.profile.LocalProfile
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح١٩ على الصيغة نفسها (بلا أندرويد وبلا قاعدة): الصيغة النصّية هي عقد التسليم بلا خادم،
 * فلو انكسرت انكسر الطريق كله. وكل حالة هنا تقابل خطأً حقيقيًا في الاستعمال:
 *
 * 1. **الوصف العربي**: وصف فيه سطر جديد أو علامة تنصيص أو إيموجي يجب أن يعود حرفًا بحرف، وإلا
 *    فسد نصّ القيد عند الطرف الآخر (وهو نصّ يحتجّ به أمام الناس).
 * 2. **القطع**: واتساب يقصّ الملفات الكبيرة — فيجب أن يُكتشف ذلك ولا يُستورد نصف ملف.
 * 3. **التحريف**: البصمة تكشف أي تعديل ولو حرفًا واحدًا في أي سطر.
 * 4. **الكلام المرفق**: الناس يلصقون «هذا الملف أرسله لي فلان» بعده، ويجب ألا يفسد شيئًا.
 * 5. **نسخة أحدث**: جهاز قديم لا يستورد ملفًا لا يفهمه — يقولها صراحةً.
 */
class HandoverBundleCodecTest {

    private fun bundle(vararg items: HandoverItem, rooms: List<String> = listOf("room-1")) = HandoverBundle(
        bundleId = "bnn-test-1",
        createdAt = 1_700_000_000_000L,
        deviceCode = "MSRB-1234-ABCD",
        dbVersion = 13,
        rooms = rooms,
        items = items.toList()
    )

    private fun entryItem(
        payload: String = """{"v":1,"kind":"ENTRY","entry":{"id":"e1","roomId":"room-1","amountMinor":"1500000"}}""",
        operationId: String = "op-1"
    ) = HandoverItem(
        operationId = operationId,
        entityType = "entry",
        entityId = "e1",
        action = "UPSERT",
        payload = payload,
        createdAt = 1_700_000_000_000L
    )

    @Test
    fun arabicAndQuotesSurviveRoundTrip() {
        val payload = "{\"note\":\"سقية \\\"أبو أحمد\\\"\\nسطر ثانٍ\",\"emoji\":\"🌾\"}"
        val text = HandoverBundleCodec.encode(bundle(entryItem(payload = payload)))

        val parsed = HandoverBundleCodec.parse(text)
        assertTrue(parsed is BundleParse.Parsed)
        val result = (parsed as BundleParse.Parsed).bundle
        assertEquals(1, result.items.size)
        assertEquals(payload, result.items[0].payload)
        assertEquals("MSRB-1234-ABCD", result.deviceCode)
        assertEquals(listOf("room-1"), result.rooms)
        assertFalse(result.hasTrailingText)
    }

    @Test
    fun singleCharacterTamperIsDetected() {
        val text = HandoverBundleCodec.encode(bundle(entryItem()))
        // التحريف داخل حقل مُرمَّز (base64url): المبالغ والأوصاف كلها مُرمَّزة، فالبحث عن رقم صريح
        // في النصّ لا يصيب شيئًا ولا يفحص شيئًا. نغيّر حرفًا واحدًا في حمولة السطر الأول.
        val lines = text.trimEnd('\n').lines().toMutableList()
        val itemIndex = lines.indexOfFirst { it.startsWith("item ") }
        assertTrue("الحزمة فيها سطر حركة", itemIndex >= 0)
        val fields = lines[itemIndex].split(' ').toMutableList()
        val payloadField = fields.last()
        val flipped = (if (payloadField.first() == 'A') 'B' else 'A') + payloadField.drop(1)
        fields[fields.lastIndex] = flipped
        lines[itemIndex] = fields.joinToString(" ")
        val tampered = lines.joinToString("\n") + "\n"
        assertTrue("التحريف وقع فعلًا", tampered != text)

        val parsed = HandoverBundleCodec.parse(tampered)
        assertTrue(parsed is BundleParse.Refused)
        assertEquals(BundleRefusal.TAMPERED, (parsed as BundleParse.Refused).reason)
    }

    @Test
    fun cutFileIsRefusedNotHalfImported() {
        val text = HandoverBundleCodec.encode(bundle(entryItem(), entryItem(operationId = "op-2")))
        // قطع واقعي: وصل نصف الملف، فلم يصل سطر البصمة أصلًا.
        val cut = text.lines().dropLast(2).joinToString("\n")

        val parsed = HandoverBundleCodec.parse(cut)
        assertTrue(parsed is BundleParse.Refused)
        assertEquals(BundleRefusal.TRUNCATED, (parsed as BundleParse.Refused).reason)
    }

    @Test
    fun countMismatchIsRefusedEvenWithAValidDigest() {
        // حزمة تقول في ترويستها ٣ عناصر وفيها عنصران، وبصمتها صحيحة (كتبها مُرسل مخطئ). تُرفض
        // كاملةً ولا يُستورد منها شيء: نصف حزمة أخطر من لا حزمة.
        val text = HandoverBundleCodec.encode(bundle(entryItem(), entryItem(operationId = "op-2")))
        // البصمة تُحسب على كل ما سبق سطر البصمة. فنبني «البدن» الصحيح أولًا (بلا سطر البصمة ولا
        // سطر فارغ زائد من `lines()`)، ثم نُعلن فيه ٣ عناصر ونُوقّع من جديد — فالجواب المطلوب هو
        // رفض «حزمة ناقصة» لا رفض «تحريف».
        val bodyLines = text.trimEnd('\n').lines().toMutableList()
        assertEquals("digest ", bodyLines.removeAt(bodyLines.lastIndex).take(7))
        val itemsIndex = bodyLines.indexOfFirst { it.startsWith("items ") }
        assertEquals("items 2", bodyLines[itemsIndex])
        bodyLines[itemsIndex] = "items 3"
        val body = bodyLines.joinToString("\n") + "\n"
        val resigned = body + "digest " + HandoverBundleCodec.digestOf(body) + "\n"

        val parsed = HandoverBundleCodec.parse(resigned)
        assertTrue(parsed is BundleParse.Refused)
        assertEquals(BundleRefusal.TRUNCATED, (parsed as BundleParse.Refused).reason)
    }

    @Test
    fun chatterAfterTheDigestIsIgnoredButDeclared() {
        val text = HandoverBundleCodec.encode(bundle(entryItem()))
        val withChatter = text + "\nهذا الملف من أحمد، افتحه في بيننا واقبل الربط.\n\nأرسلت من جوالي"

        val parsed = HandoverBundleCodec.parse(withChatter)
        assertTrue(parsed is BundleParse.Parsed)
        val result = (parsed as BundleParse.Parsed).bundle
        assertEquals(1, result.items.size)
        assertTrue(result.hasTrailingText)
    }

    @Test
    fun textWithoutHeaderIsRefusedAsNotABundle() {
        val parsed = HandoverBundleCodec.parse("السلام عليكم، هذه حركاتي:\n1500000 ريال")
        assertTrue(parsed is BundleParse.Refused)
        assertEquals(BundleRefusal.NOT_A_BUNDLE, (parsed as BundleParse.Refused).reason)
        assertFalse(HandoverBundleCodec.looksLikeHandover("السلام عليكم"))
    }

    @Test
    fun newerVersionIsRefusedWithAClearReason() {
        val text = HandoverBundleCodec.encode(bundle(entryItem())).replace("BAYNANA-HANDOVER 1", "BAYNANA-HANDOVER 7")
        // البصمة تغيّرت بتغيّر السطر، فالرفض المتوقع هنا «النسخة» لا «التحريف»؛ نعيد حسابها كما
        // يفعل مُرسل أحدث نسخة، فيصير الرفض بسبب النسخة صراحةً.
        val body = text.lines().dropLast(1).joinToString("\n") + "\n"
        val resigned = body + "digest " + HandoverBundleCodec.digestOf(body) + "\n"

        val parsed = HandoverBundleCodec.parse(resigned)
        assertTrue(parsed is BundleParse.Refused)
        assertEquals(BundleRefusal.UNSUPPORTED_VERSION, (parsed as BundleParse.Refused).reason)
    }

    @Test
    fun emptyBundleIsRefusedRatherThanImportedAsNothing() {
        val text = HandoverBundleCodec.encode(bundle())
        val parsed = HandoverBundleCodec.parse(text)
        assertTrue(parsed is BundleParse.Refused)
        assertEquals(BundleRefusal.EMPTY, (parsed as BundleParse.Refused).reason)
    }

    @Test
    fun compactCodeCarriesTheSameBundle() {
        val text = HandoverBundleCodec.encode(bundle(entryItem(), entryItem(operationId = "op-2")))
        val code = HandoverBundleCodec.compactCode(bundle(entryItem(), entryItem(operationId = "op-2")))
        assertNotNull(code)
        assertTrue(HandoverBundleCodec.looksLikeHandover(code!!))
        assertTrue(code.length <= HandoverBundleCodec.MAX_COMPACT_CODE_LENGTH)

        val parsed = HandoverBundleCodec.parseCompactCode(code)
        assertTrue(parsed is BundleParse.Parsed)
        assertEquals(2, (parsed as BundleParse.Parsed).bundle.items.size)

        // ونفس النصّ المفكوك حرفيًا من الطريق الطويل: الرمز ليس صيغة ثانية، بل نفس الحزمة مضغوطة.
        assertEquals(
            (HandoverBundleCodec.parse(text) as BundleParse.Parsed).bundle.items.map { it.payload },
            (HandoverBundleCodec.parseAny(code) as BundleParse.Parsed).bundle.items.map { it.payload }
        )
    }

    @Test
    fun hugeBundleSaysLetFileNotCode() {
        // حمولة لا تُضغط (نصّ شبه عشوائي): الرمز المختصر يتجاوز حدّ المحادثة، فالجواب الصادق
        // «أرسلها ملفًّا» — ولا نقتطع الحزمة صامتًا لنُدخلها في الحدّ.
        val blob = "{\"blob\":\"" + noise(40_000) + "\"}"
        assertNull(HandoverBundleCodec.compactCode(bundle(entryItem(payload = blob))))

        // والطريق الطويل يبقى صالحًا: لا نُفقد أحدًا قدرة الإرسال، نغيّر الطريق فقط.
        val parsed = HandoverBundleCodec.parse(HandoverBundleCodec.encode(bundle(entryItem(payload = blob))))
        assertEquals(1, (parsed as BundleParse.Parsed).bundle.items.size)
    }

    private fun noise(length: Int): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val builder = StringBuilder(length)
        var state = 88_123_456_789L
        repeat(length) {
            state = state * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
            builder.append(alphabet[((state ushr 33).toInt() and Int.MAX_VALUE) % alphabet.length])
        }
        return builder.toString()
    }

    @Test
    fun oversizedBundleIsRefusedWholeWithAClearReason() {
        val tooMany = (1..HandoverBundleCodec.MAX_ITEMS + 1)
            .map { index -> entryItem(operationId = "op-$index") }
            .toTypedArray()
        val parsed = HandoverBundleCodec.parse(HandoverBundleCodec.encode(bundle(*tooMany)))
        assertTrue(parsed is BundleParse.Refused)
        assertEquals(BundleRefusal.TOO_LARGE, (parsed as BundleParse.Refused).reason)
        assertTrue(BundleRefusal.TOO_LARGE.messageArabic.contains("أصغر"))
    }

    @Test
    fun base64UrlRoundTripsBinarySafely() {
        val bytes = ByteArray(256) { (it and 0xFF).toByte() }
        val encoded = Base64Url.encode(bytes)
        assertFalse(encoded.contains('+') || encoded.contains('/') || encoded.contains('='))
        val decoded = Base64Url.decode(encoded)
        assertNotNull(decoded)
        assertEquals(256, decoded!!.size)
        assertEquals(0, decoded[0].toInt() and 0xFF)
        assertEquals(255, decoded[255].toInt() and 0xFF)
        assertNull(Base64Url.decode("ليس-صالحًا"))
    }

    /**
     * العلامة المحلية «أنا» تعني «صاحب هذا الجهاز» على جهازه وحده. وقد وقعت فعلًا في دعوة غرفة
     * فقرأ الطرف الآخر «من: أنا». هنا حارس الحمولة نفسها: تُنقّى عند البناء، وتُقرأ «غير معروف»
     * عند الاستلام، ولا يضيع نصيب صاحب الاسم الصالح.
     */
    @Test
    fun theLocalPlaceholderNeverTravelsInsideARoomInvitation() {
        val room = LedgerRoom(
            id = "room-1",
            kind = RoomKind.WATER,
            currency = "YER_NEW",
            title = "ريّ أبو أحمد",
            status = RoomStatus.PENDING,
            linkCode = "WATER-1234",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L
        )
        val members = listOf(
            RoomMember(
                roomId = "room-1",
                memberId = "me",
                displayName = LocalProfile.PLACEHOLDER,
                isMe = true,
                role = "owner",
                joinedAt = 1_700_000_000_000L
            ),
            RoomMember(
                roomId = "room-1",
                memberId = "counterpart-room-1",
                displayName = "أبو أحمد",
                role = "counterpart",
                joinedAt = 1_700_000_000_000L
            )
        )

        val payload = HandoverPayloads.roomInvitation(
            room = room,
            members = members,
            inviterMemberId = "me",
            partnerMemberId = "counterpart-room-1"
        )
        assertFalse("العلامة المحلية سافرت في الحمولة", payload.contains(LocalProfile.PLACEHOLDER))

        val decoded = HandoverPayloads.decodeRoomInvitation(payload)
        assertEquals(
            LocalProfile.UNKNOWN_LABEL,
            decoded.members.first { it.memberId == "me" }.displayName
        )
        assertEquals("أبو أحمد", decoded.members.first { it.memberId == "counterpart-room-1" }.displayName)
    }
}

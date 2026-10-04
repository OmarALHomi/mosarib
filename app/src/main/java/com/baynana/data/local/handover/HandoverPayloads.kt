package com.baynana.data.local.handover

import com.baynana.data.local.ledger.Acknowledgement
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.data.local.profile.LocalProfile
import com.baynana.domain.ledger.AckDecision
import com.baynana.domain.ledger.EntryStatus
import org.json.JSONArray
import org.json.JSONObject

/**
 * صيغة حمولة الحزمة اليدوية: **نفس مبادئ صيغة السلك** — نصّ JSON بإصدار معلن `v`، وكل مبلغ
 * **نصًّا بالوحدة الصغرى** (ADR-04)، ولا رقم عشري في أي مكان.
 *
 * وهنا فرق مقصود عن صندوق الصادر الآلي: الحزمة تحمل نوعين إضافيين لا يرسلهما محرّك القيود —
 * **الإقرار** (حتى يُقفل الطرفان على القيد نفسه) و**دعوة الغرفة** (حتى توجد الغرفة على الجهازين
 * بنفس المعرّفات، فيُنسب القيد إلى صاحبه الصحيح لا إلى «مجهول»).
 */
object HandoverPayloads {
    const val VERSION = 1

    const val KIND_ACK = "ACK"
    const val KIND_ROOM_INVITE = "ROOM_INVITE"

    // ------------------------------------------------------------------ الإقرار

    fun acknowledgement(ack: Acknowledgement, resultingStatus: String, roomId: String = ""): String = JSONObject()
        .put("v", VERSION)
        .put("kind", KIND_ACK)
        // `roomId` في الجذر مقصود: به تُجمَّع عناصر الحزمة بالغرفة، فيُحفظ الإقرار مؤجّلًا إن وصل
        // قبل غرفته ويُطبَّق لحظة قبولها — بدل أن يُرفض ويضيع.
        .apply { if (roomId.isNotBlank()) put("roomId", roomId) }
        .put("ack", JSONObject()
            .put("entryId", ack.entryId)
            .put("memberId", ack.memberId)
            .put("decision", ack.decision)
            .put("note", ack.note)
            .put("decidedAt", ack.decidedAt)
            .put("createdAt", ack.createdAt)
            .put("resultingStatus", resultingStatus)
        )
        .toString()

    /**
     * فكّ قرار وارد. الرفض هنا صريح: قرار غير معروف، أو حالة ناتجة لا توافق القرار، أو نصّ فارغ
     * لرفض بلا سبب — كلها تُرفض ولا تُطبَّق، لأن الإقرار بلا سبب لا يُصلح شيئًا.
     */
    fun decodeAcknowledgement(payload: String): DecodedAck {
        val root = JSONObject(payload)
        require(root.optString("kind", "") == KIND_ACK) { "نوع حمولة غير متوقع" }
        val ack = root.getJSONObject("ack")
        val decision = ack.getString("decision")
        require(decision in AckDecision.all) { "قرار غير معروف: $decision" }
        val note = ack.optString("note", "")
        require(decision == AckDecision.ACKNOWLEDGED || note.isNotBlank()) {
            "الرفض وطلب التعديل يحتاجان سببًا مكتوبًا"
        }
        val resulting = ack.optString("resultingStatus", AckDecision.resultingStatus(decision))
        require(resulting == AckDecision.resultingStatus(decision)) { "حالة ناتجة لا توافق القرار" }
        val declaredRoom = root.optString("roomId", "")
        require(resulting in listOf(
            EntryStatus.ACKNOWLEDGED, EntryStatus.DISPUTED, EntryStatus.CHANGE_REQUESTED
        )) { "حالة إقرار غير معروفة: $resulting" }
        return DecodedAck(
            roomId = declaredRoom,
            acknowledgement = Acknowledgement(
                entryId = ack.getString("entryId"),
                memberId = ack.getString("memberId"),
                decision = decision,
                note = note,
                decidedAt = ack.getLong("decidedAt"),
                createdAt = ack.optLong("createdAt", ack.getLong("decidedAt"))
            ),
            resultingStatus = resulting
        )
    }

    data class DecodedAck(
        val acknowledgement: Acknowledgement,
        val resultingStatus: String,
        /** الغرفة كما أعلنها المُرسل (قد تكون فارغة في حمولات قديمة) — تُقارَن بغرفة القيد. */
        val roomId: String = ""
    )

    // -------------------------------------------------------------- دعوة الغرفة

    fun roomInvitation(
        room: LedgerRoom,
        members: List<RoomMember>,
        inviterMemberId: String,
        partnerMemberId: String,
        note: String = ""
    ): String {
        val memberArray = JSONArray()
        members.forEach { member ->
            memberArray.put(
                JSONObject()
                    .put("memberId", member.memberId)
                    // العلامة المحلية «أنا» لا تُرسل: الطرف الآخر يقرأ اسمًا أو لا يقرأ شيئًا.
                    .put("displayName", LocalProfile.wireName(member.displayName))
                    .put("phone", member.phone)
                    .put("role", member.role)
                    .put("joinedAt", member.joinedAt)
            )
        }
        return JSONObject()
            .put("v", VERSION)
            .put("kind", KIND_ROOM_INVITE)
            .put("inviterMemberId", inviterMemberId)
            // قد يكون فارغًا في لقطة غرفة بثلاثة أطراف: حينها لا يوجد «طرف مقابل» واحد يُسمّى،
            // واللقطة تُدمج حالةً ولا تُنشئ غرفة.
            .put("partnerMemberId", partnerMemberId)
            .put("note", note)
            .put("room", JSONObject()
                .put("id", room.id)
                .put("kind", room.kind)
                .put("currency", room.currency)
                .put("title", room.title)
                // الحالة تُنقل صراحةً: بها يعرف المُرسل أن الربط قُبل (PENDING → ACTIVE)، أو أن
                // الغرفة أُغلقت بالتراضي (→ CLOSED). ولا تُنقل محذوفة أبدًا: لا حذف لغرفة فيها طرفان.
                .put("status", room.status)
                .put("linkCode", room.linkCode)
                .put("createdAt", room.createdAt)
                .put("updatedAt", room.updatedAt)
            )
            .put("members", memberArray)
            .toString()
    }

    /**
     * فكّ دعوة غرفة. الشروط الصارمة مقصودة: عملة معلنة، وطرفان اثنان على الأقل، وعضوية دعوة
     * صريحة للطرف المدعو (`partnerMemberId`) موجودة في القائمة. غرفة بلا طرفين ليست غرفة، ودعوة
     * بلا معرّف عضو للطرف الآخر لا يستطيع المستقبل أن يُنسب إليها فيصير كل قيد «مجهولًا».
     */
    fun decodeRoomInvitation(payload: String): DecodedInvite {
        val root = JSONObject(payload)
        require(root.optString("kind", "") == KIND_ROOM_INVITE) { "نوع حمولة غير متوقع" }
        val roomJson = root.getJSONObject("room")
        val currency = roomJson.getString("currency")
        require(currency.isNotBlank()) { "دعوة بلا عملة: لا تُعرف فيها أرقام" }
        val partnerMemberId = root.optString("partnerMemberId", "")
        val membersJson = root.getJSONArray("members")
        val members = (0 until membersJson.length()).map { index ->
            val row = membersJson.getJSONObject(index)
            RoomMember(
                roomId = roomJson.getString("id"),
                memberId = row.getString("memberId"),
                displayName = LocalProfile.peerLabel(row.optString("displayName", "")),
                phone = row.optString("phone", ""),
                role = row.optString("role", ""),
                isMe = false,
                joinedAt = row.optLong("joinedAt", System.currentTimeMillis()),
                lastSeenAt = 0L
            )
        }
        val inviterMemberId = root.getString("inviterMemberId")
        require(members.size >= 2) { "دعوة بغرفة بلا طرفين" }
        require(partnerMemberId.isEmpty() || members.any { it.memberId == partnerMemberId }) {
            "الدعوة تسمّي عضوًا ليس من أعضاء الغرفة"
        }
        require(members.none { it.memberId == partnerMemberId && it.memberId == inviterMemberId }) {
            "الدعوة تجعل الطرفين عضوًا واحدًا"
        }
        require(members.any { it.memberId == inviterMemberId }) { "الدعوة لا تسمّي صاحبها" }
        val status = roomJson.optString("status", com.baynana.domain.ledger.RoomStatus.PENDING)
        require(status in com.baynana.domain.ledger.RoomStatus.all) { "حالة غرفة غير معروفة: $status" }

        return DecodedInvite(
            room = LedgerRoom(
                id = roomJson.getString("id"),
                kind = roomJson.optString("kind", com.baynana.domain.ledger.RoomKind.GENERAL),
                currency = currency,
                title = roomJson.optString("title", ""),
                status = status,
                linkCode = roomJson.optString("linkCode", ""),
                counterpartName = roomJson.optString("title", ""),
                counterpartPhone = "",
                createdAt = roomJson.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = roomJson.optLong("updatedAt", System.currentTimeMillis())
            ),
            members = members,
            partnerMemberId = partnerMemberId,
            inviterMemberId = inviterMemberId,
            note = root.optString("note", "")
        )
    }

    data class DecodedInvite(
        val room: LedgerRoom,
        val members: List<RoomMember>,
        val partnerMemberId: String,
        val inviterMemberId: String,
        val note: String
    )

    /** حالة قيد مشروعة قادمة مع إقرار (حماية من حمولة تكتب حالة لا يعرفها المحرّك). */
    fun isKnownEntryStatus(status: String): Boolean =
        status == EntryStatus.ACKNOWLEDGED || status == EntryStatus.DISPUTED ||
            status == EntryStatus.CHANGE_REQUESTED || status == EntryStatus.SENT
}

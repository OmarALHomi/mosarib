package com.baynana.data.local.handover

import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.data.local.profile.LocalProfile
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * حارس اسم الداعي في دعوة الغرفة.
 *
 * لِمَ Robolectric؟ لأن `org.json` يأتي من بيئة أندرويد: في اختبار JVM عارٍ يرمي
 * «Method put in org.json.JSONObject not mocked». فالحمولة تُختبر حيث تُبنى فعلًا.
 *
 * العطب الأصلي: صفّ العضو المحلي اسمه «أنا» — علامة تعني «صاحب هذا الجهاز» — وكانت تُرسل
 * حرفيًّا، فيقرأ الطرف الآخر «من: أنا» ولا يعرف من يدعوه.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HandoverInvitationNameTest {

    private fun room() = LedgerRoom(
        id = "room-1",
        kind = RoomKind.WATER,
        currency = "YER_NEW",
        title = "ريّ أبو أحمد",
        status = RoomStatus.PENDING,
        linkCode = "WATER-1234",
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L
    )

    private fun member(memberId: String, displayName: String, isMe: Boolean = false, role: String = "counterpart") =
        RoomMember(
            roomId = "room-1",
            memberId = memberId,
            displayName = displayName,
            isMe = isMe,
            role = role,
            joinedAt = 1_700_000_000_000L
        )

    private fun invitation(ownerName: String): String = HandoverPayloads.roomInvitation(
        room = room(),
        members = listOf(
            member("me", ownerName, isMe = true, role = "owner"),
            member("counterpart-room-1", "أبو أحمد")
        ),
        inviterMemberId = "me",
        partnerMemberId = "counterpart-room-1"
    )

    @Test
    fun theLocalPlaceholderNeverTravelsInsideARoomInvitation() {
        val payload = invitation(LocalProfile.PLACEHOLDER)
        assertFalse("العلامة المحلية سافرت في الحمولة", payload.contains(LocalProfile.PLACEHOLDER))

        val decoded = HandoverPayloads.decodeRoomInvitation(payload)
        assertEquals(LocalProfile.UNKNOWN_LABEL, decoded.members.first { it.memberId == "me" }.displayName)
        assertEquals("أبو أحمد", decoded.members.first { it.memberId == "counterpart-room-1" }.displayName)
    }

    @Test
    fun theRealNameArrivesAsTheNameItself() {
        val decoded = HandoverPayloads.decodeRoomInvitation(invitation("أبو سالم"))
        assertEquals("أبو سالم", decoded.members.first { it.memberId == "me" }.displayName)
    }
}

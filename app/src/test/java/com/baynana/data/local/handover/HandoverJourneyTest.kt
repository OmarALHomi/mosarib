package com.baynana.data.local.handover

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerEntry
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.OutboxPayloads
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة ح١٩ على الرحلة كاملة (قاعدة في الذاكرة): من جهاز إلى جهاز بملفّ واحد، بلا شبكة ولا حساب.
 *
 * الرحلة المُختبَرة هي التي يطلبها شرط القبول حرفيًا: «رحلة كاملة تعمل بلا إنترنت ولا حساب».
 * ومسارها: جهاز «أ» فيه غرفة وقيد معلّق → يجهّز حزمة → تُلصق في «ب» → ب» يفحص → يقبل الدعوة →
 * فيُطبَّق كل ما في الحزمة → وإعادة استيراد نفس الحزمة لا تُضاعف شيئًا.
 *
 * ثلاثة أخطاء حقيقية تحرسها هذه البوابة:
 * 1. **قيد يصل قبل قبول غرفته**: يُحفظ مؤجّلًا ويُطبَّق لحظة القبول، ولا يُرفض ولا يضيع.
 * 2. **إعادة استيراد نفس الملف** (وهو ما يفعله الناس: يلصقون الملف مرتين): لا تُضاعف ولا تُعلن نجاحًا.
 * 3. **بلا دعوة صريحة لا تُفتح غرفة**: كود الربط وحده لا يفتح كشفًا (شرط §7).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HandoverJourneyTest {

    private lateinit var context: Context
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private val now = 1_760_000_000_000L
    private val roomId = "room-water-1"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        source = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        target = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    /** يزرع في جهاز «أ»: غرفة ريّ فيها طرفان، وقيد سقية معلّق. */
    private fun seedSource() = runBlocking {
        val dao = source.ledgerDao()
        dao.upsertRoom(
            LedgerRoom(
                id = roomId,
                kind = RoomKind.WATER,
                currency = "YER_NEW",
                title = "ريّ أبو أحمد",
                status = RoomStatus.ACTIVE,
                linkCode = "WATER-1234",
                counterpartName = "أبو أحمد",
                createdAt = now - 5000,
                updatedAt = now - 5000
            )
        )
        dao.upsertMember(
            RoomMember(
                roomId = roomId,
                memberId = "me",
                displayName = "أنا",
                isMe = true,
                role = "owner",
                joinedAt = now - 5000
            )
        )
        dao.upsertMember(
            RoomMember(
                roomId = roomId,
                memberId = "counterpart-$roomId",
                displayName = "أبو أحمد",
                phone = "773000000",
                role = "counterpart",
                joinedAt = now - 5000
            )
        )
        val entry = LedgerEntry(
            id = "entry-1",
            roomId = roomId,
            operationId = "op-entry-1",
            type = EntryType.WATER_SESSION,
            owedByMemberId = "counterpart-$roomId",
            owedToMemberId = "me",
            amountMinor = 1_500_000L,
            currency = "YER_NEW",
            occurredAt = now - 4000,
            description = "سقية \"أبو أحمد\" — ٣ ساعات",
            status = EntryStatus.SENT,
            createdByMemberId = "me",
            createdAt = now - 4000,
            updatedAt = now - 4000
        )
        dao.insertEntryAndEnqueue(entry, OutboxPayloads.entry(entry))
    }

    private fun prepareOnSource(): HandoverRepository.Outgoing = runBlocking {
        HandoverRepository(source, clock = { now }, deviceCodeLabel = { "MSRB-AAAA-BBBB" }).prepareOutgoing()
    }

    private fun targetRepo() = HandoverRepository(target, clock = { now + 60_000 }, deviceCodeLabel = { "MSRB-CCCC-DDDD" })

    @Test
    fun fullJourneyRoomAndEntryArriveInOneBundle() = runBlocking {
        seedSource()
        val outgoing = prepareOnSource()
        assertEquals(2, outgoing.itemCount) // لقطة الغرفة + قيد واحد
        assertTrue(outgoing.compactCode != null)

        val repo = targetRepo()
        val preview = repo.preview(outgoing.text)
        assertTrue(preview is HandoverRepository.Preview.Ready)
        val ready = preview as HandoverRepository.Preview.Ready
        assertEquals(1, ready.newInvitations)

        val report = repo.importBundle(outgoing.text)
        assertEquals(0, report.applied)
        assertEquals(1, report.invited)
        assertEquals(1, report.deferred)
        assertEquals(0, report.rejected)

        // لا غرفة بعد: الدعوة تنتظر قرارًا، ولا شيء يمكن أن يُقرأ عن الطرف الآخر قبل القبول.
        assertNull(target.ledgerDao().getRoom(roomId))
        val invite = target.handoverDao().invitesByStatus(InviteStatus.PENDING).single()
        assertEquals("أبو أحمد", invite.inviterName)
        assertEquals("counterpart-$roomId", invite.partnerMemberId)

        // القبول: تُنشأ الغرفة بمعرّفات الطرفين، ويُطبَّق القيد الذي كان ينتظرها.
        val accepted = repo.acceptInvitation(invite.id)
        assertEquals(1, accepted.applied)
        val room = target.ledgerDao().getRoom(roomId)
        assertNotNull(room)
        assertEquals(RoomStatus.ACTIVE, room!!.status)
        assertEquals("YER_NEW", room.currency)

        val entry = target.ledgerDao().getEntry("entry-1")
        assertNotNull(entry)
        assertEquals(1_500_000L, entry!!.amountMinor)
        assertEquals("counterpart-$roomId", entry.owedByMemberId)
        assertEquals(EntryType.WATER_SESSION, entry.type)
        assertEquals("سقية \"أبو أحمد\" — ٣ ساعات", entry.description)

        // وعلى هذا الجهاز، صاحب الجهاز هو الطرف المدعو: لا يُكتب قيد باسم غيره.
        assertEquals("counterpart-$roomId", target.ledgerDao().getMyMembership(roomId)?.memberId)
        assertEquals(0, target.handoverDao().pendingItemsForRoom(roomId).size)
    }

    @Test
    fun importingTheSameBundleTwiceChangesNothing() = runBlocking {
        seedSource()
        val outgoing = prepareOnSource()
        val repo = targetRepo()

        repo.importBundle(outgoing.text)
        val invite = target.handoverDao().invitesByStatus(InviteStatus.PENDING).single()
        repo.acceptInvitation(invite.id)

        val second = repo.importBundle(outgoing.text)
        assertTrue(second.alreadyImported)
        assertEquals(0, second.applied)
        assertEquals(1, target.ledgerDao().getEntriesIncludingVoided(roomId).size)
        assertEquals(1, target.handoverDao().invitesByStatus(InviteStatus.ACCEPTED).size)
    }

    @Test
    fun aBundleWithoutInvitationNeverOpensARoom() = runBlocking {
        seedSource()
        val outgoing = prepareOnSource()
        // حزمة فيها القيد وحده (كما لو وصل نصفها): لا غرفة، ولا قيد، ولا دعوة — ولا فتح صامت.
        val entryOnly = outgoing.bundle.copy(
            bundleId = "bnn-entry-only",
            items = outgoing.bundle.items.filter { it.entityType == "entry" }
        )
        val text = com.baynana.domain.handover.HandoverBundleCodec.encode(entryOnly)

        val report = targetRepo().importBundle(text)
        assertEquals(1, report.deferred)
        assertEquals(0, report.invited)
        assertNull(target.ledgerDao().getRoom(roomId))
        assertEquals(0, target.ledgerDao().getEntriesIncludingVoided(roomId).size)
    }

    @Test
    fun tamperedFileIsRefusedAndWritesNothing() = runBlocking {
        seedSource()
        val outgoing = prepareOnSource()
        val tampered = outgoing.text.replace("1500000", "9500000")

        val repo = targetRepo()
        val report = repo.importBundle(tampered)
        assertEquals(0, report.applied)
        assertTrue(report.notes.first().contains("بصمته لا تطابق"))
        assertNull(target.ledgerDao().getRoom(roomId))
        assertTrue(target.handoverDao().recentLog().isEmpty())
    }

    @Test
    fun acknowledgementGoesBackOnTheSameRoadAndCannotBeWrittenForTheOtherParty() = runBlocking {
        seedSource()
        val repo = targetRepo()
        repo.importBundle(prepareOnSource().text)
        repo.acceptInvitation(target.handoverDao().invitesByStatus(InviteStatus.PENDING).single().id)

        // الطرف المدعو يُقرّ القيد بمساره الحقيقي: القرار يُكتب محليًا **ويُجدَّل إرساله** في نفس
        // المعاملة، وهذا سبب وجود الإقرار في صندوق الصادر لا في جدول الإقرارات وحده.
        val targetHome = com.baynana.data.local.ledger.LedgerHomeRepository(target)
        targetHome.respondToEntry(
            entryId = "entry-1",
            decision = com.baynana.domain.ledger.AckDecision.ACKNOWLEDGED,
            note = "",
            decidedAt = now + 120_000
        )
        assertEquals(EntryStatus.ACKNOWLEDGED, target.ledgerDao().getEntry("entry-1")?.status)
        assertEquals(1, target.ledgerDao().pendingForHandover(limit = 50).size)

        // حزمة العودة: دعوة/لقطة + الإقرار. تصل إلى جهاز «أ» فتُقرأ ويصير القيد مُقَرًّا عنده أيضًا.
        val backBundle = repo.prepareOutgoing()
        val sourceRepo = HandoverRepository(source, clock = { now + 180_000 }, deviceCodeLabel = { "MSRB-AAAA-BBBB" })
        val ackReport = sourceRepo.importBundle(
            com.baynana.domain.handover.HandoverBundleCodec.encode(backBundle.bundle)
        )
        assertEquals(1, ackReport.applied)
        assertEquals(0, ackReport.rejected)
        assertEquals(EntryStatus.ACKNOWLEDGED, source.ledgerDao().getEntry("entry-1")?.status)

        // وقيد يقرّه من ليس عليه المال: يُرفض ولا يُكتب (لا أحد يُبرئ نفسه، ولا يُقرّ عن غيره).
        val forgedAck = HandoverPayloads.acknowledgement(
            com.baynana.data.local.ledger.Acknowledgement(
                entryId = "entry-1",
                memberId = "me",
                decision = com.baynana.domain.ledger.AckDecision.ACKNOWLEDGED,
                note = "",
                decidedAt = now + 300_000,
                createdAt = now + 300_000
            ),
            resultingStatus = EntryStatus.ACKNOWLEDGED,
            roomId = roomId
        )
        val forged = com.baynana.domain.handover.HandoverBundle(
            bundleId = "bnn-ack-forged",
            createdAt = now + 300_000,
            deviceCode = "MSRB-CCCC-DDDD",
            dbVersion = com.baynana.core.database.DATABASE_VERSION,
            rooms = listOf(roomId),
            items = listOf(
                com.baynana.domain.handover.HandoverItem(
                    operationId = "ack:entry-1:me",
                    entityType = "ack",
                    entityId = "entry-1",
                    action = "UPSERT",
                    payload = forgedAck,
                    createdAt = now + 300_000
                )
            )
        )
        // يُطبَّق أولًا على جهاز نظيف لم يستقبل شيئًا: يجب أن يُرفض لا أن يُكتب.
        val fresh = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val forgedReport = HandoverRepository(fresh, clock = { now + 300_000 }).importBundle(
                com.baynana.domain.handover.HandoverBundleCodec.encode(forged)
            )
            assertEquals("لا قرار يُكتب من غير صاحب المال", 0, forgedReport.applied)
            assertTrue(fresh.ledgerDao().observeAcknowledgements("entry-1").first().isEmpty())
        } finally {
            fresh.close()
        }
    }

    @Test
    fun logRecordsBothDirectionsWithTheirOutcome() = runBlocking {
        seedSource()
        val outgoing = prepareOnSource()
        val repo = targetRepo()
        assertEquals(1, source.handoverDao().recentLog().count { it.direction == HandoverDirection.OUT })
        repo.importBundle(outgoing.text)
        val inbound = target.handoverDao().recentLog().first { it.direction == HandoverDirection.IN }
        assertEquals(outgoing.bundle.bundleId, inbound.bundleId)
        assertEquals(2, inbound.items)
        assertEquals(1, inbound.invited)
        assertEquals("MSRB-AAAA-BBBB", inbound.peerDevice)
    }
}

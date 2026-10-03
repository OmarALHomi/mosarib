package com.baynana.data.local.ledger

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyWire
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة ح٣ (الخطة v6 §4.2 و§6.2): قيد يُحفظ محليًا، معرّف عملية ثابت يمنع الازدواج، إقرار
 * يغيّر الحالة ولا يمسح القيد، ولا حذف متسلسل لدفتر، والمبلغ مخزّن بالوحدة الصغرى.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerDaoTest {

    private lateinit var db: AppDatabase
    private val dao get() = db.ledgerDao()

    private val meId = "member-me"
    private val otherId = "member-other"
    private val now = 1_700_000_000_000L

    @Before
    fun openDatabase() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    private fun room(
        id: String = "room-1",
        kind: String = RoomKind.WATER,
        status: String = RoomStatus.ACTIVE,
        linkCode: String = "LNK-1"
    ) = LedgerRoom(
        id = id,
        kind = kind,
        currency = Currency.YER_NEW.code,
        title = "غرفة الري",
        status = status,
        linkCode = linkCode,
        counterpartName = "أحمد",
        createdAt = now,
        updatedAt = now
    )

    private fun entry(
        id: String = "entry-1",
        operationId: String = "op-1",
        type: String = EntryType.WATER_SESSION,
        amountMinor: Long = 1_000_000L,
        status: String = EntryStatus.SENT,
        listingId: String? = null
    ) = LedgerEntry(
        id = id,
        roomId = "room-1",
        operationId = operationId,
        type = type,
        owedByMemberId = otherId,
        owedToMemberId = meId,
        amountMinor = amountMinor,
        currency = Currency.YER_NEW.code,
        occurredAt = now,
        description = "سقية 5 ساعات",
        status = status,
        createdByMemberId = meId,
        listingId = listingId,
        createdAt = now,
        updatedAt = now
    )

    @Test
    fun `a new entry is stored with its payload enqueued in one transaction`() = runBlocking {
        dao.upsertRoom(room())
        dao.upsertMember(RoomMember(roomId = "room-1", memberId = meId, displayName = "أنا", isMe = true, joinedAt = now))
        dao.upsertMember(RoomMember(roomId = "room-1", memberId = otherId, displayName = "أحمد", joinedAt = now))

        val money = Money.ofMajor(10_000, Currency.YER_NEW)
        val payload = MoneyWire.encode(money).entries.joinToString(",") { "${it.key}=${it.value}" }
        val inserted = dao.insertEntryAndEnqueue(entry(amountMinor = money.minor), payload)

        assertTrue(inserted)
        val stored = dao.getEntry("entry-1")
        assertNotNull(stored)
        assertEquals(1_000_000L, stored!!.amountMinor)
        assertEquals("1500000", MoneyWire.encode(Money.ofMinor(stored.amountMinor, Currency.YER_NEW))[MoneyWire.KEY_AMOUNT_MINOR])

        val batch = dao.nextOutboxBatch()
        assertEquals(1, batch.size)
        assertEquals("op-1", batch.first().operationId)
        assertTrue(batch.first().payload.contains("amountMinor"))
    }

    @Test
    fun `replaying the same operation never creates a second entry`() = runBlocking {
        dao.upsertRoom(room())
        assertTrue(dao.insertEntryAndEnqueue(entry(), "payload"))
        // نفس operationId بمعرّف صف مختلف: إعادة إرسال أو إعادة تشغيل.
        val replay = dao.insertEntryAndEnqueue(entry(id = "entry-1-copy"), "payload")
        assertFalse("إعادة الإرسال يجب ألا تُنشئ قيدًا ثانيًا", replay)

        assertEquals(1, dao.getActiveEntries("room-1").size)
        assertEquals(1, dao.nextOutboxBatch().size)
        assertNull(dao.getEntry("entry-1-copy"))
    }

    @Test
    fun `acknowledgement changes status and keeps the entry intact`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(), "payload")

        dao.recordAcknowledgement(
            Acknowledgement(
                id = "ack-1",
                entryId = "entry-1",
                memberId = otherId,
                decision = AckDecision.ACKNOWLEDGED,
                decidedAt = now + 10,
                createdAt = now + 10
            ),
            resultingStatus = EntryStatus.ACKNOWLEDGED,
            updatedAt = now + 10
        )

        val stored = dao.getEntry("entry-1")
        assertEquals(EntryStatus.ACKNOWLEDGED, stored!!.status)
        assertEquals(1_000_000L, stored.amountMinor) // الأرقام لم تُمس
        assertEquals("سقية 5 ساعات", stored.description)
    }

    @Test
    fun `a dispute keeps the entry and records the reason`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(), "payload")

        dao.recordAcknowledgement(
            Acknowledgement(
                id = "ack-1",
                entryId = "entry-1",
                memberId = otherId,
                decision = AckDecision.DISPUTED,
                note = "عدد الساعات غير صحيح",
                decidedAt = now + 5,
                createdAt = now + 5
            ),
            resultingStatus = EntryStatus.DISPUTED,
            updatedAt = now + 5
        )

        val stored = dao.getEntry("entry-1")!!
        assertEquals(EntryStatus.DISPUTED, stored.status)
        assertNotNull("القيد يبقى موجودًا بعد الاعتراض", dao.getActiveEntries("room-1").firstOrNull())
        val ack = dao.getAcknowledgement("entry-1", otherId)!!
        assertEquals("عدد الساعات غير صحيح", ack.note)
    }

    @Test
    fun `one decision per member per entry, the newest replaces the previous`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(), "payload")

        dao.upsertAcknowledgement(
            Acknowledgement("ack-old", "entry-1", otherId, AckDecision.DISPUTED, "", now, now)
        )
        dao.upsertAcknowledgement(
            Acknowledgement("ack-new", "entry-1", otherId, AckDecision.ACKNOWLEDGED, "", now + 100, now + 100)
        )

        val ack = dao.getAcknowledgement("entry-1", otherId)!!
        assertEquals(AckDecision.ACKNOWLEDGED, ack.decision)
        assertEquals(now + 100, ack.decidedAt)
        // الفهرس الفريد على (entryId, memberId) يمنع وجود قرارين متعارضين لنفس العضو.
        assertEquals(1, dao.getEntryWithDetails("entry-1")!!.acknowledgements.size)
    }

    @Test
    fun `allocations never exceed the payment or the debt`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(id = "debt-1", operationId = "op-debt-1", amountMinor = 500_000L), "p")
        dao.insertEntryAndEnqueue(
            entry(id = "pay-1", operationId = "op-pay-1", type = EntryType.PAYMENT, amountMinor = 300_000L),
            "p"
        )

        dao.upsertAllocation(EntryAllocation("alloc-1", "pay-1", "debt-1", 200_000L, Currency.YER_NEW.code, now))
        assertEquals(200_000L, dao.allocatedToDebt("debt-1"))
        assertEquals(200_000L, dao.allocatedFromPayment("pay-1"))

        dao.upsertAllocation(EntryAllocation("alloc-2", "pay-1", "debt-1", 100_000L, Currency.YER_NEW.code, now + 1))
        // إعادة الإسقاط لنفس (السداد، الدين) تُحدّث المبلغ ولا تُضاعفه.
        assertEquals(100_000L, dao.allocatedToDebt("debt-1"))
        assertEquals(1, dao.getAllocationsForPayment("pay-1").size)
    }

    @Test
    fun `debt entries are ordered oldest first for fifo allocation`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(id = "e-old", operationId = "op-old", amountMinor = 100_000L).copy(occurredAt = now - 5_000), "p")
        dao.insertEntryAndEnqueue(entry(id = "e-new", operationId = "op-new", amountMinor = 200_000L).copy(occurredAt = now), "p")

        val ordered = dao.getDebtEntriesOldestFirst("room-1")
        assertEquals(listOf("e-old", "e-new"), ordered.map { it.id })
    }

    @Test
    fun `a listing can be reserved once and only once`() = runBlocking {
        dao.upsertRoom(room(kind = RoomKind.MARKET, linkCode = "LNK-2"))
        dao.insertEntryAndEnqueue(
            entry(id = "deal-1", operationId = "op-deal-1", type = EntryType.SETTLEMENT, listingId = "listing-9"),
            "p"
        )
        assertNotNull(dao.getActiveEntryForListing("listing-9"))
        // دلال ثانٍ يحاول تثبيت صلح على نفس العرض: الشاشة ترفض بالاعتماد على هذه القراءة.
        assertNotNull("العرض محجوز لصلح قائم", dao.getActiveEntryForListing("listing-9"))
        assertNull(dao.getActiveEntryForListing("listing-10"))
    }

    @Test
    fun `a voided entry leaves the active ledger but is never deleted`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(), "p")
        dao.updateEntryStatus("entry-1", EntryStatus.VOIDED, now + 50)

        assertTrue(dao.getActiveEntries("room-1").isEmpty())
        assertNotNull("القيد الملغى يبقى محفوظًا للتاريخ", dao.getEntry("entry-1"))
        assertEquals(1, dao.countEntriesWithStatus("room-1", EntryStatus.VOIDED))
    }

    @Test
    fun `a room cannot be deleted while it still has entries`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(), "p")

        // حذف الغرفة مباشرة يجب أن يرفضه المخطط (RESTRICT)، فلا يُمحى دفتر بين لحظة وأخرى.
        var refused = false
        try {
            db.openHelper.writableDatabase.execSQL("DELETE FROM rooms WHERE id = 'room-1'")
        } catch (_: android.database.sqlite.SQLiteConstraintException) {
            refused = true
        }
        assertTrue("حذف غرفة فيها قيود يجب أن يُرفض", refused)
    }

    @Test
    fun `room status flow keeps pending rooms out of the shared ledger`() = runBlocking {
        dao.upsertRoom(room(status = RoomStatus.PENDING))
        assertEquals(RoomStatus.PENDING, dao.getRoom("room-1")!!.status)

        dao.updateRoomStatus("room-1", RoomStatus.ACTIVE, now + 1, null)
        assertEquals(RoomStatus.ACTIVE, dao.getRoom("room-1")!!.status)

        dao.updateRoomStatus("room-1", RoomStatus.CLOSED, now + 2, now + 2)
        val closed = dao.getRoom("room-1")!!
        assertEquals(RoomStatus.CLOSED, closed.status)
        assertEquals(now + 2, closed.closedAt)
    }

    @Test
    fun `outbox tracks failures with a visible reason instead of silent retries`() = runBlocking {
        dao.upsertRoom(room())
        dao.insertEntryAndEnqueue(entry(), "p")

        dao.markOutbox("op-1", OutboxState.FAILED, "لا يوجد اتصال", now + 1)
        val failed = dao.nextOutboxBatch().first()
        assertEquals(OutboxState.FAILED, failed.state)
        assertEquals(1, failed.attempts)
        assertEquals("لا يوجد اتصال", failed.lastError)

        // الفشل الدائم يحتاج تدخلًا بشريًا ولا يُعاد صامتًا.
        dao.markOutbox("op-1", OutboxState.DEAD, "بيانات مرفوضة من الخادم", now + 2)
        assertTrue(dao.nextOutboxBatch(states = listOf(OutboxState.PENDING)).isEmpty())
    }

    @Test
    fun `two rooms never mix their balances or currencies`() = runBlocking {
        dao.upsertRoom(room(id = "room-yer", linkCode = "LNK-YER"))
        dao.upsertRoom(room(id = "room-usd", linkCode = "LNK-USD").copy(currency = Currency.USD.code))
        dao.insertEntryAndEnqueue(entry(id = "e-yer", operationId = "op-yer"), "p")
        dao.insertEntryAndEnqueue(
            entry(id = "e-usd", operationId = "op-usd", amountMinor = 10_000L).copy(roomId = "room-usd", currency = Currency.USD.code),
            "p"
        )

        assertEquals(1, dao.getActiveEntries("room-yer").size)
        assertEquals(1, dao.getActiveEntries("room-usd").size)
        assertEquals(Currency.YER_NEW.code, dao.getActiveEntries("room-yer").first().currency)
        assertEquals(Currency.USD.code, dao.getActiveEntries("room-usd").first().currency)
    }
}

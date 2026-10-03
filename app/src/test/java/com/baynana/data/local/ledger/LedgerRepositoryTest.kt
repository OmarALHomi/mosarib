package com.baynana.data.local.ledger

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.NewDebtSpec
import com.baynana.domain.ledger.ReceiptSpec
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * بوابة ح٤ على قاعدة حقيقية: الذرّية، منع الازدواج عند إعادة التشغيل، القبض العام، التخصيص
 * اللاحق، والإلغاء بقيد عكسي يحرّر الإسقاطات.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LedgerRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: LedgerRepository
    private val dao get() = db.ledgerDao()

    private val farmer = "member-farmer"
    private val distributor = "member-distributor"
    private val now = 1_700_000_000_000L
    private var idCounter = 0

    @Before
    fun openDatabase() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = LedgerRepository(
            db = db,
            newId = { "id-${++idCounter}" },
            now = { now }
        )
        dao.upsertRoom(
            LedgerRoom(
                id = "room-1",
                kind = RoomKind.WATER,
                currency = "YER_NEW",
                title = "غرفة الري",
                status = RoomStatus.ACTIVE,
                linkCode = "LNK-1",
                createdAt = now,
                updatedAt = now
            )
        )
        dao.upsertMember(RoomMember(roomId = "room-1", memberId = farmer, displayName = "أحمد", joinedAt = now))
        dao.upsertMember(
            RoomMember(roomId = "room-1", memberId = distributor, displayName = "المسرب", isMe = true, joinedAt = now)
        )
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    private suspend fun session(amountMinor: Long, at: Long = now, operationId: String = "op-session-${++idCounter}"): LedgerEntry =
        repository.recordDebt(
            NewDebtSpec(
                id = "session-${++idCounter}",
                operationId = operationId,
                roomId = "room-1",
                type = EntryType.WATER_SESSION,
                debtorMemberId = farmer,
                creditorMemberId = distributor,
                amountMinor = amountMinor,
                currency = "YER_NEW",
                occurredAt = at,
                description = "سقية 5 ساعات"
            )
        ).entry

    private fun receiptSpec(
        amountMinor: Long,
        mode: AllocationMode,
        operationId: String = "op-pay-${++idCounter}",
        at: Long = now
    ) = ReceiptSpec(
        id = "pay-${++idCounter}",
        operationId = operationId,
        roomId = "room-1",
        debtorMemberId = farmer,
        creditorMemberId = distributor,
        amountMinor = amountMinor,
        currency = "YER_NEW",
        occurredAt = at,
        mode = mode,
        description = "قبض من المزارع"
    )

    @Test
    fun `a receipt is stored with its allocations in one atomic write`() = runBlocking {
        val debt = session(1_000_000L)
        val result = repository.recordReceipt(receiptSpec(600_000L, AllocationMode.OldestFirst))

        assertTrue(result.created)
        assertEquals(EntryType.PAYMENT, result.entry.type)
        // القيد مرآة: من استلم المال صار عليه.
        assertEquals(distributor, result.entry.owedByMemberId)
        assertEquals(farmer, result.entry.owedToMemberId)
        assertEquals(600_000L, dao.allocatedToDebt(debt.id))
        // صفّان في الصندوق: واحد للسقية وآخر للسداد، وكل قيد له صف واحد فقط.
        assertEquals(2, dao.nextOutboxBatch().size)
        assertEquals(1, dao.countOutboxForEntity(result.entry.id))
    }

    @Test
    fun `replaying the same operation id never doubles the deduction`() = runBlocking {
        val debt = session(1_000_000L)
        val spec = receiptSpec(400_000L, AllocationMode.OldestFirst, operationId = "op-replay")

        val first = repository.recordReceipt(spec)
        val replay = repository.recordReceipt(spec.copy(id = "pay-other-id"))

        assertTrue(first.created)
        assertFalse("الإعادة لا تُنشئ قيدًا ثانيًا", replay.created)
        assertEquals(first.entry.id, replay.entry.id)
        assertEquals("الخصم لم يتضاعف", 400_000L, dao.allocatedToDebt(debt.id))
        assertEquals("قيد واحد فقط", 1, dao.getActiveEntries("room-1").count { it.type == EntryType.PAYMENT })
        assertEquals("وصف صادر واحد للسداد", 1, dao.countOutboxForEntity(first.entry.id))
    }

    @Test
    fun `replaying a debt entry does not create a second session`() = runBlocking {
        val spec = NewDebtSpec(
            id = "session-A", operationId = "op-debt-replay", roomId = "room-1",
            type = EntryType.WATER_SESSION, debtorMemberId = farmer, creditorMemberId = distributor,
            amountMinor = 500_000L, currency = "YER_NEW", occurredAt = now, description = "سقية"
        )
        assertTrue(repository.recordDebt(spec).created)
        val replay = repository.recordDebt(spec.copy(id = "session-B"))
        assertFalse(replay.created)
        assertEquals("session-A", replay.entry.id)
        assertEquals(1, dao.getActiveEntries("room-1").size)
    }

    @Test
    fun `a general receipt closes nothing and stays as credit`() = runBlocking {
        session(1_000_000L)
        val result = repository.recordReceipt(receiptSpec(700_000L, AllocationMode.None))

        assertEquals(EntryType.GENERAL_RECEIPT, result.entry.type)
        assertTrue("لا إسقاط", dao.getAllocationsInRoom("room-1").isEmpty())
        assertEquals(700_000L, result.unappliedMinor)

        val balance = repository.balance("room-1")
        assertEquals("الدين كما هو", 1_000_000L, balance.openDebtMinor)
        assertEquals("والمبلغ رصيد دائن", 700_000L, balance.unappliedReceiptMinor)
    }

    @Test
    fun `oldest-first settles the oldest line and reports the skipped ones`() = runBlocking {
        val oldDebt = session(300_000L, at = now - 9_000, operationId = "op-old")
        val newDebt = session(500_000L, at = now - 1_000, operationId = "op-new")
        val result = repository.recordReceipt(receiptSpec(600_000L, AllocationMode.OldestFirst))

        assertEquals(oldDebt.id, result.plan.allocations.first().debtEntryId)
        assertEquals(300_000L, dao.allocatedToDebt(oldDebt.id))
        assertEquals(300_000L, dao.allocatedToDebt(newDebt.id))
        assertEquals("لا فائض: المبلغ وزّع على السطرين", 0L, result.unappliedMinor)
        assertTrue("لم يُتجاهل أي سطر", result.plan.skipped.isEmpty())
    }

    @Test
    fun `selected allocation only touches the chosen line even if it is not the oldest`() = runBlocking {
        val oldDebt = session(300_000L, at = now - 9_000, operationId = "op-old")
        val pickedDebt = session(400_000L, at = now - 8_000, operationId = "op-picked")
        val result = repository.recordReceipt(receiptSpec(400_000L, AllocationMode.Selected(listOf(pickedDebt.id))))

        assertEquals(0L, dao.allocatedToDebt(oldDebt.id))
        assertEquals(400_000L, dao.allocatedToDebt(pickedDebt.id))
        assertEquals("الباقي رصيد دائن في الغرفة", 0L, result.unappliedMinor)
    }

    @Test
    fun `overflow beyond all debts stays as room credit and is not sent to a third party`() = runBlocking {
        val debt = session(300_000L)
        val result = repository.recordReceipt(receiptSpec(1_000_000L, AllocationMode.OldestFirst))

        assertEquals(300_000L, dao.allocatedToDebt(debt.id))
        assertEquals(700_000L, result.unappliedMinor)
        val balance = repository.balance("room-1")
        assertEquals(0L, balance.openDebtMinor)
        assertEquals(700_000L, balance.unappliedReceiptMinor)
        assertEquals("المزارع له 700,000 عند المسرب", 700_000L, balance.netOf(farmer))
    }

    @Test
    fun `a later allocation of a general receipt keeps the original payment date`() = runBlocking {
        val debt = session(500_000L)
        val paidAt = now - 500_000
        val receipt = repository.recordReceipt(receiptSpec(500_000L, AllocationMode.None, at = paidAt)).entry

        val plan = repository.allocateExistingReceipt(
            receiptEntryId = receipt.id,
            mode = AllocationMode.OldestFirst,
            operationId = "op-late-allocation",
            decidedAt = now,
            reason = "تخصيص بعد مراجعة الكشف"
        )

        assertEquals(1, plan.allocations.size)
        assertEquals(500_000L, dao.allocatedToDebt(debt.id))
        assertEquals("تاريخ الدفع الأصلي لم يتغير", paidAt, dao.getEntry(receipt.id)!!.occurredAt)
        assertEquals("تاريخ القرار مختلف ومُسجَّل", now, dao.getAllocationsForPayment(receipt.id).single().createdAt)
        assertEquals("لا يبقَ غير مخصَّص", 0L, repository.balance("room-1").unappliedReceiptMinor)
    }

    @Test
    fun `a later allocation is not applied twice when the operation is replayed`() = runBlocking {
        val debt = session(500_000L)
        val receipt = repository.recordReceipt(receiptSpec(500_000L, AllocationMode.None)).entry

        repository.allocateExistingReceipt(receipt.id, AllocationMode.OldestFirst, "op-late", now, "أول تخصيص")
        repository.allocateExistingReceipt(receipt.id, AllocationMode.OldestFirst, "op-late", now, "أول تخصيص")

        assertEquals(500_000L, dao.allocatedToDebt(debt.id))
        assertEquals(1, dao.getAllocationsForPayment(receipt.id).size)
    }

    @Test
    fun `reversing a session voids it, keeps the row, and releases what was allocated to it`() = runBlocking {
        val debt = session(1_000_000L)
        repository.recordReceipt(receiptSpec(400_000L, AllocationMode.OldestFirst))
        assertEquals(400_000L, dao.allocatedToDebt(debt.id))

        val reversal = repository.reverseEntry(debt.id, "سقية مكررة بالخطأ", operationId = "op-reverse-1")

        assertTrue(reversal.created)
        val original = dao.getEntry(debt.id)!!
        assertEquals("الأصل باقٍ للتاريخ", EntryStatus.VOIDED, original.status)
        assertEquals(1_000_000L, original.amountMinor)
        assertEquals(EntryType.ADJUSTMENT, reversal.reversal.type)
        assertEquals(debt.id, reversal.reversal.reversesEntryId)
        assertEquals("الإسقاطات حُرّرت", 0L, dao.allocatedToDebt(debt.id))
        assertEquals("المدفوع صار رصيدًا دائنًا في الغرفة", 400_000L, repository.balance("room-1").unappliedReceiptMinor)
    }

    @Test
    fun `the reversal itself cannot be reversed and the same entry cannot be voided twice`() = runBlocking {
        val debt = session(500_000L)
        val reversal = repository.reverseEntry(debt.id, "خطأ", operationId = "op-rev")

        val second = repository.reverseEntry(debt.id, "خطأ", operationId = "op-rev-2")
        assertFalse("لا قيد عكسي ثانٍ", second.created)
        assertEquals(reversal.reversal.id, second.reversal.id)

        var refused = false
        try {
            repository.reverseEntry(reversal.reversal.id, "إلغاء العكسي", operationId = "op-rev-3")
        } catch (error: IllegalStateException) {
            refused = true
            assertTrue(error.message!!.contains("قيد عكسي"))
        }
        assertTrue("لا يُلغى قيد عكسي مباشرة", refused)
    }

    @Test
    fun `reversing a receipt reopens the debts it had settled`() = runBlocking {
        val debt = session(1_000_000L)
        val payment = repository.recordReceipt(receiptSpec(1_000_000L, AllocationMode.OldestFirst)).entry
        assertTrue(repository.balance("room-1").isSettled)

        repository.reverseEntry(payment.id, "قبض لم يصل", operationId = "op-rev-payment")

        val balance = repository.balance("room-1")
        assertEquals("الدين عاد مفتوحًا", 1_000_000L, balance.openDebtMinor)
        assertEquals(0L, balance.unappliedReceiptMinor)
        assertEquals(0L, dao.allocatedToDebt(debt.id))
    }

    @Test
    fun `a draft that was never shared can be discarded, a shared one cannot`() = runBlocking {
        val draft = LedgerEntry(
            id = "draft-1", roomId = "room-1", operationId = "op-draft", type = EntryType.WATER_SESSION,
            owedByMemberId = farmer, owedToMemberId = distributor, amountMinor = 100L, currency = "YER_NEW",
            occurredAt = now, description = "مسودة", status = EntryStatus.DRAFT,
            createdByMemberId = distributor, createdAt = now, updatedAt = now
        )
        assertTrue(dao.insertEntryIfNew(draft) > 0)
        assertTrue("المسودة غير المشتركة تُحذف", repository.discardDraft("draft-1"))

        val shared = session(500_000L)
        assertFalse("القيد المشترك لا يُحذف", repository.discardDraft(shared.id))
        assertNotNull(dao.getEntry(shared.id))
    }

    @Test
    fun `the balance derives from allocations and follows every reversal`() = runBlocking {
        val first = session(300_000L, at = now - 9_000, operationId = "op-1")
        val second = session(200_000L, at = now - 8_000, operationId = "op-2")
        // 400,000: 300,000 للأقدم كاملة، و100,000 للسطر الثاني.
        repository.recordReceipt(receiptSpec(400_000L, AllocationMode.OldestFirst))
        assertEquals(300_000L, dao.allocatedToDebt(first.id))
        assertEquals(100_000L, dao.allocatedToDebt(second.id))

        // إلغاء السقية الأولى يحرّر 300,000 فيعود السداد رصيدًا دائنًا، وتبقى الثانية ناقصة.
        repository.reverseEntry(first.id, "سقية غير صحيحة", operationId = "op-rev-1")

        val balance = repository.balance("room-1")
        assertEquals("الباقي من السقية الثانية", 100_000L, balance.openDebtMinor)
        assertEquals("الرصيد الدائن في الغرفة", 300_000L, balance.unappliedReceiptMinor)
        assertEquals(200_000L, balance.netOf(farmer))
        assertEquals(-200_000L, balance.netOf(distributor))
        assertEquals(
            "رصيد العضو = الرصيد الدائن − الدين المفتوح",
            balance.netOf(farmer),
            balance.unappliedReceiptMinor - balance.openDebtMinor
        )
    }

    @Test
    fun `a receipt of another currency never settles debts of this room`() = runBlocking {
        val debt = session(500_000L)
        val result = repository.recordReceipt(
            receiptSpec(500_000L, AllocationMode.OldestFirst).copy(currency = "SAR")
        )

        assertEquals(0L, dao.allocatedToDebt(debt.id))
        assertEquals(500_000L, result.unappliedMinor)
        assertTrue(result.plan.skipped.any { it.reason == "بعملة أخرى: لا تحويل تلقائي" })
    }
}

package com.baynana.domain.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٤: كل حالات §4.3 في الخطة، مختبرة على المحرّك النقي بلا قاعدة ولا Android.
 * الأرقام كلها بالوحدة الصغرى (فلس).
 */
class AllocationEngineTest {

    private val farmer = "farmer"
    private val distributor = "distributor"
    private val otherFarmer = "farmer-2"
    private val now = 1_700_000_000_000L

    private fun debt(
        id: String,
        amountMinor: Long,
        occurredAt: Long,
        debtor: String = farmer,
        creditor: String = distributor,
        status: String = EntryStatus.SENT,
        currency: String = "YER_NEW",
        roomId: String = "room-1",
        type: String = EntryType.WATER_SESSION
    ) = EntryView(
        id = id, operationId = "op-$id", roomId = roomId, type = type,
        owedByMemberId = debtor, owedToMemberId = creditor,
        amountMinor = amountMinor, currency = currency, occurredAt = occurredAt, status = status
    )

    private fun receipt(
        id: String = "pay-1",
        amountMinor: Long,
        occurredAt: Long = now,
        type: String = EntryType.PAYMENT,
        currency: String = "YER_NEW",
        roomId: String = "room-1"
    ) = EntryView(
        id = id, operationId = "op-$id", roomId = roomId, type = type,
        // السداد مرآة الدين: من استلم المال صار عليه، ومن دفعه صار له.
        owedByMemberId = distributor, owedToMemberId = farmer,
        amountMinor = amountMinor, currency = currency, occurredAt = occurredAt, status = EntryStatus.SENT
    )

    @Test
    fun `oldest debt is settled first`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 1_500_000L),
            candidates = listOf(
                debt("new", 1_000_000L, now - 1_000),
                debt("old", 1_000_000L, now - 9_000)
            ),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.OldestFirst
        )

        assertEquals(listOf("old", "new"), plan.allocations.map { it.debtEntryId })
        assertEquals(1_000_000L, plan.allocations[0].amountMinor)
        assertEquals(500_000L, plan.allocations[1].amountMinor)
        assertEquals(1_500_000L, plan.appliedMinor)
        assertEquals(0L, plan.unappliedMinor)
    }

    @Test
    fun `a general receipt closes no debt at all`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(type = EntryType.GENERAL_RECEIPT, amountMinor = 900_000L),
            candidates = listOf(debt("d1", 500_000L, now - 5)),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.None
        )

        assertTrue("القبض العام لا يُغلق دينًا", plan.isEmpty)
        assertEquals(0L, plan.appliedMinor)
        assertEquals("كامل المبلغ يبقى رصيدًا دائنًا في الغرفة", 900_000L, plan.unappliedMinor)
    }

    @Test
    fun `selected mode touches only the chosen lines`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 800_000L),
            candidates = listOf(
                debt("d-old", 500_000L, now - 9_000),
                debt("d-picked", 400_000L, now - 8_000),
                debt("d-other", 300_000L, now - 7_000)
            ),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.Selected(listOf("d-picked"))
        )

        assertEquals(listOf("d-picked"), plan.allocations.map { it.debtEntryId })
        assertEquals(400_000L, plan.allocations.single().amountMinor)
        assertEquals("الباقي يبقى رصيدًا دائنًا", 400_000L, plan.unappliedMinor)
        assertTrue("لم يُنظر في غير المختار", plan.skipped.none { it.entryId == "d-old" })
    }

    @Test
    fun `overflow beyond the debt stays as credit in the same room and currency`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 1_000_000L),
            candidates = listOf(debt("d1", 300_000L, now - 10)),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.OldestFirst
        )

        assertEquals(300_000L, plan.appliedMinor)
        assertEquals("الفائض لا يُنقل لشخص ثالث", 700_000L, plan.unappliedMinor)
    }

    @Test
    fun `allocation never exceeds the debt nor the receipt, and counts what was already allocated`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 1_000_000L),
            candidates = listOf(debt("d1", 600_000L, now - 10), debt("d2", 900_000L, now - 9)),
            allocatedByEntry = mapOf("pay-1" to 200_000L, "d1" to 100_000L),
            mode = AllocationMode.OldestFirst
        )

        // المتاح من السداد = 800,000. الباقي على d1 = 500,000 فيأخذه، ثم 300,000 من d2.
        assertEquals(listOf("d1", "d2"), plan.allocations.map { it.debtEntryId })
        assertEquals(500_000L, plan.allocations[0].amountMinor)
        assertEquals(300_000L, plan.allocations[1].amountMinor)
        assertEquals(0L, plan.unappliedMinor)
    }

    @Test
    fun `cross-currency and cross-room lines are skipped with a clear reason`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 1_000_000L),
            candidates = listOf(
                debt("d-sar", 500_000L, now - 10, currency = "SAR"),
                debt("d-room2", 500_000L, now - 9, roomId = "room-2")
            ),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.OldestFirst
        )

        assertTrue(plan.isEmpty)
        assertEquals(1_000_000L, plan.unappliedMinor)
        assertEquals("بعملة أخرى: لا تحويل تلقائي", plan.skipped.first { it.entryId == "d-sar" }.reason)
        assertEquals("من غرفة أخرى", plan.skipped.first { it.entryId == "d-room2" }.reason)
    }

    @Test
    fun `a debt of another member in the same room is never touched`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 1_000_000L),
            candidates = listOf(debt("d-other-farmer", 500_000L, now - 10, debtor = otherFarmer)),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.OldestFirst
        )

        assertTrue(plan.isEmpty)
        assertEquals("لدين طرف آخر في الغرفة نفسها", plan.skipped.single().reason)
    }

    @Test
    fun `voided, draft, disputed and already-settled lines are refused with reasons`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 5_000_000L),
            candidates = listOf(
                debt("d-void", 500_000L, now - 10, status = EntryStatus.VOIDED),
                debt("d-draft", 500_000L, now - 9, status = EntryStatus.DRAFT),
                debt("d-disputed", 500_000L, now - 8, status = EntryStatus.DISPUTED),
                debt("d-changed", 500_000L, now - 7, status = EntryStatus.CHANGE_REQUESTED),
                debt("d-paid", 500_000L, now - 6)
            ),
            allocatedByEntry = mapOf("d-paid" to 500_000L),
            mode = AllocationMode.OldestFirst
        )

        assertTrue(plan.isEmpty)
        val reasons = plan.skipped.associate { it.entryId to it.reason }
        assertEquals("قيد ملغى بقيد عكسي", reasons["d-void"])
        assertEquals("مسودة لم تُشارك بعد", reasons["d-draft"])
        assertEquals("قيد معترَض عليه: احسم الاعتراض أولًا", reasons["d-disputed"])
        assertEquals("قيد مطلوب تعديله: احسم الطلب أولًا", reasons["d-changed"])
        assertEquals("مسدَّد بالكامل", reasons["d-paid"])
    }

    @Test
    fun `a settled line is skipped and the next one is settled instead`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 400_000L),
            candidates = listOf(debt("d1", 500_000L, now - 10), debt("d2", 500_000L, now - 9)),
            allocatedByEntry = mapOf("d1" to 500_000L),
            mode = AllocationMode.OldestFirst
        )

        assertEquals(listOf("d2"), plan.allocations.map { it.debtEntryId })
        assertEquals(400_000L, plan.allocations.single().amountMinor)
    }

    @Test
    fun `order is deterministic when two lines share the same timestamp`() {
        val entries = listOf(debt("b", 100_000L, now), debt("a", 100_000L, now))
        val plan = AllocationEngine.plan(receipt(amountMinor = 200_000L), entries, emptyMap(), AllocationMode.OldestFirst)
        assertEquals(listOf("a", "b"), plan.allocations.map { it.debtEntryId })
    }

    @Test
    fun `a reversal is never a candidate and a debt cannot be a receipt`() {
        val reversal = EntryView(
            id = "rev-1", operationId = "op-rev", roomId = "room-1", type = EntryType.ADJUSTMENT,
            owedByMemberId = farmer, owedToMemberId = distributor,
            amountMinor = 500_000L, currency = "YER_NEW", occurredAt = now, status = EntryStatus.SENT,
            reversesEntryId = "d1"
        )
        val plan = AllocationEngine.plan(receipt(amountMinor = 500_000L), listOf(reversal), emptyMap(), AllocationMode.OldestFirst)
        assertTrue(plan.isEmpty)
        assertEquals("قيد عكسي: لا يُخصَّص عليه سداد", plan.skipped.single().reason)

        val wrongReceipt = AllocationEngine.plan(
            receipt = debt("d-pay", 100_000L, now),
            candidates = listOf(debt("d1", 100_000L, now)),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.OldestFirst
        )
        assertFalse("لا تخصيص من قيد ليس سدادًا", wrongReceipt.skipped.isEmpty())
    }

    @Test
    fun `an explicit request for a missing line is reported, not ignored`() {
        val plan = AllocationEngine.plan(
            receipt = receipt(amountMinor = 500_000L),
            candidates = listOf(debt("d1", 500_000L, now - 10)),
            allocatedByEntry = emptyMap(),
            mode = AllocationMode.Selected(listOf("d1", "ghost"))
        )

        assertEquals(listOf("d1"), plan.allocations.map { it.debtEntryId })
        assertEquals("القيد المطلوب ليس في هذه الغرفة", plan.skipped.single { it.entryId == "ghost" }.reason)
    }
}

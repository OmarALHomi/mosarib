package com.baynana.domain.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** بوابة ح٤: الأرصدة مشتقّة من القيود والإسقاطات، والديون المفتوحة مفصولة عن الأرصدة الدائنة. */
class BalanceEngineTest {

    private val farmer = "farmer"
    private val distributor = "distributor"
    private val now = 1_700_000_000_000L

    private fun session(id: String, amountMinor: Long, at: Long = now, status: String = EntryStatus.SENT) = EntryView(
        id = id, operationId = "op-$id", roomId = "room-1", type = EntryType.WATER_SESSION,
        owedByMemberId = farmer, owedToMemberId = distributor,
        amountMinor = amountMinor, currency = "YER_NEW", occurredAt = at, status = status
    )

    private fun payment(id: String, amountMinor: Long, at: Long = now) = EntryView(
        id = id, operationId = "op-$id", roomId = "room-1", type = EntryType.PAYMENT,
        owedByMemberId = distributor, owedToMemberId = farmer,
        amountMinor = amountMinor, currency = "YER_NEW", occurredAt = at, status = EntryStatus.SENT
    )

    @Test
    fun `a session puts the farmer in debt and the distributor in credit, equal and opposite`() {
        val balance = BalanceEngine.compute("room-1", "YER_NEW", listOf(session("s1", 1_000_000L)), emptyList())

        assertEquals(-1_000_000L, balance.netOf(farmer))
        assertEquals(1_000_000L, balance.netOf(distributor))
        assertEquals(1_000_000L, balance.openDebtMinor)
        assertEquals(0L, balance.unappliedReceiptMinor)
        assertFalse(balance.isSettled)
    }

    @Test
    fun `a fully allocated payment returns both sides to zero`() {
        val entries = listOf(session("s1", 1_000_000L), payment("p1", 1_000_000L))
        val allocations = listOf(AllocationView("p1", "s1", 1_000_000L))
        val balance = BalanceEngine.compute("room-1", "YER_NEW", entries, allocations)

        assertEquals(0L, balance.netOf(farmer))
        assertEquals(0L, balance.netOf(distributor))
        assertEquals(0L, balance.openDebtMinor)
        assertEquals(0L, balance.unappliedReceiptMinor)
        assertTrue("لا دين ولا رصيد معلّق", balance.isSettled)
    }

    @Test
    fun `a partial payment leaves the remainder as an open debt`() {
        val entries = listOf(session("s1", 1_000_000L), payment("p1", 400_000L))
        val balance = BalanceEngine.compute("room-1", "YER_NEW", entries, listOf(AllocationView("p1", "s1", 400_000L)))

        assertEquals(-600_000L, balance.netOf(farmer))
        assertEquals(600_000L, balance.openDebtMinor)
        assertEquals(0L, balance.unappliedReceiptMinor)
    }

    @Test
    fun `an unallocated general receipt is reported as credit, never as a closed debt`() {
        val receipt = EntryView(
            id = "r1", operationId = "op-r1", roomId = "room-1", type = EntryType.GENERAL_RECEIPT,
            owedByMemberId = distributor, owedToMemberId = farmer,
            amountMinor = 700_000L, currency = "YER_NEW", occurredAt = now, status = EntryStatus.SENT
        )
        val balance = BalanceEngine.compute("room-1", "YER_NEW", listOf(session("s1", 1_000_000L), receipt), emptyList())

        assertEquals(1_000_000L, balance.openDebtMinor)
        assertEquals(700_000L, balance.unappliedReceiptMinor)
        assertEquals(-300_000L, balance.netOf(farmer))
        assertFalse("لا يُعلن الصفاء مع وجود رصيد دائن معلّق", balance.isSettled)
    }

    @Test
    fun `a voided entry leaves the balance but stays counted nowhere`() {
        val entries = listOf(session("s1", 1_000_000L, status = EntryStatus.VOIDED), session("s2", 300_000L))
        val balance = BalanceEngine.compute("room-1", "YER_NEW", entries, emptyList())

        assertEquals(-300_000L, balance.netOf(farmer))
        assertEquals(300_000L, balance.openDebtMinor)
    }

    @Test
    fun `another currency in the same room never mixes with the room currency`() {
        val sar = session("s-sar", 500_000L).copy(currency = "SAR")
        val balance = BalanceEngine.compute("room-1", "YER_NEW", listOf(session("s1", 1_000_000L), sar), emptyList())

        assertEquals("الدين بالريال السعودي لا يظهر في رصيد اليمني", 1_000_000L, balance.openDebtMinor)
        assertEquals(-1_000_000L, balance.netOf(farmer))
    }

    @Test
    fun `an unacknowledged entry is counted and reported as awaiting acknowledgement`() {
        val entries = listOf(session("s1", 500_000L), session("s2", 500_000L, status = EntryStatus.ACKNOWLEDGED))
        val balance = BalanceEngine.compute("room-1", "YER_NEW", entries, emptyList())
        assertEquals(1, balance.awaitingAcknowledgement)
    }

    @Test
    fun `open debts come oldest first and only for the asked member`() {
        val entries = listOf(
            session("s-new", 100_000L, at = now),
            session("s-old", 100_000L, at = now - 9_000),
            session("s-paid", 100_000L, at = now - 8_000)
        )
        val allocations = listOf(AllocationView("p1", "s-paid", 100_000L))

        val open = BalanceEngine.openDebtsFor(farmer, entries, allocations)
        assertEquals(listOf("s-old", "s-new"), open.map { it.id })
        assertTrue("لا ديون على الطرف الآخر", BalanceEngine.openDebtsFor(distributor, entries, allocations).isEmpty())
    }

    @Test
    fun `open lines separate what the member owes from what they hold`() {
        val receipt = EntryView(
            id = "r1", operationId = "op-r1", roomId = "room-1", type = EntryType.GENERAL_RECEIPT,
            owedByMemberId = distributor, owedToMemberId = farmer,
            amountMinor = 200_000L, currency = "YER_NEW", occurredAt = now, status = EntryStatus.SENT
        )
        val lines = BalanceEngine.openLinesFor(
            distributor,
            listOf(session("s1", 900_000L), receipt),
            emptyList()
        )
        assertEquals("المسرب يحمل 200,000 للمزارع", 200_000L, lines.heldMinor)
        assertEquals("وليس عليه دين", 0L, lines.owesMinor)
        assertFalse(lines.isClear)
    }

    @Test
    fun `a reversed entry with its mirror cancels out and touches no balance`() {
        val original = session("s1", 1_000_000L, status = EntryStatus.VOIDED)
        val mirror = EntryView(
            id = "rev-1", operationId = "op-rev", roomId = "room-1", type = EntryType.ADJUSTMENT,
            owedByMemberId = distributor, owedToMemberId = farmer,
            amountMinor = 1_000_000L, currency = "YER_NEW", occurredAt = now + 1, status = EntryStatus.SENT,
            reversesEntryId = "s1"
        )
        val balance = BalanceEngine.compute("room-1", "YER_NEW", listOf(original, mirror), emptyList())

        assertEquals("الإلغاء لا يقلب الرصيد", 0L, balance.netOf(farmer))
        assertEquals(0L, balance.netOf(distributor))
        assertEquals(0L, balance.openDebtMinor)
        assertTrue(balance.isSettled)
    }

    @Test
    fun `drafts are counted but never enter the announced numbers`() {
        val draft = session("draft-1", 9_000_000L, status = EntryStatus.DRAFT)
        val balance = BalanceEngine.compute("room-1", "YER_NEW", listOf(session("s1", 500_000L), draft), emptyList())

        assertEquals(1, balance.draftCount)
        assertEquals("المسودة لا تُضاف إلى الدين المعلن", 500_000L, balance.openDebtMinor)
        assertEquals(-500_000L, balance.netOf(farmer))
    }

    @Test
    fun `totals of the two sides always cancel inside one room and currency`() {
        val entries = listOf(
            session("s1", 1_000_000L),
            session("s2", 250_000L, at = now - 5_000),
            payment("p1", 400_000L)
        )
        val balance = BalanceEngine.compute("room-1", "YER_NEW", entries, listOf(AllocationView("p1", "s1", 400_000L)))
        val sum = balance.members.sumOf { it.netMinor }
        assertEquals("مجموع أرصدة الغرفة صفر: كل قيد له طرفان", 0L, sum)
    }
}

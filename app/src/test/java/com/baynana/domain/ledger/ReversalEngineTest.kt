package com.baynana.domain.ledger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** بوابة ح٤: الإلغاء بقيد عكسي مرآة، لا بحذف، وبلا رقم سالب مخزَّن. */
class ReversalEngineTest {

    private val now = 1_700_000_000_000L

    private fun entry(
        id: String = "d1",
        type: String = EntryType.WATER_SESSION,
        status: String = EntryStatus.SENT,
        reverses: String? = null
    ) = EntryView(
        id = id, operationId = "op-$id", roomId = "room-1", type = type,
        owedByMemberId = "farmer", owedToMemberId = "distributor",
        amountMinor = 750_000L, currency = "YER_NEW", occurredAt = now, status = status,
        reversesEntryId = reverses
    )

    @Test
    fun `a shared entry may be reversed`() {
        assertEquals(ReversalCheck.Allowed, ReversalEngine.check(entry(), null))
    }

    @Test
    fun `the mirror swaps the parties and keeps the amount and currency`() {
        val mirror = ReversalEngine.mirror(entry(), "rev-1", "op-rev", now + 1_000, "سقية مكررة")

        assertEquals(EntryType.ADJUSTMENT, mirror.type)
        assertEquals("distributor", mirror.owedByMemberId)
        assertEquals("farmer", mirror.owedToMemberId)
        assertEquals(750_000L, mirror.amountMinor)
        assertEquals("YER_NEW", mirror.currency)
        assertEquals("d1", mirror.reversesEntryId)
        assertEquals(EntryStatus.SENT, mirror.status)
        assertEquals("سقية مكررة", mirror.description)
        assertTrue("لا مبلغ سالب في أي مكان", mirror.amountMinor > 0L)
    }

    @Test
    fun `the voided original plus its mirror cancel each other exactly`() {
        // كتابة الإلغاء تفعل الأمرين في معاملة واحدة: الأصل يصير VOIDED، والعكسي يُدرج.
        val original = entry(status = EntryStatus.VOIDED)
        val mirror = ReversalEngine.mirror(entry(), "rev-1", "op-rev", now + 1, "")
        assertEquals("المرآة تشير إلى الأصل", original.id, mirror.reversesEntryId)

        val balance = BalanceEngine.compute("room-1", "YER_NEW", listOf(original, mirror), emptyList())
        assertEquals(0L, balance.netOf("farmer"))
        assertEquals(0L, balance.netOf("distributor"))
        assertTrue(balance.isSettled)
    }

    /**
     * انحدار مُصلَح: كانت التصفية تستثني العكسي بشرط رؤية أصله في القائمة، وقائمة «النشطة» لا
     * تُظهر الملغى، فبقي العكسي محسوبًا فانقلب الدين موجبًا بعد إلغائه. العكسي يُستثنى دائمًا.
     */
    @Test
    fun `a mirror is never counted even when the voided original is not in the list`() {
        val mirror = ReversalEngine.mirror(entry(), "rev-1", "op-rev", now + 1, "")
        val balance = BalanceEngine.compute("room-1", "YER_NEW", listOf(mirror), emptyList())

        assertEquals("لا أثر للعكسي وحده", 0L, balance.netOf("farmer"))
        assertEquals(0L, balance.openDebtMinor)
        assertEquals(0L, balance.unappliedReceiptMinor)
        assertTrue(balance.isSettled)
    }

    @Test
    fun `an already voided entry is refused`() {
        val check = ReversalEngine.check(entry(status = EntryStatus.VOIDED), null)
        assertEquals("قيد ملغى سابقًا بقيد عكسي", (check as ReversalCheck.Refused).reason)
        val check2 = ReversalEngine.check(entry(), entry(id = "rev-1", type = EntryType.ADJUSTMENT))
        assertEquals("قيد ملغى سابقًا بقيد عكسي", (check2 as ReversalCheck.Refused).reason)
    }

    @Test
    fun `a reversal is not reversed directly`() {
        val reversal = entry(id = "rev-1", type = EntryType.ADJUSTMENT, reverses = "d1")
        val check = ReversalEngine.check(reversal, null)
        assertEquals(
            "لا يُلغى قيد عكسي مباشرة؛ أنشئ قيدًا جديدًا يوضح التصحيح",
            (check as ReversalCheck.Refused).reason
        )
    }

    @Test
    fun `a draft that was never shared is deleted rather than reversed`() {
        val check = ReversalEngine.check(entry(status = EntryStatus.DRAFT), null)
        assertEquals("مسودة لم تُشارك بعد؛ احذفها بدل إلغائها", (check as ReversalCheck.Refused).reason)
    }
}

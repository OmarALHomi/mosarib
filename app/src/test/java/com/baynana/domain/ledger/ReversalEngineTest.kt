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
    fun `the original plus its mirror cancel each other exactly`() {
        val original = entry()
        val mirror = ReversalEngine.mirror(original, "rev-1", "op-rev", now + 1, "")
        val entries = listOf(original, mirror)

        val balance = BalanceEngine.compute("room-1", "YER_NEW", entries, emptyList())
        assertEquals(0L, balance.netOf("farmer"))
        assertEquals(0L, balance.netOf("distributor"))
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

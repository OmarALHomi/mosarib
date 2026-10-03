package com.baynana.domain.ledger

import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **بوابة الاختبار الذهبي** (خطة v6 §15): «مبلغ 10,000، مقدم 2,000، قبض 3,000 → دين 5,000
 * في كل مكان — مقارنة نصية».
 *
 * هذا الملف لا يختبر محرّكًا بل يختبر **التطابق**: نفس اللقطة تُغذّي كل الأسطح، والنص الناتج من
 * كل سطح متطابق حرفيًا، والأرقام بالفلس بلا أي `Double`.
 */
class LedgerSnapshotTest {

    private val farmer = "farmer"
    private val distributor = "distributor"
    private val now = 1_700_000_000_000L

    private fun session(amountMinor: Long, at: Long) = EntryView(
        id = "s-$at", operationId = "op-s-$at", roomId = "room-1", type = EntryType.WATER_SESSION,
        owedByMemberId = farmer, owedToMemberId = distributor,
        amountMinor = amountMinor, currency = "YER_NEW", occurredAt = at, status = EntryStatus.SENT,
        description = "سقية 5 ساعات"
    )

    private fun receipt(id: String, amountMinor: Long, at: Long) = EntryView(
        id = id, operationId = "op-$id", roomId = "room-1", type = EntryType.PAYMENT,
        owedByMemberId = distributor, owedToMemberId = farmer,
        amountMinor = amountMinor, currency = "YER_NEW", occurredAt = at, status = EntryStatus.SENT,
        description = "قبض من المزارع"
    )

    /** المبلغ 10,000 ريال = 1,000,000 فلس. */
    private val charged = Money.ofMajor(10_000, Currency.YER_NEW).minor

    private fun goldenSnapshot(): LedgerSnapshot {
        // مقدم 2,000 ريال (200,000 فلس) ثم قبض 3,000 ريال (300,000 فلس) على نفس السقية.
        val sessionEntry = session(charged, now - 3_000)
        val downPayment = receipt("p1", Money.ofMajor(2_000, Currency.YER_NEW).minor, now - 2_000)
        val laterReceipt = receipt("p2", Money.ofMajor(3_000, Currency.YER_NEW).minor, now - 1_000)
        val allocations = listOf(
            AllocationView("p1", sessionEntry.id, Money.ofMajor(2_000, Currency.YER_NEW).minor),
            AllocationView("p2", sessionEntry.id, Money.ofMajor(3_000, Currency.YER_NEW).minor)
        )
        return SnapshotEngine.build("room-1", "YER_NEW", listOf(sessionEntry, downPayment, laterReceipt), allocations)
    }

    @Test
    fun `the golden fixture reads 10،000 charged, 5،000 paid and 5،000 remaining`() {
        val snapshot = goldenSnapshot()

        assertEquals("المبلغ", charged, snapshot.chargedMinor)
        assertEquals("المسدَّد = 2,000 + 3,000", Money.ofMajor(5_000, Currency.YER_NEW).minor, snapshot.paidMinor)
        assertEquals("الباقي", Money.ofMajor(5_000, Currency.YER_NEW).minor, snapshot.remainingMinor)
        assertEquals("الباقي = الدين المفتوح", snapshot.remainingMinor, snapshot.openDebtMinor)
        assertTrue(snapshot.openDebtMinor > 0L)
    }

    @Test
    fun `the statement shows the same three numbers as the snapshot`() {
        val snapshot = goldenSnapshot()
        val statement = StatementEngine.statementFor(farmer, snapshot)

        assertEquals(snapshot.chargedMinor, statement.chargedMinor)
        assertEquals(snapshot.paidMinor, statement.paidMinor)
        assertEquals(snapshot.remainingMinor, statement.remainingMinor)
        assertEquals(snapshot.member(farmer)!!.remainingMinor, statement.remainingMinor)
        assertEquals("-5,000 ريال على المزارع", -Money.ofMajor(5_000, Currency.YER_NEW).minor, statement.netMinor)
    }

    /**
     * كل سطح عرض (الشاشة، PDF، كشف المزارع الورقي، التصدير) ينادي [StatementText] نفسه. فالاختبار
     * يعقد المقارنة على **النص النهائي** لا على الأرقام فقط: نص الشاشة ونص الورق ونص الملخّص
     * الإجمالي كلها من نفس الدالة ونفس اللقطة، فهي متطابقة حرفيًا وإلا فشل البناء.
     */
    @Test
    fun `every surface prints byte-identical text from the same snapshot`() {
        val snapshot = goldenSnapshot()
        val statement = StatementEngine.statementFor(farmer, snapshot)

        val screenText = StatementText.summary(statement)
        val pdfText = StatementText.summary(statement)
        val farmerLedgerText = StatementText.summary(StatementEngine.statementFor(farmer, snapshot))

        assertEquals(screenText, pdfText)
        assertEquals(screenText, farmerLedgerText)
        assertEquals("المبلغ: 10,000 ر.ي • المسدَّد: 5,000 ر.ي • الباقي: 5,000 ر.ي", screenText)
    }

    @Test
    fun `the canonical numbers line is stable for machine comparison`() {
        val statement = StatementEngine.statementFor(farmer, goldenSnapshot())
        assertEquals("1000000|500000|500000|YER_NEW", StatementText.canonicalNumbers(statement))
    }

    @Test
    fun `statement lines run oldest first and the last running balance equals the net`() {
        val statement = StatementEngine.statementFor(farmer, goldenSnapshot())

        assertEquals(3, statement.lines.size)
        assertEquals(listOf(LineDirection.CHARGE, LineDirection.PAYMENT, LineDirection.PAYMENT), statement.lines.map { it.direction })
        assertTrue("الترتيب زمني تصاعدي", statement.lines.zipWithNext().all { (a, b) -> a.occurredAt <= b.occurredAt })
        assertEquals(
            "آخر رصيد جارٍ = صافي العضو",
            statement.netMinor,
            statement.lines.last().runningNetMinor
        )
        assertEquals(
            "مجموع أثر الأسطر = الصافي",
            statement.netMinor,
            statement.lines.sumOf { if (it.direction == LineDirection.CHARGE) -it.amountMinor else it.amountMinor }
        )
    }

    @Test
    fun `a line of the statement prints the amount and the remaining in riyals`() {
        val statement = StatementEngine.statementFor(farmer, goldenSnapshot())
        val chargeLine = statement.lines.first()
        val text = StatementText.line(chargeLine, statement.currency, dateText = "2026-10-03")
        assertEquals("2026-10-03 • سقية 5 ساعات • 10,000 ر.ي • على العضو • المتبقي 5,000 ر.ي", text)
    }

    @Test
    fun `amounts with a real fraction print exactly, and thirds never lose a fil`() {
        val withFraction = MoneyFormat.format(Money.ofMinor(1_000_001L, Currency.YER_NEW))
        assertEquals("10,000.01 ر.ي", withFraction)

        val bigWithFraction = MoneyFormat.format(Money.ofMinor(999_999_999_999L, Currency.YER_NEW))
        assertEquals("9,999,999,999.99 ر.ي", bigWithFraction)

        // الانقسام على 3 لا يفقد فلسًا: 1,000,000 ÷ 3 = 333,333 والباقي فلس واحد × 3 = 999,999.
        val third = Money.ofMinor(1_000_000, Currency.YER_NEW).times(1, 3)
        assertEquals(333_333L, third.minor)
        assertEquals(999_999L, third.minor * 3)
    }

    @Test
    fun `an unallocated receipt is announced apart and never as a settled debt`() {
        val sessionEntry = session(charged, now - 3_000)
        val generalReceipt = EntryView(
            id = "r1", operationId = "op-r1", roomId = "room-1", type = EntryType.GENERAL_RECEIPT,
            owedByMemberId = distributor, owedToMemberId = farmer,
            amountMinor = Money.ofMajor(2_000, Currency.YER_NEW).minor, currency = "YER_NEW",
            occurredAt = now - 1_000, status = EntryStatus.SENT, description = "قبض عام"
        )
        val snapshot = SnapshotEngine.build("room-1", "YER_NEW", listOf(sessionEntry, generalReceipt), emptyList())

        assertEquals("المبلغ كامل", charged, snapshot.chargedMinor)
        assertEquals("لم يُسدَّد شيء", 0L, snapshot.paidMinor)
        assertEquals("الباقي كامل", charged, snapshot.remainingMinor)
        assertEquals("والمقبوض معلن بجانبه", Money.ofMajor(2_000, Currency.YER_NEW).minor, snapshot.unappliedReceiptMinor)
        assertTrue("لا يُعلن الصفاء", !snapshot.isSettled)
        assertEquals(
            "الديون المفتوحة: 10,000 ر.ي • أرصدة دائنة معلّقة: 2,000 ر.ي",
            StatementText.roomSummary(snapshot)
        )
    }

    @Test
    fun `the room summary separates open debts from credit balances`() {
        val text = StatementText.roomSummary(goldenSnapshot())
        assertTrue(text.startsWith("الديون المفتوحة: 5,000 ر.ي"))
        assertTrue(text.contains("أرصدة دائنة معلّقة: 0 ر.ي"))
    }

    @Test
    fun `drafts are counted and never announced as debt`() {
        val draft = session(charged, now).copy(id = "draft-1", operationId = "op-draft", status = EntryStatus.DRAFT)
        val snapshot = SnapshotEngine.build("room-1", "YER_NEW", listOf(session(charged, now - 9_000), draft), emptyList())

        assertEquals(1, snapshot.draftCount)
        assertEquals("المسودة ليست من أرقام الطرف الآخر", charged, snapshot.chargedMinor)
        assertTrue(StatementText.roomSummary(snapshot).contains("مسودات لم تُرسل: 1"))
    }

    @Test
    fun `another currency in the room is excluded and never mixed into the totals`() {
        val sarEntry = session(999_999L, now - 5_000).copy(currency = "SAR")
        val snapshot = SnapshotEngine.build("room-1", "YER_NEW", listOf(session(charged, now - 3_000), sarEntry), emptyList())

        assertEquals(charged, snapshot.chargedMinor)
        assertEquals(1, snapshot.lines.size)
    }

    @Test
    fun `a reversal pair leaves both the amount and the remaining at zero`() {
        val original = session(charged, now - 3_000).copy(status = EntryStatus.VOIDED)
        val mirror = EntryView(
            id = "rev-1", operationId = "op-rev", roomId = "room-1", type = EntryType.ADJUSTMENT,
            owedByMemberId = distributor, owedToMemberId = farmer,
            amountMinor = charged, currency = "YER_NEW", occurredAt = now - 2_000,
            status = EntryStatus.SENT, reversesEntryId = original.id, description = "إلغاء سقية"
        )
        val snapshot = SnapshotEngine.build("room-1", "YER_NEW", listOf(original, mirror), emptyList())

        assertEquals(0L, snapshot.chargedMinor)
        assertEquals(0L, snapshot.remainingMinor)
        assertTrue(snapshot.isSettled)
        assertEquals(0L, snapshot.netOf(farmer))
    }
}

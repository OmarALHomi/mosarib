package com.baynana.domain.settlement

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * بوابة ح٨ في طبقة القواعد (بلا قاعدة ولا شبكة): لا بيع مزدوج، ولا تعديل من غير مخوّل،
 * والأرقام متطابقة (مجموع الأقساط = المتبقي، والسعاية لا تختلط بدَين المحصول).
 */
class DealEngineTest {

    private val day = 86_400_000L
    private val dealAt = 1_700_000_000_000L

    private fun seller(id: String = "m-seller", name: String = "أحمد المزارع") = DealEngine.Party(id, name)
    private fun buyer(id: String = "m-buyer", name: String = "صالح المجبري") = DealEngine.Party(id, name)
    private fun broker(id: String = "m-broker", name: String = "علي الدلال") = DealEngine.Party(id, name)

    private fun draft(
        id: String = "deal-1",
        listingId: String? = "listing-1",
        totalMinor: Long = 5_000_000L,
        advanceMinor: Long = 1_000_000L,
        count: Int = 4,
        intervalDays: Int = 30,
        commission: CommissionEngine.Policy = CommissionEngine.Policy(
            payer = CommissionPayer.BUYER,
            rateBasisPoints = 250
        ),
        createdBy: String = "m-broker"
    ) = DealEngine.Draft(
        id = id,
        listingId = listingId,
        cropTitle = "قمح",
        place = "صنعاء",
        currency = "YER_NEW",
        totalMinor = totalMinor,
        advanceMinor = advanceMinor,
        seller = seller(),
        buyer = buyer(),
        broker = broker(),
        dealAt = dealAt,
        firstDueAt = dealAt + 30 * day,
        installmentCount = count,
        intervalDays = intervalDays,
        commission = commission,
        terms = "تسليم في السوق",
        createdByMemberId = createdBy
    )

    private fun openListing(state: String = ListingState.OPEN, holder: String? = null) =
        DealEngine.ListingSnapshot(id = "listing-1", state = state, reservedByDealId = holder)

    private fun allowedPlan(draft: DealEngine.Draft = draft()): DealEngine.Plan {
        val result = DealEngine.check(draft, openListing())
        assertTrue("الصلح يجب أن يُقبل: $result", result is DealEngine.Check.Allowed)
        return (result as DealEngine.Check.Allowed).plan
    }

    private fun refusal(draft: DealEngine.Draft, listing: DealEngine.ListingSnapshot? = openListing(), others: List<DealEngine.DealSummary> = emptyList()): String {
        val result = DealEngine.check(draft, listing, others)
        assertTrue("كان المتوقع رفضًا لكن جاء: $result", result is DealEngine.Check.Refused)
        return (result as DealEngine.Check.Refused).reason
    }

    // ------------------------------------------------------------- الفحص

    @Test
    fun `a sound deal passes and its numbers reconcile exactly`() {
        val plan = allowedPlan()
        assertEquals(4_000_000L, plan.remainingMinor)
        assertEquals("مجموع الأقساط = المتبقي بلا فلس ضائع", 4_000_000L, plan.schedule.sumOf { it.amountMinor })
        assertEquals("السعاية 2.5% من 50,000 ريال = 1,250 ريال", 125_000L, plan.commission.totalMinor)
        // المشتري عليه: المتبقي + نصيبه من السعاية، والبائع يستلم: المتبقي − نصيبه.
        assertEquals(4_000_000L + 125_000L, plan.buyerOwesMinor)
        assertEquals(4_000_000L, plan.sellerReceivesMinor)
        assertEquals("العربون + المتبقي = القيمة", plan.draft.totalMinor, plan.draft.advanceMinor + plan.remainingMinor)
    }

    @Test
    fun `no one is seller and buyer in the same deal`() {
        val reason = refusal(draft().copy(buyer = seller()))
        assertTrue(reason, reason.contains("بائعًا ومشتريًا"))
    }

    @Test
    fun `a broker who is also a party is refused because the two accounts would mix`() {
        val reason = refusal(draft().copy(broker = buyer()))
        assertTrue(reason, reason.contains("دلالًا"))
    }

    @Test
    fun `an unregistered buyer is accepted by name, as the plan says for off-app deals`() {
        val plan = allowedPlan(draft().copy(buyer = DealEngine.Party(memberId = "", name = "ضيف من السوق", phone = "777000111")))
        assertEquals("ضيف من السوق", plan.draft.buyer.name)
    }

    @Test
    fun `a blank crop or a zero value is refused with a clear reason`() {
        assertTrue(refusal(draft().copy(cropTitle = "  ")).contains("المحصول"))
        assertTrue(refusal(draft().copy(totalMinor = 0L, advanceMinor = 0L)).contains("أكبر من صفر"))
    }

    @Test
    fun `an advance bigger than the price is refused`() {
        assertTrue(refusal(draft().copy(advanceMinor = 6_000_000L)).contains("العربون أكبر"))
    }

    @Test
    fun `a due date before the deal date is refused, never silently accepted`() {
        assertTrue(refusal(draft().copy(firstDueAt = dealAt - day)).contains("قبل تاريخ الصلح"))
    }

    @Test
    fun `an outstanding balance without a due date is refused`() {
        assertTrue(refusal(draft().copy(firstDueAt = null)).contains("موعد أول قسط"))
    }

    @Test
    fun `an advance that covers the price leaves no installments and is fully accepted`() {
        val plan = allowedPlan(draft().copy(advanceMinor = 5_000_000L))
        assertEquals(0L, plan.remainingMinor)
        assertTrue(plan.schedule.isEmpty())
        assertEquals(0L, plan.buyerOwesMinor - plan.commission.buyerMinor)
    }

    // ------------------------------------------------------------- السعاية

    @Test
    fun `a commission above the hard cap is refused even if the parties agree`() {
        val reason = refusal(draft().copy(commission = CommissionEngine.Policy(rateBasisPoints = 2_500)))
        assertTrue(reason, reason.contains("السقف"))
    }

    @Test
    fun `a rate and a fixed amount together are refused as an ambiguous policy`() {
        val reason = refusal(
            draft().copy(commission = CommissionEngine.Policy(rateBasisPoints = 100, fixedMinor = 50_000L))
        )
        assertTrue(reason, reason.contains("لا الاثنان"))
    }

    @Test
    fun `the cap limits the commission and says so in the notes`() {
        val plan = allowedPlan(
            draft().copy(
                commission = CommissionEngine.Policy(
                    payer = CommissionPayer.BUYER,
                    rateBasisPoints = 1_000,
                    capMinor = 300_000L
                )
            )
        )
        assertEquals(300_000L, plan.commission.totalMinor)
        assertTrue(plan.commission.cappedByMinor > 0L)
    }

    @Test
    fun `a split commission gives exact shares that add up to the total`() {
        val plan = allowedPlan(
            draft().copy(
                commission = CommissionEngine.Policy(
                    payer = CommissionPayer.SPLIT,
                    rateBasisPoints = 300,
                    sellerShareBasisPoints = 3_333
                )
            )
        )
        assertEquals(150_000L, plan.commission.totalMinor)
        assertEquals(plan.commission.totalMinor, plan.commission.sellerMinor + plan.commission.buyerMinor)
        assertEquals(49_995L, plan.commission.sellerMinor)
    }

    @Test
    fun `a seller-paid commission reduces what the seller receives and never goes negative`() {
        val plan = allowedPlan(
            draft().copy(
                totalMinor = 500_000L,
                advanceMinor = 0L,
                count = 1,
                commission = CommissionEngine.Policy(
                    payer = CommissionPayer.SELLER,
                    rateBasisPoints = 2_000
                )
            )
        )
        assertEquals(100_000L, plan.commission.totalMinor)
        assertEquals(400_000L, plan.sellerReceivesMinor)
    }

    // ------------------------------------------------------------- الأقساط

    @Test
    fun `a seller commission bigger than what remains is refused, not turned into a negative payout`() {
        val reason = refusal(
            draft().copy(
                totalMinor = 500_000L,
                advanceMinor = 490_000L,
                count = 1,
                commission = CommissionEngine.Policy(payer = CommissionPayer.SELLER, rateBasisPoints = 500)
            )
        )
        assertTrue(reason, reason.contains("أكبر من المتبقي"))
        assertTrue("الرسالة تقترح مخرجًا", reason.contains("اجعل السعاية على المشتري"))
    }

    @Test
    fun `an indivisible remainder goes to the last installment so the sum is exact`() {
        val result = InstallmentScheduleEngine.build(
            totalMinor = 1_000_000L,
            advanceMinor = 0L,
            count = 3,
            firstDueAt = dealAt + day,
            intervalDays = 30,
            currencyCode = "YER_NEW"
        )
        val ok = result as InstallmentScheduleEngine.Check.Ok
        assertEquals(listOf(333_333L, 333_333L, 333_334L), ok.installments.map { it.amountMinor })
        assertEquals(1_000_000L, ok.installments.sumOf { it.amountMinor })
    }

    @Test
    fun `a schedule with too many installments or a silly interval is refused`() {
        val many = InstallmentScheduleEngine.build(1_000_000L, 0L, 40, dealAt + day, 30, "YER_NEW")
        assertTrue((many as InstallmentScheduleEngine.Check.Refused).reason.contains("أكبر من الحد"))
        val fast = InstallmentScheduleEngine.build(1_000_000L, 0L, 4, dealAt + day, 0, "YER_NEW")
        assertTrue((fast as InstallmentScheduleEngine.Check.Refused).reason.contains("الفاصل"))
    }

    @Test
    fun `installment due dates increase by the agreed interval`() {
        val ok = InstallmentScheduleEngine.build(400_000L, 0L, 4, dealAt + 10 * day, 15, "YER_NEW")
            as InstallmentScheduleEngine.Check.Ok
        assertEquals(4, ok.installments.size)
        assertEquals(dealAt + 10 * day, ok.installments.first().dueAt)
        assertEquals(dealAt + 55 * day, ok.installments.last().dueAt)
        assertTrue(ok.installments.zipWithNext().all { (a, b) -> b.dueAt > a.dueAt })
    }

    // ------------------------------------------------------------- لا بيع مزدوج

    @Test
    fun `a listing already sold refuses a second deal clearly`() {
        val reason = refusal(draft(), openListing(state = ListingState.SOLD))
        assertTrue(reason, reason.contains("لا بيع مزدوج"))
    }

    @Test
    fun `a listing reserved by another deal is refused, and by this deal is allowed`() {
        assertTrue(refusal(draft(), openListing(ListingState.RESERVED, "deal-9")).contains("محجوز لصلح آخر"))
        val mine = DealEngine.check(draft(), openListing(ListingState.RESERVED, "deal-1"))
        assertTrue("صلحي أنا لا يمنعني من نفسي", mine is DealEngine.Check.Allowed)
    }

    @Test
    fun `an existing active deal on the same listing blocks a competitor even if the listing looks open`() {
        val others = listOf(DealEngine.DealSummary("deal-1", "listing-1", DealStatus.ACTIVE))
        val reason = refusal(draft(), openListing(), others)
        assertTrue(reason, reason.contains("لا بيع مزدوج"))
    }

    @Test
    fun `a draft or a cancelled deal on the listing does not block a new one`() {
        val others = listOf(
            DealEngine.DealSummary("deal-1", "listing-1", DealStatus.DRAFT),
            DealEngine.DealSummary("deal-2", "listing-1", DealStatus.CANCELLED)
        )
        val result = DealEngine.check(draft(), openListing(), others)
        assertTrue("المسودة والمفسوخ لا يحجزان شيئًا: $result", result is DealEngine.Check.Allowed)
    }

    @Test
    fun `a withdrawn listing is refused with its own reason`() {
        assertTrue(refusal(draft(), openListing(state = ListingState.WITHDRAWN)).contains("مسحوب"))
    }

    @Test
    fun `a deal without a listing is a private deal and needs no reservation`() {
        val plan = allowedPlan(draft(listingId = null))
        assertEquals(null, plan.draft.listingId)
    }

    // ------------------------------------------------------------- القبض

    private fun state(
        status: String = DealStatus.ACTIVE,
        installments: List<DealEngine.InstallmentState> = listOf(
            DealEngine.InstallmentState("ins-1", 1, dealAt + 30 * day, 1_000_000L),
            DealEngine.InstallmentState("ins-2", 2, dealAt + 60 * day, 1_000_000L),
            DealEngine.InstallmentState("ins-3", 3, dealAt + 90 * day, 1_000_000L)
        ),
        advance: Long = 1_000_000L
    ) = DealEngine.State(
        dealId = "deal-1",
        status = status,
        currency = "YER_NEW",
        installments = installments,
        commission = CommissionEngine.Breakdown(
            totalMinor = 125_000L,
            sellerMinor = 0L,
            buyerMinor = 125_000L,
            rateBasisPoints = 250,
            cappedByMinor = 0L
        ),
        advanceMinor = advance
    )

    @Test
    fun `a payment goes to the oldest due installment first`() {
        val result = DealEngine.planPayment(state(), DealEngine.PaymentRequest("op-1", 600_000L, dealAt + day))
        val plan = (result as DealEngine.PaymentCheck.Allowed).plan
        assertEquals(listOf("ins-1"), plan.allocations.map { it.installmentId })
        assertEquals(600_000L, plan.allocations.single().amountMinor)
        assertEquals(0L, plan.excessMinor)
        assertEquals(InstallmentStatus.PARTIAL, plan.newInstallments.first { it.id == "ins-1" }.status)
        assertEquals(DealStatus.ACTIVE, plan.resultingStatus)
    }

    @Test
    fun `a payment that covers several installments closes them in order and detects completion`() {
        val plan = (DealEngine.planPayment(
            state(),
            DealEngine.PaymentRequest("op-1", 2_500_000L, dealAt + day)
        ) as DealEngine.PaymentCheck.Allowed).plan
        assertEquals(listOf("ins-1", "ins-2", "ins-3"), plan.allocations.map { it.installmentId })
        assertEquals(listOf(1_000_000L, 1_000_000L, 500_000L), plan.allocations.map { it.amountMinor })
        assertEquals(DealStatus.ACTIVE, plan.resultingStatus)
    }

    @Test
    fun `paying everything marks the deal completed and the listing sold`() {
        val plan = (DealEngine.planPayment(
            state(),
            DealEngine.PaymentRequest("op-1", 3_000_000L, dealAt + day)
        ) as DealEngine.PaymentCheck.Allowed).plan
        assertEquals(DealStatus.COMPLETED, plan.resultingStatus)
        assertTrue(plan.notes.any { it.contains("تم البيع") })
    }

    @Test
    fun `an overpayment becomes a credit in the same room, never at a third party`() {
        val plan = (DealEngine.planPayment(
            state(),
            DealEngine.PaymentRequest("op-1", 4_000_000L, dealAt + day)
        ) as DealEngine.PaymentCheck.Allowed).plan
        assertEquals(1_000_000L, plan.excessMinor)
        assertTrue(plan.notes.any { it.contains("رصيد دائن في غرفة الصلح نفسها") })
        assertEquals(3_000_000L, plan.allocations.sumOf { it.amountMinor })
    }

    @Test
    fun `a replayed payment is refused at the pure layer so it cannot be deducted twice`() {
        val result = DealEngine.planPayment(
            state(),
            DealEngine.PaymentRequest("op-1", 500_000L, dealAt + day, seenBefore = true)
        )
        assertTrue((result as DealEngine.PaymentCheck.Refused).reason.contains("لا تُخصم مرتين"))
    }

    @Test
    fun `collecting on a cancelled deal is refused with a reason`() {
        val result = DealEngine.planPayment(state(status = DealStatus.CANCELLED), DealEngine.PaymentRequest("op-1", 500_000L, dealAt))
        assertTrue((result as DealEngine.PaymentCheck.Refused).reason.contains("مفسوخ"))
    }

    @Test
    fun `a payment on a completed deal is kept whole as a credit, not applied to nothing`() {
        val plan = (DealEngine.planPayment(
            state(status = DealStatus.COMPLETED),
            DealEngine.PaymentRequest("op-1", 700_000L, dealAt)
        ) as DealEngine.PaymentCheck.Allowed).plan
        assertTrue(plan.allocations.isEmpty())
        assertEquals(700_000L, plan.excessMinor)
    }

    @Test
    fun `a zero or negative payment is refused`() {
        assertTrue((DealEngine.planPayment(state(), DealEngine.PaymentRequest("op-1", 0L, dealAt)) as DealEngine.PaymentCheck.Refused)
            .reason.contains("أكبر من صفر"))
        assertTrue((DealEngine.planPayment(state(), DealEngine.PaymentRequest("op-1", -5L, dealAt)) as DealEngine.PaymentCheck.Refused)
            .reason.contains("أكبر من صفر"))
    }

    @Test
    fun `an already paid installment is skipped, not paid twice`() {
        val closed = listOf(
            DealEngine.InstallmentState("ins-1", 1, dealAt + 30 * day, 1_000_000L, 1_000_000L, InstallmentStatus.PAID),
            DealEngine.InstallmentState("ins-2", 2, dealAt + 60 * day, 1_000_000L)
        )
        val plan = (DealEngine.planPayment(state(installments = closed), DealEngine.PaymentRequest("op-1", 300_000L, dealAt))
            as DealEngine.PaymentCheck.Allowed).plan
        assertEquals(listOf("ins-2"), plan.allocations.map { it.installmentId })
    }

    @Test
    fun `installments out of order are still paid oldest first`() {
        val shuffled = listOf(
            DealEngine.InstallmentState("ins-3", 3, dealAt + 90 * day, 1_000_000L),
            DealEngine.InstallmentState("ins-1", 1, dealAt + 30 * day, 1_000_000L),
            DealEngine.InstallmentState("ins-2", 2, dealAt + 60 * day, 1_000_000L)
        )
        val plan = (DealEngine.planPayment(state(installments = shuffled), DealEngine.PaymentRequest("op-1", 1_500_000L, dealAt))
            as DealEngine.PaymentCheck.Allowed).plan
        assertEquals(listOf("ins-1", "ins-2"), plan.allocations.map { it.installmentId })
    }

    // ------------------------------------------------------------- الفسخ

    @Test
    fun `cancellation reverses what was collected and closes the open installments without deleting`() {
        val partlyPaid = listOf(
            DealEngine.InstallmentState("ins-1", 1, dealAt + 30 * day, 1_000_000L, 400_000L, InstallmentStatus.PARTIAL),
            DealEngine.InstallmentState("ins-2", 2, dealAt + 60 * day, 1_000_000L)
        )
        val plan = (DealEngine.planCancellation(state(installments = partlyPaid)) as DealEngine.CancellationCheck.Allowed).plan
        assertEquals(DealStatus.CANCELLED, plan.status)
        assertEquals("العربون + ما دُفع", 1_400_000L, plan.amountsToReverseMinor)
        assertEquals(listOf("ins-1", "ins-2"), plan.cancelledInstallments)
        assertTrue(plan.notes.any { it.contains("لا يمحو") })
    }

    @Test
    fun `cancelling twice, or reversing twice, is refused`() {
        assertTrue((DealEngine.planCancellation(state(status = DealStatus.CANCELLED)) as DealEngine.CancellationCheck.Refused)
            .reason.contains("مفسوخ سابقًا"))
        assertTrue((DealEngine.planCancellation(state(), reversedBefore = true) as DealEngine.CancellationCheck.Refused)
            .reason.contains("لا عكس مرتين"))
    }

    @Test
    fun `cancelling without any collection just closes the schedule`() {
        val plan = (DealEngine.planCancellation(state(advance = 0L)) as DealEngine.CancellationCheck.Allowed).plan
        assertEquals(0L, plan.amountsToReverseMinor)
        assertTrue(plan.notes.any { it.contains("بلا قيود") })
    }

    // ------------------------------------------------------------- التعديل

    @Test
    fun `only the deal creator may edit, and only before acknowledgement and first payment`() {
        val d = draft()
        assertTrue(DealEngine.checkEdit(d, "m-broker", acknowledged = false, anyPaymentMade = false) is DealEngine.EditCheck.Allowed)
        assertTrue((DealEngine.checkEdit(d, "m-seller", false, false) as DealEngine.EditCheck.Refused)
            .reason.contains("من غير منشئ الصلح"))
        assertTrue((DealEngine.checkEdit(d, "m-broker", false, true) as DealEngine.EditCheck.Refused)
            .reason.contains("بعد أول دفعة"))
        assertTrue((DealEngine.checkEdit(d, "m-broker", true, false) as DealEngine.EditCheck.Refused)
            .reason.contains("مُقرّ"))
    }

    @Test
    fun `a deal with no creator cannot be edited at all`() {
        assertTrue((DealEngine.checkEdit(draft(createdBy = ""), "m-broker", false, false) as DealEngine.EditCheck.Refused)
            .reason.contains("منشئ الصلح"))
    }

    // ------------------------------------------------------------- البيان

    @Test
    fun `the statement is one text with the same numbers the plan holds`() {
        val text = DealEngine.statementText(allowedPlan())
        assertTrue(text, text.contains("بيان صلح: قمح • صنعاء"))
        assertTrue(text, text.contains("القيمة: 50,000 ر.ي"))
        assertTrue(text, text.contains("العربون: 10,000 ر.ي"))
        assertTrue(text, text.contains("المتبقي: 40,000 ر.ي"))
        assertTrue(text, text.contains("السعاية: 1,250 ر.ي (على المشتري)"))
        assertTrue(text, text.contains("المشتري عليه: 41,250 ر.ي"))
        assertTrue(text, text.contains("البائع يستلم: 40,000 ر.ي"))
        assertTrue(text, text.contains("الأقساط: 4 كل 30 يومًا"))
        assertTrue(text, text.contains("الشروط: تسليم في السوق"))
    }

    @Test
    fun `a fully prepaid deal says there are no installments, not zero riyal ones`() {
        val text = DealEngine.statementText(allowedPlan(draft().copy(advanceMinor = 5_000_000L)))
        assertTrue(text, text.contains("لا أقساط: العربون غطّى القيمة كاملة"))
    }

    @Test
    fun `the status line shows the remaining amount only when there is one`() {
        assertEquals("قائم • المتبقي: 40,000 ر.ي", DealEngine.statusLine(DealStatus.ACTIVE, 4_000_000L, "YER_NEW"))
        assertEquals("مكتمل • لا متبقٍ", DealEngine.statusLine(DealStatus.COMPLETED, 0L, "YER_NEW"))
    }
}

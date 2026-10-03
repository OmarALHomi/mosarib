package com.baynana.data.local.settlement

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.baynana.core.database.AppDatabase
import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.settlement.CommissionEngine
import com.baynana.domain.settlement.CommissionPayer
import com.baynana.domain.settlement.CommissionStatus
import com.baynana.domain.settlement.DealEngine
import com.baynana.domain.settlement.DealStatus
import com.baynana.domain.settlement.InstallmentStatus
import com.baynana.domain.settlement.ListingState
import com.baynana.domain.ledger.NewDebtSpec
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
 * بوابة ح٨ على قاعدة حقيقية: الصلح يُكتب في الدفتر نفسه (لا محرّك حساب ثانٍ)، ولا بيع مزدوج،
 * ولا تعديل من غير مخوّل، والسعاية في بيانها المستقل، والفسخ يعكس ولا يمحو.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DealRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DealRepository
    private val day = 86_400_000L
    private val dealAt = 1_700_000_000_000L
    private var counter = 0

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = DealRepository(db, newId = { "id-${++counter}" }, now = { dealAt })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun draft(
        id: String = "deal-1",
        listingId: String? = "listing-1",
        totalMinor: Long = 5_000_000L,
        advanceMinor: Long = 1_000_000L,
        count: Int = 4,
        commission: CommissionEngine.Policy = CommissionEngine.Policy(
            payer = CommissionPayer.BUYER,
            rateBasisPoints = 250
        ),
        createdBy: String = DealRepository.OWNER_MEMBER_ID
    ) = DealEngine.Draft(
        id = id,
        listingId = listingId,
        cropTitle = "قمح",
        place = "صنعاء",
        currency = "YER_NEW",
        totalMinor = totalMinor,
        advanceMinor = advanceMinor,
        seller = DealEngine.Party("m-seller", "أحمد المزارع", "777111222"),
        buyer = DealEngine.Party("m-buyer", "صالح المجبري", "777333444"),
        broker = DealEngine.Party(DealRepository.OWNER_MEMBER_ID, "أنا", "777999000"),
        dealAt = dealAt,
        firstDueAt = dealAt + 30 * day,
        installmentCount = count,
        intervalDays = 30,
        commission = commission,
        terms = "تسليم في السوق",
        createdByMemberId = createdBy
    )

    private fun listing(state: String = ListingState.OPEN, holder: String? = null) =
        DealEngine.ListingSnapshot(id = "listing-1", state = state, reservedByDealId = holder)

    private suspend fun opened(dealId: String = "deal-1"): DealRepository.OpenOutcome {
        val result = repository.open(draft(id = dealId), listing())
        assertTrue("الصلح يجب أن يُفتح: $result", result is DealRepository.OpenResult.Opened)
        return (result as DealRepository.OpenResult.Opened).outcome
    }

    @Test
    fun `opening a deal writes the debt, the advance and the schedule in one place`() = runBlocking {
        val outcome = opened()

        // دَين المحصول في غرفة الصلح: القيمة كاملة، والعربون سداد مُخصَّص عليها.
        val debt = db.ledgerDao().getEntry("deal-debt-deal-1")!!
        assertEquals(EntryType.SETTLEMENT, debt.type)
        assertEquals(5_000_000L, debt.amountMinor)
        assertEquals("m-buyer", debt.owedByMemberId)
        assertEquals("m-seller", debt.owedToMemberId)
        assertEquals("listing-1", debt.listingId)
        assertEquals(dealAt, debt.occurredAt)

        val advance = db.ledgerDao().getEntry("deal-advance-deal-1")!!
        assertEquals(EntryType.PAYMENT, advance.type)
        assertEquals(1_000_000L, advance.amountMinor)
        assertEquals("العربون يُخصَّص على دَين المحصول", 1_000_000L, db.ledgerDao().allocatedToDebt(debt.id))

        // أربعة أقساط مجموعها = المتبقي بالضبط.
        assertEquals(4, outcome.installments.size)
        assertEquals(4_000_000L, outcome.installments.sumOf { it.amountMinor })
        assertEquals(InstallmentStatus.SCHEDULED, outcome.installments.first().status)

        // والمخطط صار مكتوبًا فعلًا: لقطة الغرفة تُظهر نفس الأرقام.
        val snapshot = com.baynana.data.local.ledger.LedgerRepository(db).snapshot(outcome.deal.roomId)
        assertEquals(5_000_000L, snapshot.chargedMinor)
        assertEquals(1_000_000L, snapshot.paidMinor)
        assertEquals(4_000_000L, snapshot.remainingMinor)
    }

    @Test
    fun `the commission lives in its own room and never mixes with the crop debt`() = runBlocking {
        val outcome = opened()
        assertEquals(1, outcome.commissions.size)
        val fee = outcome.commissions.single()
        assertEquals(125_000L, fee.totalMinor)
        assertEquals("غرفة مستقلة عن غرفة الصلح", false, fee.roomId == outcome.deal.roomId)
        assertEquals(CommissionStatus.OPEN, fee.status)

        val feeEntry = db.ledgerDao().getEntry(fee.entryId!!)!!
        assertEquals("m-buyer", feeEntry.owedByMemberId)
        assertEquals(DealRepository.OWNER_MEMBER_ID, feeEntry.owedToMemberId)
        assertEquals(125_000L, feeEntry.amountMinor)

        // دَين المحصول لم يتغيّر بسبب السعاية: بيانان لا يختلطان.
        assertEquals(5_000_000L, db.ledgerDao().getEntry("deal-debt-deal-1")!!.amountMinor)
    }

    @Test
    fun `a second deal on a listing that is already taken is refused before anything is written`() = runBlocking {
        opened("deal-1")
        val second = repository.open(draft(id = "deal-2", createdBy = "m-broker-2"), listing())
        assertTrue(second is DealRepository.OpenResult.Refused)
        assertTrue((second as DealRepository.OpenResult.Refused).reason.contains("لا بيع مزدوج"))
        assertEquals(null, db.ledgerDao().getRoom("deal-room-deal-2"))
        assertEquals(0, db.ledgerDao().getEntriesIncludingVoided("deal-room-deal-2").size)
    }

    @Test
    fun `a sold listing cannot be dealt on at all`() = runBlocking {
        val result = repository.open(draft(), listing(state = ListingState.SOLD))
        assertTrue((result as DealRepository.OpenResult.Refused).reason.contains("لا بيع مزدوج"))
    }

    @Test
    fun `a payment closes the oldest installment and moves the same money in the ledger`() = runBlocking {
        val outcome = opened()
        val paymentAt = dealAt + 31 * day
        val result = repository.recordPayment("deal-1", 1_000_000L, paymentAt, "op-pay-1")
        assertTrue("$result", result is DealRepository.PaymentResult.Recorded)
        val recorded = (result as DealRepository.PaymentResult.Recorded).outcome
        assertEquals(1_000_000L, recorded.appliedMinor)
        assertEquals(0L, recorded.excessMinor)
        assertEquals(DealStatus.ACTIVE, recorded.status)

        val installments = db.dealDao().getInstallments("deal-1")
        assertEquals(InstallmentStatus.PAID, installments.first().status)
        assertEquals(1_000_000L, installments.first().paidMinor)

        // القسط في الدفتر: تخصيص على دَين المحصول، واللقطة تقول نفس الرقم.
        val snapshot = com.baynana.data.local.ledger.LedgerRepository(db).snapshot(outcome.deal.roomId)
        assertEquals(2_000_000L, snapshot.paidMinor)
        assertEquals(3_000_000L, snapshot.remainingMinor)
    }

    @Test
    fun `a replayed payment writes no second deduction`() = runBlocking {
        opened()
        val first = repository.recordPayment("deal-1", 500_000L, dealAt + day, "op-pay-1")
        assertTrue(first is DealRepository.PaymentResult.Recorded)
        val replay = repository.recordPayment("deal-1", 500_000L, dealAt + day, "op-pay-1")

        assertTrue("إعادة نفس العملية لا تُخصم مرتين: $replay", replay is DealRepository.PaymentResult.Replayed)
        val installments = db.dealDao().getInstallments("deal-1")
        assertEquals("القسط الأول لم يُخصم مرتين", 500_000L, installments.first().paidMinor)
        val snapshot = com.baynana.data.local.ledger.LedgerRepository(db).snapshot("deal-room-deal-1")
        assertEquals(1_500_000L, snapshot.paidMinor)
    }

    @Test
    fun `paying the rest completes the deal and closes the schedule`() = runBlocking {
        opened()
        val result = repository.recordPayment("deal-1", 4_000_000L, dealAt + 60 * day, "op-pay-full")
        val recorded = (result as DealRepository.PaymentResult.Recorded).outcome
        assertEquals(DealStatus.COMPLETED, recorded.status)
        assertEquals(DealStatus.COMPLETED, db.dealDao().getDeal("deal-1")!!.status)
        assertTrue(db.dealDao().getInstallments("deal-1").all { it.status == InstallmentStatus.PAID })
        assertNotNull("تاريخ الإغلاق يُسجَّل", db.dealDao().getDeal("deal-1")!!.closedAt)
    }

    @Test
    fun `an overpayment stays a credit in the deal room, not in a third party's account`() = runBlocking {
        opened()
        val result = repository.recordPayment("deal-1", 5_000_000L, dealAt + 60 * day, "op-pay-big")
        val recorded = (result as DealRepository.PaymentResult.Recorded).outcome
        assertEquals(4_000_000L, recorded.appliedMinor)
        assertEquals(1_000_000L, recorded.excessMinor)
        // الرصيد الدائن في نفس الغرفة: المسدد في اللقطة يساوي المخصَّص لا المدفوع كله.
        val snapshot = com.baynana.data.local.ledger.LedgerRepository(db).snapshot("deal-room-deal-1")
        assertEquals(5_000_000L, snapshot.paidMinor)
        assertEquals(0L, snapshot.remainingMinor)
        assertEquals(1_000_000L, snapshot.unappliedReceiptMinor)
    }

    @Test
    fun `paying on a cancelled deal is refused with a reason and changes nothing`() = runBlocking {
        opened()
        val cancel = repository.cancel("deal-1", "اتفق الطرفان على الفسخ", "op-cancel-1", dealAt + 2 * day)
        assertTrue(cancel is DealRepository.CancelResult.Cancelled)

        val payment = repository.recordPayment("deal-1", 100_000L, dealAt + 3 * day, "op-pay-after")
        assertTrue((payment as DealRepository.PaymentResult.Refused).reason.contains("مفسوخ"))
        assertEquals(0, db.dealDao().getInstallments("deal-1").sumOf { it.paidMinor })
    }

    @Test
    fun `cancellation reverses the collected money and keeps every record`() = runBlocking {
        opened()
        repository.recordPayment("deal-1", 700_000L, dealAt + 10 * day, "op-pay-1")
        val result = repository.cancel("deal-1", "تلف المحصول قبل التسليم", "op-cancel-1", dealAt + 20 * day)
        val outcome = (result as DealRepository.CancelResult.Cancelled).outcome

        assertEquals(DealStatus.CANCELLED, outcome.status)
        assertTrue("العربون والقسط يُعكسان معًا", outcome.reversedEntries >= 2)
        assertEquals(DealStatus.CANCELLED, db.dealDao().getDeal("deal-1")!!.status)
        assertTrue(db.dealDao().getInstallments("deal-1").none { InstallmentStatus.isOpen(it.status) })
        assertEquals(
            "السعاية تُعكس مع الصلح",
            CommissionStatus.REVERSED,
            db.dealDao().getCommissions("deal-1").single().status
        )

        // لا صف مُحي: القيود والعكس موجودة، والدفتر يرجع إلى الصفر.
        val entries = db.ledgerDao().getEntriesIncludingVoided("deal-room-deal-1")
        assertTrue("القيد الأصلي باقٍ", entries.any { it.id == "deal-advance-deal-1" })
        assertTrue("والعكس موجود", entries.count { it.status == EntryStatus.VOIDED } >= 2)
        val snapshot = com.baynana.data.local.ledger.LedgerRepository(db).snapshot("deal-room-deal-1")
        assertEquals("المسدد بعد العكس = صفر", 0L, snapshot.paidMinor)
        assertEquals("الدين رجع كما كان", 5_000_000L, snapshot.remainingMinor)
    }

    @Test
    fun `cancelling twice is refused`() = runBlocking {
        opened()
        repository.cancel("deal-1", "فسخ", "op-cancel-1", dealAt + day)
        val again = repository.cancel("deal-1", "فسخ مرة أخرى", "op-cancel-2", dealAt + 2 * day)
        assertTrue((again as DealRepository.CancelResult.Refused).reason.contains("مفسوخ سابقًا"))
    }

    @Test
    fun `only the creator edits the commission, and the change reverses the old statement`() = runBlocking {
        val outcome = opened()
        val oldEntryId = outcome.commissions.single().entryId!!

        val byOther = repository.updateCommission(
            "deal-1",
            CommissionEngine.Policy(payer = CommissionPayer.SPLIT, rateBasisPoints = 200),
            actorMemberId = "m-buyer"
        )
        assertTrue((byOther as DealRepository.EditResult.Refused).reason.contains("من غير منشئ الصلح"))

        val byCreator = repository.updateCommission(
            "deal-1",
            CommissionEngine.Policy(payer = CommissionPayer.SPLIT, rateBasisPoints = 200),
            actorMemberId = DealRepository.OWNER_MEMBER_ID
        )
        assertEquals(DealRepository.EditResult.Updated, byCreator)
        val deal = db.dealDao().getDeal("deal-1")!!
        assertEquals(100_000L, deal.commissionTotalMinor)
        assertEquals(50_000L, deal.commissionSellerMinor)
        assertEquals(50_000L, deal.commissionBuyerMinor)
        assertEquals("السعاية القديمة عُكست لا مُحيت", EntryStatus.VOIDED, db.ledgerDao().getEntry(oldEntryId)!!.status)
        assertEquals(
            CommissionStatus.REVERSED,
            db.dealDao().getCommissionByEntry(oldEntryId)!!.status
        )
    }

    @Test
    fun `after the first installment no commission edit is allowed`() = runBlocking {
        opened()
        repository.recordPayment("deal-1", 300_000L, dealAt + day, "op-pay-1")
        val result = repository.updateCommission(
            "deal-1",
            CommissionEngine.Policy(payer = CommissionPayer.BUYER, rateBasisPoints = 100),
            actorMemberId = DealRepository.OWNER_MEMBER_ID
        )
        assertTrue((result as DealRepository.EditResult.Refused).reason.contains("بعد أول دفعة"))
    }

    @Test
    fun `the statement text comes from the stored rows, with the same numbers`() = runBlocking {
        opened()
        val view = repository.view("deal-1")!!
        assertEquals(4_000_000L, view.remainingMinor)
        assertTrue(view.statement, view.statement.contains("بيان صلح: قمح • صنعاء"))
        assertTrue(view.statement, view.statement.contains("القيمة: 50,000 ر.ي"))
        assertTrue(view.statement, view.statement.contains("المتبقي: 40,000 ر.ي"))
        assertTrue(view.statement, view.statement.contains("المشتري عليه: 41,250 ر.ي"))
        assertEquals("بانتظار الإقرار • المتبقي: 40,000 ر.ي", view.statusLine)
    }

    @Test
    fun `an unregistered buyer is written by name and phone so the debt is traceable`() = runBlocking {
        val unregistered = draft().copy(
            buyer = DealEngine.Party(memberId = "", name = "ضيف من السوق", phone = "777000111")
        )
        val result = repository.open(unregistered, listing())
        val deal = (result as DealRepository.OpenResult.Opened).outcome.deal
        assertEquals("ضيف من السوق", deal.buyerName)
        assertEquals("off:777000111", deal.buyerMemberId)
        assertEquals(5_000_000L, db.ledgerDao().getEntry("deal-debt-deal-1")!!.amountMinor)
    }

    @Test
    fun `a commission statement is written per payer when the broker splits it`() = runBlocking {
        assertFalse(opened("deal-1").commissions.isEmpty())

        val split = repository.open(
            draft(
                id = "deal-3",
                listingId = null,
                commission = CommissionEngine.Policy(payer = CommissionPayer.SPLIT, rateBasisPoints = 400)
            ),
            listing = null
        )
        val splitOutcome = (split as DealRepository.OpenResult.Opened).outcome
        assertEquals(2, splitOutcome.commissions.size)
        assertEquals(200_000L, splitOutcome.commissions.sumOf { it.totalMinor })
        assertTrue(splitOutcome.commissions.all { it.status == CommissionStatus.OPEN })
        assertTrue(
            "بيان لكل مدفوع: البائع والمشتري",
            splitOutcome.commissions.map { it.payerMemberId }.containsAll(listOf("m-seller", "m-buyer"))
        )
    }

    @Test
    fun `an empty ledger room is not created when the deal is refused`() = runBlocking {
        val noCrop = draft().copy(cropTitle = " ")
        val result = repository.open(noCrop, listing())
        assertTrue(result is DealRepository.OpenResult.Refused)
        assertTrue(db.ledgerDao().getEntriesIncludingVoided("deal-room-deal-1").isEmpty())
        assertEquals(null, db.ledgerDao().getRoom("deal-room-deal-1"))
    }
}

package com.baynana.data.local.settlement

import androidx.room.withTransaction
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.NewDebtSpec
import com.baynana.domain.ledger.ReceiptSpec
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import com.baynana.domain.settlement.CommissionEngine
import com.baynana.domain.settlement.CommissionStatus
import com.baynana.domain.settlement.DealEngine
import com.baynana.domain.settlement.DealStatus
import com.baynana.domain.settlement.InstallmentScheduleEngine
import com.baynana.domain.settlement.InstallmentStatus
import java.util.UUID

/**
 * تنفيذ الصلح على القاعدة: **الصلح يُكتب في غرفته في الدفتر نفسه**، فلا يوجد محرّك حساب ثانٍ
 * (ح٥: مصدر واحد لكل رقم).
 *
 * - غرفة الصلح (`deal-room-<id>`, قالب السوق): دَين المحصول على المشتري للبائع، والعربون والأقساط
 *   سدادات تُخصَّص عليه بالأقدم (FIFO) — نفس مسار الدفتر، فلا رقم منفصل يُصان بيد.
 * - غرفة السعاية (`deal-fee-<id>`): بيان مستقل في حساب الدلال، لا يختلط بدَين المحصول.
 * - كل كتابة تمرّ من `LedgerRepository`، فتأخذ معاملة واحدة وسطر صندوق صادر واحدًا ومعرّف عملية
 *   ثابتًا (لا ازدواج عند إعادة الإرسال).
 * - الصلح لا يُمحى: الفسخ يقلب القيود بقيد عكسي معلن ويُلغي حالات الأقساط، والسجل يبقى للطرفين.
 */
class DealRepository(
    private val db: AppDatabase,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val now: () -> Long = { System.currentTimeMillis() }
) {
    private val dao get() = db.dealDao()
    private val ledger = LedgerRepository(db, newId, now)

    companion object {
        /** معرّف صاحب هذا الجهاز في كل غرفة (نفس الاصطلاح في الدفتر). */
        const val OWNER_MEMBER_ID = "me"
        const val OWNER_DISPLAY_NAME = "أنا"
    }

    // ------------------------------------------------------------------ الفتح

    data class OpenOutcome(
        val deal: DealRecord,
        val installments: List<DealInstallment>,
        val commissions: List<DealCommission>,
        val notes: List<String>
    )

    sealed interface OpenResult {
        data class Opened(val outcome: OpenOutcome) : OpenResult
        data class Refused(val reason: String) : OpenResult
    }

    /** يفتح صلحًا: يفحص أولًا (وهو موضع «لا بيع مزدوج»)، ثم يكتب كله في معاملة واحدة. */
    suspend fun open(draft: DealEngine.Draft, listing: DealEngine.ListingSnapshot?): OpenResult =
        db.withTransaction {
            val blockers = dao.getBlockingDeals().map { it.toSummary() }
            val check = DealEngine.check(draft, listing, blockers)
            if (check is DealEngine.Check.Refused) {
                return@withTransaction OpenResult.Refused(check.reason)
            }
            val plan = (check as DealEngine.Check.Allowed).plan

            val sellerId = memberIdOf(draft.seller)
            val buyerId = memberIdOf(draft.buyer)
            // الدلال: إن لم يُحدَّد فالتطبيق صاحبه هو من يمسك البيان، فلا نخترع طرفًا ثالثًا.
            val brokerId = draft.broker?.let { memberIdOf(it) }?.ifBlank { OWNER_MEMBER_ID } ?: ""

            val roomId = roomIdOf(draft.id)
            writeDealRoom(roomId, draft, plan, sellerId, buyerId, brokerId)

            val deal = plan.toRecord(roomId, sellerId, buyerId, brokerId)
            dao.upsertDeal(deal)

            val installments = plan.schedule.map { it.toEntity(draft.id) }
            if (installments.isNotEmpty()) dao.upsertInstallments(installments)

            // 1) دَين المحصول: المشتري مدين للبائع بالقيمة كاملة (والعربون سداد يُخصَّص عليها).
            ledger.recordDebt(
                NewDebtSpec(
                    id = "deal-debt-${draft.id}",
                    operationId = "deal:debt:${draft.id}",
                    roomId = roomId,
                    type = EntryType.SETTLEMENT,
                    debtorMemberId = buyerId,
                    creditorMemberId = sellerId,
                    amountMinor = draft.totalMinor,
                    currency = draft.currency,
                    occurredAt = draft.dealAt,
                    description = "صلح ${draft.cropTitle.trim()}".trim(),
                    quantityNote = draft.place.trim(),
                    createdByMemberId = OWNER_MEMBER_ID,
                    listingId = draft.listingId
                )
            )

            // 2) العربون: سداد يُخصَّص على دَين المحصول بالأقدم (لا يبقى معلقًا بلا سبب).
            if (draft.advanceMinor > 0L) {
                ledger.recordReceipt(
                    ReceiptSpec(
                        id = "deal-advance-${draft.id}",
                        operationId = "deal:advance:${draft.id}",
                        roomId = roomId,
                        debtorMemberId = buyerId,
                        creditorMemberId = sellerId,
                        amountMinor = draft.advanceMinor,
                        currency = draft.currency,
                        occurredAt = draft.dealAt,
                        mode = AllocationMode.OldestFirst,
                        description = "عربون صلح ${draft.cropTitle.trim()}".trim(),
                        createdByMemberId = OWNER_MEMBER_ID
                    )
                )
            }

            // 3) السعاية: بيان مستقل في غرفة مستقلة (لا تختلط بدَين المحصول).
            val commissions = writeCommissions(plan, sellerId, buyerId, brokerId)

            OpenResult.Opened(OpenOutcome(deal, installments, commissions, plan.notes))
        }

    // ------------------------------------------------------------------ القبض

    data class PaymentOutcome(
        val entryId: String,
        val created: Boolean,
        val appliedMinor: Long,
        val excessMinor: Long,
        val status: String,
        val allocations: List<DealEngine.Allocation>
    )

    sealed interface PaymentResult {
        data class Recorded(val outcome: PaymentOutcome) : PaymentResult

        /** العملية وصلت سابقًا: لا خصم ثانٍ، ولا خطأ على الشاشة. */
        data class Replayed(val entryId: String, val unappliedMinor: Long) : PaymentResult
        data class Refused(val reason: String) : PaymentResult
    }

    /**
     * يسجّل قسطًا (أو أي قبض على الصلح): يخطط على جدول الأقساط بالمحرّك النقي، ثم يكتب السداد في
     * الدفتر بتخصيص الأقدم فالأقدم، ويحدّث حالات الأقساط في **نفس المعاملة**. فإن كانت العملية
     * سبقت (نفس `operationId`) لا يُلمس الجدول ولا يتضاعف الخصم.
     */
    suspend fun recordPayment(
        dealId: String,
        amountMinor: Long,
        paidAt: Long,
        operationId: String,
        actorMemberId: String = OWNER_MEMBER_ID
    ): PaymentResult = db.withTransaction {
        val deal = dao.getDeal(dealId)
            ?: return@withTransaction PaymentResult.Refused("الصلح غير موجود")
        val installments = dao.getInstallments(dealId)
        val request = DealEngine.PaymentRequest(
            operationId = operationId,
            amountMinor = amountMinor,
            paidAt = paidAt,
            paidByMemberId = actorMemberId
        )
        val check = DealEngine.planPayment(deal.toState(installments), request)
        if (check is DealEngine.PaymentCheck.Refused) {
            return@withTransaction PaymentResult.Refused(check.reason)
        }
        val plan = (check as DealEngine.PaymentCheck.Allowed).plan

        val receipt = ledger.recordReceipt(
            ReceiptSpec(
                id = newId(),
                operationId = operationId,
                roomId = deal.roomId,
                debtorMemberId = deal.buyerMemberId,
                creditorMemberId = deal.sellerMemberId,
                amountMinor = amountMinor,
                currency = deal.currency,
                occurredAt = paidAt,
                mode = AllocationMode.OldestFirst,
                description = "قسط صلح ${deal.cropTitle.trim()}".trim(),
                createdByMemberId = actorMemberId
            )
        )
        if (!receipt.created) {
            return@withTransaction PaymentResult.Replayed(receipt.entry.id, receipt.unappliedMinor)
        }

        // مطابقة صريحة: ما خصصه الدفتر لا بد أن يطابق ما خطّطه جدول الأقساط، وإلا توقف كل شيء.
        val ledgerApplied = receipt.plan.appliedMinor
        val scheduleApplied = plan.allocations.sumOf { it.amountMinor }
        check(ledgerApplied == scheduleApplied) {
            "تخصيص الدفتر ($ledgerApplied) لا يطابق جدول الأقساط ($scheduleApplied) — لا تُكتب أرقام متناقضة"
        }

        plan.newInstallments.forEach { updated ->
            dao.updateInstallment(updated.id, updated.paidMinor, updated.status, paidAt)
        }
        if (plan.resultingStatus != deal.status) {
            dao.setStatus(
                id = dealId,
                status = plan.resultingStatus,
                updatedAt = paidAt,
                closedAt = if (plan.resultingStatus == DealStatus.COMPLETED) paidAt else null
            )
        }

        PaymentResult.Recorded(
            PaymentOutcome(
                entryId = receipt.entry.id,
                created = true,
                appliedMinor = ledgerApplied,
                excessMinor = receipt.unappliedMinor,
                status = plan.resultingStatus,
                allocations = plan.allocations
            )
        )
    }

    // ------------------------------------------------------------------ الفسخ

    data class CancellationOutcome(
        val status: String,
        val reversedEntries: Int,
        val cancelledInstallments: Int,
        val amountsReversed: Boolean,
        val notes: List<String>
    )

    sealed interface CancelResult {
        data class Cancelled(val outcome: CancellationOutcome) : CancelResult
        data class Refused(val reason: String) : CancelResult
    }

    /**
     * فسخ الصلح: كل ما قُبض يُعكس بقيد عكسي معلن، والأقساط المفتوحة تُلغى حالاتها، والسعاية تُعكس
     * في غرفتها. لا صف يُحذف، ولا فسخ مرتين.
     */
    suspend fun cancel(
        dealId: String,
        reason: String,
        operationId: String,
        at: Long = now()
    ): CancelResult = db.withTransaction {
        val deal = dao.getDeal(dealId)
            ?: return@withTransaction CancelResult.Refused("الصلح غير موجود")
        val installments = dao.getInstallments(dealId)
        val check = DealEngine.planCancellation(deal.toState(installments))
        if (check is DealEngine.CancellationCheck.Refused) {
            return@withTransaction CancelResult.Refused(check.reason)
        }
        val plan = (check as DealEngine.CancellationCheck.Allowed).plan

        val reasonText = reason.ifBlank { "فسخ صلح" }
        var reversed = 0
        db.ledgerDao().getEntriesIncludingVoided(deal.roomId)
            .filter { it.type == EntryType.PAYMENT && it.status != EntryStatus.VOIDED }
            .forEach { entry ->
                val result = ledger.reverseEntry(entry.id, reasonText, "$operationId:${entry.id}", at)
                if (result.created) reversed++
            }
        dao.getCommissions(dealId).mapNotNull { it.entryId }.forEach { entryId ->
            val result = ledger.reverseEntry(entryId, reasonText, "$operationId:$entryId", at)
            if (result.created) reversed++
        }

        val openIds = plan.cancelledInstallments.toSet()
        installments.filter { it.id in openIds }.forEach { installment ->
            dao.updateInstallment(installment.id, installment.paidMinor, InstallmentStatus.CANCELLED, at)
        }
        // السعاية تُعكس مع الصلح دائمًا: بيان مستقل لا يبقى معلّقًا على صلح مفسوخ.
        dao.setCommissionStatus(dealId, CommissionStatus.REVERSED, at)
        dao.setStatus(dealId, DealStatus.CANCELLED, at, at)

        CancelResult.Cancelled(
            CancellationOutcome(
                status = DealStatus.CANCELLED,
                reversedEntries = reversed,
                cancelledInstallments = plan.cancelledInstallments.size,
                amountsReversed = plan.amountsToReverseMinor > 0L,
                notes = plan.notes
            )
        )
    }

    // ------------------------------------------------------------------ التعديل

    sealed interface EditResult {
        data object Updated : EditResult
        data class Refused(val reason: String) : EditResult
    }

    /**
     * تعديل السعاية: مسموح لمنشئ الصلح فقط، وقبل إقرار الطرف الآخر وقبل أول قسط. عند التعديل
     * تُعكس قيود السعاية القديمة ويُكتب البيان الجديد، فلا يبقى في حساب الدلال رقمان لسعاية واحدة.
     */
    suspend fun updateCommission(
        dealId: String,
        policy: CommissionEngine.Policy,
        actorMemberId: String
    ): EditResult = db.withTransaction {
        val deal = dao.getDeal(dealId)
            ?: return@withTransaction EditResult.Refused("الصلح غير موجود")
        val draft = deal.toDraft(policy)
        val acknowledged = db.ledgerDao().getEntriesIncludingVoided(deal.roomId)
            .any { it.status == EntryStatus.ACKNOWLEDGED }
        val anyInstallmentPaid = dao.getInstallments(dealId).any { it.paidMinor > 0L }
        val guard = DealEngine.checkEdit(draft, actorMemberId, acknowledged, anyInstallmentPaid)
        if (guard is DealEngine.EditCheck.Refused) {
            return@withTransaction EditResult.Refused(guard.reason)
        }

        val computed = CommissionEngine.compute(deal.totalMinor, deal.currency, policy)
        if (computed is CommissionEngine.Check.Refused) {
            return@withTransaction EditResult.Refused(computed.reason)
        }
        val breakdown = (computed as CommissionEngine.Check.Ok).breakdown

        val at = now()
        dao.getCommissions(dealId).mapNotNull { it.entryId }.forEach { entryId ->
            ledger.reverseEntry(entryId, "تعديل سعاية الصلح", "deal:fee-edit:$dealId:$entryId", at)
        }
        dao.setCommissionStatus(dealId, CommissionStatus.REVERSED, at)
        writeCommissions(
            plan = deal.toPlanFromRecord(policy, breakdown),
            sellerId = deal.sellerMemberId,
            buyerId = deal.buyerMemberId,
            brokerId = deal.brokerMemberId,
            at = at,
            // كل تعديل يكتب بيانًا جديدًا بمعرّفات جديدة، فيبقى البيان القديم معكوسًا لا مكتومًا.
            attempt = dao.getCommissions(dealId).size
        )
        dao.updateCommission(
            id = dealId,
            payer = policy.payer,
            rateBasisPoints = policy.rateBasisPoints,
            totalMinor = breakdown.totalMinor,
            sellerMinor = breakdown.sellerMinor,
            buyerMinor = breakdown.buyerMinor,
            updatedAt = at
        )
        EditResult.Updated
    }

    // ------------------------------------------------------------------ القراءة

    data class DealView(
        val deal: DealRecord,
        val installments: List<DealInstallment>,
        val commissions: List<DealCommission>,
        val remainingMinor: Long,
        val statement: String,
        val statusLine: String
    )

    suspend fun view(dealId: String): DealView? {
        val deal = dao.getDeal(dealId) ?: return null
        val installments = dao.getInstallments(dealId)
        val plan = deal.toPlan(installments)
        return DealView(
            deal = deal,
            installments = installments,
            commissions = dao.getCommissions(dealId),
            remainingMinor = plan.remainingMinor,
            statement = DealEngine.statementText(plan),
            statusLine = DealEngine.statusLine(deal.status, plan.remainingMinor, deal.currency)
        )
    }

    /** بيان الصلح نصًّا واحدًا (يُستعمل في الشاشة وPDF وواتساب). */
    suspend fun statementText(dealId: String): String? = view(dealId)?.statement

    // ------------------------------------------------------------------ الداخليات

    private fun memberIdOf(party: DealEngine.Party): String =
        party.memberId.ifBlank { "off:${party.phone.ifBlank { party.name }.trim()}" }

    private fun roomIdOf(dealId: String): String = "deal-room-$dealId"

    private fun feeRoomIdOf(dealId: String): String = "deal-fee-$dealId"

    private suspend fun writeDealRoom(
        roomId: String,
        draft: DealEngine.Draft,
        plan: DealEngine.Plan,
        sellerId: String,
        buyerId: String,
        brokerId: String
    ) {
        val at = now()
        db.ledgerDao().upsertRoom(
            LedgerRoom(
                id = roomId,
                kind = RoomKind.MARKET,
                currency = draft.currency,
                title = "صلح ${draft.cropTitle.trim()}".trim(),
                // الغرفة لا تُفتح للطرف الآخر إلا بعد قبوله (ح٣): PENDING حتى الإقرار.
                status = RoomStatus.PENDING,
                linkCode = "deal-$roomId",
                counterpartName = draft.buyer.name.trim(),
                counterpartPhone = draft.buyer.phone.trim(),
                createdAt = at,
                updatedAt = at
            )
        )
        val members = buildList {
            add(RoomMember(roomId, OWNER_MEMBER_ID, OWNER_DISPLAY_NAME, isMe = true, role = "owner", joinedAt = at))
            add(RoomMember(roomId, sellerId, draft.seller.name.trim(), draft.seller.phone.trim(), role = "seller", joinedAt = at))
            add(RoomMember(roomId, buyerId, draft.buyer.name.trim(), draft.buyer.phone.trim(), role = "buyer", joinedAt = at))
            if (brokerId.isNotBlank() && brokerId != OWNER_MEMBER_ID && brokerId != sellerId && brokerId != buyerId) {
                add(
                    RoomMember(
                        roomId, brokerId, draft.broker?.name?.trim().orEmpty(), draft.broker?.phone?.trim().orEmpty(),
                        role = "broker", joinedAt = at
                    )
                )
            }
        }
        members.forEach { db.ledgerDao().upsertMember(it) }
        // بيان الصلح نفسه محفوظ في ملاحظات الغرفة المرجعية (وصف مختصر لمن يفتح الغرفة لاحقًا).
        check(plan.remainingMinor >= 0L) { "متبقٍ سالب في خطة الصلح" }
    }

    private suspend fun writeCommissions(
        plan: DealEngine.Plan,
        sellerId: String,
        buyerId: String,
        brokerId: String,
        at: Long = now(),
        /** 0 لأول بيان، ويزيد مع كل تعديل: معرّفات جديدة فلا تصطدم ببيان عُكس. */
        attempt: Int = 0
    ): List<DealCommission> {
        if (plan.commission.totalMinor <= 0L || brokerId.isBlank()) return emptyList()
        val dealId = plan.draft.id
        val feeRoomId = feeRoomIdOf(dealId)
        db.ledgerDao().upsertRoom(
            LedgerRoom(
                id = feeRoomId,
                kind = RoomKind.MARKET,
                currency = plan.draft.currency,
                title = "سعاية صلح ${plan.draft.cropTitle.trim()}".trim(),
                status = RoomStatus.PENDING,
                linkCode = "fee-$dealId",
                counterpartName = plan.draft.broker?.name?.trim().orEmpty(),
                createdAt = at,
                updatedAt = at
            )
        )
        db.ledgerDao().upsertMember(
            RoomMember(feeRoomId, OWNER_MEMBER_ID, OWNER_DISPLAY_NAME, isMe = true, role = "owner", joinedAt = at)
        )
        db.ledgerDao().upsertMember(
            RoomMember(feeRoomId, brokerId, plan.draft.broker?.name?.trim().orEmpty(), role = "broker", joinedAt = at)
        )

        val shares = buildList {
            if (plan.commission.sellerMinor > 0L) add(sellerId to plan.commission.sellerMinor)
            if (plan.commission.buyerMinor > 0L) add(buyerId to plan.commission.buyerMinor)
        }
        val rows = mutableListOf<DealCommission>()
        shares.forEachIndexed { index, (payerId, share) ->
            val payerName = if (payerId == sellerId) plan.draft.seller.name.trim() else plan.draft.buyer.name.trim()
            db.ledgerDao().upsertMember(
                RoomMember(feeRoomId, payerId, payerName, role = "payer", joinedAt = at)
            )
            val suffix = if (attempt == 0) "" else "-$attempt"
            val entryId = "deal-fee-$dealId$suffix-$index"
            ledger.recordDebt(
                NewDebtSpec(
                    id = entryId,
                    operationId = "deal:fee:$dealId$suffix:$index",
                    roomId = feeRoomId,
                    type = EntryType.SETTLEMENT,
                    debtorMemberId = payerId,
                    creditorMemberId = brokerId,
                    amountMinor = share,
                    currency = plan.draft.currency,
                    occurredAt = plan.draft.dealAt,
                    description = "سعاية صلح ${plan.draft.cropTitle.trim()}".trim(),
                    createdByMemberId = OWNER_MEMBER_ID
                )
            )
            rows += DealCommission(
                id = "fee-$dealId$suffix-$index",
                dealId = dealId,
                roomId = feeRoomId,
                memberId = brokerId,
                payerMemberId = payerId,
                currency = plan.draft.currency,
                totalMinor = share,
                entryId = entryId,
                status = CommissionStatus.OPEN,
                createdAt = at,
                updatedAt = at
            )
        }
        rows.forEach { dao.upsertCommission(it) }
        return rows
    }

    private fun DealEngine.Plan.toRecord(
        roomId: String,
        sellerId: String,
        buyerId: String,
        brokerId: String
    ): DealRecord {
        val at = now()
        return DealRecord(
            id = draft.id,
            roomId = roomId,
            listingId = draft.listingId,
            cropTitle = draft.cropTitle.trim(),
            place = draft.place.trim(),
            currency = draft.currency,
            totalMinor = draft.totalMinor,
            advanceMinor = draft.advanceMinor,
            sellerMemberId = sellerId,
            sellerName = draft.seller.name.trim(),
            buyerMemberId = buyerId,
            buyerName = draft.buyer.name.trim(),
            brokerMemberId = brokerId,
            brokerName = draft.broker?.name?.trim().orEmpty(),
            commissionTotalMinor = commission.totalMinor,
            commissionSellerMinor = commission.sellerMinor,
            commissionBuyerMinor = commission.buyerMinor,
            commissionPayer = draft.commission.payer,
            commissionRateBasisPoints = draft.commission.rateBasisPoints,
            status = DealStatus.PENDING,
            dealAt = draft.dealAt,
            firstDueAt = schedule.firstOrNull()?.dueAt,
            intervalDays = draft.intervalDays,
            terms = draft.terms.trim(),
            createdByMemberId = draft.createdByMemberId,
            createdAt = at,
            updatedAt = at
        )
    }

    private fun InstallmentScheduleEngine.Installment.toEntity(dealId: String): DealInstallment = DealInstallment(
        id = "ins-$dealId-$index",
        dealId = dealId,
        seq = index,
        dueAt = dueAt,
        amountMinor = amountMinor,
        paidMinor = 0L,
        status = InstallmentStatus.SCHEDULED,
        createdAt = now(),
        updatedAt = now()
    )

    private fun DealInstallment.toEngine(): DealEngine.InstallmentState = DealEngine.InstallmentState(
        id = id,
        index = seq,
        dueAt = dueAt,
        amountMinor = amountMinor,
        paidMinor = paidMinor,
        status = status
    )

    private fun DealRecord.toBreakdown() = CommissionEngine.Breakdown(
        totalMinor = commissionTotalMinor,
        sellerMinor = commissionSellerMinor,
        buyerMinor = commissionBuyerMinor,
        rateBasisPoints = commissionRateBasisPoints,
        cappedByMinor = 0L
    )

    private fun DealRecord.toState(installments: List<DealInstallment>): DealEngine.State = DealEngine.State(
        dealId = id,
        status = status,
        currency = currency,
        installments = installments.map { it.toEngine() },
        commission = toBreakdown(),
        advanceMinor = advanceMinor
    )

    private fun DealRecord.toDraft(policy: CommissionEngine.Policy): DealEngine.Draft = DealEngine.Draft(
        id = id,
        listingId = listingId,
        cropTitle = cropTitle,
        place = place,
        currency = currency,
        totalMinor = totalMinor,
        advanceMinor = advanceMinor,
        seller = DealEngine.Party(sellerMemberId, sellerName),
        buyer = DealEngine.Party(buyerMemberId, buyerName),
        broker = if (brokerMemberId.isBlank()) null else DealEngine.Party(brokerMemberId, brokerName),
        dealAt = dealAt,
        firstDueAt = firstDueAt,
        installmentCount = 1,
        intervalDays = intervalDays,
        commission = policy,
        terms = terms,
        createdByMemberId = createdByMemberId
    )

    private fun DealRecord.toPlanFromRecord(
        policy: CommissionEngine.Policy,
        breakdown: CommissionEngine.Breakdown
    ): DealEngine.Plan {
        val remaining = totalMinor - advanceMinor
        return DealEngine.Plan(
            draft = toDraft(policy),
            schedule = emptyList(),
            commission = breakdown,
            remainingMinor = remaining,
            buyerOwesMinor = remaining + breakdown.buyerMinor,
            sellerReceivesMinor = remaining - breakdown.sellerMinor,
            notes = emptyList()
        )
    }

    /** يعيد بناء خطة الصلح من الصفوف المكتوبة: مصدر البيان الواحد للشاشة وPDF وواتساب. */
    private fun DealRecord.toPlan(installments: List<DealInstallment>): DealEngine.Plan {
        val remaining = totalMinor - advanceMinor
        val breakdown = toBreakdown()
        return DealEngine.Plan(
            draft = toDraft(
                CommissionEngine.Policy(
                    payer = commissionPayer,
                    rateBasisPoints = commissionRateBasisPoints,
                    sellerShareBasisPoints = if (commissionTotalMinor > 0L) {
                        ((commissionSellerMinor * 10_000L) / commissionTotalMinor).toInt()
                    } else {
                        5_000
                    }
                )
            ),
            schedule = installments.map {
                InstallmentScheduleEngine.Installment(it.seq, it.dueAt, it.amountMinor)
            },
            commission = breakdown,
            remainingMinor = remaining,
            buyerOwesMinor = remaining + breakdown.buyerMinor,
            sellerReceivesMinor = remaining - breakdown.sellerMinor,
            notes = emptyList()
        )
    }

    private fun DealRecord.toSummary() = DealEngine.DealSummary(
        id = id,
        listingId = listingId,
        status = status,
        sellerMemberId = sellerMemberId,
        buyerMemberId = buyerMemberId
    )
}

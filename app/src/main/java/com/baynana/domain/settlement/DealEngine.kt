package com.baynana.domain.settlement

import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyFormat

/**
 * محرّك الصلح (ح٨) — نقيّ بلا قاعدة ولا شبكة: يفحص الصلح، ويوزّع القبض، ويخطّط الفسخ، ويصوغ
 * البيان. لا يكتب شيئًا ولا يعرف من «الآن»: كل تاريخ يأتي من المستخدم، وكل رقم بالوحدة الصغرى.
 *
 * القواعد المنفَّذة (من الخطة §5.3 و§5.4):
 * 1. لا يكون الشخص بائعًا ومشتريًا في الصلح نفسه (ولا يجمع دورين).
 * 2. أول صلح (قائم أو مكتمل) يحجز العرض، ومحاولة صلح ثانٍ على العرض نفسه تُرفض بوضوح — لا بيع مزدوج.
 * 3. السعاية بيان مستقل في حساب الدلال، لا تختلط بدفتر الري ولا بدَين المحصول.
 * 4. القبض يوزَّع على الأقساط بالترتيب (الأقدم استحقاقًا أولًا)، والزائد رصيد دائن في الغرفة نفسها.
 * 5. لا تعديل بعد إقرار الطرف الآخر ولا بعد أول دفعة، ولا من غير منشئ الصلح.
 * 6. الفسخ لا يمحو: قيد عكسي معلن، وحالات تُلغى لا صفوف.
 */
object DealEngine {

    /** طرف في الصلح: رقم عضويته في الغرفة (أو مرجع محلي)، واسمه، وهاتفه (للعرض فقط). */
    data class Party(val memberId: String, val name: String, val phone: String = "")

    /** مسودة الصلح كما يدخلها الدلال قبل الفحص. */
    data class Draft(
        val id: String,
        val listingId: String? = null,
        val cropTitle: String,
        val place: String = "",
        val currency: String,
        val totalMinor: Long,
        val advanceMinor: Long = 0L,
        val seller: Party,
        val buyer: Party,
        val broker: Party? = null,
        val dealAt: Long,
        val firstDueAt: Long? = null,
        val installmentCount: Int = 1,
        val intervalDays: Int = 30,
        val commission: CommissionEngine.Policy = CommissionEngine.Policy(),
        val terms: String = "",
        val createdByMemberId: String = ""
    )

    /** العرض كما هو في السوق: الصلح لا يقرر حالة العرض بل يطلب حجزه. */
    data class ListingSnapshot(
        val id: String,
        val state: String = ListingState.OPEN,
        val reservedByDealId: String? = null
    )

    /** صلح قائم على نفس العرض، للحكم في الازدواج بلا قراءة كل الصلوح. */
    data class DealSummary(
        val id: String,
        val listingId: String?,
        val status: String,
        val sellerMemberId: String = "",
        val buyerMemberId: String = ""
    )

    data class Plan(
        val draft: Draft,
        val schedule: List<InstallmentScheduleEngine.Installment>,
        val commission: CommissionEngine.Breakdown,
        val remainingMinor: Long,
        /** ما على المشتري بعد العربون، مضافًا إليه نصيبه من السعاية. */
        val buyerOwesMinor: Long,
        /** ما يستلمه البائع بعد خصم نصيبه من السعاية. */
        val sellerReceivesMinor: Long,
        val notes: List<String>
    ) {
        init {
            check(draft.totalMinor == draft.advanceMinor + remainingMinor) {
                "الصلح غير متوازن: القيمة ≠ العربون + المتبقي"
            }
            check(buyerOwesMinor == remainingMinor + commission.buyerMinor) {
                "ما على المشتري لا يطابق المتبقي + نصيبه من السعاية"
            }
            check(sellerReceivesMinor == remainingMinor - commission.sellerMinor) {
                "ما يستلمه البائع لا يطابق المتبقي − نصيبه من السعاية"
            }
            check(sellerReceivesMinor >= 0L) { "السعاية تلتهم كامل المتبقي — راجع النسبة" }
        }
    }

    sealed interface Check {
        data class Allowed(val plan: Plan) : Check
        data class Refused(val reason: String) : Check
    }

    private fun partyKey(party: Party): String = when {
        party.memberId.isNotBlank() -> "m:${party.memberId}"
        party.phone.isNotBlank() -> "p:${party.phone}"
        else -> "n:${party.name.trim()}"
    }

    /** يفحص الصلح كاملًا قبل أي كتابة، ويُرجع الخطة أو سبب الرفض بالعربية. */
    fun check(draft: Draft, listing: ListingSnapshot?, existingDeals: List<DealSummary> = emptyList()): Check {
        if (draft.id.isBlank()) return Check.Refused("الصلح بلا معرّف")
        if (draft.cropTitle.isBlank()) return Check.Refused("اكتب المحصول: لا صلح بلا بيان")
        if (draft.seller.name.isBlank() || draft.buyer.name.isBlank()) {
            return Check.Refused("اسم البائع والمشتري مطلوبان")
        }
        if (draft.currency.isBlank()) return Check.Refused("حدّد العملة")

        // 1) لا يجمع أحد دورين في الصلح نفسه: الحسابان يختلطان والدَين يضيع بينهما.
        val keys = listOfNotNull(draft.seller, draft.buyer, draft.broker).map(::partyKey)
        if (keys.size != keys.toSet().size) {
            return Check.Refused("لا يكون الشخص بائعًا ومشتريًا (أو دلالًا) في الصلح نفسه")
        }

        if (draft.totalMinor <= 0L) return Check.Refused("قيمة الصلح يجب أن تكون أكبر من صفر")
        if (draft.advanceMinor < 0L) return Check.Refused("العربون لا يكون سالبًا")
        if (draft.advanceMinor > draft.totalMinor) return Check.Refused("العربون أكبر من قيمة الصلح")

        val remaining = draft.totalMinor - draft.advanceMinor
        if (remaining > 0L) {
            val firstDueAt = draft.firstDueAt
                ?: return Check.Refused("حدّد موعد أول قسط للمتبقي")
            if (firstDueAt < draft.dealAt) {
                return Check.Refused("موعد القسط قبل تاريخ الصلح — لا نُدخل تاريخًا غير واقعي")
            }
        }

        val schedule = when (
            val result = InstallmentScheduleEngine.build(
                totalMinor = draft.totalMinor,
                advanceMinor = draft.advanceMinor,
                count = draft.installmentCount,
                firstDueAt = draft.firstDueAt ?: 0L,
                intervalDays = draft.intervalDays,
                currencyCode = draft.currency
            )
        ) {
            is InstallmentScheduleEngine.Check.Refused -> return Check.Refused(result.reason)
            is InstallmentScheduleEngine.Check.Ok -> result.installments
        }

        val commission = when (val result = CommissionEngine.compute(draft.totalMinor, draft.currency, draft.commission)) {
            is CommissionEngine.Check.Refused -> return Check.Refused(result.reason)
            is CommissionEngine.Check.Ok -> result.breakdown
        }

        // 2) حجز العرض: صلح قائم أو مكتمل على العرض نفسه يمنع صلحًا آخر — لا بيع مزدوج.
        val listingId = draft.listingId
        if (listingId != null) {
            val snapshot = listing ?: return Check.Refused("العرض غير موجود في السوق")
            if (snapshot.id != listingId) return Check.Refused("العرض المحدد لا يطابق الصلح")
            when (snapshot.state) {
                ListingState.SOLD -> return Check.Refused("تم بيع هذا المحصول في صلح آخر — لا بيع مزدوج")
                ListingState.WITHDRAWN -> return Check.Refused("العرض مسحوب من السوق")
                ListingState.RESERVED -> {
                    val holder = snapshot.reservedByDealId
                    if (holder != null && holder != draft.id) {
                        return Check.Refused("المحصول محجوز لصلح آخر قائم — لا يباع مرتين")
                    }
                }
            }
            val competitor = existingDeals.firstOrNull { other ->
                other.id != draft.id &&
                    other.listingId == listingId &&
                    other.status in listOf(DealStatus.PENDING, DealStatus.ACTIVE, DealStatus.COMPLETED)
            }
            if (competitor != null) {
                return Check.Refused(
                    "المحصول في صلح ${DealStatus.label(competitor.status)} بالفعل — لا بيع مزدوج"
                )
            }
        }

        val notes = mutableListOf<String>()
        notes += "السعاية بيان مستقل في حساب الدلال، ولا تدخل في دَين المحصول."
        if (draft.listingId != null) notes += "العرض يُحجز بهذا الصلح ما دام قائمًا، ويُعلَم «تم البيع» عند الإكمال."
        if (keys.size < 3) notes += "بلا دلال في هذا الصلح: لا سعاية."
        if (draft.createdByMemberId.isBlank()) notes += "الصلح بلا منشئ محدَّد: لا يُسمح بتعديله بعد الإرسال."
        if (commission.cappedByMinor > 0L) {
            notes += "السعاية بلغت السقف المتفق عليه: ${commission.cappedByMinor} فلسًا."
        }

        return Check.Allowed(
            Plan(
                draft = draft,
                schedule = schedule,
                commission = commission,
                remainingMinor = remaining,
                buyerOwesMinor = remaining + commission.buyerMinor,
                sellerReceivesMinor = remaining - commission.sellerMinor,
                notes = notes
            )
        )
    }

    // ------------------------------------------------------------- القبض

    data class InstallmentState(
        val id: String,
        val index: Int,
        val dueAt: Long,
        val amountMinor: Long,
        val paidMinor: Long = 0L,
        val status: String = InstallmentStatus.SCHEDULED
    ) {
        val openMinor: Long get() = (amountMinor - paidMinor).coerceAtLeast(0L)
    }

    data class State(
        val dealId: String,
        val status: String,
        val currency: String,
        val installments: List<InstallmentState>,
        val commission: CommissionEngine.Breakdown,
        val advanceMinor: Long = 0L
    )

    data class PaymentRequest(
        val operationId: String,
        val amountMinor: Long,
        val paidAt: Long,
        val paidByMemberId: String = "",
        /** هل هذه العملية وصلتنا سابقًا؟ (شبكة أعادت الإرسال) */
        val seenBefore: Boolean = false
    )

    data class Allocation(val installmentId: String, val index: Int, val amountMinor: Long)

    data class PaymentPlan(
        val allocations: List<Allocation>,
        val excessMinor: Long,
        val newInstallments: List<InstallmentState>,
        val resultingStatus: String,
        val notes: List<String>
    )

    sealed interface PaymentCheck {
        data class Allowed(val plan: PaymentPlan) : PaymentCheck
        data class Refused(val reason: String) : PaymentCheck
    }

    /**
     * يوزّع قبضًا على الأقساط المفتوحة بالأقدم استحقاقًا، والزائد يصير رصيدًا دائنًا في الغرفة نفسها.
     * إعادة تشغيل العملية نفسها مرفوضة هنا نصًّا (وفوقها حاجز `operationId` في القاعدة).
     */
    fun planPayment(state: State, request: PaymentRequest): PaymentCheck {
        if (request.seenBefore) {
            return PaymentCheck.Refused("هذه العملية سبقت: إعادة التشغيل لا تُخصم مرتين")
        }
        if (state.status == DealStatus.CANCELLED) {
            return PaymentCheck.Refused("الصلح مفسوخ: لا يُقبض عليه، وأرجع المبلغ لصاحبه")
        }
        if (request.amountMinor <= 0L) return PaymentCheck.Refused("مبلغ القبض يجب أن يكون أكبر من صفر")
        if (request.amountMinor > Money.MAX_MINOR) return PaymentCheck.Refused("المبلغ أكبر من الحد المسموح")
        if (state.currency.isBlank()) return PaymentCheck.Refused("الصلح بلا عملة")

        val notes = mutableListOf<String>()

        if (state.status == DealStatus.COMPLETED) {
            notes += "الصلح مكتمل: المبلغ كله رصيد دائن في غرفة الصلح نفسها، ولا يُنقل لطرف آخر."
            return PaymentCheck.Allowed(
                PaymentPlan(
                    allocations = emptyList(),
                    excessMinor = request.amountMinor,
                    newInstallments = state.installments,
                    resultingStatus = DealStatus.COMPLETED,
                    notes = notes
                )
            )
        }

        var left = request.amountMinor
        val allocations = mutableListOf<Allocation>()
        val updated = state.installments
            .sortedWith(compareBy({ it.dueAt }, { it.index }))
            .map { installment ->
                if (left <= 0L || !InstallmentStatus.isOpen(installment.status) || installment.openMinor <= 0L) {
                    return@map installment
                }
                val take = minOf(left, installment.openMinor)
                left -= take
                allocations += Allocation(installment.id, installment.index, take)
                val paid = installment.paidMinor + take
                installment.copy(
                    paidMinor = paid,
                    status = if (paid >= installment.amountMinor) InstallmentStatus.PAID else InstallmentStatus.PARTIAL
                )
            }

        val excess = left
        if (excess > 0L) {
            notes += "الزائد ($excess فلسًا) رصيد دائن في غرفة الصلح نفسها، لا عند طرف ثالث."
        }
        val allPaid = updated.isNotEmpty() && updated.all { it.status == InstallmentStatus.PAID }
        val resultingStatus = when {
            updated.isEmpty() -> DealStatus.COMPLETED
            allPaid -> DealStatus.COMPLETED
            else -> DealStatus.ACTIVE
        }
        if (resultingStatus == DealStatus.COMPLETED) {
            notes += "سُدّدت الأقساط كلها: الصلح مكتمل، ويُعلَم العرض «تم البيع»."
        }
        if (state.advanceMinor > 0L) {
            notes += "العربون (${state.advanceMinor} فلسًا) محسوب سابقًا ولم يُعَد خصمه."
        }

        return PaymentCheck.Allowed(
            PaymentPlan(
                allocations = allocations,
                excessMinor = excess,
                newInstallments = updated,
                resultingStatus = resultingStatus,
                notes = notes
            )
        )
    }

    // ------------------------------------------------------------- الفسخ

    data class CancellationPlan(
        val status: String,
        val paidTotalMinor: Long,
        val amountsToReverseMinor: Long,
        val cancelledInstallments: List<String>,
        val notes: List<String>
    )

    sealed interface CancellationCheck {
        data class Allowed(val plan: CancellationPlan) : CancellationCheck
        data class Refused(val reason: String) : CancellationCheck
    }

    /**
     * الفسخ لا يمحو: العربون وما دُفع يُعكس بقيد عكسي ظاهر للطرفين، والأقساط المفتوحة تُلغى حالاتها.
     * لا يجوز الفسخ مرتين: العكس مرة واحدة.
     */
    fun planCancellation(state: State, reversedBefore: Boolean = false): CancellationCheck {
        if (state.status == DealStatus.CANCELLED) {
            return CancellationCheck.Refused("الصلح مفسوخ سابقًا: لا تكرار للعكس")
        }
        if (reversedBefore) {
            return CancellationCheck.Refused("عُكس هذا الصلح بقيد سابق: لا عكس مرتين")
        }
        val paidFromInstallments = state.installments.sumOf { it.paidMinor }
        val toReverse = state.advanceMinor + paidFromInstallments
        val notes = mutableListOf<String>()
        notes += "الفسخ لا يمحو سجلًا: قيد عكسي ظاهر للطرفين بقيمة ما قُبض."
        if (state.commission.totalMinor > 0L) {
            notes += "السعاية تُعكس معه بقيد مستقل في حساب الدلال، ولا تُنقل إلى دَين المحصول."
        }
        if (toReverse == 0L) notes += "لم يُقبض شيء بعد: الفسخ يُلغي الأقساط المفتوحة بلا قيود."
        return CancellationCheck.Allowed(
            CancellationPlan(
                status = DealStatus.CANCELLED,
                paidTotalMinor = paidFromInstallments,
                amountsToReverseMinor = toReverse,
                cancelledInstallments = state.installments
                    .filter { InstallmentStatus.isOpen(it.status) }
                    .map { it.id },
                notes = notes
            )
        )
    }

    // ------------------------------------------------------------- التعديل

    sealed interface EditCheck {
        data object Allowed : EditCheck
        data class Refused(val reason: String) : EditCheck
    }

    fun checkEdit(draft: Draft, actorMemberId: String, acknowledged: Boolean, anyPaymentMade: Boolean): EditCheck {
        if (draft.createdByMemberId.isBlank() || actorMemberId != draft.createdByMemberId) {
            return EditCheck.Refused("لا تعديل من غير منشئ الصلح: التغيير يكون بقيد عكسي معلن أو صلح جديد")
        }
        if (anyPaymentMade) {
            return EditCheck.Refused("بعد أول دفعة لا يُعدَّل الصلح: التصحيح بقيد عكسي يراه الطرفان")
        }
        if (acknowledged) {
            return EditCheck.Refused("الصلح مُقرّ من الطرفين: لا تعديل بعده — التغيير بصلح جديد")
        }
        return EditCheck.Allowed
    }

    // ------------------------------------------------------------- البيان

    private fun amount(minor: Long, currency: String): String {
        val code = Currency.fromCode(currency) ?: return "$minor فلسًا"
        return MoneyFormat.format(Money.ofMinor(minor, code))
    }

    /** بيان الصلح بنصّ واحد يُعرض في الشاشة وفي PDF وفي واتساب: مصدر واحد لا يتفرّق. */
    fun statementText(plan: Plan): String = buildString {
        append("بيان صلح: ").append(plan.draft.cropTitle.trim())
        if (plan.draft.place.isNotBlank()) append(" • ").append(plan.draft.place.trim())
        append("\nالبائع: ").append(plan.draft.seller.name.trim())
        append(" • المشتري: ").append(plan.draft.buyer.name.trim())
        plan.draft.broker?.let { if (it.name.isNotBlank()) append(" • الدلال: ").append(it.name.trim()) }
        append("\nالقيمة: ").append(amount(plan.draft.totalMinor, plan.draft.currency))
        append(" • العربون: ").append(amount(plan.draft.advanceMinor, plan.draft.currency))
        append(" • المتبقي: ").append(amount(plan.remainingMinor, plan.draft.currency))
        if (plan.commission.totalMinor > 0L) {
            append("\nالسعاية: ").append(amount(plan.commission.totalMinor, plan.draft.currency))
            append(" (").append(CommissionPayer.label(plan.draft.commission.payer)).append(")")
        }
        append("\nالمشتري عليه: ").append(amount(plan.buyerOwesMinor, plan.draft.currency))
        append(" • البائع يستلم: ").append(amount(plan.sellerReceivesMinor, plan.draft.currency))
        if (plan.schedule.isNotEmpty()) {
            append("\nالأقساط: ").append(plan.schedule.size)
            append(" كل ").append(plan.draft.intervalDays).append(" يومًا، أولها ")
            append(amount(plan.schedule.first().amountMinor, plan.draft.currency))
            append(" وآخرها ").append(amount(plan.schedule.last().amountMinor, plan.draft.currency))
        } else {
            append("\nلا أقساط: العربون غطّى القيمة كاملة")
        }
        if (plan.draft.terms.isNotBlank()) append("\nالشروط: ").append(plan.draft.terms.trim())
        if (plan.commission.sellerMinor > 0L) {
            append("\nعلى البائع من السعاية: ").append(amount(plan.commission.sellerMinor, plan.draft.currency))
        }
        if (plan.commission.buyerMinor > 0L) {
            append(" • على المشتري منها: ").append(amount(plan.commission.buyerMinor, plan.draft.currency))
        }
    }

    /** البيان يرفض الإطلاق بلا محتوى: نصّ قصير يشرح الحالة لمن يقرأ في الإشعار. */
    fun statusLine(status: String, remainingMinor: Long, currency: String): String = buildString {
        append(DealStatus.label(status))
        if (remainingMinor > 0L) append(" • المتبقي: ").append(amount(remainingMinor, currency))
        else append(" • لا متبقٍ")
    }
}

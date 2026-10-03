package com.baynana.domain.settlement

import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money

/**
 * حساب السعاية وجدول الأقساط: كل الأرقام بالوحدة الصغرى (فلس)، بلا `Double`/`Float` (ADR-04)،
 * وبلا تقريب خفي: كل قسمة تُقرَّب HALF_UP مرة واحدة، والباقي يُحمَّل على آخر قسط حتى يكون
 * **مجموع الأقساط = المتبقي بالضبط**، فلا يضيع فلس ولا يظهر قرش شبح.
 */
object CommissionEngine {

    /** سياسة السعاية: نسبة بالأساس (basis points) أو مبلغ ثابت، وسقف اختياري، ومن يدفع. */
    data class Policy(
        val payer: String = CommissionPayer.BUYER,
        /** 250 = 2.5%. صفر يعني «بلا نسبة». */
        val rateBasisPoints: Int = 0,
        /** يُستخدم فقط عندما تكون النسبة صفرًا. */
        val fixedMinor: Long = 0L,
        /** سقف اختياري بالوحدة الصغرى؛ صفر يعني بلا سقف. */
        val capMinor: Long = 0L,
        /** عند [CommissionPayer.SPLIT]: نصيب البائع بالأساس (5000 = نصفان). */
        val sellerShareBasisPoints: Int = 5_000
    )

    data class Breakdown(
        val totalMinor: Long,
        val sellerMinor: Long,
        val buyerMinor: Long,
        val rateBasisPoints: Int,
        val cappedByMinor: Long
    ) {
        init {
            check(sellerMinor + buyerMinor == totalMinor) {
                "توزيع السعاية لا يطابق المجموع: $sellerMinor + $buyerMinor ≠ $totalMinor"
            }
        }
    }

    sealed interface Check {
        data class Ok(val breakdown: Breakdown) : Check
        data class Refused(val reason: String) : Check
    }

    /**
     * يحسب السعاية على [amountMinor] وفق [policy].
     *
     * @param amountMinor قيمة الصلح كاملة (الثمن) لا المتبقي: السعاية على البيع، لا على ما تأخّر منه.
     */
    fun compute(amountMinor: Long, currencyCode: String, policy: Policy): Check {
        val currency = Currency.fromCode(currencyCode)
            ?: return Check.Refused("عملة غير معروفة: $currencyCode")
        if (amountMinor <= 0L) return Check.Refused("قيمة الصلح يجب أن تكون أكبر من صفر")
        if (amountMinor > Money.MAX_MINOR) return Check.Refused("قيمة الصلح أكبر من الحد المسموح")

        if (policy.payer !in listOf(CommissionPayer.SELLER, CommissionPayer.BUYER, CommissionPayer.SPLIT)) {
            return Check.Refused("من يدفع السعاية؟ حدّد البائع أو المشتري أو القسمة بينهما")
        }
        if (policy.rateBasisPoints < 0 || policy.rateBasisPoints > DealLimits.MAX_RATE_BASIS_POINTS) {
            return Check.Refused(
                "سعاية ${policy.rateBasisPoints / 100}% غير مقبولة: " +
                    "السقف ${DealLimits.MAX_RATE_BASIS_POINTS / 100}% من قيمة الصلح"
            )
        }
        if (policy.rateBasisPoints > 0 && policy.fixedMinor > 0L) {
            return Check.Refused("السعاية إما نسبة أو مبلغ ثابت، لا الاثنان معًا")
        }
        if (policy.fixedMinor < 0L || policy.capMinor < 0L) {
            return Check.Refused("لا تُقبل سعاية سالبة")
        }
        if (policy.payer == CommissionPayer.SPLIT &&
            (policy.sellerShareBasisPoints < 0 || policy.sellerShareBasisPoints > 10_000)
        ) {
            return Check.Refused("نصيب البائع في السعاية يجب أن يكون بين 0% و100%")
        }

        val money = Money.ofMinor(amountMinor, currency)
        val raw = when {
            policy.rateBasisPoints > 0 -> money.times(policy.rateBasisPoints.toLong(), 10_000L).minor
            else -> policy.fixedMinor
        }
        if (raw > Money.MAX_MINOR) return Check.Refused("السعاية أكبر من الحد المسموح")
        val total = if (policy.capMinor > 0L && raw > policy.capMinor) policy.capMinor else raw

        val sellerShare = when (policy.payer) {
            CommissionPayer.SELLER -> total
            CommissionPayer.BUYER -> 0L
            else -> Money.ofMinor(total, currency)
                .times(policy.sellerShareBasisPoints.toLong(), 10_000L).minor
        }
        val buyerShare = total - sellerShare

        return Check.Ok(
            Breakdown(
                totalMinor = total,
                sellerMinor = sellerShare,
                buyerMinor = buyerShare,
                rateBasisPoints = policy.rateBasisPoints,
                cappedByMinor = if (policy.capMinor > 0L && raw > policy.capMinor) policy.capMinor else 0L
            )
        )
    }
}

object InstallmentScheduleEngine {

    data class Installment(val index: Int, val dueAt: Long, val amountMinor: Long)

    sealed interface Check {
        data class Ok(val installments: List<Installment>, val remainingMinor: Long) : Check
        data class Refused(val reason: String) : Check
    }

    /**
     * يبني جدول الأقساط من الصلح: العربون أولًا، ثم الباقي أقساطًا متساوية (باستثناء الأخير الذي
     * يحمل الباقي)، وتواريخ الاستحقاق تصاعدية ومشتقة من تاريخ يحدده المستخدم لا من «الآن».
     */
    fun build(
        totalMinor: Long,
        advanceMinor: Long,
        count: Int,
        firstDueAt: Long,
        intervalDays: Int,
        currencyCode: String
    ): Check {
        Currency.fromCode(currencyCode) ?: return Check.Refused("عملة غير معروفة: $currencyCode")
        if (totalMinor <= 0L) return Check.Refused("قيمة الصلح يجب أن تكون أكبر من صفر")
        if (totalMinor > Money.MAX_MINOR) return Check.Refused("قيمة الصلح أكبر من الحد المسموح")
        if (advanceMinor < 0L) return Check.Refused("العربون لا يكون سالبًا")
        if (advanceMinor > totalMinor) return Check.Refused("العربون أكبر من قيمة الصلح")

        val remaining = totalMinor - advanceMinor
        if (remaining == 0L) {
            // العربون غطّى الصلح كاملًا: لا أقساط، والصلح ينتقل إلى «مكتمل» بعد الإقرار.
            return Check.Ok(emptyList(), 0L)
        }

        if (count < 1) return Check.Refused("عدد الأقساط يجب أن يكون قسطًا واحدًا على الأقل")
        if (count > DealLimits.MAX_INSTALLMENTS) {
            return Check.Refused("عدد الأقساط أكبر من الحد: ${DealLimits.MAX_INSTALLMENTS} قسطًا")
        }
        if (firstDueAt <= 0L) return Check.Refused("تاريخ أول قسط مطلوب")
        if (intervalDays < DealLimits.MIN_INTERVAL_DAYS || intervalDays > DealLimits.MAX_INTERVAL_DAYS) {
            return Check.Refused(
                "الفاصل بين الأقساط يجب أن يكون بين ${DealLimits.MIN_INTERVAL_DAYS} و" +
                    "${DealLimits.MAX_INTERVAL_DAYS} يومًا"
            )
        }

        val base = remaining / count
        val remainder = remaining % count
        if (base <= 0L) {
            return Check.Refused("المتبقي أصغر من أن يُقسَّم على $count أقساط")
        }

        val dayMillis = 86_400_000L
        val installments = (0 until count).map { index ->
            Installment(
                index = index + 1,
                dueAt = firstDueAt + index * intervalDays * dayMillis,
                // آخر قسط يحمل الباقي، فيكون المجموع مطابقًا للمتبقي بالضبط.
                amountMinor = if (index == count - 1) base + remainder else base
            )
        }

        val sum = installments.sumOf { it.amountMinor }
        check(sum == remaining) {
            "مجموع الأقساط ($sum) لا يطابق المتبقي ($remaining) — لا يُقبل جدول ناقص فلسًا"
        }

        return Check.Ok(installments, remaining)
    }
}

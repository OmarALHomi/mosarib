package com.baynana.domain.ledger

/** نتيجة فحص إمكانية الإلغاء. */
sealed interface ReversalCheck {
    data object Allowed : ReversalCheck
    data class Refused(val reason: String) : ReversalCheck
}

/**
 * القيود العكسية (الخطة v6 §4.4): **لا حذف لقيد شارك فيه طرف آخر**، بل قيد عكسي يظهر للطرفين.
 *
 * القيد العكسي:
 * - مرآة القيد الأصلي: نفس المبلغ ونفس العملة ونفس الغرفة، والطرفان معكوسان — فيعود أثر
 *   الأصل إلى الصفر حسابيًا بلا أي رقم سالب مخزَّن.
 * - نوعه [EntryType.ADJUSTMENT] ويشير إلى الأصل في `reversesEntryId`، فيُعرف في الشاشة وكشف
 *   الطباعة أنه إلغاء لا دين جديد.
 * - يُلغي أثر الإسقاطات المرتبطة بالأصل: السداد يعود رصيدًا متاحًا، والدين المسدَّد جزئيًا
 *   يعود مفتوحًا بمقدار ما تحرّر. القيود نفسها تبقى محفوظة للتاريخ.
 */
object ReversalEngine {

    /** هل يجوز إلغاء هذا القيد؟ */
    fun check(entry: EntryView, alreadyReversedBy: EntryView?): ReversalCheck = when {
        entry.status == EntryStatus.VOIDED || alreadyReversedBy != null ->
            Refused("قيد ملغى سابقًا بقيد عكسي")
        entry.isReversal ->
            Refused("لا يُلغى قيد عكسي مباشرة؛ أنشئ قيدًا جديدًا يوضح التصحيح")
        entry.status == EntryStatus.DRAFT ->
            Refused("مسودة لم تُشارك بعد؛ احذفها بدل إلغائها")
        entry.amountMinor <= 0L ->
            Refused("قيد بلا مبلغ")
        else -> Allowed
    }

    /** يبني القيد العكسي: نفس المبلغ والعملة، والطرفان معكوسان، ويشير إلى الأصل. */
    fun mirror(
        original: EntryView,
        reversalId: String,
        operationId: String,
        occurredAt: Long,
        reason: String
    ): EntryView {
        require(check(original, null) is ReversalCheck.Allowed) { "لا يجوز عكس هذا القيد" }
        return EntryView(
            id = reversalId,
            operationId = operationId,
            roomId = original.roomId,
            type = EntryType.ADJUSTMENT,
            owedByMemberId = original.owedToMemberId,
            owedToMemberId = original.owedByMemberId,
            amountMinor = original.amountMinor,
            currency = original.currency,
            occurredAt = occurredAt,
            status = EntryStatus.SENT,
            description = reason.ifBlank { "إلغاء قيد سابق" },
            reversesEntryId = original.id
        )
    }
}

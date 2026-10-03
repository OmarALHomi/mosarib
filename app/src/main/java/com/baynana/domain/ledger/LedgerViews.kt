package com.baynana.domain.ledger

/**
 * صورة القيد كما يحتاجها المحرّك المحاسبي: بلا أي اعتماد على Room أو Android، وبلا أي `Double`.
 * المبالغ كلها بالوحدة الصغرى (`Long`) — فلس لليمني — كما في ADR-04.
 */
data class EntryView(
    val id: String,
    val operationId: String,
    val roomId: String,
    val type: String,
    /** الطرف الذي عليه المال في هذا القيد. */
    val owedByMemberId: String,
    /** الطرف الذي له المال في هذا القيد. */
    val owedToMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Long,
    val status: String,
    val description: String = "",
    val reversesEntryId: String? = null
) {
    val isDebt: Boolean get() = type in EntryType.debts
    val isCredit: Boolean get() = type in EntryType.credits
    val isReversal: Boolean get() = reversesEntryId != null
    val isOpen: Boolean get() = status != EntryStatus.VOIDED

    /** هل يقابل هذا القيد [other] في الاتجاه والعملة والغرفة؟ (القيد مرآة نظيره) */
    fun mirrors(other: EntryView): Boolean =
        roomId == other.roomId &&
            currency == other.currency &&
            owedByMemberId == other.owedToMemberId &&
            owedToMemberId == other.owedByMemberId
}

/** إسقاط قائم بين سداد ودين، كما هو مخزَّن، ليبني عليه المحرّك المتاح. */
data class AllocationView(
    val paymentEntryId: String,
    val debtEntryId: String,
    val amountMinor: Long
)

/** مجمّع المتاح: كم أُسقط من كل قيد حتى الآن (سدادًا كان أو دينًا). */
fun allocationsByEntry(allocations: List<AllocationView>): Map<String, Long> {
    val totals = mutableMapOf<String, Long>()
    for (allocation in allocations) {
        totals[allocation.paymentEntryId] = (totals[allocation.paymentEntryId] ?: 0L) + allocation.amountMinor
        totals[allocation.debtEntryId] = (totals[allocation.debtEntryId] ?: 0L) + allocation.amountMinor
    }
    return totals
}

/** المتاح من قيد = مبلغه ناقص ما أُسقط منه. لا سالب أبدًا. */
fun EntryView.remainingMinor(allocatedByEntry: Map<String, Long>): Long {
    val allocated = allocatedByEntry[id] ?: 0L
    return (amountMinor - allocated).coerceAtLeast(0L)
}

/**
 * القيود المؤثرة حسابيًا في الأرصدة:
 * - **الملغى** يُستثنى (أثره سقط).
 * - **القيد العكسي** يُستثنى كذلك لأن أصله ملغى: لو حُسب لَحُسب الإلغاء مرتين — مرة بإسقاط
 *   الأصل ومرة بعكس الاتجاه — فصار الرصيد موجبًا بعد إلغاء دين. دوره الإظهار والتبليغ لا الحساب.
 * - **المسودة** تُستثنى لأنها لم تُشارك: لا تُدرج في أرقام يراها الطرف الآخر، بل تُعدّ وحدها
 *   ليُعلن للمستخدم أن لديه مسودات لم تُرسل.
 */
fun List<EntryView>.effectiveEntries(): List<EntryView> {
    if (isEmpty()) return this
    val voidedIds = filter { it.status == EntryStatus.VOIDED }.map { it.id }.toSet()
    return filter { entry ->
        entry.status != EntryStatus.VOIDED &&
            entry.status != EntryStatus.DRAFT &&
            (entry.reversesEntryId == null || entry.reversesEntryId !in voidedIds)
    }
}

/** رصيد عضو: موجب يعني أن الغرفة له، وسالب يعني أن عليه. */
data class MemberBalance(
    val memberId: String,
    val netMinor: Long
)

/**
 * رصيد غرفة واحدة بعملة واحدة، مشتقّ من القيود والإسقاطات لا من عمود مخزَّن.
 *
 * لا يوجد «صافي» واحد يخفي الحقيقة: الديون المفتوحة على الأعضاء مفصولة عن الأرصدة الدائنة
 * (المبالغ التي بيد الطرف الآخر ولم تُخصَّص)، وكل عضو له رصيده المعلن.
 */
data class RoomBalance(
    val roomId: String,
    val currency: String,
    val members: List<MemberBalance>,
    /** مجموع ما على الأعضاء من قيود دين لم تُسقَط بعد. */
    val openDebtMinor: Long,
    /** مجموع ما بيد الطرف الآخر من سداد/قبض لم يُخصَّص بعد (رصيد دائن في الغرفة). */
    val unappliedReceiptMinor: Long,
    /** عدد القيود التي تنتظر إقرار الطرف الآخر. */
    val awaitingAcknowledgement: Int,
    /** عدد المسودات المحلية غير المشاركة (لا تدخل في الأرقام المعلنة). */
    val draftCount: Int = 0
) {
    fun netOf(memberId: String): Long = members.firstOrNull { it.memberId == memberId }?.netMinor ?: 0L

    /** هل الغرفة صافية تمامًا: لا دين مفتوح ولا رصيد دائن معلّق. */
    val isSettled: Boolean get() = openDebtMinor == 0L && unappliedReceiptMinor == 0L
}

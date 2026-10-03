package com.baynana.domain.ledger

/** كيف يطلب المستخدم تخصيص السداد. */
sealed interface AllocationMode {
    /**
     * «هذا سداد للديون» بلا اختيار: الأقدم فالأقدم داخل الغرفة والعملة والاتجاه نفسه.
     * الشاشة تعرض ما سيحدث قبل التنفيذ (تأكيد ظاهر) ثم تنفّذ.
     */
    data object OldestFirst : AllocationMode

    /** أسطر مختارة بعينها: لا تُمس إلا هذه الأسطر. */
    data class Selected(val entryIds: List<String>) : AllocationMode

    /** لا تخصيص: قبض عام يبقى رصيدًا دائنًا في الغرفة حتى يقرر الطرفان. */
    data object None : AllocationMode
}

/** سطر نُظر فيه ولم يُخصَّص، مع سبب عربي مفهوم يُعرض للمستخدم. */
data class SkippedLine(val entryId: String, val reason: String)

data class PlannedAllocation(val debtEntryId: String, val amountMinor: Long)

/**
 * خطة التخصيص كما يحسبها المحرّك: ما سيُسقَط على أي دين وبكم، وما يبقى غير مخصَّص (رصيد دائن
 * في الغرفة نفسها والعملة نفسها)، والأسطر التي لم تُقبل مع سببها.
 */
data class AllocationPlan(
    val allocations: List<PlannedAllocation>,
    val appliedMinor: Long,
    val unappliedMinor: Long,
    val skipped: List<SkippedLine>
) {
    val isEmpty: Boolean get() = allocations.isEmpty()
}

/**
 * محرّك التخصيص: نقلة واحدة، بلا آثار جانبية، وبلا أي `Double` (كل الحساب `Long`).
 *
 * القواعد المثبّتة (الخطة v6 §4.3 و§4.4):
 * - لا يُخصَّص إلا على دين **مقابل** للسداد: نفس الغرفة، نفس العملة، والاتجاه معكوس تمامًا
 *   (سداد المزارع لا يُسقَط على دين مزارع آخر ولا على دين المسرب نفسه في الاتجاه المعاكس).
 * - لا يُخصَّص على مسودة (ليست مشتركة) ولا على قيد ملغى، ولا على قيد معترَض عليه أو مطلوب
 *   تعديله: يُحلّ الاعتراض أولًا.
 * - المتبقي بعد التخصيص يبقى **رصيدًا دائنًا في الغرفة**، ولا يُنقل لشخص ثالث ولا يُحوَّل إلى
 *   عملة أخرى. ولا يُسقَط أكثر من قيمة الدين ولا أكثر من مبلغ السداد.
 * - الترتيب حتمي: الأقدم أولًا (زمن الوقوع ثم المعرّف) فلا يختلف الترتيب بين جهازين.
 */
object AllocationEngine {

    fun plan(
        receipt: EntryView,
        candidates: List<EntryView>,
        allocatedByEntry: Map<String, Long>,
        mode: AllocationMode
    ): AllocationPlan {
        if (mode is AllocationMode.None) {
            return AllocationPlan(emptyList(), 0L, receipt.remainingMinor(allocatedByEntry), emptyList())
        }
        if (!receipt.isCredit) {
            return AllocationPlan(
                emptyList(), 0L, 0L,
                listOf(SkippedLine(receipt.id, "القيد ليس سدادًا ولا قبضًا، فلا يُخصَّص على ديون"))
            )
        }

        var available = receipt.remainingMinor(allocatedByEntry)
        val skipped = mutableListOf<SkippedLine>()
        val eligible = mutableListOf<EntryView>()

        // الترتيب حتمي: الأقدم فالأقدم ثم المعرّف لفصل التعادل.
        val roomCandidates = candidates
            .filter { it.id != receipt.id }
            .sortedWith(compareBy({ it.occurredAt }, { it.id }))

        val requested: Set<String>? = (mode as? AllocationMode.Selected)?.entryIds?.toSet()

        for (candidate in roomCandidates) {
            if (requested != null && candidate.id !in requested) continue
            val reason = ineligibilityReason(receipt, candidate, allocatedByEntry)
            if (reason != null) {
                skipped += SkippedLine(candidate.id, reason)
                continue
            }
            eligible += candidate
        }
        if (requested != null) {
            // سطر طلبه المستخدم ولم نجده بين المرشّحين أصلًا (محذوف، أو من غرفة أخرى).
            val known = roomCandidates.map { it.id }.toSet()
            requested.filterNot { it in known }.forEach {
                skipped += SkippedLine(it, "القيد المطلوب ليس في هذه الغرفة")
            }
        }

        val allocations = mutableListOf<PlannedAllocation>()
        for (debt in eligible) {
            if (available <= 0L) {
                skipped += SkippedLine(debt.id, "انتهى مبلغ السداد قبل هذا السطر")
                continue
            }
            val remainingDebt = debt.remainingMinor(allocatedByEntry)
            val take = minOf(available, remainingDebt)
            if (take <= 0L) continue
            allocations += PlannedAllocation(debt.id, take)
            available -= take
        }

        val applied = allocations.sumOf { it.amountMinor }
        return AllocationPlan(
            allocations = allocations,
            appliedMinor = applied,
            unappliedMinor = (receipt.remainingMinor(allocatedByEntry) - applied).coerceAtLeast(0L),
            skipped = skipped
        )
    }

    /** سبب عدم صلاحية السطر، أو null إن كان صالحًا للتخصيص. */
    private fun ineligibilityReason(
        receipt: EntryView,
        candidate: EntryView,
        allocatedByEntry: Map<String, Long>
    ): String? {
        if (!candidate.isDebt) {
            return when {
                candidate.isReversal -> "قيد عكسي: لا يُخصَّص عليه سداد"
                candidate.id == receipt.id -> "القيد نفسه"
                else -> "ليس دينًا"
            }
        }
        if (candidate.roomId != receipt.roomId) return "من غرفة أخرى"
        if (candidate.currency != receipt.currency) return "بعملة أخرى: لا تحويل تلقائي"
        if (!receipt.mirrors(candidate)) return "لدين طرف آخر في الغرفة نفسها"
        if (candidate.status !in EntryStatus.allocatable) {
            return when (candidate.status) {
                EntryStatus.DRAFT -> "مسودة لم تُشارك بعد"
                EntryStatus.VOIDED -> "قيد ملغى بقيد عكسي"
                EntryStatus.DISPUTED -> "قيد معترَض عليه: احسم الاعتراض أولًا"
                EntryStatus.CHANGE_REQUESTED -> "قيد مطلوب تعديله: احسم الطلب أولًا"
                else -> "حالة غير قابلة للتخصيص: ${candidate.status}"
            }
        }
        if (candidate.remainingMinor(allocatedByEntry) <= 0L) return "مسدَّد بالكامل"
        return null
    }
}

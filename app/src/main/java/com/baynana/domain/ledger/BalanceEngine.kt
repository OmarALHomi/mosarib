package com.baynana.domain.ledger

/**
 * حساب الأرصدة: مصدر واحد للحقيقة الرقمية (الخطة v6 §4.5).
 *
 * القاعدة الحسابية الواحدة: كل قيد يخصم `amountMinor` من الطرف الذي عليه `owedByMemberId`
 * ويضيفه إلى الطرف الذي له `owedToMemberId`. فالسقية تُنقص رصيد المزارع وتزيد رصيد المسرب،
 * والسداد (وهو مرآة السقية) يفعل العكس. لا سالب مخزَّن ولا «صافي» يخفي حقيقة.
 *
 * **نقطة دقيقة يُسأل عنها كثيرًا:** الحساب **طرح كامل** لا «صافي إسقاطات». فدين 1,000,000
 * سُدّد منه 400,000 يظهر أصله في الرصيد (−1,000,000) ويسدّه السداد (+400,000) فيبقى −600,000.
 * ولهذا يُبنى الرصيد من **كل** القيود المؤثرة لا من المفتوحة فقط: لو حُسبت المفتوحة وحدها
 * لظهر الرصيد −1,000,000 وهو خطأ. ولهذا أيضًا يُصفَّر الرصيد تمامًا عند إلغاء قيد بمقابله
 * العكسي: الطرح يلغي الطرح، والإسقاطات تُحرَّر فلا يبقى أثر لأيّ منهما.
 *
 * الأرصدة **مشتقّة** من القيود والإسقاطات، لا من أعمدة مخزّنة (§4.4): فلا يفسد رصيد إن فشلت
 * كتابة جزئية، ولا يحتاج ترحيلًا لو تغيّرت القاعدة.
 */
object BalanceEngine {

    /**
     * @param entries كل قيود الغرفة (المسودات تُستثنى: ليست مشتركة بعد).
     * @param allocations إسقاطات الغرفة.
     * @param roomId الغرفة.
     * @param currency عملة الغرفة؛ ما خالفها يُستثنى ولا يُخلط (§4.1).
     */
    fun compute(
        roomId: String,
        currency: String,
        entries: List<EntryView>,
        allocations: List<AllocationView>
    ): RoomBalance {
        val allocated = allocationsByEntry(allocations)
        val positions = mutableMapOf<String, Long>()
        var openDebt = 0L
        var unapplied = 0L
        var awaiting = 0

        // المسودات تُعدّ ولا تُحسب: تُعلن للمستخدم ولا تدخل أرقام الطرف الآخر.
        val drafts = entries.count {
            it.roomId == roomId && it.currency == currency && it.status == EntryStatus.DRAFT
        }

        for (entry in entries.effectiveEntries()) {
            if (entry.roomId != roomId) continue
            if (entry.currency != currency) continue

            // الطرفان يظهران في الرصيد حتى لو تقاصّا إلى صفر: الظهور أهم من الرقم.
            positions.putIfAbsent(entry.owedByMemberId, 0L)
            positions.putIfAbsent(entry.owedToMemberId, 0L)
            positions[entry.owedByMemberId] = positions.getValue(entry.owedByMemberId) - entry.amountMinor
            positions[entry.owedToMemberId] = positions.getValue(entry.owedToMemberId) + entry.amountMinor

            when {
                entry.isDebt -> openDebt += entry.remainingMinor(allocated)
                entry.isCredit -> unapplied += entry.remainingMinor(allocated)
            }
            if (entry.status == EntryStatus.SENT) awaiting++
        }

        val members = positions
            .map { MemberBalance(it.key, it.value) }
            .sortedBy { it.memberId }

        return RoomBalance(
            roomId = roomId,
            currency = currency,
            members = members,
            openDebtMinor = openDebt,
            unappliedReceiptMinor = unapplied,
            awaitingAcknowledgement = awaiting,
            draftCount = drafts
        )
    }

    /**
     * ما بقي على عضو بعينه من قيود **لم تُخصَّص** بعد، مفصولًا: ديونه المفتوحة، وما بيده من
     * مال الطرف الآخر (رصيد دائن).
     */
    fun openLinesFor(memberId: String, entries: List<EntryView>, allocations: List<AllocationView>): OpenLines {
        val allocated = allocationsByEntry(allocations)
        var owes = 0L
        var held = 0L
        for (entry in entries.effectiveEntries()) {
            val remaining = entry.remainingMinor(allocated)
            if (remaining <= 0L) continue
            if (entry.owedByMemberId == memberId && entry.isDebt) owes += remaining
            if (entry.owedByMemberId == memberId && entry.isCredit) held += remaining
        }
        return OpenLines(owesMinor = owes, heldMinor = held)
    }

    /** ديون مفتوحة على عضو، مرتبة الأقدم أولًا كما سيراها في الشاشة. */
    fun openDebtsFor(
        memberId: String,
        entries: List<EntryView>,
        allocations: List<AllocationView>
    ): List<EntryView> {
        val allocated = allocationsByEntry(allocations)
        return entries.effectiveEntries()
            .filter { it.isDebt && it.owedByMemberId == memberId && it.remainingMinor(allocated) > 0L }
            .sortedWith(compareBy({ it.occurredAt }, { it.id }))
    }
}

/** ديون عضو وما بيده من مال الآخر: لا يُجمعان في رقم واحد (§4.1). */
data class OpenLines(val owesMinor: Long, val heldMinor: Long) {
    val isClear: Boolean get() = owesMinor == 0L && heldMinor == 0L
}

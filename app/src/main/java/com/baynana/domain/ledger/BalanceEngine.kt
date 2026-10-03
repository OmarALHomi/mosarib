package com.baynana.domain.ledger

/**
 * واجهة الأرصدة: بقيت للتوافق مع الاختبارات والشاشات التي كُتبت قبل اللقطة الموحّدة، لكن **كل
 * الحساب** انتقل إلى [SnapshotEngine] (ح٥) كي لا يوجد محرّكان يختلفان يومًا.
 *
 * القاعدة الحسابية الواحدة: كل قيد يخصم `amountMinor` من الطرف الذي عليه `owedByMemberId`
 * ويضيفه إلى الطرف الذي له `owedToMemberId`. فالسقية تُنقص رصيد المزارع وتزيد رصيد المسرب،
 * والسداد (وهو مرآة السقية) يفعل العكس. لا سالب مخزَّن ولا «صافي» يخفي حقيقة.
 *
 * **نقطة دقيقة يُسأل عنها كثيرًا:** الحساب **طرح كامل** لا «صافي إسقاطات». فدين 1,000,000
 * سُدّد منه 400,000 يظهر أصله في الرصيد (−1,000,000) ويسدّه السداد (+400,000) فيبقى −600,000.
 * ولهذا يُبنى الرصيد من **كل** القيود المؤثرة لا من المفتوحة فقط، ولهذا يُصفَّر الرصيد تمامًا
 * عند إلغاء قيد بمقابله العكسي: الطرح يلغي الطرح، والإسقاطات تُحرَّر.
 *
 * الأرصدة **مشتقّة** من القيود والإسقاطات، لا من أعمدة مخزّنة (§4.4).
 */
object BalanceEngine {

    /** لقطة كاملة للغرفة: المصدر الوحيد لكل رقم معروض. */
    fun snapshot(
        roomId: String,
        currency: String,
        entries: List<EntryView>,
        allocations: List<AllocationView>
    ): LedgerSnapshot = SnapshotEngine.build(roomId, currency, entries, allocations)

    /** الأرصدة بصيغة [RoomBalance] (تحويل مباشر من اللقطة، بلا إعادة حساب). */
    fun compute(
        roomId: String,
        currency: String,
        entries: List<EntryView>,
        allocations: List<AllocationView>
    ): RoomBalance = snapshot(roomId, currency, entries, allocations).toRoomBalance()

    fun openLinesFor(memberId: String, entries: List<EntryView>, allocations: List<AllocationView>): OpenLines =
        SnapshotEngine.openLinesFor(memberId, entries, allocations)

    fun openDebtsFor(
        memberId: String,
        entries: List<EntryView>,
        allocations: List<AllocationView>
    ): List<EntryView> = SnapshotEngine.openDebtsFor(memberId, entries, allocations)
}

/** ديون عضو وما بيده من مال الآخر: لا يُجمعان في رقم واحد (§4.1). */
data class OpenLines(val owesMinor: Long, val heldMinor: Long) {
    val isClear: Boolean get() = owesMinor == 0L && heldMinor == 0L
}

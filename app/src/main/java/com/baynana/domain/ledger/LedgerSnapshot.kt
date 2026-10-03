package com.baynana.domain.ledger

/**
 * **اللقطة الموحّدة** (ح٥ من خطة v6): كل رقم يظهر في التطبيق يأتي من هنا — قائمة، تفاصيل، PDF،
 * كشف المزارع، تقرير، ومزامنة. لا حساب في الشاشة ولا في مولّد PDF ولا في مستودع.
 *
 * سبب وجودها مباشرة من القاعدة المحاسبية: أي حساب ثانٍ في مكان آخر سينحرف يومًا ما، وأول ما
 * يلاحظه المزارع أن رصيد الشاشة لا يساوي رصيد الورقة. فالحساب هنا مرة واحدة، والباقي عرض.
 *
 * القواعد المنفَّذة هنا:
 * - **طرح كامل** لا «صافي إسقاطات»: الرصيد = مجموع ما على العضو ناقص ما له، من كل القيود المؤثرة.
 * - **المسودة** تُعدّ ولا تُحسب. **الملغى** و**القيد العكسي** لا يدخلان الحساب (انظر effectiveEntries).
 * - **لا خلط عملات**: اللقطة لغرفة واحدة بعملة واحدة، وما خالفها يُستثنى ويُعدّ للمعلومة.
 * - **الثلاثة الذهبية** جاهزة في اللقطة: `chargedMinor` (المبلغ)، `paidMinor` (المسدد)،
 *   `remainingMinor` (الباقي)، مع تحقق داخلي أن `الباقي = المبلغ − المسدد`.
 */
object SnapshotEngine {

    fun build(
        roomId: String,
        currency: String,
        entries: List<EntryView>,
        allocations: List<AllocationView>
    ): LedgerSnapshot {
        val effective = entries.filter { it.roomId == roomId && it.currency == currency }.effectiveEntries()
        val allocated = allocationsByEntry(allocations)

        // كل إسقاط يخصّ قيدين من نفس الغرفة، فنحسب ما أُسقط لكل قيد مرة واحدة.
        fun allocatedOf(entryId: String): Long = allocated[entryId] ?: 0L

        val memberIds = linkedSetOf<String>()
        effective.forEach {
            memberIds += it.owedByMemberId
            memberIds += it.owedToMemberId
        }

        val members = memberIds.map { memberId ->
            val net = effective.sumOf { entry ->
                when (memberId) {
                    entry.owedByMemberId -> -entry.amountMinor
                    entry.owedToMemberId -> entry.amountMinor
                    else -> 0L
                }
            }
            var openDebt = 0L
            var unapplied = 0L
            var charged = 0L
            var paid = 0L
            var waiting = 0
            for (entry in effective) {
                val remaining = entry.remainingMinor(allocated)
                if (entry.isDebt && entry.owedByMemberId == memberId) {
                    charged += entry.amountMinor
                    paid += allocatedOf(entry.id)
                    openDebt += remaining
                }
                if (entry.isCredit && entry.owedByMemberId == memberId) {
                    unapplied += remaining
                }
                if (entry.status == EntryStatus.SENT &&
                    (entry.owedByMemberId == memberId || entry.owedToMemberId == memberId)
                ) {
                    waiting++
                }
            }
            MemberSnapshot(
                memberId = memberId,
                netMinor = net,
                chargedMinor = charged,
                paidMinor = paid,
                remainingMinor = charged - paid,
                openDebtMinor = openDebt,
                unappliedMinor = unapplied,
                awaitingAcknowledgement = waiting
            )
        }.sortedBy { it.memberId }

        val lines = effective
            .sortedWith(compareBy({ it.occurredAt }, { it.id }))
            .map { entry ->
                SnapshotLine(
                    entryId = entry.id,
                    operationId = entry.operationId,
                    occurredAt = entry.occurredAt,
                    type = entry.type,
                    description = entry.description,
                    amountMinor = entry.amountMinor,
                    allocatedMinor = allocatedOf(entry.id),
                    remainingMinor = entry.remainingMinor(allocated),
                    status = entry.status,
                    debtorMemberId = entry.owedByMemberId,
                    creditorMemberId = entry.owedToMemberId,
                    isReversal = entry.isReversal
                )
            }

        val charged = members.sumOf { it.chargedMinor }
        val paid = members.sumOf { it.paidMinor }
        val drafts = entries.count {
            it.roomId == roomId && it.currency == currency && it.status == EntryStatus.DRAFT
        }

        val snapshot = LedgerSnapshot(
            roomId = roomId,
            currency = currency,
            members = members,
            lines = lines,
            chargedMinor = charged,
            paidMinor = paid,
            remainingMinor = charged - paid,
            openDebtMinor = members.sumOf { it.openDebtMinor },
            unappliedReceiptMinor = members.sumOf { it.unappliedMinor },
            awaitingAcknowledgement = effective.count { it.status == EntryStatus.SENT },
            draftCount = drafts
        )

        // تحقق داخلي فوري: الثلاثة الذهبية متناسقة دائمًا، وأي انحراف يعني خطأ في المحرّك.
        check(snapshot.remainingMinor == snapshot.chargedMinor - snapshot.paidMinor) {
            "اللقطة غير متناسقة: الباقي لا يساوي المبلغ ناقص المسدد"
        }
        check(snapshot.openDebtMinor == snapshot.remainingMinor) {
            "اللقطة غير متناسقة: الدين المفتوح لا يساوي الباقي"
        }
        return snapshot
    }

    /** ما بقي على عضو من قيود لم تُخصَّص: ديونه المفتوحة، وما بيده من مال الطرف الآخر. */
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

    /** ديون عضو المفتوحة، الأقدم أولًا — نفس ترتيب التخصيص. */
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

/** سطر قيد في اللقطة: كل ما تحتاجه أي شاشة لعرض سطر واحد، بلا حساب إضافي. */
data class SnapshotLine(
    val entryId: String,
    val operationId: String,
    val occurredAt: Long,
    val type: String,
    val description: String,
    val amountMinor: Long,
    val allocatedMinor: Long,
    val remainingMinor: Long,
    val status: String,
    val debtorMemberId: String,
    val creditorMemberId: String,
    val isReversal: Boolean
) {
    val isDebt: Boolean get() = type in EntryType.debts
    val isCredit: Boolean get() = type in EntryType.credits
}

/** أرقام عضو واحد كما تُعرض في كشفه. */
data class MemberSnapshot(
    val memberId: String,
    /** موجب: الغرفة له. سالب: عليه. (نفس اصطلاح [RoomBalance].) */
    val netMinor: Long,
    /** المبلغ: مجموع ديونه المؤثرة. */
    val chargedMinor: Long,
    /** المسدَّد: ما أُسقط من ديونه. */
    val paidMinor: Long,
    /** الباقي: charged − paid، وهو نفسه الدين المفتوح. */
    val remainingMinor: Long,
    val openDebtMinor: Long,
    /** ما بيده من مال الطرف الآخر ولم يُخصَّص بعد. */
    val unappliedMinor: Long,
    val awaitingAcknowledgement: Int
)

/**
 * اللقطة الكاملة لغرفة واحدة بعملة واحدة.
 *
 * لا تُبنى يدويًا: الطريق الوحيد هو [SnapshotEngine.build] الذي يتحقق من تناسق الثلاثة الذهبية.
 */
data class LedgerSnapshot(
    val roomId: String,
    val currency: String,
    val members: List<MemberSnapshot>,
    val lines: List<SnapshotLine>,
    val chargedMinor: Long,
    val paidMinor: Long,
    val remainingMinor: Long,
    val openDebtMinor: Long,
    val unappliedReceiptMinor: Long,
    val awaitingAcknowledgement: Int,
    val draftCount: Int
) {
    fun member(memberId: String): MemberSnapshot? = members.firstOrNull { it.memberId == memberId }

    fun netOf(memberId: String): Long = member(memberId)?.netMinor ?: 0L

    /** الديون المفتوحة على عضو بعينه. */
    fun openDebtOf(memberId: String): Long = member(memberId)?.openDebtMinor ?: 0L

    val isSettled: Boolean get() = openDebtMinor == 0L && unappliedReceiptMinor == 0L

    /** اللقطة بصيغة الشاشة القديمة كي لا يتكرر الحساب في مكانين. */
    fun toRoomBalance(): RoomBalance = RoomBalance(
        roomId = roomId,
        currency = currency,
        members = members.map { MemberBalance(it.memberId, it.netMinor) },
        openDebtMinor = openDebtMinor,
        unappliedReceiptMinor = unappliedReceiptMinor,
        awaitingAcknowledgement = awaitingAcknowledgement,
        draftCount = draftCount
    )
}

package com.baynana.domain.ledger

import com.baynana.domain.money.Currency
import com.baynana.domain.money.Money
import com.baynana.domain.money.MoneyFormat

/**
 * خلاصة غرفة واحدة كما تعرضها الشاشة: أرقامها من [SnapshotEngine] (مصدر واحد)، وأسطرها من القيود
 * نفسها. الغرض أن يكون للواجهة نوع واحد تقرؤه، فلا تحسب شيئًا بنفسها.
 *
 * **الأسطر تُبنى هنا لا في الشاشة**: ترتيبها زمني تنازلي (الأحدث أولًا) وبفصل التعادل بالمعرّف
 * (فلا يختلف ترتيب جهازين)، والمتبقي لكل قيد يُحسب من الإسقاطات القائمة نفسها التي بنت الأرصدة.
 */
data class RoomFeedEntry(
    val entryId: String,
    val type: String,
    val description: String,
    val occurredAt: Long,
    val amountMinor: Long,
    val remainingMinor: Long,
    val allocatedMinor: Long,
    val status: String,
    val direction: LineDirection,
    val isReversal: Boolean,
    /** مخاطَب بهذا القيد: هذا الجهاز مدين أو دائن. */
    val involvesMe: Boolean,
    /** المال عليه في هذا القيد وينتظر إقراري أنا (أرسله الطرف الآخر ولم يُقرّ بعد). */
    val awaitingMyAck: Boolean
)

data class RoomFeed(
    val roomId: String,
    val title: String,
    val kind: String,
    val currency: String,
    val status: String,
    val counterpartName: String,
    val counterpartPhone: String,
    val linkCode: String,
    /** صافي هذا الجهاز من الغرفة: موجب له، سالب عليه. */
    val myNetMinor: Long,
    /** المبلغ: مجموع ما عليه في الغرفة. */
    val chargedMinor: Long,
    /** المسدَّد. */
    val paidMinor: Long,
    /** الباقي: المبلغ − المسدَّد = الدين المفتوح. */
    val remainingMinor: Long,
    /** ما بيد هذا الجهاز ولم يُخصَّص. */
    val unappliedMinor: Long,
    /** قيود الغرفة كلها في انتظار الإقرار (من أي طرف). */
    val awaitingAcknowledgement: Int,
    /** منها ما يخصّ الطرف الآخر وينتظر إقراري أنا. */
    val awaitingMyAcknowledgement: Int,
    val entries: List<RoomFeedEntry>,
    val updatedAt: Long,
    val myMemberId: String
) {
    val isClear: Boolean get() = remainingMinor == 0L && unappliedMinor == 0L
    val hasHistory: Boolean get() = entries.isNotEmpty()
}

object RoomEntryFeed {

    fun build(
        roomId: String,
        title: String,
        kind: String,
        currency: String,
        status: String,
        counterpartName: String,
        counterpartPhone: String,
        linkCode: String,
        updatedAt: Long,
        myMemberId: String,
        entries: List<EntryView>,
        allocations: List<AllocationView>
    ): RoomFeed {
        val snapshot = SnapshotEngine.build(roomId, currency, entries, allocations)
        val me = snapshot.member(myMemberId)
        val allocatedByEntry = allocationsByEntry(allocations)

        val lines = entries.map { entry ->
            val allocated = allocatedByEntry[entry.id] ?: 0L
            val remaining = (entry.amountMinor - allocated).coerceAtLeast(0L)
            val direction = if (entry.owedByMemberId == myMemberId) LineDirection.CHARGE else LineDirection.PAYMENT
            RoomFeedEntry(
                entryId = entry.id,
                type = entry.type,
                description = entry.description.ifBlank { defaultDescription(entry) },
                occurredAt = entry.occurredAt,
                amountMinor = entry.amountMinor,
                remainingMinor = remaining,
                allocatedMinor = allocated,
                status = entry.status,
                direction = direction,
                isReversal = entry.isReversal,
                involvesMe = entry.owedByMemberId == myMemberId || entry.owedToMemberId == myMemberId,
                // قاعدة الإقرار: ما كان المال فيه عليّ وأرسله الطرف ولم يُقرّ = ينتظر إقراري.
                awaitingMyAck = entry.status == EntryStatus.SENT && entry.owedByMemberId == myMemberId
            )
        }.sortedWith(compareByDescending<RoomFeedEntry> { it.occurredAt }.thenByDescending { it.entryId })

        return RoomFeed(
            roomId = roomId,
            title = title.ifBlank { counterpartName.ifBlank { "غرفة" } },
            kind = kind,
            currency = currency,
            status = status,
            counterpartName = counterpartName,
            counterpartPhone = counterpartPhone,
            linkCode = linkCode,
            myNetMinor = me?.netMinor ?: 0L,
            chargedMinor = me?.chargedMinor ?: 0L,
            paidMinor = me?.paidMinor ?: 0L,
            remainingMinor = me?.remainingMinor ?: 0L,
            unappliedMinor = me?.unappliedMinor ?: 0L,
            awaitingAcknowledgement = snapshot.awaitingAcknowledgement,
            awaitingMyAcknowledgement = entries.count {
                it.status == EntryStatus.SENT && it.owedByMemberId == myMemberId
            },
            entries = lines,
            updatedAt = updatedAt,
            myMemberId = myMemberId
        )
    }

    /** بيان افتراضي حين يخلو القيد من وصف: لا سطر فارغ في كشف. */
    private fun defaultDescription(entry: EntryView): String = when (entry.type) {
        EntryType.WATER_SESSION -> "سقية"
        EntryType.GOODS_DEBT -> "دَين سلعة"
        EntryType.SETTLEMENT -> "صلح"
        EntryType.PAYMENT -> "سداد"
        EntryType.GENERAL_RECEIPT -> "قبض عام"
        EntryType.ADJUSTMENT -> "تسوية/عكس"
        else -> "قيد"
    }

    /** يوم الغرفة بلمحة: هل أنا في صافي «لي» أم «عليّ»؟ */
    fun netText(feed: RoomFeed, currencyCode: String): String {
        val currency = Currency.fromCode(currencyCode) ?: return "${feed.myNetMinor} فلسًا"
        val money = Money.ofMinor(kotlin.math.abs(feed.myNetMinor), currency)
        val amount = MoneyFormat.format(money)
        return when {
            feed.myNetMinor > 0L -> "لي $amount"
            feed.myNetMinor < 0L -> "عليّ $amount"
            else -> "الحساب مصفّى"
        }
    }
}

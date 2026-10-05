package com.baynana.features.sync

import com.baynana.data.local.ledger.Acknowledgement
import com.baynana.data.local.ledger.LedgerEntry
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.OutboxItem
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.OutboxState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * حالة كل حركة في الدفتر: **حقيقة واحدة مكتوبة بالعربية، بلا نجاح كاذب**.
 *
 * القاعدة التي بُني عليها الملفّ: لا نقول «تمّ» إلا إذا كان في صندوق الصادر ما يثبته، ولا نقول
 * «فشل» بلا سبب مكتوب، وكل فشل دائم يُخرجه للمستخدم **مع الحل**، لا مع رقم خطأ.
 *
 * الملفّ نقيّ بلا قاعدة بيانات ولا Compose: يدخل [Snapshot] ويخرج [List] من [Row]، فيُختبر في
 * اختبار وحدة عادي. الشاشة ترسم ولا تقرّر، والقرار هنا.
 */
object SyncStatusModel {

    /** حالات العرض. الترتيب من الأطمئن إلى الأشدّ — يُستعمل في الترتيب والعدّ. */
    enum class Kind {
        /** محفوظ في دفترك ولم يخرج بعد (مسودة أو في الطابور). */
        LOCAL,

        /** خرج من الجهاز، والقيد لم يُقرّ بعد. */
        SENT,

        /** أقرّه الطرف: الحالة تغيّرت ولا شيء مُسح. */
        ACKNOWLEDGED,

        /** مشكلة قابلة للحل: إعادة محاولة تلقائية أو اعتراض يحتاج تصحيحًا. */
        PROBLEM,

        /** فشل دائم يحتاج يدًا: لا إعادة صامتة. */
        DEAD,

        /** أُلغي بقيد عكسي: أثر معلن لا حذف. */
        VOIDED
    }

    /** ما تُقرأ منه الشاشة مرة واحدة. */
    data class Snapshot(
        val rooms: List<LedgerRoom> = emptyList(),
        val entries: List<LedgerEntry> = emptyList(),
        val outbox: List<OutboxItem> = emptyList(),
        val acknowledgements: List<Acknowledgement> = emptyList(),
        val myMemberships: List<RoomMember> = emptyList()
    )

    /** صفّ واحد: حركة واحدة، وحالتها، وما العمل. */
    data class Row(
        val entryId: String,
        val roomTitle: String,
        val title: String,
        /** كلمة الشارة: تُكتب بدقّة لأن الحالة قد تكون «وارد من الطرف» لا «محفوظ». */
        val chip: String,
        val detail: String,
        val state: Kind,
        val occurredAt: Long,
        val dateText: String,
        /** العملية التي يُعاد إرسالها بزرّ «أعد المحاولة» — فارغة إن لم يكن هناك ما يُعاد. */
        val retryOperationId: String = ""
    )

    /** ما تعرضه الشاشة: صفوف + عدّاد مكتوب بالعربية. */
    data class View(
        val rows: List<Row> = emptyList(),
        val headline: String = ""
    )

    fun build(snapshot: Snapshot, dateFormat: SimpleDateFormat = defaultDateFormat()): View {
        val myIds = snapshot.myMemberships.map { it.roomId to it.memberId }.toMap()
        val outboxByEntry = snapshot.outbox
            .filter { it.entityId.isNotBlank() }
            .groupBy { it.entityId }
            .mapValues { (_, items) -> items.maxByOrNull { it.createdAt }!! }
        val ackByEntry = snapshot.acknowledgements.groupBy { it.entryId }

        val rows = snapshot.entries
            .sortedWith(compareByDescending<LedgerEntry> { it.occurredAt }.thenByDescending { it.id })
            .map { entry ->
                val room = snapshot.rooms.firstOrNull { it.id == entry.roomId }
                val mine = myIds[entry.roomId]
                val all = ackByEntry[entry.id].orEmpty()
                val others = all.filter { mine == null || it.memberId != mine }.maxByOrNull { it.decidedAt }
                val myDecision = all.firstOrNull { mine != null && it.memberId == mine }
                row(
                    entry = entry,
                    room = room,
                    pending = outboxByEntry[entry.id],
                    otherPartyDecision = others,
                    myDecision = myDecision,
                    isMine = entry.createdByMemberId.isBlank() || mine == null ||
                        entry.createdByMemberId == mine,
                    dateText = dateFormat.format(Date(entry.occurredAt))
                )
            }

        return View(rows = rows, headline = headline(rows, snapshot))
    }

    private fun row(
        entry: LedgerEntry,
        room: LedgerRoom?,
        pending: OutboxItem?,
        otherPartyDecision: Acknowledgement?,
        myDecision: Acknowledgement?,
        /** هل هذا القيد كتبه صاحب الجهاز؟ (الوارد من الطرف لا يُرسَل من هنا ولا يُنتظر له إرسال.) */
        isMine: Boolean,
        dateText: String
    ): Row {
        val roomTitle = room?.title?.takeIf { it.isNotBlank() } ?: "غرفة"
        val title = plainTitle(entry)

        // ١) الفشل أولًا: هو ما يحتاج المستخدم أن يراه فورًا، ولو كان القيد قديمًا.
        if (pending?.state == OutboxState.DEAD) {
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                chip = CHIP_DEAD,
                detail = "فشل دائم: ${reason(pending)} — الحل: أعد المحاولة من هنا، وإن تكرّر فراجع " +
                    "رقم الطرف أو اتصالك ثم أعدها.",
                state = Kind.DEAD,
                occurredAt = entry.occurredAt,
                dateText = dateText,
                retryOperationId = pending.operationId
            )
        }
        if (pending?.state == OutboxState.FAILED) {
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                chip = CHIP_PROBLEM,
                detail = "تعذّر الإرسال: ${reason(pending)} — ستُعاد المحاولة تلقائيًا، وقيدك محفوظ كما هو.",
                state = Kind.PROBLEM,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٢) الإلغاء بحالة معلنة: لا حذف ولا صمت.
        if (entry.status == EntryStatus.VOIDED) {
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                chip = CHIP_VOIDED,
                detail = "أُلغي بقيد عكسي، والأثر باقٍ عند الطرفين. لا يُحذف قيد شارك فيه غيرك.",
                state = Kind.VOIDED,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٣) اعتراض أو طلب تعديل: يُعرض بنصّ صاحبه.
        if (entry.status == EntryStatus.DISPUTED || entry.status == EntryStatus.CHANGE_REQUESTED) {
            val byOther = otherPartyDecision?.decision == entry.status
            val what = when {
                entry.status == EntryStatus.DISPUTED && byOther -> "اعترض الطرف"
                entry.status == EntryStatus.DISPUTED -> "اعترضتَ أنت"
                byOther -> "طلب الطرف تعديلًا"
                else -> "طلبتَ تعديلًا"
            }
            val note = (if (byOther) otherPartyDecision else myDecision)?.note?.takeIf { it.isNotBlank() }
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                chip = CHIP_PROBLEM,
                detail = if (note != null) {
                    "$what: «$note» — القيد باقٍ كما هو حتى يُصحَّح بقيد جديد."
                } else {
                    "$what، والقيد باقٍ كما هو حتى يُصحَّح بقيد جديد."
                },
                state = Kind.PROBLEM,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٤) أُقرّ: تغيّرت الحالة فقط.
        if (entry.status == EntryStatus.ACKNOWLEDGED) {
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                chip = CHIP_ACKNOWLEDGED,
                detail = if (isMine) {
                    "أقرّه الطرف: الحالة تغيّرت، ولم يُمسح القيد ولا تغيّر مبلغه."
                } else {
                    "أقررتَه أنت: الحالة تغيّرت، ولم يُمسح القيد ولا تغيّر مبلغه."
                },
                state = Kind.ACKNOWLEDGED,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٥) وارد من الطرف: مكانه دفترك، ولا يُرسَل من هنا أصلًا.
        if (!isMine) {
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                chip = CHIP_RECEIVED,
                detail = "وصل من الطرف إلى دفترك، وينتظر إقرارك أنت — لم يُرسل شيء من جهازك ولا شيء ناقص.",
                state = Kind.SENT,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٦) خرج من الجهاز فعلًا؟ صندوق الصادر وحده يقول ذلك، لا حالة القيد المحلية.
        if (pending?.state == OutboxState.SENT) {
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                chip = CHIP_SENT,
                detail = "أُرسل — في انتظار إقرار الطرف.",
                state = Kind.SENT,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٧) الباقي: محفوظ في دفترك ولم يخرج بعد (مسودة أو في الطابور).
        return Row(
            entryId = entry.id,
            roomTitle = roomTitle,
            title = title,
            chip = CHIP_LOCAL,
            detail = if (entry.status == EntryStatus.DRAFT) {
                "مسودة محلية لم تُرسل بعد: تظهر لك وحدك حتى تُرسلها."
            } else {
                "محفوظ في دفترك على هذا الجهاز، وسيُرسل عند عودة الاتصال."
            },
            state = Kind.LOCAL,
            occurredAt = entry.occurredAt,
            dateText = dateText
        )
    }

    /** عنوان مقروء لسطر: نوع القيد بلغة الناس، ثم وصفه إن كتبه صاحبه. */
    fun plainTitle(entry: LedgerEntry): String {
        val kind = when (entry.type) {
            EntryType.WATER_SESSION -> "سقية"
            EntryType.GOODS_DEBT -> "دَين سلعة"
            EntryType.SETTLEMENT -> "صلح"
            EntryType.PAYMENT -> "سداد"
            EntryType.GENERAL_RECEIPT -> "قبض عام"
            EntryType.ADJUSTMENT -> "تسوية"
            else -> entry.type
        }
        val description = entry.description.trim()
        return if (description.isBlank()) kind else "$kind: $description"
    }

    /** العدّاد المكتوب: يقول للمستخدم في سطر واحد أين يقف دفتره. */
    fun headline(rows: List<Row>, snapshot: Snapshot): String {
        if (rows.isEmpty()) return "لا حركة في دفترك بعد."
        val local = rows.count { it.state == Kind.LOCAL }
        val sent = rows.count { it.state == Kind.SENT || it.state == Kind.ACKNOWLEDGED }
        val problem = rows.count { it.state == Kind.PROBLEM }
        val dead = rows.count { it.state == Kind.DEAD }
        val parts = mutableListOf("${rows.size} حركة")
        if (local > 0) parts += "$local محفوظة محليًا"
        if (sent > 0) parts += "$sent مُرسلة أو مُقَرّة"
        if (problem > 0) parts += "$problem تحتاج نظرك"
        if (dead > 0) parts += "$dead فشل دائم"
        val pendingOutbox = snapshot.outbox.count { it.state == OutboxState.PENDING }
        if (pendingOutbox > 0) parts += "$pendingOutbox في الطابور"
        return parts.joinToString(" • ")
    }

    /** نصّ الشريط العلوي وقوّته (تحذير أو طمأنة). */
    data class ChannelBadge(val text: String, val isWarning: Boolean = false)

    /**
     * شريط الحالة: هل نُرسل أصلًا؟
     *
     * كان الشريط يقول «سيُرسل ما في الطابور تلقائيًا» حتى في نسخة **بلا قناة مضبوطة**، وهذا أخطر
     * من خطأ شكلي: يعني أن صاحب الدفتر يظن أن قيوده وصلت شريكه وهي على جهازه وحده. فعبارة الإرسال
     * لا تُقال إلا لقناة قائمة ([configured] = true)، و`null` (لا نعرف) تُعامل كغير مضبوطة في النصّ
     * التحذيري — لا كإرسال مؤكَّد.
     */
    fun channelBadge(online: Boolean, configured: Boolean?): ChannelBadge = when {
        configured != true -> ChannelBadge(
            text = "لم تُضبط قناة مزامنة بعد: كل ما تكتبه محفوظ في جهازك، ولا يُرسل إلى أي جهة",
            isWarning = true
        )
        !online -> ChannelBadge(
            text = "أنت بلا اتصال الآن: كل ما في هذه الشاشة محفوظ في جهازك، وسيُرسل الجديد عند عودة الشبكة",
            isWarning = true
        )
        else -> ChannelBadge("متّصل — وسيُرسل ما في الطابور تلقائيًا")
    }

    /** سبب الفشل بنصّه، وإن لم يُسجَّل سبب نقول ذلك ولا نخترع واحدًا. */
    private fun reason(item: OutboxItem): String =
        item.lastError.trim().ifBlank { "بلا سبب مسجّل من الشبكة" }

    const val CHIP_LOCAL = "محفوظ محليًا"
    const val CHIP_SENT = "أُرسل — بانتظار الإقرار"
    const val CHIP_ACKNOWLEDGED = "مُقرّ"
    const val CHIP_PROBLEM = "يحتاج نظرك"
    const val CHIP_DEAD = "فشل دائم"
    const val CHIP_VOIDED = "ملغى بقيد عكسي"
    const val CHIP_RECEIVED = "وارد من الطرف"

    fun defaultDateFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

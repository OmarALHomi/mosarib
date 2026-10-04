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
                val others = ackByEntry[entry.id].orEmpty()
                    .filter { mine == null || it.memberId != mine }
                    .maxByOrNull { it.decidedAt }
                row(
                    entry = entry,
                    room = room,
                    pending = outboxByEntry[entry.id],
                    otherPartyDecision = others,
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
                detail = "أُلغي بقيد عكسي، والأثر باقٍ عند الطرفين. لا يُحذف قيد شارك فيه غيرك.",
                state = Kind.VOIDED,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٣) اعتراض أو طلب تعديل: يُعرض بنصّ صاحبه.
        if (entry.status == EntryStatus.DISPUTED || entry.status == EntryStatus.CHANGE_REQUESTED) {
            val what = if (entry.status == EntryStatus.DISPUTED) "اعترض الطرف" else "طلب الطرف تعديلًا"
            val note = otherPartyDecision?.note?.takeIf { it.isNotBlank() }
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
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
                detail = "أقرّه الطرف: الحالة تغيّرت، ولم يُمسح القيد ولا تغيّر مبلغه.",
                state = Kind.ACKNOWLEDGED,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٥) خرج من الجهاز وينتظر.
        if (pending?.state == OutboxState.SENT || entry.status == EntryStatus.SENT) {
            return Row(
                entryId = entry.id,
                roomTitle = roomTitle,
                title = title,
                detail = "أُرسل — في انتظار إقرار الطرف.",
                state = Kind.SENT,
                occurredAt = entry.occurredAt,
                dateText = dateText
            )
        }

        // ٦) الباقي: محفوظ في دفترك ولم يخرج بعد.
        return Row(
            entryId = entry.id,
            roomTitle = roomTitle,
            title = title,
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

    /** سبب الفشل بنصّه، وإن لم يُسجَّل سبب نقول ذلك ولا نخترع واحدًا. */
    private fun reason(item: OutboxItem): String =
        item.lastError.trim().ifBlank { "بلا سبب مسجّل من الشبكة" }

    fun defaultDateFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
}

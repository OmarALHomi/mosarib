package com.baynana.data.local.ledger

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import com.baynana.domain.ledger.EntryStatus
import com.baynana.domain.ledger.OutboxState
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus

/**
 * نموذج الغرفة والقيد والإقرار (ح٣ من خطة البناء v6).
 *
 * المبادئ المثبّتة في هذا المخطط:
 * - **المال بالوحدة الصغرى** نصًّا لا رقمًا عشريًا: `amountMinor: Long` + `currency` (ADR-04).
 * - **معرّف العملية ثابت** (`operationId` بفهرس فريد): إعادة الإرسال لا تُنشئ قيدًا ثانيًا.
 * - **الإقرار طبقة فوق القيد** لا بديلًا عنه: الرفض أو طلب التعديل يبقيان القيد ويغيّران حالته.
 * - **لا حذف متسلسل لدفتر**: علاقة الغرفة بالقيود `RESTRICT`، فلا يُمحى تاريخ بطرف واحد.
 * - المفردات (`RoomKind`/`EntryType`/`EntryStatus`...) تعيش في `domain/ledger` لأن المحرّك
 *   المحاسبي يحتاجها، وهنا نستوردها فقط فلا توجد قائمتان تختلفان يومًا.
 */

/**
 * غرفة مشتركة بين طرفين أو أكثر. الغرفة تُنشأ محليًا بحالة [RoomStatus.PENDING]، ولا يعرض
 * فيها الطرف الآخر أي قيد قبل قبول الربط.
 */
@Entity(
    tableName = "rooms",
    indices = [
        Index("status"),
        Index("updatedAt"),
        Index("linkCode", unique = true)
    ]
)
data class LedgerRoom(
    @androidx.room.PrimaryKey val id: String,
    val kind: String = RoomKind.GENERAL,
    /** رمز العملة: كل غرفة بعملة واحدة، ولا تُخلط عملتان في غرفة. */
    val currency: String,
    val title: String = "",
    val status: String = RoomStatus.PENDING,
    /** كود الدعوة أو رمز الربط. فريد كي لا يلتبس طرفان. */
    val linkCode: String = "",
    /** اسم الطرف الآخر كما أدخله صاحب الجهاز (تخزين محلي للعرض). */
    val counterpartName: String = "",
    val counterpartPhone: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long? = null
)

/** عضو في الغرفة. [isMe] يعني صاحب هذا الجهاز (سجل محلي واحد لكل جهاز في الغرفة). */
@Entity(
    tableName = "room_members",
    primaryKeys = ["roomId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = LedgerRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("roomId"), Index("memberId")]
)
data class RoomMember(
    val roomId: String,
    val memberId: String,
    val displayName: String = "",
    val phone: String = "",
    val role: String = "",
    val isMe: Boolean = false,
    val joinedAt: Long,
    val lastSeenAt: Long = 0L
)

/**
 * القيد: سقية أو دين سلعة أو صلح أو سداد أو قبض عام أو تسوية.
 *
 * - الطرفان صريحان: [owedByMemberId] من عليه، [owedToMemberId] من له. لا تُفهم العلاقة من النص.
 * - [amountMinor] بالوحدة الصغرى (فلس)، و[currency] نسخة تاريخية من عملة الغرفة وقت القيد.
 * - [operationId] فريد: يمنع أي تكرار عند إعادة الإرسال أو إعادة التشغيل.
 * - [sourceTable]/[sourceId] يربطان قيود الإرث (جلسات وسندات قديمة) عند الترحيل (ح٧).
 * - الطرفان مقيّدان بعضوية الغرفة نفسها: الكتابة السلكية (ح٦) يجب أن تكتب عضوَي الغرفة قبل
 *   قيودها، وهذا ترتيب مطلوب لا عيب. الحذف RESTRICT فلا يُمحى عضو له تاريخ.
 */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = LedgerRoom::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        // طرفا القيد عضوين في **نفس** الغرفة: لا قيد على غريب، ولا خلط بين غرفتين.
        ForeignKey(
            entity = RoomMember::class,
            parentColumns = ["roomId", "memberId"],
            childColumns = ["roomId", "owedByMemberId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = RoomMember::class,
            parentColumns = ["roomId", "memberId"],
            childColumns = ["roomId", "owedToMemberId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        // يغطي الفهرسان أيضًا الاستعلام بالغرفة (roomId هو العمود الأول).
        Index("roomId", "owedByMemberId"),
        Index("roomId", "owedToMemberId"),
        Index(value = ["operationId"], unique = true),
        // قيد عكسي واحد لكل قيد: الفهرس الفريد يمنع الإلغاء المزدوج بنيويًا (والفراغات متعددة).
        Index(value = ["reversesEntryId"], unique = true),
        Index("status"),
        Index("occurredAt"),
        Index("sourceTable", "sourceId")
    ]
)
data class LedgerEntry(
    @androidx.room.PrimaryKey val id: String,
    val roomId: String,
    val operationId: String,
    val type: String,
    val owedByMemberId: String,
    val owedToMemberId: String,
    val amountMinor: Long,
    val currency: String,
    val occurredAt: Long,
    val description: String = "",
    val quantityNote: String = "",
    val status: String = EntryStatus.DRAFT,
    val createdByMemberId: String = "",
    val sourceTable: String? = null,
    val sourceId: String? = null,
    /** يُمنع البيع المزدوج: أول صلح يثبّت العرض المحجوز. */
    val listingId: String? = null,
    /**
     * القيد العكسي يشير إلى القيد الذي ألغاه. لا حذف لقيد شارك فيه طرف آخر: يُلغى بقيد عكسي
     * ظاهر للطرفين، والفهرس الفريد أعلاه يضمن ألا يُلغى القيد مرتين.
     */
    val reversesEntryId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * إسقاط سداد على قيود محددة: أي دفعة سدّدت أي دين وبكم. لا يجوز أن يتجاوز مجموع الإسقاطات
 * للسداد مبلغه، ولا أن يتجاوز المسدد على دين قيمته.
 *
 * **المفتاح طبيعي** `(paymentEntryId, debtEntryId)` لا مُعرّف مصطنع: إعادة الإسقاط لنفس الزوج
 * تُحدّث المبلغ في مكانه بدل أن تضيف صفًا متعارضًا (خطأ صامت عند استخدام معرّف جديد).
 */
@Entity(
    tableName = "entry_allocations",
    primaryKeys = ["paymentEntryId", "debtEntryId"],
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntry::class,
            parentColumns = ["id"],
            childColumns = ["paymentEntryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        ),
        ForeignKey(
            entity = LedgerEntry::class,
            parentColumns = ["id"],
            childColumns = ["debtEntryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("paymentEntryId"),
        Index("debtEntryId")
    ]
)
data class EntryAllocation(
    val paymentEntryId: String,
    val debtEntryId: String,
    val amountMinor: Long,
    val currency: String,
    val createdAt: Long
)

/**
 * إقرار عضو على قيد. قرار واحد لكل عضو لكل قيد: إعادة الإقرار تُحدّث القرار ولا تُنشئ سجلًا
 * ثانيًا. قبول الطرف يغيّر حالة القيد فقط، ولا يمسحه ولا يجمّد دفتر كاتبه.
 *
 * **المفتاح طبيعي** `(entryId, memberId)`: لا يمكن بنيويًا وجود قرارين متعارضين لنفس العضو،
 * ولا يمكن أن يكتب نداء إقرار بمعرّف جديد صفًا يتجاهله `@Upsert` صامتًا.
 */
@Entity(
    tableName = "acknowledgements",
    primaryKeys = ["entryId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = LedgerEntry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index("entryId"),
        Index("memberId"),
        Index("decidedAt")
    ]
)
data class Acknowledgement(
    val entryId: String,
    val memberId: String,
    val decision: String,
    val note: String = "",
    val decidedAt: Long,
    val createdAt: Long
)

/**
 * صندوق الصادر: كل تغيير مشترك يُسجَّل هنا في نفس معاملة حفظ القيد، ثم يرسله العامل.
 * [payload] نصّي، والمبالغ فيه بالوحدة الصغرى نصًّا (ADR-04).
 */
@Entity(
    tableName = "outbox",
    indices = [Index("state"), Index("createdAt"), Index("entityType", "entityId")]
)
data class OutboxItem(
    @androidx.room.PrimaryKey val operationId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payload: String,
    val state: String = OutboxState.PENDING,
    val attempts: Int = 0,
    val lastError: String = "",
    val createdAt: Long,
    val updatedAt: Long
)

/** مؤشر المزامنة لكل مجموعة أو غرفة: يمنع إعادة تنزيل التاريخ كاملًا. */
@Entity(tableName = "sync_state")
data class SyncState(
    @androidx.room.PrimaryKey val key: String,
    val cursor: String = "",
    val lastSyncAt: Long = 0L,
    val lastError: String = ""
)

/** قيد مع إقراراته وإسقاطاته، للقراءة والعرض في شاشة واحدة. */
data class EntryWithDetails(
    @Embedded val entry: LedgerEntry,
    @Relation(parentColumn = "id", entityColumn = "entryId")
    val acknowledgements: List<Acknowledgement>,
    @Relation(parentColumn = "id", entityColumn = "paymentEntryId")
    val allocations: List<EntryAllocation>
)

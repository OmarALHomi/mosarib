package com.baynana.data.local.handover

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ح١٩: سجلّ التسليم اليدوي — كل حزمة خرجت من هذا الجهاز أو دخلت إليه، بنتيجتها.
 *
 * **فائدة السجلّ ليست تجميلية**: هو ما يجعل الاستيراد **ممنوعًا مرتين** (فهرس فريد على
 * `bundleId + direction`)، وهو ما يجيب على السؤال العملي عند الخلاف: «ماذا أرسلتَ لي ومتى؟».
 * ولذلك يُكتب في نفس المعاملة التي تُطبَّق فيها الحزمة، فلا توجد حزمة مطبَّقة بلا صفّ سجلّ.
 */
@Entity(
    tableName = "handover_log",
    indices = [
        Index(value = ["bundleId", "direction"], unique = true),
        Index("createdAt"),
        Index("direction")
    ]
)
data class HandoverLogRow(
    @PrimaryKey val id: String,
    val bundleId: String,
    val direction: String,
    /** الغرف التي تغطيها الحزمة (معرّفات مفصولة بفواصل) — للعرض والتتبّع. */
    val rooms: String,
    /** رمز جهاز الطرف الآخر كما حملته الحزمة (إعلان لا تصريح: لا يُبنى عليه أي صلاحية). */
    val peerDevice: String,
    val createdAt: Long,
    val items: Int,
    val applied: Int,
    val duplicates: Int,
    val rejected: Int,
    /** دعوات غرف وصلت ولم تُقبل بعد (لا تدخل إلا بقبول بشري). */
    val invited: Int = 0,
    val note: String = "",
    val digest: String = ""
)

/**
 * دعوة غرفة واردة **بانتظار قرار بشري**. لا تُنشأ غرفة من حزمة تلقائيًا أبدًا: كود الربط وحده
 * لا يفتح كشفًا ولا دينًا (قاعدة §7)، والغرفة تُفتح بعد قبول الطرفين.
 */
@Entity(
    tableName = "pending_invites",
    indices = [Index("receivedAt"), Index("status"), Index("roomId")]
)
data class PendingInviteRow(
    @PrimaryKey val id: String,
    val roomId: String,
    val title: String,
    val kind: String,
    val currency: String,
    val inviterMemberId: String,
    val inviterName: String,
    /** معرّف عضو هذا الجهاز في الغرفة المدعوّة — بدونه لا يُنسب أي قيد إلى صاحبه. */
    val partnerMemberId: String,
    val payload: String,
    val receivedAt: Long,
    val bundleId: String,
    val status: String = InviteStatus.PENDING,
    val note: String = ""
)

object InviteStatus {
    const val PENDING = "PENDING"
    const val ACCEPTED = "ACCEPTED"
    const val IGNORED = "IGNORED"
}

object HandoverDirection {
    const val OUT = "OUT"
    const val IN = "IN"
}

/**
 * عنصر وصل **قبل أن تُقبل دعوة غرفته**، فيُحفظ هنا ولا يُطبَّق ولا يُرمى.
 *
 * سبب وجوده: حزمة التسليم قد تحمل الدعوة والحركات معًا، وترتيب وصولهما ليس مضمونًا في محادثة
 * واتساب. فبدل أن يُرفض القيد («غرفته غير موجودة») ويضيع، يُنتظر حتى يقبل المستخدم الدعوة،
 * فيُطبَّق تلقائيًا في اللحظة نفسها. وهذه هي الرحلة الكاملة بلا إنترنت ولا حساب: ملف واحد يكفي.
 */
@Entity(
    tableName = "pending_items",
    indices = [Index("roomId"), Index("receivedAt")]
)
data class PendingItemRow(
    @PrimaryKey val operationId: String,
    val roomId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payload: String,
    /** زمن إنشاء العنصر عند مُرسله (أو عندنا إن لم يُعلن) — للترتيب عند التطبيق. */
    val createdAt: Long,
    val receivedAt: Long,
    val bundleId: String
)

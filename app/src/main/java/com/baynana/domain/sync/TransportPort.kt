package com.baynana.domain.sync

/**
 * منفذ النقل: العقد الوحيد بين منطق المزامنة وبين أي وسيلة سلكية (Firestore اليوم، أو خادم خاص
 * في ح٢٢، أو وسيط ملفات/QR في ح١٩). الواجهة **نقية**: لا Android ولا Firebase ولا HTTP هنا.
 *
 * الأصناف الثلاثة للنتيجة مقصودة ومختلفة تمامًا، لأن ردّ الفعل عليها مختلف:
 * - [PushOutcome.Accepted]: وصل وثُبت. يُعلَّم العنصر مُرسلًا.
 * - [PushOutcome.Rejected] بخطأ غير قابل لإعادة المحاولة: رفض حقيقي (صلاحية، تحقق، نسخة غير
 *   مدعومة). يُعلَّم العنصر «ميتًا» بسبب عربي ظاهر ولا يُعاد صامتًا، وبيانات المستخدم تبقى.
 * - [PushOutcome.Rejected] قابل لإعادة المحاولة أو خطأ نقل: يُعلَّم «فشلًا» بموعد إعادة أسّي،
 *   ويُحفظ ترتيب الإرسال فلا تُرسل قيود لاحقة قبل قيد سابق معلّق.
 */
interface TransportPort {

    /**
     * إرسال دفعة بالترتيب. تُرجع نتيجة لكل عنصر بنفس الترتيب. قد ترفع استثناء عند انقطاع الشبكة،
     * وهذا يُعالج كخطأ نقل مؤقت لكل عناصر الدفعة.
     */
    suspend fun push(envelopes: List<OutboxEnvelope>): List<PushOutcome>

    /**
     * سحب التغييرات بعد [cursor] (أو من البداية إن كان null). الدلتا تعتمد زمن الخادم وcursor،
     * لا زمن الجهاز المحلي (قاعدة §6.2).
     */
    suspend fun pull(cursor: String?): PullPage
}

/** عنصر صادر جاهز للإرسال: الحمولة نصّ JSON بمبالغ نصّية بالوحدة الصغرى (ADR-04). */
data class OutboxEnvelope(
    val operationId: String,
    val entityType: String,
    val entityId: String,
    val action: String,
    val payload: String,
    val createdAt: Long,
    val attempts: Int
)

sealed interface PushOutcome {
    data object Accepted : PushOutcome

    /** رفض معلن من الطرف الآخر. [retryable] تعني: أعد المحاولة لاحقًا (ضغط، تعارض مؤقت). */
    data class Rejected(val retryable: Boolean, val reason: String) : PushOutcome

    /** خطأ نقل (انقطاع، مهلة): مؤقت دائمًا ولا يعني رفضًا. */
    data class TransportError(val message: String) : PushOutcome
}

/** تغيير قادم من الطرف الآخر. [serverTime] هو مرجع الترتيب لا زمن الجهاز. */
data class RemoteChange(
    val kind: String,
    val entityType: String,
    val entityId: String,
    val operationId: String,
    val payload: String,
    val serverTime: Long
) {
    companion object {
        const val UPSERT = "UPSERT"
        const val VOID = "VOID"
        const val DELETE = "DELETE"
    }
}

/** صفحة سحب: تغييرات + مؤشر الصفحة التالية. المؤشر لا يُحفظ إلا بعد تطبيق الصفحة كلها. */
data class PullPage(
    val changes: List<RemoteChange>,
    val nextCursor: String,
    val hasMore: Boolean = false
)

/** نتيجة تطبيق تغيير قادم على القاعدة المحلية. */
enum class ApplyOutcome {
    /** طُبّق الآن. */
    APPLIED,

    /** موجود سابقًا (نفس operationId): لم يُكتب شيء — إعادة تسليم لا تُضاعف. */
    DUPLICATE,

    /** مُنع بحجر قبر: كيان مُغلق/محذوف عن بعد لا يعود من ذاكرة قديمة. */
    SKIPPED_TOMBSTONED,

    /** نوع أو صيغة لا نفهمها: تُسجَّل للمراجعة ولا تُفشل السحب كله. */
    UNSUPPORTED
}

/** ما يجري داخل جولة مزامنة واحدة، بالعربية، ليُعرض للمستخدم بلا ترجمة. */
data class SyncReport(
    val pushed: Int = 0,
    val accepted: Int = 0,
    val deferred: Int = 0,
    val failed: Int = 0,
    val dead: Int = 0,
    val pulled: Int = 0,
    val applied: Int = 0,
    val duplicates: Int = 0,
    val tombstoned: Int = 0,
    val unsupported: Int = 0,
    val cursor: String = "",
    val stoppedForRetry: Boolean = false,
    val errors: List<String> = emptyList()
) {
    val hasWork: Boolean get() = pushed > 0 || pulled > 0

    /** ملخّص عربي لسطر واحد يُعرض في شاشة الحالة. */
    fun summaryText(): String = buildString {
        if (accepted > 0) append("أُرسل: $accepted")
        if (failed > 0) { if (isNotEmpty()) append(" • "); append("بانتظار إعادة المحاولة: $failed") }
        if (dead > 0) { if (isNotEmpty()) append(" • "); append("يحتاج تدخلًا: $dead") }
        if (applied > 0) { if (isNotEmpty()) append(" • "); append("وصل جديد: $applied") }
        if (errors.isNotEmpty()) { if (isNotEmpty()) append(" • "); append(errors.first()) }
        if (isEmpty()) append("لا جديد")
    }
}

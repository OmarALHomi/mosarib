package com.baynana.domain.settlement

/**
 * مفردات الصلح (ح٨): أطراف، حالات، وسقوف. تعيش في `domain` لأن المحرّك النقي يحتاجها، والطبقات
 * الأخرى تستوردها فقط، فلا توجد قائمتان تختلفان يومًا (نفس قاعدة `domain/ledger`).
 */
object DealPartyRole {
    const val SELLER = "SELLER" // المزارع صاحب المحصول
    const val BUYER = "BUYER"   // المجبري المشتري
    const val BROKER = "BROKER" // الدلال الوسيط

    val all: List<String> = listOf(SELLER, BUYER, BROKER)

    fun label(role: String): String = when (role) {
        SELLER -> "البائع"
        BUYER -> "المشتري"
        BROKER -> "الدلال"
        else -> "طرف"
    }
}

/** حالات الصلح. لا حذف في أي حالة: الإلغاء حالة معلنة بقيد عكسي. */
object DealStatus {
    const val DRAFT = "DRAFT"           // مسودة محلية لم تُرسل
    const val PENDING = "PENDING"       // أُرسل وبانتظار إقرار الطرف الآخر
    const val ACTIVE = "ACTIVE"         // أقرّه الطرفان: الأقساط تجري
    const val COMPLETED = "COMPLETED"   // سُدّد كاملًا (بما فيه السعاية)
    const val CANCELLED = "CANCELLED"   // فُسخ: قيد عكسي معلن، والسجل باقٍ للطرفين

    val all: List<String> = listOf(DRAFT, PENDING, ACTIVE, COMPLETED, CANCELLED)

    /** هل يُقبل القبض على صلح بهذه الحالة؟ */
    fun acceptsPayment(status: String): Boolean = status == PENDING || status == ACTIVE

    fun label(status: String): String = when (status) {
        DRAFT -> "مسودة"
        PENDING -> "بانتظار الإقرار"
        ACTIVE -> "قائم"
        COMPLETED -> "مكتمل"
        CANCELLED -> "مفسوخ"
        else -> "غير معروف"
    }
}

object InstallmentStatus {
    const val SCHEDULED = "SCHEDULED"
    const val PARTIAL = "PARTIAL"
    const val PAID = "PAID"
    const val CANCELLED = "CANCELLED"

    fun isOpen(status: String): Boolean = status == SCHEDULED || status == PARTIAL

    fun label(status: String): String = when (status) {
        SCHEDULED -> "مستحق"
        PARTIAL -> "مدفوع جزئيًا"
        PAID -> "مدفوع"
        CANCELLED -> "ملغى"
        else -> "غير معروف"
    }
}

/** من يدفع السعاية. */
object CommissionPayer {
    const val SELLER = "SELLER"
    const val BUYER = "BUYER"
    const val SPLIT = "SPLIT"

    fun label(payer: String): String = when (payer) {
        SELLER -> "على البائع"
        BUYER -> "على المشتري"
        SPLIT -> "مقسومة بينهما"
        else -> "غير محدّد"
    }
}

/** حالات العرض في السوق، لأن الصلح هو من يحجز العرض. */
object ListingState {
    const val OPEN = "OPEN"
    const val RESERVED = "RESERVED"
    const val SOLD = "SOLD"
    const val WITHDRAWN = "WITHDRAWN"
}

/**
 * سقوف صلبة، لا تُخترق حتى بموافقة الأطراف:
 * - **سقف السعاية**: 20% من قيمة الصلح. سعاية أعلى تعني أن الدلال صار شريكًا فعليًا في المحصول،
 *   وهذا ليس بيعًا ولا وساطة، ويُفسد المقارنة بين الصلوح. الرفض هنا حماية للناس لا تعقيد.
 * - **عدد الأقساط**: 36 قسطًا؛ أكثر من ذلك يعني جدولًا لا يقرؤه أحد ولا يُتابع.
 * - **الفواصل الزمنية**: يوم إلى سنة بين القسط والذي يليه، فلا «قسط يومي» بلا معنى ولا «قسط بعد عشرين سنة».
 */
object DealLimits {
    const val MAX_RATE_BASIS_POINTS = 2_000 // 20%
    const val MAX_INSTALLMENTS = 36
    const val MIN_INTERVAL_DAYS = 1
    const val MAX_INTERVAL_DAYS = 365
    const val MIN_TERM_DAYS = 0
    const val MAX_TERM_DAYS = 3_650
}

package com.baynana.domain.market

/**
 * مفردات السوق (ح٩): حالات العرض والطلب، وطريقة السعر، والوحدات كما يتكلم الناس، وحدود النشر.
 * تعيش في `domain` لأن محرّك الخصوصية والقواعد يحتاجها، ولأن **الوحدات المحلية** جزء من المصطلح
 * لا من الشاشة: «قدح»، «سلة»، «شروة كاملة»، «قنطار» — كلها تُخزَّن برمز ثابت وتُعرض بتسميتها.
 */
object ListingStatus {
    const val DRAFT = "DRAFT"                   // مسودة عند الدلال
    const val PENDING_REVIEW = "PENDING_REVIEW" // بانتظار المصادقة
    const val PUBLISHED = "PUBLISHED"           // ظاهر في السوق
    const val RESERVED = "RESERVED"             // حُجز بصلح قائم (ح٨)
    const val SOLD = "SOLD"                     // بِيع
    const val WITHDRAWN = "WITHDRAWN"           // سحبه صاحبه
    const val REJECTED = "REJECTED"             // رُفض في المصادقة
    const val EXPIRED = "EXPIRED"               // انتهى موسمه

    val all: List<String> = listOf(DRAFT, PENDING_REVIEW, PUBLISHED, RESERVED, SOLD, WITHDRAWN, REJECTED, EXPIRED)

    /** الحالات التي يجوز أن يظهر بها العرض للناس. */
    val visibleToPublic: List<String> = listOf(PUBLISHED, RESERVED)

    fun isVisible(status: String): Boolean = status in visibleToPublic

    fun label(status: String): String = when (status) {
        DRAFT -> "مسودة"
        PENDING_REVIEW -> "بانتظار المصادقة"
        PUBLISHED -> "معروض"
        RESERVED -> "محجوز"
        SOLD -> "تم البيع"
        WITHDRAWN -> "مسحوب"
        REJECTED -> "مرفوض"
        EXPIRED -> "منتهي"
        else -> "غير معروف"
    }
}

/** طريقة السعر: رقم معلن، أو «على السوم» بلا رقم. لا ثالث يُخلط بينهما. */
object PriceMode {
    const val FIXED = "FIXED"         // سعر معلن
    const val ON_OFFER = "ON_OFFER"   // على السوم والمفاوضة

    fun label(mode: String): String = when (mode) {
        FIXED -> "سعر معلن"
        ON_OFFER -> "على السوم"
        else -> "غير محدّد"
    }
}

/** وحدات البيع كما ينطقها السوق اليمني. */
object ListingUnit {
    const val WHOLE_LOT = "WHOLE_LOT" // شروة كاملة
    const val KILO = "KILO"
    const val SACK = "SACK"           // كيس
    const val QUANTAL = "QUANTAL"     // قنطار
    const val TON = "TON"
    const val PIECE = "PIECE"         // حبة
    const val CARTON = "CARTON"
    const val HEAD = "HEAD"           // رأس (مواشي)
    const val LOCAL_CUP = "LOCAL_CUP" // قدح
    const val BASKET = "BASKET"       // سلة

    val all: List<String> = listOf(WHOLE_LOT, KILO, SACK, QUANTAL, TON, PIECE, CARTON, HEAD, LOCAL_CUP, BASKET)

    fun label(unit: String): String = when (unit) {
        WHOLE_LOT -> "شروة كاملة"
        KILO -> "بالكيلو"
        SACK -> "بالكيس"
        QUANTAL -> "بالقنطار"
        TON -> "بالطن"
        PIECE -> "بالحبة"
        CARTON -> "بالكرتون"
        HEAD -> "بالرأس"
        LOCAL_CUP -> "بالقدح"
        BASKET -> "بالسلة"
        else -> "بوحدة غير معروفة"
    }
}

/** حالة طلب التسويق: الطلب **ليس دَينًا**، ولا يُنشر عرض قبل قبول المزارع. */
object MarketingRequestStatus {
    const val REQUESTED = "REQUESTED"
    const val ACCEPTED = "ACCEPTED"
    const val REFUSED = "REFUSED"
    const val WITHDRAWN = "WITHDRAWN"

    fun label(status: String): String = when (status) {
        REQUESTED -> "بانتظار موافقة المزارع"
        ACCEPTED -> "قبله المزارع"
        REFUSED -> "رفضه المزارع"
        WITHDRAWN -> "سحبه المزارع"
        else -> "غير معروف"
    }
}

/** قرار المصادقة على العرض. */
object ModerationDecision {
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
    const val NEEDS_EDIT = "NEEDS_EDIT"

    fun label(decision: String): String = when (decision) {
        APPROVED -> "مصادق عليه"
        REJECTED -> "مرفوض"
        NEEDS_EDIT -> "يحتاج تعديلًا"
        else -> "غير معروف"
    }
}

/** حدود النشر: نصّ قصير يُقرأ في الجوال، وصور محدودة، ووصف لا يتحوّل إلى دفتر ديون. */
object MarketLimits {
    const val MAX_TITLE = 80
    const val MAX_DESCRIPTION = 400
    const val MAX_QUANTITY_NOTE = 60
    const val MAX_PHOTOS = 6
    const val MAX_BROKER_NAME = 60
}

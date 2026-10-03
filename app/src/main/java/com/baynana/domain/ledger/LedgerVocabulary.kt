package com.baynana.domain.ledger

/**
 * مفردات الدفتر: كل الرموز النصية التي تُخزَّن في القاعدة وتُرسل على السلك.
 *
 * تعيش في طبقة `domain` لأنها لغة مشتركة بين المحرّك المحاسبي (FIFO، الأرصدة، القيود العكسية)
 * وبين طبقة البيانات والنقل. القيم **ثابتة لا تتغير** بعد النشر: تغيير أي رمز هنا يعني ترحيلًا
 * لملايين الصفوف، فالإضافة مسموحة والحذف أو التعديل ممنوع.
 *
 * اصطلاح الطرفين (مهم، ويُفسَّر به كل شيء لاحقًا):
 * - كل قيد يحمل طرفًا عليه `owedByMemberId` وطرفًا له `owedToMemberId`، والمبلغ ينتقل **من
 *   الطرف الذي عليه إلى الطرف الذي له** بمعنى: الطرف الذي عليه يخسر `amountMinor` من رصيده،
 *   والذي له يكسبه.
 * - قيد الدين (سقية/دين سلعة/صلح): المزارع عليه والمسرب له.
 * - قيد السداد/القبض **مرآة الدين**: الذي استلم المال صار عليه (لأنه يحمل مال الآخر)، والذي
 *   دفع صار له. فالسداد يقابل السقية ويتقاصّان حسابيًا بلا أي سالب.
 */

/** أنواع الغرف: كل غرفة قالب واحد وعملة واحدة، ولا تُخلط أرصدة قالبين. */
object RoomKind {
    const val GENERAL = "GENERAL"       // مستخدم بلا قالب (دين قريب أو بيت)
    const val WATER = "WATER"           // مسرب ومزارع
    const val SHOP = "SHOP"             // بقالة وزبون
    const val MARKET = "MARKET"         // دلال ومجبري ومزارع

    val all = listOf(GENERAL, WATER, SHOP, MARKET)
}

/** دورة حياة الغرفة. لا تُفتح إلا بعد قبول الطرفين. */
object RoomStatus {
    const val PENDING = "PENDING"       // دُعيت ولم تُقبل بعد: لا يظهر فيها قيد للطرف الآخر
    const val ACTIVE = "ACTIVE"         // قبل الطرفان: الكتابة والإقرار مفعّلان
    const val CLOSED = "CLOSED"         // أُغلقت بتراضي الطرفين: قراءة فقط
    const val REJECTED = "REJECTED"     // رُفضت الدعوة

    val all = listOf(PENDING, ACTIVE, CLOSED, REJECTED)
}

/** أنواع القيود. كل قيد بيان مستقل حتى لو سُدّد لاحقًا. */
object EntryType {
    const val WATER_SESSION = "WATER_SESSION"     // سقية: ساعات/مصدر ماء/سعر ساعة
    const val GOODS_DEBT = "GOODS_DEBT"           // دين سلعة (بقالة)
    const val SETTLEMENT = "SETTLEMENT"           // صلح ثمرة
    const val PAYMENT = "PAYMENT"                 // سداد (مربوط أو عام)
    const val GENERAL_RECEIPT = "GENERAL_RECEIPT" // قبض عام: لا يُغلق أي دين
    const val ADJUSTMENT = "ADJUSTMENT"           // تسوية/قيد عكسي

    val all = listOf(WATER_SESSION, GOODS_DEBT, SETTLEMENT, PAYMENT, GENERAL_RECEIPT, ADJUSTMENT)

    /** أنواع تزيد ما على الطرف: السقية ودين السلعة والصلح. */
    val debts = listOf(WATER_SESSION, GOODS_DEBT, SETTLEMENT)

    /** أنواع تُنقص ما على الطرف: السداد والقبض العام. */
    val credits = listOf(PAYMENT, GENERAL_RECEIPT)
}

/** حالة القيد. [VOIDED] تُصل بحالة معلنة لا بحذف. */
object EntryStatus {
    const val DRAFT = "DRAFT"                       // مسودة محلية لم تُرسل
    const val SENT = "SENT"                         // وصل الطرف الآخر ولم يُقرّ
    const val ACKNOWLEDGED = "ACKNOWLEDGED"         // أقرّه الطرف
    const val DISPUTED = "DISPUTED"                 // اعترض عليه الطرف
    const val CHANGE_REQUESTED = "CHANGE_REQUESTED" // طلب تعديلًا
    const val VOIDED = "VOIDED"                     // أُلغي بقيد عكسي

    val all = listOf(DRAFT, SENT, ACKNOWLEDGED, DISPUTED, CHANGE_REQUESTED, VOIDED)

    /**
     * الحالات التي يجوز أن يُخصَّص عليها سداد تلقائيًا (FIFO) أو صراحةً.
     * المسودة ليست مشتركة بعد، والملغى سقط بقيد عكسي، والمعترَض عليه أو المطلوب تعديله لا
     * يُغلق صامتًا: يُحلّ الاعتراض أولًا ثم يُخصَّص.
     */
    val allocatable = listOf(SENT, ACKNOWLEDGED)
}

/** قرار الإقرار لكل عضو في كل قيد. */
object AckDecision {
    const val ACKNOWLEDGED = "ACKNOWLEDGED"
    const val DISPUTED = "DISPUTED"
    const val CHANGE_REQUESTED = "CHANGE_REQUESTED"

    val all = listOf(ACKNOWLEDGED, DISPUTED, CHANGE_REQUESTED)

    /** حالة القيد التي تنتج عن كل قرار. */
    fun resultingStatus(decision: String): String = when (decision) {
        ACKNOWLEDGED -> EntryStatus.ACKNOWLEDGED
        DISPUTED -> EntryStatus.DISPUTED
        CHANGE_REQUESTED -> EntryStatus.CHANGE_REQUESTED
        else -> throw IllegalArgumentException("قرار غير معروف: $decision")
    }
}

/** حالة عنصر صندوق الصادر. [DEAD] تحتاج تدخلًا بشريًا (سبب واضح لا إعادة صامتة). */
object OutboxState {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val FAILED = "FAILED"
    const val DEAD = "DEAD"

    val all = listOf(PENDING, SENT, FAILED, DEAD)
}

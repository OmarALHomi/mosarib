package com.baynana.data.local.license

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * جدولا الترخيص (ح١٣) — وهما **سجل** لا خزانة أسرار:
 *
 * - **`licenses`**: كل استحقاق استُردّ مرة واحدة، بمفتاحه الأساسي `licenseId`. وهذا المفتاح
 *   نفسه هو مانع الـreplay: محاولة إدخال الرمز ثانية تُصطدم بصف موجود، فلا إدراج ولا تمديد.
 *   الصفّ يحمل ما يلزم للمحاسبة (المهنة والخطة والمدة وتاريخ الانتهاء) وبصمة الرمز للتدقيق —
 *   ولا يحمل مفتاحًا خاصًا ولا سرًّا، فنُسخ الاحتياطي لا تكشف ما يُنتحل به.
 * - **`license_events`**: كل محاولة استرداد، نجحت أو رُفضت، بسببها ورسالتها. وهذا ما يجعل
 *   «لماذا رُفض رمزي؟» سؤالًا له جواب على الجهاز نفسه، بلا اتصال.
 *
 * المال لا يمرّ من هنا (الترخيص استحقاق، لا رصيد)، فلا `amountMinor` ولا `currency`: الخلط بين
 * الاستحقاق والمال يفتح بابًا لمحاسبة ثانية، وهذا ما نرفضه من أول المشروع.
 */

@Entity(
    tableName = "licenses",
    indices = [Index("role"), Index("expiresAt"), Index("kind"), Index("grantedAt")]
)
data class LicenseRow(
    /** `signed:<licenseId>` للتصاريح الموقّعة، و`legacy:<بصمة الرمز>` للمفاتيح القديمة. */
    @PrimaryKey val licenseId: String,
    /** `SIGNED` أو `LEGACY` — نُعلن نوع الاستحقاق ولا نُخفيه، لأن الثاني أضعف أمنيًا بطبيعته. */
    val kind: String,
    val deviceCode: String,
    /** اسم المهنة كما في `LicenseManager.LicenseRole.name`. */
    val role: String,
    /** اسم الخطة كما صدرت: MONTHLY/YEARLY/LIFETIME أو خطة موقّعة بمدة مخصّصة. */
    val plan: String,
    val durationDays: Int,
    /** تاريخ إصدار التصريح من المالك. */
    val issuedAt: Long,
    /** نهاية الاستحقاق — للتصاريح الموقّعة هي القيمة الموقّعة نفسها، لا حسابًا محليًا. */
    val expiresAt: Long,
    /** متى استُردّ على هذا الجهاز. */
    val grantedAt: Long,
    /** بصمة SHA-256 للرمز كما كُتب (للتدقيق والدعم، ولا تُستخدم للتحقق). */
    val tokenSha256: String,
    val note: String = ""
)

@Entity(
    tableName = "license_events",
    indices = [Index("licenseId"), Index("occurredAt"), Index("outcome")]
)
data class LicenseEventRow(
    @PrimaryKey val id: String,
    /** المفتاح الذي حُاول استرداده (بنفس صيغة `licenses.licenseId`). */
    val licenseId: String,
    val occurredAt: Long,
    /** `GRANTED` أو `REJECTED`. */
    val outcome: String,
    /** اسم سبب الرفض من `LicenseRejection`، أو فراغ عند القبول. */
    val reason: String,
    val role: String,
    val plan: String,
    val deviceCode: String,
    /** نهاية الاستحقاق المقترحة من الرمز (0 إن كان مشوّهًا). */
    val expiresAt: Long,
    /** الرسالة العربية التي رآها المستخدم — للتدقيق في الشكاوى. */
    val message: String,
    /** أول أحرف من الرمز فقط: تكفي للتتبّع ولا تُعيد طباعة الرمز كاملًا في أي سجل. */
    val tokenPrefix: String,
    /** نهاية الاستحقاق قبل العملية وبعدها، ليكون أثر التمديد ظاهرًا لا مُخمَّنًا. */
    val expiresAtBefore: Long,
    val expiresAtAfter: Long
)

/** ثوابت النوع والنتيجة: نصّ واحد يُكتب في القاعدة ويُقرأ في الشاشة والتقارير. */
object LicenseKinds {
    const val SIGNED = "SIGNED"
    const val LEGACY = "LEGACY"

    const val GRANTED = "GRANTED"
    const val REJECTED = "REJECTED"
}

package com.baynana.data.local.market

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * تخزين السوق (ح٩ جزء ٢): أربعة جداول تفصل **ما يُنشر** عن **ما لا يُنشر أبدًا**.
 *
 * الفصل هنا مقصود وليس تنظيمًا: هاتف المزارع وموقعه الدقيق لا يسكنان صفّ العرض نفسه، بل جدولًا
 * مستقلًا (`market_contacts`). فحتى لو كتب أحدهم استعلامًا مهملًا `SELECT * FROM market_listings`
 * وأرسله إلى شاشة عامة، **لا يوجد في النتيجة رقم هاتف**. هذا هو الفرق بين وعد بالخصوصية وبناء لها.
 */

/** عرض محصول كما يملكه الدلال. لا يحتوي أي رقم تواصل ولا موقعًا دقيقًا. */
@Entity(
    tableName = "market_listings",
    indices = [
        Index("status"),
        Index("brokerMemberId"),
        Index("farmerMemberId"),
        Index("cropType")
    ]
)
data class MarketListingRow(
    @PrimaryKey val id: String,
    /** رقم المراجعة: يتغيّر كلما تغيّر السعر أو البيان بعد النشر، فيُعاد طلب المصادقة. */
    val revision: Int = 1,
    val status: String,
    val brokerMemberId: String,
    val farmerMemberId: String,
    /** اسم المزارع كما يظهر للدلال (يُنشر إن أراد، واسم الدلال هو المعلن دائمًا). */
    val farmerName: String = "",
    val brokerName: String = "",
    val brokerPhoneChoice: String = "HIDDEN",
    val title: String,
    val cropType: String,
    val description: String = "",
    val quantityNote: String = "",
    val priceMode: String,
    /** بالوحدة الصغرى (فلس) أو صفر في «على السوم» — لا `Double` في أي حال (ADR-04). */
    val priceMinor: Long = 0L,
    val currency: String = "YER_NEW",
    val unit: String,
    /** ما يُنشر من الموقع: المحافظة. والتفصيل في [MarketContactRow]. */
    val governorate: String = "",
    val photoCount: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val publishedAt: Long? = null,
    val closedAt: Long? = null
) {
    val isPublicVisible: Boolean get() = status == "PUBLISHED" || status == "RESERVED"
}

/**
 * جهات الاتصال والموقع الدقيق — **لا تُقرأ إلا لصاحبها**.
 *
 * مفصول عمدًا عن صفّ العرض: هذا الجدول هو الوحيد الذي يحمل هاتف المزارع، ولا يدخل في أي مسار عام
 * (لا شاشة سوق، ولا مشاركة، ولا تصدير عرض).
 */
@Entity(
    tableName = "market_contacts",
    foreignKeys = [
        ForeignKey(
            entity = MarketListingRow::class,
            parentColumns = ["id"],
            childColumns = ["listingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MarketContactRow(
    @PrimaryKey val listingId: String,
    val farmerPhone: String = "",
    /** قرار المزارع وحده: هل يُفتح باب التواصل المباشر له (بلا نشر رقمه). */
    val farmerAllowsPublicPhone: Boolean = false,
    /** رقم الدلال — يُنشر فقط إذا اختار ذلك، وهو رقمه لا رقم غيره. */
    val brokerPhone: String = "",
    val governorate: String = "",
    val district: String = "",
    val village: String = "",
    val landmark: String = ""
)

/** طلب تسويق من مزارع إلى دلال. **الطلب ليس دَينًا** ولا يُنشئ قيدًا في أي غرفة. */
@Entity(
    tableName = "market_requests",
    indices = [Index("farmerMemberId"), Index("brokerMemberId"), Index("status")]
)
data class MarketingRequestRow(
    @PrimaryKey val id: String,
    val farmerMemberId: String,
    val brokerMemberId: String,
    val cropTitle: String,
    val status: String,
    val requestedAt: Long,
    val note: String = ""
) {
    val isAccepted: Boolean get() = status == "ACCEPTED"
}

/**
 * قرار مصادقة على **مراجعة بعينها**. تعديل السعر أو البيان يرفع المراجعة فلا تسري المصادقة القديمة،
 * وهذا ما يمنع «نشر ما لم يُراجَع» من باب خلفي.
 */
@Entity(
    tableName = "market_moderations",
    primaryKeys = ["listingId", "revision"],
    foreignKeys = [
        ForeignKey(
            entity = MarketListingRow::class,
            parentColumns = ["id"],
            childColumns = ["listingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ModerationRow(
    val listingId: String,
    val revision: Int,
    val decision: String,
    val decidedAt: Long,
    val note: String = "",
    /** من قرّر: `platform` لجهة المصادقة، أو `local-admin` لأداة الإدارة المحلية في نسخة التطوير. */
    val decidedBy: String = "platform"
)

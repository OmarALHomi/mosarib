package com.baynana.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * أسماء لوحة «المسرب» القديمة، مُعاد توجيهها إلى هوية «بيننا» الجديدة.
 *
 * **ملف مؤقت مُعلن، لا يبقى.** سبب وجوده: الشاشات القديمة (العملاء، السقيات، السندات، الصلح،
 * الإعدادات، حول) تُحذف حزمةً حزمة في د٣–د٦؛ ولو حُذفت أسماء الألوان الآن لانهار البناء في كل
 * شاشة لم يُنقَل ملفها بعد. فبدل تجميد اللوحة القديمة في `Color.kt` (وهو ما يوهم أنها معتمدة)،
 * وُضع الجسر هنا صريحًا باسمه، ومعه حاجز CI يُذكّره بالموعد.
 *
 * القاعدة: **لا يُضاف اسم جديد إلى هذا الملف أبدًا**، ولا يُستعمل في أي شاشة جديدة.
 */
object LegacyPaletteAliases {
    /** للتذكير في التقارير النصية. */
    const val REMOVAL_PACK = "د٦"
}

@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val PrimaryTeal: Color get() = HarvestGreen
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val PrimaryTealLight: Color get() = HarvestGreenLight
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val PrimaryTealDark: Color get() = HarvestGreenDark
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val SecondaryAqua: Color get() = WarmEarth
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val SecondaryAquaLight: Color get() = WarmEarthLight
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val SecondaryAquaDark: Color get() = WarmEarthLight
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val AccentGold: Color get() = HarvestGold
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val AccentAmber: Color get() = WaitingAmber
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val AccentEmerald: Color get() = AcknowledgedGreen
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val AccentCoral: Color get() = DangerRed
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val AccentCrimson: Color get() = DangerRed
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val StatusActive: Color get() = AcknowledgedGreen
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val StatusDebt: Color get() = DangerRed
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val StatusPaid: Color get() = AcknowledgedGreen
@Deprecated("اسم قديم من لوحة المسرب — استعمل ألوان الهوية في Color.kt، وسيُحذف في د٦")
val StatusRunning: Color get() = WaitingAmber

package com.baynana.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * لوحة «بيننا» — **«نيلة وذهب»** (اتجاه B المعتمد بعد اختيار المالك للشعار).
 *
 * لماذا هذه اللوحة: الشعار الذي اعتمده المالك (مصافحة قلبية داخل قلب، ذهبية اللمسة، على حقل نيلي)
 * يحمل هويته كاملة. فبدل أن تُبنى الواجهة على لوحة أخرى ثم يُلصق الشعار عليها، صار الشعار هو المصدر:
 * **النيلة** لون المؤسسة (أزرق حبر دفتر، لا أزرق مؤسسة باردة)، و**الذهب** لمسة واحدة فقط في الشاشة
 * (شعار الإقرار وبصمة النسخة)، و**الكريمي** ورق الدفتر. الأخضر بقي **دلالة حالة** («مُقرّ») لا لون هوية.
 *
 * قواعد ملزمة:
 * - **لا لون يحمل معنى وحده**: كل حالة بلون ورمز وكلمة.
 * - **التباين ≥ 4.5:1** في الوضعين (يفحصه اختبار التباين في `DesignTokensTest`).
 * - **الذهبي ≤ 5% من الشاشة**، ولا يُلوَّن به أي مبلغ: الأرقام بالحبر أو بألوان الحالة.
 * - لا تُضاف ألوان جديدة إلا بسبب دلالة جديدة (حالة/تنبيه)، لا «لتجميل» شاشة.
 */

// ---------------------------------------------------------------- الأساسي: النيلة (أزرق الحبر)
val NavyNile = Color(0xFF14304F)
/** نيلي فاتح للوضع الفاتح (بقع وحدود)، ونيلي **أفتح** للوضع الليلي: القيمة الأولى لا تصلح نصًّا
 *  على خلفية ليلية (٢٫٦:١ فقط)، وهي التي كشفها اختبار التباين وأُصلحت. */
val NavyNileLight = Color(0xFF3F6489)
val NavyNileOnDark = Color(0xFF8FB0CE)
val NavyNileDark = Color(0xFF0A2137)
val NileContainer = Color(0xFFD9E4F0)
val NileContainerDark = Color(0xFF17334F)

// ---------------------------------------------------------------- الثانوي: نيلة فاتحة للأسطح المرفوعة
val NileSoft = Color(0xFF52779C)
val NileSoftLight = Color(0xFFA9C2DA)
val NileSoftContainer = Color(0xFFE6EDF5)
val NileSoftContainerDark = Color(0xFF1E2E40)

// ---------------------------------------------------------------- المميّز: ذهبي بخيل (نقطة الشعار)
val HoneyGold = Color(0xFFC99A28)
val HoneyGoldLight = Color(0xFFE7C767)
val HoneyGoldContainer = Color(0xFFF6EBC9)
val HoneyGoldContainerDark = Color(0xFF3C3115)

// ---------------------------------------------------------------- الأسطح: ورق كريمي وحبر داكن
val CreamBackground = Color(0xFFFAF7F0)
val CreamSurface = Color(0xFFFFFDF8)
val CreamSurfaceVariant = Color(0xFFEDE7DA)
val InkOnCream = Color(0xFF14202E)
val InkMuted = Color(0xFF57616C)
val CreamOutline = Color(0xFFD8D2C4)

val NightBackground = Color(0xFF0C1622)
val NightSurface = Color(0xFF14202E)
val NightSurfaceVariant = Color(0xFF1E2C3A)
val NightOnSurface = Color(0xFFF3F1EA)
val NightOnSurfaceVariant = Color(0xFFC2C9C3)
val NightOutline = Color(0xFF33434F)

// ---------------------------------------------------------------- دلالات الحالة
/** خطر مالي: تحذير، فشل دائم، اعتراض. */
val DangerRed = Color(0xFFB3261E)
val DangerRedLight = Color(0xFFFFB4AB)
val DangerContainer = Color(0xFFF9DEDC)
val DangerContainerDark = Color(0xFF5C1A17)

/** إقرار وتمام: قيد مُقرّ، صلح مكتمل. الأخضر هنا **دلالة** لا هوية. */
val AcknowledgedGreen = Color(0xFF1B6B45)
val AcknowledgedGreenLight = Color(0xFF7DDBA8)
val AcknowledgedContainer = Color(0xFFDCEFE3)
val AcknowledgedContainerDark = Color(0xFF15402B)

/** انتظار: قيد أُرسل ولم يُقرّ، قسط مستحق. */
val WaitingAmber = Color(0xFF8A5A00)
val WaitingAmberLight = Color(0xFFFFCF7A)
val WaitingContainer = Color(0xFFFBEBD0)
val WaitingContainerDark = Color(0xFF463206)

/** معلومة: مسودة، حفظ محلي، رصيد دائن معلّق. */
val InfoBlue = Color(0xFF2A5B8A)
val InfoBlueLight = Color(0xFFA8CBEE)
val InfoContainer = Color(0xFFDEEAF6)
val InfoContainerDark = Color(0xFF1B3348)

// ---------------------------------------------------------------- أزواج الحاويات والنصّ عليها
// تُعرَّف هنا (لا داخل `Theme.kt`) حتى يقيس اختبار التباين ما يُعرض فعلًا، فالقيمة المخفية لا تُقاس.
val NileOnContainer = Color(0xFF0A2137)
val NileOnContainerDark = Color(0xFFD9E4F0)
val EarthOnContainer = Color(0xFF3A2E29)
val EarthOnContainerDark = Color(0xFFF0E4DD)
val GoldOnContainer = Color(0xFF3C3115)
val GoldOnContainerDark = Color(0xFFF6EBC9)
val DangerOnContainer = Color(0xFF5C1A17)
val DangerOnContainerDark = Color(0xFFF9DEDC)
val AcknowledgedOnContainer = Color(0xFF14402B)
val AcknowledgedOnContainerDark = Color(0xFFDCEFE3)
val WaitingOnContainer = Color(0xFF463206)
val WaitingOnContainerDark = Color(0xFFFBEBD0)
val InfoOnContainer = Color(0xFF1B3348)
val InfoOnContainerDark = Color(0xFFDEEAF6)

package com.baynana.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * لوحة «بيننا» — «حقول اليمن».
 *
 * لماذا غيّرنا اللوحة المائية القديمة: كانت لوحة تطبيق توزيع مياه (فيروزي وأزرق)، و«بيننا» دفتر
 * حسابات بين طرفين. الأخضر الترابي الدافئ يقول «أرض وحصاد وأمانة»، والذهبي يُصرف بحدّ أقصى (شعار
 * الإقرار وبصمة النسخة)، والأحمر للخطر المالي وحده.
 *
 * قواعد ملزمة:
 * - **لا لون يحمل معنى وحده**: كل حالة بلون ورمز وكلمة.
 * - **التباين ≥ 4.5:1** في الوضعين (يفحصه اختبار التباين في `DesignTokensTest`).
 * - المبالغ لا تُلوَّن بالذهبي أبدًا: الذهبي للتزيين لا للأرقام.
 */

// ---------------------------------------------------------------- الأساسي: أخضر الحصاد
val HarvestGreen = Color(0xFF0E6B4F)
val HarvestGreenLight = Color(0xFF3E9B77)
val HarvestGreenDark = Color(0xFF064434)
val HarvestContainer = Color(0xFFD8EBE1)
val HarvestContainerDark = Color(0xFF123A2D)

// ---------------------------------------------------------------- الثانوي: ترابي دافئ
val WarmEarth = Color(0xFF8D6E63)
val WarmEarthLight = Color(0xFFB79A8E)
val WarmEarthContainer = Color(0xFFF0E4DD)
val WarmEarthContainerDark = Color(0xFF3A2E29)

// ---------------------------------------------------------------- المميّز: ذهبي بخيل
val HarvestGold = Color(0xFFC9A227)
val HarvestGoldContainer = Color(0xFFF6ECC8)
val HarvestGoldContainerDark = Color(0xFF3D3312)

// ---------------------------------------------------------------- الأسطح: ورق دافئ وحبر داكن
val PaperBackground = Color(0xFFF7F3EA)
val PaperSurface = Color(0xFFFFFDF8)
val PaperSurfaceVariant = Color(0xFFEDE7DA)
val InkOnPaper = Color(0xFF1B1A17)
val InkMuted = Color(0xFF5C5850)
val PaperOutline = Color(0xFFD8D2C4)

val NightBackground = Color(0xFF0E1512)
val NightSurface = Color(0xFF16201C)
val NightSurfaceVariant = Color(0xFF22302A)
val NightOnSurface = Color(0xFFF3F1EA)
val NightOnSurfaceVariant = Color(0xFFC2C9C3)
val NightOutline = Color(0xFF33443C)

// ---------------------------------------------------------------- دلالات الحالة
/** خطر مالي: تحذير، فشل دائم، اعتراض. */
val DangerRed = Color(0xFFB3261E)
val DangerRedLight = Color(0xFFFFB4AB)
val DangerContainer = Color(0xFFF9DEDC)
val DangerContainerDark = Color(0xFF5C1A17)

/** إقرار وتمام: قيد مُقرّ، صلح مكتمل. */
val AcknowledgedGreen = Color(0xFF1B6B45)
val AcknowledgedGreenLight = Color(0xFF7DDBA8)
val AcknowledgedContainer = Color(0xFFDCEFE3)
val AcknowledgedContainerDark = Color(0xFF15402B)

/** انتظار: قيد مُرسل لم يُقرّ، قسط مستحق. */
val WaitingAmber = Color(0xFF8A5A00)
val WaitingAmberLight = Color(0xFFFFCF7A)
val WaitingContainer = Color(0xFFFBEBD0)
val WaitingContainerDark = Color(0xFF463206)

/** معلومة/مسودة: مسودة محلية، رصيد دائن. */
val InfoBlue = Color(0xFF2A5B8A)
val InfoBlueLight = Color(0xFFA8CBEE)
val InfoContainer = Color(0xFFDEEAF6)
val InfoContainerDark = Color(0xFF1B3348)

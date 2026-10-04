package com.baynana.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * سمة «بيننا». `dynamicColor` مُطفأ عمدًا: هوية الدفتر ثابتة، والألوان الديناميكية من خلفية النظام
 * تُفقد معنى «مُقرّ/منتظر» إن تغيّرت.
 *
 * والألوان الدلالية تُقرأ من [BaynanaStatus] لا من `MaterialTheme.colorScheme` مباشرة: فالحالة في
 * هذا التطبيق معنى محاسبي (مُقرّ، منتظر، معترض) لا زينة، وتتبدّل مع الوضع الليلي من موضع واحد.
 *
 * ⚠️ ترتيب التعريفات مقصود: الثوابت أولًا ثم المخططات، لأن خصائص المستوى الأعلى تُهيَّأ بترتيب
 * كتابتها في الملف، فتعريف لون بعد استخدامه يعني قيمة غير مهيَّأة.
 */

// ------------------------------------------------------------------ ثوابت مساعدة
// كل قيم اللوحة تعيش في `Color.kt` (حتى يقيسها اختبار التباين)، وهذه أسماء محلية للاختصار فقط.
private val OnNile = NileOnContainer
private val OnNileDark = NileOnContainerDark
private val OnGold = GoldOnContainer
private val OnGoldDark = GoldOnContainerDark
private val OnDanger = DangerOnContainer
private val OnDangerDark = DangerOnContainerDark
private val OnAckContainer = AcknowledgedOnContainer
private val OnAckContainerDark = AcknowledgedOnContainerDark
private val OnWaitContainer = WaitingOnContainer
private val OnWaitContainerDark = WaitingOnContainerDark
private val OnInfoContainer = InfoOnContainer
private val OnInfoContainerDark = InfoOnContainerDark

// ------------------------------------------------------------------ المخططات
internal val LightColors = lightColorScheme(
    primary = NavyNile,
    onPrimary = CreamSurface,
    primaryContainer = NileContainer,
    onPrimaryContainer = OnNile,
    secondary = NileSoft,
    onSecondary = CreamSurface,
    secondaryContainer = NileSoftContainer,
    onSecondaryContainer = OnNile,
    tertiary = HoneyGold,
    onTertiary = OnGold,
    tertiaryContainer = HoneyGoldContainer,
    onTertiaryContainer = OnGold,
    background = CreamBackground,
    onBackground = InkOnCream,
    surface = CreamSurface,
    onSurface = InkOnCream,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkMuted,
    outline = CreamOutline,
    error = DangerRed,
    onError = CreamSurface,
    errorContainer = DangerContainer,
    onErrorContainer = OnDanger
)

internal val DarkColors = darkColorScheme(
    // نيلي أفتح من نظيره الفاتح: اللون الذي يبدو جميلًا كبقعة يفشل كنصّ على الليل (٢٫٦:١).
    primary = NavyNileOnDark,
    onPrimary = NightBackground,
    primaryContainer = NileContainerDark,
    onPrimaryContainer = OnNileDark,
    secondary = NileSoftLight,
    onSecondary = NightBackground,
    secondaryContainer = NileSoftContainerDark,
    onSecondaryContainer = OnNileDark,
    tertiary = HoneyGold,
    onTertiary = NightBackground,
    tertiaryContainer = HoneyGoldContainerDark,
    onTertiaryContainer = OnGoldDark,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
    error = DangerRedLight,
    // النصّ على لون الخطأ: أحمر داكن على ورديّ فاتح (٦:١)، لا ورديّ على ورديّ (١٫٣:١).
    onError = OnDanger,
    errorContainer = DangerContainerDark,
    onErrorContainer = OnDangerDark
)

/** ألوان دلالية: حالة القيد وحالة الحفظ وحالة السوق. تُقرأ من مكان واحد. */
data class BaynanaStatusColors(
    val acknowledged: Color,
    val acknowledgedContainer: Color,
    val onAcknowledgedContainer: Color,
    val waiting: Color,
    val waitingContainer: Color,
    val onWaitingContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color
)

internal val LightStatus = BaynanaStatusColors(
    acknowledged = AcknowledgedGreen,
    acknowledgedContainer = AcknowledgedContainer,
    onAcknowledgedContainer = OnAckContainer,
    waiting = WaitingAmber,
    waitingContainer = WaitingContainer,
    onWaitingContainer = OnWaitContainer,
    info = InfoBlue,
    infoContainer = InfoContainer,
    onInfoContainer = OnInfoContainer,
    danger = DangerRed,
    dangerContainer = DangerContainer,
    // نصّ الخطر على حاويته: كان مربوطًا بلون الوضع الليلي (ورديّ على ورديّ)، وهذا ما كشفه
    // اختبار التباين قبل أن يصل إلى مستخدم.
    onDangerContainer = OnDanger
)

internal val DarkStatus = BaynanaStatusColors(
    acknowledged = AcknowledgedGreenLight,
    acknowledgedContainer = AcknowledgedContainerDark,
    onAcknowledgedContainer = OnAckContainerDark,
    waiting = WaitingAmberLight,
    waitingContainer = WaitingContainerDark,
    onWaitingContainer = OnWaitContainerDark,
    info = InfoBlueLight,
    infoContainer = InfoContainerDark,
    onInfoContainer = OnInfoContainerDark,
    danger = DangerRedLight,
    dangerContainer = DangerContainerDark,
    onDangerContainer = OnDangerDark
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatus }

object BaynanaStatus {
    val colors: BaynanaStatusColors
        @Composable get() = LocalStatusColors.current
}

@Composable
fun BaynanaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalStatusColors provides if (darkTheme) DarkStatus else LightStatus) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography,
            content = content
        )
    }
}

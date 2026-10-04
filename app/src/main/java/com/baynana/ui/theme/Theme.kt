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
private val OnNileDark = Color(0xFF0A2137)
private val OnGoldDark = Color(0xFF3C3115)
private val OnDangerDark = Color(0xFF5C1A17)
private val OnNileContainer = Color(0xFFD8EBE1)
private val OnEarthContainer = Color(0xFFF0E4DD)
private val OnDangerContainer = Color(0xFFF9DEDC)
private val OnAcknowledgedContainer = Color(0xFF14402B)
private val OnWaitingContainer = Color(0xFF463206)
private val OnInfoContainer = Color(0xFF1B3348)
private val OnAcknowledgedContainerDark = Color(0xFFDCEFE3)
private val OnWaitingContainerDark = Color(0xFFFBEBD0)
private val OnInfoContainerDark = Color(0xFFDEEAF6)

// ------------------------------------------------------------------ المخططات
private val LightColors = lightColorScheme(
    primary = NavyNile,
    onPrimary = CreamSurface,
    primaryContainer = NileContainer,
    onPrimaryContainer = NavyNileDark,
    secondary = NileSoft,
    onSecondary = CreamSurface,
    secondaryContainer = NileSoftContainer,
    onSecondaryContainer = OnNileDark,
    tertiary = HoneyGold,
    onTertiary = OnNileDark,
    tertiaryContainer = HoneyGoldContainer,
    onTertiaryContainer = OnGoldDark,
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
    onErrorContainer = OnDangerDark
)

private val DarkColors = darkColorScheme(
    primary = NavyNileLight,
    onPrimary = NightBackground,
    primaryContainer = NileContainerDark,
    onPrimaryContainer = OnNileContainer,
    secondary = NileSoftLight,
    onSecondary = NightBackground,
    secondaryContainer = NileSoftContainerDark,
    onSecondaryContainer = OnEarthContainer,
    tertiary = HoneyGold,
    onTertiary = NightBackground,
    tertiaryContainer = HoneyGoldContainerDark,
    onTertiaryContainer = HoneyGoldContainer,
    background = NightBackground,
    onBackground = NightOnSurface,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
    error = DangerRedLight,
    onError = OnDangerDark,
    errorContainer = DangerContainerDark,
    onErrorContainer = OnDangerContainer
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

private val LightStatus = BaynanaStatusColors(
    acknowledged = AcknowledgedGreen,
    acknowledgedContainer = AcknowledgedContainer,
    onAcknowledgedContainer = OnAcknowledgedContainer,
    waiting = WaitingAmber,
    waitingContainer = WaitingContainer,
    onWaitingContainer = OnWaitingContainer,
    info = InfoBlue,
    infoContainer = InfoContainer,
    onInfoContainer = OnInfoContainer,
    danger = DangerRed,
    dangerContainer = DangerContainer,
    onDangerContainer = OnDangerDark
)

private val DarkStatus = BaynanaStatusColors(
    acknowledged = AcknowledgedGreenLight,
    acknowledgedContainer = AcknowledgedContainerDark,
    onAcknowledgedContainer = OnAcknowledgedContainerDark,
    waiting = WaitingAmberLight,
    waitingContainer = WaitingContainerDark,
    onWaitingContainer = OnWaitingContainerDark,
    info = InfoBlueLight,
    infoContainer = InfoContainerDark,
    onInfoContainer = OnInfoContainerDark,
    danger = DangerRedLight,
    dangerContainer = DangerContainerDark,
    onDangerContainer = OnDangerContainer
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

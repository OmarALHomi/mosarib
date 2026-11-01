package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * ألوان وتدرجات نظام التصميم الموحد لتطبيق مُسَرِّب (Mosarib Design System Tokens).
 *
 * إرشادات للمطورين ومساعدي الذكاء الاصطناعي:
 * 1. تجنب استخدام ألوان صريحة مثل `Color(0x...)` أو دوال التحقق المباشرة مثل `isAppDark()`.
 *    استخدم بدلاً منها `MaterialTheme.colorScheme` للألوان الأساسية، أو `MaterialTheme.mosarib`
 *    للألوان الدلالية الممتدة (success, warning, info, debt, credit, settled, textMuted).
 * 2. تجنب استخدام أحجام خطوط عشوائية أو صريحة `fontSize = ...sp` داخل الشاشات،
 *    واعتمد على أنماط الخط الرسمية في `MaterialTheme.typography` (مثل `bodyMedium`, `titleSmall`, `labelSmall`).
 */
@Immutable
data class MosaribExtendedColors(
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
    val debt: Color,      // money the customer owes us ("عليه")
    val credit: Color,    // money we owe / customer credit ("له")
    val settled: Color,   // balance zero
    val textMuted: Color, // secondary hints (replaces 0xFF94A3B8 / 0xFF64748B)
    val discount: Color   // discount vouchers and adjustments
)

val LightMosaribExtendedColors = MosaribExtendedColors(
    success = Color(0xFF2E7D32),
    onSuccess = Color.White,
    successContainer = Color(0xFFE8F5E9),
    onSuccessContainer = Color(0xFF2E7D32),
    warning = Color(0xFFD97706),
    onWarning = Color.White,
    warningContainer = Color(0xFFFFF3E0),
    onWarningContainer = Color(0xFFD97706),
    info = Color(0xFF0284C7),
    onInfo = Color.White,
    infoContainer = Color(0xFFE0F2FE),
    onInfoContainer = Color(0xFF0284C7),
    debt = Color(0xFFE53935),
    credit = Color(0xFF2E7D32),
    settled = Color(0xFF64748B),
    textMuted = Color(0xFF64748B),
    discount = Color(0xFF7C3AED)
)

val DarkMosaribExtendedColors = MosaribExtendedColors(
    success = Color(0xFF4ADE80),
    onSuccess = Color(0xFF0A1014),
    successContainer = Color(0xFF132A1C),
    onSuccessContainer = Color(0xFF4ADE80),
    warning = Color(0xFFFBBF24),
    onWarning = Color(0xFF0A1014),
    warningContainer = Color(0xFF2E2210),
    onWarningContainer = Color(0xFFFBBF24),
    info = Color(0xFF38BDF8),
    onInfo = Color(0xFF0A1014),
    infoContainer = Color(0xFF0C2433),
    onInfoContainer = Color(0xFF38BDF8),
    debt = Color(0xFFF87171),
    credit = Color(0xFF4ADE80),
    settled = Color(0xFF94A3B8),
    textMuted = Color(0xFF94A3B8),
    discount = Color(0xFFA78BFA)
)

val LocalMosaribExtendedColors = staticCompositionLocalOf { LightMosaribExtendedColors }

val MaterialTheme.mosarib: MosaribExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMosaribExtendedColors.current

/**
 * لوحة ألوان الأفاتار الثابتة (8 ألوان ناعمة متوافقة مع النمطين الفاتح والداكن).
 * يتم اختيار اللون بالاعتماد على معرف العميل: `abs(customer.id % 8)`.
 */
object AvatarPalette {
    data class AvatarColor(val background: Color, val onBackground: Color)

    val Light = listOf(
        AvatarColor(background = Color(0xFFE0F2FE), onBackground = Color(0xFF0369A1)), // Sky
        AvatarColor(background = Color(0xFFE8F5E9), onBackground = Color(0xFF15803D)), // Green
        AvatarColor(background = Color(0xFFFEF3C7), onBackground = Color(0xFFB45309)), // Amber
        AvatarColor(background = Color(0xFFF3E8FF), onBackground = Color(0xFF7E22CE)), // Purple
        AvatarColor(background = Color(0xFFFFE4E6), onBackground = Color(0xFFBE123C)), // Rose
        AvatarColor(background = Color(0xFFCCFBF1), onBackground = Color(0xFF0F766E)), // Teal
        AvatarColor(background = Color(0xFFEDE9FE), onBackground = Color(0xFF6D28D9)), // Indigo
        AvatarColor(background = Color(0xFFFFEDD5), onBackground = Color(0xFFC2410C))  // Orange
    )

    val Dark = listOf(
        AvatarColor(background = Color(0xFF0C2D48), onBackground = Color(0xFF7DD3FC)),
        AvatarColor(background = Color(0xFF143823), onBackground = Color(0xFF86EFAC)),
        AvatarColor(background = Color(0xFF382806), onBackground = Color(0xFFFDE047)),
        AvatarColor(background = Color(0xFF2E1065), onBackground = Color(0xFFD8B4FE)),
        AvatarColor(background = Color(0xFF3F0B1B), onBackground = Color(0xFFFDA4AF)),
        AvatarColor(background = Color(0xFF0A3632), onBackground = Color(0xFF5EEAD4)),
        AvatarColor(background = Color(0xFF1E1B4B), onBackground = Color(0xFFC4B5FD)),
        AvatarColor(background = Color(0xFF3D1A06), onBackground = Color(0xFFFDBA74))
    )

    fun forId(id: Long, isDark: Boolean): AvatarColor {
        val index = (kotlin.math.abs(id) % 8).toInt()
        return if (isDark) Dark[index] else Light[index]
    }
}


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
    val textMuted: Color  // secondary hints (replaces 0xFF94A3B8 / 0xFF64748B)
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
    textMuted = Color(0xFF64748B)
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
    textMuted = Color(0xFF94A3B8)
)

val LocalMosaribExtendedColors = staticCompositionLocalOf { LightMosaribExtendedColors }

val MaterialTheme.mosarib: MosaribExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalMosaribExtendedColors.current


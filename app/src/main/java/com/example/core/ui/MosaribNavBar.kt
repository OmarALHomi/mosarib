package com.example.core.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import com.example.ui.theme.SecondaryAquaLight

// ──────────────────────────────────────────────────────────────────────────────
// Dynamic Crested Shape that smoothly animates its peak along with active tab
// ──────────────────────────────────────────────────────────────────────────────
class DynamicCrestedBarShape(
    private val centerFraction: Float,
    private val flatTopDp: Float = 26f,
    private val crestPeakDp: Float = 5f,
    private val crestHalfWDp: Float = 50f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val cx = w * centerFraction

        val flatTop = flatTopDp * density.density
        val peak = crestPeakDp * density.density
        val halfW = crestHalfWDp * density.density

        val path = Path().apply {
            moveTo(0f, flatTop)
            val leftBound = (cx - halfW).coerceAtLeast(0f)
            val rightBound = (cx + halfW).coerceAtMost(w)

            lineTo(leftBound, flatTop)
            cubicTo(
                cx - halfW * 0.55f, flatTop,
                cx - halfW * 0.45f, peak,
                cx, peak
            )
            cubicTo(
                cx + halfW * 0.45f, peak,
                cx + halfW * 0.55f, flatTop,
                rightBound, flatTop
            )
            lineTo(w, flatTop)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        return Outline.Generic(path)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Exact Vector Icons from Reference Image (Canvas-drawn)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Tab 0 (التقارير): Analytics bar chart with rising trendline.
 */
@Composable
fun AnalyticsChartIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val barW = w * 0.18f
        val b1H = h * 0.35f
        val b2H = h * 0.55f
        val b3H = h * 0.75f

        val b1Left = w * 0.12f
        val b2Left = w * 0.41f
        val b3Left = w * 0.70f
        val baseBottom = h * 0.88f

        // Baseline
        drawLine(
            color = tint.copy(alpha = 0.5f),
            start = Offset(w * 0.08f, baseBottom),
            end = Offset(w * 0.92f, baseBottom),
            strokeWidth = 1.2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Bar 1
        drawRoundRect(
            color = tint,
            topLeft = Offset(b1Left, baseBottom - b1H),
            size = Size(barW, b1H),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Bar 2
        drawRoundRect(
            color = tint,
            topLeft = Offset(b2Left, baseBottom - b2H),
            size = Size(barW, b2H),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Bar 3
        drawRoundRect(
            color = tint,
            topLeft = Offset(b3Left, baseBottom - b3H),
            size = Size(barW, b3H),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Trendline dots
        drawCircle(
            color = tint,
            radius = 2.dp.toPx(),
            center = Offset(b1Left + barW / 2f, baseBottom - b1H - 4.dp.toPx())
        )
        drawCircle(
            color = tint,
            radius = 2.dp.toPx(),
            center = Offset(b2Left + barW / 2f, baseBottom - b2H - 4.dp.toPx())
        )
        drawCircle(
            color = tint,
            radius = 2.dp.toPx(),
            center = Offset(b3Left + barW / 2f, baseBottom - b3H - 4.dp.toPx())
        )
    }
}

/**
 * Tab 1 (العملاء): Customer ID badge with lanyard clip, photo frame, 3 lines, ribbon seal.
 */
@Composable
fun CustomersBadgeIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Top lanyard clip tab
        val clipW = w * 0.22f
        val clipH = h * 0.14f
        drawRoundRect(
            color = tint,
            topLeft = Offset((w - clipW) / 2f, h * 0.05f),
            size = Size(clipW, clipH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawCircle(
            color = tint,
            radius = 1.5.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.12f)
        )

        // 2. ID Card main body
        val cardLeft = w * 0.08f
        val cardTop = h * 0.17f
        val cardW = w * 0.84f
        val cardH = h * 0.72f
        drawRoundRect(
            color = tint,
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // 3. Photo frame on the left
        val photoLeft = cardLeft + cardW * 0.08f
        val photoTop = cardTop + cardH * 0.15f
        val photoW = cardW * 0.35f
        val photoH = cardH * 0.70f
        drawRoundRect(
            color = tint,
            topLeft = Offset(photoLeft, photoTop),
            size = Size(photoW, photoH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = 1.1.dp.toPx())
        )

        // Avatar head
        val headCx = photoLeft + photoW * 0.5f
        val headCy = photoTop + photoH * 0.35f
        drawCircle(
            color = tint,
            radius = photoW * 0.22f,
            center = Offset(headCx, headCy)
        )
        // Avatar shoulders
        val shoulderPath = Path().apply {
            moveTo(photoLeft + photoW * 0.12f, photoTop + photoH * 0.90f)
            cubicTo(
                photoLeft + photoW * 0.18f, photoTop + photoH * 0.60f,
                photoLeft + photoW * 0.82f, photoTop + photoH * 0.60f,
                photoLeft + photoW * 0.88f, photoTop + photoH * 0.90f
            )
            close()
        }
        drawPath(shoulderPath, color = tint)

        // 4. Three info lines on the right
        val lineLeft = cardLeft + cardW * 0.48f
        val lineRight = cardLeft + cardW * 0.90f
        val l1Y = cardTop + cardH * 0.28f
        val l2Y = cardTop + cardH * 0.48f
        val l3Y = cardTop + cardH * 0.68f

        drawLine(color = tint, start = Offset(lineLeft, l1Y), end = Offset(lineRight, l1Y), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(lineLeft, l2Y), end = Offset(lineRight, l2Y), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(lineLeft, l3Y), end = Offset(lineLeft + (lineRight - lineLeft) * 0.55f, l3Y), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)

        // 5. Medal ribbon seal on bottom-right
        val sealCx = cardLeft + cardW * 0.82f
        val sealCy = cardTop + cardH * 0.82f
        val sealR = cardW * 0.18f
        val ribbonPath = Path().apply {
            moveTo(sealCx - sealR * 0.6f, sealCy + sealR * 0.5f)
            lineTo(sealCx - sealR * 0.8f, sealCy + sealR * 1.6f)
            lineTo(sealCx - sealR * 0.2f, sealCy + sealR * 1.2f)
            close()
            moveTo(sealCx + sealR * 0.6f, sealCy + sealR * 0.5f)
            lineTo(sealCx + sealR * 0.8f, sealCy + sealR * 1.6f)
            lineTo(sealCx + sealR * 0.2f, sealCy + sealR * 1.2f)
            close()
        }
        drawPath(ribbonPath, color = tint)
        drawCircle(color = tint, radius = sealR, center = Offset(sealCx, sealCy))
    }
}

/**
 * Tab 2 (الرئيسية): Water drop silhouette containing a house (roof, chimney, walls, doorway).
 */
@Composable
fun HomeDropHouseIcon(
    dropTint: Color,
    houseTint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // 1. Water drop shape
        val dropPath = Path().apply {
            val topY = h * 0.08f
            val botY = h * 0.94f
            val dropW = w * 0.74f

            moveTo(cx, topY)
            cubicTo(
                cx + dropW * 0.60f, topY + (botY - topY) * 0.35f,
                cx + dropW * 0.55f, botY,
                cx, botY
            )
            cubicTo(
                cx - dropW * 0.55f, botY,
                cx - dropW * 0.60f, topY + (botY - topY) * 0.35f,
                cx, topY
            )
            close()
        }
        drawPath(path = dropPath, color = dropTint)

        // 2. House inside water drop
        val roofTopY = h * 0.38f
        val roofBaseY = h * 0.55f
        val roofHalfW = w * 0.24f

        val roofPath = Path().apply {
            moveTo(cx, roofTopY)
            lineTo(cx + roofHalfW, roofBaseY)
            lineTo(cx - roofHalfW, roofBaseY)
            close()
        }
        drawPath(roofPath, color = houseTint)

        // Chimney on right
        val chimneyLeft = cx + roofHalfW * 0.40f
        val chimneyTop = roofTopY + (roofBaseY - roofTopY) * 0.25f
        val chimneyW = roofHalfW * 0.25f
        drawRect(
            color = houseTint,
            topLeft = Offset(chimneyLeft, chimneyTop),
            size = Size(chimneyW, (roofBaseY - chimneyTop) * 0.9f)
        )

        // House walls / body
        val houseBodyTop = roofBaseY - 1.dp.toPx()
        val houseBodyW = roofHalfW * 1.6f
        val houseBodyH = h * 0.22f
        drawRect(
            color = houseTint,
            topLeft = Offset(cx - houseBodyW * 0.5f, houseBodyTop),
            size = Size(houseBodyW, houseBodyH)
        )

        // Doorway
        val doorW = houseBodyW * 0.32f
        val doorH = houseBodyH * 0.68f
        val doorLeft = cx - doorW * 0.5f
        val doorTop = houseBodyTop + houseBodyH - doorH
        drawRoundRect(
            color = dropTint,
            topLeft = Offset(doorLeft, doorTop),
            size = Size(doorW, doorH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
    }
}

/**
 * Tab 3 (سجل العمليات): Clipboard with flowchart tree and pressure gauge.
 */
@Composable
fun OperationsClipboardIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Top clip
        val clipW = w * 0.32f
        val clipH = h * 0.12f
        drawRoundRect(
            color = tint,
            topLeft = Offset((w - clipW) * 0.40f, h * 0.04f),
            size = Size(clipW, clipH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // 2. Clipboard body
        val boardLeft = w * 0.10f
        val boardTop = h * 0.12f
        val boardW = w * 0.68f
        val boardH = h * 0.82f
        drawRoundRect(
            color = tint,
            topLeft = Offset(boardLeft, boardTop),
            size = Size(boardW, boardH),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx())
        )

        // 3. Flowchart diagram
        val topNodeW = boardW * 0.38f
        val topNodeH = boardH * 0.14f
        val topNodeX = boardLeft + (boardW - topNodeW) * 0.5f
        val topNodeY = boardTop + boardH * 0.18f
        drawRect(
            color = tint,
            topLeft = Offset(topNodeX, topNodeY),
            size = Size(topNodeW, topNodeH),
            style = Stroke(width = 1.4.dp.toPx())
        )

        val stemY = topNodeY + topNodeH
        val forkY = stemY + boardH * 0.10f
        val branchY = forkY + boardH * 0.10f
        val leftNodeX = boardLeft + boardW * 0.18f
        val rightNodeX = boardLeft + boardW * 0.62f

        drawLine(color = tint, start = Offset(topNodeX + topNodeW * 0.5f, stemY), end = Offset(topNodeX + topNodeW * 0.5f, forkY), strokeWidth = 1.4.dp.toPx())
        drawLine(color = tint, start = Offset(leftNodeX, forkY), end = Offset(rightNodeX, forkY), strokeWidth = 1.4.dp.toPx())
        drawLine(color = tint, start = Offset(leftNodeX, forkY), end = Offset(leftNodeX, branchY), strokeWidth = 1.4.dp.toPx())
        drawLine(color = tint, start = Offset(rightNodeX, forkY), end = Offset(rightNodeX, branchY), strokeWidth = 1.4.dp.toPx())

        val subNodeW = boardW * 0.28f
        val subNodeH = boardH * 0.14f
        drawRect(
            color = tint,
            topLeft = Offset(leftNodeX - subNodeW * 0.5f, branchY),
            size = Size(subNodeW, subNodeH),
            style = Stroke(width = 1.4.dp.toPx())
        )
        drawRect(
            color = tint,
            topLeft = Offset(rightNodeX - subNodeW * 0.5f, branchY),
            size = Size(subNodeW, subNodeH),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // 4. Circular meter on upper-right
        val meterR = w * 0.18f
        val meterCx = boardLeft + boardW + meterR * 0.35f
        val meterCy = boardTop + boardH * 0.32f

        val elbowY = branchY + subNodeH * 0.5f
        drawLine(color = tint, start = Offset(rightNodeX + subNodeW * 0.5f, elbowY), end = Offset(meterCx, elbowY), strokeWidth = 1.4.dp.toPx())
        drawLine(color = tint, start = Offset(meterCx, elbowY), end = Offset(meterCx, meterCy + meterR), strokeWidth = 1.4.dp.toPx())

        drawCircle(
            color = tint,
            radius = meterR,
            center = Offset(meterCx, meterCy),
            style = Stroke(width = 1.6.dp.toPx())
        )
        drawLine(
            color = tint,
            start = Offset(meterCx, meterCy),
            end = Offset(meterCx + meterR * 0.65f, meterCy - meterR * 0.5f),
            strokeWidth = 1.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Tab 4 (الإعدادات): Open-ended diagonal spanner wrench with two spoked gears.
 */
@Composable
fun SettingsMaintenanceIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        // 1. Upper-left gear
        val g1Cx = cx - w * 0.24f
        val g1Cy = cy - h * 0.24f
        val g1R = w * 0.18f
        drawCircle(color = tint, radius = g1R, center = Offset(g1Cx, g1Cy), style = Stroke(width = 1.6.dp.toPx()))
        drawLine(color = tint, start = Offset(g1Cx - g1R, g1Cy), end = Offset(g1Cx + g1R, g1Cy), strokeWidth = 1.1.dp.toPx())
        drawLine(color = tint, start = Offset(g1Cx, g1Cy - g1R), end = Offset(g1Cx, g1Cy + g1R), strokeWidth = 1.1.dp.toPx())
        drawLine(color = tint, start = Offset(g1Cx - g1R * 0.7f, g1Cy - g1R * 0.7f), end = Offset(g1Cx + g1R * 0.7f, g1Cy + g1R * 0.7f), strokeWidth = 1.1.dp.toPx())
        drawLine(color = tint, start = Offset(g1Cx - g1R * 0.7f, g1Cy + g1R * 0.7f), end = Offset(g1Cx + g1R * 0.7f, g1Cy - g1R * 0.7f), strokeWidth = 1.1.dp.toPx())
        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(g1Cx, g1Cy))

        // 2. Lower-right gear
        val g2Cx = cx + w * 0.24f
        val g2Cy = cy + h * 0.24f
        val g2R = w * 0.17f
        drawCircle(color = tint, radius = g2R, center = Offset(g2Cx, g2Cy), style = Stroke(width = 1.6.dp.toPx()))
        drawLine(color = tint, start = Offset(g2Cx - g2R, g2Cy), end = Offset(g2Cx + g2R, g2Cy), strokeWidth = 1.1.dp.toPx())
        drawLine(color = tint, start = Offset(g2Cx, g2Cy - g2R), end = Offset(g2Cx, g2Cy + g2R), strokeWidth = 1.1.dp.toPx())
        drawLine(color = tint, start = Offset(g2Cx - g2R * 0.7f, g2Cy - g2R * 0.7f), end = Offset(g2Cx + g2R * 0.7f, g2Cy + g2R * 0.7f), strokeWidth = 1.1.dp.toPx())
        drawLine(color = tint, start = Offset(g2Cx - g2R * 0.7f, g2Cy + g2R * 0.7f), end = Offset(g2Cx + g2R * 0.7f, g2Cy - g2R * 0.7f), strokeWidth = 1.1.dp.toPx())
        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(g2Cx, g2Cy))

        // 3. Diagonal wrench handle from bottom-left to top-right
        val startX = cx - w * 0.32f
        val startY = cy + h * 0.32f
        val endX = cx + w * 0.20f
        val endY = cy - h * 0.20f
        drawLine(
            color = tint,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4.2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Bottom ring of wrench
        drawCircle(color = tint, radius = w * 0.12f, center = Offset(startX, startY), style = Stroke(width = 2.dp.toPx()))

        // Open-end wrench head at top-right
        val headCx = endX + w * 0.08f
        val headCy = endY - h * 0.08f
        val headR = w * 0.19f
        val headPath = Path().apply {
            arcTo(
                rect = Rect(headCx - headR, headCy - headR, headCx + headR, headCy + headR),
                startAngleDegrees = 45f,
                sweepAngleDegrees = 270f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(headPath, color = tint)
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Navigation Bar Item Definitions (RTL Order: Index 0 = Rightmost)
// ──────────────────────────────────────────────────────────────────────────────
data class MosaribNavItemData(
    val id: Int,
    val labelLine1: String,
    val labelLine2: String?,
    val testTag: String
)

val mosaribNavItems = listOf(
    MosaribNavItemData(0, "التقارير", null, "tab_reports"),
    MosaribNavItemData(1, "العملاء", null, "tab_customers"),
    MosaribNavItemData(2, "الرئيسية", null, "tab_home"),
    MosaribNavItemData(3, "سجل", "العمليات", "tab_vouchers"),
    MosaribNavItemData(4, "الإعدادات", null, "tab_settings")
)

// ──────────────────────────────────────────────────────────────────────────────
// Main MosaribNavBar Component with Fluid Animated Sliding Notch & Drop
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun MosaribNavBar(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine actual theme mode from theme surface luminance (independent of OS setting)
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // ── Harmonious Theme Colors ────────────────────────────────────────────────
    val barBg = if (isDark) Color(0xFF12242B) else Color(0xFFCDE5E7)
    val barBorderColor = if (isDark) Color(0xFF1E3C47) else Color(0xFFB8DCDF)
    val shadowColor = if (isDark) Color(0xFF00E5FF) else PrimaryTeal

    // Inactive & Active item colors
    val inactiveTint = if (isDark) Color(0xFF7E9FA8) else Color(0xFF1B3D42)
    val activeTint = if (isDark) SecondaryAquaLight else Color(0xFF005662)

    // Center Floating Circle Button colors
    val homeCircleBg = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF00E5FF), Color(0xFF00897B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF5AB6BA), Color(0xFF388E94)))
    }
    val homeDropSilhouette = if (isDark) Color(0xFF071920) else Color(0xFF1B3D42)

    // Calculate animated horizontal fraction for the sliding notch & floating circle
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val targetFraction = if (isRtl) {
        (4 - selectedIndex + 0.5f) / 5f
    } else {
        (selectedIndex + 0.5f) / 5f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "crest_sliding_x"
    )

    val crestedShape = remember(animatedFraction) {
        DynamicCrestedBarShape(
            centerFraction = animatedFraction,
            flatTopDp = 26f,
            crestPeakDp = 5f,
            crestHalfWDp = 50f
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val totalWidth = maxWidth
        val circleSize = 60.dp
        // In RTL, Alignment.TopStart is anchored at the right edge, so distance from start is (1f - fraction)
        val circleX = if (isRtl) {
            (totalWidth * (1f - animatedFraction)) - (circleSize / 2)
        } else {
            (totalWidth * animatedFraction) - (circleSize / 2)
        }

        // ── 1. Full-Width Animated Crested Bar Background ───────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .shadow(
                    elevation = if (isDark) 10.dp else 6.dp,
                    shape = crestedShape,
                    ambientColor = shadowColor.copy(alpha = if (isDark) 0.25f else 0.15f),
                    spotColor = shadowColor.copy(alpha = if (isDark) 0.35f else 0.20f)
                )
                .clip(crestedShape)
                .background(barBg)
                .border(width = 1.dp, color = barBorderColor, shape = crestedShape)
        )

        // ── 2. Navigation Items Row ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mosaribNavItems.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val currentTint by animateColorAsState(
                    targetValue = if (isSelected) activeTint else inactiveTint,
                    animationSpec = tween(200),
                    label = "tint_$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, radius = 32.dp),
                            onClick = { onItemSelected(index) }
                        )
                        .padding(bottom = 2.dp)
                        .testTag(item.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // When selected, the icon is elevated into the floating circle,
                    // so on the bar we smoothly hide the flat icon and leave room for the label!
                    AnimatedVisibility(
                        visible = !isSelected,
                        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.8f),
                        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.8f)
                    ) {
                        Box(
                            modifier = Modifier.size(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            when (index) {
                                0 -> AnalyticsChartIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                                1 -> CustomersBadgeIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                                2 -> HomeDropHouseIcon(dropTint = currentTint, houseTint = barBg, modifier = Modifier.size(28.dp))
                                3 -> OperationsClipboardIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                                4 -> SettingsMaintenanceIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(18.dp))
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Text label (supports 2 lines with proper line height to avoid clipping)
                    if (item.labelLine2 != null) {
                        Text(
                            text = "${item.labelLine1}\n${item.labelLine2}",
                            fontSize = if (isSelected) 9.5.sp else 9.sp,
                            lineHeight = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = currentTint,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    } else {
                        Text(
                            text = item.labelLine1,
                            fontSize = if (isSelected) 10.5.sp else 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = currentTint,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ── 3. Fluid Sliding Floating Drop Button ───────────────────────────────
        // The circle floats along the X axis and carries the active tab's icon!
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = circleX, y = (-12).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 36.dp),
                    onClick = { onItemSelected(selectedIndex) }
                )
                .testTag("floating_tab_indicator"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        spotColor = shadowColor.copy(alpha = if (isDark) 0.45f else 0.35f),
                        ambientColor = shadowColor.copy(alpha = 0.20f)
                    )
                    .clip(CircleShape)
                    .background(homeCircleBg)
                    .border(
                        width = 2.dp,
                        color = if (isDark) SecondaryAqua else Color.White,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Smoothly crossfade between icons as the floating circle travels to the new tab
                AnimatedContent(
                    targetState = selectedIndex,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.75f))
                            .togetherWith(fadeOut(animationSpec = tween(160)) + scaleOut(targetScale = 0.75f))
                    },
                    label = "floating_circle_icon"
                ) { targetIndex ->
                    when (targetIndex) {
                        0 -> AnalyticsChartIcon(tint = Color.White, modifier = Modifier.size(32.dp))
                        1 -> CustomersBadgeIcon(tint = Color.White, modifier = Modifier.size(32.dp))
                        2 -> HomeDropHouseIcon(
                            dropTint = homeDropSilhouette,
                            houseTint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        3 -> OperationsClipboardIcon(tint = Color.White, modifier = Modifier.size(32.dp))
                        4 -> SettingsMaintenanceIcon(tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

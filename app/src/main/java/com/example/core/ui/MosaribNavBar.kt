package com.example.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
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
// Custom Crested Shape for full-width bar with smooth bell-curve center hump
// ──────────────────────────────────────────────────────────────────────────────
class CrestedBarShape(
    private val flatTopDp: Float = 26f,
    private val crestPeakDp: Float = 5f,
    private val crestHalfWDp: Float = 52f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        val flatTop = flatTopDp * density.density
        val peak = crestPeakDp * density.density
        val halfW = crestHalfWDp * density.density

        val path = Path().apply {
            moveTo(0f, flatTop)
            lineTo(cx - halfW, flatTop)
            cubicTo(
                cx - halfW * 0.55f, flatTop,
                cx - halfW * 0.45f, peak,
                cx, peak
            )
            cubicTo(
                cx + halfW * 0.45f, peak,
                cx + halfW * 0.55f, flatTop,
                cx + halfW, flatTop
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
 * Tab 0 (سجلات السقي): Water meter gauge with needle, dial ticks, odometer, and top droplet.
 */
@Composable
fun IrrigationGaugeIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.62f
        val r = w * 0.36f

        // 1. Water meter gauge dial circle
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // 2. Radial scale tick marks around dial
        val angles = listOf(-150f, -125f, -100f, -75f, -50f, -25f, 0f, 25f, 50f)
        for (angleDeg in angles) {
            val rad = Math.toRadians(angleDeg.toDouble())
            val rOuter = r - 1.2.dp.toPx()
            val rInner = r - 4.dp.toPx()
            drawLine(
                color = tint,
                start = Offset(cx + (rOuter * Math.cos(rad)).toFloat(), cy + (rOuter * Math.sin(rad)).toFloat()),
                end = Offset(cx + (rInner * Math.cos(rad)).toFloat(), cy + (rInner * Math.sin(rad)).toFloat()),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 3. Dial needle pointing up-right (~ -45 deg)
        val needleRad = Math.toRadians(-45.0)
        val needleLen = r * 0.65f
        drawLine(
            color = tint,
            start = Offset(cx, cy),
            end = Offset(cx + (needleLen * Math.cos(needleRad)).toFloat(), cy + (needleLen * Math.sin(needleRad)).toFloat()),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(cx, cy))

        // 4. Odometer readout box at bottom
        drawRoundRect(
            color = tint,
            topLeft = Offset(cx - r * 0.45f, cy + r * 0.40f),
            size = Size(r * 0.9f, r * 0.35f),
            cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
            style = Stroke(width = 1.1.dp.toPx())
        )

        // 5. Water droplet perched on top of gauge
        val dropPath = Path().apply {
            val dropTop = h * 0.04f
            val dropBottom = cy - r * 0.42f
            val dropWidth = w * 0.36f
            moveTo(cx, dropTop)
            cubicTo(
                cx + dropWidth * 0.65f, dropTop + (dropBottom - dropTop) * 0.45f,
                cx + dropWidth * 0.55f, dropBottom,
                cx, dropBottom
            )
            cubicTo(
                cx - dropWidth * 0.55f, dropBottom,
                cx - dropWidth * 0.65f, dropTop + (dropBottom - dropTop) * 0.45f,
                cx, dropTop
            )
            close()
        }
        drawPath(path = dropPath, color = tint)

        // Droplet inner highlight
        val arcPath = Path().apply {
            moveTo(cx - w * 0.08f, cy - r * 0.70f)
            quadraticTo(cx - w * 0.03f, cy - r * 0.92f, cx + w * 0.04f, cy - r * 0.88f)
        }
        drawPath(
            path = arcPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
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
    MosaribNavItemData(0, "سجلات", "السقي", "tab_sessions"),
    MosaribNavItemData(1, "العملاء", null, "tab_customers"),
    MosaribNavItemData(2, "الرئيسية", null, "tab_home"),
    MosaribNavItemData(3, "سجل", "العمليات", "tab_vouchers"),
    MosaribNavItemData(4, "الإعدادات", null, "tab_settings")
)

// ──────────────────────────────────────────────────────────────────────────────
// Main MosaribNavBar Component
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
    // Light Theme: Soft fresh mint/cyan background (#CDE5E7) matching reference image
    // Dark Theme: Deep midnight ocean surface (#12242B)
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

    val crestedShape = remember { CrestedBarShape(flatTopDp = 26f, crestPeakDp = 5f, crestHalfWDp = 54f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── 1. Full-Width Crested Bar Background ────────────────────────────────
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
                if (index == 2) {
                    // Center item slot: Label "الرئيسية" situated with ample breathing room under circle
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 32.dp),
                                onClick = { onItemSelected(2) }
                            )
                            .padding(bottom = 4.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = item.labelLine1,
                            fontSize = 11.sp,
                            fontWeight = if (selectedIndex == 2) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (selectedIndex == 2) (if (isDark) SecondaryAqua else Color(0xFF005662)) else inactiveTint,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Side Tab Item
                    val isSelected = selectedIndex == index
                    val animatedScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "scale_$index"
                    )
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
                        // Icon sitting directly on the bar with rich details
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .graphicsLayer {
                                    scaleX = animatedScale
                                    scaleY = animatedScale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (index) {
                                0 -> IrrigationGaugeIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                                1 -> CustomersBadgeIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                                3 -> OperationsClipboardIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                                4 -> SettingsMaintenanceIcon(tint = currentTint, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Text label (supports 2 lines with proper line height to avoid clipping)
                        if (item.labelLine2 != null) {
                            Text(
                                text = "${item.labelLine1}\n${item.labelLine2}",
                                fontSize = 9.sp,
                                lineHeight = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = currentTint,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                        } else {
                            Text(
                                text = item.labelLine1,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = currentTint,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // ── 3. Floating Center Circle Button ────────────────────────────────────
        // Offset y = (-12).dp elevates the button so its bottom edge clears the text "الرئيسية" completely
        val isHomeSelected = selectedIndex == 2
        val homeScale by animateFloatAsState(
            targetValue = if (isHomeSelected) 1.05f else 0.98f,
            animationSpec = tween(240, easing = FastOutSlowInEasing),
            label = "home_scale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .graphicsLayer {
                    scaleX = homeScale
                    scaleY = homeScale
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 36.dp),
                    onClick = { onItemSelected(2) }
                )
                .testTag("tab_home"),
            contentAlignment = Alignment.Center
        ) {
            // Floating circle button with drop shadow and halo ring
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(
                        elevation = if (isHomeSelected) 12.dp else 6.dp,
                        shape = CircleShape,
                        spotColor = shadowColor.copy(alpha = if (isDark) 0.45f else 0.35f),
                        ambientColor = shadowColor.copy(alpha = 0.20f)
                    )
                    .clip(CircleShape)
                    .background(homeCircleBg)
                    .border(
                        width = 2.dp,
                        color = if (isHomeSelected) (if (isDark) SecondaryAqua else Color.White) else Color.White.copy(alpha = 0.6f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Water drop silhouette with white house inside
                HomeDropHouseIcon(
                    dropTint = homeDropSilhouette,
                    houseTint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

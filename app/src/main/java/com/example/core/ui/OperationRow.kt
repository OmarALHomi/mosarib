package com.example.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.mosarib

enum class RowGroupPosition { Single, First, Middle, Last }

data class RowStatus(val text: String, val container: Color, val content: Color)

/**
 * صف عمليات موحد وعصري لقوائم التطبيق (سندات، دورات سقي، شراء ساعات).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OperationRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,                  // who: customer / owner name, or category for general expenses
    subtitle: String,               // what: "سقي 2س 30د · 4:01 م · notes", 1 line, ellipsis
    amountText: String,             // "300", "+ 60", "− 120" (no currency symbol in rows)
    amountColor: Color,
    status: RowStatus? = null,      // small chip under the amount
    trailingNote: String? = null,   // small text under the amount (used by 011 for "الرصيد: X")
    position: RowGroupPosition = RowGroupPosition.Single,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val shape = when (position) {
        RowGroupPosition.Single -> RoundedCornerShape(16.dp)
        RowGroupPosition.First -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        RowGroupPosition.Last -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
        RowGroupPosition.Middle -> RectangleShape
    }

    val backgroundColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 60.dp)
            .clip(shape)
            .background(backgroundColor)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val dividerWidth = 0.5.dp.toPx()
                val radius = 16.dp.toPx()
                val halfStroke = strokeWidth / 2f

                when (position) {
                    RowGroupPosition.Single -> {
                        drawRoundRect(
                            color = outlineColor,
                            topLeft = Offset(halfStroke, halfStroke),
                            size = androidx.compose.ui.geometry.Size(
                                size.width - strokeWidth,
                                size.height - strokeWidth
                            ),
                            cornerRadius = CornerRadius(radius),
                            style = Stroke(strokeWidth)
                        )
                    }
                    RowGroupPosition.First -> {
                        val path = Path().apply {
                            moveTo(halfStroke, size.height)
                            lineTo(halfStroke, radius)
                            arcTo(
                                Rect(halfStroke, halfStroke, 2 * radius - halfStroke, 2 * radius - halfStroke),
                                180f,
                                90f,
                                false
                            )
                            lineTo(size.width - radius, halfStroke)
                            arcTo(
                                Rect(size.width - 2 * radius + halfStroke, halfStroke, size.width - halfStroke, 2 * radius - halfStroke),
                                270f,
                                90f,
                                false
                            )
                            lineTo(size.width - halfStroke, size.height)
                        }
                        drawPath(path, outlineColor, style = Stroke(strokeWidth))

                        // Divider under row, inset 60dp from start
                        val divStart = if (isRtl) 0f else 60.dp.toPx()
                        val divEnd = if (isRtl) size.width - 60.dp.toPx() else size.width
                        drawLine(outlineColor, Offset(divStart, size.height), Offset(divEnd, size.height), strokeWidth = dividerWidth)
                    }
                    RowGroupPosition.Middle -> {
                        drawLine(outlineColor, Offset(halfStroke, 0f), Offset(halfStroke, size.height), strokeWidth = strokeWidth)
                        drawLine(outlineColor, Offset(size.width - halfStroke, 0f), Offset(size.width - halfStroke, size.height), strokeWidth = strokeWidth)

                        // Divider under row, inset 60dp from start
                        val divStart = if (isRtl) 0f else 60.dp.toPx()
                        val divEnd = if (isRtl) size.width - 60.dp.toPx() else size.width
                        drawLine(outlineColor, Offset(divStart, size.height), Offset(divEnd, size.height), strokeWidth = dividerWidth)
                    }
                    RowGroupPosition.Last -> {
                        val path = Path().apply {
                            moveTo(halfStroke, 0f)
                            lineTo(halfStroke, size.height - radius)
                            arcTo(
                                Rect(halfStroke, size.height - 2 * radius + halfStroke, 2 * radius - halfStroke, size.height - halfStroke),
                                180f,
                                -90f,
                                false
                            )
                            lineTo(size.width - radius, size.height - halfStroke)
                            arcTo(
                                Rect(size.width - 2 * radius + halfStroke, size.height - 2 * radius + halfStroke, size.width - halfStroke, size.height - halfStroke),
                                90f,
                                -90f,
                                false
                            )
                            lineTo(size.width - halfStroke, 0f)
                        }
                        drawPath(path, outlineColor, style = Stroke(strokeWidth))
                    }
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "خيارات"
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Leading 36dp circle icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // 2. Middle Column: Title and Subtitle
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 3. Trailing Column: Amount and Status chip / Trailing note
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = amountColor,
                maxLines = 1,
                softWrap = false
            )

            if (status != null) {
                Surface(
                    color = status.container,
                    shape = CircleShape
                ) {
                    Text(
                        text = status.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = status.content,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            } else if (!trailingNote.isNullOrBlank()) {
                Text(
                    text = trailingNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.mosarib.textMuted,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

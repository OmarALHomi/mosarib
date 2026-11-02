package com.example.core.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.core.speech.SpeakButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.core.util.Formatters
import com.example.ui.theme.mosarib
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Shared component displaying a financial amount with a subtle dashed underline.
 * Tapping (or long-pressing) opens a persistent RichTooltip showing the exact amount in words.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AmountWithWords(
    amount: Double,
    currency: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    prefix: String = "" // e.g. "+ " / "− " / "عليه: "
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState(isPersistent = true)

    val formattedNumber = Formatters.formatCurrency(amount, currency)
    val displayAmount = if (prefix.isEmpty()) formattedNumber else "$prefix$formattedNumber"
    val arabicWords = Formatters.amountToArabicWords(abs(amount), currency)

    val density = LocalDensity.current
    val strokeWidth = with(density) { 1.dp.toPx() }
    val dashPx = with(density) { 3.dp.toPx() }
    val underlineColor = color.copy(alpha = 0.4f)
    val pathEffect = remember(dashPx) {
        PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx))
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(),
        tooltip = {
            RichTooltip(
                title = {
                    Text(
                        text = "المبلغ كتابةً",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.mosarib.textMuted
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.mosarib.textMuted
                ),
                text = {
                    Column {
                        Text(
                            text = arabicWords,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            SpeakButton(
                                text = { arabicWords }
                            )
                        }
                    }
                }
            )
        },
        state = tooltipState,
        modifier = modifier
    ) {
        val showTooltip: () -> Unit = {
            scope.launch {
                tooltipState.show()
            }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        Text(
            text = displayAmount,
            style = style.copy(color = color),
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "عرض المبلغ كتابةً",
                    onClick = { showTooltip() },
                    onLongClick = { showTooltip() }
                )
                .drawBehind {
                    val y = size.height - strokeWidth / 2f
                    drawLine(
                        color = underlineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth,
                        pathEffect = pathEffect
                    )
                }
                .semantics {
                    contentDescription = "$displayAmount، $arabicWords"
                }
        )
    }
}

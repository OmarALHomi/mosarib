package com.example.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.example.core.util.Formatters

/**
 * Small animated amount that smoothly counts from the previous numeric value to the new one over 400ms.
 * Respects system animation settings.
 */
@Composable
fun AnimatedAmount(
    value: Double,
    currencySymbol: String = "ر.ي",
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val enabled = areAnimationsEnabled()
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = if (enabled) {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        } else {
            snap()
        },
        label = "animated_amount"
    )

    Text(
        text = Formatters.formatCurrency(animatedValue.toDouble(), currencySymbol),
        style = style,
        color = color,
        maxLines = maxLines,
        softWrap = false,
        overflow = overflow,
        modifier = modifier
    )
}

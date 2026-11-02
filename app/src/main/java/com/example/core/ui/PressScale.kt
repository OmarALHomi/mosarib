package com.example.core.ui

import android.animation.ValueAnimator
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Checks if system animators are enabled.
 * Safely guards against missing API on older Android versions and unit test environments.
 */
fun areAnimationsEnabled(): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ValueAnimator.areAnimatorsEnabled()
        } else {
            true
        }
    } catch (_: Throwable) {
        true
    }
}

/**
 * Scales the component down to [targetScale] while pressed, springing back on release.
 */
fun Modifier.pressScale(
    targetScale: Float = 0.98f,
    interactionSource: InteractionSource
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val enabled = areAnimationsEnabled()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1f,
        animationSpec = if (enabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            snap()
        },
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Convenience overload that manages its own interaction source.
 */
fun Modifier.pressScale(
    targetScale: Float = 0.98f
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.pressScale(targetScale = targetScale, interactionSource = interactionSource)
}

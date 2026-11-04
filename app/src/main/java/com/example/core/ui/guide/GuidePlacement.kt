package com.example.core.ui.guide

import androidx.compose.ui.geometry.Rect

data class GuideRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

fun Rect.toGuideRect(): GuideRect = GuideRect(left, top, right, bottom)

data class BubblePlacement(
    val x: Float,
    val y: Float,
    val isArrowOnTop: Boolean,
    val arrowX: Float
)

object GuidePlacement {

    /**
     * Calculates the position of the tooltip bubble and arrow relative to the container.
     * Pure function with no platform dependencies:
     * - Target near bottom: bubble is placed above the target.
     * - Target near top or middle with room below: bubble is placed below the target.
     * - Target near edge: bubble is kept strictly within [margin, screenWidth - bubbleWidth - margin].
     * - Arrow points towards target center, clamped to the bubble body.
     */
    fun calculateBubblePlacement(
        target: GuideRect,
        bubbleWidth: Float,
        bubbleHeight: Float,
        screenWidth: Float,
        screenHeight: Float,
        margin: Float = 16f,
        arrowHeight: Float = 8f,
        arrowWidth: Float = 16f
    ): BubblePlacement {
        val spaceBelow = screenHeight - target.bottom - margin
        val spaceNeeded = bubbleHeight + arrowHeight

        val isArrowOnTop = spaceBelow >= spaceNeeded
        val y = if (isArrowOnTop) {
            (target.bottom + arrowHeight).coerceIn(margin, (screenHeight - bubbleHeight - margin).coerceAtLeast(margin))
        } else {
            (target.top - arrowHeight - bubbleHeight).coerceIn(margin, (screenHeight - bubbleHeight - margin).coerceAtLeast(margin))
        }

        val idealX = target.centerX - (bubbleWidth / 2f)
        val minX = margin
        val maxX = (screenWidth - bubbleWidth - margin).coerceAtLeast(minX)
        val x = idealX.coerceIn(minX, maxX)

        val relativeTargetCenterX = target.centerX - x
        val minArrowPadding = (arrowWidth / 2f + 12f).coerceAtMost(bubbleWidth / 2f)
        val maxArrowPadding = (bubbleWidth - arrowWidth / 2f - 12f).coerceAtLeast(minArrowPadding)
        val arrowX = relativeTargetCenterX.coerceIn(minArrowPadding, maxArrowPadding)

        return BubblePlacement(
            x = x,
            y = y,
            isArrowOnTop = isArrowOnTop,
            arrowX = arrowX
        )
    }
}

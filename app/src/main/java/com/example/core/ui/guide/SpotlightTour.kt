package com.example.core.ui.guide

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.speech.SpeakButton
import com.example.ui.theme.PrimaryTeal
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Full-screen spotlight tour overlay.
 * Points to registered UI targets, draws a dimmed scrim with a cut-out and pulsing ring,
 * and displays an interactive bubble with Western numerals, Arabic Cairo typography, and optional speech.
 */
@Composable
fun SpotlightTour(
    steps: List<GuideStep>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    // A focusable full-window popup, so the tour sits above bottom sheets and the bottom bar.
    Popup(
        popupPositionProvider = WindowOriginPositionProvider,
        onDismissRequest = onFinish,
        properties = PopupProperties(focusable = true, clippingEnabled = false)
    ) {
        SpotlightTourContent(steps, onFinish, modifier)
    }
}

private object WindowOriginPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset.Zero
}

@Composable
private fun SpotlightTourContent(
    steps: List<GuideStep>,
    onFinish: () -> Unit,
    modifier: Modifier
) {
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    var containerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val registry = LocalGuideRegistry.current
    val density = LocalDensity.current

    // Back button exits tour
    BackHandler {
        onFinish()
    }

    val currentStep = steps.getOrNull(currentStepIndex)
    if (currentStep == null) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    // Scroll the step's target into view first (forms are scrollable)
    LaunchedEffect(currentStepIndex) {
        registry.bringIntoView(currentStep.targetId)
    }

    // Target bounds relative to container
    val targetRect = registry.getTargetBounds(currentStep.targetId, containerCoordinates)

    // Handle missing or off-screen target: skip silently
    LaunchedEffect(currentStepIndex, targetRect, containerCoordinates) {
        if (containerCoordinates != null && targetRect == null) {
            // Wait up to 300ms for lazy layout
            delay(300)
            val rechecked = registry.getTargetBounds(currentStep.targetId, containerCoordinates)
            if (rechecked == null || rechecked.width <= 0f || rechecked.height <= 0f) {
                if (currentStepIndex < steps.size - 1) {
                    currentStepIndex++
                } else {
                    onFinish()
                }
            }
        }
    }

    val paddingPx = with(density) { 8.dp.toPx() }
    val rawCutout = if (targetRect != null && targetRect.width > 0f && targetRect.height > 0f) {
        Rect(
            left = targetRect.left - paddingPx,
            top = targetRect.top - paddingPx,
            right = targetRect.right + paddingPx,
            bottom = targetRect.bottom + paddingPx
        )
    } else {
        null
    }

    // Animated cut-out transitions between steps
    val animLeft by animateFloatAsState(targetValue = rawCutout?.left ?: 0f, animationSpec = tween(220), label = "cutout_left")
    val animTop by animateFloatAsState(targetValue = rawCutout?.top ?: 0f, animationSpec = tween(220), label = "cutout_top")
    val animRight by animateFloatAsState(targetValue = rawCutout?.right ?: 0f, animationSpec = tween(220), label = "cutout_right")
    val animBottom by animateFloatAsState(targetValue = rawCutout?.bottom ?: 0f, animationSpec = tween(220), label = "cutout_bottom")

    val activeCutout = if (rawCutout != null) {
        Rect(animLeft, animTop, animRight, animBottom)
    } else null

    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")
    val pulseExpand by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = with(density) { 6.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_expand"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    fun advanceNext() {
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        } else {
            onFinish()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { containerCoordinates = it }
            .pointerInput(activeCutout, currentStepIndex) {
                detectTapGestures { offset ->
                    if (activeCutout != null && activeCutout.contains(offset)) {
                        // Tapping the cut-out = «التالي»
                        advanceNext()
                    }
                }
            }
    ) {
        val containerSize = containerCoordinates?.size
        val screenWidth = containerSize?.width?.toFloat() ?: 0f
        val screenHeight = containerSize?.height?.toFloat() ?: 0f

        // 1. Dimmed scrim with rounded cut-out and pulse ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            // 70% black scrim
            drawRect(Color.Black.copy(alpha = 0.70f))

            if (activeCutout != null && activeCutout.width > 0f && activeCutout.height > 0f) {
                val cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())

                // Rounded clear cut-out
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(activeCutout.left, activeCutout.top),
                    size = Size(activeCutout.width, activeCutout.height),
                    cornerRadius = cornerRadius,
                    blendMode = BlendMode.Clear
                )

                // Soft pulsing ring around the cut-out
                val ringOffset = Offset(activeCutout.left - pulseExpand, activeCutout.top - pulseExpand)
                val ringSize = Size(activeCutout.width + pulseExpand * 2f, activeCutout.height + pulseExpand * 2f)
                drawRoundRect(
                    color = PrimaryTeal.copy(alpha = pulseAlpha),
                    topLeft = ringOffset,
                    size = ringSize,
                    cornerRadius = CornerRadius(cornerRadius.x + pulseExpand, cornerRadius.y + pulseExpand),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // 2. Interactive Tooltip Bubble
        if (activeCutout != null && screenWidth > 0f && screenHeight > 0f) {
            var bubbleMeasuredWidth by remember { mutableStateOf(with(density) { 320.dp.toPx() }) }
            var bubbleMeasuredHeight by remember { mutableStateOf(with(density) { 180.dp.toPx() }) }

            val placement = GuidePlacement.calculateBubblePlacement(
                target = activeCutout.toGuideRect(),
                bubbleWidth = bubbleMeasuredWidth,
                bubbleHeight = bubbleMeasuredHeight,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                margin = with(density) { 16.dp.toPx() },
                arrowHeight = with(density) { 8.dp.toPx() },
                arrowWidth = with(density) { 16.dp.toPx() }
            )

            val bubbleBg = MaterialTheme.colorScheme.surface
            val arrowColor = bubbleBg

            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(placement.x.roundToInt(), placement.y.roundToInt()) }
                    .widthIn(min = 280.dp, max = 340.dp)
                    .onGloballyPositioned { coords ->
                        bubbleMeasuredWidth = coords.size.width.toFloat()
                        bubbleMeasuredHeight = coords.size.height.toFloat()
                    }
                    .semantics {
                        contentDescription = "دليل توضيحي: ${currentStep.title}. ${currentStep.description}"
                    }
            ) {
                Column {
                    // Upward pointing arrow if bubble is placed below target
                    if (placement.isArrowOnTop) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            val arrowPath = Path().apply {
                                moveTo(placement.arrowX, 0f)
                                lineTo(placement.arrowX - 8.dp.toPx(), 8.dp.toPx())
                                lineTo(placement.arrowX + 8.dp.toPx(), 8.dp.toPx())
                                close()
                            }
                            drawPath(arrowPath, color = arrowColor)
                        }
                    }

                    // Main bubble card
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = bubbleBg,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                    ) {
                        AnimatedContent(
                            targetState = currentStepIndex,
                            transitionSpec = {
                                (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.94f)) togetherWith
                                        (fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.94f))
                            },
                            label = "step_content_anim"
                        ) { stepIdx ->
                            val step = steps[stepIdx]
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Header: Title + SpeakButton + Step Counter
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = step.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        SpeakButton(
                                            text = { step.spokenText },
                                            iconSize = 18.dp
                                        )
                                    }

                                    // Western digits counter: e.g. "2 من 4"
                                    Text(
                                        text = "${stepIdx + 1} من ${steps.size}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Description
                                Text(
                                    text = step.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 22.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Action Buttons (min 48dp height for accessibility)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { advanceNext() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryTeal,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = if (stepIdx == steps.size - 1) "إنهاء" else "التالي",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { onFinish() },
                                        modifier = Modifier.height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "تخطّي",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Downward pointing arrow if bubble is placed above target
                    if (!placement.isArrowOnTop) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            val arrowPath = Path().apply {
                                moveTo(placement.arrowX, 8.dp.toPx())
                                lineTo(placement.arrowX - 8.dp.toPx(), 0f)
                                lineTo(placement.arrowX + 8.dp.toPx(), 0f)
                                close()
                            }
                            drawPath(arrowPath, color = arrowColor)
                        }
                    }
                }
            }
        }
    }
}

package com.example.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Phase 1: Drop falls from pipe (0→1 over 900ms)
    val dropProgress = remember { Animatable(0f) }
    // Phase 2: Ripple + logo fade-in
    var showLogo by remember { mutableStateOf(false) }
    val logoAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.7f) }

    // Ripple animation (starts after drop lands)
    var rippleActive by remember { mutableStateOf(false) }
    val rippleRadius = remember { Animatable(0f) }
    val rippleAlpha = remember { Animatable(0f) }

    // Second ripple for layered effect
    val ripple2Radius = remember { Animatable(0f) }
    val ripple2Alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Phase 1: Drop falls
        dropProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(900, easing = FastOutSlowInEasing)
        )

        // Phase 2: Impact ripple + logo
        rippleActive = true
        showLogo = true

        coroutineScope {
            // Launch ripple 1
            launch {
                rippleAlpha.snapTo(0.7f)
                rippleRadius.animateTo(250f, tween(800, easing = FastOutSlowInEasing))
            }
            launch {
                rippleAlpha.animateTo(0f, tween(800, easing = FastOutSlowInEasing))
            }
            // Launch ripple 2 (delayed)
            launch {
                delay(200)
                ripple2Alpha.snapTo(0.5f)
                ripple2Radius.animateTo(180f, tween(700, easing = FastOutSlowInEasing))
            }
            launch {
                delay(200)
                ripple2Alpha.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
            }
            // Logo appears
            launch {
                logoAlpha.animateTo(1f, tween(500))
            }
            launch {
                logoScale.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
        }

        delay(1200)
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF003844),
                        Color(0xFF0B171D),
                        Color(0xFF070F14)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Canvas: Pipe + falling drop + ripple
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // ── Pipe (horizontal bar at ~18% from top) ──
            val pipeY = h * 0.16f
            val pipeHeight = 18.dp.toPx()
            val pipeWidth = w * 0.45f
            val pipeLeft = (w - pipeWidth) / 2f

            // Pipe body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2A4A52), Color(0xFF1A3038)),
                    startY = pipeY,
                    endY = pipeY + pipeHeight
                ),
                topLeft = Offset(pipeLeft, pipeY),
                size = Size(pipeWidth, pipeHeight),
                cornerRadius = CornerRadius(6.dp.toPx())
            )
            // Pipe highlight (metallic shine)
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(pipeLeft + 4.dp.toPx(), pipeY + 2.dp.toPx()),
                size = Size(pipeWidth - 8.dp.toPx(), 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
            // Pipe nozzle (short vertical piece)
            val nozzleWidth = 12.dp.toPx()
            val nozzleHeight = 14.dp.toPx()
            val nozzleX = w / 2f - nozzleWidth / 2f
            val nozzleY = pipeY + pipeHeight
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF2A4A52), Color(0xFF1E3840)),
                    startY = nozzleY,
                    endY = nozzleY + nozzleHeight
                ),
                topLeft = Offset(nozzleX, nozzleY),
                size = Size(nozzleWidth, nozzleHeight),
                cornerRadius = CornerRadius(3.dp.toPx())
            )

            // ── Water Drop ──
            val dropStartY = nozzleY + nozzleHeight
            val dropEndY = h * 0.48f // lands at center
            val currentDropY = dropStartY + (dropEndY - dropStartY) * dropProgress.value
            val dropX = w / 2f

            if (dropProgress.value < 1f) {
                // Draw teardrop shape
                val dropSize = 14.dp.toPx()
                val path = Path().apply {
                    moveTo(dropX, currentDropY - dropSize * 1.5f)
                    cubicTo(
                        dropX + dropSize, currentDropY - dropSize * 0.3f,
                        dropX + dropSize * 0.7f, currentDropY + dropSize * 0.5f,
                        dropX, currentDropY + dropSize * 0.7f
                    )
                    cubicTo(
                        dropX - dropSize * 0.7f, currentDropY + dropSize * 0.5f,
                        dropX - dropSize, currentDropY - dropSize * 0.3f,
                        dropX, currentDropY - dropSize * 1.5f
                    )
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(
                            SecondaryAqua.copy(alpha = 0.9f),
                            PrimaryTeal.copy(alpha = 0.95f)
                        ),
                        startY = currentDropY - dropSize * 1.5f,
                        endY = currentDropY + dropSize * 0.7f
                    )
                )
                // Drop highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 3.dp.toPx(),
                    center = Offset(dropX - 3.dp.toPx(), currentDropY - dropSize * 0.5f)
                )

                // Tiny drips still forming on nozzle
                val dripSize = 4.dp.toPx() * (1f - dropProgress.value)
                if (dripSize > 0.5f) {
                    drawCircle(
                        color = SecondaryAqua.copy(alpha = 0.5f * (1f - dropProgress.value)),
                        radius = dripSize,
                        center = Offset(dropX, dropStartY + 2.dp.toPx())
                    )
                }
            }

            // ── Impact Ripples ──
            if (rippleActive) {
                val impactCenter = Offset(dropX, dropEndY)
                drawCircle(
                    color = SecondaryAqua.copy(alpha = rippleAlpha.value),
                    radius = rippleRadius.value,
                    center = impactCenter,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = PrimaryTeal.copy(alpha = ripple2Alpha.value),
                    radius = ripple2Radius.value,
                    center = impactCenter,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Logo + Text (fades in after drop lands)
        if (showLogo) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .padding(top = 40.dp) // offset slightly below center
            ) {
                // Main Logo Emblem Box
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .shadow(16.dp, CircleShape, spotColor = SecondaryAqua)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryTeal, AccentEmerald)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Arabic Title: بيننا
                Text(
                    text = "بيننا",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 38.sp,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // English Title: Baynana
                Text(
                    text = "Baynana",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = AccentGold,
                        fontSize = 18.sp,
                        letterSpacing = 3.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Arabic Tagline
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "مستودع حساباتك ومعاملاتك",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB0C9D4),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }

        // Bottom Loading text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "جاري التحميل...",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF78909C),
                    fontSize = 11.sp
                )
            )
        }
    }
}

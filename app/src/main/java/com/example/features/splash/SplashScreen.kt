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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "water_ripple")
    val rippleRadius by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 180f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_radius"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha"
    )

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500)
        )
        delay(1600)
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
        // Water concentric expanding ripple canvas
        Canvas(modifier = Modifier.size(360.dp)) {
            val centerOffset = center
            drawCircle(
                color = SecondaryAqua.copy(alpha = rippleAlpha),
                radius = rippleRadius,
                center = centerOffset,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = PrimaryTeal.copy(alpha = (rippleAlpha * 0.7f).coerceIn(0f, 1f)),
                radius = (rippleRadius * 0.7f),
                center = centerOffset,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .padding(24.dp)
        ) {
            // Main Logo Emblem Box
            Box(
                modifier = Modifier
                    .size(92.dp)
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
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Arabic Title: المُسَرِّبْ
            Text(
                text = "المُسَرِّبْ",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    fontSize = 34.sp,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // English Title: Mosarib
            Text(
                text = "Mosarib",
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
                    text = "نظام إدارة وتوزيع مياه الآبار والري",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB0C9D4),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // Bottom Loading wave
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

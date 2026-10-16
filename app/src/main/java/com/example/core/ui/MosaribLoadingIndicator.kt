package com.example.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

/**
 * مؤشر تحميل مائي أنيق وانسيابي وخفيف الوزن بالكامل لتطبيق "مُسَرِّب".
 * يجمع بين حلقة دوران مائية متدرجة وأيقونة قطرة ماء ناعمة في المركز.
 */
@Composable
fun MosaribWaterLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    primaryColor: Color = PrimaryTeal,
    accentColor: Color = SecondaryAqua,
    highlightColor: Color = AccentEmerald
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MosaribWaterRing")

    // 1. زاوية دوران حلقة التدفق المائي
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    // 2. نبض ناعم جداً لقطرة الماء المركزية
    val dropletScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DropletPulse"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // حلقة الجريان المائي الدائرية
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = this.size.minDimension * 0.10f
            val arcSize = this.size.minDimension - strokeWidth
            val arcTopLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

            // مسار الحلقة الخلفية الخافت
            drawCircle(
                color = primaryColor.copy(alpha = 0.12f),
                radius = arcSize / 2f,
                style = Stroke(width = strokeWidth * 0.8f)
            )

            // قوس الجريان الدوار المتدرج
            rotate(rotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.5f to accentColor.copy(alpha = 0.4f),
                        0.85f to primaryColor,
                        1.0f to highlightColor
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // قطرة ضوئية في نهاية القوس
                val endAngleRad = Math.toRadians(270.0)
                val radius = arcSize / 2f
                val beadX = (this.size.width / 2f) + (radius * Math.cos(endAngleRad)).toFloat()
                val beadY = (this.size.height / 2f) + (radius * Math.sin(endAngleRad)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = strokeWidth * 0.65f,
                    center = Offset(beadX, beadY)
                )
            }
        }

        // قطرة الماء الأنيقة في المركز
        Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(size * 0.44f * dropletScale)
        )
    }
}

/**
 * نافذة تحميل نظيفة ومدمجة وعصرية بدون أي تعقيد.
 */
@Composable
fun MosaribLoadingDialog(
    message: String = "جاري معالجة البيانات...",
    subMessage: String? = "لحظات وتكتمل العملية بدقة",
    title: String? = null,
    subTitle: String? = null,
    onDismissRequest: () -> Unit = {}
) {
    val displayMessage = title ?: message
    val displaySubMessage = subTitle ?: subMessage
    val isDark = isAppDark()

    val infiniteTransition = rememberInfiniteTransition(label = "DotsAnim")
    val dotStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DotStep"
    )
    val animatedDots = when (dotStep.toInt()) {
        1 -> ".  "
        2 -> ".. "
        3 -> "..."
        else -> "   "
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF131F24) else Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF24363F) else Color(0xFFE2E8F0)),
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // مؤشر التحميل المائي الأنيق
                MosaribWaterLoadingIndicator(
                    size = 54.dp,
                    primaryColor = PrimaryTeal,
                    accentColor = SecondaryAqua,
                    highlightColor = AccentEmerald
                )

                Spacer(modifier = Modifier.height(16.dp))

                // العنوان مع النقاط المتتابعة
                Text(
                    text = "$displayMessage $animatedDots",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (!displaySubMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displaySubMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * مؤشر مائي مصغر ومناسب للأزرار والقوائم.
 */
@Composable
fun MosaribInlineLoading(
    modifier: Modifier = Modifier,
    text: String? = null,
    size: Dp = 20.dp,
    primaryColor: Color = PrimaryTeal,
    accentColor: Color = SecondaryAqua,
    highlightColor: Color = AccentEmerald
) {
    Row(
        modifier = modifier.padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        MosaribWaterLoadingIndicator(
            size = size,
            primaryColor = primaryColor,
            accentColor = accentColor,
            highlightColor = highlightColor
        )
        if (!text.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = primaryColor,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

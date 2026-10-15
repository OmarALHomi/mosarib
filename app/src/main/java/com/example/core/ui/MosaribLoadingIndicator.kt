package com.example.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import kotlin.math.sin

/**
 * مؤشر تحميل ابتكاري خاص بتطبيق "مُسَرِّب" (Mosarib Flow Indicator).
 * مستوحى من حركة تدفق مياه الآبار وتموجات الساقية عند انسياب قطرة الماء.
 * خفيف الوزن بنسبة 100%، بدون أي مكتبات خارجية، ويعتمد بالكامل على الرسم الرياضي المباشر عبر الـ Canvas.
 */
@Composable
fun MosaribWaterLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    primaryColor: Color = PrimaryTeal,
    accentColor: Color = SecondaryAqua,
    highlightColor: Color = AccentEmerald
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MosaribWaterFlow")

    // 1. زاوية دوران تدفق مجرى الساقية الدائرية
    val streamRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "StreamRotation"
    )

    // 2. طور اتساع تموجات الماء (Water Ripple Phase 0f -> 1f)
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RipplePhase"
    )

    // 3. حركة تنفس وارتداد قطرة الماء المركزية (Water Droplet Physics Bounce)
    val dropletBounce by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DropletBounce"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseRadius = this.size.minDimension / 2f

        // A. رسم تموجات الماء الحلقية المتسعة (Ripples)
        drawWaterRipples(
            center = center,
            maxRadius = baseRadius * 0.95f,
            phase = ripplePhase,
            rippleColor = accentColor
        )

        // B. رسم مجرى مياه الساقية الدوار المنحني (Circular Irrigation Stream)
        rotate(streamRotation, pivot = center) {
            val streamStrokeWidth = baseRadius * 0.12f
            val streamArcRadius = baseRadius * 0.78f
            val arcTopLeft = Offset(center.x - streamArcRadius, center.y - streamArcRadius)
            val arcSize = Size(streamArcRadius * 2f, streamArcRadius * 2f)

            // مجرى الساقية المتدرج (قوس التدفق الأول)
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.4f to accentColor.copy(alpha = 0.3f),
                    0.85f to primaryColor,
                    1.0f to highlightColor,
                    center = center
                ),
                startAngle = 0f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = streamStrokeWidth, cap = StrokeCap.Round)
            )

            // قطرة فقاعة مائية جارية في المدار (Orbiting Water Bead)
            val beadRadius = streamStrokeWidth * 0.65f
            val beadOffset = Offset(center.x + streamArcRadius, center.y)
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = beadRadius,
                center = beadOffset
            )
        }

        // C. رسم قطرة الماء الكريستالية النابضة في المركز (Center Crystal Droplet)
        val dropScale = 1f + (dropletBounce * 0.08f)
        val dropVerticalOffset = dropletBounce * (baseRadius * 0.06f)
        val dropletCenter = Offset(center.x, center.y + dropVerticalOffset)
        val dropletSize = baseRadius * 0.42f * dropScale

        drawCrystalDroplet(
            center = dropletCenter,
            size = dropletSize,
            primaryColor = primaryColor,
            accentColor = accentColor
        )
    }
}

/**
 * رسم حلقات تموج الماء المتتالية الناتجة عن قطرة الساقية.
 */
private fun DrawScope.drawWaterRipples(
    center: Offset,
    maxRadius: Float,
    phase: Float,
    rippleColor: Color
) {
    // 3 حلقات متداخلة بأطوار متتالية
    val rippleCount = 3
    for (i in 0 until rippleCount) {
        val currentPhase = (phase + (i.toFloat() / rippleCount)) % 1f
        val radius = maxRadius * (0.35f + 0.65f * currentPhase)
        // شفافية متدرجة تبدأ ناعمة وتتلاشى مع اتساع الحلقة
        val alpha = (1f - currentPhase).coerceIn(0f, 1f) * 0.35f

        drawCircle(
            color = rippleColor.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = maxRadius * 0.035f)
        )
    }
}

/**
 * رسم قطرة الماء الكريستالية في مركز المؤشر بواسطة مسار Bezier انسيابي دقيق.
 */
private fun DrawScope.drawCrystalDroplet(
    center: Offset,
    size: Float,
    primaryColor: Color,
    accentColor: Color
) {
    val dropPath = Path().apply {
        val topY = center.y - (size * 1.15f)
        val bottomY = center.y + (size * 0.85f)
        val leftX = center.x - (size * 0.72f)
        val rightX = center.x + (size * 0.72f)

        // رأس القطرة العلوي الحاد
        moveTo(center.x, topY)

        // القوس الانسيابي الأيسر نحو قاع القطرة الممتلئ
        cubicTo(
            center.x - (size * 0.2f), topY + (size * 0.5f),
            leftX, bottomY - (size * 0.45f),
            center.x, bottomY
        )

        // القوس الانسيابي الأيمن عائداً إلى القمة
        cubicTo(
            rightX, bottomY - (size * 0.45f),
            center.x + (size * 0.2f), topY + (size * 0.5f),
            center.x, topY
        )
        close()
    }

    // تدرج لوني مائي للقطرة (من الأزرق المائي إلى الفيروزي العميق)
    val dropBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.95f),
            primaryColor,
            primaryColor.copy(alpha = 0.85f)
        ),
        startY = center.y - size,
        endY = center.y + size
    )

    drawPath(path = dropPath, brush = dropBrush)

    // لمعة ضوئية كريستالية صغيرة داخل القطرة تعطي عمقاً زجاجياً (Specular Highlight)
    drawCircle(
        color = Color.White.copy(alpha = 0.75f),
        radius = size * 0.16f,
        center = Offset(center.x - (size * 0.18f), center.y - (size * 0.18f))
    )
}

/**
 * نافذة تحميل غامرة زجاجية كاملة مخصصة (Full Screen Glassmorphic Loading Dialog).
 * يمكن استدعاؤها في أي وقت عند إجراء مزامنة، استعادة نسخ، أو معالجة العمليات الحسابية الضخمة.
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF131F24).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.96f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 28.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // مؤشر قطرة الساقية الإبداعي
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PrimaryTeal.copy(alpha = if (isDark) 0.18f else 0.12f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MosaribWaterLoadingIndicator(
                        size = 72.dp,
                        primaryColor = PrimaryTeal,
                        accentColor = SecondaryAqua,
                        highlightColor = AccentEmerald
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // العنوان الرئيسي للتحميل
                Text(
                    text = displayMessage,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.3.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (!displaySubMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displaySubMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * ويدجت تحميل مضغوط ومناسب للبطاقات والقوائم وعمليات الحفظ السريعة.
 */
@Composable
fun MosaribInlineLoading(
    modifier: Modifier = Modifier,
    text: String? = null,
    size: Dp = 32.dp,
    primaryColor: Color = PrimaryTeal,
    accentColor: Color = SecondaryAqua,
    highlightColor: Color = AccentEmerald
) {
    Column(
        modifier = modifier.padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MosaribWaterLoadingIndicator(
            size = size,
            primaryColor = primaryColor,
            accentColor = accentColor,
            highlightColor = highlightColor
        )
        if (!text.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

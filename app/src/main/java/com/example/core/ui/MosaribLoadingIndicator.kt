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
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
import kotlin.math.PI
import kotlin.math.sin

/**
 * تحفة تحميل ابتكارية خاصة بتطبيق "مُسَرِّب" (The Living Water Droplet & Liquid Waves Engine).
 *
 * تجسد الفكرة الحقيقية لـ "مُسَرِّب" (توزيع المياه، الآبار، السواقي):
 * 1. قطرة ماء زجاجية كريستالية ضخمة وأنيقة ومضاءة بهالة نيون مائية.
 * 2. أمواج ماء سائلة حية ومتحركة داخل القطرة (Dual Dynamic Sine Waves) مع رغوة التدفق.
 * 3. فقاعات مياه طبيعية تتصاعد وتسبح داخل السائل من القاع للأعلى.
 * 4. قطرات ندى متساقطة دورياً من الساقية نحو قلب القطرة لتثير موجة التدفق.
 * 5. تموجات مائية أفقية تحت القاعدة تعكس سطح بركة الماء.
 *
 * كل ذلك برسم رياضي مباشر على Canvas بأداء فائق 60fps وبدون أي مكتبة خارجية.
 */
@Composable
fun MosaribWaterLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    primaryColor: Color = PrimaryTeal,
    accentColor: Color = SecondaryAqua,
    highlightColor: Color = AccentEmerald
) {
    val infiniteTransition = rememberInfiniteTransition(label = "MosaribMasterWater")

    // 1. طور جريان الموجة الأولى الأساسية (Front Wave Phase)
    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "FrontWavePhase"
    )

    // 2. طور جريان الموجة الخلفية لإعطاء عمق مائي ثلاثي الأبعاد (Back Wave Phase)
    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BackWavePhase"
    )

    // 3. منسوب امتلاء الماء داخل القطرة (Water Level Breathing 52% -> 66%)
    val waterLevelRatio by infiniteTransition.animateFloat(
        initialValue = 0.54f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaterLevelBreathing"
    )

    // 4. مسار صعود الفقاعات المائية من الأسفل للأعلى (0f -> 1f)
    val bubbleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BubblesRising"
    )

    // 5. قطرة الماء الدورية المتساقطة من الساقية العلوية (0f -> 1f)
    val dripProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DrippingDroplet"
    )

    // 6. هالة التموجات السفلية المتسعة (Ground Ripples)
    val groundRippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "GroundRipples"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // المركز الأساسي للقطرة
        val dropCenterX = w * 0.5f
        val dropCenterY = h * 0.54f
        val dropWidth = w * 0.64f
        val dropHeight = h * 0.72f

        // A. رسم تموجات القاعدة الأرضية (تموجات بركة الساقية تحت القطرة)
        drawGroundRipples(
            centerX = dropCenterX,
            centerY = h * 0.92f,
            maxRadiusX = w * 0.44f,
            maxRadiusY = h * 0.08f,
            progress = groundRippleProgress,
            color = accentColor
        )

        // B. رسم هالة النور المائي الخلفية المشعة (Bioluminescent Backlight Glow)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.28f),
                    primaryColor.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(dropCenterX, dropCenterY),
                radius = dropHeight * 0.65f
            ),
            center = Offset(dropCenterX, dropCenterY),
            radius = dropHeight * 0.65f
        )

        // C. بناء مسار القطرة الكريستالية الهندسي (Teardrop Geometry Path)
        val dropletPath = createTeardropPath(
            centerX = dropCenterX,
            centerY = dropCenterY,
            width = dropWidth,
            height = dropHeight
        )

        // D. قص الرسم بداخل القطرة لرسم السائل والأمواج والفقاعات (Clipped Liquid Simulation)
        clipPath(dropletPath) {
            // 1. خلفية زجاج القطرة الداخلي
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF03222A).copy(alpha = 0.65f),
                        Color(0xFF07333B).copy(alpha = 0.85f)
                    )
                )
            )

            val baseWaterY = dropCenterY + (dropHeight * 0.5f) - (dropHeight * waterLevelRatio)

            // 2. الموجة الخلفية العميقة (Back Liquid Wave)
            val backWavePath = createWavePath(
                width = w,
                height = h,
                baseY = baseWaterY - 4f,
                phase = wavePhase2,
                amplitude = dropHeight * 0.045f,
                wavelength = dropWidth * 0.9f
            )
            drawPath(
                path = backWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.65f),
                        Color(0xFF003840)
                    ),
                    startY = baseWaterY - 10f,
                    endY = h
                )
            )

            // 3. الموجة الأمامية الزاهية مع تدرج مائي متلألئ (Front Liquid Wave)
            val frontWavePath = createWavePath(
                width = w,
                height = h,
                baseY = baseWaterY,
                phase = wavePhase1,
                amplitude = dropHeight * 0.06f,
                wavelength = dropWidth * 0.75f
            )
            drawPath(
                path = frontWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.95f),
                        primaryColor.copy(alpha = 0.95f),
                        Color(0xFF004D40)
                    ),
                    startY = baseWaterY - 5f,
                    endY = h
                )
            )

            // 4. رغوة ولمعان قمة الموجة المائية (Wave Crest Foam Highlight)
            drawPath(
                path = frontWavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f),
                        highlightColor.copy(alpha = 0.9f),
                        Color.White.copy(alpha = 0.4f)
                    )
                ),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )

            // 5. فقاعات مياه متصاعدة من الأعماق (Rising Dynamic Bubbles)
            drawRisingBubbles(
                centerX = dropCenterX,
                baseWaterY = baseWaterY,
                bottomY = dropCenterY + (dropHeight * 0.42f),
                progress = bubbleProgress,
                dropWidth = dropWidth
            )
        }

        // E. إطار زجاج القطرة الكريستالي الخارجي (Crystal Glass Rim with Gradient Stroke)
        drawPath(
            path = dropletPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    accentColor.copy(alpha = 0.85f),
                    primaryColor.copy(alpha = 0.6f),
                    Color.White.copy(alpha = 0.35f)
                ),
                start = Offset(dropCenterX - (dropWidth * 0.5f), dropCenterY - (dropHeight * 0.5f)),
                end = Offset(dropCenterX + (dropWidth * 0.5f), dropCenterY + (dropHeight * 0.5f))
            ),
            style = Stroke(width = 2.8f, cap = StrokeCap.Round)
        )

        // F. لمعة الزجاج المنحنية العاكسة للضوء (Curved Specular Lens Highlight)
        val lensHighlightPath = Path().apply {
            val startX = dropCenterX - (dropWidth * 0.28f)
            val startY = dropCenterY - (dropHeight * 0.26f)
            moveTo(startX, startY)
            cubicTo(
                startX - (dropWidth * 0.12f), startY + (dropHeight * 0.18f),
                startX - (dropWidth * 0.08f), startY + (dropHeight * 0.38f),
                dropCenterX - (dropWidth * 0.2f), startY + (dropHeight * 0.48f)
            )
        }
        drawPath(
            path = lensHighlightPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.1f)
                )
            ),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // G. القطرة المتساقطة من الساقية العلوية نحو قلب الماء (Falling Canal Droplet)
        drawFallingDrip(
            centerX = dropCenterX,
            startY = h * 0.02f,
            targetY = dropCenterY - (dropHeight * 0.46f),
            progress = dripProgress,
            color = accentColor
        )
    }
}

/**
 * بناء مسار قطرة الماء الاحترافي والموزون هندسياً.
 */
private fun createTeardropPath(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float
): Path {
    val topY = centerY - (height * 0.5f)
    val bottomY = centerY + (height * 0.5f)
    val leftX = centerX - (width * 0.5f)
    val rightX = centerX + (width * 0.5f)

    return Path().apply {
        // نقطة الرأس الحادة في الأعلى
        moveTo(centerX, topY)

        // القوس الانسيابي الأيسر الهابط نحو القاع المستدير
        cubicTo(
            centerX - (width * 0.12f), topY + (height * 0.25f),
            leftX, bottomY - (height * 0.38f),
            leftX, bottomY - (height * 0.2f)
        )
        // تقويس القاع السفلي للقطرة
        cubicTo(
            leftX, bottomY,
            rightX, bottomY,
            rightX, bottomY - (height * 0.2f)
        )
        // القوس الانسيابي الأيمن الصاعد عائداً إلى القمة
        cubicTo(
            rightX, bottomY - (height * 0.38f),
            centerX + (width * 0.12f), topY + (height * 0.25f),
            centerX, topY
        )
        close()
    }
}

/**
 * بناء مسار الموجة الجيبية المائية المستمرة (Continuous Liquid Sine Wave).
 */
private fun createWavePath(
    width: Float,
    height: Float,
    baseY: Float,
    phase: Float,
    amplitude: Float,
    wavelength: Float
): Path {
    val path = Path()
    path.moveTo(0f, height)
    path.lineTo(0f, baseY)

    val step = 4f
    var x = 0f
    while (x <= width) {
        val y = baseY + (amplitude * sin((2 * PI * (x / wavelength) + phase).toFloat()))
        path.lineTo(x, y)
        x += step
    }

    path.lineTo(width, height)
    path.close()
    return path
}

/**
 * رسم فقاعات مياه تصعد بحركة تذبذب انسيابية داخل الماء.
 */
private fun DrawScope.drawRisingBubbles(
    centerX: Float,
    baseWaterY: Float,
    bottomY: Float,
    progress: Float,
    dropWidth: Float
) {
    val bubbleConfigs = listOf(
        Triple(-0.18f, 0.0f, 3.2f),
        Triple(0.12f, 0.35f, 4.0f),
        Triple(-0.04f, 0.65f, 2.5f),
        Triple(0.22f, 0.85f, 3.0f)
    )

    bubbleConfigs.forEach { (xOffsetFactor, delayFraction, radius) ->
        val bubbleProgress = (progress + delayFraction) % 1f
        val bubbleY = bottomY - ((bottomY - baseWaterY) * bubbleProgress)

        // تذبذب جانبي خفيف أثناء الصعود (Wobble)
        val wobbleX = centerX + (dropWidth * xOffsetFactor) + (sin(bubbleProgress * PI.toFloat() * 4f) * 4f)
        val alpha = if (bubbleProgress > 0.85f) (1f - bubbleProgress) / 0.15f else 0.8f

        if (bubbleY in baseWaterY..bottomY) {
            drawCircle(
                color = Color.White.copy(alpha = alpha.coerceIn(0f, 0.85f)),
                radius = radius,
                center = Offset(wobbleX, bubbleY)
            )
        }
    }
}

/**
 * رسم القطرة الساقطة دورياً من الأعلى نحو رأس القطرة الأساسية.
 */
private fun DrawScope.drawFallingDrip(
    centerX: Float,
    startY: Float,
    targetY: Float,
    progress: Float,
    color: Color
) {
    if (progress > 0.85f) return // تختفي فور ارتطامها بالماء

    val currentY = startY + ((targetY - startY) * progress)
    val dropLength = 6f + (progress * 6f)
    val dropWidth = 3.5f

    drawOval(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White,
                color
            ),
            startY = currentY - dropLength,
            endY = currentY + dropLength
        ),
        topLeft = Offset(centerX - (dropWidth / 2f), currentY - dropLength),
        size = Size(dropWidth, dropLength * 2f)
    )
}

/**
 * رسم تموجات القاعدة البيضاوية المنبثقة من تحت القطرة كسطح بركة الماء.
 */
private fun DrawScope.drawGroundRipples(
    centerX: Float,
    centerY: Float,
    maxRadiusX: Float,
    maxRadiusY: Float,
    progress: Float,
    color: Color
) {
    for (i in 0..1) {
        val rippleP = (progress + (i * 0.5f)) % 1f
        val radiusX = maxRadiusX * rippleP
        val radiusY = maxRadiusY * rippleP
        val alpha = (1f - rippleP) * 0.45f

        drawOval(
            color = color.copy(alpha = alpha),
            topLeft = Offset(centerX - radiusX, centerY - radiusY),
            size = Size(radiusX * 2f, radiusY * 2f),
            style = Stroke(width = 1.6f)
        )
    }
}

/**
 * نافذة تحميل زجاجية فاخرة وخاصة بتطبيق "مُسَرِّب" (Mosarib Master Glass Loading Dialog).
 * مصممة لتأسر العين بجمالها ودقتها وبإطار نيون مائي وتأثيرات حركة حية.
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

    // أنيميشن حركة النقاط المتتالية (...) لتعطي شعوراً حياً فائق الواقعية
    val infiniteTransition = rememberInfiniteTransition(label = "DotsAnim")
    val dotStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
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
            shape = RoundedCornerShape(28.dp),
            color = if (isDark) Color(0xFF071419).copy(alpha = 0.96f) else Color.White.copy(alpha = 0.98f),
            tonalElevation = 10.dp,
            shadowElevation = 24.dp,
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SecondaryAqua.copy(alpha = 0.7f),
                        PrimaryTeal.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            ),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // الشارة العلوية الأنيقة (هوية التطبيق)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryTeal.copy(alpha = if (isDark) 0.18f else 0.10f),
                    border = BorderStroke(1.dp, SecondaryAqua.copy(alpha = 0.25f)),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "مُسَرِّب • معالجة ذكية",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) SecondaryAqua else PrimaryTeal,
                                fontSize = 11.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                // الحاوية الكبرى للمؤشر المائي الحي
                Box(
                    modifier = Modifier
                        .size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MosaribWaterLoadingIndicator(
                        size = 120.dp,
                        primaryColor = PrimaryTeal,
                        accentColor = SecondaryAqua,
                        highlightColor = AccentEmerald
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // العنوان الرئيسي للتحميل مع النقاط الحية
                Text(
                    text = "$displayMessage $animatedDots",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        letterSpacing = 0.2.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (!displaySubMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = displaySubMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * ويدجت تحميل مائي مضغوط واحترافي للأزرار والبطاقات والقوائم وعمليات الحفظ.
 */
@Composable
fun MosaribInlineLoading(
    modifier: Modifier = Modifier,
    text: String? = null,
    size: Dp = 26.dp,
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
            Spacer(modifier = Modifier.width(8.dp))
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

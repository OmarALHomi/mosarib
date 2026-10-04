package com.baynana.ui.identity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.baynana.ui.theme.HarvestGold

/**
 * علامة «بيننا»: **قوسان متقابلان ونقطة بينهما** — الطرفان، و«بيننا» ما بينهما.
 *
 * العلامة مرسومة بالكود لا بصورة: فتتبدّل ألوانها مع السمة، ولا تتشوّه عند أي مقاس، ولا تُشحن
 * صورة زائدة الحجم في التطبيق. وهي نفس العلامة في أيقونة التطبيق (`ic_launcher_foreground`).
 */
@Composable
fun BaynanaMark(
    size: Dp = 96.dp,
    markColor: Color = MaterialTheme.colorScheme.onPrimary,
    accentColor: Color = HarvestGold
) {
    Canvas(modifier = Modifier.size(size)) {
        val canvasSize = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = canvasSize * 0.285f
        val stroke = canvasSize * 0.088f
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arcSize = Size(radius * 2, radius * 2)

        // القوس الأيمن والأيسر: كل قوس فتحته 104 درجات (أقل من نصف دائرة) فيبقى الطرفان ظاهرَين
        // ولا يقتربان من حرف الأيقونة في القصّ التكيفي.
        drawArc(
            color = markColor,
            startAngle = -52f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )
        drawArc(
            color = markColor,
            startAngle = 128f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke)
        )

        val dotRadius = canvasSize * 0.105f
        drawCircle(color = accentColor, radius = dotRadius, center = center)
    }
}

/** العلامة على خلفية الهوية (لشاشة البداية و«حول»). */
@Composable
fun BaynanaMarkTile(size: Dp = 120.dp, background: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.235f))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        BaynanaMark(size = size * 0.62f)
    }
}

/** الاسم والشعار النصّي معًا: «بيننا» وتحتها جملة الوعد. */
@Composable
fun BaynanaWordmark(showTagline: Boolean = true, markSize: Dp = 72.dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BaynanaMarkTile(size = markSize)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "بيننا",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (showTagline) {
            Text(
                text = "مستودع حساباتك ومعاملاتك",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

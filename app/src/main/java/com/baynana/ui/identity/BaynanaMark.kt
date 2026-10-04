package com.baynana.ui.identity

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baynana.R

/**
 * علامة «بيننا»: **مصافحة قلبية داخل قلب** — الطرفان يتصافحان، والشكل كله قلب، واللمسة الذهبية
 * عند ملتقى اليدين هي «بيننا».
 *
 * العلامة تُقرأ من **المورد نفسه المستعمل في أيقونة التطبيق** (`ic_launcher_foreground`)، لا من رسم
 * ثانٍ بالكود. السبب صريح: رسمان لنفس العلامة يعنيان اختلافًا حتميًا بين ما يراه المستخدم على شاشة
 * جهازه وما يراه داخل التطبيق. وحين يوجد رسم واحد، لا يوجد اختلاف.
 */
@Composable
fun BaynanaMark(size: Dp = 96.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = "شعار بيننا",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "بيننا" }
    )
}

/** العلامة على خلفية نيلة بحواف دائرية — لشاشة البداية و«حول» وأي عنوان رئيسي. */
@Composable
fun BaynanaMarkTile(size: Dp = 120.dp, background: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.235f))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        BaynanaMark(size = size * 0.68f)
    }
}

/** الاسم والعلامة معًا: «بيننا» وتحتها جملة الوعد. */
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

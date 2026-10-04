package com.baynana.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baynana.ui.identity.BaynanaMark
import com.baynana.ui.theme.NavyNile
import kotlinx.coroutines.delay

/**
 * شاشة البداية — صارت «بيننا» لا مضخة ماء.
 *
 * ما تغيّر: كان المشهد أنبوبًا تسقط منه قطرة ثم تترك دوائر ماء (هوية توزيع المياه). والآن:
 * **العلامة تظهر بهدوء** على حقل نيلي، ومعها الاسم والوعد، وسطر واحد يقول ما يجري. لا قطرة،
 * ولا دائرة ماء، ولا لغة سقي — لأن المستخدم سيفتح دفتر حسابات، لا مضخة.
 *
 * الزمن: نفس المدة القديمة تقريبًا (≈ 1.9 ثانية) حتى لا تتغيّر حِسّية الإقلاع على الأجهزة البطيئة.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val markAlpha = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.86f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        markAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        textAlpha.animateTo(1f, tween(320))
        markScale.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        delay(900)
        onTimeout()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyNile),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            BaynanaMark(
                size = 132.dp,
                modifier = Modifier
                    .alpha(markAlpha.value)
                    .scale(markScale.value)
            )
            Spacer(Modifier.height(20.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textAlpha.value)
            ) {
                Text(
                    text = "بيننا",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color(0xFFFAF7F0)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "مستودع حساباتك ومعاملاتك",
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.ui.graphics.Color(0xFFC9A24A),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "جاري تجهيز دفترك…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color(0xFFA9C2DA),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

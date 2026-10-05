package com.baynana.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.baynana.core.sync.SyncConfig
import com.baynana.core.sync.SyncTokenSetup
import com.baynana.data.local.sync.SyncBridge
import com.baynana.ui.theme.BaynanaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * «ربط الجهاز» (ح٢٢ب): إدخال رمز الجهاز الذي يمنحه المالك، ثم **إثبات** أن القناة تعمل.
 *
 * وكانت هذه هي الحلقة المفتوحة: القناة والعقد والعامل كلها تعمل، لكن لا باب يُدخل الرمز من الجهاز —
 * فجهاز حقيقي لا يستطيع ضبط القناة إلا بتعديل ملفّات. الشاشة تسدّ هذه الحلقة بأربع قواعد:
 *
 * 1. **لا حفظ بلا إثبات**: الرمز لا يُخزَّن إلا بعد جولة مزامنة حقيقية تقول «قُبل». فالرمز الخاطئ
 *    لا يبقى في الجهاز يظنّ صاحبه أن قناته مضبوطة.
 * 2. **الرفض ≠ تعذّر الوصول**: الرمز المرفوض يُصلَح، والخادم غير الواصل يُعاد عليه. وهما رسالتان
 *    مختلفتان، لأن الخلط يدفع المستخدم لتبديل رمز صحيح.
 * 3. **الرمز سرّ**: لا يُعرض بعد الحفظ، ولا يُطبع، ويُخزَّن في ملفّ إعدادات التطبيق الخاص.
 *    (وعند تغييره يُستبدل فقط — ولا يُسجَّل في أي سجلّ.)
 * 4. **لا يعمل وحده**: لا فحص دوري ولا محاولة في الخلفية من هذه الشاشة؛ الإعداد فعل يقصده المالك.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncTokenScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<SyncTokenSetup.Outcome?>(null) }
    var addressLine by remember { mutableStateOf("") }

    // العنوان يُقرأ من الأصل البنائي: لا يُحرَّر من هنا (تغييره يعني بناءً جديدًا).
    LaunchedEffect(Unit) {
        val url = SyncConfig.changesUrl(context)
        val configured = SyncConfig.deviceToken(context) != null
        addressLine = when {
            url == null -> "لا عنوان مزامنة في هذا البناء: القناة غير مؤهَّلة، والتطبيق يعمل محليًّا."
            configured -> "العنوان مضبوط، ورمز الجهاز محفوظ على هذا الجهاز. يمكنك استبداله إن تغيّر."
            else -> "العنوان مضبوط: $url — وباقي إدخال رمز الجهاز."
        }
    }

    /**
     * يحاول الرمز **قبل** حفظه. والتنفيذ في `SyncBridge` لا هنا: تنصيب القناة مسموح من هناك وحده
     * (حاجز ٢٠)، فالطبقة الدنيا تُجري الجولة وتحكم وتُثبّت — والشاشة تعرض الحكم فقط.
     */
    fun tryConnect(candidate: String) {
        busy = true
        outcome = null
        scope.launch {
            val verdict = withContext(Dispatchers.IO) { SyncBridge.tryConnect(context, candidate) }
            outcome = verdict
            busy = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("ربط الجهاز") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .then(scrollableColumnModifier()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.size(4.dp))
            Text(
                "ربط الجهاز بقناة المزامنة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(addressLine, style = MaterialTheme.typography.bodySmall)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "الرمز يأتيك من المالك. ولو غاب العنوان من هذا البناء فلا قناة، والتطبيق يعمل محليًّا كاملًا.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = token,
                onValueChange = { token = it; outcome = null },
                label = { Text("رمز الجهاز") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "رمز الجهاز، سرّ لا يُعرض على الشاشة بعد الحفظ" }
            )
            TextButton(onClick = { visible = !visible }, enabled = !busy) {
                Text(if (visible) "أخفِ الرمز" else "أظهِر الرمز مؤقّتًا")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { tryConnect(token) }, enabled = !busy && token.isNotBlank()) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("نتحقّق…")
                    } else {
                        Text("تحقّق واحفظ")
                    }
                }
                OutlinedButton(
                    onClick = { tryConnect(SyncConfig.deviceToken(context).orEmpty()) },
                    enabled = !busy && SyncConfig.deviceToken(context) != null
                ) { Text("اختبر المحفوظ الآن") }
            }

            outcome?.let { result -> OutcomeCard(result) }

            Spacer(Modifier.size(8.dp))
            Text(
                "كيف يُضبط الرمز عند المالك؟ يُولَّد على الخادم ويُسلَّم للجهاز مرّة واحدة، ولا يُشارَك في " +
                    "الرسائل الجماعية ولا يُكتب في دفتر. ومن يمسك الرمز يستطيع الوصول إلى دفتر هذه المجموعة — " +
                    "فإن ضاع، يُبدَّل من الخادم ثم يُدخل الجديد هنا.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}

/** بطاقة الحكم: علامة + لون + كلمة، وما العمل — بلا مصطلحات تقنية. */
@Composable
private fun OutcomeCard(outcome: SyncTokenSetup.Outcome) {
    val colors = BaynanaStatus.colors
    val accepted = outcome is SyncTokenSetup.Outcome.Accepted
    val refused = outcome is SyncTokenSetup.Outcome.Refused
    val container = when {
        accepted -> colors.acknowledgedContainer
        refused -> colors.dangerContainer
        else -> colors.waitingContainer
    }
    val onContainer = when {
        accepted -> colors.onAcknowledgedContainer
        refused -> colors.onDangerContainer
        else -> colors.onWaitingContainer
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when {
                    accepted -> Icons.Default.CheckCircle
                    refused -> Icons.Default.ErrorOutline
                    else -> Icons.Default.Info
                },
                contentDescription = when {
                    accepted -> "قُبل"
                    refused -> "مرفوض"
                    else -> "تعذّر الوصول"
                },
                tint = onContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                SyncTokenSetup.message(outcome),
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer,
                fontWeight = FontWeight.Medium
            )
        }
        if (SyncTokenSetup.shouldRetry(outcome)) {
            Spacer(Modifier.size(4.dp))
            Text(
                "الرمز لم يُرفض — الخادم لم يُوصَل. أعد المحاولة عند توفّر الشبكة، ولا تبدّل الرمز.",
                style = MaterialTheme.typography.bodySmall,
                color = onContainer
            )
        }
        Spacer(Modifier.size(2.dp))
        Text(
            if (accepted) "حُفظ الرمز على هذا الجهاز فقط، وستعمل المزامنة عند الكتابة وعند توفّر الشبكة."
            else "لم يُحفظ شيء. أثر المحاولة أُزيل كما كان.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** تمرير رأسي: الشاشة قصيرة، لكن التكبير ٢٠٠٪ يحتاجه. */
@Composable
private fun scrollableColumnModifier(): Modifier {
    val state = androidx.compose.foundation.rememberScrollState()
    return Modifier.verticalScroll(state)
}

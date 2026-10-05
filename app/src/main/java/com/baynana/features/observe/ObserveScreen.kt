package com.baynana.features.observe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.observe.HealthRepository
import com.baynana.domain.observe.HealthReport
import kotlinx.coroutines.launch

/**
 * «صحّة النسخة» (ح٢٤): البوابات المعلنة، وكل بوابة بحالتها وسببها وما العمل إن سقطت.
 *
 * مبادئ العرض الثابتة في هذا المشروع:
 * - **الحالة تُقرأ في نصف ثانية**: لون + علامة + كلمة (✓ سليمة / ✗ ساقطة / ؟ لم تُقس بعد).
 * - **لا تتبّع**: التقرير يُبنى على الجهاز، ويُشارَك بقرار صاحب الجهاز بزرّ صريح.
 * - **لا يعتمد على الشبكة**: فيعمل في وضع الطيران، ويُقال ذلك على الشاشة لئلا يظنّ المستخدم أن
 *   الأرقام قديمة لأنه بلا إنترنت.
 * - **حالات خمس**: تحميل، خطأ، فراغ، محتوى، وبلا شبكة (المحتوى نفسه هو حالة «بلا شبكة»).
 */
data class ObserveState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val report: HealthReport.Report? = null,
    val error: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObserveScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { HealthRepository(context, AppDatabase.getDatabase(context)) }
    var state by remember { mutableStateOf(ObserveState()) }

    val measure: () -> Unit = {
        state = state.copy(loading = state.report == null, refreshing = true, error = null)
        scope.launch {
            runCatching { repository.report() }
                .onSuccess { state = ObserveState(loading = false, report = it) }
                .onFailure {
                    state = state.copy(
                        loading = false,
                        refreshing = false,
                        error = it.message ?: "تعذّرت قراءة بيانات الجهاز"
                    )
                }
        }
    }

    LaunchedEffect(repository) { if (!state.refreshing && state.report == null) measure() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("صحّة النسخة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        // قيم محلّية: `state` خاصيّة مُفوَّضة، فلا يجوز الاعتماد على تحويلها النوعي داخل `when`.
        val report = state.report
        val error = state.error
        when {
            state.loading -> LoadingBody(Modifier.padding(padding))
            error != null -> ErrorBody(error, measure, Modifier.padding(padding))
            report == null -> EmptyBody(measure, Modifier.padding(padding))
            else -> ReportBody(
                report = report,
                refreshing = state.refreshing,
                onRefresh = measure,
                onShare = { text ->
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    runCatching {
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "مشاركة تقرير الصحّة")
                        )
                    }
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

// ------------------------------------------------------------------ الحالات

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.size(12.dp))
        Text("نجمع أرقام هذا الجهاز… ولا شيء يُرسل أثناء الجمع.")
    }
}

@Composable
private fun ErrorBody(error: String, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        GateMark(HealthReport.GateState.FAIL)
        Spacer(Modifier.size(12.dp))
        Text("تعذّر بناء التقرير", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(6.dp))
        Text(error, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.size(6.dp))
        Text(
            "هذه مشكلة في قراءة بيانات هذا الجهاز، وليست فشل مزامنة. أعد المحاولة، وإن تكرّرت أرسل هذا النصّ.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRefresh) { Text("أعد المحاولة") }
    }
}

@Composable
private fun EmptyBody(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(40.dp))
        Spacer(Modifier.size(12.dp))
        Text("لا قياس بعد", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(6.dp))
        Text(
            "ابدأ بتسجيل قيد واحد على الأقل (سقية أو سداد) ثم عد هنا: التقرير يقيس دفترك الحقيقي لا نموذجًا.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.size(16.dp))
        Button(onClick = onRefresh) { Text("اقِس الآن") }
    }
}

@Composable
private fun ReportBody(
    report: HealthReport.Report,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        VerdictCard(report)
        Spacer(Modifier.size(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRefresh, enabled = !refreshing) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (refreshing) "نقيس…" else "قِس الآن")
            }
            OutlinedButton(onClick = { onShare(report.shareText()) }) { Text("شارك التقرير") }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            "لا تتبّع: كل رقم أعلاه حُسب على هذا الجهاز ولا يُرسل تلقائيًّا. والتقرير يعمل بلا إنترنت.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.size(12.dp))
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { SectionTitle("البوابات") }
            items(report.gates) { gate -> GateRow(gate) }
            item { SectionTitle("الأرقام") }
            items(report.metrics) { metric ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(metric.first, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(metric.second, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
            item {
                Text(
                    "هذا التقرير لا يحمل قيدًا ولا مبلغًا ولا اسم عضو — يمكنك إرساله لمن يسأل بلا كشف دفترك.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

@Composable
private fun VerdictCard(report: HealthReport.Report) {
    val (color, mark) = when {
        report.failed.isNotEmpty() -> MaterialTheme.colorScheme.error to "✗"
        report.unknown.isNotEmpty() -> MaterialTheme.colorScheme.tertiary to "؟"
        else -> MaterialTheme.colorScheme.primary to "✓"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), MaterialTheme.shapes.medium)
            .padding(12.dp)
            .semantics { contentDescription = report.verdictText() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(mark, style = MaterialTheme.typography.headlineSmall, color = color)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(report.verdictText(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "قياس ${HealthReport.Metrics.dateText(report.generatedAt)} • ${report.gates.size} بوابات معلنة",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun GateRow(gate: HealthReport.Gate) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        GateMark(gate.state)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(gate.statement, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(gate.measured, style = MaterialTheme.typography.bodySmall)
            if (gate.state == HealthReport.GateState.FAIL && gate.action.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(gate.action, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** العلامة الحاملة للمعنى: لون + رمز + كلمة يقرأها TalkBack (لا لون وحده). */
@Composable
private fun GateMark(state: HealthReport.GateState) {
    val (color, mark, label) = when (state) {
        HealthReport.GateState.PASS -> Triple(MaterialTheme.colorScheme.primary, "✓", "سليمة")
        HealthReport.GateState.FAIL -> Triple(MaterialTheme.colorScheme.error, "✗", "ساقطة")
        HealthReport.GateState.UNKNOWN -> Triple(MaterialTheme.colorScheme.tertiary, "؟", "لم تُقس بعد")
    }
    Text(
        mark,
        style = MaterialTheme.typography.titleLarge,
        color = color,
        modifier = Modifier.semantics { contentDescription = label }
    )
}

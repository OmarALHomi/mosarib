package com.baynana.features.migration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.migration.LegacyMigrationOutcome
import com.baynana.data.local.migration.LegacyMigrationRepository
import com.baynana.domain.migration.CustomerReconciliation
import com.baynana.domain.migration.LegacyMigrationEngine
import com.baynana.domain.migration.LegacyMigrationPlan
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.InfoPill
import com.baynana.ui.components.SectionHeader
import com.baynana.ui.components.amountText
import com.baynana.ui.theme.BaynanaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * «لوحة الترحيل» (د٦): قرار المالك على الإرث، في شاشة كاملة لا في نافذة عابرة.
 *
 * أربع قواعد تنفّذها هذه اللوحة، وكلّها من خطّة التصميم:
 * 1. **لا ترحيل بلا جرد صفر الفرق**: الزرّ يُعطَّل من `LegacyMigrationEngine.canApply` (منطق نقيّ)،
 *    والطبقة الدنيا تحرس كل عميل على حدة — فتعطيل زرٍّ وحده ليس ضمانًا.
 * 2. **لا بدء بلا معاينة صريحة**: قبل التنفيذ يُعرض: كم عميلًا، وكم قيدًا، والإجمالي القديم مقابل
 *    المرحَّل، وجدول كل عميل (يطابق / لا يطابق وسببه).
 * 3. **الإرث لا يُمَس**: القراءة للعرض فقط، والكتابة نسخٌ إلى الدفتر، ولا حذف ولا تعديل للقديم.
 * 4. **الرفض معلن**: العميل الذي لا يطابق يُسمّى بسببه، ولا يُقال «تمّ» على ترحيل ناقص.
 *
 * والحالات الخمس موجودة كما في كل شاشة: تحميل، خطأ، فراغ («لا إرث على هذا الجهاز»)،
 * «تمّ سابقًا»، ومحتوى.
 */
private data class LegacyPanelState(
    val loading: Boolean = true,
    val plan: LegacyMigrationPlan? = null,
    val alreadyMigrated: Boolean = false,
    val running: Boolean = false,
    val outcome: LegacyMigrationOutcome? = null,
    val error: String? = null,
    val confirmOpen: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacyMigrationPanel(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { LegacyMigrationRepository(AppDatabase.getDatabase(context)) }
    var state by remember { mutableStateOf(LegacyPanelState()) }

    val load: () -> Unit = {
        state = state.copy(loading = state.plan == null, error = null)
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val plan = repository.buildPlan()
                    val already = AppDatabase.getDatabase(context)
                        .ledgerDao()
                        .getSyncState(LegacyMigrationRepository.MARKER_KEY) != null
                    plan to already
                }
            }
            result.fold(
                onSuccess = { (plan, already) ->
                    state = state.copy(loading = false, plan = plan, alreadyMigrated = already, error = null)
                },
                onFailure = { failure ->
                    state = state.copy(
                        loading = false,
                        error = failure.message ?: "تعذّرت قراءة الدفتر القديم"
                    )
                }
            )
        }
    }

    // دالّة محلّية لا lambda: تسمح بالخروج المبكر بلا تسمية ملتبسة.
    fun applyNow() {
        val plan = state.plan ?: return
        state = state.copy(running = true, confirmOpen = false, error = null)
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { repository.apply(plan) } }
            result.fold(
                onSuccess = { outcome ->
                    state = state.copy(running = false, outcome = outcome, alreadyMigrated = true)
                },
                onFailure = { failure ->
                    state = state.copy(running = false, error = failure.message ?: "تعذّر الترحيل")
                }
            )
        }
    }

    LaunchedEffect(repository) { if (state.plan == null && !state.running) load() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("الترحيل من الدفتر القديم") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        val plan = state.plan
        val error = state.error
        when {
            state.loading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("نقرأ دفترك القديم… بلا أي كتابة.")
            }

            error != null && plan == null -> Column(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = "تعذّرت قراءة الدفتر القديم",
                    body = error,
                    actionLabel = "أعد المحاولة",
                    onAction = load
                )
            }

            plan == null || plan.entries.isEmpty() -> EmptyState(
                title = "لا شيء ليُرحَّل",
                body = "لا يوجد دفتر قديم في هذا الجهاز، أو أن كل ما فيه سبق ترحيله. " +
                    "وإن كنت ترحّل من جهاز آخر، فذلك من «نقل الدفتر بجرد مطابق».",
                modifier = Modifier.padding(padding)
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                item { SectionHeader("دفترك القديم", "قراءة فقط: لا يُحذف منه شيء ولا يُعدَّل. والترحيل ينقل المتبقي بتاريخ آخر سقية مسجّل.") }
                item { LegacySummary(plan) }
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    SectionHeader(
                        title = "جدول الجرد لكل عميل",
                        subtitle = "قديم = مرحَّل، فهذا هو شرط البدء. وأي فرق يُعلن سببه هنا."
                    )
                }
                items(plan.reconciliations) { item -> ReconciliationRow(item, plan.currency) }
                if (plan.notes.isNotEmpty()) {
                    item { Spacer(Modifier.height(12.dp)) }
                    item { SectionHeader("ملاحظات") }
                    items(plan.notes) { note ->
                        Text(
                            "• $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
                item { ExecutionBlock(state, plan, onApplyClick = { state = state.copy(confirmOpen = true) }, onRetry = load) }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (state.confirmOpen) {
        val plan = state.plan
        if (plan != null) {
            AlertDialog(
                onDismissRequest = { state = state.copy(confirmOpen = false) },
                title = { Text("تأكيد الترحيل", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "سيُنقل المتبقي فقط من ${plan.reconciliations.size} عميلًا " +
                            "(${plan.entries.size} قيدًا)، بتاريخ آخر سقية مسجّل — لا بتاريخ اليوم. " +
                            "ودفترك القديم يبقى كما هو."
                    )
                },
                confirmButton = {
                    Button(onClick = { applyNow() }) { Text("رحّل الآن") }
                },
                dismissButton = {
                    TextButton(onClick = { state = state.copy(confirmOpen = false) }) { Text("ليس الآن") }
                }
            )
        }
    }
}

@Composable
private fun LegacySummary(plan: LegacyMigrationPlan) {
    val colors = BaynanaStatus.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InfoPill("عملاء: ${plan.reconciliations.size}", colors.infoContainer, colors.onInfoContainer)
        InfoPill("غرف: ${plan.rooms.size}", colors.infoContainer, colors.onInfoContainer)
        InfoPill("قيود: ${plan.entries.size}", colors.infoContainer, colors.onInfoContainer)
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "الإجمالي القديم: ${amountText(plan.legacyTotalMinor, plan.currency)}  ←  " +
            "المرحَّل: ${amountText(plan.plannedTotalMinor, plan.currency)}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
    val matches = LegacyMigrationEngine.canApply(plan)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (matches) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = if (matches) colors.onAcknowledgedContainer else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (matches) {
                "الجرد مطابق في كل الصفوف: يمكن الترحيل"
            } else {
                "الجرد لا يطابق في ${plan.reconciliations.count { !it.matches }} صفًّا: الترحيل موقوف حتى يُفهم الفرق"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (matches) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ReconciliationRow(item: CustomerReconciliation, currency: String) {
    val colors = BaynanaStatus.colors
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.customerName.ifBlank { "عميل #${item.customerId}" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(8.dp))
            if (item.matches) {
                InfoPill("يطابق", colors.acknowledgedContainer, colors.onAcknowledgedContainer)
            } else {
                InfoPill("لا يطابق", colors.dangerContainer, colors.onDangerContainer)
            }
        }
        Text(
            "قديم: ${amountText(item.legacyMinor, currency)} • مرحَّل: ${amountText(item.plannedMinor, currency)}" +
                if (item.creditMinor != 0L) " • رصيد دائن: ${amountText(item.creditMinor, currency)}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "سقيات: ${item.sessionCount} • مُرحَّلة: ${item.migratedSessions} • " +
                "مُسوَّاة سابقًا: ${item.skippedSettledSessions} • جارية: ${item.skippedLiveSessions}" +
                if (item.voucherCount > 0) " • إيصالات قديمة: ${item.voucherCount}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!item.matches) {
            Text(
                item.warnings.firstOrNull() ?: "الجرد لا يطابق",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ExecutionBlock(
    state: LegacyPanelState,
    plan: LegacyMigrationPlan,
    onApplyClick: () -> Unit,
    onRetry: () -> Unit
) {
    val colors = BaynanaStatus.colors
    state.outcome?.let { outcome ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.onAcknowledgedContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("تمّ الترحيل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text(outcome.summaryText(), style = MaterialTheme.typography.bodyMedium)
        if (!outcome.isClean) {
            Spacer(Modifier.height(6.dp))
            outcome.refusedCustomers.forEach { refused ->
                Text(
                    "• أُوقف ترحيل ${refused.customerName}: ${refused.warnings.firstOrNull() ?: "فرق في الجرد"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry) { Text("أعد قراءة الدفتر القديم") }
        return
    }

    val canApply = LegacyMigrationEngine.canApply(plan)
    Button(
        onClick = onApplyClick,
        enabled = canApply && !state.running,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (state.running) "جاري الترحيل…" else "رحّل الآن")
    }
    Spacer(Modifier.height(6.dp))
    Text(
        if (canApply) {
            "سيُطلب تأكيد صريح قبل التنفيذ: ما سيُنقل، وبأي تاريخ، وأن القديم لا يُمَس."
        } else {
            "الزرّ معطَّل ما دام في الجدول صفّ لا يطابق. أصلح الفرق في الدفتر القديم، أو أعلنه — لكن لا يُنقل رقم نصف صحيح."
        },
        style = MaterialTheme.typography.bodySmall,
        color = if (canApply) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
    )
    state.error?.let { error ->
        Spacer(Modifier.height(6.dp))
        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

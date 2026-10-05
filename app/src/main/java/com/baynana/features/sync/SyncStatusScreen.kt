package com.baynana.features.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baynana.core.database.AppDatabase
import com.baynana.core.sync.SyncConfig
import com.baynana.data.local.ledger.SyncStatusRepository
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.SectionHeader
import com.baynana.ui.components.StatusChip
import com.baynana.ui.components.SyncBadge
import com.baynana.ui.theme.BaynanaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * **حالة المزامنة**: لكل حركة سطر يقول الحقيقة: محفوظة في دفترك، أم خرجت، أم أُقرّت، أم فشلت ومعها
 * الحل. وليست شاشة تشخيص تقنية: لا أرقام حالات ولا كلمات إنجليزية، بل «ما حال قيدي؟» و«ما العمل؟».
 *
 * ثلاث قواعد مثبّتة هنا:
 * - لا نقول «تمّ الإرسال» إلا إذا كان في صندوق الصادر ما يثبته.
 * - كل فشل دائم يُعرض بسبب مكتوب **وبزرّ إعادة محاولة**: لا فشل بلا مخرج.
 * - بلا شبكة نقولها صريحة، ويبقى كل ما في الشاشة صحيحًا لأن كل شيء في الدفتر المحلّي أصلًا.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    onBack: () -> Unit,
    onOpenDeviceLink: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SyncStatusRepository(AppDatabase.getDatabase(context)) }
    val snapshot by repository.observe().collectAsStateWithLifecycle(
        initialValue = null
    )
    var message by remember { mutableStateOf<String?>(null) }
    var busyOperation by remember { mutableStateOf("") }
    var online by remember { mutableStateOf(isOnline(context)) }
    // هل قناة المزامنة مضبوطة في هذه النسخة؟ (العنوان من الأصل البنائي، والرمز على الجهاز.)
    val channelConfigured = remember { SyncConfig.isConfigured(context) }

    // نراقب الاتصال بهدوء كل عشرين ثانية: الشاشة تقول حاله بدقّة، ولا تُبنى عليها أي حقيقة محاسبية.
    LaunchedEffect(Unit) {
        while (true) {
            online = isOnline(context)
            kotlinx.coroutines.delay(20_000)
        }
    }

    // تُحسب الحالة مرة واحدة عند تغيّر اللقطة. لا `remember` داخل نطاق `LazyColumn` لأنه ليس
    // دالة تركيبة، بل حاوية عناصر — وكان هذا خطأ ترجمة حقيقيًا كشفه CI.
    val current = snapshot
    val view = current?.let { SyncStatusModel.build(it) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("حالة المزامنة", fontWeight = FontWeight.Bold)
                        Text(
                            "ما جرى لكل حركة، وما العمل فيها",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                if (snapshot == null) {
                    SyncBadge("جاري قراءة دفترك…")
                } else {
                    // لا نقول «سيُرسل» إلا لقناة مضبوطة: الوعود الكاذبة أسوأ من الاعتراف بالنقص.
                    val badge = SyncStatusModel.channelBadge(online = online, configured = channelConfigured)
                    SyncBadge(badge.text, isWarning = badge.isWarning)
                }
            }

            message?.let { text ->
                item { SyncBadge(text, isWarning = true) }
            }

            // «ربط الجهاز» (ح٢٢ب): الباب الذي بلا وجوده لا تُضبط القناة على جهاز حقيقي. ويظهر دائمًا
            // — لأن من لم يضبط القناة بعد يحتاج أن يعرف أن للقناة بابًا، لا أن يرى «غير مضبوطة» بلا مخرج.
            if (!channelConfigured) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SyncBadge("لا قناة مزامنة على هذا الجهاز — والتطبيق يعمل محليًّا كاملًا.", isWarning = true)
                        OutlinedButton(onClick = onOpenDeviceLink, modifier = Modifier.fillMaxWidth()) {
                            Text("ربط الجهاز برمز من المالك")
                        }
                    }
                }
            } else {
                item {
                    TextButton(onClick = onOpenDeviceLink) { Text("إعدادات الربط (تبديل الرمز)") }
                }
            }

            if (view == null) {
                item { SyncBadge("جاري التحميل…") }
                return@LazyColumn
            }

            item {
                SectionHeader(
                    title = view.headline,
                    subtitle = "هذه ليست شاشة تقنية: كل سطر يقول حال حركتك وما العمل فيها."
                )
            }

            if (view.rows.isEmpty()) {
                item {
                    EmptyState(
                        title = "لا حركة في دفترك بعد",
                        body = "أول قيد تكتبه يظهر هنا بحاله: محفوظ محليًا أولًا، ثم مُرسل، ثم مُقَرّ."
                    )
                }
            }

            items(view.rows, key = { it.entryId }) { row ->
                SyncRowCard(
                    row = row,
                    busy = busyOperation.isNotEmpty() && busyOperation == row.retryOperationId,
                    chip = syncLook(row.chip),
                    onRetry = {
                        scope.launch {
                            busyOperation = row.retryOperationId
                            runCatching {
                                withContext(Dispatchers.IO) { repository.retry(row.retryOperationId) }
                            }.fold(
                                onSuccess = { message = "أُعيدت الحركة إلى الطابور، وستُرسل عند الاتصال" },
                                onFailure = { message = it.message ?: "تعذّرت إعادة المحاولة" }
                            )
                            busyOperation = ""
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "لا يُحذف قيد شارك فيه غيرك، ولا نقول «تمّ» إلا إذا خرج من جهازك فعلًا. " +
                        "وما لم يُرسل بعد يبقى محفوظًا في دفترك على هذا الهاتف.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SyncRowCard(
    row: SyncStatusModel.Row,
    busy: Boolean,
    onRetry: () -> Unit,
    chip: StatusChipLook
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = chip.label,
                    container = chip.container,
                    onContainer = chip.onContainer,
                    icon = chip.icon
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    row.dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = row.roomTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(row.detail, style = MaterialTheme.typography.bodySmall)
            if (row.retryOperationId.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry, enabled = !busy) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (busy) "جاري الإعادة…" else "أعد المحاولة الآن")
                }
            }
        }
    }
}

/**
 * لون الحالة ورمزها وكلمتها: **الكلمة تُطلب من النموذج** (فهو من يعرف هل القيد وارد أو مُرسل)،
 * واللون والرمز يُختاران من الحالة. هكذا لا تنشأ كلمة في الشاشة تخالف الحقيقة المخزّنة.
 */
@Composable
private fun syncLook(chip: String): StatusChipLook {
    val colors = BaynanaStatus.colors
    return when (chip) {
        SyncStatusModel.CHIP_LOCAL ->
            StatusChipLook(chip, Icons.Default.CloudOff, colors.infoContainer, colors.onInfoContainer)
        SyncStatusModel.CHIP_SENT, SyncStatusModel.CHIP_RECEIVED ->
            StatusChipLook(chip, Icons.Default.Schedule, colors.waitingContainer, colors.onWaitingContainer)
        SyncStatusModel.CHIP_ACKNOWLEDGED ->
            StatusChipLook(chip, Icons.Default.CheckCircle, colors.acknowledgedContainer, colors.onAcknowledgedContainer)
        SyncStatusModel.CHIP_PROBLEM ->
            StatusChipLook(chip, Icons.Default.ReportProblem, colors.waitingContainer, colors.onWaitingContainer)
        SyncStatusModel.CHIP_DEAD ->
            StatusChipLook(chip, Icons.Default.Error, colors.dangerContainer, colors.onDangerContainer)
        else ->
            StatusChipLook(chip, Icons.Default.RemoveCircle, colors.dangerContainer, colors.onDangerContainer)
    }
}

private data class StatusChipLook(
    val label: String,
    val icon: ImageVector,
    val container: Color,
    val onContainer: Color
)

/**
 * هل الجهاز متّصل الآن؟ تُستعمل للصدق في أول الشاشة فقط: **لا تُبنى عليها أي حقيقة محاسبية**،
 * فالدفتر محلّي دائمًا ويعمل بلا شبكة.
 */
private fun isOnline(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

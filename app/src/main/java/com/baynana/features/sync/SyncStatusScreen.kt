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
fun SyncStatusScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SyncStatusRepository(AppDatabase.getDatabase(context)) }
    val snapshot by repository.observe().collectAsStateWithLifecycle(
        initialValue = null
    )
    var message by remember { mutableStateOf<String?>(null) }
    var busyOperation by remember { mutableStateOf("") }
    var online by remember { mutableStateOf(isOnline(context)) }

    // نراقب الاتصال بهدوء كل عشرين ثانية: الشاشة تقول حاله بدقّة، ولا تُبنى عليها أي حقيقة محاسبية.
    LaunchedEffect(Unit) {
        while (true) {
            online = isOnline(context)
            kotlinx.coroutines.delay(20_000)
        }
    }

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
                } else if (!online) {
                    SyncBadge(
                        "أنت بلا اتصال الآن: كل ما في هذه الشاشة محفوظ في جهازك، وسيُرسل الجديد عند عودة الشبكة",
                        isWarning = true
                    )
                } else {
                    SyncBadge("متّصل — وسيُرسل ما في الطابور تلقائيًا")
                }
            }

            message?.let { text ->
                item { SyncBadge(text, isWarning = true) }
            }

            val current = snapshot
            if (current == null) {
                item { SyncBadge("جاري التحميل…") }
                return@LazyColumn
            }

            val view = remember(current) { SyncStatusModel.build(current) }

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
    onRetry: () -> Unit
) {
    val look = syncLook(row.state)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(
                    text = look.label,
                    container = look.container,
                    onContainer = look.onContainer,
                    icon = look.icon
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

/** لون الحالة ورمزها وكلمتها في مكان واحد: تُقرأ الحالة في نصف ثانية. */
@Composable
private fun syncLook(kind: SyncStatusModel.Kind): StatusChipLook {
    val colors = BaynanaStatus.colors
    return when (kind) {
        SyncStatusModel.Kind.LOCAL ->
            StatusChipLook("محفوظ محليًا", Icons.Default.CloudOff, colors.infoContainer, colors.onInfoContainer)
        SyncStatusModel.Kind.SENT ->
            StatusChipLook("أُرسل — بانتظار الإقرار", Icons.Default.Schedule, colors.waitingContainer, colors.onWaitingContainer)
        SyncStatusModel.Kind.ACKNOWLEDGED ->
            StatusChipLook("مُقرّ", Icons.Default.CheckCircle, colors.acknowledgedContainer, colors.onAcknowledgedContainer)
        SyncStatusModel.Kind.PROBLEM ->
            StatusChipLook("يحتاج نظرك", Icons.Default.ReportProblem, colors.waitingContainer, colors.onWaitingContainer)
        SyncStatusModel.Kind.DEAD ->
            StatusChipLook("فشل دائم", Icons.Default.Error, colors.dangerContainer, colors.onDangerContainer)
        SyncStatusModel.Kind.VOIDED ->
            StatusChipLook("ملغى بقيد عكسي", Icons.Default.RemoveCircle, colors.dangerContainer, colors.onDangerContainer)
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

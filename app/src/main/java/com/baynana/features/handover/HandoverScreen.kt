package com.baynana.features.handover

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baynana.core.database.AppDatabase
import com.baynana.core.license.LicenseManager
import com.baynana.core.util.FileSharingHelper
import com.baynana.data.local.handover.HandoverDirection
import com.baynana.data.local.handover.HandoverLogRow
import com.baynana.data.local.handover.HandoverRepository
import com.baynana.data.local.handover.InviteStatus
import com.baynana.data.local.handover.PendingInviteRow
import com.baynana.data.local.profile.LocalProfile
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.SectionHeader
import com.baynana.ui.components.SyncBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * **التسليم بلا إنترنت ولا حساب (ح١٩)**: الطريق الذي يعمل حين لا يعمل شيء آخر.
 *
 * الفكرة كما يفعلها الناس فعلًا: تُجهَّز «حزمة» نصّية من الحركات المعلّقة، فتُلصق في واتساب أو
 * تُرسل ملفًّا، ويستوردها الطرف الآخر في جهازه. لا حساب، ولا خادم، ولا شبكة لحظة التسليم.
 *
 * ثلاث حقائق تقولها الشاشة بوضوح ولا تجمّلها:
 * - **التجهيز ليس تسليمًا**: ما في الحزمة يبقى غير مُقَرّ عندك حتى يصل إقرار الطرف. لا نقول «تمّ».
 * - **الاستيراد يُفحص قبل أن يُطبَّق**: يظهر ما في الحزمة بالأرقام، ولا يدخل شيء بلمسة واحدة على
 *   ملف لم يُفحص.
 * - **لا تُفتح غرفة بكود**: الدعوة تصل «بانتظار قرارك»، ولا يفتح الربط كشفًا ولا دينًا قبل قبولك.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandoverScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember {
        HandoverRepository(
            db = AppDatabase.getDatabase(context),
            deviceCodeLabel = { LicenseManager.getDeviceCode(context) }
        )
    }

    val invites by repository.observeInvites().collectAsStateWithLifecycle(initialValue = emptyList())
    val log by repository.observeLog().collectAsStateWithLifecycle(initialValue = emptyList())
    val waiting by repository.observeWaitingCount().collectAsStateWithLifecycle(initialValue = 0)

    var pasted by remember { mutableStateOf("") }
    var outgoing by remember { mutableStateOf<HandoverRepository.Outgoing?>(null) }
    var preview by remember { mutableStateOf<HandoverRepository.Preview?>(null) }
    var report by remember { mutableStateOf<HandoverRepository.ImportReport?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("التسليم بلا إنترنت", fontWeight = FontWeight.Bold)
                        Text(
                            "ملفّ أو رمز يمرّ في واتساب: بلا حساب وبلا خادم",
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
                SyncBadge("هذه الشاشة تعمل كاملة بلا إنترنت: لا تحتاج حسابًا ولا شبكة في لحظة التسليم.")
            }

            // ------------------------------------------------------------- أرسل
            item {
                SectionHeader(
                    title = "أرسل حركاتك",
                    subtitle = "جاهز للإرسال: $waiting عنصر — تبقى بانتظار إقرار الطرف حتى بعد الإرسال."
                )
            }
            item {
                Button(
                    onClick = {
                        busy = true
                        message = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { repository.prepareOutgoing() }
                            }
                            busy = false
                            result.fold(
                                onSuccess = { outgoing = it; preview = null; report = null },
                                onFailure = { message = it.message ?: "تعذّر تجهيز الحزمة" }
                            )
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("جهّز حزمة من الحركات المعلّقة")
                }
            }

            outgoing?.let { prepared ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(prepared.summaryArabic, style = MaterialTheme.typography.bodyMedium)
                            prepared.compactCode?.let { code ->
                                Text(
                                    "رمز مختصر للمحادثة (${code.length} حرفًا)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    code.take(80) + if (code.length > 80) "…" else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } ?: Text(
                                "الحزمة أكبر من أن تُلصق كنصّ: أرسلها ملفًّا.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    FileSharingHelper.shareText(context, prepared.text, "تسليم بيننا")
                                }) {
                                    Icon(Icons.Default.Send, contentDescription = null)
                                    Spacer(Modifier.padding(horizontal = 3.dp))
                                    Text("واتساب")
                                }
                                OutlinedButton(onClick = {
                                    val file = writeBundleFile(context, prepared)
                                    FileSharingHelper.shareTextFile(context, file, "ملف تسليم بيننا")
                                }) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(Modifier.padding(horizontal = 3.dp))
                                    Text("ملف")
                                }
                            }
                            prepared.compactCode?.let { code ->
                                TextButton(onClick = {
                                    FileSharingHelper.copyToClipboard(context, code, "رمز تسليم بيننا")
                                }) { Text("انسخ الرمز المختصر") }
                            }
                            Text(
                                "التجهيز لا يعني التسليم: يبقى كل عنصر بانتظار إقرار الطرف، وهذا يظهر في «حالة المزامنة».",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------------- استلم
            item {
                SectionHeader(
                    title = "استلم حزمة",
                    subtitle = "الصق الرمز أو نصّ الملف، افحصه أولًا، ثم استورد."
                )
            }
            item {
                OutlinedTextField(
                    value = pasted,
                    onValueChange = { pasted = it; preview = null; report = null },
                    label = { Text("نصّ الحزمة أو رمزها") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { repository.preview(pasted) }
                                }
                                busy = false
                                result.fold(
                                    onSuccess = { preview = it; report = null },
                                    onFailure = { message = it.message ?: "تعذّر فحص الحزمة" }
                                )
                            }
                        },
                        enabled = !busy && pasted.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text("افحص")
                    }
                    Button(
                        onClick = {
                            busy = true
                            message = null
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { repository.importBundle(pasted) }
                                }
                                busy = false
                                result.fold(
                                    onSuccess = { report = it; preview = null },
                                    onFailure = { message = it.message ?: "تعذّر الاستيراد" }
                                )
                            }
                        },
                        enabled = !busy && pasted.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text("استورد")
                    }
                }
            }

            preview?.let { inspected ->
                item {
                    when (inspected) {
                        is HandoverRepository.Preview.Refused -> SyncBadge(inspected.messageArabic, isWarning = true)
                        is HandoverRepository.Preview.Ready -> InfoCard(
                            title = if (inspected.alreadyImported) "حزمة سبق أن استُوردت" else "ما في الحزمة",
                            body = inspected.summaryArabic
                        )
                    }
                }
            }

            report?.let { imported ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        InfoCard(title = "نتيجة الاستيراد", body = imported.summaryArabic)
                        if (imported.ignoredTrailingText) {
                            Text(
                                "كان في اللصق كلام بعد الحزمة، ولم يدخل منه شيء.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        imported.notes.forEach { note ->
                            Text(
                                note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            message?.let { text -> item { SyncBadge(text, isWarning = true) } }

            // --------------------------------------------------------- دعوات
            item {
                SectionHeader(
                    title = "دعوات غرفة بانتظار قرارك",
                    subtitle = "الغرفة لا تُفتح إلا بقبولك: كود الربط وحده لا يكشف كشفًا ولا دينًا."
                )
            }
            if (invites.none { it.status == InviteStatus.PENDING }) {
                item {
                    EmptyState(
                        title = "لا دعوة معلّقة",
                        body = "حين يصلك ملفّ فيه دعوة غرفة، تظهر هنا بقرارك: تقبل الربط أو تتجاهله."
                    )
                }
            }
            items(invites.filter { it.status == InviteStatus.PENDING }, key = { it.id }) { invite ->
                InviteCard(
                    invite = invite,
                    onAccept = {
                        busy = true
                        message = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { repository.acceptInvitation(invite.id) }
                            }
                            busy = false
                            result.fold(
                                onSuccess = { message = it.summaryArabic },
                                onFailure = { message = it.message ?: "تعذّر قبول الدعوة" }
                            )
                        }
                    },
                    onIgnore = {
                        scope.launch {
                            withContext(Dispatchers.IO) { repository.ignoreInvitation(invite.id) }
                            message = "تم تجاهل الدعوة، ولم تُفتح غرفة ولم يدخل شيء."
                        }
                    }
                )
            }

            // --------------------------------------------------------- السجلّ
            item {
                SectionHeader(
                    title = "سجلّ التسليم",
                    subtitle = "ما خرج من جهازك وما دخل إليه، بنتيجته — حتى لا يُنسى ما أُرسل لمن."
                )
            }
            if (log.isEmpty()) {
                item {
                    EmptyState(
                        title = "لا تسليم بعد",
                        body = "أول حزمة تجهّزها أو تستوردها تُكتب هنا بحصيلتها: كم دخل، وكم كان مكررًا، وكم رُفض."
                    )
                }
            }
            items(log, key = { it.id }) { row -> LogCard(row) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InviteCard(
    invite: PendingInviteRow,
    onAccept: () -> Unit,
    onIgnore: () -> Unit
) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    invite.title.ifBlank { "غرفة بلا عنوان" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                "من: ${LocalProfile.peerLabel(invite.inviterName)} • العملة: ${invite.currency} • النوع: ${invite.kind}",
                style = MaterialTheme.typography.bodySmall
            )
            if (LocalProfile.wireName(invite.inviterName).isBlank()) {
                Text(
                    "هذا الطرف لم يُرسل اسمه (جهاز بنسخة قديمة أو اسم فارغ). الغرفة تُفتح بمعرّفات الطرفين " +
                        "لا بالأسماء، فلا يضيع دين — لكن اطلب اسمه لتعرف من يدعوك.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (invite.note.isNotBlank()) {
                Text(invite.note, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "بالقبول تُفتح الغرفة بمعرّفات الطرفين نفسها، ويُطبَّق فورًا كل قيد وصل قبل الدعوة.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccept) { Text("اقبل الربط") }
                OutlinedButton(onClick = onIgnore) { Text("تجاهل") }
            }
        }
    }
}

@Composable
private fun LogCard(row: HandoverLogRow) {
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text(
                    if (row.direction == HandoverDirection.OUT) "أُرسلت حزمة" else "وصلت حزمة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                if (row.direction == HandoverDirection.OUT) {
                    "فيها ${row.items} عنصر • ${row.rooms.ifBlank { "بلا غرفة" }}"
                } else {
                    "فيها ${row.items} • دخل ${row.applied} • مكرر ${row.duplicates} • مرفوض ${row.rejected}" +
                        if (row.invited > 0) " • دعوات ${row.invited}" else ""
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${row.bundleId} • ${java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US).format(java.util.Date(row.createdAt))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (row.note.isNotBlank()) {
                Text(row.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** يكتب الحزمة في `cacheDir/reports` — وهو المسار نفسه المصرَّح به في FileProvider. */
private fun writeBundleFile(context: Context, outgoing: HandoverRepository.Outgoing): File {
    val directory = File(context.cacheDir, "reports").apply { mkdirs() }
    val file = File(directory, outgoing.fileName)
    file.writeText(outgoing.text, Charsets.UTF_8)
    return file
}

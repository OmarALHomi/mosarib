package com.baynana.features.rooms

import android.content.Intent
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
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import com.baynana.data.local.ledger.LedgerHomeRepository
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.domain.ledger.RoomFeed
import com.baynana.domain.ledger.StatementText
import com.baynana.ui.components.EntryCard
import com.baynana.ui.components.NumbersHeader
import com.baynana.ui.components.SectionHeader
import com.baynana.ui.components.RoomStatusChip
import com.baynana.ui.components.SyncBadge
import com.baynana.ui.components.amountText
import com.baynana.ui.theme.BaynanaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * كشف الغرفة: الأرقام الثلاثة، ما ينتظر إقراري، وكل الحركات، ومشاركة الكشف نصًّا واحدًا.
 *
 * كل الأرقام من `RoomFeed` (وهو يبني `LedgerSnapshot`)، والنصّ المُشارَك من `StatementEngine`
 * نفسه — فلا يختلف ما يُقرأ على الشاشة عمّا يُرسل في واتساب.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    roomId: String,
    onBack: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val home = remember { LedgerHomeRepository(database) }
    val ledger = remember { LedgerRepository(database) }

    var feed by remember { mutableStateOf<RoomFeed?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var ackTarget by remember { mutableStateOf<String?>(null) }
    var showAddDebt by remember { mutableStateOf(false) }
    var showReceipt by remember { mutableStateOf(false) }

    suspend fun reload() {
        feed = withContext(Dispatchers.IO) { home.feedOf(roomId) }
        loaded = true
    }

    LaunchedEffect(roomId) { reload() }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(feed?.title ?: "غرفة", fontWeight = FontWeight.Bold)
                        feed?.counterpartName?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val text = withContext(Dispatchers.IO) { buildStatementText(ledger, roomId, feed) }
                            if (text != null) {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "إرسال الكشف"))
                            }
                        }
                    }) { Icon(Icons.Default.Share, contentDescription = "مشاركة الكشف") }
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
            val current = feed
            if (!loaded) {
                item { SyncBadge("جاري قراءة الكشف…") }
            } else if (current == null) {
                item { SyncBadge("لم يُعثر على الغرفة", isWarning = true) }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RoomStatusChip(current.status, Modifier.weight(1f, fill = false))
                                Spacer(Modifier.width(8.dp))
                                if (current.linkCode.isNotBlank()) {
                                    Text(
                                        "كود الربط: ${current.linkCode}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            NumbersHeader(
                                chargedMinor = current.chargedMinor,
                                paidMinor = current.paidMinor,
                                remainingMinor = current.remainingMinor,
                                currency = current.currency
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = if (current.myNetMinor > 0) {
                                    "الصافي: لك ${amountText(current.myNetMinor, current.currency)}"
                                } else if (current.myNetMinor < 0) {
                                    "الصافي: عليك ${amountText(current.myNetMinor, current.currency)}"
                                } else {
                                    "الصافي: الحساب مصفّى"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (current.unappliedMinor > 0L) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "رصيد دائن معلّق: ${amountText(current.unappliedMinor, current.currency)} — لم يُخصَّص على دَين بعد",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BaynanaStatus.colors.onInfoContainer
                                )
                            }
                        }
                    }
                }

                message?.let {
                    item { SyncBadge(it, isWarning = true) }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { showAddDebt = true }) {
                            Text("قيد جديد")
                        }
                        OutlinedButton(onClick = { showReceipt = true }) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("سداد/قبض")
                        }
                    }
                }

                val pending = current.entries.filter { it.awaitingMyAck }
                if (pending.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "ينتظر إقرارك (${pending.size})",
                            subtitle = "الإقرار يغيّر الحالة فقط، ولا يمسح القيد ولا يعدّل مبلغه"
                        )
                    }
                    items(pending, key = { it.entryId }) { entry ->
                        EntryCard(
                            entry = entry,
                            currency = current.currency,
                            dateText = dateFormat.format(Date(entry.occurredAt)),
                            trailing = {
                                Column {
                                    Button(onClick = {
                                        scope.launch {
                                            runCatching {
                                                withContext(Dispatchers.IO) {
                                                    home.respondToEntry(
                                                        entryId = entry.entryId,
                                                        decision = com.baynana.domain.ledger.AckDecision.ACKNOWLEDGED,
                                                        note = "",
                                                        decidedAt = System.currentTimeMillis()
                                                    )
                                                }
                                            }.fold(
                                                onSuccess = {
                                                    message = "تم الإقرار — أُبلغ الطرف عند المزامنة"
                                                    reload()
                                                    onChanged()
                                                },
                                                onFailure = { message = it.message ?: "تعذّر الإقرار" }
                                            )
                                        }
                                    }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("أقرّ", style = MaterialTheme.typography.labelMedium)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    OutlinedButton(onClick = { ackTarget = entry.entryId },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text("اعتراض", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        )
                    }
                }

                item {
                    SectionHeader(
                        title = "الحركات (${current.entries.size})",
                        subtitle = if (current.hasHistory) null else "لا حركة بعد في هذه الغرفة"
                    )
                }

                items(current.entries, key = { it.entryId }) { entry ->
                    EntryCard(
                        entry = entry,
                        currency = current.currency,
                        dateText = dateFormat.format(Date(entry.occurredAt))
                    )
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    SyncBadge("كل ما في هذه الغرفة محفوظ في دفترك على هذا الجهاز")
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    ackTarget?.let { entryId ->
        AckDialog(
            onDismiss = { ackTarget = null },
            onConfirm = { decision, note ->
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            home.respondToEntry(entryId, decision, note, System.currentTimeMillis())
                        }
                    }.fold(
                        onSuccess = {
                            ackTarget = null
                            message = "تم تسجيل قرارك — ولم يُمسح القيد"
                            reload()
                            onChanged()
                        },
                        onFailure = {
                            ackTarget = null
                            message = it.message ?: "تعذّر تسجيل القرار"
                        }
                    )
                }
            }
        )
    }

    if (showAddDebt) {
        AddDebtDialog(
            roomId = roomId,
            currency = feed?.currency ?: "YER_NEW",
            counterpartName = feed?.counterpartName.orEmpty(),
            onDismiss = { showAddDebt = false },
            onSaved = {
                showAddDebt = false
                message = "حُفظ في دفترك — سيصل الطرف بعد الاتصال"
                scope.launch { reload(); onChanged() }
            }
        )
    }

    if (showReceipt) {
        ReceiptDialog(
            roomId = roomId,
            currency = feed?.currency ?: "YER_NEW",
            onDismiss = { showReceipt = false },
            onSaved = { text ->
                showReceipt = false
                message = text
                scope.launch { reload(); onChanged() }
            }
        )
    }
}

/** نصّ الكشف: من `StatementEngine` نفسه — لا صياغة ثانية في الواجهة. */
private suspend fun buildStatementText(
    ledger: LedgerRepository,
    roomId: String,
    feed: RoomFeed?
): String? {
    if (feed == null) return null
    val statement = ledger.statementFor(roomId, feed.myMemberId)
    val header = "كشف حساب — ${feed.title} (${feed.counterpartName})\n"
    val summary = StatementText.summary(statement) + "\n"
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val lines = statement.lines.joinToString("\n") { line ->
        StatementText.line(line, statement.currency, format.format(Date(line.occurredAt)))
    }
    return header + summary + "\n" + lines + "\n\nمن دفتر «بيننا»"
}

@Composable
private fun AckDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var note by remember { mutableStateOf("") }
    var decision by remember { mutableStateOf(com.baynana.domain.ledger.AckDecision.DISPUTED) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اعتراض على القيد", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "الاعتراض لا يمحو القيد: يبقى ظاهرًا للطرفين مع سببك، حتى يُصحَّح بقيد جديد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (decision == com.baynana.domain.ledger.AckDecision.DISPUTED) {
                        Button(onClick = { decision = com.baynana.domain.ledger.AckDecision.DISPUTED }) { Text("اعتراض") }
                    } else {
                        OutlinedButton(onClick = { decision = com.baynana.domain.ledger.AckDecision.DISPUTED }) { Text("اعتراض") }
                    }
                    if (decision == com.baynana.domain.ledger.AckDecision.CHANGE_REQUESTED) {
                        Button(onClick = { decision = com.baynana.domain.ledger.AckDecision.CHANGE_REQUESTED }) { Text("طلب تعديل") }
                    } else {
                        OutlinedButton(onClick = { decision = com.baynana.domain.ledger.AckDecision.CHANGE_REQUESTED }) { Text("طلب تعديل") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("السبب (مطلوب)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = note.isNotBlank(),
                onClick = { onConfirm(decision, note) }
            ) { Text("أرسل") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

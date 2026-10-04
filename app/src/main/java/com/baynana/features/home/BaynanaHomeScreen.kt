package com.baynana.features.home

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.ShellTab
import com.baynana.domain.ledger.RoomFeed
import com.baynana.ui.components.AmountText
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.NetBalanceCard
import com.baynana.ui.components.RoomRow
import com.baynana.ui.components.SectionHeader
import com.baynana.ui.components.RoomStatusChip
import com.baynana.ui.components.SyncBadge
import com.baynana.ui.components.amountText
import com.baynana.ui.identity.BaynanaWordmark
import com.baynana.ui.theme.BaynanaStatus

/**
 * الرئيسية الجديدة: تجيب سؤالين فقط — «كم لي وكم عليّ؟» و«ما الذي ينتظرني؟».
 *
 * كل رقم هنا يأتي جاهزًا من `RoomFeed` (ومصدره `LedgerSnapshot`)، والشاشة لا تجمع ولا تطرح.
 */
@Composable
fun BaynanaHomeScreen(
    state: BaynanaHomeViewModel.UiState,
    onRefresh: () -> Unit,
    onOpenRoom: (String) -> Unit,
    onRunMigration: () -> Unit,
    onOpenMigrationDetail: () -> Unit,
    onOpenTab: (ShellTab) -> Unit,
    /** فتح «حالة المزامنة» من شريط الصدق أسفل الشاشة. */
    onOpenSyncStatus: () -> Unit = {}
) {
    val rooms = state.rooms
    // قاعدة ADR-04: لا تُجمع عملتان. النظام يفتح الكارت بأكثر عملة تعاملًا معك، ويعلن الباقي بحدة
    // حتى لا يقرأ المستخدم مجموعًا واحدًا من عملات مختلفة.
    val byCurrency = rooms.groupBy { it.currency }
    val primaryCurrency = byCurrency.maxByOrNull { it.value.size }?.key ?: "YER_NEW"
    val primaryRooms = byCurrency[primaryCurrency].orEmpty()
    val currency = primaryCurrency
    val net = primaryRooms.sumOf { it.myNetMinor }
    val openDebt = primaryRooms.sumOf { it.remainingMinor }
    val unapplied = primaryRooms.sumOf { it.unappliedMinor }
    val otherCurrencies = byCurrency.filterKeys { it != primaryCurrency }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "دفترك",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                }
            }
        }

        if (state.loading) {
            item { SyncBadge("جاري قراءة دفترك…") }
        }

        if (rooms.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "دفترك جديد",
                    body = "لا غرفة بعد. الغرفة حساب بينك وبين طرف واحد: موزّع ماء، بقالة، دلال، أو قريب. " +
                        "أنشئ غرفة وشارك كود الربط، فيظهر ما بينكما للطرفين.",
                    actionLabel = "أنشئ أول غرفة",
                    onAction = { onOpenTab(ShellTab.ROOMS) }
                )
            }
            item { BaynanaWordmark(showTagline = true, markSize = 56.dp) }
        }

        if (primaryRooms.isNotEmpty()) {
            item {
                NetBalanceCard(
                    netMinor = net,
                    currency = currency,
                    openDebtMinor = openDebt,
                    unappliedMinor = unapplied
                )
            }
        }

        if (otherCurrencies.isNotEmpty()) {
            item {
                Column {
                    Text(
                        "أرصدة بعملات أخرى (لا تُجمع مع ${currencyName(currency)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    otherCurrencies.forEach { (code, list) ->
                        Text(
                            text = "$code: ${amountText(list.sumOf { it.myNetMinor }, code)} في ${list.size} غرفة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (state.awaitingMe > 0) {
            item {
                SectionHeader(
                    title = "ينتظر إقرارك (${state.awaitingMe})",
                    subtitle = "قيدات أرسلها الطرف الآخر ولم تُقرّ بعد — إقرارك يغيّر حالتها ولا يمسحها"
                )
            }
            items(rooms.filter { it.awaitingMyAcknowledgement > 0 }, key = { it.roomId }) { room ->
                RoomRow(
                    title = room.title,
                    subtitle = "بانتظار إقرارك: ${room.awaitingMyAcknowledgement} قيدًا",
                    netMinor = room.myNetMinor,
                    currency = room.currency,
                    awaiting = room.awaitingMyAcknowledgement,
                    statusChip = { RoomStatusChip(room.status) },
                    onClick = { onOpenRoom(room.roomId) }
                )
            }
        }

        if (state.migration.available) {
            item {
                MigrationCard(
                    state = state,
                    onRunMigration = onRunMigration,
                    onOpenDetail = onOpenMigrationDetail
                )
            }
        }

        item {
            SectionHeader(
                title = "غرفي (${rooms.size})",
                subtitle = "اضغط أي غرفة لترى كشفها كاملًا"
            )
        }

        items(rooms, key = { it.roomId }) { room ->
            RoomRow(
                title = room.title,
                subtitle = subtitleOf(room),
                netMinor = room.myNetMinor,
                currency = room.currency,
                awaiting = room.awaitingMyAcknowledgement,
                statusChip = { RoomStatusChip(room.status) },
                onClick = { onOpenRoom(room.roomId) }
            )
        }

        item {
            Spacer(Modifier.height(6.dp))
            SyncBadge(state.syncLine)
            TextButton(onClick = onOpenSyncStatus) {
                Text(
                    "ما حال كل حركة؟ أُرسلت أم ما زالت في جهازك",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

/** اسم العملة كما يُقرأ: العملة الواحدة تُسمّى بالعربية، وأي رمز آخر يُترك رمزًا بلا ترجمة مخترعة. */
private fun currencyName(code: String): String = when (code) {
    "YER_NEW" -> "الريال الجديد"
    "YER_OLD" -> "الريال القديم"
    "SAR" -> "الريال السعودي"
    "USD" -> "الدولار"
    else -> code
}

private fun subtitleOf(room: RoomFeed): String = buildString {
    append(room.counterpartName.ifBlank { "طرف بلا اسم" })
    if (room.counterpartPhone.isNotBlank()) append(" • ").append(room.counterpartPhone)
    append(" • ")
    append(if (room.hasHistory) "آخر حركة في دفترك" else "لا حركات بعد")
}

/** بطاقة قرار الترحيل من الإرث: تُعرض بالجرد، ولا تنفّذ بلا ضغط صريح. */
@Composable
private fun MigrationCard(
    state: BaynanaHomeViewModel.UiState,
    onRunMigration: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val plan = state.migration.plan ?: return
    val colors = BaynanaStatus.colors
    androidx.compose.material3.Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colors.infoContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "دفترك القديم فيه ${plan.reconciliations.size} عميلًا و${plan.entries.size} قيدًا",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onInfoContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "المجموع: ${amountText(plan.legacyTotalMinor, plan.currency)}. " +
                    "الترحيل ينقل المتبقي كما كان بتاريخ كل سقية، ولا يحذف شيئًا من الدفتر القديم.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onInfoContainer
            )
            state.migration.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            state.migration.outcome?.let { outcome ->
                Spacer(Modifier.height(6.dp))
                Text(
                    outcome.summaryText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onInfoContainer
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRunMigration, enabled = !state.migration.running) {
                    Text(if (state.migration.running) "جاري الترحيل…" else "رحّل الآن")
                }
                OutlinedButton(onClick = onOpenDetail) { Text("أرني الجرد أولًا") }
            }
        }
    }
}

package com.baynana.features.movements

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.domain.ledger.RoomFeedEntry
import com.baynana.features.home.BaynanaHomeViewModel
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.EntryCard
import com.baynana.ui.components.InfoPill
import com.baynana.ui.theme.BaynanaStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * «الحركات»: كل ما جرى في كل الغرف في تدفّق واحد — لأنه السؤال الذي يسأله المستخدم فعلًا:
 * «ماذا حدث في دفتري هذا الشهر؟» لا «ما سقياتي؟» و«ما سنداتي؟» في شاشتين.
 *
 * الفلاتر تُبنى على النوع والحالة من `RoomFeedEntry`، والمبالغ تصل جاهزة (لا حساب هنا).
 */
private enum class MovementFilter(val label: String) {
    ALL("الكل"),
    CHARGES("عليّ"),
    PAYMENTS("لي"),
    WAITING("بانتظار الإقرار")
}

@Composable
fun MovementsScreen(
    state: BaynanaHomeViewModel.UiState,
    onOpenRoom: (String) -> Unit
) {
    var filter by remember { mutableStateOf(MovementFilter.ALL) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    // تدفّق واحد: (الغرفة، السطر) مرتّبًا بالأحدث في كل الغرف.
    val all = state.rooms
        .flatMap { room -> room.entries.map { room to it } }
        .sortedWith(compareByDescending<Pair<com.baynana.domain.ledger.RoomFeed, RoomFeedEntry>> { it.second.occurredAt })

    val visible = when (filter) {
        MovementFilter.ALL -> all
        MovementFilter.CHARGES -> all.filter { it.second.direction == com.baynana.domain.ledger.LineDirection.CHARGE }
        MovementFilter.PAYMENTS -> all.filter { it.second.direction == com.baynana.domain.ledger.LineDirection.PAYMENT }
        MovementFilter.WAITING -> all.filter { it.second.awaitingMyAck }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                Text(
                    "الحركات",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "كل ما جرى في غرفك، الأحدث أولًا. الغرفة مكتوبة على كل سطر حتى لا تختلط الحسابات.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MovementFilter.entries.forEach { option ->
                    FilterChip(
                        label = option.label,
                        selected = filter == option,
                        onClick = { filter = option }
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            item {
                EmptyState(
                    title = if (all.isEmpty()) "لا حركة بعد" else "لا شيء في هذا الفلتر",
                    body = if (all.isEmpty()) {
                        "افتح غرفة وسجّل أول قيد، أو استقبل قيدًا من طرف مقابل. كل قيد يظهر هنا فورًا."
                    } else {
                        "جرّب فلترًا آخر — القيود موجودة لكن ليست من هذا النوع."
                    }
                )
            }
        }

        items(visible, key = { it.second.entryId }) { (room, entry) ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        room.title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(0.dp))
                    if (room.awaitingMyAcknowledgement > 0) {
                        Spacer(Modifier.padding(start = 8.dp))
                        InfoPill(
                            text = "ينتظر إقرارك: ${room.awaitingMyAcknowledgement}",
                            container = BaynanaStatus.colors.waitingContainer,
                            onContainer = BaynanaStatus.colors.onWaitingContainer
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                EntryCard(
                    entry = entry,
                    currency = room.currency,
                    dateText = dateFormat.format(Date(entry.occurredAt)),
                    onClick = { onOpenRoom(room.roomId) }
                )
            }
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        androidx.compose.material3.Button(onClick = onClick) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        androidx.compose.material3.OutlinedButton(onClick = onClick) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

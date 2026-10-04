package com.baynana.features.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.room.withTransaction
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.baynana.core.database.AppDatabase
import com.baynana.core.util.LinkCodeGenerator
import com.baynana.data.local.ledger.LedgerHomeRepository
import com.baynana.data.local.ledger.LedgerRoom
import com.baynana.data.local.ledger.RoomMember
import com.baynana.domain.ledger.RoomKind
import com.baynana.domain.ledger.RoomStatus
import com.baynana.features.home.BaynanaHomeViewModel
import com.baynana.ui.components.EmptyState
import com.baynana.ui.components.RoomRow
import com.baynana.ui.components.RoomStatusChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * «غرفي»: كل حساب بينك وبين طرف واحد — مسرب، بقالة، دلال، قريب.
 *
 * الشاشة تعرض نفس أرقام الرئيسية (من `RoomFeed`)، وتتيح إنشاء غرفة جديدة بكود ربط. ولا تعرض شيئًا
 * من غرفة لم يقبلها الطرف الآخر: الغرفة تبدأ `PENDING` والقيد لا يظهر للآخر قبل القبول (ح٣).
 */
@Composable
fun RoomsScreen(
    state: BaynanaHomeViewModel.UiState,
    onOpenRoom: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }

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
                    "غرفي",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { showCreate = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("غرفة جديدة")
                }
            }
        }

        if (state.rooms.isEmpty() && !state.loading) {
            item {
                EmptyState(
                    title = "لا غرفة بعد",
                    body = "الغرفة حساب واحد بينك وبين طرف واحد: لا تختلط ديون البقالة بديون الري. " +
                        "أنشئ غرفة، أعطِ الطرف كود الربط، ولا يظهر لكما قيد إلا بعد أن يقبل الطرفان.",
                    actionLabel = "أنشئ غرفة",
                    onAction = { showCreate = true }
                )
            }
        }

        items(state.rooms, key = { it.roomId }) { room ->
            RoomRow(
                title = room.title,
                subtitle = if (room.linkCode.isNotBlank()) "كود الربط: ${room.linkCode}" else room.counterpartName,
                netMinor = room.myNetMinor,
                currency = room.currency,
                awaiting = room.awaitingMyAcknowledgement,
                statusChip = { RoomStatusChip(room.status) },
                onClick = { onOpenRoom(room.roomId) }
            )
        }

        item { Spacer(Modifier.height(18.dp)) }
    }

    if (showCreate) {
        CreateRoomDialog(
            onDismiss = { showCreate = false },
            onCreated = {
                showCreate = false
                onRefresh()
            }
        )
    }
}

/** إنشاء غرفة: النوع + الاسم + الهاتف + كود ربط يُنشأ تلقائيًا. */
@Composable
private fun CreateRoomDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kind by remember { mutableStateOf(RoomKind.WATER) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("غرفة جديدة", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "اختر نوع الحساب، واكتب اسم الطرف. لا تُخلط أرصدة نوعين.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindChip("ري", kind == RoomKind.WATER) { kind = RoomKind.WATER }
                    KindChip("بقالة", kind == RoomKind.SHOP) { kind = RoomKind.SHOP }
                    KindChip("سوق", kind == RoomKind.MARKET) { kind = RoomKind.MARKET }
                    KindChip("عام", kind == RoomKind.GENERAL) { kind = RoomKind.GENERAL }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم الطرف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("هاتفه (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank()) {
                    error = "اكتب اسم الطرف: الغرفة بلا اسم لا يُعرف صاحبها"
                    return@Button
                }
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) {
                            val db = AppDatabase.getDatabase(context)
                            val now = System.currentTimeMillis()
                            val roomId = "room-${UUID.randomUUID()}"
                            val linkCode = LinkCodeGenerator.generate()
                            db.withTransaction {
                                db.ledgerDao().upsertRoom(
                                    LedgerRoom(
                                        id = roomId,
                                        kind = kind,
                                        currency = "YER_NEW",
                                        title = name.trim(),
                                        status = RoomStatus.PENDING,
                                        linkCode = linkCode,
                                        counterpartName = name.trim(),
                                        counterpartPhone = phone.trim(),
                                        createdAt = now,
                                        updatedAt = now
                                    )
                                )
                                db.ledgerDao().upsertMember(
                                    RoomMember(
                                        roomId = roomId,
                                        memberId = LedgerHomeRepository.MY_MEMBER_ID,
                                        displayName = "أنا",
                                        isMe = true,
                                        role = "owner",
                                        joinedAt = now
                                    )
                                )
                                db.ledgerDao().upsertMember(
                                    RoomMember(
                                        roomId = roomId,
                                        memberId = "counterpart-$roomId",
                                        displayName = name.trim(),
                                        phone = phone.trim(),
                                        role = "counterpart",
                                        joinedAt = now
                                    )
                                )
                            }
                        }
                    }.fold(
                        onSuccess = { onCreated() },
                        onFailure = { error = it.message ?: "تعذّر إنشاء الغرفة" }
                    )
                }
            }) { Text("أنشئ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

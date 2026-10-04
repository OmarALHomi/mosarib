package com.baynana.features.rooms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.core.database.AppDatabase
import com.baynana.data.local.ledger.LedgerHomeRepository
import com.baynana.data.local.ledger.LedgerRepository
import com.baynana.domain.ledger.AllocationMode
import com.baynana.domain.ledger.EntryType
import com.baynana.domain.ledger.NewDebtSpec
import com.baynana.domain.ledger.ReceiptSpec
import com.baynana.domain.money.Currency
import com.baynana.domain.money.MoneyParse
import com.baynana.domain.money.MoneyParser
import com.baynana.ui.components.amountText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * حوارا الكتابة: قيد دَين جديد، وسداد/قبض.
 *
 * قواعد ملزمة في هذين الحوارين:
 * - المبلغ يُقرأ بـ[MoneyParser] **بالصيغة التي بنت ADR-04** (نصّ → فلس)، فلا `Double` في الواجهة.
 * - الحفظ **محلي فوري** في معاملة واحدة (قيد + سطر صادر)، ثم يُقال للمستخدم بصدق: «حُفظ في دفترك —
 *   سيصل الطرف بعد الاتصال». ولا وعد بمزامنة لم تحدث.
 * - القبض العام (بلا تخصيص) والخيارات الثلاثة ظاهرة بالعربية، والزائد يُعلن كرصيد دائن في الغرفة.
 */

private fun parseAmount(text: String, currencyCode: String): Result<Long> {
    val currency = Currency.fromCode(currencyCode)
        ?: return Result.failure(IllegalArgumentException("عملة غير معروفة: $currencyCode"))
    return when (val parsed = MoneyParser.parse(text, currency)) {
        is MoneyParse.Ok -> Result.success(parsed.money.minor)
        is MoneyParse.Error -> Result.failure(IllegalArgumentException(parsed.message))
    }
}

@Composable
fun AddDebtDialog(
    roomId: String,
    currency: String,
    counterpartName: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(EntryType.WATER_SESSION) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قيد جديد", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "القيد يُسجَّل في دفترك فورًا، ويظهر للطرف بعد قبول الربط. والمال على من استلم المنفعة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip("سقية", type == EntryType.WATER_SESSION) { type = EntryType.WATER_SESSION }
                    TypeChip("دَين سلعة", type == EntryType.GOODS_DEBT) { type = EntryType.GOODS_DEBT }
                    TypeChip("صلح", type == EntryType.SETTLEMENT) { type = EntryType.SETTLEMENT }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ (بالريال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("البيان — مثال: سقية 3 ساعات من البئر") },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val minor = parseAmount(amount, currency)
                    minor.fold(
                        onSuccess = { value ->
                            if (value <= 0L) {
                                error = "المبلغ يجب أن يكون أكبر من صفر"
                                return@fold
                            }
                            saving = true
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        val db = AppDatabase.getDatabase(context)
                                        LedgerRepository(db).recordDebt(
                                            NewDebtSpec(
                                                id = "entry-${UUID.randomUUID()}",
                                                operationId = "op-${UUID.randomUUID()}",
                                                roomId = roomId,
                                                type = type,
                                                debtorMemberId = "counterpart-$roomId",
                                                creditorMemberId = LedgerHomeRepository.MY_MEMBER_ID,
                                                amountMinor = value,
                                                currency = currency,
                                                occurredAt = System.currentTimeMillis(),
                                                description = description.trim().ifBlank {
                                                    when (type) {
                                                        EntryType.WATER_SESSION -> "سقية"
                                                        EntryType.GOODS_DEBT -> "دَين سلعة"
                                                        else -> "صلح"
                                                    }
                                                },
                                                createdByMemberId = LedgerHomeRepository.MY_MEMBER_ID
                                            )
                                        )
                                    }
                                }.fold(
                                    onSuccess = { onSaved() },
                                    onFailure = { error = it.message ?: "تعذّر الحفظ" }
                                )
                            }
                        },
                        onFailure = { error = it.message ?: "مبلغ غير صالح" }
                    )
                }
            ) { Text(if (saving) "جاري الحفظ…" else "احفظ في دفتري") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun ReceiptDialog(
    roomId: String,
    currency: String,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var amount by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AllocationMode.OldestFirst) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("سداد / قبض", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "من استلم المال صار عليه، فالقبض يقابل الدَين ويتقاصّان. اختر كيف يُخصَّص:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TypeChip("الأقدم فالأقدم", mode is AllocationMode.OldestFirst) { mode = AllocationMode.OldestFirst }
                    TypeChip("قبض عام", mode is AllocationMode.None) { mode = AllocationMode.None }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("المبلغ (بالريال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    when (mode) {
                        is AllocationMode.None ->
                            "القبض العام لا يُغلق دَينًا بعينه: يبقى رصيدًا دائنًا في هذه الغرفة حتى تقرّرا."
                        else ->
                            "سيُسدَّد الأقدم فالأقدم داخل هذه الغرفة والعملة نفسها، والزائد يبقى رصيدًا دائنًا هنا." 
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    parseAmount(amount, currency).fold(
                        onSuccess = { value ->
                            if (value <= 0L) {
                                error = "المبلغ يجب أن يكون أكبر من صفر"
                                return@fold
                            }
                            saving = true
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        val db = AppDatabase.getDatabase(context)
                                        LedgerRepository(db).recordReceipt(
                                            ReceiptSpec(
                                                id = "entry-${UUID.randomUUID()}",
                                                operationId = "op-${UUID.randomUUID()}",
                                                roomId = roomId,
                                                debtorMemberId = LedgerHomeRepository.MY_MEMBER_ID,
                                                creditorMemberId = "counterpart-$roomId",
                                                amountMinor = value,
                                                currency = currency,
                                                occurredAt = System.currentTimeMillis(),
                                                mode = mode,
                                                description = if (mode is AllocationMode.None) "قبض عام" else "سداد",
                                                createdByMemberId = LedgerHomeRepository.MY_MEMBER_ID
                                            )
                                        )
                                    }
                                }.fold(
                                    onSuccess = { result ->
                                        val text = if (result.unappliedMinor > 0L) {
                                            "حُفظ في دفترك — ${amountText(result.unappliedMinor, currency)} بقيت رصيدًا دائنًا في هذه الغرفة"
                                        } else {
                                            "حُفظ في دفترك — سيصل الطرف بعد الاتصال"
                                        }
                                        onSaved(text)
                                    },
                                    onFailure = { error = it.message ?: "تعذّر الحفظ" }
                                )
                            }
                        },
                        onFailure = { error = it.message ?: "مبلغ غير صالح" }
                    )
                }
            ) { Text(if (saving) "جاري الحفظ…" else "احفظ في دفتري") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label, style = MaterialTheme.typography.labelMedium) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label, style = MaterialTheme.typography.labelMedium) }
    }
}

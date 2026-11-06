package com.example.core.voice

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.isAppDark
import com.example.ui.theme.mosarib
import com.example.core.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Editable fields of the "فهمت" card. */
private enum class UField { TYPE, CUSTOMER, DURATION, RATE, AMOUNT, PAID, NOTES }

private fun typeLabel(t: VoiceCommandType?) = when (t) {
    VoiceCommandType.SESSION -> "سقي"
    VoiceCommandType.RECEIPT -> "سند قبض"
    VoiceCommandType.EXPENSE -> "سند صرف"
    VoiceCommandType.PURCHASE -> "شراء ساعات"
    VoiceCommandType.DISCOUNT -> "خصم ومسامحة"
    null -> "عملية"
}

private fun typeColor(t: VoiceCommandType?) = when (t) {
    VoiceCommandType.RECEIPT -> Color(0xFF43A047)
    VoiceCommandType.EXPENSE, VoiceCommandType.DISCOUNT -> Color(0xFFF4511E)
    VoiceCommandType.PURCHASE -> Color(0xFF8E24AA)
    else -> Color(0xFF00ACC1)
}

fun durationText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    val hours = when (h) {
        0 -> ""
        1 -> "ساعة"
        2 -> "ساعتان"
        in 3..10 -> "$h ساعات"
        else -> "$h ساعة"
    }
    return when {
        m == 0 -> hours
        h == 0 -> "$m دقيقة"
        m == 30 -> "$hours ونصف"
        m == 15 -> "$hours وربع"
        else -> "$hours و$m دقيقة"
    }
}

/** "2.5" -> 150, "2:30" -> 150, "3" -> 180. Null when it is not a duration. */
fun parseEditedDuration(raw: String): Int? {
    val s = raw.trim().replace('٫', '.').replace(',', '.')
    Regex("^(\\d{1,2}):(\\d{1,2})$").find(s)?.let {
        return it.groupValues[1].toInt() * 60 + it.groupValues[2].toInt()
    }
    val h = s.toDoubleOrNull() ?: return null
    return (h * 60).toInt().takeIf { it in 1..(48 * 60) }
}

/**
 * What the voice parser understood, as a card: one row per field, any row can be tapped and
 * edited before continuing. Read-aloud and retry sit at the top.
 */
@Composable
fun VoiceUnderstoodCard(
    command: VoiceCommand,
    heard: String,
    onContinue: (VoiceCommand) -> Unit,
    onRetry: () -> Unit,
    // Returns why the command cannot be saved directly, or null when it can.
    saveBlockReason: ((VoiceCommand) -> String?)? = null,
    onDirectSave: ((VoiceCommand) -> Unit)? = null,
    onSpeakMore: (() -> Unit)? = null
) {
    var cmd by remember(command) { mutableStateOf(command) }
    var editing by remember { mutableStateOf<UField?>(null) }
    val isDark = isAppDark()
    val accent = typeColor(cmd.type)
    val isTimed = cmd.type == VoiceCommandType.SESSION || cmd.type == VoiceCommandType.PURCHASE ||
        (cmd.type == null && cmd.durationMinutes != null)
    val total = if (isTimed && cmd.durationMinutes != null && cmd.ratePerHour != null) {
        Formatters.roundMoney(cmd.durationMinutes!! / 60.0 * cmd.ratePerHour!!)
    } else null
    val timeFmt = remember { SimpleDateFormat("h:mm", Locale.US) }
    val hourFmt = remember { SimpleDateFormat("H", Locale.US) }
    // Each time is isolated (U+2067…U+2069) so digits and ص/م keep right-to-left order.
    fun clock(ms: Long): String {
        val d = Date(ms)
        val pm = hourFmt.format(d).toInt() >= 12
        return "⁧${timeFmt.format(d)} ${if (pm) "م" else "ص"}⁩"
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDark) Color(0xFF0B2C31) else MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, accent),
            shadowElevation = if (isDark) 0.dp else 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Header: ✓ «فهمت: سقي»  …  read aloud · retry
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(30.dp).background(accent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فهمت: ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = typeLabel(cmd.type),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.weight(1f).clickable { editing = if (editing == UField.TYPE) null else UField.TYPE }
                    )
                    com.example.core.speech.SpeakButton(
                        text = { buildVoiceCommandSummary(cmd).removePrefix("فهمت: ") },
                        iconSize = 22.dp
                    )
                    IconButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة المحاولة", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                if (editing == UField.TYPE) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        listOf(VoiceCommandType.SESSION, VoiceCommandType.RECEIPT, VoiceCommandType.EXPENSE, VoiceCommandType.PURCHASE).forEach { t ->
                            FilterChip(
                                selected = cmd.type == t,
                                onClick = { cmd = cmd.copy(type = t); editing = null },
                                label = { Text(typeLabel(t)) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val name = cmd.customerQuery
                FieldRow("العميل", name, UField.CUSTOMER, editing, { editing = it }, KeyboardType.Text) { v ->
                    cmd = cmd.copy(customerQuery = v.ifBlank { null })
                }
                if (cmd.startMillis != null && cmd.endMillis != null) {
                    ValueRow("الوقت", "من ${clock(cmd.startMillis!!)} إلى ${clock(cmd.endMillis!!)}")
                }
                if (isTimed) {
                    FieldRow("المدة", cmd.durationMinutes?.let { durationText(it) }, UField.DURATION, editing, { editing = it },
                        KeyboardType.Decimal, hint = "عدد الساعات، مثل 2.5 أو 2:30",
                        editValue = cmd.durationMinutes?.let { "%d:%02d".format(it / 60, it % 60) }) { v ->
                        parseEditedDuration(v)?.let { cmd = cmd.copy(durationMinutes = it, startMillis = null, endMillis = null) }
                    }
                    MoneyRow("سعر الساعة", cmd.ratePerHour, UField.RATE, editing, { editing = it }) { cmd = cmd.copy(ratePerHour = it) }
                    total?.let { ValueRow("الإجمالي", Formatters.formatNumber(it) + " ريال", bold = true) }
                    val paidText = when {
                        cmd.paidFull -> "مسلّم كامل ✓"
                        cmd.paid != null -> Formatters.formatNumber(cmd.paid!!)
                        else -> null
                    }
                    FieldRow("المدفوع", paidText, UField.PAID, editing, { editing = it }, KeyboardType.Number,
                        editValue = cmd.paid?.let { Formatters.formatAmountInput(it.toString()) }) { v ->
                        val p = Formatters.parseAmountInput(v)
                        cmd = cmd.copy(paid = p.takeIf { it > 0 }, paidFull = false)
                    }
                    if (total != null && !cmd.paidFull) {
                        val remaining = (total - (cmd.paid ?: 0.0)).coerceAtLeast(0.0)
                        ValueRow("المتبقي", Formatters.formatNumber(remaining) + " ريال", bold = true,
                            color = if (remaining > 0) MaterialTheme.mosarib.debt else MaterialTheme.mosarib.success)
                    }
                } else {
                    MoneyRow("المبلغ", cmd.amount ?: cmd.paid, UField.AMOUNT, editing, { editing = it }) { cmd = cmd.copy(amount = it) }
                    (cmd.amount ?: cmd.paid)?.takeIf { it > 0 }?.let {
                        ValueRow("بالحروف", Formatters.amountToArabicWords(it, "ريال"))
                    }
                }
                FieldRow("الملاحظة", cmd.notes ?: cmd.category, UField.NOTES, editing, { editing = it }, KeyboardType.Text) { v ->
                    cmd = cmd.copy(notes = v.ifBlank { null })
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "سمعت: «$heard»  •  اضغط أي سطر لتعديله",
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.mosarib.textMuted,
                textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (onSpeakMore != null) {
            androidx.compose.material3.TextButton(onClick = { editing = null; onSpeakMore() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("أكمل بالصوت (أضف ما نسيته)")
            }
        }
        if (onDirectSave != null) {
            val blocked = saveBlockReason?.invoke(cmd)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MosaribPrimaryButton(
                    text = "حفظ مباشر",
                    onClick = { editing = null; onDirectSave(cmd) },
                    enabled = blocked == null,
                    modifier = Modifier.weight(1f)
                )
                com.example.core.ui.MosaribSecondaryButton(
                    text = "فتح النموذج",
                    onClick = { editing = null; onContinue(cmd) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (blocked != null) {
                Text(
                    text = blocked,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.mosarib.warning,
                        textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                    ),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp)
                )
            }
        } else {
            MosaribPrimaryButton(
                text = "متابعة",
                onClick = { editing = null; onContinue(cmd) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ValueRow(label: String, value: String, bold: Boolean = false, color: Color? = null) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.mosarib.textMuted, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Bold,
                textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
            ),
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MoneyRow(
    label: String,
    value: Double?,
    field: UField,
    editing: UField?,
    setEditing: (UField?) -> Unit,
    onValue: (Double?) -> Unit
) {
    FieldRow(label, value?.let { Formatters.formatNumber(it) }, field, editing, setEditing, KeyboardType.Number,
        editValue = value?.let { Formatters.formatAmountInput(it.toString()) }) { v ->
        onValue(Formatters.parseAmountInput(v).takeIf { it > 0 })
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String?,
    field: UField,
    editing: UField?,
    setEditing: (UField?) -> Unit,
    keyboard: KeyboardType,
    hint: String? = null,
    editValue: String? = value,
    onSave: (String) -> Unit
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    if (editing == field) {
        var text by remember(field) { mutableStateOf(editValue.orEmpty()) }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = if (keyboard == KeyboardType.Number) Formatters.formatAmountInput(it) else it },
                label = { Text(label) },
                placeholder = hint?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onSave(text); setEditing(null) }) {
                Icon(Icons.Default.Check, contentDescription = "حفظ التعديل", tint = MaterialTheme.colorScheme.primary)
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { setEditing(field) }
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.mosarib.textMuted, modifier = Modifier.weight(1f))
            Text(
                value ?: "اضغط للإضافة",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (value != null) FontWeight.Bold else FontWeight.Normal),
                color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.mosarib.textMuted
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.mosarib.textMuted, modifier = Modifier.size(14.dp))
        }
    }
}

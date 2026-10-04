package com.baynana.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baynana.data.local.migration.LegacyMigrationOutcome
import com.baynana.domain.migration.LegacyMigrationPlan
import com.baynana.ui.components.InfoPill
import com.baynana.ui.components.amountText
import com.baynana.ui.theme.BaynanaStatus

/**
 * «أرني الجرد أولًا»: كل عميل قديم، وما كان عليه، وما سيُرحَّل، وهل يطابق.
 *
 * هذا هو التزام ح٧/ح٩ المعلن: **لا ترحيل صامت**. ما لا يطابق يُرفض ويُعلن سببه، والقرار للمالك.
 */
@Composable
fun MigrationDialog(
    plan: LegacyMigrationPlan,
    running: Boolean,
    outcome: LegacyMigrationOutcome?,
    error: String?,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    val colors = BaynanaStatus.colors
    AlertDialog(
        onDismissRequest = { if (!running) onDismiss() },
        title = { Text("الترحيل من الدفتر القديم", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    "المصدر: دفتر المسرب القديم. والترحيل ينقل **المتبقي** فقط، وبتاريخ آخر سقية مسجّل، " +
                        "ولا يحذف شيئًا من الدفتر القديم.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoPill(
                        text = "عملاء: ${plan.reconciliations.size}",
                        container = colors.infoContainer,
                        onContainer = colors.onInfoContainer
                    )
                    InfoPill(
                        text = "قيود: ${plan.entries.size}",
                        container = colors.infoContainer,
                        onContainer = colors.onInfoContainer
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "الإجمالي القديم: ${amountText(plan.legacyTotalMinor, plan.currency)}  ←  " +
                        "المرحَّل: ${amountText(plan.plannedTotalMinor, plan.currency)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                plan.reconciliations.forEach { item ->
                    val pill = if (item.matches) {
                        InfoPill("يطابق", colors.acknowledgedContainer, colors.onAcknowledgedContainer)
                    } else {
                        InfoPill("لا يطابق", colors.dangerContainer, colors.onDangerContainer)
                    }
                    Column(modifier = Modifier.heightIn(min = 0.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.customerName.ifBlank { "عميل #${item.customerId}" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            pill
                        }
                        Text(
                            text = "قديم: ${amountText(item.legacyMinor, plan.currency)} • " +
                                "مرحَّل: ${amountText(item.plannedMinor, plan.currency)}" +
                                if (item.matches) "" else " • ${item.warnings.firstOrNull() ?: "الجرد لا يطابق"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.matches) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (plan.notes.isNotEmpty()) {
                    Text("ملاحظات", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    plan.notes.forEach { note ->
                        Text(
                            "• $note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                outcome?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        it.summaryText(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onAcknowledgedContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(enabled = !running && plan.allReconcile && outcome == null, onClick = onApply) {
                Text(if (running) "جاري الترحيل…" else "رحّل الآن")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !running) { Text("لاحقًا") } }
    )
}

package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** نافذة اختيار طريقة الاستخدام (مبسّطة/كاملة): لا تُغلق إلا بالاختيار. */
@Composable
fun UsageModeChooserDialog(currentSimple: Boolean, onChoose: (Boolean) -> Unit) {
    var simple by remember { mutableStateOf(true) } // المبسّطة مُحددة مسبقاً
    MosaribDialog(
        onDismissRequest = {},
        title = "اختر طريقة الاستخدام",
        confirmText = "متابعة",
        onConfirm = { onChoose(simple) },
        dismissText = null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ModeCard("مبسّطة", "الأساسيات فقط بخط واضح — مناسبة لأغلب المستخدمين", true, simple) { simple = true }
            ModeCard("كاملة", "كل الخيارات والتفاصيل", false, !simple) { simple = false }
            Text(
                "يمكنك تغييرها لاحقاً من الإعدادات ← طريقة الاستخدام",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeCard(title: String, desc: String, recommended: Boolean, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null)
            Column(Modifier.padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (recommended) {
                        Text(
                            "  مُستحسن",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

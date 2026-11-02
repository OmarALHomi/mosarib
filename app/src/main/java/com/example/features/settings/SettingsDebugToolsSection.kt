package com.example.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

// Section: أدوات التطوير واختبار البيانات (تظهر فقط في نسخة Debug)
@Composable
internal fun DebugToolsSection(
    isSeedingData: Boolean,
    onSeedClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = AccentGold)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "أدوات المطور واختبار البيانات (Debug)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "بيانات تجريبية لـ 100 عميل و 1000 عملية لاختبار التطبيق الشامل",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isSeedingData) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "جاري معالجة العمليات والبيانات... يرجى الانتظار",
                    style = MaterialTheme.typography.bodySmall.copy(color = AccentGold),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSeedClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🌱 توليد 100عميل و1000عملية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onClearClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("🧹 تصفير البيانات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ConfirmSeedDebugDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "توليد بيانات تجريبية ضخمة",
        icon = Icons.Default.Settings,
        tone = DialogTone.Neutral,
        message = "سيتم توليد 100 عميل يمني، و 1,000 عملية متنوعة تشمل جلسات سقي، سندات قبض وصرف، وخصومات تسوية، ومشتريات ماء شبكة. هل تريد المتابعة؟",
        confirmText = "بدء التوليد",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    )
}

@Composable
internal fun ConfirmClearDebugDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تأكيد تصفير البيانات",
        icon = Icons.Default.Delete,
        tone = DialogTone.Danger,
        message = "تنبيه: سيتم مسح كافة العملاء وجلسات السقي والسندات والمشتريات الحالية في قاعدة البيانات لاختبار بيئة نظيفة. هل أنت متأكد؟",
        confirmText = "تصفير الكل",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    )
}

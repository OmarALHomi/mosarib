package com.example.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.runtime.Composable

/**
 * نافذة تذكير النسخ الاحتياطي الدوري الموحدة لتطبيق مُسَرِّب.
 */
@Composable
fun BackupReminderDialog(
    onBackupNow: () -> Unit,
    onRemindLater: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onRemindLater,
        title = "تذكير: حماية بياناتك المحاسبية",
        icon = Icons.Default.Backup,
        tone = DialogTone.Warning,
        message = "مضى وقت منذ آخر نسخة احتياطية! لحماية ديون وسجلات المزارعين وأصحاب الآبار من الضياع في حال تلف أو فقدان الهاتف، يُنصح بحفظ أو مشاركة نسخة احتياطية الآن.",
        confirmText = "نسخ احتياطي الآن",
        onConfirm = onBackupNow,
        dismissText = "تذكيري لاحقاً",
        onDismiss = onRemindLater
    )
}

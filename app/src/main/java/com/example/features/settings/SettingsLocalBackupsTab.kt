package com.example.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.AutoBackupEngine
import com.example.core.util.BackupManager
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

// Content of the "on phone" backup tab: smart auto-backup panel + saved local backups
@Composable
internal fun LocalBackupsTab(
    isDark: Boolean,
    isAutoBackupEnabled: Boolean,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    autoBackups: List<AutoBackupEngine.AutoBackupItem>,
    savedBackups: List<BackupManager.BackupFileInfo>,
    expandAllLocalBackups: Boolean,
    onToggleExpandAllLocalBackups: () -> Unit,
    onRestoreAutoBackup: (AutoBackupEngine.AutoBackupItem) -> Unit,
    onDeleteAutoBackup: (AutoBackupEngine.AutoBackupItem) -> Unit,
    onCleanOldBackups: () -> Unit,
    onRefreshBackups: () -> Unit,
    onRestoreBackup: (BackupManager.BackupFileInfo) -> Unit,
    onShareBackup: (BackupManager.BackupFileInfo) -> Unit,
    onDeleteBackup: (BackupManager.BackupFileInfo) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ─── قسم النسخ التلقائي الدوري الذكي (Smart Rolling Auto-Backup) ───
        AutoBackupPanel(
            isDark = isDark,
            isAutoBackupEnabled = isAutoBackupEnabled,
            onAutoBackupEnabledChange = onAutoBackupEnabledChange,
            autoBackups = autoBackups,
            expandAllLocalBackups = expandAllLocalBackups,
            onRestoreAutoBackup = onRestoreAutoBackup,
            onDeleteAutoBackup = onDeleteAutoBackup
        )

        // ─── محتوى النسخ المحفوظة على الهاتف ───
        if (savedBackups.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "النسخ المحلية المحفوظة:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (savedBackups.size > 3) {
                        TextButton(
                            onClick = onCleanOldBackups,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("تنظيف القديمة", fontSize = 10.5.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onRefreshBackups, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Restore, contentDescription = "تحديث", modifier = Modifier.size(15.dp), tint = PrimaryTeal)
                    }
                }
            }

            val displayedBackups = if (expandAllLocalBackups) savedBackups else savedBackups.take(3)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                displayedBackups.forEach { backup ->
                    LocalBackupRow(
                        backup = backup,
                        isDark = isDark,
                        onRestore = { onRestoreBackup(backup) },
                        onShare = { onShareBackup(backup) },
                        onDelete = { onDeleteBackup(backup) }
                    )
                }

                if (savedBackups.size > 3) {
                    TextButton(
                        onClick = onToggleExpandAllLocalBackups,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        Icon(
                            if (expandAllLocalBackups) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryTeal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (expandAllLocalBackups) "عرض أحدث 3 نسخ فقط ▲" else "عرض كافة النسخ على الهاتف (${savedBackups.size}) ▼",
                            fontSize = 11.sp,
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else if (autoBackups.isEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "لا توجد نسخ احتياطية على الهاتف بعد. اضغط على 'نسخة جديدة' لإنشاء نسخة احتياطية.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AutoBackupPanel(
    isDark: Boolean,
    isAutoBackupEnabled: Boolean,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    autoBackups: List<AutoBackupEngine.AutoBackupItem>,
    expandAllLocalBackups: Boolean,
    onRestoreAutoBackup: (AutoBackupEngine.AutoBackupItem) -> Unit,
    onDeleteAutoBackup: (AutoBackupEngine.AutoBackupItem) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF131F24) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isAutoBackupEnabled) AccentEmerald.copy(alpha = 0.35f) else Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isAutoBackupEnabled) AccentEmerald.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = if (isAutoBackupEnabled) AccentEmerald else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "النسخ الاحتياطي اليومي التلقائي",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (isAutoBackupEnabled) "مفعل: حفظ يومي تلقائي ونقاط أمان" else "متوقف حالياً",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isAutoBackupEnabled) AccentEmerald else Color.Gray,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
                Switch(
                    checked = isAutoBackupEnabled,
                    onCheckedChange = onAutoBackupEnabledChange
                )
            }

            if (isAutoBackupEnabled) {
                if (autoBackups.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val displayedAuto = if (expandAllLocalBackups) autoBackups else autoBackups.take(2)
                        displayedAuto.forEach { autoItem ->
                            AutoBackupRow(
                                autoItem = autoItem,
                                isDark = isDark,
                                onRestore = { onRestoreAutoBackup(autoItem) },
                                onDelete = { onDeleteAutoBackup(autoItem) }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "جاهز: سيتم أخذ نسخة احتياطية كاملة تلقائياً كل يوم عند فتح التطبيق ونقاط أمان وقائية قبل العمليات.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        ),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoBackupRow(
    autoItem: AutoBackupEngine.AutoBackupItem,
    isDark: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (autoItem.isSafetyCheckpoint) AccentGold.copy(alpha = 0.12f) else PrimaryTeal.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (autoItem.isSafetyCheckpoint) AccentGold.copy(alpha = 0.2f) else AccentEmerald.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (autoItem.isSafetyCheckpoint) Icons.Default.Security else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (autoItem.isSafetyCheckpoint) AccentGold else AccentEmerald,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = autoItem.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = "${autoItem.formattedDate} • ${autoItem.sizeText}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp),
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (autoItem.isSafetyCheckpoint) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color(0xFF3B1212) else Color(0xFFFEE2E2))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "حذف",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF2B2111) else Color(0xFFFEF3C7))
                    .clickable(onClick = onRestore)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "استعادة",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun LocalBackupRow(
    backup: BackupManager.BackupFileInfo,
    isDark: Boolean,
    onRestore: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryTeal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(15.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = backup.formattedDate,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                )
                Text(
                    text = "${backup.sizeText} • محلي",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp),
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF2B2111) else Color(0xFFFEF3C7))
                    .clickable(onClick = onRestore)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    "استعادة",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                )
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF132A1C) else Color(0xFFE8F5E9))
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "مشاركة",
                    tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                    modifier = Modifier.size(12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0xFF2A1517) else Color(0xFFFFEBEE))
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "حذف",
                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFE53935),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

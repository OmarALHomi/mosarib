package com.example.features.settings

import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.example.core.license.LicenseManager
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
import com.example.core.util.Formatters
import com.example.core.util.GoogleDriveBackupHelper
import com.example.features.customers.DuplicateCandidate
import com.example.features.customers.DuplicateReason
import com.example.ui.theme.mosarib
import java.io.File

// Hosts every restore / delete / clean confirmation raised by the backup section
@Composable
internal fun BackupDialogsHost(
    state: SettingsBackupUiState,
    onRestoreBackupFile: (File) -> Unit,
    onDeleteBackupFile: (File) -> Unit,
    onRestoreDriveBackup: (GoogleDriveBackupHelper.DriveBackupFile) -> Unit,
    onRestoreImportedFile: (Uri) -> Unit,
    onCleanOldBackups: () -> Unit,
    onDeleteDriveBackup: (GoogleDriveBackupHelper.DriveBackupFile) -> Unit,
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // Confirmation Dialogs for Restore with Critical Data Loss Warning
    val toRestore = state.backupToRestore
    if (toRestore != null) {
        RestoreWarningDialog(
            title = "استعادة نسخة احتياطية محلية",
            backupDateOrName = toRestore.formattedDate,
            additionalInfo = toRestore.sizeText,
            onConfirm = {
                onRestoreBackupFile(toRestore.file)
                state.backupToRestore = null
            },
            onDismiss = { state.backupToRestore = null }
        )
    }

    val autoToRestore = state.autoBackupToRestore
    if (autoToRestore != null) {
        RestoreWarningDialog(
            title = "استعادة " + autoToRestore.title,
            backupDateOrName = autoToRestore.formattedDate,
            additionalInfo = autoToRestore.sizeText,
            onConfirm = {
                onRestoreBackupFile(autoToRestore.file)
                state.autoBackupToRestore = null
            },
            onDismiss = { state.autoBackupToRestore = null }
        )
    }

    val autoToDelete = state.autoBackupToDelete
    if (autoToDelete != null) {
        DeleteSafetyCheckpointDialog(
            checkpointTitle = autoToDelete.title,
            onConfirm = {
                onDeleteBackupFile(autoToDelete.file)
                state.autoBackupToDelete = null
            },
            onDismiss = { state.autoBackupToDelete = null }
        )
    }

    val toDelete = state.backupToDelete
    if (toDelete != null) {
        DeleteLocalBackupDialog(
            backupName = toDelete.name,
            onConfirm = {
                onDeleteBackupFile(toDelete.file)
                state.backupToDelete = null
            },
            onDismiss = { state.backupToDelete = null }
        )
    }

    // Google Drive AppData Cloud Restore Dialog with Critical Data Loss Warning
    val driveToRestore = state.driveBackupToRestore
    if (driveToRestore != null) {
        RestoreWarningDialog(
            title = "استعادة من سحابة Google Drive",
            backupDateOrName = driveToRestore.formattedDate,
            additionalInfo = driveToRestore.sizeText,
            onConfirm = {
                onRestoreDriveBackup(driveToRestore)
                state.driveBackupToRestore = null
            },
            onDismiss = { state.driveBackupToRestore = null }
        )
    }

    // Imported File Restore Dialog with Critical Data Loss Warning
    val uri = state.uriToRestore
    if (uri != null) {
        RestoreWarningDialog(
            title = "استعادة من ملف مستورد",
            backupDateOrName = "الملف المختار من ذاكرة الجهاز",
            additionalInfo = null,
            onConfirm = {
                onRestoreImportedFile(uri)
                state.uriToRestore = null
            },
            onDismiss = { state.uriToRestore = null }
        )
    }

    if (state.showCleanOldBackupsConfirm) {
        CleanOldBackupsDialog(
            onConfirm = {
                onCleanOldBackups()
                state.showCleanOldBackupsConfirm = false
            },
            onDismiss = { state.showCleanOldBackupsConfirm = false }
        )
    }

    val driveToDelete = state.driveBackupToDelete
    if (driveToDelete != null) {
        DeleteDriveBackupDialog(
            backupName = driveToDelete.name,
            onConfirm = {
                onDeleteDriveBackup(driveToDelete)
                state.driveBackupToDelete = null
            },
            onDismiss = { state.driveBackupToDelete = null }
        )
    }

    val scanResults by viewModel.duplicateScanResults.collectAsStateWithLifecycle()
    if (scanResults != null) {
        ImportDuplicatesPreviewDialog(
            candidates = scanResults ?: emptyList(),
            onDismiss = { viewModel.dismissDuplicateScanDialog() },
            onDeleteDuplicates = { candidatesToDelete ->
                viewModel.deleteImportDuplicates(candidatesToDelete)
            }
        )
    }
}

// Delete confirmation for a safety checkpoint (auto-backup) item
@Composable
private fun DeleteSafetyCheckpointDialog(
    checkpointTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تأكيد حذف نقطة الأمان",
        icon = Icons.Default.Delete,
        tone = DialogTone.Danger,
        message = "هل تريد حذف نقطة الأمان الوقائية ($checkpointTitle) نهائياً؟",
        confirmText = "حذف",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    )
}

// Delete confirmation for a local backup file stored on the phone
@Composable
private fun DeleteLocalBackupDialog(
    backupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تأكيد حذف النسخة",
        icon = Icons.Default.Delete,
        tone = DialogTone.Danger,
        message = "هل تريد حذف ملف النسخة الاحتياطية ($backupName) نهائياً من الهاتف؟",
        confirmText = "حذف",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    )
}

// Clean Old Backups Confirmation Dialog
@Composable
private fun CleanOldBackupsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تنظيف النسخ القديمة",
        icon = Icons.Default.CleaningServices,
        tone = DialogTone.Danger,
        message = "سيتم حذف النسخ الاحتياطية القديمة على الهاتف والإبقاء على أحدث 3 نسخ فقط لتوفير المساحة وتجنب تراكم الملفات.\n\nهل تريد المتابعة؟",
        confirmText = "تنظيف الآن",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    )
}

// Google Drive AppData Cloud Delete Dialog
@Composable
private fun DeleteDriveBackupDialog(
    backupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "حذف نسخة من Google Drive",
        icon = Icons.Default.Delete,
        tone = DialogTone.Danger,
        message = "هل تريد حذف هذه النسخة السحابية ($backupName) نهائياً من سحابة Google Drive؟",
        confirmText = "حذف من السحابة",
        onConfirm = onConfirm,
        dismissText = "إلغاء",
        onDismiss = onDismiss
    )
}

@Composable
private fun ImportDuplicatesPreviewDialog(
    candidates: List<SettingsViewModel.DuplicateCandidateDisplay>,
    onDismiss: () -> Unit,
    onDeleteDuplicates: (List<DuplicateCandidate>) -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    if (showConfirmDelete) {
        MosaribDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = "تأكيد حذف العمليات المكررة",
            icon = Icons.Default.Delete,
            tone = DialogTone.Danger,
            message = "سيتم إنشاء نسخة احتياطية محلية أولاً، ثم حذف ${candidates.size} عملية مكررة. يمكنك الاستعادة من النسخة إن لزم.",
            confirmText = "حذف الآن",
            onConfirm = {
                showConfirmDelete = false
                onDeleteDuplicates(candidates.map { it.candidate })
            },
            dismissText = "إلغاء",
            onDismiss = { showConfirmDelete = false }
        )
    }

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "فحص العمليات المستوردة المكررة",
        icon = if (candidates.isEmpty()) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
        tone = if (candidates.isEmpty()) DialogTone.Success else DialogTone.Neutral,
        confirmText = if (candidates.isNotEmpty()) "حذف المكرر (${candidates.size})" else "حسناً",
        onConfirm = {
            if (candidates.isNotEmpty()) {
                showConfirmDelete = true
            } else {
                onDismiss()
            }
        },
        dismissText = if (candidates.isNotEmpty()) "إلغاء" else null,
        onDismiss = if (candidates.isNotEmpty()) onDismiss else null
    ) {
        if (candidates.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "لا توجد عمليات مكررة",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            val totalAmount = candidates.sumOf { it.candidate.amount }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Summary row: count and total amount
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "العدد: ${candidates.size} عملية",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "الإجمالي: ${Formatters.formatCurrency(totalAmount, "")}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.mosarib.debt
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(candidates) { item ->
                        DuplicateCandidateRow(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateCandidateRow(item: SettingsViewModel.DuplicateCandidateDisplay) {
    val candidate = item.candidate
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.customerName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (candidate.reason == DuplicateReason.TYPE_MISMATCH_TWIN) MaterialTheme.mosarib.warningContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = candidate.reason.displayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (candidate.reason == DuplicateReason.TYPE_MISMATCH_TWIN) MaterialTheme.mosarib.warning else MaterialTheme.mosarib.debt,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (candidate.notes.isNotBlank()) candidate.notes else "بدون بيان",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Formatters.formatCurrency(candidate.amount, ""),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.mosarib.debt)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = Formatters.formatDate(candidate.date),
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
            )
        }
    }
}

internal enum class BackupListMode { RESTORE, MANAGE }

internal class BackupListEntry(
    val isDrive: Boolean,
    val time: Long,
    val date: String,
    val size: String,
    val onRestore: () -> Unit,
    val onShare: (() -> Unit)?,
    val onDelete: () -> Unit
)

// Two tabs (Drive / phone): tap restores (restore mode) or share/delete icons (manage mode)
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun BackupListDialog(
    manage: Boolean,
    entries: List<BackupListEntry>,
    isLoading: Boolean,
    hasAccount: Boolean,
    onRetryDrive: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var driveTab by remember { mutableStateOf(hasAccount) }
    var online by remember { mutableStateOf(LicenseManager.isInternetAvailable(context)) }
    MosaribDialog(
        onDismissRequest = onDismiss,
        title = if (manage) "حذف نسخ" else "استعادة نسخة",
        confirmText = "إغلاق",
        onConfirm = onDismiss,
        dismissText = null
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = driveTab, onClick = { driveTab = true; online = LicenseManager.isInternetAvailable(context) }, label = { Text("Drive") })
            FilterChip(selected = !driveTab, onClick = { driveTab = false }, label = { Text("الهاتف") })
        }
        val shown = entries.filter { it.isDrive == driveTab }.sortedByDescending { it.time }
        if (driveTab && (!hasAccount || !online || shown.isEmpty())) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!online && hasAccount) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = when {
                        !hasAccount -> "لم يتم ربط حساب Google. اربطه من «النسخ التلقائي» أعلاه."
                        !online -> "لا يوجد اتصال بالإنترنت — لا يمكن عرض نسخ Drive الآن."
                        isLoading -> "جارٍ تحميل نسخ Drive..."
                        else -> "لا توجد نسخ على Drive"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!online && hasAccount) {
                    TextButton(onClick = {
                        online = LicenseManager.isInternetAvailable(context)
                        if (online) onRetryDrive()
                    }) { Text("إعادة المحاولة") }
                }
            }
        } else if (shown.isEmpty()) {
            Text(
                text = "لا توجد نسخ على الهاتف",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(shown) { e ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().let {
                            if (manage) it else it.clickable { onDismiss(); e.onRestore() }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (e.isDrive) Icons.Default.Cloud else Icons.Default.PhoneAndroid,
                                contentDescription = if (e.isDrive) "Drive" else "الهاتف",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(e.date, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(e.size, style = MaterialTheme.typography.labelSmall)
                            }
                            if (manage) {
                                e.onShare?.let {
                                    IconButton(onClick = it) { Icon(Icons.Default.Share, contentDescription = "مشاركة") }
                                }
                                IconButton(onClick = { onDismiss(); e.onDelete() }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

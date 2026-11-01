package com.example.features.settings

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.core.ui.MosaribActionConfirmDialog
import com.example.core.util.Formatters
import com.example.core.util.GoogleDriveBackupHelper
import com.example.features.customers.DuplicateCandidate
import com.example.features.customers.DuplicateReason
import com.example.ui.theme.AccentEmerald
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد حذف نقطة الأمان", fontWeight = FontWeight.Bold) },
        text = {
            Text("هل تريد حذف نقطة الأمان الوقائية ($checkpointTitle) نهائياً؟")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حذف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

// Delete confirmation for a local backup file stored on the phone
@Composable
private fun DeleteLocalBackupDialog(
    backupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد حذف النسخة", fontWeight = FontWeight.Bold) },
        text = {
            Text("هل تريد حذف ملف النسخة الاحتياطية ($backupName) نهائياً من الهاتف؟")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حذف", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

// Clean Old Backups Confirmation Dialog
@Composable
private fun CleanOldBackupsDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = "تنظيف النسخ القديمة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = "سيتم حذف النسخ الاحتياطية القديمة على الهاتف والإبقاء على أحدث 3 نسخ فقط لتوفير المساحة وتجنب تراكم الملفات.\n\nهل تريد المتابعة؟",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("تنظيف الآن", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

// Google Drive AppData Cloud Delete Dialog
@Composable
private fun DeleteDriveBackupDialog(
    backupName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حذف نسخة من Google Drive", fontWeight = FontWeight.Bold) },
        text = {
            Text("هل تريد حذف هذه النسخة السحابية ($backupName) نهائياً من سحابة Google Drive؟")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
            ) {
                Text("حذف من السحابة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
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
        MosaribActionConfirmDialog(
            title = "تأكيد حذف العمليات المكررة",
            message = "سيتم إنشاء نسخة احتياطية محلية أولاً، ثم حذف ${candidates.size} عملية مكررة. يمكنك الاستعادة من النسخة إن لزم.",
            confirmText = "حذف الآن",
            dismissText = "إلغاء",
            confirmColor = MaterialTheme.mosarib.debt,
            icon = Icons.Default.Delete,
            onConfirm = {
                showConfirmDelete = false
                onDeleteDuplicates(candidates.map { it.candidate })
            },
            onDismiss = { showConfirmDelete = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "فحص العمليات المستوردة المكررة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            if (candidates.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
        },
        confirmButton = {
            if (candidates.isNotEmpty()) {
                Button(
                    onClick = { showConfirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.debt),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "حذف المكرر (${candidates.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (candidates.isEmpty()) "إغلاق" else "إلغاء")
            }
        }
    )
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

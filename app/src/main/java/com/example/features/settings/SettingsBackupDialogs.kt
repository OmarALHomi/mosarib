package com.example.features.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.util.GoogleDriveBackupHelper
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
    onDeleteDriveBackup: (GoogleDriveBackupHelper.DriveBackupFile) -> Unit
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

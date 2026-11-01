package com.example.features.settings

import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.core.util.AutoBackupEngine
import com.example.core.util.BackupManager
import com.example.core.util.GoogleDriveBackupHelper

// UI-only state of the backup & restore section (tabs, expansion, pending confirmations).
// Owned and remembered by SettingsScreen; shared with the backup section and its dialogs.
@Stable
internal class SettingsBackupUiState {
    // Backup tab selector: 0 = Local (الهاتف), 1 = Cloud (Google Drive)
    var selectedBackupSourceTab by mutableStateOf(0)
    var expandAllLocalBackups by mutableStateOf(false)
    var expandAllDriveBackups by mutableStateOf(false)

    // Uri to restore with critical warning
    var uriToRestore by mutableStateOf<Uri?>(null)
    var backupToRestore by mutableStateOf<BackupManager.BackupFileInfo?>(null)
    var autoBackupToRestore by mutableStateOf<AutoBackupEngine.AutoBackupItem?>(null)
    var backupToDelete by mutableStateOf<BackupManager.BackupFileInfo?>(null)
    var autoBackupToDelete by mutableStateOf<AutoBackupEngine.AutoBackupItem?>(null)
    var driveBackupToRestore by mutableStateOf<GoogleDriveBackupHelper.DriveBackupFile?>(null)
    var driveBackupToDelete by mutableStateOf<GoogleDriveBackupHelper.DriveBackupFile?>(null)
    var showCleanOldBackupsConfirm by mutableStateOf(false)

    var showGoogleBackupAccountDialog by mutableStateOf(false)
    var pendingAutoBackupAfterSignIn by mutableStateOf(false)
}

package com.example.features.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.AutoBackupEngine
import com.example.core.util.BackupManager
import com.example.core.util.GoogleDriveBackupHelper
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsBackupScreen(
    onBack: () -> Unit,
    state: SettingsBackupUiState,
    isDark: Boolean,
    isBackupAllowed: Boolean,
    isDeviceBackupLocked: Boolean,
    isCloudBackupAllowed: Boolean,
    isAutoBackupEnabled: Boolean,
    autoBackups: List<AutoBackupEngine.AutoBackupItem>,
    savedBackups: List<BackupManager.BackupFileInfo>,
    googleAccount: GoogleSignInAccount?,
    driveBackups: List<GoogleDriveBackupHelper.DriveBackupFile>,
    isDriveLoading: Boolean,
    onCreateBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onShareNewBackup: () -> Unit,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    onRefreshBackups: () -> Unit,
    onShareBackupFile: (File) -> Unit,
    onGoogleSignOut: () -> Unit,
    onRefreshDriveBackups: () -> Unit,
    onCheckDuplicates: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "النسخ الاحتياطي والاستعادة",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // تنبيه في حال تم تعطيل النسخ الاحتياطي
            if (!isBackupAllowed) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        BackupDisabledBanner(isDark = isDark, isDeviceBackupLocked = isDeviceBackupLocked)
                    }
                }
            }

            // -----------------------------------------------------------------
            // 1. إجراءات النسخ السريع
            // -----------------------------------------------------------------
            item {
                SettingsSectionHeader(title = "إجراءات النسخ السريع")
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onCreateBackup,
                                enabled = isBackupAllowed,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخة جديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onImportBackup,
                                enabled = isBackupAllowed,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentGold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد ملف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onShareNewBackup,
                                enabled = isBackupAllowed,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مشاركة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onCheckDuplicates,
                            enabled = isBackupAllowed,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص العمليات المستوردة المكررة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. النسخ الاحتياطي المحلي (على الهاتف)
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "النسخ الاحتياطي المحلي (على الهاتف)")
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        LocalBackupsTab(
                            isDark = isDark,
                            isAutoBackupEnabled = isAutoBackupEnabled,
                            onAutoBackupEnabledChange = onAutoBackupEnabledChange,
                            autoBackups = autoBackups,
                            savedBackups = savedBackups,
                            expandAllLocalBackups = state.expandAllLocalBackups,
                            onToggleExpandAllLocalBackups = { state.expandAllLocalBackups = !state.expandAllLocalBackups },
                            onRestoreAutoBackup = { state.autoBackupToRestore = it },
                            onDeleteAutoBackup = { state.autoBackupToDelete = it },
                            onCleanOldBackups = { state.showCleanOldBackupsConfirm = true },
                            onRefreshBackups = onRefreshBackups,
                            onRestoreBackup = { state.backupToRestore = it },
                            onShareBackup = { onShareBackupFile(it.file) },
                            onDeleteBackup = { state.backupToDelete = it }
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 3. سحابة Google Drive
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "سحابة Google Drive")
                SettingsCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        DriveBackupsTab(
                            isDark = isDark,
                            isCloudBackupAllowed = isCloudBackupAllowed,
                            googleAccount = googleAccount,
                            driveBackups = driveBackups,
                            isDriveLoading = isDriveLoading,
                            expandAllDriveBackups = state.expandAllDriveBackups,
                            onToggleExpandAllDriveBackups = { state.expandAllDriveBackups = !state.expandAllDriveBackups },
                            onConnectGoogleAccount = {
                                state.pendingAutoBackupAfterSignIn = false
                                state.showGoogleBackupAccountDialog = true
                            },
                            onUploadBackupNow = {
                                state.pendingAutoBackupAfterSignIn = true
                                state.showGoogleBackupAccountDialog = true
                            },
                            onSignOut = onGoogleSignOut,
                            onRefreshDriveBackups = onRefreshDriveBackups,
                            onRestoreDriveBackup = { state.driveBackupToRestore = it },
                            onDeleteDriveBackup = { state.driveBackupToDelete = it }
                        )
                    }
                }
            }
        }
    }
}

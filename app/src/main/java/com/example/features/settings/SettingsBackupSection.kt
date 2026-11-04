package com.example.features.settings

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

// Section 4: Real Database Backup & Storage (Phone + Google Drive)
@Composable
internal fun BackupAndRestoreSection(
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
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(googleAccount) {
        if (googleAccount != null) {
            onRefreshDriveBackups()
        }
    }
    BackupSectionCard(
        isDark = isDark,
        isBackupAllowed = isBackupAllowed,
        isDeviceBackupLocked = isDeviceBackupLocked,
        localBackupsCount = savedBackups.size,
        driveBackupsCount = driveBackups.size,
        selectedBackupSourceTab = state.selectedBackupSourceTab,
        onSelectBackupSourceTab = { state.selectedBackupSourceTab = it },
        onCreateBackup = onCreateBackup,
        onImportBackup = onImportBackup,
        onShareNewBackup = onShareNewBackup,
        onCheckDuplicates = { viewModel.scanImportDuplicates() },
        localTabContent = {
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
        },
        driveTabContent = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DriveAutoBackupSection(
                    context = context,
                    isDark = isDark,
                    isCloudBackupAllowed = isCloudBackupAllowed,
                    googleAccount = googleAccount,
                    onConnectGoogleAccount = {
                        state.pendingAutoBackupAfterSignIn = false
                        state.showGoogleBackupAccountDialog = true
                    },
                    onRefreshDriveBackups = onRefreshDriveBackups
                )
                val recordedDriveTime = com.example.core.backup.DriveBackupPreferences.getLastBackupTime(context)
                val driveListError = com.example.core.backup.DriveBackupPreferences.getListError(context) ?: com.example.core.backup.DriveBackupPreferences.getLastError(context)
                if (driveBackups.isEmpty() && recordedDriveTime > 0L && !driveListError.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF3B1E1E) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تعذر جلب قائمة النسخ السحابية: $driveListError",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
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
    )
}

@Composable
private fun BackupSectionCard(
    isDark: Boolean,
    isBackupAllowed: Boolean,
    isDeviceBackupLocked: Boolean,
    localBackupsCount: Int,
    driveBackupsCount: Int,
    selectedBackupSourceTab: Int,
    onSelectBackupSourceTab: (Int) -> Unit,
    onCreateBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onShareNewBackup: () -> Unit,
    onCheckDuplicates: () -> Unit = {},
    localTabContent: @Composable () -> Unit,
    driveTabContent: @Composable () -> Unit
) {
    SettingsSectionCard {
        // 1. Header (بدون أزرار متداخلة)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    "النسخ الاحتياطي والأمان",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    "حفظ محلي وسحابي محمي لاسترجاع الحسابات",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!isBackupAllowed) {
            BackupDisabledBanner(isDark = isDark, isDeviceBackupLocked = isDeviceBackupLocked)
        }

        // 2. Action Buttons (متناسقة بعرض موحد وبدون ضغط)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onCreateBackup,
                enabled = isBackupAllowed,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 9.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("نسخة جديدة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onImportBackup,
                enabled = isBackupAllowed,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 9.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentGold)
                Spacer(modifier = Modifier.width(4.dp))
                Text("استيراد ملف", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onShareNewBackup,
                enabled = isBackupAllowed,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 9.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrimaryTeal)
                Spacer(modifier = Modifier.width(4.dp))
                Text("مشاركة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onCheckDuplicates,
            enabled = isBackupAllowed,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 9.dp)
        ) {
            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrimaryTeal)
            Spacer(modifier = Modifier.width(6.dp))
            Text("فحص العمليات المستوردة المكررة", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Tab Switcher (على الهاتف vs سحابة درايف) لمنع تمدد الشاشة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BackupSourceTab(
                isSelected = selectedBackupSourceTab == 0,
                onClick = { onSelectBackupSourceTab(0) },
                icon = Icons.Default.Backup,
                iconTint = PrimaryTeal,
                label = "على الهاتف ($localBackupsCount)",
                selectedLabelColor = PrimaryTeal,
                modifier = Modifier.weight(1f)
            )

            BackupSourceTab(
                isSelected = selectedBackupSourceTab == 1,
                onClick = { onSelectBackupSourceTab(1) },
                icon = Icons.Default.CloudDone,
                iconTint = Color(0xFF16A34A),
                label = "سحابة Drive ($driveBackupsCount)",
                selectedLabelColor = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. المحتوى بحسب التبويب المختار
        if (selectedBackupSourceTab == 0) {
            localTabContent()
        } else {
            driveTabContent()
        }
    }
}

@Composable
internal fun BackupDisabledBanner(isDark: Boolean, isDeviceBackupLocked: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF3B1E1E) else Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isDeviceBackupLocked) "تم إيقاف ميزة النسخ الاحتياطي لهذا الجهاز من قِبل إدارة التطبيق."
                       else "تم إيقاف ميزة النسخ الاحتياطي للنسخ التجريبية من قِبل إدارة التطبيق.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                )
            )
        }
    }
}

@Composable
private fun BackupSourceTab(
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    selectedLabelColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = if (isSelected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) selectedLabelColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

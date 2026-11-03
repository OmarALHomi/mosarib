package com.example.features.settings

import android.content.Context
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.license.LicenseManager
import com.example.core.license.UserDeviceTracker
import com.example.core.ui.GoogleBackupAccountDialog
import com.example.core.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

// Content of the Google Drive backup tab
@Composable
internal fun DriveBackupsTab(
    isDark: Boolean,
    isCloudBackupAllowed: Boolean,
    googleAccount: GoogleSignInAccount?,
    driveBackups: List<GoogleDriveBackupHelper.DriveBackupFile>,
    isDriveLoading: Boolean,
    expandAllDriveBackups: Boolean,
    onToggleExpandAllDriveBackups: () -> Unit,
    onConnectGoogleAccount: () -> Unit,
    onUploadBackupNow: () -> Unit,
    onSignOut: () -> Unit,
    onRefreshDriveBackups: () -> Unit,
    onRestoreDriveBackup: (GoogleDriveBackupHelper.DriveBackupFile) -> Unit,
    onDeleteDriveBackup: (GoogleDriveBackupHelper.DriveBackupFile) -> Unit
) {
    // ─── محتوى سحابة Google Drive ───
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!isCloudBackupAllowed) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF3B1E1E) else Color(0xFFFEF2F2),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تم تعطيل ميزة النسخ السحابي عبر Drive من قِبل إدارة التطبيق.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                    )
                )
            }
        }
    }

    if (googleAccount == null) {
        DriveConnectPrompt(
            isDark = isDark,
            isCloudBackupAllowed = isCloudBackupAllowed,
            onConnectGoogleAccount = onConnectGoogleAccount
        )
    } else {
        // متصل بحساب Google
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDark) Color(0xFF14241B) else Color(0xFFF0FDF4))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DriveAccountHeader(isDark = isDark, email = googleAccount.email, onSignOut = onSignOut)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onUploadBackupNow,
                        enabled = !isDriveLoading && isCloudBackupAllowed,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفع نسخة سحابية الآن", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onRefreshDriveBackups,
                        enabled = !isDriveLoading,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "تحديث", modifier = Modifier.size(15.dp), tint = Color(0xFF16A34A))
                    }
                }

                if (isDriveLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF16A34A))
                }

                if (driveBackups.isEmpty() && !isDriveLoading) {
                    Text(
                        text = "لا توجد نسخ سحابية بعد. اضغط على 'رفع نسخة سحابية الآن' لحفظ بياناتك.",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 11.sp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    val displayedDrive = if (expandAllDriveBackups) driveBackups else driveBackups.take(3)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        displayedDrive.forEach { cloudBackup ->
                            DriveBackupRow(
                                cloudBackup = cloudBackup,
                                isDark = isDark,
                                onRestore = { onRestoreDriveBackup(cloudBackup) },
                                onDelete = { onDeleteDriveBackup(cloudBackup) }
                            )
                        }

                        if (driveBackups.size > 3) {
                            TextButton(
                                onClick = onToggleExpandAllDriveBackups,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 2.dp)
                            ) {
                                Icon(
                                    if (expandAllDriveBackups) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF16A34A)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (expandAllDriveBackups) "عرض أحدث 3 نسخ فقط ▲" else "عرض كافة النسخ في Drive (${driveBackups.size}) ▼",
                                    fontSize = 11.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun DriveConnectPrompt(
    isDark: Boolean,
    isCloudBackupAllowed: Boolean,
    onConnectGoogleAccount: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0xFF14241B) else Color(0xFFF0FDF4))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "سحابة Google Drive (المجلد المحمي)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                )
            )
            Text(
                text = "احفظ نسخ بياناتك تلقائياً في سحابة خاصة لا يمكن حذفها أو العبث بها بالخطأ وتستعيدها في أي وقت.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                    fontSize = 11.sp
                )
            )
            Button(
                onClick = onConnectGoogleAccount,
                enabled = isCloudBackupAllowed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ربط بحساب Google لحفظ النسخ سحابياً", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DriveAccountHeader(
    isDark: Boolean,
    email: String?,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF1E3A2B) else Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = email ?: "حساب Google",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF14532D)
                    ),
                    maxLines = 1
                )
                Text(
                    text = "المجلد السحابي متصل بأمان",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                        fontSize = 10.sp
                    )
                )
            }
        }

        TextButton(
            onClick = onSignOut,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("قطع الاتصال", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun DriveBackupRow(
    cloudBackup: GoogleDriveBackupHelper.DriveBackupFile,
    isDark: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color.White)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cloudBackup.formattedDate,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = "${cloudBackup.sizeText} • سحابي",
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp),
                maxLines = 1
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

// Google Drive Backup Account Dialog, pre-filled with the best known distributor identity
@Composable
internal fun DriveBackupAccountDialog(
    context: Context,
    distributorName: String,
    distributorPhone: String,
    googleAccount: GoogleSignInAccount?,
    onDismiss: () -> Unit,
    onProceed: (name: String, phone: String, shouldLaunchSignIn: Boolean) -> Unit
) {
    val initialDialogName = distributorName.takeIf { it.isNotBlank() && it != "موزع الماء / المسرب" }
        ?: UserDeviceTracker.getRegisteredName(context).takeIf { it.isNotBlank() }
        ?: LicenseManager.getActivationRecord(context)?.subscriberName
        ?: ""

    val initialDialogPhone = distributorPhone.takeIf { it.isNotBlank() && it != "777000000" }
        ?: UserDeviceTracker.getRegisteredPhone(context).takeIf { it.isNotBlank() }
        ?: ""

    GoogleBackupAccountDialog(
        initialName = initialDialogName,
        initialPhone = initialDialogPhone,
        currentGoogleAccount = googleAccount,
        onDismiss = onDismiss,
        onProceed = onProceed
    )
}

package com.example.features.settings

import android.content.Context
import android.widget.Toast
import com.example.core.database.AppDatabase
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.core.backup.BackupFrequency
import com.example.core.backup.DriveBackupPreferences
import com.example.core.backup.DriveBackupScheduler
import com.example.core.util.AutoBackupEngine
import com.example.core.util.BackupManager
import com.example.core.util.Formatters
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAdvanced by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf<BackupListMode?>(null) }

    LaunchedEffect(googleAccount) {
        if (googleAccount != null) {
            onRefreshDriveBackups()
        }
    }

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
        if (!isBackupAllowed && !isDeviceBackupLocked) {
            val openSubscription = com.example.core.ui.LocalOpenSubscription.current
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SettingsCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "النسخ الاحتياطي متاح للمشتركين — بياناتك محفوظة على هذا الهاتف",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Button(
                            onClick = openSubscription,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("اشترك", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            return@Scaffold
        }
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

            // 1. بطاقة الحالة + انسخ الآن
            item {
                val workInfos by WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWorkFlow(DriveBackupScheduler.IMMEDIATE_WORK_NAME)
                    .collectAsState(initial = emptyList())
                val driveTime = remember(workInfos, googleAccount) { DriveBackupPreferences.getLastBackupTime(context) }
                val localTime = maxOf(
                    savedBackups.maxOfOrNull { it.file.lastModified() } ?: 0L,
                    autoBackups.maxOfOrNull { it.file.lastModified() } ?: 0L
                )
                SettingsSectionHeader(title = "حالة النسخ الاحتياطي")
                SettingsCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BackupStatusLine("آخر نسخة على الهاتف", localTime, "لا توجد نسخة بعد")
                        BackupStatusLine(
                            "آخر نسخة على Drive",
                            if (googleAccount != null) driveTime else 0L,
                            if (googleAccount != null) "لم تتم أي نسخة بعد" else "غير مفعّل"
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    val file = AutoBackupEngine.createSafetyCheckpoint(
                                        context, AppDatabase.getDatabase(context), "manual"
                                    )
                                    onRefreshBackups()
                                    if (file == null) {
                                        Toast.makeText(context, "تعذّر حفظ النسخة", Toast.LENGTH_SHORT).show()
                                    } else {
                                        if (googleAccount != null && isCloudBackupAllowed) {
                                            DriveBackupScheduler.enqueueImmediateBackup(context)
                                        }
                                        Toast.makeText(context, "تم حفظ النسخة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = isBackupAllowed,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("انسخ الآن", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. النسخ التلقائي
            item {
                var frequency by remember { mutableStateOf(DriveBackupPreferences.getFrequency(context)) }
                var wifiOnly by remember { mutableStateOf(DriveBackupPreferences.isWifiOnly(context)) }
                var showOther by remember { mutableStateOf(false) }
                val lastError = DriveBackupPreferences.getLastError(context)
                val driveListError = DriveBackupPreferences.getListError(context)
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "النسخ التلقائي")
                SettingsCard {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("نسخ تلقائي", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Switch(
                                checked = isAutoBackupEnabled,
                                onCheckedChange = { on ->
                                    onAutoBackupEnabledChange(on)
                                    if (googleAccount != null) {
                                        frequency = if (!on) BackupFrequency.OFF
                                        else if (frequency == BackupFrequency.OFF) BackupFrequency.DAILY else frequency
                                        DriveBackupPreferences.setFrequency(context, frequency)
                                        DriveBackupScheduler.schedulePeriodicWork(context)
                                    }
                                }
                            )
                        }
                        if (isAutoBackupEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for ((freq, label) in listOfNotNull(BackupFrequency.DAILY to "يومي", (BackupFrequency.THREE_DAYS to "كل 3 أيام").takeIf { frequency == BackupFrequency.THREE_DAYS }, BackupFrequency.WEEKLY to "أسبوعي")) {
                                    FilterChip(
                                        selected = frequency == freq,
                                        onClick = {
                                            frequency = freq
                                            DriveBackupPreferences.setFrequency(context, freq)
                                            DriveBackupScheduler.schedulePeriodicWork(context)
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            if (googleAccount == null) {
                                OutlinedButton(
                                    onClick = {
                                        state.pendingAutoBackupAfterSignIn = false
                                        state.showGoogleBackupAccountDialog = true
                                    },
                                    enabled = isCloudBackupAllowed,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ربط بحساب Google", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "مرتبط بـ ${googleAccount.email ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onGoogleSignOut) { Text("فصل الحساب") }
                                }
                            }
                        }
                        TextButton(onClick = { showOther = !showOther }) {
                            Text("إعدادات أخرى ${if (showOther) "▲" else "▼"}", fontSize = 12.sp)
                        }
                        if (showOther) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("على الواي فاي فقط", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = wifiOnly,
                                    onCheckedChange = {
                                        wifiOnly = it
                                        DriveBackupPreferences.setWifiOnly(context, it)
                                        DriveBackupScheduler.schedulePeriodicWork(context)
                                    }
                                )
                            }
                            listOfNotNull(lastError, driveListError).filter { it.isNotBlank() }.forEach {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (it.contains("NetworkError", true)) "تعذر الاتصال بالإنترنت" else it, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626)))
                                }
                            }
                        }
                    }
                }
            }

            // 3. استعادة
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "استعادة")
                SettingsCard {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                onRefreshBackups()
                                if (googleAccount != null) onRefreshDriveBackups()
                                showList = BackupListMode.RESTORE
                            },
                            enabled = isBackupAllowed,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة نسخة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. متقدم (مطوي)
            item {
                Spacer(modifier = Modifier.height(14.dp))
                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) { Text("متقدم ${if (showAdvanced) "▲" else "▼"}", fontWeight = FontWeight.Bold) }
                if (showAdvanced) {
                    SettingsCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdvancedButton("حفظ نسخة كملف", Icons.Default.Folder, isBackupAllowed, onCreateBackup)
                            AdvancedButton("استيراد ملف", Icons.Default.Folder, isBackupAllowed, onImportBackup)
                            AdvancedButton("مشاركة ملف النسخة", Icons.Default.Share, isBackupAllowed, onShareNewBackup)
                            AdvancedButton("تنظيف النسخ القديمة", Icons.Default.CleaningServices, isBackupAllowed) {
                                state.showCleanOldBackupsConfirm = true
                            }
                            AdvancedButton("فحص العمليات المكررة", Icons.Default.CleaningServices, isBackupAllowed, onCheckDuplicates)
                            AdvancedButton("حذف نسخ", Icons.Default.Delete, isBackupAllowed) {
                                onRefreshBackups()
                                if (googleAccount != null) onRefreshDriveBackups()
                                showList = BackupListMode.MANAGE
                            }
                        }
                    }
                }
            }
        }
    }

    showList?.let { mode ->
        BackupListDialog(
            manage = mode == BackupListMode.MANAGE,
            entries = buildBackupEntries(state, autoBackups, savedBackups, driveBackups, onShareBackupFile),
            isLoading = isDriveLoading,
            hasAccount = googleAccount != null,
            onRetryDrive = onRefreshDriveBackups,
            onDismiss = { showList = null }
        )
    }
}

@Composable
private fun BackupStatusLine(label: String, time: Long, emptyText: String) {
    val days = if (time > 0L) (System.currentTimeMillis() - time) / 86_400_000L else -1L
    val color = when {
        time <= 0L -> MaterialTheme.colorScheme.onSurfaceVariant
        days <= 0L -> Color(0xFF16A34A)
        days > 7L -> Color(0xFFD97706)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val text = when {
        time <= 0L -> emptyText
        days <= 0L -> "اليوم"
        days == 1L -> "أمس"
        else -> "قبل $days يوم"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (time > 0L && days <= 0L) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text("$label: $text", style = MaterialTheme.typography.bodyMedium.copy(color = color, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun AdvancedButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// Local (manual + safety) and Drive copies merged, newest first
private fun buildBackupEntries(
    state: SettingsBackupUiState,
    autoBackups: List<AutoBackupEngine.AutoBackupItem>,
    savedBackups: List<BackupManager.BackupFileInfo>,
    driveBackups: List<GoogleDriveBackupHelper.DriveBackupFile>,
    onShare: (File) -> Unit
): List<BackupListEntry> {
    val iso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    return (savedBackups.map {
        BackupListEntry(false, it.file.lastModified(), it.formattedDate, it.sizeText,
            { state.backupToRestore = it }, { onShare(it.file) }, { state.backupToDelete = it })
    } + autoBackups.map {
        BackupListEntry(false, it.file.lastModified(), it.formattedDate, it.sizeText,
            { state.autoBackupToRestore = it }, { onShare(it.file) }, { state.autoBackupToDelete = it })
    } + driveBackups.map {
        val t = runCatching { iso.parse(it.createdTime.take(19))?.time }.getOrNull() ?: 0L
        BackupListEntry(true, t, it.formattedDate, it.sizeText,
            { state.driveBackupToRestore = it }, null, { state.driveBackupToDelete = it })
    }).sortedByDescending { it.time }
}

/**
 * Reusable section for automatic periodic backup to Google Drive.
 */
@Composable
internal fun DriveAutoBackupSection(
    context: Context,
    isDark: Boolean,
    isCloudBackupAllowed: Boolean,
    googleAccount: GoogleSignInAccount?,
    onConnectGoogleAccount: () -> Unit,
    onRefreshDriveBackups: () -> Unit = {}
) {
    if (googleAccount == null) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF14241B) else Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF1E3A2B) else Color(0xFFDCFCE7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "النسخ التلقائي إلى Google Drive",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534)
                        )
                    )
                }
                Text(
                    text = "اربط حساب Google لتفعيل النسخ إلى Drive",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
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
                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ربط بحساب Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        var frequency by remember { mutableStateOf(DriveBackupPreferences.getFrequency(context)) }
        var wifiOnly by remember { mutableStateOf(DriveBackupPreferences.isWifiOnly(context)) }
        var lastBackupTime by remember { mutableStateOf(DriveBackupPreferences.getLastBackupTime(context)) }
        var lastError by remember { mutableStateOf(DriveBackupPreferences.getLastError(context)) }

        val workInfos by WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(DriveBackupScheduler.IMMEDIATE_WORK_NAME)
            .collectAsState(initial = emptyList())

        val periodicWorkInfos by WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(DriveBackupScheduler.PERIODIC_WORK_NAME)
            .collectAsState(initial = emptyList())

        val isImmediateRunning = workInfos.any {
            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        }
        val isPeriodicRunning = periodicWorkInfos.any {
            it.state == WorkInfo.State.RUNNING
        }
        val isRunning = isImmediateRunning || isPeriodicRunning

        var prevImmediateStates by remember { mutableStateOf<Map<java.util.UUID, WorkInfo.State>>(emptyMap()) }
        var prevPeriodicStates by remember { mutableStateOf<Map<java.util.UUID, WorkInfo.State>>(emptyMap()) }

        LaunchedEffect(workInfos, periodicWorkInfos) {
            val prevTime = lastBackupTime
            val newTime = DriveBackupPreferences.getLastBackupTime(context)
            lastBackupTime = newTime
            lastError = DriveBackupPreferences.getLastError(context)

            val immediateMovedToSucceeded = workInfos.any { info ->
                info.state == WorkInfo.State.SUCCEEDED &&
                    prevImmediateStates[info.id]?.let { it == WorkInfo.State.RUNNING || it == WorkInfo.State.ENQUEUED } == true
            }
            val periodicMovedToSucceeded = periodicWorkInfos.any { info ->
                info.state == WorkInfo.State.SUCCEEDED &&
                    prevPeriodicStates[info.id]?.let { it == WorkInfo.State.RUNNING || it == WorkInfo.State.ENQUEUED } == true
            }

            prevImmediateStates = workInfos.associate { it.id to it.state }
            prevPeriodicStates = periodicWorkInfos.associate { it.id to it.state }

            if (immediateMovedToSucceeded || periodicMovedToSucceeded || (newTime > 0L && newTime != prevTime)) {
                onRefreshDriveBackups()
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // تكرار النسخ
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "تكرار النسخ الاحتياطي:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (freq in BackupFrequency.entries) {
                        val isSelected = (frequency == freq)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF16A34A) else if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF16A34A) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    frequency = freq
                                    DriveBackupPreferences.setFrequency(context, freq)
                                    DriveBackupScheduler.schedulePeriodicWork(context)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = freq.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // خيار الواي فاي فقط
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "على الواي فاي فقط",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "إيقاف الرفع عبر باقة الهاتف لتوفير البيانات",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = { checked ->
                        wifiOnly = checked
                        DriveBackupPreferences.setWifiOnly(context, checked)
                        DriveBackupScheduler.schedulePeriodicWork(context)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF16A34A)
                    )
                )
            }

            // سطر الحالة
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                val dateStatus = if (lastBackupTime > 0L) {
                    "آخر نسخة إلى Drive: ${Formatters.formatDateTime(lastBackupTime)}"
                } else {
                    "آخر نسخة إلى Drive: لم تتم أي نسخة بعد"
                }
                Text(
                    text = dateStatus,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )

                if (!lastError.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "آخر تنبيه: $lastError",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.5.sp
                            )
                        )
                    }
                }
            }

            // زر انسخ الآن مع مؤشر التقدم
            Button(
                onClick = {
                    DriveBackupScheduler.enqueueImmediateBackup(context)
                },
                enabled = !isRunning && isCloudBackupAllowed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جارٍ النسخ إلى Google Drive...", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("انسخ الآن", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

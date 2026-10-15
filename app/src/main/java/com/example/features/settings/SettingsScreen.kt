package com.example.features.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AccountCircle
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.security.BiometricHelper
import com.example.core.ui.ToastType
import com.example.core.util.AutoBackupEngine
import com.example.core.util.BackupManager
import com.example.core.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.Formatters
import com.example.features.pumps.PumpSource
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.features.feedback.FeedbackDialog
import com.example.ui.theme.SecondaryAqua

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val isActivated by viewModel.isActivated.collectAsStateWithLifecycle()
    val isSeedingData by viewModel.isSeedingData.collectAsStateWithLifecycle()

    var showConfirmSeedDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    var distributorName by remember(config.distributorName) { mutableStateOf(config.distributorName) }
    var distributorPhone by remember(config.distributorPhone) { mutableStateOf(config.distributorPhone) }
    var defaultPriceStr by remember(config.defaultPricePerHour) {
        mutableStateOf(Formatters.formatAmountInput(config.defaultPricePerHour.toString()))
    }
    var currencySymbol by remember(config.currencySymbol) { mutableStateOf(config.currencySymbol) }

    // Uri to restore with critical warning
    var uriToRestore by remember { mutableStateOf<Uri?>(null) }
    var showTourDialog by remember { mutableStateOf(false) }

    // File Picker Launcher for JSON Database Restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { uriToRestore = it }
    }

    // SAF Document Creator for saving backup anywhere on device
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackupToUri(it) }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = com.example.core.ui.isAppDark()
    val isBiometricAvailable = remember { BiometricHelper.isAvailable(context) }
    val deviceCode = remember { LicenseManager.getDeviceCode(context) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    val savedBackups by viewModel.savedBackups.collectAsStateWithLifecycle()
    val autoBackups by viewModel.autoBackups.collectAsStateWithLifecycle()
    var backupToRestore by remember { mutableStateOf<BackupManager.BackupFileInfo?>(null) }
    var autoBackupToRestore by remember { mutableStateOf<AutoBackupEngine.AutoBackupItem?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupManager.BackupFileInfo?>(null) }

    val googleAccount by viewModel.googleAccount.collectAsStateWithLifecycle()
    val driveBackups by viewModel.driveBackups.collectAsStateWithLifecycle()
    val isDriveLoading by viewModel.isDriveLoading.collectAsStateWithLifecycle()

    var driveBackupToRestore by remember { mutableStateOf<GoogleDriveBackupHelper.DriveBackupFile?>(null) }
    var driveBackupToDelete by remember { mutableStateOf<GoogleDriveBackupHelper.DriveBackupFile?>(null) }
    var showCleanOldBackupsConfirm by remember { mutableStateOf(false) }

    // Backup tab selector: 0 = Local (الهاتف), 1 = Cloud (Google Drive)
    val targetBackupSourceTab by viewModel.targetBackupSourceTab.collectAsStateWithLifecycle()
    var selectedBackupSourceTab by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(targetBackupSourceTab) {
        selectedBackupSourceTab = targetBackupSourceTab
    }
    var expandAllLocalBackups by remember { mutableStateOf(false) }
    var expandAllDriveBackups by remember { mutableStateOf(false) }

    var showGoogleBackupAccountDialog by remember { mutableStateOf(false) }
    var pendingAutoBackupAfterSignIn by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.onGoogleSignInSuccess(account)
                if (pendingAutoBackupAfterSignIn) {
                    pendingAutoBackupAfterSignIn = false
                    viewModel.backupToGoogleDriveAppData()
                }
            }
        } catch (e: Exception) {
            pendingAutoBackupAfterSignIn = false
            viewModel.showToast("فشل تسجيل الدخول بحساب Google: ${com.example.core.util.GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
        }
    }

    val driveRecoveryIntent by viewModel.driveRecoveryIntent.collectAsStateWithLifecycle()
    val driveRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDriveConsentGranted()
        } else {
            viewModel.clearDriveRecoveryIntent()
        }
    }

    androidx.compose.runtime.LaunchedEffect(driveRecoveryIntent) {
        driveRecoveryIntent?.let {
            driveRecoveryLauncher.launch(it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {



        // Section 1: Water Price Setting & Historical Guarantee
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("سعر ساعة الماء", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = defaultPriceStr,
                        onValueChange = { defaultPriceStr = Formatters.formatAmountInput(it) },
                        label = { Text("سعر الساعة ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("default_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val parsedDefPrice = Formatters.parseAmountInput(defaultPriceStr)
                    if (parsedDefPrice > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.amountToArabicWords(parsedDefPrice, currencySymbol),
                            style = MaterialTheme.typography.bodySmall.copy(color = PrimaryTeal, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Notice about historical pricing preservation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryTeal.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "يطبق على الجلسات الجديدة فقط.",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val p = if (parsedDefPrice > 0) parsedDefPrice else 5000.0
                            viewModel.updateDefaultPricePerHour(p)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("حفظ السعر الجديد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 2: Distributor Profile for Invoices
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("بيانات الموزع", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = distributorName,
                        onValueChange = { distributorName = it },
                        label = { Text("اسم الموزع أو البئر") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = distributorPhone,
                        onValueChange = { distributorPhone = it },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = {
                            currencySymbol = it
                            viewModel.updateCurrencySymbol(it)
                        },
                        label = { Text("رمز العملة") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.updateDistributorInfo(distributorName, distributorPhone)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("حفظ بيانات الموزع", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 3: Theme Mode (Day / Night / System)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(SecondaryAqua.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = SecondaryAqua, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("مظهر التطبيق", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            Triple("LIGHT", "نهاري", Icons.Default.LightMode),
                            Triple("DARK", "ليلي", Icons.Default.DarkMode),
                            Triple("SYSTEM", "تلقائي", Icons.Default.Settings)
                        ).forEach { (mode, label, icon) ->
                            val isSelected = config.themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { viewModel.updateThemeMode(mode) },
                                color = if (isSelected) PrimaryTeal else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 9.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3.5: Security & Biometric Lock (البصمة والحماية)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "قفل التطبيق بالبصمة",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isBiometricAvailable) "طلب بصمة الإصبع عند فتح التطبيق لحماية الحسابات"
                                    else "تأكد من تسجيل بصمة في إعدادات الهاتف واستخدامها لحماية حساباتك",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        Switch(
                            checked = config.biometricEnabled,
                            onCheckedChange = { viewModel.updateBiometricEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryTeal
                            )
                        )
                    }
                }
            }
        }

        // Section 4: Real Database Backup & Storage (Phone + Google Drive)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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

                    val isBackupAllowed = remember { com.example.core.license.RemoteConfigManager.isBackupAllowed(context) }
                    val isCloudBackupAllowed = remember { com.example.core.license.RemoteConfigManager.isCloudBackupAllowed(context) }

                    if (!isBackupAllowed) {
                        androidx.compose.material3.Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF3B1E1E) else Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم إيقاف ميزة النسخ الاحتياطي للنسخ التجريبية من قِبل إدارة التطبيق.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                    )
                                )
                            }
                        }
                    }

                    // 2. Action Buttons (متناسقة بعرض موحد وبدون ضغط)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = {
                                val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                createBackupLauncher.launch("mosarib_backup_$dateStr.back")
                            },
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
                            onClick = { restoreFileLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json")) },
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
                            onClick = { viewModel.createBackupAndShare() },
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
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedBackupSourceTab = 0 },
                            color = if (selectedBackupSourceTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = if (selectedBackupSourceTab == 0) 1.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "على الهاتف (${savedBackups.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedBackupSourceTab == 0) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedBackupSourceTab = 1 },
                            color = if (selectedBackupSourceTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shape = RoundedCornerShape(8.dp),
                            shadowElevation = if (selectedBackupSourceTab == 1) 1.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "سحابة Drive (${driveBackups.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedBackupSourceTab == 1) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. المحتوى بحسب التبويب المختار
                    if (selectedBackupSourceTab == 0) {
                        // ─── قسم النسخ التلقائي الدوّار (Rolling 7-Days) ───
                        if (autoBackups.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "النسخ التلقائية الذكية (آخر 7 أيام):",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AccentEmerald)
                                        )
                                    }
                                }

                                val displayedAuto = if (expandAllLocalBackups) autoBackups else autoBackups.take(2)
                                displayedAuto.forEach { autoItem ->
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

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isDark) Color(0xFF2B2111) else Color(0xFFFEF3C7))
                                                .clickable { autoBackupToRestore = autoItem }
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

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // ─── محتوى النسخ المحفوظة على الهاتف ───
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أحدث النسخ المحلية المحفوظة:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (savedBackups.size > 3) {
                                    TextButton(
                                        onClick = { showCleanOldBackupsConfirm = true },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("تنظيف القديمة", fontSize = 10.5.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    }
                                }
                                IconButton(onClick = { viewModel.loadBackups() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Restore, contentDescription = "تحديث", modifier = Modifier.size(15.dp), tint = PrimaryTeal)
                                }
                            }
                        }

                        if (savedBackups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "لا توجد نسخ محلية محفوظة بعد. اضغط على 'نسخة جديدة' لإنشاء نسخة احتياطية.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            val displayedBackups = if (expandAllLocalBackups) savedBackups else savedBackups.take(3)
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                displayedBackups.forEach { backup ->
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
                                                    .clickable { backupToRestore = backup }
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
                                                    .clickable { viewModel.shareExistingBackup(backup.file) },
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
                                                    .clickable { backupToDelete = backup },
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

                                if (savedBackups.size > 3) {
                                    TextButton(
                                        onClick = { expandAllLocalBackups = !expandAllLocalBackups },
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
                        }
                    } else {
                        // ─── محتوى سحابة Google Drive ───
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

                        val acc = googleAccount
                        if (acc == null) {
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
                                        onClick = {
                                            pendingAutoBackupAfterSignIn = false
                                            showGoogleBackupAccountDialog = true
                                        },
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
                                                    text = acc.email ?: "حساب Google",
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
                                            onClick = { viewModel.onGoogleSignOut() },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("قطع الاتصال", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.Bold))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                pendingAutoBackupAfterSignIn = true
                                                showGoogleBackupAccountDialog = true
                                            },
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
                                            onClick = { viewModel.loadDriveBackups() },
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
                                                                .clickable { driveBackupToRestore = cloudBackup }
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
                                                                .clickable { driveBackupToDelete = cloudBackup },
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

                                            if (driveBackups.size > 3) {
                                                TextButton(
                                                    onClick = { expandAllDriveBackups = !expandAllDriveBackups },
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
            }
        }

        // Section: الترخيص والتفعيل (200 عملية مجانية / تفعيل دائم)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isActivated) AccentEmerald.copy(alpha = 0.15f) else AccentGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isActivated) Icons.Default.Verified else Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (isActivated) AccentEmerald else AccentGold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "الترخيص والتفعيل",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                val headerLimit = LicenseManager.getFreeOperationsLimit(context)
                                Text(
                                    text = if (isActivated) "نسخة مفعلة بصفة دائمة" else "النسخة التجريبية ($headerLimit عملية)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isActivated) AccentEmerald else AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isActivated) AccentEmerald.copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isActivated) "مفعّل" else "تجريبي",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isActivated) AccentEmerald else Color(0xFFFF9800),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Device Code row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "رمز الجهاز الفريد (Device Token):",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                            )
                            Text(
                                text = deviceCode,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }

                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("كود جهاز المسرب", deviceCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ كود الجهاز إلى الحافظة", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isActivated) {
                        val freeLimit = LicenseManager.getFreeOperationsLimit(context)
                        val remaining = LicenseManager.getRemainingOperations(context, operationsCount)
                        val progress = if (freeLimit > 0) (operationsCount.toFloat() / freeLimit).coerceIn(0f, 1f) else 1f

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "العمليات المجانية المستخدمة:",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Text(
                                    text = "$operationsCount / $freeLimit",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (operationsCount >= freeLimit) Color(0xFFE53935) else PrimaryTeal,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (remaining > 0)
                                    "متبقي لديك $remaining عملية مجانية قبل الحاجة للتفعيل."
                                else
                                    "لقد استنفدت العمليات المجانية بالكامل ($freeLimit/$freeLimit). يرجى التفعيل للمتابعة.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (remaining == 0) Color(0xFFE53935) else Color(0xFF64748B),
                                    fontWeight = if (remaining == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showLicenseDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تفعيل النسخة الكاملة الآن", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        val plan = LicenseManager.getActivePlan(context)
                        val remDays = LicenseManager.getRemainingDays(context)
                        val actRecord = remember(isActivated) { LicenseManager.getActivationRecord(context) }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "حالة الاشتراك: ${plan?.titleArabic ?: "اشتراك سارٍ"}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                            )
                            if (actRecord != null && actRecord.subscriberName.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "مسجل باسم: ${actRecord.subscriberName} • ${actRecord.deviceModel}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "متبقي في اشتراكك الحالي: $remDays يوماً.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (remDays <= 5) Color(0xFFE53935) else PrimaryTeal,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = { showLicenseDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تجديد أو ترقية الاشتراك", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: دليل الاستخدام والتعليمات التفاعلية
        item {
            Card(
                onClick = { showTourDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = AccentGold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "دليل الاستخدام والتعليمات",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "شرح تفاعلي لكافة وظائف وأزرار التطبيق",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "عرض",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section: آراء وملاحظات المستخدمين
        item {
            Card(
                onClick = { showFeedbackDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SecondaryAqua.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = SecondaryAqua)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "شاركنا رأيك ومقترحاتك 💬",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إرسال اقتراح، إبلاغ عن مشكلة، أو تقييم التجربة مباشرة للمطور",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "عرض",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section: عن التطبيق والمطور
        item {
            Card(
                onClick = onNavigateToAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryTeal)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "عن التطبيق والمطور",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "م. عمر عبد الله علي الحومي • v1.0.0",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "عرض",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section: أدوات التطوير واختبار البيانات (تظهر فقط في نسخة Debug)
        if (com.example.BuildConfig.DEBUG) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = AccentGold)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "أدوات المطور واختبار البيانات (Debug)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "بيانات تجريبية لـ 100 عميل و 1000 عملية لاختبار التطبيق الشامل",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isSeedingData) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "جاري معالجة العمليات والبيانات... يرجى الانتظار",
                                style = MaterialTheme.typography.bodySmall.copy(color = AccentGold),
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showConfirmSeedDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("🌱 توليد 100عميل و1000عملية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showConfirmClearDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("🧹 تصفير البيانات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmSeedDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmSeedDialog = false },
            title = { Text("توليد بيانات تجريبية ضخمة", fontWeight = FontWeight.Bold) },
            text = {
                Text("سيتم توليد 100 عميل يمني، و 1,000 عملية متنوعة تشمل جلسات سقي، سندات قبض وصرف، وخصومات تسوية، ومشتريات ماء شبكة. هل تريد المتابعة؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmSeedDialog = false
                        viewModel.seedDebugData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("بدء التوليد", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmSeedDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("تأكيد تصفير البيانات", fontWeight = FontWeight.Bold) },
            text = {
                Text("تنبيه: سيتم مسح كافة العملاء وجلسات السقي والسندات والمشتريات الحالية في قاعدة البيانات لاختبار بيئة نظيفة. هل أنت متأكد؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmClearDialog = false
                        viewModel.clearAllDebugData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("تصفير الكل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(onDismiss = { showFeedbackDialog = false })
    }

    // Confirmation Dialogs for Restore with Critical Data Loss Warning
    val toRestore = backupToRestore
    if (toRestore != null) {
        RestoreWarningDialog(
            title = "استعادة نسخة احتياطية محلية",
            backupDateOrName = toRestore.formattedDate,
            additionalInfo = toRestore.sizeText,
            onConfirm = {
                viewModel.restoreBackupFromFile(toRestore.file)
                backupToRestore = null
            },
            onDismiss = { backupToRestore = null }
        )
    }

    val autoToRestore = autoBackupToRestore
    if (autoToRestore != null) {
        RestoreWarningDialog(
            title = "استعادة " + autoToRestore.title,
            backupDateOrName = autoToRestore.formattedDate,
            additionalInfo = autoToRestore.sizeText,
            onConfirm = {
                viewModel.restoreBackupFromFile(autoToRestore.file)
                autoBackupToRestore = null
            },
            onDismiss = { autoBackupToRestore = null }
        )
    }

    val toDelete = backupToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            title = { Text("تأكيد حذف النسخة", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد حذف ملف النسخة الاحتياطية (${toDelete.name}) نهائياً من الهاتف؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackupFile(toDelete.file)
                        backupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Google Drive AppData Cloud Restore Dialog with Critical Data Loss Warning
    val driveToRestore = driveBackupToRestore
    if (driveToRestore != null) {
        RestoreWarningDialog(
            title = "استعادة من سحابة Google Drive",
            backupDateOrName = driveToRestore.formattedDate,
            additionalInfo = driveToRestore.sizeText,
            onConfirm = {
                viewModel.restoreFromGoogleDriveAppData(driveToRestore)
                driveBackupToRestore = null
            },
            onDismiss = { driveBackupToRestore = null }
        )
    }

    // Imported File Restore Dialog with Critical Data Loss Warning
    val uri = uriToRestore
    if (uri != null) {
        RestoreWarningDialog(
            title = "استعادة من ملف مستورد",
            backupDateOrName = "الملف المختار من ذاكرة الجهاز",
            additionalInfo = null,
            onConfirm = {
                viewModel.restoreBackup(uri)
                uriToRestore = null
            },
            onDismiss = { uriToRestore = null }
        )
    }

    // Clean Old Backups Confirmation Dialog
    if (showCleanOldBackupsConfirm) {
        AlertDialog(
            onDismissRequest = { showCleanOldBackupsConfirm = false },
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
                    onClick = {
                        viewModel.cleanOldLocalBackups(3)
                        showCleanOldBackupsConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("تنظيف الآن", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanOldBackupsConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Google Drive AppData Cloud Delete Dialog
    val driveToDelete = driveBackupToDelete
    if (driveToDelete != null) {
        AlertDialog(
            onDismissRequest = { driveBackupToDelete = null },
            title = { Text("حذف نسخة من Google Drive", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد حذف هذه النسخة السحابية (${driveToDelete.name}) نهائياً من سحابة Google Drive؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDriveBackup(driveToDelete)
                        driveBackupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف من السحابة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { driveBackupToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Floating Toast Notification
    LuxuryToastNotification(
        toast = toast,
        onDismiss = { viewModel.dismissToast() },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 85.dp)
    )

    // License Dialog
    val isTrialLimitExceeded = LicenseManager.isTrialLimitExceeded(context, operationsCount)

    if (showLicenseDialog && !isTrialLimitExceeded) {
        LicenseDialog(
            onDismiss = { showLicenseDialog = false },
            onActivated = {
                viewModel.refreshActivationStatus()
                showLicenseDialog = false
            },
            isMandatory = false
        )
    }

    // Showcase Tour Dialog
    if (showTourDialog) {
        com.example.features.home.HomeShowcaseTourDialog(
            onDismiss = { showTourDialog = false }
        )
    }

    // Google Drive Backup Account Dialog
    if (showGoogleBackupAccountDialog) {
        val initialDialogName = distributorName.takeIf { it.isNotBlank() && it != "موزع الماء / المسرب" }
            ?: com.example.core.license.UserDeviceTracker.getRegisteredName(context).takeIf { it.isNotBlank() }
            ?: com.example.core.license.LicenseManager.getActivationRecord(context)?.subscriberName
            ?: ""

        val initialDialogPhone = distributorPhone.takeIf { it.isNotBlank() && it != "777000000" }
            ?: com.example.core.license.UserDeviceTracker.getRegisteredPhone(context).takeIf { it.isNotBlank() }
            ?: ""

        com.example.core.ui.GoogleBackupAccountDialog(
            initialName = initialDialogName,
            initialPhone = initialDialogPhone,
            currentGoogleAccount = googleAccount,
            onDismiss = {
                showGoogleBackupAccountDialog = false
                pendingAutoBackupAfterSignIn = false
            },
            onProceed = { name, phone, shouldLaunchSignIn ->
                showGoogleBackupAccountDialog = false
                com.example.core.license.UserDeviceTracker.saveUserRegistrationInfo(context, name, phone)
                viewModel.updateDistributorInfo(name, phone)

                if (shouldLaunchSignIn) {
                    val signInIntent = GoogleDriveBackupHelper.getGoogleSignInClient(context).signInIntent
                    googleSignInLauncher.launch(signInIntent)
                } else {
                    viewModel.backupToGoogleDriveAppData()
                }
            }
        )
    }
}
}

@Composable
private fun RestoreWarningDialog(
    title: String,
    backupDateOrName: String,
    additionalInfo: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF991B1B)
                ),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "النسخة: $backupDateOrName" + if (additionalInfo != null) " ($additionalInfo)" else "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تحذير فقدان البيانات:",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB91C1C)
                                )
                            )
                        }

                        Text(
                            text = "عملية الاستعادة ستقوم باستبدال قاعدة البيانات الحالية بالكامل ببيانات هذه النسخة.\n\n⚠️ أي جلسات سقي، سندات، أو عمليات شراء تم تسجيلها بعد تاريخ هذه النسخة سيتم حذفها نهائياً وفقدانها ولا يمكن التراجع عنها!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF7F1D1D),
                                lineHeight = 17.sp,
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }

                Text(
                    text = "💡 نصيحة: إذا كانت لديك بيانات جديدة غير محفوظة في هذه النسخة، قم بإلغاء العملية واضغط 'نسخة جديدة' أولاً لحفظ نسخة احتياطية إضافية.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp,
                        fontSize = 10.5.sp
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("نعم، استبدال واستعادة البيانات", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("إلغاء التراجع", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    )
}

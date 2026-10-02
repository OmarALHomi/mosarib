package com.baynana.features.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.AccountCircle
import com.baynana.core.license.LicenseDialog
import com.baynana.core.license.LicenseManager
import com.baynana.core.security.BiometricHelper
import com.baynana.core.ui.ToastType
import com.baynana.core.util.BackupManager
import com.baynana.core.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import com.baynana.core.ui.LuxuryToastNotification
import com.baynana.core.util.Formatters
import com.baynana.features.pumps.PumpSource
import com.baynana.ui.theme.AccentEmerald
import com.baynana.ui.theme.AccentGold
import com.baynana.ui.theme.PrimaryTeal
import com.baynana.ui.theme.SecondaryAqua

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

    var distributorName by remember(config.distributorName) { mutableStateOf(config.distributorName) }
    var distributorPhone by remember(config.distributorPhone) { mutableStateOf(config.distributorPhone) }
    var defaultPriceStr by remember(config.defaultPricePerHour) { mutableStateOf(config.defaultPricePerHour.toString()) }
    var currencySymbol by remember(config.currencySymbol) { mutableStateOf(config.currencySymbol) }

    // File Picker Launcher for JSON Database Restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val isBiometricAvailable = remember { BiometricHelper.isAvailable(context) }
    val deviceCode = remember { LicenseManager.getDeviceCode(context) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    val savedBackups by viewModel.savedBackups.collectAsStateWithLifecycle()
    var backupToRestore by remember { mutableStateOf<BackupManager.BackupFileInfo?>(null) }
    var backupToDelete by remember { mutableStateOf<BackupManager.BackupFileInfo?>(null) }

    val googleAccount by viewModel.googleAccount.collectAsStateWithLifecycle()
    val driveBackups by viewModel.driveBackups.collectAsStateWithLifecycle()
    val isDriveLoading by viewModel.isDriveLoading.collectAsStateWithLifecycle()

    var driveBackupToRestore by remember { mutableStateOf<GoogleDriveBackupHelper.DriveBackupFile?>(null) }
    var driveBackupToDelete by remember { mutableStateOf<GoogleDriveBackupHelper.DriveBackupFile?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.onGoogleSignInSuccess(account)
            }
        } catch (e: Exception) {
            viewModel.showToast("فشل تسجيل الدخول بحساب Google: ${e.localizedMessage}", ToastType.ERROR)
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SecondaryAqua.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = SecondaryAqua)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("مظهر التطبيق", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.themeMode == "LIGHT",
                                onClick = { viewModel.updateThemeMode("LIGHT") },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Icon(Icons.Default.LightMode, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("نهاري")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.themeMode == "DARK",
                                onClick = { viewModel.updateThemeMode("DARK") },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ليلي")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = config.themeMode == "SYSTEM",
                                onClick = { viewModel.updateThemeMode("SYSTEM") },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryTeal)
                            )
                            Text("تلقائي")
                        }
                    }
                }
            }
        }

        // Section: Roles Management (أدواري في منظومة بيننا)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌾", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("أدواري في منظومة بيننا", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("فعّل الأدوار التي تمارسها لتظهر لك أقسامها مباشرة", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val rolesList = listOf(
                        Triple("MUSRIB", "💧 مسرب ماء", "إدارة جلسات السقي وتوزيع الماء وحسابات المزارعين"),
                        Triple("FARMER", "🌾 مزارع (صاحب أرض)", "متابعة كشوفات السقي، طلب دلالين، وتسجيل مصروفات المزرعة"),
                        Triple("DALLAL", "🤝 دلال ووسيط زراعي", "نشر عروض المحاصيل في البورصة وإبرام عقود الصلح"),
                        Triple("BUYER", "📦 مشتري / مجبري", "تصفح السوق، طلب معاينات، ومتابعة الأقساط والدفعات")
                    )

                    val activeSet = config.activeRoles.split(",").toSet()

                    rolesList.forEach { (code, title, desc) ->
                        val isChecked = activeSet.contains(code)
                        val isPrimary = config.primaryRole == code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newSet = if (isChecked) {
                                        if (activeSet.size > 1) activeSet - code else activeSet
                                    } else {
                                        activeSet + code
                                    }
                                    val newPrimary = if (isPrimary && !newSet.contains(code)) newSet.first() else config.primaryRole
                                    viewModel.updateRoles(newPrimary, newSet.joinToString(","))
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    if (isPrimary) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PrimaryTeal.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("الأساسي", style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                                Text(text = desc, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    val newSet = if (checked) activeSet + code else {
                                        if (activeSet.size > 1) activeSet - code else activeSet
                                    }
                                    val newPrimary = if (isPrimary && !newSet.contains(code)) newSet.first() else config.primaryRole
                                    viewModel.updateRoles(newPrimary, newSet.joinToString(","))
                                },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryTeal)
                            )
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                            enabled = isBiometricAvailable || config.biometricEnabled,
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
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "النسخ الاحتياطي والسحابي",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    "حفظ محلي وسحابي محمي عبر Google Drive",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                )
                            }
                        }

                        // زر إنشاء نسخة محلية جديدة
                        Button(
                            onClick = { viewModel.backupToPhoneStorage() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("نسخة جديدة", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Drive AppData Cloud Integration Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .padding(12.dp)
                    ) {
                        Column {
                            val acc = googleAccount
                            if (acc == null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "سحابة Google Drive (AppData)",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF166534)
                                            )
                                        )
                                        Text(
                                            text = "احفظ نسخ بياناتك تلقائياً في سحابة خاصة لا يمكن حذفها أو العبث بها بالخطأ.",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF475569), fontSize = 11.sp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val signInIntent = GoogleDriveBackupHelper.getGoogleSignInClient(context).signInIntent
                                            googleSignInLauncher.launch(signInIntent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ربط بحساب Google", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // Signed in state
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
                                                .background(Color(0xFFDCFCE7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = acc.email ?: "حساب Google",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF14532D)),
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "المجلد السحابي المحمي (drive.appdata) متصل",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF16A34A), fontSize = 10.sp)
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

                                Spacer(modifier = Modifier.height(10.dp))

                                // Cloud Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.backupToGoogleDriveAppData() },
                                        enabled = !isDriveLoading,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("رفع نسخة إلى سحابة Drive", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.loadDriveBackups() },
                                        enabled = !isDriveLoading,
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Sync, contentDescription = "تحديث السحابة", modifier = Modifier.size(16.dp), tint = Color(0xFF16A34A))
                                    }
                                }

                                if (isDriveLoading) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF16A34A))
                                }

                                // Drive AppData Backups List
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "النسخ في سحابة Google Drive (${driveBackups.size}):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                                )

                                if (driveBackups.isEmpty() && !isDriveLoading) {
                                    Text(
                                        text = "لا توجد نسخ مرفوعة بعد. اضغط على 'رفع نسخة إلى سحابة Drive' لحفظ بياناتك سحابياً.",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 11.sp),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        driveBackups.forEach { cloudBackup ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = cloudBackup.formattedDate,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                                    )
                                                    Text(
                                                        text = "${cloudBackup.sizeText}  •  ${cloudBackup.name}",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp),
                                                        maxLines = 1
                                                    )
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFE8F5E9))
                                                            .clickable { driveBackupToRestore = cloudBackup }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text("استعادة", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold))
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFFFEBEE))
                                                            .clickable { driveBackupToDelete = cloudBackup },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(13.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Local sharing and import buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.createBackupAndShare() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrimaryTeal)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("مشاركة ملف (.back)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { restoreFileLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentGold)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("استيراد من الهاتف", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // استعراض النسخ الاحتياطية المحفوظة محلياً على الهاتف
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "النسخ المحفوظة على هذا الهاتف (${savedBackups.size})",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        IconButton(
                            onClick = { viewModel.loadBackups() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = "تحديث", modifier = Modifier.size(16.dp), tint = PrimaryTeal)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (savedBackups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد نسخ احتياطية محفوظة على الهاتف بعد. اضغط على 'نسخة جديدة' بالأعلى لإنشاء نسخة احتياطية فورية.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            savedBackups.forEach { backup ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Backup, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = backup.formattedDate,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(
                                                text = "${backup.sizeText}  •  ${backup.name}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // الأزرار: استعادة، مشاركة، حذف
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // استعادة
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PrimaryTeal.copy(alpha = 0.12f))
                                                .clickable { backupToRestore = backup }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("استعادة", style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Bold))
                                        }

                                        // مشاركة
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFE8F5E9))
                                                .clickable { viewModel.shareExistingBackup(backup.file) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                        }

                                        // حذف
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFFFEBEE))
                                                .clickable { backupToDelete = backup },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                Text(
                                    text = if (isActivated) "نسخة مفعلة بصفة دائمة" else "النسخة التجريبية (200 عملية)",
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
                        val freeLimit = LicenseManager.FREE_OPERATIONS_LIMIT
                        val progress = (operationsCount.toFloat() / freeLimit).coerceIn(0f, 1f)
                        val remaining = (freeLimit - operationsCount).coerceAtLeast(0)

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
                                    "لقد استنفدت العمليات المجانية بالكامل (200/200). يرجى التفعيل للمتابعة.",
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "حالة الاشتراك: ${plan?.titleArabic ?: "اشتراك سارٍ"}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                            )
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

        // Section: عن التطبيق والمطور
        item {
            Card(
                onClick = onNavigateToAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
    }

    // Confirmation Dialogs for Restore and Delete
    val toRestore = backupToRestore
    if (toRestore != null) {
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = { Text("تأكيد استعادة النسخة الاحتياطية", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من استعادة هذه النسخة بتاريخ ${toRestore.formattedDate}؟ سيتم تحديث وتثبيت البيانات الموجودة في النسخة.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreBackupFromFile(toRestore.file)
                        backupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("استعادة الآن", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    val toDelete = backupToDelete
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            title = { Text("تأكيد حذف النسخة", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل تريد حذف ملف النسخة الاحتياطية (${toDelete.name}) نهائياً؟")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBackupFile(toDelete.file)
                        backupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
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

    // Google Drive AppData Cloud Restore Dialog
    val driveToRestore = driveBackupToRestore
    if (driveToRestore != null) {
        AlertDialog(
            onDismissRequest = { driveBackupToRestore = null },
            title = { Text("استعادة من سحابة Google Drive", fontWeight = FontWeight.Bold) },
            text = {
                Text("هل أنت متأكد من استعادة النسخة السحابية بتاريخ ${driveToRestore.formattedDate} بحجم ${driveToRestore.sizeText}؟ سيتم تنزيل النسخة وتطبيق بياناتها بأمان.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromGoogleDriveAppData(driveToRestore)
                        driveBackupToRestore = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                ) {
                    Text("استعادة السحابة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { driveBackupToRestore = null }) {
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
    if (showLicenseDialog) {
        LicenseDialog(
            onDismiss = { showLicenseDialog = false },
            onActivated = {
                viewModel.refreshActivationStatus()
                showLicenseDialog = false
            },
            isMandatory = false
        )
    }
}
}

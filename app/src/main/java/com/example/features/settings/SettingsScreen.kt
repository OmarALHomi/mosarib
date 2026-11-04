package com.example.features.settings
import android.widget.Toast
import com.example.ui.theme.PrimaryTeal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.license.RemoteConfigManager
import com.example.core.license.UserDeviceTracker
import com.example.core.security.BiometricHelper
import com.example.core.speech.MosaribSpeaker
import com.example.core.speech.SpeechStatus
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.ToastType
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.core.util.GoogleDriveBackupHelper
import com.example.features.customers.CustomerGroupEditorDialog
import com.example.features.customers.CustomersViewModel
import com.example.features.customers.ManageGroupsDialog
import com.example.features.customers.RecycleBinDialog
import com.example.features.feedback.FeedbackDialog
import com.example.core.ui.guide.GuidePreferences
import androidx.compose.material.icons.filled.RestartAlt
import com.example.ui.theme.mosarib
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException


enum class SettingsSubPage {
    BACKUP,
    VOICE
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onSubPageOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val customersViewModel: CustomersViewModel = viewModel()
    val archivedCustomers by customersViewModel.archivedCustomers.collectAsStateWithLifecycle()
    val customGroups by customersViewModel.customGroups.collectAsStateWithLifecycle()
    val rawCustomers by customersViewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    var showManageGroupsDialog by remember { mutableStateOf(false) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<String?>(null) }
    var initialMemberIdsForEdit by remember { mutableStateOf<Set<Long>>(emptySet()) }
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

    // Backup tabs, pending restore/delete confirmations and Google sign-in flags
    val backupState = remember { SettingsBackupUiState() }

    var showPriceSheet by remember { mutableStateOf(false) }
    var showDistributorSheet by remember { mutableStateOf(false) }
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var currentSubPage by rememberSaveable { mutableStateOf<SettingsSubPage?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(currentSubPage) {
        onSubPageOpenChange(currentSubPage != null)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshRemotePolicies()
    }

    // File Picker Launcher for JSON Database Restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { backupState.uriToRestore = it }
    }

    // SAF Document Creator for saving backup anywhere on device
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackupToUri(it) }
    }

    val context = LocalContext.current
    val isDark = isAppDark()
    val isBiometricAvailable = remember { BiometricHelper.isAvailable(context) }
    val deviceCode = remember { LicenseManager.getDeviceCode(context) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    val savedBackups by viewModel.savedBackups.collectAsStateWithLifecycle()
    val autoBackups by viewModel.autoBackups.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsStateWithLifecycle()

    val googleAccount by viewModel.googleAccount.collectAsStateWithLifecycle()
    val driveBackups by viewModel.driveBackups.collectAsStateWithLifecycle()
    val isDriveLoading by viewModel.isDriveLoading.collectAsStateWithLifecycle()

    val targetBackupSourceTab by viewModel.targetBackupSourceTab.collectAsStateWithLifecycle()
    LaunchedEffect(targetBackupSourceTab) {
        backupState.selectedBackupSourceTab = targetBackupSourceTab
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.onGoogleSignInSuccess(account)
                if (backupState.pendingAutoBackupAfterSignIn) {
                    backupState.pendingAutoBackupAfterSignIn = false
                    viewModel.backupToGoogleDriveAppData()
                }
            }
        } catch (e: Exception) {
            backupState.pendingAutoBackupAfterSignIn = false
            viewModel.showToast("فشل تسجيل الدخول بحساب Google: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
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

    LaunchedEffect(driveRecoveryIntent) {
        driveRecoveryIntent?.let {
            driveRecoveryLauncher.launch(it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        val isBackupAllowed by viewModel.isBackupAllowed.collectAsStateWithLifecycle()
        val isDeviceBkLocked by viewModel.isDeviceBackupLocked.collectAsStateWithLifecycle()
        val isCloudBackupAllowed = isBackupAllowed && RemoteConfigManager.isCloudBackupAllowed(context)

        // 1. القائمة الرئيسية للإعدادات - تبقى مركبة دائماً للحفاظ على موضع التمرير بنسبة 100%
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
        ) {
            // -----------------------------------------------------------------
            // 1. الحساب والاشتراك
            // -----------------------------------------------------------------
            item {
                SettingsSectionHeader(title = "الحساب والاشتراك")
                SettingsCard {
                    // حالة الاشتراك
                    val subscriptionSubtitle = remember(isActivated, operationsCount) {
                        if (isActivated) {
                            val plan = LicenseManager.getActivePlan(context)
                            val remDays = LicenseManager.getRemainingDays(context)
                            "${plan?.titleArabic ?: "اشتراك سارٍ"} · متبقي $remDays يوماً"
                        } else {
                            val remaining = LicenseManager.getRemainingOperations(context, operationsCount)
                            "نسخة تجريبية · متبقي $remaining عملية"
                        }
                    }
                    SettingsRow(
                        title = "حالة الاشتراك",
                        subtitle = subscriptionSubtitle,
                        icon = Icons.Default.Verified,
                        iconTint = if (isActivated) MaterialTheme.mosarib.credit else MaterialTheme.mosarib.warning,
                        onClick = { showLicenseDialog = true }
                    )

                    SettingsRowDivider()

                    // رمز الجهاز
                    SettingsRow(
                        title = "رمز الجهاز",
                        subtitle = deviceCode,
                        icon = Icons.Default.Smartphone,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("رمز الجهاز", deviceCode))
                            viewModel.showToast("تم نسخ رمز الجهاز بنجاح", ToastType.SUCCESS)
                        },
                        trailing = {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("رمز الجهاز", deviceCode))
                                    viewModel.showToast("تم نسخ رمز الجهاز بنجاح", ToastType.SUCCESS)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ رمز الجهاز",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        showChevron = false
                    )

                    // قفل التطبيق بالبصمة (hide if biometrics unavailable)
                    if (isBiometricAvailable) {
                        SettingsRowDivider()
                        SettingsRow(
                            title = "قفل التطبيق بالبصمة",
                            subtitle = if (config.biometricEnabled) "مفعل لحماية بياناتك" else "معطل",
                            icon = Icons.Default.Fingerprint,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { viewModel.updateBiometricEnabled(!config.biometricEnabled) },
                            trailing = {
                                Switch(
                                    checked = config.biometricEnabled,
                                    onCheckedChange = { viewModel.updateBiometricEnabled(it) }
                                )
                            },
                            showChevron = false
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2. التسعير والموزع
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "التسعير والموزع")
                SettingsCard {
                    // سعر ساعة الماء
                    SettingsRow(
                        title = "سعر ساعة الماء",
                        subtitle = "${Formatters.formatNumber(config.defaultPricePerHour)} ${config.currencySymbol}",
                        icon = Icons.Default.AttachMoney,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showPriceSheet = true }
                    )

                    SettingsRowDivider()

                    // بيانات الموزع
                    val distSubtitle = listOf(
                        config.distributorName.ifBlank { "لم يحدد الاسم" },
                        config.distributorPhone.ifBlank { "لا يوجد هاتف" }
                    ).joinToString(" · ")
                    SettingsRow(
                        title = "بيانات الموزع",
                        subtitle = distSubtitle,
                        icon = Icons.Default.Person,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showDistributorSheet = true }
                    )

                    SettingsRowDivider()

                    // رمز العملة
                    SettingsRow(
                        title = "رمز العملة",
                        subtitle = config.currencySymbol,
                        icon = Icons.Default.MonetizationOn,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showCurrencySheet = true }
                    )
                }
            }

            // -----------------------------------------------------------------
            // 3. البيانات
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "البيانات")
                SettingsCard {
                    // النسخ الاحتياطي
                    val latestBackupDate = remember(savedBackups, autoBackups) {
                        val localLatest = savedBackups.maxByOrNull { it.file.lastModified() }
                        val autoLatest = autoBackups.maxByOrNull { it.file.lastModified() }
                        when {
                            localLatest != null && autoLatest != null -> {
                                if (localLatest.file.lastModified() >= autoLatest.file.lastModified()) localLatest.formattedDate else autoLatest.formattedDate
                            }
                            localLatest != null -> localLatest.formattedDate
                            autoLatest != null -> autoLatest.formattedDate
                            else -> null
                        }
                    }
                    val backupSubtitle = if (latestBackupDate != null) "آخر نسخة: $latestBackupDate" else "لا توجد نسخ احتياطية مسجلة"
                    SettingsRow(
                        title = "النسخ الاحتياطي والاستعادة",
                        subtitle = backupSubtitle,
                        icon = Icons.Default.CloudSync,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { currentSubPage = SettingsSubPage.BACKUP }
                    )

                    SettingsRowDivider()

                    // سلة المحذوفات
                    SettingsRow(
                        title = "سلة المحذوفات",
                        subtitle = "${archivedCustomers.size} عميل محذوف",
                        icon = Icons.Default.Delete,
                        iconTint = MaterialTheme.mosarib.debt,
                        onClick = { showRecycleBinDialog = true }
                    )

                    SettingsRowDivider()

                    // إدارة المجموعات
                    SettingsRow(
                        title = "إدارة المجموعات",
                        subtitle = "${customGroups.size} مجموعات",
                        icon = Icons.Default.Bookmarks,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showManageGroupsDialog = true }
                    )

                    SettingsRowDivider()

                    // فحص العمليات المستوردة المكررة
                    SettingsRow(
                        title = "فحص العمليات المستوردة المكررة",
                        subtitle = "فحص ومطابقة العمليات لمنع التكرار",
                        icon = Icons.Default.FindInPage,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { viewModel.scanImportDuplicates() }
                    )
                }
            }

            // -----------------------------------------------------------------
            // 4. المظهر والصوت
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "المظهر والصوت")
                SettingsCard {
                    // المظهر
                    val themeLabel = when (config.themeMode) {
                        "light" -> "فاتح"
                        "dark" -> "داكن"
                        else -> "حسب الجهاز"
                    }
                    SettingsRow(
                        title = "المظهر",
                        subtitle = themeLabel,
                        icon = Icons.Default.Palette,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { showThemeSheet = true }
                    )

                    SettingsRowDivider()

                    // القراءة الصوتية
                    var speechStatus by remember { mutableStateOf<SpeechStatus?>(null) }
                    val chosenVoiceName = remember { MosaribSpeaker.getChosenVoiceName(context) }
                    LaunchedEffect(Unit) {
                        speechStatus = MosaribSpeaker.prepare(context)
                    }
                    val voiceSubtitle = remember(speechStatus, chosenVoiceName) {
                        val statusText = when (speechStatus) {
                            SpeechStatus.Ready -> "جاهز"
                            SpeechStatus.MissingVoiceData -> "بيانات ناقصة"
                            SpeechStatus.LanguageNotSupported -> "غير مدعوم"
                            SpeechStatus.NoEngine -> "لا يوجد محرك"
                            null -> "جاري الفحص"
                        }
                        val voices = MosaribSpeaker.getInstalledOfflineArabicVoices(context)
                        val idx = voices.indexOfFirst { it.name == chosenVoiceName }
                        val vLabel = if (idx >= 0) "صوت ${idx + 1}" else "تلقائي"
                        "$statusText · Google · $vLabel"
                    }
                    SettingsRow(
                        title = "القراءة الصوتية",
                        subtitle = voiceSubtitle,
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = { currentSubPage = SettingsSubPage.VOICE }
                    )
                }
            }

            // -----------------------------------------------------------------
            // 5. المساعدة
            // -----------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(14.dp))
                SettingsSectionHeader(title = "المساعدة")
                SettingsCard {
                    // مركز المساعدة
                    SettingsRow(
                        title = "مركز المساعدة والتعليمات",
                        subtitle = "إجابات سريعة، نصائح، وشروحات توجيهية",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = MaterialTheme.mosarib.warning,
                        onClick = onNavigateToHelp
                    )

                    SettingsRowDivider()

                    // إعادة عرض التعليمات
                    SettingsRow(
                        title = "إعادة عرض التعليمات",
                        subtitle = "إعادة تفعيل الجولات التوضيحية والإرشادات التفاعلية",
                        icon = Icons.Default.RestartAlt,
                        iconTint = PrimaryTeal,
                        onClick = {
                            GuidePreferences.resetAll(context)
                            Toast.makeText(context, "تمت إعادة تفعيل جميع التعليمات التوضيحية", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsRowDivider()

                    // شاركنا رأيك
                    SettingsRow(
                        title = "شاركنا رأيك وملاحظاتك",
                        subtitle = "اقتراحات وملاحظات لتطوير التطبيق",
                        icon = Icons.Default.Star,
                        iconTint = MaterialTheme.mosarib.warning,
                        onClick = { showFeedbackDialog = true }
                    )

                    SettingsRowDivider()

                    // عن التطبيق
                    SettingsRow(
                        title = "عن التطبيق",
                        subtitle = "الإصدار ${BuildConfig.VERSION_NAME} · الشروط والتراخيص",
                        icon = Icons.Default.Info,
                        iconTint = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToAbout
                    )
                }
            }

            // -----------------------------------------------------------------
            // 6. أدوات المطور (debug builds only)
            // -----------------------------------------------------------------
            if (BuildConfig.DEBUG) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingsSectionHeader(title = "أدوات المطور")
                    SettingsCard {
                        SettingsRow(
                            title = "توليد بيانات تجريبية",
                            subtitle = "إضافة عملاء وجلسات وسندات للاختبار",
                            icon = Icons.Default.Add,
                            iconTint = MaterialTheme.colorScheme.primary,
                            onClick = { showConfirmSeedDialog = true }
                        )

                        SettingsRowDivider()

                        SettingsRow(
                            title = "مسح كافة البيانات",
                            subtitle = "إعادة تعيين قاعدة البيانات بالكامل",
                            icon = Icons.Default.Delete,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showConfirmClearDialog = true }
                        )
                    }
                }
            }
        }

        // 2. الصفحات الفرعية الكاملة - تظهر كطبقة مستقلة بكامل الشاشة فوق القائمة مع سهم رجوع واحد
        AnimatedContent(
            targetState = currentSubPage,
            transitionSpec = {
                if (targetState != null) {
                    slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Start) + fadeIn() togetherWith
                        fadeOut()
                } else {
                    fadeIn() togetherWith
                        slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.End) + fadeOut()
                }
            },
            label = "SettingsSubPageOverlay"
        ) { subPage ->
            when (subPage) {
                null -> {}
                SettingsSubPage.BACKUP -> {
                    SettingsBackupScreen(
                        onBack = { currentSubPage = null },
                        state = backupState,
                        isDark = isDark,
                        isBackupAllowed = isBackupAllowed,
                        isDeviceBackupLocked = isDeviceBkLocked,
                        isCloudBackupAllowed = isCloudBackupAllowed,
                        isAutoBackupEnabled = isAutoBackupEnabled,
                        autoBackups = autoBackups,
                        savedBackups = savedBackups,
                        googleAccount = googleAccount,
                        driveBackups = driveBackups,
                        isDriveLoading = isDriveLoading,
                        onCreateBackup = {
                            val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                            createBackupLauncher.launch("mosarib_backup_$dateStr.back")
                        },
                        onImportBackup = { restoreFileLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json")) },
                        onShareNewBackup = { viewModel.createBackupAndShare() },
                        onAutoBackupEnabledChange = { viewModel.toggleAutoBackup(it) },
                        onRefreshBackups = { viewModel.loadBackups() },
                        onShareBackupFile = { viewModel.shareExistingBackup(it) },
                        onGoogleSignOut = { viewModel.onGoogleSignOut() },
                        onRefreshDriveBackups = { viewModel.loadDriveBackups() },
                        onCheckDuplicates = { viewModel.scanImportDuplicates() }
                    )
                }
                SettingsSubPage.VOICE -> {
                    SettingsVoiceScreen(
                        onBack = { currentSubPage = null }
                    )
                }
            }
        }

        if (showConfirmSeedDialog) {
            ConfirmSeedDebugDataDialog(
                onConfirm = {
                    showConfirmSeedDialog = false
                    viewModel.seedDebugData()
                },
                onDismiss = { showConfirmSeedDialog = false }
            )
        }

        if (showConfirmClearDialog) {
            ConfirmClearDebugDataDialog(
                onConfirm = {
                    showConfirmClearDialog = false
                    viewModel.clearAllDebugData()
                },
                onDismiss = { showConfirmClearDialog = false }
            )
        }

        if (showFeedbackDialog) {
            FeedbackDialog(onDismiss = { showFeedbackDialog = false })
        }

        BackupDialogsHost(
            state = backupState,
            onRestoreBackupFile = { viewModel.restoreBackupFromFile(it) },
            onDeleteBackupFile = { viewModel.deleteBackupFile(it) },
            onRestoreDriveBackup = { viewModel.restoreFromGoogleDriveAppData(it) },
            onRestoreImportedFile = { viewModel.restoreBackup(it) },
            onCleanOldBackups = { viewModel.cleanOldLocalBackups(3) },
            onDeleteDriveBackup = { viewModel.deleteDriveBackup(it) }
        )

        // Floating Toast Notification
        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
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



        if (backupState.showGoogleBackupAccountDialog) {
            DriveBackupAccountDialog(
                context = context,
                distributorName = distributorName,
                distributorPhone = distributorPhone,
                googleAccount = googleAccount,
                onDismiss = {
                    backupState.showGoogleBackupAccountDialog = false
                    backupState.pendingAutoBackupAfterSignIn = false
                },
                onProceed = { name, phone, shouldLaunchSignIn ->
                    backupState.showGoogleBackupAccountDialog = false
                    UserDeviceTracker.saveUserRegistrationInfo(context, name, phone)
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
 
        if (showManageGroupsDialog) {
            ManageGroupsDialog(
                groups = customGroups,
                allCustomers = rawCustomers,
                onDismiss = { showManageGroupsDialog = false },
                onEditGroup = { grp, memberIds ->
                    showManageGroupsDialog = false
                    groupToEdit = grp
                    initialMemberIdsForEdit = memberIds
                },
                onRenameGroup = { oldName, newName ->
                    customersViewModel.renameGroup(oldName, newName)
                },
                onDeleteGroup = { grp ->
                    customersViewModel.deleteGroup(grp)
                },
                onAddNewGroup = {
                    showManageGroupsDialog = false
                    showAddGroupDialog = true
                }
            )
        }

        if (showAddGroupDialog || groupToEdit != null) {
            CustomerGroupEditorDialog(
                groupToEdit = groupToEdit,
                initialMemberIdsForEdit = initialMemberIdsForEdit,
                rawCustomers = rawCustomers,
                onDismiss = {
                    showAddGroupDialog = false
                    groupToEdit = null
                    initialMemberIdsForEdit = emptySet()
                },
                onSave = { name, selectedIds ->
                    customersViewModel.saveGroup(name, selectedIds, groupToEdit)
                    showAddGroupDialog = false
                    groupToEdit = null
                    initialMemberIdsForEdit = emptySet()
                }
            )
        }

        if (showRecycleBinDialog) {
            RecycleBinDialog(
                archivedCustomers = archivedCustomers,
                onRestore = { customersViewModel.restoreCustomer(it.id) },
                onPermanentlyDelete = { customersViewModel.permanentlyDeleteCustomer(it.id) },
                onEmptyBin = { customersViewModel.emptyRecycleBin() },
                onDismiss = { showRecycleBinDialog = false }
            )
        }

        // 1. Water Price Sheet
        if (showPriceSheet) {
            MosaribBottomSheet(
                onDismissRequest = { showPriceSheet = false },
                title = "سعر ساعة الماء الافتراضي",
                subtitle = "يحدد السعر المبدئي عند فتح جلسات سقي جديدة"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = defaultPriceStr,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() || it == '.' }
                            defaultPriceStr = Formatters.formatAmountInput(clean)
                        },
                        label = { Text("السعر الافتراضي لكل ساعة (${config.currencySymbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "يطبق على الجلسات الجديدة فقط ولا يغير الجلسات السابقة.",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurface)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val rawDigits = defaultPriceStr.replace(",", "").trim()
                            val p = rawDigits.toDoubleOrNull() ?: 5000.0
                            viewModel.updateDefaultPricePerHour(p)
                            showPriceSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("حفظ السعر الجديد", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 2. Distributor Profile Sheet
        if (showDistributorSheet) {
            MosaribBottomSheet(
                onDismissRequest = { showDistributorSheet = false },
                title = "بيانات الموزع",
                subtitle = "تظهر في فواتير وسندات العملاء"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = distributorName,
                        onValueChange = { distributorName = it },
                        label = { Text("اسم الموزع أو البئر") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = distributorPhone,
                        onValueChange = { distributorPhone = it },
                        label = { Text("رقم الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.updateDistributorInfo(distributorName, distributorPhone)
                            showDistributorSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("حفظ البيانات", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 3. Currency Symbol Sheet
        if (showCurrencySheet) {
            MosaribBottomSheet(
                onDismissRequest = { showCurrencySheet = false },
                title = "رمز العملة",
                subtitle = "العملة المستخدمة في عرض كافة المبالغ والفواتير"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = currencySymbol,
                        onValueChange = { currencySymbol = it },
                        label = { Text("رمز العملة (مثال: ر.ي أو ر.س أو $)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val s = currencySymbol.trim().ifBlank { "ر.ي" }
                            currencySymbol = s
                            viewModel.updateCurrencySymbol(s)
                            showCurrencySheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("حفظ رمز العملة", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 4. Theme Mode Sheet
        if (showThemeSheet) {
            MosaribBottomSheet(
                onDismissRequest = { showThemeSheet = false },
                title = "المظهر",
                subtitle = "اختر مظهر التطبيق المفضل لديك"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        Triple("light", "فاتح", Icons.Default.LightMode),
                        Triple("dark", "داكن", Icons.Default.DarkMode),
                        Triple("system", "حسب الجهاز", Icons.Default.Palette)
                    )
                    modes.forEach { (mode, label, icon) ->
                        val isSelected = config.themeMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateThemeMode(mode)
                                    showThemeSheet = false
                                },
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }


    }
}
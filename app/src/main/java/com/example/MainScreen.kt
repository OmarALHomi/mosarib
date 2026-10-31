package com.example

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.CenterAlignedTopAppBar
import com.example.features.home.LivingThemeToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.license.UserDeviceTracker
import com.example.core.sync.AppSyncEngine
import com.example.core.sync.StartupDialogType
import com.example.core.ui.DeviceBlockedScreen
import com.example.core.ui.BackupReminderDialog
import com.example.core.updates.AppBroadcast
import com.example.core.updates.AppUpdateManager
import com.example.core.updates.BroadcastAnnouncementDialog
import com.example.core.updates.ForceUpdateScreen
import com.example.core.util.PeriodicBackupReminderManager
import com.example.features.feedback.FeedbackDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.features.about.AboutScreen
import com.example.features.customers.CustomerDetailScreen
import com.example.features.customers.CustomersScreen
import com.example.features.customers.CustomersViewModel
import com.example.features.home.HomeScreen
import com.example.features.onboarding.OnboardingScreen
import com.example.features.reports.ReportsScreen
import com.example.features.splash.SplashScreen
import com.example.features.reports.ReportsViewModel
import com.example.features.sessions.SessionsViewModel
import com.example.features.settings.SettingsScreen
import com.example.features.settings.SettingsViewModel
import com.example.features.vouchers.VouchersScreen
import com.example.features.vouchers.VouchersViewModel
import com.example.core.security.BiometricLockScreen
import com.example.core.ui.MosaribNavBar
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.MosaribTheme

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    REPORTS("إحصائيات", Icons.Filled.Assessment, Icons.Default.Assessment, "tab_reports"),
    CUSTOMERS("إدارة العملاء", Icons.Filled.People, Icons.Outlined.People, "tab_customers"),
    HOME("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    VOUCHERS("العمليات", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "tab_vouchers"),
    SETTINGS("الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    sessionsViewModel: SessionsViewModel = viewModel(),
    customersViewModel: CustomersViewModel = viewModel(),
    vouchersViewModel: VouchersViewModel = viewModel(),
    reportsViewModel: ReportsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val appConfig by settingsViewModel.appConfig.collectAsStateWithLifecycle()
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (appConfig.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    MosaribTheme(darkTheme = darkTheme) {
        // Arabic RTL layout provider
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val context = LocalContext.current
            var showSplashScreen by remember { mutableStateOf(true) }
            var showOnboarding by remember { mutableStateOf(!LicenseManager.isOnboardingCompleted(context)) }
            var isBiometricUnlocked by remember { mutableStateOf(false) }
            var selectedCustomerId by remember { mutableLongStateOf(0L) }
            var showAboutScreen by remember { mutableStateOf(false) }
            var showLicenseDialog by remember { mutableStateOf(false) }
            var showFeedbackDialog by remember { mutableStateOf(false) }
            var showCustomerMenu by remember { mutableStateOf(false) }
            var showExitConfirmationDialog by remember { mutableStateOf(false) }

            val customerExcelPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    customersViewModel.startImportFromUri(context, uri)
                }
            }

            val isActivated by settingsViewModel.isActivated.collectAsStateWithLifecycle()
            val operationsCount by settingsViewModel.operationsCount.collectAsStateWithLifecycle()

            var isDeviceBlocked by remember { mutableStateOf(UserDeviceTracker.isDeviceBlockedLocally(context)) }
            var blockReason by remember { mutableStateOf(UserDeviceTracker.getBlockReason(context)) }

            // الإعلانات والتحديثات الإجبارية
            var activeBroadcast by remember { mutableStateOf<AppBroadcast?>(null) }
            var showAnnouncementDialog by remember { mutableStateOf(false) }
            var showBackupReminderDialog by remember { mutableStateOf(false) }
            var specialDeviceWarning by remember { mutableStateOf<String?>(null) }
            var showSpecialWarningDialog by remember { mutableStateOf(false) }

            // مزامنة موحدة ومنظمة مع السحابة بدون ضغط على الشبكة وطابور أولويات للنوافذ
            LaunchedEffect(Unit) {
                val decision = AppSyncEngine.performConsolidatedStartupSync(context, operationsCount)
                when (decision.dialogToShow) {
                    StartupDialogType.FORCE_UPDATE -> {
                        activeBroadcast = decision.forceUpdateBroadcast
                    }
                    StartupDialogType.DEVICE_BLOCKED -> {
                        isDeviceBlocked = true
                        blockReason = decision.blockReason
                    }
                    StartupDialogType.BACKUP_REMINDER -> {
                        showBackupReminderDialog = true
                    }
                    StartupDialogType.ANNOUNCEMENT -> {
                        activeBroadcast = decision.announcementBroadcast
                        showAnnouncementDialog = true
                    }
                    StartupDialogType.NONE -> {}
                }

                // تحديث الحالات النشطة للإعدادات فور اكتمال المزامنة
                settingsViewModel.refreshRemotePolicies()

                // فحص التنبيه الخاص بالجهاز إن وجد
                val customWarn = com.example.core.license.RemoteConfigManager.getSpecialWarningMessage(context)
                if (!customWarn.isNullOrBlank() && customWarn != specialDeviceWarning) {
                    specialDeviceWarning = customWarn
                    showSpecialWarningDialog = true
                }
            }

            val tabs = remember { listOf(AppTab.REPORTS, AppTab.CUSTOMERS, AppTab.HOME, AppTab.VOUCHERS, AppTab.SETTINGS) }
            val pagerState = rememberPagerState(initialPage = 2) { tabs.size }
            val coroutineScope = rememberCoroutineScope()
            val currentTab = tabs[pagerState.currentPage]

            // اعتراض زر التراجع: التبديل للرئيسية أولاً إن كان في شاشة فرعية، وتأكيد الخروج إن كان في الرئيسية
            val shouldInterceptExit = !showSplashScreen && !showOnboarding && !showAboutScreen && selectedCustomerId == 0L
            BackHandler(enabled = shouldInterceptExit) {
                if (currentTab != AppTab.HOME) {
                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                } else {
                    showExitConfirmationDialog = true
                }
            }

            if (showSplashScreen) {
                SplashScreen(onTimeout = { showSplashScreen = false })
            } else if (isDeviceBlocked) {
                DeviceBlockedScreen(reason = blockReason)
            } else if (activeBroadcast?.isForceUpdate == true) {
                ForceUpdateScreen(broadcast = activeBroadcast!!)
            } else if (showOnboarding) {
                OnboardingScreen(onFinish = { showOnboarding = false })
            } else if (appConfig.biometricEnabled && !isBiometricUnlocked) {
                BiometricLockScreen(onUnlock = { isBiometricUnlocked = true })
            } else if (showAboutScreen) {
                AboutScreen(onBack = { showAboutScreen = false })
            } else if (selectedCustomerId > 0) {
                CustomerDetailScreen(
                    customerId = selectedCustomerId,
                    viewModel = customersViewModel,
                    vouchersViewModel = vouchersViewModel,
                    onBack = { selectedCustomerId = 0L }
                )
            } else {
                Scaffold(
                    topBar = {
                        if (currentTab != AppTab.HOME) {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = when (currentTab) {
                                            AppTab.REPORTS -> "إحصائيات"
                                            AppTab.CUSTOMERS -> "إدارة العملاء"
                                            AppTab.VOUCHERS -> "العمليات"
                                            AppTab.SETTINGS -> "الإعدادات"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                navigationIcon = {
                                    LivingThemeToggleButton(
                                        isDarkTheme = darkTheme,
                                        onToggle = {
                                            val nextMode = if (darkTheme) "LIGHT" else "DARK"
                                            settingsViewModel.updateThemeMode(nextMode)
                                        },
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                },
                                actions = {
                                    if (currentTab == AppTab.REPORTS) {
                                        IconButton(
                                            onClick = { reportsViewModel.exportComprehensiveReportPdf() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = "تصدير PDF",
                                                tint = PrimaryTeal,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }

                                    if (currentTab == AppTab.CUSTOMERS) {
                                        Box {
                                            IconButton(onClick = { showCustomerMenu = true }) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "خيارات إدارة العملاء",
                                                    tint = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showCustomerMenu,
                                                onDismissRequest = { showCustomerMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("استيراد من إكسل", fontWeight = FontWeight.Bold) },
                                                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = PrimaryTeal) },
                                                    onClick = {
                                                        showCustomerMenu = false
                                                        customerExcelPickerLauncher.launch(
                                                            arrayOf(
                                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                                "application/vnd.ms-excel",
                                                                "text/csv",
                                                                "text/comma-separated-values",
                                                                "text/plain",
                                                                "*/*"
                                                            )
                                                        )
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("تصدير إلى إكسل (XLSX)", fontWeight = FontWeight.Bold) },
                                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = PrimaryTeal) },
                                                    onClick = {
                                                        showCustomerMenu = false
                                                        customersViewModel.exportCustomersToExcel(context)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("تصدير كملف CSV", fontWeight = FontWeight.Bold) },
                                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryTeal) },
                                                    onClick = {
                                                        showCustomerMenu = false
                                                        customersViewModel.exportCustomersToCsv(context)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("طباعة كشف الحسابات (PDF)", fontWeight = FontWeight.Bold) },
                                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentGold) },
                                                    onClick = {
                                                        showCustomerMenu = false
                                                        customersViewModel.generateAllCustomersReportPdf()
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // زر الفيدباك والآراء السريع
                                    IconButton(onClick = { showFeedbackDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "شاركنا رأيك",
                                            tint = AccentGold,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // أيقونة خط خارجي دائرية بعدد العمليات المتبقية يتغير لونها مع النفاذ
                                    val customDevLimit = com.example.core.license.RemoteConfigManager.getCustomDeviceLimit(context)
                                    if (!isActivated || customDevLimit > 0) {
                                        val freeLimit = LicenseManager.getFreeOperationsLimit(context)
                                        val remaining = LicenseManager.getRemainingOperations(context, operationsCount)
                                        val progress = if (freeLimit > 0) (operationsCount.toFloat() / freeLimit).coerceIn(0f, 1f) else 1f
                                        val indicatorColor = when {
                                            remaining <= 10 -> Color(0xFFE53935)
                                            remaining <= 40 -> Color(0xFFF57C00)
                                            else -> PrimaryTeal
                                        }

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .clickable { showLicenseDialog = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { 1f },
                                                modifier = Modifier.size(32.dp),
                                                color = indicatorColor.copy(alpha = 0.2f),
                                                strokeWidth = 2.5.dp
                                            )
                                            CircularProgressIndicator(
                                                progress = { 1f - progress },
                                                modifier = Modifier.size(32.dp),
                                                color = indicatorColor,
                                                strokeWidth = 2.5.dp
                                            )
                                            Text(
                                                text = "$remaining",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.5.sp,
                                                    color = indicatorColor
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        MosaribNavBar(
                            selectedIndex = pagerState.currentPage,
                            onItemSelected = { idx ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(idx)
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (tabs[page]) {
                                AppTab.HOME -> {
                                    HomeScreen(
                                        sessionsViewModel = sessionsViewModel,
                                        customersViewModel = customersViewModel,
                                        vouchersViewModel = vouchersViewModel,
                                        settingsViewModel = settingsViewModel,
                                        onNavigateToTab = { tab ->
                                            val targetIdx = tabs.indexOf(tab)
                                            if (targetIdx >= 0) {
                                                coroutineScope.launch { pagerState.animateScrollToPage(targetIdx) }
                                            }
                                        },
                                        onNavigateToCustomer = { custId ->
                                            selectedCustomerId = custId
                                        },
                                        onOpenReports = {
                                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                        }
                                    )
                                }
                                AppTab.REPORTS -> {
                                    ReportsScreen(viewModel = reportsViewModel)
                                }
                                AppTab.CUSTOMERS -> {
                                    CustomersScreen(
                                        viewModel = customersViewModel,
                                        sessionsViewModel = sessionsViewModel,
                                        vouchersViewModel = vouchersViewModel,
                                        onNavigateToDetail = { custId ->
                                            selectedCustomerId = custId
                                        }
                                    )
                                }
                                AppTab.VOUCHERS -> {
                                    VouchersScreen(
                                        viewModel = vouchersViewModel,
                                        sessionsViewModel = sessionsViewModel,
                                        onNavigateToCustomer = { custId ->
                                            selectedCustomerId = custId
                                        }
                                    )
                                }
                                AppTab.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = settingsViewModel,
                                        onNavigateToAbout = { showAboutScreen = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val isTrialLimitExceeded = LicenseManager.isTrialLimitExceeded(context, operationsCount)

            if (showLicenseDialog || isTrialLimitExceeded) {
                LicenseDialog(
                    onDismiss = {
                        if (!isTrialLimitExceeded) showLicenseDialog = false
                    },
                    onActivated = {
                        settingsViewModel.refreshActivationStatus()
                        showLicenseDialog = false
                    },
                    isMandatory = isTrialLimitExceeded,
                    mandatoryReason = if (isTrialLimitExceeded) {
                        "لقد تجاوزت الحد التجريبي المجاني (${LicenseManager.getFreeOperationsLimit(context)} عملية) أو قمت باستعادة نسخة تتجاوزه. تم حفظ بياناتك بأمان تام، ويرجى تفعيل الاشتراك لمتابعة العمل بالتطبيق."
                    } else null
                )
            }

            if (showFeedbackDialog) {
                FeedbackDialog(
                    onDismiss = { showFeedbackDialog = false }
                )
            }

            // نافذة الإعلان / التحديث الاختياري
            if (showAnnouncementDialog && activeBroadcast != null) {
                BroadcastAnnouncementDialog(
                    broadcast = activeBroadcast!!,
                    onDismiss = {
                        AppUpdateManager.markBroadcastSeen(context, activeBroadcast!!.id)
                        showAnnouncementDialog = false
                    }
                )
            }

            // نافذة التذكير بالنسخ الاحتياطي
            if (showBackupReminderDialog) {
                BackupReminderDialog(
                    onBackupNow = {
                        showBackupReminderDialog = false
                        PeriodicBackupReminderManager.recordBackupPerformed(context)
                        // التنقل لتبويب الإعدادات لإجراء النسخ
                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                    },
                    onRemindLater = {
                        showBackupReminderDialog = false
                        PeriodicBackupReminderManager.dismissReminderTemporarily(context)
                    }
                )
            }

            if (showSpecialWarningDialog && specialDeviceWarning != null) {
                BroadcastAnnouncementDialog(
                    broadcast = AppBroadcast(
                        id = "SPECIAL_DEVICE_WARN",
                        title = "تنبيه وتوجيه فني خاص بهذا الجهاز",
                        message = specialDeviceWarning!!,
                        type = "WARNING",
                        actionText = "حسناً، فهمت ذلك",
                        actionUrl = "",
                        publishedAt = System.currentTimeMillis()
                    ),
                    onDismiss = { showSpecialWarningDialog = false }
                )
            }

            if (showExitConfirmationDialog) {
                val context = LocalContext.current
                val activity = context as? Activity
                com.example.core.ui.MosaribExitConfirmDialog(
                    onConfirmExit = {
                        showExitConfirmationDialog = false
                        activity?.finishAffinity()
                    },
                    onDismiss = {
                        showExitConfirmationDialog = false
                    }
                )
            }
        }
    }
}

package com.example

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.features.sessions.SessionsScreen
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
    REPORTS("التقارير", Icons.Filled.Assessment, Icons.Default.Assessment, "tab_reports"),
    CUSTOMERS("العملاء", Icons.Filled.People, Icons.Outlined.People, "tab_customers"),
    HOME("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    VOUCHERS("سجل العمليات", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "tab_vouchers"),
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
            var showExitConfirmationDialog by remember { mutableStateOf(false) }

            // اعتراض زر التراجع لإظهار رسالة تأكيد الخروج من أي شاشة يؤدي التراجع منها للخروج من التطبيق
            val shouldInterceptExit = !showSplashScreen && !showOnboarding && !showAboutScreen && selectedCustomerId == 0L
            BackHandler(enabled = shouldInterceptExit) {
                showExitConfirmationDialog = true
            }

            val isActivated by settingsViewModel.isActivated.collectAsStateWithLifecycle()
            val operationsCount by settingsViewModel.operationsCount.collectAsStateWithLifecycle()

            var isDeviceBlocked by remember { mutableStateOf(UserDeviceTracker.isDeviceBlockedLocally(context)) }
            var blockReason by remember { mutableStateOf(UserDeviceTracker.getBlockReason(context)) }

            // الإعلانات والتحديثات الإجبارية
            var activeBroadcast by remember { mutableStateOf<AppBroadcast?>(null) }
            var showAnnouncementDialog by remember { mutableStateOf(false) }
            var showBackupReminderDialog by remember { mutableStateOf(false) }

            // التحقق السحابي في الخلفية للتأكد من عدم سحب الاشتراك أو حظره ومزامنة تواجد المستخدم وفحص الإعلانات
            LaunchedEffect(operationsCount) {
                val syncResult = UserDeviceTracker.syncPresence(context, operationsCount)
                if (syncResult.isBlocked) {
                    isDeviceBlocked = true
                    blockReason = syncResult.blockReason ?: "تم إيقاف التطبيق على هذا الجهاز"
                }
                LicenseManager.checkRemoteRevocationInBackground(context)

                // فحص الإعلانات والتحديث الإجباري
                val broadcast = AppUpdateManager.checkBroadcastAndUpdates(context)
                if (broadcast != null) {
                    activeBroadcast = broadcast
                    if (!broadcast.isForceUpdate && !AppUpdateManager.isBroadcastSeen(context, broadcast.id)) {
                        showAnnouncementDialog = true
                    }
                }

                // فحص التذكير الدوري بالنسخ الاحتياطي
                if (PeriodicBackupReminderManager.shouldShowBackupReminder(context, operationsCount)) {
                    showBackupReminderDialog = true
                }

                // مزامنة FCM Push Token للإشعارات المنبثقة
                try {
                    com.example.core.notifications.FcmTokenHelper.syncCurrentToken(context)
                } catch (_: Exception) {}
            }

            val tabs = remember { listOf(AppTab.REPORTS, AppTab.CUSTOMERS, AppTab.HOME, AppTab.VOUCHERS, AppTab.SETTINGS) }
            val pagerState = rememberPagerState(initialPage = 2) { tabs.size }
            val coroutineScope = rememberCoroutineScope()
            val currentTab = tabs[pagerState.currentPage]

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
                    onBack = { selectedCustomerId = 0L },
                    onAddSessionForCustomer = { customer ->
                        sessionsViewModel.requestNewSessionForCustomer(customer.id)
                        selectedCustomerId = 0L
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        if (currentTab != AppTab.HOME) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (currentTab) {
                                            AppTab.REPORTS -> "التقارير والإحصائيات"
                                            AppTab.CUSTOMERS -> "إدارة العملاء والمزارع"
                                            AppTab.VOUCHERS -> "سجل العمليات"
                                            AppTab.SETTINGS -> "الإعدادات العامة"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                actions = {
                                    if (currentTab == AppTab.REPORTS) {
                                        androidx.compose.material3.Button(
                                            onClick = { reportsViewModel.exportComprehensiveReportPdf() },
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentGold),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تصدير PDF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    if (!isActivated) {
                                        val remaining = (LicenseManager.FREE_OPERATIONS_LIMIT - operationsCount).coerceAtLeast(0)
                                        val progress = (operationsCount.toFloat() / LicenseManager.FREE_OPERATIONS_LIMIT).coerceIn(0f, 1f)
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

            if (showLicenseDialog) {
                LicenseDialog(
                    onDismiss = { showLicenseDialog = false },
                    onActivated = { showLicenseDialog = false }
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

            if (showExitConfirmationDialog) {
                val context = LocalContext.current
                val activity = context as? Activity
                ExitConfirmationDialog(
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

@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "تأكيد الخروج",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "هل أنت متأكد من رغبتك في إغلاق والخروج من تطبيق المُسَرِّب؟",
                style = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("خروج", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("إلغاء", fontWeight = FontWeight.SemiBold)
            }
        },
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

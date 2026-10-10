package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
            var showSplashScreen by remember { mutableStateOf(true) }
            var isBiometricUnlocked by remember { mutableStateOf(false) }
            var selectedTab by remember { mutableStateOf(AppTab.HOME) }
            var selectedCustomerId by remember { mutableLongStateOf(0L) }
            var showAboutScreen by remember { mutableStateOf(false) }

            if (showSplashScreen) {
                SplashScreen(onTimeout = { showSplashScreen = false })
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
                        selectedTab = AppTab.VOUCHERS
                    }
                )
            } else {
                Scaffold(
                    topBar = {
                        if (selectedTab != AppTab.HOME) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (selectedTab) {
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
                                    if (selectedTab == AppTab.REPORTS) {
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
                                }
                            )
                        }
                    },
                    bottomBar = {
                        val tabIndex = when (selectedTab) {
                            AppTab.REPORTS   -> 0
                            AppTab.CUSTOMERS -> 1
                            AppTab.HOME      -> 2
                            AppTab.VOUCHERS  -> 3
                            AppTab.SETTINGS  -> 4
                        }
                        MosaribNavBar(
                            selectedIndex = tabIndex,
                            onItemSelected = { idx ->
                                selectedTab = when (idx) {
                                    0 -> AppTab.REPORTS
                                    1 -> AppTab.CUSTOMERS
                                    3 -> AppTab.VOUCHERS
                                    4 -> AppTab.SETTINGS
                                    else -> AppTab.HOME
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
                        when (selectedTab) {
                            AppTab.HOME -> {
                                HomeScreen(
                                    sessionsViewModel = sessionsViewModel,
                                    customersViewModel = customersViewModel,
                                    vouchersViewModel = vouchersViewModel,
                                    settingsViewModel = settingsViewModel,
                                    onNavigateToTab = { tab -> selectedTab = tab },
                                    onNavigateToCustomer = { custId ->
                                        selectedCustomerId = custId
                                    },
                                    onOpenReports = { selectedTab = AppTab.REPORTS }
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
    }
}

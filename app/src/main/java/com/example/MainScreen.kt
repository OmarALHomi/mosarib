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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.example.features.customers.CustomerDetailScreen
import com.example.features.customers.CustomersScreen
import com.example.features.customers.CustomersViewModel
import com.example.features.home.HomeScreen
import com.example.features.reports.ReportsScreen
import com.example.features.reports.ReportsViewModel
import com.example.features.sessions.SessionsScreen
import com.example.features.sessions.SessionsViewModel
import com.example.features.settings.SettingsScreen
import com.example.features.settings.SettingsViewModel
import com.example.features.vouchers.VouchersScreen
import com.example.features.vouchers.VouchersViewModel
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.WaterDistributorTheme

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    SESSIONS("سجلات السقي", Icons.Filled.WaterDrop, Icons.Outlined.WaterDrop, "tab_sessions"),
    CUSTOMERS("العملاء", Icons.Filled.People, Icons.Outlined.People, "tab_customers"),
    VOUCHERS("المالية", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong, "tab_vouchers"),
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

    WaterDistributorTheme(darkTheme = darkTheme) {
        // Arabic RTL layout provider
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            var selectedTab by remember { mutableStateOf(AppTab.HOME) }
            var selectedCustomerId by remember { mutableLongStateOf(0L) }
            var showReportsScreen by remember { mutableStateOf(false) }

            if (selectedCustomerId > 0) {
                CustomerDetailScreen(
                    customerId = selectedCustomerId,
                    viewModel = customersViewModel,
                    onBack = { selectedCustomerId = 0L },
                    onAddSessionForCustomer = { _ ->
                        selectedCustomerId = 0L
                        selectedTab = AppTab.SESSIONS
                    }
                )
            } else if (showReportsScreen) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("التقارير والإحصائيات", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { showReportsScreen = false }) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        ReportsScreen(viewModel = reportsViewModel)
                    }
                }
            } else {
                Scaffold(
                    topBar = {
                        if (selectedTab != AppTab.HOME) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = when (selectedTab) {
                                            AppTab.SESSIONS -> "سجلات السقي"
                                            AppTab.CUSTOMERS -> "إدارة العملاء والمزارع"
                                            AppTab.VOUCHERS -> "المالية والمصروفات"
                                            AppTab.SETTINGS -> "الإعدادات العامة"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                actions = {
                                    if (selectedTab == AppTab.SESSIONS || selectedTab == AppTab.VOUCHERS) {
                                        IconButton(
                                            onClick = { showReportsScreen = true },
                                            modifier = Modifier.testTag("action_open_reports")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Assessment,
                                                contentDescription = "التقارير",
                                                tint = PrimaryTeal
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            AppTab.entries.forEach { tab ->
                                val isSelected = selectedTab == tab
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { selectedTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color.White,
                                        indicatorColor = PrimaryTeal,
                                        selectedTextColor = PrimaryTeal,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
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
                                    onOpenReports = { showReportsScreen = true }
                                )
                            }
                            AppTab.SESSIONS -> {
                                SessionsScreen(
                                    viewModel = sessionsViewModel,
                                    onNavigateToCustomer = { custId ->
                                        selectedCustomerId = custId
                                    }
                                )
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
                                VouchersScreen(viewModel = vouchersViewModel)
                            }
                            AppTab.SETTINGS -> {
                                SettingsScreen(viewModel = settingsViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

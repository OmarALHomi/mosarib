package com.baynana

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.baynana.core.database.AppDatabase
import com.baynana.core.security.BiometricLockScreen
import com.baynana.features.about.AboutScreen
import com.baynana.features.deals.DealsScreen
import com.baynana.features.deals.DealsViewModel
import com.baynana.features.farmer.FarmAccountingScreen
import com.baynana.features.reports.ReportsScreen
import com.baynana.features.reports.ReportsViewModel
import com.baynana.features.settings.SettingsScreen
import com.baynana.features.home.BaynanaHomeScreen
import com.baynana.features.home.BaynanaHomeViewModel
import com.baynana.features.home.MigrationDialog
import com.baynana.features.market.BaynanaMarketScreen
import com.baynana.features.market.BaynanaMarketViewModel
import com.baynana.features.more.MoreScreen
import com.baynana.features.rooms.RoomDetailScreen
import com.baynana.features.rooms.RoomsScreen
import com.baynana.features.settings.SettingsViewModel
import com.baynana.features.shell.BaynanaShell
import com.baynana.features.statements.StatementScreen
import com.baynana.features.sync.SyncStatusScreen
import com.baynana.features.shell.ShellHost
import com.baynana.features.splash.SplashScreen
import com.baynana.ui.theme.BaynanaTheme

/**
 * هيكل التطبيق الجديد: أربعة تبويبات ثابتة + «المزيد».
 *
 * الفرق عن الهيكل القديم: لا «سجلات سقي» ولا «عملاء» ولا «سجل عمليات»؛ فالمستخدم يبدأ من
 * **الرصيد** لا من النموذج. والتبويبات تُقرأ من [BaynanaShell] فيبقى المنطق قابلًا للاختبار.
 */
@Composable
fun MainApp(
    settingsViewModel: SettingsViewModel = viewModel(),
    homeViewModel: BaynanaHomeViewModel = viewModel()
) {
    val appConfig by settingsViewModel.appConfig.collectAsStateWithLifecycle()
    val biometricLockEnabled by settingsViewModel.biometricLockEnabled.collectAsStateWithLifecycle()
    val homeState by homeViewModel.state.collectAsStateWithLifecycle()

    val darkTheme = when (appConfig.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    BaynanaTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            var showSplash by remember { mutableStateOf(true) }
            var isUnlocked by remember { mutableStateOf(false) }
            var shell by remember { mutableStateOf(BaynanaShell.home()) }
            var showMigration by remember { mutableStateOf(false) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) isUnlocked = false
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            when {
                showSplash -> SplashScreen(onTimeout = { showSplash = false })

                biometricLockEnabled == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            "جاري تحضير دفترك",
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                biometricLockEnabled == true && !isUnlocked ->
                    BiometricLockScreen(onUnlock = { isUnlocked = true })

                else -> ShellScaffold(
                    shell = shell,
                    onNavigate = { shell = it },
                    homeState = homeState,
                    onOpenRoom = { roomId -> shell = BaynanaShell.room(roomId) },
                    onRefresh = homeViewModel::refresh,
                    onRunMigration = homeViewModel::runMigration,
                    showMigrationDialog = showMigration,
                    onOpenMigrationDetail = { showMigration = true },
                    onCloseMigrationDetail = { showMigration = false }
                )
            }
        }
    }
}

@Composable
private fun ShellScaffold(
    shell: BaynanaShell,
    onNavigate: (BaynanaShell) -> Unit,
    homeState: BaynanaHomeViewModel.UiState,
    onOpenRoom: (String) -> Unit,
    onRefresh: () -> Unit,
    onRunMigration: () -> Unit,
    showMigrationDialog: Boolean,
    onOpenMigrationDetail: () -> Unit,
    onCloseMigrationDetail: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                ShellTab.entries.forEach { tab ->
                    val selected = shell.tab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigate(ShellHost.navigate(tab)) },
                        icon = {
                            Icon(
                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val host = shell) {
                is BaynanaShell.Room -> {
                    BackHandler { onNavigate(BaynanaShell.rooms()) }
                    RoomDetailScreen(
                        roomId = host.roomId,
                        onBack = { onNavigate(BaynanaShell.rooms()) },
                        onChanged = onRefresh,
                        onOpenStatement = { roomId -> onNavigate(BaynanaShell.statement(roomId)) }
                    )
                }

                is BaynanaShell.Sub -> {
                    val backToParent = { onNavigate(BaynanaShell.Tab(host.parent)) }
                    BackHandler { backToParent() }
                    when (host.key) {
                        // كشف الطرف: أسطر زمنية برصيد جارٍ، ومشاركة من نفس نصّ الشاشة.
                        BaynanaShell.KEY_STATEMENT -> {
                            val roomId = host.arg.orEmpty()
                            if (roomId.isBlank()) {
                                backToParent()
                            } else {
                                StatementScreen(roomId = roomId, onBack = backToParent)
                            }
                        }

                        // قنوات فرعية تحت السوق والأدوار تُبنى هنا لاحقًا (د٦)،
                        // وحتى ذلك الحين لا يفتح مسارٌ شاشةً غير موجودة.
                        else -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("شاشة غير معروفة: ${host.key}") }
                    }
                }

                is BaynanaShell.Extra -> {
                    val backToMore = { onNavigate(ShellHost.back(host) ?: BaynanaShell.home()) }
                    BackHandler { backToMore() }
                    when (host.key) {
                        "deals" -> DealsScreen(
                            viewModel = viewModel<DealsViewModel>(),
                            onBack = backToMore
                        )

                        "farm" -> {
                            val context = LocalContext.current
                            val expensesDao = remember { AppDatabase.getDatabase(context).farmExpenseDao() }
                            FarmAccountingScreen(expensesDao = expensesDao, onBack = backToMore)
                        }

                        "reports" -> ReportsScreen(viewModel = viewModel<ReportsViewModel>())

                        "settings" -> SettingsScreen(
                            viewModel = viewModel(),
                            onNavigateToAbout = { onNavigate(BaynanaShell.extra("about")) }
                        )

                        "about" -> AboutScreen(onBack = backToMore)

                        // حالة المزامنة: ما حال كل حركة وما العمل فيها (لا نجاح كاذب).
                        "sync" -> SyncStatusScreen(onBack = backToMore)

                        // نقل الدفتر: ملفّ ترحيل + جرد يمنع «نجاحًا» ينقصه قيد (ح٢٣).
                        "transfer" -> com.baynana.features.migration.MigrationScreen(onBack = backToMore)

                        // التسليم بلا إنترنت: حزمة نصّية تُمرَّر في واتساب أو ملفًّا (ح١٩).
                        "handover" -> com.baynana.features.handover.HandoverScreen(onBack = backToMore)

                        // التحديث (ح٢٠): رقم النسخة يأتي من البناء نفسه، فلا رقم مكتوب مرتين.
                        "update" -> com.baynana.features.update.UpdateScreen(
                            onBack = backToMore,
                            currentVersionCode = BuildConfig.VERSION_CODE,
                            currentVersionName = BuildConfig.VERSION_NAME
                        )

                        "catalog" -> if (BuildConfig.DEBUG) {
                            com.baynana.features.dev.ComponentCatalogScreen(onBack = backToMore)
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("هذه الشاشة للتطوير فقط") }
                        }

                        else -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("شاشة غير معروفة: ${host.key}") }
                    }
                }

                else -> when (shell.tab) {
                    ShellTab.HOME -> BaynanaHomeScreen(
                        state = homeState,
                        onRefresh = onRefresh,
                        onOpenRoom = onOpenRoom,
                        onRunMigration = onRunMigration,
                        onOpenMigrationDetail = onOpenMigrationDetail,
                        onOpenTab = { onNavigate(ShellHost.navigate(it)) },
                        onOpenSyncStatus = { onNavigate(BaynanaShell.extra("sync")) }
                    )

                    ShellTab.ROOMS -> RoomsScreen(
                        state = homeState,
                        onOpenRoom = onOpenRoom,
                        onRefresh = onRefresh
                    )

                    ShellTab.MOVEMENTS -> com.baynana.features.movements.MovementsScreen(
                        state = homeState,
                        onOpenRoom = onOpenRoom,
                        onOpenSyncStatus = { onNavigate(BaynanaShell.extra("sync")) }
                    )

                    ShellTab.MARKET -> BaynanaMarketScreen(
                        viewModel = viewModel<BaynanaMarketViewModel>()
                    )

                    ShellTab.MORE -> MoreScreen(
                        onOpenMigration = onOpenMigrationDetail,
                        onOpenDeals = { onNavigate(BaynanaShell.extra("deals")) },
                        onOpenFarmAccounting = { onNavigate(BaynanaShell.extra("farm")) },
                        onOpenReports = { onNavigate(BaynanaShell.extra("reports")) },
                        onOpenSettings = { onNavigate(BaynanaShell.extra("settings")) },
                        onOpenAbout = { onNavigate(BaynanaShell.extra("about")) },
                        onOpenSyncStatus = { onNavigate(BaynanaShell.extra("sync")) },
                        onOpenTransfer = { onNavigate(BaynanaShell.extra("transfer")) },
                        onOpenHandover = { onNavigate(BaynanaShell.extra("handover")) },
                        onOpenUpdate = { onNavigate(BaynanaShell.extra("update")) },
                        // شاشة العيّنات للتطوير فقط: في نسخة التوزيع لا يظهر العنصر ولا الشاشة.
                        onOpenCatalog = if (BuildConfig.DEBUG) {
                            { onNavigate(BaynanaShell.extra("catalog")) }
                        } else {
                            null
                        }
                    )
                }
            }
        }

        val plan = homeState.migration.plan
        if (showMigrationDialog && plan != null) {
            MigrationDialog(
                plan = plan,
                running = homeState.migration.running,
                outcome = homeState.migration.outcome,
                error = homeState.migration.error,
                onDismiss = onCloseMigrationDetail,
                onApply = onRunMigration
            )
        }
    }
}

/** تبويبات الشريط السفلي: أربعة ثابتة والمزيد. */
enum class ShellTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home),
    ROOMS("غرفي", Icons.Filled.SupervisorAccount, Icons.Outlined.SupervisorAccount),
    MOVEMENTS("الحركات", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    MARKET("السوق", Icons.Filled.Storefront, Icons.Outlined.Storefront),
    MORE("المزيد", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
}

package com.example.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppTab
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.Formatters
import com.example.features.customers.AddEditCustomerBottomSheet
import com.example.features.customers.Customer
import com.example.features.customers.CustomersViewModel
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.LiveTimerRunningBanner
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.StartLiveTimerBottomSheet
import com.example.features.sessions.StopLiveTimerBottomSheet
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.features.settings.SettingsViewModel
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersViewModel
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import java.util.Calendar

data class DayWaterStat(
    val dayName: String,
    val dayDateText: String,
    val totalMinutes: Int,
    val isToday: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sessionsViewModel: SessionsViewModel,
    customersViewModel: CustomersViewModel,
    vouchersViewModel: VouchersViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToTab: (AppTab) -> Unit,
    onNavigateToCustomer: (Long) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by settingsViewModel.appConfig.collectAsStateWithLifecycle()
    val homeStats by sessionsViewModel.homeStats.collectAsStateWithLifecycle()
    val liveTimerState by sessionsViewModel.liveTimerState.collectAsStateWithLifecycle()
    val sessions by sessionsViewModel.filteredSessions.collectAsStateWithLifecycle()
    val customers by sessionsViewModel.customers.collectAsStateWithLifecycle()
    val pumps by sessionsViewModel.pumps.collectAsStateWithLifecycle()
    val toast by sessionsViewModel.toast.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (config.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    var showActionChoiceSheet by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var showStartLiveSheet by remember { mutableStateOf(false) }
    var showStopLiveSheet by remember { mutableStateOf(false) }
    var showAddVoucherSheet by remember { mutableStateOf(false) }
    var voucherInitialType by remember { mutableStateOf(VoucherType.RECEIPT) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. Header Branding & Animated Living Theme Toggle Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(PrimaryTeal, AccentEmerald)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "المُسَرِّبْ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Almosarib",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "لوحة التحكم وإدارة مياه الآبار والسقي",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    // Living Breathing Theme Toggle Button
                    LivingThemeToggleButton(
                        isDarkTheme = isDarkTheme,
                        onToggle = {
                            val nextMode = if (isDarkTheme) "LIGHT" else "DARK"
                            settingsViewModel.updateThemeMode(nextMode)
                        }
                    )
                }
            }

            // 2. Active Live Timer Banner (if irrigation is currently running)
            if (liveTimerState.isRunning) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        LiveTimerRunningBanner(
                            state = liveTimerState,
                            currencySymbol = config.currencySymbol,
                            onStopClick = { showStopLiveSheet = true },
                            onCancelClick = { sessionsViewModel.cancelLiveSession() }
                        )
                    }
                }
            }

            // 3. Section Title: Quick Operations
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "العمليات السريعة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "اختر عملية للتنفيذ الفوري",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // 4. Quick Action Buttons Grid (2 columns x 3 rows)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: سقي جديد | قبض دفعة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "سقي جديد",
                            subtitle = "بدء عداد أو ري يدوي",
                            icon = Icons.Default.WaterDrop,
                            iconBgColor = Color(0xFFE0F2F1),
                            iconTint = PrimaryTeal,
                            onClick = { showActionChoiceSheet = true },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            title = "قبض دفعة",
                            subtitle = "تحصيل وسداد حساب",
                            icon = Icons.Default.Payments,
                            iconBgColor = Color(0xFFE8F5E9),
                            iconTint = AccentEmerald,
                            onClick = {
                                voucherInitialType = VoucherType.RECEIPT
                                showAddVoucherSheet = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: عميل جديد | سجلات السقي
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "عميل جديد",
                            subtitle = "تسجيل مزارع وأرض",
                            icon = Icons.Default.PersonAdd,
                            iconBgColor = Color(0xFFE0F2FE),
                            iconTint = Color(0xFF0284C7),
                            onClick = { showAddCustomerSheet = true },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            title = "سجلات السقي",
                            subtitle = "مراجعة الدورات والفواتير",
                            icon = Icons.Default.History,
                            iconBgColor = Color(0xFFFEF3C7),
                            iconTint = Color(0xFFD97706),
                            onClick = { onNavigateToTab(AppTab.SESSIONS) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 3: مصروف تشغيلي | التقارير والإحصائيات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "مصروف تشغيلي",
                            subtitle = "ديزل، زيت، صيانة",
                            icon = Icons.Default.LocalGasStation,
                            iconBgColor = Color(0xFFFFEBEE),
                            iconTint = Color(0xFFE53935),
                            onClick = {
                                voucherInitialType = VoucherType.EXPENSE
                                showAddVoucherSheet = true
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            title = "التقارير",
                            subtitle = "كشوفات الحساب وملخص الأداء",
                            icon = Icons.Default.Assessment,
                            iconBgColor = Color(0xFFF3E8FF),
                            iconTint = Color(0xFF7C3AED),
                            onClick = onOpenReports,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Section: Visual Reports & Charts (رسوميات بيانية كتقارير مميزة)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التقارير الرسومية والتحليلية",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "مؤشرات حية للأداء",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            // 6. Graphical Chart 1: Weekly Water Hours Distribution Chart
            item {
                WeeklyIrrigationBarChartCard(sessions = sessions)
            }

            // 7. Graphical Chart 2: Financial Liquidity & Collection Gauge
            item {
                FinancialRecoveryGaugeCard(
                    homeStats = homeStats,
                    currencySymbol = config.currencySymbol
                )
            }

            // 8. Recent Operations Section (آخر دورات السقي المسجلة)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر دورات السقي المسجلة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "عرض الكل →",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { onNavigateToTab(AppTab.SESSIONS) }
                    )
                }
            }

            // Latest 3 Sessions Preview
            val recentSessions = sessions.take(3)
            if (recentSessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد دورات سقي مسجلة حتى الآن",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                            )
                        }
                    }
                }
            } else {
                items(recentSessions, key = { it.session.id }) { item ->
                    RecentSessionMiniCard(
                        sessionWithCustomer = item,
                        currencySymbol = config.currencySymbol,
                        onClick = { onNavigateToCustomer(item.session.customerId) }
                    )
                }
            }
        }

        // Floating Toast Notification
        LuxuryToastNotification(
            toast = toast,
            onDismiss = { sessionsViewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        )
    }

    // Sheet: Action Choice (سقي مباشر بالعداد أم يدوي)
    if (showActionChoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionChoiceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تسجيل دورة سقي جديدة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = { showActionChoiceSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 1: Live Timer
                Card(
                    onClick = {
                        showActionChoiceSheet = false
                        showStartLiveSheet = true
                    },
                    colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("بدء عداد ري مباشر (الآن)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentEmerald))
                            Text("تشغيل مؤقت يحسب الوقت والمبلغ لحظة بلحظة", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Option 2: Manual Recording
                Card(
                    onClick = {
                        showActionChoiceSheet = false
                        showAddManualSheet = true
                    },
                    colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("تسجيل يدوي (ساعات سابقة)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal))
                            Text("تسجيل سقي سابق وتحديد الساعات والمبلغ", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Sheet: Add Manual Session
    if (showAddManualSheet) {
        AddEditSessionBottomSheet(
            initialSession = null,
            customers = customers,
            pumps = pumps,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddManualSheet = false },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes ->
                sessionsViewModel.saveManualSession(id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes)
            }
        )
    }

    // Sheet: Start Live Timer
    if (showStartLiveSheet) {
        StartLiveTimerBottomSheet(
            customers = customers,
            pumps = pumps,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onDismiss = { showStartLiveSheet = false },
            onStart = { custId, custName, pumpName, rate ->
                sessionsViewModel.startLiveSession(custId, custName, pumpName, rate)
            }
        )
    }

    // Sheet: Stop Live Timer
    if (showStopLiveSheet) {
        StopLiveTimerBottomSheet(
            liveState = liveTimerState,
            currencySymbol = config.currencySymbol,
            onDismiss = { showStopLiveSheet = false },
            onConfirmStop = { paid, notes ->
                sessionsViewModel.stopAndSaveLiveSession(paid, notes)
            }
        )
    }

    // Sheet: Add Voucher (سند قبض أو مصروف)
    if (showAddVoucherSheet) {
        AddVoucherBottomSheet(
            customers = customers,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddVoucherSheet = false },
            onSave = { type, customerId, amount, category, method, notes ->
                vouchersViewModel.addVoucher(
                    type = type,
                    customerId = customerId,
                    amount = amount,
                    category = category,
                    paymentMethod = method,
                    notes = notes
                )
            }
        )
    }

    // Sheet: Add Customer
    if (showAddCustomerSheet) {
        AddEditCustomerBottomSheet(
            initialCustomer = null,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddCustomerSheet = false },
            onSave = { id, name, phone, farm, loc, notes, customPrice ->
                customersViewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice)
            }
        )
    }
}

/**
 * زر صغير نابض بالحياة لتغيير الثيم مع هالة ضوئية تفاعلية
 */
@Composable
fun LivingThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_theme")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(46.dp),
        contentAlignment = Alignment.Center
    ) {
        // Living breathing halo ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(haloScale)
                .clip(CircleShape)
                .background(
                    if (isDarkTheme) AccentGold.copy(alpha = haloAlpha)
                    else PrimaryTeal.copy(alpha = haloAlpha)
                )
        )

        // Main button pill
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isDarkTheme) Color(0xFF1E293B)
                    else Color(0xFFE0F2F1)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "تبديل المظهر",
                tint = if (isDarkTheme) AccentGold else PrimaryTeal,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(rotationZ = rotation)
            )
        }
    }
}

/**
 * بطاقة إجراء سريع مدمجة وأنيقة
 */
@Composable
fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.5.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * رسم بياني بالأعمدة لتوزيع ساعات الري الأسبوعية (Weekly Irrigation Hours Bar Chart)
 */
@Composable
fun WeeklyIrrigationBarChartCard(
    sessions: List<WaterSessionWithCustomer>,
    modifier: Modifier = Modifier
) {
    // حساب ساعات الري لآخر 7 أيام
    val daysStats = remember(sessions) {
        val now = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 86400000L - 1L

            val dayMinutes = sessions.filter {
                it.session.startTime in startOfDay..endOfDay
            }.sumOf { it.session.durationMinutes }

            val dayName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "السبت"
                Calendar.SUNDAY -> "الأحد"
                Calendar.MONDAY -> "الاثنين"
                Calendar.TUESDAY -> "الثلاثاء"
                Calendar.WEDNESDAY -> "الأربعاء"
                Calendar.THURSDAY -> "الخميس"
                Calendar.FRIDAY -> "الجمعة"
                else -> ""
            }

            DayWaterStat(
                dayName = dayName,
                dayDateText = "${cal.get(Calendar.DAY_OF_MONTH)}/${cal.get(Calendar.MONTH) + 1}",
                totalMinutes = dayMinutes,
                isToday = daysAgo == 0
            )
        }
    }

    val maxMinutes = (daysStats.maxOfOrNull { it.totalMinutes } ?: 0).coerceAtLeast(60)
    val totalWeekMinutes = daysStats.sumOf { it.totalMinutes }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مخطط سقي الأسبوع (بالساعات)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = "إجمالي السقي: ${Formatters.formatDurationArabic(totalWeekMinutes)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "آخر 7 أيام",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bars Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                daysStats.forEach { stat ->
                    val fraction = (stat.totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.04f, 1f)
                    val hoursText = if (stat.totalMinutes == 0) "0" else String.format(java.util.Locale.US, "%.1f", stat.totalMinutes / 60.0)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$hoursText س",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = if (stat.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (stat.isToday) AccentEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Animated Bar
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((100 * fraction).dp)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (stat.isToday) Brush.verticalGradient(listOf(AccentEmerald, PrimaryTeal))
                                    else if (stat.totalMinutes > 0) Brush.verticalGradient(listOf(PrimaryTeal.copy(alpha = 0.85f), SecondaryAqua))
                                    else Brush.verticalGradient(listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
                                )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stat.dayName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (stat.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (stat.isToday) PrimaryTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * مؤشر رسومي للسيولة والتحصيل المالي (Financial Recovery & Debt Gauge)
 */
@Composable
fun FinancialRecoveryGaugeCard(
    homeStats: com.example.features.sessions.HomeDashboardStats,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val totalTurnover = homeStats.totalCollectedCash + homeStats.totalOutstandingDebt
    val recoveryRatio = if (totalTurnover > 0) (homeStats.totalCollectedCash / totalTurnover).toFloat().coerceIn(0f, 1f) else 1f
    val percentInt = (recoveryRatio * 100).toInt()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "مؤشر التحصيل والسيولة المالية",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    )
                    Text(
                        text = "نسبة الدفعات المستلمة مقارنة بالديون",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (percentInt >= 70) Color(0xFFE8F5E9) else Color(0xFFFFF3E0))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$percentInt% محصل",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (percentInt >= 70) Color(0xFF2E7D32) else Color(0xFFE65100),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Graphical Segmented Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(recoveryRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentEmerald, PrimaryTeal)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2 Stat Columns: Collected vs Debts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "المقبوض كاش",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(homeStats.totalCollectedCash, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = AccentEmerald,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ديون متبقية بالذمة",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(homeStats.totalOutstandingDebt, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = if (homeStats.totalOutstandingDebt > 0) Color(0xFFB71C1C) else AccentEmerald,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * بطاقة معاينة مدمجة وسريعة لأحدث عمليات السقي
 */
@Composable
fun RecentSessionMiniCard(
    sessionWithCustomer: WaterSessionWithCustomer,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = sessionWithCustomer.session
    val c = sessionWithCustomer.customer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(PrimaryTeal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = c?.name ?: "عميل غير محدد",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    )
                    val startTimeStr = Formatters.formatTime(s.startTime)
                    val endTimeStr = Formatters.formatTime(s.endTime)
                    val dateStr = Formatters.formatDate(s.startTime)
                    Text(
                        text = "من $startTimeStr إلى $endTimeStr  •  $dateStr (${Formatters.formatDurationArabic(s.durationMinutes)})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(s.totalAmount, currencySymbol),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = if (s.remainingDebt > 0) "متبقي آجل" else "مسدد بالكامل",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (s.remainingDebt > 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp
                    )
                )
            }
        }
    }
}

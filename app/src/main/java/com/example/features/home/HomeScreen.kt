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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.features.settings.SettingsViewModel
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.SettlementResultDialog
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersViewModel
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.SecondaryAqua
import java.util.Calendar

data class DayWaterStat(
    val dayName: String,
    val dayDateText: String,
    val totalMinutes: Int,
    val isToday: Boolean
)

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
    val sessions by sessionsViewModel.filteredSessions.collectAsStateWithLifecycle()
    val customers by sessionsViewModel.customers.collectAsStateWithLifecycle()
    val pumps by sessionsViewModel.pumps.collectAsStateWithLifecycle()
    val toast by sessionsViewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by settingsViewModel.operationsCount.collectAsStateWithLifecycle()
    val isActivated by settingsViewModel.isActivated.collectAsStateWithLifecycle()
    val allSessions by vouchersViewModel.allSessions.collectAsStateWithLifecycle()
    val settlementResult by vouchersViewModel.settlementResult.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (config.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    val context = LocalContext.current
    var showActivationDialog by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var showAddVoucherSheet by remember { mutableStateOf(false) }
    var voucherInitialType by remember { mutableStateOf(VoucherType.RECEIPT) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }

    fun checkOperationAllowed(action: () -> Unit) {
        if (LicenseManager.canPerformOperation(context, operationsCount)) {
            action()
        } else {
            showActivationDialog = true
        }
    }

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
                                    text = "• Mosarib",
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

            // License Status Banner if not activated
            if (!isActivated) {
                item {
                    val remaining = (LicenseManager.FREE_OPERATIONS_LIMIT - operationsCount).coerceAtLeast(0)
                    val isExhausted = remaining == 0
                    Card(
                        onClick = { showActivationDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExhausted) Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isExhausted) Icons.Default.Warning else Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (isExhausted) Color(0xFFD32F2F) else Color(0xFFF57C00),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isExhausted)
                                        "انتهت العمليات المجانية (200/200) • انقر لتفعيل نسختك"
                                    else
                                        "النسخة التجريبية: متبقي $remaining عملية مجانية • تفعيل",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isExhausted) Color(0xFFC62828) else Color(0xFFE65100),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isExhausted) Color(0xFFD32F2F) else Color(0xFFF57C00))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "تفعيل",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    onClick = { checkOperationAllowed { showAddManualSheet = true } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(listOf(PrimaryTealDark, PrimaryTeal)))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = "سجّل سقيًا جديدًا",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                                Text(
                                    text = "أدخل مدة السقي والمبلغ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.82f)
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "بدء تسجيل السقي",
                                tint = AccentGold,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }


            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "عمليات أخرى",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "قبض دفعة",
                            subtitle = "تحصيل وسداد حساب",
                            icon = Icons.Default.Payments,
                            iconBgColor = Color(0xFFE8F5E9),
                            iconTint = AccentEmerald,
                            onClick = {
                                checkOperationAllowed {
                                    voucherInitialType = VoucherType.RECEIPT
                                    showAddVoucherSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            title = "عميل جديد",
                            subtitle = "تسجيل مزارع وأرض",
                            icon = Icons.Default.PersonAdd,
                            iconBgColor = Color(0xFFE0F2FE),
                            iconTint = Color(0xFF0284C7),
                            onClick = { showAddCustomerSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "سجلات السقي",
                            subtitle = "مراجعة الدورات والفواتير",
                            icon = Icons.Default.History,
                            iconBgColor = Color(0xFFFEF3C7),
                            iconTint = Color(0xFFD97706),
                            onClick = { onNavigateToTab(AppTab.SESSIONS) },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            title = "مصروف تشغيلي",
                            subtitle = "صيانة، عام، مصروفات",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconBgColor = Color(0xFFFFEBEE),
                            iconTint = Color(0xFFE53935),
                            onClick = {
                                checkOperationAllowed {
                                    voucherInitialType = VoucherType.EXPENSE
                                    showAddVoucherSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Recent Operations Section (آخر دورات السقي المسجلة)
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

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 6. Visual Reports & Charts
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
                        text = "التقرير الكامل ←",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable(onClick = onOpenReports)
                    )
                }
            }

            item {
                WeeklyIrrigationBarChartCard(sessions = sessions)
            }

            item {
                FinancialRecoveryGaugeCard(
                    homeStats = homeStats,
                    currencySymbol = config.currencySymbol
                )
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

    // Sheet: Add Manual Session
    if (showAddManualSheet) {
        AddEditSessionBottomSheet(
            initialSession = null,
            customers = customers,
            pumps = pumps,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = { showAddManualSheet = false },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes, billedTo, wastedMins, wastedReason, discount, costRate, pumpId ->
                sessionsViewModel.saveManualSession(
                    id = id,
                    customerId = custId,
                    pumpName = pumpName,
                    startTime = startTime,
                    endTime = endTime,
                    hours = hrs,
                    minutes = mins,
                    pricePerHour = rate,
                    amountPaid = paid,
                    notes = notes,
                    billedToCustomerId = billedTo,
                    wastedMinutes = wastedMins,
                    wastedReason = wastedReason,
                    discountAmount = discount,
                    costPricePerHour = costRate,
                    pumpSourceId = pumpId
                )
            }
        )
    }

    // Sheet: Add Voucher (سند قبض أو مصروف)
    if (showAddVoucherSheet) {
        AddVoucherBottomSheet(
            customers = customers,
            allSessions = allSessions,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddVoucherSheet = false },
            onSave = { type, customerId, amount, category, method, notes, sessionId, selectedSessionIds ->
                vouchersViewModel.addVoucher(
                    type = type,
                    customerId = customerId,
                    amount = amount,
                    category = category,
                    paymentMethod = method,
                    notes = notes,
                    sessionId = sessionId,
                    selectedSessionIds = selectedSessionIds
                )
            }
        )
    }

    settlementResult?.let { res ->
        SettlementResultDialog(
            result = res,
            onDismiss = { vouchersViewModel.clearSettlementResult() }
        )
    }

    // Sheet: Add Customer
    if (showAddCustomerSheet) {
        AddEditCustomerBottomSheet(
            initialCustomer = null,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddCustomerSheet = false },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner ->
                customersViewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner)
            }
        )
    }

    // License Activation Dialog
    if (showActivationDialog) {
        LicenseDialog(
            onDismiss = { showActivationDialog = false },
            onActivated = {
                settingsViewModel.refreshActivationStatus()
                showActivationDialog = false
            },
            isMandatory = operationsCount >= LicenseManager.FREE_OPERATIONS_LIMIT
        )
    }
}


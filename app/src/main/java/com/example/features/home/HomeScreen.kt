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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import com.example.features.wellowners.WellOwnerPurchaseBottomSheet
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
    var showHomeTour by remember { mutableStateOf(!LicenseManager.isHomeTourCompleted(context)) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var showAddVoucherSheet by remember { mutableStateOf(false) }
    var voucherInitialType by remember { mutableStateOf(VoucherType.RECEIPT) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var showAddPurchaseSheet by remember { mutableStateOf(false) }

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
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Mosarib",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Help / Interactive Tour Trigger Button
                        IconButton(
                            onClick = { showHomeTour = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = "دليل الاستخدام السريع",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(19.dp)
                            )
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
            }

            // 1.5. Google Drive Cloud Backup Quick Access Banner
            item {
                val googleAccount by settingsViewModel.googleAccount.collectAsStateWithLifecycle()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            settingsViewModel.selectDriveBackupTab()
                            onNavigateToTab(AppTab.SETTINGS)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (googleAccount != null) Color(0xFFF0FDF4) else Color(0xFFFEF3C7).copy(alpha = 0.55f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (googleAccount != null) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (googleAccount != null) Color(0xFFDCFCE7) else Color(0xFFFEF9C3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (googleAccount != null) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = if (googleAccount != null) Color(0xFF16A34A) else Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (googleAccount != null) "النسخ السحابي: متصل" else "النسخ السحابي في Google Drive",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (googleAccount != null) Color(0xFF166534) else Color(0xFF92400E)
                                        )
                                    )
                                    if (googleAccount != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "✓",
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Text(
                                    text = if (googleAccount != null)
                                        "${googleAccount?.email} • إدارة النسخ"
                                    else
                                        "اضغط لربط حساب جوجل لضمان حفظ بياناتك سحابياً",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (googleAccount != null) Color(0xFF15803D) else Color(0xFFB45309),
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (googleAccount != null) Color(0xFF16A34A) else Color(0xFFD97706))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (googleAccount != null) "إدارة النسخ ←" else "ربط الآن ←",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
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
                        text = "الوصول السريع",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
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
                            title = "سقي جديد",
                            subtitle = "تسجيل دورة ري",
                            icon = Icons.Default.WaterDrop,
                            iconBgColor = Color(0xFFE0F2FE),
                            iconTint = PrimaryTeal,
                            onClick = {
                                checkOperationAllowed {
                                    showAddManualSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            title = "قبض",
                            subtitle = "سند تحصيل كاش",
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
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "صرف",
                            subtitle = "صيانة ونفقات",
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

                        QuickActionCard(
                            title = "شراء",
                            subtitle = "شراء ماء من بئر",
                            icon = Icons.Default.LocalGasStation,
                            iconBgColor = Color(0xFFF3E8FF),
                            iconTint = Color(0xFF7E22CE),
                            onClick = {
                                checkOperationAllowed {
                                    showAddPurchaseSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "عميل جديد",
                            subtitle = "إضافة مزارع أو بئر",
                            icon = Icons.Default.PersonAdd,
                            iconBgColor = Color(0xFFE0F2FE),
                            iconTint = Color(0xFF0284C7),
                            onClick = { showAddCustomerSheet = true },
                            modifier = Modifier.weight(1f)
                        )

                        QuickActionCard(
                            title = "تقارير",
                            subtitle = "كشف الحساب والـ PDF",
                            icon = Icons.Default.Assessment,
                            iconBgColor = Color(0xFFFEF3C7),
                            iconTint = Color(0xFFD97706),
                            onClick = onOpenReports,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Recent Operations Section (آخر التسجيلات)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر التسجيلات",
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
                        modifier = Modifier.clickable { onNavigateToTab(AppTab.VOUCHERS) }
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
                        text = "سقي الأسبوع",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
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
        }

        // Floating Action Button لإضافة دورة سقي جديدة
        FloatingActionButton(
            onClick = { checkOperationAllowed { showAddManualSheet = true } },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
            shape = CircleShape,
            containerColor = PrimaryTeal,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "سجل سقي جديد",
                modifier = Modifier.size(24.dp)
            )
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
            initialType = voucherInitialType,
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

    // Sheet: Well Owner Purchase (شراء ماء من بئر)
    if (showAddPurchaseSheet) {
        val wellOwners = customers.filter { it.isWellOwner }
        WellOwnerPurchaseBottomSheet(
            owner = null,
            availableOwners = wellOwners,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddPurchaseSheet = false },
            onSaveWithOwner = { ownerId, date, duration, wasted, rate, paid, notes ->
                customersViewModel.addOwnerPurchase(
                    ownerCustomerId = ownerId,
                    date = date,
                    durationMinutes = duration,
                    wastedMinutesOnOwner = wasted,
                    purchaseRatePerHour = rate,
                    notes = notes,
                    amountPaid = paid
                )
                showAddPurchaseSheet = false
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

    // Interactive Showcase Tour Dialog for First-Time Users
    if (showHomeTour) {
        HomeShowcaseTourDialog(
            onDismiss = { showHomeTour = false }
        )
    }
}


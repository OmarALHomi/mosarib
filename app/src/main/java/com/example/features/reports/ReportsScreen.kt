package com.example.features.reports

import com.example.core.ui.AmountWithWords

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.DropdownOption
import com.example.core.ui.FilterClearAllChip
import com.example.core.ui.FilterDropdownChip
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.MosaribDatePickerDialog
import com.example.core.ui.MosaribFileActionDialog
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.features.vouchers.AccountTypeFilter
import com.example.ui.theme.mosarib
import java.util.Calendar
import kotlin.math.max

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateToCustomer: (Long) -> Unit = {},
    onViewAllDebtors: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.period.collectAsStateWithLifecycle()
    val customDateRange by viewModel.customDateRange.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val accountType by viewModel.accountType.collectAsStateWithLifecycle()
    val customGroups by viewModel.customGroups.collectAsStateWithLifecycle()
    val trendBuckets by viewModel.trendBuckets.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // PDF Open / Share Dialog
    pdfReady?.let { (file, title) ->
        MosaribFileActionDialog(
            file = file,
            title = title,
            fileType = "PDF",
            onOpen = {
                FileSharingHelper.openPdf(context, file)
                viewModel.clearPdfReady()
            },
            onShare = {
                FileSharingHelper.sharePdf(context, file, title)
                viewModel.clearPdfReady()
            },
            onDismiss = { viewModel.clearPdfReady() }
        )
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var tempStartTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val hasActiveFilters = selectedPeriod != ReportPeriod.ALL ||
        customDateRange != null ||
        selectedGroup != null ||
        accountType != AccountTypeFilter.ALL

    // Period options
    val periodOptions = remember {
        listOf(
            DropdownOption(ReportPeriod.ALL, "الكل"),
            DropdownOption(ReportPeriod.TODAY, "اليوم"),
            DropdownOption(ReportPeriod.YESTERDAY, "أمس"),
            DropdownOption(ReportPeriod.THIS_WEEK, "هذا الأسبوع"),
            DropdownOption(ReportPeriod.THIS_MONTH, "هذا الشهر"),
            DropdownOption(ReportPeriod.LAST_MONTH, "الشهر الماضي"),
            DropdownOption(ReportPeriod.THIS_YEAR, "هذه السنة"),
            DropdownOption(ReportPeriod.CUSTOM, "من - إلى…")
        )
    }

    // Group options
    val groupOptions = remember(customGroups) {
        buildList {
            add(DropdownOption<String?>(null, "الكل"))
            customGroups.forEach { add(DropdownOption(it, it)) }
            add(DropdownOption("", "بدون مجموعة"))
        }
    }

    // Account type options
    val accountOptions = remember {
        listOf(
            DropdownOption(AccountTypeFilter.ALL, "الكل"),
            DropdownOption(AccountTypeFilter.FARMER, "مزارع"),
            DropdownOption(AccountTypeFilter.WELL_OWNER, "صاحب بئر")
        )
    }

    val periodDisplayText = remember(selectedPeriod, customDateRange) {
        if (selectedPeriod == ReportPeriod.CUSTOM && customDateRange != null) {
            "${Formatters.formatDate(customDateRange!!.first)} ➔ ${Formatters.formatDate(customDateRange!!.second)}"
        } else null
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // A. Filters Bar (Unified Dropdown Chips)
            item(key = "filters_bar") {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Period Filter
                    item(key = "filter_period") {
                        FilterDropdownChip(
                            label = "الفترة",
                            selected = selectedPeriod,
                            options = periodOptions,
                            isDefault = selectedPeriod == ReportPeriod.ALL && customDateRange == null,
                            onSelect = { p ->
                                if (p == ReportPeriod.CUSTOM) {
                                    showStartDatePicker = true
                                } else {
                                    viewModel.setPeriod(p)
                                }
                            },
                            customDisplayText = periodDisplayText
                        )
                    }

                    // 2. Group Filter
                    item(key = "filter_group") {
                        FilterDropdownChip(
                            label = "المجموعة",
                            selected = selectedGroup,
                            options = groupOptions,
                            isDefault = selectedGroup == null,
                            onSelect = { viewModel.setSelectedGroup(it) }
                        )
                    }

                    // 3. Account Type Filter
                    item(key = "filter_account") {
                        FilterDropdownChip(
                            label = "نوع الحساب",
                            selected = accountType,
                            options = accountOptions,
                            isDefault = accountType == AccountTypeFilter.ALL,
                            onSelect = { viewModel.setAccountType(it) }
                        )
                    }

                    // 4. Clear All
                    if (hasActiveFilters) {
                        item(key = "filter_clear") {
                            FilterClearAllChip(
                                onClearAll = { viewModel.clearFilters() }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // B. KPI Grid (2 × 2)
            item(key = "kpi_grid") {
                ReportsKpiGrid(
                    stats = stats,
                    selectedPeriod = selectedPeriod,
                    currencySymbol = config.currencySymbol
                )
            }

            // C. Trend Chart ("الإيراد والمحصّل")
            item(key = "trend_chart") {
                TrendChartCard(
                    buckets = trendBuckets,
                    currencySymbol = config.currencySymbol
                )
            }

            // D.1. توزيع الربح (Profit Distribution Bar)
            item(key = "profit_distribution") {
                ProfitDistributionCard(
                    stats = stats,
                    currencySymbol = config.currencySymbol
                )
            }

            // D.2. أكبر المدينين (Top Debtors - New)
            if (stats.topDebtors.isNotEmpty()) {
                item(key = "top_debtors") {
                    TopDebtorsCard(
                        topDebtors = stats.topDebtors,
                        currencySymbol = config.currencySymbol,
                        onCustomerClick = onNavigateToCustomer,
                        onViewAllClick = onViewAllDebtors
                    )
                }
            }

            // D.3. حسب المجموعة (By Group - New)
            if (stats.groupStats.size >= 2) {
                item(key = "by_group") {
                    GroupBreakdownCard(
                        groupStats = stats.groupStats,
                        currencySymbol = config.currencySymbol
                    )
                }
            }

            // D.4. كفاءة الضخ (Pumping Efficiency + By Pump Top 5)
            item(key = "pumping_efficiency") {
                PumpingEfficiencyCard(
                    stats = stats
                )
            }

            // D.5. محاسبة مياه الآبار (Well Owner Accounting)
            if (stats.totalOwnerPurchaseAmount > 0 || stats.totalChargeablePurchasedMinutes > 0 || stats.totalOwnerPayments > 0) {
                item(key = "well_owner_accounting") {
                    WellOwnerAccountingCard(
                        stats = stats,
                        currencySymbol = config.currencySymbol
                    )
                }
            }

            // D.6. أكثر الحسابات استهلاكاً (Top Consuming Accounts)
            item(key = "top_consumers") {
                TopCustomersLeaderboardCard(
                    topCustomers = stats.topCustomers,
                    currencySymbol = config.currencySymbol
                )
            }
        }

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Custom Date Range Pickers
        if (showStartDatePicker) {
            MosaribDatePickerDialog(
                title = "تحديد تاريخ البداية",
                initialTimestamp = customDateRange?.first ?: System.currentTimeMillis(),
                onDismiss = { showStartDatePicker = false },
                onDateSelected = { startTs ->
                    val startCal = Calendar.getInstance().apply {
                        timeInMillis = startTs
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    tempStartTimestamp = startCal.timeInMillis
                    showStartDatePicker = false
                    showEndDatePicker = true
                }
            )
        }

        if (showEndDatePicker) {
            MosaribDatePickerDialog(
                title = "تحديد تاريخ النهاية",
                initialTimestamp = customDateRange?.second ?: maxOf(tempStartTimestamp, System.currentTimeMillis()),
                minTimestamp = tempStartTimestamp,
                onDismiss = { showEndDatePicker = false },
                onDateSelected = { endTs ->
                    showEndDatePicker = false
                    val endCal = Calendar.getInstance().apply {
                        timeInMillis = endTs
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                    viewModel.setCustomDateRange(tempStartTimestamp, endCal.timeInMillis)
                }
            )
        }
    }
}

/**
 * B. 2 × 2 KPI Grid:
 * 1. قيمة السقي (الإيراد) = totalRevenue
 * 2. المحصّل نقداً = totalCollectedCash
 * 3. صافي الربح = netOperatingProfit (credit when >= 0, debt when < 0)
 * 4. على العملاء الآن = totalOutstandingDebt ("من N عميلاً", no comparison)
 */
@Composable
private fun ReportsKpiGrid(
    stats: GeneralReportStats,
    selectedPeriod: ReportPeriod,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val showComparison = selectedPeriod != ReportPeriod.ALL
    val isProfit = stats.netOperatingProfit >= 0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Revenue & Collected Cash
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tile 1: Revenue
            val revComp = if (showComparison && stats.previousRevenue != null) {
                ReportsFilter.formatComparison(stats.totalRevenue, stats.previousRevenue)
            } else null

            KpiTile(
                title = "قيمة السقي (الإيراد)",
                amount = stats.totalRevenue,
                currencySymbol = currencySymbol,
                amountColor = MaterialTheme.colorScheme.onSurface,
                comparison = revComp,
                modifier = Modifier.weight(1f)
            )

            // Tile 2: Collected Cash
            val colComp = if (showComparison && stats.previousCollectedCash != null) {
                ReportsFilter.formatComparison(stats.totalCollectedCash, stats.previousCollectedCash)
            } else null

            KpiTile(
                title = "المحصّل نقداً",
                amount = stats.totalCollectedCash,
                currencySymbol = currencySymbol,
                amountColor = MaterialTheme.colorScheme.primary,
                comparison = colComp,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Net Profit & Outstanding Debt
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tile 3: Net Profit
            val profitComp = if (showComparison && stats.previousNetProfit != null) {
                ReportsFilter.formatComparison(stats.netOperatingProfit, stats.previousNetProfit)
            } else null

            KpiTile(
                title = "صافي الربح",
                amount = stats.netOperatingProfit,
                currencySymbol = currencySymbol,
                amountColor = if (isProfit) MaterialTheme.mosarib.credit else MaterialTheme.mosarib.debt,
                comparison = profitComp,
                modifier = Modifier.weight(1f)
            )

            // Tile 4: Outstanding Debt
            KpiTile(
                title = "على العملاء الآن",
                amount = stats.totalOutstandingDebt,
                currencySymbol = currencySymbol,
                amountColor = if (stats.totalOutstandingDebt > 0) MaterialTheme.mosarib.debt else MaterialTheme.colorScheme.onSurface,
                subText = "من ${stats.debtorsCount} عميلاً",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KpiTile(
    title: String,
    amount: Double,
    currencySymbol: String,
    amountColor: Color,
    modifier: Modifier = Modifier,
    comparison: ReportsFilter.ComparisonResult? = null,
    subText: String? = null
) {
    Card(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Label
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.mosarib.textMuted
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Amount with words on tap
            AmountWithWords(
                amount = amount,
                currency = currencySymbol,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = amountColor
            )

            if (comparison != null || subText != null) {
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Comparison line or Sub text
            if (comparison != null) {
                val compColor = when (comparison.isBetter) {
                    true -> MaterialTheme.mosarib.success
                    false -> MaterialTheme.mosarib.debt
                    null -> MaterialTheme.mosarib.textMuted
                }
                Text(
                    text = comparison.text,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = compColor
                    ),
                    maxLines = 1
                )
            } else if (subText != null) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.mosarib.textMuted
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * D.1. Profit Distribution Card:
 * Stacked ratio bar and financial legend with amounts in words.
 * Hides customer disbursements when 0.0.
 */
@Composable
private fun ProfitDistributionCard(
    stats: GeneralReportStats,
    currencySymbol: String
) {
    val totalRevenue = max(0.0, stats.totalRevenue)
    val wellCost = max(0.0, stats.totalWellCost)
    val expenses = max(0.0, stats.totalPumpExpenses)
    val profit = max(0.0, stats.netOperatingProfit)
    val isProfit = stats.netOperatingProfit >= 0

    val wellRatio = if (totalRevenue > 0) (wellCost / totalRevenue).coerceIn(0.0, 1.0).toFloat() else 0f
    val expRatio = if (totalRevenue > 0) (expenses / totalRevenue).coerceIn(0.0, 1.0).toFloat() else 0f
    val profitRatio = if (totalRevenue > 0 && isProfit) (profit / totalRevenue).coerceIn(0.0, 1.0).toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                if (isProfit) MaterialTheme.mosarib.successContainer
                                else MaterialTheme.colorScheme.errorContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = if (isProfit) MaterialTheme.mosarib.credit else MaterialTheme.mosarib.debt,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "توزيع الربح والإيراد",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = if (isProfit) MaterialTheme.mosarib.successContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isProfit) "ربح تشغيلي" else "عجز تشغيلي",
                        color = if (isProfit) MaterialTheme.mosarib.credit else MaterialTheme.mosarib.debt,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (totalRevenue > 0) {
                // Stacked 3-Way Segmented Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (profitRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(profitRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.mosarib.credit)
                        )
                    }
                    if (wellRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(wellRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    if (expRatio > 0f) {
                        Box(
                            modifier = Modifier
                                .weight(expRatio)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown with amounts in words
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinancialBreakdownRow(
                        color = MaterialTheme.mosarib.credit,
                        label = "الربح التشغيلي",
                        amount = stats.netOperatingProfit,
                        currencySymbol = currencySymbol
                    )
                    FinancialBreakdownRow(
                        color = MaterialTheme.colorScheme.secondary,
                        label = "تكلفة الآبار",
                        amount = stats.totalWellCost,
                        currencySymbol = currencySymbol
                    )
                    FinancialBreakdownRow(
                        color = MaterialTheme.colorScheme.error,
                        label = "مصاريف التشغيل",
                        amount = stats.totalPumpExpenses,
                        currencySymbol = currencySymbol
                    )

                    // Hide when 0.0
                    if (stats.customerDisbursements > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        FinancialBreakdownRow(
                            color = MaterialTheme.mosarib.warning,
                            label = "صرف للعملاء (يُضاف لديونهم)",
                            amount = stats.customerDisbursements,
                            currencySymbol = currencySymbol
                        )
                    }
                }
            } else {
                Text(
                    text = "لا توجد حركات مسجلة في هذه الفترة",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.textMuted)
                )
            }
        }
    }
}

@Composable
private fun FinancialBreakdownRow(
    color: Color,
    label: String,
    amount: Double,
    currencySymbol: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted))
        }

        AmountWithWords(
            amount = amount,
            currency = currencySymbol,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * D.2. Top Debtors Card (New):
 * Top 5 customers by receivableBalance > 0.
 * Name, amount in debt, approximate words. Tapping one opens the customer.
 * "عرض الكل" switches to customers tab with "عليه" status filter.
 */
@Composable
private fun TopDebtorsCard(
    topDebtors: List<TopDebtorStat>,
    currencySymbol: String,
    onCustomerClick: (Long) -> Unit,
    onViewAllClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.mosarib.debt,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أكبر المدينين",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                TextButton(
                    onClick = onViewAllClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "عرض الكل",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                topDebtors.forEach { debtor ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .clickable { onCustomerClick(debtor.customerId) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Line 1: Name (full width except amount) + Amount (intrinsic width at end)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = debtor.customerName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = Formatters.formatCurrency(debtor.receivableBalance, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.mosarib.debt
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Line 2: Approximate words under name, full width
                        Text(
                            text = Formatters.amountToArabicWordsApprox(debtor.receivableBalance, currencySymbol),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.textMuted
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * D.3. Group Breakdown Card (New):
 * Rows of group → revenue · collected · remaining, compact numbers, sorted by revenue.
 * Hidden when there are fewer than 2 groups.
 */
@Composable
private fun GroupBreakdownCard(
    groupStats: List<GroupReportStat>,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "حسب المجموعة",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                groupStats.forEach { grp ->
                    val revCompact = ReportsFilter.formatCompactNumber(grp.revenue)
                    val colCompact = ReportsFilter.formatCompactNumber(grp.collected)
                    val remCompact = ReportsFilter.formatCompactNumber(grp.remainingDebt)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = grp.groupName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "إيراد $revCompact · محصّل $colCompact · آجل $remCompact $currencySymbol",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.mosarib.textMuted,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * D.4. Pumping Efficiency & Volume Card:
 * Sold hours, wasted hours, number of sessions, plus "حسب المضخة" rows (pump → hours), top 5.
 */
@Composable
private fun PumpingEfficiencyCard(
    stats: GeneralReportStats
) {
    val totalIrrigationMinutes = stats.totalWaterMinutes + stats.totalDistributorWasteMinutes
    val efficiencyPercent = if (totalIrrigationMinutes > 0) {
        ((stats.totalWaterMinutes.toDouble() / totalIrrigationMinutes) * 100).toInt()
    } else 100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "كفاءة الضخ وساعات السقي",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$efficiencyPercent% كفاءة",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Big Duration Value & Sessions Count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "ساعات السقي المباعة",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                    )
                    Text(
                        text = Formatters.formatDurationArabic(stats.totalWaterMinutes),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.mosarib.textMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${stats.totalSessionsCount} سقي",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Efficiency Visual Bar
            LinearProgressIndicator(
                progress = { (efficiencyPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.mosarib.warningContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إجمالي التشغيل:",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                    )
                    Text(
                        text = Formatters.formatDurationArabic(totalIrrigationMinutes),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "هدر المسرب:",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                    )
                    Text(
                        text = Formatters.formatDurationArabic(stats.totalDistributorWasteMinutes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.mosarib.warning,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // "حسب المضخة" breakdown (top 5)
            if (stats.pumpStats.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "حسب المضخة",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    stats.pumpStats.forEach { pump ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${pump.pumpName} (${pump.sessionsCount} سقي)",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                            Text(
                                text = Formatters.formatDurationArabic(pump.totalMinutes),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * D.5. Well Owner Accounts Card:
 * Clean financial ledger comparing purchases, payments, and balance payable.
 * Fixed "454:00 س" to "454 ساعة" via formatDurationArabic.
 */
@Composable
private fun WellOwnerAccountingCard(
    stats: GeneralReportStats,
    currencySymbol: String
) {
    val purchaseAmt = max(0.0, stats.totalOwnerPurchaseAmount)
    val payments = max(0.0, stats.totalOwnerPayments)
    val fulfillmentRatio = if (purchaseAmt > 0) (payments / purchaseAmt).coerceIn(0.0, 1.0).toFloat() else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "محاسبة مياه الآبار",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = if (stats.totalOwnerPayable <= 0) MaterialTheme.mosarib.successContainer else MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (stats.totalOwnerPayable <= 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.mosarib.success,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "مسدد",
                                color = MaterialTheme.mosarib.success,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        Text(
                            text = "متبقي مستحقات",
                            color = MaterialTheme.mosarib.debt,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Metric Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    title = "صافي المشتريات",
                    value = Formatters.formatCurrency(stats.totalOwnerPurchaseAmount, currencySymbol),
                    subtitle = Formatters.formatDurationArabic(stats.totalChargeablePurchasedMinutes),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "المسدد للبئر",
                    value = Formatters.formatCurrency(stats.totalOwnerPayments, currencySymbol),
                    subtitle = "تحويلات وسداد",
                    color = MaterialTheme.mosarib.credit,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "المتبقي للبئر",
                    value = Formatters.formatCurrency(stats.totalOwnerPayable, currencySymbol),
                    subtitle = "رصيد دائن",
                    color = if (stats.totalOwnerPayable > 0) MaterialTheme.mosarib.debt else MaterialTheme.mosarib.success,
                    modifier = Modifier.weight(1f)
                )
            }

            if (purchaseAmt > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { fulfillmentRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.mosarib.credit,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted))
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color))
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted))
        }
    }
}

/**
 * D.6. Top Consuming Customers Leaderboard (Keep):
 */
@Composable
private fun TopCustomersLeaderboardCard(
    topCustomers: List<TopCustomerStat>,
    currencySymbol: String
) {
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
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أكثر الحسابات استهلاكاً",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "${topCustomers.size} حسابات",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topCustomers.isEmpty()) {
                Text(
                    text = "لا توجد بيانات استهلاك مسجلة في هذه الفترة",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.textMuted),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val maxMinutes = max(1, topCustomers.maxOf { it.totalMinutes })

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topCustomers.take(5).forEachIndexed { index, top ->
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "#${index + 1}"
                        }
                        val relativeWidth = (top.totalMinutes.toFloat() / maxMinutes).coerceIn(0.05f, 1f)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(medal)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = top.customer.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (top.customer.isWellOwner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "بئر",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatDurationArabic(top.totalMinutes),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = Formatters.formatCurrency(top.totalBilled, currencySymbol),
                                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Relative consumption progress bar
                                LinearProgressIndicator(
                                    progress = { relativeWidth },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp)),
                                    color = if (index == 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.features.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.ComprehensiveCustomerRow
import com.example.core.util.ComprehensiveReportTotals
import com.example.core.util.Formatters
import com.example.core.util.PdfReportGenerator
import com.example.core.util.PeriodRange
import com.example.features.customers.Customer
import com.example.features.customers.calculateCustomerBalance
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionRepository
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
import com.example.features.vouchers.AccountTypeFilter
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherRepository
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import com.example.features.wellowners.WellOwnerPurchaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

enum class ReportPeriod {
    ALL, TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR, CUSTOM
}

data class TopDebtorStat(
    val customerId: Long,
    val customerName: String,
    val receivableBalance: Double
)

data class GroupReportStat(
    val groupName: String,
    val revenue: Double,
    val collected: Double,
    val remainingDebt: Double
)

data class PumpReportStat(
    val pumpName: String,
    val totalMinutes: Int,
    val sessionsCount: Int
)

data class GeneralReportStats(
    val totalSessionsCount: Int = 0,
    /** Irrigation hours billed after distributor waste is deducted. */
    val totalWaterMinutes: Int = 0,
    val totalDistributorWasteMinutes: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalCollectedCash: Double = 0.0,
    /** Customer-side receivables; never offset against owner payables. */
    val totalOutstandingDebt: Double = 0.0,
    /** Net value of independently recorded well-owner purchases in the selected period. */
    val totalWellCost: Double = 0.0,
    /** Operating expense vouchers with customerId == null only. */
    val totalPumpExpenses: Double = 0.0,
    /** EXPENSE vouchers disbursed to non-owner customers (increases customer debt). */
    val customerDisbursements: Double = 0.0,
    val netOperatingProfit: Double = 0.0,
    val totalPurchasedMinutes: Int = 0,
    val totalChargeablePurchasedMinutes: Int = 0,
    val totalOwnerWasteMinutes: Int = 0,
    val totalOwnerPurchaseAmount: Double = 0.0,
    val totalOwnerWasteCredit: Double = 0.0,
    val totalOwnerPayments: Double = 0.0,
    /** Net amount accrued from purchases minus payments in the selected period. */
    val totalOwnerPayable: Double = 0.0,
    val topCustomers: List<TopCustomerStat> = emptyList(),
    val debtorsCount: Int = 0,
    val topDebtors: List<TopDebtorStat> = emptyList(),
    val groupStats: List<GroupReportStat> = emptyList(),
    val pumpStats: List<PumpReportStat> = emptyList(),
    val previousRevenue: Double? = null,
    val previousCollectedCash: Double? = null,
    val previousNetProfit: Double? = null
)

data class TopCustomerStat(
    val customer: Customer,
    val totalMinutes: Int,
    val totalBilled: Double,
    val balance: Double
)

data class ReportsFilterParams(
    val period: ReportPeriod = ReportPeriod.ALL,
    val customRange: Pair<Long, Long>? = null,
    val group: String? = null,
    val accountType: AccountTypeFilter = AccountTypeFilter.ALL
) {
    val isDefault: Boolean
        get() = period == ReportPeriod.ALL &&
                customRange == null &&
                group == null &&
                accountType == AccountTypeFilter.ALL
}

private data class ReportSnapshot(
    val stats: GeneralReportStats,
    val customerRows: List<ComprehensiveCustomerRow>,
    val totals: ComprehensiveReportTotals
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionRepo = WaterSessionRepository(db.waterSessionDao(), db.customerDao())
    private val voucherRepo = VoucherRepository(db.voucherDao(), db.customerDao())
    private val ownerPurchaseRepo = WellOwnerPurchaseRepository(db.wellOwnerPurchaseDao())
    private val settingsRepo = SettingsRepository(db.appSettingDao())

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    private val _period = MutableStateFlow(ReportPeriod.ALL)
    val period: StateFlow<ReportPeriod> = _period.asStateFlow()

    private val _customDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val customDateRange: StateFlow<Pair<Long, Long>?> = _customDateRange.asStateFlow()

    private val _selectedGroup = MutableStateFlow<String?>(null)
    val selectedGroup: StateFlow<String?> = _selectedGroup.asStateFlow()

    private val _accountType = MutableStateFlow(AccountTypeFilter.ALL)
    val accountType: StateFlow<AccountTypeFilter> = _accountType.asStateFlow()

    val allCustomers: StateFlow<List<Customer>> = db.customerDao().getAllCustomersIncludingArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customGroups: StateFlow<List<String>> = allCustomers.map { list ->
        list.mapNotNull { it.customGroup.trim().takeIf { g -> g.isNotBlank() } }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterParams = combine(_period, _customDateRange, _selectedGroup, _accountType) { p, r, g, a ->
        ReportsFilterParams(p, r, g, a)
    }

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    val stats: StateFlow<GeneralReportStats> = combine(
        sessionRepo.allSessions,
        voucherRepo.allVouchers,
        allCustomers,
        ownerPurchaseRepo.allPurchases,
        filterParams
    ) { sessions, vouchers, customers, purchases, params ->
        calculateReportSnapshot(
            allSessions = sessions,
            allVouchers = vouchers,
            allCustomers = customers,
            allPurchases = purchases,
            selectedPeriod = params.period,
            customRange = params.customRange,
            selectedGroup = params.group,
            accountType = params.accountType
        ).stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneralReportStats())

    val trendBuckets: StateFlow<List<TrendBucket>> = combine(
        sessionRepo.allSessions,
        voucherRepo.allVouchers,
        allCustomers,
        filterParams
    ) { sessions, vouchers, customers, params ->
        val filteredCusts = ReportsFilter.filterCustomers(customers, params.group, params.accountType)
        val allowedCustomerIds = if (params.group == null && params.accountType == AccountTypeFilter.ALL) {
            null
        } else {
            filteredCusts.map { it.id }.toSet()
        }
        val targetSessions = ReportsFilter.filterSessions(sessions, Pair(null, null), allowedCustomerIds)
        val targetVouchers = ReportsFilter.filterVouchers(vouchers, Pair(null, null), allowedCustomerIds)
        TrendBucketBuilder.buildBuckets(
            period = params.period,
            customRange = params.customRange,
            sessions = targetSessions,
            vouchers = targetVouchers
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setPeriod(p: ReportPeriod) {
        _period.value = p
        if (p != ReportPeriod.CUSTOM) {
            _customDateRange.value = null
        }
    }

    fun setCustomDateRange(startMillis: Long, endMillis: Long) {
        _customDateRange.value = Pair(startMillis, endMillis)
        _period.value = ReportPeriod.CUSTOM
    }

    fun setSelectedGroup(group: String?) {
        _selectedGroup.value = group
    }

    fun setAccountType(type: AccountTypeFilter) {
        _accountType.value = type
    }

    fun clearFilters() {
        _period.value = ReportPeriod.ALL
        _customDateRange.value = null
        _selectedGroup.value = null
        _accountType.value = AccountTypeFilter.ALL
    }

    private fun periodTitle(period: ReportPeriod): String = when (period) {
        ReportPeriod.ALL -> "كل الفترات"
        ReportPeriod.TODAY -> "اليوم"
        ReportPeriod.YESTERDAY -> "أمس"
        ReportPeriod.THIS_WEEK -> "هذا الأسبوع"
        ReportPeriod.THIS_MONTH -> "هذا الشهر"
        ReportPeriod.LAST_MONTH -> "الشهر الماضي"
        ReportPeriod.THIS_YEAR -> "هذه السنة"
        ReportPeriod.CUSTOM -> {
            val range = _customDateRange.value
            if (range != null) {
                "من ${Formatters.formatDate(range.first)} إلى ${Formatters.formatDate(range.second)}"
            } else "فترة مخصصة"
        }
    }

    private fun calculateReportSnapshot(
        allSessions: List<WaterSession>,
        allVouchers: List<Voucher>,
        allCustomers: List<Customer>,
        allPurchases: List<WellOwnerPurchase>,
        selectedPeriod: ReportPeriod,
        customRange: Pair<Long, Long>? = null,
        selectedGroup: String? = null,
        accountType: AccountTypeFilter = AccountTypeFilter.ALL
    ): ReportSnapshot {
        val (start, end) = PeriodRange.rangeFor(selectedPeriod, customRange = customRange)

        val filteredCustomers = ReportsFilter.filterCustomers(allCustomers, selectedGroup, accountType)
        val hasCustomerFilter = selectedGroup != null || accountType != AccountTypeFilter.ALL
        val allowedCustomerIds = if (hasCustomerFilter) filteredCustomers.map { it.id }.toSet() else null

        val sessions = ReportsFilter.filterSessions(allSessions, Pair(start, end), allowedCustomerIds)
        val vouchers = ReportsFilter.filterVouchers(allVouchers, Pair(start, end), allowedCustomerIds)
        val purchases = ReportsFilter.filterPurchases(allPurchases, Pair(start, end), allowedCustomerIds)

        val customersById = allCustomers.associateBy { it.id }

        // A session's amountPaid already contains linked receipt vouchers. Attribute those vouchers
        // by their voucher date and count only the original up-front payment on the session date.
        val linkedReceiptsBySession = allVouchers
            .filter { it.type == VoucherType.RECEIPT && it.sessionId != null }
            .groupBy { it.sessionId!! }
            .mapValues { (_, rows) -> rows.sumOf { it.amount.coerceAtLeast(0.0) } }
        fun initialPayment(session: WaterSession): Double =
            (session.amountPaid - (linkedReceiptsBySession[session.id] ?: 0.0)).coerceAtLeast(0.0)

        val totalRevenue = sessions.sumOf { it.totalAmount.coerceAtLeast(0.0) }
        val totalCollected = sessions.sumOf(::initialPayment) +
            vouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount.coerceAtLeast(0.0) }

        val customerRows = filteredCustomers.map { customer ->
            val cwb = calculateCustomerBalance(customer, sessions, vouchers, purchases = purchases)
            ComprehensiveCustomerRow(
                customerName = customer.name,
                phone = customer.phone,
                farmName = customer.farmName.ifEmpty { customer.location },
                sessionsCount = cwb.totalSessionsCount,
                waterMinutes = cwb.totalSoldMinutes,
                billed = cwb.totalBilledAmount,
                paid = cwb.totalPaidAmount,
                balance = cwb.balance,
                isWellOwner = customer.isWellOwner,
                purchasedMinutes = cwb.totalPurchasedMinutes,
                chargeablePurchasedMinutes = cwb.totalChargeablePurchasedMinutes,
                ownerWasteMinutes = (cwb.totalPurchasedMinutes - cwb.totalChargeablePurchasedMinutes).coerceAtLeast(0),
                ownerWasteCredit = cwb.totalOwnerWasteCredit,
                purchaseAmount = cwb.totalPurchaseAmount,
                ownerPayments = cwb.totalDisbursedAmount,
                payableBalance = cwb.payableBalance,
                receivableBalance = cwb.receivableBalance
            )
        }.sortedWith(compareByDescending<ComprehensiveCustomerRow> { it.balance }.thenBy { it.customerName })

        val totalOutstandingDebt = customerRows.sumOf { it.receivableBalance }
        val debtorsCount = customerRows.count { it.receivableBalance > 0.001 }

        val topDebtors = customerRows
            .filter { it.receivableBalance > 0.001 }
            .sortedByDescending { it.receivableBalance }
            .take(5)
            .mapNotNull { row ->
                val cust = filteredCustomers.find { it.name == row.customerName } ?: return@mapNotNull null
                TopDebtorStat(
                    customerId = cust.id,
                    customerName = cust.name,
                    receivableBalance = row.receivableBalance
                )
            }

        val distributorWasteMinutes = sessions.sumOf { session ->
            session.wastedMinutes.coerceIn(0, session.durationMinutes.coerceAtLeast(0))
        }
        val saleMinutes = sessions.sumOf { session ->
            (session.durationMinutes - session.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
        }
        val ownerWasteMinutes = purchases.sumOf { purchase ->
            purchase.wastedMinutesOnOwner.coerceIn(0, purchase.durationMinutes.coerceAtLeast(0))
        }
        val chargeablePurchaseMinutes = purchases.sumOf(WellOwnerPurchaseMath::chargeableMinutes)
        val totalPurchasedMinutes = purchases.sumOf { it.durationMinutes.coerceAtLeast(0) }
        val ownerPurchaseAmount = purchases.sumOf(WellOwnerPurchaseMath::payableAmount)
        val totalWellCost = ownerPurchaseAmount
        val ownerWasteCredit = purchases.sumOf(WellOwnerPurchaseMath::ownerWasteCredit)
        val ownerPayments = vouchers
            .filter { voucher ->
                voucher.type == VoucherType.EXPENSE &&
                    voucher.customerId?.let { customersById[it]?.isWellOwner == true } == true
            }
            .sumOf { it.amount.coerceAtLeast(0.0) }
        val operatingExpenses = vouchers
            .filter { voucher ->
                voucher.type == VoucherType.EXPENSE && voucher.customerId == null
            }
            .sumOf { it.amount.coerceAtLeast(0.0) }
        val customerDisbursements = vouchers
            .filter { voucher ->
                voucher.type == VoucherType.EXPENSE &&
                    voucher.customerId != null &&
                    customersById[voucher.customerId]?.isWellOwner != true
            }
            .sumOf { it.amount.coerceAtLeast(0.0) }
        val netProfit = totalRevenue - totalWellCost - operatingExpenses
        val ownerPayable = customerRows.filter { it.isWellOwner }.sumOf { it.payableBalance }

        val topList = sessions
            .groupBy { it.billedToCustomerId ?: it.customerId }
            .mapNotNull { (customerId, customerSessions) ->
                val customer = customersById[customerId] ?: return@mapNotNull null
                val cwb = calculateCustomerBalance(
                    customer,
                    customerSessions,
                    vouchers.filter { it.customerId == customer.id },
                    purchases = purchases.filter { it.ownerCustomerId == customer.id }
                )
                TopCustomerStat(
                    customer = customer,
                    totalMinutes = customerSessions.sumOf { session ->
                        (session.durationMinutes - session.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
                    },
                    totalBilled = cwb.totalBilledAmount,
                    balance = cwb.balance
                )
            }
            .sortedByDescending { it.totalMinutes }
            .take(5)

        // Group breakdown (revenue, collected, remaining)
        val groupStats = allCustomers
            .groupBy { it.customGroup.trim().ifEmpty { "بدون مجموعة" } }
            .map { (groupName, groupMembers) ->
                val gCustIds = groupMembers.map { it.id }.toSet()
                val gSessions = sessions.filter { (it.billedToCustomerId ?: it.customerId) in gCustIds }
                val gVouchers = vouchers.filter { it.customerId != null && it.customerId in gCustIds }

                val gRevenue = gSessions.sumOf { it.totalAmount.coerceAtLeast(0.0) }
                val gCollected = gSessions.sumOf(::initialPayment) +
                    gVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount.coerceAtLeast(0.0) }
                val gRemaining = customerRows
                    .filter { row -> groupMembers.any { it.name == row.customerName } }
                    .sumOf { it.receivableBalance }

                GroupReportStat(
                    groupName = groupName,
                    revenue = gRevenue,
                    collected = gCollected,
                    remainingDebt = gRemaining
                )
            }
            .sortedByDescending { it.revenue }

        // Pump breakdown (top 5 by water minutes)
        val pumpStats = sessions
            .groupBy { it.pumpName.trim().ifEmpty { "المضخة الرئيسية" } }
            .map { (pumpName, pumpSessions) ->
                PumpReportStat(
                    pumpName = pumpName,
                    totalMinutes = pumpSessions.sumOf { (it.durationMinutes - it.wastedMinutes).coerceAtLeast(0) },
                    sessionsCount = pumpSessions.size
                )
            }
            .sortedByDescending { it.totalMinutes }
            .take(5)

        // Previous period comparisons (when period is not ALL)
        val (prevRevenue, prevCollected, prevNetProfit) = if (selectedPeriod != ReportPeriod.ALL) {
            val prevRange = PeriodRange.previousRangeFor(selectedPeriod, customRange = customRange)
            val prevSessions = ReportsFilter.filterSessions(allSessions, prevRange, allowedCustomerIds)
            val prevVouchers = ReportsFilter.filterVouchers(allVouchers, prevRange, allowedCustomerIds)
            val prevPurchases = ReportsFilter.filterPurchases(allPurchases, prevRange, allowedCustomerIds)

            val pRev = prevSessions.sumOf { it.totalAmount.coerceAtLeast(0.0) }
            val pCol = prevSessions.sumOf(::initialPayment) +
                prevVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount.coerceAtLeast(0.0) }
            val pWellCost = prevPurchases.sumOf(WellOwnerPurchaseMath::payableAmount)
            val pExp = prevVouchers.filter { it.type == VoucherType.EXPENSE && it.customerId == null }.sumOf { it.amount.coerceAtLeast(0.0) }
            val pNet = pRev - pWellCost - pExp
            Triple(pRev, pCol, pNet)
        } else {
            Triple(null, null, null)
        }

        val reportStats = GeneralReportStats(
            totalSessionsCount = sessions.size,
            totalWaterMinutes = saleMinutes,
            totalDistributorWasteMinutes = distributorWasteMinutes,
            totalRevenue = totalRevenue,
            totalCollectedCash = totalCollected,
            totalOutstandingDebt = totalOutstandingDebt,
            totalWellCost = totalWellCost,
            totalPumpExpenses = operatingExpenses,
            customerDisbursements = customerDisbursements,
            netOperatingProfit = netProfit,
            totalPurchasedMinutes = totalPurchasedMinutes,
            totalChargeablePurchasedMinutes = chargeablePurchaseMinutes,
            totalOwnerWasteMinutes = ownerWasteMinutes,
            totalOwnerPurchaseAmount = ownerPurchaseAmount,
            totalOwnerWasteCredit = ownerWasteCredit,
            totalOwnerPayments = ownerPayments,
            totalOwnerPayable = ownerPayable,
            topCustomers = topList,
            debtorsCount = debtorsCount,
            topDebtors = topDebtors,
            groupStats = groupStats,
            pumpStats = pumpStats,
            previousRevenue = prevRevenue,
            previousCollectedCash = prevCollected,
            previousNetProfit = prevNetProfit
        )

        val totals = ComprehensiveReportTotals(
            sessionsCount = sessions.size,
            waterMinutes = saleMinutes,
            totalRevenue = totalRevenue,
            totalCollected = totalCollected,
            totalOutstandingDebt = totalOutstandingDebt,
            totalExpenses = operatingExpenses,
            netProfit = netProfit,
            distributorWasteMinutes = distributorWasteMinutes,
            purchasedMinutes = totalPurchasedMinutes,
            chargeablePurchasedMinutes = chargeablePurchaseMinutes,
            ownerWasteMinutes = ownerWasteMinutes,
            ownerPurchaseAmount = ownerPurchaseAmount,
            ownerWasteCredit = ownerWasteCredit,
            ownerPayments = ownerPayments,
            ownerPayable = ownerPayable
        )
        return ReportSnapshot(reportStats, customerRows, totals)
    }

    private val _pdfReadyFile = MutableStateFlow<Pair<File, String>?>(null)
    val pdfReadyFile: StateFlow<Pair<File, String>?> = _pdfReadyFile.asStateFlow()
    fun clearPdfReady() { _pdfReadyFile.value = null }

    fun showToast(msg: String, type: ToastType = ToastType.SUCCESS) {
        _toast.value = ToastMessage(message = msg, type = type)
    }

    fun dismissToast() {
        _toast.value = null
    }

    /** Exports period totals and customer-level receivable/payable details. */
    fun exportComprehensiveReportPdf() {
        viewModelScope.launch {
            try {
                val config = appConfig.value
                val selectedPeriod = _period.value
                val customRange = _customDateRange.value
                val group = _selectedGroup.value
                val accType = _accountType.value

                val customers = allCustomers.first()
                val sessions = sessionRepo.allSessions.first()
                val vouchers = voucherRepo.allVouchers.first()
                val purchases = ownerPurchaseRepo.allPurchases.first()

                val snapshot = calculateReportSnapshot(
                    allSessions = sessions,
                    allVouchers = vouchers,
                    allCustomers = customers,
                    allPurchases = purchases,
                    selectedPeriod = selectedPeriod,
                    customRange = customRange,
                    selectedGroup = group,
                    accountType = accType
                )

                if (snapshot.customerRows.isEmpty() && snapshot.totals.sessionsCount == 0 && snapshot.totals.purchasedMinutes == 0) {
                    showToast("لا توجد بيانات كافية للتصدير", ToastType.WARNING)
                    return@launch
                }

                val file = PdfReportGenerator.generateComprehensiveReportPdf(
                    context = getApplication(),
                    config = config,
                    periodTitle = periodTitle(selectedPeriod),
                    totals = snapshot.totals,
                    customerRows = snapshot.customerRows
                )
                _pdfReadyFile.value = Pair(file, "التقرير المحاسبي الشامل - ${periodTitle(selectedPeriod)}")
            } catch (e: Exception) {
                showToast("فشل في إنشاء التقرير: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }
}

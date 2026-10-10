package com.example.features.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.ComprehensiveCustomerRow
import com.example.core.util.ComprehensiveReportTotals
import com.example.core.util.PdfReportGenerator
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionRepository
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import kotlin.math.max

enum class ReportPeriod {
    ALL, TODAY, THIS_WEEK, THIS_MONTH
}

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
    /** Operating expense vouchers, excluding payments made to well owners. */
    val totalPumpExpenses: Double = 0.0,
    val netOperatingProfit: Double = 0.0,
    val totalPurchasedMinutes: Int = 0,
    val totalChargeablePurchasedMinutes: Int = 0,
    val totalOwnerWasteMinutes: Int = 0,
    val totalOwnerPurchaseAmount: Double = 0.0,
    val totalOwnerWasteCredit: Double = 0.0,
    val totalOwnerPayments: Double = 0.0,
    /** Net amount accrued from purchases minus payments in the selected period. */
    val totalOwnerPayable: Double = 0.0,
    val topCustomers: List<TopCustomerStat> = emptyList()
)

data class TopCustomerStat(
    val customer: Customer,
    val totalMinutes: Int,
    val totalBilled: Double,
    val balance: Double
)

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

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    val stats: StateFlow<GeneralReportStats> = combine(
        sessionRepo.allSessions,
        voucherRepo.allVouchers,
        db.customerDao().getAllCustomersIncludingArchived(),
        ownerPurchaseRepo.allPurchases,
        _period
    ) { sessions, vouchers, customers, purchases, selectedPeriod ->
        calculateReportSnapshot(sessions, vouchers, customers, purchases, selectedPeriod).stats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneralReportStats())

    fun setPeriod(p: ReportPeriod) {
        _period.value = p
    }

    /**
     * Start (inclusive) timestamp of the selected local-calendar period. The period has no
     * artificial end time: newly entered transactions remain visible while the report screen stays open.
     */
    private fun periodStartMillis(period: ReportPeriod): Long? {
        if (period == ReportPeriod.ALL) return null
        val calendar = Calendar.getInstance()
        when (period) {
            ReportPeriod.TODAY -> calendar.set(Calendar.HOUR_OF_DAY, 0)
            ReportPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }
            ReportPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
            }
            ReportPeriod.ALL -> Unit
        }
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun periodTitle(period: ReportPeriod): String = when (period) {
        ReportPeriod.ALL -> "كل الفترات"
        ReportPeriod.TODAY -> "اليوم"
        ReportPeriod.THIS_WEEK -> "هذا الأسبوع"
        ReportPeriod.THIS_MONTH -> "هذا الشهر"
    }

    private fun calculateReportSnapshot(
        allSessions: List<WaterSession>,
        allVouchers: List<Voucher>,
        customers: List<Customer>,
        allPurchases: List<WellOwnerPurchase>,
        selectedPeriod: ReportPeriod
    ): ReportSnapshot {
        val periodStart = periodStartMillis(selectedPeriod)
        val sessions = if (periodStart == null) allSessions else allSessions.filter { it.startTime >= periodStart }
        val vouchers = if (periodStart == null) allVouchers else allVouchers.filter { it.date >= periodStart }
        val purchases = if (periodStart == null) allPurchases else allPurchases.filter { it.date >= periodStart }
        val customersById = customers.associateBy { it.id }

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
        val totalDiscounts = vouchers
            .filter { it.type == VoucherType.DISCOUNT && it.sessionId == null }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        val customerSideExpenses = vouchers
            .filter { voucher ->
                voucher.type == VoucherType.EXPENSE &&
                    voucher.customerId?.let { customersById[it]?.isWellOwner == false } == true
            }
            .sumOf { it.amount.coerceAtLeast(0.0) }
        val totalOutstandingDebt = max(0.0, totalRevenue + customerSideExpenses - totalCollected - totalDiscounts)

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
                voucher.type == VoucherType.EXPENSE &&
                    (voucher.customerId == null || customersById[voucher.customerId]?.isWellOwner != true)
            }
            .sumOf { it.amount.coerceAtLeast(0.0) }
        val netProfit = totalRevenue - totalWellCost - operatingExpenses
        val ownerPayable = ownerPurchaseAmount - ownerPayments

        val topList = sessions
            .groupBy { it.billedToCustomerId ?: it.customerId }
            .mapNotNull { (customerId, customerSessions) ->
                val customer = customersById[customerId] ?: return@mapNotNull null
                val customerVouchers = vouchers.filter { it.customerId == customer.id }
                val customerRevenue = customerSessions.sumOf { it.totalAmount.coerceAtLeast(0.0) }
                val customerPaid = customerSessions.sumOf(::initialPayment) +
                    customerVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount.coerceAtLeast(0.0) }
                val customerDiscount = customerVouchers
                    .filter { it.type == VoucherType.DISCOUNT && it.sessionId == null }
                    .sumOf { it.amount.coerceAtLeast(0.0) }
                val customerExpense = if (customer.isWellOwner) 0.0 else customerVouchers
                    .filter { it.type == VoucherType.EXPENSE }
                    .sumOf { it.amount.coerceAtLeast(0.0) }
                TopCustomerStat(
                    customer = customer,
                    totalMinutes = customerSessions.sumOf { session ->
                        (session.durationMinutes - session.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
                    },
                    totalBilled = customerRevenue,
                    balance = max(0.0, customerRevenue + customerExpense - customerPaid - customerDiscount)
                )
            }
            .sortedByDescending { it.totalMinutes }
            .take(5)

        val reportStats = GeneralReportStats(
            totalSessionsCount = sessions.size,
            totalWaterMinutes = saleMinutes,
            totalDistributorWasteMinutes = distributorWasteMinutes,
            totalRevenue = totalRevenue,
            totalCollectedCash = totalCollected,
            totalOutstandingDebt = totalOutstandingDebt,
            totalWellCost = totalWellCost,
            totalPumpExpenses = operatingExpenses,
            netOperatingProfit = netProfit,
            totalPurchasedMinutes = totalPurchasedMinutes,
            totalChargeablePurchasedMinutes = chargeablePurchaseMinutes,
            totalOwnerWasteMinutes = ownerWasteMinutes,
            totalOwnerPurchaseAmount = ownerPurchaseAmount,
            totalOwnerWasteCredit = ownerWasteCredit,
            totalOwnerPayments = ownerPayments,
            totalOwnerPayable = ownerPayable,
            topCustomers = topList
        )

        val customerRows = customers.map { customer ->
            val customerSessions = sessions.filter { session ->
                session.billedToCustomerId == customer.id ||
                    (session.customerId == customer.id && session.billedToCustomerId == null)
            }
            val customerVouchers = vouchers.filter { it.customerId == customer.id }
            val customerPurchases = purchases.filter { it.ownerCustomerId == customer.id }
            val billed = customerSessions.sumOf { it.totalAmount.coerceAtLeast(0.0) }
            val customerPaid = customerSessions.sumOf(::initialPayment) +
                customerVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount.coerceAtLeast(0.0) }
            val discount = customerVouchers
                .filter { it.type == VoucherType.DISCOUNT && it.sessionId == null }
                .sumOf { it.amount.coerceAtLeast(0.0) }
            val customerExpenses = if (customer.isWellOwner) 0.0 else customerVouchers
                .filter { it.type == VoucherType.EXPENSE }
                .sumOf { it.amount.coerceAtLeast(0.0) }
            val receivable = max(0.0, billed + customerExpenses - customerPaid - discount)
            val purchaseAmount = customerPurchases.sumOf(WellOwnerPurchaseMath::payableAmount)
            val ownerPaymentsForCustomer = customerVouchers
                .filter { it.type == VoucherType.EXPENSE }
                .sumOf { it.amount.coerceAtLeast(0.0) }
            val payable = if (customer.isWellOwner) purchaseAmount - ownerPaymentsForCustomer else 0.0
            val purchasedMinutes = customerPurchases.sumOf { it.durationMinutes.coerceAtLeast(0) }
            val chargeableMinutes = customerPurchases.sumOf(WellOwnerPurchaseMath::chargeableMinutes)

            ComprehensiveCustomerRow(
                customerName = customer.name,
                phone = customer.phone,
                farmName = customer.farmName.ifEmpty { customer.location },
                sessionsCount = customerSessions.size,
                waterMinutes = customerSessions.sumOf { session ->
                    (session.durationMinutes - session.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
                },
                billed = billed,
                paid = customerPaid,
                balance = receivable,
                isWellOwner = customer.isWellOwner,
                purchasedMinutes = purchasedMinutes,
                chargeablePurchasedMinutes = chargeableMinutes,
                ownerWasteMinutes = (purchasedMinutes - chargeableMinutes).coerceAtLeast(0),
                ownerWasteCredit = customerPurchases.sumOf(WellOwnerPurchaseMath::ownerWasteCredit),
                purchaseAmount = purchaseAmount,
                ownerPayments = ownerPaymentsForCustomer,
                payableBalance = payable,
                receivableBalance = receivable
            )
        }.sortedWith(compareByDescending<ComprehensiveCustomerRow> { it.balance }.thenBy { it.customerName })

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
                val allCustomers = db.customerDao().getAllCustomersIncludingArchived().first()
                val allSessions = sessionRepo.allSessions.first()
                val allVouchers = voucherRepo.allVouchers.first()
                val allPurchases = ownerPurchaseRepo.allPurchases.first()
                val snapshot = calculateReportSnapshot(allSessions, allVouchers, allCustomers, allPurchases, selectedPeriod)

                if (allCustomers.isEmpty() && allSessions.isEmpty() && allPurchases.isEmpty()) {
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

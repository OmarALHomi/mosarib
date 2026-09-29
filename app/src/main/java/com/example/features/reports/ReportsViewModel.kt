package com.example.features.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.ComprehensiveCustomerRow
import com.example.core.util.ComprehensiveReportTotals
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.core.util.PdfReportGenerator
import com.example.features.customers.Customer
import com.example.features.customers.CustomerRepository
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionRepository
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherRepository
import com.example.features.vouchers.VoucherType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class ReportPeriod {
    ALL, TODAY, THIS_WEEK, THIS_MONTH
}

data class GeneralReportStats(
    val totalSessionsCount: Int = 0,
    val totalWaterMinutes: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalCollectedCash: Double = 0.0,
    val totalOutstandingDebt: Double = 0.0,
    val totalPumpExpenses: Double = 0.0,
    val netOperatingProfit: Double = 0.0,
    val topCustomers: List<TopCustomerStat> = emptyList()
)

data class TopCustomerStat(
    val customer: Customer,
    val totalMinutes: Int,
    val totalBilled: Double,
    val balance: Double
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionRepo = WaterSessionRepository(db.waterSessionDao(), db.customerDao())
    private val voucherRepo = VoucherRepository(db.voucherDao(), db.customerDao())
    private val customerRepo = CustomerRepository(db.customerDao(), sessionRepo.allSessions, voucherRepo.allVouchers)
    private val settingsRepo = SettingsRepository(db.appSettingDao())

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    private val _period = MutableStateFlow(ReportPeriod.ALL)
    val period: StateFlow<ReportPeriod> = _period.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    val stats: StateFlow<GeneralReportStats> =
        combine(
            sessionRepo.allSessions,
            voucherRepo.allVouchers,
            customerRepo.allCustomers,
            _period
        ) { sessions, vouchers, customers, selectedPeriod ->
            val periodStart = periodStartMillis(selectedPeriod)
            val filteredSessions =
                if (periodStart == null) sessions else sessions.filter { it.startTime >= periodStart }
            val filteredVouchers =
                if (periodStart == null) vouchers else vouchers.filter { it.date >= periodStart }

            val totalSessions = filteredSessions.size
            val totalMinutes = filteredSessions.sumOf { it.durationMinutes }
            val totalRevenue = filteredSessions.sumOf { it.totalAmount }
            val sessionPaid = filteredSessions.sumOf { it.amountPaid }
            val receiptVouchers = filteredVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount }
            val discountVouchers = filteredVouchers.filter { it.type == VoucherType.DISCOUNT }.sumOf { it.amount }
            val totalCollected = sessionPaid + receiptVouchers
            val totalDebts = Math.max(0.0, totalRevenue - (totalCollected + discountVouchers))
            val totalExpenses = filteredVouchers.filter { it.type == VoucherType.EXPENSE }.sumOf { it.amount }
            val netProfit = totalRevenue - totalExpenses

            // Top Customers by Minutes
            val custMap = customers.associateBy { it.id }
            val topList = filteredSessions
                .groupBy { it.customerId }
                .mapNotNull { (custId, sList) ->
                    val cust = custMap[custId]
                    if (cust != null) {
                        val custV = filteredVouchers.filter { it.customerId == custId }
                        val custPaid = sList.sumOf { it.amountPaid } +
                                custV.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount } +
                                custV.filter { it.type == VoucherType.DISCOUNT }.sumOf { it.amount }
                        val custBilled = sList.sumOf { it.totalAmount }
                        TopCustomerStat(
                            customer = cust,
                            totalMinutes = sList.sumOf { it.durationMinutes },
                            totalBilled = custBilled,
                            balance = Math.max(0.0, custBilled - custPaid)
                        )
                    } else null
                }
                .sortedByDescending { it.totalMinutes }
                .take(5)

            GeneralReportStats(
                totalSessionsCount = totalSessions,
                totalWaterMinutes = totalMinutes,
                totalRevenue = totalRevenue,
                totalCollectedCash = totalCollected,
                totalOutstandingDebt = totalDebts,
                totalPumpExpenses = totalExpenses,
                netOperatingProfit = netProfit,
                topCustomers = topList
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneralReportStats())

    fun setPeriod(p: ReportPeriod) {
        _period.value = p
    }

    /**
     * Start (inclusive) timestamp of the given report period, or `null` when the period covers
     * everything. The live statistics and the PDF export both use it so they always agree on what
     * "today", "this week" and "this month" mean.
     */
    private fun periodStartMillis(period: ReportPeriod): Long? {
        if (period == ReportPeriod.ALL) return null
        val calendar = java.util.Calendar.getInstance()
        when (period) {
            ReportPeriod.TODAY -> calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            ReportPeriod.THIS_WEEK -> {
                calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            }
            ReportPeriod.THIS_MONTH -> {
                calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            }
            ReportPeriod.ALL -> Unit
        }
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun periodTitle(period: ReportPeriod): String = when (period) {
        ReportPeriod.ALL -> "كل الفترات"
        ReportPeriod.TODAY -> "اليوم"
        ReportPeriod.THIS_WEEK -> "هذا الأسبوع"
        ReportPeriod.THIS_MONTH -> "هذا الشهر"
    }

    private val _pdfReadyFile = MutableStateFlow<Pair<java.io.File, String>?>(null)
    val pdfReadyFile: StateFlow<Pair<java.io.File, String>?> = _pdfReadyFile.asStateFlow()
    fun clearPdfReady() { _pdfReadyFile.value = null }

    fun showToast(msg: String, type: ToastType = ToastType.SUCCESS) {
        _toast.value = ToastMessage(message = msg, type = type)
    }

    fun dismissToast() {
        _toast.value = null
    }

    /**
     * Exports the real comprehensive accounting report for the currently selected period: the
     * distributor-wide totals plus one row per registered customer.
     *
     * The previous implementation exported a customer statement for `customers.first()` only, so
     * the "comprehensive" report always described one arbitrary customer and silently ignored every
     * other customer, session and voucher.
     */
    fun exportComprehensiveReportPdf() {
        viewModelScope.launch {
            try {
                val config = appConfig.value
                val selectedPeriod = _period.value
                val periodStart = periodStartMillis(selectedPeriod)

                val allCustomers = customerRepo.allCustomers.first()
                val allSessions = sessionRepo.allSessions.first()
                val allVouchers = voucherRepo.allVouchers.first()

                val sessions =
                    if (periodStart == null) allSessions else allSessions.filter { it.startTime >= periodStart }
                val vouchers =
                    if (periodStart == null) allVouchers else allVouchers.filter { it.date >= periodStart }

                if (allCustomers.isEmpty() && sessions.isEmpty()) {
                    showToast("لا توجد بيانات كافية للتصدير", ToastType.WARNING)
                    return@launch
                }

                val totalRevenue = sessions.sumOf { it.totalAmount }
                val totalCollected = sessions.sumOf { it.amountPaid } +
                        vouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount }
                val totalDiscount = vouchers.filter { it.type == VoucherType.DISCOUNT }.sumOf { it.amount }
                val totalExpenses = vouchers.filter { it.type == VoucherType.EXPENSE }.sumOf { it.amount }

                val customerRows = allCustomers
                    .map { customer ->
                        val custSessions = sessions.filter { it.customerId == customer.id }
                        val custVouchers = vouchers.filter { it.customerId == customer.id }
                        val billed = custSessions.sumOf { it.totalAmount }
                        val paid = custSessions.sumOf { it.amountPaid } +
                                custVouchers
                                    .filter { it.type == VoucherType.RECEIPT || it.type == VoucherType.DISCOUNT }
                                    .sumOf { it.amount }
                        ComprehensiveCustomerRow(
                            customerName = customer.name,
                            phone = customer.phone,
                            farmName = customer.farmName,
                            sessionsCount = custSessions.size,
                            waterMinutes = custSessions.sumOf { it.durationMinutes },
                            billed = billed,
                            paid = paid,
                            balance = Math.max(0.0, billed - paid)
                        )
                    }
                    .sortedByDescending { it.balance }

                val totals = ComprehensiveReportTotals(
                    sessionsCount = sessions.size,
                    waterMinutes = sessions.sumOf { it.durationMinutes },
                    totalRevenue = totalRevenue,
                    totalCollected = totalCollected,
                    totalOutstandingDebt = Math.max(0.0, totalRevenue - (totalCollected + totalDiscount)),
                    totalExpenses = totalExpenses,
                    netProfit = totalRevenue - totalExpenses
                )

                val file = PdfReportGenerator.generateComprehensiveReportPdf(
                    context = getApplication(),
                    config = config,
                    periodTitle = periodTitle(selectedPeriod),
                    totals = totals,
                    customerRows = customerRows
                )
                _pdfReadyFile.value = Pair(file, "التقرير المحاسبي الشامل - ${periodTitle(selectedPeriod)}")
            } catch (e: Exception) {
                showToast("فشل في إنشاء التقرير: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }
}

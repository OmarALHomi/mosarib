package com.baynana.features.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baynana.core.database.AppDatabase
import com.baynana.core.ui.ToastMessage
import com.baynana.core.ui.ToastType
import com.baynana.core.util.FileSharingHelper
import com.baynana.core.util.Formatters
import com.baynana.core.util.PdfReportGenerator
import com.baynana.features.customers.Customer
import com.baynana.features.customers.CustomerRepository
import com.baynana.features.pumps.PumpSource
import com.baynana.features.pumps.PumpSourceRepository
import com.baynana.features.settings.AppConfig
import com.baynana.features.settings.SettingsRepository
import com.baynana.features.vouchers.Voucher
import com.baynana.features.vouchers.VoucherRepository
import com.baynana.features.vouchers.VoucherType
import com.baynana.core.license.LicenseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class HomeDashboardStats(
    val totalWaterMinutes: Int = 0,
    val totalSessionsCount: Int = 0,
    val totalCollectedCash: Double = 0.0,
    val totalOutstandingDebt: Double = 0.0,
    val totalExpenses: Double = 0.0
)

enum class SessionFilter {
    ALL, TODAY, THIS_WEEK, THIS_MONTH
}

class SessionsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionRepo = WaterSessionRepository(db.waterSessionDao(), db.customerDao())
    private val customerRepo = CustomerRepository(db.customerDao(), sessionRepo.allSessions, db.voucherDao().getAllVouchers())
    private val pumpRepo = PumpSourceRepository(db.pumpSourceDao())
    private val settingsRepo = SettingsRepository(db.appSettingDao())
    private val voucherRepo = VoucherRepository(db.voucherDao(), db.customerDao())

    val operationsCount: StateFlow<Int> = combine(
        db.waterSessionDao().getSessionsCount(),
        db.voucherDao().getVouchersCount()
    ) { s, v -> s + v }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    val customers: StateFlow<List<Customer>> = customerRepo.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pumps: StateFlow<List<PumpSource>> = pumpRepo.allPumps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filter = MutableStateFlow(SessionFilter.ALL)
    val filter: StateFlow<SessionFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    /** ملف PDF جاهز — يُعرض dialog للمستخدم يختار فيه فتح أو مشاركة */
    private val _pdfReadyFile = MutableStateFlow<Pair<File, String>?>(null)
    val pdfReadyFile: StateFlow<Pair<File, String>?> = _pdfReadyFile.asStateFlow()

    fun clearPdfReady() { _pdfReadyFile.value = null }

    val allVouchers: StateFlow<List<Voucher>> = voucherRepo.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeStats: StateFlow<HomeDashboardStats> =
        combine(
            sessionRepo.allSessions,
            voucherRepo.allVouchers
        ) { sessions, vouchers ->
            val totalMinutes = sessions.sumOf { it.durationMinutes }
            val totalSessions = sessions.size
            val sessionPaid = sessions.sumOf { it.amountPaid }
            val receiptVouchers = vouchers.filter { it.type == VoucherType.RECEIPT && it.sessionId == null }.sumOf { it.amount }
            val discountVouchers = vouchers.filter { it.type == VoucherType.DISCOUNT && it.sessionId == null }.sumOf { it.amount }
            val totalBilled = sessions.sumOf { it.totalAmount }
            val totalCollected = sessionPaid + receiptVouchers
            val totalDebts = Math.max(0.0, totalBilled - (totalCollected + discountVouchers))
            val totalExpenses = vouchers.filter { it.type == VoucherType.EXPENSE }.sumOf { it.amount }

            HomeDashboardStats(
                totalWaterMinutes = totalMinutes,
                totalSessionsCount = totalSessions,
                totalCollectedCash = totalCollected,
                totalOutstandingDebt = totalDebts,
                totalExpenses = totalExpenses
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeDashboardStats())

    val filteredSessions: StateFlow<List<WaterSessionWithCustomer>> =
        combine(sessionRepo.sessionsWithCustomer, _filter, _searchQuery) { list, filterType, query ->
            val now = System.currentTimeMillis()
            val calendar = java.util.Calendar.getInstance()

            val timeFiltered = when (filterType) {
                SessionFilter.ALL -> list
                SessionFilter.TODAY -> {
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    val startOfDay = calendar.timeInMillis
                    list.filter { it.session.startTime >= startOfDay }
                }
                SessionFilter.THIS_WEEK -> {
                    calendar.set(java.util.Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    val startOfWeek = calendar.timeInMillis
                    list.filter { it.session.startTime >= startOfWeek }
                }
                SessionFilter.THIS_MONTH -> {
                    calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    val startOfMonth = calendar.timeInMillis
                    list.filter { it.session.startTime >= startOfMonth }
                }
            }

            if (query.isBlank()) {
                timeFiltered
            } else {
                timeFiltered.filter {
                    (it.customer?.name?.contains(query, ignoreCase = true) == true) ||
                    (it.customer?.farmName?.contains(query, ignoreCase = true) == true) ||
                    (it.session.pumpName.contains(query, ignoreCase = true)) ||
                    (it.session.notes.contains(query, ignoreCase = true))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(f: SessionFilter) {
        _filter.value = f
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun showToast(msg: String, type: ToastType = ToastType.SUCCESS) {
        _toast.value = ToastMessage(message = msg, type = type)
    }

    fun dismissToast() {
        _toast.value = null
    }

    suspend fun createCustomer(customer: Customer): Long {
        val id = customerRepo.insertCustomer(customer)
        showToast("تمت إضافة العميل بنجاح", ToastType.SUCCESS)
        return id
    }

    /**
     * Add Manual Water Distribution Session
     */
    fun saveManualSession(
        id: Long = 0,
        customerId: Long,
        pumpName: String = "",
        startTime: Long,
        endTime: Long,
        hours: Int,
        minutes: Int,
        pricePerHour: Double,
        amountPaid: Double,
        notes: String,
        billedToCustomerId: Long? = null
    ) {
        viewModelScope.launch {
            val totalMinutes = (hours * 60) + minutes
            val totalCost = Formatters.calculateWaterCost(totalMinutes, pricePerHour)
            val roundedPaid = Formatters.roundMoney(amountPaid)
            val debt = Formatters.roundMoney(Math.max(0.0, totalCost - roundedPaid))

            val session = WaterSession(
                id = id,
                customerId = customerId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = if (endTime > startTime) endTime else (startTime + totalMinutes * 60000L),
                durationMinutes = totalMinutes,
                pricePerHour = Formatters.roundMoney(pricePerHour),
                totalAmount = totalCost,
                amountPaid = roundedPaid,
                remainingDebt = debt,
                notes = notes,
                isLive = false,
                billedToCustomerId = billedToCustomerId
            )

            val cust = db.customerDao().getCustomerByIdDirect(session.customerId)
            if (id == 0L) {
                val totalOps = db.waterSessionDao().getSessionsCountDirect() + db.voucherDao().getVouchersCountDirect()
                if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                    showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                    return@launch
                }
                val newId = sessionRepo.insertSession(session)
                if (cust != null && cust.linkCode.isNotBlank()) {
                    com.baynana.core.sync.MusribSyncManager.syncSession(cust.linkCode, session.copy(id = newId))
                }
                showToast("تم تسجيل دورة الماء وحساب التكلفة بنجاح", ToastType.SUCCESS)
            } else {
                sessionRepo.updateSession(session)
                if (cust != null && cust.linkCode.isNotBlank()) {
                    com.baynana.core.sync.MusribSyncManager.syncSession(cust.linkCode, session)
                }
                showToast("تم تحديث بيانات دورة الماء بنجاح", ToastType.SUCCESS)
            }
        }
    }

    /**
     * سداد المبلغ المؤخر لجلسة سقي وإصدار سند قبض مرتبط
     */
    fun settleSessionDebt(
        session: WaterSession,
        amountToPay: Double,
        paymentMethod: String = "نقداً",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() + db.voucherDao().getVouchersCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val roundedPayment = Formatters.roundMoney(amountToPay)
            val newAmountPaid = Formatters.roundMoney(session.amountPaid + roundedPayment)
            val newRemainingDebt = Formatters.roundMoney(Math.max(0.0, session.totalAmount - newAmountPaid))
            val updated = session.copy(
                amountPaid = newAmountPaid,
                remainingDebt = newRemainingDebt
            )
            sessionRepo.updateSession(updated)

            // توليد سند قبض رسمي مرتبط برقم الجلسة
            val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
            val voucher = Voucher(
                voucherNumber = vNumber,
                type = VoucherType.RECEIPT,
                customerId = session.billedToCustomerId ?: session.customerId,
                sessionId = session.id,
                amount = roundedPayment,
                category = "سداد سقي",
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                notes = notes.ifBlank { "سداد دورة سقي #${session.id}" }
            )
            voucherRepo.insertVoucher(voucher)
            showToast("تم سداد المبلغ بنجاح وإصدار سند القبض المرتبط", ToastType.SUCCESS)
        }
    }

    fun deleteSession(session: WaterSession) {
        viewModelScope.launch {
            val cust = db.customerDao().getCustomerByIdDirect(session.customerId)
            sessionRepo.deleteSession(session)
            if (cust != null && cust.linkCode.isNotBlank()) {
                com.baynana.core.sync.MusribSyncManager.deleteEntry(cust.linkCode, "session_${session.id}")
            }
            showToast("تم حذف الجلسة بنجاح", ToastType.INFO)
        }
    }

    /**
     * إنشاء PDF الفاتورة ثم إظهار dialog (فتح / مشاركة)
     */
    fun generateAndShareInvoice(session: WaterSession, customer: Customer) {
        viewModelScope.launch {
            try {
                val config = appConfig.value
                val pdfFile: File = PdfReportGenerator.generateSessionInvoicePdf(
                    context = getApplication(),
                    config = config,
                    customer = customer,
                    session = session
                )
                _pdfReadyFile.value = Pair(pdfFile, "فاتورة ري مياه - ${customer.name}")
            } catch (e: Exception) {
                showToast("فشل في إنشاء ملف الفاتورة: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    /** إرسال الفاتورة عبر واتساب (مع تصحيح كود اليمن +967 تلقائياً) */
    fun sendWhatsAppBill(session: WaterSession, customer: Customer) {
        val config = appConfig.value
        val msg = buildBillMessage(session, customer, config)
        FileSharingHelper.sendWhatsAppMessage(getApplication(), customer.phone, msg)
    }

    /** إرسال الفاتورة عبر SMS */
    fun sendSmsBill(session: WaterSession, customer: Customer) {
        val config = appConfig.value
        val msg = buildBillMessage(session, customer, config)
        FileSharingHelper.sendSms(getApplication(), customer.phone, msg)
    }

    fun buildBillMessage(session: WaterSession, customer: Customer, config: com.baynana.features.settings.AppConfig = appConfig.value): String {
        val timeRange = "من ${Formatters.formatTime(session.startTime)} إلى ${Formatters.formatTime(session.endTime)}"
        val debtStatus = if (session.remainingDebt > 0) "المتبقي: ${Formatters.formatCurrency(session.remainingDebt, config.currencySymbol)}" else "خالص ومسدد"
        return """
*فاتورة ري - ${config.distributorName.ifEmpty { "المُسَرِّب" }}*
👤 العميل: ${customer.name}${if (customer.farmName.isNotEmpty()) " (${customer.farmName})" else ""}
⏱️ الوقت: $timeRange (${Formatters.formatDurationArabic(session.durationMinutes)})
💵 المبلغ: ${Formatters.formatCurrency(session.totalAmount, config.currencySymbol)} | مسدد: ${Formatters.formatCurrency(session.amountPaid, config.currencySymbol)}
📊 الحالة: $debtStatus
📅 التاريخ: ${Formatters.formatDate(session.startTime)}${FileSharingHelper.MESSAGE_FOOTER}
        """.trimIndent()
    }
}

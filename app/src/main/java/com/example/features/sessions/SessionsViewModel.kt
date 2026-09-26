package com.example.features.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.core.util.PdfReportGenerator
import com.example.features.customers.Customer
import com.example.features.customers.CustomerRepository
import com.example.features.pumps.PumpSource
import com.example.features.pumps.PumpSourceRepository
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherRepository
import com.example.features.vouchers.VoucherType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class LiveTimerState(
    val isRunning: Boolean = false,
    val sessionId: Long = 0,
    val customerId: Long = 0,
    val customerName: String = "",
    val pumpName: String = "",
    val pricePerHour: Double = 5000.0,
    val startTimestamp: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val currentCost: Double = 0.0
)

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

    private val _liveTimerState = MutableStateFlow(LiveTimerState())
    val liveTimerState: StateFlow<LiveTimerState> = _liveTimerState.asStateFlow()

    private var timerJob: Job? = null

    init {
        // Check for active live session in database
        viewModelScope.launch {
            val active = sessionRepo.getActiveLiveSessionDirect()
            if (active != null) {
                val customer = customerRepo.getCustomerByIdDirect(active.customerId)
                startTimerTicker(
                    sessionId = active.id,
                    customerId = active.customerId,
                    customerName = customer?.name ?: "عميل",
                    pumpName = active.pumpName,
                    pricePerHour = active.pricePerHour,
                    startTime = active.startTime
                )
            }
        }
    }

    val homeStats: StateFlow<HomeDashboardStats> =
        combine(
            sessionRepo.allSessions,
            voucherRepo.allVouchers
        ) { sessions, vouchers ->
            val totalMinutes = sessions.sumOf { it.durationMinutes }
            val totalSessions = sessions.size
            val sessionPaid = sessions.sumOf { it.amountPaid }
            val receiptVouchers = vouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount }
            val discountVouchers = vouchers.filter { it.type == VoucherType.DISCOUNT }.sumOf { it.amount }
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

    /**
     * Start Live Irrigation Timer
     */
    fun startLiveSession(customerId: Long, customerName: String, pumpName: String, pricePerHour: Double) {
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val session = WaterSession(
                customerId = customerId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = startTime,
                durationMinutes = 0,
                pricePerHour = pricePerHour,
                totalAmount = 0.0,
                amountPaid = 0.0,
                remainingDebt = 0.0,
                notes = "ساقية ماء جارية (مباشر)",
                isLive = true
            )
            val sessionId = sessionRepo.insertSession(session)
            startTimerTicker(sessionId, customerId, customerName, pumpName, pricePerHour, startTime)
            showToast("تم بدء عداد تشغيل وسقي الماء بنجاح", ToastType.INFO)
        }
    }

    private fun startTimerTicker(
        sessionId: Long,
        customerId: Long,
        customerName: String,
        pumpName: String,
        pricePerHour: Double,
        startTime: Long
    ) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsedSec = (now - startTime) / 1000
                val elapsedMinutes = (elapsedSec / 60).toInt()
                val cost = (elapsedMinutes.toDouble() / 60.0) * pricePerHour

                _liveTimerState.value = LiveTimerState(
                    isRunning = true,
                    sessionId = sessionId,
                    customerId = customerId,
                    customerName = customerName,
                    pumpName = pumpName,
                    pricePerHour = pricePerHour,
                    startTimestamp = startTime,
                    elapsedSeconds = elapsedSec,
                    currentCost = cost
                )
                delay(1000)
            }
        }
    }

    /**
     * Stop Live Session and finalize calculation & debt
     */
    fun stopAndSaveLiveSession(amountPaid: Double, notes: String) {
        val currentState = _liveTimerState.value
        if (!currentState.isRunning) return

        viewModelScope.launch {
            timerJob?.cancel()
            val endTime = System.currentTimeMillis()
            val totalMinutes = Math.max(1, Math.round((endTime - currentState.startTimestamp) / 60000.0).toInt())
            val totalCost = (totalMinutes.toDouble() / 60.0) * currentState.pricePerHour
            val debt = Math.max(0.0, totalCost - amountPaid)

            val updatedSession = WaterSession(
                id = currentState.sessionId,
                customerId = currentState.customerId,
                pumpName = currentState.pumpName,
                startTime = currentState.startTimestamp,
                endTime = endTime,
                durationMinutes = totalMinutes,
                pricePerHour = currentState.pricePerHour,
                totalAmount = totalCost,
                amountPaid = amountPaid,
                remainingDebt = debt,
                notes = notes.ifBlank { "ساقية ماء مكتملة" },
                isLive = false
            )

            sessionRepo.updateSession(updatedSession)

            _liveTimerState.value = LiveTimerState(isRunning = false)
            showToast("تم إيقاف العداد وحفظ جلسة الري وترحيل الحساب بنجاح", ToastType.SUCCESS)
        }
    }

    fun cancelLiveSession() {
        val currentState = _liveTimerState.value
        viewModelScope.launch {
            timerJob?.cancel()
            if (currentState.sessionId > 0) {
                sessionRepo.deleteSessionById(currentState.sessionId)
            }
            _liveTimerState.value = LiveTimerState(isRunning = false)
            showToast("تم إلغاء جلسة الري الحالية", ToastType.WARNING)
        }
    }

    /**
     * Add Manual Water Distribution Session
     */
    fun saveManualSession(
        id: Long = 0,
        customerId: Long,
        pumpName: String,
        startTime: Long,
        endTime: Long,
        hours: Int,
        minutes: Int,
        pricePerHour: Double,
        amountPaid: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val totalMinutes = (hours * 60) + minutes
            val totalCost = (totalMinutes.toDouble() / 60.0) * pricePerHour
            val debt = Math.max(0.0, totalCost - amountPaid)

            val session = WaterSession(
                id = id,
                customerId = customerId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = if (endTime > startTime) endTime else (startTime + totalMinutes * 60000L),
                durationMinutes = totalMinutes,
                pricePerHour = pricePerHour,
                totalAmount = totalCost,
                amountPaid = amountPaid,
                remainingDebt = debt,
                notes = notes,
                isLive = false
            )

            if (id == 0L) {
                sessionRepo.insertSession(session)
                showToast("تم تسجيل دورة الماء وحساب التكلفة بنجاح", ToastType.SUCCESS)
            } else {
                sessionRepo.updateSession(session)
                showToast("تم تحديث بيانات دورة الماء بنجاح", ToastType.SUCCESS)
            }
        }
    }

    fun deleteSession(session: WaterSession) {
        viewModelScope.launch {
            sessionRepo.deleteSession(session)
            showToast("تم حذف الجلسة بنجاح", ToastType.INFO)
        }
    }

    /**
     * PDF Invoice Generation & Share
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
                FileSharingHelper.sharePdf(getApplication(), pdfFile, "فاتورة ري مياه للعميل ${customer.name}")
            } catch (e: Exception) {
                showToast("فشل في إنشاء ملف الفاتورة: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    /**
     * Send Bill details via WhatsApp
     */
    fun sendWhatsAppBill(session: WaterSession, customer: Customer) {
        val config = appConfig.value
        val msg = """
            *فاتورة توزيع مياه - ${config.distributorName}*
            👤 العميل: ${customer.name}
            📍 المزرعة: ${customer.farmName.ifEmpty { "عام" }}
            ⏱️ المدة: ${Formatters.formatDurationArabic(session.durationMinutes)} (${Formatters.formatDurationShort(session.durationMinutes)})
            💰 سعر الساعة: ${Formatters.formatCurrency(session.pricePerHour, config.currencySymbol)}
            💵 الإجمالي: ${Formatters.formatCurrency(session.totalAmount, config.currencySymbol)}
            ✅ المدفوع: ${Formatters.formatCurrency(session.amountPaid, config.currencySymbol)}
            ⚠️ المتبقي: ${Formatters.formatCurrency(session.remainingDebt, config.currencySymbol)}
            📅 التاريخ: ${Formatters.formatDateTime(session.startTime)}
            ------------------------
            شكراً لتعاملكم معنا.
        """.trimIndent()
        FileSharingHelper.sendWhatsAppMessage(getApplication(), customer.phone, msg)
    }
}

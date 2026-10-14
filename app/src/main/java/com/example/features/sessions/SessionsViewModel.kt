package com.example.features.sessions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.core.util.MessageFormatHelper
import com.example.core.util.PdfReportGenerator
import com.example.features.customers.Customer
import com.example.features.customers.CustomerRepository
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherRepository
import com.example.features.vouchers.VoucherType
import com.example.core.license.LicenseManager
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
    private val customerRepo = CustomerRepository(
        db.customerDao(),
        sessionRepo.allSessions,
        db.voucherDao().getAllVouchers(),
        db.pumpSourceDao().getAllPumps(),
        db.wellOwnerPurchaseDao().getAllPurchases()
    )
    private val settingsRepo = SettingsRepository(db.appSettingDao())
    private val voucherRepo = VoucherRepository(db.voucherDao(), db.customerDao())

    val operationsCount: StateFlow<Int> = combine(
        db.waterSessionDao().getSessionsCount(),
        db.voucherDao().getVouchersCount(),
        db.wellOwnerPurchaseDao().getPurchasesCount()
    ) { sessions, vouchers, purchases -> sessions + vouchers + purchases }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    val customers: StateFlow<List<Customer>> = customerRepo.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filter = MutableStateFlow(SessionFilter.ALL)
    val filter: StateFlow<SessionFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private val _requestedCustomerForNewSession = MutableStateFlow<Long?>(null)
    val requestedCustomerForNewSession: StateFlow<Long?> = _requestedCustomerForNewSession.asStateFlow()

    fun requestNewSessionForCustomer(customerId: Long) {
        _requestedCustomerForNewSession.value = customerId
    }

    fun clearNewSessionCustomerRequest() {
        _requestedCustomerForNewSession.value = null
    }

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
        billedToCustomerId: Long? = null,
        wastedMinutes: Int = 0,
        wastedReason: String = "",
        discountAmount: Double = 0.0,
        costPricePerHour: Double = 0.0,
        pumpSourceId: Long? = null
    ) {
        viewModelScope.launch {
            val totalMinutes = (hours * 60) + minutes
            if (totalMinutes <= 0) {
                showToast("يرجى تحديد مدة سقي صالحة", ToastType.ERROR)
                return@launch
            }
            val maxSessionMinutes = 10 * 24 * 60 // 10 days = 14,400 minutes
            if (totalMinutes > maxSessionMinutes) {
                showToast("لا يمكن تسجيل سقي بساعات خيالية! الحد الأقصى للدورة الواحدة 10 أيام (240 ساعة)", ToastType.ERROR)
                return@launch
            }
            if (wastedMinutes > totalMinutes) {
                showToast("لا يمكن أن يكون الهدر أكبر من مدة السقي بتاتاً", ToastType.ERROR)
                return@launch
            }
            val netMinutes = maxOf(0, totalMinutes - wastedMinutes)
            val grossCost = Formatters.calculateWaterCost(netMinutes, pricePerHour)
            val totalCost = Formatters.roundMoney(maxOf(0.0, grossCost - discountAmount))
            val roundedPaid = Formatters.roundMoney(amountPaid)
            val isOwnerOrBilled = billedToCustomerId != null ||
                db.customerDao().getCustomerByIdDirect(customerId)?.isWellOwner == true

            val excessPaid = if (!isOwnerOrBilled && roundedPaid > totalCost) {
                Formatters.roundMoney(roundedPaid - totalCost)
            } else 0.0

            val effectivePaidForSession = if (excessPaid > 0.0) totalCost else roundedPaid
            val debt = if (isOwnerOrBilled) 0.0 else Formatters.roundMoney(maxOf(0.0, totalCost - effectivePaidForSession))

            val session = WaterSession(
                id = id,
                customerId = customerId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = if (endTime > startTime) endTime else (startTime + totalMinutes * 60000L),
                durationMinutes = totalMinutes,
                pricePerHour = Formatters.roundMoney(pricePerHour),
                totalAmount = totalCost,
                amountPaid = effectivePaidForSession,
                remainingDebt = debt,
                notes = notes,
                isLive = false,
                billedToCustomerId = billedToCustomerId,
                wastedMinutes = wastedMinutes,
                wastedReason = wastedReason,
                discountAmount = discountAmount,
                costPricePerHour = costPricePerHour,
                pumpSourceId = pumpSourceId
            )

            val savedSessionId = if (id == 0L) {
                val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                    db.voucherDao().getVouchersCountDirect() + db.wellOwnerPurchaseDao().getPurchasesCountDirect()
                if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                    showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                    return@launch
                }
                val newId = sessionRepo.insertSession(session)
                LicenseManager.recordOperationPerformed(getApplication(), totalOps)
                newId
            } else {
                sessionRepo.updateSession(session)
                id
            }

            if (excessPaid > 0.0) {
                val unpaidSessions = db.waterSessionDao()
                    .getUnpaidSessionsForCustomerDirect(customerId)
                    .filter { it.id != savedSessionId }

                var remainingExcess = excessPaid
                val settledSessionIds = mutableListOf<Long>()
                for (oldSession in unpaidSessions) {
                    if (remainingExcess <= 0.0) break
                    val settleAmt = minOf(remainingExcess, oldSession.remainingDebt)
                    val newOldPaid = Formatters.roundMoney(oldSession.amountPaid + settleAmt)
                    val newOldDebt = Formatters.roundMoney(maxOf(0.0, oldSession.totalAmount - newOldPaid))
                    sessionRepo.updateSession(oldSession.copy(amountPaid = newOldPaid, remainingDebt = newOldDebt))
                    remainingExcess = Formatters.roundMoney(remainingExcess - settleAmt)
                    settledSessionIds.add(oldSession.id)
                }

                val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
                val notesText = if (settledSessionIds.isNotEmpty()) {
                    val listStr = settledSessionIds.joinToString(", ") { "#$it" }
                    if (remainingExcess > 0.0) {
                        "سند قبض: سداد ديون سابقة لجلسات ($listStr) بمبلغ ${Formatters.formatCurrency(excessPaid - remainingExcess, "")} ومتبقي ${Formatters.formatCurrency(remainingExcess, "")} رصيد دائن للعميل (فائض نقدي من سقي #$savedSessionId)"
                    } else {
                        "سند قبض: سداد ديون سابقة لجلسات ($listStr) بالكامل من فائض نقدي لسقي #$savedSessionId"
                    }
                } else {
                    "سند قبض: رصيد دائن لصالح العميل (فائض نقدي من سقي #$savedSessionId)"
                }

                val voucher = Voucher(
                    voucherNumber = vNumber,
                    type = VoucherType.RECEIPT,
                    customerId = customerId,
                    sessionId = savedSessionId,
                    amount = excessPaid,
                    category = if (settledSessionIds.isNotEmpty()) "سداد ديون سابقة" else "رصيد دائن للعميل",
                    paymentMethod = "نقداً",
                    date = System.currentTimeMillis(),
                    notes = notesText
                )
                db.voucherDao().insertVoucher(voucher)
                showToast("تم تسجيل السقي وسند قبض بالفائض (${Formatters.formatCurrency(excessPaid, "")}) وتسوية الحساب بنجاح", ToastType.SUCCESS)
            } else {
                if (id == 0L) {
                    showToast("تم تسجيل دورة السقي وحساب التكلفة بنجاح", ToastType.SUCCESS)
                } else {
                    showToast("تم تحديث بيانات دورة السقي بنجاح", ToastType.SUCCESS)
                }
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
        notes: String = "",
        discountAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                    db.voucherDao().getVouchersCountDirect() + db.wellOwnerPurchaseDao().getPurchasesCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val roundedPayment = Formatters.roundMoney(amountToPay)
            val roundedDiscount = Formatters.roundMoney(discountAmount)
            val newAmountPaid = Formatters.roundMoney(session.amountPaid + roundedPayment)
            val newDiscount = Formatters.roundMoney(session.discountAmount + roundedDiscount)
            val newRemainingDebt = Formatters.roundMoney((session.totalAmount - newAmountPaid - newDiscount).coerceAtLeast(0.0))
            val updated = session.copy(
                amountPaid = newAmountPaid,
                discountAmount = newDiscount,
                remainingDebt = newRemainingDebt
            )
            sessionRepo.updateSession(updated)

            val baseTime = System.currentTimeMillis()
            var opsAdded = 0

            // توليد سند قبض رسمي مرتبط برقم الجلسة للمبلغ المدفوع
            if (roundedPayment > 0.0) {
                val vNumber = "REC-${baseTime.toString().takeLast(4)}"
                val voucher = Voucher(
                    voucherNumber = vNumber,
                    type = VoucherType.RECEIPT,
                    customerId = session.billedToCustomerId ?: session.customerId,
                    sessionId = session.id,
                    amount = roundedPayment,
                    category = "سداد سقي",
                    paymentMethod = paymentMethod,
                    date = baseTime,
                    notes = notes.ifBlank { "سداد دورة سقي #${session.id}" }
                )
                voucherRepo.insertVoucher(voucher)
                opsAdded++
            }

            // توليد سند خصم ومسامحة للتسوية إن وجد
            if (roundedDiscount > 0.0) {
                val dNumber = "DSC-${baseTime.toString().takeLast(4)}"
                val discVoucher = Voucher(
                    voucherNumber = dNumber,
                    type = VoucherType.DISCOUNT,
                    customerId = session.billedToCustomerId ?: session.customerId,
                    sessionId = session.id,
                    amount = roundedDiscount,
                    category = "خصم تسوية",
                    paymentMethod = "تسوية ومسامحة",
                    date = baseTime + 1,
                    notes = "خصم ومسامحة تسوية دورة سقي #${session.id}"
                )
                voucherRepo.insertVoucher(discVoucher)
                opsAdded++
            }

            if (opsAdded > 0) {
                LicenseManager.recordOperationPerformed(getApplication(), totalOps)
            }
            showToast("تم سداد وتسوية حساب الجلسة بنجاح", ToastType.SUCCESS)
        }
    }

    fun deleteSession(session: WaterSession) {
        viewModelScope.launch {
            val sessionVouchers = voucherRepo.getVouchersForSessionDirect(session.id)
            for (v in sessionVouchers) {
                voucherRepo.updateVoucher(
                    v.copy(sessionId = null, notes = "${v.notes} (دفعة على الحساب بعد حذف دورة #${session.id})".trim())
                )
            }
            sessionRepo.deleteSession(session)
            showToast("تم حذف دورة السقي وتحديث الحساب", ToastType.INFO)
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
        val msg = MessageFormatHelper.buildSessionBill(session, customer, config, isSms = false)
        FileSharingHelper.sendWhatsAppMessage(getApplication(), customer.phone, msg)
    }

    /** إرسال الفاتورة عبر SMS */
    fun sendSmsBill(session: WaterSession, customer: Customer) {
        val config = appConfig.value
        val msg = MessageFormatHelper.buildSessionBill(session, customer, config, isSms = true)
        FileSharingHelper.sendSms(getApplication(), customer.phone, msg)
    }

    fun buildBillMessage(session: WaterSession, customer: Customer, config: com.example.features.settings.AppConfig = appConfig.value, isSms: Boolean = false): String {
        return MessageFormatHelper.buildSessionBill(session, customer, config, isSms)
    }
}

package com.example.features.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.license.LicenseManager
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.FileSharingHelper
import com.example.core.util.PdfReportGenerator
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionRepository
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherRepository
import com.example.features.vouchers.VoucherType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class CustomerSort {
    NAME, HIGHEST_DEBT, MOST_WATER_HOURS
}

enum class AccountFilter {
    ALL, FARMERS, WELL_OWNERS
}

class CustomersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionRepo = WaterSessionRepository(db.waterSessionDao(), db.customerDao())
    private val voucherRepo = VoucherRepository(db.voucherDao(), db.customerDao())
    private val customerRepo = CustomerRepository(
        db.customerDao(),
        sessionRepo.allSessions,
        voucherRepo.allVouchers,
        db.pumpSourceDao().getAllPumps()
    )
    private val settingsRepo = SettingsRepository(db.appSettingDao())

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(CustomerSort.NAME)
    val sortType: StateFlow<CustomerSort> = _sortType.asStateFlow()

    private val _accountFilter = MutableStateFlow(AccountFilter.ALL)
    val accountFilter: StateFlow<AccountFilter> = _accountFilter.asStateFlow()

    fun setAccountFilter(filter: AccountFilter) {
        _accountFilter.value = filter
    }

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    /** ملف PDF جاهز — يُعرض dialog للمستخدم يختار فيه فتح أو مشاركة */
    private val _pdfReadyFile = MutableStateFlow<Pair<File, String>?>(null)
    val pdfReadyFile: StateFlow<Pair<File, String>?> = _pdfReadyFile.asStateFlow()
    fun clearPdfReady() { _pdfReadyFile.value = null }

    val rawCustomersWithBalance: StateFlow<List<CustomerWithBalance>> =
        customerRepo.customersWithBalance
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> =
        combine(customerRepo.customersWithBalance, _searchQuery, _sortType, _accountFilter) { list, query, sort, filter ->
            val byFilter = when (filter) {
                AccountFilter.ALL -> list
                AccountFilter.FARMERS -> list.filter { !it.customer.isWellOwner }
                AccountFilter.WELL_OWNERS -> list.filter { it.customer.isWellOwner }
            }
            val filtered = if (query.isBlank()) byFilter else {
                byFilter.filter {
                    it.customer.name.contains(query, ignoreCase = true) ||
                    it.customer.phone.contains(query, ignoreCase = true) ||
                    it.customer.farmName.contains(query, ignoreCase = true) ||
                    it.customer.location.contains(query, ignoreCase = true)
                }
            }

            when (sort) {
                CustomerSort.NAME -> filtered.sortedBy { it.customer.name }
                CustomerSort.HIGHEST_DEBT -> filtered.sortedByDescending { it.balance }
                CustomerSort.MOST_WATER_HOURS -> filtered.sortedByDescending { it.totalMinutes }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortType(sort: CustomerSort) {
        _sortType.value = sort
    }

    fun showToast(msg: String, type: ToastType = ToastType.SUCCESS) {
        _toast.value = ToastMessage(message = msg, type = type)
    }

    fun dismissToast() {
        _toast.value = null
    }

    val allCustomers: Flow<List<Customer>> = customerRepo.allCustomers
    val allVouchers: Flow<List<Voucher>> = voucherRepo.allVouchers

    fun saveCustomer(
        id: Long = 0,
        name: String,
        phone: String,
        farmName: String,
        location: String,
        notes: String,
        customPricePerHour: Double?,
        isBeneficiary: Boolean = false,
        isWellOwner: Boolean = false
    ) {
        viewModelScope.launch {
            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                farmName = farmName.trim(),
                location = location.trim(),
                notes = notes.trim(),
                customPricePerHour = customPricePerHour,
                isBeneficiary = isBeneficiary,
                isWellOwner = isWellOwner
            )
            if (id == 0L) {
                customerRepo.insertCustomer(customer)
                showToast(if (isWellOwner) "تمت إضافة حساب صاحب البئر بنجاح" else "تمت إضافة العميل بنجاح", ToastType.SUCCESS)
            } else {
                customerRepo.updateCustomer(customer)
                showToast("تم تعديل بيانات الحساب بنجاح", ToastType.SUCCESS)
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            customerRepo.deleteCustomer(customer)
            showToast("تم حذف العميل بنجاح", ToastType.INFO)
        }
    }

    fun getCustomerSessions(customerId: Long): Flow<List<WaterSession>> =
        combine(sessionRepo.allSessions, db.customerDao().getCustomerById(customerId), db.pumpSourceDao().getAllPumps()) { sessions, cust, pumps ->
            if (cust != null && cust.isWellOwner) {
                val ownedPumpIds = pumps.filter { it.ownerCustomerId == cust.id }.map { it.id }.toSet()
                sessions.filter {
                    (it.pumpSourceId != null && ownedPumpIds.contains(it.pumpSourceId)) ||
                    (pumps.any { p -> p.ownerCustomerId == cust.id && p.name == it.pumpName })
                }
            } else {
                sessions.filter {
                    (it.billedToCustomerId == customerId) || (it.customerId == customerId && it.billedToCustomerId == null)
                }
            }
        }

    fun getCustomerVouchers(customerId: Long): Flow<List<Voucher>> =
        voucherRepo.getVouchersForCustomer(customerId)

    fun addReceiptVoucher(customerId: Long, amount: Double, paymentMethod: String, notes: String) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() + db.voucherDao().getVouchersCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
            val voucher = Voucher(
                voucherNumber = vNumber,
                type = VoucherType.RECEIPT,
                customerId = customerId,
                amount = amount,
                category = "سداد حساب",
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                notes = notes
            )
            voucherRepo.insertVoucher(voucher)
            showToast("تم تسجيل سند القبض وتحديث رصيد العميل", ToastType.SUCCESS)
        }
    }

    fun addExpenseVoucher(customerId: Long, amount: Double, paymentMethod: String, description: String) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() + db.voucherDao().getVouchersCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val vNumber = "EXP-${System.currentTimeMillis().toString().takeLast(4)}"
            val voucher = Voucher(
                voucherNumber = vNumber,
                type = VoucherType.EXPENSE,
                customerId = customerId,
                amount = amount,
                category = description.ifBlank { "صرف للعميل / المستفيد" },
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                notes = description
            )
            voucherRepo.insertVoucher(voucher)
            showToast("تم قيد سند الصرف وتحديث رصيد العميل", ToastType.SUCCESS)
        }
    }

    fun settleSessionDebt(
        session: WaterSession,
        amount: Double,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() + db.voucherDao().getVouchersCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val newAmountPaid = (session.amountPaid + amount).coerceAtMost(session.totalAmount)
            val newDebt = (session.totalAmount - newAmountPaid).coerceAtLeast(0.0)
            val updatedSession = session.copy(
                amountPaid = newAmountPaid,
                remainingDebt = newDebt
            )
            sessionRepo.updateSession(updatedSession)

            val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
            val voucher = Voucher(
                voucherNumber = vNumber,
                type = VoucherType.RECEIPT,
                customerId = session.billedToCustomerId ?: session.customerId,
                sessionId = session.id,
                amount = amount,
                category = "سداد دورة سقي #${session.id}",
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                notes = notes.ifBlank { "سداد دورة ماء #${session.id}" }
            )
            voucherRepo.insertVoucher(voucher)
            showToast("تم سداد المبلغ وقيد سند القبض وتحديث الرصيد", ToastType.SUCCESS)
        }
    }

    fun deleteVoucher(voucher: Voucher) {
        viewModelScope.launch {
            voucherRepo.deleteVoucher(voucher)
            showToast("تم حذف السند وتحديث الرصيد", ToastType.INFO)
        }
    }

    fun deleteSession(session: WaterSession) {
        viewModelScope.launch {
            sessionRepo.deleteSession(session)
            showToast("تم حذف دورة الماء بنجاح", ToastType.INFO)
        }
    }

    fun generateCustomerStatementPdf(customer: Customer) {
        viewModelScope.launch {
            try {
                val sessions = getCustomerSessions(customer.id).first()
                val vouchers = voucherRepo.getVouchersForCustomer(customer.id).first()
                val config = appConfig.value
                val file: File = PdfReportGenerator.generateCustomerStatementPdf(
                    context = getApplication(),
                    config = config,
                    customer = customer,
                    sessions = sessions,
                    vouchers = vouchers
                )
                _pdfReadyFile.value = Pair(file, "كشف حساب ${customer.name}")
            } catch (e: Exception) {
                showToast("فشل في إنشاء كشف الحساب: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    fun buildCustomerStatementMessage(customer: Customer, item: CustomerWithBalance): String {
        val config = appConfig.value
        val debtStatus = if (customer.isWellOwner) {
            if (item.balance > 0) {
                "⚠️ المستحق لكم بذمتنا (عليك): ${com.example.core.util.Formatters.formatCurrency(item.balance, config.currencySymbol)}"
            } else if (item.balance < 0) {
                "✅ مسدد بالزيادة (لك): ${com.example.core.util.Formatters.formatCurrency(Math.abs(item.balance), config.currencySymbol)}"
            } else {
                "✅ الحساب مسدد بالكامل"
            }
        } else {
            if (item.balance > 0) {
                "⚠️ المطلوب بذمتكم: ${com.example.core.util.Formatters.formatCurrency(item.balance, config.currencySymbol)}"
            } else if (item.balance < 0) {
                "✅ لديكم رصيد دائن: ${com.example.core.util.Formatters.formatCurrency(Math.abs(item.balance), config.currencySymbol)}"
            } else {
                "✅ الحساب خالص ومسدد بالكامل"
            }
        }

        return if (customer.isWellOwner) {
            """
                *كشف حساب صاحب البئر - ${config.distributorName.ifEmpty { "المسرب" }}*
                💧 صاحب البئر: ${customer.name}
                📍 البئر / الموقع: ${customer.farmName.ifEmpty { customer.location.ifEmpty { "عام" } }}
                ⏱️ إجمالي ساعات الضخ: ${com.example.core.util.Formatters.formatDurationArabic(item.totalMinutes)}
                💰 إجمالي قيمة ساعات الضخ: ${com.example.core.util.Formatters.formatCurrency(item.totalBilledAmount, config.currencySymbol)}
                💵 إجمالي المبالغ المصروفة لكم: ${com.example.core.util.Formatters.formatCurrency(item.totalPaidAmount, config.currencySymbol)}
                ------------------------
                $debtStatus
                ------------------------${com.example.core.util.FileSharingHelper.MESSAGE_FOOTER}
            """.trimIndent()
        } else {
            """
                *كشف حساب مياه - ${config.distributorName.ifEmpty { "المسرب" }}*
                👤 العميل: ${customer.name}
                📍 المزرعة: ${customer.farmName.ifEmpty { "عام" }}
                ⏱️ إجمالي ساعات الري: ${com.example.core.util.Formatters.formatDurationArabic(item.totalMinutes)}
                💰 إجمالي قيمة المسارب: ${com.example.core.util.Formatters.formatCurrency(item.totalBilledAmount, config.currencySymbol)}
                💵 إجمالي المسدد: ${com.example.core.util.Formatters.formatCurrency(item.totalPaidAmount, config.currencySymbol)}
                ------------------------
                $debtStatus
                ------------------------${com.example.core.util.FileSharingHelper.MESSAGE_FOOTER}
            """.trimIndent()
        }
    }

    fun sendCustomerStatementWhatsApp(customer: Customer, item: CustomerWithBalance) {
        val msg = buildCustomerStatementMessage(customer, item)
        FileSharingHelper.sendWhatsAppMessage(getApplication(), customer.phone, msg)
    }
}

package com.example.features.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
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

class CustomersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionRepo = WaterSessionRepository(db.waterSessionDao(), db.customerDao())
    private val voucherRepo = VoucherRepository(db.voucherDao(), db.customerDao())
    private val customerRepo = CustomerRepository(db.customerDao(), sessionRepo.allSessions, voucherRepo.allVouchers)
    private val settingsRepo = SettingsRepository(db.appSettingDao())

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortType = MutableStateFlow(CustomerSort.NAME)
    val sortType: StateFlow<CustomerSort> = _sortType.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    val customersWithBalance: StateFlow<List<CustomerWithBalance>> =
        combine(customerRepo.customersWithBalance, _searchQuery, _sortType) { list, query, sort ->
            val filtered = if (query.isBlank()) list else {
                list.filter {
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

    fun saveCustomer(
        id: Long = 0,
        name: String,
        phone: String,
        farmName: String,
        location: String,
        notes: String,
        customPricePerHour: Double?
    ) {
        viewModelScope.launch {
            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                farmName = farmName.trim(),
                location = location.trim(),
                notes = notes.trim(),
                customPricePerHour = customPricePerHour
            )
            if (id == 0L) {
                customerRepo.insertCustomer(customer)
                showToast("تمت إضافة العميل بنجاح", ToastType.SUCCESS)
            } else {
                customerRepo.updateCustomer(customer)
                showToast("تم تعديل بيانات العميل بنجاح", ToastType.SUCCESS)
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            customerRepo.deleteCustomer(customer)
            showToast("تم حذف العميل بنجاح", ToastType.INFO)
        }
    }

    fun getCustomerSessions(customerId: Long): StateFlow<List<WaterSession>> =
        sessionRepo.getSessionsForCustomer(customerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getCustomerVouchers(customerId: Long): StateFlow<List<Voucher>> =
        voucherRepo.getVouchersForCustomer(customerId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReceiptVoucher(customerId: Long, amount: Double, paymentMethod: String, notes: String) {
        viewModelScope.launch {
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

    fun generateCustomerStatementPdf(customer: Customer) {
        viewModelScope.launch {
            try {
                val sessions = sessionRepo.getSessionsForCustomer(customer.id).first()
                val vouchers = voucherRepo.getVouchersForCustomer(customer.id).first()
                val config = appConfig.value

                val file: File = PdfReportGenerator.generateCustomerStatementPdf(
                    context = getApplication(),
                    config = config,
                    customer = customer,
                    sessions = sessions,
                    vouchers = vouchers
                )
                FileSharingHelper.sharePdf(getApplication(), file, "كشف حساب العميل ${customer.name}")
            } catch (e: Exception) {
                showToast("فشل في إنشاء كشف الحساب: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    fun sendCustomerStatementWhatsApp(customer: Customer, item: CustomerWithBalance) {
        val config = appConfig.value
        val debtStatus = if (item.balance > 0) {
            "⚠️ المطلوب بذمتكم: ${com.example.core.util.Formatters.formatCurrency(item.balance, config.currencySymbol)}"
        } else if (item.balance < 0) {
            "✅ لديكم رصيد دائن: ${com.example.core.util.Formatters.formatCurrency(Math.abs(item.balance), config.currencySymbol)}"
        } else {
            "✅ الحساب خالص ومسدد بالكامل"
        }

        val msg = """
            *كشف حساب مياه - ${config.distributorName}*
            👤 العميل: ${customer.name}
            📍 المزرعة: ${customer.farmName.ifEmpty { "عام" }}
            ⏱️ إجمالي ساعات الري: ${com.example.core.util.Formatters.formatDurationArabic(item.totalMinutes)}
            💰 إجمالي قيمة المسارب: ${com.example.core.util.Formatters.formatCurrency(item.totalBilledAmount, config.currencySymbol)}
            💵 إجمالي المسدد: ${com.example.core.util.Formatters.formatCurrency(item.totalPaidAmount, config.currencySymbol)}
            ------------------------
            $debtStatus
            ------------------------
            تحياتنا لكم.
        """.trimIndent()

        FileSharingHelper.sendWhatsAppMessage(getApplication(), customer.phone, msg)
    }
}

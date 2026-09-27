package com.example.features.vouchers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.features.customers.Customer
import com.example.features.customers.CustomerRepository
import com.example.features.sessions.WaterSessionRepository
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VouchersViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionRepo = WaterSessionRepository(db.waterSessionDao(), db.customerDao())
    private val voucherRepo = VoucherRepository(db.voucherDao(), db.customerDao())
    private val customerRepo = CustomerRepository(db.customerDao(), sessionRepo.allSessions, voucherRepo.allVouchers)
    private val settingsRepo = SettingsRepository(db.appSettingDao())

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    val customers: StateFlow<List<Customer>> = customerRepo.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTypeFilter = MutableStateFlow<VoucherType?>(null) // null = all
    val selectedTypeFilter: StateFlow<VoucherType?> = _selectedTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    val filteredVouchers: StateFlow<List<VoucherWithCustomer>> =
        combine(voucherRepo.vouchersWithCustomer, _selectedTypeFilter, _searchQuery) { list, typeFilter, query ->
            val typeFiltered = if (typeFilter == null) list else list.filter { it.voucher.type == typeFilter }
            if (query.isBlank()) {
                typeFiltered
            } else {
                typeFiltered.filter {
                    it.voucher.voucherNumber.contains(query, ignoreCase = true) ||
                    it.voucher.category.contains(query, ignoreCase = true) ||
                    it.voucher.notes.contains(query, ignoreCase = true) ||
                    (it.customer?.name?.contains(query, ignoreCase = true) == true)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTypeFilter(type: VoucherType?) {
        _selectedTypeFilter.value = type
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun showToast(msg: String, type: ToastType = ToastType.SUCCESS) {
        _toast.value = ToastMessage(message = msg, type = type)
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun addVoucher(
        type: VoucherType,
        customerId: Long?,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String
    ) {
        viewModelScope.launch {
            val prefix = if (type == VoucherType.RECEIPT) "REC" else "EXP"
            val num = "$prefix-${System.currentTimeMillis().toString().takeLast(4)}"
            val voucher = Voucher(
                voucherNumber = num,
                type = type,
                customerId = customerId,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                notes = notes
            )
            voucherRepo.insertVoucher(voucher)
            val title = if (type == VoucherType.RECEIPT) "سند القبض" else "سند الصرف والمصاريف"
            showToast("تم حفظ $title بنجاح", ToastType.SUCCESS)
        }
    }

    fun deleteVoucher(voucher: Voucher) {
        viewModelScope.launch {
            voucherRepo.deleteVoucher(voucher)
            showToast("تم حذف السند بنجاح", ToastType.INFO)
        }
    }
}

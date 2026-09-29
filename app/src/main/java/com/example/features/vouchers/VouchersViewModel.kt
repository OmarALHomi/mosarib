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

import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionWithCustomer

enum class OperationsFilter {
    ALL,        // الكل
    SESSIONS,   // سقي
    RECEIPTS,   // مقبوضات
    EXPENSES,   // مصروفات
    DEFERRED    // مؤخر / ديون
}

sealed class UnifiedOperation {
    abstract val timestamp: Long
    abstract val id: Long

    data class SessionOp(
        val sessionWithCustomer: WaterSessionWithCustomer,
        val linkedVouchers: List<Voucher> = emptyList()
    ) : UnifiedOperation() {
        override val timestamp: Long get() = sessionWithCustomer.session.startTime
        override val id: Long get() = sessionWithCustomer.session.id
    }

    data class VoucherOp(
        val voucherWithCustomer: VoucherWithCustomer
    ) : UnifiedOperation() {
        override val timestamp: Long get() = voucherWithCustomer.voucher.date
        override val id: Long get() = voucherWithCustomer.voucher.id
    }
}

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

    val allSessions: StateFlow<List<WaterSession>> = sessionRepo.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVouchers: StateFlow<List<Voucher>> = voucherRepo.allVouchers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _operationsFilter = MutableStateFlow(OperationsFilter.ALL)
    val operationsFilter: StateFlow<OperationsFilter> = _operationsFilter.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<VoucherType?>(null) // null = all
    val selectedTypeFilter: StateFlow<VoucherType?> = _selectedTypeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    /** أكثر بيانات المصروفات استخداماً في المرات السابقة لاقتراحها تلقائياً */
    val frequentExpenseDescriptions: StateFlow<List<String>> =
        voucherRepo.allVouchers.combine(MutableStateFlow(Unit)) { list, _ ->
            list.filter { it.type == VoucherType.EXPENSE }
                .map { if (it.notes.isNotBlank()) it.notes.trim() else it.category.trim() }
                .filter { it.isNotBlank() && it != "عام" && it != "مصاريف" }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(8)
                .map { it.key }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** سجل العمليات الموحد (سقي + مقبوضات + مصروفات) مع الفلترة */
    val unifiedOperations: StateFlow<List<UnifiedOperation>> =
        combine(
            sessionRepo.sessionsWithCustomer,
            voucherRepo.vouchersWithCustomer,
            _operationsFilter,
            _searchQuery
        ) { sessions, vouchers, filter, query ->
            val vouchersList = vouchers.map { it.voucher }
            val sessionOps = sessions.map { sWithC ->
                val linked = vouchersList.filter { it.sessionId == sWithC.session.id }
                UnifiedOperation.SessionOp(sWithC, linked)
            }
            val voucherOps = vouchers.map { vWithC ->
                UnifiedOperation.VoucherOp(vWithC)
            }

            val filteredList = when (filter) {
                OperationsFilter.ALL -> (sessionOps + voucherOps)
                OperationsFilter.SESSIONS -> sessionOps
                OperationsFilter.RECEIPTS -> voucherOps.filter { it.voucherWithCustomer.voucher.type == VoucherType.RECEIPT }
                OperationsFilter.EXPENSES -> voucherOps.filter { it.voucherWithCustomer.voucher.type == VoucherType.EXPENSE }
                OperationsFilter.DEFERRED -> sessionOps.filter { it.sessionWithCustomer.session.remainingDebt > 0 }
            }

            val searched = if (query.isBlank()) {
                filteredList
            } else {
                filteredList.filter { op ->
                    when (op) {
                        is UnifiedOperation.SessionOp -> {
                            val s = op.sessionWithCustomer.session
                            val c = op.sessionWithCustomer.customer
                            (c?.name?.contains(query, ignoreCase = true) == true) ||
                            (c?.farmName?.contains(query, ignoreCase = true) == true) ||
                            s.notes.contains(query, ignoreCase = true)
                        }
                        is UnifiedOperation.VoucherOp -> {
                            val v = op.voucherWithCustomer.voucher
                            val c = op.voucherWithCustomer.customer
                            v.voucherNumber.contains(query, ignoreCase = true) ||
                            v.category.contains(query, ignoreCase = true) ||
                            v.notes.contains(query, ignoreCase = true) ||
                            (c?.name?.contains(query, ignoreCase = true) == true)
                        }
                    }
                }
            }

            searched.sortedByDescending { it.timestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setOperationsFilter(filter: OperationsFilter) {
        _operationsFilter.value = filter
    }

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

    fun settleSessionDebt(
        session: WaterSession,
        amountToPay: Double,
        paymentMethod: String = "نقداً",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val newAmountPaid = session.amountPaid + amountToPay
            val newRemainingDebt = Math.max(0.0, session.totalAmount - newAmountPaid)
            val updated = session.copy(
                amountPaid = newAmountPaid,
                remainingDebt = newRemainingDebt
            )
            sessionRepo.updateSession(updated)

            val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
            val voucher = Voucher(
                voucherNumber = vNumber,
                type = VoucherType.RECEIPT,
                customerId = session.billedToCustomerId ?: session.customerId,
                sessionId = session.id,
                amount = amountToPay,
                category = "سداد سقي",
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                notes = notes.ifBlank { "سداد دورة سقي #${session.id}" }
            )
            voucherRepo.insertVoucher(voucher)
            showToast("تم سداد المبلغ بنجاح وإصدار سند القبض المرتبط", ToastType.SUCCESS)
        }
    }

    fun addVoucher(
        type: VoucherType,
        customerId: Long?,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String,
        sessionId: Long? = null
    ) {
        viewModelScope.launch {
            val prefix = if (type == VoucherType.RECEIPT) "REC" else "EXP"
            val num = "$prefix-${System.currentTimeMillis().toString().takeLast(4)}"
            val voucher = Voucher(
                voucherNumber = num,
                type = type,
                customerId = customerId,
                sessionId = sessionId,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis(),
                notes = notes
            )
            voucherRepo.insertVoucher(voucher)

            if (type == VoucherType.RECEIPT && sessionId != null && sessionId > 0) {
                val targetSession = sessionRepo.getSessionById(sessionId)
                if (targetSession != null) {
                    val newPaid = targetSession.amountPaid + amount
                    val newDebt = Math.max(0.0, targetSession.totalAmount - newPaid)
                    sessionRepo.updateSession(targetSession.copy(amountPaid = newPaid, remainingDebt = newDebt))
                }
            }

            val title = if (type == VoucherType.RECEIPT) "سند القبض" else "سند الصرف"
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

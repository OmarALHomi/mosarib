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

import com.example.core.license.LicenseManager
import com.example.core.util.Formatters
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
    private val customerRepo = CustomerRepository(
        db.customerDao(),
        sessionRepo.allSessions,
        voucherRepo.allVouchers,
        db.pumpSourceDao().getAllPumps()
    )
    private val settingsRepo = SettingsRepository(db.appSettingDao())

    val operationsCount: StateFlow<Int> = combine(
        db.waterSessionDao().getSessionsCount(),
        db.voucherDao().getVouchersCount()
    ) { s, v -> s + v }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    private val _settlementResult = MutableStateFlow<SettlementResult?>(null)
    val settlementResult: StateFlow<SettlementResult?> = _settlementResult.asStateFlow()

    fun clearSettlementResult() {
        _settlementResult.value = null
    }

    fun settleSessionDebt(
        session: WaterSession,
        amountToPay: Double,
        paymentMethod: String = "نقداً",
        notes: String = ""
    ) {
        addVoucher(
            type = VoucherType.RECEIPT,
            customerId = session.billedToCustomerId ?: session.customerId,
            amount = amountToPay,
            category = "سداد سقي",
            paymentMethod = paymentMethod,
            notes = notes,
            sessionId = session.id
        )
    }

    fun addVoucher(
        type: VoucherType,
        customerId: Long?,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String,
        sessionId: Long? = null,
        selectedSessionIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() + db.voucherDao().getVouchersCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val roundedAmount = Formatters.roundMoney(amount)

            val targetIds = when {
                selectedSessionIds.isNotEmpty() -> selectedSessionIds
                sessionId != null && sessionId > 0 -> listOf(sessionId)
                else -> emptyList()
            }

            if (type == VoucherType.RECEIPT && targetIds.isNotEmpty()) {
                val sessionsToSettle = targetIds.mapNotNull { sessionRepo.getSessionById(it) }
                val fifoResult = calculateFifoAllocation(sessionsToSettle, roundedAmount)

                val baseTime = System.currentTimeMillis()
                var voucherIdx = 0
                val summaries = mutableListOf<SettlementItemSummary>()

                for (step in fifoResult.steps) {
                    val s = step.session
                    if (step.allocatedAmount > 0.0) {
                        sessionRepo.updateSession(
                            s.copy(amountPaid = step.newPaid, remainingDebt = step.newDebt)
                        )

                        voucherIdx++
                        val vNum = if (fifoResult.steps.size == 1 && fifoResult.surplus <= 0.0) {
                            "REC-${baseTime.toString().takeLast(4)}"
                        } else {
                            "REC-${baseTime.toString().takeLast(4)}-$voucherIdx"
                        }

                        val sessionNote = if (notes.isNotBlank()) {
                            "$notes (سداد دورة #${s.id})"
                        } else {
                            "سداد دورة سقي #${s.id}"
                        }

                        val voucher = Voucher(
                            voucherNumber = vNum,
                            type = VoucherType.RECEIPT,
                            customerId = customerId,
                            sessionId = s.id,
                            amount = step.allocatedAmount,
                            category = "سداد سقي",
                            paymentMethod = paymentMethod,
                            date = baseTime + voucherIdx,
                            notes = sessionNote
                        )
                        voucherRepo.insertVoucher(voucher)
                    }

                    summaries.add(
                        SettlementItemSummary(
                            sessionId = s.id,
                            date = s.startTime,
                            originalDebt = s.remainingDebt,
                            allocatedAmount = step.allocatedAmount,
                            remainingDebtAfter = step.newDebt,
                            isFullyPaid = step.isFullyPaid
                        )
                    )
                }

                // إذا دفع العميل مبلغاً فائضاً عن كامل ديون الجلسات المختارة
                if (fifoResult.surplus > 0.0) {
                    voucherIdx++
                    val surplusVoucher = Voucher(
                        voucherNumber = "REC-${baseTime.toString().takeLast(4)}-$voucherIdx",
                        type = VoucherType.RECEIPT,
                        customerId = customerId,
                        sessionId = null,
                        amount = fifoResult.surplus,
                        category = "دفعة على الحساب",
                        paymentMethod = paymentMethod,
                        date = baseTime + voucherIdx,
                        notes = if (notes.isNotBlank()) "$notes (فائض رصيد)" else "دفعة فائضة مقيدة كرصيد دائن للعميل"
                    )
                    voucherRepo.insertVoucher(surplusVoucher)
                }

                val cust = customers.value.find { it.id == customerId }
                _settlementResult.value = SettlementResult(
                    customerName = cust?.name ?: "عميل غير محدد",
                    customerPhone = cust?.phone ?: "",
                    totalAmount = roundedAmount,
                    items = summaries,
                    surplusAmount = fifoResult.surplus,
                    currencySymbol = appConfig.value.currencySymbol
                )

                LicenseManager.recordOperationPerformed(getApplication(), totalOps)
                showToast("تم سداد السند وتوزيع المبلغ بنجاح", ToastType.SUCCESS)
            } else {
                val prefix = if (type == VoucherType.RECEIPT) "REC" else "EXP"
                val num = "$prefix-${System.currentTimeMillis().toString().takeLast(4)}"
                val voucher = Voucher(
                    voucherNumber = num,
                    type = type,
                    customerId = customerId,
                    sessionId = sessionId,
                    amount = roundedAmount,
                    category = category,
                    paymentMethod = paymentMethod,
                    date = System.currentTimeMillis(),
                    notes = notes
                )
                voucherRepo.insertVoucher(voucher)
                LicenseManager.recordOperationPerformed(getApplication(), totalOps)

                val title = if (type == VoucherType.RECEIPT) "سند القبض" else "سند الصرف"
                showToast("تم حفظ $title بنجاح", ToastType.SUCCESS)
            }
        }
    }

    fun deleteVoucher(voucher: Voucher) {
        viewModelScope.launch {
            voucherRepo.deleteVoucher(voucher)
            showToast("تم حذف السند بنجاح", ToastType.INFO)
        }
    }
}

data class SettlementItemSummary(
    val sessionId: Long,
    val date: Long,
    val originalDebt: Double,
    val allocatedAmount: Double,
    val remainingDebtAfter: Double,
    val isFullyPaid: Boolean
)

data class SettlementResult(
    val customerName: String,
    val customerPhone: String,
    val totalAmount: Double,
    val items: List<SettlementItemSummary>,
    val surplusAmount: Double = 0.0,
    val currencySymbol: String = "ر.ي"
)

data class AllocationStep(
    val session: WaterSession,
    val allocatedAmount: Double,
    val newPaid: Double,
    val newDebt: Double,
    val isFullyPaid: Boolean
)

data class FifoAllocationResult(
    val steps: List<AllocationStep>,
    val surplus: Double
)

fun calculateFifoAllocation(
    sessions: List<WaterSession>,
    totalAmount: Double
): FifoAllocationResult {
    var pool = Formatters.roundMoney(totalAmount)
    val sorted = sessions.sortedBy { it.startTime }
    val steps = mutableListOf<AllocationStep>()
    for (s in sorted) {
        if (pool <= 0.0) {
            steps.add(
                AllocationStep(
                    session = s,
                    allocatedAmount = 0.0,
                    newPaid = Formatters.roundMoney(s.amountPaid),
                    newDebt = Formatters.roundMoney(s.remainingDebt),
                    isFullyPaid = false
                )
            )
        } else {
            val sDebt = Formatters.roundMoney(s.remainingDebt)
            val allocated = Formatters.roundMoney(minOf(sDebt, pool))
            val newPaid = Formatters.roundMoney((s.amountPaid + allocated).coerceAtMost(s.totalAmount))
            val newDebt = Formatters.roundMoney((s.totalAmount - newPaid).coerceAtLeast(0.0))
            pool = Formatters.roundMoney(pool - allocated)
            steps.add(
                AllocationStep(
                    session = s,
                    allocatedAmount = allocated,
                    newPaid = newPaid,
                    newDebt = newDebt,
                    isFullyPaid = newDebt <= 0.0
                )
            )
        }
    }
    return FifoAllocationResult(steps = steps, surplus = maxOf(0.0, pool))
}


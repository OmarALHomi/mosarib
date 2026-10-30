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
import com.example.features.sessions.WaterSessionWithCustomer
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
import com.example.core.util.PdfReportGenerator
import com.example.features.sessions.WaterSession
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseRepository
import android.content.Context
import java.io.File

enum class OperationsFilter {
    ALL,        // الكل
    SESSIONS,   // سقي
    RECEIPTS,   // مقبوضات
    EXPENSES,   // مصروفات
    PURCHASES,  // شراء ساعات
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

    data class PurchaseOp(
        val purchase: WellOwnerPurchase,
        val owner: Customer?
    ) : UnifiedOperation() {
        override val timestamp: Long get() = if (purchase.startTime > 0) purchase.startTime else purchase.date
        override val id: Long get() = purchase.id
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
        db.pumpSourceDao().getAllPumps(),
        db.wellOwnerPurchaseDao().getAllPurchases()
    )
    private val settingsRepo = SettingsRepository(db.appSettingDao())

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

    private val ownerPurchaseRepo = WellOwnerPurchaseRepository(db.wellOwnerPurchaseDao())

    /** سجل العمليات الموحد (سقي + مقبوضات + مصروفات + شراء ساعات) مع الفلترة */
    private val rawOperationsFlow = combine(
        sessionRepo.sessionsWithCustomer,
        voucherRepo.vouchersWithCustomer,
        ownerPurchaseRepo.allPurchases,
        customerRepo.allCustomers
    ) { sessions, vouchers, purchases, customersList ->
        val vouchersList = vouchers.map { it.voucher }
        val sessionOps = sessions.map { sWithC ->
            val linked = vouchersList.filter { it.sessionId == sWithC.session.id }
            UnifiedOperation.SessionOp(sWithC, linked)
        }
        val voucherOps = vouchers.map { vWithC ->
            UnifiedOperation.VoucherOp(vWithC)
        }
        val purchaseOps = purchases.map { purchase ->
            val owner = customersList.find { it.id == purchase.ownerCustomerId }
            UnifiedOperation.PurchaseOp(purchase, owner)
        }
        sessionOps + voucherOps + purchaseOps
    }

    val unifiedOperations: StateFlow<List<UnifiedOperation>> =
        combine(
            rawOperationsFlow,
            _operationsFilter,
            _searchQuery
        ) { allOps, filter, query ->
            val filteredList = when (filter) {
                OperationsFilter.ALL -> allOps
                OperationsFilter.SESSIONS -> allOps.filterIsInstance<UnifiedOperation.SessionOp>()
                OperationsFilter.RECEIPTS -> allOps.filterIsInstance<UnifiedOperation.VoucherOp>()
                    .filter { it.voucherWithCustomer.voucher.type == VoucherType.RECEIPT }
                OperationsFilter.EXPENSES -> allOps.filterIsInstance<UnifiedOperation.VoucherOp>()
                    .filter { it.voucherWithCustomer.voucher.type == VoucherType.EXPENSE }
                OperationsFilter.PURCHASES -> allOps.filterIsInstance<UnifiedOperation.PurchaseOp>()
                OperationsFilter.DEFERRED -> allOps.filterIsInstance<UnifiedOperation.SessionOp>()
                    .filter { it.sessionWithCustomer.session.remainingDebt > 0 }
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
                        is UnifiedOperation.PurchaseOp -> {
                            val p = op.purchase
                            val c = op.owner
                            (c?.name?.contains(query, ignoreCase = true) == true) ||
                            p.notes.contains(query, ignoreCase = true)
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

    private val _pdfReadyFile = MutableStateFlow<Pair<File, String>?>(null)
    val pdfReadyFile: StateFlow<Pair<File, String>?> = _pdfReadyFile.asStateFlow()

    fun clearPdfReady() {
        _pdfReadyFile.value = null
    }

    fun exportOperationsLogPdf(context: Context) {
        viewModelScope.launch {
            try {
                val list = unifiedOperations.value
                if (list.isEmpty()) {
                    showToast("لا توجد عمليات لطباعتها في القائمة الحالية", ToastType.WARNING)
                    return@launch
                }
                val config = appConfig.value
                val filterName = when (_operationsFilter.value) {
                    OperationsFilter.ALL -> "كافة العمليات"
                    OperationsFilter.SESSIONS -> "جلسات السقي"
                    OperationsFilter.RECEIPTS -> "سندات القبض"
                    OperationsFilter.EXPENSES -> "سندات المصروفات"
                    OperationsFilter.PURCHASES -> "شراء ساعات الآبار"
                    OperationsFilter.DEFERRED -> "العمليات الآجلة والديون"
                }
                val file = PdfReportGenerator.generateOperationsLogPdf(
                    context = context,
                    config = config,
                    filterTitle = filterName,
                    operations = list
                )
                _pdfReadyFile.value = Pair(file, "سجل العمليات - $filterName")
            } catch (e: Exception) {
                showToast("فشل في إنشاء تقرير سجل العمليات: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
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
        notes: String = "",
        discountAmount: Double = 0.0
    ) {
        addVoucher(
            type = VoucherType.RECEIPT,
            customerId = session.billedToCustomerId ?: session.customerId,
            amount = amountToPay,
            category = "سداد سقي",
            paymentMethod = paymentMethod,
            notes = notes,
            sessionId = session.id,
            discountAmount = discountAmount
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
        selectedSessionIds: List<Long> = emptyList(),
        discountAmount: Double = 0.0
    ) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() + db.wellOwnerPurchaseDao().getPurchasesCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast(LicenseManager.getLimitReachedToastMessage(getApplication()), ToastType.ERROR)
                return@launch
            }

            val roundedAmount = Formatters.roundMoney(amount)
            val roundedDiscount = Formatters.roundMoney(discountAmount)

            val targetIds = when {
                selectedSessionIds.isNotEmpty() -> selectedSessionIds
                sessionId != null && sessionId > 0 -> listOf(sessionId)
                else -> emptyList()
            }

            if (type == VoucherType.RECEIPT && targetIds.isNotEmpty()) {
                val sessionsToSettle = targetIds.mapNotNull { sessionRepo.getSessionById(it) }
                val fifoResult = calculateFifoAllocation(sessionsToSettle, roundedAmount, roundedDiscount)

                val baseTime = System.currentTimeMillis()
                var voucherIdx = 0
                val summaries = mutableListOf<SettlementItemSummary>()

                for (step in fifoResult.steps) {
                    val s = step.session
                    if (step.allocatedAmount > 0.0 || step.allocatedDiscount > 0.0) {
                        sessionRepo.updateSession(
                            s.copy(
                                amountPaid = step.newPaid,
                                discountAmount = step.newDiscount,
                                remainingDebt = step.newDebt
                            )
                        )

                        // 1. سند قبض للمبلغ النقدي المستلم
                        if (step.allocatedAmount > 0.0) {
                            voucherIdx++
                            val vNum = "REC-${baseTime.toString().takeLast(4)}-$voucherIdx"
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

                        // 2. سند تسوية / مسامحة لمبلغ الخصم
                        if (step.allocatedDiscount > 0.0) {
                            voucherIdx++
                            val discNum = "DSC-${baseTime.toString().takeLast(4)}-$voucherIdx"
                            val discVoucher = Voucher(
                                voucherNumber = discNum,
                                type = VoucherType.DISCOUNT,
                                customerId = customerId,
                                sessionId = s.id,
                                amount = step.allocatedDiscount,
                                category = "خصم تسوية",
                                paymentMethod = "تسوية ومسامحة",
                                date = baseTime + voucherIdx,
                                notes = "خصم تسوية دورة سقي #${s.id}"
                            )
                            voucherRepo.insertVoucher(discVoucher)
                        }
                    }

                    summaries.add(
                        SettlementItemSummary(
                            sessionId = s.id,
                            date = s.startTime,
                            originalDebt = s.remainingDebt,
                            allocatedAmount = step.allocatedAmount,
                            allocatedDiscount = step.allocatedDiscount,
                            remainingDebtAfter = step.newDebt,
                            isFullyPaid = step.isFullyPaid
                        )
                    )
                }

                // إذا دفع العميل مبلغاً نقدياً فائضاً عن كامل ديون الجلسات المختارة
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
                    discountAmount = roundedDiscount,
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

                // إذا كان سند قبض عام بدون جلسات محددة ومعه خصم تسوية
                if (type == VoucherType.RECEIPT && roundedDiscount > 0.0) {
                    val discNum = "DSC-${System.currentTimeMillis().toString().takeLast(4)}"
                    val discVoucher = Voucher(
                        voucherNumber = discNum,
                        type = VoucherType.DISCOUNT,
                        customerId = customerId,
                        sessionId = null,
                        amount = roundedDiscount,
                        category = "خصم تسوية",
                        paymentMethod = "تسوية ومسامحة",
                        date = System.currentTimeMillis() + 1,
                        notes = if (notes.isNotBlank()) "$notes (خصم تسوية رصيد)" else "خصم تسوية ومسامحة في رصيد الحساب"
                    )
                    voucherRepo.insertVoucher(discVoucher)
                }

                LicenseManager.recordOperationPerformed(getApplication(), totalOps)

                val title = if (type == VoucherType.RECEIPT) "سند القبض" else "سند الصرف"
                showToast("تم حفظ $title بنجاح", ToastType.SUCCESS)
            }
        }
    }

    fun deleteVoucher(voucher: Voucher) {
        viewModelScope.launch {
            if (voucher.sessionId != null) {
                val session = sessionRepo.getSessionById(voucher.sessionId)
                if (session != null) {
                    val targetCustId = session.billedToCustomerId ?: session.customerId
                    val isWellOwner = customerRepo.getCustomerByIdDirect(targetCustId)?.isWellOwner == true
                    val newPaid = if (voucher.type == VoucherType.RECEIPT) {
                        (session.amountPaid - voucher.amount).coerceAtLeast(0.0)
                    } else session.amountPaid

                    val newDiscount = if (voucher.type == VoucherType.DISCOUNT) {
                        (session.discountAmount - voucher.amount).coerceAtLeast(0.0)
                    } else session.discountAmount

                    val newDebt = if (isWellOwner) 0.0 else {
                        Formatters.roundMoney(minOf(session.remainingDebt + voucher.amount, (session.totalAmount - newPaid).coerceAtLeast(0.0)))
                    }

                    sessionRepo.updateSession(
                        session.copy(
                            amountPaid = Formatters.roundMoney(newPaid),
                            discountAmount = Formatters.roundMoney(newDiscount),
                            remainingDebt = Formatters.roundMoney(newDebt)
                        )
                    )
                }
            }
            voucherRepo.deleteVoucher(voucher)
            showToast("تم حذف السند وتحديث الرصيد بنجاح", ToastType.INFO)
        }
    }

    fun updateVoucher(
        voucher: Voucher,
        amount: Double,
        paymentMethod: String,
        notes: String,
        category: String = voucher.category,
        customerId: Long? = voucher.customerId,
        date: Long = voucher.date,
        type: VoucherType = voucher.type
    ) {
        viewModelScope.launch {
            val roundedAmount = Formatters.roundMoney(amount)
            if (roundedAmount <= 0.0) {
                showToast("يرجى إدخال مبلغ صالح أكبر من صفر", ToastType.ERROR)
                return@launch
            }
            if (voucher.sessionId != null) {
                val session = sessionRepo.getSessionById(voucher.sessionId)
                if (session != null) {
                    val targetCustId = session.billedToCustomerId ?: session.customerId
                    val isWellOwner = customerRepo.getCustomerByIdDirect(targetCustId)?.isWellOwner == true
                    val amountDiff = roundedAmount - voucher.amount

                    val newPaid = if (voucher.type == VoucherType.RECEIPT) {
                        (session.amountPaid + amountDiff).coerceAtLeast(0.0)
                    } else session.amountPaid

                    val newDiscount = if (voucher.type == VoucherType.DISCOUNT) {
                        (session.discountAmount + amountDiff).coerceAtLeast(0.0)
                    } else session.discountAmount

                    val newDebt = if (isWellOwner) 0.0 else {
                        Formatters.roundMoney((session.remainingDebt - amountDiff).coerceIn(0.0, (session.totalAmount - newPaid).coerceAtLeast(0.0)))
                    }

                    sessionRepo.updateSession(
                        session.copy(
                            amountPaid = Formatters.roundMoney(newPaid),
                            discountAmount = Formatters.roundMoney(newDiscount),
                            remainingDebt = Formatters.roundMoney(newDebt)
                        )
                    )
                }
            }
            voucherRepo.updateVoucher(
                voucher.copy(
                    type = type,
                    amount = roundedAmount,
                    paymentMethod = paymentMethod,
                    notes = notes.trim(),
                    category = category.trim(),
                    customerId = customerId,
                    date = date
                )
            )
            showToast("تم تعديل السند بنجاح", ToastType.SUCCESS)
        }
    }

    fun deleteSession(session: WaterSession) {
        viewModelScope.launch {
            val sessionVouchers = voucherRepo.getVouchersForSessionDirect(session.id)
            for (v in sessionVouchers) {
                if (v.type == VoucherType.DISCOUNT) {
                    voucherRepo.deleteVoucher(v)
                } else {
                    voucherRepo.updateVoucher(
                        v.copy(sessionId = null, notes = "${v.notes} (دفعة على الحساب بعد حذف دورة #${session.id})".trim())
                    )
                }
            }
            sessionRepo.deleteSession(session)
            showToast("تم حذف دورة السقي وتحديث الحساب", ToastType.INFO)
        }
    }

    fun updateSession(
        id: Long,
        customerId: Long,
        pumpName: String,
        startTime: Long,
        endTime: Long,
        durationMinutes: Int,
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
            val targetCust = customerRepo.getCustomerByIdDirect(customerId)
            val isWellOwnerAccount = billedToCustomerId != null || targetCust?.isWellOwner == true
            val chargeableMinutes = (durationMinutes - wastedMinutes).coerceAtLeast(0)
            val baseAmount = (chargeableMinutes / 60.0) * pricePerHour
            val totalAmount = Formatters.roundMoney((baseAmount - discountAmount).coerceAtLeast(0.0))
            val roundedPaid = Formatters.roundMoney(amountPaid)
            val remainingDebt = if (isWellOwnerAccount) 0.0 else Formatters.roundMoney((totalAmount - roundedPaid).coerceAtLeast(0.0))

            val session = WaterSession(
                id = id,
                customerId = customerId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                pricePerHour = pricePerHour,
                totalAmount = totalAmount,
                amountPaid = roundedPaid,
                remainingDebt = remainingDebt,
                notes = notes,
                isLive = false,
                billedToCustomerId = billedToCustomerId,
                wastedMinutes = wastedMinutes,
                wastedReason = wastedReason,
                discountAmount = discountAmount,
                costPricePerHour = costPricePerHour,
                pumpSourceId = pumpSourceId
            )
            sessionRepo.updateSession(session)
            showToast("تم تحديث بيانات دورة السقي بنجاح", ToastType.SUCCESS)
        }
    }

    fun addOwnerPurchase(
        ownerCustomerId: Long,
        date: Long,
        durationMinutes: Int,
        wastedMinutesOnOwner: Int,
        purchaseRatePerHour: Double,
        notes: String,
        amountPaid: Double = 0.0,
        startTime: Long = 0L,
        endTime: Long = 0L,
        onCreated: ((WellOwnerPurchase) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val owner = customerRepo.getCustomerByIdDirect(ownerCustomerId)
            if (owner == null || !owner.isWellOwner) {
                showToast("اختر حساب صاحب بئر صالحاً لتسجيل الشراء", ToastType.ERROR)
                return@launch
            }
            if (durationMinutes <= 0 || durationMinutes > 6000) {
                showToast("لا يمكن تسجيل شراء بساعات خيالية أو صفرية", ToastType.ERROR)
                return@launch
            }
            if (wastedMinutesOnOwner > durationMinutes) {
                showToast("لا يمكن أن يكون الهدر أكبر من عملية الشراء بتاتاً", ToastType.ERROR)
                return@launch
            }
            if (purchaseRatePerHour <= 0.0) {
                showToast("يرجى تحديد سعر شراء صالح للساعة", ToastType.ERROR)
                return@launch
            }

            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast(LicenseManager.getLimitReachedToastMessage(getApplication()), ToastType.ERROR)
                return@launch
            }

            val newId = ownerPurchaseRepo.insert(
                WellOwnerPurchase(
                    ownerCustomerId = ownerCustomerId,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    wastedMinutesOnOwner = wastedMinutesOnOwner,
                    purchaseRatePerHour = Formatters.roundMoney(purchaseRatePerHour),
                    amountPaid = Formatters.roundMoney(amountPaid),
                    notes = notes.trim()
                )
            )

            // إذا تم تسليم مبلغ نقدياً فوراً لصاحب البئر، يُسجّل سند صرف (EXPENSE) مرتبط به فوراً
            if (amountPaid > 0.0) {
                val vNumber = "EXP-${System.currentTimeMillis().toString().takeLast(8)}"
                val voucher = Voucher(
                    voucherNumber = vNumber,
                    type = VoucherType.EXPENSE,
                    customerId = ownerCustomerId,
                    amount = Formatters.roundMoney(amountPaid),
                    category = "تسديد نقدي لشراء ساعات",
                    paymentMethod = "نقداً",
                    date = date,
                    notes = "دفعة مسلّمة فوراً لصاحب البئر عند شراء ساعات [شراء #$newId]"
                )
                voucherRepo.insertVoucher(voucher)
            }

            LicenseManager.recordOperationPerformed(getApplication(), totalOps)
            showToast("تم تسجيل شراء الساعات وتحديث حساب صاحب البئر", ToastType.SUCCESS)

            val createdPurchase = WellOwnerPurchase(
                id = newId,
                ownerCustomerId = ownerCustomerId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                wastedMinutesOnOwner = wastedMinutesOnOwner,
                purchaseRatePerHour = Formatters.roundMoney(purchaseRatePerHour),
                amountPaid = Formatters.roundMoney(amountPaid),
                notes = notes.trim()
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onCreated?.invoke(createdPurchase)
            }
        }
    }

    fun deleteOwnerPurchase(purchase: WellOwnerPurchase) {
        viewModelScope.launch {
            val existingVouchers = voucherRepo.getVouchersForCustomerDirect(purchase.ownerCustomerId)
            val linkedExpenseVoucher = existingVouchers.find {
                it.type == VoucherType.EXPENSE && it.notes.contains("[شراء #${purchase.id}]")
            }
            if (linkedExpenseVoucher != null) {
                voucherRepo.deleteVoucher(linkedExpenseVoucher)
            }
            ownerPurchaseRepo.delete(purchase)
            showToast("تم حذف عملية شراء الساعات وسند الصرف بنجاح", ToastType.INFO)
        }
    }

    fun updateOwnerPurchase(
        purchase: WellOwnerPurchase,
        date: Long,
        durationMinutes: Int,
        wastedMinutes: Int,
        rate: Double,
        amountPaid: Double,
        notes: String,
        startTime: Long = 0L,
        endTime: Long = 0L
    ) {
        viewModelScope.launch {
            val roundedPaid = Formatters.roundMoney(amountPaid)
            ownerPurchaseRepo.update(
                purchase.copy(
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    durationMinutes = durationMinutes,
                    wastedMinutesOnOwner = wastedMinutes,
                    purchaseRatePerHour = Formatters.roundMoney(rate),
                    amountPaid = roundedPaid,
                    notes = notes
                )
            )

            // مزامنة سند الصرف المرتبط بعملية الشراء
            val existingVouchers = voucherRepo.getVouchersForCustomerDirect(purchase.ownerCustomerId)
            val linkedExpenseVoucher = existingVouchers.find {
                it.type == VoucherType.EXPENSE && it.notes.contains("[شراء #${purchase.id}]")
            }
            if (roundedPaid > 0.0) {
                if (linkedExpenseVoucher != null) {
                    voucherRepo.updateVoucher(
                        linkedExpenseVoucher.copy(
                            amount = roundedPaid,
                            date = date
                        )
                    )
                } else {
                    val vNumber = "EXP-${System.currentTimeMillis().toString().takeLast(8)}"
                    voucherRepo.insertVoucher(
                        Voucher(
                            voucherNumber = vNumber,
                            type = VoucherType.EXPENSE,
                            customerId = purchase.ownerCustomerId,
                            amount = roundedPaid,
                            category = "تسديد نقدي لشراء ساعات",
                            paymentMethod = "نقداً",
                            date = date,
                            notes = "دفعة مسلّمة فوراً لصاحب البئر عند شراء ساعات [شراء #${purchase.id}]"
                        )
                    )
                }
            } else {
                if (linkedExpenseVoucher != null) {
                    voucherRepo.deleteVoucher(linkedExpenseVoucher)
                }
            }
            showToast("تم تعديل عملية شراء الساعات بنجاح", ToastType.SUCCESS)
        }
    }
}

data class SettlementResult(
    val customerName: String,
    val customerPhone: String,
    val totalAmount: Double,
    val discountAmount: Double = 0.0,
    val items: List<SettlementItemSummary>,
    val surplusAmount: Double = 0.0,
    val currencySymbol: String = "ر.ي"
)

data class SettlementItemSummary(
    val sessionId: Long,
    val date: Long,
    val originalDebt: Double,
    val allocatedAmount: Double,
    val allocatedDiscount: Double = 0.0,
    val remainingDebtAfter: Double,
    val isFullyPaid: Boolean
)

data class AllocationStep(
    val session: WaterSession,
    val allocatedAmount: Double,
    val allocatedDiscount: Double = 0.0,
    val newPaid: Double,
    val newDiscount: Double = 0.0,
    val newDebt: Double,
    val isFullyPaid: Boolean
)

data class FifoAllocationResult(
    val steps: List<AllocationStep>,
    val surplus: Double,
    val surplusDiscount: Double = 0.0
)

fun calculateFifoAllocation(
    sessions: List<WaterSession>,
    totalAmount: Double,
    discountAmount: Double = 0.0
): FifoAllocationResult {
    var cashPool = Formatters.roundMoney(totalAmount)
    var discountPool = Formatters.roundMoney(discountAmount)
    val sorted = sessions.sortedBy { it.startTime }
    val steps = mutableListOf<AllocationStep>()

    for (s in sorted) {
        val sDebt = Formatters.roundMoney(s.remainingDebt)
        if (cashPool <= 0.0 && discountPool <= 0.0) {
            steps.add(
                AllocationStep(
                    session = s,
                    allocatedAmount = 0.0,
                    allocatedDiscount = 0.0,
                    newPaid = Formatters.roundMoney(s.amountPaid),
                    newDiscount = Formatters.roundMoney(s.discountAmount),
                    newDebt = sDebt,
                    isFullyPaid = sDebt <= 0.0
                )
            )
        } else {
            val allocatedCash = Formatters.roundMoney(minOf(sDebt, maxOf(0.0, cashPool)))
            val debtAfterCash = Formatters.roundMoney(maxOf(0.0, sDebt - allocatedCash))
            cashPool = Formatters.roundMoney(maxOf(0.0, cashPool - allocatedCash))

            val allocatedDiscount = Formatters.roundMoney(minOf(debtAfterCash, maxOf(0.0, discountPool)))
            discountPool = Formatters.roundMoney(maxOf(0.0, discountPool - allocatedDiscount))

            val newPaid = Formatters.roundMoney((s.amountPaid + allocatedCash).coerceAtMost(s.totalAmount))
            val newDiscount = Formatters.roundMoney(s.discountAmount + allocatedDiscount)
            val newDebt = Formatters.roundMoney(maxOf(0.0, sDebt - allocatedCash - allocatedDiscount))

            steps.add(
                AllocationStep(
                    session = s,
                    allocatedAmount = allocatedCash,
                    allocatedDiscount = allocatedDiscount,
                    newPaid = newPaid,
                    newDiscount = newDiscount,
                    newDebt = newDebt,
                    isFullyPaid = newDebt <= 0.0
                )
            )
        }
    }
    return FifoAllocationResult(
        steps = steps,
        surplus = maxOf(0.0, cashPool),
        surplusDiscount = maxOf(0.0, discountPool)
    )
}


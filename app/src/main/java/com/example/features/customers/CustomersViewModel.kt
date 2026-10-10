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
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseRepository
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
    private val ownerPurchaseRepo = WellOwnerPurchaseRepository(db.wellOwnerPurchaseDao())
    private val customerRepo = CustomerRepository(
        db.customerDao(),
        sessionRepo.allSessions,
        voucherRepo.allVouchers,
        db.pumpSourceDao().getAllPumps(),
        ownerPurchaseRepo.allPurchases
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
            val existing = if (id > 0) customerRepo.getCustomerByIdDirect(id) else null
            val hasOwnerLedgerHistory = id > 0 && (
                ownerPurchaseRepo.getPurchasesForOwner(id).first().isNotEmpty() ||
                    voucherRepo.getVouchersForCustomer(id).first().any { it.type == VoucherType.EXPENSE }
                )
            val effectiveWellOwner = isWellOwner || (existing?.isWellOwner == true && hasOwnerLedgerHistory)
            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                farmName = farmName.trim(),
                location = location.trim(),
                notes = notes.trim(),
                customPricePerHour = customPricePerHour,
                isBeneficiary = isBeneficiary,
                isWellOwner = effectiveWellOwner,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                isArchived = existing?.isArchived ?: false
            )
            if (id == 0L) {
                customerRepo.insertCustomer(customer)
                showToast(if (effectiveWellOwner) "تمت إضافة حساب صاحب البئر بنجاح" else "تمت إضافة العميل بنجاح", ToastType.SUCCESS)
            } else {
                customerRepo.updateCustomer(customer)
                if (existing?.isWellOwner == true && !isWellOwner && effectiveWellOwner) {
                    showToast("حُفظت البيانات مع الإبقاء على صفة صاحب البئر لحماية سجل المشتريات والسداد", ToastType.WARNING)
                } else {
                    showToast("تم تعديل بيانات الحساب بنجاح", ToastType.SUCCESS)
                }
            }
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            // Keep financial history and foreign-key references intact; archived accounts disappear
            // from active lists but remain available to statements and backups.
            customerRepo.archiveCustomer(customer.id)
            showToast("تمت أرشفة الحساب مع الاحتفاظ بسجلاته المالية", ToastType.INFO)
        }
    }

    fun getCustomerSessions(customerId: Long): Flow<List<WaterSession>> =
        sessionRepo.allSessions.combine(db.customerDao().getCustomerById(customerId)) { sessions, _ ->
            sessions.filter {
                it.billedToCustomerId == customerId ||
                    (it.customerId == customerId && it.billedToCustomerId == null)
            }
        }

    fun getCustomerPurchases(customerId: Long): Flow<List<WellOwnerPurchase>> =
        ownerPurchaseRepo.getPurchasesForOwner(customerId)

    fun getCustomerVouchers(customerId: Long): Flow<List<Voucher>> =
        voucherRepo.getVouchersForCustomer(customerId)

    fun addOwnerPurchase(
        ownerCustomerId: Long,
        date: Long,
        durationMinutes: Int,
        wastedMinutesOnOwner: Int,
        purchaseRatePerHour: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val owner = customerRepo.getCustomerByIdDirect(ownerCustomerId)
            if (owner == null || !owner.isWellOwner) {
                showToast("اختر حساب صاحب بئر صالحاً لتسجيل الشراء", ToastType.ERROR)
                return@launch
            }
            if (durationMinutes <= 0 || wastedMinutesOnOwner !in 0..durationMinutes || purchaseRatePerHour <= 0.0) {
                showToast("تحقق من الساعات المشتراة والهدر وسعر الساعة", ToastType.ERROR)
                return@launch
            }

            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            ownerPurchaseRepo.insert(
                WellOwnerPurchase(
                    ownerCustomerId = ownerCustomerId,
                    date = date,
                    durationMinutes = durationMinutes,
                    wastedMinutesOnOwner = wastedMinutesOnOwner,
                    purchaseRatePerHour = com.example.core.util.Formatters.roundMoney(purchaseRatePerHour),
                    notes = notes.trim()
                )
            )
            LicenseManager.recordOperationPerformed(getApplication(), totalOps)
            showToast("تم تسجيل شراء الساعات وخصم الهدر على صاحب البئر من مستحقه", ToastType.SUCCESS)
        }
    }

    fun deleteOwnerPurchase(purchase: WellOwnerPurchase) {
        viewModelScope.launch {
            ownerPurchaseRepo.delete(purchase)
            showToast("تم حذف حركة الشراء وتحديث كشف صاحب البئر", ToastType.INFO)
        }
    }

    fun addReceiptVoucher(customerId: Long, amount: Double, paymentMethod: String, notes: String) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
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
            LicenseManager.recordOperationPerformed(getApplication(), totalOps)
            showToast("تم تسجيل سند القبض وتحديث رصيد العميل", ToastType.SUCCESS)
        }
    }

    fun addExpenseVoucher(customerId: Long, amount: Double, paymentMethod: String, description: String) {
        viewModelScope.launch {
            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
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
            LicenseManager.recordOperationPerformed(getApplication(), totalOps)
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
            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
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
            LicenseManager.recordOperationPerformed(getApplication(), totalOps)
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
                val purchases = ownerPurchaseRepository.getPurchasesForOwner(customer.id).first()
                val vouchers = voucherRepo.getVouchersForCustomer(customer.id).first()
                val config = appConfig.value
                val file: File = PdfReportGenerator.generateCustomerStatementPdf(
                    context = getApplication(),
                    config = config,
                    customer = customer,
                    sessions = sessions,
                    vouchers = vouchers,
                    purchases = purchases
                )
                _pdfReadyFile.value = Pair(file, "كشف حساب ${customer.name}")
            } catch (e: Exception) {
                showToast("فشل في إنشاء كشف الحساب: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    fun buildCustomerStatementMessage(customer: Customer, item: CustomerWithBalance): String {
        val config = appConfig.value
        val formatCurrency = { amount: Double -> com.example.core.util.Formatters.formatCurrency(amount, config.currencySymbol) }
        val formatDuration = { minutes: Int -> com.example.core.util.Formatters.formatDurationArabic(minutes) }
        val debtStatus = if (item.receivableBalance > 0) {
            "⚠️ المطلوب بذمتكم: ${formatCurrency(item.receivableBalance)}"
        } else if (item.receivableBalance < 0) {
            "✅ لديكم رصيد دائن: ${formatCurrency(kotlin.math.abs(item.receivableBalance))}"
        } else {
            "✅ الحساب خالص ومسدد بالكامل"
        }

        return if (customer.isWellOwner) {
            val ownerPayableStatus = when {
                item.payableBalance > 0 -> "⚠️ المتبقي المستحق لكم علينا: ${formatCurrency(item.payableBalance)}"
                item.payableBalance < 0 -> "✅ لدينا رصيد دائن عليكم: ${formatCurrency(kotlin.math.abs(item.payableBalance))}"
                else -> "✅ مستحقات الشراء مسددة بالكامل"
            }
            val ownerReceivableStatus = when {
                item.receivableBalance > 0 -> "⚠️ المستحق لنا عليكم عن السقي: ${formatCurrency(item.receivableBalance)}"
                item.receivableBalance < 0 -> "✅ لكم رصيد عن السقي: ${formatCurrency(kotlin.math.abs(item.receivableBalance))}"
                else -> "✅ حساب السقي مسدد بالكامل"
            }
            val ownerWasteMinutes = (item.totalPurchasedMinutes - item.totalChargeablePurchasedMinutes).coerceAtLeast(0)
            """
                *كشف حساب صاحب البئر - ${config.distributorName.ifEmpty { "المسرب" }}*
                💧 صاحب البئر: ${customer.name}
                📍 البئر / الموقع: ${customer.farmName.ifEmpty { customer.location.ifEmpty { "عام" } }}
                🛒 شراء الساعات: ${formatDuration(item.totalPurchasedMinutes)}، المحتسب بعد الهدر: ${formatDuration(item.totalChargeablePurchasedMinutes)}
                🕒 هدر على صاحب البئر: ${formatDuration(ownerWasteMinutes)} (خصم ${formatCurrency(item.totalOwnerWasteCredit)})
                💰 صافي قيمة المشتريات: ${formatCurrency(item.totalPurchaseAmount)}
                💸 المسدد لصاحب البئر: ${formatCurrency(item.totalDisbursedAmount)}
                $ownerPayableStatus
                ------------------------
                💧 سقي / بيع لصاحب البئر: ${formatDuration(item.totalSoldMinutes)}، بقيمة ${formatCurrency(item.totalBilledAmount)}
                💵 المحصل منه عن السقي: ${formatCurrency(item.totalPaidAmount)}
                $ownerReceivableStatus
                ------------------------
                *الحسابان مستقلان ولا تتم المقاصة تلقائياً.*
                ${com.example.core.util.FileSharingHelper.MESSAGE_FOOTER}
            """.trimIndent()
        } else {
            """
                *كشف حساب مياه - ${config.distributorName.ifEmpty { "المسرب" }}*
                👤 العميل: ${customer.name}
                📍 المزرعة: ${customer.farmName.ifEmpty { "عام" }}
                ⏱️ ساعات البيع بعد هدر المسرب: ${formatDuration(item.totalSoldMinutes)}
                💰 إجمالي قيمة المسارب: ${formatCurrency(item.totalBilledAmount)}
                💵 إجمالي المسدد نقداً: ${formatCurrency(item.totalPaidAmount)}
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

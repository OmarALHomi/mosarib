package com.example.features.customers

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.license.LicenseManager
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.ComprehensiveCustomerRow
import com.example.core.util.ComprehensiveReportTotals
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

    suspend fun insertCustomerDirect(customer: Customer): Long {
        return customerRepo.insertCustomer(customer)
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            // Keep financial history and foreign-key references intact; archived accounts disappear
            // from active lists but remain available to statements and backups.
            customerRepo.archiveCustomer(customer.id)
            showToast("تمت أرشفة الحساب مع الاحتفاظ بسجلاته المالية", ToastType.INFO)
        }
    }

    // --- ميزات الاستيراد والتصدير من وإلى إكسل (Smart Excel & CSV) ---
    private val _importDrafts = MutableStateFlow<List<ImportedCustomerDraft>?>(null)
    val importDrafts: StateFlow<List<ImportedCustomerDraft>?> = _importDrafts.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun startImportFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val existing = customerRepo.getAllCustomersDirect()
                val drafts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    CustomerExcelHelper.parseCustomerFile(context, uri, existing)
                }
                if (drafts.isEmpty()) {
                    showToast("الملف فارغ أو لم يتم التعرف على بيانات عملاء صالحة", ToastType.WARNING)
                } else {
                    _importDrafts.value = drafts
                }
            } catch (e: Exception) {
                showToast("تعذر قراءة ملف الإكسل: ${e.localizedMessage}", ToastType.ERROR)
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun updateImportDraft(updatedDraft: ImportedCustomerDraft) {
        val current = _importDrafts.value ?: return
        _importDrafts.value = current.map { if (it.localId == updatedDraft.localId) updatedDraft else it }
    }

    fun removeImportDraft(localId: String) {
        val current = _importDrafts.value ?: return
        _importDrafts.value = current.filter { it.localId != localId }
    }

    fun toggleSelectAllImportDrafts(select: Boolean) {
        val current = _importDrafts.value ?: return
        _importDrafts.value = current.map {
            if (it.validationError == null) it.copy(isSelected = select) else it
        }
    }

    fun confirmImport(updateExisting: Boolean) {
        val current = _importDrafts.value ?: return
        val selected = current.filter { it.isSelected && it.name.isNotBlank() }
        if (selected.isEmpty()) {
            showToast("يرجى تحديد حساب واحد على الأقل للاستيراد", ToastType.WARNING)
            return
        }

        viewModelScope.launch {
            try {
                var insertedCount = 0
                var updatedCount = 0
                val existingCustomers = customerRepo.getAllCustomersDirect()
                val toInsert = mutableListOf<Customer>()

                for (draft in selected) {
                    val existing = if (updateExisting) {
                        existingCustomers.find {
                            (draft.existingCustomerId != null && it.id == draft.existingCustomerId) ||
                                it.name.trim().equals(draft.name.trim(), ignoreCase = true) ||
                                (it.phone.isNotBlank() && it.phone == draft.phone)
                        }
                    } else null

                    if (existing != null) {
                        val updated = existing.copy(
                            phone = if (draft.phone.isNotBlank()) draft.phone else existing.phone,
                            farmName = if (draft.farmName.isNotBlank()) draft.farmName else existing.farmName,
                            location = if (draft.location.isNotBlank()) draft.location else existing.location,
                            notes = if (draft.notes.isNotBlank()) draft.notes else existing.notes,
                            customPricePerHour = draft.customPricePerHour ?: existing.customPricePerHour,
                            isWellOwner = draft.isWellOwner || existing.isWellOwner
                        )
                        customerRepo.updateCustomer(updated)
                        updatedCount++
                    } else {
                        val newCustomer = Customer(
                            name = draft.name.trim(),
                            phone = draft.phone.trim(),
                            farmName = draft.farmName.trim(),
                            location = draft.location.trim(),
                            notes = draft.notes.trim(),
                            customPricePerHour = draft.customPricePerHour,
                            isWellOwner = draft.isWellOwner,
                            isBeneficiary = false,
                            createdAt = System.currentTimeMillis(),
                            isArchived = false
                        )
                        val newId = db.customerDao().insertCustomer(newCustomer)
                        insertedCount++

                        // تسجيل الرصيد الافتتاحي المستورد (من دفتر الحسابات والتطبيقات الخارجية)
                        if (draft.initialBalance > 0.0) {
                            val now = System.currentTimeMillis()
                            if (draft.isDebit) {
                                // دين سابق مستحق على العميل (مدين)
                                db.waterSessionDao().insertSession(
                                    com.example.features.sessions.WaterSession(
                                        customerId = newId,
                                        pumpName = "رصيد افتتاحي",
                                        startTime = now,
                                        endTime = now,
                                        durationMinutes = 0,
                                        pricePerHour = 0.0,
                                        totalAmount = draft.initialBalance,
                                        amountPaid = 0.0,
                                        remainingDebt = draft.initialBalance,
                                        notes = if (draft.notes.isNotBlank()) "رصيد سابق: ${draft.notes}" else "رصيد ودين سابق مستورد من دفتر الحسابات",
                                        createdAt = now
                                    )
                                )
                            } else {
                                // رصيد دائن / دفعة مقدمة (له)
                                db.voucherDao().insertVoucher(
                                    com.example.features.vouchers.Voucher(
                                        type = com.example.features.vouchers.VoucherType.RECEIPT,
                                        customerId = newId,
                                        amount = draft.initialBalance,
                                        category = "رصيد افتتاحي",
                                        paymentMethod = "رصيد سابق",
                                        date = now,
                                        notes = if (draft.notes.isNotBlank()) "رصيد دائن: ${draft.notes}" else "رصيد دائن سابق مستورد من دفتر الحسابات",
                                        createdAt = now
                                    )
                                )
                            }
                        }
                    }
                }

                _importDrafts.value = null

                val msg = when {
                    insertedCount > 0 && updatedCount > 0 -> "تم استيراد $insertedCount حساب جديد وتحديث $updatedCount حساب بنجاح"
                    insertedCount > 0 -> "تم استيراد $insertedCount حساب بنجاح"
                    updatedCount > 0 -> "تم تحديث $updatedCount حساب بنجاح"
                    else -> "اكتملت العملية"
                }
                showToast(msg, ToastType.SUCCESS)
            } catch (e: Exception) {
                showToast("حدث خطأ أثناء حفظ الحسابات: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    fun cancelImport() {
        _importDrafts.value = null
    }

    fun exportCustomersToExcel(context: Context) {
        viewModelScope.launch {
            try {
                val list = rawCustomersWithBalance.value
                if (list.isEmpty()) {
                    showToast("لا يوجد عملاء لتصديرهم", ToastType.WARNING)
                    return@launch
                }
                val currency = appConfig.value.currencySymbol
                val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    CustomerExcelHelper.exportCustomersToExcel(context, list, currency)
                }
                FileSharingHelper.shareFile(
                    context = context,
                    file = file,
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    title = "مشاركة قائمة العملاء والمزارع (Excel)"
                )
            } catch (e: Exception) {
                showToast("فشل تصدير ملف الإكسل: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    fun exportCustomersToCsv(context: Context) {
        viewModelScope.launch {
            try {
                val list = rawCustomersWithBalance.value
                if (list.isEmpty()) {
                    showToast("لا يوجد عملاء لتصديرهم", ToastType.WARNING)
                    return@launch
                }
                val currency = appConfig.value.currencySymbol
                val file = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    CustomerExcelHelper.exportCustomersToCsv(context, list, currency)
                }
                FileSharingHelper.shareFile(
                    context = context,
                    file = file,
                    mimeType = "text/csv",
                    title = "مشاركة قائمة العملاء (CSV)"
                )
            } catch (e: Exception) {
                showToast("فشل تصدير ملف CSV: ${e.localizedMessage}", ToastType.ERROR)
            }
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
        notes: String,
        amountPaid: Double = 0.0
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
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val newId = ownerPurchaseRepo.insert(
                WellOwnerPurchase(
                    ownerCustomerId = ownerCustomerId,
                    date = date,
                    durationMinutes = durationMinutes,
                    wastedMinutesOnOwner = wastedMinutesOnOwner,
                    purchaseRatePerHour = com.example.core.util.Formatters.roundMoney(purchaseRatePerHour),
                    amountPaid = com.example.core.util.Formatters.roundMoney(amountPaid),
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
                    amount = com.example.core.util.Formatters.roundMoney(amountPaid),
                    category = "تسديد نقدي لشراء ساعات",
                    paymentMethod = "نقداً",
                    date = date,
                    notes = "دفعة مسلّمة فوراً لصاحب البئر عند شراء ساعات [شراء #$newId]"
                )
                voucherRepo.insertVoucher(voucher)
            }

            LicenseManager.recordOperationPerformed(getApplication(), totalOps)
            showToast("تم تسجيل شراء الساعات وتحديث حساب صاحب البئر", ToastType.SUCCESS)
        }
    }

    /**
     * تسجيل هدر على صاحب البئر يُخصم من آخر عملية شراء له ويقلل مستحقاته بسعر الشراء
     */
    fun recordWasteOnOwner(
        ownerCustomerId: Long,
        wastedMinutes: Int,
        reason: String
    ) {
        viewModelScope.launch {
            val owner = customerRepo.getCustomerByIdDirect(ownerCustomerId)
            if (owner == null || !owner.isWellOwner) {
                showToast("حساب صاحب البئر غير موجود", ToastType.ERROR)
                return@launch
            }
            val purchases = db.wellOwnerPurchaseDao().getPurchasesForOwner(ownerCustomerId).first()
            val latestPurchase = purchases.maxByOrNull { it.date }
            if (latestPurchase == null) {
                showToast("لا توجد عمليات شراء سابقة لتسجيل الهدر عليها", ToastType.ERROR)
                return@launch
            }
            val availableMinutes = latestPurchase.durationMinutes - latestPurchase.wastedMinutesOnOwner
            if (wastedMinutes <= 0) {
                showToast("يرجى إدخال مدة هدر صالحة", ToastType.ERROR)
                return@launch
            }
            if (wastedMinutes > availableMinutes) {
                showToast("لا يمكن أن يكون الهدر أكبر من عملية الشراء بتاتاً! المتاح للهدر: ${com.example.core.util.Formatters.formatDurationArabic(availableMinutes)}", ToastType.ERROR)
                return@launch
            }
            val updatedPurchase = latestPurchase.copy(
                wastedMinutesOnOwner = latestPurchase.wastedMinutesOnOwner + wastedMinutes,
                notes = if (reason.isNotBlank()) "${latestPurchase.notes} | هدر مخصوم: $reason".trim(' ', '|') else latestPurchase.notes
            )
            ownerPurchaseRepo.update(updatedPurchase)
            val credit = com.example.core.util.Formatters.calculateWaterCost(wastedMinutes, latestPurchase.purchaseRatePerHour)
            showToast("تم خصم هدر ${com.example.core.util.Formatters.formatDurationArabic(wastedMinutes)} بمبلغ ${com.example.core.util.Formatters.formatCurrency(credit, "")} من مستحقات صاحب البئر", ToastType.SUCCESS)
        }
    }

    /**
     * تسجيل دورة سقي مباشرة للعميل أو على حساب صاحب البئر دون مغادرة شاشة العميل
     */
    fun addSessionForCustomer(
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
            val grossCost = com.example.core.util.Formatters.calculateWaterCost(netMinutes, pricePerHour)
            val totalCost = com.example.core.util.Formatters.roundMoney(maxOf(0.0, grossCost - discountAmount))
            val roundedPaid = com.example.core.util.Formatters.roundMoney(amountPaid)
            val isOwnerOrBilled = billedToCustomerId != null || customerRepo.getCustomerByIdDirect(customerId)?.isWellOwner == true

            val excessPaid = if (!isOwnerOrBilled && roundedPaid > totalCost) {
                com.example.core.util.Formatters.roundMoney(roundedPaid - totalCost)
            } else 0.0

            val effectivePaidForSession = if (excessPaid > 0.0) totalCost else roundedPaid
            val debt = if (isOwnerOrBilled) 0.0 else com.example.core.util.Formatters.roundMoney(maxOf(0.0, totalCost - effectivePaidForSession))

            val session = WaterSession(
                id = 0L,
                customerId = customerId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = if (endTime > startTime) endTime else (startTime + totalMinutes * 60000L),
                durationMinutes = totalMinutes,
                pricePerHour = com.example.core.util.Formatters.roundMoney(pricePerHour),
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

            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() + db.wellOwnerPurchaseDao().getPurchasesCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }
            val newSessionId = sessionRepo.insertSession(session)
            LicenseManager.recordOperationPerformed(getApplication(), totalOps)

            if (excessPaid > 0.0) {
                val unpaidSessions = db.waterSessionDao()
                    .getUnpaidSessionsForCustomerDirect(customerId)
                    .filter { it.id != newSessionId }

                var remainingExcess = excessPaid
                val settledSessionIds = mutableListOf<Long>()
                for (oldSession in unpaidSessions) {
                    if (remainingExcess <= 0.0) break
                    val settleAmt = minOf(remainingExcess, oldSession.remainingDebt)
                    val newOldPaid = com.example.core.util.Formatters.roundMoney(oldSession.amountPaid + settleAmt)
                    val newOldDebt = com.example.core.util.Formatters.roundMoney(maxOf(0.0, oldSession.totalAmount - newOldPaid))
                    sessionRepo.updateSession(oldSession.copy(amountPaid = newOldPaid, remainingDebt = newOldDebt))
                    remainingExcess = com.example.core.util.Formatters.roundMoney(remainingExcess - settleAmt)
                    settledSessionIds.add(oldSession.id)
                }

                val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
                val notesText = if (settledSessionIds.isNotEmpty()) {
                    val listStr = settledSessionIds.joinToString(", ") { "#$it" }
                    if (remainingExcess > 0.0) {
                        "سند قبض: سداد ديون سابقة لجلسات ($listStr) بمبلغ ${com.example.core.util.Formatters.formatCurrency(excessPaid - remainingExcess, "")} ومتبقي ${com.example.core.util.Formatters.formatCurrency(remainingExcess, "")} رصيد دائن للعميل (فائض نقدي من سقي #$newSessionId)"
                    } else {
                        "سند قبض: سداد ديون سابقة لجلسات ($listStr) بالكامل من فائض نقدي لسقي #$newSessionId"
                    }
                } else {
                    "سند قبض: رصيد دائن لصالح العميل (فائض نقدي من سقي #$newSessionId)"
                }

                val voucher = Voucher(
                    voucherNumber = vNumber,
                    type = VoucherType.RECEIPT,
                    customerId = customerId,
                    sessionId = newSessionId,
                    amount = excessPaid,
                    category = if (settledSessionIds.isNotEmpty()) "سداد ديون سابقة" else "رصيد دائن للعميل",
                    paymentMethod = "نقداً",
                    date = System.currentTimeMillis(),
                    notes = notesText
                )
                db.voucherDao().insertVoucher(voucher)
                showToast("تم تسجيل السقي وسند قبض بالفائض (${com.example.core.util.Formatters.formatCurrency(excessPaid, "")}) وتسوية الحساب", ToastType.SUCCESS)
            } else {
                showToast("تم تسجيل دورة السقي بنجاح وتحديث كشف الحساب", ToastType.SUCCESS)
            }
        }
    }

    fun updateSessionForCustomer(
        id: Long,
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
            val maxSessionMinutes = 10 * 24 * 60
            if (totalMinutes > maxSessionMinutes) {
                showToast("لا يمكن تسجيل سقي بساعات خيالية! الحد الأقصى 10 أيام", ToastType.ERROR)
                return@launch
            }
            if (wastedMinutes > totalMinutes) {
                showToast("لا يمكن أن يكون الهدر أكبر من مدة السقي بتاتاً", ToastType.ERROR)
                return@launch
            }
            val netMinutes = maxOf(0, totalMinutes - wastedMinutes)
            val grossCost = com.example.core.util.Formatters.calculateWaterCost(netMinutes, pricePerHour)
            val totalCost = com.example.core.util.Formatters.roundMoney(maxOf(0.0, grossCost - discountAmount))
            val roundedPaid = com.example.core.util.Formatters.roundMoney(amountPaid)
            val isOwnerOrBilled = billedToCustomerId != null || customerRepo.getCustomerByIdDirect(customerId)?.isWellOwner == true

            val excessPaid = if (!isOwnerOrBilled && roundedPaid > totalCost) {
                com.example.core.util.Formatters.roundMoney(roundedPaid - totalCost)
            } else 0.0

            val effectivePaidForSession = if (excessPaid > 0.0) totalCost else roundedPaid
            val debt = if (isOwnerOrBilled) 0.0 else com.example.core.util.Formatters.roundMoney(maxOf(0.0, totalCost - effectivePaidForSession))

            val session = WaterSession(
                id = id,
                customerId = customerId,
                pumpName = pumpName,
                startTime = startTime,
                endTime = if (endTime > startTime) endTime else (startTime + totalMinutes * 60000L),
                durationMinutes = totalMinutes,
                pricePerHour = com.example.core.util.Formatters.roundMoney(pricePerHour),
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
            sessionRepo.updateSession(session)

            if (excessPaid > 0.0) {
                val unpaidSessions = db.waterSessionDao()
                    .getUnpaidSessionsForCustomerDirect(customerId)
                    .filter { it.id != id }

                var remainingExcess = excessPaid
                val settledSessionIds = mutableListOf<Long>()
                for (oldSession in unpaidSessions) {
                    if (remainingExcess <= 0.0) break
                    val settleAmt = minOf(remainingExcess, oldSession.remainingDebt)
                    val newOldPaid = com.example.core.util.Formatters.roundMoney(oldSession.amountPaid + settleAmt)
                    val newOldDebt = com.example.core.util.Formatters.roundMoney(maxOf(0.0, oldSession.totalAmount - newOldPaid))
                    sessionRepo.updateSession(oldSession.copy(amountPaid = newOldPaid, remainingDebt = newOldDebt))
                    remainingExcess = com.example.core.util.Formatters.roundMoney(remainingExcess - settleAmt)
                    settledSessionIds.add(oldSession.id)
                }

                val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(4)}"
                val notesText = if (settledSessionIds.isNotEmpty()) {
                    val listStr = settledSessionIds.joinToString(", ") { "#$it" }
                    if (remainingExcess > 0.0) {
                        "سند قبض: سداد ديون سابقة لجلسات ($listStr) بمبلغ ${com.example.core.util.Formatters.formatCurrency(excessPaid - remainingExcess, "")} ومتبقي ${com.example.core.util.Formatters.formatCurrency(remainingExcess, "")} رصيد دائن للعميل (فائض نقدي من سقي #$id)"
                    } else {
                        "سند قبض: سداد ديون سابقة لجلسات ($listStr) بالكامل من فائض نقدي لسقي #$id"
                    }
                } else {
                    "سند قبض: رصيد دائن لصالح العميل (فائض نقدي من سقي #$id)"
                }

                val voucher = Voucher(
                    voucherNumber = vNumber,
                    type = VoucherType.RECEIPT,
                    customerId = customerId,
                    sessionId = id,
                    amount = excessPaid,
                    category = if (settledSessionIds.isNotEmpty()) "سداد ديون سابقة" else "رصيد دائن للعميل",
                    paymentMethod = "نقداً",
                    date = System.currentTimeMillis(),
                    notes = notesText
                )
                db.voucherDao().insertVoucher(voucher)
                showToast("تم تعديل السقي وسند قبض بالفائض (${com.example.core.util.Formatters.formatCurrency(excessPaid, "")}) وتسوية الحساب", ToastType.SUCCESS)
            } else {
                showToast("تم تعديل دورة السقي بنجاح وتحديث كشف الحساب", ToastType.SUCCESS)
            }
        }
    }

    fun updateOwnerPurchase(
        purchaseId: Long,
        ownerCustomerId: Long,
        date: Long,
        durationMinutes: Int,
        wastedMinutesOnOwner: Int,
        purchaseRatePerHour: Double,
        notes: String,
        amountPaid: Double = 0.0
    ) {
        viewModelScope.launch {
            if (durationMinutes <= 0) {
                showToast("يرجى إدخال مدة شراء صالحة", ToastType.ERROR)
                return@launch
            }
            if (wastedMinutesOnOwner > durationMinutes) {
                showToast("لا يمكن أن يكون الهدر أكبر من مدة الشراء", ToastType.ERROR)
                return@launch
            }
            if (purchaseRatePerHour <= 0.0) {
                showToast("يرجى تحديد سعر شراء صالح للساعة", ToastType.ERROR)
                return@launch
            }
            val roundedPaid = com.example.core.util.Formatters.roundMoney(amountPaid)
            val purchase = WellOwnerPurchase(
                id = purchaseId,
                ownerCustomerId = ownerCustomerId,
                date = date,
                durationMinutes = durationMinutes,
                wastedMinutesOnOwner = wastedMinutesOnOwner,
                purchaseRatePerHour = com.example.core.util.Formatters.roundMoney(purchaseRatePerHour),
                amountPaid = roundedPaid,
                notes = notes.trim()
            )
            ownerPurchaseRepo.update(purchase)

            // مزامنة سند الصرف المرتبط بعملية الشراء
            val existingVouchers = voucherRepo.getVouchersForCustomerDirect(ownerCustomerId)
            val linkedExpenseVoucher = existingVouchers.find {
                it.type == VoucherType.EXPENSE && it.notes.contains("[شراء #$purchaseId]")
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
                            customerId = ownerCustomerId,
                            amount = roundedPaid,
                            category = "تسديد نقدي لشراء ساعات",
                            paymentMethod = "نقداً",
                            date = date,
                            notes = "دفعة مسلّمة فوراً لصاحب البئر عند شراء ساعات [شراء #$purchaseId]"
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
            showToast("تم حذف حركة الشراء وتحديث كشف صاحب البئر", ToastType.INFO)
        }
    }

    fun addReceiptVoucher(customerId: Long, amount: Double, paymentMethod: String, notes: String) {
        viewModelScope.launch {
            if (amount <= 0.0) {
                showToast("يرجى إدخال مبلغ صالح أكبر من صفر", ToastType.ERROR)
                return@launch
            }
            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(8)}"
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
            if (amount <= 0.0) {
                showToast("يرجى إدخال مبلغ صالح أكبر من صفر", ToastType.ERROR)
                return@launch
            }
            val totalOps = db.waterSessionDao().getSessionsCountDirect() +
                db.voucherDao().getVouchersCountDirect() +
                db.wellOwnerPurchaseDao().getPurchasesCountDirect()
            if (!LicenseManager.canPerformOperation(getApplication(), totalOps)) {
                showToast("استنفدت 200 عملية مجانية. يرجى تفعيل النسخة الكاملة للتطبيق", ToastType.ERROR)
                return@launch
            }

            val vNumber = "EXP-${System.currentTimeMillis().toString().takeLast(8)}"
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

            val vNumber = "REC-${System.currentTimeMillis().toString().takeLast(8)}"
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
            if (voucher.sessionId != null) {
                val session = sessionRepo.getSessionById(voucher.sessionId)
                if (session != null) {
                    val newPaid = (session.amountPaid - voucher.amount).coerceAtLeast(0.0)
                    val newDebt = (session.totalAmount - newPaid).coerceAtLeast(0.0)
                    sessionRepo.updateSession(
                        session.copy(
                            amountPaid = com.example.core.util.Formatters.roundMoney(newPaid),
                            remainingDebt = com.example.core.util.Formatters.roundMoney(newDebt)
                        )
                    )
                }
            }
            voucherRepo.deleteVoucher(voucher)
            showToast("تم حذف السند وتحديث الرصيد", ToastType.INFO)
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
            showToast("تم حذف دورة الماء وتحديث الحساب", ToastType.INFO)
        }
    }

    fun generateCustomerStatementPdf(customer: Customer) {
        viewModelScope.launch {
            try {
                val sessions = getCustomerSessions(customer.id).first()
                val purchases = ownerPurchaseRepo.getPurchasesForOwner(customer.id).first()
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

    fun generateAllCustomersReportPdf() {
        viewModelScope.launch {
            try {
                val list = rawCustomersWithBalance.value
                if (list.isEmpty()) {
                    showToast("لا يوجد عملاء لطباعة الكشف", ToastType.WARNING)
                    return@launch
                }
                val config = appConfig.value
                val rows = list.map { item ->
                    ComprehensiveCustomerRow(
                        customerName = item.customer.name,
                        phone = item.customer.phone,
                        farmName = item.customer.farmName,
                        sessionsCount = item.totalSessionsCount,
                        waterMinutes = item.totalMinutes,
                        billed = item.totalBilledAmount,
                        paid = item.totalPaidAmount,
                        balance = item.balance,
                        isWellOwner = item.customer.isWellOwner,
                        purchasedMinutes = item.totalPurchasedMinutes,
                        chargeablePurchasedMinutes = item.totalChargeablePurchasedMinutes,
                        ownerWasteMinutes = item.totalPurchasedMinutes - item.totalChargeablePurchasedMinutes,
                        ownerWasteCredit = item.totalOwnerWasteCredit,
                        payableBalance = item.payableBalance,
                        receivableBalance = item.receivableBalance
                    )
                }
                val file = PdfReportGenerator.generateCustomerDirectoryPdf(
                    context = getApplication(),
                    config = config,
                    customerRows = rows
                )
                _pdfReadyFile.value = Pair(file, "كشف وحسابات دليل العملاء")
            } catch (e: Exception) {
                showToast("فشل في إنشاء التقرير: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    fun buildCustomerStatementMessage(customer: Customer, item: CustomerWithBalance, isSms: Boolean = false): String {
        val config = appConfig.value
        return com.example.core.util.MessageFormatHelper.buildCustomerStatement(customer, item, config, isSms)
    }

    fun sendCustomerStatementWhatsApp(customer: Customer, item: CustomerWithBalance) {
        val msg = buildCustomerStatementMessage(customer, item, isSms = false)
        FileSharingHelper.sendWhatsAppMessage(getApplication(), customer.phone, msg)
    }
}

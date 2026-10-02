package com.example.features.deals

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.sync.DealsSyncManager
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class DealsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.settlementDealDao()

    val allDeals = dao.getAllDeals()

    private val _filterStatus = MutableStateFlow("ALL") // "ALL", "ACTIVE", "COMPLETED"
    val filterStatus: StateFlow<String> = _filterStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private var dealsListener: ListenerRegistration? = null

    val filteredDeals: StateFlow<List<SettlementDeal>> = combine(
        allDeals,
        _filterStatus,
        _searchQuery
    ) { deals, status, query ->
        deals.filter { deal ->
            val matchesStatus = when (status) {
                "ACTIVE" -> !deal.isCompleted
                "COMPLETED" -> deal.isCompleted
                else -> true
            }
            val matchesQuery = if (query.isBlank()) true else {
                deal.dealNumber.contains(query, ignoreCase = true) ||
                deal.cropTitle.contains(query, ignoreCase = true) ||
                deal.sellerName.contains(query, ignoreCase = true) ||
                deal.buyerName.contains(query, ignoreCase = true) ||
                deal.dallalName.contains(query, ignoreCase = true) ||
                deal.location.contains(query, ignoreCase = true)
            }
            matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        try {
            dealsListener = DealsSyncManager.startListeningToDeals(dao, viewModelScope)
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissToast() {
        _toast.value = null
    }

    /**
     * Creates a formal settlement deal, saves locally and pushes to Firestore.
     */
    fun createDeal(
        cropTitle: String,
        cropType: String,
        location: String,
        sellerName: String,
        sellerPhone: String,
        buyerName: String,
        buyerPhone: String,
        dallalName: String,
        dallalPhone: String,
        totalAmount: Double,
        advancePayment: Double,
        dallalCommission: Double,
        dueDate: Long?,
        termsNotes: String,
        onComplete: (SettlementDeal?) -> Unit
    ) {
        if (sellerName.isBlank() || buyerName.isBlank() || totalAmount <= 0.0) {
            _toast.value = ToastMessage("يرجى إدخال اسم البائع والمشتري ومبلغ الصفقة بدقة", ToastType.ERROR)
            onComplete(null)
            return
        }

        val id = UUID.randomUUID().toString().take(12)
        val shortSeq = (1000..9999).random()
        val dealNumber = "SLH-$shortSeq"
        val remaining = (totalAmount - advancePayment).coerceAtLeast(0.0)
        val status = if (remaining <= 0.0) "COMPLETED" else "ACTIVE"

        val deal = SettlementDeal(
            id = id,
            dealNumber = dealNumber,
            cropTitle = cropTitle.trim(),
            cropType = cropType.trim(),
            location = location.trim(),
            sellerName = sellerName.trim(),
            sellerPhone = sellerPhone.trim(),
            buyerName = buyerName.trim(),
            buyerPhone = buyerPhone.trim(),
            dallalName = dallalName.trim(),
            dallalPhone = dallalPhone.trim(),
            totalAmount = totalAmount,
            advancePayment = advancePayment,
            dallalCommission = dallalCommission,
            commissionPaid = 0.0,
            remainingAmount = remaining,
            status = status,
            dealDate = System.currentTimeMillis(),
            dueDate = dueDate,
            termsNotes = termsNotes.trim(),
            syncStatus = "SYNCED"
        )

        viewModelScope.launch {
            _isLoading.value = true
            val result = DealsSyncManager.publishDeal(deal, dao)
            _isLoading.value = false

            if (result.isSuccess) {
                _toast.value = ToastMessage("تم توثيق عقد الصلح بنجاح ومزامنته سحابياً 📜🤝", ToastType.SUCCESS)
            } else {
                _toast.value = ToastMessage("تم حفظ العقد محلياً وسيرفع للسحابة فور توفر الإنترنت", ToastType.INFO)
            }
            onComplete(deal)
        }
    }

    /**
     * Records an installment payment towards a deal.
     */
    fun recordPayment(
        deal: SettlementDeal,
        amount: Double,
        paidBy: String,
        notes: String,
        onComplete: () -> Unit
    ) {
        if (amount <= 0.0) {
            _toast.value = ToastMessage("يرجى إدخال مبلغ صحيح للدفعة", ToastType.ERROR)
            return
        }

        val newRemaining = (deal.remainingAmount - amount).coerceAtLeast(0.0)
        val payment = DealPayment(
            id = UUID.randomUUID().toString().take(10),
            dealId = deal.id,
            amount = amount,
            paidBy = paidBy,
            paymentType = "INSTALLMENT",
            notes = notes.trim(),
            paymentDate = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _isLoading.value = true
            DealsSyncManager.recordPayment(deal.id, payment, newRemaining, dao)
            _isLoading.value = false

            val msg = if (newRemaining <= 0.0) {
                "تم سداد كامل ثمن الصلح واكتمال العقد بنجاح! 🎉"
            } else {
                "تم تسجيل دفعة بقيمة $amount ريال بنجاح 💵"
            }
            _toast.value = ToastMessage(msg, ToastType.SUCCESS)
            onComplete()
        }
    }

    fun shareContract(context: Context, deal: SettlementDeal, phone: String = "") {
        SettlementDocHelper.shareContract(context, deal, phone)
    }

    fun deleteDeal(dealId: String) {
        viewModelScope.launch {
            dao.deleteDeal(dealId)
            _toast.value = ToastMessage("تم حذف العقد من السجل المحلي", ToastType.INFO)
        }
    }

    override fun onCleared() {
        super.onCleared()
        dealsListener?.remove()
    }
}

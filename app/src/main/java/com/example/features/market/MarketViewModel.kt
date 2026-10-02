package com.example.features.market

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.sync.MarketSyncManager
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.FileSharingHelper
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MarketViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.cropListingDao()

    val allListings = dao.getAllListings()

    private val _selectedCrop = MutableStateFlow("الكل")
    val selectedCrop: StateFlow<String> = _selectedCrop.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private var listenerRegistration: ListenerRegistration? = null

    // Combined filtered listings
    val filteredListings: StateFlow<List<CropListing>> = combine(
        allListings,
        _selectedCrop,
        _searchQuery
    ) { listings, crop, query ->
        listings.filter { item ->
            val matchesCrop = if (crop == "الكل") true else item.cropType == crop
            val matchesQuery = if (query.isBlank()) true else {
                item.title.contains(query, ignoreCase = true) ||
                item.district.contains(query, ignoreCase = true) ||
                item.village.contains(query, ignoreCase = true) ||
                item.dallalName.contains(query, ignoreCase = true)
            }
            matchesCrop && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Start real-time Firestore sync
        try {
            listenerRegistration = MarketSyncManager.startListeningToListings(
                dao = dao,
                coroutineScope = viewModelScope,
                onSyncStateChanged = { loading -> _isLoading.value = loading }
            )
        } catch (e: Exception) {
            // Offline fallback
        }
    }

    fun setCropFilter(crop: String) {
        _selectedCrop.value = crop
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun dismissToast() {
        _toast.value = null
    }

    /**
     * Publishes a new harvest listing to the market.
     */
    fun createListing(
        title: String,
        cropType: String,
        description: String,
        district: String,
        village: String,
        priceEstimate: Double,
        priceUnit: String,
        dallalName: String,
        dallalPhone: String,
        farmerName: String,
        farmerPhone: String,
        hideFarmerPhone: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        if (title.isBlank()) {
            _toast.value = ToastMessage("يرجى إدخال عنوان واضح للمحصول المعروض", ToastType.ERROR)
            onComplete(false)
            return
        }

        val listing = CropListing(
            id = UUID.randomUUID().toString().take(12),
            title = title.trim(),
            cropType = cropType,
            description = description.trim(),
            district = district.trim(),
            village = village.trim(),
            priceEstimate = priceEstimate,
            priceUnit = priceUnit.ifBlank { "شروة كاملة" },
            dallalName = dallalName.trim(),
            dallalPhone = dallalPhone.trim(),
            farmerName = farmerName.trim(),
            farmerPhone = farmerPhone.trim(),
            hideFarmerPhone = hideFarmerPhone,
            status = "AVAILABLE",
            createdAt = System.currentTimeMillis(),
            isFeatured = false
        )

        viewModelScope.launch {
            _isLoading.value = true
            val result = MarketSyncManager.publishListing(listing, dao)
            _isLoading.value = false

            if (result.isSuccess) {
                _toast.value = ToastMessage("تم نشر عرض المحصول في بورصة جِربة بنجاح 🌾", ToastType.SUCCESS)
                onComplete(true)
            } else {
                _toast.value = ToastMessage("تم حفظ العرض محلياً وسيرفع للسوق فور توفر الإنترنت", ToastType.INFO)
                onComplete(true)
            }
        }
    }

    /**
     * Updates listing status (e.g. mark as SOLD).
     */
    fun updateStatus(listingId: String, newStatus: String) {
        viewModelScope.launch {
            MarketSyncManager.updateListingStatus(listingId, newStatus, dao)
            val msg = if (newStatus == "SOLD") "تم تحديث العرض: تم البيع والصلح بنجاح 🤝" else "تم تحديث حالة العرض"
            _toast.value = ToastMessage(msg, ToastType.SUCCESS)
        }
    }

    /**
     * Deletes a listing from the local database.
     */
    fun deleteListing(listingId: String) {
        viewModelScope.launch {
            dao.deleteListing(listingId)
            _toast.value = ToastMessage("تم حذف العرض من القائمة", ToastType.INFO)
        }
    }

    /**
     * Opens WhatsApp with the broker with a pre-filled deal negotiation message.
     */
    fun contactDallalViaWhatsApp(context: Context, listing: CropListing) {
        val phone = listing.dallalPhone.ifBlank { "967773712030" }
        val msg = """
السلام عليكم ورحمة الله وبركاته،
الأخ الدلال: ${listing.dallalName.ifBlank { "المحترم" }}،
بخصوص عرض المحصول المعروض في تطبيق [جِربة]:
🌾 المحصول: ${listing.title} (${listing.cropType})
📍 الموقع: ${listing.district} - ${listing.village}
💰 السعر المعروض: ${listing.getDisplayPrice()}

أود الاستفسار والتفاوض ومعاينة الثمرة.
        """.trimIndent()

        FileSharingHelper.sendWhatsAppMessage(context, phone, msg)
    }

    /**
     * Sends a marketing request from a farmer to a Dallal.
     */
    fun sendMarketingRequestWhatsApp(
        context: Context,
        farmerName: String,
        cropType: String,
        village: String,
        dallalPhone: String,
        notes: String
    ) {
        val phone = dallalPhone.ifBlank { "967773712030" }
        val msg = """
السلام عليكم ورحمة الله وبركاته،
معك المزارع: ${farmerName.ifBlank { "صاحب المزرعة" }}
📍 القرية / المحل: ${village.ifBlank { "العزلة" }}
🌾 نوع المحصول: $cropType

عندي ثمرة/محصول جاهز للقطاف والتسويق، وأرغب في تكليفك بالدلالة والوساطة لبيعه عبر تطبيق [جِربة].
${if (notes.isNotBlank()) "📝 تفاصيل إضافية: $notes" else ""}

يرجى التواصل للتنسيق والمعاينة الميدانية.
        """.trimIndent()

        FileSharingHelper.sendWhatsAppMessage(context, phone, msg)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}

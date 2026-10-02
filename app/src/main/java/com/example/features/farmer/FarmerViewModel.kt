package com.example.features.farmer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.sync.MusribSyncManager
import com.example.core.sync.MusribSyncManager.MusribCloudEntry
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.LinkCodeGenerator
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FarmerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.linkedMusribDao()

    val linkedMusribs: StateFlow<List<LinkedMusrib>> = dao.getAllLinkedMusribs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMusrib = MutableStateFlow<LinkedMusrib?>(null)
    val selectedMusrib: StateFlow<LinkedMusrib?> = _selectedMusrib.asStateFlow()

    private val _ledgerEntries = MutableStateFlow<List<MusribCloudEntry>>(emptyList())
    val ledgerEntries: StateFlow<List<MusribCloudEntry>> = _ledgerEntries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    private var entriesListener: ListenerRegistration? = null

    fun dismissToast() {
        _toast.value = null
    }

    /**
     * Connects with a Musrib using a 5-character link code.
     */
    fun linkMusrib(code: String, onComplete: (Boolean) -> Unit) {
        val clean = LinkCodeGenerator.format(code)
        if (!LinkCodeGenerator.isValid(clean)) {
            _toast.value = ToastMessage("يرجى إدخال كود ربط صحيح مكون من 5 خانات", ToastType.ERROR)
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = MusribSyncManager.fetchLinkedMusrib(clean)
            _isLoading.value = false

            result.fold(
                onSuccess = { linked ->
                    dao.insert(linked)
                    _toast.value = ToastMessage("تم الربط بنجاح مع: ${linked.musribName}", ToastType.SUCCESS)
                    onComplete(true)
                },
                onFailure = { err ->
                    _toast.value = ToastMessage(err.message ?: "فشل الربط، يرجى التأكد من الكود والإنترنت", ToastType.ERROR)
                    onComplete(false)
                }
            )
        }
    }

    /**
     * Refreshes summary balance for a linked Musrib.
     */
    fun refreshMusrib(musrib: LinkedMusrib) {
        viewModelScope.launch {
            MusribSyncManager.fetchLinkedMusrib(musrib.linkCode).onSuccess { updated ->
                dao.update(updated)
            }
        }
    }

    /**
     * Removes link with a Musrib.
     */
    fun unlinkMusrib(musrib: LinkedMusrib) {
        viewModelScope.launch {
            dao.delete(musrib)
            if (_selectedMusrib.value?.linkCode == musrib.linkCode) {
                closeLedger()
            }
            _toast.value = ToastMessage("تم إلغاء ربط الحساب", ToastType.INFO)
        }
    }

    /**
     * Opens ledger and starts listening to the real-time stream of sessions and vouchers.
     */
    fun openLedger(musrib: LinkedMusrib) {
        _selectedMusrib.value = musrib
        _isLoading.value = true
        entriesListener?.remove()

        entriesListener = MusribSyncManager.listenToEntries(
            linkCode = musrib.linkCode,
            onEntries = { list ->
                _ledgerEntries.value = list.sortedByDescending { it.timestamp }
                _isLoading.value = false
                // Also update local balance if changed
                refreshMusrib(musrib)
            },
            onError = { err ->
                _isLoading.value = false
                _toast.value = ToastMessage("تعذر تحميل الحركات: ${err.message}", ToastType.ERROR)
            }
        )
    }

    fun closeLedger() {
        entriesListener?.remove()
        entriesListener = null
        _selectedMusrib.value = null
        _ledgerEntries.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        entriesListener?.remove()
    }
}

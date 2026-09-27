package com.example.features.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.util.BackupManager
import com.example.features.pumps.PumpSource
import com.example.features.pumps.PumpSourceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository(db.appSettingDao())
    private val pumpRepo = PumpSourceRepository(db.pumpSourceDao())

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    val pumps: StateFlow<List<PumpSource>> = pumpRepo.allPumps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toast = MutableStateFlow<ToastMessage?>(null)
    val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

    fun showToast(msg: String, type: ToastType = ToastType.SUCCESS) {
        _toast.value = ToastMessage(message = msg, type = type)
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun updateDistributorInfo(name: String, phone: String) {
        viewModelScope.launch {
            settingsRepo.updateDistributorName(name.trim())
            settingsRepo.updateDistributorPhone(phone.trim())
            showToast("تم تحديث بيانات المسرب / الموزع بنجاح", ToastType.SUCCESS)
        }
    }

    fun updateDefaultPricePerHour(price: Double) {
        viewModelScope.launch {
            settingsRepo.updateDefaultPricePerHour(price)
            showToast("تم تحديث سعر الساعة الافتراضي بنجاح (لا يؤثر على الجلسات السابقة)", ToastType.SUCCESS)
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            settingsRepo.updateCurrencySymbol(symbol.trim())
            showToast("تم تغيير رمز العملة بنجاح", ToastType.SUCCESS)
        }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepo.updateThemeMode(mode)
            showToast("تم تغيير نمط المظهر", ToastType.INFO)
        }
    }

    fun savePump(
        id: Long = 0,
        name: String,
        location: String,
        pricePerHour: Double,
        powerType: String,
        notes: String
    ) {
        viewModelScope.launch {
            val pump = PumpSource(
                id = id,
                name = name.trim(),
                locationOrWellNumber = location.trim(),
                defaultPricePerHour = pricePerHour,
                powerType = powerType,
                notes = notes.trim(),
                isPrimary = id == 1L
            )
            if (id == 0L) {
                pumpRepo.insertPump(pump)
                showToast("تمت إضافة المضخة بنجاح", ToastType.SUCCESS)
            } else {
                pumpRepo.updatePump(pump)
                showToast("تم تعديل بيانات المضخة", ToastType.SUCCESS)
            }
        }
    }

    fun deletePump(pump: PumpSource) {
        viewModelScope.launch {
            pumpRepo.deletePump(pump)
            showToast("تم حذف المضخة", ToastType.INFO)
        }
    }

    private val _savedBackups = MutableStateFlow<List<BackupManager.BackupFileInfo>>(emptyList())
    val savedBackups: StateFlow<List<BackupManager.BackupFileInfo>> = _savedBackups.asStateFlow()

    init {
        loadBackups()
    }

    fun loadBackups() {
        viewModelScope.launch(Dispatchers.IO) {
            _savedBackups.value = BackupManager.getAvailableBackups(getApplication())
        }
    }

    /**
     * Backup to file and launch Drive sharing intent
     */
    fun createBackupAndShare() {
        viewModelScope.launch {
            try {
                val backupFile: File = BackupManager.createBackupJson(getApplication(), db)
                BackupManager.shareBackupToDriveOrApps(getApplication(), backupFile)
                loadBackups()
                showToast("تم إنشاء النسخة الاحتياطية بنجاح ومشاركتها", ToastType.SUCCESS)
            } catch (e: Exception) {
                showToast("فشل في إنشاء النسخة الاحتياطية: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    /**
     * Save backup directly to phone's Downloads directory and local backups
     */
    fun backupToPhoneStorage() {
        viewModelScope.launch {
            val result = BackupManager.saveBackupToPhoneDownloads(getApplication(), db)
            result.onSuccess { file ->
                loadBackups()
                showToast("تم حفظ النسخة بنجاح في مجلد التنزيلات بالهاتف", ToastType.SUCCESS)
            }.onFailure { e ->
                showToast("فشل في حفظ النسخة بالهاتف: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    /**
     * Restore from user-selected JSON file URI
     */
    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            val result = BackupManager.restoreFromJson(getApplication(), db, uri)
            result.onSuccess { count ->
                loadBackups()
                showToast("تم استعادة $count سجلاً بنجاح من النسخة الاحتياطية", ToastType.SUCCESS)
            }.onFailure { e ->
                showToast("فشل في استعادة البيانات: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    /**
     * Restore directly from an internal saved backup file
     */
    fun restoreBackupFromFile(file: File) {
        viewModelScope.launch {
            val result = BackupManager.restoreFromFile(getApplication(), db, file)
            result.onSuccess { count ->
                loadBackups()
                showToast("تم استعادة $count سجلاً بنجاح من النسخة الاحتياطية", ToastType.SUCCESS)
            }.onFailure { e ->
                showToast("فشل في استعادة البيانات: ${e.localizedMessage}", ToastType.ERROR)
            }
        }
    }

    fun deleteBackupFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            BackupManager.deleteBackup(file)
            loadBackups()
        }
        showToast("تم حذف النسخة الاحتياطية", ToastType.INFO)
    }

    fun shareExistingBackup(file: File) {
        BackupManager.shareBackupToDriveOrApps(getApplication(), file)
    }
}

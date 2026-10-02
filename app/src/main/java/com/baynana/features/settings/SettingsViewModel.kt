package com.baynana.features.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baynana.core.database.AppDatabase
import com.baynana.core.ui.ToastMessage
import com.baynana.core.ui.ToastType
import com.baynana.core.license.LicenseManager
import com.baynana.core.security.BiometricHelper
import com.baynana.core.util.BackupManager
import com.baynana.core.util.GoogleDriveBackupHelper
import com.baynana.features.pumps.PumpSource
import com.baynana.features.pumps.PumpSourceRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsRepo = SettingsRepository(db.appSettingDao())
    private val pumpRepo = PumpSourceRepository(db.pumpSourceDao())

    val appConfig: StateFlow<AppConfig> = settingsRepo.appConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppConfig())

    // null means the persisted authentication policy has not loaded yet.
    val biometricLockEnabled: StateFlow<Boolean?> = settingsRepo.appConfig
        .map { it.biometricEnabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pumps: StateFlow<List<PumpSource>> = pumpRepo.allPumps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val operationsCount: StateFlow<Int> = combine(
        db.waterSessionDao().getSessionsCount(),
        db.voucherDao().getVouchersCount()
    ) { sessions, vouchers ->
        sessions + vouchers
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isActivated = MutableStateFlow(LicenseManager.isActivated(application))
    val isActivated: StateFlow<Boolean> = _isActivated.asStateFlow()

    fun refreshActivationStatus() {
        _isActivated.value = LicenseManager.isActivated(getApplication())
    }

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

    fun updateBiometricEnabled(enabled: Boolean) {
        if (enabled && !BiometricHelper.isAvailable(getApplication())) {
            showToast("سجّل بصمة في إعدادات الهاتف أولاً؛ لم يتم تفعيل القفل", ToastType.ERROR)
            return
        }
        viewModelScope.launch {
            settingsRepo.updateBiometricEnabled(enabled)
            showToast(if (enabled) "تم تفعيل القفل بالبصمة بنجاح" else "تم إلغاء قفل البصمة", ToastType.INFO)
        }
    }

    fun updateRoles(primaryRole: String, activeRoles: String) {
        viewModelScope.launch {
            settingsRepo.updateRoles(primaryRole, activeRoles)
            showToast("تم تحديث أدواري في المنظومة بنجاح 🌾", ToastType.SUCCESS)
        }
    }

    fun completeOnboarding(
        name: String,
        phone: String,
        village: String,
        primaryRole: String,
        activeRoles: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            settingsRepo.completeOnboarding(name, phone, village, primaryRole, activeRoles)
            showToast("مرحباً بك في منظومة بيننا! 🌾", ToastType.SUCCESS)
            onDone()
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
                showToast("تمت إضافة مصدر الماء بنجاح", ToastType.SUCCESS)
            } else {
                pumpRepo.updatePump(pump)
                showToast("تم تعديل بيانات مصدر الماء", ToastType.SUCCESS)
            }
        }
    }

    fun deletePump(pump: PumpSource) {
        viewModelScope.launch {
            pumpRepo.deletePump(pump)
            showToast("تم حذف مصدر الماء", ToastType.INFO)
        }
    }

    private val _savedBackups = MutableStateFlow<List<BackupManager.BackupFileInfo>>(emptyList())
    val savedBackups: StateFlow<List<BackupManager.BackupFileInfo>> = _savedBackups.asStateFlow()

    private val _googleAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val googleAccount: StateFlow<GoogleSignInAccount?> = _googleAccount.asStateFlow()

    private val _driveBackups = MutableStateFlow<List<GoogleDriveBackupHelper.DriveBackupFile>>(emptyList())
    val driveBackups: StateFlow<List<GoogleDriveBackupHelper.DriveBackupFile>> = _driveBackups.asStateFlow()

    private val _isDriveLoading = MutableStateFlow(false)
    val isDriveLoading: StateFlow<Boolean> = _isDriveLoading.asStateFlow()

    init {
        loadBackups()
        checkGoogleAccount()
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
     * Backup and send directly to Google Drive app
     */
    fun createBackupAndSendDirectToDrive() {
        viewModelScope.launch {
            try {
                val backupFile: File = BackupManager.createBackupJson(getApplication(), db)
                BackupManager.shareBackupDirectToDrive(getApplication(), backupFile)
                loadBackups()
                showToast("تم إنشاء النسخة وتوجيهها مباشرة إلى Google Drive", ToastType.SUCCESS)
            } catch (e: Exception) {
                showToast("فشل في إرسال النسخة للدرايف: ${e.localizedMessage}", ToastType.ERROR)
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

    fun checkGoogleAccount() {
        val account = GoogleDriveBackupHelper.getSignedInAccount(getApplication())
        _googleAccount.value = account
        if (account != null) {
            loadDriveBackups()
        }
    }

    fun onGoogleSignInSuccess(account: GoogleSignInAccount) {
        _googleAccount.value = account
        showToast("تم الاتصال بحساب Google: ${account.email}", ToastType.SUCCESS)
        loadDriveBackups()
    }

    fun onGoogleSignOut() {
        val client = GoogleDriveBackupHelper.getGoogleSignInClient(getApplication())
        client.signOut().addOnCompleteListener {
            _googleAccount.value = null
            _driveBackups.value = emptyList<GoogleDriveBackupHelper.DriveBackupFile>()
            showToast("تم قطع الاتصال بحساب Google", ToastType.INFO)
        }
    }

    fun loadDriveBackups() {
        val account = _googleAccount.value ?: return
        viewModelScope.launch {
            _isDriveLoading.value = true
            val result = GoogleDriveBackupHelper.listAppDataBackups(getApplication(), account)
            result.onSuccess { list ->
                _driveBackups.value = list
            }.onFailure { e ->
                showToast("تعذر جلب النسخ من Google Drive: ${e.localizedMessage}", ToastType.ERROR)
            }
            _isDriveLoading.value = false
        }
    }

    fun backupToGoogleDriveAppData() {
        val account = _googleAccount.value
        if (account == null) {
            showToast("يرجى تسجيل الدخول بحساب Google أولاً", ToastType.WARNING)
            return
        }
        viewModelScope.launch {
            _isDriveLoading.value = true
            try {
                val backupFile = BackupManager.createBackupJson(getApplication(), db)
                val result = GoogleDriveBackupHelper.uploadBackupToAppData(getApplication(), account, backupFile)
                result.onSuccess {
                    loadBackups()
                    loadDriveBackups()
                    showToast("تم رفع النسخة (.back) بنجاح إلى مجلد Google Drive السحابي المحمي", ToastType.SUCCESS)
                }.onFailure { e ->
                    showToast("فشل رفع النسخة إلى Drive: ${e.localizedMessage}", ToastType.ERROR)
                }
            } catch (e: Exception) {
                showToast("حدث خطأ أثناء إعداد النسخة: ${e.localizedMessage}", ToastType.ERROR)
            } finally {
                _isDriveLoading.value = false
            }
        }
    }

    fun restoreFromGoogleDriveAppData(driveFile: GoogleDriveBackupHelper.DriveBackupFile) {
        val account = _googleAccount.value
        if (account == null) {
            showToast("يرجى تسجيل الدخول بحساب Google أولاً", ToastType.WARNING)
            return
        }
        viewModelScope.launch {
            _isDriveLoading.value = true
            var temporaryFile: File? = null
            try {
                // Cloud names are untrusted metadata, never local filesystem paths.
                val tempFile = File.createTempFile("drive_restore_", ".back", getApplication<Application>().cacheDir)
                temporaryFile = tempFile
                val dlResult = GoogleDriveBackupHelper.downloadAppDataBackup(getApplication(), account, driveFile.id, tempFile)
                dlResult.onSuccess { file ->
                    val restoreResult = BackupManager.restoreFromFile(getApplication(), db, file)
                    restoreResult.onSuccess { count ->
                        loadBackups()
                        showToast("تم استعادة $count سجلاً بنجاح من نسخة Google Drive السحابية", ToastType.SUCCESS)
                    }.onFailure { e ->
                        showToast("فشل في تطبيق النسخة المستعادة: ${e.localizedMessage}", ToastType.ERROR)
                    }
                }.onFailure { e ->
                    showToast("فشل تنزيل النسخة من Google Drive: ${e.localizedMessage}", ToastType.ERROR)
                }
            } catch (e: Exception) {
                showToast("حدث خطأ أثناء الاستعادة من Drive: ${e.localizedMessage}", ToastType.ERROR)
            } finally {
                temporaryFile?.delete()
                _isDriveLoading.value = false
            }
        }
    }

    fun deleteDriveBackup(driveFile: GoogleDriveBackupHelper.DriveBackupFile) {
        val account = _googleAccount.value ?: return
        viewModelScope.launch {
            _isDriveLoading.value = true
            val result = GoogleDriveBackupHelper.deleteAppDataBackup(getApplication(), account, driveFile.id)
            result.onSuccess {
                loadDriveBackups()
                showToast("تم حذف النسخة السحابية من Google Drive", ToastType.INFO)
            }.onFailure { e ->
                showToast("فشل حذف النسخة السحابية: ${e.localizedMessage}", ToastType.ERROR)
            }
            _isDriveLoading.value = false
        }
    }
}

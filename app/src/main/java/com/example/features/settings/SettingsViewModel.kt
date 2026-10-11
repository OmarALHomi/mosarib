package com.example.features.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.ui.ToastMessage
import com.example.core.ui.ToastType
import com.example.core.license.LicenseManager
import com.example.core.util.BackupManager
import com.example.core.util.GoogleDriveBackupHelper
import com.example.features.pumps.PumpSource
import com.example.features.pumps.PumpSourceRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    val operationsCount: StateFlow<Int> = combine(
        db.waterSessionDao().getSessionsCount(),
        db.voucherDao().getVouchersCount(),
        db.wellOwnerPurchaseDao().getPurchasesCount()
    ) { sessions, vouchers, purchases ->
        sessions + vouchers + purchases
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
        viewModelScope.launch {
            settingsRepo.updateBiometricEnabled(enabled)
            showToast(if (enabled) "تم تفعيل القفل بالبصمة بنجاح" else "تم إلغاء قفل البصمة", ToastType.INFO)
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

    private val _driveRecoveryIntent = MutableStateFlow<Intent?>(null)
    val driveRecoveryIntent: StateFlow<Intent?> = _driveRecoveryIntent.asStateFlow()

    private var pendingDriveAction: (() -> Unit)? = null

    fun clearDriveRecoveryIntent() {
        _driveRecoveryIntent.value = null
    }

    fun onDriveConsentGranted() {
        _driveRecoveryIntent.value = null
        pendingDriveAction?.invoke()
        pendingDriveAction = null
    }

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
     * Export backup directly to user-selected SAF Uri
     */
    fun exportBackupToUri(uri: Uri) {
        viewModelScope.launch {
            val result = BackupManager.exportBackupToUri(getApplication(), db, uri)
            result.onSuccess {
                loadBackups()
                showToast("تم حفظ وتصدير النسخة الاحتياطية بنجاح في المكان المختار", ToastType.SUCCESS)
            }.onFailure { e ->
                showToast("فشل في تصدير النسخة: ${e.localizedMessage}", ToastType.ERROR)
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

    fun cleanOldLocalBackups(keepCount: Int = 3) {
        viewModelScope.launch(Dispatchers.IO) {
            val backups = BackupManager.getAvailableBackups(getApplication())
            if (backups.size > keepCount) {
                val toDelete = backups.drop(keepCount)
                toDelete.forEach { BackupManager.deleteBackup(it.file) }
                loadBackups()
                showToast("تم تنظيف ${toDelete.size} نسخة قديمة والاحتفاظ بأحدث $keepCount نسخ", ToastType.SUCCESS)
            } else {
                showToast("النسخ الحالية (${backups.size}) ضمن الحد المسموح ($keepCount نسخ)", ToastType.INFO)
            }
        }
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
                if (e is GoogleDriveBackupHelper.DriveUserRecoverableException) {
                    pendingDriveAction = { loadDriveBackups() }
                    _driveRecoveryIntent.value = e.recoveryIntent
                } else {
                    showToast("تعذر جلب النسخ من Google Drive: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
                }
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
                    if (e is GoogleDriveBackupHelper.DriveUserRecoverableException) {
                        pendingDriveAction = { backupToGoogleDriveAppData() }
                        _driveRecoveryIntent.value = e.recoveryIntent
                    } else {
                        showToast("فشل رفع النسخة إلى Drive: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
                    }
                }
            } catch (e: Exception) {
                if (e is GoogleDriveBackupHelper.DriveUserRecoverableException) {
                    pendingDriveAction = { backupToGoogleDriveAppData() }
                    _driveRecoveryIntent.value = e.recoveryIntent
                } else {
                    showToast("حدث خطأ أثناء إعداد النسخة: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
                }
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
            try {
                val tempFile = File(getApplication<Application>().cacheDir, driveFile.name)
                val dlResult = GoogleDriveBackupHelper.downloadAppDataBackup(getApplication(), account, driveFile.id, tempFile)
                dlResult.onSuccess { file ->
                    val restoreResult = BackupManager.restoreFromFile(getApplication(), db, file)
                    restoreResult.onSuccess { count ->
                        loadBackups()
                        showToast("تم استعادة $count سجلاً بنجاح من نسخة Google Drive السحابية", ToastType.SUCCESS)
                    }.onFailure { e ->
                        showToast("فشل في تطبيق النسخة المستعادة: ${e.localizedMessage}", ToastType.ERROR)
                    }
                    file.delete()
                }.onFailure { e ->
                    if (e is GoogleDriveBackupHelper.DriveUserRecoverableException) {
                        pendingDriveAction = { restoreFromGoogleDriveAppData(driveFile) }
                        _driveRecoveryIntent.value = e.recoveryIntent
                    } else {
                        showToast("فشل تنزيل النسخة من Google Drive: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
                    }
                }
            } catch (e: Exception) {
                if (e is GoogleDriveBackupHelper.DriveUserRecoverableException) {
                    pendingDriveAction = { restoreFromGoogleDriveAppData(driveFile) }
                    _driveRecoveryIntent.value = e.recoveryIntent
                } else {
                    showToast("حدث خطأ أثناء الاستعادة من Drive: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
                }
            } finally {
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
                showToast("فشل حذف النسخة السحابية: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
            }
            _isDriveLoading.value = false
        }
    }
}

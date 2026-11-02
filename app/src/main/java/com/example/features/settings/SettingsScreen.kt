package com.example.features.settings

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.features.customers.CustomersViewModel
import com.example.features.customers.ManageGroupsDialog
import com.example.features.customers.RecycleBinDialog
import com.example.features.customers.CustomerGroupEditorDialog

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.license.RemoteConfigManager
import com.example.core.license.UserDeviceTracker
import com.example.core.security.BiometricHelper
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.ToastType
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.core.util.GoogleDriveBackupHelper
import com.example.features.feedback.FeedbackDialog
import com.example.features.home.HomeShowcaseTourDialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val customersViewModel: CustomersViewModel = viewModel()
    val archivedCustomers by customersViewModel.archivedCustomers.collectAsStateWithLifecycle()
    val customGroups by customersViewModel.customGroups.collectAsStateWithLifecycle()
    val rawCustomers by customersViewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    var showManageGroupsDialog by remember { mutableStateOf(false) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<String?>(null) }
    var initialMemberIdsForEdit by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val isActivated by viewModel.isActivated.collectAsStateWithLifecycle()
    val isSeedingData by viewModel.isSeedingData.collectAsStateWithLifecycle()

    var showConfirmSeedDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    var distributorName by remember(config.distributorName) { mutableStateOf(config.distributorName) }
    var distributorPhone by remember(config.distributorPhone) { mutableStateOf(config.distributorPhone) }
    var defaultPriceStr by remember(config.defaultPricePerHour) {
        mutableStateOf(Formatters.formatAmountInput(config.defaultPricePerHour.toString()))
    }
    var currencySymbol by remember(config.currencySymbol) { mutableStateOf(config.currencySymbol) }

    // Backup tabs, pending restore/delete confirmations and Google sign-in flags
    val backupState = remember { SettingsBackupUiState() }
    var showTourDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshRemotePolicies()
    }

    // File Picker Launcher for JSON Database Restore
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { backupState.uriToRestore = it }
    }

    // SAF Document Creator for saving backup anywhere on device
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackupToUri(it) }
    }

    val context = LocalContext.current
    val isDark = isAppDark()
    val isBiometricAvailable = remember { BiometricHelper.isAvailable(context) }
    val deviceCode = remember { LicenseManager.getDeviceCode(context) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    val savedBackups by viewModel.savedBackups.collectAsStateWithLifecycle()
    val autoBackups by viewModel.autoBackups.collectAsStateWithLifecycle()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsStateWithLifecycle()

    val googleAccount by viewModel.googleAccount.collectAsStateWithLifecycle()
    val driveBackups by viewModel.driveBackups.collectAsStateWithLifecycle()
    val isDriveLoading by viewModel.isDriveLoading.collectAsStateWithLifecycle()

    val targetBackupSourceTab by viewModel.targetBackupSourceTab.collectAsStateWithLifecycle()
    LaunchedEffect(targetBackupSourceTab) {
        backupState.selectedBackupSourceTab = targetBackupSourceTab
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                viewModel.onGoogleSignInSuccess(account)
                if (backupState.pendingAutoBackupAfterSignIn) {
                    backupState.pendingAutoBackupAfterSignIn = false
                    viewModel.backupToGoogleDriveAppData()
                }
            }
        } catch (e: Exception) {
            backupState.pendingAutoBackupAfterSignIn = false
            viewModel.showToast("فشل تسجيل الدخول بحساب Google: ${GoogleDriveBackupHelper.getReadableErrorMessage(e)}", ToastType.ERROR)
        }
    }

    val driveRecoveryIntent by viewModel.driveRecoveryIntent.collectAsStateWithLifecycle()
    val driveRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDriveConsentGranted()
        } else {
            viewModel.clearDriveRecoveryIntent()
        }
    }

    LaunchedEffect(driveRecoveryIntent) {
        driveRecoveryIntent?.let {
            driveRecoveryLauncher.launch(it)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            item {
                WaterPriceSection(
                    defaultPriceStr = defaultPriceStr,
                    onDefaultPriceStrChange = { defaultPriceStr = it },
                    currencySymbol = currencySymbol,
                    onSavePrice = { viewModel.updateDefaultPricePerHour(it) }
                )
            }

            item {
                DistributorProfileSection(
                    distributorName = distributorName,
                    onDistributorNameChange = { distributorName = it },
                    distributorPhone = distributorPhone,
                    onDistributorPhoneChange = { distributorPhone = it },
                    currencySymbol = currencySymbol,
                    onCurrencySymbolChange = {
                        currencySymbol = it
                        viewModel.updateCurrencySymbol(it)
                    },
                    onSave = { viewModel.updateDistributorInfo(distributorName, distributorPhone) }
                )
            }

            item {
                ThemeModeSection(
                    themeMode = config.themeMode,
                    onThemeModeSelected = { viewModel.updateThemeMode(it) }
                )
            }

            item {
                AppLockSection(
                    isBiometricAvailable = isBiometricAvailable,
                    biometricEnabled = config.biometricEnabled,
                    onBiometricEnabledChange = { viewModel.updateBiometricEnabled(it) }
                )
            }

            item {
                val isBackupAllowed by viewModel.isBackupAllowed.collectAsStateWithLifecycle()
                val isDeviceBkLocked by viewModel.isDeviceBackupLocked.collectAsStateWithLifecycle()
                val isCloudBackupAllowed = isBackupAllowed && RemoteConfigManager.isCloudBackupAllowed(context)

                BackupAndRestoreSection(
                    state = backupState,
                    isDark = isDark,
                    isBackupAllowed = isBackupAllowed,
                    isDeviceBackupLocked = isDeviceBkLocked,
                    isCloudBackupAllowed = isCloudBackupAllowed,
                    isAutoBackupEnabled = isAutoBackupEnabled,
                    autoBackups = autoBackups,
                    savedBackups = savedBackups,
                    googleAccount = googleAccount,
                    driveBackups = driveBackups,
                    isDriveLoading = isDriveLoading,
                    onCreateBackup = {
                        val dateStr = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                        createBackupLauncher.launch("mosarib_backup_$dateStr.back")
                    },
                    onImportBackup = { restoreFileLauncher.launch(arrayOf("*/*", "application/octet-stream", "application/json")) },
                    onShareNewBackup = { viewModel.createBackupAndShare() },
                    onAutoBackupEnabledChange = { viewModel.toggleAutoBackup(it) },
                    onRefreshBackups = { viewModel.loadBackups() },
                    onShareBackupFile = { viewModel.shareExistingBackup(it) },
                    onGoogleSignOut = { viewModel.onGoogleSignOut() },
                    onRefreshDriveBackups = { viewModel.loadDriveBackups() }
                )
            }

            item {
                DataManagementSection(
                    archivedCount = archivedCustomers.size,
                    onOpenRecycleBin = { showRecycleBinDialog = true },
                    onOpenManageGroups = { showManageGroupsDialog = true }
                )
            }

            item {
                LicenseSection(
                    isActivated = isActivated,
                    operationsCount = operationsCount,
                    deviceCode = deviceCode,
                    onOpenLicenseDialog = { showLicenseDialog = true }
                )
            }

            supportSectionItems(
                onOpenTour = { showTourDialog = true },
                onOpenFeedback = { showFeedbackDialog = true },
                onNavigateToAbout = onNavigateToAbout
            )

            if (BuildConfig.DEBUG) {
                item {
                    DebugToolsSection(
                        isSeedingData = isSeedingData,
                        onSeedClick = { showConfirmSeedDialog = true },
                        onClearClick = { showConfirmClearDialog = true }
                    )
                }
            }
        }

        if (showConfirmSeedDialog) {
            ConfirmSeedDebugDataDialog(
                onConfirm = {
                    showConfirmSeedDialog = false
                    viewModel.seedDebugData()
                },
                onDismiss = { showConfirmSeedDialog = false }
            )
        }

        if (showConfirmClearDialog) {
            ConfirmClearDebugDataDialog(
                onConfirm = {
                    showConfirmClearDialog = false
                    viewModel.clearAllDebugData()
                },
                onDismiss = { showConfirmClearDialog = false }
            )
        }

        if (showFeedbackDialog) {
            FeedbackDialog(onDismiss = { showFeedbackDialog = false })
        }

        BackupDialogsHost(
            state = backupState,
            onRestoreBackupFile = { viewModel.restoreBackupFromFile(it) },
            onDeleteBackupFile = { viewModel.deleteBackupFile(it) },
            onRestoreDriveBackup = { viewModel.restoreFromGoogleDriveAppData(it) },
            onRestoreImportedFile = { viewModel.restoreBackup(it) },
            onCleanOldBackups = { viewModel.cleanOldLocalBackups(3) },
            onDeleteDriveBackup = { viewModel.deleteDriveBackup(it) }
        )

        // Floating Toast Notification
        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // License Dialog
        val isTrialLimitExceeded = LicenseManager.isTrialLimitExceeded(context, operationsCount)

        if (showLicenseDialog && !isTrialLimitExceeded) {
            LicenseDialog(
                onDismiss = { showLicenseDialog = false },
                onActivated = {
                    viewModel.refreshActivationStatus()
                    showLicenseDialog = false
                },
                isMandatory = false
            )
        }

        // Showcase Tour Dialog
        if (showTourDialog) {
            HomeShowcaseTourDialog(
                onDismiss = { showTourDialog = false }
            )
        }

        if (backupState.showGoogleBackupAccountDialog) {
            DriveBackupAccountDialog(
                context = context,
                distributorName = distributorName,
                distributorPhone = distributorPhone,
                googleAccount = googleAccount,
                onDismiss = {
                    backupState.showGoogleBackupAccountDialog = false
                    backupState.pendingAutoBackupAfterSignIn = false
                },
                onProceed = { name, phone, shouldLaunchSignIn ->
                    backupState.showGoogleBackupAccountDialog = false
                    UserDeviceTracker.saveUserRegistrationInfo(context, name, phone)
                    viewModel.updateDistributorInfo(name, phone)

                    if (shouldLaunchSignIn) {
                        val signInIntent = GoogleDriveBackupHelper.getGoogleSignInClient(context).signInIntent
                        googleSignInLauncher.launch(signInIntent)
                    } else {
                        viewModel.backupToGoogleDriveAppData()
                    }
                }
            )
        }
 
        if (showManageGroupsDialog) {
            ManageGroupsDialog(
                groups = customGroups,
                allCustomers = rawCustomers,
                onDismiss = { showManageGroupsDialog = false },
                onEditGroup = { grp, memberIds ->
                    showManageGroupsDialog = false
                    groupToEdit = grp
                    initialMemberIdsForEdit = memberIds
                },
                onRenameGroup = { oldName, newName ->
                    customersViewModel.renameGroup(oldName, newName)
                },
                onDeleteGroup = { grp ->
                    customersViewModel.deleteGroup(grp)
                },
                onAddNewGroup = {
                    showManageGroupsDialog = false
                    showAddGroupDialog = true
                }
            )
        }

        if (showAddGroupDialog || groupToEdit != null) {
            CustomerGroupEditorDialog(
                groupToEdit = groupToEdit,
                initialMemberIdsForEdit = initialMemberIdsForEdit,
                rawCustomers = rawCustomers,
                onDismiss = {
                    showAddGroupDialog = false
                    groupToEdit = null
                    initialMemberIdsForEdit = emptySet()
                },
                onSave = { name, selectedIds ->
                    customersViewModel.saveGroup(name, selectedIds, groupToEdit)
                    showAddGroupDialog = false
                    groupToEdit = null
                    initialMemberIdsForEdit = emptySet()
                }
            )
        }

        if (showRecycleBinDialog) {
            RecycleBinDialog(
                archivedCustomers = archivedCustomers,
                onRestore = { customersViewModel.restoreCustomer(it.id) },
                onPermanentlyDelete = { customersViewModel.permanentlyDeleteCustomer(it.id) },
                onEmptyBin = { customersViewModel.emptyRecycleBin() },
                onDismiss = { showRecycleBinDialog = false }
            )
        }
    }
}
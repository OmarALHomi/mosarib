package com.example.core.widget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.database.AppDatabase
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.license.UserDeviceTracker
import com.example.core.security.BiometricLockScreen
import com.example.core.ui.DeviceBlockedScreen
import com.example.core.security.DeviceLockManager
import com.example.core.security.DeviceUnauthorizedScreen
import com.example.core.ui.LocalOpenSubscription
import com.example.core.ui.LocalOperationGate
import com.example.core.ui.LocalSimpleMode
import com.example.core.ui.OperationGate
import com.example.core.ui.PostSaveMessageHost
import com.example.core.ui.PostSaveMessages
import com.example.core.util.Formatters
import com.example.core.voice.CustomerMatcher
import com.example.core.voice.VoiceAliasRepository
import com.example.core.voice.VoiceCommand
import com.example.core.voice.VoiceCommandType
import com.example.core.voice.VoiceCustomerChooserDialog
import com.example.core.voice.VoiceDialogType
import com.example.core.voice.VoiceHintSheet
import com.example.core.voice.VoiceRecordSheet
import com.example.core.voice.VoiceRecognitionHelper
import com.example.core.voice.VoiceSupportDialogs
import com.example.core.voice.VoiceUnparsedLog
import com.example.core.voice.VoiceUnrecognizedDialog
import com.example.core.voice.VoiceVoucherTypeChooserDialog
import com.example.core.voice.VoiceWellOwnerActionChooserDialog
import com.example.core.voice.buildVoiceCommandSummary
import com.example.features.customers.Customer
import com.example.features.customers.CustomersViewModel
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.SessionsViewModel
import com.example.features.settings.AppConfig
import com.example.features.settings.SettingsRepository
import com.example.features.settings.SettingsViewModel
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersViewModel
import com.example.features.wellowners.WellOwnerPurchaseBottomSheet
import com.example.ui.theme.MosaribTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Lightweight translucent host for Home-Screen widget quick recording.
 * Opens directly over the desktop on the requested form without launching MainApp,
 * while still enforcing biometric lock, block status, and operation limits.
 */
class QuickRecordActivity : ComponentActivity() {

    private var requestedMode by mutableStateOf(MODE_SESSION)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedMode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_SESSION

        if (!com.example.core.license.RemoteConfigManager.isHomeWidgetAllowed(this)) {
            android.widget.Toast.makeText(
                this,
                com.example.core.license.RemoteConfigManager.FEATURE_DISABLED_MESSAGE,
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }
        if (!DeviceLockManager.verifyOrRegister(this)) {
            setContent { DeviceUnauthorizedScreen() }
            return
        }

        setContent {
            QuickRecordHost(
                mode = requestedMode,
                onFinished = {
                    MosaribQuickWidgetProvider.requestWidgetRefresh(applicationContext)
                    finish()
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!com.example.core.license.RemoteConfigManager.isHomeWidgetAllowed(this)) {
            android.widget.Toast.makeText(
                this,
                com.example.core.license.RemoteConfigManager.FEATURE_DISABLED_MESSAGE,
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }
        requestedMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SESSION
    }

    companion object {
        const val EXTRA_MODE = "extra_quick_record_mode"
        const val MODE_SESSION = "SESSION"
        const val MODE_VOICE = "VOICE"
        const val MODE_RECEIPT = "RECEIPT"
        const val MODE_EXPENSE = "EXPENSE"
        const val MODE_PURCHASE = "PURCHASE"
    }
}

@Composable
private fun QuickRecordHost(
    mode: String,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val initialConfig by produceState<AppConfig?>(initialValue = null) {
        val db = AppDatabase.getDatabase(context)
        value = SettingsRepository(db.appSettingDao()).appConfig.first()
    }
    val readyConfig = initialConfig ?: return

    val sessionsViewModel: SessionsViewModel = viewModel()
    val customersViewModel: CustomersViewModel = viewModel()
    val vouchersViewModel: VouchersViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val appConfig by settingsViewModel.appConfig.collectAsStateWithLifecycle(initialValue = readyConfig)
    val operationsCount by settingsViewModel.operationsCount.collectAsStateWithLifecycle()
    val customers by sessionsViewModel.customers.collectAsStateWithLifecycle()
    val allSessions by vouchersViewModel.allSessions.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (appConfig.themeMode.uppercase(Locale.US)) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    var isBiometricUnlocked by remember { mutableStateOf(!appConfig.biometricEnabled) }
    val isBlocked = remember { UserDeviceTracker.isDeviceBlockedLocally(context) }

    var showLicenseDialog by remember { mutableStateOf(false) }
    val operationGate: OperationGate = { action ->
        if (LicenseManager.canPerformOperation(context, operationsCount)) action() else showLicenseDialog = true
    }

    MosaribTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalSimpleMode provides appConfig.simpleMode,
            LocalOperationGate provides operationGate,
            LocalOpenSubscription provides { showLicenseDialog = true }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.01f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { }
                    )
            ) {
                when {
                    isBlocked -> {
                        DeviceBlockedScreen(reason = UserDeviceTracker.getBlockReason(context))
                    }
                    appConfig.biometricEnabled && !isBiometricUnlocked -> {
                        BiometricLockScreen(onUnlock = { isBiometricUnlocked = true })
                    }
                    else -> {
                        QuickRecordSheetRouter(
                            mode = mode,
                            config = appConfig,
                            operationsCount = operationsCount,
                            customers = customers,
                            allSessions = allSessions,
                            sessionsViewModel = sessionsViewModel,
                            customersViewModel = customersViewModel,
                            vouchersViewModel = vouchersViewModel,
                            onShowLicense = { showLicenseDialog = true },
                            onFinished = onFinished
                        )
                    }
                }

                if (showLicenseDialog) {
                    LicenseDialog(
                        onDismiss = {
                            showLicenseDialog = false
                            if (!LicenseManager.canPerformOperation(context, operationsCount)) {
                                onFinished()
                            }
                        },
                        onActivated = {
                            settingsViewModel.refreshActivationStatus()
                            showLicenseDialog = false
                        },
                        isMandatory = false,
                        mandatoryReason = "بلغت الحد الأقصى للعمليات التجريبية المجانية (${LicenseManager.getFreeOperationsLimit(context)} عملية). يرجى تفعيل الاشتراك لإضافة عمليات جديدة."
                    )
                }

                PostSaveMessageHost()
            }
        }
    }
}

@Composable
private fun QuickRecordSheetRouter(
    mode: String,
    config: AppConfig,
    operationsCount: Int,
    customers: List<Customer>,
    allSessions: List<com.example.features.sessions.WaterSession>,
    sessionsViewModel: SessionsViewModel,
    customersViewModel: CustomersViewModel,
    vouchersViewModel: VouchersViewModel,
    onShowLicense: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val postSaveMsg by PostSaveMessages.pending.collectAsState()
    val sessionsToast by sessionsViewModel.toast.collectAsStateWithLifecycle()
    val vouchersToast by vouchersViewModel.toast.collectAsStateWithLifecycle()
    val customersToast by customersViewModel.toast.collectAsStateWithLifecycle()

    var awaitingSaveCompletion by remember { mutableStateOf(false) }

    var showSessionSheet by remember(mode) { mutableStateOf(mode == QuickRecordActivity.MODE_SESSION) }
    var showVoiceSheet by remember(mode) { mutableStateOf(mode == QuickRecordActivity.MODE_VOICE) }
    var showVoucherSheet by remember(mode) {
        mutableStateOf(mode == QuickRecordActivity.MODE_RECEIPT || mode == QuickRecordActivity.MODE_EXPENSE)
    }
    var voucherType by remember(mode) {
        mutableStateOf(if (mode == QuickRecordActivity.MODE_EXPENSE) VoucherType.EXPENSE else VoucherType.RECEIPT)
    }
    var showPurchaseSheet by remember(mode) { mutableStateOf(mode == QuickRecordActivity.MODE_PURCHASE) }
    var activeVoiceCommand by remember { mutableStateOf<VoiceCommand?>(null) }

    var showVoiceHintSheet by remember { mutableStateOf(false) }
    var voiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }
    var ambiguousVoiceCommand by remember { mutableStateOf<VoiceCommand?>(null) }
    var ambiguousCandidates by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var wellOwnerActionCommand by remember { mutableStateOf<Pair<VoiceCommand, Customer>?>(null) }
    var voucherTypeChooserCommand by remember { mutableStateOf<VoiceCommand?>(null) }
    var unrecognizedSpokenText by remember { mutableStateOf<String?>(null) }

    val activeToast = sessionsToast ?: vouchersToast ?: customersToast
    LaunchedEffect(activeToast, postSaveMsg, awaitingSaveCompletion) {
        val toastMsg = activeToast
        if (toastMsg != null) {
            Toast.makeText(context, toastMsg.message, Toast.LENGTH_SHORT).show()
            if (sessionsToast != null) sessionsViewModel.dismissToast()
            if (vouchersToast != null) vouchersViewModel.dismissToast()
            if (customersToast != null) customersViewModel.dismissToast()
            MosaribQuickWidgetProvider.requestWidgetRefresh(context)
            scope.launch {
                val exact = com.example.core.util.AppOperationsCounter.getExactTotalOperations(context)
                UserDeviceTracker.syncOperationsSnapshot(context, exact)
            }
            if (awaitingSaveCompletion && postSaveMsg == null) {
                delay(150)
                if (PostSaveMessages.pending.value == null) onFinished()
            }
        }
    }

    LaunchedEffect(postSaveMsg, awaitingSaveCompletion) {
        if (awaitingSaveCompletion && postSaveMsg == null) {
            // Wait slightly so toast effect finishes first, then exit cleanly back to desktop.
            delay(1800)
            if (PostSaveMessages.pending.value == null) onFinished()
        }
    }

    val hasAnyOpenDialog = showSessionSheet || showVoiceSheet || showVoucherSheet || showPurchaseSheet ||
        showVoiceHintSheet || voiceDialogType != VoiceDialogType.NONE ||
        ambiguousVoiceCommand != null || wellOwnerActionCommand != null ||
        voucherTypeChooserCommand != null || unrecognizedSpokenText != null ||
        postSaveMsg != null || awaitingSaveCompletion

    LaunchedEffect(hasAnyOpenDialog) {
        if (!hasAnyOpenDialog) {
            delay(120)
            if (PostSaveMessages.pending.value == null && !awaitingSaveCompletion) onFinished()
        }
    }

    LaunchedEffect(mode, operationsCount) {
        if (!LicenseManager.canPerformOperation(context, operationsCount) && !awaitingSaveCompletion) {
            onShowLicense()
        }
    }

    fun openFormForVoiceCommand(cmd: VoiceCommand) {
        activeVoiceCommand = cmd
        when (cmd.type) {
            VoiceCommandType.SESSION -> showSessionSheet = true
            VoiceCommandType.RECEIPT -> {
                voucherType = VoucherType.RECEIPT
                showVoucherSheet = true
            }
            VoiceCommandType.EXPENSE -> {
                voucherType = VoucherType.EXPENSE
                showVoucherSheet = true
            }
            VoiceCommandType.PURCHASE -> showPurchaseSheet = true
            VoiceCommandType.DISCOUNT -> {
                voucherType = VoucherType.RECEIPT
                showVoucherSheet = true
            }
            null -> {
                if (cmd.amount != null) voucherTypeChooserCommand = cmd
                else unrecognizedSpokenText = cmd.rawText
            }
        }
    }

    fun handleVoiceCandidates(matches: List<String>, edited: VoiceCommand? = null) {
        if (matches.isEmpty()) {
            unrecognizedSpokenText = ""
            return
        }
        scope.launch {
            val aliases = VoiceAliasRepository.getAliases(context)
            val knownNames = customers.map { it.name } + aliases.keys
            val parsedCmd = edited ?: VoiceRecognitionHelper.parseFirstValidCommand(
                candidates = matches,
                knownNames = knownNames,
                now = System.currentTimeMillis()
            )
            val firstCandidate = matches.first().trim()
            val hasPieces = parsedCmd != null && (
                parsedCmd.type != null || parsedCmd.amount != null ||
                    parsedCmd.durationMinutes != null || !parsedCmd.customerQuery.isNullOrBlank() ||
                    !parsedCmd.category.isNullOrBlank()
                )
            if (parsedCmd == null || !hasPieces) {
                VoiceUnparsedLog.log(context, firstCandidate, "unrecognized")
                unrecognizedSpokenText = firstCandidate
                return@launch
            }
            if (parsedCmd.type == null) {
                VoiceUnparsedLog.log(context, firstCandidate, "type_chooser")
                voucherTypeChooserCommand = parsedCmd
                return@launch
            }
            if (parsedCmd.durationMinutes != null && parsedCmd.durationMinutes > 0 && !parsedCmd.customerQuery.isNullOrBlank()) {
                val matched = CustomerMatcher.match(parsedCmd.customerQuery, customers, aliases).firstOrNull()?.customer
                if (matched != null && matched.isWellOwner) {
                    val raw = parsedCmd.rawText
                    val hasExplicit = raw.contains("سقي") || raw.contains("شراء") || raw.contains("اشترينا") || raw.contains("جلسة")
                    if (!hasExplicit) {
                        wellOwnerActionCommand = parsedCmd to matched
                        return@launch
                    }
                }
            }
            if (!parsedCmd.customerQuery.isNullOrBlank()) {
                val pool = if (parsedCmd.type == VoiceCommandType.PURCHASE) customers.filter { it.isWellOwner } else customers
                val ranked = CustomerMatcher.match(parsedCmd.customerQuery, pool, aliases)
                if (!CustomerMatcher.isHighConfidence(ranked)) {
                    ambiguousVoiceCommand = parsedCmd
                    ambiguousCandidates = ranked.map { it.customer }.take(5)
                }
            }
            openFormForVoiceCommand(parsedCmd)
        }
    }

    val voiceAliases by produceState(emptyMap<String, Long>()) { value = VoiceAliasRepository.getAliases(context) }
    fun directSaveCustomer(cmd: VoiceCommand): Customer? {
        val q = cmd.customerQuery?.takeIf { it.isNotBlank() } ?: return null
        val ranked = CustomerMatcher.match(q, customers, voiceAliases)
        return if (CustomerMatcher.isHighConfidence(ranked)) ranked.first().customer else null
    }
    fun directSaveBlockReason(cmd: VoiceCommand): String? {
        if (!LicenseManager.canPerformOperation(context, operationsCount)) return "وصلت لحد العمليات، فعّل التطبيق"
        if (cmd.warningBanner != null) return "راجع التنبيه في النموذج قبل الحفظ"
        val customer = directSaveCustomer(cmd)
        return when (cmd.type) {
            VoiceCommandType.SESSION -> when {
                customer == null -> "العميل غير مؤكد، أكمل من النموذج"
                customer.isWellOwner -> "صاحب بئر: أكمل من النموذج"
                (cmd.durationMinutes ?: 0) <= 0 -> "المدة غير محددة"
                (cmd.durationMinutes ?: 0) > 10 * 24 * 60 -> "المدة أكبر من المسموح"
                else -> null
            }
            VoiceCommandType.RECEIPT -> when {
                customer == null -> "العميل غير مؤكد، أكمل من النموذج"
                (cmd.amount ?: 0.0) <= 0.0 -> "المبلغ غير محدد"
                else -> null
            }
            VoiceCommandType.EXPENSE -> when {
                !cmd.customerQuery.isNullOrBlank() && customer == null -> "الاسم غير مؤكد، أكمل من النموذج"
                (cmd.amount ?: 0.0) <= 0.0 -> "المبلغ غير محدد"
                cmd.category.isNullOrBlank() && cmd.notes.isNullOrBlank() -> "حدد البند (مثل ديزل أو سلفة) من النموذج"
                else -> null
            }
            else -> "هذه العملية تُكمل من النموذج"
        }
    }
    fun directSaveVoiceCommand(cmd: VoiceCommand) {
        if (directSaveBlockReason(cmd) != null) { openFormForVoiceCommand(cmd); return }
        val customer = directSaveCustomer(cmd)
        awaitingSaveCompletion = true
        when (cmd.type) {
            VoiceCommandType.SESSION -> {
                val c = customer ?: return
                val mins = cmd.durationMinutes ?: return
                val rate = cmd.ratePerHour?.takeIf { it > 0 } ?: c.customPricePerHour?.takeIf { it > 0 } ?: config.defaultPricePerHour
                val total = Formatters.calculateWaterCost(mins, rate)
                val end = cmd.endMillis ?: System.currentTimeMillis()
                val start = cmd.startMillis ?: (end - mins * 60_000L)
                sessionsViewModel.saveManualSession(
                    customerId = c.id, startTime = start, endTime = end,
                    hours = mins / 60, minutes = mins % 60, pricePerHour = rate,
                    amountPaid = if (cmd.paidFull) total else (cmd.paid ?: 0.0),
                    notes = cmd.notes.orEmpty()
                )
            }
            VoiceCommandType.RECEIPT -> {
                val c = customer ?: return
                val unpaid = allSessions.filter { it.customerId == c.id && it.remainingDebt > 0.0 }
                    .sortedBy { it.startTime }.map { it.id }
                vouchersViewModel.addVoucher(
                    type = VoucherType.RECEIPT, customerId = c.id, amount = cmd.amount ?: return,
                    category = "سداد سقي", paymentMethod = "نقداً", notes = cmd.notes.orEmpty(),
                    selectedSessionIds = unpaid
                )
            }
            VoiceCommandType.EXPENSE -> vouchersViewModel.addVoucher(
                type = VoucherType.EXPENSE, customerId = customer?.id, amount = cmd.amount ?: return,
                category = cmd.category ?: cmd.notes.orEmpty(), paymentMethod = "نقداً",
                notes = if (cmd.category != null) cmd.notes.orEmpty() else ""
            )
            else -> {
                awaitingSaveCompletion = false
                openFormForVoiceCommand(cmd)
            }
        }
    }

    if (showSessionSheet) {
        AddEditSessionBottomSheet(
            initialSession = null,
            customers = customers,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = {
                showSessionSheet = false
                activeVoiceCommand = null
            },
            voiceCommand = activeVoiceCommand,
            onReSpeak = {
                showSessionSheet = false
                showVoiceSheet = true
            },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes, billedTo, wastedMins, wastedReason, discount, costRate, pumpId ->
                if (!LicenseManager.canPerformOperation(context, operationsCount)) {
                    onShowLicense()
                } else {
                    awaitingSaveCompletion = true
                    sessionsViewModel.saveManualSession(
                        id = id,
                        customerId = custId,
                        pumpName = pumpName,
                        startTime = startTime,
                        endTime = endTime,
                        hours = hrs,
                        minutes = mins,
                        pricePerHour = rate,
                        amountPaid = paid,
                        notes = notes,
                        billedToCustomerId = billedTo,
                        wastedMinutes = wastedMins,
                        wastedReason = wastedReason,
                        discountAmount = discount,
                        costPricePerHour = costRate,
                        pumpSourceId = pumpId
                    )
                }
            }
        )
    }

    if (showVoucherSheet) {
        AddVoucherBottomSheet(
            initialType = voucherType,
            customers = customers,
            allSessions = allSessions,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = {
                showVoucherSheet = false
                activeVoiceCommand = null
            },
            voiceCommand = activeVoiceCommand,
            onReSpeak = {
                showVoucherSheet = false
                showVoiceSheet = true
            },
            onSave = { type, customerId, amount, category, method, notes, sessionId, selectedSessionIds, discountAmount ->
                if (!LicenseManager.canPerformOperation(context, operationsCount)) {
                    onShowLicense()
                } else {
                    awaitingSaveCompletion = true
                    vouchersViewModel.addVoucher(
                        type = type,
                        customerId = customerId,
                        amount = amount,
                        category = category,
                        paymentMethod = method,
                        notes = notes,
                        sessionId = sessionId,
                        selectedSessionIds = selectedSessionIds,
                        discountAmount = discountAmount
                    )
                }
            }
        )
    }

    if (showPurchaseSheet) {
        val wellOwners = customers.filter { it.isWellOwner }
        WellOwnerPurchaseBottomSheet(
            owner = null,
            availableOwners = wellOwners,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = {
                showPurchaseSheet = false
                activeVoiceCommand = null
            },
            voiceCommand = activeVoiceCommand,
            onReSpeak = {
                showPurchaseSheet = false
                showVoiceSheet = true
            },
            onSaveWithOwner = { ownerId, date, duration, wasted, rate, paid, notes, startTime, endTime ->
                if (!LicenseManager.canPerformOperation(context, operationsCount)) {
                    onShowLicense()
                } else {
                    awaitingSaveCompletion = true
                    customersViewModel.addOwnerPurchase(
                        ownerCustomerId = ownerId,
                        date = date,
                        durationMinutes = duration,
                        wastedMinutesOnOwner = wasted,
                        purchaseRatePerHour = rate,
                        notes = notes,
                        amountPaid = paid,
                        startTime = startTime,
                        endTime = endTime
                    )
                    showPurchaseSheet = false
                }
            }
        )
    }

    if (showVoiceHintSheet) {
        VoiceHintSheet(
            onDismiss = { showVoiceHintSheet = false },
            onStartListening = {
                showVoiceHintSheet = false
                showVoiceSheet = true
            }
        )
    }

    if (showVoiceSheet) {
        val previewNames = remember(customers) { customers.map { it.name } }
        VoiceRecordSheet(
            examples = listOf(
                "سقي بالساعات" to listOf(
                    "مع احمد علي ساعتين ونص من 10 الف",
                    "مع احمد علي ثلاث ساعات وربع من 10 الف ومسلم 20 الف"
                ),
                "قبض وصرف" to listOf(
                    "20 الف من حارث سعيد",
                    "15 الف ديزل"
                )
            ),
            describe = { candidates ->
                VoiceRecognitionHelper.parseFirstValidCommand(candidates, previewNames)
                    ?.takeIf { it.type != null || it.amount != null || it.durationMinutes != null }
                    ?.let { buildVoiceCommandSummary(it) }
            },
            understand = { candidates ->
                VoiceRecognitionHelper.parseFirstValidCommand(candidates, previewNames)
                    ?.takeIf { it.type != null && (it.amount != null || it.durationMinutes != null || it.customerQuery != null) }
            },
            onCommand = { cmd, candidates ->
                showVoiceSheet = false
                handleVoiceCandidates(candidates, cmd)
            },
            saveBlockReason = { cmd -> directSaveBlockReason(cmd) },
            onDirectSave = { cmd ->
                showVoiceSheet = false
                if (LicenseManager.canPerformOperation(context, operationsCount)) {
                    directSaveVoiceCommand(cmd)
                } else {
                    onShowLicense()
                }
            },
            onDismiss = { showVoiceSheet = false },
            onResult = { candidates ->
                showVoiceSheet = false
                handleVoiceCandidates(candidates)
            }
        )
    }

    wellOwnerActionCommand?.let { (cmd, owner) ->
        VoiceWellOwnerActionChooserDialog(
            ownerName = owner.name,
            onIrrigationOnAccount = {
                wellOwnerActionCommand = null
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.SESSION, customerQuery = owner.name))
            },
            onPurchaseHours = {
                wellOwnerActionCommand = null
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.PURCHASE, customerQuery = owner.name))
            },
            onReSpeak = {
                wellOwnerActionCommand = null
                showVoiceSheet = true
            },
            onDismiss = { wellOwnerActionCommand = null }
        )
    }

    voucherTypeChooserCommand?.let { cmd ->
        VoiceVoucherTypeChooserDialog(
            spokenText = cmd.rawText,
            onReceipt = {
                voucherTypeChooserCommand = null
                voucherType = VoucherType.RECEIPT
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.RECEIPT))
            },
            onExpense = {
                voucherTypeChooserCommand = null
                voucherType = VoucherType.EXPENSE
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.EXPENSE))
            },
            onDiscount = {
                voucherTypeChooserCommand = null
                voucherType = VoucherType.RECEIPT
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.DISCOUNT))
            },
            onReSpeak = {
                voucherTypeChooserCommand = null
                showVoiceSheet = true
            },
            onDismiss = { voucherTypeChooserCommand = null }
        )
    }

    unrecognizedSpokenText?.let { spoken ->
        VoiceUnrecognizedDialog(
            spokenText = spoken,
            onReSpeak = {
                unrecognizedSpokenText = null
                showVoiceSheet = true
            },
            onShowExamples = {
                unrecognizedSpokenText = null
                showVoiceHintSheet = true
            },
            onDismiss = { unrecognizedSpokenText = null }
        )
    }

    VoiceSupportDialogs(
        dialogType = voiceDialogType,
        onDismiss = { voiceDialogType = VoiceDialogType.NONE }
    )

    ambiguousVoiceCommand?.let { cmd ->
        VoiceCustomerChooserDialog(
            spokenQuery = cmd.customerQuery.orEmpty(),
            candidates = ambiguousCandidates,
            onSelectCustomer = { chosenCustomer ->
                scope.launch {
                    cmd.customerQuery?.let { q ->
                        VoiceAliasRepository.saveAlias(context, q, chosenCustomer.id)
                    }
                }
                ambiguousVoiceCommand = null
                openFormForVoiceCommand(cmd.copy(customerQuery = chosenCustomer.name))
            },
            onChooseOther = {
                ambiguousVoiceCommand = null
                openFormForVoiceCommand(cmd.copy(customerQuery = null))
            },
            onAddNewCustomer = { newName ->
                ambiguousVoiceCommand = null
                scope.launch {
                    val newCustId = sessionsViewModel.createCustomer(Customer(name = newName))
                    openFormForVoiceCommand(cmd.copy(customerQuery = newName.also { newCustId }))
                }
            },
            onDismiss = { ambiguousVoiceCommand = null }
        )
    }
}

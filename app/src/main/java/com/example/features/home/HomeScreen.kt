package com.example.features.home

import com.example.core.ui.guide.GuidePreferences
import com.example.core.ui.guide.GuideTexts
import com.example.core.ui.guide.SpotlightTour
import com.example.core.ui.guide.guideTarget
import kotlinx.coroutines.delay


import androidx.compose.material.icons.filled.Mic
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.core.voice.VoiceCommand
import com.example.core.voice.VoiceCommandType
import com.example.core.voice.VoiceHintSheet
import com.example.core.voice.VoiceMissingDialog
import com.example.core.voice.VoiceDialogType
import com.example.core.voice.VoiceSupportDialogs
import com.example.core.voice.VoiceCustomerChooserDialog
import com.example.core.voice.VoiceRecordSheet
import com.example.core.voice.VoiceWellOwnerActionChooserDialog
import com.example.core.ui.LocalSimpleMode
import com.example.core.voice.VoiceVoucherTypeChooserDialog
import com.example.core.voice.VoiceUnrecognizedDialog
import com.example.core.voice.VoiceUnparsedLog
import com.example.core.voice.CustomerMatcher
import com.example.core.voice.VoiceRecognitionHelper
import com.example.core.voice.VoiceAliasRepository
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppTab
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.Formatters
import com.example.features.customers.AddEditCustomerBottomSheet
import com.example.features.customers.Customer
import com.example.features.customers.CustomersViewModel
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.features.settings.SettingsViewModel
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersViewModel
import com.example.features.wellowners.WellOwnerPurchaseBottomSheet
import com.example.core.ui.LocalOpenSubscription
import com.example.core.ui.LocalOperationGate
import com.example.core.license.LicenseManager
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.util.FileSharingHelper
import com.example.core.util.MessageFormatHelper
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.PrimaryTealLight
import com.example.ui.theme.SecondaryAqua
import com.example.ui.theme.mosarib
import java.util.Calendar

data class DayWaterStat(
    val dayName: String,
    val dayDateText: String,
    val totalMinutes: Int,
    val isToday: Boolean
)

@Composable
fun HomeScreen(
    sessionsViewModel: SessionsViewModel,
    customersViewModel: CustomersViewModel,
    vouchersViewModel: VouchersViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToTab: (AppTab) -> Unit,
    onNavigateToCustomer: (Long) -> Unit,
    onOpenReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config by settingsViewModel.appConfig.collectAsStateWithLifecycle()
    val homeStats by sessionsViewModel.homeStats.collectAsStateWithLifecycle()
    val sessions by sessionsViewModel.filteredSessions.collectAsStateWithLifecycle()
    val customers by sessionsViewModel.customers.collectAsStateWithLifecycle()
    val toast by sessionsViewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by settingsViewModel.operationsCount.collectAsStateWithLifecycle()
    val isActivated by settingsViewModel.isActivated.collectAsStateWithLifecycle()
    val allSessions by vouchersViewModel.allSessions.collectAsStateWithLifecycle()
    val customGroups by customersViewModel.customGroups.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (config.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    val context = LocalContext.current


    var showAddManualSheet by remember { mutableStateOf(false) }
    var showAddVoucherSheet by remember { mutableStateOf(false) }
    var voucherInitialType by remember { mutableStateOf(VoucherType.RECEIPT) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var showAddPurchaseSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var activeVoiceCommand by remember { mutableStateOf<VoiceCommand?>(null) }
    var showVoiceHintSheet by remember { mutableStateOf(false) }
    var showHomeVoiceRecordSheet by remember { mutableStateOf(false) }
    var voiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }
    var ambiguousVoiceCommand by remember { mutableStateOf<VoiceCommand?>(null) }
    var ambiguousCandidates by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var wellOwnerActionCommand by remember { mutableStateOf<Pair<VoiceCommand, Customer>?>(null) }
    var voucherTypeChooserCommand by remember { mutableStateOf<VoiceCommand?>(null) }
    var unrecognizedSpokenText by remember { mutableStateOf<String?>(null) }

    var activeTour by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(600)
        if (!GuidePreferences.isTourDone(context, GuidePreferences.TOUR_HOME)
            && !showAddManualSheet && !showAddVoucherSheet && !showAddCustomerSheet
            && !showAddPurchaseSheet && !showHomeVoiceRecordSheet && !showVoiceHintSheet
        ) {
            activeTour = GuidePreferences.TOUR_HOME
        }
    }

    fun openFormForVoiceCommand(cmd: VoiceCommand) {
        activeVoiceCommand = cmd
        when (cmd.type) {
            VoiceCommandType.SESSION -> {
                showAddManualSheet = true
            }
            VoiceCommandType.RECEIPT -> {
                voucherInitialType = VoucherType.RECEIPT
                showAddVoucherSheet = true
            }
            VoiceCommandType.EXPENSE -> {
                voucherInitialType = VoucherType.EXPENSE
                showAddVoucherSheet = true
            }
            VoiceCommandType.PURCHASE -> {
                showAddPurchaseSheet = true
            }
            VoiceCommandType.DISCOUNT -> {
                voucherInitialType = VoucherType.RECEIPT
                showAddVoucherSheet = true
            }
            null -> {
                if (cmd.amount != null) {
                    voucherTypeChooserCommand = cmd
                } else {
                    unrecognizedSpokenText = cmd.rawText
                }
            }
        }
    }

    fun handleVoiceCandidates(matches: List<String>, edited: VoiceCommand? = null) {
        if (matches.isEmpty()) {
            unrecognizedSpokenText = ""
            return
        }
        coroutineScope.launch {
            val aliases = VoiceAliasRepository.getAliases(context)
            val knownCustomerNames = customers.map { it.name } + aliases.keys
            val parsedCmd = edited ?: VoiceRecognitionHelper.parseFirstValidCommand(
                candidates = matches,
                knownNames = knownCustomerNames,
                now = System.currentTimeMillis()
            )
            val firstCandidate = matches.first().trim()
            VoiceRecognitionHelper.logDebug("VoiceInput", "candidates: $matches, parsed: $parsedCmd")

            val hasPieces = parsedCmd != null && (
                parsedCmd.type != null ||
                parsedCmd.amount != null ||
                parsedCmd.durationMinutes != null ||
                !parsedCmd.customerQuery.isNullOrBlank() ||
                !parsedCmd.category.isNullOrBlank()
            )

            if (parsedCmd == null || !hasPieces) {
                VoiceRecognitionHelper.logDebug("VoiceInput", "branch: UNRECOGNIZED_COMMAND, spoken='$firstCandidate'")
                VoiceUnparsedLog.log(context, firstCandidate, "unrecognized")
                unrecognizedSpokenText = firstCandidate
                return@launch
            }

            // Branch 1: Voucher type chooser (when type is undecided, open chooser with filled pieces)
            if (parsedCmd.type == null) {
                VoiceRecognitionHelper.logDebug("VoiceInput", "branch: VOUCHER_TYPE_CHOOSER, cmd=$parsedCmd")
                VoiceUnparsedLog.log(context, firstCandidate, "type_chooser")
                voucherTypeChooserCommand = parsedCmd
                return@launch
            }
            // Branch 2: Well owner duration ambiguity ("سقي على حسابه / شراء ساعات منه")
            if (parsedCmd.durationMinutes != null && parsedCmd.durationMinutes > 0 && !parsedCmd.customerQuery.isNullOrBlank()) {
                val aliases = VoiceAliasRepository.getAliases(context)
                val matched = CustomerMatcher.match(parsedCmd.customerQuery, customers, aliases).firstOrNull()?.customer
                if (matched != null && matched.isWellOwner) {
                    val raw = parsedCmd.rawText
                    val hasExplicitVerb = raw.contains("سقي") || raw.contains("شراء") || raw.contains("اشترينا") || raw.contains("جلسة")
                    if (!hasExplicitVerb) {
                        VoiceRecognitionHelper.logDebug("VoiceInput", "branch: WELL_OWNER_CHOOSER, owner=${matched.name}")
                        wellOwnerActionCommand = Pair(parsedCmd, matched)
                        return@launch
                    }
                }
            }

            // Branch 3: Customer matching
            if (!parsedCmd.customerQuery.isNullOrBlank()) {
                val list = if (parsedCmd.type == VoiceCommandType.PURCHASE) {
                    customers.filter { it.isWellOwner }
                } else {
                    customers
                }
                val aliases = VoiceAliasRepository.getAliases(context)
                val ranked = CustomerMatcher.match(parsedCmd.customerQuery, list, aliases)
                if (CustomerMatcher.isHighConfidence(ranked)) {
                    VoiceRecognitionHelper.logDebug("VoiceInput", "branch: CONFIDENT_CUSTOMER, query='${parsedCmd.customerQuery}', match='${ranked.first().customer.name}'")
                    openFormForVoiceCommand(parsedCmd)
                    return@launch
                } else {
                    // Not high confidence: either empty, single match < 0.85, or multiple ambiguous matches.
                    // Open the form with the spoken name in the field and show the chooser:
                    // the closest customers plus "إضافة عميل جديد باسم «...»"
                    VoiceRecognitionHelper.logDebug("VoiceInput", "branch: AMBIGUOUS_OR_LOW_CONFIDENCE, query='${parsedCmd.customerQuery}', count=${ranked.size}")
                    ambiguousVoiceCommand = parsedCmd
                    ambiguousCandidates = ranked.map { it.customer }.take(5) // empty when nothing is close: only "add new" / "other"
                    openFormForVoiceCommand(parsedCmd)
                    return@launch
                }
            }

            // Branch 4: Open form
            VoiceRecognitionHelper.logDebug("VoiceInput", "branch: OPEN_FORM_${parsedCmd.type}")
            openFormForVoiceCommand(parsedCmd)
        }
    }
    // Direct save from the "فهمت" card: only when nothing needs the form's extra choices.
    val voiceAliases by androidx.compose.runtime.produceState(emptyMap<String, Long>()) { value = VoiceAliasRepository.getAliases(context) }
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
        when (cmd.type) {
            VoiceCommandType.SESSION -> {
                val c = customer ?: return
                val mins = cmd.durationMinutes ?: return
                val rate = cmd.ratePerHour?.takeIf { it > 0 }
                    ?: c.customPricePerHour?.takeIf { it > 0 } ?: config.defaultPricePerHour
                val total = com.example.core.util.Formatters.calculateWaterCost(mins, rate)
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
                // Oldest unpaid sessions first, like the receipt form.
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
            else -> openFormForVoiceCommand(cmd)
        }
    }

    LaunchedEffect(Unit) {
        VoiceRecognitionHelper.checkArabicSupport(context) { _ -> }
    }

    val checkOperationAllowed = LocalOperationGate.current
    val openSubscription = LocalOpenSubscription.current
    val trialEnded = LicenseManager.isTrialLimitExceeded(context, operationsCount)

    val homeHeaderRow: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimaryTealDark, PrimaryTealLight)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_logo_mark),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "المُسَرِّبْ",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• Mosarib",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Help / Interactive Tour Trigger Button
                    IconButton(
                        onClick = { activeTour = GuidePreferences.TOUR_HOME },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "دليل الاستخدام السريع",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Living Breathing Theme Toggle Button
                    LivingThemeToggleButton(
                        isDarkTheme = isDarkTheme,
                        onToggle = {
                            val nextMode = if (isDarkTheme) "LIGHT" else "DARK"
                            settingsViewModel.updateThemeMode(nextMode)
                        }
                    )
                }
            }
    }

    val homeHeader: @Composable () -> Unit = {
        Column {
            if (trialEnded) {
                Text(
                    text = "انتهت الفترة التجريبية — اضغط للاشتراك",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { openSubscription() }
                        .padding(vertical = 6.dp)
                )
            }
            homeHeaderRow()
        }
    }

    val simpleHome = LocalSimpleMode.current
    Box(modifier = modifier.fillMaxSize()) {
        if (simpleHome) {
            Column(modifier = Modifier.fillMaxSize()) {
                homeHeader()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HomeCompactActionButton(
                        title = "سقي",
                        icon = Icons.Default.WaterDrop,
                        bg = MaterialTheme.mosarib.infoContainer,
                        tint = MaterialTheme.mosarib.onInfoContainer,
                        onClick = { checkOperationAllowed { showAddManualSheet = true } },
                        modifier = Modifier.weight(1f).guideTarget("home_session_new")
                    )
                    HomeCompactActionButton(
                        title = "قبض",
                        icon = Icons.Default.Payments,
                        bg = MaterialTheme.mosarib.successContainer,
                        tint = MaterialTheme.mosarib.onSuccessContainer,
                        onClick = {
                            checkOperationAllowed {
                                voucherInitialType = VoucherType.RECEIPT
                                showAddVoucherSheet = true
                            }
                        },
                        modifier = Modifier.weight(1f).guideTarget("home_vouchers")
                    )
                    HomeCompactActionButton(
                        title = "صرف",
                        icon = Icons.Default.Payments,
                        bg = MaterialTheme.colorScheme.errorContainer,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        onClick = {
                            checkOperationAllowed {
                                voucherInitialType = VoucherType.EXPENSE
                                showAddVoucherSheet = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    HomeCompactActionButton(
                        title = "شراء",
                        icon = Icons.Default.AccessTime,
                        bg = MaterialTheme.mosarib.infoContainer,
                        tint = MaterialTheme.mosarib.onInfoContainer,
                        onClick = { checkOperationAllowed { showAddPurchaseSheet = true } },
                        modifier = Modifier.weight(1f)
                    )
                    HomeCompactActionButton(
                        title = "صوت",
                        icon = Icons.Default.Mic,
                        bg = MaterialTheme.colorScheme.primaryContainer,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            if (!VoiceRecognitionHelper.isRecognitionAvailable(context)) {
                                voiceDialogType = VoiceDialogType.MISSING_ENGINE
                            } else {
                                checkOperationAllowed { showHomeVoiceRecordSheet = true }
                            }
                        },
                        modifier = Modifier.weight(1f).guideTarget("home_voice")
                    )
                }
                BackupReminderBanner(
                    settingsViewModel = settingsViewModel,
                    hasOperations = operationsCount >= 1,
                    onLocked = openSubscription
                )
                HomeCustomerList(
                    customersViewModel = customersViewModel,
                    onNavigateToCustomer = onNavigateToCustomer,
                    modifier = Modifier.weight(1f)
                )
            }
        } else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Header Branding & Animated Living Theme Toggle Button
            item { homeHeader() }



            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الوصول السريع",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            item {
                val simpleMode = LocalSimpleMode.current
                val VoiceCard: @Composable (Modifier) -> Unit = { mod ->
                    QuickActionCard(
                        title = "إدخال صوتي",
                        subtitle = "تسجيل دورة، سند، أو مصروف بالصوت",
                        icon = Icons.Default.Mic,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = PrimaryTeal,
                        onClick = {
                            if (!VoiceRecognitionHelper.isRecognitionAvailable(context)) {
                                voiceDialogType = VoiceDialogType.MISSING_ENGINE
                            } else {
                                checkOperationAllowed { showHomeVoiceRecordSheet = true }
                            }
                        },
                        modifier = mod.guideTarget("home_voice")
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "سقي جديد",
                            subtitle = "تسجيل دورة ري",
                            icon = Icons.Default.WaterDrop,
                            iconBgColor = MaterialTheme.mosarib.infoContainer,
                            iconTint = MaterialTheme.mosarib.info,
                            onClick = {
                                checkOperationAllowed {
                                    showAddManualSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f).guideTarget("home_session_new")
                        )

                        QuickActionCard(
                            title = "قبض",
                            subtitle = "استلمت مبلغاً",
                            icon = Icons.Default.Payments,
                            iconBgColor = MaterialTheme.mosarib.successContainer,
                            iconTint = MaterialTheme.mosarib.success,
                            onClick = {
                                checkOperationAllowed {
                                    voucherInitialType = VoucherType.RECEIPT
                                    showAddVoucherSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f).guideTarget("home_vouchers")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "صرف",
                            subtitle = "دفعت مبلغاً",
                            icon = Icons.Default.Payments,
                            iconBgColor = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.mosarib.debt,
                            onClick = {
                                checkOperationAllowed {
                                    voucherInitialType = VoucherType.EXPENSE
                                    showAddVoucherSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (simpleMode) VoiceCard(Modifier.weight(1f))
                        else QuickActionCard(
                            title = "حسابات العملاء",
                            subtitle = "الأرصدة وكشوف الحساب",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconBgColor = MaterialTheme.colorScheme.errorContainer,
                            iconTint = MaterialTheme.mosarib.debt,
                            onClick = { onNavigateToTab(AppTab.CUSTOMERS) },
                            modifier = Modifier.weight(1f).guideTarget("home_accounts")
                        )
                    }

                    if (!simpleMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionCard(
                                title = "شراء ساعات",
                                subtitle = "شراء ماء من بئر",
                                icon = Icons.Default.AccessTime,
                                iconBgColor = MaterialTheme.mosarib.infoContainer,
                                iconTint = MaterialTheme.mosarib.info,
                                onClick = {
                                    checkOperationAllowed {
                                        showAddPurchaseSheet = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            QuickActionCard(
                                title = "عميل جديد",
                                subtitle = "إضافة مزارع أو بئر",
                                icon = Icons.Default.PersonAdd,
                                iconBgColor = MaterialTheme.mosarib.infoContainer,
                                iconTint = MaterialTheme.mosarib.info,
                                onClick = { showAddCustomerSheet = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionCard(
                                title = "النسخ الاحتياطي",
                                subtitle = "حفظ واستعادة سحابية ومحلية",
                                icon = Icons.Default.CloudSync,
                                iconBgColor = MaterialTheme.mosarib.successContainer,
                                iconTint = MaterialTheme.mosarib.success,
                                onClick = {
                                    settingsViewModel.selectDriveBackupTab()
                                    onNavigateToTab(AppTab.SETTINGS)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        VoiceCard(Modifier.fillMaxWidth())
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 5. Recent Operations Section (آخر دورات السقي)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .guideTarget("home_last_sessions")
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخر دورات السقي",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    TextButton(
                        onClick = { onNavigateToTab(AppTab.VOUCHERS) }
                    ) {
                        Text(
                            text = "عرض الكل ←",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Latest 3 Sessions Preview
            val recentSessions = sessions.take(3)
            if (recentSessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = MaterialTheme.mosarib.textMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "لا توجد دورات سقي مسجلة حتى الآن",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.mosarib.textMuted
                            )
                            TextButton(
                                onClick = { checkOperationAllowed { showAddManualSheet = true } }
                            ) {
                                Text(
                                    text = "سجّل أول دورة سقي",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                items(recentSessions, key = { it.session.id }) { item ->
                    RecentSessionMiniCard(
                        sessionWithCustomer = item,
                        currencySymbol = config.currencySymbol,
                        onClick = { onNavigateToCustomer(item.session.billedToCustomerId ?: item.session.customerId) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 6. Visual Reports & Charts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سقي الأسبوع",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    TextButton(
                        onClick = onOpenReports,
                        modifier = Modifier.guideTarget("home_comprehensive_report")
                    ) {
                        Text(
                            text = "التقرير الكامل ←",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item {
                WeeklyIrrigationBarChartCard(sessions = sessions)
            }
        }

        // Floating Toast Notification
        LuxuryToastNotification(
            toast = toast,
            onDismiss = { sessionsViewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    // Sheet: Add Manual Session
    if (showAddManualSheet) {
        AddEditSessionBottomSheet(
            initialSession = null,
            customers = customers,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = {
                showAddManualSheet = false
                activeVoiceCommand = null
            },
            voiceCommand = activeVoiceCommand,
            onReSpeak = {
                showAddManualSheet = false
                showHomeVoiceRecordSheet = true
            },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes, billedTo, wastedMins, wastedReason, discount, costRate, pumpId ->
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
        )
    }

    // Sheet: Add Voucher (سند قبض أو مصروف)
    if (showAddVoucherSheet) {
        AddVoucherBottomSheet(
            initialType = voucherInitialType,
            customers = customers,
            allSessions = allSessions,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = {
                showAddVoucherSheet = false
                activeVoiceCommand = null
            },
            voiceCommand = activeVoiceCommand,
            onReSpeak = {
                showAddVoucherSheet = false
                showHomeVoiceRecordSheet = true
            },
            onSave = { type, customerId, amount, category, method, notes, sessionId, selectedSessionIds, discountAmount ->
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
        )
    }

    // Sheet: Well Owner Purchase (شراء ماء من بئر)
    if (showAddPurchaseSheet) {
        val wellOwners = customers.filter { it.isWellOwner }
        WellOwnerPurchaseBottomSheet(
            owner = null,
            availableOwners = wellOwners,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = {
                showAddPurchaseSheet = false
                activeVoiceCommand = null
            },
            voiceCommand = activeVoiceCommand,
            onReSpeak = {
                showAddPurchaseSheet = false
                showHomeVoiceRecordSheet = true
            },
            onSaveWithOwner = { ownerId, date, duration, wasted, rate, paid, notes, startTime, endTime ->
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
                showAddPurchaseSheet = false
            }
        )
    }

    // Sheet: Add Customer
    if (showAddCustomerSheet) {
        AddEditCustomerBottomSheet(
            initialCustomer = null,
            existingGroups = customGroups,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddCustomerSheet = false },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup ->
                customersViewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup)
            }
        )
    }

    // Interactive Spotlight Tour
    if (activeTour != null) {
        // A new tour id must start a fresh step counter.
        key(activeTour) {
            SpotlightTour(
                steps = GuideTexts.getTour(activeTour!!),
                onFinish = {
                    val finished = activeTour
                    activeTour = null
                    if (finished != null) {
                        GuidePreferences.markTourDone(context, finished, true)
                        if (finished == GuidePreferences.TOUR_HOME && !GuidePreferences.isTourDone(context, GuidePreferences.TOUR_NAV)) {
                            activeTour = GuidePreferences.TOUR_NAV
                        }
                    }
                }
            )
        }
    }

    // Voice Hint Sheet, Support Dialogs, and Ambiguous Customer Chooser
    if (showVoiceHintSheet) {
        VoiceHintSheet(
            onDismiss = { showVoiceHintSheet = false },
            onStartListening = {
                showVoiceHintSheet = false
                showHomeVoiceRecordSheet = true
            }
        )
    }

    if (showHomeVoiceRecordSheet) {
        val previewNames = remember(customers) { customers.map { it.name } }
        VoiceRecordSheet(
            examples = listOf(
                "سقي بالساعات" to listOf(
                    "مع احمد علي ساعتين ونص من 10 الف",
                    "مع احمد علي ثلاث ساعات وربع من 10 الف ومسلم 20 الف",
                    "مع احمد علي ساعه ونص من 12 الف مسلم كامل",
                    "سقي محمد صالح ساعتين الا ربع بسعر 9 الاف دين",
                    "مع احمد علي اربع ساعات من 10 الف وسلم 15 الف باقي الاسبوع"
                ),
                "سقي من ساعة إلى ساعة" to listOf(
                    "مع احمد علي من الساعه 8 بالصبح الى الساعه 11 ونص من 10 الف",
                    "مع احمد علي من الساعه 9 بالليل لا الساعه 1 ومسلم كامل",
                    "مع احمد علي من امس الساعه 10 بالليل لا الساعه 6 بالصبح",
                    "مع احمد علي من يوم الخميس الساعه 10 بالليل الى يوم الجمعه الساعه 4 العصر"
                ),
                "قبض" to listOf(
                    "20 الف من حارث سعيد",
                    "قبض 50 الف من محمد صالح باقي الشهر",
                    "وصل 30 الف لحساب حارث سعيد",
                    "استلمت من احمد علي 100 الف"
                ),
                "صرف وخصم" to listOf(
                    "50 الف مع يوسف قاسم سلفه",
                    "اعطيت يوسف 10 الف",
                    "خصم 5 الف لاحمد علي",
                    "15 الف ديزل",
                    "20 الف حق صيانه المضخه"
                ),
                "شراء ساعات من صاحب البئر" to listOf(
                    "اشتريت ثلاث ساعات من صاحب البئر سالم من 7 الاف"
                )
            ),
            describe = { candidates ->
                VoiceRecognitionHelper.parseFirstValidCommand(candidates, previewNames)
                    ?.takeIf { it.type != null || it.amount != null || it.durationMinutes != null }
                    ?.let { com.example.core.voice.buildVoiceCommandSummary(it) }
            },
            understand = { candidates ->
                VoiceRecognitionHelper.parseFirstValidCommand(candidates, previewNames)
                    ?.takeIf { it.type != null && (it.amount != null || it.durationMinutes != null || it.customerQuery != null) }
            },
            onCommand = { cmd, candidates ->
                showHomeVoiceRecordSheet = false
                handleVoiceCandidates(candidates, cmd)
            },
            saveBlockReason = { cmd -> directSaveBlockReason(cmd) },
            onDirectSave = { cmd ->
                showHomeVoiceRecordSheet = false
                checkOperationAllowed { directSaveVoiceCommand(cmd) }
            },
            onDismiss = { showHomeVoiceRecordSheet = false },
            onResult = { candidates ->
                showHomeVoiceRecordSheet = false
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
                showHomeVoiceRecordSheet = true
            },
            onDismiss = { wellOwnerActionCommand = null }
        )
    }

    voucherTypeChooserCommand?.let { cmd ->
        VoiceVoucherTypeChooserDialog(
            spokenText = cmd.rawText,
            onReceipt = {
                voucherTypeChooserCommand = null
                voucherInitialType = VoucherType.RECEIPT
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.RECEIPT))
            },
            onExpense = {
                voucherTypeChooserCommand = null
                voucherInitialType = VoucherType.EXPENSE
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.EXPENSE))
            },
            onDiscount = {
                voucherTypeChooserCommand = null
                voucherInitialType = VoucherType.RECEIPT
                openFormForVoiceCommand(cmd.copy(type = VoiceCommandType.DISCOUNT))
            },
            onReSpeak = {
                voucherTypeChooserCommand = null
                showHomeVoiceRecordSheet = true
            },
            onDismiss = { voucherTypeChooserCommand = null }
        )
    }

    unrecognizedSpokenText?.let { spoken ->
        VoiceUnrecognizedDialog(
            spokenText = spoken,
            onReSpeak = {
                unrecognizedSpokenText = null
                showHomeVoiceRecordSheet = true
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
                coroutineScope.launch {
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
                coroutineScope.launch {
                    val newCustId = sessionsViewModel.createCustomer(Customer(name = newName))
                    val newCust = Customer(id = newCustId, name = newName)
                    openFormForVoiceCommand(cmd.copy(customerQuery = newCust.name))
                }
            },
            onDismiss = {
                ambiguousVoiceCommand = null
            }
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp = 360, showBackground = true)
@Composable
private fun HomeCompactRowLightPreview() = HomeCompactRowPreview(false)

@androidx.compose.ui.tooling.preview.Preview(widthDp = 360, showBackground = true, backgroundColor = 0xFF0A1014)
@Composable
private fun HomeCompactRowDarkPreview() = HomeCompactRowPreview(true)

@Composable
private fun HomeCompactRowPreview(dark: Boolean) {
    com.example.ui.theme.MosaribTheme(darkTheme = dark) {
        Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeCompactActionButton("سقي", Icons.Default.WaterDrop, MaterialTheme.mosarib.infoContainer, MaterialTheme.mosarib.onInfoContainer, {}, Modifier.weight(1f))
            HomeCompactActionButton("قبض", Icons.Default.Payments, MaterialTheme.mosarib.successContainer, MaterialTheme.mosarib.onSuccessContainer, {}, Modifier.weight(1f))
            HomeCompactActionButton("صرف", Icons.Default.Payments, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, {}, Modifier.weight(1f))
            HomeCompactActionButton("شراء", Icons.Default.AccessTime, MaterialTheme.mosarib.infoContainer, MaterialTheme.mosarib.onInfoContainer, {}, Modifier.weight(1f))
            HomeCompactActionButton("صوت", Icons.Default.Mic, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, {}, Modifier.weight(1f))
        }
    }
}

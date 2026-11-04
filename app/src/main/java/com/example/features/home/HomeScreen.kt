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
import com.example.features.vouchers.SettlementResultDialog
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.VouchersViewModel
import com.example.features.wellowners.WellOwnerPurchaseBottomSheet
import com.example.core.license.LicenseDialog
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
    val settlementResult by vouchersViewModel.settlementResult.collectAsStateWithLifecycle()
    val customGroups by customersViewModel.customGroups.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (config.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    val context = LocalContext.current
    var showActivationDialog by remember { mutableStateOf(false) }


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

    fun handleVoiceCandidates(matches: List<String>) {
        if (matches.isEmpty()) {
            unrecognizedSpokenText = ""
            return
        }
        coroutineScope.launch {
            val aliases = VoiceAliasRepository.getAliases(context)
            val knownCustomerNames = customers.map { it.name } + aliases.keys
            val parsedCmd = VoiceRecognitionHelper.parseFirstValidCommand(
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
    var homePurchaseTarget by remember { mutableStateOf<Triple<Customer, String, String>?>(null) }

    LaunchedEffect(Unit) {
        VoiceRecognitionHelper.checkArabicSupport(context) { _ -> }
    }

    fun checkOperationAllowed(action: () -> Unit) {
        if (LicenseManager.canPerformOperation(context, operationsCount)) {
            action()
        } else {
            showActivationDialog = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // 1. Header Branding & Animated Living Theme Toggle Button
            item {
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
                            subtitle = "سند تحصيل كاش",
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
                            subtitle = "صيانة ونفقات",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            iconBgColor = MaterialTheme.mosarib.debt.copy(alpha = 0.12f),
                            iconTint = MaterialTheme.mosarib.debt,
                            onClick = {
                                checkOperationAllowed {
                                    voucherInitialType = VoucherType.EXPENSE
                                    showAddVoucherSheet = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

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
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionCard(
                            title = "عميل جديد",
                            subtitle = "إضافة مزارع أو بئر",
                            icon = Icons.Default.PersonAdd,
                            iconBgColor = MaterialTheme.mosarib.infoContainer,
                            iconTint = MaterialTheme.mosarib.info,
                            onClick = { showAddCustomerSheet = true },
                            modifier = Modifier.weight(1f)
                        )

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
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionCard(
                            title = "إدخال صوتي",
                            subtitle = "تسجيل دورة، سند، أو مصروف بالصوت",
                            icon = Icons.Default.Mic,
                            iconBgColor = PrimaryTeal.copy(alpha = 0.15f),
                            iconTint = PrimaryTeal,
                            onClick = {
                                if (!VoiceRecognitionHelper.isRecognitionAvailable(context)) {
                                    voiceDialogType = VoiceDialogType.MISSING_ENGINE
                                } else {
                                    showHomeVoiceRecordSheet = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().guideTarget("home_voice")
                        )
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
                    endTime = endTime,
                    onCreated = { savedPurchase ->
                        val targetOwner = customers.find { it.id == ownerId }
                        if (targetOwner != null && targetOwner.phone.isNotBlank()) {
                            val waMsg = MessageFormatHelper.buildPurchaseBill(savedPurchase, targetOwner, config, isSms = false)
                            val smsMsg = MessageFormatHelper.buildPurchaseBill(savedPurchase, targetOwner, config, isSms = true)
                            homePurchaseTarget = Triple(targetOwner, waMsg, smsMsg)
                        }
                    }
                )
                showAddPurchaseSheet = false
            }
        )
    }

    homePurchaseTarget?.let { (owner, waMsg, smsMsg) ->
        SendMessageChoiceDialog(
            recipientName = owner.name,
            recipientPhone = owner.phone,
            messageText = waMsg,
            smsMessageText = smsMsg,
            onDismiss = { homePurchaseTarget = null },
            onSendWhatsApp = {
                homePurchaseTarget = null
                FileSharingHelper.sendWhatsAppMessage(context, owner.phone, waMsg)
            },
            onSendSms = {
                homePurchaseTarget = null
                FileSharingHelper.sendSms(context, owner.phone, smsMsg)
            }
        )
    }

    settlementResult?.let { res ->
        SettlementResultDialog(
            result = res,
            onDismiss = { vouchersViewModel.clearSettlementResult() }
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

    // License Activation Dialog
    if (showActivationDialog) {
        LicenseDialog(
            onDismiss = { showActivationDialog = false },
            onActivated = {
                settingsViewModel.refreshActivationStatus()
                showActivationDialog = false
            },
            isMandatory = LicenseManager.isTrialLimitExceeded(context, operationsCount)
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

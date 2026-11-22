package com.example.features.vouchers

import com.example.core.ui.guide.GuideHelpButton
import com.example.core.ui.guide.GuidePreferences
import com.example.core.ui.guide.GuideTexts
import com.example.core.ui.guide.SpotlightTour
import com.example.core.ui.guide.guideTarget
import kotlinx.coroutines.delay


import com.example.core.voice.VoiceCommand
import com.example.core.voice.VoiceCommandType
import com.example.core.voice.VoiceFieldMic
import com.example.core.voice.VoiceSummaryBanner
import com.example.core.voice.VoiceDialogType
import com.example.core.voice.VoiceSupportDialogs
import com.example.core.voice.VoiceRecordSheet
import com.example.core.voice.VoiceUnrecognizedDialog
import com.example.core.voice.CustomerMatcher
import com.example.core.voice.VoiceAliasRepository
import com.example.core.voice.SpokenNumberParser
import com.example.core.voice.VoiceRecognitionHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.example.core.speech.MosaribSpeaker
import com.example.core.speech.SpeechStatus
import com.example.core.speech.SpeechTextBuilder
import com.example.core.speech.TtsMissingDataDialog
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import java.util.Date
import com.example.core.ui.AmountWithWords
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribDateRangeDialog
import com.example.core.ui.MosaribDialog
import com.example.core.ui.MosaribMenu
import com.example.core.speech.*
import com.example.core.ui.MosaribMenuDivider
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribSecondaryButton
import com.example.core.ui.DialogTone
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import com.example.core.ui.MosaribDatePickerDialog
import com.example.features.reports.ReportPeriod
import com.example.core.ui.DropdownOption
import com.example.core.ui.FilterClearAllChip
import com.example.core.ui.FilterDropdownChip
import com.example.core.ui.CustomerSearchPickerDialog
import com.example.features.vouchers.AccountTypeFilter
import com.example.features.vouchers.OperationsFilterState
import com.example.features.vouchers.OperationsFilterEngine
import com.example.features.vouchers.OperationsSort
import com.example.features.vouchers.SessionPaymentFilter

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.core.ui.isAppDark
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribFilterChip
import com.example.core.ui.MosaribTextField
import com.example.core.ui.OperationConfirmSheet
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.util.OperationStatement
import com.example.core.util.PdfReportGenerator
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.ToastType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.core.ui.RowGroupPosition
import com.example.core.ui.RowStatus
import com.example.core.util.DayGrouping
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import com.example.core.ui.OperationCard
import com.example.core.ui.CardBadge
import com.example.core.ui.CardChip
import com.example.features.sessions.SessionPaymentHistoryExpander
import com.example.core.ui.OperationRow
import com.example.ui.theme.mosarib
import com.example.core.ui.LocalOperationGate
import com.example.core.license.LicenseManager
import com.example.core.ui.StatBoxCard
import com.example.core.util.FileSharingHelper
import com.example.core.util.MessageFormatHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.example.core.util.Formatters
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.ui.AmountTooltip
import com.example.features.customers.CustomersViewModel
import com.example.features.customers.ledgerTotalsFor
import com.example.features.customers.Customer
import com.example.features.customers.AddEditCustomerBottomSheet
import com.example.core.ui.CustomerPickerField
import com.example.core.ui.LocalSimpleMode
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.SessionCardItem
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.core.ui.SessionDetailBottomSheet
import com.example.core.ui.VoucherDetailBottomSheet
import com.example.core.ui.WellOwnerPurchaseDetailBottomSheet
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseBottomSheet
import com.example.features.wellowners.WellOwnerPurchaseMath
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VouchersScreen(
    viewModel: VouchersViewModel,
    sessionsViewModel: SessionsViewModel? = null,
    onNavigateToCustomer: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val operations by viewModel.unifiedOperations.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val operationsFilter by viewModel.operationsFilter.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val summaryOps by viewModel.summaryOperations.collectAsStateWithLifecycle()
    val allBalances by viewModel<CustomersViewModel>().rawCustomersWithBalance.collectAsStateWithLifecycle()
    val distinctPaymentMethods by viewModel.distinctPaymentMethods.collectAsStateWithLifecycle()
    val distinctPumpNames by viewModel.distinctPumpNames.collectAsStateWithLifecycle()
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showCustomerPickerDialog by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val frequentDescriptions by viewModel.frequentExpenseDescriptions.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    var searchVoiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }
    var showSearchVoiceSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isAppDark()

    val gate = LocalOperationGate.current

    var missingSpeechStatus by remember { mutableStateOf<SpeechStatus?>(null) }
    var speechError by remember { mutableStateOf<SpeechError?>(null) }

    LaunchedEffect(Unit) {
        MosaribSpeaker.errors.collect { err ->
            speechError = err
        }
    }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var showAddPurchaseSheet by remember { mutableStateOf(false) }
    var speedDialOpen by remember { mutableStateOf(false) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }
    var voucherToEdit by remember { mutableStateOf<Voucher?>(null) }
    var sessionToEdit by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<WaterSession?>(null) }
    var purchaseToDelete by remember { mutableStateOf<WellOwnerPurchase?>(null) }
    var purchaseToEdit by remember { mutableStateOf<WellOwnerPurchase?>(null) }
    var voucherMessageTarget by remember { mutableStateOf<Triple<Customer, String, String>?>(null) }
    var voucherPdfReady by remember { mutableStateOf<Pair<File, String>?>(null) }
    var selectedLinkedSession by remember { mutableStateOf<WaterSession?>(null) }
    var sessionForDetails by remember { mutableStateOf<WaterSessionWithCustomer?>(null) }
    var voucherForDetails by remember { mutableStateOf<VoucherWithCustomer?>(null) }
    var purchaseForDetails by remember { mutableStateOf<Pair<WellOwnerPurchase, Customer?>?>(null) }

    val vmPdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    LaunchedEffect(vmPdfReady) {
        if (vmPdfReady != null) {
            voucherPdfReady = vmPdfReady
            viewModel.clearPdfReady()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val isCompactScreen = screenWidthDp < 400
    val isCompactHeight = configuration.screenHeightDp < 520 || isLandscape

    val listState = rememberLazyListState()
    val firstIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
    val summaryScrollAlpha by remember {
        derivedStateOf {
            if (firstIndex > 0) 0f
            else (1f - (firstOffset / 180f)).coerceIn(0f, 1f)
        }
    }
    val isStickyPinned by remember {
        derivedStateOf { firstIndex > 0 || firstOffset > 90 }
    }

    Box(modifier = modifier.fillMaxSize()) {
    val dayGroups = remember(operations) {
        DayGrouping.groupByDay(
            items = operations,
            timestampSelector = { op ->
                when (op) {
                    is UnifiedOperation.SessionOp -> op.sessionWithCustomer.session.startTime
                    is UnifiedOperation.VoucherOp -> op.voucherWithCustomer.voucher.date
                    is UnifiedOperation.PurchaseOp -> op.purchase.date
                }
            }
        )
    }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = if (isCompactHeight) 60.dp else 80.dp)
        ) {
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = if (isStickyPinned) 3.dp else 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isCompactHeight) 2.dp else 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(if (isCompactHeight) 34.dp else 38.dp)
                                    .clip(RoundedCornerShape(if (isCompactHeight) 10.dp else 12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(if (isCompactHeight) 15.dp else 16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "بحث بالعميل أو رقم السند...",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.setSearchQuery(it) },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("operations_search_input")
                                        )
                                    }
                                    VoiceFieldMic(onClick = {
                                        showSearchVoiceSheet = true
                                    })
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setSearchQuery("") },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // زر طباعة سجل العمليات كتقرير PDF
                            Box(
                                modifier = Modifier
                                    .size(if (isCompactHeight) 34.dp else 38.dp)
                                    .clip(RoundedCornerShape(if (isCompactHeight) 10.dp else 12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable { viewModel.exportOperationsLogPdf(context) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "طباعة سجل العمليات PDF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(if (isCompactHeight) 18.dp else 20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 6.dp))

                        // صف شرائح الفلاتر المنسدلة (الترتيب، النوع، نوع الحساب، الحالة، المجموعة، الفترة، العميل)
                        OperationsFilterChipsRow(
                            filterState = filterState,
                            customers = customers,
                            onSelectSort = { viewModel.updateFilter { s -> s.copy(sort = it) } },
                            onSelectType = { viewModel.updateFilter { s -> s.copy(type = it) } },
                            onSelectAccountType = { viewModel.updateFilter { s -> s.copy(accountType = it) } },
                            onSelectPayment = { viewModel.updateFilter { s -> s.copy(sessionPayment = it) } },
                            onSelectGroup = { viewModel.updateFilter { s -> s.copy(group = it) } },
                            onSelectPeriod = { viewModel.updateFilter { s -> s.copy(period = it, customRange = null) } },
                            onRequestCustomRange = { showDateRangeDialog = true },
                            onSelectCustomer = { viewModel.updateFilter { s -> s.copy(customerId = it) } },
                            onRequestPickCustomer = { showCustomerPickerDialog = true },
                            onClearAll = { viewModel.clearFilters() }
                        )
                    }
                }
            }

            // Merged Integrated Financial Summary Card (يتلاشى بسلاسة تامة مع التمرير دون قفز)
            item {
                val wellOwnerIds = remember(customers) { customers.filter { it.isWellOwner }.map { it.id }.toSet() }
                val totalReceipts = summaryOps.filterIsInstance<UnifiedOperation.VoucherOp>()
                    .filter { it.voucherWithCustomer.voucher.type == VoucherType.RECEIPT }.sumOf { it.voucherWithCustomer.voucher.amount } +
                    summaryOps.filterIsInstance<UnifiedOperation.SessionOp>().sumOf { it.sessionWithCustomer.session.amountPaid }
                val totalExpenses = summaryOps.filterIsInstance<UnifiedOperation.VoucherOp>()
                    .filter { voucherOp ->
                        val v = voucherOp.voucherWithCustomer.voucher
                        v.type == VoucherType.EXPENSE && (v.customerId == null || wellOwnerIds.contains(v.customerId))
                    }.sumOf { it.voucherWithCustomer.voucher.amount }
                val netProfit = totalReceipts - totalExpenses
                val totalSoldWaterMinutes = summaryOps.filterIsInstance<UnifiedOperation.SessionOp>()
                    .sumOf {
                        val s = it.sessionWithCustomer.session
                        (s.durationMinutes - s.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
                    }

                val customersMap = remember(customers) { customers.associateBy { it.id } }
                val customerName = filterState.customerId?.let { customersMap[it]?.name }
                val filterDesc = remember(filterState, customerName) {
                    val stateWithoutType = filterState.copy(type = OperationsFilter.ALL)
                    val d = OperationsFilterEngine.describe(stateWithoutType, customerName)
                    if (d.isBlank()) "ملخص الفترة: كل الوقت" else "ملخص الفترة: $d"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .graphicsLayer {
                            alpha = summaryScrollAlpha
                            scaleY = 0.94f + (0.06f * summaryScrollAlpha)
                            scaleX = 0.96f + (0.04f * summaryScrollAlpha)
                        }
                        .shadow((3 * summaryScrollAlpha).dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = filterDesc,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val currencySymbol = config.currencySymbol
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryTile("دخل (قبض)", totalReceipts, currencySymbol, AccentEmerald, Modifier.weight(1f).fillMaxHeight())
                            SummaryTile("خرج (صرف)", totalExpenses, currencySymbol, MaterialTheme.mosarib.debt, Modifier.weight(1f).fillMaxHeight())
                            SummaryTile("الباقي في الصندوق", netProfit, currencySymbol, PrimaryTeal, Modifier.weight(1f).fillMaxHeight())
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ساعات السقي: " + SpeechTextBuilder.formatDurationWords(totalSoldWaterMinutes),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        // Same numbers as the Home totals box: ledgerTotalsFor over all customers.
                        val (owedByUs, owedToUs) = remember(allBalances) { ledgerTotalsFor(allBalances) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AmountTooltip(Formatters.amountToArabicWordsApprox(owedToUs, currencySymbol), Modifier.weight(1f)) {
                                Text(
                                    text = "لك عند العملاء: " + Formatters.formatCurrency(owedToUs, currencySymbol),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.mosarib.credit)
                                )
                            }
                            AmountTooltip(Formatters.amountToArabicWordsApprox(owedByUs, currencySymbol), Modifier.weight(1f)) {
                                Text(
                                    text = "عليك: " + Formatters.formatCurrency(owedByUs, currencySymbol),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.mosarib.debt)
                                )
                            }
                        }
                        Text(
                            text = "أرصدة حالية لا تتأثر بالفترة",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                    }
                }
            }

            if (operations.isEmpty()) {
                item {
                    val hasFilters = filterState.activeCount > 0 || searchQuery.isNotBlank()
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmptyStateView(
                            icon = Icons.Default.Receipt,
                            title = if (hasFilters) "لا توجد عمليات مطابقة للفلاتر" else "لا توجد عمليات مسجلة",
                            description = if (hasFilters) "جرّب تغيير أو مسح الفلاتر لعرض العمليات" else "قم بإضافة عملية جديدة من الزر العائم (+)"
                        )
                        if (hasFilters) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    viewModel.clearFilters()
                                    viewModel.setSearchQuery("")
                                }
                            ) {
                                Text(
                                    text = "مسح الفلاتر",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else {
                dayGroups.forEach { group ->
                    item(key = group.dayKey) {
                        Row(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 14.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.headerTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = group.operationsCountText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(
                        items = group.items,
                        key = { groupItem ->
                            when (val op = groupItem.item) {
                                is UnifiedOperation.SessionOp -> "s_${op.sessionWithCustomer.session.id}"
                                is UnifiedOperation.VoucherOp -> "v_${op.voucherWithCustomer.voucher.id}"
                                is UnifiedOperation.PurchaseOp -> "p_${op.purchase.id}"
                            }
                        }
                    ) { groupItem ->
                        when (val op = groupItem.item) {
                            is UnifiedOperation.SessionOp -> {
                                val session = op.sessionWithCustomer.session
                                val customer = op.sessionWithCustomer.customer
                                val title = customer?.name ?: "عميل غير محدد"
                                val badges = listOf(CardBadge("سقي", MaterialTheme.mosarib.info))
                                val amountText = Formatters.formatCurrency(session.totalAmount, config.currencySymbol)
                                val amountInWords = Formatters.amountToArabicWords(session.totalAmount, config.currencySymbol)

                                val chips = buildList {
                                    if (session.durationMinutes > 0) {
                                        add(
                                            CardChip(
                                                text = Formatters.formatDurationCompact(session.durationMinutes),
                                                container = MaterialTheme.mosarib.info.copy(alpha = 0.12f),
                                                content = MaterialTheme.mosarib.info,
                                                icon = Icons.Default.Schedule
                                            )
                                        )
                                    }
                                    if (session.pricePerHour > 0.0) {
                                        add(
                                            CardChip(
                                                text = "${Formatters.formatNumber(session.pricePerHour)}/س",
                                                container = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                content = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                    when {
                                        session.remainingDebt <= 0.001 -> add(
                                            CardChip(
                                                text = "مسدد",
                                                container = MaterialTheme.mosarib.successContainer,
                                                content = MaterialTheme.mosarib.success,
                                                isEndAligned = true
                                            )
                                        )
                                        session.amountPaid <= 0.001 -> add(
                                            CardChip(
                                                text = "غير مسدد",
                                                container = MaterialTheme.mosarib.debt.copy(alpha = 0.12f),
                                                content = MaterialTheme.mosarib.debt,
                                                isEndAligned = true
                                            )
                                        )
                                        else -> add(
                                            CardChip(
                                                text = "باقي ${Formatters.formatNumber(session.remainingDebt)}",
                                                container = MaterialTheme.mosarib.warningContainer,
                                                content = MaterialTheme.mosarib.warning,
                                                isEndAligned = true
                                            )
                                        )
                                    }
                                }

                                val statement = session.notes.trim().takeIf { it.isNotBlank() && it != "عام" }
                                val pumpPart = session.pumpName.trim().takeIf { it.isNotBlank() } ?: "مضخة"
                                val meta = listOfNotNull(Formatters.formatTime(session.startTime), pumpPart, "#SES-${session.id}").joinToString(" • ")

                                var sessionMenuExpanded by remember(session.id) { mutableStateOf(false) }
                                var isExpanded by remember(session.id) { mutableStateOf(false) }
                                val hasLinkedPayments = op.linkedVouchers.isNotEmpty() || session.amountPaid > 0.001

                                OperationCard(
                                    modifier = Modifier.animateItem(),
                                    accent = MaterialTheme.mosarib.info,
                                    icon = Icons.Default.WaterDrop,
                                    title = title,
                                    badges = badges,
                                    amountText = amountText,
                                    amountInWords = amountInWords,
                                    chips = chips,
                                    statement = statement,
                                    meta = meta,
                                    speakText = {
                                        SpeechTextBuilder.buildSessionSentence(
                                            session = session,
                                            customerName = customer?.name ?: "عميل غير محدد",
                                            currency = config.currencySymbol
                                        )
                                    },
                                    onClick = { sessionForDetails = op.sessionWithCustomer },
                                    actions = {
                                        if (hasLinkedPayments) {
                                            IconButton(
                                                onClick = { isExpanded = !isExpanded },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "تفاصيل السداد",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    menu = {
                                        Box {
                                            IconButton(
                                                onClick = { sessionMenuExpanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "خيارات",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            MosaribMenu(
                                                expanded = sessionMenuExpanded,
                                                onDismissRequest = { sessionMenuExpanded = false }
                                            ) {
                                                if (customer != null) {
                                                    MosaribMenuItem(
                                                        text = "فتح حساب العميل",
                                                        icon = Icons.Default.Person,
                                                        onClick = {
                                                            sessionMenuExpanded = false
                                                            onNavigateToCustomer(customer.id)
                                                        }
                                                    )
                                                }
                                                if (customer != null && customer.phone.isNotEmpty()) {
                                                    MosaribMenuItem(
                                                        text = "إرسال الفاتورة",
                                                        icon = Icons.AutoMirrored.Filled.Send,
                                                        tint = MaterialTheme.mosarib.success,
                                                        onClick = {
                                                            sessionMenuExpanded = false
                                                            val waMsg = MessageFormatHelper.buildSessionBill(
                                                                session = session,
                                                                customer = customer,
                                                                config = config,
                                                                isSms = false,
                                                                sponsorCustomer = op.sessionWithCustomer.billedToCustomer,
                                                                beneficiaryCustomer = customer
                                                            )
                                                            val smsMsg = MessageFormatHelper.buildSessionBill(
                                                                session = session,
                                                                customer = customer,
                                                                config = config,
                                                                isSms = true,
                                                                sponsorCustomer = op.sessionWithCustomer.billedToCustomer,
                                                                beneficiaryCustomer = customer
                                                            )
                                                            voucherMessageTarget = Triple(customer, waMsg, smsMsg)
                                                        }
                                                    )
                                                }
                                                MosaribMenuItem(
                                                    text = "تصدير PDF",
                                                    icon = Icons.Default.PictureAsPdf,
                                                    onClick = {
                                                        sessionMenuExpanded = false
                                                        val file = PdfReportGenerator.generateSessionInvoicePdf(
                                                            context = context,
                                                            config = config,
                                                            customer = customer ?: Customer(name = "عميل غير محدد"),
                                                            session = session,
                                                            sponsorCustomer = op.sessionWithCustomer.billedToCustomer
                                                        )
                                                        voucherPdfReady = Pair(file, "فاتورة ري #${session.id}")
                                                    }
                                                )
                                                MosaribMenuItem(
                                                    text = "تعديل",
                                                    icon = Icons.Default.Edit,
                                                    onClick = {
                                                        sessionMenuExpanded = false
                                                        sessionToEdit = session
                                                    }
                                                )
                                                MosaribMenuItem(
                                                    text = "قراءة العملية",
                                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                                    onClick = {
                                                        sessionMenuExpanded = false
                                                        scope.launch {
                                                            val status = MosaribSpeaker.prepare(context)
                                                            if (status == SpeechStatus.Ready) {
                                                                val speechText = SpeechTextBuilder.buildSessionSentence(
                                                                    session = session,
                                                                    customerName = customer?.name ?: "عميل غير محدد",
                                                                    currency = config.currencySymbol
                                                                )
                                                                MosaribSpeaker.speak(speechText)
                                                            } else {
                                                                missingSpeechStatus = status
                                                            }
                                                        }
                                                    }
                                                )
                                                MosaribMenuDivider()
                                                MosaribMenuItem(
                                                    text = "حذف",
                                                    icon = Icons.Default.Delete,
                                                    destructive = true,
                                                    onClick = {
                                                        sessionMenuExpanded = false
                                                        sessionToDelete = session
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    footer = if (isExpanded && hasLinkedPayments) {
                                        {
                                            SessionPaymentHistoryExpander(
                                                session = session,
                                                linkedVouchers = op.linkedVouchers,
                                                debtToShow = session.remainingDebt,
                                                currencySymbol = config.currencySymbol,
                                                onShareVoucher = { v ->
                                                    val f = PdfReportGenerator.generateReceiptVoucherPdf(context, config, customer ?: Customer(name = "عميل نقدي"), v)
                                                    voucherPdfReady = Pair(f, "سند قبض #${v.voucherNumber}")
                                                }
                                            )
                                        }
                                    } else null
                                )
                            }
                            is UnifiedOperation.VoucherOp -> {
                                val voucher = op.voucherWithCustomer.voucher
                                val customer = op.voucherWithCustomer.customer
                                val isReceipt = voucher.type == VoucherType.RECEIPT
                                val isExpense = voucher.type == VoucherType.EXPENSE
                                val isDiscount = voucher.type == VoucherType.DISCOUNT

                                val accent = when {
                                    isReceipt -> MaterialTheme.mosarib.success
                                    isExpense -> MaterialTheme.mosarib.debt
                                    else -> MaterialTheme.mosarib.discount
                                }

                                val icon = when {
                                    isReceipt -> Icons.Default.Payments
                                    isExpense -> Icons.Default.ArrowUpward
                                    else -> Icons.Default.Bookmarks
                                }

                                val title = when {
                                    isReceipt -> customer?.name ?: "عميل نقدي"
                                    isExpense -> when {
                                        customer != null -> if (customer.isWellOwner) "صاحب بئر: ${customer.name}" else customer.name
                                        voucher.category.isNotBlank() -> voucher.category
                                        else -> "مصروف عام"
                                    }
                                    else -> customer?.name ?: "عميل غير محدد"
                                }

                                val badges = when {
                                    isReceipt -> {
                                        val b = if (voucher.sessionId != null && voucher.sessionId > 0) "سداد سقي" else "سند قبض"
                                        listOf(CardBadge(b, accent))
                                    }
                                    isExpense -> listOf(CardBadge("سند صرف", accent))
                                    else -> listOf(CardBadge("خصم", accent))
                                }

                                val amountPrefix = when {
                                    isReceipt -> "+ "
                                    isExpense -> "- "
                                    else -> "- "
                                }
                                val amountText = amountPrefix + Formatters.formatCurrency(voucher.amount, config.currencySymbol)
                                val amountInWords = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)

                                val statement = voucher.notes.trim().takeIf { it.isNotBlank() && it != "عام" }

                                val rawRef = voucher.voucherNumber.trim().removePrefix("#")
                                val cleanRef = when {
                                    isReceipt -> if (rawRef.startsWith("REC-", ignoreCase = true)) "#$rawRef" else "#REC-$rawRef"
                                    isExpense -> if (rawRef.startsWith("EXP-", ignoreCase = true)) "#$rawRef" else "#EXP-$rawRef"
                                    else -> if (rawRef.startsWith("DSC-", ignoreCase = true)) "#$rawRef" else "#DSC-$rawRef"
                                }
                                val methodPart = voucher.paymentMethod.trim().takeIf { it.isNotBlank() } ?: if (isDiscount) "تسوية" else "نقداً"
                                val meta = listOfNotNull(Formatters.formatTime(voucher.date), methodPart, cleanRef).joinToString(" • ")

                                var voucherMenuExpanded by remember(voucher.id) { mutableStateOf(false) }

                                OperationCard(
                                    modifier = Modifier.animateItem(),
                                    accent = accent,
                                    icon = icon,
                                    title = title,
                                    badges = badges,
                                    amountText = amountText,
                                    amountInWords = amountInWords,
                                    statement = statement,
                                    meta = meta,
                                    speakText = {
                                        when (voucher.type) {
                                            VoucherType.RECEIPT -> SpeechTextBuilder.buildReceiptSentence(customer?.name ?: "عميل غير محدد", voucher.amount, Date(voucher.date), config.currencySymbol)
                                            VoucherType.EXPENSE -> SpeechTextBuilder.buildExpenseSentence(customer?.name ?: voucher.notes.ifBlank { voucher.category }, voucher.amount, Date(voucher.date), config.currencySymbol)
                                            VoucherType.DISCOUNT -> SpeechTextBuilder.buildDiscountSentence(customer?.name ?: "عميل غير محدد", voucher.amount, currency = config.currencySymbol)
                                        }
                                    },
                                    onClick = { voucherForDetails = op.voucherWithCustomer },
                                    menu = {
                                        Box {
                                            IconButton(
                                                onClick = { voucherMenuExpanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "خيارات",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            MosaribMenu(
                                                expanded = voucherMenuExpanded,
                                                onDismissRequest = { voucherMenuExpanded = false }
                                            ) {
                                                if (customer != null) {
                                                    MosaribMenuItem(
                                                        text = "فتح حساب العميل",
                                                        icon = Icons.Default.Person,
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            onNavigateToCustomer(customer.id)
                                                        }
                                                    )
                                                }
                                                if (customer != null && customer.phone.isNotEmpty()) {
                                                    MosaribMenuItem(
                                                        text = "مشاركة السند (واتساب / SMS)",
                                                        icon = Icons.AutoMirrored.Filled.Send,
                                                        tint = MaterialTheme.mosarib.success,
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            val waMsg = if (isReceipt) {
                                                                MessageFormatHelper.buildReceiptVoucher(voucher, customer, null, config, isSms = false)
                                                            } else {
                                                                MessageFormatHelper.buildDisbursementVoucher(voucher, customer, config, isSms = false)
                                                            }
                                                            val smsMsg = if (isReceipt) {
                                                                MessageFormatHelper.buildReceiptVoucher(voucher, customer, null, config, isSms = true)
                                                            } else {
                                                                MessageFormatHelper.buildDisbursementVoucher(voucher, customer, config, isSms = true)
                                                            }
                                                            voucherMessageTarget = Triple(customer, waMsg, smsMsg)
                                                        }
                                                    )
                                                }
                                                MosaribMenuItem(
                                                    text = "طباعة سند PDF",
                                                    icon = Icons.Default.PictureAsPdf,
                                                    onClick = {
                                                        voucherMenuExpanded = false
                                                        val file = if (isReceipt) {
                                                            PdfReportGenerator.generateReceiptVoucherPdf(
                                                                context,
                                                                config,
                                                                customer ?: Customer(name = "عميل نقدي"),
                                                                voucher
                                                            )
                                                        } else {
                                                            PdfReportGenerator.generateExpenseVoucherPdf(
                                                                context,
                                                                config,
                                                                customer,
                                                                voucher
                                                            )
                                                        }
                                                        voucherPdfReady = Pair(file, if (isReceipt) "سند قبض #${voucher.voucherNumber}" else "سند صرف #${voucher.voucherNumber}")
                                                    }
                                                )
                                                MosaribMenuItem(
                                                    text = "تعديل السند",
                                                    icon = Icons.Default.Edit,
                                                    onClick = {
                                                        voucherMenuExpanded = false
                                                        voucherToEdit = voucher
                                                    }
                                                )
                                                MosaribMenuItem(
                                                    text = "قراءة العملية",
                                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                                    onClick = {
                                                        voucherMenuExpanded = false
                                                        scope.launch {
                                                            val status = MosaribSpeaker.prepare(context)
                                                            if (status == SpeechStatus.Ready) {
                                                                val speechText = when (voucher.type) {
                                                                    VoucherType.RECEIPT -> SpeechTextBuilder.buildReceiptSentence(
                                                                        customerName = customer?.name ?: "عميل غير محدد",
                                                                        amount = voucher.amount,
                                                                        date = Date(voucher.date),
                                                                        currency = config.currencySymbol
                                                                    )
                                                                    VoucherType.EXPENSE -> SpeechTextBuilder.buildExpenseSentence(
                                                                        nameOrCategory = customer?.name ?: voucher.notes.ifBlank { voucher.category },
                                                                        amount = voucher.amount,
                                                                        date = Date(voucher.date),
                                                                        currency = config.currencySymbol
                                                                    )
                                                                    VoucherType.DISCOUNT -> SpeechTextBuilder.buildDiscountSentence(
                                                                        customerName = customer?.name ?: "عميل غير محدد",
                                                                        amount = voucher.amount,
                                                                        currency = config.currencySymbol
                                                                    )
                                                                }
                                                                MosaribSpeaker.speak(speechText)
                                                            } else {
                                                                missingSpeechStatus = status
                                                            }
                                                        }
                                                    }
                                                )
                                                MosaribMenuDivider()
                                                MosaribMenuItem(
                                                    text = "حذف السند",
                                                    icon = Icons.Default.Delete,
                                                    destructive = true,
                                                    onClick = {
                                                        voucherMenuExpanded = false
                                                        voucherToDelete = voucher
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            is UnifiedOperation.PurchaseOp -> {
                                val purchase = op.purchase
                                val owner = op.owner
                                val totalAmount = WellOwnerPurchaseMath.payableAmount(purchase)
                                val amountText = Formatters.formatCurrency(totalAmount, config.currencySymbol)
                                val amountInWords = Formatters.amountToArabicWords(totalAmount, config.currencySymbol)

                                val chips = buildList {
                                    val duration = WellOwnerPurchaseMath.chargeableMinutes(purchase)
                                    if (duration > 0) {
                                        add(
                                            CardChip(
                                                text = Formatters.formatDurationCompact(duration),
                                                container = MaterialTheme.mosarib.warning.copy(alpha = 0.12f),
                                                content = MaterialTheme.mosarib.warning,
                                                isEndAligned = true,
                                                icon = Icons.Default.AccessTime
                                            )
                                        )
                                    }
                                }

                                val statement = purchase.notes.trim().takeIf { it.isNotBlank() }
                                val meta = listOfNotNull(Formatters.formatTime(purchase.date), "#PUR-${purchase.id}").joinToString(" • ")

                                var purchaseMenuExpanded by remember(purchase.id) { mutableStateOf(false) }

                                OperationCard(
                                    modifier = Modifier.animateItem(),
                                    accent = MaterialTheme.mosarib.warning,
                                    icon = Icons.Default.AccessTime,
                                    title = owner?.name ?: "صاحب بئر #${purchase.ownerCustomerId}",
                                    badges = listOf(CardBadge("شراء ساعات", MaterialTheme.mosarib.warning)),
                                    amountText = amountText,
                                    amountInWords = amountInWords,
                                    chips = chips,
                                    statement = statement,
                                    meta = meta,
                                    speakText = {
                                        SpeechTextBuilder.buildPurchaseSentence(
                                            ownerName = owner?.name ?: "صاحب البئر",
                                            durationMinutes = purchase.durationMinutes,
                                            amount = WellOwnerPurchaseMath.payableAmount(purchase),
                                            currency = config.currencySymbol,
                                            ratePerHour = purchase.purchaseRatePerHour
                                        )
                                    },
                                    onClick = { purchaseForDetails = Pair(purchase, owner) },
                                    menu = {
                                        Box {
                                            IconButton(
                                                onClick = { purchaseMenuExpanded = true },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.MoreVert,
                                                    contentDescription = "خيارات",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            MosaribMenu(
                                                expanded = purchaseMenuExpanded,
                                                onDismissRequest = { purchaseMenuExpanded = false }
                                            ) {
                                                MosaribMenuItem(
                                                    text = "فتح حساب العميل",
                                                    icon = Icons.Default.Person,
                                                    onClick = {
                                                        purchaseMenuExpanded = false
                                                        onNavigateToCustomer(purchase.ownerCustomerId)
                                                    }
                                                )
                                                if (owner != null && owner.phone.isNotBlank()) {
                                                    MosaribMenuItem(
                                                        text = "مشاركة فاتورة الشراء",
                                                        icon = Icons.AutoMirrored.Filled.Send,
                                                        tint = MaterialTheme.mosarib.success,
                                                        onClick = {
                                                            purchaseMenuExpanded = false
                                                            val waMsg = MessageFormatHelper.buildPurchaseBill(purchase, owner, config, isSms = false)
                                                            val smsMsg = MessageFormatHelper.buildPurchaseBill(purchase, owner, config, isSms = true)
                                                            voucherMessageTarget = Triple(owner, waMsg, smsMsg)
                                                        }
                                                    )
                                                }
                                                MosaribMenuItem(
                                                    text = "تعديل الشراء",
                                                    icon = Icons.Default.Edit,
                                                    onClick = {
                                                        purchaseMenuExpanded = false
                                                        purchaseToEdit = purchase
                                                    }
                                                )
                                                MosaribMenuItem(
                                                    text = "قراءة العملية",
                                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                                    onClick = {
                                                        purchaseMenuExpanded = false
                                                        scope.launch {
                                                            val status = MosaribSpeaker.prepare(context)
                                                            if (status == SpeechStatus.Ready) {
                                                                val speechText = SpeechTextBuilder.buildPurchaseSentence(
                                                                    ownerName = owner?.name ?: "صاحب البئر",
                                                                    durationMinutes = purchase.durationMinutes,
                                                                    amount = WellOwnerPurchaseMath.payableAmount(purchase),
                                                                    currency = config.currencySymbol,
                                                                    ratePerHour = purchase.purchaseRatePerHour
                                                                )
                                                                MosaribSpeaker.speak(speechText)
                                                            } else {
                                                                missingSpeechStatus = status
                                                            }
                                                        }
                                                    }
                                                )
                                                MosaribMenuDivider()
                                                MosaribMenuItem(
                                                    text = "حذف الشراء",
                                                    icon = Icons.Default.Delete,
                                                    destructive = true,
                                                    onClick = {
                                                        purchaseMenuExpanded = false
                                                        purchaseToDelete = purchase
                                                    }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB with MosaribMenu
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = if (isCompactHeight) 10.dp else 16.dp)
        ) {
            val fabRotation by animateFloatAsState(
                targetValue = if (speedDialOpen) 45f else 0f,
                animationSpec = tween(200),
                label = "fab_rotation"
            )

            FloatingActionButton(
                onClick = { speedDialOpen = !speedDialOpen },
                modifier = Modifier
                    .size(if (isCompactHeight) 44.dp else 48.dp)
                    .testTag("fab_add_operation"),
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة عملية",
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = fabRotation }
                )
            }

            MosaribMenu(
                expanded = speedDialOpen,
                onDismissRequest = { speedDialOpen = false }
            ) {
                MosaribMenuItem(
                    text = "دورة سقي",
                    icon = Icons.Default.WaterDrop,
                    onClick = {
                        speedDialOpen = false
                        gate { showAddManualSheet = true }
                    }
                )
                MosaribMenuItem(
                    text = "سند (قبض / صرف)",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    onClick = {
                        speedDialOpen = false
                        gate { showAddSheet = true }
                    }
                )
                MosaribMenuItem(
                    text = "شراء ساعات (من بئر)",
                    icon = Icons.Default.WaterDrop,
                    onClick = {
                        speedDialOpen = false
                        gate { showAddPurchaseSheet = true }
                    }
                )
            }
        }

        val haptic = LocalHapticFeedback.current
        LaunchedEffect(toast) {
            if (toast?.type == ToastType.SUCCESS) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        )
    }

    // Floating Detail BottomSheet for Water Session
    sessionForDetails?.let { item ->
        val s = item.session
        val c = item.customer
        val linked = allVouchers.filter { it.sessionId == s.id }
        SessionDetailBottomSheet(
            session = s,
            customer = c,
            currencySymbol = config.currencySymbol,
            linkedVouchers = linked,
            isWellOwner = c?.isWellOwner == true,
            sponsorCustomer = item.billedToCustomer,
            onDismiss = { sessionForDetails = null },
            onEdit = {
                sessionForDetails = null
                sessionToEdit = s
            },
            onDelete = {
                sessionForDetails = null
                sessionToDelete = s
            },
            onPdf = {
                sessionForDetails = null
                val file = PdfReportGenerator.generateSessionInvoicePdf(
                    context = context,
                    config = config,
                    customer = c ?: Customer(name = "عميل غير محدد"),
                    session = s,
                    sponsorCustomer = item.billedToCustomer
                )
                voucherPdfReady = Pair(file, "فاتورة ري #${s.id}")
            },
            onMessage = {
                sessionForDetails = null
                if (c != null) {
                    val waMsg = MessageFormatHelper.buildSessionBill(
                        session = s,
                        customer = c,
                        config = config,
                        isSms = false,
                        sponsorCustomer = item.billedToCustomer,
                        beneficiaryCustomer = c
                    )
                    val smsMsg = MessageFormatHelper.buildSessionBill(
                        session = s,
                        customer = c,
                        config = config,
                        isSms = true,
                        sponsorCustomer = item.billedToCustomer,
                        beneficiaryCustomer = c
                    )
                    voucherMessageTarget = Triple(c, waMsg, smsMsg)
                }
            },
            onShareVoucher = { v ->
                if (c != null) {
                    val waMsg = MessageFormatHelper.buildReceiptVoucher(v, c, s.id, config, isSms = false)
                    val smsMsg = MessageFormatHelper.buildReceiptVoucher(v, c, s.id, config, isSms = true)
                    voucherMessageTarget = Triple(c, waMsg, smsMsg)
                }
            }
        )
    }

    // Floating Detail BottomSheet for Voucher
    voucherForDetails?.let { vWithCust ->
        val v = vWithCust.voucher
        val c = vWithCust.customer
        VoucherDetailBottomSheet(
            voucher = v,
            customer = c,
            currencySymbol = config.currencySymbol,
            onDismiss = { voucherForDetails = null },
            onEdit = {
                voucherForDetails = null
                voucherToEdit = v
            },
            onDelete = {
                voucherForDetails = null
                voucherToDelete = v
            },
            onPdf = {
                voucherForDetails = null
                val file = if (v.type == VoucherType.RECEIPT) {
                    PdfReportGenerator.generateReceiptVoucherPdf(
                        context = context,
                        config = config,
                        customer = c ?: Customer(name = "عميل نقدي"),
                        voucher = v
                    )
                } else {
                    PdfReportGenerator.generateExpenseVoucherPdf(
                        context = context,
                        config = config,
                        customer = c ?: Customer(name = "مصروف عام"),
                        voucher = v
                    )
                }
                voucherPdfReady = Pair(file, "سند #${v.voucherNumber.ifEmpty { v.id.toString() }}")
            },
            onShare = {
                voucherForDetails = null
                if (c != null) {
                    val waMsg = if (v.type == VoucherType.RECEIPT) {
                        MessageFormatHelper.buildReceiptVoucher(v, c, v.sessionId ?: 0L, config, isSms = false)
                    } else {
                        MessageFormatHelper.buildDisbursementVoucher(v, c, config, isSms = false)
                    }
                    val smsMsg = if (v.type == VoucherType.RECEIPT) {
                        MessageFormatHelper.buildReceiptVoucher(v, c, v.sessionId ?: 0L, config, isSms = true)
                    } else {
                        MessageFormatHelper.buildDisbursementVoucher(v, c, config, isSms = true)
                    }
                    voucherMessageTarget = Triple(c, waMsg, smsMsg)
                } else {
                    val shareText = if (v.type == VoucherType.RECEIPT) {
                        "سند قبض #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالمبلغ: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nطريقة الدفع: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                    } else {
                        "سند صرف #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالمبلغ المصروف: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nالبيان: ${v.notes.ifBlank { v.category }}\nطريقة الصرف: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                    }
                    FileSharingHelper.shareText(context, shareText, "مشاركة السند")
                }
            }
        )
    }

    // Floating Detail BottomSheet for WellOwnerPurchase
    purchaseForDetails?.let { (p, o) ->
        WellOwnerPurchaseDetailBottomSheet(
            purchase = p,
            owner = o,
            currencySymbol = config.currencySymbol,
            onDismiss = { purchaseForDetails = null },
            onEdit = {
                purchaseForDetails = null
                purchaseToEdit = p
            },
            onDelete = {
                purchaseForDetails = null
                purchaseToDelete = p
            },
            onShare = {
                purchaseForDetails = null
                val targetOwner = o ?: customers.find { it.id == p.ownerCustomerId }
                if (targetOwner != null && targetOwner.phone.isNotBlank()) {
                    val waMsg = MessageFormatHelper.buildPurchaseBill(p, targetOwner, config, isSms = false)
                    val smsMsg = MessageFormatHelper.buildPurchaseBill(p, targetOwner, config, isSms = true)
                    voucherMessageTarget = Triple(targetOwner, waMsg, smsMsg)
                } else {
                    val fallbackOwner = targetOwner ?: Customer(name = "صاحب بئر #${p.ownerCustomerId}")
                    val shareText = MessageFormatHelper.buildPurchaseBill(p, fallbackOwner, config, isSms = false)
                    FileSharingHelper.shareText(context, shareText, "مشاركة فاتورة شراء الساعات")
                }
            }
        )
    }

    if (showAddSheet) {
        AddVoucherBottomSheet(
            customers = customers,
            allSessions = allSessions,
            frequentDescriptions = frequentDescriptions,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = sessionsViewModel?.let { vm -> { customer -> vm.createCustomer(customer) } },
            onDismiss = { showAddSheet = false },
            onSave = { type, custId, amt, cat, method, notes, sessionId, selectedSessionIds, disc ->
                viewModel.addVoucher(type, custId, amt, cat, method, notes, sessionId, selectedSessionIds, disc)
            }
        )
    }

    if (showAddManualSheet && sessionsViewModel != null) {
        AddEditSessionBottomSheet(
            initialSession = null,
            customers = customers,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel.createCustomer(customer) },
            onDismiss = { showAddManualSheet = false },
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

    // تعديل دورة سقي من سجل العمليات
    sessionToEdit?.let { s ->
        AddEditSessionBottomSheet(
            initialSession = s,
            customers = customers,
            defaultCustomerId = s.customerId,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> sessionsViewModel?.createCustomer(customer) ?: 0L },
            onDismiss = { sessionToEdit = null },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes, billedTo, wastedMins, wastedReason, discount, costRate, pumpId ->
                if (sessionsViewModel != null) {
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
                } else {
                    viewModel.updateSession(
                        id = id,
                        customerId = custId,
                        pumpName = pumpName,
                        startTime = startTime,
                        endTime = endTime,
                        durationMinutes = (hrs * 60) + mins,
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
                sessionToEdit = null
            }
        )
    }

    // حذف دورة سقي من سجل العمليات
    sessionToDelete?.let { s ->
        com.example.core.ui.MosaribActionConfirmDialog(
            title = "تأكيد حذف دورة السقي",
            message = "هل تريد بالتأكيد حذف دورة السقي #${s.id}؟ سيتم فك ارتباط أي سندات قبض مسددة لها وتحويلها لدفعات على الحساب.",
            confirmText = "حذف الدورة",
            onConfirm = {
                if (sessionsViewModel != null) {
                    sessionsViewModel.deleteSession(s)
                } else {
                    viewModel.deleteSession(s)
                }
                sessionToDelete = null
            },
            onDismiss = { sessionToDelete = null }
        )
    }

    // تعديل سند قبض أو صرف من سجل العمليات
    voucherToEdit?.let { v ->
        AddVoucherBottomSheet(
            initialVoucher = v,
            customers = customers,
            allSessions = allSessions,
            frequentDescriptions = frequentDescriptions,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = sessionsViewModel?.let { vm -> { customer -> vm.createCustomer(customer) } },
            onDismiss = { voucherToEdit = null },
            onSave = { type, custId, amt, cat, method, notes, sessionId, _, _ ->
                viewModel.updateVoucher(
                    voucher = v,
                    amount = amt,
                    paymentMethod = method,
                    notes = notes,
                    category = cat,
                    customerId = custId,
                    type = type
                )
                voucherToEdit = null
            }
        )
    }

    // حوار اختيار قناة الإرسال (واتساب أو SMS)
    voucherMessageTarget?.let { (cust, waMsg, smsMsg) ->
        SendMessageChoiceDialog(
            recipientName = cust.name,
            recipientPhone = cust.phone,
            messageText = waMsg,
            smsMessageText = smsMsg,
            onDismiss = { voucherMessageTarget = null },
            onSendWhatsApp = {
                voucherMessageTarget = null
                FileSharingHelper.sendWhatsAppMessage(context, cust.phone, waMsg)
            },
            onSendSms = {
                voucherMessageTarget = null
                FileSharingHelper.sendSms(context, cust.phone, smsMsg)
            }
        )
    }

    // حوار فتح ومشاركة ملف PDF
    voucherPdfReady?.let { (file, title) ->
        com.example.core.ui.MosaribFileActionDialog(
            file = file,
            title = title,
            fileType = "PDF",
            onOpen = {
                FileSharingHelper.openPdf(context, file)
                voucherPdfReady = null
            },
            onShare = {
                FileSharingHelper.sharePdf(context, file, title)
                voucherPdfReady = null
            },
            onDismiss = { voucherPdfReady = null }
        )
    }

    voucherToDelete?.let { v ->
        com.example.core.ui.MosaribActionConfirmDialog(
            title = "تأكيد حذف السند",
            message = "هل تريد بالتأكيد حذف هذا السند؟ سيتم إلغاء تأثيره المالي وتحديث الأرصدة فورياً.",
            confirmText = "حذف السند",
            onConfirm = {
                viewModel.deleteVoucher(v)
                voucherToDelete = null
            },
            onDismiss = { voucherToDelete = null }
        )
    }

    purchaseToDelete?.let { p ->
        com.example.core.ui.MosaribActionConfirmDialog(
            title = "تأكيد حذف عملية الشراء",
            message = "هل تريد بالتأكيد حذف عملية شراء الساعات #${p.id}؟ سيتم إلغاء تأثيرها المالي على رصيد صاحب البئر فورياً.",
            confirmText = "حذف العملية",
            onConfirm = {
                viewModel.deleteOwnerPurchase(p)
                purchaseToDelete = null
            },
            onDismiss = { purchaseToDelete = null }
        )
    }

    purchaseToEdit?.let { p ->
        val owner = customers.find { it.id == p.ownerCustomerId }
        WellOwnerPurchaseBottomSheet(
            owner = owner ?: Customer(id = p.ownerCustomerId, name = "صاحب بئر #${p.ownerCustomerId}", isWellOwner = true),
            currencySymbol = config.currencySymbol,
            initialPurchase = p,
            onDismiss = { purchaseToEdit = null },
            onSave = { date, durationMinutes, wastedMinutes, rate, amountPaid, notes, startTime, endTime ->
                viewModel.updateOwnerPurchase(
                    purchase = p,
                    date = date,
                    durationMinutes = durationMinutes,
                    wastedMinutes = wastedMinutes,
                    rate = rate,
                    amountPaid = amountPaid,
                    notes = notes,
                    startTime = startTime,
                    endTime = endTime
                )
                val targetOwner = customers.find { it.id == p.ownerCustomerId }
                if (targetOwner != null && targetOwner.phone.isNotBlank()) {
                    val updatedPurchase = p.copy(
                        date = date,
                        durationMinutes = durationMinutes,
                        wastedMinutesOnOwner = wastedMinutes,
                        purchaseRatePerHour = rate,
                        amountPaid = amountPaid,
                        notes = notes,
                        startTime = startTime,
                        endTime = endTime
                    )
                    val waMsg = MessageFormatHelper.buildPurchaseBill(updatedPurchase, targetOwner, config, isSms = false)
                    val smsMsg = MessageFormatHelper.buildPurchaseBill(updatedPurchase, targetOwner, config, isSms = true)
                    voucherMessageTarget = Triple(targetOwner, waMsg, smsMsg)
                }
                purchaseToEdit = null
            }
        )
    }

    if (showAddPurchaseSheet) {
        val wellOwners = customers.filter { it.isWellOwner }
        WellOwnerPurchaseBottomSheet(
            owner = null,
            availableOwners = wellOwners,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddPurchaseSheet = false },
            onSaveWithOwner = { ownerId, date, duration, wasted, rate, paid, notes, startTime, endTime ->
                viewModel.addOwnerPurchase(
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

    selectedLinkedSession?.let { s ->
        val customer = customers.find { it.id == s.customerId }
        MosaribDialog(
            onDismissRequest = { selectedLinkedSession = null },
            title = "تفاصيل دورة السقي #${s.id}",
            icon = Icons.Default.WaterDrop,
            tone = DialogTone.Info,
            message = customer?.name ?: "عميل غير محدد",
            confirmText = if (customer != null) "كشف حساب العميل" else "إغلاق",
            onConfirm = {
                val custId = s.customerId
                selectedLinkedSession = null
                if (customer != null) {
                    onNavigateToCustomer(custId)
                }
            },
            dismissText = if (customer != null) "إغلاق" else null,
            onDismiss = { selectedLinkedSession = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // الوقت والتاريخ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("البداية:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatDateTime(s.startTime), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("النهاية:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatDateTime(s.endTime), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // تفاصيل الساعات
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val totalRunMinutes = s.durationMinutes + s.wastedMinutes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي وقت التشغيل:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatDurationArabic(totalRunMinutes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        if (s.wastedMinutes > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("هدر وتوقفات (مخصوم):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.mosarib.debt)
                                Text(Formatters.formatDurationShort(s.wastedMinutes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.mosarib.debt)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الساعات المحتسبة:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(Formatters.formatDurationArabic(s.durationMinutes), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // الحساب المالي
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("سعر الساعة:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCurrency(s.pricePerHour, config.currencySymbol), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي القيمة:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(Formatters.formatCurrency(s.totalAmount, config.currencySymbol), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المسدد:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Formatters.formatCurrency(s.amountPaid, config.currencySymbol), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.mosarib.credit)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتبقي:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                Formatters.formatCurrency(s.remainingDebt, config.currencySymbol),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (s.remainingDebt > 0) MaterialTheme.mosarib.debt else MaterialTheme.mosarib.credit
                            )
                        }
                    }
                }

                if (s.notes.isNotBlank()) {
                    Text(
                        text = "ملاحظات: ${s.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showDateRangeDialog) {
        MosaribDateRangeDialog(
            initialStartTs = filterState.customRange?.first,
            initialEndTs = filterState.customRange?.second,
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { startTs, endTs ->
                viewModel.updateFilter { it.copy(period = ReportPeriod.CUSTOM, customRange = Pair(startTs, endTs)) }
                showDateRangeDialog = false
            }
        )
    }

    if (showCustomerPickerDialog) {
        CustomerSearchPickerDialog(
            customers = customers,
            onSelect = { customer ->
                viewModel.updateFilter { it.copy(customerId = customer.id) }
                showCustomerPickerDialog = false
            },
            onDismiss = { showCustomerPickerDialog = false }
        )
    }



    missingSpeechStatus?.let { status ->
        TtsMissingDataDialog(
            status = status,
            onDismiss = { missingSpeechStatus = null }
        )
    }

    speechError?.let { err ->
        SpeechErrorDialog(
            error = err,
            onDismiss = { speechError = null }
        )
    }

    if (showSearchVoiceSheet) {
        VoiceRecordSheet(
            title = "بحث صوتي في السندات",
            subtitle = "تحدث للبحث...",
            compact = true,
            onDismiss = { showSearchVoiceSheet = false },
            onResult = { candidates ->
                showSearchVoiceSheet = false
                val spoken = candidates.firstOrNull()?.trim().orEmpty()
                if (spoken.isNotBlank()) {
                    viewModel.setSearchQuery(spoken)
                }
            }
        )
    }

    VoiceSupportDialogs(
        dialogType = searchVoiceDialogType,
        onDismiss = { searchVoiceDialogType = VoiceDialogType.NONE }
    )
}

data class VoucherFormSnapshot(
    val type: VoucherType,
    val customerId: Long,
    val beneficiaryId: Long?,
    val amount: Double,
    val discount: Double,
    val paymentMethod: String,
    val notes: String,
    val selectedSessionIds: Set<Long>
) {
    companion object {
        fun from(
            type: VoucherType,
            customerId: Long,
            beneficiaryId: Long?,
            amountStr: String,
            discountStr: String,
            paymentMethod: String,
            notes: String,
            selectedSessionIds: Set<Long>
        ): VoucherFormSnapshot = VoucherFormSnapshot(
            type = type,
            customerId = customerId,
            beneficiaryId = beneficiaryId,
            amount = Formatters.parseAmountInput(amountStr),
            discount = Formatters.parseAmountInput(discountStr),
            paymentMethod = paymentMethod.trim(),
            notes = notes.trim(),
            selectedSessionIds = selectedSessionIds
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVoucherBottomSheet(
    initialVoucher: Voucher? = null,
    initialType: VoucherType = initialVoucher?.type ?: VoucherType.RECEIPT,
    customers: List<Customer>,
    allSessions: List<WaterSession> = emptyList(),
    frequentDescriptions: List<String> = emptyList(),
    currencySymbol: String,
    onDismiss: () -> Unit,
    voiceCommand: VoiceCommand? = null,
    onReSpeak: (() -> Unit)? = null,
    // Opened from a customer's own screen: the customer is fixed and the picker is hidden
    lockedCustomerId: Long? = null,
    // When null, «+ عميل جديد» is hidden
    onCreateCustomer: (suspend (Customer) -> Long)? = null,
    onSave: (
        type: VoucherType,
        customerId: Long?,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String,
        sessionId: Long?,
        selectedSessionIds: List<Long>,
        discountAmount: Double
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isAppDark()
    val context = LocalContext.current
    var activeTour by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(500)
        if (!GuidePreferences.isTourDone(context, GuidePreferences.TOUR_VOUCHER_FORM)) {
            activeTour = GuidePreferences.TOUR_VOUCHER_FORM
        }
    }
    val voiceCustomer = remember(voiceCommand, customers) {
        voiceCommand?.customerQuery?.let { q ->
            val ranked = CustomerMatcher.match(q, customers)
            if (CustomerMatcher.isHighConfidence(ranked)) {
                ranked.first().customer
            } else {
                null
            }
        }
    }
    val initCustId = voiceCustomer?.id ?: initialVoucher?.customerId ?: (if (voiceCommand?.customerQuery != null) 0L else customers.firstOrNull()?.id ?: 0L)
    var type by remember {
        mutableStateOf(
            when (voiceCommand?.type) {
                VoiceCommandType.EXPENSE -> VoucherType.EXPENSE
                VoiceCommandType.RECEIPT, VoiceCommandType.DISCOUNT -> VoucherType.RECEIPT
                else -> initialType
            }
        )
    }
    var selectedCustomerId by remember {
        mutableLongStateOf(initCustId)
    }
    var selectedBeneficiaryId by remember {
        mutableStateOf<Long?>(voiceCustomer?.id ?: initialVoucher?.customerId)
    }
    val initSessionsForCust = remember(initCustId, initialType, initialVoucher) {
        if (initialVoucher == null && initialType == VoucherType.RECEIPT) {
            allSessions.filter { it.customerId == initCustId && it.remainingDebt > 0 }
                .sortedBy { it.startTime }
        } else emptyList()
    }
    val initSessionIds = remember(initSessionsForCust) { initSessionsForCust.map { it.id }.toSet() }
    val initAmountStr = remember(initialVoucher, initSessionsForCust, voiceCommand) {
        if (voiceCommand != null) {
            if (voiceCommand.type == VoiceCommandType.DISCOUNT) ""
            else voiceCommand.amount?.let { Formatters.formatAmountInput(it.toString()) } ?: ""
        } else if (initialVoucher != null && initialVoucher.amount > 0) {
            Formatters.formatAmountInput(initialVoucher.amount.toString())
        } else if (initSessionsForCust.isNotEmpty()) {
            Formatters.formatAmountInput(initSessionsForCust.sumOf { it.remainingDebt }.toString())
        } else ""
    }
    var selectedSessionIds by remember { mutableStateOf(initSessionIds) }
    var amountStr by remember { mutableStateOf(initAmountStr) }
    var discountAmountStr by remember {
        mutableStateOf(
            if (voiceCommand?.type == VoiceCommandType.DISCOUNT && voiceCommand.amount != null) {
                Formatters.formatAmountInput(voiceCommand.amount.toString())
            } else ""
        )
    }
    var paymentMethod by remember { mutableStateOf(initialVoucher?.paymentMethod ?: "نقداً") }
    var notes by remember {
        mutableStateOf(
            voiceCommand?.notes
                ?: (if (voiceCommand?.type == VoiceCommandType.EXPENSE && voiceCommand.category != null) voiceCommand.category else "")
                ?: initialVoucher?.notes
                ?: ""
        )
    }

    var currentVoiceCommand by remember(voiceCommand) { mutableStateOf(voiceCommand) }
    var pendingVoiceTarget by remember { mutableStateOf<String?>(null) }
    var showVoucherVoiceSheet by remember { mutableStateOf(false) }
    var isVoucherVoiceCompact by remember { mutableStateOf(false) }
    var voucherVoicePrompt by remember { mutableStateOf("تحدث بالعملية...") }
    var unrecognizedVoucherVoiceText by remember { mutableStateOf<String?>(null) }
    var custSearchQuery by remember { mutableStateOf(if (voiceCustomer == null && voiceCommand?.customerQuery != null) voiceCommand.customerQuery ?: "" else "") }
    var beneficiarySearchQuery by remember { mutableStateOf("") }

    fun launchVoiceFor(target: String) {
        pendingVoiceTarget = target
        isVoucherVoiceCompact = target != "form_command"
        voucherVoicePrompt = when (target) {
            "amount" -> "تحدث بالمبلغ..."
            "discount" -> "تحدث بمبلغ الخصم..."
            "notes" -> "أملِ البيان أو الملاحظة..."
            "customer" -> "تحدث باسم العميل..."
            else -> "تحدث بأمر السند..."
        }
        showVoucherVoiceSheet = true
    }

    fun handleVoucherVoiceResults(matches: List<String>) {
        val spoken = matches.firstOrNull()?.trim().orEmpty()
        VoiceRecognitionHelper.logDebug("VoiceInput", "Voucher voice target: $pendingVoiceTarget, spoken='$spoken'")
        if (spoken.isNotBlank()) {
            when (pendingVoiceTarget) {
                "amount" -> {
                    val parsed = SpokenNumberParser.parseSpokenNumber(spoken)
                    if (parsed != null && parsed > 0) {
                        amountStr = Formatters.formatAmountInput(parsed.toString())
                    }
                }
                "discount" -> {
                    val parsed = SpokenNumberParser.parseSpokenNumber(spoken)
                    if (parsed != null && parsed > 0) {
                        discountAmountStr = Formatters.formatAmountInput(parsed.toString())
                    }
                }
                "notes" -> {
                    notes = if (notes.isBlank()) spoken else "$notes $spoken"
                }
                "customer" -> {
                    val ranked = CustomerMatcher.match(spoken, customers)
                    val cust = if (CustomerMatcher.isHighConfidence(ranked)) ranked.first().customer else null
                    if (cust != null) {
                        if (type == VoucherType.RECEIPT) {
                            selectedCustomerId = cust.id
                        } else {
                            selectedBeneficiaryId = cust.id
                        }
                    } else if (type == VoucherType.RECEIPT) {
                        custSearchQuery = spoken
                    } else {
                        beneficiarySearchQuery = spoken
                    }
                }
                "form_command" -> {
                    val knownCustomerNames = customers.map { it.name }
                    val cmd = VoiceRecognitionHelper.parseFirstValidCommand(matches, knownCustomerNames)
                    if (cmd != null && (cmd.amount != null || cmd.type != null || !cmd.customerQuery.isNullOrBlank())) {
                        currentVoiceCommand = cmd
                        if (cmd.type == VoiceCommandType.RECEIPT || cmd.type == VoiceCommandType.DISCOUNT) {
                            type = VoucherType.RECEIPT
                        } else if (cmd.type == VoiceCommandType.EXPENSE) {
                            type = VoucherType.EXPENSE
                        }
                        if (cmd.amount != null && cmd.amount > 0) {
                            if (cmd.type == VoiceCommandType.DISCOUNT) {
                                discountAmountStr = Formatters.formatAmountInput(cmd.amount.toString())
                                amountStr = "0"
                            } else {
                                amountStr = Formatters.formatAmountInput(cmd.amount.toString())
                            }
                        }
                        if (!cmd.customerQuery.isNullOrBlank()) {
                            val ranked = CustomerMatcher.match(cmd.customerQuery, customers)
                            val cust = if (CustomerMatcher.isHighConfidence(ranked)) ranked.first().customer else null
                            if (cust != null) {
                                if (type == VoucherType.RECEIPT) {
                                    selectedCustomerId = cust.id
                                } else {
                                    selectedBeneficiaryId = cust.id
                                }
                            } else if (type == VoucherType.RECEIPT) {
                                custSearchQuery = cmd.customerQuery.trim()
                            } else {
                                beneficiarySearchQuery = cmd.customerQuery.trim()
                            }
                        }
                        if (!cmd.notes.isNullOrBlank()) {
                            notes = cmd.notes
                        } else if (!cmd.category.isNullOrBlank()) {
                            notes = cmd.category
                        }
                        if (!cmd.notes.isNullOrBlank()) {
                            notes = if (notes.isBlank()) cmd.notes else "$notes - ${cmd.notes}"
                        }
                    } else {
                        unrecognizedVoucherVoiceText = spoken
                    }
                }
            }
        }
        pendingVoiceTarget = null
    }

    var voiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }

    var showAddCustomerSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var beneficiaryDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val selectedBeneficiary = customers.find { it.id == selectedBeneficiaryId }

    // الجلسات غير المسددة لهذا العميل مرتبة بالأقدم أولاً (الأول فالأول)
    val unsettledSessions = remember(allSessions, selectedCustomerId) {
        allSessions.filter { it.customerId == selectedCustomerId && it.remainingDebt > 0 }
            .sortedBy { it.startTime }
    }

    // النظام الهجين: التحديد التلقائي الذكي للجلسات غير المسددة وتعبئة المبلغ الافتراضي بالأقدمية
    var hasUserChangedCustomerOrType by remember { mutableStateOf(false) }
    LaunchedEffect(selectedCustomerId, type) {
        if (!hasUserChangedCustomerOrType) {
            hasUserChangedCustomerOrType = true
            return@LaunchedEffect
        }
        if (initialVoucher == null && type == VoucherType.RECEIPT) {
            val sessionsForCust = allSessions.filter { it.customerId == selectedCustomerId && it.remainingDebt > 0 }
                .sortedBy { it.startTime }
            if (sessionsForCust.isNotEmpty()) {
                selectedSessionIds = sessionsForCust.map { it.id }.toSet()
                if (amountStr.isBlank()) {
                    val totalDebt = sessionsForCust.sumOf { it.remainingDebt }
                    amountStr = Formatters.formatAmountInput(totalDebt.toString())
                }
            } else {
                selectedSessionIds = emptySet()
            }
        }
    }

    val parsedAmt = remember(amountStr) { Formatters.parseAmountInput(amountStr) }
    val parsedDisc = remember(discountAmountStr) { Formatters.parseAmountInput(discountAmountStr) }
    val canSave = parsedAmt > 0 || (type == VoucherType.RECEIPT && parsedDisc > 0)

    val currentSnapshot = VoucherFormSnapshot.from(
        type = type,
        customerId = selectedCustomerId,
        beneficiaryId = selectedBeneficiaryId,
        amountStr = amountStr,
        discountStr = discountAmountStr,
        paymentMethod = paymentMethod,
        notes = notes,
        selectedSessionIds = selectedSessionIds
    )
    val initialSnapshot = remember { currentSnapshot }
    val hasUnsavedChanges by remember(currentSnapshot, initialSnapshot) {
        derivedStateOf { (currentSnapshot != initialSnapshot) || (currentVoiceCommand != null) }
    }

    val headerTitle = when {
        initialVoucher != null && type == VoucherType.RECEIPT -> "تعديل سند (له) #${initialVoucher.voucherNumber.ifEmpty { initialVoucher.id.toString() }}"
        initialVoucher != null -> "تعديل سند (عليه) #${initialVoucher.voucherNumber.ifEmpty { initialVoucher.id.toString() }}"
        type == VoucherType.RECEIPT -> "تسجيل سند (له)"
        else -> "تسجيل سند (عليه)"
    }

    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    pendingSave?.let { save ->
        val isReceipt = type == VoucherType.RECEIPT
        val who = (if (isReceipt) selectedCustomer?.name else selectedBeneficiary?.name) ?: "مصروف عام"
        val amount = Formatters.parseAmountInput(amountStr)
        OperationConfirmSheet(
            statement = OperationStatement.voucher(isReceipt, who, amount, notes, currencySymbol),
            spoken = OperationStatement.voucherSpoken(isReceipt, who, amount, notes, currencySymbol),
            onConfirm = { pendingSave = null; save() },
            onEdit = { pendingSave = null }
        )
    }

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = headerTitle,
        subtitle = if (type == VoucherType.RECEIPT) selectedCustomer?.name else selectedBeneficiary?.name ?: "مصروف عام",
        icon = if (type == VoucherType.RECEIPT) Icons.AutoMirrored.Filled.ReceiptLong else Icons.Default.Payments,
        hasUnsavedChanges = hasUnsavedChanges,
        headerActions = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                GuideHelpButton(onClick = { activeTour = GuidePreferences.TOUR_VOUCHER_FORM })
                VoiceFieldMic(
                    onClick = {
                        if (onReSpeak != null) {
                            onReSpeak()
                        } else {
                            launchVoiceFor("form_command")
                        }
                    },
                    modifier = Modifier.guideTarget("voucher_voice_command")
                )
            }
        },
        footer = {
            // The tour popup must be created from inside the sheet window to be drawn above it.
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
                            }
                        }
                    )
                }
            }
            MosaribPrimaryButton(
                text = if (initialVoucher != null) "حفظ التعديلات" else "حفظ وترحيل السند",
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    val disc = Formatters.parseAmountInput(discountAmountStr)
                    if (amt > 0 || (type == VoucherType.RECEIPT && disc > 0)) {
                        val custId = if (type == VoucherType.RECEIPT) selectedCustomerId else selectedBeneficiaryId
                        val cat = if (type == VoucherType.RECEIPT) "سداد حساب" else notes.ifBlank { "مصروف" }
                        val selList = if (type == VoucherType.RECEIPT) selectedSessionIds.toList() else emptyList()
                        val performSave: () -> Unit = {
                            onSave(type, custId, amt, cat, paymentMethod, notes, null, selList, disc)
                            onDismiss()
                        }
                        if ((initialVoucher?.id ?: 0L) == 0L) pendingSave = performSave else performSave()
                    }
                },
                enabled = canSave,
                modifier = Modifier.weight(1f).guideTarget("voucher_save")
            )
            MosaribSecondaryButton(
                text = "إلغاء",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (currentVoiceCommand != null) {
                VoiceSummaryBanner(
                    command = currentVoiceCommand!!,
                    resolvedCustomerName = if (type == VoucherType.RECEIPT) selectedCustomer?.name else selectedBeneficiary?.name,
                    onReSpeak = onReSpeak ?: { launchVoiceFor("form_command") }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Type Toggle: قبض | صرف
            Row(
                modifier = Modifier.fillMaxWidth().guideTarget("voucher_type"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MosaribButton(
                    text = "قبض",
                    onClick = {
                        type = VoucherType.RECEIPT
                        if (selectedBeneficiaryId != null && selectedBeneficiaryId != 0L) {
                            selectedCustomerId = selectedBeneficiaryId!!
                        }
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = if (type == VoucherType.RECEIPT) AccentEmerald else if (isDark) Color(0xFF141F25) else Color(0xFFF1F5F9),
                    contentColor = if (type == VoucherType.RECEIPT) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )

                MosaribButton(
                    text = "صرف",
                    onClick = {
                        type = VoucherType.EXPENSE
                        if (selectedBeneficiaryId == null && selectedCustomerId != 0L) {
                            selectedBeneficiaryId = selectedCustomerId
                        }
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = if (type == VoucherType.EXPENSE) Color(0xFFD32F2F) else if (isDark) Color(0xFF141F25) else Color(0xFFF1F5F9),
                    contentColor = if (type == VoucherType.EXPENSE) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (type == VoucherType.RECEIPT) {
                if (lockedCustomerId != null) {
                    LockedVoucherCustomer(name = selectedCustomer?.name ?: "", label = "العميل (له)")
                } else {
                    Text("العميل (له) *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    CustomerPickerField(
                        customers = customers,
                        selected = selectedCustomer,
                        onSelect = { c ->
                            selectedCustomerId = c.id
                            custSearchQuery = ""
                            amountStr = ""
                        },
                        onAddNew = onCreateCustomer?.let { { showAddCustomerSheet = true } },
                        modifier = Modifier.fillMaxWidth().guideTarget("voucher_customer")
                    )
                    if (selectedCustomerId <= 0L && custSearchQuery.isNotBlank()) {
                        Text(
                            text = "لم أتعرّف على: ${custSearchQuery.trim()} — اختر من القائمة أو أضفه",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.warning),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }

                // الجلسات غير المسددة تُسدد تلقائياً بالأقدمية عند الحفظ؛ هنا تنبيه فقط
                if (unsettledSessions.isEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF11261B) else Color(0xFFE8F5E9))
                            .border(1.dp, if (isDark) Color(0xFF166534) else Color(0xFFC8E6C9), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "هذا العميل ليس عليه جلسات مؤخرة (سداد عام لحسابه)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                        )
                    }
                }
            } else {
                // سند صرف: تحديد العميل اختياري (مصروف عام أو مقيد على عميل)
                if (lockedCustomerId != null) {
                    LockedVoucherCustomer(name = selectedBeneficiary?.name ?: "", label = "المستفيد (عليه)")
                } else {
                    Text("المستفيد (عليه):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box {
                        MosaribTextField(
                            value = selectedBeneficiary?.name ?: "مصروف عام",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable {
                                    beneficiarySearchQuery = ""
                                    beneficiaryDropdownExpanded = true
                                }
                        )
                        MosaribMenu(
                            expanded = beneficiaryDropdownExpanded,
                            onDismissRequest = {
                                beneficiaryDropdownExpanded = false
                                beneficiarySearchQuery = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 320.dp)
                        ) {
                            MosaribTextField(
                                value = beneficiarySearchQuery,
                                onValueChange = { beneficiarySearchQuery = it },
                                placeholder = "بحث بالاسم أو الهاتف...",
                                isDense = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = if (beneficiarySearchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { beneficiarySearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                singleLine = true
                            )
                            MosaribMenuDivider()

                            if (onCreateCustomer != null) {
                                MosaribMenuItem(
                                    text = "+ عميل جديد",
                                    icon = Icons.Default.PersonAdd,
                                    tint = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        beneficiaryDropdownExpanded = false
                                        beneficiarySearchQuery = ""
                                        showAddCustomerSheet = true
                                    }
                                )
                            }

                            MosaribMenuItem(
                                text = "مصروف عام",
                                icon = Icons.Default.Payments,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    selectedBeneficiaryId = null
                                    beneficiaryDropdownExpanded = false
                                    beneficiarySearchQuery = ""
                                }
                            )

                            val filteredBeneficiaries = if (beneficiarySearchQuery.isBlank()) customers
                            else customers.filter {
                                it.name.contains(beneficiarySearchQuery, ignoreCase = true) ||
                                it.phone.contains(beneficiarySearchQuery) ||
                                it.farmName.contains(beneficiarySearchQuery, ignoreCase = true)
                            }

                            filteredBeneficiaries.forEach { c ->
                                MosaribMenuItem(
                                    text = c.name,
                                    icon = if (c.isWellOwner) Icons.Default.WaterDrop else Icons.Default.Park,
                                    onClick = {
                                        selectedBeneficiaryId = c.id
                                        beneficiaryDropdownExpanded = false
                                        beneficiarySearchQuery = ""
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MosaribTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = "المبلغ ($currencySymbol) *",
                trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("amount") }) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voucher_amount_input")
                    .guideTarget("voucher_amount")
            )

            val parsedAmt = Formatters.parseAmountInput(amountStr)
            if (parsedAmt > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedAmt, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (type == VoucherType.RECEIPT) AccentEmerald else Color(0xFFE53935),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            val extraFields: @Composable () -> Unit = {
                if (type == VoucherType.RECEIPT) {
                    Spacer(modifier = Modifier.height(10.dp))
                    MosaribTextField(
                        value = discountAmountStr,
                        onValueChange = { discountAmountStr = Formatters.formatAmountInput(it) },
                        label = "خصم تسوية ومسامحة ($currencySymbol) (اختياري)",
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .guideTarget("voucher_discount")
                            .testTag("voucher_discount_input")
                    )

                    val selectedDebtSum = unsettledSessions.filter { selectedSessionIds.contains(it.id) }.sumOf { it.remainingDebt }
                    val parsedDisc = Formatters.parseAmountInput(discountAmountStr)
                    if (selectedDebtSum > 0.0 && parsedAmt > 0.0 && parsedAmt < selectedDebtSum) {
                        val diffToSettle = Formatters.roundMoney(selectedDebtSum - parsedAmt)
                        if (diffToSettle > 0.0 && parsedDisc != diffToSettle) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        discountAmountStr = Formatters.formatAmountInput(diffToSettle.toString())
                                    }
                                    .background(
                                        color = if (isDark) Color(0xFF2E2415) else Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color(0xFFF59E0B).copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "تسوية وتصفير باقي الدين كخصم:",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                                        )
                                    )
                                }
                                Text(
                                    text = Formatters.formatCurrency(diffToSettle, currencySymbol),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                MosaribTextField(
                    value = paymentMethod,
                    onValueChange = { paymentMethod = it },
                    label = "طريقة الدفع",
                    modifier = Modifier.fillMaxWidth().guideTarget("voucher_payment_method")
                )

                Spacer(modifier = Modifier.height(12.dp))

                MosaribTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = if (type == VoucherType.RECEIPT) "البيان والملاحظات" else "بيان المصروف *",
                    placeholder = if (type == VoucherType.RECEIPT) "مثال: دفعة ري أو سداد حساب" else "اكتب بيان المصروف...",
                    trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("notes") }) },
                    modifier = Modifier.fillMaxWidth().guideTarget("voucher_notes")
                )

                // مقترحات البيان التلقائية لسندات الصرف
                if (type == VoucherType.EXPENSE && frequentDescriptions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("مقترحات سريعة للبيان:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Gray))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(frequentDescriptions) { desc ->
                            SuggestionChip(
                                onClick = { notes = desc },
                                label = { Text(desc, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
            if (!LocalSimpleMode.current) {
                extraFields()
            } else {
                var showExtraOptions by remember {
                    mutableStateOf(
                        discountAmountStr.isNotBlank() || notes.isNotBlank() ||
                            paymentMethod.trim() != "نقداً" || voiceCommand != null
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isDark) 1f else 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showExtraOptions = !showExtraOptions }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "خيارات إضافية",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Icon(
                                imageVector = if (showExtraOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        if (showExtraOptions) {
                            extraFields()
                        }
                    }
                }
            }

        }
    }

    if (showAddCustomerSheet && onCreateCustomer != null) {
        AddEditCustomerBottomSheet(
            initialCustomer = null,
            currencySymbol = currencySymbol,
            onDismiss = { showAddCustomerSheet = false },
            onSave = { _, name, phone, farmName, location, customerNotes, customPrice, isBeneficiary, isWellOwner, customGroup ->
                coroutineScope.launch {
                    runCatching {
                        onCreateCustomer(
                            Customer(
                                name = name.trim(),
                                phone = phone.trim(),
                                farmName = farmName.trim(),
                                location = location.trim(),
                                notes = customerNotes.trim(),
                                customPricePerHour = customPrice,
                                isBeneficiary = isBeneficiary,
                                isWellOwner = isWellOwner,
                                customGroup = customGroup.trim()
                            )
                        )
                    }.onSuccess { newId ->
                        custSearchQuery = ""
                        if (type == VoucherType.RECEIPT) {
                            selectedCustomerId = newId
                            amountStr = ""
                        } else {
                            selectedBeneficiaryId = newId
                        }
                    }.onFailure {
                        Toast.makeText(context, it.localizedMessage ?: "تعذرت إضافة العميل", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    if (showVoucherVoiceSheet) {
        VoiceRecordSheet(
            title = if (isVoucherVoiceCompact) "إملاء صوتي" else "الإدخال الصوتي للسند",
            subtitle = voucherVoicePrompt,
            compact = isVoucherVoiceCompact,
            onDismiss = { showVoucherVoiceSheet = false },
            onResult = { candidates ->
                showVoucherVoiceSheet = false
                handleVoucherVoiceResults(candidates)
            }
        )
    }

    unrecognizedVoucherVoiceText?.let { spoken ->
        VoiceUnrecognizedDialog(
            spokenText = spoken,
            onReSpeak = {
                unrecognizedVoucherVoiceText = null
                launchVoiceFor("form_command")
            },
            onShowExamples = {
                unrecognizedVoucherVoiceText = null
            },
            onDismiss = { unrecognizedVoucherVoiceText = null }
        )
    }

    VoiceSupportDialogs(
        dialogType = voiceDialogType,
        onDismiss = { voiceDialogType = VoiceDialogType.NONE }
    )

}

@Composable
fun OperationsFilterChipsRow(
    filterState: OperationsFilterState,
    customers: List<Customer>,
    onSelectSort: (OperationsSort) -> Unit,
    onSelectType: (OperationsFilter) -> Unit,
    onSelectAccountType: (AccountTypeFilter) -> Unit,
    onSelectPayment: (SessionPaymentFilter) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onRequestCustomRange: () -> Unit,
    onSelectCustomer: (Long?) -> Unit,
    onRequestPickCustomer: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortOptions = listOf(
        DropdownOption(OperationsSort.NEWEST, "الأحدث"),
        DropdownOption(OperationsSort.OLDEST, "الأقدم"),
        DropdownOption(OperationsSort.AMOUNT_DESC, "الأعلى مبلغاً"),
        DropdownOption(OperationsSort.AMOUNT_ASC, "الأقل مبلغاً"),
        DropdownOption(OperationsSort.NAME_ASC, "الاسم (أ-ي)"),
        DropdownOption(OperationsSort.NAME_DESC, "الاسم (ي-أ)")
    )

    val typeOptions = listOf(
        DropdownOption(OperationsFilter.ALL, "الكل"),
        DropdownOption(OperationsFilter.SESSIONS, "سقي"),
        DropdownOption(OperationsFilter.RECEIPTS, "قبض"),
        DropdownOption(OperationsFilter.EXPENSES, "صرف"),
        DropdownOption(OperationsFilter.DISCOUNTS, "خصم"),
        DropdownOption(OperationsFilter.PURCHASES, "شراء ساعات"),
        DropdownOption(OperationsFilter.DEFERRED, "آجل / ديون")
    )

    val accountOptions = listOf(
        DropdownOption(AccountTypeFilter.ALL, "الكل"),
        DropdownOption(AccountTypeFilter.FARMER, "مزارع"),
        DropdownOption(AccountTypeFilter.WELL_OWNER, "صاحب بئر")
    )

    val paymentOptions = listOf(
        DropdownOption(SessionPaymentFilter.ALL, "الكل"),
        DropdownOption(SessionPaymentFilter.PAID, "مسدد"),
        DropdownOption(SessionPaymentFilter.PARTIAL, "جزئي"),
        DropdownOption(SessionPaymentFilter.UNPAID, "غير مسدد")
    )

    val distinctGroups = remember(customers) {
        customers.mapNotNull { it.customGroup.trim().takeIf { g -> g.isNotBlank() } }.distinct().sorted()
    }

    val groupOptions = buildList<DropdownOption<String?>> {
        add(DropdownOption(null, "الكل"))
        distinctGroups.forEach { add(DropdownOption(it, it)) }
        add(DropdownOption("", "بدون مجموعة"))
    }

    val periodOptions = listOf(
        DropdownOption(ReportPeriod.ALL, "الكل"),
        DropdownOption(ReportPeriod.TODAY, "اليوم"),
        DropdownOption(ReportPeriod.YESTERDAY, "أمس"),
        DropdownOption(ReportPeriod.THIS_WEEK, "هذا الأسبوع"),
        DropdownOption(ReportPeriod.THIS_MONTH, "هذا الشهر"),
        DropdownOption(ReportPeriod.LAST_MONTH, "الشهر الماضي"),
        DropdownOption(ReportPeriod.CUSTOM, "من - إلى…")
    )

    val customerOptions = listOf<DropdownOption<Long?>>(
        DropdownOption(null, "الكل"),
        DropdownOption(-1L, "اختيار عميل…")
    )

    val periodDisplayText = remember(filterState.period, filterState.customRange) {
        if (filterState.period == ReportPeriod.CUSTOM && filterState.customRange != null) {
            val dFormat = java.text.SimpleDateFormat("d/M", java.util.Locale.US)
            "${dFormat.format(java.util.Date(filterState.customRange.first))} - ${dFormat.format(java.util.Date(filterState.customRange.second))}"
        } else null
    }

    val customerDisplayText = remember(filterState.customerId, customers) {
        filterState.customerId?.let { id -> customers.find { it.id == id }?.name }
    }

    val hasAnyFilter = filterState.sort != OperationsSort.NEWEST ||
            filterState.type != OperationsFilter.ALL ||
            filterState.accountType != AccountTypeFilter.ALL ||
            filterState.sessionPayment != SessionPaymentFilter.ALL ||
            filterState.group != null ||
            filterState.period != ReportPeriod.ALL ||
            filterState.customRange != null ||
            filterState.customerId != null

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. الترتيب
        item(key = "sort") {
            FilterDropdownChip(
                label = "الترتيب",
                selected = filterState.sort,
                options = sortOptions,
                isDefault = filterState.sort == OperationsSort.NEWEST,
                onSelect = onSelectSort
            )
        }

        // 2. النوع
        item(key = "type") {
            FilterDropdownChip(
                label = "النوع",
                selected = filterState.type,
                options = typeOptions,
                isDefault = filterState.type == OperationsFilter.ALL,
                onSelect = onSelectType
            )
        }

        // 3. نوع الحساب
        item(key = "accountType") {
            FilterDropdownChip(
                label = "نوع الحساب",
                selected = filterState.accountType,
                options = accountOptions,
                isDefault = filterState.accountType == AccountTypeFilter.ALL,
                onSelect = onSelectAccountType
            )
        }

        // 4. الحالة
        item(key = "payment") {
            FilterDropdownChip(
                label = "الحالة",
                selected = filterState.sessionPayment,
                options = paymentOptions,
                isDefault = filterState.sessionPayment == SessionPaymentFilter.ALL,
                onSelect = onSelectPayment
            )
        }

        // 5. المجموعة
        item(key = "group") {
            FilterDropdownChip(
                label = "المجموعة",
                selected = filterState.group,
                options = groupOptions,
                isDefault = filterState.group == null,
                onSelect = onSelectGroup
            )
        }

        // 6. الفترة
        item(key = "period") {
            FilterDropdownChip(
                label = "الفترة",
                selected = filterState.period,
                options = periodOptions,
                isDefault = filterState.period == ReportPeriod.ALL && filterState.customRange == null,
                onSelect = { selectedPeriod ->
                    if (selectedPeriod == ReportPeriod.CUSTOM) {
                        onRequestCustomRange()
                    } else {
                        onSelectPeriod(selectedPeriod)
                    }
                },
                customDisplayText = periodDisplayText
            )
        }

        // 7. العميل
        item(key = "customer") {
            FilterDropdownChip(
                label = "العميل",
                selected = filterState.customerId,
                options = customerOptions,
                isDefault = filterState.customerId == null,
                onSelect = { selectedId ->
                    if (selectedId == -1L) {
                        onRequestPickCustomer()
                    } else {
                        onSelectCustomer(null)
                    }
                },
                customDisplayText = customerDisplayText
            )
        }

        // زر مسح الكل عند تفعيل أي فلتر
        if (hasAnyFilter) {
            item(key = "clear_all") {
                FilterClearAllChip(onClearAll = onClearAll)
            }
        }
    }
}

/** Read-only customer line used when the voucher form is opened from that customer's screen. */
@Composable
private fun LockedVoucherCustomer(name: String, label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
    Spacer(modifier = Modifier.height(6.dp))
    Surface(
        modifier = Modifier.fillMaxWidth().guideTarget("voucher_customer"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun SummaryTile(label: String, amount: Double, currency: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted))
            AmountTooltip(Formatters.amountToArabicWordsApprox(amount, currency)) {
                Text(
                    text = Formatters.formatCurrency(amount, currency),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, color = color)
                )
            }
        }
    }
}

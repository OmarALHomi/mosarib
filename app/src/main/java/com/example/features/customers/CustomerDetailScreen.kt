package com.example.features.customers

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.ui.isAppDark
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.core.util.MessageFormatHelper
import com.example.core.util.PdfReportGenerator
import com.example.features.sessions.SettleSessionDialog
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.VouchersViewModel
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import com.example.features.wellowners.WellOwnerPurchaseBottomSheet
import androidx.compose.material3.FilledTonalButton
import com.example.core.ui.CardBadge
import com.example.core.ui.CardChip
import com.example.core.ui.OperationCard
import com.example.core.util.DayGrouping
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.mosarib
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.SecondaryAquaDark
import com.example.ui.theme.DarkMosaribExtendedColors
import java.io.File

enum class CustomerOpFilter(val title: String) {
    ALL("الكل"),
    PURCHASES("شراء ساعات"),
    SESSIONS("السقي / البيع"),
    RECEIPTS("التحصيل"),
    DISBURSEMENTS("السداد"),
    DISCOUNTS("الخصومات"),
    DEBT_ONLY("المؤخر")
}

sealed class CustomerLedgerItem {
    abstract val timestamp: Long

    data class SessionItem(
        val session: WaterSession,
        val originalCustomer: Customer? = null,
        val sponsorOwner: Customer? = null,
        val linkedVouchers: List<Voucher> = emptyList()
    ) : CustomerLedgerItem() {
        override val timestamp: Long get() = session.startTime
    }

    data class PurchaseItem(
        val purchase: WellOwnerPurchase
    ) : CustomerLedgerItem() {
        override val timestamp: Long get() = purchase.date
    }

    data class ReceiptItem(
        val voucher: Voucher
    ) : CustomerLedgerItem() {
        override val timestamp: Long get() = voucher.date
    }

    data class DisbursementItem(
        val voucher: Voucher
    ) : CustomerLedgerItem() {
        override val timestamp: Long get() = voucher.date
    }

    data class DiscountItem(
        val voucher: Voucher
    ) : CustomerLedgerItem() {
        override val timestamp: Long get() = voucher.date
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: CustomersViewModel,
    vouchersViewModel: VouchersViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit,
    onAddSessionForCustomer: (Customer) -> Unit = {}
) {
    BackHandler { onBack() }

    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val allCustomersWithBalance by viewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    val customerWithBalance = allCustomersWithBalance.find { it.customer.id == customerId }
    val allCustomers: List<Customer> by viewModel.allCustomersIncludingArchived.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeCustomers = remember(allCustomers) { allCustomers.filter { !it.isArchived } }
    val sessions by remember(customerId) { viewModel.getCustomerSessions(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val purchases by remember(customerId) { viewModel.getCustomerPurchases(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val vouchers by remember(customerId) { viewModel.getCustomerVouchers(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val customGroups by viewModel.customGroups.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf(CustomerOpFilter.ALL) }
    var showAddReceiptSheet by remember { mutableStateOf(false) }
    var showAddDisbursementSheet by remember { mutableStateOf(false) }
    var showOwnerPurchaseSheet by remember { mutableStateOf(false) }
    var showAddSessionSheet by remember { mutableStateOf(false) }
    var showOwnerWasteDialog by remember { mutableStateOf(false) }
    var showEditCustomerSheet by remember { mutableStateOf(false) }
    var sessionToSettle by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToEdit by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<WaterSession?>(null) }
    var purchaseToEdit by remember { mutableStateOf<WellOwnerPurchase?>(null) }
    var purchaseToDelete by remember { mutableStateOf<WellOwnerPurchase?>(null) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }
    var voucherToEdit by remember { mutableStateOf<Voucher?>(null) }
    var sessionForDetails by remember { mutableStateOf<WaterSession?>(null) }
    var voucherForDetails by remember { mutableStateOf<Voucher?>(null) }
    var purchaseForDetails by remember { mutableStateOf<WellOwnerPurchase?>(null) }
    var expandedSessionIds by remember { mutableStateOf(setOf<Long>()) }

    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    var localPdfReady by remember { mutableStateOf<Pair<File, String>?>(null) }
    var messageSessionTarget by remember { mutableStateOf<WaterSession?>(null) }
    var messagePurchaseTarget by remember { mutableStateOf<WellOwnerPurchase?>(null) }
    var messageCustomTarget by remember { mutableStateOf<String?>(null) }
    val activePdf = pdfReady ?: localPdfReady

    // PDF Viewer / Share Dialog
    activePdf?.let { (file, title) ->
        com.example.core.ui.MosaribFileActionDialog(
            file = file,
            title = title,
            fileType = "PDF",
            onOpen = {
                FileSharingHelper.openPdf(context, file)
                viewModel.clearPdfReady()
                localPdfReady = null
            },
            onShare = {
                FileSharingHelper.sharePdf(context, file, title)
                viewModel.clearPdfReady()
                localPdfReady = null
            },
            onDismiss = {
                viewModel.clearPdfReady()
                localPdfReady = null
            }
        )
    }

    if (customerWithBalance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم يتم العثور على العميل", fontWeight = FontWeight.Bold)
        }
        return
    }

    val customer = customerWithBalance.customer

    // Message Choice Dialog (WhatsApp / SMS) - Session Bill
    messageSessionTarget?.let { s ->
        val origCust = if (s.customerId != customerId) allCustomers.find { it.id == s.customerId } ?: customer else customer
        val sponsorCust = if (s.billedToCustomerId != null) allCustomers.find { it.id == s.billedToCustomerId } else null
        val waMsg = com.example.core.util.MessageFormatHelper.buildSessionBill(
            session = s,
            customer = customer,
            config = config,
            isSms = false,
            sponsorCustomer = sponsorCust,
            beneficiaryCustomer = origCust
        )
        val smsMsg = com.example.core.util.MessageFormatHelper.buildSessionBill(
            session = s,
            customer = customer,
            config = config,
            isSms = true,
            sponsorCustomer = sponsorCust,
            beneficiaryCustomer = origCust
        )

        SendMessageChoiceDialog(
            recipientName = customer.name,
            recipientPhone = customer.phone,
            messageText = waMsg,
            smsMessageText = smsMsg,
            onDismiss = { messageSessionTarget = null },
            onSendWhatsApp = {
                messageSessionTarget = null
                FileSharingHelper.sendWhatsAppMessage(context, customer.phone, waMsg)
            },
            onSendSms = {
                messageSessionTarget = null
                FileSharingHelper.sendSms(context, customer.phone, smsMsg)
            }
        )
    }

    // Message Choice Dialog (WhatsApp / SMS) - Well Owner Purchase Bill
    messagePurchaseTarget?.let { p ->
        val waMsg = MessageFormatHelper.buildPurchaseBill(p, customer, config, isSms = false)
        val smsMsg = MessageFormatHelper.buildPurchaseBill(p, customer, config, isSms = true)

        SendMessageChoiceDialog(
            recipientName = customer.name,
            recipientPhone = customer.phone,
            messageText = waMsg,
            smsMessageText = smsMsg,
            onDismiss = { messagePurchaseTarget = null },
            onSendWhatsApp = {
                messagePurchaseTarget = null
                FileSharingHelper.sendWhatsAppMessage(context, customer.phone, waMsg)
            },
            onSendSms = {
                messagePurchaseTarget = null
                FileSharingHelper.sendSms(context, customer.phone, smsMsg)
            }
        )
    }

    messageCustomTarget?.let { msg ->
        customerWithBalance?.let { cwb ->
            SendMessageChoiceDialog(
                recipientName = cwb.customer.name,
                recipientPhone = cwb.customer.phone,
                messageText = msg,
                onDismiss = { messageCustomTarget = null },
                onSendWhatsApp = {
                    messageCustomTarget = null
                    FileSharingHelper.sendWhatsAppMessage(context, cwb.customer.phone, msg)
                },
                onSendSms = {
                    messageCustomTarget = null
                    FileSharingHelper.sendSms(context, cwb.customer.phone, msg)
                }
            )
        }
    }

    // Build unified ledger list
    val sessionItems = remember(sessions, allCustomers, vouchers) {
        sessions.map { s ->
            val origCust = if (s.customerId != customerId) allCustomers.find { it.id == s.customerId } else null
            val sponsorOwner = if (s.billedToCustomerId != null && s.billedToCustomerId != customerId) allCustomers.find { it.id == s.billedToCustomerId } else null
            val linked = vouchers.filter { it.sessionId == s.id }
            CustomerLedgerItem.SessionItem(s, origCust, sponsorOwner, linked)
        }
    }
    val purchaseItems = remember(purchases) { purchases.map { CustomerLedgerItem.PurchaseItem(it) } }
    val receiptItems = remember(vouchers) {
        vouchers.filter { it.type == VoucherType.RECEIPT }.map { CustomerLedgerItem.ReceiptItem(it) }
    }
    val disbursementItems = remember(vouchers) {
        vouchers.filter { it.type == VoucherType.EXPENSE }.map { CustomerLedgerItem.DisbursementItem(it) }
    }
    val discountItems = remember(vouchers) {
        vouchers.filter { it.type == VoucherType.DISCOUNT }.map { CustomerLedgerItem.DiscountItem(it) }
    }

    val isCustomerAccountSettled = (customerWithBalance?.receivableBalance ?: 0.0) <= 0.001
    val sortedCustomerSessions = remember(sessions) { sessions.sortedBy { it.startTime } }

    val getEffectiveSessionDebt: (WaterSession) -> Double = remember(customerWithBalance, sortedCustomerSessions) {
        { session ->
            val recBal = customerWithBalance?.receivableBalance ?: 0.0
            if (recBal <= 0.001) 0.0
            else if (session.remainingDebt <= 0.001) 0.0
            else {
                val totalBilled = sortedCustomerSessions.sumOf { it.totalAmount }
                val totalPaidTowardsSessions = (totalBilled - recBal).coerceAtLeast(0.0)
                var runningPaid = totalPaidTowardsSessions
                var effective = session.remainingDebt
                for (s in sortedCustomerSessions) {
                    val sCost = s.totalAmount
                    if (s.id == session.id) {
                        val sPaid = runningPaid.coerceAtMost(sCost)
                        val sRemaining = (sCost - sPaid).coerceAtLeast(0.0)
                        effective = minOf(sRemaining, session.remainingDebt)
                        break
                    }
                    runningPaid = (runningPaid - sCost).coerceAtLeast(0.0)
                }
                effective
            }
        }
    }

    val allLedgerItems = remember(sessionItems, purchaseItems, receiptItems, disbursementItems, discountItems) {
        (sessionItems + purchaseItems + receiptItems + disbursementItems + discountItems).sortedByDescending { it.timestamp }
    }

    val filteredItems = remember(selectedFilter, allLedgerItems, sessionItems, purchaseItems, receiptItems, disbursementItems, discountItems, isCustomerAccountSettled) {
        when (selectedFilter) {
            CustomerOpFilter.ALL -> allLedgerItems
            CustomerOpFilter.PURCHASES -> purchaseItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.SESSIONS -> sessionItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.RECEIPTS -> receiptItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.DISBURSEMENTS -> disbursementItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.DISCOUNTS -> discountItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.DEBT_ONLY -> if (isCustomerAccountSettled) emptyList() else sessionItems.filter { getEffectiveSessionDebt(it.session) > 0.001 }.sortedByDescending { it.timestamp }
        }
    }

    val unsettledCount = remember(sessions, customerWithBalance) {
        if (isCustomerAccountSettled) 0
        else sessions.count { getEffectiveSessionDebt(it) > 0.001 }
    }

    val isDark = isAppDark()
    val avatarColor = remember(customer.id, isDark) { AvatarPalette.forId(customer.id, isDark) }
    val initials = remember(customer.name) {
        val words = customer.name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        when {
            words.isEmpty() -> ""
            words.size == 1 -> words[0].take(1)
            else -> {
                val firstLetter = words.first().take(1)
                val lastWord = words.last()
                val cleanLast = if (lastWord.startsWith("ال") && lastWord.length > 2) {
                    lastWord.removePrefix("ال")
                } else {
                    lastWord
                }
                val lastLetter = cleanLast.take(1)
                "$firstLetter$lastLetter"
            }
        }
    }
    val ledgerBalanceResult = remember(allLedgerItems, customerWithBalance?.customer?.isWellOwner) {
        val isOwner = customerWithBalance?.customer?.isWellOwner ?: false
        LedgerRunningBalance.compute(allLedgerItems, isWellOwner = isOwner)
    }

    val isReconciled = remember(ledgerBalanceResult, customerWithBalance) {
        if (customerWithBalance == null) true
        else {
            val headerBal = if (customerWithBalance.customer.isWellOwner) {
                if (customerWithBalance.payableBalance > 0) customerWithBalance.payableBalance else customerWithBalance.receivableBalance
            } else {
                customerWithBalance.receivableBalance
            }
            kotlin.math.abs(ledgerBalanceResult.finalBalance - headerBal) <= 0.01
        }
    }

    val dayGroups = remember(filteredItems) {
        DayGrouping.groupByDay(
            items = filteredItems,
            timestampSelector = { it.timestamp }
        )
    }

    Scaffold(
        floatingActionButton = {
            customerWithBalance?.let { cwb ->
                val isOwner = cwb.customer.isWellOwner
                ExtendedFloatingActionButton(
                    onClick = {
                        if (isOwner) {
                            showOwnerPurchaseSheet = true
                        } else {
                            showAddSessionSheet = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (isOwner) Icons.Default.Add else Icons.Default.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = if (isOwner) "شراء ساعات" else "سقي جديد",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    containerColor = PrimaryTeal,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        val listState = rememberLazyListState()
        val firstIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
        val firstOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }

        val scrollProgress by remember {
            derivedStateOf {
                if (firstIndex > 0) 1f
                else (firstOffset / 180f).coerceIn(0f, 1f)
            }
        }

        var moreMenuExpanded by remember { mutableStateOf(false) }
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // ============================================================
                // 1. GRADIENT HERO HEADER (ينسحب ويتلاشى بسلاسة تامة مع التمرير)
                // ============================================================
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        PrimaryTealDark,
                                        SecondaryAquaDark,
                                        PrimaryTeal.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = statusBarPadding + 52.dp, bottom = 14.dp)
                                .graphicsLayer {
                                    alpha = (1f - (scrollProgress * 0.45f)).coerceIn(0.4f, 1f)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                // الصورة الرمزية
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(avatarColor.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = avatarColor.onBackground
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    ),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )

                                if (customer.farmName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = customer.farmName,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        )
                                    }
                                }

                                val metaChips = buildList {
                                    if (customer.phone.isNotBlank()) add(customer.phone)
                                    if (customer.location.isNotBlank()) add(customer.location)
                                    if ((customer.customPricePerHour ?: 0.0) > 0) {
                                        val priceLabel = if (customer.isWellOwner) "تكلفة الشراء" else "سعر خاص"
                                        add("$priceLabel: ${Formatters.formatNumber(customer.customPricePerHour!!)} ${config.currencySymbol}/س")
                                    }
                                }
                                if (metaChips.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = metaChips.joinToString("  •  "),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                if (customer.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = customer.notes,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.85f)
                                        ),
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 30.dp)
                                    )
                                }

                                if (customer.customGroup.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Bookmarks, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = customer.customGroup,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Action Buttons
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (customer.phone.isNotEmpty()) {
                                        QuickActionButton(
                                            icon = Icons.Default.Call,
                                            label = "اتصال",
                                            onClick = { FileSharingHelper.makePhoneCall(context, customer.phone) }
                                        )
                                        QuickActionButton(
                                            icon = Icons.AutoMirrored.Filled.Send,
                                            label = "إرسال كشف",
                                            onClick = {
                                                messageCustomTarget = viewModel.buildCustomerStatementMessage(customer, customerWithBalance)
                                            }
                                        )
                                    }
                                    QuickActionButton(
                                        icon = Icons.Default.PictureAsPdf,
                                        label = "كشف PDF",
                                        onClick = { viewModel.generateCustomerStatementPdf(customer) }
                                    )
                                    if (customer.isWellOwner) {
                                        QuickActionButton(
                                            icon = Icons.Default.AccessTime,
                                            label = "تسجيل هدر",
                                            onClick = { showOwnerWasteDialog = true }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ============================================================
                    // 2. BALANCE + TOTALS CARD
                    // ============================================================
                    item {
                        val isDebt = customerWithBalance.balance > 0
                        val isCredit = customerWithBalance.balance < 0

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp)
                                .shadow(3.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (customer.isWellOwner) {
                                val hasPayable = customerWithBalance.payableBalance > 0
                                val hasReceivable = customerWithBalance.receivableBalance > 0
                                val statusText = when {
                                    hasPayable -> "عليك لصاحب البئر (متبقي مستحقات الشراء بعد خصم السقي)"
                                    hasReceivable -> "لك على صاحب البئر (ديون سقي متبقية بذمته بعد خصم الشراء)"
                                    else -> "حساب صاحب البئر مسدد (لا توجد مطالبات)"
                                }
                                val statusColor = when {
                                    hasPayable -> Color(0xFFD32F2F)
                                    hasReceivable -> Color(0xFF2E7D32)
                                    else -> AccentEmerald
                                }
                                val netAmount = if (hasPayable) customerWithBalance.payableBalance else customerWithBalance.receivableBalance

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Formatters.formatCurrency(netAmount, config.currencySymbol),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = statusColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 28.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("مشتريات ساعات", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(Formatters.formatCurrency(customerWithBalance.totalPurchaseAmount, config.currencySymbol), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentGold), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("مسدد نقداً له", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(Formatters.formatCurrency(customerWithBalance.totalDisbursedAmount, config.currencySymbol), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE53935)), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("سقي على حسابه", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(Formatters.formatCurrency(customerWithBalance.totalBilledAmount, config.currencySymbol), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal), maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            } else {
                                val isDebt = customerWithBalance.receivableBalance > 0
                                val isCredit = customerWithBalance.receivableBalance < 0
                                Text(
                                    text = when {
                                        isDebt -> "صافي الدين المتبقي بذمة العميل (لك)"
                                        isCredit -> "رصيد دائن لصالح العميل (عليك)"
                                        else -> "الحساب مسدد (لا توجد مطالبات)"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val balanceColor = when {
                                    isDebt -> Color(0xFFE53935)
                                    isCredit -> AccentEmerald
                                    else -> PrimaryTeal
                                }
                                Text(
                                    text = Formatters.formatCurrency(kotlin.math.abs(customerWithBalance.receivableBalance), config.currencySymbol),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        color = balanceColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 30.sp
                                    )
                                )
                                if (customerWithBalance.receivableBalance == 0.0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(AccentEmerald.copy(alpha = 0.12f))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("مسدد", style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dynamic Grid of Financial Indicators
                            val statOnSurface = MaterialTheme.colorScheme.onSurface
                            val statPrimary = MaterialTheme.colorScheme.primary
                            val statDebt = MaterialTheme.mosarib.debt
                            val statCredit = MaterialTheme.mosarib.credit

                            val statCells = remember(customer, customerWithBalance, sessions, config.currencySymbol, statOnSurface, statPrimary, statDebt, statCredit) {
                                buildList {
                                    if (customer.isWellOwner) {
                                        add(
                                            StatCellData(
                                                label = "قيمة الشراء الصافية",
                                                value = Formatters.formatCurrency(customerWithBalance.totalPurchaseAmount, config.currencySymbol),
                                                color = statOnSurface,
                                                icon = Icons.Default.WaterDrop
                                            )
                                        )
                                        add(
                                            StatCellData(
                                                label = "الساعات المشتراة",
                                                value = Formatters.formatDurationArabic(customerWithBalance.totalPurchasedMinutes),
                                                color = statPrimary,
                                                icon = Icons.Default.AccessTime
                                            )
                                        )
                                        add(
                                            StatCellData(
                                                label = "المسدد له",
                                                value = Formatters.formatCurrency(customerWithBalance.totalDisbursedAmount, config.currencySymbol),
                                                color = statDebt,
                                                icon = Icons.Default.Payments
                                            )
                                        )
                                        if (customerWithBalance.totalPaidAmount > 0) {
                                            add(
                                                StatCellData(
                                                    label = "المحصل منه",
                                                    value = Formatters.formatCurrency(customerWithBalance.totalPaidAmount, config.currencySymbol),
                                                    color = statCredit,
                                                    icon = Icons.Default.ArrowDownward
                                                )
                                            )
                                        }
                                        if (sessions.isNotEmpty() || customerWithBalance.totalBilledAmount > 0 || customerWithBalance.totalSoldMinutes > 0) {
                                            add(
                                                StatCellData(
                                                    label = "سقي / بيع له",
                                                    value = Formatters.formatCurrency(customerWithBalance.totalBilledAmount, config.currencySymbol),
                                                    color = statPrimary,
                                                    icon = Icons.Default.WaterDrop
                                                )
                                            )
                                            add(
                                                StatCellData(
                                                    label = "ساعات السقي له",
                                                    value = Formatters.formatDurationArabic(customerWithBalance.totalSoldMinutes),
                                                    color = statPrimary,
                                                    icon = Icons.Default.AccessTime
                                                )
                                            )
                                        }
                                        val wasteMinutes = (customerWithBalance.totalPurchasedMinutes - customerWithBalance.totalChargeablePurchasedMinutes).coerceAtLeast(0)
                                        if (wasteMinutes > 0 || customerWithBalance.totalOwnerWasteCredit > 0) {
                                            add(
                                                StatCellData(
                                                    label = "هدر على صاحب البئر",
                                                    value = Formatters.formatDurationArabic(wasteMinutes),
                                                    color = statDebt,
                                                    icon = Icons.Default.AccessTime
                                                )
                                            )
                                            add(
                                                StatCellData(
                                                    label = "خصم الهدر عليه",
                                                    value = Formatters.formatCurrency(customerWithBalance.totalOwnerWasteCredit, config.currencySymbol),
                                                    color = statDebt,
                                                    icon = Icons.Default.ArrowDownward
                                                )
                                            )
                                        }
                                    } else {
                                        add(
                                            StatCellData(
                                                label = "إجمالي السقي",
                                                value = Formatters.formatCurrency(customerWithBalance.totalBilledAmount, config.currencySymbol),
                                                color = statOnSurface,
                                                icon = Icons.Default.WaterDrop
                                            )
                                        )
                                        if (customerWithBalance.totalSoldMinutes > 0) {
                                            add(
                                                StatCellData(
                                                    label = "ساعات الري",
                                                    value = Formatters.formatDurationArabic(customerWithBalance.totalSoldMinutes),
                                                    color = statPrimary,
                                                    icon = Icons.Default.AccessTime
                                                )
                                            )
                                        }
                                        add(
                                            StatCellData(
                                                label = "المحصل نقداً",
                                                value = Formatters.formatCurrency(customerWithBalance.totalPaidAmount, config.currencySymbol),
                                                color = statCredit,
                                                icon = Icons.Default.Payments
                                            )
                                        )
                                        if (customerWithBalance.totalDisbursedAmount > 0) {
                                            add(
                                                StatCellData(
                                                    label = "إجمالي المنصرف",
                                                    value = Formatters.formatCurrency(customerWithBalance.totalDisbursedAmount, config.currencySymbol),
                                                    color = statDebt,
                                                    icon = Icons.Default.ArrowDownward
                                                )
                                            )
                                        } else if (sessions.isNotEmpty()) {
                                            add(
                                                StatCellData(
                                                    label = "عدد دورات السقي",
                                                    value = "${sessions.size} دورة",
                                                    color = statOnSurface,
                                                    icon = Icons.Default.Receipt
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                statCells.chunked(2).forEach { rowCells ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowCells.forEach { cell ->
                                            StatCellBox(
                                                modifier = Modifier.weight(1f),
                                                label = cell.label,
                                                value = cell.value,
                                                color = cell.color,
                                                icon = cell.icon
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

                // 2. شريط أزرار تسجيل العمليات المالية
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (customer.isWellOwner) {
                            Button(
                                onClick = { showOwnerPurchaseSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.warning),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "شراء ساعات",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = { showAddDisbursementSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.debt),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دفع له (صرف)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        } else {
                            Button(
                                onClick = { showAddReceiptSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.credit),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "قبض (له)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = { showAddDisbursementSheet = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.mosarib.debt),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "صرف (عليه)",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // 2. Filter Bar Chips
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomerOpFilter.entries
                            .filter { customer.isWellOwner || it != CustomerOpFilter.PURCHASES }
                            .forEach { filter ->
                            val count = when (filter) {
                                CustomerOpFilter.ALL -> allLedgerItems.size
                                CustomerOpFilter.PURCHASES -> purchaseItems.size
                                CustomerOpFilter.SESSIONS -> sessions.size
                                CustomerOpFilter.RECEIPTS -> receiptItems.size
                                CustomerOpFilter.DISBURSEMENTS -> disbursementItems.size
                                CustomerOpFilter.DISCOUNTS -> discountItems.size
                                CustomerOpFilter.DEBT_ONLY -> unsettledCount
                            }
                            item {
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = {
                                        Text(
                                            text = "${filter.title} ($count)",
                                            fontWeight = if (selectedFilter == filter) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (filter == CustomerOpFilter.DEBT_ONLY) Color(0xFFEF4444).copy(alpha = 0.2f) else PrimaryTeal.copy(alpha = 0.15f),
                                        selectedLabelColor = if (filter == CustomerOpFilter.DEBT_ONLY) Color(0xFFDC2626) else PrimaryTeal
                                    )
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 3. Unified Operations Ledger List
                if (filteredItems.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.FilterList,
                            title = "لا توجد حركات مسجلة ضمن هذا التصنيف",
                            description = "يمكنك إضافة دورة سقي جديدة أو تسجيل سند قبض / صرف"
                        )
                    }
                } else {
                    dayGroups.forEach { group ->
                        item(key = "day_${group.dayKey}") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 12.dp, bottom = 4.dp),
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
                                when (val item = groupItem.item) {
                                    is CustomerLedgerItem.PurchaseItem -> "p_${item.purchase.id}"
                                    is CustomerLedgerItem.SessionItem -> "s_${item.session.id}"
                                    is CustomerLedgerItem.ReceiptItem -> "r_${item.voucher.id}"
                                    is CustomerLedgerItem.DisbursementItem -> "d_${item.voucher.id}"
                                    is CustomerLedgerItem.DiscountItem -> "dsc_${item.voucher.id}"
                                }
                            }
                        ) { groupItem ->
                            val ledgerItem = groupItem.item
                            val runningBal = ledgerBalanceResult.balancesByItem[ledgerItem]
                            val extraNote = if (isReconciled && runningBal != null) {
                                LedgerRunningBalance.formatRunningBalance(runningBal, customer.isWellOwner, config.currencySymbol)
                            } else null

                            when (ledgerItem) {
                                is CustomerLedgerItem.PurchaseItem -> {
                                    val purchase = ledgerItem.purchase
                                    val payableAmount = WellOwnerPurchaseMath.payableAmount(purchase)
                                    val statement = purchase.notes.trim().takeIf { it.isNotBlank() }
                                    val meta = listOfNotNull(Formatters.formatTime(purchase.date), "#PUR-${purchase.id}").joinToString(" • ")

                                    val chips = buildList {
                                        if (purchase.durationMinutes > 0) {
                                            add(
                                                CardChip(
                                                    text = Formatters.formatDurationCompact(purchase.durationMinutes),
                                                    container = MaterialTheme.mosarib.warning.copy(alpha = 0.12f),
                                                    content = MaterialTheme.mosarib.warning,
                                                    icon = Icons.Default.AccessTime
                                                )
                                            )
                                        }
                                        if (purchase.purchaseRatePerHour > 0) {
                                            add(
                                                CardChip(
                                                    text = "${Formatters.formatNumber(purchase.purchaseRatePerHour)} ${config.currencySymbol}/س",
                                                    container = MaterialTheme.colorScheme.surfaceVariant,
                                                    content = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                        if (purchase.wastedMinutesOnOwner > 0) {
                                            add(
                                                CardChip(
                                                    text = "هدر: ${Formatters.formatDurationCompact(purchase.wastedMinutesOnOwner)}",
                                                    container = MaterialTheme.mosarib.debt.copy(alpha = 0.12f),
                                                    content = MaterialTheme.mosarib.debt
                                                )
                                            )
                                        }
                                    }

                                    var purchaseMenuExpanded by remember(purchase.id) { mutableStateOf(false) }

                                    OperationCard(
                                        accent = MaterialTheme.mosarib.warning,
                                        icon = Icons.Default.AccessTime,
                                        title = "شراء ساعات",
                                        badges = emptyList(),
                                        amountText = "+ " + Formatters.formatCurrency(payableAmount, config.currencySymbol),
                                        amountInWords = Formatters.amountToArabicWords(payableAmount, config.currencySymbol),
                                        chips = chips,
                                        statement = statement,
                                        meta = meta,
                                        extraNote = extraNote,
                                        onClick = { purchaseForDetails = purchase },
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
                                                DropdownMenu(
                                                    expanded = purchaseMenuExpanded,
                                                    onDismissRequest = { purchaseMenuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("تعديل", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                        onClick = {
                                                            purchaseMenuExpanded = false
                                                            purchaseToEdit = purchase
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("مشاركة", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.mosarib.credit) },
                                                        onClick = {
                                                            purchaseMenuExpanded = false
                                                            messagePurchaseTarget = purchase
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("حذف", color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.mosarib.debt) },
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

                                is CustomerLedgerItem.SessionItem -> {
                                    val session = ledgerItem.session
                                    val invoiceCustomer = ledgerItem.originalCustomer ?: customer
                                    val invoiceSponsor = ledgerItem.sponsorOwner ?: if (customer.isWellOwner && ledgerItem.originalCustomer != null) customer else null
                                    val sessionDebt = getEffectiveSessionDebt(session)
                                    val hasLinkedPayments = ledgerItem.linkedVouchers.isNotEmpty() || session.amountPaid > 0.001
                                    val isExpanded = session.id in expandedSessionIds
                                    val statement = session.notes.trim().takeIf { it.isNotBlank() }
                                    val meta = listOfNotNull(Formatters.formatTime(session.startTime), "#SES-${session.id}").joinToString(" • ")

                                    val isSponsoredForFarmer = ledgerItem.originalCustomer != null
                                    val isSponsoredOnOwner = ledgerItem.sponsorOwner != null

                                    val badges = buildList {
                                        if (isSponsoredForFarmer) {
                                            add(CardBadge("للمزارع: ${ledgerItem.originalCustomer.name}", MaterialTheme.mosarib.info))
                                        } else if (isSponsoredOnOwner) {
                                            add(CardBadge("على المالك: ${ledgerItem.sponsorOwner.name}", MaterialTheme.mosarib.info))
                                        } else if (customer.isWellOwner) {
                                            add(CardBadge("مقاصة", MaterialTheme.mosarib.info))
                                        }
                                    }

                                    val chips = buildList {
                                        if (session.durationMinutes > 0) {
                                            add(
                                                CardChip(
                                                    text = Formatters.formatDurationCompact(session.durationMinutes),
                                                    container = MaterialTheme.mosarib.info.copy(alpha = 0.12f),
                                                    content = MaterialTheme.mosarib.info,
                                                    icon = Icons.Default.AccessTime
                                                )
                                            )
                                        }
                                        if (session.pricePerHour > 0) {
                                            add(
                                                CardChip(
                                                    text = "${Formatters.formatNumber(session.pricePerHour)} ${config.currencySymbol}/س",
                                                    container = MaterialTheme.colorScheme.surfaceVariant,
                                                    content = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                        if (sessionDebt <= 0.001) {
                                            add(
                                                CardChip(
                                                    text = "مسدد",
                                                    container = MaterialTheme.mosarib.successContainer,
                                                    content = MaterialTheme.mosarib.success,
                                                    icon = Icons.Default.Check
                                                )
                                            )
                                        } else {
                                            add(
                                                CardChip(
                                                    text = "متبقي ${Formatters.formatCurrency(sessionDebt, config.currencySymbol)}",
                                                    container = MaterialTheme.mosarib.debt.copy(alpha = 0.12f),
                                                    content = MaterialTheme.mosarib.debt
                                                )
                                            )
                                        }
                                    }

                                    var sessionMenuExpanded by remember(session.id) { mutableStateOf(false) }

                                    OperationCard(
                                        accent = MaterialTheme.mosarib.info,
                                        icon = Icons.Default.WaterDrop,
                                        title = "سقي",
                                        badges = badges,
                                        amountText = "− " + Formatters.formatCurrency(session.totalAmount, config.currencySymbol),
                                        amountInWords = Formatters.amountToArabicWords(session.totalAmount, config.currencySymbol),
                                        chips = chips,
                                        statement = statement,
                                        meta = meta,
                                        extraNote = extraNote,
                                        onClick = { sessionForDetails = session },
                                        actions = {
                                            if (sessionDebt > 0.001 && !customer.isWellOwner && ledgerItem.sponsorOwner == null) {
                                                FilledTonalButton(
                                                    onClick = { sessionToSettle = session },
                                                    modifier = Modifier.height(28.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                                    colors = ButtonDefaults.filledTonalButtonColors(
                                                        containerColor = MaterialTheme.mosarib.successContainer,
                                                        contentColor = MaterialTheme.mosarib.success
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "سداد",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }
                                            }
                                            if (hasLinkedPayments) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            expandedSessionIds = if (isExpanded) expandedSessionIds - session.id else expandedSessionIds + session.id
                                                        }
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isExpanded) "إخفاء" else "السداد (${if (session.amountPaid > 0) ledgerItem.linkedVouchers.size + 1 else 0})",
                                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    )
                                                    Icon(
                                                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
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
                                                DropdownMenu(
                                                    expanded = sessionMenuExpanded,
                                                    onDismissRequest = { sessionMenuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("مشاركة (واتساب)", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.mosarib.credit) },
                                                        onClick = {
                                                            sessionMenuExpanded = false
                                                            messageSessionTarget = session
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("تصدير PDF", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.mosarib.warning) },
                                                        onClick = {
                                                            sessionMenuExpanded = false
                                                            val file = PdfReportGenerator.generateSessionInvoicePdf(
                                                                context = context,
                                                                config = config,
                                                                customer = invoiceCustomer,
                                                                session = session,
                                                                sponsorCustomer = invoiceSponsor
                                                            )
                                                            localPdfReady = Pair(file, "فاتورة ري #${session.id}")
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("تعديل", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                        onClick = {
                                                            sessionMenuExpanded = false
                                                            sessionToEdit = session
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("حذف", color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.mosarib.debt) },
                                                        onClick = {
                                                            sessionMenuExpanded = false
                                                            sessionToDelete = session
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                        footer = if (hasLinkedPayments && isExpanded) {
                                            {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                                        .padding(10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "تفاصيل المبالغ المسددة لهذه الدورة:",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    )

                                                    val initialPaid = session.amountPaid - ledgerItem.linkedVouchers.sumOf { it.amount }
                                                    if (initialPaid > 0) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "• دفعة نقدية مسددة فوراً (${Formatters.formatDate(session.startTime)})",
                                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            )
                                                            Text(
                                                                text = Formatters.formatCurrency(initialPaid, config.currencySymbol),
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.mosarib.credit)
                                                            )
                                                        }
                                                    }

                                                    ledgerItem.linkedVouchers.forEach { v ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = "• سند قبض #${v.voucherNumber.ifEmpty { v.id.toString() }} (${Formatters.formatDateTime(v.date)})",
                                                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                IconButton(
                                                                    onClick = {
                                                                        messageCustomTarget = "سند قبض سداد #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nطريقة الدفع: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                                                                    },
                                                                    modifier = Modifier.size(20.dp)
                                                                ) {
                                                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                                }
                                                            }
                                                            Text(
                                                                text = "+ ${Formatters.formatCurrency(v.amount, config.currencySymbol)}",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.mosarib.credit)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else null
                                    )
                                }

                                is CustomerLedgerItem.ReceiptItem -> {
                                    val voucher = ledgerItem.voucher
                                    val isSessionSettlement = voucher.sessionId != null && voucher.sessionId > 0
                                    val title = if (isSessionSettlement) "سداد سقي" else "سند قبض"
                                    val statement = voucher.notes.trim().takeIf { it.isNotBlank() && it != "عام" }
                                    val rawRef = voucher.voucherNumber.trim().removePrefix("#")
                                    val cleanRef = if (rawRef.startsWith("REC-", ignoreCase = true)) "#$rawRef" else "#REC-$rawRef"
                                    val methodPart = voucher.paymentMethod.trim().takeIf { it.isNotBlank() } ?: "نقداً"
                                    val meta = listOfNotNull(Formatters.formatTime(voucher.date), methodPart, cleanRef).joinToString(" • ")

                                    var voucherMenuExpanded by remember(voucher.id) { mutableStateOf(false) }

                                    OperationCard(
                                        accent = MaterialTheme.mosarib.success,
                                        icon = Icons.Default.Payments,
                                        title = title,
                                        badges = emptyList(),
                                        amountText = "+ " + Formatters.formatCurrency(voucher.amount, config.currencySymbol),
                                        amountInWords = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol),
                                        statement = statement,
                                        meta = meta,
                                        extraNote = extraNote,
                                        onClick = { voucherForDetails = voucher },
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
                                                DropdownMenu(
                                                    expanded = voucherMenuExpanded,
                                                    onDismissRequest = { voucherMenuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("مشاركة (واتساب)", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.mosarib.credit) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            messageCustomTarget = "سند قبض #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)})\nطريقة الدفع: ${voucher.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(voucher.date)}"
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("تصدير PDF", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.mosarib.warning) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            val file = PdfReportGenerator.generateReceiptVoucherPdf(
                                                                context = context,
                                                                config = config,
                                                                customer = customer,
                                                                voucher = voucher
                                                            )
                                                            localPdfReady = Pair(file, "سند قبض #${voucher.voucherNumber}")
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("تعديل", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            voucherToEdit = voucher
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("حذف", color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.mosarib.debt) },
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

                                is CustomerLedgerItem.DisbursementItem -> {
                                    val voucher = ledgerItem.voucher
                                    val title = "سند صرف"
                                    val statement = voucher.notes.trim().takeIf { it.isNotBlank() && it != "عام" } ?: voucher.category.takeIf { it.isNotBlank() && it != "عام" }
                                    val rawRef = voucher.voucherNumber.trim().removePrefix("#")
                                    val cleanRef = if (rawRef.startsWith("EXP-", ignoreCase = true)) "#$rawRef" else "#EXP-$rawRef"
                                    val methodPart = voucher.paymentMethod.trim().takeIf { it.isNotBlank() } ?: "نقداً"
                                    val meta = listOfNotNull(Formatters.formatTime(voucher.date), methodPart, cleanRef).joinToString(" • ")

                                    var voucherMenuExpanded by remember(voucher.id) { mutableStateOf(false) }

                                    OperationCard(
                                        accent = MaterialTheme.mosarib.debt,
                                        icon = Icons.Default.ArrowDownward,
                                        title = title,
                                        badges = emptyList(),
                                        amountText = "− " + Formatters.formatCurrency(voucher.amount, config.currencySymbol),
                                        amountInWords = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol),
                                        statement = statement,
                                        meta = meta,
                                        extraNote = extraNote,
                                        onClick = { voucherForDetails = voucher },
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
                                                DropdownMenu(
                                                    expanded = voucherMenuExpanded,
                                                    onDismissRequest = { voucherMenuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("مشاركة (واتساب)", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.mosarib.credit) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            messageCustomTarget = "سند صرف #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ المصروف: ${Formatters.formatCurrency(voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)})\nالبيان: ${voucher.notes.ifBlank { voucher.category }}\nطريقة الصرف: ${voucher.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(voucher.date)}"
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("تصدير PDF", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.mosarib.warning) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            val file = PdfReportGenerator.generateExpenseVoucherPdf(
                                                                context = context,
                                                                config = config,
                                                                customer = customer,
                                                                voucher = voucher
                                                            )
                                                            localPdfReady = Pair(file, "سند صرف #${voucher.voucherNumber}")
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("تعديل", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            voucherToEdit = voucher
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("حذف", color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.mosarib.debt) },
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

                                is CustomerLedgerItem.DiscountItem -> {
                                    val voucher = ledgerItem.voucher
                                    val title = "سند تسوية وخصم"
                                    val statement = voucher.notes.trim().takeIf { it.isNotBlank() && it != "عام" } ?: voucher.category.takeIf { it.isNotBlank() && it != "عام" }
                                    val rawRef = voucher.voucherNumber.trim().removePrefix("#")
                                    val cleanRef = if (rawRef.startsWith("DSC-", ignoreCase = true)) "#$rawRef" else "#DSC-$rawRef"
                                    val meta = listOfNotNull(Formatters.formatTime(voucher.date), "تسوية", cleanRef).joinToString(" • ")

                                    var voucherMenuExpanded by remember(voucher.id) { mutableStateOf(false) }

                                    OperationCard(
                                        accent = MaterialTheme.mosarib.discount,
                                        icon = Icons.Default.Bookmarks,
                                        title = title,
                                        badges = listOf(CardBadge("خصم", MaterialTheme.mosarib.discount)),
                                        amountText = "+ " + Formatters.formatCurrency(voucher.amount, config.currencySymbol),
                                        amountInWords = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol),
                                        statement = statement,
                                        meta = meta,
                                        extraNote = extraNote,
                                        onClick = { voucherForDetails = voucher },
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
                                                DropdownMenu(
                                                    expanded = voucherMenuExpanded,
                                                    onDismissRequest = { voucherMenuExpanded = false }
                                                ) {
                                                    DropdownMenuItem(
                                                        text = { Text("مشاركة (واتساب)", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = MaterialTheme.mosarib.credit) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            messageCustomTarget = "سند تسوية وخصم #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}\nالعميل: ${customer.name}\nمبلغ الخصم: ${Formatters.formatCurrency(voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)})\nالبيان: ${voucher.notes.ifBlank { voucher.category }}\nالتاريخ: ${Formatters.formatDateTime(voucher.date)}"
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("تعديل", fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                                        onClick = {
                                                            voucherMenuExpanded = false
                                                            voucherToEdit = voucher
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("حذف", color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Bold) },
                                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.mosarib.debt) },
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
                            }
                        }
                    }
                }
            }

            // ============================================================
            // STICKY TOP APP BAR (يتلاشى خلفيته واسم العميل مع التمرير بسلاسة تامة)
            // ============================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(PrimaryTealDark.copy(alpha = scrollProgress))
                    .padding(top = statusBarPadding)
                    .height(52.dp)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }

                    // Compact Customer Name & Icon (fades in smoothly with scroll)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp)
                            .graphicsLayer { alpha = scrollProgress },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(avatarColor.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = avatarColor.onBackground
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val screenWidthDp = LocalConfiguration.current.screenWidthDp
                    val isCompactScreen = screenWidthDp < 400

                    // Action Icons: PDF, Edit, More Menu
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isCompactScreen) {
                            IconButton(
                                onClick = { viewModel.generateCustomerStatementPdf(customer) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = AccentGold)
                            }
                            IconButton(
                                onClick = { showEditCustomerSheet = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White)
                            }
                        }
                        Box {
                            IconButton(
                                onClick = { moreMenuExpanded = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "المزيد", tint = Color.White)
                            }

                            CustomerActionsDropdownMenu(
                                expanded = moreMenuExpanded,
                                onDismissRequest = { moreMenuExpanded = false },
                                customer = customer,
                                onCall = {
                                    moreMenuExpanded = false
                                    FileSharingHelper.makePhoneCall(context, customer.phone)
                                },
                                onSendMessage = {
                                    moreMenuExpanded = false
                                    messageCustomTarget = viewModel.buildCustomerStatementMessage(customer, customerWithBalance)
                                },
                                onPdf = {
                                    moreMenuExpanded = false
                                    viewModel.generateCustomerStatementPdf(customer)
                                },
                                onEdit = {
                                    moreMenuExpanded = false
                                    showEditCustomerSheet = true
                                },
                                onAddSession = {
                                    moreMenuExpanded = false
                                    showAddSessionSheet = true
                                },
                                onAddReceipt = {
                                    moreMenuExpanded = false
                                    showAddReceiptSheet = true
                                },
                                onAddDisbursement = {
                                    moreMenuExpanded = false
                                    showAddDisbursementSheet = true
                                },
                                onOwnerPurchase = {
                                    moreMenuExpanded = false
                                    showOwnerPurchaseSheet = true
                                },
                                onOwnerWaste = {
                                    moreMenuExpanded = false
                                    showOwnerWasteDialog = true
                                }
                            )
                        }
                    }
                }
            }

            LuxuryToastNotification(
                toast = toast,
                onDismiss = { viewModel.dismissToast() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 85.dp)
            )
        }
    }

    // Floating Detail BottomSheet for Water Session
    sessionForDetails?.let { s ->
        val linked = vouchers.filter { it.sessionId == s.id }
        val origCust = if (s.customerId != customerId) allCustomers.find { it.id == s.customerId } ?: customer else customer
        val sponsorCust = if (s.billedToCustomerId != null) allCustomers.find { it.id == s.billedToCustomerId } else null
        com.example.core.ui.SessionDetailBottomSheet(
            session = s,
            customer = origCust,
            currencySymbol = config.currencySymbol,
            linkedVouchers = linked,
            isWellOwner = customer.isWellOwner,
            sponsorCustomer = sponsorCust,
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
                    customer = origCust,
                    session = s,
                    sponsorCustomer = sponsorCust
                )
                localPdfReady = Pair(file, "فاتورة ري #${s.id}")
            },
            onMessage = {
                sessionForDetails = null
                messageSessionTarget = s
            },
            onSettle = if (s.remainingDebt > 0 && !customer.isWellOwner && s.billedToCustomerId == null) {
                {
                    sessionForDetails = null
                    sessionToSettle = s
                }
            } else null,
            onShareVoucher = { v ->
                messageCustomTarget = "سند قبض سداد #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nطريقة الدفع: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
            }
        )
    }

    // Floating Detail BottomSheet for Voucher (Receipt / Expense / Discount)
    voucherForDetails?.let { v ->
        com.example.core.ui.VoucherDetailBottomSheet(
            voucher = v,
            customer = customer,
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
                        customer = customer,
                        voucher = v
                    )
                } else {
                    PdfReportGenerator.generateExpenseVoucherPdf(
                        context = context,
                        config = config,
                        customer = customer,
                        voucher = v
                    )
                }
                localPdfReady = Pair(file, "سند #${v.voucherNumber.ifEmpty { v.id.toString() }}")
            },
            onShare = {
                voucherForDetails = null
                val shareText = if (v.type == VoucherType.RECEIPT) {
                    "سند قبض #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nطريقة الدفع: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                } else {
                    "سند صرف #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ المصروف: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nالبيان: ${v.notes.ifBlank { v.category }}\nطريقة الصرف: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                }
                messageCustomTarget = shareText
            }
        )
    }

    // Floating Detail BottomSheet for WellOwnerPurchase
    purchaseForDetails?.let { p ->
        com.example.core.ui.WellOwnerPurchaseDetailBottomSheet(
            purchase = p,
            owner = customer,
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
                messagePurchaseTarget = p
            }
        )
    }

    // Settle Session Dialog
    sessionToSettle?.let { s ->
        SettleSessionDialog(
            session = s,
            customerName = customer.name,
            currencySymbol = config.currencySymbol,
            effectiveDebt = getEffectiveSessionDebt(s),
            onDismiss = { sessionToSettle = null },
            onConfirmSettle = { amount, discount, method, notes ->
                viewModel.settleSessionDebt(s, amount, method, notes, discount)
            }
        )
    }

    // Delete Session Confirmation
    sessionToDelete?.let { s ->
        com.example.core.ui.MosaribActionConfirmDialog(
            title = "تأكيد حذف دورة الماء",
            message = "هل أنت متأكد من حذف هذه الدورة؟ سيتم إلغاء المبالغ المقيدة بها وتحديث الحساب.",
            confirmText = "حذف الدورة",
            onConfirm = {
                viewModel.deleteSession(s)
                sessionToDelete = null
            },
            onDismiss = { sessionToDelete = null }
        )
    }

    // Delete owner-purchase confirmation
    purchaseToDelete?.let { purchase ->
        com.example.core.ui.MosaribActionConfirmDialog(
            title = "حذف عملية شراء الساعات",
            message = "سيُعاد احتساب مستحق صاحب البئر بعد حذف هذه الحركة. هل تريد المتابعة؟",
            confirmText = "حذف الحركة",
            onConfirm = {
                viewModel.deleteOwnerPurchase(purchase)
                purchaseToDelete = null
            },
            onDismiss = { purchaseToDelete = null }
        )
    }

    // Delete Voucher Confirmation
    voucherToDelete?.let { v ->
        com.example.core.ui.MosaribActionConfirmDialog(
            title = "تأكيد حذف السند",
            message = "هل أنت متأكد من حذف هذا السند؟ سيتم تحديث رصيد العميل فوراً.",
            confirmText = "حذف السند",
            onConfirm = {
                viewModel.deleteVoucher(v)
                voucherToDelete = null
            },
            onDismiss = { voucherToDelete = null }
        )
    }

    // Add/Edit Receipt Voucher Sheet (نموذج سند القبض الموحد من شاشة العمليات)
    if (showAddReceiptSheet || voucherToEdit != null) {
        val editingVoucher = voucherToEdit
        AddVoucherBottomSheet(
            initialVoucher = editingVoucher ?: Voucher(
                voucherNumber = "",
                type = VoucherType.RECEIPT,
                customerId = customer.id,
                amount = 0.0,
                category = "سداد حساب",
                paymentMethod = "نقداً",
                date = System.currentTimeMillis()
            ),
            initialType = editingVoucher?.type ?: VoucherType.RECEIPT,
            customers = if (activeCustomers.isNotEmpty()) activeCustomers else listOf(customer),
            allSessions = sessions,
            currencySymbol = config.currencySymbol,
            onDismiss = {
                showAddReceiptSheet = false
                voucherToEdit = null
            },
            onSave = { type, targetCustomerId, amount, category, method, notes, sessionId, selectedSessionIds, discountAmount ->
                if (editingVoucher != null) {
                    vouchersViewModel.updateVoucher(
                        voucher = editingVoucher,
                        amount = amount,
                        paymentMethod = method,
                        notes = notes,
                        category = category,
                        customerId = targetCustomerId ?: customer.id,
                        type = type
                    )
                    voucherToEdit = null
                } else {
                    vouchersViewModel.addVoucher(
                        type = type,
                        customerId = targetCustomerId ?: customer.id,
                        amount = amount,
                        category = category,
                        paymentMethod = method,
                        notes = notes,
                        sessionId = sessionId,
                        selectedSessionIds = selectedSessionIds,
                        discountAmount = discountAmount
                    )
                    showAddReceiptSheet = false
                }
            }
        )
    }

    // Add Disbursement Sheet (نموذج سند الصرف الموحد)
    if (showAddDisbursementSheet) {
        AddVoucherBottomSheet(
            initialVoucher = Voucher(
                voucherNumber = "",
                type = VoucherType.EXPENSE,
                customerId = customer.id,
                amount = 0.0,
                category = if (customer.isWellOwner) "مستحقات صاحب بئر" else "صرف عام",
                paymentMethod = "نقداً",
                date = System.currentTimeMillis()
            ),
            initialType = VoucherType.EXPENSE,
            customers = if (activeCustomers.isNotEmpty()) activeCustomers else listOf(customer),
            allSessions = sessions,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddDisbursementSheet = false },
            onSave = { type, targetCustomerId, amount, category, method, notes, sessionId, selectedSessionIds, discountAmount ->
                vouchersViewModel.addVoucher(
                    type = type,
                    customerId = targetCustomerId ?: customer.id,
                    amount = amount,
                    category = category,
                    paymentMethod = method,
                    notes = notes,
                    sessionId = sessionId,
                    selectedSessionIds = selectedSessionIds,
                    discountAmount = discountAmount
                )
                showAddDisbursementSheet = false
            }
        )
    }

    if ((showOwnerPurchaseSheet || purchaseToEdit != null) && customer.isWellOwner) {
        val editing = purchaseToEdit
        WellOwnerPurchaseBottomSheet(
            owner = customer,
            currencySymbol = config.currencySymbol,
            initialPurchase = editing,
            onDismiss = {
                showOwnerPurchaseSheet = false
                purchaseToEdit = null
            },
            onSave = { date, durationMinutes, wastedMinutes, purchaseRate, amountPaid, notes, startTime, endTime ->
                if (editing != null) {
                    viewModel.updateOwnerPurchase(
                        purchaseId = editing.id,
                        ownerCustomerId = customer.id,
                        date = date,
                        durationMinutes = durationMinutes,
                        wastedMinutesOnOwner = wastedMinutes,
                        purchaseRatePerHour = purchaseRate,
                        notes = notes,
                        amountPaid = amountPaid,
                        startTime = startTime,
                        endTime = endTime
                    )
                    showOwnerPurchaseSheet = false
                    messagePurchaseTarget = editing.copy(
                        date = date,
                        durationMinutes = durationMinutes,
                        wastedMinutesOnOwner = wastedMinutes,
                        purchaseRatePerHour = purchaseRate,
                        notes = notes,
                        amountPaid = amountPaid,
                        startTime = startTime,
                        endTime = endTime
                    )
                    purchaseToEdit = null
                } else {
                    viewModel.addOwnerPurchase(
                        ownerCustomerId = customer.id,
                        date = date,
                        durationMinutes = durationMinutes,
                        wastedMinutesOnOwner = wastedMinutes,
                        purchaseRatePerHour = purchaseRate,
                        notes = notes,
                        amountPaid = amountPaid,
                        startTime = startTime,
                        endTime = endTime,
                        onCreated = { createdPurchase ->
                            messagePurchaseTarget = createdPurchase
                        }
                    )
                    showOwnerPurchaseSheet = false
                }
            }
        )
    }

    // تسجيل أو تعديل دورة سقي مباشرة داخل صفحة العميل
    if (showAddSessionSheet || sessionToEdit != null) {
        val editingSession = sessionToEdit
        AddEditSessionBottomSheet(
            initialSession = editingSession,
            customers = activeCustomers,
            defaultCustomerId = editingSession?.customerId ?: customer.id,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { viewModel.insertCustomerDirect(it) },
            onDismiss = {
                showAddSessionSheet = false
                sessionToEdit = null
            },
            onSave = { id, custId, pumpName, startTime, endTime, hours, minutes, pricePerHour, amountPaid, notes, billedToCustomerId, wastedMinutes, wastedReason, discountAmount, costPricePerHour, pumpSourceId ->
                if (editingSession != null) {
                    viewModel.updateSessionForCustomer(
                        id = id,
                        customerId = custId,
                        pumpName = pumpName,
                        startTime = startTime,
                        endTime = endTime,
                        hours = hours,
                        minutes = minutes,
                        pricePerHour = pricePerHour,
                        amountPaid = amountPaid,
                        notes = notes,
                        billedToCustomerId = billedToCustomerId,
                        wastedMinutes = wastedMinutes,
                        wastedReason = wastedReason,
                        discountAmount = discountAmount,
                        costPricePerHour = costPricePerHour,
                        pumpSourceId = pumpSourceId
                    )
                    showAddSessionSheet = false
                    sessionToEdit = null
                } else {
                    viewModel.addSessionForCustomer(
                        customerId = custId,
                        pumpName = pumpName,
                        startTime = startTime,
                        endTime = endTime,
                        hours = hours,
                        minutes = minutes,
                        pricePerHour = pricePerHour,
                        amountPaid = amountPaid,
                        notes = notes,
                        billedToCustomerId = billedToCustomerId,
                        wastedMinutes = wastedMinutes,
                        wastedReason = wastedReason,
                        discountAmount = discountAmount,
                        costPricePerHour = costPricePerHour,
                        pumpSourceId = pumpSourceId
                    )
                    showAddSessionSheet = false
                }
            }
        )
    }

    // نافذة تسجيل هدر على صاحب البئر
    if (showOwnerWasteDialog && customer.isWellOwner) {
        val latestPurchase = purchases.maxByOrNull { it.date }
        if (latestPurchase == null) {
            AlertDialog(
                onDismissRequest = { showOwnerWasteDialog = false },
                title = { Text("لا توجد مشتريات سابقة", fontWeight = FontWeight.Bold) },
                text = { Text("يجب تسجيل عملية شراء ساعات أولاً ليتم خصم الهدر منها لصالح المسرب.") },
                confirmButton = {
                    TextButton(onClick = { showOwnerWasteDialog = false }) {
                        Text("حسناً")
                    }
                }
            )
        } else {
            val availableMinutes = (latestPurchase.durationMinutes - latestPurchase.wastedMinutesOnOwner).coerceAtLeast(0)
            var wasteHoursText by remember { mutableStateOf("") }
            var wasteMinutesText by remember { mutableStateOf("") }
            var wasteReason by remember { mutableStateOf("") }

            val newWasteMinutes by remember(wasteHoursText, wasteMinutesText) {
                derivedStateOf {
                    (wasteHoursText.toIntOrNull()?.coerceAtLeast(0) ?: 0) * 60 +
                        (wasteMinutesText.toIntOrNull()?.coerceIn(0, 59) ?: 0)
                }
            }
            val isExceeded by remember(newWasteMinutes, availableMinutes) {
                derivedStateOf { newWasteMinutes > availableMinutes }
            }
            val canConfirm = newWasteMinutes in 1..availableMinutes

            AlertDialog(
                onDismissRequest = { showOwnerWasteDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFE65100))
                        Spacer(Modifier.width(8.dp))
                        Text("تسجيل هدر للبئر", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "يُخصم من آخر شراء (${Formatters.formatDate(latestPurchase.date)}) بسعر (${Formatters.formatCurrency(latestPurchase.purchaseRatePerHour, config.currencySymbol)}/ساعة).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("مدة آخر شراء:")
                                    Text(Formatters.formatDurationArabic(latestPurchase.durationMinutes), fontWeight = FontWeight.Bold)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("الهدر السابق المسجل:")
                                    Text(Formatters.formatDurationArabic(latestPurchase.wastedMinutesOnOwner), color = Color(0xFFD32F2F))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("أقصى هدر متاح للخصم:", fontWeight = FontWeight.Bold)
                                    Text(Formatters.formatDurationArabic(availableMinutes), color = PrimaryTeal, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = wasteHoursText,
                                onValueChange = { wasteHoursText = it.filter(Char::isDigit).take(2) },
                                label = { Text("ساعات الهدر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = wasteMinutesText,
                                onValueChange = { wasteMinutesText = it.filter(Char::isDigit).take(2) },
                                label = { Text("دقائق الهدر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        if (isExceeded) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                border = BorderStroke(1.dp, Color(0xFFF87171)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "لا يمكن أن يكون الهدر أكبر من عملية الشراء! الحد المتاح: ${Formatters.formatDurationArabic(availableMinutes)}",
                                    color = Color(0xFFB91C1C),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = wasteReason,
                            onValueChange = { wasteReason = it },
                            label = { Text("سبب الهدر (اختياري)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (canConfirm) {
                                viewModel.recordWasteOnOwner(customer.id, newWasteMinutes, wasteReason)
                                showOwnerWasteDialog = false
                            }
                        },
                        enabled = canConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                    ) {
                        Text("خصم الهدر من المستحق", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOwnerWasteDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Edit Customer Sheet
    if (showEditCustomerSheet) {
        AddEditCustomerBottomSheet(
            initialCustomer = customer,
            existingGroups = customGroups,
            currencySymbol = config.currencySymbol,
            onDismiss = { showEditCustomerSheet = false },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup)
                showEditCustomerSheet = false
            }
        )
    }
}

private data class StatCellData(
    val label: String,
    val value: String,
    val color: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.White,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = androidx.compose.ui.Modifier.clickable { onClick() }
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .size(48.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = androidx.compose.ui.Modifier.size(22.dp)
            )
        }
        Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

@Composable
private fun StatCellBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CustomerActionsDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    customer: Customer,
    onCall: () -> Unit,
    onSendMessage: () -> Unit,
    onPdf: () -> Unit,
    onEdit: () -> Unit,
    onAddSession: () -> Unit,
    onAddReceipt: () -> Unit,
    onAddDisbursement: () -> Unit,
    onOwnerPurchase: () -> Unit,
    onOwnerWaste: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp)
    ) {
        if (customer.isWellOwner) {
            DropdownMenuItem(
                text = { Text("شراء ساعات", fontWeight = FontWeight.Bold) },
                onClick = onOwnerPurchase,
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("سقي له", fontWeight = FontWeight.Bold) },
                onClick = onAddSession,
                leadingIcon = {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = SecondaryAquaDark, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("تسجيل هدر", fontWeight = FontWeight.Bold) },
                onClick = onOwnerWaste,
                leadingIcon = {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFFFB74D), modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("سند صرف (عليه)", fontWeight = FontWeight.Bold) },
                onClick = onAddDisbursement,
                leadingIcon = {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("سند قبض (له)", fontWeight = FontWeight.Bold) },
                onClick = onAddReceipt,
                leadingIcon = {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                }
            )
            if (customer.phone.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("اتصال هاتفياً", fontWeight = FontWeight.Bold) },
                    onClick = onCall,
                    leadingIcon = {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    }
                )
                DropdownMenuItem(
                    text = { Text("إرسال كشف حساب", fontWeight = FontWeight.Bold) },
                    onClick = onSendMessage,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
                    }
                )
            }
        } else {
            if (customer.phone.isNotBlank()) {
                DropdownMenuItem(
                    text = { Text("اتصال", fontWeight = FontWeight.Bold) },
                    onClick = onCall,
                    leadingIcon = {
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    }
                )
                DropdownMenuItem(
                    text = { Text("إرسال كشف", fontWeight = FontWeight.Bold) },
                    onClick = onSendMessage,
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("سقي جديد", fontWeight = FontWeight.Bold) },
                onClick = onAddSession,
                leadingIcon = {
                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("سند قبض (له)", fontWeight = FontWeight.Bold) },
                onClick = onAddReceipt,
                leadingIcon = {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(20.dp))
                }
            )
            DropdownMenuItem(
                text = { Text("سند صرف (عليه)", fontWeight = FontWeight.Bold) },
                onClick = onAddDisbursement,
                leadingIcon = {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(20.dp))
                }
            )
        }

        DropdownMenuItem(
            text = { Text("كشف حساب PDF", fontWeight = FontWeight.Bold) },
            onClick = onPdf,
            leadingIcon = {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
            }
        )

        DropdownMenuItem(
            text = { Text("تعديل البيانات", fontWeight = FontWeight.Bold) },
            onClick = onEdit,
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
            }
        )
    }
}

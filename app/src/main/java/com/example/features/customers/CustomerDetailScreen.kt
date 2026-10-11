package com.example.features.customers

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.core.util.PdfReportGenerator
import com.example.features.sessions.SettleSessionDialog
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import com.example.features.wellowners.WellOwnerPurchaseBottomSheet
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.SecondaryAquaDark
import java.io.File

enum class CustomerOpFilter(val title: String) {
    ALL("الكل"),
    PURCHASES("شراء ساعات"),
    SESSIONS("السقي / البيع"),
    RECEIPTS("التحصيل"),
    DISBURSEMENTS("السداد"),
    DEBT_ONLY("المؤخر")
}

sealed class CustomerLedgerItem {
    abstract val timestamp: Long

    data class SessionItem(
        val session: WaterSession,
        val originalCustomer: Customer? = null,
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: CustomersViewModel,
    onBack: () -> Unit,
    onAddSessionForCustomer: (Customer) -> Unit
) {
    BackHandler { onBack() }

    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val allCustomersWithBalance by viewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    val customerWithBalance = allCustomersWithBalance.find { it.customer.id == customerId }
    val allCustomers: List<Customer> by viewModel.allCustomers.collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by remember(customerId) { viewModel.getCustomerSessions(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val purchases by remember(customerId) { viewModel.getCustomerPurchases(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val vouchers by remember(customerId) { viewModel.getCustomerVouchers(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
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

    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    var localPdfReady by remember { mutableStateOf<Pair<File, String>?>(null) }
    var messageSessionTarget by remember { mutableStateOf<WaterSession?>(null) }
    var messageCustomTarget by remember { mutableStateOf<String?>(null) }
    val activePdf = pdfReady ?: localPdfReady

    // PDF Viewer / Share Dialog
    activePdf?.let { (file, title) ->
        AlertDialog(
            onDismissRequest = {
                viewModel.clearPdfReady()
                localPdfReady = null
            },
            title = { Text("المستند جاهز", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في فتح وعرض الملف مباشرة أم مشاركته؟") },
            confirmButton = {
                Button(
                    onClick = {
                        FileSharingHelper.openPdf(context, file)
                        viewModel.clearPdfReady()
                        localPdfReady = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح / عرض", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        FileSharingHelper.sharePdf(context, file, title)
                        viewModel.clearPdfReady()
                        localPdfReady = null
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة", fontWeight = FontWeight.Bold)
                }
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

    // Message Choice Dialog (WhatsApp / SMS)
    messageSessionTarget?.let { s ->
        val timeRange = "من ${Formatters.formatTime(s.startTime)} إلى ${Formatters.formatTime(s.endTime)}"
        val debtStatus = if (s.remainingDebt > 0) "المتبقي: ${Formatters.formatCurrency(s.remainingDebt, config.currencySymbol)}" else "خالص ومسدد بالكامل"
        val msg = """
*فاتورة ري - ${config.distributorName.ifEmpty { "المُسَرِّب" }}*
👤 العميل: ${customer.name}${if (customer.farmName.isNotEmpty()) " (${customer.farmName})" else ""}
⏱️ الوقت: $timeRange (${Formatters.formatDurationArabic(s.durationMinutes)})
💵 المبلغ: ${Formatters.formatCurrency(s.totalAmount, config.currencySymbol)} | مسدد: ${Formatters.formatCurrency(s.amountPaid, config.currencySymbol)}
📊 الحالة: $debtStatus
📅 التاريخ: ${Formatters.formatDate(s.startTime)}${FileSharingHelper.MESSAGE_FOOTER}
        """.trimIndent()

        SendMessageChoiceDialog(
            recipientName = customer.name,
            recipientPhone = customer.phone,
            messageText = msg,
            onDismiss = { messageSessionTarget = null },
            onSendWhatsApp = {
                messageSessionTarget = null
                FileSharingHelper.sendWhatsAppMessage(context, customer.phone, msg)
            },
            onSendSms = {
                messageSessionTarget = null
                FileSharingHelper.sendSms(context, customer.phone, msg)
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
            val linked = vouchers.filter { it.sessionId == s.id }
            CustomerLedgerItem.SessionItem(s, origCust, linked)
        }
    }
    val purchaseItems = remember(purchases) { purchases.map { CustomerLedgerItem.PurchaseItem(it) } }
    val receiptItems = remember(vouchers) {
        vouchers.filter { it.type == VoucherType.RECEIPT }.map { CustomerLedgerItem.ReceiptItem(it) }
    }
    val standaloneReceiptItems = remember(receiptItems) {
        receiptItems.filter { it.voucher.sessionId == null }
    }
    val disbursementItems = remember(vouchers) {
        vouchers.filter { it.type == VoucherType.EXPENSE }.map { CustomerLedgerItem.DisbursementItem(it) }
    }

    val filteredItems = remember(selectedFilter, sessionItems, purchaseItems, receiptItems, disbursementItems) {
        when (selectedFilter) {
            CustomerOpFilter.ALL -> (sessionItems + purchaseItems + standaloneReceiptItems + disbursementItems).sortedByDescending { it.timestamp }
            CustomerOpFilter.PURCHASES -> purchaseItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.SESSIONS -> sessionItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.RECEIPTS -> receiptItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.DISBURSEMENTS -> disbursementItems.sortedByDescending { it.timestamp }
            CustomerOpFilter.DEBT_ONLY -> sessionItems.filter { it.session.remainingDebt > 0 }.sortedByDescending { it.timestamp }
        }
    }

    val unsettledCount = remember(sessions) { sessions.count { it.remainingDebt > 0 } }

    Scaffold(
        floatingActionButton = {
            customerWithBalance?.let { cwb ->
                ExtendedFloatingActionButton(
                    onClick = { showAddSessionSheet = true },
                    icon = {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = if (cwb.customer.isWellOwner) "تسجيل سقي له" else "تسجيل سقي للمزارع",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    },
                    containerColor = PrimaryTeal,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 90.dp)
            ) {
                // ============================================================
                // 1. GRADIENT HERO HEADER
                // ============================================================
                item {
                    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        PrimaryTealDark,
                                        SecondaryAquaDark,
                                        PrimaryTeal.copy(alpha = 0.90f)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = statusBarPadding)
                        ) {
                            // Back + Edit + PDF buttons on top
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, start = 8.dp, end = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                            }

                            // Avatar + Name + sub-info + Quick Actions
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Compact Avatar
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = customer.name.take(1),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

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
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.75f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                if (customer.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = customer.notes,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White.copy(alpha = 0.65f)
                                        ),
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 30.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ── Quick Action Buttons ──
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (customer.isWellOwner) {
                                        QuickActionButton(
                                            icon = Icons.Default.Add,
                                            label = "شراء ساعات",
                                            accentColor = AccentGold,
                                            onClick = { showOwnerPurchaseSheet = true }
                                        )
                                        QuickActionButton(
                                            icon = Icons.Default.WaterDrop,
                                            label = "سقي له",
                                            accentColor = SecondaryAquaDark,
                                            onClick = { showAddSessionSheet = true }
                                        )
                                        QuickActionButton(
                                            icon = Icons.Default.AccessTime,
                                            label = "تسجيل هدر",
                                            accentColor = Color(0xFFFFB74D),
                                            onClick = { showOwnerWasteDialog = true }
                                        )
                                        QuickActionButton(
                                            icon = Icons.Default.ArrowDownward,
                                            label = "سداد له",
                                            accentColor = Color(0xFFFF8A80),
                                            onClick = { showAddDisbursementSheet = true }
                                        )
                                        QuickActionButton(
                                            icon = Icons.Default.Payments,
                                            label = "تحصيل منه",
                                            accentColor = AccentEmerald,
                                            onClick = { showAddReceiptSheet = true }
                                        )
                                    } else {
                                        if (customer.phone.isNotEmpty()) {
                                            QuickActionButton(
                                                icon = Icons.Default.Call,
                                                label = "اتصال",
                                                onClick = { FileSharingHelper.makePhoneCall(context, customer.phone) }
                                            )
                                            QuickActionButton(
                                                icon = Icons.AutoMirrored.Filled.Send,
                                                label = "إرسال كشف",
                                                accentColor = Color(0xFF69F0AE),
                                                onClick = {
                                                    messageCustomTarget = viewModel.buildCustomerStatementMessage(customer, customerWithBalance)
                                                }
                                            )
                                        }
                                        QuickActionButton(
                                            icon = Icons.Default.Payments,
                                            label = "تحصيل",
                                            accentColor = AccentEmerald,
                                            onClick = { showAddReceiptSheet = true }
                                        )
                                        QuickActionButton(
                                            icon = Icons.Default.ArrowDownward,
                                            label = "سند صرف",
                                            accentColor = Color(0xFFFF8A80),
                                            onClick = { showAddDisbursementSheet = true }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ============================================================
                // 2. BALANCE + TOTALS CARD (floating over the gradient)
                // ============================================================
                item {
                    val isDebt = customerWithBalance.balance > 0
                    val isCredit = customerWithBalance.balance < 0

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
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
                                    else -> "حساب صاحب البئر مسدد وخالص بالكامل"
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
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("مشتريات ساعات", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp))
                                        Text(Formatters.formatCurrency(customerWithBalance.totalPurchaseAmount, config.currencySymbol), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentGold))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("مسدد نقداً له", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp))
                                        Text(Formatters.formatCurrency(customerWithBalance.totalDisbursedAmount, config.currencySymbol), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE53935)))
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("سقي على حسابه", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp))
                                        Text(Formatters.formatCurrency(customerWithBalance.totalBilledAmount, config.currencySymbol), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal))
                                    }
                                }
                            } else {
                                val isDebt = customerWithBalance.receivableBalance > 0
                                val isCredit = customerWithBalance.receivableBalance < 0
                                Text(
                                    text = when {
                                        isDebt -> "صافي الدين المتبقي بذمة العميل (لك)"
                                        isCredit -> "رصيد دائن لصالح العميل (عليك)"
                                        else -> "الحساب مسدد بالكامل (لا توجد مطالبات)"
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
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("خالص ومسدد بالكامل", style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 2x2 Grid of Financial Indicators
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCellBox(
                                        modifier = Modifier.weight(1f),
                                        label = if (customer.isWellOwner) "قيمة الشراء الصافية" else "إجمالي السقي",
                                        value = Formatters.formatCurrency(
                                            if (customer.isWellOwner) customerWithBalance.totalPurchaseAmount else customerWithBalance.totalBilledAmount,
                                            config.currencySymbol
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        icon = Icons.Default.WaterDrop
                                    )
                                    StatCellBox(
                                        modifier = Modifier.weight(1f),
                                        label = if (customer.isWellOwner) "الساعات المشتراة" else "ساعات البيع",
                                        value = Formatters.formatDurationArabic(
                                            if (customer.isWellOwner) customerWithBalance.totalPurchasedMinutes else customerWithBalance.totalSoldMinutes
                                        ),
                                        color = PrimaryTeal,
                                        icon = Icons.Default.AccessTime
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCellBox(
                                        modifier = Modifier.weight(1f),
                                        label = if (customer.isWellOwner) "المسدد له" else "المحصل نقداً",
                                        value = Formatters.formatCurrency(
                                            if (customer.isWellOwner) customerWithBalance.totalDisbursedAmount else customerWithBalance.totalPaidAmount,
                                            config.currencySymbol
                                        ),
                                        color = if (customer.isWellOwner) Color(0xFFEF5350) else AccentEmerald,
                                        icon = Icons.Default.Payments
                                    )
                                    if (customer.isWellOwner || customerWithBalance.totalDisbursedAmount > 0) {
                                        StatCellBox(
                                            modifier = Modifier.weight(1f),
                                            label = if (customer.isWellOwner) "المحصل منه" else "إجمالي المنصرف",
                                            value = Formatters.formatCurrency(
                                                if (customer.isWellOwner) customerWithBalance.totalPaidAmount else customerWithBalance.totalDisbursedAmount,
                                                config.currencySymbol
                                            ),
                                            color = if (customer.isWellOwner) AccentEmerald else Color(0xFFEF5350),
                                            icon = Icons.Default.ArrowDownward
                                        )
                                    } else {
                                        StatCellBox(
                                            modifier = Modifier.weight(1f),
                                            label = "عدد دورات السقي",
                                            value = "${sessions.size} دورة",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            icon = Icons.Default.Receipt
                                        )
                                    }
                                }
                                if (customer.isWellOwner) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatCellBox(
                                            modifier = Modifier.weight(1f),
                                            label = "سقي / بيع له",
                                            value = Formatters.formatCurrency(customerWithBalance.totalBilledAmount, config.currencySymbol),
                                            color = PrimaryTeal,
                                            icon = Icons.Default.WaterDrop
                                        )
                                        StatCellBox(
                                            modifier = Modifier.weight(1f),
                                            label = "ساعات السقي له",
                                            value = Formatters.formatDurationArabic(customerWithBalance.totalSoldMinutes),
                                            color = PrimaryTeal,
                                            icon = Icons.Default.AccessTime
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatCellBox(
                                            modifier = Modifier.weight(1f),
                                            label = "هدر على صاحب البئر",
                                            value = Formatters.formatDurationArabic(
                                                (customerWithBalance.totalPurchasedMinutes - customerWithBalance.totalChargeablePurchasedMinutes).coerceAtLeast(0)
                                            ),
                                            color = Color(0xFFD32F2F),
                                            icon = Icons.Default.AccessTime
                                        )
                                        StatCellBox(
                                            modifier = Modifier.weight(1f),
                                            label = "خصم الهدر عليه",
                                            value = Formatters.formatCurrency(customerWithBalance.totalOwnerWasteCredit, config.currencySymbol),
                                            color = Color(0xFFD32F2F),
                                            icon = Icons.Default.ArrowDownward
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
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
                                CustomerOpFilter.ALL -> filteredItems.size
                                CustomerOpFilter.PURCHASES -> purchaseItems.size
                                CustomerOpFilter.SESSIONS -> sessions.size
                                CustomerOpFilter.RECEIPTS -> receiptItems.size
                                CustomerOpFilter.DISBURSEMENTS -> disbursementItems.size
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
                    items(filteredItems, key = { item ->
                        when (item) {
                            is CustomerLedgerItem.PurchaseItem -> "purchase_${item.purchase.id}"
                            is CustomerLedgerItem.SessionItem -> "sess_${item.session.id}"
                            is CustomerLedgerItem.ReceiptItem -> "rec_${item.voucher.id}"
                            is CustomerLedgerItem.DisbursementItem -> "disb_${item.voucher.id}"
                        }
                    }) { ledgerItem ->
                        when (ledgerItem) {
                            is CustomerLedgerItem.PurchaseItem -> {
                                CustomerPurchaseCardItem(
                                    purchase = ledgerItem.purchase,
                                    currencySymbol = config.currencySymbol,
                                    onEditClick = { purchaseToEdit = ledgerItem.purchase },
                                    onDeleteClick = { purchaseToDelete = ledgerItem.purchase }
                                )
                            }
                            is CustomerLedgerItem.SessionItem -> {
                                CustomerSessionCardItem(
                                    session = ledgerItem.session,
                                    originalCustomer = ledgerItem.originalCustomer,
                                    isBeneficiaryView = false,
                                    currencySymbol = config.currencySymbol,
                                    linkedVouchers = ledgerItem.linkedVouchers,
                                    onEditClick = { sessionToEdit = ledgerItem.session },
                                    onPdfClick = {
                                        val file = PdfReportGenerator.generateSessionInvoicePdf(
                                            context = context,
                                            config = config,
                                            // This detail screen represents the billed account (including a well owner).
                                            customer = customer,
                                            session = ledgerItem.session
                                        )
                                        localPdfReady = Pair(file, "فاتورة ري #${ledgerItem.session.id}")
                                    },
                                    onMessageClick = {
                                        messageSessionTarget = ledgerItem.session
                                    },
                                    onSettleClick = {
                                        sessionToSettle = ledgerItem.session
                                    },
                                    onDeleteClick = {
                                        sessionToDelete = ledgerItem.session
                                    },
                                    onShareVoucher = { v ->
                                        messageCustomTarget = "سند قبض سداد #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nطريقة الدفع: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                                    }
                                )
                            }
                            is CustomerLedgerItem.ReceiptItem -> {
                                CustomerReceiptCardItem(
                                    voucher = ledgerItem.voucher,
                                    currencySymbol = config.currencySymbol,
                                    isWellOwner = customer.isWellOwner,
                                    onPdfClick = {
                                        val file = PdfReportGenerator.generateReceiptVoucherPdf(
                                            context = context,
                                            config = config,
                                            customer = customer,
                                            voucher = ledgerItem.voucher
                                        )
                                        localPdfReady = Pair(file, "سند قبض #${ledgerItem.voucher.voucherNumber}")
                                    },
                                    onShareWhatsApp = {
                                        messageCustomTarget = "سند قبض #${ledgerItem.voucher.voucherNumber.ifEmpty { ledgerItem.voucher.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(ledgerItem.voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(ledgerItem.voucher.amount, config.currencySymbol)})\nطريقة الدفع: ${ledgerItem.voucher.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(ledgerItem.voucher.date)}"
                                    },
                                    onDeleteClick = {
                                        voucherToDelete = ledgerItem.voucher
                                    }
                                )
                            }
                            is CustomerLedgerItem.DisbursementItem -> {
                                CustomerDisbursementCardItem(
                                    voucher = ledgerItem.voucher,
                                    currencySymbol = config.currencySymbol,
                                    isWellOwner = customer.isWellOwner,
                                    onPdfClick = {
                                        val file = PdfReportGenerator.generateExpenseVoucherPdf(
                                            context = context,
                                            config = config,
                                            customer = customer,
                                            voucher = ledgerItem.voucher
                                        )
                                        localPdfReady = Pair(file, "سند صرف #${ledgerItem.voucher.voucherNumber}")
                                    },
                                    onShareWhatsApp = {
                                        messageCustomTarget = "سند صرف #${ledgerItem.voucher.voucherNumber.ifEmpty { ledgerItem.voucher.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ المصروف: ${Formatters.formatCurrency(ledgerItem.voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(ledgerItem.voucher.amount, config.currencySymbol)})\nالبيان: ${ledgerItem.voucher.notes.ifBlank { ledgerItem.voucher.category }}\nطريقة الصرف: ${ledgerItem.voucher.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(ledgerItem.voucher.date)}"
                                    },
                                    onDeleteClick = {
                                        voucherToDelete = ledgerItem.voucher
                                    }
                                )
                            }
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

    // Settle Session Dialog
    sessionToSettle?.let { s ->
        SettleSessionDialog(
            session = s,
            customerName = customer.name,
            currencySymbol = config.currencySymbol,
            onDismiss = { sessionToSettle = null },
            onConfirmSettle = { amount, method, notes ->
                viewModel.settleSessionDebt(s, amount, method, notes)
            }
        )
    }

    // Delete Session Confirmation
    sessionToDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("تأكيد حذف دورة الماء", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف هذه الدورة؟ سيتم إلغاء المبالغ المقيدة بها.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(s)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف الدورة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Delete owner-purchase confirmation
    purchaseToDelete?.let { purchase ->
        AlertDialog(
            onDismissRequest = { purchaseToDelete = null },
            title = { Text("حذف عملية شراء الساعات", fontWeight = FontWeight.Bold) },
            text = { Text("سيُعاد احتساب مستحق صاحب البئر بعد حذف هذه الحركة. هل تريد المتابعة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOwnerPurchase(purchase)
                        purchaseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) { Text("حذف الحركة", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { purchaseToDelete = null }) { Text("إلغاء") } }
        )
    }

    // Delete Voucher Confirmation
    voucherToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = { Text("تأكيد حذف السند", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف هذا السند؟ سيتم تحديث رصيد العميل فوراً.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoucher(v)
                        voucherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف السند", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { voucherToDelete = null }) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Add Receipt Sheet
    if (showAddReceiptSheet) {
        AddReceiptBottomSheet(
            customer = customer,
            currentBalance = customerWithBalance.receivableBalance,
            currencySymbol = config.currencySymbol,
            isWellOwner = customer.isWellOwner,
            onDismiss = { showAddReceiptSheet = false },
            onConfirm = { amount, method, notes ->
                viewModel.addReceiptVoucher(customer.id, amount, method, notes)
            }
        )
    }

    // Add Disbursement Sheet (سند صرف للمستفيد)
    if (showAddDisbursementSheet) {
        AddDisbursementBottomSheet(
            customer = customer,
            currencySymbol = config.currencySymbol,
            isWellOwner = customer.isWellOwner,
            currentBalance = customerWithBalance.payableBalance,
            onDismiss = { showAddDisbursementSheet = false },
            onConfirm = { amount, method, description ->
                viewModel.addExpenseVoucher(customer.id, amount, method, description)
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
            onSave = { date, durationMinutes, wastedMinutes, purchaseRate, amountPaid, notes ->
                if (editing != null) {
                    viewModel.updateOwnerPurchase(
                        purchaseId = editing.id,
                        ownerCustomerId = customer.id,
                        date = date,
                        durationMinutes = durationMinutes,
                        wastedMinutesOnOwner = wastedMinutes,
                        purchaseRatePerHour = purchaseRate,
                        notes = notes,
                        amountPaid = amountPaid
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
                        amountPaid = amountPaid
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
            customers = allCustomers,
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
                        Text("تسجيل هدر على صاحب البئر", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "يُخصم الهدر من آخر عملية شراء (${Formatters.formatDate(latestPurchase.date)}) ويقلل المستحق له بسعر الشراء (${Formatters.formatCurrency(latestPurchase.purchaseRatePerHour, config.currencySymbol)}/ساعة).",
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
                                    text = "❌ لا يمكن أن يكون الهدر أكبر من عملية الشراء بتاتاً! الحد المتاح: ${Formatters.formatDurationArabic(availableMinutes)}",
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
                            label = { Text("سبب الهدر (انقطاع ماء، عطل بئر...)") },
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
            currencySymbol = config.currencySymbol,
            onDismiss = { showEditCustomerSheet = false },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner)
                showEditCustomerSheet = false
            }
        )
    }
}

@Composable
private fun CustomerPurchaseCardItem(
    purchase: WellOwnerPurchase,
    currencySymbol: String,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val chargeableMinutes = WellOwnerPurchaseMath.chargeableMinutes(purchase)
    val payableAmount = WellOwnerPurchaseMath.payableAmount(purchase)
    val wasteCredit = WellOwnerPurchaseMath.ownerWasteCredit(purchase)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Icon + Title/Date + Actions (Edit & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "شراء ساعات من صاحب البئر",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = Formatters.formatDateTime(purchase.date),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الشراء",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الشراء",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Specs Grid (Duration + Rate)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "المدة المسجلة",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatDurationArabic(purchase.durationMinutes),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "سعر شراء الساعة",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatCurrency(purchase.purchaseRatePerHour, currencySymbol),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Waste Alert if any
            if (purchase.wastedMinutesOnOwner > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "هدر على صاحب البئر: ${Formatters.formatDurationArabic(purchase.wastedMinutesOnOwner)}",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                            )
                        }
                        Text(
                            text = "خصم ${Formatters.formatCurrency(wasteCredit, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Bottom Financial Status Banner: الإجمالي + صرف (مدفوع) + عليك (متبقي)
            val paidAmount = purchase.amountPaid
            val remainingDebt = (payableAmount - paidAmount).coerceAtLeast(0.0)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AccentGold.copy(alpha = 0.12f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الساعات المحتسبة: ${Formatters.formatDurationArabic(chargeableMinutes)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "الإجمالي: ${Formatters.formatCurrency(payableAmount, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.8.dp)
                            .background(AccentGold.copy(alpha = 0.3f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // المدفوع نقداً (صرف)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "صرف (مدفوع):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp
                                )
                            )
                            Text(
                                text = Formatters.formatCurrency(paidAmount, currencySymbol),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (paidAmount > 0) Color(0xFFE53935) else Color.Gray,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // المتبقي (عليك لصاحب البئر)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = if (remainingDebt > 0) "عليك (متبقي):" else "الحالة:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.5.sp
                                )
                            )
                            Text(
                                text = if (remainingDebt > 0) Formatters.formatCurrency(remainingDebt, currencySymbol) else "مسدد بالكامل",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (remainingDebt > 0) Color(0xFFE65100) else AccentEmerald,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            if (purchase.notes.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = purchase.notes,
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        }
    }
}

/**
 * بطاقة دورة السقي داخل كشف حساب العميل
 * تدعم زر [سداد] إذا كان هناك متبقي، وقابلة للتوسيع لعرض تفاصيل الدفعات المرتبطة
 */
@Composable
private fun CustomerSessionCardItem(
    session: WaterSession,
    originalCustomer: Customer?,
    isBeneficiaryView: Boolean,
    currencySymbol: String,
    linkedVouchers: List<Voucher>,
    onEditClick: () -> Unit,
    onPdfClick: () -> Unit,
    onMessageClick: () -> Unit,
    onSettleClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onShareVoucher: (Voucher) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // الصف العلوي: أيقونة السقي + المدة + التاريخ + أزرار الإجراءات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "دورة سقي (${Formatters.formatDurationArabic(session.durationMinutes)})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val timeStr = "من ${Formatters.formatTime(session.startTime)} إلى ${Formatters.formatTime(session.endTime)}  •  ${Formatters.formatDate(session.startTime)}"
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onMessageClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onPdfClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = AccentGold, modifier = Modifier.size(16.dp))
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("تعديل دورة السقي", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryTeal) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف دورة السقي", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            if (session.billedToCustomerId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE0F2FE))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "💧 على حساب صاحب البئر (مدفوع / مقاصة)",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                    )
                }
            } else if (isBeneficiaryView && originalCustomer != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGold.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "سقي للعميل: ${originalCustomer.name} (مقيد على هذا الحساب)",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // المبالغ المالية
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("إجمالي القيمة", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(
                        Formatters.formatCurrency(session.totalAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(color = PrimaryTeal, fontWeight = FontWeight.ExtraBold)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("المسدد", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(
                        Formatters.formatCurrency(session.amountPaid, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("المتبقي", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(
                        if (session.billedToCustomerId != null) "مسدد بالمقاصة"
                        else if (session.remainingDebt > 0) Formatters.formatCurrency(session.remainingDebt, currencySymbol)
                        else "خالص بالكامل",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (session.billedToCustomerId != null) Color(0xFF0284C7)
                                else if (session.remainingDebt > 0) Color(0xFFE53935)
                                else AccentEmerald,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            // شريط الإجراءات: زر [سداد] إن وُجد دين (لغير المسدد بالمقاصة) + تفاصيل السداد
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.billedToCustomerId != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE0F2FE))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "💧 مسدد ومخصوم من رصيد صاحب البئر",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF0369A1),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                } else if (session.remainingDebt > 0) {
                    Button(
                        onClick = onSettleClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("سداد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentEmerald.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "خالص ومسدد بالكامل",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Expand / Collapse breakdown
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "إخفاء الدفعات" else "سجل السداد (${if (session.amountPaid > 0) linkedVouchers.size + 1 else 0})",
                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    )
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // القسم القابل للتوسيع: تفاصيل الدفعة المقدمة + سندات السداد المؤخرة
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "تفاصيل المبالغ المسددة لهذه الدورة:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )

                    // 1. الدفعة الأولى (المقدمة عند الحفظ)
                    val initialPaid = session.amountPaid - linkedVouchers.sumOf { it.amount }
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
                                text = Formatters.formatCurrency(initialPaid, currencySymbol),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentEmerald)
                            )
                        }
                    }

                    // 2. السندات المؤخرة المرتبطة
                    linkedVouchers.forEach { v ->
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
                                IconButton(onClick = { onShareVoucher(v) }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = PrimaryTeal, modifier = Modifier.size(12.dp))
                                }
                            }
                            Text(
                                text = "+ ${Formatters.formatCurrency(v.amount, currencySymbol)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentEmerald)
                            )
                        }
                    }

                    if (initialPaid <= 0 && linkedVouchers.isEmpty()) {
                        Text(
                            text = "لم يتم تسجيل أي سداد لهذه الدورة بعد",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFE53935))
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة سند القبض داخل كشف حساب العميل
 */
@Composable
private fun CustomerReceiptCardItem(
    voucher: Voucher,
    currencySymbol: String,
    isWellOwner: Boolean = false,
    onPdfClick: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Title + Tag (Left) <---> Amount (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "${if (isWellOwner) "تحصيل من صاحب البئر" else "سند قبض نقدي"} (${voucher.voucherNumber})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (voucher.sessionId != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryTeal.copy(alpha = 0.12f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "دورة #${voucher.sessionId}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "+ ${Formatters.formatCurrency(voucher.amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium.copy(color = AccentEmerald, fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Method + Date + Notes <---> Actions (WhatsApp, PDF, More)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${voucher.paymentMethod}  •  ${Formatters.formatDateTime(voucher.date)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    if (voucher.notes.isNotEmpty()) {
                        Text(
                            text = voucher.notes,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onShareWhatsApp, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", tint = Color(0xFF2E7D32), modifier = Modifier.size(15.dp))
                    }

                    IconButton(onClick = onPdfClick, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = AccentGold, modifier = Modifier.size(15.dp))
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("حذف السند", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * بطاقة سند الصرف داخل كشف حساب العميل / المستفيد
 */
@Composable
private fun CustomerDisbursementCardItem(
    voucher: Voucher,
    currencySymbol: String,
    isWellOwner: Boolean = false,
    onPdfClick: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(1.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Row 1: Title (Left) <---> Amount (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC62828).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(17.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (isWellOwner) "سداد لصاحب البئر" else "سند صرف مالي"} (${voucher.voucherNumber})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "- ${Formatters.formatCurrency(voucher.amount, currencySymbol)}",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFC62828), fontWeight = FontWeight.ExtraBold)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Method + Date + Notes <---> Actions (WhatsApp, PDF, More)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${voucher.paymentMethod}  •  ${Formatters.formatDateTime(voucher.date)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    val desc = voucher.notes.ifBlank { voucher.category }
                    if (desc.isNotEmpty()) {
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onShareWhatsApp, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", tint = Color(0xFF2E7D32), modifier = Modifier.size(15.dp))
                    }

                    IconButton(onClick = onPdfClick, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = AccentGold, modifier = Modifier.size(15.dp))
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("حذف السند", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

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
                tint = if (accentColor == androidx.compose.ui.graphics.Color.White) androidx.compose.ui.graphics.Color.White else accentColor,
                modifier = androidx.compose.ui.Modifier.size(22.dp)
            )
        }
        Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = color
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        )
    }
}

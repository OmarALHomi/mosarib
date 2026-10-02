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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.PrimaryTealDark
import com.example.ui.theme.SecondaryAquaDark
import java.io.File

enum class CustomerOpFilter(val title: String) {
    ALL("الكل"),
    SESSIONS("دورات السقي"),
    RECEIPTS("سندات القبض"),
    DISBURSEMENTS("سندات الصرف"),
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
    val allCustomersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val customerWithBalance = allCustomersWithBalance.find { it.customer.id == customerId }
    val allCustomers: List<Customer> by viewModel.allCustomers.collectAsStateWithLifecycle(initialValue = emptyList())
    val sessions by remember(customerId) { viewModel.getCustomerSessions(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val vouchers by remember(customerId) { viewModel.getCustomerVouchers(customerId) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf(CustomerOpFilter.ALL) }
    var showAddReceiptSheet by remember { mutableStateOf(false) }
    var showAddDisbursementSheet by remember { mutableStateOf(false) }
    var showEditCustomerSheet by remember { mutableStateOf(false) }
    var sessionToSettle by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<WaterSession?>(null) }
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

    androidx.compose.runtime.LaunchedEffect(customerWithBalance) {
        if (customer.linkCode.isBlank()) {
            val generated = com.example.core.util.LinkCodeGenerator.generate()
            viewModel.updateCustomer(customer.copy(linkCode = generated))
        } else {
            com.example.core.sync.MusribSyncManager.syncCustomerSummary(
                customer = customer,
                distributorName = config.distributorName.ifBlank { "المسرب" },
                distributorPhone = config.distributorPhone,
                totalBilled = customerWithBalance.totalBilledAmount,
                totalPaid = customerWithBalance.totalPaidAmount,
                currentBalance = customerWithBalance.balance
            )
        }
    }
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
    val receiptItems = remember(vouchers) {
        vouchers.filter { it.type == VoucherType.RECEIPT }.map { CustomerLedgerItem.ReceiptItem(it) }
    }
    val disbursementItems = remember(vouchers) {
        vouchers.filter { it.type == VoucherType.EXPENSE }.map { CustomerLedgerItem.DisbursementItem(it) }
    }

    val filteredItems = remember(selectedFilter, sessionItems, receiptItems, disbursementItems) {
        when (selectedFilter) {
            CustomerOpFilter.ALL -> (sessionItems + receiptItems + disbursementItems).sortedByDescending { it.timestamp }
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
                    onClick = { onAddSessionForCustomer(cwb.customer) },
                    icon = {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    text = {
                        Text(
                            text = "تسجيل دورة سقي",
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
                                    if ((customer.customPricePerHour ?: 0.0) > 0) add("سعر خاص: ${Formatters.formatNumber(customer.customPricePerHour!!)} ${config.currencySymbol}/س")
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

                                if (customer.linkCode.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.Black.copy(alpha = 0.28f))
                                            .clickable {
                                                val msg = "مرحباً يا ${customer.name}، كود ربط حسابك في تطبيق جِربة الزراعي مع المسرب (${config.distributorName.ifBlank { "المسرب" }}):\n#${customer.linkCode}\nأدخل هذا الكود في التطبيق لمتابعة دورات السقي ورصيدك مباشرة."
                                                FileSharingHelper.copyToClipboard(context, customer.linkCode)
                                                FileSharingHelper.shareText(context, msg, "مشاركة كود الربط")
                                            }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "كود ربط المزارع: #${customer.linkCode}",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = AccentGold,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "مشاركة",
                                            tint = AccentGold,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ── Quick Action Buttons ──
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                            accentColor = Color(0xFF69F0AE),
                                            onClick = {
                                                messageCustomTarget = viewModel.buildCustomerStatementMessage(customer, customerWithBalance)
                                            }
                                        )
                                    }
                                    QuickActionButton(
                                        icon = Icons.Default.Payments,
                                        label = "سند قبض",
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
                            // Balance label
                            Text(
                                text = when {
                                    isDebt -> "صافي الدين المتبقي بذمة العميل"
                                    isCredit -> "رصيد دائن لصالح العميل"
                                    else -> "الحساب مسدد بالكامل (لا توجد مطالبات)"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            // Big balance amount
                            val balanceColor = when {
                                isDebt -> Color(0xFFE53935)
                                isCredit -> AccentEmerald
                                else -> PrimaryTeal
                            }
                            Text(
                                text = if (customerWithBalance.balance == 0.0)
                                    "0 ${config.currencySymbol}"
                                else
                                    Formatters.formatCurrency(Math.abs(customerWithBalance.balance), config.currencySymbol),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = balanceColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 30.sp
                                )
                            )

                            if (customerWithBalance.balance == 0.0) {
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
                                        label = "إجمالي السقي",
                                        value = Formatters.formatCurrency(customerWithBalance.totalBilledAmount, config.currencySymbol),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        icon = Icons.Default.WaterDrop
                                    )
                                    StatCellBox(
                                        modifier = Modifier.weight(1f),
                                        label = "مدة الري الكلية",
                                        value = Formatters.formatDurationArabic(customerWithBalance.totalMinutes),
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
                                        label = "إجمالي المقبوض",
                                        value = Formatters.formatCurrency(customerWithBalance.totalPaidAmount, config.currencySymbol),
                                        color = AccentEmerald,
                                        icon = Icons.Default.Payments
                                    )
                                    if (customerWithBalance.totalDisbursedAmount > 0) {
                                        StatCellBox(
                                            modifier = Modifier.weight(1f),
                                            label = "إجمالي المنصرف",
                                            value = Formatters.formatCurrency(customerWithBalance.totalDisbursedAmount, config.currencySymbol),
                                            color = Color(0xFFEF5350),
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
                        CustomerOpFilter.entries.forEach { filter ->
                            val count = when (filter) {
                                CustomerOpFilter.ALL -> sessions.size + vouchers.size
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
                            is CustomerLedgerItem.SessionItem -> "sess_${item.session.id}"
                            is CustomerLedgerItem.ReceiptItem -> "rec_${item.voucher.id}"
                            is CustomerLedgerItem.DisbursementItem -> "disb_${item.voucher.id}"
                        }
                    }) { ledgerItem ->
                        when (ledgerItem) {
                            is CustomerLedgerItem.SessionItem -> {
                                CustomerSessionCardItem(
                                    session = ledgerItem.session,
                                    originalCustomer = ledgerItem.originalCustomer,
                                    isBeneficiaryView = false,
                                    currencySymbol = config.currencySymbol,
                                    linkedVouchers = ledgerItem.linkedVouchers,
                                    onPdfClick = {
                                        val file = PdfReportGenerator.generateSessionInvoicePdf(
                                            context = context,
                                            config = config,
                                            customer = ledgerItem.originalCustomer ?: customer,
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
            currentBalance = customerWithBalance.balance,
            currencySymbol = config.currencySymbol,
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
            onDismiss = { showAddDisbursementSheet = false },
            onConfirm = { amount, method, description ->
                viewModel.addExpenseVoucher(customer.id, amount, method, description)
            }
        )
    }

    // Edit Customer Sheet
    if (showEditCustomerSheet) {
        AddEditCustomerBottomSheet(
            initialCustomer = customer,
            currencySymbol = config.currencySymbol,
            onDismiss = { showEditCustomerSheet = false },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary)
                showEditCustomerSheet = false
            }
        )
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

            // إذا كان السقي مقيداً على حساب المستفيد نيابة عن عميل آخر
            if (isBeneficiaryView && originalCustomer != null) {
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
                        if (session.remainingDebt > 0) Formatters.formatCurrency(session.remainingDebt, currencySymbol) else "خالص بالكامل",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (session.remainingDebt > 0) Color(0xFFE53935) else AccentEmerald,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }
            }

            // شريط الإجراءات: زر [سداد] إن وُجد دين + زر فتح تفاصيل السداد (Accordion)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (session.remainingDebt > 0) {
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
                            text = "سند قبض نقدي (${voucher.voucherNumber})",
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
                        text = "سند صرف مالي (${voucher.voucherNumber})",
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

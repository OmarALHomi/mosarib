package com.example.features.vouchers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.util.PdfReportGenerator
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.ui.StatBoxCard
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.sessions.SessionCardItem
import com.example.features.sessions.SettleSessionDialog
import com.example.features.sessions.WaterSession
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

@Composable
fun VouchersScreen(
    viewModel: VouchersViewModel,
    onNavigateToCustomer: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val operations by viewModel.unifiedOperations.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val operationsFilter by viewModel.operationsFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val frequentDescriptions by viewModel.frequentExpenseDescriptions.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showActivationDialog by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }
    var sessionToSettle by remember { mutableStateOf<WaterSession?>(null) }
    var voucherMessageTarget by remember { mutableStateOf<Pair<Customer, String>?>(null) }
    var voucherPdfReady by remember { mutableStateOf<Pair<File, String>?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {

            // Financial Balance Metric Cards (المقبوضات vs المصاريف)
            item {
                val totalReceipts = allVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount }
                val totalExpenses = allVouchers.filter { it.type == VoucherType.EXPENSE }.sumOf { it.amount }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxCard(
                        title = "المقبوضات",
                        value = Formatters.formatCurrency(totalReceipts, config.currencySymbol),
                        icon = Icons.Default.ArrowDownward,
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxCard(
                        title = "المصاريف",
                        value = Formatters.formatCurrency(totalExpenses, config.currencySymbol),
                        icon = Icons.Default.ArrowUpward,
                        accentColor = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search & Filters
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
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
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "بحث بالعميل أو رقم السند...",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            fontSize = 13.sp
                                        ),
                                        maxLines = 1
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("operations_search_input")
                                )
                            }
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSearchQuery("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.ALL,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.ALL) },
                                label = { Text("الكل", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.SESSIONS,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.SESSIONS) },
                                label = { Text("سقي", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.RECEIPTS,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.RECEIPTS) },
                                label = { Text("مقبوضات", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.EXPENSES,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.EXPENSES) },
                                label = { Text("مصروفات", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE53935),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.DEFERRED,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.DEFERRED) },
                                label = { Text("مؤخر / غير مسدد", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD97706),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Operations List (Combined: Sessions + Vouchers)
            if (operations.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Receipt,
                        title = "لا توجد عمليات مسجلة",
                        description = "لم يتم العثور على أي حركة تطابق خيارات الفلترة المحددة"
                    )
                }
            } else {
                items(operations, key = { op ->
                    when (op) {
                        is UnifiedOperation.SessionOp -> "s_${op.sessionWithCustomer.session.id}"
                        is UnifiedOperation.VoucherOp -> "v_${op.voucherWithCustomer.voucher.id}"
                    }
                }) { op ->
                    when (op) {
                        is UnifiedOperation.SessionOp -> {
                            SessionCardItem(
                                sessionWithCustomer = op.sessionWithCustomer,
                                currencySymbol = config.currencySymbol,
                                linkedVouchers = op.linkedVouchers,
                                onCustomerClick = { onNavigateToCustomer(op.sessionWithCustomer.session.customerId) },
                                onEditClick = {},
                                onDeleteClick = {},
                                onPdfClick = {},
                                onMessageClick = {},
                                onSettleClick = {
                                    sessionToSettle = op.sessionWithCustomer.session
                                },
                                onShareVoucher = { v ->
                                    op.sessionWithCustomer.customer?.let { c ->
                                        val msg = "سند قبض #${v.voucherNumber}\nسداد دورة سقي #${op.sessionWithCustomer.session.id}\nالعميل: ${c.name}\nالمبلغ: ${Formatters.formatCurrency(v.amount, config.currencySymbol)}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                                        FileSharingHelper.sendWhatsAppMessage(context, c.phone, msg)
                                    }
                                }
                            )
                        }
                        is UnifiedOperation.VoucherOp -> {
                            val voucher = op.voucherWithCustomer.voucher
                            val isReceipt = voucher.type == VoucherType.RECEIPT
                            val badgeColor = if (isReceipt) AccentEmerald else Color(0xFFE53935)
                            val customer = op.voucherWithCustomer.customer

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    // 1. الصف العلوي
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable {
                                                    if (voucher.customerId != null) {
                                                        onNavigateToCustomer(voucher.customerId)
                                                    }
                                                }
                                                .weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(badgeColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                    contentDescription = null,
                                                    tint = badgeColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = if (isReceipt) "سند قبض" else "سند صرف",
                                                        style = MaterialTheme.typography.titleMedium.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "(${voucher.voucherNumber})",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                                    )
                                                }

                                                Text(
                                                    text = when {
                                                        isReceipt -> "العميل: ${customer?.name ?: "غير محدد"}"
                                                        customer != null -> "العميل: ${customer.name}"
                                                        else -> "مصروف عام"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = if (customer != null) PrimaryTeal else Color(0xFF64748B),
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (customer != null && customer.phone.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .clickable {
                                                            val rawMsg = if (isReceipt) {
                                                                "سند قبض #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)})\nطريقة الدفع: ${voucher.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(voucher.date)}"
                                                            } else {
                                                                "سند صرف #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)})\nالبيان: ${voucher.notes.ifEmpty { voucher.category }}\nطريقة الصرف: ${voucher.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(voucher.date)}"
                                                            }
                                                            voucherMessageTarget = Pair(customer, FileSharingHelper.attachMessageFooter(rawMsg))
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Send,
                                                        contentDescription = "مشاركة",
                                                        tint = PrimaryTeal,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                }
                                            }

                                            // زر معاينة وطباعة PDF
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable {
                                                        val file = if (isReceipt) {
                                                            PdfReportGenerator.generateReceiptVoucherPdf(
                                                                context = context,
                                                                config = config,
                                                                customer = customer ?: Customer(name = "عميل نقدي"),
                                                                voucher = voucher
                                                            )
                                                        } else {
                                                            PdfReportGenerator.generateExpenseVoucherPdf(
                                                                context = context,
                                                                config = config,
                                                                customer = customer,
                                                                voucher = voucher
                                                            )
                                                        }
                                                        voucherPdfReady = Pair(file, if (isReceipt) "سند قبض #${voucher.voucherNumber}" else "سند صرف #${voucher.voucherNumber}")
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PictureAsPdf,
                                                    contentDescription = "طباعة PDF",
                                                    tint = PrimaryTeal,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFFEBEE))
                                                    .clickable { voucherToDelete = voucher },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 2. كبسولة وسطى لطريقة الدفع والتاريخ
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Payments,
                                                contentDescription = null,
                                                tint = PrimaryTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = voucher.paymentMethod,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }

                                        Text(
                                            text = Formatters.formatDateTime(voucher.date),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF64748B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 3. الجزء السفلي: المبلغ البارز والتفقيط
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isReceipt) "المبلغ المقبوض" else "المبلغ المصروف",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Text(
                                                text = Formatters.formatCurrency(voucher.amount, config.currencySymbol),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = badgeColor,
                                                    fontSize = 18.sp
                                                )
                                            )
                                            Text(
                                                text = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF8D6E63),
                                                    fontSize = 10.sp,
                                                    lineHeight = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 2
                                            )
                                        }
                                    }

                                    if (voucher.sessionId != null && voucher.sessionId > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "مرتبط بدورة سقي #${voucher.sessionId}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    val noteText = voucher.notes.ifEmpty { voucher.category }
                                    if (noteText.isNotBlank() && noteText != "عام" && noteText != "سداد حساب") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "البيان: $noteText",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.Bold),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB to Add Operation / Voucher
        ExtendedFloatingActionButton(
            onClick = {
                if (LicenseManager.canPerformOperation(context, operationsCount)) {
                    showAddSheet = true
                } else {
                    showActivationDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .testTag("fab_add_operation"),
            containerColor = PrimaryTeal,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("سند جديد", fontWeight = FontWeight.Bold) }
        )

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        )
    }

    // Settle Session Dialog
    sessionToSettle?.let { s ->
        val cust = customers.find { it.id == s.customerId }
        SettleSessionDialog(
            session = s,
            customerName = cust?.name ?: "عميل غير محدد",
            currencySymbol = config.currencySymbol,
            onDismiss = { sessionToSettle = null },
            onConfirmSettle = { amt, method, notes ->
                viewModel.settleSessionDebt(s, amt, method, notes)
            }
        )
    }

    if (showAddSheet) {
        AddVoucherBottomSheet(
            customers = customers,
            allSessions = allSessions,
            frequentDescriptions = frequentDescriptions,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddSheet = false },
            onSave = { type, custId, amt, cat, method, notes, sessionId ->
                viewModel.addVoucher(type, custId, amt, cat, method, notes, sessionId)
            }
        )
    }

    // حوار اختيار قناة الإرسال (واتساب أو SMS)
    voucherMessageTarget?.let { (cust, msg) ->
        SendMessageChoiceDialog(
            recipientName = cust.name,
            recipientPhone = cust.phone,
            messageText = msg,
            onDismiss = { voucherMessageTarget = null },
            onSendWhatsApp = {
                voucherMessageTarget = null
                FileSharingHelper.sendWhatsAppMessage(context, cust.phone, msg)
            },
            onSendSms = {
                voucherMessageTarget = null
                FileSharingHelper.sendSms(context, cust.phone, msg)
            }
        )
    }

    // حوار فتح ومشاركة ملف PDF
    voucherPdfReady?.let { (file, title) ->
        AlertDialog(
            onDismissRequest = { voucherPdfReady = null },
            title = { Text("المستند جاهز", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في فتح وعرض السند مباشرة أم مشاركته كملف PDF؟") },
            confirmButton = {
                Button(
                    onClick = {
                        FileSharingHelper.openPdf(context, file)
                        voucherPdfReady = null
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
                        voucherPdfReady = null
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    voucherToDelete?.let { v ->
        AlertDialog(
            onDismissRequest = { voucherToDelete = null },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد بالتأكيد حذف هذا السند؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteVoucher(v)
                        voucherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { voucherToDelete = null }) {
                    Text("إلغاء", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // License Activation Dialog
    if (showActivationDialog) {
        LicenseDialog(
            onDismiss = { showActivationDialog = false },
            onActivated = { showActivationDialog = false },
            isMandatory = operationsCount >= LicenseManager.FREE_OPERATIONS_LIMIT
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVoucherBottomSheet(
    customers: List<Customer>,
    allSessions: List<WaterSession> = emptyList(),
    frequentDescriptions: List<String> = emptyList(),
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        type: VoucherType,
        customerId: Long?,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String,
        sessionId: Long?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(VoucherType.RECEIPT) }
    var selectedCustomerId by remember { mutableLongStateOf(customers.firstOrNull()?.id ?: 0L) }
    var selectedBeneficiaryId by remember { mutableStateOf<Long?>(null) }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    var custDropdownExpanded by remember { mutableStateOf(false) }
    var beneficiaryDropdownExpanded by remember { mutableStateOf(false) }
    var sessionDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val selectedBeneficiary = customers.find { it.id == selectedBeneficiaryId }

    // الجلسات غير المسددة لهذا العميل لإمكانية ربط السند بها
    val unsettledSessions = allSessions.filter { it.customerId == selectedCustomerId && it.remainingDebt > 0 }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (type == VoucherType.RECEIPT) "سند قبض" else "تسجيل مصروف",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Type Toggle: قبض من عميل | مصروف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { type = VoucherType.RECEIPT },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == VoucherType.RECEIPT) AccentEmerald else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == VoucherType.RECEIPT) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("قبض من عميل", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { type = VoucherType.EXPENSE },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == VoucherType.EXPENSE) Color(0xFFE53935) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (type == VoucherType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("مصروف", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (type == VoucherType.RECEIPT) {
                Text("العميل المستلم منه *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { custDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = custDropdownExpanded,
                        onDismissRequest = { custDropdownExpanded = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedCustomerId = c.id
                                    selectedSessionId = null
                                    custDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // خيار ربط السند بدورة سقي غير مسددة
                if (unsettledSessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("ربط بدورة سقي مؤخرة (اختياري):", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        val sessionTitle = if (selectedSessionId != null) {
                            val target = unsettledSessions.find { it.id == selectedSessionId }
                            "دورة سقي #${target?.id} (متبقي: ${Formatters.formatCurrency(target?.remainingDebt ?: 0.0, currencySymbol)})"
                        } else {
                            "سداد عام لحساب العميل (بدون ربط)"
                        }
                        OutlinedTextField(
                            value = sessionTitle,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { sessionDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = sessionDropdownExpanded,
                            onDismissRequest = { sessionDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("سداد عام لحساب العميل (بدون ربط)") },
                                onClick = {
                                    selectedSessionId = null
                                    sessionDropdownExpanded = false
                                }
                            )
                            unsettledSessions.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "دورة #${s.id} - ${Formatters.formatDate(s.startTime)} (متبقي: ${Formatters.formatCurrency(s.remainingDebt, currencySymbol)})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    onClick = {
                                        selectedSessionId = s.id
                                        amountStr = Formatters.formatAmountInput(s.remainingDebt.toString())
                                        sessionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // سند صرف: تحديد العميل اختياري (مصروف عام أو مقيد على عميل)
                Text("العميل / المستلم (اختياري):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedBeneficiary?.name ?: "مصروف عام (غير مقيد على عميل)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { beneficiaryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = beneficiaryDropdownExpanded,
                        onDismissRequest = { beneficiaryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("مصروف عام (غير مقيد على عميل)") },
                            onClick = {
                                selectedBeneficiaryId = null
                                beneficiaryDropdownExpanded = false
                            }
                        )
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = {
                                    Text(c.name, fontWeight = FontWeight.Bold)
                                },
                                onClick = {
                                    selectedBeneficiaryId = c.id
                                    beneficiaryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ ($currencySymbol) *", fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voucher_amount_input"),
                shape = RoundedCornerShape(12.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("طريقة الدفع", fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (type == VoucherType.RECEIPT) "البيان والملاحظات" else "بيان المصروف *", fontWeight = FontWeight.Bold) },
                placeholder = { Text(if (type == VoucherType.RECEIPT) "مثال: دفعة ري أو سداد حساب" else "اكتب بيان المصروف...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    if (amt > 0) {
                        val custId = if (type == VoucherType.RECEIPT) selectedCustomerId else selectedBeneficiaryId
                        val cat = if (type == VoucherType.RECEIPT) "سداد حساب" else notes.ifBlank { "مصروف" }
                        onSave(type, custId, amt, cat, paymentMethod, notes, selectedSessionId)
                        onDismiss()
                    }
                },
                enabled = Formatters.parseAmountInput(amountStr) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_voucher_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == VoucherType.RECEIPT) AccentEmerald else PrimaryTeal
                )
            ) {
                Text("حفظ وترحيل السند", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

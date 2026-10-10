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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.graphics.graphicsLayer
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.SessionCardItem
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.SettleSessionDialog
import com.example.features.sessions.WaterSession
import com.example.features.wellowners.WellOwnerPurchaseMath
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val frequentDescriptions by viewModel.frequentExpenseDescriptions.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val settlementResult by viewModel.settlementResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showActivationDialog by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var speedDialOpen by remember { mutableStateOf(false) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }
    var sessionToSettle by remember { mutableStateOf<WaterSession?>(null) }
    var voucherMessageTarget by remember { mutableStateOf<Pair<Customer, String>?>(null) }
    var voucherPdfReady by remember { mutableStateOf<Pair<File, String>?>(null) }
    var selectedLinkedSession by remember { mutableStateOf<WaterSession?>(null) }

    val listState = rememberLazyListState()
    val firstIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val firstOffset by remember { derivedStateOf { listState.firstVisibleItemScrollOffset } }
    val fadeFactor by remember {
        derivedStateOf {
            if (firstIndex > 0) 0f
            else (1f - (firstOffset / 200f)).coerceIn(0f, 1f)
        }
    }
    val animatedAlpha by animateFloatAsState(targetValue = fadeFactor, label = "summary_card_fade")

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
        ) {
            // Merged Integrated Financial Summary Card (يتلاشى وينكمش عند التمرير لأعلى)
            item {
                if (animatedAlpha > 0.05f) {
                    val totalReceipts = allVouchers.filter { it.type == VoucherType.RECEIPT }.sumOf { it.amount } +
                        allSessions.sumOf { it.amountPaid }
                    val totalExpenses = allVouchers.filter { it.type == VoucherType.EXPENSE }.sumOf { it.amount }
                    val totalDebts = allSessions.sumOf { it.remainingDebt }
                    val netProfit = totalReceipts - totalExpenses
                    val totalWaterMinutes = allSessions.sumOf { it.durationMinutes }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                            .graphicsLayer {
                                alpha = animatedAlpha
                                scaleY = animatedAlpha.coerceAtLeast(0.85f)
                            }
                            .shadow((3 * animatedAlpha).dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Top Row: Net Profit & Water Hours
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "صافي العائد والربح",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.5.sp)
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(netProfit, config.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (netProfit >= 0) AccentEmerald else Color(0xFFE53935),
                                            fontSize = 17.sp
                                        )
                                    )
                                }

                                Surface(
                                    color = PrimaryTeal.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(13.dp))
                                        Text(
                                            text = Formatters.formatDurationArabic(totalWaterMinutes),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = PrimaryTeal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Bottom 3-Metric Strip: Receipts, Expenses, Debts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // المقبوض
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(12.dp))
                                            Text("المقبوض", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
                                        }
                                        Text(
                                            text = Formatters.formatCurrency(totalReceipts, config.currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = AccentEmerald,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // المصروف
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(12.dp))
                                            Text("المصروف", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
                                        }
                                        Text(
                                            text = Formatters.formatCurrency(totalExpenses, config.currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFE53935),
                                            maxLines = 1
                                        )
                                    }
                                }

                                // الدائن / الديون
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(12.dp))
                                            Text("الديون (آجل)", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
                                        }
                                        Text(
                                            text = Formatters.formatCurrency(totalDebts, config.currencySymbol),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (totalDebts > 0) Color(0xFFE65100) else AccentEmerald,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search & Filters Row (Compact & Borderless)
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                                modifier = Modifier.size(16.dp)
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
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            fontSize = 12.5.sp
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
                                        fontSize = 12.5.sp
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("operations_search_input")
                                )
                            }
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

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.ALL,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.ALL) },
                                label = { Text("الكل", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = null
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.SESSIONS,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.SESSIONS) },
                                label = { Text("سقي", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = null
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.RECEIPTS,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.RECEIPTS) },
                                label = { Text("مقبوضات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentEmerald,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = null
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.EXPENSES,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.EXPENSES) },
                                label = { Text("مصروفات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE53935),
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = null
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.DEFERRED,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.DEFERRED) },
                                label = { Text("آجل / ديون", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE65100),
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = null
                            )
                        }
                        item {
                            FilterChip(
                                selected = operationsFilter == OperationsFilter.PURCHASES,
                                onClick = { viewModel.setOperationsFilter(OperationsFilter.PURCHASES) },
                                label = { Text("شراء ساعات", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                shape = RoundedCornerShape(14.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0288D1),
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            // Operations List (Combined: Sessions + Vouchers + Purchases)
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
                        is UnifiedOperation.PurchaseOp -> "p_${op.purchase.id}"
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
                            val titleText = when {
                                isReceipt -> customer?.name ?: "عميل نقدي"
                                customer != null -> if (customer.isWellOwner) "تسديد لصاحب البئر: ${customer.name}" else customer.name
                                voucher.notes.isNotBlank() && voucher.notes != "عام" -> voucher.notes
                                voucher.category.isNotBlank() -> voucher.category
                                else -> "مصروف عام"
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    // 1. الصف العلوي: علامة سحب أخضر + اسم العميل + المبلغ بخط عريض وتحته المبلغ كتابة
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // اليمين: الأيقونة الخضراء واسم العميل في موقع "سند قبض"
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(badgeColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                    contentDescription = if (isReceipt) "قبض" else "صرف",
                                                    tint = badgeColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(7.dp))

                                            Column(
                                                modifier = Modifier.clickable {
                                                    if (voucher.customerId != null) {
                                                        onNavigateToCustomer(voucher.customerId)
                                                    }
                                                }
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = titleText,
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        ),
                                                        maxLines = 1
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = badgeColor.copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isReceipt) "قبض #${voucher.voucherNumber}" else "صرف #${voucher.voucherNumber}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = badgeColor,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // اليسار: المبلغ المقبوض بخط عريض وواضح وتحته المبلغ كتابة
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = (if (isReceipt) "+ " else "- ") + Formatters.formatCurrency(voucher.amount, config.currencySymbol),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = badgeColor,
                                                    fontSize = 16.sp
                                                )
                                            )
                                            Text(
                                                text = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 2. الصف السفلي: التاريخ + طريقة الدفع + تفصيل دورة السقي + أزرار العمليات المصغرة
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = Formatters.formatDateTime(voucher.date),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 9.5.sp
                                                )
                                            )

                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                                            )

                                            Text(
                                                text = voucher.paymentMethod,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 9.5.sp
                                                )
                                            )

                                            // تفصيل صغير يبين إن كان مربوط بدورة سقي (عند الضغط يأخذنا لتفاصيل السقي)
                                            if (voucher.sessionId != null && voucher.sessionId > 0) {
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            val foundSession = allSessions.find { it.id == voucher.sessionId }
                                                            if (foundSession != null) {
                                                                selectedLinkedSession = foundSession
                                                            } else {
                                                                viewModel.showToast("دورة سقي #${voucher.sessionId}")
                                                            }
                                                        },
                                                    color = PrimaryTeal.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            Icons.Default.WaterDrop,
                                                            contentDescription = null,
                                                            tint = PrimaryTeal,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "ساقية #${voucher.sessionId} ↗",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = PrimaryTeal,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.5.sp
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            if (!isReceipt && voucher.notes.isNotBlank() && voucher.notes != titleText && voucher.notes != "عام") {
                                                Text(
                                                    text = "(${voucher.notes})",
                                                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        // أزرار سريعة ومصغرة
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (customer != null && customer.phone.isNotEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(RoundedCornerShape(6.dp))
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
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            // زر PDF
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
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
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }

                                            // زر حذف
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFFEBEE))
                                                    .clickable { voucherToDelete = voucher },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is UnifiedOperation.PurchaseOp -> {
                            val purchase = op.purchase
                            val owner = op.owner
                            val payable = WellOwnerPurchaseMath.payableAmount(purchase)
                            val chargeable = WellOwnerPurchaseMath.chargeableMinutes(purchase)
                            val waste = purchase.wastedMinutesOnOwner

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    // 1. الصف العلوي: أيقونة شراء + اسم صاحب البئر + المبلغ الإجمالي المستحق
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF0288D1).copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.WaterDrop,
                                                    contentDescription = "شراء ساعات",
                                                    tint = Color(0xFF0288D1),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(7.dp))

                                            Column(
                                                modifier = Modifier.clickable {
                                                    onNavigateToCustomer(purchase.ownerCustomerId)
                                                }
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = owner?.name ?: "صاحب بئر #${purchase.ownerCustomerId}",
                                                        style = MaterialTheme.typography.titleSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        ),
                                                        maxLines = 1
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = Color(0xFF0288D1).copy(alpha = 0.1f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "شراء ساعات #${purchase.id}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = Color(0xFF0288D1),
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            ),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // اليسار: المبلغ المستحق للمالك
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = Formatters.formatCurrency(payable, config.currencySymbol),
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF0288D1),
                                                    fontSize = 16.sp
                                                )
                                            )
                                            Text(
                                                text = Formatters.amountToArabicWords(payable, config.currencySymbol),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                maxLines = 1,
                                                textAlign = TextAlign.End
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 2. الصف الأوسط: تفاصيل الساعات (إجمالي، هدر إن وجد، صافي، وسعر الساعة)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "⏱️ المدة: ${Formatters.formatDurationArabic(purchase.durationMinutes)}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (waste > 0) {
                                            Surface(
                                                color = Color(0xFFFFEBEE),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "⚠️ هدر: ${Formatters.formatDurationArabic(waste)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFD32F2F),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Surface(
                                                color = Color(0xFFE8F5E9),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "صافي: ${Formatters.formatDurationArabic(chargeable)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF2E7D32),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "السعر: ${Formatters.formatCurrency(purchase.purchaseRatePerHour, config.currencySymbol)}/س",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 10.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (purchase.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "ملاحظات: ${purchase.notes}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF64748B),
                                                fontSize = 9.5.sp
                                            ),
                                            maxLines = 1
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 3. الصف السفلي: التاريخ
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Formatters.formatDateTime(purchase.date),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF64748B),
                                                fontSize = 9.5.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // زر عائم مصغر بأيقونة فقط بدون نص يتفرع لخياري سند وسقي
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
        ) {
            AnimatedVisibility(
                visible = speedDialOpen,
                enter = fadeIn(tween(150)) + scaleIn(tween(150)),
                exit = fadeOut(tween(100)) + scaleOut(tween(100))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // خيار 1: دورة سقي
                    Surface(
                        onClick = {
                            speedDialOpen = false
                            if (LicenseManager.canPerformOperation(context, operationsCount)) {
                                showAddManualSheet = true
                            } else {
                                showActivationDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "دورة سقي",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    // خيار 2: سند (قبض أو صرف)
                    Surface(
                        onClick = {
                            speedDialOpen = false
                            if (LicenseManager.canPerformOperation(context, operationsCount)) {
                                showAddSheet = true
                            } else {
                                showActivationDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 6.dp,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(AccentGold.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "سند (قبض / صرف)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            val fabRotation by animateFloatAsState(
                targetValue = if (speedDialOpen) 45f else 0f,
                animationSpec = tween(200),
                label = "fab_rotation"
            )

            FloatingActionButton(
                onClick = { speedDialOpen = !speedDialOpen },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("fab_add_operation"),
                shape = CircleShape,
                containerColor = PrimaryTeal,
                contentColor = Color.White,
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
        }

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
            onSave = { type, custId, amt, cat, method, notes, sessionId, selectedSessionIds ->
                viewModel.addVoucher(type, custId, amt, cat, method, notes, sessionId, selectedSessionIds)
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

    settlementResult?.let { res ->
        SettlementResultDialog(
            result = res,
            onDismiss = { viewModel.clearSettlementResult() }
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

    selectedLinkedSession?.let { s ->
        val customer = customers.find { it.id == s.customerId }
        AlertDialog(
            onDismissRequest = { selectedLinkedSession = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تفاصيل دورة السقي #${s.id}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = customer?.name ?: "عميل غير محدد",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
            },
            text = {
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
                                Text("البداية:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                                Text(Formatters.formatDateTime(s.startTime), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("النهاية:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                                Text(Formatters.formatDateTime(s.endTime), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                Text("إجمالي وقت التشغيل:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                                Text(Formatters.formatDurationArabic(totalRunMinutes), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (s.wastedMinutes > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("هدر وتوقفات (مخصوم):", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFC62828)))
                                    Text(Formatters.formatDurationShort(s.wastedMinutes), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الساعات المحتسبة:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text(Formatters.formatDurationArabic(s.durationMinutes), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryTeal)
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
                                Text("سعر الساعة:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                                Text(Formatters.formatCurrency(s.pricePerHour, config.currencySymbol), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("إجمالي القيمة:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                Text(Formatters.formatCurrency(s.totalAmount, config.currencySymbol), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المسدد:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                                Text(Formatters.formatCurrency(s.amountPaid, config.currencySymbol), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentEmerald)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("المتبقي:", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                                Text(
                                    Formatters.formatCurrency(s.remainingDebt, config.currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (s.remainingDebt > 0) Color(0xFFC62828) else AccentEmerald
                                )
                            }
                        }
                    }

                    if (s.notes.isNotBlank()) {
                        Text(
                            text = "ملاحظات: ${s.notes}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
            },
            confirmButton = {
                if (customer != null) {
                    Button(
                        onClick = {
                            selectedLinkedSession = null
                            onNavigateToCustomer(s.customerId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text("كشف حساب العميل", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = { selectedLinkedSession = null }) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (customer != null) {
                    TextButton(onClick = { selectedLinkedSession = null }) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
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
        sessionId: Long?,
        selectedSessionIds: List<Long>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(VoucherType.RECEIPT) }
    var selectedCustomerId by remember { mutableLongStateOf(customers.firstOrNull()?.id ?: 0L) }
    var selectedBeneficiaryId by remember { mutableStateOf<Long?>(null) }
    var selectedSessionIds by remember { mutableStateOf(setOf<Long>()) }
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    var custDropdownExpanded by remember { mutableStateOf(false) }
    var beneficiaryDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val selectedBeneficiary = customers.find { it.id == selectedBeneficiaryId }

    // الجلسات غير المسددة لهذا العميل مرتبة بالأقدم أولاً (الأول فالأول)
    val unsettledSessions = remember(allSessions, selectedCustomerId) {
        allSessions.filter { it.customerId == selectedCustomerId && it.remainingDebt > 0 }
            .sortedBy { it.startTime }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                                    selectedSessionIds = emptySet()
                                    amountStr = ""
                                    custDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // قسم الجلسات غير المسددة المتوفرة على العميل
                if (unsettledSessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "جلسات سقي غير مسددة (${unsettledSessions.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                                    )
                                    val totalUnsettledDebt = unsettledSessions.sumOf { it.remainingDebt }
                                    Text(
                                        text = "إجمالي المتأخرات: ${Formatters.formatCurrency(totalUnsettledDebt, currencySymbol)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            val allIds = unsettledSessions.map { it.id }.toSet()
                                            selectedSessionIds = allIds
                                            val totalSelected = unsettledSessions.sumOf { it.remainingDebt }
                                            amountStr = Formatters.formatAmountInput(totalSelected.toString())
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("تحديد الكل", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryTeal)
                                    }
                                    if (selectedSessionIds.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                selectedSessionIds = emptySet()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("إلغاء", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اختر ما تريد سداده (يوزع المبلغ بالأقدمية أولاً):",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val parsedAmt = Formatters.parseAmountInput(amountStr)
                            val sortedSelected = unsettledSessions.filter { selectedSessionIds.contains(it.id) }.sortedBy { it.startTime }

                            var allocPool = parsedAmt
                            val allocations = mutableMapOf<Long, Pair<Double, Double>>()
                            for (s in sortedSelected) {
                                val allocated = minOf(s.remainingDebt, maxOf(0.0, allocPool))
                                val rem = maxOf(0.0, s.remainingDebt - allocated)
                                allocations[s.id] = Pair(allocated, rem)
                                allocPool = maxOf(0.0, allocPool - allocated)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                unsettledSessions.forEach { session ->
                                    val isSelected = selectedSessionIds.contains(session.id)
                                    val allocInfo = allocations[session.id]

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val newSet = if (isSelected) {
                                                    selectedSessionIds - session.id
                                                } else {
                                                    selectedSessionIds + session.id
                                                }
                                                selectedSessionIds = newSet
                                                if (newSet.isNotEmpty() && amountStr.isBlank()) {
                                                    val sumSel = unsettledSessions.filter { newSet.contains(it.id) }.sumOf { it.remainingDebt }
                                                    amountStr = Formatters.formatAmountInput(sumSel.toString())
                                                }
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                PrimaryTeal.copy(alpha = 0.08f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                        ),
                                        border = if (isSelected) {
                                            BorderStroke(1.5.dp, PrimaryTeal)
                                        } else {
                                            BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            val newSet = if (checked) {
                                                                selectedSessionIds + session.id
                                                            } else {
                                                                selectedSessionIds - session.id
                                                            }
                                                            selectedSessionIds = newSet
                                                            if (newSet.isNotEmpty() && amountStr.isBlank()) {
                                                                val sumSel = unsettledSessions.filter { newSet.contains(it.id) }.sumOf { it.remainingDebt }
                                                                amountStr = Formatters.formatAmountInput(sumSel.toString())
                                                            }
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = "دورة سقي #${session.id}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                        Text(
                                                            text = Formatters.formatDate(session.startTime),
                                                            fontSize = 11.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = Formatters.formatCurrency(session.remainingDebt, currencySymbol),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color(0xFFC62828)
                                                    )
                                                    Text(
                                                        text = "المتبقي",
                                                        fontSize = 10.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }

                                            // شارة المعاينة الحية للسداد في حال اختيار الجلسة
                                            if (isSelected && allocInfo != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val (allocated, remAfter) = allocInfo
                                                val (badgeBg, badgeTextColor, badgeText) = when {
                                                    parsedAmt <= 0.0 -> Triple(
                                                        Color(0xFFEEEEEE),
                                                        Color.DarkGray,
                                                        "⚠️ أدخل المبلغ لمعاينة التوزيع"
                                                    )
                                                    remAfter == 0.0 -> Triple(
                                                        Color(0xFFE8F5E9),
                                                        Color(0xFF2E7D32),
                                                        "✅ ستُسدد بالكامل (${Formatters.formatCurrency(allocated, currencySymbol)})"
                                                    )
                                                    allocated > 0.0 -> Triple(
                                                        Color(0xFFFFF8E1),
                                                        Color(0xFFF57F17),
                                                        "⏳ سداد جزئي: ${Formatters.formatCurrency(allocated, currencySymbol)} (متبقي: ${Formatters.formatCurrency(remAfter, currencySymbol)})"
                                                    )
                                                    else -> Triple(
                                                        Color(0xFFFFEBEE),
                                                        Color(0xFFC62828),
                                                        "❌ لم يشملها المبلغ (المبلغ غير كافٍ)"
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(badgeBg)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = badgeText,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = badgeTextColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "هذا العميل ليس عليه جلسات مؤخرة (سداد عام لحسابه)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
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

            Spacer(modifier = Modifier.height(14.dp))

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
                        val selList = if (type == VoucherType.RECEIPT) selectedSessionIds.toList() else emptyList()
                        onSave(type, custId, amt, cat, paymentMethod, notes, null, selList)
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

@Composable
fun SettlementResultDialog(
    result: SettlementResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AccentEmerald.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = AccentEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "تفاصيل تسديد السند",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "تم التوزيع حسب الأقدمية (الأول فالأول)",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Customer & Total Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("العميل:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
                            Text(result.customerName, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المبلغ المقبوض:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
                            Text(
                                Formatters.formatCurrency(result.totalAmount, result.currencySymbol),
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald
                            )
                        }
                    }
                }

                Text(
                    text = "توزيع السداد على الجلسات:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                result.items.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                item.isFullyPaid -> Color(0xFFE8F5E9)
                                item.allocatedAmount > 0 -> Color(0xFFFFF8E1)
                                else -> Color(0xFFFFEBEE)
                            }
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "دورة سقي #${item.sessionId}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = Formatters.formatDate(item.date),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "المسدد: ${Formatters.formatCurrency(item.allocatedAmount, result.currencySymbol)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (item.allocatedAmount > 0) PrimaryTeal else Color(0xFFC62828)
                                )
                                Text(
                                    text = when {
                                        item.isFullyPaid -> "خالصة بالكامل ✅"
                                        item.allocatedAmount > 0 -> "متبقي: ${Formatters.formatCurrency(item.remainingDebtAfter, result.currencySymbol)} ⏳"
                                        else -> "لم يشملها المبلغ ❌"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = when {
                                        item.isFullyPaid -> Color(0xFF2E7D32)
                                        item.allocatedAmount > 0 -> Color(0xFFF57F17)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            }
                        }
                    }
                }

                if (result.surplusAmount > 0.0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("💰", fontSize = 18.sp)
                            Column {
                                Text(
                                    "فائض رصيد للعميل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PrimaryTeal
                                )
                                Text(
                                    "تم قيد ${Formatters.formatCurrency(result.surplusAmount, result.currencySymbol)} كرصيد دائن لصالح العميل",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sb = StringBuilder()
                    sb.append("السلام عليكم ورحمة الله وبركاته\n")
                    sb.append("الأخ/ ${result.customerName} المحترم\n")
                    sb.append("تم استلام وتسجيل سند قبض بمبلغ: ${Formatters.formatCurrency(result.totalAmount, result.currencySymbol)}\n\n")
                    sb.append("📋 تفاصيل تسديد الجلسات:\n")
                    result.items.forEach { item ->
                        val dateStr = Formatters.formatDate(item.date)
                        val status = if (item.isFullyPaid) "خالص بالكامل ✅" else "متبقي: ${Formatters.formatCurrency(item.remainingDebtAfter, result.currencySymbol)} ⏳"
                        sb.append("• دورة #${item.sessionId} ($dateStr):\n")
                        sb.append("   - المسدد: ${Formatters.formatCurrency(item.allocatedAmount, result.currencySymbol)}\n")
                        sb.append("   - الحالة: $status\n")
                    }
                    if (result.surplusAmount > 0.0) {
                        sb.append("• رصيد فائض لحسابكم: ${Formatters.formatCurrency(result.surplusAmount, result.currencySymbol)} 💰\n")
                    }
                    val msg = FileSharingHelper.attachMessageFooter(sb.toString())
                    FileSharingHelper.sendWhatsAppMessage(context, result.customerPhone, msg)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("إشعار واتساب", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}

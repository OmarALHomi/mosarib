package com.example.features.vouchers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.core.ui.MosaribAutoCompleteTextField
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
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.core.license.LicenseDialog
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
import com.example.features.customers.Customer
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.sessions.SessionCardItem
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.SettleSessionDialog
import com.example.features.sessions.WaterSession
import com.example.features.sessions.WaterSessionWithCustomer
import com.example.core.ui.SessionDetailBottomSheet
import com.example.core.ui.VoucherDetailBottomSheet
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
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val frequentDescriptions by viewModel.frequentExpenseDescriptions.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val settlementResult by viewModel.settlementResult.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isAppDark()

    var showActivationDialog by remember { mutableStateOf(false) }
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
    var sessionToSettle by remember { mutableStateOf<WaterSession?>(null) }
    var voucherMessageTarget by remember { mutableStateOf<Triple<Customer, String, String>?>(null) }
    var voucherPdfReady by remember { mutableStateOf<Pair<File, String>?>(null) }
    var selectedLinkedSession by remember { mutableStateOf<WaterSession?>(null) }
    var sessionForDetails by remember { mutableStateOf<WaterSessionWithCustomer?>(null) }
    var voucherForDetails by remember { mutableStateOf<VoucherWithCustomer?>(null) }

    val vmPdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    LaunchedEffect(vmPdfReady) {
        if (vmPdfReady != null) {
            voucherPdfReady = vmPdfReady
            viewModel.clearPdfReady()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = if (isCompactHeight) 80.dp else 110.dp)
        ) {
            // Merged Integrated Financial Summary Card (يتلاشى بسلاسة تامة مع التمرير دون قفز)
            item {
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
                            alpha = summaryScrollAlpha
                            scaleY = 0.94f + (0.06f * summaryScrollAlpha)
                            scaleX = 0.96f + (0.04f * summaryScrollAlpha)
                        }
                        .shadow((3 * summaryScrollAlpha).dp, RoundedCornerShape(16.dp)),
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
                                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF2A1517) else Color(0xFFFFEBEE)),
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

            if (isCompactHeight) {
                // في الوضع الأفقي: دمج حقل البحث وزر التصدير والفلاتر في صف أفقي واحد مدمج
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 8.dp),
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
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "بحث...",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                    fontSize = 11.5.sp
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
                                                fontSize = 11.5.sp
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("operations_search_input")
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setSearchQuery("") },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // زر طباعة سجل العمليات كتقرير PDF
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .clickable { viewModel.exportOperationsLogPdf(context) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "طباعة سجل العمليات PDF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            LazyRow(
                                modifier = Modifier.weight(1.9f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.ALL,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.ALL) },
                                        label = "الكل",
                                        isCompact = true
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.SESSIONS,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.SESSIONS) },
                                        label = "سقي",
                                        isCompact = true
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.RECEIPTS,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.RECEIPTS) },
                                        label = "مقبوضات",
                                        isCompact = true
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.EXPENSES,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.EXPENSES) },
                                        label = "مصروفات",
                                        isCompact = true
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.DEFERRED,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.DEFERRED) },
                                        label = "آجل / ديون",
                                        isCompact = true
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.PURCHASES,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.PURCHASES) },
                                        label = "شراء ساعات",
                                        isCompact = true
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Search & Filters Row (Sticky Header تحت البوردر)
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = if (isStickyPinned) 3.dp else 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
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

                                // زر طباعة سجل العمليات كتقرير PDF
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                        .clickable { viewModel.exportOperationsLogPdf(context) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = "طباعة سجل العمليات PDF",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.ALL,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.ALL) },
                                        label = "الكل"
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.SESSIONS,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.SESSIONS) },
                                        label = "سقي"
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.RECEIPTS,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.RECEIPTS) },
                                        label = "مقبوضات"
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.EXPENSES,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.EXPENSES) },
                                        label = "مصروفات"
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.DEFERRED,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.DEFERRED) },
                                        label = "آجل / ديون"
                                    )
                                }
                                item {
                                    MosaribFilterChip(
                                        selected = operationsFilter == OperationsFilter.PURCHASES,
                                        onClick = { viewModel.setOperationsFilter(OperationsFilter.PURCHASES) },
                                        label = "شراء ساعات"
                                    )
                                }
                            }
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
                                onCardClick = { sessionForDetails = op.sessionWithCustomer },
                                onCustomerClick = { onNavigateToCustomer(op.sessionWithCustomer.session.customerId) },
                                onEditClick = { sessionToEdit = op.sessionWithCustomer.session },
                                onDeleteClick = { sessionToDelete = op.sessionWithCustomer.session },
                                onPdfClick = {
                                    val file = PdfReportGenerator.generateSessionInvoicePdf(
                                        context = context,
                                        config = config,
                                        customer = op.sessionWithCustomer.customer ?: Customer(name = "عميل غير محدد"),
                                        session = op.sessionWithCustomer.session
                                    )
                                    voucherPdfReady = Pair(file, "فاتورة ري #${op.sessionWithCustomer.session.id}")
                                },
                                onMessageClick = {
                                    op.sessionWithCustomer.customer?.let { c ->
                                        val s = op.sessionWithCustomer.session
                                        val waMsg = MessageFormatHelper.buildSessionBill(s, c, config, isSms = false)
                                        val smsMsg = MessageFormatHelper.buildSessionBill(s, c, config, isSms = true)
                                        voucherMessageTarget = Triple(c, waMsg, smsMsg)
                                    }
                                },
                                onSettleClick = {
                                    sessionToSettle = op.sessionWithCustomer.session
                                },
                                onShareVoucher = { v ->
                                    op.sessionWithCustomer.customer?.let { c ->
                                        val waMsg = MessageFormatHelper.buildReceiptVoucher(v, c, op.sessionWithCustomer.session.id, config, isSms = false)
                                        val smsMsg = MessageFormatHelper.buildReceiptVoucher(v, c, op.sessionWithCustomer.session.id, config, isSms = true)
                                        voucherMessageTarget = Triple(c, waMsg, smsMsg)
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
                                    .shadow(1.dp, RoundedCornerShape(12.dp))
                                    .clickable { voucherForDetails = op.voucherWithCustomer },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    // 1. الصف العلوي: علامة سحب أخضر + اسم العميل + نوع السند + رقم الساقية + المبلغ
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // اليمين: الأيقونة واسم العميل وبادج السند وبادج الساقية
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
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

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .weight(1f, fill = false)
                                                    .clickable {
                                                        if (voucher.customerId != null) {
                                                            onNavigateToCustomer(voucher.customerId)
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = titleText,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Surface(
                                                    color = badgeColor.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = if (isReceipt) "قبض" else if (voucher.type == VoucherType.DISCOUNT) "خصم" else "صرف",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = badgeColor,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.5.sp
                                                        ),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    )
                                                }

                                                if (voucher.sessionId != null && voucher.sessionId > 0) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = PrimaryTeal.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.clickable {
                                                            val foundSession = allSessions.find { it.id == voucher.sessionId }
                                                            if (foundSession != null) {
                                                                selectedLinkedSession = foundSession
                                                            } else {
                                                                viewModel.showToast("دورة سقي #${voucher.sessionId}")
                                                            }
                                                        }
                                                    ) {
                                                        Text(
                                                            text = "ساقية #${voucher.sessionId}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                color = PrimaryTeal,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 9.sp
                                                            ),
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // اليسار: المبلغ المقبوض/المصروف
                                        Text(
                                            text = (if (isReceipt) "+ " else "- ") + Formatters.formatCurrency(voucher.amount, config.currencySymbol),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = badgeColor,
                                                fontSize = 15.sp
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    if (voucher.notes.isNotBlank() && voucher.notes != titleText && voucher.notes != "عام") {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                                    contentDescription = null,
                                                    tint = badgeColor,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "البيان: ${voucher.notes}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 11.5.sp
                                                    ),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 2. الصف السفلي: التاريخ + طريقة الدفع (مع اقتطاع آمن للنصوص الكبيرة) + رقم السند + الأزرار
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = Formatters.formatDateTime(voucher.date),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 9.5.sp
                                                ),
                                                maxLines = 1,
                                                softWrap = false
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
                                                ),
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )

                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                                            )

                                            Text(
                                                text = "#${voucher.voucherNumber}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 9.sp
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // أزرار العمليات المصغرة (مشاركة + PDF + تعديل + حذف)
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

                                            // زر تعديل
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { voucherToEdit = voucher },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل",
                                                    tint = PrimaryTeal,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }

                                            // زر حذف
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isDark) Color(0xFF2B1313) else Color(0xFFFFEBEE))
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
                            val totalAmount = WellOwnerPurchaseMath.payableAmount(purchase)
                            val paidAmount = purchase.amountPaid
                            val remainingDebt = (totalAmount - paidAmount).coerceAtLeast(0.0)
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
                                    // 1. الصف العلوي: أيقونة + اسم صاحب البئر + الإجمالي المالي
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // اليمين: الأيقونة واسم صاحب البئر وبادج شراء ساعات
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
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

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .weight(1f, fill = false)
                                                    .clickable {
                                                        onNavigateToCustomer(purchase.ownerCustomerId)
                                                    }
                                            ) {
                                                Text(
                                                    text = owner?.name ?: "صاحب بئر #${purchase.ownerCustomerId}",
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        fontSize = 13.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Surface(
                                                    color = Color(0xFF0288D1).copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "شراء ساعات",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color(0xFF0288D1),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 9.5.sp
                                                        ),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // اليسار: الإجمالي الكلي للشراء
                                        Text(
                                            text = Formatters.formatCurrency(totalAmount, config.currencySymbol),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0288D1),
                                                fontSize = 15.sp
                                            ),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 2. الشريط المالي المنسق (صرف المدفوع vs عليك المتبقي)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // صرف (المدفوع نقداً)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (paidAmount > 0) Color(0xFFE53935) else Color.Gray,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = "صرف (مدفوع):",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 10.sp
                                                )
                                            )
                                            Text(
                                                text = Formatters.formatCurrency(paidAmount, config.currencySymbol),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (paidAmount > 0) Color(0xFFE53935) else Color.Gray,
                                                    fontSize = 10.5.sp
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        // عليك (المتبقي لصاحب البئر)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (remainingDebt > 0) "عليك (متبقي):" else "الحالة:",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 10.sp
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            if (remainingDebt > 0) {
                                                Surface(
                                                    color = if (isDark) Color(0xFF332014) else Color(0xFFFFF3E0),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = Formatters.formatCurrency(remainingDebt, config.currencySymbol),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFFE65100),
                                                            fontSize = 10.5.sp
                                                        ),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            } else {
                                                Surface(
                                                    color = if (isDark) Color(0xFF132A1C) else Color(0xFFE8F5E9),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "مسدد",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            color = AccentEmerald,
                                                            fontSize = 9.5.sp
                                                        ),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // 3. الصف السفلي: التاريخ والمواصفات + أزرار العمليات المصغرة
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // التفاصيل والتاريخ في سطر أفقي مدمج
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = Formatters.formatDateTime(purchase.date),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 9.sp
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )

                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                                            )

                                            Text(
                                                text = Formatters.formatDurationArabic(chargeable),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )

                                            if (waste > 0) {
                                                Text(
                                                    text = "(هدر: ${Formatters.formatDurationArabic(waste)})",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFFD32F2F),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 8.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }

                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.LightGray)
                                            )

                                            Text(
                                                text = "${Formatters.formatNumber(purchase.purchaseRatePerHour)}/س",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color(0xFF64748B),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )

                                            if (purchase.notes.isNotBlank()) {
                                                Text(
                                                    text = "(${purchase.notes})",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = Color(0xFF64748B),
                                                        fontSize = 8.5.sp
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // أزرار العمليات المصغرة (مشاركة + تعديل + حذف)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (owner != null && owner.phone.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                        .clickable {
                                                            val waMsg = MessageFormatHelper.buildPurchaseBill(
                                                                purchase = purchase,
                                                                owner = owner,
                                                                chargeableMinutes = chargeable,
                                                                wasteMinutes = waste,
                                                                totalAmount = totalAmount,
                                                                paidAmount = paidAmount,
                                                                remainingDebt = remainingDebt,
                                                                config = config,
                                                                isSms = false
                                                            )
                                                            val smsMsg = MessageFormatHelper.buildPurchaseBill(
                                                                purchase = purchase,
                                                                owner = owner,
                                                                chargeableMinutes = chargeable,
                                                                wasteMinutes = waste,
                                                                totalAmount = totalAmount,
                                                                paidAmount = paidAmount,
                                                                remainingDebt = remainingDebt,
                                                                config = config,
                                                                isSms = true
                                                            )
                                                            voucherMessageTarget = Triple(owner, waMsg, smsMsg)
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

                                            // زر تعديل
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    .clickable { purchaseToEdit = purchase },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل الشراء",
                                                    tint = PrimaryTeal,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }

                                            // زر حذف
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isDark) Color(0xFF2B1313) else Color(0xFFFFEBEE))
                                                    .clickable { purchaseToDelete = purchase },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف الشراء",
                                                    tint = Color(0xFFE53935),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
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
                .padding(start = 16.dp, bottom = if (isCompactHeight) 10.dp else 16.dp)
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

                    // خيار 3: شراء ساعات (من صاحب بئر)
                    Surface(
                        onClick = {
                            speedDialOpen = false
                            if (LicenseManager.canPerformOperation(context, operationsCount)) {
                                showAddPurchaseSheet = true
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
                                    .background(Color(0xFFE1F5FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFF0288D1),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "شراء ساعات (من بئر)",
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
                    .size(if (isCompactHeight) 44.dp else 48.dp)
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
                    session = s
                )
                voucherPdfReady = Pair(file, "فاتورة ري #${s.id}")
            },
            onMessage = {
                sessionForDetails = null
                if (c != null) {
                    val waMsg = MessageFormatHelper.buildSessionBill(s, c, config, isSms = false)
                    val smsMsg = MessageFormatHelper.buildSessionBill(s, c, config, isSms = true)
                    voucherMessageTarget = Triple(c, waMsg, smsMsg)
                }
            },
            onSettle = if (s.remainingDebt > 0) {
                {
                    sessionForDetails = null
                    sessionToSettle = s
                }
            } else null,
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

    // Settle Session Dialog
    sessionToSettle?.let { s ->
        val cust = customers.find { it.id == s.customerId }
        SettleSessionDialog(
            session = s,
            customerName = cust?.name ?: "عميل غير محدد",
            currencySymbol = config.currencySymbol,
            onDismiss = { sessionToSettle = null },
            onConfirmSettle = { amt, discount, method, notes ->
                viewModel.settleSessionDebt(s, amt, method, notes, discount)
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
            onDismiss = { voucherToEdit = null },
            onSave = { type, custId, amt, cat, method, notes, sessionId, _, _ ->
                viewModel.updateVoucher(
                    voucher = v,
                    amount = amt,
                    paymentMethod = method,
                    notes = notes,
                    category = cat,
                    customerId = custId
                )
                voucherToEdit = null
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
            isMandatory = LicenseManager.isTrialLimitExceeded(context, operationsCount)
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

    var type by remember { mutableStateOf(initialType) }
    var selectedCustomerId by remember {
        mutableLongStateOf(
            if (initialVoucher?.type == VoucherType.RECEIPT && initialVoucher.customerId != null) initialVoucher.customerId!!
            else customers.firstOrNull()?.id ?: 0L
        )
    }
    var selectedBeneficiaryId by remember {
        mutableStateOf<Long?>(
            if (initialVoucher?.type == VoucherType.EXPENSE) initialVoucher.customerId else null
        )
    }
    var selectedSessionIds by remember { mutableStateOf(setOf<Long>()) }
    var amountStr by remember { mutableStateOf(if (initialVoucher != null && initialVoucher.amount > 0) initialVoucher.amount.toString() else "") }
    var discountAmountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf(initialVoucher?.paymentMethod ?: "نقداً") }
    var notes by remember { mutableStateOf(initialVoucher?.notes ?: "") }

    var custDropdownExpanded by remember { mutableStateOf(false) }
    var custSearchQuery by remember { mutableStateOf("") }
    var beneficiaryDropdownExpanded by remember { mutableStateOf(false) }
    var beneficiarySearchQuery by remember { mutableStateOf("") }

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
                val headerTitle = when {
                    initialVoucher != null && type == VoucherType.RECEIPT -> "تعديل سند قبض #${initialVoucher.voucherNumber.ifEmpty { initialVoucher.id.toString() }}"
                    initialVoucher != null -> "تعديل سند صرف #${initialVoucher.voucherNumber.ifEmpty { initialVoucher.id.toString() }}"
                    type == VoucherType.RECEIPT -> "سند قبض"
                    else -> "تسجيل مصروف"
                }
                Text(
                    text = headerTitle,
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
                MosaribButton(
                    text = "قبض من عميل",
                    onClick = { type = VoucherType.RECEIPT },
                    modifier = Modifier.weight(1f),
                    containerColor = if (type == VoucherType.RECEIPT) AccentEmerald else if (isDark) Color(0xFF141F25) else Color(0xFFF1F5F9),
                    contentColor = if (type == VoucherType.RECEIPT) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )

                MosaribButton(
                    text = "مصروف",
                    onClick = { type = VoucherType.EXPENSE },
                    modifier = Modifier.weight(1f),
                    containerColor = if (type == VoucherType.EXPENSE) Color(0xFFE53935) else if (isDark) Color(0xFF141F25) else Color(0xFFF1F5F9),
                    contentColor = if (type == VoucherType.EXPENSE) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (type == VoucherType.RECEIPT) {
                Text("العميل المستلم منه *", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                MosaribAutoCompleteTextField(
                    value = custSearchQuery.ifEmpty { selectedCustomer?.name ?: "" },
                    onValueChange = {
                        custSearchQuery = it
                        val exact = customers.find { c -> c.name.trim().equals(it.trim(), ignoreCase = true) }
                        if (exact != null) {
                            selectedCustomerId = exact.id
                            selectedSessionIds = emptySet()
                        } else {
                            selectedCustomerId = 0L
                        }
                    },
                    items = customers,
                    itemText = { it.name },
                    itemDetail = { listOf(it.farmName, it.phone).filter { s -> s.isNotBlank() }.joinToString(" • ") },
                    isItemSecondary = { it.isWellOwner },
                    isItemSelected = { it.id == selectedCustomerId },
                    placeholder = "اكتب او اختر الاسم",
                    leadingIcon = {
                        Icon(
                            imageVector = if (selectedCustomer?.isWellOwner == true) Icons.Default.WaterDrop else Icons.Default.Eco,
                            contentDescription = null,
                            tint = if (selectedCustomer?.isWellOwner == true) Color(0xFF0288D1) else PrimaryTeal
                        )
                    },
                    onItemSelected = { c ->
                        selectedCustomerId = c.id
                        custSearchQuery = c.name
                        selectedSessionIds = emptySet()
                        amountStr = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // قسم الجلسات غير المسددة المتوفرة على العميل
                if (unsettledSessions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF141F25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else Color(0xFFE2E8F0))
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
                                        style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFF87171) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
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
                            val parsedDisc = Formatters.parseAmountInput(discountAmountStr)
                            val sortedSelected = unsettledSessions.filter { selectedSessionIds.contains(it.id) }.sortedBy { it.startTime }

                            val fifoPreview = calculateFifoAllocation(sortedSelected, parsedAmt, parsedDisc)
                            val allocations = fifoPreview.steps.associate { it.session.id to Triple(it.allocatedAmount, it.allocatedDiscount, it.newDebt) }

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
                                                if (isDark) Color(0xFF10282E) else PrimaryTeal.copy(alpha = 0.08f)
                                            } else {
                                                if (isDark) Color(0xFF182329) else Color.White
                                            }
                                        ),
                                        border = if (isSelected) {
                                            BorderStroke(1.5.dp, PrimaryTeal)
                                        } else {
                                            BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else Color.LightGray.copy(alpha = 0.4f))
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
                                                        color = if (isDark) Color(0xFFF87171) else Color(0xFFC62828)
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
                                                val (allocatedCash, allocatedDisc, remAfter) = allocInfo
                                                val (badgeBg, badgeTextColor, badgeText) = when {
                                                    parsedAmt <= 0.0 && parsedDisc <= 0.0 -> Triple(
                                                        if (isDark) Color(0xFF202A30) else Color(0xFFEEEEEE),
                                                        if (isDark) Color(0xFF94A3B8) else Color.DarkGray,
                                                        "أدخل المبلغ لمعاينة التوزيع"
                                                    )
                                                    remAfter == 0.0 -> Triple(
                                                        if (isDark) Color(0xFF11261B) else Color(0xFFE8F5E9),
                                                        if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                                                        if (allocatedDisc > 0.0) {
                                                            "ستُسدد بالكامل (${Formatters.formatCurrency(allocatedCash, currencySymbol)} نقد + ${Formatters.formatCurrency(allocatedDisc, currencySymbol)} خصم)"
                                                        } else {
                                                            "ستُسدد بالكامل (${Formatters.formatCurrency(allocatedCash, currencySymbol)})"
                                                        }
                                                    )
                                                    allocatedCash > 0.0 || allocatedDisc > 0.0 -> Triple(
                                                        if (isDark) Color(0xFF2A2010) else Color(0xFFFFF8E1),
                                                        if (isDark) Color(0xFFFBBF24) else Color(0xFFF57F17),
                                                        "سداد جزئي: ${Formatters.formatCurrency(allocatedCash, currencySymbol)}${if (allocatedDisc > 0.0) " + خصم ${Formatters.formatCurrency(allocatedDisc, currencySymbol)}" else ""} (متبقي: ${Formatters.formatCurrency(remAfter, currencySymbol)})"
                                                    )
                                                    else -> Triple(
                                                        if (isDark) Color(0xFF2B1313) else Color(0xFFFFEBEE),
                                                        if (isDark) Color(0xFFF87171) else Color(0xFFC62828),
                                                        "لم يشملها المبلغ (المبلغ غير كافٍ)"
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
                Text("العميل / المستلم (اختياري):", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    MosaribTextField(
                        value = selectedBeneficiary?.name ?: "مصروف عام (غير مقيد على عميل)",
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
                    DropdownMenu(
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
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryTeal)
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
                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("مصروف عام (غير مقيد على عميل)", fontWeight = FontWeight.Bold, color = PrimaryTeal) },
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
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (c.isWellOwner) Icons.Default.WaterDrop else Icons.Default.Eco,
                                        contentDescription = null,
                                        tint = if (c.isWellOwner) Color(0xFF0288D1) else PrimaryTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Column {
                                        Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                        val sub = listOf(c.farmName, c.phone).filter { it.isNotBlank() }.joinToString(" • ")
                                        if (sub.isNotBlank()) {
                                            Text(sub, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                },
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

            Spacer(modifier = Modifier.height(14.dp))

            MosaribTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = "المبلغ ($currencySymbol) *",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voucher_amount_input")
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            MosaribTextField(
                value = notes,
                onValueChange = { notes = it },
                label = if (type == VoucherType.RECEIPT) "البيان والملاحظات" else "بيان المصروف *",
                placeholder = if (type == VoucherType.RECEIPT) "مثال: دفعة ري أو سداد حساب" else "اكتب بيان المصروف...",
                modifier = Modifier.fillMaxWidth()
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

            MosaribButton(
                text = if (initialVoucher != null) "حفظ التعديلات" else "حفظ وترحيل السند",
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    val disc = Formatters.parseAmountInput(discountAmountStr)
                    if (amt > 0 || (type == VoucherType.RECEIPT && disc > 0)) {
                        val custId = if (type == VoucherType.RECEIPT) selectedCustomerId else selectedBeneficiaryId
                        val cat = if (type == VoucherType.RECEIPT) "سداد حساب" else notes.ifBlank { "مصروف" }
                        val selList = if (type == VoucherType.RECEIPT) selectedSessionIds.toList() else emptyList()
                        onSave(type, custId, amt, cat, paymentMethod, notes, null, selList, disc)
                        onDismiss()
                    }
                },
                enabled = Formatters.parseAmountInput(amountStr) > 0 || (type == VoucherType.RECEIPT && Formatters.parseAmountInput(discountAmountStr) > 0),
                containerColor = if (type == VoucherType.RECEIPT) AccentEmerald else PrimaryTeal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_voucher_button")
            )
        }
    }
}

@Composable
fun SettlementResultDialog(
    result: SettlementResult,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppDark()
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
                        containerColor = if (isDark) Color(0xFF141F25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else Color(0xFFE2E8F0)),
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
                        if (result.discountAmount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("خصم تسوية ومسامحة:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
                                Text(
                                    Formatters.formatCurrency(result.discountAmount, result.currencySymbol),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
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
                                item.isFullyPaid -> if (isDark) Color(0xFF11261B) else Color(0xFFE8F5E9)
                                item.allocatedAmount > 0 -> if (isDark) Color(0xFF2A2010) else Color(0xFFFFF8E1)
                                else -> if (isDark) Color(0xFF2B1313) else Color(0xFFFFEBEE)
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                item.isFullyPaid -> if (isDark) Color(0xFF166534) else Color(0xFFC8E6C9)
                                item.allocatedAmount > 0 -> if (isDark) Color(0xFF854D0E) else Color(0xFFFEF08A)
                                else -> if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA)
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
                                val paidDesc = buildString {
                                    append("المسدد: ${Formatters.formatCurrency(item.allocatedAmount, result.currencySymbol)}")
                                    if (item.allocatedDiscount > 0.0) {
                                        append(" + خصم: ${Formatters.formatCurrency(item.allocatedDiscount, result.currencySymbol)}")
                                    }
                                }
                                Text(
                                    text = paidDesc,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (item.allocatedAmount > 0 || item.allocatedDiscount > 0) PrimaryTeal else if (isDark) Color(0xFFF87171) else Color(0xFFC62828)
                                )
                                if (item.isFullyPaid) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "مسدد",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = when {
                                            item.allocatedAmount > 0 -> "متبقي: ${Formatters.formatCurrency(item.remainingDebtAfter, result.currencySymbol)}"
                                            else -> "لم يشملها المبلغ"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = when {
                                            item.allocatedAmount > 0 -> if (isDark) Color(0xFFFBBF24) else Color(0xFFF57F17)
                                            else -> if (isDark) Color(0xFFF87171) else Color(0xFFC62828)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (result.surplusAmount > 0.0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF0F2624) else Color(0xFFE0F2F1)
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF115E59) else Color(0xFF99F6E4)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val smsMsg = MessageFormatHelper.buildSettlementMessage(result, isSms = true)
                        FileSharingHelper.sendSms(context, result.customerPhone, smsMsg)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val waMsg = MessageFormatHelper.buildSettlementMessage(result, isSms = false)
                        FileSharingHelper.sendWhatsAppMessage(context, result.customerPhone, waMsg)
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
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("واتساب", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}

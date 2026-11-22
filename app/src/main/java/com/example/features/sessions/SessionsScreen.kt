package com.example.features.sessions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.ui.StatBoxCard
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.core.util.MessageFormatHelper
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.core.ui.SessionDetailBottomSheet
import com.example.features.customers.Customer
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    onNavigateToCustomer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val sessions by viewModel.filteredSessions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val paymentFilter by viewModel.paymentFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val homeStats by viewModel.homeStats.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val requestedCustomerForNewSession by viewModel.requestedCustomerForNewSession.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPeriodMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var showActivationDialog by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<WaterSession?>(null) }
    var defaultCustomerIdForForm by remember { mutableStateOf<Long?>(null) }
    var messageTargetSession by remember { mutableStateOf<Triple<WaterSession, Customer, Customer?>?>(null) }
    var messageTargetCustom by remember { mutableStateOf<Triple<Customer, String, String>?>(null) }
    var sessionForDetails by remember { mutableStateOf<WaterSessionWithCustomer?>(null) }

    LaunchedEffect(requestedCustomerForNewSession) {
        requestedCustomerForNewSession?.let { customerId ->
            defaultCustomerIdForForm = customerId
            showAddManualSheet = true
            viewModel.clearNewSessionCustomerRequest()
        }
    }

    val listState = rememberLazyListState()
    val isStickyPinned by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // Search Bar & Filter Chips (Sticky Header تحت البوردر)
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = if (isStickyPinned) 3.dp else 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("بحث باسم العميل أو الملاحظات...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sessions_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. فلتر الفترة (الكل، اليوم، هذا الأسبوع، هذا الشهر)
                        Box {
                            FilterChip(
                                selected = filter != SessionFilter.ALL,
                                onClick = { showPeriodMenu = true },
                                label = {
                                    Text(
                                        text = when (filter) {
                                            SessionFilter.ALL -> "الفترة: الكل"
                                            SessionFilter.TODAY -> "اليوم"
                                            SessionFilter.THIS_WEEK -> "هذا الأسبوع"
                                            SessionFilter.THIS_MONTH -> "هذا الشهر"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = showPeriodMenu,
                                onDismissRequest = { showPeriodMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("كل الفترات") },
                                    onClick = {
                                        viewModel.setFilter(SessionFilter.ALL)
                                        showPeriodMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("اليوم") },
                                    onClick = {
                                        viewModel.setFilter(SessionFilter.TODAY)
                                        showPeriodMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("هذا الأسبوع") },
                                    onClick = {
                                        viewModel.setFilter(SessionFilter.THIS_WEEK)
                                        showPeriodMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("هذا الشهر") },
                                    onClick = {
                                        viewModel.setFilter(SessionFilter.THIS_MONTH)
                                        showPeriodMenu = false
                                    }
                                )
                            }
                        }

                        // 2. فلتر حالة السداد (الكل / متبقي / مسدد)
                        Box {
                            FilterChip(
                                selected = paymentFilter != SessionPaymentFilter.ALL,
                                onClick = { showPaymentMenu = true },
                                label = {
                                    Text(
                                        text = when (paymentFilter) {
                                            SessionPaymentFilter.ALL -> "الحالة: الكل"
                                            SessionPaymentFilter.UNSETTLED -> "متبقي"
                                            SessionPaymentFilter.SETTLED -> "مسدد"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    when (paymentFilter) {
                                        SessionPaymentFilter.ALL -> null
                                        SessionPaymentFilter.UNSETTLED -> Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (paymentFilter != SessionPaymentFilter.ALL) Color.White else Color(0xFFE53935))
                                        )
                                        SessionPaymentFilter.SETTLED -> Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (paymentFilter != SessionPaymentFilter.ALL) Color.White else Color(0xFF2E7D32))
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White
                                )
                            )
                            DropdownMenu(
                                expanded = showPaymentMenu,
                                onDismissRequest = { showPaymentMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("الكل") },
                                    onClick = {
                                        viewModel.setPaymentFilter(SessionPaymentFilter.ALL)
                                        showPaymentMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("متبقي (غير مسدد)") },
                                    leadingIcon = { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE53935))) },
                                    onClick = {
                                        viewModel.setPaymentFilter(SessionPaymentFilter.UNSETTLED)
                                        showPaymentMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("مسدد بالكامل") },
                                    leadingIcon = { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF2E7D32))) },
                                    onClick = {
                                        viewModel.setPaymentFilter(SessionPaymentFilter.SETTLED)
                                        showPaymentMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }



            // Empty State or Session Items
            if (sessions.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.WaterDrop,
                        title = "لا توجد دورات ماء مسجلة",
                        description = "اضغط على زر الإضافة لتسجيل مدة السقي والمبلغ المدفوع"
                    )
                }
            } else {
                items(sessions, key = { it.session.id }) { item ->
                    val linked = allVouchers.filter { it.sessionId == item.session.id }
                    SessionCardItem(
                        sessionWithCustomer = item,
                        currencySymbol = config.currencySymbol,
                        linkedVouchers = linked,
                        onCardClick = { sessionForDetails = item },
                        onCustomerClick = { onNavigateToCustomer(item.session.customerId) },
                        onEditClick = { sessionToEdit = item.session },
                        onDeleteClick = { sessionToDelete = item.session },
                        onPdfClick = {
                            item.customer?.let { c ->
                                viewModel.generateAndShareInvoice(item.session, c, item.billedToCustomer)
                            }
                        },
                        onMessageClick = {
                            item.customer?.let { c ->
                                messageTargetSession = Triple(item.session, c, item.billedToCustomer)
                            }
                        },
                        onShareVoucher = { voucher ->
                            item.customer?.let { c ->
                                val waMsg = MessageFormatHelper.buildReceiptVoucher(voucher, c, item.session.id, config, isSms = false)
                                val smsMsg = MessageFormatHelper.buildReceiptVoucher(voucher, c, item.session.id, config, isSms = true)
                                messageTargetCustom = Triple(c, waMsg, smsMsg)
                            }
                        }
                    )
                }
            }
        }

        // Floating Action Button (Consolidated Single Action)
        ExtendedFloatingActionButton(
            onClick = {
                if (LicenseManager.canPerformOperation(context, operationsCount)) {
                    showAddManualSheet = true
                } else {
                    showActivationDialog = true
                }
            },
            containerColor = PrimaryTeal,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(26.dp)) },
            text = { Text("دورة جديدة", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .testTag("fab_add_session")
        )

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        )
    }

    // Message Choice Dialog (WhatsApp or SMS)
    messageTargetSession?.let { (session, customer, sponsorCustomer) ->
        val waMsg = viewModel.buildBillMessage(session, customer, config, isSms = false, sponsorCustomer = sponsorCustomer)
        val smsMsg = viewModel.buildBillMessage(session, customer, config, isSms = true, sponsorCustomer = sponsorCustomer)
        SendMessageChoiceDialog(
            recipientName = customer.name,
            recipientPhone = customer.phone,
            messageText = waMsg,
            smsMessageText = smsMsg,
            onDismiss = { messageTargetSession = null },
            onSendWhatsApp = {
                messageTargetSession = null
                viewModel.sendWhatsAppBill(session, customer, sponsorCustomer)
            },
            onSendSms = {
                messageTargetSession = null
                viewModel.sendSmsBill(session, customer, sponsorCustomer)
            }
        )
    }

    // Choice Dialog for Voucher Receipt
    messageTargetCustom?.let { (cust, waMsg, smsMsg) ->
        SendMessageChoiceDialog(
            recipientName = cust.name,
            recipientPhone = cust.phone,
            messageText = waMsg,
            smsMessageText = smsMsg,
            onDismiss = { messageTargetCustom = null },
            onSendWhatsApp = {
                messageTargetCustom = null
                FileSharingHelper.sendWhatsAppMessage(context, cust.phone, waMsg)
            },
            onSendSms = {
                messageTargetCustom = null
                FileSharingHelper.sendSms(context, cust.phone, smsMsg)
            }
        )
    }

    // PDF Open / Share Dialog
    pdfReady?.let { (file, title) ->
        com.example.core.ui.MosaribFileActionDialog(
            file = file,
            title = title,
            fileType = "PDF",
            onOpen = {
                com.example.core.util.FileSharingHelper.openPdf(context, file)
                viewModel.clearPdfReady()
            },
            onShare = {
                com.example.core.util.FileSharingHelper.sharePdf(context, file, title)
                viewModel.clearPdfReady()
            },
            onDismiss = { viewModel.clearPdfReady() }
        )
    }

    // Floating Detail BottomSheet for Session
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
                if (c != null) {
                    viewModel.generateAndShareInvoice(s, c, item.billedToCustomer)
                }
            },
            onMessage = {
                sessionForDetails = null
                if (c != null) {
                    messageTargetSession = Triple(s, c, item.billedToCustomer)
                }
            },
            onShareVoucher = { v ->
                if (c != null) {
                    val waMsg = MessageFormatHelper.buildReceiptVoucher(v, c, s.id, config, isSms = false)
                    val smsMsg = MessageFormatHelper.buildReceiptVoucher(v, c, s.id, config, isSms = true)
                    messageTargetCustom = Triple(c, waMsg, smsMsg)
                }
            }
        )
    }

    // Add / Edit Manual Session Sheet
    if (showAddManualSheet || sessionToEdit != null) {
        AddEditSessionBottomSheet(
            initialSession = sessionToEdit,
            customers = customers,
            defaultCustomerId = if (sessionToEdit == null) defaultCustomerIdForForm else null,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> viewModel.createCustomer(customer) },
            onDismiss = {
                showAddManualSheet = false
                sessionToEdit = null
                defaultCustomerIdForForm = null
            },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes, billedTo, wastedMins, wastedReason, discount, costRate, pumpId ->
                viewModel.saveManualSession(
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

    // Delete Confirmation Dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
            text = { Text("هل تريد حذف هذه العملية من كشف الحساب؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("إلغاء")
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


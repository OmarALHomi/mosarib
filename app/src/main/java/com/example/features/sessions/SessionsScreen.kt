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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
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
import com.example.core.license.LicenseDialog
import com.example.core.license.LicenseManager
import com.example.features.customers.Customer
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    viewModel: SessionsViewModel,
    onNavigateToCustomer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val sessions by viewModel.filteredSessions.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val pumps by viewModel.pumps.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val homeStats by viewModel.homeStats.collectAsStateWithLifecycle()
    val allVouchers by viewModel.allVouchers.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val operationsCount by viewModel.operationsCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showActivationDialog by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToSettle by remember { mutableStateOf<WaterSession?>(null) }
    var messageTargetSession by remember { mutableStateOf<Pair<WaterSession, Customer>?>(null) }
    var messageTargetCustom by remember { mutableStateOf<Pair<Customer, String>?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // Search Bar & Filter Chips
            item {
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

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = filter == SessionFilter.ALL,
                                onClick = { viewModel.setFilter(SessionFilter.ALL) },
                                label = { Text("كل الجلسات") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filter == SessionFilter.TODAY,
                                onClick = { viewModel.setFilter(SessionFilter.TODAY) },
                                label = { Text("اليوم") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filter == SessionFilter.THIS_WEEK,
                                onClick = { viewModel.setFilter(SessionFilter.THIS_WEEK) },
                                label = { Text("هذا الأسبوع") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = filter == SessionFilter.THIS_MONTH,
                                onClick = { viewModel.setFilter(SessionFilter.THIS_MONTH) },
                                label = { Text("هذا الشهر") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
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
                        onCustomerClick = { onNavigateToCustomer(item.session.customerId) },
                        onEditClick = { sessionToEdit = item.session },
                        onDeleteClick = { sessionToDelete = item.session },
                        onPdfClick = {
                            item.customer?.let { c ->
                                viewModel.generateAndShareInvoice(item.session, c)
                            }
                        },
                        onMessageClick = {
                            item.customer?.let { c ->
                                messageTargetSession = Pair(item.session, c)
                            }
                        },
                        onSettleClick = {
                            sessionToSettle = item.session
                        },
                        onShareVoucher = { voucher ->
                            item.customer?.let { c ->
                                val rawMsg = "سند قبض #${voucher.voucherNumber.ifEmpty { voucher.id.toString() }}\nسداد دورة سقي #${item.session.id}\nالعميل: ${c.name}\nالمبلغ: ${Formatters.formatCurrency(voucher.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(voucher.amount, config.currencySymbol)})\nطريقة الدفع: ${voucher.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(voucher.date)}"
                                messageTargetCustom = Pair(c, FileSharingHelper.attachMessageFooter(rawMsg))
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
    messageTargetSession?.let { (session, customer) ->
        val msg = viewModel.buildBillMessage(session, customer, config)
        SendMessageChoiceDialog(
            recipientName = customer.name,
            recipientPhone = customer.phone,
            messageText = msg,
            onDismiss = { messageTargetSession = null },
            onSendWhatsApp = {
                messageTargetSession = null
                viewModel.sendWhatsAppBill(session, customer)
            },
            onSendSms = {
                messageTargetSession = null
                viewModel.sendSmsBill(session, customer)
            }
        )
    }

    // Choice Dialog for Voucher Receipt
    messageTargetCustom?.let { (cust, msg) ->
        SendMessageChoiceDialog(
            recipientName = cust.name,
            recipientPhone = cust.phone,
            messageText = msg,
            onDismiss = { messageTargetCustom = null },
            onSendWhatsApp = {
                messageTargetCustom = null
                FileSharingHelper.sendWhatsAppMessage(context, cust.phone, msg)
            },
            onSendSms = {
                messageTargetCustom = null
                FileSharingHelper.sendSms(context, cust.phone, msg)
            }
        )
    }

    // PDF Open / Share Dialog
    pdfReady?.let { (file, title) ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPdfReady() },
            title = { Text("الفاتورة جاهزة", fontWeight = FontWeight.Bold) },
            text = { Text("ماذا تريد أن تفعل بهذه الفاتورة؟") },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.core.util.FileSharingHelper.openPdf(context, file)
                        viewModel.clearPdfReady()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) { Text("فتح / عرض") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        com.example.core.util.FileSharingHelper.sharePdf(context, file, title)
                        viewModel.clearPdfReady()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) { Text("مشاركة") }
            }
        )
    }

    // Settle Session Debt Dialog
    sessionToSettle?.let { session ->
        val cust = customers.find { it.id == session.customerId }
        SettleSessionDialog(
            session = session,
            customerName = cust?.name ?: "عميل غير محدد",
            currencySymbol = config.currencySymbol,
            onDismiss = { sessionToSettle = null },
            onConfirmSettle = { amt, method, notes ->
                viewModel.settleSessionDebt(session, amt, method, notes)
            }
        )
    }

    // Add / Edit Manual Session Sheet
    if (showAddManualSheet || sessionToEdit != null) {
        AddEditSessionBottomSheet(
            initialSession = sessionToEdit,
            customers = customers,
            pumps = pumps,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer -> viewModel.createCustomer(customer) },
            onDismiss = {
                showAddManualSheet = false
                sessionToEdit = null
            },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes ->
                viewModel.saveManualSession(id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes)
            }
        )
    }

    // Delete Confirmation Dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف هذه الجلسة؟ سيتم خصم قيمتها من كشف حساب العميل.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("نعم، حذف")
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
            isMandatory = operationsCount >= LicenseManager.FREE_OPERATIONS_LIMIT
        )
    }
}


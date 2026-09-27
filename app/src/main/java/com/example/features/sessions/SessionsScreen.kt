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
import com.example.core.ui.StatBoxCard
import com.example.core.util.Formatters
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
    val liveTimerState by viewModel.liveTimerState.collectAsStateWithLifecycle()
    val homeStats by viewModel.homeStats.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showActionChoiceSheet by remember { mutableStateOf(false) }
    var showAddManualSheet by remember { mutableStateOf(false) }
    var showStartLiveSheet by remember { mutableStateOf(false) }
    var showStopLiveSheet by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<WaterSession?>(null) }
    var sessionToDelete by remember { mutableStateOf<WaterSession?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // Home Dashboard Statistics (2 High-impact cards: Cash collected vs Outstanding debt)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxCard(
                        title = "المقبوض كاش",
                        value = Formatters.formatCurrency(homeStats.totalCollectedCash, config.currencySymbol),
                        subtitle = if (homeStats.totalCollectedCash > 0) Formatters.amountToArabicWords(homeStats.totalCollectedCash, config.currencySymbol) else null,
                        icon = Icons.Default.ArrowDownward,
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxCard(
                        title = "ديون متبقية",
                        value = Formatters.formatCurrency(homeStats.totalOutstandingDebt, config.currencySymbol),
                        subtitle = if (homeStats.totalOutstandingDebt > 0) Formatters.amountToArabicWords(homeStats.totalOutstandingDebt, config.currencySymbol) else "خالص بالكامل",
                        icon = Icons.Default.AttachMoney,
                        accentColor = if (homeStats.totalOutstandingDebt > 0) Color(0xFFE53935) else AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Live Timer Active Card
            item {
                AnimatedVisibility(visible = liveTimerState.isRunning) {
                    LiveTimerRunningBanner(
                        state = liveTimerState,
                        currencySymbol = config.currencySymbol,
                        onStopClick = { showStopLiveSheet = true },
                        onCancelClick = { viewModel.cancelLiveSession() }
                    )
                }
            }

            // Search Bar & Filter Chips
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("بحث...") },
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
                        description = "اضغط على زر الإضافة لتسجيل دورة ري يدوية أو بدء عداد ري مباشر"
                    )
                }
            } else {
                items(sessions, key = { it.session.id }) { item ->
                    SessionCardItem(
                        sessionWithCustomer = item,
                        currencySymbol = config.currencySymbol,
                        onCustomerClick = { onNavigateToCustomer(item.session.customerId) },
                        onEditClick = { sessionToEdit = item.session },
                        onDeleteClick = { sessionToDelete = item.session },
                        onPdfClick = {
                            item.customer?.let { c ->
                                viewModel.generateAndShareInvoice(item.session, c)
                            }
                        },
                        onWhatsAppClick = {
                            item.customer?.let { c ->
                                viewModel.sendWhatsAppBill(item.session, c)
                            }
                        },
                        onSmsClick = {
                            item.customer?.let { c ->
                                viewModel.sendSmsBill(item.session, c)
                            }
                        }
                    )
                }
            }
        }

        // Floating Action Button (Consolidated Single Action)
        ExtendedFloatingActionButton(
            onClick = {
                if (customers.isEmpty()) {
                    viewModel.showToast("يرجى إضافة عميل أولاً", com.example.core.ui.ToastType.WARNING)
                } else {
                    showActionChoiceSheet = true
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
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
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

    // Action Choice Bottom Sheet (Visual & Illiterate-Friendly)
    if (showActionChoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActionChoiceSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "تسجيل دورة ماء جديدة",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (!liveTimerState.isRunning) {
                    Card(
                        onClick = {
                            showActionChoiceSheet = false
                            showStartLiveSheet = true
                        },
                        colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(AccentEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("عداد مباشر (تشغيل فوري)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentEmerald))
                                Text("بدء العداد وحساب الوقت تلقائياً", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Card(
                    onClick = {
                        showActionChoiceSheet = false
                        showAddManualSheet = true
                    },
                    colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("تسجيل يدوي (ساعات سابقة)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal))
                            Text("تسجيل سقي سابق وتحديد الساعات والمبلغ", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Add / Edit Manual Session Sheet
    if (showAddManualSheet || sessionToEdit != null) {
        AddEditSessionBottomSheet(
            initialSession = sessionToEdit,
            customers = customers,
            pumps = pumps,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onDismiss = {
                showAddManualSheet = false
                sessionToEdit = null
            },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes ->
                viewModel.saveManualSession(id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes)
            }
        )
    }

    // Start Live Timer Sheet
    if (showStartLiveSheet) {
        StartLiveTimerBottomSheet(
            customers = customers,
            pumps = pumps,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onDismiss = { showStartLiveSheet = false },
            onStart = { custId, custName, pumpName, rate ->
                viewModel.startLiveSession(custId, custName, pumpName, rate)
            }
        )
    }

    // Stop Live Timer Sheet
    if (showStopLiveSheet) {
        StopLiveTimerBottomSheet(
            liveState = liveTimerState,
            currencySymbol = config.currencySymbol,
            onDismiss = { showStopLiveSheet = false },
            onConfirmStop = { paid, notes ->
                viewModel.stopAndSaveLiveSession(paid, notes)
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
}

@Composable
fun LiveTimerRunningBanner(
    state: LiveTimerState,
    currencySymbol: String,
    onStopClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(12.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF004D56), Color(0xFF0D253A))
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .scale(scale)
                                .clip(CircleShape)
                                .background(AccentEmerald)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عداد ري مباشر قيد التشغيل",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = state.pumpName,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = SecondaryAqua
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "العميل: ${state.customerName}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "سعر الساعة: ${Formatters.formatCurrency(state.pricePerHour, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFB0C9D4)
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = Formatters.formatDurationClock(state.elapsedSeconds),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        )
                        Text(
                            text = Formatters.formatCurrency(state.currentCost, currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AccentEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStopClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("stop_live_timer_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إيقاف وحفظ الفاتورة", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onCancelClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80))
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCardItem(
    sessionWithCustomer: WaterSessionWithCustomer,
    currencySymbol: String,
    onCustomerClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPdfClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onSmsClick: () -> Unit
) {
    val session = sessionWithCustomer.session
    val customer = sessionWithCustomer.customer
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. الصف العلوي: معلومات العميل على اليمين (في RTL) و 3 أزرار دائرية/مربعة ناعمة على اليسار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العميل والمزرعة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCustomerClick() }
                        .weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = customer?.name ?: "عميل غير محدد",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = if (!customer?.farmName.isNullOrBlank()) customer?.farmName!! else "جلسة ري",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // الأزرار الثلاثة على اليسار (في RTL: المشاركة، ثم PDF، ثم خيارات إضافية)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // زر المشاركة
                    IconButton(
                        onClick = onWhatsAppClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE0F2F1))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // زر فاتورة PDF
                    IconButton(
                        onClick = onPdfClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE0F2F1))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "فاتورة PDF",
                            tint = PrimaryTeal,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // زر القائمة (المزيد)
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0F2F1))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (!customer?.phone.isNullOrEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("إرسال رسالة SMS") },
                                    leadingIcon = { Icon(Icons.Default.Sms, contentDescription = null, tint = AccentGold) },
                                    onClick = {
                                        menuExpanded = false
                                        onSmsClick()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("تعديل") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف", color = Color(0xFFE53935)) },
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

            Spacer(modifier = Modifier.height(12.dp))

            // 2. الصف الأوسط: كبسولة كاملة للمدة وسعر الساعة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // المدة مع أيقونة الساعة (في RTL: على اليمين)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatDurationArabic(session.durationMinutes),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // سعر الساعة بالعربي (في RTL: على اليسار)
                Text(
                    text = "@ ${Formatters.formatNumber(session.pricePerHour)} $currencySymbol ساعة",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. الجزء السفلي: مقسم لعمودين مع فاصل رأسي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العمود الأيمن (في RTL): الإجمالي والمسدد والمتبقي (3 كبسولات عربية)
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "الإجمالي",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    // كبسولة الإجمالي
                    SessionMetricPill(
                        label = "الإجمالي",
                        value = "${Formatters.formatNumber(session.totalAmount)} $currencySymbol",
                        bgColor = Color(0xFFE8F5E9),
                        textColor = Color(0xFF00695C)
                    )

                    // كبسولة المدفوع
                    SessionMetricPill(
                        label = "المدفوع",
                        value = "${Formatters.formatNumber(session.amountPaid)} $currencySymbol",
                        bgColor = Color(0xFFE8F5E9),
                        textColor = Color(0xFF00695C)
                    )

                    // كبسولة المتبقي
                    if (session.remainingDebt > 0) {
                        SessionMetricPill(
                            label = "المتبقي",
                            value = "${Formatters.formatNumber(session.remainingDebt)} $currencySymbol",
                            bgColor = Color(0xFFFFEBEE),
                            textColor = Color(0xFFC62828)
                        )
                    } else {
                        SessionMetricPill(
                            label = "المتبقي",
                            value = "0 $currencySymbol (خالص)",
                            bgColor = Color(0xFFE8F5E9),
                            textColor = Color(0xFF2E7D32)
                        )
                    }
                }

                // فاصل رأسي رفيع
                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                // العمود الأيسر (في RTL): التاريخ والوقت + المبلغ المتبقي/الإجمالي بالعريض والمحمر + كتابة المبلغ بالعربي
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "التاريخ والوقت",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = Formatters.formatDateTime(session.startTime),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val highlightAmount = if (session.remainingDebt > 0) session.remainingDebt else session.totalAmount
                    val highlightColor = if (session.remainingDebt > 0) Color(0xFFB71C1C) else Color(0xFF00695C)

                    Text(
                        text = "${Formatters.formatNumber(highlightAmount)} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = highlightColor,
                            fontSize = 18.sp
                        )
                    )

                    Text(
                        text = Formatters.amountToArabicWords(highlightAmount, currencySymbol),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF8D6E63),
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 2
                    )
                }
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ملاحظة: ${session.notes}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SessionMetricPill(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp
            )
        )
    }
}

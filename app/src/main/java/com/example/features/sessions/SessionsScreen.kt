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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
            // Toast Notification
            item {
                LuxuryToastNotification(
                    toast = toast,
                    onDismiss = { viewModel.dismissToast() }
                )
            }

            // Home Dashboard Statistics (Clean borderless cards with large icons & Tafqeet)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBoxCard(
                            title = "ساعات السقي",
                            value = Formatters.formatDurationArabic(homeStats.totalWaterMinutes),
                            subtitle = "${homeStats.totalSessionsCount} دورة ري",
                            icon = Icons.Default.WaterDrop,
                            accentColor = SecondaryAqua,
                            modifier = Modifier.weight(1f)
                        )
                        StatBoxCard(
                            title = "المقبوض كاش",
                            value = Formatters.formatCurrency(homeStats.totalCollectedCash, config.currencySymbol),
                            subtitle = if (homeStats.totalCollectedCash > 0) Formatters.amountToArabicWords(homeStats.totalCollectedCash, config.currencySymbol) else null,
                            icon = Icons.Default.ArrowDownward,
                            accentColor = AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatBoxCard(
                            title = "ديون متبقية",
                            value = Formatters.formatCurrency(homeStats.totalOutstandingDebt, config.currencySymbol),
                            subtitle = if (homeStats.totalOutstandingDebt > 0) Formatters.amountToArabicWords(homeStats.totalOutstandingDebt, config.currencySymbol) else "خالص بالكامل",
                            icon = Icons.Default.AttachMoney,
                            accentColor = if (homeStats.totalOutstandingDebt > 0) Color(0xFFE53935) else AccentEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        StatBoxCard(
                            title = "إجمالي المصاريف",
                            value = Formatters.formatCurrency(homeStats.totalExpenses, config.currencySymbol),
                            subtitle = if (homeStats.totalExpenses > 0) Formatters.amountToArabicWords(homeStats.totalExpenses, config.currencySymbol) else null,
                            icon = Icons.Default.ArrowUpward,
                            accentColor = Color(0xFFFF5252),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Action Buttons (Designed for high visual accessibility / illiterate-friendly)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!liveTimerState.isRunning) {
                        Button(
                            onClick = {
                                if (customers.isEmpty()) {
                                    viewModel.showToast("يرجى إضافة عميل أولاً لبدء العداد", com.example.core.ui.ToastType.WARNING)
                                } else {
                                    showStartLiveSheet = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تشغيل العداد", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (customers.isEmpty()) {
                                viewModel.showToast("يرجى إضافة عميل أولاً", com.example.core.ui.ToastType.WARNING)
                            } else {
                                showAddManualSheet = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("دورة ماء جديدة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
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
                        placeholder = { Text("بحث باسم العميل، المزرعة، أو الملاحظات...") },
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

            // Summary Quick Metrics
            item {
                val totalMinutes = sessions.sumOf { it.session.durationMinutes }
                val totalAmount = sessions.sumOf { it.session.totalAmount }
                val totalRemaining = sessions.sumOf { it.session.remainingDebt }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxCard(
                        title = "إجمالي الساعات",
                        value = Formatters.formatDurationShort(totalMinutes),
                        subtitle = "${sessions.size} دورة ري",
                        icon = Icons.Default.AccessTime,
                        accentColor = SecondaryAqua,
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxCard(
                        title = "إجمالي المبيعات",
                        value = Formatters.formatCurrency(totalAmount, config.currencySymbol),
                        subtitle = "متبقي: ${Formatters.formatNumber(totalRemaining)}",
                        icon = Icons.Default.WaterDrop,
                        accentColor = PrimaryTeal,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            // Sessions List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل دورات وتوزيع الماء (${sessions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
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
                        }
                    )
                }
            }
        }

        // Floating Action Buttons (Dual FABs)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Timer Button
            if (!liveTimerState.isRunning) {
                FloatingActionButton(
                    onClick = {
                        if (customers.isEmpty()) {
                            viewModel.showToast("يرجى إضافة عميل أولاً لبدء العداد", com.example.core.ui.ToastType.WARNING)
                        } else {
                            showStartLiveSheet = true
                        }
                    },
                    containerColor = AccentEmerald,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_start_live_timer")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("عداد مباشر", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Add Manual Session FAB
            ExtendedFloatingActionButton(
                onClick = {
                    if (customers.isEmpty()) {
                        viewModel.showToast("يرجى إضافة عميل أولاً", com.example.core.ui.ToastType.WARNING)
                    } else {
                        showAddManualSheet = true
                    }
                },
                containerColor = PrimaryTeal,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("تسجيل دورة ماء", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_session")
            )
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
    onWhatsAppClick: () -> Unit
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
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Customer Name & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCustomerClick() }
                        .weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = customer?.name ?: "عميل غير معروف",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (!customer?.farmName.isNullOrEmpty()) {
                            Text(
                                text = customer?.farmName ?: "",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("فاتورة PDF ومشاركة") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PrimaryTeal) },
                            onClick = {
                                menuExpanded = false
                                onPdfClick()
                            }
                        )
                        if (!customer?.phone.isNullOrEmpty()) {
                            DropdownMenuItem(
                                text = { Text("إرسال عبر واتساب") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = AccentEmerald) },
                                onClick = {
                                    menuExpanded = false
                                    onWhatsAppClick()
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

            Spacer(modifier = Modifier.height(12.dp))

            // Duration and Hourly Price Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = SecondaryAqua, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Formatters.formatDurationArabic(session.durationMinutes),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "@ ${Formatters.formatCurrency(session.pricePerHour, currencySymbol)}/ساعة",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amounts Row: Total, Paid, Debt
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "القيمة الإجمالية",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = Formatters.formatCurrency(session.totalAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                    )
                    Text(
                        text = Formatters.amountToArabicWords(session.totalAmount, currencySymbol),
                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Medium)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "المسدد فوراً",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = Formatters.formatCurrency(session.amountPaid, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = AccentEmerald)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "المتبقي كدين",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = Formatters.formatCurrency(session.remainingDebt, currencySymbol),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (session.remainingDebt > 0) Color(0xFFE53935) else AccentEmerald
                        )
                    )
                    if (session.remainingDebt > 0) {
                        Text(
                            text = Formatters.amountToArabicWords(session.remainingDebt, currencySymbol),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE53935), fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }

            // Footer info: Pump Name & Date
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${session.pumpName}  •  ${Formatters.formatDateTime(session.startTime)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                )

                if (session.notes.isNotEmpty()) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

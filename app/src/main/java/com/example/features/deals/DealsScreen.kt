package com.example.features.deals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.Formatters
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.StatusDebt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealsScreen(
    viewModel: DealsViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val deals by viewModel.filteredDeals.collectAsStateWithLifecycle()
    val filterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDealForPayment by remember { mutableStateOf<SettlementDeal?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "سجل عقود الصلح والأقساط",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AccentGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${deals.size} عقد",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("ابحث برقم العقد، البائع، المشتري أو المحصول...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                // Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "الكل",
                        "ACTIVE" to "جاري السداد ⏳",
                        "COMPLETED" to "مكتملة وموفية ✅"
                    )
                    tabs.forEach { (statusKey, label) ->
                        val isSelected = filterStatus == statusKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentGold else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.setFilterStatus(statusKey) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }

                // New Deal Action Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { showAddDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AccentGold.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentGold),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "إبرام وتوثيق عقد صلح جديد 📜🤝",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentGold)
                                )
                                Text(
                                    text = "توثيق البيع والأقساط وسعاية الدلال وتوليد سند واتساب",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }

                // Deals List
                if (deals.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || filterStatus != "ALL") "لا توجد عقود تطابق هذا البحث"
                                else "لا توجد عقود صلح مسجلة حتى الآن",
                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "اضغط على زر الإبرام لتوثيق اتفاق بيع ثمرة زراعية بأقساط واضحة",
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(deals, key = { it.id }) { deal ->
                            DealCard(
                                deal = deal,
                                onShare = { viewModel.shareContract(context, deal) },
                                onRecordPayment = { selectedDealForPayment = deal },
                                onDelete = { viewModel.deleteDeal(deal.id) }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp),
                    color = AccentGold
                )
            }

            LuxuryToastNotification(
                toast = toast,
                onDismiss = { viewModel.dismissToast() }
            )
        }
    }

    if (showAddDialog) {
        AddDealDialog(
            onDismiss = { showAddDialog = false },
            onSubmit = { title, crop, loc, sName, sPhone, bName, bPhone, dName, dPhone, total, adv, comm, notes ->
                viewModel.createDeal(
                    cropTitle = title,
                    cropType = crop,
                    location = loc,
                    sellerName = sName,
                    sellerPhone = sPhone,
                    buyerName = bName,
                    buyerPhone = bPhone,
                    dallalName = dName,
                    dallalPhone = dPhone,
                    totalAmount = total,
                    advancePayment = adv,
                    dallalCommission = comm,
                    dueDate = null,
                    termsNotes = notes,
                    onComplete = { newDeal ->
                        showAddDialog = false
                        if (newDeal != null) {
                            // Automatically prompt share
                            viewModel.shareContract(context, newDeal)
                        }
                    }
                )
            }
        )
    }

    if (selectedDealForPayment != null) {
        RecordPaymentDialog(
            deal = selectedDealForPayment!!,
            onDismiss = { selectedDealForPayment = null },
            onSubmit = { amount, paidBy, notes ->
                viewModel.recordPayment(selectedDealForPayment!!, amount, paidBy, notes) {
                    selectedDealForPayment = null
                }
            }
        )
    }
}

@Composable
fun DealCard(
    deal: SettlementDeal,
    onShare: () -> Unit,
    onRecordPayment: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // Header: Deal Number + Status Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGold.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = deal.dealNumber,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentGold)
                        )
                    }

                    val (statusText, statusBg, statusFg) = if (deal.isCompleted) {
                        Triple("مكتمل وموفي ✅", AccentEmerald.copy(alpha = 0.15f), AccentEmerald)
                    } else {
                        Triple("جاري السداد ⏳", AccentAmber.copy(alpha = 0.15f), AccentAmber)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = statusFg)
                        )
                    }
                }

                Text(
                    text = Formatters.formatDate(deal.dealDate),
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Crop Title & Location
            Text(
                text = "${deal.cropTitle} (${deal.cropType})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (deal.location.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = deal.location,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Parties Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🌾 البائع: ${deal.sellerName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (deal.sellerPhone.isNotBlank()) {
                            Text(
                                text = deal.sellerPhone,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📦 المشتري: ${deal.buyerName}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                        )
                        if (deal.buyerPhone.isNotBlank()) {
                            Text(
                                text = deal.buyerPhone,
                                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    if (deal.dallalName.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🤝 الدلال: ${deal.dallalName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = AccentGold, fontWeight = FontWeight.Medium)
                            )
                            if (deal.dallalCommission > 0.0) {
                                Text(
                                    text = "السعاية: ${deal.formatCurrency(deal.dallalCommission)}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = AccentGold)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Financial Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "القيمة الإجمالية", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(text = deal.formatCurrency(deal.totalAmount), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                }

                Column {
                    Text(text = "العربون المقبوض", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(text = deal.formatCurrency(deal.advancePayment), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = AccentEmerald))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "المتبقي للوفاء", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                    Text(
                        text = deal.formatCurrency(deal.remainingAmount),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (deal.isCompleted) AccentEmerald else StatusDebt
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share WhatsApp Contract
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "مشاركة السند (واتساب)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                }

                // Record Installment Payment
                if (!deal.isCompleted) {
                    Button(
                        onClick = onRecordPayment,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "تسجيل دفعة", style = MaterialTheme.typography.labelMedium)
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

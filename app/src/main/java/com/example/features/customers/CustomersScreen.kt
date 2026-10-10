package com.example.features.customers

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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.ui.StatBoxCard
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val rawCustomers by viewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    val customersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val accountFilter by viewModel.accountFilter.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSheet by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var messageChoiceCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }

    // حساب إجمالي المركز المالي للشريط السفلي (دفتر الحسابات)
    val totalOwedByFarmers = remember(rawCustomers) {
        rawCustomers.filter { !it.customer.isWellOwner && it.balance > 0 }.sumOf { it.balance }
    }
    val totalOwedToWellOwners = remember(rawCustomers) {
        rawCustomers.filter { it.customer.isWellOwner && it.balance > 0 }.sumOf { it.balance }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // 1. شريط تبويبات التصنيف الرئيسي (الكل / المزارعون / أصحاب الآبار) + البحث والفرز
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    // تبويبات الحسابات
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = accountFilter == AccountFilter.ALL,
                                onClick = { viewModel.setAccountFilter(AccountFilter.ALL) },
                                label = { Text("الكل (${rawCustomers.size})", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = accountFilter == AccountFilter.FARMERS,
                                onClick = { viewModel.setAccountFilter(AccountFilter.FARMERS) },
                                label = {
                                    val count = rawCustomers.count { !it.customer.isWellOwner }
                                    Text("👨‍🌾 المزارعون ($count)", fontWeight = FontWeight.Bold)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = accountFilter == AccountFilter.WELL_OWNERS,
                                onClick = { viewModel.setAccountFilter(AccountFilter.WELL_OWNERS) },
                                label = {
                                    val count = rawCustomers.count { it.customer.isWellOwner }
                                    Text("💧 أصحاب الآبار ($count)", fontWeight = FontWeight.Bold)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // حقل البحث
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("بحث بالاسم، المزرعة، أو الهاتف...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customers_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // خيارات الفرز
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = sortType == CustomerSort.NAME,
                                onClick = { viewModel.setSortType(CustomerSort.NAME) },
                                label = { Text("أبجدياً (الاسم)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryTeal
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortType == CustomerSort.HIGHEST_DEBT,
                                onClick = { viewModel.setSortType(CustomerSort.HIGHEST_DEBT) },
                                label = { Text("الأعلى رصيداً") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryTeal
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortType == CustomerSort.MOST_WATER_HOURS,
                                onClick = { viewModel.setSortType(CustomerSort.MOST_WATER_HOURS) },
                                label = { Text("الأكثر استهلاكاً / ضخاً") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal.copy(alpha = 0.15f),
                                    selectedLabelColor = PrimaryTeal
                                )
                            )
                        }
                    }
                }
            }

            // 2. قائمة الحسابات المضغوطة (دفتر الحسابات)
            if (customersWithBalance.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = if (accountFilter == AccountFilter.WELL_OWNERS) "لا يوجد أصحاب آبار مسجلين" else "لا يوجد عملاء مضافين",
                        description = "اضغط على زر (+) لإضافة حساب جديد"
                    )
                }
            } else {
                items(customersWithBalance, key = { it.customer.id }) { item ->
                    CustomerCardItem(
                        item = item,
                        currencySymbol = config.currencySymbol,
                        onClick = { onNavigateToDetail(item.customer.id) },
                        onCallClick = { FileSharingHelper.makePhoneCall(context, item.customer.phone) },
                        onWhatsAppClick = { messageChoiceCustomer = item },
                        onPdfStatementClick = { viewModel.generateCustomerStatementPdf(item.customer) },
                        onEditClick = { customerToEdit = item.customer },
                        onDeleteClick = { customerToDelete = item.customer }
                    )
                }
            }
        }

        // 3. الشريط المالي السفلي الثابت (دفتر الحسابات): عليك | (+) | لك
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // مستحقات أصحاب الآبار (عليك)
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "عليك (لأصحاب الآبار)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFE53935),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(totalOwedToWellOwners, config.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    )
                }

                // زر الإضافة السريع (+) الدائري في المنتصف
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = PrimaryTeal,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fab_add_customer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة حساب جديد",
                        modifier = Modifier.size(26.dp)
                    )
                }

                // ديون المزارعين (لك)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "لك (على المزارعين)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(totalOwedByFarmers, config.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp)
        )
    }

    // PDF Open / Share Dialog
    pdfReady?.let { (file, title) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearPdfReady() },
            title = { Text("كشف الحساب جاهز", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في فتح وعرض كشف الحساب مباشرة أم مشاركته؟") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        com.example.core.util.FileSharingHelper.openPdf(context, file)
                        viewModel.clearPdfReady()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) { Text("فتح / عرض") }
            },
            dismissButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        com.example.core.util.FileSharingHelper.sharePdf(context, file, title)
                        viewModel.clearPdfReady()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) { Text("مشاركة") }
            }
        )
    }

    // SMS / WhatsApp Choice Dialog
    messageChoiceCustomer?.let { item ->
        val msg = viewModel.buildCustomerStatementMessage(item.customer, item)
        SendMessageChoiceDialog(
            recipientName = item.customer.name,
            recipientPhone = item.customer.phone,
            messageText = msg,
            onDismiss = { messageChoiceCustomer = null },
            onSendWhatsApp = {
                messageChoiceCustomer = null
                FileSharingHelper.sendWhatsAppMessage(context, item.customer.phone, msg)
            },
            onSendSms = {
                messageChoiceCustomer = null
                FileSharingHelper.sendSms(context, item.customer.phone, msg)
            }
        )
    }

    // Add / Edit Customer Sheet
    if (showAddSheet || customerToEdit != null) {
        AddEditCustomerBottomSheet(
            initialCustomer = customerToEdit,
            currencySymbol = config.currencySymbol,
            onDismiss = {
                showAddSheet = false
                customerToEdit = null
            },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner)
            }
        )
    }

    // Delete confirmation
    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("تأكيد حذف العميل", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من حذف العميل (${cust.name})؟ سيتم حذف جميع جلسات الري والسندات التابعة له.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun CustomerCardItem(
    item: CustomerWithBalance,
    currencySymbol: String,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPdfStatementClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val customer = item.customer
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 1. الصف الأول: بيانات العميل (الاسم والمزرعة/الهاتف) + أزرار الإجراءات السريعة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العميل والمزرعة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (customer.isWellOwner) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1F5FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "صاحب بئر",
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customer.name.take(1),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (customer.isWellOwner) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE1F5FE))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "صاحب بئر",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF0288D1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // شارة عدد العمليات (دفتر الحسابات)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE3F2FD))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${item.totalSessionsCount} حركة",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF1976D2),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            val subInfo = buildList {
                                if (customer.farmName.isNotBlank()) add(customer.farmName)
                                if (customer.phone.isNotBlank()) add(customer.phone)
                            }.joinToString(" • ")

                            if (subInfo.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = subInfo,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // الأزرار الأربعة السريعة (اتصال، واتساب، كشف حساب PDF، خيارات)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (customer.phone.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryTeal.copy(alpha = 0.12f))
                                .clickable { onCallClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "اتصال",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                                .clickable { onWhatsAppClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال كشف",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGold.copy(alpha = 0.14f))
                            .clickable { onPdfStatementClick() },
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "كشف حساب PDF",
                            tint = AccentGold,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { menuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("عرض كشف الحساب") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تصدير كشف حساب PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentGold) },
                                onClick = {
                                    menuExpanded = false
                                    onPdfStatementClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (customer.isWellOwner) "تعديل حساب البئر" else "تعديل العميل") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف الحساب", color = Color(0xFFE53935)) },
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

            Spacer(modifier = Modifier.height(8.dp))

            // 2. الصف الثاني المختصر: إجمالي الساعات + إجمالي المتبقي / الرصيد مع الأسهم الاتجاهية
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // إجمالي الساعات
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (customer.isWellOwner) "الضخ: ${Formatters.formatDurationShort(item.totalMinutes)}" else "الساعات: ${Formatters.formatDurationShort(item.totalMinutes)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    )
                }

                // إجمالي الرصيد الاتجاهي
                if (customer.isWellOwner) {
                    // حساب صاحب البئر: موجب = عليك (أحمر + سهم لأسفل) | سالب = لك (أخضر + سهم لأعلى)
                    when {
                        item.balance > 0 -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE53935).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "عليك: ${Formatters.formatNumber(item.balance)} $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD32F2F),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        item.balance < 0 -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentEmerald.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "لك: ${Formatters.formatNumber(Math.abs(item.balance))} $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AccentEmerald,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentEmerald.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("خالص ومسدد", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentEmerald, fontSize = 12.sp))
                            }
                        }
                    }
                } else {
                    // حساب المزارع: موجب = لك (أخضر + سهم لأعلى) | سالب = عليك (أحمر + سهم لأسفل)
                    when {
                        item.balance > 0 -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "لك: ${Formatters.formatNumber(item.balance)} $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        item.balance < 0 -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE53935).copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "عليك: ${Formatters.formatNumber(Math.abs(item.balance))} $currencySymbol",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD32F2F),
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                        else -> {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentEmerald.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("خالص ومسدد", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = AccentEmerald, fontSize = 12.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

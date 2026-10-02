package com.baynana.features.customers

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
import com.baynana.core.ui.EmptyStateView
import com.baynana.core.ui.LuxuryToastNotification
import com.baynana.core.ui.SendMessageChoiceDialog
import com.baynana.core.ui.StatBoxCard
import com.baynana.core.util.FileSharingHelper
import com.baynana.core.util.Formatters
import com.baynana.ui.theme.AccentEmerald
import com.baynana.ui.theme.AccentGold
import com.baynana.ui.theme.PrimaryTeal
import com.baynana.ui.theme.SecondaryAqua

@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val customersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSheet by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var messageChoiceCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {

            // Summary Quick Stat Cards
            item {
                val totalDebts = customersWithBalance.filter { it.balance > 0 }.sumOf { it.balance }
                val totalWaterDistributedMinutes = customersWithBalance.sumOf { it.totalMinutes }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxCard(
                        title = "ديون العملاء",
                        value = Formatters.formatCurrency(totalDebts, config.currencySymbol),
                        subtitle = "${customersWithBalance.count { it.balance > 0 }} عميل مدين",
                        icon = Icons.Default.WaterDrop,
                        accentColor = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxCard(
                        title = "ساعات التوزيع",
                        value = Formatters.formatDurationShort(totalWaterDistributedMinutes),
                        subtitle = "${customersWithBalance.size} عميل مسجل",
                        icon = Icons.Default.AccessTime,
                        accentColor = AccentGold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Bar & Sort Chips
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("بحث...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customers_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = sortType == CustomerSort.NAME,
                                onClick = { viewModel.setSortType(CustomerSort.NAME) },
                                label = { Text("أبجدياً (الاسم)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortType == CustomerSort.HIGHEST_DEBT,
                                onClick = { viewModel.setSortType(CustomerSort.HIGHEST_DEBT) },
                                label = { Text("الأعلى ديوناً") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = sortType == CustomerSort.MOST_WATER_HOURS,
                                onClick = { viewModel.setSortType(CustomerSort.MOST_WATER_HOURS) },
                                label = { Text("الأكثر استهلاكاً للماء") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Customer List Items
            if (customersWithBalance.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = "لا يوجد عملاء مضافين",
                        description = "اضغط على زر (إضافة عميل) لتسجيل أول عميل أو مزارع"
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

        // Add Customer FAB
        ExtendedFloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .testTag("fab_add_customer"),
            containerColor = PrimaryTeal,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("إضافة عميل جديد", fontWeight = FontWeight.Bold) }
        )

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
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
                        com.baynana.core.util.FileSharingHelper.openPdf(context, file)
                        viewModel.clearPdfReady()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) { Text("فتح / عرض") }
            },
            dismissButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        com.baynana.core.util.FileSharingHelper.sharePdf(context, file, title)
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
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary)
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
                // العميل والمزرعة (نمنحه كامل المساحة لمنع اقتصاص الاسم)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
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

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subInfo = buildList {
                            if (customer.farmName.isNotBlank()) add(customer.farmName)
                            if (customer.phone.isNotBlank()) add(customer.phone)
                        }.joinToString(" • ")

                        if (subInfo.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subInfo,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                        // اتصال هاتف
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

                        // إرسال كشف الحساب (واتساب أو رسالة نصية)
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
                                contentDescription = "إرسال كشف (واتساب أو رسالة)",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // كشف حساب PDF
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

                    // خيارات إضافية (⋮)
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
                                text = { Text("تعديل العميل") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("حذف العميل", color = Color(0xFFE53935)) },
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

            // 2. الصف الثاني المختصر: إجمالي الساعات + إجمالي المتبقي / الرصيد
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
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "الساعات: ${Formatters.formatDurationShort(item.totalMinutes)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp
                        )
                    )
                }

                // إجمالي المتبقي / حساب الدين / مسدد / له
                when {
                    item.balance > 0 -> {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE53935).copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المتبقي: ${Formatters.formatNumber(item.balance)} $currencySymbol",
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
                            Text(
                                text = "له: ${Formatters.formatNumber(Math.abs(item.balance))} $currencySymbol",
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
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "مسدد بالكامل",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.example.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
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
    val customersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSheet by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

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
                        onWhatsAppClick = { viewModel.sendCustomerStatementWhatsApp(item.customer, item) },
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
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
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

    // Add / Edit Customer Sheet
    if (showAddSheet || customerToEdit != null) {
        AddEditCustomerBottomSheet(
            initialCustomer = customerToEdit,
            currencySymbol = config.currencySymbol,
            onDismiss = {
                showAddSheet = false
                customerToEdit = null
            },
            onSave = { id, name, phone, farm, loc, notes, customPrice ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice)
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
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // 1. الصف العلوي: بيانات العميل والأزرار السريعة بنمط سكويركل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العميل والمزرعة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.take(1),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            ),
                            maxLines = 1
                        )
                        if (customer.farmName.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Yard,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = customer.farmName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // الأزرار الأربعة السريعة (اتصال، واتساب، كشف حساب PDF، خيارات)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (customer.phone.isNotEmpty()) {
                        // اتصال هاتف
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0F2F1))
                                .clickable { onCallClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "اتصال",
                                tint = Color(0xFF00695C),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // واتساب
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .clickable { onWhatsAppClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "واتساب",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    // كشف حساب PDF
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .clickable { onPdfStatementClick() },
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "كشف حساب PDF",
                            tint = Color(0xFFC67C00),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    // خيارات إضافية (⋮)
                    Box {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F5F9))
                                .clickable { menuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(18.dp)
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

            // 2. الصف الأوسط: كبسولة لساعات السقي والهاتف
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ساعات السقي مع أيقونة الساعة أولاً في اليمين (RTL)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${Formatters.formatDurationShort(item.totalMinutes)} سقي مسجل",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    )
                }

                // رقم الهاتف أو ملاحظة على اليسار
                Text(
                    text = if (customer.phone.isNotEmpty()) customer.phone else "بدون هاتف",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. الجزء السفلي: مقسم لعمودين مع فاصل رأسي
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العمود الأيمن (في RTL): كبسولات المسارب والمسدد والرصيد
                Column(
                    modifier = Modifier.weight(1.05f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "الحسابات",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        ),
                        modifier = Modifier.padding(bottom = 1.dp)
                    )

                    // كبسولة إجمالي المسارب
                    CustomerMetricPill(
                        label = "المسارب",
                        value = "${Formatters.formatNumber(item.totalBilledAmount)} $currencySymbol",
                        bgColor = Color(0xFFE8F5E9),
                        textColor = Color(0xFF00695C)
                    )

                    // كبسولة المسدد
                    CustomerMetricPill(
                        label = "المسدد",
                        value = "${Formatters.formatNumber(item.totalPaidAmount)} $currencySymbol",
                        bgColor = Color(0xFFE8F5E9),
                        textColor = Color(0xFF00695C)
                    )

                    // كبسولة الرصيد
                    val (pillBg, pillText) = when {
                        item.balance > 0 -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                        item.balance < 0 -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                        else -> Color(0xFFF1F5F9) to Color(0xFF475569)
                    }
                    CustomerMetricPill(
                        label = "الرصيد",
                        value = if (item.balance == 0.0) "0 $currencySymbol (خالص)" else "${Formatters.formatNumber(Math.abs(item.balance))} $currencySymbol",
                        bgColor = pillBg,
                        textColor = pillText
                    )
                }

                // فاصل رأسي رفيع
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                )

                // العمود الأيسر (في RTL): حالة الحساب والمبلغ البارز وكتابته بالعربي
                Column(
                    modifier = Modifier.weight(0.95f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "حالة الحساب",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    )

                    val (statusLabel, statusColor) = when {
                        item.balance > 0 -> "مدين" to Color(0xFFB71C1C)
                        item.balance < 0 -> "له رصيد" to Color(0xFF00695C)
                        else -> "خالص" to Color(0xFF2E7D32)
                    }

                    Text(
                        text = if (item.balance == 0.0) "خالص ومسدد" else "$statusLabel: ${Formatters.formatNumber(Math.abs(item.balance))} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (item.balance != 0.0) {
                            Formatters.amountToArabicWords(Math.abs(item.balance), currencySymbol)
                        } else {
                            "لا توجد أي مبالغ مستحقة"
                        },
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
        }
    }
}

@Composable
private fun CustomerMetricPill(
    label: String,
    value: String,
    bgColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp),
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

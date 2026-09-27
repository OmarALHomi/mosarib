package com.example.features.vouchers

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoneyOff
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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

@Composable
fun VouchersScreen(
    viewModel: VouchersViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val vouchers by viewModel.filteredVouchers.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val typeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var voucherToDelete by remember { mutableStateOf<Voucher?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {

            // Financial Balance Metric Cards
            item {
                val totalReceipts = vouchers.filter { it.voucher.type == VoucherType.RECEIPT }.sumOf { it.voucher.amount }
                val totalExpenses = vouchers.filter { it.voucher.type == VoucherType.EXPENSE }.sumOf { it.voucher.amount }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBoxCard(
                        title = "المقبوضات",
                        value = Formatters.formatCurrency(totalReceipts, config.currencySymbol),
                        icon = Icons.Default.ArrowDownward,
                        accentColor = AccentEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StatBoxCard(
                        title = "المصاريف",
                        value = Formatters.formatCurrency(totalExpenses, config.currencySymbol),
                        icon = Icons.Default.ArrowUpward,
                        accentColor = Color(0xFFFF5252),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search & Filters
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("بحث...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vouchers_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = typeFilter == null,
                                onClick = { viewModel.setTypeFilter(null) },
                                label = { Text("الكل") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = typeFilter == VoucherType.RECEIPT,
                                onClick = { viewModel.setTypeFilter(VoucherType.RECEIPT) },
                                label = { Text("مقبوضات") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentEmerald,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = typeFilter == VoucherType.EXPENSE,
                                onClick = { viewModel.setTypeFilter(VoucherType.EXPENSE) },
                                label = { Text("مصاريف") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE53935),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // List of Vouchers
            if (vouchers.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Receipt,
                        title = "لا توجد سندات مسجلة",
                        description = "اضغط على زر (إضافة سند) لتسجيل سند قبض أو مصروف تشغيلي"
                    )
                }
            } else {
                items(vouchers, key = { it.voucher.id }) { item ->
                    val voucher = item.voucher
                    val isReceipt = voucher.type == VoucherType.RECEIPT
                    val badgeColor = if (isReceipt) AccentEmerald else Color(0xFFE53935)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            // 1. الصف العلوي: النوع ورقم السند والعميل وزر الحذف بسكويركل
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
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isReceipt) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = badgeColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isReceipt) "سند قبض" else "سند صرف",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "(${voucher.voucherNumber})",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                                            )
                                        }

                                        Text(
                                            text = if (item.customer != null) "العميل: ${item.customer.name}" else "مصروفات عامة",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (item.customer != null) PrimaryTeal else Color(0xFF64748B),
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                }

                                // زر الحذف بسكويركل ناعم
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFEBEE))
                                        .clickable { voucherToDelete = voucher },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. كبسولة وسطى لطريقة الدفع والتاريخ
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = voucher.paymentMethod,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }

                                Text(
                                    text = Formatters.formatDateTime(voucher.date),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF64748B)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. الجزء السفلي: المبلغ البارز والتفقيط
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isReceipt) "المبلغ المقبوض" else "المبلغ المصروف",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF475569),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(voucher.amount, config.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = badgeColor,
                                            fontSize = 18.sp
                                        )
                                    )
                                    Text(
                                        text = Formatters.amountToArabicWords(voucher.amount, config.currencySymbol),
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

                            if (voucher.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "ملاحظة: ${voucher.notes}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to Add Voucher
        ExtendedFloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .testTag("fab_add_voucher"),
            containerColor = PrimaryTeal,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("سند جديد", fontWeight = FontWeight.Bold) }
        )

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
        )
    }

    if (showAddSheet) {
        AddVoucherBottomSheet(
            customers = customers,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddSheet = false },
            onSave = { type, custId, amt, cat, method, notes ->
                viewModel.addVoucher(type, custId, amt, cat, method, notes)
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
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { voucherToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVoucherBottomSheet(
    customers: List<Customer>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        type: VoucherType,
        customerId: Long?,
        amount: Double,
        category: String,
        paymentMethod: String,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(VoucherType.RECEIPT) }
    var selectedCustomerId by remember { mutableLongStateOf(customers.firstOrNull()?.id ?: 0L) }
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    var custDropdownExpanded by remember { mutableStateOf(false) }
    val selectedCustomer = customers.find { it.id == selectedCustomerId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                Text("العميل المستلم منه:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
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
                                    custDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ ($currencySymbol) *") },
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
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("طريقة الدفع") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(if (type == VoucherType.RECEIPT) "البيان والملاحظات" else "بيان المصروف (اختياري)") },
                placeholder = { Text(if (type == VoucherType.RECEIPT) "مثال: دفعة ري أو سداد حساب" else "بيان المصروف...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    if (amt > 0) {
                        val custId = if (type == VoucherType.RECEIPT) selectedCustomerId else null
                        val cat = if (type == VoucherType.RECEIPT) "سداد حساب" else "مصاريف"
                        onSave(type, custId, amt, cat, paymentMethod, notes)
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

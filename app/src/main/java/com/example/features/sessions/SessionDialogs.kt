package com.example.features.sessions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.features.customers.AddEditCustomerBottomSheet
import com.example.features.customers.Customer
import com.example.features.pumps.PumpSource
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSessionBottomSheet(
    initialSession: WaterSession? = null,
    customers: List<Customer>,
    pumps: List<PumpSource>,
    defaultPricePerHour: Double,
    currencySymbol: String,
    onCreateCustomer: suspend (Customer) -> Long,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        customerId: Long,
        pumpName: String,
        startTime: Long,
        endTime: Long,
        hours: Int,
        minutes: Int,
        pricePerHour: Double,
        amountPaid: Double,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val coroutineScope = rememberCoroutineScope()

    var selectedCustomerId by remember {
        mutableLongStateOf(initialSession?.customerId ?: customers.firstOrNull()?.id ?: 0L)
    }
    var selectedPumpName by remember {
        mutableStateOf(initialSession?.pumpName ?: pumps.firstOrNull()?.name ?: "البئر")
    }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val initialPrice = initialSession?.pricePerHour
        ?: selectedCustomer?.customPricePerHour
        ?: defaultPricePerHour

    // remember(initialPrice) يضمن تحديث السعر تلقائياً عند تغييره في الإعدادات
    var pricePerHourStr by remember(initialPrice) { mutableStateOf(Formatters.formatAmountInput(initialPrice.toString())) }
    val pricePerHour by remember {
        derivedStateOf { Formatters.parseAmountInput(pricePerHourStr).takeIf { it > 0 } ?: defaultPricePerHour }
    }

    var hours by remember { mutableIntStateOf(initialSession?.let { it.durationMinutes / 60 } ?: 1) }
    var minutes by remember { mutableIntStateOf(initialSession?.let { it.durationMinutes % 60 } ?: 0) }
    var amountPaidStr by remember { mutableStateOf(initialSession?.amountPaid?.let { if (it > 0) it.toString() else "" } ?: "") }
    var notes by remember { mutableStateOf(initialSession?.notes ?: "") }

    val totalMinutes by remember { derivedStateOf { (hours * 60) + minutes } }
    val calculatedCost by remember { derivedStateOf { Formatters.calculateWaterCost(totalMinutes, pricePerHour) } }
    val amountPaid by remember { derivedStateOf { Formatters.parseAmountInput(amountPaidStr) } }
    val remainingDebt by remember { derivedStateOf { Math.max(0.0, calculatedCost - amountPaid) } }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var pumpDropdownExpanded by remember { mutableStateOf(false) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var customerSaveError by remember { mutableStateOf<String?>(null) }
    var showNotes by remember { mutableStateOf(!notes.isNullOrBlank()) }

    BackHandler(onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialSession == null) "تسجيل دورة ري (ساقية ماء)" else "تعديل بيانات دورة الماء",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Customer Selector
            Text(
                text = "العميل المستفيد *",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_customer_picker"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { customerDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        customers.forEach { c ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(c.name, fontWeight = FontWeight.Bold)
                                        if (c.farmName.isNotEmpty()) {
                                            Text(c.farmName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCustomerId = c.id
                                    val customerPrice = c.customPricePerHour ?: defaultPricePerHour
                                    pricePerHourStr = Formatters.formatAmountInput(customerPrice.toString())
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                IconButton(
                    onClick = {
                        customerSaveError = null
                        showAddCustomerSheet = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("session_add_customer")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "إضافة عميل", tint = PrimaryTeal)
                }
            }
            customerSaveError?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Duration: Hours and Minutes
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مدة الضخ والري:", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = Formatters.formatDurationArabic(totalMinutes),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Hours Stepper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الساعات: $hours س", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (hours > 0) hours-- },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("$hours", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedButton(
                                onClick = { if (hours < 72) hours++ },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Minutes Stepper (0, 15, 30, 45, etc.)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الدقائق: $minutes د", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { if (minutes >= 5) minutes -= 5 else if (hours > 0) { hours--; minutes = 55 } },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("-5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$minutes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { if (minutes <= 50) minutes += 5 else { hours++; minutes = 0 } },
                                shape = CircleShape,
                                modifier = Modifier.size(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                            ) {
                                Text("+5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hourly Rate (Snapshot)
            OutlinedTextField(
                value = pricePerHourStr,
                onValueChange = { pricePerHourStr = Formatters.formatAmountInput(it) },
                label = { Text("سعر ساعة الماء الحالية ($currencySymbol/ساعة)") },
                leadingIcon = {
                    Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("session_price_input"),
                shape = RoundedCornerShape(12.dp)
            )

            val parsedPrice = Formatters.parseAmountInput(pricePerHourStr)
            if (parsedPrice > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedPrice, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي قيمة الماء:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = Formatters.formatCurrency(calculatedCost, currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryTeal
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المدفوع نقداً مقدماً:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = Formatters.formatCurrency(amountPaid, currencySymbol),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentEmerald
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المتبقي كدين بذمة العميل:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = Formatters.formatCurrency(remainingDebt, currencySymbol),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (remainingDebt > 0) Color(0xFFE53935) else AccentEmerald
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Paid upfront
            OutlinedTextField(
                value = amountPaidStr,
                onValueChange = { amountPaidStr = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ المدفوع فوراً (اختياري)") },
                placeholder = { Text("0") },
                leadingIcon = {
                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("session_paid_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (amountPaid > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(amountPaid, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentEmerald,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            TextButton(onClick = { showNotes = !showNotes }) {
                Text(if (showNotes) "إخفاء الملاحظات" else "إضافة ملاحظة (اختياري)")
            }
            if (showNotes) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (selectedCustomerId > 0 && totalMinutes > 0) {
                        onSave(
                            initialSession?.id ?: 0L,
                            selectedCustomerId,
                            selectedPumpName,
                            initialSession?.startTime ?: System.currentTimeMillis(),
                            initialSession?.endTime ?: System.currentTimeMillis(),
                            hours,
                            minutes,
                            pricePerHour,
                            amountPaid,
                            notes
                        )
                        onDismiss()
                    }
                },
                enabled = selectedCustomerId > 0 && totalMinutes > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_session_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("حفظ وترحيل دورة الماء", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showAddCustomerSheet) {
        AddEditCustomerBottomSheet(
            initialCustomer = null,
            currencySymbol = currencySymbol,
            onDismiss = { showAddCustomerSheet = false },
            onSave = { _, name, phone, farmName, location, customerNotes, customPrice, isBeneficiary ->
                coroutineScope.launch {
                    runCatching {
                        onCreateCustomer(
                            Customer(
                                name = name.trim(),
                                phone = phone.trim(),
                                farmName = farmName.trim(),
                                location = location.trim(),
                                notes = customerNotes.trim(),
                                customPricePerHour = customPrice,
                                isBeneficiary = isBeneficiary
                            )
                        )
                    }.onSuccess { customerId ->
                        selectedCustomerId = customerId
                        pricePerHourStr = Formatters.formatAmountInput(
                            (customPrice ?: defaultPricePerHour).toString()
                        )
                        customerSaveError = null
                    }.onFailure { error ->
                        customerSaveError = error.localizedMessage ?: "تعذرت إضافة العميل"
                    }
                }
            }
        )
    }
}

@Composable
fun SettleSessionDialog(
    session: WaterSession,
    customerName: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmSettle: (amount: Double, paymentMethod: String, notes: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(Formatters.formatAmountInput(session.remainingDebt.toString())) }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("سداد دورة سقي #${session.id}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = AccentEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("سداد المبلغ المؤخر", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // بطاقة ملخص الجلسة
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("العميل: $customerName", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("إجمالي الجلسة: ${Formatters.formatCurrency(session.totalAmount, currencySymbol)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("المسدد سابقاً: ${Formatters.formatCurrency(session.amountPaid, currencySymbol)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AccentEmerald)
                        Text(
                            "المبلغ المؤخر المتبقي: ${Formatters.formatCurrency(session.remainingDebt, currencySymbol)}",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE53935)
                        )
                    }
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                    label = { Text("المبلغ المراد سداده ($currencySymbol) *", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                val parsed = Formatters.parseAmountInput(amountStr)
                if (parsed > 0) {
                    Text(
                        Formatters.amountToArabicWords(parsed, currencySymbol),
                        style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                    )
                }

                // طرق الدفع
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("نقداً", "تحويل بنكي", "شبكة").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("البيان والملاحظات", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    if (amt > 0) {
                        onConfirmSettle(amt, paymentMethod, notes)
                        onDismiss()
                    }
                },
                enabled = Formatters.parseAmountInput(amountStr) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("تأكيد السداد وإصدار السند", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", fontWeight = FontWeight.Bold)
            }
        }
    )
}


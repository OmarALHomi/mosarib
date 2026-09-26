package com.example.features.sessions

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.features.customers.Customer
import com.example.features.pumps.PumpSource
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSessionBottomSheet(
    initialSession: WaterSession? = null,
    customers: List<Customer>,
    pumps: List<PumpSource>,
    defaultPricePerHour: Double,
    currencySymbol: String,
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCustomerId by remember {
        mutableLongStateOf(initialSession?.customerId ?: customers.firstOrNull()?.id ?: 0L)
    }
    var selectedPumpName by remember {
        mutableStateOf(initialSession?.pumpName ?: pumps.firstOrNull()?.name ?: "المضخة الرئيسية")
    }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val initialPrice = initialSession?.pricePerHour
        ?: selectedCustomer?.customPricePerHour
        ?: pumps.find { it.name == selectedPumpName }?.defaultPricePerHour
        ?: defaultPricePerHour

    var pricePerHourStr by remember { mutableStateOf(initialPrice.toString()) }
    val pricePerHour by remember {
        derivedStateOf { pricePerHourStr.toDoubleOrNull() ?: defaultPricePerHour }
    }

    var hours by remember { mutableIntStateOf(initialSession?.let { it.durationMinutes / 60 } ?: 1) }
    var minutes by remember { mutableIntStateOf(initialSession?.let { it.durationMinutes % 60 } ?: 0) }
    var amountPaidStr by remember { mutableStateOf(initialSession?.amountPaid?.let { if (it > 0) it.toString() else "" } ?: "") }
    var notes by remember { mutableStateOf(initialSession?.notes ?: "") }

    val totalMinutes by remember { derivedStateOf { (hours * 60) + minutes } }
    val calculatedCost by remember { derivedStateOf { Formatters.calculateWaterCost(totalMinutes, pricePerHour) } }
    val amountPaid by remember { derivedStateOf { amountPaidStr.toDoubleOrNull() ?: 0.0 } }
    val remainingDebt by remember { derivedStateOf { Math.max(0.0, calculatedCost - amountPaid) } }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var pumpDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
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
            Box {
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
                                if (c.customPricePerHour != null) {
                                    pricePerHourStr = c.customPricePerHour.toString()
                                }
                                customerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pump Selector
            Text(
                text = "مضخة / بئر الماء",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box {
                OutlinedTextField(
                    value = selectedPumpName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, tint = SecondaryAqua)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { pumpDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = pumpDropdownExpanded,
                    onDismissRequest = { pumpDropdownExpanded = false }
                ) {
                    pumps.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.name} (${p.powerType})") },
                            onClick = {
                                selectedPumpName = p.name
                                pricePerHourStr = p.defaultPricePerHour.toString()
                                pumpDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                onValueChange = { pricePerHourStr = it },
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
                onValueChange = { amountPaidStr = it },
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

            Spacer(modifier = Modifier.height(14.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات (مثل: قطاع الأرض، أشجار معينة...)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartLiveTimerBottomSheet(
    customers: List<Customer>,
    pumps: List<PumpSource>,
    defaultPricePerHour: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onStart: (customerId: Long, customerName: String, pumpName: String, pricePerHour: Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCustomerId by remember {
        mutableLongStateOf(customers.firstOrNull()?.id ?: 0L)
    }
    var selectedPumpName by remember {
        mutableStateOf(pumps.firstOrNull()?.name ?: "المضخة الرئيسية")
    }

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val initialPrice = selectedCustomer?.customPricePerHour
        ?: pumps.find { it.name == selectedPumpName }?.defaultPricePerHour
        ?: defaultPricePerHour

    var pricePerHourStr by remember { mutableStateOf(initialPrice.toString()) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var pumpDropdownExpanded by remember { mutableStateOf(false) }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AccentEmerald)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "بدء عداد ري مياه مباشر (حي)",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("اختر العميل المستفيد:", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Box {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "اختر العميل...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("live_timer_customer_picker"),
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
                            text = { Text(c.name, fontWeight = FontWeight.Bold) },
                            onClick = {
                                selectedCustomerId = c.id
                                if (c.customPricePerHour != null) {
                                    pricePerHourStr = c.customPricePerHour.toString()
                                }
                                customerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            if (pumps.size > 1) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("المضخة / البئر:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    OutlinedTextField(
                        value = selectedPumpName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { pumpDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = pumpDropdownExpanded,
                        onDismissRequest = { pumpDropdownExpanded = false }
                    ) {
                        pumps.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    selectedPumpName = p.name
                                    pumpDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = pricePerHourStr,
                onValueChange = { pricePerHourStr = it },
                label = { Text("سعر الساعة المعتمد لهذه الجلسة ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val price = pricePerHourStr.toDoubleOrNull() ?: defaultPricePerHour
                    if (selectedCustomerId > 0) {
                        onStart(
                            selectedCustomerId,
                            selectedCustomer?.name ?: "عميل",
                            selectedPumpName,
                            price
                        )
                        onDismiss()
                    }
                },
                enabled = selectedCustomerId > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_live_timer_confirm_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تشغيل العداد الآن", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopLiveTimerBottomSheet(
    liveState: LiveTimerState,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirmStop: (amountPaid: Double, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountPaidStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val amountPaid by remember { derivedStateOf { amountPaidStr.toDoubleOrNull() ?: 0.0 } }
    val remainingDebt by remember { derivedStateOf { Math.max(0.0, liveState.currentCost - amountPaid) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color(0xFFE53935))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "إيقاف العداد وترحيل الفاتورة",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live metrics card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("العميل: ${liveState.customerName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المدة الإجمالية:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Formatters.formatDurationClock(liveState.elapsedSeconds),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المبلغ الإجمالي المحسوب:", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            Formatters.formatCurrency(liveState.currentCost, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal,
                            fontSize = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amountPaidStr,
                onValueChange = { amountPaidStr = it },
                label = { Text("المبلغ المسدد نقداً الآن") },
                placeholder = { Text("0") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("المتبقي بذمة العميل:")
                Text(
                    Formatters.formatCurrency(remainingDebt, currencySymbol),
                    fontWeight = FontWeight.Bold,
                    color = if (remainingDebt > 0) Color(0xFFE53935) else AccentEmerald
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات الجلسة") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onConfirmStop(amountPaid, notes)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_stop_live_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text("تثبيت وحفظ الفاتورة في الحساب", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

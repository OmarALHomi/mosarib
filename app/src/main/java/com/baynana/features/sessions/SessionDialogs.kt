package com.baynana.features.sessions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baynana.core.util.Formatters
import com.baynana.features.customers.AddEditCustomerBottomSheet
import com.baynana.features.customers.Customer
import com.baynana.features.pumps.PumpSource
import com.baynana.ui.theme.AccentEmerald
import com.baynana.ui.theme.AccentGold
import com.baynana.ui.theme.PrimaryTeal
import com.baynana.ui.theme.SecondaryAqua
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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
    val context = LocalContext.current

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

    // حالة تاريخ السقي ووقت البدء والانتهاء
    var sessionDateMillis by remember { mutableLongStateOf(initialSession?.startTime ?: System.currentTimeMillis()) }

    val defaultStartCal = remember(initialSession) {
        Calendar.getInstance().apply {
            if (initialSession != null) {
                timeInMillis = initialSession.startTime
            } else {
                add(Calendar.HOUR_OF_DAY, -1)
                set(Calendar.MINUTE, (get(Calendar.MINUTE) / 5) * 5)
            }
        }
    }
    val defaultEndCal = remember(initialSession) {
        Calendar.getInstance().apply {
            if (initialSession != null) {
                timeInMillis = initialSession.endTime
            } else {
                set(Calendar.MINUTE, (get(Calendar.MINUTE) / 5) * 5)
            }
        }
    }

    var startHour by remember { mutableIntStateOf(defaultStartCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf(defaultStartCal.get(Calendar.MINUTE)) }
    var endHour by remember { mutableIntStateOf(defaultEndCal.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableIntStateOf(defaultEndCal.get(Calendar.MINUTE)) }

    // حساب فارق الوقت تلقائياً (بما في ذلك السقي الليلي الممتد بعد منتصف الليل)
    val rawDiffMinutes = remember(startHour, startMinute, endHour, endMinute) {
        derivedStateOf {
            val startTotal = startHour * 60 + startMinute
            val endTotal = endHour * 60 + endMinute
            val diff = endTotal - startTotal
            if (diff > 0) {
                diff
            } else if (diff < 0) {
                diff + 1440 // سقي ليلي يعبر منتصف الليل
            } else {
                0
            }
        }
    }

    val totalMinutes by remember(rawDiffMinutes) {
        derivedStateOf {
            if (rawDiffMinutes.value > 0) rawDiffMinutes.value else 60
        }
    }

    val hours by remember(totalMinutes) { derivedStateOf { totalMinutes / 60 } }
    val minutes by remember(totalMinutes) { derivedStateOf { totalMinutes % 60 } }
    val calculatedCost by remember(totalMinutes, pricePerHour) {
        derivedStateOf { Formatters.calculateWaterCost(totalMinutes, pricePerHour) }
    }

    var amountPaidStr by remember { mutableStateOf(initialSession?.amountPaid?.let { if (it > 0) Formatters.formatNumber(it) else "" } ?: "") }
    var notes by remember { mutableStateOf(initialSession?.notes ?: "") }
    val amountPaid by remember { derivedStateOf { Formatters.parseAmountInput(amountPaidStr) } }
    val remainingDebt by remember(calculatedCost, amountPaid) {
        derivedStateOf { Formatters.roundMoney(Math.max(0.0, calculatedCost - amountPaid)) }
    }

    val startCal = remember(sessionDateMillis, startHour, startMinute) {
        Calendar.getInstance().apply {
            timeInMillis = sessionDateMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val endCal = remember(sessionDateMillis, startHour, startMinute, endHour, endMinute) {
        Calendar.getInstance().apply {
            timeInMillis = sessionDateMillis
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val startTotal = startHour * 60 + startMinute
            val endTotal = endHour * 60 + endMinute
            if (endTotal < startTotal) {
                add(Calendar.DAY_OF_YEAR, 1) // سقي ليلي
            }
        }
    }

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

            Spacer(modifier = Modifier.height(14.dp))

            // شريط تاريخ السقي السريع
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = sessionDateMillis }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                sessionDateMillis = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تاريخ السقي: ${Formatters.formatDate(sessionDateMillis)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "تغيير التاريخ 📅",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PrimaryTeal,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Customer Selector
            Text(
                text = "العميل *",
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

            // بطاقة تحديد الساعات التفاعلية للمسربين
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تحديد وقت وساعات السقي",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "حدد وقت البداية والنهاية أو اختر عدد الساعات بنقرة واحدة",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // بطاقتا الوقت (من الساعة ──▶ إلى الساعة)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // وقت البدء
                        TimePickerBox(
                            modifier = Modifier.weight(1f),
                            title = "وقت البدء (من)",
                            timeText = formatClockTimeArabic(startHour, startMinute),
                            accentColor = PrimaryTeal,
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        startHour = h
                                        startMinute = m
                                    },
                                    startHour,
                                    startMinute,
                                    false
                                ).show()
                            }
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryTeal.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )

                        // وقت الانتهاء
                        TimePickerBox(
                            modifier = Modifier.weight(1f),
                            title = "وقت الانتهاء (إلى)",
                            timeText = formatClockTimeArabic(endHour, endMinute),
                            accentColor = AccentEmerald,
                            icon = Icons.Default.Stop,
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        endHour = h
                                        endMinute = m
                                    },
                                    endHour,
                                    endMinute,
                                    false
                                ).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // أزرار الساعات السريعة للمسربين
                    Text(
                        text = "أو اختر عدد الساعات مباشرة بنقرة واحدة:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickHours = listOf(
                            Pair("نصف ساعة", 30),
                            Pair("1 ساعة", 60),
                            Pair("ساعتان", 120),
                            Pair("3 ساعات", 180),
                            Pair("4 ساعات", 240),
                            Pair("5 ساعات", 300),
                            Pair("6 ساعات", 360)
                        )
                        items(quickHours) { (label, mins) ->
                            val isSelected = totalMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val totalM = (startHour * 60 + startMinute) + mins
                                    endHour = (totalM / 60) % 24
                                    endMinute = totalM % 60
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // أزرار الضبط الدقيق السريعة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { endHour = (endHour + 1) % 24 },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("+ 1 ساعة", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { if (totalMinutes > 60) endHour = (endHour - 1 + 24) % 24 },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("- 1 ساعة", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val newM = endMinute + 15
                                if (newM >= 60) {
                                    endHour = (endHour + 1) % 24
                                    endMinute = newM - 60
                                } else {
                                    endMinute = newM
                                }
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("+ 15 د", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val newM = endMinute - 15
                                if (newM < 0) {
                                    if (totalMinutes > 15) {
                                        endHour = (endHour - 1 + 24) % 24
                                        endMinute = newM + 60
                                    }
                                } else {
                                    if (totalMinutes > 15) {
                                        endMinute = newM
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("- 15 د", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // بطاقة النتيجة المحسوبة تلقائياً
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AccentEmerald.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "المدة المحسوبة:",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Text(
                                    text = Formatters.formatDurationArabic(totalMinutes),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AccentEmerald
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "إجمالي قيمة الماء:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = Formatters.formatCurrency(calculatedCost, currencySymbol),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryTeal
                                    )
                                )
                            }

                            if (calculatedCost > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = Formatters.amountToArabicWords(calculatedCost, currencySymbol),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.align(Alignment.End)
                                )
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

            Spacer(modifier = Modifier.height(10.dp))

            // بطاقة حالة السداد والمتبقي كدين
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (remainingDebt > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (remainingDebt > 0) "المتبقي بذمة العميل (دين):" else "حالة الحساب:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = if (remainingDebt > 0) Formatters.formatCurrency(remainingDebt, currencySymbol) else "مسدد بالكامل ✅",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (remainingDebt > 0) Color(0xFFD32F2F) else Color(0xFF2E7D32)
                        )
                    )
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
                            startCal.timeInMillis,
                            endCal.timeInMillis,
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
                    .height(54.dp)
                    .testTag("save_session_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text(
                    text = if (initialSession == null) "حفظ وترحيل دورة السقي 💾" else "حفظ التعديلات 💾",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
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

@Composable
private fun TimePickerBox(
    modifier: Modifier = Modifier,
    title: String,
    timeText: String,
    accentColor: androidx.compose.ui.graphics.Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = timeText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    fontSize = 19.sp
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "انقر للتعديل 🕒",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            )
        }
    }
}

private fun formatClockTimeArabic(hour: Int, minute: Int): String {
    val isPm = hour >= 12
    val hour12 = when (hour % 12) {
        0 -> 12
        else -> hour % 12
    }
    val amPmStr = if (isPm) "م" else "ص"
    return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPmStr)
}


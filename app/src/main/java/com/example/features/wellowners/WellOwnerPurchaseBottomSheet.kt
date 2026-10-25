package com.example.features.wellowners

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribDatePickerDialog
import com.example.core.ui.MosaribTextField
import com.example.core.ui.MosaribTimePickerDialog
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryTeal
import java.util.Calendar

enum class PurchaseTimeMode {
    PICKER,
    MANUAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellOwnerPurchaseBottomSheet(
    owner: Customer? = null,
    availableOwners: List<Customer> = emptyList(),
    currencySymbol: String,
    initialPurchase: WellOwnerPurchase? = null,
    onDismiss: () -> Unit,
    onSaveWithOwner: ((
        ownerId: Long,
        date: Long,
        durationMinutes: Int,
        wastedMinutesOnOwner: Int,
        purchaseRatePerHour: Double,
        amountPaid: Double,
        notes: String,
        startTime: Long,
        endTime: Long
    ) -> Unit)? = null,
    onSave: ((
        date: Long,
        durationMinutes: Int,
        wastedMinutesOnOwner: Int,
        purchaseRatePerHour: Double,
        amountPaid: Double,
        notes: String,
        startTime: Long,
        endTime: Long
    ) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isAppDark()
    val now = System.currentTimeMillis()

    val ownersList = remember(availableOwners, owner) {
        if (owner != null) listOf(owner) else availableOwners.filter { it.isWellOwner }
    }
    var selectedOwner by remember(owner, ownersList) {
        mutableStateOf(owner ?: ownersList.firstOrNull())
    }
    var ownerDropdownExpanded by remember { mutableStateOf(false) }

    // تواريخ وساعات البدء والانتهاء
    val initialStartTime = initialPurchase?.startTime?.takeIf { it > 0 }
        ?: (initialPurchase?.date ?: (now - 3600000L))
    val initialEndTime = initialPurchase?.endTime?.takeIf { it > 0 }
        ?: (initialPurchase?.date?.let { it + (initialPurchase.durationMinutes * 60000L) } ?: now)

    var startDateMillis by remember(initialPurchase) { mutableLongStateOf(initialStartTime) }
    var endDateMillis by remember(initialPurchase) { mutableLongStateOf(initialEndTime) }

    val defaultStartCal = remember(startDateMillis, initialPurchase) {
        Calendar.getInstance().apply { timeInMillis = startDateMillis }
    }
    val defaultEndCal = remember(endDateMillis, initialPurchase) {
        Calendar.getInstance().apply { timeInMillis = endDateMillis }
    }

    var startHour by remember(initialPurchase) { mutableIntStateOf(defaultStartCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember(initialPurchase) { mutableIntStateOf((defaultStartCal.get(Calendar.MINUTE) / 5) * 5) }
    var endHour by remember(initialPurchase) { mutableIntStateOf(defaultEndCal.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember(initialPurchase) { mutableIntStateOf((defaultEndCal.get(Calendar.MINUTE) / 5) * 5) }

    var timeMode by remember(initialPurchase) { mutableStateOf(PurchaseTimeMode.PICKER) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val initialDurationMins = remember(initialPurchase) {
        initialPurchase?.durationMinutes?.takeIf { it > 0 } ?: 60
    }
    var hoursText by remember(initialPurchase) {
        mutableStateOf((initialDurationMins / 60).toString())
    }
    var minutesText by remember(initialPurchase) {
        mutableStateOf(if (initialDurationMins % 60 > 0) (initialDurationMins % 60).toString() else "")
    }

    val computedStartTime by remember(startDateMillis, startHour, startMinute) {
        derivedStateOf {
            Calendar.getInstance().apply {
                timeInMillis = startDateMillis
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, startMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    val computedEndTime by remember(timeMode, endDateMillis, endHour, endMinute, computedStartTime, hoursText, minutesText) {
        derivedStateOf {
            if (timeMode == PurchaseTimeMode.PICKER) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = endDateMillis
                    set(Calendar.HOUR_OF_DAY, endHour)
                    set(Calendar.MINUTE, endMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= computedStartTime) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            } else {
                val h = hoursText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val m = minutesText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                computedStartTime + ((h * 60 + m) * 60000L)
            }
        }
    }

    val durationMinutes by remember(timeMode, computedStartTime, computedEndTime, hoursText, minutesText) {
        derivedStateOf {
            if (timeMode == PurchaseTimeMode.PICKER) {
                val diff = (computedEndTime - computedStartTime) / 60000L
                diff.toInt().coerceAtLeast(0)
            } else {
                val h = hoursText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val m = minutesText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                h * 60 + m
            }
        }
    }

    val isOvernight by remember(computedStartTime, computedEndTime) {
        derivedStateOf {
            val startDay = Calendar.getInstance().apply { timeInMillis = computedStartTime; set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
            val endDay = Calendar.getInstance().apply { timeInMillis = computedEndTime; set(Calendar.HOUR_OF_DAY, 0) }.timeInMillis
            endDay > startDay
        }
    }

    var wastedHoursText by remember(initialPurchase) {
        mutableStateOf(initialPurchase?.let { if (it.wastedMinutesOnOwner > 0) (it.wastedMinutesOnOwner / 60).toString() else "" } ?: "")
    }
    var wastedMinutesText by remember(initialPurchase) {
        mutableStateOf(initialPurchase?.let { if (it.wastedMinutesOnOwner > 0) (it.wastedMinutesOnOwner % 60).toString() else "" } ?: "")
    }
    var amountPaidText by remember(initialPurchase) { mutableStateOf("") }
    var rateText by remember(initialPurchase, selectedOwner?.id, selectedOwner?.customPricePerHour) {
        mutableStateOf(
            initialPurchase?.let { Formatters.formatAmountInput(it.purchaseRatePerHour.toString()) }
                ?: selectedOwner?.customPricePerHour
                    ?.takeIf { it > 0 }
                    ?.let { Formatters.formatAmountInput(it.toString()) }
                ?: ""
        )
    }
    var notes by remember(initialPurchase) { mutableStateOf(initialPurchase?.notes ?: "") }

    val wastedMinutes by remember(wastedHoursText, wastedMinutesText) {
        derivedStateOf {
            (wastedHoursText.toIntOrNull()?.coerceAtLeast(0) ?: 0) * 60 +
                (wastedMinutesText.toIntOrNull()?.coerceIn(0, 59) ?: 0)
        }
    }
    val rate by remember(rateText) { derivedStateOf { Formatters.parseAmountInput(rateText) } }
    val amountPaid by remember(amountPaidText) { derivedStateOf { Formatters.parseAmountInput(amountPaidText) } }
    val chargeableMinutes = (durationMinutes - wastedMinutes).coerceAtLeast(0)
    val payableAmount = Formatters.calculateWaterCost(chargeableMinutes, rate)
    val remainingDueToOwner by remember(payableAmount, amountPaid) {
        derivedStateOf { Formatters.roundMoney(maxOf(0.0, payableAmount - amountPaid)) }
    }
    val wasteCredit = (Formatters.calculateWaterCost(durationMinutes, rate) - payableAmount).coerceAtLeast(0.0)
    val isWasteExceeded by remember(durationMinutes, wastedMinutes) {
        derivedStateOf { wastedMinutes > durationMinutes && durationMinutes > 0 }
    }
    val valid = durationMinutes in 1..6000 && wastedMinutes in 0..durationMinutes && rate > 0

    BackHandler(onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط العنوان
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (initialPurchase != null) "تعديل شراء ساعات" else "شراء ساعات من بئر",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (owner != null) {
                        Text(owner.name, style = MaterialTheme.typography.bodyMedium, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                    } else if (ownersList.size > 1) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { ownerDropdownExpanded = true }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    selectedOwner?.name ?: "اختر صاحب البئر...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PrimaryTeal)
                            }
                            DropdownMenu(
                                expanded = ownerDropdownExpanded,
                                onDismissRequest = { ownerDropdownExpanded = false }
                            ) {
                                ownersList.forEach { o ->
                                    DropdownMenuItem(
                                        text = { Text(o.name, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            selectedOwner = o
                                            ownerDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Text(selectedOwner?.name ?: "صاحب بئر", style = MaterialTheme.typography.bodyMedium, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }

            // ============================================================
            // بطاقة تحديد وقت الشراء بنظام التبويبين (بدء وانتهاء / من + المدة)
            // ============================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // أزرار التبديل بين النمطين
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // تبويب: اختيار (بدء وانتهاء)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { timeMode = PurchaseTimeMode.PICKER },
                            color = if (timeMode == PurchaseTimeMode.PICKER) PrimaryTeal else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = if (timeMode == PurchaseTimeMode.PICKER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "اختيار (بدء وانتهاء)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (timeMode == PurchaseTimeMode.PICKER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // تبويب: يدوي (من + المدة)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    timeMode = PurchaseTimeMode.MANUAL
                                    hoursText = (durationMinutes / 60).toString()
                                    minutesText = if (durationMinutes % 60 > 0) (durationMinutes % 60).toString() else ""
                                },
                            color = if (timeMode == PurchaseTimeMode.MANUAL) PrimaryTeal else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (timeMode == PurchaseTimeMode.MANUAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "من + عدد الساعات",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (timeMode == PurchaseTimeMode.MANUAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (timeMode == PurchaseTimeMode.PICKER) {
                        // ─── النمط الأول: اختيار وقت البدء والانتهاء ───
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // بطاقة وقت وتاريخ البدء
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "بدء الشراء (من)",
                                        style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PrimaryTeal.copy(alpha = 0.08f))
                                            .clickable { showStartTimePicker = true }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = Formatters.formatClockTimeArabic(startHour, startMinute),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showStartDatePicker = true }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = Formatters.formatDate(startDateMillis),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp)
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryTeal.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )

                            // بطاقة وقت وتاريخ الانتهاء
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "نهاية الشراء (إلى)",
                                        style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AccentEmerald.copy(alpha = 0.08f))
                                            .clickable { showEndTimePicker = true }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = Formatters.formatClockTimeArabic(endHour, endMinute),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showEndDatePicker = true }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = Formatters.formatDate(endDateMillis),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ─── النمط الثاني: وقت البدء + عدد الساعات والدقائق ───
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // بطاقة وقت البدء
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "وقت بدء الضخ (من):",
                                            style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = Formatters.formatClockTimeArabic(startHour, startMinute),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "• ${Formatters.formatDate(startDateMillis)}",
                                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        MosaribButton(
                                            text = "الوقت",
                                            onClick = { showStartTimePicker = true },
                                            modifier = Modifier.height(36.dp)
                                        )
                                        MosaribButton(
                                            text = "التاريخ",
                                            onClick = { showStartDatePicker = true },
                                            modifier = Modifier.height(36.dp)
                                        )
                                    }
                                }
                            }

                            // إدخال الساعات والدقائق
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MosaribTextField(
                                    value = hoursText,
                                    onValueChange = { hoursText = it.filter(Char::isDigit).take(4) },
                                    label = "عدد الساعات *",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                MosaribTextField(
                                    value = minutesText,
                                    onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                                    label = "الدقائق الإضافية",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // تنبيه السقي الليلي / اليوم التالي
                    if (isOvernight) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ينتهي الضخ في اليوم التالي (${Formatters.formatDate(computedEndTime)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // شريحة ملخص المدة المحتسبة
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryTeal.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المدة الإجمالية:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                        )
                        Text(
                            text = Formatters.formatDurationArabic(durationMinutes),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryTeal)
                        )
                    }
                }
            }

            // حقول وقت الهدر (التوقفات على صاحب البئر)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MosaribTextField(
                    value = wastedHoursText,
                    onValueChange = { wastedHoursText = it.filter(Char::isDigit).take(4) },
                    label = "ساعات الهدر",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                MosaribTextField(
                    value = wastedMinutesText,
                    onValueChange = { wastedMinutesText = it.filter(Char::isDigit).take(2) },
                    label = "دقائق الهدر",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            if (isWasteExceeded) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF2B1313) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFF87171)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "لا يمكن أن يكون وقت الهدر ($wastedMinutes د) أكبر من مدة الشراء ($durationMinutes د)",
                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            MosaribTextField(
                value = rateText,
                onValueChange = { rateText = Formatters.formatAmountInput(it) },
                label = "سعر شراء الساعة الواحدة ($currencySymbol) *",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            MosaribTextField(
                value = amountPaidText,
                onValueChange = { amountPaidText = Formatters.formatAmountInput(it) },
                label = "المبلغ المسلم نقداً فوراً (اختياري)",
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = AccentEmerald) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // بطاقة الملخص المالي لعملية الشراء
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المدة الصافية بعد الهدر")
                        Text(Formatters.formatDurationArabic(chargeableMinutes), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("إجمالي قيمة الضخ")
                        Text(Formatters.formatCurrency(Formatters.calculateWaterCost(durationMinutes, rate), currencySymbol))
                    }
                    if (wasteCredit > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("خصم الهدر")
                            Text(Formatters.formatCurrency(wasteCredit, currencySymbol), color = if (isDark) Color(0xFFF87171) else Color(0xFFD32F2F))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المستحق لصاحب البئر", fontWeight = FontWeight.Bold)
                        Text(Formatters.formatCurrency(payableAmount, currencySymbol), color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    }
                    if (amountPaid > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المدفوع نقداً فوراً", color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                            Text(Formatters.formatCurrency(amountPaid, currencySymbol), color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = PrimaryTeal.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المتبقي بذمتك له", fontWeight = FontWeight.ExtraBold)
                        Text(
                            Formatters.formatCurrency(remainingDueToOwner, currencySymbol),
                            color = if (remainingDueToOwner > 0) {
                                if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                            } else {
                                if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            MosaribTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "ملاحظات / البيان (اختياري)",
                leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth()
            )

            val curOwner = selectedOwner
            val canSave = valid && curOwner != null
            MosaribButton(
                text = if (initialPurchase != null) "حفظ التعديلات" else "حفظ عملية الشراء",
                onClick = {
                    if (canSave) {
                        val purchaseDate = computedStartTime
                        if (onSaveWithOwner != null) {
                            onSaveWithOwner.invoke(curOwner!!.id, purchaseDate, durationMinutes, wastedMinutes, rate, amountPaid, notes, computedStartTime, computedEndTime)
                        } else {
                            onSave?.invoke(purchaseDate, durationMinutes, wastedMinutes, rate, amountPaid, notes, computedStartTime, computedEndTime)
                        }
                    }
                },
                enabled = canSave,
                leadingIcon = { Icon(if (initialPurchase != null) Icons.Default.Check else Icons.Default.AccessTime, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // نوافذ منتقي التاريخ والوقت
    if (showStartDatePicker) {
        MosaribDatePickerDialog(
            initialTimestamp = startDateMillis,
            title = "تحديد تاريخ بدء الشراء",
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { newDate ->
                startDateMillis = newDate
                showStartDatePicker = false
            }
        )
    }

    if (showStartTimePicker) {
        MosaribTimePickerDialog(
            initialHour24 = startHour,
            initialMinute = startMinute,
            title = "تحديد وقت بدء الشراء (من)",
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { h, m ->
                startHour = h
                startMinute = m
                showStartTimePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        MosaribDatePickerDialog(
            initialTimestamp = endDateMillis,
            title = "تحديد تاريخ انتهاء الشراء",
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { newDate ->
                endDateMillis = newDate
                showEndDatePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        MosaribTimePickerDialog(
            initialHour24 = endHour,
            initialMinute = endMinute,
            title = "تحديد وقت انتهاء الشراء (إلى)",
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { h, m ->
                endHour = h
                endMinute = m
                showEndTimePicker = false
            }
        )
    }
}

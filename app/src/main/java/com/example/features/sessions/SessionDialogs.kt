package com.example.features.sessions

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.core.util.Formatters
import com.example.features.customers.AddEditCustomerBottomSheet
import com.example.features.customers.Customer
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

enum class SessionTimeMode {
    MANUAL,
    PICKER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSessionBottomSheet(
    initialSession: WaterSession? = null,
    customers: List<Customer>,
    defaultCustomerId: Long? = null,
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
        notes: String,
        billedToCustomerId: Long?,
        wastedMinutes: Int,
        wastedReason: String,
        discountAmount: Double,
        costPricePerHour: Double,
        pumpSourceId: Long?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedCustomerId by remember(initialSession?.customerId, defaultCustomerId) {
        mutableLongStateOf(initialSession?.customerId ?: defaultCustomerId ?: customers.firstOrNull()?.id ?: 0L)
    }

    val legacyPumpName = initialSession?.pumpName.orEmpty()

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val initialPrice = initialSession?.pricePerHour
        ?: selectedCustomer?.takeUnless { it.isWellOwner }?.customPricePerHour
        ?: defaultPricePerHour

    var pricePerHourStr by remember(initialPrice) { mutableStateOf(Formatters.formatAmountInput(initialPrice.toString())) }
    val pricePerHour by remember {
        derivedStateOf { Formatters.parseAmountInput(pricePerHourStr).takeIf { it > 0 } ?: defaultPricePerHour }
    }

    // Purchases from well owners are recorded independently in the owner account.
    val historicalCostPricePerHour = initialSession?.costPricePerHour ?: 0.0

    // تواريخ وساعات البدء والانتهاء (تدعم أوقات غير محدودة وتتجاوز 24 ساعة بأريحية)
    val now = System.currentTimeMillis()
    var startDateMillis by remember { mutableLongStateOf(initialSession?.startTime ?: (now - 3600000L)) }
    var endDateMillis by remember { mutableLongStateOf(initialSession?.endTime ?: now) }

    val defaultStartCal = remember(startDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = startDateMillis
        }
    }
    val defaultEndCal = remember(endDateMillis) {
        Calendar.getInstance().apply {
            timeInMillis = endDateMillis
        }
    }

    var startHour by remember { mutableIntStateOf(defaultStartCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember { mutableIntStateOf((defaultStartCal.get(Calendar.MINUTE) / 5) * 5) }
    var endHour by remember { mutableIntStateOf(defaultEndCal.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember { mutableIntStateOf((defaultEndCal.get(Calendar.MINUTE) / 5) * 5) }

    var timeMode by remember { mutableStateOf(SessionTimeMode.MANUAL) }

    val initialDurationMins = remember {
        val diffMs = (initialSession?.endTime ?: now) - (initialSession?.startTime ?: (now - 3600000L))
        if (diffMs > 0) (diffMs / 60000L).toInt() else 60
    }
    var manualHoursStr by remember {
        mutableStateOf((initialDurationMins / 60).let { if (it > 0) it.toString() else "1" })
    }
    var manualMinutesStr by remember {
        mutableStateOf((initialDurationMins % 60).let { if (it > 0) it.toString() else "" })
    }

    val isInitiallyOvernight = remember {
        val sTotal = startHour * 60 + startMinute
        val eTotal = endHour * 60 + endMinute
        endDateMillis > startDateMillis || eTotal < sTotal
    }
    var overnightNotice by remember {
        mutableStateOf<String?>(
            if (isInitiallyOvernight && endDateMillis > startDateMillis) {
                "🌙 ينتهي السقي في اليوم التالي (${Formatters.formatDate(endDateMillis)})"
            } else null
        )
    }

    // الوقت المهدور (التوقفات والأعطال)
    var wastedHoursStr by remember {
        mutableStateOf(if ((initialSession?.wastedMinutes ?: 0) >= 60) ((initialSession?.wastedMinutes ?: 0) / 60).toString() else "")
    }
    var wastedMinutesStr by remember {
        mutableStateOf(if ((initialSession?.wastedMinutes ?: 0) % 60 > 0) ((initialSession?.wastedMinutes ?: 0) % 60).toString() else "")
    }
    var wastedReason by remember { mutableStateOf(initialSession?.wastedReason ?: "") }

    val wastedTotalMinutes by remember(wastedHoursStr, wastedMinutesStr) {
        derivedStateOf {
            val wh = wastedHoursStr.toIntOrNull() ?: 0
            val wm = wastedMinutesStr.toIntOrNull() ?: 0
            (wh * 60) + wm
        }
    }

    // الخصم والمسامحة
    var discountAmountStr by remember {
        mutableStateOf(initialSession?.discountAmount?.let { if (it > 0) Formatters.formatAmountInput(it.toString()) else "" } ?: "")
    }
    val discountAmount by remember { derivedStateOf { Formatters.parseAmountInput(discountAmountStr) } }

    val startCal = remember(startDateMillis, startHour, startMinute) {
        Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val endCal = remember(endDateMillis, endHour, endMinute) {
        Calendar.getInstance().apply {
            timeInMillis = endDateMillis
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun applyManualDuration(hStr: String, mStr: String) {
        val h = hStr.toIntOrNull() ?: 0
        val m = mStr.toIntOrNull() ?: 0
        val totalM = (h * 60) + m
        if (totalM > 0) {
            val newEndMs = startCal.timeInMillis + (totalM * 60000L)
            val newEndCal = Calendar.getInstance().apply { timeInMillis = newEndMs }
            endDateMillis = newEndCal.timeInMillis
            endHour = newEndCal.get(Calendar.HOUR_OF_DAY)
            endMinute = newEndCal.get(Calendar.MINUTE)
            val startDay = Calendar.getInstance().apply { timeInMillis = startDateMillis }.get(Calendar.DAY_OF_YEAR)
            val endDay = newEndCal.get(Calendar.DAY_OF_YEAR)
            if (endDay != startDay) {
                overnightNotice = "🌙 ينتهي السقي في اليوم التالي (${Formatters.formatDate(endDateMillis)})"
            } else {
                overnightNotice = null
            }
        }
    }

    fun onStartTimeChanged(newHour: Int, newMinute: Int) {
        startHour = newHour
        startMinute = newMinute
        if (timeMode == SessionTimeMode.MANUAL) {
            applyManualDuration(manualHoursStr, manualMinutesStr)
        } else {
            val startTotal = newHour * 60 + newMinute
            val endTotal = endHour * 60 + endMinute
            if (endTotal < startTotal) {
                val nextDayCal = Calendar.getInstance().apply {
                    timeInMillis = startDateMillis
                    add(Calendar.DAY_OF_YEAR, 1)
                }
                endDateMillis = nextDayCal.timeInMillis
                val dateStr = Formatters.formatDate(endDateMillis)
                overnightNotice = "🌙 تم تقديم وقت الانتهاء تلقائياً إلى اليوم التالي ($dateStr) لأن ساعة الانتهاء تقع بعد منتصف الليل."
                Toast.makeText(context, "تم التقديم إلى اليوم التالي: $dateStr", Toast.LENGTH_SHORT).show()
            } else if (overnightNotice != null) {
                endDateMillis = startDateMillis
                overnightNotice = null
            }
        }
    }

    fun onEndTimeChanged(newHour: Int, newMinute: Int) {
        endHour = newHour
        endMinute = newMinute
        val startTotal = startHour * 60 + startMinute
        val endTotal = newHour * 60 + newMinute
        if (endTotal < startTotal) {
            val nextDayCal = Calendar.getInstance().apply {
                timeInMillis = startDateMillis
                add(Calendar.DAY_OF_YEAR, 1)
            }
            endDateMillis = nextDayCal.timeInMillis
            val dateStr = Formatters.formatDate(endDateMillis)
            overnightNotice = "🌙 تم تقديم وقت الانتهاء تلقائياً إلى اليوم التالي ($dateStr) لأن ساعة الانتهاء تقع بعد منتصف الليل."
            Toast.makeText(context, "تم التقديم إلى اليوم التالي: $dateStr", Toast.LENGTH_SHORT).show()
        } else {
            val startDay = Calendar.getInstance().apply { timeInMillis = startDateMillis }.get(Calendar.DAY_OF_YEAR)
            val endDay = Calendar.getInstance().apply { timeInMillis = endDateMillis }.get(Calendar.DAY_OF_YEAR)
            if (endDay == startDay + 1 && overnightNotice != null) {
                endDateMillis = startDateMillis
                overnightNotice = null
            }
        }
    }

    // حساب المدة الإجمالية بالدقائق بين تاريخين ووقتين (الحد الأقصى المسموح به 10 أيام = 240 ساعة)
    val MAX_SESSION_MINUTES = 10 * 24 * 60
    val grossMinutes by remember(startCal.timeInMillis, endCal.timeInMillis) {
        derivedStateOf {
            val diffMs = endCal.timeInMillis - startCal.timeInMillis
            if (diffMs > 0) (diffMs / 60000L).toInt() else 60
        }
    }

    val isDurationExceeded by remember(grossMinutes) {
        derivedStateOf { grossMinutes > MAX_SESSION_MINUTES }
    }
    val isWasteExceeded by remember(grossMinutes, wastedTotalMinutes) {
        derivedStateOf { wastedTotalMinutes > grossMinutes && grossMinutes > 0 }
    }

    // جهة الفوترة: سقي على حساب صاحب البئر (مقاصة من مستحقاته)
    var billedToCustomerId by remember(initialSession?.billedToCustomerId) {
        mutableStateOf<Long?>(initialSession?.billedToCustomerId)
    }
    var billedToOwnerDropdownExpanded by remember { mutableStateOf(false) }
    val wellOwners = remember(customers) { customers.filter { it.isWellOwner } }

    // المدة الصافية المفوترة بعد خصم الوقت المهدور
    val netBillableMinutes by remember(grossMinutes, wastedTotalMinutes) {
        derivedStateOf {
            maxOf(0, grossMinutes - wastedTotalMinutes)
        }
    }

    val hours by remember(grossMinutes) { derivedStateOf { grossMinutes / 60 } }
    val minutes by remember(grossMinutes) { derivedStateOf { grossMinutes % 60 } }

    // الحسابات المالية
    val grossWaterAmount by remember(netBillableMinutes, pricePerHour) {
        derivedStateOf { Formatters.calculateWaterCost(netBillableMinutes, pricePerHour) }
    }
    val finalFarmerCharge by remember(grossWaterAmount, discountAmount) {
        derivedStateOf { Formatters.roundMoney(maxOf(0.0, grossWaterAmount - discountAmount)) }
    }

    val isBilledToWellOwner by remember(billedToCustomerId) {
        derivedStateOf { billedToCustomerId != null }
    }
    val effectiveFarmerCharge by remember(finalFarmerCharge, isBilledToWellOwner) {
        derivedStateOf { if (isBilledToWellOwner) 0.0 else finalFarmerCharge }
    }

    var amountPaidStr by remember {
        mutableStateOf(initialSession?.amountPaid?.let { if (it > 0) Formatters.formatAmountInput(it.toString()) else "" } ?: "")
    }
    var notes by remember { mutableStateOf(initialSession?.notes ?: "") }
    val amountPaid by remember { derivedStateOf { Formatters.parseAmountInput(amountPaidStr) } }
    val remainingDebt by remember(effectiveFarmerCharge, amountPaid, isBilledToWellOwner) {
        derivedStateOf {
            if (isBilledToWellOwner) 0.0 else Formatters.roundMoney(maxOf(0.0, effectiveFarmerCharge - amountPaid))
        }
    }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var customerSaveError by remember { mutableStateOf<String?>(null) }
    var showWastedDetails by remember { mutableStateOf(wastedTotalMinutes > 0) }
    var showDiscountDetails by remember { mutableStateOf(discountAmount > 0) }
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

            Spacer(modifier = Modifier.height(12.dp))

            // اختيار العميل
            Text(
                text = "العميل الذي يُحتسب عليه السقي *",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "اختر العميل...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
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
                                        val detail = when {
                                            c.isWellOwner -> "صاحب بئر • يمكن احتساب السقي عليه"
                                            c.farmName.isNotEmpty() -> c.farmName
                                            else -> ""
                                        }
                                        if (detail.isNotEmpty()) {
                                            Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCustomerId = c.id
                                    val customerPrice = if (c.isWellOwner) null else c.customPricePerHour
                                    pricePerHourStr = Formatters.formatAmountInput((customerPrice ?: defaultPricePerHour).toString())
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "إضافة عميل", tint = PrimaryTeal)
                }
            }

            // خيار احتساب السقي على صاحب بئر (مقاصة من مستحقاته)
            if (wellOwners.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (billedToCustomerId != null) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "💧 احتساب السقي على صاحب بئر",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (billedToCustomerId != null) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "تُخصم القيمة كتسديد من مستحقات صاحب البئر لدى المسرب",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                                )
                            }
                            Switch(
                                checked = billedToCustomerId != null,
                                onCheckedChange = { checked ->
                                    billedToCustomerId = if (checked) wellOwners.firstOrNull()?.id else null
                                }
                            )
                        }
                        if (billedToCustomerId != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                val currentOwner = wellOwners.find { it.id == billedToCustomerId }
                                OutlinedTextField(
                                    value = currentOwner?.name ?: "اختر صاحب البئر...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    label = { Text("صاحب البئر المحتسب عليه السقي") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { billedToOwnerDropdownExpanded = true }
                                )
                                DropdownMenu(
                                    expanded = billedToOwnerDropdownExpanded,
                                    onDismissRequest = { billedToOwnerDropdownExpanded = false }
                                ) {
                                    wellOwners.forEach { owner ->
                                        DropdownMenuItem(
                                            text = { Text(owner.name, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                billedToCustomerId = owner.id
                                                billedToOwnerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // بطاقة تحديد وقت السقي بنظام التبويبين (يدوي / اختيار)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // شريط التبويبين (يدوي / اختيار)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // تبويب يدوي
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    timeMode = SessionTimeMode.MANUAL
                                    manualHoursStr = (grossMinutes / 60).toString()
                                    manualMinutesStr = if (grossMinutes % 60 > 0) (grossMinutes % 60).toString() else ""
                                },
                            color = if (timeMode == SessionTimeMode.MANUAL) PrimaryTeal else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (timeMode == SessionTimeMode.MANUAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "يدوي (عدد الساعات)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (timeMode == SessionTimeMode.MANUAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // تبويب اختيار
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    timeMode = SessionTimeMode.PICKER
                                },
                            color = if (timeMode == SessionTimeMode.PICKER) PrimaryTeal else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = if (timeMode == SessionTimeMode.PICKER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اختيار (بدء وانتهاء)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (timeMode == SessionTimeMode.PICKER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (timeMode == SessionTimeMode.MANUAL) {
                        // ─── التبويب الأول: يدوي (حقل الساعات + وقت البداية) ───
                        Column {
                            // وقت وتاريخ البدء
                            Text(
                                text = "وقت وتاريخ بدء السقي:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // النقر على الوقت يفتح محدد الساعة مباشرة
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                TimePickerDialog(
                                                    context,
                                                    { _, h, min -> onStartTimeChanged(h, min) },
                                                    startHour,
                                                    startMinute,
                                                    false
                                                ).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = Formatters.formatClockTimeArabic(startHour, startMinute),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                        )
                                    }

                                    // النقر على التاريخ يفتح محدد اليوم عند رغبة المستخدم فقط
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        cal.set(y, m, d)
                                                        startDateMillis = cal.timeInMillis
                                                        applyManualDuration(manualHoursStr, manualMinutesStr)
                                                    },
                                                    cal.get(Calendar.YEAR),
                                                    cal.get(Calendar.MONTH),
                                                    cal.get(Calendar.DAY_OF_MONTH)
                                                ).show()
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = Formatters.formatDate(startDateMillis),
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // حقل عدد الساعات والدقائق
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualHoursStr,
                                    onValueChange = {
                                        manualHoursStr = it.filter { c -> c.isDigit() }.take(3)
                                        applyManualDuration(manualHoursStr, manualMinutesStr)
                                    },
                                    label = { Text("عدد الساعات (أقصى حد 240 س / 10 أيام) *") },
                                    placeholder = { Text("مثلاً: 3") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryTeal) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = manualMinutesStr,
                                    onValueChange = {
                                        val filtered = it.filter { c -> c.isDigit() }
                                        val minVal = filtered.toIntOrNull() ?: 0
                                        if (minVal < 60) {
                                            manualMinutesStr = filtered
                                            applyManualDuration(manualHoursStr, manualMinutesStr)
                                        }
                                    },
                                    label = { Text("الدقائق (اختياري)") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // وقت الانتهاء المحسوب تلقائياً
                            val crossesMidnight = endDateMillis > startDateMillis
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "وقت الانتهاء التلقائي:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Text(
                                        text = "${Formatters.formatClockTimeArabic(endHour, endMinute)} ${if (crossesMidnight) "(${Formatters.formatDate(endDateMillis)}) 🌙" else ""}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryTeal)
                                    )
                                }
                            }
                        }
                    } else {
                        // ─── التبويب الثاني: اختيار (وقت البدء ووقت الانتهاء) ───
                        Column {
                            Text(
                                text = "انقر على الساعة لتغيير الوقت، أو على التاريخ إذا أردت تعديل اليوم:",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // بطاقتا الوقت والتاريخ (من ──▶ إلى)
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
                                            text = "بدء السقي (من)",
                                            style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryTeal.copy(alpha = 0.08f))
                                                .clickable {
                                                    TimePickerDialog(
                                                        context,
                                                        { _, h, min -> onStartTimeChanged(h, min) },
                                                        startHour,
                                                        startMinute,
                                                        false
                                                    ).show()
                                                }
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
                                                .clickable {
                                                    val cal = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                                                    DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            cal.set(y, m, d)
                                                            startDateMillis = cal.timeInMillis
                                                        },
                                                        cal.get(Calendar.YEAR),
                                                        cal.get(Calendar.MONTH),
                                                        cal.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                }
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
                                            text = "نهاية السقي (إلى)",
                                            style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AccentEmerald.copy(alpha = 0.08f))
                                                .clickable {
                                                    TimePickerDialog(
                                                        context,
                                                        { _, h, min -> onEndTimeChanged(h, min) },
                                                        endHour,
                                                        endMinute,
                                                        false
                                                    ).show()
                                                }
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
                                                .clickable {
                                                    val cal = Calendar.getInstance().apply { timeInMillis = endDateMillis }
                                                    DatePickerDialog(
                                                        context,
                                                        { _, y, m, d ->
                                                            cal.set(y, m, d)
                                                            endDateMillis = cal.timeInMillis
                                                            overnightNotice = null
                                                        },
                                                        cal.get(Calendar.YEAR),
                                                        cal.get(Calendar.MONTH),
                                                        cal.get(Calendar.DAY_OF_MONTH)
                                                    ).show()
                                                }
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = if (overnightNotice != null) Color(0xFF2563EB) else Color.Gray,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = Formatters.formatDate(endDateMillis),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (overnightNotice != null) Color(0xFF2563EB) else Color.Gray,
                                                    fontWeight = if (overnightNotice != null) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 10.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // إشعار التقديم التلقائي إلى اليوم التالي (عبر منتصف الليل)
                            if (overnightNotice != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = overnightNotice!!,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF1E40AF),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // شرائح المدد السريعة بنقرة واحدة (تشمل حتى 48 ساعة)
                    Text(
                        text = "أو ضبط المدة مباشرة بنقرة واحدة:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        val quickDurations = listOf(
                            Pair("نصف ساعة", 30),
                            Pair("1 ساعة", 60),
                            Pair("ساعتان", 120),
                            Pair("3 ساعات", 180),
                            Pair("4 ساعات", 240),
                            Pair("6 ساعات", 360),
                            Pair("8 ساعات", 480),
                            Pair("12 ساعة", 720),
                            Pair("24 ساعة (يوم)", 1440),
                            Pair("يومان (48 س)", 2880),
                            Pair("3 أيام (72 س)", 4320),
                            Pair("5 أيام (120 س)", 7200),
                            Pair("10 أيام (240 س)", 14400)
                        )
                        items(quickDurations) { (label, qMins) ->
                            val isSelected = grossMinutes == qMins
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newEndMs = startCal.timeInMillis + (qMins * 60000L)
                                    val newEndCal = Calendar.getInstance().apply { timeInMillis = newEndMs }
                                    endDateMillis = newEndCal.timeInMillis
                                    endHour = newEndCal.get(Calendar.HOUR_OF_DAY)
                                    endMinute = newEndCal.get(Calendar.MINUTE)
                                    manualHoursStr = (qMins / 60).toString()
                                    manualMinutesStr = if (qMins % 60 > 0) (qMins % 60).toString() else ""
                                    val startDay = Calendar.getInstance().apply { timeInMillis = startDateMillis }.get(Calendar.DAY_OF_YEAR)
                                    val endDay = newEndCal.get(Calendar.DAY_OF_YEAR)
                                    if (endDay != startDay) {
                                        overnightNotice = "🌙 ينتهي السقي في اليوم التالي (${Formatters.formatDate(endDateMillis)})"
                                    } else {
                                        overnightNotice = null
                                    }
                                },
                                label = {
                                    Text(label, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, fontSize = 11.sp)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryTeal,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (isDurationExceeded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFF87171)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚠️ لا يمكن تسجيل سقي بساعات خيالية! الحد الأقصى المسموح به للدورة الواحدة هو 10 أيام (240 ساعة / 14,400 دقيقة).",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // قسم الوقت المهدور (التوقفات / الأعطال)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = if (wastedTotalMinutes > 0) Color(0xFFFFF7ED) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ الوقت المهدور والتوقفات (يُخصم)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (wastedTotalMinutes > 0) Color(0xFFC2410C) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { showWastedDetails = !showWastedDetails },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (showWastedDetails) "إخفاء" else "تحديد هدر",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (showWastedDetails || wastedTotalMinutes > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = wastedHoursStr,
                                onValueChange = { wastedHoursStr = it.filter { char -> char.isDigit() } },
                                label = { Text("ساعات الهدر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            OutlinedTextField(
                                value = wastedMinutesStr,
                                onValueChange = { wastedMinutesStr = it.filter { char -> char.isDigit() } },
                                label = { Text("دقائق الهدر") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = wastedReason,
                            onValueChange = { wastedReason = it },
                            label = { Text("سبب التوقف (عطل مضخة، نقص ديزل...)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        if (wastedTotalMinutes > grossMinutes && grossMinutes > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "❌ لا يمكن أن يكون الهدر أكبر من مدة السقي بتاتاً!",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Historical sale price for this session. Purchase rates are captured separately.
            OutlinedTextField(
                value = pricePerHourStr,
                onValueChange = { pricePerHourStr = Formatters.formatAmountInput(it) },
                label = { Text("سعر البيع ($currencySymbol/ساعة)") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // قسم الخصم والمسامحة
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = if (discountAmount > 0) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏷️ خصم ومسامحة للمزارع (مبلغ مالي)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (discountAmount > 0) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface)
                        )
                        TextButton(onClick = { showDiscountDetails = !showDiscountDetails }) {
                            Text(if (showDiscountDetails) "إخفاء" else "إضافة خصم")
                        }
                    }

                    if (showDiscountDetails || discountAmount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = discountAmountStr,
                            onValueChange = { discountAmountStr = Formatters.formatAmountInput(it) },
                            label = { Text("مبلغ الخصم أو المسامحة ($currencySymbol)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // بطاقة الحسابات الميدانية الشاملة (للمزارع وصاحب البئر وصافي الربح)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // سطر الساعات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المدة الإجمالية:", style = MaterialTheme.typography.bodyMedium)
                        Text(Formatters.formatDurationArabic(grossMinutes), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    if (wastedTotalMinutes > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الوقت المهدور المخصوم:", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC2410C)))
                            Text("- ${Formatters.formatDurationArabic(wastedTotalMinutes)}", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFC2410C), fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الساعات الصافية المفوترة:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text(Formatters.formatDurationArabic(netBillableMinutes), style = MaterialTheme.typography.titleMedium.copy(color = PrimaryTeal, fontWeight = FontWeight.ExtraBold))
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = PrimaryTeal.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isBilledToWellOwner) {
                        val ownerName = wellOwners.find { it.id == billedToCustomerId }?.name ?: "صاحب البئر"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💧 مسجل على حساب صاحب البئر:", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0369A1), fontWeight = FontWeight.Bold))
                            Text(ownerName, style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0369A1), fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المطلوب من المزارع:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text("0.00 $currencySymbol (مدفوع من المالك)", style = MaterialTheme.typography.titleMedium.copy(color = AccentEmerald, fontWeight = FontWeight.ExtraBold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المحتسب على صاحب البئر:", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0369A1)))
                            Text(Formatters.formatCurrency(finalFarmerCharge, currencySymbol), style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0369A1), fontWeight = FontWeight.Bold))
                        }
                    } else {
                        // المبلغ المطلوب من المزارع
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المطلوب الصافي من العميل:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Text(Formatters.formatCurrency(finalFarmerCharge, currencySymbol), style = MaterialTheme.typography.titleLarge.copy(color = PrimaryTeal, fontWeight = FontWeight.ExtraBold))
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // المبلغ المدفوع فوراً والمتبقي كدين
            OutlinedTextField(
                value = amountPaidStr,
                onValueChange = { amountPaidStr = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ المدفوع فوراً نقدياً (اختياري)") },
                placeholder = { Text("0") },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("session_paid_input"),
                shape = RoundedCornerShape(12.dp)
            )

            if (remainingDebt > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المتبقي بذمة العميل (دين):", style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB91C1C)))
                    Text(Formatters.formatCurrency(remainingDebt, currencySymbol), style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold))
                }
            }

            TextButton(onClick = { showNotes = !showNotes }) {
                Text(if (showNotes) "إخفاء الملاحظات" else "إضافة ملاحظات (اختياري)")
            }
            if (showNotes) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات الجلسة") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    if (selectedCustomerId > 0 && grossMinutes in 1..MAX_SESSION_MINUTES && wastedTotalMinutes <= grossMinutes) {
                        onSave(
                            initialSession?.id ?: 0L,
                            selectedCustomerId,
                            legacyPumpName,
                            startCal.timeInMillis,
                            endCal.timeInMillis,
                            hours,
                            minutes,
                            pricePerHour,
                            amountPaid,
                            notes,
                            billedToCustomerId,
                            wastedTotalMinutes,
                            wastedReason,
                            discountAmount,
                            historicalCostPricePerHour,
                            initialSession?.pumpSourceId
                        )
                        onDismiss()
                    }
                },
                enabled = selectedCustomerId > 0 && grossMinutes in 1..MAX_SESSION_MINUTES && wastedTotalMinutes <= grossMinutes,
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
            onSave = { _, name, phone, farmName, location, customerNotes, customPrice, isBeneficiary, isWellOwner ->
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
                                isBeneficiary = isBeneficiary,
                                isWellOwner = isWellOwner
                            )
                        )
                    }.onSuccess { customerId ->
                        selectedCustomerId = customerId
                        val salePrice = if (isWellOwner) null else customPrice
                        pricePerHourStr = Formatters.formatAmountInput(
                            (salePrice ?: defaultPricePerHour).toString()
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


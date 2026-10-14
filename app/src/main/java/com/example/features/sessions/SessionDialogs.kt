package com.example.features.sessions

import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.core.ui.MosaribAutoCompleteTextField
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribDatePickerDialog
import com.example.core.ui.MosaribFilterChip
import com.example.core.ui.MosaribOutlinedButton
import com.example.core.ui.MosaribTextField
import com.example.core.ui.MosaribTimePickerDialog
import com.example.core.ui.isAppDark
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val isDark = isAppDark()

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

    // تواريخ وساعات البدء والانتهاء (تُعاد تهيئتها دائماً عند فتح نموذج جديد دون الاحتفاظ بالوقت السابق)
    val formKey = remember { System.currentTimeMillis() }
    val now = if (initialSession != null) System.currentTimeMillis() else formKey
    val initialStartTime = initialSession?.startTime ?: (now - 3600000L)
    val initialEndTime = initialSession?.endTime ?: now

    var startDateMillis by remember(initialSession?.id, formKey) { mutableLongStateOf(initialStartTime) }
    var endDateMillis by remember(initialSession?.id, formKey) { mutableLongStateOf(initialEndTime) }

    val defaultStartCal = remember(startDateMillis, initialSession?.id, formKey) {
        Calendar.getInstance().apply {
            timeInMillis = startDateMillis
        }
    }
    val defaultEndCal = remember(endDateMillis, initialSession?.id, formKey) {
        Calendar.getInstance().apply {
            timeInMillis = endDateMillis
        }
    }

    var startHour by remember(initialSession?.id, formKey) { mutableIntStateOf(defaultStartCal.get(Calendar.HOUR_OF_DAY)) }
    var startMinute by remember(initialSession?.id, formKey) { mutableIntStateOf((defaultStartCal.get(Calendar.MINUTE) / 5) * 5) }
    var endHour by remember(initialSession?.id, formKey) { mutableIntStateOf(defaultEndCal.get(Calendar.HOUR_OF_DAY)) }
    var endMinute by remember(initialSession?.id, formKey) { mutableIntStateOf((defaultEndCal.get(Calendar.MINUTE) / 5) * 5) }

    var timeMode by remember(initialSession?.id, formKey) { mutableStateOf(SessionTimeMode.PICKER) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val initialDurationMins = remember(initialSession?.id, formKey) {
        val diffMs = (initialSession?.endTime ?: now) - (initialSession?.startTime ?: (now - 3600000L))
        if (diffMs > 0) (diffMs / 60000L).toInt() else 60
    }
    var manualHoursStr by remember(initialSession?.id, formKey) {
        mutableStateOf((initialDurationMins / 60).let { if (it > 0) it.toString() else "1" })
    }
    var manualMinutesStr by remember(initialSession?.id, formKey) {
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
                "ينتهي السقي في اليوم التالي (${Formatters.formatDate(endDateMillis)})"
            } else null
        )
    }

    val dayDifference by remember(startDateMillis, endDateMillis) {
        derivedStateOf {
            val startDayCal = Calendar.getInstance().apply {
                timeInMillis = startDateMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val endDayCal = Calendar.getInstance().apply {
                timeInMillis = endDateMillis
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val diffMs = endDayCal.timeInMillis - startDayCal.timeInMillis
            (diffMs / (24 * 60 * 60 * 1000L)).toInt()
        }
    }

    val dayDescription by remember(dayDifference, endDateMillis) {
        derivedStateOf {
            when {
                dayDifference <= 0 -> null
                dayDifference == 1 -> "اليوم التالي (${Formatters.formatDate(endDateMillis)})"
                dayDifference == 2 -> "بعد يومين (${Formatters.formatDate(endDateMillis)})"
                dayDifference in 3..10 -> "بعد $dayDifference أيام (${Formatters.formatDate(endDateMillis)})"
                dayDifference > 10 -> "بعد $dayDifference يوماً (${Formatters.formatDate(endDateMillis)})"
                else -> null
            }
        }
    }

    val isEndNight by remember(endHour) {
        derivedStateOf { endHour < 6 || endHour >= 18 }
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
                overnightNotice = "ينتهي السقي في اليوم التالي (${Formatters.formatDate(endDateMillis)})"
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
                overnightNotice = "تم تقديم وقت الانتهاء تلقائياً إلى اليوم التالي ($dateStr) لأن ساعة الانتهاء تقع بعد منتصف الليل."
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
            overnightNotice = "تم تقديم وقت الانتهاء تلقائياً إلى اليوم التالي ($dateStr) لأن ساعة الانتهاء تقع بعد منتصف الليل."
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

    val isBilledToWellOwner by remember(billedToCustomerId, selectedCustomer) {
        derivedStateOf { billedToCustomerId != null || selectedCustomer?.isWellOwner == true }
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

    var customerInputText by remember(selectedCustomer?.name, formKey) {
        mutableStateOf(selectedCustomer?.name ?: "")
    }
    var showQuickCustomerDialog by remember { mutableStateOf(false) }
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
                    text = if (initialSession == null) "تسجيل سقي جديد" else "تعديل بيانات السقي",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // اختيار العميل مع إمكانية الكتابة المباشرة وظهور الاقتراحات فوراً عبر الحقل الموحد
            Row(verticalAlignment = Alignment.CenterVertically) {
                MosaribAutoCompleteTextField(
                    value = customerInputText,
                    onValueChange = {
                        customerInputText = it
                        val exact = customers.find { c -> c.name.trim().equals(it.trim(), ignoreCase = true) }
                        if (exact != null) {
                            selectedCustomerId = exact.id
                            val customerPrice = if (exact.isWellOwner) null else exact.customPricePerHour
                            pricePerHourStr = Formatters.formatAmountInput((customerPrice ?: defaultPricePerHour).toString())
                        } else {
                            selectedCustomerId = 0L
                        }
                    },
                    items = customers,
                    itemText = { it.name },
                    itemDetail = { listOf(it.farmName, it.phone).filter { s -> s.isNotBlank() }.joinToString(" • ") },
                    isItemSecondary = { it.isWellOwner },
                    isItemSelected = { it.id == selectedCustomerId },
                    placeholder = "اكتب او اختر الاسم",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
                    onItemSelected = { c ->
                        selectedCustomerId = c.id
                        customerInputText = c.name
                        val customerPrice = if (c.isWellOwner) null else c.customPricePerHour
                        pricePerHourStr = Formatters.formatAmountInput((customerPrice ?: defaultPricePerHour).toString())
                    },
                    onAddNewClicked = {
                        customerSaveError = null
                        showQuickCustomerDialog = true
                    },
                    addNewPrompt = "العميل غير مسجل: \"$customerInputText\"",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("session_customer_picker")
                )
                IconButton(
                    onClick = {
                        customerSaveError = null
                        showQuickCustomerDialog = true
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "إضافة عميل", tint = PrimaryTeal)
                }
            }

            // خيار احتساب السقي على صاحب بئر (مقاصة من مستحقاته)
            if (selectedCustomer?.isWellOwner == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (isDark) Color(0xFF0C2433) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF0369A1) else Color(0xFFBAE6FD)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حساب صاحب بئر: يُحتسب هذا السقي مقاصة تلقائية وتُخصم قيمته من مستحقات مشتريات الساعات لديه.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                fontSize = 11.5.sp
                            )
                        )
                    }
                }
            } else if (wellOwners.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = if (billedToCustomerId != null) {
                        if (isDark) Color(0xFF0C2433) else Color(0xFFE0F2FE)
                    } else {
                        if (isDark) Color(0xFF141F25) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(1.dp, if (billedToCustomerId != null) {
                        if (isDark) Color(0xFF0369A1) else Color(0xFFBAE6FD)
                    } else {
                        if (isDark) Color(0xFF2A3C45) else Color(0xFFE2E8F0)
                    }),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.WaterDrop,
                                        contentDescription = null,
                                        tint = if (billedToCustomerId != null) {
                                            if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                        } else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "احتساب السقي على صاحب بئر",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (billedToCustomerId != null) {
                                                if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                            } else MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                }
                                Text(
                                    text = "تُخصم القيمة كتسديد من مستحقات صاحب البئر لدى المسرب",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else Color.Gray,
                                        fontSize = 11.sp
                                    )
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
                                MosaribTextField(
                                    value = currentOwner?.name ?: "اختر صاحب البئر...",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    label = "صاحب البئر المحتسب عليه السقي",
                                    modifier = Modifier.fillMaxWidth()
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
                    // شريط التبويبين (اختيار / يدوي)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // تبويب اختيار (بدء وانتهاء) أولاً
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

                        // تبويب يدوي (عدد الساعات) ثانياً
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
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (timeMode == SessionTimeMode.PICKER) {
                        // ─── التبويب الأول: اختيار (وقت البدء ووقت الانتهاء) ───
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
                                            text = "نهاية السقي (إلى)",
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

                            // إشعار الانتقال لليوم التالي / أيام قادمة
                            if (dayDescription != null || overnightNotice != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF0E2338) else Color(0xFFEFF6FF)
                                    ),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF1D4ED8) else Color(0xFF93C5FD)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isEndNight) Icons.Default.DarkMode else Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = if (isEndNight) (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = dayDescription ?: overnightNotice.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E40AF),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // ─── التبويب الثاني: يدوي (حقل الساعات + وقت البداية) ───
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
                                            .clickable { showStartTimePicker = true }
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
                                            .clickable { showStartDatePicker = true }
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
                                MosaribTextField(
                                    value = manualHoursStr,
                                    onValueChange = {
                                        manualHoursStr = it.filter { c -> c.isDigit() }.take(3)
                                        applyManualDuration(manualHoursStr, manualMinutesStr)
                                    },
                                    label = "عدد الساعات *",
                                    placeholder = "مثلاً: 3",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryTeal) },
                                    modifier = Modifier.weight(1f)
                                )
                                MosaribTextField(
                                    value = manualMinutesStr,
                                    onValueChange = {
                                        val filtered = it.filter { c -> c.isDigit() }
                                        val minVal = filtered.toIntOrNull() ?: 0
                                        if (minVal < 60) {
                                            manualMinutesStr = filtered
                                            applyManualDuration(manualHoursStr, manualMinutesStr)
                                        }
                                    },
                                    label = "الدقائق",
                                    placeholder = "0",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // وقت الانتهاء المحسوب بدقة
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
                                            imageVector = if (isEndNight) Icons.Default.DarkMode else Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = if (isEndNight) (if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "وقت الانتهاء:",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = Formatters.formatClockTimeArabic(endHour, endMinute),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold, color = PrimaryTeal)
                                        )
                                        if (dayDescription != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDark) Color(0xFF0C2433) else Color(0xFFE0F2FE),
                                                border = BorderStroke(0.5.dp, if (isDark) Color(0xFF0369A1) else Color(0xFFBAE6FD))
                                            ) {
                                                Text(
                                                    text = dayDescription!!,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isDurationExceeded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF2B1313) else Color(0xFFFEF2F2)
                            ),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFF87171)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الحد الأقصى المسموح به للدورة الواحدة هو 10 أيام (240 ساعة).",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // سعر الساعة (مباشرة تحت تحديد الساعات بتصميم أخف وأصغر وبحواشي مقللة)
            Column {
                MosaribTextField(
                    value = pricePerHourStr,
                    onValueChange = { pricePerHourStr = Formatters.formatAmountInput(it) },
                    label = "سعر الساعة ($currencySymbol) *",
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isDense = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pricePerHour > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.amountToArabicWords(pricePerHour, currencySymbol),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // بطاقة منسدلة في الأخير: خيارات إضافية (الوقت المهدور والخصم)
            var showExtraOptions by remember {
                mutableStateOf(wastedTotalMinutes > 0 || discountAmount > 0)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                color = when {
                    wastedTotalMinutes > 0 || discountAmount > 0 -> if (isDark) Color(0xFF162329) else Color(0xFFF8FAFC)
                    isDark -> Color(0xFF141F25)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        wastedTotalMinutes > 0 -> if (isDark) Color(0xFF9A3412) else Color(0xFFFED7AA)
                        discountAmount > 0 -> if (isDark) Color(0xFF166534) else Color(0xFFBBF7D0)
                        isDark -> Color(0xFF2A3C45)
                        else -> Color(0xFFE2E8F0)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showExtraOptions = !showExtraOptions }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "خيارات إضافية (الوقت المهدور والخصم)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (wastedTotalMinutes > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF261811) else Color(0xFFFFF7ED),
                                    border = BorderStroke(0.5.dp, if (isDark) Color(0xFF9A3412) else Color(0xFFFED7AA))
                                ) {
                                    Text(
                                        text = "هدر: ${Formatters.formatDurationArabic(wastedTotalMinutes)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFFFB923C) else Color(0xFFC2410C),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (discountAmount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF11261B) else Color(0xFFF0FDF4),
                                    border = BorderStroke(0.5.dp, if (isDark) Color(0xFF166534) else Color(0xFFBBF7D0))
                                ) {
                                    Text(
                                        text = "خصم: ${Formatters.formatCurrency(discountAmount, currencySymbol)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Icon(
                                imageVector = if (showExtraOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showExtraOptions) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // تفاصيل الوقت المهدور
                        Text(
                            text = "الوقت المهدور (يُخصم من السقي):",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MosaribTextField(
                                value = wastedHoursStr,
                                onValueChange = { wastedHoursStr = it.filter { char -> char.isDigit() } },
                                label = "ساعات الهدر",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            MosaribTextField(
                                value = wastedMinutesStr,
                                onValueChange = { wastedMinutesStr = it.filter { char -> char.isDigit() } },
                                label = "دقائق الهدر",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        MosaribTextField(
                            value = wastedReason,
                            onValueChange = { wastedReason = it },
                            label = "سبب التوقف",
                            placeholder = "مثال: عطل مضخة، نقص ديزل...",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val commonWastedReasons = listOf("عطل مضخة", "نقص ديزل", "صيانة ليات", "انقطاع كهرباء", "توقف مؤقت")
                            items(commonWastedReasons) { r ->
                                val isSelected = wastedReason == r
                                MosaribFilterChip(
                                    selected = isSelected,
                                    onClick = { wastedReason = if (isSelected) "" else r },
                                    label = r
                                )
                            }
                        }
                        if (wastedTotalMinutes > grossMinutes && grossMinutes > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) Color(0xFF2B1313) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFF87171) else Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "لا يمكن أن يكون الهدر أكبر من مدة السقي!",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (isDark) Color(0xFFF87171) else Color(0xFFD32F2F),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // تفاصيل الخصم والمسامحة
                        Text(
                            text = "الخصم والمسامحة:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        MosaribTextField(
                            value = discountAmountStr,
                            onValueChange = { discountAmountStr = Formatters.formatAmountInput(it) },
                            label = "مبلغ الخصم ($currencySymbol)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (discountAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.amountToArabicWords(discountAmount, currencySymbol),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // بطاقة الحسابات الميدانية الشاملة
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF141F25) else PrimaryTeal.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else PrimaryTeal.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // سطر الساعات
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المدة الكلية", style = MaterialTheme.typography.bodyMedium)
                        Text(Formatters.formatDurationArabic(grossMinutes), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    if (wastedTotalMinutes > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الهدر", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFFB923C) else Color(0xFFC2410C)))
                            Text("- ${Formatters.formatDurationArabic(wastedTotalMinutes)}", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFFFB923C) else Color(0xFFC2410C), fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الصافي", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) Color(0xFF0C2433) else Color(0xFFE0F2FE))
                                .border(1.dp, if (isDark) Color(0xFF0369A1) else Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسجل على حساب صاحب البئر:", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1), fontWeight = FontWeight.Bold))
                            }
                            Text(ownerName, style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1), fontWeight = FontWeight.Bold))
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
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المحتسب على صاحب البئر:", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)))
                                Text(Formatters.formatCurrency(finalFarmerCharge, currencySymbol), style = MaterialTheme.typography.bodyMedium.copy(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1), fontWeight = FontWeight.Bold))
                            }
                            if (finalFarmerCharge > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    Formatters.amountToArabicWords(finalFarmerCharge, currencySymbol),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    } else {
                        // المبلغ المطلوب من المزارع
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("صافي الدفع", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(Formatters.formatCurrency(finalFarmerCharge, currencySymbol), style = MaterialTheme.typography.titleLarge.copy(color = PrimaryTeal, fontWeight = FontWeight.ExtraBold))
                            }
                            if (finalFarmerCharge > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    Formatters.amountToArabicWords(finalFarmerCharge, currencySymbol),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // المبلغ المستلم نقدياً فوراً
            val isFullPaid = finalFarmerCharge > 0 && amountPaid >= finalFarmerCharge
            val excessAmount = if (!isBilledToWellOwner && amountPaid > finalFarmerCharge && finalFarmerCharge > 0) {
                Formatters.roundMoney(amountPaid - finalFarmerCharge)
            } else 0.0

            Column {
                Text(
                    text = "المبلغ المستلم ($currencySymbol)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                    ),
                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. زر "دفع كامل" بجانب حقل المبلغ من جهة اليمين (في RTL) ومحاذي تماماً بارتفاع 52dp
                    if (!isBilledToWellOwner && finalFarmerCharge > 0) {
                        Surface(
                            modifier = Modifier
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    amountPaidStr = if (isFullPaid) "" else Formatters.formatAmountInput(finalFarmerCharge.toString())
                                },
                            color = if (isFullPaid) AccentEmerald.copy(alpha = 0.12f) else (if (isDark) Color(0xFF141F25) else Color.White),
                            border = BorderStroke(1.dp, if (isFullPaid) AccentEmerald else (if (isDark) Color(0xFF2A3F4D) else Color(0xFFCBD5E1))),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isFullPaid,
                                    onCheckedChange = { checked ->
                                        amountPaidStr = if (checked) Formatters.formatAmountInput(finalFarmerCharge.toString()) else ""
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "دفع كامل",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFullPaid) AccentEmerald else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }

                    // 2. حقل المبلغ المستلم بمحاذاة تامة وبنفس الارتفاع والتنسيق
                    MosaribTextField(
                        value = amountPaidStr,
                        onValueChange = { amountPaidStr = Formatters.formatAmountInput(it) },
                        placeholder = "0",
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = AccentEmerald) },
                        trailingIcon = if (amountPaidStr.isNotBlank()) {
                            {
                                IconButton(onClick = { amountPaidStr = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(16.dp))
                                }
                            }
                        } else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("session_paid_input")
                    )
                }

                if (amountPaid > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Formatters.amountToArabicWords(amountPaid, currencySymbol),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentEmerald,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // تسوية وتصفير باقي الجلسة كخصم ومسامحة عند دفع مبلغ أقل من الإجمالي
                val diffToSettle = Formatters.roundMoney(grossWaterAmount - amountPaid)
                if (amountPaid > 0.0 && diffToSettle > 0.0 && discountAmount != diffToSettle) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                discountAmountStr = Formatters.formatAmountInput(diffToSettle.toString())
                                showDiscountDetails = true
                            }
                            .background(
                                color = if (isDark) Color(0xFF2E2415) else Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFF59E0B).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تسوية وتصفير باقي الجلسة كخصم:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFCD34D) else Color(0xFF92400E)
                                )
                            )
                        }
                        Text(
                            text = Formatters.formatCurrency(diffToSettle, currencySymbol),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                            )
                        )
                    }
                }

                // إشعار الفائض النقدي وسند القبض المباشر
                if (excessAmount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        color = if (isDark) Color(0xFF0F291E) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFA7F3D0)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AccentEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "المبلغ المستلم أكبر من المطلوب بفائض: ${Formatters.formatCurrency(excessAmount, currencySymbol)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "سيتم تلقائياً إصدار سند قبض مباشر بالفائض (${Formatters.formatCurrency(excessAmount, currencySymbol)}) لتسديد ديون العميل السابقة إن وُجدت، أو قيده كرصيد دائن لصالحه عند الحفظ.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            if (isBilledToWellOwner) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF0C2433) else Color(0xFFE0F2FE))
                        .border(1.dp, if (isDark) Color(0xFF0369A1) else Color(0xFFBAE6FD), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المعاملة المحاسبية:", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)))
                    Text("مقاصة تخصم من مستحقات الشراء", style = MaterialTheme.typography.bodySmall.copy(color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1), fontWeight = FontWeight.Bold))
                }
            } else if (remainingDebt > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp)),
                    color = if (isDark) Color(0xFF2B1313) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المتبقي (آجل):",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = Formatters.formatCurrency(remainingDebt, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.amountToArabicWords(remainingDebt, currencySymbol),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            TextButton(onClick = { showNotes = !showNotes }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (showNotes) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(if (showNotes) "إخفاء الملاحظات" else "ملاحظة")
                }
            }
            if (showNotes) {
                MosaribTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "ملاحظات",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button (تسجيل وحفظ مع أيقونة دالة ودعم الحفظ الفوري للعميل الجديد)
            MosaribButton(
                text = if (initialSession == null) "تسجيل وحفظ" else "حفظ التعديلات",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    val trimmedInput = customerInputText.trim()
                    val matchedCust = customers.find { it.name.trim().equals(trimmedInput, ignoreCase = true) }
                    val finalCustId = if (selectedCustomerId > 0) selectedCustomerId else (matchedCust?.id ?: 0L)

                    if (finalCustId == 0L && trimmedInput.isNotBlank()) {
                        showQuickCustomerDialog = true
                        return@MosaribButton
                    }

                    if (finalCustId > 0 && grossMinutes in 1..MAX_SESSION_MINUTES && wastedTotalMinutes <= grossMinutes) {
                        if (endCal.timeInMillis < startCal.timeInMillis) {
                            Toast.makeText(context, "لا يمكن اختيار تاريخ أو وقت انتهاء قبل تاريخ ووقت البدء", Toast.LENGTH_SHORT).show()
                            return@MosaribButton
                        }
                        onSave(
                            initialSession?.id ?: 0L,
                            finalCustId,
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
                enabled = (selectedCustomerId > 0 || customerInputText.isNotBlank()) && grossMinutes in 1..MAX_SESSION_MINUTES && wastedTotalMinutes <= grossMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_session_button")
            )
        }
    }

    // نوافذ اختيار الوقت والتاريخ المخصصة في التطبيق بدون استخدام نوافذ النظام
    if (showStartTimePicker) {
        MosaribTimePickerDialog(
            initialHour24 = startHour,
            initialMinute = startMinute,
            title = "تحديد وقت بدء السقي",
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = { h, m ->
                onStartTimeChanged(h, m)
                showStartTimePicker = false
            }
        )
    }

    if (showStartDatePicker) {
        MosaribDatePickerDialog(
            initialTimestamp = startDateMillis,
            title = "تحديد تاريخ بدء السقي",
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { newDate ->
                val cal = Calendar.getInstance().apply { timeInMillis = newDate }
                val startCalTemp = Calendar.getInstance().apply { timeInMillis = startDateMillis }
                startCalTemp.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                startDateMillis = startCalTemp.timeInMillis
                if (endDateMillis < startDateMillis) {
                    endDateMillis = startDateMillis
                }
                applyManualDuration(manualHoursStr, manualMinutesStr)
                showStartDatePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        MosaribTimePickerDialog(
            initialHour24 = endHour,
            initialMinute = endMinute,
            title = "تحديد وقت نهاية السقي",
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { h, m ->
                onEndTimeChanged(h, m)
                showEndTimePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        MosaribDatePickerDialog(
            initialTimestamp = if (endDateMillis < startDateMillis) startDateMillis else endDateMillis,
            minTimestamp = startDateMillis,
            title = "تحديد تاريخ نهاية السقي",
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { newDate ->
                val cal = Calendar.getInstance().apply { timeInMillis = newDate }
                val endCalTemp = Calendar.getInstance().apply { timeInMillis = endDateMillis }
                endCalTemp.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                val startDayCal = Calendar.getInstance().apply {
                    timeInMillis = startDateMillis
                    set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endDayCal = Calendar.getInstance().apply {
                    timeInMillis = endCalTemp.timeInMillis
                    set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (endDayCal.before(startDayCal)) {
                    Toast.makeText(context, "لا يمكن اختيار تاريخ انتهاء قبل تاريخ البدء", Toast.LENGTH_SHORT).show()
                    endDateMillis = startDateMillis
                } else {
                    endDateMillis = endCalTemp.timeInMillis
                }
                overnightNotice = null
                showEndDatePicker = false
            }
        )
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

    if (showQuickCustomerDialog) {
        var quickPhone by remember { mutableStateOf("") }
        var quickIsWellOwner by remember { mutableStateOf(false) }
        var isQuickSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isQuickSubmitting) showQuickCustomerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF141F25) else Color.White),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryTeal.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column {
                            Text(
                                text = "تسجيل عميل جديد",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "العميل غير مسجل من قبل، هل تريد حفظه؟",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "الاسم: ${customerInputText.trim()}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MosaribTextField(
                        value = quickPhone,
                        onValueChange = { quickPhone = it },
                        label = "رقم الهاتف (اختياري)",
                        placeholder = "05xxxxxxxx",
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        isDense = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "نوع التسجيل:",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { quickIsWellOwner = false },
                            color = if (!quickIsWellOwner) PrimaryTeal.copy(alpha = 0.15f) else (if (isDark) Color(0xFF162329) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (!quickIsWellOwner) PrimaryTeal else Color.Gray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "مزارع",
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (!quickIsWellOwner) FontWeight.Bold else FontWeight.Normal,
                                    color = if (!quickIsWellOwner) PrimaryTeal else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { quickIsWellOwner = true },
                            color = if (quickIsWellOwner) Color(0xFF0284C7).copy(alpha = 0.15f) else (if (isDark) Color(0xFF162329) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (quickIsWellOwner) Color(0xFF0284C7) else Color.Gray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "صاحب بئر",
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = if (quickIsWellOwner) FontWeight.Bold else FontWeight.Normal,
                                    color = if (quickIsWellOwner) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MosaribOutlinedButton(
                            text = "إلغاء",
                            onClick = { showQuickCustomerDialog = false },
                            modifier = Modifier.weight(1f),
                            height = 46.dp
                        )
                        MosaribButton(
                            text = "حفظ العميل وتسجيل السقي",
                            isLoading = isQuickSubmitting,
                            onClick = {
                                if (!isQuickSubmitting) {
                                    isQuickSubmitting = true
                                    coroutineScope.launch {
                                        try {
                                            val newCustomer = Customer(
                                                name = customerInputText.trim(),
                                                phone = quickPhone.trim(),
                                                isWellOwner = quickIsWellOwner
                                            )
                                            val newId = onCreateCustomer(newCustomer)
                                            showQuickCustomerDialog = false
                                            // حفظ بيانات السقي مباشرة بعد الحفظ
                                            onSave(
                                                initialSession?.id ?: 0L,
                                                newId,
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
                                        } catch (e: Exception) {
                                            isQuickSubmitting = false
                                            Toast.makeText(context, "فشل حفظ العميل: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.8f),
                            height = 46.dp
                        )
                    }
                }
            }
        }
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

                MosaribTextField(
                    value = amountStr,
                    onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                    label = "المبلغ المراد سداده ($currencySymbol) *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
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
                        MosaribFilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = method
                        )
                    }
                }

                MosaribTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "البيان والملاحظات",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            MosaribButton(
                text = "تأكيد السداد وإصدار السند",
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    if (amt > 0) {
                        onConfirmSettle(amt, paymentMethod, notes)
                        onDismiss()
                    }
                },
                enabled = Formatters.parseAmountInput(amountStr) > 0,
                containerColor = AccentEmerald,
                modifier = Modifier.fillMaxWidth()
            )
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
                text = "انقر للتعديل",
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


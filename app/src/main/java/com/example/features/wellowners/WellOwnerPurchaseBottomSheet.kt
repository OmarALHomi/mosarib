package com.example.features.wellowners

import com.example.core.voice.VoiceCommand
import com.example.core.voice.VoiceFieldMic
import com.example.core.voice.VoiceSummaryBanner
import com.example.core.voice.VoiceDialogType
import com.example.core.voice.VoiceSupportDialogs
import com.example.core.voice.CustomerMatcher
import com.example.core.voice.SpokenNumberParser
import com.example.core.voice.VoiceRecognitionHelper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribDatePickerDialog
import com.example.core.ui.MosaribMenu
import com.example.core.ui.MosaribMenuItem
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribSecondaryButton
import com.example.core.ui.MosaribTextField
import com.example.core.ui.MosaribTimePickerDialog
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.mosarib
import java.util.Calendar

/**
 * نمط إدخال أوقات الشراء:
 * - PICKER: تحديد وقت البدء والانتهاء عبر منتقي الوقت والتاريخ.
 * - MANUAL: تحديد وقت البدء + عدد الساعات والدقائق مباشرة.
 */
enum class PurchaseTimeMode {
    PICKER,
    MANUAL
}

data class PurchaseFormSnapshot(
    val ownerId: Long?,
    val durationMinutes: Int,
    val wastedMinutes: Int,
    val rate: Double,
    val amountPaid: Double,
    val notes: String
) {
    companion object {
        fun from(
            ownerId: Long?,
            durationMinutes: Int,
            wastedMinutes: Int,
            rateStr: String,
            amountPaidStr: String,
            notes: String
        ): PurchaseFormSnapshot = PurchaseFormSnapshot(
            ownerId = ownerId,
            durationMinutes = durationMinutes,
            wastedMinutes = wastedMinutes,
            rate = Formatters.parseAmountInput(rateStr),
            amountPaid = Formatters.parseAmountInput(amountPaidStr),
            notes = notes.trim()
        )
    }
}

/**
 * صفيحة سفلية عائمة لتسجيل أو تعديل شراء ساعات ري من صاحب بئر.
 * تدعم حماية الإغلاق العرضي وتأكيد التغييرات غير المحفوظة عبر [MosaribBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellOwnerPurchaseBottomSheet(
    owner: Customer? = null,
    availableOwners: List<Customer> = emptyList(),
    currencySymbol: String,
    initialPurchase: WellOwnerPurchase? = null,
    onDismiss: () -> Unit,
    voiceCommand: VoiceCommand? = null,
    onReSpeak: (() -> Unit)? = null,
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
    val now = System.currentTimeMillis()

    val ownersList = remember(availableOwners, owner) {
        if (owner != null) listOf(owner) else availableOwners.filter { it.isWellOwner }
    }
    var currentVoiceCommand by remember(voiceCommand) { mutableStateOf(voiceCommand) }
    val voiceOwner = remember(voiceCommand, ownersList) {
        voiceCommand?.customerQuery?.let { q ->
            CustomerMatcher.match(q, ownersList).firstOrNull()?.customer
        }
    }
    var selectedOwner by remember(owner, ownersList, voiceOwner) {
        mutableStateOf(owner ?: voiceOwner ?: ownersList.firstOrNull())
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

    val initialVoiceDuration = voiceCommand?.durationMinutes
    var timeMode by remember(initialPurchase, voiceCommand) {
        mutableStateOf(if (initialVoiceDuration != null) PurchaseTimeMode.MANUAL else PurchaseTimeMode.PICKER)
    }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val initialDurationMins = remember(initialPurchase) {
        initialPurchase?.durationMinutes?.takeIf { it > 0 } ?: 60
    }
    var hoursText by remember(initialPurchase, voiceCommand) {
        mutableStateOf(
            if (initialVoiceDuration != null) (initialVoiceDuration / 60).toString()
            else (initialDurationMins / 60).toString()
        )
    }
    var minutesText by remember(initialPurchase, voiceCommand) {
        mutableStateOf(
            if (initialVoiceDuration != null) {
                (initialVoiceDuration % 60).let { if (it > 0) it.toString() else "" }
            } else {
                if (initialDurationMins % 60 > 0) (initialDurationMins % 60).toString() else ""
            }
        )
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
    val context = LocalContext.current
    var amountPaidText by remember(initialPurchase, voiceCommand) {
        mutableStateOf(
            voiceCommand?.paid?.let { Formatters.formatAmountInput(it.toString()) } ?: ""
        )
    }
    var rateText by remember(initialPurchase, selectedOwner?.id, selectedOwner?.customPricePerHour, voiceCommand) {
        mutableStateOf(
            voiceCommand?.ratePerHour?.let { Formatters.formatAmountInput(it.toString()) }
                ?: initialPurchase?.let { Formatters.formatAmountInput(it.purchaseRatePerHour.toString()) }
                ?: selectedOwner?.customPricePerHour
                    ?.takeIf { it > 0 }
                    ?.let { Formatters.formatAmountInput(it.toString()) }
                ?: ""
        )
    }
    var notes by remember(initialPurchase, voiceCommand) {
        mutableStateOf(voiceCommand?.notes ?: initialPurchase?.notes ?: "")
    }

    var pendingVoiceTarget by remember { mutableStateOf<String?>(null) }
    val purchaseVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotBlank()) {
                when (pendingVoiceTarget) {
                    "rate" -> {
                        val parsed = SpokenNumberParser.parseSpokenNumber(spoken)
                        if (parsed != null && parsed > 0) {
                            rateText = Formatters.formatAmountInput(parsed.toString())
                        }
                    }
                    "paid" -> {
                        val parsed = SpokenNumberParser.parseSpokenNumber(spoken)
                        if (parsed != null && parsed >= 0) {
                            amountPaidText = Formatters.formatAmountInput(parsed.toString())
                        }
                    }
                    "notes" -> {
                        notes = if (notes.isBlank()) spoken else "$notes $spoken"
                    }
                    "form_command" -> {
                        val cmd = VoiceRecognitionHelper.parseFirstValidCommand(matches)
                        if (cmd != null) {
                            currentVoiceCommand = cmd
                            if (cmd.durationMinutes != null && cmd.durationMinutes > 0) {
                                timeMode = PurchaseTimeMode.MANUAL
                                hoursText = (cmd.durationMinutes / 60).toString()
                                minutesText = (cmd.durationMinutes % 60).let { if (it > 0) it.toString() else "" }
                            }
                            if (cmd.ratePerHour != null && cmd.ratePerHour > 0) {
                                rateText = Formatters.formatAmountInput(cmd.ratePerHour.toString())
                            }
                            if (cmd.paid != null && cmd.paid >= 0) {
                                amountPaidText = Formatters.formatAmountInput(cmd.paid.toString())
                            }
                            if (!cmd.customerQuery.isNullOrBlank()) {
                                val foundOwner = ownersList.find {
                                    it.name.trim().contains(cmd.customerQuery.trim(), ignoreCase = true)
                                }
                                if (foundOwner != null) {
                                    selectedOwner = foundOwner
                                }
                            }
                            if (!cmd.notes.isNullOrBlank()) {
                                notes = cmd.notes
                            }
                        }
                    }
                }
            }
        }
        pendingVoiceTarget = null
    }

    var voiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }

    fun launchVoiceFor(target: String) {
        pendingVoiceTarget = target
        VoiceRecognitionHelper.launchRecognition(
            context = context,
            launcher = purchaseVoiceLauncher,
            onShowDialog = { voiceDialogType = it },
            prompt = when (target) {
                "rate" -> "تحدث بسعر الساعة..."
                "paid" -> "تحدث بالمبلغ المدفوع..."
                "notes" -> "أملِ الملاحظة..."
                else -> "تحدث بأمر الشراء..."
            }
        )
    }

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

    val currentSnapshot = PurchaseFormSnapshot.from(
        ownerId = selectedOwner?.id,
        durationMinutes = durationMinutes,
        wastedMinutes = wastedMinutes,
        rateStr = rateText,
        amountPaidStr = amountPaidText,
        notes = notes
    )
    val initialSnapshot = remember { currentSnapshot }
    val hasUnsavedChanges by remember(currentSnapshot, initialSnapshot) {
        derivedStateOf { (currentSnapshot != initialSnapshot) || (currentVoiceCommand != null) }
    }

    val curOwner = selectedOwner
    val canSave = valid && curOwner != null

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = if (initialPurchase != null) "تعديل شراء ساعات" else "شراء ساعات من بئر",
        subtitle = if (owner != null) owner.name else selectedOwner?.name ?: "اختر صاحب البئر...",
        icon = Icons.Default.WaterDrop,
        hasUnsavedChanges = hasUnsavedChanges,
        headerActions = {
            VoiceFieldMic(
                onClick = {
                    if (onReSpeak != null) {
                        onReSpeak()
                    } else {
                        launchVoiceFor("form_command")
                    }
                }
            )
        },
        footer = {
            MosaribPrimaryButton(
                text = if (initialPurchase != null) "حفظ التعديل" else "حفظ الشراء",
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
                modifier = Modifier.weight(1f)
            )
            MosaribSecondaryButton(
                text = "إلغاء",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentVoiceCommand != null) {
                VoiceSummaryBanner(
                    command = currentVoiceCommand!!,
                    resolvedCustomerName = selectedOwner?.name,
                    onReSpeak = onReSpeak ?: { launchVoiceFor("form_command") }
                )
            }

            // اختيار صاحب البئر إذا كان هناك أكثر من خيار ولم يتم تمريره ثابتاً
            if (owner == null && ownersList.size > 1) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ownerDropdownExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "صاحب البئر",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedOwner?.name ?: "اختر صاحب البئر...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    MosaribMenu(
                        expanded = ownerDropdownExpanded,
                        onDismissRequest = { ownerDropdownExpanded = false }
                    ) {
                        ownersList.forEach { o ->
                            MosaribMenuItem(
                                text = o.name,
                                icon = Icons.Default.Person,
                                onClick = {
                                    selectedOwner = o
                                    ownerDropdownExpanded = false
                                }
                            )
                        }
                    }
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
                            color = if (timeMode == PurchaseTimeMode.PICKER) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                                    tint = if (timeMode == PurchaseTimeMode.PICKER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "اختيار (بدء وانتهاء)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (timeMode == PurchaseTimeMode.PICKER) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            color = if (timeMode == PurchaseTimeMode.MANUAL) MaterialTheme.colorScheme.primary else Color.Transparent,
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
                                    tint = if (timeMode == PurchaseTimeMode.MANUAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "من + عدد الساعات",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (timeMode == PurchaseTimeMode.MANUAL) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
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
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
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
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = Formatters.formatDate(startDateMillis),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
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
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AccentEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
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
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = Formatters.formatDate(endDateMillis),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
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
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
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
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
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
                                .background(MaterialTheme.mosarib.info.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.mosarib.info,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ينتهي الضخ في اليوم التالي (${Formatters.formatDate(computedEndTime)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.mosarib.info,
                                    fontWeight = FontWeight.Medium
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المدة الإجمالية:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = Formatters.formatDurationArabic(durationMinutes),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        containerColor = MaterialTheme.mosarib.debt.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.mosarib.debt.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "لا يمكن أن يكون وقت الهدر ($wastedMinutes د) أكبر من مدة الشراء ($durationMinutes د)",
                        color = MaterialTheme.mosarib.debt,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            MosaribTextField(
                value = rateText,
                onValueChange = { rateText = Formatters.formatAmountInput(it) },
                label = "سعر الساعة ($currencySymbol) *",
                trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("rate") }) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            MosaribTextField(
                value = amountPaidText,
                onValueChange = { amountPaidText = Formatters.formatAmountInput(it) },
                label = "المدفوع نقداً (اختياري)",
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = AccentEmerald) },
                trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("paid") }) },
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
                            Text(
                                Formatters.formatCurrency(wasteCredit, currencySymbol),
                                color = MaterialTheme.mosarib.debt
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المستحق لصاحب البئر", fontWeight = FontWeight.Bold)
                        Text(
                            Formatters.formatCurrency(payableAmount, currencySymbol),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (amountPaid > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "المدفوع نقداً فوراً",
                                color = MaterialTheme.mosarib.credit,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                Formatters.formatCurrency(amountPaid, currencySymbol),
                                color = MaterialTheme.mosarib.credit,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المتبقي بذمتك له", fontWeight = FontWeight.ExtraBold)
                        Text(
                            Formatters.formatCurrency(remainingDueToOwner, currencySymbol),
                            color = if (remainingDueToOwner > 0) MaterialTheme.mosarib.debt else MaterialTheme.mosarib.credit,
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
                trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("notes") }) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // نوافذ منتقي التاريخ والوقت
    if (showStartDatePicker) {
        MosaribDatePickerDialog(
            initialTimestamp = startDateMillis,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { ts ->
                startDateMillis = ts
                if (endDateMillis < ts) endDateMillis = ts
                showStartDatePicker = false
            }
        )
    }

    if (showStartTimePicker) {
        MosaribTimePickerDialog(
            initialHour24 = startHour,
            initialMinute = startMinute,
            title = "وقت بدء الضخ",
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
            minTimestamp = startDateMillis,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { ts ->
                endDateMillis = ts
                showEndDatePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        MosaribTimePickerDialog(
            initialHour24 = endHour,
            initialMinute = endMinute,
            title = "وقت نهاية الضخ",
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { h, m ->
                endHour = h
                endMinute = m
                showEndTimePicker = false
            }
        )
    }

    VoiceSupportDialogs(
        dialogType = voiceDialogType,
        onDismiss = { voiceDialogType = VoiceDialogType.NONE }
    )
}

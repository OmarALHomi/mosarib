package com.example.features.wellowners

import com.example.core.ui.OperationConfirmSheet
import com.example.core.util.OperationStatement
import com.example.core.ui.guide.GuideHelpButton
import com.example.core.ui.guide.GuidePreferences
import com.example.core.ui.guide.GuideTexts
import com.example.core.ui.guide.SpotlightTour
import com.example.core.ui.guide.guideTarget
import kotlinx.coroutines.delay


import com.example.core.voice.VoiceCommand
import com.example.core.voice.VoiceFieldMic
import com.example.core.voice.VoiceSummaryBanner
import com.example.core.voice.VoiceDialogType
import com.example.core.voice.VoiceSupportDialogs
import com.example.core.voice.VoiceRecordSheet
import com.example.core.voice.VoiceUnrecognizedDialog
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.DarkMode
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
import com.example.core.ui.CustomerPickerField
import com.example.features.sessions.CompactDurationField
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribDatePickerDialog
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribSecondaryButton
import com.example.core.ui.MosaribTextField
import com.example.core.ui.MosaribTimePickerDialog
import com.example.core.ui.REQUIRED_FIELD_TEXT
import com.example.core.ui.RequiredFieldText
import com.example.core.ui.firstMissingField
import com.example.features.customers.AddEditCustomerBottomSheet
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.launch
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.ui.theme.AccentEmerald
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
private enum class PurchaseField { OWNER, START, END, DURATION, RATE, WASTE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WellOwnerPurchaseBottomSheet(
    owner: Customer? = null,
    availableOwners: List<Customer> = emptyList(),
    currencySymbol: String,
    initialPurchase: WellOwnerPurchase? = null,
    onDismiss: () -> Unit,
    voiceCommand: VoiceCommand? = null,
    onReSpeak: (() -> Unit)? = null,
    // when set, the owner picker offers «+ عميل جديد»; the new customer is created as a well owner and selected
    onCreateCustomer: (suspend (Customer) -> Long)? = null,
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
            val ranked = CustomerMatcher.match(q, ownersList)
            if (CustomerMatcher.isHighConfidence(ranked)) {
                ranked.first().customer
            } else {
                null
            }
        }
    }
    var selectedOwner by remember(owner, ownersList, voiceOwner) {
        mutableStateOf(owner ?: voiceOwner ?: if (voiceCommand?.customerQuery != null) null else ownersList.firstOrNull())
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
    // A brand-new form (no edit, no voice prefill) opens with من / إلى and the duration empty.
    val isNewEmpty = initialPurchase == null && voiceCommand == null
    var startPicked by remember(initialPurchase, voiceCommand) { mutableStateOf(!isNewEmpty) }
    var endPicked by remember(initialPurchase, voiceCommand) { mutableStateOf(!isNewEmpty) }
    // set from the duration fields, not by the user: the next duration edit may move it again
    var startAuto by remember { mutableStateOf(false) }
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
            else if (isNewEmpty) ""
            else (initialDurationMins / 60).toString()
        )
    }
    var minutesText by remember(initialPurchase, voiceCommand) {
        mutableStateOf(
            if (initialVoiceDuration != null) {
                (initialVoiceDuration % 60).let { if (it > 0) it.toString() else "" }
            } else if (isNewEmpty) {
                ""
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

    val shownEndCal = remember(computedEndTime) { Calendar.getInstance().apply { timeInMillis = computedEndTime } }

    val durationMinutes by remember(timeMode, computedStartTime, computedEndTime, hoursText, minutesText, startPicked, endPicked) {
        derivedStateOf {
            if (timeMode == PurchaseTimeMode.PICKER) {
                if (!startPicked || !endPicked) 0
                else ((computedEndTime - computedStartTime) / 60000L).toInt().coerceAtLeast(0)
            } else {
                val h = hoursText.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val m = minutesText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                h * 60 + m
            }
        }
    }

    // «إلى» is known when picked, or when a duration gives it
    val endShown = endPicked || (timeMode == PurchaseTimeMode.MANUAL && durationMinutes > 0)
    val isOvernight by remember(computedStartTime, computedEndTime, startPicked, endShown) {
        derivedStateOf {
            if (!startPicked || !endShown) return@derivedStateOf false
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
    var activeTour by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(500)
        if (!GuidePreferences.isTourDone(context, GuidePreferences.TOUR_PURCHASE_FORM)) {
            activeTour = GuidePreferences.TOUR_PURCHASE_FORM
        }
    }

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
    var showPurchaseVoiceSheet by remember { mutableStateOf(false) }
    var isPurchaseVoiceCompact by remember { mutableStateOf(false) }
    var purchaseVoicePrompt by remember { mutableStateOf("تحدث بالعملية...") }
    var unrecognizedPurchaseVoiceText by remember { mutableStateOf<String?>(null) }

    fun launchVoiceFor(target: String) {
        pendingVoiceTarget = target
        isPurchaseVoiceCompact = target != "form_command"
        purchaseVoicePrompt = when (target) {
            "rate" -> "تحدث بسعر الساعة..."
            "paid" -> "تحدث بالمبلغ المدفوع..."
            "notes" -> "أملِ الملاحظة..."
            else -> "تحدث بأمر الشراء..."
        }
        showPurchaseVoiceSheet = true
    }

    fun handlePurchaseVoiceResults(matches: List<String>) {
        val spoken = matches.firstOrNull()?.trim().orEmpty()
        VoiceRecognitionHelper.logDebug("VoiceInput", "Purchase voice target: $pendingVoiceTarget, spoken='$spoken'")
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
                    if (cmd != null && (cmd.durationMinutes != null || cmd.ratePerHour != null || cmd.paid != null || !cmd.customerQuery.isNullOrBlank())) {
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
                    } else {
                        unrecognizedPurchaseVoiceText = spoken
                    }
                }
            }
        }
        pendingVoiceTarget = null
    }

    var voiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }

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

    // Save is always enabled: the first missing field (screen order) is scrolled to, shown in red and focused.
    var errorField by remember { mutableStateOf<PurchaseField?>(null) }
    var errorTick by remember { mutableIntStateOf(0) }
    var ownerOpenSignal by remember { mutableIntStateOf(0) }
    val ownerReq = remember { BringIntoViewRequester() }
    val startReq = remember { BringIntoViewRequester() }
    val endReq = remember { BringIntoViewRequester() }
    val durationReq = remember { BringIntoViewRequester() }
    val rateReq = remember { BringIntoViewRequester() }
    val wasteReq = remember { BringIntoViewRequester() }
    val rateFocus = remember { FocusRequester() }
    var showExtras by remember { mutableStateOf(wastedMinutes > 0 || notes.isNotBlank()) }
    val ownerErr = errorField == PurchaseField.OWNER && curOwner == null
    val startErr = errorField == PurchaseField.START && !startPicked
    val endErr = errorField == PurchaseField.END && !endShown
    val rateErr = errorField == PurchaseField.RATE && rate <= 0
    fun firstMissing(): PurchaseField? = firstMissingField(
        PurchaseField.OWNER to (curOwner != null),
        PurchaseField.START to startPicked,
        PurchaseField.END to endShown,
        PurchaseField.DURATION to (durationMinutes in 1..6000),
        PurchaseField.RATE to (rate > 0),
        PurchaseField.WASTE to (wastedMinutes in 0..durationMinutes)
    )
    LaunchedEffect(errorTick) {
        if (errorTick == 0) return@LaunchedEffect
        when (errorField) {
            PurchaseField.OWNER -> { ownerReq.bringIntoView(); ownerOpenSignal++ }
            PurchaseField.START -> { startReq.bringIntoView(); showStartTimePicker = true }
            PurchaseField.END -> { endReq.bringIntoView(); showEndTimePicker = true }
            PurchaseField.DURATION -> durationReq.bringIntoView()
            PurchaseField.RATE -> { rateReq.bringIntoView(); rateFocus.requestFocus() }
            PurchaseField.WASTE -> { showExtras = true; delay(150); wasteReq.bringIntoView() }
            null -> Unit
        }
    }

    // New well owner made from the picker: select it once it shows up in the owners list.
    var showAddOwnerSheet by remember { mutableStateOf(false) }
    var createdOwnerId by remember { mutableLongStateOf(0L) }
    LaunchedEffect(ownersList, createdOwnerId) {
        if (createdOwnerId > 0L) ownersList.find { it.id == createdOwnerId }?.let { selectedOwner = it }
    }
    val scope = rememberCoroutineScope()
    fun fillStartFromDuration(totalMinutes: Int) {
        if ((startPicked && !startAuto) || totalMinutes <= 0) return
        val c = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis() - totalMinutes * 60000L
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        startDateMillis = c.timeInMillis
        startHour = c.get(Calendar.HOUR_OF_DAY)
        startMinute = c.get(Calendar.MINUTE)
        startPicked = true
        startAuto = true
    }

    var pendingSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    pendingSave?.let { save ->
        val who = curOwner?.name.orEmpty()
        OperationConfirmSheet(
            statement = OperationStatement.purchase(who, durationMinutes, payableAmount, amountPaid, currencySymbol),
            spoken = OperationStatement.purchaseSpoken(who, durationMinutes, payableAmount, amountPaid, currencySymbol),
            onConfirm = { pendingSave = null; save() },
            onEdit = { pendingSave = null }
        )
    }

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = if (initialPurchase != null) "تعديل شراء ساعات" else "شراء ساعات من بئر",
        subtitle = if (owner != null) owner.name else selectedOwner?.name ?: "اختر صاحب البئر...",
        icon = Icons.Default.WaterDrop,
        hasUnsavedChanges = hasUnsavedChanges,
        headerActions = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                GuideHelpButton(onClick = { activeTour = GuidePreferences.TOUR_PURCHASE_FORM })
                VoiceFieldMic(
                    onClick = {
                        if (onReSpeak != null) {
                            onReSpeak()
                        } else {
                            launchVoiceFor("form_command")
                        }
                    },
                    modifier = Modifier.guideTarget("purchase_voice_command")
                )
            }
        },
        footer = {
            // The tour popup must be created from inside the sheet window to be drawn above it.
            if (activeTour != null) {
                // A new tour id must start a fresh step counter.
                key(activeTour) {
                    SpotlightTour(
                        steps = GuideTexts.getTour(activeTour!!),
                        onFinish = {
                            val finished = activeTour
                            activeTour = null
                            if (finished != null) {
                                GuidePreferences.markTourDone(context, finished, true)
                            }
                        }
                    )
                }
            }
            MosaribPrimaryButton(
                text = if (initialPurchase != null) "حفظ التعديل" else "حفظ الشراء",
                onClick = {
                    val missing = firstMissing()
                    if (missing != null) {
                        errorField = missing
                        errorTick++
                        return@MosaribPrimaryButton
                    }
                    val purchaseDate = computedStartTime
                    val performSave: () -> Unit = {
                        if (onSaveWithOwner != null) {
                            onSaveWithOwner.invoke(curOwner!!.id, purchaseDate, durationMinutes, wastedMinutes, rate, amountPaid, notes, computedStartTime, computedEndTime)
                        } else {
                            onSave?.invoke(purchaseDate, durationMinutes, wastedMinutes, rate, amountPaid, notes, computedStartTime, computedEndTime)
                        }
                    }
                    if ((initialPurchase?.id ?: 0L) == 0L) pendingSave = performSave else performSave()
                },
                modifier = Modifier.weight(1f).guideTarget("purchase_save")
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentVoiceCommand != null) {
                VoiceSummaryBanner(
                    command = currentVoiceCommand!!,
                    resolvedCustomerName = selectedOwner?.name,
                    onReSpeak = onReSpeak ?: { launchVoiceFor("form_command") }
                )
            }

            // 1) صاحب البئر
            CustomerPickerField(
                customers = ownersList,
                selected = selectedOwner,
                onSelect = { selectedOwner = it },
                onAddNew = onCreateCustomer?.let { { showAddOwnerSheet = true } },
                label = "صاحب البئر",
                errorText = if (ownerErr) REQUIRED_FIELD_TEXT else null,
                openSignal = ownerOpenSignal,
                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(ownerReq).guideTarget("purchase_owner")
            )

            // 2) الوقت: من / إلى + المدة (تغيير المدة يحرّك «إلى»)
            Row(
                modifier = Modifier.fillMaxWidth().guideTarget("purchase_hours"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PurchaseTimeBox(
                    label = "من",
                    accent = MaterialTheme.colorScheme.primary,
                    timeText = if (startPicked) Formatters.formatClockTimeArabic(startHour, startMinute) else "اختر الوقت",
                    picked = startPicked,
                    isError = startErr,
                    dateText = Formatters.formatDate(startDateMillis),
                    onTime = { showStartTimePicker = true },
                    onDate = { showStartDatePicker = true },
                    modifier = Modifier.weight(1f).bringIntoViewRequester(startReq)
                )
                PurchaseTimeBox(
                    label = "إلى",
                    accent = AccentEmerald,
                    timeText = if (endShown) Formatters.formatClockTimeArabic(
                        shownEndCal.get(Calendar.HOUR_OF_DAY), shownEndCal.get(Calendar.MINUTE)
                    ) else "اختر الوقت",
                    picked = endShown,
                    isError = endErr,
                    dateText = Formatters.formatDate(computedEndTime),
                    onTime = { showEndTimePicker = true },
                    onDate = { showEndDatePicker = true },
                    modifier = Modifier.weight(1f).bringIntoViewRequester(endReq)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(durationReq),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("المدة:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                CompactDurationField(
                    value = (durationMinutes / 60).toString().let { if (it == "0") "" else it },
                    unit = "ساعات",
                    onValueChange = {
                        val h = it.filter(Char::isDigit).trimStart('0').take(3)
                        timeMode = PurchaseTimeMode.MANUAL
                        hoursText = h
                        minutesText = if (durationMinutes % 60 > 0) (durationMinutes % 60).toString() else ""
                        fillStartFromDuration((h.toIntOrNull() ?: 0) * 60 + (minutesText.toIntOrNull() ?: 0))
                    },
                    modifier = Modifier.weight(1f)
                )
                CompactDurationField(
                    value = if (durationMinutes % 60 > 0) (durationMinutes % 60).toString() else "",
                    unit = "دقائق",
                    onValueChange = {
                        val m = it.filter(Char::isDigit).trimStart('0')
                        if ((m.toIntOrNull() ?: 0) < 60) {
                            timeMode = PurchaseTimeMode.MANUAL
                            hoursText = (durationMinutes / 60).toString()
                            minutesText = m
                            fillStartFromDuration((hoursText.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0))
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (isOvernight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.mosarib.info.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.mosarib.info, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ينتهي الضخ في اليوم التالي (${Formatters.formatDate(computedEndTime)})",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.info, fontWeight = FontWeight.Medium)
                    )
                }
            }

            // 3) سعر الساعة | الواصل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).bringIntoViewRequester(rateReq).guideTarget("purchase_price")) {
                    MosaribTextField(
                        value = rateText,
                        onValueChange = { rateText = Formatters.formatAmountInput(it) },
                        label = "سعر الساعة *",
                        placeholder = "مثلاً 5,000",
                        errorText = if (rateErr) REQUIRED_FIELD_TEXT else null,
                        focusRequester = rateFocus,
                        trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("rate") }) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isDense = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (rate > 0) PurchaseWords(Formatters.amountToArabicWords(rate, currencySymbol))
                }
                Column(modifier = Modifier.weight(1f).guideTarget("purchase_paid")) {
                    MosaribTextField(
                        value = amountPaidText,
                        onValueChange = { amountPaidText = Formatters.formatAmountInput(it) },
                        label = "الواصل (المدفوع الآن)",
                        trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("paid") }) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isDense = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (amountPaid > 0) PurchaseWords(Formatters.amountToArabicWords(amountPaid, currencySymbol))
                }
            }

            // 4) المستحق له | المتبقي بذمتك
            val summaryReady = durationMinutes > 0 && rate > 0
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المستحق له", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (summaryReady) Formatters.formatCurrency(payableAmount, currencySymbol) else "—",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (payableAmount > 0) PurchaseWords(Formatters.amountToArabicWords(payableAmount, currencySymbol))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("المتبقي بذمتك", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (!summaryReady) {
                            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.ExtraBold)
                        } else if (remainingDueToOwner > 0) {
                            Text(
                                Formatters.formatCurrency(remainingDueToOwner, currencySymbol),
                                color = MaterialTheme.mosarib.debt,
                                fontWeight = FontWeight.ExtraBold
                            )
                            PurchaseWords(Formatters.amountToArabicWords(remainingDueToOwner, currencySymbol))
                        } else {
                            Text("خالص", color = MaterialTheme.mosarib.credit, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 5) خيارات إضافية: الهدر والملاحظات
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showExtras = !showExtras },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("خيارات إضافية", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Icon(
                            if (showExtras) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (showExtras) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الهدر:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            CompactDurationField(
                                value = wastedHoursText,
                                unit = "ساعات",
                                onValueChange = { wastedHoursText = it.filter(Char::isDigit).trimStart('0').take(4) },
                                modifier = Modifier.weight(1f)
                            )
                            CompactDurationField(
                                value = wastedMinutesText,
                                unit = "دقائق",
                                onValueChange = { wastedMinutesText = it.filter(Char::isDigit).trimStart('0').take(2) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (isWasteExceeded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا يمكن أن يكون وقت الهدر ($wastedMinutes د) أكبر من مدة الشراء ($durationMinutes د)",
                                color = MaterialTheme.mosarib.debt,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.bringIntoViewRequester(wasteReq)
                            )
                        } else if (wastedMinutes > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "المدة الصافية: ${Formatters.formatDurationArabic(chargeableMinutes)}" +
                                    if (wasteCredit > 0) " • خصم الهدر ${Formatters.formatCurrency(wasteCredit, currencySymbol)}" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        MosaribTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "ملاحظات / البيان (اختياري)",
                            trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("notes") }) },
                            modifier = Modifier.fillMaxWidth().guideTarget("purchase_notes")
                        )
                    }
                }
            }
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
                startPicked = true
                startAuto = false
                showStartTimePicker = false
            }
        )
    }

    // Moves the end-picker state to the shown end so editing «إلى» works after a duration edit.
    fun adoptShownEnd() {
        if (endShown) endPicked = true
        endDateMillis = computedEndTime
        endHour = shownEndCal.get(Calendar.HOUR_OF_DAY)
        endMinute = shownEndCal.get(Calendar.MINUTE)
        timeMode = PurchaseTimeMode.PICKER
    }

    if (showEndDatePicker) {
        MosaribDatePickerDialog(
            initialTimestamp = computedEndTime,
            minTimestamp = startDateMillis,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { ts ->
                adoptShownEnd()
                endDateMillis = ts
                showEndDatePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        MosaribTimePickerDialog(
            initialHour24 = shownEndCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = shownEndCal.get(Calendar.MINUTE),
            title = "وقت نهاية الضخ",
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = { h, m ->
                adoptShownEnd()
                endHour = h
                endMinute = m
                endPicked = true
                showEndTimePicker = false
            }
        )
    }

    if (showAddOwnerSheet && onCreateCustomer != null) {
        AddEditCustomerBottomSheet(
            initialCustomer = null,
            currencySymbol = currencySymbol,
            onDismiss = { showAddOwnerSheet = false },
            onSave = { _, name, phone, farmName, location, customerNotes, customPrice, isBeneficiary, _, customGroup ->
                scope.launch {
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
                                isWellOwner = true,
                                customGroup = customGroup.trim()
                            )
                        )
                    }.onSuccess { createdOwnerId = it }
                        .onFailure { Toast.makeText(context, it.localizedMessage ?: "تعذرت إضافة العميل", Toast.LENGTH_SHORT).show() }
                }
            }
        )
    }

    if (showPurchaseVoiceSheet) {
        VoiceRecordSheet(
            title = if (isPurchaseVoiceCompact) "إملاء صوتي" else "الإدخال الصوتي للشراء",
            subtitle = purchaseVoicePrompt,
            compact = isPurchaseVoiceCompact,
            onDismiss = { showPurchaseVoiceSheet = false },
            onResult = { candidates ->
                showPurchaseVoiceSheet = false
                handlePurchaseVoiceResults(candidates)
            }
        )
    }

    unrecognizedPurchaseVoiceText?.let { spoken ->
        VoiceUnrecognizedDialog(
            spokenText = spoken,
            onReSpeak = {
                unrecognizedPurchaseVoiceText = null
                launchVoiceFor("form_command")
            },
            onShowExamples = {
                unrecognizedPurchaseVoiceText = null
            },
            onDismiss = { unrecognizedPurchaseVoiceText = null }
        )
    }

    VoiceSupportDialogs(
        dialogType = voiceDialogType,
        onDismiss = { voiceDialogType = VoiceDialogType.NONE }
    )

}

@Composable
private fun PurchaseWords(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

/** Compact «من» / «إلى» box: label, tappable time, tappable date. */
@Composable
private fun PurchaseTimeBox(
    label: String,
    accent: Color,
    timeText: String,
    dateText: String,
    onTime: () -> Unit,
    onDate: () -> Unit,
    modifier: Modifier = Modifier,
    picked: Boolean = true,
    isError: Boolean = false
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = if (isError) BorderStroke(1.5.dp, Color(0xFFE53935)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.08f))
                    .clickable(onClick = onTime)
                    .padding(vertical = 2.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    timeText,
                    style = if (picked) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).clickable(onClick = onDate).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text(dateText, style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            RequiredFieldText(isError)
        }
    }
}

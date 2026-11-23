package com.example.features.sessions

import com.example.core.ui.guide.FirstTimeHint
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
import androidx.compose.runtime.LaunchedEffect

import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribDialog
import com.example.core.ui.MosaribMenu
import com.example.core.ui.MosaribMenuItem
import com.example.core.ui.MosaribMenuDivider
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribSecondaryButton
import com.example.core.ui.DialogTone
import com.example.ui.theme.mosarib
import android.content.Intent
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.core.ui.CustomerPickerField
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribDatePickerDialog
import com.example.core.ui.MosaribFilterChip
import com.example.core.ui.MosaribOutlinedButton
import com.example.core.ui.MosaribTextField
import com.example.core.ui.OperationConfirmSheet
import com.example.core.util.OperationStatement
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
import androidx.compose.material.icons.filled.Contacts
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
import androidx.compose.runtime.key
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
import com.example.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

/** End of a session that starts at [startMillis] and lasts [durationMinutes] (rolls over midnight naturally). */
internal fun sessionEndMillis(startMillis: Long, durationMinutes: Int): Long = startMillis + durationMinutes * 60000L

/**
 * مقارنة الأيام التقويمية لتحديد ما إذا كان وقت الانتهاء يقع في يوم بعد يوم البدء.
 * تقارن السنة واليوم في السنة لتغطية حالات عبور منتصف الليل ونهاية العام بشكل سليم.
 */
fun isEndDayAfterStartDay(startMillis: Long, endMillis: Long): Boolean {
    val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
    val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
    val startYear = startCal.get(Calendar.YEAR)
    val endYear = endCal.get(Calendar.YEAR)
    return when {
        endYear > startYear -> true
        endYear < startYear -> false
        else -> endCal.get(Calendar.DAY_OF_YEAR) > startCal.get(Calendar.DAY_OF_YEAR)
    }
}

data class SessionFormSnapshot(
    val customerId: Long,
    val customerInputText: String,
    val billedToCustomerId: Long?,
    val grossMinutes: Int,
    val amountPaid: Double,
    val discountAmount: Double,
    val wastedMinutes: Int,
    val wastedReason: String,
    val notes: String,
    val pricePerHour: Double,
    val startTimeMinute: Long,
    val quickName: String,
    val quickPhone: String,
    val quickIsWellOwner: Boolean
) {
    fun hasChangesFrom(other: SessionFormSnapshot): Boolean {
        if (this.customerId != other.customerId) return true
        if (this.customerId <= 0L && this.customerInputText != other.customerInputText) return true
        return this.billedToCustomerId != other.billedToCustomerId ||
            this.grossMinutes != other.grossMinutes ||
            this.amountPaid != other.amountPaid ||
            this.discountAmount != other.discountAmount ||
            this.wastedMinutes != other.wastedMinutes ||
            this.wastedReason != other.wastedReason ||
            this.notes != other.notes ||
            this.pricePerHour != other.pricePerHour ||
            this.startTimeMinute != other.startTimeMinute ||
            this.quickName != other.quickName ||
            this.quickPhone != other.quickPhone ||
            this.quickIsWellOwner != other.quickIsWellOwner
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionFormSnapshot) return false
        return !hasChangesFrom(other)
    }

    override fun hashCode(): Int {
        var result = customerId.hashCode()
        if (customerId <= 0L) {
            result = 31 * result + customerInputText.hashCode()
        }
        result = 31 * result + (billedToCustomerId?.hashCode() ?: 0)
        result = 31 * result + grossMinutes
        result = 31 * result + amountPaid.hashCode()
        result = 31 * result + discountAmount.hashCode()
        result = 31 * result + wastedMinutes
        result = 31 * result + wastedReason.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + pricePerHour.hashCode()
        result = 31 * result + startTimeMinute.hashCode()
        result = 31 * result + quickName.hashCode()
        result = 31 * result + quickPhone.hashCode()
        result = 31 * result + quickIsWellOwner.hashCode()
        return result
    }

    companion object {
        fun from(
            customerId: Long,
            customerInputText: String,
            billedToCustomerId: Long?,
            grossMinutes: Int,
            amountPaidStr: String,
            discountAmountStr: String,
            wastedMinutes: Int,
            wastedReason: String,
            notes: String,
            pricePerHourStr: String,
            startTimeMillis: Long,
            quickName: String,
            quickPhone: String,
            quickIsWellOwner: Boolean
        ): SessionFormSnapshot = SessionFormSnapshot(
            customerId = customerId,
            customerInputText = customerInputText.trim(),
            billedToCustomerId = billedToCustomerId,
            grossMinutes = grossMinutes,
            amountPaid = Formatters.parseAmountInput(amountPaidStr),
            discountAmount = Formatters.parseAmountInput(discountAmountStr),
            wastedMinutes = wastedMinutes,
            wastedReason = wastedReason.trim(),
            notes = notes.trim(),
            pricePerHour = Formatters.parseAmountInput(pricePerHourStr),
            startTimeMinute = startTimeMillis / 60000L,
            quickName = quickName.trim(),
            quickPhone = quickPhone.trim(),
            quickIsWellOwner = quickIsWellOwner
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSessionBottomSheet(
    initialSession: WaterSession? = null,
    customers: List<Customer>,
    defaultCustomerId: Long? = null,
    // true when opened from a customer's screen: the customer is fixed and the picker hidden
    lockCustomer: Boolean = false,
    defaultPricePerHour: Double,
    currencySymbol: String,
    onCreateCustomer: suspend (Customer) -> Long,
    onDismiss: () -> Unit,
    voiceCommand: VoiceCommand? = null,
    onReSpeak: (() -> Unit)? = null,
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
    var activeTour by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(500)
        if (!GuidePreferences.isTourDone(context, GuidePreferences.TOUR_SESSION_FORM)) {
            activeTour = GuidePreferences.TOUR_SESSION_FORM
        }
    }

    var currentVoiceCommand by remember(voiceCommand) { mutableStateOf(voiceCommand) }
    val initialCustomerFromVoice = remember(voiceCommand, customers) {
        voiceCommand?.customerQuery?.let { q ->
            val ranked = CustomerMatcher.match(q, customers)
            if (CustomerMatcher.isHighConfidence(ranked)) {
                ranked.first().customer
            } else {
                null
            }
        }
    }
    var selectedCustomerId by remember(initialSession?.customerId, defaultCustomerId, initialCustomerFromVoice) {
        mutableLongStateOf(initialSession?.customerId ?: initialCustomerFromVoice?.id ?: (if (voiceCommand?.customerQuery != null) 0L else defaultCustomerId ?: 0L))
    }

    var quickName by remember { mutableStateOf("") }
    var quickPhone by remember { mutableStateOf("") }
    var quickIsWellOwner by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val contactUri = result.data?.data
            if (contactUri != null) {
                try {
                    val phoneUri = if (contactUri.toString().startsWith(ContactsContract.Contacts.CONTENT_URI.toString())) {
                        val contactId = contactUri.lastPathSegment
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI.buildUpon()
                            .appendQueryParameter(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, contactId)
                            .build()
                    } else {
                        contactUri
                    }

                    var cName: String? = null
                    var cNum: String? = null

                    context.contentResolver.query(
                        phoneUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        ),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIdx >= 0) cName = cursor.getString(nameIdx)
                            if (numIdx >= 0) cNum = cursor.getString(numIdx)
                        }
                    }

                    if (!cName.isNullOrBlank()) {
                        quickName = cName.orEmpty().trim()
                    }
                    if (!cNum.isNullOrBlank()) {
                        val clean = com.example.core.util.FileSharingHelper.convertArabicDigitsToAscii(cNum.orEmpty())
                            .replace(Regex("[^0-9+]"), "").trim()
                        quickPhone = clean
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "تعذر قراءة جهة الاتصال", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val legacyPumpName = initialSession?.pumpName.orEmpty()

    val selectedCustomer = customers.find { it.id == selectedCustomerId }
    val initialPrice = voiceCommand?.ratePerHour
        ?: initialSession?.pricePerHour
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
    val initialStartTime = voiceCommand?.startMillis ?: (initialSession?.startTime ?: (now - 3600000L))
    val initialEndTime = voiceCommand?.endMillis ?: (initialSession?.endTime ?: now)

    var startDateMillis by remember(initialSession?.id, formKey, voiceCommand) { mutableLongStateOf(initialStartTime) }
    var endDateMillis by remember(initialSession?.id, formKey, voiceCommand) { mutableLongStateOf(initialEndTime) }

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

    val initialVoiceDuration = voiceCommand?.durationMinutes
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val initialDurationMins = remember(initialSession?.id, formKey) {
        val diffMs = (initialSession?.endTime ?: now) - (initialSession?.startTime ?: (now - 3600000L))
        if (diffMs > 0) (diffMs / 60000L).toInt() else 60
    }
    var manualHoursStr by remember(initialSession?.id, formKey, voiceCommand) {
        mutableStateOf(
            if (initialVoiceDuration != null) {
                (initialVoiceDuration / 60).toString()
            } else {
                (initialDurationMins / 60).let { if (it > 0) it.toString() else "1" }
            }
        )
    }
    var manualMinutesStr by remember(initialSession?.id, formKey, voiceCommand) {
        mutableStateOf(
            if (initialVoiceDuration != null) {
                (initialVoiceDuration % 60).let { if (it > 0) it.toString() else "" }
            } else {
                (initialDurationMins % 60).let { if (it > 0) it.toString() else "" }
            }
        )
    }

    val isInitiallyOvernight = remember {
        val sTotal = startHour * 60 + startMinute
        val eTotal = endHour * 60 + endMinute
        isEndDayAfterStartDay(startDateMillis, endDateMillis) || eTotal < sTotal
    }
    var overnightNotice by remember {
        mutableStateOf<String?>(
            if (isInitiallyOvernight && isEndDayAfterStartDay(startDateMillis, endDateMillis)) {
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
            val newEndCal = Calendar.getInstance().apply { timeInMillis = sessionEndMillis(startCal.timeInMillis, totalM) }
            endDateMillis = newEndCal.timeInMillis
            endHour = newEndCal.get(Calendar.HOUR_OF_DAY)
            endMinute = newEndCal.get(Calendar.MINUTE)
            if (isEndDayAfterStartDay(startDateMillis, endDateMillis)) {
                overnightNotice = "ينتهي السقي في اليوم التالي (${Formatters.formatDate(endDateMillis)})"
            } else {
                overnightNotice = null
            }
        }
    }

    // من / إلى changed (or voice): show the new duration unless the fields already say the same total
    LaunchedEffect(startCal.timeInMillis, endCal.timeInMillis) {
        val shown = (manualHoursStr.toIntOrNull() ?: 0) * 60 + (manualMinutesStr.toIntOrNull() ?: 0)
        val gross = ((endCal.timeInMillis - startCal.timeInMillis) / 60000L).toInt()
        if (gross > 0 && gross != shown) {
            manualHoursStr = (gross / 60).toString()
            manualMinutesStr = if (gross % 60 > 0) (gross % 60).toString() else ""
        }
    }

    LaunchedEffect(voiceCommand) {
        if (voiceCommand?.durationMinutes != null) {
            applyManualDuration(manualHoursStr, manualMinutesStr)
        }
    }

    fun onStartTimeChanged(newHour: Int, newMinute: Int) {
        startHour = newHour
        startMinute = newMinute
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
            if (isEndDayAfterStartDay(startDateMillis, endDateMillis) && overnightNotice != null) {
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

    var amountPaidStr by remember(voiceCommand) {
        mutableStateOf(
            if (voiceCommand?.paid != null) Formatters.formatAmountInput(voiceCommand.paid.toString())
            else initialSession?.amountPaid?.let { if (it > 0) Formatters.formatAmountInput(it.toString()) else "" } ?: ""
        )
    }
    var notes by remember(voiceCommand) {
        mutableStateOf(voiceCommand?.notes ?: initialSession?.notes ?: "")
    }


    LaunchedEffect(effectiveFarmerCharge, currentVoiceCommand) {
        if (currentVoiceCommand?.paidFull == true && effectiveFarmerCharge > 0) {
            amountPaidStr = Formatters.formatAmountInput(effectiveFarmerCharge.toString())
        }
    }

    val amountPaid by remember { derivedStateOf { Formatters.parseAmountInput(amountPaidStr) } }
    val remainingDebt by remember(effectiveFarmerCharge, amountPaid, isBilledToWellOwner) {
        derivedStateOf {
            if (isBilledToWellOwner) 0.0 else Formatters.roundMoney(maxOf(0.0, effectiveFarmerCharge - amountPaid))
        }
    }

    // Keyed on the form only: re-keying on the selected customer reset the text on every key press.
    var customerInputText by remember(formKey) {
        mutableStateOf(selectedCustomer?.name ?: voiceCommand?.customerQuery ?: "")
    }
    // A customer chosen elsewhere (chooser, new customer, late-loaded list) shows its name once.
    LaunchedEffect(selectedCustomer?.id) {
        selectedCustomer?.let { if (customerInputText != it.name) customerInputText = it.name }
    }

    var pendingVoiceTarget by remember { mutableStateOf<String?>(null) }
    var showVoiceRecordSheet by remember { mutableStateOf(false) }
    var isVoiceCompact by remember { mutableStateOf(false) }
    var voiceRecordPrompt by remember { mutableStateOf("تحدث بالعملية...") }
    var unrecognizedSessionVoiceText by remember { mutableStateOf<String?>(null) }
    var showQuickCustomerDialog by remember { mutableStateOf(false) }

    fun launchVoiceFor(target: String) {
        pendingVoiceTarget = target
        isVoiceCompact = target != "form_command"
        voiceRecordPrompt = when (target) {
            "customer" -> "تحدث باسم العميل..."
            "price" -> "تحدث بسعر الساعة..."
            "paid" -> "تحدث بالمبلغ المدفوع..."
            "notes" -> "أملِ الملاحظة..."
            else -> "تحدث بالعملية..."
        }
        showVoiceRecordSheet = true
    }

    fun handleSessionVoiceResults(matches: List<String>) {
        val spoken = matches.firstOrNull()?.trim().orEmpty()
        VoiceRecognitionHelper.logDebug("VoiceInput", "SessionDialog voice target: $pendingVoiceTarget, spoken='$spoken'")
        if (spoken.isNotBlank()) {
            when (pendingVoiceTarget) {
                "price" -> {
                    val parsed = SpokenNumberParser.parseSpokenNumber(spoken)
                    if (parsed != null && parsed > 0) {
                        pricePerHourStr = Formatters.formatAmountInput(parsed.toString())
                    }
                }
                "paid" -> {
                    val parsed = SpokenNumberParser.parseSpokenNumber(spoken)
                    if (parsed != null && parsed >= 0) {
                        amountPaidStr = Formatters.formatAmountInput(parsed.toString())
                    }
                }
                "notes" -> {
                    notes = if (notes.isBlank()) spoken else "$notes $spoken"
                }
                "customer" -> {
                    val ranked = CustomerMatcher.match(spoken, customers)
                    val cust = if (CustomerMatcher.isHighConfidence(ranked)) ranked.first().customer else null
                    if (cust != null) {
                        selectedCustomerId = cust.id
                        customerInputText = cust.name
                        val cp = if (cust.isWellOwner) null else cust.customPricePerHour
                        if (cp != null && cp > 0) {
                            pricePerHourStr = Formatters.formatAmountInput(cp.toString())
                        }
                    } else {
                        customerInputText = spoken
                        showQuickCustomerDialog = true
                    }
                }
                "form_command" -> {
                    val knownCustomerNames = customers.map { it.name }
                    val cmd = VoiceRecognitionHelper.parseFirstValidCommand(matches, knownCustomerNames)
                    if (cmd != null && (cmd.durationMinutes != null || cmd.ratePerHour != null || cmd.paid != null || !cmd.customerQuery.isNullOrBlank())) {
                        currentVoiceCommand = cmd
                        if (cmd.startMillis != null && cmd.endMillis != null) {
                            startDateMillis = cmd.startMillis
                            endDateMillis = cmd.endMillis
                            val sCal = Calendar.getInstance().apply { timeInMillis = cmd.startMillis }
                            val eCal = Calendar.getInstance().apply { timeInMillis = cmd.endMillis }
                            startHour = sCal.get(Calendar.HOUR_OF_DAY)
                            startMinute = sCal.get(Calendar.MINUTE)
                            endHour = eCal.get(Calendar.HOUR_OF_DAY)
                            endMinute = eCal.get(Calendar.MINUTE)
                        } else if (cmd.durationMinutes != null && cmd.durationMinutes > 0) {
                            val h = (cmd.durationMinutes / 60).toString()
                            val m = (cmd.durationMinutes % 60).let { if (it > 0) it.toString() else "" }
                            manualHoursStr = h
                            manualMinutesStr = m
                            applyManualDuration(h, m)
                        }
                        if (cmd.ratePerHour != null && cmd.ratePerHour > 0) {
                            pricePerHourStr = Formatters.formatAmountInput(cmd.ratePerHour.toString())
                        }
                        if (cmd.paid != null && cmd.paid >= 0) {
                            amountPaidStr = Formatters.formatAmountInput(cmd.paid.toString())
                        }
                        if (!cmd.customerQuery.isNullOrBlank()) {
                            val ranked = CustomerMatcher.match(cmd.customerQuery, customers)
                            val cust = if (CustomerMatcher.isHighConfidence(ranked)) ranked.first().customer else null
                            if (cust != null) {
                                selectedCustomerId = cust.id
                                customerInputText = cust.name
                            } else {
                                customerInputText = cmd.customerQuery.trim()
                                showQuickCustomerDialog = true
                            }
                        }
                        if (!cmd.notes.isNullOrBlank()) {
                            notes = cmd.notes
                        }
                    } else {
                        unrecognizedSessionVoiceText = spoken
                    }
                }
            }
        }
        pendingVoiceTarget = null
    }

    var voiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }
    var showAddCustomerSheet by remember { mutableStateOf(false) }
    var customerSaveError by remember { mutableStateOf<String?>(null) }
    var showWastedDetails by remember { mutableStateOf(wastedTotalMinutes > 0) }
    var showDiscountDetails by remember { mutableStateOf(discountAmount > 0) }

    val currentSnapshot = SessionFormSnapshot.from(
        customerId = selectedCustomerId,
        customerInputText = customerInputText,
        billedToCustomerId = billedToCustomerId,
        grossMinutes = grossMinutes,
        amountPaidStr = amountPaidStr,
        discountAmountStr = discountAmountStr,
        wastedMinutes = wastedTotalMinutes,
        wastedReason = wastedReason,
        notes = notes,
        pricePerHourStr = pricePerHourStr,
        startTimeMillis = startCal.timeInMillis,
        quickName = quickName,
        quickPhone = quickPhone,
        quickIsWellOwner = quickIsWellOwner
    )
    val initialSnapshot = remember { currentSnapshot }
    val hasUnsavedChanges by remember(currentSnapshot, initialSnapshot) {
        derivedStateOf { (currentSnapshot != initialSnapshot) || (currentVoiceCommand != null) }
    }

    val trimmedInput = customerInputText.trim()
    val matchedCust = customers.find { it.name.trim().equals(trimmedInput, ignoreCase = true) }
    val finalCustId = if (selectedCustomerId > 0) selectedCustomerId else (matchedCust?.id ?: 0L)
    val canSaveSession = finalCustId > 0 && grossMinutes in 1..MAX_SESSION_MINUTES && wastedTotalMinutes <= grossMinutes

    var showSaveConfirm by remember { mutableStateOf(false) }
    val performSave = {
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

    if (showSaveConfirm) {
        val who = selectedCustomer?.name ?: matchedCust?.name.orEmpty()
        OperationConfirmSheet(
            statement = OperationStatement.session(who, startCal.timeInMillis, endCal.timeInMillis, grossMinutes, finalFarmerCharge, amountPaid, remainingDebt, currencySymbol),
            spoken = OperationStatement.sessionSpoken(who, startCal.timeInMillis, endCal.timeInMillis, grossMinutes, finalFarmerCharge, amountPaid, remainingDebt, currencySymbol),
            onConfirm = { showSaveConfirm = false; performSave() },
            onEdit = { showSaveConfirm = false }
        )
    }

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = if (initialSession == null) "تسجيل دورة سقي جديدة" else "تعديل دورة سقي #${initialSession.id}",
        subtitle = selectedCustomer?.name,
        icon = Icons.Default.WaterDrop,
        hasUnsavedChanges = hasUnsavedChanges,
        headerActions = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                GuideHelpButton(onClick = { activeTour = GuidePreferences.TOUR_SESSION_FORM })
                VoiceFieldMic(
                    onClick = {
                        if (onReSpeak != null) {
                            onReSpeak()
                        } else {
                            launchVoiceFor("form_command")
                        }
                    },
                    modifier = Modifier.guideTarget("session_voice_command")
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
                text = if (initialSession == null) "حفظ السقي" else "حفظ التعديل",
                onClick = {
                    if (finalCustId == 0L && trimmedInput.isNotBlank()) {
                        showQuickCustomerDialog = true
                        return@MosaribPrimaryButton
                    }

                    if (finalCustId > 0 && grossMinutes in 1..MAX_SESSION_MINUTES && wastedTotalMinutes <= grossMinutes) {
                        if (endCal.timeInMillis < startCal.timeInMillis) {
                            Toast.makeText(context, "لا يمكن اختيار تاريخ أو وقت انتهاء قبل تاريخ ووقت البدء", Toast.LENGTH_SHORT).show()
                            return@MosaribPrimaryButton
                        }
                        if ((initialSession?.id ?: 0L) == 0L) showSaveConfirm = true else performSave()
                    }
                },
                enabled = canSaveSession,
                modifier = Modifier.weight(1f).guideTarget("session_save")
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
            verticalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            if (currentVoiceCommand != null) {
                VoiceSummaryBanner(
                    command = currentVoiceCommand!!,
                    resolvedCustomerName = selectedCustomer?.name,
                    onReSpeak = onReSpeak ?: { launchVoiceFor("form_command") }
                )
            }

            // اختيار العميل من القائمة فقط (لا كتابة في الحقل)
            if (lockCustomer && selectedCustomer != null) {
                // Opened from the customer's own screen: no picker needed
                Surface(
                    modifier = Modifier.fillMaxWidth().guideTarget("session_customer"),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedCustomer.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            
            } else {
                CustomerPickerField(
                    customers = customers,
                    selected = selectedCustomer,
                    onSelect = { c ->
                        selectedCustomerId = c.id
                        customerInputText = c.name
                        val customerPrice = if (c.isWellOwner) null else c.customPricePerHour
                        pricePerHourStr = Formatters.formatAmountInput((customerPrice ?: defaultPricePerHour).toString())
                    },
                    onAddNew = {
                        customerSaveError = null
                        quickName = if (selectedCustomerId > 0L) "" else customerInputText.trim()
                        showQuickCustomerDialog = true
                    },
                    trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("customer") }, modifier = Modifier.guideTarget("session_mic")) },
                    modifier = Modifier
                        .guideTarget("session_customer")
                        .testTag("session_customer_picker")
                )
                if (selectedCustomerId <= 0L && customerInputText.isNotBlank()) {
                    Text(
                        text = "لم أتعرّف على: ${customerInputText.trim()} — اختر من القائمة أو أضفه",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.warning),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }


            // إرشاد احتساب السقي على صاحب بئر (يظهر فقط مع خيار صاحب البئر)
            if (selectedCustomer?.isWellOwner == true) {
                FirstTimeHint(
                    id = GuideTexts.HINT_SESSION_WELL_OWNER,
                    text = GuideTexts.HINT_SESSION_WELL_OWNER_TEXT
                )
            }


            // بطاقة تحديد وقت السقي بنظام التبويبين (يدوي / اختيار)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    run {
                        Column {

                            // بطاقتا الوقت والتاريخ (من ──▶ إلى)
                            Row(
                                modifier = Modifier.fillMaxWidth().guideTarget("session_time_choice"),
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
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "من",
                                            style = MaterialTheme.typography.labelSmall.copy(color = PrimaryTeal, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryTeal.copy(alpha = 0.08f))
                                                .clickable { showStartTimePicker = true }
                                                .padding(vertical = 2.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = Formatters.formatClockTimeArabic(startHour, startMinute),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
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
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
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
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            text = "إلى",
                                            style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(AccentEmerald.copy(alpha = 0.08f))
                                                .clickable { showEndTimePicker = true }
                                                .padding(vertical = 2.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = Formatters.formatClockTimeArabic(endHour, endMinute),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
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
                                                tint = if (overnightNotice != null) MaterialTheme.mosarib.info else Color.Gray,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = Formatters.formatDate(endDateMillis),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (overnightNotice != null) MaterialTheme.mosarib.info else Color.Gray,
                                                    fontWeight = if (overnightNotice != null) FontWeight.Bold else FontWeight.Normal
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
                                    modifier = Modifier.align(Alignment.End),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.mosarib.info.copy(alpha = 0.12f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.mosarib.info.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isEndNight) Icons.Default.DarkMode else Icons.Default.WbSunny,
                                            contentDescription = null,
                                            tint = if (isEndNight) (MaterialTheme.mosarib.info) else (if (isDark) MaterialTheme.mosarib.warning else MaterialTheme.mosarib.warning),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = dayDescription ?: overnightNotice.orEmpty(),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.mosarib.info,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    // المدة قابلة للتعديل: تغييرها يحرّك «إلى» = «من» + المدة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المدة:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        CompactDurationField(
                            value = manualHoursStr,
                            unit = "ساعات",
                            onValueChange = {
                                manualHoursStr = it.filter { c -> c.isDigit() }.trimStart('0').take(3)
                                applyManualDuration(manualHoursStr, manualMinutesStr)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        CompactDurationField(
                            value = manualMinutesStr,
                            unit = "دقائق",
                            onValueChange = {
                                val filtered = it.filter { c -> c.isDigit() }.trimStart('0')
                                if ((filtered.toIntOrNull() ?: 0) < 60) {
                                    manualMinutesStr = filtered
                                    applyManualDuration(manualHoursStr, manualMinutesStr)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (isDurationExceeded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.mosarib.debt.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.mosarib.debt.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.mosarib.debt,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "الحد الأقصى المسموح به للدورة الواحدة هو 10 أيام (240 ساعة).",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.mosarib.debt,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }


            // الواصل (المدفوع الآن)
            val isFullPaid = finalFarmerCharge > 0 && amountPaid >= finalFarmerCharge
            val excessAmount = if (!isBilledToWellOwner && amountPaid > finalFarmerCharge && finalFarmerCharge > 0) {
                Formatters.roundMoney(amountPaid - finalFarmerCharge)
            } else 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
            // سعر الساعة (مباشرة تحت تحديد الساعات بتصميم أخف وأصغر وبحواشي مقللة)
            Column(modifier = Modifier.weight(1f).guideTarget("session_price")) {
                MosaribTextField(
                    value = pricePerHourStr,
                    onValueChange = { pricePerHourStr = Formatters.formatAmountInput(it) },
                    label = "سعر الساعة *",
                    trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("price") }) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isDense = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pricePerHour > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.amountToArabicWords(pricePerHour, currencySymbol),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "الواصل",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                    if (!isBilledToWellOwner && finalFarmerCharge > 0) {
                        Surface(
                            modifier = Modifier
                                .guideTarget("session_full_paid")
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    amountPaidStr = if (isFullPaid) "" else Formatters.formatAmountInput(finalFarmerCharge.toString())
                                },
                            color = if (isFullPaid) AccentEmerald.copy(alpha = 0.12f) else (MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, if (isFullPaid) AccentEmerald else (MaterialTheme.colorScheme.outlineVariant)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isFullPaid) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
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
                }

                MosaribTextField(
                    value = amountPaidStr,
                    onValueChange = { amountPaidStr = Formatters.formatAmountInput(it) },
                    placeholder = "0",
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VoiceFieldMic(onClick = { launchVoiceFor("paid") })
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .guideTarget("session_paid")
                        .testTag("session_paid_input")
                )

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
            }
            }

            // بطاقة الحسابات الميدانية الشاملة
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else PrimaryTeal.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else PrimaryTeal.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // المدة الكلية والصافي تظهران فقط عند وجود هدر
                    if (wastedTotalMinutes > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("المدة الكلية", style = MaterialTheme.typography.bodyMedium)
                            Text(Formatters.formatDurationArabic(grossMinutes), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("الهدر", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.warning))
                            Text("- ${Formatters.formatDurationArabic(wastedTotalMinutes)}", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.warning, fontWeight = FontWeight.Bold))
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
                        Spacer(modifier = Modifier.height(Spacing.s))
                        HorizontalDivider(color = PrimaryTeal.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(Spacing.s))
                    }

                    if (isBilledToWellOwner) {
                        val ownerName = wellOwners.find { it.id == billedToCustomerId }?.name ?: "صاحب البئر"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.mosarib.info.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.mosarib.info.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.mosarib.info, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مسجل على حساب صاحب البئر:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.info, fontWeight = FontWeight.Bold))
                            }
                            Text(ownerName, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.info, fontWeight = FontWeight.Bold))
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
                                Text("المحتسب على صاحب البئر:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.info))
                                Text(Formatters.formatCurrency(finalFarmerCharge, currencySymbol), style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.mosarib.info, fontWeight = FontWeight.Bold))
                            }
                            if (finalFarmerCharge > 0) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    Formatters.amountToArabicWords(finalFarmerCharge, currencySymbol),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.mosarib.info,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    } else {
                        // المطلوب | المتبقي عليه (الواصل يظهر في حقله)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("المطلوب", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                Text(Formatters.formatCurrency(finalFarmerCharge, currencySymbol), style = MaterialTheme.typography.titleMedium.copy(color = PrimaryTeal, fontWeight = FontWeight.ExtraBold))
                                if (finalFarmerCharge > 0) {
                                    Text(
                                        Formatters.amountToArabicWords(finalFarmerCharge, currencySymbol),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "المتبقي عليه",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (remainingDebt > 0) MaterialTheme.mosarib.debt else AccentEmerald
                                    )
                                )
                                Text(
                                    if (remainingDebt > 0) Formatters.formatCurrency(remainingDebt, currencySymbol) else "خالص",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (remainingDebt > 0) MaterialTheme.mosarib.debt else AccentEmerald,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                                if (remainingDebt > 0) {
                                    Text(
                                        Formatters.amountToArabicWords(remainingDebt, currencySymbol),
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.debt, fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column {

                // إشعار الفائض النقدي وسند القبض المباشر
                if (excessAmount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        color = MaterialTheme.mosarib.credit.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.mosarib.credit.copy(alpha = 0.4f)),
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
                                        color = MaterialTheme.mosarib.credit
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "سيتم تلقائياً إصدار سند قبض مباشر بالفائض (${Formatters.formatCurrency(excessAmount, currencySymbol)}) لتسديد ديون العميل السابقة إن وُجدت، أو قيده كرصيد دائن لصالحه عند الحفظ.",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.mosarib.credit
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
                        .background(MaterialTheme.mosarib.info.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.mosarib.info.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("المعاملة المحاسبية:", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.info))
                    Text("مقاصة تخصم من مستحقات الشراء", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.mosarib.info, fontWeight = FontWeight.Bold))
                }
            }

            // بطاقة منسدلة في الأخير: خيارات إضافية (الوقت المهدور والخصم)
            var showExtraOptions by remember {
                mutableStateOf(wastedTotalMinutes > 0 || discountAmount > 0 || billedToCustomerId != null || notes.isNotBlank())
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .guideTarget("session_extra")
                    .clip(RoundedCornerShape(12.dp)),
                color = when {
                    wastedTotalMinutes > 0 || discountAmount > 0 -> MaterialTheme.colorScheme.surfaceVariant
                    isDark -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        wastedTotalMinutes > 0 -> MaterialTheme.mosarib.warning.copy(alpha = 0.4f)
                        discountAmount > 0 -> MaterialTheme.mosarib.credit.copy(alpha = 0.4f)
                        isDark -> MaterialTheme.colorScheme.outlineVariant
                        else -> MaterialTheme.colorScheme.outlineVariant
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
                                text = "خيارات إضافية",
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
                                    color = MaterialTheme.mosarib.warning.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.mosarib.warning.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "هدر: ${Formatters.formatDurationArabic(wastedTotalMinutes)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.mosarib.warning,
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
                                    color = MaterialTheme.mosarib.credit.copy(alpha = 0.12f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.mosarib.credit.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "خصم: ${Formatters.formatCurrency(discountAmount, currencySymbol)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.mosarib.credit,
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

            // خيار احتساب السقي على صاحب بئر (مقاصة من مستحقاته)
            if (selectedCustomer?.isWellOwner == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = MaterialTheme.mosarib.info.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.mosarib.info.copy(alpha = 0.4f)),
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
                            tint = MaterialTheme.mosarib.info,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حساب صاحب بئر: يُحتسب هذا السقي مقاصة تلقائية وتُخصم قيمته من مستحقات مشتريات الساعات لديه.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.mosarib.info,
                                
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
                        MaterialTheme.mosarib.info.copy(alpha = 0.12f)
                    } else {
                        if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                    border = BorderStroke(1.dp, if (billedToCustomerId != null) {
                        MaterialTheme.mosarib.info.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
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
                                                MaterialTheme.mosarib.info
                                            } else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "احتساب السقي على صاحب بئر",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (billedToCustomerId != null) {
                                                    MaterialTheme.mosarib.info
                                                } else MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                Text(
                                    text = "تُخصم القيمة كتسديد من مستحقات صاحب البئر لدى المسرب",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                MosaribMenu(
                                    expanded = billedToOwnerDropdownExpanded,
                                    onDismissRequest = { billedToOwnerDropdownExpanded = false }
                                ) {
                                    wellOwners.forEach { owner ->
                                        MosaribMenuItem(
                                            text = owner.name,
                                            icon = Icons.Default.Person,
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

                        Spacer(modifier = Modifier.height(12.dp))
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
                                color = MaterialTheme.mosarib.debt.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.mosarib.debt.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.mosarib.debt,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "لا يمكن أن يكون الهدر أكبر من مدة السقي!",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.mosarib.debt,
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
                                color = MaterialTheme.mosarib.warning.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.mosarib.warning.copy(alpha = 0.5f),
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
                                tint = MaterialTheme.mosarib.warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تسوية وتصفير باقي الجلسة كخصم:",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.mosarib.warning
                                )
                            )
                        }
                        Text(
                            text = Formatters.formatCurrency(diffToSettle, currencySymbol),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.mosarib.warning
                            )
                        )
                    }
                }
                        if (discountAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.amountToArabicWords(discountAmount, currencySymbol),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.mosarib.credit,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        MosaribTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "ملاحظات",
                            trailingIcon = { VoiceFieldMic(onClick = { launchVoiceFor("notes") }) },
                            modifier = Modifier.fillMaxWidth().guideTarget("session_notes")
                        )
                    }
                }
            }
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
            initialName = if (selectedCustomerId > 0L) "" else customerInputText.trim(),
            currencySymbol = currencySymbol,
            onDismiss = { showAddCustomerSheet = false },
            onSave = { _, name, phone, farmName, location, customerNotes, customPrice, isBeneficiary, isWellOwner, customGroup ->
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
                                isWellOwner = isWellOwner,
                                customGroup = customGroup.trim()
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
        var isQuickSubmitting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isQuickSubmitting) showQuickCustomerDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                text = "تسجيل العميل واختياره فوراً لمتابعة السقي",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    MosaribTextField(
                        value = quickName,
                        onValueChange = { quickName = it },
                        label = "اسم العميل *",
                        placeholder = "اكتب اسم العميل...",
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
                        modifier = Modifier.fillMaxWidth(),
                        isDense = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    MosaribTextField(
                        value = quickPhone,
                        onValueChange = { quickPhone = it },
                        label = "رقم الهاتف (اختياري)",
                        placeholder = "777xxxxxx",
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                contactPickerLauncher.launch(intent)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = "جهات الاتصال",
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        isDense = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "هل هذا مورد (صاحب بئر)؟",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Switch(checked = quickIsWellOwner, onCheckedChange = { quickIsWellOwner = it })
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
                            text = "حفظ وتحديد العميل",
                            isLoading = isQuickSubmitting,
                            enabled = quickName.isNotBlank() && !isQuickSubmitting,
                            onClick = {
                                if (quickName.isBlank() || isQuickSubmitting) return@MosaribButton
                                isQuickSubmitting = true
                                coroutineScope.launch {
                                    try {
                                        val newCustomer = Customer(
                                            name = quickName.trim(),
                                            phone = quickPhone.trim(),
                                            isWellOwner = quickIsWellOwner
                                        )
                                        val newId = onCreateCustomer(newCustomer)
                                        selectedCustomerId = newId
                                        customerInputText = quickName.trim()
                                        val customerPrice = if (quickIsWellOwner) null else newCustomer.customPricePerHour
                                        pricePerHourStr = Formatters.formatAmountInput((customerPrice ?: defaultPricePerHour).toString())
                                        showQuickCustomerDialog = false
                                        Toast.makeText(context, "تم حفظ وتحديد: ${quickName.trim()}", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        isQuickSubmitting = false
                                        Toast.makeText(context, "فشل حفظ العميل: ${e.message}", Toast.LENGTH_SHORT).show()
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

    if (showVoiceRecordSheet) {
        VoiceRecordSheet(
            title = if (isVoiceCompact) "إملاء صوتي" else "الإدخال الصوتي للسقي",
            subtitle = voiceRecordPrompt,
            compact = isVoiceCompact,
            onDismiss = { showVoiceRecordSheet = false },
            onResult = { candidates ->
                showVoiceRecordSheet = false
                handleSessionVoiceResults(candidates)
            }
        )
    }

    unrecognizedSessionVoiceText?.let { spoken ->
        VoiceUnrecognizedDialog(
            spokenText = spoken,
            onReSpeak = {
                unrecognizedSessionVoiceText = null
                launchVoiceFor("form_command")
            },
            onShowExamples = {
                unrecognizedSessionVoiceText = null
            },
            onDismiss = { unrecognizedSessionVoiceText = null }
        )
    }

    VoiceSupportDialogs(
        dialogType = voiceDialogType,
        onDismiss = { voiceDialogType = VoiceDialogType.NONE }
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
                    
                )
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "انقر للتعديل",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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



/** ~44dp numeric field for the duration row; empty shows «0». */
@Composable
private fun CompactDurationField(
    value: String,
    unit: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.text.BasicTextField(
        value = value.ifEmpty { "0" },
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = textColor),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) { inner() }
                Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

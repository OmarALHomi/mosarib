package com.example.features.wellowners

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribDatePickerDialog
import com.example.core.ui.MosaribOutlinedButton
import com.example.core.ui.MosaribTextField
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.ui.theme.PrimaryTeal

import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellOwnerPurchaseBottomSheet(
    owner: Customer? = null,
    availableOwners: List<Customer> = emptyList(),
    currencySymbol: String,
    initialPurchase: WellOwnerPurchase? = null,
    onDismiss: () -> Unit,
    onSaveWithOwner: ((ownerId: Long, date: Long, durationMinutes: Int, wastedMinutesOnOwner: Int, purchaseRatePerHour: Double, amountPaid: Double, notes: String) -> Unit)? = null,
    onSave: ((date: Long, durationMinutes: Int, wastedMinutesOnOwner: Int, purchaseRatePerHour: Double, amountPaid: Double, notes: String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isAppDark()
    val now = System.currentTimeMillis()
    var date by remember(initialPurchase) { mutableLongStateOf(initialPurchase?.date ?: now) }
    var showDatePicker by remember { mutableStateOf(false) }

    val ownersList = remember(availableOwners, owner) {
        if (owner != null) listOf(owner) else availableOwners.filter { it.isWellOwner }
    }
    var selectedOwner by remember(owner, ownersList) {
        mutableStateOf(owner ?: ownersList.firstOrNull())
    }
    var ownerDropdownExpanded by remember { mutableStateOf(false) }

    var hoursText by remember(initialPurchase) {
        mutableStateOf(initialPurchase?.let { (it.durationMinutes / 60).toString() } ?: "")
    }
    var minutesText by remember(initialPurchase) {
        mutableStateOf(initialPurchase?.let { (it.durationMinutes % 60).toString() } ?: "")
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

    val durationMinutes by remember(hoursText, minutesText) {
        derivedStateOf {
            (hoursText.toIntOrNull()?.coerceAtLeast(0) ?: 0) * 60 +
                (minutesText.toIntOrNull()?.coerceIn(0, 59) ?: 0)
        }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (initialPurchase != null) "تعديل شراء ساعات" else "شراء ساعات",
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

            // زر تحديد التاريخ باستخدام النافذة المخصصة
            MosaribOutlinedButton(
                text = "تاريخ الشراء: ${Formatters.formatDate(date)}",
                onClick = { showDatePicker = true },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryTeal) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MosaribTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter(Char::isDigit).take(4) },
                    label = "الساعات المشتراة *",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                MosaribTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                    label = "الدقائق",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

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
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الهدر أكبر من مدة الشراء (${Formatters.formatDurationArabic(durationMinutes)})!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            MosaribTextField(
                value = rateText,
                onValueChange = { rateText = Formatters.formatAmountInput(it) },
                label = "سعر الساعة ($currencySymbol) *",
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryTeal) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MosaribTextField(
                    value = amountPaidText,
                    onValueChange = { amountPaidText = Formatters.formatAmountInput(it) },
                    label = "المدفوع نقدياً ($currencySymbol)",
                    placeholder = "0",
                    leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryTeal) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                if (payableAmount > 0) {
                    val isFullPaid = amountPaid >= payableAmount
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                amountPaidText = if (isFullPaid) "" else Formatters.formatAmountInput(payableAmount.toString())
                            },
                        color = if (isFullPaid) PrimaryTeal.copy(alpha = 0.12f) else (if (isDark) Color(0xFF141F25) else Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, if (isFullPaid) PrimaryTeal else (if (isDark) Color(0xFF2A3C45) else Color(0xFFCBD5E1))),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(
                                checked = isFullPaid,
                                onCheckedChange = { checked ->
                                    amountPaidText = if (checked) Formatters.formatAmountInput(payableAmount.toString()) else ""
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دفع كامل",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFullPaid) PrimaryTeal else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF141F25) else PrimaryTeal.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2A3C45) else PrimaryTeal.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المدة")
                        Text(Formatters.formatDurationArabic(durationMinutes), fontWeight = FontWeight.SemiBold)
                    }
                    if (wastedMinutes > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الهدر", color = if (isDark) Color(0xFFF87171) else Color(0xFFD32F2F))
                            Text("− ${Formatters.formatDurationArabic(wastedMinutes)}", color = if (isDark) Color(0xFFF87171) else Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الصافي المحتسب", fontWeight = FontWeight.Bold)
                        Text(Formatters.formatDurationArabic(chargeableMinutes), color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    }
                    if (wasteCredit > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("خصم الهدر")
                            Text(Formatters.formatCurrency(wasteCredit, currencySymbol), color = if (isDark) Color(0xFFF87171) else Color(0xFFD32F2F))
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المستحق", fontWeight = FontWeight.Bold)
                        Text(Formatters.formatCurrency(payableAmount, currencySymbol), color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    }
                    if (amountPaid > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المدفوع", color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                            Text(Formatters.formatCurrency(amountPaid, currencySymbol), color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = PrimaryTeal.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المتبقي", fontWeight = FontWeight.ExtraBold)
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
                label = "ملاحظات (اختياري)",
                leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth()
            )

            val curOwner = selectedOwner
            val canSave = valid && curOwner != null
            MosaribButton(
                text = if (initialPurchase != null) "حفظ التعديلات" else "حفظ عملية الشراء",
                onClick = {
                    if (canSave) {
                        onSaveWithOwner?.invoke(curOwner!!.id, date, durationMinutes, wastedMinutes, rate, amountPaid, notes)
                        onSave?.invoke(date, durationMinutes, wastedMinutes, rate, amountPaid, notes)
                    }
                },
                enabled = canSave,
                leadingIcon = { Icon(if (initialPurchase != null) Icons.Default.Check else Icons.Default.AccessTime, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        MosaribDatePickerDialog(
            initialTimestamp = date,
            title = "تحديد تاريخ الشراء",
            onDismiss = { showDatePicker = false },
            onDateSelected = { newDate ->
                date = newDate
                showDatePicker = false
            }
        )
    }
}

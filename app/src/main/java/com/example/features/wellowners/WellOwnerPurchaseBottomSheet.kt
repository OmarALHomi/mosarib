package com.example.features.wellowners

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.ui.theme.PrimaryTeal
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellOwnerPurchaseBottomSheet(
    owner: Customer,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (date: Long, durationMinutes: Int, wastedMinutesOnOwner: Int, purchaseRatePerHour: Double, amountPaid: Double, notes: String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val now = System.currentTimeMillis()
    var date by remember { mutableLongStateOf(now) }
    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var wastedHoursText by remember { mutableStateOf("") }
    var wastedMinutesText by remember { mutableStateOf("") }
    var amountPaidText by remember { mutableStateOf("") }
    var rateText by remember(owner.id, owner.customPricePerHour) {
        mutableStateOf(
            owner.customPricePerHour
                ?.takeIf { it > 0 }
                ?.let { Formatters.formatAmountInput(it.toString()) }
                ?: ""
        )
    }
    var notes by remember { mutableStateOf("") }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("شراء ساعات من صاحب البئر", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(owner.name, style = MaterialTheme.typography.bodyMedium, color = PrimaryTeal, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                }
            }
            Text(
                "عملية شراء مستقلة عن سقي المزارعين؛ السداد يُسجّل من كشف صاحب البئر.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = date }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            date = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("تاريخ الشراء: ${Formatters.formatDate(date)}")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter(Char::isDigit).take(4) },
                    label = { Text("الساعات المشتراة *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit).take(2) },
                    label = { Text("الدقائق") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = wastedHoursText,
                    onValueChange = { wastedHoursText = it.filter(Char::isDigit).take(4) },
                    label = { Text("هدر على صاحب البئر (ساعات)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = wastedMinutesText,
                    onValueChange = { wastedMinutesText = it.filter(Char::isDigit).take(2) },
                    label = { Text("الهدر (دقائق)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
            if (isWasteExceeded) {
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
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "❌ لا يمكن أن يكون الهدر أكبر من عملية الشراء بتاتاً! (المدة المشتراة: ${Formatters.formatDurationArabic(durationMinutes)})",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB91C1C), fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = rateText,
                onValueChange = { rateText = Formatters.formatAmountInput(it) },
                label = { Text("سعر ساعة الشراء ($currencySymbol) *") },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = amountPaidText,
                onValueChange = { amountPaidText = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ المسلَّم نقدياً فوراً لصاحب البئر ($currencySymbol)") },
                placeholder = { Text("0") },
                leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = PrimaryTeal) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryTeal.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المدة المسجلة")
                        Text(Formatters.formatDurationArabic(durationMinutes), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("هدر محسوب لصاحب البئر", color = Color(0xFFD32F2F))
                        Text("− ${Formatters.formatDurationArabic(wastedMinutes)}", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الساعات المحتسبة عليه", fontWeight = FontWeight.Bold)
                        Text(Formatters.formatDurationArabic(chargeableMinutes), color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("قيمة الهدر المخصومة")
                        Text(Formatters.formatCurrency(wasteCredit, currencySymbol), color = Color(0xFFD32F2F))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("المستحق الإجمالي لصاحب البئر", fontWeight = FontWeight.Bold)
                        Text(Formatters.formatCurrency(payableAmount, currencySymbol), color = PrimaryTeal, fontWeight = FontWeight.Bold)
                    }
                    if (amountPaid > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("المبلغ المسلَّم نقدياً", color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                            Text(Formatters.formatCurrency(amountPaid, currencySymbol), color = Color(0xFF15803D), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(color = PrimaryTeal.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 2.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الباقي لصالح صاحب البئر", fontWeight = FontWeight.ExtraBold)
                        Text(
                            Formatters.formatCurrency(remainingDueToOwner, currencySymbol),
                            color = if (remainingDueToOwner > 0) Color(0xFFB91C1C) else Color(0xFF15803D),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات (اختياري)") },
                leadingIcon = { Icon(Icons.Default.WaterDrop, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (valid) onSave(date, durationMinutes, wastedMinutes, rate, amountPaid, notes)
                },
                enabled = valid,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("حفظ عملية الشراء", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

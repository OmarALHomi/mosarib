package com.example.features.customers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribSecondaryButton
import com.example.core.ui.MosaribTextField
import com.example.core.util.Formatters
import com.example.ui.theme.mosarib

@Composable
fun AddReceiptBottomSheet(
    customer: Customer,
    currentBalance: Double,
    currencySymbol: String,
    isWellOwner: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String) -> Unit
) {
    val initialAmount = if (currentBalance > 0) currentBalance.toString() else ""
    var amountStr by remember { mutableStateOf(initialAmount) }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    val hasUnsavedChanges = remember(amountStr, paymentMethod, notes, initialAmount) {
        amountStr != initialAmount || paymentMethod != "نقداً" || notes.isNotBlank()
    }

    val parsedAmt = Formatters.parseAmountInput(amountStr)

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isWellOwner) "تحصيل من صاحب البئر عن السقي" else "تسجيل سند قبض من العميل",
        subtitle = "${if (isWellOwner) "صاحب البئر" else "العميل"}: ${customer.name}",
        icon = Icons.Default.Payments,
        iconTint = MaterialTheme.mosarib.success,
        hasUnsavedChanges = hasUnsavedChanges,
        footer = {
            MosaribSecondaryButton(
                text = "إلغاء",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )

            MosaribPrimaryButton(
                text = if (isWellOwner) "تسجيل التحصيل" else "حفظ سند القبض",
                onClick = {
                    if (parsedAmt > 0) {
                        onConfirm(parsedAmt, paymentMethod, notes)
                        onDismiss()
                    }
                },
                enabled = parsedAmt > 0,
                tone = DialogTone.Success,
                modifier = Modifier
                    .weight(2f)
                    .testTag("save_receipt_button")
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${when {
                    isWellOwner && currentBalance > 0 -> "المستحق منه عن السقي"
                    isWellOwner && currentBalance < 0 -> "رصيد له عن السقي"
                    isWellOwner -> "رصيد السقي"
                    currentBalance > 0 -> "الرصيد المستحق حالياً"
                    currentBalance < 0 -> "رصيد دائن للعميل"
                    else -> "الرصيد المستحق حالياً"
                }}: ${Formatters.formatCurrency(kotlin.math.abs(currentBalance), currencySymbol)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (currentBalance > 0) MaterialTheme.mosarib.debt else MaterialTheme.mosarib.success,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            MosaribTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = "${if (isWellOwner) "المبلغ المحصل منه" else "المبلغ المقبوض"} ($currencySymbol) *",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_amount_input")
            )

            if (parsedAmt > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedAmt, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.mosarib.success,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MosaribTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = "طريقة الدفع",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            MosaribTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "ملاحظات السند",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AddDisbursementBottomSheet(
    customer: Customer,
    currencySymbol: String,
    isWellOwner: Boolean = false,
    currentBalance: Double = 0.0,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, description: String) -> Unit
) {
    val initialAmount = if (isWellOwner && currentBalance > 0) currentBalance.toString() else ""
    var amountStr by remember { mutableStateOf(initialAmount) }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var description by remember { mutableStateOf("") }

    val hasUnsavedChanges = remember(amountStr, paymentMethod, description, initialAmount) {
        amountStr != initialAmount || paymentMethod != "نقداً" || description.isNotBlank()
    }

    val parsedAmt = Formatters.parseAmountInput(amountStr)

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isWellOwner) "سداد مستحقات صاحب البئر" else "تسجيل سند صرف مالي",
        subtitle = "${if (isWellOwner) "يُسدد لصاحب البئر" else "يُصرف للعميل"}: ${customer.name}",
        icon = Icons.Default.ArrowDownward,
        iconTint = MaterialTheme.mosarib.debt,
        hasUnsavedChanges = hasUnsavedChanges,
        footer = {
            MosaribSecondaryButton(
                text = "إلغاء",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )

            MosaribPrimaryButton(
                text = if (isWellOwner) "تسجيل السداد" else "حفظ سند الصرف",
                onClick = {
                    if (parsedAmt > 0) {
                        onConfirm(parsedAmt, paymentMethod, description)
                        onDismiss()
                    }
                },
                enabled = parsedAmt > 0,
                tone = DialogTone.Danger,
                modifier = Modifier
                    .weight(2f)
                    .testTag("save_disbursement_button")
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isWellOwner) {
                Text(
                    text = "${when {
                        currentBalance > 0 -> "مستحقات الشراء المتبقية"
                        currentBalance < 0 -> "رصيد لك عند صاحب البئر"
                        else -> "مستحقات الشراء مسددة"
                    }}: ${Formatters.formatCurrency(kotlin.math.abs(currentBalance), currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (currentBalance > 0) MaterialTheme.mosarib.debt else MaterialTheme.mosarib.success,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
            Text(
                text = if (isWellOwner) "سيُخصم هذا السداد من صافي رصيد مستحقات صاحب البئر." else "سيتم قيد هذا المبلغ في كشف الحساب ويحدث تغييراً في رصيد العميل",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(14.dp))

            MosaribTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = "${if (isWellOwner) "المبلغ المسدد لصاحب البئر" else "المبلغ المصروف"} ($currencySymbol) *",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("disbursement_amount_input")
            )

            if (parsedAmt > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedAmt, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.mosarib.debt,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            MosaribTextField(
                value = description,
                onValueChange = { description = it },
                label = "البيان / سبب الصرف *",
                placeholder = if (isWellOwner) "مثال: دفعة من مستحقات شراء الساعات..." else "مثال: دفعة نقدية، سلفة، تسوية أرباح...",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            MosaribTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = "طريقة الدفع",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

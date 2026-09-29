package com.example.features.customers

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
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
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptBottomSheet(
    customer: Customer,
    currentBalance: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountStr by remember { mutableStateOf(if (currentBalance > 0) currentBalance.toString() else "") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تسجيل سند قبض وسداد من العميل",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("العميل: ${customer.name}", fontWeight = FontWeight.SemiBold)
            Text(
                text = "الرصيد المستحق حالياً: ${Formatters.formatCurrency(currentBalance, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (currentBalance > 0) Color(0xFFE53935) else AccentEmerald
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ المقبوض ($currencySymbol) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_amount_input"),
                shape = RoundedCornerShape(12.dp)
            )

            val parsedAmt = Formatters.parseAmountInput(amountStr)
            if (parsedAmt > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedAmt, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentEmerald,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("طريقة الدفع (نقداً، حوالة، تحويل بنكي...)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات السند") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    if (amt > 0) {
                        onConfirm(amt, paymentMethod, notes)
                        onDismiss()
                    }
                },
                enabled = Formatters.parseAmountInput(amountStr) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_receipt_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
            ) {
                Text("حفظ وترحيل سند القبض", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDisbursementBottomSheet(
    customer: Customer,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, description: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountStr by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تسجيل سند صرف للمستفيد",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("يُصرف للسيد / المستفيد: ${customer.name}", fontWeight = FontWeight.SemiBold)
            Text(
                text = "سيتم قيد هذا المبلغ في كشف الحساب ويحدث تغييراً في رصيد العميل",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ المصروف ($currencySymbol) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("disbursement_amount_input"),
                shape = RoundedCornerShape(12.dp)
            )

            val parsedAmt = Formatters.parseAmountInput(amountStr)
            if (parsedAmt > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedAmt, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("البيان / سبب الصرف *") },
                placeholder = { Text("مثال: دفعة نقدية، سلفة، تسوية أرباح...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("طريقة الدفع (نقداً، تحويل بنكي...)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    if (amt > 0) {
                        onConfirm(amt, paymentMethod, description)
                        onDismiss()
                    }
                },
                enabled = Formatters.parseAmountInput(amountStr) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_disbursement_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Text("حفظ وترحيل سند الصرف", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

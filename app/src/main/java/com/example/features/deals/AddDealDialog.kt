package com.example.features.deals

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.features.market.CROP_CATEGORIES
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

@Composable
fun AddDealDialog(
    onDismiss: () -> Unit,
    onSubmit: (
        cropTitle: String,
        cropType: String,
        location: String,
        sellerName: String,
        sellerPhone: String,
        buyerName: String,
        buyerPhone: String,
        dallalName: String,
        dallalPhone: String,
        totalAmount: Double,
        advancePayment: Double,
        dallalCommission: Double,
        termsNotes: String
    ) -> Unit
) {
    var cropTitle by remember { mutableStateOf("") }
    var selectedCrop by remember { mutableStateOf("قات") }
    var location by remember { mutableStateOf("") }
    var sellerName by remember { mutableStateOf("") }
    var sellerPhone by remember { mutableStateOf("") }
    var buyerName by remember { mutableStateOf("") }
    var buyerPhone by remember { mutableStateOf("") }
    var dallalName by remember { mutableStateOf("") }
    var dallalPhone by remember { mutableStateOf("") }
    var totalText by remember { mutableStateOf("") }
    var advanceText by remember { mutableStateOf("") }
    var commissionText by remember { mutableStateOf("") }
    var termsNotes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إبرام وثيقة عقد صلح 📜🤝",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        )
                        Text(
                            text = "توثيق البيع والشراء والأقساط وسعاية الدلال",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Crop details
                OutlinedTextField(
                    value = cropTitle,
                    onValueChange = { cropTitle = it },
                    label = { Text("المحصول / الثمرة (مثال: مزرعة عنب عاصمي)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = selectedCrop,
                        onValueChange = { selectedCrop = it },
                        label = { Text("نوع المحصول (قات/عنب/...)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("الموقع (القرية / العزلة)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Seller (Farmer) Info
                Text(
                    text = "الطرف الأول (البائع / صاحب المزرعة):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sellerName,
                        onValueChange = { sellerName = it },
                        label = { Text("اسم البائع") },
                        modifier = Modifier.weight(1.1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = sellerPhone,
                        onValueChange = { sellerPhone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Buyer (Mojabri) Info
                Text(
                    text = "الطرف الثاني (المشتري / المجبري):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyerName,
                        onValueChange = { buyerName = it },
                        label = { Text("اسم المشتري") },
                        modifier = Modifier.weight(1.1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = buyerPhone,
                        onValueChange = { buyerPhone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Broker (Dallal) Info
                Text(
                    text = "الوسيط (الدلال):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = AccentGold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dallalName,
                        onValueChange = { dallalName = it },
                        label = { Text("اسم الدلال") },
                        modifier = Modifier.weight(1.1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = dallalPhone,
                        onValueChange = { dallalPhone = it },
                        label = { Text("رقم الهاتف") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Financials: Total, Advance, Commission
                Text(
                    text = "المبالغ المالية والاتفاق (بالريال):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it },
                        label = { Text("القيمة الإجمالية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = advanceText,
                        onValueChange = { advanceText = it },
                        label = { Text("العربون المقبوض") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = commissionText,
                    onValueChange = { commissionText = it },
                    label = { Text("سعاية الدلال المتفق عليها (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = termsNotes,
                    onValueChange = { termsNotes = it },
                    label = { Text("شروط الصلح، مواعيد سداد الأقساط، وملاحظات...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val total = totalText.toDoubleOrNull() ?: 0.0
                        val advance = advanceText.toDoubleOrNull() ?: 0.0
                        val commission = commissionText.toDoubleOrNull() ?: 0.0
                        onSubmit(
                            cropTitle,
                            selectedCrop,
                            location,
                            sellerName,
                            sellerPhone,
                            buyerName,
                            buyerPhone,
                            dallalName,
                            dallalPhone,
                            total,
                            advance,
                            commission,
                            termsNotes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                ) {
                    Text(
                        text = "توثيق وإصدار وثيقة الصلح 📜🤝",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
    }
}

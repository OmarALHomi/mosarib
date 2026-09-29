package com.example.features.customers

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import com.example.core.util.Formatters
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCustomerBottomSheet(
    initialCustomer: Customer? = null,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        phone: String,
        farmName: String,
        location: String,
        notes: String,
        customPricePerHour: Double?,
        isBeneficiary: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var farmName by remember { mutableStateOf(initialCustomer?.farmName ?: "") }
    var location by remember { mutableStateOf(initialCustomer?.location ?: "") }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var isBeneficiary by remember { mutableStateOf(initialCustomer?.isBeneficiary ?: false) }
    var customPriceStr by remember {
        mutableStateOf(initialCustomer?.customPricePerHour?.let { it.toString() } ?: "")
    }

    // Contact Picker Launcher (Direct Phone Number + Name without requiring READ_CONTACTS permission)
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { phoneUri ->
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(phoneUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (nameIndex >= 0) {
                                val contactName = cursor.getString(nameIndex)
                                if (!contactName.isNullOrBlank() && (name.isBlank() || name == initialCustomer?.name)) {
                                    name = contactName
                                }
                            }
                            if (numberIndex >= 0) {
                                val contactNumber = cursor.getString(numberIndex)
                                if (!contactNumber.isNullOrBlank()) {
                                    phone = contactNumber
                                        .replace(" ", "")
                                        .replace("-", "")
                                        .replace("(", "")
                                        .replace(")", "")
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    BackHandler(onBack = onDismiss)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialCustomer == null) "إضافة عميل / مزارع جديد" else "تعديل بيانات العميل",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contact Picker Button (Clean borderless surface, direct Phone picker)
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    contactPickerLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal.copy(alpha = 0.12f),
                    contentColor = PrimaryTeal
                )
            ) {
                Icon(Icons.Default.Contacts, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("استيراد من جهات الاتصال", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم العميل *") },
                placeholder = { Text("مثال: أبو صالح العامري") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف (للاتصال والواتساب)") },
                placeholder = { Text("777123456") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryTeal) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_phone_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Farm Name
            OutlinedTextField(
                value = farmName,
                onValueChange = { farmName = it },
                label = { Text("اسم المزرعة أو الأرض (اختياري)") },
                placeholder = { Text("مثال: بستان النخيل، أرض السد") },
                leadingIcon = { Icon(Icons.Default.Yard, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("الموقع أو المنطقة (اختياري)") },
                placeholder = { Text("مثال: القطاع الغربي، وادي الخير") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Price Per Hour
            OutlinedTextField(
                value = customPriceStr,
                onValueChange = { customPriceStr = Formatters.formatAmountInput(it) },
                label = { Text("سعر خاص ومخصص لهذا العميل ($currencySymbol/ساعة) - اختياري") },
                placeholder = { Text("اتركه فارغاً لاستخدام سعر الساعة الافتراضي") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            val customPriceNum = Formatters.parseAmountInput(customPriceStr)
            if (customPriceNum > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(customPriceNum, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PrimaryTeal,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Account Type Selector (Regular Customer vs Beneficiary Account)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "تصنيف ونوع الحساب *",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isBeneficiary,
                            onClick = { isBeneficiary = false },
                            label = { Text("عميل عادي (مزارع)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = isBeneficiary,
                            onClick = { isBeneficiary = true },
                            label = { Text("حساب مستفيد / شريك", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isBeneficiary) {
                            "حساب مستفيد: يقبل تسجيل سندات صرف وقبض، وتسجيل سقي له أو لأحد العملاء على حسابه وتحميل المبالغ عليه."
                        } else {
                            "عميل عادي: تجري له دورات سقي وتسدد فواتيره، ويمكن إسناد سند صرف له لتغيير رصيده."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات إضافية") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val customRate = if (customPriceStr.isNotBlank()) Formatters.parseAmountInput(customPriceStr) else null
                        onSave(
                            initialCustomer?.id ?: 0L,
                            name,
                            phone,
                            farmName,
                            location,
                            notes,
                            customRate,
                            isBeneficiary
                        )
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_customer_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text(
                    text = if (initialCustomer == null) "حفظ وإضافة العميل" else "تحديث البيانات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

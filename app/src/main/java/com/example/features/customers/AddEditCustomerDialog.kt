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
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

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
        isBeneficiary: Boolean,
        isWellOwner: Boolean
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
    var isWellOwner by remember { mutableStateOf(initialCustomer?.isWellOwner ?: false) }
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
                    var contactName: String? = null
                    var contactNumber: String? = null
                    var contactId: String? = null

                    // 1. First attempt: Direct query on the returned URI
                    context.contentResolver.query(phoneUri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                .takeIf { it >= 0 } ?: cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            if (nameIndex >= 0) {
                                contactName = cursor.getString(nameIndex)
                            }

                            val numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIndex >= 0) {
                                contactNumber = cursor.getString(numIndex)
                            }

                            val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                                .takeIf { it >= 0 } ?: cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            if (idIndex >= 0) {
                                contactId = cursor.getString(idIndex)
                            }
                        }
                    }

                    // 2. Fallback: If phone number is still null, query Phone.CONTENT_URI using contact ID
                    if (contactNumber.isNullOrBlank()) {
                        val cid = contactId ?: phoneUri.lastPathSegment
                        if (!cid.isNullOrBlank()) {
                            context.contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                arrayOf(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                                ),
                                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ? OR ${ContactsContract.CommonDataKinds.Phone._ID} = ?",
                                arrayOf(cid, cid),
                                null
                            )?.use { phoneCursor ->
                                if (phoneCursor.moveToFirst()) {
                                    val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                    if (numIdx >= 0) {
                                        contactNumber = phoneCursor.getString(numIdx)
                                    }
                                    if (contactName.isNullOrBlank()) {
                                        val nameIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                        if (nameIdx >= 0) {
                                            contactName = phoneCursor.getString(nameIdx)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!contactName.isNullOrBlank() && (name.isBlank() || name == initialCustomer?.name)) {
                        name = contactName.orEmpty().trim()
                    }

                    if (!contactNumber.isNullOrBlank()) {
                        val converted = com.example.core.util.FileSharingHelper.convertArabicDigitsToAscii(contactNumber.orEmpty())
                        val clean = converted.replace(Regex("[^0-9+]"), "").trim()
                        if (clean.isNotEmpty()) {
                            phone = clean
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
                    text = when {
                        initialCustomer != null -> if (isWellOwner) "تعديل حساب صاحب البئر" else "تعديل بيانات العميل"
                        isWellOwner -> "إضافة صاحب بئر جديد"
                        else -> "إضافة عميل / مزارع جديد"
                    },
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

            // تحديد نوع الحساب (مزارع أو صاحب بئر)
            Text(
                text = "نوع الحساب:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isWellOwner,
                    onClick = { isWellOwner = false },
                    label = { Text("👨‍🌾 مزارع (عميل سقي)", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryTeal,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
                FilterChip(
                    selected = isWellOwner,
                    onClick = { isWellOwner = true },
                    label = { Text("💧 صاحب بئر (مورد ماء)", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SecondaryAqua,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White
                    )
                )
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
                label = { Text(if (isWellOwner) "اسم صاحب البئر *" else "اسم العميل / المزارع *") },
                placeholder = { Text(if (isWellOwner) "مثال: الحاج علي ناصر (مالك بئر الوادي)" else "مثال: أبو صالح العامري") },
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

            // Farm or Well Name
            OutlinedTextField(
                value = farmName,
                onValueChange = { farmName = it },
                label = { Text(if (isWellOwner) "اسم البئر أو المنطقة (اختياري)" else "اسم المزرعة أو الأرض (اختياري)") },
                placeholder = { Text(if (isWellOwner) "مثال: بئر الخير، بئر الصافية" else "مثال: بستان النخيل، أرض السد") },
                leadingIcon = { Icon(if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Yard, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(if (isWellOwner) "موقع البئر (اختياري)" else "الموقع أو المنطقة (اختياري)") },
                placeholder = { Text(if (isWellOwner) "مثال: وادي ضهر، المزرعة الشمالية" else "مثال: القطاع الغربي، وادي الخير") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Custom Price / Cost Price Per Hour
            OutlinedTextField(
                value = customPriceStr,
                onValueChange = { customPriceStr = Formatters.formatAmountInput(it) },
                label = { Text(if (isWellOwner) "تكلفة شراء ساعة الضخ من هذا البئر ($currencySymbol/ساعة) - اختياري" else "سعر خاص ومخصص لهذا العميل ($currencySymbol/ساعة) - اختياري") },
                placeholder = { Text(if (isWellOwner) "تكلفة شراء ساعة الضخ الافتراضية من صاحب هذا البئر" else "اتركه فارغاً لاستخدام سعر الساعة الافتراضي") },
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
                            isBeneficiary,
                            isWellOwner
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
                    text = when {
                        initialCustomer != null -> "تحديث البيانات"
                        isWellOwner -> "حفظ وإضافة صاحب البئر"
                        else -> "حفظ وإضافة العميل"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

package com.example.features.customers

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribFilterChip
import com.example.core.ui.MosaribOutlinedButton
import com.example.core.ui.MosaribTextField
import com.example.core.util.Formatters
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

                    var contactName: String? = null
                    var contactNumber: String? = null
                    var contactId: String? = null

                    // 1. Direct query with DISPLAY_NAME and NUMBER
                    context.contentResolver.query(
                        phoneUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                        ),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)

                            if (nameIdx >= 0) contactName = cursor.getString(nameIdx)
                            if (numIdx >= 0) contactNumber = cursor.getString(numIdx)
                            if (idIdx >= 0) contactId = cursor.getString(idIdx)
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            // تحديد نوع الحساب (مزارع أو صاحب بئر)
            Text(
                text = "نوع الحساب:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MosaribFilterChip(
                    selected = !isWellOwner,
                    onClick = { isWellOwner = false },
                    label = "👨‍🌾 مزارع (عميل سقي)"
                )
                MosaribFilterChip(
                    selected = isWellOwner,
                    onClick = { isWellOwner = true },
                    label = "💧 صاحب بئر (مورد ماء)"
                )
            }

            // زر استيراد من جهات الاتصال
            MosaribOutlinedButton(
                text = "استيراد من جهات الاتصال",
                onClick = {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    contactPickerLauncher.launch(intent)
                },
                leadingIcon = { Icon(Icons.Default.Contacts, contentDescription = null, tint = PrimaryTeal) }
            )

            // الاسم
            MosaribTextField(
                value = name,
                onValueChange = { name = it },
                label = if (isWellOwner) "اسم صاحب البئر *" else "اسم العميل / المزارع *",
                placeholder = if (isWellOwner) "مثال: الحاج علي ناصر (مالك بئر الوادي)" else "مثال: أبو صالح العامري",
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input")
            )

            // رقم الهاتف
            MosaribTextField(
                value = phone,
                onValueChange = { phone = it },
                label = "رقم الهاتف (للاتصال والواتساب)",
                placeholder = "777123456",
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryTeal) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_phone_input")
            )

            // اسم المزرعة أو البئر
            MosaribTextField(
                value = farmName,
                onValueChange = { farmName = it },
                label = if (isWellOwner) "اسم البئر أو المنطقة (اختياري)" else "اسم المزرعة أو الأرض (اختياري)",
                placeholder = if (isWellOwner) "مثال: بئر الخير، بئر الصافية" else "مثال: بستان النخيل، أرض السد",
                leadingIcon = { Icon(if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Yard, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth()
            )

            // الموقع أو المنطقة
            MosaribTextField(
                value = location,
                onValueChange = { location = it },
                label = if (isWellOwner) "موقع البئر (اختياري)" else "الموقع أو المنطقة (اختياري)",
                placeholder = if (isWellOwner) "مثال: وادي ضهر، المزرعة الشمالية" else "مثال: القطاع الغربي، وادي الخير",
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth()
            )

            // السعر المخصص
            MosaribTextField(
                value = customPriceStr,
                onValueChange = { customPriceStr = Formatters.formatAmountInput(it) },
                label = if (isWellOwner) "تكلفة شراء ساعة الضخ من هذا البئر ($currencySymbol/ساعة) - اختياري" else "سعر خاص ومخصص لهذا العميل ($currencySymbol/ساعة) - اختياري",
                placeholder = if (isWellOwner) "تكلفة شراء ساعة الضخ الافتراضية من صاحب هذا البئر" else "اتركه فارغاً لاستخدام سعر الساعة الافتراضي",
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            val customPriceNum = Formatters.parseAmountInput(customPriceStr)
            if (customPriceNum > 0) {
                Text(
                    text = Formatters.amountToArabicWords(customPriceNum, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PrimaryTeal,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // الملاحظات
            MosaribTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "ملاحظات إضافية",
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = PrimaryTeal) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            MosaribButton(
                text = when {
                    initialCustomer != null -> "تحديث البيانات"
                    isWellOwner -> "حفظ وإضافة صاحب البئر"
                    else -> "حفظ وإضافة العميل"
                },
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
                    .testTag("save_customer_button")
            )
        }
    }
}

package com.example.features.customers

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.ui.MosaribBottomSheet
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribSecondaryButton
import com.example.core.ui.MosaribTextField
import com.example.core.util.Formatters
import com.example.ui.theme.mosarib

data class CustomerFormSnapshot(
    val name: String,
    val phone: String,
    val farmName: String,
    val location: String,
    val notes: String,
    val customGroup: String,
    val isWellOwner: Boolean,
    val isBeneficiary: Boolean,
    val customPricePerHour: Double?
) {
    companion object {
        fun from(
            name: String,
            phone: String,
            farmName: String,
            location: String,
            notes: String,
            customGroup: String,
            isWellOwner: Boolean,
            isBeneficiary: Boolean,
            customPriceStr: String
        ): CustomerFormSnapshot = CustomerFormSnapshot(
            name = name.trim(),
            phone = phone.trim(),
            farmName = farmName.trim(),
            location = location.trim(),
            notes = notes.trim(),
            customGroup = customGroup.trim(),
            isWellOwner = isWellOwner,
            isBeneficiary = isBeneficiary,
            customPricePerHour = Formatters.parseAmountInput(customPriceStr).takeIf { it > 0.0 }
        )
    }
}

/**
 * صفيحة إضافة وتعديل العميل وصاحب البئر مبنية على [MosaribBottomSheet] مع رصد التغييرات وتذييل ثابت.
 */
@Composable
fun AddEditCustomerBottomSheet(
    initialCustomer: Customer? = null,
    existingGroups: List<String> = emptyList(),
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
        isWellOwner: Boolean,
        customGroup: String
    ) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var phone by remember { mutableStateOf(initialCustomer?.phone ?: "") }
    var farmName by remember { mutableStateOf(initialCustomer?.farmName ?: "") }
    var location by remember { mutableStateOf(initialCustomer?.location ?: "") }
    var notes by remember { mutableStateOf(initialCustomer?.notes ?: "") }
    var customGroup by remember { mutableStateOf(initialCustomer?.customGroup ?: "") }
    var isWellOwner by remember { mutableStateOf(initialCustomer?.isWellOwner ?: false) }
    var isBeneficiary by remember { mutableStateOf(initialCustomer?.isBeneficiary ?: false) }
    var customPriceStr by remember {
        mutableStateOf(initialCustomer?.customPricePerHour?.let { it.toString() } ?: "")
    }

    val farmerColor = MaterialTheme.mosarib.success
    val wellOwnerColor = MaterialTheme.mosarib.info

    val activeColor by animateColorAsState(
        targetValue = if (isWellOwner) wellOwnerColor else farmerColor,
        animationSpec = tween(300),
        label = "active_theme_color"
    )

    val currentSnapshot = CustomerFormSnapshot.from(
        name = name,
        phone = phone,
        farmName = farmName,
        location = location,
        notes = notes,
        customGroup = customGroup,
        isWellOwner = isWellOwner,
        isBeneficiary = isBeneficiary,
        customPriceStr = customPriceStr
    )
    val initialSnapshot = remember { currentSnapshot }
    val hasUnsavedChanges = currentSnapshot != initialSnapshot

    // أداة استيراد الاسم ورقم الهاتف من جهات الاتصال
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

    val sheetTitle = when {
        initialCustomer != null -> if (isWellOwner) "تعديل حساب صاحب البئر" else "تعديل بيانات العميل"
        isWellOwner -> "إضافة صاحب بئر جديد"
        else -> "إضافة عميل / مزارع جديد"
    }
    val sheetSubtitle = if (initialCustomer != null) "تحديث البيانات المسجلة" else "تسجيل حساب جديد في النظام"

    MosaribBottomSheet(
        onDismissRequest = onDismiss,
        title = sheetTitle,
        subtitle = sheetSubtitle,
        icon = if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Park,
        iconTint = activeColor,
        hasUnsavedChanges = hasUnsavedChanges,
        footer = {
            MosaribSecondaryButton(
                text = "إلغاء",
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )

            MosaribPrimaryButton(
                text = when {
                    initialCustomer != null -> "حفظ التعديلات"
                    isWellOwner -> "حفظ صاحب البئر"
                    else -> "حفظ وإضافة العميل"
                },
                onClick = {
                    if (name.isNotBlank()) {
                        val customRate = if (customPriceStr.isNotBlank()) Formatters.parseAmountInput(customPriceStr) else null
                        onSave(
                            initialCustomer?.id ?: 0L,
                            name.trim(),
                            phone.trim(),
                            farmName.trim(),
                            location.trim(),
                            notes.trim(),
                            customRate,
                            isBeneficiary,
                            isWellOwner,
                            customGroup.trim()
                        )
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .weight(2f)
                    .testTag("save_customer_button")
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. هل هذا مورد (صاحب بئر)؟
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "هل هذا مورد (صاحب بئر)؟",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Switch(checked = isWellOwner, onCheckedChange = { isWellOwner = it })
            }

            // 2. البطاقة الأولى: البيانات الأساسية
            FormSectionCard(
                title = "البيانات الأساسية",
                icon = Icons.Default.Person,
                iconColor = activeColor
            ) {
                MosaribTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = if (isWellOwner) "اسم صاحب البئر *" else "اسم العميل / المزارع *",
                    placeholder = if (isWellOwner) "مثال: الحاج علي ناصر (مالك بئر الوادي)" else "مثال: أبو صالح العامري",
                    isDense = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = activeColor) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                MosaribTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "رقم الهاتف (للاتصال والتواصل)",
                    placeholder = "مثال: 777123456",
                    isDense = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = activeColor) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            contactPickerLauncher.launch(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = "جهات الاتصال",
                                tint = activeColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )
            }

            // 3. البطاقة الثانية: بيانات الأرض / البئر والموقع
            FormSectionCard(
                title = if (isWellOwner) "بيانات البئر والموقع" else "بيانات المزرعة والموقع",
                icon = if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Yard,
                iconColor = activeColor
            ) {
                MosaribTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = if (isWellOwner) "اسم البئر أو المحطة (اختياري)" else "اسم المزرعة أو الأرض (اختياري)",
                    placeholder = if (isWellOwner) "مثال: بئر الخير، بئر الصافية" else "مثال: بستان النخيل، أرض السد",
                    isDense = true,
                    leadingIcon = {
                        Icon(
                            imageVector = if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Yard,
                            contentDescription = null,
                            tint = activeColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                MosaribTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = if (isWellOwner) "موقع البئر (اختياري)" else "الموقع أو المنطقة (اختياري)",
                    placeholder = if (isWellOwner) "مثال: وادي ضهر، المحطة الشمالية" else "مثال: القطاع الغربي، وادي الخير",
                    isDense = true,
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = activeColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                MosaribTextField(
                    value = customGroup,
                    onValueChange = { customGroup = it },
                    label = "المجموعة (اختياري)",
                    placeholder = if (isWellOwner) "مثال: آبار الوادي..." else "مثال: مجموعة الشرق...",
                    isDense = true,
                    leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null, tint = activeColor) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingGroups.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existingGroups.forEach { grp ->
                            val isSelected = customGroup.trim().equals(grp.trim(), ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, if (isSelected) activeColor else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable {
                                    customGroup = if (isSelected) "" else grp
                                }
                            ) {
                                Text(
                                    text = grp,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. البطاقة الثالثة: التسعير والحساب المالي
            FormSectionCard(
                title = "التسعير والحساب المالي",
                icon = Icons.Default.AttachMoney,
                iconColor = MaterialTheme.mosarib.warning
            ) {
                MosaribTextField(
                    value = customPriceStr,
                    onValueChange = { customPriceStr = Formatters.formatAmountInput(it) },
                    label = if (isWellOwner) "تكلفة شراء ساعة الضخ ($currencySymbol/ساعة) - اختياري" else "سعر سقي مخصص لهذا العميل ($currencySymbol/ساعة) - اختياري",
                    placeholder = if (isWellOwner) "تكلفة الشراء الافتراضية من هذا البئر" else "اتركه فارغاً لاستخدام السعر العام",
                    isDense = true,
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.mosarib.warning) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                val customPriceNum = Formatters.parseAmountInput(customPriceStr)
                AnimatedVisibility(
                    visible = customPriceNum > 0,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(200))
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.mosarib.warningContainer,
                        border = BorderStroke(1.dp, MaterialTheme.mosarib.warning.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.mosarib.warning,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = Formatters.amountToArabicWords(customPriceNum, currencySymbol),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.mosarib.onWarningContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                MosaribTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "ملاحظات إضافية (اختياري)",
                    placeholder = "أي معلومات أو شروط خاصة بالحساب...",
                    isDense = true,
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = activeColor) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * كرت قسم منسق ومجمع لحقول النموذج (Form Section Container).
 */
@Composable
private fun FormSectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            content()
        }
    }
}

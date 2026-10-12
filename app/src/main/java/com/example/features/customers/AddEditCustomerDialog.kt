package com.example.features.customers

import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribOutlinedButton
import com.example.core.ui.MosaribTextField
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

/**
 * نموذج إضافة وتعديل العميل وصاحب البئر بتصميم عصري فائق التباين والفخامة (Design System Overhaul).
 * - بطاقة هوية تفاعلية تعكس الحساب والحرف الأول ديناميكياً.
 * - محوّل نمط الحساب المقسم (مزارع / صاحب بئر) بألوان متناسقة وتأثيرات بصرية حية.
 * - تنظيم مدخلات البيانات ضمن كروت أقسام واضحة (البيانات الأساسية، الموقع والأرض، الحساب والتسعير).
 * - استيراد فوري ذكي من جهات الاتصال عبر دليل الهاتف المدمج.
 * - دعم كامل ودقيق لنظام RTL والوضعين الفاتح والمظلم.
 */
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
    val isDark = isAppDark()

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

    // اللون الرئيسي النشط يتكيف ديناميكياً مع نوع الحساب (تركوازي للمزارع، وأزرق مائي لصاحب البئر)
    val activeColor by animateColorAsState(
        targetValue = if (isWellOwner) Color(0xFF0284C7) else PrimaryTeal,
        animationSpec = tween(300),
        label = "active_theme_color"
    )

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

    BackHandler(onBack = onDismiss)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) Color(0xFF10191F) else Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) Color(0xFF2C3E4A) else Color(0xFFCBD5E1))
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. شريط العنوان العلوي الأنيق
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(activeColor.copy(alpha = if (isDark) 0.22f else 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (initialCustomer != null) Icons.Default.Person else Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = activeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = when {
                                    initialCustomer != null -> if (isWellOwner) "تعديل حساب صاحب البئر" else "تعديل بيانات العميل"
                                    isWellOwner -> "إضافة صاحب بئر جديد"
                                    else -> "إضافة عميل / مزارع جديد"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                                )
                            )
                            Text(
                                text = if (initialCustomer != null) "تحديث البيانات المسجلة" else "تسجيل حساب جديد في النظام",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1A262E) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء",
                            modifier = Modifier.size(18.dp),
                            tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }

                // 2. بطاقة المعاينة الفورية للأفاتار والهوية (Hero Identity Card)
                val initialLetter = name.trim().firstOrNull()?.toString() ?: if (isWellOwner) "ب" else "ع"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF141F25) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF263842) else Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // دائرة الأفاتار مع شارة نوع الحساب
                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(52.dp),
                                shape = CircleShape,
                                color = activeColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                                border = BorderStroke(1.5.dp, activeColor)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initialLetter,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeColor
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .size(19.dp)
                                    .align(Alignment.BottomEnd),
                                shape = CircleShape,
                                color = activeColor
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Agriculture,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (name.isNotBlank()) name else (if (isWellOwner) "اسم صاحب البئر..." else "اسم العميل..."),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (name.isNotBlank()) (if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A))
                                    else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8))
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = activeColor.copy(alpha = if (isDark) 0.20f else 0.10f)
                                ) {
                                    Text(
                                        text = if (isWellOwner) "💧 صاحب بئر" else "👨‍🌾 مزارع",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                                if (phone.isNotBlank()) {
                                    Text(
                                        text = phone,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // زر استيراد سريع ومدمج من جهات الاتصال
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                    contactPickerLauncher.launch(intent)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF1E2D36) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF2C3E4A) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Contacts,
                                    contentDescription = "جهات الاتصال",
                                    tint = activeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "دليل الهاتف",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                // 3. شريط اختيار نوع الحساب (مزارع vs صاحب بئر) بتصميم مجزأ وعصري
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PersonaTypeCard(
                        title = "مزارع (عميل)",
                        subtitle = "سقي ومستحقات",
                        icon = Icons.Default.Agriculture,
                        isSelected = !isWellOwner,
                        activeColor = PrimaryTeal,
                        modifier = Modifier.weight(1f),
                        onClick = { isWellOwner = false }
                    )
                    PersonaTypeCard(
                        title = "صاحب بئر (مورد)",
                        subtitle = "شراء ومقاصة",
                        icon = Icons.Default.WaterDrop,
                        isSelected = isWellOwner,
                        activeColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { isWellOwner = true }
                    )
                }

                // 4. البطاقة الأولى: البيانات الأساسية
                FormSectionCard(
                    title = "البيانات الأساسية",
                    icon = Icons.Default.Person,
                    iconColor = activeColor
                ) {
                    // حقل الاسم
                    MosaribTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = if (isWellOwner) "اسم صاحب البئر *" else "اسم العميل / المزارع *",
                        placeholder = if (isWellOwner) "مثال: الحاج علي ناصر (مالك بئر الوادي)" else "مثال: أبو صالح العامري",
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = activeColor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_name_input")
                    )

                    // حقل الهاتف مع زر الاتصال السريع
                    MosaribTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "رقم الهاتف (للاتصال والتواصل)",
                        placeholder = "مثال: 777123456",
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

                // 5. البطاقة الثانية: بيانات الأرض / البئر والموقع
                FormSectionCard(
                    title = if (isWellOwner) "بيانات البئر والموقع" else "بيانات المزرعة والموقع",
                    icon = if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Yard,
                    iconColor = activeColor
                ) {
                    // اسم المزرعة أو البئر
                    MosaribTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = if (isWellOwner) "اسم البئر أو المحطة (اختياري)" else "اسم المزرعة أو الأرض (اختياري)",
                        placeholder = if (isWellOwner) "مثال: بئر الخير، بئر الصافية" else "مثال: بستان النخيل، أرض السد",
                        leadingIcon = {
                            Icon(
                                imageVector = if (isWellOwner) Icons.Default.WaterDrop else Icons.Default.Yard,
                                contentDescription = null,
                                tint = activeColor
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // الموقع أو المنطقة
                    MosaribTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = if (isWellOwner) "موقع البئر (اختياري)" else "الموقع أو المنطقة (اختياري)",
                        placeholder = if (isWellOwner) "مثال: وادي ضهر، المحطة الشمالية" else "مثال: القطاع الغربي، وادي الخير",
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = activeColor) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 6. البطاقة الثالثة: التسعير والحساب المالي
                FormSectionCard(
                    title = "التسعير والحساب المالي",
                    icon = Icons.Default.AttachMoney,
                    iconColor = AccentGold
                ) {
                    // السعر المخصص للساعة
                    MosaribTextField(
                        value = customPriceStr,
                        onValueChange = { customPriceStr = Formatters.formatAmountInput(it) },
                        label = if (isWellOwner) "تكلفة شراء ساعة الضخ ($currencySymbol/ساعة) - اختياري" else "سعر سقي مخصص لهذا العميل ($currencySymbol/ساعة) - اختياري",
                        placeholder = if (isWellOwner) "تكلفة الشراء الافتراضية من هذا البئر" else "اتركه فارغاً لاستخدام السعر العام",
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentGold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // لافتة التفقيط بالكلمات العربية عند تحديد مبلغ
                    val customPriceNum = Formatters.parseAmountInput(customPriceStr)
                    AnimatedVisibility(
                        visible = customPriceNum > 0,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(200))
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = AccentGold.copy(alpha = if (isDark) 0.16f else 0.10f),
                            border = BorderStroke(1.dp, AccentGold.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = Formatters.amountToArabicWords(customPriceNum, currencySymbol),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    // الملاحظات
                    MosaribTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "ملاحظات إضافية (اختياري)",
                        placeholder = "أي معلومات أو شروط خاصة بالحساب...",
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = activeColor) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 7. أزرار الإجراءات السفلية (إلغاء وحفظ الحساب)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MosaribOutlinedButton(
                        text = "إلغاء",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 48.dp
                    )

                    MosaribButton(
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
                                    isWellOwner
                                )
                                onDismiss()
                            }
                        },
                        enabled = name.isNotBlank(),
                        containerColor = activeColor,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("save_customer_button"),
                        height = 48.dp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * كرت نمط الحساب المجزأ (Persona Type Switcher Card).
 */
@Composable
private fun PersonaTypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = isAppDark()
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor.copy(alpha = if (isDark) 0.18f else 0.10f)
        else if (isDark) Color(0xFF141F25) else Color(0xFFF8FAFC),
        animationSpec = tween(250),
        label = "persona_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) activeColor
        else if (isDark) Color(0xFF263842) else Color(0xFFE2E8F0),
        animationSpec = tween(250),
        label = "persona_border"
    )

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) activeColor else (if (isDark) Color(0xFF202E36) else Color(0xFFE2E8F0))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isSelected) activeColor else (if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)),
                        fontSize = 12.5.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontSize = 10.5.sp
                    ),
                    maxLines = 1
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(16.dp)
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
    val isDark = isAppDark()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF141F25) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF263842) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = if (isDark) 0.22f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        fontSize = 13.5.sp
                    )
                )
            }
            content()
        }
    }
}

package com.example.features.customers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.ui.MosaribButton
import com.example.core.ui.MosaribOutlinedButton
import com.example.core.ui.MosaribTextField
import com.example.core.ui.isAppDark
import com.example.core.util.Formatters
import com.example.ui.theme.PrimaryTeal

private enum class DraftFilterTab {
    ALL, SELECTED, NEW_ONLY, EXISTING_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerImportReviewDialog(
    drafts: List<ImportedCustomerDraft>,
    onDraftUpdated: (ImportedCustomerDraft) -> Unit,
    onDraftDeleted: (String) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit,
    onConfirmImport: (updateExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppDark()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf(DraftFilterTab.ALL) }
    var updateExistingCustomers by remember { mutableStateOf(true) }
    var draftToEdit by remember { mutableStateOf<ImportedCustomerDraft?>(null) }

    val totalCount = drafts.size
    val selectedCount = remember(drafts) { drafts.count { it.isSelected && it.name.isNotBlank() } }
    val allSelected = remember(drafts) { drafts.isNotEmpty() && drafts.all { it.isSelected } }
    val existingCount = remember(drafts) { drafts.count { it.isExistingInDb } }
    val newCount = remember(drafts) { drafts.count { !it.isExistingInDb } }

    val filteredDrafts by remember(drafts, searchQuery, filterTab) {
        derivedStateOf {
            drafts.filter { draft ->
                val matchesQuery = searchQuery.isBlank() ||
                    draft.name.contains(searchQuery, ignoreCase = true) ||
                    draft.phone.contains(searchQuery) ||
                    draft.farmName.contains(searchQuery, ignoreCase = true) ||
                    draft.location.contains(searchQuery, ignoreCase = true)

                val matchesTab = when (filterTab) {
                    DraftFilterTab.ALL -> true
                    DraftFilterTab.SELECTED -> draft.isSelected
                    DraftFilterTab.NEW_ONLY -> !draft.isExistingInDb
                    DraftFilterTab.EXISTING_ONLY -> draft.isExistingInDb
                }
                matchesQuery && matchesTab
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. الرأس الرئيسي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = PrimaryTeal.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "مراجعة استيراد العملاء",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "تم العثور على $totalCount حساب في الملف (المحدد: $selectedCount)",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. حقل البحث السريع في المسودات
            MosaribTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "بحث في الحسابات المستوردة...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. أزرار الفلترة السريعة
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = filterTab == DraftFilterTab.ALL,
                        onClick = { filterTab = DraftFilterTab.ALL },
                        label = { Text("الكل ($totalCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterTab == DraftFilterTab.SELECTED,
                        onClick = { filterTab = DraftFilterTab.SELECTED },
                        label = { Text("المحدد ($selectedCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = filterTab == DraftFilterTab.NEW_ONLY,
                        onClick = { filterTab = DraftFilterTab.NEW_ONLY },
                        label = { Text("حسابات جديدة ($newCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                if (existingCount > 0) {
                    item {
                        FilterChip(
                            selected = filterTab == DraftFilterTab.EXISTING_ONLY,
                            onClick = { filterTab = DraftFilterTab.EXISTING_ONLY },
                            label = { Text("موجود مسبقاً ($existingCount)", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD97706),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. شريط الإجراءات الجماعية وخيار تحديث المسجلين مسبقاً
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF131D24) else Color(0xFFF1F5F9)
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { onToggleSelectAll(!allSelected) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { onToggleSelectAll(it) },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (allSelected) "إلغاء تحديد الكل" else "تحديد الكل ($totalCount)",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        if (existingCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تحديث بيانات المسجل مسبقاً",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = updateExistingCustomers,
                                    onCheckedChange = { updateExistingCustomers = it },
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5. قائمة الحسابات المستوردة
            Box(modifier = Modifier.weight(1f)) {
                if (filteredDrafts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد نتائج مطابقة للبحث أو التصفية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDrafts, key = { it.localId }) { draft ->
                            ImportDraftCard(
                                draft = draft,
                                isDark = isDark,
                                onToggleSelected = {
                                    val updated = draft.copy(isSelected = !draft.isSelected)
                                    onDraftUpdated(updated)
                                },
                                onEditClick = { draftToEdit = draft },
                                onDeleteClick = { onDraftDeleted(draft.localId) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // 6. شريط الحفظ والتأكيد السفلي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MosaribOutlinedButton(
                    text = "إلغاء",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )

                MosaribButton(
                    text = "تأكيد واستيراد ($selectedCount)",
                    onClick = { onConfirmImport(updateExistingCustomers) },
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1.8f)
                )
            }
        }
    }

    // نافذة التعديل السريع للحساب
    draftToEdit?.let { editingDraft ->
        EditImportDraftDialog(
            draft = editingDraft,
            onSave = { updated ->
                onDraftUpdated(updated)
                draftToEdit = null
            },
            onDismiss = { draftToEdit = null }
        )
    }
}

@Composable
private fun ImportDraftCard(
    draft: ImportedCustomerDraft,
    isDark: Boolean,
    onToggleSelected: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val hasError = draft.validationError != null
    val borderColor = when {
        hasError -> Color(0xFFEF4444)
        draft.isSelected -> PrimaryTeal
        isDark -> Color(0xFF2A3C45)
        else -> Color(0xFFE2E8F0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelected() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (draft.isSelected) {
                if (isDark) Color(0xFF14242A) else Color(0xFFF0FDFA)
            } else {
                if (isDark) Color(0xFF131D24) else Color(0xFFFFFFFF)
            }
        ),
        border = BorderStroke(if (draft.isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = draft.isSelected,
                onCheckedChange = { onToggleSelected() },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // تفاصيل الحساب
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (draft.name.isNotBlank()) draft.name else "اسم العميل مفقود!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (hasError) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    )

                    // شارة صاحب بئر
                    if (draft.isWellOwner) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF0C2433) else Color(0xFFE0F2FE),
                            border = BorderStroke(0.5.dp, if (isDark) Color(0xFF0369A1) else Color(0xFFBAE6FD))
                        ) {
                            Text(
                                text = "صاحب بئر",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // شارة مسجل مسبقاً
                    if (draft.isExistingInDb) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF2C1E0A) else Color(0xFFFEF3C7),
                            border = BorderStroke(0.5.dp, Color(0xFFD97706))
                        ) {
                            Text(
                                text = "مسجل مسبقاً",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFD97706),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (hasError) {
                    Text(
                        text = draft.validationError.orEmpty(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // معلومات الهاتف والمزرعة والموقع
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (draft.phone.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(draft.phone, style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp))
                        }
                    }

                    val locDetail = when {
                        draft.farmName.isNotEmpty() && draft.location.isNotEmpty() -> "${draft.farmName} - ${draft.location}"
                        draft.farmName.isNotEmpty() -> draft.farmName
                        draft.location.isNotEmpty() -> draft.location
                        else -> ""
                    }
                    if (locDetail.isNotEmpty()) {
                        Text(
                            text = locDetail,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp),
                            maxLines = 1
                        )
                    }

                    draft.customPricePerHour?.let { price ->
                        Text(
                            text = "${Formatters.formatCurrency(price, "").trim()} ر.ي/س",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // أزرار التحكم بالسطر (تعديل / حذف)
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "تعديل",
                    modifier = Modifier.size(16.dp),
                    tint = PrimaryTeal
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف من القائمة",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun EditImportDraftDialog(
    draft: ImportedCustomerDraft,
    onSave: (ImportedCustomerDraft) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(draft.name) }
    var phone by remember { mutableStateOf(draft.phone) }
    var farmName by remember { mutableStateOf(draft.farmName) }
    var location by remember { mutableStateOf(draft.location) }
    var priceStr by remember { mutableStateOf(draft.customPricePerHour?.let { Formatters.formatAmountInput(it.toString()) } ?: "") }
    var isWellOwner by remember { mutableStateOf(draft.isWellOwner) }
    var notes by remember { mutableStateOf(draft.notes) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "تعديل بيانات الحساب المستورد",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(14.dp))

                MosaribTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "اسم العميل *",
                    placeholder = "الاسم الكامل...",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                MosaribTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "رقم الهاتف",
                    placeholder = "77XXXXXXX",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MosaribTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = "المزرعة",
                        modifier = Modifier.weight(1f)
                    )
                    MosaribTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = "الموقع",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                MosaribTextField(
                    value = priceStr,
                    onValueChange = { priceStr = Formatters.formatAmountInput(it) },
                    label = "سعر الساعة المخصص (ر.ي)",
                    placeholder = "اختياري",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حساب صاحب بئر مستقل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(checked = isWellOwner, onCheckedChange = { isWellOwner = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MosaribOutlinedButton(
                        text = "إلغاء",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    MosaribButton(
                        text = "تم وحفظ",
                        onClick = {
                            val parsedPrice = Formatters.parseAmountInput(priceStr).takeIf { it > 0 }
                            val cleanName = name.trim()
                            val validationErr = if (cleanName.isBlank()) "اسم العميل مطلوب" else null
                            onSave(
                                draft.copy(
                                    name = cleanName,
                                    phone = phone.trim(),
                                    farmName = farmName.trim(),
                                    location = location.trim(),
                                    customPricePerHour = parsedPrice,
                                    isWellOwner = isWellOwner,
                                    notes = notes.trim(),
                                    validationError = validationErr,
                                    isSelected = validationErr == null
                                )
                            )
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

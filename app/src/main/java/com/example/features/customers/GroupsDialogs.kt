package com.example.features.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.DialogTone
import com.example.core.ui.MosaribDialog
import com.example.core.ui.MosaribPrimaryButton
import com.example.core.ui.MosaribTextField
import com.example.ui.theme.mosarib

/**
 * نافذة إضافة أو تعديل مجموعة وتحديد أعضائها من العملاء
 */
@Composable
fun AddEditGroupDialog(
    initialGroupName: String? = null,
    initialMemberIds: Set<Long> = emptySet(),
    allCustomers: List<CustomerWithBalance>,
    onDismiss: () -> Unit,
    onSave: (name: String, memberIds: Set<Long>) -> Unit
) {
    var name by remember { mutableStateOf(initialGroupName ?: "") }
    var selectedIds by remember { mutableStateOf(initialMemberIds) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCustomers = remember(allCustomers, searchQuery) {
        if (searchQuery.isBlank()) allCustomers
        else allCustomers.filter {
            it.customer.name.contains(searchQuery, ignoreCase = true) ||
            it.customer.phone.contains(searchQuery) ||
            it.customer.farmName.contains(searchQuery, ignoreCase = true)
        }
    }

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = if (initialGroupName != null) "تعديل المجموعة" else "مجموعة جديدة",
        icon = Icons.Default.Bookmarks,
        tone = DialogTone.Neutral,
        confirmText = "حفظ",
        onConfirm = {
            if (name.isNotBlank()) {
                onSave(name.trim(), selectedIds)
                onDismiss()
            }
        },
        dismissText = "إلغاء",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // حقل اسم المجموعة
            MosaribTextField(
                value = name,
                onValueChange = { name = it },
                label = "اسم المجموعة *",
                placeholder = "مثال: مجموعة الشرق، آبار الوادي...",
                modifier = Modifier.fillMaxWidth()
            )

            // رأس قائمة الأعضاء وتحديد الكل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "الأعضاء (${selectedIds.size} محدد)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row {
                    TextButton(
                        onClick = {
                            selectedIds = allCustomers.map { it.customer.id }.toSet()
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("الكل", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    if (selectedIds.isNotEmpty()) {
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("إلغاء", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.mosarib.textMuted)
                        }
                    }
                }
            }

            // حقل بحث سريع داخل العملاء
            MosaribTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "بحث في العملاء...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(14.dp))
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            // قائمة العملاء للاختيار
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredCustomers, key = { it.customer.id }) { item ->
                    val isSelected = selectedIds.contains(item.customer.id)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (isSelected) {
                                    selectedIds - item.customer.id
                                } else {
                                    selectedIds + item.customer.id
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked == true) {
                                        selectedIds + item.customer.id
                                    } else {
                                        selectedIds - item.customer.id
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                imageVector = if (item.customer.isWellOwner) Icons.Default.WaterDrop else Icons.Default.Park,
                                contentDescription = null,
                                tint = if (item.customer.isWellOwner) MaterialTheme.colorScheme.primary else MaterialTheme.mosarib.success,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.customer.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                                if (item.customer.farmName.isNotBlank() || item.customer.phone.isNotBlank()) {
                                    Text(
                                        text = item.customer.farmName.ifBlank { item.customer.phone },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة إدارة المجموعات: عرض المجموعات، تعديلها، تعديل أسمائها، أو حذفها
 */
@Composable
fun ManageGroupsDialog(
    groups: List<String>,
    allCustomers: List<CustomerWithBalance>,
    onDismiss: () -> Unit,
    onEditGroup: (groupName: String, currentMemberIds: Set<Long>) -> Unit,
    onRenameGroup: (oldName: String, newName: String) -> Unit,
    onDeleteGroup: (groupName: String) -> Unit,
    onAddNewGroup: () -> Unit
) {
    var groupToDelete by remember { mutableStateOf<String?>(null) }

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "إدارة المجموعات",
        icon = Icons.Default.Group,
        tone = DialogTone.Neutral,
        confirmText = "إغلاق",
        onConfirm = onDismiss,
        dismissText = null
    ) {
        if (groups.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Bookmarks,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "لا توجد مجموعات حالياً",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups, key = { it }) { grp ->
                    val memberCount = allCustomers.count {
                        it.customer.customGroup.trim().equals(grp.trim(), ignoreCase = true)
                    }
                    val memberIds = allCustomers
                        .filter { it.customer.customGroup.trim().equals(grp.trim(), ignoreCase = true) }
                        .map { it.customer.id }
                        .toSet()

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = grp,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "$memberCount عميل",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // زر تعديل الأعضاء
                                IconButton(
                                    onClick = { onEditGroup(grp, memberIds) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "تعديل",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // زر حذف المجموعة
                                IconButton(
                                    onClick = { groupToDelete = grp },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.mosarib.debt,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        MosaribPrimaryButton(
            text = "مجموعة جديدة",
            onClick = onAddNewGroup,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // تأكيد حذف المجموعة
    groupToDelete?.let { grp ->
        MosaribDialog(
            onDismissRequest = { groupToDelete = null },
            title = "حذف المجموعة",
            message = "هل تريد حذف مجموعة ($grp)؟ لن يتم حذف أي عميل، بل سيُفك ارتباطهم بالمجموعة فقط.",
            tone = DialogTone.Danger,
            confirmText = "حذف",
            onConfirm = {
                onDeleteGroup(grp)
                groupToDelete = null
            },
            dismissText = "إلغاء",
            onDismiss = { groupToDelete = null }
        )
    }
}

package com.example.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.features.customers.Customer

/** Pick-only customer field: tap opens a menu with an in-menu search and «+ عميل جديد». */
@Composable
fun CustomerPickerField(
    customers: List<Customer>,
    selected: Customer?,
    onSelect: (Customer) -> Unit,
    onAddNew: (() -> Unit)?,
    modifier: Modifier = Modifier,
    label: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    Box(modifier = modifier) {
        MosaribTextField(
            value = selected?.name ?: "اختر العميل...",
            onValueChange = {},
            readOnly = true,
            label = label,
            leadingIcon = {
                Icon(
                    imageVector = if (selected?.isWellOwner == true) Icons.Default.WaterDrop else Icons.Default.Park,
                    contentDescription = null,
                    tint = if (selected?.isWellOwner == true) Color(0xFF0288D1) else Color(0xFF2E7D32)
                )
            },
            trailingIcon = trailingIcon ?: { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        // The overlay stops the trailing slot (mic) from being covered: it leaves the end 56dp free.
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = if (trailingIcon != null) 56.dp else 0.dp)
                .clickable { query = ""; expanded = true }
        )
        MosaribMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; query = "" },
            modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 320.dp)
        ) {
            MosaribTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "بحث بالاسم أو الهاتف...",
                isDense = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = "مسح", modifier = Modifier.size(14.dp)) } }
                } else null,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            )
            MosaribMenuDivider()
            if (onAddNew != null) MosaribMenuItem(
                text = "+ عميل جديد",
                icon = Icons.Default.PersonAdd,
                tint = MaterialTheme.colorScheme.primary,
                onClick = { expanded = false; query = ""; onAddNew() }
            )
            customers.filter {
                query.isBlank() || it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query) || it.farmName.contains(query, ignoreCase = true)
            }.forEach { c ->
                MosaribMenuItem(
                    text = c.name,
                    icon = if (c.isWellOwner) Icons.Default.WaterDrop else Icons.Default.Park,
                    onClick = { expanded = false; query = ""; onSelect(c) }
                )
            }
        }
    }
}

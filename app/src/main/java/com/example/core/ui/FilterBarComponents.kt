package com.example.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.ui.theme.mosarib
import java.util.Calendar

data class DropdownOption<out T>(val value: T, val label: String)

/**
 * شريحة منسدلة موحدة للفلاتر تعتمد على MosaribMenu و MosaribMenuItem.
 * ارتفاع 36dp، حواف دائرية CircleShape، وحاشية أفقية 12dp.
 * إذا كانت القيمة افتراضية: إطار 1dp outline مع نص onSurfaceVariant.
 * إذا كانت غير افتراضية: خلفية primaryContainer مع نص onPrimaryContainer بدون إطار.
 */
@Composable
fun <T> FilterDropdownChip(
    label: String,
    selected: T,
    options: List<DropdownOption<T>>,
    isDefault: Boolean,
    onSelect: (T) -> Unit,
    footer: (@Composable () -> Unit)? = null,
    customDisplayText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val currentOption = remember(selected, options) { options.find { it.value == selected } }

    val displayText = remember(isDefault, label, currentOption, customDisplayText) {
        if (customDisplayText != null) {
            customDisplayText
        } else if (isDefault) {
            "$label: ${currentOption?.label ?: "الكل"}"
        } else {
            currentOption?.label ?: label
        }
    }

    Box {
        Surface(
            shape = CircleShape,
            color = if (isDefault) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
            border = if (isDefault) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            modifier = Modifier
                .height(36.dp)
                .clip(CircleShape)
                .clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isDefault) FontWeight.Normal else FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        MosaribMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 180.dp)
        ) {
            options.forEach { opt ->
                val isSelected = opt.value == selected
                MosaribMenuItem(
                    text = opt.label,
                    icon = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    trailing = if (isSelected) "✓" else null,
                    onClick = {
                        expanded = false
                        onSelect(opt.value)
                    }
                )
            }

            if (footer != null) {
                MosaribMenuDivider()
                footer()
            }
        }
    }
}

/**
 * شريحة مسح الكل: تظهر في نهاية صف الفلاتر عند تفعيل أي فلتر غير افتراضي.
 * بلون debt.
 */
@Composable
fun FilterClearAllChip(
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val debtColor = MaterialTheme.mosarib.debt
    Surface(
        shape = CircleShape,
        color = debtColor.copy(alpha = 0.12f),
        contentColor = debtColor,
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .clickable { onClearAll() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = "مسح الكل ✕",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = debtColor
            )
        }
    }
}

/**
 * نافذة اختيار عميل سريعة مع البحث على MosaribDialog.
 */
@Composable
fun CustomerSearchPickerDialog(
    customers: List<Customer>,
    onSelect: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredCustomers = remember(customers, query) {
        if (query.isBlank()) customers
        else {
            val q = query.trim()
            customers.filter {
                it.name.contains(q, ignoreCase = true) ||
                it.phone.contains(q, ignoreCase = true) ||
                it.farmName.contains(q, ignoreCase = true)
            }
        }
    }

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "اختيار عميل",
        icon = Icons.Default.Person,
        tone = DialogTone.Neutral,
        message = null,
        confirmText = "إلغاء",
        onConfirm = onDismiss,
        dismissText = null
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("بحث عن عميل...", style = MaterialTheme.typography.bodyMedium) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            items(filteredCustomers, key = { it.id }) { customer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(customer) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (customer.farmName.isNotBlank() || customer.phone.isNotBlank()) {
                            Text(
                                text = listOfNotNull(
                                    customer.farmName.takeIf { it.isNotBlank() },
                                    customer.phone.takeIf { it.isNotBlank() }
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

/**
 * نافذة حوار لتحديد فترة مخصصة (من تاريخ - إلى تاريخ) على MosaribDialog.
 */
@Composable
fun MosaribDateRangeDialog(
    initialStartTs: Long?,
    initialEndTs: Long?,
    onDismiss: () -> Unit,
    onConfirm: (startTs: Long, endTs: Long) -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    var startTimestamp by remember {
        mutableLongStateOf(initialStartTs ?: now)
    }
    var endTimestamp by remember {
        mutableLongStateOf(initialEndTs ?: maxOf(startTimestamp, now))
    }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    MosaribDialog(
        onDismissRequest = onDismiss,
        title = "تحديد الفترة (من - إلى)",
        icon = Icons.Default.DateRange,
        tone = DialogTone.Neutral,
        message = "اختر تاريخ البداية والنهاية لتصفية العمليات",
        confirmText = "تطبيق",
        onConfirm = {
            val startCal = Calendar.getInstance().apply {
                timeInMillis = startTimestamp
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = endTimestamp
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            onConfirm(startCal.timeInMillis, endCal.timeInMillis)
        },
        dismissText = "إلغاء",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // من تاريخ
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showStartPicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "من تاريخ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDate(startTimestamp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // إلى تاريخ
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showEndPicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "إلى تاريخ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatDate(endTimestamp),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        MosaribDatePickerDialog(
            title = "من تاريخ",
            initialTimestamp = startTimestamp,
            onDismiss = { showStartPicker = false },
            onDateSelected = { ts ->
                startTimestamp = ts
                if (endTimestamp < ts) endTimestamp = ts
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        MosaribDatePickerDialog(
            title = "إلى تاريخ",
            initialTimestamp = endTimestamp,
            minTimestamp = startTimestamp,
            onDismiss = { showEndPicker = false },
            onDateSelected = { ts ->
                endTimestamp = ts
                showEndPicker = false
            }
        )
    }
}

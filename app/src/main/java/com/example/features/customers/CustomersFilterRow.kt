package com.example.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.ui.DropdownOption
import com.example.core.ui.FilterClearAllChip
import com.example.core.ui.FilterDropdownChip
import com.example.core.ui.MosaribMenuItem

/**
 * رأس قائمة الحسابات: حقل البحث ثم صف شرائح الفلاتر المنسدلة.
 */
@Composable
internal fun CustomersListHeader(
    isCompact: Boolean,
    isPinned: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filterRow: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = if (!isCompact && isPinned) 3.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isCompact) 4.dp else 6.dp)) {
            // حقل البحث بكامل العرض
            CustomersSearchField(
                isCompact = isCompact,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )

            Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 6.dp))

            // صف شرائح الفلاتر المنسدلة
            filterRow()
        }
    }
}

@Composable
private fun CustomersSearchField(
    isCompact: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val searchTextStyle = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 34.dp else 38.dp)
            .clip(RoundedCornerShape(if (isCompact) 10.dp else 12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isCompact) 0.45f else 0.5f))
            .padding(horizontal = if (isCompact) 8.dp else 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (isCompact) 15.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 6.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "بحث بالاسم، المزرعة، أو الهاتف...",
                        style = searchTextStyle.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        ),
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { onSearchQueryChange(it) },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = searchTextStyle.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customers_search_input")
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onSearchQueryChange("") },
                    modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "مسح البحث",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isCompact) 13.dp else 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * صف شرائح الفلاتر المنسدلة لشاشة العملاء:
 * 1. الترتيب
 * 2. نوع الحساب
 * 3. الحالة
 * 4. المجموعة
 * 5. زر "مسح الكل ✕" إذا كان أي منها غير افتراضي
 */
@Composable
fun CustomersFilterChipsRow(
    sortType: CustomerSort,
    accountFilter: AccountFilter,
    balanceFilter: CustomerBalanceFilter,
    selectedGroup: String?,
    customGroups: List<String>,
    rawCustomers: List<CustomerWithBalance>,
    onSelectSort: (CustomerSort) -> Unit,
    onSelectAccount: (AccountFilter) -> Unit,
    onSelectBalance: (CustomerBalanceFilter) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onAddNewGroup: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortOptions = listOf(
        DropdownOption(CustomerSort.NAME, "الاسم (أ-ي)"),
        DropdownOption(CustomerSort.NAME_DESC, "الاسم (ي-أ)"),
        DropdownOption(CustomerSort.HIGHEST_DEBT, "الأعلى رصيداً"),
        DropdownOption(CustomerSort.LOWEST_DEBT, "الأقل رصيداً"),
        DropdownOption(CustomerSort.NEWEST, "الأحدث إضافة"),
        DropdownOption(CustomerSort.MOST_WATER_HOURS, "الأكثر ساعات سقي")
    )

    val accountOptions = listOf(
        DropdownOption(AccountFilter.ALL, "الكل"),
        DropdownOption(AccountFilter.FARMERS, "مزارع"),
        DropdownOption(AccountFilter.WELL_OWNERS, "صاحب بئر")
    )

    val balanceOptions = listOf(
        DropdownOption(CustomerBalanceFilter.ALL, "الكل"),
        DropdownOption(CustomerBalanceFilter.DEBTOR, "عليه"),
        DropdownOption(CustomerBalanceFilter.CREDITOR, "له"),
        DropdownOption(CustomerBalanceFilter.SETTLED, "متوازن")
    )

    val groupOptions = buildList<DropdownOption<String?>> {
        add(DropdownOption(null, "الكل"))
        customGroups.forEach { grp ->
            val count = rawCustomers.count { it.customer.customGroup == grp }
            add(DropdownOption(grp, "$grp ($count)"))
        }
        add(DropdownOption("", "بدون مجموعة"))
    }

    val hasActiveFilter = sortType != CustomerSort.NAME ||
            accountFilter != AccountFilter.ALL ||
            balanceFilter != CustomerBalanceFilter.ALL ||
            selectedGroup != null

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. الترتيب
        item(key = "sort") {
            FilterDropdownChip(
                label = "الترتيب",
                selected = sortType,
                options = sortOptions,
                isDefault = sortType == CustomerSort.NAME,
                onSelect = onSelectSort
            )
        }

        // 2. نوع الحساب
        item(key = "account") {
            FilterDropdownChip(
                label = "نوع الحساب",
                selected = accountFilter,
                options = accountOptions,
                isDefault = accountFilter == AccountFilter.ALL,
                onSelect = onSelectAccount
            )
        }

        // 3. الحالة
        item(key = "balance") {
            FilterDropdownChip(
                label = "الحالة",
                selected = balanceFilter,
                options = balanceOptions,
                isDefault = balanceFilter == CustomerBalanceFilter.ALL,
                onSelect = onSelectBalance
            )
        }

        // 4. المجموعة
        item(key = "group") {
            FilterDropdownChip(
                label = "المجموعة",
                selected = selectedGroup,
                options = groupOptions,
                isDefault = selectedGroup == null,
                onSelect = onSelectGroup,
                footer = {
                    MosaribMenuItem(
                        text = "مجموعة جديدة",
                        icon = Icons.Default.Add,
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = onAddNewGroup
                    )
                }
            )
        }

        // 5. شريحة إضافة مجموعة (+ مجموعة) في نهاية صف الشرائح
        item(key = "add_group") {
            AddGroupChip(onClick = onAddNewGroup)
        }

        // 6. زر مسح الكل عند تفعيل أي فلتر
        if (hasActiveFilter) {
            item(key = "clear_all") {
                FilterClearAllChip(onClearAll = onClearAll)
            }
        }
    }
}

/**
 * شريحة إضافة مجموعة سريعة "+ مجموعة" بتصميم متناسق مع شرائح الفلاتر.
 */
@Composable
fun AddGroupChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "مجموعة",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

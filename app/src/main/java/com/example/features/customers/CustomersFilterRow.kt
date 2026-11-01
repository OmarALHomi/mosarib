package com.example.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.MosaribFilterChip
import com.example.core.ui.MosaribFilterRow
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal

/**
 * رأس قائمة الحسابات: حقل البحث ثم شريط الفلاتر
 * (نسخة مضغوطة للوضع الأفقي، ونسخة أوسع للوضع الرأسي مع ظل عند التثبيت)
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
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = if (isCompact) 2.dp else 4.dp)) {
            // حقل البحث بمظهر واسع وأنيق ومستغل للمساحة السابقة
            CustomersSearchField(
                isCompact = isCompact,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange
            )

            Spacer(modifier = Modifier.height(if (isCompact) 3.dp else 5.dp))

            // شريط الفلاتر الموحّد (نوع الحساب | حالة الرصيد | المجموعة | الترتيب)
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
    val fontSize = if (isCompact) 11.5.sp else 12.sp

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
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            fontSize = fontSize
                        ),
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { onSearchQueryChange(it) },
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = fontSize
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
                        contentDescription = "مسح",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isCompact) 13.dp else 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun CustomersDropdownFilterRow(
    accountFilter: AccountFilter,
    balanceFilter: CustomerBalanceFilter,
    selectedGroup: String?,
    customGroups: List<String>,
    sortType: CustomerSort,
    rawCustomers: List<CustomerWithBalance>,
    debtorCount: Int,
    creditorCount: Int,
    settledCount: Int,
    archivedCount: Int = 0,
    onSelectAccountFilter: (AccountFilter) -> Unit,
    onSelectBalanceFilter: (CustomerBalanceFilter) -> Unit,
    onSelectGroup: (String?) -> Unit,
    onSelectSortType: (CustomerSort) -> Unit,
    onAddGroupClick: () -> Unit,
    onManageGroupsClick: () -> Unit,
    onOpenRecycleBin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    MosaribFilterRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        // 1. فلتر النوع (الكل، مزارعين، آبار)
        AccountTypeFilterChip(
            accountFilter = accountFilter,
            rawCustomers = rawCustomers,
            onSelectAccountFilter = onSelectAccountFilter
        )

        // 2. فلتر الحالة / الرصيد (الكل، عليه، له، متوازن)
        BalanceFilterChip(
            balanceFilter = balanceFilter,
            debtorCount = debtorCount,
            creditorCount = creditorCount,
            settledCount = settledCount,
            onSelectBalanceFilter = onSelectBalanceFilter
        )

        // 3. فلتر المجموعة
        GroupFilterChip(
            selectedGroup = selectedGroup,
            customGroups = customGroups,
            rawCustomers = rawCustomers,
            archivedCount = archivedCount,
            onSelectGroup = onSelectGroup,
            onManageGroupsClick = onManageGroupsClick,
            onOpenRecycleBin = onOpenRecycleBin
        )

        // 4. زر إضافة مجموعة (+ مجموعة)
        AddGroupChip(onClick = onAddGroupClick)

        // فاصل
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(18.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )

        // 5. الترتيب
        SortFilterChip(
            sortType = sortType,
            onSelectSortType = onSelectSortType
        )
    }
}

@Composable
private fun AccountTypeFilterChip(
    accountFilter: AccountFilter,
    rawCustomers: List<CustomerWithBalance>,
    onSelectAccountFilter: (AccountFilter) -> Unit
) {
    var showTypeMenu by remember { mutableStateOf(false) }

    Box {
        MosaribFilterChip(
            selected = accountFilter != AccountFilter.ALL,
            onClick = { showTypeMenu = true },
            label = when (accountFilter) {
                AccountFilter.ALL -> "النوع: الكل"
                AccountFilter.FARMERS -> "مزارعون"
                AccountFilter.WELL_OWNERS -> "أصحاب الآبار"
            },
            isCompact = true,
            leadingIcon = {
                when (accountFilter) {
                    AccountFilter.ALL -> null
                    AccountFilter.FARMERS -> Icon(Icons.Default.Park, null, modifier = Modifier.size(14.dp), tint = if (accountFilter != AccountFilter.ALL) Color.White else Color(0xFF2E7D32))
                    AccountFilter.WELL_OWNERS -> Icon(Icons.Default.WaterDrop, null, modifier = Modifier.size(14.dp), tint = if (accountFilter != AccountFilter.ALL) Color.White else PrimaryTeal)
                }
            }
        )
        DropdownMenu(
            expanded = showTypeMenu,
            onDismissRequest = { showTypeMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("الكل (${rawCustomers.size})") },
                onClick = {
                    onSelectAccountFilter(AccountFilter.ALL)
                    showTypeMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("مزارعون (${rawCustomers.count { !it.customer.isWellOwner }})") },
                leadingIcon = { Icon(Icons.Default.Park, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp)) },
                onClick = {
                    onSelectAccountFilter(AccountFilter.FARMERS)
                    showTypeMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("أصحاب الآبار (${rawCustomers.count { it.customer.isWellOwner }})") },
                leadingIcon = { Icon(Icons.Default.WaterDrop, null, tint = PrimaryTeal, modifier = Modifier.size(18.dp)) },
                onClick = {
                    onSelectAccountFilter(AccountFilter.WELL_OWNERS)
                    showTypeMenu = false
                }
            )
        }
    }
}

@Composable
private fun BalanceFilterChip(
    balanceFilter: CustomerBalanceFilter,
    debtorCount: Int,
    creditorCount: Int,
    settledCount: Int,
    onSelectBalanceFilter: (CustomerBalanceFilter) -> Unit
) {
    var showBalanceMenu by remember { mutableStateOf(false) }

    Box {
        MosaribFilterChip(
            selected = balanceFilter != CustomerBalanceFilter.ALL,
            onClick = { showBalanceMenu = true },
            label = when (balanceFilter) {
                CustomerBalanceFilter.ALL -> "الحالة: الكل"
                CustomerBalanceFilter.DEBTOR -> "عليه ($debtorCount)"
                CustomerBalanceFilter.CREDITOR -> "له ($creditorCount)"
                CustomerBalanceFilter.SETTLED -> "متوازن ($settledCount)"
            },
            isCompact = true,
            leadingIcon = {
                when (balanceFilter) {
                    CustomerBalanceFilter.ALL -> null
                    CustomerBalanceFilter.DEBTOR -> Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (balanceFilter != CustomerBalanceFilter.ALL) Color.White else Color(0xFFE53935)))
                    CustomerBalanceFilter.CREDITOR -> Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (balanceFilter != CustomerBalanceFilter.ALL) Color.White else Color(0xFF2E7D32)))
                    CustomerBalanceFilter.SETTLED -> Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(13.dp), tint = if (balanceFilter != CustomerBalanceFilter.ALL) Color.White else AccentEmerald)
                }
            }
        )
        DropdownMenu(
            expanded = showBalanceMenu,
            onDismissRequest = { showBalanceMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("الكل") },
                onClick = {
                    onSelectBalanceFilter(CustomerBalanceFilter.ALL)
                    showBalanceMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("عليه ($debtorCount)") },
                leadingIcon = { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFE53935))) },
                onClick = {
                    onSelectBalanceFilter(CustomerBalanceFilter.DEBTOR)
                    showBalanceMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("له ($creditorCount)") },
                leadingIcon = { Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2E7D32))) },
                onClick = {
                    onSelectBalanceFilter(CustomerBalanceFilter.CREDITOR)
                    showBalanceMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("متوازن ($settledCount)") },
                leadingIcon = { Icon(Icons.Default.CheckCircle, null, tint = AccentEmerald, modifier = Modifier.size(16.dp)) },
                onClick = {
                    onSelectBalanceFilter(CustomerBalanceFilter.SETTLED)
                    showBalanceMenu = false
                }
            )
        }
    }
}

@Composable
private fun GroupFilterChip(
    selectedGroup: String?,
    customGroups: List<String>,
    rawCustomers: List<CustomerWithBalance>,
    archivedCount: Int = 0,
    onSelectGroup: (String?) -> Unit,
    onManageGroupsClick: () -> Unit,
    onOpenRecycleBin: () -> Unit = {}
) {
    var showGroupMenu by remember { mutableStateOf(false) }

    Box {
        MosaribFilterChip(
            selected = selectedGroup != null,
            onClick = { showGroupMenu = true },
            label = selectedGroup ?: "المجموعة: الكل",
            isCompact = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Bookmarks,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = if (selectedGroup != null) Color.White else AccentGold
                )
            }
        )
        DropdownMenu(
            expanded = showGroupMenu,
            onDismissRequest = { showGroupMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("كل المجموعات") },
                onClick = {
                    onSelectGroup(null)
                    showGroupMenu = false
                }
            )
            customGroups.forEach { grp ->
                val count = rawCustomers.count { it.customer.customGroup.trim().equals(grp.trim(), ignoreCase = true) }
                DropdownMenuItem(
                    text = { Text("$grp ($count)") },
                    leadingIcon = { Icon(Icons.Default.Bookmarks, null, tint = AccentGold, modifier = Modifier.size(16.dp)) },
                    onClick = {
                        onSelectGroup(grp)
                        showGroupMenu = false
                    }
                )
            }
            if (customGroups.isNotEmpty()) {
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("إدارة المجموعات ⚙️", fontWeight = FontWeight.Bold) },
                onClick = {
                    showGroupMenu = false
                    onManageGroupsClick()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "سلة المحذوفات 🗑️",
                            fontWeight = FontWeight.Bold,
                            color = if (archivedCount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                        if (archivedCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$archivedCount",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                },
                onClick = {
                    showGroupMenu = false
                    onOpenRecycleBin()
                }
            )
        }
    }
}

@Composable
private fun AddGroupChip(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "مجموعة",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun SortFilterChip(
    sortType: CustomerSort,
    onSelectSortType: (CustomerSort) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Box {
        MosaribFilterChip(
            selected = sortType != CustomerSort.NAME,
            onClick = { showSortMenu = true },
            label = when (sortType) {
                CustomerSort.NAME -> "الاسم"
                CustomerSort.HIGHEST_DEBT -> "الأعلى رصيداً"
                CustomerSort.MOST_WATER_HOURS -> "الأكثر ساعات"
            },
            isCompact = true,
            leadingIcon = {
                Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(13.dp))
            }
        )
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("الاسم") },
                onClick = {
                    onSelectSortType(CustomerSort.NAME)
                    showSortMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("الأعلى رصيداً") },
                onClick = {
                    onSelectSortType(CustomerSort.HIGHEST_DEBT)
                    showSortMenu = false
                }
            )
            DropdownMenuItem(
                text = { Text("الأكثر ساعات") },
                onClick = {
                    onSelectSortType(CustomerSort.MOST_WATER_HOURS)
                    showSortMenu = false
                }
            )
        }
    }
}

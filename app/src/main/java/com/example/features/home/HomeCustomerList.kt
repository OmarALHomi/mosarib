package com.example.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.AmountWithWords
import com.example.core.util.FileSharingHelper
import com.example.features.customers.AccountFilter
import com.example.features.customers.AddEditCustomerBottomSheet
import com.example.features.customers.ArchiveCustomerDialog
import com.example.features.customers.Customer
import com.example.features.customers.CustomerBalanceFilter
import com.example.features.customers.CustomerCardItem
import com.example.features.customers.CustomerSort
import com.example.features.customers.CustomerStatementMessageDialog
import com.example.features.customers.CustomerWithBalance
import com.example.features.customers.CustomersImportExportDialogs
import com.example.features.customers.CustomerGroupEditorDialog
import com.example.features.customers.CustomersViewModel
import com.example.features.customers.ManageGroupsDialog
import com.example.features.customers.ledgerTotalsFor
import com.example.ui.theme.mosarib

/**
 * Simple-mode home body under the action buttons: filter chips, search, customers sorted by last
 * operation, and a fixed totals box. All filtering lives in [CustomersViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCustomerList(
    customersViewModel: CustomersViewModel,
    onNavigateToCustomer: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var totalsBoxHeight by remember { mutableStateOf(110.dp) }
    val config by customersViewModel.appConfig.collectAsStateWithLifecycle()
    val customers by customersViewModel.customersWithBalance.collectAsStateWithLifecycle()
    val balanceFilter by customersViewModel.balanceFilter.collectAsStateWithLifecycle()
    val accountFilter by customersViewModel.accountFilter.collectAsStateWithLifecycle()
    val selectedGroup by customersViewModel.selectedGroup.collectAsStateWithLifecycle()
    val groups by customersViewModel.customGroups.collectAsStateWithLifecycle()
    val query by customersViewModel.searchQuery.collectAsStateWithLifecycle()

    var searchOpen by remember { mutableStateOf(false) }
    var groupMenuOpen by remember { mutableStateOf(false) }
    val rawCustomers by customersViewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    var showManageGroups by remember { mutableStateOf(false) }
    var showGroupEditor by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<String?>(null) }
    var groupEditIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var messageChoice by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    // The view model is shared with the (hidden) Customers tab: sort by last operation while shown,
    // and restore its defaults on leaving so full mode is unchanged.
    DisposableEffect(Unit) {
        customersViewModel.clearFilters()
        customersViewModel.setSortType(CustomerSort.LAST_ACTIVITY)
        onDispose { customersViewModel.clearFilters() }
    }

    val (owedByUs, owedToUs) = remember(customers) { ledgerTotalsFor(customers) }
    val currency = config.currencySymbol

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                listOf(
                    "الكل" to CustomerBalanceFilter.ALL,
                    "لك" to CustomerBalanceFilter.DEBTOR,
                    "عليك" to CustomerBalanceFilter.CREDITOR
                ).forEach { (label, filter) ->
                    FilterChip(
                        selected = balanceFilter == filter,
                        onClick = { customersViewModel.setBalanceFilter(filter) },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
                Box {
                    FilterChip(
                        selected = !selectedGroup.isNullOrBlank(),
                        onClick = { groupMenuOpen = true },
                        label = {
                            Text(
                                (selectedGroup?.takeIf { it.isNotBlank() } ?: "المجموعة") + " ▾",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    )
                    DropdownMenu(expanded = groupMenuOpen, onDismissRequest = { groupMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("كل المجموعات") },
                            onClick = { customersViewModel.setSelectedGroup(null); groupMenuOpen = false }
                        )
                        groups.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g) },
                                onClick = { customersViewModel.setSelectedGroup(g); groupMenuOpen = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("إدارة المجموعات…") },
                            onClick = { groupMenuOpen = false; showManageGroups = true }
                        )
                    }
                }
                listOf(
                    "مزارع" to AccountFilter.FARMERS,
                    "صاحب بئر" to AccountFilter.WELL_OWNERS
                ).forEach { (label, filter) ->
                    FilterChip(
                        selected = accountFilter == filter,
                        onClick = {
                            customersViewModel.setAccountFilter(if (accountFilter == filter) AccountFilter.ALL else filter)
                        },
                        label = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                    )
                }
              }
                IconButton(
                    onClick = {
                        if (searchOpen) customersViewModel.setSearchQuery("")
                        searchOpen = !searchOpen
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (searchOpen) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "بحث",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { customersViewModel.setSearchQuery(it) },
                    placeholder = { Text("ابحث بالاسم أو الهاتف") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                // room (= measured totals box height) so the last card scrolls clear of the box
                contentPadding = PaddingValues(bottom = totalsBoxHeight)
            ) {
                items(customers, key = { it.customer.id }) { item ->
                    CustomerCardItem(
                        item = item,
                        currencySymbol = currency,
                        onClick = { onNavigateToCustomer(item.customer.id) },
                        onCallClick = { FileSharingHelper.makePhoneCall(context, item.customer.phone) },
                        onWhatsAppClick = { messageChoice = item },
                        onPdfStatementClick = { customersViewModel.generateCustomerStatementPdf(item.customer) },
                        onEditClick = { customerToEdit = item.customer },
                        onDeleteClick = { customerToDelete = item.customer }
                    )
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { totalsBoxHeight = with(density) { it.height.toDp() } }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                TotalColumn("لك", owedToUs, currency, MaterialTheme.mosarib.credit, Modifier.weight(1f))
                TotalColumn("عليك", owedByUs, currency, MaterialTheme.mosarib.debt, Modifier.weight(1f))
            }
        }
    }

    CustomersImportExportDialogs(viewModel = customersViewModel)
    if (showManageGroups) {
        ManageGroupsDialog(
            groups = groups,
            allCustomers = rawCustomers,
            onDismiss = { showManageGroups = false },
            onEditGroup = { grp, ids -> showManageGroups = false; groupToEdit = grp; groupEditIds = ids; showGroupEditor = true },
            onRenameGroup = { old, new -> customersViewModel.renameGroup(old, new) },
            onDeleteGroup = { customersViewModel.deleteGroup(it) },
            onAddNewGroup = { showManageGroups = false; groupToEdit = null; groupEditIds = emptySet(); showGroupEditor = true }
        )
    }
    if (showGroupEditor) {
        CustomerGroupEditorDialog(
            groupToEdit = groupToEdit,
            initialMemberIdsForEdit = groupEditIds,
            rawCustomers = rawCustomers,
            onDismiss = { showGroupEditor = false; groupToEdit = null },
            onSave = { name, ids ->
                customersViewModel.saveGroup(name, ids, groupToEdit)
                showGroupEditor = false
                groupToEdit = null
            }
        )
    }
    messageChoice?.let {
        CustomerStatementMessageDialog(item = it, viewModel = customersViewModel, onDismiss = { messageChoice = null })
    }
    customerToEdit?.let { c ->
        AddEditCustomerBottomSheet(
            initialCustomer = c,
            existingGroups = groups,
            currencySymbol = currency,
            onDismiss = { customerToEdit = null },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup ->
                customersViewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup)
            }
        )
    }
    customerToDelete?.let { c ->
        ArchiveCustomerDialog(
            customer = c,
            onConfirm = { customersViewModel.deleteCustomer(c); customerToDelete = null },
            onDismiss = { customerToDelete = null }
        )
    }
}

@Composable
private fun TotalColumn(
    label: String,
    amount: Double,
    currency: String,
    color: Color,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$label: ", style = MaterialTheme.typography.labelLarge.copy(color = color, fontWeight = FontWeight.Bold))
        AmountWithWords(
            amount = amount,
            currency = currency,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = color
        )
    }
}

/** Small icon + one-word button for the single action row of simple-mode home. */
@Composable
fun HomeCompactActionButton(
    title: String,
    icon: ImageVector,
    bg: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge.copy(color = tint, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

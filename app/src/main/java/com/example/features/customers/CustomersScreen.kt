package com.example.features.customers

import android.content.res.Configuration
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.util.FileSharingHelper
import com.example.features.sessions.SessionsViewModel
import com.example.features.vouchers.VouchersViewModel

@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onNavigateToDetail: (Long) -> Unit,
    sessionsViewModel: SessionsViewModel? = null,
    vouchersViewModel: VouchersViewModel? = null,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val rawCustomers by viewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    val customersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val accountFilter by viewModel.accountFilter.collectAsStateWithLifecycle()
    val balanceFilter by viewModel.balanceFilter.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val customGroups by viewModel.customGroups.collectAsStateWithLifecycle()
    val archivedCustomers by viewModel.archivedCustomers.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val allSessions = vouchersViewModel?.allSessions?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val context = LocalContext.current

    var showQuickAddChoice by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddSessionSheet by remember { mutableStateOf(false) }
    var showAddVoucherSheet by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var messageChoiceCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showManageGroupsDialog by remember { mutableStateOf(false) }
    var showRecycleBinDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<String?>(null) }
    var initialMemberIdsForEdit by remember { mutableStateOf<Set<Long>>(emptySet()) }

    // حساب المركز المالي التفاعلي للشريط السفلي (دفتر الحسابات) تفاعلياً مع الفلتر المختار
    val (cardOwedByUs, cardOwedToUs) = remember(rawCustomers, accountFilter) {
        computeLedgerTotals(rawCustomers, accountFilter)
    }

    // إحصائيات الفلترة بالرصيد (عليه / له / مسدد) المتجاوبة مع الفئة والمجموعة المحددة
    val baseListForBadges = remember(rawCustomers, accountFilter, selectedGroup) {
        filterForBalanceBadges(rawCustomers, accountFilter, selectedGroup)
    }
    val debtorCount = remember(baseListForBadges) {
        baseListForBadges.count { it.receivableBalance > 0.001 }
    }
    val creditorCount = remember(baseListForBadges) {
        baseListForBadges.count { it.payableBalance > 0.001 || it.receivableBalance < -0.001 }
    }
    val settledCount = remember(baseListForBadges) {
        baseListForBadges.count {
            it.receivableBalance <= 0.001 && it.receivableBalance >= -0.001 && it.payableBalance <= 0.001
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidthDp = configuration.screenWidthDp
    val isWideLandscape = (isLandscape || configuration.screenHeightDp < 520) && screenWidthDp >= 600
    val isCompactHeight = isWideLandscape

    val listState = rememberLazyListState()
    val isStickyPinned by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24 }
    }

    // شريط الفلاتر الموحّد (نوع الحساب | حالة الرصيد | المجموعة | الترتيب)
    val filterRow: @Composable () -> Unit = {
        CustomersDropdownFilterRow(
            accountFilter = accountFilter,
            balanceFilter = balanceFilter,
            selectedGroup = selectedGroup,
            customGroups = customGroups,
            sortType = sortType,
            rawCustomers = rawCustomers,
            debtorCount = debtorCount,
            creditorCount = creditorCount,
            settledCount = settledCount,
            onSelectAccountFilter = { viewModel.setAccountFilter(it) },
            onSelectBalanceFilter = { viewModel.setBalanceFilter(it) },
            onSelectGroup = { viewModel.setSelectedGroup(it) },
            onSelectSortType = { viewModel.setSortType(it) },
            onAddGroupClick = { showAddGroupDialog = true },
            onManageGroupsClick = { showManageGroupsDialog = true },
            archivedCount = archivedCustomers.size,
            onOpenRecycleBin = { showRecycleBinDialog = true }
        )
    }

    CustomersListContent(
        listState = listState,
        isCompactHeight = isCompactHeight,
        isStickyPinned = isStickyPinned,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        filterRow = filterRow,
        customers = customersWithBalance,
        balanceFilter = balanceFilter,
        accountFilter = accountFilter,
        currencySymbol = config.currencySymbol,
        owedByUs = cardOwedByUs,
        owedToUs = cardOwedToUs,
        toast = toast,
        onCustomerClick = { onNavigateToDetail(it.customer.id) },
        onCallClick = { FileSharingHelper.makePhoneCall(context, it.customer.phone) },
        onWhatsAppClick = { messageChoiceCustomer = it },
        onPdfStatementClick = { viewModel.generateCustomerStatementPdf(it.customer) },
        onEditClick = { customerToEdit = it.customer },
        onDeleteClick = { customerToDelete = it.customer },
        onQuickAddClick = { showQuickAddChoice = true },
        onDismissToast = { viewModel.dismissToast() },
        modifier = modifier
    )

    // استيراد إكسل + مؤشر القراءة + نافذة فتح / مشاركة الملف الجاهز
    CustomersImportExportDialogs(viewModel = viewModel)

    // SMS / WhatsApp Choice Dialog
    messageChoiceCustomer?.let { item ->
        CustomerStatementMessageDialog(
            item = item,
            viewModel = viewModel,
            onDismiss = { messageChoiceCustomer = null }
        )
    }

    // Add / Edit Customer Sheet
    if (showAddSheet || customerToEdit != null) {
        AddEditCustomerBottomSheet(
            initialCustomer = customerToEdit,
            existingGroups = customGroups,
            currencySymbol = config.currencySymbol,
            onDismiss = {
                showAddSheet = false
                customerToEdit = null
            },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner, customGroup)
            }
        )
    }

    // Quick Add Choice Bottom Sheet (عميل جديد / سقي / معاملة)
    if (showQuickAddChoice) {
        QuickAddChoiceSheet(
            onDismiss = { showQuickAddChoice = false },
            onAddCustomer = {
                showQuickAddChoice = false
                showAddSheet = true
            },
            onAddSession = {
                showQuickAddChoice = false
                showAddSessionSheet = true
            },
            onAddVoucher = {
                showQuickAddChoice = false
                showAddVoucherSheet = true
            }
        )
    }

    // Sheet: Add Manual Session (سقي)
    if (showAddSessionSheet) {
        QuickAddSessionSheet(
            customers = rawCustomers.map { it.customer },
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            sessionsViewModel = sessionsViewModel,
            onDismiss = { showAddSessionSheet = false },
            onSaved = {
                showAddSessionSheet = false
                viewModel.showToast("تم تسجيل دورة السقي بنجاح وتحديث الكشوفات")
            }
        )
    }

    // Sheet: Add Voucher (معاملة)
    if (showAddVoucherSheet) {
        QuickAddVoucherSheet(
            customers = rawCustomers.map { it.customer },
            allSessions = allSessions,
            currencySymbol = config.currencySymbol,
            vouchersViewModel = vouchersViewModel,
            onDismiss = { showAddVoucherSheet = false },
            onSaved = {
                showAddVoucherSheet = false
                viewModel.showToast("تم تسجيل المعاملة بنجاح وتحديث الحسابات")
            }
        )
    }

    // Delete confirmation
    customerToDelete?.let { cust ->
        ArchiveCustomerDialog(
            customer = cust,
            onConfirm = {
                viewModel.deleteCustomer(cust)
                customerToDelete = null
            },
            onDismiss = { customerToDelete = null }
        )
    }

    // Add / Edit Custom Group Dialog
    if (showAddGroupDialog || groupToEdit != null) {
        CustomerGroupEditorDialog(
            groupToEdit = groupToEdit,
            initialMemberIdsForEdit = initialMemberIdsForEdit,
            rawCustomers = rawCustomers,
            onDismiss = {
                showAddGroupDialog = false
                groupToEdit = null
                initialMemberIdsForEdit = emptySet()
            },
            onSave = { name, selectedIds ->
                viewModel.saveGroup(name, selectedIds, groupToEdit)
                showAddGroupDialog = false
                groupToEdit = null
                initialMemberIdsForEdit = emptySet()
            }
        )
    }

    // Recycle Bin Dialog (سلة المحذوفات)
    if (showRecycleBinDialog) {
        RecycleBinDialog(
            archivedCustomers = archivedCustomers,
            onRestore = { viewModel.restoreCustomer(it.id) },
            onPermanentlyDelete = { viewModel.permanentlyDeleteCustomer(it.id) },
            onEmptyBin = { viewModel.emptyRecycleBin() },
            onDismiss = { showRecycleBinDialog = false }
        )
    }

    // Manage Groups Dialog
    if (showManageGroupsDialog) {
        ManageGroupsDialog(
            groups = customGroups,
            allCustomers = rawCustomers,
            onDismiss = { showManageGroupsDialog = false },
            onEditGroup = { grp, memberIds ->
                showManageGroupsDialog = false
                groupToEdit = grp
                initialMemberIdsForEdit = memberIds
            },
            onRenameGroup = { oldName, newName ->
                viewModel.renameGroup(oldName, newName)
            },
            onDeleteGroup = { grp ->
                viewModel.deleteGroup(grp)
            },
            onAddNewGroup = {
                showManageGroupsDialog = false
                groupToEdit = null
                initialMemberIdsForEdit = emptySet()
                showAddGroupDialog = true
            }
        )
    }
}

/**
 * إجمالي (عليك / لك) للشريط المالي السفلي حسب نوع الحساب المختار
 */
private fun computeLedgerTotals(
    rawCustomers: List<CustomerWithBalance>,
    accountFilter: AccountFilter
): Pair<Double, Double> = when (accountFilter) {
    AccountFilter.ALL -> {
        val owedBy = rawCustomers.sumOf {
            if (it.customer.isWellOwner) it.payableBalance
            else if (it.receivableBalance < 0) -it.receivableBalance
            else 0.0
        }
        val owedTo = rawCustomers.sumOf { it.receivableBalance.coerceAtLeast(0.0) }
        Pair(owedBy, owedTo)
    }
    AccountFilter.FARMERS -> {
        val farmers = rawCustomers.filter { !it.customer.isWellOwner }
        val owedBy = farmers.sumOf { if (it.receivableBalance < 0) -it.receivableBalance else 0.0 }
        val owedTo = farmers.sumOf { it.receivableBalance.coerceAtLeast(0.0) }
        Pair(owedBy, owedTo)
    }
    AccountFilter.WELL_OWNERS -> {
        val owners = rawCustomers.filter { it.customer.isWellOwner }
        val owedBy = owners.sumOf { it.payableBalance }
        val owedTo = owners.sumOf { it.receivableBalance.coerceAtLeast(0.0) }
        Pair(owedBy, owedTo)
    }
}

/**
 * القائمة الأساسية لعدّادات حالة الرصيد (بعد تطبيق نوع الحساب والمجموعة فقط)
 */
private fun filterForBalanceBadges(
    rawCustomers: List<CustomerWithBalance>,
    accountFilter: AccountFilter,
    selectedGroup: String?
): List<CustomerWithBalance> {
    val byAcc = when (accountFilter) {
        AccountFilter.ALL -> rawCustomers
        AccountFilter.FARMERS -> rawCustomers.filter { !it.customer.isWellOwner }
        AccountFilter.WELL_OWNERS -> rawCustomers.filter { it.customer.isWellOwner }
    }
    return if (selectedGroup.isNullOrBlank()) byAcc
    else byAcc.filter { it.customer.customGroup.trim().equals(selectedGroup.trim(), ignoreCase = true) }
}

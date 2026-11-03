package com.example.features.customers

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.core.voice.CustomerMatcher
import com.example.core.voice.VoiceRecognitionHelper
import com.example.core.voice.VoiceDialogType
import com.example.core.voice.VoiceSupportDialogs


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
    var voiceDialogType by remember { mutableStateOf(VoiceDialogType.NONE) }
    val searchVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS).orEmpty()
            val spoken = matches.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotBlank()) {
                val ranked = CustomerMatcher.match(spoken, rawCustomers.map { it.customer })
                val best = ranked.firstOrNull()
                if (best != null && best.score >= 0.60f) {
                    viewModel.setSearchQuery(best.customer.name)
                } else {
                    viewModel.setSearchQuery(spoken)
                }
            }
        }
    }

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
    val isFilterActive = searchQuery.isNotBlank() ||
        accountFilter != AccountFilter.ALL ||
        balanceFilter != CustomerBalanceFilter.ALL ||
        !selectedGroup.isNullOrBlank()

    val filterHint = if (isFilterActive) "للمعروضين (${customersWithBalance.size})" else null

    // حساب المركز المالي التفاعلي للشريط السفلي (دفتر الحسابات) تفاعلياً مع القائمة المعروضة
    val (cardOwedByUs, cardOwedToUs) = remember(customersWithBalance) {
        ledgerTotalsFor(customersWithBalance)
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

    // صف شرائح الفلاتر المنسدلة المحدث (الترتيب، نوع الحساب، الحالة، المجموعة)
    val filterRow: @Composable () -> Unit = {
        CustomersFilterChipsRow(
            sortType = sortType,
            accountFilter = accountFilter,
            balanceFilter = balanceFilter,
            selectedGroup = selectedGroup,
            customGroups = customGroups,
            rawCustomers = rawCustomers,
            onSelectSort = { viewModel.setSortType(it) },
            onSelectAccount = { viewModel.setAccountFilter(it) },
            onSelectBalance = { viewModel.setBalanceFilter(it) },
            onSelectGroup = { viewModel.setSelectedGroup(it) },
            onAddNewGroup = { showAddGroupDialog = true },
            onClearAll = { viewModel.clearFilters() }
        )
    }

    CustomersListContent(
        listState = listState,
        isCompactHeight = isCompactHeight,
        isStickyPinned = isStickyPinned,
        searchQuery = searchQuery,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onVoiceSearchClick = {
            VoiceRecognitionHelper.launchRecognition(
                context = context,
                launcher = searchVoiceLauncher,
                onShowDialog = { voiceDialogType = it },
                prompt = "ابحث بالصوت..."
            )
        },
        filterRow = filterRow,
        customers = customersWithBalance,
        balanceFilter = balanceFilter,
        accountFilter = accountFilter,
        currencySymbol = config.currencySymbol,
        owedByUs = cardOwedByUs,
        owedToUs = cardOwedToUs,
        filterHint = filterHint,
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

    VoiceSupportDialogs(
        dialogType = voiceDialogType,
        onDismiss = { voiceDialogType = VoiceDialogType.NONE }
    )
}

/**
 * إجمالي (عليك / لك) للشريط المالي من قائمة العملاء المعروضة
 * - عليك (owedBy): مستحقات أصحاب الآبار (payable) + المبالغ الدائنة للعملاء (سالب receivable)
 * - لك (owedTo): ديون العملاء (موجب receivable)
 */
fun ledgerTotalsFor(customers: List<CustomerWithBalance>): Pair<Double, Double> {
    if (customers.isEmpty()) return Pair(0.0, 0.0)
    val owedBy = customers.sumOf {
        if (it.customer.isWellOwner) it.payableBalance
        else if (it.receivableBalance < 0) -it.receivableBalance
        else 0.0
    }
    val owedTo = customers.sumOf { it.receivableBalance.coerceAtLeast(0.0) }
    return Pair(owedBy, owedTo)
}

/**
 * إجمالي (عليك / لك) للشريط المالي السفلي حسب نوع الحساب المختار (للتوافق واختبارات التطابق)
 */
fun computeLedgerTotals(
    rawCustomers: List<CustomerWithBalance>,
    accountFilter: AccountFilter
): Pair<Double, Double> = when (accountFilter) {
    AccountFilter.ALL -> ledgerTotalsFor(rawCustomers)
    AccountFilter.FARMERS -> ledgerTotalsFor(rawCustomers.filter { !it.customer.isWellOwner })
    AccountFilter.WELL_OWNERS -> ledgerTotalsFor(rawCustomers.filter { it.customer.isWellOwner })
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

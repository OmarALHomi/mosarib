package com.example.features.customers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Close
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.MosaribFilterChip
import com.example.core.ui.MosaribFilterRow
import com.example.core.ui.MosaribTextField
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.ui.StatBoxCard
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val rawCustomers by viewModel.rawCustomersWithBalance.collectAsStateWithLifecycle()
    val customersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val accountFilter by viewModel.accountFilter.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddSheet by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var messageChoiceCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }

    val importDrafts by viewModel.importDrafts.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.startImportFromUri(context, uri)
        }
    }

    // حساب المركز المالي التفاعلي للشريط السفلي (دفتر الحسابات) تفاعلياً مع الفلتر المختار
    val (cardOwedByUs, cardOwedToUs) = remember(rawCustomers, accountFilter) {
        when (accountFilter) {
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
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // 1. شريط تبويبات التصنيف الرئيسي (الكل / المزارعون / أصحاب الآبار) + البحث والفرز
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    // تبويبات الحسابات الموحدة
                    MosaribFilterRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        MosaribFilterChip(
                            selected = accountFilter == AccountFilter.ALL,
                            onClick = { viewModel.setAccountFilter(AccountFilter.ALL) },
                            label = "الكل",
                            badgeCount = rawCustomers.size
                        )

                        MosaribFilterChip(
                            selected = accountFilter == AccountFilter.FARMERS,
                            onClick = { viewModel.setAccountFilter(AccountFilter.FARMERS) },
                            label = "المزارعون",
                            badgeCount = rawCustomers.count { !it.customer.isWellOwner },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Agriculture,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (accountFilter == AccountFilter.FARMERS) Color.White else PrimaryTeal
                                )
                            }
                        )

                        MosaribFilterChip(
                            selected = accountFilter == AccountFilter.WELL_OWNERS,
                            onClick = { viewModel.setAccountFilter(AccountFilter.WELL_OWNERS) },
                            label = "أصحاب الآبار",
                            badgeCount = rawCustomers.count { it.customer.isWellOwner },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (accountFilter == AccountFilter.WELL_OWNERS) Color.White else PrimaryTeal
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // حقل البحث مع أزرار استيراد وتصدير إكسل
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MosaribTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = "بحث بالاسم، المزرعة، أو الهاتف...",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            } else null,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("customers_search_input"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        // زر استيراد إكسل
                        Surface(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    excelPickerLauncher.launch(
                                        arrayOf(
                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            "application/vnd.ms-excel",
                                            "text/csv",
                                            "text/comma-separated-values",
                                            "text/plain",
                                            "*/*"
                                        )
                                    )
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "استيراد من إكسل",
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // زر تصدير إكسل
                        Box {
                            Surface(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showExportMenu = true },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = "تصدير إلى إكسل",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("تصدير إلى إكسل (XLSX)", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null, tint = PrimaryTeal) },
                                    onClick = {
                                        showExportMenu = false
                                        viewModel.exportCustomersToExcel(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("تصدير كملف CSV", fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryTeal) },
                                    onClick = {
                                        showExportMenu = false
                                        viewModel.exportCustomersToCsv(context)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // خيارات الفرز الموحدة
                    MosaribFilterRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        MosaribFilterChip(
                            selected = sortType == CustomerSort.NAME,
                            onClick = { viewModel.setSortType(CustomerSort.NAME) },
                            label = "أبجدياً (الاسم)"
                        )
                        MosaribFilterChip(
                            selected = sortType == CustomerSort.HIGHEST_DEBT,
                            onClick = { viewModel.setSortType(CustomerSort.HIGHEST_DEBT) },
                            label = "الأعلى رصيداً"
                        )
                        MosaribFilterChip(
                            selected = sortType == CustomerSort.MOST_WATER_HOURS,
                            onClick = { viewModel.setSortType(CustomerSort.MOST_WATER_HOURS) },
                            label = "الأكثر استهلاكاً / ضخاً"
                        )
                    }
                }
            }

            // 2. قائمة الحسابات المضغوطة (دفتر الحسابات)
            if (customersWithBalance.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = if (accountFilter == AccountFilter.WELL_OWNERS) "لا يوجد أصحاب آبار مسجلين" else "لا يوجد عملاء مضافين",
                        description = "اضغط على زر (+) لإضافة حساب جديد"
                    )
                }
            } else {
                items(customersWithBalance, key = { it.customer.id }) { item ->
                    CustomerCardItem(
                        item = item,
                        currencySymbol = config.currencySymbol,
                        onClick = { onNavigateToDetail(item.customer.id) },
                        onCallClick = { FileSharingHelper.makePhoneCall(context, item.customer.phone) },
                        onWhatsAppClick = { messageChoiceCustomer = item },
                        onPdfStatementClick = { viewModel.generateCustomerStatementPdf(item.customer) },
                        onEditClick = { customerToEdit = item.customer },
                        onDeleteClick = { customerToDelete = item.customer }
                    )
                }
            }
        }

        // 3. الشريط المالي السفلي الثابت (دفتر الحسابات) التفاعلي: عليك | (+) | لك
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // مستحقات عليك (مختلفة تفاعلياً حسب الفلتر المختار)
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        when (accountFilter) {
                            AccountFilter.ALL -> {
                                Text(
                                    text = "عليك",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                            AccountFilter.FARMERS -> {
                                Icon(
                                    imageVector = Icons.Default.Agriculture,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "عليك",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                            AccountFilter.WELL_OWNERS -> {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "عليك",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFFE53935),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        text = Formatters.formatCurrency(cardOwedByUs, config.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    )
                }

                // زر الإضافة السريع (+) الدائري في المنتصف
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = PrimaryTeal,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fab_add_customer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة حساب جديد",
                        modifier = Modifier.size(26.dp)
                    )
                }

                // مستحقات لك (مختلفة تفاعلياً حسب الفلتر المختار)
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (accountFilter) {
                            AccountFilter.ALL -> {
                                Text(
                                    text = "لك",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                            AccountFilter.FARMERS -> {
                                Text(
                                    text = "لك",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.Agriculture,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            AccountFilter.WELL_OWNERS -> {
                                Text(
                                    text = "لك",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32))
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(cardOwedToUs, config.currencySymbol),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        }

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 75.dp)
        )
    }

    // شاشة مراجعة استيراد إكسل الذكية
    importDrafts?.let { drafts ->
        CustomerImportReviewDialog(
            drafts = drafts,
            onDraftUpdated = { viewModel.updateImportDraft(it) },
            onDraftDeleted = { viewModel.removeImportDraft(it) },
            onToggleSelectAll = { viewModel.toggleSelectAllImportDrafts(it) },
            onConfirmImport = { updateExisting -> viewModel.confirmImport(updateExisting) },
            onDismiss = { viewModel.cancelImport() }
        )
    }

    // مؤشر جاري قراءة ملف الإكسل بذكاء
    if (isImporting) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("قراءة ملف الإكسل", fontWeight = FontWeight.Bold) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryTeal)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("يتم فحص الأعمدة والبيانات بذكاء...")
                }
            }
        )
    }

    // PDF Open / Share Dialog
    pdfReady?.let { (file, title) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearPdfReady() },
            title = { Text("كشف الحساب جاهز", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في فتح وعرض كشف الحساب مباشرة أم مشاركته؟") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        com.example.core.util.FileSharingHelper.openPdf(context, file)
                        viewModel.clearPdfReady()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) { Text("فتح / عرض") }
            },
            dismissButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        com.example.core.util.FileSharingHelper.sharePdf(context, file, title)
                        viewModel.clearPdfReady()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) { Text("مشاركة") }
            }
        )
    }

    // SMS / WhatsApp Choice Dialog
    messageChoiceCustomer?.let { item ->
        val msg = viewModel.buildCustomerStatementMessage(item.customer, item)
        SendMessageChoiceDialog(
            recipientName = item.customer.name,
            recipientPhone = item.customer.phone,
            messageText = msg,
            onDismiss = { messageChoiceCustomer = null },
            onSendWhatsApp = {
                messageChoiceCustomer = null
                FileSharingHelper.sendWhatsAppMessage(context, item.customer.phone, msg)
            },
            onSendSms = {
                messageChoiceCustomer = null
                FileSharingHelper.sendSms(context, item.customer.phone, msg)
            }
        )
    }

    // Add / Edit Customer Sheet
    if (showAddSheet || customerToEdit != null) {
        AddEditCustomerBottomSheet(
            initialCustomer = customerToEdit,
            currencySymbol = config.currencySymbol,
            onDismiss = {
                showAddSheet = false
                customerToEdit = null
            },
            onSave = { id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice, isBeneficiary, isWellOwner)
            }
        )
    }

    // Delete confirmation
    customerToDelete?.let { cust ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            title = { Text("أرشفة الحساب", fontWeight = FontWeight.Bold) },
            text = { Text("سيختفي حساب (${cust.name}) من القائمة النشطة، مع الاحتفاظ بجميع المشتريات والسقي والسندات في السجلات والنسخ الاحتياطية.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(cust)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("حذف نهائي")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun CustomerCardItem(
    item: CustomerWithBalance,
    currencySymbol: String,
    onClick: () -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onPdfStatementClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val customer = item.customer
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .shadow(1.5.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // 1. الصف الأول: بيانات العميل (الاسم والمزرعة/الهاتف) + أزرار الإجراءات السريعة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العميل والمزرعة
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (customer.isWellOwner) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE1F5FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "صاحب بئر",
                                tint = Color(0xFF0288D1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryTeal.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customer.name.take(1),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = PrimaryTeal,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (customer.isWellOwner) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE1F5FE))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "صاحب بئر",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF0288D1),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.5.sp
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // شارة عدد العمليات (دفتر الحسابات)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE3F2FD))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${item.totalOperationsCount} حركة",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF1976D2),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            val subInfo = buildList {
                                if (customer.farmName.isNotBlank()) add(customer.farmName)
                                if (customer.phone.isNotBlank()) add(customer.phone)
                            }.joinToString(" • ")

                            if (subInfo.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = subInfo,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // الأزرار الأربعة السريعة (اتصال، واتساب، كشف حساب PDF، خيارات)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (customer.phone.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryTeal.copy(alpha = 0.12f))
                                .clickable { onCallClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "اتصال",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32).copy(alpha = 0.12f))
                                .clickable { onWhatsAppClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "إرسال كشف",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentGold.copy(alpha = 0.14f))
                            .clickable { onPdfStatementClick() },
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "كشف حساب PDF",
                            tint = AccentGold,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { menuExpanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("عرض كشف الحساب") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("تصدير كشف حساب PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = AccentGold) },
                                onClick = {
                                    menuExpanded = false
                                    onPdfStatementClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (customer.isWellOwner) "تعديل حساب البئر" else "تعديل العميل") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("أرشفة الحساب", color = Color(0xFFE53935)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935)) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact summary: well owners show both independent account directions.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (customer.isWellOwner) {
                    Text(
                        text = "شراء ${Formatters.formatDurationShort(item.totalPurchasedMinutes)}  •  سقي له ${Formatters.formatDurationShort(item.totalSoldMinutes)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val payableLabel = when {
                            item.payableBalance > 0 -> "عليك لصاحب البئر"
                            item.payableBalance < 0 -> "لك رصيد عند صاحب البئر"
                            else -> "مستحقات الشراء مسددة"
                        }
                        val payableColor = if (item.payableBalance > 0) Color(0xFFD32F2F) else AccentEmerald
                        val receivableLabel = when {
                            item.receivableBalance > 0 -> "لك على صاحب البئر"
                            item.receivableBalance < 0 -> "عليك له (رصيد)"
                            else -> "سقي مسدد"
                        }
                        val receivableColor = if (item.receivableBalance > 0) Color(0xFFD32F2F) else AccentEmerald
                        Column(modifier = Modifier.weight(1f)) {
                            Text(payableLabel, style = MaterialTheme.typography.labelSmall, color = payableColor, maxLines = 1)
                            Text(
                                Formatters.formatCurrency(kotlin.math.abs(item.payableBalance), currencySymbol),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = payableColor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(receivableLabel, style = MaterialTheme.typography.labelSmall, color = receivableColor, maxLines = 1)
                            Text(
                                Formatters.formatCurrency(kotlin.math.abs(item.receivableBalance), currencySymbol),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold, color = receivableColor),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "الساعات: ${Formatters.formatDurationShort(item.totalSoldMinutes)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            )
                        }
                        val owesUs = item.receivableBalance > 0
                        val label = when {
                            owesUs -> "لك: ${Formatters.formatCurrency(item.receivableBalance, currencySymbol)}"
                            item.receivableBalance < 0 -> "عليه رصيد: ${Formatters.formatCurrency(kotlin.math.abs(item.receivableBalance), currencySymbol)}"
                            else -> "خالص"
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (owesUs) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

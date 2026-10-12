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
import androidx.compose.material.icons.filled.Eco
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
            // 1. شريط الترويسة العلوي (App Bar) مع أزرار الإجراءات (استيراد إكسل، تصدير إكسل، طباعة كشف PDF)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إدارة الحسابات",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // زر استيراد إكسل
                        IconButton(
                            onClick = {
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = "استيراد من إكسل",
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // زر تصدير إكسل
                        Box {
                            IconButton(
                                onClick = { showExportMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "تصدير إلى إكسل",
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
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

                        // زر طباعة كشف PDF الشامل
                        IconButton(
                            onClick = { viewModel.generateAllCustomersReportPdf() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "طباعة كشف PDF",
                                tint = AccentGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 2. شريط الفلاتر المصغرة وحقل البحث المدمج وفلاتر الترتيب
            item {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                    // أولاً: فلاتر الكل / المزارعون / أصحاب الآبار (مصغرة)
                    MosaribFilterRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        MosaribFilterChip(
                            selected = accountFilter == AccountFilter.ALL,
                            onClick = { viewModel.setAccountFilter(AccountFilter.ALL) },
                            label = "الكل",
                            badgeCount = rawCustomers.size,
                            isCompact = true
                        )

                        MosaribFilterChip(
                            selected = accountFilter == AccountFilter.FARMERS,
                            onClick = { viewModel.setAccountFilter(AccountFilter.FARMERS) },
                            label = "المزارعون",
                            badgeCount = rawCustomers.count { !it.customer.isWellOwner },
                            isCompact = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (accountFilter == AccountFilter.FARMERS) Color.White else PrimaryTeal
                                )
                            }
                        )

                        MosaribFilterChip(
                            selected = accountFilter == AccountFilter.WELL_OWNERS,
                            onClick = { viewModel.setAccountFilter(AccountFilter.WELL_OWNERS) },
                            label = "أصحاب الآبار",
                            badgeCount = rawCustomers.count { it.customer.isWellOwner },
                            isCompact = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (accountFilter == AccountFilter.WELL_OWNERS) Color.White else PrimaryTeal
                                )
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ثانياً: حقل البحث مصغر ومدمج بكامل العرض وبدون أزرار ضخمة بجانبه
                    MosaribTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = "بحث بالاسم، المزرعة، أو الهاتف...",
                        isDense = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "مسح",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customers_search_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // ثالثاً: فلاتر الترتيب (مصغرة)
                    MosaribFilterRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        MosaribFilterChip(
                            selected = sortType == CustomerSort.NAME,
                            onClick = { viewModel.setSortType(CustomerSort.NAME) },
                            label = "أبجدياً (الاسم)",
                            isCompact = true
                        )
                        MosaribFilterChip(
                            selected = sortType == CustomerSort.HIGHEST_DEBT,
                            onClick = { viewModel.setSortType(CustomerSort.HIGHEST_DEBT) },
                            label = "الأعلى رصيداً",
                            isCompact = true
                        )
                        MosaribFilterChip(
                            selected = sortType == CustomerSort.MOST_WATER_HOURS,
                            onClick = { viewModel.setSortType(CustomerSort.MOST_WATER_HOURS) },
                            label = "الأكثر استهلاكاً / ضخاً",
                            isCompact = true
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // مستحقات عليك (مختلفة تفاعلياً حسب الفلتر المختار)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
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
                                        imageVector = Icons.Default.Eco,
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

                    // مساحة مخصصة للزر العائم لضمان عدم التداخل والتوسيط التام
                    Spacer(modifier = Modifier.width(56.dp))

                    // مستحقات لك (مختلفة تفاعلياً حسب الفلتر المختار)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
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
                                        imageVector = Icons.Default.Eco,
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

                // زر الإضافة السريع (+) الدائري في المنتصف الهندسي الدقيق للكرت دائماً
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = PrimaryTeal,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .testTag("fab_add_customer")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة حساب جديد",
                        modifier = Modifier.size(26.dp)
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
        val waMsg = viewModel.buildCustomerStatementMessage(item.customer, item, isSms = false)
        val smsMsg = viewModel.buildCustomerStatementMessage(item.customer, item, isSms = true)
        SendMessageChoiceDialog(
            recipientName = item.customer.name,
            recipientPhone = item.customer.phone,
            messageText = waMsg,
            smsMessageText = smsMsg,
            onDismiss = { messageChoiceCustomer = null },
            onSendWhatsApp = {
                messageChoiceCustomer = null
                FileSharingHelper.sendWhatsAppMessage(context, item.customer.phone, waMsg)
            },
            onSendSms = {
                messageChoiceCustomer = null
                FileSharingHelper.sendSms(context, item.customer.phone, smsMsg)
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
            // 1. الصف الأول: أيقونة (صاحب بئر أو عميل) بجانبها اسم العميل مباشرة + أزرار الإجراءات مثل ما هي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // أيقونة صاحب بئر أو عميل بجانب الاسم مباشرة
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (customer.isWellOwner) Color(0xFFE1F5FE)
                                else PrimaryTeal.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (customer.isWellOwner) Icons.Default.WaterDrop else Icons.Default.Eco,
                            contentDescription = if (customer.isWellOwner) "صاحب بئر" else "عميل مزارع",
                            tint = if (customer.isWellOwner) Color(0xFF0288D1) else PrimaryTeal,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // الأزرار مثل ما هي الآن (اتصال، واتساب، كشف PDF، خيارات)
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

            Spacer(modifier = Modifier.height(4.dp))

            // 2. الصف الثاني: الحركات والرقم
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // شارة عدد العمليات
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
                    if (customer.phone.isNotBlank()) add(customer.phone)
                    if (customer.farmName.isNotBlank()) add(customer.farmName)
                }.joinToString(" • ")

                if (subInfo.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subInfo,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // 3. الصف الثالث: المبلغ باللون الأحمر أو الأخضر أو مسدد (لا تعرض عدد الساعات فقط المبلغ بخط واضح وبولد)
            if (customer.isWellOwner) {
                val hasPayable = item.payableBalance != 0.0
                val hasReceivable = item.receivableBalance != 0.0

                if (!hasPayable && !hasReceivable) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "مسدد بالكامل",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 12.5.sp
                            )
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasPayable) {
                            val isDebt = item.payableBalance > 0
                            val pColor = if (isDebt) Color(0xFFE53935) else Color(0xFF2E7D32)
                            val pText = if (isDebt) "عليك: ${Formatters.formatCurrency(item.payableBalance, currencySymbol)}"
                            else "لك رصيد: ${Formatters.formatCurrency(-item.payableBalance, currencySymbol)}"
                            Text(
                                text = pText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = pColor,
                                    fontSize = 13.5.sp
                                )
                            )
                        } else {
                            Text(
                                text = "مستحقات الشراء مسددة",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.5.sp
                                )
                            )
                        }

                        if (hasReceivable) {
                            val isDebt = item.receivableBalance > 0
                            val rColor = if (isDebt) Color(0xFFE53935) else Color(0xFF2E7D32)
                            val rText = if (isDebt) "مطلوب منه: ${Formatters.formatCurrency(item.receivableBalance, currencySymbol)}"
                            else "له رصيد: ${Formatters.formatCurrency(-item.receivableBalance, currencySymbol)}"
                            Text(
                                text = rText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = rColor,
                                    fontSize = 13.5.sp
                                )
                            )
                        }
                    }
                }
            } else {
                // عميل مزارع عادي
                when {
                    item.receivableBalance > 0 -> {
                        Text(
                            text = "مطلوب منه: ${Formatters.formatCurrency(item.receivableBalance, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE53935),
                                fontSize = 14.sp
                            )
                        )
                    }
                    item.receivableBalance < 0 -> {
                        Text(
                            text = "له رصيد متبقي: ${Formatters.formatCurrency(-item.receivableBalance, currencySymbol)}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF2E7D32),
                                fontSize = 14.sp
                            )
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE8F5E9))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "مسدد بالكامل",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

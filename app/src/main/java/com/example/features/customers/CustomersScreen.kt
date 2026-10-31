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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.core.ui.isAppDark
import com.example.core.ui.MosaribTextField
import com.example.core.ui.SendMessageChoiceDialog
import com.example.core.ui.StatBoxCard
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.features.sessions.SessionsViewModel
import com.example.features.sessions.AddEditSessionBottomSheet
import com.example.features.vouchers.VouchersViewModel
import com.example.features.vouchers.AddVoucherBottomSheet
import com.example.features.vouchers.VoucherType

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val allSessions = vouchersViewModel?.allSessions?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val context = LocalContext.current
    val isDark = isAppDark()

    var showQuickAddChoice by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddSessionSheet by remember { mutableStateOf(false) }
    var showAddVoucherSheet by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }
    var messageChoiceCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }

    val importDrafts by viewModel.importDrafts.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

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

    // إحصائيات الفلترة بالرصيد (عليه / له / مسدد) المتجاوبة مع الفئة والمجموعة المحددة
    val baseListForBadges = remember(rawCustomers, accountFilter, selectedGroup) {
        val byAcc = when (accountFilter) {
            AccountFilter.ALL -> rawCustomers
            AccountFilter.FARMERS -> rawCustomers.filter { !it.customer.isWellOwner }
            AccountFilter.WELL_OWNERS -> rawCustomers.filter { it.customer.isWellOwner }
        }
        if (selectedGroup.isNullOrBlank()) byAcc
        else byAcc.filter { it.customer.customGroup.trim().equals(selectedGroup?.trim(), ignoreCase = true) }
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
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isCompactHeight) 80.dp else 120.dp)
        ) {
            // شريط الفلاتر المصغرة وحقل البحث المدمج وفلاتر الترتيب المتجاوبة
            if (isCompactHeight) {
                // في الوضع الأفقي: حقل البحث وشريط الفلاتر ثلاثي الأقسام
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                    .padding(horizontal = 8.dp),
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
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "بحث بالاسم، المزرعة، أو الهاتف...",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                    fontSize = 11.5.sp
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.setSearchQuery(it) },
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 11.5.sp
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("customers_search_input")
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setSearchQuery("") },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

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
                                customGroups.forEach { grp ->
                                    MosaribFilterChip(
                                        selected = selectedGroup == grp,
                                        onClick = {
                                            if (selectedGroup == grp) viewModel.setSelectedGroup(null)
                                            else viewModel.setSelectedGroup(grp)
                                        },
                                        label = grp,
                                        badgeCount = rawCustomers.count { it.customer.customGroup.trim().equals(grp.trim(), ignoreCase = true) },
                                        isCompact = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Bookmarks,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = if (selectedGroup == grp) Color.White else AccentGold
                                            )
                                        }
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                )
                                MosaribFilterChip(
                                    selected = balanceFilter == CustomerBalanceFilter.DEBTOR,
                                    onClick = {
                                        if (balanceFilter == CustomerBalanceFilter.DEBTOR) viewModel.setBalanceFilter(CustomerBalanceFilter.ALL)
                                        else viewModel.setBalanceFilter(CustomerBalanceFilter.DEBTOR)
                                    },
                                    label = "عليه",
                                    badgeCount = debtorCount,
                                    isCompact = true,
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (balanceFilter == CustomerBalanceFilter.DEBTOR) Color.White else (if (isDark) Color(0xFFF87171) else Color(0xFFE53935)))
                                        )
                                    }
                                )
                                MosaribFilterChip(
                                    selected = balanceFilter == CustomerBalanceFilter.CREDITOR,
                                    onClick = {
                                        if (balanceFilter == CustomerBalanceFilter.CREDITOR) viewModel.setBalanceFilter(CustomerBalanceFilter.ALL)
                                        else viewModel.setBalanceFilter(CustomerBalanceFilter.CREDITOR)
                                    },
                                    label = "له",
                                    badgeCount = creditorCount,
                                    isCompact = true,
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (balanceFilter == CustomerBalanceFilter.CREDITOR) Color.White else (if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)))
                                        )
                                    }
                                )
                                MosaribFilterChip(
                                    selected = balanceFilter == CustomerBalanceFilter.SETTLED,
                                    onClick = {
                                        if (balanceFilter == CustomerBalanceFilter.SETTLED) viewModel.setBalanceFilter(CustomerBalanceFilter.ALL)
                                        else viewModel.setBalanceFilter(CustomerBalanceFilter.SETTLED)
                                    },
                                    label = "مسدد",
                                    badgeCount = settledCount,
                                    isCompact = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (balanceFilter == CustomerBalanceFilter.SETTLED) Color.White else AccentEmerald
                                        )
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                )
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
                }
            } else {
                // في الوضع الرأسي: StickyHeader أنيق يضم حقل البحث أولاً ثم شريط الفلاتر ثلاثي الأقسام
                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = if (isStickyPinned) 3.dp else 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                            // حقل البحث بمظهر واسع وأنيق ومستغل للمساحة السابقة
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp),
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
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "بحث بالاسم، المزرعة، أو الهاتف...",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                                    fontSize = 12.sp
                                                ),
                                                maxLines = 1
                                            )
                                        }
                                        BasicTextField(
                                            value = searchQuery,
                                            onValueChange = { viewModel.setSearchQuery(it) },
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 12.sp
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("customers_search_input")
                                        )
                                    }
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.setSearchQuery("") },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(5.dp))

                            // شريط الفلاتر الموحّد ثلاثي الأقسام (نوع الحساب أولاً | حالة الرصيد | الترتيب)
                            MosaribFilterRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                // القسم 1: نوع الحساب
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

                                customGroups.forEach { grp ->
                                    MosaribFilterChip(
                                        selected = selectedGroup == grp,
                                        onClick = {
                                            if (selectedGroup == grp) viewModel.setSelectedGroup(null)
                                            else viewModel.setSelectedGroup(grp)
                                        },
                                        label = grp,
                                        badgeCount = rawCustomers.count { it.customer.customGroup.trim().equals(grp.trim(), ignoreCase = true) },
                                        isCompact = true,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Bookmarks,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = if (selectedGroup == grp) Color.White else AccentGold
                                            )
                                        }
                                    )
                                }

                                // فاصل أول
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                )

                                // القسم 2: حالة الرصيد
                                MosaribFilterChip(
                                    selected = balanceFilter == CustomerBalanceFilter.DEBTOR,
                                    onClick = {
                                        if (balanceFilter == CustomerBalanceFilter.DEBTOR) viewModel.setBalanceFilter(CustomerBalanceFilter.ALL)
                                        else viewModel.setBalanceFilter(CustomerBalanceFilter.DEBTOR)
                                    },
                                    label = "عليه",
                                    badgeCount = debtorCount,
                                    isCompact = true,
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (balanceFilter == CustomerBalanceFilter.DEBTOR) Color.White else (if (isDark) Color(0xFFF87171) else Color(0xFFE53935)))
                                        )
                                    }
                                )
                                MosaribFilterChip(
                                    selected = balanceFilter == CustomerBalanceFilter.CREDITOR,
                                    onClick = {
                                        if (balanceFilter == CustomerBalanceFilter.CREDITOR) viewModel.setBalanceFilter(CustomerBalanceFilter.ALL)
                                        else viewModel.setBalanceFilter(CustomerBalanceFilter.CREDITOR)
                                    },
                                    label = "له",
                                    badgeCount = creditorCount,
                                    isCompact = true,
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (balanceFilter == CustomerBalanceFilter.CREDITOR) Color.White else (if (isDark) Color(0xFF4ADE80) else Color(0xFF2E7D32)))
                                        )
                                    }
                                )
                                MosaribFilterChip(
                                    selected = balanceFilter == CustomerBalanceFilter.SETTLED,
                                    onClick = {
                                        if (balanceFilter == CustomerBalanceFilter.SETTLED) viewModel.setBalanceFilter(CustomerBalanceFilter.ALL)
                                        else viewModel.setBalanceFilter(CustomerBalanceFilter.SETTLED)
                                    },
                                    label = "مسدد",
                                    badgeCount = settledCount,
                                    isCompact = true,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(13.dp),
                                            tint = if (balanceFilter == CustomerBalanceFilter.SETTLED) Color.White else AccentEmerald
                                        )
                                    }
                                )

                                // فاصل ثانٍ
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                )

                                // القسم 3: الترتيب
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
                }
            }

            // 2. قائمة الحسابات المضغوطة (دفتر الحسابات)
            if (customersWithBalance.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = when {
                            balanceFilter == CustomerBalanceFilter.DEBTOR -> "لا يوجد عملاء عليهم مبالغ"
                            balanceFilter == CustomerBalanceFilter.CREDITOR -> "لا يوجد عملاء لهم أرصدة"
                            balanceFilter == CustomerBalanceFilter.SETTLED -> "لا يوجد عملاء مسددون بالكامل"
                            accountFilter == AccountFilter.WELL_OWNERS -> "لا يوجد أصحاب آبار مسجلين"
                            else -> "لا يوجد عملاء مضافين"
                        },
                        description = if (balanceFilter != CustomerBalanceFilter.ALL) "جرّب تغيير خيارات الفلترة لعرض باقي الحسابات" else "اضغط على زر (+) لإضافة حساب جديد"
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

        if (!isCompactHeight) {
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
                                ),
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                            ),
                                            softWrap = false,
                                            maxLines = 1
                                        )
                                    }
                                    AccountFilter.FARMERS -> {
                                        Text(
                                            text = "لك",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp
                                            ),
                                            softWrap = false,
                                            maxLines = 1
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
                                            ),
                                            softWrap = false,
                                            maxLines = 1
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
                                ),
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // زر الإضافة السريع (+) الدائري في المنتصف الهندسي الدقيق للكرت دائماً
                    FloatingActionButton(
                        onClick = { showQuickAddChoice = true },
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
                            contentDescription = "إضافة سريعة",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        } else {
            // في الوضع الأفقي / الشاشات المضغوطة: زر عائم مدمج أنيق في الزاوية دون حجب أي كرت
            FloatingActionButton(
                onClick = { showQuickAddChoice = true },
                containerColor = PrimaryTeal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 12.dp)
                    .size(46.dp)
                    .testTag("fab_add_customer")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "إضافة حساب جديد",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp)
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

    // مؤشر جاري قراءة ملف الإكسل بذكاء بهوية مسرب المائية
    if (isImporting) {
        com.example.core.ui.MosaribLoadingDialog(
            title = "جاري قراءة ملف البيانات",
            subTitle = "يتم فحص الأعمدة وتدفق البيانات بذكاء..."
        )
    }

    // File Open / Share Dialog (PDF / Excel / CSV)
    pdfReady?.let { (file, title) ->
        com.example.core.ui.MosaribFileActionDialog(
            file = file,
            title = title,
            fileType = when {
                file.name.endsWith(".xlsx", true) -> "Excel"
                file.name.endsWith(".csv", true) -> "CSV"
                else -> "PDF"
            },
            onOpen = {
                if (file.name.endsWith(".xlsx", true)) {
                    com.example.core.util.FileSharingHelper.openFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                } else if (file.name.endsWith(".csv", true)) {
                    com.example.core.util.FileSharingHelper.openFile(context, file, "text/csv")
                } else {
                    com.example.core.util.FileSharingHelper.openPdf(context, file)
                }
                viewModel.clearPdfReady()
            },
            onShare = {
                if (file.name.endsWith(".xlsx", true)) {
                    com.example.core.util.FileSharingHelper.shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", title)
                } else if (file.name.endsWith(".csv", true)) {
                    com.example.core.util.FileSharingHelper.shareFile(context, file, "text/csv", title)
                } else {
                    com.example.core.util.FileSharingHelper.sharePdf(context, file, title)
                }
                viewModel.clearPdfReady()
            },
            onDismiss = { viewModel.clearPdfReady() }
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
        ModalBottomSheet(
            onDismissRequest = { showQuickAddChoice = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إضافة سريعة",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp
                    )
                )
                Text(
                    text = "اختر الإجراء الذي تود تسجيله مباشرة",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // 1. عميل جديد
                QuickAddOptionItem(
                    icon = Icons.Default.PersonAdd,
                    iconTint = PrimaryTeal,
                    iconBg = PrimaryTeal.copy(alpha = 0.12f),
                    title = "عميل أو صاحب بئر جديد",
                    subtitle = "تسجيل حساب مزارع جديد أو صاحب بئر",
                    onClick = {
                        showQuickAddChoice = false
                        showAddSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. سقي
                QuickAddOptionItem(
                    icon = Icons.Default.WaterDrop,
                    iconTint = Color(0xFF0288D1),
                    iconBg = Color(0xFF0288D1).copy(alpha = 0.12f),
                    title = "تسجيل دورة سقي",
                    subtitle = "حساب ساعات ري مزارع وتحديث الحساب",
                    onClick = {
                        showQuickAddChoice = false
                        showAddSessionSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. معاملة
                QuickAddOptionItem(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    iconTint = AccentGold,
                    iconBg = AccentGold.copy(alpha = 0.14f),
                    title = "معاملة مالية (سند)",
                    subtitle = "تسجيل سند قبض نقد/بنك أو سند صرف",
                    onClick = {
                        showQuickAddChoice = false
                        showAddVoucherSheet = true
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }

    // Sheet: Add Manual Session (سقي)
    if (showAddSessionSheet) {
        val customersList = rawCustomers.map { it.customer }
        AddEditSessionBottomSheet(
            initialSession = null,
            customers = customersList,
            defaultPricePerHour = config.defaultPricePerHour,
            currencySymbol = config.currencySymbol,
            onCreateCustomer = { customer ->
                sessionsViewModel?.createCustomer(customer) ?: 0L
            },
            onDismiss = { showAddSessionSheet = false },
            onSave = { id, custId, pumpName, startTime, endTime, hrs, mins, rate, paid, notes, billedTo, wastedMins, wastedReason, discount, costRate, pumpId ->
                sessionsViewModel?.saveManualSession(
                    id = id,
                    customerId = custId,
                    pumpName = pumpName,
                    startTime = startTime,
                    endTime = endTime,
                    hours = hrs,
                    minutes = mins,
                    pricePerHour = rate,
                    amountPaid = paid,
                    notes = notes,
                    billedToCustomerId = billedTo,
                    wastedMinutes = wastedMins,
                    wastedReason = wastedReason,
                    discountAmount = discount,
                    costPricePerHour = costRate,
                    pumpSourceId = pumpId
                )
                showAddSessionSheet = false
                viewModel.showToast("تم تسجيل دورة السقي بنجاح وتحديث الكشوفات")
            }
        )
    }

    // Sheet: Add Voucher (معاملة)
    if (showAddVoucherSheet) {
        val customersList = rawCustomers.map { it.customer }
        AddVoucherBottomSheet(
            initialType = VoucherType.RECEIPT,
            customers = customersList,
            allSessions = allSessions,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddVoucherSheet = false },
            onSave = { type, customerId, amount, category, method, notes, sessionId, selectedSessionIds, discountAmount ->
                vouchersViewModel?.addVoucher(
                    type = type,
                    customerId = customerId,
                    amount = amount,
                    category = category,
                    paymentMethod = method,
                    notes = notes,
                    sessionId = sessionId,
                    selectedSessionIds = selectedSessionIds,
                    discountAmount = discountAmount
                )
                showAddVoucherSheet = false
                viewModel.showToast("تم تسجيل المعاملة بنجاح وتحديث الحسابات")
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
    val isDark = isAppDark()
    var menuExpanded by remember(customer.id) { mutableStateOf(false) }

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
                                if (isDark) Color(0xFF1E293B)
                                else if (customer.isWellOwner) Color(0xFFE1F5FE)
                                else PrimaryTeal.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (customer.isWellOwner) Icons.Default.WaterDrop else Icons.Default.Eco,
                            contentDescription = if (customer.isWellOwner) "صاحب بئر" else "عميل مزارع",
                            tint = if (isDark) Color.White else (if (customer.isWellOwner) Color(0xFF0288D1) else PrimaryTeal),
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

                val screenWidth = LocalConfiguration.current.screenWidthDp
                val isCompactCard = screenWidth < 410
                val isUltraCompact = screenWidth < 330

                // الأزرار المتجاوبة: في الشاشات العادية تظهر الأزرار، وفي الشاشات المضغوطة يقتصر الكرت على الأهم أو الثلاث نقاط
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isUltraCompact && customer.phone.isNotEmpty()) {
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

                        if (!isCompactCard) {
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
                    }

                    if (!isCompactCard) {
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
                                text = { Text("عرض كشف الحساب", fontWeight = FontWeight.Bold) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryTeal) },
                                onClick = {
                                    menuExpanded = false
                                    onClick()
                                }
                            )
                            if (customer.phone.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("اتصال هاتفي (${customer.phone})") },
                                    leadingIcon = { Icon(Icons.Default.Call, contentDescription = null, tint = PrimaryTeal) },
                                    onClick = {
                                        menuExpanded = false
                                        onCallClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("إرسال كشف عبر واتساب") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color(0xFF2E7D32)) },
                                    onClick = {
                                        menuExpanded = false
                                        onWhatsAppClick()
                                    }
                                )
                            }
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
                                text = { Text("أرشفة الحساب", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) },
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

                if (customer.customGroup.isNotBlank()) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGold.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = customer.customGroup,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
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

            // 3. الصف الثالث: المبلغ باللون الأحمر أو الأخضر أو مسدد (يتوسط البطاقة أفقياً) مع التفقيط باللغة العربية
            val (statusText, statusAmount, statusColor) = if (customer.isWellOwner) {
                val net = item.payableBalance - item.receivableBalance
                when {
                    net > 0.001 -> Triple("له", net, Color(0xFF2E7D32))
                    net < -0.001 -> Triple("عليه", -net, Color(0xFFE53935))
                    else -> Triple("مسدد", 0.0, AccentEmerald)
                }
            } else {
                when {
                    item.receivableBalance > 0.001 -> Triple("عليه", item.receivableBalance, Color(0xFFE53935))
                    item.receivableBalance < -0.001 -> Triple("له", -item.receivableBalance, Color(0xFF2E7D32))
                    else -> Triple("مسدد", 0.0, AccentEmerald)
                }
            }

            if (statusText == "مسدد") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDark) Color(0xFF132A1C) else Color(0xFFE8F5E9))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AccentEmerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "مسدد",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AccentEmerald,
                                    fontSize = 12.5.sp
                                )
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "$statusText: ${Formatters.formatCurrency(statusAmount, currencySymbol)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = statusColor,
                            fontSize = 14.5.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    val tafqeetWords = Formatters.amountToArabicWords(statusAmount, currencySymbol)
                    if (tafqeetWords.isNotBlank()) {
                        Text(
                            text = tafqeetWords,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = statusColor.copy(alpha = 0.82f),
                                fontSize = 10.5.sp
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddOptionItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

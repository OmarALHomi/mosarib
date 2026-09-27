package com.example.features.customers

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.EmptyStateView
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: Long,
    viewModel: CustomersViewModel,
    onBack: () -> Unit,
    onAddSessionForCustomer: (Customer) -> Unit
) {
    BackHandler { onBack() }

    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val allCustomersWithBalance by viewModel.customersWithBalance.collectAsStateWithLifecycle()
    val customerWithBalance = allCustomersWithBalance.find { it.customer.id == customerId }
    val toast by viewModel.toast.collectAsStateWithLifecycle()

    val sessions by viewModel.getCustomerSessions(customerId).collectAsStateWithLifecycle()
    val vouchers by viewModel.getCustomerVouchers(customerId).collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddReceiptSheet by remember { mutableStateOf(false) }
    var showEditCustomerSheet by remember { mutableStateOf(false) }
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    var localSessionPdfReady by remember { mutableStateOf<Pair<java.io.File, String>?>(null) }
    val activePdf = pdfReady ?: localSessionPdfReady

    // PDF Open / Share Dialog
    activePdf?.let { (file, title) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                viewModel.clearPdfReady()
                localSessionPdfReady = null
            },
            title = { Text("المستند جاهز", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في فتح وعرض الملف مباشرة أم مشاركته؟") },
            confirmButton = {
                Button(
                    onClick = {
                        FileSharingHelper.openPdf(context, file)
                        viewModel.clearPdfReady()
                        localSessionPdfReady = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح / عرض")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        FileSharingHelper.sharePdf(context, file, title)
                        viewModel.clearPdfReady()
                        localSessionPdfReady = null
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة")
                }
            }
        )
    }

    if (customerWithBalance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لم يتم العثور على العميل")
        }
        return
    }

    val customer = customerWithBalance.customer

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        customer.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateCustomerStatementPdf(customer) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "كشف حساب PDF", tint = AccentGold)
                    }
                    IconButton(onClick = { showEditCustomerSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل بيانات العميل", tint = PrimaryTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {

            // Customer Summary Banner Card (Modern Compact & Borderless)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // 1. Header: Avatar + Customer Name + Farm & Action Squircles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE0F2F1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = customer.name.take(1),
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF00695C)
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A),
                                            fontSize = 17.sp
                                        )
                                    )
                                    if (customer.farmName.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Yard,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = customer.farmName,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            // Quick Action Buttons (Squircle 34dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (customer.phone.isNotEmpty()) {
                                    // الاتصال الهاتفي
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE8F5E9))
                                            .clickable { FileSharingHelper.makePhoneCall(context, customer.phone) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = "اتصال",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // إرسال كشف واتساب
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFCCFBF1))
                                            .clickable { viewModel.sendCustomerStatementWhatsApp(customer, customerWithBalance) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "واتساب",
                                            tint = Color(0xFF0F766E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // تصدير كشف حساب PDF
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .clickable { viewModel.generateCustomerStatementPdf(customer) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PictureAsPdf,
                                        contentDescription = "كشف حساب PDF",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // تعديل بيانات العميل
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE0F2FE))
                                        .clickable { showEditCustomerSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "تعديل",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. تفاصيل ملف العميل (الهاتف، العنوان، السعر المخصص، الملاحظات)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (customer.phone.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { FileSharingHelper.makePhoneCall(context, customer.phone) }
                                    ) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = customer.phone,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = Color(0xFF0F766E),
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "لا يوجد رقم هاتف مسجل",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8))
                                    )
                                }

                                if (customer.location.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = customer.location,
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF475569))
                                        )
                                    }
                                }
                            }

                            if ((customer.customPricePerHour ?: 0.0) > 0 || customer.notes.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if ((customer.customPricePerHour ?: 0.0) > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "سعر خاص: ${Formatters.formatNumber(customer.customPricePerHour!!)} ${config.currencySymbol}/ساعة",
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    if (customer.notes.isNotEmpty()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = customer.notes,
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B)),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3. كبسولة الرصيد المالي والتفقيط بالعربي وزر قبض دفعة
                        val isDebt = customerWithBalance.balance > 0
                        val isCredit = customerWithBalance.balance < 0

                        val (balanceBg, balanceTextColor) = when {
                            isDebt -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
                            isCredit -> Color(0xFFE8F5E9) to Color(0xFF00695C)
                            else -> Color(0xFFF1F5F9) to Color(0xFF334155)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(balanceBg)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        isDebt -> "دين متبقي بذمة العميل"
                                        isCredit -> "رصيد دائن للعميل (مقدم)"
                                        else -> "الحساب خالص ومسدد بالكامل"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = balanceTextColor.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = if (customerWithBalance.balance == 0.0) "0 ${config.currencySymbol}" else Formatters.formatCurrency(Math.abs(customerWithBalance.balance), config.currencySymbol),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = balanceTextColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp
                                    )
                                )
                                if (customerWithBalance.balance != 0.0) {
                                    Text(
                                        text = Formatters.amountToArabicWords(Math.abs(customerWithBalance.balance), config.currencySymbol),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF5D4037),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = { showAddReceiptSheet = true },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isDebt) AccentEmerald else PrimaryTeal),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("قبض دفعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 4. الإحصائيات الفرعية في 3 أعمدة أنيقة
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ساعات الري", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.5.sp))
                                Text(
                                    Formatters.formatDurationArabic(customerWithBalance.totalMinutes),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي المسارب", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.5.sp))
                                Text(
                                    Formatters.formatCurrency(customerWithBalance.totalBilledAmount, config.currencySymbol),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("إجمالي المقبوض", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.5.sp))
                                Text(
                                    Formatters.formatCurrency(customerWithBalance.totalPaidAmount, config.currencySymbol),
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // Tabs: السقي | القبض
            item {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.padding(horizontal = 10.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("السقي (${sessions.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.WaterDrop, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("القبض (${vouchers.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Receipt, contentDescription = null) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tab Content 0: Sessions
            if (selectedTab == 0) {
                if (sessions.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.WaterDrop,
                            title = "لا توجد دورات ماء مسجلة لهذا العميل",
                            description = "يمكنك تسجيل دورة ري جديدة الآن"
                        )
                    }
                } else {
                    items(sessions, key = { it.id }) { s ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${Formatters.formatDurationArabic(s.durationMinutes)}  (@ ${Formatters.formatNumber(s.pricePerHour)})",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    )
                                    Text(
                                        text = Formatters.formatCurrency(s.totalAmount, config.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(color = PrimaryTeal, fontWeight = FontWeight.ExtraBold)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "المسدد فوراً: ${Formatters.formatCurrency(s.amountPaid, config.currencySymbol)}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = AccentEmerald, fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (s.remainingDebt > 0) "المتبقي: ${Formatters.formatCurrency(s.remainingDebt, config.currencySymbol)}" else "خالص بالكامل",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (s.remainingDebt > 0) Color(0xFFE53935) else AccentEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = Formatters.formatDateTime(s.startTime),
                                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (customer.phone.isNotEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFE8F5E9))
                                                    .clickable {
                                                        val msg = "دورة ري #${s.id}\nالتاريخ: ${Formatters.formatDateTime(s.startTime)}\nالمدة: ${Formatters.formatDurationArabic(s.durationMinutes)}\nالمبلغ: ${Formatters.formatCurrency(s.totalAmount, config.currencySymbol)}\nالمسدد: ${Formatters.formatCurrency(s.amountPaid, config.currencySymbol)}\nالمتبقي: ${Formatters.formatCurrency(s.remainingDebt, config.currencySymbol)}"
                                                        FileSharingHelper.sendWhatsAppMessage(context, customer.phone, msg)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "واتساب", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE0F2F1))
                                                .clickable {
                                                    val file = com.example.core.util.PdfReportGenerator.generateSessionInvoicePdf(
                                                        context = context,
                                                        config = config,
                                                        customer = customer,
                                                        session = s
                                                    )
                                                    localSessionPdfReady = Pair(file, "فاتورة ري #${s.id}")
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "فاتورة PDF", tint = Color(0xFF00695C), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab Content 1: Vouchers (Receipts)
                if (vouchers.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Receipt,
                            title = "لا توجد سندات قبض مسجلة",
                            description = "اضغط على زر (سند قبض / سداد) لإضافة دفعة مالية"
                        )
                    }
                } else {
                    items(vouchers, key = { it.id }) { v ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AccentEmerald.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "سند قبض نقدي (${v.voucherNumber})",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                        )
                                        Text(
                                            text = "${v.paymentMethod}  •  ${Formatters.formatDateTime(v.date)}",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                                        )
                                        if (v.notes.isNotEmpty()) {
                                            Text(
                                                text = v.notes,
                                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = Formatters.formatCurrency(v.amount, config.currencySymbol),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = AccentEmerald,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )

                                    if (customer.phone.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE8F5E9))
                                                .clickable {
                                                    val msg = "سند قبض #${v.voucherNumber.ifEmpty { v.id.toString() }}\nالعميل: ${customer.name}\nالمبلغ: ${Formatters.formatCurrency(v.amount, config.currencySymbol)} (${Formatters.amountToArabicWords(v.amount, config.currencySymbol)})\nطريقة الدفع: ${v.paymentMethod}\nالتاريخ: ${Formatters.formatDateTime(v.date)}"
                                                    FileSharingHelper.sendWhatsAppMessage(context, customer.phone, msg)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "واتساب",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE0F2F1))
                                            .clickable {
                                                val file = com.example.core.util.PdfReportGenerator.generateReceiptVoucherPdf(
                                                    context = context,
                                                    config = config,
                                                    customer = customer,
                                                    voucher = v
                                                )
                                                localSessionPdfReady = Pair(file, "سند قبض #${v.voucherNumber.ifEmpty { v.id.toString() }}")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = "سند PDF",
                                            tint = Color(0xFF00695C),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        LuxuryToastNotification(
            toast = toast,
            onDismiss = { viewModel.dismissToast() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
        )
    }
}

    // Add Receipt Sheet
    if (showAddReceiptSheet) {
        AddReceiptBottomSheet(
            customer = customer,
            currentBalance = customerWithBalance.balance,
            currencySymbol = config.currencySymbol,
            onDismiss = { showAddReceiptSheet = false },
            onConfirm = { amount, method, notes ->
                viewModel.addReceiptVoucher(customer.id, amount, method, notes)
            }
        )
    }

    // Edit Customer Sheet
    if (showEditCustomerSheet) {
        AddEditCustomerBottomSheet(
            initialCustomer = customer,
            currencySymbol = config.currencySymbol,
            onDismiss = { showEditCustomerSheet = false },
            onSave = { id, name, phone, farm, loc, notes, customPrice ->
                viewModel.saveCustomer(id, name, phone, farm, loc, notes, customPrice)
                showEditCustomerSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReceiptBottomSheet(
    customer: Customer,
    currentBalance: Double,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, method: String, notes: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountStr by remember { mutableStateOf(if (currentBalance > 0) currentBalance.toString() else "") }
    var paymentMethod by remember { mutableStateOf("نقداً") }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تسجيل سند قبض وسداد من العميل",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "إلغاء")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("العميل: ${customer.name}", fontWeight = FontWeight.SemiBold)
            Text(
                text = "الرصيد المستحق حالياً: ${Formatters.formatCurrency(currentBalance, currencySymbol)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (currentBalance > 0) Color(0xFFE53935) else AccentEmerald
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = Formatters.formatAmountInput(it) },
                label = { Text("المبلغ المقبوض ($currencySymbol) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("receipt_amount_input"),
                shape = RoundedCornerShape(12.dp)
            )

            val parsedAmt = Formatters.parseAmountInput(amountStr)
            if (parsedAmt > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = Formatters.amountToArabicWords(parsedAmt, currencySymbol),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = AccentEmerald,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("طريقة الدفع (نقداً، حوالة، تحويل بنكي...)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات السند") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = Formatters.parseAmountInput(amountStr)
                    if (amt > 0) {
                        onConfirm(amt, paymentMethod, notes)
                        onDismiss()
                    }
                },
                enabled = Formatters.parseAmountInput(amountStr) > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_receipt_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
            ) {
                Text("حفظ وترحيل سند القبض", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

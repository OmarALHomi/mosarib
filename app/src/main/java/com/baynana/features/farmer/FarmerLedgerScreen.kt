package com.baynana.features.farmer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baynana.core.sync.MusribSyncManager.MusribCloudEntry
import com.baynana.core.util.FileSharingHelper
import com.baynana.core.util.Formatters
import com.baynana.ui.theme.AccentEmerald
import com.baynana.ui.theme.AccentGold
import com.baynana.ui.theme.PrimaryTeal
import com.baynana.ui.theme.PrimaryTealDark
import com.baynana.ui.theme.StatusDebt
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerLedgerScreen(
    viewModel: FarmerViewModel,
    musrib: LinkedMusrib,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val entries by viewModel.ledgerEntries.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = musrib.musribName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (musrib.farmName.isNotBlank()) {
                            Text(
                                text = "مزرعة: ${musrib.farmName}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.8f))
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                actions = {
                    if (musrib.musribPhone.isNotBlank()) {
                        IconButton(onClick = { FileSharingHelper.makePhoneCall(context, musrib.musribPhone) }) {
                            Icon(Icons.Default.Call, contentDescription = "اتصال بالمسرب", tint = Color.White)
                        }
                    }
                    IconButton(onClick = { viewModel.refreshMusrib(musrib) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTealDark,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ============================================================
            // 1. HERO BALANCE CARD
            // ============================================================
            item {
                val balance = musrib.currentBalance
                val isDebtor = balance > 0
                val isCreditor = balance < 0
                val isZero = balance == 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isDebtor -> StatusDebt.copy(alpha = 0.15f)
                            isCreditor -> AccentEmerald.copy(alpha = 0.15f)
                            else -> PrimaryTeal.copy(alpha = 0.15f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                isDebtor -> "المتبقي عليك للمسرب"
                                isCreditor -> "رصيد لك عند المسرب"
                                else -> "الحساب متزن / خالص"
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isDebtor -> StatusDebt
                                    isCreditor -> AccentEmerald
                                    else -> PrimaryTeal
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${Formatters.formatNumber(abs(balance))} ر.ي",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = when {
                                    isDebtor -> StatusDebt
                                    isCreditor -> AccentEmerald
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي السقيات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${Formatters.formatNumber(musrib.totalDebit)} ر.ي", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("إجمالي المسدد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${Formatters.formatNumber(musrib.totalPaid)} ر.ي", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentEmerald))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("كود الربط", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("#${musrib.linkCode}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentGold))
                            }
                        }
                    }
                }
            }

            // ============================================================
            // 2. LOADING STATE
            // ============================================================
            if (isLoading && entries.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                }
            }

            // ============================================================
            // 3. EMPTY STATE
            // ============================================================
            if (!isLoading && entries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryTeal, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد دورات سقي أو سندات مسجلة بعد",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ستظهر أي دورة سقي جديدة أو سند قبض يسجله المسرب هنا فوراً وبشكل لحظي.",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ============================================================
            // 4. ENTRIES STREAM (Water Sessions & Vouchers)
            // ============================================================
            items(entries, key = { it.entryId }) { entry ->
                FarmerEntryCard(entry)
            }
        }
    }
}

@Composable
private fun FarmerEntryCard(entry: MusribCloudEntry) {
    val isSession = entry.entryType == "WATER_SESSION"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSession) PrimaryTeal.copy(alpha = 0.15f)
                        else AccentEmerald.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSession) Icons.Default.WaterDrop else Icons.Default.Payments,
                    contentDescription = null,
                    tint = if (isSession) PrimaryTeal else AccentEmerald,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSession) "دورة سقي" else "سند قبض (دفعة مسددة)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = entry.date,
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (isSession) {
                    Text(
                        text = "المدة: ${entry.hours} س و ${entry.minutes} د  •  السعر: ${Formatters.formatNumber(entry.pricePerHour)} ر.ي/س",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    if (entry.amountPaid > 0) {
                        Text(
                            text = "المسدد فوراً: ${Formatters.formatNumber(entry.amountPaid)} ر.ي  •  المؤخر: ${Formatters.formatNumber(entry.remainingDebt)} ر.ي",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald, fontWeight = FontWeight.SemiBold)
                        )
                    }
                } else {
                    Text(
                        text = "سند رقم: ${entry.receiptNumber.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                if (entry.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.notes,
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount
            Text(
                text = if (isSession) "${Formatters.formatNumber(entry.totalPrice)} ر.ي" else "- ${Formatters.formatNumber(entry.voucherAmount)} ر.ي",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSession) MaterialTheme.colorScheme.onSurface else AccentEmerald
                )
            )
        }
    }
}

package com.example.features.reports

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.LuxuryToastNotification
import com.example.core.ui.StatBoxCard
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.appConfig.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.period.collectAsStateWithLifecycle()
    val toast by viewModel.toast.collectAsStateWithLifecycle()
    val pdfReady by viewModel.pdfReadyFile.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // PDF Open / Share Dialog
    pdfReady?.let { (file, title) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearPdfReady() },
            title = { Text("تم إنشاء التقرير بنجاح", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في فتح وعرض التقرير مباشرة أم مشاركته؟") },
            confirmButton = {
                Button(
                    onClick = {
                        com.example.core.util.FileSharingHelper.openPdf(context, file)
                        viewModel.clearPdfReady()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح / عرض")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        com.example.core.util.FileSharingHelper.sharePdf(context, file, title)
                        viewModel.clearPdfReady()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة")
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Clean Action Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التقارير والإحصائيات",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Button(
                        onClick = { viewModel.exportComprehensiveReportPdf() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير PDF", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

        // Period Filters
        item {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.ALL,
                        onClick = { viewModel.setPeriod(ReportPeriod.ALL) },
                        label = { Text("كامل الفترة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.TODAY,
                        onClick = { viewModel.setPeriod(ReportPeriod.TODAY) },
                        label = { Text("اليوم") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.THIS_WEEK,
                        onClick = { viewModel.setPeriod(ReportPeriod.THIS_WEEK) },
                        label = { Text("هذا الأسبوع") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == ReportPeriod.THIS_MONTH,
                        onClick = { viewModel.setPeriod(ReportPeriod.THIS_MONTH) },
                        label = { Text("هذا الشهر") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Metrics Grid 1: Revenue, Net Profit
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBoxCard(
                    title = "إجمالي قيمة السقي",
                    value = Formatters.formatCurrency(stats.totalRevenue, config.currencySymbol),
                    subtitle = "${stats.totalSessionsCount} دورة (بعد الهدر والخصومات)",
                    icon = Icons.Default.AttachMoney,
                    accentColor = PrimaryTeal,
                    modifier = Modifier.weight(1f)
                )
                StatBoxCard(
                    title = "صافي الأرباح التشغيلية",
                    value = Formatters.formatCurrency(stats.netOperatingProfit, config.currencySymbol),
                    subtitle = "بعد مشتريات الآبار والتشغيل",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accentColor = if (stats.netOperatingProfit >= 0) AccentEmerald else Color(0xFFE53935),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Grid 2: Cashflow & Operating Expenses
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBoxCard(
                    title = "المقبوضات النقدية",
                    value = Formatters.formatCurrency(stats.totalCollectedCash, config.currencySymbol),
                    subtitle = "التحصيل الفعلي",
                    icon = Icons.Default.ArrowDownward,
                    accentColor = AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
                StatBoxCard(
                    title = "مصاريف التشغيل",
                    value = Formatters.formatCurrency(stats.totalPumpExpenses, config.currencySymbol),
                    subtitle = "باستثناء سداد مستحقات الآبار",
                    icon = Icons.Default.ArrowUpward,
                    accentColor = Color(0xFFFF5252),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Grid 3: Water Duration & Outstanding Debts
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBoxCard(
                    title = "ساعات البيع بعد هدر المسرب",
                    value = Formatters.formatDurationArabic(stats.totalWaterMinutes),
                    subtitle = "هدر المسرب: ${Formatters.formatDurationShort(stats.totalDistributorWasteMinutes)}",
                    icon = Icons.Default.AccessTime,
                    accentColor = SecondaryAqua,
                    modifier = Modifier.weight(1f)
                )
                StatBoxCard(
                    title = "ديون السقي للفترة",
                    value = Formatters.formatCurrency(stats.totalOutstandingDebt, config.currencySymbol),
                    subtitle = "منفصلة عن مستحقات شراء الآبار",
                    icon = Icons.Default.WaterDrop,
                    accentColor = Color(0xFFFF8A80),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Well-owner purchases and owner-side waste are reported separately from irrigation sales.
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBoxCard(
                    title = "ساعات الشراء المحتسبة",
                    value = Formatters.formatDurationArabic(stats.totalChargeablePurchasedMinutes),
                    subtitle = "المسجلة: ${Formatters.formatDurationShort(stats.totalPurchasedMinutes)}",
                    icon = Icons.Default.AccessTime,
                    accentColor = PrimaryTeal,
                    modifier = Modifier.weight(1f)
                )
                StatBoxCard(
                    title = "هدر على صاحب البئر",
                    value = Formatters.formatDurationArabic(stats.totalOwnerWasteMinutes),
                    subtitle = "قيمة الخصم: ${Formatters.formatCurrency(stats.totalOwnerWasteCredit, config.currencySymbol)}",
                    icon = Icons.Default.ArrowDownward,
                    accentColor = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBoxCard(
                    title = "صافي قيمة مشتريات الآبار",
                    value = Formatters.formatCurrency(stats.totalOwnerPurchaseAmount, config.currencySymbol),
                    subtitle = "السداد لأصحاب الآبار: ${Formatters.formatCurrency(stats.totalOwnerPayments, config.currencySymbol)}",
                    icon = Icons.Default.Payments,
                    accentColor = AccentGold,
                    modifier = Modifier.weight(1f)
                )
                StatBoxCard(
                    title = "مستحقات شراء الآبار",
                    value = Formatters.formatCurrency(stats.totalOwnerPayable, config.currencySymbol),
                    subtitle = if (stats.totalOwnerPayable >= 0) "المشتريات ناقص ما سُدد" else "السداد تجاوز شراء هذه الفترة",
                    icon = Icons.Default.ArrowUpward,
                    accentColor = if (stats.totalOwnerPayable > 0) Color(0xFFE53935) else AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Collection Progress Ratio Bar
        item {
            val collectionRatio = if (stats.totalRevenue > 0) {
                (stats.totalCollectedCash / stats.totalRevenue).coerceIn(0.0, 1.0).toFloat()
            } else 0f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نسبة التحصيل",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${(collectionRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentEmerald)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { collectionRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = AccentEmerald,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "المحصل: ${Formatters.formatCurrency(stats.totalCollectedCash, config.currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = AccentEmerald)
                        )
                        Text(
                            text = "الآجل: ${Formatters.formatCurrency(stats.totalOutstandingDebt, config.currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE53935))
                        )
                    }
                }
            }
        }

        // Top Consuming Customers Section
        item {
            Text(
                text = "أكثر الحسابات سقيًا",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        if (stats.topCustomers.isEmpty()) {
            item {
                Text(
                    text = "لا توجد بيانات استهلاك مسجلة بعد",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(stats.topCustomers) { top ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryTeal.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    if (top.customer.isWellOwner) "${top.customer.name} • صاحب بئر" else top.customer.name,
                                    fontWeight = FontWeight.Bold
                                )
                                if (top.customer.farmName.isNotEmpty()) {
                                    Text(top.customer.farmName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Formatters.formatDurationArabic(top.totalMinutes),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryTeal)
                            )
                            Text(
                                text = Formatters.formatCurrency(top.totalBilled, config.currencySymbol),
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                            )
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
            .align(Alignment.BottomCenter)
            .padding(bottom = 85.dp)
    )
}
}

package com.example.features.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.LuxuryToastNotification
import com.example.core.util.FileSharingHelper
import com.example.core.util.Formatters
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryAqua
import kotlin.math.max

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
    val context = LocalContext.current

    // PDF Open / Share Dialog
    pdfReady?.let { (file, title) ->
        AlertDialog(
            onDismissRequest = { viewModel.clearPdfReady() },
            title = { Text("تم إنشاء التقرير بنجاح", fontWeight = FontWeight.Bold) },
            text = { Text("هل ترغب في فتح وعرض التقرير مباشرة أم مشاركته؟") },
            confirmButton = {
                Button(
                    onClick = {
                        FileSharingHelper.openPdf(context, file)
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
                OutlinedButton(
                    onClick = {
                        FileSharingHelper.sharePdf(context, file, title)
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
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header Action Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = PrimaryTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مؤشرات الأداء والتقارير",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = when (selectedPeriod) {
                                ReportPeriod.ALL -> "إحصاءات شاملة لكافة الفترات"
                                ReportPeriod.TODAY -> "حركة وإيرادات اليوم"
                                ReportPeriod.THIS_WEEK -> "مؤشرات الأسبوع الجاري"
                                ReportPeriod.THIS_MONTH -> "مؤشرات الشهر الجاري"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                        )
                    }

                    Button(
                        onClick = { viewModel.exportComprehensiveReportPdf() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير PDF", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Period Filters Row
            item {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        PeriodFilterChip(
                            label = "كامل الفترة",
                            selected = selectedPeriod == ReportPeriod.ALL,
                            onClick = { viewModel.setPeriod(ReportPeriod.ALL) }
                        )
                    }
                    item {
                        PeriodFilterChip(
                            label = "اليوم",
                            selected = selectedPeriod == ReportPeriod.TODAY,
                            onClick = { viewModel.setPeriod(ReportPeriod.TODAY) }
                        )
                    }
                    item {
                        PeriodFilterChip(
                            label = "هذا الأسبوع",
                            selected = selectedPeriod == ReportPeriod.THIS_WEEK,
                            onClick = { viewModel.setPeriod(ReportPeriod.THIS_WEEK) }
                        )
                    }
                    item {
                        PeriodFilterChip(
                            label = "هذا الشهر",
                            selected = selectedPeriod == ReportPeriod.THIS_MONTH,
                            onClick = { viewModel.setPeriod(ReportPeriod.THIS_MONTH) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // 1. Hero KPI Card: Net Operating Profit with 3-Way Ratio Bar
            item {
                HeroProfitCard(
                    stats = stats,
                    currencySymbol = config.currencySymbol
                )
            }

            // 2. Liquidity & Collection Gauge Card
            item {
                LiquidityCollectionCard(
                    stats = stats,
                    currencySymbol = config.currencySymbol
                )
            }

            // 3. Irrigation Volume & Pumping Efficiency
            item {
                IrrigationEfficiencyCard(
                    stats = stats
                )
            }

            // 4. Well Owner Accounts Card (if applicable)
            item {
                if (stats.totalOwnerPurchaseAmount > 0 || stats.totalChargeablePurchasedMinutes > 0 || stats.totalOwnerPayments > 0) {
                    WellOwnerAccountingCard(
                        stats = stats,
                        currencySymbol = config.currencySymbol
                    )
                }
            }

            // 5. Top Consuming Customers Leaderboard
            item {
                TopCustomersLeaderboardCard(
                    topCustomers = stats.topCustomers,
                    currencySymbol = config.currencySymbol
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
}

@Composable
private fun PeriodFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryTeal,
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, if (selected) PrimaryTeal else Color.LightGray.copy(alpha = 0.3f))
    )
}

/**
 * 1. Hero Net Profit KPI Card:
 * Highlights net operating profit with clear financial distribution bar (Revenue = Profit + WellCost + Expenses).
 */
@Composable
private fun HeroProfitCard(
    stats: GeneralReportStats,
    currencySymbol: String
) {
    val isProfit = stats.netOperatingProfit >= 0
    val totalRevenue = max(0.0, stats.totalRevenue)
    val wellCost = max(0.0, stats.totalWellCost)
    val expenses = max(0.0, stats.totalPumpExpenses)
    val profit = max(0.0, stats.netOperatingProfit)

    val wellRatio = if (totalRevenue > 0) (wellCost / totalRevenue).coerceIn(0.0, 1.0).toFloat() else 0f
    val expRatio = if (totalRevenue > 0) (expenses / totalRevenue).coerceIn(0.0, 1.0).toFloat() else 0f
    val profitRatio = if (totalRevenue > 0 && isProfit) (profit / totalRevenue).coerceIn(0.0, 1.0).toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isProfit) AccentEmerald.copy(alpha = 0.08f) else Color(0xFFE53935).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                // Top Headline & Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isProfit) AccentEmerald.copy(alpha = 0.15f) else Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = if (isProfit) AccentEmerald else Color(0xFFE53935),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "صافي الأرباح التشغيلية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Surface(
                        color = if (isProfit) AccentEmerald.copy(alpha = 0.12f) else Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isProfit) "عائد إيجابي 📈" else "عجز تشغيلي ⚠️",
                            color = if (isProfit) Color(0xFF1B5E20) else Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Big Formatted Number
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = Formatters.formatCurrency(stats.netOperatingProfit, currencySymbol),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isProfit) AccentEmerald else Color(0xFFE53935),
                            fontSize = 26.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Visual 3-Way Segmented Financial Ratio Bar
                if (totalRevenue > 0) {
                    Text(
                        text = "توزيع الإيراد: ${Formatters.formatCurrency(totalRevenue, currencySymbol)}",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFEEEEEE))
                    ) {
                        if (profitRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(profitRatio)
                                    .fillMaxHeight()
                                    .background(AccentEmerald)
                            )
                        }
                        if (wellRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(wellRatio)
                                    .fillMaxHeight()
                                    .background(SecondaryAqua)
                            )
                        }
                        if (expRatio > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(expRatio)
                                    .fillMaxHeight()
                                    .background(Color(0xFFFF6B6B))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Minimal Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FinancialLegendItem(
                            color = AccentEmerald,
                            label = "الربح",
                            value = "${(profitRatio * 100).toInt()}%"
                        )
                        FinancialLegendItem(
                            color = SecondaryAqua,
                            label = "تكلفة الآبار",
                            value = Formatters.formatCurrency(stats.totalWellCost, currencySymbol)
                        )
                        FinancialLegendItem(
                            color = Color(0xFFFF6B6B),
                            label = "مصاريف التشغيل",
                            value = Formatters.formatCurrency(stats.totalPumpExpenses, currencySymbol)
                        )
                    }
                } else {
                    Text(
                        text = "لا توجد حركات مسجلة في هذه الفترة",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialLegendItem(
    color: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
            Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp))
        }
    }
}

/**
 * 2. Liquidity & Collection Health Card:
 * Visual collection rate progress bar with side-by-side cash vs debt metrics.
 */
@Composable
private fun LiquidityCollectionCard(
    stats: GeneralReportStats,
    currencySymbol: String
) {
    val totalBilled = stats.totalCollectedCash + stats.totalOutstandingDebt
    val collectionPercent = if (totalBilled > 0) {
        ((stats.totalCollectedCash / totalBilled) * 100).toInt()
    } else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AccentEmerald.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "السيولة ونسبة التحصيل",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = if (collectionPercent >= 70) AccentEmerald.copy(alpha = 0.12f) else Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$collectionPercent% تم تحصيله",
                        color = if (collectionPercent >= 70) Color(0xFF1B5E20) else Color(0xFFE65100),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two-tone Proportional Bar
            val ratio = (collectionPercent / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = AccentEmerald,
                trackColor = Color(0xFFFFCDD2)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Split Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cash Collected
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = AccentEmerald.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(14.dp))
                            Text("المحصّل نقداً", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(stats.totalCollectedCash, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AccentEmerald
                        )
                    }
                }

                // Outstanding Debt
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                            Text("ديون مؤجلة (آجل)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatCurrency(stats.totalOutstandingDebt, currencySymbol),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFE53935)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. Irrigation Volume & Pumping Efficiency:
 * Shows billed net irrigation hours vs waste hours with an efficiency percentage.
 */
@Composable
private fun IrrigationEfficiencyCard(
    stats: GeneralReportStats
) {
    val totalIrrigationMinutes = stats.totalWaterMinutes + stats.totalDistributorWasteMinutes
    val efficiencyPercent = if (totalIrrigationMinutes > 0) {
        ((stats.totalWaterMinutes.toDouble() / totalIrrigationMinutes) * 100).toInt()
    } else 100

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(PrimaryTeal.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "كفاءة الضخ وساعات السقي",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = PrimaryTeal.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$efficiencyPercent% كفاءة التوزيع",
                        color = PrimaryTeal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Big Duration Value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("الساعات المباعة للمزارعين", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                    Text(
                        text = Formatters.formatDurationArabic(stats.totalWaterMinutes),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryTeal
                        )
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        Text(
                            text = "${stats.totalSessionsCount} دورة سقي",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Efficiency Visual Bar
            LinearProgressIndicator(
                progress = { (efficiencyPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryTeal,
                trackColor = Color(0xFFFFCC80)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "إجمالي التشغيل: ${Formatters.formatDurationShort(totalIrrigationMinutes)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
                Text(
                    text = "هدر المسرب: ${Formatters.formatDurationShort(stats.totalDistributorWasteMinutes)}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

/**
 * 4. Well Owner Accounts Card:
 * Clean financial ledger comparing purchases, payments, and balance payable.
 */
@Composable
private fun WellOwnerAccountingCard(
    stats: GeneralReportStats,
    currencySymbol: String
) {
    val purchaseAmt = max(0.0, stats.totalOwnerPurchaseAmount)
    val payments = max(0.0, stats.totalOwnerPayments)
    val fulfillmentRatio = if (purchaseAmt > 0) (payments / purchaseAmt).coerceIn(0.0, 1.0).toFloat() else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = Color(0xFFB8860B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "محاسبة مياه الآبار",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Surface(
                    color = if (stats.totalOwnerPayable <= 0) AccentEmerald.copy(alpha = 0.12f) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (stats.totalOwnerPayable <= 0) "خالص بالكامل ✅" else "متبقي مستحقات",
                        color = if (stats.totalOwnerPayable <= 0) Color(0xFF1B5E20) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Metric Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    title = "صافي المشتريات",
                    value = Formatters.formatCurrency(stats.totalOwnerPurchaseAmount, currencySymbol),
                    subtitle = Formatters.formatDurationShort(stats.totalChargeablePurchasedMinutes),
                    color = PrimaryTeal,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "المسدد للبئر",
                    value = Formatters.formatCurrency(stats.totalOwnerPayments, currencySymbol),
                    subtitle = "تحويلات وسداد",
                    color = AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    title = "المتبقي للبئر",
                    value = Formatters.formatCurrency(stats.totalOwnerPayable, currencySymbol),
                    subtitle = "رصيد دائن",
                    color = if (stats.totalOwnerPayable > 0) Color(0xFFE53935) else AccentEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            if (purchaseAmt > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { fulfillmentRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = AccentEmerald,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = color))
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 9.sp))
        }
    }
}

/**
 * 5. Top Consuming Customers Leaderboard:
 * High-end visual ranking with medal badges and relative consumption bars.
 */
@Composable
private fun TopCustomersLeaderboardCard(
    topCustomers: List<TopCustomerStat>,
    currencySymbol: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFB8860B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "أكثر الحسابات استهلاكاً",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "${topCustomers.size} حسابات",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (topCustomers.isEmpty()) {
                Text(
                    text = "لا توجد بيانات استهلاك مسجلة في هذه الفترة",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val maxMinutes = max(1, topCustomers.maxOf { it.totalMinutes })

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topCustomers.take(5).forEachIndexed { index, top ->
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "#${index + 1}"
                        }
                        val relativeWidth = (top.totalMinutes.toFloat() / maxMinutes).coerceIn(0.05f, 1f)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(medal, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = top.customer.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        if (top.customer.isWellOwner) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = PrimaryTeal.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "بئر",
                                                    color = PrimaryTeal,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = Formatters.formatDurationArabic(top.totalMinutes),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PrimaryTeal
                                        )
                                        Text(
                                            text = Formatters.formatCurrency(top.totalBilled, currencySymbol),
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Relative consumption progress bar
                                LinearProgressIndicator(
                                    progress = { relativeWidth },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp)),
                                    color = if (index == 0) AccentGold else PrimaryTeal,
                                    trackColor = Color.LightGray.copy(alpha = 0.25f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

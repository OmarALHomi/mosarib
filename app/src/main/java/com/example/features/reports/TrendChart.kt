package com.example.features.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.example.core.util.Formatters
import com.example.features.sessions.WaterSession
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.ui.theme.mosarib
import java.util.Calendar
import kotlin.math.max

data class TrendBucket(
    val label: String,            // Short X label, e.g. "1", "15", "نوفمبر"
    val fullLabel: String,        // Descriptive label for tooltip, e.g. "15 نوفمبر" or "نوفمبر 2026"
    val revenue: Double = 0.0,
    val collected: Double = 0.0,
    val startMillis: Long = 0L,
    val endMillis: Long = 0L
)

object TrendBucketBuilder {

    val ARABIC_MONTH_NAMES = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    /**
     * Builds buckets for the trend chart.
     * Periods up to 31 days use DAY buckets.
     * Longer periods, THIS_YEAR, and ALL use MONTH buckets (last 12 months for ALL).
     * Empty buckets are preserved with 0.0 amounts.
     */
    fun buildBuckets(
        period: ReportPeriod,
        customRange: Pair<Long, Long>?,
        sessions: List<WaterSession>,
        vouchers: List<Voucher>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<TrendBucket> {
        val buckets = when (period) {
            ReportPeriod.ALL -> buildLast12Months(nowMillis)
            ReportPeriod.THIS_YEAR -> buildThisYearMonths(nowMillis)
            ReportPeriod.CUSTOM -> {
                if (customRange == null) {
                    buildLast12Months(nowMillis)
                } else {
                    val days = ((customRange.second - customRange.first) / (24L * 60 * 60 * 1000L)) + 1
                    if (days <= 31) {
                        buildCustomDays(customRange.first, customRange.second)
                    } else {
                        buildCustomMonths(customRange.first, customRange.second)
                    }
                }
            }
            ReportPeriod.TODAY -> buildSingleDay(nowMillis, "اليوم")
            ReportPeriod.YESTERDAY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                buildSingleDay(cal.timeInMillis, "أمس")
            }
            ReportPeriod.THIS_WEEK -> buildThisWeekDays(nowMillis)
            ReportPeriod.THIS_MONTH -> buildMonthDays(nowMillis, 0)
            ReportPeriod.LAST_MONTH -> buildMonthDays(nowMillis, -1)
        }

        // Map vouchers for initial payment deductions
        val linkedReceiptsBySession = vouchers
            .filter { it.type == VoucherType.RECEIPT && it.sessionId != null }
            .groupBy { it.sessionId!! }
            .mapValues { (_, rows) -> rows.sumOf { it.amount.coerceAtLeast(0.0) } }

        fun initialPayment(s: WaterSession): Double =
            (s.amountPaid - (linkedReceiptsBySession[s.id] ?: 0.0)).coerceAtLeast(0.0)

        return buckets.map { b ->
            val bucketSessions = sessions.filter { it.startTime >= b.startMillis && it.startTime <= b.endMillis }
            val bucketVouchers = vouchers.filter { it.date >= b.startMillis && it.date <= b.endMillis && it.type == VoucherType.RECEIPT }

            val rev = bucketSessions.sumOf { it.totalAmount.coerceAtLeast(0.0) }
            val col = bucketSessions.sumOf(::initialPayment) + bucketVouchers.sumOf { it.amount.coerceAtLeast(0.0) }

            b.copy(revenue = rev, collected = col)
        }
    }

    private fun buildSingleDay(timestamp: Long, label: String): List<TrendBucket> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        val dayNum = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = ARABIC_MONTH_NAMES[cal.get(Calendar.MONTH)]
        return listOf(
            TrendBucket(
                label = label,
                fullLabel = "$label ($dayNum $monthName)",
                startMillis = start,
                endMillis = end
            )
        )
    }

    private fun buildThisWeekDays(nowMillis: Long): List<TrendBucket> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysSinceSaturday = (dayOfWeek - Calendar.SATURDAY + 7) % 7
        cal.add(Calendar.DAY_OF_YEAR, -daysSinceSaturday)

        val dayNames = arrayOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
        val result = mutableListOf<TrendBucket>()

        for (i in 0..6) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            val dNum = cal.get(Calendar.DAY_OF_MONTH)
            val mName = ARABIC_MONTH_NAMES[cal.get(Calendar.MONTH)]
            result.add(
                TrendBucket(
                    label = dayNames[i],
                    fullLabel = "${dayNames[i]} $dNum $mName",
                    startMillis = start,
                    endMillis = end
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun buildMonthDays(nowMillis: Long, monthOffset: Int): List<TrendBucket> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            if (monthOffset != 0) add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthName = ARABIC_MONTH_NAMES[cal.get(Calendar.MONTH)]
        val result = mutableListOf<TrendBucket>()

        for (d in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            result.add(
                TrendBucket(
                    label = d.toString(),
                    fullLabel = "$d $monthName",
                    startMillis = start,
                    endMillis = end
                )
            )
        }
        return result
    }

    private fun buildCustomDays(startMillis: Long, endMillis: Long): List<TrendBucket> {
        val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val result = mutableListOf<TrendBucket>()

        while (cal.timeInMillis <= endMillis) {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = minOf(cal.timeInMillis, endMillis)

            val dNum = cal.get(Calendar.DAY_OF_MONTH)
            val mName = ARABIC_MONTH_NAMES[cal.get(Calendar.MONTH)]
            result.add(
                TrendBucket(
                    label = dNum.toString(),
                    fullLabel = "$dNum $mName",
                    startMillis = start,
                    endMillis = end
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    private fun buildLast12Months(nowMillis: Long): List<TrendBucket> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.MONTH, -11)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val result = mutableListOf<TrendBucket>()
        for (i in 0..11) {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            val mIdx = cal.get(Calendar.MONTH)
            val y = cal.get(Calendar.YEAR)
            val monthName = ARABIC_MONTH_NAMES[mIdx]
            result.add(
                TrendBucket(
                    label = monthName,
                    fullLabel = "$monthName $y",
                    startMillis = start,
                    endMillis = end
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }

    private fun buildThisYearMonths(nowMillis: Long): List<TrendBucket> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_YEAR, 1)
        }
        val y = cal.get(Calendar.YEAR)
        val result = mutableListOf<TrendBucket>()
        for (m in 0..11) {
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis

            val monthName = ARABIC_MONTH_NAMES[m]
            result.add(
                TrendBucket(
                    label = monthName,
                    fullLabel = "$monthName $y",
                    startMillis = start,
                    endMillis = end
                )
            )
        }
        return result
    }

    private fun buildCustomMonths(startMillis: Long, endMillis: Long): List<TrendBucket> {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val result = mutableListOf<TrendBucket>()
        while (cal.timeInMillis <= endMillis) {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = maxOf(cal.timeInMillis, startMillis)

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = minOf(cal.timeInMillis, endMillis)

            val mIdx = cal.get(Calendar.MONTH)
            val y = cal.get(Calendar.YEAR)
            val monthName = ARABIC_MONTH_NAMES[mIdx]
            result.add(
                TrendBucket(
                    label = monthName,
                    fullLabel = "$monthName $y",
                    startMillis = start,
                    endMillis = end
                )
            )
            cal.add(Calendar.MONTH, 1)
        }
        return result
    }
}

/**
 * Visual grouped-bar trend chart for Revenue and Collected cash.
 * Follows Arabic reading order: oldest on the right, newest on the left.
 * Bars, labels, grid, and tooltip are aligned with unified coordinates.
 */
@Composable
fun TrendChartCard(
    buckets: List<TrendBucket>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val isEmpty = buckets.isEmpty() || buckets.all { it.revenue == 0.0 && it.collected == 0.0 }
    var selectedIndex by remember(buckets) { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.mosarib.textMuted
    )
    val selectedLabelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )

    Card(
        modifier = modifier
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
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الإيراد والمحصّل",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "الإيراد",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.mosarib.success)
                        )
                        Text(
                            text = "المحصّل",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد حركات في هذه الفترة",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.mosarib.textMuted)
                    )
                }
            } else {
                // Tooltip display when a bar is selected
                selectedIndex?.let { idx ->
                    if (idx in buckets.indices) {
                        val b = buckets[idx]
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = b.fullLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "إيراد: ${Formatters.formatCurrency(b.revenue, currencySymbol)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "محصّل: ${Formatters.formatCurrency(b.collected, currencySymbol)}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.mosarib.success,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                val primaryColor = MaterialTheme.colorScheme.primary
                val successColor = MaterialTheme.mosarib.success
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                val selectedOverlayColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

                val maxVal = max(10.0, buckets.maxOf { max(it.revenue, it.collected) })
                val y1 = maxVal * 0.33
                val y2 = maxVal * 0.67
                val y3 = maxVal

                val chartHeightDp = 150.dp
                val labelHeightDp = 24.dp
                val totalCanvasHeightDp = chartHeightDp + labelHeightDp

                // In RTL Mosarib, the first child in a Row is placed on the RIGHT edge!
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Y-axis labels placed on the right edge
                    Column(
                        modifier = Modifier
                            .height(chartHeightDp)
                            .padding(start = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = ReportsFilter.formatCompactNumber(y3),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                        Text(
                            text = ReportsFilter.formatCompactNumber(y2),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                        Text(
                            text = ReportsFilter.formatCompactNumber(y1),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.mosarib.textMuted)
                        )
                    }

                    // Canvas chart with grouped bars and X-axis labels
                    // Bars and labels use unified coordinates: oldest on the right, newest on the left
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(totalCanvasHeightDp)
                            .pointerInput(buckets) {
                                detectTapGestures { offset ->
                                    val count = buckets.size
                                    if (count > 0) {
                                        val slotW = size.width / count
                                        // Oldest (index 0) is on the right
                                        val fromRight = size.width - offset.x
                                        val tappedIdx = (fromRight / slotW).toInt().coerceIn(0, count - 1)
                                        selectedIndex = if (selectedIndex == tappedIdx) null else tappedIdx
                                    }
                                }
                            }
                    ) {
                        val w = size.width
                        val chartH = chartHeightDp.toPx()
                        val count = buckets.size
                        val slotW = w / count

                        // Draw 3 horizontal grid lines across chart area
                        val gridY3 = 0f
                        val gridY2 = chartH * (1f - (y2 / maxVal).toFloat())
                        val gridY1 = chartH * (1f - (y1 / maxVal).toFloat())
                        val baselineY = chartH

                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        drawLine(gridColor, Offset(0f, gridY3), Offset(w, gridY3), pathEffect = pathEffect, strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, gridY2), Offset(w, gridY2), pathEffect = pathEffect, strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, gridY1), Offset(w, gridY1), pathEffect = pathEffect, strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, baselineY), Offset(w, baselineY), strokeWidth = 1.5f)

                        // Bar group dimensions within each slot
                        val totalGroupW = (slotW * 0.72f).coerceAtMost(28.dp.toPx())
                        val singleBarW = (totalGroupW / 2f).coerceAtLeast(2.dp.toPx())

                        // Render each bucket from oldest (index 0 on the right) to newest (on the left)
                        for (i in 0 until count) {
                            val b = buckets[i]
                            val slotRight = w - (i * slotW)
                            val slotLeft = slotRight - slotW
                            val centerX = slotRight - (slotW / 2f)

                            val revHeight = ((b.revenue / maxVal).toFloat() * chartH).coerceIn(0f, chartH)
                            val colHeight = ((b.collected / maxVal).toFloat() * chartH).coerceIn(0f, chartH)

                            // Highlight selected slot
                            if (selectedIndex == i) {
                                drawRoundRect(
                                    color = selectedOverlayColor,
                                    topLeft = Offset(slotLeft, 0f),
                                    size = Size(slotW, chartH),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }

                            // Revenue bar on right of pair, Collected bar on left of pair
                            val colLeft = centerX - totalGroupW / 2f
                            val revLeft = colLeft + singleBarW

                            if (revHeight > 0f) {
                                drawRoundRect(
                                    color = primaryColor,
                                    topLeft = Offset(revLeft, chartH - revHeight),
                                    size = Size(singleBarW, revHeight),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }

                            if (colHeight > 0f) {
                                drawRoundRect(
                                    color = successColor,
                                    topLeft = Offset(colLeft, chartH - colHeight),
                                    size = Size(singleBarW, colHeight),
                                    cornerRadius = CornerRadius(4f, 4f)
                                )
                            }

                            // X-axis label placed at the exact same centerX!
                            // "Show a label under every bar when there are ≤ 8 buckets, otherwise every second bar, with no overlap."
                            val showLabel = count <= 8 || (i % 2 == 0)
                            if (showLabel) {
                                val currentStyle = if (selectedIndex == i) selectedLabelStyle else labelStyle
                                val measured = textMeasurer.measure(b.label, style = currentStyle)
                                val textW = measured.size.width
                                val textX = (centerX - textW / 2f).coerceIn(0f, (w - textW).coerceAtLeast(0f))
                                val textY = chartH + 6.dp.toPx()
                                drawText(measured, topLeft = Offset(textX, textY))
                            }
                        }
                    }
                }
            }
        }
    }
}

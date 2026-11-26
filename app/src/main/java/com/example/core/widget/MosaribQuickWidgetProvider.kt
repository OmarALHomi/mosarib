package com.example.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.core.database.AppDatabase
import com.example.core.notifications.DeepLinks
import com.example.core.util.Formatters
import com.example.features.customers.Customer
import com.example.features.sessions.WaterSession
import com.example.features.settings.SettingsRepository
import com.example.features.vouchers.Voucher
import com.example.features.vouchers.VoucherType
import com.example.features.wellowners.WellOwnerPurchase
import com.example.features.wellowners.WellOwnerPurchaseMath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class TodayWidgetSummary(
    val sessionsCount: Int,
    val waterMinutes: Int,
    val receiptsTotal: Double
)

data class RecentWidgetOpRow(
    val type: String,
    val name: String,
    val quantity: String,
    val price: String,
    val paid: String,
    val remaining: String,
    val ts: Long = 0L,
    val remainingValue: Double = 0.0
)

enum class WidgetSizeMode {
    COMPACT_ONE_ROW,
    STANDARD_TWO_ROWS,
    EXPANDED_THREE_PLUS_ROWS
}

object MosaribWidgetSummary {

    fun dayRange(nowMillis: Long = System.currentTimeMillis()): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val end = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        return start to end
    }

    /**
     * Computes today's watering duration/count and collected receipts without double-counting
     * receipt vouchers that settle a session started today.
     */
    fun computeToday(
        sessions: List<WaterSession>,
        vouchers: List<Voucher>,
        nowMillis: Long = System.currentTimeMillis()
    ): TodayWidgetSummary {
        val (startOfDay, endOfDay) = dayRange(nowMillis)
        val todaySessions = sessions.filter { it.startTime in startOfDay..endOfDay }
        val todaySessionIds = todaySessions.mapTo(HashSet()) { it.id }

        val sessionReceipts = todaySessions.sumOf { it.amountPaid.coerceAtLeast(0.0) }
        val additionalReceipts = vouchers
            .filter { v ->
                v.type == VoucherType.RECEIPT &&
                    v.date in startOfDay..endOfDay &&
                    (v.sessionId == null || v.sessionId !in todaySessionIds)
            }
            .sumOf { it.amount.coerceAtLeast(0.0) }

        val totalMinutes = todaySessions.sumOf {
            (it.durationMinutes - it.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0)
        }
        return TodayWidgetSummary(
            sessionsCount = todaySessions.size,
            waterMinutes = totalMinutes,
            receiptsTotal = Formatters.roundMoney(sessionReceipts + additionalReceipts)
        )
    }

    fun sizeModeForHeightDp(minHeightDp: Int): WidgetSizeMode = when {
        minHeightDp in 1 until 125 -> WidgetSizeMode.COMPACT_ONE_ROW
        minHeightDp >= 225 -> WidgetSizeMode.EXPANDED_THREE_PLUS_ROWS
        else -> WidgetSizeMode.STANDARD_TWO_ROWS
    }

    fun sizeModeForOptions(options: Bundle): WidgetSizeMode {
        val semRowSpan = options.getInt("semAppWidgetRowSpan", 0)
        val minHeightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        if (semRowSpan == 1) return WidgetSizeMode.COMPACT_ONE_ROW
        if (semRowSpan >= 3 || minHeightDp >= 225) return WidgetSizeMode.EXPANDED_THREE_PLUS_ROWS
        return sizeModeForHeightDp(minHeightDp)
    }

    fun buildRecentOperationRows(
        sessions: List<WaterSession>,
        vouchers: List<Voucher>,
        purchases: List<WellOwnerPurchase>,
        customersById: Map<Long, Customer>,
        limit: Int = 4
    ): List<RecentWidgetOpRow> {
        data class Entry(val ts: Long, val row: RecentWidgetOpRow)

        val entries = mutableListOf<Entry>()
        sessions.forEach { s ->
            val custName = customersById[s.billedToCustomerId ?: s.customerId]?.name ?: "عميل"
            val qty = Formatters.formatDurationFriendly((s.durationMinutes - s.wastedMinutes.coerceAtLeast(0)).coerceAtLeast(0))
            entries.add(
                Entry(
                    s.startTime,
                    RecentWidgetOpRow(
                        type = "سقي",
                        name = custName,
                        quantity = qty,
                        price = Formatters.formatNumber(s.totalAmount),
                        paid = Formatters.formatNumber(s.amountPaid),
                        remaining = Formatters.formatNumber(s.remainingDebt),
                        remainingValue = s.remainingDebt
                    )
                )
            )
        }
        vouchers.forEach { v ->
            val custName = v.customerId?.let { customersById[it]?.name } ?: v.category.ifBlank { "عام" }
            val amt = Formatters.formatNumber(v.amount)
            val row = when (v.type) {
                VoucherType.RECEIPT -> RecentWidgetOpRow("قبض", custName, "-", amt, amt, "0")
                VoucherType.EXPENSE -> RecentWidgetOpRow("صرف", custName, "-", amt, amt, "0")
                VoucherType.DISCOUNT -> RecentWidgetOpRow("خصم", custName, "-", amt, "0", "0")
            }
            entries.add(Entry(v.date, row))
        }
        purchases.forEach { p ->
            val ownerName = customersById[p.ownerCustomerId]?.name ?: "صاحب بئر"
            val payable = WellOwnerPurchaseMath.payableAmount(p)
            val rem = (payable - p.amountPaid).coerceAtLeast(0.0)
            val qty = Formatters.formatDurationFriendly(WellOwnerPurchaseMath.chargeableMinutes(p))
            val ts = if (p.startTime > 0L) p.startTime else p.date
            entries.add(
                Entry(
                    ts,
                    RecentWidgetOpRow(
                        type = "شراء",
                        name = ownerName,
                        quantity = qty,
                        price = Formatters.formatNumber(payable),
                        paid = Formatters.formatNumber(p.amountPaid),
                        remaining = Formatters.formatNumber(rem),
                        remainingValue = rem
                    )
                )
            )
        }

        return entries.sortedByDescending { it.ts }.take(limit).map { it.row.copy(ts = it.ts) }
    }

    /** Last [limit] operations (sessions, vouchers, purchases), newest first. Empty when the app lock is on. */
    suspend fun loadRecentRows(context: Context, limit: Int = 20): List<RecentWidgetOpRow> {
        val db = AppDatabase.getDatabase(context.applicationContext)
        if (SettingsRepository(db.appSettingDao()).appConfig.first().biometricEnabled) return emptyList()
        return buildRecentOperationRows(
            sessions = db.waterSessionDao().getAllSessions().first().take(limit),
            vouchers = db.voucherDao().getAllVouchers().first().take(limit),
            purchases = db.wellOwnerPurchaseDao().getAllPurchases().first().take(limit),
            customersById = db.customerDao().getAllCustomers().first().associateBy { it.id },
            limit = limit
        )
    }

    fun formatOpsCount(n: Int): String = when (n) {
        1 -> "عملية واحدة"
        2 -> "عمليتان"
        in 3..10 -> "$n عمليات"
        else -> "$n عملية"
    }

    fun formatCompactDuration(minutes: Int): String {
        if (minutes <= 0) return "0د"
        val hrs = minutes / 60
        val rem = minutes % 60
        return when {
            hrs > 0 && rem > 0 -> "${hrs}س ${rem}د"
            hrs > 0 -> "${hrs}س"
            else -> "${rem}د"
        }
    }
}

class MosaribQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgetsAsync(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            requestWidgetRefresh(context)
        }
    }

    private fun updateWidgetsAsync(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val (startOfDay, endOfDay) = MosaribWidgetSummary.dayRange()
                val todaySessions = db.waterSessionDao().getSessionsBetween(startOfDay, endOfDay).first()
                val todayVouchers = db.voucherDao().getVouchersBetween(startOfDay, endOfDay).first()
                val config = SettingsRepository(db.appSettingDao()).appConfig.first()
                val summary = MosaribWidgetSummary.computeToday(todaySessions, todayVouchers)

                val sessionsText = "${Formatters.formatDurationFriendly(summary.waterMinutes)} • ${MosaribWidgetSummary.formatOpsCount(summary.sessionsCount)}"
                val receiptsText = Formatters.formatCurrency(summary.receiptsTotal, config.currencySymbol)
                val widgetAllowed = com.example.core.license.RemoteConfigManager.isHomeWidgetAllowed(appContext)
                val locked = config.biometricEnabled || !widgetAllowed

                for (widgetId in appWidgetIds) {
                    val opts = appWidgetManager.getAppWidgetOptions(widgetId)
                    val mode = MosaribWidgetSummary.sizeModeForOptions(opts)
                    val views = buildRemoteViews(
                        context = appContext,
                        widgetId = widgetId,
                        sizeMode = mode,
                        sessionsText = when {
                            !widgetAllowed -> "🔒 متوقف مؤقتاً"
                            config.biometricEnabled -> "🔒 مقفل"
                            else -> sessionsText
                        },
                        receiptsText = when {
                            !widgetAllowed -> "من الإدارة"
                            config.biometricEnabled -> "🔒 مقفل"
                            else -> receiptsText
                        },
                        locked = locked
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_recent_list)
            } catch (_: Exception) {
                for (widgetId in appWidgetIds) {
                    val views = buildRemoteViews(
                        context = appContext,
                        widgetId = widgetId,
                        sizeMode = WidgetSizeMode.STANDARD_TWO_ROWS,
                        sessionsText = "0 دقيقة • 0 عمليات",
                        receiptsText = "0"
                    )
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        widgetId: Int,
        sizeMode: WidgetSizeMode,
        sessionsText: String,
        receiptsText: String,
        locked: Boolean = false
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_record)

        val showHeader = sizeMode != WidgetSizeMode.COMPACT_ONE_ROW
        val vis = if (showHeader) View.VISIBLE else View.GONE
        views.setViewVisibility(R.id.widget_header, vis)
        views.setViewVisibility(R.id.widget_summary_row, vis)
        views.setViewVisibility(R.id.widget_compact_row, View.VISIBLE)
        views.setViewVisibility(
            R.id.widget_recent_section,
            if (sizeMode == WidgetSizeMode.EXPANDED_THREE_PLUS_ROWS && !locked) View.VISIBLE else View.GONE
        )
        views.setTextViewText(R.id.widget_today_sessions, sessionsText)
        views.setTextViewText(R.id.widget_today_receipts, receiptsText)

        val serviceIntent = Intent(context, MosaribWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = android.net.Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_recent_list, serviceIntent)
        views.setEmptyView(R.id.widget_recent_list, R.id.widget_recent_empty)

        val sessionPi = actionPendingIntent(context, QuickRecordActivity.MODE_SESSION, 1001 + widgetId)
        val voicePi = actionPendingIntent(context, QuickRecordActivity.MODE_VOICE, 2001 + widgetId)
        val receiptPi = actionPendingIntent(context, QuickRecordActivity.MODE_RECEIPT, 3001 + widgetId)
        val expensePi = actionPendingIntent(context, QuickRecordActivity.MODE_EXPENSE, 4001 + widgetId)
        val purchasePi = actionPendingIntent(context, QuickRecordActivity.MODE_PURCHASE, 7001 + widgetId)

        // action tiles (same in every size): سقي | قبض | صرف | شراء | صوتي
        views.setOnClickPendingIntent(R.id.widget_compact_session, sessionPi)
        views.setOnClickPendingIntent(R.id.widget_compact_receipt, receiptPi)
        views.setOnClickPendingIntent(R.id.widget_compact_expense, expensePi)
        views.setOnClickPendingIntent(R.id.widget_compact_purchase, purchasePi)
        views.setOnClickPendingIntent(R.id.widget_compact_voice, voicePi)

        val openOpsPi = PendingIntent.getActivity(
            context,
            6001 + widgetId,
            DeepLinks.appIntent(context, "mosarib://operations"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_recent_section, openOpsPi)
        views.setPendingIntentTemplate(R.id.widget_recent_list, openOpsPi)

        val refreshIntent = Intent(context, MosaribQuickWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPi = PendingIntent.getBroadcast(
            context,
            5001 + widgetId,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPi)
        return views
    }

    private fun actionPendingIntent(context: Context, mode: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, QuickRecordActivity::class.java).apply {
            action = "com.example.widget.QUICK_RECORD_$mode"
            putExtra(QuickRecordActivity.EXTRA_MODE, mode)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_REFRESH = "com.example.widget.ACTION_REFRESH"

        fun requestWidgetRefresh(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(appContext, MosaribQuickWidgetProvider::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(appContext, MosaribQuickWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            appContext.sendBroadcast(intent)
        }
    }
}

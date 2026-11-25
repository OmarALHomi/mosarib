package com.example.core.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.core.util.Formatters
import kotlinx.coroutines.runBlocking

class MosaribWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = Factory(applicationContext)

    private class Factory(private val context: Context) : RemoteViewsFactory {
        private var rows: List<RecentWidgetOpRow> = emptyList()

        override fun onCreate() {}

        // Runs on a binder thread, so blocking here is fine. Returns no rows when the app lock is on.
        override fun onDataSetChanged() {
            rows = try {
                runBlocking { MosaribWidgetSummary.loadRecentRows(context, 20) }
            } catch (_: Exception) {
                emptyList()
            }
        }

        override fun onDestroy() { rows = emptyList() }
        override fun getCount() = rows.size

        override fun getViewAt(position: Int): RemoteViews {
            val r = rows.getOrNull(position) ?: return RemoteViews(context.packageName, R.layout.widget_op_row)
            val v = RemoteViews(context.packageName, R.layout.widget_op_row)
            val color = when (r.type) {
                "سقي" -> Color.parseColor("#FF4FC3F7")
                "قبض" -> Color.parseColor("#FF6EE7B7")
                "صرف" -> Color.parseColor("#FFFF8A8A")
                "شراء" -> Color.parseColor("#FFFFD54F")
                else -> Color.parseColor("#FFAABCC6")
            }
            v.setInt(R.id.op_stripe, "setBackgroundColor", color)
            v.setTextViewText(R.id.op_type, r.type)
            v.setTextColor(R.id.op_type, color)
            v.setTextViewText(R.id.op_name, r.name)
            v.setTextViewText(R.id.op_date, if (r.ts > 0) "${Formatters.formatDateShort(r.ts)} ${Formatters.formatTime(r.ts)}" else "")
            v.setTextViewText(R.id.op_duration, r.quantity)
            v.setViewVisibility(R.id.op_duration, if (r.quantity.isBlank() || r.quantity == "-") View.GONE else View.VISIBLE)

            if (r.type == "قبض" || r.type == "صرف" || r.type == "خصم") {
                v.setTextViewText(R.id.op_value, "المبلغ: ${r.price}")
                v.setViewVisibility(R.id.op_paid, View.GONE)
                v.setViewVisibility(R.id.op_rem, View.GONE)
            } else {
                v.setTextViewText(R.id.op_value, "القيمة: ${r.price}")
                v.setViewVisibility(R.id.op_paid, View.VISIBLE)
                v.setTextViewText(R.id.op_paid, "الواصل: ${r.paid}")
                v.setViewVisibility(R.id.op_rem, View.VISIBLE)
                if (r.remainingValue > 0.0) {
                    v.setTextViewText(R.id.op_rem, "الباقي: ${r.remaining}")
                    v.setTextColor(R.id.op_rem, Color.parseColor("#FFFF8A8A"))
                } else {
                    v.setTextViewText(R.id.op_rem, "خالص")
                    v.setTextColor(R.id.op_rem, Color.parseColor("#FF6EE7B7"))
                }
            }
            v.setOnClickFillInIntent(R.id.op_root, Intent())
            return v
        }

        override fun getLoadingView(): RemoteViews? = null
        override fun getViewTypeCount() = 1
        override fun getItemId(position: Int) = position.toLong()
        override fun hasStableIds() = false
    }
}

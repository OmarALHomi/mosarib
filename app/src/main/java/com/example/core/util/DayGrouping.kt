package com.example.core.util

import com.example.core.ui.RowGroupPosition
import java.util.Calendar
import java.util.Locale

data class DayGroupItem<T>(
    val item: T,
    val position: RowGroupPosition
)

data class DayGroup<T>(
    val dayKey: String,
    val headerTitle: String,
    val operationsCountText: String,
    val items: List<DayGroupItem<T>>
)

object DayGrouping {
    private val ARABIC_DAY_NAMES = mapOf(
        Calendar.SUNDAY to "الأحد",
        Calendar.MONDAY to "الإثنين",
        Calendar.TUESDAY to "الثلاثاء",
        Calendar.WEDNESDAY to "الأربعاء",
        Calendar.THURSDAY to "الخميس",
        Calendar.FRIDAY to "الجمعة",
        Calendar.SATURDAY to "السبت"
    )

    private val ARABIC_MONTH_NAMES = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    fun formatDayHeader(timestamp: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }

        val targetYear = targetCal.get(Calendar.YEAR)
        val targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR)

        val nowYear = nowCal.get(Calendar.YEAR)
        val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        val dayName = ARABIC_DAY_NAMES[targetCal.get(Calendar.DAY_OF_WEEK)] ?: ""
        val dayOfMonth = targetCal.get(Calendar.DAY_OF_MONTH)
        val monthName = ARABIC_MONTH_NAMES.getOrElse(targetCal.get(Calendar.MONTH)) { "" }

        val isToday = (targetYear == nowYear && targetDayOfYear == nowDayOfYear)

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = (targetYear == yesterdayCal.get(Calendar.YEAR) && targetDayOfYear == yesterdayCal.get(Calendar.DAY_OF_YEAR))

        return when {
            isToday -> "اليوم · $dayName $dayOfMonth $monthName"
            isYesterday -> "أمس · $dayName $dayOfMonth $monthName"
            targetYear == nowYear -> "$dayName $dayOfMonth $monthName"
            else -> "$dayName $dayOfMonth $monthName $targetYear"
        }
    }

    fun formatDayKey(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "d_%04d%02d%02d", y, m, d)
    }

    fun formatOperationsCount(count: Int): String {
        return "$count عمليات"
    }

    fun <T> groupByDay(
        items: List<T>,
        timestampSelector: (T) -> Long,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DayGroup<T>> {
        if (items.isEmpty()) return emptyList()

        val groupsMap = LinkedHashMap<String, MutableList<T>>()
        val dayTimestampMap = LinkedHashMap<String, Long>()

        for (item in items) {
            val ts = timestampSelector(item)
            val key = formatDayKey(ts)
            groupsMap.getOrPut(key) { mutableListOf() }.add(item)
            if (!dayTimestampMap.containsKey(key)) {
                dayTimestampMap[key] = ts
            }
        }

        return groupsMap.map { (key, groupItems) ->
            val ts = dayTimestampMap[key] ?: 0L
            val headerTitle = formatDayHeader(ts, nowMillis)
            val countText = formatOperationsCount(groupItems.size)
            val size = groupItems.size

            val mappedItems = groupItems.mapIndexed { index, item ->
                val position = when {
                    size == 1 -> RowGroupPosition.Single
                    index == 0 -> RowGroupPosition.First
                    index == size - 1 -> RowGroupPosition.Last
                    else -> RowGroupPosition.Middle
                }
                DayGroupItem(item, position)
            }

            DayGroup(
                dayKey = key,
                headerTitle = headerTitle,
                operationsCountText = countText,
                items = mappedItems
            )
        }
    }
}


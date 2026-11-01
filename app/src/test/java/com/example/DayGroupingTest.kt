package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import com.example.core.ui.RowGroupPosition
import com.example.core.util.DayGrouping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DayGroupingTest {

    @Test
    fun testIconsExist() {
        assertNotNull(Icons.Default.SouthWest)
        assertNotNull(Icons.Default.NorthEast)
    }

    @Test
    fun testDayHeaderLabels() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = nowCal.timeInMillis

        // 1. Today: 1 Nov 2026 (Sunday)
        val todayLabel = DayGrouping.formatDayHeader(nowMillis, nowMillis)
        assertEquals("اليوم · الأحد 1 نوفمبر", todayLabel)

        // 2. Yesterday: 31 Oct 2026 (Saturday)
        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayLabel = DayGrouping.formatDayHeader(yesterdayCal.timeInMillis, nowMillis)
        assertEquals("أمس · السبت 31 أكتوبر", yesterdayLabel)

        // 3. Older day same year: 29 Oct 2026 (Thursday)
        val olderCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 29, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val olderLabel = DayGrouping.formatDayHeader(olderCal.timeInMillis, nowMillis)
        assertEquals("الخميس 29 أكتوبر", olderLabel)

        // 4. Other year: 15 March 2025 (Saturday) -> year shown only for other years
        val otherYearCal = Calendar.getInstance().apply {
            set(2025, Calendar.MARCH, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val otherYearLabel = DayGrouping.formatDayHeader(otherYearCal.timeInMillis, nowMillis)
        assertEquals("السبت 15 مارس 2025", otherYearLabel)
    }

    @Test
    fun testPositionsForSingleAndMultipleItems() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t1 = nowCal.timeInMillis
        val t2 = t1 + 1000
        val t3 = t1 + 2000

        // Single item day
        val singleList = listOf(t1)
        val singleGroup = DayGrouping.groupByDay(singleList, { it }, nowCal.timeInMillis)
        assertEquals(1, singleGroup.size)
        assertEquals(1, singleGroup[0].items.size)
        assertEquals(RowGroupPosition.Single, singleGroup[0].items[0].position)
        assertEquals("1 عمليات", singleGroup[0].operationsCountText)

        // 3 items day
        val threeList = listOf(t1, t2, t3)
        val threeGroup = DayGrouping.groupByDay(threeList, { it }, nowCal.timeInMillis)
        assertEquals(1, threeGroup.size)
        assertEquals(3, threeGroup[0].items.size)
        assertEquals(RowGroupPosition.First, threeGroup[0].items[0].position)
        assertEquals(RowGroupPosition.Middle, threeGroup[0].items[1].position)
        assertEquals(RowGroupPosition.Last, threeGroup[0].items[2].position)
        assertEquals("3 عمليات", threeGroup[0].operationsCountText)
    }
}

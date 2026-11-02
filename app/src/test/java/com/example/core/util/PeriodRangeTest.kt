package com.example.core.util

import com.example.features.reports.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PeriodRangeTest {

    @Test
    fun testAllPeriodReturnsNullRange() {
        val (start, end) = PeriodRange.rangeFor(ReportPeriod.ALL)
        assertNull(start)
        assertNull(end)
    }

    @Test
    fun testCustomPeriodUsesCustomRange() {
        val custom = Pair(1000L, 2000L)
        val (start, end) = PeriodRange.rangeFor(ReportPeriod.CUSTOM, customRange = custom)
        assertEquals(1000L, start)
        assertEquals(2000L, end)
    }

    @Test
    fun testTodayPeriod() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val (start, end) = PeriodRange.rangeFor(ReportPeriod.TODAY, nowMillis = cal.timeInMillis)
        assertNotNull(start)
        assertNotNull(end)

        val startCal = Calendar.getInstance().apply { timeInMillis = start!! }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, startCal.get(Calendar.MONTH))
        assertEquals(2, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(0, startCal.get(Calendar.MILLISECOND))

        val endCal = Calendar.getInstance().apply { timeInMillis = end!! }
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
        assertEquals(2, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testYesterdayPeriod() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2, 10, 0, 0)
        }
        val (start, end) = PeriodRange.rangeFor(ReportPeriod.YESTERDAY, nowMillis = cal.timeInMillis)
        assertNotNull(start)
        assertNotNull(end)

        val startCal = Calendar.getInstance().apply { timeInMillis = start!! }
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        val endCal = Calendar.getInstance().apply { timeInMillis = end!! }
        assertEquals(1, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testThisWeekStartsOnSaturday() {
        // Monday Nov 2, 2026 -> Saturday was Oct 31, 2026
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 2, 12, 0, 0)
        }
        val (start, end) = PeriodRange.rangeFor(ReportPeriod.THIS_WEEK, nowMillis = cal.timeInMillis)
        assertNotNull(start)
        assertNotNull(end)

        val startCal = Calendar.getInstance().apply { timeInMillis = start!! }
        assertEquals(Calendar.SATURDAY, startCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(31, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        val endCal = Calendar.getInstance().apply { timeInMillis = end!! }
        assertEquals(Calendar.FRIDAY, endCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(6, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
    }

    @Test
    fun testThisMonthPeriod() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 12, 0, 0)
        }
        val (start, end) = PeriodRange.rangeFor(ReportPeriod.THIS_MONTH, nowMillis = cal.timeInMillis)
        assertNotNull(start)
        assertNotNull(end)

        val startCal = Calendar.getInstance().apply { timeInMillis = start!! }
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.NOVEMBER, startCal.get(Calendar.MONTH))

        val endCal = Calendar.getInstance().apply { timeInMillis = end!! }
        assertEquals(30, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.NOVEMBER, endCal.get(Calendar.MONTH))
    }

    @Test
    fun testLastMonthPeriodWithYearBoundary() {
        // January 15, 2027 -> Last month should be December 2026 (1 to 31)
        val cal = Calendar.getInstance().apply {
            set(2027, Calendar.JANUARY, 15, 10, 0, 0)
        }
        val (start, end) = PeriodRange.rangeFor(ReportPeriod.LAST_MONTH, nowMillis = cal.timeInMillis)
        assertNotNull(start)
        assertNotNull(end)

        val startCal = Calendar.getInstance().apply { timeInMillis = start!! }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, startCal.get(Calendar.MONTH))
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        val endCal = Calendar.getInstance().apply { timeInMillis = end!! }
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, endCal.get(Calendar.MONTH))
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }
}

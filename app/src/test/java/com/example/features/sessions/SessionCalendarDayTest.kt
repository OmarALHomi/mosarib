package com.example.features.sessions

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SessionCalendarDayTest {

    private fun createMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun sameDay_daytime_returnsFalse() {
        val start = createMillis(2026, Calendar.NOVEMBER, 2, 4, 45)
        val end = createMillis(2026, Calendar.NOVEMBER, 2, 5, 45)
        assertFalse(isEndDayAfterStartDay(start, end))
    }

    @Test
    fun sameDay_morningToEvening_returnsFalse() {
        val start = createMillis(2026, Calendar.NOVEMBER, 2, 8, 0)
        val end = createMillis(2026, Calendar.NOVEMBER, 2, 22, 30)
        assertFalse(isEndDayAfterStartDay(start, end))
    }

    @Test
    fun crossingMidnight_returnsTrue() {
        val start = createMillis(2026, Calendar.NOVEMBER, 2, 23, 30)
        val end = createMillis(2026, Calendar.NOVEMBER, 3, 1, 30)
        assertTrue(isEndDayAfterStartDay(start, end))
    }

    @Test
    fun crossingYearEnd_returnsTrue() {
        val start = createMillis(2026, Calendar.DECEMBER, 31, 23, 0)
        val end = createMillis(2027, Calendar.JANUARY, 1, 2, 0)
        assertTrue(isEndDayAfterStartDay(start, end))
    }

    @Test
    fun reversedDates_endBeforeStart_returnsFalse() {
        val start = createMillis(2026, Calendar.NOVEMBER, 3, 10, 0)
        val end = createMillis(2026, Calendar.NOVEMBER, 2, 10, 0)
        assertFalse(isEndDayAfterStartDay(start, end))
    }

    @Test
    fun twoDaysLater_returnsTrue() {
        val start = createMillis(2026, Calendar.NOVEMBER, 2, 10, 0)
        val end = createMillis(2026, Calendar.NOVEMBER, 4, 10, 0)
        assertTrue(isEndDayAfterStartDay(start, end))
    }
}

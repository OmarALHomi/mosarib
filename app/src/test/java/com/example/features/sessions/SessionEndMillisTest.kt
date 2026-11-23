package com.example.features.sessions

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class SessionEndMillisTest {
    private fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear(); set(2026, Calendar.NOVEMBER, day, hour, minute, 0)
    }

    @Test
    fun sameDay() {
        assertEquals(at(2, 13, 30).timeInMillis, sessionEndMillis(at(2, 10, 0).timeInMillis, 210))
    }

    @Test
    fun crossesMidnight() {
        val end = Calendar.getInstance().apply { timeInMillis = sessionEndMillis(at(2, 22, 30).timeInMillis, 5 * 60 + 15) }
        assertEquals(3, end.get(Calendar.DAY_OF_MONTH))
        assertEquals(3, end.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, end.get(Calendar.MINUTE))
    }
}

class SessionStartFromDurationTest {
    @Test
    fun nowMinusDuration_dropsSeconds() {
        val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 2, 10, 30, 45) }.timeInMillis
        val want = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, 2, 8, 30, 0) }.timeInMillis
        assertEquals(want, sessionStartFromDuration(now, 120))
    }
}

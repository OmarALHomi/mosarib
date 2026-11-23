package com.example.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeConversionTest {
    @Test
    fun roundTripsAll24Hours() {
        for (h in 0..23) assertEquals(h, hour12To24(hour24To12(h), h >= 12))
    }

    @Test
    fun edgeHours() {
        assertEquals(0, hour12To24(12, false))
        assertEquals(12, hour12To24(12, true))
        assertEquals(12, hour24To12(0))
        assertEquals(12, hour24To12(12))
        assertEquals(1, hour24To12(13))
    }
}

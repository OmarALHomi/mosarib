package com.example.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormValidationTest {
    private enum class F { CUSTOMER, START, END, PRICE }

    @Test
    fun returnsFirstMissingInScreenOrder() {
        val r = firstMissingField(F.CUSTOMER to true, F.START to false, F.END to false, F.PRICE to false)
        assertEquals(F.START, r)
    }

    @Test
    fun customerComesFirst() {
        assertEquals(F.CUSTOMER, firstMissingField(F.CUSTOMER to false, F.START to false))
    }

    @Test
    fun nullWhenAllFilled() {
        assertNull(firstMissingField(F.CUSTOMER to true, F.PRICE to true))
    }

    @Test
    fun durationAloneIsEnoughForTime() {
        assertEquals(true, hasTimeInput(false, false, 120))
        assertEquals(true, hasTimeInput(true, true, 0))
        assertEquals(false, hasTimeInput(true, false, 0))
    }

    @Test
    fun nothingGivenPointsToDuration() {
        val r = firstMissingField(F.CUSTOMER to true, F.END to hasTimeInput(false, false, 0), F.PRICE to false)
        assertEquals(F.END, r)
        assertNull(firstMissingField(F.END to hasTimeInput(false, false, 120)))
    }

    @Test
    fun clearedDurationWithUnpickedTimesIsNoTime() {
        // after the duration is cleared the auto times are reset to unpicked
        assertEquals(false, hasTimeInput(false, false, 0))
        assertEquals(F.END, firstMissingField(F.END to hasTimeInput(false, false, 0)))
        // user-picked times keep the duration alive
        assertEquals(true, hasTimeInput(true, true, 90))
    }
}

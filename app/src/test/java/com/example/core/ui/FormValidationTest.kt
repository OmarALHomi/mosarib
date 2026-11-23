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
}

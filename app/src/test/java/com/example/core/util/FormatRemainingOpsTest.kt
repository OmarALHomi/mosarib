package com.example.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatRemainingOpsTest {
    @Test
    fun smallShownAsNumber() {
        assertEquals("42", Formatters.formatRemainingOps(42))
        assertEquals("99999", Formatters.formatRemainingOps(99_999, true))
    }

    @Test
    fun hugeShownAsUnlimited() {
        assertEquals("∞", Formatters.formatRemainingOps(100_000))
        assertEquals("غير محدود", Formatters.formatRemainingOps(1783788676, true))
    }
}

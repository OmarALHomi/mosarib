package com.example.core.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateManagerTest {

    @Test
    fun testShouldForceUpdate_allFiveRequiredCases() {
        // (4, 5, true) → true
        assertTrue(
            "Current 4 < Min 5 with flag true must force update",
            AppUpdateManager.shouldForceUpdate(currentCode = 4, minCode = 5, isForceUpdateFlag = true)
        )

        // (5, 5, true) → false
        assertFalse(
            "Current 5 == Min 5 with flag true must NOT force update",
            AppUpdateManager.shouldForceUpdate(currentCode = 5, minCode = 5, isForceUpdateFlag = true)
        )

        // (6, 5, true) → false
        assertFalse(
            "Current 6 > Min 5 with flag true must NOT force update",
            AppUpdateManager.shouldForceUpdate(currentCode = 6, minCode = 5, isForceUpdateFlag = true)
        )

        // (4, 1, true) → false
        assertFalse(
            "Current 4 > Min 1 with flag true must NOT force update",
            AppUpdateManager.shouldForceUpdate(currentCode = 4, minCode = 1, isForceUpdateFlag = true)
        )

        // (4, 5, false) → true
        assertTrue(
            "Current 4 < Min 5 with flag false must force update",
            AppUpdateManager.shouldForceUpdate(currentCode = 4, minCode = 5, isForceUpdateFlag = false)
        )
    }
}

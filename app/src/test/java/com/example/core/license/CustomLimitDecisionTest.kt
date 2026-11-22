package com.example.core.license

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLimitDecisionTest {
    // Phone prefs from the bug report: custom limit 1783793664, 4987 ops used.
    @Test fun customLimitAllowsEvenAfterTrialUsedUp() =
        assertTrue(LicenseManager.isWithinCustomLimit(4987, 0, 1783793664))

    @Test fun blocksAtLimit() = assertFalse(LicenseManager.isWithinCustomLimit(10, 3, 10))

    @Test fun usesLargerOfStoredAndCurrent() {
        assertFalse(LicenseManager.isWithinCustomLimit(3, 10, 10))
        assertTrue(LicenseManager.isWithinCustomLimit(3, 9, 10))
    }
}

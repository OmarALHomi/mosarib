package com.example.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OperationGateTest {
    @Test fun showsOnceWhenLimitExceeded() {
        assertTrue(shouldAutoShowLicenseSheet(limitExceeded = true, alreadyShown = false))
        assertFalse(shouldAutoShowLicenseSheet(limitExceeded = true, alreadyShown = true))
        assertFalse(shouldAutoShowLicenseSheet(limitExceeded = false, alreadyShown = false))
    }
}

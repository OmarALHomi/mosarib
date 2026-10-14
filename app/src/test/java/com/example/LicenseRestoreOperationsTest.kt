package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.license.LicenseManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicenseRestoreOperationsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val sp = context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
        sp.edit().clear().commit()
        LicenseManager.resetTrialConsumptionForRestore(context)
    }

    @Test
    fun testRestoreWithOperationsBelowLimitAllowsContinuation() {
        val restoredOps = 20
        val limitExceeded = LicenseManager.syncOperationsAfterRestore(context, restoredOps)

        assertFalse("Restoring 20 ops should NOT exceed limit", limitExceeded)
        assertTrue("User should be allowed to perform operations", LicenseManager.canPerformOperation(context, restoredOps))
        assertEquals(
            "Remaining operations should be 30",
            LicenseManager.FREE_OPERATIONS_LIMIT - restoredOps,
            LicenseManager.getRemainingOperations(context, restoredOps)
        )
        assertFalse("Trial should NOT be consumed", LicenseManager.isTrialConsumed(context))
    }

    @Test
    fun testRestoreWithOperationsAtOrAboveLimitLocksApp() {
        val restoredOps = 250
        val limitExceeded = LicenseManager.syncOperationsAfterRestore(context, restoredOps)

        assertTrue("Restoring 250 ops MUST exceed limit", limitExceeded)
        assertFalse("User should NOT be allowed to perform more operations", LicenseManager.canPerformOperation(context, restoredOps))
        assertEquals(
            "Remaining operations should be 0",
            0,
            LicenseManager.getRemainingOperations(context, restoredOps)
        )
        assertTrue("Trial MUST be marked as consumed", LicenseManager.isTrialConsumed(context))
    }

    @Test
    fun testRestoreExactLimitLocksApp() {
        val restoredOps = LicenseManager.FREE_OPERATIONS_LIMIT
        val limitExceeded = LicenseManager.syncOperationsAfterRestore(context, restoredOps)

        assertTrue("Restoring exact limit ops MUST exceed limit", limitExceeded)
        assertFalse("User should NOT be allowed to perform operations", LicenseManager.canPerformOperation(context, restoredOps))
        assertEquals(0, LicenseManager.getRemainingOperations(context, restoredOps))
        assertTrue("Trial MUST be marked as consumed", LicenseManager.isTrialConsumed(context))
    }
}

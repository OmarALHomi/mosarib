package com.example

import com.example.core.sync.RESYNC_MIN_INTERVAL_MS
import com.example.core.sync.shouldResync
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResyncThrottleTest {
    @Test fun neverSyncedRuns() = assertTrue(shouldResync(0L, 1_000_000_000L))
    @Test fun withinWindowSkips() = assertFalse(shouldResync(1000L, 1000L + RESYNC_MIN_INTERVAL_MS - 1))
    @Test fun afterWindowRuns() = assertTrue(shouldResync(1000L, 1000L + RESYNC_MIN_INTERVAL_MS))
}

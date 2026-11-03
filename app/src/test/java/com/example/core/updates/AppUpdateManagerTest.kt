package com.example.core.updates

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class AppUpdateManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        val sp = context.getSharedPreferences("mosarib_license_prefs", Context.MODE_PRIVATE)
        sp.edit().clear().commit()
    }

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

    @Test
    fun testForceUpdateCacheFollowsDashboard() {
        // Initially default cached minimum is 1
        assertEquals(1, AppUpdateManager.getCachedMinVersionCode(context))

        // 1. Dashboard publishes force update with minCode = 5
        AppUpdateManager.updateCachedForceUpdate(
            context = context,
            minVersionCode = 5,
            requiredVersionName = "1.5.0",
            actionUrl = "https://example.com/update",
            message = "تحديث هام"
        )
        assertEquals(5, AppUpdateManager.getCachedMinVersionCode(context))
        assertTrue(
            "An app at version 4 is blocked offline when cached min is 5",
            AppUpdateManager.shouldForceUpdate(currentCode = 4, minCode = AppUpdateManager.getCachedMinVersionCode(context))
        )

        // 2. Dashboard cancels force update: broadcast changes back to minCode = 1
        // Cache must be overwritten every time (including 1) so offline users are not blocked!
        AppUpdateManager.updateCachedForceUpdate(
            context = context,
            minVersionCode = 1,
            requiredVersionName = "",
            actionUrl = "",
            message = ""
        )
        assertEquals(1, AppUpdateManager.getCachedMinVersionCode(context))
        assertFalse(
            "An app at version 4 is no longer blocked offline after dashboard cancelled force update",
            AppUpdateManager.shouldForceUpdate(currentCode = 4, minCode = AppUpdateManager.getCachedMinVersionCode(context))
        )

        // 3. Set cache back to 6, then test clearing when broadcast is empty
        AppUpdateManager.updateCachedForceUpdate(context = context, minVersionCode = 6)
        assertEquals(6, AppUpdateManager.getCachedMinVersionCode(context))

        // When /app_broadcast is deleted / empty, cache is cleared
        AppUpdateManager.clearForceUpdateCache(context)
        assertEquals(
            "After clearing cache, getCachedMinVersionCode returns default 1",
            1,
            AppUpdateManager.getCachedMinVersionCode(context)
        )
        assertFalse(
            "An app at version 4 is NOT blocked offline when broadcast is removed",
            AppUpdateManager.shouldForceUpdate(currentCode = 4, minCode = AppUpdateManager.getCachedMinVersionCode(context))
        )
    }

    @Test
    fun testBroadcastDismissalForToday() {
        val broadcastId = "test_bc_123"
        assertFalse(AppUpdateManager.isBroadcastSeen(context, broadcastId))

        // Dismiss for today
        AppUpdateManager.dismissBroadcastForToday(context, broadcastId)
        assertTrue(
            "Broadcast must be marked seen/suppressed for today",
            AppUpdateManager.isBroadcastSeen(context, broadcastId)
        )

        // Permanent seen
        val permId = "perm_bc_456"
        assertFalse(AppUpdateManager.isBroadcastSeen(context, permId))
        AppUpdateManager.markBroadcastSeen(context, permId)
        assertTrue(AppUpdateManager.isBroadcastSeen(context, permId))
    }
}

package com.example.core.ui.guide

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
class GuideTourTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        GuidePreferences.resetAll(context)
    }

    @Test
    fun testGuidePreferencesDoneAndReset() {
        // Initial state: tour and hints not done
        assertFalse(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_HOME))
        assertFalse(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_SESSION_FORM))
        assertFalse(GuidePreferences.isHintDone(context, "test_hint"))

        // Mark done
        GuidePreferences.markTourDone(context, GuidePreferences.TOUR_HOME, true)
        GuidePreferences.markHintDone(context, "test_hint", true)

        assertTrue(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_HOME))
        assertFalse(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_SESSION_FORM))
        assertTrue(GuidePreferences.isHintDone(context, "test_hint"))

        // Reset all
        GuidePreferences.resetAll(context)
        assertFalse(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_HOME))
        assertFalse(GuidePreferences.isHintDone(context, "test_hint"))
    }

    @Test
    fun testMigrationFromLegacyHomeTourCompleted() {
        // Given legacy home tour was completed in LicenseManager before GuidePreferences existed
        LicenseManager.setHomeTourCompleted(context, true)

        // Then GuidePreferences automatically treats the home tour as done and migrates it
        assertTrue(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_HOME))

        // And other tours remain unmigrated / false
        assertFalse(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_SESSION_FORM))
        assertFalse(GuidePreferences.isTourDone(context, GuidePreferences.TOUR_VOUCHER_FORM))
    }

    @Test
    fun testBubblePlacementTargetNearBottomPlacesBubbleAbove() {
        // Screen 400x800, target near bottom (y = 700 to 760)
        val target = GuideRect(left = 50f, top = 700f, right = 350f, bottom = 760f)
        val bubbleWidth = 300f
        val bubbleHeight = 150f
        val screenWidth = 400f
        val screenHeight = 800f
        val margin = 16f
        val arrowHeight = 8f

        val placement = GuidePlacement.calculateBubblePlacement(
            target = target,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            margin = margin,
            arrowHeight = arrowHeight
        )

        // Since remaining space below is 800 - 760 - 16 = 24 < (150 + 8), bubble must go ABOVE
        assertFalse("Bubble must be placed above target (isArrowOnTop = false)", placement.isArrowOnTop)
        assertTrue("Bubble Y must be above target.top", placement.y < target.top)
        assertEquals("Bubble Y position", 700f - arrowHeight - bubbleHeight, placement.y, 0.1f)
    }

    @Test
    fun testBubblePlacementTargetNearTopPlacesBubbleBelow() {
        // Screen 400x800, target near top (y = 40 to 90)
        val target = GuideRect(left = 50f, top = 40f, right = 350f, bottom = 90f)
        val bubbleWidth = 300f
        val bubbleHeight = 150f
        val screenWidth = 400f
        val screenHeight = 800f
        val margin = 16f
        val arrowHeight = 8f

        val placement = GuidePlacement.calculateBubblePlacement(
            target = target,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            margin = margin,
            arrowHeight = arrowHeight
        )

        // Plenty of room below: bubble must go BELOW
        assertTrue("Bubble must be placed below target (isArrowOnTop = true)", placement.isArrowOnTop)
        assertTrue("Bubble Y must be below target.bottom", placement.y > target.bottom)
        assertEquals("Bubble Y position", 90f + arrowHeight, placement.y, 0.1f)
    }

    @Test
    fun testBubblePlacementAtEdgesKeepsInsideMargins() {
        val bubbleWidth = 300f
        val bubbleHeight = 120f
        val screenWidth = 400f
        val screenHeight = 800f
        val margin = 16f

        // 1. Target hugging the right edge
        val rightTarget = GuideRect(left = 370f, top = 200f, right = 400f, bottom = 250f)
        val rightPlacement = GuidePlacement.calculateBubblePlacement(
            target = rightTarget,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            margin = margin
        )

        // Bubble must not exceed screenWidth - margin
        assertTrue("Bubble right edge within margin", rightPlacement.x + bubbleWidth <= screenWidth - margin + 0.1f)
        assertEquals("Bubble X clamped to max available", screenWidth - bubbleWidth - margin, rightPlacement.x, 0.1f)

        // 2. Target hugging the left edge
        val leftTarget = GuideRect(left = 0f, top = 200f, right = 30f, bottom = 250f)
        val leftPlacement = GuidePlacement.calculateBubblePlacement(
            target = leftTarget,
            bubbleWidth = bubbleWidth,
            bubbleHeight = bubbleHeight,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            margin = margin
        )

        // Bubble must not be less than margin
        assertTrue("Bubble X >= margin", leftPlacement.x >= margin - 0.1f)
        assertEquals("Bubble X clamped to margin", margin, leftPlacement.x, 0.1f)
    }
}

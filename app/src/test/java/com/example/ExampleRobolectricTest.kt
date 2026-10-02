package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.core.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("بيننا", context.getString(R.string.app_name))
        assertEquals("مستودع حساباتك ومعاملاتك", context.getString(R.string.app_subtitle))
    }

    /**
     * هوية التطبيق المعتمدة: قرار المالك 2026-10-03. لا تثبيت سابق لأي مستخدم، فالتغيير الآن
     * لا يكسر مسار تحديث. بعد أول توزيع حقيقي يفشل هذا الاختبار إن غُيّر المعرّف سهوًا.
     */
    @Test
    fun `application identity is the approved one`() {
        assertEquals("com.baynana.app", BuildConfig.APPLICATION_ID)
        assertEquals(false, BuildConfig.APPLICATION_ID.contains("com.example"))
        assertEquals(false, BuildConfig.APPLICATION_ID.contains("omarAlhomi"))
    }

    @Test
    fun `launch MainActivity`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun `water cost calculation test`() {
        // 3 hours 30 minutes = 210 minutes at 5000 rate/hr => 17500
        val cost = Formatters.calculateWaterCost(210, 5000.0)
        assertEquals(17500.0, cost, 0.001)

        // 45 minutes at 6000 rate/hr => 4500
        val cost45 = Formatters.calculateWaterCost(45, 6000.0)
        assertEquals(4500.0, cost45, 0.001)
    }

    @Test
    fun `format duration arabic tests`() {
        assertEquals("0 دقيقة", Formatters.formatDurationArabic(0))
        assertEquals("ساعة واحدة", Formatters.formatDurationArabic(60))
        assertEquals("ساعتان", Formatters.formatDurationArabic(120))
        assertEquals("3 ساعات و 30 دقيقة", Formatters.formatDurationArabic(210))
        assertEquals("45 دقيقة", Formatters.formatDurationArabic(45))
        assertEquals("دقيقة واحدة", Formatters.formatDurationArabic(1))
    }

    @Test
    fun `format clock and currency tests`() {
        val clock = Formatters.formatDurationClock(3665)
        assertEquals("01:01:05", clock)

        val curr = Formatters.formatCurrency(15000.0, "ر.ي")
        assertEquals("15,000 ر.ي", curr)
    }
}

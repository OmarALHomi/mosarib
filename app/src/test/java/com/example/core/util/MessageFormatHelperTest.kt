package com.example.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MessageFormatHelperTest {

    private fun createMillis(year: Int, month: Int, day: Int, hour: Int = 12): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun testKeycapDigits() {
        assertEquals("1️⃣", MessageFormatHelper.keycapDigits(1))
        assertEquals("9️⃣", MessageFormatHelper.keycapDigits(9))
        assertEquals("1️⃣0️⃣", MessageFormatHelper.keycapDigits(10))
        assertEquals("1️⃣7️⃣", MessageFormatHelper.keycapDigits(17))
        assertEquals("2️⃣7️⃣", MessageFormatHelper.keycapDigits(27))
        assertEquals("3️⃣1️⃣", MessageFormatHelper.keycapDigits(31))
    }

    @Test
    fun testDateLine_specExamples() {
        // 2026-10-27 is Tuesday: "2️⃣7️⃣ الثلاثاء · 27/10/2026"
        val millis27 = createMillis(2026, 10, 27)
        assertEquals("2️⃣7️⃣ الثلاثاء · 27/10/2026", MessageFormatHelper.dateLine(millis27))

        // 2026-11-05 is Thursday (5th day of November 2026)
        val millis5 = createMillis(2026, 11, 5)
        assertEquals("5️⃣ الخميس · 05/11/2026", MessageFormatHelper.dateLine(millis5))

        // Day 5 on a Wednesday (2026-08-05): "5️⃣ الأربعاء · 05/08/2026"
        val millisWed5 = createMillis(2026, 8, 5)
        assertEquals("5️⃣ الأربعاء · 05/08/2026", MessageFormatHelper.dateLine(millisWed5))
    }

    @Test
    fun testDateLine_requiredDays() {
        // Day 1: 2026-11-01 is Sunday -> "1️⃣ الأحد · 01/11/2026"
        val m1 = createMillis(2026, 11, 1)
        assertEquals("1️⃣ الأحد · 01/11/2026", MessageFormatHelper.dateLine(m1))

        // Day 9: 2026-11-09 is Monday -> "9️⃣ الإثنين · 09/11/2026"
        val m9 = createMillis(2026, 11, 9)
        assertEquals("9️⃣ الإثنين · 09/11/2026", MessageFormatHelper.dateLine(m9))

        // Day 10: 2026-11-10 is Tuesday -> "1️⃣0️⃣ الثلاثاء · 10/11/2026"
        val m10 = createMillis(2026, 11, 10)
        assertEquals("1️⃣0️⃣ الثلاثاء · 10/11/2026", MessageFormatHelper.dateLine(m10))

        // Day 17: 2026-11-17 is Tuesday -> "1️⃣7️⃣ الثلاثاء · 17/11/2026"
        val m17 = createMillis(2026, 11, 17)
        assertEquals("1️⃣7️⃣ الثلاثاء · 17/11/2026", MessageFormatHelper.dateLine(m17))

        // Day 31: 2026-10-31 is Saturday -> "3️⃣1️⃣ السبت · 31/10/2026"
        val m31 = createMillis(2026, 10, 31)
        assertEquals("3️⃣1️⃣ السبت · 31/10/2026", MessageFormatHelper.dateLine(m31))
    }

    @Test
    fun testWeekdayNames() {
        // Saturday: 2026-10-31
        assertEquals("السبت", MessageFormatHelper.formatWeekdayArabic(createMillis(2026, 10, 31)))
        // Sunday: 2026-11-01
        assertEquals("الأحد", MessageFormatHelper.formatWeekdayArabic(createMillis(2026, 11, 1)))
        // Monday: 2026-11-02
        assertEquals("الإثنين", MessageFormatHelper.formatWeekdayArabic(createMillis(2026, 11, 2)))
        // Tuesday: 2026-11-03
        assertEquals("الثلاثاء", MessageFormatHelper.formatWeekdayArabic(createMillis(2026, 11, 3)))
        // Wednesday: 2026-11-04
        assertEquals("الأربعاء", MessageFormatHelper.formatWeekdayArabic(createMillis(2026, 11, 4)))
        // Thursday: 2026-11-05
        assertEquals("الخميس", MessageFormatHelper.formatWeekdayArabic(createMillis(2026, 11, 5)))
        // Friday: 2026-11-06
        assertEquals("الجمعة", MessageFormatHelper.formatWeekdayArabic(createMillis(2026, 11, 6)))
    }

    @Test
    fun testCustomLabel() {
        val m = createMillis(2026, 10, 27)
        assertEquals("2️⃣7️⃣ حتى: الثلاثاء · 27/10/2026", MessageFormatHelper.dateLine(m, label = "حتى"))
    }
}

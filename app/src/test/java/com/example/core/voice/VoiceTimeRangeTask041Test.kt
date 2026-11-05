package com.example.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Phrases the owner said on the phone (2026-11-05); the speech engine writes times as "10:00". */
class VoiceTimeRangeTask041Test {
    private val now = Calendar.getInstance().apply { set(2026, 10, 5, 17, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    private val names = listOf("احمد علي", "يوسف احمد")

    private fun parse(text: String) = VoiceCommandParser.parse(text, names, now)

    private fun hourOf(ms: Long?) = Calendar.getInstance().apply { timeInMillis = ms!! }.get(Calendar.HOUR_OF_DAY)
    private fun dayOf(ms: Long?) = Calendar.getInstance().apply { timeInMillis = ms!! }.get(Calendar.DAY_OF_MONTH)

    @Test
    fun noonToMidnightWithColonTimes() {
        val c = parse("مع احمد علي من الساعه 12:00 بالظهر الى الساعه 12:00 بالليل من 10,000")
        assertEquals(720, c.durationMinutes)
        assertEquals(12, hourOf(c.startMillis))
        assertEquals(0, hourOf(c.endMillis))
        assertNull(c.notes)
        assertEquals("احمد علي", c.customerQuery)
    }

    @Test
    fun nightToMorningIsTwelveHours() {
        val c = parse("مع احمد علي من الساعه 10:00 بالليل الى الساعه 10:00 صباحا من 10,000")
        assertEquals(720, c.durationMinutes)
        assertEquals(22, hourOf(c.startMillis))
    }

    @Test
    fun lateMorningNoonWordToDawn() {
        val c = parse("مع احمد علي من الساعه 10:00 ظهرا الى الساعه 2:00 فجرا من 10,000 ريال مسلم كامل")
        assertEquals(960, c.durationMinutes)
        assertTrue(c.paidFull)
        assertNull(c.notes)
    }

    @Test
    fun morningToMidnight() {
        assertEquals(960, parse("مع احمد علي من الساعه 8:00 بالصبح الى الساعه 12:00 بالليل من 10,000").durationMinutes)
    }

    @Test
    fun afternoonWordsWithColon() {
        assertEquals(150, parse("مع احمد علي من الساعه 3:30 عصرا الى الساعه 6:00 مساء").durationMinutes)
        assertEquals(240, parse("مع احمد علي من الساعه 8:00 بالصبح للساعه 12:00 الظهر").durationMinutes)
    }

    @Test
    fun dayWordBeforeFrom() {
        val c = parse("مع احمد علي امس من 5 المغرب الى 11 بالليل من 10 الف")
        assertEquals(360, c.durationMinutes)
        assertEquals(4, dayOf(c.startMillis))
        assertNull(c.notes)
    }

    @Test
    fun leadingRecordVerbIsNotPartOfTheName() {
        assertEquals("احمد علي", parse("سجل مع احمد علي ساعتين من 4000 ريال مسلم").customerQuery)
    }

    @Test
    fun notesComeFromTheEndOnly() {
        val c = parse("مع احمد علي ساعتين من 10 الف ومسلم 5000 باقي على الشهر")
        assertEquals("باقي على الشهر", c.notes)
        val noise = parse("يا اخي مع احمد علي ساعتين من 10 الف")
        assertNull(noise.notes)
    }
}

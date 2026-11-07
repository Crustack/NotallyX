package com.philkes.notallyx.data.model.extensions

import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM unit tests for context-free date and reminder formatting across multiple locales
 * (`PREP-008`).
 */
class ReminderModelExtensionsTest {

    @Test
    fun `toRepetitionText formats no-repetition, daily, weekly, monthly, and custom intervals across locales`() {
        val date =
            Calendar.getInstance()
                .apply {
                    set(2026, Calendar.MARCH, 15, 9, 30, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .time

        val noRepReminder = Reminder(id = 1L, dateTime = date, repetition = null)
        assertEquals("Does not repeat", noRepReminder.toRepetitionText(Locale.ENGLISH))
        assertEquals("Keine Wiederholung", noRepReminder.toRepetitionText(Locale.GERMAN))
        assertEquals("Aucune répétition", noRepReminder.toRepetitionText(Locale.FRENCH))

        val dailyRep = Repetition(1, RepetitionTimeUnit.DAYS)
        assertEquals("Daily", dailyRep.toText(Locale.ENGLISH))
        assertEquals("Täglich", dailyRep.toText(Locale.GERMAN))

        val every3Weeks = Repetition(3, RepetitionTimeUnit.WEEKS)
        assertEquals("Every 3 Weeks", every3Weeks.toText(Locale.ENGLISH))
        assertEquals("Alle 3 Wochen", every3Weeks.toText(Locale.GERMAN))

        val monthlyFixedDay =
            Reminder(
                id = 2L,
                dateTime = date,
                repetition = Repetition(1, RepetitionTimeUnit.MONTHS),
            )
        assertEquals("15. of the month", monthlyFixedDay.toRepetitionText(Locale.ENGLISH))
        assertEquals("15. des Monats", monthlyFixedDay.toRepetitionText(Locale.GERMAN))

        val lastDayDate =
            Calendar.getInstance()
                .apply {
                    set(2026, Calendar.MARCH, 31, 9, 30, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .time
        val monthlyLastDay =
            Reminder(
                id = 3L,
                dateTime = lastDayDate,
                repetition = Repetition(1, RepetitionTimeUnit.MONTHS),
            )
        assertEquals("Last Day of the month", monthlyLastDay.toRepetitionText(Locale.ENGLISH))
        assertEquals("Letzter Tag des Monats", monthlyLastDay.toRepetitionText(Locale.GERMAN))
    }

    @Test
    fun `toText formats nth weekday of month repetitions without Android Context`() {
        val secondMondayEvery2Months =
            Repetition(
                value = 2,
                unit = RepetitionTimeUnit.MONTHS,
                occurrence = 2,
                dayOfWeek = Calendar.MONDAY,
            )
        assertEquals(
            "Every 2 Months 2. Monday of the month",
            secondMondayEvery2Months.toText(Locale.ENGLISH),
        )

        val lastFridayMonthly =
            Repetition(
                value = 1,
                unit = RepetitionTimeUnit.MONTHS,
                occurrence = -1,
                dayOfWeek = Calendar.FRIDAY,
            )
        assertEquals(
            " Last Friday of the month",
            lastFridayMonthly.toText(Locale.ENGLISH),
        )
    }

    @Test
    fun `formatDate formats relative and ISO dates without Android Context`() {
        val now = Date()
        val relative = now.formatDate(DateFormat.RELATIVE, TimeFormat.NONE)
        assertTrue(relative.isNotBlank())

        val fixedDate =
            Calendar.getInstance()
                .apply {
                    set(2026, Calendar.NOVEMBER, 1, 14, 5, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .time
        val iso = fixedDate.formatDate(DateFormat.SHORT_ISO, TimeFormat.TWENTY_FOUR_H)
        assertEquals("2026-11-01 14:05", iso)
    }

    @Test
    fun `nextNotification and lastNotification calculate accurately on pure JVM`() {
        val start =
            Calendar.getInstance()
                .apply {
                    set(2026, Calendar.JANUARY, 1, 10, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .time
        val reminder =
            Reminder(
                id = 1L,
                dateTime = start,
                repetition = Repetition(1, RepetitionTimeUnit.DAYS),
            )
        val probe =
            Calendar.getInstance()
                .apply {
                    set(2026, Calendar.JANUARY, 3, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                .time

        val last = reminder.lastNotification(before = probe)
        val next = reminder.nextNotification(from = probe)
        assertNotNull(last)
        assertNotNull(next)
        assertEquals(3, last!!.toCalendar().get(Calendar.DAY_OF_MONTH))
        assertEquals(4, next!!.toCalendar().get(Calendar.DAY_OF_MONTH))
    }
}

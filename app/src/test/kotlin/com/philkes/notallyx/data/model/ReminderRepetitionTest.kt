package com.philkes.notallyx.data.model

import java.util.Calendar
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRepetitionTest {

    private fun createDate(year: Int, month: Int, day: Int, hour: Int = 9, minute: Int = 0): Date {
        return Calendar.getInstance()
            .apply {
                set(year, month, day, hour, minute, 0)
                set(Calendar.MILLISECOND, 0)
            }
            .time
    }

    private fun Calendar.copy(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        return calendar
    }

    @Test
    fun `nextNotification for simple Daily repetition`() {
        val start = createDate(2023, Calendar.JANUARY, 1, 10, 0)
        val reminder = Reminder(1, start, Repetition(1, RepetitionTimeUnit.DAYS))

        // From same day, before time
        val from1 = createDate(2023, Calendar.JANUARY, 1, 9, 0)
        assertEquals(start, reminder.nextNotification(from1))

        // From same day, after time
        val from2 = createDate(2023, Calendar.JANUARY, 1, 11, 0)
        assertEquals(createDate(2023, Calendar.JANUARY, 2, 10, 0), reminder.nextNotification(from2))
    }

    @Test
    fun `nextNotification for simple Monthly repetition`() {
        val start = createDate(2023, Calendar.JANUARY, 31, 10, 0)
        val reminder = Reminder(1, start, Repetition(1, RepetitionTimeUnit.MONTHS))

        // From Feb 1st
        val from = createDate(2023, Calendar.FEBRUARY, 1, 9, 0)
        // Simple monthly repetition uses Repetition.toMillis() which is not ideal for variable
        // month lengths
        // but let's see what the current implementation does.
        // Current implementation:
        // val intervalsPassed = timeDifferenceMillis / rep.toMillis()
        // val unitsUntilNext = ((rep.value) * (intervalsPassed + 1)).toInt()
        // reminderStart.add(rep.unit.toCalendarField(), unitsUntilNext)

        val expected =
            Calendar.getInstance()
                .apply {
                    time = start
                    add(Calendar.MONTH, 1)
                }
                .time
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - 1st Wednesday`() {
        // 2023-01-01 is Sunday. 1st Wed is 2023-01-04.
        val start = createDate(2023, Calendar.JANUARY, 4, 10, 0)
        val repetition =
            Repetition(1, RepetitionTimeUnit.MONTHS, occurrence = 1, dayOfWeek = Calendar.WEDNESDAY)
        val reminder = Reminder(1, start, repetition)

        // From Jan 5th, next should be 1st Wed of Feb (Feb 1st)
        val from = createDate(2023, Calendar.JANUARY, 5, 9, 0)
        val expected = createDate(2023, Calendar.FEBRUARY, 1, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - Last Friday`() {
        // Jan 2023: last Friday is Jan 27th.
        val start = createDate(2023, Calendar.JANUARY, 27, 10, 0)
        val repetition =
            Repetition(1, RepetitionTimeUnit.MONTHS, occurrence = -1, dayOfWeek = Calendar.FRIDAY)
        val reminder = Reminder(1, start, repetition)

        // From Jan 28th, next should be last Fri of Feb (Feb 24th)
        val from = createDate(2023, Calendar.JANUARY, 28, 9, 0)
        val expected = createDate(2023, Calendar.FEBRUARY, 24, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - 2nd Tuesday every 2 months`() {
        // Jan 2023: 2nd Tuesday is Jan 10th.
        val start = createDate(2023, Calendar.JANUARY, 10, 10, 0)
        val repetition =
            Repetition(2, RepetitionTimeUnit.MONTHS, occurrence = 2, dayOfWeek = Calendar.TUESDAY)
        val reminder = Reminder(1, start, repetition)

        // From Jan 11th, next should be 2nd Tue of March (March 14th)
        val from = createDate(2023, Calendar.JANUARY, 11, 9, 0)
        val expected = createDate(2023, Calendar.MARCH, 14, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - 5th Monday fallback to 4th`() {
        // May 2023 has 5 Mondays (1, 8, 15, 22, 29).
        // June 2023 has 4 Mondays (5, 12, 19, 26).

        val start = createDate(2023, Calendar.MAY, 29, 10, 0)
        // occurrence = 5, dayOfWeek = Monday
        val repetition =
            Repetition(1, RepetitionTimeUnit.MONTHS, occurrence = 5, dayOfWeek = Calendar.MONDAY)
        val reminder = Reminder(1, start, repetition)

        // From May 30th, next should be 4th Monday of June because June only has 4 Mondays
        val from = createDate(2023, Calendar.MAY, 30, 9, 0)
        val expected = createDate(2023, Calendar.JUNE, 26, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - Year transition`() {
        // Dec 2022: 1st Saturday is Dec 3rd.
        val start = createDate(2022, Calendar.DECEMBER, 3, 10, 0)
        val repetition =
            Repetition(1, RepetitionTimeUnit.MONTHS, occurrence = 1, dayOfWeek = Calendar.SATURDAY)
        val reminder = Reminder(1, start, repetition)

        // From Dec 4th, next should be Jan 7th 2023
        val from = createDate(2022, Calendar.DECEMBER, 4, 9, 0)
        val expected = createDate(2023, Calendar.JANUARY, 7, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification when from is way in the future`() {
        val start = createDate(2023, Calendar.JANUARY, 1, 10, 0)
        val repetition =
            Repetition(1, RepetitionTimeUnit.MONTHS, occurrence = 1, dayOfWeek = Calendar.MONDAY)
        val reminder = Reminder(1, start, repetition)

        // From Jan 2024
        val from = createDate(2024, Calendar.JANUARY, 1, 9, 0)
        // 1st Monday of Jan 2024 is Jan 1st.
        // Since from is Jan 1st 9:00, and reminder time is 10:00, it should be Jan 1st 2024.
        val expected = createDate(2024, Calendar.JANUARY, 1, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - 1st Sunday when 2nd is Monday`() {
        val start = createDate(2023, Calendar.OCTOBER, 2, 10, 0)
        val repetition =
            Repetition(1, RepetitionTimeUnit.MONTHS, occurrence = 1, dayOfWeek = Calendar.SUNDAY)
        val reminder = Reminder(1, start, repetition)

        // From Oct 2nd
        val from = createDate(2023, Calendar.OCTOBER, 2, 11, 0)
        val expected = createDate(2023, Calendar.NOVEMBER, 5, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - Last day of month handling`() {
        val start = createDate(2023, Calendar.MARCH, 29, 10, 0)
        val repetition = Repetition(1, RepetitionTimeUnit.MONTHS)
        val reminder = Reminder(1, start, repetition)

        // From March 30
        val from = createDate(2023, Calendar.MARCH, 30, 9, 0)
        val expected = createDate(2023, Calendar.APRIL, 29, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - 13th day of month handling`() {
        val start = createDate(2023, Calendar.MARCH, 13, 10, 0)
        val repetition = Repetition(1, RepetitionTimeUnit.MONTHS)
        val reminder = Reminder(1, start, repetition)

        // From March 30
        val from = createDate(2023, Calendar.MARCH, 15, 9, 0)
        val expected = createDate(2023, Calendar.APRIL, 13, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }

    @Test
    fun `nextNotification for Advanced Monthly - 13th day of every 2nd month handling`() {
        val start = createDate(2023, Calendar.MARCH, 13, 10, 0)
        val repetition = Repetition(2, RepetitionTimeUnit.MONTHS)
        val reminder = Reminder(1, start, repetition)

        // From March 30
        val from = createDate(2023, Calendar.MARCH, 15, 9, 0)
        val expected = createDate(2023, Calendar.MAY, 13, 10, 0)
        assertEquals(expected, reminder.nextNotification(from))
    }
}

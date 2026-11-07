package com.philkes.notallyx.data.model

import com.philkes.notallyx.data.model.extensions.findLastNotificationDate
import com.philkes.notallyx.data.model.extensions.findLastNotified
import com.philkes.notallyx.data.model.extensions.findNextNotificationDate
import com.philkes.notallyx.data.model.extensions.lastNotification
import com.philkes.notallyx.data.model.extensions.nextNotification
import com.philkes.notallyx.data.model.extensions.nextRepetition
import com.philkes.notallyx.data.model.extensions.toMillis
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTest {

    @Test
    fun testToMillis() {
        assertEquals(60 * 1000, Repetition(1, RepetitionTimeUnit.MINUTES).toMillis())
        assertEquals(60 * 60 * 1000, Repetition(1, RepetitionTimeUnit.HOURS).toMillis())
        assertEquals(24 * 60 * 60 * 1000, Repetition(1, RepetitionTimeUnit.DAYS).toMillis())
        assertEquals(7L * 24 * 60 * 60 * 1000, Repetition(1, RepetitionTimeUnit.WEEKS).toMillis())
        assertEquals(31L * 24 * 60 * 60 * 1000, Repetition(1, RepetitionTimeUnit.MONTHS).toMillis())
        assertEquals(365L * 24 * 60 * 60 * 1000, Repetition(1, RepetitionTimeUnit.YEARS).toMillis())
    }

    @Test
    fun testNextRepetition() {
        val repetitionStart = calendar(2000, 0, 1)
        val reminder = Reminder(0, repetitionStart.time, Repetition(1, RepetitionTimeUnit.YEARS))
        val from = calendar(2004, 6, 3, 3, 1, 2).time

        val actual = reminder.nextRepetition(from)!!.time

        // Expected: 01.01.2005
        val expected = repetitionStart.copy().apply { add(Calendar.YEAR, 5) }.timeInMillis
        assertEquals(expected, actual)
    }

    @Test
    fun testNextNotification() {
        val start = calendar(2023, 0, 1, 10)
        val reminderNoRep = Reminder(0, start.time, null)

        // Before start, no rep -> returns start
        assertEquals(start.time, reminderNoRep.nextNotification(calendar(2022, 11, 31, 10).time))
        // After start, no rep -> returns null
        assertEquals(null, reminderNoRep.nextNotification(calendar(2023, 0, 1, 10, 0, 1).time))

        val reminderRep = Reminder(0, start.time, Repetition(1, RepetitionTimeUnit.DAYS))
        // Before start, with rep -> returns start
        assertEquals(start.time, reminderRep.nextNotification(calendar(2022, 11, 31, 10).time))
        // Exactly at start, with rep -> returns next (1 day later)
        val expectedAfterStart = start.copy().apply { add(Calendar.DAY_OF_MONTH, 1) }.time
        assertEquals(expectedAfterStart, reminderRep.nextNotification(start.time))
        // After start, with rep -> returns next
        val fromAfterStart = start.copy().apply { add(Calendar.HOUR, 5) }.time
        assertEquals(expectedAfterStart, reminderRep.nextNotification(fromAfterStart))
    }

    @Test
    fun testLastNotification() {
        val start = calendar(2023, 0, 1, 10)
        val reminderNoRep = Reminder(0, start.time, null)

        // Before start, no rep -> returns null
        assertEquals(null, reminderNoRep.lastNotification(calendar(2022, 11, 31, 10).time))
        // Exactly at start, no rep -> returns null (per code before or == start)
        assertEquals(null, reminderNoRep.lastNotification(start.time))
        // After start, no rep -> returns start
        val afterStart = start.copy().apply { add(Calendar.SECOND, 1) }.time
        assertEquals(start.time, reminderNoRep.lastNotification(afterStart))

        val reminderRep = Reminder(0, start.time, Repetition(1, RepetitionTimeUnit.HOURS))
        // Before start, with rep -> returns null
        assertEquals(null, reminderRep.lastNotification(calendar(2023, 0, 1, 9).time))
        // After start, with rep -> returns last
        val afterStartRep = start.copy().apply { add(Calendar.MINUTE, 30) }.time
        assertEquals(start.time, reminderRep.lastNotification(afterStartRep))

        // Exactly on a spike (2 hours after start) -> should return the one BEFORE (1 hour after
        // start)
        val twoHoursAfter = start.copy().apply { add(Calendar.HOUR, 2) }.time
        val oneHourAfter = start.copy().apply { add(Calendar.HOUR, 1) }.time
        assertEquals(oneHourAfter, reminderRep.lastNotification(twoHoursAfter))

        // Multiple intervals
        val threeHoursAndHalf =
            start
                .copy()
                .apply {
                    add(Calendar.HOUR, 3)
                    add(Calendar.MINUTE, 30)
                }
                .time
        val threeHoursAfter = start.copy().apply { add(Calendar.HOUR, 3) }.time
        assertEquals(threeHoursAfter, reminderRep.lastNotification(threeHoursAndHalf))
    }

    @Test
    fun testFindNextNotificationDate() {
        val nextYear = Calendar.getInstance().get(Calendar.YEAR) + 1
        val r1 = Reminder(1, calendar(nextYear, 0, 1, 11).time, null)
        val r2 = Reminder(2, calendar(nextYear, 0, 1, 12).time, null)

        val reminders = listOf(r1, r2)
        val nextDate = reminders.findNextNotificationDate()
        assertEquals(r1.dateTime, nextDate)
    }

    @Test
    fun testFindLastNotificationDate() {
        val r1 = Reminder(1, calendar(2020, 0, 1, 10).time, null)
        val r2 = Reminder(2, calendar(2021, 0, 1, 10).time, null)

        val reminders = listOf(r1, r2)
        val lastDate = reminders.findLastNotificationDate()
        // Both are in the past, so lastNotification() for both should be their dateTime.
        // findLastNotificationDate should return the MAXIMUM of them.
        assertEquals(r2.dateTime, lastDate)
    }

    @Test
    fun testFindLastNotified() {
        val r1 = Reminder(1, calendar(2020, 0, 1, 10).time, null)
        val r2 = Reminder(2, calendar(2021, 0, 1, 10).time, null)

        val reminders = listOf(r1, r2)
        val lastNotified = reminders.findLastNotified()
        // r2 was last notified later than r1
        assertEquals(r2, lastNotified)

        // Case where before is specified
        val before = calendar(2020, 6, 1, 10).time
        val lastNotifiedBefore = reminders.findLastNotified(before)
        // Only r1 was notified before 2020-06-01
        assertEquals(r1, lastNotifiedBefore)
    }

    @Test
    fun testRepetitionsWithDifferentUnits() {
        val start = calendar(2023, 0, 1, 10)

        // MINUTES
        val reminderMinutes = Reminder(0, start.time, Repetition(30, RepetitionTimeUnit.MINUTES))
        val after35Min = start.copy().apply { add(Calendar.MINUTE, 35) }.time
        // Expected is 10:30, because 10:00 + 30m = 10:30, which is before 10:35
        assertEquals(
            start.copy().apply { add(Calendar.MINUTE, 30) }.time,
            reminderMinutes.lastNotification(after35Min),
        )
        val after65Min = start.copy().apply { add(Calendar.MINUTE, 65) }.time
        val expectedMin = start.copy().apply { add(Calendar.MINUTE, 60) }.time
        assertEquals(expectedMin, reminderMinutes.lastNotification(after65Min))

        // WEEKS
        val reminderWeeks = Reminder(0, start.time, Repetition(1, RepetitionTimeUnit.WEEKS))
        val after1Week =
            start
                .copy()
                .apply {
                    add(Calendar.WEEK_OF_YEAR, 1)
                    add(Calendar.SECOND, 1)
                }
                .time
        assertEquals(
            start.copy().apply { add(Calendar.WEEK_OF_YEAR, 1) }.time,
            reminderWeeks.lastNotification(after1Week),
        )

        // MONTHS
        val reminderMonths = Reminder(0, start.time, Repetition(1, RepetitionTimeUnit.MONTHS))
        val after1Month =
            start
                .copy()
                .apply {
                    add(Calendar.MONTH, 1)
                    add(Calendar.SECOND, 1)
                }
                .time
        assertEquals(
            start.copy().apply { add(Calendar.MONTH, 1) }.time,
            reminderMonths.lastNotification(after1Month),
        )

        // YEARS
        val reminderYears = Reminder(0, start.time, Repetition(1, RepetitionTimeUnit.YEARS))
        val after1Year =
            start
                .copy()
                .apply {
                    add(Calendar.YEAR, 1)
                    add(Calendar.SECOND, 1)
                }
                .time
        assertEquals(
            start.copy().apply { add(Calendar.YEAR, 1) }.time,
            reminderYears.lastNotification(after1Year),
        )
    }

    private fun calendar(
        year: Int,
        month: Int,
        day: Int,
        hourOfDay: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        millis: Int = 0,
    ): Calendar =
        Calendar.getInstance().apply {
            set(year, month, day, hourOfDay, minute, second)
            set(Calendar.MILLISECOND, millis)
        }

    private fun Calendar.copy(): Calendar {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        return calendar
    }
}

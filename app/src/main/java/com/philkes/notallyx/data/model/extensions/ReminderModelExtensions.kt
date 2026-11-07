package com.philkes.notallyx.data.model.extensions

import android.content.Context
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.extensions.getQuantityStringPlain
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun Reminder.toRepetitionText(context: Context): String {
    val rep = repetition ?: return context.getString(R.string.reminder_no_repetition)
    if (rep.unit == RepetitionTimeUnit.MONTHS && rep.occurrence == null && rep.value == 1) {
        val calendar = dateTime.toCalendar()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val isLastDayOfMonth = dayOfMonth == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (isLastDayOfMonth)
            context.getString(
                R.string.of_the_month_last,
                context.getQuantityStringPlain(R.plurals.days, 1),
            )
        else context.getString(R.string.of_the_month, "$dayOfMonth.")
    }
    return rep.toText(context)
}

fun Repetition.toText(context: Context): String {
    if (unit == RepetitionTimeUnit.MONTHS && occurrence != null && dayOfWeek != null) {
        val dayOfWeekStr =
            SimpleDateFormat("EEEE", Locale.getDefault())
                .format(
                    Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek!!) }.time
                )
        val monthlyOptionText =
            when (occurrence) {
                -1 -> context.getString(R.string.of_the_month_last, dayOfWeekStr)
                else -> "$occurrence. ${context.getString(R.string.of_the_month, dayOfWeekStr)}"
            }
        val prefix =
            if (value == 1) ""
            else "${context.getString(R.string.every)} $value ${context.getString(R.string.months)}"
        return "$prefix $monthlyOptionText"
    }
    return when {
        value == 1 && unit == RepetitionTimeUnit.DAYS -> context.getString(R.string.daily)
        value == 1 && unit == RepetitionTimeUnit.WEEKS -> context.getString(R.string.weekly)
        value == 1 && unit == RepetitionTimeUnit.MONTHS -> context.getString(R.string.monthly)
        value == 1 && unit == RepetitionTimeUnit.YEARS -> context.getString(R.string.yearly)
        else -> "${context.getString(R.string.every)} $value ${unit.toText(context)}"
    }
}

private fun RepetitionTimeUnit.toText(context: Context): String {
    val resId =
        when (this) {
            RepetitionTimeUnit.MINUTES -> R.string.minutes
            RepetitionTimeUnit.HOURS -> R.string.hours
            RepetitionTimeUnit.DAYS -> return context.getQuantityStringPlain(R.plurals.days, 10)
            RepetitionTimeUnit.WEEKS -> R.string.weeks
            RepetitionTimeUnit.MONTHS -> R.string.months
            RepetitionTimeUnit.YEARS -> R.string.years
        }
    return context.getString(resId)
}

fun Collection<Reminder>.copy() = map { it.copy() }

fun RepetitionTimeUnit.toCalendarField(): Int {
    return when (this) {
        RepetitionTimeUnit.MINUTES -> Calendar.MINUTE
        RepetitionTimeUnit.HOURS -> Calendar.HOUR
        RepetitionTimeUnit.DAYS -> Calendar.DAY_OF_MONTH
        RepetitionTimeUnit.WEEKS -> Calendar.WEEK_OF_YEAR
        RepetitionTimeUnit.MONTHS -> Calendar.MONTH
        RepetitionTimeUnit.YEARS -> Calendar.YEAR
    }
}

fun Reminder.lastNotification(before: Date = Date()): Date? {
    if (before.before(dateTime) || before == dateTime) {
        return null
    }
    if (repetition == null) {
        return dateTime
    }

    val calendar = dateTime.toCalendar()
    val field = repetition!!.unit.toCalendarField()
    val value = repetition!!.value

    var last = calendar.time
    // Increment until we are at or after 'before'
    while (true) {
        calendar.add(field, value)
        if (calendar.time.after(before) || calendar.time == before) {
            break
        }
        last = calendar.time
    }

    return last
}

fun Reminder.nextNotification(from: Date = Date()): Date? {
    if (from.before(dateTime)) {
        return dateTime
    }
    if (repetition == null) {
        return null
    }

    val rep = repetition!!
    if (rep.unit == RepetitionTimeUnit.MONTHS && rep.occurrence != null && rep.dayOfWeek != null) {
        val next = dateTime.toCalendar()
        val fromCal = from.toCalendar()
        while (true) {
            val targetDate =
                findOccurrenceInMonth(
                    next.get(Calendar.YEAR),
                    next.get(Calendar.MONTH),
                    rep.occurrence!!,
                    rep.dayOfWeek!!,
                    dateTime.toCalendar(),
                )
            if (targetDate.after(fromCal)) {
                return targetDate.time
            }
            next.add(Calendar.MONTH, rep.value)
        }
    }
    val calendar = dateTime.toCalendar()
    val field = repetition!!.unit.toCalendarField()
    val value = repetition!!.value

    // If from is exactly at dateTime, we want the next one
    while (true) {
        calendar.add(field, value)
        if (calendar.time.after(from)) {
            break
        }
    }

    return calendar.time
}

private fun findOccurrenceInMonth(
    year: Int,
    month: Int,
    occurrence: Int,
    dayOfWeek: Int,
    originalTime: Calendar,
): Calendar {
    val cal =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, originalTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, originalTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, originalTime.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, originalTime.get(Calendar.MILLISECOND))
        }

    if (occurrence > 0) {
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.DAY_OF_MONTH, 7 * (occurrence - 1))

        if (cal.get(Calendar.MONTH) != month) {
            return findOccurrenceInMonth(year, month, occurrence - 1, dayOfWeek, originalTime)
        }
    } else if (occurrence == -1) {
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
    }
    return cal
}

fun Reminder.nextRepetition(from: Date = Date()): Date? {
    if (repetition == null) {
        return null
    }
    return nextNotification(from)
}

private fun Reminder.hasUpcomingNotification() = !(dateTime.before(Date()) && repetition == null)

fun Repetition.toMillis(): Long {
    return Calendar.getInstance()
        .apply {
            timeInMillis = 0
            add(unit.toCalendarField(), value)
        }
        .timeInMillis
}

fun Collection<Reminder>.hasAnyUpcomingNotifications(): Boolean {
    return any { it.hasUpcomingNotification() }
}

fun Collection<Reminder>.findNextNotificationDate(): Date? {
    return mapNotNull { it.nextNotification() }.minOrNull()
}

fun Collection<Reminder>.findLastNotificationDate(before: Date = Date()): Date? {
    return mapNotNull { it.lastNotification(before) }.maxOrNull()
}

fun Collection<Reminder>.findLastNotified(before: Date = Date()): Reminder? {
    return filter { it.lastNotification(before) != null }
        .maxByOrNull { it.lastNotification(before)!! }
}

fun Date.toCalendar() = Calendar.getInstance().apply { timeInMillis = this@toCalendar.time }

fun List<Reminder>.haveAnyRepetition(): Boolean = any { it.repetition != null }

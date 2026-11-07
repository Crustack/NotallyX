package com.philkes.notallyx.data.model.extensions

import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Platform-independent localized strings for formatting [Reminder] and [Repetition] descriptions
 * without requiring `android.content.Context` (`PREP-008`).
 */
data class RepetitionStrings(
    val noRepetition: String = "Does not repeat",
    val every: String = "Every",
    val daily: String = "Daily",
    val weekly: String = "Weekly",
    val monthly: String = "Monthly",
    val yearly: String = "Yearly",
    val minutes: String = "Minutes",
    val hours: String = "Hours",
    val daysSingular: String = "Day",
    val daysPlural: String = "Days",
    val weeks: String = "Weeks",
    val months: String = "Months",
    val years: String = "Years",
    val ofTheMonthPattern: String = "%1\$s of the month",
    val ofTheMonthLastPattern: String = "Last %1\$s of the month",
) {
    fun ofTheMonth(arg: String): String = String.format(ofTheMonthPattern, arg)

    fun ofTheMonthLast(arg: String): String = String.format(ofTheMonthLastPattern, arg)

    companion object {
        fun forLocale(locale: Locale = Locale.getDefault()): RepetitionStrings {
            return when (locale.language) {
                "de" ->
                    RepetitionStrings(
                        noRepetition = "Keine Wiederholung",
                        every = "Alle",
                        daily = "Täglich",
                        weekly = "Wöchentlich",
                        monthly = "Monatlich",
                        yearly = "Jährlich",
                        minutes = "Minuten",
                        hours = "Stunden",
                        daysSingular = "Tag",
                        daysPlural = "Tage",
                        weeks = "Wochen",
                        months = "Monate",
                        years = "Jahre",
                        ofTheMonthPattern = "%1\$s des Monats",
                        ofTheMonthLastPattern = "Letzter %1\$s des Monats",
                    )
                "fr" ->
                    RepetitionStrings(
                        noRepetition = "Aucune répétition",
                        every = "Tous les",
                        daily = "Quotidien",
                        weekly = "Hebdomadaire",
                        monthly = "Mensuel",
                        yearly = "Annuel",
                        minutes = "Minutes",
                        hours = "Heures",
                        daysSingular = "Jour",
                        daysPlural = "Jours",
                        weeks = "Semaines",
                        months = "Mois",
                        years = "Années",
                        ofTheMonthPattern = "%1\$s du mois",
                        ofTheMonthLastPattern = "Dernier %1\$s du mois",
                    )
                "es" ->
                    RepetitionStrings(
                        noRepetition = "Sin repetición",
                        every = "Cada",
                        daily = "Diariamente",
                        weekly = "Semanalmente",
                        monthly = "Mensualmente",
                        yearly = "Anualmente",
                        minutes = "Minutos",
                        hours = "Horas",
                        daysSingular = "Día",
                        daysPlural = "Días",
                        weeks = "Semanas",
                        months = "Meses",
                        years = "Años",
                        ofTheMonthPattern = "%1\$s del mes",
                        ofTheMonthLastPattern = "Último %1\$s del mes",
                    )
                else -> RepetitionStrings()
            }
        }
    }
}

fun Date.formatDate(
    dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    timeFormat: TimeFormat = TimeFormat.TWENTY_FOUR_H,
    ensureFullFormat: Boolean = false,
): String {
    val (effectiveDateFormat, effectiveTimeFormat) =
        if (ensureFullFormat && dateFormat != DateFormat.RELATIVE) {
            Pair(
                dateFormat.takeIf { it != DateFormat.NONE } ?: DateFormat.DD_MM_YY_GER,
                timeFormat.takeIf { it != TimeFormat.NONE } ?: TimeFormat.TWENTY_FOUR_H,
            )
        } else Pair(dateFormat, timeFormat)
    if (effectiveDateFormat == DateFormat.NONE && effectiveTimeFormat == TimeFormat.NONE) {
        return ""
    }
    val datePart = effectiveDateFormat.format(this)
    val timePart = effectiveTimeFormat.format(this)

    return if (datePart.isNotEmpty() && timePart.isNotEmpty()) {
        "$datePart $timePart"
    } else {
        datePart + timePart
    }
}

fun Reminder.toRepetitionText(
    locale: Locale = Locale.getDefault(),
    strings: RepetitionStrings = RepetitionStrings.forLocale(locale),
): String {
    val rep = repetition ?: return strings.noRepetition
    if (rep.unit == RepetitionTimeUnit.MONTHS && rep.occurrence == null && rep.value == 1) {
        val calendar = dateTime.toCalendar()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val isLastDayOfMonth = dayOfMonth == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (isLastDayOfMonth) {
            strings.ofTheMonthLast(strings.daysSingular)
        } else {
            strings.ofTheMonth("$dayOfMonth.")
        }
    }
    return rep.toText(locale, strings)
}

fun Repetition.toText(
    locale: Locale = Locale.getDefault(),
    strings: RepetitionStrings = RepetitionStrings.forLocale(locale),
): String {
    if (unit == RepetitionTimeUnit.MONTHS && occurrence != null && dayOfWeek != null) {
        val dayOfWeekStr =
            SimpleDateFormat("EEEE", locale)
                .format(
                    Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek!!) }.time
                )
        val monthlyOptionText =
            when (occurrence) {
                -1 -> strings.ofTheMonthLast(dayOfWeekStr)
                else -> "$occurrence. ${strings.ofTheMonth(dayOfWeekStr)}"
            }
        val prefix = if (value == 1) "" else "${strings.every} $value ${strings.months}"
        return "$prefix $monthlyOptionText"
    }
    return when {
        value == 1 && unit == RepetitionTimeUnit.DAYS -> strings.daily
        value == 1 && unit == RepetitionTimeUnit.WEEKS -> strings.weekly
        value == 1 && unit == RepetitionTimeUnit.MONTHS -> strings.monthly
        value == 1 && unit == RepetitionTimeUnit.YEARS -> strings.yearly
        else -> "${strings.every} $value ${unit.toText(strings)}"
    }
}

private fun RepetitionTimeUnit.toText(strings: RepetitionStrings): String {
    return when (this) {
        RepetitionTimeUnit.MINUTES -> strings.minutes
        RepetitionTimeUnit.HOURS -> strings.hours
        RepetitionTimeUnit.DAYS -> strings.daysPlural
        RepetitionTimeUnit.WEEKS -> strings.weeks
        RepetitionTimeUnit.MONTHS -> strings.months
        RepetitionTimeUnit.YEARS -> strings.years
    }
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

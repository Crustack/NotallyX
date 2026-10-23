package com.philkes.notallyx.data.model

import java.util.Calendar
import java.util.TimeZone

/** A calendar day, independent of MaterialDatePicker's UTC representation. Month is zero-based. */
data class SearchDate(val year: Int, val month: Int, val day: Int) {

    fun toUtcMillis(): Long = calendar(TimeZone.getTimeZone("UTC")).timeInMillis

    /** Local midnight (inclusive) to the following local midnight (exclusive), including DST. */
    fun timestampRange(timeZone: TimeZone = TimeZone.getDefault()): LongRange {
        val calendar = calendar(timeZone)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return start until calendar.timeInMillis
    }

    private fun calendar(timeZone: TimeZone): Calendar =
        Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day)
        }

    companion object {
        fun fromUtcMillis(utcMillis: Long): SearchDate {
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = utcMillis
            return SearchDate(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
            )
        }
    }
}

package com.philkes.notallyx.data.model

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchDateTest {

    @Test
    fun pickerDateRoundTripsWithoutUsingDeviceTimeZone() {
        val original = TimeZone.getDefault()
        try {
            for (zone in listOf("Asia/Shanghai", "America/Los_Angeles", "Pacific/Kiritimati")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                val date = SearchDate(2026, Calendar.JANUARY, 2)
                assertEquals(1767312000000L, date.toUtcMillis())
                assertEquals(date, SearchDate.fromUtcMillis(date.toUtcMillis()))
            }
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun localDayStartsOnPreviousUtcDateInShanghai() {
        val range = SearchDate(2026, Calendar.JANUARY, 2).timestampRange(zone("Asia/Shanghai"))
        assertEquals(1767283200000L, range.first)
        assertEquals(24 * HOUR, range.last + 1 - range.first)
    }

    @Test
    fun localDayStartsLaterThanUtcInLosAngeles() {
        val range =
            SearchDate(2026, Calendar.JANUARY, 2).timestampRange(zone("America/Los_Angeles"))
        assertEquals(1767340800000L, range.first)
    }

    @Test
    fun springDstDayHas23Hours() {
        val range = SearchDate(2026, Calendar.MARCH, 8).timestampRange(zone("America/New_York"))
        assertEquals(23 * HOUR, range.last + 1 - range.first)
    }

    @Test
    fun autumnDstDayHas25Hours() {
        val range = SearchDate(2026, Calendar.NOVEMBER, 1).timestampRange(zone("America/New_York"))
        assertEquals(25 * HOUR, range.last + 1 - range.first)
    }

    @Test
    fun leapDayAndYearEndFinishAtNextCalendarDay() {
        for ((date, next) in
            listOf(
                SearchDate(2024, Calendar.FEBRUARY, 29) to SearchDate(2024, Calendar.MARCH, 1),
                SearchDate(2026, Calendar.DECEMBER, 31) to SearchDate(2027, Calendar.JANUARY, 1),
            )) {
            assertEquals(next.toUtcMillis(), date.timestampRange(zone("UTC")).last + 1)
        }
    }

    private fun zone(id: String) = TimeZone.getTimeZone(id)

    companion object {
        private const val HOUR = 60 * 60 * 1000L
    }
}

package com.philkes.notallyx.utils

import com.philkes.notallyx.data.model.SearchDate
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateJumpUtilsTest {

    private val date = SearchDate(2026, Calendar.JANUARY, 2)
    private val utc = TimeZone.getTimeZone("UTC")
    private val start = date.timestampRange(utc).first
    private val end = date.timestampRange(utc).last + 1

    @Test
    fun jumpsToFirstSameDayItemWithoutRemovingOtherDatesOrHeaders() {
        val list = listOf(null, end + HOUR, null, start + HOUR, start, start - HOUR)
        val original = list.toList()
        assertEquals(3, list.findDateJumpPosition(date, utc) { it })
        assertEquals(original, list)
    }

    @Test
    fun usesDisplayPositionsInBothAscendingAndDescendingLists() {
        val ascending = listOf(start - HOUR, start + HOUR, start + 2 * HOUR, end + HOUR)
        assertEquals(1, ascending.findDateJumpPosition(date, utc) { it })
        assertEquals(1, ascending.reversed().findDateJumpPosition(date, utc) { it })
    }

    @Test
    fun missingDayUsesClosestAvailableTimestampOnEitherSide() {
        assertEquals(0, listOf(start - 10, end + 100).findDateJumpPosition(date, utc) { it })
        assertEquals(1, listOf(start - 100, end + 10).findDateJumpPosition(date, utc) { it })
    }

    @Test
    fun equalDistanceKeepsDisplayOrder() {
        val list = listOf(end, start - 1)
        assertEquals(0, list.findDateJumpPosition(date, utc) { it })
        assertEquals(0, list.reversed().findDateJumpPosition(date, utc) { it })
    }

    @Test
    fun datesOutsideAvailableHistoryClampToClosestNote() {
        assertEquals(1, listOf(end + HOUR, end).findDateJumpPosition(date, utc) { it })
        assertEquals(0, listOf(start - 1, start - HOUR).findDateJumpPosition(date, utc) { it })
    }

    @Test
    fun emptyListsAndHeadersHaveNoJumpTarget() {
        assertNull(emptyList<Long?>().findDateJumpPosition(date, utc) { it })
        assertNull(listOf<Long?>(null, null).findDateJumpPosition(date, utc) { it })
    }

    @Test
    fun dayIncludesLocalMidnightAndLastMillisecondButNotNextMidnight() {
        assertEquals(1, listOf(end, start).findDateJumpPosition(date, utc) { it })
        assertEquals(1, listOf(end, end - 1).findDateJumpPosition(date, utc) { it })
    }

    @Test
    fun targetFollowsLocalCalendarDateRatherThanUtcDate() {
        val list = listOf(start, start + 20 * HOUR)
        assertEquals(
            0,
            list.findDateJumpPosition(date, TimeZone.getTimeZone("Asia/Shanghai")) { it },
        )
        assertEquals(
            1,
            list.findDateJumpPosition(date, TimeZone.getTimeZone("America/Los_Angeles")) { it },
        )
    }

    @Test
    fun callerCanFollowModifiedDateInsteadOfCreationDate() {
        val list = listOf(start to end, end to start)
        assertEquals(0, list.findDateJumpPosition(date, utc) { it.first })
        assertEquals(1, list.findDateJumpPosition(date, utc) { it.second })
    }

    companion object {
        private const val HOUR = 60 * 60 * 1000L
    }
}

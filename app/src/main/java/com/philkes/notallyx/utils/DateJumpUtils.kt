package com.philkes.notallyx.utils

import com.philkes.notallyx.data.model.SearchDate
import java.util.TimeZone

/**
 * Returns an index in the actual displayed list, including any headers. Null timestamps are
 * skipped. Prefer the first item on the chosen local day; otherwise use the closest timestamp to
 * that day. Equal distances keep display order. The list is never filtered or reordered.
 */
fun <T> List<T>.findDateJumpPosition(
    date: SearchDate,
    timeZone: TimeZone = TimeZone.getDefault(),
    timestampOf: (T) -> Long?,
): Int? {
    val range = date.timestampRange(timeZone)
    var closestPosition: Int? = null
    var closestDistance = Long.MAX_VALUE
    forEachIndexed { position, item ->
        val timestamp = timestampOf(item) ?: return@forEachIndexed
        if (timestamp in range) return position
        val distance =
            if (timestamp < range.first) range.first - timestamp else timestamp - range.last
        if (distance < closestDistance) {
            closestDistance = distance
            closestPosition = position
        }
    }
    return closestPosition
}

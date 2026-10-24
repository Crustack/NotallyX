package com.philkes.notallyx.feature.main.sorting

import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.extensions.findNextNotificationDate

fun BaseNote.compareNextNotification(other: BaseNote): Int {
    if (other.reminders.isEmpty() && reminders.isNotEmpty()) {
        return 1
    }
    if (other.reminders.isNotEmpty() && reminders.isEmpty()) {
        return -1
    }
    if (other.reminders.isEmpty() && reminders.isEmpty()) {
        return 0
    }
    val nextNotification = reminders.findNextNotificationDate()
    val otherNextNotification = other.reminders.findNextNotificationDate()
    if (nextNotification == null && otherNextNotification != null) {
        return -1
    }
    if (nextNotification != null && otherNextNotification == null) {
        return 1
    }
    if (nextNotification == null && otherNextNotification == null) {
        return 0
    }
    return nextNotification!!.compareTo(otherNextNotification!!)
}

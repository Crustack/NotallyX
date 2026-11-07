package com.philkes.notallyx.feature.main.sorting

import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.extensions.findLastNotificationDate

fun BaseNote.compareLastNotification(other: BaseNote): Int {
    if (other.reminders.isEmpty() && reminders.isNotEmpty()) {
        return 1
    }
    if (other.reminders.isNotEmpty() && reminders.isEmpty()) {
        return -1
    }
    if (other.reminders.isEmpty() && reminders.isEmpty()) {
        return 0
    }
    val lastNotification = reminders.findLastNotificationDate()
    val otherLastNotification = other.reminders.findLastNotificationDate()
    if (lastNotification == null && otherLastNotification != null) {
        return -1
    }
    if (lastNotification != null && otherLastNotification == null) {
        return 1
    }
    if (lastNotification == null && otherLastNotification == null) {
        return 0
    }
    return lastNotification!!.compareTo(otherLastNotification!!)
}

package com.philkes.notallyx.utils

import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.getSafeLong
import com.philkes.notallyx.data.model.getSafeString
import java.util.Date
import org.json.JSONObject

typealias NotallyReminderJson = String

/**
 * Parse Notally Reminder JSON to NotallyX Reminder
 *
 * [Notally/Reminder.kt](https://github.com/OmGodse/Notally/blob/master/app/src/main/java/com/omgodse/notally/room/Reminder.kt)
 */
fun NotallyReminderJson?.toNotallyXReminder(): Reminder? {
    if (this == null) return null
    return with(JSONObject(this)) {
        val dateTime = getSafeLong("timestamp")?.let { Date(it) } ?: return null
        val repetition =
            getSafeString("frequency")?.let {
                when (it) {
                    "DAILY" -> Repetition(1, RepetitionTimeUnit.DAYS)
                    "MONTHLY" -> Repetition(1, RepetitionTimeUnit.MONTHS)
                    else -> null
                }
            }
        Reminder(0, dateTime, repetition)
    }
}

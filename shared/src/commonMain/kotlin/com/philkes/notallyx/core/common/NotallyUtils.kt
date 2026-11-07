package com.philkes.notallyx.core.common

import com.philkes.notallyx.core.database.getSafeLong
import com.philkes.notallyx.core.database.getSafeString
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import java.util.Date
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

typealias NotallyReminderJson = String

/**
 * Parse Notally Reminder JSON to NotallyX Reminder
 *
 * [Notally/Reminder.kt](https://github.com/OmGodse/Notally/blob/master/app/src/main/java/com/omgodse/notally/room/Reminder.kt)
 */
fun NotallyReminderJson?.toNotallyXReminder(): Reminder? {
    if (this == null) return null
    return runCatching {
        val jsonObject = Json.parseToJsonElement(this).jsonObject
        val dateTime = jsonObject.getSafeLong("timestamp")?.let { Date(it) } ?: return null
        val repetition =
            jsonObject.getSafeString("frequency")?.let {
                when (it) {
                    "DAILY" -> Repetition(1, RepetitionTimeUnit.DAYS)
                    "MONTHLY" -> Repetition(1, RepetitionTimeUnit.MONTHS)
                    else -> null
                }
            }
        Reminder(0, dateTime, repetition)
    }
        .getOrNull()
}

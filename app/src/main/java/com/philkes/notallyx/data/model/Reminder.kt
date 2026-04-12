package com.philkes.notallyx.data.model

import android.os.Parcelable
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reminder(
    var id: Long,
    var dateTime: Date,
    var repetition: Repetition?,
    var isNotificationVisible: Boolean = false,
) : Parcelable

@Parcelize
data class Repetition(
    var value: Int,
    var unit: RepetitionTimeUnit,
    /**
     * If unit is MONTHS this can be set to repeat every nth occurrence of dayOfWeek in the month.
     */
    var occurrence: Int? = null,
    /**
     * If unit is MONTHS and occurence is set this can be set to repeat every nth occurrence of
     * dayOfWeek in the month.
     */
    var dayOfWeek: Int? = null,
) : Parcelable

enum class RepetitionTimeUnit {
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
}

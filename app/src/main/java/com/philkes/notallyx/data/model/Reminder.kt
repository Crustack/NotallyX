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
    var occurrence: Int? = null,
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

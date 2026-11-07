package com.philkes.notallyx.data.model

import java.util.Date
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.util.Date", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }

    override fun deserialize(decoder: Decoder): Date {
        return Date(decoder.decodeLong())
    }
}

@Serializable
data class Reminder(
    var id: Long,
    @Serializable(with = DateAsLongSerializer::class) var dateTime: Date,
    var repetition: Repetition?,
    var isNotificationVisible: Boolean = false,
)

@Serializable
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
)

@Serializable
enum class RepetitionTimeUnit {
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
}

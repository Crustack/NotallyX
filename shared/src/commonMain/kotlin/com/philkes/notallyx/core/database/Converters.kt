package com.philkes.notallyx.core.database

import androidx.room3.ColumnTypeConverter
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.SpanRepresentation
import java.util.Calendar
import java.util.Date
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class ConverterException(message: String, cause: Throwable) : RuntimeException(message, cause)

object Converters {

    @ColumnTypeConverter
    fun labelsToJson(labels: List<String>): String =
        try {
            buildJsonArray { labels.forEach { add(JsonPrimitive(it)) } }.toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Labels to JSON", e)
            )
            "[]"
        }

    @ColumnTypeConverter
    fun jsonToLabels(json: String): List<String> =
        try {
            jsonToLabels(Json.parseToJsonElement(json).jsonArray)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Labels", e)
            )
            emptyList()
        }

    fun jsonToLabels(jsonArray: JsonArray): List<String> = jsonArray.map { element ->
        val primitive = element.jsonPrimitive
        require(primitive !is JsonNull) { "Null label element" }
        primitive.content
    }

    @ColumnTypeConverter
    fun filesToJson(files: List<FileAttachment>): String =
        try {
            buildJsonArray {
                files.forEach { file ->
                    add(
                        buildJsonObject {
                            put("localName", file.localName)
                            put("originalName", file.originalName)
                            put("mimeType", file.mimeType)
                        }
                    )
                }
            }
                .toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Files attachments to JSON", e)
            )
            "[]"
        }

    @ColumnTypeConverter
    fun jsonToFiles(json: String): List<FileAttachment> =
        try {
            jsonToFiles(Json.parseToJsonElement(json).jsonArray)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Files attachments", e)
            )
            emptyList()
        }

    fun jsonToFiles(jsonArray: JsonArray): List<FileAttachment> = jsonArray.map { element ->
        val jsonObject = element.jsonObject
        val localName = getSafeLocalName(jsonObject)
        val originalName = getSafeOriginalName(jsonObject)
        val mimeType = jsonObject.getRequiredString("mimeType")
        FileAttachment(localName, originalName, mimeType)
    }

    @ColumnTypeConverter
    fun audiosToJson(audios: List<Audio>): String =
        try {
            buildJsonArray {
                audios.forEach { audio ->
                    add(
                        buildJsonObject {
                            put("name", audio.name)
                            audio.duration?.let { put("duration", it) }
                            put("timestamp", audio.timestamp)
                        }
                    )
                }
            }
                .toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Audios attachments to JSON", e)
            )
            "[]"
        }

    @ColumnTypeConverter
    fun jsonToAudios(json: String): List<Audio> =
        try {
            jsonToAudios(Json.parseToJsonElement(json).jsonArray)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Audios attachments", e)
            )
            emptyList()
        }

    fun jsonToAudios(json: JsonArray): List<Audio> = json.map { element ->
        val jsonObject = element.jsonObject
        val name = jsonObject.getRequiredString("name")
        val duration = jsonObject.getSafeLong("duration")
        val timestamp = jsonObject.getRequiredLong("timestamp")
        Audio(name, duration, timestamp)
    }

    @ColumnTypeConverter
    fun jsonToSpans(json: String): List<SpanRepresentation> =
        try {
            jsonToSpans(Json.parseToJsonElement(json).jsonArray)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Spans", e)
            )
            emptyList()
        }

    fun jsonToSpans(jsonArray: JsonArray): List<SpanRepresentation> =
        jsonArray.mapNotNull { element ->
            val jsonObject = element.jsonObject
            val bold = jsonObject.getSafeBoolean("bold")
            val link = jsonObject.getSafeBoolean("link")
            val linkData = jsonObject.getSafeString("linkData")
            val italic = jsonObject.getSafeBoolean("italic")
            val monospace = jsonObject.getSafeBoolean("monospace")
            val strikethrough = jsonObject.getSafeBoolean("strikethrough")
            try {
                val start = jsonObject.getRequiredInt("start")
                val end = jsonObject.getRequiredInt("end")
                SpanRepresentation(
                    start,
                    end,
                    bold,
                    link,
                    linkData,
                    italic,
                    monospace,
                    strikethrough,
                )
            } catch (e: Exception) {
                null
            }
        }

    @ColumnTypeConverter
    fun spansToJson(list: List<SpanRepresentation>): String =
        try {
            spansToJSONArray(list).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Spans to JSON", e)
            )
            "[]"
        }

    fun spansToJSONArray(list: List<SpanRepresentation>): JsonArray = buildJsonArray {
        list.forEach { representation ->
            add(
                buildJsonObject {
                    put("bold", representation.bold)
                    put("link", representation.link)
                    representation.linkData?.let { put("linkData", it) }
                    put("italic", representation.italic)
                    put("monospace", representation.monospace)
                    put("strikethrough", representation.strikethrough)
                    put("start", representation.start)
                    put("end", representation.end)
                }
            )
        }
    }

    @ColumnTypeConverter
    fun jsonToItems(json: String): List<ListItem> =
        try {
            jsonToItems(Json.parseToJsonElement(json).jsonArray)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to List Items", e)
            )
            emptyList()
        }

    fun jsonToItems(json: JsonArray): List<ListItem> = json.map { element ->
        val jsonObject = element.jsonObject
        val body = jsonObject.getSafeString("body") ?: ""
        val checked = jsonObject.getSafeBoolean("checked")
        val isChild = jsonObject.getSafeBoolean("isChild")
        val order = jsonObject.getSafeInt("order")
        val checkedTimestamp = jsonObject.getSafeLong("checkedTimestamp")
        ListItem(
            body,
            checked,
            isChild,
            order,
            mutableListOf(),
            checkedTimestamp = checkedTimestamp,
        )
    }

    @ColumnTypeConverter
    fun itemsToJson(list: List<ListItem>): String =
        try {
            itemsToJSONArray(list).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert List Items to JSON", e)
            )
            "[]"
        }

    fun itemsToJSONArray(list: List<ListItem>): JsonArray = buildJsonArray {
        list.forEach { item ->
            add(
                buildJsonObject {
                    put("body", item.body)
                    put("checked", item.checked)
                    put("isChild", item.isChild)
                    item.order?.let { put("order", it) }
                    item.checkedTimestamp?.let { put("checkedTimestamp", it) }
                }
            )
        }
    }

    @ColumnTypeConverter
    fun remindersToJson(reminders: List<Reminder>): String =
        try {
            remindersToJSONArray(reminders).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Reminders to JSON", e)
            )
            "[]"
        }

    fun remindersToJSONArray(reminders: List<Reminder>): JsonArray = buildJsonArray {
        reminders.forEach { reminder ->
            add(
                buildJsonObject {
                    put("id", reminder.id)
                    put("dateTime", reminder.dateTime.time)
                    reminder.repetition?.let { put("repetition", repetitionToJsonObject(it)) }
                    put("isNotificationVisible", reminder.isNotificationVisible)
                }
            )
        }
    }

    @ColumnTypeConverter
    fun jsonToReminders(json: String): List<Reminder> =
        try {
            jsonToReminders(Json.parseToJsonElement(json).jsonArray)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Reminders", e)
            )
            emptyList()
        }

    fun jsonToReminders(jsonArray: JsonArray): List<Reminder> = jsonArray.map { element ->
        val jsonObject = element.jsonObject
        val id = jsonObject.getRequiredLong("id")
        val dateTime = Date(jsonObject.getRequiredLong("dateTime"))
        val repetition =
            when (val repElement = jsonObject["repetition"]) {
                null,
                is JsonNull -> null
                is JsonObject -> jsonObjectToRepetition(repElement)
                is JsonPrimitive ->
                    if (repElement.isString && repElement.content.isNotEmpty()) {
                        jsonToRepetition(repElement.content)
                    } else {
                        null
                    }
                else -> null
            }
        val isNotificationVisible = jsonObject.getSafeBoolean("isNotificationVisible")
        Reminder(id, dateTime, repetition, isNotificationVisible)
    }

    @ColumnTypeConverter
    fun repetitionToJson(repetition: Repetition): String =
        try {
            repetitionToJsonObject(repetition).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Reminder Repetition to JSON", e)
            )
            ""
        }

    fun repetitionToJsonObject(repetition: Repetition): JsonObject = buildJsonObject {
        put("value", repetition.value)
        put("unit", repetition.unit.name)
        repetition.occurrence?.let { put("occurrence", it) }
        repetition.dayOfWeek?.let { put("dayOfWeek", it) }
    }

    @ColumnTypeConverter
    fun jsonToRepetition(json: String): Repetition =
        try {
            val jsonObject = Json.parseToJsonElement(json).jsonObject
            jsonObjectToRepetition(jsonObject)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Reminder Repetition", e)
            )
            Repetition(1, RepetitionTimeUnit.DAYS)
        }

    private fun jsonObjectToRepetition(jsonObject: JsonObject): Repetition {
        val value = jsonObject.getRequiredInt("value").coerceAtLeast(1)
        val unit = RepetitionTimeUnit.valueOf(jsonObject.getRequiredString("unit"))
        val (occurrence, dayOfWeek) = getSafeRepetitionCustomization(jsonObject)
        return Repetition(value, unit, occurrence, dayOfWeek)
    }

    private fun getSafeRepetitionCustomization(jsonObject: JsonObject): Pair<Int?, Int?> {
        val occurrence = jsonObject.getSafeInt("occurrence")
        val dayOfWeek = jsonObject.getSafeInt("dayOfWeek")
        if (
            occurrence != null &&
                dayOfWeek != null &&
                occurrence in setOf(-1, 1, 2, 3, 4) &&
                dayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY
        ) {
            return Pair(occurrence, dayOfWeek)
        }
        return Pair(null, null)
    }

    private fun getSafeLocalName(jsonObject: JsonObject): String =
        jsonObject.getSafeString("localName") ?: jsonObject.getRequiredString("name")

    private fun getSafeOriginalName(jsonObject: JsonObject): String =
        jsonObject.getSafeString("originalName")
            ?: getSafeLocalName(jsonObject).substringAfterLast("/")
}

fun JsonObject.getSafeString(name: String): String? {
    val element = this[name] ?: return null
    if (element is JsonNull) return null
    return (element as? JsonPrimitive)?.content ?: element.toString()
}

fun JsonObject.getSafeLong(name: String): Long? {
    val element = this[name] ?: return null
    if (element is JsonNull) return null
    return (element as? JsonPrimitive)?.longOrNull
}

fun JsonObject.getSafeBoolean(name: String): Boolean {
    val element = this[name] ?: return false
    if (element is JsonNull) return false
    return (element as? JsonPrimitive)?.booleanOrNull ?: false
}

fun JsonObject.getSafeInt(name: String): Int? {
    val element = this[name] ?: return null
    if (element is JsonNull) return null
    return (element as? JsonPrimitive)?.intOrNull
}

internal fun JsonObject.getRequiredString(name: String): String {
    val element =
        this[name] ?: throw IllegalArgumentException("Missing required string field '$name'")
    require(element !is JsonNull) { "Null required string field '$name'" }
    return element.jsonPrimitive.content
}

internal fun JsonObject.getRequiredLong(name: String): Long {
    val element =
        this[name] ?: throw IllegalArgumentException("Missing required long field '$name'")
    require(element !is JsonNull) { "Null required long field '$name'" }
    return element.jsonPrimitive.longOrNull
        ?: throw IllegalArgumentException("Invalid long field '$name'")
}

internal fun JsonObject.getRequiredInt(name: String): Int {
    val element = this[name] ?: throw IllegalArgumentException("Missing required int field '$name'")
    require(element !is JsonNull) { "Null required int field '$name'" }
    return element.jsonPrimitive.intOrNull
        ?: throw IllegalArgumentException("Invalid int field '$name'")
}

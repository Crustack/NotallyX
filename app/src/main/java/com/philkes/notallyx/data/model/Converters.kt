package com.philkes.notallyx.data.model

import androidx.room.TypeConverter
import java.util.Calendar
import java.util.Date
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ConverterException(message: String, cause: Throwable) : RuntimeException(message, cause)

object Converters {

    @TypeConverter
    fun labelsToJson(labels: List<String>) =
        try {
            JSONArray(labels).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Labels to JSON", e)
            )
            "[]"
        }

    @TypeConverter
    fun jsonToLabels(json: String) =
        try {
            jsonToLabels(JSONArray(json))
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Labels", e)
            )
            emptyList()
        }

    fun jsonToLabels(jsonArray: JSONArray) = jsonArray.iterable<String>().toList()

    @TypeConverter
    fun filesToJson(files: List<FileAttachment>): String {
        return try {
            val objects =
                files.map { file ->
                    val jsonObject = JSONObject()
                    jsonObject.put("localName", file.localName)
                    jsonObject.put("originalName", file.originalName)
                    jsonObject.put("mimeType", file.mimeType)
                }
            JSONArray(objects).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Files attachments to JSON", e)
            )
            "[]"
        }
    }

    @TypeConverter
    fun jsonToFiles(json: String) =
        try {
            jsonToFiles(JSONArray(json))
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Files attachments", e)
            )
            emptyList()
        }

    fun jsonToFiles(jsonArray: JSONArray): List<FileAttachment> {
        return jsonArray.iterable<JSONObject>().map { jsonObject ->
            val localName = getSafeLocalName(jsonObject)
            val originalName = getSafeOriginalName(jsonObject)
            val mimeType = jsonObject.getString("mimeType")
            FileAttachment(localName, originalName, mimeType)
        }
    }

    @TypeConverter
    fun audiosToJson(audios: List<Audio>): String {
        return try {
            val objects =
                audios.map { audio ->
                    val jsonObject = JSONObject()
                    jsonObject.put("name", audio.name)
                    jsonObject.put("duration", audio.duration)
                    jsonObject.put("timestamp", audio.timestamp)
                }
            JSONArray(objects).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Audios attachments to JSON", e)
            )
            "[]"
        }
    }

    @TypeConverter
    fun jsonToAudios(json: String) =
        try {
            jsonToAudios(JSONArray(json))
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Audios attachments", e)
            )
            emptyList()
        }

    fun jsonToAudios(json: JSONArray): List<Audio> {
        return json.iterable<JSONObject>().map { jsonObject ->
            val name = jsonObject.getString("name")
            val duration = jsonObject.getSafeLong("duration")
            val timestamp = jsonObject.getLong("timestamp")
            Audio(name, duration, timestamp)
        }
    }

    @TypeConverter
    fun jsonToSpans(json: String) =
        try {
            jsonToSpans(JSONArray(json))
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Spans", e)
            )
            emptyList()
        }

    fun jsonToSpans(jsonArray: JSONArray): List<SpanRepresentation> {
        return jsonArray
            .iterable<JSONObject>()
            .map { jsonObject ->
                val bold = jsonObject.getSafeBoolean("bold")
                val link = jsonObject.getSafeBoolean("link")
                val linkData = jsonObject.getSafeString("linkData")
                val italic = jsonObject.getSafeBoolean("italic")
                val monospace = jsonObject.getSafeBoolean("monospace")
                val strikethrough = jsonObject.getSafeBoolean("strikethrough")
                try {
                    val start = jsonObject.getInt("start")
                    val end = jsonObject.getInt("end")
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
            .filterNotNull()
    }

    @TypeConverter
    fun spansToJson(list: List<SpanRepresentation>) =
        try {
            spansToJSONArray(list).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Spans to JSON", e)
            )
            "[]"
        }

    fun spansToJSONArray(list: List<SpanRepresentation>): JSONArray {
        val objects =
            list.map { representation ->
                val jsonObject = JSONObject()
                jsonObject.put("bold", representation.bold)
                jsonObject.put("link", representation.link)
                jsonObject.put("linkData", representation.linkData)
                jsonObject.put("italic", representation.italic)
                jsonObject.put("monospace", representation.monospace)
                jsonObject.put("strikethrough", representation.strikethrough)
                jsonObject.put("start", representation.start)
                jsonObject.put("end", representation.end)
            }
        return JSONArray(objects)
    }

    @TypeConverter
    fun jsonToItems(json: String) =
        try {
            jsonToItems(JSONArray(json))
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to List Items", e)
            )
            emptyList()
        }

    fun jsonToItems(json: JSONArray): List<ListItem> {
        return json.iterable<JSONObject>().map { jsonObject ->
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
    }

    @TypeConverter
    fun itemsToJson(list: List<ListItem>) =
        try {
            itemsToJSONArray(list).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert List Items to JSON", e)
            )
            "[]"
        }

    fun itemsToJSONArray(list: List<ListItem>): JSONArray {
        val objects =
            list.map { item ->
                val jsonObject = JSONObject()
                jsonObject.put("body", item.body)
                jsonObject.put("checked", item.checked)
                jsonObject.put("isChild", item.isChild)
                jsonObject.put("order", item.order)
                jsonObject.put("checkedTimestamp", item.checkedTimestamp)
            }
        return JSONArray(objects)
    }

    @TypeConverter
    fun remindersToJson(reminders: List<Reminder>) =
        try {
            remindersToJSONArray(reminders).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Reminders to JSON", e)
            )
            "[]"
        }

    fun remindersToJSONArray(reminders: List<Reminder>): JSONArray {
        val objects =
            reminders.map { reminder ->
                JSONObject().apply {
                    put("id", reminder.id) // Store date as long timestamp
                    put("dateTime", reminder.dateTime.time) // Store date as long timestamp
                    put("repetition", reminder.repetition?.let { repetitionToJsonObject(it) })
                    put("isNotificationVisible", reminder.isNotificationVisible)
                }
            }
        return JSONArray(objects)
    }

    @TypeConverter
    fun jsonToReminders(json: String) =
        try {
            jsonToReminders(JSONArray(json))
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Reminders", e)
            )
            emptyList()
        }

    fun jsonToReminders(jsonArray: JSONArray): List<Reminder> {
        return jsonArray.iterable<JSONObject>().map { jsonObject ->
            val id = jsonObject.getLong("id")
            val dateTime = Date(jsonObject.getLong("dateTime"))
            val repetition = jsonObject.getSafeString("repetition")?.let { jsonToRepetition(it) }
            val isNotificationVisible = jsonObject.getSafeBoolean("isNotificationVisible")
            Reminder(id, dateTime, repetition, isNotificationVisible)
        }
    }

    @TypeConverter
    fun repetitionToJson(repetition: Repetition): String {
        return try {
            repetitionToJsonObject(repetition).toString()
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert Reminder Repetition to JSON", e)
            )
            ""
        }
    }

    fun repetitionToJsonObject(repetition: Repetition): JSONObject {
        val jsonObject = JSONObject()
        jsonObject.put("value", repetition.value)
        jsonObject.put("unit", repetition.unit.name) // Store the TimeUnit as a string
        repetition.occurrence?.let { jsonObject.put("occurrence", it) }
        repetition.dayOfWeek?.let { jsonObject.put("dayOfWeek", it) }
        return jsonObject
    }

    @TypeConverter
    fun jsonToRepetition(json: String): Repetition {
        return try {
            val jsonObject = JSONObject(json)
            val value = jsonObject.getInt("value").coerceAtLeast(1)
            val unit = RepetitionTimeUnit.valueOf(jsonObject.getString("unit"))
            val (occurrence, dayOfWeek) = getSafeRepetitionCustomization(jsonObject)
            Repetition(value, unit, occurrence, dayOfWeek)
        } catch (e: Exception) {
            ConverterErrorReporter.reportError(
                ConverterException("Failed to convert JSON to Reminder Repetition", e)
            )
            Repetition(1, RepetitionTimeUnit.DAYS)
        }
    }

    private fun getSafeRepetitionCustomization(jsonObject: JSONObject): Pair<Int?, Int?> {
        if (jsonObject.has("occurrence") && jsonObject.has("dayOfWeek")) {
            val occurrence = jsonObject.getInt("occurrence")
            val dayOfWeek = jsonObject.getInt("dayOfWeek")
            if (
                occurrence in setOf(-1, 1, 2, 3, 4) &&
                    dayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY
            ) {
                return Pair(occurrence, dayOfWeek)
            }
        }
        return Pair(null, null)
    }

    private fun getSafeLocalName(jsonObject: JSONObject): String {
        return try {
            jsonObject.getString("localName")
        } catch (e: JSONException) {
            jsonObject.getString("name")
        }
    }

    private fun getSafeOriginalName(jsonObject: JSONObject): String {
        return try {
            jsonObject.getString("originalName")
        } catch (e: JSONException) {
            getSafeLocalName(jsonObject).substringAfterLast("/")
        }
    }

    private fun JSONObject.getSafeBoolean(name: String): Boolean {
        return try {
            getBoolean(name)
        } catch (exception: JSONException) {
            false
        }
    }

    private fun JSONObject.getSafeInt(name: String): Int? {
        return try {
            getInt(name)
        } catch (exception: JSONException) {
            null
        }
    }

    private fun <T> JSONArray.iterable() = Iterable {
        object : Iterator<T> {
            var index = 0

            override fun next(): T {
                val element = get(index)
                index++
                return element as T
            }

            override fun hasNext(): Boolean {
                return index < length()
            }
        }
    }
}

fun JSONObject.getSafeString(name: String): String? {
    return try {
        getString(name)
    } catch (exception: JSONException) {
        null
    }
}

fun JSONObject.getSafeLong(name: String): Long? {
    return try {
        getLong(name)
    } catch (exception: JSONException) {
        null
    }
}

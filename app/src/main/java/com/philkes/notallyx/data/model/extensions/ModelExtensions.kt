package com.philkes.notallyx.data.model.extensions

import com.philkes.notallyx.core.common.extensions.decodeImageDimensions
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.ui.extensions.applySpans
import com.philkes.notallyx.core.ui.extensions.toHtml
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.BaseNote.Companion.COLOR_DEFAULT
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.isValidColorString
import com.philkes.notallyx.feature.migration.imports.utils.createMarkdownFromBodyAndSpans
import java.io.File
import java.text.DateFormat
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import org.jsoup.nodes.Entities

private const val NOTE_URL_PREFIX = "note://"
private val NOTE_URL_POSTFIX_NOTE = "/${Type.NOTE.name}"
private val NOTE_URL_POSTFIX_LIST = "/${Type.LIST.name}"

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

fun CharSequence?.isNoteUrl(): Boolean {
    return this?.let { startsWith(NOTE_URL_PREFIX) } ?: false
}

fun Long.createNoteUrl(type: Type): String {
    val postfix =
        when (type) {
            Type.LIST -> NOTE_URL_POSTFIX_LIST
            Type.NOTE -> NOTE_URL_POSTFIX_NOTE
        }
    return "$NOTE_URL_PREFIX$this$postfix"
}

fun String.getNoteIdFromUrl(): Long {
    return substringAfter(NOTE_URL_PREFIX).substringBefore("/").toLong()
}

fun String.getNoteTypeFromUrl(): Type {
    return Type.valueOf(substringAfterLast("/"))
}

val FileAttachment.isImage: Boolean
    get() {
        return mimeType.isImageMimeType
    }

internal val String.isImageMimeType: Boolean
    get() {
        return startsWith("image/")
    }

fun BaseNote.toTxt(includeTitle: Boolean = true, includeCreationDate: Boolean = true) =
    buildString {
        val date = DateFormat.getDateInstance(DateFormat.FULL).format(timestamp)
        val body =
            when (type) {
                Type.NOTE -> body
                Type.LIST -> items.toText()
            }

        if (title.isNotEmpty() && includeTitle) {
            append("${title}\n\n")
        }
        if (includeCreationDate) {
            append("$date\n\n")
        }
        append(body)
        return toString()
    }

fun BaseNote.toJson(): String {
    val jsonObject = buildJsonObject {
        put("type", type.name)
        put("color", color)
        put("title", title)
        put("pinned", pinned)
        put("timestamp", timestamp)
        put("modifiedTimestamp", modifiedTimestamp)
        put("labels", buildJsonArray { labels.forEach { add(JsonPrimitive(it)) } })

        when (type) {
            Type.NOTE -> {
                put("body", body)
                put("spans", Converters.spansToJSONArray(spans))
            }

            Type.LIST -> {
                put("items", Converters.itemsToJSONArray(items))
            }
        }
        put("reminders", Converters.remindersToJSONArray(reminders))
        put("viewMode", viewMode.name)
    }
    return prettyJson.encodeToString(JsonObject.serializer(), jsonObject)
}

fun String.toBaseNote(): BaseNote {
    val jsonObject = Json.parseToJsonElement(this).jsonObject
    val id = jsonObject.getLongOrDefault("id", -1L)
    val type = Type.valueOfOrDefault(jsonObject.getStringOrDefault("type", Type.NOTE.name))
    val folder = Folder.valueOfOrDefault(jsonObject.getStringOrDefault("folder", Folder.NOTES.name))
    val color =
        jsonObject.getStringOrDefault("color", COLOR_DEFAULT).takeIf { it.isValid() }
            ?: COLOR_DEFAULT
    val title = jsonObject.getStringOrDefault("title", "")
    val pinned = jsonObject.getBooleanOrDefault("pinned", false)
    val pinnedToStatusBar = jsonObject.getBooleanOrDefault("isPinnedToStatus", false)
    val timestamp = jsonObject.getLongOrDefault("timestamp", System.currentTimeMillis())
    val modifiedTimestamp = jsonObject.getLongOrDefault("modifiedTimestamp", timestamp)
    val labels = Converters.jsonToLabels(jsonObject.getArrayOrEmpty("labels"))
    val body = jsonObject.getStringOrDefault("body", "")
    val spans = Converters.jsonToSpans(jsonObject.getArrayOrEmpty("spans"))
    val items = Converters.jsonToItems(jsonObject.getArrayOrEmpty("items"))
    val images = Converters.jsonToFiles(jsonObject.getArrayOrEmpty("images"))
    val files = Converters.jsonToFiles(jsonObject.getArrayOrEmpty("files"))
    val audios = Converters.jsonToAudios(jsonObject.getArrayOrEmpty("audios"))
    val reminders = Converters.jsonToReminders(jsonObject.getArrayOrEmpty("reminders"))
    val viewMode = NoteViewMode.valueOfOrDefault(jsonObject.getStringOrDefault("viewMode", ""))
    return BaseNote(
        id,
        type,
        folder,
        color,
        title,
        pinned,
        timestamp,
        modifiedTimestamp,
        labels,
        body,
        spans,
        items,
        images,
        files,
        audios,
        reminders,
        viewMode,
        pinnedToStatusBar,
    )
}

private fun JsonObject.getStringOrDefault(key: String, defaultValue: String): String {
    val element = this[key] ?: return defaultValue
    if (element is JsonNull) return defaultValue
    return (element as? JsonPrimitive)?.content ?: defaultValue
}

private fun JsonObject.getArrayOrEmpty(key: String): JsonArray {
    val element = this[key] ?: return buildJsonArray {}
    return element as? JsonArray ?: buildJsonArray {}
}

private fun JsonObject.getBooleanOrDefault(key: String, defaultValue: Boolean): Boolean {
    val element = this[key] ?: return defaultValue
    if (element is JsonNull) return defaultValue
    return (element as? JsonPrimitive)?.booleanOrNull ?: defaultValue
}

private fun JsonObject.getLongOrDefault(key: String, defaultValue: Long): Long {
    val element = this[key] ?: return defaultValue
    if (element is JsonNull) return defaultValue
    return (element as? JsonPrimitive)?.longOrNull ?: defaultValue
}

@OptIn(ExperimentalEncodingApi::class)
fun BaseNote.toHtml(showDateCreated: Boolean, imagesRootFolder: File?) = buildString {
    val date = DateFormat.getDateInstance(DateFormat.FULL).format(timestamp)
    val title = Entities.escape(title)

    append("<!DOCTYPE html>")
    append("<html><head>")
    append("<meta charset=\"UTF-8\"><title>$title</title>")
    append(
        """
        <style>
            @page {
                margin: 1in;
            }

            body {
                font-family: sans-serif;
                line-height: 1.5;
                word-wrap: break-word;
            }

            img {
                max-width: 100%;
                height: auto;
                margin-top: 12px;
                margin-bottom: 12px;
            }

            h2, h3 {
                margin-bottom: 12px;
            }

            p {
                margin-bottom: 10px;
            }
        </style>
        """
            .trimIndent()
    )
    append("</head><body>")
    append("<h2>$title</h2>")

    if (showDateCreated) {
        append("<p>$date</p>")
    }

    when (type) {
        Type.NOTE -> {
            val body = body.applySpans(spans).toHtml()
            append(body)
        }

        Type.LIST -> {
            append("<ol style=\"list-style: none; padding: 0;\">")
            items.forEach { item ->
                val body = Entities.escape(item.body)
                val checked = if (item.checked) "checked" else ""
                val child = if (item.isChild) "style=\"margin-left: 20px\"" else ""
                append("<li><input type=\"checkbox\" $child $checked>$body</li>")
            }
            append("</ol>")
        }
    }
    if (images.isNotEmpty()) {
        append("<h3>Attached Images</h3>")
        images.forEach { image ->
            val file =
                if (imagesRootFolder != null) File(imagesRootFolder, image.localName) else null
            file?.readBytes()?.let { bytes ->
                val base64String = Base64.encode(bytes)
                val dimensions = file.decodeImageDimensions()
                val sizeAttributes =
                    if (dimensions != null) {
                        " width=\"${dimensions.first}\" height=\"${dimensions.second}\""
                    } else {
                        ""
                    }
                append(
                    "<img src=\"data:image/jpeg;base64,$base64String\" alt=\"${image.originalName}\"$sizeAttributes />"
                )
            }
        }
    }
    append("</body></html>")
}

fun BaseNote.toMarkdown(): String = buildString {
    when (type) {
        Type.NOTE -> {
            append(createMarkdownFromBodyAndSpans(body, spans))
        }
        Type.LIST -> {
            append(items.toMarkdownChecklist())
        }
    }
}

private fun List<ListItem>.toMarkdownChecklist(): String = buildString {
    for (item in this@toMarkdownChecklist) {
        val check = if (item.checked) "[x]" else "[ ]"
        val indent = if (item.isChild) "    " else ""
        appendLine("$indent- $check ${item.body}")
    }
}

fun BaseNote.attachmentsDifferFrom(other: BaseNote): Boolean {
    return files.size != other.files.size ||
        files.any { file -> other.files.none { it.localName == file.localName } } ||
        other.files.any { file -> files.none { it.localName == file.localName } } ||
        images.any { image -> other.images.none { it.localName == image.localName } } ||
        other.images.any { image -> images.none { it.localName == image.localName } } ||
        audios.any { audio -> other.audios.none { it.name == audio.name } } ||
        other.audios.any { audio -> audios.none { it.name == audio.name } }
}

fun List<ListItem>.toText() = buildString {
    for (item in this@toText) {
        val check = if (item.checked) "[✓]" else "[ ]"
        val childIndentation = if (item.isChild) "    " else ""
        appendLine("$childIndentation$check ${item.body}")
    }
}

fun Collection<ListItem>.deepCopy() = map { it.copy(children = mutableListOf()) }

private fun ColorString.isValid() =
    when (this) {
        COLOR_DEFAULT -> true
        else -> isValidColorString()
    }

data class SharedNote(
    val title: String,
    val text: String,
    val images: List<FileAttachment>,
    val files: List<FileAttachment>,
)

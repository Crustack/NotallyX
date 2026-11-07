package com.philkes.notallyx.data.model.extensions

import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.util.Base64
import androidx.core.content.IntentCompat
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.decodeToBitmap
import com.philkes.notallyx.core.common.extensions.getFileName
import com.philkes.notallyx.core.common.extensions.getMimeType
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.ui.extensions.applySpans
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.extensions.toHtml
import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.BaseNote.Companion.COLOR_DEFAULT
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.migration.imports.utils.createMarkdownFromBodyAndSpans
import java.io.File
import java.text.DateFormat
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val NOTE_URL_PREFIX = "note://"
private val NOTE_URL_POSTFIX_NOTE = "/${Type.NOTE.name}"
private val NOTE_URL_POSTFIX_LIST = "/${Type.LIST.name}"

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
private val String.isImageMimeType: Boolean
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
    val jsonObject =
        JSONObject()
            .put("type", type.name)
            .put("color", color)
            .put("title", title)
            .put("pinned", pinned)
            .put("timestamp", timestamp)
            .put("modifiedTimestamp", modifiedTimestamp)
            .put("labels", JSONArray(labels))

    when (type) {
        Type.NOTE -> {
            jsonObject.put("body", body)
            jsonObject.put("spans", Converters.spansToJSONArray(spans))
        }

        Type.LIST -> {
            jsonObject.put("items", Converters.itemsToJSONArray(items))
        }
    }
    jsonObject.put("reminders", Converters.remindersToJSONArray(reminders))
    jsonObject.put("viewMode", viewMode.name)
    return jsonObject.toString(2)
}

fun String.toBaseNote(): BaseNote {
    val jsonObject = JSONObject(this)
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

private fun JSONObject.getStringOrDefault(key: String, defaultValue: String): String {
    return try {
        getString(key)
    } catch (_: JSONException) {
        defaultValue
    }
}

private fun JSONObject.getArrayOrEmpty(key: String): JSONArray {
    return try {
        getJSONArray(key)
    } catch (exception: JSONException) {
        JSONArray("[]")
    }
}

private fun JSONObject.getBooleanOrDefault(key: String, defaultValue: Boolean): Boolean {
    return try {
        getBoolean(key)
    } catch (exception: JSONException) {
        defaultValue
    }
}

private fun JSONObject.getLongOrDefault(key: String, defaultValue: Long): Long {
    return try {
        getLong(key)
    } catch (exception: JSONException) {
        defaultValue
    }
}

fun BaseNote.toHtml(showDateCreated: Boolean, imagesRootFolder: File?) = buildString {
    val date = DateFormat.getDateInstance(DateFormat.FULL).format(timestamp)
    val title = Html.escapeHtml(title)

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
                val body = Html.escapeHtml(item.body)
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
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val options = file.decodeToBitmap()
                val width = options?.width
                val height = options?.height
                append(
                    "<img src=\"data:image/jpeg;base64,$base64String\" alt=\"${image.originalName}\"${options?.let { " width=\"$width\" height=\"$height\"" } ?: ""} />"
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
        else ->
            try {
                android.graphics.Color.parseColor(this)
                true
            } catch (_: Exception) {
                false
            }
    }

data class SharedNote(
    val title: String,
    val text: String,
    val images: List<FileAttachment>,
    val files: List<FileAttachment>,
)

fun Intent.generateBaseNote(context: ContextWrapper): SharedNote {
    val title = getStringExtra(Intent.EXTRA_SUBJECT) ?: data?.let { context.getFileName(it) } ?: ""
    val intentFiles =
        IntentCompat.getParcelableArrayListExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
            ?: IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)?.let {
                listOf(it)
            }
    val text =
        (data?.let { uri ->
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    try {
                        inputStream.bufferedReader().readText()
                    } catch (e: Exception) {
                        context.log(
                            tag = "",
                            msg = "Reading text contents from intent failed",
                            throwable = e,
                        )
                        null
                    }
                }
                    ?: run {
                        context.showToast(R.string.cant_load_file)
                        null
                    }
            } ?: getStringExtra(Intent.EXTRA_TEXT) ?: "")
            .let {
                if (it.length > MAX_BODY_CHAR_LENGTH) {
                    context.showToast(
                        context.getString(
                            R.string.note_text_too_long_truncated,
                            MAX_BODY_CHAR_LENGTH,
                        )
                    )
                }
                it.take(MAX_BODY_CHAR_LENGTH)
            }
    val (images, files) =
        intentFiles?.let {
            val filesByType = it.groupBy { uri ->
                context.getMimeType(uri)?.let { mimeType ->
                    if (mimeType.isImageMimeType) {
                        NoteModel.FileType.IMAGE
                    } else {
                        NoteModel.FileType.ANY
                    }
                } ?: NoteModel.FileType.ANY
            }
            val images =
                filesByType[NoteModel.FileType.IMAGE]?.let { images ->
                    images.map { FileAttachment("", it.toString(), "") }
                } ?: listOf()
            val files =
                filesByType[NoteModel.FileType.ANY]?.let { otherFiles ->
                    otherFiles.map { FileAttachment("", it.toString(), "") }
                } ?: listOf()
            Pair(images, files)
        } ?: Pair(listOf(), listOf())
    return SharedNote(title, text, images, files = files)
}

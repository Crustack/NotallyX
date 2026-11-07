package com.philkes.notallyx.feature.migration.imports.evernote

import com.philkes.notallyx.core.common.extensions.startsWithAnyOf
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.migration.imports.evernote.EvernoteImporter.Companion.parseTimestamp
import com.philkes.notallyx.feature.migration.imports.utils.parseBodyAndSpansFromHtml
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.convert.AnnotationStrategy
import org.simpleframework.xml.core.Persister

open class EvernoteImporter {
    protected val serializer: Serializer = Persister(AnnotationStrategy())

    fun parseExport(inputStream: InputStream): EvernoteExport? =
        try {
            serializer.read(EvernoteExport::class.java, inputStream)
        } catch (e: Exception) {
            null
        }

    companion object {
        private const val EVERNOTE_DATE_FORMAT = "yyyyMMdd'T'HHmmss'Z'"
        private const val EVERNOTE_NOTE_XML_TAG = "en-note"

        fun parseTimestamp(timestamp: String): Long {
            val format = SimpleDateFormat(EVERNOTE_DATE_FORMAT, Locale.getDefault())
            format.timeZone = TimeZone.getTimeZone("UTC")
            return try {
                val date: Date = format.parse(timestamp) ?: Date()
                date.time
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}

private const val EVERNOTE_NOTE_XML_TAG = "en-note"

fun EvernoteNote.mapToBaseNote(): BaseNote {
    val (body, spans) =
        parseBodyAndSpansFromHtml(
            content,
            EVERNOTE_NOTE_XML_TAG,
            useInnermostRootTag = true,
            brTagsAsNewLine = false,
        )
    val images = resources.filterByMimeTypePrefix("image").toFileAttachments()
    val files = resources.filterByExcludedMimeTypePrefixes("image", "audio").toFileAttachments()
    val audios =
        resources.filterByMimeTypePrefix("audio").map {
            Audio(it.attributes!!.fileName, -1, System.currentTimeMillis())
        }

    return BaseNote(
        0L,
        type = if (tasks.isEmpty()) Type.NOTE else Type.LIST,
        folder = Folder.NOTES,
        color = BaseNote.COLOR_DEFAULT,
        title = title,
        pinned = false,
        timestamp = parseTimestamp(created),
        modifiedTimestamp = parseTimestamp(updated),
        labels = tag.map { it.name },
        body = body,
        spans = spans,
        items = tasks.mapToListItem(),
        images = images,
        files = files,
        audios = audios,
        reminders = mutableListOf(),
        viewMode = NoteViewMode.EDIT,
        isPinnedToStatus = false,
    )
}

fun Collection<EvernoteResource>.filterByMimeTypePrefix(
    mimeTypePrefix: String
): List<EvernoteResource> {
    return filter { it.mime.startsWith(mimeTypePrefix) }
}

fun Collection<EvernoteResource>.filterByExcludedMimeTypePrefixes(
    vararg mimeTypePrefix: String
): List<EvernoteResource> {
    return filter { !it.mime.startsWithAnyOf(*mimeTypePrefix) }
}

fun Collection<EvernoteResource>.toFileAttachments(): List<FileAttachment> {
    return map { FileAttachment(it.attributes!!.fileName, it.attributes.fileName, it.mime) }
}

fun Collection<EvernoteTask>.mapToListItem(): List<ListItem> {
    return sortedBy { it.sortWeight }
        .mapIndexed { index, evernoteTask ->
            ListItem(
                body = evernoteTask.title,
                checked = evernoteTask.taskStatus == TaskStatus.COMPLETED,
                isChild = false,
                order = index,
                children = mutableListOf(),
            )
        }
}

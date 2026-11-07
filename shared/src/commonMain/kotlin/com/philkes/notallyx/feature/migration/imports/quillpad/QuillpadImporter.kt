package com.philkes.notallyx.feature.migration.imports.quillpad

import com.philkes.notallyx.core.common.extensions.toMillis
import com.philkes.notallyx.core.platform.MimeTypeUtils
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.migration.imports.utils.parseBodyAndSpansFromMarkdown
import java.util.Date
import kotlinx.serialization.json.Json

open class QuillpadImporter {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    fun QuillpadNote.toBaseNote(notebookMap: Map<Long, String>): BaseNote {
        val (body, spans) =
            if (!isList && content != null) {
                parseBodyAndSpansFromMarkdown(content)
            } else {
                Pair("", emptyList())
            }

        val items =
            taskList?.mapIndexed { index, task ->
                ListItem(
                    body = task.content,
                    checked = task.isDone,
                    isChild = false,
                    order = index,
                    children = mutableListOf(),
                )
            } ?: emptyList()

        val images = mutableListOf<FileAttachment>()
        val files = mutableListOf<FileAttachment>()
        val audios = mutableListOf<Audio>()

        attachments?.forEach { attachment ->
            when (attachment.type) {
                "AUDIO" ->
                    audios.add(
                        Audio(
                            name = attachment.fileName,
                            duration = null,
                            timestamp = modifiedDate.toMillis(),
                        )
                    )
                else -> {
                    val mimetype = MimeTypeUtils.getMimeTypeFromFileName(attachment.fileName)
                    if (mimetype?.startsWith("image/") == true) {
                        images.add(
                            FileAttachment(
                                localName = attachment.fileName,
                                originalName = attachment.description ?: attachment.fileName,
                                mimeType = mimetype,
                            )
                        )
                    } else {
                        files.add(
                            FileAttachment(
                                localName = attachment.fileName,
                                originalName = attachment.description ?: attachment.fileName,
                                mimeType = mimetype ?: "application/octet-stream",
                            )
                        )
                    }
                }
            }
        }

        val labels = mutableSetOf<String>()
        notebookId?.let { id -> notebookMap[id]?.let { labels.add(it) } }
        tags?.forEach { labels.add(it.name) }

        val reminders =
            this.reminders.map { Reminder(id = it.id, dateTime = Date(it.date), repetition = null) }

        return BaseNote(
            id = 0L,
            type = if (isList) Type.LIST else Type.NOTE,
            folder =
                when {
                    isDeleted -> Folder.DELETED
                    isArchived -> Folder.ARCHIVED
                    else -> Folder.NOTES
                },
            color = BaseNote.COLOR_DEFAULT,
            title = title ?: "",
            pinned = isPinned,
            timestamp = creationDate.toMillis(),
            modifiedTimestamp = modifiedDate.toMillis(),
            labels = labels.sorted().toList(),
            body = body,
            spans = spans,
            items = items,
            images = images,
            files = files,
            audios = audios,
            reminders = reminders,
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
    }
}

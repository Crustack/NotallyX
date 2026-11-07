package com.philkes.notallyx.feature.migration.imports.google

import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.migration.imports.utils.parseBodyAndSpansFromHtml
import kotlinx.serialization.json.Json

open class GoogleKeepImporter {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    fun String.parseToBaseNote(relativePath: String? = null): BaseNote {
        val googleKeepNote = json.decodeFromString<GoogleKeepNote>(this)
        val (body, spans) =
            parseBodyAndSpansFromHtml(
                googleKeepNote.textContentHtml,
                paragraphsAsNewLine = true,
            )

        val images =
            googleKeepNote.attachments
                .filter { it.mimetype.startsWith("image") }
                .map { attachment ->
                    FileAttachment(
                        "${relativePath?.let { "$it/" } ?: ""}${attachment.filePath}",
                        attachment.filePath,
                        attachment.mimetype,
                    )
                }
        val files =
            googleKeepNote.attachments
                .filter { !it.mimetype.startsWith("audio") && !it.mimetype.startsWith("image") }
                .map { attachment ->
                    FileAttachment(
                        "${relativePath?.let { "$it/" } ?: ""}${attachment.filePath}",
                        attachment.filePath,
                        attachment.mimetype,
                    )
                }
        val audios =
            googleKeepNote.attachments
                .filter { it.mimetype.startsWith("audio") }
                .map { attachment ->
                    Audio(
                        "${relativePath?.let { "$it/" } ?: ""}${attachment.filePath}",
                        0L,
                        System.currentTimeMillis(),
                    )
                }
        val items =
            googleKeepNote.listContent.mapIndexed { index, item ->
                ListItem(
                    body = item.text,
                    checked = item.isChecked,
                    isChild = false,
                    order = index,
                    children = mutableListOf(),
                )
            }

        return BaseNote(
            id = 0L,
            type = if (googleKeepNote.listContent.isNotEmpty()) Type.LIST else Type.NOTE,
            folder =
                when {
                    googleKeepNote.isTrashed -> Folder.DELETED
                    googleKeepNote.isArchived -> Folder.ARCHIVED
                    else -> Folder.NOTES
                },
            color = BaseNote.COLOR_DEFAULT,
            title = googleKeepNote.title,
            pinned = googleKeepNote.isPinned,
            timestamp = googleKeepNote.createdTimestampUsec / 1000,
            modifiedTimestamp = googleKeepNote.userEditedTimestampUsec / 1000,
            labels = googleKeepNote.labels.map { it.name },
            body = body,
            spans = spans,
            items = items,
            images = images,
            files = files,
            audios = audios,
            reminders = mutableListOf(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
    }
}

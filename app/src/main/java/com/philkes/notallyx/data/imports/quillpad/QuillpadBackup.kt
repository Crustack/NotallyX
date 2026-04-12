package com.philkes.notallyx.data.imports.quillpad

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
data class QuillpadBackup(
    val notes: List<QuillpadNote> = emptyList(),
    val notebooks: List<QuillpadNotebook> = emptyList(),
    val tags: List<QuillpadTag> = emptyList(),
    val reminders: List<QuillpadReminder> = emptyList(),
    val joins: List<QuillpadJoin> = emptyList(),
)

@InternalSerializationApi
@Serializable
data class QuillpadNote(
    val id: Long,
    val title: String? = null,
    val content: String? = null,
    val isList: Boolean = false,
    val taskList: List<QuillpadTask>? = null,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val creationDate: Long,
    val modifiedDate: Long,
    val notebookId: Long? = null,
    val tags: List<QuillpadTag>? = null,
    val attachments: List<QuillpadAttachment>? = null,
    val reminders: List<QuillpadReminder> = emptyList(),
)

@InternalSerializationApi
@Serializable
data class QuillpadTask(val id: Int, val content: String, val isDone: Boolean)

@InternalSerializationApi @Serializable data class QuillpadNotebook(val id: Long, val name: String)

@InternalSerializationApi @Serializable data class QuillpadTag(val id: Long, val name: String)

@InternalSerializationApi
@Serializable
data class QuillpadReminder(
    val id: Long,
    val noteId: Long,
    val date: Long,
    val name: String? = null,
)

@InternalSerializationApi @Serializable data class QuillpadJoin(val tagId: Long, val noteId: Long)

@InternalSerializationApi
@Serializable
data class QuillpadAttachment(
    val type: String? = null,
    val description: String? = null,
    val fileName: String,
)

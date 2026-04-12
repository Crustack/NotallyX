package com.philkes.notallyx.data.imports.google

import com.philkes.notallyx.data.model.BaseNote
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@InternalSerializationApi
@Serializable
data class GoogleKeepNote(
    val attachments: List<GoogleKeepAttachment> = listOf(),
    val color: String = BaseNote.COLOR_DEFAULT,
    val isTrashed: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val textContent: String = "",
    val textContentHtml: String = "",
    val title: String = "",
    val labels: List<GoogleKeepLabel> = listOf(),
    val userEditedTimestampUsec: Long = System.currentTimeMillis(),
    val createdTimestampUsec: Long = System.currentTimeMillis(),
    val listContent: List<GoogleKeepListItem> = listOf(),
)

@InternalSerializationApi @Serializable data class GoogleKeepLabel(val name: String)

@InternalSerializationApi
@Serializable
data class GoogleKeepAttachment(val filePath: String, val mimetype: String)

@InternalSerializationApi
@Serializable
data class GoogleKeepListItem(val text: String, val isChecked: Boolean)

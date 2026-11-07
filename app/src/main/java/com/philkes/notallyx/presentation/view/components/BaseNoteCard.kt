package com.philkes.notallyx.presentation.view.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.note.listitem.init
import com.philkes.notallyx.presentation.viewmodel.preference.DateFormat
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSortBy
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import com.philkes.notallyx.presentation.viewmodel.preference.TimeFormat
import com.philkes.notallyx.presentation.viewmodel.preference.displayBodySize
import com.philkes.notallyx.presentation.viewmodel.preference.displaySmallerSize
import com.philkes.notallyx.presentation.viewmodel.preference.displayTitleSize
import java.io.File
import java.util.Date

/** Composable replacing recycler_base_note.xml layout. Exactly 1 BaseNoteCard composable. */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun BaseNoteCard(
    baseNote: BaseNote,
    modifier: Modifier = Modifier,
    imageRoot: File? = null,
    isSelected: Boolean = false,
    dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    timeFormat: TimeFormat = TimeFormat.NONE,
    sortBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    textSize: Float = 16f,
    maxTitleLines: Int = 2,
    maxLines: Int = 10,
    maxItems: Int = 5,
    hideLabels: Boolean = false,
    hideImages: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onReminderClick: (() -> Unit)? = null,
) {
    val backgroundColor = parseNoteColor(baseNote.color)
    val isDefaultColor = baseNote.color == BaseNote.COLOR_DEFAULT
    val stroke =
        if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.outline)
        } else if (isDefaultColor) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        } else null

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = stroke,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            // Image Preview
            if (baseNote.images.isNotEmpty() && !hideImages) {
                val imageFile =
                    if (imageRoot != null) File(imageRoot, baseNote.images[0].localName) else null
                val bitmap =
                    remember(imageFile) {
                        if (imageFile?.exists() == true) {
                            try {
                                BitmapFactory.decodeFile(imageFile.absolutePath)?.asImageBitmap()
                            } catch (_: Exception) {
                                null
                            }
                        } else null
                    }
                if (bitmap != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).padding(bottom = 8.dp)) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                        )
                        if (baseNote.images.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            ) {
                                Text(
                                    text = baseNote.images.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.cant_load_image),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Title
            val titleText =
                if (baseNote.title.isNotEmpty()) {
                    baseNote.title
                } else if (baseNote.hasNoContents() && baseNote.images.isEmpty()) {
                    stringResource(baseNote.getEmptyMessage())
                } else ""

            if (titleText.isNotEmpty()) {
                Text(
                    text = titleText,
                    fontSize = textSize.displayTitleSize.sp,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = maxTitleLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Date text and Reminder chip (directly below note title)
            val (dateTimestamp, datePrefixResId) =
                when (sortBy) {
                    NotesSortBy.CREATION_DATE -> Pair(baseNote.timestamp, R.string.creation_date)
                    NotesSortBy.MODIFIED_DATE ->
                        Pair(baseNote.modifiedTimestamp, R.string.modified_date)
                    else -> Pair(null, null)
                }

            val hasDate =
                dateTimestamp != null &&
                    (dateFormat != DateFormat.NONE || timeFormat != TimeFormat.NONE)
            val hasReminder = baseNote.reminders.isNotEmpty()

            if (hasDate || hasReminder) {
                if (titleText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (hasDate && dateTimestamp != null) {
                        val prefixText = datePrefixResId?.let { stringResource(it) } ?: ""
                        val formattedDate = Date(dateTimestamp).format(dateFormat, timeFormat)
                        Text(
                            text = "$prefixText $formattedDate".trim(),
                            fontSize = textSize.displayBodySize.sp,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }

                    if (hasReminder) {
                        Spacer(modifier = Modifier.weight(1f))
                        ReminderChip(
                            baseNote = baseNote,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            textSize = textSize,
                            onClick = onReminderClick,
                        )
                    }
                }
            }

            // Files (placed under Date & Reminder row, before Body text or Checklist)
            if (baseNote.files.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FileAttachmentPreviewChip(fileName = baseNote.files[0].originalName)
                    if (baseNote.files.size > 1) {
                        val moreCount = baseNote.files.size - 1
                        val moreFilesText =
                            pluralStringResource(
                                R.plurals.more_files,
                                moreCount,
                                moreCount,
                            )
                        FileAttachmentPreviewChip(
                            fileName = moreFilesText,
                            showIcon = false,
                        )
                    }
                }
            }

            // Body text for Note
            if (baseNote.type == Type.NOTE && baseNote.body.isNotEmpty()) {
                val showBody = maxLines >= 1 || baseNote.title.isEmpty()
                if (showBody) {
                    if (titleText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = baseNote.body,
                        fontSize = textSize.displayBodySize.sp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (maxLines >= 1) maxLines else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Checklist for List
            if (baseNote.type == Type.LIST && baseNote.items.isNotEmpty()) {
                val initializedItems = remember(baseNote.items) { baseNote.items.init() }
                val forceShowFirstItem = maxItems < 1 && baseNote.title.isEmpty()
                val displayedItems = initializedItems.take(if (forceShowFirstItem) 1 else maxItems)

                if (displayedItems.isNotEmpty()) {
                    if (titleText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        displayedItems.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp),
                            ) {
                                Icon(
                                    painter =
                                        painterResource(
                                            id =
                                                if (item.checked) R.drawable.checkbox_16
                                                else R.drawable.checkbox_outline_16
                                        ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.body,
                                    fontSize = textSize.displayBodySize.sp,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (initializedItems.size > displayedItems.size) {
                            Text(
                                text = "+${initializedItems.size - displayedItems.size}",
                                fontSize = textSize.displaySmallerSize.sp,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            // Labels
            if (!hideLabels && baseNote.labels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(modifier = Modifier.fillMaxWidth()) {
                    baseNote.labels.forEach { label ->
                        LabelChip(
                            text = label,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun BaseNote.hasNoContents() = body.isEmpty() && items.isEmpty()

private fun BaseNote.isEmpty() = title.isBlank() && hasNoContents() && images.isEmpty()

private fun BaseNote.getEmptyMessage() =
    when (type) {
        Type.NOTE -> R.string.empty_note
        Type.LIST -> R.string.empty_list
    }

@Composable
private fun parseNoteColor(colorHex: String): Color {
    if (colorHex == BaseNote.COLOR_DEFAULT) {
        return MaterialTheme.colorScheme.surface
    }
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.surface
    }
}

object SampleNotes {
    val LOREM_IPSUM_NOTE =
        BaseNote(
            id = 0L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = "Lorem Ipsum Note",
            pinned = false,
            timestamp = 1767355200000L,
            modifiedTimestamp = 1767355200000L,
            labels = emptyList(),
            body =
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val LIST_NOTE =
        BaseNote(
            id = 0L,
            type = Type.LIST,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.CLAY.toColorString(),
            title = "Shopping List",
            pinned = false,
            timestamp = 1767355200000L,
            modifiedTimestamp = 1767355200000L,
            labels = emptyList(),
            body = "",
            spans = emptyList(),
            items =
                listOf(
                    ListItem(
                        "Buy groceries",
                        checked = false,
                        isChild = false,
                        order = 0,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Clean the house",
                        checked = true,
                        isChild = false,
                        order = 1,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Call mom",
                        checked = false,
                        isChild = false,
                        order = 2,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Finish project draft",
                        checked = false,
                        isChild = false,
                        order = 3,
                        children = mutableListOf(),
                    ),
                ),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val RICH_TEXT_NOTE =
        BaseNote(
            id = 0L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = "Note with Everything",
            pinned = false,
            timestamp = 1767355200000L,
            modifiedTimestamp = 1767355200000L,
            labels = listOf("Work", "Personal", "Important", "Ideas", "Project"),
            body =
                "This note contains multiple attachments, labels, and a reminder to demonstrate full UI card features.",
            spans = emptyList(),
            items = emptyList(),
            images =
                listOf(
                    FileAttachment("cat.jpg", "cat.jpg", "image/jpeg"),
                    FileAttachment("cat.jpg", "cat.jpg", "image/jpeg"),
                ),
            files =
                listOf(
                    FileAttachment("file1.pdf", "Specification.pdf", "application/pdf"),
                    FileAttachment("file2.zip", "Project_Archive.zip", "application/zip"),
                ),
            audios = listOf(Audio("audio1.aac", 60000L, 1767355200000L)),
            reminders =
                listOf(Reminder(id = 1L, dateTime = Date(1767441600000L), repetition = null)),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val GENERATED_LIST_NOTE =
        BaseNote(
            id = 0L,
            type = Type.LIST,
            folder = Folder.NOTES,
            color = "#F8BBD0",
            title = "List 1",
            pinned = false,
            timestamp = 1767351600000L,
            modifiedTimestamp = 1767351600000L,
            labels = emptyList(),
            body = "",
            spans = emptyList(),
            items =
                listOf(
                    ListItem(
                        "Item0",
                        checked = false,
                        isChild = false,
                        order = 0,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Item1",
                        checked = false,
                        isChild = false,
                        order = 1,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Item2",
                        checked = false,
                        isChild = false,
                        order = 2,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Item3",
                        checked = false,
                        isChild = false,
                        order = 3,
                        children = mutableListOf(),
                    ),
                ),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )
}

@Preview(showBackground = true, name = "Lorem Ipsum Note")
@Composable
private fun LoremIpsumNotePreview() {
    NotallyXTheme { BaseNoteCard(baseNote = SampleNotes.LOREM_IPSUM_NOTE) }
}

@Preview(showBackground = true, name = "List Note")
@Composable
private fun ListNotePreview() {
    NotallyXTheme { BaseNoteCard(baseNote = SampleNotes.LIST_NOTE) }
}

@Preview(showBackground = true, name = "Rich Text Note With Everything")
@Composable
private fun RichTextNotePreview() {
    NotallyXTheme { BaseNoteCard(baseNote = SampleNotes.RICH_TEXT_NOTE) }
}

@Preview(showBackground = true, name = "Generated List Note")
@Composable
private fun GeneratedListNotePreview() {
    NotallyXTheme { BaseNoteCard(baseNote = SampleNotes.GENERATED_LIST_NOTE) }
}

@Preview(showBackground = true, name = "Lorem Ipsum Note")
@Composable
private fun LoremIpsumNoteSelectedPreview() {
    NotallyXTheme { BaseNoteCard(baseNote = SampleNotes.LOREM_IPSUM_NOTE, isSelected = true) }
}

@Preview(showBackground = true, name = "Lorem Ipsum Note")
@Composable
private fun LoremIpsumNoteSelectedDarkPreview() {
    NotallyXTheme(theme = Theme.DARK) {
        BaseNoteCard(baseNote = SampleNotes.LOREM_IPSUM_NOTE, isSelected = true)
    }
}

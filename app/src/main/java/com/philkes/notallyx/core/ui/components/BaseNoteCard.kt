package com.philkes.notallyx.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import coil3.compose.SubcomposeAsyncImage
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.common.extensions.isPreview
import com.philkes.notallyx.core.common.extensions.plus
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.preference.displayBodySize
import com.philkes.notallyx.core.preference.displaySmallerSize
import com.philkes.notallyx.core.preference.displayTitleSize
import com.philkes.notallyx.core.preference.model.UserPreferences
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.extensions.toAnnotatedString
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.theme.contentColorFor
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.listitem.init
import com.philkes.notallyx.feature.editor.screens.FileAttachmentPreviewChip
import java.io.File
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

data class NoteCardDisplayConfig(
    val dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormat: TimeFormat = TimeFormat.NONE,
    val sortBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    val textSize: Float = 16f,
    val maxTitleLines: Int = 2,
    val maxLines: Int = 10,
    val maxItems: Int = 5,
    val hideLabels: Boolean = false,
    val hideImages: Boolean = false,
)

fun UserPreferences.toDisplayConfig(): NoteCardDisplayConfig =
    NoteCardDisplayConfig(
        dateFormat = dateFormatOverview,
        timeFormat = timeFormatOverview,
        sortBy = notesSorting.sortedBy,
        textSize = textSizeOverview,
        maxTitleLines = maxTitle,
        maxLines = maxLines,
        maxItems = maxItems,
        hideLabels = labelTagsHiddenInOverview,
        hideImages = imagesHiddenInOverview,
    )

/** Composable replacing recycler_base_note.xml layout. Exactly 1 BaseNoteCard composable. */
@Composable
internal fun BaseNoteCard(
    baseNote: BaseNote,
    modifier: Modifier = Modifier,
    config: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    imageRoot: File? = if (isPreview()) null else LocalContext.current.getCurrentImagesDirectory(),
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onReminderClick: (() -> Unit)? = null,
) {
    val backgroundColor = parseNoteColor(baseNote.color)
    val contentColor = contentColorFor(backgroundColor)
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
        colors =
            CardDefaults.cardColors(
                containerColor = backgroundColor,
                contentColor = contentColor,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = stroke,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image Preview
            if (baseNote.images.isNotEmpty() && !config.hideImages) {
                val imageFile =
                    if (imageRoot != null)
                        File(
                            imageRoot,
                            baseNote.images[0].localName,
                        )
                    else null
                ImagePreview(imageFile, baseNote)
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // Title and Reminder
                val titleText =
                    if (baseNote.title.isNotEmpty()) {
                        baseNote.title
                    } else if (baseNote.hasNoContents() && baseNote.images.isEmpty()) {
                        stringResource(baseNote.getEmptyMessage())
                    } else ""
                val hasReminder = baseNote.reminders.isNotEmpty()

                if (titleText.isNotEmpty() || hasReminder) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (titleText.isNotEmpty()) {
                            Text(
                                text = titleText,
                                fontSize = config.textSize.displayTitleSize.sp,
                                style = MaterialTheme.typography.titleMedium,
                                color = LocalContentColor.current,
                                maxLines = config.maxTitleLines,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (hasReminder) {
                            Spacer(modifier = Modifier.weight(1f))
                            ReminderChip(
                                baseNote = baseNote,
                                dateFormat = config.dateFormat,
                                timeFormat = config.timeFormat,
                                textSize = config.textSize,
                                onClick = onReminderClick,
                            )
                        }
                    }
                }

                // Date text
                val (dateTimestamp, datePrefixResId) =
                    when (config.sortBy) {
                        NotesSortBy.CREATION_DATE ->
                            Pair(baseNote.timestamp, R.string.creation_date)

                        NotesSortBy.MODIFIED_DATE ->
                            Pair(baseNote.modifiedTimestamp, R.string.modified_date)

                        else -> Pair(null, null)
                    }

                val hasDate =
                    dateTimestamp != null &&
                        (config.dateFormat != DateFormat.NONE ||
                            config.timeFormat != TimeFormat.NONE)

                if (hasDate) {
                    val prefixText = datePrefixResId?.let { stringResource(it) } ?: ""
                    val formattedDate =
                        Date(dateTimestamp).format(config.dateFormat, config.timeFormat)
                    Text(
                        text = "$prefixText $formattedDate".trim(),
                        fontSize = config.textSize.displayBodySize.sp,
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalContentColor.current.copy(alpha = 0.7f),
                    )
                }

                // Files (placed under Date & Reminder row, before Body text or Checklist)
                if (baseNote.files.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    val showBody = config.maxLines >= 1 || baseNote.title.isEmpty()
                    if (showBody) {
                        if (titleText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        val annotatedBody =
                            remember(baseNote.body, baseNote.spans) {
                                baseNote.body.toAnnotatedString(baseNote.spans)
                            }
                        Text(
                            text = annotatedBody,
                            fontSize = config.textSize.displayBodySize.sp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current,
                            maxLines = if (config.maxLines >= 1) config.maxLines else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Checklist for List
                if (baseNote.type == Type.LIST && baseNote.items.isNotEmpty()) {
                    val initializedItems = remember(baseNote.items) { baseNote.items.init() }
                    val forceShowFirstItem = config.maxItems < 1 && baseNote.title.isEmpty()
                    val displayedItems =
                        initializedItems.take(if (forceShowFirstItem) 1 else config.maxItems)

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
                                    if (item.isChild) {
                                        Spacer(modifier = Modifier.width(13.dp))
                                    }
                                    Icon(
                                        painter =
                                            painterResource(
                                                id =
                                                    if (item.checked) R.drawable.checkbox_16
                                                    else R.drawable.checkbox_outline_16
                                            ),
                                        contentDescription = null,
                                        tint = LocalContentColor.current,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.body,
                                        fontSize = config.textSize.displayBodySize.sp,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LocalContentColor.current,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            if (initializedItems.size > displayedItems.size) {
                                Text(
                                    text = "+${initializedItems.size - displayedItems.size}",
                                    fontSize = config.textSize.displaySmallerSize.sp,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalContentColor.current,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                }

                // Labels
                if (!config.hideLabels && baseNote.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        baseNote.labels.forEach { label ->
                            LabelChip(
                                text = label,
                                textColor = LocalContentColor.current,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImagePreview(
    imageFile: File?,
    baseNote: BaseNote,
) {
    val isPreview = isPreview()
    if (isPreview) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(8f / 5f)) {
            Image(
                painter = painterResource(id = R.drawable.vacation),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            if (baseNote.images.size > 1) {
                ImageCountChip(
                    count = baseNote.images.size,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }
        }
    } else if (imageFile?.exists() == true) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(8f / 5f)) {
            SubcomposeAsyncImage(
                model = imageFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                error = {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.cant_load_image),
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalContentColor.current,
                        )
                    }
                },
            )
            if (baseNote.images.size > 1) {
                ImageCountChip(
                    count = baseNote.images.size,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
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
                color = LocalContentColor.current,
            )
        }
    }
}

private fun BaseNote.hasNoContents() = body.isEmpty() && items.isEmpty()

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
        val cleanHex = colorHex.removePrefix("#")
        val hex = cleanHex.toLong(16)
        if (cleanHex.length <= 6) Color(0xFF000000 or hex) else Color(hex)
    } catch (_: Exception) {
        try {
            Color(colorHex.toColorInt())
        } catch (_: Exception) {
            MaterialTheme.colorScheme.surface
        }
    }
}

object SampleNotes {
    private val date = Instant.parse("2026-01-02T12:00:00Z")

    val LOREM_IPSUM_NOTE =
        BaseNote(
            id = 0L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = "Lorem Ipsum Note",
            pinned = false,
            timestamp = date.toEpochMilliseconds(),
            modifiedTimestamp = date.plus(10.minutes).toEpochMilliseconds(),
            labels = listOf("Label1"),
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
            id = 2L,
            type = Type.LIST,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.CLAY.toColorString(),
            title = "Shopping List",
            pinned = false,
            timestamp = date.plus(1.days).toEpochMilliseconds(),
            modifiedTimestamp = date.plus(1.days).plus(1.minutes).toEpochMilliseconds(),
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
                        isChild = true,
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
            id = 3L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = "Note with Everything",
            pinned = false,
            timestamp = date.plus(2.days).toEpochMilliseconds(),
            modifiedTimestamp = date.plus(2.days).plus(1.minutes).toEpochMilliseconds(),
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
                listOf(Reminder(id = 1L, dateTime = Date().plus(2.hours), repetition = null)),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val GENERATED_LIST_NOTE =
        BaseNote(
            id = 4L,
            type = Type.LIST,
            folder = Folder.NOTES,
            color = "#F8BBD0",
            title = "List 1",
            pinned = false,
            timestamp = date.plus(3.days).toEpochMilliseconds(),
            modifiedTimestamp = date.plus(3.days).plus(1.minutes).toEpochMilliseconds(),
            labels = listOf("Label1"),
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
                        checked = true,
                        checkedTimestamp = date.plus(3.days).plus(30.seconds).toEpochMilliseconds(),
                        isChild = false,
                        order = 2,
                        children = mutableListOf(),
                    ),
                    ListItem(
                        "Item3",
                        checked = true,
                        isChild = true,
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

    val ALL = listOf(LOREM_IPSUM_NOTE, LIST_NOTE, RICH_TEXT_NOTE, GENERATED_LIST_NOTE)
}

@Preview(showBackground = true, name = "Lorem Ipsum Note")
@Composable
private fun LoremIpsumNotePreview() {
    NotallyXTheme {
        BaseNoteCard(
            baseNote = SampleNotes.LOREM_IPSUM_NOTE,
            imageRoot = null,
        )
    }
}

@Preview(showBackground = true, name = "List Note")
@Composable
private fun ListNotePreview() {
    NotallyXTheme {
        BaseNoteCard(
            baseNote = SampleNotes.LIST_NOTE,
            imageRoot = null,
        )
    }
}

@Preview(showBackground = true, name = "Rich Text Note With Everything")
@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RichTextNotePreview() {
    NotallyXTheme {
        BaseNoteCard(
            baseNote = SampleNotes.RICH_TEXT_NOTE,
            imageRoot = null,
        )
    }
}

@Preview(showBackground = true, name = "Generated List Note")
@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GeneratedListNotePreview() {
    NotallyXTheme {
        BaseNoteCard(
            baseNote = SampleNotes.GENERATED_LIST_NOTE,
            imageRoot = null,
        )
    }
}

@Preview(showBackground = true, name = "Dark List Note")
@Composable
private fun DarkListNotePreview() {
    NotallyXTheme {
        BaseNoteCard(
            baseNote = SampleNotes.GENERATED_LIST_NOTE.copy(color = "#111111"),
            imageRoot = null,
        )
    }
}

@Preview(showBackground = true, name = "Lorem Ipsum Note")
@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoremIpsumNoteSelectedPreview() {
    NotallyXTheme {
        BaseNoteCard(baseNote = SampleNotes.LOREM_IPSUM_NOTE, isSelected = true, imageRoot = null)
    }
}

@Preview(showBackground = true)
@Composable
private fun NoteWithEverythingMinimalDisplayPreview() {
    NotallyXTheme {
        BaseNoteCard(
            baseNote = SampleNotes.RICH_TEXT_NOTE,
            config =
                NoteCardDisplayConfig(
                    dateFormat = DateFormat.NONE,
                    timeFormat = TimeFormat.NONE,
                    maxItems = 1,
                    maxLines = 1,
                    hideLabels = true,
                    hideImages = true,
                ),
            imageRoot = null,
            isSelected = true,
        )
    }
}

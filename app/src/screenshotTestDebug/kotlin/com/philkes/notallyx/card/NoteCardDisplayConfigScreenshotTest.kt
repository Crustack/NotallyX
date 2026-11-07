package com.philkes.notallyx.card

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type

private val PREVIEW_TEXT_NOTE =
    BaseNote(
        id = 101L,
        type = Type.NOTE,
        folder = Folder.NOTES,
        color = BaseNote.COLOR_DEFAULT,
        title = "Title Line 1\nTitle Line 2\nTitle Line 3\nTitle Line 4",
        pinned = false,
        timestamp = 1700000000000L,
        modifiedTimestamp = 1700000000000L,
        labels = listOf("Work", "Design", "Important"),
        body =
            "Body Line 1: Quick brown fox jumps over the lazy dog.\n" +
                "Body Line 2: The five boxing wizards jump quickly.\n" +
                "Body Line 3: Pack my box with five dozen liquor jugs.\n" +
                "Body Line 4: Jackdaws love my big sphinx of quartz.",
        spans = emptyList(),
        items = emptyList(),
        images =
            listOf(
                FileAttachment("cat.jpg", "cat.jpg", "image/jpeg"),
                FileAttachment("dog.jpg", "dog.jpg", "image/jpeg"),
            ),
        files = emptyList(),
        audios = emptyList(),
        reminders = emptyList(),
        viewMode = NoteViewMode.EDIT,
        isPinnedToStatus = false,
    )

private val PREVIEW_LIST_NOTE =
    BaseNote(
        id = 102L,
        type = Type.LIST,
        folder = Folder.NOTES,
        color = BaseNote.COLOR_DEFAULT,
        title = "Weekly Checklist",
        pinned = false,
        timestamp = 1700000000000L,
        modifiedTimestamp = 1700000000000L,
        labels = listOf("Chores"),
        body = "",
        spans = emptyList(),
        items =
            listOf(
                ListItem(
                    "Item 1: Buy groceries",
                    checked = false,
                    isChild = false,
                    order = 0,
                    children = mutableListOf(),
                ),
                ListItem(
                    "Item 2: Clean the house",
                    checked = true,
                    isChild = false,
                    order = 1,
                    children = mutableListOf(),
                ),
                ListItem(
                    "Item 3: Call plumber",
                    checked = false,
                    isChild = false,
                    order = 2,
                    children = mutableListOf(),
                ),
                ListItem(
                    "Item 4: Pay electric bill",
                    checked = false,
                    isChild = false,
                    order = 3,
                    children = mutableListOf(),
                ),
                ListItem(
                    "Item 5: Water plants",
                    checked = false,
                    isChild = false,
                    order = 4,
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

@Composable
private fun CardScreenshotWrapper(content: @Composable () -> Unit) {
    NotallyXTheme { Box(modifier = Modifier.width(360.dp).padding(8.dp)) { content() } }
}

// ==================== maxItems (0, 1, 3) ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxItems_0() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_LIST_NOTE,
            config = NoteCardDisplayConfig(maxItems = 0),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxItems_1() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_LIST_NOTE,
            config = NoteCardDisplayConfig(maxItems = 1),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxItems_3() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_LIST_NOTE,
            config = NoteCardDisplayConfig(maxItems = 3),
        )
    }
}

// ==================== maxLines (0, 1, 3) ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxLines_0() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(maxLines = 0),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxLines_1() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(maxLines = 1),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxLines_3() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(maxLines = 3),
        )
    }
}

// ==================== maxTitle (1, 3) ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxTitle_1() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(maxTitleLines = 1),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_MaxTitle_3() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(maxTitleLines = 3),
        )
    }
}

// ==================== labelTagsHiddenInOverview (true, false) ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_Labels_Hidden() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(hideLabels = true),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_Labels_Visible() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(hideLabels = false),
        )
    }
}

// ==================== imagesHiddenInOverview (true, false) ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_Images_Hidden() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(hideImages = true),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_Images_Visible() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(hideImages = false),
        )
    }
}

// ==================== textSize (16f, 10f, 22f) ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_TextSize_Default16() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(textSize = 16f),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_TextSize_Small10() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(textSize = 10f),
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NoteCard_TextSize_Large22() {
    CardScreenshotWrapper {
        BaseNoteCard(
            baseNote = PREVIEW_TEXT_NOTE,
            config = NoteCardDisplayConfig(textSize = 22f),
        )
    }
}

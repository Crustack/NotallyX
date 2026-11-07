package com.philkes.notallyx.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.MainScreen
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@Composable
private fun CardScreenshotWrapper(content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(360.dp).padding(8.dp)) { content() }
}

// ==================== BaseNoteCard - Text Note ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_Text_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = SampleNotes.LOREM_IPSUM_NOTE) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_Text_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = SampleNotes.LOREM_IPSUM_NOTE) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_Text_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = SampleNotes.LOREM_IPSUM_NOTE) }
    }
}

// ==================== BaseNoteCard - List Note ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_List_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = SampleNotes.LIST_NOTE) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_List_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = SampleNotes.LIST_NOTE) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_List_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = SampleNotes.LIST_NOTE) }
    }
}

// ==================== NotesGridScreen ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NotesGridScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) { NotesGridScreen(items = SampleNotes.ALL) }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NotesGridScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) { NotesGridScreen(items = SampleNotes.ALL) }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun NotesGridScreen_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) { NotesGridScreen(items = SampleNotes.ALL) }
}

// ==================== MainScreen ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun MainScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        MainScreen(title = "Notes") { NotesGridScreen(items = SampleNotes.ALL) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun MainScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        MainScreen(title = "Notes") { NotesGridScreen(items = SampleNotes.ALL) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun MainScreen_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        MainScreen(title = "Notes") { NotesGridScreen(items = SampleNotes.ALL) }
    }
}

package com.philkes.notallyx.empty

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.labels.LabelsScreen
import com.philkes.notallyx.feature.main.routes.NotesGridScreen

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyNotesScreen_Notes_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        NotesGridScreen(
            items = emptyList(),
            backgroundResId = R.drawable.notebook,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyNotesScreen_Notes_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        NotesGridScreen(
            items = emptyList(),
            backgroundResId = R.drawable.notebook,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyNotesScreen_Archived_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        NotesGridScreen(
            items = emptyList(),
            backgroundResId = R.drawable.archive,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyNotesScreen_Archived_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        NotesGridScreen(
            items = emptyList(),
            backgroundResId = R.drawable.archive,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyNotesScreen_Deleted_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        NotesGridScreen(
            items = emptyList(),
            backgroundResId = R.drawable.delete,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyNotesScreen_Deleted_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        NotesGridScreen(
            items = emptyList(),
            backgroundResId = R.drawable.delete,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyNotesScreen_Reminders_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        NotesGridScreen(
            items = emptyList(),
            backgroundResId = R.drawable.notifications,
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyLabelsScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) { LabelsScreen(labels = emptyList()) }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun EmptyLabelsScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) { LabelsScreen(labels = emptyList()) }
}

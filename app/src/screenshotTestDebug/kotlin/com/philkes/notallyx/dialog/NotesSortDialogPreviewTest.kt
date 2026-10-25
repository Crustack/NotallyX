package com.philkes.notallyx.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.settings.dialogs.NotesSortDialog

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun NotesSortDialog_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        NotesSortDialog(
            currentSort =
                NotesSort(sortedBy = NotesSortBy.TITLE, sortDirection = SortDirection.ASC),
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewTest
@Composable
fun NotesSortDialog_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        NotesSortDialog(
            currentSort =
                NotesSort(sortedBy = NotesSortBy.TITLE, sortDirection = SortDirection.ASC),
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}

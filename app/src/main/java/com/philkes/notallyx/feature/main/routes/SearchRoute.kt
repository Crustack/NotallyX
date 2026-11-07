package com.philkes.notallyx.feature.main.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.main.routes.search.SearchRoute as SearchRouteImpl

@Composable
fun SearchRoute(
    initialFolder: Folder?,
    initialLabel: String?,
    displayConfig: NoteCardDisplayConfig,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: (BaseNote) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") notallyRouteModel: NotesRouteViewModel? = null,
) {
    SearchRouteImpl(
        initialFolder = initialFolder,
        initialLabel = initialLabel,
        displayConfig = displayConfig,
        onNoteClick = onNoteClick,
        onNoteLongClick = onNoteLongClick,
        onReminderClick = onReminderClick,
        modifier = modifier,
    )
}

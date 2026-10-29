package com.philkes.notallyx.feature.main.routes

import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.main.PickNoteScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun PickNoteRoute(
    excludeNoteId: Long?,
    onNotePicked: (BaseNote) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    preferences: NotallyXPreferences = koinInject(),
) {
    val context = LocalContext.current
    val databaseStateFlow =
        NotallyDatabase.getDatabase(context.applicationContext as ContextWrapper)
    val database by databaseStateFlow.collectAsStateWithLifecycle()

    val notes by
        produceState<List<BaseNote>>(emptyList(), database, excludeNoteId) {
            val db = database ?: return@produceState
            value =
                withContext(Dispatchers.IO) {
                    val allNotes = db.getBaseNoteDao().getAllNotes()
                    if (excludeNoteId != null) {
                        allNotes.filter { it.id != excludeNoteId }
                    } else {
                        allNotes
                    }
                }
        }

    val isGrid = preferences.notesView.value == NotesView.GRID

    PickNoteScreen(
        notes = notes,
        isGrid = isGrid,
        onNoteClick = onNotePicked,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

const val EXTRA_EXCLUDE_NOTE_ID = "notallyx.intent.extra.EXCLUDE_NOTE_ID"
const val EXTRA_PICKED_NOTE_ID = "notallyx.intent.extra.PICKED_NOTE_ID"
const val EXTRA_PICKED_NOTE_TITLE = "notallyx.intent.extra.PICKED_NOTE_TITLE"
const val EXTRA_PICKED_NOTE_TYPE = "notallyx.intent.extra.PICKED_NOTE_TYPE"

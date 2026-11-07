package com.philkes.notallyx.feature.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.extensions.collectIn
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

open class PickNoteActivity : AppCompatActivity() {

    protected val preferences: NotallyXPreferences by inject()
    private val excludedNoteId by lazy { intent.getLongExtra(EXTRA_EXCLUDE_NOTE_ID, -1L) }
    private val notesState = mutableStateOf<List<BaseNote>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = Intent()
        setResult(RESULT_CANCELED, result)

        setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                PickNoteScreen(
                    notes = notesState.value,
                    isGrid = preferences.notesView.value == NotesView.GRID,
                    onNoteClick = { note -> onNotePicked(note) },
                    onBackClick = { finish() },
                )
            }
        }

        NotallyDatabase.getDatabase(this.application).collectIn(this) { database ->
            val notes =
                database?.let { db ->
                    withContext(Dispatchers.IO) {
                        db.getBaseNoteDao().getAllNotes().filter { it.id != excludedNoteId }
                    }
                } ?: return@collectIn
            notesState.value = notes
        }
    }

    open fun onNotePicked(note: BaseNote) {
        val success = Intent()
        success.putExtra(EXTRA_PICKED_NOTE_ID, note.id)
        success.putExtra(EXTRA_PICKED_NOTE_TITLE, note.title)
        success.putExtra(EXTRA_PICKED_NOTE_TYPE, note.type.name)
        setResult(RESULT_OK, success)
        finish()
    }

    companion object {
        const val EXTRA_EXCLUDE_NOTE_ID = "notallyx.intent.extra.EXCLUDE_NOTE_ID"

        const val EXTRA_PICKED_NOTE_ID = "notallyx.intent.extra.PICKED_NOTE_ID"
        const val EXTRA_PICKED_NOTE_TITLE = "notallyx.intent.extra.PICKED_NOTE_TITLE"
        const val EXTRA_PICKED_NOTE_TYPE = "notallyx.intent.extra.PICKED_NOTE_TYPE"
    }
}

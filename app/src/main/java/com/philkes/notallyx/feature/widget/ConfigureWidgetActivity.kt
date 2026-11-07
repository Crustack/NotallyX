package com.philkes.notallyx.feature.widget

import android.appwidget.AppWidgetManager
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
import com.philkes.notallyx.feature.main.PickNoteScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class ConfigureWidgetActivity : AppCompatActivity() {

    private val preferences: NotallyXPreferences by inject()
    private val notesState = mutableStateOf<List<BaseNote>>(emptyList())

    private val id by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val result = Intent()
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
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
                    withContext(Dispatchers.IO) { db.getBaseNoteDao().getAllNotes() }
                } ?: return@collectIn
            notesState.value = notes
        }
    }

    private fun onNotePicked(note: BaseNote) {
        preferences.updateWidget(id, note.id, note.type)
        NoteWidget.updateWidget(application, id, note.id, note.type)

        val success = Intent()
        success.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        setResult(RESULT_OK, success)
        finish()
    }
}

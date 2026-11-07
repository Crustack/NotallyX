package com.philkes.notallyx.feature.main.routes.notes

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.toDisplayConfig
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(
    app: Application,
    preferences: NotallyXPreferences,
    actionMode: ActionMode = ActionMode(),
) : BaseViewModel(app, preferences) {

    private val _sideEffects = Channel<NotesSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<NotesSideEffect> = _sideEffects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val baseNotesFlow: Flow<List<Item>> =
        combine(
                databaseStateFlow,
                preferences.userPreferencesFlow,
            ) { database, userPrefs ->
                Pair(database, userPrefs)
            }
            .flatMapLatest { (database, userPrefs) ->
                database
                    ?.getBaseNoteDao()
                    ?.getByFolder(Folder.NOTES)
                    ?.map { list ->
                        val filtered = list.filter { baseNote ->
                            baseNote.labels.none { userPrefs.labelsHidden.contains(it) }
                        }
                        app.createItemsFromNotes(filtered, userPrefs.notesSorting)
                    }
                    ?.catch { emit(emptyList()) } ?: flowOf(emptyList())
            }
            .onStart { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<NotesUiState> =
        combine(
                baseNotesFlow,
                preferences.userPreferencesFlow,
                actionMode.selectedIdsFlow,
            ) { items, userPrefs, selectedIds ->
                NotesUiState(
                    items = items,
                    isGrid = userPrefs.notesView == NotesView.GRID,
                    selectedIds = selectedIds,
                    displayConfig = userPrefs.toDisplayConfig(),
                    isLoading = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    NotesUiState(
                        isGrid = preferences.userPreferencesFlow.value.notesView == NotesView.GRID,
                        selectedIds = actionMode.selectedIdsFlow.value,
                        displayConfig = preferences.userPreferencesFlow.value.toDisplayConfig(),
                    ),
            )

    val baseNotes: StateFlow<List<Item>> =
        uiState
            .map { it.items }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

    fun onAction(action: NotesAction) {
        when (action) {
            is NotesAction.NoteClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(NotesSideEffect.OpenNote(action.note, action.position))
                }
            }

            is NotesAction.NoteLongClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(
                        NotesSideEffect.SelectNote(action.note, action.position, action.items)
                    )
                }
            }

            is NotesAction.ReminderClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(NotesSideEffect.OpenReminder(action.note))
                }
            }
        }
    }
}

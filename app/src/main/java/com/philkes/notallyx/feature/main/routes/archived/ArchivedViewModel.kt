package com.philkes.notallyx.feature.main.routes.archived

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

class ArchivedViewModel(
    app: Application,
    preferences: NotallyXPreferences,
    actionMode: ActionMode = ActionMode(),
) : BaseViewModel(app, preferences) {

    private val _sideEffects = Channel<ArchivedSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ArchivedSideEffect> = _sideEffects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val archivedNotesFlow: Flow<List<Item>> =
        combine(
                databaseStateFlow,
                preferences.userPreferencesFlow,
            ) { database, userPrefs ->
                Pair(database, userPrefs)
            }
            .flatMapLatest { (database, userPrefs) ->
                database
                    ?.getBaseNoteDao()
                    ?.getByFolder(Folder.ARCHIVED)
                    ?.map { list -> app.createItemsFromNotes(list, userPrefs.notesSorting) }
                    ?.catch { emit(emptyList()) } ?: flowOf(emptyList())
            }
            .onStart { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<ArchivedUiState> =
        combine(
                archivedNotesFlow,
                preferences.userPreferencesFlow,
                actionMode.selectedIdsFlow,
            ) { items, userPrefs, selectedIds ->
                ArchivedUiState(
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
                    ArchivedUiState(
                        isGrid = preferences.userPreferencesFlow.value.notesView == NotesView.GRID,
                        selectedIds = actionMode.selectedIdsFlow.value,
                        displayConfig = preferences.userPreferencesFlow.value.toDisplayConfig(),
                    ),
            )

    val archivedNotes: StateFlow<List<Item>> =
        uiState
            .map { it.items }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

    fun onAction(action: ArchivedAction) {
        when (action) {
            is ArchivedAction.NoteClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(ArchivedSideEffect.OpenNote(action.note, action.position))
                }
            }

            is ArchivedAction.NoteLongClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(
                        ArchivedSideEffect.SelectNote(action.note, action.position, action.items)
                    )
                }
            }

            is ArchivedAction.ReminderClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(ArchivedSideEffect.OpenReminder(action.note))
                }
            }
        }
    }
}

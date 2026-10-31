package com.philkes.notallyx.feature.main.routes.labels

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.toDisplayConfig
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.Item
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DisplayLabelViewModel(
    app: Application,
    preferences: NotallyXPreferences,
    actionMode: ActionMode = ActionMode(),
) : BaseViewModel(app, preferences) {

    private val _label = MutableStateFlow("")
    private val _sideEffects = Channel<DisplayLabelSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<DisplayLabelSideEffect> = _sideEffects.receiveAsFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesByLabelFlow: Flow<List<Item>> =
        combine(
                databaseStateFlow,
                preferences.userPreferencesFlow,
                _label,
            ) { database, userPrefs, label ->
                Triple(database, userPrefs, label)
            }
            .flatMapLatest { (database, userPrefs, label) ->
                if (label.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    database
                        ?.getBaseNoteDao()
                        ?.getByLabel(label)
                        ?.map { list -> app.createItemsFromNotes(list, userPrefs.notesSorting) }
                        ?.catch { emit(emptyList()) } ?: flowOf(emptyList())
                }
            }
            .onStart { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<DisplayLabelUiState> =
        combine(
                notesByLabelFlow,
                preferences.userPreferencesFlow,
                actionMode.selectedIdsFlow,
                _label,
            ) { items, userPrefs, selectedIds, label ->
                DisplayLabelUiState(
                    label = label,
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
                    DisplayLabelUiState(
                        isGrid = preferences.userPreferencesFlow.value.notesView == NotesView.GRID,
                        selectedIds = actionMode.selectedIdsFlow.value,
                        displayConfig = preferences.userPreferencesFlow.value.toDisplayConfig(),
                    ),
            )

    fun onAction(action: DisplayLabelAction) {
        when (action) {
            is DisplayLabelAction.SetLabel -> {
                _label.update { action.label }
            }

            is DisplayLabelAction.NoteClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(DisplayLabelSideEffect.OpenNote(action.note, action.position))
                }
            }

            is DisplayLabelAction.NoteLongClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(
                        DisplayLabelSideEffect.SelectNote(
                            action.note,
                            action.position,
                            action.items,
                        )
                    )
                }
            }

            is DisplayLabelAction.ReminderClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(DisplayLabelSideEffect.OpenReminder(action.note))
                }
            }
        }
    }
}

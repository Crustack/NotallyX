package com.philkes.notallyx.feature.reminder

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.toDisplayConfig
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.extensions.hasAnyUpcomingNotifications
import com.philkes.notallyx.feature.main.sorting.compareLastNotification
import com.philkes.notallyx.feature.main.sorting.compareNextNotification
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    app: Application,
    preferences: NotallyXPreferences,
    actionMode: ActionMode = ActionMode(),
) : BaseViewModel(app, preferences) {

    private val _sideEffects = Channel<RemindersSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<RemindersSideEffect> = _sideEffects.receiveAsFlow()

    private val _filterMode = MutableStateFlow(FilterOptions.UPCOMING)

    @OptIn(ExperimentalCoroutinesApi::class)
    val reminderNotes: StateFlow<List<BaseNote>> =
        databaseStateFlow
            .flatMapLatest { database ->
                database?.getBaseNoteDao()?.getAllWithReminders()?.catch { emit(emptyList()) }
                    ?: flowOf(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

    val uiState: StateFlow<RemindersUiState> =
        combine(
                reminderNotes,
                _filterMode,
                preferences.userPreferencesFlow,
                actionMode.selectedIdsFlow,
            ) { notes, filterMode, userPrefs, selectedIds ->
                val filtered =
                    when (filterMode) {
                        FilterOptions.ALL ->
                            notes.sortedWith { n1, n2 ->
                                val sort =
                                    n1.compareNextNotification(n2).takeIf { it != 0 }
                                        ?: n1.compareLastNotification(n2)
                                -1 * sort
                            }
                        FilterOptions.UPCOMING ->
                            notes
                                .filter { it.reminders.hasAnyUpcomingNotifications() }
                                .sortedWith { n1, n2 -> n1.compareNextNotification(n2) }
                        FilterOptions.ELAPSED ->
                            notes
                                .filter { !it.reminders.hasAnyUpcomingNotifications() }
                                .sortedWith { n1, n2 -> -1 * n1.compareLastNotification(n2) }
                    }
                RemindersUiState(
                    items = app.createItemsFromNotes(filtered),
                    rawNotes = notes,
                    isGrid = userPrefs.notesView == NotesView.GRID,
                    selectedIds = selectedIds,
                    displayConfig = userPrefs.toDisplayConfig(),
                    filterMode = filterMode,
                    isLoading = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    RemindersUiState(
                        isGrid = preferences.userPreferencesFlow.value.notesView == NotesView.GRID,
                        selectedIds = actionMode.selectedIdsFlow.value,
                        displayConfig = preferences.userPreferencesFlow.value.toDisplayConfig(),
                        filterMode = FilterOptions.UPCOMING,
                    ),
            )

    fun onAction(action: RemindersAction) {
        when (action) {
            is RemindersAction.FilterOptionSelected -> {
                _filterMode.value = action.filterOption
            }

            is RemindersAction.NoteClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(RemindersSideEffect.OpenNote(action.note, action.position))
                }
            }

            is RemindersAction.NoteLongClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(
                        RemindersSideEffect.SelectNote(action.note, action.position, action.items)
                    )
                }
            }

            is RemindersAction.ReminderClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(RemindersSideEffect.OpenReminder(action.note))
                }
            }
        }
    }
}

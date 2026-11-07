package com.philkes.notallyx.feature.main.routes.picknote

import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.data.model.Folder
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PickNoteViewModel(
    preferences: NotallyXPreferences,
    databaseStateFlow: StateFlow<NotallyDatabase?> = NotallyDatabase.instance,
    pathProvider: PathProvider = DesktopPathProvider(),
) : BaseViewModel(preferences, databaseStateFlow, pathProvider) {

    private val _sideEffects = Channel<PickNoteSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<PickNoteSideEffect> = _sideEffects.receiveAsFlow()

    private val _excludeNoteId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesFlow =
        combine(databaseStateFlow, _excludeNoteId) { db, excludeId -> db to excludeId }
            .flatMapLatest { (db, excludeId) ->
                val dao = db?.getBaseNoteDao()
                if (dao == null) {
                    flowOf(emptyList())
                } else {
                    dao.getByFolder(Folder.NOTES)
                        .map { notes ->
                            if (excludeId != null) {
                                notes.filter { it.id != excludeId }
                            } else {
                                notes
                            }
                        }
                        .catch { emit(emptyList()) }
                }
            }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<PickNoteUiState> =
        combine(notesFlow, preferences.userPreferencesFlow) { notes, userPrefs ->
                PickNoteUiState(
                    notes = notes,
                    isGrid = userPrefs.notesView == NotesView.GRID,
                    isLoading = false,
                    imageRoot =
                        pathProvider.getMediaDirectory(
                            "Images",
                            preferences.dataInPublicFolder.value,
                        ),
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    PickNoteUiState(
                        isGrid = preferences.userPreferencesFlow.value.notesView == NotesView.GRID,
                        isLoading = true,
                        imageRoot =
                            pathProvider.getMediaDirectory(
                                "Images",
                                preferences.dataInPublicFolder.value,
                            ),
                    ),
            )

    fun onAction(action: PickNoteAction) {
        when (action) {
            is PickNoteAction.Init -> {
                _excludeNoteId.value = action.excludeNoteId
            }

            is PickNoteAction.NoteClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(PickNoteSideEffect.NotePicked(action.note))
                }
            }

            is PickNoteAction.BackClicked -> {
                viewModelScope.launch { _sideEffects.send(PickNoteSideEffect.NavigateBack) }
            }
        }
    }
}

package com.philkes.notallyx.feature.main.routes.search

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.common.extensions.getCurrentImagesDirectory
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.toDisplayConfig
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchViewModel(
    app: Application,
    preferences: NotallyXPreferences,
    val actionMode: ActionMode = ActionMode(),
) : BaseViewModel(app, preferences) {

    private val _sideEffects = Channel<SearchSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<SearchSideEffect> = _sideEffects.receiveAsFlow()

    private val _keyword = MutableStateFlow("")
    private val _folder = MutableStateFlow(Folder.NOTES)
    private val _label = MutableStateFlow<String?>("")

    val imageRoot: File?
        get() = app.getCurrentImagesDirectory()

    private data class QueryParams(
        val keyword: String,
        val folder: Folder,
        val label: String?,
    )

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResultsFlow: Flow<List<Item>> =
        combine(
                _keyword.debounce(100.milliseconds).distinctUntilChanged(),
                _folder,
                _label,
            ) { keyword, folder, label ->
                QueryParams(keyword, folder, label)
            }
            .combine(
                combine(databaseStateFlow, preferences.userPreferencesFlow) { db, prefs ->
                    db to prefs
                }
            ) { params, (db, prefs) ->
                Triple(params, db, prefs)
            }
            .flatMapLatest { (params, db, prefs) ->
                val dao = db?.getBaseNoteDao()
                if (dao == null) {
                    flowOf(emptyList())
                } else {
                    dao.getByKeyword(params.keyword, params.folder, params.label)
                        .map { notes -> app.createItemsFromNotes(notes, prefs.notesSorting) }
                        .catch { emit(emptyList()) }
                }
            }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<SearchUiState> =
        combine(
                searchResultsFlow,
                preferences.userPreferencesFlow,
                actionMode.selectedIdsFlow,
                _keyword,
                combine(_folder, _label) { folder, label -> folder to label },
            ) { items, userPrefs, selectedIds, keyword, (folder, label) ->
                SearchUiState(
                    items = items,
                    isGrid = userPrefs.notesView == NotesView.GRID,
                    selectedIds = selectedIds,
                    displayConfig = userPrefs.toDisplayConfig(),
                    searchKeyword = keyword,
                    selectedFolder = folder,
                    currentLabel = label,
                    imageRoot = imageRoot,
                    isLoading = false,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    SearchUiState(
                        isGrid = preferences.userPreferencesFlow.value.notesView == NotesView.GRID,
                        selectedIds = actionMode.selectedIdsFlow.value,
                        displayConfig = preferences.userPreferencesFlow.value.toDisplayConfig(),
                        imageRoot = imageRoot,
                    ),
            )

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.Init -> {
                if (action.folder != null) {
                    _folder.value = action.folder
                }
                _label.value = action.label
            }

            is SearchAction.KeywordChanged -> {
                _keyword.value = action.keyword
            }

            is SearchAction.FolderSelected -> {
                _folder.value = action.folder
            }

            is SearchAction.NoteClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(SearchSideEffect.OpenNote(action.note, action.position))
                }
            }

            is SearchAction.NoteLongClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(
                        SearchSideEffect.SelectNote(action.note, action.position, action.items)
                    )
                }
            }

            is SearchAction.ReminderClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(SearchSideEffect.OpenReminder(action.note))
                }
            }
        }
    }
}

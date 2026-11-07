package com.philkes.notallyx.feature.main.routes.deleted

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.ActionMode
import com.philkes.notallyx.core.common.extensions.deleteAttachments
import com.philkes.notallyx.core.database.Converters
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.toDisplayConfig
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.data.model.Attachment
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeletedViewModel(
    app: Application,
    preferences: NotallyXPreferences,
    actionMode: ActionMode = ActionMode(),
) : BaseViewModel(app, preferences) {

    private val _activeDialog = MutableStateFlow<DeletedDialogState?>(null)

    private val _sideEffects = Channel<DeletedSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<DeletedSideEffect> = _sideEffects.receiveAsFlow()

    fun showDeleteAllDialog() {
        _activeDialog.value = DeletedDialogState.ConfirmDeleteAll
    }

    fun dismissDeleteAllDialog() {
        _activeDialog.value = null
    }

    fun confirmDeleteAll() {
        deleteAllTrashedBaseNotes()
        dismissDeleteAllDialog()
    }

    val showConfirmDeleteAllDialog: StateFlow<Boolean> =
        _activeDialog
            .map { it is DeletedDialogState.ConfirmDeleteAll }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val deletedNotesFlow: Flow<List<Item>> =
        databaseStateFlow
            .flatMapLatest { database ->
                database?.getBaseNoteDao()?.getByFolder(Folder.DELETED)?.catch { emit(emptyList()) }
                    ?: flowOf(emptyList())
            }
            .map { list -> app.createItemsFromNotes(list) }
            .onStart { emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    val uiState: StateFlow<DeletedUiState> =
        combine(
                deletedNotesFlow,
                preferences.userPreferencesFlow,
                actionMode.selectedIdsFlow,
                _activeDialog,
            ) { items, userPrefs, selectedIds, dialog ->
                DeletedUiState(
                    items = items,
                    isGrid = userPrefs.notesView == NotesView.GRID,
                    selectedIds = selectedIds,
                    displayConfig = userPrefs.toDisplayConfig(),
                    isLoading = false,
                    activeDialog = dialog,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    DeletedUiState(
                        isGrid = preferences.userPreferencesFlow.value.notesView == NotesView.GRID,
                        selectedIds = actionMode.selectedIdsFlow.value,
                        displayConfig = preferences.userPreferencesFlow.value.toDisplayConfig(),
                    ),
            )

    val deletedNotes: StateFlow<List<Item>> =
        uiState
            .map { it.items }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

    fun onAction(action: DeletedAction) {
        when (action) {
            is DeletedAction.RequestDeleteAll -> showDeleteAllDialog()
            is DeletedAction.DismissDialog -> dismissDeleteAllDialog()
            is DeletedAction.ConfirmDeleteAll -> confirmDeleteAll()
            is DeletedAction.NoteClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(DeletedSideEffect.OpenNote(action.note, action.position))
                }
            }

            is DeletedAction.NoteLongClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(
                        DeletedSideEffect.SelectNote(action.note, action.position, action.items)
                    )
                }
            }

            is DeletedAction.ReminderClicked -> {
                viewModelScope.launch {
                    _sideEffects.send(DeletedSideEffect.OpenReminder(action.note))
                }
            }
        }
    }

    fun deleteAllTrashedBaseNotes() {
        val dao = baseNoteDao ?: return
        viewModelScope.launch {
            val ids: LongArray
            val images = ArrayList<FileAttachment>()
            val files = ArrayList<FileAttachment>()
            val audios = ArrayList<Audio>()
            withContext(Dispatchers.IO) {
                ids = dao.getDeletedNoteIds()
                val imageStrings = dao.getDeletedNoteImages()
                val fileStrings = dao.getDeletedNoteFiles()
                val audioStrings = dao.getDeletedNoteAudios()
                imageStrings.flatMapTo(images) { json -> Converters.jsonToFiles(json) }
                fileStrings.flatMapTo(files) { json -> Converters.jsonToFiles(json) }
                audioStrings.flatMapTo(audios) { json -> Converters.jsonToAudios(json) }
                dao.deleteFrom(Folder.DELETED)
            }
            val attachments = ArrayList<Attachment>(images.size + files.size + audios.size)
            attachments.addAll(images)
            attachments.addAll(files)
            attachments.addAll(audios)
            withContext(Dispatchers.IO) { app.deleteAttachments(attachments, ids) }
        }
    }
}

package com.philkes.notallyx.feature.editor

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.print.PdfPrintListener
import android.view.View
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.common.extensions.toReadablePath
import com.philkes.notallyx.core.common.extensions.uniqueCurrentMillis
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction as PrefEditListAction
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.core.ui.extensions.showSnackbar
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.deepCopy
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.ListItemBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.changes.ListItemsChange
import com.philkes.notallyx.feature.editor.history.computeListItemBodyDelta
import com.philkes.notallyx.feature.editor.listitem.checkItem
import com.philkes.notallyx.feature.editor.listitem.cloneList
import com.philkes.notallyx.feature.editor.listitem.initChildren
import com.philkes.notallyx.feature.editor.listitem.initOrders
import com.philkes.notallyx.feature.editor.listitem.moveWithChildren
import com.philkes.notallyx.feature.editor.listitem.removeWithChildren
import com.philkes.notallyx.feature.editor.listitem.splitByChecked
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.SearchState
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.exportPdfFile
import com.philkes.notallyx.feature.migration.backup.exportPlainTextFile
import com.philkes.notallyx.feature.reminder.cancelPinAndReminders
import com.philkes.notallyx.feature.widget.NoteWidget
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class EditListViewModel(
    app: Application,
    preferences: NotallyXPreferences,
) : NoteModel(app, preferences), NoteActionHandler {

    private val changeHistory = ChangeHistory()

    private var isInitialized = false
    private val _noteId = MutableStateFlow(0L)
    private val _isLoading = MutableStateFlow(true)
    private val _title = MutableStateFlow("")
    private val _itemsVersion = MutableStateFlow(0)
    private val _labels = MutableStateFlow<List<String>>(emptyList())
    private val _color = MutableStateFlow(color)
    private val _pinned = MutableStateFlow(false)
    private val _isPinnedToStatus = MutableStateFlow(false)
    private val _searchState = MutableStateFlow(SearchState())
    private val _topActions = MutableStateFlow(preferences.getSafeEditNoteActivityTopActions())
    private val _bottomAction = MutableStateFlow(preferences.editNoteActivityBottomAction.value)
    private val _focusFirstItem = MutableStateFlow(false)

    var selectedExportMimeType: ExportMimeType = ExportMimeType.TXT
    private var autoSaveJob: Job? = null

    private val _sideEffects = Channel<EditListSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<EditListSideEffect> = _sideEffects.receiveAsFlow()

    private data class CoreListState(
        val noteId: Long,
        val title: String,
        val itemsVersion: Int,
        val labels: List<String>,
        val color: ColorString,
        val pinned: Boolean,
        val isPinnedToStatus: Boolean,
        val searchState: SearchState,
        val focusFirstItem: Boolean,
    )

    private data class MetaState(
        val topActions: List<EditAction>,
        val bottomAction: EditAction,
        val canUndo: Boolean,
        val canRedo: Boolean,
    )

    val uiState: StateFlow<EditListUiState> =
        combine(
                combine(
                    _noteId,
                    _title,
                    _itemsVersion,
                    _labels,
                    _color,
                    _pinned,
                    _isPinnedToStatus,
                    _searchState,
                    _focusFirstItem,
                ) { args: Array<Any> ->
                    @Suppress("UNCHECKED_CAST")
                    CoreListState(
                        noteId = args[0] as Long,
                        title = args[1] as String,
                        itemsVersion = args[2] as Int,
                        labels = args[3] as List<String>,
                        color = args[4] as ColorString,
                        pinned = args[5] as Boolean,
                        isPinnedToStatus = args[6] as Boolean,
                        searchState = args[7] as SearchState,
                        focusFirstItem = args[8] as Boolean,
                    )
                },
                combine(
                    _topActions,
                    _bottomAction,
                    changeHistory.canUndo,
                    changeHistory.canRedo,
                ) { top, bottom, canUndo, canRedo ->
                    MetaState(top, bottom, canUndo, canRedo)
                },
                combine(images, files, audios, reminders) { imgs, fls, auds, rems ->
                    listOf(imgs, fls, auds, rems)
                },
                combine(viewMode, activeDialog, activeBottomSheet, _isLoading) {
                    vm,
                    dlg,
                    bs,
                    loading ->
                    listOf(vm, dlg, bs, loading)
                },
            ) { core, meta, attachments, dialogAndMode ->
                @Suppress("UNCHECKED_CAST") val imgs = attachments[0] as List<FileAttachment>
                @Suppress("UNCHECKED_CAST") val fls = attachments[1] as List<FileAttachment>
                @Suppress("UNCHECKED_CAST") val auds = attachments[2] as List<Audio>
                @Suppress("UNCHECKED_CAST") val rems = attachments[3] as List<Reminder>

                val vm = dialogAndMode[0] as NoteViewMode
                val dlg = dialogAndMode[1] as? EditorDialogState
                val bs = dialogAndMode[2] as? EditorBottomSheetState
                val loading = dialogAndMode[3] as Boolean

                val listItemSort = preferences.listItemSorting.value
                val clonedItems = items.cloneList()
                val (activeItemsList, checkedItemsList) =
                    when (listItemSort) {
                        ListItemSort.NO_AUTO_SORT -> {
                            Pair(clonedItems, emptyList())
                        }
                        ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP -> {
                            val (checked, active) = clonedItems.splitByChecked()
                            Pair(active, checked.sortedByDescending { it.checkedTimestamp ?: 0L })
                        }
                        else -> {
                            val (checked, active) = clonedItems.splitByChecked()
                            Pair(active, checked)
                        }
                    }

                val prefs =
                    EditNotePreferences(
                        notesSortBy = preferences.notesSorting.value.sortedBy,
                        dateFormat = preferences.dateFormatNoteView.value,
                        timeFormat = preferences.timeFormatNoteView.value,
                        topActions = meta.topActions,
                        bottomAction = meta.bottomAction,
                    )

                EditListUiState(
                    isLoading = loading,
                    noteId = core.noteId,
                    title = core.title,
                    items = activeItemsList,
                    checkedItems = checkedItemsList,
                    createdTimestamp = timestamp,
                    modifiedTimestamp = modifiedTimestamp,
                    images = imgs,
                    mediaRoot = imageRoot,
                    files = fls,
                    audios = auds,
                    labels = core.labels,
                    reminders = rems,
                    pinned = core.pinned,
                    isPinnedToStatus = core.isPinnedToStatus,
                    viewMode = vm,
                    folder = folder,
                    type = type,
                    color = core.color,
                    canUndo = meta.canUndo,
                    canRedo = meta.canRedo,
                    searchState = core.searchState,
                    focusFirstItem = core.focusFirstItem,
                    preferences = prefs,
                    activeDialog = dlg,
                    activeBottomSheet = bs,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = EditListUiState(isLoading = true),
            )

    fun onAction(action: EditListAction) {
        when (action) {
            is EditListAction.Init -> init(action.key)
            is EditListAction.TitleChanged -> onTitleChange(action.title)
            is EditListAction.ItemTextChanged ->
                applyListItemTextChange(action.item, action.newText)
            is EditListAction.ItemChecked -> {
                applyListChange {
                    val targetItem = items.find { it.id == action.item.id } ?: action.item
                    items.checkItem(targetItem, action.checked)
                }
            }
            is EditListAction.ItemDeleted -> {
                applyListChange {
                    val targetItem = items.find { it.id == action.item.id } ?: action.item
                    items.removeWithChildren(targetItem)
                }
            }
            is EditListAction.ItemSwipeRight -> onItemSwipeRight(action.item)
            is EditListAction.ItemSwipeLeft -> onItemSwipeLeft(action.item)
            is EditListAction.ItemMove ->
                onItemMove(action.fromIndex, action.toIndex, action.originalItem)
            is EditListAction.AddItemClicked -> onAddItem()
            is EditListAction.SearchQueryChanged -> onSearchQueryChange(action.query)
            is EditListAction.NextSearchResult -> onNextSearchResult()
            is EditListAction.PrevSearchResult -> onPrevSearchResult()
            is EditListAction.CloseSearch -> endSearch()
            is EditListAction.StartSearch -> {
                _searchState.update {
                    it.copy(isActive = true, query = "", resultsCount = 0, resultPos = 0)
                }
            }
            is EditListAction.Undo -> {
                if (changeHistory.canUndo.value) changeHistory.undo()
            }
            is EditListAction.Redo -> {
                if (changeHistory.canRedo.value) changeHistory.redo()
            }
            is EditListAction.ActionClicked -> handleAction(action.action)
            is EditListAction.ActionLongClicked -> {
                emitSideEffect(EditListSideEffect.OpenActionSelection(action.action))
            }
            is EditListAction.ListActionClicked -> handleListAction(action.action)
            is EditListAction.UpdateTopActions -> {
                _topActions.value = action.topActions
            }
            is EditListAction.UpdateBottomAction -> {
                _bottomAction.value = action.bottomAction
            }
            is EditListAction.MoreOptionsClicked -> {
                activeBottomSheet.value =
                    EditorBottomSheetState.MoreList(_topActions.value, _bottomAction.value)
            }
            is EditListAction.ImageClicked -> viewImage(action.image)
            is EditListAction.FileClicked -> viewFile(action.file)
            is EditListAction.FileDelete -> deleteFiles(arrayListOf(action.file))
            is EditListAction.AudioClicked -> playAudio(action.audio)
            is EditListAction.ReminderClicked -> handleAction(EditAction.REMINDERS)
            is EditListAction.BackClicked -> handleBack()
            is EditListAction.DismissDialog -> activeDialog.value = null
            is EditListAction.DismissBottomSheet -> activeBottomSheet.value = null

            // Results
            is EditListAction.LabelsSelected -> {
                if (action.selectedLabels != labels) {
                    setLabels(action.selectedLabels)
                    _labels.value = action.selectedLabels
                    resetIdleTimer()
                }
            }
            is EditListAction.ImagesDeleted -> {
                val toDelete =
                    ArrayList(images.value.filter { it.localName in action.deletedImages })
                if (toDelete.isNotEmpty()) deleteImages(toDelete)
            }
            is EditListAction.AudioDeleted -> {
                val audio = audios.value.find { it.name == action.audioName }
                if (audio != null) deleteAudio(audio)
            }
            is EditListAction.AudioRecorded -> {
                if (action.success) addAudio()
            }
            is EditListAction.RemindersUpdated -> {
                if (id == 0L) {
                    id = action.noteId
                    _noteId.value = action.noteId
                }
                viewModelScope.launch { refreshOriginalNote() }
            }
            is EditListAction.ImagesPicked -> addImages(action.uris)
            is EditListAction.FilesPicked -> addFiles(action.uris)
            is EditListAction.ExportFilePicked -> {
                emitSideEffect(EditListSideEffect.ExportFile(action.uri, getBaseNote()))
            }
            is EditListAction.AudioPermissionResult -> {
                if (action.granted) {
                    if (audioRoot != null) {
                        emitSideEffect(EditListSideEffect.Navigate(NotallyNavKey.RecordAudio))
                    } else {
                        emitSideEffect(
                            EditListSideEffect.ShowToast(R.string.insert_an_sd_card_audio)
                        )
                    }
                } else {
                    activeDialog.value =
                        EditorDialogState.RejectionConfirm(
                            msgResId = R.string.to_record_audio,
                            onConfirm = {
                                val intent =
                                    android.content
                                        .Intent(
                                            android.provider.Settings
                                                .ACTION_APPLICATION_DETAILS_SETTINGS
                                        )
                                        .apply { data = "package:${app.packageName}".toUri() }
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                app.startActivity(intent)
                            },
                        )
                }
            }
            is EditListAction.NotificationPermissionResult -> {
                if (action.granted) {
                    isPinnedToStatus = true
                    _isPinnedToStatus.value = true
                    emitSideEffect(EditListSideEffect.RefreshStatusBarPin(getBaseNote(), true))
                } else {
                    activeDialog.value =
                        EditorDialogState.RejectionConfirm(
                            msgResId = R.string.to_pin_note_status_bar,
                            onConfirm = {
                                val intent =
                                    android.content
                                        .Intent(
                                            android.provider.Settings
                                                .ACTION_APPLICATION_DETAILS_SETTINGS
                                        )
                                        .apply { data = "package:${app.packageName}".toUri() }
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                app.startActivity(intent)
                            },
                        )
                }
            }
            is EditListAction.ColorSelected -> {
                color = action.color
                _color.value = action.color
                resetIdleTimer()
            }
            is EditListAction.ColorChanged -> {
                viewModelScope.launch(Dispatchers.IO) {
                    database.value?.getBaseNoteDao()?.updateColor(action.oldColor, action.newColor)
                }
                if (color == action.oldColor) {
                    color = action.newColor
                    _color.value = action.newColor
                }
                if (preferences.defaultNoteColor.value == action.oldColor) {
                    preferences.defaultNoteColor.save(action.newColor)
                }
                resetIdleTimer()
            }
            is EditListAction.ColorDeleted -> {
                viewModelScope.launch(Dispatchers.IO) {
                    database.value
                        ?.getBaseNoteDao()
                        ?.updateColor(action.colorToDelete, action.newColor)
                }
                if (color == action.colorToDelete) {
                    color = action.newColor
                    _color.value = action.newColor
                }
                if (preferences.defaultNoteColor.value == action.colorToDelete) {
                    preferences.defaultNoteColor.save(BaseNote.COLOR_DEFAULT)
                }
                resetIdleTimer()
            }
            is EditListAction.SetDefaultColor -> {
                preferences.defaultNoteColor.save(action.newDefault)
            }
            is EditListAction.ExportConfirmed -> {
                selectedExportMimeType = action.mimeType
                emitSideEffect(EditListSideEffect.ExportNote(getBaseNote(), action.mimeType))
            }
            is EditListAction.DeleteForeverConfirmed -> {
                viewModelScope.launch {
                    deleteBaseNote()
                    isInitialized = false
                    id = 0L
                    _noteId.value = 0L
                    emitSideEffect(EditListSideEffect.NavigateBack)
                }
            }
        }
    }

    fun showPermissionRationale(msgResId: Int, onConfirm: () -> Unit) {
        activeDialog.value = EditorDialogState.PermissionRationale(msgResId, onConfirm)
    }

    private fun emitSideEffect(effect: EditListSideEffect) {
        viewModelScope.launch { _sideEffects.send(effect) }
    }

    private fun init(key: NotallyNavKey.EditList) {
        viewModelScope.launch {
            if (isInitialized && (key.noteId == null || key.noteId == id)) {
                return@launch
            }
            isInitialized = true
            _isLoading.value = true
            type = Type.LIST
            setState(key.noteId ?: 0L)
            _noteId.value = id
            if (isNewNote) {
                if (key.initialFolder != null) folder = key.initialFolder
                if (key.initialLabel != null) setLabels(listOf(key.initialLabel))
                if (items.isEmpty()) {
                    items.add(
                        ListItem(
                            body = "",
                            checked = false,
                            isChild = false,
                            order = 0,
                            children = mutableListOf(),
                            id = 0,
                        )
                    )
                }
                _focusFirstItem.value = true
            }
            _title.value = title
            _labels.value = labels.toList()
            _color.value = color
            _pinned.value = pinned
            _isPinnedToStatus.value = isPinnedToStatus
            _itemsVersion.update { it + 1 }
            _isLoading.value = false
        }
    }

    private fun resetIdleTimer() {
        autoSaveJob?.cancel()
        val idleTime = preferences.autoSaveAfterIdleTime.value
        if (idleTime > -1) {
            autoSaveJob = viewModelScope.launch {
                delay((idleTime.toLong() * 1000).milliseconds)
                if (isModified() && !isEmpty()) {
                    saveNoteAndBroadcast(checkBackupOnSave = false)
                }
            }
        }
    }

    private fun updateModel() {
        modifiedTimestamp = System.currentTimeMillis()
        title = _title.value
    }

    suspend fun saveNoteAndBroadcast(checkBackupOnSave: Boolean = true): Long {
        updateModel()
        val noteId = saveNote(checkBackupOnSave)
        _noteId.value = noteId
        NoteWidget.sendBroadcast(app, longArrayOf(noteId))
        return noteId
    }

    fun handleBack() {
        viewModelScope.launch {
            updateModel()
            if (isEmpty()) {
                deleteBaseNote(checkAutoSave = false)
            } else if (isModified()) {
                saveNoteAndBroadcast(checkBackupOnSave = true)
            } else {
                checkBackupOnSave()
            }
            isInitialized = false
            id = 0L
            _noteId.value = 0L
            emitSideEffect(EditListSideEffect.NavigateBack)
        }
    }

    fun saveOnExitIfNeeded() {
        if (isModified() && !isEmpty()) {
            updateModel()
            runBlocking(Dispatchers.IO) {
                saveNote(checkBackupOnSave = false)
                NoteWidget.sendBroadcast(app, longArrayOf(id))
            }
        }
    }

    private fun onTitleChange(newTitle: String) {
        _title.value = newTitle
        title = newTitle
        resetIdleTimer()
    }

    fun applyListChange(action: () -> Unit) {
        val oldList = items.cloneList()
        action()
        initChildren(items)
        val newList = items.cloneList()
        changeHistory.push(
            ListItemsChange(
                notallyModel = this,
                oldItems = oldList,
                newItems = newList,
                onItemsUpdated = { _itemsVersion.update { it + 1 } },
            )
        )
        _itemsVersion.update { it + 1 }
        resetIdleTimer()
    }

    fun applyListItemTextChange(
        item: ListItem,
        newText: String,
        oldText: String = item.body,
    ) {
        val newBody = AnnotatedString(newText)
        val oldBody = AnnotatedString(oldText)
        if (oldBody != newBody) {
            val delta =
                computeListItemBodyDelta(
                    oldValue = oldBody,
                    newValue = newBody,
                    itemId = item.id,
                    notallyModel = this,
                    oldSelection = TextRange.Zero,
                    newSelection = TextRange.Zero,
                    onItemUpdated = { _, _ ->
                        _itemsVersion.update { it + 1 }
                        if (_searchState.value.isActive) {
                            updateSearchResults(_searchState.value.query)
                        }
                        resetIdleTimer()
                    },
                )
            if (delta != null) {
                val top = changeHistory.peek()
                if (
                    !changeHistory.canRedo.value &&
                        (top is ListItemBodyDeltaChange) &&
                        top.tryMergeWith(delta)
                ) {
                    // Merged
                } else {
                    changeHistory.push(delta)
                }
            }
            val targetItem = items.find { it.id == item.id } ?: item
            targetItem.body = newBody.text
            _itemsVersion.update { it + 1 }
            if (_searchState.value.isActive) {
                updateSearchResults(_searchState.value.query)
            }
            resetIdleTimer()
        }
    }

    private fun onAddItem() {
        applyListChange {
            val maxId = items.maxOfOrNull { it.id } ?: -1
            val newItem =
                ListItem(
                    body = "",
                    checked = false,
                    isChild = false,
                    order = items.size,
                    children = mutableListOf(),
                    id = maxId + 1,
                )
            items.add(newItem)
        }
    }

    private fun onItemSwipeRight(item: ListItem) {
        val targetItem = items.find { it.id == item.id } ?: item
        val listItemSort = preferences.listItemSorting.value
        val (checked, active) = items.splitByChecked()
        val sectionList = if (targetItem.checked) checked else active
        val idx = sectionList.indexOf(targetItem)
        if (idx > 0) {
            applyListChange { targetItem.isChild = true }
        }
    }

    private fun onItemSwipeLeft(item: ListItem) {
        val targetItem = items.find { it.id == item.id } ?: item
        if (targetItem.isChild) {
            applyListChange { targetItem.isChild = false }
        }
    }

    private fun onItemMove(fromIndex: Int, toIndex: Int, originalItem: ListItem?) {
        if (fromIndex in items.indices && toIndex in 0..items.size) {
            applyListChange { items.moveWithChildren(fromIndex, toIndex, originalItem) }
        }
    }

    private fun updateSearchResults(query: String) {
        if (query.isEmpty()) {
            _searchState.update { it.copy(resultsCount = 0, resultPos = 0) }
            return
        }
        val text = items.joinToString(" ") { it.body }
        val occurrences = text.findAllOccurrences(query)
        val count = occurrences.size
        _searchState.update { current ->
            val pos =
                if (count > 0) {
                    if (current.resultPos >= count) count - 1 else current.resultPos
                } else 0
            current.copy(resultsCount = count, resultPos = pos)
        }
    }

    private fun onSearchQueryChange(query: String) {
        _searchState.update { it.copy(query = query) }
        updateSearchResults(query)
    }

    private fun onNextSearchResult() {
        _searchState.update { current ->
            if (current.resultsCount > 0) {
                current.copy(resultPos = (current.resultPos + 1) % current.resultsCount)
            } else current
        }
    }

    private fun onPrevSearchResult() {
        _searchState.update { current ->
            if (current.resultsCount > 0) {
                val newPos =
                    if (current.resultPos > 0) current.resultPos - 1 else current.resultsCount - 1
                current.copy(resultPos = newPos)
            } else current
        }
    }

    private fun endSearch() {
        _searchState.value = SearchState()
    }

    private fun moveNote(toFolder: Folder) {
        if (toFolder != Folder.NOTES) {
            app.cancelPinAndReminders(id, reminders.value)
        }
        folder = toFolder
        viewModelScope.launch {
            saveNote(checkBackupOnSave = false)
            isInitialized = false
            id = 0L
            _noteId.value = 0L
            emitSideEffect(EditListSideEffect.NavigateBack)
        }
    }

    suspend fun duplicateNote(note: BaseNote) = duplicateNotes(listOf(note)).first()

    suspend fun duplicateNotes(notes: Collection<BaseNote>): List<Long> {
        val now = System.currentTimeMillis()
        val copies: List<BaseNote> = notes.map { original ->
            original
                .deepCopy()
                .copy(
                    id = 0L,
                    title =
                        if (original.title.isNotEmpty())
                            "${original.title} (${app.getString(R.string.copy)})"
                        else app.getString(R.string.copy),
                    timestamp = now,
                    modifiedTimestamp = now,
                )
        }
        return withContext(Dispatchers.IO) { baseNoteDao!!.insert(copies) }
    }

    // --- NoteActionHandler overrides ---
    override fun pinToStatus() {
        if (!_isPinnedToStatus.value) {
            emitSideEffect(EditListSideEffect.RequestNotificationPermission(showRationale = false))
        } else {
            isPinnedToStatus = false
            _isPinnedToStatus.value = false
            emitSideEffect(EditListSideEffect.RefreshStatusBarPin(getBaseNote(), false))
        }
    }

    override fun recordAudio() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (
            ContextCompat.checkSelfPermission(app, permission) != PackageManager.PERMISSION_GRANTED
        ) {
            emitSideEffect(
                EditListSideEffect.RequestAudioPermission(permission, showRationale = false)
            )
        } else {
            if (audioRoot != null) {
                emitSideEffect(EditListSideEffect.Navigate(NotallyNavKey.RecordAudio))
            } else {
                emitSideEffect(EditListSideEffect.ShowToast(R.string.insert_an_sd_card_audio))
            }
        }
    }

    override fun addImages() {
        if (imageRoot != null) {
            emitSideEffect(EditListSideEffect.LaunchAddImages(allowMultiple = true))
        } else {
            emitSideEffect(EditListSideEffect.ShowToast(R.string.insert_an_sd_card_images))
        }
    }

    override fun attachFiles() {
        if (filesRoot != null) {
            emitSideEffect(EditListSideEffect.LaunchAttachFiles(allowMultiple = true))
        } else {
            emitSideEffect(EditListSideEffect.ShowToast(R.string.insert_an_sd_card_files))
        }
    }

    override fun addNoteLink() {}

    override fun updateNoteLink(range: TextRange) {}

    override fun viewImage(image: FileAttachment) {
        val position = images.value.indexOf(image)
        emitSideEffect(
            EditListSideEffect.Navigate(
                NotallyNavKey.ViewImage(
                    noteId = id,
                    initialIndex = if (position != -1) position else 0,
                )
            )
        )
    }

    override fun viewFile(fileAttachment: FileAttachment) {
        val file = app.resolveAttachmentFile(SUBFOLDER_FILES, fileAttachment.localName)
        if (file != null && file.exists()) {
            val uri = app.getUriForFile(file)
            emitSideEffect(EditListSideEffect.ViewAttachmentFile(uri, fileAttachment.mimeType))
        }
    }

    override fun playAudio(audio: Audio) {
        emitSideEffect(
            EditListSideEffect.Navigate(
                NotallyNavKey.PlayAudio(
                    audioName = audio.name,
                    duration = audio.duration,
                    timestamp = audio.timestamp,
                )
            )
        )
    }

    override fun handleAction(action: EditAction) {
        when (action) {
            EditAction.SEARCH -> {
                _searchState.update {
                    it.copy(isActive = true, query = "", resultsCount = 0, resultPos = 0)
                }
            }
            EditAction.PIN -> {
                pinned = !pinned
                _pinned.value = pinned
            }
            EditAction.PIN_TO_STATUS -> pinToStatus()
            EditAction.REMINDERS -> {
                viewModelScope.launch {
                    val noteId =
                        if (id != 0L) id else saveNoteAndBroadcast(checkBackupOnSave = false)
                    emitSideEffect(EditListSideEffect.Navigate(NotallyNavKey.NoteReminders(noteId)))
                }
            }
            EditAction.LABELS -> {
                emitSideEffect(EditListSideEffect.Navigate(NotallyNavKey.SelectLabels(labels)))
            }
            EditAction.CHANGE_COLOR -> {
                viewModelScope.launch {
                    val db = database.value ?: return@launch
                    val colors: MutableSet<ColorString> =
                        withContext(Dispatchers.IO) {
                            db.getBaseNoteDao().getAllColors().toMutableSet()
                        }
                    if (colors.none { it == color }) {
                        colors.add(color)
                    }
                    activeDialog.value =
                        EditorDialogState.ColorPicker(
                            currentColor = color,
                            colors = colors,
                            defaultColor = preferences.defaultNoteColor.value,
                            onColorSelected = { onAction(EditListAction.ColorSelected(it)) },
                            onColorChanged = { oldC, newC ->
                                onAction(EditListAction.ColorChanged(oldC, newC))
                            },
                            onColorDeleted = { delC, newC ->
                                onAction(EditListAction.ColorDeleted(delC, newC))
                            },
                            onSetDefaultColor = { onAction(EditListAction.SetDefaultColor(it)) },
                        )
                }
            }
            EditAction.DUPLICATE -> {
                viewModelScope.launch {
                    saveNoteAndBroadcast()
                    val duplicateId = duplicateNote(getBaseNote())
                    emitSideEffect(EditListSideEffect.NavigateBack)
                    emitSideEffect(EditListSideEffect.Navigate(NotallyNavKey.EditList(duplicateId)))
                }
            }
            EditAction.EXPORT -> {
                activeBottomSheet.value = EditorBottomSheetState.Export { mimeType ->
                    onAction(EditListAction.ExportConfirmed(mimeType))
                }
            }
            EditAction.SHARE -> {
                emitSideEffect(EditListSideEffect.ShareNote(getBaseNote()))
            }
            EditAction.DELETE -> moveNote(Folder.DELETED)
            EditAction.ARCHIVE -> {
                if (folder == Folder.ARCHIVED) {
                    moveNote(Folder.NOTES)
                } else {
                    moveNote(Folder.ARCHIVED)
                }
            }
            EditAction.RESTORE -> moveNote(Folder.NOTES)
            EditAction.TOGGLE_VIEW_MODE -> toggleViewMode()
            EditAction.CONVERT -> {
                viewModelScope.launch {
                    updateModel()
                    convertTo(Type.NOTE)
                    isInitialized = false
                    val convertedId = id
                    id = 0L
                    _noteId.value = 0L
                    emitSideEffect(EditListSideEffect.NavigateBack)
                    emitSideEffect(EditListSideEffect.Navigate(NotallyNavKey.EditNote(convertedId)))
                }
            }
            EditAction.DELETE_FOREVER -> {
                activeDialog.value =
                    EditorDialogState.DeleteForeverConfirm(
                        onConfirm = { onAction(EditListAction.DeleteForeverConfirmed) }
                    )
            }
        }
    }

    override fun handleListAction(action: PrefEditListAction) {
        when (action) {
            PrefEditListAction.CHECK_ALL -> {
                applyListChange {
                    val now = uniqueCurrentMillis()
                    items.forEach { item ->
                        item.checked = true
                        item.checkedTimestamp = now
                    }
                }
            }
            PrefEditListAction.UNCHECK_ALL -> {
                applyListChange {
                    items.forEach { item ->
                        item.checked = false
                        item.checkedTimestamp = null
                    }
                }
            }
            PrefEditListAction.DELETE_CHECKED -> {
                applyListChange {
                    items.removeAll { it.checked }
                    initOrders(items)
                }
            }
        }
    }

    fun exportNoteToFile(fileUri: Uri, note: BaseNote, snackbarView: View) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log("EditListViewModel", throwable = throwable)
            app.showToast(R.string.something_went_wrong)
        }
        viewModelScope.launch(exceptionHandler) {
            when (selectedExportMimeType) {
                ExportMimeType.PDF -> {
                    exportPdfFile(
                        app,
                        note,
                        DocumentFile.fromSingleUri(app, fileUri)!!,
                        pdfPrintListener =
                            object : PdfPrintListener {
                                override fun onSuccess(file: DocumentFile) {
                                    val message = app.getQuantityString(R.plurals.exported_notes, 1)
                                    snackbarView.showSnackbar(
                                        "$message to '${app.toReadablePath(fileUri)}'",
                                        R.string.open_link,
                                    ) {
                                        app.viewFile(fileUri, ExportMimeType.PDF.mimeType)
                                    }
                                }

                                override fun onFailure(message: CharSequence?) {
                                    app.log("EditListViewModel", stackTrace = message as String?)
                                }
                            },
                    )
                }
                else -> {
                    exportPlainTextFile(
                        app,
                        note,
                        DocumentFile.fromSingleUri(app, fileUri)!!,
                        selectedExportMimeType,
                    )
                    val message = app.getQuantityString(R.plurals.exported_notes, 1)
                    snackbarView.showSnackbar(
                        "$message to '${app.toReadablePath(fileUri)}'",
                        R.string.open_link,
                    ) {
                        app.viewFile(fileUri, selectedExportMimeType.mimeType)
                    }
                }
            }
        }
    }
}

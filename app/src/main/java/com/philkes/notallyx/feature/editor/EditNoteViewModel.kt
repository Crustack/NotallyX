package com.philkes.notallyx.feature.editor

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.print.PdfPrintListener
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
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
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.addOrUpdateLink
import com.philkes.notallyx.core.ui.extensions.clearFormatting
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.core.ui.extensions.preserveStylesFrom
import com.philkes.notallyx.core.ui.extensions.removeLink
import com.philkes.notallyx.core.ui.extensions.showSnackbar
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.extensions.toggleBold
import com.philkes.notallyx.core.ui.extensions.toggleItalic
import com.philkes.notallyx.core.ui.extensions.toggleMonospace
import com.philkes.notallyx.core.ui.extensions.toggleStrikethrough
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.deepCopy
import com.philkes.notallyx.data.model.extensions.createNoteUrl
import com.philkes.notallyx.data.model.extensions.getNoteIdFromUrl
import com.philkes.notallyx.data.model.extensions.getNoteTypeFromUrl
import com.philkes.notallyx.data.model.extensions.isNoteUrl
import com.philkes.notallyx.feature.editor.formatting.getFormattingState
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.NoteBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.computeBodyDelta
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

class EditNoteViewModel(
    app: Application,
    preferences: NotallyXPreferences,
) : NoteModel(app, preferences), NoteActionHandler {

    private val changeHistory = ChangeHistory()

    private var isInitialized = false
    private val _noteId = MutableStateFlow(0L)
    private val _isLoading = MutableStateFlow(true)
    private val _title = MutableStateFlow("")
    private val _bodyValue = MutableStateFlow(TextFieldValue())
    private val _labels = MutableStateFlow<List<String>>(emptyList())
    private val _color = MutableStateFlow(color)
    private val _pinned = MutableStateFlow(false)
    private val _isPinnedToStatus = MutableStateFlow(false)
    private val _isFormattingMenuVisible = MutableStateFlow(false)
    private val _searchState = MutableStateFlow(SearchState())
    private val _topActions = MutableStateFlow(preferences.getSafeEditNoteActivityTopActions())
    private val _bottomAction = MutableStateFlow(preferences.editNoteActivityBottomAction.value)

    var selectedExportMimeType: ExportMimeType = ExportMimeType.TXT
    private var selectedLinkRange: TextRange? = null
    private var autoSaveJob: Job? = null

    private val _sideEffects = Channel<EditNoteSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<EditNoteSideEffect> = _sideEffects.receiveAsFlow()

    private data class CoreTextState(
        val noteId: Long,
        val title: String,
        val body: TextFieldValue,
        val labels: List<String>,
        val color: ColorString,
        val pinned: Boolean,
        val isPinnedToStatus: Boolean,
        val isFormattingMenuVisible: Boolean,
        val searchState: SearchState,
    )

    private data class MetaState(
        val topActions: List<EditAction>,
        val bottomAction: EditAction,
        val canUndo: Boolean,
        val canRedo: Boolean,
    )

    val uiState: StateFlow<EditNoteUiState> =
        combine(
                combine(
                    _noteId,
                    _title,
                    _bodyValue,
                    _labels,
                    _color,
                    _pinned,
                    _isPinnedToStatus,
                    _isFormattingMenuVisible,
                    _searchState,
                ) { args: Array<Any> ->
                    @Suppress("UNCHECKED_CAST")
                    CoreTextState(
                        noteId = args[0] as Long,
                        title = args[1] as String,
                        body = args[2] as TextFieldValue,
                        labels = args[3] as List<String>,
                        color = args[4] as ColorString,
                        pinned = args[5] as Boolean,
                        isPinnedToStatus = args[6] as Boolean,
                        isFormattingMenuVisible = args[7] as Boolean,
                        searchState = args[8] as SearchState,
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

                val selection = core.body.selection
                val formatting =
                    core.body.annotatedString.getFormattingState(selection.min, selection.max)

                val prefs =
                    EditNotePreferences(
                        notesSortBy = preferences.notesSorting.value.sortedBy,
                        dateFormat = preferences.dateFormatNoteView.value,
                        timeFormat = preferences.timeFormatNoteView.value,
                        topActions = meta.topActions,
                        bottomAction = meta.bottomAction,
                    )

                EditNoteUiState(
                    isLoading = loading,
                    noteId = core.noteId,
                    title = core.title,
                    bodyValue = core.body,
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
                    isFormattingMenuVisible = core.isFormattingMenuVisible,
                    formattingState = formatting,
                    searchState = core.searchState,
                    preferences = prefs,
                    activeDialog = dlg,
                    activeBottomSheet = bs,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = EditNoteUiState(isLoading = true),
            )

    fun onAction(action: EditNoteAction) {
        when (action) {
            is EditNoteAction.Init -> init(action.key)
            is EditNoteAction.TitleChanged -> onTitleChange(action.title)
            is EditNoteAction.BodyChanged -> onBodyValueChange(action.bodyValue)
            is EditNoteAction.SearchQueryChanged -> onSearchQueryChange(action.query)
            is EditNoteAction.NextSearchResult -> onNextSearchResult()
            is EditNoteAction.PrevSearchResult -> onPrevSearchResult()
            is EditNoteAction.CloseSearch -> endSearch()
            is EditNoteAction.StartSearch -> {
                _searchState.update {
                    it.copy(isActive = true, query = "", resultsCount = 0, resultPos = 0)
                }
            }
            is EditNoteAction.ToggleFormattingMenu -> {
                _isFormattingMenuVisible.update { !it }
            }
            is EditNoteAction.CloseFormattingMenu -> {
                _isFormattingMenuVisible.value = false
            }
            is EditNoteAction.Undo -> {
                if (changeHistory.canUndo.value) changeHistory.undo()
            }
            is EditNoteAction.Redo -> {
                if (changeHistory.canRedo.value) changeHistory.redo()
            }
            is EditNoteAction.ToggleBold -> applyBodyChange(_bodyValue.value.toggleBold())
            is EditNoteAction.ToggleItalic -> applyBodyChange(_bodyValue.value.toggleItalic())
            is EditNoteAction.ToggleStrikethrough ->
                applyBodyChange(_bodyValue.value.toggleStrikethrough())
            is EditNoteAction.ToggleMonospace -> applyBodyChange(_bodyValue.value.toggleMonospace())
            is EditNoteAction.ClearFormatting -> applyBodyChange(_bodyValue.value.clearFormatting())
            is EditNoteAction.RequestLinkDialog -> showLinkDialog()
            is EditNoteAction.LinkClicked ->
                showLinkOptionsDialog(action.url, action.displayText, action.range)
            is EditNoteAction.ApplyLink -> applyLink(action.url, action.displayText, action.range)
            is EditNoteAction.RemoveLink -> removeLink(action.range)
            is EditNoteAction.OpenLink -> openLink(action.url)
            is EditNoteAction.ActionClicked -> handleAction(action.action)
            is EditNoteAction.ActionLongClicked -> {
                emitSideEffect(EditNoteSideEffect.OpenActionSelection(action.action))
            }
            is EditNoteAction.UpdateTopActions -> {
                _topActions.value = action.topActions
            }
            is EditNoteAction.UpdateBottomAction -> {
                _bottomAction.value = action.bottomAction
            }
            is EditNoteAction.AddItemClicked -> {
                activeBottomSheet.value = EditorBottomSheetState.AddNote
            }
            is EditNoteAction.MoreOptionsClicked -> {
                activeBottomSheet.value =
                    EditorBottomSheetState.MoreNote(_topActions.value, _bottomAction.value)
            }
            is EditNoteAction.ImageClicked -> viewImage(action.image)
            is EditNoteAction.FileClicked -> viewFile(action.file)
            is EditNoteAction.FileDelete -> deleteFiles(arrayListOf(action.file))
            is EditNoteAction.AudioClicked -> playAudio(action.audio)
            is EditNoteAction.ReminderClicked -> handleAction(EditAction.REMINDERS)
            is EditNoteAction.BackClicked -> handleBack()
            is EditNoteAction.DismissDialog -> activeDialog.value = null
            is EditNoteAction.DismissBottomSheet -> activeBottomSheet.value = null

            // Results
            is EditNoteAction.LabelsSelected -> {
                if (action.selectedLabels != labels) {
                    setLabels(action.selectedLabels)
                    _labels.value = action.selectedLabels
                    resetIdleTimer()
                }
            }
            is EditNoteAction.ImagesDeleted -> {
                val toDelete =
                    ArrayList(images.value.filter { it.localName in action.deletedImages })
                if (toDelete.isNotEmpty()) deleteImages(toDelete)
            }
            is EditNoteAction.AudioDeleted -> {
                val audio = audios.value.find { it.name == action.audioName }
                if (audio != null) deleteAudio(audio)
            }
            is EditNoteAction.AudioRecorded -> {
                if (action.success) addAudio()
            }
            is EditNoteAction.RemindersUpdated -> {
                if (id == 0L) {
                    id = action.noteId
                    _noteId.value = action.noteId
                }
                viewModelScope.launch { refreshOriginalNote() }
            }
            is EditNoteAction.NotePicked ->
                handleNotePicked(action.noteId, action.noteTitle, action.noteType)
            is EditNoteAction.ImagesPicked -> addImages(action.uris)
            is EditNoteAction.FilesPicked -> addFiles(action.uris)
            is EditNoteAction.ExportFilePicked -> {
                emitSideEffect(EditNoteSideEffect.ExportFile(action.uri, getBaseNote()))
            }
            is EditNoteAction.AudioPermissionResult -> {
                if (action.granted) {
                    if (audioRoot != null) {
                        emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.RecordAudio))
                    } else {
                        emitSideEffect(
                            EditNoteSideEffect.ShowToast(R.string.insert_an_sd_card_audio)
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
            is EditNoteAction.NotificationPermissionResult -> {
                if (action.granted) {
                    isPinnedToStatus = true
                    _isPinnedToStatus.value = true
                    emitSideEffect(EditNoteSideEffect.RefreshStatusBarPin(getBaseNote(), true))
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
            is EditNoteAction.ColorSelected -> {
                color = action.color
                _color.value = action.color
                resetIdleTimer()
            }
            is EditNoteAction.ColorChanged -> {
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
            is EditNoteAction.ColorDeleted -> {
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
            is EditNoteAction.SetDefaultColor -> {
                preferences.defaultNoteColor.save(action.newDefault)
            }
            is EditNoteAction.ExportConfirmed -> {
                selectedExportMimeType = action.mimeType
                emitSideEffect(EditNoteSideEffect.ExportNote(getBaseNote(), action.mimeType))
            }
            is EditNoteAction.DeleteForeverConfirmed -> {
                viewModelScope.launch {
                    deleteBaseNote()
                    isInitialized = false
                    id = 0L
                    _noteId.value = 0L
                    emitSideEffect(EditNoteSideEffect.NavigateBack)
                }
            }
        }
    }

    fun showPermissionRationale(msgResId: Int, onConfirm: () -> Unit) {
        activeDialog.value = EditorDialogState.PermissionRationale(msgResId, onConfirm)
    }

    private fun emitSideEffect(effect: EditNoteSideEffect) {
        viewModelScope.launch { _sideEffects.send(effect) }
    }

    private fun init(key: NotallyNavKey.EditNote) {
        viewModelScope.launch {
            if (isInitialized && (key.noteId == null || key.noteId == id)) {
                return@launch
            }
            isInitialized = true
            _isLoading.value = true
            type = Type.NOTE
            setState(key.noteId ?: 0L)
            _noteId.value = id
            if (isNewNote) {
                if (key.initialFolder != null) folder = key.initialFolder
                if (key.initialLabel != null) setLabels(listOf(key.initialLabel))
                if (key.initialTitle != null) title = key.initialTitle
                if (key.initialBody != null) {
                    body =
                        TextFieldValue(
                            annotatedString = AnnotatedString(key.initialBody),
                            selection = TextRange(key.initialBody.length),
                        )
                }
                if (key.initialImageUris.isNotEmpty()) {
                    addImages(key.initialImageUris.map { it.toUri() }.toTypedArray())
                }
                if (key.initialFileUris.isNotEmpty()) {
                    addFiles(key.initialFileUris.map { it.toUri() }.toTypedArray())
                }
            }
            _title.value = title
            _bodyValue.value = body
            _labels.value = labels.toList()
            _color.value = color
            _pinned.value = pinned
            _isPinnedToStatus.value = isPinnedToStatus
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
        body = _bodyValue.value
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
            emitSideEffect(EditNoteSideEffect.NavigateBack)
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

    private fun onBodyValueChange(newBody: TextFieldValue) {
        val newText = newBody.text
        val oldText = body.text
        if (newText != oldText) {
            if (_searchState.value.isActive) {
                updateSearchResults(_searchState.value.query)
            }
            resetIdleTimer()
        }
        if (newBody.selection.min != newBody.selection.max) {
            _isFormattingMenuVisible.value = true
        }
        val updatedValue = newBody.preserveStylesFrom(_bodyValue.value)
        if (updatedValue.annotatedString != _bodyValue.value.annotatedString) {
            val delta =
                computeBodyDelta(
                    oldValue = _bodyValue.value,
                    newValue = updatedValue,
                    notallyModel = this,
                    onBodyUpdated = { restored ->
                        _bodyValue.value = restored
                        body = restored
                        if (_searchState.value.isActive) {
                            updateSearchResults(_searchState.value.query)
                        }
                        resetIdleTimer()
                    },
                )
            if (delta != null) {
                pushBodyChange(delta)
            }
        }
        _bodyValue.value = updatedValue
        body = updatedValue
    }

    private fun applyBodyChange(newBody: TextFieldValue) {
        val oldBody = _bodyValue.value
        if (newBody.annotatedString != oldBody.annotatedString) {
            val delta =
                computeBodyDelta(
                    oldValue = oldBody,
                    newValue = newBody,
                    notallyModel = this,
                    onBodyUpdated = { restored ->
                        _bodyValue.value = restored
                        body = restored
                        if (_searchState.value.isActive) {
                            updateSearchResults(_searchState.value.query)
                        }
                        resetIdleTimer()
                    },
                )
            if (delta != null) {
                pushBodyChange(delta)
            }
        }
        _bodyValue.value = newBody
        body = newBody
        resetIdleTimer()
    }

    private fun pushBodyChange(change: NoteBodyDeltaChange) {
        val top = changeHistory.peek()
        if (
            !changeHistory.canRedo.value && (top is NoteBodyDeltaChange) && top.tryMergeWith(change)
        ) {
            // Merged
        } else {
            changeHistory.push(change)
        }
    }

    private fun updateSearchResults(query: String) {
        if (query.isEmpty()) {
            _searchState.update { it.copy(resultsCount = 0, resultPos = 0) }
            return
        }
        val text = body.text
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

    private fun applyLink(url: String, displayText: String, range: TextRange? = null) {
        applyBodyChange(_bodyValue.value.addOrUpdateLink(url, displayText, range))
    }

    private fun removeLink(range: TextRange) {
        applyBodyChange(_bodyValue.value.removeLink(range))
    }

    private fun openLink(url: String) {
        if (url.isNoteUrl()) {
            val targetId = url.getNoteIdFromUrl()
            val targetType = url.getNoteTypeFromUrl()
            if (targetType == Type.NOTE) {
                emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.EditNote(targetId)))
            } else {
                emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.EditList(targetId)))
            }
        } else {
            emitSideEffect(EditNoteSideEffect.OpenUrl(url))
        }
    }

    private fun showLinkDialog() {
        val selection = _bodyValue.value.selection
        val min = selection.min
        val max = selection.max
        val hasSelection = min < max
        val selectedText = if (hasSelection) _bodyValue.value.text.substring(min, max) else ""
        val existingUrl =
            if (hasSelection) {
                _bodyValue.value.annotatedString
                    .getStringAnnotations("URL", min, max)
                    .firstOrNull()
                    ?.item ?: ""
            } else ""

        val isNoteUrl = existingUrl.isNoteUrl()
        val range = if (hasSelection) TextRange(min, max) else null
        activeDialog.value =
            EditorDialogState.EditLink(
                urlBefore = existingUrl,
                displayTextBefore = selectedText,
                isNoteUrl = isNoteUrl,
                onConfirm = { urlAfter, displayTextAfter ->
                    if (urlAfter != null) {
                        applyLink(urlAfter, displayTextAfter, range)
                    }
                },
            )
    }

    private fun showLinkOptionsDialog(url: String, displayText: String, range: TextRange) {
        val isNoteUrl = url.isNoteUrl()
        activeDialog.value =
            EditorDialogState.LinkOptions(
                url = url,
                displayText = displayText,
                isNoteUrl = isNoteUrl,
                onOpen = { openLink(url) },
                onEdit = {
                    activeDialog.value =
                        EditorDialogState.EditLink(
                            urlBefore = url,
                            displayTextBefore = displayText,
                            isNoteUrl = isNoteUrl,
                            onConfirm = { urlAfter, displayTextAfter ->
                                if (urlAfter != null) {
                                    applyLink(urlAfter, displayTextAfter, range)
                                }
                            },
                        )
                },
                onRemove = { removeLink(range) },
            )
    }

    private fun handleNotePicked(noteId: Long, noteTitle: String, noteType: Type) {
        val fallbackText = app.getString(R.string.note)
        val (title, url) = (noteTitle.ifEmpty { fallbackText }) to noteId.createNoteUrl(noteType)
        val range = selectedLinkRange
        val currentBody = _bodyValue.value
        if (range != null && range.min < range.max) {
            val start = range.min
            val end = range.max
            val builder = AnnotatedString.Builder(currentBody.text)
            currentBody.annotatedString.spanStyles.forEach { sRange ->
                builder.addStyle(sRange.item, sRange.start, sRange.end)
            }
            currentBody.annotatedString
                .getStringAnnotations("URL", 0, currentBody.text.length)
                .filterNot { it.start == start && it.end == end }
                .forEach { uRange ->
                    builder.addStringAnnotation("URL", uRange.item, uRange.start, uRange.end)
                }
            builder.addStringAnnotation("URL", url, start, end)
            builder.addStyle(
                SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                start,
                end,
            )
            val newAnnotatedString = builder.toAnnotatedString()
            val newBody =
                TextFieldValue(
                    annotatedString = newAnnotatedString,
                    selection = currentBody.selection,
                )
            applyBodyChange(newBody)
        } else {
            val textToAppend = title
            val start = currentBody.text.length
            val end = start + textToAppend.length
            val builder = AnnotatedString.Builder(currentBody.annotatedString)
            builder.append(textToAppend)
            builder.addStringAnnotation("URL", url, start, end)
            builder.addStyle(
                SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                start,
                end,
            )
            val newAnnotatedString = builder.toAnnotatedString()
            val newBody =
                TextFieldValue(
                    annotatedString = newAnnotatedString,
                    selection = TextRange(newAnnotatedString.text.length),
                )
            applyBodyChange(newBody)
        }
        selectedLinkRange = null
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
            emitSideEffect(EditNoteSideEffect.NavigateBack)
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
            emitSideEffect(EditNoteSideEffect.RequestNotificationPermission(showRationale = false))
        } else {
            isPinnedToStatus = false
            _isPinnedToStatus.value = false
            emitSideEffect(EditNoteSideEffect.RefreshStatusBarPin(getBaseNote(), false))
        }
    }

    override fun recordAudio() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (
            ContextCompat.checkSelfPermission(app, permission) != PackageManager.PERMISSION_GRANTED
        ) {
            emitSideEffect(
                EditNoteSideEffect.RequestAudioPermission(permission, showRationale = false)
            )
        } else {
            if (audioRoot != null) {
                emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.RecordAudio))
            } else {
                emitSideEffect(EditNoteSideEffect.ShowToast(R.string.insert_an_sd_card_audio))
            }
        }
    }

    override fun addImages() {
        if (imageRoot != null) {
            emitSideEffect(EditNoteSideEffect.LaunchAddImages(allowMultiple = true))
        } else {
            emitSideEffect(EditNoteSideEffect.ShowToast(R.string.insert_an_sd_card_images))
        }
    }

    override fun attachFiles() {
        if (filesRoot != null) {
            emitSideEffect(EditNoteSideEffect.LaunchAttachFiles(allowMultiple = true))
        } else {
            emitSideEffect(EditNoteSideEffect.ShowToast(R.string.insert_an_sd_card_files))
        }
    }

    override fun addNoteLink() {
        selectedLinkRange = null
        emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.PickNote(excludeNoteId = id)))
    }

    override fun updateNoteLink(range: TextRange) {
        selectedLinkRange = range
        emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.PickNote(excludeNoteId = id)))
    }

    override fun viewImage(image: FileAttachment) {
        val position = images.value.indexOf(image)
        emitSideEffect(
            EditNoteSideEffect.Navigate(
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
            emitSideEffect(EditNoteSideEffect.ViewAttachmentFile(uri, fileAttachment.mimeType))
        }
    }

    override fun playAudio(audio: Audio) {
        emitSideEffect(
            EditNoteSideEffect.Navigate(
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
                    emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.NoteReminders(noteId)))
                }
            }
            EditAction.LABELS -> {
                emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.SelectLabels(labels)))
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
                            onColorSelected = { onAction(EditNoteAction.ColorSelected(it)) },
                            onColorChanged = { oldC, newC ->
                                onAction(EditNoteAction.ColorChanged(oldC, newC))
                            },
                            onColorDeleted = { delC, newC ->
                                onAction(EditNoteAction.ColorDeleted(delC, newC))
                            },
                            onSetDefaultColor = { onAction(EditNoteAction.SetDefaultColor(it)) },
                        )
                }
            }
            EditAction.DUPLICATE -> {
                viewModelScope.launch {
                    saveNoteAndBroadcast()
                    val duplicateId = duplicateNote(getBaseNote())
                    emitSideEffect(EditNoteSideEffect.NavigateBack)
                    emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.EditNote(duplicateId)))
                }
            }
            EditAction.EXPORT -> {
                activeBottomSheet.value = EditorBottomSheetState.Export { mimeType ->
                    onAction(EditNoteAction.ExportConfirmed(mimeType))
                }
            }
            EditAction.SHARE -> {
                emitSideEffect(EditNoteSideEffect.ShareNote(getBaseNote()))
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
                    convertTo(Type.LIST)
                    isInitialized = false
                    val convertedId = id
                    id = 0L
                    _noteId.value = 0L
                    emitSideEffect(EditNoteSideEffect.NavigateBack)
                    emitSideEffect(EditNoteSideEffect.Navigate(NotallyNavKey.EditList(convertedId)))
                }
            }
            EditAction.DELETE_FOREVER -> {
                activeDialog.value =
                    EditorDialogState.DeleteForeverConfirm(
                        onConfirm = { onAction(EditNoteAction.DeleteForeverConfirmed) }
                    )
            }
        }
    }

    fun exportNoteToFile(fileUri: Uri, note: BaseNote, snackbarView: View) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log("EditNoteViewModel", throwable = throwable)
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
                                    app.log("EditNoteViewModel", stackTrace = message as String?)
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

    override fun handleListAction(action: EditListAction) {}
}

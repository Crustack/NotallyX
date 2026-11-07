package com.philkes.notallyx.feature.editor

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.getFileName
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.NoteBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.computeBodyDelta
import com.philkes.notallyx.feature.editor.screens.SearchState
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTextPlainViewModel(
    app: Application,
    preferences: NotallyXPreferences,
) : NoteModel(app, preferences) {

    private val changeHistory = ChangeHistory()

    private val _title = MutableStateFlow("")
    private val _bodyValue = MutableStateFlow(TextFieldValue())
    private val _fileUri = MutableStateFlow<Uri?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _searchState = MutableStateFlow(SearchState())
    private val _dialogState = MutableStateFlow<EditTextPlainDialogState?>(null)

    private val _sideEffects = Channel<EditTextPlainSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<EditTextPlainSideEffect> = _sideEffects.receiveAsFlow()

    private data class FileData(
        val title: String,
        val body: TextFieldValue,
        val uri: Uri?,
        val loading: Boolean,
    )

    val uiState: StateFlow<EditTextPlainUiState> =
        combine(
                combine(_title, _bodyValue, _fileUri, _isLoading) { title, body, uri, loading ->
                    FileData(title, body, uri, loading)
                },
                _searchState,
                _dialogState,
                changeHistory.canUndo,
                changeHistory.canRedo,
            ) { fileData, search, dialog, canUndo, canRedo ->
                EditTextPlainUiState(
                    title = fileData.title,
                    bodyValue = fileData.body,
                    fileUri = fileData.uri,
                    isLoading = fileData.loading,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    color = color,
                    searchState = search,
                    activeDialog = dialog,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = EditTextPlainUiState(isLoading = true),
            )

    fun onAction(action: EditTextPlainAction) {
        when (action) {
            is EditTextPlainAction.Init -> init(action.fileUriString)
            is EditTextPlainAction.TitleChanged -> {
                _title.value = action.title
                title = action.title
            }
            is EditTextPlainAction.BodyChanged -> applyBodyChange(action.bodyValue)
            is EditTextPlainAction.SearchQueryChanged -> onSearchQueryChange(action.query)
            is EditTextPlainAction.NextSearchResult -> onNextSearchResult()
            is EditTextPlainAction.PrevSearchResult -> onPrevSearchResult()
            is EditTextPlainAction.CloseSearch -> endSearch()
            is EditTextPlainAction.StartSearch -> {
                _searchState.update { it.copy(isActive = true) }
            }
            is EditTextPlainAction.Undo -> changeHistory.undo()
            is EditTextPlainAction.Redo -> changeHistory.redo()
            is EditTextPlainAction.Save -> {
                _fileUri.value?.let { saveToUri(it) }
            }
            is EditTextPlainAction.SaveToUri -> saveToUri(action.uri)
            is EditTextPlainAction.RequestSaveAs -> {
                val name = _title.value.ifEmpty { "note.txt" }
                viewModelScope.launch {
                    _sideEffects.send(EditTextPlainSideEffect.LaunchSaveAs(name))
                }
            }
            is EditTextPlainAction.ConvertToNote -> convertToTextNote()
            is EditTextPlainAction.DismissDialog -> {
                _dialogState.value = null
            }
            is EditTextPlainAction.BackClicked -> {
                viewModelScope.launch { _sideEffects.send(EditTextPlainSideEffect.NavigateBack) }
            }
        }
    }

    private fun init(fileUriString: String?) {
        viewModelScope.launch {
            type = Type.NOTE
            setState(0L, createInDb = false)

            val uri = fileUriString?.toUri()
            _fileUri.value = uri

            if (uri != null) {
                val fileName = app.getFileName(uri) ?: ""
                _title.value = fileName
                title = fileName

                _isLoading.value = true
                val text =
                    withContext(Dispatchers.IO) {
                        try {
                            val inputStream =
                                if (
                                    uri.scheme == ContentResolver.SCHEME_FILE ||
                                        uri.scheme == "file"
                                ) {
                                    uri.path?.let { File(it).inputStream() }
                                } else {
                                    app.contentResolver.openInputStream(uri)
                                }
                            inputStream?.use { stream -> stream.bufferedReader().readText() } ?: ""
                        } catch (e: Exception) {
                            app.log("EditTextPlainViewModel", "Failed to read file", throwable = e)
                            ""
                        }
                    }
                val newBody = TextFieldValue(AnnotatedString(text), TextRange(text.length))
                _bodyValue.value = newBody
                body = newBody
                _isLoading.value = false
            } else {
                _isLoading.value = false
            }
        }
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
                    },
                )
            if (delta != null) {
                pushBodyChange(delta)
            }
        }
        _bodyValue.value = newBody
        body = newBody
        if (_searchState.value.isActive) {
            updateSearchResults(_searchState.value.query)
        }
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
        val text = _bodyValue.value.text
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

    private fun saveToUri(uri: Uri) {
        val textToSave = _bodyValue.value.text
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputStream =
                    if (uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme == "file") {
                        uri.path?.let { File(it).outputStream() }
                    } else {
                        app.contentResolver.openOutputStream(uri)
                    } ?: throw IOException("Failed to open output stream for URI: $uri")
                outputStream.use { it.write(textToSave.toByteArray()) }

                _fileUri.value = uri
                val fileName = app.getFileName(uri)
                if (!fileName.isNullOrEmpty()) {
                    _title.value = fileName
                    title = fileName
                }
                _sideEffects.send(EditTextPlainSideEffect.ShowToast(R.string.saved_to_device))
            } catch (e: Exception) {
                _dialogState.value =
                    EditTextPlainDialogState.Error(
                        titleResId = R.string.something_went_wrong,
                        message = e.message,
                    )
            }
        }
    }

    private fun convertToTextNote() {
        viewModelScope.launch {
            title = _title.value
            body = _bodyValue.value
            type = Type.NOTE
            val noteId = saveNote(checkBackupOnSave = false)
            _sideEffects.send(EditTextPlainSideEffect.NavigateToEditNote(noteId))
        }
    }
}

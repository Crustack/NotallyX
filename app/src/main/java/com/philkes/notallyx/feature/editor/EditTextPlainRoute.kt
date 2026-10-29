package com.philkes.notallyx.feature.editor

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.getFileName
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.NoteBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.computeBodyDelta
import com.philkes.notallyx.feature.editor.screens.EditChangeHandlers
import com.philkes.notallyx.feature.editor.screens.EditClickHandlers
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import com.philkes.notallyx.feature.editor.screens.EditorDialogs
import com.philkes.notallyx.feature.editor.screens.SearchState
import com.philkes.notallyx.navigation.LocalNavigator
import com.philkes.notallyx.navigation.NotallyNavKey
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditTextPlainRoute(
    key: NotallyNavKey.EditTextPlain,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    notallyModel: EditActivityViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyModel.preferences,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    val changeHistory = remember { ChangeHistory() }

    var originalFileUri by remember { mutableStateOf(key.fileUri?.let { Uri.parse(it) }) }
    val titleState = remember { mutableStateOf("") }
    val bodyValueState = remember { mutableStateOf(TextFieldValue()) }
    val colorIntState = remember { mutableIntStateOf(context.extractColor(notallyModel.color)) }

    val isSearchActiveState = remember { mutableStateOf(false) }
    val searchQueryState = remember { mutableStateOf("") }
    val searchResultPosState = remember { mutableIntStateOf(0) }
    val searchResultsCountState = remember { mutableIntStateOf(0) }

    fun updateSearchResults(query: String) {
        if (query.isEmpty()) {
            searchResultsCountState.intValue = 0
            searchResultPosState.intValue = 0
            return
        }
        val text = bodyValueState.value.text
        val occurrences = text.findAllOccurrences(query)
        searchResultsCountState.intValue = occurrences.size
        if (occurrences.isNotEmpty()) {
            if (searchResultPosState.intValue >= occurrences.size) {
                searchResultPosState.intValue = occurrences.size - 1
            }
        } else {
            searchResultPosState.intValue = 0
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQueryState.value = query
        updateSearchResults(query)
    }

    fun onNextSearchResult() {
        val total = searchResultsCountState.intValue
        if (total > 0) {
            searchResultPosState.intValue = (searchResultPosState.intValue + 1) % total
        }
    }

    fun onPrevSearchResult() {
        val total = searchResultsCountState.intValue
        if (total > 0) {
            val current = searchResultPosState.intValue
            searchResultPosState.intValue = if (current > 0) current - 1 else total - 1
        }
    }

    fun endSearch() {
        isSearchActiveState.value = false
        searchQueryState.value = ""
        searchResultsCountState.intValue = 0
        searchResultPosState.intValue = 0
    }

    fun pushBodyChange(change: NoteBodyDeltaChange) {
        val top = changeHistory.peek()
        if (
            !changeHistory.canRedo.value && (top is NoteBodyDeltaChange) && top.tryMergeWith(change)
        ) {
            // Merged into top change
        } else {
            changeHistory.push(change)
        }
    }

    fun applyBodyChange(newBody: TextFieldValue) {
        val oldBody = bodyValueState.value
        if (newBody.annotatedString != oldBody.annotatedString) {
            val delta =
                computeBodyDelta(
                    oldValue = oldBody,
                    newValue = newBody,
                    notallyModel = notallyModel,
                    onBodyUpdated = { restored ->
                        bodyValueState.value = restored
                        if (isSearchActiveState.value) {
                            updateSearchResults(searchQueryState.value)
                        }
                    },
                )
            if (delta != null) {
                pushBodyChange(delta)
            }
        }
        bodyValueState.value = newBody
        notallyModel.body = newBody
    }

    fun saveToUri(uri: Uri) {
        val textToSave = bodyValueState.value.text
        notallyModel.viewModelScope.launch(Dispatchers.IO) {
            try {
                val outputStream =
                    if (uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme == "file") {
                        uri.path?.let { File(it).outputStream() }
                    } else {
                        context.contentResolver.openOutputStream(uri)
                    } ?: throw IOException("Failed to open output stream for URI: $uri")
                outputStream.use { it.write(textToSave.toByteArray()) }
                withContext(Dispatchers.Main) {
                    originalFileUri = uri
                    val fileName = context.getFileName(uri)
                    if (!fileName.isNullOrEmpty()) {
                        titleState.value = fileName
                        notallyModel.title = fileName
                    }
                    context.showToast(R.string.saved_to_device)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    notallyModel.activeDialog.value =
                        EditorDialogState.Error(
                            titleResId = R.string.something_went_wrong,
                            message = e.message,
                        )
                }
            }
        }
    }

    fun convertToTextNote() {
        coroutineScope.launch {
            notallyModel.title = titleState.value
            notallyModel.body = bodyValueState.value
            notallyModel.type = Type.NOTE
            val id = notallyModel.saveNote(checkBackupOnSave = false)
            navigator?.let { nav ->
                nav.navigate(NotallyNavKey.EditNote(noteId = id))
                nav.currentStack.remove(key)
            }
        }
    }

    val saveAsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri -> saveToUri(uri) }
            }
        }

    BackHandler { onBackClick() }

    LaunchedEffect(key.fileUri) {
        notallyModel.type = Type.NOTE
        notallyModel.changeUiState(UiState.Loading)
        notallyModel.setState(0L, createInDb = false)

        val uri = key.fileUri?.let { Uri.parse(it) }
        originalFileUri = uri

        if (uri != null) {
            val fileName = context.getFileName(uri) ?: ""
            titleState.value = fileName
            notallyModel.title = fileName

            val text =
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream =
                            if (uri.scheme == ContentResolver.SCHEME_FILE || uri.scheme == "file") {
                                uri.path?.let { File(it).inputStream() }
                            } else {
                                context.contentResolver.openInputStream(uri)
                            }
                        inputStream?.use { stream -> stream.bufferedReader().readText() } ?: ""
                    } catch (e: Exception) {
                        context.log("EditTextPlainRoute", "Failed to read file", throwable = e)
                        ""
                    }
                }
            val body = TextFieldValue(AnnotatedString(text), TextRange(text.length))
            bodyValueState.value = body
            notallyModel.body = body
        }
        notallyModel.changeUiState(UiState.Success)
    }

    val canUndoState = changeHistory.canUndo.collectAsStateWithLifecycle(false)
    val canRedoState = changeHistory.canRedo.collectAsStateWithLifecycle(false)
    val searchState =
        SearchState(
            isActive = isSearchActiveState.value,
            query = searchQueryState.value,
            resultsCount = searchResultsCountState.intValue,
            resultPos = searchResultPosState.intValue,
        )

    val noteState =
        EditNoteState(
            title = titleState.value,
            bodyValue = bodyValueState.value,
            createdTimestamp = notallyModel.timestamp,
            modifiedTimestamp = notallyModel.modifiedTimestamp,
        )

    EditNoteScreen(
        noteState = noteState,
        searchState = searchState,
        backgroundColor = Color(colorIntState.intValue),
        canUndo = canUndoState.value,
        canRedo = canRedoState.value,
        changeHandlers =
            object : EditChangeHandlers {
                override fun onTitleChange(title: String) {
                    titleState.value = title
                    notallyModel.title = title
                }

                override fun onBodyValueChange(bodyValue: TextFieldValue) {
                    applyBodyChange(bodyValue)
                    if (isSearchActiveState.value) {
                        updateSearchResults(searchQueryState.value)
                    }
                }

                override fun onSearchQueryChange(query: String) {
                    onSearchQueryChange(query)
                }
            },
        clickHandlers =
            object : EditClickHandlers {
                override fun onBackClick() {
                    onBackClick()
                }

                override fun onMoreOptionsClick() {}

                override fun onUndoClick() {
                    changeHistory.undo()
                }

                override fun onRedoClick() {
                    changeHistory.redo()
                }

                override fun onNextSearchResult() {
                    onNextSearchResult()
                }

                override fun onPrevSearchResult() {
                    onPrevSearchResult()
                }

                override fun onCloseSearch() {
                    endSearch()
                }
            },
        bottomLeftActions = {
            IconButton(onClick = { convertToTextNote() }) {
                Icon(
                    painter = painterResource(id = R.drawable.convert_to_text),
                    contentDescription = stringResource(id = R.string.convert_to_text_note),
                )
            }
            IconButton(onClick = { originalFileUri?.let { saveToUri(it) } }) {
                Icon(
                    painter = painterResource(id = R.drawable.save),
                    contentDescription = stringResource(id = R.string.save),
                )
            }
            IconButton(
                onClick = {
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "text/*"
                            val name = titleState.value.ifEmpty { "note.txt" }
                            putExtra(Intent.EXTRA_TITLE, name)
                        }
                    saveAsLauncher.launch(intent)
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.save_as),
                    contentDescription = stringResource(id = R.string.save_to_device),
                )
            }
        },
        modifier = modifier,
    )

    EditorDialogs(notallyModel)
}

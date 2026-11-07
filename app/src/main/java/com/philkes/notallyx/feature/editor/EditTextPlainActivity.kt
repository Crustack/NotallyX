package com.philkes.notallyx.feature.editor

// import com.philkes.notallyx.collectIn
// import com.philkes.notallyx.showAndFocus
// import com.philkes.notallyx.showToast
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.getFileName
import com.philkes.notallyx.core.ui.extensions.collectIn
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.screens.EditChangeHandlers
import com.philkes.notallyx.feature.editor.screens.EditClickHandlers
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditTextPlainActivity : EditActivity(Type.NOTE) {

    private var searchResultIndices: List<Pair<Int, Int>>? = null
    private lateinit var saveAsActivityResultLauncher: ActivityResultLauncher<Intent>
    private val originalFileUri = MutableStateFlow<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.action == Intent.ACTION_VIEW && intent.data != null) {
            originalFileUri.value = intent.data
        }
        setupActivityResultLaunchers()
        super.onCreate(savedInstanceState)
    }

    private fun setupActivityResultLaunchers() {
        saveAsActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri -> saveToUri(uri) }
                }
            }
    }

    override suspend fun checkSave() {}

    override fun configureUI() {
        originalFileUri.collectIn(this) { uri ->
            uri?.let {
                val fileName = getFileName(it)
                if (!fileName.isNullOrEmpty()) {
                    notallyModel.title = fileName
                    titleState.value = fileName
                }
            }
        }
    }

    override fun toggleCanEdit(mode: NoteViewMode) {}

    @SuppressLint("MissingSuperCall") override fun onSaveInstanceState(outState: Bundle) {}

    override suspend fun highlightSearchResults(search: String): Int {
        if (search.isEmpty()) {
            return 0
        }
        val text = notallyModel.body.toString()
        val occurrences = withContext(Dispatchers.Default) { text.findAllOccurrences(search) }
        searchResultIndices = occurrences
        return searchResultIndices!!.size
    }

    override fun selectSearchResult(resultPos: Int) {}

    @Composable
    override fun RenderScreen() {
        val canUndoState = changeHistory.canUndo.collectAsStateWithLifecycle(false)
        val canRedoState = changeHistory.canRedo.collectAsStateWithLifecycle(false)
        val searchState = this.searchState

        val noteState =
            EditNoteState(
                title = titleState.value,
                bodyText = bodyTextState.value,
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

                    override fun onBodyTextChange(bodyText: String) {
                        bodyTextState.value = bodyText
                        notallyModel.body.replace(0, notallyModel.body.length, bodyText)
                        if (isSearchActiveState.value) {
                            updateSearchResults(searchQueryState.value)
                        }
                    }

                    override fun onSearchQueryChange(query: String) {
                        this@EditTextPlainActivity.onSearchQueryChange(query)
                    }
                },
            clickHandlers =
                object : EditClickHandlers {
                    override fun onBackClick() {
                        finish()
                    }

                    override fun onMoreOptionsClick() {
                        openMoreOptionsBottomSheet()
                    }

                    override fun onUndoClick() {
                        changeHistory.undo()
                    }

                    override fun onRedoClick() {
                        changeHistory.redo()
                    }

                    override fun onNextSearchResult() {
                        this@EditTextPlainActivity.onNextSearchResult()
                    }

                    override fun onPrevSearchResult() {
                        this@EditTextPlainActivity.onPrevSearchResult()
                    }

                    override fun onCloseSearch() {
                        this@EditTextPlainActivity.endSearch()
                    }
                },
            bottomLeftActions = {
                IconButton(onClick = { convertToTextNote() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.convert_to_text),
                        contentDescription = stringResource(id = R.string.convert_to_text_note),
                    )
                }
                IconButton(onClick = { originalFileUri.value?.let { saveToUri(it) } }) {
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
                            }
                        saveAsActivityResultLauncher.launch(intent)
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.save_as),
                        contentDescription = stringResource(id = R.string.save_to_device),
                    )
                }
            },
        )
    }

    override fun resetToolbars() {}

    override fun setStateFromModel(savedInstanceState: Bundle?) {
        super.setStateFromModel(savedInstanceState)
        bodyTextState.value = notallyModel.body.toString()
        titleState.value = notallyModel.title
    }

    override fun initBottomMenu() {}

    override fun setupAdditionalListeners() {}

    override fun initChangeHistory() {
        changeHistory = ChangeHistory()
    }

    private fun saveToUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val outputStream =
                        contentResolver.openOutputStream(uri)
                            ?: throw IOException("Failed to open output stream for URI: $uri")
                    outputStream.use { it.write(notallyModel.body.toString().toByteArray()) }
                }
                originalFileUri.value = uri
                showToast(R.string.saved_to_device)
            } catch (e: Exception) {
                notallyModel.activeDialog.value =
                    EditorDialogState.Error(
                        titleResId = R.string.something_went_wrong,
                        message = e.message,
                    )
            }
        }
    }

    private fun convertToTextNote() {
        lifecycleScope.launch {
            val id = notallyModel.saveNote(checkBackupOnSave = false)
            val intent =
                Intent(this@EditTextPlainActivity, EditNoteActivity::class.java).apply {
                    putExtra(EXTRA_SELECTED_BASE_NOTE, id)
                }
            startActivity(intent)
            finish()
        }
    }

    companion object {
        private const val EXTRA_SELECTION_START = "notallyx.intent.extra.EXTRA_SELECTION_START"
        private const val EXTRA_SELECTION_END = "notallyx.intent.extra.EXTRA_SELECTION_END"
    }
}

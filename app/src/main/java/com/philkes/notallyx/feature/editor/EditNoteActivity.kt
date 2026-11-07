package com.philkes.notallyx.feature.editor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.copyToClipBoard
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.openNote
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.ui.extensions.addOrUpdateLink
import com.philkes.notallyx.core.ui.extensions.clearFormatting
import com.philkes.notallyx.core.ui.extensions.preserveStylesFrom
import com.philkes.notallyx.core.ui.extensions.removeLink
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.extensions.toggleBold
import com.philkes.notallyx.core.ui.extensions.toggleItalic
import com.philkes.notallyx.core.ui.extensions.toggleMonospace
import com.philkes.notallyx.core.ui.extensions.toggleStrikethrough
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.getNoteIdFromUrl
import com.philkes.notallyx.data.model.extensions.getNoteTypeFromUrl
import com.philkes.notallyx.data.model.extensions.isNoteUrl
import com.philkes.notallyx.feature.editor.formatting.getFormattingState
import com.philkes.notallyx.feature.editor.screens.EditChangeHandlers
import com.philkes.notallyx.feature.editor.screens.EditClickHandlers
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditNoteActivity : EditActivity(Type.NOTE) {

    private var searchResultIndices: List<Pair<Int, Int>>? = null
    private val bodyValueState = mutableStateOf(TextFieldValue())

    override fun configureUI() {}

    override suspend fun highlightSearchResults(search: String): Int {
        if (search.isEmpty()) {
            return 0
        }
        val text = notallyModel.body.text
        val occurrences = withContext(Dispatchers.Default) { text.findAllOccurrences(search) }
        searchResultIndices = occurrences
        return searchResultIndices!!.size
    }

    override fun selectSearchResult(resultPos: Int) {}

    private fun applyOrToggleBold() {
        val updated = bodyValueState.value.toggleBold()
        bodyValueState.value = updated
        notallyModel.body = updated
    }

    private fun applyOrToggleItalic() {
        val updated = bodyValueState.value.toggleItalic()
        bodyValueState.value = updated
        notallyModel.body = updated
    }

    private fun applyOrToggleStrikethrough() {
        val updated = bodyValueState.value.toggleStrikethrough()
        bodyValueState.value = updated
        notallyModel.body = updated
    }

    private fun applyOrToggleMonospace() {
        val updated = bodyValueState.value.toggleMonospace()
        bodyValueState.value = updated
        notallyModel.body = updated
    }

    private fun clearFormatting() {
        val updated = bodyValueState.value.clearFormatting()
        bodyValueState.value = updated
        notallyModel.body = updated
    }

    @Composable
    override fun RenderScreen() {
        val imagesState = notallyModel.images.collectAsStateWithLifecycle(emptyList())
        val filesState = notallyModel.files.collectAsStateWithLifecycle(emptyList())
        val audiosState = notallyModel.audios.collectAsStateWithLifecycle(emptyList())
        val remindersState = notallyModel.reminders.collectAsStateWithLifecycle(emptyList())

        val topActionsState = remember {
            mutableStateOf(preferences.getSafeEditNoteActivityTopActions())
        }
        val bottomActionState = remember {
            mutableStateOf(preferences.editNoteActivityBottomAction.value)
        }
        val canUndoState = changeHistory.canUndo.collectAsStateWithLifecycle(false)
        val canRedoState = changeHistory.canRedo.collectAsStateWithLifecycle(false)
        val viewModeState = notallyModel.viewMode.collectAsStateWithLifecycle(NoteViewMode.EDIT)

        val noteState =
            EditNoteState(
                title = titleState.value,
                bodyValue = bodyValueState.value,
                createdTimestamp = notallyModel.timestamp,
                modifiedTimestamp = notallyModel.modifiedTimestamp,
                images = imagesState.value,
                mediaRoot = notallyModel.imageRoot,
                files = filesState.value,
                audios = audiosState.value,
                labels = labelsState.value,
                reminders = remindersState.value,
                pinned = notallyModel.pinned,
                viewMode = viewModeState.value,
                folder = notallyModel.folder,
                type = notallyModel.type,
                isPinnedToStatus = notallyModel.isPinnedToStatus,
            )

        val preferencesState =
            EditNotePreferences(
                notesSortBy = preferences.notesSorting.value.sortedBy,
                dateFormat = preferences.dateFormatNoteView.value,
                timeFormat = preferences.timeFormatNoteView.value,
                topActions = topActionsState.value,
                bottomAction = bottomActionState.value,
            )

        val searchState = this.searchState

        val formattingState =
            remember(bodyValueState.value) {
                val selection = bodyValueState.value.selection
                bodyValueState.value.annotatedString.getFormattingState(
                    selection.min,
                    selection.max,
                )
            }

        EditNoteScreen(
            noteState = noteState,
            preferences = preferencesState,
            searchState = searchState,
            backgroundColor = Color(colorIntState.intValue),
            canUndo = canUndoState.value,
            canRedo = canRedoState.value,
            isFormattingMenuVisible = isFormattingMenuVisible.value,
            formattingState = formattingState,
            changeHandlers =
                object : EditChangeHandlers {
                    override fun onTitleChange(title: String) {
                        titleState.value = title
                        notallyModel.title = title
                    }

                    override fun onBodyValueChange(bodyValue: TextFieldValue) {
                        val newText = bodyValue.text
                        val oldText = notallyModel.body.text
                        if (newText != oldText) {
                            if (isSearchActiveState.value) {
                                updateSearchResults(searchQueryState.value)
                            }
                        }
                        if (bodyValue.selection.min != bodyValue.selection.max) {
                            isFormattingMenuVisible.value = true
                        }
                        val updatedValue = bodyValue.preserveStylesFrom(bodyValueState.value)
                        bodyValueState.value = updatedValue
                        notallyModel.body = updatedValue
                    }

                    override fun onSearchQueryChange(query: String) {
                        this@EditNoteActivity.onSearchQueryChange(query)
                    }
                },
            clickHandlers =
                object : EditClickHandlers {
                    override fun onBackClick() {
                        finish()
                    }

                    override fun onAddItemClick() {
                        notallyModel.activeBottomSheet.value = EditorBottomSheetState.AddNote
                    }

                    override fun onMoreOptionsClick() {
                        openMoreOptionsBottomSheet()
                    }

                    override fun onFormatClick() {
                        isFormattingMenuVisible.value = !isFormattingMenuVisible.value
                    }

                    override fun onCloseFormattingClick() {
                        isFormattingMenuVisible.value = false
                    }

                    override fun onActionClick(action: EditAction) {
                        actionHandler.handleAction(action)
                    }

                    override fun onActionLongClick(action: EditAction) {
                        openActionSelectionBottomSheet(
                            action = action,
                            onUpdateTopActions = { topActionsState.value = it },
                            onUpdateBottomAction = { bottomActionState.value = it },
                        )
                    }

                    override fun onUndoClick() {
                        if (changeHistory.canUndo.value) {
                            changeHistory.undo()
                        }
                    }

                    override fun onRedoClick() {
                        if (changeHistory.canRedo.value) {
                            changeHistory.redo()
                        }
                    }

                    override fun onBoldClick() {
                        applyOrToggleBold()
                    }

                    override fun onItalicClick() {
                        applyOrToggleItalic()
                    }

                    override fun onStrikethroughClick() {
                        applyOrToggleStrikethrough()
                    }

                    override fun onMonospaceClick() {
                        applyOrToggleMonospace()
                    }

                    override fun onLinkClick() {
                        showLinkDialog()
                    }

                    override fun onLinkClicked(
                        url: String,
                        displayText: String,
                        range: TextRange,
                    ) {
                        showLinkOptionsDialog(url, displayText, range)
                    }

                    override fun onClearFormattingClick() {
                        clearFormatting()
                    }

                    override fun onImageClick(image: FileAttachment) {
                        actionHandler.viewImage(image)
                    }

                    override fun onFileClick(file: FileAttachment) {
                        actionHandler.viewFile(file)
                    }

                    override fun onAudioClick(audio: Audio) {
                        actionHandler.playAudio(audio)
                    }

                    override fun onNextSearchResult() {
                        this@EditNoteActivity.onNextSearchResult()
                    }

                    override fun onPrevSearchResult() {
                        this@EditNoteActivity.onPrevSearchResult()
                    }

                    override fun onCloseSearch() {
                        this@EditNoteActivity.endSearch()
                    }
                },
        )
    }

    override fun setStateFromModel(savedInstanceState: Bundle?) {
        super.setStateFromModel(savedInstanceState)
        bodyValueState.value = notallyModel.body
    }

    private fun showLinkDialog() {
        val selection = bodyValueState.value.selection
        val min = selection.min
        val max = selection.max
        val hasSelection = min < max
        val selectedText = if (hasSelection) bodyValueState.value.text.substring(min, max) else ""
        val existingUrl =
            if (hasSelection) {
                bodyValueState.value.annotatedString
                    .getStringAnnotations("URL", min, max)
                    .firstOrNull()
                    ?.item ?: ""
            } else ""

        val isNoteUrl = existingUrl.isNoteUrl()
        val range = if (hasSelection) TextRange(min, max) else null
        notallyModel.activeDialog.value =
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
        notallyModel.activeDialog.value =
            EditorDialogState.LinkOptions(
                url = url,
                displayText = displayText,
                isNoteUrl = isNoteUrl,
                onOpen = { openLink(url) },
                onEdit = {
                    notallyModel.activeDialog.value =
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

    private fun applyLink(url: String, displayText: String, range: TextRange? = null) {
        val updated = bodyValueState.value.addOrUpdateLink(url, displayText, range)
        bodyValueState.value = updated
        notallyModel.body = updated
    }

    private fun removeLink(range: TextRange) {
        val updated = bodyValueState.value.removeLink(range)
        bodyValueState.value = updated
        notallyModel.body = updated
    }

    private fun openLink(url: String) {
        if (url.isNoteUrl()) {
            openNote(url.getNoteIdFromUrl(), url.getNoteTypeFromUrl())
        } else {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).wrapWithChooser(this)
            try {
                startActivity(intent)
            } catch (exception: Exception) {
                showToast(R.string.cant_open_link)
            }
        }
    }

    private fun copyLink(url: String) {
        copyToClipBoard(url)
        showToast(R.string.copied_link)
    }

    companion object {
        private const val EXTRA_SELECTION_START = "notallyx.intent.extra.EXTRA_SELECTION_START"
        private const val EXTRA_SELECTION_END = "notallyx.intent.extra.EXTRA_SELECTION_END"
    }
}

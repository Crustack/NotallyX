package com.philkes.notallyx.presentation.activity.note

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.getNoteIdFromUrl
import com.philkes.notallyx.data.model.getNoteTypeFromUrl
import com.philkes.notallyx.data.model.isNoteUrl
import com.philkes.notallyx.presentation.createBoldSpan
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.note.action.AddNoteBottomSheet
import com.philkes.notallyx.presentation.view.screens.EditChangeHandlers
import com.philkes.notallyx.presentation.view.screens.EditClickHandlers
import com.philkes.notallyx.presentation.view.screens.EditNotePreferences
import com.philkes.notallyx.presentation.view.screens.EditNoteScreen
import com.philkes.notallyx.presentation.view.screens.EditNoteState
import com.philkes.notallyx.presentation.viewmodel.preference.EditAction
import com.philkes.notallyx.utils.copyToClipBoard
import com.philkes.notallyx.utils.findAllOccurrences
import com.philkes.notallyx.utils.openNote
import com.philkes.notallyx.utils.wrapWithChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditNoteActivity : EditActivity(Type.NOTE) {

    private var searchResultIndices: List<Pair<Int, Int>>? = null

    override fun configureUI() {}

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
                bodyText = bodyTextState.value,
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

        EditNoteScreen(
            noteState = noteState,
            preferences = preferencesState,
            backgroundColor = Color(colorIntState.intValue),
            canUndo = canUndoState.value,
            canRedo = canRedoState.value,
            isFormattingMenuVisible = isFormattingMenuVisible.value,
            changeHandlers =
                object : EditChangeHandlers {
                    override fun onTitleChange(title: String) {
                        titleState.value = title
                        notallyModel.title = title
                    }

                    override fun onBodyTextChange(bodyText: String) {
                        bodyTextState.value = bodyText
                        notallyModel.body.replace(0, notallyModel.body.length, bodyText)
                    }
                },
            clickHandlers =
                object : EditClickHandlers {
                    override fun onBackClick() {
                        finish()
                    }

                    override fun onAddItemClick() {
                        AddNoteBottomSheet(actionHandler, colorInt)
                            .show(supportFragmentManager, AddNoteBottomSheet.TAG)
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
                        notallyModel.body.setSpan(
                            createBoldSpan(),
                            0,
                            notallyModel.body.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        bodyTextState.value = notallyModel.body.toString()
                    }

                    override fun onItalicClick() {
                        notallyModel.body.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            0,
                            notallyModel.body.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        bodyTextState.value = notallyModel.body.toString()
                    }

                    override fun onStrikethroughClick() {
                        notallyModel.body.setSpan(
                            StrikethroughSpan(),
                            0,
                            notallyModel.body.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        bodyTextState.value = notallyModel.body.toString()
                    }

                    override fun onMonospaceClick() {
                        notallyModel.body.setSpan(
                            TypefaceSpan("monospace"),
                            0,
                            notallyModel.body.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                        bodyTextState.value = notallyModel.body.toString()
                    }

                    override fun onClearFormattingClick() {
                        notallyModel.body.clearSpans()
                        bodyTextState.value = notallyModel.body.toString()
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
                },
        )
    }

    override fun setStateFromModel(savedInstanceState: Bundle?) {
        super.setStateFromModel(savedInstanceState)
        bodyTextState.value = notallyModel.body.toString()
    }

    private fun openLink(span: URLSpan) {
        span.url?.let {
            if (it.isNoteUrl()) {
                span.navigateToNote()
            } else {
                openLink(span.url)
            }
        }
    }

    private fun copyLink(span: URLSpan) {
        copyToClipBoard(span.url)
        showToast(R.string.copied_link)
    }

    private fun openLink(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri).wrapWithChooser(this)
        try {
            startActivity(intent)
        } catch (exception: Exception) {
            showToast(R.string.cant_open_link)
        }
    }

    private fun URLSpan.navigateToNote() {
        openNote(this.url.getNoteIdFromUrl(), this.url.getNoteTypeFromUrl())
    }

    companion object {
        private const val EXTRA_SELECTION_START = "notallyx.intent.extra.EXTRA_SELECTION_START"
        private const val EXTRA_SELECTION_END = "notallyx.intent.extra.EXTRA_SELECTION_END"
    }
}

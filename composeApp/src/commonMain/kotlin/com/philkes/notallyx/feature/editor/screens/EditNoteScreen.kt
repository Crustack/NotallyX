package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.core.ui.components.parseNoteColor
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.edit
import com.philkes.notallyx.feature.editor.EditNoteAction
import com.philkes.notallyx.feature.editor.EditNoteUiState
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.formatting.TextFormattingState
import com.philkes.notallyx.note
import com.philkes.notallyx.text_format
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditNoteScreen(
    noteState: EditNoteState,
    modifier: Modifier = Modifier,
    preferences: EditNotePreferences = EditNotePreferences(),
    searchState: SearchState = SearchState(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    isFormattingMenuVisible: Boolean = false,
    formattingState: TextFormattingState = TextFormattingState(),
    changeHandlers: EditChangeHandlers = object : EditChangeHandlers {},
    clickHandlers: EditClickHandlers = object : EditClickHandlers {},
    bottomLeftActions: @Composable RowScope.() -> Unit = {
        IconButton(onClick = { clickHandlers.onFormatClick() }) {
            Icon(
                painter = painterResource(Res.drawable.text_format),
                contentDescription = stringResource(Res.string.edit),
            )
        }
    },
) {
    val scrollState = rememberScrollState()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(searchState.isActive, searchState.query, searchState.resultPos) {
        if (
            searchState.isActive && searchState.query.isNotEmpty() && searchState.resultsCount > 0
        ) {
            val occurrences = noteState.bodyValue.text.findAllOccurrences(searchState.query)
            val selectedRange = occurrences.getOrNull(searchState.resultPos)
            if (selectedRange != null && textLayoutResult != null) {
                val line = textLayoutResult!!.getLineForOffset(selectedRange.first)
                val lineTop = textLayoutResult!!.getLineTop(line).toInt()
                scrollState.animateScrollTo(lineTop)
            }
        }
    }

    val keyboardShortcutModifier = Modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)) {
            when (event.key) {
                Key.B -> {
                    clickHandlers.onBoldClick()
                    true
                }
                Key.I -> {
                    clickHandlers.onItalicClick()
                    true
                }
                Key.U -> {
                    clickHandlers.onStrikethroughClick()
                    true
                }
                Key.S -> {
                    clickHandlers.onBackClick()
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }

    EditScreen(
        noteState = noteState,
        modifier = modifier.then(keyboardShortcutModifier),
        preferences = preferences,
        searchState = searchState,
        scrollState = scrollState,
        backgroundColor = backgroundColor,
        canUndo = canUndo,
        canRedo = canRedo,
        isFormattingMenuVisible = isFormattingMenuVisible,
        formattingState = formattingState,
        changeHandlers = changeHandlers,
        clickHandlers = clickHandlers,
        bottomLeftActions = bottomLeftActions,
        content = {
            val bodyDesc = stringResource(Res.string.note)
            val visualTransformation =
                remember(searchState.isActive, searchState.query, searchState.resultPos) {
                    if (searchState.isActive && searchState.query.isNotEmpty()) {
                        SearchHighlightVisualTransformation(
                            query = searchState.query,
                            selectedIndex = searchState.resultPos,
                        )
                    } else {
                        VisualTransformation.None
                    }
                }

            val bodyValue = noteState.bodyValue
            val currentBodyValue by rememberUpdatedState(bodyValue)
            val currentClickHandlers by rememberUpdatedState(clickHandlers)
            BasicTextField(
                value = bodyValue,
                onValueChange = { changeHandlers.onBodyValueChange(it) },
                onTextLayout = { textLayoutResult = it },
                visualTransformation = visualTransformation,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .testTag(TestTags.NOTE_BODY_INPUT)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown(pass = PointerEventPass.Initial)
                                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                if (
                                    up != null &&
                                        (up.position - down.position).getDistance() <
                                            viewConfiguration.touchSlop
                                ) {
                                    textLayoutResult?.let { layout ->
                                        val offset = layout.getOffsetForPosition(up.position)
                                        val annotated = currentBodyValue.annotatedString
                                        val urlAnnotations =
                                            annotated.getStringAnnotations(
                                                "URL",
                                                0,
                                                annotated.length,
                                            )
                                        val link = urlAnnotations.firstOrNull {
                                            offset in it.start until it.end ||
                                                (offset == it.end &&
                                                    offset > it.start &&
                                                    layout
                                                        .getBoundingBox(it.end - 1)
                                                        .contains(up.position))
                                        }
                                        if (link != null) {
                                            val displayText =
                                                annotated.text.substring(link.start, link.end)
                                            currentClickHandlers.onLinkClicked(
                                                link.item,
                                                displayText,
                                                TextRange(link.start, link.end),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        .semantics { contentDescription = bodyDesc },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
                cursorBrush = SolidColor(LocalContentColor.current),
                decorationBox = { innerTextField ->
                    Box {
                        if (bodyValue.text.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.note),
                                style = MaterialTheme.typography.bodyLarge,
                                color = LocalContentColor.current.copy(alpha = 0.6f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        },
    )
}

@Composable
fun EditNoteScreen(
    state: EditNoteUiState,
    onAction: (EditNoteAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noteState =
        remember(state) {
            EditNoteState(
                title = state.title,
                bodyValue = state.bodyValue,
                createdTimestamp = state.createdTimestamp,
                modifiedTimestamp = state.modifiedTimestamp,
                images = state.images,
                mediaRoot = state.mediaRoot,
                files = state.files,
                audios = state.audios,
                labels = state.labels,
                reminders = state.reminders,
                pinned = state.pinned,
                viewMode = state.viewMode,
                folder = state.folder,
                type = state.type,
                isPinnedToStatus = state.isPinnedToStatus,
            )
        }

    val changeHandlers =
        remember(onAction) {
            object : EditChangeHandlers {
                override fun onTitleChange(title: String) {
                    onAction(EditNoteAction.TitleChanged(title))
                }

                override fun onBodyValueChange(bodyValue: TextFieldValue) {
                    onAction(EditNoteAction.BodyChanged(bodyValue))
                }

                override fun onSearchQueryChange(query: String) {
                    onAction(EditNoteAction.SearchQueryChanged(query))
                }
            }
        }

    val clickHandlers =
        remember(onAction) {
            object : EditClickHandlers {
                override fun onBackClick() {
                    onAction(EditNoteAction.BackClicked)
                }

                override fun onAddItemClick() {
                    onAction(EditNoteAction.AddItemClicked)
                }

                override fun onMoreOptionsClick() {
                    onAction(EditNoteAction.MoreOptionsClicked)
                }

                override fun onFormatClick() {
                    onAction(EditNoteAction.ToggleFormattingMenu)
                }

                override fun onCloseFormattingClick() {
                    onAction(EditNoteAction.CloseFormattingMenu)
                }

                override fun onActionClick(action: EditAction) {
                    onAction(EditNoteAction.ActionClicked(action))
                }

                override fun onActionLongClick(action: EditAction) {
                    onAction(EditNoteAction.ActionLongClicked(action))
                }

                override fun onUndoClick() {
                    onAction(EditNoteAction.Undo)
                }

                override fun onRedoClick() {
                    onAction(EditNoteAction.Redo)
                }

                override fun onBoldClick() {
                    onAction(EditNoteAction.ToggleBold)
                }

                override fun onItalicClick() {
                    onAction(EditNoteAction.ToggleItalic)
                }

                override fun onStrikethroughClick() {
                    onAction(EditNoteAction.ToggleStrikethrough)
                }

                override fun onMonospaceClick() {
                    onAction(EditNoteAction.ToggleMonospace)
                }

                override fun onLinkClick() {
                    onAction(EditNoteAction.RequestLinkDialog)
                }

                override fun onLinkClicked(url: String, displayText: String, range: TextRange) {
                    onAction(EditNoteAction.LinkClicked(url, displayText, range))
                }

                override fun onClearFormattingClick() {
                    onAction(EditNoteAction.ClearFormatting)
                }

                override fun onImageClick(image: FileAttachment) {
                    onAction(EditNoteAction.ImageClicked(image))
                }

                override fun onFileClick(file: FileAttachment) {
                    onAction(EditNoteAction.FileClicked(file))
                }

                override fun onFileDelete(file: FileAttachment) {
                    onAction(EditNoteAction.FileDelete(file))
                }

                override fun onAudioClick(audio: Audio) {
                    onAction(EditNoteAction.AudioClicked(audio))
                }

                override fun onReminderClick(reminder: Reminder) {
                    onAction(EditNoteAction.ReminderClicked(reminder))
                }

                override fun onNextSearchResult() {
                    onAction(EditNoteAction.NextSearchResult)
                }

                override fun onPrevSearchResult() {
                    onAction(EditNoteAction.PrevSearchResult)
                }

                override fun onCloseSearch() {
                    onAction(EditNoteAction.CloseSearch)
                }
            }
        }

    val actionHandler =
        remember(onAction) {
            object : NoteActionHandler {
                override fun pinToStatus() {
                    onAction(EditNoteAction.PinToStatus)
                }

                override fun recordAudio() {
                    onAction(EditNoteAction.RecordAudio)
                }

                override fun startRecordAudioActivity() {
                    onAction(EditNoteAction.RecordAudio)
                }

                override fun addImages() {
                    onAction(EditNoteAction.AddImages)
                }

                override fun attachFiles() {
                    onAction(EditNoteAction.AttachFiles)
                }

                override fun addNoteLink() {
                    onAction(EditNoteAction.AddNoteLink)
                }

                override fun updateNoteLink(range: TextRange) {
                    onAction(EditNoteAction.UpdateNoteLink(range))
                }

                override fun viewImage(image: FileAttachment) {
                    onAction(EditNoteAction.ImageClicked(image))
                }

                override fun viewFile(fileAttachment: FileAttachment) {
                    onAction(EditNoteAction.FileClicked(fileAttachment))
                }

                override fun playAudio(audio: Audio) {
                    onAction(EditNoteAction.AudioClicked(audio))
                }

                override fun handleAction(action: EditAction) {
                    onAction(EditNoteAction.ActionClicked(action))
                }

                override fun handleListAction(action: EditListAction) {}
            }
        }

    val backgroundColor = parseNoteColor(state.color)

    val keyboardShortcutModifier = Modifier.onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)) {
            when (event.key) {
                Key.B -> {
                    clickHandlers.onBoldClick()
                    true
                }
                Key.I -> {
                    clickHandlers.onItalicClick()
                    true
                }
                Key.U -> {
                    clickHandlers.onStrikethroughClick()
                    true
                }
                Key.S -> {
                    clickHandlers.onBackClick()
                    true
                }
                else -> false
            }
        } else {
            false
        }
    }

    Box(modifier = modifier.fillMaxSize().then(keyboardShortcutModifier)) {
        EditNoteScreen(
            noteState = noteState,
            modifier = Modifier.fillMaxSize(),
            preferences = state.preferences,
            searchState = state.searchState,
            backgroundColor = backgroundColor,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            isFormattingMenuVisible = state.isFormattingMenuVisible,
            formattingState = state.formattingState,
            changeHandlers = changeHandlers,
            clickHandlers = clickHandlers,
        )

        EditorDialogs(
            activeDialog = state.activeDialog,
            onDismissRequest = { onAction(EditNoteAction.DismissDialog) },
        )

        EditorBottomSheets(
            activeBottomSheet = state.activeBottomSheet,
            onDismissRequest = { sheet -> onAction(EditNoteAction.DismissBottomSheet(sheet)) },
            actionHandler = actionHandler,
            pinned = state.pinned,
            viewMode = state.viewMode,
            folder = state.folder,
            type = state.type,
            isPinnedToStatus = state.isPinnedToStatus,
            backgroundColor = if (state.color != BaseNote.COLOR_DEFAULT) backgroundColor else null,
        )
    }
}

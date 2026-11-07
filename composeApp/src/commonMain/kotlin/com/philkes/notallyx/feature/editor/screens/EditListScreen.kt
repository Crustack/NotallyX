package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.add
import com.philkes.notallyx.add_item
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.ui.components.parseNoteColor
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.delete
import com.philkes.notallyx.drag_handle
import com.philkes.notallyx.feature.editor.EditListAction
import com.philkes.notallyx.feature.editor.EditListUiState
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.listitem.dragContainer
import com.philkes.notallyx.feature.editor.listitem.draggableModifier
import com.philkes.notallyx.feature.editor.listitem.rememberListItemDragDropState
import com.philkes.notallyx.item
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditListScreen(
    noteState: EditNoteState,
    modifier: Modifier = Modifier,
    preferences: EditNotePreferences = EditNotePreferences(),
    searchState: SearchState = SearchState(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    changeHandlers: EditChangeHandlers = object : EditChangeHandlers {},
    clickHandlers: EditClickHandlers = object : EditClickHandlers {},
    focusFirstItem: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    var itemToFocusId by remember { mutableStateOf<Int?>(null) }
    var hasFocusedOnce by rememberSaveable { mutableStateOf(false) }
    var shouldFocusNewItem by remember { mutableStateOf(false) }

    val wrappedClickHandlers =
        remember(clickHandlers) {
            object : EditClickHandlers by clickHandlers {
                override fun onAddItemClick() {
                    clickHandlers.onAddItemClick()
                    shouldFocusNewItem = true
                }
            }
        }

    LaunchedEffect(focusFirstItem, noteState.items) {
        if (focusFirstItem && !hasFocusedOnce && noteState.items.isNotEmpty()) {
            hasFocusedOnce = true
            itemToFocusId = noteState.items.first().id
        }
    }

    LaunchedEffect(shouldFocusNewItem, noteState.items) {
        if (shouldFocusNewItem && noteState.items.isNotEmpty()) {
            shouldFocusNewItem = false
            itemToFocusId = noteState.items.maxByOrNull { it.id }?.id
        }
    }

    LaunchedEffect(itemToFocusId) {
        val id = itemToFocusId
        if (id != null) {
            withFrameNanos {} // Ensures layout pass is ready before requesting focus
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Drag and Drop
    val listState = rememberLazyListState()

    LaunchedEffect(searchState.isActive, searchState.query, searchState.resultPos) {
        if (
            searchState.isActive && searchState.query.isNotEmpty() && searchState.resultsCount > 0
        ) {
            var remaining = searchState.resultPos
            var matchedIndex = -1
            for ((index, item) in noteState.items.withIndex()) {
                val count = item.body.findAllOccurrences(searchState.query).size
                if (remaining < count) {
                    matchedIndex = index
                    break
                }
                remaining -= count
            }
            if (matchedIndex != -1) {
                listState.animateScrollToItem(matchedIndex)
            }
        }
    }

    val dragDropState =
        rememberListItemDragDropState(listState, noteState.items) { fromIndex, toIndex, originalItem
            ->
            changeHandlers.onItemMove(fromIndex, toIndex, originalItem)
        }

    EditScreen(
        noteState = noteState.copy(type = Type.LIST),
        modifier = modifier,
        preferences = preferences,
        searchState = searchState,
        backgroundColor = backgroundColor,
        canUndo = canUndo,
        canRedo = canRedo,
        changeHandlers = changeHandlers,
        clickHandlers = wrappedClickHandlers,
        bottomLeftActions = {},
        content = {
            LazyColumn(
                modifier = Modifier.dragContainer(dragDropState),
                state = listState,
            ) {
                // Checklist Unchecked Items
                itemsIndexed(
                    items = noteState.items,
                    key = { index, item -> if (item.id >= 0) item.id else "item_$index" },
                ) { index, item ->
                    val onSwipeRight = { changeHandlers.onItemSwipeRight(item) }
                    val onSwipeLeft = { changeHandlers.onItemSwipeLeft(item) }
                    val swipeModifier =
                        if (dragDropState.draggingItemIndex == null) {
                            createSwipeModifier(onSwipeRight, onSwipeLeft)
                        } else Modifier
                    val draggingIndex = dragDropState.draggingItemIndex
                    val isItemInDragGroup = dragDropState.isItemOrChildDragging(index)
                    val isNextInGroup =
                        index + 1 < noteState.items.size &&
                            dragDropState.isItemOrChildDragging(index + 1)
                    val dragState =
                        when {
                            !isItemInDragGroup -> DragState.NONE
                            index == draggingIndex -> DragState.PARENT
                            isItemInDragGroup && !isNextInGroup -> DragState.LAST_CHILD
                            else -> DragState.CHILD
                        }

                    val itemVisualTransformation =
                        if (searchState.isActive && searchState.query.isNotEmpty()) {
                            createSearchHighlightListItemTransformation(
                                noteState,
                                index,
                                searchState,
                                item,
                            )
                        } else {
                            VisualTransformation.None
                        }

                    ChecklistItemRow(
                        modifier =
                            Modifier.then(swipeModifier)
                                .then(draggableModifier(dragDropState, index)),
                        value = item.body,
                        onValueChange = { newText ->
                            changeHandlers.onItemTextChange(
                                item,
                                newText,
                            )
                        },
                        isChecked = item.checked,
                        isChild = item.isChild,
                        visualTransformation = itemVisualTransformation,
                        onCheckedChange = { checked ->
                            changeHandlers.onItemCheckedChange(item, checked)
                        },
                        onEnterKey = { wrappedClickHandlers.onAddItemClick() },
                        onBackspaceOnEmpty = {
                            val prevItem = if (index > 0) noteState.items[index - 1] else null
                            if (prevItem != null) {
                                itemToFocusId = prevItem.id
                            }
                            changeHandlers.onItemDelete(item)
                        },
                        onDelete = { changeHandlers.onItemDelete(item) },
                        focusRequester = if (item.id == itemToFocusId) focusRequester else null,
                        dragState = dragState,
                        backgroundColor = backgroundColor,
                        dragHandleContentDescription = stringResource(Res.string.drag_handle),
                        placeholderText = stringResource(Res.string.item),
                        deleteContentDescription = stringResource(Res.string.delete),
                    )
                }
            }

            // Add Item Button
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .testTag(TestTags.CHECKLIST_ADD_BUTTON)
                        .clickable { wrappedClickHandlers.onAddItemClick() }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.add),
                    contentDescription = null,
                    tint = LocalContentColor.current.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = stringResource(Res.string.add_item),
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalContentColor.current.copy(alpha = 0.7f),
                )
            }

            // Checklist Checked Items
            if (noteState.checkedItems.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    itemsIndexed(
                        items = noteState.checkedItems,
                        key = { index, item ->
                            if (item.id >= 0) item.id else "checked_item_$index"
                        },
                    ) { _, item ->
                        ChecklistItemRow(
                            value = item.body,
                            onValueChange = { newText ->
                                changeHandlers.onItemTextChange(item, newText)
                            },
                            isChecked = item.checked,
                            onCheckedChange = { checked ->
                                changeHandlers.onItemCheckedChange(item, checked)
                            },
                            isChild = item.isChild,
                            focusRequester = if (item.id == itemToFocusId) focusRequester else null,
                            onEnterKey = { wrappedClickHandlers.onAddItemClick() },
                            onBackspaceOnEmpty = { changeHandlers.onItemDelete(item) },
                            onDelete = { changeHandlers.onItemDelete(item) },
                            backgroundColor = backgroundColor,
                            dragHandleContentDescription = stringResource(Res.string.drag_handle),
                            placeholderText = stringResource(Res.string.item),
                            deleteContentDescription = stringResource(Res.string.delete),
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun EditListScreen(
    state: EditListUiState,
    onAction: (EditListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noteState =
        remember(state) {
            EditNoteState(
                title = state.title,
                items = state.items,
                checkedItems = state.checkedItems,
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
                    onAction(EditListAction.TitleChanged(title))
                }

                override fun onItemTextChange(item: ListItem, text: String) {
                    onAction(EditListAction.ItemTextChanged(item, text))
                }

                override fun onItemCheckedChange(item: ListItem, checked: Boolean) {
                    onAction(EditListAction.ItemChecked(item, checked))
                }

                override fun onItemDelete(item: ListItem) {
                    onAction(EditListAction.ItemDeleted(item))
                }

                override fun onItemSwipeRight(item: ListItem) {
                    onAction(EditListAction.ItemSwipeRight(item))
                }

                override fun onItemSwipeLeft(item: ListItem) {
                    onAction(EditListAction.ItemSwipeLeft(item))
                }

                override fun onItemMove(fromIndex: Int, toIndex: Int, originalItem: ListItem?) {
                    onAction(EditListAction.ItemMove(fromIndex, toIndex, originalItem))
                }

                override fun onSearchQueryChange(query: String) {
                    onAction(EditListAction.SearchQueryChanged(query))
                }
            }
        }

    val clickHandlers =
        remember(onAction) {
            object : EditClickHandlers {
                override fun onBackClick() {
                    onAction(EditListAction.BackClicked)
                }

                override fun onAddItemClick() {
                    onAction(EditListAction.AddItemClicked)
                }

                override fun onMoreOptionsClick() {
                    onAction(EditListAction.MoreOptionsClicked)
                }

                override fun onActionClick(action: EditAction) {
                    onAction(EditListAction.ActionClicked(action))
                }

                override fun onActionLongClick(action: EditAction) {
                    onAction(EditListAction.ActionLongClicked(action))
                }

                override fun onUndoClick() {
                    onAction(EditListAction.Undo)
                }

                override fun onRedoClick() {
                    onAction(EditListAction.Redo)
                }

                override fun onImageClick(image: FileAttachment) {
                    onAction(EditListAction.ImageClicked(image))
                }

                override fun onFileClick(file: FileAttachment) {
                    onAction(EditListAction.FileClicked(file))
                }

                override fun onFileDelete(file: FileAttachment) {
                    onAction(EditListAction.FileDelete(file))
                }

                override fun onAudioClick(audio: Audio) {
                    onAction(EditListAction.AudioClicked(audio))
                }

                override fun onReminderClick(reminder: Reminder) {
                    onAction(EditListAction.ReminderClicked(reminder))
                }

                override fun onNextSearchResult() {
                    onAction(EditListAction.NextSearchResult)
                }

                override fun onPrevSearchResult() {
                    onAction(EditListAction.PrevSearchResult)
                }

                override fun onCloseSearch() {
                    onAction(EditListAction.CloseSearch)
                }
            }
        }

    val actionHandler =
        remember(onAction) {
            object : NoteActionHandler {
                override fun pinToStatus() {
                    onAction(EditListAction.PinToStatus)
                }

                override fun recordAudio() {
                    onAction(EditListAction.RecordAudio)
                }

                override fun startRecordAudioActivity() {
                    onAction(EditListAction.RecordAudio)
                }

                override fun addImages() {
                    onAction(EditListAction.AddImages)
                }

                override fun attachFiles() {
                    onAction(EditListAction.AttachFiles)
                }

                override fun addNoteLink() {
                    onAction(EditListAction.AddNoteLink)
                }

                override fun updateNoteLink(range: TextRange) {}

                override fun viewImage(image: FileAttachment) {
                    onAction(EditListAction.ImageClicked(image))
                }

                override fun viewFile(fileAttachment: FileAttachment) {
                    onAction(EditListAction.FileClicked(fileAttachment))
                }

                override fun playAudio(audio: Audio) {
                    onAction(EditListAction.AudioClicked(audio))
                }

                override fun handleAction(action: EditAction) {
                    onAction(EditListAction.ActionClicked(action))
                }

                override fun handleListAction(
                    action: com.philkes.notallyx.core.preference.EditListAction
                ) {
                    onAction(EditListAction.ListActionClicked(action))
                }
            }
        }

    val backgroundColor = parseNoteColor(state.color)

    Box(modifier = modifier.fillMaxSize()) {
        EditListScreen(
            noteState = noteState,
            modifier = Modifier.fillMaxSize(),
            preferences = state.preferences,
            searchState = state.searchState,
            backgroundColor = backgroundColor,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            changeHandlers = changeHandlers,
            clickHandlers = clickHandlers,
            focusFirstItem = state.focusFirstItem,
        )

        EditorDialogs(
            activeDialog = state.activeDialog,
            onDismissRequest = { onAction(EditListAction.DismissDialog) },
        )

        EditorBottomSheets(
            activeBottomSheet = state.activeBottomSheet,
            onDismissRequest = { sheet -> onAction(EditListAction.DismissBottomSheet(sheet)) },
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

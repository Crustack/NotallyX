package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.LabelChip
import com.philkes.notallyx.core.ui.components.ReminderChip
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.formatting.TextFormattingBar
import com.philkes.notallyx.feature.editor.formatting.TextFormattingBarActions
import com.philkes.notallyx.feature.editor.formatting.TextFormattingState
import com.philkes.notallyx.feature.media.audio.AudioItemRow
import java.io.File
import java.util.Date
import kotlinx.coroutines.android.awaitFrame

data class EditNoteState(
    val title: String = "",
    val bodyText: String = "",
    val items: List<ListItem> = emptyList(),
    val checkedItems: List<ListItem> = emptyList(),
    val createdTimestamp: Long? = null,
    val modifiedTimestamp: Long? = null,
    val images: List<FileAttachment> = emptyList(),
    val mediaRoot: File? = null,
    val files: List<FileAttachment> = emptyList(),
    val audios: List<Audio> = emptyList(),
    val labels: List<String> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val pinned: Boolean = false,
    val viewMode: NoteViewMode = NoteViewMode.EDIT,
    val folder: Folder = Folder.NOTES,
    val type: Type = Type.NOTE,
    val isPinnedToStatus: Boolean = false,
)

data class EditNotePreferences(
    val notesSortBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    val dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormat: TimeFormat = TimeFormat.NONE,
    val topActions: List<EditAction> =
        listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
    val bottomAction: EditAction = EditAction.TOGGLE_VIEW_MODE,
)

internal interface EditChangeHandlers {
    fun onTitleChange(title: String) {}

    fun onBodyTextChange(bodyText: String) {}

    fun onItemTextChange(item: ListItem, text: String) {}

    fun onItemCheckedChange(item: ListItem, checked: Boolean) {}

    fun onItemDelete(item: ListItem) {}

    fun onItemSwipeRight(item: ListItem) {}

    fun onItemSwipeLeft(item: ListItem) {}
}

internal typealias EditNoteChangeHandlers = EditChangeHandlers

internal interface EditClickHandlers {
    fun onBackClick() {}

    fun onAddItemClick() {}

    fun onMoreOptionsClick() {}

    fun onFormatClick() {}

    fun onCloseFormattingClick() {}

    fun onActionClick(action: EditAction) {}

    fun onActionLongClick(action: EditAction) {}

    fun onUndoClick() {}

    fun onRedoClick() {}

    fun onBoldClick() {}

    fun onItalicClick() {}

    fun onStrikethroughClick() {}

    fun onMonospaceClick() {}

    fun onLinkClick() {}

    fun onClearFormattingClick() {}

    fun onImageClick(image: FileAttachment) {}

    fun onFileClick(file: FileAttachment) {}

    fun onAudioClick(audio: Audio) {}

    fun onReminderClick(reminder: Reminder) {}
}

internal typealias EditNoteClickHandlers = EditClickHandlers

@Composable
private fun ActionIconButton(
    iconResId: Int,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val clickModifier =
        if (onLongClick != null) {
            Modifier.combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick,
            )
        } else {
            Modifier.clickable(enabled = enabled, onClick = onClick)
        }

    Box(
        modifier = modifier.size(48.dp).clip(CircleShape).then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint =
                if (enabled) LocalContentColor.current
                else LocalContentColor.current.copy(alpha = 0.38f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionIconButtonPreview() {
    NotallyXTheme {
        ActionIconButton(
            iconResId = R.drawable.search,
            contentDescription = "Search",
            onClick = {},
        )
    }
}

@Composable
private fun FileAttachmentPreviewChip(
    fileName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = fileName,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditScreenImageSection(
    images: List<FileAttachment>,
    mediaRoot: File?,
    onImageClick: (FileAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        images.forEach { imageAttachment ->
            val imageFile =
                remember(imageAttachment, mediaRoot) {
                    if (mediaRoot != null && imageAttachment.localName.isNotEmpty()) {
                        File(mediaRoot, imageAttachment.localName)
                    } else null
                }

            if (imageFile?.exists() == true) {
                val bitmap =
                    remember(imageFile) {
                        try {
                            android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath)
                        } catch (e: Exception) {
                            null
                        }
                    }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = imageAttachment.originalName,
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { onImageClick(imageAttachment) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditScreenAudioSection(
    audios: List<Audio>,
    onAudioClick: (Audio) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.audio_recordings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        audios.forEach { audio ->
            AudioItemRow(
                dateText = audio.name,
                lengthText = "${(audio.duration ?: 0L) / 1000}s",
                onClick = { onAudioClick(audio) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditScreen(
    noteState: EditNoteState,
    modifier: Modifier = Modifier,
    preferences: EditNotePreferences = EditNotePreferences(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    lazyListState: LazyListState = rememberLazyListState(),
    isFormattingMenuVisible: Boolean = false,
    formattingState: TextFormattingState = TextFormattingState(),
    changeHandlers: EditChangeHandlers = object : EditChangeHandlers {},
    clickHandlers: EditClickHandlers = object : EditClickHandlers {},
    bottomLeftActions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { clickHandlers.onBackClick() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = stringResource(id = R.string.back),
                        )
                    }
                },
                actions = {
                    preferences.topActions.forEach { action ->
                        val (titleResId, iconResId) =
                            action.getTitleAndIcon(
                                noteState.pinned,
                                noteState.viewMode,
                                noteState.folder,
                                noteState.type,
                                noteState.isPinnedToStatus,
                            )
                        ActionIconButton(
                            iconResId = iconResId,
                            contentDescription = stringResource(id = titleResId),
                            onClick = { clickHandlers.onActionClick(action) },
                            onLongClick = { clickHandlers.onActionLongClick(action) },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor),
            )
        },
        bottomBar = {
            if (isFormattingMenuVisible) {
                TextFormattingBar(
                    formattingState = formattingState,
                    backgroundColor = backgroundColor,
                    actions =
                        TextFormattingBarActions(
                            onBoldClick = { clickHandlers.onBoldClick() },
                            onItalicClick = { clickHandlers.onItalicClick() },
                            onStrikethroughClick = { clickHandlers.onStrikethroughClick() },
                            onMonospaceClick = { clickHandlers.onMonospaceClick() },
                            onLinkClick = { clickHandlers.onLinkClick() },
                            onClearFormattingClick = { clickHandlers.onClearFormattingClick() },
                            onCloseClick = { clickHandlers.onCloseFormattingClick() },
                        ),
                )
            } else {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(backgroundColor)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { clickHandlers.onAddItemClick() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.add),
                                contentDescription = stringResource(id = R.string.add_item),
                            )
                        }

                        bottomLeftActions()
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { clickHandlers.onUndoClick() },
                            enabled = canUndo,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.undo),
                                contentDescription = stringResource(id = R.string.undo),
                            )
                        }

                        IconButton(
                            onClick = { clickHandlers.onRedoClick() },
                            enabled = canRedo,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.redo),
                                contentDescription = stringResource(id = R.string.redo),
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (titleResId, iconResId) =
                            preferences.bottomAction.getTitleAndIcon(
                                noteState.pinned,
                                noteState.viewMode,
                                noteState.folder,
                                noteState.type,
                                noteState.isPinnedToStatus,
                            )
                        ActionIconButton(
                            iconResId = iconResId,
                            contentDescription = stringResource(id = titleResId),
                            onClick = { clickHandlers.onActionClick(preferences.bottomAction) },
                            onLongClick = {
                                clickHandlers.onActionLongClick(preferences.bottomAction)
                            },
                        )

                        IconButton(onClick = { clickHandlers.onMoreOptionsClick() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.more_vert),
                                contentDescription =
                                    stringResource(id = R.string.tap_for_more_options),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(bottom = 16.dp),
        ) {
            // Image Preview
            if (noteState.images.isNotEmpty()) {
                item {
                    EditScreenImageSection(
                        images = noteState.images,
                        mediaRoot = noteState.mediaRoot,
                        onImageClick = { clickHandlers.onImageClick(it) },
                    )
                }
            }

            // Title Field
            item {
                val titleDesc = stringResource(id = R.string.title)
                BasicTextField(
                    value = noteState.title,
                    onValueChange = { changeHandlers.onTitleChange(it) },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 2.dp)
                            .semantics { contentDescription = titleDesc },
                    textStyle =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteState.title.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        ),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            item {
                val prefixRes =
                    if (preferences.notesSortBy == NotesSortBy.CREATION_DATE) R.string.creation_date
                    else R.string.modified_date
                val prefixText = stringResource(id = prefixRes)

                val targetTimestamp =
                    when (preferences.notesSortBy) {
                        NotesSortBy.CREATION_DATE ->
                            noteState.createdTimestamp ?: noteState.modifiedTimestamp
                        else -> noteState.modifiedTimestamp ?: noteState.createdTimestamp
                    }
                val formattedDateText =
                    if (
                        targetTimestamp != null &&
                            (preferences.dateFormat != DateFormat.NONE ||
                                preferences.timeFormat != TimeFormat.NONE)
                    ) {
                        val formatted =
                            Date(targetTimestamp)
                                .format(preferences.dateFormat, preferences.timeFormat)
                        if (formatted.isNotEmpty()) "$prefixText $formatted" else ""
                    } else ""

                if (formattedDateText.isNotEmpty() || noteState.reminders.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (formattedDateText.isNotEmpty()) {
                            Text(
                                text = formattedDateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }

                        if (noteState.reminders.isNotEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                            ReminderChip(
                                reminders = noteState.reminders,
                                dateFormat = preferences.dateFormat,
                                timeFormat = preferences.timeFormat,
                                onClick = {
                                    clickHandlers.onReminderClick(noteState.reminders.first())
                                },
                            )
                        }
                    }
                }
            }

            // Files Preview
            if (noteState.files.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier =
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(noteState.files, key = { it.originalName }) { fileAttachment ->
                            FileAttachmentPreviewChip(
                                fileName = fileAttachment.originalName,
                                onClick = { clickHandlers.onFileClick(fileAttachment) },
                            )
                        }
                    }
                }
            }

            // Labels
            if (noteState.labels.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier =
                            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(noteState.labels, key = { it }) { labelName ->
                            LabelChip(text = labelName)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
            }

            content()

            // Audio Recordings Section
            if (noteState.audios.isNotEmpty()) {
                item {
                    EditScreenAudioSection(
                        audios = noteState.audios,
                        onAudioClick = { clickHandlers.onAudioClick(it) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun EditNoteScreen(
    noteState: EditNoteState,
    modifier: Modifier = Modifier,
    preferences: EditNotePreferences = EditNotePreferences(),
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
                painter = painterResource(id = R.drawable.text_format),
                contentDescription = stringResource(id = R.string.edit),
            )
        }
    },
) {
    EditScreen(
        noteState = noteState,
        modifier = modifier,
        preferences = preferences,
        backgroundColor = backgroundColor,
        canUndo = canUndo,
        canRedo = canRedo,
        isFormattingMenuVisible = isFormattingMenuVisible,
        formattingState = formattingState,
        changeHandlers = changeHandlers,
        clickHandlers = clickHandlers,
        bottomLeftActions = bottomLeftActions,
        content = {
            item {
                val bodyDesc = stringResource(id = R.string.note)
                BasicTextField(
                    value = noteState.bodyText,
                    onValueChange = { changeHandlers.onBodyTextChange(it) },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .semantics { contentDescription = bodyDesc },
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteState.bodyText.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.note),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color =
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        ),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
        },
    )
}

@Composable
internal fun EditListScreen(
    noteState: EditNoteState,
    modifier: Modifier = Modifier,
    preferences: EditNotePreferences = EditNotePreferences(),
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
            awaitFrame() // Ensures layout pass is ready before requesting focus
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    EditScreen(
        noteState = noteState.copy(type = Type.LIST),
        modifier = modifier,
        preferences = preferences,
        backgroundColor = backgroundColor,
        canUndo = canUndo,
        canRedo = canRedo,
        changeHandlers = changeHandlers,
        clickHandlers = wrappedClickHandlers,
        bottomLeftActions = {},
        content = {
            // Checklist Unchecked Items
            itemsIndexed(
                items = noteState.items,
                key = { _, item -> item.id },
            ) { index, item ->
                ChecklistItemRow(
                    value = item.body,
                    onValueChange = { newText -> changeHandlers.onItemTextChange(item, newText) },
                    isChecked = item.checked,
                    isChild = item.isChild,
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
                    onSwipeRight = { changeHandlers.onItemSwipeRight(item) },
                    onSwipeLeft = { changeHandlers.onItemSwipeLeft(item) },
                    focusRequester = if (item.id == itemToFocusId) focusRequester else null,
                )
            }

            // Add Item Button
            item {
                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable { wrappedClickHandlers.onAddItemClick() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.add_item),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Checklist Checked Items
            if (noteState.checkedItems.isNotEmpty()) {
                item {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
                items(
                    items = noteState.checkedItems,
                    key = { it.id },
                ) { item ->
                    ChecklistItemRow(
                        value = item.body,
                        onValueChange = { newText ->
                            changeHandlers.onItemTextChange(item, newText)
                        },
                        isChecked = item.checked,
                        isChild = item.isChild,
                        onCheckedChange = { checked ->
                            changeHandlers.onItemCheckedChange(item, checked)
                        },
                        onEnterKey = { wrappedClickHandlers.onAddItemClick() },
                        onBackspaceOnEmpty = { changeHandlers.onItemDelete(item) },
                        onDelete = { changeHandlers.onItemDelete(item) },
                        onSwipeRight = { changeHandlers.onItemSwipeRight(item) },
                        onSwipeLeft = { changeHandlers.onItemSwipeLeft(item) },
                        focusRequester = if (item.id == itemToFocusId) focusRequester else null,
                    )
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun EditNoteScreenPreview() {
    NotallyXTheme {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = "Project Planning Note",
                    bodyText =
                        "Here are the core tasks for the upcoming release:\n- Review design mockups\n- Finalize Jetpack Compose migration\n- Conduct QA testing",
                    createdTimestamp = System.currentTimeMillis() - 86400000L,
                    modifiedTimestamp = System.currentTimeMillis(),
                    images =
                        listOf(
                            FileAttachment(
                                localName = "sample_image.png",
                                originalName = "Sample Image.png",
                                mimeType = "image/png",
                            )
                        ),
                    files =
                        listOf(
                            FileAttachment(
                                localName = "spec.pdf",
                                originalName = "Project_Spec.pdf",
                                mimeType = "application/pdf",
                            ),
                            FileAttachment(
                                localName = "notes.txt",
                                originalName = "Meeting_Notes.txt",
                                mimeType = "text/plain",
                            ),
                        ),
                    audios =
                        listOf(
                            Audio(
                                name = "Voice Memo 01",
                                duration = 42000L,
                                timestamp = System.currentTimeMillis(),
                            )
                        ),
                    labels = listOf("Work", "Compose", "Important"),
                    reminders =
                        listOf(
                            Reminder(
                                id = 1L,
                                dateTime = Date(),
                                repetition = null,
                            )
                        ),
                    pinned = true,
                    type = Type.NOTE,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.AM_PM,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            canUndo = true,
            canRedo = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditListScreenPreview() {
    NotallyXTheme {
        EditListScreen(
            noteState =
                EditNoteState(
                    title = "Weekly Grocery List",
                    createdTimestamp = System.currentTimeMillis() - 86400000L,
                    modifiedTimestamp = System.currentTimeMillis(),
                    items =
                        listOf(
                            ListItem(
                                body = "Fresh Organic Apples",
                                checked = false,
                                isChild = false,
                                order = 1,
                                children = mutableListOf(),
                            ),
                            ListItem(
                                body = "Almond Milk (Unsweetened)",
                                checked = false,
                                isChild = false,
                                order = 2,
                                children = mutableListOf(),
                            ),
                            ListItem(
                                body = "Whole Grain Bread",
                                checked = false,
                                isChild = false,
                                order = 3,
                                children = mutableListOf(),
                            ),
                        ),
                    checkedItems =
                        listOf(
                            ListItem(
                                body = "Greek Yogurt",
                                checked = true,
                                isChild = false,
                                order = 4,
                                children = mutableListOf(),
                            ),
                            ListItem(
                                body = "Avocados",
                                checked = true,
                                isChild = false,
                                order = 5,
                                children = mutableListOf(),
                            ),
                        ),
                    images =
                        listOf(
                            FileAttachment(
                                localName = "recipe.jpg",
                                originalName = "Recipe_Idea.jpg",
                                mimeType = "image/jpeg",
                            )
                        ),
                    files =
                        listOf(
                            FileAttachment(
                                localName = "coupons.pdf",
                                originalName = "Store_Coupons.pdf",
                                mimeType = "application/pdf",
                            )
                        ),
                    audios =
                        listOf(
                            Audio(
                                name = "Shopping Voice Note",
                                duration = 18000L,
                                timestamp = System.currentTimeMillis(),
                            )
                        ),
                    labels = listOf("Shopping", "Personal"),
                    reminders =
                        listOf(
                            Reminder(
                                id = 2L,
                                dateTime = Date(),
                                repetition = null,
                            )
                        ),
                    pinned = false,
                    type = Type.LIST,
                ),
            preferences =
                EditNotePreferences(
                    notesSortBy = NotesSortBy.MODIFIED_DATE,
                    dateFormat = DateFormat.DD_MM_YY_GER,
                    timeFormat = TimeFormat.AM_PM,
                    topActions = listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN),
                    bottomAction = EditAction.TOGGLE_VIEW_MODE,
                ),
            canUndo = true,
            canRedo = true,
        )
    }
}

package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.isPreview
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.ImageCountChip
import com.philkes.notallyx.core.ui.components.LabelChip
import com.philkes.notallyx.core.ui.components.ReminderChip
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.theme.contentColorFor
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.feature.editor.EditListAction
import com.philkes.notallyx.feature.editor.EditListUiState
import com.philkes.notallyx.feature.editor.EditNoteAction
import com.philkes.notallyx.feature.editor.EditNoteUiState
import com.philkes.notallyx.feature.editor.NoteActionHandler
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.editor.formatting.TextFormattingBar
import com.philkes.notallyx.feature.editor.formatting.TextFormattingBarActions
import com.philkes.notallyx.feature.editor.formatting.TextFormattingState
import com.philkes.notallyx.feature.editor.listitem.dragContainer
import com.philkes.notallyx.feature.editor.listitem.draggableModifier
import com.philkes.notallyx.feature.editor.listitem.rememberListItemDragDropState
import com.philkes.notallyx.feature.media.audio.AudioItemRow
import com.philkes.notallyx.navigation.LocalBackButtonVisibility
import java.io.File
import java.util.Date
import kotlinx.coroutines.android.awaitFrame

data class EditNoteState(
    val title: String = "",
    val bodyValue: TextFieldValue = TextFieldValue(),
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

    fun onBodyValueChange(bodyValue: TextFieldValue) {}

    fun onItemTextChange(item: ListItem, text: String) {}

    fun onItemTextChange(item: ListItem, text: AnnotatedString) {
        onItemTextChange(item, text.text)
    }

    fun onItemCheckedChange(item: ListItem, checked: Boolean) {}

    fun onItemDelete(item: ListItem) {}

    fun onItemSwipeRight(item: ListItem) {}

    fun onItemSwipeLeft(item: ListItem) {}

    fun onItemMove(fromIndex: Int, toIndex: Int, originalItem: ListItem? = null) {}

    fun onSearchQueryChange(query: String) {}
}

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

    fun onLinkClicked(url: String, displayText: String, range: TextRange) {}

    fun onClearFormattingClick() {}

    fun onImageClick(image: FileAttachment) {}

    fun onFileClick(file: FileAttachment) {}

    fun onFileDelete(file: FileAttachment) {}

    fun onAudioClick(audio: Audio) {}

    fun onReminderClick(reminder: Reminder) = onActionClick(EditAction.REMINDERS)

    fun onNextSearchResult() {}

    fun onPrevSearchResult() {}

    fun onCloseSearch() {}
}

@Composable
private fun EditScreenImageSection(
    images: List<FileAttachment>,
    mediaRoot: File?,
    onImageClick: (FileAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { images.size })

    LaunchedEffect(images.size) {
        if (images.isNotEmpty() && pagerState.currentPage >= images.size) {
            pagerState.scrollToPage(images.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier.fillMaxWidth().height(200.dp).semantics {
                    contentDescription = "Image Pager"
                },
        ) { page ->
            AttachedImages(images, page, mediaRoot, onImageClick)
        }

        if (images.size > 1) {
            ImageCountChip(images, pagerState, onImageClick)
        }
    }
}

@Composable
private fun AttachedImages(
    images: List<FileAttachment>,
    page: Int,
    mediaRoot: File?,
    onImageClick: (FileAttachment) -> Unit,
) {
    val imageAttachment = images.getOrNull(page) ?: return
    val imageFile =
        remember(imageAttachment, mediaRoot) {
            if (mediaRoot != null && imageAttachment.localName.isNotEmpty()) {
                File(mediaRoot, imageAttachment.localName)
            } else null
        }

    if (isPreview()) {
        Image(
            painter = painterResource(id = R.drawable.cat),
            contentDescription = imageAttachment.originalName,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth().height(400.dp).clickable { onImageClick(imageAttachment) },
        )
        return
    }

    if (imageFile?.exists() == true) {
        SubcomposeAsyncImage(
            model = imageFile,
            contentDescription = imageAttachment.originalName,
            contentScale = ContentScale.Crop,
            modifier =
                Modifier.fillMaxWidth().height(400.dp).clickable { onImageClick(imageAttachment) },
            error = {
                ImagePreviewCard(
                    onClick = { onImageClick(imageAttachment) },
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                )
            },
        )
    } else {
        ImagePreviewCard(
            onClick = { onImageClick(imageAttachment) },
            modifier = Modifier.fillMaxWidth().height(250.dp),
        )
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
            color = LocalContentColor.current,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        audios.forEach { audio ->
            AudioItemRow(
                dateText = audio.name,
                lengthText = "${(audio.duration ?: 0L) / 1000}s",
                contentColor = LocalContentColor.current,
                lengthColor = LocalContentColor.current.copy(alpha = 0.7f),
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
    searchState: SearchState = SearchState(),
    scrollState: ScrollState = rememberScrollState(),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    isFormattingMenuVisible: Boolean = false,
    formattingState: TextFormattingState = TextFormattingState(),
    changeHandlers: EditChangeHandlers = object : EditChangeHandlers {},
    clickHandlers: EditClickHandlers = object : EditClickHandlers {},
    bottomLeftActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var fileToDelete by remember { mutableStateOf<FileAttachment?>(null) }

    val contentColor = contentColorFor(backgroundColor)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = backgroundColor,
        contentColor = contentColor,
        topBar = {
            if (searchState.isActive) {
                SearchTopAppBar(
                    searchState = searchState,
                    onQueryChange = { changeHandlers.onSearchQueryChange(it) },
                    onNextResult = { clickHandlers.onNextSearchResult() },
                    onPrevResult = { clickHandlers.onPrevSearchResult() },
                    onCloseSearch = { clickHandlers.onCloseSearch() },
                    backgroundColor = backgroundColor,
                )
            } else {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = { clickHandlers.onBackClick() }) {
                            Icon(
                                painter =
                                    if (LocalBackButtonVisibility.current)
                                        painterResource(id = R.drawable.arrow_back)
                                    else painterResource(id = R.drawable.close),
                                contentDescription =
                                    if (LocalBackButtonVisibility.current)
                                        stringResource(id = R.string.back)
                                    else stringResource(id = R.string.cancel),
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
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = backgroundColor,
                            navigationIconContentColor = contentColor,
                            actionIconContentColor = contentColor,
                            titleContentColor = contentColor,
                        ),
                )
            }
        },
        bottomBar = {
            if (isFormattingMenuVisible) {
                TextFormattingBar(
                    formattingState = formattingState,
                    backgroundColor = backgroundColor,
                    modifier = Modifier.padding(top = 8.dp),
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
        val columnModifier =
            if (noteState.type != Type.LIST) {
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(bottom = 16.dp)
            } else {
                Modifier.fillMaxSize().padding(innerPadding).padding(bottom = 16.dp)
            }

        Column(modifier = columnModifier) {
            // Image Preview
            if (noteState.images.isNotEmpty()) {
                EditScreenImageSection(
                    images = noteState.images,
                    mediaRoot = noteState.mediaRoot,
                    onImageClick = { clickHandlers.onImageClick(it) },
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                // Title Field
                val titleDesc = stringResource(id = R.string.title)
                BasicTextField(
                    value = noteState.title,
                    onValueChange = { changeHandlers.onTitleChange(it) },
                    modifier =
                        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 0.dp).semantics {
                            contentDescription = titleDesc
                        },
                    textStyle =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = LocalContentColor.current
                        ),
                    cursorBrush = SolidColor(LocalContentColor.current),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteState.title.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = LocalContentColor.current.copy(alpha = 0.6f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )

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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (formattedDateText.isNotEmpty()) {
                            Text(
                                text = formattedDateText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalContentColor.current.copy(alpha = 0.7f),
                            )
                        }

                        if (noteState.reminders.isNotEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                            ReminderChip(
                                reminders = noteState.reminders,
                                dateFormat = preferences.dateFormat,
                                timeFormat = preferences.timeFormat,
                                backgroundColor = backgroundColor,
                                onClick = {
                                    clickHandlers.onReminderClick(noteState.reminders.first())
                                },
                            )
                        }
                    }
                }

                // Files Preview
                if (noteState.files.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(noteState.files, key = { it.originalName }) { fileAttachment ->
                            FileAttachmentPreviewChip(
                                fileName = fileAttachment.originalName,
                                onClick = { clickHandlers.onFileClick(fileAttachment) },
                                onLongClick = { fileToDelete = fileAttachment },
                            )
                        }
                    }
                }

                // Labels
                if (noteState.labels.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(noteState.labels, key = { it }) { labelName ->
                            LabelChip(text = labelName)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            )
            Spacer(modifier = Modifier.height(4.dp))

            content()

            // Audio Recordings Section
            if (noteState.audios.isNotEmpty()) {
                EditScreenAudioSection(
                    audios = noteState.audios,
                    onAudioClick = { clickHandlers.onAudioClick(it) },
                )
            }
        }

        fileToDelete?.let { file ->
            ConfirmDialog(
                message = stringResource(R.string.delete_file, file.originalName),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = { fileToDelete = null },
                onConfirm = {
                    val toDelete = file
                    fileToDelete = null
                    clickHandlers.onFileDelete(toDelete)
                },
            )
        }
    }
}

@Composable
internal fun EditNoteScreen(
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
                painter = painterResource(id = R.drawable.text_format),
                contentDescription = stringResource(id = R.string.edit),
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

    EditScreen(
        noteState = noteState,
        modifier = modifier,
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
            val bodyDesc = stringResource(id = R.string.note)
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
                // TODO: user should be able to click anywhere in the are to focus the textfield
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
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
                                text = stringResource(id = R.string.note),
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
    noteModel: NoteModel? = null,
    actionHandler: NoteActionHandler? = null,
) {
    val context = LocalContext.current
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

    val backgroundColor = Color(context.extractColor(state.color))

    Box(modifier = modifier) {
        EditNoteScreen(
            noteState = noteState,
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

        if (noteModel != null) {
            EditorDialogs(noteModel)
            if (actionHandler != null) {
                EditorBottomSheets(
                    model = noteModel,
                    actionHandler = actionHandler,
                    backgroundColor =
                        if (state.color != BaseNote.COLOR_DEFAULT) backgroundColor else null,
                )
            }
        }
    }
}

@Composable
internal fun EditListScreen(
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
            awaitFrame() // Ensures layout pass is ready before requesting focus
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
                    key = { _, item -> item.id },
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
                    )
                }
            }

            // Add Item Button
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
                    tint = LocalContentColor.current.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 12.dp),
                )
                Text(
                    text = stringResource(id = R.string.add_item),
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
                            onCheckedChange = { checked ->
                                changeHandlers.onItemCheckedChange(item, checked)
                            },
                            isChild = item.isChild,
                            focusRequester = if (item.id == itemToFocusId) focusRequester else null,
                            onEnterKey = { wrappedClickHandlers.onAddItemClick() },
                            onBackspaceOnEmpty = { changeHandlers.onItemDelete(item) },
                            onDelete = { changeHandlers.onItemDelete(item) },
                            backgroundColor = backgroundColor,
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
    noteModel: NoteModel? = null,
    actionHandler: NoteActionHandler? = null,
) {
    val context = LocalContext.current
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

    val backgroundColor = Color(context.extractColor(state.color))

    Box(modifier = modifier) {
        EditListScreen(
            noteState = noteState,
            preferences = state.preferences,
            searchState = state.searchState,
            backgroundColor = backgroundColor,
            canUndo = state.canUndo,
            canRedo = state.canRedo,
            changeHandlers = changeHandlers,
            clickHandlers = clickHandlers,
            focusFirstItem = state.focusFirstItem,
        )

        if (noteModel != null) {
            EditorDialogs(noteModel)
            if (actionHandler != null) {
                EditorBottomSheets(
                    model = noteModel,
                    actionHandler = actionHandler,
                    backgroundColor =
                        if (state.color != BaseNote.COLOR_DEFAULT) backgroundColor else null,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditNoteScreenPreview() {
    NotallyXTheme {
        EditNoteScreen(
            noteState =
                EditNoteState(
                    title = "Project Planning Note",
                    bodyValue =
                        TextFieldValue(
                            "Here are the core tasks for the upcoming release:\n- Review design mockups\n- Finalize Jetpack Compose migration\n- Conduct QA testing"
                        ),
                    createdTimestamp = System.currentTimeMillis() - 86400000L,
                    modifiedTimestamp = System.currentTimeMillis(),
                    images =
                        listOf(
                            FileAttachment(
                                localName = "sample_image.png",
                                originalName = "Sample Image.png",
                                mimeType = "image/png",
                            ),
                            FileAttachment(
                                localName = "sample_image2.png",
                                originalName = "Sample Image2.png",
                                mimeType = "image/png",
                            ),
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
                                repetition = Repetition(1, RepetitionTimeUnit.DAYS),
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
            backgroundColor =
                Color(
                    LocalContext.current.extractColor(
                        com.philkes.notallyx.data.model.Color.CORAL.toColorString()
                    )
                ),
            canUndo = true,
        )
    }
}

@Preview(showBackground = true)
@Preview(name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditListColoredScreenPreview() {
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
                                id = 0,
                                body = "Fresh Organic Apples",
                                checked = false,
                                isChild = false,
                                order = 1,
                                children = mutableListOf(),
                            ),
                            ListItem(
                                id = 1,
                                body = "Almond Milk (Unsweetened)",
                                checked = false,
                                isChild = false,
                                order = 2,
                                children = mutableListOf(),
                            ),
                            ListItem(
                                id = 2,
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
                                id = 3,
                                body = "Greek Yogurt",
                                checked = true,
                                isChild = false,
                                order = 4,
                                children = mutableListOf(),
                            ),
                            ListItem(
                                id = 4,
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
            backgroundColor =
                Color(
                    LocalContext.current.extractColor(
                        com.philkes.notallyx.data.model.Color.CORAL.toColorString()
                    )
                ),
        )
    }
}

package com.philkes.notallyx.feature.editor.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.philkes.notallyx.Res
import com.philkes.notallyx.add
import com.philkes.notallyx.add_item
import com.philkes.notallyx.arrow_back
import com.philkes.notallyx.audio_recordings
import com.philkes.notallyx.back
import com.philkes.notallyx.cancel
import com.philkes.notallyx.close
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.ImageCountChip
import com.philkes.notallyx.core.ui.components.LabelChip
import com.philkes.notallyx.core.ui.components.ReminderChip
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.theme.contentColorFor
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.creation_date
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.formatDate
import com.philkes.notallyx.delete
import com.philkes.notallyx.delete_file
import com.philkes.notallyx.feature.editor.formatting.TextFormattingBar
import com.philkes.notallyx.feature.editor.formatting.TextFormattingBarActions
import com.philkes.notallyx.feature.editor.formatting.TextFormattingState
import com.philkes.notallyx.feature.media.audio.AudioItemRow
import com.philkes.notallyx.modified_date
import com.philkes.notallyx.more_vert
import com.philkes.notallyx.navigation.LocalBackButtonVisibility
import com.philkes.notallyx.redo
import com.philkes.notallyx.tap_for_more_options
import com.philkes.notallyx.title
import com.philkes.notallyx.undo
import java.io.File
import java.util.Date
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

interface EditChangeHandlers {
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

interface EditClickHandlers {
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
            text = stringResource(Res.string.audio_recordings),
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
                        IconButton(
                            onClick = { clickHandlers.onBackClick() },
                            modifier = Modifier.testTag(TestTags.SAVE_AND_CLOSE_BUTTON),
                        ) {
                            Icon(
                                painter =
                                    if (LocalBackButtonVisibility.current)
                                        painterResource(Res.drawable.arrow_back)
                                    else painterResource(Res.drawable.close),
                                contentDescription =
                                    if (LocalBackButtonVisibility.current)
                                        stringResource(Res.string.back)
                                    else stringResource(Res.string.cancel),
                            )
                        }
                    },
                    actions = {
                        preferences.topActions.forEach { action ->
                            val (titleRes, iconRes) =
                                action.getTitleAndIcon(
                                    noteState.pinned,
                                    noteState.viewMode,
                                    noteState.folder,
                                    noteState.type,
                                    noteState.isPinnedToStatus,
                                )
                            val testTagModifier =
                                when (action) {
                                    EditAction.DELETE ->
                                        Modifier.testTag(TestTags.DELETE_NOTE_BUTTON)
                                    EditAction.CHANGE_COLOR ->
                                        Modifier.testTag(TestTags.COLOR_PICKER_BUTTON)
                                    else -> Modifier
                                }
                            ActionIconButton(
                                icon = iconRes,
                                contentDescription = stringResource(titleRes),
                                onClick = { clickHandlers.onActionClick(action) },
                                onLongClick = { clickHandlers.onActionLongClick(action) },
                                modifier = testTagModifier,
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
                                painter = painterResource(Res.drawable.add),
                                contentDescription = stringResource(Res.string.add_item),
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
                                painter = painterResource(Res.drawable.undo),
                                contentDescription = stringResource(Res.string.undo),
                            )
                        }

                        IconButton(
                            onClick = { clickHandlers.onRedoClick() },
                            enabled = canRedo,
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.redo),
                                contentDescription = stringResource(Res.string.redo),
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (titleRes, iconRes) =
                            preferences.bottomAction.getTitleAndIcon(
                                noteState.pinned,
                                noteState.viewMode,
                                noteState.folder,
                                noteState.type,
                                noteState.isPinnedToStatus,
                            )
                        ActionIconButton(
                            icon = iconRes,
                            contentDescription = stringResource(titleRes),
                            onClick = { clickHandlers.onActionClick(preferences.bottomAction) },
                            onLongClick = {
                                clickHandlers.onActionLongClick(preferences.bottomAction)
                            },
                        )

                        IconButton(onClick = { clickHandlers.onMoreOptionsClick() }) {
                            Icon(
                                painter = painterResource(Res.drawable.more_vert),
                                contentDescription =
                                    stringResource(Res.string.tap_for_more_options),
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
                val titleDesc = stringResource(Res.string.title)
                BasicTextField(
                    value = noteState.title,
                    onValueChange = { changeHandlers.onTitleChange(it) },
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(top = 4.dp, bottom = 0.dp)
                            .testTag(TestTags.NOTE_TITLE_INPUT)
                            .semantics { contentDescription = titleDesc },
                    textStyle =
                        MaterialTheme.typography.headlineSmall.copy(
                            color = LocalContentColor.current
                        ),
                    cursorBrush = SolidColor(LocalContentColor.current),
                    decorationBox = { innerTextField ->
                        Box {
                            if (noteState.title.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.title),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = LocalContentColor.current.copy(alpha = 0.6f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                val prefixRes =
                    if (preferences.notesSortBy == NotesSortBy.CREATION_DATE)
                        Res.string.creation_date
                    else Res.string.modified_date
                val prefixText = stringResource(prefixRes)

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
                                .formatDate(preferences.dateFormat, preferences.timeFormat)
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
                message = stringResource(Res.string.delete_file, file.originalName),
                positiveButtonText = stringResource(Res.string.delete),
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

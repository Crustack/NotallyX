package com.philkes.notallyx.feature.main

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.ResultEffect
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.shareNote
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.ErrorDialog
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.TriStateMultiChoiceDialog
import com.philkes.notallyx.core.ui.dialog.color.ColorPickerDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.EditListRoute
import com.philkes.notallyx.feature.editor.EditNoteRoute
import com.philkes.notallyx.feature.editor.EditTextPlainRoute
import com.philkes.notallyx.feature.labels.LabelsViewModel
import com.philkes.notallyx.feature.labels.SelectLabelsRoute
import com.philkes.notallyx.feature.main.routes.PickNoteRoute
import com.philkes.notallyx.feature.main.routes.SearchRoute
import com.philkes.notallyx.feature.main.routes.archived.ArchivedRoute
import com.philkes.notallyx.feature.main.routes.deleted.DeletedRoute
import com.philkes.notallyx.feature.main.routes.labels.DisplayLabelRoute
import com.philkes.notallyx.feature.main.routes.labels.LabelsRoute
import com.philkes.notallyx.feature.main.routes.labels.UnlabeledRoute
import com.philkes.notallyx.feature.main.routes.notes.NotesRoute
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.main.routes.reminders.RemindersRoute
import com.philkes.notallyx.feature.main.routes.settings.SettingsRoute
import com.philkes.notallyx.feature.media.audio.PlayAudioRoute
import com.philkes.notallyx.feature.media.audio.RecordAudioRoute
import com.philkes.notallyx.feature.media.image.ViewImageRoute
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.progress.MigrationProgress
import com.philkes.notallyx.feature.reminder.NoteRemindersRoute
import com.philkes.notallyx.navigation.AudioDeletedResult
import com.philkes.notallyx.navigation.AudioRecordedResult
import com.philkes.notallyx.navigation.ColorPickerResult
import com.philkes.notallyx.navigation.ConfirmResult
import com.philkes.notallyx.navigation.ExportFormatResult
import com.philkes.notallyx.navigation.ImagesDeletedResult
import com.philkes.notallyx.navigation.ListDetailScene
import com.philkes.notallyx.navigation.LocalNavigator
import com.philkes.notallyx.navigation.Navigator
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.navigation.PickedNoteResult
import com.philkes.notallyx.navigation.RemindersUpdatedResult
import com.philkes.notallyx.navigation.SelectedLabelsResult
import com.philkes.notallyx.navigation.rememberListDetailSceneStrategy
import com.philkes.notallyx.navigation.rememberNavigationState
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.koinViewModel

private const val ACTION_DELETE_SELECTED_FOREVER = "DELETE_SELECTED_FOREVER"

@Composable
fun NotallyApp(
    startKey: NotallyNavKey,
    activity: Activity,
    currentNavKeyFlow: MutableStateFlow<NotallyNavKey>,
    currentTitleFlow: MutableStateFlow<String>,
    onStartViewChanged: (Boolean) -> Unit,
    onPrepareNewNoteIntent: (Intent) -> Intent,
    onNoteClick: (BaseNote, Int) -> Unit,
    onNoteLongClick: (BaseNote, Int, List<Item>) -> Unit,
    onReminderClick: ((BaseNote) -> Unit)? = null,
    onExportNotes: (ExportMimeType) -> Unit,
    onMoveSelectedNotes: (Folder) -> Unit,
    onDeleteSelectedNotesForever: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onShowToast: (Int) -> Unit,
    migrationProgressState: MigrationProgress?,
    modifier: Modifier = Modifier,
    skipStartViewOnBack: Boolean = false,
    pendingNavKeyFlow: MutableStateFlow<NotallyNavKey?>? = null,
    mainActivityViewModel: MainActivityViewModel = koinViewModel(),
    notallyRouteModel: NotesRouteViewModel = koinViewModel(),
    labelsViewModel: LabelsViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyRouteModel.preferences,
) {
    val currentTheme by preferences.theme.flow.collectAsStateWithLifecycle(preferences.theme.value)
    val useDynamicColors by
        preferences.useDynamicColors.flow.collectAsStateWithLifecycle(
            preferences.useDynamicColors.value
        )
    NotallyXTheme(theme = currentTheme, dynamicColor = useDynamicColors) {
        val navigationState = rememberNavigationState(startRoute = startKey)
        val navigator =
            remember(navigationState, skipStartViewOnBack) {
                Navigator(navigationState, skipStartViewOnBack = skipStartViewOnBack)
            }

        val isActionModeEnabled by
            notallyRouteModel.actionMode.enabled.collectAsStateWithLifecycle()
        BackHandler(enabled = isActionModeEnabled) { notallyRouteModel.actionMode.close(true) }

        if (pendingNavKeyFlow != null) {
            val pendingNavKey by pendingNavKeyFlow.collectAsStateWithLifecycle()
            LaunchedEffect(pendingNavKey) {
                pendingNavKey?.let { key ->
                    if (
                        key is NotallyNavKey.EditNote ||
                            key is NotallyNavKey.EditList ||
                            key is NotallyNavKey.EditTextPlain
                    ) {
                        if (
                            navigator.currentRoute is NotallyNavKey.EditNote ||
                                navigator.currentRoute is NotallyNavKey.EditList ||
                                navigator.currentRoute is NotallyNavKey.EditTextPlain
                        ) {
                            navigator.currentStack.removeLastOrNull()
                        }
                    }
                    navigator.navigate(key)
                    pendingNavKeyFlow.value = null
                }
            }
        }

        val activeScreenKey =
            (navigator.currentStack.findLast {
                it !is NotallyNavKey.Confirm &&
                    it !is NotallyNavKey.ColorPicker &&
                    it !is NotallyNavKey.ExportFormat
            } as? NotallyNavKey) ?: navigator.currentTopLevelRoute

        val isSecondaryScreen =
            activeScreenKey is NotallyNavKey.PickNote ||
                activeScreenKey is NotallyNavKey.SelectLabels ||
                activeScreenKey is NotallyNavKey.NoteReminders ||
                activeScreenKey is NotallyNavKey.ViewImage ||
                activeScreenKey is NotallyNavKey.PlayAudio ||
                activeScreenKey is NotallyNavKey.RecordAudio ||
                activeScreenKey is NotallyNavKey.EditNote ||
                activeScreenKey is NotallyNavKey.EditList ||
                activeScreenKey is NotallyNavKey.EditTextPlain

        val currentTitle =
            when (val key = activeScreenKey) {
                is NotallyNavKey.Notes -> activity.getString(R.string.notes)
                is NotallyNavKey.Labels -> activity.getString(R.string.labels)
                is NotallyNavKey.Deleted -> activity.getString(R.string.deleted)
                is NotallyNavKey.Archived -> activity.getString(R.string.archived)
                is NotallyNavKey.Reminders -> activity.getString(R.string.reminders)
                is NotallyNavKey.Settings -> activity.getString(R.string.settings)
                is NotallyNavKey.Unlabeled -> activity.getString(R.string.unlabeled)
                is NotallyNavKey.DisplayLabel -> key.label
                is NotallyNavKey.Search -> activity.getString(R.string.search)
                is NotallyNavKey.PickNote -> activity.getString(R.string.select_note)
                is NotallyNavKey.SelectLabels -> activity.getString(R.string.select_labels)
                is NotallyNavKey.NoteReminders -> activity.getString(R.string.reminders)
                else -> ""
            }

        val currentFolder =
            when (activeScreenKey) {
                is NotallyNavKey.Archived -> Folder.ARCHIVED
                is NotallyNavKey.Deleted -> Folder.DELETED
                else -> notallyRouteModel.folder.collectAsStateWithLifecycle(Folder.NOTES).value
            }

        LaunchedEffect(activeScreenKey, navigator.currentTopLevelRoute) {
            currentNavKeyFlow.value = activeScreenKey
            currentTitleFlow.value = currentTitle
            onStartViewChanged(keyMatchesStartKey(navigator.currentTopLevelRoute, startKey))
            when (activeScreenKey) {
                is NotallyNavKey.Archived -> {
                    notallyRouteModel.setFolder(Folder.ARCHIVED)
                    notallyRouteModel.currentLabel = NotesRouteViewModel.CURRENT_LABEL_EMPTY
                }
                is NotallyNavKey.Deleted -> {
                    notallyRouteModel.setFolder(Folder.DELETED)
                    notallyRouteModel.currentLabel = NotesRouteViewModel.CURRENT_LABEL_EMPTY
                }
                is NotallyNavKey.DisplayLabel -> {
                    notallyRouteModel.setFolder(Folder.NOTES)
                    notallyRouteModel.currentLabel = activeScreenKey.label
                }
                is NotallyNavKey.Unlabeled -> {
                    notallyRouteModel.setFolder(Folder.NOTES)
                    notallyRouteModel.currentLabel = NotesRouteViewModel.CURRENT_LABEL_NONE
                }
                is NotallyNavKey.Notes -> {
                    notallyRouteModel.setFolder(Folder.NOTES)
                    notallyRouteModel.currentLabel = NotesRouteViewModel.CURRENT_LABEL_EMPTY
                }
                is NotallyNavKey.Search -> {
                    // search route manages its own folder and label
                }
                else -> {}
            }
        }

        val activeDialog by mainActivityViewModel.activeDialog.collectAsStateWithLifecycle()
        val mainProgress by mainActivityViewModel.progress.collectAsStateWithLifecycle()
        val routeProgress by notallyRouteModel.progress.collectAsStateWithLifecycle()

        val labelsState = mainActivityViewModel.labels.collectAsStateWithLifecycle(emptyList())
        val actionModeEnabledState =
            notallyRouteModel.actionMode.enabled.collectAsStateWithLifecycle(false)
        val selectedNoteIds by
            notallyRouteModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
        val selectedNotesCount = selectedNoteIds.size

        val selectedNotes = notallyRouteModel.actionMode.selectedNotes.values
        val isAllPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned }
        val isAllPinnedToStatus =
            selectedNotes.isNotEmpty() && selectedNotes.all { it.isPinnedToStatus }

        val dateFormatOverview by
            preferences.dateFormatOverview.flow.collectAsStateWithLifecycle(
                preferences.dateFormatOverview.value
            )
        val timeFormatOverview by
            preferences.timeFormatOverview.flow.collectAsStateWithLifecycle(
                preferences.timeFormatOverview.value
            )
        val notesSorting by
            preferences.notesSorting.flow.collectAsStateWithLifecycle(
                preferences.notesSorting.value
            )
        val textSizeOverview by
            preferences.textSizeOverview.flow.collectAsStateWithLifecycle(
                preferences.textSizeOverview.value
            )
        val maxTitle by
            preferences.maxTitle.flow.collectAsStateWithLifecycle(preferences.maxTitle.value)
        val maxLines by
            preferences.maxLines.flow.collectAsStateWithLifecycle(preferences.maxLines.value)
        val maxItems by
            preferences.maxItems.flow.collectAsStateWithLifecycle(preferences.maxItems.value)
        val labelTagsHiddenInOverview by
            preferences.labelTagsHiddenInOverview.flow.collectAsStateWithLifecycle(
                preferences.labelTagsHiddenInOverview.value
            )
        val imagesHiddenInOverview by
            preferences.imagesHiddenInOverview.flow.collectAsStateWithLifecycle(
                preferences.imagesHiddenInOverview.value
            )

        val displayConfig =
            NoteCardDisplayConfig(
                dateFormat = dateFormatOverview,
                timeFormat = timeFormatOverview,
                sortBy = notesSorting.sortedBy,
                textSize = textSizeOverview,
                maxTitleLines = maxTitle,
                maxLines = maxLines,
                maxItems = maxItems,
                hideLabels = labelTagsHiddenInOverview,
                hideImages = imagesHiddenInOverview,
            )

        CompositionLocalProvider(
            LocalNavigator provides navigator,
            LocalResultEventBus provides navigationState.resultEventBus,
        ) {
            ResultEffect<ConfirmResult> { result ->
                if (result.confirmed) {
                    when (result.actionType) {
                        ACTION_DELETE_SELECTED_FOREVER -> {
                            onDeleteSelectedNotesForever()
                        }
                    }
                }
            }

            ResultEffect<ColorPickerResult> { result ->
                when (result) {
                    is ColorPickerResult.Selected -> notallyRouteModel.colorBaseNote(result.color)
                    is ColorPickerResult.Changed -> {
                        notallyRouteModel.changeColor(result.oldColor, result.newColor)
                        if (preferences.defaultNoteColor.value == result.oldColor) {
                            preferences.defaultNoteColor.save(result.newColor)
                        }
                    }
                    is ColorPickerResult.Deleted -> {
                        notallyRouteModel.changeColor(result.colorToDelete, result.newColor)
                        if (preferences.defaultNoteColor.value == result.colorToDelete) {
                            preferences.defaultNoteColor.save(BaseNote.COLOR_DEFAULT)
                        }
                    }
                    is ColorPickerResult.DefaultSet -> {
                        preferences.defaultNoteColor.save(result.defaultColor)
                    }
                }
            }

            ResultEffect<ExportFormatResult> { result -> onExportNotes(result.format) }

            val actionModeListener =
                remember(notallyRouteModel, mainActivityViewModel) {
                    object : ActionModeListener {
                        override fun onClose() {
                            notallyRouteModel.actionMode.close(true)
                        }

                        override fun onSelectAll() {
                            notallyRouteModel.selectAll()
                        }

                        override fun onPin() {
                            notallyRouteModel.togglePinSelectedNotes()
                        }

                        override fun onLabels() {
                            notallyRouteModel.onLabelsClick(
                                onNavigateToLabels = { navigator.navigate(NotallyNavKey.Labels) },
                                onShowDialog = { labels, baseNotes ->
                                    val checkedPositions =
                                        labels
                                            .map { label ->
                                                if (baseNotes.all { it.labels.contains(label) }) {
                                                    ToggleableState.On
                                                } else if (
                                                    baseNotes.any { it.labels.contains(label) }
                                                ) {
                                                    ToggleableState.Indeterminate
                                                } else {
                                                    ToggleableState.Off
                                                }
                                            }
                                            .toTypedArray()

                                    mainActivityViewModel.showSelectLabelsDialog(
                                        labels = labels,
                                        initialStates = checkedPositions,
                                        onConfirm = { finalStates ->
                                            val checkedLabels =
                                                finalStates.mapIndexedNotNull { index, checked ->
                                                    if (checked == ToggleableState.On) {
                                                        labels[index]
                                                    } else null
                                                }
                                            val uncheckedLabels =
                                                finalStates.mapIndexedNotNull { index, checked ->
                                                    if (checked == ToggleableState.Off) {
                                                        labels[index]
                                                    } else null
                                                }
                                            val updatedNotes = baseNotes.map { baseNote ->
                                                val noteLabels = baseNote.labels.toMutableList()
                                                checkedLabels.forEach { checkedLabel ->
                                                    if (!noteLabels.contains(checkedLabel)) {
                                                        noteLabels.add(checkedLabel)
                                                    }
                                                }
                                                uncheckedLabels.forEach { uncheckedLabel ->
                                                    noteLabels.remove(uncheckedLabel)
                                                }
                                                baseNote.id to noteLabels
                                            }
                                            notallyRouteModel.updateBaseNotesLabels(updatedNotes)
                                        },
                                    )
                                },
                            )
                        }

                        override fun onDelete() {
                            onMoveSelectedNotes(Folder.DELETED)
                        }

                        override fun onRestore() {
                            onMoveSelectedNotes(Folder.NOTES)
                        }

                        override fun onArchive() {
                            onMoveSelectedNotes(Folder.ARCHIVED)
                        }

                        override fun onDeleteForever() {
                            navigator.navigate(
                                NotallyNavKey.Confirm(
                                    message = activity.getString(R.string.delete_selected_notes),
                                    actionType = ACTION_DELETE_SELECTED_FOREVER,
                                    positiveButtonText = activity.getString(R.string.delete),
                                )
                            )
                        }

                        override fun onDuplicate() {
                            notallyRouteModel.duplicateSelectedBaseNotes()
                        }

                        override fun onChangeColor() {
                            notallyRouteModel.onChangeColorClick { colors, currentColor ->
                                navigator.navigate(
                                    NotallyNavKey.ColorPicker(
                                        currentColor = currentColor,
                                        colors = colors.toList(),
                                        defaultColor = preferences.defaultNoteColor.value,
                                    )
                                )
                            }
                        }

                        override fun onPinToStatus() {
                            notallyRouteModel.onPinToStatusClick(
                                activity = activity,
                                onRequestPermission = onRequestNotificationPermission,
                            )
                        }

                        override fun onShare() {
                            if (!notallyRouteModel.actionMode.isEmpty()) {
                                val baseNote = notallyRouteModel.actionMode.getFirstNote()
                                activity.shareNote(baseNote)
                            }
                        }

                        override fun onExport() {
                            navigator.navigate(NotallyNavKey.ExportFormat())
                        }
                    }
                }

            val handleReminderClick: (BaseNote) -> Unit = { note ->
                if (notallyRouteModel.actionMode.isEnabled()) {
                    if (notallyRouteModel.actionMode.selectedNotes.contains(note.id)) {
                        notallyRouteModel.actionMode.remove(note.id)
                    } else {
                        notallyRouteModel.actionMode.add(note.id, note)
                    }
                } else if (onReminderClick != null) {
                    onReminderClick(note)
                } else {
                    navigator.navigate(NotallyNavKey.NoteReminders(note.id))
                }
            }

            val handleNoteClick: (BaseNote, Int) -> Unit = { note, pos ->
                if (notallyRouteModel.actionMode.isEnabled()) {
                    onNoteClick(note, pos)
                } else {
                    val targetKey =
                        if (note.type == Type.NOTE) {
                            NotallyNavKey.EditNote(noteId = note.id)
                        } else {
                            NotallyNavKey.EditList(noteId = note.id)
                        }
                    if (
                        navigator.currentRoute is NotallyNavKey.EditNote ||
                            navigator.currentRoute is NotallyNavKey.EditList
                    ) {
                        navigator.currentStack.removeLastOrNull()
                    }
                    navigator.navigate(targetKey)
                }
            }

            val entryProvider =
                entryProvider<NavKey> {
                    entry<NotallyNavKey.Notes>(metadata = ListDetailScene.listPane()) {
                        NotesRoute(
                            displayConfig = displayConfig,
                            onNoteClick = handleNoteClick,
                            onNoteLongClick = onNoteLongClick,
                            onReminderClick = handleReminderClick,
                        )
                    }

                    entry<NotallyNavKey.Labels>(metadata = ListDetailScene.topLevel()) {
                        LabelsRoute(
                            onNavigateToLabel = { label ->
                                navigator.navigate(NotallyNavKey.DisplayLabel(label))
                            },
                            onShowToast = onShowToast,
                            labelsViewModel = labelsViewModel,
                        )
                    }

                    entry<NotallyNavKey.Deleted>(metadata = ListDetailScene.listPane()) {
                        DeletedRoute(
                            displayConfig = displayConfig,
                            onNoteClick = handleNoteClick,
                            onNoteLongClick = onNoteLongClick,
                            onReminderClick = handleReminderClick,
                        )
                    }

                    entry<NotallyNavKey.Archived>(metadata = ListDetailScene.listPane()) {
                        ArchivedRoute(
                            displayConfig = displayConfig,
                            onNoteClick = handleNoteClick,
                            onNoteLongClick = onNoteLongClick,
                            onReminderClick = handleReminderClick,
                        )
                    }

                    entry<NotallyNavKey.Reminders>(metadata = ListDetailScene.listPane()) {
                        RemindersRoute(
                            displayConfig = displayConfig,
                            onNoteClick = handleNoteClick,
                            onNoteLongClick = onNoteLongClick,
                            onReminderClick = handleReminderClick,
                            notallyRouteModel = notallyRouteModel,
                        )
                    }

                    entry<NotallyNavKey.Settings>(metadata = ListDetailScene.topLevel()) {
                        SettingsRoute(notallyRouteModel = notallyRouteModel)
                    }

                    entry<NotallyNavKey.Unlabeled>(metadata = ListDetailScene.listPane()) {
                        UnlabeledRoute(
                            displayConfig = displayConfig,
                            onNoteClick = handleNoteClick,
                            onNoteLongClick = onNoteLongClick,
                            onReminderClick = handleReminderClick,
                        )
                    }

                    entry<NotallyNavKey.DisplayLabel>(
                        metadata = ListDetailScene.listPane(isTopLevel = false)
                    ) { key ->
                        DisplayLabelRoute(
                            label = key.label,
                            displayConfig = displayConfig,
                            onNoteClick = handleNoteClick,
                            onNoteLongClick = onNoteLongClick,
                            onReminderClick = handleReminderClick,
                            notallyRouteModel = notallyRouteModel,
                        )
                    }

                    entry<NotallyNavKey.Search>(
                        metadata = ListDetailScene.listPane(isTopLevel = false)
                    ) { key ->
                        SearchRoute(
                            initialFolder = key.initialFolder,
                            initialLabel = key.initialLabel,
                            displayConfig = displayConfig,
                            onNoteClick = handleNoteClick,
                            onNoteLongClick = onNoteLongClick,
                            onReminderClick = handleReminderClick,
                            notallyRouteModel = notallyRouteModel,
                        )
                    }

                    // --- Editor Destinations ---

                    val handleBack: () -> Unit = {
                        if (!navigator.goBack()) {
                            activity.finish()
                        }
                    }

                    entry<NotallyNavKey.EditNote>(metadata = ListDetailScene.detailPane()) { key ->
                        EditNoteRoute(
                            key = key,
                            onBackClick = handleBack,
                        )
                    }

                    entry<NotallyNavKey.EditList>(metadata = ListDetailScene.detailPane()) { key ->
                        EditListRoute(
                            key = key,
                            onBackClick = handleBack,
                        )
                    }

                    entry<NotallyNavKey.EditTextPlain>(metadata = ListDetailScene.detailPane()) {
                        key ->
                        EditTextPlainRoute(
                            key = key,
                            onBackClick = handleBack,
                        )
                    }

                    // --- Secondary Screen Destinations ---

                    entry<NotallyNavKey.PickNote> { key ->
                        val resultBus = LocalResultEventBus.current
                        PickNoteRoute(
                            excludeNoteId = key.excludeNoteId,
                            onNotePicked = { note ->
                                resultBus.sendResult(
                                    PickedNoteResult(note.id, note.title, note.type)
                                )
                                handleBack()
                            },
                            onBackClick = handleBack,
                        )
                    }

                    entry<NotallyNavKey.SelectLabels> { key ->
                        val resultBus = LocalResultEventBus.current
                        SelectLabelsRoute(
                            initialLabels = key.currentLabels,
                            onLabelsSelected = { selectedLabels ->
                                resultBus.sendResult(SelectedLabelsResult(selectedLabels))
                            },
                            onBackClick = handleBack,
                        )
                    }

                    entry<NotallyNavKey.NoteReminders> { key ->
                        val resultBus = LocalResultEventBus.current
                        NoteRemindersRoute(
                            noteId = key.noteId,
                            onBackClick = {
                                resultBus.sendResult(RemindersUpdatedResult(key.noteId))
                                handleBack()
                            },
                        )
                    }

                    entry<NotallyNavKey.ViewImage> { key ->
                        val resultBus = LocalResultEventBus.current
                        ViewImageRoute(
                            key = key,
                            onDeletedImages = { deletedImages ->
                                resultBus.sendResult(ImagesDeletedResult(deletedImages))
                            },
                            onBackClick = handleBack,
                        )
                    }

                    entry<NotallyNavKey.PlayAudio> { key ->
                        val resultBus = LocalResultEventBus.current
                        PlayAudioRoute(
                            key = key,
                            onAudioDeleted = { audioName ->
                                resultBus.sendResult(AudioDeletedResult(audioName))
                            },
                            onBackClick = handleBack,
                        )
                    }

                    entry<NotallyNavKey.RecordAudio> {
                        val resultBus = LocalResultEventBus.current
                        RecordAudioRoute(
                            onRecorded = { resultBus.sendResult(AudioRecordedResult(true)) },
                            onBackClick = handleBack,
                        )
                    }

                    // --- Dialog Scenes ---

                    entry<NotallyNavKey.Confirm>(
                        metadata =
                            DialogSceneStrategy.dialog(
                                DialogProperties(
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true,
                                )
                            )
                    ) { key ->
                        val resultBus = LocalResultEventBus.current
                        ConfirmDialog(
                            title = key.title,
                            message = key.message,
                            positiveButtonText =
                                key.positiveButtonText ?: stringResource(R.string.delete),
                            negativeButtonText =
                                key.negativeButtonText ?: stringResource(R.string.cancel),
                            onDismissRequest = { navigator.goBack() },
                            onConfirm = {
                                resultBus.sendResult(
                                    ConfirmResult(key.actionType, confirmed = true)
                                )
                                navigator.goBack()
                            },
                        )
                    }

                    entry<NotallyNavKey.ColorPicker>(metadata = DialogSceneStrategy.dialog()) { key
                        ->
                        val resultBus = LocalResultEventBus.current
                        ColorPickerDialog(
                            currentColor = key.currentColor ?: BaseNote.COLOR_DEFAULT,
                            colors = key.colors.toSet(),
                            defaultColor = key.defaultColor,
                            onDismissRequest = { navigator.goBack() },
                            onColorSelected = { color ->
                                resultBus.sendResult<ColorPickerResult>(
                                    ColorPickerResult.Selected(color)
                                )
                            },
                            onColorChanged = { oldColor, newColor ->
                                resultBus.sendResult<ColorPickerResult>(
                                    ColorPickerResult.Changed(oldColor, newColor)
                                )
                            },
                            onColorDeleted = { colorToDelete, newColor ->
                                resultBus.sendResult<ColorPickerResult>(
                                    ColorPickerResult.Deleted(colorToDelete, newColor)
                                )
                            },
                            onSetDefaultColor = { newDefault ->
                                resultBus.sendResult<ColorPickerResult>(
                                    ColorPickerResult.DefaultSet(newDefault)
                                )
                            },
                        )
                    }

                    entry<NotallyNavKey.ExportFormat>(metadata = DialogSceneStrategy.dialog()) { key
                        ->
                        val resultBus = LocalResultEventBus.current
                        ListDialog(
                            title = stringResource(R.string.export),
                            items = key.formats.map { it.name },
                            onDismissRequest = { navigator.goBack() },
                            onItemClick = { index ->
                                resultBus.sendResult(ExportFormatResult(key.formats[index]))
                                navigator.goBack()
                            },
                        )
                    }
                }

            val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
            val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

            MainScreen(
                title = currentTitle,
                labels = labelsState.value,
                selectedNavKey = activeScreenKey,
                actionModeEnabled = actionModeEnabledState.value,
                selectedCount = selectedNotesCount,
                currentFolder = currentFolder,
                isAllPinned = isAllPinned,
                isAllPinnedToStatus = isAllPinnedToStatus,
                isSecondaryScreen = isSecondaryScreen,
                showFabs =
                    activeScreenKey is NotallyNavKey.Notes ||
                        activeScreenKey is NotallyNavKey.DisplayLabel ||
                        activeScreenKey is NotallyNavKey.Unlabeled,
                showSearchOption =
                    !preferences.alwaysShowSearchBar.value &&
                        !MainActivity.ACTIVITES_WITHOUT_SEARCH.contains(activeScreenKey),
                isInSearchMode = activeScreenKey is NotallyNavKey.Search,
                onNavigationItemClick = { targetKey -> navigator.navigate(targetKey) },
                onTakeNoteClick = {
                    val currentLabel = notallyRouteModel.currentLabel
                    val label =
                        if (
                            !currentLabel.isNullOrEmpty() &&
                                currentLabel != NotesRouteViewModel.CURRENT_LABEL_EMPTY &&
                                currentLabel != NotesRouteViewModel.CURRENT_LABEL_NONE
                        )
                            currentLabel
                        else null
                    if (
                        navigator.currentRoute is NotallyNavKey.EditNote ||
                            navigator.currentRoute is NotallyNavKey.EditList
                    ) {
                        navigator.currentStack.removeLastOrNull()
                    }
                    navigator.navigate(
                        NotallyNavKey.EditNote(
                            initialFolder = notallyRouteModel.folder.value,
                            initialLabel = label,
                        )
                    )
                },
                onMakeListClick = {
                    val currentLabel = notallyRouteModel.currentLabel
                    val label =
                        if (
                            !currentLabel.isNullOrEmpty() &&
                                currentLabel != NotesRouteViewModel.CURRENT_LABEL_EMPTY &&
                                currentLabel != NotesRouteViewModel.CURRENT_LABEL_NONE
                        )
                            currentLabel
                        else null
                    if (
                        navigator.currentRoute is NotallyNavKey.EditNote ||
                            navigator.currentRoute is NotallyNavKey.EditList
                    ) {
                        navigator.currentStack.removeLastOrNull()
                    }
                    navigator.navigate(
                        NotallyNavKey.EditList(
                            initialFolder = notallyRouteModel.folder.value,
                            initialLabel = label,
                        )
                    )
                },
                onSearchClick = {
                    if (navigator.currentRoute is NotallyNavKey.Search) {
                        notallyRouteModel.keyword = ""
                        navigator.goBack()
                    } else {
                        navigator.navigate(
                            NotallyNavKey.Search(
                                initialFolder = notallyRouteModel.folder.value,
                                initialLabel = notallyRouteModel.currentLabel,
                            )
                        )
                    }
                },
                onAddClick =
                    if (activeScreenKey is NotallyNavKey.Labels) {
                        { labelsViewModel.showAddDialog() }
                    } else null,
                actionModeListener = actionModeListener,
                modifier = modifier,
            ) {
                NavDisplay(
                    entries = navigationState.toDecoratedEntries(entryProvider),
                    sceneStrategies = listOf(dialogStrategy, listDetailStrategy),
                    transitionSpec = {
                        val fromTopLevel =
                            initialState.entries
                                .lastOrNull()
                                ?.metadata
                                ?.contains(ListDetailScene.TopLevelKey) == true
                        val toTopLevel =
                            targetState.entries
                                .lastOrNull()
                                ?.metadata
                                ?.contains(ListDetailScene.TopLevelKey) == true
                        if (fromTopLevel && toTopLevel) {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(220))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300),
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { -it / 3 },
                                    animationSpec = tween(300),
                                ) + fadeOut(animationSpec = tween(150))
                        }
                    },
                    popTransitionSpec = {
                        val fromTopLevel =
                            initialState.entries
                                .lastOrNull()
                                ?.metadata
                                ?.contains(ListDetailScene.TopLevelKey) == true
                        val toTopLevel =
                            targetState.entries
                                .lastOrNull()
                                ?.metadata
                                ?.contains(ListDetailScene.TopLevelKey) == true
                        if (fromTopLevel && toTopLevel) {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(220))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(300),
                            ) + fadeIn(animationSpec = tween(150)) togetherWith
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(300),
                                ) + fadeOut(animationSpec = tween(300))
                        }
                    },
                    predictivePopTransitionSpec = { _ ->
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = tween(300),
                        ) + fadeIn(animationSpec = tween(150)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { it },
                                animationSpec = tween(300),
                            ) + fadeOut(animationSpec = tween(300))
                    },
                    onBack = {
                        if (notallyRouteModel.actionMode.enabled.value) {
                            notallyRouteModel.actionMode.close(true)
                        } else if (!navigator.goBack()) {
                            activity.finish()
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        NotallyAppDialogs(
            activeDialog = activeDialog,
            mainProgress = mainProgress,
            routeProgress = routeProgress,
            migrationProgress = migrationProgressState,
            onDismissDialog = { mainActivityViewModel.dismissDialog() },
        )
    }
}

@Composable
private fun NotallyAppDialogs(
    activeDialog: MainActiveDialog?,
    mainProgress: com.philkes.notallyx.core.common.result.Progress?,
    routeProgress: com.philkes.notallyx.core.common.result.Progress?,
    migrationProgress: MigrationProgress?,
    onDismissDialog: () -> Unit,
) {
    when (activeDialog) {
        is MainActiveDialog.ColorPicker -> {
            ColorPickerDialog(
                currentColor = activeDialog.currentColor ?: BaseNote.COLOR_DEFAULT,
                colors = activeDialog.colors,
                defaultColor = activeDialog.defaultColor,
                onDismissRequest = onDismissDialog,
                onColorSelected = { color ->
                    onDismissDialog()
                    activeDialog.onColorSelected(color)
                },
                onColorChanged = { oldColor, newColor ->
                    onDismissDialog()
                    activeDialog.onColorChanged?.invoke(oldColor, newColor)
                },
                onColorDeleted = { colorToDelete, newColor ->
                    onDismissDialog()
                    activeDialog.onColorDeleted?.invoke(colorToDelete, newColor)
                },
                onSetDefaultColor = { newDefault ->
                    activeDialog.onSetDefaultColor?.invoke(newDefault)
                },
            )
        }
        is MainActiveDialog.ExportFormat -> {
            ListDialog(
                title = stringResource(R.string.export),
                items = activeDialog.formats.map { it.name },
                onDismissRequest = onDismissDialog,
                onItemClick = { index ->
                    onDismissDialog()
                    activeDialog.onFormatSelected(activeDialog.formats[index])
                },
            )
        }
        is MainActiveDialog.SelectLabels -> {
            TriStateMultiChoiceDialog(
                title = stringResource(R.string.labels),
                items = activeDialog.labels,
                initialStates = activeDialog.initialStates,
                onDismissRequest = onDismissDialog,
                onConfirm = { finalStates ->
                    onDismissDialog()
                    activeDialog.onConfirm(finalStates)
                },
            )
        }
        is MainActiveDialog.Confirm -> {
            ConfirmDialog(
                title = activeDialog.title,
                message = activeDialog.message,
                positiveButtonText =
                    activeDialog.positiveButtonText ?: stringResource(R.string.delete),
                negativeButtonText =
                    activeDialog.negativeButtonText ?: stringResource(R.string.cancel),
                onDismissRequest = onDismissDialog,
                onConfirm = {
                    onDismissDialog()
                    activeDialog.onConfirm()
                },
            )
        }
        is MainActiveDialog.Error -> {
            ErrorDialog(
                title = activeDialog.title,
                message = activeDialog.message,
                details = activeDialog.details,
                neutralButtonText = activeDialog.neutralButtonText,
                onNeutralButtonClick = activeDialog.onNeutralButtonClick,
                onDismissRequest = onDismissDialog,
            )
        }
        null -> {}
    }

    val activeProgress =
        listOfNotNull(mainProgress, routeProgress, migrationProgress).firstOrNull { it.inProgress }

    if (activeProgress != null) {
        val msg =
            when {
                activeProgress.indeterminate -> stringResource(R.string.calculating)
                else ->
                    stringResource(
                        R.string.count,
                        activeProgress.current,
                        activeProgress.total,
                    ) + (activeProgress.countSuffix?.let { " $it" } ?: "")
            }
        val progressValue =
            if (activeProgress.indeterminate || activeProgress.total == 0) null
            else activeProgress.current.toFloat() / activeProgress.total.toFloat()

        ProgressDialog(
            title = stringResource(activeProgress.titleId),
            message = msg,
            progress = progressValue,
        )
    }
}

private fun keyMatchesStartKey(currentKey: NotallyNavKey, startKey: NotallyNavKey): Boolean {
    return currentKey == startKey
}

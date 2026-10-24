package com.philkes.notallyx.feature.main

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.shareNote
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.dialog.ColorPickerDialog
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.ErrorDialog
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.TriStateMultiChoiceDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.feature.editor.EditListActivity
import com.philkes.notallyx.feature.editor.EditNoteActivity
import com.philkes.notallyx.feature.labels.LabelsViewModel
import com.philkes.notallyx.feature.main.routes.SearchRoute
import com.philkes.notallyx.feature.main.routes.archived.ArchivedRoute
import com.philkes.notallyx.feature.main.routes.deleted.DeletedRoute
import com.philkes.notallyx.feature.main.routes.labels.DisplayLabelRoute
import com.philkes.notallyx.feature.main.routes.labels.LabelsRoute
import com.philkes.notallyx.feature.main.routes.labels.UnlabeledRoute
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesRoute
import com.philkes.notallyx.feature.main.routes.reminders.RemindersRoute
import com.philkes.notallyx.feature.main.routes.settings.SettingsRoute
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.progress.MigrationProgress
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.androidx.compose.koinViewModel

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
    onReminderClick: (BaseNote) -> Unit,
    onExportNotes: (ExportMimeType) -> Unit,
    onMoveSelectedNotes: (Folder) -> Unit,
    onDeleteSelectedNotesForever: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onShowToast: (Int) -> Unit,
    migrationProgressState: MigrationProgress?,
    modifier: Modifier = Modifier,
    mainActivityViewModel: MainActivityViewModel = koinViewModel(),
    notallyFragmentModel: NotesFragmentViewModel = koinViewModel(),
    labelsViewModel: LabelsViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyFragmentModel.preferences,
) {
    NotallyXTheme(theme = preferences.theme.value) {
        val backStack = rememberNavBackStack(startKey)
        val currentKey = (backStack.lastOrNull() as? NotallyNavKey) ?: startKey

        val currentTitle =
            when (val key = currentKey) {
                is NotallyNavKey.Notes -> activity.getString(R.string.notes)
                is NotallyNavKey.Labels -> activity.getString(R.string.labels)
                is NotallyNavKey.Deleted -> activity.getString(R.string.deleted)
                is NotallyNavKey.Archived -> activity.getString(R.string.archived)
                is NotallyNavKey.Reminders -> activity.getString(R.string.reminders)
                is NotallyNavKey.Settings -> activity.getString(R.string.settings)
                is NotallyNavKey.Unlabeled -> activity.getString(R.string.unlabeled)
                is NotallyNavKey.DisplayLabel -> key.label
                is NotallyNavKey.Search -> activity.getString(R.string.search)
            }

        LaunchedEffect(currentKey) {
            currentNavKeyFlow.value = currentKey
            currentTitleFlow.value = currentTitle
            onStartViewChanged(keyMatchesStartKey(currentKey, startKey))
        }

        val activeDialog by mainActivityViewModel.activeDialog.collectAsStateWithLifecycle()
        val mainProgress by mainActivityViewModel.progress.collectAsStateWithLifecycle()
        val fragmentProgress by notallyFragmentModel.progress.collectAsStateWithLifecycle()

        val labelsState = mainActivityViewModel.labels.collectAsStateWithLifecycle(emptyList())
        val actionModeEnabledState =
            notallyFragmentModel.actionMode.enabled.collectAsStateWithLifecycle(false)
        val selectedNoteIds by
            notallyFragmentModel.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
        val selectedNotesCount = selectedNoteIds.size

        val currentFolder by notallyFragmentModel.folder.collectAsStateWithLifecycle(Folder.NOTES)

        val selectedNotes = notallyFragmentModel.actionMode.selectedNotes.values
        val isAllPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned }
        val isAllPinnedToStatus =
            selectedNotes.isNotEmpty() && selectedNotes.all { it.isPinnedToStatus }

        val displayConfig =
            NoteCardDisplayConfig(
                dateFormat = preferences.dateFormatOverview.value,
                timeFormat = preferences.timeFormatOverview.value,
                sortBy = preferences.notesSorting.value.sortedBy,
                textSize = preferences.textSizeOverview.value,
                maxTitleLines = preferences.maxTitle.value,
                maxLines = preferences.maxLines.value,
                maxItems = preferences.maxItems.value,
                hideLabels = preferences.labelTagsHiddenInOverview.value,
                hideImages = preferences.imagesHiddenInOverview.value,
            )

        val actionModeListener =
            remember(notallyFragmentModel, mainActivityViewModel) {
                object : ActionModeListener {
                    override fun onClose() {
                        notallyFragmentModel.actionMode.close(true)
                    }

                    override fun onSelectAll() {
                        notallyFragmentModel.selectAll()
                    }

                    override fun onPin() {
                        notallyFragmentModel.togglePinSelectedNotes()
                    }

                    override fun onLabels() {
                        notallyFragmentModel.onLabelsClick(
                            onNavigateToLabels = { backStack.add(NotallyNavKey.Labels) },
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
                                        notallyFragmentModel.updateBaseNotesLabels(updatedNotes)
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
                        onDeleteSelectedNotesForever()
                    }

                    override fun onDuplicate() {
                        notallyFragmentModel.duplicateSelectedBaseNotes()
                    }

                    override fun onChangeColor() {
                        notallyFragmentModel.onChangeColorClick { colors, currentColor ->
                            mainActivityViewModel.showColorPicker(
                                colors = colors,
                                currentColor = currentColor,
                                onColorSelected = { selectedColor ->
                                    if (selectedColor == BaseNote.COLOR_NEW) {
                                        mainActivityViewModel.showColorPicker(
                                            colors = colors + preferences.defaultNoteColor.value,
                                            currentColor = null,
                                            onColorSelected = { color ->
                                                notallyFragmentModel.colorBaseNote(color)
                                            },
                                        )
                                    } else {
                                        notallyFragmentModel.colorBaseNote(selectedColor)
                                    }
                                },
                            )
                        }
                    }

                    override fun onPinToStatus() {
                        notallyFragmentModel.onPinToStatusClick(
                            activity = activity,
                            onRequestPermission = onRequestNotificationPermission,
                        )
                    }

                    override fun onShare() {
                        if (!notallyFragmentModel.actionMode.isEmpty()) {
                            val baseNote = notallyFragmentModel.actionMode.getFirstNote()
                            activity.shareNote(baseNote)
                        }
                    }

                    override fun onExport() {
                        mainActivityViewModel.showExportFormatDialog { mimeType ->
                            onExportNotes(mimeType)
                        }
                    }
                }
            }

        MainScreen(
            title = currentTitle,
            labels = labelsState.value,
            selectedNavKey = currentKey,
            actionModeEnabled = actionModeEnabledState.value,
            selectedCount = selectedNotesCount,
            currentFolder = currentFolder,
            isAllPinned = isAllPinned,
            isAllPinnedToStatus = isAllPinnedToStatus,
            showFabs =
                currentKey is NotallyNavKey.Notes ||
                    currentKey is NotallyNavKey.DisplayLabel ||
                    currentKey is NotallyNavKey.Unlabeled,
            showSearchOption =
                !preferences.alwaysShowSearchBar.value &&
                    !MainActivity.ACTIVITES_WITHOUT_SEARCH.contains(currentKey),
            isInSearchMode = currentKey is NotallyNavKey.Search,
            onNavigationItemClick = { targetKey ->
                if (currentKey != targetKey) {
                    if (
                        targetKey !is NotallyNavKey.DisplayLabel &&
                            targetKey !is NotallyNavKey.Search
                    ) {
                        while (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                        if (backStack.lastOrNull() != targetKey) {
                            if (backStack.isNotEmpty()) backStack.removeAt(backStack.lastIndex)
                            backStack.add(targetKey)
                        }
                    } else {
                        backStack.add(targetKey)
                    }
                }
            },
            onTakeNoteClick = {
                val intent = Intent(activity, EditNoteActivity::class.java)
                activity.startActivity(onPrepareNewNoteIntent(intent))
            },
            onMakeListClick = {
                val intent = Intent(activity, EditListActivity::class.java)
                activity.startActivity(onPrepareNewNoteIntent(intent))
            },
            onSearchClick = {
                if (currentKey is NotallyNavKey.Search) {
                    notallyFragmentModel.keyword = ""
                    if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                } else {
                    backStack.add(
                        NotallyNavKey.Search(
                            initialFolder = notallyFragmentModel.folder.value,
                            initialLabel = notallyFragmentModel.currentLabel,
                        )
                    )
                }
            },
            onAddClick =
                if (currentKey is NotallyNavKey.Labels) {
                    { labelsViewModel.showAddDialog() }
                } else null,
            actionModeListener = actionModeListener,
            modifier = modifier,
        ) {
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.fillMaxSize(),
                entryProvider =
                    entryProvider {
                        entry<NotallyNavKey.Notes> {
                            NotesRoute(
                                displayConfig = displayConfig,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onReminderClick = onReminderClick,
                            )
                        }

                        entry<NotallyNavKey.Labels> {
                            LabelsRoute(
                                onNavigateToLabel = { label ->
                                    backStack.add(NotallyNavKey.DisplayLabel(label))
                                },
                                onShowToast = onShowToast,
                            )
                        }

                        entry<NotallyNavKey.Deleted> {
                            DeletedRoute(
                                displayConfig = displayConfig,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onReminderClick = onReminderClick,
                            )
                        }

                        entry<NotallyNavKey.Archived> {
                            ArchivedRoute(
                                displayConfig = displayConfig,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onReminderClick = onReminderClick,
                            )
                        }

                        entry<NotallyNavKey.Reminders> {
                            RemindersRoute(
                                displayConfig = displayConfig,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onReminderClick = onReminderClick,
                            )
                        }

                        entry<NotallyNavKey.Settings> { SettingsRoute() }

                        entry<NotallyNavKey.Unlabeled> {
                            UnlabeledRoute(
                                displayConfig = displayConfig,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onReminderClick = onReminderClick,
                            )
                        }

                        entry<NotallyNavKey.DisplayLabel> { key ->
                            DisplayLabelRoute(
                                label = key.label,
                                displayConfig = displayConfig,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onReminderClick = onReminderClick,
                            )
                        }

                        entry<NotallyNavKey.Search> { key ->
                            SearchRoute(
                                initialFolder = key.initialFolder,
                                initialLabel = key.initialLabel,
                                displayConfig = displayConfig,
                                onNoteClick = onNoteClick,
                                onNoteLongClick = onNoteLongClick,
                                onReminderClick = onReminderClick,
                            )
                        }
                    },
            )
        }

        NotallyAppDialogs(
            activeDialog = activeDialog,
            mainProgress = mainProgress,
            fragmentProgress = fragmentProgress,
            migrationProgress = migrationProgressState,
            onDismissDialog = { mainActivityViewModel.dismissDialog() },
        )
    }
}

@Composable
private fun NotallyAppDialogs(
    activeDialog: MainActiveDialog?,
    mainProgress: com.philkes.notallyx.core.common.result.Progress?,
    fragmentProgress: com.philkes.notallyx.core.common.result.Progress?,
    migrationProgress: MigrationProgress?,
    onDismissDialog: () -> Unit,
) {
    when (activeDialog) {
        is MainActiveDialog.ColorPicker -> {
            ColorPickerDialog(
                currentColor = activeDialog.currentColor ?: BaseNote.COLOR_DEFAULT,
                onDismissRequest = onDismissDialog,
                onColorSelected = { color ->
                    onDismissDialog()
                    activeDialog.onColorSelected(color)
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
        listOfNotNull(mainProgress, fragmentProgress, migrationProgress).firstOrNull {
            it.inProgress
        }

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

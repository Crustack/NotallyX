package com.philkes.notallyx.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.philkes.notallyx.Res
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.settings
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    database: NotallyDatabase,
    modifier: Modifier = Modifier,
    initialPreferences: PreferencesUiModel = PreferencesUiModel(),
    onPreferencesChanged: ((PreferencesUiModel) -> Unit)? = null,
    onBack: () -> Unit = {},
) {
    var preferences by remember(initialPreferences) { mutableStateOf(initialPreferences) }
    var activeDialog by remember { mutableStateOf<SettingsDialogState?>(null) }

    fun updatePreferences(newPrefs: PreferencesUiModel) {
        preferences = newPrefs
        onPreferencesChanged?.invoke(newPrefs)
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.BackClicked -> onBack()
            is SettingsAction.DismissDialog -> activeDialog = null
            is SettingsAction.ShowNotesViewDialog -> {
                updatePreferences(
                    preferences.copy(
                        notesView =
                            if (preferences.notesView == NotesView.GRID) NotesView.LIST
                            else NotesView.GRID
                    )
                )
            }
            is SettingsAction.ShowThemeDialog -> {
                activeDialog =
                    SettingsDialogState.Theme(
                        initialTheme = preferences.theme,
                        initialUseDynamicColors = preferences.useDynamicColors,
                        onConfirm = { theme, dynamicColors ->
                            updatePreferences(
                                preferences.copy(
                                    theme = theme,
                                    useDynamicColors = dynamicColors,
                                )
                            )
                            activeDialog = null
                        },
                    )
            }
            is SettingsAction.SetTextSizeNoteEditor -> {
                updatePreferences(preferences.copy(textSizeNoteEditor = action.textSize))
            }
            is SettingsAction.SetTextSizeOverview -> {
                updatePreferences(preferences.copy(textSizeOverview = action.textSize))
            }
            is SettingsAction.ShowAlwaysShowSearchBarDialog -> {
                updatePreferences(
                    preferences.copy(alwaysShowSearchBar = !preferences.alwaysShowSearchBar)
                )
            }
            is SettingsAction.ShowNotesSortDialog -> {
                activeDialog =
                    SettingsDialogState.NotesSort(
                        currentSort = preferences.notesSort,
                        onConfirm = { sort ->
                            updatePreferences(preferences.copy(notesSort = sort))
                            activeDialog = null
                        },
                    )
            }
            is SettingsAction.ShowDateFormatOverviewDialog -> {
                activeDialog =
                    SettingsDialogState.DateTimeFormat(
                        initialDate = preferences.dateFormatOverview,
                        initialTime = preferences.timeFormatOverview,
                        onConfirm = { date, time ->
                            updatePreferences(
                                preferences.copy(
                                    dateFormatOverview = date,
                                    timeFormatOverview = time,
                                )
                            )
                            activeDialog = null
                        },
                    )
            }
            is SettingsAction.ShowDateFormatNoteViewDialog -> {
                activeDialog =
                    SettingsDialogState.DateTimeFormat(
                        initialDate = preferences.dateFormatNoteView,
                        initialTime = preferences.timeFormatNoteView,
                        onConfirm = { date, time ->
                            updatePreferences(
                                preferences.copy(
                                    dateFormatNoteView = date,
                                    timeFormatNoteView = time,
                                )
                            )
                            activeDialog = null
                        },
                    )
            }
            is SettingsAction.ShowListItemSortingDialog -> {
                updatePreferences(
                    preferences.copy(
                        listItemSorting =
                            when (preferences.listItemSorting) {
                                ListItemSort.NO_AUTO_SORT -> ListItemSort.AUTO_SORT_BY_CHECKED
                                ListItemSort.AUTO_SORT_BY_CHECKED ->
                                    ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP
                                ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP ->
                                    ListItemSort.NO_AUTO_SORT
                            }
                    )
                )
            }
            is SettingsAction.ShowDefaultListNoteViewModeDialog -> {
                updatePreferences(
                    preferences.copy(
                        defaultListNoteViewMode =
                            when (preferences.defaultListNoteViewMode) {
                                DefaultListNoteViewMode.READ_ONLY -> DefaultListNoteViewMode.EDIT
                                DefaultListNoteViewMode.EDIT -> DefaultListNoteViewMode.LAST_USED
                                DefaultListNoteViewMode.LAST_USED ->
                                    DefaultListNoteViewMode.READ_ONLY
                            }
                    )
                )
            }
            is SettingsAction.SetAutoSaveAfterIdleTime -> {
                updatePreferences(preferences.copy(autoSaveAfterIdleTime = action.seconds))
            }
            is SettingsAction.UpdateAutoRemoveDays -> {
                updatePreferences(preferences.copy(autoRemoveDeletedNotesAfterDays = action.days))
            }
            is SettingsAction.SetMaxTitle -> {
                updatePreferences(preferences.copy(maxTitle = action.maxTitle))
            }
            is SettingsAction.SetMaxItems -> {
                updatePreferences(preferences.copy(maxItems = action.maxItems))
            }
            is SettingsAction.SetMaxLines -> {
                updatePreferences(preferences.copy(maxLines = action.maxLines))
            }
            is SettingsAction.SetMaxLabels -> {
                updatePreferences(preferences.copy(maxLabels = action.maxLabels))
            }
            is SettingsAction.ShowLabelTagsHiddenDialog -> {
                updatePreferences(
                    preferences.copy(
                        labelTagsHiddenInOverview = !preferences.labelTagsHiddenInOverview
                    )
                )
            }
            is SettingsAction.ShowImagesHiddenDialog -> {
                updatePreferences(
                    preferences.copy(imagesHiddenInOverview = !preferences.imagesHiddenInOverview)
                )
            }
            is SettingsAction.ShowSecureFlagDialog -> {
                updatePreferences(preferences.copy(secureFlag = !preferences.secureFlag))
            }
            is SettingsAction.ShowDataInPublicFolderDialog -> {
                updatePreferences(
                    preferences.copy(dataInPublicFolder = !preferences.dataInPublicFolder)
                )
            }
            else -> {}
        }
    }

    val uiState =
        SettingsUiState(
            preferences = preferences,
            activeDialog = activeDialog,
            isBiometricsAvailable = false,
        )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.NAV_SETTINGS),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SettingsScreen(
                state = uiState,
                onAction = ::onAction,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

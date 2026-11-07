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
    onBack: () -> Unit = {},
) {
    var preferences by remember { mutableStateOf(PreferencesUiModel()) }
    var activeDialog by remember { mutableStateOf<SettingsDialogState?>(null) }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.BackClicked -> onBack()
            is SettingsAction.DismissDialog -> activeDialog = null
            is SettingsAction.ShowNotesViewDialog -> {
                preferences =
                    preferences.copy(
                        notesView =
                            if (preferences.notesView == NotesView.GRID) NotesView.LIST
                            else NotesView.GRID
                    )
            }
            is SettingsAction.ShowThemeDialog -> {
                activeDialog =
                    SettingsDialogState.Theme(
                        initialTheme = preferences.theme,
                        initialUseDynamicColors = preferences.useDynamicColors,
                        onConfirm = { theme, dynamicColors ->
                            preferences =
                                preferences.copy(
                                    theme = theme,
                                    useDynamicColors = dynamicColors,
                                )
                            activeDialog = null
                        },
                    )
            }
            is SettingsAction.SetTextSizeNoteEditor -> {
                preferences = preferences.copy(textSizeNoteEditor = action.textSize)
            }
            is SettingsAction.SetTextSizeOverview -> {
                preferences = preferences.copy(textSizeOverview = action.textSize)
            }
            is SettingsAction.ShowAlwaysShowSearchBarDialog -> {
                preferences =
                    preferences.copy(alwaysShowSearchBar = !preferences.alwaysShowSearchBar)
            }
            is SettingsAction.ShowNotesSortDialog -> {
                activeDialog =
                    SettingsDialogState.NotesSort(
                        currentSort = preferences.notesSort,
                        onConfirm = { sort ->
                            preferences = preferences.copy(notesSort = sort)
                            activeDialog = null
                        },
                    )
            }
            is SettingsAction.ShowListItemSortingDialog -> {
                preferences =
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
            }
            is SettingsAction.ShowDefaultListNoteViewModeDialog -> {
                preferences =
                    preferences.copy(
                        defaultListNoteViewMode =
                            when (preferences.defaultListNoteViewMode) {
                                DefaultListNoteViewMode.READ_ONLY -> DefaultListNoteViewMode.EDIT
                                DefaultListNoteViewMode.EDIT -> DefaultListNoteViewMode.LAST_USED
                                DefaultListNoteViewMode.LAST_USED ->
                                    DefaultListNoteViewMode.READ_ONLY
                            }
                    )
            }
            is SettingsAction.SetAutoSaveAfterIdleTime -> {
                preferences = preferences.copy(autoSaveAfterIdleTime = action.seconds)
            }
            is SettingsAction.UpdateAutoRemoveDays -> {
                preferences = preferences.copy(autoRemoveDeletedNotesAfterDays = action.days)
            }
            is SettingsAction.SetMaxTitle -> {
                preferences = preferences.copy(maxTitle = action.maxTitle)
            }
            is SettingsAction.SetMaxItems -> {
                preferences = preferences.copy(maxItems = action.maxItems)
            }
            is SettingsAction.SetMaxLines -> {
                preferences = preferences.copy(maxLines = action.maxLines)
            }
            is SettingsAction.SetMaxLabels -> {
                preferences = preferences.copy(maxLabels = action.maxLabels)
            }
            is SettingsAction.ShowLabelTagsHiddenDialog -> {
                preferences =
                    preferences.copy(
                        labelTagsHiddenInOverview = !preferences.labelTagsHiddenInOverview
                    )
            }
            is SettingsAction.ShowImagesHiddenDialog -> {
                preferences =
                    preferences.copy(imagesHiddenInOverview = !preferences.imagesHiddenInOverview)
            }
            is SettingsAction.ShowSecureFlagDialog -> {
                preferences = preferences.copy(secureFlag = !preferences.secureFlag)
            }
            is SettingsAction.ShowDataInPublicFolderDialog -> {
                preferences = preferences.copy(dataInPublicFolder = !preferences.dataInPublicFolder)
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

package com.philkes.notallyx.desktop

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.editor.CommonEditListScreen
import com.philkes.notallyx.feature.editor.CommonEditNoteScreen
import com.philkes.notallyx.feature.labels.CommonLabelsScreen
import com.philkes.notallyx.feature.main.routes.archived.CommonArchivedScreen
import com.philkes.notallyx.feature.main.routes.deleted.CommonDeletedScreen
import com.philkes.notallyx.feature.main.routes.notes.CommonNotesScreen
import com.philkes.notallyx.feature.main.routes.search.CommonSearchScreen
import com.philkes.notallyx.feature.main.routes.settings.CommonSettingsScreen
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

sealed interface DesktopScreen {
    data object Notes : DesktopScreen

    data class EditNote(val noteId: Long = 0L) : DesktopScreen

    data class EditList(val noteId: Long = 0L) : DesktopScreen

    data class Search(val folder: Folder = Folder.NOTES, val keyword: String = "") : DesktopScreen

    data object Labels : DesktopScreen

    data object Archived : DesktopScreen

    data object Deleted : DesktopScreen

    data object Settings : DesktopScreen
}

@Composable
fun DesktopAppNavHost(
    database: NotallyDatabase = remember {
        GlobalContext.get().getOrNull<NotallyDatabase>() ?: NotallyDatabase.instance.value!!
    },
    modifier: Modifier = Modifier,
) {
    var currentScreen by remember { mutableStateOf<DesktopScreen>(DesktopScreen.Notes) }
    val noteDao = remember(database) { database.getBaseNoteDao() }
    val labelDao = remember(database) { database.getLabelDao() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp).testTag(TestTags.NAV_DRAWER)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "NotallyX",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("Notes") },
                    selected = currentScreen is DesktopScreen.Notes,
                    onClick = {
                        currentScreen = DesktopScreen.Notes
                        scope.launch { drawerState.close() }
                    },
                    modifier =
                        Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(TestTags.NAV_NOTES),
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search") },
                    selected = currentScreen is DesktopScreen.Search,
                    onClick = {
                        currentScreen = DesktopScreen.Search()
                        scope.launch { drawerState.close() }
                    },
                    modifier =
                        Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(TestTags.NAV_SEARCH),
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Label, contentDescription = null) },
                    label = { Text("Labels") },
                    selected = currentScreen is DesktopScreen.Labels,
                    onClick = {
                        currentScreen = DesktopScreen.Labels
                        scope.launch { drawerState.close() }
                    },
                    modifier =
                        Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(TestTags.NAV_LABELS),
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                    label = { Text("Archive") },
                    selected = currentScreen is DesktopScreen.Archived,
                    onClick = {
                        currentScreen = DesktopScreen.Archived
                        scope.launch { drawerState.close() }
                    },
                    modifier =
                        Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(TestTags.NAV_ARCHIVED),
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Trash") },
                    selected = currentScreen is DesktopScreen.Deleted,
                    onClick = {
                        currentScreen = DesktopScreen.Deleted
                        scope.launch { drawerState.close() }
                    },
                    modifier =
                        Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(TestTags.NAV_DELETED),
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = currentScreen is DesktopScreen.Settings,
                    onClick = {
                        currentScreen = DesktopScreen.Settings
                        scope.launch { drawerState.close() }
                    },
                    modifier =
                        Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            .testTag(TestTags.NAV_SETTINGS),
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            when (val screen = currentScreen) {
                is DesktopScreen.Notes -> {
                    CommonNotesScreen(
                        noteDao = noteDao,
                        onOpenNote = { currentScreen = DesktopScreen.EditNote(it) },
                        onOpenList = { currentScreen = DesktopScreen.EditList(it) },
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                    )
                }

                is DesktopScreen.EditNote -> {
                    CommonEditNoteScreen(
                        noteId = screen.noteId,
                        noteDao = noteDao,
                        onBack = { currentScreen = DesktopScreen.Notes },
                    )
                }

                is DesktopScreen.EditList -> {
                    CommonEditListScreen(
                        noteId = screen.noteId,
                        noteDao = noteDao,
                        onBack = { currentScreen = DesktopScreen.Notes },
                    )
                }

                is DesktopScreen.Search -> {
                    CommonSearchScreen(
                        noteDao = noteDao,
                        initialKeyword = screen.keyword,
                        initialFolder = screen.folder,
                        onOpenNote = { currentScreen = DesktopScreen.EditNote(it) },
                        onOpenList = { currentScreen = DesktopScreen.EditList(it) },
                        onBack = { currentScreen = DesktopScreen.Notes },
                    )
                }

                is DesktopScreen.Labels -> {
                    CommonLabelsScreen(
                        labelDao = labelDao,
                        onBack = { currentScreen = DesktopScreen.Notes },
                        onLabelClick = { label ->
                            currentScreen = DesktopScreen.Search(keyword = label)
                        },
                    )
                }

                is DesktopScreen.Archived -> {
                    CommonArchivedScreen(
                        noteDao = noteDao,
                        onOpenNote = { currentScreen = DesktopScreen.EditNote(it) },
                        onOpenList = { currentScreen = DesktopScreen.EditList(it) },
                        onBack = { currentScreen = DesktopScreen.Notes },
                    )
                }

                is DesktopScreen.Deleted -> {
                    CommonDeletedScreen(
                        noteDao = noteDao,
                        onBack = { currentScreen = DesktopScreen.Notes },
                    )
                }

                is DesktopScreen.Settings -> {
                    CommonSettingsScreen(
                        database = database,
                        onBack = { currentScreen = DesktopScreen.Notes },
                    )
                }
            }
        }
    }
}

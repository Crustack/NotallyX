package com.philkes.notallyx.desktop

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.editor.EditListRoute
import com.philkes.notallyx.feature.editor.EditNoteRoute
import com.philkes.notallyx.feature.labels.LabelsRoute
import com.philkes.notallyx.feature.main.routes.archived.ArchivedRoute
import com.philkes.notallyx.feature.main.routes.deleted.DeletedRoute
import com.philkes.notallyx.feature.main.routes.labels.UnlabeledRoute
import com.philkes.notallyx.feature.main.routes.notes.NotesRoute
import com.philkes.notallyx.feature.main.routes.search.SearchRoute
import com.philkes.notallyx.feature.reminder.RemindersRoute
import com.philkes.notallyx.feature.settings.SettingsRoute
import com.philkes.notallyx.feature.settings.toNoteCardDisplayConfig
import com.philkes.notallyx.navigation.ListDetailScene
import com.philkes.notallyx.navigation.LocalNavigator
import com.philkes.notallyx.navigation.Navigator
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.navigation.rememberListDetailSceneStrategy
import com.philkes.notallyx.navigation.rememberNavigationState
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

@Composable
fun DesktopNavDisplay(
    database: NotallyDatabase = remember {
        GlobalContext.get().getOrNull<NotallyDatabase>() ?: NotallyDatabase.instance.value!!
    },
    preferencesStore: DesktopPreferencesStore = remember {
        GlobalContext.get().getOrNull<DesktopPreferencesStore>()
            ?: DesktopPreferencesStore(DesktopPathProvider())
    },
    modifier: Modifier = Modifier,
) {
    val preferences by preferencesStore.preferences.collectAsState()
    val displayConfig = remember(preferences) { preferences.toNoteCardDisplayConfig() }

    val navigationState = rememberNavigationState(startRoute = NotallyNavKey.Notes)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    val noteDao = remember(database) { database.getBaseNoteDao() }
    val labelDao = remember(database) { database.getLabelDao() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val keyboardModifier =
        Modifier.focusRequester(focusRequester).focusable().onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                if ((event.isCtrlPressed || event.isMetaPressed) && event.key == Key.N) {
                    navigator.navigate(NotallyNavKey.EditNote(noteId = 0L))
                    true
                } else if (event.key == Key.Escape) {
                    if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                        true
                    } else {
                        navigator.goBack()
                    }
                } else {
                    false
                }
            } else {
                false
            }
        }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    NotallyXTheme(theme = preferences.theme) {
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
                        selected = navigator.currentTopLevelRoute == NotallyNavKey.Notes,
                        onClick = {
                            navigator.navigate(NotallyNavKey.Notes)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search") },
                        selected = navigator.currentRoute is NotallyNavKey.Search,
                        onClick = {
                            navigator.navigate(NotallyNavKey.Search())
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.Label, contentDescription = null) },
                        label = { Text("Labels") },
                        selected = navigator.currentTopLevelRoute == NotallyNavKey.Labels,
                        onClick = {
                            navigator.navigate(NotallyNavKey.Labels)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        label = { Text("Reminders") },
                        selected = navigator.currentTopLevelRoute == NotallyNavKey.Reminders,
                        onClick = {
                            navigator.navigate(NotallyNavKey.Reminders)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Archive, contentDescription = null) },
                        label = { Text("Archive") },
                        selected = navigator.currentTopLevelRoute == NotallyNavKey.Archived,
                        onClick = {
                            navigator.navigate(NotallyNavKey.Archived)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        label = { Text("Trash") },
                        selected = navigator.currentTopLevelRoute == NotallyNavKey.Deleted,
                        onClick = {
                            navigator.navigate(NotallyNavKey.Deleted)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = navigator.currentTopLevelRoute == NotallyNavKey.Settings,
                        onClick = {
                            navigator.navigate(NotallyNavKey.Settings)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }
            },
            modifier = modifier.fillMaxSize().then(keyboardModifier),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                val entryProvider =
                    entryProvider<NavKey> {
                        entry<NotallyNavKey.Notes>(metadata = ListDetailScene.listPane()) {
                            NotesRoute(
                                noteDao = noteDao,
                                displayConfig = displayConfig,
                                isGrid = preferences.notesView == NotesView.GRID,
                                onToggleGrid = { isGrid ->
                                    preferencesStore.updatePreferences {
                                        it.copy(
                                            notesView =
                                                if (isGrid) NotesView.GRID else NotesView.LIST
                                        )
                                    }
                                },
                                onOpenNote = {
                                    navigator.navigate(NotallyNavKey.EditNote(noteId = it))
                                },
                                onOpenList = {
                                    navigator.navigate(NotallyNavKey.EditList(noteId = it))
                                },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                                onCreateNote = {
                                    navigator.navigate(NotallyNavKey.EditNote(noteId = 0L))
                                },
                                onCreateList = {
                                    navigator.navigate(NotallyNavKey.EditList(noteId = 0L))
                                },
                            )
                        }

                        entry<NotallyNavKey.Unlabeled>(metadata = ListDetailScene.listPane()) {
                            UnlabeledRoute(
                                noteDao = noteDao,
                                displayConfig = displayConfig,
                                onOpenNote = {
                                    navigator.navigate(NotallyNavKey.EditNote(noteId = it))
                                },
                                onOpenList = {
                                    navigator.navigate(NotallyNavKey.EditList(noteId = it))
                                },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                            )
                        }

                        entry<NotallyNavKey.DisplayLabel>(
                            metadata = ListDetailScene.listPane(isTopLevel = false)
                        ) { key ->
                            SearchRoute(
                                noteDao = noteDao,
                                displayConfig = displayConfig,
                                initialKeyword = key.label,
                                initialFolder = Folder.NOTES,
                                onOpenNote = {
                                    navigator.navigate(NotallyNavKey.EditNote(noteId = it))
                                },
                                onOpenList = {
                                    navigator.navigate(NotallyNavKey.EditList(noteId = it))
                                },
                                onBack = { navigator.goBack() },
                            )
                        }

                        entry<NotallyNavKey.Search>(
                            metadata = ListDetailScene.listPane(isTopLevel = false)
                        ) { key ->
                            SearchRoute(
                                noteDao = noteDao,
                                displayConfig = displayConfig,
                                initialKeyword = key.initialLabel ?: "",
                                initialFolder = key.initialFolder ?: Folder.NOTES,
                                onOpenNote = {
                                    navigator.navigate(NotallyNavKey.EditNote(noteId = it))
                                },
                                onOpenList = {
                                    navigator.navigate(NotallyNavKey.EditList(noteId = it))
                                },
                                onBack = { navigator.goBack() },
                            )
                        }

                        entry<NotallyNavKey.Labels>(metadata = ListDetailScene.topLevel()) {
                            LabelsRoute(
                                labelDao = labelDao,
                                onBack = { navigator.goBack() },
                                onLabelClick = { label ->
                                    navigator.navigate(NotallyNavKey.Search(initialLabel = label))
                                },
                            )
                        }

                        entry<NotallyNavKey.Reminders>(metadata = ListDetailScene.listPane()) {
                            RemindersRoute(
                                noteDao = noteDao,
                                displayConfig = displayConfig,
                                onOpenNote = {
                                    navigator.navigate(NotallyNavKey.EditNote(noteId = it))
                                },
                                onOpenList = {
                                    navigator.navigate(NotallyNavKey.EditList(noteId = it))
                                },
                                onOpenDrawer = { scope.launch { drawerState.open() } },
                            )
                        }

                        entry<NotallyNavKey.Archived>(metadata = ListDetailScene.listPane()) {
                            ArchivedRoute(
                                noteDao = noteDao,
                                displayConfig = displayConfig,
                                onOpenNote = {
                                    navigator.navigate(NotallyNavKey.EditNote(noteId = it))
                                },
                                onOpenList = {
                                    navigator.navigate(NotallyNavKey.EditList(noteId = it))
                                },
                                onBack = { navigator.goBack() },
                            )
                        }

                        entry<NotallyNavKey.Deleted>(metadata = ListDetailScene.listPane()) {
                            DeletedRoute(
                                noteDao = noteDao,
                                displayConfig = displayConfig,
                                onBack = { navigator.goBack() },
                            )
                        }

                        entry<NotallyNavKey.Settings>(metadata = ListDetailScene.topLevel()) {
                            SettingsRoute(
                                database = database,
                                initialPreferences = preferences,
                                onPreferencesChanged = { updated ->
                                    preferencesStore.setPreferences(updated)
                                },
                                onBack = { navigator.goBack() },
                            )
                        }

                        entry<NotallyNavKey.EditNote>(metadata = ListDetailScene.detailPane()) { key
                            ->
                            EditNoteRoute(
                                key = key,
                                noteDao = noteDao,
                                onBack = { navigator.goBack() },
                            )
                        }

                        entry<NotallyNavKey.EditList>(metadata = ListDetailScene.detailPane()) { key
                            ->
                            EditListRoute(
                                key = key,
                                noteDao = noteDao,
                                onBack = { navigator.goBack() },
                            )
                        }
                    }

                CompositionLocalProvider(LocalNavigator provides navigator) {
                    NavDisplay(
                        entries = navigationState.toDecoratedEntries(entryProvider),
                        sceneStrategies = listOf(listDetailStrategy),
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
                        onBack = { navigator.goBack() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

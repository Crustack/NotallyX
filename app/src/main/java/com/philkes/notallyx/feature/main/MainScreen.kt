package com.philkes.notallyx.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.navigation.NotallyNavKey
import kotlinx.coroutines.launch

internal interface ActionModeListener {
    fun onClose() {}

    fun onSelectAll() {}

    fun onPin() {}

    fun onLabels() {}

    fun onDelete() {}

    fun onRestore() {}

    fun onArchive() {}

    fun onDeleteForever() {}

    fun onDuplicate() {}

    fun onChangeColor() {}

    fun onPinToStatus() {}

    fun onShare() {}

    fun onExport() {}
}

@Composable
internal fun MainDrawerContent(
    selectedNavKey: NotallyNavKey,
    title: String,
    labels: List<Label>,
    onNavigationItemClick: (NotallyNavKey) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        DrawerHeaderView()
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.notes)) },
            selected = selectedNavKey is NotallyNavKey.Notes,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.home),
                    contentDescription = null,
                )
            },
            onClick = {
                onCloseDrawer()
                onNavigationItemClick(NotallyNavKey.Notes)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.unlabeled)) },
            selected = selectedNavKey is NotallyNavKey.Unlabeled,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.label_off),
                    contentDescription = null,
                )
            },
            onClick = {
                onCloseDrawer()
                onNavigationItemClick(NotallyNavKey.Unlabeled)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.labels)) },
            selected = selectedNavKey is NotallyNavKey.Labels,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.label_more),
                    contentDescription = null,
                )
            },
            onClick = {
                onCloseDrawer()
                onNavigationItemClick(NotallyNavKey.Labels)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        labels.forEach { label ->
            NavigationDrawerItem(
                label = { Text(text = label.value) },
                selected =
                    selectedNavKey is NotallyNavKey.DisplayLabel &&
                        selectedNavKey.label == label.value,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.label),
                        contentDescription = null,
                    )
                },
                onClick = {
                    onCloseDrawer()
                    onNavigationItemClick(NotallyNavKey.DisplayLabel(label.value))
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.deleted)) },
            selected = selectedNavKey is NotallyNavKey.Deleted,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.delete),
                    contentDescription = null,
                )
            },
            onClick = {
                onCloseDrawer()
                onNavigationItemClick(NotallyNavKey.Deleted)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.archived)) },
            selected = selectedNavKey is NotallyNavKey.Archived,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.archive),
                    contentDescription = null,
                )
            },
            onClick = {
                onCloseDrawer()
                onNavigationItemClick(NotallyNavKey.Archived)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.reminders)) },
            selected = selectedNavKey is NotallyNavKey.Reminders,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.notifications),
                    contentDescription = null,
                )
            },
            onClick = {
                onCloseDrawer()
                onNavigationItemClick(NotallyNavKey.Reminders)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        NavigationDrawerItem(
            label = { Text(text = stringResource(R.string.settings)) },
            selected = selectedNavKey is NotallyNavKey.Settings,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.settings),
                    contentDescription = null,
                )
            },
            onClick = {
                onCloseDrawer()
                onNavigationItemClick(NotallyNavKey.Settings)
            },
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

/** Composable replacing activity_main.xml layout and drawer navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    title: String,
    modifier: Modifier = Modifier,
    labels: List<Label> = emptyList(),
    selectedNavKey: NotallyNavKey = NotallyNavKey.Notes,
    actionModeEnabled: Boolean = false,
    selectedCount: Int = 0,
    currentFolder: Folder = Folder.NOTES,
    isAllPinned: Boolean = false,
    isAllPinnedToStatus: Boolean = false,
    showFabs: Boolean = true,
    showSearchOption: Boolean = true,
    isInSearchMode: Boolean = false,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    onNavigationItemClick: (NotallyNavKey) -> Unit = {},
    onTakeNoteClick: () -> Unit = {},
    onMakeListClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAddClick: (() -> Unit)? = null,
    actionModeListener: ActionModeListener = object : ActionModeListener {},
    content: @Composable () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                selectedNavKey = selectedNavKey,
                title = title,
                labels = labels,
                onNavigationItemClick = onNavigationItemClick,
                onCloseDrawer = { scope.launch { drawerState.close() } },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) {
        Scaffold(
            topBar = {
                if (actionModeEnabled) {
                    TopAppBar(
                        title = { Text(text = selectedCount.toString()) },
                        navigationIcon = {
                            IconButton(onClick = { actionModeListener.onClose() }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.close),
                                    contentDescription = stringResource(R.string.cancel),
                                )
                            }
                        },
                        actions = {
                            ActionModeActions(
                                currentFolder = currentFolder,
                                selectedCount = selectedCount,
                                isAllPinned = isAllPinned,
                                isAllPinnedToStatus = isAllPinnedToStatus,
                                listener = actionModeListener,
                            )
                        },
                    )
                } else {
                    TopAppBar(
                        title = { Text(text = title) },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        if (drawerState.isClosed) drawerState.open()
                                        else drawerState.close()
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.menu),
                                    contentDescription = stringResource(R.string.app_name),
                                )
                            }
                        },
                        actions = {
                            if (onAddClick != null) {
                                IconButton(onClick = onAddClick) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.add),
                                        contentDescription = stringResource(R.string.add_label),
                                    )
                                }
                            }
                            if (showSearchOption) {
                                IconButton(
                                    onClick = onSearchClick,
                                    modifier = Modifier.testTag(TestTags.NAVIGATION),
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id =
                                                    if (isInSearchMode) R.drawable.close
                                                    else R.drawable.search
                                            ),
                                        contentDescription =
                                            stringResource(
                                                if (isInSearchMode) R.string.cancel
                                                else R.string.search
                                            ),
                                    )
                                }
                            }
                        },
                    )
                }
            },
            floatingActionButton = {
                if (showFabs && !actionModeEnabled) {
                    Column(horizontalAlignment = Alignment.End) {
                        FloatingActionButton(
                            onClick = onMakeListClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(bottom = 16.dp),
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.checkbox),
                                contentDescription = stringResource(R.string.make_list),
                            )
                        }
                        FloatingActionButton(
                            onClick = onTakeNoteClick,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.edit),
                                contentDescription = stringResource(R.string.take_note),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) { content() }
        }
    }
}

@Composable
private fun ActionModeActions(
    currentFolder: Folder,
    selectedCount: Int,
    isAllPinned: Boolean,
    isAllPinnedToStatus: Boolean,
    listener: ActionModeListener,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        var showOverflowMenu by remember { mutableStateOf(false) }

        IconButton(onClick = { listener.onSelectAll() }) {
            Icon(
                painter = painterResource(id = R.drawable.select_all),
                contentDescription = stringResource(R.string.select_all),
            )
        }

        when (currentFolder) {
            Folder.NOTES -> {
                IconButton(onClick = { listener.onPin() }) {
                    Icon(
                        painter =
                            painterResource(
                                id = if (isAllPinned) R.drawable.unpin else R.drawable.pin
                            ),
                        contentDescription =
                            stringResource(if (isAllPinned) R.string.unpin else R.string.pin),
                    )
                }
                IconButton(onClick = { listener.onLabels() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.label),
                        contentDescription = stringResource(R.string.labels),
                    )
                }
                IconButton(onClick = { listener.onDelete() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
            Folder.ARCHIVED -> {
                IconButton(onClick = { listener.onRestore() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.unarchive),
                        contentDescription = stringResource(R.string.unarchive),
                    )
                }
                IconButton(onClick = { listener.onDelete() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
            Folder.DELETED -> {
                IconButton(onClick = { listener.onRestore() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.restore),
                        contentDescription = stringResource(R.string.restore),
                    )
                }
                IconButton(onClick = { listener.onDeleteForever() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = stringResource(R.string.delete_forever),
                    )
                }
            }
        }

        Box {
            IconButton(onClick = { showOverflowMenu = true }) {
                Icon(
                    painter = painterResource(id = R.drawable.more_vert),
                    contentDescription = stringResource(R.string.tap_for_more_options),
                )
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false },
            ) {
                when (currentFolder) {
                    Folder.NOTES -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicate)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.archive)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.archive),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onArchive()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.change_color)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.change_color),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onChangeColor()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isAllPinnedToStatus) R.string.unpin_from_status_bar
                                        else R.string.pin_to_status_bar
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter =
                                        painterResource(
                                            if (isAllPinnedToStatus) R.drawable.pinboard_filled
                                            else R.drawable.pinboard
                                        ),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onPinToStatus()
                            },
                        )
                    }
                    Folder.ARCHIVED -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicate)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onDuplicate()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(
                                        if (isAllPinned) R.string.unpin else R.string.pin
                                    )
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter =
                                        painterResource(
                                            if (isAllPinned) R.drawable.unpin else R.drawable.pin
                                        ),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onPin()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.labels)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.label),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onLabels()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.change_color)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.change_color),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onChangeColor()
                            },
                        )
                    }
                    Folder.DELETED -> {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.change_color)) },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.change_color),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                listener.onChangeColor()
                            },
                        )
                    }
                }

                if (selectedCount == 1) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.share)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            listener.onShare()
                        },
                    )
                }

                DropdownMenuItem(
                    text = { Text(stringResource(R.string.export)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.export),
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        showOverflowMenu = false
                        listener.onExport()
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionModeActionsPreview() {
    NotallyXTheme {
        ActionModeActions(
            currentFolder = Folder.NOTES,
            selectedCount = 2,
            isAllPinned = false,
            isAllPinnedToStatus = false,
            listener = object : ActionModeListener {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    NotallyXTheme { MainScreen(title = "Notes") }
}

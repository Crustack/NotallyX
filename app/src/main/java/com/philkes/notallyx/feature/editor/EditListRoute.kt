package com.philkes.notallyx.feature.editor

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.common.extensions.shareNote
import com.philkes.notallyx.core.common.extensions.uniqueCurrentMillis
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.ListItemBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.changes.ListItemsChange
import com.philkes.notallyx.feature.editor.history.computeListItemBodyDelta
import com.philkes.notallyx.feature.editor.listitem.checkItem
import com.philkes.notallyx.feature.editor.listitem.cloneList
import com.philkes.notallyx.feature.editor.listitem.initChildren
import com.philkes.notallyx.feature.editor.listitem.initOrders
import com.philkes.notallyx.feature.editor.listitem.moveWithChildren
import com.philkes.notallyx.feature.editor.listitem.removeWithChildren
import com.philkes.notallyx.feature.editor.listitem.splitByChecked
import com.philkes.notallyx.feature.editor.screens.EditChangeHandlers
import com.philkes.notallyx.feature.editor.screens.EditClickHandlers
import com.philkes.notallyx.feature.editor.screens.EditListScreen
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteState
import com.philkes.notallyx.feature.editor.screens.EditorBottomSheets
import com.philkes.notallyx.feature.editor.screens.EditorDialogs
import com.philkes.notallyx.feature.editor.screens.SearchState
import com.philkes.notallyx.feature.editor.screens.openActionSelectionBottomSheet
import com.philkes.notallyx.feature.migration.backup.exportNote
import com.philkes.notallyx.feature.reminder.cancelPinAndReminders
import com.philkes.notallyx.feature.widget.NoteWidget
import com.philkes.notallyx.navigation.AudioDeletedResult
import com.philkes.notallyx.navigation.AudioRecordedResult
import com.philkes.notallyx.navigation.ImagesDeletedResult
import com.philkes.notallyx.navigation.LocalNavigator
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.navigation.RemindersUpdatedResult
import com.philkes.notallyx.navigation.SelectedLabelsResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditListRoute(
    key: NotallyNavKey.EditList,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    notallyModel: EditActivityViewModel = koinViewModel(),
    preferences: NotallyXPreferences = notallyModel.preferences,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavigator.current

    val changeHistory = remember { ChangeHistory() }

    val titleState = remember { mutableStateOf(notallyModel.title) }
    val labelsState = remember { mutableStateOf<List<String>>(notallyModel.labels.toList()) }
    val colorIntState = remember { mutableIntStateOf(context.extractColor(notallyModel.color)) }
    val itemsVersionState = remember { mutableIntStateOf(0) }

    val isSearchActiveState = remember { mutableStateOf(false) }
    val searchQueryState = remember { mutableStateOf("") }
    val searchResultPosState = remember { mutableIntStateOf(0) }
    val searchResultsCountState = remember { mutableIntStateOf(0) }

    var lastEditTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun resetIdleTimer() {
        lastEditTime = System.currentTimeMillis()
    }

    fun updateModel() {
        notallyModel.modifiedTimestamp = System.currentTimeMillis()
        notallyModel.title = titleState.value
    }

    suspend fun saveNote(checkBackupOnSave: Boolean = true): Long {
        updateModel()
        val id = notallyModel.saveNote(checkBackupOnSave)
        NoteWidget.sendBroadcast(context.applicationContext as Application, longArrayOf(id))
        return id
    }

    suspend fun checkSaveAndExit() {
        updateModel()
        if (notallyModel.isEmpty()) {
            notallyModel.deleteBaseNote(checkAutoSave = false)
        } else if (notallyModel.isModified()) {
            saveNote(checkBackupOnSave = true)
        } else {
            notallyModel.checkBackupOnSave()
        }
        onBackClick()
    }

    BackHandler { coroutineScope.launch { checkSaveAndExit() } }

    DisposableEffect(Unit) {
        onDispose {
            if (notallyModel.isModified() && !notallyModel.isEmpty()) {
                updateModel()
                runBlocking(Dispatchers.IO) { notallyModel.saveNote(checkBackupOnSave = false) }
            }
        }
    }

    LaunchedEffect(key) {
        if (
            notallyModel.uiState is UiState.Success &&
                (key.noteId != null && key.noteId == notallyModel.id)
        ) {
            return@LaunchedEffect
        }
        notallyModel.type = Type.LIST
        notallyModel.changeUiState(UiState.Loading)
        notallyModel.setState(key.noteId ?: 0L)
        if (notallyModel.isNewNote) {
            if (key.initialFolder != null) {
                notallyModel.folder = key.initialFolder
            }
            if (key.initialLabel != null) {
                notallyModel.setLabels(listOf(key.initialLabel))
            }
            if (notallyModel.items.isEmpty()) {
                notallyModel.items.add(
                    ListItem(
                        body = "",
                        checked = false,
                        isChild = false,
                        order = 0,
                        children = mutableListOf(),
                        id = 0,
                    )
                )
            }
        }
        titleState.value = notallyModel.title
        labelsState.value = notallyModel.labels.toList()
        colorIntState.intValue = context.extractColor(notallyModel.color)
        itemsVersionState.intValue++
        notallyModel.changeUiState(UiState.Success)
    }

    LaunchedEffect(lastEditTime) {
        val idleTime = preferences.autoSaveAfterIdleTime.value
        if (idleTime > -1) {
            delay(idleTime.toLong() * 1000)
            if (notallyModel.isModified() && !notallyModel.isEmpty()) {
                saveNote(checkBackupOnSave = false)
            }
        }
    }

    // --- Search Handling ---
    fun updateSearchResults(query: String) {
        if (query.isEmpty()) {
            searchResultsCountState.intValue = 0
            searchResultPosState.intValue = 0
            return
        }
        val text = notallyModel.items.joinToString(" ") { it.body }
        val occurrences = text.findAllOccurrences(query)
        searchResultsCountState.intValue = occurrences.size
        if (occurrences.isNotEmpty()) {
            if (searchResultPosState.intValue >= occurrences.size) {
                searchResultPosState.intValue = occurrences.size - 1
            }
        } else {
            searchResultPosState.intValue = 0
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQueryState.value = query
        updateSearchResults(query)
    }

    fun onNextSearchResult() {
        val total = searchResultsCountState.intValue
        if (total > 0) {
            searchResultPosState.intValue = (searchResultPosState.intValue + 1) % total
        }
    }

    fun onPrevSearchResult() {
        val total = searchResultsCountState.intValue
        if (total > 0) {
            val current = searchResultPosState.intValue
            searchResultPosState.intValue = if (current > 0) current - 1 else total - 1
        }
    }

    fun endSearch() {
        isSearchActiveState.value = false
        searchQueryState.value = ""
        searchResultsCountState.intValue = 0
        searchResultPosState.intValue = 0
    }

    // --- List Item Changes & Delta History ---
    fun applyListChange(action: () -> Unit) {
        val oldList = notallyModel.items.cloneList()
        action()
        initChildren(notallyModel.items)
        val newList = notallyModel.items.cloneList()
        changeHistory.push(
            ListItemsChange(
                notallyModel = notallyModel,
                oldItems = oldList,
                newItems = newList,
                onItemsUpdated = { itemsVersionState.intValue++ },
            )
        )
        itemsVersionState.intValue++
        resetIdleTimer()
    }

    fun pushListItemBodyChange(change: ListItemBodyDeltaChange) {
        val top = changeHistory.peek()
        if (
            !changeHistory.canRedo.value &&
                (top is ListItemBodyDeltaChange) &&
                top.tryMergeWith(change)
        ) {
            // Merged
        } else {
            changeHistory.push(change)
        }
    }

    fun applyListItemTextChange(
        item: ListItem,
        newBody: AnnotatedString,
        oldBody: AnnotatedString = AnnotatedString(item.body),
        oldSelection: TextRange = TextRange.Zero,
        newSelection: TextRange = TextRange.Zero,
    ) {
        if (oldBody != newBody) {
            val delta =
                computeListItemBodyDelta(
                    oldValue = oldBody,
                    newValue = newBody,
                    itemId = item.id,
                    notallyModel = notallyModel,
                    oldSelection = oldSelection,
                    newSelection = newSelection,
                    onItemUpdated = { _, _ ->
                        itemsVersionState.intValue++
                        if (isSearchActiveState.value) {
                            updateSearchResults(searchQueryState.value)
                        }
                        resetIdleTimer()
                    },
                )
            if (delta != null) {
                pushListItemBodyChange(delta)
            }
            item.body = newBody.text
            itemsVersionState.intValue++
            if (isSearchActiveState.value) {
                updateSearchResults(searchQueryState.value)
            }
            resetIdleTimer()
        }
    }

    fun applyListItemTextChange(item: ListItem, newText: String) {
        applyListItemTextChange(
            item = item,
            newBody = AnnotatedString(newText),
            oldBody = AnnotatedString(item.body),
        )
    }

    fun handleListAction(action: EditListAction) {
        when (action) {
            EditListAction.CHECK_ALL -> {
                applyListChange {
                    val now = uniqueCurrentMillis()
                    notallyModel.items.forEach { item ->
                        item.checked = true
                        item.checkedTimestamp = now
                    }
                }
            }
            EditListAction.UNCHECK_ALL -> {
                applyListChange {
                    notallyModel.items.forEach { item ->
                        item.checked = false
                        item.checkedTimestamp = null
                    }
                }
            }
            EditListAction.DELETE_CHECKED -> {
                applyListChange {
                    notallyModel.items.removeAll { it.checked }
                    initOrders(notallyModel.items)
                }
            }
        }
    }

    // --- Navigation 3 Result Handlers ---
    ResultEffect<SelectedLabelsResult> { result ->
        if (result.selectedLabels != notallyModel.labels) {
            notallyModel.setLabels(result.selectedLabels)
            labelsState.value = result.selectedLabels
            resetIdleTimer()
        }
    }

    ResultEffect<ImagesDeletedResult> { result ->
        val toDelete =
            ArrayList(notallyModel.images.value.filter { it.localName in result.deletedImages })
        if (toDelete.isNotEmpty()) {
            notallyModel.deleteImages(toDelete)
        }
    }

    ResultEffect<AudioDeletedResult> { result ->
        val audio = notallyModel.audios.value.find { it.name == result.audioName }
        if (audio != null) {
            notallyModel.deleteAudio(audio)
        }
    }

    ResultEffect<AudioRecordedResult> { result ->
        if (result.success) {
            notallyModel.addAudio()
        }
    }

    ResultEffect<RemindersUpdatedResult> { result ->
        if (notallyModel.id == 0L) {
            notallyModel.id = result.noteId
        }
        notallyModel.refreshOriginalNote()
    }

    // --- Activity Result Launchers ---
    val addImagesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val uri = res.data?.data
                val clipData = res.data?.clipData
                if (uri != null) {
                    notallyModel.addImages(arrayOf(uri))
                } else if (clipData != null) {
                    val uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    notallyModel.addImages(uris)
                }
            }
        }

    val attachFilesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val uri = res.data?.data
                val clipData = res.data?.clipData
                if (uri != null) {
                    notallyModel.addFiles(arrayOf(uri))
                } else if (clipData != null) {
                    val uris = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    notallyModel.addFiles(uris)
                }
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                res.data?.data?.let { uri ->
                    activity?.let { act ->
                        notallyModel.exportNoteToFile(
                            uri,
                            notallyModel.getBaseNote(),
                            act.findViewById(android.R.id.content),
                        )
                    }
                }
            }
        }

    val recordAudioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                if (notallyModel.audioRoot != null) {
                    navigator?.navigate(NotallyNavKey.RecordAudio)
                } else {
                    context.showToast(R.string.insert_an_sd_card_audio)
                }
            } else {
                notallyModel.activeDialog.value =
                    EditorDialogState.RejectionConfirm(
                        msgResId = R.string.to_record_audio,
                        onConfirm = {
                            val intent =
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                )
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        },
                    )
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val act = activity ?: return@rememberLauncherForActivityResult
            if (granted) {
                notallyModel.isPinnedToStatus = true
                act.refreshStatusBarPin(notallyModel.getBaseNote())
            } else {
                notallyModel.activeDialog.value =
                    EditorDialogState.RejectionConfirm(
                        msgResId = R.string.to_pin_note_status_bar,
                        onConfirm = {
                            val intent =
                                Intent(
                                    android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                )
                            intent.data = Uri.parse("package:${context.packageName}")
                            context.startActivity(intent)
                        },
                    )
            }
        }

    fun moveNote(toFolder: Folder) {
        if (toFolder != Folder.NOTES) {
            activity?.cancelPinAndReminders(notallyModel.id, notallyModel.reminders.value)
        }
        notallyModel.folder = toFolder
        coroutineScope.launch {
            notallyModel.saveNote(checkBackupOnSave = false)
            onBackClick()
        }
    }

    // --- Action Handler ---
    val actionHandler =
        remember(notallyModel, navigator, activity, notificationPermissionLauncher) {
            object : NoteActionHandler {
                override fun pinToStatus() {
                    val act = activity ?: return
                    if (!notallyModel.isPinnedToStatus) {
                        act.checkNotificationPermission(
                            permissionLauncher = notificationPermissionLauncher,
                            onShowRationale = { msgResId, onConfirm ->
                                notallyModel.activeDialog.value =
                                    EditorDialogState.PermissionRationale(msgResId, onConfirm)
                            },
                        ) {
                            notallyModel.isPinnedToStatus = true
                            act.refreshStatusBarPin(notallyModel.getBaseNote())
                        }
                    } else {
                        notallyModel.isPinnedToStatus = false
                        act.refreshStatusBarPin(notallyModel.getBaseNote())
                    }
                }

                override fun recordAudio() {
                    val permission = Manifest.permission.RECORD_AUDIO
                    if (
                        ContextCompat.checkSelfPermission(context, permission) !=
                            PackageManager.PERMISSION_GRANTED
                    ) {
                        if (activity?.shouldShowRequestPermissionRationale(permission) == true) {
                            notallyModel.activeDialog.value =
                                EditorDialogState.AudioPermissionRationale(
                                    onConfirm = { recordAudioPermissionLauncher.launch(permission) }
                                )
                        } else {
                            recordAudioPermissionLauncher.launch(permission)
                        }
                    } else {
                        if (notallyModel.audioRoot != null) {
                            navigator?.navigate(NotallyNavKey.RecordAudio)
                        } else {
                            context.showToast(R.string.insert_an_sd_card_audio)
                        }
                    }
                }

                override fun addImages() {
                    if (notallyModel.imageRoot != null) {
                        val intent =
                            Intent(Intent.ACTION_GET_CONTENT)
                                .apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                .wrapWithChooser(context)
                        addImagesLauncher.launch(intent)
                    } else context.showToast(R.string.insert_an_sd_card_images)
                }

                override fun attachFiles() {
                    if (notallyModel.filesRoot != null) {
                        val intent =
                            Intent(Intent.ACTION_GET_CONTENT)
                                .apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                .wrapWithChooser(context)
                        attachFilesLauncher.launch(intent)
                    } else context.showToast(R.string.insert_an_sd_card_files)
                }

                override fun addNoteLink() {}

                override fun updateNoteLink(range: TextRange) {}

                override fun viewImage(image: FileAttachment) {
                    val position = notallyModel.images.value.indexOf(image)
                    navigator?.navigate(
                        NotallyNavKey.ViewImage(
                            noteId = notallyModel.id,
                            initialIndex = if (position != -1) position else 0,
                        )
                    )
                }

                override fun viewFile(fileAttachment: FileAttachment) {
                    val file =
                        context.applicationContext.resolveAttachmentFile(
                            SUBFOLDER_FILES,
                            fileAttachment.localName,
                        )
                    if (file != null && file.exists()) {
                        val uri = context.getUriForFile(file)
                        context.viewFile(uri, fileAttachment.mimeType)
                    }
                }

                override fun playAudio(audio: Audio) {
                    navigator?.navigate(
                        NotallyNavKey.PlayAudio(
                            audioName = audio.name,
                            duration = audio.duration,
                            timestamp = audio.timestamp,
                        )
                    )
                }

                override fun handleAction(action: EditAction) {
                    when (action) {
                        EditAction.SEARCH -> {
                            isSearchActiveState.value = true
                            searchQueryState.value = ""
                            searchResultsCountState.intValue = 0
                            searchResultPosState.intValue = 0
                        }
                        EditAction.PIN -> {
                            notallyModel.pinned = !notallyModel.pinned
                        }
                        EditAction.PIN_TO_STATUS -> pinToStatus()
                        EditAction.REMINDERS -> {
                            coroutineScope.launch {
                                val noteId =
                                    if (notallyModel.id != 0L) notallyModel.id
                                    else saveNote(checkBackupOnSave = false)
                                navigator?.navigate(NotallyNavKey.NoteReminders(noteId))
                            }
                        }
                        EditAction.LABELS -> {
                            navigator?.navigate(NotallyNavKey.SelectLabels(notallyModel.labels))
                        }
                        EditAction.CHANGE_COLOR -> {
                            coroutineScope.launch {
                                val dbWrapper =
                                    (context as? ContextWrapper) ?: activity ?: return@launch
                                val db =
                                    NotallyDatabase.getDatabase(dbWrapper).value ?: return@launch
                                val colors: MutableSet<ColorString> =
                                    withContext(Dispatchers.IO) {
                                            db.getBaseNoteDao().getAllColors().toMutableSet()
                                        }
                                        .toMutableSet()
                                if (colors.none { it == notallyModel.color }) {
                                    colors.add(notallyModel.color)
                                }
                                notallyModel.activeDialog.value =
                                    EditorDialogState.ColorPicker(
                                        currentColor = notallyModel.color,
                                        colors = colors,
                                        defaultColor = preferences.defaultNoteColor.value,
                                        onColorSelected = { selectedColor ->
                                            notallyModel.color = selectedColor
                                            colorIntState.intValue =
                                                context.extractColor(selectedColor)
                                            resetIdleTimer()
                                        },
                                        onColorChanged = { oldColor, newColor ->
                                            coroutineScope.launch(Dispatchers.IO) {
                                                db.getBaseNoteDao().updateColor(oldColor, newColor)
                                            }
                                            if (notallyModel.color == oldColor) {
                                                notallyModel.color = newColor
                                                colorIntState.intValue =
                                                    context.extractColor(newColor)
                                            }
                                            if (preferences.defaultNoteColor.value == oldColor) {
                                                preferences.defaultNoteColor.save(newColor)
                                            }
                                            resetIdleTimer()
                                        },
                                        onColorDeleted = { colorToDelete, newColor ->
                                            coroutineScope.launch(Dispatchers.IO) {
                                                db.getBaseNoteDao()
                                                    .updateColor(colorToDelete, newColor)
                                            }
                                            if (notallyModel.color == colorToDelete) {
                                                notallyModel.color = newColor
                                                colorIntState.intValue =
                                                    context.extractColor(newColor)
                                            }
                                            if (
                                                preferences.defaultNoteColor.value == colorToDelete
                                            ) {
                                                preferences.defaultNoteColor.save(
                                                    BaseNote.COLOR_DEFAULT
                                                )
                                            }
                                            resetIdleTimer()
                                        },
                                        onSetDefaultColor = { newDefault ->
                                            preferences.defaultNoteColor.save(newDefault)
                                        },
                                    )
                            }
                        }
                        EditAction.DUPLICATE -> {
                            coroutineScope.launch {
                                saveNote()
                                val duplicateId =
                                    notallyModel.duplicateNote(notallyModel.getBaseNote())
                                onBackClick()
                                navigator?.navigate(NotallyNavKey.EditList(duplicateId))
                            }
                        }
                        EditAction.EXPORT -> {
                            notallyModel.activeBottomSheet.value =
                                EditorBottomSheetState.Export { mimeType ->
                                    notallyModel.selectedExportMimeType = mimeType
                                    activity?.let { act ->
                                        act.exportNote(
                                            notallyModel.getBaseNote(),
                                            mimeType,
                                            exportLauncher,
                                        )
                                    }
                                }
                        }
                        EditAction.SHARE -> {
                            activity?.shareNote(notallyModel.getBaseNote())
                        }
                        EditAction.DELETE -> moveNote(Folder.DELETED)
                        EditAction.ARCHIVE -> {
                            if (notallyModel.folder == Folder.ARCHIVED) {
                                moveNote(Folder.NOTES)
                            } else {
                                moveNote(Folder.ARCHIVED)
                            }
                        }
                        EditAction.RESTORE -> moveNote(Folder.NOTES)
                        EditAction.TOGGLE_VIEW_MODE -> notallyModel.toggleViewMode()
                        EditAction.CONVERT -> {
                            coroutineScope.launch {
                                updateModel()
                                notallyModel.convertTo(Type.NOTE)
                                onBackClick()
                                navigator?.navigate(NotallyNavKey.EditNote(notallyModel.id))
                            }
                        }
                        EditAction.DELETE_FOREVER -> {
                            notallyModel.activeDialog.value =
                                EditorDialogState.DeleteForeverConfirm(
                                    onConfirm = {
                                        coroutineScope.launch {
                                            notallyModel.deleteBaseNote()
                                            onBackClick()
                                        }
                                    }
                                )
                        }
                    }
                }

                override fun handleListAction(action: EditListAction) {
                    handleListAction(action)
                }
            }
        }

    val topActionsState = remember {
        mutableStateOf(preferences.getSafeEditNoteActivityTopActions())
    }
    val bottomActionState = remember {
        mutableStateOf(preferences.editNoteActivityBottomAction.value)
    }

    when (notallyModel.uiState) {
        UiState.Loading -> Box(modifier = modifier.fillMaxSize()) {}
        UiState.Success -> {
            val imagesState = notallyModel.images.collectAsStateWithLifecycle(emptyList())
            val filesState = notallyModel.files.collectAsStateWithLifecycle(emptyList())
            val audiosState = notallyModel.audios.collectAsStateWithLifecycle(emptyList())
            val remindersState = notallyModel.reminders.collectAsStateWithLifecycle(emptyList())
            val canUndoState = changeHistory.canUndo.collectAsStateWithLifecycle(false)
            val canRedoState = changeHistory.canRedo.collectAsStateWithLifecycle(false)
            val stackPointerState = changeHistory.stackPointer.collectAsStateWithLifecycle(-1)
            val viewModeState = notallyModel.viewMode.collectAsStateWithLifecycle(NoteViewMode.EDIT)

            // Read stackPointerState and itemsVersionState to trigger recomposition on changes
            stackPointerState.value
            itemsVersionState.intValue

            val listItemSort = preferences.listItemSorting.value
            val (activeItemsList, checkedItemsList) =
                when (listItemSort) {
                    ListItemSort.NO_AUTO_SORT -> {
                        Pair(notallyModel.items.toList(), emptyList())
                    }
                    ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP -> {
                        val (checked, active) = notallyModel.items.splitByChecked()
                        Pair(active, checked.sortedByDescending { it.checkedTimestamp ?: 0L })
                    }
                    else -> {
                        val (checked, active) = notallyModel.items.splitByChecked()
                        Pair(active, checked)
                    }
                }

            val focusFirstItem = remember { notallyModel.isNewNote }

            val noteState =
                EditNoteState(
                    title = titleState.value,
                    items = activeItemsList,
                    checkedItems = checkedItemsList,
                    createdTimestamp = notallyModel.timestamp,
                    modifiedTimestamp = notallyModel.modifiedTimestamp,
                    images = imagesState.value,
                    mediaRoot = notallyModel.imageRoot,
                    files = filesState.value,
                    audios = audiosState.value,
                    labels = labelsState.value,
                    reminders = remindersState.value,
                    pinned = notallyModel.pinned,
                    viewMode = viewModeState.value,
                    folder = notallyModel.folder,
                    type = notallyModel.type,
                    isPinnedToStatus = notallyModel.isPinnedToStatus,
                )

            val preferencesState =
                EditNotePreferences(
                    notesSortBy = preferences.notesSorting.value.sortedBy,
                    dateFormat = preferences.dateFormatNoteView.value,
                    timeFormat = preferences.timeFormatNoteView.value,
                    topActions = topActionsState.value,
                    bottomAction = bottomActionState.value,
                )

            val searchState =
                SearchState(
                    isActive = isSearchActiveState.value,
                    query = searchQueryState.value,
                    resultsCount = searchResultsCountState.intValue,
                    resultPos = searchResultPosState.intValue,
                )

            EditListScreen(
                noteState = noteState,
                modifier = modifier,
                preferences = preferencesState,
                searchState = searchState,
                backgroundColor = Color(colorIntState.intValue),
                canUndo = canUndoState.value,
                canRedo = canRedoState.value,
                changeHandlers =
                    object : EditChangeHandlers {
                        override fun onTitleChange(title: String) {
                            titleState.value = title
                            notallyModel.title = title
                            resetIdleTimer()
                        }

                        override fun onSearchQueryChange(query: String) {
                            onSearchQueryChange(query)
                        }

                        override fun onItemTextChange(item: ListItem, text: String) {
                            if (item.body != text) {
                                applyListItemTextChange(item, text)
                            }
                        }

                        override fun onItemTextChange(item: ListItem, text: AnnotatedString) {
                            if (item.body != text.text) {
                                applyListItemTextChange(item, text)
                            }
                        }

                        override fun onItemCheckedChange(item: ListItem, checked: Boolean) {
                            applyListChange { notallyModel.items.checkItem(item, checked) }
                        }

                        override fun onItemDelete(item: ListItem) {
                            applyListChange { notallyModel.items.removeWithChildren(item) }
                        }

                        override fun onItemSwipeRight(item: ListItem) {
                            val sectionList =
                                if (item.checked) checkedItemsList else activeItemsList
                            val idx = sectionList.indexOf(item)
                            if (idx > 0) {
                                applyListChange { item.isChild = true }
                            }
                        }

                        override fun onItemSwipeLeft(item: ListItem) {
                            if (item.isChild) {
                                applyListChange { item.isChild = false }
                            }
                        }

                        override fun onItemMove(
                            fromIndex: Int,
                            toIndex: Int,
                            originalItem: ListItem?,
                        ) {
                            if (
                                fromIndex in notallyModel.items.indices &&
                                    toIndex in 0..notallyModel.items.size
                            ) {
                                applyListChange {
                                    notallyModel.items.moveWithChildren(
                                        fromIndex,
                                        toIndex,
                                        originalItem,
                                    )
                                }
                            }
                        }
                    },
                clickHandlers =
                    object : EditClickHandlers {
                        override fun onBackClick() {
                            coroutineScope.launch { checkSaveAndExit() }
                        }

                        override fun onAddItemClick() {
                            applyListChange {
                                val maxId = notallyModel.items.maxOfOrNull { it.id } ?: -1
                                val newItem =
                                    ListItem(
                                        body = "",
                                        checked = false,
                                        isChild = false,
                                        order = notallyModel.items.size,
                                        children = mutableListOf(),
                                        id = maxId + 1,
                                    )
                                notallyModel.items.add(newItem)
                            }
                        }

                        override fun onMoreOptionsClick() {
                            val topActions = preferences.getSafeEditNoteActivityTopActions()
                            val bottomAction = preferences.editNoteActivityBottomAction.value
                            notallyModel.activeBottomSheet.value =
                                EditorBottomSheetState.MoreList(topActions, bottomAction)
                        }

                        override fun onActionClick(action: EditAction) {
                            actionHandler.handleAction(action)
                        }

                        override fun onActionLongClick(action: EditAction) {
                            openActionSelectionBottomSheet(
                                context = context,
                                model = notallyModel,
                                preferences = preferences,
                                action = action,
                                onUpdateTopActions = { topActionsState.value = it },
                                onUpdateBottomAction = { bottomActionState.value = it },
                            )
                        }

                        override fun onUndoClick() {
                            if (changeHistory.canUndo.value) {
                                changeHistory.undo()
                            }
                        }

                        override fun onRedoClick() {
                            if (changeHistory.canRedo.value) {
                                changeHistory.redo()
                            }
                        }

                        override fun onImageClick(image: FileAttachment) {
                            actionHandler.viewImage(image)
                        }

                        override fun onFileClick(file: FileAttachment) {
                            actionHandler.viewFile(file)
                        }

                        override fun onFileDelete(file: FileAttachment) {
                            notallyModel.deleteFiles(arrayListOf(file))
                        }

                        override fun onAudioClick(audio: Audio) {
                            actionHandler.playAudio(audio)
                        }

                        override fun onNextSearchResult() {
                            onNextSearchResult()
                        }

                        override fun onPrevSearchResult() {
                            onPrevSearchResult()
                        }

                        override fun onCloseSearch() {
                            endSearch()
                        }
                    },
                focusFirstItem = focusFirstItem,
            )

            EditorDialogs(notallyModel)
            EditorBottomSheets(
                model = notallyModel,
                actionHandler = actionHandler,
                backgroundColor =
                    if (colorIntState.intValue != 0) Color(colorIntState.intValue) else null,
            )
        }
    }
}

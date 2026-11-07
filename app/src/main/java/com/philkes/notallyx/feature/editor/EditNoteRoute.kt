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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.SUBFOLDER_FILES
import com.philkes.notallyx.core.common.extensions.findAllOccurrences
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.resolveAttachmentFile
import com.philkes.notallyx.core.common.extensions.shareNote
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.addOrUpdateLink
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.clearFormatting
import com.philkes.notallyx.core.ui.extensions.extractColor
import com.philkes.notallyx.core.ui.extensions.preserveStylesFrom
import com.philkes.notallyx.core.ui.extensions.removeLink
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.extensions.toggleBold
import com.philkes.notallyx.core.ui.extensions.toggleItalic
import com.philkes.notallyx.core.ui.extensions.toggleMonospace
import com.philkes.notallyx.core.ui.extensions.toggleStrikethrough
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.createNoteUrl
import com.philkes.notallyx.data.model.extensions.getNoteIdFromUrl
import com.philkes.notallyx.data.model.extensions.getNoteTypeFromUrl
import com.philkes.notallyx.data.model.extensions.isNoteUrl
import com.philkes.notallyx.feature.editor.formatting.getFormattingState
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.NoteBodyDeltaChange
import com.philkes.notallyx.feature.editor.history.computeBodyDelta
import com.philkes.notallyx.feature.editor.screens.EditChangeHandlers
import com.philkes.notallyx.feature.editor.screens.EditClickHandlers
import com.philkes.notallyx.feature.editor.screens.EditNotePreferences
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
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
import com.philkes.notallyx.navigation.PickedNoteResult
import com.philkes.notallyx.navigation.RemindersUpdatedResult
import com.philkes.notallyx.navigation.SelectedLabelsResult
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditNoteRoute(
    key: NotallyNavKey.EditNote,
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
    val bodyValueState = remember { mutableStateOf(notallyModel.body) }
    val labelsState = remember { mutableStateOf<List<String>>(notallyModel.labels.toList()) }
    val colorIntState = remember { mutableIntStateOf(context.extractColor(notallyModel.color)) }
    val isFormattingMenuVisible = remember { mutableStateOf(false) }

    val isSearchActiveState = remember { mutableStateOf(false) }
    val searchQueryState = remember { mutableStateOf("") }
    val searchResultPosState = remember { mutableIntStateOf(0) }
    val searchResultsCountState = remember { mutableIntStateOf(0) }

    var selectedLinkRange by remember { mutableStateOf<TextRange?>(null) }
    var lastEditTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun resetIdleTimer() {
        lastEditTime = System.currentTimeMillis()
    }

    fun updateModel() {
        notallyModel.modifiedTimestamp = System.currentTimeMillis()
        notallyModel.title = titleState.value
        notallyModel.body = bodyValueState.value
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
        notallyModel.type = Type.NOTE
        notallyModel.changeUiState(UiState.Loading)
        notallyModel.setState(key.noteId ?: 0L)
        if (notallyModel.isNewNote) {
            if (key.initialFolder != null) {
                notallyModel.folder = key.initialFolder
            }
            if (key.initialLabel != null) {
                notallyModel.setLabels(listOf(key.initialLabel))
            }
            if (key.initialTitle != null) {
                notallyModel.title = key.initialTitle
            }
            if (key.initialBody != null) {
                notallyModel.body =
                    TextFieldValue(
                        annotatedString = AnnotatedString(key.initialBody),
                        selection = TextRange(key.initialBody.length),
                    )
            }
            if (key.initialImageUris.isNotEmpty()) {
                notallyModel.addImages(key.initialImageUris.map { Uri.parse(it) }.toTypedArray())
            }
            if (key.initialFileUris.isNotEmpty()) {
                notallyModel.addFiles(key.initialFileUris.map { Uri.parse(it) }.toTypedArray())
            }
        }
        titleState.value = notallyModel.title
        bodyValueState.value = notallyModel.body
        labelsState.value = notallyModel.labels.toList()
        colorIntState.intValue = context.extractColor(notallyModel.color)
        notallyModel.changeUiState(UiState.Success)
    }

    LaunchedEffect(lastEditTime) {
        val idleTime = preferences.autoSaveAfterIdleTime.value
        if (idleTime > -1) {
            delay((idleTime.toLong() * 1000).milliseconds)
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
        val text = notallyModel.body.text
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

    // --- Body delta history ---
    fun pushBodyChange(change: NoteBodyDeltaChange) {
        val top = changeHistory.peek()
        if (
            !changeHistory.canRedo.value && (top is NoteBodyDeltaChange) && top.tryMergeWith(change)
        ) {
            // Merged
        } else {
            changeHistory.push(change)
        }
    }

    fun applyBodyChange(newBody: TextFieldValue) {
        val oldBody = bodyValueState.value
        if (newBody.annotatedString != oldBody.annotatedString) {
            val delta =
                computeBodyDelta(
                    oldValue = oldBody,
                    newValue = newBody,
                    notallyModel = notallyModel,
                    onBodyUpdated = { restored ->
                        bodyValueState.value = restored
                        if (isSearchActiveState.value) {
                            updateSearchResults(searchQueryState.value)
                        }
                        resetIdleTimer()
                    },
                )
            if (delta != null) {
                pushBodyChange(delta)
            }
        }
        bodyValueState.value = newBody
        notallyModel.body = newBody
        resetIdleTimer()
    }

    fun applyLink(url: String, displayText: String, range: TextRange? = null) {
        applyBodyChange(bodyValueState.value.addOrUpdateLink(url, displayText, range))
    }

    fun removeLink(range: TextRange) {
        applyBodyChange(bodyValueState.value.removeLink(range))
    }

    fun openLink(url: String) {
        if (url.isNoteUrl()) {
            val targetId = url.getNoteIdFromUrl()
            val targetType = url.getNoteTypeFromUrl()
            if (targetType == Type.NOTE) {
                navigator?.navigate(NotallyNavKey.EditNote(targetId))
            } else {
                navigator?.navigate(NotallyNavKey.EditList(targetId))
            }
        } else {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri).wrapWithChooser(context)
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                context.showToast(R.string.cant_open_link)
            }
        }
    }

    fun showLinkDialog() {
        val selection = bodyValueState.value.selection
        val min = selection.min
        val max = selection.max
        val hasSelection = min < max
        val selectedText = if (hasSelection) bodyValueState.value.text.substring(min, max) else ""
        val existingUrl =
            if (hasSelection) {
                bodyValueState.value.annotatedString
                    .getStringAnnotations("URL", min, max)
                    .firstOrNull()
                    ?.item ?: ""
            } else ""

        val isNoteUrl = existingUrl.isNoteUrl()
        val range = if (hasSelection) TextRange(min, max) else null
        notallyModel.activeDialog.value =
            EditorDialogState.EditLink(
                urlBefore = existingUrl,
                displayTextBefore = selectedText,
                isNoteUrl = isNoteUrl,
                onConfirm = { urlAfter, displayTextAfter ->
                    if (urlAfter != null) {
                        applyLink(urlAfter, displayTextAfter, range)
                    }
                },
            )
    }

    fun showLinkOptionsDialog(url: String, displayText: String, range: TextRange) {
        val isNoteUrl = url.isNoteUrl()
        notallyModel.activeDialog.value =
            EditorDialogState.LinkOptions(
                url = url,
                displayText = displayText,
                isNoteUrl = isNoteUrl,
                onOpen = { openLink(url) },
                onEdit = {
                    notallyModel.activeDialog.value =
                        EditorDialogState.EditLink(
                            urlBefore = url,
                            displayTextBefore = displayText,
                            isNoteUrl = isNoteUrl,
                            onConfirm = { urlAfter, displayTextAfter ->
                                if (urlAfter != null) {
                                    applyLink(urlAfter, displayTextAfter, range)
                                }
                            },
                        )
                },
                onRemove = { removeLink(range) },
            )
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

    val fallbackNoteText = androidx.compose.ui.res.stringResource(R.string.note)
    ResultEffect<PickedNoteResult> { result ->
        val (title, url) =
            result.noteTitle.ifEmpty { fallbackNoteText } to
                result.noteId.createNoteUrl(result.noteType)
        val range = selectedLinkRange
        if (range != null && range.min < range.max) {
            val start = range.min
            val end = range.max
            val builder = AnnotatedString.Builder(notallyModel.body.text)
            notallyModel.body.annotatedString.spanStyles.forEach { sRange ->
                builder.addStyle(sRange.item, sRange.start, sRange.end)
            }
            notallyModel.body.annotatedString
                .getStringAnnotations("URL", 0, notallyModel.body.text.length)
                .filterNot { it.start == start && it.end == end }
                .forEach { uRange ->
                    builder.addStringAnnotation("URL", uRange.item, uRange.start, uRange.end)
                }
            builder.addStringAnnotation("URL", url, start, end)
            builder.addStyle(
                SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                start,
                end,
            )
            val newAnnotatedString = builder.toAnnotatedString()
            val newBody =
                TextFieldValue(
                    annotatedString = newAnnotatedString,
                    selection = notallyModel.body.selection,
                )
            bodyValueState.value = newBody
            notallyModel.body = newBody
        } else {
            val textToAppend = title
            val start = notallyModel.body.text.length
            val end = start + textToAppend.length
            val builder = AnnotatedString.Builder(notallyModel.body.annotatedString)
            builder.append(textToAppend)
            builder.addStringAnnotation("URL", url, start, end)
            builder.addStyle(
                SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
                start,
                end,
            )
            val newAnnotatedString = builder.toAnnotatedString()
            val newBody =
                TextFieldValue(
                    annotatedString = newAnnotatedString,
                    selection = TextRange(newAnnotatedString.text.length),
                )
            bodyValueState.value = newBody
            notallyModel.body = newBody
        }
        selectedLinkRange = null
        resetIdleTimer()
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

                override fun addNoteLink() {
                    selectedLinkRange = null
                    navigator?.navigate(NotallyNavKey.PickNote(excludeNoteId = notallyModel.id))
                }

                override fun updateNoteLink(range: TextRange) {
                    selectedLinkRange = range
                    navigator?.navigate(NotallyNavKey.PickNote(excludeNoteId = notallyModel.id))
                }

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
                                navigator?.navigate(NotallyNavKey.EditNote(duplicateId))
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
                                notallyModel.convertTo(Type.LIST)
                                onBackClick()
                                navigator?.navigate(NotallyNavKey.EditList(notallyModel.id))
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

                override fun handleListAction(action: EditListAction) {}
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
            val viewModeState = notallyModel.viewMode.collectAsStateWithLifecycle(NoteViewMode.EDIT)

            val noteState =
                EditNoteState(
                    title = titleState.value,
                    bodyValue = bodyValueState.value,
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

            val formattingState =
                remember(bodyValueState.value) {
                    val selection = bodyValueState.value.selection
                    bodyValueState.value.annotatedString.getFormattingState(
                        selection.min,
                        selection.max,
                    )
                }

            EditNoteScreen(
                noteState = noteState,
                modifier = modifier,
                preferences = preferencesState,
                searchState = searchState,
                backgroundColor = Color(colorIntState.intValue),
                canUndo = canUndoState.value,
                canRedo = canRedoState.value,
                isFormattingMenuVisible = isFormattingMenuVisible.value,
                formattingState = formattingState,
                changeHandlers =
                    object : EditChangeHandlers {
                        override fun onTitleChange(title: String) {
                            titleState.value = title
                            notallyModel.title = title
                            resetIdleTimer()
                        }

                        override fun onBodyValueChange(bodyValue: TextFieldValue) {
                            val newText = bodyValue.text
                            val oldText = notallyModel.body.text
                            if (newText != oldText) {
                                if (isSearchActiveState.value) {
                                    updateSearchResults(searchQueryState.value)
                                }
                                resetIdleTimer()
                            }
                            if (bodyValue.selection.min != bodyValue.selection.max) {
                                isFormattingMenuVisible.value = true
                            }
                            val updatedValue = bodyValue.preserveStylesFrom(bodyValueState.value)
                            if (
                                updatedValue.annotatedString != bodyValueState.value.annotatedString
                            ) {
                                val delta =
                                    computeBodyDelta(
                                        oldValue = bodyValueState.value,
                                        newValue = updatedValue,
                                        notallyModel = notallyModel,
                                        onBodyUpdated = { restored ->
                                            bodyValueState.value = restored
                                            if (isSearchActiveState.value) {
                                                updateSearchResults(searchQueryState.value)
                                            }
                                            resetIdleTimer()
                                        },
                                    )
                                if (delta != null) {
                                    pushBodyChange(delta)
                                }
                            }
                            bodyValueState.value = updatedValue
                            notallyModel.body = updatedValue
                        }

                        override fun onSearchQueryChange(query: String) {
                            onSearchQueryChange(query)
                        }
                    },
                clickHandlers =
                    object : EditClickHandlers {
                        override fun onBackClick() {
                            coroutineScope.launch { checkSaveAndExit() }
                        }

                        override fun onAddItemClick() {
                            notallyModel.activeBottomSheet.value = EditorBottomSheetState.AddNote
                        }

                        override fun onMoreOptionsClick() {
                            val topActions = preferences.getSafeEditNoteActivityTopActions()
                            val bottomAction = preferences.editNoteActivityBottomAction.value
                            notallyModel.activeBottomSheet.value =
                                EditorBottomSheetState.MoreNote(topActions, bottomAction)
                        }

                        override fun onFormatClick() {
                            isFormattingMenuVisible.value = !isFormattingMenuVisible.value
                        }

                        override fun onCloseFormattingClick() {
                            isFormattingMenuVisible.value = false
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

                        override fun onBoldClick() {
                            applyBodyChange(bodyValueState.value.toggleBold())
                        }

                        override fun onItalicClick() {
                            applyBodyChange(bodyValueState.value.toggleItalic())
                        }

                        override fun onStrikethroughClick() {
                            applyBodyChange(bodyValueState.value.toggleStrikethrough())
                        }

                        override fun onMonospaceClick() {
                            applyBodyChange(bodyValueState.value.toggleMonospace())
                        }

                        override fun onLinkClick() {
                            showLinkDialog()
                        }

                        override fun onLinkClicked(
                            url: String,
                            displayText: String,
                            range: TextRange,
                        ) {
                            showLinkOptionsDialog(url, displayText, range)
                        }

                        override fun onClearFormattingClick() {
                            applyBodyChange(bodyValueState.value.clearFormatting())
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

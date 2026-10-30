package com.philkes.notallyx.feature.editor

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.philkes.notallyx.core.common.extensions.shareNote
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.feature.editor.screens.EditNoteScreen
import com.philkes.notallyx.feature.editor.screens.openActionSelectionBottomSheet
import com.philkes.notallyx.feature.migration.backup.exportNote
import com.philkes.notallyx.navigation.AudioDeletedResult
import com.philkes.notallyx.navigation.AudioRecordedResult
import com.philkes.notallyx.navigation.ImagesDeletedResult
import com.philkes.notallyx.navigation.LocalNavigator
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.navigation.PickedNoteResult
import com.philkes.notallyx.navigation.RemindersUpdatedResult
import com.philkes.notallyx.navigation.SelectedLabelsResult
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun EditNoteRoute(
    key: NotallyNavKey.EditNote,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: EditNoteViewModel = koinViewModel(),
    preferences: NotallyXPreferences = koinInject(),
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val activity = LocalActivity.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler { viewModel.onAction(EditNoteAction.BackClicked) }

    LaunchedEffect(key) { viewModel.onAction(EditNoteAction.Init(key)) }

    // --- Activity Result Launchers ---
    val addImagesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val uri = res.data?.data
                val clipData = res.data?.clipData
                val uris =
                    if (uri != null) {
                        arrayOf(uri)
                    } else if (clipData != null) {
                        Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    } else {
                        emptyArray()
                    }
                if (uris.isNotEmpty()) {
                    viewModel.onAction(EditNoteAction.ImagesPicked(uris))
                }
            }
        }

    val attachFilesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                val uri = res.data?.data
                val clipData = res.data?.clipData
                val uris =
                    if (uri != null) {
                        arrayOf(uri)
                    } else if (clipData != null) {
                        Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    } else {
                        emptyArray()
                    }
                if (uris.isNotEmpty()) {
                    viewModel.onAction(EditNoteAction.FilesPicked(uris))
                }
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            if (res.resultCode == Activity.RESULT_OK) {
                res.data?.data?.let { uri ->
                    viewModel.onAction(EditNoteAction.ExportFilePicked(uri))
                }
            }
        }

    val recordAudioPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onAction(EditNoteAction.AudioPermissionResult(granted))
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onAction(EditNoteAction.NotificationPermissionResult(granted))
        }

    // --- Navigation 3 Result Effects ---
    ResultEffect<SelectedLabelsResult> { result ->
        viewModel.onAction(EditNoteAction.LabelsSelected(result.selectedLabels))
    }
    ResultEffect<ImagesDeletedResult> { result ->
        viewModel.onAction(EditNoteAction.ImagesDeleted(result.deletedImages))
    }
    ResultEffect<AudioDeletedResult> { result ->
        viewModel.onAction(EditNoteAction.AudioDeleted(result.audioName))
    }
    ResultEffect<AudioRecordedResult> { result ->
        viewModel.onAction(EditNoteAction.AudioRecorded(result.success))
    }
    ResultEffect<RemindersUpdatedResult> { result ->
        viewModel.onAction(EditNoteAction.RemindersUpdated(result.noteId))
    }
    ResultEffect<PickedNoteResult> { result ->
        viewModel.onAction(
            EditNoteAction.NotePicked(result.noteId, result.noteTitle, result.noteType)
        )
    }

    // --- Side Effects ---
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is EditNoteSideEffect.ShowToast -> {
                    Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
                }
                is EditNoteSideEffect.OpenUrl -> {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, effect.url.toUri()).wrapWithChooser(context)
                    )
                }
                is EditNoteSideEffect.ShareNote -> {
                    activity?.shareNote(effect.note)
                }
                is EditNoteSideEffect.Navigate -> {
                    navigator?.navigate(effect.navKey)
                }
                is EditNoteSideEffect.ExportNote -> {
                    activity?.exportNote(effect.note, effect.mimeType, exportLauncher)
                }
                is EditNoteSideEffect.ExportFile -> {
                    activity?.let { act ->
                        viewModel.exportNoteToFile(
                            effect.uri,
                            effect.note,
                            act.findViewById(android.R.id.content),
                        )
                    }
                }
                is EditNoteSideEffect.RequestAudioPermission -> {
                    recordAudioPermissionLauncher.launch(effect.permission)
                }
                is EditNoteSideEffect.RequestNotificationPermission -> {
                    activity?.checkNotificationPermission(
                        permissionLauncher = notificationPermissionLauncher,
                        onShowRationale = { msgResId, onConfirm ->
                            viewModel.showPermissionRationale(msgResId, onConfirm)
                        },
                    ) {
                        viewModel.onAction(EditNoteAction.NotificationPermissionResult(true))
                    }
                }
                is EditNoteSideEffect.RefreshStatusBarPin -> {
                    activity?.refreshStatusBarPin(effect.note)
                }
                is EditNoteSideEffect.LaunchAddImages -> {
                    val intent =
                        Intent(Intent.ACTION_GET_CONTENT)
                            .apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, effect.allowMultiple)
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            .wrapWithChooser(context)
                    addImagesLauncher.launch(intent)
                }
                is EditNoteSideEffect.LaunchAttachFiles -> {
                    val intent =
                        Intent(Intent.ACTION_GET_CONTENT)
                            .apply {
                                type = "*/*"
                                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, effect.allowMultiple)
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            .wrapWithChooser(context)
                    attachFilesLauncher.launch(intent)
                }
                is EditNoteSideEffect.OpenActionSelection -> {
                    openActionSelectionBottomSheet(
                        context = context,
                        model = viewModel,
                        preferences = preferences,
                        action = effect.action,
                        onUpdateTopActions = {
                            viewModel.onAction(EditNoteAction.UpdateTopActions(it))
                        },
                        onUpdateBottomAction = {
                            viewModel.onAction(EditNoteAction.UpdateBottomAction(it))
                        },
                    )
                }
                is EditNoteSideEffect.ViewAttachmentFile -> {
                    context.viewFile(effect.uri, effect.mimeType)
                }
                is EditNoteSideEffect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    EditNoteScreen(
        state = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
        noteModel = viewModel,
        actionHandler = viewModel,
    )
}

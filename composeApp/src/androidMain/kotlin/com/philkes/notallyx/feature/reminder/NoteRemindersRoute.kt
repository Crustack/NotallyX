package com.philkes.notallyx.feature.reminder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import org.koin.androidx.compose.koinViewModel

@Composable
fun NoteRemindersRoute(
    noteId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NoteRemindersViewModel = koinViewModel(),
) {
    val activity = LocalActivity.current as? FragmentActivity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(onBack = onBackClick)

    val alarmPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onAction(
                NoteRemindersAction.PermissionResult(
                    permissionType = PermissionType.ALARM,
                    isGranted = activity?.canScheduleAlarms() == true,
                )
            )
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            viewModel.onAction(
                NoteRemindersAction.PermissionResult(
                    permissionType = PermissionType.NOTIFICATION,
                    isGranted = isGranted,
                )
            )
        }

    LaunchedEffect(noteId) { viewModel.onAction(NoteRemindersAction.Init(noteId)) }

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is NoteRemindersSideEffect.CheckPermissions -> {
                    activity?.checkNotificationPermission(
                        permissionLauncher = notificationPermissionLauncher,
                        alsoCheckAlarmPermission = true,
                        alarmPermissionResultLauncher = alarmPermissionLauncher,
                        onShowRationale = { msgResId, _ ->
                            viewModel.onAction(
                                NoteRemindersAction.ShowPermissionRationale(
                                    messageResId = msgResId,
                                    permissionType = PermissionType.NOTIFICATION,
                                )
                            )
                        },
                    ) {
                        viewModel.onAction(
                            NoteRemindersAction.CheckPermissionsResult(hasPermission = true)
                        )
                    }
                }

                is NoteRemindersSideEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }

                is NoteRemindersSideEffect.RequestAlarmPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent =
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = "package:${activity?.packageName}".toUri()
                            }
                        alarmPermissionLauncher.launch(intent)
                    }
                }
            }
        }
    }

    NoteRemindersScreen(
        state = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBackClick,
        modifier = modifier,
    )
}

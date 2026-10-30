package com.philkes.notallyx.feature.main.routes.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_JSON
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.common.extensions.openExternalMediaFolder
import com.philkes.notallyx.core.common.extensions.reportBug
import com.philkes.notallyx.core.common.extensions.viewLogs
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.security.AuthenticatorProvider
import com.philkes.notallyx.core.ui.extensions.setEnabledSecureFlag
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesRouteViewModel.Companion.CURRENT_LABEL_EMPTY
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.feature.settings.SettingsAction
import com.philkes.notallyx.feature.settings.SettingsScreen
import com.philkes.notallyx.feature.settings.SettingsSideEffect
import com.philkes.notallyx.feature.settings.SettingsViewModel
import java.io.File
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    notallyRouteModel: NotesRouteViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity

    LaunchedEffect(Unit) { notallyRouteModel.currentLabel = CURRENT_LABEL_EMPTY }

    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val importBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.onAction(
                        SettingsAction.ImportBackupSelected(
                            uri,
                            context.contentResolver.getType(uri),
                        )
                    )
                }
            }
        }

    val importRawDatabaseLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.onAction(SettingsAction.ImportRawDatabaseSelected(uri))
                }
            }
        }

    val importOtherLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.onAction(SettingsAction.ImportOtherFileSelected(uri))
                }
            }
        }

    val exportBackupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.onAction(SettingsAction.ExportBackupConfirmed(uri))
                }
            } else {
                settingsViewModel.onAction(SettingsAction.CancelBiometricFlow)
            }
        }

    val chooseBackupFolderLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.onAction(SettingsAction.SetupBackupsFolder(uri))
                }
            } else {
                settingsViewModel.onAction(SettingsAction.CancelBackupsFolderSelection)
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val importSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.onAction(SettingsAction.ImportSettingsConfirmed(uri))
                }
            }
        }

    val exportSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.onAction(SettingsAction.ExportSettingsConfirmed(uri))
                }
            }
        }

    val biometricEnrollLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            settingsViewModel.onAction(SettingsAction.BiometricEnrollFinished)
        }

    LaunchedEffect(Unit) {
        settingsViewModel.sideEffects.collect { effect ->
            when (effect) {
                is SettingsSideEffect.ShowToast -> context.showToast(effect.messageRes)
                is SettingsSideEffect.ShowToastString -> context.showToast(effect.message)
                is SettingsSideEffect.OpenBrowser -> {
                    val intent =
                        Intent(Intent.ACTION_VIEW, effect.url.toUri()).wrapWithChooser(context)
                    context.startActivity(intent)
                }
                is SettingsSideEffect.LaunchBiometricPrompt -> {
                    if (activity != null) {
                        AuthenticatorProvider.instance.authenticate(
                            activity = activity,
                            cipherIv =
                                if (!effect.isEncrypt) settingsViewModel.preferences.iv.value
                                else null,
                            isForDecrypt = !effect.isEncrypt,
                            onSuccess = { cipher ->
                                if (cipher != null) {
                                    settingsViewModel.onAction(
                                        SettingsAction.BiometricAuthSuccess(
                                            cipher,
                                            effect.isEncrypt,
                                        )
                                    )
                                }
                            },
                            onError = {},
                        )
                    }
                }
                is SettingsSideEffect.StartDatabaseTransition -> {
                    if (activity != null) {
                        DatabaseTransitionActivity.start(activity, effect.action)
                    }
                }
                is SettingsSideEffect.StartBiometricDatabaseTransition -> {
                    if (activity != null) {
                        DatabaseTransitionActivity.start(
                            activity,
                            enableBiometricLock = effect.enableBiometricLock,
                            encryptDatabase = effect.encryptDatabase,
                            cipher = effect.cipher,
                        )
                    }
                }
                is SettingsSideEffect.LaunchExportBackup -> {
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                type = MIME_TYPE_ZIP
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, effect.title)
                            }
                            .wrapWithChooser(context)
                    exportBackupLauncher.launch(intent)
                }
                is SettingsSideEffect.LaunchImportBackup -> {
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .apply {
                                type = "*/*"
                                putExtra(
                                    Intent.EXTRA_MIME_TYPES,
                                    arrayOf(MIME_TYPE_ZIP, "text/xml"),
                                )
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            .wrapWithChooser(context)
                    importBackupLauncher.launch(intent)
                }
                is SettingsSideEffect.LaunchImportRawDatabase -> {
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .apply {
                                type = "*/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(
                                    Intent.EXTRA_MIME_TYPES,
                                    arrayOf("application/octet-stream", "application/x-sqlite3"),
                                )
                            }
                            .wrapWithChooser(context)
                    importRawDatabaseLauncher.launch(intent)
                }
                is SettingsSideEffect.LaunchImportOther -> {
                    val intent =
                        if (effect.isTree) {
                            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                .apply { addCategory(Intent.CATEGORY_DEFAULT) }
                                .wrapWithChooser(context)
                        } else {
                            Intent(Intent.ACTION_OPEN_DOCUMENT)
                                .apply {
                                    type =
                                        if (
                                            effect.mimeTypes.size == 1 &&
                                                effect.mimeTypes[0] != "*/*"
                                        )
                                            effect.mimeTypes[0]
                                        else "*/*"
                                    putExtra(Intent.EXTRA_MIME_TYPES, effect.mimeTypes)
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                .wrapWithChooser(context)
                        }
                    importOtherLauncher.launch(intent)
                }
                is SettingsSideEffect.LaunchChooseBackupFolder -> {
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                            .apply {
                                if (
                                    effect.initialUri != null &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                ) {
                                    putExtra(
                                        DocumentsContract.EXTRA_INITIAL_URI,
                                        effect.initialUri,
                                    )
                                }
                            }
                            .wrapWithChooser(context)
                    chooseBackupFolderLauncher.launch(intent)
                }
                is SettingsSideEffect.RequestNotificationPermission -> {
                    notificationPermissionLauncher.launch(effect.permission)
                }
                is SettingsSideEffect.LaunchImportSettings -> {
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .apply {
                                type = MIME_TYPE_JSON
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, "NotallyX_Settings.json")
                            }
                            .wrapWithChooser(context)
                    importSettingsLauncher.launch(intent)
                }
                is SettingsSideEffect.LaunchExportSettings -> {
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                type = MIME_TYPE_JSON
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, effect.title)
                            }
                            .wrapWithChooser(context)
                    exportSettingsLauncher.launch(intent)
                }
                is SettingsSideEffect.LaunchBiometricEnroll -> {
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            @Suppress("DEPRECATION") Intent(Settings.ACTION_FINGERPRINT_ENROLL)
                        } else {
                            Intent(Settings.ACTION_SECURITY_SETTINGS)
                        }
                    biometricEnrollLauncher.launch(intent)
                }
                is SettingsSideEffect.SendFeedbackEmail -> {
                    val intent =
                        Intent(Intent.ACTION_SEND)
                            .apply {
                                selector = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("notallyx@yahoo.com"))
                                putExtra(Intent.EXTRA_SUBJECT, "NotallyX [Feedback]")
                                effect.logUri?.let { putExtra(Intent.EXTRA_STREAM, it) }
                            }
                            .wrapWithChooser(context)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        context.showToast(R.string.install_an_email)
                    }
                }
                is SettingsSideEffect.ReportBug -> {
                    context.reportBug(effect.logs)
                }
                is SettingsSideEffect.ViewLogs -> {
                    (activity ?: context as? ContextWrapper)?.viewLogs()
                }
                is SettingsSideEffect.OpenExternalDirectory -> {
                    context.openExternalMediaFolder(File(effect.path))
                }
                is SettingsSideEffect.SetWindowSecure -> {
                    activity?.setEnabledSecureFlag(effect.enabled)
                }
            }
        }
    }

    SettingsScreen(
        state = uiState,
        modifier = modifier,
        onAction = settingsViewModel::onAction,
    )
}

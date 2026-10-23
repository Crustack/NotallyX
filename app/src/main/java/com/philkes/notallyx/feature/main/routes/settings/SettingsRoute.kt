package com.philkes.notallyx.feature.main.routes.settings

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import com.philkes.notallyx.R
import com.philkes.notallyx.cancelAutoRemoveOldDeletedNotes
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_JSON
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.common.extensions.catchNoBrowserInstalled
import com.philkes.notallyx.core.common.extensions.copyToClipBoard
import com.philkes.notallyx.core.common.extensions.getExternalCrashesDirectory
import com.philkes.notallyx.core.common.extensions.getLastExceptionLog
import com.philkes.notallyx.core.common.extensions.getLogFile
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.openExternalMediaFolder
import com.philkes.notallyx.core.common.extensions.putNavKey
import com.philkes.notallyx.core.common.extensions.reportBug
import com.philkes.notallyx.core.common.extensions.viewLogs
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.BooleanPreference
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.EnumPreference
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_MAX_MIN
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MIN
import com.philkes.notallyx.core.preference.TextProvider
import com.philkes.notallyx.core.security.AuthenticatorProvider
import com.philkes.notallyx.core.ui.dialog.IconListItem
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.extensions.setEnabledSecureFlag
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel
import com.philkes.notallyx.feature.main.routes.notes.NotesFragmentViewModel.Companion.CURRENT_LABEL_EMPTY
import com.philkes.notallyx.feature.migration.backup.DatabaseAction
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.feature.migration.backup.exportPreferences
import com.philkes.notallyx.feature.migration.imports.Display
import com.philkes.notallyx.feature.migration.imports.FOLDER_OR_FILE_MIMETYPE
import com.philkes.notallyx.feature.migration.imports.ImportSource
import com.philkes.notallyx.feature.migration.imports.txt.APPLICATION_TEXT_MIME_TYPES
import com.philkes.notallyx.feature.settings.SettingsActions
import com.philkes.notallyx.feature.settings.SettingsDialogState
import com.philkes.notallyx.feature.settings.SettingsScreen
import com.philkes.notallyx.feature.settings.SettingsViewModel
import com.philkes.notallyx.feature.settings.createFromPreferences
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.scheduleAutoRemoveOldDeletedNotes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@android.annotation.SuppressLint("LocalContextGetResourceValue")
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    notallyFragmentModel: NotesFragmentViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(Unit) { notallyFragmentModel.currentLabel = CURRENT_LABEL_EMPTY }

    var activeDialogState by remember { mutableStateOf<SettingsDialogState?>(null) }
    var selectedImportSource by remember { mutableStateOf<ImportSource?>(null) }
    var pendingBiometricContinuation by remember { mutableStateOf<(() -> Unit)?>(null) }

    val activity =
        remember(context) {
            var ctx = context
            while (ctx is ContextWrapper) {
                if (ctx is FragmentActivity) break
                ctx = ctx.baseContext
            }
            ctx as? FragmentActivity
        }

    val labelsState by settingsViewModel.labels.collectAsStateWithLifecycle(emptyList())
    val importProgressState by settingsViewModel.importProgress.collectAsStateWithLifecycle(null)

    val importBackupActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    when (context.contentResolver.getType(uri)) {
                        "text/xml" -> settingsViewModel.importXmlBackup(uri)
                        MIME_TYPE_ZIP -> {
                            val defaultPassword =
                                settingsViewModel.preferences.backupPassword.value.takeIf {
                                    it != PASSWORD_EMPTY
                                } ?: ""
                            activeDialogState =
                                SettingsDialogState.ImportNotesPassword(
                                    initialPassword = defaultPassword,
                                    onConfirm = { usedPassword, checkDuplicates ->
                                        settingsViewModel.importZipBackup(
                                            uri,
                                            usedPassword,
                                            checkDuplicates,
                                        )
                                    },
                                )
                        }
                    }
                }
            }
        }

    val importRawDatabaseActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { settingsViewModel.importRawDatabase(it, false) }
            }
        }

    val importOtherActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImportSource?.let { source ->
                        settingsViewModel.importFromOtherApp(uri, source)
                    }
                }
            }
        }

    val exportBackupActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.exportBackup(uri) {
                        pendingBiometricContinuation?.invoke()
                        pendingBiometricContinuation = null
                    }
                }
            } else {
                pendingBiometricContinuation = null
            }
        }

    val chooseBackupFolderActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.setupBackupsFolder(uri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        activity?.let { act ->
                            val permission = Manifest.permission.POST_NOTIFICATIONS
                            if (
                                act.checkSelfPermission(permission) !=
                                    PackageManager.PERMISSION_GRANTED
                            ) {
                                activeDialogState =
                                    SettingsDialogState.Confirm(
                                        message =
                                            act.getString(
                                                R.string
                                                    .please_grant_notally_notification_auto_backup
                                            ),
                                        positiveButtonText = act.getString(R.string.continue_),
                                        negativeButtonText = act.getString(R.string.skip),
                                        onConfirm = {
                                            act.requestPermissions(arrayOf(permission), 0)
                                        },
                                    )
                            }
                        }
                    }
                }
            } else {
                if (
                    settingsViewModel.showRefreshBackupsFolderAfterThemeChange ||
                        settingsViewModel.previousBackupsFolder != null
                ) {
                    settingsViewModel.cancelFolderSelection()
                }
            }
        }

    fun buildBackupTitle(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault())
        val ts = sdf.format(Date())
        return "NotallyX Backup $ts.zip"
    }

    fun showEncryptDatabaseDialog() {
        activeDialogState =
            SettingsDialogState.EncryptDatabase(
                onContinue = { shouldEncrypt ->
                    if (shouldEncrypt && activity != null) {
                        AuthenticatorProvider.instance.authenticate(
                            activity,
                            isForDecrypt = false,
                            onSuccess = { cipher ->
                                requireNotNull(cipher) { "Missing cipher for biometric encryption" }
                                DatabaseTransitionActivity.start(
                                    activity,
                                    enableBiometricLock = true,
                                    encryptDatabase = true,
                                    cipher = cipher,
                                )
                            },
                        ) {
                            // Biometrics not setup / error
                        }
                    } else {
                        settingsViewModel.preferences.biometricLockEncryptsDb.save(false)
                        settingsViewModel.preferences.biometricLock.save(BiometricLock.ENABLED)
                        context.showToast(R.string.biometrics_setup_success)
                    }
                }
            )
    }

    fun showEnableBiometricLock() {
        activeDialogState =
            SettingsDialogState.Confirm(
                message = context.applicationContext.getString(R.string.biometric_backup_advice),
                positiveButtonText = context.applicationContext.getString(R.string.export),
                negativeButtonText = context.applicationContext.getString(R.string.cancel),
                neutralButtonText = context.applicationContext.getString(R.string.continue_),
                onNeutralButtonClick = { showEncryptDatabaseDialog() },
                onConfirm = {
                    pendingBiometricContinuation = { showEncryptDatabaseDialog() }
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                type = MIME_TYPE_ZIP
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, buildBackupTitle())
                            }
                            .wrapWithChooser(context)
                    exportBackupActivityResultLauncher.launch(intent)
                },
            )
    }

    fun showDisableBiometricLock() {
        fun doDisable() {
            if (settingsViewModel.preferences.biometricLockEncryptsDb.value && activity != null) {
                AuthenticatorProvider.instance.authenticate(
                    activity,
                    settingsViewModel.preferences.iv.value,
                    isForDecrypt = true,
                    onSuccess = { cipher ->
                        DatabaseTransitionActivity.start(
                            activity,
                            enableBiometricLock = false,
                            encryptDatabase = false,
                            cipher = cipher,
                        )
                    },
                ) {}
            } else {
                settingsViewModel.preferences.biometricLock.save(BiometricLock.DISABLED)
                settingsViewModel.preferences.biometricLockEncryptsDb.save(false)
                context.showToast(R.string.biometrics_disable_success)
            }
        }

        activeDialogState =
            SettingsDialogState.Confirm(
                message = context.applicationContext.getString(R.string.biometric_backup_advice),
                positiveButtonText = context.applicationContext.getString(R.string.export),
                negativeButtonText = context.applicationContext.getString(R.string.cancel),
                neutralButtonText = context.applicationContext.getString(R.string.continue_),
                onNeutralButtonClick = { doDisable() },
                onConfirm = {
                    pendingBiometricContinuation = { doDisable() }
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                type = MIME_TYPE_ZIP
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, buildBackupTitle())
                            }
                            .wrapWithChooser(context)
                    exportBackupActivityResultLauncher.launch(intent)
                },
            )
    }

    val setupLockActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            showEnableBiometricLock()
        }

    val exportSettingsActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    if (context.exportPreferences(settingsViewModel.preferences, uri)) {
                        context.showToast(R.string.export_settings_success)
                    } else {
                        context.showToast(R.string.export_settings_failure)
                    }
                }
            }
        }

    fun askForUriPermissions(uri: Uri) {
        chooseBackupFolderActivityResultLauncher.launch(
            Intent(ACTION_OPEN_DOCUMENT_TREE).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                }
            }
        )
    }

    val importSettingsActivityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result
            ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    settingsViewModel.importPreferences(
                        context,
                        uri,
                        ::askForUriPermissions,
                        { context.showToast(R.string.import_settings_success) },
                    ) {
                        context.showToast(R.string.import_settings_failure)
                    }
                }
            }
        }

    fun <T> showEnumChoiceDialog(
        enumPreference: EnumPreference<T>,
        onSave: (newValue: T) -> Unit,
    ) where T : Enum<T>, T : TextProvider {
        val enumEntries = enumPreference.value::class.java.enumConstants!!.toList()
        val items = enumEntries.map { SelectionItem(it, it.getText(context)) }
        activeDialogState =
            SettingsDialogState.SingleSelection(
                title = context.applicationContext.getString(enumPreference.titleResId!!),
                items = items,
                selectedItem = enumPreference.value,
                onConfirm = { newValue -> onSave(newValue) },
            )
    }

    fun showThemeDialog() {
        activeDialogState =
            SettingsDialogState.Theme(
                initialTheme = settingsViewModel.preferences.theme.value,
                initialUseDynamicColors = settingsViewModel.preferences.useDynamicColors.value,
                onConfirm = { newTheme, useDynamicColors ->
                    settingsViewModel.savePreference(settingsViewModel.preferences.theme, newTheme)
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.useDynamicColors,
                        useDynamicColors,
                    )
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent!!.component
                    val mainIntent =
                        Intent.makeRestartActivityTask(componentName)
                            .putNavKey(NotallyNavKey.Settings)
                    mainIntent.setPackage(context.packageName)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                },
            )
    }

    fun showBooleanPreferenceDialog(
        preference: BooleanPreference,
        messageResId: Int? = null,
        enabled: Boolean = true,
        disabledTextResId: Int? = null,
        onSave: (newValue: Boolean) -> Unit,
    ) {
        if (!enabled) return
        activeDialogState =
            SettingsDialogState.BooleanPref(
                title = context.applicationContext.getString(preference.titleResId!!),
                message = messageResId?.let { context.applicationContext.getString(it) },
                initialValue = preference.value,
                onConfirm = onSave,
            )
    }

    fun showStartViewDialog(labels: List<String>?) {
        val preference = settingsViewModel.preferences.startView
        val value = preference.value
        val appCtx = context.applicationContext
        val notesText =
            "${appCtx.getText(R.string.notes)} (${appCtx.getText(R.string.text_default)})"
        val unlabeledText = appCtx.getText(R.string.unlabeled).toString()

        val values =
            mutableListOf(notesText to START_VIEW_DEFAULT, unlabeledText to START_VIEW_UNLABELED)
                .apply { labels?.forEach { add(it to it) } }

        val items = values.map { SelectionItem(it.second, it.first) }
        activeDialogState =
            SettingsDialogState.SingleSelection(
                title = appCtx.getString(preference.titleResId!!),
                items = items,
                selectedItem = value,
                onConfirm = { newValue -> settingsViewModel.savePreference(preference, newValue) },
            )
    }

    val appCtx = context.applicationContext

    fun showPeriodicBackupDialog() {
        val backupFolder = settingsViewModel.preferences.backupsFolder.value
        if (backupFolder == EMPTY_PATH) return
        activeDialogState =
            SettingsDialogState.BooleanPref(
                title = appCtx.getString(R.string.backup_periodic),
                message = appCtx.getString(R.string.backup_periodic_hint),
                initialValue = settingsViewModel.preferences.periodicBackups.value.periodInDays > 0,
                onConfirm = { enabled ->
                    if (enabled) {
                        val periodInDays =
                            settingsViewModel.preferences.periodicBackups.value.periodInDays.let {
                                if (it >= BACKUP_PERIOD_DAYS_MIN) it else BACKUP_PERIOD_DAYS_MIN
                            }
                        val maxBackups =
                            settingsViewModel.preferences.periodicBackups.value.maxBackups.let {
                                if (it >= BACKUP_MAX_MIN) it else BACKUP_MAX_MIN
                            }
                        settingsViewModel.savePreference(
                            settingsViewModel.preferences.periodicBackups,
                            settingsViewModel.preferences.periodicBackups.value.copy(
                                periodInDays = periodInDays,
                                maxBackups = maxBackups,
                            ),
                        )
                    } else {
                        settingsViewModel.savePreference(
                            settingsViewModel.preferences.periodicBackups,
                            settingsViewModel.preferences.periodicBackups.value.copy(
                                periodInDays = 0
                            ),
                        )
                    }
                },
            )
    }

    fun showBackupPasswordDialog() {
        val password =
            settingsViewModel.preferences.backupPassword.value.takeIf { it != PASSWORD_EMPTY } ?: ""
        activeDialogState =
            SettingsDialogState.BackupPassword(
                password = password,
                onConfirm = { updatedPassword ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.backupPassword,
                        updatedPassword,
                    )
                },
            )
    }

    fun showBiometricsNotSetupDialog() {
        activeDialogState =
            SettingsDialogState.Confirm(
                message = appCtx.getString(R.string.biometrics_not_setup),
                positiveButtonText = appCtx.getString(R.string.tap_to_set_up),
                negativeButtonText = appCtx.getString(R.string.cancel),
                onConfirm = {
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            Intent(Settings.ACTION_FINGERPRINT_ENROLL)
                        } else {
                            Intent(Settings.ACTION_SECURITY_SETTINGS)
                        }
                    setupLockActivityResultLauncher.launch(intent)
                },
            )
    }

    fun showBiometricLockDialog() {
        val preference = settingsViewModel.preferences.biometricLock
        val value = preference.value
        val enumEntries = BiometricLock.entries
        val items = enumEntries.map { SelectionItem(it, appCtx.getString(it.textResId)) }

        activeDialogState =
            SettingsDialogState.SingleSelection(
                title = appCtx.getString(preference.titleResId!!),
                items = items,
                selectedItem = value,
                onConfirm = { newValue ->
                    if (newValue == value) {
                        return@SingleSelection
                    }
                    if (newValue == BiometricLock.ENABLED) {
                        when (
                            AuthenticatorProvider.instance.canAuthenticateWithBiometrics(context)
                        ) {
                            BiometricManager.BIOMETRIC_SUCCESS -> showEnableBiometricLock()
                            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                                context.showToast(R.string.biometrics_no_support)
                            }

                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                                showBiometricsNotSetupDialog()
                        }
                    } else {
                        when (
                            AuthenticatorProvider.instance.canAuthenticateWithBiometrics(context)
                        ) {
                            BiometricManager.BIOMETRIC_SUCCESS -> showDisableBiometricLock()
                            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                                context.showToast(R.string.biometrics_no_support)
                                settingsViewModel.savePreference(
                                    settingsViewModel.preferences.biometricLock,
                                    BiometricLock.DISABLED,
                                )
                            }

                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                showBiometricsNotSetupDialog()
                                settingsViewModel.savePreference(
                                    settingsViewModel.preferences.biometricLock,
                                    BiometricLock.DISABLED,
                                )
                            }
                        }
                    }
                },
            )
    }

    fun displayChooseBackupFolderDialog() {
        activeDialogState =
            SettingsDialogState.Confirm(
                message = appCtx.getString(R.string.auto_backups_folder_hint),
                positiveButtonText = appCtx.getString(R.string.choose_folder),
                negativeButtonText = appCtx.getString(R.string.cancel),
                neutralButtonText = appCtx.getString(R.string.documentation),
                onNeutralButtonClick = {
                    val uri =
                        "https://crustack.github.io/NotallyX/docs/features/backups#using-cloud-storagewebdav"
                            .toUri()
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri).wrapWithChooser(context))
                },
                onConfirm = {
                    val intent = Intent(ACTION_OPEN_DOCUMENT_TREE).wrapWithChooser(context)
                    chooseBackupFolderActivityResultLauncher.launch(intent)
                },
            )
    }

    fun openLink(link: String) {
        val uri = link.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).wrapWithChooser(context)
        context.startActivity(intent)
    }

    fun handleImportSourceSelection(which: Int, notallyItems: List<Display>) {
        when (which) {
            0 -> {
                activeDialogState =
                    SettingsDialogState.Confirm(
                        message =
                            appCtx.getString(
                                R.string.import_from_notally,
                                appCtx.getString(R.string.import_backup),
                            ),
                        positiveButtonText = appCtx.getString(android.R.string.ok),
                        negativeButtonText = "",
                    )
                return
            }

            1 -> {
                importRawDatabaseActivityResultLauncher.launch(
                    Intent(Intent.ACTION_OPEN_DOCUMENT)
                        .apply {
                            type = "*/*"
                            addCategory(Intent.CATEGORY_OPENABLE)
                            val mimeTypes =
                                arrayOf("application/octet-stream", "application/x-sqlite3")
                            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                        }
                        .wrapWithChooser(context)
                )
                return
            }
        }

        val source = ImportSource.entries[which - notallyItems.size]
        selectedImportSource = source
        val docUrl = source.documentationUrl

        activeDialogState =
            SettingsDialogState.Confirm(
                message = appCtx.getString(source.helpTextResId),
                positiveButtonText = appCtx.getString(R.string.import_action),
                negativeButtonText = appCtx.getString(R.string.cancel),
                neutralButtonText = docUrl?.let { appCtx.getString(R.string.help) },
                onNeutralButtonClick =
                    docUrl?.let { url ->
                        {
                            val intent =
                                Intent(Intent.ACTION_VIEW)
                                    .apply { data = Uri.parse(url) }
                                    .wrapWithChooser(context)
                            context.startActivity(intent)
                        }
                    },
                onConfirm = {
                    when (source.mimeType) {
                        FOLDER_OR_FILE_MIMETYPE -> {
                            activeDialogState =
                                SettingsDialogState.ListSelection(
                                    title = appCtx.getString(source.displayNameResId),
                                    items =
                                        listOf(
                                            appCtx.getString(R.string.folder),
                                            appCtx.getString(R.string.single_file),
                                        ),
                                    onItemClick = { choice ->
                                        when (choice) {
                                            0 ->
                                                importOtherActivityResultLauncher.launch(
                                                    Intent(ACTION_OPEN_DOCUMENT_TREE)
                                                        .apply {
                                                            addCategory(Intent.CATEGORY_DEFAULT)
                                                        }
                                                        .wrapWithChooser(context)
                                                )

                                            1 ->
                                                importOtherActivityResultLauncher.launch(
                                                    Intent(Intent.ACTION_OPEN_DOCUMENT)
                                                        .apply {
                                                            type = "text/*"
                                                            addCategory(Intent.CATEGORY_OPENABLE)
                                                            putExtra(
                                                                Intent.EXTRA_MIME_TYPES,
                                                                arrayOf("text/*") +
                                                                    APPLICATION_TEXT_MIME_TYPES,
                                                            )
                                                        }
                                                        .wrapWithChooser(context)
                                                )
                                        }
                                    },
                                )
                        }

                        else ->
                            importOtherActivityResultLauncher.launch(
                                Intent(Intent.ACTION_OPEN_DOCUMENT)
                                    .apply {
                                        type = "application/*"
                                        putExtra(
                                            Intent.EXTRA_MIME_TYPES,
                                            arrayOf(source.mimeType),
                                        )
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                    }
                                    .wrapWithChooser(context)
                            )
                    }
                },
            )
    }

    fun importFromOtherApp() {
        val notallyItems =
            mutableListOf(
                object : Display {
                    override fun getTextId() = R.string.notally

                    override fun getIconId() = R.drawable.icon_notally
                },
                object : Display {
                    override fun getTextId() = R.string.notallyx_raw_database

                    override fun getIconId() = R.drawable.database
                },
            )
        val allSources = notallyItems + ImportSource.entries.toMutableList()
        val iconListItems = allSources.map { item ->
            IconListItem(
                iconResId = item.getIconId(),
                text = appCtx.getString(item.getTextId()),
            )
        }
        activeDialogState =
            SettingsDialogState.IconList(
                title = appCtx.getString(R.string.choose_other_app),
                items = iconListItems,
                onItemClick = { which -> handleImportSourceSelection(which, notallyItems) },
            )
    }

    fun showFeedbackDialog() {
        val options =
            listOf(
                appCtx.getString(R.string.report_bug),
                appCtx.getString(R.string.make_feature_request),
                appCtx.getString(R.string.send_feedback),
            )
        activeDialogState =
            SettingsDialogState.ListSelection(
                title = appCtx.getString(R.string.send_feedback),
                items = options,
                onItemClick = { which ->
                    when (which) {
                        0 -> {
                            val app = context.applicationContext as Application
                            val logs = app.getLastExceptionLog()
                            context.reportBug(logs)
                        }

                        1 ->
                            context.catchNoBrowserInstalled {
                                context.startActivity(
                                    Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "https://github.com/Crustack/NotallyX/issues/new?labels=enhancement&template=feature_request.md"
                                            ),
                                        )
                                        .wrapWithChooser(context)
                                )
                            }

                        2 -> {
                            val intent =
                                Intent(Intent.ACTION_SEND)
                                    .apply {
                                        selector =
                                            Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                                        putExtra(
                                            Intent.EXTRA_EMAIL,
                                            arrayOf("notallyx@yahoo.com"),
                                        )
                                        putExtra(Intent.EXTRA_SUBJECT, "NotallyX [Feedback]")
                                        val app = context.applicationContext as Application
                                        val log = app.getLogFile()
                                        if (log.exists()) {
                                            val uri = app.getUriForFile(log)
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                        }
                                    }
                                    .wrapWithChooser(context)
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                context.showToast(R.string.install_an_email)
                            }
                        }
                    }
                },
            )
    }

    fun showCrashesDialog() {
        (context as? ContextWrapper)?.let { ctx ->
            val externalCrashesDirectory = ctx.getExternalCrashesDirectory()
            activeDialogState =
                SettingsDialogState.Confirm(
                    message =
                        "${appCtx.getString(R.string.view_crash_logs)}: ${externalCrashesDirectory.absolutePath}",
                    positiveButtonText = appCtx.getString(android.R.string.ok),
                    negativeButtonText = appCtx.getString(R.string.cancel),
                    neutralButtonText = appCtx.getString(R.string.copy),
                    onNeutralButtonClick = {
                        ctx.copyToClipBoard(externalCrashesDirectory.absolutePath)
                    },
                )
            ctx.openExternalMediaFolder(externalCrashesDirectory)
        }
    }

    val actions =
        SettingsActions(
            onDismissDialog = { activeDialogState = null },
            onNotesViewClick = {
                showEnumChoiceDialog(settingsViewModel.preferences.notesView) { newValue ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.notesView,
                        newValue,
                    )
                }
            },
            onThemeClick = { showThemeDialog() },
            onDateFormatOverviewClick = {
                activeDialogState =
                    SettingsDialogState.DateTimeFormat(
                        initialDate = settingsViewModel.preferences.dateFormatOverview.value,
                        initialTime = settingsViewModel.preferences.timeFormatOverview.value,
                        onConfirm = { newDate, newTime ->
                            settingsViewModel.savePreference(
                                settingsViewModel.preferences.dateFormatOverview,
                                newDate,
                            )
                            settingsViewModel.savePreference(
                                settingsViewModel.preferences.timeFormatOverview,
                                newTime,
                            )
                        },
                    )
            },
            onDateFormatNoteViewClick = {
                println("DEBUG_ROUTE: onDateFormatNoteViewClick invoked!")
                activeDialogState =
                    SettingsDialogState.DateTimeFormat(
                        initialDate = settingsViewModel.preferences.dateFormatNoteView.value,
                        initialTime = settingsViewModel.preferences.timeFormatNoteView.value,
                        onConfirm = { newDate, newTime ->
                            settingsViewModel.savePreference(
                                settingsViewModel.preferences.dateFormatNoteView,
                                newDate,
                            )
                            settingsViewModel.savePreference(
                                settingsViewModel.preferences.timeFormatNoteView,
                                newTime,
                            )
                        },
                    )
            },
            onTextSizeNoteEditorChange = { newValue ->
                settingsViewModel.savePreference(
                    settingsViewModel.preferences.textSizeNoteEditor,
                    newValue,
                )
            },
            onTextSizeOverviewChange = { newValue ->
                settingsViewModel.savePreference(
                    settingsViewModel.preferences.textSizeOverview,
                    newValue,
                )
            },
            onAlwaysShowSearchBarClick = {
                showBooleanPreferenceDialog(
                    settingsViewModel.preferences.alwaysShowSearchBar,
                    R.string.always_show_search_bar_hint,
                ) { enabled ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.alwaysShowSearchBar,
                        enabled,
                    )
                }
            },
            onNotesSortOrderClick = {
                activeDialogState =
                    SettingsDialogState.NotesSort(
                        currentSort = settingsViewModel.preferences.notesSorting.value,
                        onConfirm = { newSort ->
                            settingsViewModel.savePreference(
                                settingsViewModel.preferences.notesSorting,
                                newSort,
                            )
                            activeDialogState = null
                        },
                    )
            },
            onCheckedListItemSortingClick = {
                showEnumChoiceDialog(settingsViewModel.preferences.listItemSorting) { newValue ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.listItemSorting,
                        newValue,
                    )
                }
            },
            onStartViewClick = { showStartViewDialog(labelsState.map { it.value }) },
            onDefaultListNoteViewModeClick = {
                showEnumChoiceDialog(settingsViewModel.preferences.defaultListNoteViewMode) {
                    newValue ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.defaultListNoteViewMode,
                        newValue,
                    )
                }
            },
            onAutoSaveAfterIdleChange = { newValue ->
                settingsViewModel.savePreference(
                    settingsViewModel.preferences.autoSaveAfterIdleTime,
                    newValue,
                )
            },
            onAutoRemoveDeletedNotesChange = { newValue ->
                settingsViewModel.savePreference(
                    settingsViewModel.preferences.autoRemoveDeletedNotesAfterDays,
                    newValue,
                )
                val workManager = WorkManager.getInstance(context)
                if (newValue > 0) {
                    (context as? ContextWrapper)?.let {
                        workManager.scheduleAutoRemoveOldDeletedNotes(it)
                    }
                } else {
                    workManager.cancelAutoRemoveOldDeletedNotes()
                }
            },
            onMaxTitleChange = { newValue ->
                settingsViewModel.savePreference(settingsViewModel.preferences.maxTitle, newValue)
            },
            onMaxItemsChange = { newValue ->
                settingsViewModel.savePreference(settingsViewModel.preferences.maxItems, newValue)
            },
            onMaxLinesChange = { newValue ->
                settingsViewModel.savePreference(settingsViewModel.preferences.maxLines, newValue)
            },
            onMaxLabelsChange = { newValue ->
                settingsViewModel.savePreference(
                    settingsViewModel.preferences.maxLabels,
                    newValue,
                )
            },
            onLabelTagsHiddenClick = {
                showBooleanPreferenceDialog(
                    settingsViewModel.preferences.labelTagsHiddenInOverview,
                    R.string.labels_hidden_in_overview_title,
                ) { enabled ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.labelTagsHiddenInOverview,
                        enabled,
                    )
                }
            },
            onImagesHiddenClick = {
                showBooleanPreferenceDialog(
                    settingsViewModel.preferences.imagesHiddenInOverview,
                    R.string.images_hidden_in_overview_title,
                ) { enabled ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.imagesHiddenInOverview,
                        enabled,
                    )
                }
            },
            onImportBackupClick = {
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
                importBackupActivityResultLauncher.launch(intent)
            },
            onImportOtherClick = { importFromOtherApp() },
            onExportBackupClick = {
                val intent =
                    Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .apply {
                            type = MIME_TYPE_ZIP
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(Intent.EXTRA_TITLE, buildBackupTitle())
                        }
                        .wrapWithChooser(context)
                exportBackupActivityResultLauncher.launch(intent)
            },
            onBackupsFolderClick = {
                val folder = settingsViewModel.preferences.backupsFolder.value
                if (folder == EMPTY_PATH) {
                    displayChooseBackupFolderDialog()
                } else {
                    activeDialogState =
                        SettingsDialogState.ListSelection(
                            title = appCtx.getString(R.string.auto_backups_folder),
                            items =
                                listOf(
                                    appCtx.getString(R.string.clear),
                                    appCtx.getString(R.string.choose_another_folder),
                                ),
                            onItemClick = { which ->
                                when (which) {
                                    0 -> settingsViewModel.disableBackups()
                                    1 -> displayChooseBackupFolderDialog()
                                }
                            },
                        )
                }
            },
            onBackupOnSaveClick = {
                showBooleanPreferenceDialog(
                    settingsViewModel.preferences.backupOnSave,
                    R.string.auto_backup_on_save,
                    enabled = settingsViewModel.preferences.backupsFolder.value != EMPTY_PATH,
                    disabledTextResId = R.string.auto_backups_folder_set,
                ) { enabled ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.backupOnSave,
                        enabled,
                    )
                }
            },
            onPeriodicBackupsClick = { showPeriodicBackupDialog() },
            onPeriodicBackupsPeriodInDaysChange = { newValue ->
                val current = settingsViewModel.preferences.periodicBackups.value
                settingsViewModel.savePreference(
                    settingsViewModel.preferences.periodicBackups,
                    current.copy(periodInDays = newValue),
                )
            },
            onPeriodicBackupsMaxChange = { newValue ->
                val current = settingsViewModel.preferences.periodicBackups.value
                settingsViewModel.savePreference(
                    settingsViewModel.preferences.periodicBackups,
                    current.copy(maxBackups = newValue),
                )
            },
            onBiometricLockClick = { showBiometricLockDialog() },
            onBackupPasswordClick = { showBackupPasswordDialog() },
            onSecureFlagClick = {
                showBooleanPreferenceDialog(settingsViewModel.preferences.secureFlag) { enabled ->
                    settingsViewModel.savePreference(
                        settingsViewModel.preferences.secureFlag,
                        enabled,
                    )
                    activity?.setEnabledSecureFlag(enabled)
                }
            },
            onImportSettingsClick = {
                activeDialogState =
                    SettingsDialogState.Confirm(
                        message = appCtx.getString(R.string.import_settings_message),
                        positiveButtonText = appCtx.getString(R.string.import_action),
                        negativeButtonText = appCtx.getString(R.string.cancel),
                        onConfirm = {
                            val intent =
                                Intent(Intent.ACTION_OPEN_DOCUMENT)
                                    .apply {
                                        type = MIME_TYPE_JSON
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        putExtra(
                                            Intent.EXTRA_TITLE,
                                            "NotallyX_Settings.json",
                                        )
                                    }
                                    .wrapWithChooser(context)
                            importSettingsActivityResultLauncher.launch(intent)
                        },
                    )
            },
            onExportSettingsClick = {
                activeDialogState =
                    SettingsDialogState.Confirm(
                        message = appCtx.getString(R.string.export_settings_message),
                        positiveButtonText = appCtx.getString(R.string.export),
                        negativeButtonText = appCtx.getString(R.string.cancel),
                        onConfirm = {
                            val intent =
                                Intent(Intent.ACTION_CREATE_DOCUMENT)
                                    .apply {
                                        type = MIME_TYPE_JSON
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        putExtra(
                                            Intent.EXTRA_TITLE,
                                            "NotallyX_Settings.json",
                                        )
                                    }
                                    .wrapWithChooser(context)
                            exportSettingsActivityResultLauncher.launch(intent)
                        },
                    )
            },
            onResetSettingsClick = {
                activeDialogState =
                    SettingsDialogState.Confirm(
                        message = appCtx.getString(R.string.reset_settings_message),
                        positiveButtonText = appCtx.getString(R.string.reset_settings),
                        negativeButtonText = appCtx.getString(R.string.cancel),
                        onConfirm = {
                            coroutineScope.launch {
                                settingsViewModel.resetPreferences { _ ->
                                    context.showToast(R.string.reset_settings_success)
                                }
                            }
                        },
                    )
            },
            onDataInPublicFolderClick = {
                showBooleanPreferenceDialog(
                    settingsViewModel.preferences.dataInPublicFolder,
                    R.string.data_in_public_message,
                ) { enabled ->
                    activity?.let {
                        if (enabled) {
                            DatabaseTransitionActivity.start(
                                it,
                                DatabaseAction.ENABLE_DATA_IN_PUBLIC,
                            )
                        } else {
                            DatabaseTransitionActivity.start(
                                it,
                                DatabaseAction.DISABLE_DATA_IN_PUBLIC,
                            )
                        }
                    }
                }
            },
            onClearDataClick = {
                activeDialogState =
                    SettingsDialogState.Confirm(
                        message = appCtx.getString(R.string.clear_data_message),
                        positiveButtonText = appCtx.getString(R.string.delete_all),
                        negativeButtonText = appCtx.getString(R.string.cancel),
                        onConfirm = { settingsViewModel.deleteAll() },
                    )
            },
            onSendFeedbackClick = { showFeedbackDialog() },
            onRateClick = {
                openLink("https://play.google.com/store/apps/details?id=com.philkes.notallyx")
            },
            onDonateClick = { openLink("https://ko-fi.com/crustack") },
            onViewLogsClick = { (context as? ContextWrapper)?.viewLogs() },
            onViewCrashesClick = { showCrashesDialog() },
            onDocumentationClick = { openLink("https://crustack.github.io/NotallyX") },
            onSourceCodeClick = { openLink("https://github.com/Crustack/NotallyX") },
        )

    val uiState = context.createFromPreferences(settingsViewModel.preferences)
    SettingsScreen(
        state = uiState,
        modifier = modifier,
        dialogState = activeDialogState,
        importProgress = importProgressState,
        actions = actions,
    )
}

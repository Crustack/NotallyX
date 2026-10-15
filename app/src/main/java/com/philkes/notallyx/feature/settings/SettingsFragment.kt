package com.philkes.notallyx.feature.settings

import android.Manifest
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.content.Intent.ACTION_OPEN_DOCUMENT_TREE
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.philkes.notallyx.R
import com.philkes.notallyx.cancelAutoRemoveOldDeletedNotes
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_JSON
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.common.extensions.catchNoBrowserInstalled
import com.philkes.notallyx.core.common.extensions.copyToClipBoard
import com.philkes.notallyx.core.common.extensions.getExternalCrashesDirectory
import com.philkes.notallyx.core.common.extensions.getExtraBooleanFromBundleOrIntent
import com.philkes.notallyx.core.common.extensions.getLastExceptionLog
import com.philkes.notallyx.core.common.extensions.getLogFile
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.openExternalMediaFolder
import com.philkes.notallyx.core.common.extensions.reportBug
import com.philkes.notallyx.core.common.extensions.viewLogs
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.BooleanPreference
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.EnumPreference
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_MAX_MIN
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MIN
import com.philkes.notallyx.core.preference.TextProvider
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.security.AuthenticatorProvider
import com.philkes.notallyx.core.ui.dialog.IconListItem
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.extensions.setEnabledSecureFlag
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.backup.DatabaseAction
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.feature.migration.backup.exportPreferences
import com.philkes.notallyx.feature.migration.imports.Display
import com.philkes.notallyx.feature.migration.imports.FOLDER_OR_FILE_MIMETYPE
import com.philkes.notallyx.feature.migration.imports.ImportSource
import com.philkes.notallyx.feature.migration.imports.txt.APPLICATION_TEXT_MIME_TYPES
import com.philkes.notallyx.scheduleAutoRemoveOldDeletedNotes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment() {

    private val model: SettingsViewModel by viewModel()
    val scrollState = ScrollState(0)
    private val activeDialogState = mutableStateOf<SettingsDialogState?>(null)

    private lateinit var importBackupActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var importRawDatabaseActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var importOtherActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportBackupActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var chooseBackupFolderActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var setupLockActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var disableLockActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportSettingsActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var importSettingsActivityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var selectedImportSource: ImportSource

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val composeView =
            ComposeView(requireContext()).apply {
                setContent {
                    val themeState =
                        model.preferences.theme.flow.collectAsStateWithLifecycle(
                            model.preferences.theme.value
                        )
                    val labelsState = model.labels.collectAsStateWithLifecycle(emptyList())
                    val importProgressState = model.importProgress.collectAsStateWithLifecycle(null)

                    NotallyXTheme(theme = themeState.value) {
                        val uiState = requireContext().createFromPreferences(model.preferences)
                        SettingsScreen(
                            state = uiState,
                            scrollState = scrollState,
                            dialogState = activeDialogState.value,
                            importProgress = importProgressState.value,
                            onDismissDialog = { activeDialogState.value = null },
                            onNotesViewClick = {
                                showEnumChoiceDialog(model.preferences.notesView) { newValue ->
                                    model.savePreference(model.preferences.notesView, newValue)
                                }
                            },
                            onThemeClick = { showThemeDialog() },
                            onDateFormatOverviewClick = {
                                showDateTimeFormatDialog(
                                    R.string.date_format_overview,
                                    model.preferences.dateFormatOverview,
                                    model.preferences.timeFormatOverview,
                                ) { newDate, newTime ->
                                    model.savePreference(
                                        model.preferences.dateFormatOverview,
                                        newDate,
                                    )
                                    model.savePreference(
                                        model.preferences.timeFormatOverview,
                                        newTime,
                                    )
                                }
                            },
                            onDateFormatNoteViewClick = { showDateTimeFormatNoteViewDialog() },
                            onTextSizeNoteEditorChange = { newValue ->
                                model.savePreference(
                                    model.preferences.textSizeNoteEditor,
                                    newValue,
                                )
                            },
                            onTextSizeOverviewChange = { newValue ->
                                model.savePreference(model.preferences.textSizeOverview, newValue)
                            },
                            onAlwaysShowSearchBarClick = {
                                showBooleanPreferenceDialog(
                                    model.preferences.alwaysShowSearchBar,
                                    R.string.always_show_search_bar_hint,
                                ) { enabled ->
                                    model.savePreference(
                                        model.preferences.alwaysShowSearchBar,
                                        enabled,
                                    )
                                }
                            },
                            onNotesSortOrderClick = { showNotesSortDialog() },
                            onCheckedListItemSortingClick = {
                                showEnumChoiceDialog(model.preferences.listItemSorting) { newValue
                                    ->
                                    model.savePreference(
                                        model.preferences.listItemSorting,
                                        newValue,
                                    )
                                }
                            },
                            onStartViewClick = {
                                showStartViewDialog(labelsState.value.map { it.value })
                            },
                            onDefaultListNoteViewModeClick = {
                                showEnumChoiceDialog(model.preferences.defaultListNoteViewMode) {
                                    newValue ->
                                    model.savePreference(
                                        model.preferences.defaultListNoteViewMode,
                                        newValue,
                                    )
                                }
                            },
                            onAutoSaveAfterIdleChange = { newValue ->
                                model.savePreference(
                                    model.preferences.autoSaveAfterIdleTime,
                                    newValue,
                                )
                            },
                            onAutoRemoveDeletedNotesChange = { newValue ->
                                model.savePreference(
                                    model.preferences.autoRemoveDeletedNotesAfterDays,
                                    newValue,
                                )
                                val workManager = WorkManager.getInstance(requireContext())
                                if (newValue > 0) {
                                    workManager.scheduleAutoRemoveOldDeletedNotes(
                                        requireContext() as ContextWrapper
                                    )
                                } else {
                                    workManager.cancelAutoRemoveOldDeletedNotes()
                                }
                            },
                            onMaxTitleChange = { newValue ->
                                model.savePreference(model.preferences.maxTitle, newValue)
                            },
                            onMaxItemsChange = { newValue ->
                                model.savePreference(model.preferences.maxItems, newValue)
                            },
                            onMaxLinesChange = { newValue ->
                                model.savePreference(model.preferences.maxLines, newValue)
                            },
                            onMaxLabelsChange = { newValue ->
                                model.savePreference(model.preferences.maxLabels, newValue)
                            },
                            onLabelTagsHiddenClick = {
                                showBooleanPreferenceDialog(
                                    model.preferences.labelTagsHiddenInOverview,
                                    R.string.labels_hidden_in_overview_title,
                                ) { enabled ->
                                    model.savePreference(
                                        model.preferences.labelTagsHiddenInOverview,
                                        enabled,
                                    )
                                }
                            },
                            onImagesHiddenClick = {
                                showBooleanPreferenceDialog(
                                    model.preferences.imagesHiddenInOverview,
                                    R.string.images_hidden_in_overview_title,
                                ) { enabled ->
                                    model.savePreference(
                                        model.preferences.imagesHiddenInOverview,
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
                                        .wrapWithChooser(requireContext())
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
                                        .wrapWithChooser(requireContext())
                                exportBackupActivityResultLauncher.launch(intent)
                            },
                            onBackupsFolderClick = {
                                val folder = model.preferences.backupsFolder.value
                                if (folder == EMPTY_PATH) {
                                    displayChooseBackupFolderDialog()
                                } else {
                                    activeDialogState.value =
                                        SettingsDialogState.ListSelection(
                                            title = getString(R.string.auto_backups_folder),
                                            items =
                                                listOf(
                                                    getString(R.string.clear),
                                                    getString(R.string.choose_another_folder),
                                                ),
                                            onItemClick = { which ->
                                                when (which) {
                                                    0 -> model.disableBackups()
                                                    1 -> displayChooseBackupFolderDialog()
                                                }
                                            },
                                        )
                                }
                            },
                            onBackupOnSaveClick = {
                                showBooleanPreferenceDialog(
                                    model.preferences.backupOnSave,
                                    R.string.auto_backup_on_save,
                                    enabled = model.preferences.backupsFolder.value != EMPTY_PATH,
                                    disabledTextResId = R.string.auto_backups_folder_set,
                                ) { enabled ->
                                    model.savePreference(model.preferences.backupOnSave, enabled)
                                }
                            },
                            onPeriodicBackupsClick = { showPeriodicBackupDialog() },
                            onPeriodicBackupsPeriodInDaysChange = { newValue ->
                                val current = model.preferences.periodicBackups.value
                                model.savePreference(
                                    model.preferences.periodicBackups,
                                    current.copy(periodInDays = newValue),
                                )
                            },
                            onPeriodicBackupsMaxChange = { newValue ->
                                val current = model.preferences.periodicBackups.value
                                model.savePreference(
                                    model.preferences.periodicBackups,
                                    current.copy(maxBackups = newValue),
                                )
                            },
                            onBiometricLockClick = { showBiometricLockDialog() },
                            onBackupPasswordClick = { showBackupPasswordDialog() },
                            onSecureFlagClick = {
                                showBooleanPreferenceDialog(model.preferences.secureFlag) { enabled
                                    ->
                                    model.savePreference(model.preferences.secureFlag, enabled)
                                    activity?.setEnabledSecureFlag(enabled)
                                }
                            },
                            onImportSettingsClick = {
                                activeDialogState.value =
                                    SettingsDialogState.Confirm(
                                        message = getString(R.string.import_settings_message),
                                        positiveButtonText = getString(R.string.import_action),
                                        negativeButtonText = getString(R.string.cancel),
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
                                                    .wrapWithChooser(requireContext())
                                            importSettingsActivityResultLauncher.launch(intent)
                                        },
                                    )
                            },
                            onExportSettingsClick = {
                                activeDialogState.value =
                                    SettingsDialogState.Confirm(
                                        message = getString(R.string.export_settings_message),
                                        positiveButtonText = getString(R.string.export),
                                        negativeButtonText = getString(R.string.cancel),
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
                                                    .wrapWithChooser(requireContext())
                                            exportSettingsActivityResultLauncher.launch(intent)
                                        },
                                    )
                            },
                            onResetSettingsClick = {
                                activeDialogState.value =
                                    SettingsDialogState.Confirm(
                                        message = getString(R.string.reset_settings_message),
                                        positiveButtonText = getString(R.string.reset_settings),
                                        negativeButtonText = getString(R.string.cancel),
                                        onConfirm = {
                                            lifecycleScope.launch {
                                                model.resetPreferences { _ ->
                                                    showToast(R.string.reset_settings_success)
                                                }
                                            }
                                        },
                                    )
                            },
                            onDataInPublicFolderClick = {
                                showBooleanPreferenceDialog(
                                    model.preferences.dataInPublicFolder,
                                    R.string.data_in_public_message,
                                ) { enabled ->
                                    if (enabled) {
                                        DatabaseTransitionActivity.start(
                                            requireActivity(),
                                            DatabaseAction.ENABLE_DATA_IN_PUBLIC,
                                        )
                                    } else {
                                        DatabaseTransitionActivity.start(
                                            requireActivity(),
                                            DatabaseAction.DISABLE_DATA_IN_PUBLIC,
                                        )
                                    }
                                }
                            },
                            onClearDataClick = {
                                activeDialogState.value =
                                    SettingsDialogState.Confirm(
                                        message = getString(R.string.clear_data_message),
                                        positiveButtonText = getString(R.string.delete_all),
                                        negativeButtonText = getString(R.string.cancel),
                                        onConfirm = { model.deleteAll() },
                                    )
                            },
                            onSendFeedbackClick = { showFeedbackDialog() },
                            onRateClick = {
                                openLink(
                                    "https://play.google.com/store/apps/details?id=com.philkes.notallyx"
                                )
                            },
                            onDonateClick = { openLink("https://ko-fi.com/crustack") },
                            onViewLogsClick = { (requireContext() as ContextWrapper).viewLogs() },
                            onViewCrashesClick = { showCrashesDialog() },
                            onDocumentationClick = {
                                openLink("https://crustack.github.io/NotallyX")
                            },
                            onSourceCodeClick = {
                                openLink("https://github.com/Crustack/NotallyX")
                            },
                        )
                    }
                }
            }
        return composeView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActivityResultLaunchers()
        val showImportBackupsFolder =
            getExtraBooleanFromBundleOrIntent(
                savedInstanceState,
                EXTRA_SHOW_IMPORT_BACKUPS_FOLDER,
                false,
            )
        showImportBackupsFolder.let {
            if (it) {
                model.refreshBackupsFolder(
                    askForUriPermissions = { backupFolderUri ->
                        activeDialogState.value =
                            SettingsDialogState.Confirm(
                                message = getString(R.string.auto_backups_folder_rechoose),
                                positiveButtonText = getString(R.string.choose_folder),
                                negativeButtonText = getString(R.string.cancel),
                                onConfirm = { askForUriPermissions(backupFolderUri) },
                            )
                    }
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (model.showRefreshBackupsFolderAfterThemeChange) {
            outState.putBoolean(EXTRA_SHOW_IMPORT_BACKUPS_FOLDER, true)
        }
    }

    private fun setupActivityResultLaunchers() {
        importBackupActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { importBackup(it) }
                }
            }
        importRawDatabaseActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { model.importRawDatabase(it, false) }
                }
            }
        importOtherActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        model.importFromOtherApp(uri, selectedImportSource)
                    }
                }
            }
        exportBackupActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        model.exportBackup(uri) {
                            pendingBiometricContinuation?.invoke()
                            pendingBiometricContinuation = null
                        }
                    }
                } else {
                    pendingBiometricContinuation = null
                }
            }
        chooseBackupFolderActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        model.setupBackupsFolder(uri)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            activity?.let {
                                val permission = Manifest.permission.POST_NOTIFICATIONS
                                if (
                                    it.checkSelfPermission(permission) !=
                                        PackageManager.PERMISSION_GRANTED
                                ) {
                                    activeDialogState.value =
                                        SettingsDialogState.Confirm(
                                            message =
                                                getString(
                                                    R.string
                                                        .please_grant_notally_notification_auto_backup
                                                ),
                                            positiveButtonText = getString(R.string.continue_),
                                            negativeButtonText = getString(R.string.skip),
                                            onConfirm = {
                                                it.requestPermissions(arrayOf(permission), 0)
                                            },
                                        )
                                }
                            }
                        }
                    }
                } else {
                    if (
                        model.showRefreshBackupsFolderAfterThemeChange ||
                            model.previousBackupsFolder != null
                    ) {
                        model.cancelFolderSelection()
                    }
                }
            }
        setupLockActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                showEnableBiometricLock()
            }
        disableLockActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                showDisableBiometricLock()
            }
        exportSettingsActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        if (requireContext().exportPreferences(model.preferences, uri)) {
                            showToast(R.string.export_settings_success)
                        } else {
                            showToast(R.string.export_settings_failure)
                        }
                    }
                }
            }
        importSettingsActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        model.importPreferences(
                            requireContext(),
                            uri,
                            ::askForUriPermissions,
                            { showToast(R.string.import_settings_success) },
                        ) {
                            showToast(R.string.import_settings_failure)
                        }
                    }
                }
            }
    }

    private fun importBackup(uri: Uri) {
        when (requireContext().contentResolver.getType(uri)) {
            "text/xml" -> {
                model.importXmlBackup(uri)
            }

            MIME_TYPE_ZIP -> {
                val defaultPassword =
                    model.preferences.backupPassword.value.takeIf { it != PASSWORD_EMPTY } ?: ""
                activeDialogState.value =
                    SettingsDialogState.ImportNotesPassword(
                        initialPassword = defaultPassword,
                        onConfirm = { usedPassword, checkDuplicates ->
                            model.importZipBackup(uri, usedPassword, checkDuplicates)
                        },
                    )
            }
        }
    }

    private inline fun <reified T> showEnumChoiceDialog(
        enumPreference: EnumPreference<T>,
        crossinline onSave: (newValue: T) -> Unit,
    ) where T : Enum<T>, T : TextProvider {
        val context = requireContext()
        val enumEntries = T::class.java.enumConstants!!.toList()
        val items = enumEntries.map { SelectionItem(it, it.getText(context)) }
        activeDialogState.value =
            SettingsDialogState.SingleSelection(
                title = getString(enumPreference.titleResId!!),
                items = items,
                selectedItem = enumPreference.value,
                onConfirm = { newValue -> onSave(newValue) },
            )
    }

    private fun showThemeDialog() {
        activeDialogState.value =
            SettingsDialogState.Theme(
                initialTheme = model.preferences.theme.value,
                initialUseDynamicColors = model.preferences.useDynamicColors.value,
                onConfirm = { newTheme, useDynamicColors ->
                    model.savePreference(model.preferences.theme, newTheme)
                    model.savePreference(model.preferences.useDynamicColors, useDynamicColors)
                    val context = requireContext()
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent!!.component
                    val mainIntent =
                        Intent.makeRestartActivityTask(componentName).apply {
                            putExtra(MainActivity.EXTRA_FRAGMENT_TO_OPEN, R.id.Settings)
                        }
                    mainIntent.setPackage(context.packageName)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                },
            )
    }

    private fun showDateTimeFormatDialog(
        titleResId: Int,
        datePreference: EnumPreference<DateFormat>,
        timePreference: EnumPreference<TimeFormat>,
        onSave: (dateFormat: DateFormat, timeFormat: TimeFormat) -> Unit,
    ) {
        activeDialogState.value =
            SettingsDialogState.DateTimeFormat(
                initialDate = datePreference.value,
                initialTime = timePreference.value,
                onConfirm = onSave,
            )
    }

    private fun showNotesSortDialog() {
        activeDialogState.value =
            SettingsDialogState.NotesSort(
                currentSort = model.preferences.notesSorting.value,
                onConfirm = { newSort ->
                    model.savePreference(model.preferences.notesSorting, newSort)
                },
            )
    }

    fun showDateTimeFormatNoteViewDialog() {
        showDateTimeFormatDialog(
            R.string.date_format_note_view,
            model.preferences.dateFormatNoteView,
            model.preferences.timeFormatNoteView,
        ) { newDate, newTime ->
            model.savePreference(model.preferences.dateFormatNoteView, newDate)
            model.savePreference(model.preferences.timeFormatNoteView, newTime)
        }
    }

    private fun showBooleanPreferenceDialog(
        preference: BooleanPreference,
        messageResId: Int? = null,
        enabled: Boolean = true,
        disabledTextResId: Int? = null,
        onSave: (newValue: Boolean) -> Unit,
    ) {
        if (!enabled) return
        activeDialogState.value =
            SettingsDialogState.BooleanPref(
                title = getString(preference.titleResId!!),
                message = messageResId?.let { getString(it) },
                initialValue = preference.value,
                onConfirm = onSave,
            )
    }

    private fun showStartViewDialog(labels: List<String>?) {
        val context = requireContext()
        val preference = model.preferences.startView
        val value = preference.value
        val notesText =
            "${context.getText(R.string.notes)} (${context.getText(R.string.text_default)})"
        val unlabeledText = context.getText(R.string.unlabeled).toString()

        val values =
            mutableListOf(notesText to START_VIEW_DEFAULT, unlabeledText to START_VIEW_UNLABELED)
                .apply { labels?.forEach { add(it to it) } }

        val items = values.map { SelectionItem(it.second, it.first) }
        activeDialogState.value =
            SettingsDialogState.SingleSelection(
                title = getString(preference.titleResId!!),
                items = items,
                selectedItem = value,
                onConfirm = { newValue -> model.savePreference(preference, newValue) },
            )
    }

    private fun showPeriodicBackupDialog() {
        val backupFolder = model.preferences.backupsFolder.value
        if (backupFolder == EMPTY_PATH) return
        activeDialogState.value =
            SettingsDialogState.BooleanPref(
                title = getString(R.string.backup_periodic),
                message = getString(R.string.backup_periodic_hint),
                initialValue = model.preferences.periodicBackups.value.periodInDays > 0,
                onConfirm = { enabled ->
                    if (enabled) {
                        val periodInDays =
                            model.preferences.periodicBackups.value.periodInDays.let {
                                if (it >= BACKUP_PERIOD_DAYS_MIN) it else BACKUP_PERIOD_DAYS_MIN
                            }
                        val maxBackups =
                            model.preferences.periodicBackups.value.maxBackups.let {
                                if (it >= BACKUP_MAX_MIN) it else BACKUP_MAX_MIN
                            }
                        model.savePreference(
                            model.preferences.periodicBackups,
                            model.preferences.periodicBackups.value.copy(
                                periodInDays = periodInDays,
                                maxBackups = maxBackups,
                            ),
                        )
                    } else {
                        model.savePreference(
                            model.preferences.periodicBackups,
                            model.preferences.periodicBackups.value.copy(periodInDays = 0),
                        )
                    }
                },
            )
    }

    private fun showBackupPasswordDialog() {
        val password = model.preferences.backupPassword.value.takeIf { it != PASSWORD_EMPTY } ?: ""
        activeDialogState.value =
            SettingsDialogState.BackupPassword(
                password = password,
                onConfirm = { updatedPassword ->
                    model.savePreference(model.preferences.backupPassword, updatedPassword)
                },
            )
    }

    private fun showBiometricLockDialog() {
        val context = requireContext()
        val preference = model.preferences.biometricLock
        val value = preference.value
        val enumEntries = BiometricLock.entries
        val items = enumEntries.map { SelectionItem(it, context.getString(it.textResId)) }

        activeDialogState.value =
            SettingsDialogState.SingleSelection(
                title = getString(preference.titleResId!!),
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
                                model.savePreference(
                                    model.preferences.biometricLock,
                                    BiometricLock.DISABLED,
                                )
                            }

                            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                                showBiometricsNotSetupDialog()
                                model.savePreference(
                                    model.preferences.biometricLock,
                                    BiometricLock.DISABLED,
                                )
                            }
                        }
                    }
                },
            )
    }

    private fun importFromOtherApp() {
        val notallyItems =
            mutableListOf(
                object : Display {
                    override fun getTextId(): Int {
                        return R.string.notally
                    }

                    override fun getIconId(): Int {
                        return R.drawable.icon_notally
                    }
                },
                object : Display {
                    override fun getTextId(): Int {
                        return R.string.notallyx_raw_database
                    }

                    override fun getIconId(): Int {
                        return R.drawable.database
                    }
                },
            )
        val allSources = notallyItems + ImportSource.entries.toMutableList()
        val iconListItems = allSources.map { item ->
            IconListItem(
                iconResId = item.getIconId(),
                text = getString(item.getTextId()),
            )
        }
        activeDialogState.value =
            SettingsDialogState.IconList(
                title = getString(R.string.choose_other_app),
                items = iconListItems,
                onItemClick = { which -> handleImportSourceSelection(which, notallyItems) },
            )
    }

    private fun handleImportSourceSelection(which: Int, notallyItems: List<Display>) {
        when (which) {
            0 -> {
                activeDialogState.value =
                    SettingsDialogState.Confirm(
                        message =
                            getString(
                                R.string.import_from_notally,
                                getString(R.string.import_backup),
                            ),
                        positiveButtonText = getString(android.R.string.ok),
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
                        .wrapWithChooser(requireContext())
                )
                return
            }
        }

        selectedImportSource = ImportSource.entries[which - notallyItems.size]
        val docUrl = selectedImportSource.documentationUrl

        activeDialogState.value =
            SettingsDialogState.Confirm(
                message = getString(selectedImportSource.helpTextResId),
                positiveButtonText = getString(R.string.import_action),
                negativeButtonText = getString(R.string.cancel),
                neutralButtonText = docUrl?.let { getString(R.string.help) },
                onNeutralButtonClick =
                    docUrl?.let { url ->
                        {
                            val intent =
                                Intent(Intent.ACTION_VIEW)
                                    .apply { data = Uri.parse(url) }
                                    .wrapWithChooser(requireContext())
                            startActivity(intent)
                        }
                    },
                onConfirm = { showImportSourceActionDialog() },
            )
    }

    private fun showImportSourceActionDialog() {
        val context = requireContext()
        when (selectedImportSource.mimeType) {
            FOLDER_OR_FILE_MIMETYPE -> {
                activeDialogState.value =
                    SettingsDialogState.ListSelection(
                        title = getString(selectedImportSource.displayNameResId),
                        items =
                            listOf(
                                getString(R.string.folder),
                                getString(R.string.single_file),
                            ),
                        onItemClick = { choice ->
                            when (choice) {
                                0 ->
                                    importOtherActivityResultLauncher.launch(
                                        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                            .apply { addCategory(Intent.CATEGORY_DEFAULT) }
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
                                                    arrayOf("text/*") + APPLICATION_TEXT_MIME_TYPES,
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
                                arrayOf(selectedImportSource.mimeType),
                            )
                            addCategory(Intent.CATEGORY_OPENABLE)
                        }
                        .wrapWithChooser(context)
                )
        }
    }

    private fun showFeedbackDialog() {
        val options =
            listOf(
                getString(R.string.report_bug),
                getString(R.string.make_feature_request),
                getString(R.string.send_feedback),
            )
        activeDialogState.value =
            SettingsDialogState.ListSelection(
                title = getString(R.string.send_feedback),
                items = options,
                onItemClick = { which ->
                    when (which) {
                        0 -> {
                            val app = requireContext().applicationContext as Application
                            val logs = app.getLastExceptionLog()
                            reportBug(logs)
                        }

                        1 ->
                            requireContext().catchNoBrowserInstalled {
                                startActivity(
                                    Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(
                                                "https://github.com/Crustack/NotallyX/issues/new?labels=enhancement&template=feature_request.md"
                                            ),
                                        )
                                        .wrapWithChooser(requireContext())
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
                                        val app = requireContext().applicationContext as Application
                                        val log = app.getLogFile()
                                        if (log.exists()) {
                                            val uri = app.getUriForFile(log)
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                        }
                                    }
                                    .wrapWithChooser(requireContext())
                            try {
                                startActivity(intent)
                            } catch (exception: ActivityNotFoundException) {
                                showToast(R.string.install_an_email)
                            }
                        }
                    }
                },
            )
    }

    private fun showCrashesDialog() {
        (requireContext() as ContextWrapper).let { context ->
            val externalCrashesDirectory = context.getExternalCrashesDirectory()
            activeDialogState.value =
                SettingsDialogState.Confirm(
                    message =
                        "${getString(R.string.view_crash_logs)}: ${externalCrashesDirectory.absolutePath}",
                    positiveButtonText = getString(android.R.string.ok),
                    negativeButtonText = getString(R.string.cancel),
                    neutralButtonText = getString(R.string.copy),
                    onNeutralButtonClick = {
                        context.copyToClipBoard(externalCrashesDirectory.absolutePath)
                    },
                )
            context.openExternalMediaFolder(externalCrashesDirectory)
        }
    }

    private fun displayChooseBackupFolderDialog() {
        activeDialogState.value =
            SettingsDialogState.Confirm(
                message = getString(R.string.auto_backups_folder_hint),
                positiveButtonText = getString(R.string.choose_folder),
                negativeButtonText = getString(R.string.cancel),
                neutralButtonText = getString(R.string.documentation),
                onNeutralButtonClick = {
                    openDocsLink("features/backups#using-cloud-storagewebdav")
                },
                onConfirm = {
                    val intent = Intent(ACTION_OPEN_DOCUMENT_TREE).wrapWithChooser(requireContext())
                    chooseBackupFolderActivityResultLauncher.launch(intent)
                },
            )
    }

    private var pendingBiometricContinuation: (() -> Unit)? = null

    private fun showEnableBiometricLock() {
        showBiometricBackupAdvice { showEncryptDatabaseDialog() }
    }

    private fun showEncryptDatabaseDialog() {
        activeDialogState.value =
            SettingsDialogState.EncryptDatabase(
                onContinue = { shouldEncrypt ->
                    if (shouldEncrypt) {
                        AuthenticatorProvider.instance.authenticate(
                            requireActivity(),
                            isForDecrypt = false,
                            onSuccess = { cipher ->
                                requireNotNull(cipher) { "Missing cipher for biometric encryption" }
                                DatabaseTransitionActivity.start(
                                    requireActivity(),
                                    enableBiometricLock = true,
                                    encryptDatabase = true,
                                    cipher = cipher,
                                )
                            },
                        ) {
                            showBiometricsNotSetupDialog()
                        }
                    } else {
                        model.preferences.biometricLockEncryptsDb.save(false)
                        model.preferences.biometricLock.save(BiometricLock.ENABLED)
                        showToast(R.string.biometrics_setup_success)
                    }
                }
            )
    }

    private fun showDisableBiometricLock() {
        showBiometricBackupAdvice {
            if (model.preferences.biometricLockEncryptsDb.value) {
                AuthenticatorProvider.instance.authenticate(
                    requireActivity(),
                    model.preferences.iv.value,
                    isForDecrypt = true,
                    onSuccess = { cipher ->
                        DatabaseTransitionActivity.start(
                            requireActivity(),
                            enableBiometricLock = false,
                            encryptDatabase = false,
                            cipher = cipher,
                        )
                    },
                ) {}
            } else {
                model.preferences.biometricLock.save(BiometricLock.DISABLED)
                model.preferences.biometricLockEncryptsDb.save(false)
                showToast(R.string.biometrics_disable_success)
            }
        }
    }

    private fun showBiometricBackupAdvice(onContinue: () -> Unit) {
        activeDialogState.value =
            SettingsDialogState.Confirm(
                message = getString(R.string.biometric_backup_advice),
                positiveButtonText = getString(R.string.export),
                negativeButtonText = getString(R.string.cancel),
                neutralButtonText = getString(R.string.continue_),
                onNeutralButtonClick = { onContinue() },
                onConfirm = {
                    pendingBiometricContinuation = onContinue
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                            .apply {
                                type = MIME_TYPE_ZIP
                                addCategory(Intent.CATEGORY_OPENABLE)
                                putExtra(Intent.EXTRA_TITLE, buildBackupTitle())
                            }
                            .wrapWithChooser(requireContext())
                    exportBackupActivityResultLauncher.launch(intent)
                },
            )
    }

    private fun buildBackupTitle(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault())
        val ts = sdf.format(Date())
        return "NotallyX Backup $ts.zip"
    }

    private fun showBiometricsNotSetupDialog() {
        activeDialogState.value =
            SettingsDialogState.Confirm(
                message = getString(R.string.biometrics_not_setup),
                positiveButtonText = getString(R.string.tap_to_set_up),
                negativeButtonText = getString(R.string.cancel),
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

    private fun openLink(link: String) {
        val uri = link.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).wrapWithChooser(requireContext())
        startActivity(intent)
    }

    private fun openDocsLink(docPath: String) {
        openLink("https://crustack.github.io/NotallyX/docs/$docPath")
    }

    private fun askForUriPermissions(uri: Uri) {
        chooseBackupFolderActivityResultLauncher.launch(
            Intent(ACTION_OPEN_DOCUMENT_TREE).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
                }
            }
        )
    }

    companion object {
        private const val TAG = "SettingsFragment"
        const val EXTRA_SHOW_IMPORT_BACKUPS_FOLDER =
            "notallyx.intent.extra.SHOW_IMPORT_BACKUPS_FOLDER"
    }
}

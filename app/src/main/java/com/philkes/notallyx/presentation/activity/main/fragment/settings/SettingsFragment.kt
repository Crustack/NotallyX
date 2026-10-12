package com.philkes.notallyx.presentation.activity.main.fragment.settings

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
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.platform.ComposeView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
import com.google.android.material.textview.MaterialTextView
import com.philkes.notallyx.R
import com.philkes.notallyx.cancelAutoRemoveOldDeletedNotes
import com.philkes.notallyx.data.imports.Display
import com.philkes.notallyx.data.imports.FOLDER_OR_FILE_MIMETYPE
import com.philkes.notallyx.data.imports.ImportSource
import com.philkes.notallyx.data.imports.txt.APPLICATION_TEXT_MIME_TYPES
import com.philkes.notallyx.databinding.ChoiceItemBinding
import com.philkes.notallyx.databinding.DialogDatetimeFormatBinding
import com.philkes.notallyx.databinding.DialogImportBinding
import com.philkes.notallyx.databinding.DialogNotesSortBinding
import com.philkes.notallyx.databinding.DialogPreferenceBooleanBinding
import com.philkes.notallyx.databinding.DialogPreferenceEnumWithToggleBinding
import com.philkes.notallyx.databinding.DialogSelectionBoxBinding
import com.philkes.notallyx.databinding.DialogTextInputBinding
import com.philkes.notallyx.presentation.activity.DatabaseAction
import com.philkes.notallyx.presentation.activity.DatabaseTransitionActivity
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.checkedTag
import com.philkes.notallyx.presentation.dp
import com.philkes.notallyx.presentation.select
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.setEnabledSecureFlag
import com.philkes.notallyx.presentation.setupImportProgressDialog
import com.philkes.notallyx.presentation.showAndFocus
import com.philkes.notallyx.presentation.showDialog
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.misc.MenuDialog
import com.philkes.notallyx.presentation.view.misc.TextWithIconAdapter
import com.philkes.notallyx.presentation.view.screens.SettingsScreen
import com.philkes.notallyx.presentation.view.screens.createFromPreferences
import com.philkes.notallyx.presentation.viewmodel.main.fragment.SettingsViewModel
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.presentation.viewmodel.preference.BooleanPreference
import com.philkes.notallyx.presentation.viewmodel.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.presentation.viewmodel.preference.DateFormat
import com.philkes.notallyx.presentation.viewmodel.preference.EnumPreference
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSort
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSortBy
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup.Companion.BACKUP_MAX_MIN
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MIN
import com.philkes.notallyx.presentation.viewmodel.preference.SortDirection
import com.philkes.notallyx.presentation.viewmodel.preference.TextProvider
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import com.philkes.notallyx.presentation.viewmodel.preference.TimeFormat
import com.philkes.notallyx.scheduleAutoRemoveOldDeletedNotes
import com.philkes.notallyx.utils.MIME_TYPE_JSON
import com.philkes.notallyx.utils.MIME_TYPE_ZIP
import com.philkes.notallyx.utils.backup.exportPreferences
import com.philkes.notallyx.utils.catchNoBrowserInstalled
import com.philkes.notallyx.utils.copyToClipBoard
import com.philkes.notallyx.utils.getExternalCrashesDirectory
import com.philkes.notallyx.utils.getExtraBooleanFromBundleOrIntent
import com.philkes.notallyx.utils.getLastExceptionLog
import com.philkes.notallyx.utils.getLogFile
import com.philkes.notallyx.utils.getUriForFile
import com.philkes.notallyx.utils.openExternalMediaFolder
import com.philkes.notallyx.utils.reportBug
import com.philkes.notallyx.utils.security.AuthenticatorProvider
import com.philkes.notallyx.utils.viewLogs
import com.philkes.notallyx.utils.wrapWithChooser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SettingsFragment : Fragment() {

    private val model: SettingsViewModel by viewModel()
    val scrollState = ScrollState(0)

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
                    NotallyXTheme(theme = themeState.value) {
                        val uiState = requireContext().createFromPreferences(model.preferences)
                        SettingsScreen(
                            state = uiState,
                            scrollState = scrollState,
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
                                    MenuDialog(requireContext())
                                        .add(R.string.clear) { model.disableBackups() }
                                        .add(R.string.choose_another_folder) {
                                            displayChooseBackupFolderDialog()
                                        }
                                        .show()
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
                                showDialog(
                                    R.string.import_settings_message,
                                    R.string.import_action,
                                    { _, _ ->
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
                                showDialog(
                                    R.string.export_settings_message,
                                    R.string.export,
                                    { _, _ ->
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
                                showDialog(
                                    R.string.reset_settings_message,
                                    R.string.reset_settings,
                                    { _, _ ->
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
                                MaterialAlertDialogBuilder(requireContext())
                                    .setMessage(R.string.clear_data_message)
                                    .setPositiveButton(R.string.delete_all) { _, _ ->
                                        model.deleteAll()
                                    }
                                    .setCancelButton()
                                    .show()
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
        model.importProgress.setupImportProgressDialog(this@SettingsFragment)
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
                    requireContext(),
                    askForUriPermissions = ::askForUriPermissions,
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
                                    MaterialAlertDialogBuilder(it)
                                        .setMessage(
                                            R.string.please_grant_notally_notification_auto_backup
                                        )
                                        .setNegativeButton(R.string.skip, null)
                                        .setPositiveButton(R.string.continue_) { _, _ ->
                                            it.requestPermissions(arrayOf(permission), 0)
                                        }
                                        .show()
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
                val layout = DialogImportBinding.inflate(layoutInflater, null, false)
                val password = model.preferences.backupPassword.value
                layout.InputText.apply {
                    if (password != PASSWORD_EMPTY) {
                        setText(password)
                    }
                    transformationMethod = PasswordTransformationMethod.getInstance()
                }
                layout.InputTextLayout.endIconMode = END_ICON_PASSWORD_TOGGLE
                layout.Message.apply {
                    setText(R.string.import_backup_password_hint)
                    visibility = View.VISIBLE
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.backup_password)
                    .setView(layout.root)
                    .setPositiveButton(R.string.import_backup) { dialog, _ ->
                        dialog.cancel()
                        val usedPassword = layout.InputText.text.toString()
                        val checkDuplicates = layout.CheckDuplicates.isChecked
                        model.importZipBackup(uri, usedPassword, checkDuplicates)
                    }
                    .setCancelButton()
                    .show()
            }
        }
    }

    private inline fun <reified T> showEnumChoiceDialog(
        enumPreference: EnumPreference<T>,
        crossinline onSave: (newValue: T) -> Unit,
    ) where T : Enum<T>, T : TextProvider {
        val context = requireContext()
        val enumEntries = T::class.java.enumConstants!!.toList()
        val entries = enumEntries.map { it.getText(context) }.toTypedArray()
        val checked = enumEntries.indexOfFirst { it == enumPreference.value }
        MaterialAlertDialogBuilder(context)
            .setTitle(enumPreference.titleResId!!)
            .setSingleChoiceItems(entries, checked) { dialog, which ->
                dialog.cancel()
                val newValue = enumEntries[which]
                onSave(newValue)
            }
            .setCancelButton()
            .show()
    }

    private fun showThemeDialog() {
        val context = requireContext()
        val layout = DialogPreferenceEnumWithToggleBinding.inflate(layoutInflater, null, false)
        val themeValue = model.preferences.theme.value
        val useDynamicColorsValue = model.preferences.useDynamicColors.value

        Theme.entries.forEachIndexed { idx, theme ->
            ChoiceItemBinding.inflate(layoutInflater).root.apply {
                id = idx
                text = theme.getText(context)
                tag = theme
                layout.EnumRadioGroup.addView(this)
                if (theme == themeValue) {
                    layout.EnumRadioGroup.check(this.id)
                }
            }
        }

        layout.Toggle.apply {
            isVisible = DynamicColors.isDynamicColorAvailable()
            setText(R.string.theme_use_dynamic_colors)
            isChecked = useDynamicColorsValue
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(model.preferences.theme.titleResId!!)
            .setView(layout.root)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.cancel()
                val theme = layout.EnumRadioGroup.checkedTag() as Theme
                val useDynamicColors = layout.Toggle.isChecked
                model.savePreference(model.preferences.theme, theme)
                model.savePreference(model.preferences.useDynamicColors, useDynamicColors)
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
            }
            .setCancelButton()
            .show()
    }

    private fun showDateTimeFormatDialog(
        titleResId: Int,
        datePreference: EnumPreference<DateFormat>,
        timePreference: EnumPreference<TimeFormat>,
        onSave: (dateFormat: DateFormat, timeFormat: TimeFormat) -> Unit,
    ) {
        val context = requireContext()
        val layout = DialogDatetimeFormatBinding.inflate(layoutInflater, null, false)
        var selectedDate = datePreference.value
        var selectedTime = timePreference.value

        val dateEntries = DateFormat.entries.map { it.getText(context) }.toTypedArray()
        layout.DateSelectionBox.apply {
            setSimpleItems(dateEntries)
            select(selectedDate.getText(context))
            setOnItemClickListener { _, _, position, _ ->
                selectedDate = DateFormat.entries[position]
            }
        }

        val timeEntries = TimeFormat.entries.map { it.getText(context) }.toTypedArray()
        layout.TimeSelectionBox.apply {
            setSimpleItems(timeEntries)
            select(selectedTime.getText(context))
            setOnItemClickListener { _, _, position, _ ->
                selectedTime = TimeFormat.entries[position]
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(titleResId)
            .setView(layout.root)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.cancel()
                onSave(selectedDate, selectedTime)
            }
            .setCancelButton()
            .showAndFocus(allowFullSize = true)
    }

    private fun showNotesSortDialog() {
        val context = requireContext()
        val value = model.preferences.notesSorting.value
        val layout = DialogNotesSortBinding.inflate(layoutInflater, null, false)

        NotesSortBy.entries.forEachIndexed { idx, notesSortBy ->
            ChoiceItemBinding.inflate(layoutInflater).root.apply {
                id = idx
                text = context.getString(notesSortBy.textResId)
                tag = notesSortBy
                layout.NotesSortByRadioGroup.addView(this)
                setCompoundDrawablesRelativeWithIntrinsicBounds(notesSortBy.iconResId, 0, 0, 0)
                if (notesSortBy == value.sortedBy) {
                    layout.NotesSortByRadioGroup.check(this.id)
                }
            }
        }

        SortDirection.entries.forEachIndexed { idx, sortDir ->
            ChoiceItemBinding.inflate(layoutInflater).root.apply {
                id = idx
                text = context.getString(sortDir.textResId)
                tag = sortDir
                setCompoundDrawablesRelativeWithIntrinsicBounds(sortDir.iconResId, 0, 0, 0)
                layout.NotesSortDirectionRadioGroup.addView(this)
                if (sortDir == value.sortDirection) {
                    layout.NotesSortDirectionRadioGroup.check(this.id)
                }
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(model.preferences.notesSorting.titleResId!!)
            .setView(layout.root)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.cancel()
                val newSortBy = layout.NotesSortByRadioGroup.checkedTag() as NotesSortBy
                val newSortDirection =
                    layout.NotesSortDirectionRadioGroup.checkedTag() as SortDirection
                model.savePreference(
                    model.preferences.notesSorting,
                    NotesSort(newSortBy, newSortDirection),
                )
            }
            .setCancelButton()
            .show()
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
        val context = requireContext()
        if (!enabled) return
        val value = preference.value
        val layout =
            DialogPreferenceBooleanBinding.inflate(layoutInflater, null, false).apply {
                Title.setText(preference.titleResId!!)
                messageResId?.let { Message.setText(it) }
                if (value) {
                    EnabledButton.isChecked = true
                } else {
                    DisabledButton.isChecked = true
                }
            }
        val dialog =
            MaterialAlertDialogBuilder(context).setView(layout.root).setCancelButton().show()
        layout.apply {
            EnabledButton.setOnClickListener {
                dialog.cancel()
                if (!value) {
                    onSave.invoke(true)
                }
            }
            DisabledButton.setOnClickListener {
                dialog.cancel()
                if (value) {
                    onSave.invoke(false)
                }
            }
        }
    }

    private fun showStartViewDialog(labels: List<String>?) {
        val context = requireContext()
        val preference = model.preferences.startView
        val value = preference.value
        val notesText =
            "${context.getText(R.string.notes)} (${context.getText(R.string.text_default)})"
        val unlabeledText = context.getText(R.string.unlabeled).toString()
        val textValue =
            when (value) {
                START_VIEW_DEFAULT -> notesText
                START_VIEW_UNLABELED -> unlabeledText
                else -> value
            }

        val layout = DialogSelectionBoxBinding.inflate(layoutInflater, null, false)
        layout.Message.setText(R.string.start_view_hint)
        val values =
            mutableListOf(notesText to START_VIEW_DEFAULT, unlabeledText to START_VIEW_UNLABELED)
                .apply { labels?.forEach { add(it to it) } }
        var selected = values.indexOfFirst { it.first == textValue }
        layout.SelectionBox.apply {
            setSimpleItems(values.map { it.first }.toTypedArray())
            select(textValue)
            setOnItemClickListener { _, _, position, _ -> selected = position }
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(preference.titleResId!!)
            .setView(layout.root)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.cancel()
                if (selected >= 0 && selected < values.size) {
                    val newValue = values[selected].second
                    model.savePreference(preference, newValue)
                }
            }
            .setCancelButton()
            .showAndFocus(allowFullSize = true)
    }

    private fun showPeriodicBackupDialog() {
        val context = requireContext()
        val backupFolder = model.preferences.backupsFolder.value
        if (backupFolder == EMPTY_PATH) return
        val value = model.preferences.periodicBackups.value.periodInDays > 0
        val layout =
            DialogPreferenceBooleanBinding.inflate(layoutInflater, null, false).apply {
                Title.setText(R.string.backup_periodic)
                Message.setText(R.string.backup_periodic_hint)
                if (value) {
                    EnabledButton.isChecked = true
                } else {
                    DisabledButton.isChecked = true
                }
            }
        val dialog =
            MaterialAlertDialogBuilder(context).setView(layout.root).setCancelButton().show()
        layout.apply {
            EnabledButton.setOnClickListener {
                dialog.cancel()
                if (!value) {
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
                }
            }
            DisabledButton.setOnClickListener {
                dialog.cancel()
                if (value) {
                    model.savePreference(
                        model.preferences.periodicBackups,
                        model.preferences.periodicBackups.value.copy(periodInDays = 0),
                    )
                }
            }
        }
    }

    private fun showBackupPasswordDialog() {
        val context = requireContext()
        val preference = model.preferences.backupPassword
        val password = preference.value
        val layout = DialogTextInputBinding.inflate(layoutInflater, null, false)
        layout.InputText.apply {
            if (password != PASSWORD_EMPTY) {
                setText(password)
            }
            transformationMethod = PasswordTransformationMethod.getInstance()
        }
        layout.InputTextLayout.endIconMode = END_ICON_PASSWORD_TOGGLE
        layout.Message.apply {
            setText(R.string.backup_password_hint)
            visibility = View.VISIBLE
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(preference.titleResId!!)
            .setView(layout.root)
            .setPositiveButton(R.string.save) { dialog, _ ->
                dialog.cancel()
                val updatedPassword = layout.InputText.text.toString()
                model.savePreference(preference, updatedPassword)
            }
            .setCancelButton()
            .setNeutralButton(R.string.clear) { dialog, _ ->
                dialog.cancel()
                model.savePreference(preference, PASSWORD_EMPTY)
            }
            .showAndFocus(allowFullSize = true)
    }

    private fun showBiometricLockDialog() {
        val context = requireContext()
        val preference = model.preferences.biometricLock
        val value = preference.value
        val enumEntries = BiometricLock.entries
        val entries = enumEntries.map { context.getString(it.textResId) }.toTypedArray()
        val checked = enumEntries.indexOfFirst { it == value }

        MaterialAlertDialogBuilder(context)
            .setTitle(preference.titleResId!!)
            .setSingleChoiceItems(entries, checked) { dialog, which ->
                dialog.cancel()
                val newValue = enumEntries[which]
                if (newValue == value) {
                    return@setSingleChoiceItems
                }
                if (newValue == BiometricLock.ENABLED) {
                    when (AuthenticatorProvider.instance.canAuthenticateWithBiometrics(context)) {
                        BiometricManager.BIOMETRIC_SUCCESS -> showEnableBiometricLock()
                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                            context.showToast(R.string.biometrics_no_support)
                        }

                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                            showBiometricsNotSetupDialog()
                    }
                } else {
                    when (AuthenticatorProvider.instance.canAuthenticateWithBiometrics(context)) {
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
            }
            .setCancelButton()
            .show()
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
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_other_app)
            .setAdapter(
                TextWithIconAdapter(
                    requireContext(),
                    notallyItems + ImportSource.entries.toMutableList(),
                    { item -> getString(item.getTextId()) },
                    Display::getIconId,
                )
            ) { _, which ->
                when (which) {
                    0 -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(
                                getString(
                                    R.string.import_from_notally,
                                    getString(R.string.import_backup),
                                )
                            )
                            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                        return@setAdapter
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
                        return@setAdapter
                    }
                }
                selectedImportSource = ImportSource.entries[which - notallyItems.size]
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(selectedImportSource.helpTextResId)
                    .setPositiveButton(R.string.import_action) { dialog, _ ->
                        dialog.cancel()
                        when (selectedImportSource.mimeType) {
                            FOLDER_OR_FILE_MIMETYPE ->
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(selectedImportSource.displayNameResId)
                                    .setItems(
                                        arrayOf(
                                            getString(R.string.folder),
                                            getString(R.string.single_file),
                                        )
                                    ) { _, which ->
                                        when (which) {
                                            0 ->
                                                importOtherActivityResultLauncher.launch(
                                                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                                                        .apply {
                                                            addCategory(Intent.CATEGORY_DEFAULT)
                                                        }
                                                        .wrapWithChooser(requireContext())
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
                                                        .wrapWithChooser(requireContext())
                                                )
                                        }
                                    }
                                    .setCancelButton()
                                    .show()

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
                                        .wrapWithChooser(requireContext())
                                )
                        }
                    }
                    .also {
                        selectedImportSource.documentationUrl?.let<String, Unit> { docUrl ->
                            it.setNegativeButton(R.string.help) { _, _ ->
                                val intent =
                                    Intent(Intent.ACTION_VIEW)
                                        .apply { data = Uri.parse(docUrl) }
                                        .wrapWithChooser(requireContext())
                                startActivity(intent)
                            }
                        }
                    }
                    .setNeutralButton(R.string.cancel) { dialog, _ -> dialog.cancel() }
                    .showAndFocus(allowFullSize = true)
            }
            .setCancelButton()
            .show()
    }

    private fun showFeedbackDialog() {
        val options =
            arrayOf(
                getString(R.string.report_bug),
                getString(R.string.make_feature_request),
                getString(R.string.send_feedback),
            )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.send_feedback)
            .setItems(options) { _, which ->
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
                                    selector = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("notallyx@yahoo.com"))
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
            }
            .setCancelButton()
            .show()
    }

    private fun showCrashesDialog() {
        (requireContext() as ContextWrapper).let { context ->
            val externalCrashesDirectory = context.getExternalCrashesDirectory()
            MaterialAlertDialogBuilder(context)
                .setMessage(
                    "${getString(R.string.view_crash_logs)}: ${externalCrashesDirectory.absolutePath}"
                )
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.copy) { _, _ ->
                    context.copyToClipBoard(externalCrashesDirectory.absolutePath)
                }
                .show()
            context.openExternalMediaFolder(externalCrashesDirectory)
        }
    }

    private fun displayChooseBackupFolderDialog() {
        showDialog(
            getString(R.string.auto_backups_folder_hint),
            R.string.choose_folder,
            { _, _ ->
                val intent = Intent(ACTION_OPEN_DOCUMENT_TREE).wrapWithChooser(requireContext())
                chooseBackupFolderActivityResultLauncher.launch(intent)
            },
            R.string.documentation,
            { _, _ -> openDocsLink("features/backups#using-cloud-storagewebdav") },
            fullSize = true,
        )
    }

    private var pendingBiometricContinuation: (() -> Unit)? = null

    private fun showEnableBiometricLock() {
        showBiometricBackupAdvice { showEncryptDatabaseDialog() }
    }

    private fun showEncryptDatabaseDialog() {
        val padding = 20.dp
        val layout =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding / 2, padding, 0)
            }
        val messageView =
            MaterialTextView(requireContext()).apply { setText(R.string.encrypt_database_advice) }
        val checkBox =
            MaterialCheckBox(requireContext()).apply {
                setText(R.string.encrypt_database_via_biometric)
                isChecked = false
            }
        layout.addView(messageView)
        layout.addView(checkBox)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.biometric_lock)
            .setView(layout)
            .setPositiveButton(R.string.continue_) { _, _ ->
                val shouldEncrypt = checkBox.isChecked
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
            .setNegativeButton(R.string.cancel, null)
            .show()
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
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.biometric_backup_advice)
            .setPositiveButton(R.string.export) { _, _ ->
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
            }
            .setNeutralButton(R.string.continue_) { _, _ -> onContinue() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun buildBackupTitle(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault())
        val ts = sdf.format(Date())
        return "NotallyX Backup $ts.zip"
    }

    private fun showBiometricsNotSetupDialog() {
        showDialog(
            R.string.biometrics_not_setup,
            R.string.tap_to_set_up,
            { _, _ ->
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

package com.philkes.notallyx.feature.settings

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.philkes.notallyx.R
import com.philkes.notallyx.cancelAutoRemoveOldDeletedNotes
import com.philkes.notallyx.core.common.extensions.copyToClipBoard
import com.philkes.notallyx.core.common.extensions.deleteAttachments
import com.philkes.notallyx.core.common.extensions.getBackupDir
import com.philkes.notallyx.core.common.extensions.getExternalCrashesDirectory
import com.philkes.notallyx.core.common.extensions.getLastExceptionLog
import com.philkes.notallyx.core.common.extensions.getLogFile
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.toMessage
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.BasePreference
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_MAX_MIN
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MIN
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.model.SecurePreferences
import com.philkes.notallyx.core.preference.model.UserPreferences
import com.philkes.notallyx.core.security.AuthenticatorProvider
import com.philkes.notallyx.core.ui.dialog.IconListItem
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.extensions.exportedText
import com.philkes.notallyx.core.ui.extensions.restartApplication
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.feature.migration.backup.DatabaseAction
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.feature.migration.backup.exportAsZip
import com.philkes.notallyx.feature.migration.backup.exportPreferences
import com.philkes.notallyx.feature.migration.backup.importRawDatabase
import com.philkes.notallyx.feature.migration.backup.importZip
import com.philkes.notallyx.feature.migration.backup.readAsBackup
import com.philkes.notallyx.feature.migration.imports.Display
import com.philkes.notallyx.feature.migration.imports.FOLDER_OR_FILE_MIMETYPE
import com.philkes.notallyx.feature.migration.imports.ImportException
import com.philkes.notallyx.feature.migration.imports.ImportSource
import com.philkes.notallyx.feature.migration.imports.NotesImporter
import com.philkes.notallyx.feature.migration.imports.txt.APPLICATION_TEXT_MIME_TYPES
import com.philkes.notallyx.feature.reminder.cancelPinAndReminders
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.scheduleAutoRemoveOldDeletedNotes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val app: Application,
    val preferences: NotallyXPreferences,
) : AndroidViewModel(app) {

    private var database: NotallyDatabase? = null
    private lateinit var baseNoteDao: BaseNoteDao
    private lateinit var labelDao: LabelDao
    private lateinit var commonDao: CommonDao

    private val appVersion: String by lazy {
        try {
            val pInfo = app.packageManager.getPackageInfo(app.packageName, 0)
            pInfo.versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private enum class BiometricFlowState {
        IDLE,
        ENABLE_PENDING_EXPORT,
        DISABLE_PENDING_EXPORT,
    }

    private var biometricFlowState = BiometricFlowState.IDLE
    private var selectedImportSource: ImportSource? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val labels: StateFlow<List<Label>> =
        NotallyDatabase.getDatabase(app)
            .flatMapLatest { database -> database?.getLabelDao()?.getAll() ?: flowOf(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    val importProgress: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)
    val progress: StateFlow<Progress?>
        field = MutableStateFlow<Progress?>(null)

    private val _activeDialog = MutableStateFlow<SettingsDialogState?>(null)
    private val _isLoading = MutableStateFlow(false)

    private val _sideEffects = Channel<SettingsSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<SettingsSideEffect> = _sideEffects.receiveAsFlow()

    private val preferencesUiModel: StateFlow<PreferencesUiModel> =
        combine(
                preferences.userPreferencesFlow,
                preferences.securePreferencesFlow,
            ) { userPrefs, securePrefs ->
                toPreferencesUiModel(userPrefs, securePrefs, appVersion)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    toPreferencesUiModel(
                        preferences.userPreferencesFlow.value,
                        preferences.securePreferencesFlow.value,
                        appVersion,
                    ),
            )

    val uiState: StateFlow<SettingsUiState> =
        combine(
                preferencesUiModel,
                labels,
                importProgress,
                _activeDialog,
                _isLoading,
            ) { prefs, lbls, prog, dialog, loading ->
                SettingsUiState(
                    preferences = prefs,
                    labels = lbls,
                    importProgress = prog,
                    activeDialog = dialog,
                    isLoading = loading,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    SettingsUiState(
                        preferences = preferencesUiModel.value,
                        labels = labels.value,
                        importProgress = importProgress.value,
                        activeDialog = _activeDialog.value,
                        isLoading = _isLoading.value,
                    ),
            )

    internal var showRefreshBackupsFolderAfterThemeChange = false
    internal var previousBackupsFolder: String? = null

    init {
        viewModelScope.launch { NotallyDatabase.getDatabase(app).collect { init(it) } }
    }

    private fun init(database: NotallyDatabase?) {
        if (database == null) return
        this.database = database
        baseNoteDao = database.getBaseNoteDao()
        labelDao = database.getLabelDao()
        commonDao = database.getCommonDao()
    }

    private fun emitSideEffect(effect: SettingsSideEffect) {
        viewModelScope.launch { _sideEffects.send(effect) }
    }

    fun buildBackupTitle(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH-mm", Locale.getDefault())
        val ts = sdf.format(Date())
        return "NotallyX Backup $ts.zip"
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.DismissDialog -> {
                _activeDialog.update { null }
            }

            // Appearance
            SettingsAction.ShowNotesViewDialog -> {
                val items = NotesView.entries.map { SelectionItem(it, it.getText(app)) }
                _activeDialog.update {
                    SettingsDialogState.SingleSelection(
                        title = app.getString(preferences.notesView.titleResId!!),
                        items = items,
                        selectedItem = preferences.notesView.value,
                        onConfirm = { onAction(SettingsAction.SetNotesView(it)) },
                    )
                }
            }
            is SettingsAction.SetNotesView -> {
                savePreference(preferences.notesView, action.notesView)
                _activeDialog.update { null }
            }
            SettingsAction.ShowThemeDialog -> {
                _activeDialog.update {
                    SettingsDialogState.Theme(
                        initialTheme = preferences.theme.value,
                        initialUseDynamicColors = preferences.useDynamicColors.value,
                        onConfirm = { theme, dynamicColors ->
                            onAction(SettingsAction.SetTheme(theme, dynamicColors))
                        },
                    )
                }
            }
            is SettingsAction.SetTheme -> {
                savePreference(preferences.theme, action.theme)
                savePreference(preferences.useDynamicColors, action.dynamicColors)
                _activeDialog.update { null }
            }
            SettingsAction.ShowDateFormatOverviewDialog -> {
                _activeDialog.update {
                    SettingsDialogState.DateTimeFormat(
                        initialDate = preferences.dateFormatOverview.value,
                        initialTime = preferences.timeFormatOverview.value,
                        onConfirm = { date, time ->
                            onAction(SettingsAction.SetDateFormatOverview(date, time))
                        },
                    )
                }
            }
            is SettingsAction.SetDateFormatOverview -> {
                savePreference(preferences.dateFormatOverview, action.dateFormat)
                savePreference(preferences.timeFormatOverview, action.timeFormat)
                _activeDialog.update { null }
            }
            SettingsAction.ShowDateFormatNoteViewDialog -> {
                _activeDialog.update {
                    SettingsDialogState.DateTimeFormat(
                        initialDate = preferences.dateFormatNoteView.value,
                        initialTime = preferences.timeFormatNoteView.value,
                        onConfirm = { date, time ->
                            onAction(SettingsAction.SetDateFormatNoteView(date, time))
                        },
                    )
                }
            }
            is SettingsAction.SetDateFormatNoteView -> {
                savePreference(preferences.dateFormatNoteView, action.dateFormat)
                savePreference(preferences.timeFormatNoteView, action.timeFormat)
                _activeDialog.update { null }
            }
            is SettingsAction.SetTextSizeNoteEditor -> {
                savePreference(preferences.textSizeNoteEditor, action.textSize)
            }
            is SettingsAction.SetTextSizeOverview -> {
                savePreference(preferences.textSizeOverview, action.textSize)
            }
            SettingsAction.ShowAlwaysShowSearchBarDialog -> {
                _activeDialog.update {
                    SettingsDialogState.BooleanPref(
                        title = app.getString(preferences.alwaysShowSearchBar.titleResId!!),
                        message = app.getString(R.string.always_show_search_bar_hint),
                        initialValue = preferences.alwaysShowSearchBar.value,
                        onConfirm = { onAction(SettingsAction.SetAlwaysShowSearchBar(it)) },
                    )
                }
            }
            is SettingsAction.SetAlwaysShowSearchBar -> {
                savePreference(preferences.alwaysShowSearchBar, action.enabled)
                _activeDialog.update { null }
            }

            // Behaviour
            SettingsAction.ShowNotesSortDialog -> {
                _activeDialog.update {
                    SettingsDialogState.NotesSort(
                        currentSort = preferences.notesSorting.value,
                        onConfirm = { onAction(SettingsAction.SetNotesSort(it)) },
                    )
                }
            }
            is SettingsAction.SetNotesSort -> {
                savePreference(preferences.notesSorting, action.sort)
                _activeDialog.update { null }
            }
            SettingsAction.ShowListItemSortingDialog -> {
                val items = ListItemSort.entries.map { SelectionItem(it, it.getText(app)) }
                _activeDialog.update {
                    SettingsDialogState.SingleSelection(
                        title = app.getString(preferences.listItemSorting.titleResId!!),
                        items = items,
                        selectedItem = preferences.listItemSorting.value,
                        onConfirm = { onAction(SettingsAction.SetListItemSorting(it)) },
                    )
                }
            }
            is SettingsAction.SetListItemSorting -> {
                savePreference(preferences.listItemSorting, action.sorting)
                _activeDialog.update { null }
            }
            SettingsAction.ShowStartViewDialog -> {
                val preference = preferences.startView
                val value = preference.value
                val notesText =
                    "${app.getText(R.string.notes)} (${app.getText(R.string.text_default)})"
                val unlabeledText = app.getText(R.string.unlabeled).toString()
                val values =
                    mutableListOf(
                            notesText to START_VIEW_DEFAULT,
                            unlabeledText to START_VIEW_UNLABELED,
                        )
                        .apply { labels.value.forEach { add(it.value to it.value) } }
                val items = values.map { SelectionItem(it.second, it.first) }
                _activeDialog.update {
                    SettingsDialogState.SingleSelection(
                        title = app.getString(preference.titleResId!!),
                        items = items,
                        selectedItem = value,
                        onConfirm = { onAction(SettingsAction.SetStartView(it)) },
                    )
                }
            }
            is SettingsAction.SetStartView -> {
                savePreference(preferences.startView, action.startView)
                _activeDialog.update { null }
            }
            SettingsAction.ShowDefaultListNoteViewModeDialog -> {
                val items =
                    DefaultListNoteViewMode.entries.map { SelectionItem(it, it.getText(app)) }
                _activeDialog.update {
                    SettingsDialogState.SingleSelection(
                        title = app.getString(preferences.defaultListNoteViewMode.titleResId!!),
                        items = items,
                        selectedItem = preferences.defaultListNoteViewMode.value,
                        onConfirm = { onAction(SettingsAction.SetDefaultListNoteViewMode(it)) },
                    )
                }
            }
            is SettingsAction.SetDefaultListNoteViewMode -> {
                savePreference(preferences.defaultListNoteViewMode, action.mode)
                _activeDialog.update { null }
            }
            is SettingsAction.SetAutoSaveAfterIdleTime -> {
                savePreference(preferences.autoSaveAfterIdleTime, action.seconds)
            }
            is SettingsAction.UpdateAutoRemoveDays -> {
                updateAutoRemoveDeletedNotes(action.days)
            }

            // Content Density
            is SettingsAction.SetMaxTitle -> {
                savePreference(preferences.maxTitle, action.maxTitle)
            }
            is SettingsAction.SetMaxItems -> {
                savePreference(preferences.maxItems, action.maxItems)
            }
            is SettingsAction.SetMaxLines -> {
                savePreference(preferences.maxLines, action.maxLines)
            }
            is SettingsAction.SetMaxLabels -> {
                savePreference(preferences.maxLabels, action.maxLabels)
            }
            SettingsAction.ShowLabelTagsHiddenDialog -> {
                _activeDialog.update {
                    SettingsDialogState.BooleanPref(
                        title = app.getString(preferences.labelTagsHiddenInOverview.titleResId!!),
                        message = app.getString(R.string.labels_hidden_in_overview_title),
                        initialValue = preferences.labelTagsHiddenInOverview.value,
                        onConfirm = { onAction(SettingsAction.SetLabelTagsHidden(it)) },
                    )
                }
            }
            is SettingsAction.SetLabelTagsHidden -> {
                savePreference(preferences.labelTagsHiddenInOverview, action.hidden)
                _activeDialog.update { null }
            }
            SettingsAction.ShowImagesHiddenDialog -> {
                _activeDialog.update {
                    SettingsDialogState.BooleanPref(
                        title = app.getString(preferences.imagesHiddenInOverview.titleResId!!),
                        message = app.getString(R.string.images_hidden_in_overview_title),
                        initialValue = preferences.imagesHiddenInOverview.value,
                        onConfirm = { onAction(SettingsAction.SetImagesHidden(it)) },
                    )
                }
            }
            is SettingsAction.SetImagesHidden -> {
                savePreference(preferences.imagesHiddenInOverview, action.hidden)
                _activeDialog.update { null }
            }

            // Backup & Restore
            SettingsAction.RequestImportBackup -> {
                emitSideEffect(SettingsSideEffect.LaunchImportBackup)
            }
            is SettingsAction.ImportBackupSelected -> {
                val mimeType = action.mimeType ?: app.contentResolver.getType(action.uri)
                if (mimeType == "text/xml" || action.uri.toString().endsWith(".xml")) {
                    importXmlBackup(action.uri)
                } else {
                    val defaultPassword =
                        preferences.backupPassword.value.takeIf { it != PASSWORD_EMPTY } ?: ""
                    _activeDialog.update {
                        SettingsDialogState.ImportNotesPassword(
                            initialPassword = defaultPassword,
                            onConfirm = { usedPassword, checkDuplicates ->
                                onAction(
                                    SettingsAction.ImportZipConfirmed(
                                        action.uri,
                                        usedPassword,
                                        checkDuplicates,
                                    )
                                )
                            },
                        )
                    }
                }
            }
            is SettingsAction.ImportZipConfirmed -> {
                _activeDialog.update { null }
                importZipBackup(action.uri, action.password, action.checkDuplicates)
            }
            SettingsAction.ShowImportOtherDialog -> {
                showImportOtherDialog()
            }
            is SettingsAction.SelectImportOtherSource -> {
                handleImportSourceSelection(action.which)
            }
            is SettingsAction.ImportRawDatabaseSelected -> {
                importRawDatabase(action.uri, false)
            }
            is SettingsAction.ImportOtherFileSelected -> {
                selectedImportSource?.let { source -> importFromOtherApp(action.uri, source) }
            }
            SettingsAction.RequestExportBackup -> {
                emitSideEffect(SettingsSideEffect.LaunchExportBackup(buildBackupTitle()))
            }
            is SettingsAction.ExportBackupConfirmed -> {
                exportBackup(action.uri)
            }
            SettingsAction.ClickBackupsFolder -> {
                val folder = preferences.backupsFolder.value
                if (folder == EMPTY_PATH) {
                    displayChooseBackupFolderDialog()
                } else {
                    _activeDialog.update {
                        SettingsDialogState.ListSelection(
                            title = app.getString(R.string.auto_backups_folder),
                            items =
                                listOf(
                                    app.getString(R.string.clear),
                                    app.getString(R.string.choose_another_folder),
                                ),
                            onItemClick = { which ->
                                when (which) {
                                    0 -> {
                                        disableBackups()
                                        _activeDialog.update { null }
                                    }
                                    1 -> displayChooseBackupFolderDialog()
                                }
                            },
                        )
                    }
                }
            }
            is SettingsAction.SetupBackupsFolder -> {
                setupBackupsFolder(action.uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permission = Manifest.permission.POST_NOTIFICATIONS
                    if (
                        ContextCompat.checkSelfPermission(app, permission) !=
                            PackageManager.PERMISSION_GRANTED
                    ) {
                        _activeDialog.update {
                            SettingsDialogState.Confirm(
                                message =
                                    app.getString(
                                        R.string.please_grant_notally_notification_auto_backup
                                    ),
                                positiveButtonText = app.getString(R.string.continue_),
                                negativeButtonText = app.getString(R.string.skip),
                                onConfirm = {
                                    _activeDialog.update { null }
                                    emitSideEffect(
                                        SettingsSideEffect.RequestNotificationPermission(permission)
                                    )
                                },
                            )
                        }
                    }
                }
            }
            SettingsAction.CancelBackupsFolderSelection -> {
                if (showRefreshBackupsFolderAfterThemeChange || previousBackupsFolder != null) {
                    cancelFolderSelection()
                }
            }
            SettingsAction.ShowBackupOnSaveDialog -> {
                if (preferences.backupsFolder.value != EMPTY_PATH) {
                    _activeDialog.update {
                        SettingsDialogState.BooleanPref(
                            title = app.getString(preferences.backupOnSave.titleResId!!),
                            message = app.getString(R.string.auto_backup_on_save),
                            initialValue = preferences.backupOnSave.value,
                            onConfirm = { onAction(SettingsAction.SetBackupOnSave(it)) },
                        )
                    }
                }
            }
            is SettingsAction.SetBackupOnSave -> {
                savePreference(preferences.backupOnSave, action.enabled)
                _activeDialog.update { null }
            }
            SettingsAction.ShowPeriodicBackupsDialog -> {
                if (preferences.backupsFolder.value != EMPTY_PATH) {
                    _activeDialog.update {
                        SettingsDialogState.BooleanPref(
                            title = app.getString(R.string.backup_periodic),
                            message = app.getString(R.string.backup_periodic_hint),
                            initialValue = preferences.periodicBackups.value.periodInDays > 0,
                            onConfirm = { onAction(SettingsAction.SetPeriodicBackupsEnabled(it)) },
                        )
                    }
                }
            }
            is SettingsAction.SetPeriodicBackupsEnabled -> {
                _activeDialog.update { null }
                if (action.enabled) {
                    val current = preferences.periodicBackups.value
                    val periodInDays =
                        if (current.periodInDays >= BACKUP_PERIOD_DAYS_MIN) current.periodInDays
                        else BACKUP_PERIOD_DAYS_MIN
                    val maxBackups =
                        if (current.maxBackups >= BACKUP_MAX_MIN) current.maxBackups
                        else BACKUP_MAX_MIN
                    savePreference(
                        preferences.periodicBackups,
                        current.copy(periodInDays = periodInDays, maxBackups = maxBackups),
                    )
                } else {
                    val current = preferences.periodicBackups.value
                    savePreference(
                        preferences.periodicBackups,
                        current.copy(periodInDays = 0),
                    )
                }
            }
            is SettingsAction.SetPeriodicBackupsPeriodInDays -> {
                val current = preferences.periodicBackups.value
                savePreference(
                    preferences.periodicBackups,
                    current.copy(periodInDays = action.days),
                )
            }
            is SettingsAction.SetPeriodicBackupsMax -> {
                val current = preferences.periodicBackups.value
                savePreference(preferences.periodicBackups, current.copy(maxBackups = action.max))
            }

            // Security
            SettingsAction.ShowBiometricLockDialog -> {
                val preference = preferences.biometricLock
                val value = preference.value
                val items =
                    BiometricLock.entries.map { SelectionItem(it, app.getString(it.textResId)) }
                _activeDialog.update {
                    SettingsDialogState.SingleSelection(
                        title = app.getString(preference.titleResId!!),
                        items = items,
                        selectedItem = value,
                        onConfirm = { onAction(SettingsAction.SelectBiometricLock(it)) },
                    )
                }
            }
            is SettingsAction.SelectBiometricLock -> {
                _activeDialog.update { null }
                handleSelectBiometricLock(action.lock)
            }
            is SettingsAction.BiometricAuthSuccess -> {
                _activeDialog.update { null }
                if (action.isEncrypt) {
                    emitSideEffect(
                        SettingsSideEffect.StartBiometricDatabaseTransition(
                            enableBiometricLock = true,
                            encryptDatabase = true,
                            cipher = action.cipher,
                        )
                    )
                } else {
                    emitSideEffect(
                        SettingsSideEffect.StartBiometricDatabaseTransition(
                            enableBiometricLock = false,
                            encryptDatabase = false,
                            cipher = action.cipher,
                        )
                    )
                }
            }
            SettingsAction.BiometricEnrollFinished -> {
                showEnableBiometricLock()
            }
            SettingsAction.CancelBiometricFlow -> {
                biometricFlowState = BiometricFlowState.IDLE
            }
            SettingsAction.ShowBackupPasswordDialog -> {
                val password =
                    preferences.backupPassword.value.takeIf { it != PASSWORD_EMPTY } ?: ""
                _activeDialog.update {
                    SettingsDialogState.BackupPassword(
                        password = password,
                        onConfirm = { onAction(SettingsAction.SetBackupPassword(it)) },
                    )
                }
            }
            is SettingsAction.SetBackupPassword -> {
                savePreference(preferences.backupPassword, action.password)
                _activeDialog.update { null }
            }
            SettingsAction.ShowSecureFlagDialog -> {
                _activeDialog.update {
                    SettingsDialogState.BooleanPref(
                        title = app.getString(preferences.secureFlag.titleResId!!),
                        message = null,
                        initialValue = preferences.secureFlag.value,
                        onConfirm = { onAction(SettingsAction.SetSecureFlag(it)) },
                    )
                }
            }
            is SettingsAction.SetSecureFlag -> {
                savePreference(preferences.secureFlag, action.enabled)
                emitSideEffect(SettingsSideEffect.SetWindowSecure(action.enabled))
                _activeDialog.update { null }
            }

            // General
            SettingsAction.RequestImportSettings -> {
                _activeDialog.update {
                    SettingsDialogState.Confirm(
                        message = app.getString(R.string.import_settings_message),
                        positiveButtonText = app.getString(R.string.import_action),
                        negativeButtonText = app.getString(R.string.cancel),
                        onConfirm = {
                            _activeDialog.update { null }
                            emitSideEffect(SettingsSideEffect.LaunchImportSettings)
                        },
                    )
                }
            }
            is SettingsAction.ImportSettingsConfirmed -> {
                importSettings(action.uri)
            }
            SettingsAction.RequestExportSettings -> {
                _activeDialog.update {
                    SettingsDialogState.Confirm(
                        message = app.getString(R.string.export_settings_message),
                        positiveButtonText = app.getString(R.string.export),
                        negativeButtonText = app.getString(R.string.cancel),
                        onConfirm = {
                            _activeDialog.update { null }
                            emitSideEffect(
                                SettingsSideEffect.LaunchExportSettings("NotallyX_Settings.json")
                            )
                        },
                    )
                }
            }
            is SettingsAction.ExportSettingsConfirmed -> {
                exportSettings(action.uri)
            }
            SettingsAction.RequestResetSettings -> {
                _activeDialog.update {
                    SettingsDialogState.Confirm(
                        message = app.getString(R.string.reset_settings_message),
                        positiveButtonText = app.getString(R.string.reset_settings),
                        negativeButtonText = app.getString(R.string.cancel),
                        onConfirm = {
                            _activeDialog.update { null }
                            onAction(SettingsAction.ResetSettingsConfirmed)
                        },
                    )
                }
            }
            SettingsAction.ResetSettingsConfirmed -> {
                resetPreferences()
            }
            SettingsAction.ShowDataInPublicFolderDialog -> {
                _activeDialog.update {
                    SettingsDialogState.BooleanPref(
                        title = app.getString(preferences.dataInPublicFolder.titleResId!!),
                        message = app.getString(R.string.data_in_public_message),
                        initialValue = preferences.dataInPublicFolder.value,
                        onConfirm = { onAction(SettingsAction.SetDataInPublicFolder(it)) },
                    )
                }
            }
            is SettingsAction.SetDataInPublicFolder -> {
                _activeDialog.update { null }
                val actionTransition =
                    if (action.enabled) DatabaseAction.ENABLE_DATA_IN_PUBLIC
                    else DatabaseAction.DISABLE_DATA_IN_PUBLIC
                emitSideEffect(SettingsSideEffect.StartDatabaseTransition(actionTransition))
            }
            SettingsAction.RequestClearData -> {
                _activeDialog.update {
                    SettingsDialogState.Confirm(
                        message = app.getString(R.string.clear_data_message),
                        positiveButtonText = app.getString(R.string.delete_all),
                        negativeButtonText = app.getString(R.string.cancel),
                        onConfirm = {
                            _activeDialog.update { null }
                            onAction(SettingsAction.ClearDataConfirmed)
                        },
                    )
                }
            }
            SettingsAction.ClearDataConfirmed -> {
                deleteAll()
            }

            // About
            SettingsAction.ShowSendFeedbackDialog -> {
                val options =
                    listOf(
                        app.getString(R.string.report_bug),
                        app.getString(R.string.make_feature_request),
                        app.getString(R.string.send_feedback),
                    )
                _activeDialog.update {
                    SettingsDialogState.ListSelection(
                        title = app.getString(R.string.send_feedback),
                        items = options,
                        onItemClick = { onAction(SettingsAction.SelectFeedbackOption(it)) },
                    )
                }
            }
            is SettingsAction.SelectFeedbackOption -> {
                _activeDialog.update { null }
                when (action.which) {
                    0 -> emitSideEffect(SettingsSideEffect.ReportBug(app.getLastExceptionLog()))
                    1 ->
                        emitSideEffect(
                            SettingsSideEffect.OpenBrowser(
                                "https://github.com/Crustack/NotallyX/issues/new?labels=enhancement&template=feature_request.md"
                            )
                        )
                    2 -> {
                        val log = app.getLogFile()
                        val uri = if (log.exists()) app.getUriForFile(log) else null
                        emitSideEffect(SettingsSideEffect.SendFeedbackEmail(uri))
                    }
                }
            }
            SettingsAction.RateApp -> {
                emitSideEffect(
                    SettingsSideEffect.OpenBrowser(
                        "https://play.google.com/store/apps/details?id=com.philkes.notallyx"
                    )
                )
            }
            SettingsAction.Donate -> {
                emitSideEffect(SettingsSideEffect.OpenBrowser("https://ko-fi.com/crustack"))
            }
            SettingsAction.ViewLogs -> {
                emitSideEffect(SettingsSideEffect.ViewLogs)
            }
            SettingsAction.ShowCrashesDialog -> {
                val externalCrashesDirectory = app.getExternalCrashesDirectory()
                _activeDialog.update {
                    SettingsDialogState.Confirm(
                        message =
                            "${app.getString(R.string.view_crash_logs)}: ${externalCrashesDirectory.absolutePath}",
                        positiveButtonText = app.getString(android.R.string.ok),
                        negativeButtonText = app.getString(R.string.cancel),
                        neutralButtonText = app.getString(R.string.copy),
                        onNeutralButtonClick = {
                            app.copyToClipBoard(externalCrashesDirectory.absolutePath)
                        },
                    )
                }
                emitSideEffect(
                    SettingsSideEffect.OpenExternalDirectory(externalCrashesDirectory.absolutePath)
                )
            }
            SettingsAction.OpenDocumentation -> {
                emitSideEffect(
                    SettingsSideEffect.OpenBrowser("https://crustack.github.io/NotallyX")
                )
            }
            SettingsAction.OpenSourceCode -> {
                emitSideEffect(
                    SettingsSideEffect.OpenBrowser("https://github.com/Crustack/NotallyX")
                )
            }
        }
    }

    private fun handleSelectBiometricLock(newValue: BiometricLock) {
        if (newValue == preferences.biometricLock.value) return
        val status = AuthenticatorProvider.instance.canAuthenticateWithBiometrics(app)
        if (newValue == BiometricLock.ENABLED) {
            when (status) {
                BiometricManager.BIOMETRIC_SUCCESS -> showEnableBiometricLock()
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    emitSideEffect(SettingsSideEffect.ShowToast(R.string.biometrics_no_support))
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> showBiometricsNotSetupDialog()
            }
        } else {
            when (status) {
                BiometricManager.BIOMETRIC_SUCCESS -> showDisableBiometricLock()
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                    emitSideEffect(SettingsSideEffect.ShowToast(R.string.biometrics_no_support))
                    savePreference(preferences.biometricLock, BiometricLock.DISABLED)
                }
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    showBiometricsNotSetupDialog()
                    savePreference(preferences.biometricLock, BiometricLock.DISABLED)
                }
            }
        }
    }

    private fun showEnableBiometricLock() {
        _activeDialog.update {
            SettingsDialogState.Confirm(
                message = app.getString(R.string.biometric_backup_advice),
                positiveButtonText = app.getString(R.string.export),
                negativeButtonText = app.getString(R.string.cancel),
                neutralButtonText = app.getString(R.string.continue_),
                onNeutralButtonClick = { showEncryptDatabaseDialog() },
                onConfirm = {
                    biometricFlowState = BiometricFlowState.ENABLE_PENDING_EXPORT
                    _activeDialog.update { null }
                    emitSideEffect(SettingsSideEffect.LaunchExportBackup(buildBackupTitle()))
                },
            )
        }
    }

    private fun showDisableBiometricLock() {
        _activeDialog.update {
            SettingsDialogState.Confirm(
                message = app.getString(R.string.biometric_backup_advice),
                positiveButtonText = app.getString(R.string.export),
                negativeButtonText = app.getString(R.string.cancel),
                neutralButtonText = app.getString(R.string.continue_),
                onNeutralButtonClick = {
                    _activeDialog.update { null }
                    executeDisableBiometrics()
                },
                onConfirm = {
                    biometricFlowState = BiometricFlowState.DISABLE_PENDING_EXPORT
                    _activeDialog.update { null }
                    emitSideEffect(SettingsSideEffect.LaunchExportBackup(buildBackupTitle()))
                },
            )
        }
    }

    private fun showEncryptDatabaseDialog() {
        _activeDialog.update {
            SettingsDialogState.EncryptDatabase(
                onContinue = { shouldEncrypt ->
                    if (shouldEncrypt) {
                        emitSideEffect(SettingsSideEffect.LaunchBiometricPrompt(isEncrypt = true))
                    } else {
                        savePreference(preferences.biometricLockEncryptsDb, false)
                        savePreference(preferences.biometricLock, BiometricLock.ENABLED)
                        emitSideEffect(
                            SettingsSideEffect.ShowToast(R.string.biometrics_setup_success)
                        )
                        _activeDialog.update { null }
                    }
                }
            )
        }
    }

    private fun executeDisableBiometrics() {
        if (preferences.biometricLockEncryptsDb.value) {
            emitSideEffect(SettingsSideEffect.LaunchBiometricPrompt(isEncrypt = false))
        } else {
            savePreference(preferences.biometricLock, BiometricLock.DISABLED)
            savePreference(preferences.biometricLockEncryptsDb, false)
            emitSideEffect(SettingsSideEffect.ShowToast(R.string.biometrics_disable_success))
        }
    }

    private fun showBiometricsNotSetupDialog() {
        _activeDialog.update {
            SettingsDialogState.Confirm(
                message = app.getString(R.string.biometrics_not_setup),
                positiveButtonText = app.getString(R.string.tap_to_set_up),
                negativeButtonText = app.getString(R.string.cancel),
                onConfirm = {
                    _activeDialog.update { null }
                    emitSideEffect(SettingsSideEffect.LaunchBiometricEnroll)
                },
            )
        }
    }

    private fun displayChooseBackupFolderDialog() {
        _activeDialog.update {
            SettingsDialogState.Confirm(
                message = app.getString(R.string.auto_backups_folder_hint),
                positiveButtonText = app.getString(R.string.choose_folder),
                negativeButtonText = app.getString(R.string.cancel),
                neutralButtonText = app.getString(R.string.documentation),
                onNeutralButtonClick = {
                    emitSideEffect(
                        SettingsSideEffect.OpenBrowser(
                            "https://crustack.github.io/NotallyX/docs/features/backups#using-cloud-storagewebdav"
                        )
                    )
                },
                onConfirm = {
                    _activeDialog.update { null }
                    emitSideEffect(SettingsSideEffect.LaunchChooseBackupFolder())
                },
            )
        }
    }

    private fun showImportOtherDialog() {
        val notallyItems =
            listOf(
                object : Display {
                    override fun getTextId() = R.string.notally

                    override fun getIconId() = R.drawable.icon_notally
                },
                object : Display {
                    override fun getTextId() = R.string.notallyx_raw_database

                    override fun getIconId() = R.drawable.database
                },
            )
        val allSources = notallyItems + ImportSource.entries.toList()
        val iconListItems = allSources.map { item ->
            IconListItem(
                iconResId = item.getIconId(),
                text = app.getString(item.getTextId()),
            )
        }
        _activeDialog.update {
            SettingsDialogState.IconList(
                title = app.getString(R.string.choose_other_app),
                items = iconListItems,
                onItemClick = { onAction(SettingsAction.SelectImportOtherSource(it)) },
            )
        }
    }

    private fun handleImportSourceSelection(which: Int) {
        val notallyItemCount = 2
        when (which) {
            0 -> {
                _activeDialog.update {
                    SettingsDialogState.Confirm(
                        message =
                            app.getString(
                                R.string.import_from_notally,
                                app.getString(R.string.import_backup),
                            ),
                        positiveButtonText = app.getString(android.R.string.ok),
                        onConfirm = { _activeDialog.update { null } },
                    )
                }
                return
            }
            1 -> {
                _activeDialog.update { null }
                emitSideEffect(SettingsSideEffect.LaunchImportRawDatabase)
                return
            }
        }

        val source = ImportSource.entries[which - notallyItemCount]
        selectedImportSource = source
        val docUrl = source.documentationUrl

        _activeDialog.update {
            SettingsDialogState.Confirm(
                message = app.getString(source.helpTextResId),
                positiveButtonText = app.getString(R.string.import_action),
                negativeButtonText = app.getString(R.string.cancel),
                neutralButtonText = docUrl?.let { app.getString(R.string.help) },
                onNeutralButtonClick =
                    docUrl?.let { url -> { emitSideEffect(SettingsSideEffect.OpenBrowser(url)) } },
                onConfirm = {
                    when (source.mimeType) {
                        FOLDER_OR_FILE_MIMETYPE -> {
                            _activeDialog.update {
                                SettingsDialogState.ListSelection(
                                    title = app.getString(source.displayNameResId),
                                    items =
                                        listOf(
                                            app.getString(R.string.folder),
                                            app.getString(R.string.single_file),
                                        ),
                                    onItemClick = { choice ->
                                        _activeDialog.update { null }
                                        when (choice) {
                                            0 ->
                                                emitSideEffect(
                                                    SettingsSideEffect.LaunchImportOther(
                                                        arrayOf(),
                                                        isTree = true,
                                                    )
                                                )
                                            1 ->
                                                emitSideEffect(
                                                    SettingsSideEffect.LaunchImportOther(
                                                        arrayOf("text/*") +
                                                            APPLICATION_TEXT_MIME_TYPES,
                                                        isTree = false,
                                                    )
                                                )
                                        }
                                    },
                                )
                            }
                        }
                        else -> {
                            _activeDialog.update { null }
                            emitSideEffect(
                                SettingsSideEffect.LaunchImportOther(
                                    arrayOf(source.mimeType),
                                    isTree = false,
                                )
                            )
                        }
                    }
                },
            )
        }
    }

    fun updateAutoRemoveDeletedNotes(days: Int) {
        savePreference(preferences.autoRemoveDeletedNotesAfterDays, days)
        val workManager = WorkManager.getInstance(app)
        if (days > 0) {
            workManager.scheduleAutoRemoveOldDeletedNotes(app)
        } else {
            workManager.cancelAutoRemoveOldDeletedNotes()
        }
    }

    fun <T> savePreference(preference: BasePreference<T>, value: T) {
        viewModelScope.launch(Dispatchers.IO) { preference.save(value) }
    }

    fun disableBackups() {
        val value = preferences.backupsFolder.value
        if (!preferences.isDefaultOrEmptyBackupFolder(value)) {
            clearPersistedUriPermissions(value)
        }
        savePreference(preferences.backupsFolder, EMPTY_PATH)
        savePreference(
            preferences.periodicBackups,
            preferences.periodicBackups.value.copy(periodInDays = 0),
        )
    }

    fun setupBackupsFolder(uri: Uri) {
        val oldBackupsFolder = preferences.backupsFolder.value
        val newBackupsFolder = uri.toString()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            app.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            app.log(TAG, throwable = e)
        }
        if (newBackupsFolder != oldBackupsFolder) {
            if (!preferences.isDefaultOrEmptyBackupFolder(oldBackupsFolder)) {
                clearPersistedUriPermissions(oldBackupsFolder)
            }
            savePreference(preferences.backupsFolder, newBackupsFolder)
        }
        showRefreshBackupsFolderAfterThemeChange = false
        previousBackupsFolder = null
    }

    private fun clearPersistedUriPermissions(folderPath: String) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val folderUri =
            try {
                folderPath.toUri()
            } catch (_: Exception) {
                null
            }
        app.contentResolver.persistedUriPermissions.forEach { permission ->
            val uriPath = permission.uri.path
            if (
                (permission.uri == folderUri) ||
                    (permission.uri.toString() == folderPath) ||
                    (uriPath != null && folderUri?.path?.let { uriPath.contains(it) } == true)
            ) {
                app.contentResolver.releasePersistableUriPermission(permission.uri, flags)
            }
        }
    }

    fun exportBackup(uri: Uri, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            val exportedNotesAndAttachments =
                withContext(Dispatchers.IO) {
                    app.log(TAG, msg = "Exporting backup to '$uri'...")
                    return@withContext app.exportAsZip(
                            uri,
                            password = preferences.backupPassword.value,
                            backupProgress = progress,
                        )
                        .also { app.log(TAG, msg = "Finished exporting backup to '$uri'") }
                }

            emitSideEffect(
                SettingsSideEffect.ShowToastString(app.exportedText(exportedNotesAndAttachments))
            )
            onComplete?.invoke()

            when (biometricFlowState) {
                BiometricFlowState.ENABLE_PENDING_EXPORT -> {
                    biometricFlowState = BiometricFlowState.IDLE
                    showEncryptDatabaseDialog()
                }
                BiometricFlowState.DISABLE_PENDING_EXPORT -> {
                    biometricFlowState = BiometricFlowState.IDLE
                    executeDisableBiometrics()
                }
                BiometricFlowState.IDLE -> {}
            }
        }
    }

    fun importRawDatabase(uri: Uri, checkDuplicates: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            emitSideEffect(
                SettingsSideEffect.ShowToastString(
                    "${app.getString(R.string.invalid_backup)}: ${throwable.message}"
                )
            )
        }

        viewModelScope.launch(exceptionHandler) {
            val importResult =
                withContext(Dispatchers.IO) {
                    app.importRawDatabase(uri, checkDuplicates, importProgress)
                }
            emitSideEffect(SettingsSideEffect.ShowToastString(app.toMessage(importResult)))
        }
    }

    fun importZipBackup(uri: Uri, password: String, checkDuplicates: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            emitSideEffect(
                SettingsSideEffect.ShowToastString(
                    "${app.getString(R.string.invalid_backup)}: ${throwable.message}"
                )
            )
        }

        val backupDir = app.getBackupDir()
        viewModelScope.launch(exceptionHandler) {
            app.importZip(uri, backupDir, password, checkDuplicates, importProgress)
        }
    }

    fun importXmlBackup(uri: Uri) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            emitSideEffect(
                SettingsSideEffect.ShowToastString(
                    "${app.getString(R.string.invalid_backup)}: ${throwable.message}"
                )
            )
        }

        viewModelScope.launch(exceptionHandler) {
            val result =
                withContext(Dispatchers.IO) {
                    val stream =
                        requireNotNull(
                            app.contentResolver.openInputStream(uri),
                            { "InputStream for '$uri' is null" },
                        )
                    val (baseNotes, labels) = stream.readAsBackup()
                    commonDao.importBackup(baseNotes, labels, 0, false)
                }
            emitSideEffect(SettingsSideEffect.ShowToastString(app.toMessage(result)))
        }
    }

    fun importFromOtherApp(uri: Uri, importSource: ImportSource) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            if (throwable is ImportException) {
                emitSideEffect(SettingsSideEffect.ShowToast(throwable.textResId))
            } else {
                emitSideEffect(
                    SettingsSideEffect.ShowToastString(
                        "${app.getString(R.string.invalid_backup)}: ${throwable.message}"
                    )
                )
            }
        }

        viewModelScope.launch(exceptionHandler) {
            val database = NotallyDatabase.getDatabase(app).value
            val result =
                withContext(Dispatchers.IO) {
                    NotesImporter(app, database!!).import(uri, importSource, importProgress)
                }
            emitSideEffect(SettingsSideEffect.ShowToastString(app.toMessage(result)))
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            val (ids, noteReminders) =
                withContext(Dispatchers.IO) {
                    Pair(baseNoteDao.getAllIds().toLongArray(), baseNoteDao.getAllReminders())
                }
            noteReminders.forEach { app.cancelPinAndReminders(it.id, it.reminders) }
            val deletedNotes = deleteBaseNotes(ids)
            app.deleteAttachments(deletedNotes)
            withContext(Dispatchers.IO) { labelDao.deleteAll() }
            savePreference(preferences.startView, START_VIEW_DEFAULT)
            emitSideEffect(SettingsSideEffect.ShowToast(R.string.cleared_data))
        }
    }

    private suspend fun deleteBaseNotes(ids: LongArray): Collection<BaseNote> {
        val notes = withContext(Dispatchers.IO) { baseNoteDao.getByIds(ids) }
        app.cancelPinAndReminders(notes)
        return withContext(Dispatchers.IO) {
            baseNoteDao.delete(ids)
            return@withContext notes
        }
    }

    fun resetPreferences(callback: ((restartRequired: Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val backupsFolder = preferences.backupsFolder.value
            val isThemeDefault = preferences.theme.value == Theme.FOLLOW_SYSTEM
            if (preferences.dataInPublicFolder.value) {
                DatabaseTransitionActivity.disableDataInPublic(app, preferences)
            }
            if (preferences.isLockEnabled) {
                DatabaseTransitionActivity.disableBiometricsEncryption(app, preferences)
            }
            preferences.reset()
            if (!preferences.isDefaultOrEmptyBackupFolder(backupsFolder)) {
                clearPersistedUriPermissions(backupsFolder)
            }
            callback?.invoke(!isThemeDefault)
            emitSideEffect(SettingsSideEffect.ShowToast(R.string.reset_settings_success))
            app.restartApplication(NotallyNavKey.Settings)
        }
    }

    fun exportSettings(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            if (app.exportPreferences(preferences, uri)) {
                emitSideEffect(SettingsSideEffect.ShowToast(R.string.export_settings_success))
            } else {
                emitSideEffect(SettingsSideEffect.ShowToast(R.string.export_settings_failure))
            }
        }
    }

    fun importSettings(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldBackupsFolder = preferences.backupsFolder.value
            val themeBefore = preferences.theme.value
            val useDynamicColorsBefore = preferences.useDynamicColors.value
            val oldStartView = preferences.startView.value

            val success = preferences.import(app, uri)

            finishImportPreferences(
                oldBackupsFolder,
                themeBefore,
                useDynamicColorsBefore,
                oldStartView,
                askForUriPermissions = { folderUri ->
                    emitSideEffect(SettingsSideEffect.LaunchChooseBackupFolder(folderUri))
                },
            ) {
                if (success) {
                    emitSideEffect(SettingsSideEffect.ShowToast(R.string.import_settings_success))
                } else {
                    emitSideEffect(SettingsSideEffect.ShowToast(R.string.import_settings_failure))
                }
            }
        }
    }

    private fun finishImportPreferences(
        oldBackupsFolder: String,
        themeBefore: Theme,
        useDynamicColorsBefore: Boolean,
        oldStartView: String,
        askForUriPermissions: (uri: Uri) -> Unit,
        callback: () -> Unit,
    ) {
        val backupFolder = preferences.backupsFolder.getFreshValue()
        val hasUseDynamicColorsChange =
            useDynamicColorsBefore != preferences.useDynamicColors.getFreshValue()
        if (oldBackupsFolder != backupFolder) {
            showRefreshBackupsFolderAfterThemeChange = true
            previousBackupsFolder = oldBackupsFolder
            if (themeBefore == preferences.theme.getFreshValue() && !hasUseDynamicColorsChange) {
                refreshBackupsFolder(
                    backupFolder,
                    oldBackupsFolder,
                    askForUriPermissions,
                )
            }
        } else {
            showRefreshBackupsFolderAfterThemeChange = false
            previousBackupsFolder = null
        }
        val startView = preferences.startView.getFreshValue()
        if (oldStartView != startView) {
            refreshStartView(startView, oldStartView)
        }
        preferences.theme.refresh()
        callback()
        if (showRefreshBackupsFolderAfterThemeChange) {
            app.restartApplication(NotallyNavKey.Settings, EXTRA_SHOW_IMPORT_BACKUPS_FOLDER to true)
        }
    }

    fun refreshBackupsFolder(
        backupFolder: String = preferences.backupsFolder.value,
        oldBackupsFolder: String? = previousBackupsFolder,
        askForUriPermissions: (backupFolderUri: Uri) -> Unit,
    ) {
        if (oldBackupsFolder != null) {
            previousBackupsFolder = oldBackupsFolder
        }
        if (preferences.isDefaultOrEmptyBackupFolder(backupFolder)) {
            return
        }
        try {
            val backupFolderUri = backupFolder.toUri()
            askForUriPermissions(backupFolderUri)
        } catch (_: Exception) {
            cancelFolderSelection(oldBackupsFolder)
        }
    }

    fun cancelFolderSelection(oldBackupsFolder: String? = previousBackupsFolder) {
        if (!showRefreshBackupsFolderAfterThemeChange && previousBackupsFolder == null) {
            return
        }
        showRefreshBackupsFolderAfterThemeChange = false
        val previousFolder = oldBackupsFolder ?: previousBackupsFolder
        previousBackupsFolder = null
        if (previousFolder != null && hasPersistedUriPermission(previousFolder)) {
            savePreference(preferences.backupsFolder, previousFolder)
        } else {
            disableBackups()
        }
    }

    fun hasPersistedUriPermission(folderPath: String): Boolean {
        if (preferences.isDefaultOrEmptyBackupFolder(folderPath)) {
            return true
        }
        val uri =
            try {
                folderPath.toUri()
            } catch (_: Exception) {
                return false
            }
        return app.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission &&
                permission.isWritePermission &&
                (permission.uri == uri ||
                    permission.uri.toString() == folderPath ||
                    (permission.uri.path != null &&
                        uri.path?.let { permission.uri.path?.contains(it) } == true))
        }
    }

    private fun refreshStartView(startView: String, oldStartView: String) {
        if (startView in setOf(START_VIEW_DEFAULT, START_VIEW_UNLABELED)) {
            savePreference(preferences.startView, startView)
        } else {
            viewModelScope.launch {
                val startViewLabelExists =
                    withContext(Dispatchers.IO) { labelDao.exists(startView) }
                savePreference(
                    preferences.startView,
                    if (startViewLabelExists) startView else oldStartView,
                )
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
        const val EXTRA_SHOW_IMPORT_BACKUPS_FOLDER =
            "notallyx.intent.extra.SHOW_IMPORT_BACKUPS_FOLDER"

        fun toPreferencesUiModel(
            userPrefs: UserPreferences,
            securePrefs: SecurePreferences,
            appVersion: String,
        ): PreferencesUiModel {
            return PreferencesUiModel(
                notesView = userPrefs.notesView,
                theme = userPrefs.theme,
                useDynamicColors = userPrefs.useDynamicColors,
                dateFormatOverview = userPrefs.dateFormatOverview,
                timeFormatOverview = userPrefs.timeFormatOverview,
                dateFormatNoteView = userPrefs.dateFormatNoteView,
                timeFormatNoteView = userPrefs.timeFormatNoteView,
                textSizeNoteEditor = userPrefs.textSizeNoteEditor,
                textSizeOverview = userPrefs.textSizeOverview,
                alwaysShowSearchBar = userPrefs.alwaysShowSearchBar,
                notesSort = userPrefs.notesSorting,
                listItemSorting = userPrefs.listItemSorting,
                startView = userPrefs.startView.ifEmpty { START_VIEW_DEFAULT },
                defaultListNoteViewMode = userPrefs.defaultListNoteViewMode,
                autoSaveAfterIdleTime = userPrefs.autoSaveAfterIdleTime,
                autoRemoveDeletedNotesAfterDays = userPrefs.autoRemoveDeletedNotesAfterDays,
                maxTitle = userPrefs.maxTitle,
                maxItems = userPrefs.maxItems,
                maxLines = userPrefs.maxLines,
                maxLabels = userPrefs.maxLabels,
                labelTagsHiddenInOverview = userPrefs.labelTagsHiddenInOverview,
                imagesHiddenInOverview = userPrefs.imagesHiddenInOverview,
                backupsFolder = userPrefs.backupsFolder.ifEmpty { EMPTY_PATH },
                backupOnSave = userPrefs.backupOnSave,
                periodicBackups = userPrefs.periodicBackups,
                periodicBackupLastExecution = userPrefs.periodicBackupLastExecution,
                biometricLock = userPrefs.biometricLock,
                biometricLockEncryptsDb = userPrefs.biometricLockEncryptsDb,
                backupPassword = securePrefs.backupPassword,
                secureFlag = userPrefs.secureFlag,
                dataInPublicFolder = userPrefs.dataInPublicFolder,
                appVersion = appVersion,
            )
        }
    }
}

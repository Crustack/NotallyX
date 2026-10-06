package com.philkes.notallyx.presentation.viewmodel.main.fragment

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.imports.ImportException
import com.philkes.notallyx.data.imports.ImportSource
import com.philkes.notallyx.data.imports.NotesImporter
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.presentation.activity.DatabaseTransitionActivity
import com.philkes.notallyx.presentation.activity.main.fragment.settings.SettingsFragment.Companion.EXTRA_SHOW_IMPORT_BACKUPS_FOLDER
import com.philkes.notallyx.presentation.exportedText
import com.philkes.notallyx.presentation.restartApplication
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.preference.BasePreference
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import com.philkes.notallyx.utils.backup.exportAsZip
import com.philkes.notallyx.utils.backup.importRawDatabase
import com.philkes.notallyx.utils.backup.importZip
import com.philkes.notallyx.utils.backup.readAsBackup
import com.philkes.notallyx.utils.cancelPinAndReminders
import com.philkes.notallyx.utils.deleteAttachments
import com.philkes.notallyx.utils.getBackupDir
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.toMessage
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
        app.contentResolver.persistedUriPermissions.forEach { permission ->
            val uriPath = permission.uri.path
            if (uriPath?.contains(folderPath) == true) {
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

            app.showToast(app.exportedText(exportedNotesAndAttachments))
            onComplete?.invoke()
        }
    }

    fun importRawDatabase(uri: Uri, checkDuplicates: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            app.showToast("${app.getString(R.string.invalid_backup)}: ${throwable.message}")
        }

        viewModelScope.launch(exceptionHandler) {
            val importResult =
                withContext(Dispatchers.IO) {
                    app.importRawDatabase(uri, checkDuplicates, importProgress)
                }
            app.showToast(app.toMessage(importResult))
        }
    }

    fun importZipBackup(uri: Uri, password: String, checkDuplicates: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            app.showToast("${app.getString(R.string.invalid_backup)}: ${throwable.message}")
        }

        val backupDir = app.getBackupDir()
        viewModelScope.launch(exceptionHandler) {
            app.importZip(uri, backupDir, password, checkDuplicates, importProgress)
        }
    }

    fun importXmlBackup(uri: Uri) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            app.showToast("${app.getString(R.string.invalid_backup)}: ${throwable.message}")
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
            app.showToast(app.toMessage(result))
        }
    }

    fun importFromOtherApp(uri: Uri, importSource: ImportSource) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            app.log(TAG, throwable = throwable)
            if (throwable is ImportException) {
                app.showToast(throwable.textResId)
            } else {
                app.showToast("${app.getString(R.string.invalid_backup)}: ${throwable.message}")
            }
        }

        viewModelScope.launch(exceptionHandler) {
            val database =
                withContext(Dispatchers.Main.immediate) { NotallyDatabase.getDatabase(app).value }
            val result =
                withContext(Dispatchers.IO) {
                    NotesImporter(app, database!!).import(uri, importSource, importProgress)
                }
            app.showToast(app.toMessage(result))
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
            app.showToast(R.string.cleared_data)
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

    suspend fun resetPreferences(callback: (restartRequired: Boolean) -> Unit) {
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
        callback(!isThemeDefault)
        app.restartApplication(R.id.Settings)
    }

    fun importPreferences(
        context: Context,
        uri: Uri,
        askForUriPermissions: (uri: Uri) -> Unit,
        onSuccess: () -> Unit,
        onFailure: () -> Unit,
    ) {
        val oldBackupsFolder = preferences.backupsFolder.value
        val themeBefore = preferences.theme.value
        val useDynamicColorsBefore = preferences.useDynamicColors.value
        val oldStartView = preferences.startView.value

        val success = preferences.import(context, uri)

        finishImportPreferences(
            oldBackupsFolder,
            themeBefore,
            useDynamicColorsBefore,
            oldStartView,
            context,
            askForUriPermissions,
        ) {
            if (success) {
                onSuccess()
            } else onFailure()
        }
    }

    private fun finishImportPreferences(
        oldBackupsFolder: String,
        themeBefore: Theme,
        useDynamicColorsBefore: Boolean,
        oldStartView: String,
        context: Context,
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
                    context,
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
            app.restartApplication(R.id.Settings, EXTRA_SHOW_IMPORT_BACKUPS_FOLDER to true)
        }
    }

    fun refreshBackupsFolder(
        context: Context,
        backupFolder: String = preferences.backupsFolder.value,
        oldBackupsFolder: String? = previousBackupsFolder,
        askForUriPermissions: (uri: Uri) -> Unit,
    ) {
        if (oldBackupsFolder != null) {
            previousBackupsFolder = oldBackupsFolder
        }
        if (preferences.isDefaultOrEmptyBackupFolder(backupFolder)) {
            return
        }
        try {
            val backupFolderUri = backupFolder.toUri()
            var isPositiveButtonClicked = false
            MaterialAlertDialogBuilder(context)
                .setMessage(R.string.auto_backups_folder_rechoose)
                .setCancelButton { _, _ -> cancelFolderSelection(oldBackupsFolder) }
                .setOnDismissListener {
                    if (!isPositiveButtonClicked) {
                        cancelFolderSelection(oldBackupsFolder)
                    }
                }
                .setPositiveButton(R.string.choose_folder) { _, _ ->
                    isPositiveButtonClicked = true
                    askForUriPermissions(backupFolderUri)
                }
                .show()
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
                    permission.uri.path?.contains(folderPath) == true)
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
    }
}

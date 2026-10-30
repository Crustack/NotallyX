package com.philkes.notallyx.feature.settings

import android.net.Uri
import androidx.annotation.StringRes
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.dialog.IconListItem
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.feature.migration.backup.DatabaseAction
import javax.crypto.Cipher

data class PreferencesUiModel(
    val notesView: NotesView = NotesView.LIST,
    val theme: Theme = Theme.FOLLOW_SYSTEM,
    val useDynamicColors: Boolean = false,
    val dateFormatOverview: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormatOverview: TimeFormat = TimeFormat.NONE,
    val dateFormatNoteView: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormatNoteView: TimeFormat = TimeFormat.TWENTY_FOUR_H,
    val textSizeNoteEditor: Float = 16f,
    val textSizeOverview: Float = 16f,
    val alwaysShowSearchBar: Boolean = false,
    val notesSort: NotesSort = NotesSort(),
    val listItemSorting: ListItemSort = ListItemSort.AUTO_SORT_BY_CHECKED,
    val startView: String = START_VIEW_DEFAULT,
    val defaultListNoteViewMode: DefaultListNoteViewMode = DefaultListNoteViewMode.LAST_USED,
    val autoSaveAfterIdleTime: Int = 5,
    val autoRemoveDeletedNotesAfterDays: Int = 0,
    val maxTitle: Int = 1,
    val maxItems: Int = 4,
    val maxLines: Int = 8,
    val maxLabels: Int = 5,
    val labelTagsHiddenInOverview: Boolean = false,
    val imagesHiddenInOverview: Boolean = false,
    val backupsFolder: String = EMPTY_PATH,
    val backupOnSave: Boolean = false,
    val periodicBackups: PeriodicBackup = PeriodicBackup(0, 0),
    val periodicBackupLastExecution: Long = -1L,
    val biometricLock: BiometricLock = BiometricLock.DISABLED,
    val biometricLockEncryptsDb: Boolean = false,
    val backupPassword: String = PASSWORD_EMPTY,
    val secureFlag: Boolean = false,
    val dataInPublicFolder: Boolean = false,
    val appVersion: String = "",
)

sealed interface SettingsDialogState {
    data class SingleSelection<T>(
        val title: String,
        val items: List<SelectionItem<T>>,
        val selectedItem: T,
        val onConfirm: (T) -> Unit,
    ) : SettingsDialogState

    data class Theme(
        val initialTheme: com.philkes.notallyx.core.preference.Theme,
        val initialUseDynamicColors: Boolean,
        val onConfirm:
            (
                newTheme: com.philkes.notallyx.core.preference.Theme,
                useDynamicColors: Boolean,
            ) -> Unit,
    ) : SettingsDialogState

    data class DateTimeFormat(
        val initialDate: DateFormat,
        val initialTime: TimeFormat,
        val onConfirm: (DateFormat, TimeFormat) -> Unit,
    ) : SettingsDialogState

    data class NotesSort(
        val currentSort: com.philkes.notallyx.core.preference.NotesSort,
        val onConfirm: (com.philkes.notallyx.core.preference.NotesSort) -> Unit,
    ) : SettingsDialogState

    data class BooleanPref(
        val title: String,
        val message: String? = null,
        val initialValue: Boolean,
        val onConfirm: (Boolean) -> Unit,
    ) : SettingsDialogState

    data class BackupPassword(
        val password: String,
        val onConfirm: (String) -> Unit,
    ) : SettingsDialogState

    data class ImportNotesPassword(
        val initialPassword: String,
        val onConfirm: (password: String, checkDuplicates: Boolean) -> Unit,
    ) : SettingsDialogState

    data class IconList(
        val title: String,
        val items: List<IconListItem>,
        val onItemClick: (Int) -> Unit,
    ) : SettingsDialogState

    data class ListSelection(
        val title: String,
        val items: List<String>,
        val onItemClick: (Int) -> Unit,
    ) : SettingsDialogState

    data class Confirm(
        val title: String? = null,
        val message: String,
        val positiveButtonText: String = "",
        val negativeButtonText: String = "",
        val neutralButtonText: String? = null,
        val onNeutralButtonClick: (() -> Unit)? = null,
        val onConfirm: () -> Unit = {},
    ) : SettingsDialogState

    data class EncryptDatabase(val onContinue: (Boolean) -> Unit) : SettingsDialogState
}

data class SettingsUiState(
    val preferences: PreferencesUiModel = PreferencesUiModel(),
    val labels: List<Label> = emptyList(),
    val importProgress: Progress? = null,
    val activeDialog: SettingsDialogState? = null,
    val isLoading: Boolean = false,
) {
    // Convenience constructor for backwards compatibility in previews and tests
    constructor(
        preferences: PreferencesUiModel = PreferencesUiModel(),
        labels: List<Label> = emptyList(),
        importProgress: Progress? = null,
        activeDialog: SettingsDialogState? = null,
        isLoading: Boolean = false,
        biometricLock: BiometricLock = preferences.biometricLock,
        biometricLockEncryptsDb: Boolean = preferences.biometricLockEncryptsDb,
    ) : this(
        preferences =
            preferences.copy(
                biometricLock = biometricLock,
                biometricLockEncryptsDb = biometricLockEncryptsDb,
            ),
        labels = labels,
        importProgress = importProgress,
        activeDialog = activeDialog,
        isLoading = isLoading,
    )
}

sealed interface SettingsAction {
    data object DismissDialog : SettingsAction

    // Appearance
    data object ShowNotesViewDialog : SettingsAction

    data class SetNotesView(val notesView: NotesView) : SettingsAction

    data object ShowThemeDialog : SettingsAction

    data class SetTheme(
        val theme: Theme,
        val dynamicColors: Boolean,
    ) : SettingsAction

    data object ShowDateFormatOverviewDialog : SettingsAction

    data class SetDateFormatOverview(
        val dateFormat: DateFormat,
        val timeFormat: TimeFormat,
    ) : SettingsAction

    data object ShowDateFormatNoteViewDialog : SettingsAction

    data class SetDateFormatNoteView(
        val dateFormat: DateFormat,
        val timeFormat: TimeFormat,
    ) : SettingsAction

    data class SetTextSizeNoteEditor(val textSize: Float) : SettingsAction

    data class SetTextSizeOverview(val textSize: Float) : SettingsAction

    data object ShowAlwaysShowSearchBarDialog : SettingsAction

    data class SetAlwaysShowSearchBar(val enabled: Boolean) : SettingsAction

    // Behaviour
    data object ShowNotesSortDialog : SettingsAction

    data class SetNotesSort(val sort: NotesSort) : SettingsAction

    data object ShowListItemSortingDialog : SettingsAction

    data class SetListItemSorting(val sorting: ListItemSort) : SettingsAction

    data object ShowStartViewDialog : SettingsAction

    data class SetStartView(val startView: String) : SettingsAction

    data object ShowDefaultListNoteViewModeDialog : SettingsAction

    data class SetDefaultListNoteViewMode(val mode: DefaultListNoteViewMode) : SettingsAction

    data class SetAutoSaveAfterIdleTime(val seconds: Int) : SettingsAction

    data class UpdateAutoRemoveDays(val days: Int) : SettingsAction

    // Content Density
    data class SetMaxTitle(val maxTitle: Int) : SettingsAction

    data class SetMaxItems(val maxItems: Int) : SettingsAction

    data class SetMaxLines(val maxLines: Int) : SettingsAction

    data class SetMaxLabels(val maxLabels: Int) : SettingsAction

    data object ShowLabelTagsHiddenDialog : SettingsAction

    data class SetLabelTagsHidden(val hidden: Boolean) : SettingsAction

    data object ShowImagesHiddenDialog : SettingsAction

    data class SetImagesHidden(val hidden: Boolean) : SettingsAction

    // Backup & Restore
    data object RequestImportBackup : SettingsAction

    data class ImportBackupSelected(val uri: Uri, val mimeType: String? = null) : SettingsAction

    data class ImportZipConfirmed(
        val uri: Uri,
        val password: String,
        val checkDuplicates: Boolean,
    ) : SettingsAction

    data object ShowImportOtherDialog : SettingsAction

    data class SelectImportOtherSource(val which: Int) : SettingsAction

    data class ImportRawDatabaseSelected(val uri: Uri) : SettingsAction

    data class ImportOtherFileSelected(val uri: Uri) : SettingsAction

    data object RequestExportBackup : SettingsAction

    data class ExportBackupConfirmed(val uri: Uri) : SettingsAction

    data object ClickBackupsFolder : SettingsAction

    data class SetupBackupsFolder(val uri: Uri) : SettingsAction

    data object CancelBackupsFolderSelection : SettingsAction

    data object ShowBackupOnSaveDialog : SettingsAction

    data class SetBackupOnSave(val enabled: Boolean) : SettingsAction

    data object ShowPeriodicBackupsDialog : SettingsAction

    data class SetPeriodicBackupsEnabled(val enabled: Boolean) : SettingsAction

    data class SetPeriodicBackupsPeriodInDays(val days: Int) : SettingsAction

    data class SetPeriodicBackupsMax(val max: Int) : SettingsAction

    // Security
    data object ShowBiometricLockDialog : SettingsAction

    data class SelectBiometricLock(val lock: BiometricLock) : SettingsAction

    data class BiometricAuthSuccess(val cipher: Cipher, val isEncrypt: Boolean) : SettingsAction

    data object BiometricEnrollFinished : SettingsAction

    data object CancelBiometricFlow : SettingsAction

    data object ShowBackupPasswordDialog : SettingsAction

    data class SetBackupPassword(val password: String) : SettingsAction

    data object ShowSecureFlagDialog : SettingsAction

    data class SetSecureFlag(val enabled: Boolean) : SettingsAction

    // General
    data object RequestImportSettings : SettingsAction

    data class ImportSettingsConfirmed(val uri: Uri) : SettingsAction

    data object RequestExportSettings : SettingsAction

    data class ExportSettingsConfirmed(val uri: Uri) : SettingsAction

    data object RequestResetSettings : SettingsAction

    data object ResetSettingsConfirmed : SettingsAction

    data object ShowDataInPublicFolderDialog : SettingsAction

    data class SetDataInPublicFolder(val enabled: Boolean) : SettingsAction

    data object RequestClearData : SettingsAction

    data object ClearDataConfirmed : SettingsAction

    // About
    data object ShowSendFeedbackDialog : SettingsAction

    data class SelectFeedbackOption(val which: Int) : SettingsAction

    data object RateApp : SettingsAction

    data object Donate : SettingsAction

    data object ViewLogs : SettingsAction

    data object ShowCrashesDialog : SettingsAction

    data object OpenDocumentation : SettingsAction

    data object OpenSourceCode : SettingsAction
}

sealed interface SettingsSideEffect {
    data class ShowToast(@StringRes val messageRes: Int) : SettingsSideEffect

    data class ShowToastString(val message: String) : SettingsSideEffect

    data class OpenBrowser(val url: String) : SettingsSideEffect

    data class LaunchBiometricPrompt(val isEncrypt: Boolean) : SettingsSideEffect

    data class StartDatabaseTransition(val action: DatabaseAction) : SettingsSideEffect

    data class StartBiometricDatabaseTransition(
        val enableBiometricLock: Boolean,
        val encryptDatabase: Boolean,
        val cipher: Cipher,
    ) : SettingsSideEffect

    data class LaunchExportBackup(val title: String) : SettingsSideEffect

    data object LaunchImportBackup : SettingsSideEffect

    data object LaunchImportRawDatabase : SettingsSideEffect

    data class LaunchImportOther(
        val mimeTypes: Array<String>,
        val isTree: Boolean,
    ) : SettingsSideEffect {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LaunchImportOther

            if (!mimeTypes.contentEquals(other.mimeTypes)) return false
            if (isTree != other.isTree) return false

            return true
        }

        override fun hashCode(): Int {
            var result = mimeTypes.contentHashCode()
            result = 31 * result + isTree.hashCode()
            return result
        }
    }

    data class LaunchChooseBackupFolder(val initialUri: Uri? = null) : SettingsSideEffect

    data class RequestNotificationPermission(val permission: String) : SettingsSideEffect

    data object LaunchImportSettings : SettingsSideEffect

    data class LaunchExportSettings(val title: String) : SettingsSideEffect

    data object LaunchBiometricEnroll : SettingsSideEffect

    data class SendFeedbackEmail(val logUri: Uri?) : SettingsSideEffect

    data class ReportBug(val logs: String?) : SettingsSideEffect

    data object ViewLogs : SettingsSideEffect

    data class OpenExternalDirectory(val path: String) : SettingsSideEffect

    data class SetWindowSecure(val enabled: Boolean) : SettingsSideEffect
}

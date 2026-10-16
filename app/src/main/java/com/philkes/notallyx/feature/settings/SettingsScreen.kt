package com.philkes.notallyx.feature.settings

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getDocumentFolder
import com.philkes.notallyx.core.common.extensions.toReadablePath
import com.philkes.notallyx.core.common.result.ImportProgress
import com.philkes.notallyx.core.common.result.ImportStage
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.MAX_ITEMS_MAX
import com.philkes.notallyx.core.preference.MAX_LABELS_IN_NAVIGATION_MAX
import com.philkes.notallyx.core.preference.MAX_TITLE_LINES_MAX
import com.philkes.notallyx.core.preference.MAX_TITLE_LINES_MIN
import com.philkes.notallyx.core.preference.NUMBER_PREF_DISABLED
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_MAX_MAX
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_MAX_MIN
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MAX
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MIN
import com.philkes.notallyx.core.preference.TEXT_SIZE_MAX
import com.philkes.notallyx.core.preference.TEXT_SIZE_MIN
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.SectionHeaderItem
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.IconListDialog
import com.philkes.notallyx.core.ui.dialog.IconListItem
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.dialog.SingleSelectionDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.extensions.getQuantityStringPlain
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.settings.dialogs.DateTimeFormatDialog
import com.philkes.notallyx.feature.settings.dialogs.EncryptDatabaseDialog
import com.philkes.notallyx.feature.settings.dialogs.ImportNotesDialog
import com.philkes.notallyx.feature.settings.dialogs.NotesSortDialog
import java.util.Date
import kotlin.math.roundToInt

internal sealed interface SettingsDialogState {
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

@Composable
internal fun Context.createFromPreferences(preferences: NotallyXPreferences): SettingsUiState {
    val appVersion = remember {
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }
    return with(preferences) {
        SettingsUiState(
            notesView = notesView.flow.collectAsStateWithLifecycle(notesView.value).value,
            theme = theme.flow.collectAsStateWithLifecycle(theme.value).value,
            useDynamicColors =
                useDynamicColors.flow.collectAsStateWithLifecycle(useDynamicColors.value).value,
            dateFormatOverview =
                dateFormatOverview.flow.collectAsStateWithLifecycle(dateFormatOverview.value).value,
            timeFormatOverview =
                timeFormatOverview.flow.collectAsStateWithLifecycle(timeFormatOverview.value).value,
            dateFormatNoteView =
                dateFormatNoteView.flow.collectAsStateWithLifecycle(dateFormatNoteView.value).value,
            timeFormatNoteView =
                timeFormatNoteView.flow.collectAsStateWithLifecycle(timeFormatNoteView.value).value,
            textSizeNoteEditor =
                textSizeNoteEditor.flow.collectAsStateWithLifecycle(textSizeNoteEditor.value).value,
            textSizeOverview =
                textSizeOverview.flow.collectAsStateWithLifecycle(textSizeOverview.value).value,
            alwaysShowSearchBar =
                alwaysShowSearchBar.flow
                    .collectAsStateWithLifecycle(alwaysShowSearchBar.value)
                    .value,
            notesSort = notesSorting.flow.collectAsStateWithLifecycle(notesSorting.value).value,
            listItemSorting =
                listItemSorting.flow.collectAsStateWithLifecycle(listItemSorting.value).value,
            startView = startView.flow.collectAsStateWithLifecycle(startView.value).value,
            defaultListNoteViewMode =
                defaultListNoteViewMode.flow
                    .collectAsStateWithLifecycle(defaultListNoteViewMode.value)
                    .value,
            autoSaveAfterIdleTime =
                autoSaveAfterIdleTime.flow
                    .collectAsStateWithLifecycle(autoSaveAfterIdleTime.value)
                    .value,
            autoRemoveDeletedNotesAfterDays =
                autoRemoveDeletedNotesAfterDays.flow
                    .collectAsStateWithLifecycle(autoRemoveDeletedNotesAfterDays.value)
                    .value,
            maxTitle = maxTitle.flow.collectAsStateWithLifecycle(maxTitle.value).value,
            maxItems = maxItems.flow.collectAsStateWithLifecycle(maxItems.value).value,
            maxLines = maxLines.flow.collectAsStateWithLifecycle(maxLines.value).value,
            maxLabels = maxLabels.flow.collectAsStateWithLifecycle(maxLabels.value).value,
            labelTagsHiddenInOverview =
                labelTagsHiddenInOverview.flow
                    .collectAsStateWithLifecycle(labelTagsHiddenInOverview.value)
                    .value,
            imagesHiddenInOverview =
                imagesHiddenInOverview.flow
                    .collectAsStateWithLifecycle(imagesHiddenInOverview.value)
                    .value,
            backupsFolder =
                backupsFolder.flow.collectAsStateWithLifecycle(backupsFolder.value).value,
            backupOnSave = backupOnSave.flow.collectAsStateWithLifecycle(backupOnSave.value).value,
            periodicBackups =
                periodicBackups.flow.collectAsStateWithLifecycle(periodicBackups.value).value,
            periodicBackupLastExecution =
                periodicBackupLastExecution.flow
                    .collectAsStateWithLifecycle(periodicBackupLastExecution.value)
                    .value,
            biometricLock =
                biometricLock.flow.collectAsStateWithLifecycle(biometricLock.value).value,
            biometricLockEncryptsDb =
                biometricLockEncryptsDb.flow
                    .collectAsStateWithLifecycle(biometricLockEncryptsDb.value)
                    .value,
            backupPassword =
                backupPassword.flow.collectAsStateWithLifecycle(backupPassword.value).value,
            secureFlag = secureFlag.flow.collectAsStateWithLifecycle(secureFlag.value).value,
            dataInPublicFolder =
                dataInPublicFolder.flow.collectAsStateWithLifecycle(dataInPublicFolder.value).value,
            appVersion = appVersion,
        )
    }
}

data class SettingsActions(
    val onDismissDialog: () -> Unit = {},
    val onNotesViewClick: () -> Unit = {},
    val onThemeClick: () -> Unit = {},
    val onDateFormatOverviewClick: () -> Unit = {},
    val onDateFormatNoteViewClick: () -> Unit = {},
    val onTextSizeNoteEditorChange: (Float) -> Unit = {},
    val onTextSizeOverviewChange: (Float) -> Unit = {},
    val onAlwaysShowSearchBarClick: () -> Unit = {},
    val onNotesSortOrderClick: () -> Unit = {},
    val onCheckedListItemSortingClick: () -> Unit = {},
    val onStartViewClick: () -> Unit = {},
    val onDefaultListNoteViewModeClick: () -> Unit = {},
    val onAutoSaveAfterIdleChange: (Int) -> Unit = {},
    val onAutoRemoveDeletedNotesChange: (Int) -> Unit = {},
    val onMaxTitleChange: (Int) -> Unit = {},
    val onMaxItemsChange: (Int) -> Unit = {},
    val onMaxLinesChange: (Int) -> Unit = {},
    val onMaxLabelsChange: (Int) -> Unit = {},
    val onLabelTagsHiddenClick: () -> Unit = {},
    val onImagesHiddenClick: () -> Unit = {},
    val onImportBackupClick: () -> Unit = {},
    val onImportOtherClick: () -> Unit = {},
    val onExportBackupClick: () -> Unit = {},
    val onBackupsFolderClick: () -> Unit = {},
    val onBackupOnSaveClick: () -> Unit = {},
    val onPeriodicBackupsClick: () -> Unit = {},
    val onPeriodicBackupsPeriodInDaysChange: (Int) -> Unit = {},
    val onPeriodicBackupsMaxChange: (Int) -> Unit = {},
    val onBiometricLockClick: () -> Unit = {},
    val onBackupPasswordClick: () -> Unit = {},
    val onSecureFlagClick: () -> Unit = {},
    val onImportSettingsClick: () -> Unit = {},
    val onExportSettingsClick: () -> Unit = {},
    val onResetSettingsClick: () -> Unit = {},
    val onDataInPublicFolderClick: () -> Unit = {},
    val onClearDataClick: () -> Unit = {},
    val onSendFeedbackClick: () -> Unit = {},
    val onRateClick: () -> Unit = {},
    val onDonateClick: () -> Unit = {},
    val onViewLogsClick: () -> Unit = {},
    val onViewCrashesClick: () -> Unit = {},
    val onDocumentationClick: () -> Unit = {},
    val onSourceCodeClick: () -> Unit = {},
)

private fun formatDateTime(
    context: Context,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
): String {
    val dateText = if (dateFormat == DateFormat.NONE) "" else dateFormat.getText(context)
    val timeText = if (timeFormat == TimeFormat.NONE) "" else timeFormat.getText(context)
    return when {
        dateFormat == DateFormat.NONE && timeFormat == TimeFormat.NONE ->
            dateFormat.getText(context)
        dateFormat == DateFormat.NONE -> timeText
        timeFormat == TimeFormat.NONE -> dateText
        else -> "$dateText $timeText"
    }
}

@Composable
private fun SettingsAppearanceSection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.appearance))
        PreferenceRow(
            title = stringResource(id = R.string.view),
            value = state.notesView.getText(context),
            testTag = "View",
            onClick = actions.onNotesViewClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.theme),
            value = state.theme.getText(context),
            testTag = "Theme",
            onClick = actions.onThemeClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.date_format_overview),
            value = formatDateTime(context, state.dateFormatOverview, state.timeFormatOverview),
            testTag = "DateFormatOverview",
            onClick = actions.onDateFormatOverviewClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.date_format_note_view),
            value = formatDateTime(context, state.dateFormatNoteView, state.timeFormatNoteView),
            testTag = "DateFormatNoteView",
            onClick = actions.onDateFormatNoteViewClick,
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.text_size_note_editor),
            value = state.textSizeNoteEditor,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            testTag = "TextSize",
            onValueChange = actions.onTextSizeNoteEditorChange,
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.text_size_overview),
            value = state.textSizeOverview,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            testTag = "TextSizeOverview",
            onValueChange = actions.onTextSizeOverviewChange,
        )
        PreferenceRow(
            title = stringResource(id = R.string.always_show_search_bar),
            value =
                if (state.alwaysShowSearchBar) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "ShowSearchInTopBar",
            onClick = actions.onAlwaysShowSearchBarClick,
        )
    }
}

@Composable
private fun SettingsBehaviourSection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val notesText = "${stringResource(R.string.notes)} (${stringResource(R.string.text_default)})"
    val unlabeledText = stringResource(R.string.unlabeled)
    val startViewValueText =
        when (state.startView) {
            START_VIEW_DEFAULT -> notesText
            START_VIEW_UNLABELED -> unlabeledText
            else -> state.startView
        }
    val offText = stringResource(id = R.string.off)

    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.behaviour))
        PreferenceRow(
            title = stringResource(id = R.string.notes_sorted_by),
            value = state.notesSort.getText(context),
            testTag = "NotesSortOrder",
            onClick = actions.onNotesSortOrderClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.list_item_auto_sort),
            value = state.listItemSorting.getText(context),
            testTag = "CheckedListItemSorting",
            onClick = actions.onCheckedListItemSortingClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.start_view),
            value = startViewValueText,
            testTag = "StartView",
            onClick = actions.onStartViewClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.default_list_note_view_mode),
            value = state.defaultListNoteViewMode.getText(context),
            testTag = "DefaultListNoteViewMode",
            onClick = actions.onDefaultListNoteViewModeClick,
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.auto_save_after_idle_time),
            value = state.autoSaveAfterIdleTime,
            valueRange = NUMBER_PREF_DISABLED..Integer.MAX_VALUE,
            labelFormatter = { v -> if (v == -1) offText else "${v}s" },
            testTag = "AutoSaveAfterIdle",
            onValueChange = actions.onAutoSaveAfterIdleChange,
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.auto_remove_deleted_notes),
            value = state.autoRemoveDeletedNotesAfterDays,
            valueRange = 0..Integer.MAX_VALUE,
            labelFormatter = { v ->
                if (v == 0) offText else "$v ${context.getQuantityStringPlain(R.plurals.days, v)}"
            },
            testTag = "AutoEmptyBin",
            onValueChange = actions.onAutoRemoveDeletedNotesChange,
        )
    }
}

@Composable
private fun SettingsContentDensitySection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.content_density))
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_lines_to_display_title),
            value = state.maxTitle.toFloat(),
            valueRange = MAX_TITLE_LINES_MIN.toFloat()..MAX_TITLE_LINES_MAX.toFloat(),
            testTag = "MaxTitle",
            onValueChange = { actions.onMaxTitleChange(it.roundToInt()) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_items_to_display),
            value = state.maxItems.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            testTag = "MaxItems",
            onValueChange = { actions.onMaxItemsChange(it.roundToInt()) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_lines_to_display),
            value = state.maxLines.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            testTag = "MaxLines",
            onValueChange = { actions.onMaxLinesChange(it.roundToInt()) },
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.max_labels_to_display),
            value = state.maxLabels,
            valueRange = 0..MAX_LABELS_IN_NAVIGATION_MAX,
            testTag = "MaxLabels",
            onValueChange = actions.onMaxLabelsChange,
        )
        PreferenceRow(
            title = stringResource(id = R.string.labels_hidden_in_overview_title),
            value =
                if (state.labelTagsHiddenInOverview) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "LabelsHiddenInOverview",
            onClick = actions.onLabelTagsHiddenClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.images_hidden_in_overview_title),
            value =
                if (state.imagesHiddenInOverview) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "ImagesHiddenInOverview",
            onClick = actions.onImagesHiddenClick,
        )
    }
}

@Composable
private fun SettingsBackupSection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val backupsFolderText =
        if (state.backupsFolder == EMPTY_PATH) {
            stringResource(id = R.string.tap_to_set_up)
        } else {
            val uri = Uri.parse(state.backupsFolder)
            val folder = context.getDocumentFolder(uri)
            if (folder != null && folder.exists()) {
                context.toReadablePath(uri)
            } else {
                stringResource(id = R.string.cant_find_folder)
            }
        }
    val periodicBackupsEnabled =
        state.periodicBackups.periodInDays > 0 && state.backupsFolder != EMPTY_PATH

    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.backup))
        PreferenceRow(
            title = stringResource(id = R.string.import_backup),
            testTag = "ImportBackup",
            onClick = actions.onImportBackupClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.import_other),
            testTag = "ImportOther",
            onClick = actions.onImportOtherClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.export_backup),
            testTag = "ExportBackup",
            onClick = actions.onExportBackupClick,
        )

        SectionHeaderItem(title = stringResource(id = R.string.auto_backup))
        PreferenceRow(
            title = stringResource(id = R.string.auto_backups_folder),
            value = backupsFolderText,
            testTag = "BackupsFolder",
            onClick = actions.onBackupsFolderClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.auto_backup_on_save),
            value =
                if (state.backupsFolder == EMPTY_PATH)
                    stringResource(id = R.string.auto_backups_folder_set)
                else if (state.backupOnSave) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            enabled = state.backupsFolder != EMPTY_PATH,
            testTag = "BackupOnSave",
            onClick = actions.onBackupOnSaveClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.backup_periodic),
            value =
                if (state.backupsFolder == EMPTY_PATH)
                    stringResource(id = R.string.auto_backups_folder_set)
                else if (periodicBackupsEnabled) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            enabled = state.backupsFolder != EMPTY_PATH,
            testTag = "PeriodicBackups",
            onClick = actions.onPeriodicBackupsClick,
        )
        if (state.periodicBackupLastExecution != -1L) {
            val lastBackupFormatted =
                Date(state.periodicBackupLastExecution)
                    .format(
                        state.dateFormatOverview,
                        state.timeFormatOverview,
                        ensureFullFormat = true,
                    )
            Text(
                text = "${stringResource(id = R.string.auto_backup_last)}: $lastBackupFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.testTag("PeriodicBackupLastExecution")
                        .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        StepperPreferenceRow(
            title = stringResource(id = R.string.backup_period_days),
            value = state.periodicBackups.periodInDays,
            valueRange = BACKUP_PERIOD_DAYS_MIN..BACKUP_PERIOD_DAYS_MAX,
            enabled = periodicBackupsEnabled,
            testTag = "PeriodicBackupsPeriodInDays",
            onValueChange = actions.onPeriodicBackupsPeriodInDaysChange,
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.max_backups),
            value = state.periodicBackups.maxBackups,
            valueRange = BACKUP_MAX_MIN..BACKUP_MAX_MAX,
            enabled = periodicBackupsEnabled,
            testTag = "PeriodicBackupsMax",
            onValueChange = actions.onPeriodicBackupsMaxChange,
        )
    }
}

@Composable
private fun SettingsSecuritySection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val biometricLockText =
        if (state.biometricLock == BiometricLock.ENABLED && state.biometricLockEncryptsDb) {
            stringResource(id = R.string.biometric_lock_enabled_with_encryption)
        } else {
            state.biometricLock.getText(context)
        }

    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.security))
        PreferenceRow(
            title = stringResource(id = R.string.biometric_lock),
            value = biometricLockText,
            testTag = "BiometricLock",
            onClick = actions.onBiometricLockClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.backup_password),
            value =
                if (state.backupPassword != PASSWORD_EMPTY) "••••••••"
                else stringResource(id = R.string.tap_to_set_up),
            testTag = "BackupPassword",
            onClick = actions.onBackupPasswordClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.disallow_screenshots),
            value =
                if (state.secureFlag) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "SecureFlag",
            onClick = actions.onSecureFlagClick,
        )
    }
}

@Composable
private fun SettingsGeneralSection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.settings))
        PreferenceRow(
            title = stringResource(id = R.string.import_settings),
            testTag = "ImportSettings",
            onClick = actions.onImportSettingsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.export_settings),
            testTag = "ExportSettings",
            onClick = actions.onExportSettingsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.reset_settings),
            testTag = "ResetSettings",
            onClick = actions.onResetSettingsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.data_in_public),
            value =
                if (state.dataInPublicFolder) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "DataInPublicFolder",
            onClick = actions.onDataInPublicFolderClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.clear_data),
            testTag = "ClearData",
            onClick = actions.onClearDataClick,
        )
    }
}

@Composable
private fun SettingsAboutSection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.about))
        PreferenceRow(
            title = stringResource(id = R.string.send_feedback),
            testTag = "SendFeedback",
            onClick = actions.onSendFeedbackClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.rate),
            testTag = "Rate",
            onClick = actions.onRateClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.donate),
            testTag = "Donate",
            onClick = actions.onDonateClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.view_logs),
            testTag = "ViewLogs",
            onClick = actions.onViewLogsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.view_crash_logs),
            testTag = "ViewCrashes",
            onClick = actions.onViewCrashesClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.documentation),
            testTag = "Documentation",
            onClick = actions.onDocumentationClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.source_code),
            testTag = "SourceCode",
            onClick = actions.onSourceCodeClick,
        )
        if (state.appVersion.isNotEmpty()) {
            Text(
                text = "v${state.appVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.testTag("VersionText").padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

/** Composable replacing fragment_settings.xml layout. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    dialogState: SettingsDialogState? = null,
    importProgress: Progress? = null,
    actions: SettingsActions = SettingsActions(),
) {
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .verticalScroll(scrollState)
    ) {
        SettingsAppearanceSection(state = state, actions = actions)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsBehaviourSection(state = state, actions = actions)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsContentDensitySection(state = state, actions = actions)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsBackupSection(state = state, actions = actions)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsSecuritySection(state = state, actions = actions)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsGeneralSection(state = state, actions = actions)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsAboutSection(state = state, actions = actions)
    }

    when (dialogState) {
        is SettingsDialogState.SingleSelection<*> -> {
            @Suppress("UNCHECKED_CAST")
            val s = dialogState as SettingsDialogState.SingleSelection<Any>
            SingleSelectionDialog(
                title = s.title,
                items = s.items,
                selectedItem = s.selectedItem,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = { selected ->
                    s.onConfirm(selected)
                    actions.onDismissDialog()
                },
            )
        }
        is SettingsDialogState.Theme -> {
            PreferenceEnumWithToggleDialog(
                title = stringResource(R.string.theme),
                items = Theme.entries.map { SelectionItem(it, it.getText(context)) },
                selectedItem = dialogState.initialTheme,
                toggleLabel = stringResource(R.string.theme_use_dynamic_colors),
                initialToggleValue = dialogState.initialUseDynamicColors,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = { newTheme, useDynamicColors ->
                    dialogState.onConfirm(newTheme, useDynamicColors)
                    actions.onDismissDialog()
                },
            )
        }
        is SettingsDialogState.DateTimeFormat -> {
            DateTimeFormatDialog(
                initialDateFormat = dialogState.initialDate,
                initialTimeFormat = dialogState.initialTime,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = { newDate, newTime ->
                    dialogState.onConfirm(newDate, newTime)
                    actions.onDismissDialog()
                },
            )
        }
        is SettingsDialogState.NotesSort -> {
            NotesSortDialog(
                currentSort = dialogState.currentSort,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = { newSort ->
                    dialogState.onConfirm(newSort)
                    actions.onDismissDialog()
                },
            )
        }
        is SettingsDialogState.BooleanPref -> {
            PreferenceBooleanDialog(
                title = dialogState.title,
                message = dialogState.message,
                initialValue = dialogState.initialValue,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = { newValue ->
                    dialogState.onConfirm(newValue)
                    actions.onDismissDialog()
                },
            )
        }
        is SettingsDialogState.BackupPassword -> {
            TextInputDialog(
                title = stringResource(R.string.backup_password),
                initialValue = dialogState.password,
                message = stringResource(R.string.backup_password_hint),
                isPassword = true,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = { updatedPassword ->
                    dialogState.onConfirm(updatedPassword)
                    actions.onDismissDialog()
                },
            )
        }
        is SettingsDialogState.ImportNotesPassword -> {
            ImportNotesDialog(
                title = stringResource(R.string.backup_password),
                initialText = dialogState.initialPassword,
                message = stringResource(R.string.import_backup_password_hint),
                initialCheckDuplicates = true,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = { usedPassword, checkDuplicates ->
                    dialogState.onConfirm(usedPassword, checkDuplicates)
                    actions.onDismissDialog()
                },
            )
        }
        is SettingsDialogState.IconList -> {
            IconListDialog(
                title = dialogState.title,
                items = dialogState.items,
                onDismissRequest = actions.onDismissDialog,
                onItemClick = { which ->
                    actions.onDismissDialog()
                    dialogState.onItemClick(which)
                },
            )
        }
        is SettingsDialogState.ListSelection -> {
            ListDialog(
                title = dialogState.title,
                items = dialogState.items,
                onDismissRequest = actions.onDismissDialog,
                onItemClick = { which ->
                    actions.onDismissDialog()
                    dialogState.onItemClick(which)
                },
            )
        }
        is SettingsDialogState.Confirm -> {
            ConfirmDialog(
                title = dialogState.title,
                message = dialogState.message,
                positiveButtonText =
                    dialogState.positiveButtonText.ifEmpty { stringResource(android.R.string.ok) },
                negativeButtonText =
                    dialogState.negativeButtonText.ifEmpty { stringResource(R.string.cancel) },
                neutralButtonText = dialogState.neutralButtonText,
                onNeutralButtonClick = dialogState.onNeutralButtonClick,
                onDismissRequest = actions.onDismissDialog,
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.EncryptDatabase -> {
            EncryptDatabaseDialog(
                onDismissRequest = actions.onDismissDialog,
                onContinue = { shouldEncrypt ->
                    actions.onDismissDialog()
                    dialogState.onContinue(shouldEncrypt)
                },
            )
        }
        null -> {}
    }

    if (importProgress?.inProgress == true) {
        val msg =
            if (importProgress is ImportProgress) {
                val stageStr =
                    stringResource(
                        when (importProgress.stage) {
                            ImportStage.IMPORT_NOTES -> R.string.imported_notes
                            ImportStage.EXTRACT_FILES -> R.string.extracted_files
                            ImportStage.IMPORT_FILES -> R.string.imported_files
                        }
                    )
                stringResource(R.string.count, importProgress.current, importProgress.total) +
                    " " +
                    stageStr
            } else {
                stringResource(R.string.count, importProgress.current, importProgress.total) +
                    (importProgress.countSuffix?.let { " $it" } ?: "")
            }
        val progressValue =
            if (importProgress.indeterminate || importProgress.total == 0) null
            else importProgress.current.toFloat() / importProgress.total.toFloat()

        ProgressDialog(
            title = stringResource(importProgress.titleId),
            message = msg,
            progress = progressValue,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NotallyXTheme { SettingsScreen(state = SettingsUiState()) }
}

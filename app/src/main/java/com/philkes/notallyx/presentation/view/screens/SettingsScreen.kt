package com.philkes.notallyx.presentation.view.screens

import android.R.attr.maxLines
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
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.presentation.getQuantityStringPlain
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.components.PreferenceRow
import com.philkes.notallyx.presentation.view.components.SectionHeaderItem
import com.philkes.notallyx.presentation.view.components.SeekBarPreferenceRow
import com.philkes.notallyx.presentation.view.components.StepperPreferenceRow
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.presentation.viewmodel.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.presentation.viewmodel.preference.DateFormat
import com.philkes.notallyx.presentation.viewmodel.preference.DefaultListNoteViewMode
import com.philkes.notallyx.presentation.viewmodel.preference.ListItemSort
import com.philkes.notallyx.presentation.viewmodel.preference.MAX_ITEMS_MAX
import com.philkes.notallyx.presentation.viewmodel.preference.MAX_LABELS_IN_NAVIGATION_MAX
import com.philkes.notallyx.presentation.viewmodel.preference.MAX_TITLE_LINES_MAX
import com.philkes.notallyx.presentation.viewmodel.preference.MAX_TITLE_LINES_MIN
import com.philkes.notallyx.presentation.viewmodel.preference.NUMBER_PREF_DISABLED
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSort
import com.philkes.notallyx.presentation.viewmodel.preference.NotesView
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup.Companion.BACKUP_MAX_MAX
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup.Companion.BACKUP_MAX_MIN
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MAX
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MIN
import com.philkes.notallyx.presentation.viewmodel.preference.TEXT_SIZE_MAX
import com.philkes.notallyx.presentation.viewmodel.preference.TEXT_SIZE_MIN
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import com.philkes.notallyx.presentation.viewmodel.preference.TimeFormat
import com.philkes.notallyx.utils.getDocumentFolder
import com.philkes.notallyx.utils.toReadablePath
import java.util.Date
import kotlin.math.roundToInt

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
fun Context.createFromPreferences(preferences: NotallyXPreferences): SettingsUiState {
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

/** Composable replacing fragment_settings.xml layout. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onNotesViewClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onDateFormatOverviewClick: () -> Unit = {},
    onDateFormatNoteViewClick: () -> Unit = {},
    onTextSizeNoteEditorChange: (Float) -> Unit = {},
    onTextSizeOverviewChange: (Float) -> Unit = {},
    onAlwaysShowSearchBarClick: () -> Unit = {},
    onNotesSortOrderClick: () -> Unit = {},
    onCheckedListItemSortingClick: () -> Unit = {},
    onStartViewClick: () -> Unit = {},
    onDefaultListNoteViewModeClick: () -> Unit = {},
    onAutoSaveAfterIdleChange: (Int) -> Unit = {},
    onAutoRemoveDeletedNotesChange: (Int) -> Unit = {},
    onMaxTitleChange: (Int) -> Unit = {},
    onMaxItemsChange: (Int) -> Unit = {},
    onMaxLinesChange: (Int) -> Unit = {},
    onMaxLabelsChange: (Int) -> Unit = {},
    onLabelTagsHiddenClick: () -> Unit = {},
    onImagesHiddenClick: () -> Unit = {},
    onImportBackupClick: () -> Unit = {},
    onImportOtherClick: () -> Unit = {},
    onExportBackupClick: () -> Unit = {},
    onBackupsFolderClick: () -> Unit = {},
    onBackupOnSaveClick: () -> Unit = {},
    onPeriodicBackupsClick: () -> Unit = {},
    onPeriodicBackupsPeriodInDaysChange: (Int) -> Unit = {},
    onPeriodicBackupsMaxChange: (Int) -> Unit = {},
    onBiometricLockClick: () -> Unit = {},
    onBackupPasswordClick: () -> Unit = {},
    onSecureFlagClick: () -> Unit = {},
    onImportSettingsClick: () -> Unit = {},
    onExportSettingsClick: () -> Unit = {},
    onResetSettingsClick: () -> Unit = {},
    onDataInPublicFolderClick: () -> Unit = {},
    onClearDataClick: () -> Unit = {},
    onSendFeedbackClick: () -> Unit = {},
    onRateClick: () -> Unit = {},
    onDonateClick: () -> Unit = {},
    onViewLogsClick: () -> Unit = {},
    onViewCrashesClick: () -> Unit = {},
    onDocumentationClick: () -> Unit = {},
    onSourceCodeClick: () -> Unit = {},
) {
    val context = LocalContext.current

    fun formatDateTime(dateFormat: DateFormat, timeFormat: TimeFormat): String {
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

    val notesText = "${stringResource(R.string.notes)} (${stringResource(R.string.text_default)})"
    val unlabeledText = stringResource(R.string.unlabeled)
    val startViewValueText =
        when (state.startView) {
            START_VIEW_DEFAULT -> notesText
            START_VIEW_UNLABELED -> unlabeledText
            else -> state.startView
        }

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

    val biometricLockText =
        if (state.biometricLock == BiometricLock.ENABLED && state.biometricLockEncryptsDb) {
            stringResource(id = R.string.biometric_lock_enabled_with_encryption)
        } else {
            state.biometricLock.getText(context)
        }

    val periodicBackupsEnabled =
        state.periodicBackups.periodInDays > 0 && state.backupsFolder != EMPTY_PATH

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true }
                .verticalScroll(scrollState)
    ) {
        // Section: Appearance
        SectionHeaderItem(title = stringResource(id = R.string.appearance))
        PreferenceRow(
            title = stringResource(id = R.string.view),
            value = state.notesView.getText(context),
            testTag = "View",
            onClick = onNotesViewClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.theme),
            value = state.theme.getText(context),
            testTag = "Theme",
            onClick = onThemeClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.date_format_overview),
            value = formatDateTime(state.dateFormatOverview, state.timeFormatOverview),
            testTag = "DateFormatOverview",
            onClick = onDateFormatOverviewClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.date_format_note_view),
            value = formatDateTime(state.dateFormatNoteView, state.timeFormatNoteView),
            testTag = "DateFormatNoteView",
            onClick = onDateFormatNoteViewClick,
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.text_size_note_editor),
            value = state.textSizeNoteEditor,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            testTag = "TextSize",
            onValueChange = onTextSizeNoteEditorChange,
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.text_size_overview),
            value = state.textSizeOverview,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            testTag = "TextSizeOverview",
            onValueChange = onTextSizeOverviewChange,
        )
        PreferenceRow(
            title = stringResource(id = R.string.always_show_search_bar),
            value =
                if (state.alwaysShowSearchBar) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "ShowSearchInTopBar",
            onClick = onAlwaysShowSearchBarClick,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Behaviour
        SectionHeaderItem(title = stringResource(id = R.string.behaviour))
        PreferenceRow(
            title = stringResource(id = R.string.notes_sorted_by),
            value = state.notesSort.getText(context),
            testTag = "NotesSortOrder",
            onClick = onNotesSortOrderClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.list_item_auto_sort),
            value = state.listItemSorting.getText(context),
            testTag = "CheckedListItemSorting",
            onClick = onCheckedListItemSortingClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.start_view),
            value = startViewValueText,
            testTag = "StartView",
            onClick = onStartViewClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.default_list_note_view_mode),
            value = state.defaultListNoteViewMode.getText(context),
            testTag = "DefaultListNoteViewMode",
            onClick = onDefaultListNoteViewModeClick,
        )
        val offText = stringResource(id = R.string.off)
        StepperPreferenceRow(
            title = stringResource(id = R.string.auto_save_after_idle_time),
            value = state.autoSaveAfterIdleTime,
            valueRange = NUMBER_PREF_DISABLED..Integer.MAX_VALUE,
            labelFormatter = { v -> if (v == -1) offText else "${v}s" },
            testTag = "AutoSaveAfterIdle",
            onValueChange = onAutoSaveAfterIdleChange,
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.auto_remove_deleted_notes),
            value = state.autoRemoveDeletedNotesAfterDays,
            valueRange = 0..Integer.MAX_VALUE,
            labelFormatter = { v ->
                if (v == 0) offText else "$v ${context.getQuantityStringPlain(R.plurals.days, v)}"
            },
            testTag = "AutoEmptyBin",
            onValueChange = onAutoRemoveDeletedNotesChange,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Content Density
        SectionHeaderItem(title = stringResource(id = R.string.content_density))
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_lines_to_display_title),
            value = state.maxTitle.toFloat(),
            valueRange = MAX_TITLE_LINES_MIN.toFloat()..MAX_TITLE_LINES_MAX.toFloat(),
            testTag = "MaxTitle",
            onValueChange = { onMaxTitleChange(it.roundToInt()) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_items_to_display),
            value = state.maxItems.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            testTag = "MaxItems",
            onValueChange = { onMaxItemsChange(it.roundToInt()) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_lines_to_display),
            value = state.maxLines.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            testTag = "MaxLines",
            onValueChange = { onMaxLinesChange(it.roundToInt()) },
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.max_labels_to_display),
            value = state.maxLabels,
            valueRange = 0..MAX_LABELS_IN_NAVIGATION_MAX,
            testTag = "MaxLabels",
            onValueChange = onMaxLabelsChange,
        )
        PreferenceRow(
            title = stringResource(id = R.string.labels_hidden_in_overview_title),
            value =
                if (state.labelTagsHiddenInOverview) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "LabelsHiddenInOverview",
            onClick = onLabelTagsHiddenClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.images_hidden_in_overview_title),
            value =
                if (state.imagesHiddenInOverview) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "ImagesHiddenInOverview",
            onClick = onImagesHiddenClick,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Backup
        SectionHeaderItem(title = stringResource(id = R.string.backup))
        PreferenceRow(
            title = stringResource(id = R.string.import_backup),
            testTag = "ImportBackup",
            onClick = onImportBackupClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.import_other),
            testTag = "ImportOther",
            onClick = onImportOtherClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.export_backup),
            testTag = "ExportBackup",
            onClick = onExportBackupClick,
        )

        SectionHeaderItem(title = stringResource(id = R.string.auto_backup))
        PreferenceRow(
            title = stringResource(id = R.string.auto_backups_folder),
            value = backupsFolderText,
            testTag = "BackupsFolder",
            onClick = onBackupsFolderClick,
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
            onClick = onBackupOnSaveClick,
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
            onClick = onPeriodicBackupsClick,
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
            onValueChange = onPeriodicBackupsPeriodInDaysChange,
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.max_backups),
            value = state.periodicBackups.maxBackups,
            valueRange = BACKUP_MAX_MIN..BACKUP_MAX_MAX,
            enabled = periodicBackupsEnabled,
            testTag = "PeriodicBackupsMax",
            onValueChange = onPeriodicBackupsMaxChange,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Security
        SectionHeaderItem(title = stringResource(id = R.string.security))
        PreferenceRow(
            title = stringResource(id = R.string.biometric_lock),
            value = biometricLockText,
            testTag = "BiometricLock",
            onClick = onBiometricLockClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.backup_password),
            value =
                if (state.backupPassword != PASSWORD_EMPTY) "••••••••"
                else stringResource(id = R.string.tap_to_set_up),
            testTag = "BackupPassword",
            onClick = onBackupPasswordClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.disallow_screenshots),
            value =
                if (state.secureFlag) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "SecureFlag",
            onClick = onSecureFlagClick,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: Settings
        SectionHeaderItem(title = stringResource(id = R.string.settings))
        PreferenceRow(
            title = stringResource(id = R.string.import_settings),
            testTag = "ImportSettings",
            onClick = onImportSettingsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.export_settings),
            testTag = "ExportSettings",
            onClick = onExportSettingsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.reset_settings),
            testTag = "ResetSettings",
            onClick = onResetSettingsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.data_in_public),
            value =
                if (state.dataInPublicFolder) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            testTag = "DataInPublicFolder",
            onClick = onDataInPublicFolderClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.clear_data),
            testTag = "ClearData",
            onClick = onClearDataClick,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Section: About
        SectionHeaderItem(title = stringResource(id = R.string.about))
        PreferenceRow(
            title = stringResource(id = R.string.send_feedback),
            testTag = "SendFeedback",
            onClick = onSendFeedbackClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.rate),
            testTag = "Rate",
            onClick = onRateClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.donate),
            testTag = "Donate",
            onClick = onDonateClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.view_logs),
            testTag = "ViewLogs",
            onClick = onViewLogsClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.view_crash_logs),
            testTag = "ViewCrashes",
            onClick = onViewCrashesClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.documentation),
            testTag = "Documentation",
            onClick = onDocumentationClick,
        )
        PreferenceRow(
            title = stringResource(id = R.string.source_code),
            testTag = "SourceCode",
            onClick = onSourceCodeClick,
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NotallyXTheme { SettingsScreen(state = SettingsUiState()) }
}

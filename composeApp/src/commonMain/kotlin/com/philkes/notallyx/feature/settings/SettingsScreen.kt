package com.philkes.notallyx.feature.settings

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.Res
import com.philkes.notallyx.about
import com.philkes.notallyx.always_show_search_bar
import com.philkes.notallyx.appearance
import com.philkes.notallyx.ascending
import com.philkes.notallyx.auto_backup
import com.philkes.notallyx.auto_backup_last
import com.philkes.notallyx.auto_backup_on_save
import com.philkes.notallyx.auto_backups_folder
import com.philkes.notallyx.auto_backups_folder_set
import com.philkes.notallyx.auto_remove_deleted_notes
import com.philkes.notallyx.auto_save_after_idle_time
import com.philkes.notallyx.auto_sort_by_checked
import com.philkes.notallyx.auto_sort_by_checked_timestamp
import com.philkes.notallyx.backup
import com.philkes.notallyx.backup_password
import com.philkes.notallyx.backup_password_hint
import com.philkes.notallyx.backup_period_days
import com.philkes.notallyx.backup_periodic
import com.philkes.notallyx.behaviour
import com.philkes.notallyx.biometric_lock
import com.philkes.notallyx.biometric_lock_enabled_with_encryption
import com.philkes.notallyx.cancel
import com.philkes.notallyx.clear_data
import com.philkes.notallyx.color
import com.philkes.notallyx.content_density
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.Constants.EMPTY_PATH
import com.philkes.notallyx.core.preference.Constants.MAX_ITEMS_MAX
import com.philkes.notallyx.core.preference.Constants.MAX_LABELS_IN_NAVIGATION_MAX
import com.philkes.notallyx.core.preference.Constants.MAX_TITLE_LINES_MAX
import com.philkes.notallyx.core.preference.Constants.MAX_TITLE_LINES_MIN
import com.philkes.notallyx.core.preference.Constants.NUMBER_PREF_DISABLED
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.Constants.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.Constants.START_VIEW_UNLABELED
import com.philkes.notallyx.core.preference.Constants.TEXT_SIZE_MAX
import com.philkes.notallyx.core.preference.Constants.TEXT_SIZE_MIN
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_MAX_MAX
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_MAX_MIN
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MAX
import com.philkes.notallyx.core.preference.PeriodicBackup.Companion.BACKUP_PERIOD_DAYS_MIN
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.SectionHeaderItem
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.IconListDialog
import com.philkes.notallyx.core.ui.dialog.ListDialog
import com.philkes.notallyx.core.ui.dialog.ProgressDialog
import com.philkes.notallyx.core.ui.dialog.SelectionItem
import com.philkes.notallyx.core.ui.dialog.SingleSelectionDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.count
import com.philkes.notallyx.creation_date
import com.philkes.notallyx.dark
import com.philkes.notallyx.data.model.extensions.formatDate
import com.philkes.notallyx.data_in_public
import com.philkes.notallyx.date_format_note_view
import com.philkes.notallyx.date_format_overview
import com.philkes.notallyx.days
import com.philkes.notallyx.default_list_note_view_mode
import com.philkes.notallyx.descending
import com.philkes.notallyx.disabled
import com.philkes.notallyx.disallow_screenshots
import com.philkes.notallyx.documentation
import com.philkes.notallyx.donate
import com.philkes.notallyx.edit
import com.philkes.notallyx.enabled
import com.philkes.notallyx.export_backup
import com.philkes.notallyx.export_settings
import com.philkes.notallyx.feature.settings.dialogs.DateTimeFormatDialog
import com.philkes.notallyx.feature.settings.dialogs.EncryptDatabaseDialog
import com.philkes.notallyx.feature.settings.dialogs.ImportNotesDialog
import com.philkes.notallyx.feature.settings.dialogs.NotesSortDialog
import com.philkes.notallyx.feature.settings.dialogs.displayLabel
import com.philkes.notallyx.follow_system
import com.philkes.notallyx.grid
import com.philkes.notallyx.images_hidden_in_overview_title
import com.philkes.notallyx.import_backup
import com.philkes.notallyx.import_backup_password_hint
import com.philkes.notallyx.import_other
import com.philkes.notallyx.import_settings
import com.philkes.notallyx.importing_backup
import com.philkes.notallyx.labels_hidden_in_overview_title
import com.philkes.notallyx.last_used
import com.philkes.notallyx.light
import com.philkes.notallyx.list
import com.philkes.notallyx.list_item_auto_sort
import com.philkes.notallyx.max_backups
import com.philkes.notallyx.max_items_to_display
import com.philkes.notallyx.max_labels_to_display
import com.philkes.notallyx.max_lines_to_display
import com.philkes.notallyx.max_lines_to_display_title
import com.philkes.notallyx.modified_date
import com.philkes.notallyx.no_auto_sort
import com.philkes.notallyx.notes
import com.philkes.notallyx.notes_sorted_by
import com.philkes.notallyx.off
import com.philkes.notallyx.rate
import com.philkes.notallyx.read_only
import com.philkes.notallyx.reset_settings
import com.philkes.notallyx.security
import com.philkes.notallyx.send_feedback
import com.philkes.notallyx.settings
import com.philkes.notallyx.source_code
import com.philkes.notallyx.start_view
import com.philkes.notallyx.super_dark
import com.philkes.notallyx.tap_to_set_up
import com.philkes.notallyx.text_default
import com.philkes.notallyx.text_size_note_editor
import com.philkes.notallyx.text_size_overview
import com.philkes.notallyx.theme
import com.philkes.notallyx.theme_use_dynamic_colors
import com.philkes.notallyx.title
import com.philkes.notallyx.unlabeled
import com.philkes.notallyx.view
import com.philkes.notallyx.view_crash_logs
import com.philkes.notallyx.view_logs
import java.util.Date
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun NotesView.displayLabel(): String =
    when (this) {
        NotesView.GRID -> stringResource(Res.string.grid)
        NotesView.LIST -> stringResource(Res.string.list)
    }

@Composable
fun Theme.displayLabel(): String =
    when (this) {
        Theme.LIGHT -> stringResource(Res.string.light)
        Theme.DARK -> stringResource(Res.string.dark)
        Theme.SUPER_DARK -> stringResource(Res.string.super_dark)
        Theme.FOLLOW_SYSTEM -> stringResource(Res.string.follow_system)
    }

@Composable
fun ListItemSort.displayLabel(): String =
    when (this) {
        ListItemSort.NO_AUTO_SORT -> stringResource(Res.string.no_auto_sort)
        ListItemSort.AUTO_SORT_BY_CHECKED -> stringResource(Res.string.auto_sort_by_checked)
        ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP ->
            stringResource(Res.string.auto_sort_by_checked_timestamp)
    }

@Composable
fun DefaultListNoteViewMode.displayLabel(): String =
    when (this) {
        DefaultListNoteViewMode.READ_ONLY -> stringResource(Res.string.read_only)
        DefaultListNoteViewMode.EDIT -> stringResource(Res.string.edit)
        DefaultListNoteViewMode.LAST_USED -> stringResource(Res.string.last_used)
    }

@Composable
fun BiometricLock.displayLabel(): String =
    when (this) {
        BiometricLock.DISABLED -> stringResource(Res.string.disabled)
        BiometricLock.ENABLED -> stringResource(Res.string.enabled)
    }

@Composable
fun NotesSort.displayLabel(): String {
    val sortByName =
        when (sortedBy) {
            NotesSortBy.TITLE -> stringResource(Res.string.title)
            NotesSortBy.CREATION_DATE -> stringResource(Res.string.creation_date)
            NotesSortBy.MODIFIED_DATE -> stringResource(Res.string.modified_date)
            NotesSortBy.COLOR -> stringResource(Res.string.color)
        }
    val directionName =
        when (sortDirection) {
            SortDirection.ASC -> stringResource(Res.string.ascending)
            SortDirection.DESC -> stringResource(Res.string.descending)
        }
    return "$sortByName ($directionName)"
}

@Composable
private fun formatDateTime(
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
): String {
    val dateText = if (dateFormat == DateFormat.NONE) "" else dateFormat.displayLabel()
    val timeText = if (timeFormat == TimeFormat.NONE) "" else timeFormat.displayLabel()
    return when {
        dateFormat == DateFormat.NONE && timeFormat == TimeFormat.NONE -> dateFormat.displayLabel()
        dateFormat == DateFormat.NONE -> timeText
        timeFormat == TimeFormat.NONE -> dateText
        else -> "$dateText $timeText"
    }
}

@Composable
private fun SettingsAppearanceSection(
    state: PreferencesUiModel,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(Res.string.appearance))
        PreferenceRow(
            title = stringResource(Res.string.view),
            value = state.notesView.displayLabel(),
            modifier = Modifier.testTag(TestTags.SETTINGS_NOTES_VIEW),
            onClick = { onAction(SettingsAction.ShowNotesViewDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.theme),
            value = state.theme.displayLabel(),
            modifier = Modifier.testTag(TestTags.SETTINGS_THEME),
            onClick = { onAction(SettingsAction.ShowThemeDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.date_format_overview),
            value = formatDateTime(state.dateFormatOverview, state.timeFormatOverview),
            onClick = { onAction(SettingsAction.ShowDateFormatOverviewDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.date_format_note_view),
            value = formatDateTime(state.dateFormatNoteView, state.timeFormatNoteView),
            onClick = { onAction(SettingsAction.ShowDateFormatNoteViewDialog) },
        )
        SeekBarPreferenceRow(
            title = stringResource(Res.string.text_size_note_editor),
            value = state.textSizeNoteEditor,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            onValueChange = { onAction(SettingsAction.SetTextSizeNoteEditor(it)) },
        )
        SeekBarPreferenceRow(
            title = stringResource(Res.string.text_size_overview),
            value = state.textSizeOverview,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            onValueChange = { onAction(SettingsAction.SetTextSizeOverview(it)) },
        )
        PreferenceRow(
            title = stringResource(Res.string.always_show_search_bar),
            value =
                if (state.alwaysShowSearchBar) stringResource(Res.string.enabled)
                else stringResource(Res.string.disabled),
            onClick = { onAction(SettingsAction.ShowAlwaysShowSearchBarDialog) },
        )
    }
}

@Composable
private fun SettingsBehaviourSection(
    state: PreferencesUiModel,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val notesText =
        "${stringResource(Res.string.notes)} (${stringResource(Res.string.text_default)})"
    val unlabeledText = stringResource(Res.string.unlabeled)
    val startViewValueText =
        when (state.startView) {
            START_VIEW_DEFAULT -> notesText
            START_VIEW_UNLABELED -> unlabeledText
            else -> state.startView
        }
    val offText = stringResource(Res.string.off)
    val daySingular = pluralStringResource(Res.plurals.days, 1)
    val dayPlural = pluralStringResource(Res.plurals.days, 2)

    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(Res.string.behaviour))
        PreferenceRow(
            title = stringResource(Res.string.notes_sorted_by),
            value = state.notesSort.displayLabel(),
            modifier = Modifier.testTag(TestTags.SETTINGS_SORTING),
            onClick = { onAction(SettingsAction.ShowNotesSortDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.list_item_auto_sort),
            value = state.listItemSorting.displayLabel(),
            onClick = { onAction(SettingsAction.ShowListItemSortingDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.start_view),
            value = startViewValueText,
            onClick = { onAction(SettingsAction.ShowStartViewDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.default_list_note_view_mode),
            value = state.defaultListNoteViewMode.displayLabel(),
            onClick = { onAction(SettingsAction.ShowDefaultListNoteViewModeDialog) },
        )
        StepperPreferenceRow(
            title = stringResource(Res.string.auto_save_after_idle_time),
            value = state.autoSaveAfterIdleTime,
            valueRange = NUMBER_PREF_DISABLED..Int.MAX_VALUE,
            labelFormatter = { v -> if (v == -1) offText else "${v}s" },
            onValueChange = { onAction(SettingsAction.SetAutoSaveAfterIdleTime(it)) },
        )
        StepperPreferenceRow(
            title = stringResource(Res.string.auto_remove_deleted_notes),
            value = state.autoRemoveDeletedNotesAfterDays,
            valueRange = 0..Int.MAX_VALUE,
            labelFormatter = { v ->
                if (v == 0) offText else "$v ${if (v == 1) daySingular else dayPlural}"
            },
            onValueChange = { onAction(SettingsAction.UpdateAutoRemoveDays(it)) },
        )
    }
}

@Composable
private fun SettingsContentDensitySection(
    state: PreferencesUiModel,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(Res.string.content_density))
        SeekBarPreferenceRow(
            title = stringResource(Res.string.max_lines_to_display_title),
            value = state.maxTitle.toFloat(),
            valueRange = MAX_TITLE_LINES_MIN.toFloat()..MAX_TITLE_LINES_MAX.toFloat(),
            onValueChange = { onAction(SettingsAction.SetMaxTitle(it.roundToInt())) },
        )
        SeekBarPreferenceRow(
            title = stringResource(Res.string.max_items_to_display),
            value = state.maxItems.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            onValueChange = { onAction(SettingsAction.SetMaxItems(it.roundToInt())) },
        )
        SeekBarPreferenceRow(
            title = stringResource(Res.string.max_lines_to_display),
            value = state.maxLines.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            onValueChange = { onAction(SettingsAction.SetMaxLines(it.roundToInt())) },
        )
        StepperPreferenceRow(
            title = stringResource(Res.string.max_labels_to_display),
            value = state.maxLabels,
            valueRange = 0..MAX_LABELS_IN_NAVIGATION_MAX,
            onValueChange = { onAction(SettingsAction.SetMaxLabels(it)) },
        )
        PreferenceRow(
            title = stringResource(Res.string.labels_hidden_in_overview_title),
            value =
                if (state.labelTagsHiddenInOverview) stringResource(Res.string.enabled)
                else stringResource(Res.string.disabled),
            onClick = { onAction(SettingsAction.ShowLabelTagsHiddenDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.images_hidden_in_overview_title),
            value =
                if (state.imagesHiddenInOverview) stringResource(Res.string.enabled)
                else stringResource(Res.string.disabled),
            onClick = { onAction(SettingsAction.ShowImagesHiddenDialog) },
        )
    }
}

@Composable
private fun SettingsBackupSection(
    state: PreferencesUiModel,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backupsFolderText =
        if (state.backupsFolder == EMPTY_PATH) {
            stringResource(Res.string.tap_to_set_up)
        } else {
            state.backupsFolder
        }
    val periodicBackupsEnabled =
        state.periodicBackups.periodInDays > 0 && state.backupsFolder != EMPTY_PATH

    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(Res.string.backup))
        PreferenceRow(
            title = stringResource(Res.string.import_backup),
            onClick = { onAction(SettingsAction.RequestImportBackup) },
        )
        PreferenceRow(
            title = stringResource(Res.string.import_other),
            onClick = { onAction(SettingsAction.ShowImportOtherDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.export_backup),
            onClick = { onAction(SettingsAction.RequestExportBackup) },
        )

        SectionHeaderItem(title = stringResource(Res.string.auto_backup))
        PreferenceRow(
            title = stringResource(Res.string.auto_backups_folder),
            value = backupsFolderText,
            onClick = { onAction(SettingsAction.ClickBackupsFolder) },
        )
        PreferenceRow(
            title = stringResource(Res.string.auto_backup_on_save),
            value =
                if (state.backupsFolder == EMPTY_PATH)
                    stringResource(Res.string.auto_backups_folder_set)
                else if (state.backupOnSave) stringResource(Res.string.enabled)
                else stringResource(Res.string.disabled),
            enabled = state.backupsFolder != EMPTY_PATH,
            onClick = { onAction(SettingsAction.ShowBackupOnSaveDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.backup_periodic),
            value =
                if (state.backupsFolder == EMPTY_PATH)
                    stringResource(Res.string.auto_backups_folder_set)
                else if (periodicBackupsEnabled) stringResource(Res.string.enabled)
                else stringResource(Res.string.disabled),
            enabled = state.backupsFolder != EMPTY_PATH,
            onClick = { onAction(SettingsAction.ShowPeriodicBackupsDialog) },
        )
        if (state.periodicBackupLastExecution != -1L) {
            val lastBackupFormatted =
                Date(state.periodicBackupLastExecution)
                    .formatDate(
                        state.dateFormatOverview,
                        state.timeFormatOverview,
                        ensureFullFormat = true,
                    )
            Text(
                text = "${stringResource(Res.string.auto_backup_last)}: $lastBackupFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        StepperPreferenceRow(
            title = stringResource(Res.string.backup_period_days),
            value = state.periodicBackups.periodInDays,
            valueRange = BACKUP_PERIOD_DAYS_MIN..BACKUP_PERIOD_DAYS_MAX,
            enabled = periodicBackupsEnabled,
            onValueChange = { onAction(SettingsAction.SetPeriodicBackupsPeriodInDays(it)) },
        )
        StepperPreferenceRow(
            title = stringResource(Res.string.max_backups),
            value = state.periodicBackups.maxBackups,
            valueRange = BACKUP_MAX_MIN..BACKUP_MAX_MAX,
            enabled = periodicBackupsEnabled,
            onValueChange = { onAction(SettingsAction.SetPeriodicBackupsMax(it)) },
        )
    }
}

@Composable
private fun SettingsSecuritySection(
    state: PreferencesUiModel,
    isBiometricsAvailable: Boolean,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val biometricLockText =
        if (
            NotallyDatabase.FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB &&
                state.biometricLock == BiometricLock.ENABLED &&
                state.biometricLockEncryptsDb
        ) {
            stringResource(Res.string.biometric_lock_enabled_with_encryption)
        } else {
            state.biometricLock.displayLabel()
        }

    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(Res.string.security))
        if (isBiometricsAvailable) {
            PreferenceRow(
                modifier = Modifier.testTag(TestTags.SETTINGS_BIOMETRIC_LOCK),
                title = stringResource(Res.string.biometric_lock),
                value = biometricLockText,
                onClick = { onAction(SettingsAction.ShowBiometricLockDialog) },
            )
        }
        PreferenceRow(
            title = stringResource(Res.string.backup_password),
            value =
                if (state.backupPassword != PASSWORD_EMPTY) "••••••••"
                else stringResource(Res.string.tap_to_set_up),
            onClick = { onAction(SettingsAction.ShowBackupPasswordDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.disallow_screenshots),
            value =
                if (state.secureFlag) stringResource(Res.string.enabled)
                else stringResource(Res.string.disabled),
            onClick = { onAction(SettingsAction.ShowSecureFlagDialog) },
        )
    }
}

@Composable
private fun SettingsGeneralSection(
    state: PreferencesUiModel,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(Res.string.settings))
        PreferenceRow(
            title = stringResource(Res.string.import_settings),
            onClick = { onAction(SettingsAction.RequestImportSettings) },
        )
        PreferenceRow(
            title = stringResource(Res.string.export_settings),
            onClick = { onAction(SettingsAction.RequestExportSettings) },
        )
        PreferenceRow(
            title = stringResource(Res.string.reset_settings),
            onClick = { onAction(SettingsAction.RequestResetSettings) },
        )
        PreferenceRow(
            title = stringResource(Res.string.data_in_public),
            value =
                if (state.dataInPublicFolder) stringResource(Res.string.enabled)
                else stringResource(Res.string.disabled),
            onClick = { onAction(SettingsAction.ShowDataInPublicFolderDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.clear_data),
            onClick = { onAction(SettingsAction.RequestClearData) },
        )
    }
}

@Composable
private fun SettingsAboutSection(
    state: PreferencesUiModel,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(Res.string.about))
        PreferenceRow(
            title = stringResource(Res.string.send_feedback),
            onClick = { onAction(SettingsAction.ShowSendFeedbackDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.rate),
            onClick = { onAction(SettingsAction.RateApp) },
        )
        PreferenceRow(
            title = stringResource(Res.string.donate),
            onClick = { onAction(SettingsAction.Donate) },
        )
        PreferenceRow(
            title = stringResource(Res.string.view_logs),
            onClick = { onAction(SettingsAction.ViewLogs) },
        )
        PreferenceRow(
            title = stringResource(Res.string.view_crash_logs),
            onClick = { onAction(SettingsAction.ShowCrashesDialog) },
        )
        PreferenceRow(
            title = stringResource(Res.string.documentation),
            onClick = { onAction(SettingsAction.OpenDocumentation) },
        )
        PreferenceRow(
            title = stringResource(Res.string.source_code),
            onClick = { onAction(SettingsAction.OpenSourceCode) },
        )
        if (state.appVersion.isNotEmpty()) {
            Text(
                text = "v${state.appVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

/** Composable replacing fragment_settings.xml layout. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onAction: (SettingsAction) -> Unit = {},
) {
    Column(
        modifier =
            modifier.fillMaxSize().verticalScroll(scrollState).testTag(TestTags.SETTINGS_LIST)
    ) {
        SettingsAppearanceSection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsBehaviourSection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsContentDensitySection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsBackupSection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsSecuritySection(
            state = state.preferences,
            isBiometricsAvailable = state.isBiometricsAvailable,
            onAction = onAction,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsGeneralSection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsAboutSection(state = state.preferences, onAction = onAction)
    }

    when (val dialogState = state.activeDialog) {
        is SettingsDialogState.SingleSelection<*> -> {
            @Suppress("UNCHECKED_CAST")
            val s = dialogState as SettingsDialogState.SingleSelection<Any>
            SingleSelectionDialog(
                title = s.title,
                items = s.items,
                selectedItem = s.selectedItem,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = s.onConfirm,
            )
        }
        is SettingsDialogState.Theme -> {
            PreferenceEnumWithToggleDialog(
                title = stringResource(Res.string.theme),
                items = Theme.entries.map { SelectionItem(it, it.displayLabel()) },
                selectedItem = dialogState.initialTheme,
                toggleLabel = stringResource(Res.string.theme_use_dynamic_colors),
                initialToggleValue = dialogState.initialUseDynamicColors,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.DateTimeFormat -> {
            DateTimeFormatDialog(
                initialDateFormat = dialogState.initialDate,
                initialTimeFormat = dialogState.initialTime,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.NotesSort -> {
            NotesSortDialog(
                currentSort = dialogState.currentSort,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.BooleanPref -> {
            PreferenceBooleanDialog(
                title = dialogState.title,
                message = dialogState.message,
                initialValue = dialogState.initialValue,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.BackupPassword -> {
            TextInputDialog(
                title = stringResource(Res.string.backup_password),
                initialValue = dialogState.password,
                message = stringResource(Res.string.backup_password_hint),
                isPassword = true,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.ImportNotesPassword -> {
            ImportNotesDialog(
                title = stringResource(Res.string.backup_password),
                initialText = dialogState.initialPassword,
                message = stringResource(Res.string.import_backup_password_hint),
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.IconList -> {
            IconListDialog(
                title = dialogState.title,
                items = dialogState.items,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onItemClick = dialogState.onItemClick,
            )
        }
        is SettingsDialogState.ListSelection -> {
            ListDialog(
                title = dialogState.title,
                items = dialogState.items,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onItemClick = dialogState.onItemClick,
            )
        }
        is SettingsDialogState.Confirm -> {
            ConfirmDialog(
                title = dialogState.title,
                message = dialogState.message,
                positiveButtonText = dialogState.positiveButtonText.ifEmpty { "OK" },
                negativeButtonText =
                    dialogState.negativeButtonText.ifEmpty { stringResource(Res.string.cancel) },
                neutralButtonText = dialogState.neutralButtonText,
                onNeutralButtonClick = dialogState.onNeutralButtonClick,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.EncryptDatabase -> {
            if (NotallyDatabase.FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB) {
                EncryptDatabaseDialog(
                    onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                    onContinue = dialogState.onContinue,
                )
            }
        }
        null -> {}
    }

    val importProgress = state.importProgress
    if (importProgress?.inProgress == true) {
        val countText =
            stringResource(Res.string.count, importProgress.current, importProgress.total)
        val msg =
            if (importProgress.countSuffix != null) {
                "$countText ${importProgress.countSuffix}"
            } else {
                countText
            }
        val progressValue =
            if (importProgress.indeterminate || importProgress.total == 0) null
            else importProgress.current.toFloat() / importProgress.total.toFloat()

        ProgressDialog(
            title = stringResource(Res.string.importing_backup),
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

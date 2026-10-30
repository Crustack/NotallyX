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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getDocumentFolder
import com.philkes.notallyx.core.common.extensions.toReadablePath
import com.philkes.notallyx.core.common.result.ImportProgress
import com.philkes.notallyx.core.common.result.ImportStage
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.MAX_ITEMS_MAX
import com.philkes.notallyx.core.preference.MAX_LABELS_IN_NAVIGATION_MAX
import com.philkes.notallyx.core.preference.MAX_TITLE_LINES_MAX
import com.philkes.notallyx.core.preference.MAX_TITLE_LINES_MIN
import com.philkes.notallyx.core.preference.NUMBER_PREF_DISABLED
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_DEFAULT
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.START_VIEW_UNLABELED
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
    state: PreferencesUiModel,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        SectionHeaderItem(title = stringResource(id = R.string.appearance))
        PreferenceRow(
            title = stringResource(id = R.string.view),
            value = state.notesView.getText(context),
            onClick = { onAction(SettingsAction.ShowNotesViewDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.theme),
            value = state.theme.getText(context),
            onClick = { onAction(SettingsAction.ShowThemeDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.date_format_overview),
            value = formatDateTime(context, state.dateFormatOverview, state.timeFormatOverview),
            onClick = { onAction(SettingsAction.ShowDateFormatOverviewDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.date_format_note_view),
            value = formatDateTime(context, state.dateFormatNoteView, state.timeFormatNoteView),
            onClick = { onAction(SettingsAction.ShowDateFormatNoteViewDialog) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.text_size_note_editor),
            value = state.textSizeNoteEditor,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            onValueChange = { onAction(SettingsAction.SetTextSizeNoteEditor(it)) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.text_size_overview),
            value = state.textSizeOverview,
            valueRange = TEXT_SIZE_MIN..TEXT_SIZE_MAX,
            showPreviewText = true,
            onValueChange = { onAction(SettingsAction.SetTextSizeOverview(it)) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.always_show_search_bar),
            value =
                if (state.alwaysShowSearchBar) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
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
            onClick = { onAction(SettingsAction.ShowNotesSortDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.list_item_auto_sort),
            value = state.listItemSorting.getText(context),
            onClick = { onAction(SettingsAction.ShowListItemSortingDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.start_view),
            value = startViewValueText,
            onClick = { onAction(SettingsAction.ShowStartViewDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.default_list_note_view_mode),
            value = state.defaultListNoteViewMode.getText(context),
            onClick = { onAction(SettingsAction.ShowDefaultListNoteViewModeDialog) },
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.auto_save_after_idle_time),
            value = state.autoSaveAfterIdleTime,
            valueRange = NUMBER_PREF_DISABLED..Integer.MAX_VALUE,
            labelFormatter = { v -> if (v == -1) offText else "${v}s" },
            onValueChange = { onAction(SettingsAction.SetAutoSaveAfterIdleTime(it)) },
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.auto_remove_deleted_notes),
            value = state.autoRemoveDeletedNotesAfterDays,
            valueRange = 0..Integer.MAX_VALUE,
            labelFormatter = { v ->
                if (v == 0) offText else "$v ${context.getQuantityStringPlain(R.plurals.days, v)}"
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
        SectionHeaderItem(title = stringResource(id = R.string.content_density))
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_lines_to_display_title),
            value = state.maxTitle.toFloat(),
            valueRange = MAX_TITLE_LINES_MIN.toFloat()..MAX_TITLE_LINES_MAX.toFloat(),
            onValueChange = { onAction(SettingsAction.SetMaxTitle(it.roundToInt())) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_items_to_display),
            value = state.maxItems.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            onValueChange = { onAction(SettingsAction.SetMaxItems(it.roundToInt())) },
        )
        SeekBarPreferenceRow(
            title = stringResource(id = R.string.max_lines_to_display),
            value = state.maxLines.toFloat(),
            valueRange = 0f..MAX_ITEMS_MAX.toFloat(),
            onValueChange = { onAction(SettingsAction.SetMaxLines(it.roundToInt())) },
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.max_labels_to_display),
            value = state.maxLabels,
            valueRange = 0..MAX_LABELS_IN_NAVIGATION_MAX,
            onValueChange = { onAction(SettingsAction.SetMaxLabels(it)) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.labels_hidden_in_overview_title),
            value =
                if (state.labelTagsHiddenInOverview) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            onClick = { onAction(SettingsAction.ShowLabelTagsHiddenDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.images_hidden_in_overview_title),
            value =
                if (state.imagesHiddenInOverview) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
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
            onClick = { onAction(SettingsAction.RequestImportBackup) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.import_other),
            onClick = { onAction(SettingsAction.ShowImportOtherDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.export_backup),
            onClick = { onAction(SettingsAction.RequestExportBackup) },
        )

        SectionHeaderItem(title = stringResource(id = R.string.auto_backup))
        PreferenceRow(
            title = stringResource(id = R.string.auto_backups_folder),
            value = backupsFolderText,
            onClick = { onAction(SettingsAction.ClickBackupsFolder) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.auto_backup_on_save),
            value =
                if (state.backupsFolder == EMPTY_PATH)
                    stringResource(id = R.string.auto_backups_folder_set)
                else if (state.backupOnSave) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            enabled = state.backupsFolder != EMPTY_PATH,
            onClick = { onAction(SettingsAction.ShowBackupOnSaveDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.backup_periodic),
            value =
                if (state.backupsFolder == EMPTY_PATH)
                    stringResource(id = R.string.auto_backups_folder_set)
                else if (periodicBackupsEnabled) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            enabled = state.backupsFolder != EMPTY_PATH,
            onClick = { onAction(SettingsAction.ShowPeriodicBackupsDialog) },
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
        StepperPreferenceRow(
            title = stringResource(id = R.string.backup_period_days),
            value = state.periodicBackups.periodInDays,
            valueRange = BACKUP_PERIOD_DAYS_MIN..BACKUP_PERIOD_DAYS_MAX,
            enabled = periodicBackupsEnabled,
            onValueChange = { onAction(SettingsAction.SetPeriodicBackupsPeriodInDays(it)) },
        )
        StepperPreferenceRow(
            title = stringResource(id = R.string.max_backups),
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
    onAction: (SettingsAction) -> Unit,
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
            onClick = { onAction(SettingsAction.ShowBiometricLockDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.backup_password),
            value =
                if (state.backupPassword != PASSWORD_EMPTY) "••••••••"
                else stringResource(id = R.string.tap_to_set_up),
            onClick = { onAction(SettingsAction.ShowBackupPasswordDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.disallow_screenshots),
            value =
                if (state.secureFlag) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
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
        SectionHeaderItem(title = stringResource(id = R.string.settings))
        PreferenceRow(
            title = stringResource(id = R.string.import_settings),
            onClick = { onAction(SettingsAction.RequestImportSettings) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.export_settings),
            onClick = { onAction(SettingsAction.RequestExportSettings) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.reset_settings),
            onClick = { onAction(SettingsAction.RequestResetSettings) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.data_in_public),
            value =
                if (state.dataInPublicFolder) stringResource(id = R.string.enabled)
                else stringResource(id = R.string.disabled),
            onClick = { onAction(SettingsAction.ShowDataInPublicFolderDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.clear_data),
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
        SectionHeaderItem(title = stringResource(id = R.string.about))
        PreferenceRow(
            title = stringResource(id = R.string.send_feedback),
            onClick = { onAction(SettingsAction.ShowSendFeedbackDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.rate),
            onClick = { onAction(SettingsAction.RateApp) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.donate),
            onClick = { onAction(SettingsAction.Donate) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.view_logs),
            onClick = { onAction(SettingsAction.ViewLogs) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.view_crash_logs),
            onClick = { onAction(SettingsAction.ShowCrashesDialog) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.documentation),
            onClick = { onAction(SettingsAction.OpenDocumentation) },
        )
        PreferenceRow(
            title = stringResource(id = R.string.source_code),
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
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    onAction: (SettingsAction) -> Unit = {},
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize().verticalScroll(scrollState)) {
        SettingsAppearanceSection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsBehaviourSection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsContentDensitySection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsBackupSection(state = state.preferences, onAction = onAction)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingsSecuritySection(state = state.preferences, onAction = onAction)
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
                title = stringResource(R.string.theme),
                items = Theme.entries.map { SelectionItem(it, it.getText(context)) },
                selectedItem = dialogState.initialTheme,
                toggleLabel = stringResource(R.string.theme_use_dynamic_colors),
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
                title = stringResource(R.string.backup_password),
                initialValue = dialogState.password,
                message = stringResource(R.string.backup_password_hint),
                isPassword = true,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.ImportNotesPassword -> {
            ImportNotesDialog(
                title = stringResource(R.string.backup_password),
                initialText = dialogState.initialPassword,
                message = stringResource(R.string.import_backup_password_hint),
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
                positiveButtonText =
                    dialogState.positiveButtonText.ifEmpty { stringResource(android.R.string.ok) },
                negativeButtonText =
                    dialogState.negativeButtonText.ifEmpty { stringResource(R.string.cancel) },
                neutralButtonText = dialogState.neutralButtonText,
                onNeutralButtonClick = dialogState.onNeutralButtonClick,
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onConfirm = dialogState.onConfirm,
            )
        }
        is SettingsDialogState.EncryptDatabase -> {
            EncryptDatabaseDialog(
                onDismissRequest = { onAction(SettingsAction.DismissDialog) },
                onContinue = dialogState.onContinue,
            )
        }
        null -> {}
    }

    val importProgress = state.importProgress
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

package com.philkes.notallyx.feature.reminder

import androidx.annotation.StringRes
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.Reminder
import java.util.Calendar

const val NEW_REMINDER_ID = -1L

enum class PermissionType {
    NOTIFICATION,
    ALARM,
}

sealed interface NoteRemindersDialogState {
    data class DatePicker(
        val reminder: Reminder?,
        val calendar: Calendar,
        val initialDateMillis: Long,
    ) : NoteRemindersDialogState

    data class TimePicker(
        val reminder: Reminder?,
        val calendar: Calendar,
    ) : NoteRemindersDialogState

    data class Repetition(
        val reminder: Reminder?,
        val calendar: Calendar,
        val initialOption: RepetitionOption,
    ) : NoteRemindersDialogState

    data class MonthlyAdvanced(
        val reminder: Reminder?,
        val calendar: Calendar,
        val options: List<String>,
        val selectedIndex: Int,
        val dayOfWeek: Int,
    ) : NoteRemindersDialogState

    data class CustomRepetition(
        val reminder: Reminder?,
        val calendar: Calendar,
        val initialValue: String,
        val initialUnit: TimeUnit,
    ) : NoteRemindersDialogState

    data class ConfirmDelete(
        val reminder: Reminder,
        val formattedDateTime: String,
        val repetitionText: String,
    ) : NoteRemindersDialogState

    data class PermissionRationale(
        @StringRes val messageResId: Int,
        val permissionType: PermissionType,
    ) : NoteRemindersDialogState
}

typealias ReminderDialogState = NoteRemindersDialogState

data class NoteRemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormat: TimeFormat = TimeFormat.TWENTY_FOUR_H,
    val activeDialog: NoteRemindersDialogState? = null,
    val isLoading: Boolean = false,
)

sealed interface NoteRemindersAction {
    data class Init(val noteId: Long) : NoteRemindersAction

    data object AddReminderClicked : NoteRemindersAction

    data class EditReminderClicked(val reminder: Reminder) : NoteRemindersAction

    data class RequestDeleteReminder(val reminder: Reminder) : NoteRemindersAction

    data class ConfirmDeleteReminder(val reminder: Reminder? = null) : NoteRemindersAction

    data object DismissDialog : NoteRemindersAction

    data class DateSelected(
        val year: Int,
        val month: Int,
        val day: Int,
        val reminder: Reminder? = null,
        val calendar: Calendar? = null,
    ) : NoteRemindersAction

    data class TimeSelected(
        val hour: Int,
        val minute: Int,
        val reminder: Reminder? = null,
        val calendar: Calendar? = null,
    ) : NoteRemindersAction

    data object TimePickerBack : NoteRemindersAction

    data class RepetitionOptionSelected(
        val option: RepetitionOption,
        val reminder: Reminder? = null,
        val calendar: Calendar? = null,
    ) : NoteRemindersAction

    data class MonthlyAdvancedSelected(
        val which: Int,
        val dayOfWeek: Int,
        val reminder: Reminder? = null,
        val calendar: Calendar? = null,
    ) : NoteRemindersAction

    data class CustomRepetitionSelected(
        val value: Int,
        val timeUnit: TimeUnit,
        val reminder: Reminder? = null,
        val calendar: Calendar? = null,
    ) : NoteRemindersAction

    data class ShowPermissionRationale(
        @StringRes val messageResId: Int,
        val permissionType: PermissionType,
    ) : NoteRemindersAction

    data class ConfirmPermissionRationale(val permissionType: PermissionType) : NoteRemindersAction

    data class PermissionResult(val permissionType: PermissionType, val isGranted: Boolean) :
        NoteRemindersAction

    data class CheckPermissionsResult(val hasPermission: Boolean) : NoteRemindersAction
}

sealed interface NoteRemindersSideEffect {
    data class CheckPermissions(
        val pendingReminder: Reminder? = null,
        val pendingCalendar: Calendar? = null,
    ) : NoteRemindersSideEffect

    data class RequestNotificationPermission(
        val pendingReminder: Reminder? = null,
        val pendingCalendar: Calendar? = null,
    ) : NoteRemindersSideEffect

    data class RequestAlarmPermission(
        val pendingReminder: Reminder? = null,
        val pendingCalendar: Calendar? = null,
    ) : NoteRemindersSideEffect
}

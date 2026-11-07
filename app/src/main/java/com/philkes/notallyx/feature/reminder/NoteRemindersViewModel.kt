package com.philkes.notallyx.feature.reminder

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.BaseViewModel
import com.philkes.notallyx.core.common.extensions.now
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.extensions.getQuantityStringPlain
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.extensions.toCalendar
import com.philkes.notallyx.data.model.extensions.toText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteRemindersViewModel(
    app: Application,
    preferences: NotallyXPreferences,
) : BaseViewModel(app, preferences) {

    private var noteId: Long = -1L
    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    private val _activeDialog = MutableStateFlow<NoteRemindersDialogState?>(null)
    private val _isLoading = MutableStateFlow(true)

    private var pendingReminder: Reminder? = null
    private var pendingCalendar: Calendar? = null
    private var pendingReminderToDelete: Reminder? = null

    private val _sideEffects = Channel<NoteRemindersSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<NoteRemindersSideEffect> = _sideEffects.receiveAsFlow()

    val uiState: StateFlow<NoteRemindersUiState> =
        combine(
                _reminders,
                _activeDialog,
                _isLoading,
                preferences.userPreferencesFlow,
            ) { reminders, activeDialog, isLoading, userPrefs ->
                NoteRemindersUiState(
                    reminders = reminders,
                    dateFormat = userPrefs.dateFormatNoteView,
                    timeFormat = userPrefs.timeFormatNoteView,
                    activeDialog = activeDialog,
                    isLoading = isLoading,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    NoteRemindersUiState(
                        dateFormat = preferences.userPreferencesFlow.value.dateFormatNoteView,
                        timeFormat = preferences.userPreferencesFlow.value.timeFormatNoteView,
                        isLoading = true,
                    ),
            )

    fun onAction(action: NoteRemindersAction) {
        when (action) {
            is NoteRemindersAction.Init -> {
                if (noteId != action.noteId) {
                    noteId = action.noteId
                    loadNote(action.noteId)
                }
            }

            is NoteRemindersAction.AddReminderClicked -> {
                checkPermissionsAndOpenDatePicker(null, null)
            }

            is NoteRemindersAction.EditReminderClicked -> {
                checkPermissionsAndOpenDatePicker(action.reminder, null)
            }

            is NoteRemindersAction.RequestDeleteReminder -> {
                val reminder = action.reminder
                pendingReminderToDelete = reminder
                val formattedDateTime =
                    reminder.dateTime.format(
                        preferences.dateFormatNoteView.value,
                        preferences.timeFormatNoteView.value,
                        ensureFullFormat = true,
                    )
                val repetitionText =
                    reminder.repetition?.toText(app)
                        ?: app.getString(R.string.reminder_no_repetition)

                _activeDialog.value =
                    NoteRemindersDialogState.ConfirmDelete(
                        reminder = reminder,
                        formattedDateTime = formattedDateTime,
                        repetitionText = repetitionText,
                    )
            }

            is NoteRemindersAction.ConfirmDeleteReminder -> {
                val currentDialog = _activeDialog.value as? NoteRemindersDialogState.ConfirmDelete
                val reminderToDelete =
                    action.reminder ?: currentDialog?.reminder ?: pendingReminderToDelete
                _activeDialog.value = null
                pendingReminderToDelete = null
                reminderToDelete?.let { deleteReminder(it) }
            }

            is NoteRemindersAction.DismissDialog -> {
                _activeDialog.value = null
            }

            is NoteRemindersAction.DateSelected -> {
                val currentDialog = _activeDialog.value as? NoteRemindersDialogState.DatePicker
                val calendar = action.calendar ?: currentDialog?.calendar ?: now()
                val reminder = action.reminder ?: currentDialog?.reminder
                calendar.set(action.year, action.month, action.day)
                _activeDialog.value = NoteRemindersDialogState.TimePicker(reminder, calendar)
            }

            is NoteRemindersAction.TimePickerBack -> {
                val currentDialog = _activeDialog.value as? NoteRemindersDialogState.TimePicker
                val calendar = currentDialog?.calendar ?: now()
                val reminder = currentDialog?.reminder
                val utcCalendar =
                    Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        clear()
                        set(
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH),
                        )
                    }
                _activeDialog.value =
                    NoteRemindersDialogState.DatePicker(
                        reminder = reminder,
                        calendar = calendar,
                        initialDateMillis = utcCalendar.timeInMillis,
                    )
            }

            is NoteRemindersAction.TimeSelected -> {
                val currentDialog = _activeDialog.value as? NoteRemindersDialogState.TimePicker
                val calendar = action.calendar ?: currentDialog?.calendar ?: now()
                val reminder = action.reminder ?: currentDialog?.reminder
                calendar.set(Calendar.HOUR_OF_DAY, action.hour)
                calendar.set(Calendar.MINUTE, action.minute)
                showRepetitionDialog(reminder, calendar)
            }

            is NoteRemindersAction.RepetitionOptionSelected -> {
                val currentDialog = _activeDialog.value as? NoteRemindersDialogState.Repetition
                val calendar = action.calendar ?: currentDialog?.calendar ?: now()
                val reminder = action.reminder ?: currentDialog?.reminder
                _activeDialog.value = null
                when (action.option) {
                    RepetitionOption.NONE -> saveReminder(reminder, calendar, null)
                    RepetitionOption.DAILY ->
                        saveReminder(reminder, calendar, Repetition(1, RepetitionTimeUnit.DAYS))
                    RepetitionOption.WEEKLY ->
                        saveReminder(reminder, calendar, Repetition(1, RepetitionTimeUnit.WEEKS))
                    RepetitionOption.MONTHLY ->
                        showMonthlyAdvancedRepetitionDialog(reminder, calendar)
                    RepetitionOption.YEARLY ->
                        saveReminder(reminder, calendar, Repetition(1, RepetitionTimeUnit.YEARS))
                    RepetitionOption.CUSTOM -> showCustomRepetitionDialog(reminder, calendar)
                }
            }

            is NoteRemindersAction.MonthlyAdvancedSelected -> {
                val currentDialog = _activeDialog.value as? NoteRemindersDialogState.MonthlyAdvanced
                val calendar = action.calendar ?: currentDialog?.calendar ?: now()
                val reminder = action.reminder ?: currentDialog?.reminder
                _activeDialog.value = null
                val repetition =
                    when (action.which) {
                        0 -> Repetition(1, RepetitionTimeUnit.MONTHS)
                        1 -> Repetition(1, RepetitionTimeUnit.MONTHS, 1, action.dayOfWeek)
                        2 -> Repetition(1, RepetitionTimeUnit.MONTHS, 2, action.dayOfWeek)
                        3 -> Repetition(1, RepetitionTimeUnit.MONTHS, 3, action.dayOfWeek)
                        4 -> Repetition(1, RepetitionTimeUnit.MONTHS, 4, action.dayOfWeek)
                        5 -> Repetition(1, RepetitionTimeUnit.MONTHS, -1, action.dayOfWeek)
                        else -> Repetition(1, RepetitionTimeUnit.MONTHS)
                    }
                saveReminder(reminder, calendar, repetition)
            }

            is NoteRemindersAction.CustomRepetitionSelected -> {
                val currentDialog =
                    _activeDialog.value as? NoteRemindersDialogState.CustomRepetition
                val calendar = action.calendar ?: currentDialog?.calendar ?: now()
                val reminder = action.reminder ?: currentDialog?.reminder
                val targetUnit =
                    when (action.timeUnit) {
                        TimeUnit.MINUTES -> RepetitionTimeUnit.MINUTES
                        TimeUnit.HOURS -> RepetitionTimeUnit.HOURS
                        TimeUnit.DAYS -> RepetitionTimeUnit.DAYS
                        TimeUnit.WEEKS -> RepetitionTimeUnit.WEEKS
                        TimeUnit.MONTHS -> RepetitionTimeUnit.MONTHS
                        TimeUnit.YEARS -> RepetitionTimeUnit.YEARS
                    }
                if (targetUnit == RepetitionTimeUnit.MONTHS && action.value == 1) {
                    showMonthlyAdvancedRepetitionDialog(reminder, calendar)
                } else {
                    _activeDialog.value = null
                    saveReminder(reminder, calendar, Repetition(action.value, targetUnit))
                }
            }

            is NoteRemindersAction.ShowPermissionRationale -> {
                _activeDialog.value =
                    NoteRemindersDialogState.PermissionRationale(
                        messageResId = action.messageResId,
                        permissionType = action.permissionType,
                    )
            }

            is NoteRemindersAction.ConfirmPermissionRationale -> {
                _activeDialog.value = null
                viewModelScope.launch {
                    when (action.permissionType) {
                        PermissionType.NOTIFICATION -> {
                            _sideEffects.send(
                                NoteRemindersSideEffect.RequestNotificationPermission(
                                    pendingReminder,
                                    pendingCalendar,
                                )
                            )
                        }
                        PermissionType.ALARM -> {
                            _sideEffects.send(
                                NoteRemindersSideEffect.RequestAlarmPermission(
                                    pendingReminder,
                                    pendingCalendar,
                                )
                            )
                        }
                    }
                }
            }

            is NoteRemindersAction.PermissionResult -> {
                if (action.isGranted) {
                    if (action.permissionType == PermissionType.NOTIFICATION) {
                        if (app.canScheduleAlarms()) {
                            openDatePicker(pendingReminder, pendingCalendar)
                        } else {
                            viewModelScope.launch {
                                _sideEffects.send(
                                    NoteRemindersSideEffect.RequestAlarmPermission(
                                        pendingReminder,
                                        pendingCalendar,
                                    )
                                )
                            }
                        }
                    } else {
                        openDatePicker(pendingReminder, pendingCalendar)
                    }
                }
            }

            is NoteRemindersAction.CheckPermissionsResult -> {
                if (action.hasPermission) {
                    openDatePicker(pendingReminder, pendingCalendar)
                }
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val note = baseNoteDao?.get(id)
                val loadedReminders = note?.reminders ?: emptyList()
                _reminders.value = loadedReminders
                _isLoading.value = false
                if (loadedReminders.isEmpty()) {
                    checkPermissionsAndOpenDatePicker(null, null)
                } else if (!app.canScheduleAlarms()) {
                    _sideEffects.send(NoteRemindersSideEffect.CheckPermissions())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load note $id", e)
                _isLoading.value = false
            }
        }
    }

    private fun checkPermissionsAndOpenDatePicker(reminder: Reminder?, calendar: Calendar?) {
        pendingReminder = reminder
        pendingCalendar = calendar
        val hasNotification =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    app,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
        val hasAlarm = app.canScheduleAlarms()

        if (hasNotification && hasAlarm) {
            openDatePicker(reminder, calendar)
        } else {
            viewModelScope.launch {
                _sideEffects.send(NoteRemindersSideEffect.CheckPermissions(reminder, calendar))
            }
        }
    }

    private fun openDatePicker(reminder: Reminder?, calendar: Calendar?) {
        val usedCalendar = calendar ?: reminder?.dateTime?.toCalendar() ?: now()
        val utcCalendar =
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    usedCalendar.get(Calendar.YEAR),
                    usedCalendar.get(Calendar.MONTH),
                    usedCalendar.get(Calendar.DAY_OF_MONTH),
                )
            }
        _activeDialog.value =
            NoteRemindersDialogState.DatePicker(
                reminder = reminder,
                calendar = usedCalendar,
                initialDateMillis = utcCalendar.timeInMillis,
            )
    }

    private fun showRepetitionDialog(
        reminder: Reminder? = null,
        calendar: Calendar,
        fromCustomRepetitionDialog: Boolean = false,
    ) {
        val repetition = reminder?.repetition
        val initialOption =
            when {
                fromCustomRepetitionDialog -> RepetitionOption.CUSTOM
                repetition == null -> RepetitionOption.NONE
                repetition.value == 1 && repetition.unit == RepetitionTimeUnit.DAYS ->
                    RepetitionOption.DAILY
                repetition.value == 1 && repetition.unit == RepetitionTimeUnit.WEEKS ->
                    RepetitionOption.WEEKLY
                repetition.value == 1 && repetition.unit == RepetitionTimeUnit.MONTHS ->
                    RepetitionOption.MONTHLY
                repetition.value == 1 && repetition.unit == RepetitionTimeUnit.YEARS ->
                    RepetitionOption.YEARLY
                else -> RepetitionOption.CUSTOM
            }

        if (initialOption == RepetitionOption.CUSTOM && !fromCustomRepetitionDialog) {
            showCustomRepetitionDialog(reminder, calendar)
            return
        }

        _activeDialog.value = NoteRemindersDialogState.Repetition(reminder, calendar, initialOption)
    }

    private fun showCustomRepetitionDialog(reminder: Reminder? = null, calendar: Calendar) {
        val initialValue = reminder?.repetition?.value?.toString() ?: "1"
        val initialUnit =
            when (reminder?.repetition?.unit) {
                RepetitionTimeUnit.MINUTES -> TimeUnit.MINUTES
                RepetitionTimeUnit.HOURS -> TimeUnit.HOURS
                RepetitionTimeUnit.DAYS -> TimeUnit.DAYS
                RepetitionTimeUnit.WEEKS -> TimeUnit.WEEKS
                RepetitionTimeUnit.MONTHS -> TimeUnit.MONTHS
                RepetitionTimeUnit.YEARS -> TimeUnit.YEARS
                else -> TimeUnit.WEEKS
            }

        _activeDialog.value =
            NoteRemindersDialogState.CustomRepetition(
                reminder = reminder,
                calendar = calendar,
                initialValue = initialValue,
                initialUnit = initialUnit,
            )
    }

    private fun showMonthlyAdvancedRepetitionDialog(
        reminder: Reminder? = null,
        calendar: Calendar,
    ) {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val isLastDayOfMonth = dayOfMonth == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthlyOptionText =
            if (isLastDayOfMonth) {
                app.getString(
                    R.string.of_the_month_last,
                    app.getQuantityStringPlain(R.plurals.days, 1),
                )
            } else {
                "$dayOfMonth. ${app.getString(R.string.of_the_month, app.getQuantityStringPlain(R.plurals.days, 1))}"
            }

        val options =
            listOf(
                monthlyOptionText,
                "1. ${app.getString(R.string.of_the_month, dayOfWeekStr)}",
                "2. ${app.getString(R.string.of_the_month, dayOfWeekStr)}",
                "3. ${app.getString(R.string.of_the_month, dayOfWeekStr)}",
                "4. ${app.getString(R.string.of_the_month, dayOfWeekStr)}",
                app.getString(R.string.of_the_month_last, dayOfWeekStr),
            )

        var selectedIndex = 0
        reminder?.repetition?.let {
            if (it.unit == RepetitionTimeUnit.MONTHS && it.value == 1) {
                selectedIndex =
                    when (it.occurrence) {
                        null -> 0
                        1 -> 1
                        2 -> 2
                        3 -> 3
                        4 -> 4
                        -1 -> 5
                        else -> 0
                    }
            }
        }

        _activeDialog.value =
            NoteRemindersDialogState.MonthlyAdvanced(
                reminder = reminder,
                calendar = calendar,
                options = options,
                selectedIndex = selectedIndex,
                dayOfWeek = dayOfWeek,
            )
    }

    private fun saveReminder(reminder: Reminder?, calendar: Calendar, repetition: Repetition?) {
        val updatedReminder =
            Reminder(
                reminder?.id ?: NEW_REMINDER_ID,
                calendar.time,
                repetition,
            )
        if (reminder != null) {
            updateReminder(updatedReminder)
        } else {
            addReminder(updatedReminder)
        }
    }

    private fun deleteReminder(reminder: Reminder) {
        app.cancelReminder(noteId, reminder.id)
        val currentReminders = _reminders.value.filter { it.id != reminder.id }
        _reminders.value = currentReminders
        viewModelScope.launch(Dispatchers.IO) {
            try {
                baseNoteDao?.updateReminders(noteId, currentReminders)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete reminder from database", e)
            }
        }
    }

    private fun addReminder(reminder: Reminder) {
        val currentReminders = ArrayList(_reminders.value)
        val newReminder = reminder.copy(id = (currentReminders.maxOfOrNull { it.id } ?: -1) + 1)
        currentReminders.add(newReminder)
        _reminders.value = currentReminders
        viewModelScope.launch(Dispatchers.IO) {
            try {
                baseNoteDao?.updateReminders(noteId, currentReminders)
                app.scheduleReminder(noteId, newReminder)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add reminder to database", e)
            }
        }
    }

    private fun updateReminder(updatedReminder: Reminder) {
        if (updatedReminder.id != NEW_REMINDER_ID) {
            app.cancelReminder(noteId, updatedReminder.id)
        }
        val currentReminders = _reminders.value.toMutableList()
        val idx = currentReminders.indexOfFirst { it.id == updatedReminder.id }
        if (idx != -1) {
            currentReminders[idx] = updatedReminder
            _reminders.value = currentReminders
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    baseNoteDao?.updateReminders(noteId, currentReminders)
                    app.scheduleReminder(noteId, updatedReminder)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update reminder in database", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "NoteRemindersViewModel"
    }
}

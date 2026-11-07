package com.philkes.notallyx.feature.reminder

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TimePicker
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.now
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.extensions.checkAlarmPermission
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.createComposeDialog
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.extensions.getQuantityStringPlain
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.extensions.toCalendar
import com.philkes.notallyx.data.model.extensions.toText
import com.philkes.notallyx.feature.editor.NoteModel
import com.philkes.notallyx.feature.reminder.adapter.ReminderListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class RemindersActivity : LockedActivity<ComposeBinding>(), ReminderListener {

    private lateinit var alarmPermissionActivityResultLauncher: ActivityResultLauncher<Intent>
    private val model: NoteModel by viewModel()
    private var selectedReminder: Reminder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val composeView = ComposeView(this)
        binding = ComposeBinding(composeView)
        setContentView(composeView)

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                val remindersState = model.reminders.collectAsStateWithLifecycle(emptyList())

                RemindersScreen(
                    reminders = remindersState.value,
                    dateFormat = preferences.dateFormatNoteView.value,
                    timeFormat = preferences.timeFormatNoteView.value,
                    onAddReminderClick = { showDatePickerDialog() },
                    onEditReminder = { reminder -> edit(reminder) },
                    onDeleteReminder = { reminder -> delete(reminder) },
                    onBackClick = { finish() },
                )
            }
        }

        alarmPermissionActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (canScheduleAlarms()) {
                    showDatePickerDialog(selectedReminder)
                }
            }
        val noteId = intent.getLongExtra(NOTE_ID, 0L)
        lifecycleScope.launch {
            model.setState(noteId)
            if (model.reminders.value.isEmpty()) {
                showDatePickerDialog()
            } else if (!canScheduleAlarms()) {
                checkNotificationPermission(
                    REQUEST_NOTIFICATION_PERMISSION_REQUEST_CODE,
                    alsoCheckAlarmPermission = true,
                ) {}
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NOTIFICATION_PERMISSION_ON_OPEN_REQUEST_CODE -> {
                if (
                    grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    checkAlarmPermission(alarmPermissionActivityResultLauncher) {
                        showDatePickerDialog(selectedReminder)
                    }
                }
            }
        }
    }

    private fun showDatePickerDialog(reminder: Reminder? = null, calendar: Calendar? = null) {
        selectedReminder = reminder
        checkNotificationPermission(
            REQUEST_NOTIFICATION_PERMISSION_ON_OPEN_REQUEST_CODE,
            alsoCheckAlarmPermission = true,
            alarmPermissionResultLauncher = alarmPermissionActivityResultLauncher,
        ) {
            DatePickerFragment(calendar?.time ?: reminder?.dateTime) { year, month, day ->
                    val usedCalendar = calendar ?: reminder?.dateTime?.toCalendar() ?: now()
                    usedCalendar.set(year, month, day)
                    showTimePickerDialog(reminder, usedCalendar)
                }
                .show(supportFragmentManager, "reminderDatePicker")
        }
    }

    private fun showTimePickerDialog(reminder: Reminder? = null, calendar: Calendar) {
        TimePickerFragment(
                calendar,
                object : TimePickerListener {
                    override fun onBack() {
                        showDatePickerDialog(reminder, calendar)
                    }

                    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        showRepetitionDialog(reminder, calendar) { updatedRepetition ->
                            val updatedReminder =
                                Reminder(
                                    reminder?.id ?: NEW_REMINDER_ID,
                                    calendar.time,
                                    updatedRepetition,
                                )
                            if (reminder != null) {
                                lifecycleScope.launch { model.updateReminder(updatedReminder) }
                            } else {
                                lifecycleScope.launch { model.addReminder(updatedReminder) }
                            }
                        }
                    }
                },
            )
            .show(supportFragmentManager, "reminderTimePicker")
    }

    private fun showRepetitionDialog(
        reminder: Reminder? = null,
        calendar: Calendar,
        fromCustomRepetitionDialog: Boolean = false,
        onRepetitionSelected: (Repetition?) -> Unit,
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
            showCustomRepetitionDialog(reminder, calendar, onRepetitionSelected)
            return
        }

        val composeView = ComposeView(this)
        val dialog = createComposeDialog(this, composeView)

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                ReminderRepetitionDialog(
                    initialOption = initialOption,
                    onDismissRequest = { dialog.dismiss() },
                    onOptionSelected = { selected ->
                        dialog.dismiss()
                        when (selected) {
                            RepetitionOption.NONE -> onRepetitionSelected(null)
                            RepetitionOption.DAILY ->
                                onRepetitionSelected(Repetition(1, RepetitionTimeUnit.DAYS))
                            RepetitionOption.WEEKLY ->
                                onRepetitionSelected(Repetition(1, RepetitionTimeUnit.WEEKS))
                            RepetitionOption.MONTHLY ->
                                showMonthlyAdvancedRepetitionDialog(
                                    reminder,
                                    calendar,
                                    onRepetitionSelected,
                                )
                            RepetitionOption.YEARLY ->
                                onRepetitionSelected(Repetition(1, RepetitionTimeUnit.YEARS))
                            RepetitionOption.CUSTOM ->
                                showCustomRepetitionDialog(reminder, calendar, onRepetitionSelected)
                        }
                    },
                )
            }
        }
        dialog.show()
    }

    private fun showMonthlyAdvancedRepetitionDialog(
        reminder: Reminder? = null,
        calendar: Calendar,
        onRepetitionSelected: (Repetition?) -> Unit,
    ) {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val isLastDayOfMonth = dayOfMonth == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthlyOptionText =
            if (isLastDayOfMonth) {
                getString(R.string.of_the_month_last, getQuantityStringPlain(R.plurals.days, 1))
            } else {
                "$dayOfMonth. ${getString(R.string.of_the_month, getQuantityStringPlain(R.plurals.days, 1))}"
            }

        val options =
            listOf(
                monthlyOptionText,
                "1. ${getString(R.string.of_the_month, dayOfWeekStr)}",
                "2. ${getString(R.string.of_the_month, dayOfWeekStr)}",
                "3. ${getString(R.string.of_the_month, dayOfWeekStr)}",
                "4. ${getString(R.string.of_the_month, dayOfWeekStr)}",
                getString(R.string.of_the_month_last, dayOfWeekStr),
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

        val composeView = ComposeView(this)
        val dialog = createComposeDialog(this, composeView)

        val items = options.mapIndexed { index, option ->
            com.philkes.notallyx.core.ui.dialog.SelectionItem(index, option)
        }

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                com.philkes.notallyx.core.ui.dialog.SingleSelectionDialog(
                    title = stringResource(R.string.monthly),
                    items = items,
                    selectedItem = selectedIndex,
                    onDismissRequest = {
                        dialog.dismiss()
                        showRepetitionDialog(
                            reminder,
                            calendar,
                            fromCustomRepetitionDialog = true,
                            onRepetitionSelected = onRepetitionSelected,
                        )
                    },
                    onConfirm = { which ->
                        val repetition =
                            when (which) {
                                0 -> Repetition(1, RepetitionTimeUnit.MONTHS)
                                1 -> Repetition(1, RepetitionTimeUnit.MONTHS, 1, dayOfWeek)
                                2 -> Repetition(1, RepetitionTimeUnit.MONTHS, 2, dayOfWeek)
                                3 -> Repetition(1, RepetitionTimeUnit.MONTHS, 3, dayOfWeek)
                                4 -> Repetition(1, RepetitionTimeUnit.MONTHS, 4, dayOfWeek)
                                5 -> Repetition(1, RepetitionTimeUnit.MONTHS, -1, dayOfWeek)
                                else -> Repetition(1, RepetitionTimeUnit.MONTHS)
                            }
                        onRepetitionSelected(repetition)
                        dialog.dismiss()
                    },
                )
            }
        }
        dialog.show()
    }

    private fun showCustomRepetitionDialog(
        reminder: Reminder? = null,
        calendar: Calendar,
        onRepetitionSelected: (Repetition?) -> Unit,
    ) {
        val composeView = ComposeView(this)
        val dialog = createComposeDialog(this, composeView)

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

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                CustomRepetitionDialog(
                    initialValue = initialValue,
                    initialTimeUnit = initialUnit,
                    onDismissRequest = {
                        dialog.dismiss()
                        showRepetitionDialog(
                            reminder,
                            calendar,
                            fromCustomRepetitionDialog = true,
                            onRepetitionSelected = onRepetitionSelected,
                        )
                    },
                    onConfirm = { valInt, unitEnum ->
                        dialog.dismiss()
                        val targetUnit =
                            when (unitEnum) {
                                TimeUnit.MINUTES -> RepetitionTimeUnit.MINUTES
                                TimeUnit.HOURS -> RepetitionTimeUnit.HOURS
                                TimeUnit.DAYS -> RepetitionTimeUnit.DAYS
                                TimeUnit.WEEKS -> RepetitionTimeUnit.WEEKS
                                TimeUnit.MONTHS -> RepetitionTimeUnit.MONTHS
                                TimeUnit.YEARS -> RepetitionTimeUnit.YEARS
                            }
                        if (targetUnit == RepetitionTimeUnit.MONTHS && valInt == 1) {
                            showMonthlyAdvancedRepetitionDialog(
                                reminder,
                                calendar,
                                onRepetitionSelected,
                            )
                        } else {
                            onRepetitionSelected(Repetition(valInt, targetUnit))
                        }
                    },
                )
            }
        }
        dialog.show()
    }

    private fun confirmDeletion(reminder: Reminder) {
        val composeView = ComposeView(this)
        val dialog = createComposeDialog(this, composeView)

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                com.philkes.notallyx.core.ui.dialog.ConfirmDialog(
                    title = stringResource(R.string.delete_reminder_question),
                    message =
                        "${reminder.dateTime.format(
                            preferences.dateFormatNoteView.value,
                            preferences.timeFormatNoteView.value,
                            ensureFullFormat = true,
                        )}\n${reminder.repetition?.toText(this@RemindersActivity) ?: stringResource(R.string.reminder_no_repetition)}",
                    positiveButtonText = stringResource(R.string.delete),
                    onDismissRequest = { dialog.dismiss() },
                    onConfirm = { lifecycleScope.launch { model.removeReminder(reminder) } },
                )
            }
        }
        dialog.show()
    }

    override fun delete(reminder: Reminder) {
        confirmDeletion(reminder)
    }

    override fun edit(reminder: Reminder) {
        showDatePickerDialog(reminder)
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION_ON_OPEN_REQUEST_CODE = 101
        private const val REQUEST_NOTIFICATION_PERMISSION_REQUEST_CODE = 102
        const val NOTE_ID = "NOTE_ID"
        const val NEW_REMINDER_ID = -1L
    }
}

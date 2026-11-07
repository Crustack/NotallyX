package com.philkes.notallyx.feature.reminder

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.widget.Button
import android.widget.RadioButton
import android.widget.TimePicker
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.now
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.extensions.checkAlarmPermission
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.extensions.getQuantityStringPlain
import com.philkes.notallyx.core.ui.extensions.setCancelButton
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
        val composeView = ComposeView(this)
        val dialog = MaterialAlertDialogBuilder(this).setView(composeView).create()

        val repetition = reminder?.repetition
        val initialOption =
            when {
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
                                onRepetitionSelected(Repetition(1, RepetitionTimeUnit.MONTHS))
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
            arrayOf(
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

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.monthly)
            .setSingleChoiceItems(options, selectedIndex) { dialog, which ->
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
            }
            .setNegativeButton(R.string.back) { dialog, _ ->
                dialog.dismiss()
                showRepetitionDialog(
                    reminder,
                    calendar,
                    onRepetitionSelected = onRepetitionSelected,
                )
            }
            .show()
    }

    private fun showCustomRepetitionDialog(
        reminder: Reminder? = null,
        calendar: Calendar,
        onRepetitionSelected: (Repetition?) -> Unit,
    ) {
        val composeView = ComposeView(this)
        val dialog = MaterialAlertDialogBuilder(this).setView(composeView).create()

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
                    onDismissRequest = { dialog.dismiss() },
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
                        onRepetitionSelected(Repetition(valInt, targetUnit))
                    },
                )
            }
        }
        dialog.show()
    }

    private fun RadioButton.setOnCheckedEnableButton(button: Button) {
        setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                button.isEnabled = true
            }
        }
    }

    private fun Editable?.hasValueBiggerZero() =
        (!isNullOrEmpty() && toString().toIntOrNull()?.let { it > 0 } ?: false)

    private fun confirmDeletion(reminder: Reminder) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_reminder_question)
            .setMessage(
                "${reminder.dateTime.format(
                    preferences.dateFormatNoteView.value,
                    preferences.timeFormatNoteView.value,
                    ensureFullFormat = true,)}\n${reminder.repetition?.toText(this) ?: getString(R.string.reminder_no_repetition)}"
            )
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch { model.removeReminder(reminder) }
            }
            .setCancelButton()
            .show()
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

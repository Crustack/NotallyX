package com.philkes.notallyx.presentation.activity.note.reminders

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.TimePicker
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.ui.platform.ComposeView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.toCalendar
import com.philkes.notallyx.data.model.toText
import com.philkes.notallyx.databinding.DialogReminderCustomRepetitionBinding
import com.philkes.notallyx.databinding.DialogReminderRepetitionBinding
import com.philkes.notallyx.presentation.activity.ComposeBinding
import com.philkes.notallyx.presentation.activity.LockedActivity
import com.philkes.notallyx.presentation.checkAlarmPermission
import com.philkes.notallyx.presentation.checkNotificationPermission
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.presentation.getQuantityStringPlain
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.showAndFocus
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.main.reminder.ReminderListener
import com.philkes.notallyx.presentation.view.screens.RemindersScreen
import com.philkes.notallyx.presentation.viewmodel.edit.NoteModel
import com.philkes.notallyx.utils.canScheduleAlarms
import com.philkes.notallyx.utils.now
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
        val dialogView =
            DialogReminderRepetitionBinding.inflate(layoutInflater).apply {
                if (reminder == null && fromCustomRepetitionDialog) {
                    None.isChecked = true
                } else {
                    reminder?.repetition.apply {
                        when {
                            this == null -> None.isChecked = true
                            value == 1 && unit == RepetitionTimeUnit.DAYS -> Daily.isChecked = true

                            value == 1 && unit == RepetitionTimeUnit.WEEKS ->
                                Weekly.isChecked = true

                            value == 1 && unit == RepetitionTimeUnit.MONTHS && occurrence == null ->
                                Monthly.isChecked = true

                            value == 1 &&
                                unit == RepetitionTimeUnit.MONTHS &&
                                occurrence != null -> {
                                MonthlyAdvanced.apply {
                                    visibility = View.VISIBLE
                                    text = toText(this@RemindersActivity)
                                    isChecked = true
                                }
                            }

                            value == 1 && unit == RepetitionTimeUnit.YEARS ->
                                Yearly.isChecked = true

                            fromCustomRepetitionDialog -> Custom.isChecked = true
                            else -> {
                                showCustomRepetitionDialog(
                                    reminder,
                                    calendar,
                                    onRepetitionSelected,
                                )
                                return
                            }
                        }
                    }
                }
            }

        val dialog =
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.repetition)
                .setView(dialogView.root)
                .setPositiveButton(R.string.save) { _, _ ->
                    val repetition =
                        when (dialogView.RepetitionOptions.checkedRadioButtonId) {
                            R.id.None -> null
                            R.id.Daily -> Repetition(1, RepetitionTimeUnit.DAYS)
                            R.id.Weekly -> Repetition(1, RepetitionTimeUnit.WEEKS)
                            R.id.Monthly -> Repetition(1, RepetitionTimeUnit.MONTHS)
                            R.id.Yearly -> Repetition(1, RepetitionTimeUnit.YEARS)
                            R.id.MonthlyAdvanced -> reminder?.repetition?.copy()
                            R.id.Custom -> reminder?.repetition?.copy()
                            else -> null
                        }
                    onRepetitionSelected(repetition)
                }
                .setNegativeButton(R.string.back) { _, _ ->
                    showTimePickerDialog(reminder, calendar)
                }
                .show()
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        dialogView.apply {
            Custom.setOnClickListener {
                dialog.dismiss()
                showCustomRepetitionDialog(reminder, calendar, onRepetitionSelected)
            }
            None.setOnCheckedEnableButton(positiveButton)
            Daily.setOnCheckedEnableButton(positiveButton)
            Weekly.setOnCheckedEnableButton(positiveButton)
            Monthly.setOnClickListener {
                dialog.dismiss()
                showMonthlyAdvancedRepetitionDialog(reminder, calendar, onRepetitionSelected)
            }
            MonthlyAdvanced.setOnClickListener {
                dialog.dismiss()
                showMonthlyAdvancedRepetitionDialog(reminder, calendar, onRepetitionSelected)
            }
            Yearly.setOnCheckedEnableButton(positiveButton)
        }
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
        val dialogView =
            DialogReminderCustomRepetitionBinding.inflate(layoutInflater).apply {
                reminder?.repetition?.let {
                    when (it.unit) {
                        RepetitionTimeUnit.MINUTES -> Minutes
                        RepetitionTimeUnit.HOURS -> Hours
                        RepetitionTimeUnit.DAYS -> Days
                        RepetitionTimeUnit.WEEKS -> Weeks
                        RepetitionTimeUnit.MONTHS -> Months
                        RepetitionTimeUnit.YEARS -> Years
                    }.isChecked = true
                    Value.setText(it.value.toString())
                }
            }

        val dialog =
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.repetition_custom)
                .setView(dialogView.root)
                .setPositiveButton(R.string.save) { _, _ ->
                    val value = dialogView.Value.text.toString().toIntOrNull() ?: 1
                    val selectedTimeUnit =
                        when (dialogView.TimeUnitGroup.checkedRadioButtonId) {
                            R.id.Minutes -> RepetitionTimeUnit.MINUTES
                            R.id.Hours -> RepetitionTimeUnit.HOURS
                            R.id.Days -> RepetitionTimeUnit.DAYS
                            R.id.Weeks -> RepetitionTimeUnit.WEEKS
                            R.id.Months -> RepetitionTimeUnit.MONTHS
                            R.id.Years -> RepetitionTimeUnit.YEARS
                            else -> null
                        }
                    onRepetitionSelected(
                        selectedTimeUnit?.let {
                            if (it == RepetitionTimeUnit.MONTHS && value == 1) {
                                showMonthlyAdvancedRepetitionDialog(
                                    reminder,
                                    calendar,
                                    onRepetitionSelected,
                                )
                                return@setPositiveButton
                            }
                            Repetition(value, it)
                        }
                    )
                }
                .setBackgroundInsetBottom(0)
                .setBackgroundInsetTop(0)
                .setNegativeButton(R.string.back) { dialog, _ ->
                    dialog.dismiss()
                    showRepetitionDialog(
                        reminder,
                        calendar,
                        fromCustomRepetitionDialog = true,
                        onRepetitionSelected,
                    )
                }
                .showAndFocus(dialogView.Value, allowFullSize = true)
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        dialogView.Value.doAfterTextChanged { text ->
            positiveButton.isEnabled = text.hasValueBiggerZero()
        }
        positiveButton.isEnabled = reminder?.repetition != null
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

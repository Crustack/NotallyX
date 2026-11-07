package com.philkes.notallyx.feature.reminder

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.now
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.checkAlarmPermission
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.core.ui.extensions.getQuantityStringPlain
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.extensions.toCalendar
import com.philkes.notallyx.data.model.extensions.toText
import com.philkes.notallyx.feature.editor.NoteModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
fun NoteRemindersRoute(
    noteId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    model: NoteModel = koinViewModel(),
    preferences: NotallyXPreferences = koinInject(),
) {
    val context = LocalContext.current
    val activity = remember(context) { requireNotNull(context.findActivity()) }
    val scope = rememberCoroutineScope()

    val handleBack: () -> Unit = { onBackClick() }
    BackHandler(onBack = handleBack)

    val reminders by model.reminders.collectAsStateWithLifecycle(emptyList())
    var dialogState by remember { mutableStateOf<ReminderDialogState?>(null) }
    var selectedReminder by remember { mutableStateOf<Reminder?>(null) }

    fun canScheduleAlarms(): Boolean = context.canScheduleAlarms()

    val alarmPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (canScheduleAlarms()) {
                selectedReminder?.let { r ->
                    val usedCalendar = r.dateTime.toCalendar()
                    val utcCalendar =
                        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            clear()
                            set(
                                usedCalendar.get(Calendar.YEAR),
                                usedCalendar.get(Calendar.MONTH),
                                usedCalendar.get(Calendar.DAY_OF_MONTH),
                            )
                        }
                    dialogState =
                        ReminderDialogState.DatePicker(
                            reminder = r,
                            calendar = usedCalendar,
                            initialDateMillis = utcCalendar.timeInMillis,
                        )
                }
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            if (isGranted) {
                activity?.checkAlarmPermission(
                    alarmPermissionLauncher,
                    onShowRationale = { msgResId, onConfirm ->
                        dialogState =
                            ReminderDialogState.Rationale(
                                activity?.getString(msgResId) ?: "",
                                onConfirm,
                            )
                    },
                ) {
                    selectedReminder?.let { r ->
                        val usedCalendar = r.dateTime.toCalendar()
                        val utcCalendar =
                            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                clear()
                                set(
                                    usedCalendar.get(Calendar.YEAR),
                                    usedCalendar.get(Calendar.MONTH),
                                    usedCalendar.get(Calendar.DAY_OF_MONTH),
                                )
                            }
                        dialogState =
                            ReminderDialogState.DatePicker(
                                reminder = r,
                                calendar = usedCalendar,
                                initialDateMillis = utcCalendar.timeInMillis,
                            )
                    }
                }
            }
        }

    fun showDatePickerDialog(reminder: Reminder? = null, calendar: Calendar? = null) {
        selectedReminder = reminder
        activity?.checkNotificationPermission(
            permissionLauncher = notificationPermissionLauncher,
            alsoCheckAlarmPermission = true,
            alarmPermissionResultLauncher = alarmPermissionLauncher,
            onShowRationale = { msgResId, onConfirm ->
                dialogState =
                    ReminderDialogState.Rationale(
                        activity?.getString(msgResId) ?: "",
                        onConfirm,
                    )
            },
        ) {
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
            dialogState =
                ReminderDialogState.DatePicker(
                    reminder = reminder,
                    calendar = usedCalendar,
                    initialDateMillis = utcCalendar.timeInMillis,
                )
        }
    }

    LaunchedEffect(noteId) {
        model.setState(noteId)
        if (model.reminders.value.isEmpty()) {
            showDatePickerDialog()
        } else if (!canScheduleAlarms()) {
            activity?.checkNotificationPermission(
                permissionLauncher = notificationPermissionLauncher,
                alsoCheckAlarmPermission = true,
                alarmPermissionResultLauncher = alarmPermissionLauncher,
                onShowRationale = { msgResId, onConfirm ->
                    dialogState =
                        ReminderDialogState.Rationale(
                            activity?.getString(msgResId) ?: "",
                            onConfirm,
                        )
                },
            ) {}
        }
    }

    fun showTimePickerDialog(reminder: Reminder? = null, calendar: Calendar) {
        dialogState = ReminderDialogState.TimePicker(reminder = reminder, calendar = calendar)
    }

    fun showCustomRepetitionDialog(reminder: Reminder? = null, calendar: Calendar) {
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

        dialogState =
            ReminderDialogState.CustomRepetition(
                reminder = reminder,
                calendar = calendar,
                initialValue = initialValue,
                initialUnit = initialUnit,
            )
    }

    fun showMonthlyAdvancedRepetitionDialog(reminder: Reminder? = null, calendar: Calendar) {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val isLastDayOfMonth = dayOfMonth == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthlyOptionText =
            if (isLastDayOfMonth) {
                activity.getString(
                    R.string.of_the_month_last,
                    activity.getQuantityStringPlain(R.plurals.days, 1),
                )
            } else {
                "$dayOfMonth. ${activity.getString(R.string.of_the_month, activity.getQuantityStringPlain(R.plurals.days, 1))}"
            }

        val options =
            listOf(
                monthlyOptionText,
                "1. ${activity.getString(R.string.of_the_month, dayOfWeekStr)}",
                "2. ${activity.getString(R.string.of_the_month, dayOfWeekStr)}",
                "3. ${activity.getString(R.string.of_the_month, dayOfWeekStr)}",
                "4. ${activity.getString(R.string.of_the_month, dayOfWeekStr)}",
                activity.getString(R.string.of_the_month_last, dayOfWeekStr),
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

        dialogState =
            ReminderDialogState.MonthlyAdvanced(
                reminder = reminder,
                calendar = calendar,
                options = options,
                selectedIndex = selectedIndex,
                dayOfWeek = dayOfWeek,
            )
    }

    fun saveReminder(reminder: Reminder?, calendar: Calendar, repetition: Repetition?) {
        val updatedReminder =
            Reminder(
                reminder?.id ?: NEW_REMINDER_ID,
                calendar.time,
                repetition,
            )
        if (reminder != null) {
            scope.launch { model.updateReminder(updatedReminder) }
        } else {
            scope.launch { model.addReminder(updatedReminder) }
        }
    }

    fun showRepetitionDialog(
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

        dialogState = ReminderDialogState.Repetition(reminder, calendar, initialOption)
    }

    fun confirmDeletion(reminder: Reminder) {
        val formattedDateTime =
            reminder.dateTime.format(
                preferences.dateFormatNoteView.value,
                preferences.timeFormatNoteView.value,
                ensureFullFormat = true,
            )
        val repetitionText =
            reminder.repetition?.toText(context)
                ?: activity.getString(R.string.reminder_no_repetition)

        dialogState =
            ReminderDialogState.ConfirmDelete(
                reminder = reminder,
                formattedDateTime = formattedDateTime,
                repetitionText = repetitionText,
            )
    }

    NoteRemindersScreen(
        reminders = reminders,
        dateFormat = preferences.dateFormatNoteView.value,
        timeFormat = preferences.timeFormatNoteView.value,
        modifier = modifier,
        dialogState = dialogState,
        onAddReminderClick = { showDatePickerDialog() },
        onEditReminder = { reminder -> showDatePickerDialog(reminder) },
        onDeleteReminder = { reminder -> confirmDeletion(reminder) },
        onBackClick = onBackClick,
        onDismissDialog = { dialogState = null },
        onDateSelected = { reminder, calendar, year, month, day ->
            calendar.set(year, month, day)
            showTimePickerDialog(reminder, calendar)
        },
        onTimeSelected = { reminder, calendar, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            showRepetitionDialog(reminder, calendar)
        },
        onTimePickerBack = { reminder, calendar -> showDatePickerDialog(reminder, calendar) },
        onRepetitionOptionSelected = { reminder, calendar, selected ->
            dialogState = null
            when (selected) {
                RepetitionOption.NONE -> saveReminder(reminder, calendar, null)
                RepetitionOption.DAILY ->
                    saveReminder(reminder, calendar, Repetition(1, RepetitionTimeUnit.DAYS))
                RepetitionOption.WEEKLY ->
                    saveReminder(reminder, calendar, Repetition(1, RepetitionTimeUnit.WEEKS))
                RepetitionOption.MONTHLY -> showMonthlyAdvancedRepetitionDialog(reminder, calendar)
                RepetitionOption.YEARLY ->
                    saveReminder(reminder, calendar, Repetition(1, RepetitionTimeUnit.YEARS))
                RepetitionOption.CUSTOM -> showCustomRepetitionDialog(reminder, calendar)
            }
        },
        onMonthlyAdvancedSelected = { reminder, calendar, which, dayOfWeek ->
            dialogState = null
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
            saveReminder(reminder, calendar, repetition)
        },
        onCustomRepetitionSelected = { reminder, calendar, valInt, unitEnum ->
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
                showMonthlyAdvancedRepetitionDialog(reminder, calendar)
            } else {
                dialogState = null
                saveReminder(reminder, calendar, Repetition(valInt, targetUnit))
            }
        },
        onConfirmDeleteReminder = { reminder ->
            dialogState = null
            scope.launch { model.removeReminder(reminder) }
        },
    )
}

const val NOTE_ID = "NOTE_ID"
const val NEW_REMINDER_ID = -1L

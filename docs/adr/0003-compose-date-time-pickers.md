# Migrate Date and Time Pickers to Jetpack Compose

Migrate reminder date and time selection from View-based `MaterialDatePicker` and `MaterialTimePicker` (`DatePickerFragment.kt` and `TimePickerFragment.kt`) to Jetpack Compose Material 3 `DatePickerDialog` and custom `TimePickerDialog` wrapping `TimePicker`. Date and time dialog states are managed directly in `RemindersScreen` via `ReminderDialogState` (`ReminderDialogState.DatePicker` and `ReminderDialogState.TimePicker`), enabling full removal of `DialogFragment` subclasses and unifying all reminder configuration dialogs in Compose state.

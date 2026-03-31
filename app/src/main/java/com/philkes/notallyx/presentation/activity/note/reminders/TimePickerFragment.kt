package com.philkes.notallyx.presentation.activity.note.reminders

import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import java.util.Calendar

class TimePickerFragment(private val calendar: Calendar, private val listener: TimePickerListener) :
    DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val is24Hour = DateFormat.is24HourFormat(requireContext())

        val timePicker =
            MaterialTimePicker.Builder()
                .setTimeFormat(if (is24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                .setInputMode(INPUT_MODE_CLOCK)
                .setHour(hour)
                .setMinute(minute)
                .build()

        timePicker.addOnPositiveButtonClickListener {
            listener.onTimeSet(null, timePicker.hour, timePicker.minute)
        }
        timePicker.addOnNegativeButtonClickListener {
            listener.onBack()
            dismiss()
        }
        timePicker.addOnCancelListener { dismiss() }

        timePicker.show(parentFragmentManager, "TimePicker")
        dismiss()
    }
}

interface TimePickerListener {
    fun onTimeSet(view: android.widget.TimePicker?, hourOfDay: Int, minute: Int)

    fun onBack()
}

package com.philkes.notallyx.presentation.view.main.reminder

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.toRepetitionText
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.components.ReminderChipItem
import com.philkes.notallyx.presentation.viewmodel.preference.DateFormat
import com.philkes.notallyx.presentation.viewmodel.preference.TimeFormat

class ReminderVH(
    private val composeView: ComposeView,
    private val dateFormat: DateFormat,
    private val timeFormat: TimeFormat,
    private val listener: ReminderListener,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(value: Reminder) {
        val dateTimeText = value.dateTime.format(dateFormat, timeFormat, ensureFullFormat = true)
        val repetitionText = value.toRepetitionText(composeView.context)

        composeView.setContent {
            NotallyXTheme {
                ReminderChipItem(
                    dateTimeText = dateTimeText,
                    repetitionText = repetitionText,
                    onEdit = { listener.edit(value) },
                    onDelete = { listener.delete(value) },
                )
            }
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            dateFormat: DateFormat,
            timeFormat: TimeFormat,
            listener: ReminderListener,
        ): ReminderVH {
            val composeView =
                ComposeView(parent.context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }
            return ReminderVH(composeView, dateFormat, timeFormat, listener)
        }
    }
}

package com.philkes.notallyx.utils

import android.graphics.Paint
import android.view.View.GONE
import android.view.View.VISIBLE
import com.google.android.material.chip.Chip
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.findNextNotificationDate
import com.philkes.notallyx.presentation.format
import java.util.Date

fun setupReminderChip(baseNote: BaseNote, reminderChip: Chip) {
    val now = Date(System.currentTimeMillis())
    val mostRecentNotificationDate =
        baseNote.reminders.findNextNotificationDate()
            ?: baseNote.reminders.maxOfOrNull { it.dateTime }
    if (mostRecentNotificationDate == null) {
        reminderChip.visibility = GONE
        return
    }
    reminderChip.apply {
        visibility = VISIBLE
        text = mostRecentNotificationDate.format()
        setCloseIconVisible(haveAnyRepetition(baseNote.reminders))
        setChipBackgroundColorResource(R.color.md_theme_secondaryContainer)
        val isElapsed = mostRecentNotificationDate < now
        alpha = if (isElapsed) 0.5f else 1.0f
        paintFlags =
            if (isElapsed) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}

private fun haveAnyRepetition(reminders: List<Reminder>) = reminders.any { it.repetition != null }

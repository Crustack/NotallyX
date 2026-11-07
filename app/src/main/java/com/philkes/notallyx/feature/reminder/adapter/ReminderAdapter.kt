package com.philkes.notallyx.feature.reminder.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.Reminder

class ReminderAdapter(
    private val dateFormat: DateFormat,
    private val timeFormat: TimeFormat,
    private val listener: ReminderListener,
) : ListAdapter<Reminder, ReminderVH>(ReminderDiffCallback()) {

    override fun onBindViewHolder(holder: ReminderVH, position: Int) {
        val reminder = getItem(position)
        holder.bind(reminder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderVH {
        return ReminderVH.create(parent, dateFormat, timeFormat, listener)
    }
}

interface ReminderListener {
    fun delete(reminder: Reminder)

    fun edit(reminder: Reminder)
}

class ReminderDiffCallback : DiffUtil.ItemCallback<Reminder>() {

    override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
        return oldItem == newItem
    }
}

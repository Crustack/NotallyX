package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.hasAnyUpcomingNotifications

class RemindersFragment : NotallyFragment() {
    private val currentReminderNotes = MutableLiveData<List<Item>>()
    private val allReminderNotes: LiveData<List<Item>> by lazy { model.reminderNotes!! }
    private var filterMode = FilterOptions.ALL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentReminderNotes.value = allReminderNotes.value
        binding?.ReminderFilter?.visibility = View.VISIBLE
        allReminderNotes.observe(viewLifecycleOwner) { _ -> applyFilter(filterMode) }
        binding?.ReminderFilter?.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) {
                binding?.ReminderFilter?.check(R.id.all)
                return@setOnCheckedStateChangeListener
            }
            filterMode =
                when (checkedIds.first()) {
                    R.id.elapsed -> FilterOptions.ELAPSED
                    R.id.upcoming -> FilterOptions.UPCOMING
                    else -> FilterOptions.ALL
                }
            applyFilter(filterMode)
        }
    }

    override fun getBackground(): Int = R.drawable.notifications

    override fun getObservable(): LiveData<List<Item>> = currentReminderNotes

    fun applyFilter(filterOptions: FilterOptions) {
        val items: List<Item> = allReminderNotes.value ?: return
        val filteredList: List<Item> =
            when (filterOptions) {
                FilterOptions.ALL -> {
                    items
                }
                FilterOptions.UPCOMING -> {
                    items.filter { it is BaseNote && it.reminders.hasAnyUpcomingNotifications() }
                }
                FilterOptions.ELAPSED -> {
                    items.filter { it is BaseNote && !it.reminders.hasAnyUpcomingNotifications() }
                }
            }
        currentReminderNotes.value = filteredList
    }
}

enum class FilterOptions {
    ALL,
    UPCOMING,
    ELAPSED,
}

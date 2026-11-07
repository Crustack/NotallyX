package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.hasAnyUpcomingNotifications
import com.philkes.notallyx.presentation.repeatOnLifecycleScope
import com.philkes.notallyx.presentation.view.main.BaseNoteAdapter
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteLastNotificationSort
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteMostRecentNotificationSort
import com.philkes.notallyx.presentation.view.main.sorting.BaseNoteNextNotificationSort
import com.philkes.notallyx.presentation.viewmodel.main.fragment.RemindersViewModel
import com.philkes.notallyx.presentation.viewmodel.preference.SortDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class RemindersFragment : NotesFragment() {
    private val remindersViewModel: RemindersViewModel by viewModel()
    private val currentReminderNotes = MutableStateFlow<List<Item>>(emptyList())
    private val allReminderNotes: StateFlow<List<Item>> by lazy { remindersViewModel.reminderNotes }
    val filterModeFlow = MutableStateFlow(FilterOptions.UPCOMING)
    val filterMode: FilterOptions
        get() = filterModeFlow.value

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentReminderNotes.value = allReminderNotes.value
        viewLifecycleOwner.repeatOnLifecycleScope {
            launch { allReminderNotes.collect { applyFilter(filterMode) } }
        }
    }

    override fun getBackgroundResId(): Int = R.drawable.notifications

    override fun getFlow(): Flow<List<Item>> = currentReminderNotes

    override fun notesAdapterSortCallback(): (BaseNoteAdapter) -> SortedListAdapterCallback<Item> =
        { adapter ->
            when (filterMode) {
                FilterOptions.UPCOMING -> BaseNoteNextNotificationSort(adapter, SortDirection.ASC)
                FilterOptions.ELAPSED -> BaseNoteLastNotificationSort(adapter, SortDirection.DESC)
                FilterOptions.ALL -> BaseNoteMostRecentNotificationSort(adapter, SortDirection.DESC)
            }
        }

    fun applyFilter(filterOptions: FilterOptions) {
        filterModeFlow.value = filterOptions
        val items: List<Item> = allReminderNotes.value
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
        notesAdapter?.setNotesSortCallback(notesAdapterSortCallback())
    }
}

enum class FilterOptions {
    UPCOMING,
    ELAPSED,
    ALL,
}

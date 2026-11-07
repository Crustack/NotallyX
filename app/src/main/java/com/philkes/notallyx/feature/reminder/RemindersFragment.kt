package com.philkes.notallyx.feature.reminder

import android.os.Bundle
import android.view.View
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.extensions.createItemsFromNotes
import com.philkes.notallyx.core.ui.extensions.repeatOnLifecycleScope
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.extensions.hasAnyUpcomingNotifications
import com.philkes.notallyx.feature.main.sorting.compareLastNotification
import com.philkes.notallyx.feature.main.sorting.compareNextNotification
import com.philkes.notallyx.feature.main.tab.NotesFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class RemindersFragment : NotesFragment() {
    private val remindersViewModel: RemindersViewModel by viewModel()
    private val currentReminderNotes = MutableStateFlow<List<Item>>(emptyList())
    private val allReminderNotes: StateFlow<List<BaseNote>> by lazy {
        remindersViewModel.reminderNotes
    }
    val filterModeFlow = MutableStateFlow(FilterOptions.UPCOMING)
    val filterMode: FilterOptions
        get() = filterModeFlow.value

    private var allNotes: List<BaseNote> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.repeatOnLifecycleScope {
            launch {
                allReminderNotes.collect { notes ->
                    allNotes = notes
                    applyFilter(filterMode)
                }
            }
        }
    }

    override fun getBackgroundResId(): Int = R.drawable.notifications

    override fun getFlow(): Flow<List<Item>> = currentReminderNotes

    fun applyFilter(filterOptions: FilterOptions) {
        filterModeFlow.value = filterOptions
        val filtered =
            when (filterOptions) {
                FilterOptions.ALL ->
                    allNotes.sortedWith { n1, n2 ->
                        val sort =
                            n1.compareNextNotification(n2).takeIf { it != 0 }
                                ?: n1.compareLastNotification(n2)
                        -1 * sort
                    }
                FilterOptions.UPCOMING ->
                    allNotes
                        .filter { it.reminders.hasAnyUpcomingNotifications() }
                        .sortedWith { n1, n2 -> n1.compareNextNotification(n2) }
                FilterOptions.ELAPSED ->
                    allNotes
                        .filter { !it.reminders.hasAnyUpcomingNotifications() }
                        .sortedWith { n1, n2 -> -1 * n1.compareLastNotification(n2) }
            }
        currentReminderNotes.value = requireContext().createItemsFromNotes(filtered)
    }
}

enum class FilterOptions {
    UPCOMING,
    ELAPSED,
    ALL,
}

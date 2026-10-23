package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.SearchDate
import com.philkes.notallyx.data.model.isEmpty
import com.philkes.notallyx.presentation.hideKeyboard
import com.philkes.notallyx.presentation.repeatOnLifecycleScope
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.viewmodel.preference.NotesSortBy
import com.philkes.notallyx.utils.findDateJumpPosition
import kotlinx.coroutines.launch

class SearchFragment : NotesFragment() {

    private var selectedDate: SearchDate? = null
    private var pendingDateJump: SearchDate? = null
    private var submittedItems: List<Item>? = null
    private val dateJumpTask = Runnable { jumpToPendingDate() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate =
            if (savedInstanceState?.containsKey(STATE_SELECTED_DATE) == true) {
                SearchDate.fromUtcMillis(savedInstanceState.getLong(STATE_SELECTED_DATE))
            } else null
        if (savedInstanceState?.getBoolean(STATE_JUMP_PENDING) == true) {
            pendingDateJump = selectedDate
        }
        savedInstanceState?.getString(STATE_KEYWORD)?.let { model.keyword = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedDate?.let { outState.putLong(STATE_SELECTED_DATE, it.toUtcMillis()) }
        outState.putBoolean(STATE_JUMP_PENDING, pendingDateJump != null)
        outState.putString(STATE_KEYWORD, model.keyword)
        outState.putSerializable(EXTRA_INITIAL_FOLDER, model.folder.value)
    }

    override fun onDestroyView() {
        binding?.MainListView?.removeCallbacks(dateJumpTask)
        submittedItems = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val initialFolder =
            (savedInstanceState ?: arguments)?.let {
                BundleCompat.getSerializable(it, EXTRA_INITIAL_FOLDER, Folder::class.java)
            }
        val initialLabel = arguments?.getString(EXTRA_INITIAL_LABEL)
        model.currentLabel = initialLabel
        initialFolder?.let { model.setFolder(it) }
        binding?.ChipGroup?.visibility = View.VISIBLE
        binding?.MainListView?.scrollIndicators = View.SCROLL_INDICATOR_TOP
        super.onViewCreated(view, savedInstanceState)

        setupDateJump()
        // An empty keyword still needs the complete current list for date navigation.
        model.searchResults?.fetch(model.keyword, model.folder.value, model.currentLabel)
        if (initialLabel?.isEmpty() == true) {
            val checked =
                when (initialFolder ?: model.folder.value) {
                    Folder.NOTES -> R.id.Notes
                    Folder.DELETED -> R.id.Deleted
                    Folder.ARCHIVED -> R.id.Archived
                }
            binding?.ChipGroup?.apply {
                setOnCheckedStateChangeListener { _, checkedId ->
                    when (checkedId.firstOrNull()) {
                        R.id.Notes -> model.setFolder(Folder.NOTES)
                        R.id.Deleted -> model.setFolder(Folder.DELETED)
                        R.id.Archived -> model.setFolder(Folder.ARCHIVED)
                    }
                }
                check(checked)
                isVisible = true
            }
        } else binding?.ChipGroup?.isVisible = false

        viewLifecycleOwner.repeatOnLifecycleScope {
            launch {
                getFlow().collect { items ->
                    model.actionMode.updateSelected(
                        items.filterIsInstance<BaseNote>().map { it.id }
                    )
                    notesAdapter?.setSearchKeyword(model.keyword)
                }
            }
            launch {
                model.searchResults?.isLoading?.collect { isLoading ->
                    binding?.ImageView?.isVisible = !isLoading && model.searchResults.isEmpty
                    binding?.LoadingProgress?.isVisible = isLoading
                    if (!isLoading) scheduleDateJump()
                }
            }
        }
        scheduleDateJump()
    }

    private fun setupDateJump() {
        binding?.EnterSearchKeywordLayout?.apply {
            endIconMode = TextInputLayout.END_ICON_CUSTOM
            isEndIconCheckable = false
            setEndIconDrawable(R.drawable.calendar_search)
            setEndIconContentDescription(R.string.jump_to_date)
            setEndIconOnClickListener { showDatePicker() }
        }
        // DialogFragment restores the calendar itself; reattach its confirmation callback.
        @Suppress("UNCHECKED_CAST")
        val restoredPicker =
            childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<Long>
        restoredPicker?.let { attachDatePickerListener(it) }
    }

    private fun showDatePicker() {
        if (childFragmentManager.findFragmentByTag(DATE_PICKER_TAG) != null) return
        binding?.EnterSearchKeyword?.let {
            it.clearFocus()
            activity?.hideKeyboard(it)
        }
        val picker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.jump_to_date)
                .setSelection(
                    selectedDate?.toUtcMillis() ?: MaterialDatePicker.todayInUtcMilliseconds()
                )
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build()
        attachDatePickerListener(picker)
        picker.show(childFragmentManager, DATE_PICKER_TAG)
    }

    private fun attachDatePickerListener(picker: MaterialDatePicker<Long>) {
        picker.clearOnPositiveButtonClickListeners()
        picker.addOnPositiveButtonClickListener { selection ->
            selectedDate = SearchDate.fromUtcMillis(selection)
            pendingDateJump = selectedDate
            scheduleDateJump()
        }
    }

    override fun onNotesSubmitted(items: List<Item>) {
        submittedItems = items
        scheduleDateJump()
    }

    private fun scheduleDateJump() {
        if (pendingDateJump == null) return
        binding?.MainListView?.apply {
            removeCallbacks(dateJumpTask)
            // Run after SortedList updates and its automatic insertion scrolls.
            post(dateJumpTask)
        }
    }

    private fun jumpToPendingDate() {
        val date = pendingDateJump ?: return
        val submitted = submittedItems ?: return
        if (model.searchResults?.isCurrentResult(submitted) != true) return
        val recyclerView = binding?.MainListView ?: return
        val items = notesAdapter?.currentList ?: return
        val sortBy = model.preferences.notesSorting.value.sortedBy
        val position =
            items.findDateJumpPosition(date) { item ->
                (item as? BaseNote)?.let {
                    if (sortBy == NotesSortBy.MODIFIED_DATE) it.modifiedTimestamp else it.timestamp
                }
            }
        pendingDateJump = null
        if (position == null) {
            requireContext().showToast(R.string.no_notes_to_jump_to)
            return
        }
        when (val layout = recyclerView.layoutManager) {
            is LinearLayoutManager -> layout.scrollToPositionWithOffset(position, 0)
            is StaggeredGridLayoutManager -> layout.scrollToPositionWithOffset(position, 0)
            else -> recyclerView.scrollToPosition(position)
        }
    }

    override fun getBackground() = R.drawable.search

    override fun getFlow() = model.searchResults!!.results

    companion object {
        private const val DATE_PICKER_TAG = "JumpToDatePicker"
        private const val STATE_SELECTED_DATE = "jump_selected_date"
        private const val STATE_JUMP_PENDING = "date_jump_pending"
        private const val STATE_KEYWORD = "search_keyword"
        const val EXTRA_INITIAL_FOLDER = "notallyx.intent.extra.INITIAL_FOLDER"
        const val EXTRA_INITIAL_LABEL = "notallyx.intent.extra.INITIAL_LABEL"
    }
}

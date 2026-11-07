package com.philkes.notallyx.feature.main.tab

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.google.android.material.snackbar.Snackbar
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.ui.components.ItemListener
import com.philkes.notallyx.core.ui.extensions.getQuantityString
import com.philkes.notallyx.core.ui.extensions.movedToResId
import com.philkes.notallyx.core.ui.extensions.repeatOnLifecycleScope
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.feature.editor.EditActivity
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_FOLDER_FROM
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_FOLDER_TO
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_NOTE_ID
import com.philkes.notallyx.feature.editor.EditActivity.Companion.EXTRA_SELECTED_BASE_NOTE
import com.philkes.notallyx.feature.editor.EditListActivity
import com.philkes.notallyx.feature.editor.EditNoteActivity
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.main.NotesGridScreen
import com.philkes.notallyx.feature.main.adapter.BaseNoteAdapter
import com.philkes.notallyx.feature.main.adapter.BaseNoteVHPreferences
import com.philkes.notallyx.feature.main.adapter.createCallback
import com.philkes.notallyx.feature.reminder.RemindersActivity
import com.philkes.notallyx.feature.reminder.RemindersFragment
import com.philkes.notallyx.feature.search.SearchFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.activityViewModel

abstract class NotesFragment : Fragment(), ItemListener {

    protected var notesAdapter: BaseNoteAdapter? = null
    private lateinit var openNoteActivityResultLauncher: ActivityResultLauncher<Intent>
    private var lastSelectedNotePosition = -1

    internal val model: NotesFragmentViewModel by activityViewModel()

    override fun onDestroyView() {
        super.onDestroyView()
        notesAdapter = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        setupObserver()
        setupActivityResultLaunchers()
    }

    private fun setupActivityResultLaunchers() {
        openNoteActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data
                    val id = data?.getLongExtra(EXTRA_NOTE_ID, -1)
                    if (id != null) {
                        val folderFrom = Folder.valueOf(data.getStringExtra(EXTRA_FOLDER_FROM)!!)
                        val folderTo = Folder.valueOf(data.getStringExtra(EXTRA_FOLDER_TO)!!)
                        view?.let { currentView ->
                            Snackbar.make(
                                    currentView,
                                    requireContext().getQuantityString(folderTo.movedToResId(), 1),
                                    Snackbar.LENGTH_SHORT,
                                )
                                .apply {
                                    setAction(R.string.undo) {
                                        model.moveBaseNotes(longArrayOf(id), folderFrom)
                                    }
                                }
                                .show()
                        }
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setHasOptionsMenu(true)
        val composeView =
            ComposeView(requireContext()).apply {
                setContent {
                    NotallyXTheme(theme = model.preferences.theme.value) {
                        val itemsState =
                            getFlow().collectAsStateWithLifecycle(initialValue = emptyList())
                        val isGrid = model.preferences.notesView.value == NotesView.GRID
                        val selectedFolder by
                            model.folder.collectAsStateWithLifecycle(initialValue = Folder.NOTES)
                        val folderForGrid =
                            if (this@NotesFragment is SearchFragment) selectedFolder else null
                        val isLoading =
                            if (this@NotesFragment is SearchFragment) {
                                model.searchResults
                                    ?.isLoading
                                    ?.collectAsStateWithLifecycle(initialValue = false)
                                    ?.value == true
                            } else false

                        val selectedFilterOption by
                            if (this@NotesFragment is RemindersFragment) {
                                (this@NotesFragment as RemindersFragment)
                                    .filterModeFlow
                                    .collectAsStateWithLifecycle()
                            } else
                                androidx.compose.runtime.remember {
                                    androidx.compose.runtime.mutableStateOf(null)
                                }

                        val selectedNoteIds by
                            model.actionMode.selectedIdsFlow.collectAsStateWithLifecycle(emptySet())
                        val searchKeyword by model.keywordFlow.collectAsStateWithLifecycle()

                        NotesGridScreen(
                            items = itemsState.value,
                            isGrid = isGrid,
                            imageRoot = model.imageRoot,
                            searchKeyword = searchKeyword,
                            onSearchKeywordChange = { newKeyword ->
                                if (this@NotesFragment is SearchFragment) {
                                    model.keyword = newKeyword
                                }
                            },
                            showSearchBar =
                                (this@NotesFragment is SearchFragment) ||
                                    model.preferences.alwaysShowSearchBar.value,
                            selectedFolder = folderForGrid,
                            onFolderSelected = { folder -> model.setFolder(folder) },
                            selectedFilterOption = selectedFilterOption,
                            onFilterOptionSelected = { filterOption ->
                                if (this@NotesFragment is RemindersFragment) {
                                    (this@NotesFragment as RemindersFragment).applyFilter(
                                        filterOption
                                    )
                                }
                            },
                            selectedNoteIds = selectedNoteIds,
                            backgroundResId = getBackgroundResId(),
                            isLoading = isLoading,
                            dateFormat = model.preferences.dateFormatOverview.value,
                            timeFormat = model.preferences.timeFormatOverview.value,
                            sortBy = model.preferences.notesSorting.value.sortedBy,
                            textSize = model.preferences.textSizeOverview.value,
                            maxTitleLines = model.preferences.maxTitle.value,
                            maxLines = model.preferences.maxLines.value,
                            maxItems = model.preferences.maxItems.value,
                            hideLabels = model.preferences.labelTagsHiddenInOverview.value,
                            hideImages = model.preferences.imagesHiddenInOverview.value,
                            onNoteClick = { _, position -> onClick(position) },
                            onNoteLongClick = { _, position -> onLongClick(position) },
                            onReminderClick = { _, position -> onReminderClick(position) },
                        )
                    }
                }
            }
        return composeView
    }

    override fun onClick(position: Int) {
        if (position != -1) {
            notesAdapter?.getItem(position)?.let { item ->
                if (item is BaseNote) {
                    if (model.actionMode.isEnabled()) {
                        handleNoteSelection(item.id, position, item)
                    } else {
                        when (item.type) {
                            Type.NOTE -> goToActivity(EditNoteActivity::class.java, item)
                            Type.LIST -> goToActivity(EditListActivity::class.java, item)
                        }
                    }
                }
            }
        }
    }

    override fun onReminderClick(position: Int) {
        if (model.actionMode.isEnabled()) {
            onClick(position)
            return
        }
        if (position != -1) {
            notesAdapter?.getItem(position)?.let { item ->
                if (item is BaseNote) {
                    val intent =
                        Intent(requireContext(), RemindersActivity::class.java).apply {
                            putExtra(RemindersActivity.NOTE_ID, item.id)
                        }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onLongClick(position: Int) {
        if (position != -1) {
            if (model.actionMode.selectedNotes.isNotEmpty()) {
                if (lastSelectedNotePosition > position) {
                        position..lastSelectedNotePosition
                    } else {
                        lastSelectedNotePosition..position
                    }
                    .forEach { pos ->
                        notesAdapter!!.getItem(pos)?.let { item ->
                            if (item is BaseNote) {
                                if (!model.actionMode.selectedNotes.contains(item.id)) {
                                    handleNoteSelection(item.id, pos, item)
                                }
                            }
                        }
                    }
            } else {
                notesAdapter?.getItem(position)?.let { item ->
                    if (item is BaseNote) {
                        handleNoteSelection(item.id, position, item)
                    }
                }
            }
        }
    }

    private fun handleNoteSelection(id: Long, position: Int, baseNote: BaseNote) {
        if (model.actionMode.selectedNotes.contains(id)) {
            model.actionMode.remove(id)
        } else {
            model.actionMode.add(id, baseNote)
            lastSelectedNotePosition = position
        }
        notesAdapter?.notifyItemChanged(position, 0)
    }

    private fun setupAdapter() {
        notesAdapter =
            with(model.preferences) {
                BaseNoteAdapter(
                    model.actionMode.selectedIds,
                    dateFormatOverview.value,
                    timeFormatOverview.value,
                    notesAdapterSortCallback(),
                    BaseNoteVHPreferences(
                        textSizeOverview.value,
                        maxItems.value,
                        maxLines.value,
                        maxTitle.value,
                        labelTagsHiddenInOverview.value,
                        imagesHiddenInOverview.value,
                        notesSorting.value.sortedBy,
                    ),
                    model.imageRoot,
                    this@NotesFragment,
                )
            }

        model.actionMode.addListener = { notesAdapter?.notifyDataSetChanged() }
        if (activity is MainActivity) {
            (activity as MainActivity).getCurrentFragmentNotes = {
                notesAdapter?.currentList?.filterIsInstance<BaseNote>()
            }
        }
    }

    protected open fun notesAdapterSortCallback():
        (BaseNoteAdapter) -> SortedListAdapterCallback<Item> = { adapter ->
        model.preferences.notesSorting.value.createCallback(adapter)
    }

    private fun setupObserver() {
        var lastSubmittedList: List<Item>? = null
        var currentAppliedSort = model.preferences.notesSorting.value

        viewLifecycleOwner.repeatOnLifecycleScope {
            launch {
                getFlow().collect { list ->
                    if (list != lastSubmittedList) {
                        lastSubmittedList = list
                        notesAdapter?.submitList(list)
                    }
                }
            }
            launch {
                model.preferences.notesSorting.flow.collect { notesSort ->
                    if (notesSort != currentAppliedSort) {
                        currentAppliedSort = notesSort
                        notesAdapter?.setNotesSort(notesSort)
                    }
                }
            }
            launch {
                model.actionMode.closeListener.collect { event ->
                    event?.handle { ids ->
                        notesAdapter?.currentList?.forEachIndexed { index, item ->
                            if (item is BaseNote && ids.contains(item.id)) {
                                notesAdapter?.notifyItemChanged(index, 0)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun goToActivity(activity: Class<*>, baseNote: BaseNote) {
        val intent = Intent(requireContext(), activity)
        intent.putExtra(EXTRA_SELECTED_BASE_NOTE, baseNote.id)
        val isInSearch = view?.findNavController()?.currentDestination?.id == R.id.Search
        if (isInSearch && model.keyword.isNotBlank()) {
            intent.putExtra(EditActivity.EXTRA_INITIAL_SEARCH_QUERY, model.keyword)
        }
        openNoteActivityResultLauncher.launch(intent)
    }

    abstract fun getBackgroundResId(): Int

    abstract fun getFlow(): Flow<List<Item>>

    open fun prepareNewNoteIntent(intent: Intent): Intent {
        return intent
    }
}

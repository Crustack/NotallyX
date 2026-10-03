package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.isEmpty
import com.philkes.notallyx.presentation.repeatOnLifecycleScope
import kotlinx.coroutines.launch

class SearchFragment : NotesFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO: autofocus and show keyboard
        val initialFolder = arguments?.let {
            BundleCompat.getSerializable(it, EXTRA_INITIAL_FOLDER, Folder::class.java)
        }
        binding?.ChipGroup?.visibility = View.VISIBLE
        binding?.MainListView?.scrollIndicators = View.SCROLL_INDICATOR_TOP
        super.onViewCreated(view, savedInstanceState)

        val initialLabel = arguments?.getString(EXTRA_INITIAL_LABEL)
        model.currentLabel = initialLabel
        if (initialLabel?.isEmpty() == true) {
            val checked =
                when (initialFolder ?: model.folder.value) {
                    Folder.NOTES -> R.id.Notes
                    Folder.DELETED -> R.id.Deleted
                    Folder.ARCHIVED -> R.id.Archived
                }

            binding?.ChipGroup?.apply {
                setOnCheckedStateChangeListener { _, checkedId ->
                    when (checkedId.first()) {
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
                }
            }
        }
    }

    override fun getBackground() = R.drawable.search

    override fun getFlow() = model.searchResults!!.results

    companion object {
        const val EXTRA_INITIAL_FOLDER = "notallyx.intent.extra.INITIAL_FOLDER"
        const val EXTRA_INITIAL_LABEL = "notallyx.intent.extra.INITIAL_LABEL"
    }
}

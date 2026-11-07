package com.philkes.notallyx.feature.search

// import com.philkes.notallyx.repeatOnLifecycleScope
import android.os.Bundle
import android.view.View
import androidx.core.os.BundleCompat
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.main.tab.NotesFragment
import kotlinx.coroutines.launch

class SearchFragment : NotesFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val initialFolder = arguments?.let {
            BundleCompat.getSerializable(it, EXTRA_INITIAL_FOLDER, Folder::class.java)
        }
        super.onViewCreated(view, savedInstanceState)

        val initialLabel = arguments?.getString(EXTRA_INITIAL_LABEL)
        model.currentLabel = initialLabel
        if (initialLabel.isNullOrEmpty()) {
            initialFolder?.let { model.setFolder(it) }
        }

        viewLifecycleOwner.repeatOnLifecycleScope {
            launch {
                getFlow().collect { items ->
                    model.actionMode.updateSelected(
                        items.filterIsInstance<BaseNote>().map { it.id }
                    )
                    notesAdapter?.setSearchKeyword(model.keyword)
                }
            }
        }
    }

    override fun getBackgroundResId() = R.drawable.search

    override fun getFlow() = model.searchResults!!.results

    companion object {
        const val EXTRA_INITIAL_FOLDER = "notallyx.intent.extra.INITIAL_FOLDER"
        const val EXTRA_INITIAL_LABEL = "notallyx.intent.extra.INITIAL_LABEL"
    }
}

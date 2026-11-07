package com.philkes.notallyx.feature.labels

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.main.tab.NotesFragment
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.viewmodel.ext.android.viewModel

class DisplayLabelFragment : NotesFragment() {

    private val displayLabelViewModel: DisplayLabelViewModel by viewModel()
    private lateinit var label: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.NOTES)
    }

    override fun getBackgroundResId() = R.drawable.label

    override fun getFlow(): Flow<List<Item>> {
        label =
            requireNotNull(
                requireArguments().getString(EXTRA_DISPLAYED_LABEL),
                { "DisplayLabelFragment does not have '$EXTRA_DISPLAYED_LABEL' arg" },
            )
        return displayLabelViewModel.getNotesByLabel(label)
    }

    override fun prepareNewNoteIntent(intent: Intent): Intent {
        return intent.putExtra(EXTRA_DISPLAYED_LABEL, label)
    }

    companion object {
        const val EXTRA_DISPLAYED_LABEL = "notallyx.intent.extra.DISPLAYED_LABEL"
    }
}

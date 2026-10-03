package com.philkes.notallyx.presentation.activity.main.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.viewmodel.main.fragment.DisplayLabelViewModel
import kotlinx.coroutines.flow.Flow

class DisplayLabelFragment : NotesFragment() {

    private val displayLabelViewModel: DisplayLabelViewModel by viewModels()
    private lateinit var label: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.NOTES)
    }

    override fun getBackground() = R.drawable.label

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

package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.presentation.viewmodel.main.fragment.NotesViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class NotesOverviewFragment : NotesFragment() {

    private val notesViewModel: NotesViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.NOTES)
    }

    override fun getFlow() = notesViewModel.baseNotes

    override fun getBackground() = R.drawable.notebook
}

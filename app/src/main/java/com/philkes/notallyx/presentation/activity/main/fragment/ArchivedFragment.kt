package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.presentation.viewmodel.main.fragment.ArchivedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArchivedFragment : NotesFragment() {

    private val archivedViewModel: ArchivedViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.ARCHIVED)
    }

    override fun getBackground() = R.drawable.archive

    override fun getFlow() = archivedViewModel.archivedNotes
}

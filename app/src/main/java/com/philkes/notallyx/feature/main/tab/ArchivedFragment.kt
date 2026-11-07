package com.philkes.notallyx.feature.main.tab

import android.os.Bundle
import android.view.View
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import org.koin.androidx.viewmodel.ext.android.viewModel

class ArchivedFragment : NotesFragment() {

    private val archivedViewModel: ArchivedViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.ARCHIVED)
    }

    override fun getBackgroundResId() = R.drawable.archive

    override fun getFlow() = archivedViewModel.archivedNotes
}

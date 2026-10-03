package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.presentation.viewmodel.main.fragment.ArchivedViewModel

class ArchivedFragment : NotesFragment() {

    private val archivedViewModel: ArchivedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.ARCHIVED)
    }

    override fun getBackground() = R.drawable.archive

    override fun getFlow() = archivedViewModel.archivedNotes
}

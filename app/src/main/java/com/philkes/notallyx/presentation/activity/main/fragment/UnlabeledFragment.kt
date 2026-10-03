package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.presentation.viewmodel.main.fragment.UnlabeledViewModel
import kotlinx.coroutines.flow.Flow

class UnlabeledFragment : NotesFragment() {

    private val unlabeledViewModel: UnlabeledViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.NOTES)
    }

    override fun getBackground() = R.drawable.label_off

    override fun getFlow(): Flow<List<Item>> {
        return unlabeledViewModel.unlabeledNotes
    }
}

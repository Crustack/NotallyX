package com.philkes.notallyx.feature.main.tab

import android.os.Bundle
import android.view.View
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Item
import kotlinx.coroutines.flow.Flow
import org.koin.androidx.viewmodel.ext.android.viewModel

class UnlabeledFragment : NotesFragment() {

    private val unlabeledViewModel: UnlabeledViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.NOTES)
    }

    override fun getBackgroundResId() = R.drawable.label_off

    override fun getFlow(): Flow<List<Item>> {
        return unlabeledViewModel.unlabeledNotes
    }
}

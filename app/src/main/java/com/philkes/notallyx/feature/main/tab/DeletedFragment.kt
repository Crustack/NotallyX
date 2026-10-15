package com.philkes.notallyx.feature.main.tab

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.extensions.add
import com.philkes.notallyx.data.model.Folder
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeletedFragment : NotesFragment() {

    private val deletedViewModel: DeletedViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.DELETED)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.delete_all, R.drawable.delete_all) {
            deletedViewModel.showDeleteAllDialog()
        }
    }

    @Composable
    override fun ContentOverlay() {
        val showConfirmDeleteAllDialog by
            deletedViewModel.showConfirmDeleteAllDialog.collectAsStateWithLifecycle()

        if (showConfirmDeleteAllDialog) {
            ConfirmDialog(
                message = stringResource(R.string.delete_all_notes),
                positiveButtonText = stringResource(R.string.delete),
                onDismissRequest = { deletedViewModel.dismissDeleteAllDialog() },
                onConfirm = { deletedViewModel.confirmDeleteAll() },
            )
        }
    }

    override fun getBackgroundResId() = R.drawable.delete

    override fun getFlow() = deletedViewModel.deletedNotes
}

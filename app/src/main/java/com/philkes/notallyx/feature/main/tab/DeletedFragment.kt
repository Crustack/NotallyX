package com.philkes.notallyx.feature.main.tab

import android.app.Dialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.Window
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.extensions.add
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Folder
import org.koin.androidx.viewmodel.ext.android.viewModel

class DeletedFragment : NotesFragment() {

    private val deletedViewModel: DeletedViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.setFolder(Folder.DELETED)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.delete_all, R.drawable.delete_all) { deleteAllNotes() }
    }

    private fun deleteAllNotes() {
        val composeView = ComposeView(requireContext())
        val dialog =
            Dialog(requireContext()).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                setContentView(composeView)
            }
        composeView.setContent {
            NotallyXTheme(theme = deletedViewModel.preferences.theme.value) {
                ConfirmDialog(
                    message = stringResource(R.string.delete_all_notes),
                    positiveButtonText = stringResource(R.string.delete),
                    onDismissRequest = { dialog.dismiss() },
                    onConfirm = { deletedViewModel.deleteAllTrashedBaseNotes() },
                )
            }
        }
        dialog.show()
    }

    override fun getBackgroundResId() = R.drawable.delete

    override fun getFlow() = deletedViewModel.deletedNotes
}

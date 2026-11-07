package com.philkes.notallyx.presentation.activity.note

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.ComposeBinding
import com.philkes.notallyx.presentation.activity.LockedActivity
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.dialog.TextInputDialog
import com.philkes.notallyx.presentation.view.screens.SelectLabelsScreen
import com.philkes.notallyx.presentation.viewmodel.main.fragment.LabelsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class SelectLabelsActivity : LockedActivity<ComposeBinding>() {

    private val labelsViewModel: LabelsViewModel by viewModel()
    private lateinit var selectedLabels: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val composeView = ComposeView(this)
        binding = ComposeBinding(composeView)
        setContentView(composeView)

        val savedList = savedInstanceState?.getStringArrayList(EXTRA_SELECTED_LABELS)
        val passedList =
            requireNotNull(
                intent.getStringArrayListExtra(EXTRA_SELECTED_LABELS),
                { "SelectLabelsActivity intent has no '$EXTRA_SELECTED_LABELS' extra" },
            )
        selectedLabels = savedList ?: passedList

        val result = Intent()
        result.putExtra(EXTRA_SELECTED_LABELS, selectedLabels)
        setResult(RESULT_OK, result)

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                val labelsState = labelsViewModel.labels.collectAsStateWithLifecycle(emptyList())
                val selectedLabelsState = remember {
                    mutableStateListOf(*selectedLabels.toTypedArray())
                }

                SelectLabelsScreen(
                    labels = labelsState.value,
                    selectedLabels = selectedLabelsState,
                    onLabelToggle = { value, checked ->
                        if (checked) {
                            if (!selectedLabels.contains(value)) {
                                selectedLabels.add(value)
                                selectedLabelsState.add(value)
                            }
                        } else {
                            selectedLabels.remove(value)
                            selectedLabelsState.remove(value)
                        }
                    },
                    onAddLabelClick = { addLabel() },
                    onBackClick = { finish() },
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(EXTRA_SELECTED_LABELS, selectedLabels)
    }

    private fun addLabel() {
        val composeView = ComposeView(this)
        val dialog = MaterialAlertDialogBuilder(this).setView(composeView).create()

        composeView.setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                TextInputDialog(
                    title = stringResource(R.string.add_label),
                    initialValue = "",
                    onDismissRequest = { dialog.dismiss() },
                    onConfirm = { value ->
                        if (value.isNotEmpty()) {
                            labelsViewModel.insertLabel(value) { success ->
                                if (success) {
                                    dialog.dismiss()
                                } else showToast(R.string.label_exists)
                            }
                        }
                    },
                )
            }
        }
        dialog.show()
    }

    companion object {
        const val EXTRA_SELECTED_LABELS = "notallyx.intent.extra.EXTRA_SELECTED_LABELS"
    }
}

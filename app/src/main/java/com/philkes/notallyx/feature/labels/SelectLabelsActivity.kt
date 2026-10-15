package com.philkes.notallyx.feature.labels

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.security.ComposeBinding
import com.philkes.notallyx.core.security.LockedActivity
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
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
                val dialogState by labelsViewModel.dialogState.collectAsStateWithLifecycle()

                SelectLabelsScreen(
                    labels = labelsState.value,
                    selectedLabels = selectedLabelsState,
                    dialogState = dialogState,
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
                    onAddLabelClick = { labelsViewModel.showAddDialog() },
                    onBackClick = { finish() },
                    onDismissDialog = { labelsViewModel.dismissDialog() },
                    onConfirmAddLabel = { value ->
                        labelsViewModel.insertLabel(value) { success ->
                            if (success) {
                                labelsViewModel.dismissDialog()
                            } else showToast(R.string.label_exists)
                        }
                    },
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(EXTRA_SELECTED_LABELS, selectedLabels)
    }

    companion object {
        const val EXTRA_SELECTED_LABELS = "notallyx.intent.extra.EXTRA_SELECTED_LABELS"
    }
}

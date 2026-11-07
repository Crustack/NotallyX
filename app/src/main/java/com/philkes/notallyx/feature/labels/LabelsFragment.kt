package com.philkes.notallyx.feature.labels

// import com.philkes.notallyx.displayEditLabelDialog
// import com.philkes.notallyx.setCancelButton
// import com.philkes.notallyx.showToast
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.extensions.displayEditLabelDialog
import com.philkes.notallyx.core.ui.extensions.setCancelButton
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.labels.DisplayLabelFragment.Companion.EXTRA_DISPLAYED_LABEL
import com.philkes.notallyx.feature.labels.adapter.LabelData
import com.philkes.notallyx.feature.labels.adapter.LabelListener
import org.koin.androidx.viewmodel.ext.android.viewModel

class LabelsFragment : Fragment(), LabelListener {

    private val model: LabelsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                NotallyXTheme(theme = model.preferences.theme.value) {
                    val labelsFlow = remember {
                        kotlinx.coroutines.flow.combine(
                            model.labels,
                            model.preferences.labelsHidden.flow,
                        ) { labels, hiddenLabels ->
                            labels.map { label ->
                                LabelData(
                                    label.value,
                                    !hiddenLabels.contains(label.value),
                                    label.order,
                                )
                            }
                        }
                    }
                    val labelsState = labelsFlow.collectAsStateWithLifecycle(emptyList())

                    LabelsScreen(
                        labels = labelsState.value,
                        onLabelClick = { labelData ->
                            val bundle = Bundle()
                            bundle.putString(EXTRA_DISPLAYED_LABEL, labelData.value)
                            findNavController().navigate(R.id.LabelsToDisplayLabel, bundle)
                        },
                        onEditClick = { labelData ->
                            displayEditLabelDialog(labelData.value, model)
                        },
                        onDeleteClick = { labelData -> confirmDeletion(labelData.value) },
                        onToggleVisibilityClick = { labelData ->
                            val hiddenLabels = model.preferences.labelsHidden.value.toMutableSet()
                            if (labelData.visibleInNavigation) {
                                hiddenLabels.add(labelData.value)
                            } else {
                                hiddenLabels.remove(labelData.value)
                            }
                            model.savePreference(model.preferences.labelsHidden, hiddenLabels)
                        },
                    )
                }
            }
        }
    }

    override fun onClick(position: Int) {}

    override fun onEdit(position: Int) {}

    override fun onDelete(position: Int) {}

    override fun onToggleVisibility(position: Int) {}

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {}

    internal fun displayAddLabelDialog() {
        val composeView = ComposeView(requireContext())
        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(composeView).create()

        composeView.setContent {
            NotallyXTheme(theme = model.preferences.theme.value) {
                TextInputDialog(
                    title = stringResource(R.string.add_label),
                    initialValue = "",
                    onDismissRequest = { dialog.dismiss() },
                    onConfirm = { value ->
                        if (value.isNotEmpty()) {
                            model.insertLabel(value) { success: Boolean ->
                                if (success) {
                                    dialog.dismiss()
                                } else {
                                    showToast(R.string.label_exists)
                                }
                            }
                        }
                    },
                )
            }
        }
        dialog.show()
    }

    private fun confirmDeletion(value: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_label)
            .setMessage(R.string.your_notes_associated)
            .setPositiveButton(R.string.delete) { _, _ -> model.deleteLabel(value) }
            .setCancelButton()
            .show()
    }
}

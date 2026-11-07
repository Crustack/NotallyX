package com.philkes.notallyx.feature.labels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.labels.DisplayLabelFragment.Companion.EXTRA_DISPLAYED_LABEL
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
                    val dialogState by model.dialogState.collectAsStateWithLifecycle()

                    LabelsScreen(
                        labels = labelsState.value,
                        dialogState = dialogState,
                        onLabelClick = { labelData ->
                            val bundle = Bundle()
                            bundle.putString(EXTRA_DISPLAYED_LABEL, labelData.value)
                            findNavController().navigate(R.id.LabelsToDisplayLabel, bundle)
                        },
                        onEditClick = { labelData -> model.showEditDialog(labelData.value) },
                        onDeleteClick = { labelData -> model.showDeleteDialog(labelData.value) },
                        onToggleVisibilityClick = { labelData ->
                            val hiddenLabels = model.preferences.labelsHidden.value.toMutableSet()
                            if (labelData.visibleInNavigation) {
                                hiddenLabels.add(labelData.value)
                            } else {
                                hiddenLabels.remove(labelData.value)
                            }
                            model.savePreference(model.preferences.labelsHidden, hiddenLabels)
                        },
                        onDismissDialog = { model.dismissDialog() },
                        onConfirmAddLabel = { value ->
                            model.insertLabel(value) { success ->
                                if (success) {
                                    model.dismissDialog()
                                } else {
                                    showToast(R.string.label_exists)
                                }
                            }
                        },
                        onConfirmEditLabel = { oldLabel, newLabel ->
                            model.updateLabel(oldLabel, newLabel) { success ->
                                if (success) {
                                    model.dismissDialog()
                                } else {
                                    showToast(R.string.label_exists)
                                }
                            }
                        },
                        onConfirmDeleteLabel = { label ->
                            model.deleteLabel(label)
                            model.dismissDialog()
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
        model.showAddDialog()
    }
}

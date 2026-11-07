package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.databinding.DialogInputBinding
import com.philkes.notallyx.presentation.activity.main.fragment.DisplayLabelFragment.Companion.EXTRA_DISPLAYED_LABEL
import com.philkes.notallyx.presentation.add
import com.philkes.notallyx.presentation.displayEditLabelDialog
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.showAndFocus
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.main.label.LabelData
import com.philkes.notallyx.presentation.view.main.label.LabelListener
import com.philkes.notallyx.presentation.view.screens.LabelsScreen
import com.philkes.notallyx.presentation.viewmodel.main.fragment.LabelsViewModel
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.viewModel

class LabelsFragment : Fragment(), LabelListener {

    private val model: LabelsViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        setHasOptionsMenu(true)
        val composeView =
            ComposeView(requireContext()).apply {
                setContent {
                    NotallyXTheme(theme = model.preferences.theme.value) {
                        val labelsFlow = remember {
                            combine(
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
                                val hiddenLabels =
                                    model.preferences.labelsHidden.value.toMutableSet()
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
        return composeView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.add_label, R.drawable.add) { displayAddLabelDialog() }
    }

    override fun onClick(position: Int) {}

    override fun onEdit(position: Int) {}

    override fun onDelete(position: Int) {}

    override fun onToggleVisibility(position: Int) {}

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {}

    private fun displayAddLabelDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogBinding = DialogInputBinding.inflate(inflater)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_label)
            .setView(dialogBinding.root)
            .setCancelButton()
            .setPositiveButton(R.string.save) { dialog, _ ->
                val value = dialogBinding.EditText.text.toString().trim()
                if (value.isNotEmpty()) {
                    model.insertLabel(value) { success: Boolean ->
                        if (success) {
                            dialog.dismiss()
                        } else {
                            showToast(R.string.label_exists)
                        }
                    }
                }
            }
            .showAndFocus(dialogBinding.EditText, allowFullSize = true) { positiveButton ->
                dialogBinding.EditText.doAfterTextChanged { text ->
                    positiveButton.isEnabled = !text.isNullOrEmpty()
                }
                positiveButton.isEnabled = false
            }
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

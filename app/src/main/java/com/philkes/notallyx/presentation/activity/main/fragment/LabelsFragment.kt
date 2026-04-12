package com.philkes.notallyx.presentation.activity.main.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.databinding.DialogInputBinding
import com.philkes.notallyx.databinding.FragmentNotesBinding
import com.philkes.notallyx.presentation.activity.main.fragment.DisplayLabelFragment.Companion.EXTRA_DISPLAYED_LABEL
import com.philkes.notallyx.presentation.add
import com.philkes.notallyx.presentation.displayEditLabelDialog
import com.philkes.notallyx.presentation.initListView
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.showAndFocus
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.main.label.LabelAdapter
import com.philkes.notallyx.presentation.view.main.label.LabelData
import com.philkes.notallyx.presentation.view.main.label.LabelListener
import com.philkes.notallyx.presentation.viewmodel.BaseNoteModel

class LabelsFragment : Fragment(), LabelListener {

    private var labelAdapter: LabelAdapter? = null
    private var binding: FragmentNotesBinding? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private val model: BaseNoteModel by activityViewModels()

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        labelAdapter = null
        itemTouchHelper = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        labelAdapter = LabelAdapter(this)

        binding?.MainListView?.apply {
            initListView(requireContext())
            adapter = labelAdapter
            binding?.ImageView?.setImageResource(R.drawable.label)
            setupItemTouchHelper(this)
        }

        setupObserver()
    }

    private fun setupItemTouchHelper(recyclerView: RecyclerView) {
        val itemTouchHelperCallback =
            object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

                private var currentList = labelAdapter?.currentList
                private var didReorder = false

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder,
                ): Boolean {
                    val fromPosition = viewHolder.bindingAdapterPosition
                    val toPosition = target.bindingAdapterPosition
                    currentList = labelAdapter?.onItemMove(fromPosition, toPosition)
                    didReorder = fromPosition != toPosition
                    return didReorder
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

                override fun isLongPressDragEnabled(): Boolean = true

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                ) {
                    super.clearView(recyclerView, viewHolder)
                    if (!didReorder) return

                    currentList?.let { list ->
                        val size = list.size
                        val updatedLabels =
                            list.mapIndexed { index, labelData ->
                                Label(labelData.value, size - 1 - index)
                            }
                        model.updateLabels(updatedLabels)
                    }
                    didReorder = false
                }
            }
        itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        itemTouchHelper?.startDrag(viewHolder)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        setHasOptionsMenu(true)
        binding = FragmentNotesBinding.inflate(inflater)
        return binding?.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.add_label, R.drawable.add) { displayAddLabelDialog() }
    }

    override fun onClick(position: Int) {
        labelAdapter?.currentList?.get(position)?.let { labelData ->
            val bundle = Bundle()
            bundle.putString(EXTRA_DISPLAYED_LABEL, labelData.value)
            findNavController().navigate(R.id.LabelsToDisplayLabel, bundle)
        }
    }

    override fun onEdit(position: Int) {
        labelAdapter?.currentList?.get(position)?.let { labelData ->
            displayEditLabelDialog(labelData.value, model)
        }
    }

    override fun onDelete(position: Int) {
        labelAdapter?.currentList?.get(position)?.let { labelData ->
            confirmDeletion(labelData.value)
        }
    }

    override fun onToggleVisibility(position: Int) {
        labelAdapter?.currentList?.get(position)?.let { value ->
            val hiddenLabels = model.preferences.labelsHidden.value.toMutableSet()
            if (value.visibleInNavigation) {
                hiddenLabels.add(value.value)
            } else {
                hiddenLabels.remove(value.value)
            }
            model.savePreference(model.preferences.labelsHidden, hiddenLabels)

            val currentList = labelAdapter!!.currentList.toMutableList()
            currentList[position] =
                currentList[position].copy(visibleInNavigation = !value.visibleInNavigation)
            labelAdapter!!.submitList(currentList)
        }
    }

    private fun setupObserver() {
        model.labels.observe(viewLifecycleOwner) { labels ->
            val hiddenLabels = model.preferences.labelsHidden.value
            val labelsData =
                labels.map { label ->
                    LabelData(label.value, !hiddenLabels.contains(label.value), label.order)
                }
            labelAdapter?.submitList(labelsData)
            binding?.ImageView?.isVisible = labels.isEmpty()
        }
    }

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

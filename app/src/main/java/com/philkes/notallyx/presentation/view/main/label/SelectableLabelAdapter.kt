package com.philkes.notallyx.presentation.view.main.label

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.databinding.RecyclerSelectableLabelBinding

class SelectableLabelAdapter(private val selectedLabels: List<String>) :
    ListAdapter<Label, SelectableLabelVH>(LabelDiffCallback()) {

    var onChecked: ((position: Int, checked: Boolean) -> Unit)? = null

    override fun onBindViewHolder(holder: SelectableLabelVH, position: Int) {
        val label = getItem(position)
        holder.bind(label, selectedLabels.contains(label.value))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectableLabelVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerSelectableLabelBinding.inflate(inflater, parent, false)
        return SelectableLabelVH(binding, requireNotNull(onChecked, { "onChecked is null" }))
    }
}

class LabelDiffCallback() : DiffUtil.ItemCallback<Label>() {

    override fun areItemsTheSame(oldItem: Label, newItem: Label): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Label, newItem: Label): Boolean {
        return oldItem.value == newItem.value && oldItem.order == newItem.order
    }
}

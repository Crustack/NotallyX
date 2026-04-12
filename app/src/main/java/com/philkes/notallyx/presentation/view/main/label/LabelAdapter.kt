package com.philkes.notallyx.presentation.view.main.label

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.philkes.notallyx.databinding.RecyclerLabelBinding

class LabelAdapter(private val listener: LabelListener) :
    ListAdapter<LabelData, LabelVH>(DiffCallback) {

    override fun onBindViewHolder(holder: LabelVH, position: Int) {
        val label = getItem(position)
        holder.bind(label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerLabelBinding.inflate(inflater, parent, false)
        return LabelVH(binding, listener)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): List<LabelData> {
        if (
            fromPosition !in currentList.indices ||
                toPosition !in currentList.indices ||
                fromPosition == toPosition
        ) {
            return currentList
        }
        val list = currentList.toMutableList()
        val fromLabel = list.removeAt(fromPosition)
        list.add(toPosition, fromLabel)
        submitList(list)
        return list
    }
}

data class LabelData(val value: String, var visibleInNavigation: Boolean, val order: Int)

private object DiffCallback : DiffUtil.ItemCallback<LabelData>() {

    override fun areItemsTheSame(oldItem: LabelData, newItem: LabelData): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: LabelData, newItem: LabelData): Boolean {
        return oldItem == newItem
    }
}

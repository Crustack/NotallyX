package com.philkes.notallyx.presentation.view.main.label

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.R
import com.philkes.notallyx.databinding.RecyclerLabelBinding

@SuppressLint("ClickableViewAccessibility")
class LabelVH(private val binding: RecyclerLabelBinding, listener: LabelListener) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        binding.apply {
            LabelText.setOnClickListener { listener.onClick(absoluteAdapterPosition) }
            LabelText.setOnLongClickListener { _ ->
                listener.onStartDrag(this@LabelVH)
                true
            }
            DragHandle.setOnLongClickListener { _ ->
                listener.onStartDrag(this@LabelVH)
                true
            }
            EditButton.setOnClickListener { listener.onEdit(absoluteAdapterPosition) }
            DeleteButton.setOnClickListener { listener.onDelete(absoluteAdapterPosition) }
            VisibilityButton.setOnClickListener {
                listener.onToggleVisibility(absoluteAdapterPosition)
            }
        }
    }

    fun bind(value: LabelData) {
        binding.LabelText.text = value.value
        binding.VisibilityButton.setImageResource(
            if (value.visibleInNavigation) R.drawable.visibility else R.drawable.visibility_off
        )
    }
}

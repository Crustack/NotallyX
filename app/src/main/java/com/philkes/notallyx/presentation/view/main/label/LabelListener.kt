package com.philkes.notallyx.presentation.view.main.label

import androidx.recyclerview.widget.RecyclerView

interface LabelListener {

    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)

    fun onClick(position: Int)

    fun onEdit(position: Int)

    fun onDelete(position: Int)

    fun onToggleVisibility(position: Int)
}

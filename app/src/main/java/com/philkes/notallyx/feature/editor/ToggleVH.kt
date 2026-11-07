package com.philkes.notallyx.feature.editor

import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.core.ui.extensions.setControlsContrastColorForAllViews
import com.philkes.notallyx.databinding.RecyclerToggleBinding

// import com.philkes.notallyx.setControlsContrastColorForAllViews

class ToggleVH(private val binding: RecyclerToggleBinding, @ColorInt private val color: Int?) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(toggle: Toggle) {
        binding.root.apply {
            setIconResource(toggle.drawable)
            contentDescription = context.getString(toggle.titleResId)
            setOnClickListener { toggle.onToggle(toggle) }
            isChecked = toggle.checked
            color?.let { setControlsContrastColorForAllViews(it, overwriteBackground = false) }
        }
    }
}

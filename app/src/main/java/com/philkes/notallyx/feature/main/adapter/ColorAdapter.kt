package com.philkes.notallyx.feature.main.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.core.ui.components.ItemListener
import com.philkes.notallyx.data.model.ColorString
import com.philkes.notallyx.databinding.RecyclerColorBinding

class ColorAdapter(
    private val colors: List<ColorString>,
    private val selectedColor: ColorString?,
    var defaultColor: ColorString?,
    private val listener: ItemListener,
) : RecyclerView.Adapter<ColorVH>() {

    override fun getItemCount() = colors.size

    override fun onBindViewHolder(holder: ColorVH, position: Int) {
        val color = colors[position]
        holder.bind(color, color == selectedColor, color == defaultColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerColorBinding.inflate(inflater, parent, false)
        return ColorVH(binding, listener)
    }
}

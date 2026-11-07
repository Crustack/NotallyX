package com.philkes.notallyx.core.ui.views

import android.R
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.philkes.notallyx.core.ui.extensions.toDp

class TextWithIconAdapter<T>(
    context: Context,
    objects: List<T>,
    private val getText: (T) -> String,
    private val getIconResId: (T) -> Int,
) : ArrayAdapter<T>(context, R.layout.simple_list_item_1, R.id.text1, objects) {

    private var initialPaddingStart: Int? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (super.getView(position, convertView, parent) as TextView).apply {
            if (initialPaddingStart == null) {
                initialPaddingStart = paddingStart
            }
            val item = getItem(position)!!
            setCompoundDrawablesRelativeWithIntrinsicBounds(getIconResId(item), 0, 0, 0)
            setPaddingRelative(30.toDp, paddingTop, paddingEnd, paddingBottom)
            text = getText(item)
            compoundDrawablePadding = 10.toDp
        }
    }
}

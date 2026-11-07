package com.philkes.notallyx.feature.main.adapter

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.philkes.notallyx.core.ui.components.SectionHeaderItem
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Header

class HeaderVH(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

    init {
        val params = composeView.layoutParams
        if (params is StaggeredGridLayoutManager.LayoutParams) {
            params.isFullSpan = true
        }
    }

    fun bind(header: Header) {
        composeView.setContent { NotallyXTheme { SectionHeaderItem(title = header.label) } }
    }

    companion object {
        fun create(parent: ViewGroup): HeaderVH {
            val composeView =
                ComposeView(parent.context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }
            return HeaderVH(composeView)
        }
    }
}

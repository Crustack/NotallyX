package com.philkes.notallyx.presentation.view.note

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.components.ErrorStateCard
import com.philkes.notallyx.utils.FileError

class ErrorVH(private val composeView: ComposeView) : RecyclerView.ViewHolder(composeView) {

    fun bind(error: FileError) {
        composeView.setContent {
            NotallyXTheme { ErrorStateCard(name = error.name, description = error.description) }
        }
    }

    companion object {
        fun create(parent: ViewGroup): ErrorVH {
            val composeView =
                ComposeView(parent.context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }
            return ErrorVH(composeView)
        }
    }
}

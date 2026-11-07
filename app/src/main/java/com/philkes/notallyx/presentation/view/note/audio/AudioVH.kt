package com.philkes.notallyx.presentation.view.note.audio

import android.text.format.DateUtils
import android.view.ViewGroup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.presentation.getContrastFontColor
import com.philkes.notallyx.presentation.theme.NotallyXTheme
import com.philkes.notallyx.presentation.view.components.AudioItemRow
import java.text.DateFormat

class AudioVH(
    private val composeView: ComposeView,
    private val onClick: (Int) -> Unit,
    private val formatter: DateFormat,
) : RecyclerView.ViewHolder(composeView) {

    fun bind(audio: Audio, color: Int?) {
        val dateText = formatter.format(audio.timestamp)
        val lengthText = audio.duration?.let { DateUtils.formatElapsedTime(it / 1000) } ?: "-"
        val contrastColor = color?.let { Color(composeView.context.getContrastFontColor(it)) }

        composeView.setContent {
            NotallyXTheme {
                AudioItemRow(
                    dateText = dateText,
                    lengthText = lengthText,
                    contentColor = contrastColor ?: Color.Unspecified,
                    onClick = { onClick(absoluteAdapterPosition) },
                )
            }
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onClick: (Int) -> Unit,
            formatter: DateFormat,
        ): AudioVH {
            val composeView =
                ComposeView(parent.context).apply {
                    layoutParams =
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                }
            return AudioVH(composeView, onClick, formatter)
        }
    }
}

package com.philkes.notallyx.feature.media.preview

// import com.philkes.notallyx.setControlsContrastColorForAllViews
import androidx.recyclerview.widget.RecyclerView
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.ui.extensions.setControlsContrastColorForAllViews
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.databinding.RecyclerPreviewFileBinding

class PreviewFileVH(
    private val binding: RecyclerPreviewFileBinding,
    onClick: (position: Int) -> Unit,
    onLongClick: (position: Int) -> Boolean,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.FileName.apply {
            setOnClickListener { onClick(absoluteAdapterPosition) }
            setOnLongClickListener { onLongClick(absoluteAdapterPosition) }
        }
    }

    fun bind(fileAttachment: FileAttachment, color: Int?) {
        binding.FileName.apply {
            text = fileAttachment.originalName
            setChipIconResource(getIconForMimeType(fileAttachment.mimeType))
            color?.let { setControlsContrastColorForAllViews(it) }
        }
    }

    private fun getIconForMimeType(mimeType: String): Int {
        return when {
            mimeType.startsWith("image/") -> R.drawable.add_images
            mimeType.startsWith("video/") -> R.drawable.video
            mimeType.startsWith("audio/") -> R.drawable.record_audio
            mimeType.startsWith(MIME_TYPE_ZIP) -> R.drawable.archive
            else -> R.drawable.text_file
        }
    }
}

package com.philkes.notallyx.presentation.view.note.action

import android.os.Build
import androidx.annotation.ColorInt
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.note.NoteActionHandler

/** BottomSheet inside note for adding files, recording audio. */
class AddBottomSheet(handler: NoteActionHandler, @ColorInt color: Int?) :
    ActionBottomSheet(createActions(handler), color) {

    companion object {
        const val TAG = "AddBottomSheet"

        fun createActions(actionHandler: NoteActionHandler) =
            listOf(
                Action(R.string.add_images, R.drawable.add_images) { _ ->
                    actionHandler.addImages()
                    true
                },
                Action(R.string.attach_file, R.drawable.text_file) { _ ->
                    actionHandler.attachFiles()
                    true
                },
            ) +
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    listOf(
                        Action(R.string.record_audio, R.drawable.record_audio) { _ ->
                            actionHandler.recordAudio()
                            true
                        }
                    )
                else listOf()
    }
}

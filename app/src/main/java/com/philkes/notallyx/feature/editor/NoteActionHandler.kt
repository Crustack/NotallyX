package com.philkes.notallyx.feature.editor

import androidx.compose.ui.text.TextRange
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment

interface NoteActionHandler {
    fun pinToStatus() {}

    fun recordAudio() {}

    fun startRecordAudioActivity() {}

    fun addImages() {}

    fun attachFiles() {}

    fun addNoteLink() {}

    fun updateNoteLink(range: TextRange) {}

    fun viewImage(image: FileAttachment) {}

    fun viewFile(fileAttachment: FileAttachment) {}

    fun playAudio(audio: Audio) {}

    fun handleAction(action: EditAction) {}

    fun handleListAction(action: EditListAction) {}
}

package com.philkes.notallyx.feature.editor

import android.app.Activity
import androidx.compose.ui.text.TextRange
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.core.ui.extensions.checkNotificationPermission
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.feature.main.MainActiveDialog
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager

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

    companion object {
        const val REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS = 1
    }
}

fun Activity.refreshStatusBarPin(note: BaseNote) {
    val refresh = {
        if (note.isPinnedToStatus) {
            PinnedNotificationManager.notify(this, note)
        } else {
            PinnedNotificationManager.cancel(this, note.id)
        }
    }
    if (note.isPinnedToStatus) {
        checkNotificationPermission(
            NoteActionHandler.REQUEST_NOTIFICATION_PERMISSION_PIN_TO_STATUS,
            onShowRationale = { msgResId, onConfirm ->
                if (this is MainActivity) {
                    mainActivityViewModel.activeDialog.value =
                        MainActiveDialog.Confirm(
                            message = getString(msgResId),
                            positiveButtonText = getString(R.string.continue_),
                            onConfirm = onConfirm,
                        )
                }
            },
        ) {
            refresh()
        }
    } else {
        refresh()
    }
}

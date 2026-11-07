package com.philkes.notallyx.feature.editor

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.text.TextRange
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.EditListAction
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.feature.reminder.NoOpReminderScheduler
import com.philkes.notallyx.feature.reminder.ReminderScheduler
import org.koin.core.context.GlobalContext

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
    val scheduler =
        GlobalContext.getOrNull()?.getOrNull<ReminderScheduler>() ?: NoOpReminderScheduler()
    if (note.isPinnedToStatus) {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        ) {
            scheduler.pinNoteToNotificationBar(note)
        }
    } else {
        scheduler.unpinNoteFromNotificationBar(note.id)
    }
}

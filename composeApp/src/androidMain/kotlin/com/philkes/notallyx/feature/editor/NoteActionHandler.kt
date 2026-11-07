package com.philkes.notallyx.feature.editor

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.reminder.NoOpReminderScheduler
import com.philkes.notallyx.feature.reminder.ReminderScheduler
import org.koin.core.context.GlobalContext

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

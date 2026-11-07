package com.philkes.notallyx.activity.main

import android.app.Notification
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.feature.editor.refreshStatusBarPin
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.reminder.PinnedNotificationManager
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.waitUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest : UiTestBase() {

    @Test
    fun restorePinnedNotifications() {
        var pinned1 =
            createBaseNote(
                title = "Pinned 1",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        var pinned2 =
            createBaseNote(
                title = "Pinned 2",
                body = "Body",
                labels = listOf("label"),
                isPinnedToStatus = true,
            )
        runBlocking {
            withContext(Dispatchers.Main) {
                val baseNoteDao = database.getBaseNoteDao()
                pinned1 = pinned1.copy(id = baseNoteDao.insert(pinned1))
                pinned2 = pinned2.copy(id = baseNoteDao.insert(pinned2))
                baseNoteDao.insert(
                    createBaseNote(title = "Not Pinned", body = "Body", labels = listOf("label"))
                )
            }
        }
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            activity.refreshStatusBarPin(pinned1)
            activity.refreshStatusBarPin(pinned2)
        }

        waitUntil(10_000) { PinnedNotificationManager.getPinnedNotifications(context).size == 2 }
        val pinnedNotifications = PinnedNotificationManager.getPinnedNotifications(context)
        assertTrue(
            "Notification for every pinned Note shown",
            setOf(pinned1, pinned2).all { pinnedNote ->
                pinnedNotifications.any {
                    it!!.id.toLong() == pinnedNote.id &&
                        pinnedNote.title.contains(
                            it.notification.extras.getString(Notification.EXTRA_TITLE)!!
                        ) &&
                        pinnedNote.body.contains(
                            it.notification.extras.getString(Notification.EXTRA_TEXT)!!
                        )
                }
            },
        )

        scenario.close()
    }
}

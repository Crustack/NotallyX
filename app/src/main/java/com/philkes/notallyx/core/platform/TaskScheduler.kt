package com.philkes.notallyx.core.platform

import android.content.Context
import android.content.ContextWrapper
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.philkes.notallyx.cancelAutoRemoveOldDeletedNotes
import com.philkes.notallyx.feature.migration.backup.CleanupMissingAttachmentsWorker
import com.philkes.notallyx.feature.migration.backup.cancelAutoBackup
import com.philkes.notallyx.feature.migration.backup.scheduleAutoBackup
import com.philkes.notallyx.scheduleAutoRemoveOldDeletedNotes

/**
 * Platform contract for scheduling background maintenance tasks (periodic backups, auto-removing
 * old deleted notes, and cleaning up missing attachments).
 */
interface TaskScheduler {
    fun schedulePeriodicBackup(periodInDays: Long)

    fun cancelPeriodicBackup()

    fun scheduleAutoRemoveDeletedNotes()

    fun cancelAutoRemoveDeletedNotes()

    fun triggerMissingAttachmentsCleanup()
}

class NoOpTaskScheduler : TaskScheduler {
    override fun schedulePeriodicBackup(periodInDays: Long) {}

    override fun cancelPeriodicBackup() {}

    override fun scheduleAutoRemoveDeletedNotes() {}

    override fun cancelAutoRemoveDeletedNotes() {}

    override fun triggerMissingAttachmentsCleanup() {}
}

class AndroidTaskScheduler(private val context: Context) : TaskScheduler {
    private fun workManagerOrNull(): WorkManager? = runCatching {
        WorkManager.getInstance(context)
    }
        .getOrNull()

    private fun contextWrapper(): ContextWrapper =
        (context as? ContextWrapper) ?: ContextWrapper(context)

    override fun schedulePeriodicBackup(periodInDays: Long) {
        workManagerOrNull()?.scheduleAutoBackup(contextWrapper(), periodInDays)
    }

    override fun cancelPeriodicBackup() {
        workManagerOrNull()?.cancelAutoBackup()
    }

    override fun scheduleAutoRemoveDeletedNotes() {
        workManagerOrNull()?.scheduleAutoRemoveOldDeletedNotes(contextWrapper())
    }

    override fun cancelAutoRemoveDeletedNotes() {
        workManagerOrNull()?.cancelAutoRemoveOldDeletedNotes()
    }

    override fun triggerMissingAttachmentsCleanup() {
        val request = OneTimeWorkRequestBuilder<CleanupMissingAttachmentsWorker>().build()
        workManagerOrNull()?.enqueue(request)
    }
}

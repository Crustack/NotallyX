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

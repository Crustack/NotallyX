package com.philkes.notallyx.core.platform

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

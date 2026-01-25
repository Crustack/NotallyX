package com.philkes.notallyx.utils.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class CleanupMissingAttachmentsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CLEANUP_MISSING_ATTACHMENTS) {
            val request = OneTimeWorkRequestBuilder<CleanupMissingAttachmentsWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    companion object {
        const val ACTION_CLEANUP_MISSING_ATTACHMENTS =
            "com.philkes.notallyx.action.CLEANUP_MISSING_ATTACHMENTS"
    }
}

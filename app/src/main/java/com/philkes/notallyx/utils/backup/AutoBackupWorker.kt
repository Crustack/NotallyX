package com.philkes.notallyx.utils.backup

import android.content.Context
import android.content.ContextWrapper
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoBackupWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return (context.applicationContext as ContextWrapper).createBackup()
    }
}

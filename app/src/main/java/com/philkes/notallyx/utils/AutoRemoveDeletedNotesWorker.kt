package com.philkes.notallyx.utils

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import java.util.Date
import kotlin.collections.isNotEmpty

class AutoRemoveDeletedNotesWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return (context.applicationContext as ContextWrapper).removeOldDeletedNotes()
    }

    companion object {
        const val TAG = "AutoRemoveDeletedNotes"
    }
}

private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

suspend fun ContextWrapper.removeOldDeletedNotes(): ListenableWorker.Result {
    val app = applicationContext as Application
    val preferences = NotallyXPreferences.getInstance(app)
    val days = preferences.autoRemoveDeletedNotesAfterDays.value
    if (days <= 0) return ListenableWorker.Result.success()

    val now = System.currentTimeMillis()
    val before = now - (days * ONE_DAY_MILLIS)

    Log.d(
        AutoRemoveDeletedNotesWorker.TAG,
        "Removing notes that have been deleted for $days days or more (since: ${Date(before).format()})",
    )

    val database = NotallyDatabase.getDatabase(this, false).value
    val baseNoteDao = database.getBaseNoteDao()

    return try {
        val ids = baseNoteDao.getDeletedNoteIdsOlderThan(before)
        if (ids.isNotEmpty()) {
            val imageStrings = baseNoteDao.getImages(ids)
            val fileStrings = baseNoteDao.getFiles(ids)
            val audioStrings = baseNoteDao.getAudios(ids)

            val images = imageStrings.flatMap { json -> Converters.jsonToFiles(json) }
            val files = fileStrings.flatMap { json -> Converters.jsonToFiles(json) }
            val audios = audioStrings.flatMap { json -> Converters.jsonToAudios(json) }

            baseNoteDao.delete(ids)
            deleteAttachments(images + files + audios, ids)
            log(
                AutoRemoveDeletedNotesWorker.TAG,
                msg = "Auto removed ${ids.size} deleted notes after $days days",
            )
        }
        ListenableWorker.Result.success()
    } catch (e: Exception) {
        log(
            AutoRemoveDeletedNotesWorker.TAG,
            msg = "Auto remove deleted notes after $days days failed",
            throwable = e,
        )
        ListenableWorker.Result.failure()
    }
}

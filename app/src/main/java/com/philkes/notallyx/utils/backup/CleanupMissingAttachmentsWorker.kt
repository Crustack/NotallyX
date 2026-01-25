package com.philkes.notallyx.utils.backup

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.utils.SUBFOLDER_AUDIOS
import com.philkes.notallyx.utils.SUBFOLDER_FILES
import com.philkes.notallyx.utils.SUBFOLDER_IMAGES
import com.philkes.notallyx.utils.createChannelIfNotExists
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.resolveAttachmentFile

/** Scans all notes and removes references to attachments whose underlying files are missing. */
class CleanupMissingAttachmentsWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = ContextWrapper(applicationContext)
        val database = NotallyDatabase.getDatabase(ctx, observePreferences = false).value
        val dao = database.getBaseNoteDao()

        var removedImages = 0
        var removedFiles = 0
        var removedAudios = 0
        var affectedNotes = 0

        val notes = dao.getAll()
        ctx.log(TAG, "Scanning ${notes.size} notes to check for missing attachments...")
        notes.forEach { note ->
            val originalImages = ArrayList(note.images)
            val originalFiles = ArrayList(note.files)
            val originalAudios = ArrayList(note.audios)

            val filteredImages =
                originalImages.filter { fa: FileAttachment ->
                    val file = ctx.resolveAttachmentFile(SUBFOLDER_IMAGES, fa.localName)
                    file != null && file.exists()
                }
            val filteredFiles =
                originalFiles.filter { fa: FileAttachment ->
                    val file = ctx.resolveAttachmentFile(SUBFOLDER_FILES, fa.localName)
                    file != null && file.exists()
                }
            val filteredAudios =
                originalAudios.filter { au: Audio ->
                    val file = ctx.resolveAttachmentFile(SUBFOLDER_AUDIOS, au.name)
                    file != null && file.exists()
                }

            val imgRemoved = originalImages.size - filteredImages.size
            val fileRemoved = originalFiles.size - filteredFiles.size
            val audRemoved = originalAudios.size - filteredAudios.size

            if (imgRemoved + fileRemoved + audRemoved > 0) {
                affectedNotes++
                if (imgRemoved > 0) {
                    removedImages += imgRemoved
                    dao.updateImages(note.id, filteredImages)
                }
                if (fileRemoved > 0) {
                    removedFiles += fileRemoved
                    dao.updateFiles(note.id, filteredFiles)
                }
                if (audRemoved > 0) {
                    removedAudios += audRemoved
                    dao.updateAudios(note.id, filteredAudios)
                }
            }
        }
        ctx.log(TAG, "Cleaned up missing attachments from $affectedNotes notes")

        postCompletionNotification(removedImages + removedFiles + removedAudios, affectedNotes)
        return Result.success()
    }

    private fun postCompletionNotification(totalRemoved: Int, affectedNotes: Int) {
        val ctx = ContextWrapper(applicationContext)
        val manager = ctx.getSystemService<NotificationManager>() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createChannelIfNotExists(WORKER_NOTIFICATION_CHANNEL_ID)
        }
        val notification =
            NotificationCompat.Builder(ctx, WORKER_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.delete)
                .setContentTitle(ctx.getString(R.string.cleanup_finished_title))
                .setContentText(
                    ctx.getString(R.string.cleanup_finished_summary, totalRemoved, affectedNotes)
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        manager.notify(WORKER_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "CleanupMissingAttachmentsWorker"
        // Reuse the same channel name as backups for simplicity
        private const val WORKER_NOTIFICATION_CHANNEL_ID = "AutoBackups"
        private const val WORKER_NOTIFICATION_ID = 123416
    }
}

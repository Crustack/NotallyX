package com.philkes.notallyx.utils.backup

import com.philkes.notallyx.data.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.utils.SUBFOLDER_AUDIOS
import com.philkes.notallyx.utils.SUBFOLDER_FILES
import com.philkes.notallyx.utils.SUBFOLDER_IMAGES
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps previous states of the "auto backup on save" ZIP inside the ZIP itself, so that a broken or
 * accidentally emptied database can never overwrite the last good backup.
 *
 * Layout of the backup directory (= content of the ZIP):
 * ```
 * NotallyDatabase                           current state, the only one used by the importer
 * Images/ Files/ Audios/                    attachments of the current state
 * History/NotallyDatabase_2026-10-27        previous database states, at most one per day
 * History/Media/Images|Files|Audios/        attachments only needed by the History states
 * ```
 *
 * Rules:
 * - Before the root database is replaced, the previous one is stored as the snapshot of the day it
 *   was written. A later backup on the same day replaces that day's snapshot.
 * - Databases without any notes are never stored as snapshot and never replace one.
 * - Only the newest [merge] `maxSnapshots` snapshots are kept, but the newest snapshot that still
 *   contains notes is never removed.
 * - Attachments that are no longer part of the current state are moved to `History/Media` and are
 *   kept as long as one of the remaining snapshots references them.
 *
 * Contains no Android dependencies so it can be tested with plain JVM unit tests.
 */
object BackupHistory {

    const val HISTORY_FOLDER = "History"
    const val MEDIA_FOLDER = "Media"
    const val SNAPSHOT_PREFIX = "${DATABASE_NAME}_"

    val ATTACHMENT_FOLDERS = listOf(SUBFOLDER_IMAGES, SUBFOLDER_FILES, SUBFOLDER_AUDIOS)

    private val SNAPSHOT_NAME_REGEX = Regex("${Regex.escape(SNAPSHOT_PREFIX)}\\d{4}-\\d{2}-\\d{2}")

    private val SNAPSHOT_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)

    /**
     * @param noteCount amount of notes in the database
     * @param attachments referenced attachments as relative paths, e.g. `Images/<localName>`
     */
    data class SnapshotInfo(val noteCount: Int, val attachments: Set<String>)

    /** Reads a (decrypted) database file. Throws if the file cannot be read. */
    fun interface SnapshotInspector {
        fun inspect(databaseFile: File): SnapshotInfo
    }

    data class MergeResult(
        val snapshots: List<String>,
        val removedSnapshots: List<String>,
        val movedToMedia: List<String>,
        val removedMedia: List<String>,
        val skippedEmptySnapshot: Boolean,
    ) {
        override fun toString(): String =
            "snapshots=$snapshots, removedSnapshots=$removedSnapshots, " +
                "movedToMedia=${movedToMedia.size}, removedMedia=${removedMedia.size}, " +
                "skippedEmptySnapshot=$skippedEmptySnapshot"
    }

    fun snapshotName(timestamp: Long): String =
        synchronized(SNAPSHOT_DATE_FORMAT) {
            SNAPSHOT_PREFIX + SNAPSHOT_DATE_FORMAT.format(Date(timestamp))
        }

    /**
     * Carries the history of [previousDir] over into [newDir] and adds the previous state to it.
     *
     * @param previousDir extracted content of the previous backup ZIP, `null` if there is none
     * @param newDir content of the new backup, already containing the current database and
     *   attachments. Its `History` folder is created/updated by this function.
     * @param previousTimestamp when the previous backup was written
     * @param maxSnapshots amount of daily snapshots to keep (at least 1)
     */
    fun merge(
        previousDir: File?,
        newDir: File,
        previousTimestamp: Long,
        maxSnapshots: Int,
        inspector: SnapshotInspector,
    ): MergeResult {
        val historyDir = File(newDir, HISTORY_FOLDER)
        val mediaDir = File(historyDir, MEDIA_FOLDER)

        // 1. Carry over the existing history
        val previousHistory = previousDir?.let { File(it, HISTORY_FOLDER) }
        if (previousHistory != null && previousHistory.isDirectory) {
            historyDir.mkdirs()
            previousHistory.copyRecursively(historyDir, overwrite = true)
        }

        // 2. Store the previous database as snapshot of its day
        var skippedEmptySnapshot = false
        val previousDatabase =
            previousDir?.let { File(it, DATABASE_NAME) }?.takeIf { it.isFile && it.length() > 0 }
        if (previousDatabase != null) {
            val previousInfo = runCatching { inspector.inspect(previousDatabase) }.getOrNull()
            val target = File(historyDir, snapshotName(previousTimestamp))
            val targetHasNotes =
                target.isFile &&
                    (runCatching { inspector.inspect(target) }.getOrNull()?.noteCount ?: 0) > 0
            when {
                previousInfo != null && previousInfo.noteCount == 0 -> skippedEmptySnapshot = true
                // Never replace a good snapshot of the same day with an unreadable database
                previousInfo == null && targetHasNotes -> {}
                else -> {
                    // Unreadable databases are kept as well, they might still be recoverable
                    historyDir.mkdirs()
                    previousDatabase.copyTo(target, overwrite = true)
                }
            }
        }

        // 3. Attachments that are not part of the current state anymore go to History/Media
        val movedToMedia = mutableListOf<String>()
        if (previousDir != null) {
            ATTACHMENT_FOLDERS.forEach { folder ->
                File(previousDir, folder)
                    .listFiles()
                    ?.filter { it.isFile }
                    ?.forEach { file ->
                        val path = "$folder/${file.name}"
                        val target = File(mediaDir, path)
                        if (!File(newDir, path).exists() && !target.exists()) {
                            file.copyTo(target)
                            movedToMedia.add(path)
                        }
                    }
            }
        }
        // Attachments that are part of the current state again don't need a second copy
        ATTACHMENT_FOLDERS.forEach { folder ->
            File(mediaDir, folder).listFiles()?.forEach { file ->
                if (File(newDir, "$folder/${file.name}").exists()) {
                    file.delete()
                }
            }
        }

        // 4. Remove old snapshots
        val snapshots =
            historyDir
                .listFiles { file -> file.isFile && SNAPSHOT_NAME_REGEX.matches(file.name) }
                ?.sortedByDescending { it.name } ?: emptyList()
        val infos = snapshots.associateWith { runCatching { inspector.inspect(it) }.getOrNull() }
        val keep = snapshots.take(maxSnapshots.coerceAtLeast(1)).toMutableList()
        if (keep.none { (infos[it]?.noteCount ?: 0) > 0 }) {
            snapshots.firstOrNull { (infos[it]?.noteCount ?: 0) > 0 }?.let { keep.add(it) }
        }
        val removedSnapshots = (snapshots - keep.toSet()).map { it.name }
        (snapshots - keep.toSet()).forEach { it.delete() }

        // 5. Remove media that no remaining snapshot needs anymore. If any snapshot cannot be
        // read, nothing is removed since its references are unknown.
        val removedMedia = mutableListOf<String>()
        if (keep.all { infos[it] != null }) {
            val referenced = keep.flatMap { infos[it]!!.attachments }.toSet()
            ATTACHMENT_FOLDERS.forEach { folder ->
                File(mediaDir, folder).listFiles()?.forEach { file ->
                    val path = "$folder/${file.name}"
                    if (path !in referenced && file.delete()) {
                        removedMedia.add(path)
                    }
                }
            }
        }
        ATTACHMENT_FOLDERS.forEach { folder -> File(mediaDir, folder).deleteIfEmpty() }
        mediaDir.deleteIfEmpty()
        historyDir.deleteIfEmpty()

        return MergeResult(
            snapshots = keep.map { it.name }.sortedDescending(),
            removedSnapshots = removedSnapshots,
            movedToMedia = movedToMedia,
            removedMedia = removedMedia,
            skippedEmptySnapshot = skippedEmptySnapshot,
        )
    }

    private fun File.deleteIfEmpty() {
        if (isDirectory && list()?.isEmpty() == true) {
            delete()
        }
    }
}

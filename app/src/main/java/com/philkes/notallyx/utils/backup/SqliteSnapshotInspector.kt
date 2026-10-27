package com.philkes.notallyx.utils.backup

import android.database.sqlite.SQLiteDatabase
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.utils.SUBFOLDER_AUDIOS
import com.philkes.notallyx.utils.SUBFOLDER_FILES
import com.philkes.notallyx.utils.SUBFOLDER_IMAGES
import java.io.File
import org.json.JSONArray

/**
 * Reads note count and referenced attachments of an unencrypted database copy, as stored in backup
 * ZIPs. Uses the strict JSON converters so that unparsable attachment data throws instead of
 * silently returning no references (which could lead to History media being removed).
 */
object SqliteSnapshotInspector : BackupHistory.SnapshotInspector {

    override fun inspect(databaseFile: File): BackupHistory.SnapshotInfo {
        // SQLite may create -wal/-shm/-journal files next to the opened database. They must not
        // end up in the backup, therefore a temporary copy is inspected.
        val copy = File.createTempFile("snapshot", ".db")
        try {
            databaseFile.copyTo(copy, overwrite = true)
            return inspectCopy(copy)
        } finally {
            listOf("", "-wal", "-shm", "-journal").forEach { File(copy.path + it).delete() }
        }
    }

    private fun inspectCopy(databaseFile: File): BackupHistory.SnapshotInfo {
        SQLiteDatabase.openDatabase(
                databaseFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS,
            )
            .use { database ->
                val noteCount =
                    database.rawQuery("SELECT COUNT(*) FROM BaseNote", null).use { cursor ->
                        if (cursor.moveToFirst()) cursor.getInt(0) else 0
                    }
                val attachments = mutableSetOf<String>()
                database.rawQuery("SELECT images, files, audios FROM BaseNote", null).use { cursor
                    ->
                    while (cursor.moveToNext()) {
                        cursor.getString(0)?.let { json ->
                            Converters.jsonToFiles(JSONArray(json)).forEach {
                                attachments.add("$SUBFOLDER_IMAGES/${it.localName}")
                            }
                        }
                        cursor.getString(1)?.let { json ->
                            Converters.jsonToFiles(JSONArray(json)).forEach {
                                attachments.add("$SUBFOLDER_FILES/${it.localName}")
                            }
                        }
                        cursor.getString(2)?.let { json ->
                            Converters.jsonToAudios(JSONArray(json)).forEach {
                                attachments.add("$SUBFOLDER_AUDIOS/${it.name}")
                            }
                        }
                    }
                }
                return BackupHistory.SnapshotInfo(noteCount, attachments)
            }
    }
}

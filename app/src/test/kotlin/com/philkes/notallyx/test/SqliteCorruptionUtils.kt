package com.philkes.notallyx.test

import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SimpleSQLiteQuery
import com.philkes.notallyx.data.NotallyDatabase
import java.io.File
import java.io.RandomAccessFile

/** Length of the `SQLite format 3\u0000` magic at the start of every SQLite database file. */
private const val SQLITE_HEADER_MAGIC_LENGTH = 16

/**
 * Page size from the SQLite header (bytes 16..17, big-endian). The value `1` is a special encoding
 * for 65536.
 */
fun File.readSqlitePageSize(): Int {
    RandomAccessFile(this, "r").use { raf ->
        raf.seek(16)
        val high = raf.read()
        val low = raf.read()
        require(high >= 0 && low >= 0) { "$name is too small to contain a SQLite header" }
        val encoded = (high shl 8) or low
        return if (encoded == 1) 65536 else encoded
    }
}

/**
 * Overwrites page [pageNumber] (1-based) with bytes that are not a valid b-tree page. `0xFF` is
 * none of SQLite's valid page types (2, 5, 10, 13), so the page is rejected as *malformed* as soon
 * as the b-tree is walked.
 *
 * Fails fast if the recipe does not actually produce a malformed image on the SQLite build in use.
 */
fun File.corruptPage(pageNumber: Int, pageSize: Int = readSqlitePageSize()) {
    require(pageNumber > 1) { "Page 1 holds the header, use destroySqliteHeaderMagic() instead" }
    val offset = (pageNumber - 1).toLong() * pageSize
    require(offset + pageSize <= length()) {
        "Page $pageNumber is beyond the end of $name (${length()} bytes, page size $pageSize)"
    }
    RandomAccessFile(this, "rw").use { raf ->
        raf.seek(offset)
        raf.write(ByteArray(pageSize) { 0xFF.toByte() })
        raf.fd.sync()
    }
    val integrityCheck = integrityCheckOnCopy()
    check(integrityCheck != "ok") {
        "Corrupting page $pageNumber did not produce a malformed database, integrity_check said: $integrityCheck"
    }
}

/** Destroys the `SQLite format 3` magic so the file cannot be opened as a database at all. */
fun File.destroySqliteHeaderMagic() {
    RandomAccessFile(this, "rw").use { raf ->
        raf.seek(0)
        raf.write(ByteArray(SQLITE_HEADER_MAGIC_LENGTH) { 0x00 })
        raf.fd.sync()
    }
    val integrityCheck = integrityCheckOnCopy()
    check(integrityCheck != "ok") {
        "Destroying the header magic did not produce an unreadable database, integrity_check said: $integrityCheck"
    }
}

/**
 * Runs `PRAGMA integrity_check` against a **copy** of this file, so neither the original nor the
 * verdict is affected by a corruption handler deleting anything. Returns `"ok"` for a healthy
 * database, otherwise the reported problem or the exception raised while opening.
 */
fun File.integrityCheckOnCopy(): String {
    val copy = File.createTempFile("$name-integrity-check", null)
    copyTo(copy, overwrite = true)
    return try {
        // A no-op error handler, otherwise the default one would delete the copy before reporting.
        SQLiteDatabase.openDatabase(
                copy.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
                DatabaseErrorHandler {},
            )
            .use { db ->
                db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    if (!cursor.moveToFirst()) return "integrity_check returned no rows"
                    buildList {
                            do {
                                add(cursor.getString(0))
                            } while (cursor.moveToNext())
                        }
                        .joinToString("; ")
                }
            }
    } catch (e: Exception) {
        "${e.javaClass.simpleName}: ${e.message}"
    } finally {
        copy.delete()
    }
}

/** `SELECT rootpage FROM sqlite_master WHERE name = ?` */
fun NotallyDatabase.rootPageOf(table: String): Int {
    val query =
        SimpleSQLiteQuery("SELECT rootpage FROM sqlite_master WHERE name = ?", arrayOf(table))
    val rootPage = getBaseNoteDao().query(query)
    check(rootPage > 1) { "Unexpected rootpage $rootPage for table '$table'" }
    return rootPage
}

/** Convenience over `<db>`, `<db>-wal`, `<db>-shm`. */
fun File.databaseFiles(): List<File> =
    listOf(this, File(parentFile, "$name-wal"), File(parentFile, "$name-shm"))

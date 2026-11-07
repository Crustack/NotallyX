package com.philkes.notallyx.test

import android.database.sqlite.SQLiteDatabase
import androidx.room3.RoomRawQuery
import com.philkes.notallyx.core.database.NotallyDatabase
import java.io.File
import java.io.RandomAccessFile

private const val SQLITE_HEADER_MAGIC_LENGTH = 16

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

fun File.integrityCheckOnCopy(): String {
    val copy = File.createTempFile("$name-integrity-check", null)
    copyTo(copy, overwrite = true)
    return try {
        SQLiteDatabase.openDatabase(
                copy.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ) {}
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

suspend fun NotallyDatabase.rootPageOf(table: String): Int {
    val query =
        RoomRawQuery(
            sql = "SELECT rootpage FROM sqlite_master WHERE name = ?",
            onBindStatement = { it.bindText(1, table) },
        )
    val rootPage = getBaseNoteDao().query(query)
    check(rootPage > 1) { "Unexpected rootpage $rootPage for table '$table'" }
    return rootPage
}

fun File.databaseFiles(): List<File> =
    listOf(this, File(parentFile, "$name-wal"), File(parentFile, "$name-shm"))

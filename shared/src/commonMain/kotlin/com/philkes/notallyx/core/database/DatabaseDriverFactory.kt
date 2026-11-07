package com.philkes.notallyx.core.database

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import com.philkes.notallyx.core.platform.PathProvider

const val NOTALLY_DATABASE_NAME = "NotallyDatabase"

/**
 * Abstracts Room 3 `SQLiteDriver` and builder creation so `NotallyDatabase` can be instantiated on
 * both Android (via `BackupSQLiteDriver` or `BundledSQLiteDriver`) and Desktop/JVM (via
 * `BundledSQLiteDriver`) without requiring a `ContextWrapper`.
 */
interface DatabaseDriverFactory {
    val pathProvider: PathProvider

    fun <T : RoomDatabase> createBuilder(
        databaseName: String = NOTALLY_DATABASE_NAME,
        dataInPublicFolder: Boolean = false,
        inMemory: Boolean = false,
    ): RoomDatabase.Builder<T>

    fun createDriver(
        databaseName: String = NOTALLY_DATABASE_NAME,
        dataInPublicFolder: Boolean = false,
    ): SQLiteDriver
}

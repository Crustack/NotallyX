package com.philkes.notallyx.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.PathProvider

class DesktopDatabaseDriverFactory(
    override val pathProvider: PathProvider = DesktopPathProvider()
) : DatabaseDriverFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : RoomDatabase> createBuilder(
        databaseName: String,
        dataInPublicFolder: Boolean,
        inMemory: Boolean,
    ): RoomDatabase.Builder<T> {
        val builder =
            if (inMemory || databaseName == ":memory:") {
                Room.inMemoryDatabaseBuilder<NotallyDatabase>()
            } else {
                val dbFile = pathProvider.getDatabaseFile(databaseName, dataInPublicFolder)
                dbFile.parentFile?.mkdirs()
                Room.databaseBuilder<NotallyDatabase>(name = dbFile.absolutePath)
            }
        return builder as RoomDatabase.Builder<T>
    }

    override fun createDriver(databaseName: String, dataInPublicFolder: Boolean): SQLiteDriver {
        return BundledSQLiteDriver()
    }
}

package com.philkes.notallyx.core.database

import android.content.ContextWrapper
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.PathProvider

/**
 * Abstracts Room 3 `SQLiteDriver` and builder creation so `NotallyDatabase` can be instantiated on
 * both Android (via `BackupSQLiteDriver` or `BundledSQLiteDriver`) and Desktop/JVM (via
 * `BundledSQLiteDriver`) without requiring a `ContextWrapper`.
 */
interface DatabaseDriverFactory {
    val pathProvider: PathProvider

    fun createBuilder(
        databaseName: String = NotallyDatabase.DATABASE_NAME,
        dataInPublicFolder: Boolean = false,
        inMemory: Boolean = false,
    ): RoomDatabase.Builder<NotallyDatabase>

    fun createDriver(
        databaseName: String = NotallyDatabase.DATABASE_NAME,
        dataInPublicFolder: Boolean = false,
    ): SQLiteDriver
}

class AndroidDatabaseDriverFactory(
    private val context: ContextWrapper,
    override val pathProvider: PathProvider = AndroidPathProvider(context),
    private val openHelperFactoryProvider: (() -> SupportSQLiteOpenHelper.Factory)? = null,
) : DatabaseDriverFactory {

    override fun createBuilder(
        databaseName: String,
        dataInPublicFolder: Boolean,
        inMemory: Boolean,
    ): RoomDatabase.Builder<NotallyDatabase> {
        return if (inMemory || databaseName == ":memory:") {
            Room.inMemoryDatabaseBuilder<NotallyDatabase>(context)
        } else {
            val dbName =
                if (dataInPublicFolder) {
                    pathProvider
                        .getDatabaseFile(databaseName, dataInPublicFolder = true)
                        .absolutePath
                } else {
                    databaseName
                }
            Room.databaseBuilder<NotallyDatabase>(context, dbName)
        }
    }

    override fun createDriver(databaseName: String, dataInPublicFolder: Boolean): SQLiteDriver {
        val factory = openHelperFactoryProvider?.invoke() ?: BackupOpenHelperFactory(context)
        return BackupSQLiteDriver(context, factory)
    }
}

class JvmBundledDatabaseDriverFactory(override val pathProvider: PathProvider) :
    DatabaseDriverFactory {

    override fun createBuilder(
        databaseName: String,
        dataInPublicFolder: Boolean,
        inMemory: Boolean,
    ): RoomDatabase.Builder<NotallyDatabase> {
        return if (inMemory || databaseName == ":memory:") {
            Room.inMemoryDatabaseBuilder<NotallyDatabase>()
        } else {
            val dbFile = pathProvider.getDatabaseFile(databaseName, dataInPublicFolder)
            dbFile.parentFile?.mkdirs()
            Room.databaseBuilder<NotallyDatabase>(name = dbFile.absolutePath)
        }
    }

    override fun createDriver(databaseName: String, dataInPublicFolder: Boolean): SQLiteDriver {
        return BundledSQLiteDriver()
    }
}

package com.philkes.notallyx.core.database

import android.content.ContextWrapper
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.PathProvider

class AndroidDatabaseDriverFactory(
    private val context: ContextWrapper,
    override val pathProvider: PathProvider = AndroidPathProvider(context),
    private val openHelperFactoryProvider: (() -> SupportSQLiteOpenHelper.Factory)? = null,
) : DatabaseDriverFactory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : RoomDatabase> createBuilder(
        databaseName: String,
        dataInPublicFolder: Boolean,
        inMemory: Boolean,
    ): RoomDatabase.Builder<T> {
        val builder =
            if (inMemory || databaseName == ":memory:") {
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
        return builder as RoomDatabase.Builder<T>
    }

    override fun createDriver(databaseName: String, dataInPublicFolder: Boolean): SQLiteDriver {
        val factory = openHelperFactoryProvider?.invoke() ?: BackupOpenHelperFactory(context)
        return BackupSQLiteDriver(context, factory)
    }
}

class JvmBundledDatabaseDriverFactory(override val pathProvider: PathProvider) :
    DatabaseDriverFactory {

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

package com.philkes.notallyx.test.fakes

import androidx.room3.RoomDatabase
import androidx.sqlite.SQLiteDriver
import com.philkes.notallyx.core.database.DatabaseDriverFactory
import com.philkes.notallyx.core.database.JvmBundledDatabaseDriverFactory
import com.philkes.notallyx.core.platform.DesktopPathProvider
import com.philkes.notallyx.core.platform.PathProvider
import java.io.File

class FakePathProvider(
    rootDir: File =
        File(System.getProperty("java.io.tmpdir"), "notallyx-test-${System.nanoTime()}").apply {
            mkdirs()
        }
) : PathProvider by DesktopPathProvider(baseDirOverride = rootDir) {
    val rootDirectory: File = rootDir
}

class FakeDatabaseDriverFactory(override val pathProvider: PathProvider = FakePathProvider()) :
    DatabaseDriverFactory {
    private val delegate = JvmBundledDatabaseDriverFactory(pathProvider)

    override fun <T : RoomDatabase> createBuilder(
        databaseName: String,
        dataInPublicFolder: Boolean,
        inMemory: Boolean,
    ): RoomDatabase.Builder<T> {
        return delegate.createBuilder(databaseName, dataInPublicFolder, inMemory)
    }

    override fun createDriver(databaseName: String, dataInPublicFolder: Boolean): SQLiteDriver {
        return delegate.createDriver(databaseName, dataInPublicFolder)
    }
}

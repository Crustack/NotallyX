package com.philkes.notallyx.data

import android.content.ContextWrapper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.philkes.notallyx.utils.backup.backupDatabaseFiles
import com.philkes.notallyx.utils.log

private const val TAG = "BackupOpenHelperFactory"

/**
 * Default [SupportSQLiteOpenHelper.Factory] that creates database dump before default factory
 * deletes the db.
 */
open class BackupOpenHelperFactory(
    private val app: ContextWrapper,
    private val delegate: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
) : SupportSQLiteOpenHelper.Factory {

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration
    ): SupportSQLiteOpenHelper =
        delegate.create(
            SupportSQLiteOpenHelper.Configuration(
                configuration.context,
                configuration.name,
                RecordingCallback(configuration.callback, app),
                configuration.useNoBackupDirectory,
                configuration.allowDataLossOnRecovery,
            )
        )

    open class RecordingCallback(
        private val delegate: SupportSQLiteOpenHelper.Callback,
        private val app: ContextWrapper,
    ) : SupportSQLiteOpenHelper.Callback(delegate.version) {

        override fun onConfigure(db: SupportSQLiteDatabase) = delegate.onConfigure(db)

        override fun onCreate(db: SupportSQLiteDatabase) = delegate.onCreate(db)

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onUpgrade(db, oldVersion, newVersion)

        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onDowngrade(db, oldVersion, newVersion)

        override fun onOpen(db: SupportSQLiteDatabase) = delegate.onOpen(db)

        override fun onCorruption(db: SupportSQLiteDatabase) {
            try {
                app.log(TAG, stackTrace = "Database was corrupted")
                app.backupDatabaseFiles()?.let { delegate.onCorruption(db) }
            } catch (e: Exception) {
                throw RuntimeException(
                    "Database was corrupted, please report this via an issue on Github",
                    e,
                )
            } finally {
                throw IllegalStateException(
                    "Database was corrupted, please report this via an issue on Github"
                )
            }
        }
    }
}

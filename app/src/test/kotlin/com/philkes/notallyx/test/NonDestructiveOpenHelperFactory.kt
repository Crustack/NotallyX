package com.philkes.notallyx.test

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mirrors the planned P0 fix: the very same [FrameworkSQLiteOpenHelperFactory] Room uses by
 * default, but with a [SupportSQLiteOpenHelper.Callback.onCorruption] that only *records* the event
 * instead of calling `SQLiteDatabase.deleteDatabase`. Everything else is delegated to Room's own
 * callback, so the only difference to the production configuration is the corruption handler.
 */
class NonDestructiveOpenHelperFactory(
    private val delegate: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()
) : SupportSQLiteOpenHelper.Factory {

    val corruptionReported = AtomicBoolean(false)

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration
    ): SupportSQLiteOpenHelper =
        delegate.create(
            SupportSQLiteOpenHelper.Configuration(
                configuration.context,
                configuration.name,
                RecordingCallback(configuration.callback, corruptionReported),
                configuration.useNoBackupDirectory,
                configuration.allowDataLossOnRecovery,
            )
        )

    private class RecordingCallback(
        private val delegate: SupportSQLiteOpenHelper.Callback,
        private val corruptionReported: AtomicBoolean,
    ) : SupportSQLiteOpenHelper.Callback(delegate.version) {

        override fun onConfigure(db: SupportSQLiteDatabase) = delegate.onConfigure(db)

        override fun onCreate(db: SupportSQLiteDatabase) = delegate.onCreate(db)

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onUpgrade(db, oldVersion, newVersion)

        override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) =
            delegate.onDowngrade(db, oldVersion, newVersion)

        override fun onOpen(db: SupportSQLiteDatabase) = delegate.onOpen(db)

        /** The whole point: report, never delete. */
        override fun onCorruption(db: SupportSQLiteDatabase) {
            corruptionReported.set(true)
        }
    }
}

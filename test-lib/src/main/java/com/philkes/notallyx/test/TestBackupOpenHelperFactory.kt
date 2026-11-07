package com.philkes.notallyx.test

import android.content.ContextWrapper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.philkes.notallyx.data.BackupOpenHelperFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Mirrors the planned P0 fix: the very same [FrameworkSQLiteOpenHelperFactory] Room uses by
 * default, but with a [SupportSQLiteOpenHelper.Callback.onCorruption] that *records* the event.
 * Everything else is delegated to Room's own callback, so the only difference to the production
 * configuration is the corruption handler.
 */
class TestBackupOpenHelperFactory(
    private val app: ContextWrapper,
    private val delegate: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
) : BackupOpenHelperFactory(app, delegate) {

    val corruptionReported = AtomicBoolean(false)

    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration
    ): SupportSQLiteOpenHelper =
        delegate.create(
            SupportSQLiteOpenHelper.Configuration(
                configuration.context,
                configuration.name,
                TestRecordingCallback(app, configuration.callback, corruptionReported),
                configuration.useNoBackupDirectory,
                configuration.allowDataLossOnRecovery,
            )
        )

    class TestRecordingCallback(
        app: ContextWrapper,
        delegate: SupportSQLiteOpenHelper.Callback,
        private val corruptionReported: AtomicBoolean,
    ) : RecordingCallback(delegate, app) {

        override fun onCorruption(db: SupportSQLiteDatabase) {
            corruptionReported.set(true)
            super.onCorruption(db)
        }
    }
}

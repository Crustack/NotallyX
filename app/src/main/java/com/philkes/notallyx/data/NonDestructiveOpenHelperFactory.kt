package com.philkes.notallyx.data

import android.content.ContextWrapper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.utils.backup.BACKUP_TIMESTAMP_FORMATTER
import com.philkes.notallyx.utils.getExternalMediaDirectory
import com.philkes.notallyx.utils.log
import java.io.File
import java.util.Date

private const val TAG = "NonDestructiveOpenHelperFactory"

/** Default [SupportSQLiteOpenHelper.Factory] deletes database on corruption. */
class NonDestructiveOpenHelperFactory(
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

    internal class RecordingCallback(
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
            app.log(TAG, stackTrace = "Database was corrupted")
            val timestamp = BACKUP_TIMESTAMP_FORMATTER.format(Date())
            val targetDir =
                try {
                    app.getExternalMediaDirectory().apply { mkdirs() }
                } catch (_: Exception) {
                    File(app.filesDir, "corrupted_backups").apply { mkdirs() }
                }
            targetDir.mkdirs()
            val dbFiles =
                if (NotallyXPreferences.getInstance(app).dataInPublicFolder.value) {
                    NotallyDatabase.getExternalDatabaseFiles(app)
                } else {
                    NotallyDatabase.getInternalDatabaseFiles(app)
                }
            dbFiles.forEach { dbFile ->
                if (dbFile.exists()) {
                    try {
                        val destination = File(targetDir, "${dbFile.name}_CORRUPTED_$timestamp")
                        app.log(
                            TAG,
                            msg = "Copying corrupted file ${dbFile.path} to ${destination.path}...",
                        )
                        dbFile.copyTo(destination, overwrite = true)
                    } catch (e: Exception) {
                        app.log(
                            TAG,
                            msg = "Failed to copy corrupted file ${dbFile.path}",
                            throwable = e,
                        )
                    }
                }
            }
        }
    }
}

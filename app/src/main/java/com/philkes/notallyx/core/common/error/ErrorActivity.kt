package com.philkes.notallyx.core.common.error

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.R.string.auto_backup_failed
import com.philkes.notallyx.R.string.crash_export_backup_failed
import com.philkes.notallyx.R.string.report_bug
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.common.extensions.copyToClipBoard
import com.philkes.notallyx.core.common.extensions.copyToLarge
import com.philkes.notallyx.core.common.extensions.exportCrashLogs
import com.philkes.notallyx.core.common.extensions.getLogsDir
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.reportBug
import com.philkes.notallyx.core.common.extensions.showErrorDialog
import com.philkes.notallyx.core.common.extensions.toMessage
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.extensions.viewLogs
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.exportedText
import com.philkes.notallyx.core.ui.extensions.restartApplication
import com.philkes.notallyx.core.ui.extensions.setCancelButton
import com.philkes.notallyx.core.ui.extensions.setupProgressDialog
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.FILE_TIMESTAMP_FORMAT
import com.philkes.notallyx.feature.migration.backup.backupDatabaseFiles
import com.philkes.notallyx.feature.migration.backup.copyDatabase
import com.philkes.notallyx.feature.migration.backup.exportAsZip
import com.philkes.notallyx.feature.migration.backup.exportRawDatabase
import com.philkes.notallyx.feature.migration.backup.importRawDatabase
import java.io.File
import java.util.Date
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity used when the app is about to crash. Implicitly used by
 * `cat.ereza:customactivityoncrash`.
 */
class ErrorActivity : AppCompatActivity() {

    private lateinit var exportBackupActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportDatabaseActivityResultLauncher: ActivityResultLauncher<Intent>
    private var backupPath: File? = null
    private var appLogs: File? = null
    private val backupProgress = MutableStateFlow<Progress?>(null)
    private val crashLogsLinkState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = NotallyXPreferences.getInstance(this)
        val stacktrace = CustomActivityOnCrash.getStackTraceFromIntent(intent)
        val exceptionTitle = stacktrace?.lines()?.firstOrNull()?.replaceFirst(":", ":\n") ?: ""
        val exceptionDetails = stacktrace?.lines()?.drop(1)?.joinToString("\n") ?: ""

        if (stacktrace != null) {
            application.log(TAG, stackTrace = stacktrace)
        }

        setContent {
            NotallyXTheme(theme = preferences.theme.value) {
                ErrorScreen(
                    exceptionTitle = exceptionTitle,
                    stackTrace = exceptionDetails,
                    crashLogsLinkText = crashLogsLinkState.value,
                    onRestartClick = {
                        CustomActivityOnCrash.restartApplication(
                            this@ErrorActivity,
                            CustomActivityOnCrash.getConfigFromIntent(intent)!!,
                        )
                    },
                    onReportBugClick = { reportBug(stacktrace) },
                    onExportBackupClick = { onExportBackupRequested(stacktrace) },
                    onViewLogsClick = { viewLogs() },
                    onCopyStackTraceClick = {
                        if (stacktrace != null) {
                            copyToClipBoard(stacktrace)
                        }
                    },
                    onCrashLogsLinkClick = {
                        appLogs?.let { viewFile(getUriForFile(it), ExportMimeType.TXT.mimeType) }
                    },
                )
            }
        }

        setupExportBackup(stacktrace)

        val now = Date()
        lifecycleScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                log(TAG, msg = "Creating crash database dump failed", throwable = throwable)
            }
        ) {
            backupPath = withContext(Dispatchers.IO) { application.backupDatabaseFiles(now) }
        }

        lifecycleScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                log(TAG, msg = "Creating crash logs dump failed", throwable = throwable)
            }
        ) {
            appLogs =
                withContext(Dispatchers.IO) {
                        CustomActivityOnCrash.getCustomCrashDataFromIntent(intent)?.toInt()?.let {
                            application.exportCrashLogs(it, now)
                        }
                    }
                    ?.also { crashLogsLinkState.value = getString(R.string.view_crash_logs) }
        }
    }

    private fun onExportBackupRequested(stacktrace: String?) {
        MaterialAlertDialogBuilder(this)
            .setMessage(
                getString(
                    R.string.crash_export_backup_message,
                    getString(R.string.continue_),
                    getString(report_bug),
                )
            )
            .setPositiveButton(R.string.continue_) { _, _ ->
                val intent =
                    Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .apply {
                            type = MIME_TYPE_ZIP
                            addCategory(Intent.CATEGORY_OPENABLE)
                            putExtra(
                                Intent.EXTRA_TITLE,
                                "NotallyX_Crash_Backup-${FILE_TIMESTAMP_FORMAT.format(Date())}",
                            )
                        }
                        .wrapWithChooser(this@ErrorActivity)
                exportBackupActivityResultLauncher.launch(intent)
            }
            .setCancelButton()
            .show()
    }

    private fun setupExportBackup(stacktrace: String?) {
        exportBackupActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        val preferences = NotallyXPreferences.getInstance(this)
                        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                            try {
                                DocumentsContract.deleteDocument(contentResolver, uri)
                                Log.d(TAG, "Successfully deleted empty backup file '$uri'")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to delete empty backup file '$uri'", e)
                            }
                            showErrorDialog(
                                throwable,
                                auto_backup_failed,
                                getString(
                                    R.string.crash_export_backup,
                                    getString(report_bug),
                                    getString(R.string.export_database),
                                ),
                                originalStacktrace = stacktrace,
                                neutralButtonTextResId = R.string.export_database,
                                neutralButtonClickListener = { _, _ ->
                                    val intent =
                                        Intent(Intent.ACTION_CREATE_DOCUMENT)
                                            .apply {
                                                type = "application/octet-stream"
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                                putExtra(
                                                    Intent.EXTRA_TITLE,
                                                    "NotallyX_Raw_Database-${
                                                        FILE_TIMESTAMP_FORMAT.format(
                                                            Date()
                                                        )
                                                    }.sqlite",
                                                )
                                            }
                                            .wrapWithChooser(this@ErrorActivity)
                                    exportDatabaseActivityResultLauncher.launch(intent)
                                },
                            )
                        }
                        lifecycleScope.launch(exceptionHandler) {
                            val exportedNotesAndAttachments =
                                withContext(Dispatchers.IO) {
                                    return@withContext application.exportAsZip(
                                        uri,
                                        password = preferences.backupPassword.value,
                                        backupProgress = backupProgress,
                                    )
                                }
                            val message = application.exportedText(exportedNotesAndAttachments)
                            application.showToast(message)
                        }
                    }
                }
            }
        exportDatabaseActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    result.data?.data?.let { uri ->
                        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                            try {
                                DocumentsContract.deleteDocument(contentResolver, uri)
                                Log.d(TAG, "Successfully deleted empty database file '$uri'")
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to delete empty database file '$uri'", e)
                            }
                            showErrorDialog(
                                throwable,
                                auto_backup_failed,
                                getString(crash_export_backup_failed, getString(report_bug)),
                                originalStacktrace = stacktrace,
                            )
                        }
                        lifecycleScope.launch(exceptionHandler) {
                            val exportException: Throwable? =
                                withContext(Dispatchers.IO) {
                                    try {
                                        application.exportRawDatabase(uri)
                                        null
                                    } catch (e: Exception) {
                                        log(
                                            TAG,
                                            "Could not checkpoint and export database, only export of raw files from internal and external db paths is available",
                                            throwable = e,
                                        )
                                        e
                                    }
                                }
                            if (exportException != null) {
                                showErrorDialog(
                                    exportException,
                                    auto_backup_failed,
                                    getString(
                                        R.string.crash_export_raw_backup_failed,
                                        backupPath ?: "null",
                                        getString(report_bug),
                                    ),
                                )
                                return@launch
                            }
                            application.showToast(
                                "${getString(R.string.exported)} ${getString(R.string.database)}"
                            )
                            MaterialAlertDialogBuilder(this@ErrorActivity)
                                .apply {
                                    setTitle(R.string.database)
                                    setMessage(R.string.reimport_database_message)
                                    setPositiveButton(R.string.reimport_database) { dialog, _ ->
                                        dialog.cancel()
                                        val exceptionHandler =
                                            CoroutineExceptionHandler { _, throwable ->
                                                showErrorDialog(
                                                    throwable,
                                                    R.string.reimport_database_failed,
                                                    getString(
                                                        crash_export_backup_failed,
                                                        getString(report_bug),
                                                    ),
                                                    originalStacktrace =
                                                        throwable.stackTraceToString(),
                                                )
                                            }
                                        lifecycleScope.launch(exceptionHandler) {
                                            val importResult =
                                                withContext(Dispatchers.IO) {
                                                    val (_, databaseCopy) =
                                                        copyDatabase(
                                                            suffix = "_BACKUP_BEFORE_REIMPORT"
                                                        )
                                                    databaseCopy.copyToLarge(
                                                        File(
                                                            getLogsDir(),
                                                            "${NotallyDatabase.DATABASE_NAME}_BACKUP_BEFORE_REIMPORT.sqlite",
                                                        ),
                                                        overwrite = true,
                                                        deleteSourceFile = true,
                                                    )
                                                    deleteDatabase(NotallyDatabase.DATABASE_NAME)
                                                    NotallyDatabase.clearInstance()
                                                    application.importRawDatabase(
                                                        uri,
                                                        false,
                                                        backupProgress,
                                                    )
                                                }
                                            MaterialAlertDialogBuilder(this@ErrorActivity)
                                                .setMessage(
                                                    this@ErrorActivity.toMessage(importResult)
                                                )
                                                .setPositiveButton(R.string.restart_app) { _, _ ->
                                                    restartApplication()
                                                }
                                                .setCancelButton()
                                                .show()
                                        }
                                    }
                                    setCancelButton()
                                }
                                .show()
                        }
                    }
                }
            }
        backupProgress.setupProgressDialog(this)
    }

    companion object {
        private const val TAG = "ErrorActivity"
    }
}

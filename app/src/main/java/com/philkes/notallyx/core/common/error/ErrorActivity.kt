package com.philkes.notallyx.core.common.error

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.philkes.notallyx.R
import com.philkes.notallyx.R.string.auto_backup_failed
import com.philkes.notallyx.R.string.crash_export_backup_failed
import com.philkes.notallyx.R.string.report_bug
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.common.extensions.copyToClipBoard
import com.philkes.notallyx.core.common.extensions.copyToLarge
import com.philkes.notallyx.core.common.extensions.getLogsDir
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.extensions.reportBug
import com.philkes.notallyx.core.common.extensions.showErrorDialog
import com.philkes.notallyx.core.common.extensions.toMessage
import com.philkes.notallyx.core.common.extensions.viewFile
import com.philkes.notallyx.core.common.extensions.viewLogs
import com.philkes.notallyx.core.common.extensions.wrapWithChooser
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.ui.extensions.exportedText
import com.philkes.notallyx.core.ui.extensions.restartApplication
import com.philkes.notallyx.core.ui.extensions.setupProgressDialog
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.FILE_TIMESTAMP_FORMAT
import com.philkes.notallyx.feature.migration.backup.copyDatabase
import com.philkes.notallyx.feature.migration.backup.exportAsZip
import com.philkes.notallyx.feature.migration.backup.exportRawDatabase
import com.philkes.notallyx.feature.migration.backup.importRawDatabase
import java.io.File
import java.util.Date
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Activity used when the app is about to crash. Implicitly used by
 * `cat.ereza:customactivityoncrash`.
 */
class ErrorActivity : AppCompatActivity() {

    private val viewModel: ErrorActivityViewModel by viewModel()

    private lateinit var exportBackupActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportDatabaseActivityResultLauncher: ActivityResultLauncher<Intent>

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
                val crashLogsLinkText = viewModel.crashLogsLinkText.collectAsStateWithLifecycle()
                val showExportBackupDialog =
                    viewModel.showExportBackupDialog.collectAsStateWithLifecycle()
                val showReimportDialog = viewModel.showReimportDialog.collectAsStateWithLifecycle()
                val importResultDialogMessage =
                    viewModel.importResultDialogMessage.collectAsStateWithLifecycle()

                ErrorScreen(
                    uiState =
                        ErrorScreenUiState(
                            exceptionTitle = exceptionTitle,
                            stackTrace = exceptionDetails,
                            crashLogsLinkText = crashLogsLinkText.value,
                            showExportBackupDialog = showExportBackupDialog.value,
                            showReimportDialog = showReimportDialog.value,
                            importResultDialogMessage = importResultDialogMessage.value,
                        ),
                    onDismissExportBackupDialog = { viewModel.setShowExportBackupDialog(false) },
                    onConfirmExportBackupDialog = {
                        viewModel.setShowExportBackupDialog(false)
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
                    },
                    onDismissReimportDialog = { viewModel.setShowReimportDialog(false) },
                    onConfirmReimportDialog = {
                        viewModel.setShowReimportDialog(false)
                        viewModel.pendingImportUri?.let { uri ->
                            val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                                showErrorDialog(
                                    throwable,
                                    R.string.reimport_database_failed,
                                    getString(
                                        crash_export_backup_failed,
                                        getString(report_bug),
                                    ),
                                    originalStacktrace = throwable.stackTraceToString(),
                                )
                            }
                            lifecycleScope.launch(exceptionHandler) {
                                val importResult =
                                    withContext(Dispatchers.IO) {
                                        val (_, databaseCopy) =
                                            copyDatabase(suffix = "_BACKUP_BEFORE_REIMPORT")
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
                                            viewModel.backupProgress,
                                        )
                                    }
                                viewModel.setImportResultDialogMessage(
                                    this@ErrorActivity.toMessage(importResult)
                                )
                            }
                        }
                    },
                    onDismissImportResultDialog = {
                        viewModel.setImportResultDialogMessage(null)
                        restartApplication()
                    },
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
                        viewModel.appLogs?.let {
                            viewFile(getUriForFile(it), ExportMimeType.TXT.mimeType)
                        }
                    },
                )
            }
        }

        setupExportBackup(stacktrace)

        viewModel.loadCrashData(intent, Date())
    }

    private fun onExportBackupRequested(stacktrace: String?) {
        viewModel.setShowExportBackupDialog(true)
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
                                        backupProgress = viewModel.backupProgress,
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
                                        viewModel.backupPath ?: "null",
                                        getString(report_bug),
                                    ),
                                )
                                return@launch
                            }
                            application.showToast(
                                "${getString(R.string.exported)} ${getString(R.string.database)}"
                            )
                            viewModel.pendingImportUri = uri
                            viewModel.setShowReimportDialog(true)
                        }
                    }
                }
            }
        viewModel.backupProgress.setupProgressDialog(this)
    }

    companion object {
        private const val TAG = "ErrorActivity"
    }
}

package com.philkes.notallyx.utils

import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.R
import com.philkes.notallyx.R.string.auto_backup_failed
import com.philkes.notallyx.R.string.crash_export_backup_failed
import com.philkes.notallyx.R.string.report_bug
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.databinding.ActivityErrorBinding
import com.philkes.notallyx.presentation.exportedText
import com.philkes.notallyx.presentation.restartApplication
import com.philkes.notallyx.presentation.setCancelButton
import com.philkes.notallyx.presentation.setupProgressDialog
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.view.misc.Progress
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.utils.backup.BACKUP_TIMESTAMP_FORMATTER
import com.philkes.notallyx.utils.backup.copyDatabase
import com.philkes.notallyx.utils.backup.exportAsZip
import com.philkes.notallyx.utils.backup.exportRawDatabase
import com.philkes.notallyx.utils.backup.importRawDatabase
import java.util.Date
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity used when the app is about to crash. Implicitly used by
 * `cat.ereza:customactivityoncrash`.
 */
class ErrorActivity : AppCompatActivity() {

    private lateinit var exportBackupActivityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var exportDatabaseActivityResultLauncher: ActivityResultLauncher<Intent>
    private val exportBackupProgress = MutableLiveData<Progress>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            RestartButton.setOnClickListener {
                CustomActivityOnCrash.restartApplication(
                    this@ErrorActivity,
                    CustomActivityOnCrash.getConfigFromIntent(intent)!!,
                )
            }
            val stacktrace = CustomActivityOnCrash.getStackTraceFromIntent(intent)
            stacktrace?.let {
                application.log(TAG, stackTrace = it)
                ExceptionTitle.text = stacktrace.lines().firstOrNull()?.replaceFirst(":", ":\n")
                ExceptionDetails.text = stacktrace.lines().drop(1).joinToString("\n")
                CopyButton.setOnClickListener { copyToClipBoard(stacktrace) }
            }
            ReportButton.setOnClickListener { reportBug(stacktrace) }
            ViewLogsButton.setOnClickListener { viewLogs() }
            setupExportBackup(binding, stacktrace)
        }
    }

    private fun setupExportBackup(binding: ActivityErrorBinding, stacktrace: String?) {
        binding.ExportBackupButton.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setMessage(
                    getString(
                        R.string.crash_export_backup_message,
                        getString(R.string.continue_),
                        getString(R.string.report_bug),
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
                                    "NotallyX_Crash_Backup-${BACKUP_TIMESTAMP_FORMATTER.format(Date())}",
                                )
                            }
                            .wrapWithChooser(this@ErrorActivity)
                    exportBackupActivityResultLauncher.launch(intent)
                }
                .setCancelButton()
                .show()
        }
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
                                                        BACKUP_TIMESTAMP_FORMATTER.format(
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
                                        backupProgress = exportBackupProgress,
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
                                // This is the specific method for SAF Uris
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
                            withContext(Dispatchers.IO) { application.exportRawDatabase(uri) }
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
                                                        stacktrace, // TODO: which stacktrace?
                                                )
                                            }
                                        lifecycleScope.launch(exceptionHandler) {
                                            val importResult =
                                                withContext(Dispatchers.IO) {
                                                    // Safety copy of internal database
                                                    val (_, databaseCopy) =
                                                        copyDatabase(suffix = "_BEFORE_REIMPORT")
                                                    deleteDatabase(NotallyDatabase.DATABASE_NAME)
                                                    NotallyDatabase.clearInstance()
                                                    application.importRawDatabase(
                                                        uri,
                                                        exportBackupProgress,
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
        exportBackupProgress.setupProgressDialog(this)
    }

    companion object {
        private const val TAG = "ErrorActivity"
    }
}

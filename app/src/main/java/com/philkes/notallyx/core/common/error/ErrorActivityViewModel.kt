package com.philkes.notallyx.core.common.error

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.exportCrashLogs
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.feature.migration.backup.backupDatabaseFiles
import java.io.File
import java.util.Date
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ErrorDialogState(
    val titleResId: Int,
    val message: String,
    val details: String,
    val neutralButtonTextResId: Int? = null,
    val onNeutralButtonClick: (() -> Unit)? = null,
)

class ErrorActivityViewModel(app: Application) : AndroidViewModel(app) {

    var backupPath: File? = null
    var appLogs: File? = null
    var pendingImportUri: Uri? = null

    val backupProgress = MutableStateFlow<Progress?>(null)

    val errorDialogState = MutableStateFlow<ErrorDialogState?>(null)

    val crashLogsLinkText: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    val showExportBackupDialog: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val showReimportDialog: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val importResultDialogMessage: StateFlow<String?>
        field = MutableStateFlow<String?>(null)

    fun setShowExportBackupDialog(show: Boolean) {
        showExportBackupDialog.value = show
    }

    fun setShowReimportDialog(show: Boolean) {
        showReimportDialog.value = show
    }

    fun setImportResultDialogMessage(message: String?) {
        importResultDialogMessage.value = message
    }

    fun setCrashLogsLinkText(text: String?) {
        crashLogsLinkText.value = text
    }

    fun loadCrashData(intent: Intent, now: Date) {
        val app = getApplication<Application>()
        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                app.log(TAG, msg = "Creating crash database dump failed", throwable = throwable)
            }
        ) {
            backupPath = withContext(Dispatchers.IO) { app.backupDatabaseFiles(now) }
        }

        viewModelScope.launch(
            CoroutineExceptionHandler { _, throwable ->
                app.log(TAG, msg = "Creating crash logs dump failed", throwable = throwable)
            }
        ) {
            appLogs =
                withContext(Dispatchers.IO) {
                        CustomActivityOnCrash.getCustomCrashDataFromIntent(intent)?.toInt()?.let {
                            app.exportCrashLogs(it, now)
                        }
                    }
                    ?.also { setCrashLogsLinkText(app.getString(R.string.view_crash_logs)) }
        }
    }

    companion object {
        private const val TAG = "ErrorActivityViewModel"
    }
}

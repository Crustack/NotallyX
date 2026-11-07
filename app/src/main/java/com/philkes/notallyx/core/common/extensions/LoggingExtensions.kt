package com.philkes.notallyx.core.common.extensions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.philkes.notallyx.BuildConfig
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.extensions.showToast
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.feature.migration.backup.FILE_TIMESTAMP_FORMAT
import com.philkes.notallyx.feature.migration.backup.LOG_DATE_FORMATTER
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date

private const val MAX_LOGS_FILE_SIZE_KB: Long = 2048

fun Context.log(
    tag: String,
    msg: String? = null,
    throwable: Throwable? = null,
    stackTrace: String? = null,
) {
    val folder = getLogsDir()
    folder.mkdir()
    logToFile(
        tag,
        DocumentFile.fromFile(folder),
        "$APP_LOG_FILE_NAME.txt",
        msg,
        throwable,
        stackTrace,
    )
}

fun ContextWrapper.getLastExceptionLog(): String? {
    val logFile = getLogFile()
    if (logFile.exists()) {
        return logFile.readText().substringAfterLast("[Start]")
    }
    return null
}

fun ContextWrapper.viewLogs() {
    val logFile = getLogFile()
    if (logFile.exists()) {
        viewFile(getUriForFile(logFile), ExportMimeType.TXT.mimeType)
    } else {
        showToast(getString(R.string.not_exists, logFile.name))
    }
}

fun ContextWrapper.getLogFileUri(): Uri? =
    getLogFile().let { if (it.exists()) getUriForFile(it) else null }

fun ContextWrapper.exportCrashLogs(pid: Int, now: Date = Date()): File? {
    val (timestamp, timeString) =
        now.let { Pair(FILE_TIMESTAMP_FORMAT.format(it), LOG_DATE_FORMATTER.format(it)) }
    val crashesDir =
        try {
            getExternalCrashesDirectory()
        } catch (_: Exception) {
            File(filesDir, "crashes").apply { mkdirs() }
        }
    val targetDir = File(crashesDir, timestamp).apply { mkdirs() }
    val logFile = File(targetDir, "crash_logs_$timestamp.txt")
    log("AndroidExtensions", msg = "Exporting crash logs to '$logFile'")
    try {
        FileOutputStream(logFile).use { fos ->
            PrintWriter(fos).apply {
                printBuildInfos(timeString)
                this.println("----------------------------------------")
                flush()
            }
            val process = Runtime.getRuntime().exec("logcat -d --pid=$pid")
            process.inputStream.use { input -> input.copyTo(fos) }
        }
        keepOnlyNewest(crashesDir, 20)
        return logFile
    } catch (e: IOException) {
        log("AndroidExtensions", msg = "Exporting crash logs failed", throwable = e)
        return null
    }
}

private fun PrintWriter.printBuildInfos(timeString: String) {
    this.println("Time : $timeString")
    this.println("Version code : " + BuildConfig.VERSION_CODE)
    this.println("Version name : " + BuildConfig.VERSION_NAME)
    this.println("Model : " + Build.MODEL)
    this.println("Device : " + Build.DEVICE)
    this.println("Brand : " + Build.BRAND)
    this.println("Manufacturer : " + Build.MANUFACTURER)
    this.println("Android : " + Build.VERSION.SDK_INT)
}

private fun Context.logToFile(
    tag: String,
    folder: DocumentFile,
    fileName: String,
    msg: String? = null,
    throwable: Throwable? = null,
    stackTrace: String? = null,
) {
    msg?.let {
        if (throwable != null) {
            Log.e(tag, it, throwable)
        } else if (stackTrace != null) {
            Log.e(tag, "$it: $stackTrace")
        } else {
            Log.i(tag, it)
        }
    }
    throwable?.let { Log.e(tag, "Exception occurred", it) }
    stackTrace?.let { Log.e(tag, "Exception occurred: $it") }

    val logFile =
        folder.findFile(fileName).let {
            if (it == null || !it.exists()) {
                folder.createFileSafe("text/plain", fileName.removeSuffix(".txt"), ".txt")
            } else if (it.isLargerThanKb(MAX_LOGS_FILE_SIZE_KB)) {
                it.delete()
                folder.createFileSafe("text/plain", fileName.removeSuffix(".txt"), ".txt")
            } else it
        }

    logFile?.let { file ->
        val contentResolver = contentResolver
        val (outputStream, logFileContents) =
            try {
                Pair(contentResolver.openOutputStream(file.uri, "wa"), null)
            } catch (_: UnsupportedOperationException) {
                Pair(
                    contentResolver.openOutputStream(file.uri, "w"),
                    contentResolver.readFileContents(file.uri),
                )
            }

        outputStream?.use { output ->
            val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))

            val formatter = DateFormat.getDateTimeInstance()
            val time = formatter.format(System.currentTimeMillis())

            logFileContents?.let { writer.println(it) }
            if (throwable != null || stackTrace != null) {
                writer.println("[Start]")
            }
            msg?.let { writer.println("${LOG_DATE_FORMATTER.format(Date())} - $tag - $msg") }
            throwable?.printStackTrace(writer)
            stackTrace?.let { writer.println(it) }
            if (throwable != null || stackTrace != null) {
                writer.printBuildInfos(time)
                writer.println("[End]")
            }

            writer.close()
        } ?: Log.e(tag, "Error opening output stream for log file")
    } ?: Log.e(tag, "Error: log file could not be found or created")
}

fun Context.reportBug(stackTrace: String?) {
    catchNoBrowserInstalled {
        try {
            startActivity(createReportBugIntent(stackTrace))
        } catch (_: IllegalArgumentException) {
            if (stackTrace != null) {
                copyToClipBoard(stackTrace)
            }
            showToast(R.string.report_bug_stacktrace_too_long)
            startActivity(
                Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/Crustack/NotallyX/issues/new?labels=bug&template=bug_report.md"
                            .toUri(),
                    )
                    .wrapWithChooser(this)
            )
        }
    }
}

fun Context.catchNoBrowserInstalled(callback: () -> Unit) {
    try {
        callback()
    } catch (_: ActivityNotFoundException) {
        showToast(R.string.install_a_browser)
    }
}

fun Context.createReportBugIntent(
    stackTrace: String?,
    title: String? = null,
    body: String? = null,
): Intent {
    fun String?.asQueryParam(paramName: String): String {
        return this?.let { "&$paramName=${URLEncoder.encode(this, "UTF-8")}" } ?: ""
    }

    val url =
        "https://github.com/Crustack/NotallyX/issues/new?labels=bug&projects=&template=bug_report.yml${
            title.asQueryParam("title")
        }&version=${BuildConfig.VERSION_NAME}&android-version=${Build.VERSION.SDK_INT}${
            stackTrace.asQueryParam("logs")
        }${
            body.asQueryParam("what-happened")
        }"
    if (url.length > 2000) {
        throw IllegalArgumentException("Given stacktrace is too long to build a valid URL!")
    }
    return Intent(Intent.ACTION_VIEW, url.toUri()).wrapWithChooser(this)
}

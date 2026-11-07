package com.philkes.notallyx.feature.migration.backup

import android.content.Context
import android.net.Uri
import android.print.PdfPrintListener
import android.print.printPdf
import androidx.documentfile.provider.DocumentFile
import com.philkes.notallyx.core.common.extensions.getExportedPath
import com.philkes.notallyx.core.platform.PlatformUri
import java.io.File
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AndroidPdfExporter(private val context: Context) : PdfExporter {
    override suspend fun exportHtmlToPdf(
        htmlContent: String,
        targetUri: PlatformUri,
    ): Result<PlatformUri> =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val tempFile = DocumentFile.fromFile(File(context.getExportedPath(), "temp.pdf"))
                val outputUri = Uri.parse(targetUri.uriString)
                context.printPdf(
                    tempFile,
                    htmlContent,
                    object : PdfPrintListener {
                        override fun onSuccess(file: DocumentFile) {
                            try {
                                context.contentResolver.openOutputStream(outputUri)?.use { outStream
                                    ->
                                    context.contentResolver
                                        .openInputStream(file.uri)
                                        ?.copyTo(outStream)
                                }
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(targetUri))
                                }
                            } catch (e: Exception) {
                                if (continuation.isActive) {
                                    continuation.resume(Result.failure(e))
                                }
                            }
                        }

                        override fun onFailure(message: CharSequence?) {
                            if (continuation.isActive) {
                                continuation.resume(
                                    Result.failure(
                                        RuntimeException(message?.toString() ?: "PDF export failed")
                                    )
                                )
                            }
                        }
                    },
                )
            }
        }
}

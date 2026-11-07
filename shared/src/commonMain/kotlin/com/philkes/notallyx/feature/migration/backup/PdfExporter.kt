package com.philkes.notallyx.feature.migration.backup

import com.philkes.notallyx.core.platform.PlatformUri

interface PdfExporter {
    suspend fun exportHtmlToPdf(
        htmlContent: String,
        targetUri: PlatformUri,
    ): Result<PlatformUri>
}

class NoOpPdfExporter : PdfExporter {
    override suspend fun exportHtmlToPdf(
        htmlContent: String,
        targetUri: PlatformUri,
    ): Result<PlatformUri> = Result.success(targetUri)
}

package com.philkes.notallyx.presentation.viewmodel

import com.philkes.notallyx.utils.MIME_TYPE_JSON

enum class ExportMimeType(val mimeType: String, val fileExtension: String) {
    TXT("text/plain", "txt"),
    MD("text/markdown", "md"),
    PDF("application/pdf", "pdf"),
    JSON(MIME_TYPE_JSON, "json"),
    HTML("text/html", "html"),
}

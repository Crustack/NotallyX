package com.philkes.notallyx.feature.migration

enum class ExportMimeType(val mimeType: String, val fileExtension: String) {
    TXT("text/plain", "txt"),
    MD("text/markdown", "md"),
    PDF("application/pdf", "pdf"),
    JSON("application/json", "json"),
    HTML("text/html", "html"),
}

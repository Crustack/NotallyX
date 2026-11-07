package com.philkes.notallyx.core.platform

import java.net.URLConnection

/**
 * Portable MIME type and file extension lookup replacing `android.webkit.MimeTypeMap` in shared
 * domain/data code.
 */
object MimeTypeUtils {
    private val mimeToExtension =
        mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp",
            "image/gif" to "gif",
            "image/bmp" to "bmp",
            "image/svg+xml" to "svg",
            "image/heic" to "heic",
            "image/heif" to "heif",
            "audio/mp4" to "m4a",
            "audio/x-m4a" to "m4a",
            "audio/mpeg" to "mp3",
            "audio/mp3" to "mp3",
            "audio/wav" to "wav",
            "audio/x-wav" to "wav",
            "audio/ogg" to "ogg",
            "audio/aac" to "aac",
            "audio/AMR" to "amr",
            "video/mp4" to "mp4",
            "video/webm" to "webm",
            "application/pdf" to "pdf",
            "application/zip" to "zip",
            "application/json" to "json",
            "application/xml" to "xml",
            "text/plain" to "txt",
            "text/html" to "html",
            "text/markdown" to "md",
            "text/csv" to "csv",
        )

    private val extensionToMime: Map<String, String> = buildMap {
        mimeToExtension.forEach { (mime, ext) -> putIfAbsent(ext, mime) }
        put("jpeg", "image/jpeg")
        put("htm", "text/html")
        put("markdown", "text/markdown")
    }

    fun getExtensionFromMimeType(mimeType: String?): String? {
        if (mimeType.isNullOrBlank()) return null
        val normalized = mimeType.substringBefore(';').trim().lowercase()
        return mimeToExtension[normalized]
            ?: normalized.substringAfter('/', "").takeIf {
                it.isNotEmpty() && !it.contains('+') && !it.contains('.') && it != "octet-stream"
            }
    }

    fun getMimeTypeFromExtension(extension: String?): String? {
        if (extension.isNullOrBlank()) return null
        val cleanExt = extension.trim().removePrefix(".").lowercase()
        return extensionToMime[cleanExt] ?: URLConnection.guessContentTypeFromName("file.$cleanExt")
    }

    fun getMimeTypeFromFileName(fileName: String?): String? {
        if (fileName.isNullOrBlank()) return null
        val ext = fileName.substringAfterLast('.', "")
        return getMimeTypeFromExtension(ext)
    }

    fun getFileExtensionFromUrl(url: String?): String {
        if (url.isNullOrBlank()) return ""
        val withoutFragment = url.substringBefore('#')
        val withoutQuery = withoutFragment.substringBefore('?')
        val fileName = withoutQuery.substringAfterLast('/')
        return if (fileName.contains('.')) {
            fileName.substringAfterLast('.', "").lowercase()
        } else {
            ""
        }
    }
}

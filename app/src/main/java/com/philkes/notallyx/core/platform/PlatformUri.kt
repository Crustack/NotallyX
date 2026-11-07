package com.philkes.notallyx.core.platform

import java.io.File
import kotlinx.serialization.Serializable

/**
 * Platform-independent reference to a local file or content stream.
 * - On Android: wraps either a `file://` path or a SAF `content://` URI string.
 * - On Desktop JVM: wraps an absolute or relative filesystem path.
 */
@JvmInline
@Serializable
value class PlatformUri(val uriString: String) {
    override fun toString(): String = uriString

    fun toFileOrNull(): File? {
        return when {
            uriString.startsWith("content://") -> null
            uriString.startsWith("file://") -> File(uriString.removePrefix("file://"))
            else -> File(uriString)
        }
    }

    companion object {
        fun fromFile(file: File): PlatformUri = PlatformUri(file.absolutePath)
    }
}

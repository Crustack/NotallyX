package com.philkes.notallyx.core.ui.platform

import com.philkes.notallyx.core.platform.PlatformUri

class AndroidFilePicker(
    private val onPickFile: (suspend (List<String>) -> PlatformUri?)? = null,
    private val onPickMultipleFiles: (suspend (List<String>) -> List<PlatformUri>)? = null,
    private val onPickDirectory: (suspend () -> PlatformUri?)? = null,
    private val onCreateDocument: (suspend (String, String) -> PlatformUri?)? = null,
) : FilePicker {
    override suspend fun pickFile(mimeTypes: List<String>): PlatformUri? =
        onPickFile?.invoke(mimeTypes)

    override suspend fun pickMultipleFiles(mimeTypes: List<String>): List<PlatformUri> =
        onPickMultipleFiles?.invoke(mimeTypes) ?: emptyList()

    override suspend fun pickDirectory(): PlatformUri? = onPickDirectory?.invoke()

    override suspend fun createDocument(fileName: String, mimeType: String): PlatformUri? =
        onCreateDocument?.invoke(fileName, mimeType)
}

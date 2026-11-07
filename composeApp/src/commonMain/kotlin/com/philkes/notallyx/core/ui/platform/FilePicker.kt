package com.philkes.notallyx.core.ui.platform

import com.philkes.notallyx.core.platform.PlatformUri

interface FilePicker {
    suspend fun pickFile(mimeTypes: List<String> = listOf("*/*")): PlatformUri?

    suspend fun pickMultipleFiles(mimeTypes: List<String> = listOf("*/*")): List<PlatformUri>

    suspend fun pickDirectory(): PlatformUri?

    suspend fun createDocument(fileName: String, mimeType: String): PlatformUri?
}

class NoOpFilePicker : FilePicker {
    override suspend fun pickFile(mimeTypes: List<String>): PlatformUri? = null

    override suspend fun pickMultipleFiles(mimeTypes: List<String>): List<PlatformUri> = emptyList()

    override suspend fun pickDirectory(): PlatformUri? = null

    override suspend fun createDocument(fileName: String, mimeType: String): PlatformUri? = null
}

package com.philkes.notallyx.core.ui.platform

import com.philkes.notallyx.core.platform.PlatformUri
import java.awt.GraphicsEnvironment
import java.io.File
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DesktopFilePicker(
    private val onPickFile: (suspend (List<String>) -> PlatformUri?)? = null,
    private val onPickMultipleFiles: (suspend (List<String>) -> List<PlatformUri>)? = null,
    private val onPickDirectory: (suspend () -> PlatformUri?)? = null,
    private val onCreateDocument: (suspend (String, String) -> PlatformUri?)? = null,
) : FilePicker {

    override suspend fun pickFile(mimeTypes: List<String>): PlatformUri? {
        if (onPickFile != null) return onPickFile.invoke(mimeTypes)
        return withContext(Dispatchers.IO) {
            if (GraphicsEnvironment.isHeadless()) return@withContext null
            val chooser =
                JFileChooser().apply {
                    dialogTitle = "Select File"
                    isMultiSelectionEnabled = false
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile?.let { PlatformUri.fromFile(it) }
            } else {
                null
            }
        }
    }

    override suspend fun pickMultipleFiles(mimeTypes: List<String>): List<PlatformUri> {
        if (onPickMultipleFiles != null) return onPickMultipleFiles.invoke(mimeTypes)
        return withContext(Dispatchers.IO) {
            if (GraphicsEnvironment.isHeadless()) return@withContext emptyList()
            val chooser =
                JFileChooser().apply {
                    dialogTitle = "Select Files"
                    isMultiSelectionEnabled = true
                    fileSelectionMode = JFileChooser.FILES_ONLY
                }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFiles.map { PlatformUri.fromFile(it) }
            } else {
                emptyList()
            }
        }
    }

    override suspend fun pickDirectory(): PlatformUri? {
        if (onPickDirectory != null) return onPickDirectory.invoke()
        return withContext(Dispatchers.IO) {
            if (GraphicsEnvironment.isHeadless()) return@withContext null
            val chooser =
                JFileChooser().apply {
                    dialogTitle = "Select Directory"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    isAcceptAllFileFilterUsed = false
                }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile?.let { PlatformUri.fromFile(it) }
            } else {
                null
            }
        }
    }

    override suspend fun createDocument(fileName: String, mimeType: String): PlatformUri? {
        if (onCreateDocument != null) return onCreateDocument.invoke(fileName, mimeType)
        return withContext(Dispatchers.IO) {
            if (GraphicsEnvironment.isHeadless()) return@withContext null
            val chooser =
                JFileChooser().apply {
                    dialogTitle = "Save File"
                    selectedFile = File(fileName)
                }
            if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile?.let { PlatformUri.fromFile(it) }
            } else {
                null
            }
        }
    }
}

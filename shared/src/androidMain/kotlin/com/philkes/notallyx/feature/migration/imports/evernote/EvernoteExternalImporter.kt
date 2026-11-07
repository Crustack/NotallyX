package com.philkes.notallyx.feature.migration.imports.evernote

import android.app.Application
import android.net.Uri
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.write
import com.philkes.notallyx.core.common.result.ImportProgress
import com.philkes.notallyx.core.common.result.ImportStage
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.platform.AppLogger
import com.philkes.notallyx.core.platform.MimeTypeUtils
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.feature.migration.imports.ExternalImporter
import com.philkes.notallyx.feature.migration.imports.ImportException
import java.io.File
import java.io.InputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.MutableStateFlow

class EvernoteExternalImporter : EvernoteImporter(), ExternalImporter {

    override fun import(
        app: Application,
        source: Uri,
        destination: File,
        progress: MutableStateFlow<Progress?>?,
    ): Pair<List<BaseNote>, File> {
        progress?.value = ImportProgress(indeterminate = true)
        if (MimeTypeUtils.getFileExtensionFromUrl(source.toString()) != "enex") {
            throw ImportException(
                R.string.invalid_evernote,
                IllegalArgumentException("Provided file is not in ENEX format"),
            )
        }
        return importFromStream(
            inputStream = app.contentResolver.openInputStream(source)!!,
            destination = destination,
            progress = progress,
        )
    }

    fun importFromStream(
        inputStream: InputStream,
        destination: File,
        progress: MutableStateFlow<Progress?>? = null,
    ): Pair<List<BaseNote>, File> {
        val evernoteExport: EvernoteExport =
            parseExport(inputStream)
                ?: throw ImportException(
                    R.string.invalid_evernote,
                    IllegalArgumentException("Failed to parse Evernote export"),
                )

        val total = evernoteExport.notes.size
        progress?.value = ImportProgress(total = total)
        var counter = 1
        try {
            val notes =
                evernoteExport.notes.map {
                    val note = it.mapToBaseNote()
                    progress?.value = ImportProgress(current = counter++, total = total)
                    note
                }
            val resources =
                evernoteExport.notes.flatMap { it.resources }.distinctBy { it.attributes?.fileName }
            saveResourcesToFiles(resources, destination, progress)
            return Pair(notes, destination)
        } catch (e: Exception) {
            throw ImportException(R.string.invalid_evernote, e)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun saveResourcesToFiles(
        resources: Collection<EvernoteResource>,
        dir: File,
        progress: MutableStateFlow<Progress?>? = null,
    ) {
        progress?.value = ImportProgress(total = resources.size, stage = ImportStage.EXTRACT_FILES)
        resources.forEachIndexed { idx, it ->
            val file = File(dir, it.attributes!!.fileName)
            try {
                val data = Base64.Mime.decode(it.data!!.content.trim())
                file.write(data)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to decode Evernote resource", e)
            }
            progress?.value =
                ImportProgress(
                    current = idx + 1,
                    total = resources.size,
                    stage = ImportStage.EXTRACT_FILES,
                )
        }
    }

    companion object {
        private const val TAG = "EvernoteImporter"
    }
}

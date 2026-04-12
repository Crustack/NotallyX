package com.philkes.notallyx.data.imports

// TODO: wait for new roboelectric version for API 35
//  see https://github.com/robolectric/robolectric/pull/9680

import android.app.Application
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.utils.decodeToBitmap
import com.philkes.notallyx.utils.getCurrentMediaRoot
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NotesImporterTest {
    private lateinit var application: Application
    private lateinit var database: NotallyDatabase

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        database =
            Room.inMemoryDatabaseBuilder(application, NotallyDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @Test
    fun `importFiles Google Keep`() {
        testImport(ImportSource.GOOGLE_KEEP, 8)
    }

    @Test
    fun `importFiles Google Keep german Takeout zip`() {
        testImport(
            prepareImportSources(ImportSource.GOOGLE_KEEP, "Takeout_German.zip"),
            ImportSource.GOOGLE_KEEP,
            8,
        )
    }

    @Test
    fun `importFiles Google Keep other Takeout zip`() {
        testImport(
            prepareImportSources(ImportSource.GOOGLE_KEEP, "Takeout_Other.zip"),
            ImportSource.GOOGLE_KEEP,
            8,
        )
    }

    @Test
    fun `importFiles Evernote`() {
        // Evernote does not export archived and deleted notes
        testImport(ImportSource.EVERNOTE, 6)
    }

    @Test
    fun `importFiles Quillpad`() {
        testImport(ImportSource.QUILLPAD, 9)
    }

    private fun testImport(importSource: ImportSource, expectedAmountNotes: Int) {
        testImport(prepareImportSources(importSource), importSource, expectedAmountNotes)
    }

    private fun testImport(
        importSourceFile: File,
        importSource: ImportSource,
        expectedAmountNotes: Int,
    ) {
        val importOutputFolder = application.getCurrentMediaRoot()
        runBlocking {
            NotesImporter(application, database).import(importSourceFile.toUri(), importSource)

            val actual = database.getBaseNoteDao().getAll().sortedBy { it.title }
            assertThat(actual).hasSize(expectedAmountNotes)
            actual.forEach { note ->
                note.images.forEach {
                    val imageFile = File(importOutputFolder, "Images/${it.localName}")
                    assertThat(imageFile)
                        .exists()
                        .`is`(
                            Condition(
                                { file ->
                                    val bitmap = file.decodeToBitmap()
                                    bitmap != null && bitmap.width == 1200 && bitmap.height == 1600
                                },
                                "Image",
                            )
                        )
                }
                note.files.forEach {
                    assertThat(File(importOutputFolder, "Files/${it.localName}")).exists()
                }
                note.audios.forEach {
                    val audioFile = File(importOutputFolder, "Audios/${it.name}")
                    assertThat(audioFile).exists()
                    // TODO: Metadata is not properly stored
                    //                        .`is`(
                    //                            Condition(
                    //                                { file ->
                    //                                    val retriever = MediaMetadataRetriever()
                    //
                    // retriever.setDataSource(file.absolutePath)
                    //                                    val hasAudio =
                    //                                        retriever.extractMetadata(
                    //
                    // MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
                    //                                        )
                    //                                    hasAudio != null
                    //                                },
                    //                                "Audio",
                    //                            )
                    //                        )
                }
            }
        }
    }

    private fun prepareImportSources(importSource: ImportSource, fileName: String? = null): File {
        val tempDir = Files.createTempDirectory("imports-${importSource.name.lowercase()}").toFile()
        copyTestFilesToTempDir("imports/${importSource.name.lowercase()}", tempDir)
        println("Input folder: ${tempDir.absolutePath}")
        if (fileName != null) {
            return File(tempDir, fileName)
        }
        return when (importSource) {
            ImportSource.GOOGLE_KEEP -> File(tempDir, "Takeout.zip")
            ImportSource.EVERNOTE -> File(tempDir, "Notebook.enex")
            ImportSource.PLAIN_TEXT -> File(tempDir, "text.txt")
            ImportSource.JSON -> File(tempDir, "text.json")
            ImportSource.QUILLPAD -> File(tempDir, "quillpad_backup.zip")
        }
    }

    private fun copyTestFilesToTempDir(resourceFolderPath: String, destination: File) {
        val files =
            javaClass.classLoader!!.getResources(resourceFolderPath).toList().flatMap { url ->
                File(url.toURI()).listFiles()?.toList() ?: listOf()
            }
        files
            .filter { !it.isDirectory }
            .forEach { file ->
                val outputFile = File(destination, file.name)
                file.inputStream().use { input ->
                    outputFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
    }
}

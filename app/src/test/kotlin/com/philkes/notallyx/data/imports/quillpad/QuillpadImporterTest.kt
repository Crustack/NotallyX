package com.philkes.notallyx.data.imports.quillpad

import com.philkes.notallyx.data.model.Audio
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test

class QuillpadImporterTest {

    private val importer = QuillpadImporter()

    fun String.parseToBaseNote(notebookMap: Map<Long, String> = mapOf()) =
        with(importer) {
            json.decodeFromString<QuillpadNote>(this@parseToBaseNote).toBaseNote(notebookMap)
        }

    @Test
    fun `parseToBaseNote text note with labels`() {
        // language=json
        val json =
            """
          {
              "title": "Normal Note",
              "content": "This is some note, nothing special",
              "creationDate": 1729518341,
              "modifiedDate": 1729518341,
              "notebookId": 2,
              "id": 4,
              "tags": [
                {
                  "name": "Tag1",
                  "id": 1
                }
              ]
            }
        """
                .trimIndent()
        val expected =
            createBaseNote(
                title = "Normal Note",
                timestamp = 1729518341000,
                modifiedTimestamp = 1729518341000,
                labels = listOf("Notebook1", "Tag1"),
                body = "This is some note, nothing special",
            )
        val actual = json.parseToBaseNote(mapOf(2L to "Notebook1"))

        assertEquals(expected, actual)
    }

    @Test
    fun `parseToBaseNote trashed note`() {
        // language=json
        val json =
            """
            {
              "title": "Trashed Note",
              "content": "This is deleted",
              "isDeleted": true,
              "creationDate": 1775998133,
              "modifiedDate": 1775998137,
              "deletionDate": 1775998149,
              "id": 8
            }
        """
                .trimIndent()

        val actual = json.parseToBaseNote()

        assertThat(actual)
            .extracting("title", "folder")
            .containsExactly("Trashed Note", Folder.DELETED)
    }

    @Test
    fun `parseToBaseNote archived note`() {

        // language=json
        val json =
            """
            {
              "title": "Archived Note",
              "content": "This is an archived note",
              "isArchived": true,
              "creationDate": 1775998153,
              "modifiedDate": 1775998168,
              "id": 9
            }
        """
                .trimIndent()

        val actual = json.parseToBaseNote()

        assertThat(actual)
            .extracting("title", "folder")
            .containsExactly("Archived Note", Folder.ARCHIVED)
    }

    @Test
    fun `parseToBaseNote pinned note`() {
        // language=json
        val json =
            """
            {
              "title": "Pinned Note",
              "isPinned": true,
              "creationDate": 1775998153,
              "modifiedDate": 1775998168,
              "id": 9
            }
        """
                .trimIndent()

        val actual = json.parseToBaseNote()

        assertThat(actual).extracting("title", "pinned").containsExactly("Pinned Note", true)
    }

    @Test
    fun `parseToBaseNote list note`() {
        // language=json
        val json =
            """
           {
              "title": "List Note",
              "isList": true,
              "taskList": [
                {
                  "id": 0,
                  "content": "Task1",
                  "isDone": false
                },
                {
                  "id": 1,
                  "content": "Task2",
                  "isDone": true
                }
              ],
              "creationDate": 1775997909,
              "modifiedDate": 1775997985,
              "id": 3
          }
        """
                .trimIndent()

        val actual = json.parseToBaseNote()

        assertThat(actual)
            .extracting("type", "title", "items")
            .containsExactly(
                Type.LIST,
                "List Note",
                listOf(
                    ListItem("Task1", false, false, 0, mutableListOf()),
                    ListItem("Task2", true, false, 1, mutableListOf()),
                ),
            )
    }

    @Test
    fun `parseToBaseNote note with images`() {
        // language=json
        val json =
            """
           {
              "title": "Image Note",
              "creationDate": 1775998031,
              "modifiedDate": 1775998049,
              "attachments": [
                {
                  "description": "image.jpg",
                  "fileName": "image.jpg"
                }
              ],
              "id": 5
            }
        """
                .trimIndent()

        val actual = json.parseToBaseNote()

        assertThat(actual)
            .extracting("title", "images")
            .containsExactly(
                "Image Note",
                listOf(FileAttachment("image.jpg", "image.jpg", "image/jpeg")),
            )
    }

    // TODO: in Quillpad not possible to attach non-image file?
    //    @Test
    //    fun `parseToBaseNote note with files`() {
    //        val json =
    //            """
    //            {
    //              "title": "File Note",
    //              "attachments": [
    //                {
    //                  "filePath": "document.doc",
    //                  "mimetype": "application/msword"
    //                }
    //              ],
    //            }
    //        """
    //                .trimIndent()
    //
    //        val actual = with(importer) { json.parseToBaseNote() }
    //
    //        assertThat(actual)
    //            .extracting("title", "files")
    //            .containsExactly(
    //                "File Note",
    //                listOf(FileAttachment("document.doc", "document.doc", "application/msword")),
    //            )
    //    }

    @Test
    fun `parseToBaseNote note with audio`() {
        // language=json
        val json =
            """
             {
              "title": "Audio Note",
              "creationDate": 1775998102,
              "modifiedDate": 1775998121,
              "attachments": [
                {
                  "type": "AUDIO",
                  "description": "Recorded Clip",
                  "fileName": "audio.mp3"
                }
              ],
              "id": 7
            }
        """
                .trimIndent()

        val actual = json.parseToBaseNote()

        assertThat(actual.title).isEqualTo("Audio Note")
        assertThat(actual.audios[0].name).isEqualTo("audio.mp3")
    }

    @Test
    fun `parseToBaseNote note with reminder`() {
        // language=json
        val json =
            """
            {
              "title": "Reminder Note",
              "creationDate": 1775998181,
              "modifiedDate": 1775998219,
              "id": 10,
              "reminders": [
                {
                  "name": "Reminder1",
                  "noteId": 10,
                  "date": 1776012540,
                  "id": 1
                }
              ]
            }
        """
                .trimIndent()

        val actual = json.parseToBaseNote()

        assertThat(actual.title).isEqualTo("Reminder Note")
        assertThat(actual.reminders).isEqualTo(listOf(Reminder(1, Date(1776012540), null)))
    }

    companion object {
        fun createBaseNote(
            id: Long = 0L,
            type: Type = Type.NOTE,
            folder: Folder = Folder.NOTES,
            color: String = BaseNote.COLOR_DEFAULT,
            title: String = "Note",
            pinned: Boolean = false,
            timestamp: Long = System.currentTimeMillis(),
            modifiedTimestamp: Long = System.currentTimeMillis(),
            labels: List<String> = listOf(),
            body: String = "",
            spans: List<SpanRepresentation> = listOf(),
            items: List<ListItem> = listOf(),
            images: List<FileAttachment> = listOf(),
            files: List<FileAttachment> = listOf(),
            audios: List<Audio> = listOf(),
            reminders: List<Reminder> = listOf(),
        ): BaseNote {
            return BaseNote(
                id,
                type,
                folder,
                color,
                title,
                pinned,
                timestamp,
                modifiedTimestamp,
                labels,
                body,
                spans,
                items,
                images,
                files,
                audios,
                reminders,
                NoteViewMode.EDIT,
                isPinnedToStatus = false,
            )
        }
    }
}

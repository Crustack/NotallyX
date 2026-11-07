package com.philkes.notallyx.navigation

import androidx.navigation3.runtime.NavKey
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.migration.ExportMimeType
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JVM unit test verifying that every [NotallyNavKey] subtype is registered in
 * [NotallyNavSavedStateConfig] for open polymorphic [NavKey] serialization (`PREP-021`).
 */
class NavigationStateSerializationTest {

    private val json = Json { serializersModule = NotallyNavSavedStateConfig.serializersModule }

    private val navKeySerializer = PolymorphicSerializer(NavKey::class)

    @Test
    fun `all NotallyNavKey subtypes round-trip polymorphically as NavKey without reflection`() {
        val allSampleKeys: List<NotallyNavKey> =
            listOf(
                NotallyNavKey.Notes,
                NotallyNavKey.Unlabeled,
                NotallyNavKey.Labels,
                NotallyNavKey.Deleted,
                NotallyNavKey.Archived,
                NotallyNavKey.Reminders,
                NotallyNavKey.Settings,
                NotallyNavKey.DisplayLabel(label = "work"),
                NotallyNavKey.Search(initialFolder = Folder.NOTES, initialLabel = "work"),
                NotallyNavKey.EditNote(
                    noteId = 42L,
                    initialFolder = Folder.NOTES,
                    initialLabel = "work",
                    initialTitle = "Hello",
                    initialBody = "World",
                    initialImageUris = listOf("content://img/1"),
                    initialFileUris = listOf("content://file/1"),
                ),
                NotallyNavKey.EditList(
                    noteId = 99L,
                    initialFolder = Folder.ARCHIVED,
                    initialLabel = "shopping",
                ),
                NotallyNavKey.EditTextPlain(fileUri = "file:///tmp/test.txt"),
                NotallyNavKey.ViewImage(
                    noteId = 42L,
                    imageUris = listOf("img1.jpg", "img2.jpg"),
                    initialIndex = 1,
                ),
                NotallyNavKey.PlayAudio(
                    audioName = "rec.m4a",
                    duration = 5000L,
                    timestamp = 1700000000000L,
                ),
                NotallyNavKey.RecordAudio,
                NotallyNavKey.PickNote(excludeNoteId = 42L),
                NotallyNavKey.NoteReminders(noteId = 42L),
                NotallyNavKey.SelectLabels(currentLabels = listOf("work", "personal")),
                NotallyNavKey.ColorPicker(
                    currentColor = "#FAAFA8",
                    colors = listOf("DEFAULT", "#FAAFA8"),
                    defaultColor = "DEFAULT",
                ),
                NotallyNavKey.ExportFormat(
                    formats = listOf(ExportMimeType.PDF, ExportMimeType.TXT)
                ),
                NotallyNavKey.Confirm(
                    title = "Delete?",
                    message = "Are you sure?",
                    actionType = "DELETE",
                    positiveButtonText = "Yes",
                    negativeButtonText = "No",
                ),
            )

        assertEquals(
            "Every sealed subtype of NotallyNavKey must be included in test samples",
            NotallyNavKey::class.sealedSubclasses.size,
            allSampleKeys.map { it::class }.toSet().size,
        )

        for (key in allSampleKeys) {
            val encoded = json.encodeToString(navKeySerializer, key)
            val decoded = json.decodeFromString(navKeySerializer, encoded)
            assertEquals(key, decoded)
        }
    }
}

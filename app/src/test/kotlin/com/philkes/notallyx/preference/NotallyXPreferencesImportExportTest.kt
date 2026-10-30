package com.philkes.notallyx.preference

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.LegacyNotallyXPreferences
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.test.FakeAndroidKeyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class NotallyXPreferencesImportExportTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences

    @Before
    fun setUp() {
        FakeAndroidKeyStore.setup
        application = ApplicationProvider.getApplicationContext()
        application.getDefaultSharedPreferences().edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences = NotallyXPreferences.getInstance(application)
        preferences.reset()
    }

    @After
    fun tearDown() {
        NotallyXPreferences.clearInstance()
    }

    @Test
    fun `toJsonString exports preferences and excludes sensitive fields`() {
        val jsonString = preferences.toJsonString()
        val json = Json.parseToJsonElement(jsonString).jsonObject

        // Verify sensitive fields are excluded
        assertThat(json.containsKey("biometricLock")).isFalse()
        assertThat(json.containsKey("biometricLockEncryptsDb")).isFalse()
        assertThat(json.containsKey("iv")).isFalse()
        assertThat(json.containsKey("encryption_iv")).isFalse()
        assertThat(json.containsKey("databaseEncryptionKey")).isFalse()
        assertThat(json.containsKey("database_encryption_key")).isFalse()
        assertThat(json.containsKey("dataInPublicFolder")).isFalse()
        assertThat(json.containsKey("dataOnExternalStorage")).isFalse()
        assertThat(json.containsKey("dataSchemaId")).isFalse()

        // Verify standard preferences are present
        assertThat(json.containsKey("theme")).isTrue()
        assertThat(json.containsKey("notesView")).isTrue()
        assertThat(json.containsKey("maxItems")).isTrue()
        assertThat(json.containsKey("maxLines")).isTrue()
        assertThat(json.containsKey("periodicBackups")).isTrue()
        assertThat(json.containsKey("notesSorting")).isTrue()
    }

    @Test
    fun `import modern json string updates preferences`() {
        val modernJson =
            """
            {
                "theme": "DARK",
                "useDynamicColors": true,
                "notesView": "GRID",
                "maxItems": 9,
                "maxLines": 6,
                "maxTitle": 3,
                "backupsFolder": "/storage/emulated/0/Backups",
                "backupOnSave": true,
                "periodicBackups": {
                    "periodInDays": 3,
                    "maxBackups": 15
                },
                "notesSorting": {
                    "sortedBy": "TITLE",
                    "sortDirection": "ASC"
                },
                "labelsHidden": ["LabelA", "LabelB"],
                "widgets": {
                    "101": 202
                },
                "widgetNoteTypes": {
                    "101": "LIST"
                }
            }
            """
                .trimIndent()

        val success = preferences.import(modernJson)
        assertThat(success).isTrue()

        val data = runBlocking { preferences.userDataStore.data.first() }
        assertThat(data.theme).isEqualTo(Theme.DARK)
        assertThat(data.useDynamicColors).isTrue()
        assertThat(data.notesView).isEqualTo(NotesView.GRID)
        assertThat(data.maxItems).isEqualTo(9)
        assertThat(data.maxLines).isEqualTo(6)
        assertThat(data.maxTitle).isEqualTo(3)
        assertThat(data.backupsFolder).isEqualTo("/storage/emulated/0/Backups")
        assertThat(data.backupOnSave).isTrue()
        assertThat(data.periodicBackups.periodInDays).isEqualTo(3)
        assertThat(data.periodicBackups.maxBackups).isEqualTo(15)
        assertThat(data.notesSorting.sortedBy).isEqualTo(NotesSortBy.TITLE)
        assertThat(data.notesSorting.sortDirection).isEqualTo(SortDirection.ASC)
        assertThat(data.labelsHidden).containsExactlyInAnyOrder("LabelA", "LabelB")
        assertThat(data.widgets[101]).isEqualTo(202L)
        assertThat(data.widgetNoteTypes[101]).isEqualTo(Type.LIST)
    }

    @Test
    fun `import legacy json format normalizes and updates preferences`() {
        val legacyJson =
            """
            {
                "theme": "light",
                "useDynamicColors": false,
                "view": "grid",
                "dateFormatOverview": "shortIso",
                "timeFormatOverview": "amPm",
                "dateFormatNoteView": "ddMmYyGer",
                "timeFormatNoteView": "twentyFourH",
                "listItemSorting": "autoSortByChecked",
                "defaultListNoteViewMode": "readOnly",
                "maxItemsToDisplayInList.v1": 7,
                "maxLinesToDisplayInNote.v1": 5,
                "maxLinesToDisplayInTitle": 2,
                "labelsHiddenInNavigation": ["Work", "Personal"],
                "labelsHiddenInOverview": true,
                "imagesHiddenInOverview": true,
                "alwaysShowSearchBar": true,
                "maxLabelsInNavigation": 12,
                "autoBackup": "/legacy/backup/folder",
                "backupOnSave": true,
                "autoBackupPeriodDays": 5,
                "maxBackups": 20,
                "periodicBackupLastExecution": 1700000000000,
                "autoRemoveDeletedNotesAfterDays": 30,
                "autoSaveAfterIdleTime": 10,
                "secureFlag": true,
                "editNoteActivityTopActions": "PIN,SEARCH,REMINDERS",
                "editNoteActivityBottomAction": "toggleViewMode",
                "defaultNoteColor": "#FFFFFF",
                "notesSorting": "autoSortByTitle",
                "notesSortingDirection": "ASC",
                "widget:55": 999,
                "widgetNoteType:55": "NOTE"
            }
            """
                .trimIndent()

        val success = preferences.import(legacyJson)
        assertThat(success).isTrue()

        val data = runBlocking { preferences.userDataStore.data.first() }
        assertThat(data.theme).isEqualTo(Theme.LIGHT)
        assertThat(data.notesView).isEqualTo(NotesView.GRID)
        assertThat(data.dateFormatOverview).isEqualTo(DateFormat.SHORT_ISO)
        assertThat(data.timeFormatOverview).isEqualTo(TimeFormat.AM_PM)
        assertThat(data.dateFormatNoteView).isEqualTo(DateFormat.DD_MM_YY_GER)
        assertThat(data.timeFormatNoteView).isEqualTo(TimeFormat.TWENTY_FOUR_H)
        assertThat(data.listItemSorting).isEqualTo(ListItemSort.AUTO_SORT_BY_CHECKED)
        assertThat(data.defaultListNoteViewMode).isEqualTo(DefaultListNoteViewMode.READ_ONLY)
        assertThat(data.maxItems).isEqualTo(7)
        assertThat(data.maxLines).isEqualTo(5)
        assertThat(data.maxTitle).isEqualTo(2)
        assertThat(data.labelsHidden).containsExactlyInAnyOrder("Work", "Personal")
        assertThat(data.labelTagsHiddenInOverview).isTrue()
        assertThat(data.imagesHiddenInOverview).isTrue()
        assertThat(data.alwaysShowSearchBar).isTrue()
        assertThat(data.maxLabels).isEqualTo(12)
        assertThat(data.backupsFolder).isEqualTo("/legacy/backup/folder")
        assertThat(data.backupOnSave).isTrue()
        assertThat(data.periodicBackups.periodInDays).isEqualTo(5)
        assertThat(data.periodicBackups.maxBackups).isEqualTo(20)
        assertThat(data.periodicBackupLastExecution).isEqualTo(1700000000000L)
        assertThat(data.autoRemoveDeletedNotesAfterDays).isEqualTo(30)
        assertThat(data.autoSaveAfterIdleTime).isEqualTo(10)
        assertThat(data.secureFlag).isTrue()
        assertThat(data.editNoteActivityTopActions)
            .isEqualTo(listOf(EditAction.PIN, EditAction.SEARCH, EditAction.REMINDERS))
        assertThat(data.editNoteActivityBottomAction).isEqualTo(EditAction.TOGGLE_VIEW_MODE)
        assertThat(data.defaultNoteColor).isEqualTo("#FFFFFF")
        assertThat(data.notesSorting.sortedBy).isEqualTo(NotesSortBy.TITLE)
        assertThat(data.notesSorting.sortDirection).isEqualTo(SortDirection.ASC)
        assertThat(data.widgets[55]).isEqualTo(999L)
        assertThat(data.widgetNoteTypes[55]).isEqualTo(Type.NOTE)
    }

    @Test
    fun `import does not override security credentials from JSON`() {
        val maliciousJson =
            """
            {
                "theme": "LIGHT",
                "biometricLock": "ENABLED",
                "biometricLockEncryptsDb": true,
                "iv": [1, 2, 3],
                "databaseEncryptionKey": [4, 5, 6],
                "dataInPublicFolder": true
            }
            """
                .trimIndent()

        val success = preferences.import(maliciousJson)
        assertThat(success).isTrue()

        val data = runBlocking { preferences.userDataStore.data.first() }
        // Theme was updated
        assertThat(data.theme).isEqualTo(Theme.LIGHT)
        // Sensitive credentials remained unchanged
        assertThat(preferences.isLockEnabled).isFalse()
        assertThat(data.biometricLockEncryptsDb).isFalse()
        assertThat(data.dataInPublicFolder).isFalse()
    }

    @Test
    fun `LegacyNotallyXPreferences isLegacyJson correctly detects legacy format`() {
        val legacy = Json.parseToJsonElement("""{"view": "list"}""").jsonObject
        val modern = Json.parseToJsonElement("""{"notesView": "LIST"}""").jsonObject

        assertThat(LegacyNotallyXPreferences.isLegacyJson(legacy)).isTrue()
        assertThat(LegacyNotallyXPreferences.isLegacyJson(modern)).isFalse()
    }
}

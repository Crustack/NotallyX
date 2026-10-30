package com.philkes.notallyx.core.preference.model

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.DefaultListNoteViewMode
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.ListItemSort
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.DEFAULT_EDIT_NOTE_BOTTOM_ACTION
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.DEFAULT_EDIT_NOTE_TOP_ACTIONS
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesView
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.core.preference.TEXT_SIZE_DEFAULT
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Type
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class UserPreferences(
    val theme: Theme = Theme.FOLLOW_SYSTEM,
    val useDynamicColors: Boolean = false,
    val textSizeNoteEditor: Float = TEXT_SIZE_DEFAULT,
    val textSizeOverview: Float = TEXT_SIZE_DEFAULT,
    val dateFormatOverview: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormatOverview: TimeFormat = TimeFormat.NONE,
    val dateFormatNoteView: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormatNoteView: TimeFormat = TimeFormat.TWENTY_FOUR_H,
    val notesView: NotesView = NotesView.LIST,
    val notesSorting: NotesSort = NotesSort(),
    val startView: String = "",
    val listItemSorting: ListItemSort = ListItemSort.AUTO_SORT_BY_CHECKED,
    val defaultListNoteViewMode: DefaultListNoteViewMode = DefaultListNoteViewMode.LAST_USED,
    val maxItems: Int = 4,
    val maxLines: Int = 8,
    val maxTitle: Int = 1,
    val labelsHidden: Set<String> = emptySet(),
    val labelTagsHiddenInOverview: Boolean = false,
    val imagesHiddenInOverview: Boolean = false,
    val alwaysShowSearchBar: Boolean = false,
    val maxLabels: Int = 5,
    val backupsFolder: String = "",
    val backupOnSave: Boolean = false,
    val periodicBackups: PeriodicBackup = PeriodicBackup(),
    val periodicBackupLastExecution: Long = -1L,
    val autoRemoveDeletedNotesAfterDays: Int = 0,
    val autoSaveAfterIdleTime: Int = 5,
    val biometricLock: BiometricLock = BiometricLock.DISABLED,
    val biometricLockEncryptsDb: Boolean = false,
    val iv: ByteArray? = null,
    val databaseEncryptionKey: ByteArray = ByteArray(0),
    val secureFlag: Boolean = false,
    val dataInPublicFolder: Boolean = false,
    val editNoteActivityTopActions: List<EditAction> = DEFAULT_EDIT_NOTE_TOP_ACTIONS,
    val editNoteActivityBottomAction: EditAction = DEFAULT_EDIT_NOTE_BOTTOM_ACTION,
    val dataSchemaId: Int = 0,
    val defaultNoteColor: String = BaseNote.COLOR_DEFAULT,
    val widgets: Map<Int, Long> = emptyMap(),
    val widgetNoteTypes: Map<Int, Type> = emptyMap(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserPreferences

        if (theme != other.theme) return false
        if (useDynamicColors != other.useDynamicColors) return false
        if (textSizeNoteEditor != other.textSizeNoteEditor) return false
        if (textSizeOverview != other.textSizeOverview) return false
        if (dateFormatOverview != other.dateFormatOverview) return false
        if (timeFormatOverview != other.timeFormatOverview) return false
        if (dateFormatNoteView != other.dateFormatNoteView) return false
        if (timeFormatNoteView != other.timeFormatNoteView) return false
        if (notesView != other.notesView) return false
        if (notesSorting != other.notesSorting) return false
        if (startView != other.startView) return false
        if (listItemSorting != other.listItemSorting) return false
        if (defaultListNoteViewMode != other.defaultListNoteViewMode) return false
        if (maxItems != other.maxItems) return false
        if (maxLines != other.maxLines) return false
        if (maxTitle != other.maxTitle) return false
        if (labelsHidden != other.labelsHidden) return false
        if (labelTagsHiddenInOverview != other.labelTagsHiddenInOverview) return false
        if (imagesHiddenInOverview != other.imagesHiddenInOverview) return false
        if (alwaysShowSearchBar != other.alwaysShowSearchBar) return false
        if (maxLabels != other.maxLabels) return false
        if (backupsFolder != other.backupsFolder) return false
        if (backupOnSave != other.backupOnSave) return false
        if (periodicBackups != other.periodicBackups) return false
        if (periodicBackupLastExecution != other.periodicBackupLastExecution) return false
        if (autoRemoveDeletedNotesAfterDays != other.autoRemoveDeletedNotesAfterDays) return false
        if (autoSaveAfterIdleTime != other.autoSaveAfterIdleTime) return false
        if (biometricLock != other.biometricLock) return false
        if (biometricLockEncryptsDb != other.biometricLockEncryptsDb) return false
        if (iv != null) {
            if (other.iv == null) return false
            if (!iv.contentEquals(other.iv)) return false
        } else if (other.iv != null) return false
        if (!databaseEncryptionKey.contentEquals(other.databaseEncryptionKey)) return false
        if (secureFlag != other.secureFlag) return false
        if (dataInPublicFolder != other.dataInPublicFolder) return false
        if (editNoteActivityTopActions != other.editNoteActivityTopActions) return false
        if (editNoteActivityBottomAction != other.editNoteActivityBottomAction) return false
        if (dataSchemaId != other.dataSchemaId) return false
        if (defaultNoteColor != other.defaultNoteColor) return false
        if (widgets != other.widgets) return false
        if (widgetNoteTypes != other.widgetNoteTypes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = theme.hashCode()
        result = 31 * result + useDynamicColors.hashCode()
        result = 31 * result + textSizeNoteEditor.hashCode()
        result = 31 * result + textSizeOverview.hashCode()
        result = 31 * result + dateFormatOverview.hashCode()
        result = 31 * result + timeFormatOverview.hashCode()
        result = 31 * result + dateFormatNoteView.hashCode()
        result = 31 * result + timeFormatNoteView.hashCode()
        result = 31 * result + notesView.hashCode()
        result = 31 * result + notesSorting.hashCode()
        result = 31 * result + startView.hashCode()
        result = 31 * result + listItemSorting.hashCode()
        result = 31 * result + defaultListNoteViewMode.hashCode()
        result = 31 * result + maxItems
        result = 31 * result + maxLines
        result = 31 * result + maxTitle
        result = 31 * result + labelsHidden.hashCode()
        result = 31 * result + labelTagsHiddenInOverview.hashCode()
        result = 31 * result + imagesHiddenInOverview.hashCode()
        result = 31 * result + alwaysShowSearchBar.hashCode()
        result = 31 * result + maxLabels
        result = 31 * result + backupsFolder.hashCode()
        result = 31 * result + backupOnSave.hashCode()
        result = 31 * result + periodicBackups.hashCode()
        result = 31 * result + periodicBackupLastExecution.hashCode()
        result = 31 * result + autoRemoveDeletedNotesAfterDays
        result = 31 * result + autoSaveAfterIdleTime
        result = 31 * result + biometricLock.hashCode()
        result = 31 * result + biometricLockEncryptsDb.hashCode()
        result = 31 * result + (iv?.contentHashCode() ?: 0)
        result = 31 * result + databaseEncryptionKey.contentHashCode()
        result = 31 * result + secureFlag.hashCode()
        result = 31 * result + dataInPublicFolder.hashCode()
        result = 31 * result + editNoteActivityTopActions.hashCode()
        result = 31 * result + editNoteActivityBottomAction.hashCode()
        result = 31 * result + dataSchemaId
        result = 31 * result + defaultNoteColor.hashCode()
        result = 31 * result + widgets.hashCode()
        result = 31 * result + widgetNoteTypes.hashCode()
        return result
    }
}

object UserPreferencesSerializer : Serializer<UserPreferences> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override val defaultValue: UserPreferences = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        return try {
            json.decodeFromString(
                UserPreferences.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to deserialize UserPreferences", e)
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        output.write(json.encodeToString(UserPreferences.serializer(), t).encodeToByteArray())
    }
}

val UserPreferencesCorruptionHandler = ReplaceFileCorruptionHandler { UserPreferences() }

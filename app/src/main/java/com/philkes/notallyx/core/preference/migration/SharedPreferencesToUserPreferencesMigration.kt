package com.philkes.notallyx.core.preference.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
import com.philkes.notallyx.core.common.extensions.deserializeEnums
import com.philkes.notallyx.core.common.extensions.fromCamelCaseToEnumName
import com.philkes.notallyx.core.common.extensions.getDefaultSharedPreferences
import com.philkes.notallyx.core.common.extensions.toPreservedByteArray
import com.philkes.notallyx.core.preference.EditAction
import com.philkes.notallyx.core.preference.NotesSort
import com.philkes.notallyx.core.preference.NotesSortBy
import com.philkes.notallyx.core.preference.PeriodicBackup
import com.philkes.notallyx.core.preference.SortDirection
import com.philkes.notallyx.core.preference.model.UserPreferences
import com.philkes.notallyx.data.model.Type

class SharedPreferencesToUserPreferencesMigration(
    private val context: Context,
    private val sharedPrefsProvider: (() -> SharedPreferences)? = null,
) : DataMigration<UserPreferences> {

    private val sharedPrefs by lazy {
        sharedPrefsProvider?.invoke() ?: context.getDefaultSharedPreferences()
    }

    override suspend fun shouldMigrate(currentData: UserPreferences): Boolean {
        return sharedPrefs.all.isNotEmpty()
    }

    override suspend fun migrate(currentData: UserPreferences): UserPreferences {
        val sp = sharedPrefs
        val all = sp.all

        val widgets = currentData.widgets.toMutableMap()
        val widgetNoteTypes = currentData.widgetNoteTypes.toMutableMap()

        for ((key, value) in all) {
            if (key.startsWith("widget:")) {
                val id = key.substringAfter("widget:").toIntOrNull()
                val noteId = (value as? Number)?.toLong()
                if (id != null && noteId != null) {
                    widgets[id] = noteId
                }
            } else if (key.startsWith("widgetNoteType:")) {
                val id = key.substringAfter("widgetNoteType:").toIntOrNull()
                val typeStr = value as? String
                if (id != null && typeStr != null) {
                    try {
                        widgetNoteTypes[id] = Type.valueOf(typeStr)
                    } catch (_: Exception) {}
                }
            }
        }

        val sortedByName = sp.getString("notesSorting", null)
        val sortedBy =
            sortedByName?.let {
                try {
                    NotesSortBy.fromValue(it)
                } catch (_: Exception) {
                    currentData.notesSorting.sortedBy
                }
            } ?: currentData.notesSorting.sortedBy

        val sortDirectionName = sp.getString("notesSortingDirection", null)
        val sortDirection =
            sortDirectionName?.let {
                try {
                    SortDirection.valueOf(it)
                } catch (_: Exception) {
                    currentData.notesSorting.sortDirection
                }
            } ?: currentData.notesSorting.sortDirection

        val notesSorting = NotesSort(sortedBy, sortDirection)

        val periodInDays =
            sp.getInt("autoBackupPeriodDays", currentData.periodicBackups.periodInDays)
        val maxBackups = sp.getInt("maxBackups", currentData.periodicBackups.maxBackups)
        val periodicBackups = PeriodicBackup(periodInDays, maxBackups)

        val editTopActions =
            sp.getString("editNoteActivityTopActions", null)?.let {
                EditAction::class.java.deserializeEnums(it)
            } ?: currentData.editNoteActivityTopActions

        val iv = sp.getString("encryption_iv", null)?.toPreservedByteArray ?: currentData.iv
        val databaseEncryptionKey =
            sp.getString("database_encryption_key", null)?.toPreservedByteArray
                ?: currentData.databaseEncryptionKey

        return currentData.copy(
            theme = sp.getEnum("theme", currentData.theme),
            useDynamicColors = sp.getBoolean("useDynamicColors", currentData.useDynamicColors),
            textSizeNoteEditor = sp.getFloat("textSizeNoteEditor", currentData.textSizeNoteEditor),
            textSizeOverview = sp.getFloat("textSizeOverview", currentData.textSizeOverview),
            dateFormatOverview = sp.getEnum("dateFormatOverview", currentData.dateFormatOverview),
            timeFormatOverview = sp.getEnum("timeFormatOverview", currentData.timeFormatOverview),
            dateFormatNoteView = sp.getEnum("dateFormatNoteView", currentData.dateFormatNoteView),
            timeFormatNoteView = sp.getEnum("timeFormatNoteView", currentData.timeFormatNoteView),
            notesView = sp.getEnum("view", currentData.notesView),
            notesSorting = notesSorting,
            startView = sp.getString("startView", currentData.startView) ?: currentData.startView,
            listItemSorting = sp.getEnum("listItemSorting", currentData.listItemSorting),
            defaultListNoteViewMode =
                sp.getEnum("defaultListNoteViewMode", currentData.defaultListNoteViewMode),
            maxItems = sp.getInt("maxItemsToDisplayInList.v1", currentData.maxItems),
            maxLines = sp.getInt("maxLinesToDisplayInNote.v1", currentData.maxLines),
            maxTitle = sp.getInt("maxLinesToDisplayInTitle", currentData.maxTitle),
            labelsHidden =
                sp.getStringSet("labelsHiddenInNavigation", null) ?: currentData.labelsHidden,
            labelTagsHiddenInOverview =
                sp.getBoolean("labelsHiddenInOverview", currentData.labelTagsHiddenInOverview),
            imagesHiddenInOverview =
                sp.getBoolean("imagesHiddenInOverview", currentData.imagesHiddenInOverview),
            alwaysShowSearchBar =
                sp.getBoolean("alwaysShowSearchBar", currentData.alwaysShowSearchBar),
            maxLabels = sp.getInt("maxLabelsInNavigation", currentData.maxLabels),
            backupsFolder =
                sp.getString("autoBackup", currentData.backupsFolder) ?: currentData.backupsFolder,
            backupOnSave = sp.getBoolean("backupOnSave", currentData.backupOnSave),
            periodicBackups = periodicBackups,
            periodicBackupLastExecution =
                sp.getLong(
                    "periodicBackupLastExecution",
                    currentData.periodicBackupLastExecution,
                ),
            autoRemoveDeletedNotesAfterDays =
                sp.getInt(
                    "autoRemoveDeletedNotesAfterDays",
                    currentData.autoRemoveDeletedNotesAfterDays,
                ),
            autoSaveAfterIdleTime =
                sp.getInt("autoSaveAfterIdleTime", currentData.autoSaveAfterIdleTime),
            biometricLock = sp.getEnum("biometricLock", currentData.biometricLock),
            biometricLockEncryptsDb =
                sp.getBoolean("biometricLockEncryptsDb", currentData.biometricLockEncryptsDb),
            iv = iv,
            databaseEncryptionKey = databaseEncryptionKey,
            secureFlag = sp.getBoolean("secureFlag", currentData.secureFlag),
            dataInPublicFolder =
                sp.getBoolean("dataOnExternalStorage", currentData.dataInPublicFolder),
            editNoteActivityTopActions = editTopActions,
            editNoteActivityBottomAction =
                sp.getEnum(
                    "editNoteActivityBottomAction",
                    currentData.editNoteActivityBottomAction,
                ),
            dataSchemaId = sp.getInt("dataSchemaId", currentData.dataSchemaId),
            defaultNoteColor =
                sp.getString("defaultNoteColor", currentData.defaultNoteColor)
                    ?: currentData.defaultNoteColor,
            widgets = widgets,
            widgetNoteTypes = widgetNoteTypes,
        )
    }

    override suspend fun cleanUp() {
        sharedPrefs.edit(commit = true) { clear() }
    }

    private inline fun <reified T : Enum<T>> SharedPreferences.getEnum(
        key: String,
        defaultValue: T,
    ): T {
        val str = getString(key, null) ?: return defaultValue
        return try {
            java.lang.Enum.valueOf(T::class.java, str.fromCamelCaseToEnumName())
        } catch (_: Exception) {
            try {
                java.lang.Enum.valueOf(T::class.java, str)
            } catch (_: Exception) {
                defaultValue
            }
        }
    }
}

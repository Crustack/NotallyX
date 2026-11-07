package com.philkes.notallyx.core.preference

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.FileStorage
import androidx.datastore.dataStoreFile
import androidx.datastore.tink.AeadSerializer
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getExternalBackupsDirectory
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.migration.LegacyEncryptedPrefsMigration
import com.philkes.notallyx.core.preference.migration.SharedPreferencesToUserPreferencesMigration
import com.philkes.notallyx.core.preference.model.SecurePreferences
import com.philkes.notallyx.core.preference.model.SecurePreferencesCorruptionHandler
import com.philkes.notallyx.core.preference.model.SecurePreferencesSerializer
import com.philkes.notallyx.core.preference.model.UserPreferences
import com.philkes.notallyx.core.preference.model.UserPreferencesCorruptionHandler
import com.philkes.notallyx.core.preference.model.UserPreferencesSerializer
import com.philkes.notallyx.core.security.TinkAeadProvider
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Type
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

const val TEXT_SIZE_DEFAULT = 16f
const val TEXT_SIZE_MIN = 12f
const val TEXT_SIZE_MAX = 32f

const val NUMBER_PREF_DISABLED = -1

const val MAX_TITLE_LINES_MIN = 1
const val MAX_TITLE_LINES_MAX = 10

const val MAX_LABELS_IN_NAVIGATION_MAX = 200

const val MAX_ITEMS_MAX = 10

class NotallyXPreferences(
    val userDataStore: DataStore<UserPreferences>,
    val secureDataStore: DataStore<SecurePreferences>,
    val context: Context,
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    val userPreferencesFlow: StateFlow<UserPreferences> =
        userDataStore.data
            .catch { e -> if (e is IOException) emit(UserPreferences()) else throw e }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue =
                    runBlocking {
                        try {
                            userDataStore.data.first()
                        } catch (_: Exception) {
                            UserPreferences()
                        }
                    },
            )

    val securePreferencesFlow: StateFlow<SecurePreferences> =
        secureDataStore.data
            .catch { e -> if (e is IOException) emit(SecurePreferences()) else throw e }
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue =
                    runBlocking {
                        try {
                            secureDataStore.data.first()
                        } catch (_: Exception) {
                            SecurePreferences()
                        }
                    },
            )

    val theme: EnumPreference<Theme> =
        EnumPreference(
            key = "theme",
            defaultValue = Theme.FOLLOW_SYSTEM,
            enumClass = Theme::class.java,
            titleResId = R.string.theme,
            flow = userPreferencesFlow.mapState(scope) { it.theme },
            onUpdate = { newTheme -> userDataStore.updateData { it.copy(theme = newTheme) } },
            scope = scope,
        )

    val useDynamicColors: BooleanPreference =
        BooleanPreference(
            key = "useDynamicColors",
            defaultValue = false,
            flow = userPreferencesFlow.mapState(scope) { it.useDynamicColors },
            onUpdate = { value -> userDataStore.updateData { it.copy(useDynamicColors = value) } },
            scope = scope,
        )

    val textSizeNoteEditor: FloatPreference =
        FloatPreference(
            key = "textSizeNoteEditor",
            defaultValue = TEXT_SIZE_DEFAULT,
            min = TEXT_SIZE_MIN,
            max = TEXT_SIZE_MAX,
            titleResId = R.string.text_size_note_editor,
            flow = userPreferencesFlow.mapState(scope) { it.textSizeNoteEditor },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(textSizeNoteEditor = value) }
            },
            scope = scope,
        )

    val textSizeOverview: FloatPreference =
        FloatPreference(
            key = "textSizeOverview",
            defaultValue = TEXT_SIZE_DEFAULT,
            min = TEXT_SIZE_MIN,
            max = TEXT_SIZE_MAX,
            titleResId = R.string.text_size_overview,
            flow = userPreferencesFlow.mapState(scope) { it.textSizeOverview },
            onUpdate = { value -> userDataStore.updateData { it.copy(textSizeOverview = value) } },
            scope = scope,
        )

    val dateFormatOverview: EnumPreference<DateFormat> =
        EnumPreference(
            key = "dateFormatOverview",
            defaultValue = DateFormat.DD_MM_YY_GER,
            enumClass = DateFormat::class.java,
            titleResId = R.string.date_format_overview,
            flow = userPreferencesFlow.mapState(scope) { it.dateFormatOverview },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(dateFormatOverview = value) }
            },
            scope = scope,
        )

    val timeFormatOverview: EnumPreference<TimeFormat> =
        EnumPreference(
            key = "timeFormatOverview",
            defaultValue = TimeFormat.NONE,
            enumClass = TimeFormat::class.java,
            titleResId = R.string.time_format_overview,
            flow = userPreferencesFlow.mapState(scope) { it.timeFormatOverview },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(timeFormatOverview = value) }
            },
            scope = scope,
        )

    val dateFormatNoteView: EnumPreference<DateFormat> =
        EnumPreference(
            key = "dateFormatNoteView",
            defaultValue = DateFormat.DD_MM_YY_GER,
            enumClass = DateFormat::class.java,
            titleResId = R.string.date_format_note_view,
            flow = userPreferencesFlow.mapState(scope) { it.dateFormatNoteView },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(dateFormatNoteView = value) }
            },
            scope = scope,
        )

    val timeFormatNoteView: EnumPreference<TimeFormat> =
        EnumPreference(
            key = "timeFormatNoteView",
            defaultValue = TimeFormat.TWENTY_FOUR_H,
            enumClass = TimeFormat::class.java,
            titleResId = R.string.time_format_note_view,
            flow = userPreferencesFlow.mapState(scope) { it.timeFormatNoteView },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(timeFormatNoteView = value) }
            },
            scope = scope,
        )

    val notesView: EnumPreference<NotesView> =
        EnumPreference(
            key = "view",
            defaultValue = NotesView.LIST,
            enumClass = NotesView::class.java,
            titleResId = R.string.view,
            flow = userPreferencesFlow.mapState(scope) { it.notesView },
            onUpdate = { value -> userDataStore.updateData { it.copy(notesView = value) } },
            scope = scope,
        )

    val notesSorting: NotesSortPreference =
        NotesSortPreference(
            flow = userPreferencesFlow.mapState(scope) { it.notesSorting },
            onUpdate = { value -> userDataStore.updateData { it.copy(notesSorting = value) } },
            scope = scope,
        )

    val startView: StringPreference =
        StringPreference(
            key = "startView",
            defaultValue = START_VIEW_DEFAULT,
            titleResId = R.string.start_view,
            flow = userPreferencesFlow.mapState(scope) { it.startView },
            onUpdate = { value -> userDataStore.updateData { it.copy(startView = value) } },
            scope = scope,
        )

    val listItemSorting: EnumPreference<ListItemSort> =
        EnumPreference(
            key = "listItemSorting",
            defaultValue = ListItemSort.AUTO_SORT_BY_CHECKED,
            enumClass = ListItemSort::class.java,
            titleResId = R.string.list_item_auto_sort,
            flow = userPreferencesFlow.mapState(scope) { it.listItemSorting },
            onUpdate = { value -> userDataStore.updateData { it.copy(listItemSorting = value) } },
            scope = scope,
        )

    val defaultListNoteViewMode: EnumPreference<DefaultListNoteViewMode> =
        EnumPreference(
            key = "defaultListNoteViewMode",
            defaultValue = DefaultListNoteViewMode.LAST_USED,
            enumClass = DefaultListNoteViewMode::class.java,
            titleResId = R.string.default_list_note_view_mode,
            flow = userPreferencesFlow.mapState(scope) { it.defaultListNoteViewMode },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(defaultListNoteViewMode = value) }
            },
            scope = scope,
        )

    val maxItems: IntPreference =
        IntPreference(
            key = "maxItemsToDisplayInList.v1",
            defaultValue = 4,
            min = 0,
            max = MAX_ITEMS_MAX,
            titleResId = R.string.max_items_to_display,
            flow = userPreferencesFlow.mapState(scope) { it.maxItems },
            onUpdate = { value -> userDataStore.updateData { it.copy(maxItems = value) } },
            scope = scope,
        )

    val maxLines: IntPreference =
        IntPreference(
            key = "maxLinesToDisplayInNote.v1",
            defaultValue = 8,
            min = 0,
            max = MAX_ITEMS_MAX,
            titleResId = R.string.max_lines_to_display,
            flow = userPreferencesFlow.mapState(scope) { it.maxLines },
            onUpdate = { value -> userDataStore.updateData { it.copy(maxLines = value) } },
            scope = scope,
        )

    val maxTitle: IntPreference =
        IntPreference(
            key = "maxLinesToDisplayInTitle",
            defaultValue = 1,
            min = MAX_TITLE_LINES_MIN,
            max = MAX_TITLE_LINES_MAX,
            titleResId = R.string.max_lines_to_display_title,
            flow = userPreferencesFlow.mapState(scope) { it.maxTitle },
            onUpdate = { value -> userDataStore.updateData { it.copy(maxTitle = value) } },
            scope = scope,
        )

    val labelsHidden: StringSetPreference =
        StringSetPreference(
            key = "labelsHiddenInNavigation",
            defaultValue = emptySet(),
            flow = userPreferencesFlow.mapState(scope) { it.labelsHidden },
            onUpdate = { value -> userDataStore.updateData { it.copy(labelsHidden = value) } },
            scope = scope,
        )

    val labelTagsHiddenInOverview: BooleanPreference =
        BooleanPreference(
            key = "labelsHiddenInOverview",
            defaultValue = false,
            titleResId = R.string.labels_hidden_in_overview_title,
            flow = userPreferencesFlow.mapState(scope) { it.labelTagsHiddenInOverview },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(labelTagsHiddenInOverview = value) }
            },
            scope = scope,
        )

    val imagesHiddenInOverview: BooleanPreference =
        BooleanPreference(
            key = "imagesHiddenInOverview",
            defaultValue = false,
            titleResId = R.string.images_hidden_in_overview_title,
            flow = userPreferencesFlow.mapState(scope) { it.imagesHiddenInOverview },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(imagesHiddenInOverview = value) }
            },
            scope = scope,
        )

    val alwaysShowSearchBar: BooleanPreference =
        BooleanPreference(
            key = "alwaysShowSearchBar",
            defaultValue = false,
            titleResId = R.string.always_show_search_bar,
            flow = userPreferencesFlow.mapState(scope) { it.alwaysShowSearchBar },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(alwaysShowSearchBar = value) }
            },
            scope = scope,
        )

    val maxLabels: IntPreference =
        IntPreference(
            key = "maxLabelsInNavigation",
            defaultValue = 5,
            min = 0,
            max = MAX_LABELS_IN_NAVIGATION_MAX,
            titleResId = R.string.max_labels_to_display,
            flow = userPreferencesFlow.mapState(scope) { it.maxLabels },
            onUpdate = { value -> userDataStore.updateData { it.copy(maxLabels = value) } },
            scope = scope,
        )

    val defaultBackupsFolder: String
        get() =
            try {
                context.getExternalBackupsDirectory().toUri().toString()
            } catch (_: Exception) {
                EMPTY_PATH
            }

    val backupsFolder: StringPreference =
        object :
            StringPreference(
                key = "autoBackup",
                defaultValue = EMPTY_PATH,
                titleResId = R.string.auto_backups_folder,
                flow = userPreferencesFlow.mapState(scope) { it.backupsFolder },
                onUpdate = { value -> userDataStore.updateData { it.copy(backupsFolder = value) } },
                scope = scope,
            ) {
            override val defaultValue: String
                get() = defaultBackupsFolder

            override val value: String
                get() = super.value.ifEmpty { defaultBackupsFolder }
        }

    val backupOnSave: BooleanPreference =
        BooleanPreference(
            key = "backupOnSave",
            defaultValue = false,
            titleResId = R.string.auto_backup_on_save,
            flow = userPreferencesFlow.mapState(scope) { it.backupOnSave },
            onUpdate = { value -> userDataStore.updateData { it.copy(backupOnSave = value) } },
            scope = scope,
        )

    val periodicBackups: PeriodicBackupsPreference =
        PeriodicBackupsPreference(
            flow = userPreferencesFlow.mapState(scope) { it.periodicBackups },
            onUpdate = { value -> userDataStore.updateData { it.copy(periodicBackups = value) } },
            scope = scope,
        )

    val periodicBackupLastExecution: LongPreference =
        LongPreference(
            key = "periodicBackupLastExecution",
            defaultValue = -1L,
            flow = userPreferencesFlow.mapState(scope) { it.periodicBackupLastExecution },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(periodicBackupLastExecution = value) }
            },
            scope = scope,
        )

    val autoRemoveDeletedNotesAfterDays: IntPreference =
        IntPreference(
            key = "autoRemoveDeletedNotesAfterDays",
            defaultValue = 0,
            min = 0,
            max = Integer.MAX_VALUE,
            titleResId = R.string.auto_remove_deleted_notes,
            flow = userPreferencesFlow.mapState(scope) { it.autoRemoveDeletedNotesAfterDays },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(autoRemoveDeletedNotesAfterDays = value) }
            },
            scope = scope,
        )

    val backupPassword: StringPreference =
        StringPreference(
            key = "backupPassword",
            defaultValue = PASSWORD_EMPTY,
            titleResId = R.string.backup_password,
            flow = securePreferencesFlow.mapState(scope) { it.backupPassword },
            onUpdate = { value -> secureDataStore.updateData { it.copy(backupPassword = value) } },
            scope = scope,
        )

    val autoSaveAfterIdleTime: IntPreference =
        IntPreference(
            key = "autoSaveAfterIdleTime",
            defaultValue = 5,
            min = -1,
            max = 60 * 60 * 5,
            titleResId = R.string.auto_save_after_idle_time,
            flow = userPreferencesFlow.mapState(scope) { it.autoSaveAfterIdleTime },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(autoSaveAfterIdleTime = value) }
            },
            scope = scope,
        )

    val biometricLock: EnumPreference<BiometricLock> =
        EnumPreference(
            key = "biometricLock",
            defaultValue = BiometricLock.DISABLED,
            enumClass = BiometricLock::class.java,
            titleResId = R.string.biometric_lock,
            flow = userPreferencesFlow.mapState(scope) { it.biometricLock },
            onUpdate = { value -> userDataStore.updateData { it.copy(biometricLock = value) } },
            scope = scope,
        )

    val biometricLockEncryptsDb: BooleanPreference =
        BooleanPreference(
            key = "biometricLockEncryptsDb",
            defaultValue = false,
            flow = userPreferencesFlow.mapState(scope) { it.biometricLockEncryptsDb },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(biometricLockEncryptsDb = value) }
            },
            scope = scope,
        )

    val iv: ByteArrayPreference =
        ByteArrayPreference(
            key = "encryption_iv",
            defaultValue = null,
            flow = userPreferencesFlow.mapState(scope) { it.iv },
            onUpdate = { value -> userDataStore.updateData { it.copy(iv = value) } },
            scope = scope,
        )

    val databaseEncryptionKey: EncryptedPassphrasePreference =
        EncryptedPassphrasePreference(
            key = "database_encryption_key",
            defaultValue = ByteArray(0),
            flow = userPreferencesFlow.mapState(scope) { it.databaseEncryptionKey },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(databaseEncryptionKey = value) }
            },
            scope = scope,
        )

    val fallbackDatabaseEncryptionKey: ByteArrayPreference =
        ByteArrayPreference(
            key = "fallback_database_encryption_key",
            defaultValue = ByteArray(0),
            flow = securePreferencesFlow.mapState(scope) { it.fallbackDatabaseEncryptionKey },
            onUpdate = { value ->
                secureDataStore.updateData {
                    it.copy(fallbackDatabaseEncryptionKey = value ?: ByteArray(0))
                }
            },
            scope = scope,
        )

    val secureFlag: BooleanPreference =
        BooleanPreference(
            key = "secureFlag",
            defaultValue = false,
            titleResId = R.string.disallow_screenshots,
            flow = userPreferencesFlow.mapState(scope) { it.secureFlag },
            onUpdate = { value -> userDataStore.updateData { it.copy(secureFlag = value) } },
            scope = scope,
        )

    val dataInPublicFolder: BooleanPreference =
        BooleanPreference(
            key = "dataOnExternalStorage",
            defaultValue = false,
            titleResId = R.string.data_in_public,
            flow = userPreferencesFlow.mapState(scope) { it.dataInPublicFolder },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(dataInPublicFolder = value) }
            },
            scope = scope,
        )

    val editNoteActivityTopActions: EnumListPreference<EditAction> =
        EnumListPreference(
            key = "editNoteActivityTopActions",
            defaultValue = DEFAULT_EDIT_NOTE_TOP_ACTIONS,
            enumClass = EditAction::class.java,
            flow = userPreferencesFlow.mapState(scope) { it.editNoteActivityTopActions },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(editNoteActivityTopActions = value) }
            },
            scope = scope,
        )

    val editNoteActivityBottomAction: EnumPreference<EditAction> =
        EnumPreference(
            key = "editNoteActivityBottomAction",
            defaultValue = DEFAULT_EDIT_NOTE_BOTTOM_ACTION,
            enumClass = EditAction::class.java,
            flow = userPreferencesFlow.mapState(scope) { it.editNoteActivityBottomAction },
            onUpdate = { value ->
                userDataStore.updateData { it.copy(editNoteActivityBottomAction = value) }
            },
            scope = scope,
        )

    fun getSafeEditNoteActivityTopActions(): List<EditAction> {
        return editNoteActivityTopActions.value.let { actions ->
            if (actions.size != 3) {
                editNoteActivityTopActions.save(DEFAULT_EDIT_NOTE_TOP_ACTIONS)
                DEFAULT_EDIT_NOTE_TOP_ACTIONS
            } else {
                actions
            }
        }
    }

    val dataSchemaId: IntPreference =
        IntPreference(
            key = "dataSchemaId",
            defaultValue = 0,
            min = 0,
            max = Integer.MAX_VALUE,
            flow = userPreferencesFlow.mapState(scope) { it.dataSchemaId },
            onUpdate = { value -> userDataStore.updateData { it.copy(dataSchemaId = value) } },
            scope = scope,
        )

    val defaultNoteColor: StringPreference =
        StringPreference(
            key = "defaultNoteColor",
            defaultValue = BaseNote.COLOR_DEFAULT,
            flow = userPreferencesFlow.mapState(scope) { it.defaultNoteColor },
            onUpdate = { value -> userDataStore.updateData { it.copy(defaultNoteColor = value) } },
            scope = scope,
        )

    fun setDataSchemaId(value: Int) {
        dataSchemaId.save(value)
    }

    fun getWidgetData(id: Int): Long = userPreferencesFlow.value.widgets[id] ?: 0L

    fun getWidgetNoteType(id: Int): Type? = userPreferencesFlow.value.widgetNoteTypes[id]

    fun deleteWidget(id: Int) {
        scope.launch {
            userDataStore.updateData {
                it.copy(
                    widgets = it.widgets - id,
                    widgetNoteTypes = it.widgetNoteTypes - id,
                )
            }
        }
    }

    fun updateWidget(id: Int, noteId: Long, noteType: Type) {
        scope.launch {
            userDataStore.updateData {
                it.copy(
                    widgets = it.widgets + (id to noteId),
                    widgetNoteTypes = it.widgetNoteTypes + (id to noteType),
                )
            }
        }
    }

    fun getUpdatableWidgets(noteIds: LongArray? = null): List<Pair<Int, Long>> {
        val widgets = userPreferencesFlow.value.widgets
        val updatable = ArrayList<Pair<Int, Long>>()
        for ((id, noteId) in widgets) {
            if (noteIds == null || noteIds.contains(noteId)) {
                updatable.add(Pair(id, noteId))
            }
        }
        return updatable
    }

    fun showDateCreated(): Boolean {
        return dateFormatNoteView.value != DateFormat.NONE ||
            timeFormatNoteView.value != TimeFormat.NONE
    }

    private val exportJson = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val importExportIgnored =
        setOf(
            "biometricLock",
            "biometricLockEncryptsDb",
            "iv",
            "encryption_iv",
            "databaseEncryptionKey",
            "database_encryption_key",
            "dataInPublicFolder",
            "dataOnExternalStorage",
            "dataSchemaId",
        )

    fun toJsonString(): String {
        val userPrefs = userPreferencesFlow.value
        val jsonElement =
            exportJson.encodeToJsonElement(UserPreferences.serializer(), userPrefs).jsonObject
        val filtered = JsonObject(jsonElement.filterKeys { it !in importExportIgnored })
        return exportJson.encodeToString(JsonObject.serializer(), filtered)
    }

    fun import(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return false
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            import(jsonString)
        } catch (_: Exception) {
            false
        }
    }

    fun import(jsonString: String): Boolean {
        return try {
            val rawJson = exportJson.parseToJsonElement(jsonString).jsonObject
            val normalizedJson = LegacyNotallyXPreferences.normalize(rawJson, importExportIgnored)
            runBlocking {
                userDataStore.updateData { current ->
                    val currentJson =
                        exportJson
                            .encodeToJsonElement(UserPreferences.serializer(), current)
                            .jsonObject
                    val merged = buildJsonObject {
                        currentJson.forEach { (k, v) -> put(k, v) }
                        normalizedJson.forEach { (k, v) -> put(k, v) }
                    }
                    val imported = exportJson.decodeFromJsonElement<UserPreferences>(merged)
                    imported.copy(
                        biometricLock = current.biometricLock,
                        biometricLockEncryptsDb = current.biometricLockEncryptsDb,
                        iv = current.iv,
                        databaseEncryptionKey = current.databaseEncryptionKey,
                        dataInPublicFolder = current.dataInPublicFolder,
                    )
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun reset() {
        runBlocking {
            userDataStore.updateData { UserPreferences() }
            secureDataStore.updateData { SecurePreferences() }
        }
    }

    val isLockEnabled: Boolean
        get() = biometricLock.value == BiometricLock.ENABLED

    fun isDefaultOrEmptyBackupFolder(value: String) =
        value == EMPTY_PATH || value == backupsFolder.defaultValue

    companion object {
        const val EMPTY_PATH = "emptyPath"
        const val START_VIEW_DEFAULT = ""
        const val START_VIEW_UNLABELED = "com.philkes.notallyx.startview.UNLABELED"

        val DEFAULT_EDIT_NOTE_TOP_ACTIONS =
            listOf(EditAction.SEARCH, EditAction.REMINDERS, EditAction.PIN)
        val DEFAULT_EDIT_NOTE_BOTTOM_ACTION = EditAction.TOGGLE_VIEW_MODE

        @Volatile private var instance: NotallyXPreferences? = null

        fun getInstance(context: Context): NotallyXPreferences {
            return instance
                ?: synchronized(this) {
                    instance ?: create(context.applicationContext ?: context).also { instance = it }
                }
        }

        fun create(
            context: Context,
            userDataStore: DataStore<UserPreferences>? = null,
            secureDataStore: DataStore<SecurePreferences>? = null,
            scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        ): NotallyXPreferences {
            val userStore =
                userDataStore
                    ?: DataStoreFactory.create(
                        serializer = UserPreferencesSerializer,
                        produceFile = { context.dataStoreFile("notallyx_preferences.json") },
                        migrations = listOf(SharedPreferencesToUserPreferencesMigration(context)),
                        corruptionHandler = UserPreferencesCorruptionHandler,
                        scope = scope,
                    )

            val secureStore =
                secureDataStore
                    ?: run {
                        val aead = TinkAeadProvider.provideAead(context)
                        val secureFile = "notallyx_secure_preferences.bin"
                        DataStoreFactory.create(
                            storage =
                                FileStorage(
                                    serializer =
                                        AeadSerializer(
                                            aead = aead,
                                            wrappedSerializer = SecurePreferencesSerializer,
                                            associatedData = secureFile.encodeToByteArray(),
                                        ),
                                    produceFile = { context.dataStoreFile(secureFile) },
                                ),
                            migrations = listOf(LegacyEncryptedPrefsMigration(context)),
                            corruptionHandler = SecurePreferencesCorruptionHandler,
                            scope = scope,
                        )
                    }

            return NotallyXPreferences(userStore, secureStore, context, scope).also {
                if (instance == null) {
                    instance = it
                }
            }
        }

        fun clearInstance() {
            synchronized(this) {
                instance?.let { oldInstance ->
                    try {
                        runBlocking { oldInstance.scope.coroutineContext.job.cancelAndJoin() }
                    } catch (_: Exception) {}
                    try {
                        oldInstance.context.dataStoreFile("notallyx_preferences.json").delete()
                        oldInstance.context
                            .dataStoreFile("notallyx_secure_preferences.bin")
                            .delete()
                    } catch (_: Exception) {}
                }
                instance = null
            }
        }
    }
}

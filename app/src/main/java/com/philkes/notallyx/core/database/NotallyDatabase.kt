package com.philkes.notallyx.core.database

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.philkes.notallyx.NotallyXApplication.Companion.isTestRunner
import com.philkes.notallyx.core.common.extensions.getExternalMediaDirectory
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.getInitializedCipherForDecryption
import com.philkes.notallyx.core.security.isEncryptedDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.toColorString
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@ColumnTypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = 11)
abstract class NotallyDatabase : RoomDatabase() {

    abstract fun getLabelDao(): LabelDao

    abstract fun getCommonDao(): CommonDao

    abstract fun getBaseNoteDao(): BaseNoteDao

    suspend fun checkpoint(): Boolean {
        return try {
            val result = getBaseNoteDao().query(RoomRawQuery("pragma wal_checkpoint(TRUNCATE)"))
            result == 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun ping() = getBaseNoteDao().query(RoomRawQuery("SELECT 1")) == 1

    companion object {

        const val DATABASE_NAME = "NotallyDatabase"

        val instance: StateFlow<NotallyDatabase?>
            field = MutableStateFlow(null)
        @Volatile private var replacementInProgress = false

        fun getCurrentDatabaseFile(context: ContextWrapper): File {
            return if (NotallyXPreferences.getInstance(context).dataInPublicFolder.value) {
                getExternalDatabaseFile(context)
            } else {
                getInternalDatabaseFile(context)
            }
        }

        fun getCurrentDatabaseFiles(context: ContextWrapper): List<File> {
            return if (NotallyXPreferences.getInstance(context).dataInPublicFolder.value) {
                getExternalDatabaseFiles(context)
            } else {
                getInternalDatabaseFiles(context)
            }
        }

        fun getExternalDatabaseFile(context: ContextWrapper): File {
            return File(context.getExternalMediaDirectory(), DATABASE_NAME)
        }

        fun getExternalDatabaseFiles(context: ContextWrapper): List<File> {
            val directory = context.getExternalMediaDirectory()
            return listOf(
                    File(directory, DATABASE_NAME),
                    File(directory, "$DATABASE_NAME-shm"),
                    File(directory, "$DATABASE_NAME-wal"),
                    File(directory, "$DATABASE_NAME-journal"),
                )
                .filter { it.exists() }
        }

        fun getInternalDatabaseFile(context: Context): File {
            return context.getDatabasePath(DATABASE_NAME)
        }

        fun getInternalDatabaseFiles(context: ContextWrapper): List<File> {
            val directory = context.getDatabasePath(DATABASE_NAME).parentFile
            return listOf(
                    File(directory, DATABASE_NAME),
                    File(directory, "$DATABASE_NAME-shm"),
                    File(directory, "$DATABASE_NAME-wal"),
                )
                .filter { it.exists() }
        }

        private fun getCurrentDatabaseName(
            context: ContextWrapper,
            dataInPublicFolder: Boolean,
        ): String {
            return if (dataInPublicFolder) {
                getExternalDatabaseFile(context).absolutePath
            } else {
                DATABASE_NAME
            }
        }

        fun getDatabase(context: ContextWrapper): StateFlow<NotallyDatabase?> {
            if (instance.value == null && !replacementInProgress) {
                synchronized(this) {
                    if (instance.value == null) {
                        if (isTestRunner()) {
                            instance.value = getTestDatabase(context)
                        } else {
                            val preferences = NotallyXPreferences.getInstance(context)
                            instance.value = createInstance(context, preferences)
                        }
                    }
                }
            }
            return instance
        }

        fun clearInstance() {
            synchronized(this) {
                instance.value?.let { database ->
                    instance.value = null
                    database.close()
                }
                testInstance?.let { database ->
                    database.close()
                    testInstance = null
                }
            }
        }

        fun startReplacement() {
            replacementInProgress = true
        }

        fun endReplacement() {
            replacementInProgress = false
        }

        fun isBeingReplaced() = replacementInProgress

        private var testInstance: NotallyDatabase? = null

        private fun getTestDatabase(context: ContextWrapper): NotallyDatabase {
            return testInstance
                ?: synchronized(this) {
                    testInstance =
                        Room.inMemoryDatabaseBuilder(context, NotallyDatabase::class.java)
                            .allowMainThreadQueries()
                            .build()
                    return testInstance!!
                }
        }

        fun getFreshDatabase(
            context: ContextWrapper,
            dataInPublic: Boolean,
            biometricLock: BiometricLock,
        ): NotallyDatabase {
            return if (isTestRunner()) {
                getTestDatabase(context)
            } else {
                createInstance(
                    context,
                    NotallyXPreferences.getInstance(context),
                    dataInPublic = dataInPublic,
                    biometricLock = biometricLock,
                )
            }
        }

        private fun createInstance(
            context: ContextWrapper,
            preferences: NotallyXPreferences,
            dataInPublic: Boolean = preferences.dataInPublicFolder.value,
            biometricLock: BiometricLock = preferences.biometricLock.value,
        ): NotallyDatabase {
            Log.d(
                DATABASE_NAME,
                "Creating database instance with dataInPublic: '$dataInPublic' and biometric lock: '$biometricLock' (experimental encryption: ${preferences.biometricLockEncryptsDb.value})",
            )
            clearInstance()
            val instanceBuilder =
                builder(context, dataInPublic)
                    .setDriver(BackupSQLiteDriver(context, BackupOpenHelperFactory(context)))
                    .setupEncryption(context, preferences, biometricLock)
            return instanceBuilder.build()
        }

        internal fun builder(
            context: ContextWrapper,
            dataInPublic: Boolean,
        ): Builder<NotallyDatabase> =
            Room.databaseBuilder(
                    context,
                    NotallyDatabase::class.java,
                    getCurrentDatabaseName(context, dataInPublic),
                )
                .setDriver(
                    BackupSQLiteDriver(
                        context,
                        androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory(),
                    )
                )
                .addMigrations(
                    Migration2,
                    Migration3,
                    Migration4,
                    Migration5,
                    Migration6,
                    Migration7,
                    Migration8,
                    Migration9,
                    Migration10,
                    Migration11,
                )

        @VisibleForTesting
        internal fun Builder<NotallyDatabase>.setupEncryption(
            context: ContextWrapper,
            preferences: NotallyXPreferences,
            biometricLock: BiometricLock = preferences.biometricLock.value,
        ): Builder<NotallyDatabase> {
            return this.apply {
                val dbFile = getCurrentDatabaseFile(context)
                if (
                    preferences.biometricLockEncryptsDb.value ||
                        (biometricLock == BiometricLock.ENABLED &&
                            dbFile.isEncryptedDatabase(context))
                ) {
                    if (dbFile.isEncryptedDatabase(context)) {
                        initializeDecryption(context, preferences, this)
                    } else {
                        context.log(
                            DATABASE_NAME,
                            "Database is not encrypted even though biometric lock is enabled, disabling biometric lock",
                        )

                        preferences.biometricLock.save(BiometricLock.DISABLED)
                    }
                }
            }
        }

        private fun initializeDecryption(
            context: ContextWrapper,
            preferences: NotallyXPreferences,
            instanceBuilder: Builder<NotallyDatabase>,
        ) {
            System.loadLibrary("sqlcipher")
            val initializationVector =
                requireNotNull(preferences.iv.value) {
                    "IV cannot be null when initializing database decryption"
                }
            val cipher = getInitializedCipherForDecryption(iv = initializationVector)
            val encryptedPassphrase = preferences.databaseEncryptionKey.value
            val passphrase = cipher.doFinal(encryptedPassphrase)
            val factory = BackupOpenHelperFactory(context, SupportOpenHelperFactory(passphrase))
            instanceBuilder.setDriver(BackupSQLiteDriver(context, factory))
        }

        fun postNewInstance(
            context: ContextWrapper,
            dataInPublic: Boolean? = null,
            biometricLock: BiometricLock? = null,
        ) {
            val preferences = NotallyXPreferences.getInstance(context)
            val notallyDatabase =
                getFreshDatabase(
                    context,
                    dataInPublic ?: preferences.dataInPublicFolder.value,
                    biometricLock ?: preferences.biometricLock.value,
                )
            postInstance(notallyDatabase)
        }

        fun postInstance(notallyDatabase: NotallyDatabase) {
            synchronized(this) {
                instance.value = notallyDatabase
                replacementInProgress = false
            }
        }

        object Migration2 : Migration(1, 2) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `color` TEXT NOT NULL DEFAULT 'DEFAULT'"
                )
            }
        }

        object Migration3 : Migration(2, 3) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `images` TEXT NOT NULL DEFAULT `[]`"
                )
            }
        }

        object Migration4 : Migration(3, 4) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `audios` TEXT NOT NULL DEFAULT `[]`"
                )
            }
        }

        object Migration5 : Migration(4, 5) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `files` TEXT NOT NULL DEFAULT `[]`"
                )
            }
        }

        object Migration6 : Migration(5, 6) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `modifiedTimestamp` INTEGER NOT NULL DEFAULT 'timestamp'"
                )
            }
        }

        object Migration7 : Migration(6, 7) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `reminders` TEXT NOT NULL DEFAULT `[]`"
                )
            }
        }

        object Migration8 : Migration(7, 8) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val updates = mutableListOf<Pair<String, Long>>()
                connection.prepare("SELECT id, color FROM BaseNote").use { stmt ->
                    while (stmt.step()) {
                        val id = stmt.getLong(0)
                        val colorString = if (stmt.isNull(1)) "" else stmt.getText(1)
                        val color = Color.valueOfOrDefault(colorString)
                        val hexColor = color.toColorString()
                        updates.add(Pair(hexColor, id))
                    }
                }
                connection.prepare("UPDATE BaseNote SET color = ? WHERE id = ?").use { stmt ->
                    for ((hexColor, id) in updates) {
                        stmt.bindText(1, hexColor)
                        stmt.bindLong(2, id)
                        stmt.step()
                        stmt.reset()
                    }
                }
            }
        }

        object Migration9 : Migration(8, 9) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `viewMode` TEXT NOT NULL DEFAULT '${NoteViewMode.EDIT.name}'"
                )
            }
        }

        object Migration10 : Migration(9, 10) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `isPinnedToStatus` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        object Migration11 : Migration(10, 11) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `Label` ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0"
                )
                val values = mutableListOf<String>()
                connection.prepare("SELECT value FROM Label ORDER BY value DESC").use { stmt ->
                    while (stmt.step()) {
                        values.add(stmt.getText(0))
                    }
                }
                connection.prepare("UPDATE Label SET `order` = ? WHERE value = ?").use { stmt ->
                    values.forEachIndexed { order, value ->
                        stmt.bindLong(1, order.toLong())
                        stmt.bindText(2, value)
                        stmt.step()
                        stmt.reset()
                    }
                }
            }
        }
    }
}

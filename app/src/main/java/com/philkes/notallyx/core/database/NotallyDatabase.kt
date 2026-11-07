package com.philkes.notallyx.core.database

import android.content.Context
import android.content.ContextWrapper
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
import com.philkes.notallyx.core.common.extensions.copyToLarge
import com.philkes.notallyx.core.platform.AndroidPathProvider
import com.philkes.notallyx.core.platform.AppLogger
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.decryptDatabase
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
@Database(entities = [BaseNote::class, Label::class], version = NotallyDatabase.DATABASE_VERSION)
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
        const val DATABASE_VERSION = 11
        const val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false

        val ALL_MIGRATIONS: Array<Migration>
            get() =
                arrayOf(
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

        val instance: StateFlow<NotallyDatabase?>
            field = MutableStateFlow(null)
        @Volatile private var replacementInProgress = false

        fun getCurrentDatabaseFile(
            pathProvider: PathProvider,
            dataInPublicFolder: Boolean = false,
        ): File {
            return pathProvider.getDatabaseFile(DATABASE_NAME, dataInPublicFolder)
        }

        fun getCurrentDatabaseFiles(
            pathProvider: PathProvider,
            dataInPublicFolder: Boolean = false,
        ): List<File> {
            return pathProvider.getDatabaseFiles(DATABASE_NAME, dataInPublicFolder)
        }

        fun getExternalDatabaseFile(pathProvider: PathProvider): File {
            return pathProvider.getDatabaseFile(DATABASE_NAME, dataInPublicFolder = true)
        }

        fun getExternalDatabaseFiles(pathProvider: PathProvider): List<File> {
            return pathProvider.getDatabaseFiles(DATABASE_NAME, dataInPublicFolder = true)
        }

        fun getInternalDatabaseFile(pathProvider: PathProvider): File {
            return pathProvider.getDatabaseFile(DATABASE_NAME, dataInPublicFolder = false)
        }

        fun getInternalDatabaseFiles(pathProvider: PathProvider): List<File> {
            return pathProvider.getDatabaseFiles(DATABASE_NAME, dataInPublicFolder = false)
        }

        fun getCurrentDatabaseFile(context: ContextWrapper): File {
            val dataInPublic = NotallyXPreferences.getInstance(context).dataInPublicFolder.value
            return getCurrentDatabaseFile(AndroidPathProvider(context), dataInPublic)
        }

        fun getCurrentDatabaseFiles(context: ContextWrapper): List<File> {
            val dataInPublic = NotallyXPreferences.getInstance(context).dataInPublicFolder.value
            return getCurrentDatabaseFiles(AndroidPathProvider(context), dataInPublic)
        }

        fun getExternalDatabaseFile(context: ContextWrapper): File {
            return getExternalDatabaseFile(AndroidPathProvider(context))
        }

        fun getExternalDatabaseFiles(context: ContextWrapper): List<File> {
            return getExternalDatabaseFiles(AndroidPathProvider(context))
        }

        fun getInternalDatabaseFile(context: Context): File {
            return getInternalDatabaseFile(AndroidPathProvider(context))
        }

        fun getInternalDatabaseFiles(context: ContextWrapper): List<File> {
            return getInternalDatabaseFiles(AndroidPathProvider(context))
        }

        private fun getCurrentDatabaseName(
            pathProvider: PathProvider,
            dataInPublicFolder: Boolean,
        ): String {
            return if (dataInPublicFolder) {
                getExternalDatabaseFile(pathProvider).absolutePath
            } else {
                DATABASE_NAME
            }
        }

        fun builder(
            driverFactory: DatabaseDriverFactory,
            dataInPublic: Boolean = false,
            inMemory: Boolean = false,
        ): Builder<NotallyDatabase> {
            return driverFactory
                .createBuilder(DATABASE_NAME, dataInPublic, inMemory)
                .setDriver(driverFactory.createDriver(DATABASE_NAME, dataInPublic))
                .addMigrations(*ALL_MIGRATIONS)
        }

        fun createDatabase(
            driverFactory: DatabaseDriverFactory,
            dataInPublic: Boolean = false,
            inMemory: Boolean = false,
        ): NotallyDatabase {
            return builder(driverFactory, dataInPublic, inMemory).build()
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
            AppLogger.d(
                DATABASE_NAME,
                "Creating database instance with dataInPublic: '$dataInPublic' and biometric lock: '$biometricLock' (experimental encryption: ${preferences.biometricLockEncryptsDb.value})",
            )
            clearInstance()
            val instanceBuilder =
                builder(context, dataInPublic)
                    .setDriver(BackupSQLiteDriver(context, BackupOpenHelperFactory(context)))
                    .setupEncryption(context, preferences, biometricLock, dataInPublic)
            return instanceBuilder.build()
        }

        internal fun builder(
            context: ContextWrapper,
            dataInPublic: Boolean,
        ): Builder<NotallyDatabase> {
            val pathProvider = AndroidPathProvider(context)
            return Room.databaseBuilder(
                    context,
                    NotallyDatabase::class.java,
                    getCurrentDatabaseName(pathProvider, dataInPublic),
                )
                .setDriver(
                    BackupSQLiteDriver(
                        context,
                        androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory(),
                    )
                )
                .addMigrations(*ALL_MIGRATIONS)
        }

        @VisibleForTesting
        internal fun Builder<NotallyDatabase>.setupEncryption(
            context: ContextWrapper,
            preferences: NotallyXPreferences,
            biometricLock: BiometricLock = preferences.biometricLock.value,
            dataInPublic: Boolean = preferences.dataInPublicFolder.value,
        ): Builder<NotallyDatabase> {
            return this.apply {
                val pathProvider = AndroidPathProvider(context)
                val dbFile = getCurrentDatabaseFile(pathProvider, dataInPublic)
                if (
                    preferences.biometricLockEncryptsDb.value ||
                        (biometricLock == BiometricLock.ENABLED &&
                            dbFile.isEncryptedDatabase(context))
                ) {
                    if (dbFile.isEncryptedDatabase(context)) {
                        if (!FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB) {
                            val decrypted =
                                decryptDatabaseOnStartupIfNeeded(context, preferences, dataInPublic)
                            if (!decrypted) {
                                initializeDecryption(context, preferences, this)
                            }
                        } else {
                            initializeDecryption(context, preferences, this)
                        }
                    } else {
                        AppLogger.w(
                            DATABASE_NAME,
                            "Database is not encrypted even though biometric lock encryption was set, clearing biometricLockEncryptsDb",
                        )
                        preferences.biometricLockEncryptsDb.save(false)
                        if (FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB) {
                            preferences.biometricLock.save(BiometricLock.DISABLED)
                        }
                    }
                }
            }
        }

        /**
         * Performs a one-time non-destructive decryption migration on startup when
         * [FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB] is `false` and the database on disk is still
         * encrypted with SQLCipher. Preserves [BiometricLock.ENABLED] while resetting
         * `biometricLockEncryptsDb` to `false`.
         */
        fun decryptDatabaseOnStartupIfNeeded(
            context: ContextWrapper,
            preferences: NotallyXPreferences,
            dataInPublic: Boolean = preferences.dataInPublicFolder.value,
        ): Boolean {
            if (FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB) return false
            val pathProvider = AndroidPathProvider(context)
            val dbFile = getCurrentDatabaseFile(pathProvider, dataInPublic)
            if (!dbFile.exists() || !dbFile.isEncryptedDatabase(context)) {
                if (preferences.biometricLockEncryptsDb.value) {
                    preferences.biometricLockEncryptsDb.save(false)
                }
                return false
            }
            return try {
                val passphrase = resolveDecryptionPassphrase(preferences)
                val parentDir = dbFile.parentFile ?: context.filesDir
                val tempCopy = File(parentDir, "${dbFile.name}-startup-decrypt")
                val backupCopy = File(parentDir, "${dbFile.name}-startup-decrypt-backup")
                dbFile.copyToLarge(tempCopy, overwrite = true)
                dbFile.copyToLarge(backupCopy, overwrite = true)
                decryptDatabase(context, tempCopy, passphrase)
                getCurrentDatabaseFiles(pathProvider, dataInPublic).forEach { it.delete() }
                tempCopy.copyToLarge(dbFile, overwrite = true, deleteSourceFile = true)
                if (dbFile.isEncryptedDatabase(context)) {
                    backupCopy.copyToLarge(dbFile, overwrite = true, deleteSourceFile = true)
                    false
                } else {
                    backupCopy.delete()
                    preferences.biometricLockEncryptsDb.save(false)
                    true
                }
            } catch (e: Exception) {
                AppLogger.e(
                    DATABASE_NAME,
                    "Startup decryption migration failed, keeping encrypted fallback",
                    e,
                )
                false
            }
        }

        private fun resolveDecryptionPassphrase(preferences: NotallyXPreferences): ByteArray {
            val fallback = preferences.fallbackDatabaseEncryptionKey.value
            if (fallback != null && fallback.isNotEmpty()) {
                return fallback
            }
            val initializationVector =
                requireNotNull(preferences.iv.value) {
                    "IV cannot be null when initializing database decryption"
                }
            val cipher = getInitializedCipherForDecryption(iv = initializationVector)
            val encryptedPassphrase = preferences.databaseEncryptionKey.value
            require(encryptedPassphrase.isNotEmpty()) {
                "Encrypted passphrase is empty when initializing database decryption"
            }
            return cipher.doFinal(encryptedPassphrase)
        }

        private fun initializeDecryption(
            context: ContextWrapper,
            preferences: NotallyXPreferences,
            instanceBuilder: Builder<NotallyDatabase>,
        ) {
            System.loadLibrary("sqlcipher")
            val passphrase = resolveDecryptionPassphrase(preferences)
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

        object Migration5 : Migration(4, 5) {

            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `files` TEXT NOT NULL DEFAULT `[]`"
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

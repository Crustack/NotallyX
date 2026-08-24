package com.philkes.notallyx.data

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Observer
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.philkes.notallyx.NotallyXApplication.Companion.isTestRunner
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.presentation.view.misc.NotNullLiveData
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.observeForeverSkipFirst
import com.philkes.notallyx.utils.getExternalMediaDirectory
import com.philkes.notallyx.utils.security.SQLCipherUtils
import com.philkes.notallyx.utils.security.getInitializedCipherForDecryption
import java.io.File
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@TypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = 11)
abstract class NotallyDatabase : RoomDatabase() {

    abstract fun getLabelDao(): LabelDao

    abstract fun getCommonDao(): CommonDao

    abstract fun getBaseNoteDao(): BaseNoteDao

    /**
     * Runs a full WAL checkpoint. The first column of `pragma wal_checkpoint` is `busy`, which is
     * `1` when another connection held a read lock and the checkpoint could **not** be completed -
     * in that case the database file alone does not contain the most recent commits, so copying it
     * would silently lose them.
     *
     * @return `true` if all pages were written back into the database file
     */
    fun checkpoint(): Boolean {
        return getBaseNoteDao().query(SimpleSQLiteQuery("pragma wal_checkpoint(FULL)")) == 0
    }

    /**
     * Like [checkpoint], but retries a few times and throws if the write-ahead-log could not be
     * written back. To be used before the database file is copied or replaced.
     */
    fun checkpointOrThrow(attempts: Int = 3) {
        repeat(attempts) {
            if (checkpoint()) {
                return
            }
        }
        throw IllegalStateException(
            "Could not checkpoint the database after $attempts attempts, another connection is still using it"
        )
    }

    fun ping() = getBaseNoteDao().query(SimpleSQLiteQuery("SELECT 1")) == 1

    private var biometricLockObserver: Observer<BiometricLock>? = null
    private var dataInPublicFolderObserver: Observer<Boolean>? = null

    companion object {

        const val DATABASE_NAME = "NotallyDatabase"

        @Volatile private var instance: NotNullLiveData<NotallyDatabase>? = null

        fun getCurrentDatabaseFile(context: ContextWrapper): File {
            return if (NotallyXPreferences.getInstance(context).dataInPublicFolder.value) {
                getExternalDatabaseFile(context)
            } else {
                getInternalDatabaseFile(context)
            }
        }

        fun getExternalDatabaseFile(context: ContextWrapper): File {
            return File(context.getExternalMediaDirectory(), DATABASE_NAME)
        }

        fun getExternalDatabaseFiles(context: ContextWrapper): List<File> {
            return listOf(
                File(context.getExternalMediaDirectory(), DATABASE_NAME),
                File(context.getExternalMediaDirectory(), "$DATABASE_NAME-shm"),
                File(context.getExternalMediaDirectory(), "$DATABASE_NAME-wal"),
            )
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
        }

        @VisibleForTesting
        internal fun createBuilder(
            context: Context,
            databaseName: String,
        ): RoomDatabase.Builder<NotallyDatabase> {
            return Room.databaseBuilder(context, NotallyDatabase::class.java, databaseName)
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

        fun getDatabase(
            context: ContextWrapper,
            observePreferences: Boolean = true,
        ): NotNullLiveData<NotallyDatabase> {
            return instance
                ?: synchronized(this) {
                    // Re-check inside the lock, otherwise concurrent callers each build their own
                    // instance and all but the last one are leaked open, writing to the same file
                    instance
                        ?: run {
                            val preferences = NotallyXPreferences.getInstance(context)
                            NotNullLiveData(
                                    createInstance(context, preferences, observePreferences)
                                )
                                .also { instance = it }
                        }
                }
        }

        fun clearInstance(context: Context) {
            val preferences = NotallyXPreferences.getInstance(context)
            instance?.value?.biometricLockObserver?.let {
                preferences.biometricLock.removeObserver(it)
            }
            instance?.value?.dataInPublicFolderObserver?.let {
                preferences.dataInPublicFolder.removeObserver(it)
            }
            instance?.value?.close()
            instance = null
        }

        /**
         * Closes the currently held instance, so it does not keep a connection (and a
         * write-ahead-log writer) on the database file after it has been replaced by another
         * instance.
         */
        private fun closeInstance() {
            instance?.value?.let { previous ->
                try {
                    if (previous.isOpen) {
                        previous.close()
                    }
                } catch (_: Exception) {
                    // Nothing can be done about a failing close, never prevent the replacement
                }
            }
        }

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

        fun getFreshDatabase(context: ContextWrapper, dataInPublic: Boolean): NotallyDatabase {
            return if (isTestRunner()) {
                getTestDatabase(context)
            } else {
                createInstance(
                    context,
                    NotallyXPreferences.getInstance(context),
                    false,
                    dataInPublic = dataInPublic,
                )
            }
        }

        private fun createInstance(
            context: ContextWrapper,
            preferences: NotallyXPreferences,
            observePreferences: Boolean,
            dataInPublic: Boolean = preferences.dataInPublicFolder.value,
        ): NotallyDatabase {
            val instanceBuilder =
                createBuilder(context, getCurrentDatabaseName(context, dataInPublic))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                System.loadLibrary("sqlcipher")
                var cipherFactory: SupportOpenHelperFactory? = null
                if (preferences.isLockEnabled) {
                    if (
                        SQLCipherUtils.getDatabaseState(getCurrentDatabaseFile(context)) ==
                            SQLCipherUtils.State.ENCRYPTED
                    ) {
                        cipherFactory = initializeDecryption(preferences)
                    } else {
                        preferences.biometricLock.save(BiometricLock.DISABLED)
                    }
                } else {
                    if (
                        SQLCipherUtils.getDatabaseState(getCurrentDatabaseFile(context)) ==
                            SQLCipherUtils.State.ENCRYPTED
                    ) {
                        preferences.biometricLock.save(BiometricLock.ENABLED)
                        cipherFactory = initializeDecryption(preferences)
                    }
                }
                // Wrap the actual open helper (SQLCipher when the database is encrypted, the
                // framework one otherwise) in the non-destructive factory, so a corruption is
                // backed up instead of deleting the database - without discarding the cipher
                // factory the encrypted database needs to be opened at all.
                val openHelperFactory =
                    if (cipherFactory != null) {
                        NonDestructiveOpenHelperFactory(context, cipherFactory)
                    } else {
                        NonDestructiveOpenHelperFactory(context)
                    }
                val instance = instanceBuilder.openHelperFactory(openHelperFactory).build()
                if (observePreferences) {
                    instance.biometricLockObserver = Observer {
                        NotallyDatabase.instance?.value?.biometricLockObserver?.let {
                            preferences.biometricLock.removeObserver(it)
                        }
                        closeInstance()
                        val newInstance = createInstance(context, preferences, true)
                        NotallyDatabase.instance?.postValue(newInstance)
                        preferences.biometricLock.observeForeverSkipFirst(
                            newInstance.biometricLockObserver!!
                        )
                    }
                    preferences.biometricLock.observeForeverSkipFirst(
                        instance.biometricLockObserver!!
                    )

                    instance.dataInPublicFolderObserver = Observer {
                        NotallyDatabase.instance?.value?.dataInPublicFolderObserver?.let {
                            preferences.dataInPublicFolder.removeObserver(it)
                        }
                        closeInstance()
                        val newInstance = createInstance(context, preferences, true)
                        NotallyDatabase.instance?.postValue(newInstance)
                        preferences.dataInPublicFolder.observeForeverSkipFirst(
                            newInstance.dataInPublicFolderObserver!!
                        )
                    }
                    preferences.dataInPublicFolder.observeForeverSkipFirst(
                        instance.dataInPublicFolderObserver!!
                    )
                }
                return instance
            }
            return instanceBuilder.build()
        }

        @RequiresApi(Build.VERSION_CODES.M)
        private fun initializeDecryption(
            preferences: NotallyXPreferences
        ): SupportOpenHelperFactory {
            val initializationVector = preferences.iv.value!!
            val cipher = getInitializedCipherForDecryption(iv = initializationVector)
            val encryptedPassphrase = preferences.databaseEncryptionKey.value
            val passphrase = cipher.doFinal(encryptedPassphrase)
            return SupportOpenHelperFactory(passphrase)
        }

        object Migration2 : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `color` TEXT NOT NULL DEFAULT 'DEFAULT'"
                )
            }
        }

        object Migration3 : Migration(2, 3) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `images` TEXT NOT NULL DEFAULT `[]`")
            }
        }

        object Migration4 : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `audios` TEXT NOT NULL DEFAULT `[]`")
            }
        }

        object Migration5 : Migration(4, 5) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `BaseNote` ADD COLUMN `files` TEXT NOT NULL DEFAULT `[]`")
            }
        }

        object Migration6 : Migration(5, 6) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `modifiedTimestamp` INTEGER NOT NULL DEFAULT 'timestamp'"
                )
            }
        }

        object Migration7 : Migration(6, 7) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `reminders` TEXT NOT NULL DEFAULT `[]`"
                )
            }
        }

        object Migration8 : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val cursor = db.query("SELECT id, color FROM BaseNote")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    val colorString = cursor.getString(cursor.getColumnIndexOrThrow("color"))
                    val color = Color.valueOfOrDefault(colorString)
                    val hexColor = color.toColorString()
                    db.execSQL("UPDATE BaseNote SET color = ? WHERE id = ?", arrayOf(hexColor, id))
                }
                cursor.close()
            }
        }

        object Migration9 : Migration(8, 9) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `viewMode` TEXT NOT NULL DEFAULT '${NoteViewMode.EDIT.name}'"
                )
            }
        }

        object Migration10 : Migration(9, 10) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `BaseNote` ADD COLUMN `isPinnedToStatus` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        object Migration11 : Migration(10, 11) {

            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `Label` ADD COLUMN `order` INTEGER NOT NULL DEFAULT 0")
                val cursor = db.query("SELECT value FROM Label ORDER BY value DESC")
                var order = 0
                while (cursor.moveToNext()) {
                    val value = cursor.getString(0)
                    db.execSQL(
                        "UPDATE Label SET `order` = ? WHERE value = ?",
                        arrayOf(order, value),
                    )
                    order++
                }
                cursor.close()
            }
        }
    }
}

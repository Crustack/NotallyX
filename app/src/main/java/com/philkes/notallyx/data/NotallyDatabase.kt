package com.philkes.notallyx.data

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Color
import com.philkes.notallyx.data.model.Converters
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.toColorString
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.utils.getExternalMediaDirectory
import java.io.File

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
        return try {
            getBaseNoteDao().query(SimpleSQLiteQuery("pragma wal_checkpoint(FULL)")) == 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Like [checkpoint], but retries a few times with backoff and throws if the write-ahead-log
     * could not be written back. To be used before the database file is copied or replaced.
     */
    fun checkpointOrThrow(attempts: Int = 3, retryDelayMs: Long = 50) {
        repeat(attempts) { attempt ->
            if (checkpoint()) {
                return
            }
            if (attempt < attempts - 1 && retryDelayMs > 0) {
                try {
                    Thread.sleep(retryDelayMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
        throw IllegalStateException(
            "Could not checkpoint the database after $attempts attempts, another connection is still using it"
        )
    }

    fun ping() = getBaseNoteDao().query(SimpleSQLiteQuery("SELECT 1")) == 1

    companion object {

        const val DATABASE_NAME = "NotallyDatabase"

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

        internal fun getCurrentDatabaseName(
            context: ContextWrapper,
            dataInPublicFolder: Boolean,
        ): String {
            return if (dataInPublicFolder) {
                getExternalDatabaseFile(context).absolutePath
            } else {
                DATABASE_NAME
            }
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
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(c.getColumnIndexOrThrow("id"))
                        val colorString = c.getString(c.getColumnIndexOrThrow("color"))
                        val color = Color.valueOfOrDefault(colorString)
                        val hexColor = color.toColorString()
                        db.execSQL(
                            "UPDATE BaseNote SET color = ? WHERE id = ?",
                            arrayOf(hexColor, id),
                        )
                    }
                }
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
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val value = c.getString(0)
                        db.execSQL(
                            "UPDATE Label SET `order` = ? WHERE value = ?",
                            arrayOf(order, value),
                        )
                        order++
                    }
                }
            }
        }
    }
}

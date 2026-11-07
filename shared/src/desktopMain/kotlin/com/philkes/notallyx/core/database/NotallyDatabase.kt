package com.philkes.notallyx.core.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomRawQuery
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.philkes.notallyx.core.platform.PathProvider
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

@ColumnTypeConverters(Converters::class)
@Database(entities = [BaseNote::class, Label::class], version = NotallyDatabase.DATABASE_VERSION)
actual abstract class NotallyDatabase : RoomDatabase() {

    actual abstract fun getLabelDao(): LabelDao

    actual abstract fun getCommonDao(): CommonDao

    actual abstract fun getBaseNoteDao(): BaseNoteDao

    actual suspend fun checkpoint(): Boolean {
        return try {
            val result = getBaseNoteDao().query(RoomRawQuery("pragma wal_checkpoint(TRUNCATE)"))
            result == 0
        } catch (_: Exception) {
            false
        }
    }

    actual suspend fun ping() = getBaseNoteDao().query(RoomRawQuery("SELECT 1")) == 1

    actual companion object {

        actual const val DATABASE_NAME = "NotallyDatabase"
        actual const val DATABASE_VERSION = 11
        actual const val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false

        actual val ALL_MIGRATIONS: Array<Migration>
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

        actual val instance: StateFlow<NotallyDatabase?>
            field = MutableStateFlow(null)
        @Volatile private var replacementInProgress = false

        actual fun getCurrentDatabaseFile(
            pathProvider: PathProvider,
            dataInPublicFolder: Boolean,
        ): File {
            return pathProvider.getDatabaseFile(DATABASE_NAME, dataInPublicFolder)
        }

        actual fun getCurrentDatabaseFiles(
            pathProvider: PathProvider,
            dataInPublicFolder: Boolean,
        ): List<File> {
            return pathProvider.getDatabaseFiles(DATABASE_NAME, dataInPublicFolder)
        }

        actual fun getExternalDatabaseFile(pathProvider: PathProvider): File {
            return pathProvider.getDatabaseFile(DATABASE_NAME, dataInPublicFolder = true)
        }

        actual fun getExternalDatabaseFiles(pathProvider: PathProvider): List<File> {
            return pathProvider.getDatabaseFiles(DATABASE_NAME, dataInPublicFolder = true)
        }

        actual fun getInternalDatabaseFile(pathProvider: PathProvider): File {
            return pathProvider.getDatabaseFile(DATABASE_NAME, dataInPublicFolder = false)
        }

        actual fun getInternalDatabaseFiles(pathProvider: PathProvider): List<File> {
            return pathProvider.getDatabaseFiles(DATABASE_NAME, dataInPublicFolder = false)
        }

        actual fun builder(
            driverFactory: DatabaseDriverFactory,
            dataInPublic: Boolean,
            inMemory: Boolean,
        ): RoomDatabase.Builder<NotallyDatabase> {
            return driverFactory
                .createBuilder<NotallyDatabase>(DATABASE_NAME, dataInPublic, inMemory)
                .setDriver(driverFactory.createDriver(DATABASE_NAME, dataInPublic))
                .addMigrations(*ALL_MIGRATIONS)
        }

        actual fun createDatabase(
            driverFactory: DatabaseDriverFactory,
            dataInPublic: Boolean,
            inMemory: Boolean,
        ): NotallyDatabase {
            return builder(driverFactory, dataInPublic, inMemory).build()
        }

        actual fun clearInstance() {
            synchronized(this) {
                instance.value?.let { database ->
                    instance.value = null
                    database.close()
                }
            }
        }

        actual fun startReplacement() {
            replacementInProgress = true
        }

        actual fun endReplacement() {
            replacementInProgress = false
        }

        actual fun isBeingReplaced() = replacementInProgress

        actual fun postInstance(notallyDatabase: NotallyDatabase) {
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

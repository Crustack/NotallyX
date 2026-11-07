package com.philkes.notallyx.core.database

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.CommonDao
import com.philkes.notallyx.data.dao.LabelDao
import java.io.File
import kotlinx.coroutines.flow.StateFlow

expect abstract class NotallyDatabase() : RoomDatabase {

    abstract fun getLabelDao(): LabelDao

    abstract fun getCommonDao(): CommonDao

    abstract fun getBaseNoteDao(): BaseNoteDao

    suspend fun checkpoint(): Boolean

    suspend fun ping(): Boolean

    companion object {

        val DATABASE_NAME: String
        val DATABASE_VERSION: Int
        val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB: Boolean

        val ALL_MIGRATIONS: Array<Migration>

        val instance: StateFlow<NotallyDatabase?>

        fun getCurrentDatabaseFile(
            pathProvider: PathProvider,
            dataInPublicFolder: Boolean = false,
        ): File

        fun getCurrentDatabaseFiles(
            pathProvider: PathProvider,
            dataInPublicFolder: Boolean = false,
        ): List<File>

        fun getExternalDatabaseFile(pathProvider: PathProvider): File

        fun getExternalDatabaseFiles(pathProvider: PathProvider): List<File>

        fun getInternalDatabaseFile(pathProvider: PathProvider): File

        fun getInternalDatabaseFiles(pathProvider: PathProvider): List<File>

        fun builder(
            driverFactory: DatabaseDriverFactory,
            dataInPublic: Boolean = false,
            inMemory: Boolean = false,
        ): RoomDatabase.Builder<NotallyDatabase>

        fun createDatabase(
            driverFactory: DatabaseDriverFactory,
            dataInPublic: Boolean = false,
            inMemory: Boolean = false,
        ): NotallyDatabase

        fun clearInstance()

        fun startReplacement()

        fun endReplacement()

        fun isBeingReplaced(): Boolean

        fun postInstance(notallyDatabase: NotallyDatabase)
    }
}

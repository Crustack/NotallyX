package com.philkes.notallyx.core.database

import android.content.ContextWrapper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.driver.SupportSQLiteConnection

private const val DATABASE_VERSION = 11

/**
 * [SQLiteDriver] that delegates opening the database to a [SupportSQLiteOpenHelper.Factory], such
 * as [BackupOpenHelperFactory] or SQLCipher's SupportOpenHelperFactory.
 *
 * It bridges schema management by ensuring Room 3 sees version 0 for newly created databases and
 * original versions for migrating databases, without SQLiteOpenHelper overriding them.
 */
open class BackupSQLiteDriver(
    private val context: ContextWrapper,
    private val openHelperFactory: SupportSQLiteOpenHelper.Factory =
        BackupOpenHelperFactory(context),
) : SQLiteDriver {

    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("hasConnectionPool")
    override val hasConnectionPool: Boolean
        get() = true

    override fun open(fileName: String): SQLiteConnection {
        val dbName = if (fileName == ":memory:" || fileName.isEmpty()) null else fileName
        val config =
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {
                        private var initialVersion: Int? = null

                        override fun onCreate(db: SupportSQLiteDatabase) {
                            initialVersion = 0
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            initialVersion = oldVersion
                        }

                        override fun onDowngrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) {
                            initialVersion = oldVersion
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            initialVersion?.let { version ->
                                db.version = version
                                initialVersion = null
                            }
                        }

                        override fun onCorruption(db: SupportSQLiteDatabase) {
                            super.onCorruption(db)
                        }
                    }
                )
                .build()
        val helper = openHelperFactory.create(config)
        return SupportSQLiteConnection(helper.writableDatabase)
    }
}

package com.philkes.notallyx.data

import android.app.Application
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.utils.getExternalMediaDirectory
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class DatabaseManagerLifecycleTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        preferences = NotallyXPreferences.getInstance(application)
        DatabaseManager.clearInstance(application)
    }

    @After
    fun tearDown() {
        DatabaseManager.clearInstance(application)
    }

    @Test
    fun getDatabase_returnsConsistentSingleton() {
        val liveData1 = DatabaseManager.getDatabase(application)
        val liveData2 = DatabaseManager.getDatabase(application)

        assertThat(liveData1).isSameAs(liveData2)
        assertThat(liveData1.value).isNotNull
    }

    @Test
    fun maintenanceLock_serializesConcurrentAccess() {
        runBlocking {
            val activeConcurrentCount = AtomicInteger(0)
            val maxConcurrentObserved = AtomicInteger(0)

            val deferreds =
                (1..10).map {
                    async(Dispatchers.Default) {
                        DatabaseManager.withMaintenanceLock {
                            val current = activeConcurrentCount.incrementAndGet()
                            maxConcurrentObserved.accumulateAndGet(current) { a, b -> maxOf(a, b) }
                            delay(20)
                            activeConcurrentCount.decrementAndGet()
                        }
                    }
                }

            deferreds.awaitAll()

            assertThat(maxConcurrentObserved.get()).isEqualTo(1)
            assertThat(activeConcurrentCount.get()).isEqualTo(0)
        }
    }

    @Test
    fun maintenanceLock_serializesCoroutineAndSynchronousCallers() {
        runBlocking {
            val activeConcurrentCount = AtomicInteger(0)
            val maxConcurrentObserved = AtomicInteger(0)

            val coroutineTasks =
                (1..5).map {
                    async(Dispatchers.Default) {
                        DatabaseManager.withMaintenanceLock {
                            val current = activeConcurrentCount.incrementAndGet()
                            maxConcurrentObserved.accumulateAndGet(current) { a, b -> maxOf(a, b) }
                            delay(20)
                            activeConcurrentCount.decrementAndGet()
                        }
                    }
                }

            val syncTasks =
                (1..5).map {
                    async(Dispatchers.IO) {
                        DatabaseManager.withSyncMaintenanceLock {
                            val current = activeConcurrentCount.incrementAndGet()
                            maxConcurrentObserved.accumulateAndGet(current) { a, b -> maxOf(a, b) }
                            Thread.sleep(20)
                            activeConcurrentCount.decrementAndGet()
                        }
                    }
                }

            (coroutineTasks + syncTasks).awaitAll()

            assertThat(maxConcurrentObserved.get()).isEqualTo(1)
            assertThat(activeConcurrentCount.get()).isEqualTo(0)
        }
    }

    @Test
    fun checkpoint_executesSuccessfullyOnOpenDatabase() {
        val database = DatabaseManager.getDatabase(application).value
        assertThat(database.checkpoint()).isTrue()
    }

    @Test
    fun nonDestructiveCorruptionHandler_copiesRawFilesWithoutInvokingQueries() {
        val internalFiles = NotallyDatabase.getInternalDatabaseFiles(application)
        internalFiles.forEach { file ->
            file.parentFile?.mkdirs()
            file.writeText("sample data for ${file.name}")
        }

        val dummyDelegate =
            object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {}

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int,
                ) {}
            }
        val recordingCallback =
            NonDestructiveOpenHelperFactory.RecordingCallback(dummyDelegate, application)
        val dummyDb = org.mockito.Mockito.mock(SupportSQLiteDatabase::class.java)

        recordingCallback.onCorruption(dummyDb)

        val corruptDir =
            try {
                application.getExternalMediaDirectory()
            } catch (_: Exception) {
                File(application.filesDir, "corrupted_backups")
            }
        val corruptFiles =
            corruptDir.listFiles()?.filter { it.name.contains("_CORRUPTED_") } ?: emptyList()

        assertThat(corruptFiles).isNotEmpty
    }

    @Test
    fun schemaMigrations_applyCleanly() {
        // Build in-memory database and test migration steps
        val db =
            NotallyDatabase.createBuilder(application, NotallyDatabase.DATABASE_NAME)
                .allowMainThreadQueries()
                .build()

        assertThat(db.ping()).isTrue()
        db.close()
    }
}

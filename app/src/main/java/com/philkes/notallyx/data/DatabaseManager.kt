package com.philkes.notallyx.data

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.philkes.notallyx.NotallyXApplication.Companion.isTestRunner
import com.philkes.notallyx.presentation.view.misc.NotNullLiveData
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.observeForeverSkipFirst
import com.philkes.notallyx.utils.security.SQLCipherUtils
import com.philkes.notallyx.utils.security.getInitializedCipherForDecryption
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

object DatabaseManager {

    private val maintenanceMutex = Mutex()

    @Volatile private var instance: NotNullLiveData<NotallyDatabase>? = null

    private var biometricLockObserver: Observer<BiometricLock>? = null
    private var dataInPublicFolderObserver: Observer<Boolean>? = null
    private var observingPreferences: NotallyXPreferences? = null

    private var testInstance: NotallyDatabase? = null

    /**
     * Executes the given suspend block while holding the database maintenance mutex. Prevents
     * concurrent lifecycle transitions (location switch, encryption toggle, restore/import) from
     * clashing.
     */
    suspend fun <T> withMaintenanceLock(block: suspend () -> T): T {
        return maintenanceMutex.withLock { block() }
    }

    /** Synchronous variant of maintenance lock for non-coroutine contexts. */
    fun <T> withSyncMaintenanceLock(block: () -> T): T {
        return runBlocking { maintenanceMutex.withLock { block() } }
    }

    /**
     * Returns the singleton LiveData containing the active NotallyDatabase instance. Preference
     * observers are always registered on the singleton to guarantee that preference changes (such
     * as biometric encryption or storage folder switches) recreate and post the updated instance.
     */
    fun getDatabase(context: ContextWrapper): NotNullLiveData<NotallyDatabase> {
        instance?.let { return it }
        return withSyncMaintenanceLock {
            instance
                ?: synchronized(this) {
                    instance
                        ?: run {
                            val preferences = NotallyXPreferences.getInstance(context)
                            val initialDb =
                                if (isTestRunner()) {
                                    getTestDatabase(context)
                                } else {
                                    createDatabaseInstance(context, preferences)
                                }
                            val liveData = NotNullLiveData(initialDb).also { instance = it }
                            if (!isTestRunner()) {
                                setupPreferenceObservers(context, preferences)
                            }
                            liveData
                        }
                }
        }
    }

    /**
     * Safely closes the currently active database instance, performing a WAL checkpoint first if
     * the database is open.
     */
    fun closeInstance() {
        withSyncMaintenanceLock { synchronized(this) { closeInstanceUnlocked() } }
    }

    private fun closeInstanceUnlocked() {
        instance?.value?.let { previous ->
            try {
                if (previous.isOpen) {
                    try {
                        previous.checkpoint()
                    } catch (_: Exception) {}
                    previous.close()
                }
            } catch (_: Exception) {
                // Nothing can be done about a failing close, never prevent subsequent
                // operations
            }
        }
    }

    /**
     * Clears and closes the active database instance and detaches preference observers. Primarily
     * used in testing and reset scenarios.
     */
    fun clearInstance(@Suppress("UNUSED_PARAMETER") context: Context? = null) {
        withSyncMaintenanceLock {
            synchronized(this) {
                removePreferenceObservers()
                closeInstanceUnlocked()
                instance = null
                testInstance = null
            }
        }
    }

    private fun setupPreferenceObservers(
        context: ContextWrapper,
        preferences: NotallyXPreferences,
    ) {
        removePreferenceObservers()
        observingPreferences = preferences

        val bioObserver = Observer<BiometricLock> { recreateInstance(context, preferences) }
        biometricLockObserver = bioObserver
        preferences.biometricLock.observeForeverSkipFirst(bioObserver)

        val folderObserver = Observer<Boolean> { recreateInstance(context, preferences) }
        dataInPublicFolderObserver = folderObserver
        preferences.dataInPublicFolder.observeForeverSkipFirst(folderObserver)
    }

    private fun removePreferenceObservers() {
        observingPreferences?.let { prefs ->
            biometricLockObserver?.let { prefs.biometricLock.removeObserver(it) }
            dataInPublicFolderObserver?.let { prefs.dataInPublicFolder.removeObserver(it) }
        }
        biometricLockObserver = null
        dataInPublicFolderObserver = null
        observingPreferences = null
    }

    /** Recreates the database instance following preference or storage changes. */
    fun recreateInstance(
        context: ContextWrapper,
        preferences: NotallyXPreferences = NotallyXPreferences.getInstance(context),
        dataInPublic: Boolean = preferences.dataInPublicFolder.value,
    ): NotallyDatabase {
        return withSyncMaintenanceLock {
            synchronized(this) {
                removePreferenceObservers()
                closeInstanceUnlocked()
                val newInstance = createDatabaseInstance(context, preferences, dataInPublic)
                val liveData = instance
                if (liveData != null) {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        liveData.value = newInstance
                    } else {
                        liveData.postValue(newInstance)
                    }
                }
                if (!isTestRunner()) {
                    setupPreferenceObservers(context, preferences)
                }
                newInstance
            }
        }
    }

    fun createStandaloneInstance(context: ContextWrapper, dataInPublic: Boolean): NotallyDatabase {
        return if (isTestRunner()) {
            getTestDatabase(context)
        } else {
            createDatabaseInstance(
                context,
                NotallyXPreferences.getInstance(context),
                dataInPublic = dataInPublic,
            )
        }
    }

    private fun getTestDatabase(context: ContextWrapper): NotallyDatabase {
        return testInstance
            ?: synchronized(this) {
                testInstance
                    ?: run {
                        Room.inMemoryDatabaseBuilder(context, NotallyDatabase::class.java)
                            .allowMainThreadQueries()
                            .build()
                            .also { testInstance = it }
                    }
            }
    }

    internal fun createDatabaseInstance(
        context: ContextWrapper,
        preferences: NotallyXPreferences,
        dataInPublic: Boolean = preferences.dataInPublicFolder.value,
    ): NotallyDatabase {
        val databaseName = NotallyDatabase.getCurrentDatabaseName(context, dataInPublic)
        val instanceBuilder = NotallyDatabase.createBuilder(context, databaseName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isTestRunner()) {
            try {
                System.loadLibrary("sqlcipher")
            } catch (_: UnsatisfiedLinkError) {}
            var cipherFactory: SupportOpenHelperFactory? = null
            val dbFile =
                if (dataInPublic) {
                    NotallyDatabase.getExternalDatabaseFile(context)
                } else {
                    NotallyDatabase.getInternalDatabaseFile(context)
                }

            if (preferences.isLockEnabled) {
                if (SQLCipherUtils.getDatabaseState(dbFile) == SQLCipherUtils.State.ENCRYPTED) {
                    cipherFactory = initializeDecryption(preferences)
                } else {
                    preferences.biometricLock.save(BiometricLock.DISABLED)
                }
            } else {
                if (SQLCipherUtils.getDatabaseState(dbFile) == SQLCipherUtils.State.ENCRYPTED) {
                    preferences.biometricLock.save(BiometricLock.ENABLED)
                    cipherFactory = initializeDecryption(preferences)
                }
            }

            val openHelperFactory: SupportSQLiteOpenHelper.Factory =
                if (cipherFactory != null) {
                    NonDestructiveOpenHelperFactory(context, cipherFactory)
                } else {
                    NonDestructiveOpenHelperFactory(context)
                }
            return instanceBuilder.openHelperFactory(openHelperFactory).build()
        }
        return instanceBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initializeDecryption(preferences: NotallyXPreferences): SupportOpenHelperFactory {
        val initializationVector = preferences.iv.value!!
        val cipher = getInitializedCipherForDecryption(iv = initializationVector)
        val encryptedPassphrase = preferences.databaseEncryptionKey.value
        val passphrase = cipher.doFinal(encryptedPassphrase)
        return SupportOpenHelperFactory(passphrase)
    }
}

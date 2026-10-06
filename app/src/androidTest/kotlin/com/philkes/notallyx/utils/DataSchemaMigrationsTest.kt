package com.philkes.notallyx.utils

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.initFakeBiometric
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.waitUntil
import com.philkes.notallyx.test.waitUntilSucceeds
import com.philkes.notallyx.utils.security.encryptDatabase
import com.philkes.notallyx.utils.security.isEncryptedDatabase
import com.philkes.notallyx.utils.security.isUnencryptedDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class DataSchemaMigrationsTest : UiTestBase() {

    @Test
    fun migration3_whenBiometricLockDisabled_doesNotShowWarning() {
        preferences.setDataSchemaId(2)
        preferences.biometricLock.save(BiometricLock.DISABLED)
        preferences.biometricLockEncryptsDb.save(false)

        val app = context.applicationContext as Application
        val result = runBlocking { app.runMigrations() }

        assertTrue(result.didWork)
        assertFalse(result.showBiometricWarning)
        assertEquals(3, preferences.dataSchemaId.value)
        assertEquals(BiometricLock.DISABLED, preferences.biometricLock.value)
        assertFalse(preferences.biometricLockEncryptsDb.value)
    }

    @Test
    fun migration3_whenBiometricLockEnabledButDatabaseUnencrypted_doesNotShowWarning() {
        preferences.setDataSchemaId(2)
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(false)

        val dbFile = NotallyDatabase.getCurrentDatabaseFile(context)
        assertTrue(dbFile.isUnencryptedDatabase(context))

        val app = context.applicationContext as Application
        val result = runBlocking { app.runMigrations() }

        assertTrue(result.didWork)
        assertFalse(result.showBiometricWarning)
        assertEquals(3, preferences.dataSchemaId.value)
        assertEquals(BiometricLock.ENABLED, preferences.biometricLock.value)
        assertFalse(preferences.biometricLockEncryptsDb.value)
    }

    @Test
    fun migration3_withEncryptedDatabaseAndBiometricLock_showsWarningAndDecryptsWhenUserChoosesToDecrypt() {
        val fakeBiometric = initFakeBiometric()

        // 1. Seed note in database and configure schema ID 2 with encrypted database
        runBlocking {
            withContext(Dispatchers.Main) {
                database
                    .getBaseNoteDao()
                    .insert(
                        createBaseNote(title = "Migration Test Note", body = "Migration Test Body")
                    )
            }
            withContext(Dispatchers.IO) { database.checkpoint() }
            NotallyDatabase.clearInstance()

            val dbFile = NotallyDatabase.getCurrentDatabaseFile(context)
            val cipher = fakeBiometric.getEncryptionCipher()
            val passphrase = preferences.databaseEncryptionKey.init(cipher)
            preferences.iv.save(cipher.iv)
            fakeBiometric.iv = cipher.iv
            preferences.fallbackDatabaseEncryptionKey.save(passphrase)

            encryptDatabase(context, dbFile, passphrase)
            assertTrue(dbFile.isEncryptedDatabase(context))

            preferences.setDataSchemaId(2)
            preferences.biometricLock.save(BiometricLock.ENABLED)
            preferences.biometricLockEncryptsDb.save(false)
        }

        // 2. Launch MainActivity to trigger DataSchemaMigrations (Migration 3)
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 3. Verify experimental warning dialog appears and click "Decrypt"
        R.string.decrypt.byText().perform(click())

        // 4. Assert migration complete, database decrypted, biometric lock stays ENABLED, and DB
        // encryption preference is FALSE
        waitUntil(15_000L) {
            preferences.dataSchemaId.value == 3 &&
                preferences.biometricLock.value == BiometricLock.ENABLED &&
                !preferences.biometricLockEncryptsDb.value &&
                NotallyDatabase.getCurrentDatabaseFile(context).isUnencryptedDatabase(context)
        }

        // Navigate to Notes fragment to verify notes list
        navigateTo(R.id.Notes)

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Migration Test Note")))
        }

        scenario.close()
    }

    @Test
    fun migration3_withEncryptedDatabaseAndBiometricLock_showsWarningAndKeepsEncryptedWhenUserDeclinesToDecrypt() {
        val fakeBiometric = initFakeBiometric()

        // 1. Seed note in database and configure schema ID 2 with encrypted database
        runBlocking {
            withContext(Dispatchers.Main) {
                database
                    .getBaseNoteDao()
                    .insert(
                        createBaseNote(title = "Migration Test Note", body = "Migration Test Body")
                    )
            }
            withContext(Dispatchers.IO) { database.checkpoint() }
            NotallyDatabase.clearInstance()

            val dbFile = NotallyDatabase.getCurrentDatabaseFile(context)
            val cipher = fakeBiometric.getEncryptionCipher()
            val passphrase = preferences.databaseEncryptionKey.init(cipher)
            preferences.iv.save(cipher.iv)
            fakeBiometric.iv = cipher.iv
            preferences.fallbackDatabaseEncryptionKey.save(passphrase)

            encryptDatabase(context, dbFile, passphrase)
            assertTrue(dbFile.isEncryptedDatabase(context))

            preferences.setDataSchemaId(2)
            preferences.biometricLock.save(BiometricLock.ENABLED)
            preferences.biometricLockEncryptsDb.save(false)
        }

        // 2. Launch MainActivity to trigger DataSchemaMigrations (Migration 3)
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 3. Verify experimental warning dialog appears and click "OK" (decline decrypt)
        android.R.string.ok.byText().perform(click())

        // 4. Assert migration complete, biometric lock stays ENABLED, DB encryption preference is
        // TRUE, and DB remains ENCRYPTED
        waitUntil(15_000L) {
            preferences.dataSchemaId.value == 3 &&
                preferences.biometricLock.value == BiometricLock.ENABLED &&
                preferences.biometricLockEncryptsDb.value &&
                NotallyDatabase.getCurrentDatabaseFile(context).isEncryptedDatabase(context)
        }

        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Migration Test Note")))
        }

        scenario.close()
    }
}

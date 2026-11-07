package com.philkes.notallyx.utils

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.database.runMigrations
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.security.encryptDatabase
import com.philkes.notallyx.core.security.isEncryptedDatabase
import com.philkes.notallyx.core.security.isUnencryptedDatabase
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.initFakeBiometric
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.waitUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class DataSchemaMigrationsTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

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
        val decryptText = context.getString(R.string.decrypt)
        composeTestRule.waitUntil(15_000L) {
            composeTestRule.onAllNodes(hasText(decryptText)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(decryptText).performClick()

        // 4. Assert migration complete, database decrypted, biometric lock stays ENABLED, and DB
        // encryption preference is FALSE
        waitUntil(15_000L) {
            preferences.dataSchemaId.value == 3 &&
                preferences.biometricLock.value == BiometricLock.ENABLED &&
                !preferences.biometricLockEncryptsDb.value &&
                NotallyDatabase.getCurrentDatabaseFile(context).isUnencryptedDatabase(context)
        }

        // Navigate to Notes fragment to verify notes list
        composeTestRule.navigateTo(NotallyNavKey.Notes)

        composeTestRule.waitUntil(15_000L) {
            composeTestRule
                .onAllNodes(hasText("Migration Test Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Migration Test Note").assertIsDisplayed()

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
        val okText = context.getString(android.R.string.ok)
        composeTestRule.waitUntil(15_000L) {
            composeTestRule.onAllNodes(hasText(okText)).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(okText).performClick()

        // 4. Assert migration complete, biometric lock stays ENABLED, DB encryption preference is
        // TRUE, and DB remains ENCRYPTED
        waitUntil(15_000L) {
            preferences.dataSchemaId.value == 3 &&
                preferences.biometricLock.value == BiometricLock.ENABLED &&
                preferences.biometricLockEncryptsDb.value &&
                NotallyDatabase.getCurrentDatabaseFile(context).isEncryptedDatabase(context)
        }

        composeTestRule.waitUntil(15_000L) {
            composeTestRule
                .onAllNodes(hasText("Migration Test Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Migration Test Note").assertIsDisplayed()

        scenario.close()
    }
}

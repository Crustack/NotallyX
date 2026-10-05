package com.philkes.notallyx.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.philkes.notallyx.data.NotallyDatabase.Companion.setupEncryption
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NotallyDatabaseTest {

    @Test
    fun biometricLockEncryptsDb_defaultIsFalse() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val preferences = NotallyXPreferences.getInstance(application)
        assertFalse(preferences.biometricLockEncryptsDb.value)
    }

    @Test
    fun setupEncryption_whenBiometricLockEncryptsDbIsFalse_doesNotEnableEncryptionOrModifyPreferences() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val preferences = NotallyXPreferences.getInstance(application)
        preferences.biometricLockEncryptsDb.save(false)
        preferences.biometricLock.save(BiometricLock.ENABLED)

        val builder = NotallyDatabase.builder(application, false)
        builder.setupEncryption(application, preferences, BiometricLock.ENABLED)

        assertEquals(BiometricLock.ENABLED, preferences.biometricLock.value)
    }
}

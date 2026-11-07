package com.philkes.notallyx.activity

import android.content.Intent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.security.UnlockReceiver
import com.philkes.notallyx.core.ui.extensions.setEnabledSecureFlag
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.initFakeBiometric
import com.philkes.notallyx.test.waitUntil
import kotlinx.coroutines.runBlocking
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
class LockedActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun unencryptedLockState_doesNotCrashWithNullIvOnResume() {
        initFakeBiometric()
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(value = false)
        preferences.iv.save(null)

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = true

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(takeNoteLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun encryptedLockState_authenticatesAndUnlocksWithValidIv() {
        initFakeBiometric()
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(value = true)

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = true

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(takeNoteLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).assertIsDisplayed()
        assertFalse(application.locked.value)

        scenario.close()
    }

    @Test
    fun authenticationFailure_finishesActivity() {
        val fakeBiometric = initFakeBiometric()
        fakeBiometric.iv = null
        preferences.biometricLock.save(BiometricLock.ENABLED)

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = true

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        waitUntil { scenario.state == Lifecycle.State.DESTROYED }
    }

    @Test
    fun lockDeviceScreenShowsLockOverlay() {
        val fakeBiometric = initFakeBiometric()
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(value = false)

        val noteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Confidential Note", body = "Secret text content"))
        }

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = false

        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SELECTED_BASE_NOTE, noteId)
            }
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Confidential Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Confidential Note").assertIsDisplayed()
        scenario.onActivity { activity ->
            assertEquals(View.VISIBLE, activity.binding.root.visibility)
        }

        // Simulate device screen turning off / screen lock via PhoneLockReceiver
        fakeBiometric.deferAuthentication = true
        val unlockReceiver = UnlockReceiver(application)
        unlockReceiver.onReceive(context, Intent(Intent.ACTION_SCREEN_OFF))
        assertTrue(application.locked.value)

        // Activity moves to paused/stopped when screen turns off
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.onActivity { activity ->
            assertEquals(View.INVISIBLE, activity.binding.root.visibility)
        }

        // Screen turned back on / activity returns to foreground
        scenario.moveToState(Lifecycle.State.RESUMED)
        waitUntil(5_000) { fakeBiometric.hasPendingAuthentication }

        // While authentication is pending, root view remains hidden (lock overlay protection)
        scenario.onActivity { activity ->
            assertEquals(View.INVISIBLE, activity.binding.root.visibility)
        }
        assertTrue(application.locked.value)

        // Successful authentication unlocks and restores note editor visibility
        fakeBiometric.completeAuthentication()
        waitUntil(5_000) { !application.locked.value }

        scenario.onActivity { activity ->
            assertEquals(View.VISIBLE, activity.binding.root.visibility)
        }
        composeTestRule.onNodeWithText("Confidential Note").assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun screenLockDuringNoteEditing_whenAuthenticationFails_finishesActivity() {
        val fakeBiometric = initFakeBiometric()
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(value = false)

        val noteId = runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Top Secret Note", body = "Sensitive information"))
        }

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = false

        val intent =
            Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_SELECTED_BASE_NOTE, noteId)
            }
        val scenario = ActivityScenario.launch<MainActivity>(intent)

        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasText("Top Secret Note"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithText("Top Secret Note").assertIsDisplayed()

        fakeBiometric.deferAuthentication = true
        val unlockReceiver = UnlockReceiver(application)
        unlockReceiver.onReceive(context, Intent(Intent.ACTION_SCREEN_OFF))
        assertTrue(application.locked.value)

        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        waitUntil(5_000) { fakeBiometric.hasPendingAuthentication }

        // Failed authentication immediately finishes the activity
        fakeBiometric.failAuthentication()
        waitUntil { scenario.state == Lifecycle.State.DESTROYED }
    }

    @Test
    fun backgroundTransition_withScreenLock_requiresAuthenticationToResume() {
        val fakeBiometric = initFakeBiometric()
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(value = false)

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = true

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(takeNoteLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).assertIsDisplayed()
        assertFalse(application.locked.value)

        // App transitions to background
        scenario.moveToState(Lifecycle.State.CREATED)

        // Device locks while in background
        fakeBiometric.deferAuthentication = true
        val unlockReceiver = UnlockReceiver(application)
        unlockReceiver.onReceive(context, Intent(Intent.ACTION_SCREEN_OFF))
        assertTrue(application.locked.value)

        // Return to foreground
        scenario.moveToState(Lifecycle.State.RESUMED)
        waitUntil(5_000) { fakeBiometric.hasPendingAuthentication }

        // Content is hidden while authentication is pending
        scenario.onActivity { activity ->
            assertEquals(View.INVISIBLE, activity.binding.root.visibility)
        }

        // Complete authentication
        fakeBiometric.completeAuthentication()
        waitUntil(5_000) { !application.locked.value }

        scenario.onActivity { activity ->
            assertEquals(View.VISIBLE, activity.binding.root.visibility)
        }
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun backgroundTransition_whenBiometricLockDisabled_doesNotLockOrHide() {
        preferences.biometricLock.save(BiometricLock.DISABLED)

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = false

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        composeTestRule.waitUntil(10_000) {
            composeTestRule
                .onAllNodes(hasContentDescription(takeNoteLabel))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).assertIsDisplayed()

        // Transition to background and back
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)

        assertFalse(application.locked.value)
        scenario.onActivity { activity ->
            assertEquals(View.VISIBLE, activity.binding.root.visibility)
        }
        composeTestRule.onNodeWithContentDescription(takeNoteLabel).assertIsDisplayed()

        scenario.close()
    }

    @Test
    fun phoneLockReceiver_onlyLocksOnScreenOffAction() {
        val application = context.applicationContext as NotallyXApplication
        val receiver = UnlockReceiver(application)

        application.locked.value = false
        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))
        assertFalse(application.locked.value)

        receiver.onReceive(context, Intent(Intent.ACTION_USER_PRESENT))
        assertFalse(application.locked.value)

        receiver.onReceive(context, Intent(Intent.ACTION_SCREEN_OFF))
        assertTrue(application.locked.value)
    }

    @Test
    fun secureFlagPreference_togglesWindowFlagSecure() {
        preferences.secureFlag.save(true)

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        scenario.onActivity { activity ->
            val flags = activity.window.attributes.flags
            assertTrue((flags and WindowManager.LayoutParams.FLAG_SECURE) != 0)
        }

        preferences.secureFlag.save(false)
        scenario.onActivity { activity ->
            activity.setEnabledSecureFlag(false)
            val flags = activity.window.attributes.flags
            assertEquals(0, flags and WindowManager.LayoutParams.FLAG_SECURE)
        }

        scenario.close()
    }
}

package com.philkes.notallyx.presentation.activity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.initFakeBiometric
import com.philkes.notallyx.test.waitUntil
import org.junit.Assert.assertFalse
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

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasTestTag("TakeNote")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("TakeNote").assertIsDisplayed()

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

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasTestTag("TakeNote")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("TakeNote").assertIsDisplayed()
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
}

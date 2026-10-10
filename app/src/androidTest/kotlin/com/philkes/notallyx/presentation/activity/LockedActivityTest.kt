package com.philkes.notallyx.presentation.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.initFakeBiometric
import com.philkes.notallyx.test.waitUntilSucceeds
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class LockedActivityTest : UiTestBase() {

    @Test
    fun unencryptedLockState_doesNotCrashWithNullIvOnResume() {
        initFakeBiometric()
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(false)
        preferences.iv.save(null)

        val application = context.applicationContext as NotallyXApplication
        application.locked.value = true

        val scenario = ActivityScenario.launch(MainActivity::class.java)

        waitUntilSucceeds { R.id.MainListView.byId().check(matches(isDisplayed())) }

        scenario.close()
    }
}

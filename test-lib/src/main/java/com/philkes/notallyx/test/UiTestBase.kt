package com.philkes.notallyx.test

import android.Manifest
import android.content.ContextWrapper
import android.os.Build
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.presentation.viewmodel.preference.PeriodicBackup
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

abstract class UiTestBase : KoinComponent {

    val context
        get() = ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext)

    val database: NotallyDatabase
        get() = NotallyDatabase.getDatabase(context).value!!

    val preferences: NotallyXPreferences
        get() = get()

    val toolbarBackButton: ViewInteraction
        get() =
            onDisplayView(
                childAtPosition(
                    allOf(
                        withId(R.id.Toolbar),
                        childAtPosition(withId(R.id.main_content_layout), 0),
                    ),
                    0,
                )
            )

    @Before
    fun setup() {
        val packageName = context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            uiAutomation.grantRuntimePermission(
                packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            // Allows your app to set exact alarms without forcing the user to Settings
            device.executeShellCommand("appops set $packageName SCHEDULE_EXACT_ALARM allow")
        }
        preferences.backupsFolder.save(EMPTY_PATH)
        preferences.periodicBackups.save(PeriodicBackup(0, 0))
        preferences.backupOnSave.save(false)
    }
}

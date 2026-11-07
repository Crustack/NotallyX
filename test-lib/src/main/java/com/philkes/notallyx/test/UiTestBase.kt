package com.philkes.notallyx.test

// import com.philkes.notallyx.R
import android.Manifest
import android.content.ContextWrapper
import android.os.Build
import androidx.test.espresso.ViewInteraction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.philkes.notallyx.core.common.error.*
import com.philkes.notallyx.core.common.extensions.*
import com.philkes.notallyx.core.common.result.*
import com.philkes.notallyx.core.database.*
import com.philkes.notallyx.core.preference.*
import com.philkes.notallyx.core.preference.NotallyXPreferences.Companion.EMPTY_PATH
import com.philkes.notallyx.core.security.*
import com.philkes.notallyx.core.ui.components.*
import com.philkes.notallyx.core.ui.dialog.*
import com.philkes.notallyx.core.ui.extensions.*
import com.philkes.notallyx.core.ui.views.*
import com.philkes.notallyx.data.model.extensions.*
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
        get() = R.string.back.byContentDescription()

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

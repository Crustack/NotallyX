package com.philkes.notallyx.viewmodel.main.fragment

import android.app.Application
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
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
import com.philkes.notallyx.data.ShadowContextImplMedia
import com.philkes.notallyx.data.model.extensions.*
import com.philkes.notallyx.feature.settings.SettingsViewModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowEnvironment
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], shadows = [ShadowContextImplMedia::class])
class SettingsViewModelTest {

    private lateinit var application: Application
    private lateinit var preferences: NotallyXPreferences
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        application.getDefaultSharedPreferences().edit().clear().commit()
        NotallyXPreferences.clearInstance()
        preferences =
            GlobalContext.getOrNull()?.get<NotallyXPreferences>()
                ?: NotallyXPreferences.getInstance(application)
        preferences.reset()
        viewModel = SettingsViewModel(application, preferences)
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED)
    }

    @After
    fun tearDown() {
        NotallyXPreferences.clearInstance()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 2000
        while (!condition() && System.currentTimeMillis() < end) {
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
            Thread.sleep(10)
        }
    }

    @Test
    fun cancelFolderSelection_restoresPreviousValidFolder() {
        val initialFolder = preferences.backupsFolder.value
        viewModel.showRefreshBackupsFolderAfterThemeChange = true
        viewModel.previousBackupsFolder = initialFolder

        // Simulate importing a new backup folder
        preferences.backupsFolder.save(
            "content://com.android.externalstorage.documents/tree/primary%3AImported"
        )

        viewModel.cancelFolderSelection(initialFolder)
        waitUntil { preferences.backupsFolder.value == initialFolder }

        assertThat(preferences.backupsFolder.value).isEqualTo(initialFolder)
        assertThat(viewModel.showRefreshBackupsFolderAfterThemeChange).isFalse()
        assertThat(viewModel.previousBackupsFolder).isNull()
    }

    @Test
    fun cancelFolderSelection_disablesBackupsWhenNoValidPreviousFolder() {
        viewModel.showRefreshBackupsFolderAfterThemeChange = true
        viewModel.previousBackupsFolder = "content://unauthorized/tree/folder"

        // Simulate importing a new backup folder
        preferences.backupsFolder.save(
            "content://com.android.externalstorage.documents/tree/primary%3AImported"
        )

        viewModel.cancelFolderSelection()
        waitUntil {
            preferences.backupsFolder.value == EMPTY_PATH &&
                preferences.periodicBackups.value.periodInDays == 0
        }

        assertThat(preferences.backupsFolder.value).isEqualTo(EMPTY_PATH)
        assertThat(preferences.periodicBackups.value.periodInDays).isEqualTo(0)
        assertThat(viewModel.showRefreshBackupsFolderAfterThemeChange).isFalse()
        assertThat(viewModel.previousBackupsFolder).isNull()
    }
}

package com.philkes.notallyx.misc.appstore.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.MainScreen
import com.philkes.notallyx.feature.reminder.FilterOptions
import com.philkes.notallyx.feature.reminder.RemindersScreen
import com.philkes.notallyx.misc.appstore.AppStorePreview
import com.philkes.notallyx.misc.appstore.AppStoreSampleData
import com.philkes.notallyx.misc.appstore.DeviceFrame
import com.philkes.notallyx.navigation.NotallyNavKey

/** 7. Set reminders for important notes */
@AppStorePreview
@PreviewTest
@Composable
fun `07_Reminders`() {
    NotallyXTheme {
        DeviceFrame(
            title = stringResource(R.string.store_headline_reminders),
            screenContent = {
                MainScreen(
                    title = stringResource(R.string.reminders),
                    selectedNavKey = NotallyNavKey.Reminders,
                    content = {
                        RemindersScreen(
                            notes = AppStoreSampleData.REMINDER_NOTES,
                            selectedNoteIds = emptySet(),
                            isGrid = false,
                            displayConfig = NoteCardDisplayConfig(),
                            onNoteClick = { _, _ -> },
                            onNoteLongClick = { _, _, _ -> },
                            onReminderClick = { _, _ -> },
                            initialFilterMode = FilterOptions.UPCOMING,
                        )
                    },
                )
            },
        )
    }
}

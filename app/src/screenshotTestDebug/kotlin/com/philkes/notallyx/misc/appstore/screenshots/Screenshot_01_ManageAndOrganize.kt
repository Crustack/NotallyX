package com.philkes.notallyx.misc.appstore.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.MainScreen
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.misc.appstore.AppStorePreview
import com.philkes.notallyx.misc.appstore.AppStoreSampleData
import com.philkes.notallyx.misc.appstore.DeviceFrame

/** 1. Manage and organize all your notes */
@AppStorePreview
@PreviewTest
@Composable
fun `01_ManageAndOrganize`() {
    NotallyXTheme {
        DeviceFrame(
            title = stringResource(R.string.store_headline_manage_and_organize),
            screenContent = {
                MainScreen(
                    title = stringResource(R.string.notes),
                    showFabs = true,
                    showSearchOption = true,
                    content = {
                        NotesGridScreen(
                            items = AppStoreSampleData.MAIN_NOTES,
                            displayConfig = NoteCardDisplayConfig(maxLines = 7),
                        )
                    },
                )
            },
        )
    }
}

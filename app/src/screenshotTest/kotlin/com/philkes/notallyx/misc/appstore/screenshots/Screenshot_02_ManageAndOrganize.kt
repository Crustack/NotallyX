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

/** 2. Alternatively also in a Grid */
@AppStorePreview
@PreviewTest
@Composable
fun `02_ManageAndOrganize`() {
    NotallyXTheme {
        DeviceFrame(
            title = stringResource(R.string.store_headline_alternatively_also_in_a_grid),
            screenContent = {
                MainScreen(
                    title = stringResource(R.string.notes),
                    showFabs = true,
                    showSearchOption = true,
                    content = {
                        NotesGridScreen(
                            items = AppStoreSampleData.MAIN_NOTES,
                            isGrid = true,
                            displayConfig = NoteCardDisplayConfig(maxLines = 7),
                        )
                    },
                )
            },
        )
    }
}

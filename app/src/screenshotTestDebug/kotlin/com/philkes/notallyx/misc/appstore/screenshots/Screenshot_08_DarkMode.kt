package com.philkes.notallyx.misc.appstore.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.main.MainScreen
import com.philkes.notallyx.feature.main.routes.NotesGridScreen
import com.philkes.notallyx.misc.appstore.AppStorePreview
import com.philkes.notallyx.misc.appstore.AppStoreSampleData
import com.philkes.notallyx.misc.appstore.DeviceFrame

/** 8. Battery-Saving Dark Mode */
@AppStorePreview
@PreviewTest
@Composable
fun `08_DarkMode`() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        DeviceFrame(
            title = stringResource(R.string.store_headline_dark_mode),
            statusBarColor = Color.Black,
            statusBarContentColor = Color.White,
            screenContent = {
                MainScreen(
                    title = stringResource(R.string.notes),
                    showFabs = true,
                    showSearchOption = true,
                    content = {
                        NotesGridScreen(
                            items = AppStoreSampleData.mainNotes(),
                            isGrid = true,
                        )
                    },
                )
            },
        )
    }
}

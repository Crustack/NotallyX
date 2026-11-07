package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.feature.settings.SettingsRoute
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun settingsScreen_rendersOptionsAndAllowsToggling() = runUiTest {
        setContent {
            NotallyXTheme {
                SettingsRoute(
                    database = database,
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.NAV_SETTINGS).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_LIST).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_NOTES_VIEW).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_THEME).assertIsDisplayed()

        // Toggle notes view
        onNodeWithTag(TestTags.SETTINGS_NOTES_VIEW).performClick()
        onNodeWithTag(TestTags.SETTINGS_NOTES_VIEW).assertIsDisplayed()
    }
}

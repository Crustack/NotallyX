package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.feature.main.routes.settings.CommonSettingsScreen
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun settingsScreen_rendersOptionsAndAllowsToggling() = runUiTest {
        setContent {
            NotallyXTheme {
                CommonSettingsScreen(
                    database = database,
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_LIST).assertIsDisplayed()
        onNodeWithText("Appearance").assertIsDisplayed()
        onNodeWithText("Notes View").assertIsDisplayed()
        onNodeWithText("Grid").assertIsDisplayed()

        // Toggle notes view to List
        onNodeWithTag(TestTags.SETTINGS_NOTES_VIEW).performClick()
        onNodeWithText("List").assertIsDisplayed()

        // Theme item
        onNodeWithText("Theme").assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_THEME).performClick()
        onNodeWithText("Dark").assertIsDisplayed()

        // Database Maintenance section
        onNodeWithText("Storage & Database").assertIsDisplayed()
        onNodeWithText("Checkpoint Database").assertIsDisplayed()
    }
}

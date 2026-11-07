package com.philkes.notallyx.feature.main

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.philkes.notallyx.Res
import com.philkes.notallyx.cancel
import com.philkes.notallyx.color
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.date_format_overview
import com.philkes.notallyx.feature.settings.SettingsRoute
import com.philkes.notallyx.images_hidden_in_overview_title
import com.philkes.notallyx.labels_hidden_in_overview_title
import com.philkes.notallyx.modified_date
import com.philkes.notallyx.notes_sorted_by
import com.philkes.notallyx.test.CommonComposeUiTestBase
import com.philkes.notallyx.theme
import com.philkes.notallyx.theme_use_dynamic_colors
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

    @Test
    fun settingsScreen_themeDialog_rendersThemeOptionsAndDismisses() = runUiTest {
        setContent {
            NotallyXTheme {
                SettingsRoute(
                    database = database,
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_THEME).performClick()
        onAllNodesWithTextRes(Res.string.theme).assertCountEquals(2)
        onNodeWithTextRes(Res.string.theme_use_dynamic_colors).assertIsDisplayed()

        onNodeWithTextRes(Res.string.cancel).performClick()
        onNodeWithTextRes(Res.string.theme_use_dynamic_colors).assertDoesNotExist()
        onNodeWithTag(TestTags.SETTINGS_THEME).assertIsDisplayed()
    }

    @Test
    fun settingsScreen_sortingDialog_rendersSortOptions() = runUiTest {
        setContent {
            NotallyXTheme {
                SettingsRoute(
                    database = database,
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_SORTING).performScrollTo().performClick()
        onAllNodesWithTextRes(Res.string.notes_sorted_by).assertCountEquals(2)
        onNodeWithTextRes(Res.string.modified_date).assertIsDisplayed()
        onNodeWithTextRes(Res.string.color).assertIsDisplayed()

        onNodeWithTextRes(Res.string.cancel).performClick()
        onNodeWithTextRes(Res.string.color).assertDoesNotExist()
        onNodeWithTag(TestTags.SETTINGS_SORTING).assertIsDisplayed()
    }

    @Test
    fun cardDisplayPreferences_togglesVisibilitySettings() = runUiTest {
        setContent {
            NotallyXTheme {
                SettingsRoute(
                    database = database,
                    onBack = {},
                )
            }
        }

        onNodeWithTextRes(Res.string.labels_hidden_in_overview_title)
            .performScrollTo()
            .assertIsDisplayed()
        onNodeWithTextRes(Res.string.labels_hidden_in_overview_title).performClick()

        onNodeWithTextRes(Res.string.images_hidden_in_overview_title)
            .performScrollTo()
            .assertIsDisplayed()
        onNodeWithTextRes(Res.string.images_hidden_in_overview_title).performClick()
    }

    @Test
    fun dateFormatSettings_showsDateTimeDialogAndDismisses() = runUiTest {
        setContent {
            NotallyXTheme {
                SettingsRoute(
                    database = database,
                    onBack = {},
                )
            }
        }

        onNodeWithTextRes(Res.string.date_format_overview).performScrollTo().performClick()

        onNodeWithTextRes(Res.string.cancel).performClick()
        onNodeWithTextRes(Res.string.date_format_overview).assertIsDisplayed()
    }
}

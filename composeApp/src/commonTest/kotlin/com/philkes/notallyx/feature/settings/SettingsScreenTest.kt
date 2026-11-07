package com.philkes.notallyx.feature.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.test.CommonComposeUiTestBase
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest : CommonComposeUiTestBase() {

    @Test
    fun settingsScreen_rendersBiometricRow_whenBiometricsAvailable() = runUiTest {
        setContent {
            NotallyXTheme {
                SettingsScreen(
                    state = SettingsUiState(isBiometricsAvailable = true),
                    onAction = {},
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_LIST).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_BIOMETRIC_LOCK).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsScreen_hidesBiometricRow_whenBiometricsUnavailable() = runUiTest {
        setContent {
            NotallyXTheme {
                SettingsScreen(
                    state = SettingsUiState(isBiometricsAvailable = false),
                    onAction = {},
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_LIST).assertIsDisplayed()
        onNodeWithTag(TestTags.SETTINGS_BIOMETRIC_LOCK).assertDoesNotExist()
    }

    @Test
    fun settingsScreen_dispatchesAction_whenBiometricLockClicked() = runUiTest {
        var dispatchedAction: SettingsAction? = null

        setContent {
            NotallyXTheme {
                SettingsScreen(
                    state = SettingsUiState(isBiometricsAvailable = true),
                    onAction = { dispatchedAction = it },
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_BIOMETRIC_LOCK).performScrollTo().performClick()
        assertEquals(SettingsAction.ShowBiometricLockDialog, dispatchedAction)
    }

    @Test
    fun settingsScreen_dispatchesAction_whenNotesViewClicked() = runUiTest {
        var dispatchedAction: SettingsAction? = null

        setContent {
            NotallyXTheme {
                SettingsScreen(
                    state = SettingsUiState(),
                    onAction = { dispatchedAction = it },
                )
            }
        }

        onNodeWithTag(TestTags.SETTINGS_NOTES_VIEW).performClick()
        assertEquals(SettingsAction.ShowNotesViewDialog, dispatchedAction)
    }
}

package com.philkes.notallyx.desktop.test

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class DesktopAppSmokeTest : DesktopComposeUiTestBase() {

    @Test
    fun launchApp_displaysEmptyNotes() = runDesktopAppTest {
        onNodeWithText("No notes yet").assertIsDisplayed()
    }
}

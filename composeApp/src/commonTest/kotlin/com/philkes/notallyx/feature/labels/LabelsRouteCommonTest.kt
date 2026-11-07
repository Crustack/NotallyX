package com.philkes.notallyx.feature.labels

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.TestTags
import com.philkes.notallyx.test.CommonComposeUiTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class LabelsRouteCommonTest : CommonComposeUiTestBase() {

    @Test
    fun emptyLabels_displaysEmptyState() = runUiTest {
        setContent {
            NotallyXTheme {
                LabelsRoute(
                    labelDao = labelDao,
                    onBack = {},
                )
            }
        }

        onNodeWithText("No labels yet").assertIsDisplayed()
    }

    @Test
    fun addLabel_persistsToDatabase() = runUiTest {
        setContent {
            NotallyXTheme {
                LabelsRoute(
                    labelDao = labelDao,
                    onBack = {},
                )
            }
        }

        onNodeWithTag(TestTags.ADD_LABEL_INPUT).performTextInput("Work")
        onNodeWithTag(TestTags.ADD_LABEL_BUTTON).performClick()

        waitUntil(timeoutMillis = 5_000) { runBlocking { labelDao.getAll().first().isNotEmpty() } }

        onNodeWithText("Work").assertIsDisplayed()

        runBlocking {
            val labels = labelDao.getAll().first()
            assertEquals(1, labels.size)
            assertEquals("Work", labels.first().value)
        }
    }

    @Test
    fun deleteLabel_removesFromDatabaseAfterConfirmation() = runUiTest {
        insertLabel(name = "Urgent")

        setContent {
            NotallyXTheme {
                LabelsRoute(
                    labelDao = labelDao,
                    onBack = {},
                )
            }
        }

        onNodeWithText("Urgent").assertIsDisplayed()

        onAllNodesWithContentDescription("Delete")[0].performClick()
        onNodeWithTag("ConfirmDialog_Confirm").performClick()

        waitUntil(timeoutMillis = 5_000) { runBlocking { labelDao.getAll().first().isEmpty() } }

        runBlocking {
            val labels = labelDao.getAll().first()
            assertTrue(labels.isEmpty())
        }
    }
}

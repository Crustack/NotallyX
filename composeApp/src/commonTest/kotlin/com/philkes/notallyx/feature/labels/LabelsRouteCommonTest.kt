package com.philkes.notallyx.feature.labels

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
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
    fun clickDeleteButton_triggersRequestDeleteLabel() = runUiTest {
        var requestedLabel: String? = null
        val state =
            LabelsUiState(
                labels = listOf(LabelData("Urgent", true, 0)),
                activeDialog = null,
            )
        setContent {
            NotallyXTheme {
                LabelsScreen(
                    state = state,
                    onAction = {
                        if (it is LabelsAction.RequestDeleteLabel) {
                            requestedLabel = it.label.value
                        }
                    },
                )
            }
        }
        onAllNodesWithTag(TestTags.LABEL_DELETE)[0].performClick()
        assertEquals("Urgent", requestedLabel)
    }

    @Test
    fun confirmDeleteDialog_invokesConfirmAction() = runUiTest {
        var confirmedLabel: String? = null
        val state =
            LabelsUiState(
                labels = listOf(LabelData("Urgent", true, 0)),
                activeDialog = LabelDialogState.Delete("Urgent"),
            )
        setContent {
            NotallyXTheme {
                LabelsScreen(
                    state = state,
                    onAction = {
                        if (it is LabelsAction.ConfirmDeleteLabel) {
                            confirmedLabel = it.label
                        }
                    },
                )
            }
        }
        onNodeWithTag("ConfirmDialog_Confirm").assertIsDisplayed()
        onNodeWithTag("ConfirmDialog_Confirm").performClick()
        assertEquals("Urgent", confirmedLabel)
    }
}

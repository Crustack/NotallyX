package com.philkes.notallyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.R
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.feature.editor.EditTextPlainDialogState
import com.philkes.notallyx.feature.editor.EditTextPlainUiState
import com.philkes.notallyx.feature.editor.screens.EditTextPlainScreen
import com.philkes.notallyx.feature.editor.screens.SearchState

private val SAMPLE_BODY =
    TextFieldValue(
        "Meeting summary and action items:\n- Review pull requests\n- Update dependencies\n- Run instrumented tests"
    )

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditTextPlainScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditTextPlainScreen(
            state =
                EditTextPlainUiState(
                    title = "notes.txt",
                    bodyValue = SAMPLE_BODY,
                    canUndo = true,
                    canRedo = false,
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditTextPlainScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        EditTextPlainScreen(
            state =
                EditTextPlainUiState(
                    title = "todo.md",
                    bodyValue = SAMPLE_BODY,
                    canUndo = false,
                    canRedo = true,
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditTextPlainScreen_SearchActive() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditTextPlainScreen(
            state =
                EditTextPlainUiState(
                    title = "document.txt",
                    bodyValue = SAMPLE_BODY,
                    searchState =
                        SearchState(
                            isActive = true,
                            query = "Review",
                            resultsCount = 1,
                            resultPos = 0,
                        ),
                ),
            onAction = {},
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun EditTextPlainScreen_ErrorDialog() {
    NotallyXTheme(theme = Theme.LIGHT) {
        EditTextPlainScreen(
            state =
                EditTextPlainUiState(
                    title = "readme.txt",
                    bodyValue = SAMPLE_BODY,
                    activeDialog =
                        EditTextPlainDialogState.Error(
                            titleResId = R.string.error,
                            message = "Could not save file to disk",
                        ),
                ),
            onAction = {},
        )
    }
}

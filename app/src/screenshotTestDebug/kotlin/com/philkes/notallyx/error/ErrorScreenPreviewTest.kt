package com.philkes.notallyx.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.common.error.ErrorScreen
import com.philkes.notallyx.core.common.error.ErrorScreenUiState
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

private val SAMPLE_SHORT_STACK_TRACE =
    """
    java.lang.NullPointerException: Attempt to invoke virtual method
    	at com.philkes.notallyx.MainActivity.onCreate(MainActivity.kt:90)
    	at android.app.Activity.performCreate(Activity.java:8000)
    """
        .trimIndent()

private val SAMPLE_LONG_STACK_TRACE =
    """
    java.lang.RuntimeException: Fatal exception occurred in background worker
    	at com.philkes.notallyx.core.common.error.ErrorActivityTest.testCrash(ErrorActivityTest.kt:42)
    	at com.philkes.notallyx.data.repository.NoteRepositoryImpl.syncNotes(NoteRepositoryImpl.kt:156)
    	at com.philkes.notallyx.presentation.viewmodel.NotallyModel.loadData(NotallyModel.kt:88)
    	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
    	at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
    	at kotlinx.coroutines.scheduling.CoroutineScheduler.runSafely(CoroutineScheduler.kt:570)
    	at kotlinx.coroutines.scheduling.CoroutineScheduler.Worker.executeTask(CoroutineScheduler.kt:750)
    	at kotlinx.coroutines.scheduling.CoroutineScheduler.Worker.runWorker(CoroutineScheduler.kt:677)
    	at kotlinx.coroutines.scheduling.CoroutineScheduler.Worker.run(CoroutineScheduler.kt:664)
    """
        .trimIndent()

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ErrorScreen_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ErrorScreen(
            uiState =
                ErrorScreenUiState(
                    exceptionTitle =
                        "java.lang.NullPointerException: Attempt to invoke virtual method",
                    stackTrace = SAMPLE_SHORT_STACK_TRACE,
                    crashLogsLinkText = "View Crash Logs",
                )
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ErrorScreen_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        ErrorScreen(
            uiState =
                ErrorScreenUiState(
                    exceptionTitle =
                        "java.lang.NullPointerException: Attempt to invoke virtual method",
                    stackTrace = SAMPLE_SHORT_STACK_TRACE,
                    crashLogsLinkText = "View Crash Logs",
                )
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ErrorScreen_LongStackTrace() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ErrorScreen(
            uiState =
                ErrorScreenUiState(
                    exceptionTitle =
                        "java.lang.RuntimeException: Fatal exception occurred in background worker",
                    stackTrace = SAMPLE_LONG_STACK_TRACE,
                    crashLogsLinkText = "View Crash Logs",
                )
        )
    }
}

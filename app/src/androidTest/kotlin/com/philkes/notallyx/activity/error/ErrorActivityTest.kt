package com.philkes.notallyx.activity.error

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipboardManager
import android.content.Intent
import android.os.Process
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.intent.matcher.UriMatchers.hasHost
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.error.ErrorActivity
import com.philkes.notallyx.core.common.extensions.MIME_TYPE_ZIP
import com.philkes.notallyx.core.common.extensions.getLogFile
import com.philkes.notallyx.core.common.extensions.getUriForFile
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.feature.migration.ExportMimeType
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.waitUntil
import java.io.File
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class ErrorActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    private fun createErrorIntent(
        stackTrace: String = DEFAULT_STACK_TRACE,
        customCrashData: String? = null,
    ): Intent {
        return Intent(ApplicationProvider.getApplicationContext(), ErrorActivity::class.java)
            .apply {
                putExtra(EXTRA_STACK_TRACE, stackTrace)
                if (customCrashData != null) {
                    putExtra(EXTRA_CUSTOM_CRASH_DATA, customCrashData)
                }
            }
    }

    @Test
    fun copyStackTrace_copiesToClipboard() {
        val scenario = ActivityScenario.launch<ErrorActivity>(createErrorIntent())
        try {
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.copy))
                .performClick()

            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val clipboard =
                    ContextCompat.getSystemService(context, ClipboardManager::class.java)
                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                assertEquals(DEFAULT_STACK_TRACE, clipText)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun exportBackup_dismissDialog_cancelsExport() {
        val scenario = ActivityScenario.launch<ErrorActivity>(createErrorIntent())
        try {
            composeTestRule.onNodeWithText(context.getString(R.string.export_backup)).performClick()

            val continueLabel = context.getString(R.string.continue_)
            val cancelLabel = context.getString(R.string.cancel)

            composeTestRule.onNodeWithText(continueLabel).assertIsDisplayed()
            composeTestRule.onNodeWithText(cancelLabel).performClick()

            composeTestRule.onNodeWithText(continueLabel).assertDoesNotExist()
        } finally {
            scenario.close()
        }
    }

    @Test
    fun exportBackup_confirmDialog_exportsBackupZipSuccessfully() {
        runBlocking {
            database
                .getBaseNoteDao()
                .insert(createBaseNote(title = "Crash Backup Note", body = "Crash Backup Body"))
        }

        Intents.init()
        val exportFile =
            File(context.cacheDir, "crash_backup_test.zip").apply { if (exists()) delete() }

        try {
            val exportUri = context.getUriForFile(exportFile)
            val exportResult =
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().apply { data = exportUri },
                )
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            hasAction(Intent.ACTION_CREATE_DOCUMENT),
                        ),
                    )
                )
                .respondWith(exportResult)

            val scenario = ActivityScenario.launch<ErrorActivity>(createErrorIntent())
            try {
                composeTestRule
                    .onNodeWithText(context.getString(R.string.export_backup))
                    .performClick()

                val continueLabel = context.getString(R.string.continue_)
                composeTestRule.onNodeWithText(continueLabel).performClick()

                intended(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(
                                hasAction(Intent.ACTION_CREATE_DOCUMENT),
                                hasType(MIME_TYPE_ZIP),
                            ),
                        ),
                    )
                )

                waitUntil(10_000) { exportFile.exists() && exportFile.length() > 0 }
                assertTrue("Backup zip file should exist", exportFile.exists())
                assertTrue("Backup zip file should not be empty", exportFile.length() > 0)

                ZipFile(exportFile).use { zip ->
                    val entryNames = zip.entries().asSequence().map { it.name }.toList()
                    assertTrue(
                        "Backup zip should contain database file",
                        entryNames.any { it.contains(NotallyDatabase.DATABASE_NAME) },
                    )
                }
            } finally {
                scenario.close()
            }
        } finally {
            exportFile.delete()
            Intents.release()
        }
    }

    @Test
    fun reportBug_launchesBrowserChooserIntent() {
        Intents.init()
        try {
            val chooserResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            hasAction(Intent.ACTION_VIEW),
                        ),
                    )
                )
                .respondWith(chooserResult)

            val scenario = ActivityScenario.launch<ErrorActivity>(createErrorIntent())
            try {
                composeTestRule
                    .onNodeWithText(context.getString(R.string.report_bug))
                    .performClick()

                intended(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(
                                hasAction(Intent.ACTION_VIEW),
                                hasData(hasHost("github.com")),
                            ),
                        ),
                    )
                )
            } finally {
                scenario.close()
            }
        } finally {
            Intents.release()
        }
    }

    @Test
    fun viewLogs_whenLogFileExists_launchesViewFileIntent() {
        val logFile =
            context.getLogFile().apply {
                parentFile?.mkdirs()
                writeText("Test log entry line 1\nTest log entry line 2")
            }

        Intents.init()
        try {
            val chooserResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            hasAction(Intent.ACTION_VIEW),
                        ),
                    )
                )
                .respondWith(chooserResult)

            val scenario = ActivityScenario.launch<ErrorActivity>(createErrorIntent())
            try {
                composeTestRule.onNodeWithText(context.getString(R.string.view_logs)).performClick()

                intended(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(
                                hasAction(Intent.ACTION_VIEW),
                                hasType(ExportMimeType.TXT.mimeType),
                            ),
                        ),
                    )
                )
            } finally {
                scenario.close()
            }
        } finally {
            logFile.delete()
            Intents.release()
        }
    }

    @Test
    fun viewCrashLogsLink_whenAvailable_launchesViewFileIntent() {
        Intents.init()
        try {
            val chooserResult = Instrumentation.ActivityResult(Activity.RESULT_OK, null)
            intending(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            hasAction(Intent.ACTION_VIEW),
                        ),
                    )
                )
                .respondWith(chooserResult)

            val scenario =
                ActivityScenario.launch<ErrorActivity>(
                    createErrorIntent(customCrashData = Process.myPid().toString())
                )
            try {
                val viewCrashLogsLabel = context.getString(R.string.view_crash_logs)
                composeTestRule.waitUntil(10_000) {
                    composeTestRule
                        .onAllNodes(androidx.compose.ui.test.hasText(viewCrashLogsLabel))
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }

                composeTestRule.onNodeWithText(viewCrashLogsLabel).performClick()

                intended(
                    allOf(
                        hasAction(Intent.ACTION_CHOOSER),
                        hasExtra(
                            Intent.EXTRA_INTENT,
                            allOf(
                                hasAction(Intent.ACTION_VIEW),
                                hasType(ExportMimeType.TXT.mimeType),
                            ),
                        ),
                    )
                )
            } finally {
                scenario.close()
            }
        } finally {
            Intents.release()
        }
    }

    companion object {
        private const val EXTRA_STACK_TRACE = "cat.ereza.customactivityoncrash.EXTRA_STACK_TRACE"
        private const val EXTRA_CUSTOM_CRASH_DATA =
            "cat.ereza.customactivityoncrash.EXTRA_CUSTOM_CRASH_DATA"

        private const val DEFAULT_STACK_TRACE_DETAILS =
            "\tat com.philkes.notallyx.FakeClass.fakeMethod(FakeClass.kt:42)\n" +
                "\tat com.philkes.notallyx.FakeClass.anotherMethod(FakeClass.kt:100)"
        private const val DEFAULT_STACK_TRACE =
            "java.lang.NullPointerException: Test crash occurred\n$DEFAULT_STACK_TRACE_DETAILS"
    }
}

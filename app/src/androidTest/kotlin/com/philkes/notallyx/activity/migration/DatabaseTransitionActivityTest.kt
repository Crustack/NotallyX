package com.philkes.notallyx.activity.migration

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.backup.DatabaseAction
import com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity
import com.philkes.notallyx.test.UiTestBase
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class DatabaseTransitionActivityTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun displaysStatusTextAndErrorDialog_whenEncryptionCipherMissing() {
        Intents.init()
        val intent =
            Intent(context, DatabaseTransitionActivity::class.java).apply {
                putExtra(
                    DatabaseTransitionActivity.EXTRA_ACTION,
                    DatabaseAction.ENABLE_BIOMETRIC_LOCK.name,
                )
                putExtra(DatabaseTransitionActivity.EXTRA_ENABLE_BIOMETRIC_LOCK, true)
                putExtra(DatabaseTransitionActivity.EXTRA_ENCRYPT_DATABASE, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        val scenario = ActivityScenario.launch<DatabaseTransitionActivity>(intent)
        try {
            // Verify status text is displayed
            composeTestRule
                .onNodeWithText(context.getString(R.string.encrypting_database_biometric))
                .assertIsDisplayed()

            // Verify ErrorDialog is displayed with title and message
            composeTestRule.onNodeWithText(context.getString(R.string.error)).assertIsDisplayed()
            composeTestRule
                .onNodeWithText("Missing cipher for biometric encryption")
                .assertIsDisplayed()

            // Dismiss the error dialog via OK button
            composeTestRule.onNodeWithText(context.getString(android.R.string.ok)).performClick()

            // Verify MainActivity was launched on error dismissal
            Intents.intended(hasComponent(MainActivity::class.java.name))
        } finally {
            scenario.close()
            Intents.release()
        }
    }
}

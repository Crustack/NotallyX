---
name: write-ui-tests
description: Guide for creating and maintaining Android UI instrumented tests in NotallyX using Jetpack Compose UI testing APIs, Espresso, UiAutomator, and project extensions in UiUtils.kt and UiTestBase. Standardizes test setup, database pre-filling via runBlocking, navigation, localized string lookups, and asynchronous assertions.
metadata:
   author: NotallyX
   last-updated: '2026-10-03'
   keywords:
      - Android
      - UI Testing
      - Jetpack Compose
      - Espresso
      - UiAutomator
      - AndroidJUnit4
      - UiTestBase
      - UiUtils
      - runBlocking
      - Room
      - ActivityScenario
   scope: Android instrumented tests (app/src/androidTest/)
   primary-goal: Ensure consistent, flake-free Compose and hybrid UI tests using Jetpack Compose testing APIs, Espresso, UiAutomator, and project testing base utilities matching NotallyX patterns.
---

# NotallyX UI Testing Skill

## Purpose

Use this skill to create, update, or maintain instrumented UI tests in `app/src/androidTest/kotlin/com/philkes/notallyx/`. This guide standardizes how tests are structured for Jetpack Compose screens/components, Android Views (`MainActivity`, `EditActivity`), and main drawer fragments (`DeletedFragment`, `ArchivedFragment`, `LabelsFragment`, `RemindersFragment`, `SettingsFragment`).

---

## Core Principles & Framework Architecture

### 1. Leverage `UiTestBase` and Official Android Testing APIs

In NotallyX, instrumented UI tests rely directly on Jetpack Compose UI Testing (`androidx.compose.ui.test`), Espresso (`androidx.test.espresso`), UiAutomator (`androidx.test.uiautomator`), and custom test helpers defined in `:test-lib` (`UiTestBase` and `UiUtils.kt`).

* **Inherit from `UiTestBase`**: All UI test classes extend `UiTestBase`, which automatically handles context wrapping, database access, preference initialization, back button access, and runtime permission granting (`POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`).
* **Compose Test Rules**:
  * Use `createEmptyComposeRule()` when launching activities manually via `ActivityScenario.launch(...)` (e.g., `EditActivityTest`, `LabelsFragmentTest`).
  * Use `createAndroidComposeRule<MainActivity>()` when testing screens or fragments hosted directly by `MainActivity` (e.g., `SettingsFragmentTest`).
* **Asynchronous Wait Helpers**: Use `composeTestRule.waitUntil(timeoutMs) { condition }` or global `waitUntil(timeoutMs) { condition }` to poll until asynchronous Room, StateFlow, or UI state changes complete.

```kotlin
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class FeatureFragmentTest : UiTestBase() {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun featureWorkflow() {
        // Seed database or preference state
        runBlocking {
            database.getBaseNoteDao().insert(createBaseNote(title = "Test Note"))
        }

        // Navigate and perform Compose interactions
        composeTestRule.navigateTo(NotallyNavKey.Settings)
        composeTestRule.onNodeWithText(context.getString(R.string.view)).performClick()
    }
}
```

### 2. Framework Selection Rules

| Screen Component | Testing Framework & Rule | Interaction Mechanism |
|---|---|---|
| **Jetpack Compose UI** | `ComposeTestRule` (`createEmptyComposeRule` / `createAndroidComposeRule`) | `onNodeWithText(...)`, `onNodeWithContentDescription(...)`, `performClick()`, `performTextInput(...)` |
| **Android Views (XML)** | Espresso (`onView(...)`) | `onView(withId(...)).perform(click())`, `typeText(...)`, `closeSoftKeyboard()` |
| **Material Dialogs / Pickers** | Espresso / Compose Interop | `onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())` |
| **System / OS Interactions** | UiAutomator (`UiDevice`) | Permission dialogs, notifications, drag-and-drop, device lock/unlock (`lockAndUnlockDevice()`) |

### 3. Strict UI Interaction Rule (NO Direct Activity / ViewModel State Mutation)

> [!CAUTION]
> **Never mutate Activity state or ViewModel properties directly in UI tests.**
> UI tests (instrumented tests in `app/src/androidTest/` and screenshot tests in `app/src/test/`) must interact with the user interface purely through user inputs—such as typing text into text fields with `.performTextInput(...)` / `typeText(...)`, clicking buttons with `.performClick()` / `.perform(click())`, or scrolling—or through standard Compose/Espresso nodes.
>
> Direct state mutation (e.g., `activity.titleState.value = "..."`, `activity.notallyModel.title = "..."`) is strictly forbidden in UI tests because it bypasses IME/keyboard input events, recomposition pipelines, and UI event listeners, producing invalid UI test results.

---

## Jetpack Compose UI Testing Patterns

### 1. Matching Nodes with Localized String Resources

Always look up strings dynamically through `context.getString(R.string.*)` rather than hardcoding raw UI string values. This ensures tests stay resilient against localization updates and matches existing tests like `EditActivityTest` and `SettingsFragmentTest`.

```kotlin
val takeNoteLabel = context.getString(R.string.take_note)
val titleLabel = context.getString(R.string.title)
val saveLabel = context.getString(R.string.save)

// Find node by localized content description
composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

// Find node by localized text
composeTestRule.onNodeWithText(saveLabel).performClick()
```

### 2. Semantics Matchers & Specialized Interactions

* **Text Fields with Actions**: Match editable fields using `hasSetTextAction()` or `hasContentDescription(...)`:
  ```kotlin
  composeTestRule.onNode(hasSetTextAction()).performTextInput("Text value")
  composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput("Title")
  ```
* **Appending Text**: Use the `appendText(...)` extension on `SemanticsNodeInteraction` (defined in `UiUtils.kt`) to append text at the end of an editable text field:
  ```kotlin
  composeTestRule.onNodeWithContentDescription(titleLabel).appendText(" Appended")
  ```
* **Dialog Nodes**: Target nodes inside Compose dialogs using `hasAnyAncestor(isComposeDialog())`:
  ```kotlin
  composeTestRule
      .onNode(hasText(context.getString(R.string.grid)) and hasAnyAncestor(isComposeDialog()))
      .performClick()
  ```
* **Node Collections**: Use `onAllNodes(...)` with `.onLast()`, `.fetchSemanticsNodes()`, or `.size` to assert on multiple matching nodes:
  ```kotlin
  composeTestRule.waitUntil(5_000) {
      composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().size >= 2
  }
  composeTestRule.onAllNodes(hasText("label")).onLast().assertIsDisplayed()
  ```

### 3. Compose Semantics Actions and Assertions Cheat-Sheet

| Category | Method / Helper | Description |
|---|---|---|
| **Actions** | `.performClick()` | Clicks on the node |
| | `.performTextInput("text")` | Inputs text into an editable node |
| | `.performTextReplacement("text")` | Replaces existing text |
| | `.appendText("text")` | Appends text to the end of editable node text |
| | `.performScrollTo()` | Scrolls parent until node is visible |
| | `.performTouchInput { swipeLeft() }` | Performs custom touch gestures |
| **Assertions** | `.assertIsDisplayed()` | Asserts node is visible on screen |
| | `fetchSemanticsNodes().isNotEmpty()` | Checks node presence in tree inside `waitUntil` |
| | `hasAnyAncestor(isComposeDialog())` | Filters nodes contained within a Compose Dialog |

---

## Hybrid UI Testing (Compose + Espresso + UiAutomator)

In NotallyX, screens often combine Compose UIs with Android Views or Material Components (e.g. Material Date/Time pickers or custom dialogs).

### 1. Interacting with Android Views / Material Pickers via Espresso

When interacting with native Android View components like Material DatePicker or TimePicker, use Espresso's `onView(...)`:

```kotlin
fun confirmDateAndTime() {
    onView(withId(com.google.android.material.R.id.confirm_button)).perform(click())
    onView(withId(com.google.android.material.R.id.material_timepicker_ok_button)).perform(click())
}
```

### 2. View Locator Extensions (`UiUtils.kt`)

`UiUtils.kt` provides concise extension properties on `Int` and `String` for Espresso view interactions:

```kotlin
// Locates view by ID and asserts displayed
R.id.MainListView.byId()

// Locates view by text string resource
R.string.save.byText()

// Locates view by content description resource
R.string.pin.byContentDescription()

// Locates view by raw literal text
"Test".byText()
```

### 3. Intercepting Chooser / Intent Activity Results

When testing features that trigger system file pickers or export/import intents (e.g. export settings in `SettingsFragmentTest`), use Espresso `Intents`:

```kotlin
Intents.init()
intending(
    allOf(
        hasAction(Intent.ACTION_CHOOSER),
        hasExtra(`is`(Intent.EXTRA_INTENT), hasAction(Intent.ACTION_CREATE_DOCUMENT)),
    )
).respondWithFunction {
    Instrumentation.ActivityResult(
        Activity.RESULT_OK,
        Intent().apply {
            data = context.getUriForFile(exportFile)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        },
    )
}
```

---

## Inheriting from `UiTestBase`

For all instrumented UI tests, extend `UiTestBase`.

```kotlin
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class EditActivityTest : UiTestBase() {
    @get:Rule val composeTestRule = createEmptyComposeRule()
    // ...
}
```

`UiTestBase` provides:
* **`context`**: Context wrapper around `InstrumentationRegistry.getInstrumentation().targetContext`.
* **`database`**: Direct access to `NotallyDatabase`.
* **`preferences`**: Access to `NotallyXPreferences`.
* **`toolbarBackButton`**: `ViewInteraction` for navigation back in top app bars (`R.string.back.byContentDescription()`).
* **`@Before setup()`**: Handles runtime notification permissions (`POST_NOTIFICATIONS`), exact alarm permissions (`SCHEDULE_EXACT_ALARM`), and resets test preferences before each test execution.

---

## Data Pre-filling & State Preparation

### 1. Pre-filling Database / Preferences via `runBlocking`

To seed the Room database with notes, labels, or preferences before launching an activity or rule, execute DAO operations inside `runBlocking`.

```kotlin
runBlocking {
    database.getLabelDao().insert(listOf(Label("label", 0)))
    database.getBaseNoteDao().insert(
        listOf(
            createBaseNote(
                title = "Test Note",
                body = "Body text",
                pinned = true,
                labels = listOf("label")
            )
        )
    )
}
```

> [!IMPORTANT]
> Always seed database or preference state **before** launching `ActivityScenario.launch(...)` or performing actions that trigger initial screen rendering.

### 2. Domain Factory Helpers (`UiUtils.kt`)

Use domain factories defined in `UiUtils.kt`:
* **`createBaseNote(...)`**: Construct test notes with custom titles, bodies, labels, reminders, items, or folders.
* **`createListItem(...)`**: Construct list item entities for list notes.

Example:
```kotlin
val note = createBaseNote(
    title = "List Note",
    pinned = true,
    items = listOf(createListItem("Item A"), createListItem("Item B", isChild = true))
)
```

### 3. Navigation Helpers (`UiUtils.kt`)

Use the `navigateTo` extension function on `ComposeTestRule` or globally to open the navigation drawer and switch screens:

```kotlin
// Compose navigation helper
composeTestRule.navigateTo(NotallyNavKey.Settings)
composeTestRule.navigateTo(NotallyNavKey.Notes)
composeTestRule.navigateTo(NotallyNavKey.Labels)
```

---

## Synchronization & Asynchronous Assertions

### 1. Compose `waitUntil` Polling

When waiting for database updates, StateFlow emissions, or UI transitions in Compose:

```kotlin
composeTestRule.waitUntil(10_000) {
    composeTestRule.onAllNodes(hasText("label")).fetchSemanticsNodes().isNotEmpty()
}
```

### 2. Specialized Settings Wait Helper (`waitUntilSettingsValue`)

For settings changes in `SettingsFragmentTest`:

```kotlin
composeTestRule.waitUntilSettingsValue(R.string.always_show_search_bar, R.string.enabled)
composeTestRule.waitUntilSettingsValue(R.string.biometric_lock, R.string.disabled)
```

### 3. Global Polling & Verification Extensions (`UiUtils.kt`)

* **`waitUntil(timeoutMs) { condition }`**: Global condition polling function.
* **`waitUntilSucceeds(timeoutMs) { interaction }`**: Polls an Espresso interaction until it succeeds without throwing an exception.
* **`assertToastDisplayed(textResId)`**: Asserts a Toast message with localized text was displayed.
* **`assertLogAppeared(tag, prefix)`**: Checks Logcat output for expected log entry.

---

## Baseline Test Templates

### Template 1: Activity UI Test (`ActivityScenario.launch` + `createEmptyComposeRule`)

*Baseline from `EditActivityTest.kt`*

```kotlin
package com.philkes.notallyx.activity.edit

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.waitUntil
import kotlinx.coroutines.runBlocking
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class CreateNoteTest : UiTestBase() {

    @get:Rule val composeTestRule = createEmptyComposeRule()

    @Test
    fun createAndEditTextNote() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        val takeNoteLabel = context.getString(R.string.take_note)
        val titleLabel = context.getString(R.string.title)
        val bodyLabel = context.getString(R.string.note)
        val saveLabel = context.getString(R.string.save)

        composeTestRule.onNodeWithContentDescription(takeNoteLabel).performClick()

        composeTestRule.onNodeWithContentDescription(bodyLabel).performTextInput("Body text")
        composeTestRule.onNodeWithContentDescription(titleLabel).performTextInput("Note Title")

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.pin)).performClick()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.back)).performClick()

        composeTestRule.waitUntil(10_000) {
            composeTestRule.onAllNodes(hasText("Note Title")).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Note Title").assertIsDisplayed()

        scenario.close()
    }
}
```

### Template 2: Fragment / Hosted Screen Test (`createAndroidComposeRule<MainActivity>`)

*Baseline from `SettingsFragmentTest.kt`*

```kotlin
package com.philkes.notallyx.activity.main.fragments

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog as isComposeDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.navigation.NotallyNavKey
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.waitUntilSettingsValue
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class SettingsFragmentTest : UiTestBase() {

    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsToggleWorkflow() {
        composeTestRule.navigateTo(NotallyNavKey.Settings)

        val saveLabel = context.getString(R.string.save)

        composeTestRule
            .onNodeWithText(context.getString(R.string.always_show_search_bar))
            .performScrollTo()
            .performClick()

        composeTestRule
            .onNode(hasText(context.getString(R.string.enabled)) and hasAnyAncestor(isComposeDialog()))
            .performClick()

        composeTestRule.onNodeWithText(saveLabel).performClick()

        composeTestRule.waitUntilSettingsValue(R.string.always_show_search_bar, R.string.enabled)
    }
}
```

---

## Best Practices Checklist

- [ ] Class extends `UiTestBase` to inherit DB access, context, preferences, and automatic permission setup.
- [ ] Annotated with `@LargeTest`, `@RunWith(AndroidJUnit4::class)`, and `@FixMethodOrder`.
- [ ] Selected the appropriate Compose test rule (`createEmptyComposeRule()` for `ActivityScenario.launch`, or `createAndroidComposeRule<MainActivity>()` for hosted MainActivity fragments).
- [ ] Referenced UI labels dynamically using `context.getString(R.string.*)` instead of hardcoded strings.
- [ ] Interacted with the UI purely through UI actions (`performClick()`, `performTextInput()`, Espresso actions) without mutating Activity or ViewModel properties directly.
- [ ] Wrapped database and preference pre-filling inside `runBlocking { ... }` prior to UI interactions.
- [ ] Utilized helper functions in `UiUtils.kt` (`navigateTo`, `createBaseNote`, `waitUntil`, `waitUntilSettingsValue`, `appendText`).
- [ ] Handled asynchronous updates with `composeTestRule.waitUntil(timeoutMs) { ... }` instead of fixed `Thread.sleep()`.
- [ ] Placed any new reusable custom ViewActions, Matchers, or Compose test helpers directly into `UiUtils.kt`.
- [ ] Validated tests by running the specific test class/method via Gradle:
  ```shell
  ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.philkes.notallyx.activity.edit.EditActivityTest#createAndEditTextNote
  ```

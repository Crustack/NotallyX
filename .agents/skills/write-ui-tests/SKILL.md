---
name: write-ui-tests
description: Guide for creating and maintaining Android UI instrumented tests in NotallyX using Espresso, UiAutomator, and project-specific extensions in UiUtils.kt and UiTestBase. Standardizes test setup, database pre-filling via runBlocking, RecyclerView matching, navigation, and asynchronous assertions.
metadata:
   author: NotallyX
   last-updated: '2026-10-03'
   keywords:
      - Android
      - UI Testing
      - Espresso
      - UiAutomator
      - AndroidJUnit4
      - UiTestBase
      - UiUtils
      - runBlocking
      - Room
      - ActivityScenario
      - RecyclerView
   scope: Android instrumented tests (app/src/androidTest/)
   primary-goal: Ensure consistent, reliable, and readable Espresso/UiAutomator UI tests matching NotallyX patterns
---

# NotallyX UI Testing Skill

## Purpose

Use this skill to create, update, or maintain instrumented UI tests in `app/src/androidTest/kotlin/com/philkes/notallyx/`. This guide standardizes how tests are structured for Activities (`MainActivity`, `EditActivity`) and main drawer fragments (`DeletedFragment`, `ArchivedFragment`, `LabelsFragment`, `RemindersFragment`).

---

## Core Principles & Setup

### 1. Prefer Espresso Over UiAutomator

Always prefer using **Espresso** for view matchers, actions, and assertions. Only use **UiAutomator** (e.g. `UiDevice`, `dragAndDrop`, `swipeItem`) if an interaction or assertion is not possible with Espresso (such as system-level interactions, device locking/waking, or complex gesture drag-and-drop).

### 2. Inherit from `UiTestBase`

Every UI test class must extend `UiTestBase`.

```kotlin
@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class FeatureFragmentTest : UiTestBase() {
   // ...
}
```

`UiTestBase` provides:
* **`context`**: Context wrapper around `InstrumentationRegistry.getInstrumentation().targetContext`.
* **`database`**: Access to `NotallyDatabase`.
* **`preferences`**: Access to `NotallyXPreferences`.
* **`toolbarBackButton`**: `ViewInteraction` for navigation back in top app bars.
* **`@Before setup()`**: Handles runtime notification permissions (`POST_NOTIFICATIONS`), exact alarm permissions (`SCHEDULE_EXACT_ALARM`), and resets test preferences.

---

## Data Pre-filling & State Preparation

### 1. Use `runBlocking` for Pre-filling Database / Mocking State

To seed the database with notes, labels, or preferences before launching an activity or fragment, execute DAO operations inside `runBlocking`.

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
> Always seed database or preference state **before** calling `ActivityScenario.launch(...)`, so the Activity reads the newly populated data on startup.

### 2. Factory Helper Functions

Use the test domain factories defined in `UiUtils.kt`:
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

### 3. Launching and Closing `ActivityScenario`

Always manage `ActivityScenario` lifecycle explicitly and close it at the end of the test.

```kotlin
val scenario = ActivityScenario.launch(MainActivity::class.java)

// Perform Espresso actions and assertions...

scenario.close()
```

For main thread actions (e.g. status bar notification pinning), use `scenario.onActivity { activity -> ... }` or `withContext(Dispatchers.Main)` inside `runBlocking`.

---

## Custom Extension Functions (`UiUtils.kt`)

Avoid raw `onView(withId(...))` or verbose matchers when standard `UiUtils.kt` extension functions are available.

> [!TIP]
> When writing new custom, reusable `ViewAction`s, `Matcher`s, or UI assertion helpers, place them directly into `UiUtils.kt` so they can be shared across all test files.

### 1. View Locators & Matching Extensions

| Extension | Usage | Description |
|---|---|---|
| `Int.byId(...)` | `R.id.MainListView.byId()` | Locates a view by resource ID. Checks if displayed by default (`checkDisplayed = true`). |
| `Int.byText(...)` | `R.string.save.byText()` | Locates a view matching string resource text. |
| `Int.byContentDescription(...)` | `R.string.pin.byContentDescription()` | Locates a view matching string resource content description. |
| `String.byText(...)` | `"Test".byText()` | Locates a view matching exact literal text. |
| `String.byContentDescription(...)` | `"Unpin".byContentDescription()` | Locates a view matching exact literal content description. |
| `onDisplayView(matcher)` | `onDisplayView(withText("label"))` | Equivalent to `onView(allOf(matcher, isDisplayed()))`. |

Example:
```kotlin
// Locating and interacting with views
R.id.TakeNote.byId().perform(click())
R.id.EnterTitle.byId().perform(typeText("Title"), closeSoftKeyboard())
R.string.save.byText(withId(android.R.id.button1)).perform(scrollTo(), click())
```

### 2. Navigation Helper (`navigateTo`)

Use `navigateTo(fragmentId)` to open the navigation drawer and select a main drawer destination.

```kotlin
navigateTo(R.id.Archived)
navigateTo(R.id.Deleted)
navigateTo(R.id.Labels)
navigateTo(R.id.Reminders)
```

### 3. RecyclerView & List Position Interactivity

* **Performing actions at a position**:
  ```kotlin
  R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, click()))
  R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))
  ```

* **Asserting child views at a position (`onPositionView`)**:
  ```kotlin
  R.id.MainListView.byId()
      .onPositionView(0, withId(R.id.Title))
      .check(matches(withText("Expected Title")))
  ```

* **Label items locator**:
  ```kotlin
  onLabelItem("label1").check(matches(withText("label1")))
  ```

---

## Synchronization & Asynchronous Verification

### 1. Waiting for View State (`waitUntilSucceeds`)

When UI updates occur asynchronously (e.g. Room database flow emissions or navigation transitions), wrap assertions inside `waitUntilSucceeds`:

```kotlin
waitUntilSucceeds {
    R.id.MainListView.byId()
        .onPositionView(0, withId(R.id.Title))
        .check(matches(withText("Test")))
}
```

### 2. Waiting for Condition (`waitUntil`)

When waiting for background state or file operations:

```kotlin
waitUntil(10_000L) {
    NotallyDatabase.getCurrentDatabaseFile(context) == NotallyDatabase.getExternalDatabaseFile(context)
}
```

### 3. Asserting Toast / Work / Log Output

* **Toasts**: `assertToastDisplayed(R.string.note_deleted)` or `assertToastDisplayed("Text")`
* **WorkManager**: `assertWorkExecuted(workUniqueName)`
* **Logcat**: `assertLogAppeared(tag, expectedPrefix)`

---

## Complete Test Templates

### Template 1: Fragment UI Test (`DeletedFragmentTest` style)

```kotlin
package com.philkes.notallyx.presentation.activity.main.fragments

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.byText
import com.philkes.notallyx.test.createBaseNote
import com.philkes.notallyx.test.navigateTo
import com.philkes.notallyx.test.onDisplayView
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.waitUntilSucceeds
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.allOf
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class SampleFragmentTest : UiTestBase() {

    @Test
    fun sampleFragmentWorkflow() {
        // 1. Seed database via runBlocking
        runBlocking {
            database.getBaseNoteDao().insert(
                listOf(createBaseNote(title = "Sample Note", body = "Body Content"))
            )
        }

        // 2. Launch ActivityScenario
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // 3. Interact with main list
        R.id.MainListView.byId().perform(actionOnItemAtPosition<ViewHolder>(0, longClick()))
        R.string.delete.byContentDescription().perform(click())

        // 4. Verify item removed from main list
        "Sample Note".byText(checkDisplayed = false).check(doesNotExist())

        // 5. Navigate to target fragment
        navigateTo(R.id.Deleted)

        // 6. Assert item appears in fragment list
        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("Sample Note")))
        }

        // 7. Close scenario
        scenario.close()
    }
}
```

### Template 2: Activity UI Test (`EditActivityTest` style)

```kotlin
package com.philkes.notallyx.presentation.activity.edit

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.waitUntilSucceeds
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@FixMethodOrder
class SampleEditActivityTest : UiTestBase() {

    @Test
    fun createAndSaveNote() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // Create new note
        R.id.TakeNote.byId().perform(click())

        // Enter title & body
        R.id.EnterTitle.byId().perform(typeText("New Note"), closeSoftKeyboard())
        R.id.EnterBody.byId().perform(typeText("New Body"), closeSoftKeyboard())

        // Pin note
        R.string.pin.byContentDescription().perform(click())

        // Navigate back to main screen
        toolbarBackButton.perform(click())

        // Verify note displays on main list
        waitUntilSucceeds {
            R.id.MainListView.byId()
                .onPositionView(0, withId(R.id.Title))
                .check(matches(withText("New Note")))
        }

        scenario.close()
    }
}
```

---

## Best Practices Checklist

- [ ] Preferred Espresso over UiAutomator, reserving UiAutomator strictly for operations not possible with Espresso.
- [ ] Extended `UiTestBase` for common context, DB, preferences, and permissions handling.
- [ ] Added `@LargeTest`, `@RunWith(AndroidJUnit4::class)`, and `@FixMethodOrder`.
- [ ] Wrapped database/preference seeding in `runBlocking { ... }` prior to `ActivityScenario.launch(...)`.
- [ ] Used `Int.byId()`, `Int.byText()`, `Int.byContentDescription()`, `String.byText()`, or `String.byContentDescription()` extensions instead of raw matchers.
- [ ] Placed any new reusable custom ViewActions or Matchers in `UiUtils.kt`.
- [ ] Used `navigateTo(R.id.FragmentId)` for navigation drawer interactions.
- [ ] Used `waitUntilSucceeds { ... }` when asserting asynchronous flow updates or UI transitions.
- [ ] Explicitly called `scenario.close()` at the end of each test method.

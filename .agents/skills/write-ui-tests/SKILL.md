---
name: write-ui-tests
description: Guide for creating and maintaining Android UI instrumented tests in NotallyX using Jetpack Compose UI testing best practices and Kaspresso (ComposeScreen, Kakao Screen, flakiness safety), Espresso, UiAutomator, and project extensions in UiUtils.kt and UiTestBase. Standardizes test setup, Page Object model, database pre-filling via runBlocking, and asynchronous assertions.
metadata:
   author: NotallyX
   last-updated: '2026-10-03'
   keywords:
      - Android
      - UI Testing
      - Jetpack Compose
      - Kaspresso
      - ComposeScreen
      - KNode
      - Kakao
      - Espresso
      - UiAutomator
      - AndroidJUnit4
      - UiTestBase
      - UiUtils
      - runBlocking
      - Page Object Model
      - Room
   scope: Android instrumented tests (app/src/androidTest/)
   primary-goal: Ensure consistent, flake-free Compose and hybrid UI tests using Kaspresso Page Objects and Jetpack Compose testing best practices matching NotallyX patterns.
---

# NotallyX UI Testing Skill

## Purpose

Use this skill to create, update, or maintain instrumented UI tests in `app/src/androidTest/kotlin/com/philkes/notallyx/`. This guide standardizes how tests are structured for Jetpack Compose screens/components, Android Views (`MainActivity`, `EditActivity`), and main drawer fragments (`DeletedFragment`, `ArchivedFragment`, `LabelsFragment`, `RemindersFragment`, `SettingsFragment`).

---

## Core Principles & Framework Architecture

### 1. Leverage Kaspresso as the UI Testing Foundation

**Kaspresso** provides a high-level, declarative DSL built on top of Jetpack Compose Testing APIs, Kakao (for Android Views), Espresso, and UiAutomator.

* **Flakiness Protection**: All Kaspresso steps and assertions execute within `flakySafely` by default, automatically retrying transient failures up to a configurable timeout.
* **Readable Steps**: Group interactions into `step("Step name") { ... }` blocks for clear console logs, test reporting, and debugging output.
* **Page Object Pattern**: Enforce separation between test logic and UI elements using `ComposeScreen` (for Compose) and Kakao `Screen` (for Views).

```kotlin
@LargeTest
@RunWith(AndroidJUnit4::class)
class FeatureComposeTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun featureWorkflow() = run {
        step("1. Seed database") {
            runBlocking {
                database.getBaseNoteDao().insert(...)
            }
        }

        step("2. Verify Compose screen elements") {
            MainComposeScreen(composeTestRule) {
                titleText.assertIsDisplayed()
                titleText.assertTextEquals("Expected Title")
                addNoteButton.performClick()
            }
        }
    }
}
```

### 2. Framework Selection Rules

| Screen Component | Preferred Testing Framework | Page Object Abstraction |
|---|---|---|
| **Pure Jetpack Compose** | Kaspresso Compose + `ComposeTestRule` | `ComposeScreen<T>(semanticsProvider)` |
| **Android Views (XML)** | Kaspresso Kakao + Espresso | Kakao `Screen<T>` (`KView`, `KRecyclerView`, `KButton`) |
| **Hybrid (Views + Compose)** | Kaspresso Kakao `Screen` + `ComposeScreen` | Combined Screen Objects |
| **System/OS Interactions** | UiAutomator (`UiDevice`) | Only when Espresso/Compose APIs cannot interact (e.g. system permissions, notifications) |

---

## Jetpack Compose UI Testing Best Practices

### 1. Page Object Pattern with `ComposeScreen`

Define Page Objects extending `ComposeScreen<T>` for all Compose screens or reusable Composable sections. Use `KNode` properties defined with semantics matchers (`hasTestTag`, `hasText`, `hasContentDescription`).

```kotlin
object SettingsComposeScreen : ComposeScreen<SettingsComposeScreen>(
    semanticsProvider = null // Injected when invoking screen
) {
    val screenTitle: KNode = child { hasTestTag("SettingsTitle") }
    val backupSectionHeader: KNode = child { hasText("Backups") }
    val autoBackupToggle: KNode = child { hasTestTag("AutoBackupToggle") }
    val saveButton: KNode = child { hasContentDescription("Save Settings") }
    val customItem: (String) -> KNode = { name ->
        child { hasTestTag("SettingItem_$name") }
    }
}
```

> [!TIP]
> Always pass `semanticsProvider` (the `ComposeTestRule`) when instantiating or referencing `ComposeScreen` inside tests:
> ```kotlin
> SettingsComposeScreen(composeTestRule) {
>     screenTitle.assertIsDisplayed()
>     autoBackupToggle.performClick()
> }
> ```

### 2. Semantics & TestTags Convention

* **Prefer `Modifier.testTag(...)`**: Assign explicit, unique test tags to interactive or assertable Composable nodes (e.g., `"NoteItem_123"`, `"AddNoteFAB"`, `"TitleInput"`).
* **Use Semantics Matchers**: Match nodes using `hasTestTag(...)`, `hasText(...)`, `hasContentDescription(...)`, or `hasSetTextAction()`.
* **Avoid Fragile Tree Traversal**: Do not rely on relative node hierarchy index or deep parent-child positioning unless testing structural layout specifics.

```kotlin
// Production Composable:
Text(
    text = note.title,
    modifier = Modifier.testTag("NoteTitle_${note.id}")
)

// Test KNode:
val noteTitle = child { hasTestTag("NoteTitle_123") }
```

### 3. KNode Actions and Assertions Cheat-Sheet

| Category | Method | Description |
|---|---|---|
| **Actions** | `.performClick()` | Clicks on the node |
| | `.performTextInput("text")` | Inputs text into a focused text field |
| | `.performTextClear()` | Clears existing text |
| | `.performScrollTo()` | Scrolls parent until node is visible |
| | `.performTouchInput { swipeUp() }` | Performs custom touch gestures |
| **Assertions** | `.assertIsDisplayed()` | Asserts node is visible on screen |
| | `.assertDoesNotExist()` | Asserts node is not in the semantics tree |
| | `.assertTextEquals("expected")` | Asserts exact text match |
| | `.assertTextContains("substring")` | Asserts text contains substring |
| | `.assertIsEnabled()` / `.assertIsNotEnabled()` | Asserts enabled state |
| | `.assertIsSelected()` / `.assertIsToggleable()` | Asserts selection or toggle state |

---

## Hybrid UI Testing (Android Views + Jetpack Compose)

In NotallyX, screens often combine XML Views (`RecyclerView`, `Toolbar`) with `ComposeView` wrappers (`HeaderVH`, `ReminderVH`, `LabelsFragment`, `SettingsFragment`).

### 1. Defining Kakao Screen for Views & Compose

Kakao allows mixing `KView` / `KRecyclerView` with `KNode` or `ComposeScreen` blocks in a single screen object:

```kotlin
object LabelsScreen : Screen<LabelsScreen>() {
    // Kakao XML View matchers
    val recyclerView = KRecyclerView(
        builder = { withId(R.id.MainListView) },
        itemTypeBuilder = { itemType(::LabelItemVH) }
    )
    val addLabelFab = KView { withId(R.id.AddLabelFab) }

    class LabelItemVH(parent: Matcher<View>) : KRecyclerItem<LabelItemVH>(parent) {
        val labelText = KView { withId(R.id.LabelText) }
        val editButton = KView { withId(R.id.EditButton) }
        val deleteButton = KView { withId(R.id.DeleteButton) }
    }
}
```

### 2. Interacting across Views and Compose in a Single Test

```kotlin
@Test
fun hybridViewAndComposeInteraction() = run {
    step("1. Interact with Android View") {
        LabelsScreen {
            addLabelFab.click()
        }
    }

    step("2. Interact with Compose dialog/view") {
        LabelDialogComposeScreen(composeTestRule) {
            inputField.performTextInput("New Label")
            confirmButton.performClick()
        }
    }

    step("3. Verify list update in View RecyclerView") {
        LabelsScreen {
            recyclerView {
                firstChild<LabelsScreen.LabelItemVH> {
                    labelText.hasText("New Label")
                }
            }
        }
    }
}
```

---

## Inheriting from `UiTestBase`

For tests requiring common setup, database instance, preferences, and permissions handling, extend `UiTestBase`.

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

### 1. Pre-filling Database / Mocking State via `runBlocking`

To seed the Room database with notes, labels, or preferences before launching an activity or rule, execute DAO operations inside `runBlocking` or within Kaspresso's `before { ... }` block.

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
> Always seed database or preference state **before** calling `ActivityScenario.launch(...)` or `createAndroidComposeRule`, so the Activity/Compose content reads the newly populated data on initial render.

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

---

## Custom Extension Functions (`UiUtils.kt`)

Avoid raw `onView(withId(...))` or verbose matchers when standard `UiUtils.kt` extension functions are available for legacy view matching.

> [!TIP]
> When writing new custom, reusable `ViewAction`s, `Matcher`s, or UI assertion helpers, place them directly into `UiUtils.kt` so they can be shared across all test files.

### View Locators & Navigation Extensions

| Extension | Usage | Description |
|---|---|---|
| `Int.byId(...)` | `R.id.MainListView.byId()` | Locates a view by resource ID. Checks if displayed by default (`checkDisplayed = true`). |
| `Int.byText(...)` | `R.string.save.byText()` | Locates a view matching string resource text. |
| `Int.byContentDescription(...)` | `R.string.pin.byContentDescription()` | Locates a view matching string resource content description. |
| `String.byText(...)` | `"Test".byText()` | Locates a view matching exact literal text. |
| `navigateTo(fragmentId)` | `navigateTo(R.id.Archived)` | Opens the navigation drawer and selects a destination. |

---

## Synchronization & Flakiness Management

### 1. Kaspresso Automatic Retry (`flakySafely`)

Kaspresso automatically wraps actions and assertions inside `flakySafely { ... }` during `step` calls, eliminating manual thread sleeps or custom polling loops for transient UI changes.

### 2. Compose Idle & Waiting Helpers

When waiting for coroutine emissions, animations, or Room flow updates in Compose:

```kotlin
// Wait for Compose recomposition & idle state
composeTestRule.waitForIdle()

// Wait until custom condition evaluates to true
composeTestRule.waitUntil(timeoutMillis = 5000L) {
    composeTestRule
        .onAllNodes(hasTestTag("SyncCompletedBanner"))
        .fetchSemanticsNodes().isNotEmpty()
}
```

### 3. Legacy Waiting Helpers (`waitUntilSucceeds`)

For legacy View flows, wrap assertions inside `waitUntilSucceeds`:

```kotlin
waitUntilSucceeds {
    R.id.MainListView.byId()
        .onPositionView(0, withId(R.id.Title))
        .check(matches(withText("Test")))
}
```

---

## Complete Test Templates

### Template 1: Pure Compose UI Test (`Kaspresso` + `ComposeTestRule`)

```kotlin
package com.philkes.notallyx.presentation.activity.main.fragments

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.screens.ComposeScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.createBaseNote
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 1. Define Page Object using ComposeScreen
object NotesComposeScreen : ComposeScreen<NotesComposeScreen>(
    semanticsProvider = null
) {
    val searchInput: KNode = child { hasTestTag("SearchInput") }
    val noteTitle: (String) -> KNode = { title -> child { hasText(title) } }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class NotesComposeTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun searchNotesWorkflow() = run {
        step("1. Seed test data") {
            runBlocking {
                database.getBaseNoteDao().insert(
                    listOf(createBaseNote(title = "Shopping List", body = "Milk, Eggs"))
                )
            }
        }

        step("2. Perform search in Compose UI") {
            NotesComposeScreen(composeTestRule) {
                searchInput.performTextInput("Shopping")
                noteTitle("Shopping List").assertIsDisplayed()
            }
        }
    }
}
```

### Template 2: Hybrid Activity UI Test (`Kakao` + `ComposeScreen`)

```kotlin
package com.philkes.notallyx.presentation.activity.edit

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.screens.ComposeScreen
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.test.UiTestBase
import com.philkes.notallyx.test.byContentDescription
import com.philkes.notallyx.test.byId
import com.philkes.notallyx.test.onPositionView
import com.philkes.notallyx.test.waitUntilSucceeds
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

object EditNoteComposeBar : ComposeScreen<EditNoteComposeBar>(semanticsProvider = null) {
    val pinButton: KNode = child { hasContentDescription("Pin Note") }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class EditNoteHybridTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createAndSaveNoteWorkflow() = run {
        step("1. Create new note") {
            R.id.TakeNote.byId().perform(click())
        }

        step("2. Enter title and body") {
            R.id.EnterTitle.byId().perform(typeText("New Note"), closeSoftKeyboard())
            R.id.EnterBody.byId().perform(typeText("New Body"), closeSoftKeyboard())
        }

        step("3. Interact with Compose bottom bar / menu") {
            EditNoteComposeBar(composeTestRule) {
                pinButton.performClick()
            }
        }

        step("4. Return to main list and verify") {
            toolbarBackButton.performClick()

            waitUntilSucceeds {
                R.id.MainListView.byId()
                    .onPositionView(0, withId(R.id.Title))
                    .check(matches(withText("New Note")))
            }
        }
    }
}
```

---

## Best Practices Checklist

- [ ] Used Kaspresso (`TestCase`, `ComposeScreen`, Kakao `Screen`) as the primary UI testing foundation.
- [ ] Added `kaspressoBuilder = Kaspresso.Builder.withComposeSupport()` for Compose-enabled tests.
- [ ] Structured UI elements using the Page Object pattern (`ComposeScreen` for Compose, `Screen` for Views).
- [ ] Preferred explicit `Modifier.testTag(...)` or semantic properties (`hasText`, `hasContentDescription`) over fragile layout node hierarchies.
- [ ] Wrapped database/preference seeding in `runBlocking { ... }` prior to `createAndroidComposeRule` or `ActivityScenario.launch(...)`.
- [ ] Placed any new reusable custom ViewActions, Matchers, or Compose test helpers in `UiUtils.kt`.
- [ ] Grouped test execution steps inside `step("Step description") { ... }` for clear logging and failure analysis.
- [ ] Avoided manual `Thread.sleep()`; relied on Kaspresso's `flakySafely` and `composeTestRule.waitForIdle()` for synchronization.
- [ ] Extended `UiTestBase` when tests require DB access, context, or permissions handling.
- [ ] Validated the added/changed tests by running affected tests via gradlew:
  ```shell
  ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.philkes.notallyx.presentation.activity.edit.EditActivityTest#createAndEditTextNote
  ```

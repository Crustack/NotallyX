---
name: evaluate-jetpack-composables
description: Evaluate existing Jetpack Compose Composables against official Android best practices. Checks Modifiers, Previews, State Hoisting, Function Complexity, Package Locality, and Performance optimizations. Use when reviewing Compose code, auditing `@Composable` functions, or refactoring UI components.
metadata:
   author: NotallyX
   last-updated: '2026-10-03'
   keywords:
      - Android
      - Jetpack Compose
      - Code Review
      - Best Practices
      - Modifiers
      - Previews
      - State Hoisting
      - Performance
      - Recomposition
      - DerivedStateOf
   scope: Jetpack Compose UI code (.kt files containing @Composable)
   primary-goal: Audit Jetpack Compose functions for design system adherence, performance, state hoisting, package structure, and preview completeness.
---

# Evaluate Jetpack Compose Composables Skill

## Purpose

Use this skill to perform comprehensive evaluations and audits of Jetpack Compose `@Composable` functions in a codebase. This skill analyzes Composable functions against official Android Compose recommendations, design system guidelines, state management rules, and performance best practices.

It leverages high-trust primary reference guides:
* [compose-best-practices.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-best-practices.md) - Performance, lazy keys, `derivedStateOf`, deferred state reads, and backwards write protection.
* [compose-state.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-state.md) - State management, `remember` / `rememberSaveable`, Unidirectional Data Flow (UDF), and state holders.

---

## Scope & Discovery Protocol

Before evaluating, determine the scope of analysis:

1. **Explicit User Context**:
   * If the user specifies explicit `@Composable` functions, files, or packages (e.g., "Check `NoteCard.kt`" or "Evaluate composables in `presentation/view/`"), **restrict analysis strictly to the provided files/context**.
2. **Implicit / Unspecified Context**:
   * If no explicit context is provided, automatically explore `@Composable` functions across the project using tools like `grep(pattern = "@Composable")` or `find_files(query = "*.kt")`. Focus primarily on modified or feature UI files.

---

## Core Evaluation Criteria

Each `@Composable` function under review must be evaluated across the following 7 core categories:

### 1. Modifiers Usage & Application

* **Optional Parameter**: Every public or reusable `@Composable` function MUST accept an optional `modifier: Modifier = Modifier` parameter as its first optional parameter.
* **Root Application**: The `modifier` parameter passed from the caller MUST be applied directly to the top-level / root layout element inside the Composable function.
* **Default Value**: The default value must strictly be `Modifier` (not custom or hardcoded modifiers like `Modifier.fillMaxWidth()`).
* **Modifier Chaining & Chunks**: Avoid applying the passed `modifier` parameter multiple times to different nested children. Concatenate internal layout properties onto `modifier` (e.g., `modifier.fillMaxWidth().padding(...)`) rather than replacing it.

```kotlin
// BAD - Missing modifier parameter, hardcoded root modifier
@Composable
fun NoteCard(title: String, body: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) { ... }
}

// GOOD - Accepts optional modifier and applies to root element
@Composable
fun NoteCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) { ... }
}
```

### 2. Compose Previews Completeness

* **Preview Definition**: Every UI `@Composable` MUST have at least one `@Preview` function defined in the same file or a dedicated preview file.
* **Parameter Utilization**: The `@Preview` function MUST pass mock or sample values for **every required parameter** expected by the Composable function.
* **Event Handlers**: Event callbacks and click listeners (e.g., `onClick: () -> Unit`) can be passed empty lambdas `{}` or logging callbacks in previews.
* **Theme & Context**: Previews should be wrapped in the application's Material Theme (e.g. `NotallyXTheme { ... }`) to ensure accurate rendering.
* **Multipreviews / Devices**: Provide multi-configuration previews (Light/Dark mode, Font scaling, or Screen sizes) for core design system components when relevant.

```kotlin
// GOOD - Comprehensive preview providing mock parameters
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NoteCardPreview() {
    NotallyXTheme {
        NoteCard(
            title = "Sample Title",
            body = "Sample note content body",
            onClick = {}
        )
    }
}
```

### 3. State Hoisting & Parameter Hygiene

Refer to [compose-state.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-state.md) for state hoisting principles.

* **Parameter Bloat & UI State Holders**: If a Composable accepts a large number of state parameters (e.g., >5 individual primitive/data fields), bundle them into a single Immutable UI State data class (e.g., `NoteCardUiState`).
* **Grouped Event Lambdas**: If a Composable takes many event callbacks, consider grouping them into an event interface or data class (e.g., `NoteCardEvents`).
* **Stateless vs. Stateful Separation**:
  * **Stateless Composable**: Pure UI component taking values (`state: T`) and emitting events (`onEvent: () -> Unit`). Highly reusable and previewable.
  * **Stateful Wrapper**: Optional container that collects ViewModel flows (`collectAsStateWithLifecycle()`) and passes values down to the stateless component.
* **Unidirectional Data Flow (UDF)**: Ensure state flows down and events flow up. Avoid passing raw ViewModel references or mutable state objects (`MutableState<T>`) into stateless leaf components.

```kotlin
// BAD - Parameter bloat & passing MutableState down
@Composable
fun EditNoteScreen(
    titleState: MutableState<String>,
    bodyState: MutableState<String>,
    isPinned: Boolean,
    isArchived: Boolean,
    folder: String,
    color: Int,
    // ... 10 more parameters
) { ... }

// GOOD - Bundled UI state & stateless/stateful separation
data class EditNoteUiState(
    val title: String = "",
    val body: String = "",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false
)

@Composable
fun EditNoteContent(
    uiState: EditNoteUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onPinToggle: () -> Unit,
    modifier: Modifier = Modifier
) { ... }
```

### 4. Function Complexity & Splitting Boundaries

* **Single Responsibility**: Check if large `@Composable` functions (e.g. >100 lines) contain multiple distinct UI sections (e.g., TopBar, ContentList, BottomBar, Dialogs).
* **Judicious Splitting**: Extract sub-sections into smaller private/internal helper `@Composable` functions **only when it improves readability, reusability, or recomposition scope**.
* **Avoid Over-Splitting**: Do not split simple layout blocks purely for the sake of creating smaller functions if it adds unnecessary boilerplate or obfuscates the layout hierarchy.

### 5. Package Structure & Component Locality

* **Global Design System**: Generic, reusable UI building blocks (e.g. custom buttons, dialogs, cards, input fields used across multiple features) belong in shared core packages (e.g., `presentation/components/`, `core/ui/`, or `designsystem/`).
* **Feature Locality**: Screen-specific or feature-specific composables must live inside their respective feature package (e.g., `presentation/activity/main/fragment/`), co-located with their screen code and ViewModel.
* **Visibility Scoping**: Feature-specific sub-composables and helper components should use `internal` or `private` visibility rather than polluting the public package namespace.

### 6. Performance & State Best Practices

Refer to [compose-best-practices.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-best-practices.md) for detailed performance patterns.

* **Expensive Calculations**: Ensure heavy operations inside composition body (e.g., sorting, filtering, formatting dates) are wrapped in `remember(key) { ... }` or calculated upstream in the ViewModel.
* **Lazy Layout Keys**: Ensure all `LazyColumn`, `LazyRow`, and `LazyVerticalGrid` `items` calls supply a stable, unique `key` parameter (`key = { item -> item.id }`).
* **Rapidly Changing State & `derivedStateOf`**: Use `derivedStateOf` when state changes frequently (e.g. scroll position) but UI recomposition is only needed when a derived condition/threshold changes.
* **Deferred State Reads & Lambda Modifiers**: Use lambda versions of modifiers (e.g., `Modifier.offset { IntOffset(...) }`, `Modifier.drawBehind { ... }`) when reading rapidly changing state to bypass composition/layout passes and execute directly in layout/draw phases.
* **No Backwards Writes**: Verify that state is never written/mutated directly inside the body of a Composable function during composition. State mutations must only happen inside event callbacks or side-effect handlers.
* **Non-Observable Collections**: Ensure mutable collections like `ArrayList` or `mutableListOf()` are NOT used as Compose state without observable wrappers (`mutableStateListOf()`, `State<List<T>>`).
* **Lifecycle-Aware Collection**: Ensure Kotlin Flows are collected safely in Android UI using `collectAsStateWithLifecycle()`.

---

## Review Output Format

When executing an evaluation, compile findings into a clear, structured Markdown report. Group all findings by severity and provide concrete code recommendations.

### Report Structure

```markdown
# Jetpack Compose Evaluation Report

**Target Scope**: `[Evaluated Files / Functions / Packages]`
**Date**: `[YYYY-MM-DD]`

---

## Executive Summary
- **Total Composables Audited**: `X`
- **Errors / Best Practice Violations**: `X`
- **Warnings**: `X`
- **Suggestions / Optimizations**: `X`

---

## 🚨 Errors / Best Practice Violations (High Severity)
*Critical issues impacting correctness, performance loops, missing modifiers, or severe UDF violations.*

### 1. `[ComposableName]` - `[File.kt:Line]`
- **Category**: `[Modifiers / Performance / State Hoisting / etc.]`
- **Issue**: Description of the violation.
- **Reference**: Link to [compose-best-practices.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-best-practices.md) or [compose-state.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-state.md).
- **Recommendation**:
```kotlin
// BEFORE
...

// AFTER
...
```

---

## ⚠️ Warnings (Medium Severity)
*Missing previews, parameter bloat, un-keyed lazy lists, or suboptimal package locality.*

### 1. `[ComposableName]` - `[File.kt:Line]`
- **Category**: `[Previews / Parameter Bloat / Package Structure]`
- **Issue**: Description of the issue.
- **Recommendation**: Actionable fix or code snippet.

---

## 💡 Suggestions & Optimizations (Low Severity)
*Minor formatting, function splitting opportunities, or optional performance enhancements.*

### 1. `[ComposableName]` - `[File.kt:Line]`
- **Category**: `[Code Hygiene / Splitting / DerivedStateOf]`
- **Issue**: Description.
- **Recommendation**: Suggestion.

---

## Next Steps & Action Plan
- [ ] Fix High Severity Errors
- [ ] Add missing Previews & Keys
- [ ] Refactor Parameter Bloat
```

---

## Storing Progress & Plan File

To ensure evaluation reports are persisted for tracking and iterative refactoring:
* Save the output report into `.agents/plans/` (e.g., `.agents/plans/compose-evaluation-report.md` or a user-specified path inside `.agents/plans/`).
* Inform the user of the saved report path and summarize key action items.

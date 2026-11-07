---
name: create-compose-ui-elements
description: Create and evaluate Jetpack Compose UI elements (screens, routes, components, and dialogs) according to official Android best practices and NotallyX design patterns. Use when creating new Compose UI elements, implementing screens or components, or auditing and refactoring existing @Composable functions.
metadata:
   author: NotallyX
   last-updated: '2026-10-26'
   keywords:
      - Android
      - Jetpack Compose
      - UI Development
      - UI Components
      - Code Review
      - Best Practices
      - Modifiers
      - Previews
      - State Hoisting
      - Performance
      - Recomposition
      - Navigation 3
   scope: Jetpack Compose UI code (.kt files containing @Composable)
   primary-goal: Guide the creation of new Jetpack Compose UI elements and audit existing composables for design system adherence, performance, state hoisting, package structure, and preview completeness.
---

# Jetpack Compose UI Elements: Creation & Evaluation Guide

## Purpose

Use this skill when **creating new Jetpack Compose UI elements** (screens, routes, reusable components, dialogs, bottom sheets) or when **evaluating, auditing, and refactoring** existing `@Composable` functions in NotallyX.

This guide standardizes:
1. **Creation Workflow**: Step-by-step process for authoring clean, idiomatic, and testable Compose elements.
2. **Templates & Patterns**: Canonical NotallyX templates for Routes, Screens, Components, and Dialogs.
3. **Core Standards Checklist**: The 7 golden rules for modifier hygiene, state hoisting, performance, theming, and previews.
4. **Audit Protocol**: Structured reporting format for evaluating and refactoring Compose code.

Primary reference guides:
* [compose-best-practices.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-best-practices.md) - Performance, lazy layout keys, `derivedStateOf`, deferred state reads, and backwards write avoidance.
* [compose-state.md](file:///home/user/AndroidStudioProjects/NotallyX/.agents/skills/evaluate-jetpack-composables/references/compose-state.md) - State management, `remember` / `rememberSaveable`, Unidirectional Data Flow (UDF), and state holders.

---

## Component Architecture in NotallyX

NotallyX organizes Compose UI into four distinct architectural layers:

```
┌────────────────────────────────────────────────────────┐
│ 1. Route (Stateful Container)                          │
│    Injects ViewModel via Koin, collects StateFlow,     │
│    binds callbacks, coordinates Navigation 3 backstack │
└───────────────────────────┬────────────────────────────┘
                            │ (State down, events up)
┌───────────────────────────▼────────────────────────────┐
│ 2. Screen (Stateless Content)                          │
│    Pure UI scaffold; receives immutable state & emits  │
│    events; fully previewable and unit-testable         │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│ 3. Reusable Components (Leaf Views)                    │
│    Cards, chips, rows, lists, buttons;                 │
│    accepts Modifier = Modifier, stateless              │
└────────────────────────────────────────────────────────┘
```

| Layer | Responsibility | Package Location | Example |
|---|---|---|---|
| **Route** | Stateful navigation target; ViewModel injection, `collectAsStateWithLifecycle` | `feature/<name>/routes/` or `feature/<name>/` | `NotesRoute.kt`, `SettingsRoute.kt` |
| **Screen** | Stateless screen layout with Scaffold, TopAppBar, scrollable content | `feature/<name>/` or `feature/<name>/screens/` | `MainScreen.kt`, `EditNoteScreen.kt` |
| **Component** | Reusable design system UI building block | `core/ui/components/` or `feature/<name>/` | `BaseNoteCard.kt`, `PreferenceRow.kt` |
| **Dialog / Sheet** | Modal overlay; accepts confirmation/dismiss lambdas | `core/ui/dialog/` or `feature/<name>/dialogs/` | `ConfirmDialog.kt`, `ActionModalBottomSheet.kt` |

---

## Step-by-Step Creation Workflow

When requested to create or implement a new UI element:

### Step 1: Determine Component Classification & Location
- If the element is a **top-level destination** in `MainActivity`: create a `Route` in `feature/main/routes/<name>/` and register its `NotallyNavKey` in `navigation/NotallyNavKey.kt` and `NotallyApp.kt`.
- If the element is a **screen or dialog specific to a feature**: place it directly within `feature/<name>/`.
- If the element is a **general-purpose UI primitive** (reusable across multiple features): place it in `core/ui/components/` or `core/ui/dialog/`.

### Step 2: Design State & Event Interface (UDF)
- Keep UI components **stateless** by hoisting state to the caller.
- If the component accepts more than 4-5 parameters, bundle them into a single Immutable data class (e.g. `[Name]UiState`).
- Expose explicit event callbacks (e.g. `onItemClick: (Item) -> Unit`, `onDismiss: () -> Unit`) instead of passing `MutableState` or ViewModel references.

### Step 3: Composable Signature & Modifier Hygiene
- **Mandatory Modifier Parameter**: Every public or reusable `@Composable` function MUST accept `modifier: Modifier = Modifier` as its first optional parameter.
- **Root Application**: Apply the passed `modifier` directly to the top-level layout container inside the function.
- **Default Value**: Never supply a pre-configured modifier (such as `Modifier.fillMaxWidth()`) as the parameter default value. Concatenate internal defaults inside the body if required (`modifier.fillMaxWidth()`).

```kotlin
// BAD
@Composable
fun NoteCard(title: String, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier = Modifier.padding(8.dp)) { ... } // Ignored caller modifier!
}

// GOOD
@Composable
fun NoteCard(
    title: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) { ... }
}
```

### Step 4: Material 3 Theming & String Localization
- **M3 Styling**: Use `MaterialTheme.colorScheme` and `MaterialTheme.typography`.
- **Theme Wrapper**: Use `NotallyXTheme` to support dynamic system colors, dark theme, and custom note colors.
- **Strictly No Hardcoded Strings**: Look up all user-facing text from Android string resources via `stringResource(R.string.*)`.

### Step 5: Performance & Optimization Rules
- **Lazy Layout Keys**: Always provide a stable unique `key` parameter to `items()` in `LazyColumn`, `LazyRow`, or `LazyVerticalGrid`:
  ```kotlin
  items(items = notes, key = { note -> note.id }) { note ->
      NoteCard(note = note)
  }
  ```
- **Derived State**: Use `derivedStateOf` for calculations that derive from rapidly changing states (e.g. scroll offsets, item thresholds) to prevent excessive recomposition.
- **Deferred Reads**: For values changing on every frame (animations, gestures), use lambda modifiers (`Modifier.offset { ... }`, `Modifier.drawBehind { ... }`) to bypass the composition phase.
- **Zero Backwards Writes**: Never mutate state variables inside the body of a `@Composable`. State writes belong strictly inside event lambdas or side effects.

### Step 6: Create Comprehensive Previews
- Every Composable MUST have at least one `@Preview(showBackground = true)` function.
- Provide light and dark mode variants wrapped in `NotallyXTheme`.
- For multi-locale app-store previews, use `@AppStorePreview` / `@LocalesPreview`.

```kotlin
@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeatureComponentPreview() {
    NotallyXTheme {
        FeatureComponent(
            state = FeatureUiState(title = "Sample Title", isSelected = true),
            onClick = {},
        )
    }
}
```

---

## Canonical NotallyX Templates

### 1. Stateless Screen & Stateful Route Template

*Use this when creating a new screen or navigation destination.*

```kotlin
package com.philkes.notallyx.feature.featurename

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import org.koin.androidx.compose.koinViewModel

// 1. Stateful Route Container
@Composable
fun FeatureRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeatureViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FeatureScreen(
        state = uiState,
        onAction = viewModel::performAction,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

// 2. Stateless Screen Content
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(
    state: FeatureUiState,
    onAction: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen content
        }
    }
}

// 3. Previews
@Preview(name = "Light Mode", showBackground = true)
@Composable
private fun FeatureScreenPreview() {
    NotallyXTheme {
        FeatureScreen(
            state = FeatureUiState(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
```

### 2. Reusable UI Component Template

*Use this when creating cards, list rows, chips, or inputs.*

```kotlin
package com.philkes.notallyx.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
fun CustomItemCard(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(name = "Card Preview", showBackground = true)
@Composable
private fun CustomItemCardPreview() {
    NotallyXTheme {
        CustomItemCard(
            title = "Item Title",
            subtitle = "Supporting item subtitle text",
            onClick = {},
        )
    }
}
```

### 3. Modal Dialog Template

*Use this when creating dialogs or bottom sheets.*

```kotlin
package com.philkes.notallyx.core.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = modifier,
    )
}

@Preview(name = "Dialog Preview", showBackground = true)
@Composable
private fun ConfirmActionDialogPreview() {
    NotallyXTheme {
        ConfirmActionDialog(
            title = "Delete Item",
            message = "Are you sure you want to delete this item?",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
```

---

## Evaluation & Audit Protocol

When evaluating existing or newly created `@Composable` functions, review against these 7 core criteria:

### The 7 Core Standards

1. **Modifiers**:
   - Accepts `modifier: Modifier = Modifier` as the first optional parameter.
   - Passed `modifier` is applied to the root element.
   - Never has hardcoded modifiers as default (e.g. `Modifier.fillMaxWidth()`).
2. **Previews**:
   - Contains `@Preview(showBackground = true)` wrapped in `NotallyXTheme`.
   - Passes sample data for every required parameter.
3. **State Hoisting**:
   - Stateless UI separated from stateful route/container.
   - State flows down, events flow up (UDF).
   - Bundles large parameter lists (>4-5 parameters) into UI state data classes.
   - Never passes `MutableState<T>` or raw ViewModels into reusable leaf components.
4. **Localization & Theming**:
   - All text comes from `stringResource(R.string.*)`. Zero hardcoded user-visible strings.
   - Colors and typography reference `MaterialTheme` / `NotallyXTheme`.
5. **Performance**:
   - Lazy layout items have unique, stable `key` expressions.
   - Heavy calculations wrapped in `remember(keys)`.
   - `derivedStateOf` used for rapid state change thresholds (e.g. scroll offsets).
   - Zero backwards writes during composition passes.
6. **Function Complexity & Locality**:
   - Large composables (>100 lines) decomposed into focused private helper composables.
   - Shared building blocks placed in `core/ui/components/` or `core/ui/dialog/`.
   - Feature-specific composables co-located in `feature/<name>/`.
7. **Lifecycle-Aware Observation**:
   - Flows collected via `collectAsStateWithLifecycle()` in Compose.

### Audit Report Structure

When performing a dedicated audit or code review, compile findings into this report format:

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
*Issues impacting correctness, performance loops, missing modifiers, or backwards writes.*

### 1. `[ComposableName]` - `[File.kt:Line]`
- **Category**: `[Modifiers / Performance / State Hoisting]`
- **Issue**: Description of violation.
- **Recommendation**:
```kotlin
// BEFORE
...

// AFTER
...
```

---

## ⚠️ Warnings (Medium Severity)
*Missing previews, parameter bloat, un-keyed lazy lists, or hardcoded strings.*

### 1. `[ComposableName]` - `[File.kt:Line]`
- **Category**: `[Previews / Keys / Localization]`
- **Issue**: Description.
- **Recommendation**: Suggested fix.

---

## 💡 Suggestions & Optimizations (Low Severity)
*Opportunities for derivedStateOf, deferred state reads, or function splitting.*

---

## Next Steps
- [ ] Fix high-severity issues
- [ ] Add missing previews and lazy layout keys
```

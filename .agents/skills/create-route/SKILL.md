---
name: create-route
description: Best practices, architecture, and canonical templates for creating new Routes and feature destinations in NotallyX following MVI / Unidirectional Data Flow (Model, ViewModel, Screen, Route). Use when creating a new Route or navigation destination, refactoring an existing screen/route to MVI, or structuring feature UI state, intent-based actions, side effects, and activity launchers.
metadata:
  author: NotallyX
  last-updated: '2026-10-30'
  keywords:
    - Android
    - Jetpack Compose
    - Navigation 3
    - MVI
    - Unidirectional Data Flow
    - ViewModel
    - Route
    - Screen
    - Side Effects
    - Channel
    - StateFlow
    - Koin
---

# Creating New Routes & Destinations in NotallyX

## Purpose

Use this skill when **creating a new navigation Route / screen destination** or **refactoring existing routes** in NotallyX.

This guide codifies the **4-File Feature Quad** established during the modernization of `SettingsRoute`, enforcing **Unidirectional Data Flow (MVI)**, strict separation of concerns, testability, and zero business or framework logic inside Compose UI elements.

---

## The 4-File Feature Quad

Every route or navigation destination in NotallyX is divided into four cleanly separated files within the feature package:

```
feature/<feature_name>/
├── <Feature>Model.kt         # 1. UI Contract: UiState, Action, SideEffect, DialogState
├── <Feature>ViewModel.kt     # 2. Logic & State Holder: StateFlow, Channel, onAction()
├── <Feature>Screen.kt        # 3. Stateless UI: Pure Composable, Scaffold, Previews
└── routes/<Feature>Route.kt   # 4. Stateful Coordinator: Koin injection, launchers, side effects
```

> [!NOTE]
> Top-level destinations hosted in `MainActivity` place the Route file in `feature/main/routes/<feature_name>/<Feature>Route.kt` (e.g. `SettingsRoute.kt`), while feature-internal routes reside under `feature/<feature_name>/routes/`.

### Responsibilities Matrix

| File | Type | Primary Responsibilities | What It Must NOT Do |
|---|---|---|---|
| **`<Feature>Model.kt`** | Data / Interfaces | Defines `UiState`, `Action` (intents), `SideEffect` (one-off events), and `DialogState`. | No Compose UI code, no Android `View`/`Activity` dependencies. |
| **`<Feature>ViewModel.kt`** | `AndroidViewModel` / `ViewModel` | Manages business logic, room queries, preferences, coroutines, combines `uiState: StateFlow`, emits `sideEffects: Flow`, processes `onAction(action)`. | No UI layouts, no `ContextWrapper` traversal, no `mutableStateOf`, no `FragmentActivity` references. |
| **`<Feature>Screen.kt`** | Stateless `@Composable` | Pure presentation: Scaffold, top bar, list/grid, dialogs, previews. Accepts `(state, onAction, onNavigateBack, modifier)`. | Never injects ViewModel, never collects flows, no `remember { mutableStateOf }` for business state, no launchers. |
| **`<Feature>Route.kt`** | Stateful `@Composable` | Glues ViewModel to Screen: injects via `koinViewModel()`, collects `uiState`, collects `sideEffects` in `LaunchedEffect`, sets up `rememberLauncherForActivityResult`, invokes `<Feature>Screen`. | No business logic, no file parsing, no complex UI rendering (delegate to Screen), no sprawling lambda bags. |

---

## Architectural Principles (MVI / UDF)

```
           ┌──────────────────────────────────────────────┐
           │              <Feature>Route                  │
           │  - Injects ViewModel via Koin                │
           │  - Collects uiState with Lifecycle           │
           │  - Collects sideEffects in LaunchedEffect    │
           │  - Hosts ActivityResultLaunchers             │
           └──────────────┬───────────────────────────────┘
                          │ passes uiState & onAction
                          ▼
           ┌──────────────────────────────────────────────┐
           │              <Feature>Screen                 │
           │  - Stateless Scaffold & Content UI           │
           │  - Renders dialogs from state.activeDialog   │
           │  - Dispatches onAction(FeatureAction)        │
           └──────────────┬───────────────────────────────┘
                          │ dispatches user intents
                          │ onAction(Action)
                          ▼
           ┌──────────────────────────────────────────────┐
           │             <Feature>ViewModel               │
           │  - Combines DAOs, Prefs, Progress            │
           │  - Exposes uiState: StateFlow<UiState>       │
           │  - Emits sideEffects via Channel             │
           │  - Coordinates multi-step state machines     │
           └──────────────────────────────────────────────┘
```

### 1. Unified UI State
- Consolidate all independent state streams (preferences, Room query flows, active dialogs, import/export progress, loading indicators) into a single immutable data class: `StateFlow<<Feature>UiState>`.
- Use `combine(...)` inside the ViewModel and expose via `stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = ...)`.
- Eliminate raw `remember { mutableStateOf(...) }` inside composables for business state.

### 2. Intent-Based Actions
- Replace sprawling, unstable lambda callback bags (e.g. `FeatureActions(onA = {}, onB = {}, onC = {})`) with a single sealed interface `<Feature>Action`.
- The screen exposes a single event sink: `onAction: (<Feature>Action) -> Unit`.
- The ViewModel handles all intents in a single centralized `fun onAction(action: <Feature>Action)` method.

### 3. Decouple Framework Logic & Side Effects
- One-off events (Biometric authentication, Toasts, opening URLs, launching external intents, database transitions) must be emitted from ViewModel via a `Channel<<Feature>SideEffect>(Channel.BUFFERED)` and exposed as `Flow<<Feature>SideEffect>`.
- The Route collects `sideEffects` within `LaunchedEffect(Unit)` and executes the Android framework actions (e.g., `context.showToast()`, `context.startActivity()`).
- Never cast `Context` in a while-loop (`while (context is ContextWrapper)`) to find an Activity. Use `LocalActivity.current as? FragmentActivity`.

### 4. Explicit Workflow State Machines (No Transient Closures)
- Never store pending closures (`pendingBiometricContinuation: (() -> Unit)?`) in Compose `remember` state to handle multi-step flows (e.g. export backup -> prompt biometric auth -> encrypt db).
- Model multi-step flows as explicit state transitions inside the ViewModel using a private enum (e.g. `BiometricFlowState { IDLE, ENABLE_PENDING_EXPORT, DISABLE_PENDING_EXPORT }`).

### 5. Activity Result Launchers
- `rememberLauncherForActivityResult` instances in the Route must do only one thing: forward the selected URI or result directly to `viewModel.onAction(<Feature>Action.ResultReceived(uri))`.
- File creation titles, MIME types, and document queries are calculated in the ViewModel and provided via `SideEffect` parameters.

---

## Canonical Code Templates

### 1. `<Feature>Model.kt`

```kotlin
package com.philkes.notallyx.feature.example

import android.net.Uri
import androidx.annotation.StringRes
import com.philkes.notallyx.data.model.BaseNote

// 1. Immutable Unified State
data class ExampleUiState(
    val items: List<BaseNote> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val activeDialog: ExampleDialogState? = null,
)

// 2. Dialog / Modal States
sealed interface ExampleDialogState {
    data class ConfirmDelete(val noteId: Long, val noteTitle: String) : ExampleDialogState
    data class EditTitle(val noteId: Long, val currentTitle: String) : ExampleDialogState
}

// 3. User Intents (Actions)
sealed interface ExampleAction {
    data class SearchQueryChanged(val query: String) : ExampleAction
    data class NoteClicked(val noteId: Long) : ExampleAction
    data class RequestDeleteNote(val noteId: Long, val noteTitle: String) : ExampleAction
    data object ConfirmDeleteNote : ExampleAction
    data object DismissDialog : ExampleAction
    data object RequestExportNotes : ExampleAction
    data class ExportNotesConfirmed(val destinationUri: Uri) : ExampleAction
}

// 4. One-Off Side Effects
sealed interface ExampleSideEffect {
    data class ShowToast(@StringRes val messageRes: Int) : ExampleSideEffect
    data class NavigateToEditor(val noteId: Long) : ExampleSideEffect
    data class LaunchExportFile(val filename: String) : ExampleSideEffect
    data class OpenBrowser(val url: String) : ExampleSideEffect
}
```

---

### 2. `<Feature>ViewModel.kt`

```kotlin
package com.philkes.notallyx.feature.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.philkes.notallyx.R
import com.philkes.notallyx.data.dao.BaseNoteDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExampleViewModel(
    private val app: Application,
    private val baseNoteDao: BaseNoteDao,
) : AndroidViewModel(app) {

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _activeDialog = MutableStateFlow<ExampleDialogState?>(null)

    // One-off Side Effect Channel
    private val _sideEffects = Channel<ExampleSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ExampleSideEffect> = _sideEffects.receiveAsFlow()

    // Unified StateFlow
    val uiState: StateFlow<ExampleUiState> =
        combine(
            baseNoteDao.getAllNotes(),
            _searchQuery,
            _isLoading,
            _activeDialog,
        ) { notes, query, loading, dialog ->
            val filtered = if (query.isBlank()) notes else notes.filter { it.title.contains(query, ignoreCase = true) }
            ExampleUiState(
                items = filtered,
                searchQuery = query,
                isLoading = loading,
                activeDialog = dialog,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ExampleUiState(isLoading = true),
        )

    private fun emitSideEffect(effect: ExampleSideEffect) {
        viewModelScope.launch { _sideEffects.send(effect) }
    }

    // Central Action Dispatcher
    fun onAction(action: ExampleAction) {
        when (action) {
            is ExampleAction.SearchQueryChanged -> {
                _searchQuery.update { action.query }
            }

            is ExampleAction.NoteClicked -> {
                emitSideEffect(ExampleSideEffect.NavigateToEditor(action.noteId))
            }

            is ExampleAction.RequestDeleteNote -> {
                _activeDialog.update { ExampleDialogState.ConfirmDelete(action.noteId, action.noteTitle) }
            }

            is ExampleAction.ConfirmDeleteNote -> {
                val dialog = _activeDialog.value as? ExampleDialogState.ConfirmDelete ?: return
                _activeDialog.update { null }
                deleteNote(dialog.noteId)
            }

            is ExampleAction.DismissDialog -> {
                _activeDialog.update { null }
            }

            is ExampleAction.RequestExportNotes -> {
                val filename = "NotallyX_Export_${System.currentTimeMillis()}.txt"
                emitSideEffect(ExampleSideEffect.LaunchExportFile(filename))
            }

            is ExampleAction.ExportNotesConfirmed -> {
                exportNotesToUri(action.destinationUri)
            }
        }
    }

    private fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            baseNoteDao.deleteById(noteId)
            emitSideEffect(ExampleSideEffect.ShowToast(R.string.deleted))
        }
    }

    private fun exportNotesToUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.update { true }
            try {
                // Execute export on Dispatchers.IO
                emitSideEffect(ExampleSideEffect.ShowToast(R.string.saved))
            } catch (e: Exception) {
                emitSideEffect(ExampleSideEffect.ShowToast(R.string.error))
            } finally {
                _isLoading.update { false }
            }
        }
    }
}
```

---

### 3. `<Feature>Screen.kt`

```kotlin
package com.philkes.notallyx.feature.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.theme.NotallyXTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleScreen(
    state: ExampleUiState,
    onAction: (ExampleAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
                .padding(innerPadding),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.items, key = { it.id }) { note ->
                        // Render note item
                    }
                }
            }

            // Dialogs rendered directly from state.activeDialog
            when (val dialog = state.activeDialog) {
                is ExampleDialogState.ConfirmDelete -> {
                    AlertDialog(
                        onDismissRequest = { onAction(ExampleAction.DismissDialog) },
                        title = { Text(stringResource(R.string.delete)) },
                        text = { Text(dialog.noteTitle) },
                        confirmButton = {
                            TextButton(onClick = { onAction(ExampleAction.ConfirmDeleteNote) }) {
                                Text(stringResource(R.string.delete))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { onAction(ExampleAction.DismissDialog) }) {
                                Text(stringResource(R.string.cancel))
                            }
                        },
                    )
                }
                is ExampleDialogState.EditTitle -> { /* Render title dialog */ }
                null -> Unit
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true)
@Composable
private fun ExampleScreenPreview() {
    NotallyXTheme {
        ExampleScreen(
            state = ExampleUiState(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
```

---

### 4. `<Feature>Route.kt`

```kotlin
package com.philkes.notallyx.feature.main.routes.example

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.common.showToast
import com.philkes.notallyx.core.common.wrapWithChooser
import com.philkes.notallyx.feature.example.ExampleAction
import com.philkes.notallyx.feature.example.ExampleScreen
import com.philkes.notallyx.feature.example.ExampleSideEffect
import com.philkes.notallyx.feature.example.ExampleViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExampleRoute(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExampleViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Activity Result Launcher forwards output directly to onAction
    val exportFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.onAction(ExampleAction.ExportNotesConfirmed(uri))
                }
            }
        }

    // Collect Side Effects in LaunchedEffect
    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is ExampleSideEffect.ShowToast -> {
                    context.showToast(effect.messageRes)
                }

                is ExampleSideEffect.NavigateToEditor -> {
                    onNavigateToEditor(effect.noteId)
                }

                is ExampleSideEffect.LaunchExportFile -> {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TITLE, effect.filename)
                    }.wrapWithChooser(context)
                    exportFileLauncher.launch(intent)
                }

                is ExampleSideEffect.OpenBrowser -> {
                    val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri()).wrapWithChooser(context)
                    context.startActivity(intent)
                }
            }
        }
    }

    // Bind stateless screen
    ExampleScreen(
        state = uiState,
        modifier = modifier,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}
```

---

## Wiring Up a New Route in NotallyX

### 1. Navigation Key (`navigation/NotallyNavKey.kt`)
Add a `@Serializable` key implementing `NotallyNavKey`:
```kotlin
@Serializable
data object Example : NotallyNavKey
```

### 2. Dependency Injection (`di/FeatureModule.kt`)
Register the ViewModel using Koin's `viewModelOf`:
```kotlin
viewModelOf(::ExampleViewModel)
```

### 3. Navigation Display (`feature/main/NotallyApp.kt`)
Register the destination in `NavDisplay`'s `entryProvider`:
```kotlin
entry<NotallyNavKey.Example> {
    ExampleRoute(
        onNavigateBack = { navBackStack.pop() },
        onNavigateToEditor = { noteId -> navBackStack.push(NotallyNavKey.Editor(noteId)) },
    )
}
```

---

## Testing Strategy for Routes

### 1. ViewModel Unit Tests (`app/src/test/`)
Test state transitions and side effects without Android framework dependencies:
```kotlin
@Test
fun `when search query action dispatched then uiState is updated`() = runTest {
    val viewModel = ExampleViewModel(app, baseNoteDao)
    viewModel.onAction(ExampleAction.SearchQueryChanged("groceries"))

    assertEquals("groceries", viewModel.uiState.value.searchQuery)
}

@Test
fun `when note clicked then navigate side effect emitted`() = runTest {
    val viewModel = ExampleViewModel(app, baseNoteDao)
    viewModel.sideEffects.test {
        viewModel.onAction(ExampleAction.NoteClicked(123L))
        val effect = awaitItem()
        assertTrue(effect is ExampleSideEffect.NavigateToEditor)
        assertEquals(123L, (effect as ExampleSideEffect.NavigateToEditor).noteId)
    }
}
```

### 2. Compose Previews & Screen Tests
- Ensure `@Preview` renders cleanly with mock `ExampleUiState`.
- Roborazzi screenshot tests in `app/src/test/` verify pixel fidelity across theme changes.

### 3. Instrumented Route Tests (`app/src/androidTest/`)
Extend `UiTestBase` and interact exclusively through Compose semantics:
```kotlin
@Test
fun testExampleRoute_displaysNotes() {
    composeTestRule.setContent {
        ExampleRoute(onNavigateBack = {}, onNavigateToEditor = {})
    }
    composeTestRule.onNodeWithText("Sample Note").assertIsDisplayed()
}
```

---

## Anti-Patterns & Code Smells to Avoid

- ❌ **The "Lambda Bag" Antipattern**: Passing 15 separate lambdas (`onThemeChange`, `onNotesSort`, `onBackupClick`, etc.) through layers. **Fix**: Use single `onAction: (Action) -> Unit`.
- ❌ **`ContextWrapper` While-Loops**: Walking `var current = context; while (current is ContextWrapper)...` in composables to find an Activity. **Fix**: Use `LocalActivity.current as? FragmentActivity`.
- ❌ **Transient Closures in `remember`**: Storing `var pendingAction: (() -> Unit)? by remember { mutableStateOf(null) }` for multi-step dialogs or authentication. **Fix**: Model workflow state transitions inside ViewModel.
- ❌ **Business Logic in Compose**: Naming backup files with `SimpleDateFormat`, parsing JSON preferences, or triggering `WorkManager` inside composables. **Fix**: Keep all IO and background scheduling in ViewModel.
- ❌ **State Hoisting Failure**: Declaring `val showDialog = remember { mutableStateOf(false) }` inside the Composable. **Fix**: Hoist dialog visibility to `state.activeDialog` in the ViewModel.
- ❌ **Blocking Dispatchers.Main**: Executing database calls or file writes synchronously inside ViewModel. **Fix**: Always dispatch heavy operations to `Dispatchers.IO`.
- ❌ **Hardcoded Strings**: Writing raw string literals in UI. **Fix**: Always use `stringResource(R.string.*)`.

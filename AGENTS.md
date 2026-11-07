# AGENTS.md - NotallyX Development Guidelines for AI Agents

## Project Overview & Purpose

**NotallyX** is an open-source, minimalistic, yet feature-rich note-taking Android application built with Kotlin. It provides users with a distraction-free experience for creating text notes, task lists, rich-text formatting, media attachments (images, audio, files), labels, reminders, and encrypted backups.

- **Target Platform**: Android (Min SDK: 24, Compile/Target SDK: 37, JVM Target: 17).
- **Core Architecture**: Feature-first MVI (Model-View-Intent) / Unidirectional Data Flow architecture backed by Android Jetpack components (100% Jetpack Compose, Material 3, Adaptive multi-pane, Room 3, Kotlin Coroutines Flow/StateFlow, ViewModel, Navigation 3, WorkManager), Jetpack DataStore with Google Tink AEAD encryption, Coil 3, Jetpack Media3, Koin 4.x Dependency Injection with Koin Compiler Plugin, and Jetpack Glance widgets.
- **Key Tenets**: User privacy, offline-first reliability, non-destructive data handling, robust backup/restore mechanisms, and clean Kotlin idioms.

---

## Tech Stack & Conventions

### Language & Tooling
- **Language**: Kotlin 2.4.20 (Standard Kotlin Idioms, Coroutines, Serialization).
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin 9.4.1, KSP (`com.google.devtools.ksp`) for Room 3 compiler.
- **Dependency Injection**: Koin 4.x (`io.insert-koin`) with Koin Compiler Plugin (`io.insert-koin.compiler.plugin`).
- **Code Formatter**: `ktfmt` with Kotlin standard style (`kotlinLangStyle`).

### UI Framework & Conventions
- **UI Toolkit**: Pure Jetpack Compose with Material 3 (`androidx.compose.material3`). 100% Compose UI across all activities, screens, bottom sheets, and dialogs (zero ViewBinding / XML layouts).
- **Theming**: `NotallyXTheme` supporting dynamic system colors, dark mode, and custom note colors.
- **Navigation**: Jetpack Navigation 3 (`androidx.navigation3.runtime`, `androidx.navigation3.ui`) using type-safe `@Serializable` `NotallyNavKey` entries with `rememberNavBackStack`, `NavDisplay`, `entryProvider`, and adaptive two-pane list-detail support (`ListDetailScene`).
- **Lists & Grids**: Compose `LazyColumn` and `LazyVerticalGrid` / `LazyVerticalStaggeredGrid`. Reordering and gestures use custom Compose drag-and-drop and swipe handlers.
- **Media & Images**: Coil 3 (`coil.compose`) for asynchronous image loading and decoding; Jetpack Media3 (`androidx.media3.exoplayer`, `androidx.media3.session`, `AudioPlayService`) for audio recording and playback.
- **App Widgets**: Jetpack Glance (`androidx.glance.appwidget`, `NoteWidget.kt`).
- **Security & Locks**: `LockedActivity` extends `AppCompatActivity` for biometric authentication and secure screen locking.

### State Management & Concurrency
- **Feature Architecture**: Unidirectional Data Flow (MVI). Destinations implement the **4-File Feature Quad**:
  - `<Feature>Model.kt`: Immutable `UiState`, sealed `Action` (intents), `SideEffect` (one-off events), and `DialogState`.
  - `<Feature>ViewModel.kt`: Central state holder exposing `uiState: StateFlow<UiState>` and `sideEffects: Flow<SideEffect>`, dispatching all intents through `onAction(action)`.
  - `<Feature>Screen.kt`: Stateless `@Composable` rendering the UI state, dispatching events through `onAction: (Action) -> Unit`, with `@Preview` functions.
  - `<Feature>Route.kt`: Stateful coordinator Composable injecting ViewModel via `koinViewModel()`, collecting `uiState` with lifecycle, handling `sideEffects`, and hosting activity result launchers.
- **UI Observation**: Lifecycle-aware collection using `collectAsStateWithLifecycle()` in composables, or `repeatOnLifecycleScope` in Activities.
- **Async & Concurrency**: Kotlin Coroutines (`viewModelScope`, `CoroutineScope`).
- **Dispatchers**:
  - `Dispatchers.Main`: UI interactions and StateFlow emissions.
  - `Dispatchers.IO`: Room database operations, DataStore read/writes, disk I/O, file exports/imports, compression, encryption.
  - `Dispatchers.Default`: Intensive CPU calculations (e.g. search occurrences, text delta calculations).

### Database & Storage Patterns
- **Database**: Room 3 Database (`NotallyDatabase`, versioned with KSP schema exports in `app/schemas/`). Uses Room SQLite Driver (`BackupSQLiteDriver`), `@ColumnTypeConverters(Converters::class)`, and `RoomRawQuery`.
- **Migrations**: Incremental Room schema migrations defined as `Migration(from, to)` in `NotallyDatabase.kt`. High-level application data migrations (attachment relocation, note splitting) in `core/database/DataSchemaMigrations.kt`.
- **Preferences & Encryption**:
  - General preferences: Jetpack DataStore (`androidx.datastore.preferences`, `UserPreferences`).
  - Sensitive preferences: Encrypted DataStore (`androidx.datastore.tink`) using Google Tink AEAD (`SecurePreferences`).
  - Database encryption: Optional SQLCipher database encryption (`net.zetetic:sqlcipher-android`) and biometric database lock.
- **Storage Strategy**:
  - Internal storage for private app data and default SQLite database.
  - Optional external storage mode (`dataInPublicFolder`).
  - Strict WAL checkpointing: `NotallyDatabase.checkpoint()` executes `RoomRawQuery("pragma wal_checkpoint(TRUNCATE)")` to ensure WAL is flushed before any database export, backup, or replacement.
- **Type Converters**: `core/database/Converters.kt` converts complex models (spans, attachments, folders, reminders, labels) to/from JSON strings.

### App Documentation
- **Docusaurus Website**: App user and developer documentation is located in `@documentation` (`/documentation`). It is a Docusaurus website containing feature guides, setup instructions, FAQs, settings, security & privacy details, and contribution docs.

---

## Repository Directory Map

```
NotallyX/
├── app/
│   ├── schemas/                                       # Room schema exports
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml                    # Manifest, permissions, activities, receivers, services
│   │   │   ├── java/com/philkes/notallyx/
│   │   │   │   ├── NotallyXApplication.kt             # Application entry point & Koin initialization
│   │   │   │   ├── core/                              # Core domain & infrastructure
│   │   │   │   │   ├── common/                        # Extensions, results (Progress), error screens, BaseViewModel
│   │   │   │   │   ├── database/                      # Room 3 DB (NotallyDatabase), SQLite drivers, migrations, converters
│   │   │   │   │   ├── preference/                    # NotallyXPreferences, DataStore models (UserPreferences, SecurePreferences)
│   │   │   │   │   ├── security/                      # Biometrics, SQLCipher, Tink AEAD, LockedActivity
│   │   │   │   │   └── ui/                            # Compose theme, BaseNoteCard, dialogs, UI components
│   │   │   │   ├── data/                              # Data layer
│   │   │   │   │   ├── dao/                           # Room 3 DAOs (BaseNoteDao, LabelDao, CommonDao)
│   │   │   │   │   └── model/                         # Entities & models (BaseNote, Label, Folder, Item)
│   │   │   │   ├── di/                                # Koin modules (AppModule, FeatureModule, DatabaseModule, PreferencesModule)
│   │   │   │   ├── feature/                           # Feature-first MVI modules
│   │   │   │   │   ├── editor/                        # Note & list editing (EditNoteRoute, EditListRoute, EditTextPlainRoute, EditScreen)
│   │   │   │   │   ├── labels/                        # Labels screen, selector, viewmodels, models, routes
│   │   │   │   │   ├── main/                          # MainActivity, NotallyApp, routes/ (notes, archived, deleted, search, picknote)
│   │   │   │   │   ├── media/                         # Audio recording/playback (AudioPlayService), image viewer (ViewImageRoute)
│   │   │   │   │   ├── migration/                     # Backup, restore, exporters, external importers (Keep, Evernote, etc.)
│   │   │   │   │   ├── reminder/                      # Reminders screen, notifications, alarm receivers, routes
│   │   │   │   │   ├── settings/                      # Settings screen, preference rows, dialogs, route, viewmodel
│   │   │   │   │   └── widget/                        # Jetpack Glance widgets (NoteWidget)
│   │   │   │   └── navigation/                        # Navigation 3 keys (NotallyNavKey), Navigator, ListDetailScene
│   │   │   └── res/                                   # Resources (strings, drawables, XML descriptors)
│   │   ├── test/                                      # Unit & Robolectric test suite, Roborazzi screenshot tests
│   │   ├── androidTest/                               # Instrumented UI tests extending UiTestBase
│   │   └── screenshotTestDebug/                       # Compose Screenshot Testing previews
│   └── build.gradle.kts                               # App build configurations & dependencies
├── test-lib/                                          # Shared test utilities (UiTestBase, UiUtils, Fakes)
├── documentation/                                     # Docusaurus website for app documentation (@documentation)
├── build.gradle.kts                                   # Root build configuration
└── settings.gradle.kts                                # Project & repository settings
```

---

## Agent Guidelines & Constraints

### Do's
- **Compose First**: Build user interfaces with Jetpack Compose composables and Material 3. When creating new Compose UI elements or reviewing existing ones, consult [.agents/skills/create-compose-ui-elements/SKILL.md](.agents/skills/create-compose-ui-elements/SKILL.md) and [.agents/skills/evaluate-jetpack-composables/SKILL.md](.agents/skills/evaluate-jetpack-composables/SKILL.md).
- **MVI Feature Quad**: Structure feature destinations using the **4-File Feature Quad** (`Model.kt`, `ViewModel.kt`, `Screen.kt`, `Route.kt`) following [.agents/skills/create-route/SKILL.md](.agents/skills/create-route/SKILL.md).
- **Single Action Sink**: Consolidate UI callbacks into a single `onAction: (Action) -> Unit` event sink in the Screen and handle it centrally in `viewModel.onAction(action)`.
- **One-Off Side Effects**: Emit navigation, toasts, and launcher events through a `Channel<SideEffect>` in the ViewModel and collect them in `LaunchedEffect` in the Route.
- **State Hoisting**: Keep screen composables stateless and testable by hoisting all state into an immutable `UiState` data class managed by the ViewModel.
- **Use Navigation 3**: Manage destinations with type-safe `@Serializable` `NotallyNavKey` entries in `NavDisplay`. Support adaptive split-screen layouts using `ListDetailScene`.
- **Preserve Data Integrity**: Always safeguard user notes. Never drop tables destructively without valid Room 3 migration paths.
- **Run Checkpoints Before DB Copy**: When exporting, backing up, or copying SQLite files, call `database.checkpoint()` to ensure the WAL is flushed.
- **Offload Heavy Work**: Perform file I/O, DataStore reads/writes, parsing (JSON/Markdown/HTML), and database queries on `Dispatchers.IO`.
- **Format with ktfmt**: Ensure Kotlin files comply with `ktfmt` standard style (`./gradlew ktfmtFormat`).
- **Follow Existing Patterns**: Match existing naming conventions (`*Route`, `*Screen`, `*ViewModel`, `*Model`, `*Dao`, `*NavKey`).
- **Write Unit, UI & Screenshot Tests**: Add unit tests under `app/src/test/`, instrumented tests under `app/src/androidTest/`, and Roborazzi / Compose screenshot tests whenever introducing features, migration steps, or UI changes.

### Don'ts
- **NO XML Layouts**: Build all new UI screens, dialogs, and components with Jetpack Compose.
- **NO Direct State Mutation in UI Tests**: Interact exclusively through Compose semantics (`performClick()`, `performTextInput()`) or Espresso actions. Never directly assign activity/viewmodel state fields in UI tests.
- **NO Blocking Main Thread**: Offload synchronous database queries, heavy regex, DataStore writes, or file operations to `Dispatchers.IO`.
- **NO Raw SQL Injections**: Use Room DAO annotations (`@Query`, `@Insert`, `@Update`, `@Delete`) or properly parameterized `RoomRawQuery`.
- **NO Hardcoded Strings**: Use string resources (`R.string.*`) for all UI-visible text to preserve localization support across 30+ languages.
- **NO Lambda Bags**: Avoid passing multiple individual lambda callbacks through screen composable hierarchies; use a single `onAction: (Action) -> Unit`.
- **NO Context Casting While-Loops**: Avoid traversing `while (context is ContextWrapper)` to find an Activity. Use `LocalActivity.current as? FragmentActivity`.
- **NO Transient Closures in `remember`**: Never store pending action closures in Compose `remember` state across dialogs or authentication. Model workflow state transitions explicitly in the ViewModel.

---

## Common Tasks & Workflows

### 1. Adding a New Screen / Feature (4-File Feature Quad)
> [!TIP]
> When creating any new feature destination or route in NotallyX, strictly follow the templates and guidelines in [.agents/skills/create-route/SKILL.md](.agents/skills/create-route/SKILL.md) and [.agents/skills/create-compose-ui-elements/SKILL.md](.agents/skills/create-compose-ui-elements/SKILL.md).

Every new destination consists of four cleanly separated files within the feature package:

#### a. `<Feature>Model.kt` (UI Contract)
```kotlin
package com.philkes.notallyx.feature.example

import androidx.annotation.StringRes
import com.philkes.notallyx.data.model.BaseNote

data class ExampleUiState(
    val items: List<BaseNote> = emptyList(),
    val isLoading: Boolean = false,
    val activeDialog: ExampleDialogState? = null,
)

sealed interface ExampleDialogState {
    data class ConfirmDelete(val noteId: Long, val noteTitle: String) : ExampleDialogState
}

sealed interface ExampleAction {
    data class NoteClicked(val noteId: Long) : ExampleAction
    data class RequestDeleteNote(val noteId: Long, val noteTitle: String) : ExampleAction
    data object ConfirmDeleteNote : ExampleAction
    data object DismissDialog : ExampleAction
}

sealed interface ExampleSideEffect {
    data class ShowToast(@StringRes val messageRes: Int) : ExampleSideEffect
    data class NavigateToEditor(val noteId: Long) : ExampleSideEffect
}
```

#### b. `<Feature>ViewModel.kt` (State Holder & Logic)
```kotlin
package com.philkes.notallyx.feature.example

import androidx.lifecycle.ViewModel
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

class ExampleViewModel(
    private val baseNoteDao: BaseNoteDao,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _activeDialog = MutableStateFlow<ExampleDialogState?>(null)
    private val _sideEffects = Channel<ExampleSideEffect>(Channel.BUFFERED)
    val sideEffects: Flow<ExampleSideEffect> = _sideEffects.receiveAsFlow()

    val uiState: StateFlow<ExampleUiState> =
        combine(
            baseNoteDao.getAllNotes(),
            _isLoading,
            _activeDialog,
        ) { notes, loading, dialog ->
            ExampleUiState(items = notes, isLoading = loading, activeDialog = dialog)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ExampleUiState(isLoading = true),
        )

    fun onAction(action: ExampleAction) {
        when (action) {
            is ExampleAction.NoteClicked -> {
                viewModelScope.launch { _sideEffects.send(ExampleSideEffect.NavigateToEditor(action.noteId)) }
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
        }
    }

    private fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            baseNoteDao.deleteById(noteId)
            _sideEffects.send(ExampleSideEffect.ShowToast(R.string.deleted))
        }
    }
}
```

#### c. `<Feature>Screen.kt` (Stateless UI)
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
                        // Render note row
                    }
                }
            }

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
        ExampleScreen(state = ExampleUiState(), onAction = {}, onNavigateBack = {})
    }
}
```

#### d. `<Feature>Route.kt` (Stateful Coordinator)
```kotlin
package com.philkes.notallyx.feature.main.routes.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.philkes.notallyx.core.common.showToast
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.sideEffects.collect { effect ->
            when (effect) {
                is ExampleSideEffect.ShowToast -> context.showToast(effect.messageRes)
                is ExampleSideEffect.NavigateToEditor -> onNavigateToEditor(effect.noteId)
            }
        }
    }

    ExampleScreen(
        state = uiState,
        modifier = modifier,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
    )
}
```

#### e. Wiring Navigation & Dependency Injection
1. **Destination Key**: Define a `@Serializable` key in `navigation/NotallyNavKey.kt`:
   ```kotlin
   @Serializable
   data object Example : NotallyNavKey
   ```
2. **ViewModel Injection**: Register in `di/FeatureModule.kt`:
   ```kotlin
   viewModelOf(::ExampleViewModel)
   ```
3. **Navigation Entry**: Register in `feature/main/NotallyApp.kt`:
   ```kotlin
   entry<NotallyNavKey.Example> {
       ExampleRoute(
           onNavigateBack = { navBackStack.pop() },
           onNavigateToEditor = { noteId -> navBackStack.push(NotallyNavKey.Editor(noteId)) },
       )
   }
   ```

---

### 2. Creating a Database Migration (Room 3)
When modifying Room entities (`BaseNote`, `Label`, etc.):
1. Update entity classes in `com.philkes.notallyx.data.model`.
2. Increment the `version` number in `@Database(entities = [...], version = NEW_VERSION)` in `NotallyDatabase.kt`.
3. Define the `Migration` object in `NotallyDatabase.kt`:
   ```kotlin
   val Migration11_12 = object : Migration(11, 12) {
       override fun migrate(connection: SQLiteConnection) {
           connection.execSQL("ALTER TABLE notes ADD COLUMN new_feature_column TEXT DEFAULT NULL")
       }
   }
   ```
4. Add the migration object to `addMigrations(...)` in `NotallyDatabase.builder`.
5. Run `./gradlew kspDebugKotlin` to regenerate the Room schema JSON in `app/schemas/`.
6. Write migration verification tests in `app/src/test/kotlin/com/philkes/notallyx/data/` or `app/src/androidTest/kotlin/com/philkes/notallyx/utils/DataSchemaMigrationsTest.kt`.

---

### 3. Writing and Running Tests

#### Test Types & Targets:
- **Unit & Robolectric Tests**: `app/src/test/` for business logic, ViewModels, formatters, importers, and Robolectric lifecycle tests.
- **Roborazzi Screenshot Tests**: `app/src/test/kotlin/.../ScreenshotTests.kt` with reference images in `app/src/screenshots/`.
- **Compose Screenshot Tests**: `app/src/screenshotTestDebug/` validating composable previews against references.
- **Instrumented UI Tests**: `app/src/androidTest/` extending `UiTestBase` from `:test-lib` for end-to-end user flows.

#### Running Tests via CLI:
```bash
# Run unit and Robolectric tests
./gradlew testDebugUnitTest

# Roborazzi screenshot tests
./gradlew verifyRoborazziDebug          # Verify against reference images
./gradlew recordRoborazziDebug          # Record new reference images

# Compose screenshot preview tests
./gradlew validateDebugScreenshotTest    # Validate Compose screenshot tests
./gradlew updateDebugScreenshotTest      # Update Compose screenshot reference images

# Instrumented UI tests (connected device or emulator)
./gradlew connectedDebugAndroidTest

# Code formatting (ktfmt)
./gradlew ktfmtCheck                   # Check code formatting without applying
./gradlew ktfmtFormat                  # Reformat all Kotlin files
```

---

## Code Style & Quality Rules

1. **Formatting**: All Kotlin source code must follow `ktfmt` standard style (4-space indent, strict import ordering). Run `./gradlew ktfmtFormat` before committing.
2. **Naming Conventions**:
   - Entities & Models: PascalCase data classes (`BaseNote`, `Label`, `Attachment`).
   - DAOs: Interface suffix `*Dao` (`BaseNoteDao`, `LabelDao`).
   - ViewModels: `*ViewModel` (`SettingsViewModel`, `EditNoteViewModel`).
   - Feature Models: `*Model.kt` (`SettingsModel.kt`, `EditNoteModel.kt`).
   - Navigation Keys: PascalCase data class/object implementing `NotallyNavKey` (`NotallyNavKey.Notes`).
   - Screens & Routes: PascalCase functions (`NotesGridScreen`, `SettingsRoute`).
   - Preferences: DataStore wrapper properties in `NotallyXPreferences`.
3. **Error Handling**:
   - Catch specific exceptions rather than swallowing generic `Throwable`.
   - Provide user feedback using `showToast`, `showSnackbar`, or Compose dialog state.
   - For background jobs, update progress observers (`Progress.ProgressUpdate`, `MigrationProgress`).

---

## Agent Skills & Context Pointers

- **Route & Feature Creation**: Canonical 4-File Feature Quad and MVI architecture for all new destinations. See [.agents/skills/create-route/SKILL.md](.agents/skills/create-route/SKILL.md).
- **Compose UI Elements**: Creation, layout patterns, Material 3 theming, previews, and performance for Compose UI elements. See [.agents/skills/create-compose-ui-elements/SKILL.md](.agents/skills/create-compose-ui-elements/SKILL.md).
- **Evaluate Composables**: Audit and refactor existing `@Composable` functions against performance, modifier hygiene, and state hoisting standards. See [.agents/skills/evaluate-jetpack-composables/SKILL.md](.agents/skills/evaluate-jetpack-composables/SKILL.md).
- **UI Testing**: Standardized patterns for instrumented Compose and hybrid tests (`UiTestBase`, `UiUtils`, database seeding). See [.agents/skills/write-ui-tests/SKILL.md](.agents/skills/write-ui-tests/SKILL.md).
- **Roborazzi Screenshot Tests**: Best practices and workflows for JVM Roborazzi screenshot tests. See [.agents/skills/roborazzi/SKILL.md](.agents/skills/roborazzi/SKILL.md).
- **DI Migration & Koin**: Koin 4.x setup, safe DSL, and Koin Compiler Plugin migration. See [.agents/skills/android-di-migration/SKILL.md](.agents/skills/android-di-migration/SKILL.md).
- **LiveData to StateFlow**: Structured workflow for migrating legacy LiveData to Flow and StateFlow. See [.agents/skills/migrate-livedata-to-stateflow/SKILL.md](.agents/skills/migrate-livedata-to-stateflow/SKILL.md).
- **Code Review**: Dual-axis review (Standards & Spec) for branches and PRs. See [.agents/skills/code-review/SKILL.md](.agents/skills/code-review/SKILL.md).
- **Domain Modeling**: Terminology, `GLOSSARY.md`, and ADR conventions. See [.agents/skills/domain-modeling/SKILL.md](.agents/skills/domain-modeling/SKILL.md).
- **Writing for Agents**: Guidelines for authoring skills, `AGENTS.md`, and context pointers. See [.agents/skills/writing-for-agents/SKILL.md](.agents/skills/writing-for-agents/SKILL.md).
- **Domain Docs**: Repo domain exploration guidelines. See [docs/agents/domain.md](docs/agents/domain.md).
- **Issue Tracker**: Local file-based issue tracking under `.scratch/`. See [docs/agents/issue-tracker.md](docs/agents/issue-tracker.md).

# AGENTS.md - NotallyX Development Guidelines for AI Agents

## Project Overview & Purpose

**NotallyX** is an open-source, minimalistic, yet feature-rich note-taking Android application built with Kotlin. It provides users with a distraction-free experience for creating text notes, task lists, rich-text formatting, media attachments (images, audio, files), labels, reminders, and encrypted backups.

- **Target Platform**: Android (Min SDK: 24, Compile/Target SDK: 37, JVM Target: 17).
- **Core Architecture**: Feature-first MVVM (Model-View-ViewModel) architecture backed by Android Jetpack components (Jetpack Compose, Room, Kotlin Coroutines Flow/StateFlow, ViewModel, Navigation 3, WorkManager), Koin Dependency Injection, and Jetpack Glance.
- **Key Tenets**: User privacy, offline-first reliability, non-destructive data handling, robust backup/restore mechanisms, and clean Kotlin idioms.

---

## Tech Stack & Conventions

### Language & Tooling
- **Language**: Kotlin 2.4.20 (Standard Kotlin Idioms, Coroutines, Serialization).
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`), Android Gradle Plugin 9.4.1, KSP (`com.google.devtools.ksp`) for Room compiler.
- **Dependency Injection**: Koin 4.x (`io.insert-koin`) with Koin Compiler Plugin (`io.insert-koin.compiler.plugin`).
- **Code Formatter**: `ktfmt` with Kotlin standard style (`kotlinLangStyle`).

### UI Framework & Conventions
- **UI Toolkit**: Pure Jetpack Compose with Material 3 (`androidx.compose.material3`).
- **Theming**: `NotallyXTheme` supporting dynamic system colors, dark mode, and custom note colors.
- **Navigation**: Jetpack Navigation 3 (`androidx.navigation3.runtime`, `androidx.navigation3.ui`) using type-safe `@Serializable` `NotallyNavKey` entries with `rememberNavBackStack`, `NavDisplay`, and `entryProvider`.
- **Lists & Grids**: Compose `LazyColumn` and `LazyVerticalGrid` / `LazyVerticalStaggeredGrid`. Reordering and gestures use custom Compose drag-and-drop and swipe handlers.
- **App Widgets**: Jetpack Glance (`androidx.glance.appwidget`, `NoteWidget.kt`).
- **Legacy Interop**: `LockedActivity<T : ViewBinding>` uses `ComposeBinding` as a lightweight wrapper around `ComposeView`. Do not create XML layouts for new UI features.

### State Management & Concurrency
- **State Holders**: `ViewModel` / `AndroidViewModel` subclasses exposing observable state via `StateFlow` / `MutableStateFlow` (`asStateFlow()`), registered via Koin `viewModelOf`.
- **UI Observation**: Lifecycle-aware collection using `collectAsStateWithLifecycle()` in composables, or `repeatOnLifecycleScope` in Activities.
- **Async & Concurrency**: Kotlin Coroutines (`viewModelScope`, `CoroutineScope`).
- **Dispatchers**:
  - `Dispatchers.Main`: UI interactions and StateFlow emissions.
  - `Dispatchers.IO`: Database operations, disk I/O, file exports/imports, compression, encryption.
  - `Dispatchers.Default`: Intensive CPU calculations (e.g. search occurrences, text delta calculations).
  - Handle coroutine exceptions using `CoroutineExceptionHandler` or try-catch blocks around suspend calls.

### Database & Storage Patterns
- **Database**: Room Database (`NotallyDatabase`, versioned with KSP schema exports in `app/schemas/`).
- **Migrations**: Incremental schema migrations defined in `core/database/DataSchemaMigrations.kt`.
- **Encryption**: Optional SQLCipher database encryption (`net.zetetic:sqlcipher-android`) and AndroidX Security `EncryptedSharedPreferences` for sensitive preferences.
- **Storage Strategy**:
  - Internal storage for private app data and default SQLite database.
  - Optional external storage mode (`dataInPublicFolder`).
  - Strict WAL checkpointing (`pragma WAL_CHECKPOINT(FULL)`) before any database export, backup, or replacement.
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
│   │   │   ├── AndroidManifest.xml                    # Manifest, permissions, activities, receivers
│   │   │   ├── java/com/philkes/notallyx/
│   │   │   │   ├── NotallyXApplication.kt             # Application entry point & Koin initialization
│   │   │   │   ├── core/                              # Core domain & infrastructure
│   │   │   │   │   ├── common/                        # Extensions, results (Progress), error screens, BaseViewModel
│   │   │   │   │   ├── database/                      # Room DB (NotallyDatabase), migrations, converters
│   │   │   │   │   ├── preference/                    # NotallyXPreferences, sorting models
│   │   │   │   │   ├── security/                      # Biometrics, encryption, SQLCipher, LockedActivity
│   │   │   │   │   └── ui/                            # Compose theme, BaseNoteCard, dialogs, UI components
│   │   │   │   ├── data/                              # Data layer
│   │   │   │   │   ├── dao/                           # Room DAOs (BaseNoteDao, LabelDao, CommonDao)
│   │   │   │   │   └── model/                         # Entities & models (BaseNote, Label, Folder, Item)
│   │   │   │   ├── di/                                # Koin modules (AppModule, FeatureModule, DatabaseModule)
│   │   │   │   ├── feature/                           # Feature-first modules
│   │   │   │   │   ├── editor/                        # Note & list editing (EditActivity, NoteModel, bottom sheets)
│   │   │   │   │   ├── labels/                        # Labels screen, selector, viewmodels
│   │   │   │   │   ├── main/                          # MainActivity, NotallyApp, routes/ (Notes, Archived, Deleted, etc.)
│   │   │   │   │   ├── media/                         # Audio recording/playback, image viewer
│   │   │   │   │   ├── migration/                     # Backup, restore, exporters, external importers (Keep, Evernote, etc.)
│   │   │   │   │   ├── reminder/                      # Reminders screen, notifications, alarm receivers
│   │   │   │   │   ├── settings/                      # Settings screen, preference rows, dialogs
│   │   │   │   │   └── widget/                        # Jetpack Glance widgets (NoteWidget)
│   │   │   │   └── navigation/                        # Navigation 3 keys (NotallyNavKey)
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
- **Compose First**: Build user interfaces with Jetpack Compose composables and Material 3. When creating or refactoring UI elements, follow the [.agents/skills/evaluate-jetpack-composables](.agents/skills/create-compose-ui-elements/SKILL.md) skill.
- **State Hoisting**: Keep screen composables stateless and testable by hoisting state to ViewModel `StateFlow` and exposing event callbacks.
- **Use Navigation 3**: Manage destinations with type-safe `NotallyNavKey` entries in `NavDisplay`.
- **Preserve Data Integrity**: Always safeguard user notes. Never drop tables destructively without valid migration paths.
- **Run Checkpoints Before DB Copy**: When exporting, backing up, or copying SQLite files, call `database.checkpoint()` to ensure the WAL is flushed.
- **Offload Heavy Work**: Perform file I/O, parsing (JSON/Markdown/HTML), and database queries on `Dispatchers.IO`.
- **Format with ktfmt**: Ensure Kotlin files comply with `ktfmt` standard style (`./gradlew ktfmtFormat`).
- **Follow Existing Patterns**: Match existing naming conventions (`*Activity`, `*Route`, `*Screen`, `*ViewModel`, `*Dao`, `*NavKey`).
- **Write Unit, UI & Screenshot Tests**: Add unit tests under `app/src/test/`, instrumented tests under `app/src/androidTest/`, and Roborazzi / Compose screenshot tests whenever introducing features, migration steps, or UI changes.

### Don'ts
- **NO XML Layouts for New Screens**: Use Jetpack Compose composables for all new UI screens, dialogs, and components.
- **NO Direct State Mutation in UI Tests**: Always interact through Compose semantics (`performClick()`, `performTextInput()`) or Espresso actions. Never directly assign activity/viewmodel state fields in UI tests.
- **NO Blocking Main Thread**: Never invoke synchronous database queries, heavy regex, or file operations on the main thread.
- **NO Raw SQL Injections**: Use Room DAO annotations (`@Query`, `@Insert`, `@Update`, `@Delete`) or properly parameterized `SupportSQLiteQuery`.
- **NO Hardcoded Strings**: Use string resources (`R.string.*`) for all UI-visible text to preserve localization support across 30+ languages.
- **NO Unapproved Third-Party Libraries**: Avoid adding heavy external dependencies unless explicitly requested.

---

## Common Tasks & Workflows

### 1. Adding a New Screen / Feature
> [!TIP]
> When creating any new Compose UI elements (screens, routes, components, or dialogs), strictly follow the templates and guidelines in [.agents/skills/evaluate-jetpack-composables/SKILL.md](.agents/skills/create-compose-ui-elements/SKILL.md) for modifier hygiene, state hoisting, M3 theming, previews, and performance.

1. **Destination Key**: Define a `@Serializable` key in `navigation/NotallyNavKey.kt` implementing `NotallyNavKey`.
2. **ViewModel**: Expose `StateFlow` state in a `ViewModel` in `feature/<name>/` and register it in `di/FeatureModule.kt` via `viewModelOf(::MyViewModel)`.
3. **Screen Composable**: Build a stateless Composable in `feature/<name>/<Name>Screen.kt` accepting data state and hoisted callback lambdas (`onItemClick`, `onBackClick`).
4. **Route Composable**: If hosted in `MainActivity`, create a Route in `feature/main/routes/<name>/<Name>Route.kt` that injects the ViewModel with `koinViewModel()`, collects state via `collectAsStateWithLifecycle()`, and binds callbacks to the stateless screen.
5. **Navigation Entry**: Register the destination in `feature/main/NotallyApp.kt` inside `NavDisplay`'s `entryProvider`:
   ```kotlin
   entry<NotallyNavKey.MyDestination> {
       MyRoute(...)
   }
   ```
6. **Previews**: Add `@Preview` functions wrapped in `NotallyXTheme` to verify the UI across light and dark themes.

#### Example Route and Screen Pattern:
```kotlin
@Composable
fun FeatureRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeatureViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        state = uiState,
        onRefresh = viewModel::refresh,
        onBackClick = onNavigateBack,
        modifier = modifier,
    )
}

@Composable
fun FeatureScreen(
    state: FeatureUiState,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feature_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        // Render content with innerPadding
    }
}
```

---

### 2. Creating a Database Migration
When modifying Room entities (`BaseNote`, `Label`, etc.):
1. Update entity classes in `com.philkes.notallyx.data.model`.
2. Increment the `version` number in `@Database(entities = [...], version = NEW_VERSION)` in `NotallyDatabase.kt`.
3. Define the `Migration` object in `core/database/DataSchemaMigrations.kt` (or `NotallyDatabase.kt`):
   ```kotlin
   val MIGRATION_11_12 = object : Migration(11, 12) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE notes ADD COLUMN new_feature_column TEXT DEFAULT NULL")
       }
   }
   ```
4. Add the migration object to the `MIGRATIONS` array in `NotallyDatabase.kt`.
5. Run `./gradlew kspDebugKotlin` to regenerate the Room schema JSON in `app/schemas/`.
6. Write migration verification tests in `app/src/test/kotlin/com/philkes/notallyx/data/` or `app/src/androidTest/kotlin/com/philkes/notallyx/utils/DataSchemaMigrationsTest.kt`.

---

### 3. Writing and Running Tests

#### Test Types & Targets:
- **Unit & Robolectric Tests**: `app/src/test/` for business logic, importers, formatters, and Robolectric lifecycle tests.
- **Roborazzi Screenshot Tests**: `app/src/test/kotlin/.../ScreenshotTests.kt` with reference images in `app/src/screenshots/`.
- **Compose Screenshot Tests**: `app/src/screenshotTestDebug/` validating composable previews against references.
- **Instrumented UI Tests**: `app/src/androidTest/` extending `UiTestBase` from `:test-lib` for end-to-end user flows.

#### Running Tests via CLI:
```bash
# Run unit and Robolectric tests
./gradlew testDebugUnitTest

# Roborazzi screenshot tests
./gradlew verifyRoborazziDebug        # Verify against reference images
./gradlew recordRoborazziDebug        # Record new reference images

# Compose screenshot preview tests
./gradlew validateDebugScreenshotTest  # Validate Compose screenshot tests
./gradlew updateDebugScreenshotTest    # Update Compose screenshot reference images

# Instrumented UI tests (connected device or emulator)
./gradlew connectedDebugAndroidTest

# Code formatting (ktfmt)
./gradlew ktfmtCheck                 # Check code formatting without applying
./gradlew ktfmtFormat                # Reformat all Kotlin files
```

---

## Code Style & Quality Rules

1. **Formatting**: All Kotlin source code must follow `ktfmt` Kotlin style (4-space indent, strict import ordering). Run `./gradlew ktfmtFormat` before committing.
2. **Naming Conventions**:
   - Entities & Models: PascalCase data classes (`BaseNote`, `Label`, `Attachment`).
   - DAOs: Interface suffix `*Dao` (`BaseNoteDao`, `LabelDao`).
   - ViewModels: `*ViewModel` or `*Model` (`MainActivityViewModel`, `NoteModel`).
   - Navigation Keys: PascalCase data class/object implementing `NotallyNavKey` (`NotallyNavKey.Notes`).
   - Composables: PascalCase functions (`BaseNoteCard`, `NotesGridScreen`, `SettingsRoute`).
   - Preferences: Wrapper properties in `NotallyXPreferences`.
3. **Error Handling**:
   - Catch specific exceptions rather than swallowing generic `Throwable`.
   - Provide user feedback using `showToast`, `showSnackbar`, or Compose dialog state.
   - For background jobs, update progress observers (`Progress.ProgressUpdate`, `MigrationProgress`, etc.).

---

## Agent Skills & Context Pointers

- **Issue Tracker**: Local markdown files under `.scratch/`. See `docs/agents/issue-tracker.md`.
- **Domain Docs**: Single-context layout (`GLOSSARY.md` + `docs/adr/`). See `docs/agents/domain.md`.
- **UI Testing Guide**: Standardized patterns for instrumented Compose and hybrid tests (`UiTestBase`, `UiUtils`, `runBlocking` seeding). See `.agents/skills/write-ui-tests/SKILL.md`.
- **Roborazzi Screenshot Tests**: Workflow and best practices for JVM screenshot tests. See `.agents/skills/roborazzi/SKILL.md`.
- **Compose UI Elements (Creation & Evaluation)**: Canonical templates, guidelines, and audit checklists for creating new Compose screens, routes, components, and dialogs. See [.agents/skills/create-compose-ui-elements/SKILL.md](.agents/skills/create-compose-ui-elements/SKILL.md).
- **Create Route & Destination**: Best practices and canonical templates for creating new Routes and features following MVI / Unidirectional Data Flow (Model, ViewModel, Screen, Route). See [.agents/skills/create-route/SKILL.md](.agents/skills/create-route/SKILL.md).

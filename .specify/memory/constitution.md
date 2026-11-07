<!--
Sync Impact Report:
- Version change: Unratified Template (0.0.0) → 1.0.0
- Principles defined:
  - Principle I: Compose-First UI & Zero XML (Initial Ratification)
  - Principle II: Feature-First MVI & The 4-File Feature Quad (Initial Ratification)
  - Principle III: Data Integrity & Non-Destructive Storage (Initial Ratification)
  - Principle IV: Privacy-First & Offline-First Security (Initial Ratification)
  - Principle V: Strict Thread & Concurrency Discipline (Initial Ratification)
  - Principle VI: Comprehensive Multi-Layer Testing (Initial Ratification)
  - Principle VII: Code Health, Formatting & Idiomatic Kotlin (Initial Ratification)
- Added sections:
  - Core Principles
  - Architecture & Technology Constraints
  - Development Workflow & Quality Gates
  - Governance
- Removed sections: None
- Follow-up TODOs: None
-->

# NotallyX Constitution

## Core Principles

### I. Compose-First UI & Zero XML
All user interfaces, screens, dialogs, bottom sheets, and widgets MUST be built exclusively with Jetpack Compose and Material 3 (`androidx.compose.material3`, Glance for widgets). XML layouts, ViewBinding, and Android Views MUST NOT be introduced. All UI elements MUST hoist state, provide `@Preview` composables covering both light and dark themes, and adhere strictly to Compose modifier hygiene. UI-visible text MUST NOT be hardcoded and MUST use Android string resources (`R.string.*`) to preserve localization support across all supported languages.

*Rationale*: NotallyX maintains a modern, declarative, 100% Compose UI codebase. Eliminating XML prevents architectural fragmentation, reduces build overhead, and enables fluid animations, responsive layouts, and system-wide dynamic color theming.

### II. Feature-First MVI & The 4-File Feature Quad
Every feature destination MUST strictly adhere to Unidirectional Data Flow (MVI) structured into the 4-File Feature Quad:
1. `<Feature>Model.kt`: Contains immutable `UiState`, sealed `Action` intents, `SideEffect` one-off events, and `DialogState`.
2. `<Feature>ViewModel.kt`: Central state holder managing `StateFlow<UiState>` and emitting one-off events via `Channel<SideEffect>`. All user interactions MUST enter through a centralized `onAction(action)` sink.
3. `<Feature>Screen.kt`: Stateless `@Composable` rendering `UiState` and dispatching user interactions through a single `onAction: (Action) -> Unit`. Composable parameter lists MUST NOT accept fragmented bags of individual lambda callbacks.
4. `<Feature>Route.kt`: Stateful coordinator injecting the ViewModel via `koinViewModel()`, collecting state via `collectAsStateWithLifecycle()`, and collecting side effects in `LaunchedEffect`.
Transient closures MUST NOT be stored in Compose `remember` state across dialogs or authentication barriers; state transitions MUST be explicitly modeled in the ViewModel.

*Rationale*: Strict MVI feature quad separation decouples presentation from business logic, prevents callback clutter, prevents state loss across lifecycle events or configuration changes, and maximizes testability.

### III. Data Integrity & Non-Destructive Storage
User notes, attachments, and configurations are sacred. Destructive operations (such as dropping tables or columns without data migration) are STRICTLY FORBIDDEN.
- Every Room entity change MUST provide an explicit incremental Room 3 migration (`Migration(from, to)`) accompanied by verified schema exports in `app/schemas/`.
- SQLite database exports, backups, or file replacements MUST execute `NotallyDatabase.checkpoint()` (`RoomRawQuery("pragma wal_checkpoint(TRUNCATE)")`) prior to file access to flush the Write-Ahead Log (WAL) and eliminate corruption risks.
- Database access MUST use type-safe Room DAO annotations (`@Query`, `@Insert`, `@Update`, `@Delete`) or properly parameterized queries; raw SQL string concatenation is STRICTLY FORBIDDEN.

*Rationale*: Users rely on NotallyX for irreplaceable personal notes and records. Zero data loss is a non-negotiable operational principle. Mandatory WAL checkpointing ensures data durability and consistency across backups and restores.

### IV. Privacy-First & Offline-First Security
NotallyX MUST operate offline-first with zero tracking, zero telemetry, and zero unrequested network communication.
- General preferences MUST be stored in Jetpack DataStore (`UserPreferences`).
- Sensitive preferences and security tokens MUST be secured using Google Tink AEAD encrypted DataStore (`SecurePreferences`).
- Database locking MUST support SQLCipher encryption (`net.zetetic:sqlcipher-android`) and biometric access validation via `LockedActivity`.

*Rationale*: User privacy and trust are paramount. Note content and sensitive configuration must remain private, on-device, and cryptographically protected from unauthorized local and external exposure.

### V. Strict Thread & Concurrency Discipline
The Android main thread MUST NEVER be blocked by I/O, database access, or heavy computations.
- Room database operations, DataStore read/writes, file I/O, backup/restore, encryption, and compression MUST execute on `Dispatchers.IO`.
- Intensive CPU calculations (including full-text search indexing, regex evaluation, and note formatting parsing) MUST execute on `Dispatchers.Default`.
- UI state emissions and direct UI interactions MUST execute on `Dispatchers.Main`.

*Rationale*: Deterministic dispatcher assignment guarantees UI responsiveness, eliminates UI stutter (jank), preserves 60/120 FPS rendering, and prevents Application Not Responding (ANR) crashes.

### VI. Comprehensive Multi-Layer Testing
Every functional capability MUST be validated through automated multi-layered testing:
- Business logic, ViewModels, formatters, and data converters MUST have unit and Robolectric tests under `app/src/test/`.
- UI visual fidelity and layout consistency MUST be protected against regressions using Roborazzi screenshot tests (`verifyRoborazziDebug`) and Compose Preview screenshot tests.
- Instrumented UI tests under `app/src/androidTest/` MUST extend `UiTestBase` and interact exclusively through Compose semantics (`performClick()`, `performTextInput()`) or Espresso actions. UI tests MUST NOT directly mutate ViewModel or Activity state fields.

*Rationale*: Multi-tiered automated tests catch behavioral and visual regressions before release, enabling rapid continuous delivery without sacrificing reliability or visual polish.

### VII. Code Health, Formatting & Idiomatic Kotlin
All code contributed to the repository MUST adhere to standard Kotlin idioms and formatting standards:
- Source code MUST strictly conform to `ktfmt` standard style (4-space indent, strict import ordering) verified via `./gradlew ktfmtCheck`.
- Dependency injection MUST follow Koin 4.x safe DSL with the Koin Compiler Plugin (`io.insert-koin.compiler.plugin`).
- Exception handling MUST be explicit and targeted; catching and silently discarding generic `Throwable` or `Exception` is PROHIBITED.

*Rationale*: Automated code formatting and strict architectural conventions eliminate stylistic debates in code reviews, minimize cognitive friction, and preserve repository maintainability.

## Architecture & Technology Constraints

The following architectural boundaries and component baselines MUST be respected across all modules:

- **Target Platform**: Android with Minimum SDK 24, Compile/Target SDK 37, and JVM Target 17.
- **Language & Build**: Kotlin 2.4.20, Android Gradle Plugin 9.4.1, Gradle Kotlin DSL (`build.gradle.kts`), and KSP (`com.google.devtools.ksp`).
- **UI Framework**: Pure Jetpack Compose with Material 3 (`androidx.compose.material3`). Zero XML layouts or ViewBinding.
- **Navigation**: Jetpack Navigation 3 (`androidx.navigation3.runtime`, `androidx.navigation3.ui`) utilizing type-safe `@Serializable` `NotallyNavKey` entries, `rememberNavBackStack`, `NavDisplay`, and adaptive two-pane `ListDetailScene`.
- **Dependency Injection**: Koin 4.x (`io.insert-koin`) utilizing the Koin Compiler Plugin (`viewModelOf`, `singleOf`).
- **Database & Preferences**: Room 3 (`NotallyDatabase`) with KSP schema exports in `app/schemas/`, Jetpack DataStore (`UserPreferences`), and Google Tink AEAD encrypted DataStore (`SecurePreferences`).
- **Media & Widgets**: Coil 3 for image loading, Jetpack Media3 for audio recording/playback (`AudioPlayService`), and Jetpack Glance for home screen widgets (`NoteWidget`).
- **Documentation**: User and technical documentation MUST reside in the Docusaurus website under `/documentation` (`@documentation`).

## Development Workflow & Quality Gates

To ensure strict compliance with architectural and quality baselines, all changes MUST satisfy the following quality gates prior to merging:

1. **Formatting & Style Gate**: Run `./gradlew ktfmtCheck` (or `./gradlew ktfmtFormat` to apply changes automatically). No formatting violations are permitted.
2. **Unit & Logic Gate**: Run `./gradlew testDebugUnitTest`. All unit and Robolectric tests MUST pass without failure.
3. **Visual Regression Gate**: Run `./gradlew verifyRoborazziDebug` and `./gradlew validateDebugScreenshotTest`. Any visual diffs MUST be explicitly reviewed and acknowledged.
4. **Database Integrity Gate**: Any changes to entities MUST produce a corresponding schema JSON via `./gradlew kspDebugKotlin` under `app/schemas/` and include automated migration tests.
5. **Localization Gate**: No raw strings in UI components; all user-facing strings MUST reference resources in `res/values/strings.xml`.
6. **Code Review Gate**: All PRs MUST undergo dual-axis review evaluating adherence to both functional specification and NotallyX architectural guidelines.

## Governance

This Constitution acts as the supreme technical authority for the NotallyX codebase.

- **Supremacy**: The principles, constraints, and quality gates set forth in this document supersede informal agreements, historical habits, or unratified patterns.
- **Amendment Process**: Amendments to this constitution require a formal proposal detailing the rationale, impact analysis, and code migration strategy. Amendments must be approved by project maintainers and documented in git commit history.
- **Versioning Policy**:
  - **MAJOR (X.0.0)**: Incompatible changes to core principles, breaking architecture overhauls, or removal of quality gates.
  - **MINOR (x.Y.0)**: Addition of new principles, introduction of new architectural constraints, or expansion of quality gates.
  - **PATCH (x.y.Z)**: Non-semantic clarifications, wording improvements, or typo fixes.
- **Compliance Review**: All architectural reviews, pull requests, and automated agent workflows MUST verify compliance with this Constitution and `AGENTS.md`.

**Version**: 1.0.0 | **Ratified**: 2026-10-31 | **Last Amended**: 2026-10-31

# Implementation Plan: Unified Cross-Platform UI Composables (KMP)

**Branch**: `005-unify-ui-composables` | **Date**: 2026-11-05 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/005-unify-ui-composables/spec.md`

## Summary

Unify the presentation layer across Android and Desktop by migrating all stateless UI screens, dialogs, cards, and toolbars from `composeApp/src/androidMain` into `composeApp/src/commonMain`. Both platforms will leverage the exact same base composables for every route (Notes Overview, Note Editor, Checklist Editor, Plain Text Editor, Search, Labels, Archive, Trash, Settings, Reminders, Media, and Pick Note). 

Disconnected duplicate screen stubs (`CommonEditNoteScreen`, `CommonEditListScreen`, `CommonNotesScreen`, etc.) introduced in milestone 004 are completely deprecated and deleted. Platform differences (biometrics, system notifications, battery optimizations) are cleanly controlled via state-driven capability flags in immutable `UiState`. Localization and vector assets are unified using Compose Multiplatform Resources (`Res.string.*`, `Res.drawable.*`). Navigation backstack management and adaptive two-pane display (`ListDetailScene`) are unified in `commonMain` using Jetpack Navigation 3.

## Technical Context

**Language/Version**: Kotlin 2.4.20 / JVM Target 17  
**Primary Dependencies**: Kotlin Multiplatform Gradle Plugin (`2.4.20`), Compose Multiplatform (`1.12.2`), Android Gradle Plugin (`9.4.1`), Jetpack Compose / Compose Multiplatform Material 3 (`1.9.0`), Jetpack Navigation 3 (`1.2.0`), Koin 4.x (`4.2.2`) + Koin Compiler Plugin (`1.2.1`), Coil 3 (`3.1.0`), Compose Multiplatform Resources  
**Storage**: Room 3 (`3.0.3`) SQLite via `BundledSQLiteDriver` (`NotallyDatabase`), Jetpack DataStore preferences, filesystem attachments via platform storage abstractions  
**Testing**: Cross-platform Compose UI testing via `runComposeUiTest` in `composeApp/src/commonTest/`, ViewModel unit tests in `:composeApp:testAndroidHostTest`, Roborazzi screenshot verification (`./gradlew verifyRoborazziDebug`), Compose screenshot tests (`./gradlew validateDebugScreenshotTest`), and `ktfmt` compliance (`./gradlew ktfmtFormat`)  
**Target Platform**: Android (Min SDK 24, Target SDK 37) and Desktop JVM (Linux, macOS, Windows; 64-bit JVM 17+)  
**Project Type**: Multi-module Kotlin Multiplatform application (`:shared`, `:composeApp`, `:app`, `:desktopApp`)  
**Performance Goals**: 60/120 FPS UI rendering without dropped frames, instantaneous search filtering, sub-16ms UI action handling, zero UI stutter  
**Constraints**: Zero `android.*` framework imports in shared screen composables; 100% localization preservation across 30+ languages via `Res.string`; strict adherence to MVI 4-File Feature Quad; zero duplicate screen codebases; `ktfmt` standard style formatting  
**Scale/Scope**: 17+ screen composables migrated to `commonMain`, 8 divergent stubs deleted, 100% route parity across Android and Desktop  

## Constitution Check

*GATE: Passed before Phase 0 research. Re-verified after Phase 1 design.*

- **Principle I: Compose-First UI & Zero XML**: **PASS**. 100% Jetpack Compose and Material 3 across all routes. All UI elements hoist state, provide light/dark theme `@Preview`s, and use Compose Multiplatform string resources (`Res.string.*`) for localization.
- **Principle II: Feature-First MVI & The 4-File Feature Quad**: **PASS**. Every feature destination adheres strictly to the 4-File Feature Quad (`Model.kt`, `ViewModel.kt`, `Screen.kt`, `Route.kt`). Stateless screens accept immutable `UiState` and dispatch through a single `onAction: (Action) -> Unit` event sink.
- **Principle III: Data Integrity & Non-Destructive Storage**: **PASS**. Database DAOs, migrations, and Room 3 persistence remain untouched; presentation unification consumes existing models and DAOs non-destructively.
- **Principle IV: Privacy-First & Offline-First Security**: **PASS**. 100% offline-first presentation. Zero telemetry, zero external tracking.
- **Principle V: Strict Thread & Concurrency Discipline**: **PASS**. Coroutine dispatchers respected (`Dispatchers.Main` for UI emissions, `Dispatchers.IO` for storage/file operations, `Dispatchers.Default` for parsing/search).
- **Principle VI: Comprehensive Multi-Layer Testing**: **PASS**. Shared screens allow cross-platform Compose UI tests in `commonTest`, unit tests in `androidHostTest`, and existing Roborazzi screenshot tests in `:app` remain guarded against regressions.
- **Principle VII: Code Health, Formatting & Idiomatic Kotlin**: **PASS**. All migrated and newly introduced files conform to `ktfmt` standard style (`./gradlew ktfmtFormat`) and Koin 4.x safe DI conventions.

## Project Structure

### Documentation (this feature)

```text
specs/005-unify-ui-composables/
├── plan.md                         # This file (/speckit-plan command output)
├── research.md                     # Phase 0 technical decisions & screen migration inventory
├── data-model.md                   # Phase 1 presentation entities & source-set allocation
├── quickstart.md                   # Phase 1 runnable validation & verification gates
├── contracts/                      # Phase 1 component & event sink contracts
│   └── ui-contracts.md
├── checklists/
│   └── requirements.md             # Spec quality checklist (/speckit-specify output)
└── tasks.md                        # Phase 2 task breakdown (created by /speckit-tasks)
```

### Source Code (repository root)

```text
NotallyX/
├── shared/                                             # KMP Domain & Data Module
│   └── src/
│       ├── commonMain/kotlin/com/philkes/notallyx/     # BaseNote, Label, BaseNoteDao, Converters, Migrations
│       ├── desktopMain/kotlin/com/philkes/notallyx/    # Desktop database & storage implementations
│       └── androidMain/kotlin/com/philkes/notallyx/    # Android database & storage implementations
│
├── composeApp/                                         # KMP Presentation & Shared UI Module
│   ├── build.gradle.kts                                # Configures Compose Multiplatform Resources
│   └── src/
│       ├── commonMain/
│       │   ├── composeResources/                       # Shared strings.xml and vector drawables
│       │   └── kotlin/com/philkes/notallyx/
│       │       ├── navigation/                         # NotallyNavKey, NavDisplay, ListDetailScene
│       │       ├── core/ui/                            # Shared components (cards, chips), dialogs, themes
│       │       └── feature/                            # [UNIFIED MVI 4-FILE QUAD PER FEATURE]
│       │           ├── editor/                         # EditNoteScreen, EditListScreen, EditTextPlainScreen
│       │           ├── main/                           # NotesGridScreen, PickNoteScreen, routes (Search, Folders)
│       │           ├── labels/                         # LabelsScreen, SelectLabelsScreen
│       │           ├── settings/                       # SettingsScreen (with capability flags)
│       │           ├── reminder/                       # RemindersScreen, NoteRemindersScreen
│       │           └── media/                          # PlayAudioScreen, RecordAudioScreen, ViewImageScreen
│       ├── desktopMain/kotlin/com/philkes/notallyx/    # Desktop platform delegates (FilePicker, ShareHandler)
│       └── androidMain/kotlin/com/philkes/notallyx/    # Android Activity launchers & route coordinators
│
├── desktopApp/                                         # Desktop Application Host Subproject
│   └── src/desktopMain/kotlin/com/philkes/notallyx/desktop/
│       ├── Main.kt                                     # Hosts shared NotallyApp / NavDisplay
│       └── di/DesktopModule.kt                         # Desktop DI registrations
│
└── app/                                                # Lean Android Host Subproject
    └── src/main/java/com/philkes/notallyx/             # Android Activity, Service, Receiver registrations
```

## Complexity Tracking

> **No violations**. Clean separation of concerns following NotallyX Constitution and standard Kotlin Multiplatform architectural patterns.

# Implementation Plan: Desktop Target for Kotlin Multiplatform

**Branch**: `004-kmp-desktop-target` | **Date**: 2026-11-04 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/004-kmp-desktop-target/spec.md`

## Summary

Expand **NotallyX** to support a desktop target on **Kotlin Multiplatform (KMP)** by introducing a dedicated desktop application host subproject (`:desktopApp`) and enabling the `desktop` JVM target across `:shared` (Domain & Data) and `:composeApp` (Presentation & UI). Implementation is phased into three progressive milestones:
1. **Milestone 1**: A minimal, runnable desktop target executing via Compose Multiplatform for Desktop with an interactive example starter screen.
2. **Milestone 2**: Core note management (Notes Overview, Text Note Editor, and Checklist Editor) sharing the exact same Room 3 database engine (`NotallyDatabase`), DAOs, converters, and v1–v11 migrations via `BundledSQLiteDriver`.
3. **Milestone 3**: Progressive route parity for remaining shared screens (Search, Labels, Archive, Trash, Settings, Import/Export) while cleanly isolating and omitting mobile-only hardware subsystems (biometric lock, system notifications, home screen widgets).

## Technical Context

**Language/Version**: Kotlin 2.4.20 / JVM Target 17  
**Primary Dependencies**: Kotlin Multiplatform Gradle Plugin (`2.4.20`), Compose Multiplatform for Desktop (`org.jetbrains.compose`), Android Gradle Plugin (`9.4.1`), Jetpack Compose / Compose Multiplatform Material 3, Room 3 (`3.0.3`) + `sqlite-bundled` (`2.7.1`), Koin 4.x (`4.2.2`) + Koin Compiler Plugin (`1.2.1`), `kotlinx-serialization-json` (`1.11.0`), Coil 3 (`3.1.0`), `jsoup` (`1.23.2`), `commonmark` (`0.30.0`), `PrettyTime` (`4.0.6.Final`)  
**Storage**: Room 3 SQLite via `BundledSQLiteDriver` (`NotallyDatabase` v11 schema exported to `shared/schemas/`), standard OS application data directory (`~/.local/share/NotallyX` on Linux, `~/Library/Application Support/NotallyX` on macOS, `%APPDATA%\NotallyX` on Windows), filesystem attachments via `DesktopPathProvider` and `DesktopFileStorage`  
**Testing**: Pure-JVM unit tests with `BundledSQLiteDriver` in `:shared` (`NotallyDatabaseJvmTest`), Compose desktop UI testing, and `./gradlew ktfmtCheck`  
**Target Platform**: Desktop JVM (Linux, macOS, Windows; 64-bit JVM 17+); Android target (`:app`) preserved intact; iOS and other targets explicitly out of scope  
**Project Type**: Multi-module Kotlin Multiplatform desktop application (`:desktopApp`) sharing `:shared` and `:composeApp` with `:app`  
**Performance Goals**: Desktop window startup latency < 2.0s, 60 FPS Compose rendering, zero UI stutter, instantaneous search over local SQLite database  
**Constraints**: Zero `android.*` imports in `:desktopApp` and desktop source sets; 100% on-disk SQLite binary format compatibility between Android and Desktop; biometrics, notifications, and home screen widgets excluded from desktop; `ktfmt` (`kotlinLangStyle`) formatting compliance  
**Scale/Scope**: 4 subprojects (`:shared`, `:composeApp`, `:app`, `:desktopApp`), 3 delivery milestones, 1 shared Room database schema, 100% code sharing of core domain entities and persistence  

## Constitution Check

*GATE: Passed before Phase 0 research. Re-verified after Phase 1 design.*

- **Principle I: Compose-First UI & Zero XML**: **PASS**. Desktop UI is built exclusively using Compose Multiplatform with Material 3. Zero XML layouts. Reusable components hoist state and support light/dark theming.
- **Principle II: Feature-First MVI & The 4-File Feature Quad**: **PASS**. As shared features are surfaced on desktop in Milestones 2 and 3, they adhere strictly to the 4-File Feature Quad (`Model.kt`, `ViewModel.kt`, `Screen.kt`, `Route.kt`) and unidirectional `onAction(Action)` flow.
- **Principle III: Data Integrity & Non-Destructive Storage**: **PASS**. Desktop utilizes the exact same Room 3 `NotallyDatabase` definition, v1–v11 migrations, and `BundledSQLiteDriver`. `NotallyDatabase.checkpoint()` (`pragma wal_checkpoint(TRUNCATE)`) is executed to ensure data durability on desktop.
- **Principle IV: Privacy-First & Offline-First Security**: **PASS**. Desktop operates 100% offline-first with local-only SQLite storage in the user's OS application directory. Zero telemetry, zero unauthorized network calls.
- **Principle V: Strict Thread & Concurrency Discipline**: **PASS**. Database operations and disk I/O execute on `Dispatchers.IO`; note formatting and search parsing on `Dispatchers.Default`; UI state emissions on `Dispatchers.Main`.
- **Principle VI: Comprehensive Multi-Layer Testing**: **PASS**. Room database operations on desktop are verified through pure-JVM tests (`NotallyDatabaseJvmTest`), and desktop build tasks are validated via automated CI gates.
- **Principle VII: Code Health, Formatting & Idiomatic Kotlin**: **PASS**. All newly introduced code conforms to `ktfmt` standard style (`./gradlew ktfmtCheck`) and Koin 4.x safe DI conventions.

## Project Structure

### Documentation (this feature)

```text
specs/004-kmp-desktop-target/
├── plan.md                         # This file (/speckit-plan command output)
├── research.md                     # Phase 0 technical decisions & rationale
├── data-model.md                   # Phase 1 module & source-set entity ownership model
├── quickstart.md                   # Phase 1 runnable validation & gate checks
├── contracts/                      # Phase 1 module, build & DI boundary contracts
│   └── desktop-contracts.md
├── checklists/
│   └── requirements.md             # Spec quality checklist (/speckit-specify output)
└── tasks.md                        # Phase 2 task breakdown (created by /speckit-tasks)
```

### Source Code (repository root)

```text
NotallyX/
├── shared/                                             # KMP Domain & Data Module
│   ├── build.gradle.kts                                # Adds jvm("desktop") target
│   └── src/
│       ├── commonMain/kotlin/com/philkes/notallyx/     # BaseNote, Label, BaseNoteDao, Converters, Migrations, PathProvider
│       ├── desktopMain/kotlin/com/philkes/notallyx/    # [NEW] DesktopDatabaseDriverFactory, DesktopFileStorage, DesktopLogger
│       └── androidMain/kotlin/com/philkes/notallyx/    # AndroidDatabaseDriverFactory, AndroidPathProvider, TinkAeadProvider
│
├── composeApp/                                         # KMP Presentation & Shared UI Module
│   ├── build.gradle.kts                                # Adds jvm("desktop") target
│   └── src/
│       ├── commonMain/kotlin/com/philkes/notallyx/     # NotallyNavKey, NavigationState, Theme, Shared Dialogs & Components
│       ├── desktopMain/kotlin/com/philkes/notallyx/    # [NEW] DesktopFilePicker, DesktopShareHandler, DesktopExampleScreen
│       └── androidMain/kotlin/com/philkes/notallyx/    # AndroidFilePicker, AndroidShareHandler, AndroidPermissionChecker
│
├── desktopApp/                                         # [NEW] Desktop Application Host Subproject
│   ├── build.gradle.kts                                # kotlin("multiplatform") + application + Compose Desktop
│   └── src/
│       └── desktopMain/kotlin/com/philkes/notallyx/desktop/
│           ├── Main.kt                                 # Desktop entry point with singleWindowApplication
│           ├── DesktopAppInfo.kt                       # Host environment metadata provider
│           └── di/
│               └── DesktopModule.kt                    # Koin configuration for Desktop target
│
├── app/                                                # Android Application Host Subproject (Unchanged)
│   └── src/main/java/com/philkes/notallyx/             # NotallyXApplication, MainActivity, LockedActivity, NoteWidget
│
└── settings.gradle.kts                                 # Includes :desktopApp alongside :shared, :composeApp, :app, :test-lib
```

**Structure Decision**: A dedicated desktop application module (`:desktopApp`) is introduced to isolate the desktop window runner, lifecycle hooks, and desktop-specific packaging, while `:shared` and `:composeApp` enable the `desktop` JVM target to provide maximum code sharing across domain, persistence, and presentation layers without impacting the Android application (`:app`).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations. All principles and quality gates passed.*

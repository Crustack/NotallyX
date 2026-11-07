# Implementation Plan: Kotlin Multiplatform Project Migration (Android Target Only)

**Branch**: `003-kmp-android-migration` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-kmp-android-migration/spec.md` and completed prerequisite baseline in [`specs/002-kmp-migration-prep/`](../002-kmp-migration-prep/spec.md)

## Summary

Migrate **NotallyX** from a single-module Android application (`:app`) into a multi-module **Kotlin Multiplatform (KMP)** project structured into `:shared` (Domain & Data KMP module), `:composeApp` (Presentation & Shared UI KMP module), and `:app` (Android Application Host module), alongside `:test-lib` and `:build-logic`. For this feature phase, `:shared` and `:composeApp` apply `org.jetbrains.kotlin.multiplatform` and AGP 9's `com.android.kotlin.multiplatform.library` with **only the Android target enabled** (`kotlin { android { ... } }`), separating platform-agnostic code into `src/commonMain` (with zero `android.*` imports) and Android platform implementations into `src/androidMain` (or `:app`). Desktop (`jvm("desktop")`) target enablement is deferred to a subsequent feature.

## Technical Context

**Language/Version**: Kotlin 2.4.20 / JVM Target 17  
**Primary Dependencies**: Android Gradle Plugin `9.4.1` (`com.android.application` & `com.android.kotlin.multiplatform.library`), Kotlin Multiplatform Gradle Plugin (`2.4.20`), Jetpack Compose (BOM `2026.09.00`), Material 3 (`1.3.0` adaptive), Jetpack Navigation 3 (`1.2.0`), Lifecycle Runtime/ViewModel Compose (`2.11.0`), Room 3 (`3.0.3`) + `sqlite-bundled` (`2.7.1`), Jetpack DataStore (`1.3.0`), Koin 4.x (`4.2.2`) + Koin Compiler Plugin (`1.2.1`), `kotlinx-serialization-json` (`1.11.0`), Coil 3 (`3.1.0`), Skydoves ColorPicker (`1.3.0`), `jsoup` (`1.23.2`), `zip4j` (`2.11.6`), `commonmark` (`0.30.0`), `PrettyTime` (`4.0.6.Final`)  
**Storage**: Room 3 SQLite in `:shared` (v11 schema exported to `shared/schemas/`, WAL mode with `pragma wal_checkpoint(TRUNCATE)`), Jetpack DataStore (`UserPreferences` & Tink AEAD `SecurePreferencesStore`), filesystem attachments via `PathProvider` & `FileStorage`  
**Testing**: JUnit 4 + Coroutines Test + `BundledSQLiteDriver` unit tests in `:shared` and `:composeApp` (`androidHostTest`), Robolectric 4.17 & Roborazzi (`verifyRoborazziDebug`) & Compose Screenshot tests (`validateDebugScreenshotTest`) & AndroidX Instrumented UI tests (`connectedDebugAndroidTest`) in `:app`  
**Target Platform**: Android (Min SDK 24, Compile/Target SDK 37) via KMP Android target (`kotlin { android { ... } }`); Desktop target deferred  
**Project Type**: Multi-module Kotlin Multiplatform mobile application (`:shared`, `:composeApp`, `:app`, `:test-lib`, `:build-logic`)  
**Performance Goals**: 60/120 FPS UI rendering, zero main-thread blocking I/O, parallel multi-module Gradle compilation, 100% lossless upgrade path from existing Android installations  
**Constraints**: Zero `android.*` imports in `shared/src/commonMain` and `composeApp/src/commonMain`; strict acyclic dependency graph (`:app` → `:composeApp` → `:shared`); preserve `applicationId = "com.philkes.notallyx"` and `debug`/`beta`/`release` variants; AGP 9.4.1 built-in Kotlin & new DSL compliance; `ktfmt` (`kotlinLangStyle`) compliance  
**Scale/Scope**: 3 production modules (`:shared`, `:composeApp`, `:app`), 18 feature routes (4-File Feature Quads), ~30 ViewModels, 3 DAOs, 11 DB schema versions, 30 translated locales  

## Constitution Check

*GATE: Passed before Phase 0 research. Re-verified after Phase 1 design.*

- **Principle I: Compose-First UI & Zero XML**: **PASS**. 100% Compose Material 3 UI is preserved in `:composeApp` and `:app` (with Glance widgets in `:app`). Zero hardcoded UI strings across all 30 locales.
- **Principle II: Feature-First MVI & The 4-File Feature Quad**: **PASS**. All 18 feature destinations in `:composeApp` preserve their 4-File Feature Quad (`Model.kt`, `ViewModel.kt`, `Screen.kt`, `Route.kt`) and unidirectional `onAction(Action)` flow.
- **Principle III: Data Integrity & Non-Destructive Storage**: **PASS**. Room 3 `NotallyDatabase` (v1–v11 migrations) and schema exports (`shared/schemas/`) are preserved intact in `:shared`. `NotallyDatabase.checkpoint()` (`pragma wal_checkpoint(TRUNCATE)`) is enforced before any backup, export, or startup decryption swap.
- **Principle IV: Privacy-First & Offline-First Security**: **PASS**. Offline-first architecture, `LockedActivity` biometric lock in `:app`, Tink AEAD `AndroidSecurePreferencesStore` in `:shared/androidMain`, and automatic startup decryption for legacy SQLCipher databases (`FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`) are preserved.
- **Principle V: Strict Thread & Concurrency Discipline**: **PASS**. `Dispatchers.IO` for Room, DataStore, and file I/O; `Dispatchers.Default` for search/diff/parsing; `Dispatchers.Main` for UI state.
- **Principle VI: Comprehensive Multi-Layer Testing**: **PASS**. Unit tests execute across `:shared`, `:composeApp`, and `:app`, while Roborazzi, Compose Screenshot, and `androidTest` suites in `:app` verify visual and end-to-end Android behavior.
- **Principle VII: Code Health, Formatting & Idiomatic Kotlin**: **PASS**. All modules apply `ktfmt` (`kotlinLangStyle`) and Koin 4.x Safe DSL with Koin Compiler Plugin.

## Project Structure

### Documentation (this feature)

```text
specs/003-kmp-android-migration/
├── plan.md                         # This file (/speckit-plan command output)
├── research.md                     # Phase 0 technical decisions & rationale
├── data-model.md                   # Phase 1 module & source-set entity ownership model
├── quickstart.md                   # Phase 1 runnable validation & gate checks
├── contracts/                      # Phase 1 module, build & DI boundary contracts
│   └── module-boundaries.md
├── checklists/
│   └── requirements.md             # Spec quality checklist (/speckit-specify output)
└── tasks.md                        # Phase 2 task breakdown (created by /speckit-tasks)
```

### Source Code (repository root)

```text
NotallyX/
├── shared/                                             # [NEW] KMP Domain & Data Module (Android target only)
│   ├── build.gradle.kts                                # kotlin("multiplatform") + com.android.kotlin.multiplatform.library + KSP
│   ├── schemas/                                        # Room 3 exported schemas (v1.json - v11.json)
│   └── src/
│       ├── commonMain/kotlin/com/philkes/notallyx/
│       │   ├── core/
│       │   │   ├── common/                             # Cache, NotallyUtils, NoteRepairUtils, NoteSplitUtils, Progress
│       │   │   ├── database/                           # Converters, ConverterErrorReporter, DatabaseDriverFactory, Migrations
│       │   │   ├── platform/                           # PathProvider, DesktopPathProvider, FileStorage, PlatformUri, MimeTypeUtils, Logger, AppInfo, TaskScheduler
│       │   │   └── preference/                         # UserPreferences, SecurePreferences, SecurePreferencesStore, PeriodicBackup
│       │   ├── data/
│       │   │   ├── dao/                                # BaseNoteDao, LabelDao, CommonDao, DaoExtensions
│       │   │   └── model/                              # BaseNote, Label, Attachment, Audio, FileAttachment, Reminder, ListItem, SpanRepresentation, extensions/
│       │   └── feature/
│       │       ├── media/audio/                        # AudioPlayer, AudioRecorder (+ NoOp implementations)
│       │       ├── migration/                          # ExportMimeType, PdfExporter, pure-JVM importers (Keep, Evernote, Quillpad, PlainText, Markdown, HTML)
│       │       └── reminder/                           # ReminderScheduler (+ NoOpReminderScheduler)
│       ├── androidMain/
│       │   ├── kotlin/com/philkes/notallyx/
│       │   │   ├── core/
│       │   │   │   ├── common/extensions/              # AndroidExtensions, IOExtensions, LoggingExtensions, MiscExtensions, ZipVerificationExtensions
│       │   │   │   ├── database/                       # NotallyDatabase, AndroidDatabaseDriverFactory, BackupSQLiteDriver, BackupOpenHelperFactory, DataSchemaMigrations
│       │   │   │   ├── platform/                       # AndroidPathProvider, AndroidFileStorage
│       │   │   │   ├── preference/                     # NotallyXPreferences, AndroidSecurePreferencesStore, Preference, NotesSorting, Legacy migrations
│       │   │   │   └── security/                       # EncryptionUtils, TinkAeadProvider
│       │   │   ├── di/                                 # DatabaseModule, PreferencesModule
│       │   │   └── feature/migration/backup/           # AndroidPdfExporter, ImportExtensions, ExportExtensions, AutoBackupExtensions, AttachmentImportExtensions
│       │   ├── java/
│       │   │   ├── android/print/                      # PdfExtensions.kt
│       │   │   └── com/philkes/notallyx/core/security/ # SQLCipherUtils.java
│       │   └── res/                                    # Shared strings (30 locales), plurals, vector drawables, colors, raw, xml
│       ├── commonTest/kotlin/                          # Portable domain, converter & model extension tests
│       └── androidHostTest/kotlin/                     # Room BundledSQLiteDriver migration tests, DataStore & importer/exporter tests
│
├── composeApp/                                         # [NEW] KMP Presentation & Shared UI Module (Android target only)
│   ├── build.gradle.kts                                # kotlin("multiplatform") + com.android.kotlin.multiplatform.library + Compose
│   └── src/
│       ├── commonMain/kotlin/com/philkes/notallyx/
│       │   ├── core/ui/platform/                       # FilePicker, ShareHandler, PermissionChecker, AppPermission
│       │   └── navigation/                             # NotallyNavKey, NavigationState, NotallyNavSavedStateConfig, Navigator, ListDetailScene
│       ├── androidMain/kotlin/com/philkes/notallyx/
│       │   ├── core/
│       │   │   ├── common/                             # BaseViewModel, ViewModelExtensions, error/ErrorScreen
│       │   │   └── ui/                                 # components/, dialog/, extensions/, platform/ (AndroidFilePicker, AndroidShareHandler, AndroidPermissionChecker, UiText), theme/, views/
│       │   ├── di/                                     # ComposeFeatureModule (shared ViewModels & NoteModel)
│       │   └── feature/                                # 18 Feature Quads (editor/, labels/, main/, media/, reminder/, settings/)
│       ├── commonTest/kotlin/                          # NavigationStateSerializationTest
│       └── androidHostTest/kotlin/                     # Shared ViewModel & UI unit tests
│
├── app/                                                # Android Application Host Module (com.android.application)
│   ├── build.gradle.kts                                # App packaging, signing, buildTypes (debug/beta/release), Roborazzi, Screenshot & TestLab
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   └── java/com/philkes/notallyx/
│       │       ├── NotallyXApplication.kt
│       │       ├── core/
│       │       │   ├── common/error/                   # ErrorActivity, ErrorActivityViewModel, PidCrashDataCollector
│       │       │   ├── platform/                       # AndroidAppInfo, AndroidTaskScheduler
│       │       │   └── security/                       # LockedActivity, BiometricAuthenticator, SystemBiometricAuthenticator, LockUtils, PhoneLockReceiver
│       │       ├── di/                                 # AppModule (platformModule, androidHostModule, appModules)
│       │       └── feature/
│       │           ├── editor/                         # EditActivityViewModel
│       │           ├── main/                           # MainActivity, MainActivityViewModel, AutoRemoveDeletedNotesWorker
│       │           ├── media/audio/                    # AudioPlayService, AudioRecordService, AndroidServiceAudioPlayer, AndroidServiceAudioRecorder
│       │           ├── migration/backup/               # AutoBackupWorker, CleanupMissingAttachmentsWorker, CleanupMissingAttachmentsReceiver, DatabaseTransitionActivity
│       │           ├── reminder/                       # ReminderReceiver, AndroidReminderScheduler, AlarmExtensions, PinnedNotificationManager
│       │           └── widget/                         # Jetpack Glance NoteWidget, NoteWidgetReceiver, ConfigureWidgetActivity, ToggleChecklistAction
│       ├── test/                                       # Android host/worker/Robolectric & Roborazzi screenshot tests
│       ├── screenshotTestDebug/                        # Compose Preview Screenshot tests
│       └── androidTest/                                # Instrumented UI & decryption migration tests
│
├── test-lib/                                           # Shared test fakes & instrumentation utilities
└── build-logic/                                        # Gradle convention & task plugins (updated for KMP modules)
```

**Structure Decision**: Multi-module KMP layout separating `:shared` (Domain/Data), `:composeApp` (Presentation/UI), and `:app` (Android Application Host) with `com.android.kotlin.multiplatform.library` and only the Android target enabled. This satisfies `FR-001` through `FR-011` while keeping all existing `:app` Gradle tasks, test suites, and build variants operational.

## Complexity Tracking

| Violation / Deviation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Hosting shared Android resources (`namespace = "com.philkes.notallyx"`) in `:shared/src/androidMain/res` with transitive `api(project(":shared"))` exposure to `:composeApp` and `:app` | Domain preferences (`Preference.kt`), backup/import progress models, and shared Compose UI all reference `com.philkes.notallyx.R.*`, while `commonMain` source sets remain strictly free of `R.*` and `android.*`. | Keeping `namespace = "com.philkes.notallyx"` in `:app` would create a forbidden circular dependency (`:shared` / `:composeApp` → `:app`), and rewriting all 100+ UI files to `Res.*` simultaneously during module extraction would conflate resource API changes with physical module splitting. |

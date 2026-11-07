# Implementation Plan: Multiplatform Migration Prerequisites (Android & Desktop Readiness)

**Branch**: `002-kmp-migration-prep` | **Date**: 2026-11-01 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-kmp-migration-prep/spec.md` and prerequisite analysis in [`docs/kmp-migration/`](../../docs/kmp-migration/README.md)

## Summary

Prepare the existing single-module **NotallyX** Android codebase for a seamless multi-module **Kotlin Multiplatform (KMP)** extraction targeting **Android** and **Desktop (JVM: Linux, macOS, Windows)**. All 22 prerequisite tasks (`PREP-001` through `PREP-022`) are executed incrementally inside `:app` and `:build-logic` across 6 phases:
1. **Architecture & ViewModel Decoupling**: Decouple `Context`/`ContextWrapper` from `NotallyDatabase` and DAOs; replace `android.net.Uri` with portable `PlatformUri`/relative paths; migrate ViewModels from `AndroidViewModel`/`Application` to platform-independent `androidx.lifecycle.ViewModel`; replace `org.json`, `android.util.*`, `android.text.Html`, `MimeTypeMap`, and `@Parcelize` with portable Kotlin/JVM equivalents; configure polymorphic `SavedStateConfiguration` for `NotallyNavKey`; abstract `ShareHandler` and `PermissionChecker`.
2. **Storage & File Access Abstraction**: Introduce `FileStorage` and `PathProvider` in `:shared`-mapped packages, `FilePicker` in `:composeApp`-mapped packages, and `PdfExporter` wrapping WebView PDF generation.
3. **Dependency, Library & Resource Preparation**: Migrate strings (30 locales), plurals, and shared vector drawables to Compose Multiplatform Resources (`Res.*` + `UiText`), remove `R.*` from ViewModels, decouple `ReminderModelExtensions` from `Context`, and isolate `LocalContext` from shared UI components.
4. **Persistence & Driver Abstraction**: Abstract Room 3 driver creation via `DatabaseDriverFactory` (`BundledSQLiteDriver` for JVM/Desktop), isolate Tink AEAD behind `SecurePreferencesStore`, and sunset SQLCipher database encryption (`FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`) with an automatic one-time startup decryption migration.
5. **Android-Only Feature Encapsulation**: Encapsulate `AlarmManager`/`NotificationManager` behind `ReminderScheduler`, Media3/`MediaRecorder` behind `AudioPlayer`/`AudioRecorder`, `WorkManager` workers behind `TaskScheduler` + suspend use cases, and keep `BiometricPrompt`/`LockedActivity` and Glance widgets isolated to the Android entry layer.
6. **Multi-Module & Testing Preparation**: Separate pure JVM unit tests from Robolectric tests, split `:test-lib` into pure-Kotlin fakes vs. Android UI test utilities, and extract KMP-ready convention plugins in `:build-logic`.

## Technical Context

**Language/Version**: Kotlin 2.4.20 / JVM Target 17  
**Primary Dependencies**: Jetpack Compose (BOM `2026.09.00`) / Compose Multiplatform (≥ 1.10), Material 3 (`1.3.0` adaptive), Jetpack Navigation 3 (`1.2.0`), Lifecycle ViewModel Compose (`2.11.0`), Room 3 (`3.0.3`) + `sqlite-bundled` (`2.7.1`), Jetpack DataStore (`1.3.0`), Koin 4.x (`4.2.2`) + Koin Compiler Plugin (`1.2.1`), `kotlinx-serialization-json` (`1.11.0`), Coil 3 (`3.1.0`), Skydoves ColorPicker (`1.3.0`), `jsoup` (`1.23.2`), `zip4j` (`2.11.6`), `commonmark` (`0.30.0`), `PrettyTime` (`4.0.6.Final`)  
**Storage**: Room 3 SQLite (v11 schema, WAL mode with `pragma wal_checkpoint(TRUNCATE)`), Jetpack DataStore (`UserPreferences`), Google Tink AEAD (`SecurePreferencesStore` on Android), filesystem attachments via `PathProvider` & `FileStorage`  
**Testing**: JUnit 4 + Coroutines Test + `BundledSQLiteDriver` pure JVM unit tests (`./gradlew testDebugUnitTest`), Robolectric 4.17 for Android platform implementations, Roborazzi (`./gradlew verifyRoborazziDebug`), Compose Screenshot tests (`./gradlew validateDebugScreenshotTest`), AndroidX Instrumented UI tests (`./gradlew connectedDebugAndroidTest`)  
**Target Platform**: Android (Min SDK 24, Compile/Target SDK 37) and Desktop JVM (Linux, macOS, Windows)  
**Project Type**: Mobile + Desktop Multiplatform Application (in-place prerequisite preparation within `:app`, `:test-lib`, and `:build-logic` prior to module split)  
**Performance Goals**: 60/120 FPS UI rendering, zero main-thread blocking I/O, fast pure-JVM test execution (< 10s for domain/converter/ViewModel unit suites), 100% lossless one-time decryption migration  
**Constraints**: Zero data loss across Room v1–v11 schemas and backups; 100% byte-compatible JSON converter output; all 30 translated locales preserved; AGP 9.4.1 compatibility; strict `ktfmt` (`kotlinLangStyle`) compliance  
**Scale/Scope**: ~30 ViewModels, 18 feature routes (4-File Feature Quads), 3 DAOs, 11 DB schema versions, 30 locales, 22 `PREP-*` work packages across `:app`, `:test-lib`, and `:build-logic`  

## Constitution Check

*GATE: Passed before Phase 0 research. Re-verified after Phase 1 design.*

- **Principle I: Compose-First UI & Zero XML**: **PASS**. 100% Compose Material 3 UI is preserved. Migrating shared strings/drawables to Compose Multiplatform Resources (`Res.string.*`) upholds the zero-hardcoded-strings mandate across all 30 locales while enabling multiplatform compilation.
- **Principle II: Feature-First MVI & The 4-File Feature Quad**: **PASS**. Every feature retains its 4-File Feature Quad (`Model.kt`, `ViewModel.kt`, `Screen.kt`, `Route.kt`). ViewModels become cleaner and strictly platform-independent (`androidx.lifecycle.ViewModel` + `UiText` + platform interfaces).
- **Principle III: Data Integrity & Non-Destructive Storage**: **PASS**. All Room v1–v11 schemas and migrations are preserved. `NotallyDatabase.checkpoint()` (`pragma wal_checkpoint(TRUNCATE)`) is enforced before any file copy, backup, or one-time decryption swap. `Converters.kt` replacement with `kotlinx.serialization.json` is verified by round-trip byte-compatibility tests.
- **Principle IV: Privacy-First & Offline-First Security**: **PASS (with documented evolution)**. Offline-first operation, `LockedActivity` biometric app lock, and Tink AEAD `SecurePreferencesStore` on Android are preserved. SQLCipher database encryption is retired via `FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false` with an automatic startup decryption migration so databases are portable to Desktop `BundledSQLiteDriver` (documented in Complexity Tracking below).
- **Principle V: Strict Thread & Concurrency Discipline**: **PASS**. All DB, DataStore, file I/O, PDF export, and backup operations run on `Dispatchers.IO`; diff/search on `Dispatchers.Default`; UI state on `Dispatchers.Main`.
- **Principle VI: Comprehensive Multi-Layer Testing**: **PASS**. Pure JVM unit tests are expanded for ViewModels, Converters, Navigation serialization, and Room (`BundledSQLiteDriver`), while Robolectric, Roborazzi, Compose Screenshot, and `androidTest` suites continue to guard Android behavior.
- **Principle VII: Code Health, Formatting & Idiomatic Kotlin**: **PASS**. All code adheres to `ktfmt` (4-space indent) and Koin 4.x Safe DSL with Koin Compiler Plugin.

## Project Structure

### Documentation (this feature)

```text
specs/002-kmp-migration-prep/
├── plan.md                         # This file (/speckit-plan command output)
├── research.md                     # Phase 0 technical decisions & rationale
├── data-model.md                   # Phase 1 portable entity & state machine models
├── quickstart.md                   # Phase 1 runnable validation & gate checks
├── contracts/                      # Phase 1 platform boundary interfaces
│   └── platform-contracts.md
├── checklists/
│   └── requirements.md             # Spec quality checklist (/speckit-specify output)
└── tasks.md                        # Phase 2 task breakdown (created by /speckit-tasks)
```

### Source Code (repository root)

During this prerequisite initiative, all code remains in `:app`, `:test-lib`, and `:build-logic`, organized into clear package boundaries that map 1:1 to the future `:shared`, `:composeApp`, and `:androidApp` modules:

```text
app/src/main/
├── java/com/philkes/notallyx/
│   ├── core/
│   │   ├── platform/               # [NEW - maps to :shared] PathProvider, FileStorage, PlatformUri, TaskScheduler, Logger
│   │   ├── database/               # [maps to :shared] NotallyDatabase, Converters (kotlinx.serialization), DatabaseDriverFactory
│   │   ├── preference/             # [maps to :shared] NotallyXPreferences, UserPreferences, SecurePreferencesStore
│   │   ├── security/               # [maps to :androidApp] BiometricAuthenticator, LockedActivity, EncryptionUtils (decryption migration)
│   │   └── ui/
│   │       ├── platform/           # [NEW - maps to :composeApp] FilePicker, ShareHandler, PermissionChecker, UiText
│   │       ├── components/         # [maps to :composeApp] BaseNoteCard, ReminderChip, LabelChip (zero LocalContext)
│   │       └── dialog/color/       # [maps to :composeApp] ColorPickerControls, ColorPickerDialog
│   ├── data/
│   │   ├── dao/                    # [maps to :shared] BaseNoteDao, LabelDao, CommonDao, DaoExtensions (zero Context)
│   │   └── model/                  # [maps to :shared] BaseNote, Label, Attachment, Audio, FileAttachment, Reminder (zero Uri/Parcelable)
│   ├── di/                         # Koin modules split by shared vs. Android platform bindings
│   ├── feature/
│   │   ├── editor/                 # [maps to :composeApp] EditNote/EditList Feature Quads (platform-agnostic ViewModels)
│   │   ├── labels/                 # [maps to :composeApp] Labels Feature Quad
│   │   ├── main/                   # [maps to :composeApp + :androidApp MainActivity]
│   │   ├── media/                  # [maps to :shared interfaces + :composeApp UI + :androidApp Media3/MediaRecorder services]
│   │   ├── migration/              # [maps to :shared] Importers, Exporters, PdfExporter, Backup use cases
│   │   ├── reminder/               # [maps to :shared ReminderScheduler + :composeApp UI + :androidApp AlarmManager/Receivers]
│   │   ├── settings/               # [maps to :composeApp] Settings Feature Quad
│   │   └── widget/                 # [maps to :androidApp] Jetpack Glance NoteWidget
│   └── navigation/                 # [maps to :composeApp] NotallyNavKey, NavigationState, NotallyNavSavedStateConfig
├── composeResources/               # [NEW - maps to :composeApp] Multiplatform strings (30 locales), plurals, shared vector drawables
└── res/                            # [maps to :androidApp] Android-only mipmaps, xml provider paths, widget descriptors, notification icons

test-lib/src/main/java/com/philkes/notallyx/test/
├── fakes/                          # Pure Kotlin fakes & test fixtures (FakeFileStorage, FakePathProvider, FakeReminderScheduler, etc.)
└── android/                        # Android-only instrumentation helpers (UiTestBase, UiUtils)

build-logic/src/main/kotlin/com/philkes/notallyx/buildlogic/
└── convention/                     # Reusable Kotlin & Android/KMP convention plugins
```

**Structure Decision**: In-place modular package architecture inside `:app`, `:test-lib`, and `:build-logic`. Packages are strictly partitioned by their target KMP module (`:shared`, `:composeApp`, `:androidApp`) so that once all 13 readiness checklist gates (`CH-01`–`CH-13`) pass, physical module extraction is a mechanical file move.

## Complexity Tracking

| Violation / Deviation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Retiring SQLCipher database encryption (`FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`) with one-time startup decryption migration (Principle IV evolution) | Desktop JVM (`BundledSQLiteDriver`) and cross-platform database backups require standard unencrypted SQLite files; SQLCipher for Android is Android-only. | Leaving SQLCipher active on Android prevents opening exported databases on Desktop, and dropping SQLCipher without an automatic decryption migration would lock existing encrypted-DB users out of their notes. |
| Introducing platform boundary interfaces (`PathProvider`, `FileStorage`, `FilePicker`, `DatabaseDriverFactory`, `PdfExporter`, `ReminderScheduler`, `TaskScheduler`, `AudioPlayer`, `AudioRecorder`, `ShareHandler`, `PermissionChecker`) | ViewModels, DAOs, and domain services in `commonMain` cannot reference Android `Context`, `Uri`, `DocumentFile`, `WebView`, `AlarmManager`, `WorkManager`, or `Media3`. | Direct platform calls inside ViewModels and DAOs block compilation on non-Android targets and require Robolectric for unit testing. |

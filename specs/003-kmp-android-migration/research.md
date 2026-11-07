# Technical Research & Decisions: Kotlin Multiplatform Project Migration (Android Target Only)

**Feature**: `003-kmp-android-migration` ([spec.md](spec.md))  
**Baseline**: [`002-kmp-migration-prep`](../002-kmp-migration-prep/spec.md) & [`docs/kmp-migration/`](../../docs/kmp-migration/README.md)  
**Date**: 2026-11-01  
**Status**: Completed  

---

## 1. Module Topology & Naming Strategy (`:shared`, `:composeApp`, `:app`, `:test-lib`, `:build-logic`)

### Decision
Extract two new Kotlin Multiplatform library modules—`:shared` (`shared/`) and `:composeApp` (`composeApp/`)—while retaining `:app` (`app/`) as the Android Application Host module (corresponding to `androidApp` in architectural diagrams):
- **`:shared`**: Pure Domain & Data KMP module (`commonMain`, `androidMain`, `commonTest`, `androidHostTest`).
- **`:composeApp`**: Shared Presentation & Compose UI KMP module (`commonMain`, `androidMain`, `commonTest`, `androidHostTest`), depending on `api(project(":shared"))`.
- **`:app`**: Android Application Entry module (`com.android.application`), depending on `implementation(project(":composeApp"))` and `implementation(project(":shared"))`.
- **`:test-lib`**: Shared test utilities and fakes supporting unit and instrumented tests across modules.

### Rationale
- Enforces the strict 3-tier unidirectional dependency graph (`:app` → `:composeApp` → `:shared`) required by `FR-001` and `FR-004`.
- Retaining `:app` (`app/`) as the Gradle subproject path for the Android application host preserves 100% compatibility with existing developer commands (`./gradlew assembleDebug`, `./gradlew assembleBeta`, `./gradlew testDebugUnitTest`, `./gradlew verifyRoborazziDebug`, `./gradlew validateDebugScreenshotTest`, `./gradlew connectedDebugAndroidTest`), Android Studio run configurations, Fastlane screenshot paths (`app/src/screenshotTestDebug`), and Roborazzi reference directories (`app/src/screenshots`).
- Defers `:desktopApp` and `jvm("desktop")` target configuration cleanly to the subsequent Desktop target feature (`FR-002`).

### Alternatives Considered
- *Renaming `:app` directory to `:androidApp` immediately*: Rejected because it breaks existing root/module Gradle task invocations, IDE run configurations, Fastlane paths, and screenshot test paths without adding architectural value over keeping `:app` as the slim Android host module.
- *Single combined KMP library module (`:shared` containing both data and Compose UI)*: Rejected because keeping `:shared` free of Compose and ViewModels enforces clean domain/data separation and prevents circular dependencies (`FR-001`, `FR-004`).

---

## 2. AGP 9.4.1 & Kotlin 2.4.20 Multiplatform Library Plugin (`com.android.kotlin.multiplatform.library`)

### Decision
Apply `org.jetbrains.kotlin.multiplatform` together with the official Android-KMP Library plugin `com.android.kotlin.multiplatform.library` (AGP `9.4.1`) in both `:shared` and `:composeApp`, configuring only the `android` target block (`kotlin { android { ... } }`) with `compileSdk = 37`, `minSdk = 24`, `JvmTarget.JVM_17`, `withJava()`, and `withHostTest { isIncludeAndroidResources = true }`.

### Rationale
- Under AGP 9.0+, built-in Kotlin and the new public DSL interfaces are mandatory defaults; combining `org.jetbrains.kotlin.multiplatform` with `com.android.library` or `com.android.application` in the same subproject is disallowed without legacy opt-out flags.
- `com.android.kotlin.multiplatform.library` uses a single-variant architecture optimized for KMP libraries and natively separates `src/commonMain`, `src/androidMain`, `src/commonTest`, and `src/androidHostTest`.
- Build variants (`debug`, `beta`, `release`), app signing, and R8 shrinking remain owned by the `:app` (`com.android.application`) host module (`FR-009`).

### Alternatives Considered
- *Using legacy `com.android.library` with `kotlin("multiplatform")` and `android.newDsl=false`*: Rejected because `android.newDsl=false` is deprecated in AGP 9.4 and scheduled for removal, and violates project guidelines against legacy AGP workarounds.

---

## 3. Physical Separation of `commonMain` Contracts vs. `androidMain` Implementations

### Decision
Split every hybrid file created during `002-kmp-migration-prep` (where common interfaces and `Android*` implementations coexisted in a single `.kt` file inside `:app`) into two distinct source-set files:
1. **`commonMain`**: Pure Kotlin interfaces, value classes, data models, DAOs, portable converters, and platform-agnostic implementations (`PathProvider` + `DesktopPathProvider`, `DatabaseDriverFactory` + `JvmBundledDatabaseDriverFactory`, `FileStorage`, `PdfExporter`, `SecurePreferencesStore`, `ReminderScheduler` + `NoOpReminderScheduler`, `TaskScheduler` + `NoOpTaskScheduler`, `AudioPlayer` + `NoOpAudioPlayer`, `AudioRecorder` + `NoOpAudioRecorder`, `FilePicker`, `ShareHandler`, `PermissionChecker`, `UiText`, `Logger`, `AppInfo`, `PlatformUri`, `MimeTypeUtils`).
2. **`androidMain` (or `:app`)**: Android-specific implementations (`AndroidPathProvider`, `AndroidDatabaseDriverFactory`, `BackupSQLiteDriver`, `BackupOpenHelperFactory`, `AndroidFileStorage`, `AndroidPdfExporter`, `AndroidSecurePreferencesStore`, `AndroidReminderScheduler`, `AndroidTaskScheduler`, `AndroidServiceAudioPlayer`, `AndroidServiceAudioRecorder`, `AndroidFilePicker`, `AndroidShareHandler`, `AndroidPermissionChecker`, `AndroidAppInfo`).

### Rationale
- Satisfies `FR-003` and `SC-001`: 100% of files in `shared/src/commonMain` and `composeApp/src/commonMain` contain zero `import android.*` statements.
- Ensures that when `jvm("desktop")` is added in the next feature, `commonMain` requires zero file splits or Android import removals.

### Alternatives Considered
- *Using `expect`/`actual` classes for every service instead of interfaces*: Rejected because constructor-injected interfaces (`PathProvider`, `FileStorage`, `ReminderScheduler`, etc.) are already established from `002-kmp-migration-prep`, integrate seamlessly with Koin 4.x DI, and allow easy test fake substitution (`FakePathProvider`, `FakeFileStorage`) in `commonTest`.

---

## 4. Room 3 KMP & KSP Configuration in `:shared`

### Decision
Move `NotallyDatabase`, `Converters`, `BaseNoteDao`, `LabelDao`, `CommonDao`, `DaoExtensions`, all domain entities (`BaseNote`, `Label`, `Attachment`, `Audio`, `FileAttachment`, `Reminder`, `ListItem`, `SpanRepresentation`, etc.), and the Room schema directory (`schemas/com.philkes.notallyx.core.database.NotallyDatabase/1.json`–`11.json`) into `:shared`:
- Configure KSP (`com.google.devtools.ksp` `2.3.12`) in `:shared` with `add("kspAndroid", libs.androidx.room3.compiler)` and `ksp { arg("room.generateKotlin", "true"); arg("room.schemaLocation", "$projectDir/schemas") }`.
- Keep the `@Database`-annotated `NotallyDatabase` class and `ALL_MIGRATIONS` (`Migration2`–`Migration11` using `SQLiteConnection` APIs) in `:shared` while isolating Android `ContextWrapper` factory helpers, `BackupSQLiteDriver`, `BackupOpenHelperFactory`, `DataSchemaMigrations`, and SQLCipher startup decryption (`EncryptionUtils`, `SQLCipherUtils`) in `shared/src/androidMain`.

### Rationale
- Room 3 (`3.0.3`) and `androidx.sqlite` (`2.7.1`) are KMP-native. Running Room KSP in `:shared` generates the Kotlin database implementation once for the data layer and keeps all v1–v11 schema definitions co-located with the database entities (`FR-007`).
- Removing `StaticTextProvider` (`R.string.*`) from `NoteViewMode` eliminates the last resource reference from core database entities in `shared/src/commonMain`.

### Alternatives Considered
- *Keeping `NotallyDatabase` in `:app` and only moving entities to `:shared`*: Rejected because DAOs, repositories, and shared ViewModels in `:composeApp` depend on `NotallyDatabase` and DAOs, which must reside in `:shared` (`FR-001`, `FR-004`).

---

## 5. Resource Namespace & 30-Locale Multiplatform Accessibility Strategy

### Decision
Enable Android resources (`androidResources { enable = true }`) in `:shared` with `namespace = "com.philkes.notallyx"` (hosting the shared `values*/strings.xml` across all 30 locales, `drawable/`, `color/`, `raw/`, and `xml/` resources in `shared/src/androidMain/res` alongside Compose Multiplatform resources support in `:composeApp`), while configuring `namespace = "com.philkes.notallyx.composeapp"` in `:composeApp` and `namespace = "com.philkes.notallyx.app"` (with `applicationId = "com.philkes.notallyx"`) in `:app`:
- Re-export `:shared` from `:composeApp` via `api(project(":shared"))` so `com.philkes.notallyx.R` and shared resource descriptors are transitively visible across `:shared/androidMain`, `:composeApp`, and `:app`.
- Keep `AndroidAppInfo` in `:app` (referencing `com.philkes.notallyx.app.BuildConfig`) so `VERSION_CODE` and `VERSION_NAME` are injected via the `AppInfo` contract without any KMP module depending on `BuildConfig`.
- Update `LocalizationPlugin` (`GenerateLocalesPreviewTask`), `autoTranslate` tasks, and `.github/scripts/update_translation_coverage.py` to support the modularized resource directory path.

### Rationale
- In Android Gradle Plugin, `applicationId = "com.philkes.notallyx"` in `:app` controls the published package identity on devices and app stores, while `namespace` controls the generated `R` and `BuildConfig` package.
- Hosting `namespace = "com.philkes.notallyx"` in the base `:shared` module allows all 100+ existing resource references (`com.philkes.notallyx.R.*`) in `:shared/androidMain`, `:composeApp`, and `:app` to resolve without circular dependencies, while `commonMain` source sets remain strictly free of `android.*` and `R.*` imports (`FR-003`, `FR-006`, `FR-009`).

### Alternatives Considered
- *Keeping `namespace = "com.philkes.notallyx"` on `:app`*: Rejected because `:shared` and `:composeApp` cannot depend on `:app` (`FR-004`), which would break every `com.philkes.notallyx.R` reference inside `:shared` and `:composeApp`.

---

## 6. Cross-Module Koin 4.x Dependency Injection Partitioning

### Decision
Partition Koin modules across the three layers in accordance with `FR-008`:
1. **`:shared` (`com.philkes.notallyx.di`)**:
   - `databaseModule` and `preferencesModule` providing `PathProvider`, `DatabaseDriverFactory`, `NotallyDatabase`, `SecurePreferencesStore`, `NotallyXPreferences`, and `DataStore` instances.
2. **`:composeApp` (`com.philkes.notallyx.di`)**:
   - `composeFeatureModule` providing `ActionMode`, `NoteModel`, and all shared feature ViewModels (`NotesRouteViewModel`, `NotesViewModel`, `ArchivedViewModel`, `DeletedViewModel`, `DisplayLabelViewModel`, `LabelsViewModel`, `SelectLabelsViewModel`, `RemindersViewModel`, `NoteRemindersViewModel`, `SettingsViewModel`, `UnlabeledViewModel`, `SearchViewModel`, `PickNoteViewModel`, `ViewImageViewModel`, `PlayAudioViewModel`, `RecordAudioViewModel`, `EditNoteViewModel`, `EditListViewModel`, `EditTextPlainViewModel`).
3. **`:app` (`com.philkes.notallyx.di`)**:
   - `platformModule` and `androidHostModule` providing `AndroidAppInfo`, `AndroidFileStorage`, `AndroidFilePicker`, `AndroidPdfExporter`, `AndroidServiceAudioPlayer`, `AndroidServiceAudioRecorder`, `AndroidShareHandler`, `AndroidPermissionChecker`, `AndroidReminderScheduler`, `AndroidTaskScheduler`, `AutoRemoveDeletedNotesWorker` (`workerOf`), and Android-only Activity ViewModels (`MainActivityViewModel`, `EditActivityViewModel`, `ErrorActivityViewModel`).
   - `appModules = listOf(platformModule, preferencesModule, databaseModule, composeFeatureModule, androidHostModule)` initialized in `NotallyXApplication.onCreate()`.

### Rationale
- Ensures `:shared` only references domain/data classes, `:composeApp` only references shared presentation and `:shared` contracts, and `:app` wires the concrete Android OS services (`WorkManager`, `Media3` services, `AlarmManager`, `BuildConfig`) at startup.

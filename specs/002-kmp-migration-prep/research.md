# Technical Research & Decisions: Multiplatform Migration Prerequisites

**Feature**: `002-kmp-migration-prep` ([docs/kmp-migration/](../../docs/kmp-migration/README.md))  
**Date**: 2026-11-01  
**Status**: Completed  

---

## 1. Execution Strategy: In-Place Prerequisite Refactoring Before Module Extraction

### Decision
Execute all 22 prerequisite tasks (`PREP-001` through `PREP-022`) incrementally inside the existing single-module `:app` project before introducing the multi-module KMP layout (`:shared`, `:composeApp`, `:androidApp`, `:desktopApp`). Code destined for `:shared/commonMain` or `:composeApp/commonMain` is placed in packages that map 1:1 to those future source sets and must be free of Android-only imports.

### Rationale
- Keeps the Android application buildable, testable, and releasable after every commit.
- Isolates API decoupling issues (Context removal, `org.json` replacement, `Uri` abstraction, Compose Multiplatform Resources migration) from Gradle multi-module KMP build configuration complexity.
- Provides a clear, objective Go/No-Go gate (`07-migration-readiness-checklist.md`, `CH-01`–`CH-13`) before physical file moves.

### Alternatives Considered
- *Big-bang module split first*: Rejected because moving hundreds of files with `android.*` imports into `commonMain` source sets breaks compilation across the entire repository at once and blocks incremental testing.

---

## 2. Target Platforms & JVM-Shared Baseline

### Decision
Target exclusively **Android** (`androidTarget()`) and **Desktop JVM** (`jvm("desktop")` on Linux, macOS, Windows). Because both targets run on the JVM, `commonMain` may continue to use standard JDK APIs (`java.io.File`, `java.nio`, `java.time`, `java.util`) and pure JVM libraries (`jsoup`, `commonmark`, `zip4j`, `simple-xml`, `PrettyTime`). Only Android framework APIs (`android.*`, Android-only `androidx.*` artifacts, and Android's bundled `org.json`) are blockers.

### Rationale
- Avoids unnecessary rewrites of working JVM libraries (`zip4j`, `jsoup`, `commonmark`, `PrettyTime`) that already run identically on Android and Desktop JVM.
- Focuses engineering effort strictly on actual Android framework coupling (`Context`, `Uri`, `AndroidViewModel`, `R.*`, `DocumentFile`, `WebView`, `Media3`, `WorkManager`, `AlarmManager`, `BiometricPrompt`, `SQLCipher`).

### Alternatives Considered
- *Rewriting all JVM libraries to Kotlin-native equivalents (`okio`, `kotlinx-datetime`, custom ZIP/HTML parsers) now*: Rejected as unnecessary scope creep since non-JVM targets (iOS, Web/Wasm) are explicitly out of scope.

---

## 3. ViewModel Placement & Navigation 3 State Serialization (`PREP-003`, `PREP-021`)

### Decision
1. Place all shared feature ViewModels in the presentation layer (`:composeApp/commonMain`), inheriting from `androidx.lifecycle.ViewModel` (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`), and remove all `AndroidViewModel` inheritance and `Application` constructor parameters.
2. Keep `rememberSaveableStateHolderNavEntryDecorator()` and `rememberViewModelStoreNavEntryDecorator()` in `NavigationState.kt`.
3. Configure an explicit `SavedStateConfiguration` with a polymorphic `SerializersModule` registering all `NotallyNavKey` subtypes for `rememberNavBackStack`.

### Rationale
- Keeps `:shared` strictly focused on Domain and Data layers and prevents circular dependencies between ViewModels and UI models/screens.
- On non-Android targets, Navigation 3 cannot use Android reflection-based `Parcelable`/`Serializable` bundle saving and requires explicit polymorphic `SerializersModule` registration for `NavKey` subtypes.
- Adding `kotlinx-coroutines-swing` on Desktop provides `Dispatchers.Main.immediate` required by `viewModelScope`.

### Alternatives Considered
- *Putting ViewModels in `:shared`*: Rejected because ViewModels reference Compose UI state models, Compose `TextFieldValue`, and UI resource types (`StringResource`), which belong in `:composeApp`.

---

## 4. Portable JSON Type Converters & Android Utility Replacement (`PREP-020`)

### Decision
- Replace `org.json` (`JSONArray`, `JSONObject`) in `Converters.kt` and `ModelExtensions.kt` with `kotlinx.serialization.json` (`JsonElement` / `buildJsonArray` / `buildJsonObject`), maintaining 100% byte-compatible JSON output and lenient legacy parsing.
- Replace `android.util.Base64` with `kotlin.io.encoding.Base64`.
- Replace `android.text.Html` with `jsoup` (already in `libs.versions.toml`).
- Replace `android.webkit.MimeTypeMap` with a deterministic extension-to-MIME lookup table + `URLConnection.guessContentTypeFromName`.
- Replace `android.util.Log` with an injectable/multiplatform `Logger` facade and `ConverterErrorReporter` interface.
- Remove `@Parcelize` / `android.os.Parcelable` from domain models (`Attachment`, `Audio`, `FileAttachment`, `Reminder`), relying on `@Serializable`.

### Rationale
- `org.json` is bundled in the Android framework stub jar and is absent on standard Desktop JVM. Furthermore, the standalone `org.json:json` Maven artifact has subtle behavioral differences from Android's fork (e.g., `JSONObject.NULL` and `optString` behavior), whereas `kotlinx.serialization.json` is already a project dependency and gives full control over exact JSON formatting.

### Alternatives Considered
- *Adding `org.json:json` to production dependencies*: Rejected due to behavioral discrepancies with Android's built-in `org.json` implementation and duplication with `kotlinx.serialization.json`.

---

## 5. Database Driver Abstraction & SQLCipher Encryption Sunset (`PREP-001`, `PREP-010`, `PREP-012`)

### Decision
1. Abstract database creation and SQLite driver selection behind `DatabaseDriverFactory` and `PathProvider`, eliminating `Context`/`ContextWrapper` from `NotallyDatabase`, DAOs (`BaseNoteDao`, `CommonDao`, `DaoExtensions`), and `SearchResult`.
2. Use `BundledSQLiteDriver` (`androidx.sqlite:sqlite-bundled`) for pure JVM unit tests and future Desktop JVM execution, while preserving Android's WAL checkpoint (`pragma wal_checkpoint(TRUNCATE)`) and single-source schema version (`DATABASE_VERSION = 11`).
3. Sunset SQLCipher database encryption via `const val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`:
   - Hide the "Encrypt Database" toggle in Settings.
   - Stop `DataSchemaMigrations` from enabling `biometricLockEncryptsDb`.
   - On Android startup, if `isEncryptedDatabase()` detects an existing encrypted database, run a one-time decryption migration after biometric authentication (checkpoint WAL, decrypt via SQLCipher export/copy, atomically swap files, and set `biometricLockEncryptsDb = false`).

### Rationale
- `BundledSQLiteDriver` on Desktop does not support SQLCipher, and encrypted Android databases cannot be shared or restored on Desktop without decryption.
- Automatically decrypting existing encrypted databases on startup (reusing the proven `DatabaseTransitionActivity` decryption routine) guarantees zero data loss for existing users while converging all installations on standard unencrypted SQLite.

### Alternatives Considered
- *Dropping encrypted database support without a startup decryption migration*: Rejected because existing Android users with `biometricLockEncryptsDb = true` would be locked out of their notes upon upgrading.
- *Shipping SQLCipher native binaries for Linux/macOS/Windows Desktop*: Rejected due to native cross-compilation licensing/packaging complexity and incompatibility with standard `BundledSQLiteDriver`.

---

## 6. Storage, Paths, File Picker & PDF Export (`PREP-002`, `PREP-004`, `PREP-005`, `PREP-006`)

### Decision
- **`PathProvider` (`:shared`)**: Resolves database, backup, media, and cache directories. On Android, delegates to internal app storage or the legacy `dataInPublicFolder` external media directory. On Desktop, resolves to OS-standard application data directories (`$XDG_DATA_HOME/NotallyX` or `~/.local/share/NotallyX` on Linux, `~/Library/Application Support/NotallyX` on macOS, `%APPDATA%\NotallyX` on Windows).
- **`FileStorage` (`:shared`) & `PlatformFile`**: Abstracts stream/byte I/O, file copying, deletion, and metadata lookup without exposing `android.net.Uri` or `DocumentFile` outside `AndroidFileStorage`.
- **`FilePicker` (`:composeApp`)**: Abstracts user-initiated file and folder selection (`pickImportFile`, `pickExportFolder`, `pickAttachment`), backed by Activity Result / SAF launchers on Android and `JFileChooser` / `FileDialog` on Desktop.
- **`PdfExporter` (`:shared`)**: Accepts shared HTML export output and writes a PDF asynchronously (`suspend`), encapsulating Android's `WebView` + `PrintDocumentAdapter` (`android/print/PdfExtensions.kt`) inside `AndroidPdfExporter` and removing `PdfPrintListener` from ViewModels.

### Rationale
- Cleanly separates headless background file I/O (`FileStorage` usable by importers, exporters, and background tasks) from interactive UI file dialogs (`FilePicker` scoped to Compose routes).
- Reuses NotallyX's existing HTML note export generator across platforms so PDF rendering only needs an HTML-to-PDF backend per platform.

### Alternatives Considered
- *Passing `java.io.File` everywhere on Android*: Rejected because Android SAF URIs (`content://`) from document pickers and third-party providers are not backed by direct `java.io.File` paths and require `ContentResolver` streams inside `AndroidFileStorage`.

---

## 7. Compose Multiplatform Resources & 30-Locale Localization (`PREP-019`)

### Decision
- Introduce a platform-agnostic `UiText` wrapper (supporting `StringResource`, plural resources, and formatted arguments) to replace `@StringRes Int` and `R.string.*` in ViewModels, `*Model.kt` `SideEffect`s, and domain extensions.
- Migrate shared `strings.xml` (across all 30 translated locales) and shared vector drawables to Compose Multiplatform Resources (`composeResources/`), while keeping Android-only resources (launcher mipmaps, widget provider XMLs, notification icons, activity themes) in `app/src/main/res/`.
- Update `LocalizationPlugin` / `GenerateLocalesPreviewTask` and translation scripts in `build-logic` to operate on the multiplatform resources directory.

### Rationale
- `com.philkes.notallyx.R` is generated by AGP only for Android targets and cannot be referenced from `commonMain`.
- Introducing `UiText` decouples ViewModels from Android `Context.getString(...)` while preserving full localization across all 30 languages.

### Alternatives Considered
- *Custom JSON/properties localization system*: Rejected because Compose Multiplatform Resources (`org.jetbrains.compose.resources`) is the official JetBrains standard, supports XML strings/plurals/drawables, and generates type-safe `Res.string.*` and `Res.drawable.*` accessors.

---

## 8. Encapsulation of Android-Only Features (`PREP-011`, `PREP-013`, `PREP-014`, `PREP-015`, `PREP-016`, `PREP-022`)

### Decision
Define slim platform contracts in `:shared` and `:composeApp` for system services:
- `SecurePreferencesStore`: Wraps Tink AEAD (`datastore-tink` + Android KeyStore) on Android; standard DataStore on Desktop.
- `ReminderScheduler`: Wraps `AlarmManager` and `NotificationManager` / `PinnedNotificationManager` on Android; binds to a safe No-Op implementation on Desktop (and in JVM unit tests) while keeping reminder metadata editable in notes.
- `TaskScheduler`: Wraps `WorkManager` (`AutoBackupWorker`, `AutoRemoveDeletedNotesWorker`, `CleanupMissingAttachmentsWorker`) on Android, while extracting the core backup, trash-purge, and attachment-cleanup logic into plain `suspend` use cases in `:shared`.
- `AudioPlayer` & `AudioRecorder`: Wraps `AudioPlayService` (Media3 ExoPlayer) and `AudioRecordService` (`MediaRecorder`) on Android, exposing reactive playback/recording state and capability flags to `PlayAudioViewModel` and `RecordAudioViewModel`.
- `ShareHandler`, `ExternalLauncher` & `PermissionChecker`: Wraps `Intent.ACTION_SEND`, `FileProvider`, email intents, and `PackageManager`/`Manifest` checks on Android.
- `BiometricAuthenticator` & `LockedActivity` and Jetpack Glance `NoteWidget`: Remain strictly Android-only in the mobile entry layer.

### Rationale
- Preserves 100% of existing Android functionality and user experience while ensuring shared ViewModels and repositories have zero compile-time or runtime dependency on Android OS services.

### Alternatives Considered
- *Implementing full Desktop OS background daemons and system tray alarms during the prerequisite phase*: Rejected to keep prerequisite scope focused on decoupling; Desktop starts with safe No-Op schedulers and can add in-process schedulers post-extraction.

# 01. Current Architecture & Codebase Analysis

## Overview

This document provides a comprehensive structural and architectural audit of **NotallyX**, based directly on the project's source code, Gradle configurations, and package structures as of repository state.

---

## Project Structure & Gradle Setup

NotallyX is currently organized as a single-application Android project with a custom Gradle build-logic inclusion and a dedicated test helper library.

```
NotallyX/
├── app/                                               # Main Android Application Module (:app)
│   ├── build.gradle.kts                               # AGP application configuration & dependencies
│   ├── schemas/                                       # Room 3 schema JSON exports (v1 - v11)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml                    # Manifest, permissions, receivers, services
│       │   └── java/com/philkes/notallyx/             # Kotlin source root
│       ├── test/                                      # Unit & Robolectric test suite (JVM)
│       ├── androidTest/                               # Instrumented UI tests (device/emulator)
│       └── screenshotTestDebug/                       # Compose Screenshot Testing previews
├── test-lib/                                          # Shared Test Infrastructure Library (:test-lib)
│   └── build.gradle.kts                               # Android library module for test utilities
├── build-logic/                                       # Included Gradle Build Logic (:build-logic)
│   └── src/main/kotlin/.../buildlogic/                # Custom packaging, localization, screenshot, developer plugins (task plugins, NOT convention plugins)
├── build.gradle.kts                                   # Root build script
├── settings.gradle.kts                                # Subproject inclusions (:app, :test-lib)
└── gradle/
    └── libs.versions.toml                             # Centralized Version Catalog
```

### Key Tooling & Versions

- **Kotlin Version**: `2.4.20`
- **Android Gradle Plugin (AGP)**: `9.4.1`
- **KSP Version**: `2.3.12`
- **JVM Target**: `17` (Java 17)
- **Compile / Target SDK**: `37`
- **Min SDK**: `24` (Android 7.0)
- **Code Formatter**: `ktfmt` 0.27.0 (`kotlinLangStyle`)

---

## Architectural Pattern: 4-File Feature Quad (MVI)

NotallyX mandates a strict **Feature-First MVI (Model-View-Intent)** architecture for UI destinations, referred to as the **4-File Feature Quad**:

```
feature/<feature_name>/
├── <Feature>Model.kt          # UI Contract: UiState, DialogState, Action (Intents), SideEffect
├── <Feature>ViewModel.kt      # Central Logic: Exposes StateFlow<UiState> & Channel<SideEffect>
├── <Feature>Screen.kt         # Stateless @Composable UI rendering UiState, sinks onAction
└── <Feature>Route.kt          # Stateful Coordinator: Injects ViewModel, collects state/effects
```

### Example Quad Structure in Code:
- **Editor Feature**: [`EditNoteModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditNoteModel.kt), [`EditNoteViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditNoteViewModel.kt), [`EditScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/EditScreen.kt), [`EditNoteRoute.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditNoteRoute.kt)
- **Labels Feature**: [`LabelsModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelsModel.kt), [`LabelsViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelsViewModel.kt), [`LabelsScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelsScreen.kt), [`LabelsRoute.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/routes/labels/LabelsRoute.kt)

---

## Layer-by-Layer Architectural Analysis

### 1. Presentation & UI Layer

- **Framework**: 100% Jetpack Compose with Material 3 (`androidx.compose.material3`).
- **Adaptive Layouts**: Uses [`androidx.compose.material3.adaptive`](file:///home/user/AndroidStudioProjects/NotallyX/gradle/libs.versions.toml#L53) with custom [`ListDetailScene`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/navigation/ListDetailScene.kt) for multi-pane tablet / foldable list-detail support.
- **Navigation**: Jetpack Navigation 3 (`androidx.navigation3.runtime`, `androidx.navigation3.ui`, `androidx.lifecycle:lifecycle-viewmodel-navigation3`) using type-safe `@Serializable` [`NotallyNavKey`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/navigation/NotallyNavKey.kt) entries with `NavDisplay` in [`NotallyApp.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/NotallyApp.kt). Per-top-level-route back stacks live in [`NavigationState.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/navigation/NavigationState.kt), which **already** applies `rememberSaveableStateHolderNavEntryDecorator()`, `rememberViewModelStoreNavEntryDecorator()` and a custom result-event-bus decorator. Back stacks are created with `rememberNavBackStack(key)` (Android reflection-based serialization; no explicit `SavedStateConfiguration`).
- **Resources**: Android `res/` with `R.string` / plurals, vector drawables, mipmaps, and **30 translated locales** (`values-*`), maintained via the `translations-converter` and `auto-translation` Gradle plugins. `R` is referenced from ViewModels and `SideEffect`s (`@StringRes`).
- **Image Loading**: Coil 3 (`io.coil-kt.coil3:coil-compose`) for async image rendering.
- **App Widgets**: Jetpack Glance (`androidx.glance.appwidget`) for home screen widgets (`NoteWidget.kt`).
- **Color Picker**: `com.github.skydoves:colorpicker-compose` used in [`ColorPickerControls.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/color/ColorPickerControls.kt).

### 2. Domain & Application Business Logic

- **State Holders**: AndroidX `ViewModel` (`viewModelScope`, `koinViewModel()`). `BaseViewModel`, `NoteModel`, `MainActivityViewModel`, `SettingsViewModel` and `ErrorActivityViewModel` extend `AndroidViewModel`, and nearly every feature ViewModel takes an `Application` constructor parameter. ViewModels also import `android.net.Uri`, `android.view.View`, `android.Manifest` / `PackageManager` (permission checks), `android.print.PdfPrintListener` and `com.philkes.notallyx.R`.
- **Concurrency**: Kotlin Coroutines (`StateFlow`, `SharedFlow`, `Channel`, `combine`, `stateIn`).
  - `Dispatchers.Main`: UI interactions and StateFlow emissions.
  - `Dispatchers.IO`: File I/O, database access, backup generation, DataStore writes.
  - `Dispatchers.Default`: Text search, diff/delta calculation ([`NoteBodyDeltaChange.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/history/changes/NoteBodyDeltaChange.kt)), undo/redo change history.
- **Key Business Utilities**:
  - Note splitting: [`NoteSplitUtils.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/NoteSplitUtils.kt)
  - Note repair: [`NoteRepairUtils.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/NoteRepairUtils.kt)
  - Sorting algorithms: `BaseNoteTitleSort`, `BaseNoteModifiedDateSort`, `BaseNoteCreationDateSort`, `BaseNoteColorSort`.

### 3. Data & Persistence Layer

- **Database Framework**: Room 3 (`androidx.room3:room3-runtime`, `room3-compiler`, `room3-common`, `room3-sqlite-wrapper`).
- **Database Class**: [`NotallyDatabase.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt) (v11 schema).
  - Uses `@ColumnTypeConverters(Converters::class)` converting complex objects (spans, attachments, folders, reminders, labels) to/from JSON strings **using `org.json` (`JSONArray`/`JSONObject`)**, which ships with the Android framework but is not part of the JDK.
  - Custom database driver wrapper: [`BackupSQLiteDriver.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/database/BackupSQLiteDriver.kt), a Room 3 `SQLiteDriver` that bridges to a `SupportSQLiteOpenHelper.Factory` ([`BackupOpenHelperFactory`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/database/BackupOpenHelperFactory.kt) or SQLCipher's `SupportOpenHelperFactory`) and requires a `ContextWrapper`. It duplicates the schema version (`DATABASE_VERSION = 11`). `androidx.sqlite:sqlite-bundled` is already declared as a dependency.
  - **Live, opt-in** SQLCipher database encryption (`net.zetetic:sqlcipher-android`): enabled when the user activates biometric lock with `biometricLockEncryptsDb` (see `NotallyDatabase.setupEncryption`, `DatabaseTransitionActivity`, and `DataSchemaMigrations`, which sets `biometricLockEncryptsDb = true` for users who already had biometric lock enabled). There is **no** `FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB` yet.
  - Checkpoint mechanism: `database.checkpoint()` executes `pragma wal_checkpoint(TRUNCATE)` before database copies or backups.
- **DAOs**:
  - [`BaseNoteDao.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/dao/BaseNoteDao.kt): Flow-based reactive queries for notes, archives, deleted notes, search.
  - [`LabelDao.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/dao/LabelDao.kt): Flow-based queries for labels.
  - [`CommonDao.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/dao/CommonDao.kt): Cross-table transactional operations.
- **Entities & Models**:
  - [`BaseNote.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/BaseNote.kt): Core note entity containing title, body, spans, color, viewMode, pinnedStatus, reminders, images, audios, files.
  - [`Label.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/Label.kt): Label entity.
  - Models `Attachment`, `Audio`, `FileAttachment` and `Reminder` implement `android.os.Parcelable` via `@Parcelize`. [`ModelExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/extensions/ModelExtensions.kt) uses `android.net.Uri`, `android.text.Html`, `android.util.Base64`, `Intent` and `org.json`. `BaseNoteDao`, `DaoExtensions` and `SearchResult` reference `Context`/`ContextWrapper`, and `CommonDao` uses `android.util.Log`.
  - Attachment classes: [`Attachment.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/Attachment.kt), [`Audio.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/Audio.kt), [`FileAttachment.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/FileAttachment.kt).
- **Preferences & DataStore**:
  - [`NotallyXPreferences.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/preference/NotallyXPreferences.kt): DataStore wrapper exposing reactive `StateFlow` preferences.
  - General preferences: Jetpack DataStore (`androidx.datastore.preferences`, `UserPreferences`).
  - Sensitive preferences: Encrypted DataStore (`androidx.datastore.tink`) using Google Tink AEAD (`SecurePreferences`).

### 4. Security & Authentication Layer

- **Biometrics**: [`BiometricAuthenticator.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/security/BiometricAuthenticator.kt) wrapping `androidx.biometric.BiometricPrompt`.
- **Lock Activity**: [`LockedActivity.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/security/LockedActivity.kt) extending `AppCompatActivity` for screen security and biometric verification.
- **Database & Prefs Key Management**: Google Tink AEAD (`com.google.crypto.tink:tink-android`) in [`TinkAeadProvider.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/security/TinkAeadProvider.kt) storing keys in Android KeyStore.

### 5. Media & Service Layer

- **Audio Playback**: [`AudioPlayService.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/AudioPlayService.kt) powered by Jetpack Media3 ExoPlayer (`androidx.media3.exoplayer`, `androidx.media3.session`).
- **Audio Recording**: [`AudioRecordService.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/AudioRecordService.kt) utilizing Android `MediaRecorder` (`OutputFormat.MPEG_4` + `AudioEncoder.AAC`, so all stored recordings are **AAC in `.m4a` containers**).
- **Crash Reporting**: `cat.ereza:customactivityoncrash` with `ErrorActivity` / `ErrorActivityViewModel` (Android-only).

### 6. Migration, Import & Export Layer

- **Exporters**: [`ExportExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/ExportExtensions.kt) (ZIP backups via `zip4j`, decrypting an encrypted DB before export) and [`NoteExportExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/NoteExportExtensions.kt) (HTML, TXT, JSON, Markdown, PDF). **PDF is produced by rendering the HTML export in an off-screen `android.webkit.WebView`** and printing it through `PrintDocumentAdapter` in [`android/print/PdfExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/android/print/PdfExtensions.kt) (placed in the `android.print` package to access package-private APIs). `PdfPrintListener` leaks into several ViewModels.
- **Importers**: [`NotesImporter.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/imports/NotesImporter.kt) supporting Google Keep ([`GoogleKeepImporter.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/imports/google/GoogleKeepImporter.kt)), Evernote ([`EvernoteImporter.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/imports/evernote/EvernoteImporter.kt), which uses `simple-xml`, `android.util.Base64` and `MimeTypeMap`), Quillpad, JSON, Plain Text, Markdown (`commonmark`), and HTML (`jsoup`).
- **Sharing & Intents**: `Intent.ACTION_SEND` share actions (notes, images, audio, crash reports via email) and `FileProvider` URIs in `AndroidExtensions.kt`, `PlayAudioRoute`, `ViewImageRoute` and `SettingsRoute`. `MainActivity` receives `ACTION_SEND`/`ACTION_SEND_MULTIPLE` to create notes from shared content.

### 7. Background Processing & System Integrations

- **WorkManager**:
  - [`AutoBackupWorker.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/AutoBackupWorker.kt): Scheduled periodic ZIP backups.
  - [`AutoRemoveDeletedNotesWorker.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/routes/settings/AutoRemoveDeletedNotesWorker.kt): Purging trashed notes older than configured days.
  - [`CleanupMissingAttachmentsWorker.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/CleanupMissingAttachmentsWorker.kt): Storage cleanup.
- **Alarm & Notification Receivers**:
  - [`ReminderReceiver.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/ReminderReceiver.kt): `AlarmManager` triggers for scheduled note reminders.
  - [`PinnedNotificationManager.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/PinnedNotificationManager.kt): Ongoing status bar notification pins.

### 8. Dependency Injection Setup (Koin 4.x)

Koin 4.2.2 modules are registered in `di/`:
- [`AppModule.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/di/AppModule.kt): Consolidates modules.
- [`DatabaseModule.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/di/DatabaseModule.kt): Provides `NotallyDatabase` instance using `androidContext()`.
- [`PreferencesModule.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/di/PreferencesModule.kt): Provides Tink `Aead` and `NotallyXPreferences`.
- [`FeatureModule.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/di/FeatureModule.kt): Registers ViewModels (`viewModelOf(::ExampleViewModel)`) and WorkManager workers (`workerOf(::AutoRemoveDeletedNotesWorker)`). Enabled with Koin Compiler Plugin (`io.insert-koin.compiler.plugin`).

---

## Detailed Directory Map

```
app/src/main/java/com/philkes/notallyx/
├── NotallyXApplication.kt                      # Application lifecycle, Koin init, theme/backup observers
├── core/
│   ├── common/                                 # Extensions, results, error activity, NoteSplitUtils
│   ├── database/                               # Room 3 DB, Converters, BackupSQLiteDriver, Migrations
│   ├── preference/                             # NotallyXPreferences, UserPreferences, SecurePreferences
│   ├── security/                               # BiometricAuthenticator, LockedActivity, TinkAeadProvider
│   └── ui/                                     # NotallyXTheme, BaseNoteCard, Dialogs, ColorPicker
├── data/
│   ├── dao/                                    # Room 3 DAOs (BaseNoteDao, LabelDao, CommonDao)
│   └── model/                                  # Entities (BaseNote, Label, Attachment, Reminder, Color)
├── di/                                         # Koin modules (AppModule, DatabaseModule, FeatureModule, PreferencesModule)
├── feature/
│   ├── editor/                                 # Note & list editing, undo/redo ChangeHistory, formatting bar
│   ├── labels/                                 # Label management & selection
│   ├── main/                                   # MainActivity, NotallyApp, NotesGridScreen, routes
│   ├── media/                                  # AudioPlayService, AudioRecordService, ViewImageRoute
│   ├── migration/                              # Exporters (PDF/HTML/ZIP), Importers (Keep/Evernote/JSON)
│   ├── reminder/                               # NoteReminders, AlarmExtensions, ReminderReceiver
│   ├── settings/                               # SettingsRoute, AutoRemoveDeletedNotesWorker
│   └── widget/                                 # Jetpack Glance NoteWidget
└── navigation/                                 # NotallyNavKey (@Serializable), ListDetailScene
```

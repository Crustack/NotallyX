# 02. KMP Compatibility & Dependency Analysis

## Overview

This document provides a granular analysis of all Android-specific framework dependencies, third-party libraries, and platform APIs in **NotallyX**. It evaluates each item against **Kotlin Multiplatform (KMP)** requirements targeting **Android** and **Desktop (JVM)**.

> [!IMPORTANT]
> **Both targets are JVM-based.** A `commonMain` source set that is shared only by `androidTarget()` and `jvm("desktop")` can use JDK APIs (`java.io.File`, `java.nio`, `java.time`, `java.util.UUID`, …) and plain Java libraries (jsoup, commonmark, zip4j, simple-xml, PrettyTime, …). Only **Android framework** APIs (`android.*`, `androidx.*` Android-only artifacts, `org.json` which is bundled with the Android framework) are blockers. Replacing JVM libraries with KMP-native ones (okio, kotlinx-datetime) is therefore **optional** unless non-JVM targets are added later.

---

## Refined Layer Architecture & Boundaries

```
NotallyX Architecture Layers
├── :shared (Domain & Data Layer)
│     ├── Domain Entities, DAOs, Room DB, DataStore Preferences, Repositories
│     └── Platform Interfaces: PathProvider, FileStorage, DatabaseDriverFactory, SecurePreferencesStore,
│                              TaskScheduler, ReminderScheduler, AudioPlayer, AudioRecorder, PdfExporter
├── :composeApp (Presentation Layer)
│     ├── ViewModels (androidx.lifecycle.ViewModel), UI State, Nav3 Routes, Screens, Themes, composeResources
│     └── UI Platform Interfaces: FilePicker, ShareHandler, PermissionChecker
├── :androidApp (Android Executable Entry)
│     └── MainActivity, Application, Glance Widgets, WorkManager, AlarmReceivers, Biometrics, ErrorActivity
└── :desktopApp (Desktop Executable Entry)
      └── main.kt, Window, No-Op schedulers (initially), kotlinx-coroutines-swing
```

---

## Android Framework Coupling Analysis

### 1. Storage & Filesystem Coupling
- **Android SAF (`DocumentFile`, `ContentResolver`, `android.net.Uri`)**:
  - *Current Usage*: Used in [`IOExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/extensions/IOExtensions.kt), [`ImportExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/ImportExtensions.kt), [`ExportExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/ExportExtensions.kt), [`NoteExportExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/NoteExportExtensions.kt), [`ModelExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/extensions/ModelExtensions.kt), and in ViewModels (`EditNoteViewModel`, `EditListViewModel`, `EditTextPlainViewModel`, `NotesRouteViewModel`, `PlayAudioViewModel`, `ViewImageViewModel`, `SettingsViewModel`, `EditActivityViewModel`).
  - *KMP Challenge*: SAF and `android.net.Uri` do not exist on Desktop JVM.
  - *Action*: Define `FileStorage` in `:shared/commonMain` and an explicit `FilePicker` interface in `:composeApp/commonMain`. `composeApp/androidMain` uses SAF launchers. `composeApp/desktopMain` uses Swing `JFileChooser` / AWT `FileDialog`.

### 2. Context & Application Lifecycle Coupling
- **`android.content.Context`, `ContextWrapper` & `AndroidViewModel`**:
  - *Current Usage*: Passed into the database builder and driver ([`NotallyDatabase.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt), `BackupSQLiteDriver`, `BackupOpenHelperFactory`, `DataSchemaMigrations`), DAOs (`BaseNoteDao`, `DaoExtensions`), models (`SearchResult`, `ModelExtensions`, `ReminderModelExtensions`), preferences ([`NotallyXPreferences.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/preference/NotallyXPreferences.kt)), and ViewModels (5 `AndroidViewModel` subclasses, ~25 ViewModels with an `Application` parameter).
  - *KMP Challenge*: `Context` is Android-only.
  - *Action*: Remove `Context` parameter requirements from shared repositories, DAOs, and ViewModels. Supply `Context` via Koin `androidMain` bindings on Android, and `PathProvider` on Desktop.

### 3. Android-Only Utility APIs in Domain/Data Code
- **`org.json`** (`Converters.kt`, `ModelExtensions.kt`): bundled with the Android framework, **absent on Desktop JVM**. Replace with `kotlinx.serialization.json` (`JsonElement` API). Do not just add the `org.json:json` artifact, because its behavior differs from Android's fork (e.g. `null` handling, `optString`).
- **`android.util.Base64`** (`ModelExtensions.kt`, `EvernoteImporter.kt`): replace with `kotlin.io.encoding.Base64`.
- **`android.text.Html`** (`ModelExtensions.kt`): replace with `jsoup` (already a dependency).
- **`android.util.Log`** (`CommonDao`, `NotallyDatabase`, ViewModels, …): introduce a tiny logging facade (`expect`/`actual` or an interface).
- **`android.webkit.MimeTypeMap`** (`AndroidExtensions.kt`, `EvernoteImporter.kt`): replace with a static extension → MIME map or `java.net.URLConnection.guessContentTypeFromName`.
- **`android.database.sqlite.SQLiteConstraintException`** (`LabelsViewModel`, `SelectLabelsViewModel`): catch `androidx.sqlite.SQLiteException` / Room 3 driver exceptions instead.
- **`@Parcelize` / `android.os.Parcelable`** (`Attachment`, `Audio`, `FileAttachment`, `Reminder`): remove and rely on `@Serializable`, or use an `expect` annotation if parceling is still needed for Android `Intent`s.

### 4. Resources & Localization
- **Android `R` resources**: strings, plurals, drawables and 30 translated locales live in `app/src/main/res`. `R.string.*` is used in ViewModels and `@StringRes` in `SideEffect`s.
- *KMP Challenge*: `R` is not available in `commonMain`.
- *Action*: Migrate to **Compose Multiplatform Resources** (`composeApp/src/commonMain/composeResources`, generated `Res`). ViewModels emit `StringResource` (or a `UiText` wrapper) instead of `@StringRes Int`. Android-only resources (launcher icons, widget XML descriptors, `xml/` provider paths, notification icons) remain in `androidApp/src/main/res`. The `translations-converter` / `auto-translation` plugins currently target Android `values-*/strings.xml` (see OQ-11).

### 5. Navigation 3 on Non-Android Targets
- `rememberNavBackStack(key)` relies on Android reflection-based serialization. On non-Android targets the back stack requires a `SavedStateConfiguration` with a `SerializersModule` that registers every `NotallyNavKey` subtype polymorphically. `NavigationState.kt` already uses a custom `NavKeySerializer`, which must be checked for compatibility.

### 6. Platform Intents & Permissions
- `Intent.ACTION_SEND` share flows, `FileProvider`, the email intent for crash reports, receiving `ACTION_SEND` in `MainActivity`, and `PackageManager` / `Manifest` permission checks inside ViewModels (`EditNoteViewModel`, `EditListViewModel`, `NoteRemindersViewModel`, `SettingsViewModel`).
- *Action*: `ShareHandler` and `PermissionChecker` interfaces in `:composeApp/commonMain`. Receiving shared content stays Android-only (Desktop alternative: OS drag-and-drop, see OQ-16).

### 7. Background Tasks & Reminders
- **Background Tasks & Alarms**:
  - *Current Usage*: [`AutoBackupWorker.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/AutoBackupWorker.kt), [`AutoRemoveDeletedNotesWorker.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/routes/settings/AutoRemoveDeletedNotesWorker.kt), [`CleanupMissingAttachmentsWorker.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/CleanupMissingAttachmentsWorker.kt) and [`AlarmExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/AlarmExtensions.kt).
  - *Action*: Define `TaskScheduler` and `ReminderScheduler` interfaces in `:shared/commonMain`. On Android, implement via WorkManager & AlarmManager in `androidApp`. On Desktop, `desktopApp` binds **No-Op implementations** initially, consistent with the Android-only designation. Whether Desktop later gets in-process schedulers (`ScheduledExecutorService` / coroutine loops while the app runs) is open (OQ-01, OQ-02).

### 8. Database Encryption & Biometrics
- **Database Encryption**:
  - *Current State*: SQLCipher encryption is **live and opt-in on Android** (biometric lock + `biometricLockEncryptsDb`). Existing users may already have an encrypted database.
  - *Decision*: Database encryption will be disabled on all platforms. `PREP-012` introduces `const val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`, hides the setting, and runs a **one-time decryption migration** for already-encrypted databases. Desktop JVM uses standard unencrypted Room 3 KMP (`sqlite-bundled`).
- **Biometric App Lock**:
  - *Decision*: `BiometricPrompt` and `LockedActivity` UI app lock are Android-only components housed in `androidApp`.

### 9. PDF Export
- PDF export renders the note's HTML export in an off-screen **`WebView`** and prints it via `PrintDocumentAdapter` (`android/print/PdfExtensions.kt`). Both are Android-only. Desktop needs an HTML→PDF engine (e.g. OpenHTMLtoPDF) or the feature is dropped on Desktop (OQ-06).

### 10. Audio Codecs
- Recordings are **AAC in `.m4a`** (`MediaRecorder.OutputFormat.MPEG_4` + `AudioEncoder.AAC`). Java Sound (`javax.sound`) cannot decode AAC out of the box, so Desktop playback needs an additional decoder or player library (OQ-07).

---

## Third-Party Dependency Compatibility Matrix

| Library | Current Group / Artifact | Version | KMP Status (Desktop JVM) | Recommended Action / KMP Alternative |
| :--- | :--- | :---: | :---: | :--- |
| **Compose Multiplatform** | `org.jetbrains.compose` | TBD (≥ 1.10, aligned with Compose BOM `2026.09.00`) | **Compatible** | Share UI in `:composeApp`. Version must provide multiplatform Navigation 3, lifecycle-viewmodel-navigation3 and material3-adaptive. |
| **Multiplatform ViewModel** | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` | Align with AndroidX lifecycle `2.11.0` | **Compatible** | Native KMP ViewModel in `:composeApp/commonMain`. Add `kotlinx-coroutines-swing` to `desktopApp`. |
| **Navigation 3** | `androidx.navigation3:navigation3-runtime` / `-ui` | 1.2.0 | **Compatible** (JetBrains `org.jetbrains.androidx.navigation3` artifacts) | Configure polymorphic `SavedStateConfiguration` for `NotallyNavKey` (`PREP-021`). |
| **Lifecycle ViewModel Nav3** | `androidx.lifecycle:lifecycle-viewmodel-navigation3` | 2.11.0 | **Compatible** (JetBrains artifact) | Keep. Decorators already configured. |
| **Material 3 Adaptive** | `androidx.compose.material3.adaptive:adaptive` | 1.3.0 | **Compatible** (JetBrains `org.jetbrains.compose.material3.adaptive`) | Keep `ListDetailScene` in `:composeApp`. Verify window size class APIs on Desktop. |
| **Material Icons Extended** | `androidx.compose.material:material-icons-extended` | BOM | **Compatible** | Use the CMP artifact. Note: it is no longer actively published and is large, so consider copying the used icons. |
| **Skydoves ColorPicker** | `com.github.skydoves:colorpicker-compose` | 1.3.0 | **KMP Compatible** | **Keep** in `:composeApp`. ColorPicker Compose supports KMP natively. |
| **Coil 3** | `io.coil-kt.coil3:coil-compose` | 3.1.0 | **Compatible** | Keep in `:composeApp`. Coil 3 natively supports JVM Desktop. |
| **Room 3** | `androidx.room3:room3-runtime` | 3.0.3 | **Compatible** | Configure Room 3 KMP in `:shared` with `androidx.sqlite:sqlite-bundled` (`BundledSQLiteDriver`) on Desktop. `room3-sqlite-wrapper` is Android-only. |
| **SQLite Bundled** | `androidx.sqlite:sqlite-bundled` | 2.7.1 | **Compatible** | Already declared. JVM artifact ships native libraries for Linux/macOS/Windows inside the JAR. |
| **SQLite Framework** | `androidx.sqlite:sqlite-framework` | 2.7.1 | **Android Only** | Android driver only. |
| **Jetpack DataStore** | `androidx.datastore:datastore-preferences` | 1.3.0 | **Compatible** | Keep in `:shared`. Standard unencrypted DataStore on Desktop. |
| **DataStore Tink** | `androidx.datastore:datastore-tink` | 1.3.0 | **Android Only** | Behind `SecurePreferencesStore` in `androidApp` / `shared/androidMain`. |
| **Koin 4.x Core** | `io.insert-koin:koin-core` | 4.2.2 | **Compatible** | Use `koin-core`, `koin-compose`, and `koin-compose-viewmodel` across `:shared` and `:composeApp`. Replace `koin-androidx-compose`. |
| **Koin Navigation 3** | `io.insert-koin:koin-compose-navigation3` | 4.2.2 | **To verify** | Verify multiplatform publication (OQ-14). |
| **Koin Compiler Plugin** | `io.insert-koin.compiler.plugin` | 1.2.1 | **To verify** | Verify KMP source-set support (OQ-14). |
| **Koin WorkManager** | `io.insert-koin:koin-androidx-workmanager` | 4.2.2 | **Android Only** | Keep in `androidApp`. |
| **Kotlinx Serialization** | `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.11.0 | **Compatible** | Keep in `:shared`. Also replaces `org.json`. |
| **Kotlin Coroutines** | `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.11.0 | **Compatible** | Add `kotlinx-coroutines-swing` in `desktopApp`. |
| **org.json** | Android framework (`org.json.*`) | - | **Not available** | Replace with `kotlinx.serialization.json` (`PREP-020`). |
| **PrettyTime** | `org.ocpsoft.prettytime:prettytime` | 4.0.6 | **JVM Compatible** | Can stay in JVM-only `commonMain`. Replacement with `kotlinx-datetime` is optional (OQ-08). |
| **jsoup** | `org.jsoup:jsoup` | 1.23.2 | **JVM Compatible** | Keep in `:shared` (HTML import/export, replaces `android.text.Html`). |
| **CommonMark** | `org.commonmark:commonmark` (+ GFM strikethrough) | 0.30.0 | **JVM Compatible** | Keep in `:shared`. |
| **Simple XML** | `org.simpleframework:simple-xml` | 2.7.1 | **JVM Compatible** | Keep in `:shared` (Evernote import). Unmaintained, so watch for issues. |
| **zip4j** | `net.lingala.zip4j:zip4j` | 2.11.6 | **JVM Compatible** | Keep in `:shared` (backups). |
| **Commons Configuration 2** | `org.apache.commons:commons-configuration2` | 2.15.1 | **JVM Compatible** | Keep where used. |
| **DocumentFile** | `androidx.documentfile:documentfile` | 1.1.0 | **Android Only** | Confine to `AndroidFileStorage`. |
| **CustomActivityOnCrash** | `cat.ereza:customactivityoncrash` | 2.4.0 | **Android Only** | Keep in `androidApp`. Desktop crash handling via `Thread.setDefaultUncaughtExceptionHandler` (OQ-20). |
| **Material Components** | `com.google.android.material:material` | 1.14.0 | **Android Only** | Keep in `androidApp` (XML themes for activities). |
| **AppCompat** | `androidx.appcompat:appcompat` | 1.6.1 | **Android Only** | Keep in `androidApp` (`LockedActivity`). |
| **Jetpack Glance** | `androidx.glance:glance-appwidget` | 1.1.1 | **Android Only** | Retain in `androidApp`. |
| **WorkManager** | `androidx.work:work-runtime` | 2.12.0 | **Android Only** | Abstract via `TaskScheduler` in `:shared`. Implement via WorkManager in `androidApp`. |
| **AlarmManager / Receiver**| `android.app.AlarmManager` | - | **Android Only** | Abstract via `ReminderScheduler` in `:shared`. Implement via AlarmManager in `androidApp`. |
| **Biometrics** | `androidx.biometric:biometric` | 1.1.0 | **Android Only** | Retain in `androidApp`. |
| **SQLCipher** | `net.zetetic:sqlcipher-android` | 4.19.1 | **Android Only** | Keep only for the one-time decryption migration (`PREP-012`). Unencrypted SQLite on Desktop. |
| **Tink Android** | `com.google.crypto.tink:tink-android` | 1.16.0 | **Android Only** | Unencrypted DataStore on Desktop (see OQ-05 for secrets). |
| **Jetpack Media3** | `androidx.media3:media3-exoplayer` / `-session` | 1.11.1 | **Android Only** | Abstract behind `AudioPlayer` interface in `:shared`. Desktop needs an AAC-capable player (OQ-07). |

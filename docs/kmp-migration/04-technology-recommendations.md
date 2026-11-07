# 04. Technology Evaluations & Recommendations

## Overview

This document evaluates candidate technologies for **NotallyX's target Kotlin Multiplatform (KMP)** architecture targeting **Android** and **Desktop (JVM)**.

---

## 1. UI Framework: Compose Multiplatform (`:composeApp`)

### Assessment & Suitability
- **Current State**: NotallyX is already 100% written in Jetpack Compose Material 3 (`androidx.compose.material3`).
- **Compose Multiplatform (JetBrains)**: Provides pixel-perfect UI rendering on Desktop (JVM) using Skia (Skiko). Compose Multiplatform shares the exact same declarative UI paradigms, Modifier system, and Material 3 design tokens as Android Jetpack Compose.
- **Color Picker**: `com.github.skydoves:colorpicker-compose` v1.3.0 **natively supports Compose Multiplatform** (Android + Desktop). It resides directly in `:composeApp/commonMain`.
- **File Choosers & Pickers**: An explicit `FilePicker` interface in `:composeApp/commonMain` delegates to Activity Result Launchers in `composeApp/androidMain` and Swing `JFileChooser` / AWT `FileDialog` in `composeApp/desktopMain`.

### Recommendation
- **Adopt Compose Multiplatform** for shared UI screens and components in `:composeApp`. Keep `skydoves:colorpicker-compose` in shared UI.

---

## 2. Multiplatform ViewModel & Navigation 3 (`:composeApp`)

### Assessment & Suitability
- **Module Placement**: ViewModels reside strictly in `:composeApp` (Presentation Layer) alongside UI screens and Navigation 3 routes. This avoids circular dependencies with `:shared` (Data/Domain Layer).
- **ViewModel in KMP**: Official `androidx.lifecycle.ViewModel` is supported in KMP common code via `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (v2.10.0+).
- **Desktop Dispatchers**: On Desktop JVM, `ViewModel.viewModelScope` runs on `Dispatchers.Main.immediate`. Adding `kotlinx-coroutines-swing` to `desktopApp` enables correct Swing UI event thread dispatching.
- **Navigation 3 ViewModel Scoping**: When using Navigation 3 (`NavDisplay`) in common code, ViewModel lifecycle scoping is enabled by decorating `NavDisplay` with `rememberViewModelStoreNavEntryDecorator()` and `rememberSaveableStateHolderNavEntryDecorator()`:

```kotlin
NavDisplay(
    entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
    ),
    backStack = backStack,
    entryProvider = entryProvider { ... }
)
```

### Recommendation
- **Place ViewModels in `:composeApp`**. Inherit from `androidx.lifecycle.ViewModel` in `commonMain`. Add `kotlinx-coroutines-swing` to `desktopApp`. Configure Navigation 3 entry decorators for ViewModel scoping.

---

## 3. Dependency Injection: Koin 4.x

### Assessment & Suitability
- **Current State**: NotallyX uses Koin 4.2.2 (`koin-core`, `koin-android`, `koin-androidx-compose`, `koin-compose-navigation3`).
- **KMP Compatibility**: `koin-core` and `koin-compose` are fully multiplatform native. `koinViewModel()` is supported across Android and Desktop JVM via `koin-compose-viewmodel`.

### Recommendation
- **Retain Koin 4.x**. Use `koin-core` and `koin-compose` across `:shared` and `:composeApp`. Keep `koin-android` in `androidApp`.

---

## 4. Database & Persistence: Room 3 KMP (`:shared`)

### Assessment & Suitability
- **Current State**: NotallyX uses Room 3 (`androidx.room3:room3-runtime`, `room3-compiler`) v11 schema.
- **Room 3 KMP**: AndroidX Room 3 officially supports Kotlin Multiplatform for Android and JVM Desktop using `androidx.sqlite.driver.bundled.BundledSQLiteDriver`.
- **Encryption Policy**: Database encryption with SQLCipher is disabled across all targets via `const val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`. Desktop JVM uses standard unencrypted `BundledSQLiteDriver()`.
- **Native Packaging**: The Compose Multiplatform Gradle plugin packaging options in `desktopApp/build.gradle.kts` package native SQLite binaries for macOS (ARM64/x86_64), Linux (x64/aarch64), and Windows (x64).

### Comparison Table

| Feature | Current Android Room 3 | Target Room 3 KMP (Desktop + Android) |
| :--- | :--- | :--- |
| **Module Placement** | `:app` | `:shared` (Data/Domain Layer) |
| **Compiler / CodeGen** | KSP (`room3-compiler`) | KSP multiplatform plugin |
| **SQLite Driver** | `FrameworkSQLiteOpenHelperFactory` | `BundledSQLiteDriver` (`sqlite-bundled`) |
| **Database Directory** | `context.getDatabasePath()` | `PathProvider` (Standard OS App Data Directory: e.g., `~/.local/share/NotallyX/`) |
| **Encryption** | Disabled (`FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`) | Standard unencrypted `sqlite-bundled` |

### Recommendation
- **Migrate to Room 3 KMP in `:shared`**. Retain Room DAOs (`BaseNoteDao`, `LabelDao`, `CommonDao`) and `@ColumnTypeConverters`. Store Desktop database in standard OS application data directories.

---

## 5. Preferences & Configuration: Jetpack DataStore KMP (`:shared`)

### Assessment & Suitability
- **Current State**: NotallyX uses Jetpack DataStore (`androidx.datastore:datastore-preferences`).
- **KMP Status**: `androidx.datastore` is officially multiplatform native for Android and JVM Desktop.
- **Encryption Policy**: Tink AEAD encrypted preferences are an Android-only feature in `androidApp`. Desktop JVM uses standard unencrypted DataStore in standard OS application data directories.

### Recommendation
- **Use `androidx.datastore` in `:shared`**. Standard DataStore will be used on Desktop JVM inside standard OS application data directories.

---

## 6. File System & Storage Abstraction: `okio` / `kotlinx-io` (`:shared`)

### Assessment & Suitability
- **Current State**: Reliance on `android.net.Uri`, `DocumentFile`, and `ContentResolver`.
- **KMP Options**: `okio.Path` / `okio.FileSystem` or `kotlinx-io`. `okio` provides robust cross-platform file reading, writing, atomic moves, and directory listing for both Android and JVM Desktop.

### Recommendation
- **Adopt `okio.FileSystem`** inside `FileStorage` implementations in `:shared`.

---

## 7. Explicit Android-Only Features (`androidApp`)

The following features are **explicitly designated as Android-Only Features**. They reside exclusively in `androidApp`:

- **Biometric & PIN Lock**: `BiometricPrompt` and `LockedActivity`.
- **Experimental Encryption Implementation**: SQLCipher and Tink AEAD (disabled via `FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`).
- **Notifications & Alarms**: `AlarmManager` reminders, `ReminderReceiver`, `PinnedNotificationManager`.
- **App Widgets**: Jetpack Glance `NoteWidget`.
- **Auto-Backups & Cleanup**: WorkManager workers (`AutoBackupWorker`, `AutoRemoveDeletedNotesWorker`).

---

## 8. Summary of Technology Stack Recommendations

| Component | Current Technology | Target KMP Technology (Android + Desktop) | Module Location |
| :--- | :--- | :--- | :--- |
| **Language** | Kotlin 2.4.20 | Kotlin 2.4.20 (KMP) | Root |
| **UI Framework** | Jetpack Compose (Material 3) | Compose Multiplatform (Material 3) | `:composeApp` |
| **ViewModel** | `androidx.lifecycle.ViewModel` | `androidx.lifecycle.ViewModel` + `kotlinx-coroutines-swing` | `:composeApp` |
| **Navigation** | Jetpack Navigation 3 | Jetpack Navigation 3 + Entry Decorators | `:composeApp` |
| **Dependency Injection**| Koin 4.2.2 | Koin 4.2.2 (`koin-core`, `koin-compose`) | `:shared` & `:composeApp` |
| **Color Picker** | Skydoves ColorPicker | Skydoves ColorPicker (**Already KMP Native**) | `:composeApp` |
| **File Picker** | SAF Launchers | `FilePicker` Interface | `:composeApp` |
| **Database** | Room 3 (Android) | Room 3 KMP (`sqlite-bundled`, unencrypted) | `:shared` |
| **Preferences** | Jetpack DataStore + Tink | Jetpack DataStore KMP | `:shared` |
| **Desktop Path** | `context.getDatabasePath()` | Standard OS App Data Directory via `PathProvider` | `:shared` |
| **File I/O** | SAF / `DocumentFile` | `FileStorage` + `okio.FileSystem` | `:shared` |
| **Date / Time** | `java.util.Date` + `PrettyTime` | `kotlinx-datetime` | `:shared` |
| **Serialization** | `kotlinx.serialization` | `kotlinx.serialization` | `:shared` |
| **Image Loading** | Coil 3 (`coil-compose`) | Coil 3 KMP | `:composeApp` |
| **Audio Playback** | Media3 ExoPlayer | `AudioPlayer` (Media3 on Android, JavaSound on Desktop) | `:shared` |
| **Android-Only Features**| Biometrics, Alarms, Glance, WorkManager | Retained in `androidApp` | `:androidApp` |

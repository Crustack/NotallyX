---
sidebar_position: 1
title: Desktop Target (Kotlin Multiplatform)
description: Architectural overview, testing methodology, and developer setup for NotallyX Desktop.
---

# Desktop Target for Kotlin Multiplatform (KMP)

This document provides an engineering overview of the **Desktop Target (Kotlin Multiplatform)** in NotallyX. It outlines the multi-module project structure, the binary-compatible Room persistence architecture, the cross-platform Compose UI test harness, and developer workflows for local development and verification.

---

## 1. Architectural Overview & Module Structure

NotallyX targets both **Android** and **Desktop (Linux, macOS, Windows)** using Kotlin Multiplatform (Kotlin 2.4.20) and Compose Multiplatform for Desktop (JVM 17).

```
NotallyX/
├── shared/              # KMP Domain & Data Module
│   ├── commonMain/      # Domain models (BaseNote, Label), DAOs, migrations v1–v11
│   ├── desktopMain/     # DesktopDatabaseDriverFactory, DesktopFileStorage, DesktopLogger
│   └── androidMain/     # AndroidDatabaseDriverFactory, AndroidPathProvider
│
├── composeApp/          # KMP Presentation & UI Module
│   ├── commonMain/      # Compose Multiplatform screens, shared ViewModels, theme, dialogs
│   ├── desktopMain/     # DesktopFilePicker, DesktopShareHandler, DesktopPermissionChecker
│   ├── androidMain/     # AndroidFilePicker, AndroidShareHandler, AndroidPermissionChecker
│   └── commonTest/       # Universal Compose UI tests (runComposeUiTest)
│
├── desktopApp/          # Desktop Application Host Subproject
│   └── desktopMain/     # Desktop entry point (singleWindowApplication), DesktopAppNavHost, DI
│
└── app/                 # Android Application Host Subproject
    └── main/            # NotallyXApplication, MainActivity, Android Manifest & Activities
```

### Module Responsibilities

1. **`:shared` (KMP Domain & Data)**:
   - Contains pure business logic, Room 3 `NotallyDatabase` definitions, DAOs, converters, and full migration chain (v1 through v11).
   - Targets Android JVM and Desktop JVM (`jvm("desktop")`).
   - Uses `BundledSQLiteDriver` on Desktop to guarantee exact SQLite binary compatibility with Android databases without any operating system SQLite discrepancies.

2. **`:composeApp` (KMP Presentation & Shared UI)**:
   - Implements declarative user interfaces with Compose Multiplatform Material 3.
   - Houses common feature destinations (`CommonNotesScreen`, `CommonSearchScreen`, `CommonLabelsScreen`, `CommonArchivedScreen`, `CommonDeletedScreen`, and `CommonSettingsScreen`).
   - Contains universal Compose UI tests in `src/commonTest` executing identically across Desktop JVM and Android JVM host test environments.

3. **`:desktopApp` (Desktop Application Host)**:
   - Houses the desktop executable bootstrap (`Main.kt`) running `singleWindowApplication`.
   - Manages window lifecycle (min window dimension 400x300, close request hooks).
   - Coordinates desktop navigation via `DesktopAppNavHost`.
   - Configures desktop Koin dependency injection (`DesktopModule.kt`).
   - Executes automatic WAL checkpointing (`NotallyDatabase.checkpoint()`) on application exit.

4. **`:app` (Android Application Host)**:
   - Houses platform-specific Android integrations: Biometric prompt (`BiometricAuthenticator`), Glance AppWidgets (`NoteWidget`), system status bar notifications, and WorkManager background tasks.

---

## 2. Storage & Room 3 Binary Parity

NotallyX Desktop uses the identical Room 3 database schema and migration steps as Android.

### File System Storage Locations

Desktop data and attachments are stored in standard user application directories managed by `DesktopPathProvider`:

| OS | Application Database Path |
| :--- | :--- |
| **Linux** | `~/.local/share/NotallyX/NotallyDatabase` |
| **macOS** | `~/Library/Application Support/NotallyX/NotallyDatabase` |
| **Windows** | `%APPDATA%\NotallyX\NotallyDatabase` |

### Non-Destructive Storage & Durability

- **Bundled SQLite Driver**: SQLite operations execute using `androidx.sqlite:sqlite-bundled`, ensuring deterministic behavior across platforms.
- **WAL Checkpointing on Shutdown**: When the desktop window receives an `onCloseRequest` event, `Main.kt` triggers `NotallyDatabase.checkpoint()`, running `PRAGMA wal_checkpoint(TRUNCATE)` on `Dispatchers.IO` before exiting to flush the write-ahead log to disk.

---

## 3. Cross-Platform Compose UI Testing

Cross-platform Compose UI tests reside in `composeApp/src/commonTest` and execute using `runComposeUiTest`.

### `CommonComposeUiTestBase` Harness

`CommonComposeUiTestBase` provides:
- An isolated in-memory `NotallyDatabase` powered by `BundledSQLiteDriver`.
- An ephemeral test Koin container without requiring Android `Context` or `ActivityScenario`.
- Platform-agnostic `TestTags` (e.g., `TestTags.SEARCH_INPUT`, `TestTags.NOTE_CARD`).

### Cross-Target Runner Abstraction

To support running common UI tests on both Desktop JVM and Android JVM without emulator dependencies, an `expect`/`actual` base class pattern is used:

```kotlin
// composeApp/src/commonTest/kotlin/com/philkes/notallyx/test/BaseComposeTest.kt
expect abstract class BaseComposeTest()

// composeApp/src/desktopTest/kotlin/com/philkes/notallyx/test/BaseComposeTest.kt
actual abstract class BaseComposeTest actual constructor()

// composeApp/src/androidHostTest/kotlin/com/philkes/notallyx/test/BaseComposeTest.kt
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35], qualifiers = "w1024dp-h768dp")
actual abstract class BaseComposeTest actual constructor()
```

This guarantees that:
- `./gradlew :composeApp:desktopTest` executes pure Desktop Compose UI testing.
- `./gradlew :composeApp:testAndroidHostTest` runs the identical UI assertions using Robolectric on Android JVM with desktop layout qualifiers (`w1024dp-h768dp`).

Platform-specific tests requiring Android hardware APIs (biometrics, widget binding, intent handling) are retained in `app/src/androidTest`.

---

## 4. Mobile Feature Decoupling

Mobile-specific hardware capabilities are decoupled from Desktop:

- **Biometric Authentication**: Omitted from desktop settings; database encryption is handled independently.
- **System Notifications & Alarms**: Mobile `AlarmManager` and status-bar pinned reminders are hidden on desktop.
- **Home Screen AppWidgets**: Android Glance widgets are confined strictly to `:app`.
- **Static Import Audit**: Desktop source sets (`desktopApp/`, `shared/src/desktopMain/`, `composeApp/src/desktopMain/`, and `composeApp/src/commonTest/`) maintain a hard invariant of **zero `android.*` imports**.

---

## 5. Developer Setup & Commands

### Prerequisites

- **JDK**: Java Development Kit 17+ (e.g., OpenJDK 17 or 21).
- **Gradle**: Uses project Gradle wrapper (`./gradlew`).

### Running the Desktop Application

Launch the desktop UI locally:

```bash
./gradlew :desktopApp:run
```

### Build & Package Verification

Compile all desktop targets and build the desktop distribution:

```bash
# Compile shared and UI desktop targets
./gradlew :shared:compileKotlinDesktop :composeApp:compileKotlinDesktop

# Assemble desktop application JAR
./gradlew :desktopApp:assemble

# Run verification task
./gradlew :desktopApp:verifyDesktopBuild
```

### Running Test Suites

```bash
# Run Compose UI tests on Desktop JVM
./gradlew :composeApp:desktopTest

# Run Compose UI tests on Android Host / Robolectric
./gradlew :composeApp:testAndroidHostTest

# Run pure-JVM Room database tests
./gradlew :shared:desktopTest
```

### Code Formatting

All Kotlin source files adhere to `ktfmt` (`kotlinLangStyle`):

```bash
# Check formatting
./gradlew ktfmtCheck

# Apply formatting
./gradlew ktfmtFormat
```

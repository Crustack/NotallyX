# Kotlin Multiplatform (KMP) Migration Prerequisites & Evaluation

## Executive Summary

NotallyX is already in an advantageous starting position for a Kotlin Multiplatform (KMP) migration:
- **UI Framework**: Already **100% Jetpack Compose** across all activities, screens, bottom sheets, and dialogs. No Android ViewBinding or XML layout files are used for app screens (only a single `widget_preview.xml` for legacy AppWidget picker).
- **Language**: Nearly 100% Kotlin 2.4.20. Only **1 Java file** exists across the entire project (`SQLCipherUtils.java`).
- **Modern Architecture**: Uses Unidirectional Data Flow (MVI) with StateFlow/Flow, Koin 4.x (already multiplatform-capable), and Coil 3 (multiplatform image loading).

However, significant preparation is required before code can be moved into a `commonMain` shared source set:
1. **Monolithic Architecture**: The project consists of a single `:app` application module and a `:test-lib` module; there are no independent feature or data modules.
2. **Android/JVM-Specific Dependencies & APIs**: Heavy reliance on Android-only and JVM-only libraries for database encryption (`sqlcipher-android`), Tink AEAD (`tink-android`), background tasks (`androidx.work`), biometrics (`androidx.biometric`), widgets (`androidx.glance`), audio (`androidx.media3`), file I/O (`java.io.File`, `DocumentFile`), date/time (`java.util.Date`, `Calendar`), and data parsers (`jsoup`, `simple-xml`, `zip4j`, `commonmark`, `zstd-jni`, `prettytime`).
3. **Android Framework Coupling**: Pervasive use of `android.content.Context`, `android.net.Uri`, `android.os.Parcelable` / `@Parcelize`, and `AndroidViewModel` across domain and presentation models.

---

## 1. Java Code & Platform-Specific Java APIs

### Current State
- **Java Source Files**: Exactly one Java file:
  - `../../app/src/main/java/com/philkes/notallyx/core/security/SQLCipherUtils.java` (329 lines)
  - Interacts directly with `net.zetetic.database.sqlcipher.SQLiteDatabase`, `android.content.ContextWrapper`, `android.text.Editable`, and `java.io.FileInputStream`.
- **Platform-Specific Java Standard Library Usage**:
  - **Date & Time**: Widespread usage of `java.util.Date`, `java.util.Calendar`, `java.util.TimeZone`, and `java.util.Locale` in database converters (`Converters.kt`), data models (`BaseNote`, `Reminder`), viewmodels, formatters, and UI cards.
  - **File & Stream I/O**: Direct usage of `java.io.File`, `java.io.InputStream`, `java.io.OutputStream`, `FileInputStream`, `FileOutputStream`, and `java.io.Serializable` throughout backup, export, import, preferences, and attachment handling.
  - **Archive / Compression**: `java.util.zip.ZipInputStream`, `ZipEntry`, and `CRC32` used across Google Keep and Quillpad importers.
  - **Concurrency Primitives**: `java.util.concurrent.atomic.AtomicInteger` and `AtomicBoolean` used in migrations and converters.
- **Android Framework APIs in Domain/Data Code**:
  - `android.net.Uri` is used across models (`Attachment`, `NoteModel`), ViewModels, and backup/import/export utilities.
  - `android.os.Parcelable` and `@Parcelize` are used on `Attachment`, `Audio`, `FileAttachment`, and `Reminder`.
  - Multiple ViewModels inherit from `AndroidViewModel(app: Application)` rather than platform-agnostic `ViewModel`.

### Action Items & Pre-Migration Steps
- [ ] **Convert `SQLCipherUtils.java` to Kotlin**: Rewrite into Kotlin and isolate database encryption logic behind a platform interface (or keep in `androidMain`).
- [x] **Migrate Date/Time to `kotlinx-datetime`**: Replace `java.util.Date`, `Calendar`, and `TimeZone` in core data models (`BaseNote`, `Reminder`), Room converters, and presentation models with `kotlinx.datetime.Instant` and `LocalDateTime`.
- [x] **Abstract File System I/O with Okio**: Replace direct `java.io.File`, `InputStream`, and `OutputStream` in domain/export/import business logic with Multiplatform `okio.Path`, `okio.FileSystem`, `Source`, and `Sink`.
- [ ] **Replace `android.net.Uri` in Core/Domain Layer**: Represent attachment and storage references as domain-level URI strings or path wrappers (e.g. `uri-kmp` or custom value class), mapping to `android.net.Uri` only in Android-specific storage adapters.
- [ ] **Decouple `@Parcelize` and `Parcelable`**: Remove `Parcelable` from core data models or use `@CommonParcelize` multiplatform expect/actual annotations (or rely purely on `@Serializable`).
- [ ] **Migrate `AndroidViewModel` to standard `ViewModel`**: Remove `Application` parameter from ViewModels; inject platform-specific service interfaces (e.g. `NotificationManager`, `Toaster`, `StringProvider`) via Koin.

---

## 2. Dependencies & Ecosystem Analysis

### Ready for KMP (No Migration Needed)
| Library | Current Version | KMP Status | Notes |
| :--- | :--- | :--- | :--- |
| **Kotlin Coroutines** | `1.11.0` | ✅ KMP native | Fully multiplatform (`kotlinx-coroutines-core`). |
| **Kotlin Serialization** | `1.11.0` | ✅ KMP native | Fully multiplatform JSON engine. |
| **Koin** | `4.2.2` | ✅ KMP native | `koin-core`, `koin-compose` support Android, iOS, Desktop. |
| **Coil 3** | `3.1.0` | ✅ KMP native | `io.coil-kt.coil3` is built for multiplatform image loading. |
| **AndroidX SQLite Bundled** | `2.7.1` | ✅ KMP native | `androidx.sqlite:sqlite-bundled` supports KMP targets. |
| **Jetpack DataStore** | `1.3.0` | ✅ KMP native | Core `androidx.datastore:datastore-preferences` supports KMP. |
| **Jetpack Navigation 3** | `1.2.0` | ✅ KMP ready | Navigation 3 runtime and Compose integrations support KMP. |

### Android / JVM-Only Dependencies Requiring Migration or Isolation

| Library / Module | Purpose in NotallyX | Multiplatform Alternative or Strategy |
| :--- | :--- | :--- |
| **`net.zetetic:sqlcipher-android`** | Optional database encryption | Keep in `androidMain` behind a platform SQLite driver factory, or investigate `SQLCipher` multiplatform binaries / native SQLite encryption. |
| **`com.google.crypto.tink:tink-android`** & `androidx.datastore.tink` | AEAD encryption for secure preferences | Isolate in `androidMain`. For iOS/Desktop, implement an expect/actual `Cipher` or use KMP secure storage (e.g., Apple Keychain / multiplatform key-value encryption). |
| **`androidx.room3:*`** | Room 3 local SQLite database | Room 2.7+ / 3.x supports KMP targets (Android, iOS, JVM). Ensure entities and DAOs reside in shared modules using `androidx.room` KMP artifacts. |
| **`androidx.work:work-runtime`** | Auto-backup & auto-deletion workers | Keep in `androidMain`. Create an interface (e.g. `BackgroundJobScheduler`) implemented via WorkManager on Android and platform background tasks on other targets. |
| **`androidx.biometric:biometric`** | App & database biometric lock | Isolate in `androidMain`. Define a multiplatform `BiometricAuthenticator` interface. |
| **`androidx.glance:glance-appwidget`** | Android home screen widget | Strictly Android-only feature; isolate in `androidMain` or a dedicated `:feature:widget-android` module. |
| **`androidx.media3:*`** | Audio recording & playback service | Android-only media pipeline; isolate `AudioPlayService`/`AudioRecordService` in `androidMain`. Provide a common `AudioPlayer` interface. |
| **`androidx.documentfile:documentfile`** | Android SAF (Storage Access Framework) | Isolate in `androidMain` for external storage integration. |
| **`org.jsoup:jsoup`** | HTML parsing & text formatting | Replace with **`com.fleeksoft.ksoup:ksoup`** (Multiplatform Kotlin HTML parser) or similar KMP library. |
| **`org.commonmark:commonmark`** | Markdown parsing & export | Replace with a Multiplatform Markdown parser (e.g., `org.jetbrains:markdown` or `multiplatform-markdown-renderer`). |
| **`cat.ereza:customactivityoncrash`** | Crash handler activity | Isolate in Android app entry point (`androidMain`). |

---

## 3. Modularization Technical Debt

### Current State
```
NotallyX/
├── app/       # Monolithic Android application containing ALL core, data, and feature code
└── test-lib/  # Test utilities (UiTestBase, UiUtils, Fakes)
```
- The codebase follows a disciplined **Feature Quad** structure (`Model.kt`, `ViewModel.kt`, `Screen.kt`, `Route.kt`) inside `com.philkes.notallyx.feature.*`, and clean `core/` and `data/` packages.
- However, all layers currently reside in the single `:app` Android module and have direct compile-time access to Android framework classes (`android.content.Context`, `android.content.Intent`, `android.os.Bundle`).
- Circular dependencies or tight coupling to Android components prevent simply applying the `kotlin("multiplatform")` plugin to `:app`.

### Recommended Target Modularization
Before converting modules to KMP, decompose the monolithic `:app` into Gradle modules following high cohesion, low coupling:

```
NotallyX/
├── core/
│   ├── model/          # (KMP) Pure entities: BaseNote, Label, Folder, Item, Reminder
│   ├── common/         # (KMP) Extensions, Progress, Result, Logging interfaces
│   ├── database/       # (KMP) Room database, DAOs, converters, migrations
│   └── preference/     # (KMP) DataStore models, preferences interfaces
├── feature/
│   ├── editor/         # (KMP/CMP) Note/List editor state, viewmodels, screens
│   ├── notes/          # (KMP/CMP) Notes grid, search, archive, trash
│   ├── labels/         # (KMP/CMP) Label management
│   ├── settings/       # (KMP/CMP) App settings & preferences UI
│   ├── media/          # (Mixed) Audio recording & playback, image viewer
│   └── migration/      # (KMP core / Android I/O) Backup, restore, exporters, importers
├── androidApp/         # (Android Application) MainActivity, LockedActivity, Services, WorkManager, Glance widget
└── test-lib/           # (KMP/Android) Shared testing fakes & test harnesses
```

### Action Items & Pre-Migration Steps
- [ ] **Extract `:core:model`**: Move pure domain classes (`BaseNote`, `Label`, `Folder`, `Item`, `Reminder`, spans) with zero Android framework imports into a separate Kotlin module.
- [ ] **Extract `:core:database`**: Move Room DAOs, entities, and database configuration into a data module configured with multiplatform Room 3 / SQLite driver.
- [ ] **Extract `:core:preference`**: Move DataStore definitions and preference wrappers into a separate module, isolating Android-specific Tink AEAD encryption behind an interface.
- [ ] **Decouple Business Logic from `Context`**: Audit utilities and managers in `core/common/` that accept `Context`; refactor them to use pure Kotlin or injectable platform interfaces.

---

## 4. UI Layer: Jetpack Compose to Compose Multiplatform

### Current State
- **100% Jetpack Compose**: All screens and dialogs are written in Jetpack Compose with Material 3.
- **No ViewBinding / XML**: Zero XML screen layouts.
- **Adaptive Layouts**: Uses `androidx.compose.material3.adaptive` and `ListDetailScene` for multi-pane layouts.
- **Resource Access**: Currently uses Android resource identifiers (`R.string.*`, `R.drawable.*`) and `stringResource(...)`.

### Action Items & Pre-Migration Steps
- [ ] **Prepare for Compose Multiplatform Resources**: When transitioning to Compose Multiplatform, migrate `R.string.*` and `R.drawable.*` usages in shared screens to `Res.string.*` / `Res.drawable.*` using the Compose Multiplatform resource library.
- [ ] **Audit Android Insets & System Bars**: Refactor direct window insets manipulation (`window.insetsController`) and `LockedActivity` screen locking to respect multiplatform windowing or handle them strictly at the Android host activity level.
- [ ] **Review Material Icons**: Migrate `androidx.compose.material:material-icons-extended` usages to multiplatform icon libraries or custom SVG drawables to reduce bundle size and ensure cross-platform compatibility.

---

## 5. Testing & Verification Strategy

### Current State
- Unit tests run under Robolectric (`testDebugUnitTest`).
- Screenshot testing uses **Roborazzi** (`verifyRoborazziDebug`) and **Compose Screenshot Testing**.
- Instrumented UI tests use AndroidX Test / Espresso (`connectedDebugAndroidTest`).

### Action Items & Pre-Migration Steps
- [ ] **Migrate Pure Unit Tests to `kotlin.test`**: Transition unit tests that verify domain logic, migrations, and ViewModels from `org.junit.Test` and Android runners to standard `kotlin.test`.
- [ ] **Retain Android Instrumented Tests in `:androidApp`**: Keep Robolectric and instrumented Android UI tests for Android-specific components (WorkManager, Glance, Biometrics, LockedActivity).
- [ ] **Prepare Roborazzi for Multiplatform**: Roborazzi already supports Compose Multiplatform desktop/JVM screenshot testing; configure shared UI screenshot tests to run against JVM targets.

---

## 6. Prioritized Phased Roadmap

### Phase 1: Code Cleansing & Language Alignment (Lowest Risk)
1. Convert `SQLCipherUtils.java` to Kotlin.
2. Replace `java.util.Date` / `Calendar` with `kotlinx-datetime`.
3. Eliminate `AndroidViewModel` in favor of standard `ViewModel` with injected platform dependencies.
4. Replace `android.net.Uri` in shared data models and viewmodels.

### Phase 2: Modularization
1. Create `:core:model` and isolate pure entities.
2. Create `:core:database` with Room KMP dependencies.
3. Create `:core:preference` with multiplatform DataStore.
4. Separate Android-only features (Glance widgets, Media3 services, WorkManager workers, Biometric lock) into platform-specific packages or modules.

### Phase 3: Dependency Replacement & Multiplatform I/O
1. Replace `jsoup` with `ksoup`.
2. Replace `simple-xml` with `xmlutil` or `kotlinx.serialization`.
3. Adopt `okio` for file, stream, and archive operations in import/export workflows.
4. Replace `commonmark` with a multiplatform Markdown parser.

### Phase 4: Compose Multiplatform Adoption
1. Apply Compose Multiplatform Gradle plugin to shared feature modules.
2. Migrate string and drawable resources to Compose Multiplatform `Res`.
3. Set up desktop or iOS target builds to verify multiplatform compilation.

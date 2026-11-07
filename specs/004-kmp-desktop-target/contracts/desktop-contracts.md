# Interface & Boundary Contracts: Desktop Target for Kotlin Multiplatform

**Feature**: `004-kmp-desktop-target` ([spec.md](../spec.md))  
**Date**: 2026-11-04  
**Status**: Completed  

---

## 1. Gradle Subproject Dependency Contract

The project structure adds `:desktopApp` as a sibling application module to `:app`:

```mermaid
graph TD
    DesktopApp[":desktopApp (Desktop JVM Host)"]
    AndroidApp[":app (Android Host)"]
    ComposeApp[":composeApp (Android + Desktop KMP)"]
    Shared[":shared (Android + Desktop KMP)"]

    DesktopApp -->|implementation| ComposeApp
    DesktopApp -->|implementation| Shared
    AndroidApp -->|implementation| ComposeApp
    AndroidApp -->|implementation| Shared
    ComposeApp -->|api| Shared
```

### Dependency Rules
1. **`:desktopApp`** depends on `:composeApp` and `:shared`. It MUST NOT depend on `:app`.
2. **`:app`** depends on `:composeApp` and `:shared`. It MUST NOT depend on `:desktopApp`.
3. **`:composeApp`** depends on `:shared` via `api(project(":shared"))`. It MUST NOT depend on `:app` or `:desktopApp`.
4. **`:shared`** is the bottom-most leaf module and MUST NOT depend on `:composeApp`, `:app`, or `:desktopApp`.

---

## 2. Multiplatform Target Configuration Contract

### 2.1 `:shared/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.koin.compiler)
    id("notallyx.kotlin.convention")
}

kotlin {
    android {
        namespace = "com.philkes.notallyx"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
        withJava()
        androidResources { enable = true }
        withHostTest { isIncludeAndroidResources = true }
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }
    
    jvm("desktop") {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.room3.common)
            api(libs.androidx.room3.runtime)
            api(libs.sqlite.bundled)
            api(libs.kotlinx.serialization)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            api(libs.jsoup)
            api(libs.prettytime)
            api(libs.simple.xml)
            api(libs.bundles.commonmark)
        }
        
        val desktopMain by getting {
            dependencies {
                // Desktop JVM specific dependencies
            }
        }
    }
}
```

### 2.2 `:desktopApp/build.gradle.kts` (New)
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.koin.compiler)
    application
}

kotlin {
    jvm("desktop") {
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
    }

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":composeApp"))
                implementation(libs.androidx.compose.ui)
                implementation(libs.androidx.compose.material3)
                implementation(libs.koin.core)
            }
        }
    }
}

application {
    mainClass.set("com.philkes.notallyx.desktop.MainKt")
}
```

---

## 3. Platform Abstraction & Dependency Injection Contract

All platform-specific facilities required by shared ViewModels and repositories MUST be satisfied via the established interface contracts registered in Koin:

| Contract Interface | Android Implementation (`:app` / `:shared:androidMain`) | Desktop Implementation (`:desktopApp` / `:shared:desktopMain`) |
| :--- | :--- | :--- |
| `DatabaseDriverFactory` | `AndroidDatabaseDriverFactory` (supports standard + encrypted DBs) | `DesktopDatabaseDriverFactory` (uses `BundledSQLiteDriver`) |
| `PathProvider` | `AndroidPathProvider` (maps to Android Context directories) | `DesktopPathProvider` (maps to OS standard app data directories) |
| `FileStorage` | `AndroidFileStorage` (supports `content://` and files) | `DesktopFileStorage` (standard JVM file I/O) |
| `FilePicker` | `AndroidFilePicker` (uses ActivityResult contracts) | `DesktopFilePicker` (standard desktop file dialog or file drag-drop) |
| `ShareHandler` | `AndroidShareHandler` (uses Android `ACTION_SEND` Intent) | `DesktopShareHandler` (system clipboard / email handler) |
| `PermissionChecker` | `AndroidPermissionChecker` (checks runtime Android permissions) | `DesktopPermissionChecker` (always returns `Granted`) |
| `TaskScheduler` | `AndroidTaskScheduler` (WorkManager) | `NoOpTaskScheduler` (no-op on desktop) |
| `ReminderScheduler` | `AndroidReminderScheduler` (AlarmManager) | `NoOpReminderScheduler` (no-op on desktop) |
| `AudioPlayer` | `AndroidServiceAudioPlayer` (Media3 service) | `NoOpAudioPlayer` (no-op in initial milestones) |
| `AudioRecorder` | `AndroidServiceAudioRecorder` (Media3 service) | `NoOpAudioRecorder` (no-op in initial milestones) |

---

## 4. Milestone Delivery Contract

### Milestone 1 Contract (Example Starter Screen)
- **Executable Target**: `./gradlew :desktopApp:run` compiles and launches the application window.
- **Screen Rendered**: `DesktopExampleScreen` displaying:
  - Welcome title and build/environment information (OS name, version, JVM runtime).
  - Verification badge confirming `:shared` and `:composeApp` dependency resolution.
  - Interactive counter or button confirming Compose event handling and state updates on desktop.
- **Window Behavior**: Window title set to "NotallyX", minimum size enforced (400x300), clean exit on close.

### Milestone 2 Contract (Core Note & Checklist Management)
- **Room Database Connectivity**: `NotallyDatabase` instantiated via `DesktopDatabaseDriverFactory` pointing to the user's desktop application directory.
- **Routes Enabled**:
  1. `NotesRoute` / `NotesScreen`: Lists notes, supports search and filtering.
  2. `EditNoteRoute` / `EditNoteScreen`: Full title, body, and formatting editing for text notes.
  3. `EditListRoute` / `EditListScreen`: Checkbox toggling, item addition, and editing for checklist notes.
- **Data Durability**: Automatic saving of edits to SQLite database; lossless persistence across restarts.

### Milestone 3 Contract (Full Route Parity & Cross-Platform UI Testing)
- Remaining shared routes operational: `LabelsRoute`, `SearchRoute`, `ArchivedRoute`, `DeletedRoute`, `SettingsRoute`, and `ImportExportRoute`.
- Mobile-specific features (biometrics, widgets, Android notification channels) remain absent and non-invocable.
- Cross-platform Compose UI tests executing via `runComposeUiTest` in `composeApp/src/commonTest` run uniformly on both Desktop JVM and Android, verifying shared route functionality without Android Activity or emulator dependencies.

---

## 5. Cross-Platform UI Testing & Test Boundary Contract

To avoid duplicating UI test logic and ensure complete test parity between Android and Desktop, UI tests are partitioned strictly by architectural scope:

### 5.1 Test Placement & Source-Set Allocation

| Test Scope | Target Source Set | Test Runner / Framework | Responsibilities & Test Scope |
| :--- | :--- | :--- | :--- |
| **Common Compose UI Tests** | `composeApp/src/commonTest/` | `runComposeUiTest` (`androidx.compose.ui.test` / Compose Multiplatform) | Full Compose hierarchy interaction: clicking, text typing, state checks, dialog confirmation, route navigation for Notes Overview, Note Editor, Checklist Editor, Search, Labels, Archive, Trash, and Settings. Runs identically on Desktop JVM and Android without emulator. |
| **Pure-JVM Database & Migration Tests** | `shared/src/desktopTest/` & `shared/src/androidHostTest/` | JUnit 4 + `BundledSQLiteDriver` | In-memory Room DB, DAO CRUD operations, v1–v11 migrations, type converters, WAL checkpointing. |
| **Android-Specific Integration Tests** | `app/src/androidTest/` | `AndroidJUnit4` + Espresso / `UiDevice` / Android test runner | Tests requiring Android OS services: Biometric authentication (`LockedActivityTest`), AlarmManager / exact alarms (`RemindersActivityTest`), Glance AppWidgets (`WidgetTest`), Crash handler (`ErrorActivityTest`), Intent filters (`EditTextPlainActivityTest`), SQLCipher database decryption (`DatabaseDecryptionMigrationTest`). |
| **Desktop-Specific Integration Tests** | `desktopApp/src/desktopTest/` | Compose Desktop testing | Desktop window lifecycle (`onCloseRequest`, resizing), desktop file drag-and-drop, desktop application menus. |

### 5.2 Decoupling Rules for Common Compose Tests

1. **Replace `ActivityScenario` with `runComposeUiTest`**:
   - Tests MUST NOT launch `MainActivity` via `ActivityScenario.launch(MainActivity::class.java)`.
   - Tests execute inside `runComposeUiTest { setContent { NotallyApp(...) } }` or by composing route composables directly.
2. **Replace Android Lifecycle & DI Hooks with Test Fixtures**:
   - `NotallyDatabase` is instantiated as an in-memory database using `BundledSQLiteDriver`.
   - Koin modules are started and stopped per-test using a shared `CommonComposeUiTestBase` harness.
3. **Decouple String Resources**:
   - Tests MUST NOT rely on `context.getString(R.string.x)` or Android `ContextWrapper`.
   - Tests MUST prioritize stable `Modifier.testTag(TestTags.XYZ)` semantics for node matching, ensuring test stability across languages and platforms. Where text matching is required, shared test string constants or multiplatform resource helpers are used.
4. **Zero Android OS / Espresso Imports in `commonTest`**:
   - No `androidx.test.espresso.*`, `androidx.test.uiautomator.*`, `android.content.Intent`, `android.app.Activity`, or `InstrumentationRegistry` in `composeApp/src/commonTest`.


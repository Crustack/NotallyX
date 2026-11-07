# 03. Prerequisites Roadmap

## Overview

This roadmap defines **22 prioritized, actionable prerequisite tasks** (`PREP-001` to `PREP-022`) organized into 6 logical phases. All tasks are designed to be implemented directly within the current Android project **prior to introducing KMP targets** (`desktopApp` / `composeApp`).

> [!NOTE]
> References such as "`:shared/commonMain`" denote the **target** module/source set. Until module extraction, the code lives in `:app` in the package mapped to that module (see [05 – Package-to-Module Mapping](./05-proposed-kmp-architecture.md#package-to-module-mapping)) and must not use APIs unavailable in the target source set. Task IDs are stable identifiers. Tasks added later (`PREP-019`+) are listed inside the phase they belong to, so IDs are not strictly sequential.

---

## Phase 1: Architecture & ViewModel Decoupling

### PREP-001: Decouple Android Context from Database Creation & Access
- **Task ID**: `PREP-001`
- **Title**: Decouple `Context` and `ContextWrapper` from `NotallyDatabase` and Database Provider
- **Description**: Refactor [`NotallyDatabase.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt) and [`DatabaseModule.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/di/DatabaseModule.kt) to accept abstract database configuration options or path suppliers instead of requiring `ContextWrapper` directly in core database methods.
- **Reason**: Database instantiation on Desktop JVM will not have an Android `Context`. Room 3 KMP accepts path strings and native driver factories.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/BackupSQLiteDriver.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/BackupOpenHelperFactory.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/DataSchemaMigrations.kt`
  - `app/src/main/java/com/philkes/notallyx/data/dao/BaseNoteDao.kt`
  - `app/src/main/java/com/philkes/notallyx/data/dao/DaoExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/data/model/SearchResult.kt`
  - `app/src/main/java/com/philkes/notallyx/di/DatabaseModule.kt`
- **Priority**: Critical
- **Effort**: Medium
- **Dependencies**: None
- **Acceptance Criteria**:
  - `NotallyDatabase.getFreshDatabase` accepts database path or configuration supplier.
  - `DatabaseModule.kt` binds platform context only in Android DI modules.
  - Unit tests run without needing `ContextWrapper` for in-memory / file DB creation.
- **Expected Outcome**: Clean database instantiation that works with both Android `Context` and JVM Desktop directory paths.

---

### PREP-002: Abstract Android Uri in Core Domain Models
- **Task ID**: `PREP-002`
- **Title**: Replace `android.net.Uri` in model extensions and ViewModels with path strings / platform handles
- **Description**: The entity classes themselves (`BaseNote`, `Attachment`, `Audio`, `FileAttachment`) already store attachment references as `String` names. `android.net.Uri` is used in [`ModelExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/extensions/ModelExtensions.kt) and in ViewModels (`EditNoteViewModel`, `EditListViewModel`, `EditTextPlainViewModel`, `EditActivityViewModel`, `NotesRouteViewModel`, `PlayAudioViewModel`, `ViewImageViewModel`, `SettingsViewModel`). Replace these with path strings or an opaque platform file handle produced by `FilePicker` (`PREP-004`), so that `Uri` is only created or resolved in Android platform code.
- **Reason**: `android.net.Uri` is an Android framework class that does not exist on JVM Desktop.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/data/model/extensions/ModelExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/editor/EditNoteViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/editor/EditListViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/editor/EditTextPlainViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/editor/EditActivityViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/main/routes/notes/NotesRouteViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/media/audio/PlayAudioViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/media/image/ViewImageViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/settings/SettingsViewModel.kt`
- **Priority**: High
- **Effort**: Medium
- **Dependencies**: None (completes together with `PREP-004`)
- **Acceptance Criteria**:
  - Zero imports of `android.net.Uri` in `data/` (including `data/model/extensions/`) and in ViewModels.
  - Stored attachment references remain relative names (no `content://` URIs persisted).
  - All existing unit tests pass.
- **Note**: Removing `@Parcelize`/`Parcelable` from models is handled in `PREP-020`.
- **Expected Outcome**: Pure Kotlin domain models suitable for multiplatform compiled code.

---

### PREP-003: Presentation Layer ViewModels & Navigation 3 Entry Decorators
- **Task ID**: `PREP-003`
- **Title**: Remove `AndroidViewModel` / `Application` from ViewModels and verify Navigation 3 entry decorators
- **Description**: `BaseViewModel`, `NoteModel`, `MainActivityViewModel`, `SettingsViewModel` and `ErrorActivityViewModel` extend `AndroidViewModel`, and about 25 feature ViewModels take an `Application` constructor parameter (for example [`SettingsViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsViewModel.kt) with 1,483 lines, [`EditNoteViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditNoteViewModel.kt), [`EditListViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditListViewModel.kt)). Migrate all of them to plain `androidx.lifecycle.ViewModel` and inject the interfaces they need (`PathProvider`, `FileStorage`, `PdfExporter`, `ShareHandler`, `PermissionChecker`, `AudioPlayer`, …). Remove `android.view.View` and `android.print.PdfPrintListener` from ViewModel APIs. Decide which Activity-scoped ViewModels (`MainActivityViewModel`, `EditActivityViewModel`, `ErrorActivityViewModel`) stay Android-only in `androidApp` (OQ-19). The Navigation 3 entry decorators (`rememberSaveableStateHolderNavEntryDecorator()`, `rememberViewModelStoreNavEntryDecorator()`) are **already configured** in [`NavigationState.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/navigation/NavigationState.kt), so only verify they stay in place.
- **Reason**: Placing ViewModels in `:composeApp` keeps `:shared` strictly focused on Domain/Data layers and prevents circular dependencies. `androidx.lifecycle.ViewModel` is officially supported in KMP common code (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`), but `AndroidViewModel` is not.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/common/BaseViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/editor/NoteModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/**/*ViewModel.kt` (all ViewModels)
  - `app/src/main/java/com/philkes/notallyx/di/FeatureModule.kt`
- **Priority**: High
- **Effort**: Large
- **Dependencies**: `PREP-001`, `PREP-006` (constructor injection targets). Full completion also requires `PREP-002`, `PREP-005`, `PREP-019`, `PREP-020`, `PREP-022`.
- **Acceptance Criteria**:
  - Zero `AndroidViewModel` subclasses and zero `android.app.Application` constructor parameters in ViewModels destined for `:composeApp`.
  - Zero `android.*` imports in those ViewModels (verified by grep).
  - `NavigationState` keeps `rememberViewModelStoreNavEntryDecorator()` and `rememberSaveableStateHolderNavEntryDecorator()`.
  - Unit tests for ViewModels run cleanly in pure JVM tests without Robolectric.
- **Expected Outcome**: Platform-agnostic ViewModels properly scoped via Navigation 3 in `:composeApp`.

---

### PREP-020: Replace Android-Only Utility APIs in Domain & Data Code
- **Task ID**: `PREP-020`
- **Title**: Remove `org.json`, `android.util.*`, `android.text.Html`, `MimeTypeMap`, Android SQLite exceptions and `@Parcelize` from shared-bound code
- **Description**:
  - `org.json` in [`Converters.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/database/Converters.kt) and `ModelExtensions.kt` → `kotlinx.serialization.json` (`JsonElement` API). **The persisted JSON format must stay byte-compatible**, so add round-trip tests against existing DB fixtures and backups before switching.
  - `android.util.Base64` (`ModelExtensions.kt`, `EvernoteImporter.kt`) → `kotlin.io.encoding.Base64`.
  - `android.text.Html` (`ModelExtensions.kt`) → `jsoup`.
  - `android.util.Log` → a small logging facade (`Logger` interface or `expect`/`actual`).
  - `android.webkit.MimeTypeMap` (`AndroidExtensions.kt`, `EvernoteImporter.kt`) → static extension→MIME map.
  - `android.database.sqlite.SQLiteConstraintException` (`LabelsViewModel`, `SelectLabelsViewModel`) → `androidx.sqlite.SQLiteException`.
  - `@Parcelize`/`Parcelable` on `Attachment`, `Audio`, `FileAttachment`, `Reminder` → remove (use `@Serializable`) or an `expect` annotation.
  - `ConverterErrorReporter.kt` (`android.app.Dialog`) → report through an interface implemented in UI/Android code.
- **Reason**: These APIs are part of the Android framework and do not exist on Desktop JVM, even though Desktop is a JVM target.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/database/Converters.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/ConverterErrorReporter.kt`
  - `app/src/main/java/com/philkes/notallyx/data/model/extensions/ModelExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/data/model/{Attachment,Audio,FileAttachment,Reminder}.kt`
  - `app/src/main/java/com/philkes/notallyx/data/dao/CommonDao.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/migration/imports/evernote/EvernoteImporter.kt`
- **Priority**: Critical
- **Effort**: Medium
- **Dependencies**: None
- **Acceptance Criteria**:
  - Zero `org.json`, `android.util.*`, `android.text.*`, `android.os.Parcelable`, `kotlinx.parcelize` imports in `data/` and `core/database/`.
  - Converter round-trip tests prove identical JSON output and parsing for existing data (including malformed/legacy data handled by `NoteRepairUtils`).
- **Expected Outcome**: Domain and data code compiles against a plain JVM classpath.

---

### PREP-021: Make Navigation 3 Back Stack Serializable on Non-Android Targets
- **Task ID**: `PREP-021`
- **Title**: Configure polymorphic `SavedStateConfiguration` for `NotallyNavKey`
- **Description**: [`NavigationState.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/navigation/NavigationState.kt) creates back stacks with `rememberNavBackStack(key)`, which relies on Android reflection-based serialization. On non-Android targets, Navigation 3 requires a `SavedStateConfiguration` whose `SerializersModule` registers every `NotallyNavKey` subtype polymorphically. Introduce this configuration now (it also works on Android) and verify the custom `NavKeySerializer` / `MutableStateSerializer` usage. Ensure no `NotallyNavKey` carries `Parcelable` or Android types.
- **Reason**: Without it, the shared navigation code compiles but fails at runtime on Desktop when saving or restoring state.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/navigation/NavigationState.kt`
  - `app/src/main/java/com/philkes/notallyx/navigation/NotallyNavKey.kt`
- **Priority**: High
- **Effort**: Small
- **Dependencies**: None
- **Acceptance Criteria**:
  - Back stacks are created with an explicit `SavedStateConfiguration` listing all `NotallyNavKey` subtypes.
  - Unit test serializes and deserializes every `NotallyNavKey` without Android reflection.
- **Expected Outcome**: Navigation state is portable to Desktop.

---

### PREP-022: Abstract Platform Intents & Runtime Permission Checks
- **Task ID**: `PREP-022`
- **Title**: Introduce `ShareHandler`, `ExternalLauncher` and `PermissionChecker` interfaces
- **Description**: Move `Intent.ACTION_SEND` share flows (notes, images, audio), the crash-report email intent, `FileProvider` URI creation (`AndroidExtensions.kt`, `PlayAudioRoute`, `ViewImageRoute`, `SettingsRoute`) and `Manifest`/`PackageManager`/`Build.VERSION` permission checks inside ViewModels (`EditNoteViewModel`, `EditListViewModel`, `NoteRemindersViewModel`, `SettingsViewModel`) behind interfaces in `:composeApp/commonMain`. Receiving `ACTION_SEND` in `MainActivity` stays Android-only.
- **Reason**: Intents, `FileProvider` and runtime permissions are Android-only. Desktop needs alternatives (clipboard, `java.awt.Desktop.open/mail`) or No-Ops.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/common/extensions/AndroidExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/media/audio/PlayAudioRoute.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/media/image/ViewImageRoute.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/main/routes/settings/SettingsRoute.kt`
  - ViewModels listed above
- **Priority**: Medium
- **Effort**: Medium
- **Dependencies**: `PREP-002`
- **Acceptance Criteria**:
  - No `android.Manifest`, `PackageManager`, `Intent` or `Build` imports in ViewModels.
  - Share/permission behaviour on Android unchanged (covered by existing UI tests).
- **Expected Outcome**: ViewModels express intent ("share this note") without Android APIs.

---

## Phase 2: Storage & File Access Abstraction

### PREP-004: Introduce `FileStorage` and `FilePicker` Interfaces
- **Task ID**: `PREP-004`
- **Title**: Create `FileStorage` interface in `:shared` and explicit `FilePicker` interface in `:composeApp`
- **Description**: Create a `FileStorage` interface in `:shared/commonMain` providing low-level byte/stream I/O. Create an explicit `FilePicker` interface in `:composeApp/commonMain` for UI file selection (`pickImportFile`, `pickExportFolder`). Implement `AndroidFilePicker` in `composeApp/androidMain` using Activity Result Launchers/SAF, and `DesktopFilePicker` in `composeApp/desktopMain` using Swing `JFileChooser` / AWT `FileDialog`.
- **Reason**: Eliminates duplicate file chooser logic across modules and establishes explicit platform ownership.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/common/extensions/IOExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/migration/backup/ImportExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/migration/backup/ExportExtensions.kt`
- **Priority**: Critical
- **Effort**: Large
- **Dependencies**: `PREP-002`
- **Acceptance Criteria**:
  - `FileStorage` defined in `:shared/commonMain`.
  - `FilePicker` defined in `:composeApp/commonMain`.
  - UI import/export bottom sheets invoke `FilePicker` interface.
- **Expected Outcome**: Clean file storage and picker abstractions across Android and Desktop.

---

### PREP-005: Abstract PDF Export Functionality
- **Task ID**: `PREP-005`
- **Title**: Extract a `PdfExporter` interface from the WebView + `PrintDocumentAdapter` pipeline
- **Description**: PDF export currently renders the note's HTML export (`note.toHtml(...)`) in an off-screen `android.webkit.WebView` and prints it through `PrintDocumentAdapter` in [`android/print/PdfExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/android/print/PdfExtensions.kt), called from [`NoteExportExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/NoteExportExtensions.kt). The `android.print.PdfPrintListener` callback type leaks into `EditNoteViewModel`, `EditListViewModel`, `EditActivityViewModel` and `NotesRouteViewModel`. Introduce `PdfExporter` in `:shared/commonMain` (input: HTML string + target file, output: `suspend` result) and implement `AndroidPdfExporter` by wrapping the existing WebView pipeline. Keep the HTML generation shared, because a Desktop HTML→PDF engine (e.g. OpenHTMLtoPDF) can reuse it (OQ-06).
- **Reason**: `WebView`, `PrintDocumentAdapter` and the `android.print` package hack are Android-specific.
- **Affected Files**:
  - `app/src/main/java/android/print/PdfExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/migration/backup/NoteExportExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/editor/bottomsheet/ExportBottomSheet.kt`
  - ViewModels importing `PdfPrintListener`
- **Priority**: Medium
- **Effort**: Medium
- **Dependencies**: `PREP-004`
- **Acceptance Criteria**:
  - `PdfExporter` interface defined with a coroutine-based API (no `PdfPrintListener` in shared code).
  - Export code and ViewModels call `PdfExporter` only.
  - PDF export on Android continues to work as expected.
- **Expected Outcome**: Isolated PDF generation interface ready for multiplatform implementation.

---

### PREP-006: Standardize Path and Directory Management for Standard OS Directories
- **Task ID**: `PREP-006`
- **Title**: Create a `PathProvider` interface mapping standard OS application data directories
- **Description**: Create a `PathProvider` interface in `:shared/commonMain` with methods: `getDatabaseDirectory()`, `getBackupsDirectory()`, `getMediaDirectory()`, `getCacheDirectory()`. Implement `AndroidPathProvider` using `Context` methods, and `DesktopPathProvider` resolving to standard OS application data directories (`~/.local/share/NotallyX` on Linux, `~/Library/Application Support/NotallyX` on macOS, `%APPDATA%\NotallyX` on Windows).
- **Reason**: Desktop JVM user data must reside in standard OS application data directories. On Linux, respect `$XDG_DATA_HOME` (fallback `~/.local/share`). The Android-only `dataInPublicFolder` mode (DB and attachments in a public folder) must be expressed through `PathProvider` too. Whether Desktop gets a configurable data directory is open (OQ-12).
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/preference/NotallyXPreferences.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt`
  - `app/src/main/java/com/philkes/notallyx/core/common/extensions/AndroidExtensions.kt`
- **Priority**: High
- **Effort**: Medium
- **Dependencies**: `PREP-001`
- **Acceptance Criteria**:
  - `PathProvider` interface defined in `:shared/commonMain`.
  - `NotallyXPreferences` and `NotallyDatabase` resolve directories via `PathProvider`.
  - Tests verify custom `PathProvider` overrides for temporary test directories.
- **Expected Outcome**: Standard OS application data directory resolution across all platforms.

---

## Phase 3: Dependency & Library Preparation

### PREP-007: Verify Skydoves ColorPicker Multiplatform Configuration
- **Task ID**: `PREP-007`
- **Title**: Audit and verify `com.github.skydoves:colorpicker-compose` for KMP shared UI
- **Description**: Audit [`ColorPickerControls.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/color/ColorPickerControls.kt). Skydoves `colorpicker-compose` v1.3.0 natively supports Compose Multiplatform (Android + Desktop). Ensure color picker usages do not invoke Android-specific Context or view helpers.
- **Reason**: `skydoves:colorpicker-compose` is already KMP-compatible out of the box; no library replacement is required!
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/ui/dialog/color/ColorPickerControls.kt`
  - `app/src/main/java/com/philkes/notallyx/core/ui/dialog/color/ColorPickerDialog.kt`
- **Priority**: Low
- **Effort**: Small
- **Dependencies**: None
- **Acceptance Criteria**:
  - ColorPicker controls operate purely via Compose state and Canvas.
  - Zero Android `Context` imports in color picker dialogs.
- **Expected Outcome**: Confirmed multiplatform compatibility for color picker component.

---

### PREP-008: Decouple Date Utilities from Android `Context` (PrettyTime decision)
- **Task ID**: `PREP-008`
- **Title**: Remove `Context` from date/reminder formatting. Optionally replace PrettyTime with `kotlinx-datetime`.
- **Description**: [`ReminderModelExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/extensions/ReminderModelExtensions.kt) takes an Android `Context` (for string resources / locale), and [`ModelExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/data/model/extensions/ModelExtensions.kt) mixes formatting with Android APIs. **Mandatory part**: remove the `Context` dependency (pass `Locale`/resolved strings or move formatting into the UI layer using Compose resources). **Optional part (OQ-08)**: replacing PrettyTime with `kotlinx-datetime`. PrettyTime is a plain JVM library and works on both targets. `kotlinx-datetime` provides date math but **no localized relative-time formatting** ("2 hours ago"), so replacing PrettyTime means re-implementing it with Compose Multiplatform plurals across 30 locales.
- **Reason**: The Android `Context` is the real KMP blocker. PrettyTime itself is not.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/data/model/extensions/ReminderModelExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/data/model/extensions/ModelExtensions.kt`
  - `app/build.gradle.kts`
  - `gradle/libs.versions.toml`
- **Priority**: Medium (mandatory part) / Low (optional PrettyTime replacement)
- **Effort**: Small (mandatory part) / Medium (optional part)
- **Dependencies**: `PREP-019` if formatting moves to Compose resources
- **Acceptance Criteria**:
  - No `android.content.Context` in `data/model/extensions/`.
  - Relative time strings ("2 hours ago", "Tomorrow at 9:00 AM") remain localized in all supported locales (snapshot tests for a few locales).
  - *If OQ-08 decides to replace PrettyTime*: PrettyTime dependency removed and `kotlinx-datetime` added.
- **Expected Outcome**: Context-free, JVM-portable date/time formatting.

---

### PREP-009: Audit & Isolate Android-Specific Compose Modifiers and Utilities
- **Task ID**: `PREP-009`
- **Title**: Audit composables for Android-specific imports (`LocalContext`, `AndroidView`, HapticFeedback)
- **Description**: Review all composables in `core/ui/` and `feature/`. Ensure `LocalContext.current` is not referenced inside low-level UI components. Isolate Android-specific haptic feedback and orientation listeners behind platform composition locals or helpers.
- **Reason**: Low-level UI components should compile on Desktop Compose without needing an Android `Context`.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/editor/screens/EditScreen.kt`
  - `app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt`
- **Priority**: High
- **Effort**: Medium
- **Dependencies**: None
- **Acceptance Criteria**:
  - Core UI components in `core/ui/components` contain zero imports of `android.content.Context`.
  - Android-specific UI behaviors (haptics, clipboard) handled via standard Compose `LocalHapticFeedback` / `LocalClipboardManager`.
- **Expected Outcome**: Highly reusable Compose UI components ready for Desktop preview and execution.

---

### PREP-019: Migrate Android Resources to Compose Multiplatform Resources
- **Task ID**: `PREP-019`
- **Title**: Move strings, plurals and drawables used by shared UI to Compose Multiplatform Resources, and remove `R` from ViewModels
- **Description**: Shared UI and ViewModels reference `com.philkes.notallyx.R` (`R.string.*`, `R.plurals.*`, `R.drawable.*`), and `SideEffect`s carry `@StringRes Int`. Steps:
  1. Introduce a `UiText` / `StringResource`-based message type for ViewModels and side effects (can be done in `:app` now with an Android-backed implementation).
  2. Add the Compose Multiplatform resources plugin and move `values*/strings.xml` (30 locales) and vector drawables into `composeResources/` (`values-<locale>` qualifiers; note that CMP uses different region qualifiers, e.g. `values-pt-rBR` → `values-pt-rBR` must be verified).
  3. Keep Android-only resources (launcher mipmaps, widget/provider `xml/`, notification icons, activity themes) in Android `res/`.
  4. Adapt or replace the `translations-converter` and `auto-translation` Gradle plugins and `GenerateLocalesPreviewTask` (OQ-11).
- **Reason**: `R` is not available in `commonMain`. With 30 locales this is one of the largest mechanical changes and the one most likely to cause translation regressions.
- **Affected Files**:
  - `app/src/main/res/values*/strings.xml`, `app/src/main/res/drawable/`
  - All ViewModels and `*Model.kt` side-effect definitions using `R`/`@StringRes`
  - `build-logic/src/main/kotlin/com/philkes/notallyx/buildlogic/LocalizationPlugin.kt`, `tasks/GenerateLocalesPreviewTask.kt`
  - `AGENTS.md` (the "NO Hardcoded Strings" rule must reference `Res.string`)
- **Priority**: Critical
- **Effort**: Large
- **Dependencies**: OQ-11 resolved
- **Acceptance Criteria**:
  - Zero `com.philkes.notallyx.R` imports in ViewModels and in code destined for `:composeApp/commonMain`.
  - All 30 locales render identical strings (screenshot/locale tests).
  - Translation workflow documented and functional.
- **Expected Outcome**: Localized UI that compiles for Android and Desktop.

---

## Phase 4: Persistence & Driver Abstraction

### PREP-010: Abstract Room 3 Database Driver Construction
- **Task ID**: `PREP-010`
- **Title**: Encapsulate `BackupSQLiteDriver` and SQLite open helper factories for Room 3 KMP
- **Description**: Refactor database driver initialization in [`NotallyDatabase.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt) so that the `RoomDatabase.Builder` driver is supplied by an abstract `DatabaseDriverFactory`. Today the Android driver is `BackupSQLiteDriver` → `BackupOpenHelperFactory` (a `SupportSQLiteOpenHelper` bridge via `room3-sqlite-wrapper`/`sqlite-framework`) or SQLCipher's `SupportOpenHelperFactory`. It also hardcodes `DATABASE_VERSION = 11` separately from `@Database(version = 11)`, so derive it from a single constant. Make sure Room `Migration`s and `DataSchemaMigrations` only use `SQLiteConnection` APIs (no `SupportSQLiteDatabase`). On Desktop JVM, configure `BundledSQLiteDriver()`. Whether Android also switches to `BundledSQLiteDriver` is open (OQ-10).
- **Reason**: Room 3 KMP on Desktop JVM uses standard unencrypted `androidx.sqlite.driver.bundled.BundledSQLiteDriver`. `SupportSQLiteOpenHelper` is Android-only.
- **Note**: Desktop native-library packaging is a post-gate concern (the `desktopApp` module does not exist yet). `sqlite-bundled`'s JVM artifact ships the native libraries inside the JAR, but packaged distributions (jpackage, ProGuard) must be verified then.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/BackupSQLiteDriver.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/BackupOpenHelperFactory.kt`
- **Priority**: High
- **Effort**: Medium
- **Dependencies**: `PREP-001`
- **Acceptance Criteria**:
  - `DatabaseDriverFactory` interface created in the `:shared`-mapped package.
  - `NotallyDatabase` uses `DatabaseDriverFactory` to configure its builder.
  - A JVM unit test opens, migrates (v1→v11 fixtures) and checkpoints the DB using `BundledSQLiteDriver()` without Robolectric.
  - Schema version defined in exactly one place.
- **Expected Outcome**: Modular database driver pipeline ready for Desktop bundled SQLite driver.

---

### PREP-011: Isolate Tink AEAD Encrypted DataStore to Android Layer
- **Task ID**: `PREP-011`
- **Title**: Isolate Tink AEAD Android KeyStore dependency in `SecurePreferences` to `androidApp`
- **Description**: Encapsulate [`PreferencesModule.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/di/PreferencesModule.kt) so Tink AEAD encryption is bound strictly within Android Koin modules (`androidMain`). Introduce a `SecurePreferencesStore` interface in `:shared/commonMain` that `NotallyXPreferences` depends on. The Android implementation uses `datastore-tink` + Android KeyStore. The Desktop implementation uses standard DataStore in the OS app data directory (or the OS keychain, see OQ-05). Remove `databaseEncryptionKey` once `PREP-012` has completed the decryption migration.
- **Reason**: Encrypted preferences with Tink AEAD are an **Android-Only Feature**. Desktop JVM uses standard DataStore.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/security/TinkAeadProvider.kt`
  - `app/src/main/java/com/philkes/notallyx/core/preference/NotallyXPreferences.kt`
  - `app/src/main/java/com/philkes/notallyx/di/PreferencesModule.kt`
- **Priority**: High
- **Effort**: Small
- **Dependencies**: None
- **Acceptance Criteria**:
  - Tink KeyStore provider isolated within Android DI module (`androidMain`).
  - `NotallyXPreferences` depends only on `SecurePreferencesStore` and a standard DataStore factory.
  - Existing encrypted secure preferences on Android remain readable after the refactor (migration test).
- **Expected Outcome**: Encapsulated Tink AEAD preference pipeline.

---

### PREP-012: Sunset Database Encryption (Feature Flag + Decryption Migration)
- **Task ID**: `PREP-012`
- **Title**: Introduce `FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false` and decrypt existing encrypted databases
- **Description**: SQLCipher encryption is **currently live** on Android: it is enabled when biometric lock is on and `biometricLockEncryptsDb` is set (`NotallyDatabase.setupEncryption`, `SettingsScreen`, `SettingsViewModel`, `LockUtils`, `DatabaseTransitionActivity`). `DataSchemaMigrations` even sets `biometricLockEncryptsDb = true` for users who already had biometric lock. The flag does **not** exist yet. Steps:
  1. Add `const val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false` and hide the "encrypt database" option in Settings behind it.
  2. On startup, if the DB file is encrypted (`isEncryptedDatabase`), run a **one-time decryption migration** that reuses the existing decrypt path of `DatabaseTransitionActivity`. This requires a biometric unlock to obtain the passphrase, then a WAL checkpoint, an atomic file swap, and resetting `biometricLockEncryptsDb` to `false`. Keep the biometric UI lock itself working.
  3. Stop `DataSchemaMigrations` from enabling `biometricLockEncryptsDb`.
  4. Keep the SQLCipher dependency (Android only) until telemetry-free evidence suggests all users have migrated (OQ-03). Then remove SQLCipher, `databaseEncryptionKey` and the encrypted-DB branches.
- **Reason**: Encrypted databases cannot be opened by `BundledSQLiteDriver` on Desktop, nor restored from raw DB copies. Silently flipping a flag would lock existing users out of their notes.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/database/NotallyDatabase.kt`
  - `app/src/main/java/com/philkes/notallyx/core/database/DataSchemaMigrations.kt`
  - `app/src/main/java/com/philkes/notallyx/core/security/EncryptionUtils.kt`
  - `app/src/main/java/com/philkes/notallyx/core/security/LockUtils.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/migration/backup/DatabaseTransitionActivity.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/settings/{SettingsScreen,SettingsViewModel,SettingsModel}.kt`
- **Priority**: Critical
- **Effort**: Medium
- **Dependencies**: `PREP-010`. Blocked by OQ-03 (product decision & user communication).
- **Acceptance Criteria**:
  - `FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false` defined and the settings option hidden.
  - Instrumented test: an encrypted v11 DB is decrypted on first launch without data loss (note count, attachments, labels identical).
  - No code path can newly encrypt a database while the flag is `false`.
  - Changelog / documentation (`/documentation`) updated to explain the change.
- **Expected Outcome**: All Android installations converge on an unencrypted database that is portable to Desktop.

---

## Phase 5: Android-Only Feature Encapsulation

### PREP-013: Abstract `ReminderScheduler` Interface for Note Reminders
- **Task ID**: `PREP-013`
- **Title**: Abstract `AlarmManager` and `NotificationManager` behind a `ReminderScheduler` interface in `:shared`
- **Description**: Refactor [`AlarmExtensions.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/AlarmExtensions.kt) and [`NoteRemindersViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/NoteRemindersViewModel.kt) to delegate reminder scheduling to a `ReminderScheduler` interface in `:shared/commonMain`. Implement `AndroidReminderScheduler` in `androidApp` using `AlarmManager`/`NotificationManager`. `desktopApp` binds a **No-Op `ReminderScheduler`** initially, because reminders and notifications are Android-only features. Reminder metadata is still stored and editable on Desktop. A real Desktop scheduler (in-process timers + `java.awt.SystemTray` notifications while the app runs) is open (OQ-01). The reminder *rescheduling* logic triggered on boot/app update (`ReminderReceiver`) stays in `androidApp`.
- **Reason**: Establishes clean platform decoupling while preserving reminder metadata across platforms.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/feature/reminder/AlarmExtensions.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/reminder/NoteRemindersViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/reminder/PinnedNotificationManager.kt`
- **Priority**: High
- **Effort**: Medium
- **Dependencies**: `PREP-003`
- **Acceptance Criteria**:
  - `ReminderScheduler` interface created in `:shared/commonMain`.
  - ViewModels interact exclusively through `ReminderScheduler` (no `Manifest`/`PackageManager`/`Build` checks, see `PREP-022`).
  - Android implementation wraps `AlarmManager` and `NotificationManager`.
  - A No-Op implementation exists and is used in JVM unit tests.
- **Expected Outcome**: Decoupled reminder scheduling layer.

---

### PREP-014: Abstract Audio Recording & Playback Operations
- **Task ID**: `PREP-014`
- **Title**: Create `AudioPlayer` and `AudioRecorder` interfaces isolating Media3 and MediaRecorder
- **Description**: Refactor [`PlayAudioViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/PlayAudioViewModel.kt) and [`RecordAudioViewModel.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/RecordAudioViewModel.kt) to use abstract `AudioPlayer` and `AudioRecorder` interfaces defined in `:shared/commonMain`.
- **Reason**: Jetpack Media3 (`AudioPlayService`) and `MediaRecorder` (`AudioRecordService`) are Android-specific services. Recordings are AAC/`.m4a`, which Java Sound cannot decode, so the Desktop implementation choice (and whether Desktop supports recording at all) is open (OQ-07). Design the interfaces so that `AudioRecorder` can be absent (`null` / capability flag) on a platform.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/feature/media/audio/PlayAudioViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/media/audio/RecordAudioViewModel.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/media/audio/AudioPlayService.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/media/audio/AudioRecordService.kt`
- **Priority**: High
- **Effort**: Medium
- **Dependencies**: `PREP-003`
- **Acceptance Criteria**:
  - `AudioPlayer` and `AudioRecorder` interfaces defined in `:shared/commonMain`.
  - Audio ViewModels do not bind to Android `Service` directly.
  - Android implementations wrap `AudioPlayService` and `AudioRecordService`.
- **Expected Outcome**: Audio ViewModels ready for KMP shared compilation.

---

### PREP-015: Abstract `TaskScheduler` Interface for Background Auto-Backups
- **Task ID**: `PREP-015`
- **Title**: Create `TaskScheduler` interface in `:shared` isolating WorkManager workers
- **Description**: Create a `TaskScheduler` interface in `:shared/commonMain` for periodic tasks (`AutoBackupWorker`, `AutoRemoveDeletedNotesWorker`, `CleanupMissingAttachmentsWorker`). Move the actual work (backup creation, purge, cleanup) into plain `suspend` use cases in `:shared`, so workers become thin wrappers. Implement `AndroidTaskScheduler` in `androidApp` using `WorkManager` (`koin-androidx-workmanager` `workerOf` registrations move there). `desktopApp` binds a **No-Op `TaskScheduler`** initially. Whether Desktop runs these use cases on startup or periodically while running is open (OQ-02).
- **Reason**: Decouples auto-backup trigger scheduling from Android `WorkManager` and keeps the business logic shareable.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/NotallyXApplication.kt`
  - `app/src/main/java/com/philkes/notallyx/di/FeatureModule.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/migration/backup/AutoBackupWorker.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/migration/backup/CleanupMissingAttachmentsWorker.kt`
  - `app/src/main/java/com/philkes/notallyx/feature/main/routes/settings/AutoRemoveDeletedNotesWorker.kt`
- **Priority**: Medium
- **Effort**: Medium
- **Dependencies**: None
- **Acceptance Criteria**:
  - `TaskScheduler` interface created in `:shared/commonMain`.
  - `NotallyXApplication` and settings changes delegate background task scheduling to `TaskScheduler`.
  - Backup/purge/cleanup logic is unit-testable without WorkManager.
- **Expected Outcome**: Background task management decoupled from WorkManager.

---

### PREP-016: Isolate Biometric Lock UI and Secure Window Flags
- **Task ID**: `PREP-016`
- **Title**: Isolate `BiometricPrompt` and `LockedActivity` as Android-only security features
- **Description**: Keep [`LockedActivity.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/security/LockedActivity.kt) and [`BiometricAuthenticator.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/security/BiometricAuthenticator.kt) inside `androidApp`.
- **Reason**: Biometric/PIN UI app locking is an Android-Only Feature. On Desktop JVM, UI lock is bypassed.
- **Affected Files**:
  - `app/src/main/java/com/philkes/notallyx/core/security/BiometricAuthenticator.kt`
  - `app/src/main/java/com/philkes/notallyx/core/security/LockedActivity.kt`
  - `app/src/main/java/com/philkes/notallyx/core/security/LockUtils.kt`
- **Priority**: Medium
- **Effort**: Small
- **Dependencies**: None
- **Acceptance Criteria**:
  - Biometric lock components isolated in `androidApp`.
  - Shared preferences maintain `BiometricLock` enum preference without triggering `BiometricPrompt` on Desktop.
- **Expected Outcome**: Encapsulated Android biometric lock.

---

## Phase 6: Multi-Module & Testing Preparation

### PREP-017: Isolate Pure Kotlin Unit Tests from Robolectric Tests
- **Task ID**: `PREP-017`
- **Title**: Separate pure JVM unit tests from Robolectric Android runner tests
- **Description**: Reorganize unit tests under `app/src/test/`. Ensure domain tests, model extension tests, and ViewModel unit tests do not annotate with `@RunWith(RobolectricTestRunner::class)` or use Android `ApplicationProvider`. Split `:test-lib` (an Android library containing `UiTestBase`, `UiUtils` and fakes such as `FakeBiometricAuthenticator`) into pure-Kotlin fakes/fixtures (future `commonTest`/`jvmTest` usage) and Android instrumentation helpers. Roborazzi and Compose Screenshot tests stay Android-only. Whether Desktop gets its own screenshot tests is a post-gate decision.
- **Reason**: Tests for shared KMP code must run as standard Kotlin JVM unit tests without Android runtime overhead.
- **Affected Files**:
  - `app/src/test/kotlin/com/philkes/notallyx/...`
  - `test-lib/build.gradle.kts`
  - `test-lib/src/main/java/com/philkes/notallyx/test/...`
- **Priority**: High
- **Effort**: Medium
- **Dependencies**: `PREP-003`
- **Acceptance Criteria**:
  - Domain, Model, and ViewModel unit tests run without Robolectric in pure JVM test tasks.
  - Robolectric tests remain clearly demarcated for Android-specific component testing.
- **Expected Outcome**: Fast, portable test suite ready for KMP shared test tasks.

---

### PREP-018: Prepare Gradle Build Logic for KMP Plugin Structure
- **Task ID**: `PREP-018`
- **Title**: Refactor `build-logic` plugins to support Kotlin Multiplatform plugin application
- **Description**: `build-logic/` currently contains task-oriented plugins (`AndroidPackagingPlugin`, `DeveloperToolsPlugin`, `LocalizationPlugin`, `ScreenshotsPlugin`), not convention plugins. All Android/Kotlin configuration lives in `app/build.gradle.kts`. Extract convention plugins that separate pure Kotlin settings (JVM target 17, ktfmt, serialization, Koin compiler) from Android settings. Account for **AGP 9** constraints:
  - KMP library modules (`shared`, `composeApp`) must use `com.android.kotlin.multiplatform.library`. It supports a single variant (no build types/flavors such as the current `beta`), has Android resources and `BuildConfig` disabled by default, and uses a different test DSL.
  - `BuildConfig.VERSION_CODE/VERSION_NAME` (used in `LoggingExtensions.kt`) must be provided to shared code differently (e.g. an `AppInfo` interface bound in `androidApp`).
  - KSP for Room 3 in KMP is configured per target (`kspAndroid`, `kspDesktop`) and the Room schema directory (`app/schemas`) must move with the DB.
  - `LocalizationPlugin` / `GenerateLocalesPreviewTask` and the translations plugins depend on Android `res/` layout (see `PREP-019`).
- **Reason**: Prepares Gradle build logic for applying `kotlin("multiplatform")` in future `shared` and `composeApp` modules.
- **Affected Files**:
  - `build-logic/build.gradle.kts`
  - `build-logic/src/main/kotlin/com/philkes/notallyx/buildlogic/`
  - `app/build.gradle.kts`
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `gradle/libs.versions.toml`
- **Priority**: Medium
- **Effort**: Medium
- **Dependencies**: None (can run in parallel with other phases; must be complete before module extraction)
- **Acceptance Criteria**:
  - `build-logic` provides reusable Kotlin-only and Android-specific convention plugins, and `app/build.gradle.kts` uses them.
  - No shared-bound code reads `BuildConfig` directly.
  - `./gradlew assembleDebug`, `./gradlew assembleBeta` and `./gradlew testDebugUnitTest` continue to succeed cleanly.
- **Expected Outcome**: Modernized, multi-module ready Gradle build system.

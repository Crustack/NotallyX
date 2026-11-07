# Tasks: Kotlin Multiplatform Android Migration

**Input**: Design documents from `/specs/003-kmp-android-migration/`
**Prerequisites**: [plan.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/003-kmp-android-migration/plan.md) (required), [spec.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/003-kmp-android-migration/spec.md) (required for user stories), [research.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/003-kmp-android-migration/research.md), [data-model.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/003-kmp-android-migration/data-model.md), [contracts/module-boundaries.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/003-kmp-android-migration/contracts/module-boundaries.md), [quickstart.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/003-kmp-android-migration/quickstart.md)

**Tests**: Existing unit, Roborazzi, Compose Preview Screenshot, and instrumented UI test suites are migrated and executed across `:shared`, `:composeApp`, `:test-lib`, and `:app` to verify zero regressions (`SC-001`–`SC-005`).

**Organization**: Tasks are grouped by phase and user story (`US2` → `US3` → `US1`) so that each KMP module (`:shared`, `:composeApp`, `:app`) is extracted once without double-moving files and can be compiled and tested independently at its phase checkpoint.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Exact file paths are included in every task description

## Path Conventions

- **`:shared` KMP Domain & Data Module**: `shared/src/commonMain/kotlin/`, `shared/src/androidMain/kotlin/`, `shared/src/androidMain/java/`, `shared/src/androidMain/res/`, `shared/src/commonTest/kotlin/`, `shared/src/androidHostTest/kotlin/`, `shared/schemas/`
- **`:composeApp` KMP Presentation & UI Module**: `composeApp/src/commonMain/kotlin/`, `composeApp/src/androidMain/kotlin/`, `composeApp/src/commonTest/kotlin/`, `composeApp/src/androidHostTest/kotlin/`
- **`:app` Android Application Host Module**: `app/src/main/java/`, `app/src/main/res/`, `app/src/test/kotlin/`, `app/src/screenshotTest/kotlin/`, `app/src/androidTest/kotlin/`
- **`:test-lib` Shared Test Utilities Module**: `test-lib/src/main/java/`
- **`:build-logic` Gradle Convention Plugins**: `build-logic/convention/src/main/kotlin/`

---

## Phase 1: Setup (Gradle Catalog, Subproject Registration & Convention Plugins)

**Purpose**: Configure Kotlin Multiplatform and AGP 9.4.1 KMP library plugins in the version catalog, register `:shared` and `:composeApp` in Gradle settings, update `:build-logic` convention plugins to support KMP modules, and create the module build scripts.

- [X] T001 Add `kotlin-multiplatform` (`org.jetbrains.kotlin.multiplatform`) and `android-kotlin-multiplatform-library` (`com.android.kotlin.multiplatform.library`) plugin aliases to `gradle/libs.versions.toml`
- [X] T002 Declare `alias(libs.plugins.kotlin.multiplatform) apply false` and `alias(libs.plugins.android.kotlin.multiplatform.library) apply false` in root `build.gradle.kts`
- [X] T003 Include `:shared` and `:composeApp` subprojects alongside `:app` and `:test-lib` in `settings.gradle.kts`
- [X] T004 [P] Update `NotallyxKotlinConventionPlugin` to configure `KotlinMultiplatformExtension` alongside `KotlinTopLevelExtension` and ktfmt for KMP source sets in `build-logic/src/main/kotlin/com/philkes/notallyx/buildlogic/convention/NotallyxKotlinConventionPlugin.kt`
- [X] T005 [P] Update `LocalizationPlugin` (`checkTranslations` and `formatTranslations` tasks) to resolve `res` directory from `shared/src/androidMain/res` in `build-logic/src/main/kotlin/com/philkes/notallyx/buildlogic/LocalizationPlugin.kt`
- [X] T006 [P] Create `:shared` KMP build configuration (`kotlin.multiplatform`, `android.kotlin.multiplatform.library`, `androidx.room`, `ksp`, `koin-compiler`, `namespace = "com.philkes.notallyx"`, `withJava()`, `androidResources.enable = true`, `withHostTest`) in `shared/build.gradle.kts`
- [X] T007 [P] Create `:composeApp` KMP build configuration (`kotlin.multiplatform`, `android.kotlin.multiplatform.library`, `kotlin.compose`, `kotlin.serialization`, `koin-compiler`, `namespace = "com.philkes.notallyx.composeapp"`, `api(project(":shared"))`, `withHostTest`) in `composeApp/build.gradle.kts`

---

## Phase 2: Foundational (Platform Contract vs. Android Implementation File Splits)

**Purpose**: Split hybrid files created during `002-kmp-migration-prep` where platform-agnostic interfaces and `Android*` implementations currently reside in the same `.kt` file, and remove unused Android resource references from domain models.

**⚠️ CRITICAL**: Must complete before moving files into `:shared` and `:composeApp` so `commonMain` source sets contain zero `android.*` or `com.philkes.notallyx.R` imports.

- [X] T008 [P] Remove unused `StaticTextProvider` interface implementation and `com.philkes.notallyx.R` import from `NoteViewMode` in `app/src/main/java/com/philkes/notallyx/data/model/NoteViewMode.kt`
- [X] T009 [P] Split `AndroidPathProvider` out of `PathProvider.kt` into `AndroidPathProvider.kt` in `app/src/main/java/com/philkes/notallyx/core/platform/PathProvider.kt` and `app/src/main/java/com/philkes/notallyx/core/platform/AndroidPathProvider.kt`
- [X] T010 [P] Split `AndroidDatabaseDriverFactory` out of `DatabaseDriverFactory.kt` into `AndroidDatabaseDriverFactory.kt` in `app/src/main/java/com/philkes/notallyx/core/database/DatabaseDriverFactory.kt` and `app/src/main/java/com/philkes/notallyx/core/database/AndroidDatabaseDriverFactory.kt`
- [X] T011 [P] Split `AndroidFileStorage` and `Uri.toPlatformUri()` / `PlatformUri.toAndroidUri()` out of `FileStorage.kt` into `AndroidFileStorage.kt` in `app/src/main/java/com/philkes/notallyx/core/platform/FileStorage.kt` and `app/src/main/java/com/philkes/notallyx/core/platform/AndroidFileStorage.kt`
- [X] T012 [P] Split `AndroidSecurePreferencesStore` out of `SecurePreferencesStore.kt` into `AndroidSecurePreferencesStore.kt` in `app/src/main/java/com/philkes/notallyx/core/preference/SecurePreferencesStore.kt` and `app/src/main/java/com/philkes/notallyx/core/preference/AndroidSecurePreferencesStore.kt`
- [X] T013 [P] Split `AndroidPdfExporter` out of `PdfExporter.kt` into `AndroidPdfExporter.kt` in `app/src/main/java/com/philkes/notallyx/feature/migration/backup/PdfExporter.kt` and `app/src/main/java/com/philkes/notallyx/feature/migration/backup/AndroidPdfExporter.kt`
- [X] T014 [P] Split `AndroidReminderScheduler` out of `ReminderScheduler.kt` into `AndroidReminderScheduler.kt` in `app/src/main/java/com/philkes/notallyx/feature/reminder/ReminderScheduler.kt` and `app/src/main/java/com/philkes/notallyx/feature/reminder/AndroidReminderScheduler.kt`
- [X] T015 [P] Split `AndroidTaskScheduler` out of `TaskScheduler.kt` into `AndroidTaskScheduler.kt` in `app/src/main/java/com/philkes/notallyx/core/platform/TaskScheduler.kt` and `app/src/main/java/com/philkes/notallyx/core/platform/AndroidTaskScheduler.kt`
- [X] T016 [P] Split `AndroidServiceAudioPlayer` out of `AudioPlayer.kt` into `AndroidServiceAudioPlayer.kt` in `app/src/main/java/com/philkes/notallyx/feature/media/audio/AudioPlayer.kt` and `app/src/main/java/com/philkes/notallyx/feature/media/audio/AndroidServiceAudioPlayer.kt`
- [X] T017 [P] Split `AndroidServiceAudioRecorder` out of `AudioRecorder.kt` into `AndroidServiceAudioRecorder.kt` in `app/src/main/java/com/philkes/notallyx/feature/media/audio/AudioRecorder.kt` and `app/src/main/java/com/philkes/notallyx/feature/media/audio/AndroidServiceAudioRecorder.kt`
- [X] T018 [P] Split `AndroidUiText.kt` (`UiText.asString(Context)` and `@Composable UiText.asString()`) out of `UiText.kt` in `app/src/main/java/com/philkes/notallyx/core/ui/platform/UiText.kt` and `app/src/main/java/com/philkes/notallyx/core/ui/platform/AndroidUiText.kt`
- [X] T019 [P] Split `AndroidFilePicker` and `rememberFilePicker` out of `FilePicker.kt` into `AndroidFilePicker.kt` in `app/src/main/java/com/philkes/notallyx/core/ui/platform/FilePicker.kt` and `app/src/main/java/com/philkes/notallyx/core/ui/platform/AndroidFilePicker.kt`
- [X] T020 [P] Split `AndroidShareHandler` and `rememberShareHandler` out of `ShareHandler.kt` into `AndroidShareHandler.kt` in `app/src/main/java/com/philkes/notallyx/core/ui/platform/ShareHandler.kt` and `app/src/main/java/com/philkes/notallyx/core/ui/platform/AndroidShareHandler.kt`
- [X] T021 [P] Split `AndroidPermissionChecker` and `rememberPermissionChecker` out of `PermissionChecker.kt` into `AndroidPermissionChecker.kt` in `app/src/main/java/com/philkes/notallyx/core/ui/platform/PermissionChecker.kt` and `app/src/main/java/com/philkes/notallyx/core/ui/platform/AndroidPermissionChecker.kt`

**Checkpoint**: All platform contracts are cleanly separated from Android-specific implementations.

---

## Phase 3: User Story 2 - Multiplatform Shared Domain and Data Separation (Priority: P1) 🎯 MVP

**Goal**: Extract all domain models, Room entities/DAOs/migrations, preferences, encryption, backup/import/export logic, shared Android resources, and domain/data unit tests into the `:shared` KMP module (`commonMain`, `androidMain`, `commonTest`, `androidHostTest`).

**Independent Test**: Run `./gradlew :shared:compileAndroidMain :shared:testAndroidHostTest` and verify zero `android.*` or `com.philkes.notallyx.R` imports in `shared/src/commonMain`.

### Shared Common Source Set (`shared/src/commonMain`)

- [X] T022 [P] [US2] Move all 18 domain model and entity files (`Attachment.kt`, `Audio.kt`, `BaseNote.kt`, `ColorString.kt`, `FileAttachment.kt`, `Folder.kt`, `Header.kt`, `Item.kt`, `Label.kt`, `ListItem.kt`, `ModelExtensions.kt`, `NoteTextFormatting.kt`, `NoteViewMode.kt`, `Reminder.kt`, `SearchResult.kt`, `SpanRepresentation.kt`, `Type.kt`) to `shared/src/commonMain/kotlin/com/philkes/notallyx/data/model/`
- [X] T023 [P] [US2] Move Room DAOs (`BaseNoteDao.kt`, `CommonDao.kt`, `LabelDao.kt`, `NoteIdReminder.kt`) and `Converters.kt` + `DatabaseDriverFactory.kt` to `shared/src/commonMain/kotlin/com/philkes/notallyx/data/`
- [X] T024 [P] [US2] Move pure platform contracts (`AppInfo.kt`, `AudioPlayer.kt`, `AudioRecorder.kt`, `FileStorage.kt`, `JvmFileStorage.kt`, `Logger.kt`, `MimeTypeUtils.kt`, `PathProvider.kt`, `PdfExporter.kt`, `PlatformUri.kt`, `ReminderScheduler.kt`, `TaskScheduler.kt`) to `shared/src/commonMain/kotlin/com/philkes/notallyx/core/platform/`
- [X] T025 [P] [US2] Move `UiText.kt` to `shared/src/commonMain/kotlin/com/philkes/notallyx/core/ui/model/UiText.kt`
- [X] T026 [P] [US2] Move pure utilities (`SearchExtensions.kt`, `SortedList.kt`, `TextExtensions.kt`, `UndoRedoManager.kt`, `backup/NoteExporter.kt`, `changehistory/Change.kt`, `changehistory/ChangeHistory.kt`, `changehistory/EditTextState.kt`, `changehistory/EditTextWithHistoryChange.kt`, `changehistory/ListChange.kt`, `changehistory/ValueChange.kt`) to `shared/src/commonMain/kotlin/com/philkes/notallyx/utils/`
- [X] T027 [P] [US2] Move pure preferences contract (`SecurePreferencesStore.kt`) and pure note importers (`ImportException.kt`, `ImportProgress.kt`, `NoteImporter.kt`, `google/GoogleKeepImporter.kt`, `evernote/EvernoteImporter.kt`, `evernote/EvernoteExport.kt`, `quillpad/QuillpadImporter.kt`, `plain/PlainTextImporter.kt`, `json/JsonNoteParser.kt`) to `shared/src/commonMain/kotlin/com/philkes/notallyx/feature/settings/preferences/` and `shared/src/commonMain/kotlin/com/philkes/notallyx/data/imports/`

### Shared Android Source Set & Resources (`shared/src/androidMain` & `shared/schemas`)

- [X] T028 [P] [US2] Move Room schemas (`1.json`–`8.json`) from `app/schemas/com.philkes.notallyx.data.NotallyDatabase/` to `shared/schemas/com.philkes.notallyx.data.NotallyDatabase/`
- [X] T029 [P] [US2] Move shared Android resources (`values*`, `drawable*`, `font*`, `xml/backup_rules.xml`, `xml/data_extraction_rules.xml`, `xml/file_paths.xml`) from `app/src/main/res/` to `shared/src/androidMain/res/` (retaining `mipmap*`, `xml/shortcuts.xml`, and `xml/*_widget_info.xml` in `app/src/main/res/`)
- [X] T030 [P] [US2] Move Android database classes (`NotallyDatabase.kt`, `AndroidDatabaseDriverFactory.kt`, `BackupSQLiteDriver.kt`, `BackupOpenHelperFactory.kt`, `DataSchemaMigrations.kt`, `DataUtil.kt`) to `shared/src/androidMain/kotlin/com/philkes/notallyx/data/`
- [X] T031 [P] [US2] Move Android import extensions (`ExternalImporter.kt`, `NotesImporter.kt`, `ImportExtensions.kt`, `txt/PlainTextExtendedImporter.kt`) to `shared/src/androidMain/kotlin/com/philkes/notallyx/data/imports/`
- [X] T032 [P] [US2] Move Android platform implementations (`AndroidFileStorage.kt`, `AndroidLogger.kt`, `AndroidPathProvider.kt`, `AndroidPdfExporter.kt`, `PdfExtensions.kt`, `StaticTextProvider.kt`) to `shared/src/androidMain/kotlin/com/philkes/notallyx/core/`
- [X] T033 [P] [US2] Move Android preferences classes (`BasePreference.kt`, `BootstrapPreferences.kt`, `Constants.kt`, `LockedFolderObserver.kt`, `NotallyXPreferences.kt`, `Preference.kt`, `PreferenceCategories.kt`, `PreferenceModel.kt`, `AndroidSecurePreferencesStore.kt`, `SharedPreferencesExtensions.kt`) to `shared/src/androidMain/kotlin/com/philkes/notallyx/feature/settings/preferences/`
- [X] T034 [P] [US2] Move Android utility, backup, and security classes (`Cache.kt`, `CompressUtility.kt`, `CoroutinesExtensions.kt`, `ErrorUtils.kt`, `FileError.kt`, `IOExtensions.kt`, `Log.kt`, `MicrophonePermissionUtils.kt`, `MIMEUtils.kt`, `NoteSplitUtils.kt`, `backup/BackupExtensions.kt`, `backup/ExportExtensions.kt`, `backup/ImportExtensions.kt`, `backup/ModifiedContextWrapper.kt`, `backup/XmlUtils.kt`, `security/EncryptionUtils.kt`, `security/TinkAeadProvider.kt`) to `shared/src/androidMain/kotlin/com/philkes/notallyx/utils/`
- [X] T035 [P] [US2] Move `SQLCipherUtils.java` to `shared/src/androidMain/java/com/philkes/notallyx/utils/security/SQLCipherUtils.java`
- [X] T036 [US2] Move `DatabaseModule.kt` and `PreferencesModule.kt` to `shared/src/androidMain/kotlin/com/philkes/notallyx/core/di/`

### Shared Unit Tests (`shared/src/commonTest` & `shared/src/androidHostTest`)

- [X] T037 [P] [US2] Move platform-agnostic domain and utility unit tests (`ModelExtensionsTest.kt`, `ReminderTest.kt`, `SpanRepresentationTest.kt`, `UndoRedoManagerTest.kt`, `ChangeHistoryTest.kt`) to `shared/src/commonTest/kotlin/`
- [X] T038 [P] [US2] Move Android host unit tests for database, converters, importers, and preferences (`ConvertersTest.kt`, `NotallyDatabaseMigrationTest.kt`, `DataUtilTest.kt`, `EvernoteImporterTest.kt`, `GoogleKeepImporterTest.kt`, `PlainTextImporterTest.kt`, `QuillpadImporterTest.kt`, `ImportExtensionsTest.kt`, `ExportTest.kt`, `NotallyXPreferencesTest.kt`, `BackupMigrationTest.kt`, `NoteSplitUtilsTest.kt`, `TextExtensionsTest.kt`) to `shared/src/androidHostTest/kotlin/`
- [X] T039 [US2] Verify `:shared` compiles and passes all unit tests via `./gradlew :shared:compileAndroidMain :shared:testAndroidHostTest`

**Checkpoint**: User Story 2 (`:shared` module) is independently compilable, tested, and verified to have zero `android.*` imports in `commonMain`.

---

## Phase 4: User Story 3 - Multiplatform Shared Presentation and UI Separation (Priority: P2)

**Goal**: Extract all shared navigation routes, `BaseViewModel`, `core/ui/` design system components/dialogs/themes, `ComposeFeatureModule`, and all 18 feature MVI architecture quads (`Model`, `ViewModel`, `Screen`, `Route`) into `:composeApp` (`commonMain`, `androidMain`, `commonTest`, `androidHostTest`).

**Independent Test**: Run `./gradlew :composeApp:compileAndroidMain :composeApp:testAndroidHostTest` and verify zero `android.*` imports in `composeApp/src/commonMain`.

### ComposeApp Common Source Set (`composeApp/src/commonMain`)

- [X] T040 [P] [US3] Move `NotallyRoute.kt` and `Navigator.kt` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/navigation/`
- [X] T041 [P] [US3] Move presentation platform contracts (`FilePicker.kt`, `ShareHandler.kt`, `PermissionChecker.kt`) to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/platform/`

### ComposeApp Android Source Set (`composeApp/src/androidMain`)

- [X] T042 [P] [US3] Move `BaseViewModel.kt`, `AndroidUiText.kt`, and `AndroidFilePicker.kt`, `AndroidShareHandler.kt`, `AndroidPermissionChecker.kt` to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/core/ui/`
- [X] T043 [P] [US3] Move `core/ui/component/` (`NoteHeader.kt`, `NoteContent.kt`, `NoteList.kt`, `SearchBar.kt`, `NotallyTopAppBar.kt`, `PinnedToggleRow.kt`, `SwipeableListItem.kt`, `RichTextComponents.kt`, `AudioAttachmentItem.kt`, `ImageAttachmentGrid.kt`, `FileAttachmentChip.kt`, `LabelChip.kt`, `ColorPickerGrid.kt`, etc.), `core/ui/dialog/` (`ConfirmationDialog.kt`, `InputTextDialog.kt`, `ChoiceDialog.kt`, `ProgressDialog.kt`, `ColorPickerDialog.kt`, `NoteClickAttachmentDialog.kt`, `SelectLabelsDialog.kt`, `AudioRecordDialog.kt`, `PermissionRationaleDialog.kt`, `MoveFolderBlockedDialog.kt`, `NoteCharacterLimitDialog.kt`), `core/ui/extension/`, `core/ui/image/` (`AttachmentImageLoader.kt`, `AudioWaveView.kt`), `core/ui/text/`, `core/ui/theme/`, and `core/ui/widget/` to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/core/ui/`
- [X] T044 [P] [US3] Move `feature/labels/` MVI files (`LabelsModel.kt`, `LabelsViewModel.kt`, `LabelsScreen.kt`, `LabelsRoute.kt`, `SelectLabelsModel.kt`, `SelectLabelsViewModel.kt`, `SelectLabelsScreen.kt`, `SelectLabelsRoute.kt`, `DisplayLabelScreen.kt`, `DisplayLabelRoute.kt`, `LabelData.kt`) to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/feature/labels/`
- [X] T045 [P] [US3] Move `feature/editor/` MVI files (`NotallyModel.kt`, `EditNoteModel.kt`, `EditNoteViewModel.kt`, `TakeNoteScreen.kt`, `TakeNoteRoute.kt`, `MakeListScreen.kt`, `MakeListRoute.kt`, `EditorTopBar.kt`, `EditorBottomBar.kt`, `FormattingToolbar.kt`, `NoteActionBottomSheet.kt`, `FindInNoteBar.kt`, `CheckListContent.kt`, `AddItemInputRow.kt`, `ChildItemsToggleRow.kt`, `ActionSelectionScreen.kt`, `ActionSelectionRoute.kt`, `JumpToNoteDialog.kt`, `DynamicNoteActionHandler.kt`, `list/`, `model/`, `span/`) to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/feature/editor/`
- [X] T046 [P] [US3] Move shared `feature/main/` UI & MVI files (`BaseNoteModel.kt`, `MainScreen.kt`, `NotallyApp.kt`, `DrawerItem.kt`, `NoteActionHandler.kt`, `NotesScreen.kt`, `NotesRoute.kt`, `DeletedRoute.kt`, `ArchivedRoute.kt`, `UnlabeledRoute.kt`, `SearchModel.kt`, `SearchViewModel.kt`, `SearchScreen.kt`, `SearchRoute.kt`) to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/feature/main/` (keeping `MainActivity.kt`, `MainActivityViewModel.kt`, `EditActivityViewModel.kt`, `ErrorActivity*`, `DatabaseTransitionActivity.kt` in `:app`)
- [X] T047 [P] [US3] Move shared `feature/media/` UI & MVI files (`ImageViewerModel.kt`, `ImageViewerViewModel.kt`, `ImageViewerScreen.kt`, `ImageViewerRoute.kt`, `AudioPlayModel.kt`, `AudioPlayViewModel.kt`, `AudioRecordModel.kt`, `AudioRecordViewModel.kt`) to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/feature/media/` (keeping `AudioPlayService.kt`, `AudioRecordService.kt`, `AndroidServiceAudioPlayer.kt`, `AndroidServiceAudioRecorder.kt` in `:app`)
- [X] T048 [P] [US3] Move shared `feature/reminder/` UI & MVI files (`RemindersModel.kt`, `RemindersViewModel.kt`, `RemindersScreen.kt`, `RemindersRoute.kt`, `DateTimePickerDialogs.kt`, `CustomRepeatDialog.kt`, `NotificationBlockedDialogs.kt`) to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/feature/reminder/` (keeping `ReminderReceiver.kt` and `AndroidReminderScheduler.kt` in `:app`)
- [X] T049 [P] [US3] Move shared `feature/settings/` UI & MVI files (`SettingsModel.kt`, `SettingsViewModel.kt`, `SettingsScreen.kt`, `SettingsRoute.kt`, `SettingsComponents.kt`, `SettingsDialogs.kt`, `LibrariesDialog.kt`, `BackupPasswordDialog.kt`, `ImportSourceDialog.kt`, `SelectDisplayedLabelsDialog.kt`) to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/feature/settings/` (keeping `LockedActivity.kt`, `BiometricAuthenticator.kt`, and `work/*` in `:app`)
- [X] T050 [US3] Update `ComposeFeatureModule.kt` to remove `:app`-only ViewModels (`MainActivityViewModel`, `EditActivityViewModel`, `ErrorActivityViewModel`) and move `ComposeFeatureModule.kt` to `composeApp/src/androidMain/kotlin/com/philkes/notallyx/core/di/ComposeFeatureModule.kt`

### ComposeApp Unit Tests (`composeApp/src/commonTest` & `composeApp/src/androidHostTest`)

- [X] T051 [P] [US3] Move `NavigatorTest.kt` to `composeApp/src/commonTest/kotlin/com/philkes/notallyx/navigation/NavigatorTest.kt`
- [X] T052 [P] [US3] Move shared ViewModel and list manager unit tests (`BaseNoteModelTest.kt`, `LabelsViewModelTest.kt`, `RemindersViewModelTest.kt`, `SearchViewModelTest.kt`, `SelectLabelsViewModelTest.kt`, `SettingsViewModelTest.kt`, `ImageViewerViewModelTest.kt`, `AudioPlayViewModelTest.kt`, `AudioRecordViewModelTest.kt`, `ListManagerTest.kt`, `ListManagerWithHistoryTest.kt`, `ListItemDragCallbackTest.kt`) to `composeApp/src/androidHostTest/kotlin/`
- [X] T053 [US3] Verify `:composeApp` compiles and passes all unit tests via `./gradlew :composeApp:compileAndroidMain :composeApp:testAndroidHostTest`

**Checkpoint**: User Story 3 (`:composeApp` module) is independently compilable, tested, and verified to have zero `android.*` imports in `commonMain`.

---

## Phase 5: User Story 1 - Seamless Android User Experience and Upgrade Continuity (Priority: P1)

**Goal**: Configure `:app` as a lean Android application host (`namespace = "com.philkes.notallyx.app"`, `applicationId = "com.philkes.notallyx"`) wired to `:composeApp` and `:shared`, update `:test-lib`, and verify all Android unit, Roborazzi, Compose Preview Screenshot, and instrumented UI tests pass with 100% upgrade continuity.

**Independent Test**: Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug testDebugScreenshotTest` and verify APK `applicationId`, manifest component names, and database/preference upgrade compatibility.

- [X] T054 [US1] Update `:app/build.gradle.kts` to set `namespace = "com.philkes.notallyx.app"`, keep `defaultConfig.applicationId = "com.philkes.notallyx"`, depend on `implementation(project(":composeApp"))` and `implementation(project(":shared"))`, update `autoTranslate` `resDir` to `../shared/src/androidMain/res`, and remove migrated Room schema KSP arguments in `app/build.gradle.kts`
- [X] T055 [P] [US1] Update `AndroidManifest.xml` in `app/src/main/AndroidManifest.xml` so all Activities, Services, BroadcastReceivers, and FileProviders resolve against `namespace = "com.philkes.notallyx.app"` while preserving external class names (`com.philkes.notallyx.*`)
- [X] T056 [P] [US1] Update `AndroidAppInfo.kt` to import `com.philkes.notallyx.app.BuildConfig` in `app/src/main/java/com/philkes/notallyx/core/platform/AndroidAppInfo.kt`
- [X] T057 [US1] Update `AppModule.kt` in `app/src/main/java/com/philkes/notallyx/core/di/AppModule.kt` to register Android-host singletons (`AndroidPathProvider`, `AndroidFileStorage`, `AndroidPdfExporter`, `AndroidReminderScheduler`, `AndroidTaskScheduler`, `AndroidServiceAudioPlayer`, `AndroidServiceAudioRecorder`, `AndroidLogger`, `AndroidAppInfo`, `BiometricAuthenticator`) and `:app`-only ViewModels (`MainActivityViewModel`, `EditActivityViewModel`, `ErrorActivityViewModel`)
- [X] T058 [P] [US1] Update `:test-lib/build.gradle.kts` to depend on `api(project(":shared"))` and `api(project(":composeApp"))` in `test-lib/build.gradle.kts`
- [X] T059 [US1] Verify remaining `:app` unit, Roborazzi, and Compose Preview Screenshot tests (`MainActivityViewModelTest.kt`, `EditActivityViewModelTest.kt`, `ErrorActivityViewModelTest.kt`, `NoteWidgetTest.kt`, `ComposePreviewRoborazziTest.kt`, `ScreenRoborazziTest.kt`, `AllPreviewsScreenshots.kt`) compile and pass via `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:verifyRoborazziDebug testDebugScreenshotTest :app:compileDebugAndroidTestKotlin`

**Checkpoint**: `:app`, `:composeApp`, `:shared`, and `:test-lib` build and pass all unit, Roborazzi, and screenshot tests; debug APK produces `applicationId = "com.philkes.notallyx"`.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Update localization scripts/workflows and documentation for the new KMP module paths, enforce code formatting (`ktfmtFormat`), and run the full `quickstart.md` verification suite.

- [X] T060 [P] Update translation coverage script path from `app/src/main/res` to `shared/src/androidMain/res` in `.github/scripts/update_translation_coverage.py`
- [X] T061 [P] Update GitHub Actions translation workflow trigger path from `app/src/main/res/values/strings.xml` to `shared/src/androidMain/res/values/strings.xml` in `.github/workflows/update-translation-coverage.yaml`
- [X] T062 [P] Update `AGENTS.md` architecture and module structure documentation to reflect `:shared`, `:composeApp`, `:app`, `:test-lib`, and `:build-logic` in `AGENTS.md`
- [X] T063 Run `./gradlew ktfmtFormat ktfmtFormat checkTranslations` and execute all verification checks in `specs/003-kmp-android-migration/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — can start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1 — splits hybrid platform contract/implementation files before module extraction. Blocks Phases 3, 4, and 5.
- **Phase 3 (User Story 2 - `:shared` P1)**: Depends on Phase 2 — extracts domain and data layers into `:shared` and verifies `:shared` compiles and passes unit tests independently.
- **Phase 4 (User Story 3 - `:composeApp` P2)**: Depends on Phase 3 (`:composeApp` depends on `api(project(":shared"))`) — extracts presentation and UI layers into `:composeApp` and verifies `:composeApp` compiles and passes unit tests independently.
- **Phase 5 (User Story 1 - `:app` Host & Integration P1)**: Depends on Phases 3 and 4 — wires `:app` and `:test-lib` to `:composeApp` and `:shared` and verifies end-to-end Android application packaging and regression suites.
- **Phase 6 (Polish)**: Depends on Phase 5 — updates CI/localization scripts, documentation, formatting, and final verification.

### User Story Dependencies

- **User Story 2 (`:shared`, P1)**: Starts immediately after Phase 2 Foundational. Has zero dependency on `:composeApp` or `:app`.
- **User Story 3 (`:composeApp`, P2)**: Depends on `:shared` (`US2`). Has zero dependency on `:app`.
- **User Story 1 (`:app` Host & Upgrade Continuity, P1)**: Depends on `:shared` (`US2`) and `:composeApp` (`US3`) to assemble the complete Android application.

### Parallel Opportunities

- **Phase 1**: `T004`, `T005`, `T006`, `T007` touch separate Gradle build/plugin files and can run in parallel after `T001`–`T003`.
- **Phase 2**: `T008`–`T021` all split distinct `.kt` files and can run in parallel.
- **Phase 3 (`US2`)**: `T022`–`T027` (`commonMain` moves) and `T028`–`T035` (`androidMain` moves) operate on distinct packages and can run in parallel before `T036`–`T039`.
- **Phase 4 (`US3`)**: `T040`–`T041` (`commonMain` moves) and `T042`–`T049` (`androidMain` feature moves) operate on distinct feature packages and can run in parallel before `T050`–`T053`.
- **Phase 5 (`US1`)**: `T055`, `T056`, `T058` can run in parallel alongside `T054` and `T057`.
- **Phase 6**: `T060`, `T061`, `T062` modify distinct files and can run in parallel before `T063`.

---

## Parallel Example: Phase 2 (Foundational Contract Splits) & Phase 3 (`:shared` Extraction)

```bash
# Phase 2: Split all hybrid contract/Android implementation files in parallel:
Task: "T009 [P] Split AndroidPathProvider out of PathProvider.kt"
Task: "T010 [P] Split AndroidDatabaseDriverFactory out of DatabaseDriverFactory.kt"
Task: "T011 [P] Split AndroidFileStorage out of FileStorage.kt"
Task: "T012 [P] Split AndroidSecurePreferencesStore out of SecurePreferencesStore.kt"

# Phase 3 (US2): Move domain models, DAOs, and pure utilities into shared/src/commonMain in parallel:
Task: "T022 [P] [US2] Move all 18 domain model and entity files to shared/src/commonMain/kotlin/com/philkes/notallyx/data/model/"
Task: "T023 [P] [US2] Move Room DAOs and Converters.kt + DatabaseDriverFactory.kt to shared/src/commonMain/kotlin/com/philkes/notallyx/data/"
Task: "T024 [P] [US2] Move pure platform contracts to shared/src/commonMain/kotlin/com/philkes/notallyx/core/platform/"
```

---

## Implementation Strategy

### Incremental Delivery

1. **Complete Phase 1 + Phase 2**: Gradle KMP plugins registered, `:shared` and `:composeApp` configured, and all hybrid platform contract files cleanly split.
2. **Complete Phase 3 (User Story 2 - `:shared`)**: Validate `:shared` compiles and passes all domain/data unit tests independently (`./gradlew :shared:compileAndroidMain :shared:testAndroidHostTest`) with zero `android.*` imports in `shared/src/commonMain`.
3. **Complete Phase 4 (User Story 3 - `:composeApp`)**: Validate `:composeApp` compiles and passes all navigation/ViewModel unit tests independently (`./gradlew :composeApp:compileAndroidMain :composeApp:testAndroidHostTest`) with zero `android.*` imports in `composeApp/src/commonMain`.
4. **Complete Phase 5 (User Story 1 - `:app` Host)**: Validate `:app` packages with `applicationId = "com.philkes.notallyx"` and passes all unit, Roborazzi, and screenshot tests.
5. **Complete Phase 6 (Polish)**: Validate translation scripts, `ktfmtFormat`, and full `quickstart.md` verification suite.

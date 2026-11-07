# Tasks: Desktop Target for Kotlin Multiplatform

**Input**: Design documents from `/specs/004-kmp-desktop-target/`  
**Prerequisites**: [plan.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/004-kmp-desktop-target/plan.md) (required), [spec.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/004-kmp-desktop-target/spec.md) (required for user stories), [research.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/004-kmp-desktop-target/research.md), [data-model.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/004-kmp-desktop-target/data-model.md), [contracts/desktop-contracts.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/004-kmp-desktop-target/contracts/desktop-contracts.md), [quickstart.md](file:///home/user/AndroidStudioProjects/NotallyX/specs/004-kmp-desktop-target/quickstart.md)

**Tests**: Cross-platform Compose UI tests (`runComposeUiTest`) across `:composeApp:commonTest`, pure-JVM database tests in `:shared:desktopTest`, and platform-specific tests in `:app:androidTest` and `:desktopApp` (`SC-001`–`SC-006`).

**Organization**: Tasks are grouped by phase and user story (`US1` → `US3` → `US2` → `US4`), aligning with the 3 delivery milestones:
- **Milestone 1**: Phase 1 (Setup), Phase 2 (Foundational), Phase 3 (US1 - Runnable Starter Screen) 🎯 MVP
- **Milestone 2**: Phase 4 (US3 - Room DB Parity), Phase 5 (US2 - Core Note & Checklist Management)
- **Milestone 3**: Phase 6 (US4 - Full Route Parity, Mobile Decoupling & Cross-Platform UI Tests), Phase 7 (Polish)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`, `[US4]`)
- Exact file paths are included in every task description

## Path Conventions

- **`:shared` KMP Domain & Data Module**: `shared/src/commonMain/kotlin/`, `shared/src/desktopMain/kotlin/`, `shared/src/androidMain/kotlin/`, `shared/src/desktopTest/kotlin/`, `shared/schemas/`
- **`:composeApp` KMP Presentation & UI Module**: `composeApp/src/commonMain/kotlin/`, `composeApp/src/desktopMain/kotlin/`, `composeApp/src/androidMain/kotlin/`, `composeApp/src/commonTest/kotlin/`
- **`:desktopApp` Desktop Application Host Module**: `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/`, `desktopApp/src/desktopTest/kotlin/`
- **`:app` Android Application Host Module**: `app/src/main/java/`, `app/src/androidTest/` (platform-specific OS tests preserved)
- **`:build-logic` Gradle Convention Plugins**: `build-logic/convention/src/main/kotlin/`

---

## Phase 1: Setup (Gradle Catalog, Subproject Registration & Desktop Target Setup)

**Purpose**: Configure Compose Desktop dependencies in the version catalog, register `:desktopApp` in Gradle settings, update `:build-logic` convention plugins to support the desktop JVM target, and add `jvm("desktop")` target definitions to `:shared` and `:composeApp`.

- [X] T001 Register Compose Multiplatform Desktop plugins and library aliases in `gradle/libs.versions.toml`
- [X] T002 Include `:desktopApp` subproject alongside `:shared`, `:composeApp`, `:app`, and `:test-lib` in `settings.gradle.kts`
- [X] T003 [P] Update `NotallyxKotlinConventionPlugin` to support the `desktop` JVM target (JVM 17) and ktfmt configuration for desktop source sets in `build-logic/src/main/kotlin/com/philkes/notallyx/buildlogic/convention/NotallyxKotlinConventionPlugin.kt`
- [X] T004 [P] Add `jvm("desktop")` target and `desktopMain` source set definition to `shared/build.gradle.kts`
- [X] T005 [P] Add `jvm("desktop")` target and `desktopMain` source set definition to `composeApp/build.gradle.kts`
- [X] T006 Create `:desktopApp` subproject build configuration with `kotlin.multiplatform`, `application`, and Compose Desktop in `desktopApp/build.gradle.kts`

---

## Phase 2: Foundational (Desktop Platform Abstractions & DI Module)

**Purpose**: Implement desktop platform contracts and dependency injection wiring required by all desktop user stories.

**⚠️ CRITICAL**: Must complete before implementing any user story to provide non-Android implementations of platform contracts.

- [X] T007 [P] Implement `DesktopFileStorage` using standard JVM `java.io.File` and `java.nio.file.Path` in `shared/src/desktopMain/kotlin/com/philkes/notallyx/core/platform/DesktopFileStorage.kt`
- [X] T008 [P] Implement `DesktopLogger` printing to standard console and application logs in `shared/src/desktopMain/kotlin/com/philkes/notallyx/core/platform/DesktopLogger.kt`
- [X] T009 [P] Implement `DesktopAppInfo` reporting host OS name, OS version, and JVM runtime metadata in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/DesktopAppInfo.kt`
- [X] T010 [P] Implement `DesktopFilePicker` using desktop dialogs / file choosers in `composeApp/src/desktopMain/kotlin/com/philkes/notallyx/core/ui/platform/DesktopFilePicker.kt`
- [X] T011 [P] Implement `DesktopShareHandler` utilizing system clipboard or desktop mail client in `composeApp/src/desktopMain/kotlin/com/philkes/notallyx/core/ui/platform/DesktopShareHandler.kt`
- [X] T012 [P] Implement `DesktopPermissionChecker` returning Granted for all permission requests in `composeApp/src/desktopMain/kotlin/com/philkes/notallyx/core/ui/platform/DesktopPermissionChecker.kt`
- [X] T013 Configure desktop Koin dependency injection module wiring desktop platform implementations in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/di/DesktopModule.kt`

**Checkpoint**: Foundation ready — desktop platform bridges and DI are compiled and ready for User Story 1.

---

## Phase 3: User Story 1 - Desktop Application Launch and Baseline Screen (Milestone 1) (Priority: P1) 🎯 MVP

**Goal**: Deliver a runnable desktop application window running via Compose Multiplatform for Desktop with an interactive example starter screen verifying application bootstrap, diagnostics, input handling, and clean shutdown.

**Independent Test**: Execute `./gradlew :desktopApp:run` and verify that a desktop window titled "NotallyX" appears within 2.0 seconds, renders `DesktopExampleScreen`, responds to window resizing, increments a counter on interaction, and exits cleanly when closed.

### Implementation for User Story 1

- [X] T014 [P] [US1] Create `DesktopExampleScreen.kt` with header, diagnostic environment information from `DesktopAppInfo`, and an interactive counter button in `composeApp/src/desktopMain/kotlin/com/philkes/notallyx/core/ui/screens/DesktopExampleScreen.kt`
- [X] T015 [US1] Implement desktop entry point `main()` initializing Koin and launching `singleWindowApplication` with `NotallyXTheme` and `DesktopExampleScreen` in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/Main.kt`
- [X] T016 [US1] Add window lifecycle handlers (`onCloseRequest`, minimum window dimensions 400x300, window title "NotallyX") in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/Main.kt`
- [X] T017 [US1] Add desktop run configuration and build verification task in `desktopApp/build.gradle.kts` and verify `./gradlew :desktopApp:assemble` succeeds

**Checkpoint**: Milestone 1 (MVP) is fully functional and independently runnable via `./gradlew :desktopApp:run`.

---

## Phase 4: User Story 3 - Full Data Parity and Storage Compatibility (Priority: P2)

**Goal**: Enable Room 3 `NotallyDatabase` on desktop JVM with `BundledSQLiteDriver`, reusing identical entities, DAOs, converters, and migrations (v1–v11) to guarantee 100% binary compatibility with Android databases.

**Independent Test**: Run pure-JVM unit tests via `./gradlew :shared:testAndroidHostTest --tests "com.philkes.notallyx.core.database.NotallyDatabaseJvmTest"` and verify that database instances initialize, migrate, and ping successfully.

### Implementation for User Story 3

- [X] T018 [P] [US3] Implement `DesktopDatabaseDriverFactory` implementing `DatabaseDriverFactory` with `BundledSQLiteDriver` and `DesktopPathProvider` in `shared/src/desktopMain/kotlin/com/philkes/notallyx/core/database/DesktopDatabaseDriverFactory.kt`
- [X] T019 [US3] Decouple `NotallyDatabase` companion object and builder from Android `ContextWrapper` so `createDatabase(driverFactory)` is directly callable on pure desktop JVM in `shared/src/androidMain/kotlin/com/philkes/notallyx/core/database/NotallyDatabase.kt`
- [X] T020 [P] [US3] Add pure-JVM database test verifying in-memory and file-based CRUD, checkpointing, and v1–v11 migrations in `shared/src/desktopTest/kotlin/com/philkes/notallyx/core/database/DesktopDatabaseTest.kt`
- [X] T021 [US3] Wire `DesktopDatabaseDriverFactory` into the desktop DI graph in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/di/DesktopModule.kt`

**Checkpoint**: Database parity is complete and verified — desktop reads and writes identical SQLite databases as Android.

---

## Phase 5: User Story 2 - Primary Note and Checklist Management (Milestone 2) (Priority: P2)

**Goal**: Deliver core note management on desktop: Notes Overview, Text Note Editor, and Checklist Note Editor backed by the shared Room database, with automatic persistence and responsive desktop layout.

**Independent Test**: Launch `./gradlew :desktopApp:run`, create and edit text notes and checklists, verify changes appear in overview and persist across application restarts.

### Implementation for User Story 2

- [X] T022 [P] [US2] Move shared note card components (`BaseNoteCard.kt`, `LabelChip.kt`, `ReminderChip.kt`, `ColorSwatchItem.kt`) from `composeApp/src/androidMain/` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/components/`
- [X] T023 [P] [US2] Move shared note editing dialogs (`ConfirmDialog.kt`, `ErrorDialog.kt`, `TextInputDialog.kt`, `ColorPickerDialog.kt`) from `composeApp/src/androidMain/` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/dialog/`
- [X] T024 [P] [US2] Move `NotesModel.kt`, `EditNoteModel.kt`, and `EditListModel.kt` from `composeApp/src/androidMain/` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/` and `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/notes/`
- [X] T025 [P] [US2] Move `NotesScreen.kt`, `EditScreen.kt`, and `ChecklistItemRow.kt` from `composeApp/src/androidMain/` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/notes/` and `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/screens/`
- [X] T026 [US2] Create desktop navigation coordinator hosting `NotesRoute`, `EditNoteRoute`, and `EditListRoute` in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/DesktopAppNavHost.kt`
- [X] T027 [US2] Update `Main.kt` in `:desktopApp` to switch from Milestone 1 example screen to `DesktopAppNavHost` in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/Main.kt`
- [X] T028 [US2] Add automatic WAL checkpointing on desktop window close (`onCloseRequest`) via `NotallyDatabase.checkpoint()` in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/Main.kt`

**Checkpoint**: Milestone 2 is complete — desktop users can view, create, edit, check, and persist notes and checklists locally.

---

## Phase 6: User Story 4 - Comprehensive Route Parity, Mobile-Only Feature Exclusion & Cross-Platform UI Tests (Milestone 3) (Priority: P3)

**Goal**: Port remaining shared routes (Search, Labels, Archive, Trash, Settings, Import/Export) to desktop, while cleanly isolating and omitting mobile-only hardware subsystems (biometrics, mobile notifications, home screen widgets). Establish cross-platform Compose UI testing in `composeApp/src/commonTest` using `runComposeUiTest` so that Compose UI tests (notes overview, note editor, checklist editor, search, labels, archive, trash, and settings) run universally on both Desktop JVM and Android, retaining only platform-specific tests in `app/src/androidTest` and `desktopApp/src/desktopTest`.

**Independent Test**: Navigate through all secondary shared routes on desktop, confirming search queries filter notes accurately, labels can be created and assigned, and no mobile-specific prompts or errors appear. Execute `./gradlew :composeApp:desktopTest` and verify all common Compose UI tests pass on Desktop JVM without an Android runtime or emulator.

### Implementation for User Story 4

- [X] T029 [P] [US4] Move `SearchModel.kt`, `SearchScreen.kt`, `SearchViewModel.kt`, and `SearchRoute.kt` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/search/`
- [X] T030 [P] [US4] Move `LabelsModel.kt`, `LabelsScreen.kt`, `LabelsViewModel.kt`, and `LabelsRoute.kt` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/labels/`
- [X] T031 [P] [US4] Move `ArchivedModel.kt`, `ArchivedScreen.kt`, `DeletedModel.kt`, and `DeletedScreen.kt` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/archived/` and `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/deleted/`
- [X] T032 [P] [US4] Move shared settings UI components and import/export flows to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/settings/`
- [X] T033 [US4] Conditionally hide mobile-specific preference categories (Biometric Lock, Home Screen Widgets, Status Bar Pinned Notifications) when running on desktop in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/settings/SettingsRoute.kt`
- [X] T034 [US4] Register all remaining shared routes in `DesktopAppNavHost.kt` in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/DesktopAppNavHost.kt`
- [X] T035 [P] [US4] Configure Compose Multiplatform UI testing dependencies (`androidx.compose.ui:ui-test`) and test resources in `composeApp/build.gradle.kts` (`commonTest`)
- [X] T036 [P] [US4] Implement `CommonComposeUiTestBase` in `composeApp/src/commonTest/kotlin/com/philkes/notallyx/test/CommonComposeUiTestBase.kt` providing in-memory `NotallyDatabase` (via `BundledSQLiteDriver`), test Koin context, and `runComposeUiTest` composition harness without Android `ActivityScenario` or `Context`
- [X] T037 [US4] Migrate core note & checklist editor Compose UI tests from `app/src/androidTest/kotlin/com/philkes/notallyx/activity/edit/EditRouteTest.kt` and `EditListRouteTest.kt` to `composeApp/src/commonTest/kotlin/com/philkes/notallyx/feature/editor/EditRouteCommonTest.kt` and `EditListRouteCommonTest.kt`, replacing `ActivityScenario.launch()` and `context.getString()` with `runComposeUiTest` and platform-agnostic `TestTags`
- [X] T038 [US4] Migrate Notes overview and multi-selection Compose UI tests from `app/src/androidTest/kotlin/com/philkes/notallyx/activity/main/MainActivityTest.kt` to `composeApp/src/commonTest/kotlin/com/philkes/notallyx/feature/main/NotesRouteCommonTest.kt` using `CommonComposeUiTestBase`
- [X] T039 [US4] Migrate secondary route Compose UI tests (`LabelsRouteTest.kt`, `SearchRouteTest.kt`, `ArchivedRouteTest.kt`, `DeletedRouteTest.kt`, and core `SettingsRouteTest.kt`) from `app/src/androidTest/` to `composeApp/src/commonTest/` alongside their corresponding route migrations
- [X] T040 [P] [US4] Audit and retain platform-specific Android tests in `app/src/androidTest/` (specifically `LockedActivityTest`, `RemindersActivityTest`, `WidgetTest`, `ErrorActivityTest`, `DatabaseTransitionActivityTest`, `EditTextPlainActivityTest`, and SQLCipher decryption tests) to assert Android OS integrations (Biometrics, AlarmManager, Glance AppWidgets, Intents, and Notification channels)

**Checkpoint**: Milestone 3 is complete — desktop target reaches full route parity with mobile-only capabilities cleanly decoupled, and shared Compose UI tests run uniformly across Desktop and Android.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Static audits, code formatting, multiplatform test suite execution, end-to-end quickstart validation, and documentation updates.

- [X] T041 [P] Run static audit ensuring zero `android.*` imports in `desktopApp/`, `desktopMain/`, and `composeApp/src/commonTest`
- [X] T042 Run `./gradlew ktfmtFormat` and apply formatting via `./gradlew ktfmtFormat` across all subprojects
- [X] T043 Run cross-platform Compose UI test suites across targets via `./gradlew :composeApp:desktopTest` and `./gradlew :composeApp:testAndroidHostTest`
- [X] T044 Execute automated quickstart verification scenarios from `specs/004-kmp-desktop-target/quickstart.md`
- [X] T045 [P] Update architecture, testing guide, and desktop developer setup documentation in `documentation/docs/architecture/kmp-desktop.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion — BLOCKS all user story phases.
- **User Story 1 (Phase 3 - Milestone 1)**: Depends on Phase 2 completion — delivers runnable desktop MVP.
- **User Story 3 (Phase 4 - Data Parity)**: Depends on Phase 2 completion — can proceed after US1 to establish desktop database persistence.
- **User Story 2 (Phase 5 - Milestone 2)**: Depends on Phase 3 (US1 window) and Phase 4 (US3 database persistence).
- **User Story 4 & Common UI Tests (Phase 6 - Milestone 3)**: Depends on Phase 5 (US2 navigation host & core routes).
- **Polish (Phase 7)**: Depends on completion of all desired user stories.

```mermaid
graph TD
    Phase1["Phase 1: Setup"] --> Phase2["Phase 2: Foundational"]
    Phase2 --> Phase3["Phase 3: US1 (Milestone 1 MVP)"]
    Phase2 --> Phase4["Phase 4: US3 (DB Parity)"]
    Phase3 --> Phase5["Phase 5: US2 (Milestone 2 Notes/Checklists)"]
    Phase4 --> Phase5
    Phase5 --> Phase6["Phase 6: US4 & Common UI Tests (Milestone 3)"]
    Phase6 --> Phase7["Phase 7: Polish & Validation"]
```

---

## Parallel Opportunities

- **Phase 1 (Setup)**: Tasks `T003`, `T004`, `T005` can run in parallel once `T001` and `T002` are complete.
- **Phase 2 (Foundational)**: Platform implementation tasks `T007`, `T008`, `T009`, `T010`, `T011`, `T012` can all run in parallel.
- **Phase 3 (User Story 1)**: Starter screen creation (`T014`) can run in parallel with window configuration (`T016`).
- **Phase 4 (User Story 3)**: `DesktopDatabaseDriverFactory` (`T018`) and `DesktopDatabaseTest` (`T020`) can run in parallel.
- **Phase 5 (User Story 2)**: Component migration tasks `T022`, `T023`, `T024`, `T025` can all run in parallel.
- **Phase 6 (User Story 4 & Common UI Tests)**:
  - Route migrations `T029`, `T030`, `T031`, `T032` can run in parallel.
  - Test infrastructure `T035`, `T036` can run in parallel with route migrations.
  - Test migrations `T037`, `T038`, `T039` can run in parallel once `T036` and routes are ready.
  - Platform-specific test retention audit `T040` can run in parallel with test migrations.
- **Phase 7 (Polish)**: `T041`, `T042`, and `T045` can run in parallel.

---

## Implementation Strategy

### MVP First (Milestone 1: User Story 1 Only)

1. Complete **Phase 1: Setup** (configure catalog, subproject, and convention plugins).
2. Complete **Phase 2: Foundational** (desktop platform implementations & DI).
3. Complete **Phase 3: User Story 1** (interactive starter screen in `:desktopApp`).
4. **STOP and VALIDATE**: Run `./gradlew :desktopApp:run` to verify desktop window creation, rendering, and interaction.
5. This delivers the first requested milestone: a plain example, runnable desktop target with an example screen!

### Incremental Milestone Progression

1. **Milestone 1 (MVP)**: Phase 1 + Phase 2 + Phase 3 → Runnable desktop target with example screen.
2. **Milestone 2**: Phase 4 (US3 Room DB) + Phase 5 (US2 Core Notes/Checklists) → Fully functional note-taking app on desktop with shared SQLite database.
3. **Milestone 3**: Phase 6 (US4 Route Parity & Cross-Platform UI Tests) + Phase 7 (Polish) → Complete route parity across Search, Labels, Archive, Trash, and Settings with mobile-only features cleanly decoupled, backed by cross-platform Compose UI tests running uniformly on Desktop and Android.

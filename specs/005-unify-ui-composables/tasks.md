---
description: "Task list for unified cross-platform UI composables implementation"
---

# Tasks: Unified Cross-Platform UI Composables (KMP)

**Input**: Design documents from `specs/005-unify-ui-composables/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/ui-contracts.md`, `quickstart.md`)  
**Prerequisites**: Kotlin 2.4.20, Compose Multiplatform 1.12.2, Room 3.0.3, Koin 4.x

## Format: `- [ ] [ID] [P?] [Story?] Description with file path`
- **[P]**: Parallelizable (independent files, no blocking prerequisites)
- **[Story]**: User story identifier (`[US1]`, `[US2]`, `[US3]`, `[US4]`, `[US5]`)
- Clear description including exact file paths

---

## Phase 1: Setup (Shared Multiplatform Infrastructure)

**Purpose**: Configure Compose Multiplatform Resources and common package layout in `composeApp`

- [X] T001 Configure Compose Multiplatform Resources plugin and dependencies in `composeApp/build.gradle.kts`
- [X] T002 [P] Establish shared string resource files under `composeApp/src/commonMain/composeResources/values/strings.xml`
- [X] T003 [P] Establish shared vector drawables under `composeApp/src/commonMain/composeResources/drawable/`
- [X] T004 [P] Configure multiplatform resource generation and packaging in `composeApp/build.gradle.kts`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core UI foundation, base theme, chips, and navigation contracts in `commonMain` that all screens depend upon

- [X] T005 [P] Unify shared theme tokens and typography in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/theme/NotallyXTheme.kt`
- [X] T006 [P] Verify base component chips (`LabelChip`, `ReminderChip`, `ImageCountChip`) in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/components/NoteChips.kt`
- [X] T007 [P] Consolidate shared note card composable `BaseNoteCard` in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt`
- [X] T008 [P] Consolidate shared confirmation dialog `ConfirmDialog` in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/dialog/ConfirmDialog.kt`
- [X] T009 Unify Navigation 3 keys and navigation state contracts in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/navigation/NotallyNavKey.kt`

**Checkpoint**: Shared theme, chips, cards, and navigation infrastructure ready. User story implementation can now begin.

---

## Phase 3: User Story 1 - Consistent Note and Checklist Editing (Priority: P1) 🎯 MVP

**Goal**: Both Android and Desktop share the exact same note and checklist editor composables, eliminating `CommonEditNoteScreen` and `CommonEditListScreen`.

**Independent Test**: Open any text note or checklist note on Desktop and Android; verify identical rendering of title, body, formatting bar, checklist item rows, check/uncheck animations, and color changes.

### Implementation for User Story 1

- [X] T010 [P] [US1] Migrate stateless `EditNoteScreen` composable to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/screens/EditNoteScreen.kt`
- [X] T011 [P] [US1] Migrate stateless `EditListScreen` composable to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/screens/EditListScreen.kt`
- [X] T012 [P] [US1] Migrate stateless `EditTextPlainScreen` composable to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/screens/EditTextPlainScreen.kt`
- [X] T013 [P] [US1] Migrate shared editor dialogs to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/screens/EditorDialogs.kt`
- [X] T014 [US1] Wire shared `EditNoteRoute` coordinator in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/EditNoteRoute.kt`
- [X] T015 [US1] Wire shared `EditListRoute` coordinator in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/EditListRoute.kt`
- [X] T016 [US1] Remove deprecated stub `CommonEditNoteScreen.kt` from `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/CommonEditNoteScreen.kt`
- [X] T017 [US1] Remove deprecated stub `CommonEditListScreen.kt` from `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/CommonEditListScreen.kt`
- [X] T018 [US1] Update desktop navigation in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/DesktopAppNavHost.kt` to invoke shared `EditNoteScreen` and `EditListScreen`
- [X] T019 [P] [US1] Add editor composable tests in `composeApp/src/commonTest/kotlin/com/philkes/notallyx/feature/editor/EditNoteScreenTest.kt`

**Checkpoint**: User Story 1 MVP complete. Android and Desktop now share identical note and checklist editing screens.

---

## Phase 4: User Story 2 - Shared Note Overview and Organization Views (Priority: P2)

**Goal**: Overview grids, search, label manager, and folder screens share the exact same UI composables across Android and Desktop.

**Independent Test**: Navigate to Notes grid, Search, Labels, Archive, and Trash on Desktop and Android; confirm note cards, search query highlights, and label dialogs display identically.

### Implementation for User Story 2

- [X] T020 [P] [US2] Migrate stateless `NotesGridScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/NotesGridScreen.kt`
- [X] T021 [P] [US2] Migrate stateless `SearchScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/search/SearchScreen.kt`
- [X] T022 [P] [US2] Migrate stateless `LabelsScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/labels/LabelsScreen.kt`
- [X] T023 [P] [US2] Migrate stateless `SelectLabelsScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/labels/SelectLabelsScreen.kt`
- [X] T024 [P] [US2] Migrate folder screens (`ArchivedScreen`, `DeletedScreen`) to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/`
- [X] T025 [US2] Wire shared route coordinators for Overview, Search, and Folders in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/`
- [X] T026 [US2] Delete deprecated stubs `CommonNotesScreen.kt`, `CommonSearchScreen.kt`, `CommonLabelsScreen.kt`, `CommonArchivedScreen.kt`, `CommonDeletedScreen.kt` from `composeApp/src/commonMain/`
- [X] T027 [US2] Update desktop navigation in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/DesktopAppNavHost.kt` to use shared `NotesGridScreen`, `SearchScreen`, and `LabelsScreen`
- [X] T028 [P] [US2] Add overview and search composable tests in `composeApp/src/commonTest/kotlin/com/philkes/notallyx/feature/main/NotesGridScreenTest.kt`

**Checkpoint**: User Story 2 complete. Notes overview, search, folders, and labels screens are unified.

---

## Phase 5: User Story 3 - Shared Settings and Reminders Management (Priority: P2)

**Goal**: Unified Settings and Reminders screens, cleanly accommodating platform differences via state capability flags.

**Independent Test**: Open Settings and Reminders on both platforms; verify preferences rows, switches, theme pickers, and reminder cards match visually while platform-specific entries (biometrics) adapt based on capability flags.

### Implementation for User Story 3

- [X] T029 [P] [US3] Add capability flags (`isBiometricsAvailable`, `isNotificationPermissionRequired`, `isBatteryOptimizationAvailable`) to `SettingsUiState` in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/settings/SettingsModel.kt`
- [X] T030 [P] [US3] Add capability flags (`canScheduleExactAlarms`, `isNotificationPermissionGranted`) to `RemindersUiState` in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/reminder/RemindersModel.kt`
- [X] T031 [US3] Migrate stateless `SettingsScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/settings/SettingsScreen.kt`, rendering platform-specific rows conditionally on capability flags
- [X] T032 [P] [US3] Migrate stateless `RemindersScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/reminder/RemindersScreen.kt`
- [X] T033 [P] [US3] Migrate stateless `NoteRemindersScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/reminder/NoteRemindersScreen.kt`
- [X] T034 [US3] Wire shared `SettingsRoute` and `RemindersRoute` in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/`
- [X] T035 [US3] Delete deprecated stub `CommonSettingsScreen.kt` from `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/routes/settings/CommonSettingsScreen.kt`
- [X] T036 [US3] Update desktop navigation in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/DesktopAppNavHost.kt` to use shared `SettingsScreen` and `RemindersScreen`
- [X] T037 [P] [US3] Add Settings capability flag tests in `composeApp/src/commonTest/kotlin/com/philkes/notallyx/feature/settings/SettingsScreenTest.kt`

**Checkpoint**: User Story 3 complete. Settings and Reminders screens unified across platforms.

---

## Phase 6: User Story 4 - Shared Dialogs, Modals, Action Sheets, and Media (Priority: P3)

**Goal**: Unify secondary dialogs (color picker, sort options) and media viewer/playback screens across platforms.

**Independent Test**: Open color picker dialogs, deletion confirmations, audio player, image viewer, and pick note screen on Android and Desktop; verify identical presentation.

### Implementation for User Story 4

- [X] T038 [P] [US4] Consolidate `ColorPickerDialog` and note color palette in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/core/ui/dialog/color/ColorPickerDialog.kt`
- [X] T039 [P] [US4] Migrate stateless `PlayAudioScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/media/audio/PlayAudioScreen.kt`
- [X] T040 [P] [US4] Migrate stateless `RecordAudioScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/media/audio/RecordAudioScreen.kt`
- [X] T041 [P] [US4] Migrate stateless `ViewImageScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/media/image/ViewImageScreen.kt`
- [X] T042 [P] [US4] Migrate stateless `PickNoteScreen` to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/main/PickNoteScreen.kt`
- [X] T043 [P] [US4] Add media and dialog composable tests in `composeApp/src/commonTest/kotlin/com/philkes/notallyx/feature/media/PlayAudioScreenTest.kt`

**Checkpoint**: User Story 4 complete. Secondary dialogs, picker modals, and media screens unified.

---

## Phase 7: User Story 5 - Graceful Form Factor & Platform Adaptation (Priority: P4)

**Goal**: Unify desktop navigation with Navigation 3 `ListDetailScene` adaptive two-pane layouts and keyboard shortcuts.

**Independent Test**: Run desktop app on wide screen; verify notes list and note editor display side-by-side in adaptive two-pane layout. Verify desktop keyboard shortcuts (`Ctrl+B`, `Ctrl+I`, `Ctrl+S`).

### Implementation for User Story 5

- [X] T044 [P] [US5] Implement desktop keyboard shortcut handlers (`Ctrl+B`, `Ctrl+I`, `Ctrl+U`, `Ctrl+S`) in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/editor/screens/EditNoteScreen.kt`
- [X] T045 [US5] Wire shared Navigation 3 `ListDetailScene` adaptive two-pane container in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/navigation/ListDetailScene.kt`
- [X] T046 [US5] Update `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/Main.kt` to host shared `NotallyApp` / Navigation 3 container
- [X] T047 [US5] Deprecate and remove legacy `DesktopAppNavHost.kt` in `desktopApp/src/desktopMain/kotlin/com/philkes/notallyx/desktop/DesktopAppNavHost.kt`
- [X] T048 [P] [US5] Add adaptive two-pane navigation test in `composeApp/src/commonTest/kotlin/com/philkes/notallyx/navigation/ListDetailSceneTest.kt`

**Checkpoint**: User Story 5 complete. Full platform adaptation, adaptive split-screen, and keyboard shortcuts operational.

---

## Phase 8: Polish & Cross-Cutting Verification

**Purpose**: Format verification, multiplatform compilation, regression testing, and cleanup

- [X] T049 Remove all obsolete Android-only screen references and imports in `composeApp/src/androidMain/`
- [X] T050 [P] Run code formatting across the repository: `./gradlew ktfmtFormat`
- [X] T051 Verify code formatting gate: `./gradlew ktfmtCheck`
- [X] T052 Verify Android application compilation: `./gradlew :app:assembleDebug`
- [X] T053 Verify Desktop application assembly: `./gradlew :desktopApp:desktopJar`
- [X] T054 Run unit and presentation tests: `./gradlew :composeApp:testAndroidHostTest`
- [X] T055 Run visual regression screenshot tests: `./gradlew verifyRoborazziDebug` and `./gradlew validateDebugScreenshotTest`
- [X] T056 Execute manual quickstart verification scenarios per `specs/005-unify-ui-composables/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies
- **Setup (Phase 1)**: Can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1; blocks all User Stories.
- **User Stories (Phase 3 to 7)**: Depend on Phase 2.
  - Phase 3 (US1 - MVP): Primary deliverable; can proceed first.
  - Phase 4 (US2): Can run in parallel with US3 or sequentially after US1.
  - Phase 5 (US3): Can run in parallel with US2 or sequentially after US2.
  - Phase 6 (US4): Depends on US1 (editor dialogs) and US2.
  - Phase 7 (US5): Integrates US1 and US2 into the shared Navigation 3 `ListDetailScene`.
- **Polish (Phase 8)**: Depends on all user stories completing.

### Parallel Opportunities
- In Phase 1: T002, T003, T004 can run in parallel.
- In Phase 2: T005, T006, T007, T008 can run in parallel.
- In Phase 3 (US1): Screen migrations T010, T011, T012, T013 can proceed in parallel.
- In Phase 4 (US2): Screen migrations T020, T021, T022, T023, T024 can proceed in parallel.
- In Phase 5 (US3): T029, T030, T032, T033 can run in parallel.
- In Phase 6 (US4): T038, T039, T040, T041, T042 can run in parallel.
- In Phase 8: T050, T051, T052, T053, T054, T055 run sequentially to validate gates.

---

## Implementation Strategy

### MVP Milestone (Phase 1 + Phase 2 + Phase 3)
1. Complete Setup and Foundational phases.
2. Complete Phase 3 (User Story 1: Note & Checklist Editor unification).
3. Validate independently on Desktop (`./gradlew :desktopApp:run`) and Android (`./gradlew :app:installDebug`).
4. At this point, the primary core feature of NotallyX (note editing) is 100% unified with zero divergent stubs!

### Incremental Full Parity (Phases 4 through 7)
- Add User Story 2 (Notes overview, search, folders, labels).
- Add User Story 3 (Settings & Reminders with capability flags).
- Add User Story 4 (Secondary dialogs & media viewers).
- Add User Story 5 (Navigation 3 adaptive two-pane split layout & desktop shortcuts).
- Complete Polish & verification gates.

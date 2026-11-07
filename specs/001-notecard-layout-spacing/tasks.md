# Tasks: Notes Card Layout and Spacing Redesign

**Input**: Design documents from `specs/001-notecard-layout-spacing/` (`spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/notecard-contracts.md`, `quickstart.md`)  
**Feature Branch**: `001-notecard-layout-spacing`  
**Prerequisites**: `spec.md`, `plan.md`, `contracts/notecard-contracts.md`  

## Format: `- [ ] [ID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: User story identifier (`[US1]`, `[US2]`, `[US3]`) for story phase tasks
- File paths are explicitly specified for every task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Baseline verification and test environment preparation

- [X] T001 Inspect current Compose previews and verify baseline tests in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure and component parameters that MUST be complete before user story changes

- [X] T002 Add isGrid parameter with default false to BaseNoteCard in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T003 Pass isGrid flag to BaseNoteCard calls in LazyVerticalStaggeredGrid and LazyColumn in app/src/main/java/com/philkes/notallyx/feature/main/routes/NotesGridScreen.kt

**Checkpoint**: Foundation ready — BaseNoteCard can distinguish List and Grid view contexts without breaking existing callers

---

## Phase 3: User Story 1 - Clean Content-First Reading Experience (Priority: P1) 🎯 MVP

**Goal**: Note title and content (body text or checklist items) are positioned immediately adjacent to each other without intermediate dates or attachments interrupting reading flow.

**Independent Test**: Render notes with titles, dates, files, and body/checklist in Android Studio Previews (`LoremIpsumNotePreview`, `ListNotePreview`). Confirm that body text or checklist begins immediately below the title with a 4dp gap.

### Implementation for User Story 1

- [X] T004 [US1] Remove intermediate Date text and File attachment chips from between Title and Body/Checklist in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T005 [US1] Establish 4dp Title-to-Body vertical spacing and set title layout weight(1f, fill = false) in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T006 [US1] Ensure checklist items render directly under title with 2dp vertical item padding in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T007 [US1] Verify User Story 1 reading flow in LoremIpsumNotePreview and ListNotePreview in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt

**Checkpoint**: User Story 1 functional and verifiable independently. Title and content flow continuously.

---

## Phase 4: User Story 2 - Consistent Metadata Footnote Layout (Priority: P2)

**Goal**: Consolidate secondary metadata (compact attachment indicator, label chips, and sort date) into a dedicated bottom footnote container, eliminating card height bloat and uneven gaps.

**Independent Test**: Render notes with varying metadata combinations (files only, labels only, date only, all metadata present, no metadata). Confirm footnote layout renders at the bottom with 8dp top separation, or collapses completely with 0dp height when no metadata exists.

### Implementation for User Story 2

- [X] T008 [P] [US2] Implement AttachmentSummaryBadge composable rendering paperclip icon and attachment count in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T009 [P] [US2] Streamline LabelChip inner padding to horizontal 8dp and vertical 2dp in app/src/main/java/com/philkes/notallyx/core/ui/components/LabelChip.kt
- [X] T010 [US2] Implement NoteFootnoteRow consolidating AttachmentSummaryBadge, LabelChips, and sort timestamp in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T011 [US2] Ensure zero empty-state height consumption when note has no attachments, labels, or active timestamp in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T012 [US2] Verify footnote layout with RichTextNotePreview and NoteWithEverythingMinimalDisplayPreview in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt

**Checkpoint**: User Stories 1 and 2 functional. Notes have clean reading hierarchy and unified bottom metadata footnote.

---

## Phase 5: User Story 3 - Adaptive Reminder Presentation for List and Grid Modes (Priority: P3)

**Goal**: Adapt reminder affordance between List View (full alarm schedule chip) and Grid View (compact icon badge), preventing multiline header sprawl in narrow grid columns.

**Independent Test**: Render notes with active reminders in List and Grid view configurations. Verify Grid cards display a compact alarm icon badge at the top-right of the title, while List cards display the scheduled timestamp chip, and tapping either triggers the reminder callback.

### Implementation for User Story 3

- [X] T013 [US3] Implement compact reminder icon affordance for isGrid true anchored at header top-right in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T014 [US3] Implement inline or wrapping ReminderChip affordance for isGrid false in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T015 [US3] Wire onReminderClick callback handling for both grid icon and list chip in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T016 [US3] Calibrate internal content padding to 12dp for list and 10dp for grid in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt
- [X] T017 [US3] Validate reminder affordances across themes and view modes in app/src/screenshotTestDebug/kotlin/com/philkes/notallyx/reminder/RemindersPreviewTest.kt

**Checkpoint**: All three user stories functional and integrated.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Screenshot regression testing, code style verification, and validation guide execution

- [X] T018 [P] Update NoteCardDisplayConfigScreenshotTest previews for grid and list layout variations in app/src/screenshotTestDebug/kotlin/com/philkes/notallyx/card/NoteCardDisplayConfigScreenshotTest.kt
- [X] T019 Run automated screenshot regression verification via gradle verifyRoborazziDebug in app/
- [X] T020 Run unit and Robolectric tests via gradle testDebugUnitTest in app/
- [X] T021 Verify code formatting compliance via gradle ktfmtFormat across all modified files
- [X] T022 Execute full quickstart validation scenarios per specs/001-notecard-layout-spacing/quickstart.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories.
- **User Story 1 (Phase 3 - P1)**: Depends on Phase 2 completion. Delivers the core reading MVP.
- **User Story 2 (Phase 4 - P2)**: Depends on Phase 2 and integrates with the bottom of the card below US1 content.
- **User Story 3 (Phase 5 - P3)**: Depends on Phase 2 (uses `isGrid` parameter introduced in foundational phase).
- **Polish (Phase 6)**: Depends on all user stories being complete.

### Parallel Opportunities

- **Phase 4 (US2)**: `T008` (implementing `AttachmentSummaryBadge` in `BaseNoteCard.kt`) and `T009` (modifying `LabelChip.kt`) can proceed in parallel as they touch different files.
- **Phase 6 (Polish)**: `T018` preview updates can be prepared in parallel with test verification scripts.

---

## Parallel Example: User Story 2

```bash
# Developer / Subagent A:
Task: "T008 [P] [US2] Implement AttachmentSummaryBadge composable in app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt"

# Developer / Subagent B:
Task: "T009 [P] [US2] Streamline LabelChip inner padding in app/src/main/java/com/philkes/notallyx/core/ui/components/LabelChip.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1 (Setup) and Phase 2 (Foundational: `isGrid` parameter wiring).
2. Complete Phase 3 (User Story 1: remove intervening date/files, connect title directly to body).
3. **STOP and VALIDATE**: Inspect `LoremIpsumNotePreview` and `ListNotePreview`. Note reading flow is immediately fixed.

### Incremental Delivery
1. Add User Story 2: Move metadata (compact attachments, labels, sort date) into the footnote row. Verify `RichTextNotePreview`.
2. Add User Story 3: Provide compact reminder icon in Grid view and `10.dp` grid padding.
3. Run Polish Phase: Execute `./gradlew ktfmtFormat`, `./gradlew testDebugUnitTest`, and screenshot tests.

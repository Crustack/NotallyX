# Implementation Plan: Notes Card Layout and Spacing Redesign

**Branch**: `001-notecard-layout-spacing` | **Date**: 2026-10-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-notecard-layout-spacing/spec.md` ([GitHub Issue #918](https://github.com/Crustack/NotallyX/issues/918))

## Summary

Redesign the layout, spacing, and element hierarchy of `BaseNoteCard` in NotallyX. Reorganize the card from an irregular layout where dates and full file attachment chips sit between title and body, into an intuitive, content-first visual hierarchy:
- Header: Title text with end-aligned Reminder affordance (full chip in List view, compact icon/badge in Grid view).
- Primary Content: Body text or Checklist items immediately following the title.
- Metadata Footnote: Consolidated footer containing a compact attachment indicator (`[📎 N]`), streamlined label chips, and sort timestamp.
- Spacing Tokens: Uniform 12dp internal padding for List view, 10dp for Grid view, 4dp title-to-body gap, and 8dp content-to-footnote gap.

## Technical Context

**Language/Version**: Kotlin 2.4.20 / JVM 17  
**Primary Dependencies**: Jetpack Compose (1.8+ / Material 3 1.3+), Coil 3 (image loading), Koin 4.x  
**Storage**: N/A (pure UI layout and component spacing; no Room entity or DataStore schema modifications)  
**Testing**: Roborazzi screenshot verification (`./gradlew verifyRoborazziDebug`), AGP screenshot tests (`./gradlew validateDebugScreenshotTest`), Robolectric/JUnit 4 unit tests (`./gradlew testDebugUnitTest`)  
**Target Platform**: Android (Min SDK 24, Compile/Target SDK 37)  
**Project Type**: Android Mobile Application  
**Performance Goals**: 60/120 FPS fluid scrolling in `LazyVerticalStaggeredGrid` and `LazyColumn`; zero unnecessary recomposition overhead  
**Constraints**: Backward-compatible parameters for `BaseNoteCard`, preserve interactive callbacks (`onClick`, `onLongClick`, `onReminderClick`), conform to project `ktfmt` style (4-space indent)  
**Scale/Scope**: Primary updates to `BaseNoteCard.kt`, `LabelChip.kt`, `NotesGridScreen.kt`, and screenshot test previews in `app/src/screenshotTestDebug/`  

## Constitution Check

*GATE: Passed before Phase 0 research. Re-checked post Phase 1 design.*

- **Principle I: Compose-First UI & Zero XML**: PASS. All components implemented purely in Jetpack Compose and Material 3 with proper state hoisting and `@Preview` coverage. Zero XML layouts.
- **Principle II: Feature-First MVI & The 4-File Feature Quad**: PASS. Changes reside within the shared presentation component layer (`core/ui/components/BaseNoteCard.kt`) and are consumed by MVI feature routes (`NotesGridScreen`, `PickNoteScreen`).
- **Principle III: Data Integrity & Non-Destructive Storage**: PASS. No database schema or entity changes.
- **Principle IV: Privacy-First & Offline-First Security**: PASS. Offline-first UI components with zero tracking or external network requests.
- **Principle V: Strict Thread & Concurrency Discipline**: PASS. All layout rendering executed within Compose UI runtime on main dispatcher; image loading asynchronously handled by Coil.
- **Principle VI: Comprehensive Multi-Layer Testing**: PASS. Visual layout fidelity guarded by Roborazzi screenshot tests and Compose `@Preview` tests in `screenshotTestDebug`.
- **Principle VII: Code Health, Formatting & Idiomatic Kotlin**: PASS. Code strictly conforms to `ktfmt` 4-space indentation and clean import standards.

## Project Structure

### Documentation (this feature)

```text
specs/001-notecard-layout-spacing/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Technical research & decisions (Phase 0)
├── data-model.md        # Layout model & spacing tokens (Phase 1)
├── quickstart.md        # Developer & tester validation guide (Phase 1)
├── contracts/           # Component signatures & behavior contracts (Phase 1)
│   └── notecard-contracts.md
└── checklists/
    └── requirements.md  # Quality checklist (/speckit-specify output)
```

### Source Code (affected modules)

```text
app/src/main/java/com/philkes/notallyx/
├── core/ui/components/
│   ├── BaseNoteCard.kt          # Primary card layout, hierarchy, and spacing redesign
│   ├── LabelChip.kt             # Streamlined chip padding and subtle border styling
│   └── ReminderChip.kt          # Adaptive reminder chip and compact icon affordance
└── feature/main/routes/
    └── NotesGridScreen.kt       # Propagating isGrid flag to BaseNoteCard in grid vs list lanes

app/src/screenshotTestDebug/kotlin/com/philkes/notallyx/
├── card/
│   └── NoteCardDisplayConfigScreenshotTest.kt # Previews for BaseNoteCard layout variations
└── theme/
    └── ThemePreviewTest.kt                    # Grid and list view theme screenshot previews
```

**Structure Decision**: Android Mobile Application layout. Changes are isolated to `core/ui/components/` and consumed via standard Compose components by `feature/main/routes/NotesGridScreen.kt`.

## Complexity Tracking

> No violations of the NotallyX Constitution detected. Zero architectural complexity exceptions required.

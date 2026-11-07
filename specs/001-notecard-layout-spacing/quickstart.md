# Quickstart Validation Guide: Notes Card Layout and Spacing Redesign

**Feature**: Notes List/Grid - Layout and Spacing Redesign ([Issue #918](https://github.com/Crustack/NotallyX/issues/918))  
**Date**: 2026-10-31  
**Status**: Ready for Implementation  

---

## 1. Overview
This guide provides developers and testers with concrete, runnable commands and visual scenarios to validate the implementation of the `BaseNoteCard` layout and spacing redesign.

---

## 2. Validation Scenarios

### Scenario A: Visual Regression & Layout Inspection in Android Studio Previews
Open [`BaseNoteCard.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt) in Android Studio and inspect the interactive `@Preview` composables:
1. **`LoremIpsumNotePreview`**: Verify title is immediately followed by body text; footnote displays "Label1".
2. **`ListNotePreview`**: Verify checklist items follow title immediately with 4dp gap; no intermediate whitespace.
3. **`RichTextNotePreview`**:
   - Inspect note with all elements: Image, Title, Reminder, Body, Attachments, Labels, Date.
   - Verify image is at top.
   - Verify title has reminder at top-right.
   - Verify body follows title immediately.
   - Verify footnote contains `[📎 2]`, 5 label chips, and sort timestamp at the bottom.
4. **`DarkListNotePreview` & `LoremIpsumNoteSelectedPreview`**: Verify selection border (`3.dp` stroke) and contrast colors.

### Scenario B: Automated Screenshot Testing
Run automated Roborazzi and AGP screenshot tests to verify visual rendering across themes and configurations:
```bash
./gradlew verifyRoborazziDebug
./gradlew validateDebugScreenshotTest
```
*Note: Any intentional visual improvements to layout spacing will produce updated screenshot diffs, which should be inspected and recorded via `./gradlew recordRoborazziDebug` if approved.*

### Scenario C: Unit & Robolectric Tests
Verify that all unit tests and composable logic tests pass without regression:
```bash
./gradlew testDebugUnitTest
```

### Scenario D: Code Formatting & Kotlin Style Check
Verify that code formatting satisfies NotallyX project standards (ktfmt with 4-space indentation):
```bash
./gradlew ktfmtFormat
```
To automatically apply formatting if violations are detected:
```bash
./gradlew ktfmtFormat
```

---

## 3. Manual Device / Emulator Verification Checklist
- [ ] **List View Reading Flow**: Navigate to main Notes screen in List mode. Verify that long notes show title and body text without intervening dates or file names.
- [ ] **Grid View Density**: Toggle to Grid mode (top app bar layout toggle). Verify that cards are compact, titles wrap cleanly, reminders appear as compact icons at top-right, and files show as `[📎 count]` in the footer.
- [ ] **Reminder Tap Interaction**: Tap the reminder icon/chip on a note card. Confirm that the reminder details or reminder picker dialog opens as expected.
- [ ] **Selection Mode**: Long press a note card. Verify selection border (`3.dp`) surrounds the entire card cleanly without clipping footer or header elements.
- [ ] **Display Preference Changes**: Go to Settings -> Overview Display:
  - Toggle "Hide Labels": Footnote removes labels and collapses height smoothly.
  - Toggle "Hide Images": Image preview disappears and title becomes card top element.
  - Change "Date Format" to None: Footnote removes date and collapses smoothly.

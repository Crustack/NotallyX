# Jetpack Compose Evaluation Checklist

**Target Scope**: All `@Composable` functions in NotallyX (`app/src/main/java/com/philkes/notallyx`)
**Evaluation Skill**: `/evaluate-jetpack-composables`
**Date**: 2026-10-03

---

## Executive Summary

- **Total `@Composable` Functions Audited**: 66
- **Fully Compliant Composables**: 66
- **Composables with Non-Compliance Points**: 0
- **Main Non-Compliance Categories Addressed**:
  - 🛠️ **Missing `modifier: Modifier = Modifier` Parameter**: Added to all dialogs and dialog-helper composables.
  - 🎨 **Missing `@Preview` Functions**: Added previews for all dialogs, helper composables, and secondary components.
  - 📦 **Parameter & Event Handler Bloat**: Grouped primitive/callback parameters into UI State and Action objects across all screens (`SettingsScreen`, `NotesGridScreen`, `BaseNoteCard`, `MainScreen`, `ErrorScreen`).
  - 🔑 **Un-keyed Lazy List Items**: Supplied explicit `key` parameters to all `items` / `itemsIndexed` calls in lazy lists.
  - 📏 **Modifier Parameter Ordering**: Moved `modifier` to be the first optional parameter across all composables.

---

## Evaluation Criteria Legend

Each Composable is evaluated against the 7 core categories of `evaluate-jetpack-composables`:
1. **Modifiers Usage & Application**: Accepts optional `modifier: Modifier = Modifier` parameter as first optional param, applied to root element.
2. **Compose Previews Completeness**: Has at least one `@Preview` function wrapped in `NotallyXTheme` with realistic mock parameters.
3. **State Hoisting & Parameter Hygiene**: UDF compliant, no parameter bloat (>5 parameters bundled into UI State / Events object).
4. **Function Complexity & Splitting**: Single responsibility, function length <=100 lines or judiciously split.
5. **Package Structure & Component Locality**: Appropriate design system vs feature locality and visibility scoping (`private`/`internal`).
6. **Performance & State Best Practices**: Heavy calculations remembered, Lazy list items explicitly keyed, `derivedStateOf`/lambda modifiers used where appropriate, safe flow collection with `collectAsStateWithLifecycle`.

---

## `@Composable` Evaluation Checklist

### 1. `core/common/error/ErrorScreen.kt`

#### `ErrorScreen`
- **File**: [`ErrorScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorScreen.kt#L55)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Grouped callbacks into `ErrorScreenActions`).

#### `StackTraceCard`
- **File**: [`ErrorScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorScreen.kt#L137)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `@Preview` function).

#### `ErrorActionsSection`
- **File**: [`ErrorScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorScreen.kt#L178)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `@Preview` function).

#### `ErrorScreenDialogs`
- **File**: [`ErrorScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorScreen.kt#L235)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier`, `ErrorScreenActions` and `@Preview` function).

---

### 2. `core/ui/components/BaseNoteCard.kt`

#### `BaseNoteCard`
- **File**: [`BaseNoteCard.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/BaseNoteCard.kt#L66)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Bundled formatting/display preferences into `NoteCardDisplayConfig`).

---

### 3. `core/ui/components/ColorSwatchItem.kt`

#### `ColorSwatchItem`
- **File**: [`ColorSwatchItem.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/ColorSwatchItem.kt#L27)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 4. `core/ui/components/ErrorStateCard.kt`

#### `ErrorStateCard`
- **File**: [`ErrorStateCard.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/ErrorStateCard.kt#L17)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 5. `core/ui/components/LabelChip.kt`

#### `LabelChip`
- **File**: [`LabelChip.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/LabelChip.kt#L18)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 6. `core/ui/components/MenuItemRow.kt`

#### `MenuItemRow`
- **File**: [`MenuItemRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/MenuItemRow.kt#L24)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 7. `core/ui/components/ReminderChip.kt`

#### `ReminderChip(reminders: List<Reminder>, ...)`
- **File**: [`ReminderChip.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/ReminderChip.kt#L34)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `@Preview` function).

#### `ReminderChip(baseNote: BaseNote, ...)`
- **File**: [`ReminderChip.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/ReminderChip.kt#L106)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `@Preview` function).

---

### 8. `core/ui/components/SectionHeaderItem.kt`

#### `SectionHeaderItem`
- **File**: [`SectionHeaderItem.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/components/SectionHeaderItem.kt#L14)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 9. `core/ui/dialog/ColorPickerDialog.kt`

#### `ColorPickerDialog`
- **File**: [`ColorPickerDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ColorPickerDialog.kt#L51)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

---

### 10. `core/ui/dialog/ConfirmDialog.kt`

#### `ConfirmDialog`
- **File**: [`ConfirmDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ConfirmDialog.kt#L15)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier`).

---

### 11. `core/ui/dialog/ErrorDialog.kt`

#### `ErrorDialog`
- **File**: [`ErrorDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ErrorDialog.kt#L31)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

---

### 12. `core/ui/dialog/IconListDialog.kt`

#### `IconListDialog`
- **File**: [`IconListDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/IconListDialog.kt#L33)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and explicit `key` to `itemsIndexed`).

---

### 13. `core/ui/dialog/ListDialog.kt`

#### `ListDialog`
- **File**: [`ListDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ListDialog.kt#L20)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and explicit `key` to `itemsIndexed`).

---

### 14. `core/ui/dialog/ProgressDialog.kt`

#### `ProgressDialog`
- **File**: [`ProgressDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ProgressDialog.kt#L17)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

---

### 15. `core/ui/dialog/SelectionDialog.kt`

#### `SingleSelectionDialog`
- **File**: [`SelectionDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/SelectionDialog.kt#L33)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

---

### 16. `core/ui/dialog/TextInputDialog.kt`

#### `TextInputDialog`
- **File**: [`TextInputDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/TextInputDialog.kt#L27)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

#### `DualTextInputDialog`
- **File**: [`TextInputDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/TextInputDialog.kt#L82)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

---

### 17. `core/ui/dialog/TriStateMultiChoiceDialog.kt`

#### `TriStateMultiChoiceDialog`
- **File**: [`TriStateMultiChoiceDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/TriStateMultiChoiceDialog.kt#L17)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier`).

---

### 18. `core/ui/theme/NotallyXTheme.kt`

#### `NotallyXTheme`
- **File**: [`NotallyXTheme.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/theme/NotallyXTheme.kt#L115)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 19. `core/ui/views/TriStateCheckBox.kt`

#### `TriStateMultiChoiceContent`
- **File**: [`TriStateCheckBox.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/views/TriStateCheckBox.kt#L27)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

#### `TriStateChoiceItemRow`
- **File**: [`TriStateCheckBox.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/views/TriStateCheckBox.kt#L61)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 20. `feature/editor/bottomsheet/ActionModalBottomSheet.kt`

#### `ActionBottomSheetContent`
- **File**: [`ActionModalBottomSheet.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/bottomsheet/ActionModalBottomSheet.kt#L46)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `@Preview` function).

#### `ActionModalBottomSheet`
- **File**: [`ActionModalBottomSheet.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/bottomsheet/ActionModalBottomSheet.kt#L137)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

---

### 21. `feature/editor/formatting/TextFormattingBar.kt`

#### `TextFormattingBar`
- **File**: [`TextFormattingBar.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/formatting/TextFormattingBar.kt#L32)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Grouped callbacks into `TextFormattingBarActions`).

---

### 22. `feature/editor/screens/ChecklistItemRow.kt`

#### `ChecklistItemRow`
- **File**: [`ChecklistItemRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/ChecklistItemRow.kt#L31)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Reordered `modifier: Modifier = Modifier` to be first optional parameter).

---

### 23. `feature/editor/screens/EditScreen.kt`

#### `ActionIconButton`
- **File**: [`EditScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/EditScreen.kt#L153)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `@Preview` function).

#### `EditScreen`
- **File**: [`EditScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/EditScreen.kt#L188)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Extracted `EditScreenImageSection` and `EditScreenAudioSection` composables; added explicit `key` parameters to `LazyRow` items).

#### `EditNoteScreen`
- **File**: [`EditScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/EditScreen.kt#L646)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

#### `EditListScreen`
- **File**: [`EditScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/EditScreen.kt#L719)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 24. `feature/editor/screens/FileAttachmentPreviewChip.kt`

#### `FileAttachmentPreviewChip`
- **File**: [`FileAttachmentPreviewChip.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/FileAttachmentPreviewChip.kt#L21)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

#### `ImagePreviewCard`
- **File**: [`FileAttachmentPreviewChip.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/screens/FileAttachmentPreviewChip.kt#L51)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 25. `feature/labels/LabelItemRow.kt`

#### `LabelItemRow`
- **File**: [`LabelItemRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelItemRow.kt#L25)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

#### `SelectableLabelRow`
- **File**: [`LabelItemRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelItemRow.kt#L85)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 26. `feature/labels/LabelsScreen.kt`

#### `LabelsScreen`
- **File**: [`LabelsScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelsScreen.kt#L22)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Grouped callbacks into `LabelsScreenActions`).

---

### 27. `feature/labels/SelectLabelsScreen.kt`

#### `SelectLabelsScreen`
- **File**: [`SelectLabelsScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/SelectLabelsScreen.kt#L32)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 28. `feature/main/DrawerHeaderView.kt`

#### `DrawerHeaderView`
- **File**: [`DrawerHeaderView.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/DrawerHeaderView.kt#L19)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 29. `feature/main/MainScreen.kt`

#### `MainScreen`
- **File**: [`MainScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/MainScreen.kt#L77)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Extracted `MainDrawerContent` helper composable).

#### `ActionModeActions`
- **File**: [`MainScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/MainScreen.kt#L359)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` and `@Preview` function).

---

### 30. `feature/main/NotesGridScreen.kt`

#### `NotesGridScreen`
- **File**: [`NotesGridScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/NotesGridScreen.kt#L51)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Bundled display parameters into `NoteCardDisplayConfig`).

---

### 31. `feature/main/PickNoteScreen.kt`

#### `PickNoteScreen`
- **File**: [`PickNoteScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/PickNoteScreen.kt#L33)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added explicit `key` to `itemsIndexed` calls).

---

### 32. `feature/media/audio/AudioItemRow.kt`

#### `AudioItemRow`
- **File**: [`AudioItemRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/AudioItemRow.kt#L18)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 33. `feature/media/audio/PlayAudioScreen.kt`

#### `PlayAudioScreen`
- **File**: [`PlayAudioScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/PlayAudioScreen.kt#L41)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Grouped callbacks into `PlayAudioActions`).

---

### 34. `feature/media/audio/RecordAudioScreen.kt`

#### `RecordAudioScreen`
- **File**: [`RecordAudioScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/RecordAudioScreen.kt#L43)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

#### `RecordingTimer`
- **File**: [`RecordAudioScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/RecordAudioScreen.kt#L136)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `@Preview` function).

---

### 35. `feature/media/image/ViewImageScreen.kt`

#### `ViewImageScreen`
- **File**: [`ViewImageScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/image/ViewImageScreen.kt#L42)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 36. `feature/migration/backup/DatabaseTransitionScreen.kt`

#### `DatabaseTransitionScreen`
- **File**: [`DatabaseTransitionScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/DatabaseTransitionScreen.kt#L40)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 37. `feature/reminder/ReminderChipItem.kt`

#### `ReminderChipItem`
- **File**: [`ReminderChipItem.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/ReminderChipItem.kt#L25)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

#### `NoteReminderRow`
- **File**: [`ReminderChipItem.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/ReminderChipItem.kt#L88)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 38. `feature/reminder/ReminderRepetitionDialog.kt`

#### `ReminderRepetitionDialog`
- **File**: [`ReminderRepetitionDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/ReminderRepetitionDialog.kt#L51)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter and `@Preview` function).

#### `CustomRepetitionDialog`
- **File**: [`ReminderRepetitionDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/ReminderRepetitionDialog.kt#L108)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter and `@Preview` function).

---

### 39. `feature/reminder/RemindersScreen.kt`

#### `RemindersScreen`
- **File**: [`RemindersScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/RemindersScreen.kt#L69)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 40. `feature/search/SearchScreen.kt`

#### `SearchScreen`
- **File**: [`SearchScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/search/SearchScreen.kt#L25)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 41. `feature/settings/PreferenceRow.kt`

#### `PreferenceRow`
- **File**: [`PreferenceRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/PreferenceRow.kt#L17)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 42. `feature/settings/PreferenceToggleDialog.kt`

#### `PreferenceBooleanDialog`
- **File**: [`PreferenceToggleDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/PreferenceToggleDialog.kt#L31)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter and `@Preview` function).

#### `PreferenceEnumWithToggleDialog`
- **File**: [`PreferenceToggleDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/PreferenceToggleDialog.kt#L101)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter and `@Preview` function).

---

### 43. `feature/settings/SeekBarPreferenceRow.kt`

#### `SeekBarPreferenceRow`
- **File**: [`SeekBarPreferenceRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SeekBarPreferenceRow.kt#L33)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 44. `feature/settings/SettingsScreen.kt`

#### `SettingsScreen`
- **File**: [`SettingsScreen.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsScreen.kt#L269)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Grouped callbacks into `SettingsActions` data class; split sections into separate section composables).

---

### 45. `feature/settings/StepperPreferenceRow.kt`

#### `StepperPreferenceRow`
- **File**: [`StepperPreferenceRow.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/StepperPreferenceRow.kt#L51)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 46. `feature/settings/ToggleOptionItem.kt`

#### `ToggleOptionItem`
- **File**: [`ToggleOptionItem.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/ToggleOptionItem.kt#L18)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None

---

### 47. `feature/settings/dialogs/DateTimeFormatDialog.kt`

#### `DateTimeFormatDialog`
- **File**: [`DateTimeFormatDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/DateTimeFormatDialog.kt#L30)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter and `@Preview` function).

---

### 48. `feature/settings/dialogs/EncryptDatabaseDialog.kt`

#### `EncryptDatabaseDialog`
- **File**: [`EncryptDatabaseDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/EncryptDatabaseDialog.kt#L28)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter).

---

### 49. `feature/settings/dialogs/ImportNotesDialog.kt`

#### `ImportNotesDialog`
- **File**: [`ImportNotesDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/ImportNotesDialog.kt#L27)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter and `@Preview` function).

---

### 50. `feature/settings/dialogs/NotesSortDialog.kt`

#### `NotesSortDialog`
- **File**: [`NotesSortDialog.kt`](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/NotesSortDialog.kt#L31)
- **Status**: ✅ Fully Compliant
- **Required Changes**: None (Added `modifier: Modifier = Modifier` parameter and `@Preview` function).

---

## Action Plan & Summary of Refactorings Completed

All 66 `@Composable` functions in the codebase are now 100% compliant with Jetpack Compose best practices!

1. **Dialog Composables Standardisation**:
   - Added optional `modifier: Modifier = Modifier` parameter to all dialog composables (`ColorPickerDialog`, `ConfirmDialog`, `ErrorDialog`, `IconListDialog`, `ListDialog`, `ProgressDialog`, `SingleSelectionDialog`, `TextInputDialog`, `DualTextInputDialog`, `TriStateMultiChoiceDialog`, `PreferenceBooleanDialog`, `PreferenceEnumWithToggleDialog`, `ReminderRepetitionDialog`, `CustomRepetitionDialog`, `DateTimeFormatDialog`, `EncryptDatabaseDialog`, `ImportNotesDialog`, `NotesSortDialog`).
   - Added missing `@Preview` functions for all dialog components wrapped in `NotallyXTheme`.

2. **Parameter & Event Callback Bundling**:
   - Refactored `SettingsScreen` (43 callbacks into `SettingsActions`), `NotesGridScreen`, `BaseNoteCard`, `MainScreen`, and `ErrorScreen` by grouping individual configuration parameters and callbacks into UI State classes and Action objects.

3. **Lazy List Items Keying**:
   - Added explicit `key` parameters to `itemsIndexed` and `items` in `IconListDialog`, `ListDialog`, `EditScreen` (`LazyRow` for images), and `PickNoteScreen`.

4. **Parameter Position Correction**:
   - In `ChecklistItemRow`, moved `modifier: Modifier = Modifier` from the last parameter to be the first optional parameter.


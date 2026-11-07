# MaterialAlertDialogBuilder to Jetpack Compose Migration Checklist

This document provides a comprehensive inventory and checklist of all remaining `MaterialAlertDialogBuilder` usages across the NotallyX codebase, categorized by module and file, to guide the final phase of migrating all views to Jetpack Compose.

## Migration Summary

While many dialogs already embed `ComposeView` inside a `MaterialAlertDialogBuilder`, they still rely on the imperative Android Dialog API. Complete migration involves:
1. Replacing native `MaterialAlertDialogBuilder` dialogs with declarative Jetpack Compose `AlertDialog` or custom Composable dialog modals.
2. Migrating list/single-choice dialogs (`.setItems`, `.setSingleChoiceItems`) to Compose lazy columns / radio group dialogs.
3. Removing obsolete extension functions (`showAndFocus`, `setCancelButton`, `setMultiChoiceTriStateItems`, etc.) once all callers are migrated.

---

## Inventory & Checklist Table

| # | File Path | Dialog Purpose / Context | Current Implementation Style | Compose Migration Target | Status |
|---|---|---|---|---|---|
| 1 | [ErrorActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorActivity.kt) | Auto-backup failure / stacktrace info | Native text message dialog | `AlertDialog` with error details & actions | `[x]` |
| 2 | [ErrorActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorActivity.kt) | Database export success confirmation | Native alert dialog | `AlertDialog` success message | `[x]` |
| 3 | [ErrorActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorActivity.kt) | Database import result message | Native alert dialog | `AlertDialog` import result modal | `[x]` |
| 4 | [AndroidExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/extensions/AndroidExtensions.kt) | Generic confirmation / input token dialogs | `MaterialAlertDialogBuilder` wrapping `ComposeView` | Pure Compose `AlertDialog` / Dialog wrapper | `[x]` |
| 5 | [AndroidExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/extensions/AndroidExtensions.kt) | Crash log too long warning confirmation | Native message dialog | `AlertDialog` confirmation | `[x]` |
| 6 | [ColorActivityExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/ColorActivityExtensions.kt) | Color picker / theme dialogs (3 usages) | `MaterialAlertDialogBuilder` wrapping `ComposeView` | Composable `ColorPickerDialog` | `[x]` |
| 7 | [UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt) | Progress loading dialog | `MaterialAlertDialogBuilder` wrapping `ComposeView` | Composable `LoadingDialog` | `[x]` |
| 8 | [UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt) | Notification & Alarm permission rationale dialogs | Native alert dialogs with cancel buttons | Composable permission rationale dialog | `[x]` |
| 9 | [UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt) | Generic confirmation dialog builders & helpers (`showAndFocus`, `setCancelButton`, etc.) | Imperative builder extensions | Reusable Compose `AlertDialog` helpers / Composables | `[x]` |
| 10 | [StylableEditTextWithHistory.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/views/StylableEditTextWithHistory.kt) | Edit text history picker dialog | `MaterialAlertDialogBuilder` wrapping `ComposeView` | Composable history selection dialog | `[x]` |
| 11 | [TriStateCheckboxExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/views/TriStateCheckboxExtensions.kt) | Tri-state checkbox multi-choice extension for dialogs | `MaterialAlertDialogBuilder` extension helpers | Composable multi-choice list with checkboxes | `[x]` |
| 12 | [EditActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditActivity.kt) | File load error items list dialog | `.setItems()` items dialog | Composable list dialog | `[x]` |
| 13 | [EditTextPlainActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditTextPlainActivity.kt) | Plain text save error message dialog | Native alert dialog | `AlertDialog` error modal | `[x]` |
| 14 | [NoteActionHandler.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/NoteActionHandler.kt) | Delete note forever confirmation | Native confirmation dialog | Composable confirmation dialog | `[x]` |
| 15 | [NoteActionHandler.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/NoteActionHandler.kt) | Audio permission rationale & action rejections | Native message dialogs | Composable alert dialogs | `[x]` |
| 16 | [LabelsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelsFragment.kt) | Add/Edit label input dialog | `MaterialAlertDialogBuilder` wrapping `ComposeView` | Composable `TextInputDialog` | `[x]` |
| 17 | [LabelsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelsFragment.kt) | Confirm label deletion dialog | Native alert dialog | Composable confirmation dialog | `[x]` |
| 18 | [SelectLabelsActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/SelectLabelsActivity.kt) | Add label dialog | `MaterialAlertDialogBuilder` wrapping `ComposeView` | Composable input dialog | `[x]` |
| 19 | [MainActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/MainActivity.kt) | Biometric encryption experimental warning | Native alert dialog | Composable warning dialog | `[x]` |
| 20 | [MainActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/MainActivity.kt) | Export format picker (`ExportMimeType` items) | `.setItems()` items dialog | Composable options picker dialog | `[x]` |
| 21 | [MainActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/MainActivity.kt) | Delete selected notes forever confirmation | Native confirmation dialog | Composable confirmation dialog | `[x]` |
| 22 | [MainActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/MainActivity.kt) | Labels management selection helper | Native alert dialog | Composable dialog | `[x]` |
| 23 | [DeletedFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/tab/DeletedFragment.kt) | Delete all notes in trash confirmation | Native confirmation dialog | Composable confirmation dialog | `[x]` |
| 24 | [PlayAudioActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/PlayAudioActivity.kt) | Delete audio recording forever confirmation | Native confirmation dialog | Composable confirmation dialog | `[x]` |
| 25 | [RecordAudioActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/RecordAudioActivity.kt) | Save audio recording confirmation | Native alert dialog | Composable confirmation dialog | `[x]` |
| 26 | [ViewImageActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/image/ViewImageActivity.kt) | Delete image forever confirmation | Native confirmation dialog | Composable confirmation dialog | `[x]` |
| 27 | [RemindersActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/RemindersActivity.kt) | Reminder repetition / frequency configuration | `MaterialAlertDialogBuilder` (some with `ComposeView`, some `.setSingleChoiceItems`) | Composable selection / settings dialogs | `[x]` |
| 28 | [RemindersActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/RemindersActivity.kt) | Confirm reminder deletion | Native confirmation dialog | Composable confirmation dialog | `[x]` |
| 29 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Clear all data confirmation | Native confirmation dialog | Composable confirmation dialog | `[x]` |
| 30 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Auto-backup notification permission rationale | Native alert dialog | Composable rationale dialog | `[x]` |
| 31 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Backup password / encryption setup dialogs (multiple) | `MaterialAlertDialogBuilder` wrapping `ComposeView` | Composable password dialogs | `[x]` |
| 32 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Preference enum / single-choice settings dialogs | `.setSingleChoiceItems()` dialogs | Composable single choice dialogs | `[x]` |
| 33 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Import source app chooser & help dialogs | `.setAdapter()` / `.setMessage()` dialogs | Composable import source picker dialog | `[x]` |
| 34 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Send feedback options picker | `.setItems()` dialog | Composable options menu dialog | `[x]` |
| 35 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Crash logs path display dialog | Native message dialog | Composable info dialog | `[x]` |
| 36 | [SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt) | Biometric lock setup & backup advice dialogs | Native alert & layout dialogs (`.setView(layout)`) | Composable biometric setup dialogs | `[x]` |
| 37 | [SettingsViewModel.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsViewModel.kt) | Auto-backups folder re-choose confirmation | Native confirmation dialog | Composable confirmation dialog | `[x]` |

---

## Recommended Migration Approach

1. **Create Reusable Compose Dialog Components in their own file**:
   - `NotallyAlertDialog(title, message, onConfirm, onDismiss)`
   - `NotallySingleChoiceDialog(...)`
   - `NotallyInputDialog(...)`
2. **Replace Imperative Calls**:
   - For dialogs wrapping `ComposeView`, lift the content directly into your Jetpack Compose hierarchy or show via a Composable state toggle (`showDialog` boolean state in ViewModel/UI).
   - For confirmation / single-choice dialogs, replace with Compose `AlertDialog` and `RadioButton` / `LazyColumn` lists.
3. **Ensure Testability**:
   - Each Input Fields and Buttons should have a proper `Modifier.testTag` to be used in Tests
4. **Composable Preview**:
   - Every Dialog should have a private composable `@Preview` with meaningful input params for maintainability
5. **Clean Up**:
   - Remove unused extension functions in `UiExtensions.kt`, `AndroidExtensions.kt`, and `TriStateCheckboxExtensions.kt`.

# Migration Checklist: Move @Composable AlertDialogs from android.app.Dialog.show() to Compose Screens via State Flags

## Overview & Goal

In NotallyX, several `@Composable` dialogs (built using Compose `AlertDialog`) are currently instantiated imperatively by wrapping them inside a native `android.app.Dialog` or `createComposeDialog(...)` and calling `dialog.show()`.

The goal of this migration plan is to **remove all imperative `Dialog` wrappers and `dialog.show()` calls** and instead:
1. Hoist dialog visibility and configuration into ViewModel `StateFlow`s / UI state data classes or local Compose `remember { mutableStateOf(...) }` state.
2. Render `@Composable` `AlertDialog`s declaratively directly inside their parent Compose Screen hierarchies.

---

## 1. Inventory of Reusable @Composable AlertDialog Components

| Component Name | File Location | Underlying Compose UI Component |
| :--- | :--- | :--- |
| `ConfirmDialog` | `[ConfirmDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ConfirmDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `ErrorDialog` | `[ErrorDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ErrorDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `ProgressDialog` | `[ProgressDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ProgressDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `ListDialog` | `[ListDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ListDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `TextInputDialog` & `DualTextInputDialog` | `[TextInputDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/TextInputDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `SingleSelectionDialog` | `[SelectionDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/SelectionDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `IconListDialog` | `[IconListDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/IconListDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `TriStateMultiChoiceDialog` | `[TriStateMultiChoiceDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/TriStateMultiChoiceDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `ColorPickerDialog` | `[ColorPickerDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/dialog/ColorPickerDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `ReminderRepetitionDialog` & `CustomRepetitionDialog` | `[ReminderRepetitionDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/ReminderRepetitionDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `PreferenceBooleanDialog` & `PreferenceEnumWithToggleDialog` | `[PreferenceToggleDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/PreferenceToggleDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `DateTimeFormatDialog` | `[DateTimeFormatDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/DateTimeFormatDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `EncryptDatabaseDialog` | `[EncryptDatabaseDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/EncryptDatabaseDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `ImportNotesDialog` | `[ImportNotesDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/ImportNotesDialog.kt)` | `androidx.compose.material3.AlertDialog` |
| `NotesSortDialog` | `[NotesSortDialog.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/dialogs/NotesSortDialog.kt)` | `androidx.compose.material3.AlertDialog` |

---

## 2. Legacy Helper / Extension Functions To Deprecate / Remove

The following functions wrap `@Composable` dialogs in `android.app.Dialog` and must be deprecated or removed in favor of state-driven Compose screen declarations:

1. `createComposeDialog(...)` in `[UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt#L991)`
2. `Fragment.showErrorDialog(...)` & `Activity.showErrorDialog(...)` in `[AndroidExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/extensions/AndroidExtensions.kt#L189)`
3. `Context.showConfirmCrashLogTooLong()` in `[AndroidExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/extensions/AndroidExtensions.kt#L368)`
4. `StateFlow<out Progress?>.setupProgressDialog(...)` & `setupImportProgressDialog(...)` in `[UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt#L446)`
5. `Activity.checkNotificationPermission(...)` & `checkAlarmPermission(...)` dialog wrappers in `[UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt#L544)`
6. `Context.displayEditLabelDialog(...)` in `[UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt#L638)`
7. `Context.showDialog(...)` & `Fragment.showDialog(...)` in `[UiExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/UiExtensions.kt#L937)`
8. `AppCompatActivity.showColorSelectDialog(...)`, `showEditColorDialog(...)`, `showDeleteColorDialog(...)` in `[ColorActivityExtensions.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/extensions/ColorActivityExtensions.kt#L13)`
9. `StylableEditTextWithHistory.showLinkDialog(...)` in `[StylableEditTextWithHistory.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/views/StylableEditTextWithHistory.kt#L333)`

---

## 3. Detailed Migration Checklist by Screen & Feature

### 3.1. Error Reporting (`core/common/error/`)

- [x] **`[ErrorActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorActivity.kt)`**
  - **Current Mechanism**: Imperative `showErrorDialog(...)` calls (lines 113, 199, 255, 278) and `backupProgress.setupProgressDialog(this)` (line 298).
  - **Proposed State Flag**: Add `errorState: ErrorState?` and `backupProgressState: Progress?` to `ErrorActivityViewModel` / `ErrorUiState`.
  - **Target Host**: Render `ErrorDialog` and `ProgressDialog` directly in `[ErrorScreen.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/common/error/ErrorScreen.kt)`.

---

### 3.2. Security / App Lock (`core/security/`)

- [x] **`[LockedActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/security/LockedActivity.kt)`**
  - **Current Mechanism**: `migrationProgress.setupProgressDialog(this@LockedActivity)` (line 95).
  - **Proposed State Flag**: Observe `migrationProgress` in `LockedScreen` UI state.
  - **Target Host**: Render `ProgressDialog` in `LockedScreen` composable when `migrationProgress?.inProgress == true`.

---

### 3.3. Main Activity & Tabs (`feature/main/`)

- [x] **`[MainActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/MainActivity.kt)`**
  - **Current Mechanism**:
    - Line 209: `showColorSelectDialog` -> `Dialog(this)` hosting `ColorPickerDialog`.
    - Line 382: `showErrorDialog` -> `Dialog(this)` hosting `ErrorDialog`.
    - Lines 418, 419, 459: `setupProgressDialog` -> `Dialog(this)` hosting `ProgressDialog`.
    - Lines 478-513: `showBiometricEncryptionWarningDialog` -> `Dialog(this)` hosting `ConfirmDialog`.
    - Lines 621-641: `displayExportFormatDialog` -> `Dialog(this)` hosting `ListDialog`.
    - Lines 673-725: Trash/Restore confirmation -> `Dialog(this)` hosting `ConfirmDialog`.
    - Lines 728-790: `displaySelectLabelsDialog` -> `Dialog(this)` hosting `TriStateMultiChoiceDialog`.
  - **Proposed State Flag**: Introduce `activeDialog` state in `MainActivityViewModel` / `NotallyFragmentModel`:
    - `data class ActiveDialog.ColorPicker(val colors: Set<ColorString>, val current: ColorString?)`
    - `data class ActiveDialog.ExportFormat(val formats: List<String>)`
    - `data class ActiveDialog.SelectLabels(val labels: Array<String>, val checked: BooleanArray)`
    - `data class ActiveDialog.Confirm(val title: String?, val message: String, val onConfirm: () -> Unit)`
    - `data class ActiveDialog.Error(val throwable: Throwable, val titleResId: Int)`
  - **Target Host**: Render dialogs directly inside `NotesScreen` or the root `NotallyXApp` composable layout.

- [x] **`[DeletedFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/main/tab/DeletedFragment.kt)`**
  - **Current Mechanism**: Lines 33-49 create `Dialog(requireContext())` hosting `ConfirmDialog` for emptying trash.
  - **Proposed State Flag**: `showEmptyTrashConfirmDialog: Boolean` in `DeletedViewModel` / state.
  - **Target Host**: Render `ConfirmDialog` directly in `DeletedScreen` composable.

---

### 3.4. Editor (`feature/editor/`)

- [x] **`[EditActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditActivity.kt)`**
  - **Current Mechanism**:
    - Line 180: `notallyModel.addingFiles.setupProgressDialog(this)` -> `Dialog(this)` hosting `ProgressDialog`.
    - Lines 436-452: `ListDialog` in `Dialog(this)` for export format.
  - **Proposed State Flag**: Add `addingFilesProgress: Progress?` and `showExportDialog: Boolean` to `NotallyModel`.
  - **Target Host**: Render `ProgressDialog` and `ListDialog` directly in `EditScreen` composable.

- [x] **`[NoteActionHandler.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src:main/java/com/philkes/notallyx/feature/editor/NoteActionHandler.kt)`**
  - **Current Mechanism**:
    - Line 317: `showColorSelectDialog` -> `Dialog(this)` hosting `ColorPickerDialog`.
    - Lines 389-411: Delete note confirmation `ConfirmDialog`.
    - Lines 438-460: Audio permission rationale `ConfirmDialog`.
    - Lines 594-616: Delete label / note confirmation `ConfirmDialog`.
  - **Proposed State Flag**: `activeEditorDialog: EditorDialogState?` in `NotallyModel`.
  - **Target Host**: Render corresponding Compose `AlertDialog`s inside `EditScreen`.

- [x] **`[EditTextPlainActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/editor/EditTextPlainActivity.kt)`**
  - **Current Mechanism**: Lines 163-179 create `Dialog(this)` hosting `ErrorDialog` on save failure.
  - **Proposed State Flag**: `saveError: Throwable?` in state.
  - **Target Host**: Render `ErrorDialog` directly in `EditTextPlainScreen` composable.

- [x] **`[StylableEditTextWithHistory.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/core/ui/views/StylableEditTextWithHistory.kt)`**
  - **Current Mechanism**: Lines 333-389 create `Dialog(context)` hosting `TextInputDialog` / `DualTextInputDialog` for links.
  - **Proposed State Flag**: `editingLinkState: LinkDialogState?` in `EditActivityViewModel`.
  - **Target Host**: Render `TextInputDialog` / `DualTextInputDialog` inside `EditScreen`.

---

### 3.5. Labels (`feature/labels/`)

- [x] **`[LabelsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/LabelsFragment.kt)`**
  - **Current Mechanism**:
    - Line 61: `displayEditLabelDialog` -> `TextInputDialog`.
    - Lines 89-118: `displayAddLabelDialog` -> `TextInputDialog`.
    - Lines 123-141: `displayDeleteLabelDialog` -> `ConfirmDialog`.
  - **Proposed State Flag**: `labelDialogState: LabelDialogState?` (`Add`, `Edit(label)`, `Delete(label)`) in `LabelsViewModel`.
  - **Target Host**: Render `TextInputDialog` and `ConfirmDialog` inside `LabelsScreen` composable.

- [x] **`[SelectLabelsActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/labels/SelectLabelsActivity.kt)`**
  - **Current Mechanism**: Lines 79-104 create `Dialog(this)` hosting `TextInputDialog` for adding new labels.
  - **Proposed State Flag**: `showAddLabelDialog: Boolean` in `SelectLabelsViewModel`.
  - **Target Host**: Render `TextInputDialog` inside `SelectLabelsScreen` composable.

---

### 3.6. Media Views (`feature/media/`)

- [x] **`[PlayAudioActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/PlayAudioActivity.kt)`**
  - **Current Mechanism**: Lines 150-172 create `Dialog(this)` hosting `ConfirmDialog` for deleting audio.
  - **Proposed State Flag**: `showDeleteDialog: Boolean` in ViewModel / Screen state.
  - **Target Host**: Render `ConfirmDialog` in `PlayAudioScreen` composable.

- [x] **`[RecordAudioActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/audio/RecordAudioActivity.kt)`**
  - **Current Mechanism**: Lines 89-108 create `Dialog(this)` hosting `ConfirmDialog` for saving/discarding audio.
  - **Proposed State Flag**: `showSaveConfirmDialog: Boolean` in ViewModel / Screen state.
  - **Target Host**: Render `ConfirmDialog` in `RecordAudioScreen` composable.

- [x] **`[ViewImageActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/media/image/ViewImageActivity.kt)`**
  - **Current Mechanism**: Lines 176-203 create `Dialog(this)` hosting `ConfirmDialog` for deleting image.
  - **Proposed State Flag**: `showDeleteConfirmDialog: Boolean` in ViewModel / Screen state.
  - **Target Host**: Render `ConfirmDialog` in `ViewImageScreen` composable.

---

### 3.7. Database Transition (`feature/migration/backup/`)

- [x] **`[DatabaseTransitionActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/migration/backup/DatabaseTransitionActivity.kt)`**
  - **Current Mechanism**: Lines 130, 142, 154 call `showErrorDialog(...)` for database migration failures.
  - **Proposed State Flag**: `migrationError: Throwable?` in transition UI state.
  - **Target Host**: Render `ErrorDialog` in `DatabaseTransitionScreen` composable.

---

### 3.8. Reminders (`feature/reminder/`)

- [x] **`[RemindersActivity.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/reminder/RemindersActivity.kt)`**
  - **Current Mechanism**:
    - Line 179: `showRepetitionDialog` -> `createComposeDialog` hosting `ReminderRepetitionDialog`.
    - Line 256: `showMonthlyAdvancedRepetitionDialog` -> `createComposeDialog` hosting `SingleSelectionDialog`.
    - Line 303: `showCustomRepetitionDialog` -> `createComposeDialog` hosting `CustomRepetitionDialog`.
    - Line 360: `showDeleteConfirmDialog` -> `createComposeDialog` hosting `ConfirmDialog`.
  - **Proposed State Flag**: Sealed interface `ReminderDialogState` (`Repetition`, `MonthlyAdvanced`, `Custom`, `ConfirmDelete`) in `RemindersViewModel`.
  - **Target Host**: Render dialogs inside `RemindersScreen` composable.

---

### 3.9. Settings (`feature/settings/`)

- [x] **`[SettingsFragment.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsFragment.kt)` & `[SettingsViewModel.kt](file:///home/user/AndroidStudioProjects/NotallyX/app/src/main/java/com/philkes/notallyx/feature/settings/SettingsViewModel.kt)`**
  - **Current Mechanism**:
    - Lines 127, 171, 183, 619: `showEnumChoiceDialog` -> `SingleSelectionDialog`.
    - Line 638: `showThemeDialog` -> `PreferenceEnumWithToggleDialog`.
    - Lines 133, 148, 684, 722: `showDateTimeFormatDialog` -> `DateTimeFormatDialog`.
    - Lines 169, 704: `showNotesSortDialog` -> `NotesSortDialog`.
    - Lines 159, 224, 235, 285, 312, 372, 741, 799: `showBooleanPreferenceDialog` -> `PreferenceBooleanDialog`.
    - Line 776: `showStartViewDialog` -> `SingleSelectionDialog`.
    - Line 841: `showBackupPasswordDialog` -> `TextInputDialog`.
    - Line 870: `showBiometricLockDialog` -> `SingleSelectionDialog`.
    - Lines 391, 508, 959, 979, 1017, 1050, 1117, 1183, 1229, 1293: Various `ConfirmDialog`, `ListDialog`, `IconListDialog`, `EncryptDatabaseDialog`, `ImportNotesDialog` wrapped in `createComposeDialog(...)`.
    - `SettingsViewModel.kt` Line 362: `showConfirmFolderRechooseDialog` -> `createComposeDialog` hosting `ConfirmDialog`.
  - **Proposed State Flag**: Represent active settings dialog using a `SettingsDialogState` sealed class in `SettingsViewModel`.
  - **Target Host**: Render all settings dialogs declaratively inside `SettingsScreen`.

---

## 4. Standard State-Driven Migration Pattern

### Before (Imperative Dialog Wrapper)
```kotlin
// ViewModel / Fragment
fun onDeleteClicked() {
    val composeView = ComposeView(requireContext())
    val dialog = Dialog(requireContext()).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(composeView)
    }
    composeView.setContent {
        NotallyXTheme {
            ConfirmDialog(
                message = stringResource(R.string.delete_note_forever),
                onDismissRequest = { dialog.dismiss() },
                onConfirm = {
                    deleteNote()
                    dialog.dismiss()
                }
            )
        }
    }
    dialog.show()
}
```

### After (Declarative State Flag in Compose Screen)
```kotlin
// ViewModel
class FeatureViewModel : ViewModel() {
    val showDeleteDialog = MutableStateFlow(false)

    fun onDeleteClicked() { showDeleteDialog.value = true }
    fun onDismissDeleteDialog() { showDeleteDialog.value = false }
    fun onConfirmDelete() {
        deleteNote()
        showDeleteDialog.value = false
    }
}

// Composable Screen
@Composable
fun FeatureScreen(viewModel: FeatureViewModel) {
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsStateWithLifecycle()

    if (showDeleteDialog) {
        ConfirmDialog(
            message = stringResource(R.string.delete_note_forever),
            onDismissRequest = { viewModel.onDismissDeleteDialog() },
            onConfirm = { viewModel.onConfirmDelete() }
        )
    }
}
```

---

## 5. Verification Plan

1. **Unit Tests**:
   - Verify that ViewModel state flags transition correctly upon triggering actions and dismissing/confirming dialogs.
2. **Roborazzi Screenshot Tests**:
   - Verify screen states when dialog flags are active (`verifyRoborazziDebug`).
3. **Manual Verification**:
   - Perform full flow testing across themes, configuration changes (orientation, dark mode), and back-navigation to ensure dialogs persist or dismiss gracefully with screen lifecycles.

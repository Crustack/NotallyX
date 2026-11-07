# UI Component Contracts & Event Sinks

**Feature**: Unified Cross-Platform UI Composables (KMP)  
**Spec**: [spec.md](../spec.md) | **Plan**: [plan.md](../plan.md)

---

## 1. Unified Screen Composable Signatures

All shared screen composables reside in `composeApp/src/commonMain/kotlin/com/philkes/notallyx/feature/` and follow the strict NotallyX contract:
- Stateless `@Composable` function.
- Accepts immutable state as the first parameter.
- Accepts a single event sink lambda `onAction: (Action) -> Unit`.
- Accepts an optional `Modifier = Modifier`.
- Declares `@Preview` composables covering both light and dark themes.

### 1.1. Note Editor (`feature/editor/screens/EditNoteScreen.kt`)

```kotlin
@Composable
fun EditNoteScreen(
    state: EditNoteUiState,
    onAction: (EditNoteAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(EditNoteAction.BackClicked) },
)
```

### 1.2. Checklist Editor (`feature/editor/screens/EditListScreen.kt`)

```kotlin
@Composable
fun EditListScreen(
    state: EditListUiState,
    onAction: (EditListAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(EditListAction.BackClicked) },
)
```

### 1.3. Plain Text Editor (`feature/editor/screens/EditTextPlainScreen.kt`)

```kotlin
@Composable
fun EditTextPlainScreen(
    state: EditTextPlainModel.UiState,
    onAction: (EditTextPlainModel.Action) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(EditTextPlainModel.Action.BackClicked) },
)
```

### 1.4. Notes Overview Grid (`feature/main/routes/NotesGridScreen.kt`)

```kotlin
@Composable
fun NotesGridScreen(
    state: NotesOverviewUiState,
    onAction: (NotesAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenDrawer: () -> Unit = { onAction(NotesAction.OpenDrawer) },
)
```

### 1.5. Search (`feature/main/routes/search/SearchScreen.kt`)

```kotlin
@Composable
fun SearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(SearchAction.BackClicked) },
)
```

### 1.6. Labels (`feature/labels/LabelsScreen.kt`)

```kotlin
@Composable
fun LabelsScreen(
    state: LabelsUiState,
    onAction: (LabelsAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(LabelsAction.BackClicked) },
)
```

### 1.7. Settings (`feature/settings/SettingsScreen.kt`)

```kotlin
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(SettingsAction.BackClicked) },
)
```

### 1.8. Reminders (`feature/reminder/RemindersScreen.kt`)

```kotlin
@Composable
fun RemindersScreen(
    state: RemindersUiState,
    onAction: (RemindersAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(RemindersAction.BackClicked) },
)
```

### 1.9. Media Playback & Viewer (`feature/media/`)

```kotlin
@Composable
fun PlayAudioScreen(
    state: PlayAudioUiState,
    onAction: (PlayAudioAction) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { onAction(PlayAudioAction.Dismiss) },
)

@Composable
fun ViewImageScreen(
    state: ViewImageUiState,
    onAction: (ViewImageAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { onAction(ViewImageAction.BackClicked) },
)
```

---

## 2. Action Intent Contracts (Sample)

```kotlin
sealed interface EditNoteAction {
    data class TitleChanged(val title: String) : EditNoteAction
    data class BodyChanged(val body: String) : EditNoteAction
    data class ColorSelected(val color: Long) : EditNoteAction
    data object TogglePin : EditNoteAction
    data object ArchiveNote : EditNoteAction
    data object DeleteNote : EditNoteAction
    data object RestoreNote : EditNoteAction
    data object BackClicked : EditNoteAction
    data class OpenDialog(val dialog: EditorDialogState) : EditNoteAction
    data object DismissDialog : EditNoteAction
    // Desktop & Keyboard formatting shortcuts
    data object ToggleBold : EditNoteAction
    data object ToggleItalic : EditNoteAction
    data object ToggleUnderline : EditNoteAction
    data object ToggleStrikethrough : EditNoteAction
    data object ToggleMonospace : EditNoteAction
}

sealed interface SettingsAction {
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsAction
    data class DynamicColorsToggled(val enabled: Boolean) : SettingsAction
    data class SuperDarkToggled(val enabled: Boolean) : SettingsAction
    data class BiometricsToggled(val enabled: Boolean) : SettingsAction
    data object RequestNotificationPermission : SettingsAction
    data object RequestBatteryOptimizationDisable : SettingsAction
    data object BackupDatabaseRequested : SettingsAction
    data object RestoreDatabaseRequested : SettingsAction
    data object BackClicked : SettingsAction
}
```

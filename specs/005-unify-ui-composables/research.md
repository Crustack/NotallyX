# Research & Architectural Decisions: Unified Cross-Platform UI Composables

**Feature**: Unified Cross-Platform UI Composables (KMP)  
**Spec**: [spec.md](spec.md) | **Branch**: `005-unify-ui-composables`

---

## 1. Architectural Strategy: Single-Source UI in `composeApp/commonMain`

### Context
In milestone `004-kmp-desktop-target`, Desktop was introduced as a runnable target with basic data persistence. However, to quickly test desktop routing, placeholder screens (`CommonEditNoteScreen`, `CommonEditListScreen`, `CommonNotesScreen`, etc.) were created in `commonMain` while Android retained the full-featured, rich MVI screens in `composeApp/src/androidMain` (such as `EditScreen.kt` with 1,664 lines). This created divergent implementations and code duplication.

### Decision
Migrate **all** stateless UI screen composables, dialogs, cards, chips, and toolbars from `composeApp/src/androidMain` into `composeApp/src/commonMain`. Deprecate and completely delete the disconnected `Common*Screen.kt` stubs.

### Inventory of Unified Screens
| Feature Area | Composable Screen | Source in `androidMain` | Destination in `commonMain` | Replaces Stub |
| :--- | :--- | :--- | :--- | :--- |
| **Editor** | `EditNoteScreen` | `feature/editor/screens/EditScreen.kt` | `feature/editor/screens/EditNoteScreen.kt` | `CommonEditNoteScreen.kt` |
| **Editor** | `EditListScreen` | `feature/editor/screens/EditScreen.kt` | `feature/editor/screens/EditListScreen.kt` | `CommonEditListScreen.kt` |
| **Editor** | `EditTextPlainScreen` | `feature/editor/screens/EditTextPlainScreen.kt` | `feature/editor/screens/EditTextPlainScreen.kt` | *(new on desktop)* |
| **Editor** | `EditorDialogs` | `feature/editor/screens/EditorDialogs.kt` | `feature/editor/screens/EditorDialogs.kt` | *(new on desktop)* |
| **Notes / Main** | `NotesGridScreen` | `feature/main/routes/NotesGridScreen.kt` | `feature/main/routes/NotesGridScreen.kt` | `CommonNotesScreen.kt` |
| **Search** | `SearchScreen` | `feature/main/routes/search/SearchScreen.kt` | `feature/main/routes/search/SearchScreen.kt` | `CommonSearchScreen.kt` |
| **Labels** | `LabelsScreen` | `feature/labels/LabelsScreen.kt` | `feature/labels/LabelsScreen.kt` | `CommonLabelsScreen.kt` |
| **Labels** | `SelectLabelsScreen` | `feature/labels/SelectLabelsScreen.kt` | `feature/labels/SelectLabelsScreen.kt` | *(new on desktop)* |
| **Folders** | `ArchivedScreen` | `feature/main/routes/archived/ArchivedScreen.kt` | `feature/main/routes/archived/ArchivedScreen.kt` | `CommonArchivedScreen.kt` |
| **Folders** | `DeletedScreen` | `feature/main/routes/deleted/DeletedScreen.kt` | `feature/main/routes/deleted/DeletedScreen.kt` | `CommonDeletedScreen.kt` |
| **Settings** | `SettingsScreen` | `feature/settings/SettingsScreen.kt` | `feature/settings/SettingsScreen.kt` | `CommonSettingsScreen.kt` |
| **Reminders** | `RemindersScreen` | `feature/reminder/RemindersScreen.kt` | `feature/reminder/RemindersScreen.kt` | *(new on desktop)* |
| **Reminders** | `NoteRemindersScreen`| `feature/reminder/NoteRemindersScreen.kt` | `feature/reminder/NoteRemindersScreen.kt` | *(new on desktop)* |
| **Media** | `PlayAudioScreen` | `feature/media/audio/PlayAudioScreen.kt` | `feature/media/audio/PlayAudioScreen.kt` | *(new on desktop)* |
| **Media** | `RecordAudioScreen`| `feature/media/audio/RecordAudioScreen.kt`| `feature/media/audio/RecordAudioScreen.kt`| *(new on desktop)* |
| **Media** | `ViewImageScreen` | `feature/media/image/ViewImageScreen.kt` | `feature/media/image/ViewImageScreen.kt` | *(new on desktop)* |
| **Pick Note** | `PickNoteScreen` | `feature/main/PickNoteScreen.kt` | `feature/main/PickNoteScreen.kt` | *(new on desktop)* |

---

## 2. Resource Management: Compose Multiplatform Resources

### Context
Constitutional Principle I and AGENTS.md forbid hardcoded UI strings to guarantee localization across 30+ supported languages. Android historically accesses strings via `R.string.*` and drawables via `R.drawable.*`.

### Decision
Apply `alias(libs.plugins.compose.multiplatform)` in `composeApp/build.gradle.kts` and configure `compose.components.resources` in `commonMain.dependencies`.
Shared strings and drawables are organized in `composeApp/src/commonMain/composeResources/` or referenced via Compose Multiplatform's generated `Res.string.*` and `Res.drawable.*` accessors.

### Alternatives Evaluated
- *Expect/Actual String Resolvers*: Requires creating boilerplate wrapper functions for hundreds of string resource IDs. Rejected in favor of the official standard `Res.string` generator.
- *Hardcoded English Strings*: Violates NotallyX Constitution Principle I and localization quality gate. Rejected.

---

## 3. Platform Capabilities & Nuance Handling: State-Driven Flags

### Context
Certain routes contain platform-exclusive controls:
- **Settings**: Android exposes biometric app lock settings, notification permission requests, and battery optimization whitelisting. Desktop does not require notification runtime permissions or Android biometric prompts.
- **Reminders**: Android requires exact alarm permissions (`SCHEDULE_EXACT_ALARM`) and POST_NOTIFICATIONS permissions on Android 13+.
- **Media**: Desktop may play audio files via Java Sound or Media3 desktop bindings, but might not yet have a microphone recording implementation.

### Decision
Model platform capabilities directly as boolean properties in immutable `UiState`:
- `SettingsUiState.isBiometricsAvailable`: When `false`, the biometric toggle section is omitted from the layout.
- `SettingsUiState.isNotificationPermissionRequired`: When `false`, system notification settings row is hidden.
- `RemindersUiState.canScheduleExactAlarms`: When `false`, exact alarm permission warnings are suppressed.
- `MediaUiState.isAudioRecordingSupported`: When `false`, recording controls display an informative disabled state or are hidden.

### Rationale
- Keeps composables 100% pure, deterministic, and testable via Compose Previews and automated tests.
- Eliminates messy conditional platform checks (`if (isAndroid)`) inside the UI layout hierarchy.
- Allows ViewModels or Route coordinators to calculate capabilities once upon initialization.

---

## 4. Navigation Coordination: Shared Navigation 3 & ListDetailScene

### Context
Navigation 3 primitives (`NotallyNavKey`, `rememberNavBackStack`, `NavDisplay`, `ListDetailScene`) were already migrated to `composeApp/src/commonMain/kotlin/com/philkes/notallyx/navigation/`. However, `desktopApp` still used an ad-hoc `DesktopAppNavHost` with manual state toggling.

### Decision
Unify navigation architecture by using Jetpack Navigation 3 in `commonMain`:
1. Use `NotallyNavKey` as the single source of truth for all destinations on Android and Desktop.
2. In `composeApp/commonMain`, provide `NotallyNavDisplay` hosting shared `<Feature>Route` coordinators.
3. On Desktop wide windows (and Android foldables/tablets), leverage `ListDetailScene` to automatically present two-pane layouts (Notes Overview on left, Note Editor on right).
4. Desktop window container (`desktopApp/Main.kt`) delegates its main content directly to the shared `NotallyApp` / `NotallyNavDisplay`.

---

## 5. MVI Feature Quad Enforcement & Separation of Concerns

### Structure
Every feature follows the strict NotallyX 4-File Feature Quad:
1. `<Feature>Model.kt` (`commonMain`): `UiState`, `Action`, `SideEffect`, `DialogState`.
2. `<Feature>ViewModel.kt` (`commonMain`): State holder using Coroutines `StateFlow` and Koin injection.
3. `<Feature>Screen.kt` (`commonMain`): Stateless Composable rendering `UiState`, dispatching via single `onAction: (Action) -> Unit`.
4. `<Feature>Route.kt` (`commonMain` or platform route): Injects ViewModel, collects state with lifecycle, passes platform delegates (e.g. `ShareHandler`, `FilePicker`).

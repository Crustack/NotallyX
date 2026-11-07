# Feature Specification: Unified Cross-Platform UI Composables (KMP)

**Feature Branch**: `005-unify-ui-composables`

**Created**: 2026-11-05

**Status**: Draft

**Input**: User description: "after @004-kmp-desktop-target did the basic migration to kotlin multiplatform, I now want to ensure desktop and android app use the same UI components(composables wherever possible. Currently e.g. Desktop uses CommonEditNoteScreen for the edit note route, but android has its own EditScreen with its own implementation for the UI. Android an Desktop should leverage the same base composables, for every route and only where major differences are they should have their own composables. Most UI composables should live in composeApp commonMain, so that desktop and android use them"

## Clarifications

### Session 2026-11-05

- Q: Which routes should have their base UI composables reside in composeApp commonMain? → A: Every route in NotallyX (including Settings, Reminders, Media, Labels, Note/List Editors, Search, and Pick Note) must have its base composable live in composeApp commonMain.
- Q: How should the shared Settings and Reminders composables handle features that only exist on specific platforms (such as mobile biometric locks, system notification permissions, and desktop file export)? → A: State-driven capability flags: Shared screens conditionally render platform-specific rows/controls based on boolean flags in UiState (e.g., hiding biometric options if unsupported on desktop), keeping the composables completely shared, pure, and testable.
- Q: How should localized string and icon resources be accessed within the shared composables in composeApp/commonMain? → A: Compose Multiplatform Resources: Utilize Compose Multiplatform's standard resource generator (Res.string.*, Res.drawable.*), making string resources directly accessible in commonMain across all platforms.
- Q: Should Desktop migrate to the shared Jetpack Navigation 3 backstack architecture alongside Android, or retain a dedicated desktop navigation shell that directly embeds the shared screen composables? → A: Shared Navigation 3 & Route Coordinators: Both Android and Desktop use shared NotallyNavKey entries, NavDisplay, and adaptive ListDetailScene in commonMain, sharing route coordinator logic and backstack handling.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consistent Note and Checklist Editing Experience Across Platforms (Priority: P1)

As a user creating or modifying notes and checklists on either Android or Desktop, I want both platforms to use the same editing UI, layouts, formatting controls, and item interactions, so that switching between my phone and computer provides an identical and familiar experience without missing capabilities or visual inconsistencies.

**Why this priority**: Note and checklist editing is the primary creative activity in NotallyX. Having divergent editing screens creates maintenance overhead and leaves desktop users with a degraded, simplified editor compared to Android. Unifying the editor UI composables ensures full feature parity and single-source UI maintenance.

**Independent Test**: Can be tested by opening an identical note and checklist on both Android and Desktop, verifying that note title, body text formatting, checklist item checkboxes, reordering affordances, label chips, reminder chips, and color themes render with the same visual hierarchy and layout structure.

**Acceptance Scenarios**:

1. **Given** a text note opened on Android or Desktop, **When** the user views the screen, **Then** the note editor renders using shared base composables displaying title, body, date header, chips, and top/action bars consistently on both platforms.
2. **Given** a checklist note opened on Android or Desktop, **When** the user toggles item completion, adds items, or deletes items, **Then** identical item row composables and strike-through animations are presented and trigger state updates predictably.
3. **Given** note styling or color changes on either platform, **When** the user changes the note color palette, **Then** the editor background and accent styling update using the shared note color themes.

---

### User Story 2 - Shared Note Overview and Organization Views (Priority: P2)

As a user browsing my notes, archived items, trash, or searching for specific tags and keywords, I want the overview grids, search screens, and label management views to look and behave consistently across Android and Desktop, so that my organization workflows remain uniform across devices.

**Why this priority**: Note discovery, searching, and categorization constitute the primary navigation workflow. Reusing the core note grid/list composables, search result cards, and label management dialogs eliminates redundant screen implementations and guarantees visual consistency.

**Independent Test**: Can be tested by navigating to the main notes grid, archived folder, deleted folder, and label manager on both Desktop and Android, verifying that note cards, empty states, search result representations, and label dialogs display identical layouts and information density.

**Acceptance Scenarios**:

1. **Given** the main notes view on either platform, **When** notes and checklists are listed, **Then** they are displayed using shared note card composables that faithfully render titles, content previews, label chips, and media indicators.
2. **Given** the search screen on either platform, **When** the user enters a search query, **Then** search results, highlight matches, and folder filter chips render using the shared search screen composables.
3. **Given** the label management screen or dialog, **When** the user creates, renames, or deletes labels, **Then** the same label list composables and edit dialogs are presented on both platforms.

---

### User Story 3 - Shared Settings and Reminders Management Across Platforms (Priority: P2)

As a user configuring app preferences, theme options, security preferences, or reviewing scheduled reminders on Android or Desktop, I want the Settings and Reminders screens to share the same layout, sections, toggle components, and time picker dialogs across platforms.

**Why this priority**: Settings and Reminders are critical system routes. Sharing their composables ensures changes to preferences or reminder interfaces automatically propagate to all platforms while keeping platform-specific setting entries cleanly managed.

**Independent Test**: Can be tested by navigating to the Settings and Reminders screens on Android and Desktop, verifying that preferences groups, switches, theme selectors, reminder cards, and time pickers render with identical layouts and typography.

**Acceptance Scenarios**:

1. **Given** the Settings route opened on Android or Desktop, **When** the user navigates preferences sections, **Then** preference rows, headers, radio options, and switches render using the shared Settings composables.
2. **Given** the Reminders overview or note reminder configuration, **When** viewing or setting reminders, **Then** shared reminder cards, time chips, and scheduler dialogs display identically across platforms.

---

### User Story 4 - Shared Dialogs, Modals, and Action Sheets (Priority: P3)

As a user invoking contextual actions such as color selection, confirmation prompts, reminder configurations, or sorting preferences, I want these dialogs and selectors to share the same design language and interaction rules across Android and Desktop.

**Why this priority**: Secondary user interactions (color pickers, deletion confirmations, date/time pickers) often suffer from drift when implemented separately. Unifying them into shared composables guarantees consistent UX policies.

**Independent Test**: Can be tested by opening color picker dialogs, deletion confirmations, and export/sort dialogs on both platforms, confirming that layout, choices, and button arrangements behave identically.

**Acceptance Scenarios**:

1. **Given** any screen where a confirmation dialog is triggered (e.g., permanent deletion), **When** the dialog appears, **Then** the shared confirmation dialog composable renders with matching typography, buttons, and accessibility tags.
2. **Given** the note color picker, **When** invoked on Android or Desktop, **Then** the palette choices and selection indicator render identically using shared color picker composables.

---

### User Story 5 - Graceful Form Factor & Platform Adaptation (Priority: P4)

As a desktop user with a mouse, keyboard, and wide screen (or a mobile user with a touch screen and compact display), I want the shared UI composables to adapt smoothly to my platform's primary input methods and screen dimensions without breaking shared layouts.

**Why this priority**: Shared UI must feel natural on both touch-first mobile devices and pointer/keyboard-first desktop environments. Responsive layouts, shared Navigation 3 adaptive two-pane navigation (`ListDetailScene`), and platform-specific chrome (e.g. window borders vs system status bars) ensure high usability across form factors.

**Independent Test**: Can be tested on Desktop by resizing the window from narrow (phone-like) to wide (desktop monitor) and using keyboard shortcuts and mouse clicks, verifying that layouts adapt responsively and controls remain easily accessible.

**Acceptance Scenarios**:

1. **Given** a desktop window resized to wide aspect ratios, **When** viewing notes or editors, **Then** Navigation 3's adaptive `ListDetailScene` presents notes list and note editor in a responsive two-pane split layout without UI clipping.
2. **Given** desktop pointer hover interactions, **When** hovering over note cards or interactive action items, **Then** appropriate hover states and tooltips display where relevant while preserving touch targets on mobile.

---

### Edge Cases

- **Platform-Specific Capabilities**: How does the shared UI handle features that exist only on mobile (e.g., camera capture, biometric lock) or desktop (e.g., native window menu bars, file drag-and-drop)?
  - Shared composables render rows and options conditionally based on capability flags in `UiState` (e.g., `isBiometricsAvailable = false` hides the biometric preference item on desktop), while platform-specific side effects or intents are handled by platform route coordinators.
- **Physical Keyboard vs Virtual Keyboard (IME)**: How does the editor layout behave with desktop physical keyboard versus mobile virtual keyboard with screen insets?
  - Shared editor layouts must not hardcode mobile IME padding assumptions; layout scrolling and insets must adjust adaptively based on the active platform environment.
- **Varying Screen Densities and Font Scaling**: How are shared typography and dimensions preserved across diverse monitor DPIs and phone screen scales?
  - Density-independent units (dp/sp) and Material 3 design tokens are shared across common layouts, ensuring consistent visual proportions.
- **Image and Media Attachments**: How do images and attachments render across platforms?
  - Image preview components utilize multiplatform image loading capabilities with consistent aspect ratios and placeholder states.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST locate all shared stateless UI composables (screens, cards, chips, dialogs, and top app bars) in the shared multiplatform presentation layer (`composeApp/commonMain`).
- **FR-002**: Every route in the application (including Note Editor, Checklist Editor, Plain Text Editor, Notes Overview, Search, Archived, Deleted, Labels, Settings, Reminders, Media Player/Viewer, and Pick Note) MUST have its stateless base composable implementation located in `composeApp/commonMain` and shared between Android and Desktop.
- **FR-003**: The system MUST deprecate and eliminate disconnected duplicate screen implementations (such as separate divergent `CommonEditNoteScreen` and `EditScreen` UI implementations).
- **FR-004**: Shared screen composables MUST be stateless, accepting an immutable `UiState` and emitting user intents through a unified `onAction: (Action) -> Unit` event sink adhering to Unidirectional Data Flow (MVI).
- **FR-005**: Shared UI components MUST NOT directly instantiate or depend on platform-specific framework classes (such as Android `Activity`, `Context`, `Intent`, or `Bundle`).
- **FR-006**: Both Android and Desktop MUST utilize shared Jetpack Navigation 3 backstack management (`NavDisplay`, `NotallyNavKey`, `rememberNavBackStack`) and shared `<Feature>Route` coordinators in `composeApp/commonMain`, leveraging `ListDetailScene` for adaptive two-pane navigation on wide desktop displays.
- **FR-007**: Where genuine platform differences exist (e.g., mobile biometric app locking, notification permissions, battery optimizations, or desktop file exports), shared composables MUST conditionally render or adapt controls based on capability flags and properties within immutable `UiState`, preserving a single pure composable layout across platforms without code duplication or fragmented slot parameters.
- **FR-008**: Shared UI components MUST support consistent theming, including dynamic color schemes, dark mode, super dark mode, and custom note background color palettes across both platforms.
- **FR-009**: All user-visible text, labels, and icon assets in shared composables MUST use Compose Multiplatform Resources (`Res.string.*`, `Res.drawable.*`) to preserve full multi-language localization across 30+ supported languages and consistent iconography on both Android and Desktop.
- **FR-010**: Shared composables MUST declare explicit test tags and accessibility semantics so automated UI and visual regression tests can target elements consistently across platforms.

### Key Entities *(include if feature involves data)*

- **NoteUiState**: Represents the immutable presentation state for viewing or editing a note, including title, body text, color palette, timestamps, label tags, reminder states, attachments, and active dialog flags.
- **ChecklistUiState**: Represents the immutable presentation state for checklist editing, including checklist items (text, completed status, order index), color, tags, and item action modes.
- **NotesOverviewUiState**: Represents the list or grid presentation state of notes, folders, active filters, search keywords, selected items for batch actions, and view modes.
- **SettingsUiState**: Represents the presentation state of application preferences, theme selections, security/lock configuration, and backup states, including capability flags (e.g., `isBiometricsAvailable`, `isNotificationPermissionRequired`) that instruct shared composables which platform options to render.
- **RemindersUiState**: Represents the presentation state of scheduled notes, reminder timestamps, and reminder recurrence configurations.
- **Action Intent**: Sealed hierarchy of user-initiated events dispatched from UI components to state coordinators (e.g., `TitleChanged`, `ContentChanged`, `ColorSelected`, `DeleteRequested`, `ItemToggled`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of application routes (Notes Overview, Note Editor, Checklist Editor, Search, Labels, Archive, Trash, Settings, Reminders, Media, and Pick Note) utilize shared composables across both Android and Desktop targets.
- **SC-002**: Divergent duplicate screen implementations (e.g. divergent `CommonEditNoteScreen` and `EditScreen` codebases) are reduced to 0 across the project.
- **SC-003**: Cross-platform visual and interactive parity is achieved, with automated visual regression tests and UI preview tests validating consistent rendering across supported form factors.
- **SC-004**: Maintenance overhead for UI bug fixes and visual updates is halved, as styling and layout adjustments to shared composables immediately reflect on both Android and Desktop.
- **SC-005**: All shared UI components render and respond to user interactions smoothly without perceptible lag or frame drops during editing, scrolling, or theme switching.

## Assumptions

- Both Android and Desktop targets compile against Compose Multiplatform and Jetpack Compose Material 3 with unified UI dependencies.
- Multiplatform resources (strings, drawables, fonts) are supported via Compose Multiplatform resource generation (`compose.components.resources`), making all translations and vector assets accessible directly in `commonMain`.
- Business logic and state management remain encapsulated in ViewModels and MVI models residing in shared multiplatform modules, leaving UI composables purely responsible for rendering state and forwarding actions.
- Platform-specific operations (such as audio recording hardware access, biometric prompt verification, Android system intents, and desktop window management) remain encapsulated at the route layer or via platform-specific services passed into shared composable slots.
- Form factor adaptations (e.g., adaptive grid column counts, desktop content max-width constraints) are resolved responsively through standard Compose layout modifiers and window size classes rather than duplicating screen composables.

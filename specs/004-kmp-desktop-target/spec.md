# Feature Specification: Desktop Application Target (Kotlin Multiplatform)

**Feature Branch**: `004-kmp-desktop-target`

**Created**: 2026-11-04

**Status**: Draft

**Input**: User description: "Setup Desktop target for kotlin multiplatform. A first milestone is just a plain example, runnable desktop target with an example screen. Eventually, we want to include all routes the android app has as well. Stuff like biometric lock, notifications and home screen widgets are not scope of the implementation yet. The most important ones are notes overview, edit note/list routes. It should use as much shared code as possible with the android app. We do not care for support for iOS or other targets. The desktop app should be able to use exactly the same database format and use the same libraries (room)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Desktop Application Launch and Baseline Screen (Milestone 1) (Priority: P1)

As a desktop user, I want to launch the NotallyX desktop application on my computer and view a clean, responsive starter screen confirming that the application runs locally, so that I can verify the desktop environment is operational before interacting with full note features.

**Why this priority**: Establishing a runnable desktop target with an interactive baseline screen is the foundational milestone. It proves desktop execution, window lifecycle management, and UI rendering work correctly before connecting complex data persistence and feature navigation.

**Independent Test**: Can be independently tested by starting the desktop application from the command line or desktop launcher, verifying that a desktop window opens within 2 seconds, renders the example verification screen, responds to window resizing, and exits cleanly when closed.

**Acceptance Scenarios**:

1. **Given** a supported desktop operating system environment, **When** the user launches the desktop application, **Then** a desktop window appears with appropriate initial dimensions, displaying an interactive example screen with welcome and status information.
2. **Given** the active desktop application window, **When** the user resizes, maximizes, or restores the window, **Then** the screen layout adapts responsively without UI clipping or rendering artifacts.
3. **Given** the running desktop application, **When** the user triggers the window close action or standard exit command, **Then** the application closes promptly without hanging or background orphan processes.

---

### User Story 2 - Primary Note and Checklist Management (Milestone 2) (Priority: P2)

As a desktop user, I want to view my notes overview, create and edit text notes, and manage checklist/list notes directly on my desktop computer, so that I can capture ideas, manage to-do items, and organize information with the comfort of a physical keyboard and larger screen.

**Why this priority**: Notes overview and note/list editing constitute the core value proposition of NotallyX. Once the desktop target is established, these primary routes provide immediate utility to users.

**Independent Test**: Can be independently tested by launching the desktop application, creating a new text note with title and body, creating a new checklist note with multiple items, checking and reordering items, searching existing notes from the overview, and confirming all changes persist across application restarts.

**Acceptance Scenarios**:

1. **Given** the desktop application on the notes overview screen, **When** the user views the screen, **Then** existing notes and checklists are displayed in an organized list/grid layout with titles, content snippets, and modification dates.
2. **Given** the notes overview screen, **When** the user initiates note creation, **Then** a note editor route opens allowing immediate entry of note title and rich or plain text content.
3. **Given** a text note in the editor, **When** the user edits content and returns to the overview or saves, **Then** the updated note is immediately visible in the overview and saved to persistent storage.
4. **Given** the notes overview screen, **When** the user initiates checklist creation, **Then** a list editor route opens allowing entry of items, toggling completion checkboxes, and editing item descriptions.
5. **Given** a checklist note in the editor, **When** the user checks or unchecks an item, **Then** the state updates visually with strike-through styling and saves automatically.

---

### User Story 3 - Full Data Parity and Storage Compatibility (Priority: P2)

As a cross-platform user who uses NotallyX on both mobile and desktop devices, I want the desktop application to read and write the exact same database format as the mobile application, so that I can transfer my notes file or backup between devices without format conversion, data loss, or schema mismatch.

**Why this priority**: Users must never lose note content, metadata, or formatting when moving between mobile and desktop. Utilizing the identical underlying database format ensures zero migration friction and guarantees bi-directional compatibility.

**Independent Test**: Can be tested by copying an existing database file containing notes, checklists, labels, and formatting spans from the mobile application into the desktop data directory, launching the desktop application, verifying all notes appear identically, editing notes on desktop, and verifying the modified database opens seamlessly on mobile.

**Acceptance Scenarios**:

1. **Given** a database file created by the mobile application, **When** opened by the desktop application, **Then** all notes, checklists, labels, and metadata are read accurately without requiring schema conversion or migration errors.
2. **Given** notes and checklists edited or created on the desktop application, **When** the resulting database file is opened on the mobile application, **Then** all edits, new items, and timestamps appear accurately with complete integrity.
3. **Given** complex note features such as pinned notes, archived notes, trashed notes, and labels, **When** viewed on desktop, **Then** their states and relationships match the underlying shared schema exactly.

---

### User Story 4 - Comprehensive Route Parity with Mobile-Only Feature Exclusion (Milestone 3) (Priority: P3)

As a desktop user, I want access to all remaining standard routes available in the mobile application (search, labels management, archive, trash, settings, and import/export), while mobile-specific features that do not belong on desktop (biometric app lock, mobile system notifications, and home screen widgets) are cleanly excluded.

**Why this priority**: Delivering full route parity completes the desktop experience, providing parity with all relevant mobile screens while consciously omitting mobile-specific platform capabilities that are not applicable or scoped for desktop.

**Independent Test**: Can be tested by navigating through every shared route on desktop (search, labels, trash, archive, settings, import/export), verifying expected functionality, and confirming that no UI prompts, settings, or errors related to biometric hardware, mobile notification channels, or home screen widgets are presented.

**Acceptance Scenarios**:

1. **Given** the desktop application, **When** the user navigates through secondary routes (search, label management, archive, trash, and settings), **Then** each route functions with the same business logic and navigation flow as the mobile version.
2. **Given** the desktop settings and security configuration, **When** the user inspects available options, **Then** mobile-only options (biometric unlock, home screen widget settings, and mobile notification channels) are hidden or cleanly omitted from desktop menus.
3. **Given** the desktop application lifecycle, **When** background alarms or notification triggers occur in the domain layer, **Then** they do not attempt to invoke mobile-specific operating system notification managers or crash the desktop process.

---

### Edge Cases

- **Initial Run Without Existing Data**: What happens when the desktop application is launched for the very first time and no existing database exists? The system must automatically initialize an empty, valid database at the standard user storage path without errors.
- **Corrupted or Incompatible Local Database**: How does the desktop application handle an unreadable or corrupt database file? The application must report a clear user-facing error dialog and avoid unrecoverable crashes, offering to back up the damaged file or initialize a fresh database.
- **Abrupt Window Closure During Edits**: What happens if the user abruptly closes the desktop window while actively editing a note? All active text and checklist modifications must be automatically persisted or flushed to disk prior to window destruction to prevent data loss.
- **Simultaneous Database Access**: What happens if another process or instance opens the local database file? The application must handle file lock conditions gracefully and inform the user if the database is in use by another running instance.
- **Rapid Window Resizing and Ultra-Wide Displays**: How does the desktop UI react to extreme window aspect ratios or ultra-wide monitors? The layouts must enforce sensible maximum content widths and adapt responsively between single-pane and adaptive multi-pane views without stretching text lines unreadably.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a standalone desktop application executable for standard desktop environments (Linux, macOS, Windows).
- **FR-002**: For Milestone 1, the desktop application MUST launch into a responsive starter screen that verifies application bootstrap, UI framework initialization, and window management.
- **FR-003**: The desktop application MUST maximize code sharing with the mobile application by utilizing shared business logic, domain models, and data persistence modules.
- **FR-004**: The desktop application MUST use the identical database engine and on-disk storage schema as the mobile application, ensuring bi-directional database file compatibility.
- **FR-005**: For Milestone 2, the system MUST implement the Notes Overview route on desktop, displaying the user's active, pinned, and organized notes with search and filtering capabilities.
- **FR-006**: For Milestone 2, the system MUST implement the Text Note Editor route on desktop, enabling full creation, viewing, updating, and deletion of text notes.
- **FR-007**: For Milestone 2, the system MUST implement the Checklist Note Editor route on desktop, enabling creation, item addition, editing, reordering, checking, and unchecking of checklist items.
- **FR-008**: For Milestone 3, the desktop application MUST progressively support remaining shared routes, including Label Management, Search, Archive, Trash, Settings, and Import/Export.
- **FR-009**: The system MUST exclude mobile-only hardware and platform features on the desktop target, specifically omitting biometric app lock, mobile system notifications, and home screen widgets.
- **FR-010**: The desktop application MUST store user data and settings in standard user-scoped desktop operating system directories.
- **FR-011**: The system MUST automatically persist user edits to prevent data loss when navigating away from notes or when closing the application window.

### Key Entities *(include if feature involves data)*

- **Note**: Represents an individual note entity with title, text body, creation timestamp, last modified timestamp, color/theming attributes, pinned status, archived status, and trashed status.
- **Checklist Item**: Represents an actionable item within a checklist note, including text description, checked/completed state, and ordinal display position.
- **Label**: Represents a user-defined category or tag associated with one or more notes for filtering and organizational purposes.
- **User Preferences**: Represents application settings, including display theme (light/dark/system), sort order, view modes (grid vs list), and editor preferences.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The desktop application starts and presents an interactive user interface within 2.0 seconds on standard desktop hardware.
- **SC-002**: 100% of note content, formatting, checklist items, and labels created in the mobile app are readable and editable on the desktop application with zero data loss or corruption.
- **SC-003**: 100% of database files produced by the desktop application can be opened and edited on the mobile application without requiring migration or conversion steps.
- **SC-004**: More than 80% of core business logic, domain models, and presentation state code is shared between the desktop and mobile targets, avoiding redundant implementation.
- **SC-005**: 0% regression or disruption to existing mobile application functionality, performance, or packaging when introducing the desktop target.
- **SC-006**: 100% of mobile-specific dependencies (biometrics, mobile push/local notifications, home screen widgets) are decoupled from the desktop compilation target.

## Assumptions

- **Target Platforms**: The desktop application targets standard desktop operating systems (Linux, macOS, Windows) running on standard 64-bit JVM environments. Non-desktop platforms such as iOS, watchOS, or web are explicitly out of scope.
- **Shared Code Strategy**: The architecture reuses the existing shared domain, data, and presentation modules (`:shared` and `:composeApp`), adding a dedicated desktop application runner module (e.g., `:desktopApp`) to host the desktop window.
- **Storage Location**: The local database and configuration are stored in standard OS user application data directories (e.g., `~/.local/share/NotallyX/` on Linux, `~/Library/Application Support/NotallyX/` on macOS, and `%APPDATA%\NotallyX\` on Windows).
- **Phased Implementation**:
  - Milestone 1 focuses exclusively on proving a runnable desktop application with a minimal verification/example screen.
  - Milestone 2 introduces core note workflows: notes overview, text note editor, and checklist editor backed by the shared database.
  - Milestone 3 expands to full route parity (search, labels, archive, trash, settings, backup/restore) while excluding mobile-only capabilities.
- **Excluded Features**: Biometric app lock, mobile system notifications, and home screen widgets are intentionally omitted from desktop requirements. Application locking on desktop, if desired in the future, will be addressed as a separate desktop-specific feature.

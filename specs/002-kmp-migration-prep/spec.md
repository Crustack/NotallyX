# Feature Specification: Multiplatform Migration Prerequisites (Android & Desktop Readiness)

**Feature Branch**: `002-kmp-migration-prep`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "Prepare migration to kotlin multiplatform, see @kmp-migration for requirements and first analysis /home/user/AndroidStudioProjects/NotallyX/docs/kmp-migration/"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Seamless Note & Attachment Portability Across Mobile and Desktop (Priority: P1)

As a NotallyX user who wants to access, back up, and transfer my notes, checklists, labels, reminders, and media attachments between Android and Desktop (Linux, macOS, Windows), I need my notes database, preferences, and attachment references to be stored in a platform-independent format with zero data loss, even if I previously enabled database encryption on mobile.

**Why this priority**: Data portability and zero data loss are the foundational requirements for running NotallyX on both mobile and desktop. If note data, JSON-serialized metadata, attachment paths, or encrypted databases are locked to mobile-specific storage formats, users cannot open their backups or databases on desktop or upgrading mobile versions.

**Independent Test**: Can be fully tested by migrating existing user databases (v1 through v11, including previously encrypted databases and notes with spans, labels, reminders, images, audio, and file attachments), performing full backup/restore cycles, and verifying that all records open and round-trip identically in a desktop-compatible environment without mobile operating system services.

**Acceptance Scenarios**:

1. **Given** an existing user with notes containing formatted text spans, checklists, labels, reminders, and attachments, **When** the database is opened, read, or exported, **Then** all data serializes and deserializes identically without relying on mobile-only system libraries, and all attachment paths are stored as portable relative identifiers rather than device-specific content URIs.
2. **Given** an existing user who previously enabled biometric database encryption on mobile, **When** they launch the updated application and authenticate via biometric unlock, **Then** the application performs a one-time automatic decryption of the database, hides the retired database encryption toggle, keeps biometric screen lock active, and preserves 100% of notes, labels, and attachments.
3. **Given** a desktop environment on Linux, macOS, or Windows, **When** application storage paths are resolved for databases, backups, media attachments, and caches, **Then** all user data is placed in the standard operating system application data directory (`~/.local/share/NotallyX` or `$XDG_DATA_HOME/NotallyX` on Linux, `~/Library/Application Support/NotallyX/` on macOS, and `%APPDATA%\NotallyX\` on Windows).
4. **Given** user preferences and sensitive configuration settings, **When** accessed by application features, **Then** general preferences persist in a portable format across platforms while mobile hardware-backed encryption remains isolated to mobile devices.

---

### User Story 2 - Uninterrupted Core Note-Taking, Navigation & Localization on Any Target (Priority: P1)

As a user creating, editing, searching, and organizing notes and checklists in any of the 30 supported languages, I want all screens, dialogs, color pickers, navigation flows, and relative timestamps to function identically without depending on mobile-only UI or system resources.

**Why this priority**: Core note-taking, multi-pane navigation, and localization across 30 languages represent the primary user experience. Decoupling state holders, navigation state, and localized strings/icons from mobile resource identifiers is required before the user interface can run on desktop.

**Independent Test**: Can be independently verified by launching every primary user flow (note creation, rich text formatting, checklist editing, undo/redo history, label management, search, color selection, and multi-pane navigation) and running state management tests in a pure desktop-compatible environment across multiple locales.

**Acceptance Scenarios**:

1. **Given** any feature screen (note editor, checklist editor, notes overview, labels, reminders, settings), **When** the user interacts with the screen or navigates forward and backward across routes, **Then** screen state and navigation history serialize and restore cleanly without mobile reflection or platform context dependencies.
2. **Given** the application is set to any of the 30 supported locales, **When** UI screens, pluralized messages, relative dates (e.g., "2 hours ago"), or action notifications are displayed, **Then** all user-visible text and icons resolve accurately from shared multiplatform resources with zero missing translations or hardcoded strings.
3. **Given** low-level UI components (note cards, editor formatting bars, color picker dialogs), **When** rendered in list, grid, or adaptive two-pane layouts, **Then** all components render and respond to user input without requiring a mobile activity context.

---

### User Story 3 - Cross-Platform Import, Export, Sharing & Media Interactions (Priority: P2)

As a user importing notes from other apps, exporting notes (as PDF, HTML, Markdown, TXT, JSON, or ZIP backups), or working with image and audio attachments, I want file selection, document export, sharing, and audio playback/recording to work through clean platform-adapted workflows on both mobile and desktop.

**Why this priority**: Import, export, backups, and media attachments are vital workflows that touch the host operating system's file dialogs, document renderers, and audio subsystems. Abstracting these boundaries ensures mobile users experience zero regression while enabling native desktop file choosers and media handlers.

**Independent Test**: Can be tested independently by executing note imports (Google Keep, Evernote, Quillpad, JSON, Markdown, HTML, plain text), exporting notes to all supported formats including PDF and ZIP backup, selecting files via the file picker abstraction, and playing/recording audio attachments.

**Acceptance Scenarios**:

1. **Given** a user importing backups or attaching files/images to a note, **When** they trigger file or folder selection, **Then** the application delegates to the platform's native file chooser (system document picker on mobile, native desktop file dialog on desktop) and processes the selected files through a unified storage interface.
2. **Given** a user exporting a note to PDF, **When** the export is initiated, **Then** the application generates shared HTML content and delegates PDF conversion to the platform PDF exporter without exposing mobile print listeners to feature state holders.
3. **Given** a note with audio attachments, **When** the user plays or records audio, **Then** playback and recording operate through platform-independent media contracts, gracefully adapting if recording hardware or codec support is unavailable on a given platform.
4. **Given** a user sharing a note, exporting a crash report, or triggering an action that requires device permissions on mobile, **When** the action is invoked, **Then** permission verification and external sharing are handled via platform abstractions without leaking mobile intent or package manager logic into shared feature logic.

---

### User Story 4 - Clean Encapsulation of Mobile-Only System Integrations (Priority: P3)

As an Android user, I want my existing mobile-specific conveniences—biometric/PIN app lock, home screen widgets, status bar pinned notifications, scheduled reminder alarms, and background auto-backups—to continue working exactly as before, while allowing the shared application core to run on desktop where those mobile OS services do not exist.

**Why this priority**: Mobile OS features provide high value on Android but rely on hardware or OS daemons absent on desktop. Encapsulating them behind clean scheduling and security boundaries prevents them from blocking desktop execution while preserving 100% of Android functionality.

**Independent Test**: Can be tested by verifying that scheduled reminders, pinned notifications, periodic auto-backups, trash auto-removal, attachment cleanup, home screen widgets, and biometric lock function on Android, while the shared application logic executes with safe no-op schedulers when mobile services are absent.

**Acceptance Scenarios**:

1. **Given** a user creating or editing note reminders on any platform, **When** reminder metadata is saved, **Then** the reminder is persisted in the note record and scheduled via the platform reminder scheduler (triggering system alarms/notifications on mobile and safely operating as a no-op on desktop without errors).
2. **Given** configured background maintenance tasks (periodic ZIP auto-backup, automatic removal of trashed notes after configured days, and missing attachment cleanup), **When** triggered, **Then** the core backup, purge, and cleanup logic executes independently of the mobile background worker framework.
3. **Given** a user with biometric/PIN lock enabled on mobile, **When** they open the app on mobile, **Then** the lock screen protects access as before, whereas on desktop the mobile lock activity is bypassed without breaking preference storage.

---

### Edge Cases

- **Legacy Encrypted Database Upgrade Interrupted or Canceled**: If a user with a legacy encrypted database dismisses or fails the biometric prompt on startup, the database remains safely untouched and encrypted until biometric authentication succeeds; partial or corrupted decryption states are prevented via atomic file replacement after a write-ahead log (WAL) checkpoint.
- **Malformed or Legacy JSON in Database Columns**: When converting legacy note spans, reminders, labels, or attachment lists stored in database columns, malformed or missing fields must be repaired or defaulted identically to existing repair behavior without data loss.
- **Platform Missing Audio Recording Capability**: When running on a desktop environment without an active microphone or compatible AAC encoder, the audio recording capability reports as unavailable and the UI gracefully disables or hides recording without crashing.
- **Regional Locale Codes in Multiplatform Resources**: Languages with regional variants (such as Brazilian Portuguese `pt-rBR`, Simplified/Traditional Chinese, etc.) across the 30 supported locales must map accurately to multiplatform resource directory qualifiers so no locale falls back to English unexpectedly.
- **External Public Folder Mode on Mobile**: Users on Android who enabled the legacy "store data in public folder" setting must continue to have their database and attachments resolved accurately via the path provider abstraction.
- **Deep Navigation Back Stack Serialization**: When saving and restoring navigation state with multiple nested routes and arguments on non-mobile targets, all route keys must serialize polymorphically without runtime reflection failures.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The application MUST decouple all database creation, schema migrations, DAO operations, and search result models from mobile context objects, accepting abstract directory paths and database driver factories instead.
- **FR-002**: All domain models, model extensions, and feature state holders MUST represent file and attachment references using platform-agnostic paths or opaque handles, with zero direct usage of mobile URI classes (`android.net.Uri`) or mobile parcelable annotations.
- **FR-003**: All feature state holders (ViewModels) destined for shared presentation MUST be platform-independent, containing zero references to mobile `Application`, `Context`, `View`, `Intent`, `PackageManager`, `Manifest`, print adapters, or mobile resource IDs (`R.*`).
- **FR-004**: Database type converters and model serialization MUST use portable JSON serialization that produces and parses 100% byte-compatible JSON for all existing database records (v1–v11) and backup archives without relying on mobile-bundled `org.json`, `Base64`, `Html`, or `MimeTypeMap` utilities.
- **FR-005**: Navigation state management MUST explicitly register all navigation route keys for polymorphic serialization so that back stacks can be saved and restored on both mobile and desktop targets without reflection.
- **FR-006**: File system reads/writes and user-initiated file/directory selection MUST be separated into a low-level storage interface (`FileStorage`) in the domain/data layer and a UI file selection interface (`FilePicker`) in the presentation layer.
- **FR-007**: Directory resolution for databases, backups, media, and caches MUST be abstracted behind a `PathProvider` that maps to existing app/public folders on Android and to standard OS application data directories on Desktop (`~/.local/share/NotallyX` respecting `$XDG_DATA_HOME` on Linux, `~/Library/Application Support/NotallyX` on macOS, `%APPDATA%\NotallyX` on Windows).
- **FR-008**: PDF note export MUST separate shared HTML generation from platform PDF rendering via an asynchronous `PdfExporter` interface, isolating mobile WebView and print adapter mechanics to the mobile platform layer.
- **FR-009**: All user-facing strings, plurals, and shared vector icons across all 30 supported locales MUST be migrated to multiplatform resources, and state holders MUST emit platform-agnostic text descriptors rather than integer resource IDs, while keeping mobile-only assets (launcher icons, widget XMLs, notification icons) in the mobile resource directory.
- **FR-010**: Low-level UI components (including note cards, editor screens, and color picker dialogs) and date/reminder formatters MUST operate without mobile context dependencies while preserving localized relative time formatting across all supported locales.
- **FR-011**: Database encryption MUST be disabled via a global feature flag (`FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`), the corresponding toggle in Settings MUST be hidden, and any existing encrypted user database MUST be automatically decrypted on startup (following biometric authentication, WAL checkpoint, and atomic file swap) with zero data loss.
- **FR-012**: Sensitive preferences storage MUST be abstracted behind a `SecurePreferencesStore` interface so hardware-backed encryption remains isolated to the mobile layer while standard preferences operate portably across platforms.
- **FR-013**: Note reminder scheduling (`ReminderScheduler`), periodic background tasks (`TaskScheduler` for auto-backup, trash auto-deletion, and attachment cleanup), audio playback/recording (`AudioPlayer`, `AudioRecorder`), external sharing (`ShareHandler`), and runtime permission checks (`PermissionChecker`) MUST be abstracted behind platform interfaces with full mobile implementations and safe no-op or desktop-compatible implementations.
- **FR-014**: Automated unit tests for domain models, data converters, database operations, and feature state holders MUST be separated from mobile framework test runners so they execute as fast, pure JVM unit tests, and build configurations MUST separate pure cross-platform settings from mobile packaging settings.

### Key Entities

- **Portable Note & Attachment Model**: The core domain representation of notes (`BaseNote`), labels (`Label`), checklists, reminders (`Reminder`), and attachments (`Attachment`, `Audio`, `FileAttachment`) using relative path identifiers and portable serialization.
- **Platform Service Contracts**: The boundary interfaces decoupling shared business and UI logic from the host OS:
  - *Data & Domain Contracts*: `PathProvider`, `FileStorage`, `DatabaseDriverFactory`, `SecurePreferencesStore`, `TaskScheduler`, `ReminderScheduler`, `AudioPlayer`, `AudioRecorder`, `PdfExporter`, `Logger`.
  - *Presentation Contracts*: `FilePicker`, `ShareHandler`, `ExternalLauncher`, `PermissionChecker`, `UiText`.
- **Database Decryption Migration State**: The one-time transition workflow that detects a legacy encrypted SQLite file on startup, authenticates the user, flushes the write-ahead log, decrypts to a standard SQLite file, atomically swaps the database file, and clears the encryption preference flag.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of domain models, database converters, DAOs, and shared feature state holders contain zero mobile-only framework imports (`android.*`), verified by automated static checks and pure JVM compilation.
- **SC-002**: 100% of existing database schema versions (v1 through v11) and legacy encrypted user databases migrate and open without data loss, verified by automated migration and decryption round-trip tests preserving 100% of notes, labels, reminders, and attachments.
- **SC-003**: All 30 supported locales render strings, plurals, and relative timestamps accurately with zero regressions in localization preview and visual verification checks.
- **SC-004**: 100% of domain, data converter, and feature state holder unit tests execute in pure JVM mode without requiring a mobile simulation runner, while 100% of existing Android instrumented UI tests and visual screenshot tests continue to pass.
- **SC-005**: All 13 readiness gate criteria defined in the KMP Migration Readiness Checklist (`CH-01` through `CH-13`) are satisfied, leaving the repository ready for multi-module extraction (`shared`, `composeApp`, `androidApp`, `desktopApp`).

## Assumptions

- All prerequisite refactorings in this specification are executed incrementally within the existing application structure before physically splitting code into separate `shared`, `composeApp`, `androidApp`, and `desktopApp` modules.
- The target platforms for the multiplatform architecture are exclusively **Android** and **Desktop (JVM: Linux, macOS, Windows)**; non-JVM targets (iOS, Web/Wasm) are out of scope.
- Because both target platforms run on the JVM, standard JDK utilities (`java.io`, `java.nio`, `java.time`, `java.util`) and pure JVM libraries (`jsoup`, `commonmark`, `zip4j`, `simple-xml`, `PrettyTime`) remain valid in shared code unless explicitly replaced.
- Biometric/PIN app lock UI, home screen widgets, system alarm notifications, and OS background work schedulers remain Android-only features; on Desktop, reminder and background task interfaces initially bind to safe no-op implementations while preserving all reminder data in notes.
- Existing translation automation workflows will be adapted to support the multiplatform resource directory layout so translators and maintainers retain a working localization pipeline.

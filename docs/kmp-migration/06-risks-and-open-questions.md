# 06. Technical Risks, Uncertainties & Open Questions

## Overview

This document highlights technical risks, potential breaking changes, desktop-specific considerations, and resolved decisions prior to beginning the KMP migration.

---

## Technical Risk Matrix

| Risk ID | Risk Title | Category | Impact | Likelihood | Mitigation Strategy |
| :--- | :--- | :--- | :---: | :---: | :--- |
| **RISK-01** | SAF Path Incompatibility | File System | **Medium** | **High** | Store relative attachment paths in database rather than full SAF `content://` URIs. |
| **RISK-02** | Swing UI Main Dispatcher in Desktop ViewModels | Coroutines | **Medium** | **Low** | Include `kotlinx-coroutines-swing` in `desktopApp` so `viewModelScope` dispatches to Swing UI thread correctly. |
| **RISK-03** | Room 3 Native SQLite Packaging Across OS Architectures | Build Tooling | **Medium** | **Low** | Configure Compose Multiplatform Gradle plugin packaging options in `desktopApp` for `BundledSQLiteDriver`. |
| **RISK-04** | KSP Code Generation for KMP Room 3 | Build Tooling | **Medium** | **Low** | Use latest KSP version (`2.3.12`+) configured with Room KMP code generation flags. |

> [!NOTE]
> **Eliminated Desktop Risks**: Because Biometric/PIN Lock, Database/Preference Encryption (SQLCipher/Tink), Notifications/Alarms, Glance Widgets, and WorkManager Auto-Backups are explicitly designated as **Android-Only Features**, all desktop risks associated with encryption drivers, task schedulers, or native OS biometrics are eliminated!

---

## Data Migration & Persistence Integrity

1. **Database File Format Consistency**:
   - NotallyX relies on Room WAL mode (`pragma wal_checkpoint(TRUNCATE)`). When syncing or copying SQLite files between Android and Desktop, WAL files (`-wal`, `-shm`) must be flushed prior to file access.
2. **DataStore Preferences JSON Compatibility**:
   - `UserPreferences` in NotallyX uses Kotlinx Serialization JSON. Desktop and Android must maintain identical `@Serializable` data definitions to ensure settings files can be shared or backed up seamlessly.

---

## Desktop-Specific User Experience (UX) Considerations

```
Desktop Window Considerations
   ├── Window Management: Resizable main window, min width 600dp, min height 400dp
   ├── Application Data Directory: Standard OS data directory (~/.local/share/NotallyX, etc.)
   ├── Context Menus: Mouse right-click support on note cards
   ├── Keyboard Shortcuts: Ctrl+N (New Note), Ctrl+F (Search), Ctrl+S (Save), Esc (Back)
   └── Drag-and-Drop: OS file drag-and-drop into note editor attachments
```

---

## Resolved Architectural Decisions

1. **ViewModel Layer Placement**:
   - *Decision*: ViewModels reside strictly in `:composeApp` (Presentation Layer) alongside UI screens and Navigation 3 routes, keeping `:shared` strictly focused on Domain and Data layers.
2. **Database Encryption & Biometrics Policy**:
   - *Decision*: Database encryption is disabled across all platforms via `const val FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`. Biometrics and PIN lock are Android-only UI components in `androidApp`.
3. **File Chooser & FilePicker Ownership**:
   - *Decision*: An explicit `FilePicker` interface is defined in `:composeApp/commonMain`. `composeApp/androidMain` handles Activity Result Launchers / SAF, while `composeApp/desktopMain` handles Swing `JFileChooser` / AWT `FileDialog`.
4. **Initialization & Startup Pattern**:
   - *Decision*: Executable modules (`androidApp`, `desktopApp`) perform platform startup/DI initialization and hand control directly to `:composeApp`.
5. **Desktop Application Data Directory Location**:
   - *Decision*: On Desktop JVM, user data resides in standard OS application data directories:
     - **Linux**: `~/.local/share/NotallyX/`
     - **macOS**: `~/Library/Application Support/NotallyX/`
     - **Windows**: `%APPDATA%\NotallyX\`

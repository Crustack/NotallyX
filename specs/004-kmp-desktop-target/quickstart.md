# Quickstart Validation Guide: Desktop Target for Kotlin Multiplatform

**Feature**: `004-kmp-desktop-target` ([spec.md](spec.md))  
**Date**: 2026-11-04  
**Status**: Ready for Task Generation (`/speckit-tasks`)  

---

## 1. Overview

This guide provides runnable validation scenarios and verification commands to prove that the **Desktop Target (Kotlin Multiplatform)** has been correctly established in NotallyX across its three delivery milestones:
- **Milestone 1**: Minimal runnable desktop application with an interactive verification/starter screen.
- **Milestone 2**: Core note and checklist management backed by the shared Room database.
- **Milestone 3**: Comprehensive route parity with clean isolation of mobile-only features.

---

## 2. Automated Verification Scenarios

### Scenario A: Desktop Target Build & Compilation (`FR-001`, `FR-002`, `SC-001`)
Verify that `:desktopApp`, `:composeApp`, and `:shared` compile with the `desktop` JVM target enabled:

```bash
# Compile shared and presentation desktop targets
./gradlew :shared:compileKotlinDesktop :composeApp:compileKotlinDesktop

# Assemble desktop application
./gradlew :desktopApp:assemble
```

**Expected Outcome**:
- All desktop source sets compile cleanly to JVM bytecode targeting JVM 17.
- Zero compilation errors or unresolved dependencies.

---

### Scenario B: Pure-JVM Database & Migration Verification (`FR-004`, `SC-002`, `SC-003`)
Verify that Room 3 runs on desktop JVM with `BundledSQLiteDriver`, passes all v1 through v11 database migrations, and supports checkpointing:

```bash
./gradlew :shared:testAndroidHostTest --tests "com.philkes.notallyx.core.database.NotallyDatabaseJvmTest"
```

**Expected Outcome**:
- In-memory and on-disk Room 3 database instances initialize and ping successfully on pure JVM.
- All migrations (v1 → v11) execute deterministically with complete schema validation.
- `checkpoint()` flushes WAL data cleanly.

---

### Scenario C: Static Desktop Source Set Audit (`FR-003`, `FR-009`, `SC-006`)
Verify that `:desktopApp` and desktop source sets (`shared/src/desktopMain`, `composeApp/src/desktopMain`) contain zero mobile operating system dependencies:

1. Search `desktopApp/src/` for `^import android\.` → **0 matches**.
2. Search `shared/src/desktopMain/` for `^import android\.` → **0 matches**.
3. Search `composeApp/src/desktopMain/` for `^import android\.` → **0 matches**.
4. Inspect `desktopApp/build.gradle.kts` → **0 dependencies** on `:app`, `androidx.biometric`, or `androidx.glance`.

---

### Scenario D: Code Formatting & Quality Gate
Verify that all newly created and updated files strictly conform to the repository's `ktfmt` code style:

```bash
./gradlew ktfmtFormat
```

**Expected Outcome**:
- `ktfmtFormat` passes without any style or formatting violations across all modules.

---

### Scenario E: Cross-Platform Compose UI Test Verification (`SC-004`, `SC-005`)
Verify that shared Compose UI tests in `composeApp/src/commonTest` execute successfully on both Desktop JVM and Android targets without requiring an Android emulator or device:

```bash
# Run Compose UI tests on Desktop JVM
./gradlew :composeApp:desktopTest

# Run Compose UI tests on Android Host / Robolectric
./gradlew :composeApp:testAndroidHostTest
```

**Expected Outcome**:
- All common Compose UI tests (Notes overview, text note editor, checklist editor, search, labels, archive, trash, settings) pass with zero platform-specific failures.
- No `ActivityScenario` or `InstrumentationRegistry` dependencies are invoked during execution.

---

## 3. Interactive Milestone Run Scenarios

### Milestone 1: Runnable Desktop Starter Screen (`FR-002`, `SC-001`)

**Command**:
```bash
./gradlew :desktopApp:run
```

**Validation Checklist**:
1. **Window Creation**: A desktop window titled "NotallyX" appears within 2.0 seconds.
2. **Starter Screen Content**: Screen displays:
   - "NotallyX Desktop" header.
   - Host platform diagnostics (OS name, version, JVM runtime).
   - Module resolution badge confirming communication with `:shared` and `:composeApp`.
3. **Interactive Control**: Tapping or clicking an interactive button updates an on-screen state counter, verifying Compose desktop input handling.
4. **Window Resizing**: Resizing or maximizing the window adapts the UI layout fluidly without clipping.
5. **Clean Termination**: Closing the window terminates the JVM process cleanly without orphan threads.

---

### Milestone 2: Core Note & Checklist Management (`FR-005`, `FR-006`, `FR-007`)

**Validation Checklist**:
1. **First Run Database Creation**:
   - Check that `~/.local/share/NotallyX/NotallyDatabase` (or OS equivalent) is created automatically.
2. **Text Note Creation**:
   - Create a note titled "Desktop Test Note" with body text.
   - Return to overview; verify the note appears in the grid/list.
3. **Checklist Note Creation**:
   - Create a checklist note titled "Desktop To-Do".
   - Add items, toggle completion state; verify strike-through rendering.
4. **Persistence Across Restarts**:
   - Close the desktop application.
   - Relaunch via `./gradlew :desktopApp:run`.
   - Verify all notes, checklists, and modified timestamps remain exactly intact.
5. **Bi-Directional Database Compatibility**:
   - Copy an Android `NotallyDatabase` file into the desktop data directory.
   - Launch desktop app: verify all mobile notes and labels load without errors.
   - Edit a note on desktop, copy the database back to an Android device or emulator, and verify the edits appear on Android.

---

### Milestone 3: Comprehensive Route Parity & Mobile Feature Decoupling (`FR-008`, `FR-009`)

**Validation Checklist**:
1. **Route Navigation**:
   - Navigate to Search, Labels, Archive, Trash, and Settings.
   - Confirm search queries filter notes accurately in real time.
2. **Feature Decoupling Verification**:
   - Verify that Biometric Lock settings, notification scheduling options, and home screen widget settings are completely omitted from desktop menus.

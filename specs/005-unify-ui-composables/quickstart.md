# Quickstart & Verification Guide: Unified UI Composables

**Feature**: Unified Cross-Platform UI Composables (KMP)  
**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

---

## 1. Prerequisites

- **JDK**: Java Development Kit 17 (or compatible 64-bit JDK).
- **Android SDK**: Command-line tools or Android Studio with SDK Platform 37.
- **Git Branch**: `005-unify-ui-composables`.

---

## 2. Automated Verification & Quality Gates

Run the standard repository verification commands from the root directory:

### Gate 1: Code Formatting (`ktfmt`)
Verify that all newly created and refactored Kotlin files conform to `ktfmt` standard Kotlin style:
```bash
./gradlew ktfmtCheck
```
*To fix any formatting issues automatically, run `./gradlew ktfmtFormat`.*

### Gate 2: Compilation & Multiplatform Assembly
Verify that both `:app` (Android) and `:desktopApp` (Desktop) compile without unresolved references or missing composable dependencies:
```bash
# Android debug build
./gradlew :app:assembleDebug

# Desktop JAR assembly
./gradlew :desktopApp:desktopJar
```

### Gate 3: Unit & Logic Tests
Run unit tests across shared and presentation modules to verify MVI state handling:
```bash
./gradlew :composeApp:testAndroidHostTest
./gradlew :shared:testAndroidHostTest
```

### Gate 4: Visual & Screenshot Regression Gates
Validate that existing Roborazzi and Compose Screenshot preview tests continue to match visual references:
```bash
./gradlew verifyRoborazziDebug
./gradlew validateDebugScreenshotTest
```

---

## 3. End-to-End Manual Run Scenarios

### Scenario A: Desktop Application End-to-End Run
1. Launch the desktop target locally:
   ```bash
   ./gradlew :desktopApp:run
   ```
2. **Notes Overview Validation**:
   - Verify that the main notes list displays existing notes in the shared card layout.
   - Verify folder switching (Notes, Archive, Trash) using the shared drawer/navigation bar.
3. **Editor Screen Validation**:
   - Click "Create Note" or an existing note.
   - Verify that the unified `EditNoteScreen` opens, displaying title, body text field, date chip, color picker, and action buttons.
   - Type note content; press `Ctrl+B` or formatting icons to verify formatting.
   - Click back; verify the note appears updated in the overview grid.
4. **Settings & Reminders Validation**:
   - Open Settings; verify shared preference rows render cleanly.
   - Verify that mobile-only items (e.g. biometric lock) are omitted on desktop according to capability flags.

### Scenario B: Android Application End-to-End Run
1. Install and launch the Android debug application on a connected device or emulator:
   ```bash
   ./gradlew :app:installDebug
   ```
2. **Parity Check**:
   - Confirm that note editor, checklist editor, overview grid, and settings match the desktop UI structure and retain full Android system integrations (system insets, back gesture, biometric lock when enabled).

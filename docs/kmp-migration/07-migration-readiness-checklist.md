# 07. Migration Readiness Checklist

## Overview

This checklist serves as the **objective Go / No-Go gate** to evaluate whether the **NotallyX** codebase is sufficiently prepared to begin creating KMP multi-module targets (`shared`, `composeApp`, `desktopApp`).

---

## 1. Architectural Decoupling Checklist

- [ ] **CH-01: ViewModels in Presentation Layer (`:composeApp`)**  
  *Criteria*: 100% of ViewModels build in `:composeApp/commonMain` without importing `android.content.Context` or `android.widget.Toast`. `NotallyApp` configures `NavDisplay` with `rememberViewModelStoreNavEntryDecorator()`.  
  *Verification*: Run `./gradlew testDebugUnitTest` ensuring ViewModels instantiate in pure JVM unit tests.

- [ ] **CH-02: Domain Model URI Abstraction**  
  *Criteria*: Zero occurrences of `android.net.Uri` in `data/model/` (`BaseNote`, `Attachment`, `Audio`, `FileAttachment`).  
  *Verification*: `grep -r "import android.net.Uri" app/src/main/java/com/philkes/notallyx/data/model/` returns 0 results.

- [ ] **CH-03: Path, Storage & FilePicker Abstraction**  
  *Criteria*: All file I/O operations in import, export, and backup logic pass through `FileStorage` and `PathProvider` in `:shared`, and `FilePicker` in `:composeApp`.  
  *Verification*: Code review confirms no direct calls to `DocumentFile.fromSingleUri` or `ContentResolver` outside `AndroidFileStorage`.

---

## 2. Dependency Compatibility Checklist

- [ ] **CH-04: Multiplatform Compose & Skydoves ColorPicker**  
  *Criteria*: Skydoves ColorPicker controls confirmed operating purely via Compose state and Canvas in `:composeApp`.  
  *Verification*: ColorPicker dialogs contain zero imports of `android.content.Context`.

- [ ] **CH-05: Date/Time Portability**  
  *Criteria*: `PrettyTime` library removed; date/time formatting utilizes `kotlinx-datetime`.  
  *Verification*: `prettytime` library absent from `libs.versions.toml`.

- [ ] **CH-06: Pure Compose UI Composables**  
  *Criteria*: All core UI components in `core/ui/components` compile without `LocalContext.current` dependencies.  
  *Verification*: Code audit of `BaseNoteCard.kt` and `EditScreen.kt`.

---

## 3. Persistence & Database Driver Checklist

- [ ] **CH-07: Room 3 KMP Driver Factory & Native Packaging**  
  *Criteria*: `NotallyDatabase` driver configuration isolated behind `DatabaseDriverFactory` (`sqlite-bundled` on Desktop). Desktop Gradle packaging configured for native SQLite binaries.  
  *Verification*: Database unit tests execute with both in-memory and file drivers without requiring `ContextWrapper`.

- [ ] **CH-08: DataStore & Path Provider**  
  *Criteria*: Preference DataStore uses standard `PathProvider` resolving to standard OS app data directories on Desktop.  
  *Verification*: DataStore tests execute cleanly on pure JVM.

---

## 4. Android-Only Feature Encapsulation Checklist

- [ ] **CH-09: Android Reminders & Notifications Encapsulated**  
  *Criteria*: `AlarmManager` reminders and `ReminderReceiver` isolated within `androidApp`.  
  *Verification*: ViewModels interact with an optional `ReminderScheduler` interface (No-Op or JVM scheduler on Desktop).

- [ ] **CH-10: Audio Player & Recorder Abstraction**  
  *Criteria*: Audio playback and recording handled via `AudioPlayer` and `AudioRecorder` interfaces in `:shared`.  
  *Verification*: ViewModels do not bind to Android `Service` directly.

- [ ] **CH-11: WorkManager, Encryption Flag & Biometrics Encapsulated**  
  *Criteria*: WorkManager workers (`AutoBackupWorker`), `FEATURE_FLAG_BIOMETRIC_ENCRYPTS_DB = false`, and `BiometricPrompt` isolated in `androidApp`.  
  *Verification*: `NotallyXApplication` handles WorkManager, encryption flags, and biometric calls exclusively in Android lifecycle.

---

## 5. Build & Testing Checklist

- [ ] **CH-12: Separated Test Suites**  
  *Criteria*: Domain and ViewModel unit tests run as pure JVM tests without `@RunWith(RobolectricTestRunner::class)`.  
  *Verification*: Executing `./gradlew testDebugUnitTest` runs all core business logic tests in pure JVM mode.

- [ ] **CH-13: Clean Gradle Build Logic**  
  *Criteria*: `build-logic` plugins cleanly separate pure Kotlin settings from Android AGP settings.  
  *Verification*: `./gradlew assembleDebug` completes without warnings or plugin evaluation conflicts.

---

## Go / No-Go Decision Gate

```
If ALL 13 criteria above are checked [X]:
  ==> APPROVED TO BEGIN KMP MIGRATION (Phase 1: Module Extraction)

If ANY criteria remain unchecked [ ]:
  ==> HOLD KMP MIGRATION. Complete remaining PREP tasks in `03-prerequisites-roadmap.md`.
```

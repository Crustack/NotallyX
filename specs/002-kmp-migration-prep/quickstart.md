# Quickstart Validation Guide: Multiplatform Migration Prerequisites

**Feature**: `002-kmp-migration-prep`  
**Date**: 2026-11-01  
**Status**: Ready for Task Generation (`/speckit-tasks`)  

---

## 1. Overview

This guide provides runnable validation scenarios and verification commands to prove that all 22 prerequisite refactorings (`PREP-001` through `PREP-022`) and all 13 Go/No-Go readiness checks (`CH-01` through `CH-13`) pass without regression.

---

## 2. Automated Verification Commands

### Scenario A: Pure JVM Unit Tests (Database, Converters, Domain & ViewModels)
Verify that `Converters` (`kotlinx.serialization.json`), Room 3 `NotallyDatabase` (with `BundledSQLiteDriver`), `NotallyNavKey` polymorphic serialization, and all feature ViewModels run in pure JVM unit tests without Robolectric or `ContextWrapper`:

```bash
./gradlew testDebugUnitTest
```

### Scenario B: Static Zero-Android-Import Audit (Go/No-Go Gate Checks)
Run static checks against the packages mapped to `:shared/commonMain` and `:composeApp/commonMain` to verify zero Android framework leakage:

1. **Zero `android.net.Uri` in Domain Models & Extensions (`CH-02`)**:
   - Search `app/src/main/java/com/philkes/notallyx/data/` for `import android.net.Uri` → expected **0 matches**.
2. **Zero `org.json`, `android.util.*`, `android.text.*`, `Parcelable` in Data & Database (`PREP-020`)**:
   - Search `app/src/main/java/com/philkes/notallyx/data/` and `core/database/` for `import org.json`, `import android.os.Parcelable`, `import kotlinx.parcelize` → expected **0 matches**.
3. **Zero `AndroidViewModel`, `Application`, `Context`, or `R.*` in Shared Feature ViewModels (`CH-01`, `PREP-003`, `PREP-019`)**:
   - Search `app/src/main/java/com/philkes/notallyx/feature/` ViewModels (excluding Android-only `MainActivityViewModel`, `EditActivityViewModel`, `ErrorActivityViewModel`) for `import android.` or `import com.philkes.notallyx.R` → expected **0 matches**.
4. **Zero `LocalContext` in Low-Level UI Components (`CH-04`, `CH-06`)**:
   - Search `app/src/main/java/com/philkes/notallyx/core/ui/components/` and `core/ui/dialog/color/` for `LocalContext` or `import android.content.Context` → expected **0 matches**.

### Scenario C: Visual Regression & Localization Verification (30 Locales)
Verify that the migration to Compose Multiplatform Resources (`PREP-019`) and UI component cleanup (`PREP-007`, `PREP-009`) produce zero visual or localization regressions across all themes and locales:

```bash
./gradlew verifyRoborazziDebug
./gradlew validateDebugScreenshotTest
```

### Scenario D: Build Variants & Code Style Gate (`PREP-018`)
Verify that the refactored `build-logic` convention plugins, `debug` and `beta` build variants, and `ktfmt` formatting pass cleanly:

```bash
./gradlew ktfmtCheck
./gradlew assembleDebug assembleBeta
```

---

## 3. End-to-End Functional Verification Scenarios

1. **Legacy Encrypted Database Decryption (`PREP-012`)**:
   - Run the instrumented decryption migration test on an encrypted v11 SQLCipher database fixture.
   - Verify that after authentication, `NotallyDatabase` checkpoints WAL, decrypts the database to standard SQLite, atomically replaces the file, sets `biometricLockEncryptsDb = false`, and preserves 100% of notes, checklists, labels, reminders, and attachments.
2. **Converter Byte-Compatibility Round-Trip (`PREP-020`)**:
   - Run `ConvertersTest` against v1–v11 JSON fixtures for `SpanRepresentation`, `ListItem`, `FileAttachment`, `Audio`, `Reminder`, and `Label` lists.
   - Verify identical JSON serialization output and lenient fallback handling for malformed inputs.
3. **Import / Export & PDF Generation (`PREP-004`, `PREP-005`)**:
   - Export a formatted note with checklists and images to PDF, HTML, TXT, JSON, and Markdown.
   - Export a full ZIP backup and re-import it, verifying all notes and media files restore via `FileStorage` and `PathProvider`.
4. **Navigation 3 State Serialization (`PREP-021`)**:
   - Run `NavigationStateSerializationTest` verifying that every `NotallyNavKey` subtype serializes and deserializes through `NotallyNavSavedStateConfig` without reflection.

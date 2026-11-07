# Quickstart Validation Guide: Kotlin Multiplatform Project Migration (Android Target Only)

**Feature**: `003-kmp-android-migration` ([spec.md](spec.md))  
**Date**: 2026-11-01  
**Status**: Ready for Task Generation (`/speckit-tasks`)  

---

## 1. Overview

This guide provides runnable validation scenarios and verification commands to prove that **NotallyX** has been cleanly migrated from a single-module Android project into a multi-module **Kotlin Multiplatform (KMP)** architecture (`:shared`, `:composeApp`, `:app`, `:test-lib`) targeting **Android** with zero regressions.

---

## 2. Automated Verification Scenarios

### Scenario A: Multi-Module KMP Compilation & Variant Assembly (`FR-001`, `FR-002`, `FR-009`, `SC-002`)
Verify that `:shared` and `:composeApp` compile with `org.jetbrains.kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` (Android target only) and that `:app` packages both `debug` and `beta` APK variants cleanly:

```bash
./gradlew :shared:assembleAndroidMain :composeApp:assembleAndroidMain
./gradlew assembleDebug assembleBeta
```

**Expected Outcome**:
- `:shared` and `:composeApp` compile `commonMain` and `androidMain` without errors.
- `:app` produces `NotallyX-<version>.apk` for `debug` (`com.philkes.notallyx.debug`) and `beta` (`com.philkes.notallyx.beta`).
- Room 3 KSP generates `NotallyDatabase_Impl.kt` and exports schemas under `shared/schemas/`.

---

### Scenario B: Static `commonMain` Zero-Android-Import Audit (`FR-003`, `SC-001`, `SC-005`)
Verify that 100% of files inside `shared/src/commonMain` and `composeApp/src/commonMain` contain zero Android framework imports (`import android.*`) and that `:shared` has zero dependency on `:composeApp` or `:app`:

1. Search `shared/src/commonMain/` for `^import android\.` → **0 matches**.
2. Search `composeApp/src/commonMain/` for `^import android\.` → **0 matches**.
3. Search `shared/src/commonMain/` for `import com\.philkes\.notallyx\.R` → **0 matches**.
4. Inspect `shared/build.gradle.kts` dependencies → **0 references** to `:composeApp` or `:app`.

---

### Scenario C: Multi-Module Unit Test Suites (`FR-010`, `SC-003`)
Run unit tests across `:shared`, `:composeApp`, and `:app` to verify database migrations (v1–v11 with `BundledSQLiteDriver`), JSON converter byte compatibility, navigation polymorphic serialization, and ViewModel behavior:

```bash
./gradlew test
```

**Expected Outcome**:
- `:shared:testAndroidHostTest` passes all database, DAO, converter, preference, and importer/exporter unit tests.
- `:composeApp:testAndroidHostTest` passes all navigation serialization and shared ViewModel unit tests.
- `:app:testDebugUnitTest` passes all Android host, worker, and Robolectric tests.

---

### Scenario D: Visual Regression & 30-Locale Verification (`FR-006`, `SC-003`, `SC-004`)
Verify that all 30 translated locales, Compose previews, and Roborazzi screenshot tests render identically after moving shared UI and resources across module boundaries:

```bash
./gradlew generateLocalesPreview
./gradlew verifyRoborazziDebug
./gradlew validateDebugScreenshotTest
```

**Expected Outcome**:
- `generateLocalesPreview` generates `@AppStorePreview` covering all supported locales.
- `verifyRoborazziDebug` and `validateDebugScreenshotTest` pass with zero unexpected visual diffs.

---

### Scenario E: Code Formatting Gate (`FR-011`)
Verify that all new and moved source files across `:shared`, `:composeApp`, `:app`, `:test-lib`, and `:build-logic` comply with `ktfmt` (`kotlinLangStyle`):

```bash
./gradlew ktfmtFormat
```

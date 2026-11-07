# Feature Specification: Kotlin Multiplatform Project Migration (Android Target Only)

**Feature Branch**: `003-kmp-android-migration`

**Created**: 2026-11-01

**Status**: Draft

**Input**: User description: "based on @002-kmp-migration-prep now migrate the pure android app to kotlin multiplatform project. For starters only focus on having an android target, we will add desktop target later./home/user/AndroidStudioProjects/NotallyX/specs/002-kmp-migration-prep/"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Seamless Android User Experience and Upgrade Continuity (Priority: P1)

As an existing or new NotallyX Android user, I want the application to install, upgrade, and operate identically to the previous release—preserving all my notes, checklists, labels, reminders, attachments, widgets, biometric app lock, backups, and 30-language translations—after the project transitions to a multiplatform modular structure.

**Why this priority**: User-facing continuity and zero data loss are the highest priority. Migrating the internal project architecture to multiplatform modules must be completely invisible to Android users upgrading from prior versions or installing the app fresh.

**Independent Test**: Can be fully tested by building and installing the Android application across all build variants, upgrading over an existing user database (v1 through v11, including legacy encrypted databases), and running the full suite of automated user journey, visual regression, and backup/restore tests.

**Acceptance Scenarios**:

1. **Given** an existing Android installation with notes, checklists, formatting spans, labels, reminders, audio recordings, images, file attachments, and user preferences, **When** the user upgrades to the multiplatform-structured Android build, **Then** the application launches with the identical application identifier, preserves 100% of user data and preferences, and requires no manual migration steps.
2. **Given** a user interacting with any feature screen (notes overview, note editor, checklist editor, labels, search, reminders, settings, import/export, or media playback/recording) in any of the 30 supported locales, **When** they perform standard actions or navigate across single-pane and adaptive two-pane layouts, **Then** all screens, localized strings, pluralized text, icons, and navigation flows behave identically with zero visual or behavioral regressions.
3. **Given** a user who relies on Android-specific system integrations (home screen widgets, status bar pinned notifications, scheduled reminder alarms, background periodic auto-backups, crash reporting, and biometric/PIN app lock), **When** those system events trigger or are configured, **Then** each integration functions identically to the pre-migration application.

---

### User Story 2 - Multiplatform Shared Domain and Data Separation (Priority: P1)

As a NotallyX maintainer preparing to support additional platforms in the future, I want all core business rules, domain models, database persistence, preference storage, import/export logic, and platform service contracts extracted into a dedicated shared multiplatform domain/data boundary configured with an Android target, so that platform-independent logic is strictly separated from Android operating system code.

**Why this priority**: Separating the domain and data layers into a dedicated multiplatform boundary (`commonMain` vs `androidMain`) is the foundational structural milestone that enforces platform independence at compile time and prevents Android-specific dependencies from creeping back into core logic.

**Independent Test**: Can be independently tested by compiling the shared domain/data module with only its common and Android source sets enabled, verifying that common source sets contain zero mobile operating system references, and executing all database, converter, preference, and import/export unit tests against the shared module.

**Acceptance Scenarios**:

1. **Given** the domain models, database schema and migrations (v1 through v11), data access objects, JSON converters, preference stores, and import/export engines, **When** placed in the common source set of the shared domain/data module, **Then** they compile and pass all automated tests without any direct dependency on mobile operating system APIs or UI components.
2. **Given** platform-specific data operations (such as resolving device storage paths, creating the mobile database driver, accessing content streams, or storing hardware-backed encrypted preferences), **When** the application runs on Android, **Then** the Android source set of the shared module supplies the mobile implementations seamlessly through the established platform contracts.
3. **Given** database schema definitions and migration histories (v1 through v11), **When** the shared domain/data module is built, **Then** database schemas are generated and verified deterministically with zero changes to existing table structures or on-disk SQLite compatibility.

---

### User Story 3 - Multiplatform Shared Presentation and UI Separation (Priority: P2)

As a NotallyX maintainer, I want all shared feature screens, UI components, themes, navigation routes, state holders, and localized multiplatform resources (30 languages) extracted into a dedicated shared multiplatform presentation module configured with an Android target, while keeping the Android application entry point focused solely on mobile OS hosting and mobile-only features.

**Why this priority**: Extracting the presentation layer (`commonMain` and `androidMain`) decouples the 18 feature routes, ~30 state holders, and 30-locale UI resources from the Android application shell, making future addition of a desktop target a clean module-level addition rather than another codebase-wide refactoring.

**Independent Test**: Can be tested by compiling the shared presentation module and the Android application entry point module independently, running pure unit tests on all shared state holders and navigation serialization, and verifying all visual screenshot tests andlocalization preview tasks pass.

**Acceptance Scenarios**:

1. **Given** all shared feature state holders, UI state models, stateless screens, reusable UI components, color pickers, navigation routes, and multiplatform resources across all 30 locales, **When** located in the common source set of the shared presentation module, **Then** they compile cleanly with zero references to mobile activity contexts or mobile-only resource identifiers.
2. **Given** interactive platform UI integrations (file/folder selection dialogs, external sharing sheets, permission checks, and Android activity result launchers), **When** invoked from shared feature routes on Android, **Then** the Android source set of the shared presentation module fulfills those requests using native Android system dialogs and intents.
3. **Given** the Android application entry module, **When** inspected and built, **Then** it contains only mobile application bootstrap logic, activity hosts, home screen widgets, background work schedulers, alarm/notification receivers, biometric lock screens, and mobile packaging configurations, delegating all core UI rendering and state management to the shared presentation module.

---

### Edge Cases

- **Upgrade from Legacy Versions & Encrypted Databases**: When an existing Android user upgrades from an older version (including one with a legacy encrypted database or public folder storage enabled), the application entry point and shared modules must coordinate startup decryption, database file resolution, and WAL checkpointing without path mismatches or race conditions.
- **Resource & Asset Resolution Across Module Boundaries**: Moving shared strings (30 locales) and vector icons into the shared presentation module while keeping Android-only resources (launcher mipmaps, widget layouts, notification icons, file provider paths) in the Android application module must not cause missing resource crashes, broken intent filters, or duplicate resource merge conflicts.
- **Dependency Injection Initialization Order**: When the Android application starts (or when background workers, alarm receivers, or home screen widgets wake up the process independently of the main activity), dependency injection modules across the shared data, shared presentation, and Android entry layers must initialize completely before any component requests dependencies.
- **Release Minification and Shrinker Rules**: Splitting classes across shared multiplatform modules and the Android application entry module must preserve all required serialization, database entity, and crash-handler keep rules so release and beta builds do not fail at runtime under code shrinking.
- **Translation and Localization Tooling Continuity**: Automated translation export, auto-translation, and 30-locale preview generation workflows must continue to locate and process strings in their new multiplatform module location without manual path workarounds.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST be structured into clearly separated multiplatform and application modules consisting of:
  1. A shared domain and data multiplatform module (`shared`) containing platform-independent business logic, domain entities, database persistence, preferences, import/export logic, and platform service contracts in its common source set, and Android data implementations in its Android source set.
  2. A shared presentation multiplatform module (`composeApp`) containing platform-independent feature state holders, UI state models, screens, routes, navigation state, themes, shared UI components, and 30-locale multiplatform resources in its common source set, and Android UI/launcher implementations in its Android source set.
  3. An Android application entry module (`androidApp` or `:app` acting as the Android application host) containing Android application startup, activities, home screen widgets, background task workers, broadcast receivers, foreground audio services, biometric lock UI, and Android packaging/signing configuration.
- **FR-002**: Both multiplatform modules (`shared` and `composeApp`) MUST be configured exclusivamente with an Android target (`commonMain`, `androidMain`, `commonTest`, `androidUnitTest` / `androidInstrumentedTest`) for this phase; desktop or other non-Android targets MUST NOT be enabled yet, ensuring focused validation of the multiplatform architecture on Android.
- **FR-003**: The common source sets (`commonMain` and `commonTest`) of both `shared` and `composeApp` MUST enforce zero compile-time dependencies on Android-only operating system packages (`android.*`), guaranteeing that all code in `commonMain` is genuinely platform-agnostic.
- **FR-004**: The shared domain/data module (`shared`) MUST have zero dependency on the shared presentation module (`composeApp`) or the Android application module, enforcing strict unidirectional architectural layering (`androidApp` → `composeApp` → `shared`).
- **FR-005**: All 18 feature destinations in the shared presentation module MUST preserve the 4-File Feature Quad pattern (`Model`, `ViewModel`, `Screen`, `Route`) and unidirectional data flow without regression.
- **FR-006**: All user-facing strings, plurals, and shared icons across all 30 supported locales MUST be served from the shared presentation module's multiplatform resources, while Android-only OS resources (app launcher icons, widget metadata/layouts, notification icons, file provider XML paths, and activity themes) MUST remain in the Android application module.
- **FR-007**: Database schema generation, migration definitions (v1 through v11), and WAL checkpointing MUST operate within the shared domain/data module and produce schemas 100% compatible with existing user installations.
- **FR-008**: Dependency injection bindings MUST be partitioned cleanly so that shared domain/data bindings are defined in `shared`, shared presentation/ViewModel bindings are defined in `composeApp`, and Android platform implementations and OS services are wired at application startup in the Android application module.
- **FR-009**: The Android application output MUST preserve the exact application ID (`com.philkes.notallyx`), build variants (`debug`, `beta`, `release` with their respective application ID and version suffixes), signing configurations, and minification/keep rules so existing users can upgrade seamlessly.
- **FR-010**: All automated test suites—including pure unit tests in `shared` and `composeApp`, shared test doubles in `test-lib`, visual regression screenshot tests, and Android instrumented UI tests—MUST be migrated or wired to execute cleanly against the new module structure.
- **FR-011**: Build automation for code formatting, translation export/auto-translation, and locale preview generation MUST be updated to operate seamlessly across the new multi-module layout.

### Key Entities

- **Shared Domain & Data Module (`shared`)**: The multiplatform library boundary encapsulating core note/label/reminder/attachment entities, database schemas and DAOs, preference repositories, import/export processors, and platform service contracts (`commonMain`) alongside their Android data-layer implementations (`androidMain`).
- **Shared Presentation Module (`composeApp`)**: The multiplatform UI and presentation library boundary encapsulating feature state holders, UI state models, declarative screens, navigation routes, shared UI components, multiplatform localized resources for 30 languages (`commonMain`), and Android UI bridge implementations such as file pickers, share handlers, and permission checkers (`androidMain`).
- **Android Application Host Module**: The deployable Android application package containing mobile OS entry points (application class, activities, widgets, background workers, alarm receivers, media services, biometric lock) that initializes dependency injection and hosts the shared presentation layer.
- **Cross-Module Dependency Injection Graph**: The modularized service registration structure linking shared domain repositories, shared presentation state holders, and Android platform service implementations at runtime.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of code placed in `commonMain` source sets of `shared` and `composeApp` compiles under multiplatform rules with zero references to mobile-only operating system packages (`android.*`).
- **SC-002**: 100% of existing Android application features, build variants (`debug`, `beta`, `release`), and upgrade paths from database versions v1 through v11 (including legacy encrypted databases) function without data loss or behavioral regression.
- **SC-003**: 100% of existing automated test suites—including pure unit tests, visual screenshot verification tests, and Android instrumented UI tests—pass across the multi-module project structure.
- **SC-004**: All 30 supported locales render accurately in the application and localization verification checks with zero missing strings or broken resource references across module boundaries.
- **SC-005**: Adding a future desktop target requires zero structural file moves of core domain, data, or shared presentation code out of the Android application module, as 100% of shareable business and UI logic already resides in `commonMain` of `shared` and `composeApp`.

## Assumptions

- All 22 prerequisite decoupling tasks (`PREP-001` through `PREP-022`) and the 13 readiness checklist gates (`CH-01` through `CH-13`) from `002-kmp-migration-prep` are treated as the completed baseline for this migration.
- Only the **Android** target is configured in the multiplatform modules during this feature; the Desktop (`jvm("desktop")`) target and `desktopApp` executable module are explicitly out of scope and will be added in a subsequent feature.
- Because the ultimate multiplatform targets for NotallyX are exclusively Android and Desktop (both running on the JVM), `commonMain` in `shared` and `composeApp` continues to permit standard JDK APIs (`java.io`, `java.nio`, `java.time`, `java.util`) and pure-JVM libraries (`jsoup`, `commonmark`, `zip4j`, `simple-xml`, `PrettyTime`) as established in `002-kmp-migration-prep`.
- Android-specific features—including Jetpack Glance home screen widgets, WorkManager periodic tasks, AlarmManager reminder notifications, foreground Media3/MediaRecorder services, and Biometric/PIN `LockedActivity`—remain in the Android application host layer and communicate with shared layers through the platform contracts established in `002-kmp-migration-prep`.
- Whether the Android application host module retains the Gradle path `:app` or is named `:androidApp` (with corresponding build script updates) is an implementation-level project layout choice for the planning phase, provided the resulting APK/AAB artifacts, application IDs, and CI/developer workflows remain unbroken.

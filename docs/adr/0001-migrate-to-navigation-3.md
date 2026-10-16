# 1. Migrate Navigation System to Jetpack Navigation 3 (Pure Compose)

* Status: Accepted
* Date: 2026-10-13

## Context and Problem Statement

NotallyX previously used Jetpack Navigation 2 with an XML navigation graph (`app/src/main/res/navigation/navigation.xml`) and `NavHostFragment`. Although the screen contents were implemented using Jetpack Compose (`ComposeView`), the outer screen containers were held inside Android `Fragment` subclasses (`NotesFragment`, `LabelsFragment`, `SettingsFragment`, `RemindersFragment`, `SearchFragment`, etc.).

This hybrid approach introduced extra Fragment lifecycle complexity, XML navigation graph maintenance overhead, and interoperability friction between Compose `MainScreen` and Fragment-based destinations.

## Decision Drivers

* Modernize the navigation architecture to standard Jetpack Navigation 3 (`androidx.navigation3`).
* Eliminate Android Fragment hosts in favor of pure Composable screen destinations.
* Achieve type-safe route definitions (`NavKey`) using `kotlinx.serialization`.
* Remove legacy Navigation 2 dependencies (`androidx.navigation:navigation-fragment-ktx` and `androidx.navigation:navigation-ui-ktx`) and XML navigation resource files.

## Considered Options

1. **Option 1: Jetpack Navigation 3 with Fragment Interoperability** - Retain Android Fragments and wrap them in `NavDisplay` via Compose `AndroidView`.
2. **Option 2: Jetpack Navigation 3 Pure Compose** - Fully migrate screen destinations to Composable screens, removing Android Fragment hosts entirely.

## Decision Outcome

Chosen Option: **Option 2 (Jetpack Navigation 3 Pure Compose)**.

All Fragment host classes (`NotesFragment`, `NotesOverviewFragment`, `LabelsFragment`, `SettingsFragment`, `DeletedFragment`, `ArchivedFragment`, `RemindersFragment`, `SearchFragment`, `DisplayLabelFragment`, `UnlabeledFragment`) will be converted to pure Composable screens or integrated into existing Composable screens (`NotesGridScreen`, `LabelsScreen`, `SettingsScreen`, etc.). `MainActivity` will host `NavDisplay` with a `NavBackStack` initialized via `rememberNavBackStack`.

### Positive Consequences

* Single unified UI and navigation layer built entirely in Jetpack Compose.
* Elimination of Fragment lifecycle boilerplate and XML navigation graphs.
* Strongly-typed navigation keys (`NavKey`) with compile-time safety and automatic serialization.
* Reduced dependency footprint by removing legacy Navigation 2 libraries.

### Negative Consequences / Risks

* Requires updating all test cases and screenshot tests that previously referenced `NavHostFragment` or Fragment transaction managers.

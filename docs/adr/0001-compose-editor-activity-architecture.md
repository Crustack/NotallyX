# 0001. Retain Host Activities for Compose Note Editor Screens

* Status: Accepted
* Date: 2026-03-31

## Context and Problem Statement
Phase 7 of the Jetpack Compose migration requires converting the Note Editor UI (`activity_edit.xml`, `EditActivity.kt`, `EditNoteActivity.kt`, `EditListActivity.kt`) to Jetpack Compose.
`EditNoteActivity` and `EditListActivity` are separate entry points registered in `AndroidManifest.xml` and invoked directly by external intents, shortcuts, widgets, and navigation graphs with specific extras (`EXTRA_SELECTED_BASE_NOTE`).

## Decision Drivers
* Maintain 100% backward compatibility with external Android intents, shortcuts, and app widgets.
* Avoid breaking existing activity lifecycles, auto-save logic, and ViewModel scopes.
* Enable incremental migration to Compose screens (`EditNoteScreen`, `EditListScreen`) while reusing shared Compose sub-views.

## Considered Options
1. **Single-Activity Architecture**: Refactor `EditActivity` and all editor screens into a single activity hosted by Compose Navigation.
2. **Host Activity Pattern**: Keep `EditNoteActivity` and `EditListActivity` as distinct host `AppCompatActivity` entry points, setting content to `EditNoteScreen` and `EditListScreen` Composables via `setContent { NotallyXTheme { ... } }`.

## Decision Outcome
Chosen option: **Option 2 (Host Activity Pattern)**.

### Positive Consequences
* Preserves external intent contracts and manifest definitions without requiring manifest or intent filter changes.
* `EditNoteScreen` and `EditListScreen` can be cleanly tested with `@Preview` and Compose UI test rules.
* Shares modular Composable components (`TextFormattingBar`, `ImagePreviewRow`, `FilesPreviewColumn`, `ChecklistItemRow`) between text notes and checklist notes.

### Negative Consequences
* Activity subclasses remain in the codebase during transition instead of moving to pure Compose Navigation routes.

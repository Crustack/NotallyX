# Glossary

## Note Editor Screen
The Compose UI screen (`EditNoteScreen`, `EditListScreen`) responsible for editing a note's title, body or checklist items, attachments (images, files, audio), labels, reminders, and background color.

## Text Formatting Bar
The toolbar composable (`TextFormattingBar.kt`) replacing `bottom_text_formatting_menu.xml` that displays rich text formatting actions (bold, italic, strikethrough, monospace, heading, bullet list, link, clear formatting) when text is selected in the note body editor.

## Navigation 3 Destinations
The set of type-safe `NavKey` definitions (`NotallyNavKey`) and corresponding Composable screens (`NavDisplay`) managing top-level screen transitions (Notes, Labels, Deleted, Archived, Reminders, Settings, Search, DisplayLabel, Unlabeled) without Android Fragments or XML navigation graphs.

## Reminder Date & Time Pickers
The Compose dialog components (`DatePickerDialog` and custom `TimePickerDialog` wrapping M3 `TimePicker`) integrated into `RemindersScreen` via `ReminderDialogState`, replacing legacy `DialogFragment` wrappers (`DatePickerFragment` and `TimePickerFragment`).

## Checklist Item
An individual item within a task list that contains a text body, a checked state, an optional completion timestamp, and can either be a top-level parent item or a single-level indented child item.

## Parent Item
A top-level checklist item that can contain child items. Its completion status automatically reflects whether all of its child items are completed.

## Child Item
An indented checklist item positioned beneath a parent item. A child item belongs to its preceding parent item.



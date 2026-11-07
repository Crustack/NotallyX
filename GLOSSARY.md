# Glossary

## Note Editor Screen
The Compose UI screen (`EditNoteScreen`, `EditListScreen`) responsible for editing a note's title, body or checklist items, attachments (images, files, audio), labels, reminders, and background color.

## Text Formatting Bar
The toolbar composable (`TextFormattingBar.kt`) replacing `bottom_text_formatting_menu.xml` that displays rich text formatting actions (bold, italic, strikethrough, monospace, heading, bullet list, link, clear formatting) when text is selected in the note body editor.

## Navigation 3 Destinations
The set of type-safe `NavKey` definitions (`NotallyNavKey`) and corresponding Composable screens (`NavDisplay`) managing top-level screen transitions (Notes, Labels, Deleted, Archived, Reminders, Settings, Search, DisplayLabel, Unlabeled) without Android Fragments or XML navigation graphs.

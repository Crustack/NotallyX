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
An individual item (`ListItem`) within a task list containing a text body (`body`), a checked state (`checked`), an indentation flag (`isChild`), a list of nested children (`children`), and an optional completion timestamp (`checkedTimestamp`).

## Parent Item
A top-level checklist item (`isChild = false`) that contains nested child items in its `children` list. A parent item is automatically checked when all of its children are checked, and checking a parent item automatically checks all of its children.

## Child Item
An indented checklist item (`isChild = true`) positioned beneath the preceding parent item. A child item has an empty `children` list.

## Auto-Sort by Checked
A list sorting behavior (`ListItemSort`) where fully checked parent items (and standalone checked items) move to a separate checked items section, while unchecked items and partially-checked parent items remain in the active checklist section. When set to `NO_AUTO_SORT`, checked and unchecked items remain together in inline list order.

## Drag Handle
A visual affordance (`drag_handle` icon) on a checklist item row that serves as the touch target for reordering items.





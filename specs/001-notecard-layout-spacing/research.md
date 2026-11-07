# Technical Research & Decisions: Notes Card Layout and Spacing Redesign

**Feature**: Notes List/Grid - Layout and Spacing Redesign ([Issue #918](https://github.com/Crustack/NotallyX/issues/918))  
**Date**: 2026-10-31  
**Status**: Completed  

---

## 1. Content-First Visual Hierarchy

### Problem Context
In `BaseNoteCard.kt`, the current vertical order of composables inside the content column is:
1. `ImagePreview`
2. `FlowRow { Title + ReminderChip }`
3. `Text(Date)` (Creation or Modification timestamp)
4. `FlowRow { FileAttachmentPreviewChip }`
5. Note Body text / Checklist items
6. `FlowRow { LabelChip }`

Having Date text and File chips between Title and Body disrupts reading flow. Users scanning their notes want to read the note content immediately after the title without wading through metadata.

### Decision
Reorganize the vertical composition of `BaseNoteCard` into four distinct layers:
1. **Media Layer**: Full-width `ImagePreview` (unchanged aspect ratio `8:5`, optional).
2. **Header Layer**: Title text (start-aligned) + Reminder affordance (end-aligned).
3. **Primary Content Layer**: Note Body text or Checklist items immediately below the title, separated by a standard 4dp vertical gap.
4. **Metadata Footnote Layer**: Consolidated bottom container grouping all secondary information:
   - Attachment indicator summary
   - Label chips
   - Sort date timestamp (`Creation` or `Modified`)

### Rationale
- Completely preserves continuous reading flow between title and content.
- Aligns with modern Android note-taking UI standards (e.g. Google Keep, FairNote, ColorNote) and community consensus on Issue #918.
- Keeps secondary metadata accessible at the bottom without cluttering note readability.

### Alternatives Considered
- *Keep Date under Title*: Evaluated but rejected. Having dates in the header pushes body text down inconsistently depending on whether date display is enabled.
- *Keep Files under Title*: Evaluated but rejected. Large file chips fragment the visual connection between title and body.

---

## 2. File Attachment Representation on Overview Cards

### Problem Context
Currently, `BaseNoteCard` renders file attachments using full `FileAttachmentPreviewChip(fileName = baseNote.files[0].originalName)` plus `+X more files` chips. In multi-column grid view or with long file names, this causes severe horizontal wrapping and height bloat on cards where attachments cannot even be opened directly.

### Decision
Replace full-width filename chips on overview cards with a compact **Attachment Summary Indicator**:
- A small icon (e.g., `R.drawable.attachment_16` or vector paperclip) accompanied by the attachment count (e.g., `📎 2`).
- Positioned in the metadata footnote layer at the bottom of the card.
- If no files are attached, the indicator is completely omitted.

### Rationale
- Decreases card height bloat by ~20–30% on notes with files in Grid View.
- Overview cards are for scanning, not file management; detailed file names are visible once the note is opened in the editor.
- Compact badge fits comfortably alongside labels and date in the footnote row.

### Alternatives Considered
- *Full filename chips in List View, compact in Grid View*: Rejected because even in List View, filename chips between or below content add unnecessary visual noise. A consistent compact footnote indicator is cleaner across both views.

---

## 3. Reminder Indicator: List View vs. Grid View

### Problem Context
The current `ReminderChip` renders a full date-time string (e.g. `"Jan 2, 2026, 14:00"`) inside a pill shape with padding. In narrow grid lanes (minimum 180dp), when paired with a title, `FlowRow` either forces the reminder to wrap to a line of its own or causes the title to wrap into single words.

### Decision
- **In Grid View (`isGrid = true`)**:
  - Render a compact **Reminder Icon Badge**: an icon button/badge displaying the alarm bell (or repeat icon for recurring reminders) with subtle indicator styling, anchoring it to the top-right of the header.
  - Keeps the header height predictable (1–2 lines of title).
- **In List View (`isGrid = false`)**:
  - Render the standard or slightly streamlined `ReminderChip` showing the scheduled alarm time, right-aligned.
  - Title takes available space (`weight(1f, fill = false)`), allowing the reminder chip to stay beside short titles, and gracefully wrapping below the title only when title text requires the full width.
- Both modes preserve the interactive `onReminderClick` callback.

### Rationale
- Solves the maintainer's primary inquiry on Issue #918 ("What happens if the Reminder does not fit to the right of the title in grid?").
- Optimizes spatial efficiency in grid columns while retaining rich detail in wide list rows.

### Alternatives Considered
- *Move Reminder to the Footnote*: Rejected. An active reminder is a critical status indicator that users expect to see immediately in the note header.

---

## 4. Spacing System & Padding Tokens

### Problem Context
Current spacing uses arbitrary, mixed padding:
- Card outer margin: `horizontal = 4.dp, vertical = 4.dp`
- Content column inner padding: `12.dp`
- Column verticalArrangement: `spacedBy(2.dp)`
- Title-to-body explicit spacer: `Spacer(4.dp)` (resulting in 6dp total)
- Body-to-labels spacer: `Spacer(6.dp)` (resulting in 8dp total)
- Label chip inner padding: `horizontal = 12.dp, vertical = 6.dp` with 1dp border

### Decision
Establish uniform, predictable spacing tokens:
- **Card Outer Margin**: `horizontal = 4.dp, vertical = 4.dp` (retained for grid spacing).
- **Card Inner Content Padding**:
  - List View: `12.dp` uniform all sides.
  - Grid View: `10.dp` uniform all sides (provides higher visual density and more content width in 180dp columns).
- **Inter-Element Spacing**:
  - Title to Body: `4.dp`
  - Body / Checklist to Footnote: `8.dp`
  - Footnote horizontal item gap: `6.dp`
  - Footnote vertical wrapping gap: `4.dp`
- **Label Chip Dimensions**:
  - Streamline `LabelChip` inner padding to `horizontal = 8.dp, vertical = 2.dp` (or 3dp) with rounded corners and subtle border or tinted surface, preventing labels from commanding excessive card height.

### Rationale
- Predictable spacing tokens create visual harmony and prevent jarring jumps between cards of varying content.
- Reducing label chip bulk preserves card space for the note body.

---

## 5. View Mode Awareness in `BaseNoteCard`

### Problem Context
`BaseNoteCard` currently takes `config: NoteCardDisplayConfig` without knowing whether it is being rendered in `isGrid = true` or `isGrid = false` mode.

### Decision
Add `isGrid: Boolean = false` parameter to `BaseNoteCard` (with default `false` for backwards compatibility with existing screenshot tests and callers).
Update `NotesGridScreen.kt` to pass `isGrid = true` in the `LazyVerticalStaggeredGrid` item lane, and `isGrid = false` in the `LazyColumn` item lane.

### Rationale
- Non-breaking change: existing calls (e.g. in previews or `PickNoteScreen`) continue to function without modification.
- Explicit and unambiguous: allows `BaseNoteCard` to conditionally render compact reminder and padding tokens without guessing screen dimensions.

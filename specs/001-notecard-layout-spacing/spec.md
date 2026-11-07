# Feature Specification: Notes Card Layout and Spacing Redesign

**Feature Branch**: `001-notecard-layout-spacing`

**Created**: 2026-10-31

**Status**: Draft

**Input**: User description: "Implementation for issue #918 from github. Checkout the current spacing in BaseNoteCard component. Read the issue comments and take them into consideration when proposing spacing, alignment and placement of the elemnts of the BaseNoteCard for List and Grid View"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Clean Content-First Reading Experience (Priority: P1)

As a user browsing my notes in list or grid view, I want note titles and note contents (body text or checklist items) to appear immediately adjacent to each other without intermediate dates or attachments interrupting them, so that I can quickly read and scan note contents without visual friction.

**Why this priority**: The primary function of a note card is reading note title and body content. In the current layout, timestamps and file chips are positioned directly between the title and body, disrupting the visual flow and making notes hard to read. Fixing this flow provides immediate value to all users.

**Independent Test**: Can be tested independently by viewing notes with titles, body text/checklists, dates, and attachments in both list and grid views, verifying that the body content or checklist directly succeeds the title.

**Acceptance Scenarios**:

1. **Given** a note with a title, creation/modification date, and body text, **When** displayed in list or grid view, **Then** the body text appears immediately below the title, with dates positioned in the card's bottom metadata footer.
2. **Given** a checklist note with a title, attachments, and checklist items, **When** displayed in list or grid view, **Then** the checklist items appear immediately below the title, with attachments represented as compact metadata in the footer.
3. **Given** a note without a title but with body content, **When** displayed, **Then** the body content appears at the top of the card's content area with standard top margin.

---

### User Story 2 - Consistent Metadata Footnote Layout (Priority: P2)

As a user viewing notes with auxiliary information (creation/modified dates, category labels, file attachments), I want metadata consistently placed in a dedicated bottom footer area across both list and grid views, so that card layouts feel structured, predictable, and uncluttered.

**Why this priority**: Notes with combinations of labels, dates, and files currently suffer from irregular vertical gaps and misaligned chips. Consolidating secondary information into a bottom footer establishes visual rhythm and accommodates diverse metadata without cluttering the primary note content.

**Independent Test**: Can be tested independently by creating notes with various combinations of metadata (no metadata, labels only, attachments only, date only, all metadata present) and verifying consistent footer alignment and spacing.

**Acceptance Scenarios**:

1. **Given** a note with labels and an active sort date (creation or modification date), **When** rendered, **Then** the labels and date appear in the bottom metadata section, with labels aligned to the start (left) and the date formatted cleanly.
2. **Given** a note with file attachments, **When** rendered in the overview card, **Then** attachments are summarized with a compact attachment icon and count indicator rather than full-width filename chips.
3. **Given** a note with no labels, attachments, or visible date, **When** rendered, **Then** the card contains no empty footer whitespace and terminates cleanly after the body content.

---

### User Story 3 - Adaptive Reminder Presentation for List and Grid Modes (Priority: P3)

As a user scheduling reminders on notes, I want reminder information presented clearly without causing excessive header wrapping or displacement in narrow grid columns or list rows, so that I can immediately identify upcoming alarms without sacrificing note layout balance.

**Why this priority**: Full reminder chips containing complete dates and times occupy significant horizontal space. In multi-column grid views, long titles combined with full reminder chips cause erratic wrapping or push content downwards.

**Independent Test**: Can be tested independently by rendering notes with short and long titles alongside upcoming and elapsed reminders in both single-column list mode and multi-column grid mode.

**Acceptance Scenarios**:

1. **Given** a note in Grid View with an active reminder, **When** displayed, **Then** the reminder is represented with a compact reminder indicator (e.g. icon with compact relative or abbreviated time) aligned to the top-right of the title row, preventing multiline header sprawl.
2. **Given** a note in List View with an active reminder, **When** displayed, **Then** the reminder chip displays the formatted alarm schedule aligned to the end of the header row, wrapping gracefully below the title only when title text exceeds available width.
3. **Given** a note where the reminder is tapped, **When** the user taps the reminder badge or icon, **Then** the reminder interaction callback triggers to view or adjust the alarm schedule.

---

### Edge Cases

- **Long Title + Active Reminder**: When a note has a multi-line title and an active reminder in narrow column widths, the title truncates according to user display preferences (`maxTitleLines`), and the reminder indicator maintains its top-right anchor without overlapping text.
- **Empty Note with Image Only**: When a note has an attached image but no title or body, the image renders at the top and the card displays appropriate placeholder or empty-state padding without extraneous blank space.
- **Excessive Labels**: When a note has 5+ labels, the footer displays labels wrapping cleanly or constrained gracefully so the card does not become disproportionately tall.
- **Zero Content Note (Placeholder)**: When a note has no title, no body, no checklist, and no attachments, the empty message placeholder ("Empty note" or "Empty list") renders centered with uniform card padding.
- **Checklist with Checked and Unchecked Items**: When rendered in list or grid view, checklist item rows maintain consistent line height and indentation for child items without interfering with footer metadata.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Note cards MUST place note title and primary content (body text or checklist items) sequentially without intervening dates, labels, or file attachments.
- **FR-002**: Note cards MUST display secondary metadata (active sort date, labels, and file attachment summary) in a consolidated footer area below the note content.
- **FR-003**: The card layout MUST support both List View (single full-width column) and Grid View (staggered multi-column) modes with consistent internal padding and spacing standards.
- **FR-004**: In List View, the outer card margins and inner content padding MUST follow a predictable spacing grid: uniform internal padding (12dp), standardized vertical gap between title and content (4dp to 6dp), and standardized spacing before the metadata footer (8dp).
- **FR-005**: In Grid View, element density MUST be optimized for narrower lane widths: outer card margins and internal padding MUST be calibrated (10dp to 12dp internal padding) with compact metadata indicators.
- **FR-006**: File attachments on overview cards MUST be presented as a compact summary (attachment icon with count indicator, e.g., paperclip icon with badge "+2") instead of rendering elongated individual file name chips.
- **FR-007**: Active reminders MUST be anchored in the header region aligned to the top-end (top-right for LTR layouts). In Grid View, the reminder MUST display compactly (icon or icon with abbreviated time) to conserve horizontal space. In List View, the reminder MAY display the full alarm timestamp chip.
- **FR-008**: Creation or modification date timestamps (when enabled by user preferences) MUST be positioned in the bottom metadata footer, styled in small secondary caption typography, aligned cleanly (e.g., end-aligned or paired with labels).
- **FR-009**: Category labels MUST be rendered in the metadata footer with streamlined, compact chip styling (reduced padding and subtle borders) to prevent excessive vertical height expansion.
- **FR-010**: When image previews are present and enabled by preferences, the preview image MUST remain anchored at the very top of the card spanning the full card width, with content padding applied below the image.
- **FR-011**: Tapping the reminder affordance on the card MUST trigger the reminder management action, preserving existing tap target functionality.
- **FR-012**: The card layout MUST respect all user display configuration preferences, including `textSize`, `maxTitleLines`, `maxLines`, `maxItems`, `dateFormat`, `timeFormat`, `sortBy`, `hideLabels`, and `hideImages`.

### Key Entities

- **Note Card Display Hierarchy**: The visual structure defining the vertical arrangement of card elements:
  - *Top Layer*: Image Preview (optional, full-width)
  - *Header Layer*: Title (start-aligned) + Reminder Affordance (end-aligned)
  - *Body Layer*: Note Body Text or Interactive Checklist Items
  - *Footer Layer*: Metadata Footnote containing Attachment Summary Indicator, Label Chips, and Timestamp

- **View Mode Layout Profile**: Distinct layout configuration profiles:
  - *List Profile*: Full-width layout prioritizing detailed information, full reminder chips, and comfortable spacing.
  - *Grid Profile*: Multi-column lane layout prioritizing spatial efficiency, compact summary icons, condensed metadata, and reduced padding.

- **Metadata Footnote**: The bottom container encapsulating secondary note attributes:
  - *Attachment Indicator*: File icon accompanied by total count of attachments.
  - *Label Collection*: Compact badge chips for user-assigned tags.
  - *Timestamp*: Formatted date/time reflecting the current sort order (Creation or Modification).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of note cards display title and body content consecutively without intervening dates or attachments across all supported view configurations.
- **SC-002**: Note card vertical footprint in multi-column Grid View is reduced by at least 15% for notes with attachments and metadata compared to the legacy layout due to compact icon indicators.
- **SC-003**: 100% of existing visual regression and preview tests (including light, dark, and OLED themes) validate without unintended layout clipping or text overlap.
- **SC-004**: User scanability ratings in usability reviews improve: users identify note content and reminders faster due to dedicated header and footer zones.
- **SC-005**: Zero loss of interactive capabilities: reminder tap actions, card selection border indicators, and selection long-press actions function identically to existing behavior.

## Assumptions

- Users prioritize reading note content over reading metadata when browsing their notes overview.
- The existing user preferences for date format, time format, label visibility, image visibility, and text sizes remain authoritative and will continue to dictate which elements appear.
- Audio attachments and file attachments share a unified attachment count indicator in the compact footnote summary.
- The default card background color styling and dynamic theming are preserved without modification.
- Single-line and multi-line titles will truncate with ellipsis according to the configured `maxTitleLines`.

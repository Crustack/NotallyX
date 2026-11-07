# UI Component Contracts: Notes Card Layout and Spacing Redesign

**Feature**: Notes List/Grid - Layout and Spacing Redesign ([Issue #918](https://github.com/Crustack/NotallyX/issues/918))  
**Date**: 2026-10-31  
**Status**: Completed  

---

## 1. `BaseNoteCard` Contract

### Signature
```kotlin
package com.philkes.notallyx.core.ui.components

@Composable
internal fun BaseNoteCard(
    baseNote: BaseNote,
    modifier: Modifier = Modifier,
    isGrid: Boolean = false,
    config: NoteCardDisplayConfig = NoteCardDisplayConfig(),
    imageRoot: File? = if (isPreview()) null else LocalContext.current.getCurrentImagesDirectory(),
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    onReminderClick: (() -> Unit)? = null,
)
```

### Preconditions & Invariants
- `isGrid = true`: Applies `10.dp` inner content padding and compact reminder icon/badge in header.
- `isGrid = false`: Applies `12.dp` inner content padding and full or inline reminder chip.
- Content (body or checklist) directly follows Title without intervening metadata.
- If `baseNote.hasNoContents()` and no images/attachments, displays localized empty note/list text centered or as title fallback.
- `onClick` triggered on whole card tap.
- `onLongClick` triggered on long press for multi-selection.
- `onReminderClick` triggered specifically when the reminder affordance is clicked.

---

## 2. `AttachmentSummaryBadge` Contract

### Signature
```kotlin
package com.philkes.notallyx.core.ui.components

@Composable
internal fun AttachmentSummaryBadge(
    count: Int,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalContentColor.current.copy(alpha = 0.7f),
)
```

### Contract Requirements
- Renders an attachment paperclip/file icon (`size = 14.dp` or `16.dp`) alongside the numeric count (e.g., `"2"`).
- Styled compactly to align seamlessly with `labelSmall` metadata typography.
- Does not render if `count <= 0`.

---

## 3. `NoteFootnoteRow` Contract

### Signature
```kotlin
package com.philkes.notallyx.core.ui.components

@Composable
internal fun NoteFootnoteRow(
    attachmentCount: Int,
    labels: List<String>,
    timestampText: String?,
    modifier: Modifier = Modifier,
    contentColor: Color = LocalContentColor.current,
)
```

### Contract Requirements
- If `attachmentCount == 0`, `labels.isEmpty()`, and `timestampText == null`, this composable emits nothing and consumes 0dp height.
- Uses `FlowRow` with `horizontalArrangement = Arrangement.spacedBy(6.dp)` and `verticalArrangement = Arrangement.spacedBy(4.dp)`.
- Left-aligns `AttachmentSummaryBadge` and `LabelChip` items.
- Aligns or terminates with `timestampText` styled with `MaterialTheme.typography.labelSmall`.

---

## 4. `ReminderAffordance` Contract

### Signature
```kotlin
package com.philkes.notallyx.core.ui.components

@Composable
internal fun ReminderAffordance(
    baseNote: BaseNote,
    isGrid: Boolean,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    textSize: Float,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
)
```

### Contract Requirements
- If `baseNote.reminders.isEmpty()`, renders nothing.
- If `isGrid = true`: renders a compact, circular or subtle surface with bell/recurring icon (`size = 16.dp`), optimized for small widths, with minimum 24dp clickable bounds.
- If `isGrid = false`: renders `ReminderChip` displaying formatted alarm date/time.
- Captures click events to execute `onClick` independently of card navigation.

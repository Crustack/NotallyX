# Data Model: Notes Card Layout and Spacing Redesign

**Feature**: Notes List/Grid - Layout and Spacing Redesign ([Issue #918](https://github.com/Crustack/NotallyX/issues/918))  
**Date**: 2026-10-31  
**Status**: Completed  

---

## 1. Overview
This document specifies the UI model hierarchy, spacing tokens, and configuration attributes governing `BaseNoteCard` rendering across List View and Grid View modes.

---

## 2. Component Layout Hierarchy Model

```
BaseNoteCard
├── Card Container
│   ├── Stroke / Selection Border
│   └── Column
│       ├── [Media Slot] (optional)
│       │   └── ImagePreview (aspectRatio 8:5)
│       │
│       └── Content Container (Padding: 12dp list / 10dp grid)
│           ├── [Header Slot]
│           │   ├── Title Text (start-aligned, maxLines = config.maxTitleLines)
│           │   └── Reminder Affordance (end-aligned; full chip in list, compact icon in grid)
│           │
│           ├── Spacer (height = 4dp, if title is present)
│           │
│           ├── [Primary Content Slot]
│           │   ├── Case Type.NOTE: Note Body Text (formatted spans, maxLines = config.maxLines)
│           │   └── Case Type.LIST: Checklist Column (items up to config.maxItems + overflow counter)
│           │
│           └── [Metadata Footnote Slot] (rendered if files, labels, or date present)
│               ├── Spacer (height = 8dp)
│               └── Footnote FlowRow / Row:
│                   ├── Attachment Indicator (📎 + count, if files.isNotEmpty())
│                   ├── Label Chips (collection of compact tags, if !config.hideLabels)
│                   └── Sort Timestamp (Creation or Modified date, labelSmall typography)
```

---

## 3. Layout Spacing Tokens

| Spacing Token | List View (`isGrid = false`) | Grid View (`isGrid = true`) | Description |
|---|---|---|---|
| `CardOuterPadding` | `4.dp` horizontal, `4.dp` vertical | `4.dp` horizontal, `4.dp` vertical | Outer margin around card in grid/list container |
| `ContentInnerPadding` | `12.dp` uniform | `10.dp` uniform | Padding inside the card content column |
| `TitleToBodyGap` | `4.dp` | `4.dp` | Vertical distance between title and note body / checklist |
| `ContentToFootnoteGap`| `8.dp` | `8.dp` | Vertical separation between body/checklist and footnote metadata |
| `FootnoteItemGap` | `6.dp` | `6.dp` | Horizontal spacing between metadata items (files, labels, date) |
| `FootnoteLineGap` | `4.dp` | `4.dp` | Vertical spacing between wrapped metadata rows |
| `LabelHorizontalPadding`| `8.dp` | `8.dp` | Internal horizontal padding of individual `LabelChip` |
| `LabelVerticalPadding` | `2.dp` | `2.dp` | Internal vertical padding of individual `LabelChip` |

---

## 4. UI Configuration Data Class

```kotlin
/**
 * Configuration options dictating how [BaseNoteCard] displays contents and metadata.
 */
data class NoteCardDisplayConfig(
    val dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    val timeFormat: TimeFormat = TimeFormat.NONE,
    val sortBy: NotesSortBy = NotesSortBy.CREATION_DATE,
    val textSize: Float = 16f,
    val maxTitleLines: Int = 2,
    val maxLines: Int = 10,
    val maxItems: Int = 5,
    val hideLabels: Boolean = false,
    val hideImages: Boolean = false,
)
```

---

## 5. Footnote State Derivation Model

```kotlin
data class NoteFootnoteData(
    val attachmentCount: Int,
    val labels: List<String>,
    val timestampText: String?,
) {
    val isVisible: Boolean
        get() = attachmentCount > 0 || labels.isNotEmpty() || timestampText != null
}
```

- **`attachmentCount`**: `baseNote.files.size + baseNote.audios.size` (or `baseNote.files.size`).
- **`labels`**: `if (config.hideLabels) emptyList() else baseNote.labels`.
- **`timestampText`**: Derived from `config.sortBy` (`CREATION_DATE` -> `baseNote.timestamp`, `MODIFIED_DATE` -> `baseNote.modifiedTimestamp`) when formatted with `config.dateFormat` and `config.timeFormat`. Returns `null` if both formats are `NONE` or sort date is unavailable.

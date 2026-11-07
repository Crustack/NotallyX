package com.philkes.notallyx.reminder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.Theme
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.components.BaseNoteCard
import com.philkes.notallyx.core.ui.components.NoteCardDisplayConfig
import com.philkes.notallyx.core.ui.components.ReminderChip
import com.philkes.notallyx.core.ui.components.SampleNotes
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.feature.reminder.FilterOptions
import com.philkes.notallyx.feature.reminder.RemindersScreen
import java.util.Date

private val UPCOMING_REMINDER =
    Reminder(
        id = 1L,
        dateTime = Date(1956571200000L), // Fixed future date (2032-01-02 12:00)
        repetition = null,
    )

private val REPEATING_REMINDER =
    Reminder(
        id = 2L,
        dateTime = Date(1956657600000L), // Fixed future date (2032-01-03 12:00)
        repetition = Repetition(1, RepetitionTimeUnit.DAYS),
    )

private val ELAPSED_REMINDER =
    Reminder(
        id = 3L,
        dateTime = Date(1640995200000L), // 2022-01-01 00:00 (Elapsed)
        repetition = null,
    )

private val NOTE_WITH_UPCOMING =
    SampleNotes.LOREM_IPSUM_NOTE.copy(
        id = 101L,
        title = "Sprint Planning Meeting",
        reminders = listOf(UPCOMING_REMINDER),
    )

private val NOTE_WITH_REPEATING =
    SampleNotes.LIST_NOTE.copy(
        id = 102L,
        title = "Daily Grocery Checklist",
        reminders = listOf(REPEATING_REMINDER),
    )

private val NOTE_WITH_ELAPSED =
    SampleNotes.RICH_TEXT_NOTE.copy(
        id = 103L,
        title = "Expired Project Deadline",
        reminders = listOf(ELAPSED_REMINDER),
    )

private val ALL_REMINDER_NOTES = listOf(NOTE_WITH_UPCOMING, NOTE_WITH_REPEATING, NOTE_WITH_ELAPSED)

@Composable
private fun CardScreenshotWrapper(content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(360.dp).padding(8.dp)) { content() }
}

// ==================== ReminderChip Component Previews ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ReminderChip_Upcoming_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ReminderChip(
            reminders = listOf(UPCOMING_REMINDER),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ReminderChip_Upcoming_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        ReminderChip(
            reminders = listOf(UPCOMING_REMINDER),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ReminderChip_Upcoming_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        ReminderChip(
            reminders = listOf(UPCOMING_REMINDER),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ReminderChip_Elapsed_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ReminderChip(
            reminders = listOf(ELAPSED_REMINDER),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ReminderChip_Elapsed_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        ReminderChip(
            reminders = listOf(ELAPSED_REMINDER),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ReminderChip_WithRepetition_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        ReminderChip(
            reminders = listOf(REPEATING_REMINDER),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun ReminderChip_WithRepetition_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        ReminderChip(
            reminders = listOf(REPEATING_REMINDER),
            dateFormat = DateFormat.DD_MM_YY_GER,
            timeFormat = TimeFormat.TWENTY_FOUR_H,
        )
    }
}

// ==================== BaseNoteCard with Reminders ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_UpcomingReminder_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = NOTE_WITH_UPCOMING) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_UpcomingReminder_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = NOTE_WITH_UPCOMING) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_UpcomingReminder_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = NOTE_WITH_UPCOMING) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_ElapsedReminder_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = NOTE_WITH_ELAPSED) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_ElapsedReminder_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = NOTE_WITH_ELAPSED) }
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun BaseNoteCard_ElapsedReminder_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        CardScreenshotWrapper { BaseNoteCard(baseNote = NOTE_WITH_ELAPSED) }
    }
}

// ==================== RemindersScreen Layout & Filter States ====================

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_UpcomingFilter_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        RemindersScreen(
            notes = ALL_REMINDER_NOTES,
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.UPCOMING,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_UpcomingFilter_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        RemindersScreen(
            notes = ALL_REMINDER_NOTES,
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.UPCOMING,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_ElapsedFilter_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        RemindersScreen(
            notes = ALL_REMINDER_NOTES,
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.ELAPSED,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_ElapsedFilter_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        RemindersScreen(
            notes = ALL_REMINDER_NOTES,
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.ELAPSED,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_AllFilter_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        RemindersScreen(
            notes = ALL_REMINDER_NOTES,
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.ALL,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_AllFilter_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        RemindersScreen(
            notes = ALL_REMINDER_NOTES,
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.ALL,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_Empty_Light() {
    NotallyXTheme(theme = Theme.LIGHT) {
        RemindersScreen(
            notes = emptyList(),
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.UPCOMING,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_Empty_Dark() {
    NotallyXTheme(theme = Theme.DARK) {
        RemindersScreen(
            notes = emptyList(),
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.UPCOMING,
        )
    }
}

@Preview(showBackground = true)
@PreviewTest
@Composable
fun RemindersScreen_Empty_SuperDark() {
    NotallyXTheme(theme = Theme.SUPER_DARK) {
        RemindersScreen(
            notes = emptyList(),
            selectedNoteIds = emptySet(),
            isGrid = false,
            displayConfig = NoteCardDisplayConfig(),
            onNoteClick = { _, _ -> },
            onNoteLongClick = { _, _, _ -> },
            onReminderClick = { _, _ -> },
            initialFilterMode = FilterOptions.UPCOMING,
        )
    }
}

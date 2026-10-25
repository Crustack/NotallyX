package com.philkes.notallyx.misc.appstore

import androidx.compose.ui.tooling.preview.Preview
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.FileAttachment
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.ListItem
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Reminder
import com.philkes.notallyx.data.model.Repetition
import com.philkes.notallyx.data.model.RepetitionTimeUnit
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.toColorString
import java.util.Date

/**
 * Multi-preview annotation for generating app store marketing screenshots across multiple locales
 * (English, German, etc.) as described in
 * docs/generate_app_store_screenshots_directly_from_jetpack_compose_previews.md.
 */
@Preview(
    name = "en-US",
    locale = "en",
    widthDp = 572,
    heightDp = 1024,
    showBackground = true,
)
@Preview(
    name = "de-DE",
    locale = "de",
    widthDp = 572,
    heightDp = 1024,
    showBackground = true,
)
annotation class MarketingPreview

object MarketingSampleData {
    val DAILY_JOURNAL_NOTE =
        BaseNote(
            id = 1L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = BaseNote.COLOR_DEFAULT,
            title = "Daily Journal",
            pinned = false,
            timestamp = System.currentTimeMillis() - 86400000L,
            modifiedTimestamp = System.currentTimeMillis(),
            labels = emptyList(),
            body =
                "Date: October 19, 2024\n\nToday was a productive day! I managed to:\n- Finish the presentation for Monday\n- Attend the team meeting\n- Go for a 30-minute run\n\nLooking forward to a relaxing weekend ahead.",
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files =
                listOf(
                    FileAttachment(
                        localName = "document.pdf",
                        originalName = "document.pdf",
                        mimeType = "application/pdf",
                    )
                ),
            audios = emptyList(),
            reminders =
                listOf(
                    Reminder(
                        id = 1L,
                        dateTime = Date(System.currentTimeMillis() + 3600000L),
                        repetition = Repetition(1, RepetitionTimeUnit.DAYS),
                    )
                ),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val FORMATTING_BODY =
        "This is a bold text.\nThis is italic text.\nThis is strikethrough text.\nAnd this is a code block in monospace font."

    val FORMATTING_SPANS =
        listOf(
            SpanRepresentation(start = 0, end = 20, bold = true),
            SpanRepresentation(start = 21, end = 41, italic = true),
            SpanRepresentation(start = 42, end = 69, strikethrough = true),
            SpanRepresentation(start = 70, end = 113, monospace = true),
        )

    val FORMATTING_NOTE =
        BaseNote(
            id = 2L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.CLAY.toColorString(),
            title = "Formatting",
            pinned = false,
            timestamp = System.currentTimeMillis() - 43200000L,
            modifiedTimestamp = System.currentTimeMillis(),
            labels = emptyList(),
            body = FORMATTING_BODY,
            spans = FORMATTING_SPANS,
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val GROCERY_LIST_NOTE =
        BaseNote(
            id = 3L,
            type = Type.LIST,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.FOG.toColorString(),
            title = "Grocery List",
            pinned = false,
            timestamp = System.currentTimeMillis() - 21600000L,
            modifiedTimestamp = System.currentTimeMillis(),
            labels = listOf("Todo"),
            body = "",
            spans = emptyList(),
            items =
                listOf(
                    ListItem(
                        "Breakfast",
                        checked = false,
                        isChild = false,
                        order = 0,
                        children = mutableListOf(),
                        id = 1,
                    ),
                    ListItem(
                        "Coffee",
                        checked = false,
                        isChild = true,
                        order = 1,
                        children = mutableListOf(),
                        id = 2,
                    ),
                    ListItem(
                        "Bread",
                        checked = true,
                        isChild = true,
                        order = 2,
                        children = mutableListOf(),
                        id = 3,
                    ),
                    ListItem(
                        "Cleaning",
                        checked = false,
                        isChild = false,
                        order = 3,
                        children = mutableListOf(),
                        id = 4,
                    ),
                    ListItem(
                        "Dish soap",
                        checked = false,
                        isChild = true,
                        order = 4,
                        children = mutableListOf(),
                        id = 5,
                    ),
                    ListItem(
                        "Sponge",
                        checked = true,
                        isChild = true,
                        order = 5,
                        children = mutableListOf(),
                        id = 6,
                    ),
                    ListItem(
                        "Snacks",
                        checked = false,
                        isChild = false,
                        order = 6,
                        children = mutableListOf(),
                        id = 7,
                    ),
                    ListItem(
                        "Cookies",
                        checked = false,
                        isChild = true,
                        order = 7,
                        children = mutableListOf(),
                        id = 8,
                    ),
                    ListItem(
                        "Chips",
                        checked = false,
                        isChild = true,
                        order = 8,
                        children = mutableListOf(),
                        id = 9,
                    ),
                ),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val VACATION_NOTE =
        BaseNote(
            id = 4L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.DUSK.toColorString(),
            title = "Vacation",
            pinned = false,
            timestamp = System.currentTimeMillis() - 10800000L,
            modifiedTimestamp = System.currentTimeMillis(),
            labels = emptyList(),
            body =
                "Here are some places I'd like to visit this summer:\n- Beach Resort in Maldives\n- Mountain Hiking in Switzerland\n- City Tour in Tokyo",
            spans = emptyList(),
            items = emptyList(),
            images =
                listOf(
                    FileAttachment(
                        localName = "maldives.jpg",
                        originalName = "Maldives.jpg",
                        mimeType = "image/jpeg",
                    ),
                    FileAttachment(
                        localName = "swiss.jpg",
                        originalName = "Switzerland.jpg",
                        mimeType = "image/jpeg",
                    ),
                    FileAttachment(
                        localName = "tokyo.jpg",
                        originalName = "Tokyo.jpg",
                        mimeType = "image/jpeg",
                    ),
                ),
            files =
                listOf(
                    FileAttachment(
                        localName = "itinerary.pdf",
                        originalName = "Trip_Itinerary.pdf",
                        mimeType = "application/pdf",
                    )
                ),
            audios = emptyList(),
            reminders =
                listOf(
                    Reminder(
                        id = 2L,
                        dateTime = Date(System.currentTimeMillis() + 86400000L),
                        repetition = null,
                    )
                ),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val DINNER_BUCKETLIST_NOTE =
        BaseNote(
            id = 5L,
            type = Type.LIST,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.BLOSSOM.toColorString(),
            title = "Dinner Bucketlist",
            pinned = false,
            timestamp = System.currentTimeMillis() - 5400000L,
            modifiedTimestamp = System.currentTimeMillis(),
            labels = listOf("Todo"),
            body = "",
            spans = emptyList(),
            items =
                listOf(
                    ListItem(
                        "🌯 Burrito",
                        checked = false,
                        isChild = false,
                        order = 0,
                        children = mutableListOf(),
                        id = 10,
                    ),
                    ListItem(
                        "🥪 Philly Cheesesteak",
                        checked = false,
                        isChild = false,
                        order = 1,
                        children = mutableListOf(),
                        id = 11,
                    ),
                    ListItem(
                        "🥟 Pierogi",
                        checked = false,
                        isChild = false,
                        order = 2,
                        children = mutableListOf(),
                        id = 12,
                    ),
                    ListItem(
                        "🍜 Ramen",
                        checked = true,
                        isChild = false,
                        order = 3,
                        children = mutableListOf(),
                        id = 13,
                    ),
                ),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders = emptyList(),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val DOCTOR_APPOINTMENT_NOTE =
        BaseNote(
            id = 6L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.MINT.toColorString(),
            title = "Doctor Appointment",
            pinned = false,
            timestamp = System.currentTimeMillis() - 7200000L,
            modifiedTimestamp = System.currentTimeMillis(),
            labels = listOf("Health"),
            body = "Annual checkup at City Health Clinic with Dr. Miller.",
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders =
                listOf(
                    Reminder(
                        id = 3L,
                        dateTime = Date(System.currentTimeMillis() + 172800000L),
                        repetition = null,
                    )
                ),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val CAR_SERVICE_NOTE =
        BaseNote(
            id = 7L,
            type = Type.NOTE,
            folder = Folder.NOTES,
            color = com.philkes.notallyx.data.model.Color.SAND.toColorString(),
            title = "Car Service",
            pinned = false,
            timestamp = System.currentTimeMillis() - 3600000L,
            modifiedTimestamp = System.currentTimeMillis(),
            labels = emptyList(),
            body = "Scheduled oil change, brake inspection, and tire rotation.",
            spans = emptyList(),
            items = emptyList(),
            images = emptyList(),
            files = emptyList(),
            audios = emptyList(),
            reminders =
                listOf(
                    Reminder(
                        id = 4L,
                        dateTime = Date(System.currentTimeMillis() + 259200000L),
                        repetition = null,
                    )
                ),
            viewMode = NoteViewMode.EDIT,
            isPinnedToStatus = false,
        )

    val MAIN_NOTES =
        listOf(
            DAILY_JOURNAL_NOTE,
            FORMATTING_NOTE,
            GROCERY_LIST_NOTE,
            VACATION_NOTE,
            DINNER_BUCKETLIST_NOTE,
        )

    val REMINDER_NOTES =
        listOf(
            VACATION_NOTE,
            DAILY_JOURNAL_NOTE,
            DOCTOR_APPOINTMENT_NOTE,
            CAR_SERVICE_NOTE,
        )
}

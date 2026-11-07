package com.philkes.notallyx.core.ui.components

import com.philkes.notallyx.core.preference.model.UserPreferences

fun UserPreferences.toDisplayConfig(): NoteCardDisplayConfig =
    NoteCardDisplayConfig(
        dateFormat = dateFormatOverview,
        timeFormat = timeFormatOverview,
        sortBy = notesSorting.sortedBy,
        textSize = textSizeOverview,
        maxTitleLines = maxTitle,
        maxLines = maxLines,
        maxItems = maxItems,
        hideLabels = labelTagsHiddenInOverview,
        hideImages = imagesHiddenInOverview,
    )

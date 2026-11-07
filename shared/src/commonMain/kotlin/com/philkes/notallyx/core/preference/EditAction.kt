package com.philkes.notallyx.core.preference

import kotlinx.serialization.Serializable

@Serializable
enum class EditAction {
    SEARCH,
    PIN,
    REMINDERS,
    LABELS,
    CHANGE_COLOR,
    DUPLICATE,
    EXPORT,
    SHARE,
    DELETE,
    ARCHIVE,
    TOGGLE_VIEW_MODE,
    CONVERT,
    DELETE_FOREVER,
    RESTORE,
    PIN_TO_STATUS,
}

@Serializable
enum class EditListAction {
    DELETE_CHECKED,
    CHECK_ALL,
    UNCHECK_ALL,
}

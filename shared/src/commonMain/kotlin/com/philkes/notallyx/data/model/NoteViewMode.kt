package com.philkes.notallyx.data.model

enum class NoteViewMode {
    READ_ONLY,
    EDIT;

    companion object {
        fun valueOfOrDefault(value: String) =
            try {
                NoteViewMode.valueOf(value)
            } catch (e: Exception) {
                EDIT
            }
    }
}

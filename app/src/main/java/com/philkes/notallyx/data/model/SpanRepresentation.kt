package com.philkes.notallyx.data.model

import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH

data class SpanRepresentation(
    var start: Int,
    var end: Int,
    var bold: Boolean = false,
    var link: Boolean = false,
    var linkData: String? = null,
    var italic: Boolean = false,
    var monospace: Boolean = false,
    var strikethrough: Boolean = false,
) {

    fun isNotUseless(): Boolean {
        return bold || link || italic || monospace || strikethrough
    }

    fun isEqualInSize(representation: SpanRepresentation): Boolean {
        return start == representation.start && end == representation.end
    }

    fun isInsideBounds(): Boolean = start < MAX_BODY_CHAR_LENGTH && end <= MAX_BODY_CHAR_LENGTH
}

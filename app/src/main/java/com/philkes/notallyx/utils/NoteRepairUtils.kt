package com.philkes.notallyx.utils

import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH

/**
 * Utilities to repair oversized note rows that can cause SQLiteBlobTooBigException during reads.
 */
object NoteRepairUtils {

    /**
     * Truncate the body of the note with [id] to [MAX_BODY_CHAR_LENGTH] characters (if needed) and
     * clip spans to the new body length. Re-inserts the note with REPLACE semantics.
     */
    suspend fun truncateBodyAndFixSpans(dao: BaseNoteDao, id: Long) {
        // First, truncate at DB level to avoid re-triggering blob-too-big on read
        dao.truncateBody(id, MAX_BODY_CHAR_LENGTH)

        // Now load the row and clip spans to the truncated length
        val existing = dao.get(id) ?: return
        val newBody = existing.body
        val clippedSpans =
            if (existing.spans.isNotEmpty())
                NoteSplitUtils.sliceSpans(existing.spans, 0, newBody.length, 0)
            else emptyList()

        val repaired = existing.copy(spans = clippedSpans)
        // REPLACE existing row with clipped spans
        dao.insert(repaired)
    }
}

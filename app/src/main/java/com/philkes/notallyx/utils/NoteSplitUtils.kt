package com.philkes.notallyx.utils

import com.philkes.notallyx.data.dao.BaseNoteDao
import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.createNoteUrl

/**
 * Shared utilities for splitting oversized text notes while preserving spans and adding navigation
 * links between parts.
 */
object NoteSplitUtils {

    const val LINK_TEXT: String = "\n\nOpen next part"

    /**
     * Returns spans that intersect with [rangeStart, rangeEnd) from [spans], with their positions
     * shifted by [shift]. Typically, use shift = -rangeStart so that returned spans are relative to
     * the start of the sliced body substring.
     */
    fun sliceSpans(
        spans: List<SpanRepresentation>,
        rangeStart: Int,
        rangeEnd: Int,
        shift: Int,
    ): List<SpanRepresentation> {
        if (spans.isEmpty()) return emptyList()
        val result = ArrayList<SpanRepresentation>()
        for (s in spans) {
            if (s.end <= rangeStart || s.start >= rangeEnd) continue
            val newStart = maxOf(s.start, rangeStart) + shift
            val newEnd = minOf(s.end, rangeEnd) + shift
            if (newEnd <= newStart) continue
            result.add(
                SpanRepresentation(
                    start = newStart,
                    end = newEnd,
                    bold = s.bold,
                    link = s.link,
                    linkData = s.linkData,
                    italic = s.italic,
                    monospace = s.monospace,
                    strikethrough = s.strikethrough,
                )
            )
        }
        return result
    }

    /**
     * Produce body chunks for [note] such that each non-final chunk reserves bytes for the link
     * text. Spans are sliced and shifted relative to each chunk.
     */
    fun sliceIntoChunks(
        note: BaseNote,
        linkTextLength: Int,
    ): List<Pair<String, List<SpanRepresentation>>> {
        val result = ArrayList<Pair<String, List<SpanRepresentation>>>()
        val body = note.body
        val spans = note.spans
        val allowedWithLink = MAX_BODY_CHAR_LENGTH - linkTextLength
        var startIndex = 0

        while (true) {
            val remainingLen = body.length - startIndex
            if (remainingLen <= MAX_BODY_CHAR_LENGTH) {
                val finalBody = body.substring(startIndex)
                val finalSpans = sliceSpans(spans, startIndex, body.length, -startIndex)
                result.add(Pair(finalBody, finalSpans))
                break
            }
            val chunkLen = allowedWithLink.coerceAtLeast(0)
            val chunkBody = body.substring(startIndex, startIndex + chunkLen)
            val chunkSpans = sliceSpans(spans, startIndex, startIndex + chunkLen, -startIndex)
            result.add(Pair(chunkBody, chunkSpans))
            startIndex += chunkLen
        }
        return result
    }

    /**
     * Splits an oversized text note into multiple parts for import, inserting them into the DB.
     * Returns a pair of (firstPartId, list of (insertedId, insertedSpans)). Spans are preserved per
     * chunk and navigation link spans are appended to all non-final parts.
     */
    suspend fun splitAndInsertForImport(
        original: BaseNote,
        dao: BaseNoteDao,
    ): Pair<Long, List<Pair<Long, List<SpanRepresentation>>>> {
        check(original.type == Type.NOTE)
        val linkText = LINK_TEXT
        val linkTextLength = linkText.length

        val chunks = sliceIntoChunks(original, linkTextLength)

        // Insert from last to first so we have the nextId available when creating link spans
        var nextId: Long? = null
        val inserted = ArrayList<Pair<Long, List<SpanRepresentation>>>(chunks.size)
        for (i in chunks.lastIndex downTo 0) {
            val (chunkBody, chunkSpans) = chunks[i]

            val body = if (nextId != null) chunkBody + linkText else chunkBody
            val spans =
                if (nextId != null) {
                    val linkStart = chunkBody.length + ("\n\n").length
                    val linkEnd = linkStart + ("Open next part").length
                    val linkSpan =
                        SpanRepresentation(
                            start = linkStart,
                            end = linkEnd,
                            link = true,
                            linkData = nextId!!.createNoteUrl(original.type),
                        )
                    chunkSpans + linkSpan
                } else chunkSpans

            val title = if (i == 0) original.title else "${original.title} (${i})"
            val toInsert =
                original.copy(
                    id = 0,
                    title = title,
                    body = body,
                    spans = spans,
                    items = emptyList(),
                )
            val newId = dao.insert(toInsert)
            inserted.add(Pair(newId, spans))
            nextId = newId
        }
        // nextId now holds the id of the first part (inserted last in the loop)
        return Pair(requireNotNull(nextId), inserted)
    }

    /**
     * Migration helper: Split an existing oversized note in-place by updating the current note to
     * the truncated chunk + link, and creating following parts for the remaining text. Returns the
     * number of created subsequent notes.
     */
    suspend fun splitOversizedExistingNoteForMigration(original: BaseNote, dao: BaseNoteDao): Int {
        check(original.type == Type.NOTE)
        val linkText = LINK_TEXT
        val linkTextLength = linkText.length

        var current = original
        var counter = 0
        val originalBody = original.body
        val originalSpans = original.spans
        var startIndex = 0

        val allowedWithLink = MAX_BODY_CHAR_LENGTH - linkTextLength

        while (true) {
            val remainingLen = originalBody.length - startIndex
            if (remainingLen <= MAX_BODY_CHAR_LENGTH) {
                // Final part: no link text, just update current with the rest and correct spans
                val finalBody = originalBody.substring(startIndex)
                val finalSpans =
                    sliceSpans(originalSpans, startIndex, originalBody.length, shift = -startIndex)
                val updatedFinal =
                    current.copy(body = finalBody, spans = finalSpans, items = emptyList())
                dao.insert(updatedFinal) // replace existing current
                break
            }

            // Non-final part: reserve space for link text
            val chunkLen = allowedWithLink.coerceAtLeast(0)
            val chunkBody = originalBody.substring(startIndex, startIndex + chunkLen)
            val chunkSpans =
                sliceSpans(originalSpans, startIndex, startIndex + chunkLen, shift = -startIndex)

            // Create the next note from the remaining text (beyond this chunk)
            val nextStart = startIndex + chunkLen
            val nextBodyFull = originalBody.substring(nextStart)
            val nextSpansFull =
                sliceSpans(originalSpans, nextStart, originalBody.length, shift = -nextStart)

            // Insert the next note first to obtain its id for the link
            val nextNoteToInsert =
                current.copy(
                    id = 0, // auto-generate new ID
                    title = "${original.title} (${++counter})",
                    body = nextBodyFull,
                    spans = nextSpansFull,
                    items = emptyList(),
                )
            val nextId = dao.insert(nextNoteToInsert)

            // Append link to current chunk
            val linkStart = chunkBody.length + ("\n\n").length
            val linkEnd = linkStart + ("Open next part").length
            val linkSpan =
                SpanRepresentation(
                    start = linkStart,
                    end = linkEnd,
                    link = true,
                    linkData = nextId.createNoteUrl(current.type),
                )

            val updatedCurrent =
                current.copy(
                    body = chunkBody + linkText,
                    spans = chunkSpans + linkSpan,
                    items = emptyList(),
                )
            dao.insert(updatedCurrent)

            // Continue with the newly created note as current
            current = nextNoteToInsert.copy(id = nextId)
            startIndex = nextStart
        }

        return counter
    }
}

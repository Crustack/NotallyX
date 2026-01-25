package com.philkes.notallyx.data.dao

import androidx.room.Dao
import androidx.room.Transaction
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.dao.BaseNoteDao.Companion.MAX_BODY_CHAR_LENGTH
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Label
import com.philkes.notallyx.data.model.LabelsInBaseNote
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.createNoteUrl
import com.philkes.notallyx.data.model.getNoteIdFromUrl
import com.philkes.notallyx.data.model.getNoteTypeFromUrl
import com.philkes.notallyx.data.model.isNoteUrl
import com.philkes.notallyx.utils.NoteSplitUtils

@Dao
abstract class CommonDao(private val database: NotallyDatabase) {

    @Transaction
    open suspend fun deleteLabel(value: String) {
        val labelsInBaseNotes =
            database.getBaseNoteDao().getListOfBaseNotesByLabel(value).map { baseNote ->
                val labels = ArrayList(baseNote.labels)
                labels.remove(value)
                LabelsInBaseNote(baseNote.id, labels)
            }
        database.getBaseNoteDao().update(labelsInBaseNotes)
        database.getLabelDao().delete(value)
    }

    @Transaction
    open suspend fun updateLabel(oldValue: String, newValue: String) {
        val labelsInBaseNotes =
            database.getBaseNoteDao().getListOfBaseNotesByLabel(oldValue).map { baseNote ->
                val labels = ArrayList(baseNote.labels)
                labels.remove(oldValue)
                labels.add(newValue)
                LabelsInBaseNote(baseNote.id, labels)
            }
        database.getBaseNoteDao().update(labelsInBaseNotes)
        database.getLabelDao().update(oldValue, newValue)
    }

    @Transaction
    open suspend fun importBackup(baseNotes: List<BaseNote>, labels: List<Label>) {
        val dao = database.getBaseNoteDao()
        // Insert notes, splitting oversized text notes instead of truncating
        baseNotes.forEach { note ->
            if (note.type == Type.NOTE && note.body.length > MAX_BODY_CHAR_LENGTH) {
                NoteSplitUtils.splitAndInsertForImport(note, dao)
            } else {
                dao.insert(note.copy(id = 0))
            }
        }
        database.getLabelDao().insert(labels)
    }

    /**
     * Import backup with remapping of note links inside spans. Uses a single bulk insert to obtain
     * new IDs, builds an oldId->newId mapping based on [originalIds] order, then rewrites any
     * note:// links in spans to reference the newly created IDs.
     */
    @Transaction
    open suspend fun importBackup(
        baseNotes: List<BaseNote>,
        originalIds: List<Long>,
        labels: List<Label>,
    ) {
        val baseNoteDao = database.getBaseNoteDao()

        // 1) Insert notes with splitting; build mapping from original id -> first-part new id
        val idMap = HashMap<Long, Long>(originalIds.size)
        // Keep all inserted note ids with their spans for remapping pass
        val insertedParts = ArrayList<Pair<Long, List<SpanRepresentation>>>()

        for (i in baseNotes.indices) {
            val original = baseNotes[i]
            val (firstId, parts) =
                if (original.type == Type.NOTE && original.body.length > MAX_BODY_CHAR_LENGTH) {
                    NoteSplitUtils.splitAndInsertForImport(original, baseNoteDao)
                } else {
                    val newId = baseNoteDao.insert(original.copy(id = 0))
                    Pair(newId, listOf(Pair(newId, original.spans)))
                }
            val oldId = originalIds.getOrNull(i)
            if (oldId != null) idMap[oldId] = firstId
            insertedParts.addAll(parts)
        }

        // 2) Remap note links in spans for all inserted notes
        for ((noteId, spans) in insertedParts) {
            var changed = false
            val updated =
                spans.map { span ->
                    if (span.link && span.linkData?.isNoteUrl() == true) {
                        val url = span.linkData!!
                        val oldTargetId = url.getNoteIdFromUrl()
                        val type = url.getNoteTypeFromUrl()
                        val newTargetId = idMap[oldTargetId]
                        if (newTargetId != null) {
                            changed = true
                            span.copy(linkData = newTargetId.createNoteUrl(type))
                        } else span
                    } else span
                }
            if (changed) {
                baseNoteDao.updateSpans(noteId, updated)
            }
        }

        database.getLabelDao().insert(labels)
    }
}

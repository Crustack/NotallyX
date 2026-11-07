package com.philkes.notallyx.core.preference

import com.philkes.notallyx.core.common.extensions.deserializeEnums
import com.philkes.notallyx.core.common.extensions.fromCamelCaseToEnumName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

object LegacyNotallyXPreferences {
    val HANDLED_LEGACY_KEYS =
        setOf(
            "theme",
            "dateFormatOverview",
            "timeFormatOverview",
            "dateFormatNoteView",
            "timeFormatNoteView",
            "view",
            "notesView",
            "notesSorting",
            "notesSortingDirection",
            "listItemSorting",
            "defaultListNoteViewMode",
            "editNoteActivityBottomAction",
            "editNoteActivityTopActions",
            "maxItemsToDisplayInList.v1",
            "maxItems",
            "maxLinesToDisplayInNote.v1",
            "maxLines",
            "maxLinesToDisplayInTitle",
            "maxTitle",
            "labelsHiddenInNavigation",
            "labelsHidden",
            "labelsHiddenInOverview",
            "labelTagsHiddenInOverview",
            "maxLabelsInNavigation",
            "maxLabels",
            "autoBackup",
            "backupsFolder",
            "autoBackupPeriodDays",
            "maxBackups",
            "periodicBackups",
            "widgets",
            "widgetNoteTypes",
        )

    val DISTINCT_LEGACY_KEYS =
        setOf(
            "view",
            "autoBackup",
            "maxItemsToDisplayInList.v1",
            "maxLinesToDisplayInNote.v1",
            "maxLinesToDisplayInTitle",
            "labelsHiddenInNavigation",
            "labelsHiddenInOverview",
            "maxLabelsInNavigation",
            "autoBackupPeriodDays",
            "notesSortingDirection",
        )

    fun isLegacyJson(raw: JsonObject): Boolean {
        return raw.keys.any { key ->
            (key in DISTINCT_LEGACY_KEYS) ||
                key.startsWith("widget:") ||
                key.startsWith("widgetNoteType:")
        }
    }

    fun normalize(raw: JsonObject, ignoredKeys: Set<String> = emptySet()): JsonObject {
        val result = mutableMapOf<String, JsonElement>()

        raw["theme"].parseEnumName<Theme>()?.let { result["theme"] = JsonPrimitive(it) }
        raw["dateFormatOverview"].parseEnumName<DateFormat>()?.let {
            result["dateFormatOverview"] = JsonPrimitive(it)
        }
        raw["timeFormatOverview"].parseEnumName<TimeFormat>()?.let {
            result["timeFormatOverview"] = JsonPrimitive(it)
        }
        raw["dateFormatNoteView"].parseEnumName<DateFormat>()?.let {
            result["dateFormatNoteView"] = JsonPrimitive(it)
        }
        raw["timeFormatNoteView"].parseEnumName<TimeFormat>()?.let {
            result["timeFormatNoteView"] = JsonPrimitive(it)
        }
        (raw["notesView"] ?: raw["view"]).parseEnumName<NotesView>()?.let {
            result["notesView"] = JsonPrimitive(it)
        }
        raw["listItemSorting"].parseEnumName<ListItemSort>()?.let {
            result["listItemSorting"] = JsonPrimitive(it)
        }
        raw["defaultListNoteViewMode"].parseEnumName<DefaultListNoteViewMode>()?.let {
            result["defaultListNoteViewMode"] = JsonPrimitive(it)
        }
        raw["editNoteActivityBottomAction"].parseEnumName<EditAction>()?.let {
            result["editNoteActivityBottomAction"] = JsonPrimitive(it)
        }

        val sortingElement = raw["notesSorting"]
        if (sortingElement is JsonObject) {
            result["notesSorting"] = sortingElement
        } else if (sortingElement is JsonPrimitive) {
            val sortedBy =
                NotesSortBy.fromValue(sortingElement.content)?.name
                    ?: NotesSortBy.CREATION_DATE.name
            val dir =
                raw["notesSortingDirection"].parseEnumName<SortDirection>()
                    ?: SortDirection.DESC.name
            result["notesSorting"] = buildJsonObject {
                put("sortedBy", sortedBy)
                put("sortDirection", dir)
            }
        }

        val periodicElement = raw["periodicBackups"]
        if (periodicElement is JsonObject) {
            result["periodicBackups"] = periodicElement
        } else if (raw.containsKey("autoBackupPeriodDays") || raw.containsKey("maxBackups")) {
            result["periodicBackups"] = buildJsonObject {
                val periodInDays = (raw["autoBackupPeriodDays"] as? JsonPrimitive)?.intOrNull ?: 1
                val maxBackups = (raw["maxBackups"] as? JsonPrimitive)?.intOrNull ?: 5
                put("periodInDays", periodInDays)
                put("maxBackups", maxBackups)
            }
        }

        (raw["maxItems"] ?: raw["maxItemsToDisplayInList.v1"])?.let { result["maxItems"] = it }
        (raw["maxLines"] ?: raw["maxLinesToDisplayInNote.v1"])?.let { result["maxLines"] = it }
        (raw["maxTitle"] ?: raw["maxLinesToDisplayInTitle"])?.let { result["maxTitle"] = it }
        (raw["labelsHidden"] ?: raw["labelsHiddenInNavigation"])?.let {
            result["labelsHidden"] = it
        }
        (raw["labelTagsHiddenInOverview"] ?: raw["labelsHiddenInOverview"])?.let {
            result["labelTagsHiddenInOverview"] = it
        }
        (raw["maxLabels"] ?: raw["maxLabelsInNavigation"])?.let { result["maxLabels"] = it }
        (raw["backupsFolder"] ?: raw["autoBackup"])?.let { result["backupsFolder"] = it }

        val topActions = raw["editNoteActivityTopActions"]
        if (topActions is JsonArray) {
            result["editNoteActivityTopActions"] = topActions
        } else if ((topActions is JsonPrimitive) && topActions.isString) {
            val actions = EditAction::class.java.deserializeEnums(topActions.content)
            result["editNoteActivityTopActions"] = JsonArray(actions.map { JsonPrimitive(it.name) })
        }

        val widgets = mutableMapOf<String, JsonElement>()
        val widgetNoteTypes = mutableMapOf<String, JsonElement>()
        (raw["widgets"] as? JsonObject)?.forEach { (k, v) -> widgets[k] = v }
        (raw["widgetNoteTypes"] as? JsonObject)?.forEach { (k, v) -> widgetNoteTypes[k] = v }

        for ((key, value) in raw) {
            if (key.startsWith("widget:")) {
                val id = key.substringAfter("widget:")
                if (id.toIntOrNull() != null && value is JsonPrimitive) {
                    widgets[id] = value
                }
            } else if (key.startsWith("widgetNoteType:")) {
                val id = key.substringAfter("widgetNoteType:")
                if (id.toIntOrNull() != null && value is JsonPrimitive) {
                    widgetNoteTypes[id] = value
                }
            } else if (key !in HANDLED_LEGACY_KEYS && key !in ignoredKeys) {
                result[key] = value
            }
        }

        if (widgets.isNotEmpty()) result["widgets"] = JsonObject(widgets)
        if (widgetNoteTypes.isNotEmpty()) result["widgetNoteTypes"] = JsonObject(widgetNoteTypes)

        return JsonObject(result)
    }

    private inline fun <reified E : Enum<E>> JsonElement?.parseEnumName(): String? {
        val str = (this as? JsonPrimitive)?.content ?: return null
        return try {
            java.lang.Enum.valueOf(E::class.java, str).name
        } catch (_: Exception) {
            try {
                java.lang.Enum.valueOf(E::class.java, str.fromCamelCaseToEnumName()).name
            } catch (_: Exception) {
                null
            }
        }
    }
}

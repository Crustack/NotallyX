package com.philkes.notallyx.data.model

import android.content.Context
import android.text.Html
import android.util.Base64
import androidx.core.text.toHtml
import com.philkes.notallyx.R
import com.philkes.notallyx.data.dao.NoteIdReminder
import com.philkes.notallyx.data.imports.markdown.createMarkdownFromBodyAndSpans
import com.philkes.notallyx.data.model.BaseNote.Companion.COLOR_DEFAULT
import com.philkes.notallyx.presentation.applySpans
import com.philkes.notallyx.utils.decodeToBitmap
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val NOTE_URL_PREFIX = "note://"
private val NOTE_URL_POSTFIX_NOTE = "/${Type.NOTE.name}"
private val NOTE_URL_POSTFIX_LIST = "/${Type.LIST.name}"

fun CharSequence?.isNoteUrl(): Boolean {
    return this?.let { startsWith(NOTE_URL_PREFIX) } ?: false
}

fun Long.createNoteUrl(type: Type): String {
    val postfix =
        when (type) {
            Type.LIST -> NOTE_URL_POSTFIX_LIST
            Type.NOTE -> NOTE_URL_POSTFIX_NOTE
        }
    return "$NOTE_URL_PREFIX$this$postfix"
}

fun String.getNoteIdFromUrl(): Long {
    return substringAfter(NOTE_URL_PREFIX).substringBefore("/").toLong()
}

fun String.getNoteTypeFromUrl(): Type {
    return Type.valueOf(substringAfterLast("/"))
}

val FileAttachment.isImage: Boolean
    get() {
        return mimeType.isImageMimeType
    }
val String.isImageMimeType: Boolean
    get() {
        return startsWith("image/")
    }
val String.isAudioMimeType: Boolean
    get() {
        return startsWith("audio/")
    }

fun BaseNote.toTxt(includeTitle: Boolean = true, includeCreationDate: Boolean = true) =
    buildString {
        val date = DateFormat.getDateInstance(DateFormat.FULL).format(timestamp)
        val body =
            when (type) {
                Type.NOTE -> body
                Type.LIST -> items.toText()
            }

        if (title.isNotEmpty() && includeTitle) {
            append("${title}\n\n")
        }
        if (includeCreationDate) {
            append("$date\n\n")
        }
        append(body)
        return toString()
    }

fun BaseNote.toJson(): String {
    val jsonObject =
        JSONObject()
            .put("type", type.name)
            .put("color", color)
            .put("title", title)
            .put("pinned", pinned)
            .put("timestamp", timestamp)
            .put("modifiedTimestamp", modifiedTimestamp)
            .put("labels", JSONArray(labels))

    when (type) {
        Type.NOTE -> {
            jsonObject.put("body", body)
            jsonObject.put("spans", Converters.spansToJSONArray(spans))
        }

        Type.LIST -> {
            jsonObject.put("items", Converters.itemsToJSONArray(items))
        }
    }
    jsonObject.put("reminders", Converters.remindersToJSONArray(reminders))
    jsonObject.put("viewMode", viewMode.name)
    return jsonObject.toString(2)
}

fun String.toBaseNote(): BaseNote {
    val jsonObject = JSONObject(this)
    val id = jsonObject.getLongOrDefault("id", -1L)
    val type = Type.valueOfOrDefault(jsonObject.getStringOrDefault("type", Type.NOTE.name))
    val folder = Folder.valueOfOrDefault(jsonObject.getStringOrDefault("folder", Folder.NOTES.name))
    val color =
        jsonObject.getStringOrDefault("color", COLOR_DEFAULT).takeIf { it.isValid() }
            ?: COLOR_DEFAULT
    val title = jsonObject.getStringOrDefault("title", "")
    val pinned = jsonObject.getBooleanOrDefault("pinned", false)
    val timestamp = jsonObject.getLongOrDefault("timestamp", System.currentTimeMillis())
    val modifiedTimestamp = jsonObject.getLongOrDefault("modifiedTimestamp", timestamp)
    val labels = Converters.jsonToLabels(jsonObject.getArrayOrEmpty("labels"))
    val body = jsonObject.getStringOrDefault("body", "")
    val spans = Converters.jsonToSpans(jsonObject.getArrayOrEmpty("spans"))
    val items = Converters.jsonToItems(jsonObject.getArrayOrEmpty("items"))
    val images = Converters.jsonToFiles(jsonObject.getArrayOrEmpty("images"))
    val files = Converters.jsonToFiles(jsonObject.getArrayOrEmpty("files"))
    val audios = Converters.jsonToAudios(jsonObject.getArrayOrEmpty("audios"))
    val reminders = Converters.jsonToReminders(jsonObject.getArrayOrEmpty("reminders"))
    val viewMode = NoteViewMode.valueOfOrDefault(jsonObject.getStringOrDefault("viewMode", ""))
    return BaseNote(
        id,
        type,
        folder,
        color,
        title,
        pinned,
        timestamp,
        modifiedTimestamp,
        labels,
        body,
        spans,
        items,
        images,
        files,
        audios,
        reminders,
        viewMode,
    )
}

private fun JSONObject.getStringOrDefault(key: String, defaultValue: String): String {
    return try {
        getString(key)
    } catch (exception: JSONException) {
        defaultValue
    }
}

private fun JSONObject.getArrayOrEmpty(key: String): JSONArray {
    return try {
        getJSONArray(key)
    } catch (exception: JSONException) {
        JSONArray("[]")
    }
}

private fun JSONObject.getBooleanOrDefault(key: String, defaultValue: Boolean): Boolean {
    return try {
        getBoolean(key)
    } catch (exception: JSONException) {
        defaultValue
    }
}

private fun JSONObject.getLongOrDefault(key: String, defaultValue: Long): Long {
    return try {
        getLong(key)
    } catch (exception: JSONException) {
        defaultValue
    }
}

fun BaseNote.toHtml(showDateCreated: Boolean, imagesRootFolder: File?) = buildString {
    val date = DateFormat.getDateInstance(DateFormat.FULL).format(timestamp)
    val title = Html.escapeHtml(title)

    append("<!DOCTYPE html>")
    append("<html><head>")
    append("<meta charset=\"UTF-8\"><title>$title</title>")
    append("</head><body>")
    append("<h2>$title</h2>")

    if (showDateCreated) {
        append("<p>$date</p>")
    }

    when (type) {
        Type.NOTE -> {
            val body = body.applySpans(spans).toHtml()
            append(body)
        }

        Type.LIST -> {
            append("<ol style=\"list-style: none; padding: 0;\">")
            items.forEach { item ->
                val body = Html.escapeHtml(item.body)
                val checked = if (item.checked) "checked" else ""
                val child = if (item.isChild) "style=\"margin-left: 20px\"" else ""
                append("<li><input type=\"checkbox\" $child $checked>$body</li>")
            }
            append("</ol>")
        }
    }
    if (images.isNotEmpty()) {
        append("<h3>Attached Images</h3>")
        images.forEach { image ->
            val file =
                if (imagesRootFolder != null) File(imagesRootFolder, image.localName) else null
            file?.readBytes()?.let { bytes ->
                val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val options = file.decodeToBitmap()
                val width = options?.width
                val height = options?.height
                append(
                    "<img src=\"data:image/jpeg;base64,$base64String\" alt=\"${image.originalName}\"${options?.let { " width=\"$width\" height=\"$height\"" } ?: ""} />"
                )
            }
        }
    }
    append("</body></html>")
}

fun List<BaseNote>.toNoteIdReminders() = map { NoteIdReminder(it.id, it.reminders) }

fun BaseNote.toMarkdown(): String = buildString {
    when (type) {
        Type.NOTE -> {
            append(createMarkdownFromBodyAndSpans(body, spans))
        }
        Type.LIST -> {
            append(items.toMarkdownChecklist())
        }
    }
}

private fun List<ListItem>.toMarkdownChecklist(): String = buildString {
    for (item in this@toMarkdownChecklist) {
        val check = if (item.checked) "[x]" else "[ ]"
        val indent = if (item.isChild) "    " else ""
        appendLine("$indent- $check ${item.body}")
    }
}

private fun String.toMarkdownWithSpans(spans: List<SpanRepresentation>): String {
    if (spans.isEmpty()) return this
    val before = Array(this.length + 1) { StringBuilder() }
    val after = Array(this.length + 1) { StringBuilder() }

    // Prioritize links: avoid adding other markers within link ranges to reduce nesting issues
    val linkRanges = spans.filter { it.link }.map { it.start to it.end }
    fun inLinkRange(index: Int): Boolean =
        linkRanges.any { index >= it.first && index <= it.second }

    // Add markers for non-link spans first
    spans
        .filter { !it.link }
        .forEach { s ->
            val start = s.start.coerceIn(0, this.length)
            val end = s.end.coerceIn(0, this.length)
            if (start >= end) return@forEach
            if (!inLinkRange(start) && !inLinkRange(end)) {
                if (s.bold) {
                    before[start].append("**")
                    after[end].insert(0, "**")
                }
                if (s.italic) {
                    before[start].append("_")
                    after[end].insert(0, "_")
                }
                if (s.monospace) {
                    before[start].append("`")
                    after[end].insert(0, "`")
                }
                if (s.strikethrough) {
                    before[start].append("~~")
                    after[end].insert(0, "~~")
                }
            }
        }

    // Add link markers
    spans
        .filter { it.link }
        .forEach { s ->
            val start = s.start.coerceIn(0, this.length)
            val end = s.end.coerceIn(0, this.length)
            if (start >= end) return@forEach
            before[start].append("[")
            val url = s.linkData ?: this.safeSubstring(start, end)
            after[end].append("](${url})")
        }

    // Build output
    val out = StringBuilder()
    for (i in 0..this.length) {
        out.append(before[i])
        if (i < this.length) out.append(this[i])
        out.append(after[i])
    }
    return out.toString()
}

private fun String.safeSubstring(start: Int, end: Int): String {
    val s = start.coerceIn(0, this.length)
    val e = end.coerceIn(0, this.length)
    return if (s < e) this.substring(s, e) else ""
}

fun BaseNote.attachmentsDifferFrom(other: BaseNote): Boolean {
    return files.size != other.files.size ||
        files.any { file -> other.files.none { it.localName == file.localName } } ||
        other.files.any { file -> files.none { it.localName == file.localName } } ||
        images.any { image -> other.images.none { it.localName == image.localName } } ||
        other.images.any { image -> images.none { it.localName == image.localName } } ||
        audios.any { audio -> other.audios.none { it.name == audio.name } } ||
        other.audios.any { audio -> audios.none { it.name == audio.name } }
}

fun Reminder.toRepetitionText(context: Context): String {
    val rep = repetition ?: return context.getString(R.string.reminder_no_repetition)
    if (rep.unit == RepetitionTimeUnit.MONTHS && rep.occurrence == null && rep.value == 1) {
        val calendar = dateTime.toCalendar()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val isLastDayOfMonth = dayOfMonth == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return if (isLastDayOfMonth)
            context.getString(R.string.of_the_month_last, context.getString(R.string.day))
        else context.getString(R.string.of_the_month, "$dayOfMonth.")
    }
    return rep.toText(context)
}

fun Repetition.toText(context: Context): String {
    if (unit == RepetitionTimeUnit.MONTHS && occurrence != null && dayOfWeek != null) {
        val dayOfWeekStr =
            SimpleDateFormat("EEEE", Locale.getDefault())
                .format(
                    Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, dayOfWeek!!) }.time
                )
        val monthlyOptionText =
            when (occurrence) {
                -1 -> context.getString(R.string.of_the_month_last, dayOfWeekStr)
                else -> "$occurrence. ${context.getString(R.string.of_the_month, dayOfWeekStr)}"
            }
        val prefix =
            if (value == 1) ""
            else "${context.getString(R.string.every)} $value ${context.getString(R.string.months)}"
        return "$prefix $monthlyOptionText"
    }
    return when {
        value == 1 && unit == RepetitionTimeUnit.DAYS -> context.getString(R.string.daily)
        value == 1 && unit == RepetitionTimeUnit.WEEKS -> context.getString(R.string.weekly)
        value == 1 && unit == RepetitionTimeUnit.MONTHS -> context.getString(R.string.monthly)
        value == 1 && unit == RepetitionTimeUnit.YEARS -> context.getString(R.string.yearly)
        else -> "${context.getString(R.string.every)} $value ${unit.toText(context)}"
    }
}

private fun RepetitionTimeUnit.toText(context: Context): String {
    val resId =
        when (this) {
            RepetitionTimeUnit.MINUTES -> R.string.minutes
            RepetitionTimeUnit.HOURS -> R.string.hours
            RepetitionTimeUnit.DAYS -> R.string.days
            RepetitionTimeUnit.WEEKS -> R.string.weeks
            RepetitionTimeUnit.MONTHS -> R.string.months
            RepetitionTimeUnit.YEARS -> R.string.years
        }
    return context.getString(resId)
}

fun Collection<Reminder>.copy() = map { it.copy() }

fun RepetitionTimeUnit.toCalendarField(): Int {
    return when (this) {
        RepetitionTimeUnit.MINUTES -> Calendar.MINUTE
        RepetitionTimeUnit.HOURS -> Calendar.HOUR
        RepetitionTimeUnit.DAYS -> Calendar.DAY_OF_MONTH
        RepetitionTimeUnit.WEEKS -> Calendar.WEEK_OF_YEAR
        RepetitionTimeUnit.MONTHS -> Calendar.MONTH
        RepetitionTimeUnit.YEARS -> Calendar.YEAR
    }
}

fun Reminder.lastNotification(before: Date = Date()): Date? {
    if (before.before(dateTime) || before == dateTime) {
        return null
    }
    if (repetition == null) {
        return dateTime
    }

    val calendar = dateTime.toCalendar()
    val field = repetition!!.unit.toCalendarField()
    val value = repetition!!.value

    var last = calendar.time
    // Increment until we are at or after 'before'
    while (true) {
        calendar.add(field, value)
        if (calendar.time.after(before) || calendar.time == before) {
            break
        }
        last = calendar.time
    }

    return last
}

fun Reminder.nextNotification(from: Date = Date()): Date? {
    if (from.before(dateTime)) {
        return dateTime
    }
    if (repetition == null) {
        return null
    }

    val rep = repetition!!
    if (rep.unit == RepetitionTimeUnit.MONTHS && rep.occurrence != null && rep.dayOfWeek != null) {
        val next = dateTime.toCalendar()
        val fromCal = from.toCalendar()
        while (true) {
            val targetDate =
                findOccurrenceInMonth(
                    next.get(Calendar.YEAR),
                    next.get(Calendar.MONTH),
                    rep.occurrence!!,
                    rep.dayOfWeek!!,
                    dateTime.toCalendar(),
                )
            if (targetDate.after(fromCal)) {
                return targetDate.time
            }
            next.add(Calendar.MONTH, rep.value)
        }
    }
    val calendar = dateTime.toCalendar()
    val field = repetition!!.unit.toCalendarField()
    val value = repetition!!.value

    // If from is exactly at dateTime, we want the next one
    while (true) {
        calendar.add(field, value)
        if (calendar.time.after(from)) {
            break
        }
    }

    return calendar.time
}

private fun findOccurrenceInMonth(
    year: Int,
    month: Int,
    occurrence: Int,
    dayOfWeek: Int,
    originalTime: Calendar,
): Calendar {
    val cal =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, originalTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, originalTime.get(Calendar.MINUTE))
            set(Calendar.SECOND, originalTime.get(Calendar.SECOND))
            set(Calendar.MILLISECOND, originalTime.get(Calendar.MILLISECOND))
        }

    if (occurrence > 0) {
        // Find first dayOfWeek
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        // Advance to the Nth occurrence
        cal.add(Calendar.DAY_OF_MONTH, 7 * (occurrence - 1))

        // If we advanced into the next month, it means there are fewer than 'occurrence' of that
        // day in this month.
        // The requirement says "if not, it'll behave like the previous option, so 4th day" for the
        // last day,
        // but for 1st-4th it should probably stay in the month if it exists.
        // Actually, for 1st-4th, they ALWAYS exist in every month. Only 5th might not exist.
        if (cal.get(Calendar.MONTH) != month) {
            // Fallback to 4th if 5th doesn't exist (though only 1-4 and -1 are currently planned)
            return findOccurrenceInMonth(year, month, occurrence - 1, dayOfWeek, originalTime)
        }
    } else if (occurrence == -1) {
        // Last occurrence
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
    }
    return cal
}

fun Reminder.nextRepetition(from: Date = Date()): Date? {
    if (repetition == null) {
        return null
    }
    return nextNotification(from)
}

fun Reminder.hasUpcomingNotification() = !(dateTime.before(Date()) && repetition == null)

fun Repetition.toMillis(): Long {
    return Calendar.getInstance()
        .apply {
            timeInMillis = 0
            add(unit.toCalendarField(), value)
        }
        .timeInMillis
}

fun Collection<Reminder>.hasAnyUpcomingNotifications(): Boolean {
    return any { it.hasUpcomingNotification() }
}

fun Collection<Reminder>.findNextNotificationDate(): Date? {
    return mapNotNull { it.nextNotification() }.minOrNull()
}

fun Collection<Reminder>.findLastNotificationDate(before: Date = Date()): Date? {
    return mapNotNull { it.lastNotification(before) }.maxOrNull()
}

fun Collection<Reminder>.findLastNotified(before: Date = Date()): Reminder? {
    return filter { it.lastNotification(before) != null }
        .maxByOrNull { it.lastNotification(before)!! }
}

fun Date.toCalendar() = Calendar.getInstance().apply { timeInMillis = this@toCalendar.time }

fun List<ListItem>.toText() = buildString {
    for (item in this@toText) {
        val check = if (item.checked) "[✓]" else "[ ]"
        val childIndentation = if (item.isChild) "    " else ""
        appendLine("$childIndentation$check ${item.body}")
    }
}

fun Collection<ListItem>.deepCopy() = map { it.copy(children = mutableListOf()) }

fun ColorString.isValid() =
    when (this) {
        COLOR_DEFAULT -> true
        else ->
            try {
                android.graphics.Color.parseColor(this)
                true
            } catch (e: Exception) {
                false
            }
    }

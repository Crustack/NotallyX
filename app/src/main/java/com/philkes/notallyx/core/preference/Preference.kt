package com.philkes.notallyx.core.preference

import android.content.Context
import android.os.Build
import com.philkes.notallyx.R
import com.philkes.notallyx.core.ui.extensions.format
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.ocpsoft.prettytime.PrettyTime

/** Common interface for observable preferences backed by StateFlow and async updates. */
interface Preference<T> {
    val key: String
    val value: T
    val flow: StateFlow<T>
    val defaultValue: T
    val titleResId: Int?

    fun save(value: T)

    suspend fun update(value: T)
}

typealias BasePreference<T> = Preference<T>

fun <T, R> StateFlow<T>.mapState(
    scope: CoroutineScope,
    transform: (T) -> R,
): StateFlow<R> {
    return map { transform(it) }
        .distinctUntilChanged()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = transform(value),
        )
}

open class PreferenceHolder<T>(
    override val key: String,
    override val defaultValue: T,
    override val titleResId: Int? = null,
    override val flow: StateFlow<T>,
    private val onUpdate: suspend (T) -> Unit,
    private val scope: CoroutineScope,
) : Preference<T> {

    @Volatile private var localOverride: T? = null

    override val value: T
        get() = localOverride ?: flow.value

    override fun save(value: T) {
        localOverride = value
        scope.launch {
            try {
                onUpdate(value)
            } finally {
                localOverride = null
            }
        }
    }

    override suspend fun update(value: T) {
        localOverride = value
        try {
            onUpdate(value)
        } finally {
            localOverride = null
        }
    }

    fun refresh() {}

    fun getFreshValue(): T = value
}

class IntPreference(
    key: String,
    defaultValue: Int,
    val min: Int,
    val max: Int,
    titleResId: Int? = null,
    flow: StateFlow<Int>,
    onUpdate: suspend (Int) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<Int>(key, defaultValue, titleResId, flow, onUpdate, scope)

class FloatPreference(
    key: String,
    defaultValue: Float,
    val min: Float,
    val max: Float,
    titleResId: Int? = null,
    flow: StateFlow<Float>,
    onUpdate: suspend (Float) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<Float>(key, defaultValue, titleResId, flow, onUpdate, scope)

class LongPreference(
    key: String,
    defaultValue: Long,
    titleResId: Int? = null,
    flow: StateFlow<Long>,
    onUpdate: suspend (Long) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<Long>(key, defaultValue, titleResId, flow, onUpdate, scope)

open class StringPreference(
    key: String,
    defaultValue: String,
    titleResId: Int? = null,
    flow: StateFlow<String>,
    onUpdate: suspend (String) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<String>(key, defaultValue, titleResId, flow, onUpdate, scope)

class BooleanPreference(
    key: String,
    defaultValue: Boolean,
    titleResId: Int? = null,
    flow: StateFlow<Boolean>,
    onUpdate: suspend (Boolean) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<Boolean>(key, defaultValue, titleResId, flow, onUpdate, scope)

class StringSetPreference(
    key: String,
    defaultValue: Set<String>,
    titleResId: Int? = null,
    flow: StateFlow<Set<String>>,
    onUpdate: suspend (Set<String>) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<Set<String>>(key, defaultValue, titleResId, flow, onUpdate, scope)

class EnumPreference<T>(
    key: String,
    defaultValue: T,
    val enumClass: Class<T>,
    titleResId: Int? = null,
    flow: StateFlow<T>,
    onUpdate: suspend (T) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<T>(key, defaultValue, titleResId, flow, onUpdate, scope)
    where T : Enum<T>, T : TextProvider

class EnumListPreference<T>(
    key: String,
    defaultValue: List<T>,
    val enumClass: Class<T>,
    titleResId: Int? = null,
    flow: StateFlow<List<T>>,
    onUpdate: suspend (List<T>) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<List<T>>(key, defaultValue, titleResId, flow, onUpdate, scope)
    where T : Enum<T>

class ByteArrayPreference(
    key: String,
    defaultValue: ByteArray?,
    titleResId: Int? = null,
    flow: StateFlow<ByteArray?>,
    onUpdate: suspend (ByteArray?) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<ByteArray?>(key, defaultValue, titleResId, flow, onUpdate, scope)

class EncryptedPassphrasePreference(
    key: String,
    defaultValue: ByteArray,
    titleResId: Int? = null,
    flow: StateFlow<ByteArray>,
    onUpdate: suspend (ByteArray) -> Unit,
    scope: CoroutineScope,
) : PreferenceHolder<ByteArray>(key, defaultValue, titleResId, flow, onUpdate, scope) {

    fun init(cipher: Cipher): ByteArray {
        val random =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                SecureRandom.getInstanceStrong()
            } else {
                SecureRandom()
            }
        val result = ByteArray(64)

        random.nextBytes(result)

        // filter out zero byte values, as SQLCipher does not like them
        while (result.contains(0)) {
            random.nextBytes(result)
        }

        val encryptedPassphrase = cipher.doFinal(result)
        save(encryptedPassphrase)
        return result
    }
}

fun <T> Flow<T>.withPrevious(): Flow<Pair<T?, T>> {
    return scan<T, Pair<T?, T>?>(null) { prev, current -> Pair(prev?.second, current) }
        .filterNotNull()
}

interface TextProvider {
    fun getText(context: Context): String
}

interface StaticTextProvider : TextProvider {
    val textResId: Int

    override fun getText(context: Context): String {
        return context.getString(textResId)
    }
}

@Serializable
enum class NotesView(override val textResId: Int) : StaticTextProvider {
    LIST(R.string.list),
    GRID(R.string.grid),
}

@Serializable
enum class Theme(override val textResId: Int) : StaticTextProvider {
    LIGHT(R.string.light),
    DARK(R.string.dark),
    SUPER_DARK(R.string.super_dark),
    FOLLOW_SYSTEM(R.string.follow_system),
}

class ThreadLocalDateFormat(private val format: String, private val locale: Locale) {
    private val threadLocal =
        object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue(): SimpleDateFormat {
                return SimpleDateFormat(format, locale)
            }
        }

    fun format(date: Date): String {
        return threadLocal.get()!!.format(date)
    }

    fun toPattern(): String {
        return threadLocal.get()!!.toPattern()
    }
}

class ThreadLocalDateInstance(private val style: Int) {
    private val threadLocal =
        object : ThreadLocal<java.text.DateFormat>() {
            override fun initialValue(): java.text.DateFormat {
                return java.text.DateFormat.getDateInstance(style)
            }
        }

    fun format(date: Date): String {
        return threadLocal.get()!!.format(date)
    }
}

private fun oneDayAgo(): Date =
    java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }.time

private val ISO_DATE_FORMAT = ThreadLocalDateFormat("yyyy-MM-dd", Locale.US)
private val MM_DD_YY_FORMAT = ThreadLocalDateFormat("MM/dd/yy", Locale.US)
private val DD_MM_YY_FORMAT = ThreadLocalDateFormat("dd/MM/yy", Locale.UK)
private val DD_MM_YY_FORMAT_GER = ThreadLocalDateFormat("dd.MM.yy", Locale.GERMANY)
private val FULL_FORMAT = ThreadLocalDateInstance(java.text.DateFormat.FULL)

@Serializable
enum class DateFormat(val format: (Date) -> String, private val textHint: String = "") :
    TextProvider {
    NONE({ "" }),
    FULL(FULL_FORMAT::format),
    RELATIVE({ PrettyTime().format(it) }),
    DD_MM_YY_GER(DD_MM_YY_FORMAT_GER::format, " (${DD_MM_YY_FORMAT_GER.toPattern()})"),
    DD_MM_YY(DD_MM_YY_FORMAT::format, " (${DD_MM_YY_FORMAT.toPattern()})"),
    MM_DD_YY(MM_DD_YY_FORMAT::format, " (${MM_DD_YY_FORMAT.toPattern()})"),
    SHORT_ISO(ISO_DATE_FORMAT::format, " (${ISO_DATE_FORMAT.toPattern()})");

    override fun getText(context: Context): String {
        return when (this) {
            NONE -> context.getString(R.string.none)
            else -> oneDayAgo().format(this, TimeFormat.NONE) + textHint
        }
    }
}

private val TWENTY_FOUR_H_FORMAT = ThreadLocalDateFormat("HH:mm", Locale.GERMANY)
private val AM_PM_FORMAT = ThreadLocalDateFormat("hh:mm a", Locale.US)

@Serializable
enum class TimeFormat(val format: (Date) -> String) : TextProvider {
    NONE({ "" }),
    TWENTY_FOUR_H(TWENTY_FOUR_H_FORMAT::format),
    AM_PM(AM_PM_FORMAT::format);

    override fun getText(context: Context): String {
        return when (this) {
            NONE -> context.getString(R.string.none)
            else -> oneDayAgo().format(dateFormat = DateFormat.NONE, timeFormat = this)
        }
    }
}

typealias TextSizeSp = Float

val TextSizeSp.displayBodySize: Float
    get() = (this - 2)

val TextSizeSp.displaySmallerSize: Float
    get() = (this - 3)

val TextSizeSp.displayTitleSize: Float
    get() = this

@Serializable
enum class ListItemSort(override val textResId: Int) : StaticTextProvider {
    NO_AUTO_SORT(R.string.no_auto_sort),
    AUTO_SORT_BY_CHECKED(R.string.auto_sort_by_checked),
    AUTO_SORT_BY_CHECKED_TIMESTAMP(R.string.auto_sort_by_checked_timestamp),
}

@Serializable
enum class DefaultListNoteViewMode(override val textResId: Int) : StaticTextProvider {
    READ_ONLY(R.string.read_only),
    EDIT(R.string.edit),
    LAST_USED(R.string.last_used);

    fun toNoteViewMode(lastUsed: NoteViewMode): NoteViewMode {
        return when (this) {
            READ_ONLY -> NoteViewMode.READ_ONLY
            EDIT -> NoteViewMode.EDIT
            LAST_USED -> lastUsed
        }
    }
}

@Serializable
enum class BiometricLock(override val textResId: Int) : StaticTextProvider {
    ENABLED(R.string.enabled),
    DISABLED(R.string.disabled);

    override fun getText(context: Context): String = context.getString(textResId)
}

@Serializable
enum class EditAction(override val textResId: Int, val drawableResId: Int) : StaticTextProvider {
    SEARCH(R.string.search, R.drawable.search),
    PIN(R.string.pin, R.drawable.pin),
    REMINDERS(R.string.reminders, R.drawable.notifications),
    LABELS(R.string.labels, R.drawable.label),
    CHANGE_COLOR(R.string.change_color, R.drawable.change_color),
    DUPLICATE(R.string.duplicate, R.drawable.content_copy),
    EXPORT(R.string.export, R.drawable.export),
    SHARE(R.string.share, R.drawable.share),
    DELETE(R.string.delete, R.drawable.delete),
    ARCHIVE(R.string.archive, R.drawable.archive),
    TOGGLE_VIEW_MODE(R.string.edit, R.drawable.visibility),
    CONVERT(R.string.convert_to_list_note, R.drawable.convert_to_text),
    DELETE_FOREVER(R.string.delete_forever, R.drawable.delete),
    RESTORE(R.string.restore, R.drawable.restore),
    PIN_TO_STATUS(R.string.pin_to_status_bar, R.drawable.pinboard);

    fun getTitleAndIcon(
        pinned: Boolean,
        viewMode: NoteViewMode?,
        folder: Folder? = null,
        type: Type? = null,
        isPinnedToStatus: Boolean = false,
    ): Pair<Int, Int> {
        val icon =
            when (this) {
                PIN -> if (pinned) R.drawable.unpin else R.drawable.pin
                PIN_TO_STATUS ->
                    if (isPinnedToStatus) R.drawable.pinboard_filled else R.drawable.pinboard
                ARCHIVE ->
                    if (folder == Folder.ARCHIVED) R.drawable.unarchive else R.drawable.archive
                RESTORE ->
                    if (folder == Folder.ARCHIVED) R.drawable.unarchive else R.drawable.restore
                TOGGLE_VIEW_MODE ->
                    if (viewMode == NoteViewMode.READ_ONLY) R.drawable.edit
                    else R.drawable.visibility
                else -> drawableResId
            }
        val title =
            when (this) {
                PIN -> if (pinned) R.string.unpin else R.string.pin
                PIN_TO_STATUS ->
                    if (isPinnedToStatus) R.string.unpin_from_status_bar
                    else R.string.pin_to_status_bar
                ARCHIVE -> if (folder == Folder.ARCHIVED) R.string.unarchive else R.string.archive
                RESTORE -> if (folder == Folder.ARCHIVED) R.string.unarchive else R.string.restore
                TOGGLE_VIEW_MODE ->
                    if (viewMode == NoteViewMode.READ_ONLY) R.string.edit else R.string.read_only
                CONVERT ->
                    if (type == Type.LIST) R.string.convert_to_text_note
                    else R.string.convert_to_list_note
                else -> textResId
            }
        return title to icon
    }
}

enum class EditListAction(override val textResId: Int, val drawableResId: Int) :
    StaticTextProvider {
    DELETE_CHECKED(R.string.delete_checked_items, R.drawable.delete_all),
    CHECK_ALL(R.string.check_all_items, R.drawable.checkbox_checked),
    UNCHECK_ALL(R.string.uncheck_all_items, R.drawable.checkbox_unchecked),
}

object Constants {
    const val PASSWORD_EMPTY = "None"
}

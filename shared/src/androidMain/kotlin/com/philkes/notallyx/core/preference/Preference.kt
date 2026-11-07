package com.philkes.notallyx.core.preference

import android.content.Context
import android.os.Build
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.data.model.extensions.formatDate
import java.security.SecureRandom
import java.util.Date
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
) : PreferenceHolder<T>(key, defaultValue, titleResId, flow, onUpdate, scope) where T : Enum<T>

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

val NotesView.textResId: Int
    get() =
        when (this) {
            NotesView.LIST -> R.string.list
            NotesView.GRID -> R.string.grid
        }

fun NotesView.getText(context: Context): String = context.getString(textResId)

val Theme.textResId: Int
    get() =
        when (this) {
            Theme.LIGHT -> R.string.light
            Theme.DARK -> R.string.dark
            Theme.SUPER_DARK -> R.string.super_dark
            Theme.FOLLOW_SYSTEM -> R.string.follow_system
        }

fun Theme.getText(context: Context): String = context.getString(textResId)

private fun oneDayAgo(): Date =
    java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }.time

fun DateFormat.getText(context: Context): String {
    return when (this) {
        DateFormat.NONE -> context.getString(R.string.none)
        else -> oneDayAgo().formatDate(this, TimeFormat.NONE) + textHint
    }
}

fun TimeFormat.getText(context: Context): String {
    return when (this) {
        TimeFormat.NONE -> context.getString(R.string.none)
        else -> oneDayAgo().formatDate(dateFormat = DateFormat.NONE, timeFormat = this)
    }
}

typealias TextSizeSp = Float

val TextSizeSp.displayBodySize: Float
    get() = (this - 2)

val TextSizeSp.displaySmallerSize: Float
    get() = (this - 3)

val TextSizeSp.displayTitleSize: Float
    get() = this

val ListItemSort.textResId: Int
    get() =
        when (this) {
            ListItemSort.NO_AUTO_SORT -> R.string.no_auto_sort
            ListItemSort.AUTO_SORT_BY_CHECKED -> R.string.auto_sort_by_checked
            ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP -> R.string.auto_sort_by_checked_timestamp
        }

fun ListItemSort.getText(context: Context): String = context.getString(textResId)

val DefaultListNoteViewMode.textResId: Int
    get() =
        when (this) {
            DefaultListNoteViewMode.READ_ONLY -> R.string.read_only
            DefaultListNoteViewMode.EDIT -> R.string.edit
            DefaultListNoteViewMode.LAST_USED -> R.string.last_used
        }

fun DefaultListNoteViewMode.getText(context: Context): String = context.getString(textResId)

val BiometricLock.textResId: Int
    get() =
        when (this) {
            BiometricLock.ENABLED -> R.string.enabled
            BiometricLock.DISABLED -> R.string.disabled
        }

fun BiometricLock.getText(context: Context): String = context.getString(textResId)

@Serializable
val EditAction.textResId: Int
    get() =
        when (this) {
            EditAction.SEARCH -> R.string.search
            EditAction.PIN -> R.string.pin
            EditAction.REMINDERS -> R.string.reminders
            EditAction.LABELS -> R.string.labels
            EditAction.CHANGE_COLOR -> R.string.change_color
            EditAction.DUPLICATE -> R.string.duplicate
            EditAction.EXPORT -> R.string.export
            EditAction.SHARE -> R.string.share
            EditAction.DELETE -> R.string.delete
            EditAction.ARCHIVE -> R.string.archive
            EditAction.TOGGLE_VIEW_MODE -> R.string.edit
            EditAction.CONVERT -> R.string.convert_to_list_note
            EditAction.DELETE_FOREVER -> R.string.delete_forever
            EditAction.RESTORE -> R.string.restore
            EditAction.PIN_TO_STATUS -> R.string.pin_to_status_bar
        }

val EditAction.drawableResId: Int
    get() =
        when (this) {
            EditAction.SEARCH -> R.drawable.search
            EditAction.PIN -> R.drawable.pin
            EditAction.REMINDERS -> R.drawable.notifications
            EditAction.LABELS -> R.drawable.label
            EditAction.CHANGE_COLOR -> R.drawable.change_color
            EditAction.DUPLICATE -> R.drawable.content_copy
            EditAction.EXPORT -> R.drawable.export
            EditAction.SHARE -> R.drawable.share
            EditAction.DELETE -> R.drawable.delete
            EditAction.ARCHIVE -> R.drawable.archive
            EditAction.TOGGLE_VIEW_MODE -> R.drawable.visibility
            EditAction.CONVERT -> R.drawable.convert_to_text
            EditAction.DELETE_FOREVER -> R.drawable.delete
            EditAction.RESTORE -> R.drawable.restore
            EditAction.PIN_TO_STATUS -> R.drawable.pinboard
        }

fun EditAction.getTitleAndIcon(
    pinned: Boolean,
    viewMode: NoteViewMode?,
    folder: Folder? = null,
    type: Type? = null,
    isPinnedToStatus: Boolean = false,
): Pair<Int, Int> {
    val icon =
        when (this) {
            EditAction.PIN -> if (pinned) R.drawable.unpin else R.drawable.pin
            EditAction.PIN_TO_STATUS ->
                if (isPinnedToStatus) R.drawable.pinboard_filled else R.drawable.pinboard
            EditAction.ARCHIVE ->
                if (folder == Folder.ARCHIVED) R.drawable.unarchive else R.drawable.archive
            EditAction.RESTORE ->
                if (folder == Folder.ARCHIVED) R.drawable.unarchive else R.drawable.restore
            EditAction.TOGGLE_VIEW_MODE ->
                if (viewMode == NoteViewMode.READ_ONLY) R.drawable.edit else R.drawable.visibility
            else -> drawableResId
        }
    val title =
        when (this) {
            EditAction.PIN -> if (pinned) R.string.unpin else R.string.pin
            EditAction.PIN_TO_STATUS ->
                if (isPinnedToStatus) R.string.unpin_from_status_bar else R.string.pin_to_status_bar
            EditAction.ARCHIVE ->
                if (folder == Folder.ARCHIVED) R.string.unarchive else R.string.archive
            EditAction.RESTORE ->
                if (folder == Folder.ARCHIVED) R.string.unarchive else R.string.restore
            EditAction.TOGGLE_VIEW_MODE ->
                if (viewMode == NoteViewMode.READ_ONLY) R.string.edit else R.string.read_only
            EditAction.CONVERT ->
                if (type == Type.LIST) R.string.convert_to_text_note
                else R.string.convert_to_list_note
            else -> textResId
        }
    return title to icon
}

val EditListAction.textResId: Int
    get() =
        when (this) {
            EditListAction.DELETE_CHECKED -> R.string.delete_checked_items
            EditListAction.CHECK_ALL -> R.string.check_all_items
            EditListAction.UNCHECK_ALL -> R.string.uncheck_all_items
        }

val EditListAction.drawableResId: Int
    get() =
        when (this) {
            EditListAction.DELETE_CHECKED -> R.drawable.delete_all
            EditListAction.CHECK_ALL -> R.drawable.checkbox_checked
            EditListAction.UNCHECK_ALL -> R.drawable.checkbox_unchecked
        }

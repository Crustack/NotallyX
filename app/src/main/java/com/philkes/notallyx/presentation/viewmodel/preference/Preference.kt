package com.philkes.notallyx.presentation.viewmodel.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import com.philkes.notallyx.R
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.NoteViewMode
import com.philkes.notallyx.data.model.Type
import com.philkes.notallyx.presentation.format
import com.philkes.notallyx.presentation.merge
import com.philkes.notallyx.presentation.view.misc.NotNullLiveData
import com.philkes.notallyx.presentation.view.note.listitem.adapter.CheckedListItemAdapter
import com.philkes.notallyx.presentation.view.note.listitem.sorting.ListItemCheckedTimestampSortCallback
import com.philkes.notallyx.presentation.view.note.listitem.sorting.ListItemParentSortCallback
import com.philkes.notallyx.utils.createObserverSkipFirst
import com.philkes.notallyx.utils.deserializeEnums
import com.philkes.notallyx.utils.fromCamelCaseToEnumName
import com.philkes.notallyx.utils.serializeEnums
import com.philkes.notallyx.utils.toCamelCase
import com.philkes.notallyx.utils.toPreservedByteArray
import com.philkes.notallyx.utils.toPreservedString
import java.security.SecureRandom
import java.util.Date
import javax.crypto.Cipher

/**
 * Every Preference can be observed like a [NotNullLiveData].
 *
 * @param titleResId Optional string resource id, if preference can be set via the UI.
 */
abstract class BasePreference<T>(
    private val sharedPreferences: SharedPreferences,
    protected val defaultValue: T,
    val titleResId: Int? = null,
) {
    private var data: NotNullLiveData<T>? = null
    private var cachedValue: T? = null

    val value: T
        get() {
            if (cachedValue == null) {
                cachedValue = getValue(sharedPreferences)
            }
            return cachedValue!!
        }

    protected abstract fun getValue(sharedPreferences: SharedPreferences): T

    fun getData(): NotNullLiveData<T> {
        if (data == null) {
            data = NotNullLiveData(value)
        }
        return data as NotNullLiveData<T>
    }

    internal fun save(value: T) {
        sharedPreferences.edit(true) { put(value) }
        cachedValue = value
        getData().postValue(value)
    }

    protected abstract fun SharedPreferences.Editor.put(value: T)

    fun observe(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
        getData().observe(lifecycleOwner, observer)
    }

    fun <C> merge(other: BasePreference<C>): MediatorLiveData<Pair<T, C>> {
        return getData().merge(other.getData())
    }

    fun <C> merge(other: LiveData<C>): MediatorLiveData<Pair<T, C?>> {
        return getData().merge(other)
    }

    fun <C> merge(other: NotNullLiveData<C>): MediatorLiveData<Pair<T, C>> {
        return getData().merge(other)
    }

    fun observeForever(observer: Observer<T>) {
        getData().observeForever(observer)
    }

    fun removeObserver(observer: Observer<T>) {
        getData().removeObserver(observer)
    }

    fun removeObservers(lifecycleOwner: LifecycleOwner) {
        getData().removeObservers(lifecycleOwner)
    }

    fun observeForeverWithPrevious(observer: Observer<Pair<T?, T>>) {
        val mediator = MediatorLiveData<Pair<T?, T>>()
        var previousValue: T? = null

        mediator.addSource(getData()) { currentValue ->
            mediator.value = Pair(previousValue, currentValue!!)
            previousValue = currentValue
        }

        mediator.observeForever(observer)
    }

    fun refresh() {
        cachedValue = null
        getData().postValue(value)
    }

    fun getFreshValue() = getValue(sharedPreferences)
}

fun <T> BasePreference<T>.observeForeverSkipFirst(observer: Observer<T>) {
    this.observeForever(createObserverSkipFirst(observer))
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

class EnumPreference<T>(
    sharedPreferences: SharedPreferences,
    val key: String,
    defaultValue: T,
    private val enumClass: Class<T>,
    titleResId: Int? = null,
) : BasePreference<T>(sharedPreferences, defaultValue, titleResId) where
T : Enum<T>,
T : TextProvider {

    override fun getValue(sharedPreferences: SharedPreferences): T {
        val storedValue = sharedPreferences.getString(key, null)
        return try {
            storedValue?.let { java.lang.Enum.valueOf(enumClass, it.fromCamelCaseToEnumName()) }
                ?: defaultValue
        } catch (e: IllegalArgumentException) {
            defaultValue
        }
    }

    override fun SharedPreferences.Editor.put(value: T) {
        putString(key, value.name.toCamelCase())
    }
}

class StringSetPreference(
    private val key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: Set<String>,
    titleResId: Int? = null,
) : BasePreference<Set<String>>(sharedPreferences, defaultValue, titleResId) {

    override fun getValue(sharedPreferences: SharedPreferences): Set<String> {
        return sharedPreferences.getStringSet(key, defaultValue)!!
    }

    override fun SharedPreferences.Editor.put(value: Set<String>) {
        putStringSet(key, value)
    }
}

class EnumListPreference<T>(
    private val key: String,
    sharedPreferences: SharedPreferences,
    private val enumClass: Class<T>,
    defaultValue: List<T>,
    titleResId: Int? = null,
) : BasePreference<List<T>>(sharedPreferences, defaultValue, titleResId) where T : Enum<T> {

    override fun getValue(sharedPreferences: SharedPreferences): List<T> {
        return sharedPreferences.getString(key, null)?.let { enumClass.deserializeEnums(it) }
            ?: defaultValue
    }

    override fun SharedPreferences.Editor.put(value: List<T>) {
        putString(key, value.serializeEnums())
    }
}

inline fun <reified T> createEnumListPreference(
    sharedPreferences: SharedPreferences,
    key: String,
    defaultValue: List<T>,
    titleResId: Int? = null,
): EnumListPreference<T> where T : Enum<T> {
    return EnumListPreference(key, sharedPreferences, T::class.java, defaultValue, titleResId)
}

inline fun <reified T> createEnumPreference(
    sharedPreferences: SharedPreferences,
    key: String,
    defaultValue: T,
    titleResId: Int? = null,
): EnumPreference<T> where T : Enum<T>, T : TextProvider {
    return EnumPreference(sharedPreferences, key, defaultValue, T::class.java, titleResId)
}

class IntPreference(
    val key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: Int,
    val min: Int,
    val max: Int,
    titleResId: Int? = null,
) : BasePreference<Int>(sharedPreferences, defaultValue, titleResId) {

    override fun getValue(sharedPreferences: SharedPreferences): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    override fun SharedPreferences.Editor.put(value: Int) {
        putInt(key, value)
    }
}

class FloatPreference(
    val key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: Float,
    val min: Float,
    val max: Float,
    titleResId: Int? = null,
) : BasePreference<Float>(sharedPreferences, defaultValue, titleResId) {

    override fun getValue(sharedPreferences: SharedPreferences): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    override fun SharedPreferences.Editor.put(value: Float) {
        putFloat(key, value)
    }
}

class LongPreference(val key: String, sharedPreferences: SharedPreferences, defaultValue: Long) :
    BasePreference<Long>(sharedPreferences, defaultValue) {

    override fun getValue(sharedPreferences: SharedPreferences): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    override fun SharedPreferences.Editor.put(value: Long) {
        putLong(key, value)
    }
}

class StringPreference(
    private val key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: String,
    titleResId: Int? = null,
) : BasePreference<String>(sharedPreferences, defaultValue, titleResId) {

    override fun getValue(sharedPreferences: SharedPreferences): String {
        return sharedPreferences.getString(key, defaultValue)!!
    }

    override fun SharedPreferences.Editor.put(value: String) {
        putString(key, value)
    }
}

class BooleanPreference(
    private val key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: Boolean,
    titleResId: Int? = null,
) : BasePreference<Boolean>(sharedPreferences, defaultValue, titleResId) {

    override fun getValue(sharedPreferences: SharedPreferences): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    override fun SharedPreferences.Editor.put(value: Boolean) {
        putBoolean(key, value)
    }
}

class ByteArrayPreference(
    val key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: ByteArray?,
    titleResId: Int? = null,
) : BasePreference<ByteArray?>(sharedPreferences, defaultValue, titleResId) {

    override fun getValue(sharedPreferences: SharedPreferences): ByteArray? {
        return sharedPreferences.getString(key, null)?.toPreservedByteArray ?: defaultValue
    }

    override fun SharedPreferences.Editor.put(value: ByteArray?) {
        putString(key, value?.toPreservedString)
    }
}

class EncryptedPassphrasePreference(
    val key: String,
    sharedPreferences: SharedPreferences,
    defaultValue: ByteArray,
    titleResId: Int? = null,
) : BasePreference<ByteArray>(sharedPreferences, defaultValue, titleResId) {

    override fun getValue(sharedPreferences: SharedPreferences): ByteArray {
        return sharedPreferences.getString(key, null)?.toPreservedByteArray ?: defaultValue
    }

    override fun SharedPreferences.Editor.put(value: ByteArray) {
        putString(key, value.toPreservedString)
    }

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

enum class NotesView(override val textResId: Int) : StaticTextProvider {
    LIST(R.string.list),
    GRID(R.string.grid),
}

enum class Theme(override val textResId: Int) : StaticTextProvider {
    LIGHT(R.string.light),
    DARK(R.string.dark),
    SUPER_DARK(R.string.super_dark),
    FOLLOW_SYSTEM(R.string.follow_system),
}

enum class DateFormat : TextProvider {
    NONE,
    RELATIVE,
    ABSOLUTE_SHORT,
    TIMESTAMP_SHORT,
    SHORT_ISO,
    ABSOLUTE;

    override fun getText(context: Context): String {
        if (this == NONE) {
            return context.getString(R.string.none)
        }
        return Date(System.currentTimeMillis() - 86400000).format(this)
    }
}

typealias TextSizeSp = Float

val TextSizeSp.editBodySize: Float
    get() = this

val TextSizeSp.editTitleSize: Float
    get() = (this + 4)

val TextSizeSp.displayBodySize: Float
    get() = (this - 2)

val TextSizeSp.displaySmallerSize: Float
    get() = (this - 3)

val TextSizeSp.displayTitleSize: Float
    get() = this

enum class ListItemSort(override val textResId: Int) : StaticTextProvider {
    NO_AUTO_SORT(R.string.no_auto_sort),
    AUTO_SORT_BY_CHECKED(R.string.auto_sort_by_checked),
    AUTO_SORT_BY_CHECKED_TIMESTAMP(R.string.auto_sort_by_checked_timestamp),
}

val ListItemSort.isAutoSortChecked
    get() =
        this in
            setOf(ListItemSort.AUTO_SORT_BY_CHECKED, ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP)

fun ListItemSort.callback(adapterChecked: CheckedListItemAdapter) =
    when (this) {
        ListItemSort.AUTO_SORT_BY_CHECKED_TIMESTAMP ->
            ListItemCheckedTimestampSortCallback(adapterChecked)
        else -> ListItemParentSortCallback(adapterChecked)
    }

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

enum class BiometricLock(override val textResId: Int) : StaticTextProvider {
    ENABLED(R.string.enabled),
    DISABLED(R.string.disabled);

    override fun getText(context: Context): String = context.getString(textResId)
}

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
    RESTORE(R.string.restore, R.drawable.restore);

    fun getTitleAndIcon(
        pinned: Boolean,
        viewMode: NoteViewMode?,
        folder: Folder? = null,
        type: Type? = null,
    ): Pair<Int, Int> {
        val icon =
            when (this) {
                PIN -> if (pinned) R.drawable.unpin else R.drawable.pin
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

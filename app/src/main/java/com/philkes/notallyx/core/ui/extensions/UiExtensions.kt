package com.philkes.notallyx.core.ui.extensions

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SuggestionSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.getSpans
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.getUrl
import com.philkes.notallyx.core.common.result.ImportProgress
import com.philkes.notallyx.core.common.result.ImportStage
import com.philkes.notallyx.core.common.result.Progress
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.core.ui.dialog.ConfirmDialog
import com.philkes.notallyx.core.ui.dialog.TextInputDialog
import com.philkes.notallyx.core.ui.theme.NotallyXTheme
import com.philkes.notallyx.core.ui.views.StylableEditTextWithHistory
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.data.model.Header
import com.philkes.notallyx.data.model.Item
import com.philkes.notallyx.data.model.SpanRepresentation
import com.philkes.notallyx.data.model.extensions.findNextNotificationDate
import com.philkes.notallyx.data.model.extensions.haveAnyRepetition
import com.philkes.notallyx.feature.editor.EditActivityViewModel
import com.philkes.notallyx.feature.editor.history.ChangeHistory
import com.philkes.notallyx.feature.editor.history.changes.EditTextState
import com.philkes.notallyx.feature.editor.history.changes.EditTextWithHistoryChange
import com.philkes.notallyx.feature.editor.listitem.ListItemVH
import com.philkes.notallyx.feature.editor.listitem.ListManager
import com.philkes.notallyx.feature.labels.LabelsViewModel
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.backup.NotesAndAttachments
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollNestedScrollView
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import me.zhanghai.android.fastscroll.PopupStyles

/**
 * For some reason, this method crashes sometimes with an IndexOutOfBoundsException that I've not
 * been able to replicate. When this happens, to prevent the entire app from crashing and becoming
 * unusable, the exception is suppressed.
 */
fun String.applySpans(representations: List<SpanRepresentation>): Editable {
    val editable = Editable.Factory.getInstance().newEditable(this)
    representations.forEach { (start, end, bold, link, linkData, italic, monospace, strikethrough)
        ->
        try {
            if (bold) {
                editable.setSpan(createBoldSpan(), start, end)
            }
            if (italic) {
                editable.setSpan(StyleSpan(Typeface.ITALIC), start, end)
            }
            if (link) {
                val url = linkData ?: getUrl(start, end)
                editable.setSpan(URLSpan(url), start, end)
            }
            if (monospace) {
                editable.setSpan(TypefaceSpan("monospace"), start, end)
            }
            if (strikethrough) {
                editable.setSpan(StrikethroughSpan(), start, end)
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
        }
    }
    return editable
}

fun createBoldSpan() =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        StyleSpan(Typeface.BOLD, 700)
    } else {
        StyleSpan(Typeface.BOLD)
    }

/**
 * Adjusts or removes spans based on the selection range.
 *
 * @param selectionStart the start index of the selection
 * @param selectionEnd the end index of the selection
 */
fun Editable.removeSelectionFromSpans(
    selectionStart: Int,
    selectionEnd: Int,
    spans: Collection<CharacterStyle> =
        getSpans(selectionStart, selectionEnd, CharacterStyle::class.java).toList(),
) {
    for (span in spans) {
        val spanStart = getSpanStart(span)
        val spanEnd = getSpanEnd(span)

        when {
            // Case 1: Selection is exactly the span's range (remove entire span)
            selectionStart <= spanStart && selectionEnd >= spanEnd -> {
                removeSpan(span)
            }
            // Case 2: Selection is part of the span (split the span)
            selectionStart > spanStart && selectionEnd < spanEnd -> {
                // Remove the original span
                removeSpan(span)
                // Reapply two new spans: one for the part before the selection, and one for the
                // part after
                setSpan(span, spanStart, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                setSpan(span, selectionEnd, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Case 3: Selection overlaps the start of the span (trim the span)
            selectionStart <= spanStart && selectionEnd < spanEnd -> {
                removeSpan(span)
                setSpan(span, selectionEnd, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            // Case 4: Selection overlaps the end of the span (trim the span)
            else -> {
                removeSpan(span)
                setSpan(span, spanStart, selectionStart, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }
}

private fun Spannable.setSpan(span: Any, start: Int, end: Int) {
    if (end <= length) {
        setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    } else setSpan(span, start, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun EditText.setOnNextAction(onNext: () -> Unit) {
    setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

    setOnKeyListener { v, keyCode, event ->
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            onNext()
            return@setOnKeyListener true
        } else return@setOnKeyListener false
    }

    setOnEditorActionListener { v, actionId, event ->
        if (actionId == EditorInfo.IME_ACTION_NEXT) {
            onNext()
            return@setOnEditorActionListener true
        } else return@setOnEditorActionListener false
    }
}

fun Menu.add(
    title: Int,
    drawable: Int,
    showAsAction: Int = MenuItem.SHOW_AS_ACTION_IF_ROOM,
    groupId: Int = Menu.NONE,
    itemId: Int = Menu.NONE,
    order: Int = Menu.NONE,
    onClick: (item: MenuItem) -> Unit,
): MenuItem {
    val menuItem =
        add(groupId, itemId, order, title).setIcon(drawable).setOnMenuItemClickListener { item ->
            onClick(item)
            item.isChecked = true
            return@setOnMenuItemClickListener false
        }
    menuItem.setShowAsAction(showAsAction)
    return menuItem
}

fun ViewGroup.addIconButton(
    title: Int,
    drawable: Int,
    colorInt: Int,
    marginStart: Int = 10,
    onLongClick: View.OnLongClickListener? = null,
    onClick: View.OnClickListener? = null,
): ImageButton {
    val view =
        ImageButton(ContextThemeWrapper(context, R.style.AppTheme)).apply {
            setImageResource(drawable)
            val titleText = context.getString(title)
            contentDescription = titleText
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tooltipText = titleText
            }
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.actionBarItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setOnLongClickListener(onLongClick)
            setOnClickListener(onClick)

            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            layoutParams =
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                    )
                    .apply { setMargins(marginStart.toDp, marginTop, 0, marginBottom) }
            setPadding(8.toDp)
            setControlsContrastColorForAllViews(colorInt)
        }
    addView(view)
    return view
}

fun TextView.displayFormattedTimestamp(
    timestamp: Long?,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    prefixResId: Int? = null,
) {
    if ((dateFormat != DateFormat.NONE || timeFormat != TimeFormat.NONE) && timestamp != null) {
        visibility = View.VISIBLE
        text =
            "${prefixResId?.let { getString(it) } ?: ""} ${Date(timestamp).format(dateFormat, timeFormat)}"
                .trim()
    } else visibility = View.GONE
}

fun TextView.setTextSizeSp(textSizeSp: Float) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
}

val Int.toDp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

val Float.toDpFloat: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

/**
 * Creates a TextWatcher for an EditText that is part of a list. Everytime the text is changed, a
 * Change is added to the ChangeHistory.
 *
 * @param positionGetter Function to determine the current position of the EditText in the list
 *   (e.g. the current absoluteAdapterPosition when using RecyclerViewer.Adapter)
 * @param onTextChanged optional text change handler. Returns whether or not the original change
 *   should be ignored or not.
 */
fun EditText.createListTextWatcherWithHistory(
    listManager: ListManager,
    positionGetter: () -> Int,
    onTextChanged: ((text: CharSequence, start: Int, count: Int) -> Boolean)? = null,
) =
    object : TextWatcher {
        private var ignoreOriginalChange: Boolean = false
        private lateinit var textBefore: Editable

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            textBefore = text.clone()
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            ignoreOriginalChange = onTextChanged?.invoke(s!!, start, count) ?: false
        }

        override fun afterTextChanged(s: Editable?) {
            val textAfter = s!!.clone()
            if (textAfter.hasNotChanged(textBefore)) {
                return
            }
            if (!ignoreOriginalChange) {
                listManager.changeText(
                    positionGetter.invoke(),
                    EditTextState(textAfter, selectionStart),
                )
            }
        }
    }

fun StylableEditTextWithHistory.createTextWatcherWithHistory(
    changeHistory: ChangeHistory,
    onTextChanged: ((text: CharSequence, start: Int, count: Int) -> Unit)? = null,
    updateModel: (text: Editable) -> Unit,
) =
    object : TextWatcher {
        private lateinit var stateBefore: EditTextState

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            stateBefore = EditTextState(getTextClone(), selectionStart)
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged?.invoke(s!!, start, count)
        }

        override fun afterTextChanged(s: Editable?) {
            val textAfter = requireNotNull(s, { "afterTextChanged: Editable is null" }).clone()
            if (textAfter.hasNotChanged(stateBefore.getEditableText())) {
                return
            }
            updateModel.invoke(textAfter)
            changeHistory.push(
                EditTextWithHistoryChange(
                    this@createTextWatcherWithHistory,
                    stateBefore,
                    EditTextState(textAfter, selectionStart),
                    updateModel,
                )
            )
        }
    }

fun Editable.hasNotChanged(before: Editable): Boolean {
    return toString() == before.toString() && getSpans<SuggestionSpan>().isNotEmpty()
}

fun Editable.clone(): Editable = Editable.Factory.getInstance().newEditable(this)

fun View.getString(id: Int, vararg formatArgs: String): String {
    return context.resources.getString(id, *formatArgs)
}

fun View.getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any): String {
    return context.getQuantityString(id, quantity, *formatArgs)
}

fun Folder.movedToResId(): Int {
    return when (this) {
        Folder.DELETED -> R.plurals.deleted_selected_notes
        Folder.ARCHIVED -> R.plurals.archived_selected_notes
        Folder.NOTES -> R.plurals.restored_selected_notes
    }
}

fun RadioGroup.checkedTag(): Any? {
    return this.findViewById<RadioButton?>(this.checkedRadioButtonId)?.tag
}

fun Context.showKeyboard(view: View) {
    ContextCompat.getSystemService(this, InputMethodManager::class.java)
        ?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

fun Context.hideKeyboard(view: View) {
    ContextCompat.getSystemService(this, InputMethodManager::class.java)
        ?.hideSoftInputFromWindow(view.windowToken, 0)
}

inline fun LifecycleOwner.repeatOnLifecycleScope(
    state: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit,
) = lifecycleScope.launch { repeatOnLifecycle(state) { block() } }

inline fun <T> Flow<T>.collectIn(
    owner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline action: suspend (value: T) -> Unit,
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(minActiveState) { collect { action(it) } }
    }
}

@JvmName("setupProgressDialogStateFlow")
fun StateFlow<out Progress?>.setupProgressDialog(activity: Activity) {
    setupProgressDialog(activity, activity.layoutInflater, activity as LifecycleOwner)
}

@JvmName("setupImportProgressDialogStateFlow")
fun StateFlow<out Progress?>.setupImportProgressDialog(fragment: Fragment) {
    setupProgressDialog(
        fragment.requireContext(),
        fragment.layoutInflater,
        fragment.viewLifecycleOwner,
    ) { context, progress ->
        val importProgress = progress as ImportProgress
        val stageStr =
            context.getString(
                when (importProgress.stage) {
                    ImportStage.IMPORT_NOTES -> R.string.imported_notes
                    ImportStage.EXTRACT_FILES -> R.string.extracted_files
                    ImportStage.IMPORT_FILES -> R.string.imported_files
                }
            )
        "${context.getString(R.string.count, importProgress.current, importProgress.total)} $stageStr"
    }
}

private fun <T : Progress> StateFlow<T?>.setupProgressDialog(
    context: Context,
    layoutInflater: LayoutInflater,
    viewLifecycleOwner: LifecycleOwner,
    renderProgressMessage: ((context: Context, progress: T) -> String)? = null,
) {
    val activity = context as? Activity ?: return
    val preferences = NotallyXPreferences.getInstance(activity)
    val composeView = ComposeView(context)
    val dialog =
        Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(composeView)
            setCancelable(false)
            window?.decorView?.let { decorView ->
                decorView.setViewTreeLifecycleOwner(viewLifecycleOwner)
                if (viewLifecycleOwner is ViewModelStoreOwner) {
                    decorView.setViewTreeViewModelStoreOwner(viewLifecycleOwner)
                }
                if (viewLifecycleOwner is SavedStateRegistryOwner) {
                    decorView.setViewTreeSavedStateRegistryOwner(viewLifecycleOwner)
                }
            }
        }

    viewLifecycleOwner.repeatOnLifecycleScope {
        collect { progress ->
            if (progress == null || !progress.inProgress) {
                dialog.dismiss()
                return@collect
            }

            val msg =
                when {
                    progress.indeterminate -> context.getString(R.string.calculating)
                    renderProgressMessage != null -> renderProgressMessage.invoke(context, progress)
                    else ->
                        context.getString(R.string.count, progress.current, progress.total) +
                            (progress.countSuffix?.let { " $it" } ?: "")
                }

            val progressValue =
                if (progress.indeterminate || progress.total == 0) null
                else progress.current.toFloat() / progress.total.toFloat()

            composeView.setContent {
                NotallyXTheme(theme = preferences.theme.value) {
                    com.philkes.notallyx.core.ui.dialog.ProgressDialog(
                        title = stringResource(progress.titleId),
                        message = msg,
                        progress = progressValue,
                    )
                }
            }
            if (!dialog.isShowing) {
                dialog.show()
            }
        }
    }
}

fun Activity.checkNotificationPermission(
    requestCode: Int,
    alsoCheckAlarmPermission: Boolean = false,
    alarmPermissionResultLauncher: ActivityResultLauncher<Intent>? = null,
    onSuccess: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(permission)) {
                val composeView = ComposeView(this)
                val dialog =
                    Dialog(this).apply {
                        requestWindowFeature(Window.FEATURE_NO_TITLE)
                        window?.setBackgroundDrawableResource(android.R.color.transparent)
                        setContentView(composeView)
                    }
                val preferences = NotallyXPreferences.getInstance(this)
                composeView.setContent {
                    NotallyXTheme(theme = preferences.theme.value) {
                        ConfirmDialog(
                            message = stringResource(R.string.please_grant_notally_notification),
                            positiveButtonText = stringResource(R.string.continue_),
                            negativeButtonText = stringResource(R.string.cancel),
                            onDismissRequest = { dialog.dismiss() },
                            onConfirm = { requestPermissions(arrayOf(permission), requestCode) },
                        )
                    }
                }
                dialog.show()
            } else requestPermissions(arrayOf(permission), requestCode)
        } else if (alsoCheckAlarmPermission)
            checkAlarmPermission(alarmPermissionResultLauncher, onSuccess)
        else onSuccess()
    } else if (alsoCheckAlarmPermission)
        checkAlarmPermission(alarmPermissionResultLauncher, onSuccess)
    else onSuccess()
}

fun Activity.checkAlarmPermission(
    resultLauncher: ActivityResultLauncher<Intent>?,
    onSuccess: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (getSystemService<AlarmManager>()!!.canScheduleExactAlarms()) {
            onSuccess()
        } else {
            val composeView = ComposeView(this)
            val dialog =
                Dialog(this).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    window?.setBackgroundDrawableResource(android.R.color.transparent)
                    setContentView(composeView)
                }
            val preferences = NotallyXPreferences.getInstance(this)
            composeView.setContent {
                NotallyXTheme(theme = preferences.theme.value) {
                    ConfirmDialog(
                        message = stringResource(R.string.please_grant_notally_alarm),
                        positiveButtonText = stringResource(R.string.continue_),
                        negativeButtonText = stringResource(R.string.cancel),
                        onDismissRequest = { dialog.dismiss() },
                        onConfirm = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            intent.data = Uri.parse("package:$packageName")
                            if (resultLauncher != null) {
                                resultLauncher.launch(intent)
                            } else {
                                startActivity(intent)
                            }
                        },
                    )
                }
            }
            dialog.show()
        }
    } else onSuccess()
}

fun Activity.setEnabledSecureFlag(enabled: Boolean) {
    if (enabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

fun Fragment.displayEditLabelDialog(
    oldValue: String,
    viewModel: LabelsViewModel,
    onUpdateLabel: ((oldLabel: String, newLabel: String) -> Unit)? = null,
) {
    requireContext().displayEditLabelDialog(oldValue, viewModel, layoutInflater, onUpdateLabel)
}

fun Activity.displayEditLabelDialog(
    oldValue: String,
    viewModel: EditActivityViewModel,
    onUpdateLabel: ((oldLabel: String, newLabel: String) -> Unit)? = null,
) {
    displayEditLabelDialog(oldValue, viewModel, layoutInflater, onUpdateLabel)
}

fun Context.displayEditLabelDialog(
    oldValue: String,
    viewModel: LabelsViewModel,
    layoutInflater: LayoutInflater,
    onUpdateLabel: ((oldLabel: String, newLabel: String) -> Unit)? = null,
) {
    displayEditLabelDialog(oldValue, viewModel::updateLabel, layoutInflater, onUpdateLabel)
}

fun Context.displayEditLabelDialog(
    oldValue: String,
    viewModel: EditActivityViewModel,
    layoutInflater: LayoutInflater,
    onUpdateLabel: ((oldLabel: String, newLabel: String) -> Unit)? = null,
) {
    displayEditLabelDialog(oldValue, viewModel::updateLabel, layoutInflater, onUpdateLabel)
}

fun Context.displayEditLabelDialog(
    oldValue: String,
    updateLabel: (oldValue: String, newValue: String, onComplete: (Boolean) -> Unit) -> Unit,
    layoutInflater: LayoutInflater,
    onUpdateLabel: ((oldLabel: String, newLabel: String) -> Unit)? = null,
) {
    val activity = (this as? Activity) ?: return
    val preferences = NotallyXPreferences.getInstance(activity)
    val composeView = ComposeView(activity)
    val dialog =
        Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            setContentView(composeView)
        }

    composeView.setContent {
        NotallyXTheme(theme = preferences.theme.value) {
            TextInputDialog(
                title = stringResource(R.string.edit_label),
                initialValue = oldValue,
                onDismissRequest = { dialog.dismiss() },
                onConfirm = { value ->
                    if (value.isNotEmpty()) {
                        updateLabel(oldValue, value) { success ->
                            if (success) {
                                onUpdateLabel?.invoke(oldValue, value)
                                dialog.dismiss()
                            } else showToast(R.string.label_exists)
                        }
                    }
                },
            )
        }
    }
    dialog.show()
}

fun Date.format(
    dateFormat: DateFormat = DateFormat.DD_MM_YY_GER,
    timeFormat: TimeFormat = TimeFormat.TWENTY_FOUR_H,
    ensureFullFormat: Boolean = false,
): String {
    val (effectiveDateFormat, effectiveTimeFormat) =
        if (ensureFullFormat && dateFormat != DateFormat.RELATIVE) {
            Pair(
                dateFormat.takeIf { it != DateFormat.NONE } ?: DateFormat.DD_MM_YY_GER,
                timeFormat.takeIf { it != TimeFormat.NONE } ?: TimeFormat.TWENTY_FOUR_H,
            )
        } else Pair(dateFormat, timeFormat)
    if (effectiveDateFormat == DateFormat.NONE && effectiveTimeFormat == TimeFormat.NONE) {
        return ""
    }
    val datePart = effectiveDateFormat.format(this)
    val timePart = effectiveTimeFormat.format(this)

    return if (datePart.isNotEmpty() && timePart.isNotEmpty()) {
        "$datePart $timePart"
    } else {
        datePart + timePart
    }
}

fun Context.getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any): String {
    return resources.getQuantityString(id, quantity, quantity, *formatArgs)
}

fun Context.getQuantityStringPlain(id: Int, quantity: Int): String {
    return resources.getQuantityString(id, quantity)
}

@ColorInt
fun @receiver:ColorInt Int.withAlpha(alpha: Float): Int {
    return android.graphics.Color.argb(
        (255 * alpha).toInt(),
        android.graphics.Color.red(this),
        android.graphics.Color.green(this),
        android.graphics.Color.blue(this),
    )
}

fun Context.getColorFromAttr(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    val resolved = theme.resolveAttribute(attr, typedValue, true)
    if (!resolved) {
        throw IllegalArgumentException("Attribute not found in current theme")
    }
    return if (typedValue.resourceId != 0) {
        // It's a reference (@color/something), resolve it properly
        ContextCompat.getColor(this, typedValue.resourceId)
    } else {
        // It's a direct color value
        typedValue.data
    }
}

fun View.setControlsContrastColorForAllViews(
    @ColorInt backgroundColor: Int,
    overwriteBackground: Boolean = true,
) {
    val controlsColor = context.getContrastFontColor(backgroundColor)
    setControlsColorForAllViews(controlsColor, backgroundColor, overwriteBackground)
}

var counter = 0

fun View.setControlsColorForAllViews(
    @ColorInt controlsColor: Int,
    @ColorInt backgroundColor: Int,
    overwriteBackground: Boolean = true,
) {
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.setControlsColorForAllViews(
                controlsColor,
                backgroundColor,
                overwriteBackground,
            ) // Recursive call for nested layouts
        }
        if (this is MaterialCardView) {
            checkedIconTint = ColorStateList.valueOf(controlsColor)
            val colorStateList =
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(controlsColor, controlsColor.withAlpha(0.3f)),
                )
            setStrokeColor(colorStateList)
        }
        if (this is FastScrollNestedScrollView) {
            this.addFastScroll(context, controlsColor)
        }
    } else {
        val controlsStateList =
            ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_enabled),
                    intArrayOf(-android.R.attr.state_enabled),
                ),
                intArrayOf(controlsColor, controlsColor.withAlpha(0.3f)),
            )
        if (this is Chip) {
            setTextColor(controlsStateList)
            setLinkTextColor(controlsStateList)
            val chipColor = backgroundColor.getSlightContrastColor()
            chipBackgroundColor = ColorStateList.valueOf(chipColor)
            chipIconTint = controlsStateList
            chipStrokeColor = controlsStateList
            closeIconTint = controlsStateList
            checkedIconTint = controlsStateList
            return
        }
        if (this is TextView) {
            setCompoundDrawableTint(controlsColor)
            setTextColor(controlsStateList)
            setLinkTextColor(controlsStateList)
            if (isTextSelectable || this is EditText) {
                val highlight = controlsColor.withAlpha(0.4f)
                setHintTextColor(highlight)
                highlightColor = highlight
                setSelectionHandleColor(controlsColor.withAlpha(0.8f))
            }
        }
        if (this is CompoundButton) {
            buttonTintList = controlsStateList
        }
        if (this is MaterialButton) {
            iconTint = controlsStateList
        }
        if (this is ImageButton) {
            imageTintList = controlsStateList
        }
        if (this is MaterialCheckBox) {
            buttonIconTintList = ColorStateList.valueOf(backgroundColor)
        }
        if (overwriteBackground) {
            backgroundTintList = ColorStateList.valueOf(controlsColor)
        }
    }
}

fun TextView.setSelectionHandleColor(@ColorInt color: Int) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            textSelectHandleLeft?.withTint(color)?.let { setTextSelectHandleLeft(it) }
            textSelectHandleRight?.withTint(color)?.let { setTextSelectHandleRight(it) }
            textSelectHandle?.withTint(color)?.let { setTextSelectHandle(it) }
            textCursorDrawable?.let { DrawableCompat.setTint(it, color) }
        } else {
            setSelectHandleColor(color)
        }
    } catch (_: Exception) {}
}

/**
 * This uses light-graylisted Android APIs. Verified that it works in devices running APIs 21
 * and 28. Source: https://gist.github.com/carranca/7e3414622ad7fc6ef375c8cd8dc840c9
 */
private fun TextView.setSelectHandleColor(@ColorInt color: Int) {
    // Retrieve a reference to this text field's android.widget.Editor
    val editor = getEditor()

    handles.forEach {
        // Retrieve the field pointing to the drawable currently being used for the select handle
        val resourceField = TextView::class.java.getDeclaredField(it.resourceFieldName)
        resourceField.isAccessible = true

        // Retrieve the drawable resource from that field
        val drawableId = resourceField.getInt(this)
        val drawable = ContextCompat.getDrawable(context, drawableId)

        // Apply a filter on that drawable with the desired colour
        drawable?.setColorFilter(color, PorterDuff.Mode.SRC_IN)

        // Override the drawable being used by the Editor with our coloured drawable
        val selectHandleField = editor.javaClass.getDeclaredField(it.selectHandleFieldName)
        selectHandleField.isAccessible = true
        selectHandleField.set(editor, drawable)
    }
}

private class HandleDescriptor(val resourceFieldName: String, val selectHandleFieldName: String)

private val handles =
    arrayOf(
        HandleDescriptor(
            resourceFieldName = "mTextSelectHandleRes",
            selectHandleFieldName = "mSelectHandleCenter",
        ),
        HandleDescriptor(
            resourceFieldName = "mTextSelectHandleLeftRes",
            selectHandleFieldName = "mSelectHandleLeft",
        ),
        HandleDescriptor(
            resourceFieldName = "mTextSelectHandleRightRes",
            selectHandleFieldName = "mSelectHandleRight",
        ),
    )

private fun TextView.getEditor(): Any {
    val editorField = TextView::class.java.getDeclaredField("mEditor")
    editorField.isAccessible = true
    return editorField.get(this)
}

fun TextView.setCompoundDrawableTint(@ColorInt color: Int) {
    compoundDrawablesRelative.forEach { drawable ->
        drawable?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), color) }
    }
    compoundDrawables.forEach { drawable ->
        drawable?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), color) }
    }
    (background as? MaterialShapeDrawable)?.setStrokeTint(color)
}

fun Drawable.withTint(@ColorInt color: Int): Drawable {
    val oldDrawable = DrawableCompat.unwrap<Drawable>(this)
    val newDrawable = oldDrawable.constantState?.newDrawable()?.mutate() ?: oldDrawable.mutate()
    val wrappedDrawable = DrawableCompat.wrap(newDrawable)
    DrawableCompat.setTint(wrappedDrawable, color)
    return wrappedDrawable
}

@ColorInt
fun Context.getContrastFontColor(@ColorInt backgroundColor: Int): Int {
    return if (backgroundColor.isLightColor()) ContextCompat.getColor(this, R.color.TextDark)
    else ContextCompat.getColor(this, R.color.TextLight)
}

fun @receiver:ColorInt Int.isLightColor() = ColorUtils.calculateLuminance(this) > 0.5

@ColorInt
fun @receiver:ColorInt Int.getSlightContrastColor(): Int {
    val overlayColor = if (this.isLightColor()) Color.BLACK else Color.WHITE
    // Composite black/white over the background color
    return ColorUtils.compositeColors(
        ColorUtils.setAlphaComponent(overlayColor, 15), // ~10% alpha
        this,
    )
}

fun Fragment.showDialog(
    messageResId: Int,
    positiveButtonTextResId: Int,
    onPositiveButtonClickListener: DialogInterface.OnClickListener,
    neutralButtonTextResId: Int? = null,
    onNeutralButtonClickListener: DialogInterface.OnClickListener? = null,
    fullSize: Boolean = false,
) =
    requireContext()
        .showDialog(
            messageResId,
            positiveButtonTextResId,
            onPositiveButtonClickListener,
            neutralButtonTextResId,
            onNeutralButtonClickListener,
            fullSize,
        )

fun Fragment.showDialog(
    message: String,
    positiveButtonTextResId: Int,
    onPositiveButtonClickListener: DialogInterface.OnClickListener,
    neutralButtonTextResId: Int? = null,
    onNeutralButtonClickListener: DialogInterface.OnClickListener? = null,
    fullSize: Boolean = false,
) =
    requireContext()
        .showDialog(
            message,
            positiveButtonTextResId,
            onPositiveButtonClickListener,
            neutralButtonTextResId,
            onNeutralButtonClickListener,
            fullSize,
        )

fun Context.showDialog(
    messageResId: Int,
    positiveButtonTextResId: Int,
    onPositiveButtonClickListener: DialogInterface.OnClickListener,
    neutralButtonTextResId: Int? = null,
    onNeutralButtonClickListener: DialogInterface.OnClickListener? = null,
    fullSize: Boolean = false,
) {
    showDialog(
        message = getString(messageResId),
        positiveButtonTextResId = positiveButtonTextResId,
        onPositiveButtonClickListener = onPositiveButtonClickListener,
        neutralButtonTextResId = neutralButtonTextResId,
        onNeutralButtonClickListener = onNeutralButtonClickListener,
        fullSize = fullSize,
    )
}

fun createComposeDialog(
    context: Context,
    composeView: ComposeView,
    lifecycleOwner: LifecycleOwner? = null,
): Dialog {
    val owner = lifecycleOwner ?: (context as? LifecycleOwner)
    val viewModelStoreOwner = owner as? ViewModelStoreOwner ?: (context as? ViewModelStoreOwner)
    val savedStateRegistryOwner =
        owner as? SavedStateRegistryOwner ?: (context as? SavedStateRegistryOwner)

    return Dialog(context).apply {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setContentView(composeView)
        window?.decorView?.let { decorView ->
            owner?.let { decorView.setViewTreeLifecycleOwner(it) }
            viewModelStoreOwner?.let { decorView.setViewTreeViewModelStoreOwner(it) }
            savedStateRegistryOwner?.let { decorView.setViewTreeSavedStateRegistryOwner(it) }
        }
    }
}

fun Context.showDialog(
    message: String,
    positiveButtonTextResId: Int,
    onPositiveButtonClickListener: DialogInterface.OnClickListener,
    neutralButtonTextResId: Int? = null,
    onNeutralButtonClickListener: DialogInterface.OnClickListener? = null,
    fullSize: Boolean = false,
) {
    val activity = (this as? Activity) ?: return
    val preferences = NotallyXPreferences.getInstance(activity)
    val composeView = ComposeView(activity)
    val dialog = createComposeDialog(activity, composeView)

    composeView.setContent {
        NotallyXTheme(theme = preferences.theme.value) {
            ConfirmDialog(
                message = message,
                positiveButtonText = stringResource(positiveButtonTextResId),
                negativeButtonText = stringResource(R.string.cancel),
                neutralButtonText = neutralButtonTextResId?.let { stringResource(it) },
                onNeutralButtonClick =
                    onNeutralButtonClickListener?.let { listener ->
                        { listener.onClick(dialog, DialogInterface.BUTTON_NEUTRAL) }
                    },
                onDismissRequest = { dialog.dismiss() },
                onConfirm = {
                    onPositiveButtonClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
                },
            )
        }
    }
    dialog.show()
}

fun Fragment.showToast(messageResId: Int) = requireContext().showToast(messageResId)

fun Context.showToast(messageResId: Int) =
    ContextCompat.getMainExecutor(this).execute {
        Toast.makeText(this, messageResId, Toast.LENGTH_LONG).show()
    }

fun Context.showToast(message: CharSequence) =
    ContextCompat.getMainExecutor(this).execute {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

fun View.showSnackbar(
    msgResId: Int,
    actionResId: Int? = null,
    onActionListener: View.OnClickListener? = null,
) = showSnackbar(getString(msgResId), actionResId, onActionListener)

fun View.showSnackbar(
    message: String,
    actionResId: Int? = null,
    onActionListener: View.OnClickListener? = null,
) =
    Snackbar.make(this, message, Snackbar.LENGTH_LONG)
        .apply { actionResId?.let { setAction(it, onActionListener) } }
        .show()

fun Context.restartApplication(
    fragmentIdToOpen: Int? = null,
    extra: Pair<String, Boolean>? = null,
) {
    //    val intent = packageManager.getLaunchIntentForPackage(packageName)
    //    val componentName = intent!!.component
    //    val mainIntent =
    //        Intent.makeRestartActivityTask(componentName).apply {
    //            fragmentIdToOpen?.let { putExtra(MainActivity.EXTRA_FRAGMENT_TO_OPEN, it) }
    //            extra?.let { (key, value) -> putExtra(key, value) }
    //        }
    //    mainIntent.setPackage(packageName)
    //    startActivity(mainIntent)
    //    Runtime.getRuntime().exit(0)

    val intent =
        Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            fragmentIdToOpen?.let { putExtra(MainActivity.EXTRA_FRAGMENT_TO_OPEN, it) }
            extra?.let { (key, value) -> putExtra(key, value) }
        }
    startActivity(intent)
}

@ColorInt
fun Context.extractColor(color: String): Int {
    return when (color) {
        BaseNote.COLOR_DEFAULT -> return getColorFromAttr(R.attr.colorSurface)
        else -> android.graphics.Color.parseColor(color)
    }
}

fun ViewGroup.addFastScroll(context: Context, @ColorInt colorInt: Int) {
    FastScrollerBuilder(this).useColoredStyle(context, colorInt).build()
}

fun FastScrollerBuilder.useColoredStyle(
    context: Context,
    @ColorInt colorInt: Int,
): FastScrollerBuilder {
    AppCompatResources.getDrawable(context, me.zhanghai.android.fastscroll.R.drawable.afs_md2_thumb)
        ?.mutate()
        ?.let {
            DrawableCompat.setTint(it, colorInt)
            setThumbDrawable(it)
        }
    ContextCompat.getDrawable(context, R.drawable.scroll_track)?.let { setTrackDrawable(it) }
    setPadding(0, 0, 2.toDp, 0)
    setPopupStyle(PopupStyles.MD2)
    return this
}

fun Window.setLightStatusAndNavBar(value: Boolean, view: View = decorView) {
    val windowInsetsControllerCompat = WindowInsetsControllerCompat(this, view)
    windowInsetsControllerCompat.isAppearanceLightStatusBars = value
    windowInsetsControllerCompat.isAppearanceLightNavigationBars = value
}

private fun getOutlinedDrawable(context: Context): MaterialShapeDrawable {
    val model =
        ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(RelativeCornerSize(0.5f))
            .build()

    return MaterialShapeDrawable(model).apply {
        fillColor = ColorStateList.valueOf(0)
        strokeWidth = context.resources.displayMetrics.density
        strokeColor = ContextCompat.getColorStateList(context, R.color.chip_stroke)
    }
}

fun RecyclerView.initListView(context: Context) {
    setHasFixedSize(true)
    layoutManager = LinearLayoutManager(context)
    addItemDecoration(DividerItemDecoration(context, RecyclerView.VERTICAL))
    setPadding(0, 0, 0, 0)
}

val RecyclerView.focusedViewHolder
    get() = focusedChild?.let { view ->
        val position = getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) {
            null
        } else {
            findViewHolderForAdapterPosition(position)
        }
    }

fun RecyclerView.showKeyboardOnFocusedItem() {
    (focusedViewHolder as? ListItemVH)?.let {
        it.binding.root.context?.showKeyboard(it.binding.EditText)
    }
}

fun RecyclerView.hideKeyboardOnFocusedItem() {
    (focusedViewHolder as? ListItemVH)?.let {
        it.binding.root.context?.hideKeyboard(it.binding.EditText)
    }
}

fun MaterialAutoCompleteTextView.select(value: CharSequence) {
    setText(value, false)
}

fun Context.createTextView(textResId: Int, padding: Int = 16.toDp): TextView {
    return AppCompatTextView(this).apply {
        setText(textResId)
        TextViewCompat.setTextAppearance(
            this,
            android.R.style.TextAppearance_Material_DialogWindowTitle,
        )
        updatePadding(padding, padding, padding, padding)
        maxLines = Integer.MAX_VALUE
        ellipsize = null
    }
}

fun Chip.setupReminderChip(
    baseNote: BaseNote,
    dateFormat: DateFormat,
    timeFormat: TimeFormat,
    textSize: Float? = null,
) {
    val now = Date(System.currentTimeMillis())
    val mostRecentNotificationDate =
        baseNote.reminders.findNextNotificationDate()
            ?: baseNote.reminders.maxOfOrNull { it.dateTime }
    if (mostRecentNotificationDate == null) {
        this.visibility = GONE
        return
    }
    this.apply {
        visibility = VISIBLE
        text = mostRecentNotificationDate.format(dateFormat, timeFormat, ensureFullFormat = true)
        textSize?.let {
            setTextSizeSp(it)
            chipIconSize =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    it + 4,
                    resources.displayMetrics,
                )
        }
        setCloseIconVisible(baseNote.reminders.haveAnyRepetition())
        val isElapsed = mostRecentNotificationDate < now
        alpha = if (isElapsed) 0.5f else 1.0f
        paintFlags =
            if (isElapsed) paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }
}

fun Context.exportedText(notesAndAttachments: NotesAndAttachments) =
    "${getString(R.string.exported)} ${notesAndAttachments.first} ${if(notesAndAttachments.first == 1) getString(R.string.note) else getString(R.string.notes)} (${notesAndAttachments.second} ${getQuantityString(R.plurals.attachments, notesAndAttachments.second)})"

fun Context.createItemsFromNotes(list: List<BaseNote>): List<Item> {
    val pinned by lazy { Header(getString(R.string.pinned)) }
    val others by lazy { Header(getString(R.string.others)) }
    val archived by lazy { Header(getString(R.string.archived)) }
    if (list.isEmpty()) {
        return list
    } else {
        val firstPinnedNote = list.indexOfFirst { baseNote -> baseNote.pinned }
        val firstUnpinnedNote = list.indexOfFirst { baseNote ->
            !baseNote.pinned && baseNote.folder != Folder.ARCHIVED
        }
        val mutableList: MutableList<Item> = list.toMutableList()
        if (firstPinnedNote != -1) {
            mutableList.add(firstPinnedNote, pinned)
            if (firstUnpinnedNote != -1) {
                mutableList.add(firstUnpinnedNote + 1, others)
            }
        }
        val firstArchivedNote = mutableList.indexOfFirst { item ->
            item is BaseNote && item.folder == Folder.ARCHIVED
        }
        if (firstArchivedNote != -1) {
            mutableList.add(firstArchivedNote, archived)
        }
        return mutableList
    }
}

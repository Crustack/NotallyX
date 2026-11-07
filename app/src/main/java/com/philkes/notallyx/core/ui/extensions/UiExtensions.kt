package com.philkes.notallyx.core.ui.extensions

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.extensions.putNavKey
import com.philkes.notallyx.core.preference.DateFormat
import com.philkes.notallyx.core.preference.TimeFormat
import com.philkes.notallyx.data.model.BaseNote
import com.philkes.notallyx.data.model.Folder
import com.philkes.notallyx.feature.main.MainActivity
import com.philkes.notallyx.feature.migration.backup.NotesAndAttachments
import com.philkes.notallyx.navigation.NotallyNavKey
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

fun View.getString(id: Int, vararg formatArgs: String): String {
    return context.resources.getString(id, *formatArgs)
}

fun Folder.movedToResId(): Int {
    return when (this) {
        Folder.DELETED -> R.plurals.deleted_selected_notes
        Folder.ARCHIVED -> R.plurals.archived_selected_notes
        Folder.NOTES -> R.plurals.restored_selected_notes
    }
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

fun Activity.checkNotificationPermission(
    permissionLauncher: ActivityResultLauncher<String>? = null,
    alsoCheckAlarmPermission: Boolean = false,
    alarmPermissionResultLauncher: ActivityResultLauncher<Intent>? = null,
    onShowRationale: ((messageResId: Int, onConfirm: () -> Unit) -> Unit)? = null,
    onSuccess: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            val onConfirm: () -> Unit = { permissionLauncher?.launch(permission) }
            if (shouldShowRequestPermissionRationale(permission)) {
                if (onShowRationale != null) {
                    onShowRationale(R.string.please_grant_notally_notification, onConfirm)
                } else {
                    onConfirm()
                }
            } else {
                onConfirm()
            }
        } else if (alsoCheckAlarmPermission) {
            checkAlarmPermission(alarmPermissionResultLauncher, onShowRationale, onSuccess)
        } else {
            onSuccess()
        }
    } else if (alsoCheckAlarmPermission) {
        checkAlarmPermission(alarmPermissionResultLauncher, onShowRationale, onSuccess)
    } else {
        onSuccess()
    }
}

fun Activity.checkAlarmPermission(
    resultLauncher: ActivityResultLauncher<Intent>?,
    onShowRationale: ((messageResId: Int, onConfirm: () -> Unit) -> Unit)? = null,
    onSuccess: () -> Unit,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (getSystemService<AlarmManager>()!!.canScheduleExactAlarms()) {
            onSuccess()
        } else {
            val onConfirm = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:$packageName")
                if (resultLauncher != null) {
                    resultLauncher.launch(intent)
                } else {
                    startActivity(intent)
                }
            }
            if (onShowRationale != null) {
                onShowRationale(R.string.please_grant_notally_alarm, onConfirm)
            } else {
                onConfirm()
            }
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

@ColorInt
fun Context.getContrastFontColor(@ColorInt backgroundColor: Int): Int {
    return if (backgroundColor.isLightColor()) ContextCompat.getColor(this, R.color.TextDark)
    else ContextCompat.getColor(this, R.color.TextLight)
}

fun @receiver:ColorInt Int.isLightColor() = ColorUtils.calculateLuminance(this) > 0.5

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
    destinationToOpen: NotallyNavKey? = null,
    extra: Pair<String, Boolean>? = null,
) {
    val intent =
        Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            destinationToOpen?.let { putNavKey(it) }
            extra?.let { (key, value) -> putExtra(key, value) }
        }
    startActivity(intent)
}

@ColorInt
fun Context.extractColor(color: String): Int {
    return when (color) {
        BaseNote.COLOR_DEFAULT -> getColorFromAttr(R.attr.colorSurface)
        else -> Color.parseColor(color)
    }
}

fun Window.setLightStatusAndNavBar(value: Boolean, view: View = decorView) {
    val windowInsetsControllerCompat = WindowInsetsControllerCompat(this, view)
    windowInsetsControllerCompat.isAppearanceLightStatusBars = value
    windowInsetsControllerCompat.isAppearanceLightNavigationBars = value
}

fun Context.exportedText(notesAndAttachments: NotesAndAttachments) =
    "${getString(R.string.exported)} ${notesAndAttachments.first} ${if(notesAndAttachments.first == 1) getString(R.string.note) else getString(R.string.notes)} (${notesAndAttachments.second} ${getQuantityString(R.plurals.attachments, notesAndAttachments.second)})"

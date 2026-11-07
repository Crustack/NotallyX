package com.philkes.notallyx.core.ui.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.philkes.notallyx.core.ui.model.UiText

@Composable
fun UiText.asString(): String {
    return when (this) {
        is UiText.DynamicString -> value
        is UiText.StringRes ->
            if (args.isEmpty()) stringResource(resId)
            else stringResource(resId, *args.toTypedArray())
        is UiText.PluralRes ->
            if (args.isEmpty()) pluralStringResource(resId, quantity, quantity)
            else pluralStringResource(resId, quantity, *args.toTypedArray())
    }
}

fun UiText.asString(context: Context): String {
    return when (this) {
        is UiText.DynamicString -> value
        is UiText.StringRes ->
            if (args.isEmpty()) context.getString(resId)
            else context.getString(resId, *args.toTypedArray())
        is UiText.PluralRes ->
            if (args.isEmpty()) context.resources.getQuantityString(resId, quantity, quantity)
            else context.resources.getQuantityString(resId, quantity, *args.toTypedArray())
    }
}

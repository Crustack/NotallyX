package com.philkes.notallyx.core.ui.platform

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * Platform-independent localized text representation used by ViewModels and MVI `SideEffect`s so
 * shared presentation logic does not depend on `Context.getString(...)`.
 */
sealed interface UiText {
    data class DynamicString(val value: String) : UiText

    data class StringRes(val resId: Int, val args: List<Any> = emptyList()) : UiText {
        constructor(resId: Int, vararg args: Any) : this(resId, args.toList())
    }

    data class PluralRes(
        val resId: Int,
        val quantity: Int,
        val args: List<Any> = emptyList(),
    ) : UiText {
        constructor(
            resId: Int,
            quantity: Int,
            vararg args: Any,
        ) : this(resId, quantity, args.toList())
    }

    @Composable
    fun asString(): String {
        return when (this) {
            is DynamicString -> value
            is StringRes ->
                if (args.isEmpty()) stringResource(resId)
                else stringResource(resId, *args.toTypedArray())
            is PluralRes ->
                if (args.isEmpty()) pluralStringResource(resId, quantity, quantity)
                else pluralStringResource(resId, quantity, *args.toTypedArray())
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringRes ->
                if (args.isEmpty()) context.getString(resId)
                else context.getString(resId, *args.toTypedArray())
            is PluralRes ->
                if (args.isEmpty()) context.resources.getQuantityString(resId, quantity, quantity)
                else context.resources.getQuantityString(resId, quantity, *args.toTypedArray())
        }
    }
}

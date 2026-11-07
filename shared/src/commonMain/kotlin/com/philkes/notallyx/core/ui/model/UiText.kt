package com.philkes.notallyx.core.ui.model

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
}

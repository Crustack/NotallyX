package com.philkes.notallyx.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

val LocalNavigator = staticCompositionLocalOf<Navigator?> { null }

class Navigator(
    val state: NavigationState,
    val skipStartViewOnBack: Boolean = false,
) {
    private val _reselectEvents = MutableSharedFlow<NotallyNavKey>(extraBufferCapacity = 1)
    val reselectEvents = _reselectEvents.asSharedFlow()

    val currentTopLevelRoute: NotallyNavKey
        get() = state.topLevelRoute

    val currentStack: NavBackStack<NavKey>
        get() =
            state.backStacks[state.topLevelRoute]
                ?: error("Stack for ${state.topLevelRoute} not found")

    val currentRoute: NavKey
        get() = currentStack.last()

    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            if (state.topLevelRoute == route) {
                val stack = state.backStacks[route]
                if (stack != null && stack.size > 1) {
                    while (stack.size > 1) {
                        stack.removeLastOrNull()
                    }
                } else {
                    _reselectEvents.tryEmit(route as NotallyNavKey)
                }
            } else {
                state.topLevelRoute = route as NotallyNavKey
            }
        } else if (route is NotallyNavKey.DisplayLabel) {
            if (state.topLevelRoute != NotallyNavKey.Labels) {
                state.topLevelRoute = NotallyNavKey.Labels
            }
            val stack = state.backStacks[NotallyNavKey.Labels] ?: currentStack
            while (stack.size > 1 && stack.last() is NotallyNavKey.DisplayLabel) {
                stack.removeLastOrNull()
            }
            stack.add(route)
        } else {
            currentStack.add(route)
        }
    }

    fun onReselect(route: NotallyNavKey) {
        _reselectEvents.tryEmit(route)
    }

    fun goBack(): Boolean {
        val stack = currentStack
        if (stack.isEmpty()) return false
        val current = stack.last()

        return if (stack.size <= 1 || current == state.topLevelRoute) {
            if (state.topLevelRoute != state.startRoute && !skipStartViewOnBack) {
                state.topLevelRoute = state.startRoute
                true
            } else {
                false
            }
        } else {
            stack.removeLastOrNull() != null
        }
    }
}

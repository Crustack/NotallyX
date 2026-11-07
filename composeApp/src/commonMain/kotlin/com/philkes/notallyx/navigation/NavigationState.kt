package com.philkes.notallyx.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.ResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val NotallyNavSavedStateConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(NotallyNavKey.Notes.serializer())
            subclass(NotallyNavKey.Unlabeled.serializer())
            subclass(NotallyNavKey.Labels.serializer())
            subclass(NotallyNavKey.Deleted.serializer())
            subclass(NotallyNavKey.Archived.serializer())
            subclass(NotallyNavKey.Reminders.serializer())
            subclass(NotallyNavKey.Settings.serializer())
            subclass(NotallyNavKey.DisplayLabel.serializer())
            subclass(NotallyNavKey.Search.serializer())
            subclass(NotallyNavKey.EditNote.serializer())
            subclass(NotallyNavKey.EditList.serializer())
            subclass(NotallyNavKey.EditTextPlain.serializer())
            subclass(NotallyNavKey.ViewImage.serializer())
            subclass(NotallyNavKey.PlayAudio.serializer())
            subclass(NotallyNavKey.RecordAudio.serializer())
            subclass(NotallyNavKey.PickNote.serializer())
            subclass(NotallyNavKey.NoteReminders.serializer())
            subclass(NotallyNavKey.SelectLabels.serializer())
            subclass(NotallyNavKey.ColorPicker.serializer())
            subclass(NotallyNavKey.ExportFormat.serializer())
            subclass(NotallyNavKey.Confirm.serializer())
        }
    }
}

val DEFAULT_TOP_LEVEL_NAV_KEYS: Set<NotallyNavKey> =
    setOf(
        NotallyNavKey.Notes,
        NotallyNavKey.Unlabeled,
        NotallyNavKey.Labels,
        NotallyNavKey.Deleted,
        NotallyNavKey.Archived,
        NotallyNavKey.Reminders,
        NotallyNavKey.Settings,
    )

@Composable
fun rememberNavigationState(
    startRoute: NotallyNavKey,
    topLevelRoutes: Set<NotallyNavKey> = DEFAULT_TOP_LEVEL_NAV_KEYS,
): NavigationState {
    val allTopLevelRoutes =
        remember(startRoute, topLevelRoutes) {
            if (startRoute !in topLevelRoutes) {
                topLevelRoutes + startRoute
            } else {
                topLevelRoutes
            }
        }

    val topLevelRoute =
        rememberSerializable(
            startRoute,
            allTopLevelRoutes,
            configuration = NotallyNavSavedStateConfig,
            serializer = MutableStateSerializer(NotallyNavKey.serializer()),
        ) {
            mutableStateOf(startRoute)
        }

    val backStacks = allTopLevelRoutes.associateWith { key ->
        rememberNavBackStack(NotallyNavSavedStateConfig, key)
    }

    val resultEventBus = rememberResultEventBus()

    return remember(startRoute, allTopLevelRoutes, resultEventBus) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
            resultEventBus = resultEventBus,
        )
    }
}

class NavigationState(
    val startRoute: NotallyNavKey,
    topLevelRoute: MutableState<NotallyNavKey>,
    val backStacks: Map<NotallyNavKey, NavBackStack<NavKey>>,
    val resultEventBus: ResultEventBus,
) {
    var topLevelRoute: NotallyNavKey by topLevelRoute

    @Composable
    fun toDecoratedEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
        val decoratedEntries = backStacks.mapValues { (_, stack) ->
            val decorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                    rememberViewModelStoreNavEntryDecorator<NavKey>(),
                    rememberResultEventBusNavEntryDecorator<NavKey>(resultEventBus),
                )
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = decorators,
                entryProvider = entryProvider,
            )
        }

        return getTopLevelRoutesInUse().flatMap { decoratedEntries[it] ?: emptyList() }
    }

    fun getTopLevelRoutesInUse(): List<NotallyNavKey> =
        if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

package com.philkes.notallyx.navigation

import androidx.navigation3.runtime.NavKey
import com.philkes.notallyx.data.model.Folder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NotallyNavKey : NavKey {

    @Serializable @SerialName("Notes") data object Notes : NotallyNavKey

    @Serializable @SerialName("Labels") data object Labels : NotallyNavKey

    @Serializable @SerialName("Deleted") data object Deleted : NotallyNavKey

    @Serializable @SerialName("Archived") data object Archived : NotallyNavKey

    @Serializable @SerialName("Reminders") data object Reminders : NotallyNavKey

    @Serializable @SerialName("Settings") data object Settings : NotallyNavKey

    @Serializable @SerialName("Unlabeled") data object Unlabeled : NotallyNavKey

    @Serializable
    @SerialName("DisplayLabel")
    data class DisplayLabel(val label: String) : NotallyNavKey

    @Serializable
    @SerialName("Search")
    data class Search(
        val initialFolder: Folder? = null,
        val initialLabel: String? = null,
    ) : NotallyNavKey
}

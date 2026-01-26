package com.philkes.notallyx.data.remote

import kotlinx.serialization.Serializable

interface NotallyApi {
    @retrofit2.http.GET("sync/pull")
    suspend fun pullChanges(@retrofit2.http.Query("lastSync") lastSync: Long): SyncResponse

    @retrofit2.http.POST("sync/push")
    suspend fun pushChanges(@retrofit2.http.Body request: SyncRequest): SyncRequestResponse

    @retrofit2.http.POST("clients/register")
    suspend fun registerDevice(@retrofit2.http.Body request: RegisterDeviceRequest): RegisterDeviceResponse
}

@Serializable
data class SyncResponse(
    val notes: List<SyncNote>,
    val timestamp: Long
)

@Serializable
data class SyncRequest(
    val deviceId: String,
    val notes: List<SyncNote>,
    val labels: List<SyncLabel>? = null // Optional if not synced separately yet
)

@Serializable
data class SyncRequestResponse(
    val status: String,
    val timestamp: Long
)

@Serializable
data class RegisterDeviceRequest(
    val deviceId: String,
    val model: String
)

@Serializable
data class RegisterDeviceResponse(
    val id: Long,
    val status: String
)

@Serializable
data class SyncLabel(
    val value: String
)

@Serializable
data class SyncNote(
    val id: Long = 0, // 0 for new notes from client, but sync usually keeps IDs? For now assume ID sync.
    val type: String,
    val folder: String,
    val color: String,
    val title: String,
    val pinned: Boolean,
    val timestamp: Long,
    val modifiedTimestamp: Long,
    val labels: List<String>,
    val body: String,
    val spans: List<SyncSpan> = emptyList(),
    val items: List<SyncListItem> = emptyList(),
    val viewMode: String = "EDIT"
)

@Serializable
data class SyncSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean = false,
    val link: Boolean = false,
    val linkData: String? = null,
    val italic: Boolean = false,
    val monospace: Boolean = false,
    val strikethrough: Boolean = false,
)

@Serializable
data class SyncListItem(
    val body: String,
    val checked: Boolean,
    val isChild: Boolean,
    val order: Int?,
    val children: List<SyncListItem> = emptyList(),
    val id: Int = -1
)

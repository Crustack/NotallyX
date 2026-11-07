package com.philkes.notallyx.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Audio(var name: String, var duration: Long?, var timestamp: Long) : Attachment

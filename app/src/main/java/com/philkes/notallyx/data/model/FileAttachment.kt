package com.philkes.notallyx.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileAttachment(var localName: String, var originalName: String, var mimeType: String) :
    Attachment

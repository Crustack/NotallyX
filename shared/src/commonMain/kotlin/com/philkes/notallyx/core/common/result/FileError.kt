package com.philkes.notallyx.core.common.result

import com.philkes.notallyx.data.model.FileType

data class FileError(
    val name: String,
    val description: String,
    val fileType: FileType,
)

package com.philkes.notallyx.core.common.result

import com.philkes.notallyx.feature.editor.NoteModel

data class FileError(
    val name: String,
    val description: String,
    val fileType: NoteModel.FileType,
)

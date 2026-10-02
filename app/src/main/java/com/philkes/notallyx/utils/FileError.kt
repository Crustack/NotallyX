package com.philkes.notallyx.utils

import com.philkes.notallyx.presentation.viewmodel.edit.NoteModel

data class FileError(
    val name: String,
    val description: String,
    val fileType: NoteModel.FileType,
)

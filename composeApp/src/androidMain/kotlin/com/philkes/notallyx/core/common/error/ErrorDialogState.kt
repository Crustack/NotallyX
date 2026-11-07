package com.philkes.notallyx.core.common.error

data class ErrorDialogState(
    val titleResId: Int,
    val message: String,
    val details: String,
    val neutralButtonTextResId: Int? = null,
    val onNeutralButtonClick: (() -> Unit)? = null,
)

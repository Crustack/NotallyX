package com.philkes.notallyx.core.common.result

abstract class Progress(
    val titleId: Int,
    val current: Int = 0,
    val total: Int = 0,
    val inProgress: Boolean = true,
    val indeterminate: Boolean = false,
    val countSuffix: String? = null,
)

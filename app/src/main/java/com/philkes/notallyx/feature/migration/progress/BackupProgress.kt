package com.philkes.notallyx.feature.migration.progress

import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.result.Progress

open class BackupProgress(
    current: Int = 0,
    total: Int = 0,
    inProgress: Boolean = true,
    indeterminate: Boolean = false,
    countSuffix: String? = null,
) : Progress(R.string.export_backup, current, total, inProgress, indeterminate, countSuffix)

package com.philkes.notallyx.feature.migration.progress

import com.philkes.notallyx.R
import com.philkes.notallyx.core.common.result.Progress

open class DeleteAttachmentProgress(
    current: Int = 0,
    total: Int = 0,
    inProgress: Boolean = true,
    indeterminate: Boolean = false,
) : Progress(R.string.deleting_files, current, total, inProgress, indeterminate)

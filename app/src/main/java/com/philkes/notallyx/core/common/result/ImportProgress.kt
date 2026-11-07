package com.philkes.notallyx.core.common.result

import com.philkes.notallyx.R

open class ImportProgress(
    current: Int = 0,
    total: Int = 0,
    inProgress: Boolean = true,
    indeterminate: Boolean = false,
    val stage: ImportStage = ImportStage.IMPORT_NOTES,
) : Progress(R.string.importing_backup, current, total, inProgress, indeterminate)

enum class ImportStage {
    IMPORT_NOTES,
    EXTRACT_FILES,
    IMPORT_FILES,
}

package com.philkes.notallyx.core.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

class PeriodicBackupsPreference(
    flow: StateFlow<PeriodicBackup>,
    onUpdate: suspend (PeriodicBackup) -> Unit,
    scope: CoroutineScope,
) :
    PreferenceHolder<PeriodicBackup>(
        key = BACKUP_PERIOD_DAYS_KEY,
        defaultValue = PeriodicBackup(),
        flow = flow,
        onUpdate = onUpdate,
        scope = scope,
    ) {
    companion object {
        const val BACKUP_PERIOD_DAYS_KEY = "autoBackupPeriodDays"
        const val BACKUP_MAX_KEY = "maxBackups"
    }
}

@Serializable
data class PeriodicBackup(val periodInDays: Int = 1, val maxBackups: Int = 3) {
    companion object {

        const val BACKUP_PERIOD_DAYS_MIN = 1
        const val BACKUP_PERIOD_DAYS_MAX = 31

        const val BACKUP_MAX_MIN = 1
        const val BACKUP_MAX_MAX = 10
    }
}

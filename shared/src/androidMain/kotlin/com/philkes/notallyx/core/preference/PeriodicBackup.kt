package com.philkes.notallyx.core.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

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

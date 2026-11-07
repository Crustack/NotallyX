package com.philkes.notallyx.core.preference

import kotlinx.serialization.Serializable

@Serializable
data class PeriodicBackup(val periodInDays: Int = 1, val maxBackups: Int = 3) {
    companion object {
        const val BACKUP_PERIOD_DAYS_MIN = 1
        const val BACKUP_PERIOD_DAYS_MAX = 31

        const val BACKUP_MAX_MIN = 1
        const val BACKUP_MAX_MAX = 10
    }
}

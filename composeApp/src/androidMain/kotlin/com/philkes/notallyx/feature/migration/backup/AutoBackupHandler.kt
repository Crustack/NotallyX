package com.philkes.notallyx.feature.migration.backup

import android.content.Context
import android.content.ContextWrapper
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.data.model.BaseNote

object AutoBackupHandler {
    var onCheckBackupOnSave:
        ((
            context: Context,
            preferences: NotallyXPreferences,
            note: BaseNote?,
            forceFullBackup: Boolean,
        ) -> Unit)? =
        null
}

fun ContextWrapper.checkBackupOnSave(
    preferences: NotallyXPreferences,
    note: BaseNote? = null,
    forceFullBackup: Boolean = false,
) {
    AutoBackupHandler.onCheckBackupOnSave?.invoke(this, preferences, note, forceFullBackup)
}

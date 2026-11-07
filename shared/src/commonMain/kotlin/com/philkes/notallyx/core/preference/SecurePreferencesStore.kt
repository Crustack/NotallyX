package com.philkes.notallyx.core.preference

import androidx.datastore.core.DataStore
import com.philkes.notallyx.core.preference.model.SecurePreferences

const val USER_PREFERENCES_FILE_NAME = "notallyx_preferences.json"
const val SECURE_PREFERENCES_FILE_NAME = "notallyx_secure_preferences.bin"

interface SecurePreferencesStore {
    val dataStore: DataStore<SecurePreferences>
}

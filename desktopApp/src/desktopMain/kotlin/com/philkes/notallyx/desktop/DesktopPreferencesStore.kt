package com.philkes.notallyx.desktop

import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.feature.settings.PreferencesUiModel
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

class DesktopPreferencesStore(
    private val pathProvider: PathProvider,
    initialPreferences: PreferencesUiModel? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    private val preferencesFile: File
        get() = File(pathProvider.getPreferencesDirectory(), PREFERENCES_FILE_NAME)

    private val _preferences = MutableStateFlow(initialPreferences ?: loadPreferences())
    val preferences: StateFlow<PreferencesUiModel> = _preferences.asStateFlow()

    private fun loadPreferences(): PreferencesUiModel {
        return try {
            if (preferencesFile.exists()) {
                json.decodeFromString<PreferencesUiModel>(preferencesFile.readText())
            } else {
                PreferencesUiModel()
            }
        } catch (_: Exception) {
            PreferencesUiModel()
        }
    }

    fun updatePreferences(update: (PreferencesUiModel) -> PreferencesUiModel) {
        val newPrefs = update(_preferences.value)
        _preferences.value = newPrefs
        savePreferencesDirectly(newPrefs)
    }

    fun setPreferences(newPrefs: PreferencesUiModel) {
        _preferences.value = newPrefs
        savePreferencesDirectly(newPrefs)
    }

    fun savePreferencesDirectly(prefs: PreferencesUiModel = _preferences.value) {
        try {
            preferencesFile.parentFile?.mkdirs()
            preferencesFile.writeText(json.encodeToString(prefs))
        } catch (_: Exception) {}
    }

    companion object {
        const val PREFERENCES_FILE_NAME = "desktop_preferences.json"
    }
}

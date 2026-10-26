package com.philkes.notallyx.feature.migration.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.philkes.notallyx.core.common.extensions.log
import com.philkes.notallyx.core.preference.NotallyXPreferences
import java.io.IOException
import java.io.InputStream
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "PreferenceBackupExtensions"

fun Context.exportPreferences(preferences: NotallyXPreferences, uri: Uri): Boolean {
    try {
        contentResolver.openOutputStream(uri)?.use {
            it.write(preferences.toJsonString().toByteArray())
        } ?: return false
        return true
    } catch (e: IOException) {
        if (this is ContextWrapper) {
            log(TAG, throwable = e)
        } else {
            Log.e(TAG, "Export preferences failed", e)
        }
        return false
    }
}

fun Context.importPreferences(
    jsonFile: Uri,
    to: SharedPreferences.Editor,
    ignore: Set<String>,
): Boolean {
    try {
        val inputStream: InputStream? = contentResolver.openInputStream(jsonFile)
        val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: return false
        val jsonObject = JSONObject(jsonString)
        for (key in jsonObject.keys()) {
            if (key in ignore) {
                continue
            }
            when (val value = jsonObject.get(key)) {
                is Int -> to.putInt(key, value)
                is Boolean -> to.putBoolean(key, value)
                is Double -> to.putFloat(key, value.toFloat())
                is Long -> to.putLong(key, value)
                is JSONArray -> {
                    val set = (0 until value.length()).map { value.getString(it) }.toSet()
                    to.putStringSet(key, set)
                }

                else -> to.putString(key, value.toString())
            }
        }
        return to.commit()
    } catch (e: Exception) {
        if (this is ContextWrapper) {
            log(TAG, "Import preferences from '$jsonFile' failed", throwable = e)
        } else {
            Log.e(TAG, "Import preferences from '$jsonFile' failed", e)
        }
        return false
    }
}

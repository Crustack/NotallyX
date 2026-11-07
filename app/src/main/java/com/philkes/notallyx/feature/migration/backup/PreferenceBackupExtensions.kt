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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

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
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        for ((key, value) in jsonObject.entries) {
            if (key in ignore || value is JsonNull) {
                continue
            }
            when (value) {
                is JsonArray -> {
                    val set = value.map { it.jsonPrimitive.content }.toSet()
                    to.putStringSet(key, set)
                }
                is JsonPrimitive -> {
                    if (value.isString) {
                        to.putString(key, value.content)
                    } else {
                        value.booleanOrNull?.let { to.putBoolean(key, it) }
                            ?: value.intOrNull?.let { to.putInt(key, it) }
                            ?: value.longOrNull?.let { to.putLong(key, it) }
                            ?: value.doubleOrNull?.let { to.putFloat(key, it.toFloat()) }
                            ?: to.putString(key, value.content)
                    }
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

package com.philkes.notallyx.core.preference.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.core.DataMigration
import com.philkes.notallyx.core.common.extensions.toPreservedByteArray
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import com.philkes.notallyx.core.preference.model.SecurePreferences
import java.io.File

class LegacyEncryptedPrefsMigration(
    private val context: Context,
    private val sharedPrefsProvider: (() -> SharedPreferences)? = null,
) : DataMigration<SecurePreferences> {

    override suspend fun shouldMigrate(currentData: SecurePreferences): Boolean {
        if (sharedPrefsProvider != null) {
            val sp = sharedPrefsProvider.invoke()
            return sp.all.isNotEmpty() &&
                (currentData.backupPassword == PASSWORD_EMPTY ||
                    currentData.backupPassword.isEmpty()) &&
                currentData.fallbackDatabaseEncryptionKey.isEmpty()
        }
        val legacyPrefsFile = getLegacyPrefsFile()
        val plainPrefs = context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE)
        val hasData = (legacyPrefsFile?.exists() == true) || plainPrefs.all.isNotEmpty()
        return hasData &&
            (currentData.backupPassword == PASSWORD_EMPTY ||
                currentData.backupPassword.isEmpty()) &&
            currentData.fallbackDatabaseEncryptionKey.isEmpty()
    }

    override suspend fun migrate(currentData: SecurePreferences): SecurePreferences {
        return try {
            val legacyPrefs =
                sharedPrefsProvider?.invoke()
                    ?: getLegacyEncryptedSharedPreferences(context)
                    ?: context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE)

            val password =
                legacyPrefs.getString("backupPassword", null) ?: currentData.backupPassword
            val fallbackKey =
                legacyPrefs
                    .getString("fallback_database_encryption_key", null)
                    ?.toPreservedByteArray ?: currentData.fallbackDatabaseEncryptionKey

            SecurePreferences(
                backupPassword = password,
                fallbackDatabaseEncryptionKey = fallbackKey,
            )
        } catch (_: Exception) {
            // In case of Keystore invalidation/reset or in test runner, fallback to plain
            // SharedPreferences
            try {
                val plainPrefs =
                    context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE)
                val password =
                    plainPrefs.getString("backupPassword", null) ?: currentData.backupPassword
                val fallbackKey =
                    plainPrefs
                        .getString("fallback_database_encryption_key", null)
                        ?.toPreservedByteArray ?: currentData.fallbackDatabaseEncryptionKey
                SecurePreferences(
                    backupPassword = password,
                    fallbackDatabaseEncryptionKey = fallbackKey,
                )
            } catch (_: Exception) {
                currentData
            }
        }
    }

    override suspend fun cleanUp() {
        try {
            val legacyPrefs = sharedPrefsProvider?.invoke()
            if (legacyPrefs != null) {
                legacyPrefs.edit(commit = true) { clear() }
            } else {
                context.getSharedPreferences("secret_shared_prefs", Context.MODE_PRIVATE).edit(
                    commit = true
                ) {
                    clear()
                }
                getLegacyPrefsFile()?.let { file -> if (file.exists()) file.delete() }
            }
        } catch (_: Exception) {}
    }

    private fun getLegacyPrefsFile(): File? {
        val dataDir = context.applicationInfo?.dataDir ?: context.filesDir?.parent
        return dataDir?.let { File(it, "shared_prefs/secret_shared_prefs.xml") }
    }

    private fun getLegacyEncryptedSharedPreferences(context: Context): SharedPreferences? {
        return try {
            val masterKeyBuilderClass = Class.forName("androidx.security.crypto.MasterKey\$Builder")
            val keySchemeClass = Class.forName("androidx.security.crypto.MasterKey\$KeyScheme")
            val aes256Gcm = keySchemeClass.getField("AES256_GCM").get(null)
            val builder =
                masterKeyBuilderClass.getConstructor(Context::class.java).newInstance(context)
            masterKeyBuilderClass
                .getMethod("setKeyScheme", keySchemeClass)
                .invoke(builder, aes256Gcm)
            val masterKey = masterKeyBuilderClass.getMethod("build").invoke(builder)

            val masterKeyClass = Class.forName("androidx.security.crypto.MasterKey")
            val espClass = Class.forName("androidx.security.crypto.EncryptedSharedPreferences")
            val keyEncSchemeClass =
                Class.forName(
                    "androidx.security.crypto.EncryptedSharedPreferences\$PrefKeyEncryptionScheme"
                )
            val valEncSchemeClass =
                Class.forName(
                    "androidx.security.crypto.EncryptedSharedPreferences\$PrefValueEncryptionScheme"
                )

            val keyScheme = keyEncSchemeClass.getField("AES256_SIV").get(null)
            val valScheme = valEncSchemeClass.getField("AES256_GCM").get(null)

            val createMethod =
                espClass.getMethod(
                    "create",
                    Context::class.java,
                    String::class.java,
                    masterKeyClass,
                    keyEncSchemeClass,
                    valEncSchemeClass,
                )
            createMethod.invoke(
                null,
                context,
                "secret_shared_prefs",
                masterKey,
                keyScheme,
                valScheme,
            ) as? SharedPreferences
        } catch (_: Throwable) {
            null
        }
    }
}

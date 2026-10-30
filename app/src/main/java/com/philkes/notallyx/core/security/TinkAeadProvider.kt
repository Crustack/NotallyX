package com.philkes.notallyx.core.security

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.PredefinedAeadParameters
import com.google.crypto.tink.integration.android.AndroidKeysetManager

object TinkAeadProvider {
    const val KEYSET_NAME = "notallyx_keyset"
    const val PREFS_FILE = "notallyx_keyset_prefs"
    const val MASTER_KEY_URI = "android-keystore://notallyx_master_key"

    fun provideAead(context: Context, masterKeyUri: String? = MASTER_KEY_URI): Aead {
        AeadConfig.register()
        return try {
            val builder =
                AndroidKeysetManager.Builder()
                    .withSharedPref(context, KEYSET_NAME, PREFS_FILE)
                    .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
            if (masterKeyUri != null) {
                builder.withMasterKeyUri(masterKeyUri)
            }
            builder.build().keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        } catch (_: Exception) {
            // Fallback for tests or environments where Android Keystore master key is unavailable
            try {
                AndroidKeysetManager.Builder()
                    .withSharedPref(context, KEYSET_NAME, PREFS_FILE)
                    .withKeyTemplate(KeyTemplate.createFrom(PredefinedAeadParameters.AES256_GCM))
                    .build()
                    .keysetHandle
                    .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
            } catch (_: Exception) {
                // Fallback for LayoutLib / Compose preview environments where
                // SharedPrefKeysetWriter fails
                createInMemoryAead()
            }
        }
    }

    fun createInMemoryAead(): Aead {
        AeadConfig.register()
        return KeysetHandle.generateNew(PredefinedAeadParameters.AES256_GCM)
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }
}

package com.philkes.notallyx.core.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.FileStorage
import androidx.datastore.tink.AeadSerializer
import com.philkes.notallyx.core.platform.PathProvider
import com.philkes.notallyx.core.preference.migration.LegacyEncryptedPrefsMigration
import com.philkes.notallyx.core.preference.model.SecurePreferences
import com.philkes.notallyx.core.preference.model.SecurePreferencesCorruptionHandler
import com.philkes.notallyx.core.preference.model.SecurePreferencesSerializer
import com.philkes.notallyx.core.security.TinkAeadProvider
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AndroidSecurePreferencesStore(
    private val context: Context,
    private val pathProvider: PathProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SecurePreferencesStore {
    override val dataStore: DataStore<SecurePreferences> by lazy {
        val aead = TinkAeadProvider.provideAead(context)
        DataStoreFactory.create(
            storage =
                FileStorage(
                    serializer =
                        AeadSerializer(
                            aead = aead,
                            wrappedSerializer = SecurePreferencesSerializer,
                            associatedData = SECURE_PREFERENCES_FILE_NAME.encodeToByteArray(),
                        ),
                    produceFile = { pathProvider.getDataStoreFile(SECURE_PREFERENCES_FILE_NAME) },
                ),
            migrations = listOf(LegacyEncryptedPrefsMigration(context)),
            corruptionHandler = SecurePreferencesCorruptionHandler,
            scope = scope,
        )
    }
}

class JvmSecurePreferencesStore(
    private val pathProvider: PathProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : SecurePreferencesStore {
    override val dataStore: DataStore<SecurePreferences> by lazy {
        val keysetFile = File(pathProvider.getPreferencesDirectory(), "notallyx_keyset.json")
        val aead = TinkAeadProvider.provideDesktopAead(keysetFile)
        DataStoreFactory.create(
            storage =
                FileStorage(
                    serializer =
                        AeadSerializer(
                            aead = aead,
                            wrappedSerializer = SecurePreferencesSerializer,
                            associatedData = SECURE_PREFERENCES_FILE_NAME.encodeToByteArray(),
                        ),
                    produceFile = { pathProvider.getDataStoreFile(SECURE_PREFERENCES_FILE_NAME) },
                ),
            corruptionHandler = SecurePreferencesCorruptionHandler,
            scope = scope,
        )
    }
}

package com.philkes.notallyx.core.preference.model

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.philkes.notallyx.core.preference.Constants.PASSWORD_EMPTY
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
data class SecurePreferences(
    val backupPassword: String = PASSWORD_EMPTY,
    val fallbackDatabaseEncryptionKey: ByteArray = ByteArray(0),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecurePreferences

        if (backupPassword != other.backupPassword) return false
        if (!fallbackDatabaseEncryptionKey.contentEquals(other.fallbackDatabaseEncryptionKey))
            return false

        return true
    }

    override fun hashCode(): Int {
        var result = backupPassword.hashCode()
        result = 31 * result + fallbackDatabaseEncryptionKey.contentHashCode()
        return result
    }
}

object SecurePreferencesSerializer : Serializer<SecurePreferences> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    override val defaultValue: SecurePreferences = SecurePreferences()

    override suspend fun readFrom(input: InputStream): SecurePreferences {
        return try {
            json.decodeFromString(
                SecurePreferences.serializer(),
                input.readBytes().decodeToString(),
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to deserialize SecurePreferences", e)
        }
    }

    override suspend fun writeTo(t: SecurePreferences, output: OutputStream) {
        output.write(json.encodeToString(SecurePreferences.serializer(), t).encodeToByteArray())
    }
}

val SecurePreferencesCorruptionHandler = ReplaceFileCorruptionHandler { SecurePreferences() }

package com.philkes.notallyx.feature.migration.backup

import android.app.Application
import android.content.Context
import android.content.Intent
import com.philkes.notallyx.core.common.extensions.copyToLarge
import com.philkes.notallyx.core.common.extensions.getExternalBackupsDirectory
import com.philkes.notallyx.core.common.extensions.migrateAllAttachments
import com.philkes.notallyx.core.database.NotallyDatabase
import com.philkes.notallyx.core.database.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.core.preference.BiometricLock
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.decryptDatabase
import com.philkes.notallyx.core.security.encryptDatabase
import com.philkes.notallyx.core.security.exceptions.DecryptionException
import com.philkes.notallyx.core.security.exceptions.EncryptionException
import com.philkes.notallyx.core.security.isEncryptedDatabase
import com.philkes.notallyx.core.security.isUnencryptedDatabase
import java.io.File
import java.util.Date
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseTransitionManager {
    private const val EXTRA_ACTION = "notallyx.intent.extra.DATABASE_ACTION"
    private const val EXTRA_ENABLE_BIOMETRIC_LOCK = "notallyx.intent.extra.ENABLE_BIOMETRIC_LOCK"
    private const val EXTRA_ENCRYPT_DATABASE = "notallyx.intent.extra.ENCRYPT_DATABASE"
    var pendingCipher: Cipher? = null

    fun start(
        context: Context,
        enableBiometricLock: Boolean,
        encryptDatabase: Boolean,
        cipher: Cipher? = null,
    ) {
        pendingCipher = cipher
        val action =
            if (enableBiometricLock) DatabaseAction.ENABLE_BIOMETRIC_LOCK
            else DatabaseAction.DISABLE_BIOMETRIC_LOCK
        val intent =
            Intent()
                .setClassName(
                    context.packageName,
                    "com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity",
                )
                .apply {
                    putExtra(EXTRA_ACTION, action.name)
                    putExtra(EXTRA_ENABLE_BIOMETRIC_LOCK, enableBiometricLock)
                    putExtra(EXTRA_ENCRYPT_DATABASE, encryptDatabase)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
        context.startActivity(intent)
    }

    fun start(context: Context, action: DatabaseAction, cipher: Cipher? = null) {
        pendingCipher = cipher
        val intent =
            Intent()
                .setClassName(
                    context.packageName,
                    "com.philkes.notallyx.feature.migration.backup.DatabaseTransitionActivity",
                )
                .apply {
                    putExtra(EXTRA_ACTION, action.name)
                    if (action == DatabaseAction.ENABLE_BIOMETRIC_LOCK) {
                        putExtra(EXTRA_ENABLE_BIOMETRIC_LOCK, true)
                        putExtra(EXTRA_ENCRYPT_DATABASE, true)
                    } else if (action == DatabaseAction.DISABLE_BIOMETRIC_LOCK) {
                        putExtra(EXTRA_ENABLE_BIOMETRIC_LOCK, false)
                        putExtra(EXTRA_ENCRYPT_DATABASE, false)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
        context.startActivity(intent)
    }

    suspend fun enableDataInPublic(
        app: Application,
        preferences: NotallyXPreferences,
        callback: (() -> Unit)? = null,
    ) {
        val database = NotallyDatabase.getDatabase(app)
        withContext(Dispatchers.IO) {
            NotallyDatabase.startReplacement()
            try {
                database.value!!.checkpoint()
                NotallyDatabase.clearInstance()
                val targetDirectory = NotallyDatabase.getExternalDatabaseFile(app).parentFile
                NotallyDatabase.getExternalDatabaseFiles(app).forEach { it.delete() }
                val internalDatabaseFiles = NotallyDatabase.getInternalDatabaseFiles(app)
                internalDatabaseFiles.forEach {
                    it.copyToLarge(File(targetDirectory, it.name), overwrite = true)
                }
                val notallyDatabase =
                    NotallyDatabase.getFreshDatabase(
                        app,
                        true,
                        preferences.biometricLock.value,
                    )
                val ping =
                    try {
                        notallyDatabase.ping()
                    } catch (e: Exception) {
                        throw RuntimeException(
                            "Moving internal '${internalDatabaseFiles.map { it.name }}' to public '$targetDirectory' folder failed",
                            e,
                        )
                    }
                if (!ping) {
                    throw RuntimeException(
                        "Moving internal '${internalDatabaseFiles.map { it.name }}' to public '$targetDirectory' folder failed"
                    )
                }
                app.migrateAllAttachments(toPrivate = false)
            } catch (e: Exception) {
                NotallyDatabase.postNewInstance(app, dataInPublic = false)
                throw e
            }
        }
        NotallyDatabase.postNewInstance(app, dataInPublic = true)
        preferences.dataInPublicFolder.save(true)
        callback?.invoke()
    }

    suspend fun disableDataInPublic(
        app: Application,
        preferences: NotallyXPreferences,
        callback: (() -> Unit)? = null,
    ) {
        val database = NotallyDatabase.getDatabase(app)
        withContext(Dispatchers.IO) {
            NotallyDatabase.startReplacement()
            try {
                database.value!!.checkpoint()
                NotallyDatabase.clearInstance()
                val targetDirectory = NotallyDatabase.getInternalDatabaseFile(app).parentFile
                NotallyDatabase.getInternalDatabaseFiles(app).forEach { it.delete() }
                val externalDatabaseFiles = NotallyDatabase.getExternalDatabaseFiles(app)
                externalDatabaseFiles.forEach {
                    it.copyToLarge(File(targetDirectory, it.name), overwrite = true)
                }
                val notallyDatabase =
                    NotallyDatabase.getFreshDatabase(
                        app,
                        false,
                        preferences.biometricLock.value,
                    )
                val ping =
                    try {
                        notallyDatabase.ping()
                    } catch (e: Exception) {
                        throw RuntimeException(
                            "Moving public '${externalDatabaseFiles.map { it.name }}' to internal '$targetDirectory' folder failed",
                            e,
                        )
                    }
                if (!ping) {
                    throw RuntimeException(
                        "Moving public '${externalDatabaseFiles.map { it.name }}' to internal '$targetDirectory' folder failed"
                    )
                }
                app.migrateAllAttachments(toPrivate = true)
            } catch (e: Exception) {
                NotallyDatabase.postNewInstance(app, dataInPublic = true)
                throw e
            }
        }
        NotallyDatabase.postNewInstance(app, dataInPublic = false)
        preferences.dataInPublicFolder.save(false)
        callback?.invoke()
    }

    suspend fun enableBiometricsEncryption(
        app: Application,
        preferences: NotallyXPreferences,
        cipher: Cipher,
    ) {
        val passphrase = preferences.databaseEncryptionKey.init(cipher)
        withContext(Dispatchers.IO) {
            preferences.iv.save(cipher.iv)
            NotallyDatabase.startReplacement()
            try {
                val (_, dbFileCopy) = app.copyDatabase(suffix = "-encrypt")
                val (_, dbFileBackup) = app.copyDatabase(suffix = "-encrypt-backup")
                NotallyDatabase.clearInstance()
                encryptDatabase(app, dbFileCopy, passphrase)
                val originalDbFiles = NotallyDatabase.getCurrentDatabaseFiles(app)
                originalDbFiles.forEach { it.delete() }
                val originalDbFile = NotallyDatabase.getCurrentDatabaseFile(app)
                dbFileCopy.copyToLarge(
                    originalDbFile,
                    overwrite = true,
                    deleteSourceFile = true,
                )
                if (originalDbFile.isUnencryptedDatabase(app)) {
                    dbFileBackup.copyToLarge(originalDbFile, overwrite = true)
                    val externalBackupFile =
                        File(
                            app.getExternalBackupsDirectory(),
                            "${DATABASE_NAME}_Backup_before_encryption_${FILE_TIMESTAMP_FORMAT.format(
                                Date()
                            )}",
                        )
                    dbFileBackup.copyToLarge(
                        externalBackupFile,
                        overwrite = true,
                        deleteSourceFile = true,
                    )
                    throw EncryptionException(
                        "Encrypt succeeded but overwritten database is not encrypted, restored unencrypted database and created additional backup at ${externalBackupFile.absolutePath}"
                    )
                }
            } catch (e: Exception) {
                NotallyDatabase.postNewInstance(app, biometricLock = BiometricLock.DISABLED)
                throw e
            }
        }
        NotallyDatabase.postNewInstance(app, biometricLock = BiometricLock.ENABLED)
        preferences.fallbackDatabaseEncryptionKey.save(passphrase)
        preferences.biometricLock.save(BiometricLock.ENABLED)
        preferences.biometricLockEncryptsDb.save(true)
    }

    suspend fun disableBiometricsEncryption(
        app: Application,
        preferences: NotallyXPreferences,
        cipher: Cipher? = null,
        keepBiometricLockEnabled: Boolean = false,
        callback: (() -> Unit)? = null,
    ) {
        val targetLockState =
            if (keepBiometricLockEnabled) BiometricLock.ENABLED else BiometricLock.DISABLED
        if (!preferences.biometricLockEncryptsDb.value) {
            NotallyDatabase.postNewInstance(app, biometricLock = targetLockState)
            preferences.biometricLock.save(targetLockState)
            preferences.biometricLockEncryptsDb.save(false)
            callback?.invoke()
            return
        }
        val encryptedPassphrase = preferences.databaseEncryptionKey.value
        val passphrase =
            cipher?.doFinal(encryptedPassphrase)
                ?: preferences.fallbackDatabaseEncryptionKey.value!!
        withContext(Dispatchers.IO) {
            NotallyDatabase.startReplacement()
            try {
                val (_, dbFileCopy) = app.copyDatabase(decrypt = false, suffix = "-decrypt")
                val (_, dbFileBackup) =
                    app.copyDatabase(decrypt = false, suffix = "-decrypt-backup")
                NotallyDatabase.clearInstance()
                decryptDatabase(app, dbFileCopy, passphrase)
                val originalDbFiles = NotallyDatabase.getCurrentDatabaseFiles(app)
                originalDbFiles.forEach { it.delete() }
                val originalDbFile = NotallyDatabase.getCurrentDatabaseFile(app)
                dbFileCopy.copyToLarge(
                    originalDbFile,
                    overwrite = true,
                    deleteSourceFile = true,
                )
                if (originalDbFile.isEncryptedDatabase(app)) {
                    dbFileBackup.copyToLarge(originalDbFile, overwrite = true)
                    val externalBackupFile =
                        File(
                            app.getExternalBackupsDirectory(),
                            "${DATABASE_NAME}_Backup_before_decryption_${FILE_TIMESTAMP_FORMAT.format(
                                Date()
                            )}",
                        )
                    dbFileBackup.copyToLarge(
                        externalBackupFile,
                        overwrite = true,
                        deleteSourceFile = true,
                    )
                    throw DecryptionException(
                        "Decrypt succeeded but overwritten database is still encrypted, restored encrypted database and created additional backup at ${externalBackupFile.absolutePath}"
                    )
                }
            } catch (e: Exception) {
                NotallyDatabase.postNewInstance(app, biometricLock = BiometricLock.ENABLED)
                throw e
            }
        }
        NotallyDatabase.postNewInstance(app, biometricLock = targetLockState)
        preferences.biometricLock.save(targetLockState)
        preferences.biometricLockEncryptsDb.save(false)
        callback?.invoke()
    }
}

package com.philkes.notallyx.presentation.activity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.DynamicColors
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.data.NotallyDatabase
import com.philkes.notallyx.data.NotallyDatabase.Companion.DATABASE_NAME
import com.philkes.notallyx.databinding.ActivityDatabaseTransitionBinding
import com.philkes.notallyx.presentation.activity.main.MainActivity
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.viewmodel.preference.BiometricLock
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import com.philkes.notallyx.utils.backup.FILE_TIMESTAMP_FORMAT
import com.philkes.notallyx.utils.backup.copyDatabase
import com.philkes.notallyx.utils.copyToLarge
import com.philkes.notallyx.utils.getExternalBackupsDirectory
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.migrateAllAttachments
import com.philkes.notallyx.utils.security.DecryptionException
import com.philkes.notallyx.utils.security.EncryptionException
import com.philkes.notallyx.utils.security.decryptDatabase
import com.philkes.notallyx.utils.security.encryptDatabase
import com.philkes.notallyx.utils.security.isEncryptedDatabase
import com.philkes.notallyx.utils.security.isUnencryptedDatabase
import com.philkes.notallyx.utils.showErrorDialog
import java.io.File
import java.util.Date
import javax.crypto.Cipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class DatabaseTransitionActivity : AppCompatActivity() {

    private val preferences: NotallyXPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notallyXApplication = application as NotallyXApplication
        if (preferences.useDynamicColors.value) {
            if (DynamicColors.isDynamicColorAvailable()) {
                DynamicColors.applyToActivitiesIfAvailable(notallyXApplication)
            }
        } else {
            when (preferences.theme.value) {
                Theme.SUPER_DARK -> theme.applyStyle(R.style.AppTheme_SuperDark, true)
                else -> theme.applyStyle(R.style.AppTheme, true)
            }
        }

        val binding = ActivityDatabaseTransitionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState != null) return

        val actionName = intent.getStringExtra(EXTRA_ACTION)
        val action =
            actionName?.let { DatabaseAction.valueOf(it) } ?: DatabaseAction.ENABLE_DATA_IN_PUBLIC

        val enableBiometricLock =
            if (intent.hasExtra(EXTRA_ENABLE_BIOMETRIC_LOCK)) {
                intent.getBooleanExtra(EXTRA_ENABLE_BIOMETRIC_LOCK, false)
            } else {
                action == DatabaseAction.ENABLE_BIOMETRIC_LOCK
            }

        val encryptDatabase =
            if (intent.hasExtra(EXTRA_ENCRYPT_DATABASE)) {
                intent.getBooleanExtra(EXTRA_ENCRYPT_DATABASE, false)
            } else {
                action == DatabaseAction.ENABLE_BIOMETRIC_LOCK
            }

        val cipher = pendingCipher
        pendingCipher = null

        val messageResId =
            when (action) {
                DatabaseAction.ENABLE_DATA_IN_PUBLIC -> R.string.moving_database_to_external
                DatabaseAction.DISABLE_DATA_IN_PUBLIC -> R.string.moving_database_to_internal
                DatabaseAction.ENABLE_BIOMETRIC_LOCK,
                DatabaseAction.DISABLE_BIOMETRIC_LOCK -> {
                    if (encryptDatabase) R.string.encrypting_database_biometric
                    else R.string.decrypting_database_biometric
                }
            }
        binding.TransitionText.setText(messageResId)

        lifecycleScope.launch {
            try {
                when (action) {
                    DatabaseAction.ENABLE_DATA_IN_PUBLIC ->
                        enableDataInPublic(notallyXApplication, preferences)
                    DatabaseAction.DISABLE_DATA_IN_PUBLIC ->
                        disableDataInPublic(notallyXApplication, preferences)
                    DatabaseAction.ENABLE_BIOMETRIC_LOCK,
                    DatabaseAction.DISABLE_BIOMETRIC_LOCK -> {
                        if (encryptDatabase) {
                            requireNotNull(cipher) { "Missing cipher for biometric encryption" }
                            enableBiometricsEncryption(notallyXApplication, preferences, cipher)
                        } else {
                            if (preferences.biometricLockEncryptsDb.value) {
                                disableBiometricsEncryption(
                                    notallyXApplication,
                                    preferences,
                                    cipher,
                                    keepBiometricLockEnabled = enableBiometricLock,
                                )
                            } else {
                                preferences.biometricLockEncryptsDb.save(false)
                                preferences.biometricLock.save(
                                    if (enableBiometricLock) BiometricLock.ENABLED
                                    else BiometricLock.DISABLED
                                )
                            }
                        }
                        notallyXApplication.locked.value = false
                        showToast(
                            if (enableBiometricLock && !encryptDatabase)
                                R.string.biometrics_decrypted_success
                            else if (enableBiometricLock) R.string.biometrics_setup_success
                            else R.string.biometrics_disable_success
                        )
                    }
                }
                launchMainActivity()
            } catch (e: Exception) {
                notallyXApplication.log(TAG, throwable = e)
                when (e) {
                    is EncryptionException -> {
                        val dialog =
                            showErrorDialog(
                                e,
                                R.string.biometrics_setup_failure,
                                getString(
                                    R.string.biometrics_setup_failure_encrypt,
                                    getString(R.string.report_bug),
                                ),
                            )
                        dialog?.setOnDismissListener { launchMainActivity() }
                    }
                    is DecryptionException -> {
                        val dialog =
                            showErrorDialog(
                                e,
                                R.string.biometrics_setup_failure,
                                getString(
                                    R.string.biometrics_setup_failure_decrypt,
                                    getString(R.string.report_bug),
                                ),
                            )
                        dialog?.setOnDismissListener { launchMainActivity() }
                    }
                    else -> {
                        val dialog =
                            showErrorDialog(
                                e,
                                R.string.error,
                                e.message ?: "Database migration failed",
                            )
                        dialog?.setOnDismissListener { launchMainActivity() }
                    }
                }
            }
        }
    }

    private fun launchMainActivity() {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_FRAGMENT_TO_OPEN, R.id.Settings)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val TAG = "DatabaseTransitionActivity"
        const val EXTRA_ACTION = "notallyx.intent.extra.DATABASE_ACTION"
        const val EXTRA_ENABLE_BIOMETRIC_LOCK = "notallyx.intent.extra.ENABLE_BIOMETRIC_LOCK"
        const val EXTRA_ENCRYPT_DATABASE = "notallyx.intent.extra.ENCRYPT_DATABASE"
        private var pendingCipher: Cipher? = null

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
                Intent(context, DatabaseTransitionActivity::class.java).apply {
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
                Intent(context, DatabaseTransitionActivity::class.java).apply {
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
            val database =
                withContext(Dispatchers.Main.immediate) { NotallyDatabase.getDatabase(app) }
            withContext(Dispatchers.IO) {
                NotallyDatabase.startReplacement()
                try {
                    database.value!!.checkpoint()
                    withContext(Dispatchers.Main.immediate) { NotallyDatabase.clearInstance() }
                    val targetDirectory = NotallyDatabase.getExternalDatabaseFile(app).parentFile
                    NotallyDatabase.getExternalDatabaseFiles(app).forEach { it.delete() }
                    val internalDatabaseFiles = NotallyDatabase.getInternalDatabaseFiles(app)
                    internalDatabaseFiles.forEach {
                        it.copyToLarge(File(targetDirectory, it.name), overwrite = true)
                    }
                    val notallyDatabase =
                        withContext(Dispatchers.Main.immediate) {
                            NotallyDatabase.getFreshDatabase(
                                app,
                                true,
                                preferences.biometricLock.value,
                            )
                        }
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
                    withContext(Dispatchers.Main.immediate) {
                        NotallyDatabase.postNewInstance(app, dataInPublic = false)
                    }
                    throw e
                }
            }
            withContext(Dispatchers.Main.immediate) {
                NotallyDatabase.postNewInstance(app, dataInPublic = true)
                preferences.dataInPublicFolder.save(true)
            }
            callback?.invoke()
        }

        suspend fun disableDataInPublic(
            app: Application,
            preferences: NotallyXPreferences,
            callback: (() -> Unit)? = null,
        ) {
            val database =
                withContext(Dispatchers.Main.immediate) { NotallyDatabase.getDatabase(app) }
            withContext(Dispatchers.IO) {
                NotallyDatabase.startReplacement()
                try {
                    database.value!!.checkpoint()
                    withContext(Dispatchers.Main.immediate) { NotallyDatabase.clearInstance() }
                    val targetDirectory = NotallyDatabase.getInternalDatabaseFile(app).parentFile
                    NotallyDatabase.getInternalDatabaseFiles(app).forEach { it.delete() }
                    val externalDatabaseFiles = NotallyDatabase.getExternalDatabaseFiles(app)
                    externalDatabaseFiles.forEach {
                        it.copyToLarge(File(targetDirectory, it.name), overwrite = true)
                    }
                    val notallyDatabase =
                        withContext(Dispatchers.Main.immediate) {
                            NotallyDatabase.getFreshDatabase(
                                app,
                                false,
                                preferences.biometricLock.value,
                            )
                        }
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
                    withContext(Dispatchers.Main.immediate) {
                        NotallyDatabase.postNewInstance(app, dataInPublic = true)
                    }
                    throw e
                }
            }
            withContext(Dispatchers.Main.immediate) {
                NotallyDatabase.postNewInstance(app, dataInPublic = false)
                preferences.dataInPublicFolder.save(false)
            }
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
                    withContext(Dispatchers.Main.immediate) { NotallyDatabase.clearInstance() }
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
                    withContext(Dispatchers.Main.immediate) {
                        NotallyDatabase.postNewInstance(app, biometricLock = BiometricLock.DISABLED)
                    }
                    throw e
                }
            }
            withContext(Dispatchers.Main.immediate) {
                NotallyDatabase.postNewInstance(app, biometricLock = BiometricLock.ENABLED)
                preferences.fallbackDatabaseEncryptionKey.save(passphrase)
                preferences.biometricLock.save(BiometricLock.ENABLED)
                preferences.biometricLockEncryptsDb.save(true)
            }
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
                withContext(Dispatchers.Main.immediate) {
                    NotallyDatabase.postNewInstance(app, biometricLock = targetLockState)
                    preferences.biometricLock.save(targetLockState)
                    preferences.biometricLockEncryptsDb.save(false)
                }
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
                    withContext(Dispatchers.Main.immediate) { NotallyDatabase.clearInstance() }
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
                    withContext(Dispatchers.Main.immediate) {
                        NotallyDatabase.postNewInstance(app, biometricLock = BiometricLock.ENABLED)
                    }
                    throw e
                }
            }
            withContext(Dispatchers.Main.immediate) {
                NotallyDatabase.postNewInstance(app, biometricLock = targetLockState)
                preferences.biometricLock.save(targetLockState)
                preferences.biometricLockEncryptsDb.save(false)
            }
            callback?.invoke()
        }
    }
}

enum class DatabaseAction {
    ENABLE_DATA_IN_PUBLIC,
    DISABLE_DATA_IN_PUBLIC,
    ENABLE_BIOMETRIC_LOCK,
    DISABLE_BIOMETRIC_LOCK,
}

package com.philkes.notallyx.presentation.activity

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.database.sqlite.SQLiteBlobTooBigException
import android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_HW_NOT_PRESENT
import android.hardware.biometrics.BiometricPrompt.BIOMETRIC_ERROR_NO_BIOMETRICS
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.setupProgressDialog
import com.philkes.notallyx.presentation.showToast
import com.philkes.notallyx.presentation.viewmodel.BaseNoteModel
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import com.philkes.notallyx.presentation.viewmodel.progress.MigrationProgress
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.secondsBetween
import com.philkes.notallyx.utils.security.showBiometricOrPinPrompt
import com.philkes.notallyx.utils.splitOversizedNotes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

abstract class LockedActivity<T : ViewBinding> : AppCompatActivity() {

    private lateinit var notallyXApplication: NotallyXApplication
    private lateinit var biometricAuthenticationActivityResultLauncher:
        ActivityResultLauncher<Intent>

    protected lateinit var binding: T
    protected lateinit var preferences: NotallyXPreferences
    val baseModel: BaseNoteModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupGlobalExceptionHandler()
        initViewModel()
        notallyXApplication = (application as NotallyXApplication)
        preferences = NotallyXPreferences.getInstance(notallyXApplication)
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
        biometricAuthenticationActivityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    unlock()
                } else {
                    finish()
                }
            }
    }

    open fun initViewModel() {
        baseModel.startObserving()
    }

    private fun setupGlobalExceptionHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            if (
                throwable is SQLiteBlobTooBigException ||
                    throwable.cause is SQLiteBlobTooBigException
            ) {
                lifecycleScope.launch {
                    EXCEPTION_HANDLER_MUTEX.withLock {
                        val time = System.currentTimeMillis()
                        if (!isExceptionAlreadyBeingHandled(time)) {
                            EXCEPTION_HANDLER_MUTEX_LAST_TIMESTAMP = time
                            val migrationProgress =
                                MutableLiveData<MigrationProgress>().apply {
                                    setupProgressDialog(this@LockedActivity)
                                    postValue(
                                        MigrationProgress(
                                            R.string.migration_splitting_notes,
                                            indeterminate = true,
                                        )
                                    )
                                }
                            log(
                                TAG,
                                msg =
                                    "SQLiteBlobTooBigException occurred, trying to fix broken notes...",
                            )
                            withContext(Dispatchers.IO) { application.splitOversizedNotes() }
                            migrationProgress.postValue(
                                MigrationProgress(R.string.migrating_data, inProgress = false)
                            )
                        }
                    }
                }
            } else {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun isExceptionAlreadyBeingHandled(time: Long): Boolean =
        EXCEPTION_HANDLER_MUTEX_LAST_TIMESTAMP?.let { it.secondsBetween(time) < 20 } ?: false

    override fun onResume() {
        if (preferences.isLockEnabled) {
            if (hasToAuthenticateWithBiometric()) {
                hide()
                showLockScreen()
            } else {
                show()
            }
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (preferences.isLockEnabled && notallyXApplication.locked.value) {
            hide()
        }
    }

    open fun showLockScreen() {
        showBiometricOrPinPrompt(
            true,
            preferences.iv.value!!,
            biometricAuthenticationActivityResultLauncher,
            R.string.unlock,
            onSuccess = { unlock() },
        ) { errorCode ->
            when (errorCode) {
                BIOMETRIC_ERROR_NO_BIOMETRICS -> {
                    MaterialAlertDialogBuilder(this)
                        .setMessage(R.string.unlock_with_biometrics_not_setup)
                        .setPositiveButton(R.string.disable) { _, _ ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                lifecycleScope.launch {
                                    baseModel.disableBiometricLock()
                                    showToast(R.string.biometrics_disable_success)
                                }
                            }
                            hide()
                        }
                        .setNegativeButton(R.string.tap_to_set_up) { _, _ ->
                            val intent =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    Intent(Settings.ACTION_FINGERPRINT_ENROLL)
                                } else {
                                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                                }
                            startActivity(intent)
                        }
                        .show()
                }

                BIOMETRIC_ERROR_HW_NOT_PRESENT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        lifecycleScope.launch {
                            baseModel.disableBiometricLock()
                            showToast(R.string.biometrics_disable_success)
                        }
                    }
                    show()
                }

                else -> finish()
            }
        }
    }

    private fun unlock() {
        notallyXApplication.locked.value = false
        show()
    }

    protected fun show() {
        binding.root.visibility = VISIBLE
    }

    protected fun hide() {
        binding.root.visibility = INVISIBLE
    }

    private fun hasToAuthenticateWithBiometric(): Boolean {
        return ContextCompat.getSystemService(this, KeyguardManager::class.java)?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                (it.isDeviceLocked || notallyXApplication.locked.value)
            } else {
                false
            }
        } ?: false
    }

    companion object {
        private const val TAG = "LockedActivity"
        private val EXCEPTION_HANDLER_MUTEX = Mutex()
        private var EXCEPTION_HANDLER_MUTEX_LAST_TIMESTAMP: Long? = null
    }
}

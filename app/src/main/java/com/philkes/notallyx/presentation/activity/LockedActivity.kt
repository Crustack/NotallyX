package com.philkes.notallyx.presentation.activity

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.database.sqlite.SQLiteBlobTooBigException
import android.os.Build
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.color.DynamicColors
import com.philkes.notallyx.NotallyXApplication
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.setupProgressDialog
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import com.philkes.notallyx.presentation.viewmodel.preference.Theme
import com.philkes.notallyx.presentation.viewmodel.progress.MigrationProgress
import com.philkes.notallyx.utils.log
import com.philkes.notallyx.utils.secondsBetween
import com.philkes.notallyx.utils.security.AuthenticatorProvider
import com.philkes.notallyx.utils.splitOversizedNotes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

abstract class LockedActivity<T : ViewBinding> : AppCompatActivity() {

    private lateinit var notallyXApplication: NotallyXApplication
    private lateinit var biometricAuthenticationActivityResultLauncher:
        ActivityResultLauncher<Intent>

    val migrationProgress: StateFlow<MigrationProgress?>
        field = MutableStateFlow<MigrationProgress?>(null)

    protected fun setMigrationProgress(progress: MigrationProgress?) {
        migrationProgress.value = progress
    }

    internal lateinit var binding: T
    internal val preferences: NotallyXPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupGlobalExceptionHandler()
        initViewModel()
        notallyXApplication = (application as NotallyXApplication)
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

    open fun initViewModel() {}

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
                            migrationProgress.setupProgressDialog(this@LockedActivity)
                            setMigrationProgress(
                                MigrationProgress(
                                    R.string.migration_splitting_notes,
                                    indeterminate = true,
                                )
                            )
                            log(
                                TAG,
                                msg =
                                    "SQLiteBlobTooBigException occurred, trying to fix broken notes...",
                            )
                            withContext(Dispatchers.IO) { application.splitOversizedNotes() }
                            setMigrationProgress(
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
        AuthenticatorProvider.instance.authenticate(
            this,
            preferences.iv.value!!,
            isForDecrypt = true,
            onSuccess = { unlock() },
            onError = { finish() },
        )
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

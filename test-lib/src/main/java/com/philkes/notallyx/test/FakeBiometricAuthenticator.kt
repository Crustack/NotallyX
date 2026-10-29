package com.philkes.notallyx.test

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.BiometricAuthenticator
import com.philkes.notallyx.core.security.getCipher
import com.philkes.notallyx.core.security.getOrCreateSecretKey
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FakeBiometricAuthenticator : BiometricAuthenticator, KoinComponent {
    var cipher: Cipher? = null
    var shouldSucceed: Boolean = true
    var iv: ByteArray? = null
    var deferAuthentication: Boolean = false
    private var pendingSuccess: (() -> Unit)? = null
    private var pendingError: (() -> Unit)? = null

    val hasPendingAuthentication: Boolean
        get() = pendingSuccess != null || pendingError != null

    fun completeAuthentication() {
        val action = pendingSuccess
        pendingSuccess = null
        pendingError = null
        if (action != null) {
            Handler(Looper.getMainLooper()).post { action.invoke() }
        }
    }

    fun failAuthentication() {
        val action = pendingError
        pendingSuccess = null
        pendingError = null
        if (action != null) {
            Handler(Looper.getMainLooper()).post { action.invoke() }
        }
    }

    private var encryptCounter = 0
    private var decryptCounter = 0
    private val secretKey: SecretKey by lazy { getOrCreateSecretKey() }
    private val preferences: NotallyXPreferences by inject()

    override fun authenticate(
        activity: FragmentActivity,
        cipherIv: ByteArray?,
        isForDecrypt: Boolean,
        onSuccess: (Cipher?) -> Unit,
        onError: (String) -> Unit,
    ) {
        val executeSuccess = {
            if (iv != null) {
                if (isForDecrypt) {
                    if (preferences.biometricLockEncryptsDb.value && cipherIv != null) {
                        val c = getCipher()
                        decryptCounter++
                        c.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(cipherIv))
                        onSuccess(c)
                    } else {
                        onSuccess(cipher)
                    }
                } else {
                    val c = getCipher()
                    encryptCounter++
                    c.init(Cipher.ENCRYPT_MODE, secretKey)
                    onSuccess(cipher ?: c)
                }
            } else if (cipher != null) {
                onSuccess(cipher)
            } else {
                onError("Authentication failed")
            }
        }
        val executeError = { onError("Authentication failed") }

        if (deferAuthentication) {
            pendingSuccess = executeSuccess
            pendingError = executeError
            return
        }

        if (!shouldSucceed) {
            executeError()
            return
        }

        executeSuccess()
    }

    override fun canAuthenticateWithBiometrics(context: Context): Int =
        BiometricManager.BIOMETRIC_SUCCESS

    fun getEncryptionCipher(): Cipher {
        return getCipher().apply { init(Cipher.ENCRYPT_MODE, secretKey) }
    }

    fun getEncryptionCounter(): Int {
        return encryptCounter
    }

    fun getDecryptionCounter(): Int {
        return decryptCounter
    }
}

package com.philkes.notallyx.test

import android.content.Context
import android.hardware.biometrics.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.philkes.notallyx.core.preference.NotallyXPreferences
import com.philkes.notallyx.core.security.BiometricAuthenticator
import com.philkes.notallyx.core.security.getCipher
import com.philkes.notallyx.core.security.getOrCreateSecretKey
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class FakeBiometricAuthenticator : BiometricAuthenticator, KoinComponent {
    var cipher: Cipher? = null
    var shouldSucceed: Boolean = true
    var iv: ByteArray? = null

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
        if (!shouldSucceed) {
            onError("Authentication failed")
            return
        }

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

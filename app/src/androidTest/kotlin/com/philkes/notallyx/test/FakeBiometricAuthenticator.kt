package com.philkes.notallyx.test

import android.content.Context
import android.hardware.biometrics.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.philkes.notallyx.utils.security.BiometricAuthenticator
import com.philkes.notallyx.utils.security.getCipher
import com.philkes.notallyx.utils.security.getOrCreateSecretKey
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class FakeBiometricAuthenticator : BiometricAuthenticator {
    var iv: ByteArray? = null
    private var encryptCounter = 0
    private var decryptCounter = 0
    private val secretKey: SecretKey

    constructor() {
        secretKey = getOrCreateSecretKey()
    }

    override fun authenticate(
        activity: FragmentActivity,
        cipherIv: ByteArray?,
        isForDecrypt: Boolean,
        onSuccess: (Cipher) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (iv != null) {
            val cipher = getCipher()
            if (isForDecrypt) {
                decryptCounter++
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(cipherIv))
            } else {
                encryptCounter++
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            }
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

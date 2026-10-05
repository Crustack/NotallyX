package com.philkes.notallyx.test

import android.content.Context
import android.hardware.biometrics.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.philkes.notallyx.utils.security.BiometricAuthenticator
import javax.crypto.Cipher

class FakeBiometricAuthenticator : BiometricAuthenticator {
    var cipher: Cipher? = null
    var shouldSucceed: Boolean = true

    override fun authenticate(
        activity: FragmentActivity,
        cipherIv: ByteArray?,
        isForDecrypt: Boolean,
        onSuccess: (Cipher?) -> Unit,
        onError: (String) -> Unit,
    ) {
        if (shouldSucceed) {
            onSuccess(cipher)
        } else {
            onError("Authentication failed")
        }
    }

    override fun canAuthenticateWithBiometrics(context: Context) =
        BiometricManager.BIOMETRIC_SUCCESS
}

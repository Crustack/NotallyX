package com.philkes.notallyx.utils.security

import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.philkes.notallyx.R
import javax.crypto.Cipher

class SystemBiometricAuthenticator(
    private val titleResId: Int = R.string.unlock,
    private val descriptionResId: Int? = null,
) : BiometricAuthenticator {

    override fun authenticate(
        activity: FragmentActivity,
        cipherIv: ByteArray?,
        isForDecrypt: Boolean,
        onSuccess: (Cipher?) -> Unit,
        onError: (String) -> Unit,
    ) {
        showBiometricOrPinPrompt(
            isForDecrypt = isForDecrypt,
            context = activity,
            titleResId = titleResId,
            descriptionResId = descriptionResId,
            cipherIv = cipherIv,
            onSuccess = onSuccess,
            onFailure = { errorCode -> onError(errorCode?.toString() ?: "Authentication failed") },
        )
    }

    override fun canAuthenticateWithBiometrics(context: Context): Int {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        val authenticators =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
            }
        return biometricManager.canAuthenticate(authenticators)
    }
}

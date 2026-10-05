package com.philkes.notallyx.utils.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.philkes.notallyx.R
import com.philkes.notallyx.presentation.viewmodel.preference.NotallyXPreferences
import javax.crypto.Cipher

fun showBiometricOrPinPrompt(
    isForDecrypt: Boolean,
    context: FragmentActivity,
    titleResId: Int,
    descriptionResId: Int? = null,
    cipherIv: ByteArray? = null,
    onSuccess: (cipher: Cipher?) -> Unit,
    onFailure: (errorCode: Int?) -> Unit,
) {
    val promptInfo =
        BiometricPrompt.PromptInfo.Builder()
            .apply {
                setTitle(context.getString(titleResId))
                descriptionResId?.let { setDescription(context.getString(descriptionResId)) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    )
                } else {
                    setNegativeButtonText(context.getString(R.string.cancel))
                    setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                }
            }
            .build()
    val preferences = NotallyXPreferences.getInstance(context)
    val cipher =
        if (preferences.biometricLockEncryptsDb.value) {
            if (isForDecrypt) {
                getInitializedCipherForDecryption(iv = cipherIv!!)
            } else {
                getInitializedCipherForEncryption()
            }
        } else {
            null
        }
    val authCallback =
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess.invoke(result.cryptoObject?.cipher)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailure.invoke(null)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onFailure.invoke(errorCode)
            }
        }
    val prompt = BiometricPrompt(context, ContextCompat.getMainExecutor(context), authCallback)
    if (cipher != null) {
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    } else {
        prompt.authenticate(promptInfo)
    }
}

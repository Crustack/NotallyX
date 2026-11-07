package com.philkes.notallyx.core.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

interface BiometricAuthenticator {

    fun authenticate(
        activity: FragmentActivity,
        cipherIv: ByteArray? = null,
        isForDecrypt: Boolean = false,
        onSuccess: (Cipher?) -> Unit,
        onError: (String) -> Unit,
    )

    fun canAuthenticateWithBiometrics(context: Context): Int
}

package com.philkes.notallyx.utils.security

object AuthenticatorProvider {
    var instance: BiometricAuthenticator = SystemBiometricAuthenticator()

    fun reset() {
        instance = SystemBiometricAuthenticator()
    }
}
